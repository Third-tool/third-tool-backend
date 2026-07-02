package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import java.util.List;

/**
 * AI 제안 Axis Selections (Story-AS-E1).
 *
 * <p>ADR023(예정) 용어 기준 Selection = 판례. Axis 학습 과정에서 활용할 구체 사례·문제·응용 리스트.
 * <p>{@code selections}는 사례 리스트 (최소 1개).
 * {@code rationale}은 판례 선택 이유(선택 — LLM 안정 생성 보장 없어 null 허용).</p>
 */
public record SelectionsSuggestion(List<String> selections, String rationale) {

    public SelectionsSuggestion {
        if (selections == null || selections.isEmpty()) {
            throw new IllegalArgumentException("selections는 최소 1개 이상이어야 합니다.");
        }
        selections = selections.stream()
                .map(value -> {
                    if (value == null || value.isBlank()) {
                        throw new IllegalArgumentException("selections의 각 원소는 blank일 수 없습니다.");
                    }
                    return value.trim();
                })
                .toList();
        if (rationale != null) {
            String trimmed = rationale.trim();
            rationale = trimmed.isEmpty() ? null : trimmed;
        }
    }
}
