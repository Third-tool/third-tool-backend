package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import java.util.List;

/**
 * AI 제안 — LearningLayer 후보 생성 outbound Port (Story-AS-E1).
 *
 * <p>구현체는 Static(코드 상수 기반 카탈로그) / LLM(Vertex AI Gemini) 등으로 교체 가능.
 * 도메인 레이어는 본 Port를 import하지 않는다 — Application Service만 호출.</p>
 *
 * <p><b>Port 계약 (구현체 공통)</b>:</p>
 * <ul>
 *   <li>{@code existingLayerNames}의 각 원소는 trim 후 비교한다 (conventions.md §1.1 입력 정규화).</li>
 *   <li>{@code existingLayerNames}가 null이거나 빈 리스트면 중복 제거 없이 후보 전체에서 limit만큼 반환.</li>
 *   <li>{@code existingLayerNames}의 null 원소는 무시한다 (예외 미발생).</li>
 *   <li>{@code limit <= 0}이면 빈 목록을 반환하고 예외를 던지지 않는다.</li>
 *   <li>구현체는 입력 List를 변경해선 안 된다 (read-only contract).</li>
 * </ul>
 *
 * @see AxisSuggestionPort
 */
public interface LayerSuggestionPort {

    /**
     * 사용자의 concept context + 기존 Layer 이름 목록을 입력으로 Layer 후보를 반환한다.
     *
     * <p>구현체는 {@code existingLayerNames}와 중복되는 후보를 제거하고 최대 {@code limit}개까지 반환한다.
     * 호출 실패 시(타임아웃 / 파싱 실패 / 인증 실패)는 구현체별 예외로 던지며, 상위 Service가
     * 빈 목록 + {@code suggestionsAvailable: false}로 변환한다(Fallback 정책).</p>
     */
    List<LayerSuggestion> suggest(LayerSuggestionContext context,
                                   List<String> existingLayerNames,
                                   int limit);
}
