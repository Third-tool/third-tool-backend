package com.example.thirdtool.LearningFacade.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Selection 컨테이너 + 자식 노드 Request DTO (Story-LT-E3-S3-11).
 */
public final class AxisSelectionRequest {

    private AxisSelectionRequest() {}

    // ─── 컨테이너 ─────────────────────────
    public record AddSelection(
            @NotBlank(message = "Selection 이름은 비어 있을 수 없습니다.")
            @Size(max = 100)
            String name
    ) {}

    public record RenameSelection(
            @NotBlank
            @Size(max = 100)
            String name
    ) {}

    // ─── 자식 노드 ────────────────────────
    public record AddNode(
            @NotBlank(message = "title은 비어 있을 수 없습니다.")
            @Size(max = 200)
            String title,

            @Size(max = 500)
            String rationale,

            @NotBlank(message = "body는 비어 있을 수 없습니다.")
            String body
    ) {}

    public record UpdateNode(
            @Size(max = 200) String title,
            @Size(max = 500) String rationale,
            String body
    ) {}

    public record ReorderNodes(
            @NotEmpty List<@NotNull Long> orderedNodeIds
    ) {}
}
