package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.LearningFacade.domain.model.*;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.ActionRevisionRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.RevisionReasonOptionRepository;
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
    private final ActionRevisionRepository revisionRepository;
    private final RevisionReasonOptionRepository reasonOptionRepository;
    private final VerbFormValidator verbFormValidator;

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
    // 8. 행동 추가
    // ──────────────────────────────────────────────────────

    public AddAction addAction(UserEntity user, Long axisId, String description) {
        LearningFacade facade = loadFacade(user.getId());
        LearningAxis axis = findAxis(facade, axisId);
        AxisAction action = axis.addAction(description);
        facadeRepository.save(facade);
        return AddAction.of(action, verbFormValidator.isSuggested(description));
    }

    // ──────────────────────────────────────────────────────
    // 9. 행동 동사 수정
    // ──────────────────────────────────────────────────────

    public UpdateAction updateAction(UserEntity user, Long axisId, Long actionId,
                                     String description, String revisionReasonLabel) {
        LearningFacade facade = loadFacade(user.getId());
        LearningAxis axis = findAxis(facade, axisId);
        AxisAction action = findAction(axis, actionId);

        ActionChangeRecord record = action.updateDescription(description);

        if (record.isChanged()) {
            String reasonLabel = resolveReasonLabel(revisionReasonLabel);
            ActionRevision revision = ActionRevision.create(
                    action,
                    record.getPreviousDescription(),
                    record.getNewDescription(),
                    reasonLabel
                                                           );
            revisionRepository.save(revision);
            facadeRepository.save(facade);
        }

        return UpdateAction.of(action, record, verbFormValidator.isSuggested(description));
    }

    // ──────────────────────────────────────────────────────
    // 10. 행동 삭제
    // ──────────────────────────────────────────────────────

    public void removeAction(UserEntity user, Long axisId, Long actionId) {
        LearningFacade facade = loadFacade(user.getId());
        LearningAxis axis = findAxis(facade, axisId);
        axis.removeAction(actionId);
        facadeRepository.save(facade);
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

    private AxisAction findAction(LearningAxis axis, Long actionId) {
        return axis.getActions().stream()
                   .filter(a -> a.getId().equals(actionId))
                   .findFirst()
                   .orElseThrow(() -> LearningFacadeDomainException.of(
                           ErrorCode.LEARNING_AXIS_ACTION_NOT_FOUND));
    }


    private String resolveReasonLabel(String revisionReasonLabel) {
        if (revisionReasonLabel == null || revisionReasonLabel.isBlank()) {
            return null;
        }
        reasonOptionRepository.findByLabelAndActiveTrue(revisionReasonLabel)
                              .orElseThrow(() -> LearningFacadeDomainException.of(
                                      ErrorCode.INVALID_INPUT,
                                      "유효하지 않은 수정 이유입니다: " + revisionReasonLabel));
        return revisionReasonLabel;
    }
}