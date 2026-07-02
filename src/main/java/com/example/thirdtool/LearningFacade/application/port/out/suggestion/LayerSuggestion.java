package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

/**
 * AI 제안 Layer 후보 (Story-AS-E1).
 *
 * <p>{@code name}은 사용자에게 노출될 Layer 이름(필수, trim 후 저장).
 * {@code rationale}은 "왜 이 Layer를 제안했는지" 설명(선택 — LLM이 안정적으로 생성한다는 보장이 없어 null 허용).</p>
 */
public record LayerSuggestion(String name, String rationale) {

    public LayerSuggestion {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name은 blank일 수 없습니다.");
        }
        name = name.trim();
        if (rationale != null) {
            String trimmed = rationale.trim();
            rationale = trimmed.isEmpty() ? null : trimmed;
        }
    }
}
