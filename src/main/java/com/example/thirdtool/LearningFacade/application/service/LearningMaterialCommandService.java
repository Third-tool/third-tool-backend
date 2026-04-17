package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.LearningFacade.domain.model.*;
import com.example.thirdtool.LearningFacade.domain.repository.*;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningMaterialResponse;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningMaterialResponse.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningMaterialCommandService {

    private final LearningFacadeRepository facadeRepository;
    private final LearningMaterialRepository materialRepository;
    private final ActionMaterialRepository actionMaterialRepository;
    private final CoverageRecalculator coverageRecalculator;

    // ──────────────────────────────────────────────────────
    // 13. 학습 자료 등록
    // ──────────────────────────────────────────────────────

    @Transactional
    public CreateMaterial createMaterial(Long userId, String name, MaterialType materialType,
                                         String url, List<Long> linkedActionIds) {
        LearningFacade facade = loadFacade(userId);
        LearningMaterial material = LearningMaterial.create(facade, name, materialType, url);
        materialRepository.save(material);

        // 행동 연결 + 커버리지 재계산
        if (linkedActionIds != null && !linkedActionIds.isEmpty()) {
            for (Long actionId : linkedActionIds) {
                AxisAction action = findActionInFacade(facade, actionId);
                ActionMaterial mapping = ActionMaterial.create(action, material);
                actionMaterialRepository.save(mapping);
                coverageRecalculator.recalculate(action);
            }
        }

        // 최신 매핑 포함 재로딩
        LearningMaterial saved = materialRepository.findById(material.getId())
                                                   .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.LEARNING_MATERIAL_NOT_FOUND));
        return CreateMaterial.of(saved);
    }

    // ──────────────────────────────────────────────────────
    // 14. 학습 자료 목록 조회
    // ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MaterialSummary> getMaterials(Long userId) {
        LearningFacade facade = loadFacade(userId);
        return materialRepository.findByFacadeId(facade.getId())
                                 .stream()
                                 .map(MaterialSummary::of)
                                 .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────
    // 15. 자료 이름 수정
    // ──────────────────────────────────────────────────────

    @Transactional
    public UpdateMaterialName updateMaterialName(Long userId, Long materialId, String newName) {
        LearningMaterial material = loadMaterial(materialId, userId);
        material.updateName(newName);
        return UpdateMaterialName.of(material);
    }

    // ──────────────────────────────────────────────────────
    // 16. 숙련도 자가 평가 수정
    // ──────────────────────────────────────────────────────

    @Transactional
    public UpdateProficiency updateProficiency(Long userId, Long materialId,
                                               ProficiencyLevel newLevel) {
        // UNRATED 복귀 차단 — Application Service 책임
        if (newLevel == ProficiencyLevel.UNRATED) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_MATERIAL_PROFICIENCY_UNRATED_NOT_ALLOWED);
        }

        LearningMaterial material = loadMaterial(materialId, userId);
        material.updateProficiencyLevel(newLevel);

        // 연결된 모든 행동의 커버리지 재계산
        List<ActionCoverageItem> updatedCoverages = material.getActionMappings().stream()
                                                            .map(mapping -> {
                                                                AxisAction action = mapping.getAction();
                                                                CoverageStatus newStatus = coverageRecalculator.recalculate(action);
                                                                return ActionCoverageItem.of(action);
                                                            })
                                                            .collect(Collectors.toList());

        return UpdateProficiency.of(material, updatedCoverages);
    }

    // ──────────────────────────────────────────────────────
    // 17. 행동-자료 연결 추가
    // ──────────────────────────────────────────────────────

    @Transactional
    public LinkedActions linkAction(Long userId, Long materialId, Long actionId) {
        LearningMaterial material = loadMaterial(materialId, userId);

        if (actionMaterialRepository.existsByActionIdAndMaterialId(actionId, materialId)) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_ACTION_MATERIAL_ALREADY_LINKED);
        }

        LearningFacade facade = loadFacade(userId);
        AxisAction action = findActionInFacade(facade, actionId);

        ActionMaterial mapping = ActionMaterial.create(action, material);
        actionMaterialRepository.save(mapping);
        coverageRecalculator.recalculate(action);

        // 최신 매핑 포함 재로딩
        LearningMaterial reloaded = materialRepository.findById(materialId)
                                                      .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.LEARNING_MATERIAL_NOT_FOUND));
        return LinkedActions.of(reloaded);
    }

    // ──────────────────────────────────────────────────────
    // 18. 행동-자료 연결 해제
    // ──────────────────────────────────────────────────────

    @Transactional
    public UnlinkAction unlinkAction(Long userId, Long materialId, Long actionId) {
        loadMaterial(materialId, userId);   // 소유권 + 존재 검증

        ActionMaterial mapping = actionMaterialRepository
                .findByActionIdAndMaterialId(actionId, materialId)
                .orElseThrow(() -> LearningFacadeDomainException.of(
                        ErrorCode.LEARNING_ACTION_MATERIAL_NOT_LINKED));

        AxisAction action = mapping.getAction();
        actionMaterialRepository.delete(mapping);

        // 남은 매핑 기준으로 커버리지 재계산
        CoverageStatus newStatus = coverageRecalculator.recalculate(action);

        // 남은 연결 목록 조회
        List<ActionMaterial> remaining = actionMaterialRepository.findByActionId(action.getId());
        List<LinkedActionSummary> remainingActions = remaining.stream()
                                                              .map(m -> {
                                                                  AxisAction a = m.getAction();
                                                                  return new LinkedActionSummary(
                                                                          a.getId(), a.getDescription(),
                                                                          a.getAxis().getId(), a.getAxis().getName());
                                                              })
                                                              .collect(Collectors.toList());

        return new UnlinkAction(
                materialId,
                actionId,
                ActionCoverageItem.of(action),
                remainingActions
        );
    }

    // ──────────────────────────────────────────────────────
    // 19. 학습 자료 삭제
    // ──────────────────────────────────────────────────────

    @Transactional
    public void deleteMaterial(Long userId, Long materialId) {
        LearningMaterial material = loadMaterial(materialId, userId);

        // 삭제 전 영향받는 행동 목록 수집 (cascade 삭제 후 접근 불가)
        List<AxisAction> affectedActions = material.getActionMappings().stream()
                                                   .map(ActionMaterial::getAction)
                                                   .collect(Collectors.toList());

        materialRepository.delete(material);
        // orphanRemoval이 ActionMaterial을 함께 삭제한다.
        // 각 행동의 커버리지 재계산 (남은 매핑 기준)
        affectedActions.forEach(coverageRecalculator::recalculate);
    }

    // ──────────────────────────────────────────────────────
    // 내부 유틸
    // ──────────────────────────────────────────────────────

    private LearningFacade loadFacade(Long userId) {
        return facadeRepository.findByUserId(userId)
                               .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_NOT_FOUND));
    }

    private LearningMaterial loadMaterial(Long materialId, Long userId) {
        LearningMaterial material = materialRepository.findById(materialId)
                                                      .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.LEARNING_MATERIAL_NOT_FOUND));
        if (!material.getFacade().getUser().getId().equals(userId)) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_NOT_FOUND);
        }
        return material;
    }

    /** facade 전체를 순회해 actionId로 AxisAction을 찾는다. */
    private AxisAction findActionInFacade(LearningFacade facade, Long actionId) {
        return facade.getAxes().stream()
                     .flatMap(axis -> axis.getActions().stream())
                     .filter(a -> a.getId().equals(actionId))
                     .findFirst()
                     .orElseThrow(() -> LearningFacadeDomainException.of(
                             ErrorCode.LEARNING_AXIS_ACTION_NOT_FOUND));
    }
}