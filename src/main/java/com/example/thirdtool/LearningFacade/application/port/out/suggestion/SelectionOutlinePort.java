package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

/**
 * AI Selection outline 생성 outbound Port (Story-AS-E1-S1-7, 이슈 #17).
 *
 * <p>이전 {@code SelectionsSuggestionPort} (axis 전체 셀렉션 리스트 통짜)는 SUPERSEDED.
 * 하나의 새 Selection 컨테이너 이름 후보 + 그 하위 챕터 outline을 반환한다.
 */
public interface SelectionOutlinePort {

    SelectionOutlineResponse suggest(SelectionOutlineRequest request);
}
