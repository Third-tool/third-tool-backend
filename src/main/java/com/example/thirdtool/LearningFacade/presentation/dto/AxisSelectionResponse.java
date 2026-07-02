package com.example.thirdtool.LearningFacade.presentation.dto;

import com.example.thirdtool.LearningFacade.domain.model.AxisSelection;
import com.example.thirdtool.LearningFacade.domain.model.AxisSelectionNode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Selection 컨테이너 + 자식 노드 Response DTO (Story-LT-E3-S3-11).
 */
public final class AxisSelectionResponse {

    private AxisSelectionResponse() {}

    // ─── 컨테이너 ─────────────────────────

    public record SelectionDetail(
            Long selectionId,
            Long axisId,
            String name,
            LocalDateTime createdAt
    ) {
        public static SelectionDetail of(AxisSelection selection) {
            return new SelectionDetail(
                    selection.getId(),
                    selection.getAxis() != null ? selection.getAxis().getId() : null,
                    selection.getName(),
                    selection.getCreatedAt());
        }
    }

    public record SelectionList(List<SelectionDetail> selections) {
        public static SelectionList of(List<AxisSelection> selections) {
            return new SelectionList(selections.stream().map(SelectionDetail::of).toList());
        }
    }

    // ─── 자식 노드 ────────────────────────

    public record NodeDetail(
            Long nodeId,
            Long selectionId,
            int displayOrder,
            String title,
            String rationale,
            String body,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static NodeDetail of(AxisSelectionNode node) {
            return new NodeDetail(
                    node.getId(),
                    node.getSelection() != null ? node.getSelection().getId() : null,
                    node.getDisplayOrder(),
                    node.getTitle(),
                    node.getRationale(),
                    node.getBody(),
                    node.getCreatedAt(),
                    node.getUpdatedAt());
        }
    }

    public record NodeList(List<NodeDetail> nodes) {
        public static NodeList of(List<AxisSelectionNode> nodes) {
            return new NodeList(nodes.stream().map(NodeDetail::of).toList());
        }
    }

    public record Reordered(List<NodeDetail> nodes) {
        public static Reordered of(List<AxisSelectionNode> nodes) {
            return new Reordered(nodes.stream().map(NodeDetail::of).toList());
        }
    }
}
