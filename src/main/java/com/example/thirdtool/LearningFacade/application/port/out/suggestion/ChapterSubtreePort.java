package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

/**
 * AI 챕터 subtree 생성 outbound Port (Story-AS-E1-S1-6, 이슈 #17).
 * {@link ChaptersOutlinePort}가 반환한 챕터 하나에 대해 subtree(ASCII 통짜)를 병렬 생성한다.
 */
public interface ChapterSubtreePort {

    ChapterSubtreeResponse suggest(ChapterSubtreeRequest request);
}
