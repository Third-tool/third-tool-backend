package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import java.util.List;

/**
 * AI 제안 — 하위 주제(AxisTopic) 후보 생성 outbound Port.
 *
 * <p>축 이름({@code axisName})을 추가 컨텍스트로 받아 해당 축 내부의 주제 후보를 반환한다.
 * 도메인 레이어는 본 Port를 import하지 않는다 — Application Service만 호출.</p>
 *
 * @see AxisSuggestionPort
 */
public interface AxisTopicSuggestionPort {

    /**
     * 사용자의 concept context + 축 이름 + 기존 주제 이름 목록을 입력으로 주제 후보를 반환한다.
     *
     * <p>구현체는 {@code existingTopicNames}와 중복되는 후보를 제거하고 최대 {@code limit}개까지 반환한다.
     * 호출 실패 시는 구현체별 예외로 던지며, 상위 Service가 빈 목록 + {@code suggestionsAvailable: false}로
     * 변환한다(Fallback 정책).</p>
     */
    List<AxisTopicSuggestion> suggest(SuggestionConceptContext conceptContext,
                                      String axisName,
                                      List<String> existingTopicNames,
                                      int limit);
}
