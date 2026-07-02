package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import java.util.List;

/**
 * 챕터 outline 응답 (Story-AS-E1-S1-5).
 *
 * <p>{@code chapters}: 챕터 title/rationale 리스트 (body 미포함).
 * {@code providerContext}: 응답 출처 (예: "static:backend-developer" · "llm:vertex-gemini-2.5-flash").
 * {@code suggestionsAvailable}: LLM 실패·인증 실패 등으로 사용 가능한 초안이 없으면 false (ADR010 fallback).
 */
public record ChaptersOutlineResponse(
        List<ChapterOutlineItem> chapters,
        String providerContext,
        boolean suggestionsAvailable
) {

    public ChaptersOutlineResponse {
        chapters = chapters == null ? List.of() : List.copyOf(chapters);
        if (providerContext == null || providerContext.isBlank()) {
            throw new IllegalArgumentException("providerContext는 blank일 수 없습니다.");
        }
        providerContext = providerContext.trim();
    }

    public static ChaptersOutlineResponse unavailable(String providerContext) {
        return new ChaptersOutlineResponse(List.of(), providerContext, false);
    }
}
