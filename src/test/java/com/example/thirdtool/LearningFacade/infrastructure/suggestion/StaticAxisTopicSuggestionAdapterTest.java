package com.example.thirdtool.LearningFacade.infrastructure.suggestion;

import com.example.thirdtool.LearningFacade.application.port.out.suggestion.AxisTopicSuggestion;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.SuggestionConceptContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StaticAxisTopicSuggestionAdapter")
class StaticAxisTopicSuggestionAdapterTest {

    private final StaticAxisTopicSuggestionAdapter adapter = new StaticAxisTopicSuggestionAdapter();
    private final SuggestionConceptContext context = new SuggestionConceptContext(
            List.of("백엔드 개발자"),
            "구현과 기획을 함께 가져가고 싶어서",
            "더 설득력 있는 제품"
    );

    @Nested
    @DisplayName("해피")
    class Happy {

        @Test
        @DisplayName("existing이 빈 리스트면 고정 목록을 limit만큼 순서대로 반환한다")
        void suggest_existing_빈_limit_5_고정목록_앞5개() {
            List<AxisTopicSuggestion> result = adapter.suggest(context, "기초 지식", List.of(), 5);

            assertThat(result).extracting(AxisTopicSuggestion::description)
                    .containsExactly("기초 이해", "구조 설계", "심화 응용", "도구 활용", "실전 사례");
            assertThat(result).allSatisfy(s -> assertThat(s.rationale()).isNull());
        }
    }

    @Nested
    @DisplayName("엣지")
    class Edge {

        @Test
        @DisplayName("existing 1건 포함 시 해당 항목을 제외하고 limit만큼 반환한다")
        void suggest_existing_기초이해_limit_5_나머지_5개() {
            List<AxisTopicSuggestion> result = adapter.suggest(
                    context, "기초 지식", List.of("기초 이해"), 5
            );

            assertThat(result).extracting(AxisTopicSuggestion::description)
                    .containsExactly("구조 설계", "심화 응용", "도구 활용", "실전 사례", "핵심 개념 정리");
        }

        @Test
        @DisplayName("existing 전체가 겹치면 빈 목록 반환 (예외 X)")
        void suggest_existing_전체겹침_빈목록() {
            List<AxisTopicSuggestion> result = adapter.suggest(
                    context,
                    "기초 지식",
                    StaticAxisTopicSuggestionAdapter.FIXED_TOPIC_NAMES,
                    10
            );

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("limit=0이면 빈 목록 반환")
        void suggest_limit_0_빈목록() {
            List<AxisTopicSuggestion> result = adapter.suggest(
                    context, "기초 지식", List.of(), 0
            );

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("limit이 음수이면 빈 목록 반환")
        void suggest_limit_음수_빈목록() {
            List<AxisTopicSuggestion> result = adapter.suggest(
                    context, "기초 지식", List.of(), -1
            );

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("limit이 가용 후보 수보다 크면 가용 후보 전부 반환")
        void suggest_limit_가용보다큼_가용전부() {
            List<AxisTopicSuggestion> result = adapter.suggest(
                    context, "기초 지식", List.of(), 100
            );

            assertThat(result).hasSize(StaticAxisTopicSuggestionAdapter.FIXED_TOPIC_NAMES.size());
        }

        @Test
        @DisplayName("existing이 null이면 고정 목록을 그대로 limit만큼 반환한다 (방어)")
        void suggest_existing_null_방어() {
            List<AxisTopicSuggestion> result = adapter.suggest(
                    context, "기초 지식", null, 3
            );

            assertThat(result).extracting(AxisTopicSuggestion::description)
                    .containsExactly("기초 이해", "구조 설계", "심화 응용");
        }

        @Test
        @DisplayName("existing 원소의 앞뒤 공백은 trim 후 비교한다")
        void suggest_existing_공백포함_trim_비교() {
            List<AxisTopicSuggestion> result = adapter.suggest(
                    context, "기초 지식", List.of("  기초 이해  "), 10
            );

            assertThat(result).extracting(AxisTopicSuggestion::description)
                    .doesNotContain("기초 이해")
                    .hasSize(9);
        }
    }
}
