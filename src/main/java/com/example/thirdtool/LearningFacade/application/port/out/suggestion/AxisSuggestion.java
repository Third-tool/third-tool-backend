package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

/**
 * AI 제안 축 후보.
 *
 * <p>{@code description}은 사용자에게 노출될 축 이름(필수).
 * {@code rationale}은 "왜 이 축을 제안했는지" 설명(선택 — LLM이 안정적으로 생성한다는 보장이 없어 null 허용).</p>
 */
public record AxisSuggestion(String description, String rationale) {

    public AxisSuggestion {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description은 blank일 수 없습니다.");
        }
        description = description.trim();
        if (rationale != null) {
            String trimmed = rationale.trim();
            rationale = trimmed.isEmpty() ? null : trimmed;
        }
    }
}
