package com.example.thirdtool.LearningFacade.application.dto;

import java.util.List;

/**
 * Selection 컨테이너 + 자식 노드 Command DTO (Story-LT-E3-S3-11).
 */
public final class AxisSelectionCommand {

    private AxisSelectionCommand() {}

    // ─── 컨테이너 ───────────────────────────
    public record AddSelection(Long userId, Long axisId, String name) {}
    public record RenameSelection(Long userId, Long selectionId, String name) {}
    public record RemoveSelection(Long userId, Long axisId, Long selectionId) {}

    // ─── 자식 노드 ──────────────────────────
    public record AddNode(
            Long userId,
            Long selectionId,
            String title,
            String rationale,
            String body
    ) {}

    public record UpdateNode(
            Long userId,
            Long nodeId,
            String title,
            String rationale,
            String body,
            boolean titlePresent,
            boolean rationalePresent,
            boolean bodyPresent
    ) {}

    public record RemoveNode(Long userId, Long nodeId) {}

    public record ReorderNodes(Long userId, Long selectionId, List<Long> orderedNodeIds) {}
}
