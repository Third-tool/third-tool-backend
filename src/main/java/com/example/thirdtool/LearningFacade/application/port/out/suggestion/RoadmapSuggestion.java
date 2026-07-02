package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import java.util.List;

/**
 * AI 제안 Axis Roadmap (Story-AS-E1).
 *
 * <p>ADR023(예정) 용어 기준 Roadmap = 헌법. Axis 안에서의 학습 순서 청사진 (챕터 순서).
 * <p>{@code outline}은 챕터 title 순서 리스트 (최소 1개).
 * {@code rationale}은 이 로드맵을 제안한 이유(선택 — LLM 안정 생성 보장 없어 null 허용).</p>
 */
public record RoadmapSuggestion(List<String> outline, String rationale) {

    public RoadmapSuggestion {
        if (outline == null || outline.isEmpty()) {
            throw new IllegalArgumentException("outline은 최소 1개 이상이어야 합니다.");
        }
        outline = outline.stream()
                .map(value -> {
                    if (value == null || value.isBlank()) {
                        throw new IllegalArgumentException("outline의 각 원소는 blank일 수 없습니다.");
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
