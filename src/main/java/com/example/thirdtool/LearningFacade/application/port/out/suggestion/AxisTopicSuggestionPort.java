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
     *
     * <p><b>Port 계약 (구현체 공통)</b>:</p>
     * <ul>
     *   <li>{@code existingTopicNames}의 각 원소는 trim 후 비교한다 (conventions.md §1.1 입력 정규화).</li>
     *   <li>{@code existingTopicNames}가 null이거나 빈 리스트면 중복 제거 없이 후보 전체에서 limit만큼 반환.</li>
     *   <li>{@code existingTopicNames}의 null 원소는 무시한다 (예외 미발생).</li>
     *   <li>{@code limit <= 0}이면 빈 목록을 반환하고 예외를 던지지 않는다.</li>
     *   <li>구현체는 입력 List를 변경해선 안 된다 (read-only contract).</li>
     *   <li>Static 구현은 {@code axisName}을 사용하지 않으나, LLM 구현은 축 컨텍스트로 활용한다.</li>
     * </ul>
     */
    List<AxisTopicSuggestion> suggest(SuggestionConceptContext conceptContext,
                                      String axisName,
                                      List<String> existingTopicNames,
                                      int limit);
}
