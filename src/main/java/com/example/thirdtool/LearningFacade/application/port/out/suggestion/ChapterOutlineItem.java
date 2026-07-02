package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

/**
 * 챕터 outline 원소 (Story-AS-E1-S1-5, 이슈 #17).
 * ChaptersOutlinePort / SelectionOutlinePort 두 곳에서 재사용.
 *
 * <p>{@code title}은 챕터 이름 (예: "1. 하네스 엔지니어링 기초").
 * {@code rationale}은 이 챕터를 제안한 이유 (선택 · nullable).
 */
public record ChapterOutlineItem(String title, String rationale) {

    public ChapterOutlineItem {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title은 blank일 수 없습니다.");
        }
        title = title.trim();
        if (rationale != null) {
            String trimmed = rationale.trim();
            rationale = trimmed.isEmpty() ? null : trimmed;
        }
    }
}
