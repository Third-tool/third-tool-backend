package com.example.thirdtool.LearningFacade.application.dto;

import com.example.thirdtool.LearningFacade.application.port.out.suggestion.ChapterOutlineItem;

import java.util.List;

/**
 * Suggestion Application-layer Command records (Story-AS-E5-S5-3).
 * REST Request DTO(presentation)와 분리해 계층 의존 방향 유지.
 */
public final class SuggestionCommand {

    private SuggestionCommand() {}

    public record ChaptersOutline(
            List<String> concepts,
            String layerName,
            String axisName,
            String axisReason,
            Integer chapterCountHint,
            String freeformHint
    ) {}

    public record ChapterSubtree(
            List<String> concepts,
            String layerName,
            String axisName,
            ChapterOutlineItem chapter,
            List<ChapterOutlineItem> siblingChapters
    ) {}

    public record SelectionOutline(
            List<String> concepts,
            String layerName,
            String axisName,
            String roadmapContent,
            String variantHint,
            Integer chapterCountHint
    ) {}

    public record SelectionSubtree(
            List<String> concepts,
            String layerName,
            String axisName,
            ChapterOutlineItem chapter,
            String selectionName,
            List<ChapterOutlineItem> selectionSiblings
    ) {}
}
