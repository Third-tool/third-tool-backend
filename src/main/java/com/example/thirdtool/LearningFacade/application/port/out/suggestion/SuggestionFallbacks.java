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
 *
 * <p><b>GlobalExceptionHandler 단일 진입점 원칙(conventions.md §2.3)에 대한 의도된 예외</b>:
 * 일반적으로 BusinessException 변환은 GlobalExceptionHandler가 단독 책임이지만, AI 호출 실패는
 * "사용자에게 5xx 미노출 + 부가 정보 영역만 비움"이라는 정책(ADR010)을 따르므로 Application Service
 * 계층에서 선제 catch한다. 본 유틸이 그 단일 변환점이며, 후속 Service들이 동일 정책을 분산 구현하지
 * 않도록 설계되었다. GlobalExceptionHandler는 Suggestion 외 모든 예외의 진입점으로 유지된다.</p>
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
