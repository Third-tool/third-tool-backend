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
        LearningFacade facade = LearningFacade.create(command.user(), command.concepts());
        return CreateFacade.of(facadeRepository.save(facade));
    }

    // ──────────────────────────────────────────────────────
    // 3. 컨셉 수정 (Story-LT-E1-S4: concepts[] 통째 교체)
    // ──────────────────────────────────────────────────────

    public UpdateConcepts updateConcepts(LearningFacadeCommand.UpdateConcepts command) {
        LearningFacade facade = loadFacade(command.userId());
        ConceptsChangeRecord record = facade.updateConcepts(command.concepts());
        if (record.isChanged()) {
            facadeRepository.save(facade);
        }
        return UpdateConcepts.of(facade, record);
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
        // Fix — Axis↔Deck 완전 통합: 축 소프트 삭제 → flush → Deck 연쇄 소프트 삭제 순서.
        //
        // 순서 재배치 근거 (Reviewer Sceptical Major 지적):
        //   Deck 연쇄를 axis flush 이전에 실행하면 관측 순서(deck 삭제 → axis 삭제)와
        //   코드 순서(axis softDelete → deck 연쇄)가 뒤바뀐다. 현재는 무해하지만,
        //   향후 softDeleteByAxisId가 "삭제된 축의 Deck만 삭제한다"는 방어 로직을
        //   추가할 경우 flush 이전 조회가 0건을 반환해 연쇄 삭제가 누락된다.
        //   → facadeRepository.save로 axis softDelete 먼저 flush → Deck 연쇄.
        // Deck.softDelete()가 소속 Card까지 연쇄 처리하므로 카드 별도 순회 불필요.
        // 세 호출 모두 동일 @Transactional 경계 내에서 원자성 보장.
        facadeRepository.save(facade);
        deckCommandService.softDeleteByAxisId(command.axisId());
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
    // Layer (Story-LT-E2-S4·S5)
    // ──────────────────────────────────────────────────────

    public AddLayer addLayer(LearningFacadeCommand.AddLayer command) {
        LearningFacade facade = loadFacade(command.userId());
        LearningLayer layer = facade.addLayer(command.name());
        facadeRepository.save(facade);
        if (layer.getId() == null) {
            throw new IllegalStateException(
                    "LearningLayer id가 cascade save 후에도 null입니다. JPA 설정 회귀 가능성.");
        }
        return AddLayer.of(layer, facade.isLayerCountExceedsRecommended());
    }

    public RenameLayer renameLayer(LearningFacadeCommand.RenameLayer command) {
        LearningFacade facade = loadFacade(command.userId());
        boolean changed = facade.renameLayer(command.layerId(), command.name());
        if (changed) {
            facadeRepository.save(facade);
        }
        LearningLayer layer = facade.findLayer(command.layerId());
        return RenameLayer.of(layer, changed);
    }

    public void removeLayer(LearningFacadeCommand.RemoveLayer command) {
        LearningFacade facade = loadFacade(command.userId());
        facade.removeLayer(command.layerId());
        facadeRepository.save(facade);
    }

    public ReorderLayers reorderLayers(LearningFacadeCommand.ReorderLayers command) {
        LearningFacade facade = loadFacade(command.userId());
        facade.reorderLayers(command.orderedLayerIds());
        facadeRepository.save(facade);
        return ReorderLayers.of(facade.getLayers());
    }

    // ──────────────────────────────────────────────────────
    // 사용자가 명시적으로 Deck을 생성하는 경로는 폐기되었다.
    // (Fix — Axis↔Deck 완전 통합, BE-Story 2, 2026-07-01)
    // Deck은 이제 LearningAxisCreatedEventHandler가 Axis 생성 이벤트에 반응해 자동으로만 생성한다.
    // 이전 createDeckUnderAxis()는 축=덱 정책에 따라 제거됨. deckCommandService 필드는
    // softDeleteByAxisId 조율(removeAxis 흐름) 목적으로만 유지된다.
    // ──────────────────────────────────────────────────────

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
