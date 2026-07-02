package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.LearningFacade.application.dto.SuggestionCommand;
import com.example.thirdtool.LearningFacade.application.dto.SuggestionResult;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.ChapterOutlineItem;
import com.example.thirdtool.LearningFacade.infrastructure.suggestion.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story-AS-E5-S5-3 (부분) · SuggestionAppService 통합 시나리오.
 *
 * <p>M3 하이라이트: chapters-outline 첫 응답 확인 (milestone.md line 251 예시 정합).
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
    @DisplayName("[M3 하이라이트] milestone.md 예시 그대로 — concepts=[백엔드,기획자] layer=기능의 구현 axis=하네스 엔지니어링 → 5개 챕터 200")
    void chaptersOutline_m3MilestoneExample() {
        SuggestionResult.ChaptersOutline result = service.chaptersOutline(
                new SuggestionCommand.ChaptersOutline(
                        List.of("백엔드", "기획자"), "기능의 구현", "하네스 엔지니어링",
                        "AI 에이전트 기본 프레임", null, null));

        assertThat(result.suggestionsAvailable()).isTrue();
        assertThat(result.providerContext()).isEqualTo("static:backend-developer");
        assertThat(result.chapters()).hasSize(5);
        assertThat(result.chapters().get(0).title()).isEqualTo("1. 하네스 엔지니어링 기초");
        assertThat(result.chapters().get(0).rationale()).contains("실전");
    }

    @Test
    @DisplayName("[M3 하이라이트] milestone.md chapter-subtree 예시 — chapter=1. 하네스 엔지니어링 기초 → body ASCII")
    void chapterSubtree_m3MilestoneExample() {
        SuggestionResult.ChapterSubtree result = service.chapterSubtree(
                new SuggestionCommand.ChapterSubtree(
                        List.of("백엔드", "기획자"), "기능의 구현", "하네스 엔지니어링",
                        new ChapterOutlineItem("1. 하네스 엔지니어링 기초", "AI 에이전트 기본 프레임"),
                        List.of()));

        assertThat(result.suggestionsAvailable()).isTrue();
        assertThat(result.bodyAsciiTree()).contains("├── 1-1. 정의와 본질");
        assertThat(result.bodyAsciiTree()).contains("모델 + 하네스");
        assertThat(result.providerContext()).isEqualTo("static:backend-developer");
    }

    @Test
    @DisplayName("[해피] REST 원칙 축 (backward compat) → 4개 챕터")
    void chaptersOutline_restOriginalAxis() {
        SuggestionResult.ChaptersOutline result = service.chaptersOutline(
                new SuggestionCommand.ChaptersOutline(
                        List.of("백엔드"), "웹 API", "REST 원칙", null, null, null));

        assertThat(result.suggestionsAvailable()).isTrue();
        assertThat(result.chapters()).hasSize(4);
        assertThat(result.chapters().get(0).title()).startsWith("1. REST");
    }

    @Test
    @DisplayName("[해피] selection-outline: 기능 아키텍처 variantHint → nameCandidate 매칭")
    void selectionOutline_variantHintMatched() {
        SuggestionResult.SelectionOutline result = service.selectionOutline(
                new SuggestionCommand.SelectionOutline(
                        List.of("백엔드"), "웹 API", "REST 원칙",
                        null, "기능 아키텍처", null));

        assertThat(result.suggestionsAvailable()).isTrue();
        assertThat(result.nameCandidate()).contains("기능 아키텍처");
        assertThat(result.chapters()).isNotEmpty();
    }

    @Test
    @DisplayName("[해피] selection-subtree: 하네스 엔지니어링 selection → subtree 반환")
    void selectionSubtree_harnessEngineering() {
        SuggestionResult.ChapterSubtree result = service.selectionSubtree(
                new SuggestionCommand.SelectionSubtree(
                        List.of("백엔드"), "기능의 구현", "하네스 엔지니어링",
                        new ChapterOutlineItem("1. AutoGPT 초기 사례", null),
                        "실전 하네스 사례 selections v1",
                        List.of()));

        assertThat(result.suggestionsAvailable()).isTrue();
        assertThat(result.bodyAsciiTree()).contains("Planner");
    }

    @Test
    @DisplayName("[엣지] catalog 미매칭 (알 수 없는 axisName) → suggestionsAvailable=false")
    void chaptersOutline_unavailable() {
        SuggestionResult.ChaptersOutline result = service.chaptersOutline(
                new SuggestionCommand.ChaptersOutline(
                        List.of("백엔드"), "layer", "존재하지 않는 축",
                        null, null, null));

        assertThat(result.suggestionsAvailable()).isFalse();
        assertThat(result.chapters()).isEmpty();
        assertThat(result.providerContext()).isEqualTo("static:backend-developer");
    }
}
