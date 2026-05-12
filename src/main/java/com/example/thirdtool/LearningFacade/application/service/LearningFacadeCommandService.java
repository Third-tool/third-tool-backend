package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.application.dto.LearningFacadeCommand;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.LearningFacade.domain.model.*;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.RevisionReasonOptionRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicDeletionRecordRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicRevisionRepository;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningFacadeResponse.*;
import lombok.RequiredArgsConstructor;
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
    // 8. 주제 추가
    // ──────────────────────────────────────────────────────

    public AddTopic addTopic(LearningFacadeCommand.AddTopic command) {
        LearningFacade facade = loadFacade(command.userId());
        LearningAxis axis = findAxis(facade, command.axisId());
        AxisTopic topic = axis.addTopic(command.name(), command.description());
        facadeRepository.save(facade);
        return AddTopic.of(topic);
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
