package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Story-AS-E1-S1-5~S1-8 · 신규 6-Port DTO record 단위 테스트.
 */
@DisplayName("6-Port DTO record 유효성 (Story-AS-E1-S1-5~S1-8)")
class SixPortDtoTest {

    @Test
    @DisplayName("ChapterOutlineItem: title trim + rationale blank → null")
    void chapterOutlineItem_normalize() {
        ChapterOutlineItem item = new ChapterOutlineItem("  1. 챕터  ", "   ");
        assertThat(item.title()).isEqualTo("1. 챕터");
        assertThat(item.rationale()).isNull();
    }

    @Test
    @DisplayName("ChapterOutlineItem: title blank → IllegalArgumentException")
    void chapterOutlineItem_titleBlank() {
        assertThatThrownBy(() -> new ChapterOutlineItem("   ", "근거"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ChaptersOutlineRequest: 필수 필드 검증 + concepts trim")
    void chaptersOutlineRequest_valid() {
        ChaptersOutlineRequest req = new ChaptersOutlineRequest(
                List.of("  백엔드  ", "기획자"), "기능의 구현", "하네스 엔지니어링",
                "AI 프레임", 5, null);
        assertThat(req.concepts()).containsExactly("백엔드", "기획자");
        assertThat(req.axisName()).isEqualTo("하네스 엔지니어링");
    }

    @Test
    @DisplayName("ChaptersOutlineRequest: concepts 빈 리스트 → 예외")
    void chaptersOutlineRequest_emptyConcepts() {
        assertThatThrownBy(() -> new ChaptersOutlineRequest(
                List.of(), "layer", "axis", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ChaptersOutlineResponse.unavailable: 빈 chapters + suggestionsAvailable=false")
    void chaptersOutlineResponse_unavailable() {
        ChaptersOutlineResponse res = ChaptersOutlineResponse.unavailable("static:backend-developer");
        assertThat(res.chapters()).isEmpty();
        assertThat(res.suggestionsAvailable()).isFalse();
        assertThat(res.providerContext()).isEqualTo("static:backend-developer");
    }

    @Test
    @DisplayName("ChapterSubtreeRequest: chapter null → 예외")
    void chapterSubtreeRequest_nullChapter() {
        assertThatThrownBy(() -> new ChapterSubtreeRequest(
                List.of("백엔드"), "layer", "axis", null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ChapterSubtreeResponse.unavailable: bodyAsciiTree null 허용")
    void chapterSubtreeResponse_unavailable() {
        ChapterSubtreeResponse res = ChapterSubtreeResponse.unavailable("static:generic");
        assertThat(res.bodyAsciiTree()).isNull();
        assertThat(res.suggestionsAvailable()).isFalse();
    }

    @Test
    @DisplayName("SelectionOutlineRequest: variantHint blank → null 정규화")
    void selectionOutlineRequest_variantHintBlank() {
        SelectionOutlineRequest req = new SelectionOutlineRequest(
                List.of("백엔드"), "layer", "axis", null, "   ", null);
        assertThat(req.variantHint()).isNull();
    }

    @Test
    @DisplayName("SelectionSubtreeRequest: selectionName blank → 예외")
    void selectionSubtreeRequest_selectionNameBlank() {
        ChapterOutlineItem chapter = new ChapterOutlineItem("1. 챕터", null);
        assertThatThrownBy(() -> new SelectionSubtreeRequest(
                List.of("백엔드"), "layer", "axis", chapter, "   ", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SelectionOutlineResponse.unavailable: nameCandidate null + 빈 chapters")
    void selectionOutlineResponse_unavailable() {
        SelectionOutlineResponse res = SelectionOutlineResponse.unavailable("static:backend-developer");
        assertThat(res.nameCandidate()).isNull();
        assertThat(res.chapters()).isEmpty();
        assertThat(res.suggestionsAvailable()).isFalse();
    }
}
