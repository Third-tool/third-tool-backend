package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SuggestionFallbackResult")
class SuggestionFallbackResultTest {

    @Nested
    @DisplayName("해피")
    class Happy {

        @Test
        @DisplayName("available로 감싸면 suggestionsAvailable=true + 목록 보존")
        void available_정상() {
            List<AxisSuggestion> items = List.of(
                    new AxisSuggestion("기초", null),
                    new AxisSuggestion("심화", "근거")
            );

            SuggestionFallbackResult<AxisSuggestion> result = SuggestionFallbackResult.available(items);

            assertThat(result.suggestions()).hasSize(2);
            assertThat(result.suggestions()).extracting(AxisSuggestion::description)
                    .containsExactly("기초", "심화");
            assertThat(result.suggestionsAvailable()).isTrue();
        }

        @Test
        @DisplayName("unavailable은 빈 목록 + suggestionsAvailable=false")
        void unavailable_정상() {
            SuggestionFallbackResult<AxisSuggestion> result = SuggestionFallbackResult.unavailable();

            assertThat(result.suggestions()).isEmpty();
            assertThat(result.suggestionsAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("엣지")
    class Edge {

        @Test
        @DisplayName("available 입력이 빈 목록이면 suggestionsAvailable=true + 빈 목록")
        void available_빈목록_정상() {
            SuggestionFallbackResult<AxisSuggestion> result = SuggestionFallbackResult.available(List.of());

            assertThat(result.suggestions()).isEmpty();
            assertThat(result.suggestionsAvailable()).isTrue();
        }

        @Test
        @DisplayName("available 입력 리스트를 외부에서 변경해도 결과에 반영되지 않는다 (방어 복사)")
        void available_방어복사() {
            List<AxisSuggestion> mutable = new ArrayList<>();
            mutable.add(new AxisSuggestion("기초", null));

            SuggestionFallbackResult<AxisSuggestion> result = SuggestionFallbackResult.available(mutable);
            mutable.add(new AxisSuggestion("이후에추가", null));

            assertThat(result.suggestions()).hasSize(1);
            assertThat(result.suggestions().get(0).description()).isEqualTo("기초");
        }

        @Test
        @DisplayName("결과의 suggestions는 불변(읽기 전용)이다")
        void suggestions_불변() {
            SuggestionFallbackResult<AxisSuggestion> result =
                    SuggestionFallbackResult.available(List.of(new AxisSuggestion("기초", null)));

            assertThatThrownBy(() -> result.suggestions().add(new AxisSuggestion("추가", null)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("예외")
    class Exception {

        @Test
        @DisplayName("available에 null 입력 시 NPE")
        void available_null_예외() {
            assertThatThrownBy(() -> SuggestionFallbackResult.available(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
