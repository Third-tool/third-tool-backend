package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import java.util.List;

/**
 * 챕터 subtree 생성 요청 (Story-AS-E1-S1-6).
 *
 * <p>{@code chapter}: 이번에 subtree를 생성할 챕터 (title/rationale).
 * {@code siblingChapters}: 같은 axis 안의 다른 챕터들 (LLM이 중복 방지 · 흐름 참고용).
 */
public record ChapterSubtreeRequest(
        List<String> concepts,
        String layerName,
        String axisName,
        ChapterOutlineItem chapter,
        List<ChapterOutlineItem> siblingChapters
) {

    public ChapterSubtreeRequest {
        if (concepts == null || concepts.isEmpty()) {
            throw new IllegalArgumentException("concepts는 최소 1개 이상이어야 합니다.");
        }
        concepts = concepts.stream()
                .map(v -> {
                    if (v == null || v.isBlank()) {
                        throw new IllegalArgumentException("concepts의 각 원소는 blank일 수 없습니다.");
                    }
                    return v.trim();
                })
                .toList();
        if (layerName == null || layerName.isBlank()) {
            throw new IllegalArgumentException("layerName은 blank일 수 없습니다.");
        }
        layerName = layerName.trim();
        if (axisName == null || axisName.isBlank()) {
            throw new IllegalArgumentException("axisName은 blank일 수 없습니다.");
        }
        axisName = axisName.trim();
        if (chapter == null) {
            throw new IllegalArgumentException("chapter는 null일 수 없습니다.");
        }
        siblingChapters = siblingChapters == null ? List.of() : List.copyOf(siblingChapters);
    }
}
