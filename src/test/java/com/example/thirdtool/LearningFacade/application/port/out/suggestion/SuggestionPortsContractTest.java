package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Story-AS-E1 · 3 Port(Layer/Roadmap/Selections) 인터페이스 + record 컴파일 · 계약 검증.
 * 임의 stub Adapter로 시그니처 확정 실험.
 */
@DisplayName("AS Epic 1 · 4-Port 스켈레톤 계약")
class SuggestionPortsContractTest {

    // ─── 임의 Adapter stub (컴파일 실험) ───────────────────

    private static class LayerStub implements LayerSuggestionPort {
        @Override
        public List<LayerSuggestion> suggest(LayerSuggestionContext context,
                                             List<String> existingLayerNames,
                                             int limit) {
            return List.of(new LayerSuggestion("UI", "화면 계층"));
        }
    }

    private static class RoadmapStub implements RoadmapSuggestionPort {
        @Override
        public RoadmapSuggestion suggest(RoadmapSuggestionContext context) {
            return new RoadmapSuggestion(List.of("Ch1", "Ch2"), null);
        }
    }

    private static class SelectionsStub implements SelectionsSuggestionPort {
        @Override
        public SelectionsSuggestion suggest(SelectionsSuggestionContext context) {
            return new SelectionsSuggestion(List.of("사례1", "사례2"), "핵심 판례");
        }
    }

    // ─── Layer ─────────────────────────────────────────────

    @Nested
    @DisplayName("LayerSuggestion / Context / Port")
    class Layer {

        @Test
        @DisplayName("LayerSuggestion_유효값_trim_저장")
        void LayerSuggestion_유효값_trim_저장() {
            LayerSuggestion s = new LayerSuggestion("  UI  ", "  화면 계층  ");
            assertThat(s.name()).isEqualTo("UI");
            assertThat(s.rationale()).isEqualTo("화면 계층");
        }

        @Test
        @DisplayName("LayerSuggestion_name_blank_예외")
        void LayerSuggestion_name_blank_예외() {
            assertThatThrownBy(() -> new LayerSuggestion("  ", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("LayerSuggestionContext_concepts_blank_예외")
        void LayerSuggestionContext_concepts_blank_예외() {
            assertThatThrownBy(() -> new LayerSuggestionContext(1L, List.of("  "), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("LayerSuggestionContext_facadeId_null_예외")
        void LayerSuggestionContext_facadeId_null_예외() {
            assertThatThrownBy(() -> new LayerSuggestionContext(null, List.of("A"), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Port_stub_구현_시그니처_확정")
        void Port_stub_구현_시그니처_확정() {
            LayerSuggestionPort port = new LayerStub();
            List<LayerSuggestion> result = port.suggest(
                    new LayerSuggestionContext(1L, List.of("백엔드"), null),
                    List.of(),
                    5
            );
            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("UI");
        }
    }

    // ─── Roadmap ───────────────────────────────────────────

    @Nested
    @DisplayName("RoadmapSuggestion / Context / Port")
    class Roadmap {

        @Test
        @DisplayName("RoadmapSuggestion_outline_각_원소_trim")
        void RoadmapSuggestion_outline_각_원소_trim() {
            RoadmapSuggestion s = new RoadmapSuggestion(List.of("  Ch1  ", "  Ch2  "), "설계 이유");
            assertThat(s.outline()).containsExactly("Ch1", "Ch2");
        }

        @Test
        @DisplayName("RoadmapSuggestion_outline_빈_리스트_예외")
        void RoadmapSuggestion_outline_빈_리스트_예외() {
            assertThatThrownBy(() -> new RoadmapSuggestion(List.of(), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("RoadmapContext_axisId_null_예외")
        void RoadmapContext_axisId_null_예외() {
            assertThatThrownBy(() -> new RoadmapSuggestionContext(1L, null, null,
                    "API 설계", null, List.of("백엔드"), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Port_stub_구현_시그니처_확정")
        void Port_stub_구현_시그니처_확정() {
            RoadmapSuggestionPort port = new RoadmapStub();
            RoadmapSuggestion result = port.suggest(new RoadmapSuggestionContext(
                    1L, 10L, 100L, "API 설계", "백엔드 계층", List.of("Spring", "JPA"), "backend-developer"));
            assertThat(result.outline()).containsExactly("Ch1", "Ch2");
        }
    }

    // ─── Selections ────────────────────────────────────────

    @Nested
    @DisplayName("SelectionsSuggestion / Context / Port")
    class Selections {

        @Test
        @DisplayName("SelectionsSuggestion_selections_trim_저장")
        void SelectionsSuggestion_selections_trim_저장() {
            SelectionsSuggestion s = new SelectionsSuggestion(List.of("  사례1  "), null);
            assertThat(s.selections()).containsExactly("사례1");
        }

        @Test
        @DisplayName("SelectionsSuggestion_selections_빈리스트_예외")
        void SelectionsSuggestion_selections_빈리스트_예외() {
            assertThatThrownBy(() -> new SelectionsSuggestion(List.of(), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("SelectionsContext_roadmapOutline_nullable_허용")
        void SelectionsContext_roadmapOutline_nullable_허용() {
            SelectionsSuggestionContext ctx = new SelectionsSuggestionContext(
                    1L, 10L, 100L, "API 설계", List.of("Spring"), null, null);
            assertThat(ctx.roadmapOutline()).isNull();
        }

        @Test
        @DisplayName("SelectionsContext_facadeId_null_예외")
        void SelectionsContext_facadeId_null_예외() {
            assertThatThrownBy(() -> new SelectionsSuggestionContext(
                    null, 10L, 100L, "A", List.of("B"), null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Port_stub_구현_시그니처_확정")
        void Port_stub_구현_시그니처_확정() {
            SelectionsSuggestionPort port = new SelectionsStub();
            SelectionsSuggestion result = port.suggest(new SelectionsSuggestionContext(
                    1L, 10L, 100L, "API 설계", List.of("Spring"),
                    List.of("Ch1", "Ch2"), "backend-developer"));
            assertThat(result.selections()).containsExactly("사례1", "사례2");
        }
    }
}
