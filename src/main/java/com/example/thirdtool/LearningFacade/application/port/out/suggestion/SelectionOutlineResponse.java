package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import java.util.List;

/**
 * Selection outline 응답 (Story-AS-E1-S1-7).
 * {@code nameCandidate}: 컨테이너 이름 후보 (예: "능 아키텍처 selections v2").
 * {@code chapters}: 컨테이너 하위 챕터 outline.
 */
public record SelectionOutlineResponse(
        String nameCandidate,
        List<ChapterOutlineItem> chapters,
        String providerContext,
        boolean suggestionsAvailable
) {

    public SelectionOutlineResponse {
        if (providerContext == null || providerContext.isBlank()) {
            throw new IllegalArgumentException("providerContext는 blank일 수 없습니다.");
        }
        providerContext = providerContext.trim();
        chapters = chapters == null ? List.of() : List.copyOf(chapters);
        // nameCandidate는 unavailable 시 null 허용.
    }

    public static SelectionOutlineResponse unavailable(String providerContext) {
        return new SelectionOutlineResponse(null, List.of(), providerContext, false);
    }
}
