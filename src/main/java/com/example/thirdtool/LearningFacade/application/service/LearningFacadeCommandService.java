package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.application.service.DeckCommandService;
import com.example.thirdtool.Deck.presentation.dto.DeckResponse;
import com.example.thirdtool.LearningFacade.application.dto.LearningFacadeCommand;
import com.example.thirdtool.LearningFacade.domain.event.LearningAxisCreatedEvent;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.LearningFacade.domain.model.*;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningMaterialRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.RevisionReasonOptionRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicDeletionRecordRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicMaterialRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicRevisionRepository;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningFacadeResponse.*;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class LearningFacadeCommandService {

    private final LearningFacadeRepository facadeRepository;
    private final TopicRevisionRepository topicRevisionRepository;
    private final RevisionReasonOptionRepository revisionReasonOptionRepository;
    private final TopicDeletionRecordRepository topicDeletionRecordRepository;
    private final LearningMaterialRepository learningMaterialRepository;
    private final TopicMaterialRepository topicMaterialRepository;
    private final ApplicationEventPublisher eventPublisher;
    // Fix-Story 2: 축 스코프 Deck 생성 위임 (LearningFacade → Deck Application write).
    // 자동 생성(LearningAxisCreatedEventHandler)과 별개로 사용자가 명시한 신규 Deck을 처리.
    private final DeckCommandService deckCommandService;

    // ──────────────────────────────────────────────────────
    // 1. LearningFacade 생성
    // ──────────────────────────────────────────────────────

    public CreateFacade createFacade(LearningFacadeCommand.CreateFacade command) {
        if (facadeRepository.existsByUserId(command.user().getId())) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_ALREADY_EXISTS);
        }
        LearningFacade facade = LearningFacade.create(command.user(), command.concept());
        return CreateFacade.of(facadeRepository.save(facade));
    }

    // ──────────────────────────────────────────────────────
    // 3. 컨셉 수정
    // ──────────────────────────────────────────────────────

    public UpdateConcept updateConcept(LearningFacadeCommand.UpdateConcept command) {
        LearningFacade facade = loadFacade(command.userId());
        ConceptChangeRecord record = facade.updateConcept(command.concept());
        if (record.isChanged()) {
            facadeRepository.save(facade);
        }
        return UpdateConcept.of(facade, record);
    }

    // ──────────────────────────────────────────────────────
    // 4. 축 추가
    // ──────────────────────────────────────────────────────

    public AddAxis addAxis(LearningFacadeCommand.AddAxis command) {
        LearningFacade facade = loadFacade(command.userId());
        LearningAxis axis = facade.addAxis(command.name());
        facadeRepository.save(facade);

        // IDENTITY cascade로 save 직후 axis.getId()가 채워져야 정상 (ADR007 §결정).
        // null이면 cascade·flush 설정 변경의 회귀 신호 — 즉시 실패시켜 무음 NPE 방지.
        if (axis.getId() == null) {
            throw new IllegalStateException(
                    "LearningAxis id가 cascade save 후에도 null입니다. JPA 설정 회귀 가능성.");
        }

        eventPublisher.publishEvent(
                new LearningAxisCreatedEvent(command.userId(), axis.getId(), axis.getName()));

        return AddAxis.of(axis, facade.isAxisCountExceedsRecommended());
    }

    // ──────────────────────────────────────────────────────
    // 5. 축 이름 수정
    // ──────────────────────────────────────────────────────

    public UpdateAxisName updateAxisName(LearningFacadeCommand.UpdateAxisName command) {
        LearningFacade facade = loadFacade(command.userId());
        LearningAxis axis = findAxis(facade, command.axisId());

        String trimmed = command.name() == null ? null : command.name().trim();
        boolean isDuplicate = facade.getAxes().stream()
                .filter(a -> !a.getId().equals(command.axisId()))
                .anyMatch(a -> a.getName().equals(trimmed));
        if (isDuplicate) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_AXIS_DUPLICATE_NAME);
        }

        axis.updateName(command.name());
        facadeRepository.save(facade);
        return UpdateAxisName.of(axis);
    }

    // ──────────────────────────────────────────────────────
    // 6. 축 삭제
    // ──────────────────────────────────────────────────────

    public void removeAxis(LearningFacadeCommand.RemoveAxis command) {
        LearningFacade facade = loadFacade(command.userId());
        facade.removeAxis(command.axisId());
        // Fix — Axis↔Deck 완전 통합: 축이 소프트 삭제되면 소속 Deck도 연쇄 소프트 삭제한다.
        // Deck.softDelete()가 소속 Card까지 연쇄 처리하므로 카드 별도 순회 불필요.
        // 동일 트랜잭션 내에서 처리되어 원자성 보장.
        deckCommandService.softDeleteByAxisId(command.axisId());
        facadeRepository.save(facade);
    }

    // ──────────────────────────────────────────────────────
    // 7. 축 순서 변경
    // ──────────────────────────────────────────────────────

    public ReorderAxes reorderAxes(LearningFacadeCommand.ReorderAxes command) {
        LearningFacade facade = loadFacade(command.userId());
        facade.reorderAxes(command.orderedAxisIds());
        facadeRepository.save(facade);
        return ReorderAxes.of(facade.getAxes());
    }

    // ──────────────────────────────────────────────────────
    // 7-bis. 축 스코프 Deck 생성 (Fix-Story 2)
    // ──────────────────────────────────────────────────────

    /**
     * 사용자가 axis에 명시적으로 신규 Deck을 추가한다.
     *
     * <p>Cross-BC write — Deck BC의 {@link DeckCommandService#createUnderAxis} 위임. 본 메서드는
     * facade/axis 소유권만 검증하고 Deck 생성·중복 검증은 Deck BC가 담당한다.
     *
     * <p>응답 DTO에 axisName이 동봉되도록 검증된 axis.name을 그대로 전달 — 추가 lookup 없음.
     */
    public DeckResponse.Create createDeckUnderAxis(LearningFacadeCommand.CreateAxisDeck command) {
        LearningFacade facade = loadFacade(command.userId());
        LearningAxis axis = findAxis(facade, command.axisId());
        return deckCommandService.createUnderAxis(
                facade.getUser(),
                axis.getId(),
                command.name(),
                axis.getName());
    }

    // ──────────────────────────────────────────────────────
    // 8. 주제 추가
    // ──────────────────────────────────────────────────────

    public AddTopic addTopic(LearningFacadeCommand.AddTopic command) {
        LearningFacade facade = loadFacade(command.userId());
        LearningAxis axis = findAxis(facade, command.axisId());
        AxisTopic topic = axis.addTopic(command.name(), command.description());
        facadeRepository.save(facade);

        // Story-004-2: 신규 주제는 항상 NO_MATERIAL이므로 "기존 자료에 연결하기" 후보를 함께 응답한다.
        // 스펙: findByFacadeId + existsByTopicIdAndMaterialId 조합 (신규 Repository 메서드 도입 없음)
        List<LinkableMaterialItem> linkable = learningMaterialRepository.findByFacadeId(facade.getId()).stream()
                .filter(m -> !topicMaterialRepository.existsByTopicIdAndMaterialId(topic.getId(), m.getId()))
                .map(LinkableMaterialItem::of)
                .toList();
        return AddTopic.of(topic, linkable);
    }

    // ──────────────────────────────────────────────────────
    // 9. 주제 부분 수정 (idempotent)
    // ──────────────────────────────────────────────────────

    public UpdateTopic updateTopic(LearningFacadeCommand.UpdateTopic command) {
        LearningFacade facade = loadFacade(command.userId());
        LearningAxis axis = findAxis(facade, command.axisId());
        AxisTopic topic = axis.findTopic(command.topicId());

        // 이름 변경 발생 시 이력을 위해 이전 값 보존 (description 수정만 일어나면 이력 X)
        String previousName = topic.getName();
        boolean nameChanged = false;
        boolean descriptionChanged = false;

        if (command.namePresent()) {
            nameChanged = topic.updateName(command.name());
        }
        if (command.descriptionPresent()) {
            descriptionChanged = topic.updateDescription(command.description());
        }

        if (nameChanged) {
            String reasonLabel = resolveReasonLabel(command.revisionReasonOptionId());
            TopicRevision revision = TopicRevision.of(topic, previousName, topic.getName(), reasonLabel);
            topicRevisionRepository.save(revision);
        }
        if (nameChanged || descriptionChanged) {
            facadeRepository.save(facade);
        }
        return UpdateTopic.of(topic);
    }

    /**
     * reasonOptionId가 null이면 라벨 없음(이유 미선택). 값이 있으면 active 선택지를 조회하여
     * 라벨을 스냅샷한다. 비활성·미존재 선택지는 REVISION_REASON_NOT_FOUND로 거부.
     */
    private String resolveReasonLabel(Long reasonOptionId) {
        if (reasonOptionId == null) {
            return null;
        }
        return revisionReasonOptionRepository.findActiveById(reasonOptionId)
                .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.REVISION_REASON_NOT_FOUND))
                .getLabel();
    }

    // ──────────────────────────────────────────────────────
    // 10. 주제 삭제
    // ──────────────────────────────────────────────────────

    public void removeTopic(LearningFacadeCommand.RemoveTopic command) {
        LearningFacade facade = loadFacade(command.userId());
        LearningAxis axis = findAxis(facade, command.axisId());

        // 삭제 직전 스냅샷을 archive로 보존 (ADR003: AxisTopic은 soft delete 미적용 — archive 패턴)
        AxisTopic topic = axis.findTopic(command.topicId());
        topicDeletionRecordRepository.save(TopicDeletionRecord.of(topic));

        axis.removeTopic(command.topicId());
        facadeRepository.save(facade);
    }

    // ──────────────────────────────────────────────────────
    // 11. 주제 순서 변경
    // ──────────────────────────────────────────────────────

    public ReorderTopics reorderTopics(LearningFacadeCommand.ReorderTopics command) {
        LearningFacade facade = loadFacade(command.userId());
        LearningAxis axis = findAxis(facade, command.axisId());
        axis.reorderTopics(command.orderedTopicIds());
        facadeRepository.save(facade);
        return ReorderTopics.of(axis);
    }

    // ──────────────────────────────────────────────────────
    // 내부 유틸
    // ──────────────────────────────────────────────────────

    private LearningFacade loadFacade(Long userId) {
        return facadeRepository.findByUserId(userId)
                .orElseThrow(() -> LearningFacadeDomainException.of(
                        ErrorCode.LEARNING_FACADE_NOT_FOUND));
    }

    private LearningAxis findAxis(LearningFacade facade, Long axisId) {
        return facade.getAxes().stream()
                .filter(a -> a.getId().equals(axisId))
                .findFirst()
                .orElseThrow(() -> LearningFacadeDomainException.of(
                        ErrorCode.LEARNING_AXIS_NOT_FOUND));
    }
}
