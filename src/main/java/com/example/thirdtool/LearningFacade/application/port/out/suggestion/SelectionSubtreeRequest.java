package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import java.util.List;

/**
 * Selection 챕터 subtree 생성 요청 (Story-AS-E1-S1-8).
 * Roadmap의 ChapterSubtree와 시그니처 유사하되 selectionName·selectionSiblings 추가.
 */
public record SelectionSubtreeRequest(
        List<String> concepts,
        String layerName,
        String axisName,
        ChapterOutlineItem chapter,
        String selectionName,
        List<ChapterOutlineItem> selectionSiblings
) {

    public SelectionSubtreeRequest {
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
        if (selectionName == null || selectionName.isBlank()) {
            throw new IllegalArgumentException("selectionName은 blank일 수 없습니다.");
        }
        selectionName = selectionName.trim();
        selectionSiblings = selectionSiblings == null ? List.of() : List.copyOf(selectionSiblings);
    }
}
