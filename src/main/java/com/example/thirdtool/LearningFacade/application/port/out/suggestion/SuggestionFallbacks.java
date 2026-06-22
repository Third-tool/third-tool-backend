package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * AI 제안 호출에서 발생하는 사전 정의된 실패만 catch하여 빈 결과로 변환하는 Fallback 유틸 (ADR010).
 *
 * <p>대상 ErrorCode 3종(타임아웃 / 응답 형식 오류 / 인증 실패)에 대해서만 fallback을 적용한다.
 * 그 외 ErrorCode는 호출자가 처리하도록 원본 예외를 그대로 전파한다 — fail-fast.</p>
 */
public final class SuggestionFallbacks {

    private static final Logger log = LoggerFactory.getLogger(SuggestionFallbacks.class);

    private static final String MDC_KEY = "suggestionErrorCode";

    private static final Set<ErrorCode> SUGGESTION_FAILURE_CODES = EnumSet.of(
            ErrorCode.LEARNING_FACADE_SUGGESTION_TIMEOUT,
            ErrorCode.LEARNING_FACADE_SUGGESTION_INVALID_RESPONSE,
            ErrorCode.LEARNING_FACADE_SUGGESTION_AUTH_FAILED
    );

    private SuggestionFallbacks() {
    }

    public static <T> SuggestionFallbackResult<T> protect(Supplier<List<T>> portCall) {
        Objects.requireNonNull(portCall, "portCall은 null일 수 없습니다.");
        try {
            return SuggestionFallbackResult.available(portCall.get());
        } catch (LearningFacadeDomainException ex) {
            ErrorCode code = ex.getErrorCode();
            if (!SUGGESTION_FAILURE_CODES.contains(code)) {
                throw ex;
            }
            try {
                MDC.put(MDC_KEY, code.getCode());
                log.warn("AI 제안 호출 실패 — fallback 활성화: code={} message={}", code.getCode(), ex.getMessage());
            } finally {
                MDC.remove(MDC_KEY);
            }
            return SuggestionFallbackResult.unavailable();
        }
    }
}
