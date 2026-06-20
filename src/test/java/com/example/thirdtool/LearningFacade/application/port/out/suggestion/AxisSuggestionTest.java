package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AxisSuggestion")
class AxisSuggestionTest {

    @Nested
    @DisplayName("해피")
    class Happy {

        @Test
        @DisplayName("description + rationale 정상 입력")
        void create_valid() {
            AxisSuggestion suggestion = new AxisSuggestion("아키텍처", "구조적 판단을 정련");

            assertThat(suggestion.description()).isEqualTo("아키텍처");
            assertThat(suggestion.rationale()).isEqualTo("구조적 판단을 정련");
        }

        @Test
        @DisplayName("rationale이 null이어도 생성된다")
        void create_rationale_null_허용() {
            AxisSuggestion suggestion = new AxisSuggestion("아키텍처", null);

            assertThat(suggestion.description()).isEqualTo("아키텍처");
            assertThat(suggestion.rationale()).isNull();
        }
    }

    @Nested
    @DisplayName("엣지")
    class Edge {

        @Test
        @DisplayName("description은 trim되어 저장된다")
        void description_trim() {
            AxisSuggestion suggestion = new AxisSuggestion("  아키텍처  ", null);

            assertThat(suggestion.description()).isEqualTo("아키텍처");
        }

        @Test
        @DisplayName("rationale이 null이 아니면 trim되어 저장된다")
        void rationale_trim() {
            AxisSuggestion suggestion = new AxisSuggestion("아키텍처", "  근거  ");

            assertThat(suggestion.rationale()).isEqualTo("근거");
        }

        @Test
        @DisplayName("rationale이 빈 문자열이면 null로 정규화된다")
        void rationale_empty_string_null로_정규화() {
            AxisSuggestion suggestion = new AxisSuggestion("아키텍처", "");

            assertThat(suggestion.rationale()).isNull();
        }

        @Test
        @DisplayName("rationale이 공백만으로 구성되면 null로 정규화된다")
        void rationale_blank_null로_정규화() {
            AxisSuggestion suggestion = new AxisSuggestion("아키텍처", "   \t\n  ");

            assertThat(suggestion.rationale()).isNull();
        }
    }

    @Nested
    @DisplayName("예외")
    class Exception {

        @Test
        @DisplayName("description이 null이면 IAE")
        void create_description_null_예외() {
            assertThatThrownBy(() -> new AxisSuggestion(null, "rationale"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("description이 blank이면 IAE")
        void create_description_blank_예외() {
            assertThatThrownBy(() -> new AxisSuggestion("   ", "rationale"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
