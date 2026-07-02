package com.example.thirdtool.LearningFacade.presentation.dto;

import com.example.thirdtool.LearningFacade.domain.model.AxisRoadmapNode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Roadmap 노드 Response DTO (Story-LT-E3-S3-8).
 */
public final class AxisRoadmapNodeResponse {

    private AxisRoadmapNodeResponse() {}

    public record NodeDetail(
            Long nodeId,
            Long axisId,
            int displayOrder,
            String title,
            String rationale,
            String body,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static NodeDetail of(AxisRoadmapNode node) {
            return new NodeDetail(
                    node.getId(),
                    node.getAxis() != null ? node.getAxis().getId() : null,
                    node.getDisplayOrder(),
                    node.getTitle(),
                    node.getRationale(),
                    node.getBody(),
                    node.getCreatedAt(),
                    node.getUpdatedAt());
        }
    }

    public record NodeList(List<NodeDetail> nodes) {
        public static NodeList of(List<AxisRoadmapNode> nodes) {
            return new NodeList(nodes.stream().map(NodeDetail::of).toList());
        }
    }

    public record Reordered(List<NodeDetail> nodes) {
        public static Reordered of(List<AxisRoadmapNode> nodes) {
            return new Reordered(nodes.stream().map(NodeDetail::of).toList());
        }
    }
}
