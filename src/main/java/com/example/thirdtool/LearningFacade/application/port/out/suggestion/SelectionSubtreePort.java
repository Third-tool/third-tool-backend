package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

/**
 * AI Selection 챕터 subtree 생성 outbound Port (Story-AS-E1-S1-8, 이슈 #17).
 * ChapterSubtreeResponse를 재사용 (bodyAsciiTree · providerContext · suggestionsAvailable).
 */
public interface SelectionSubtreePort {

    ChapterSubtreeResponse suggest(SelectionSubtreeRequest request);
}
