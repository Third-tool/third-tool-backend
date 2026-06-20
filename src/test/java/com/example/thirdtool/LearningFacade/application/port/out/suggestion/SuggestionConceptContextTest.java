package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SuggestionConceptContext")
class SuggestionConceptContextTest {

    @Nested
    @DisplayName("해피")
    class Happy {

        @Test
        @DisplayName("정상 입력은 그대로 보유된다")
        void create_valid() {
            SuggestionConceptContext context = new SuggestionConceptContext(
                    List.of("백엔드 개발자", "기획자"),
                    "구현과 문제정의를 함께 가져가고 싶어서",
                    "더 설득력 있고 실행력 있는 제품"
            );

            assertThat(context.conceptSet()).containsExactly("백엔드 개발자", "기획자");
            assertThat(context.compositionReason()).isEqualTo("구현과 문제정의를 함께 가져가고 싶어서");
            assertThat(context.desiredOutcome()).isEqualTo("더 설득력 있고 실행력 있는 제품");
        }
    }

    @Nested
    @DisplayName("엣지")
    class Edge {

        @Test
        @DisplayName("conceptSet 원소·compositionReason·desiredOutcome는 trim되어 저장된다")
        void create_trim_normalized() {
            SuggestionConceptContext context = new SuggestionConceptContext(
                    List.of("  백엔드  ", "\t기획자\n"),
                    "  reason  ",
                    "  outcome  "
            );

            assertThat(context.conceptSet()).containsExactly("백엔드", "기획자");
            assertThat(context.compositionReason()).isEqualTo("reason");
            assertThat(context.desiredOutcome()).isEqualTo("outcome");
        }

        @Test
        @DisplayName("conceptSet은 immutable로 보호되어 외부 변경이 반영되지 않는다")
        void conceptSet_immutable() {
            List<String> mutable = new ArrayList<>(Arrays.asList("백엔드", "기획자"));

            SuggestionConceptContext context = new SuggestionConceptContext(
                    mutable, "reason", "outcome"
            );
            mutable.add("디자이너");

            assertThat(context.conceptSet()).hasSize(2);
            assertThatThrownBy(() -> context.conceptSet().add("PM"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("예외")
    class Exception {

        @Test
        @DisplayName("conceptSet이 null이면 IAE")
        void create_conceptSet_null_예외() {
            assertThatThrownBy(() -> new SuggestionConceptContext(null, "r", "o"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("conceptSet이 빈 리스트이면 IAE")
        void create_conceptSet_empty_예외() {
            assertThatThrownBy(() -> new SuggestionConceptContext(List.of(), "r", "o"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("conceptSet 원소가 null이면 IAE")
        void create_conceptSet_원소_null_예외() {
            List<String> conceptSet = Arrays.asList("백엔드", null);

            assertThatThrownBy(() -> new SuggestionConceptContext(conceptSet, "r", "o"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("conceptSet 원소가 blank이면 IAE")
        void create_conceptSet_원소_blank_예외() {
            assertThatThrownBy(() -> new SuggestionConceptContext(List.of("백엔드", "   "), "r", "o"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("compositionReason이 null이면 IAE")
        void create_compositionReason_null_예외() {
            assertThatThrownBy(() -> new SuggestionConceptContext(List.of("백엔드"), null, "o"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("compositionReason이 blank이면 IAE")
        void create_compositionReason_blank_예외() {
            assertThatThrownBy(() -> new SuggestionConceptContext(List.of("백엔드"), "  ", "o"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("desiredOutcome이 null이면 IAE")
        void create_desiredOutcome_null_예외() {
            assertThatThrownBy(() -> new SuggestionConceptContext(List.of("백엔드"), "r", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("desiredOutcome이 blank이면 IAE")
        void create_desiredOutcome_blank_예외() {
            assertThatThrownBy(() -> new SuggestionConceptContext(List.of("백엔드"), "r", "\t\n"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
