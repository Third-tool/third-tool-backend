package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

/**
 * AI 제안 하위 주제 후보.
 *
 * <p>{@code description}은 사용자에게 노출될 주제 이름(필수).
 * {@code rationale}은 "왜 이 주제를 제안했는지" 설명(선택 — null 허용).</p>
 */
public record AxisTopicSuggestion(String description, String rationale) {

    public AxisTopicSuggestion {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description은 blank일 수 없습니다.");
        }
        description = description.trim();
        if (rationale != null) {
            rationale = rationale.trim();
        }
    }
}
