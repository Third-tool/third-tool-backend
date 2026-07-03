package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

/**
 * 챕터 subtree 응답 (Story-AS-E1-S1-6).
 * {@code bodyAsciiTree}: 챕터 subtree 통짜 (예: "├── 1-1. 정의와 본질\n│       ...").
 * body는 v1은 파싱 없이 그대로 AxisRoadmapNode.body에 저장 (이슈 #17).
 */
public record ChapterSubtreeResponse(
        String bodyAsciiTree,
        String providerContext,
        boolean suggestionsAvailable
) {

    public ChapterSubtreeResponse {
        if (providerContext == null || providerContext.isBlank()) {
            throw new IllegalArgumentException("providerContext는 blank일 수 없습니다.");
        }
        providerContext = providerContext.trim();
        // bodyAsciiTree는 unavailable 시 null 허용.
    }

    public static ChapterSubtreeResponse unavailable(String providerContext) {
        return new ChapterSubtreeResponse(null, providerContext, false);
    }
}
