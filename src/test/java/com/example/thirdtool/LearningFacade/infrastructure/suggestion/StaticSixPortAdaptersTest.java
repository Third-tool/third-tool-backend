package com.example.thirdtool.LearningFacade.infrastructure.suggestion;

import com.example.thirdtool.LearningFacade.application.port.out.suggestion.*;
import com.example.thirdtool.LearningFacade.application.service.RoleDetector;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story-AS-E2-S2-5~S2-8 · 4개 Static Adapter 단위 테스트.
 * classpath의 backend-developer.json을 실제 로드해 검증.
 */
@DisplayName("Static Six-Port Adapters (Story-AS-E2-S2-5~S2-8)")
class StaticSixPortAdaptersTest {

    private SuggestionCatalogLoader catalogLoader;
    private RoleDetector roleDetector;

    private StaticChaptersOutlineAdapter chaptersOutlineAdapter;
    private StaticChapterSubtreeAdapter chapterSubtreeAdapter;
    private StaticSelectionOutlineAdapter selectionOutlineAdapter;
    private StaticSelectionSubtreeAdapter selectionSubtreeAdapter;

    @BeforeEach
    void setUp() {
        catalogLoader = new SuggestionCatalogLoader(new ObjectMapper());
        roleDetector = new RoleDetector();

        chaptersOutlineAdapter = new StaticChaptersOutlineAdapter(catalogLoader, roleDetector);
        chapterSubtreeAdapter = new StaticChapterSubtreeAdapter(catalogLoader, roleDetector);
        selectionOutlineAdapter = new StaticSelectionOutlineAdapter(catalogLoader, roleDetector);
        selectionSubtreeAdapter = new StaticSelectionSubtreeAdapter(catalogLoader, roleDetector);
    }

    // ─── ChaptersOutlineAdapter ─────────────────────────────

    @Test
    @DisplayName("[해피] concepts=[백엔드] → role=backend-developer 감지 + REST 원칙 챕터 반환")
    void chaptersOutline_backendDeveloper_restOriginciples() {
        ChaptersOutlineResponse res = chaptersOutlineAdapter.suggest(new ChaptersOutlineRequest(
                List.of("백엔드"), "웹 API", "REST 원칙", null, null, null));

        assertThat(res.suggestionsAvailable()).isTrue();
        assertThat(res.providerContext()).isEqualTo("static:backend-developer");
        assertThat(res.chapters()).isNotEmpty();
        assertThat(res.chapters().get(0).title()).startsWith("1.");
    }

    @Test
    @DisplayName("[엣지] chapterCountHint=2 → 상위 2개만 반환")
    void chaptersOutline_countHint() {
        ChaptersOutlineResponse res = chaptersOutlineAdapter.suggest(new ChaptersOutlineRequest(
                List.of("백엔드"), "웹 API", "REST 원칙", null, 2, null));

        assertThat(res.chapters()).hasSize(2);
    }

    @Test
    @DisplayName("[엣지] axis 미매칭 → suggestionsAvailable=false")
    void chaptersOutline_axisMismatch_unavailable() {
        ChaptersOutlineResponse res = chaptersOutlineAdapter.suggest(new ChaptersOutlineRequest(
                List.of("백엔드"), "웹 API", "없는 축", null, null, null));

        assertThat(res.suggestionsAvailable()).isFalse();
        assertThat(res.chapters()).isEmpty();
        assertThat(res.providerContext()).isEqualTo("static:backend-developer");
    }

    // ─── ChapterSubtreeAdapter ─────────────────────────────

    @Test
    @DisplayName("[해피] REST 원칙의 챕터 1 → bodyAsciiTree 반환")
    void chapterSubtree_matchesFirstChapter() {
        ChapterSubtreeResponse res = chapterSubtreeAdapter.suggest(new ChapterSubtreeRequest(
                List.of("백엔드"), "웹 API", "REST 원칙",
                new ChapterOutlineItem("1. REST 아키텍처 개요", null),
                List.of()));

        assertThat(res.suggestionsAvailable()).isTrue();
        assertThat(res.bodyAsciiTree()).contains("├── 1-1.");
        assertThat(res.providerContext()).isEqualTo("static:backend-developer");
    }

    @Test
    @DisplayName("[엣지] chapter title 미매칭 → unavailable")
    void chapterSubtree_titleMismatch_unavailable() {
        ChapterSubtreeResponse res = chapterSubtreeAdapter.suggest(new ChapterSubtreeRequest(
                List.of("백엔드"), "웹 API", "REST 원칙",
                new ChapterOutlineItem("존재하지 않는 챕터", null),
                List.of()));

        assertThat(res.suggestionsAvailable()).isFalse();
    }

    // ─── SelectionOutlineAdapter ─────────────────────────────

    @Test
    @DisplayName("[해피] variantHint='능 아키텍처' → nameCandidate 매칭")
    void selectionOutline_variantHintMatched() {
        SelectionOutlineResponse res = selectionOutlineAdapter.suggest(new SelectionOutlineRequest(
                List.of("백엔드"), "웹 API", "REST 원칙", null, "능 아키텍처", null));

        assertThat(res.suggestionsAvailable()).isTrue();
        assertThat(res.nameCandidate()).contains("능 아키텍처");
        assertThat(res.chapters()).isNotEmpty();
    }

    @Test
    @DisplayName("[해피] variantHint 없어도 axis 매칭 첫 항목 반환")
    void selectionOutline_axisOnlyMatch() {
        SelectionOutlineResponse res = selectionOutlineAdapter.suggest(new SelectionOutlineRequest(
                List.of("백엔드"), "웹 API", "Aggregate 설계", null, null, null));

        assertThat(res.suggestionsAvailable()).isTrue();
        assertThat(res.nameCandidate()).isNotBlank();
    }

    // ─── SelectionSubtreeAdapter ─────────────────────────────

    @Test
    @DisplayName("[해피] Selection 컨테이너 하위 챕터 subtree 반환")
    void selectionSubtree_matched() {
        ChapterSubtreeResponse res = selectionSubtreeAdapter.suggest(new SelectionSubtreeRequest(
                List.of("백엔드"), "웹 API", "REST 원칙",
                new ChapterOutlineItem("1. 큰 조직의 API-First 사례", null),
                "능 아키텍처 REST selections v1",
                List.of()));

        assertThat(res.suggestionsAvailable()).isTrue();
        assertThat(res.bodyAsciiTree()).contains("Stripe");
    }

    @Test
    @DisplayName("[엣지] role 미감지 (concepts=[알 수 없는 개념]) → generic으로 폴백")
    void unknownRole_fallsBackToGeneric() {
        ChaptersOutlineResponse res = chaptersOutlineAdapter.suggest(new ChaptersOutlineRequest(
                List.of("아무개 특수 분야"), "layer", "REST 원칙", null, null, null));

        assertThat(res.providerContext()).isEqualTo("static:generic");
    }
}
