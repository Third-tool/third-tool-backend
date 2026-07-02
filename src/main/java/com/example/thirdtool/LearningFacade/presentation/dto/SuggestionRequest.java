package com.example.thirdtool.LearningFacade.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 6-Port Suggestion REST Request DTO (Story-AS-E5-S5-3 부분).
 *
 * <p>M3에는 4개 신규 outline/subtree 엔드포인트만 노출. Layer/Axis는 SuggestionConceptContext가
 * compositionReason/desiredOutcome을 요구하므로 별도 브랜치에서 UX 확정 후 노출.
 */
public final class SuggestionRequest {

    private SuggestionRequest() {}

    public record ChaptersOutlineRequest(
            @NotEmpty(message = "concepts는 최소 1개 이상이어야 합니다.")
            @Size(max = 5)
            List<@NotBlank String> concepts,

            @NotBlank String layerName,
            @NotBlank String axisName,
            String axisReason,
            Integer chapterCountHint,
            String freeformHint
    ) {}

    public record ChapterOutlineItemDto(
            @NotBlank String title,
            String rationale
    ) {}

    public record ChapterSubtreeRequest(
            @NotEmpty @Size(max = 5) List<@NotBlank String> concepts,
            @NotBlank String layerName,
            @NotBlank String axisName,
            @NotNull @Valid ChapterOutlineItemDto chapter,
            List<@Valid ChapterOutlineItemDto> siblingChapters
    ) {}

    public record SelectionOutlineRequest(
            @NotEmpty @Size(max = 5) List<@NotBlank String> concepts,
            @NotBlank String layerName,
            @NotBlank String axisName,
            String roadmapContent,
            String variantHint,
            Integer chapterCountHint
    ) {}

    public record SelectionSubtreeRequest(
            @NotEmpty @Size(max = 5) List<@NotBlank String> concepts,
            @NotBlank String layerName,
            @NotBlank String axisName,
            @NotNull @Valid ChapterOutlineItemDto chapter,
            @NotBlank String selectionName,
            List<@Valid ChapterOutlineItemDto> selectionSiblings
    ) {}
}
