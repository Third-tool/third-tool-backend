package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import java.util.List;
import java.util.Objects;

/**
 * AI 제안 호출의 결과를 표현하는 VO.
 *
 * <p>정상 호출이면 {@link #available(List)}로 결과 목록을 감싸고,
 * AI 호출 실패(타임아웃 / 파싱 실패 / 인증 실패)로 Fallback이 발동하면
 * {@link #unavailable()}로 빈 목록 + {@code suggestionsAvailable=false}를 반환한다.</p>
 *
 * <p>{@link SuggestionFallbacks}가 본 VO를 생성한다 — 직접 생성자 호출은 권장되지 않는다.</p>
 *
 * @param <T> 제안 원소 타입(AxisSuggestion / AxisTopicSuggestion 등).
 */
public record SuggestionFallbackResult<T>(List<T> suggestions, boolean suggestionsAvailable) {

    public SuggestionFallbackResult {
        Objects.requireNonNull(suggestions, "suggestions는 null일 수 없습니다.");
        suggestions = List.copyOf(suggestions);
    }

    public static <T> SuggestionFallbackResult<T> available(List<T> items) {
        Objects.requireNonNull(items, "items는 null일 수 없습니다.");
        return new SuggestionFallbackResult<>(items, true);
    }

    public static <T> SuggestionFallbackResult<T> unavailable() {
        return new SuggestionFallbackResult<>(List.of(), false);
    }
}
