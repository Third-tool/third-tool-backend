package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SuggestionFallbacks")
class SuggestionFallbacksTest {

    @Nested
    @DisplayName("ErrorCode 계약 회귀 안전망")
    class ErrorCodeContract {

        @Test
        @DisplayName("LEARNING_FACADE_SUGGESTION_TIMEOUT의 code는 LF_SUGGEST_001")
        void LEARNING_FACADE_SUGGESTION_TIMEOUT_code_LF_SUGGEST_001() {
            assertThat(ErrorCode.LEARNING_FACADE_SUGGESTION_TIMEOUT.getCode())
                    .isEqualTo("LF_SUGGEST_001");
        }

        @Test
        @DisplayName("LEARNING_FACADE_SUGGESTION_INVALID_RESPONSE의 code는 LF_SUGGEST_002")
        void LEARNING_FACADE_SUGGESTION_INVALID_RESPONSE_code_LF_SUGGEST_002() {
            assertThat(ErrorCode.LEARNING_FACADE_SUGGESTION_INVALID_RESPONSE.getCode())
                    .isEqualTo("LF_SUGGEST_002");
        }

        @Test
        @DisplayName("LEARNING_FACADE_SUGGESTION_AUTH_FAILED의 code는 LF_SUGGEST_003")
        void LEARNING_FACADE_SUGGESTION_AUTH_FAILED_code_LF_SUGGEST_003() {
            assertThat(ErrorCode.LEARNING_FACADE_SUGGESTION_AUTH_FAILED.getCode())
                    .isEqualTo("LF_SUGGEST_003");
        }
    }

    @Nested
    @DisplayName("해피")
    class Happy {

        @Test
        @DisplayName("정상 Supplier → available + 결과 목록 보존")
        void protect_정상_available() {
            List<AxisSuggestion> items = List.of(
                    new AxisSuggestion("기초", null),
                    new AxisSuggestion("심화", "근거")
            );

            SuggestionFallbackResult<AxisSuggestion> result = SuggestionFallbacks.protect(() -> items);

            assertThat(result.suggestionsAvailable()).isTrue();
            assertThat(result.suggestions()).extracting(AxisSuggestion::description)
                    .containsExactly("기초", "심화");
        }
    }

    @Nested
    @DisplayName("엣지")
    class Edge {

        @Test
        @DisplayName("Supplier가 빈 목록을 반환해도 available=true")
        void protect_빈목록_available() {
            SuggestionFallbackResult<AxisSuggestion> result = SuggestionFallbacks.protect(List::of);

            assertThat(result.suggestionsAvailable()).isTrue();
            assertThat(result.suggestions()).isEmpty();
        }

        @Test
        @DisplayName("LF_SUGGEST_001(타임아웃) 예외 → unavailable + 빈 목록")
        void protect_LF_SUGGEST_001_unavailable() {
            SuggestionFallbackResult<AxisSuggestion> result = SuggestionFallbacks.protect(() -> {
                throw LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_SUGGESTION_TIMEOUT);
            });

            assertThat(result.suggestionsAvailable()).isFalse();
            assertThat(result.suggestions()).isEmpty();
        }

        @Test
        @DisplayName("LF_SUGGEST_002(응답 형식 오류) 예외 → unavailable + 빈 목록")
        void protect_LF_SUGGEST_002_unavailable() {
            SuggestionFallbackResult<AxisSuggestion> result = SuggestionFallbacks.protect(() -> {
                throw LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_SUGGESTION_INVALID_RESPONSE);
            });

            assertThat(result.suggestionsAvailable()).isFalse();
            assertThat(result.suggestions()).isEmpty();
        }

        @Test
        @DisplayName("LF_SUGGEST_003(인증 실패) 예외 → unavailable + 빈 목록")
        void protect_LF_SUGGEST_003_unavailable() {
            SuggestionFallbackResult<AxisSuggestion> result = SuggestionFallbacks.protect(() -> {
                throw LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_SUGGESTION_AUTH_FAILED);
            });

            assertThat(result.suggestionsAvailable()).isFalse();
            assertThat(result.suggestions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("예외")
    class Exception {

        @Test
        @DisplayName("Suggestion 외 ErrorCode 예외는 그대로 전파된다")
        void protect_다른ErrorCode_전파() {
            assertThatThrownBy(() -> SuggestionFallbacks.protect(() -> {
                throw LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_NOT_FOUND);
            }))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting(ex -> ((LearningFacadeDomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.LEARNING_FACADE_NOT_FOUND);
        }

        @Test
        @DisplayName("일반 RuntimeException은 catch하지 않고 전파된다")
        void protect_RuntimeException_전파() {
            assertThatThrownBy(() -> SuggestionFallbacks.protect(() -> {
                throw new IllegalStateException("boom");
            }))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("boom");
        }

        @Test
        @DisplayName("portCall이 null이면 NPE")
        void protect_null_예외() {
            assertThatThrownBy(() -> SuggestionFallbacks.protect(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
