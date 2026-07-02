package com.example.thirdtool.LearningFacade.application.dto;

import com.example.thirdtool.LearningFacade.application.port.out.suggestion.ChapterOutlineItem;

import java.util.List;

/**
 * Suggestion Application-layer Result records (Story-AS-E5-S5-3).
 *
 * <p>Presentation Response DTO와 분리 — application → presentation 역방향 의존 방지 (PACKAGE.md §3).
 * Controller가 Result → Response 매핑을 담당.
 */
public final class SuggestionResult {

    private SuggestionResult() {}

    public record ChaptersOutline(
            List<ChapterOutlineItem> chapters,
            String providerContext,
            boolean suggestionsAvailable
    ) {}

    public record ChapterSubtree(
            String bodyAsciiTree,
            String providerContext,
            boolean suggestionsAvailable
    ) {}

    public record SelectionOutline(
            String nameCandidate,
            List<ChapterOutlineItem> chapters,
            String providerContext,
            boolean suggestionsAvailable
    ) {}
}
