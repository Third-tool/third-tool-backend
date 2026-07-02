package com.example.thirdtool.LearningFacade.application.dto;

import java.util.List;

/**
 * Roadmap 노드 Command DTO (Story-LT-E3-S3-8).
 * 사용자 요청을 Application Service로 전달하는 record 셋트.
 */
public final class AxisRoadmapNodeCommand {

    private AxisRoadmapNodeCommand() {}

    /** POST /api/v1/axes/{axisId}/roadmap-nodes */
    public record Add(
            Long userId,
            Long axisId,
            String title,
            String rationale,
            String body
    ) {}

    /**
     * PATCH /api/v1/roadmap-nodes/{nodeId} — 부분 필드 업데이트.
     * 각 필드는 "미제공(null)"과 "빈문자열/blank"를 구분해서 처리:
     *   · title/body — null이면 미제공(no-op), blank이면 예외
     *   · rationale — null이면 미제공, blank이면 null로 정규화
     */
    public record Update(
            Long userId,
            Long nodeId,
            String title,       // null = 미제공
            String rationale,   // null = 미제공, blank = null로 정규화
            String body,        // null = 미제공
            boolean titlePresent,
            boolean rationalePresent,
            boolean bodyPresent
    ) {}

    /** DELETE /api/v1/roadmap-nodes/{nodeId} */
    public record Remove(Long userId, Long nodeId) {}

    /** PUT /api/v1/axes/{axisId}/roadmap-nodes/order */
    public record Reorder(Long userId, Long axisId, List<Long> orderedNodeIds) {}
}
