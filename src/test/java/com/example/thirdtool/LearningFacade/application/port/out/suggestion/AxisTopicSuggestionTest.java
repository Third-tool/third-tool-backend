package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AxisTopicSuggestion")
class AxisTopicSuggestionTest {

    @Nested
    @DisplayName("해피")
    class Happy {

        @Test
        @DisplayName("description + rationale 정상 입력")
        void create_valid() {
            AxisTopicSuggestion suggestion = new AxisTopicSuggestion("도메인 모델링", "Aggregate 경계 설정");

            assertThat(suggestion.description()).isEqualTo("도메인 모델링");
            assertThat(suggestion.rationale()).isEqualTo("Aggregate 경계 설정");
        }

        @Test
        @DisplayName("rationale이 null이어도 생성된다")
        void create_rationale_null_허용() {
            AxisTopicSuggestion suggestion = new AxisTopicSuggestion("도메인 모델링", null);

            assertThat(suggestion.description()).isEqualTo("도메인 모델링");
            assertThat(suggestion.rationale()).isNull();
        }
    }

    @Nested
    @DisplayName("엣지")
    class Edge {

        @Test
        @DisplayName("description은 trim되어 저장된다")
        void description_trim() {
            AxisTopicSuggestion suggestion = new AxisTopicSuggestion("  도메인 모델링  ", null);

            assertThat(suggestion.description()).isEqualTo("도메인 모델링");
        }

        @Test
        @DisplayName("rationale이 null이 아니면 trim되어 저장된다")
        void rationale_trim() {
            AxisTopicSuggestion suggestion = new AxisTopicSuggestion("도메인 모델링", "  근거  ");

            assertThat(suggestion.rationale()).isEqualTo("근거");
        }
    }

    @Nested
    @DisplayName("예외")
    class Exception {

        @Test
        @DisplayName("description이 null이면 IAE")
        void create_description_null_예외() {
            assertThatThrownBy(() -> new AxisTopicSuggestion(null, "rationale"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("description이 blank이면 IAE")
        void create_description_blank_예외() {
            assertThatThrownBy(() -> new AxisTopicSuggestion("   ", "rationale"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
