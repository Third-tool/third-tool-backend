package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.LearningFacade.domain.model.*;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.RevisionReasonOptionRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicRevisionRepository;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningFacadeRequest;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningFacadeResponse.*;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class LearningFacadeCommandService {

    private final LearningFacadeRepository facadeRepository;
    private final TopicRevisionRepository topicRevisionRepository;
    private final RevisionReasonOptionRepository revisionReasonOptionRepository;

    // ──────────────────────────────────────────────────────
    // 1. LearningFacade 생성
    // ──────────────────────────────────────────────────────

    public CreateFacade createFacade(UserEntity user, String concept) {
        if (facadeRepository.existsByUserId(user.getId())) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_ALREADY_EXISTS);
        }
        LearningFacade facade = LearningFacade.create(user, concept);
        return CreateFacade.of(facadeRepository.save(facade));
    }

    // ──────────────────────────────────────────────────────
    // 3. 컨셉 수정
    // ──────────────────────────────────────────────────────

    public UpdateConcept updateConcept(UserEntity user, String newConcept) {
        LearningFacade facade = loadFacade(user.getId());
        ConceptChangeRecord record = facade.updateConcept(newConcept);
        if (record.isChanged()) {
            facadeRepository.save(facade);
        }
        return UpdateConcept.of(facade, record);
    }

    // ──────────────────────────────────────────────────────
    // 4. 축 추가
    // ──────────────────────────────────────────────────────

    public AddAxis addAxis(UserEntity user, String name) {
        LearningFacade facade = loadFacade(user.getId());
        LearningAxis axis = facade.addAxis(name);
        facadeRepository.save(facade);
        return AddAxis.of(axis, facade.isAxisCountExceedsRecommended());
    }

    // ──────────────────────────────────────────────────────
    // 5. 축 이름 수정
    // ──────────────────────────────────────────────────────

    public UpdateAxisName updateAxisName(UserEntity user, Long axisId, String newName) {
        LearningFacade facade = loadFacade(user.getId());
        LearningAxis axis = findAxis(facade, axisId);

        String trimmed = newName == null ? null : newName.trim();
        boolean isDuplicate = facade.getAxes().stream()
                .filter(a -> !a.getId().equals(axisId))
                .anyMatch(a -> a.getName().equals(trimmed));
        if (isDuplicate) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_AXIS_DUPLICATE_NAME);
        }

        axis.updateName(newName);
        facadeRepository.save(facade);
        return UpdateAxisName.of(axis);
    }

    // ──────────────────────────────────────────────────────
    // 6. 축 삭제
    // ──────────────────────────────────────────────────────

    public void removeAxis(UserEntity user, Long axisId) {
        LearningFacade facade = loadFacade(user.getId());
        facade.removeAxis(axisId);
        facadeRepository.save(facade);
    }

    // ──────────────────────────────────────────────────────
    // 7. 축 순서 변경
    // ──────────────────────────────────────────────────────

    public ReorderAxes reorderAxes(UserEntity user, List<Long> orderedAxisIds) {
        LearningFacade facade = loadFacade(user.getId());
        facade.reorderAxes(orderedAxisIds);
        facadeRepository.save(facade);
        return ReorderAxes.of(facade.getAxes());
    }

    // ──────────────────────────────────────────────────────
    // 8. 주제 추가
    // ──────────────────────────────────────────────────────

    public AddTopic addTopic(UserEntity user, Long axisId, String name, String description) {
        LearningFacade facade = loadFacade(user.getId());
        LearningAxis axis = findAxis(facade, axisId);
        AxisTopic topic = axis.addTopic(name, description);
        facadeRepository.save(facade);
        return AddTopic.of(topic);
    }

    // ──────────────────────────────────────────────────────
    // 9. 주제 부분 수정 (idempotent)
    // ──────────────────────────────────────────────────────

    public UpdateTopic updateTopic(UserEntity user, Long axisId, Long topicId,
                                    LearningFacadeRequest.UpdateTopic command) {
        LearningFacade facade = loadFacade(user.getId());
        LearningAxis axis = findAxis(facade, axisId);
        AxisTopic topic = axis.findTopic(topicId);

        // 이름 변경 발생 시 이력을 위해 이전 값 보존 (description 수정만 일어나면 이력 X)
        String previousName = topic.getName();
        boolean nameChanged = false;
        boolean descriptionChanged = false;

        if (command.isNamePresent()) {
            nameChanged = topic.updateName(command.getName());
        }
        if (command.isDescriptionPresent()) {
            descriptionChanged = topic.updateDescription(command.getDescription());
        }

        if (nameChanged) {
            String reasonLabel = resolveReasonLabel(command.getRevisionReasonOptionId());
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

    public void removeTopic(UserEntity user, Long axisId, Long topicId) {
        LearningFacade facade = loadFacade(user.getId());
        LearningAxis axis = findAxis(facade, axisId);
        axis.removeTopic(topicId);
        facadeRepository.save(facade);
    }

    // ──────────────────────────────────────────────────────
    // 11. 주제 순서 변경
    // ──────────────────────────────────────────────────────

    public ReorderTopics reorderTopics(UserEntity user, Long axisId, List<Long> orderedTopicIds) {
        LearningFacade facade = loadFacade(user.getId());
        LearningAxis axis = findAxis(facade, axisId);
        axis.reorderTopics(orderedTopicIds);
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
