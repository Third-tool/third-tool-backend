package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import java.util.List;

/**
 * AI 제안 — 학습 축(LearningAxis) 후보 생성 outbound Port.
 *
 * <p>구현체는 Static(코드 상수 기반 Fallback) / LLM(Vertex AI Gemini) 등으로 교체 가능.
 * 도메인 레이어는 본 Port를 import하지 않는다 — Application Service만 호출.</p>
 *
 * @see AxisTopicSuggestionPort
 */
public interface AxisSuggestionPort {

    /**
     * 사용자의 concept context + 기존 축 이름 목록을 입력으로 축 후보를 반환한다.
     *
     * <p>구현체는 {@code existingAxisNames}와 중복되는 후보를 제거하고 최대 {@code limit}개까지 반환한다.
     * 호출 실패 시(타임아웃 / 파싱 실패 / 인증 실패)는 구현체별 예외로 던지며, 상위 Service가
     * 빈 목록 + {@code suggestionsAvailable: false}로 변환한다(Fallback 정책).</p>
     */
    List<AxisSuggestion> suggest(SuggestionConceptContext conceptContext,
                                 List<String> existingAxisNames,
                                 int limit);
}
