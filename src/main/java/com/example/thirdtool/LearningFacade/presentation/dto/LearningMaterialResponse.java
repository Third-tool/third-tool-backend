package com.example.thirdtool.LearningFacade.presentation.dto;

import com.example.thirdtool.LearningFacade.domain.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class LearningMaterialResponse {

    // ──────────────────────────────────────────────────────
    // 공유 내부 레코드
    // ──────────────────────────────────────────────────────

    /**
     * 자료에 연결된 행동 아이템.
     * 자료 등록·목록·연결 추가 응답에서 공통 사용한다.
     */
    public record LinkedActionItem(
            Long actionId,
            String description,
            Long axisId,
            String axisName,
            String coverageStatus   // CoverageStatus.name()
    ) {
        public static LinkedActionItem of(ActionMaterial mapping) {
            AxisAction action = mapping.getAction();
            LearningAxis axis = action.getAxis();
            return new LinkedActionItem(
                    action.getId(),
                    action.getDescription(),
                    axis.getId(),
                    axis.getName(),
                    action.getCoverageStatus().name()
            );
        }
    }

    /**
     * 목록 조회 응답의 행동 아이템 (axisName 포함, coverageStatus 미포함).
     */
    public record LinkedActionSummary(
            Long actionId,
            String description,
            Long axisId,
            String axisName
    ) {
        public static LinkedActionSummary of(ActionMaterial mapping) {
            AxisAction action = mapping.getAction();
            LearningAxis axis = action.getAxis();
            return new LinkedActionSummary(
                    action.getId(),
                    action.getDescription(),
                    axis.getId(),
                    axis.getName()
            );
        }
    }

    /**
     * 커버리지 재계산 결과 아이템. 숙련도 변경 응답에서 사용한다.
     */
    public record ActionCoverageItem(
            Long actionId,
            String description,
            String coverageStatus
    ) {
        public static ActionCoverageItem of(AxisAction action) {
            return new ActionCoverageItem(
                    action.getId(),
                    action.getDescription(),
                    action.getCoverageStatus().name()
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 13. 학습 자료 등록 — POST /learning-facade/materials
    // ──────────────────────────────────────────────────────

    public record CreateMaterial(
            Long materialId,
            String name,
            String materialType,            // MaterialType.name()
            String url,
            String proficiencyLevel,        // ProficiencyLevel.name()
            List<LinkedActionItem> linkedActions,
            LocalDateTime createdAt
    ) {
        public static CreateMaterial of(LearningMaterial material) {
            return new CreateMaterial(
                    material.getId(),
                    material.getName(),
                    material.getMaterialType().name(),
                    material.getUrl(),
                    material.getProficiencyLevel().name(),
                    material.getActionMappings().stream()
                            .map(LinkedActionItem::of)
                            .collect(Collectors.toList()),
                    material.getCreatedAt()
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 14. 학습 자료 목록 조회 — GET /learning-facade/materials
    // ──────────────────────────────────────────────────────

    public record MaterialSummary(
            Long materialId,
            String name,
            String materialType,
            String url,
            String proficiencyLevel,
            List<LinkedActionSummary> linkedActions,
            LocalDateTime createdAt
    ) {
        public static MaterialSummary of(LearningMaterial material) {
            return new MaterialSummary(
                    material.getId(),
                    material.getName(),
                    material.getMaterialType().name(),
                    material.getUrl(),
                    material.getProficiencyLevel().name(),
                    material.getActionMappings().stream()
                            .map(LinkedActionSummary::of)
                            .collect(Collectors.toList()),
                    material.getCreatedAt()
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 15. 자료 이름 수정 — PATCH /learning-facade/materials/{materialId}/name
    // ──────────────────────────────────────────────────────

    public record UpdateMaterialName(
            Long materialId,
            String name
    ) {
        public static UpdateMaterialName of(LearningMaterial material) {
            return new UpdateMaterialName(material.getId(), material.getName());
        }
    }

    // ──────────────────────────────────────────────────────
    // 16. 숙련도 수정 — PATCH /learning-facade/materials/{materialId}/proficiency
    // ──────────────────────────────────────────────────────

    public record UpdateProficiency(
            Long materialId,
            String proficiencyLevel,
            List<ActionCoverageItem> updatedCoverages,
            boolean isCardCreationSuggested     // MASTERED 달성 시 Card 생성 안내
    ) {
        public static UpdateProficiency of(LearningMaterial material,
                                           List<ActionCoverageItem> updatedCoverages) {
            boolean isCardCreationSuggested =
                    material.getProficiencyLevel() == ProficiencyLevel.MASTERED;
            return new UpdateProficiency(
                    material.getId(),
                    material.getProficiencyLevel().name(),
                    updatedCoverages,
                    isCardCreationSuggested
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 17. 행동-자료 연결 추가 — POST /learning-facade/materials/{materialId}/actions
    // ──────────────────────────────────────────────────────

    public record LinkedActions(
            Long materialId,
            List<LinkedActionItem> linkedActions
    ) {
        public static LinkedActions of(LearningMaterial material) {
            return new LinkedActions(
                    material.getId(),
                    material.getActionMappings().stream()
                            .map(LinkedActionItem::of)
                            .collect(Collectors.toList())
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 18. 행동-자료 연결 해제 — DELETE /learning-facade/materials/{materialId}/actions/{actionId}
    // ──────────────────────────────────────────────────────

    public record UnlinkAction(
            Long materialId,
            Long unlinkedActionId,
            ActionCoverageItem updatedCoverage,
            List<LinkedActionSummary> remainingLinkedActions
    ) {}
}