package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.LearningFacade.infrastructure.suggestion.*;
import com.example.thirdtool.LearningFacade.presentation.dto.SuggestionRequest;
import com.example.thirdtool.LearningFacade.presentation.dto.SuggestionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story-AS-E5-S5-3 (부분) · SuggestionAppService 통합 시나리오.
 *
 * <p>M3 하이라이트: chapters-outline 첫 응답 확인.
 * Static Adapter를 실제로 배선 · classpath backend-developer.json 로드.
 */
@DisplayName("SuggestionAppService — M3 하이라이트 (Story-AS-E5-S5-3)")
class SuggestionAppServiceTest {

    private SuggestionAppService service;

    @BeforeEach
    void setUp() {
        SuggestionCatalogLoader loader = new SuggestionCatalogLoader(new ObjectMapper());
        RoleDetector detector = new RoleDetector();

        service = new SuggestionAppService(
                new StaticChaptersOutlineAdapter(loader, detector),
                new StaticChapterSubtreeAdapter(loader, detector),
                new StaticSelectionOutlineAdapter(loader, detector),
                new StaticSelectionSubtreeAdapter(loader, detector)
        );
    }

    @Test
    @DisplayName("[M3 하이라이트] chapters-outline concepts=[백엔드,기획자] axis=REST 원칙 → 챕터 리스트 + providerContext")
    void chaptersOutline_m3Highlight() {
        SuggestionResponse.ChaptersOutlineResponseBody response = service.chaptersOutline(
                new SuggestionRequest.ChaptersOutlineRequest(
                        List.of("백엔드", "기획자"), "웹 API", "REST 원칙",
                        "REST 학습 목적", null, null));

        assertThat(response.suggestionsAvailable()).isTrue();
        assertThat(response.providerContext()).isEqualTo("static:backend-developer");
        assertThat(response.chapters()).isNotEmpty();
        assertThat(response.chapters()).allSatisfy(c -> {
            assertThat(c.title()).isNotBlank();
        });
    }

    @Test
    @DisplayName("[해피] chapter-subtree → bodyAsciiTree ASCII 반환")
    void chapterSubtree_returnsBody() {
        SuggestionResponse.ChapterSubtreeResponseBody response = service.chapterSubtree(
                new SuggestionRequest.ChapterSubtreeRequest(
                        List.of("백엔드"), "웹 API", "REST 원칙",
                        new SuggestionRequest.ChapterOutlineItemDto("1. REST 아키텍처 개요", null),
                        List.of()));

        assertThat(response.suggestionsAvailable()).isTrue();
        assertThat(response.bodyAsciiTree()).contains("├── 1-1.");
        assertThat(response.providerContext()).isEqualTo("static:backend-developer");
    }

    @Test
    @DisplayName("[해피] selection-outline → nameCandidate + chapters")
    void selectionOutline_returnsCandidateName() {
        SuggestionResponse.SelectionOutlineResponseBody response = service.selectionOutline(
                new SuggestionRequest.SelectionOutlineRequest(
                        List.of("백엔드"), "웹 API", "REST 원칙",
                        null, "능 아키텍처", null));

        assertThat(response.suggestionsAvailable()).isTrue();
        assertThat(response.nameCandidate()).contains("능 아키텍처");
        assertThat(response.chapters()).isNotEmpty();
    }

    @Test
    @DisplayName("[해피] selection-subtree → 매칭된 챕터의 subtree 반환")
    void selectionSubtree_returnsBody() {
        SuggestionResponse.ChapterSubtreeResponseBody response = service.selectionSubtree(
                new SuggestionRequest.SelectionSubtreeRequest(
                        List.of("백엔드"), "웹 API", "REST 원칙",
                        new SuggestionRequest.ChapterOutlineItemDto("1. 큰 조직의 API-First 사례", null),
                        "능 아키텍처 REST selections v1",
                        List.of()));

        assertThat(response.suggestionsAvailable()).isTrue();
        assertThat(response.bodyAsciiTree()).contains("Stripe");
    }

    @Test
    @DisplayName("[엣지] catalog 미매칭 (알 수 없는 axisName) → suggestionsAvailable=false")
    void chaptersOutline_unavailable() {
        SuggestionResponse.ChaptersOutlineResponseBody response = service.chaptersOutline(
                new SuggestionRequest.ChaptersOutlineRequest(
                        List.of("백엔드"), "layer", "존재하지 않는 축",
                        null, null, null));

        assertThat(response.suggestionsAvailable()).isFalse();
        assertThat(response.chapters()).isEmpty();
        assertThat(response.providerContext()).isEqualTo("static:backend-developer");
    }
}
