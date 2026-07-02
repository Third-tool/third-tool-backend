package com.example.thirdtool.LearningFacade.infrastructure.suggestion;

import com.example.thirdtool.LearningFacade.application.port.out.suggestion.LayerSuggestion;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.LayerSuggestionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story-AS-E2-S1 · StaticLayerSuggestionAdapter — role 카탈로그 + dedupe + limit 검증.
 */
@DisplayName("StaticLayerSuggestionAdapter")
class StaticLayerSuggestionAdapterTest {

    private StaticLayerSuggestionAdapter adapter;

    @BeforeEach
    void setUp() {
        SuggestionCatalogLoader loader = new SuggestionCatalogLoader(new ObjectMapper());
        adapter = new StaticLayerSuggestionAdapter(loader);
    }

    @Nested
    @DisplayName("suggest — 해피")
    class Happy {

        @Test
        @DisplayName("suggest_backend_developer_role_웹API가_첫_항목")
        void suggest_backend_developer_role_웹API가_첫_항목() {
            LayerSuggestionContext ctx = new LayerSuggestionContext(
                    1L, List.of("백엔드"), "backend-developer");

            List<LayerSuggestion> result = adapter.suggest(ctx, List.of(), 5);

            assertThat(result).hasSize(5);
            assertThat(result.get(0).name()).isEqualTo("웹 API");
        }

        @Test
        @DisplayName("suggest_role_null_generic_fallback_반환")
        void suggest_role_null_generic_fallback_반환() {
            LayerSuggestionContext ctx = new LayerSuggestionContext(
                    1L, List.of("random"), null);

            List<LayerSuggestion> result = adapter.suggest(ctx, List.of(), 5);

            // generic.json has 3 layers
            assertThat(result).hasSize(3);
            assertThat(result.get(0).name()).isEqualTo("핵심 개념");
        }

        @Test
        @DisplayName("suggest_존재하지_않는_role_generic_폴백")
        void suggest_존재하지_않는_role_generic_폴백() {
            LayerSuggestionContext ctx = new LayerSuggestionContext(
                    1L, List.of("random"), "nonexistent-role");

            List<LayerSuggestion> result = adapter.suggest(ctx, List.of(), 5);

            assertThat(result).isNotEmpty();
            assertThat(result.get(0).name()).isEqualTo("핵심 개념");
        }
    }

    @Nested
    @DisplayName("suggest — 필터")
    class Filter {

        @Test
        @DisplayName("suggest_existingLayerNames_에_포함된_이름_제외")
        void suggest_existingLayerNames_에_포함된_이름_제외() {
            LayerSuggestionContext ctx = new LayerSuggestionContext(
                    1L, List.of("백엔드"), "backend-developer");

            List<LayerSuggestion> result = adapter.suggest(ctx, List.of("웹 API"), 5);

            assertThat(result).extracting(LayerSuggestion::name)
                    .doesNotContain("웹 API");
            assertThat(result.get(0).name()).isEqualTo("도메인 로직");
        }

        @Test
        @DisplayName("suggest_existingLayerNames_trim_적용_후_비교")
        void suggest_existingLayerNames_trim_적용_후_비교() {
            LayerSuggestionContext ctx = new LayerSuggestionContext(
                    1L, List.of("백엔드"), "backend-developer");

            List<LayerSuggestion> result = adapter.suggest(ctx,
                    java.util.Arrays.asList("  웹 API  ", null), 5);

            assertThat(result).extracting(LayerSuggestion::name)
                    .doesNotContain("웹 API");
        }

        @Test
        @DisplayName("suggest_limit_2_상위_2개만_반환")
        void suggest_limit_2_상위_2개만_반환() {
            LayerSuggestionContext ctx = new LayerSuggestionContext(
                    1L, List.of("백엔드"), "backend-developer");

            List<LayerSuggestion> result = adapter.suggest(ctx, List.of(), 2);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("suggest_limit_0_빈_리스트")
        void suggest_limit_0_빈_리스트() {
            LayerSuggestionContext ctx = new LayerSuggestionContext(
                    1L, List.of("백엔드"), "backend-developer");

            List<LayerSuggestion> result = adapter.suggest(ctx, List.of(), 0);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("suggest_limit_음수_빈_리스트")
        void suggest_limit_음수_빈_리스트() {
            LayerSuggestionContext ctx = new LayerSuggestionContext(
                    1L, List.of("백엔드"), "backend-developer");

            List<LayerSuggestion> result = adapter.suggest(ctx, List.of(), -1);

            assertThat(result).isEmpty();
        }
    }
}
