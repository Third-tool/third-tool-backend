package com.example.thirdtool.LearningFacade.infrastructure.suggestion;

import com.example.thirdtool.LearningFacade.application.port.out.suggestion.AxisSuggestion;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.SuggestionConceptContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StaticAxisSuggestionAdapter")
class StaticAxisSuggestionAdapterTest {

    private final StaticAxisSuggestionAdapter adapter = new StaticAxisSuggestionAdapter();
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
        void suggest_existing_빈_limit_5_고정목록_그대로() {
            List<AxisSuggestion> result = adapter.suggest(context, List.of(), 5);

            assertThat(result).extracting(AxisSuggestion::description)
                    .containsExactlyElementsOf(StaticAxisSuggestionAdapter.FIXED_AXIS_NAMES);
            assertThat(result).allSatisfy(s -> assertThat(s.rationale()).isNull());
        }
    }

    @Nested
    @DisplayName("엣지")
    class Edge {

        @Test
        @DisplayName("existing 1건 포함 시 해당 항목을 제외한 나머지 4개를 반환한다 (AC #2)")
        void suggest_existing_기초지식_limit_5_4개_반환() {
            List<AxisSuggestion> result = adapter.suggest(context, List.of("기초 지식"), 5);

            assertThat(result).extracting(AxisSuggestion::description)
                    .containsExactly("핵심 방법론", "도구와 환경", "실전 응용", "심화 주제");
        }

        @Test
        @DisplayName("existing 전체 5건이 겹치면 빈 목록 반환 (AC #3, 예외 X)")
        void suggest_existing_전체겹침_빈목록() {
            List<AxisSuggestion> result = adapter.suggest(
                    context,
                    StaticAxisSuggestionAdapter.FIXED_AXIS_NAMES,
                    5
            );

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("limit=0이면 빈 목록 반환 (AC #4)")
        void suggest_limit_0_빈목록() {
            List<AxisSuggestion> result = adapter.suggest(context, List.of(), 0);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("limit이 음수이면 빈 목록 반환")
        void suggest_limit_음수_빈목록() {
            List<AxisSuggestion> result = adapter.suggest(context, List.of(), -3);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("limit이 가용 후보 수보다 크면 가용 후보 전부 반환")
        void suggest_limit_가용보다큼_가용전부() {
            List<AxisSuggestion> result = adapter.suggest(context, List.of(), 100);

            assertThat(result).hasSize(StaticAxisSuggestionAdapter.FIXED_AXIS_NAMES.size());
        }

        @Test
        @DisplayName("existing이 null이면 고정 목록을 그대로 반환한다 (방어)")
        void suggest_existing_null_고정목록_그대로() {
            List<AxisSuggestion> result = adapter.suggest(context, null, 5);

            assertThat(result).extracting(AxisSuggestion::description)
                    .containsExactlyElementsOf(StaticAxisSuggestionAdapter.FIXED_AXIS_NAMES);
        }

        @Test
        @DisplayName("existing 원소의 앞뒤 공백은 trim 후 비교한다")
        void suggest_existing_공백포함_trim_비교() {
            List<AxisSuggestion> result = adapter.suggest(context, List.of("  기초 지식  "), 5);

            assertThat(result).extracting(AxisSuggestion::description)
                    .doesNotContain("기초 지식")
                    .hasSize(4);
        }

        @Test
        @DisplayName("existing 원소에 null이 섞여 있어도 무시하고 진행한다")
        void suggest_existing_null원소_무시() {
            List<String> existing = new java.util.ArrayList<>();
            existing.add("기초 지식");
            existing.add(null);

            List<AxisSuggestion> result = adapter.suggest(context, existing, 5);

            assertThat(result).extracting(AxisSuggestion::description)
                    .doesNotContain("기초 지식")
                    .hasSize(4);
        }
    }
}
