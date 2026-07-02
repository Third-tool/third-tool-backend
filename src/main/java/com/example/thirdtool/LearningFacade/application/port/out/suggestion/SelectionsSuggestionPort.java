package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

/**
 * AI 제안 — Axis Selections(판례) 생성 outbound Port (Story-AS-E1).
 *
 * <p>Axis 하나에 대해 학습 과정에서 활용할 구체 사례·응용 판례 리스트를 반환한다.
 * ADR023(예정) 용어 기준: Selection = 판례(구체 사례).
 *
 * <p><b>Port 계약 (구현체 공통)</b>:</p>
 * <ul>
 *   <li>{@code context.roadmapOutline}이 제공되면 챕터별로 관련성 있는 판례를 생성.</li>
 *   <li>실패 시(타임아웃 / 파싱 실패 / 인증 실패)는 구현체별 예외로 던지며, 상위 Service가
 *       빈 selections + {@code selectionsAvailable: false}로 변환.</li>
 *   <li>구현체는 입력 context를 변경해선 안 된다 (read-only contract).</li>
 * </ul>
 */
public interface SelectionsSuggestionPort {

    /**
     * 지정 Axis에 대해 판례(구체 사례) 리스트를 반환한다.
     */
    SelectionsSuggestion suggest(SelectionsSuggestionContext context);
}
