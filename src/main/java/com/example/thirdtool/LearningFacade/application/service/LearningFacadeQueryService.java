package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.Card.application.service.CardQueryService;
import com.example.thirdtool.Card.domain.model.CardStatus;
import com.example.thirdtool.Card.presentation.dto.CardResponse;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.application.service.DeckQueryService;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.LearningFacade.application.dto.LearningFacadeQuery;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.LearningFacade.domain.model.AxisTopic;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.domain.model.MaterialType;
import com.example.thirdtool.LearningFacade.domain.model.TopicMaterial;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicMaterialRepository;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningFacadeResponse.FacadeDetail;
import com.example.thirdtool.LearningFacade.presentation.dto.MaterialBreakdown;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningFacadeQueryService {

    private final LearningFacadeRepository facadeRepository;
    private final TopicMaterialRepository topicMaterialRepository;
    private final DeckQueryService deckQueryService;
    private final CardQueryService cardQueryService;

    /**
     * Cross-BC inbound (Story 6-1 Layer 1 한정) — 사용자 LearningFacade가 보유한 모든 axis ID를 반환.
     *
     * <p>Review BC가 오늘 학습 후보 수집 시 Layer 1(사용자 직업 컨셉) 범위로 카드를 좁히기 위해 호출한다.
     * 사용자에게 LearningFacade가 아예 없으면 빈 리스트 — 호출 측은 fallback으로 사용자 전체 카드를 본다
     * (Spec 6-1 엣지 케이스).
     *
     * <p>도메인 행위 메서드(LearningFacade)를 노출하지 않고 ID 리스트만 반환해 BC 경계 유지.
     */
    @Transactional(readOnly = true)
    public List<Long> findAxisIdsByUserId(Long userId) {
        return facadeRepository.findByUserId(userId)
                .map(facade -> facade.getAxes().stream()
                        .map(LearningAxis::getId)
                        .toList())
                .orElseGet(List::of);
    }

    /**
     * Cross-BC read — Deck BC가 응답 DTO에 axisName을 동봉할 때 사용한다.
     * 입력이 null/empty면 빈 Map. 미존재 axisId는 결과에 포함되지 않는다 (= 호출자가 null로 처리).
     */
    @Transactional(readOnly = true)
    public Map<Long, String> findAxisNamesByIds(Collection<Long> axisIds) {
        return facadeRepository.findAxisNamesByIds(axisIds);
    }

    @Transactional(readOnly = true)
    public FacadeDetail getFacade(LearningFacadeQuery.GetFacade query) {
        LearningFacade facade = facadeRepository.findByUserId(query.userId())
                .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_NOT_FOUND));

        List<Long> topicIds = facade.getAxes().stream()
                .flatMap(axis -> axis.getTopics().stream())
                .map(AxisTopic::getId)
                .collect(Collectors.toList());

        Map<Long, MaterialBreakdown> breakdownByTopic = computeBreakdown(topicIds);

        // Story-005-2: 축별 연결 Deck 목록 일괄 조회 (N+1 회피).
        List<Long> axisIds = facade.getAxes().stream()
                .map(LearningAxis::getId)
                .collect(Collectors.toList());
        Map<Long, List<Deck>> linkedDecksByAxis = deckQueryService.findByAxisIds(axisIds).stream()
                .collect(Collectors.groupingBy(Deck::getAxisId));

        return FacadeDetail.of(facade, breakdownByTopic, linkedDecksByAxis);
    }

    /**
     * 축 스코프 카드 조회 — fix-deck-axis-visibility (0.0.2v) Fix-Story 4.
     *
     * <p>흐름: (1) userId의 LearningFacade 로드 → (2) axisId가 그 사용자의 축 목록에 포함되는지
     * 소유권 검증 → (3) CardQueryService.findByAxisIds로 위임. today 집계와 동일 read-model 공유.
     *
     * <p>실패 응답:
     * <ul>
     *   <li>userId가 LearningFacade 미보유 → {@link ErrorCode#LEARNING_FACADE_NOT_FOUND} (404)
     *   <li>axisId가 다른 사용자 소유 또는 존재하지 않음 → {@link ErrorCode#LEARNING_FACADE_FORBIDDEN} (403)
     * </ul>
     *
     * <p>status가 null이면 호출자 책임으로 ON_FIELD를 기본값으로 넘긴다 (컨트롤러가 처리).
     */
    @Transactional(readOnly = true)
    public List<CardResponse.Summary> findAxisCards(LearningFacadeQuery.FindAxisCards query) {
        LearningFacade facade = facadeRepository.findByUserId(query.userId())
                .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_NOT_FOUND));

        boolean owned = facade.getAxes().stream()
                .map(LearningAxis::getId)
                .anyMatch(id -> id.equals(query.axisId()));
        if (!owned) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_FORBIDDEN);
        }

        CardStatus status = query.status() != null ? query.status() : CardStatus.ON_FIELD;
        return cardQueryService.findByAxisIds(query.userId(), List.of(query.axisId()), status);
    }

    // table-spec §3-1 (4): 저장하지 않고 인메모리 그룹핑. 주제당 자료 수가 수십 건 수준이라 한 번의 IN 쿼리로 충분.
    private Map<Long, MaterialBreakdown> computeBreakdown(List<Long> topicIds) {
        Map<Long, MaterialBreakdown> result = new HashMap<>();
        if (topicIds.isEmpty()) {
            return result;
        }

        List<TopicMaterial> mappings = topicMaterialRepository.findByTopicIdIn(topicIds);

        Map<Long, List<MaterialType>> typesByTopic = mappings.stream()
                .collect(Collectors.groupingBy(
                        tm -> tm.getTopic().getId(),
                        Collectors.mapping(
                                tm -> tm.getMaterial().getMaterialType(),
                                Collectors.toList())));

        for (Long topicId : topicIds) {
            List<MaterialType> types = typesByTopic.getOrDefault(topicId, List.of());
            result.put(topicId, MaterialBreakdown.from(types));
        }
        return result;
    }
}
