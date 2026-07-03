package com.example.thirdtool.LearningFacade.presentation.dto;

import com.example.thirdtool.LearningFacade.application.port.out.suggestion.ChapterOutlineItem;

import java.util.List;

/**
 * 6-Port Suggestion REST Response DTO (Story-AS-E5-S5-3 부분).
 */
public final class SuggestionResponse {

    private SuggestionResponse() {}

    public record ChapterOutlineItemResponse(String title, String rationale) {
        public static ChapterOutlineItemResponse of(ChapterOutlineItem item) {
            return new ChapterOutlineItemResponse(item.title(), item.rationale());
        }
    }

    public record ChaptersOutlineResponseBody(
            List<ChapterOutlineItemResponse> chapters,
            String providerContext,
            boolean suggestionsAvailable
    ) {}

    public record ChapterSubtreeResponseBody(
            String bodyAsciiTree,
            String providerContext,
            boolean suggestionsAvailable
    ) {}

    public record SelectionOutlineResponseBody(
            String nameCandidate,
            List<ChapterOutlineItemResponse> chapters,
            String providerContext,
            boolean suggestionsAvailable
    ) {}
}
