package com.example.thirdtool.LearningFacade.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Roadmap 노드 Request DTO 셋트 (Story-LT-E3-S3-8).
 * 검증 어노테이션은 여기서만 (도메인 검증은 별도 · 이중 방어).
 */
public final class AxisRoadmapNodeRequest {

    private AxisRoadmapNodeRequest() {}

    public record Add(
            @NotBlank(message = "title은 비어 있을 수 없습니다.")
            @Size(max = 200, message = "title은 200자 이내여야 합니다.")
            String title,

            @Size(max = 500, message = "rationale은 500자 이내여야 합니다.")
            String rationale,

            @NotBlank(message = "body는 비어 있을 수 없습니다.")
            String body
    ) {}

    /**
     * PATCH — 필드 미제공(null)과 blank 구분.
     * rationale은 blank 전달 시 null로 정규화됨.
     */
    public record Update(
            @Size(max = 200)
            String title,

            @Size(max = 500)
            String rationale,

            String body
    ) {}

    public record Reorder(
            @NotEmpty
            List<@NotNull Long> orderedNodeIds
    ) {}
}
