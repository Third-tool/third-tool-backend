package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

/**
 * AI 제안 — Axis Roadmap(헌법) 생성 outbound Port (Story-AS-E1).
 *
 * <p>Axis 하나에 대해 학습 순서 청사진(챕터 리스트)을 반환한다.
 * ADR023(예정) 용어 기준: Roadmap = 헌법(구조적 청사진).
 *
 * <p>구현체는 Static / LLM 등으로 교체 가능. 도메인 레이어는 본 Port를 import하지 않는다.</p>
 *
 * <p><b>Port 계약 (구현체 공통)</b>:</p>
 * <ul>
 *   <li>{@code context.concepts}는 사전에 trim 정규화된 상태로 전달된다.</li>
 *   <li>실패 시(타임아웃 / 파싱 실패 / 인증 실패)는 구현체별 예외로 던지며, 상위 Service가
 *       {@code roadmapAvailable: false} + 빈 outline 응답으로 변환.</li>
 *   <li>구현체는 입력 context를 변경해선 안 된다 (read-only contract).</li>
 * </ul>
 */
public interface RoadmapSuggestionPort {

    /**
     * 지정 Axis에 대해 Roadmap outline을 반환한다.
     */
    RoadmapSuggestion suggest(RoadmapSuggestionContext context);
}
