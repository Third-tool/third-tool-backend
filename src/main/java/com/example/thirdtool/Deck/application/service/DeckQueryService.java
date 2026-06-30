package com.example.thirdtool.Deck.application.service;


import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.infrastructure.repository.DeckRepository;
import com.example.thirdtool.Deck.presentation.dto.DeckResponse;
import com.example.thirdtool.LearningFacade.application.service.LearningFacadeQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class DeckQueryService {

    private final DeckRepository deckRepository;
    private final LearningFacadeQueryService learningFacadeQueryService;

    public DeckQueryService(
            DeckRepository deckRepository,
            // Deck ↔ LearningFacade 양방향 read 협력 (PACKAGE.md §6).
            // LearningFacadeQueryService 또한 DeckQueryService에 의존하므로 @Lazy로 순환을 해소한다.
            @Lazy LearningFacadeQueryService learningFacadeQueryService
    ) {
        this.deckRepository = deckRepository;
        this.learningFacadeQueryService = learningFacadeQueryService;
    }

    // ─── 외부 API ─────────────────────────────────────────

    /**
     * 덱 단건 조회.
     * 삭제된 덱 접근 시 DECK_ALREADY_DELETED 예외.
     * axisId가 설정되어 있으면 cross-BC read로 axisName도 함께 동봉한다.
     */
    public DeckResponse.Detail findById(Long deckId) {
        Deck deck = getActiveDeck(deckId);
        String axisName = resolveAxisName(deck.getAxisId());
        return DeckResponse.Detail.of(deck, axisName);
    }

    /**
     * 내 루트 덱 목록 조회 (depth = 0, 논리 삭제 제외).
     * Repository 레벨에서 deleted = false 필터링.
     */
    public DeckResponse.Page findRootDecks(Long userId, Pageable pageable) {
        Page<Deck> page = deckRepository.findRootDecksByUserId(userId, pageable);

        Map<Long, String> axisNames = resolveAxisNames(page.getContent());

        List<DeckResponse.Summary> content = page.getContent()
                                                 .stream()
                                                 .map(deck -> DeckResponse.Summary.of(deck, lookupAxisName(axisNames, deck.getAxisId())))
                                                 .toList();

        return new DeckResponse.Page(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                pageable.getPageNumber(),
                pageable.getPageSize()
        );
    }

    /**
     * 축 ID 목록에 귀속된 활성 Deck 일괄 조회 (Story-005-2).
     * LearningFacade BC가 facade 상세 조회 시 축별 linkedDecks 매핑 용도로 호출.
     * 빈 collection이면 빈 리스트 단락 — DB 호출 회피.
     */
    public List<Deck> findByAxisIds(Collection<Long> axisIds) {
        if (axisIds == null || axisIds.isEmpty()) {
            return List.of();
        }
        return deckRepository.findByAxisIdInAndDeletedFalse(axisIds);
    }

    /**
     * 하위 덱 목록 조회.
     * 부모 덱의 subDecks 중 논리 삭제된 덱은 제외하여 반환.
     */
    public DeckResponse.SubDeckList findSubDecks(Long deckId) {
        Deck parent = getActiveDeck(deckId);

        List<Deck> activeSubDecks = parent.getSubDecks()
                                          .stream()
                                          .filter(sub -> !sub.isDeleted())
                                          .toList();

        Map<Long, String> axisNames = resolveAxisNames(activeSubDecks);

        List<DeckResponse.Summary> subDecks = activeSubDecks.stream()
                                                            .map(deck -> DeckResponse.Summary.of(deck, lookupAxisName(axisNames, deck.getAxisId())))
                                                            .toList();

        return new DeckResponse.SubDeckList(deckId, subDecks);
    }

    // ─── 내부 공용 메서드 ─────────────────────────────────

    /**
     * 활성 덱 조회.
     * 존재하지 않거나 논리 삭제된 덱이면 예외.
     * DeckCommandService / DeckHierarchyService에서 공통 사용.
     */
    public Deck getActiveDeck(Long deckId) {
        Deck deck = deckRepository.findById(deckId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        if (deck.isDeleted()) {
            throw new BusinessException(ErrorCode.DECK_ALREADY_DELETED);
        }
        return deck;
    }

    /**
     * Deck 컬렉션이 참조하는 axisId들을 모아 cross-BC read 1회로 axisName Map을 만든다.
     * 고아 덱(axisId == null)은 lookup 대상이 아니다 — N+1 회피.
     */
    private Map<Long, String> resolveAxisNames(Collection<Deck> decks) {
        Set<Long> axisIds = decks.stream()
                                  .map(Deck::getAxisId)
                                  .filter(Objects::nonNull)
                                  .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        return learningFacadeQueryService.findAxisNamesByIds(axisIds);
    }

    /**
     * 단건 axisId에 대응하는 axisName 조회. null axisId이면 null 반환 — DB 호출 회피.
     */
    private String resolveAxisName(Long axisId) {
        if (axisId == null) {
            return null;
        }
        return learningFacadeQueryService.findAxisNamesByIds(List.of(axisId)).get(axisId);
    }

    /**
     * Map.of()로 만든 불변 Map은 get(null)에서 NPE를 던지므로 null axisId 가드가 필요.
     */
    private String lookupAxisName(Map<Long, String> axisNames, Long axisId) {
        return axisId == null ? null : axisNames.get(axisId);
    }
}
