package com.example.thirdtool.LearningFacade.presentation.dto;

import com.example.thirdtool.LearningFacade.domain.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class LearningFacadeResponse {

    // ──────────────────────────────────────────────────────
    // 공유 내부 레코드
    // ──────────────────────────────────────────────────────

    public record ActionItem(
            Long actionId,
            String description,
            String coverageStatus,          // CoverageStatus.name()
            int revisionCount,
            boolean isRefinementSuggested
    ) {
        public static ActionItem of(AxisAction action) {
            return new ActionItem(
                    action.getId(),
                    action.getDescription(),
                    action.getCoverageStatus().name(),
                    action.getRevisionCount(),
                    action.isRefinementSuggested()
            );
        }
    }

    public record AxisItem(
            Long axisId,
            String name,
            int displayOrder,
            List<ActionItem> actions
    ) {
        public static AxisItem of(LearningAxis axis) {
            return new AxisItem(
                    axis.getId(),
                    axis.getName(),
                    axis.getDisplayOrder(),
                    axis.getActions().stream()
                        .map(ActionItem::of)
                        .collect(Collectors.toList())
            );
        }
    }

    public record CoverageSummary(
            int totalActions,
            int uncoveredActions    // NO_MATERIAL 상태인 행동 수
    ) {}

    // ──────────────────────────────────────────────────────
    // 1. LearningFacade 생성 — POST /learning-facade
    // ──────────────────────────────────────────────────────

    public record CreateFacade(
            Long facadeId,
            String concept,
            List<AxisItem> axes,
            LocalDateTime createdAt
    ) {
        public static CreateFacade of(LearningFacade facade) {
            return new CreateFacade(
                    facade.getId(),
                    facade.getConcept(),
                    List.of(),              // 신규 Facade는 axes 없음
                    facade.getCreatedAt()
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 2. LearningFacade 단건 조회 — GET /learning-facade
    // ──────────────────────────────────────────────────────

    public record FacadeDetail(
            Long facadeId,
            String concept,
            CoverageSummary coverageSummary,
            boolean isAxisCountExceedsRecommended,
            List<AxisItem> axes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static FacadeDetail of(LearningFacade facade) {
            List<AxisAction> allActions = facade.getAxes().stream()
                                                .flatMap(axis -> axis.getActions().stream())
                                                .collect(Collectors.toList());

            int totalActions     = allActions.size();
            int uncoveredActions = (int) allActions.stream()
                                                   .filter(a -> a.getCoverageStatus() == CoverageStatus.NO_MATERIAL)
                                                   .count();

            return new FacadeDetail(
                    facade.getId(),
                    facade.getConcept(),
                    new CoverageSummary(totalActions, uncoveredActions),
                    facade.isAxisCountExceedsRecommended(),
                    facade.getAxes().stream().map(AxisItem::of).collect(Collectors.toList()),
                    facade.getCreatedAt(),
                    facade.getUpdatedAt()
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 3. 컨셉 수정 — PATCH /learning-facade/concept
    // ──────────────────────────────────────────────────────

    public record UpdateConcept(
            Long facadeId,
            String concept,
            boolean isConceptChanged,
            boolean isDrifted,
            LocalDateTime updatedAt
    ) {
        public static UpdateConcept of(LearningFacade facade, ConceptChangeRecord record) {
            return new UpdateConcept(
                    facade.getId(),
                    facade.getConcept(),
                    record.isChanged(),
                    record.isDrifted(),
                    facade.getUpdatedAt()
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 4. 축 추가 — POST /learning-facade/axes
    // ──────────────────────────────────────────────────────

    public record AddAxis(
            Long axisId,
            String name,
            int displayOrder,
            List<ActionItem> actions,
            boolean isAxisCountExceedsRecommended
    ) {
        public static AddAxis of(LearningAxis axis, boolean isAxisCountExceedsRecommended) {
            return new AddAxis(
                    axis.getId(),
                    axis.getName(),
                    axis.getDisplayOrder(),
                    List.of(),             // 신규 축은 actions 없음
                    isAxisCountExceedsRecommended
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 5. 축 이름 수정 — PATCH /learning-facade/axes/{axisId}
    // ──────────────────────────────────────────────────────

    public record UpdateAxisName(
            Long axisId,
            String name,
            int displayOrder
    ) {
        public static UpdateAxisName of(LearningAxis axis) {
            return new UpdateAxisName(axis.getId(), axis.getName(), axis.getDisplayOrder());
        }
    }

    // ──────────────────────────────────────────────────────
    // 7. 축 순서 변경 — PUT /learning-facade/axes/order
    // ──────────────────────────────────────────────────────

    public record AxisOrderItem(
            Long axisId,
            String name,
            int displayOrder
    ) {
        public static AxisOrderItem of(LearningAxis axis) {
            return new AxisOrderItem(axis.getId(), axis.getName(), axis.getDisplayOrder());
        }
    }

    public record ReorderAxes(
            List<AxisOrderItem> axes
    ) {
        public static ReorderAxes of(List<LearningAxis> axes) {
            return new ReorderAxes(
                    axes.stream().map(AxisOrderItem::of).collect(Collectors.toList())
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 8. 행동 추가 — POST /learning-facade/axes/{axisId}/actions
    // ──────────────────────────────────────────────────────

    public record AddAction(
            Long actionId,
            Long axisId,
            String description,
            String coverageStatus,
            int revisionCount,
            boolean isRefinementSuggested,
            boolean isVerbFormSuggested     // App Service 판단 플래그
    ) {
        public static AddAction of(AxisAction action, boolean isVerbFormSuggested) {
            return new AddAction(
                    action.getId(),
                    action.getAxis().getId(),
                    action.getDescription(),
                    action.getCoverageStatus().name(),
                    action.getRevisionCount(),
                    action.isRefinementSuggested(),
                    isVerbFormSuggested
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 9. 행동 동사 수정 — PATCH /learning-facade/axes/{axisId}/actions/{actionId}
    // ──────────────────────────────────────────────────────

    public record UpdateAction(
            Long actionId,
            Long axisId,
            String description,
            String coverageStatus,
            int revisionCount,
            boolean isRefinementSuggested,
            boolean isActionChanged,        // ActionChangeRecord 결과
            boolean isVerbFormSuggested
    ) {
        public static UpdateAction of(AxisAction action, ActionChangeRecord record, boolean isVerbFormSuggested) {
            return new UpdateAction(
                    action.getId(),
                    action.getAxis().getId(),
                    action.getDescription(),
                    action.getCoverageStatus().name(),
                    action.getRevisionCount(),
                    action.isRefinementSuggested(),
                    record.isChanged(),
                    isVerbFormSuggested
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 11. 행동 수정 이력 조회 — GET /learning-facade/actions/{actionId}/revisions
    // ──────────────────────────────────────────────────────

    public record RevisionItem(
            Long revisionId,
            String previousDescription,
            String newDescription,
            String revisionReasonLabel,     // nullable
            LocalDateTime revisedAt
    ) {
        public static RevisionItem of(ActionRevision revision) {
            return new RevisionItem(
                    revision.getId(),
                    revision.getPreviousDescription(),
                    revision.getNewDescription(),
                    revision.getRevisionReasonLabel(),
                    revision.getRevisedAt()
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 12. 수정 이유 선택지 목록 — GET /learning-facade/revision-reason-options
    // ──────────────────────────────────────────────────────

    public record RevisionReasonOptionItem(
            Long id,
            String label
    ) {
        public static RevisionReasonOptionItem of(RevisionReasonOption option) {
            return new RevisionReasonOptionItem(option.getId(), option.getLabel());
        }
    }
}