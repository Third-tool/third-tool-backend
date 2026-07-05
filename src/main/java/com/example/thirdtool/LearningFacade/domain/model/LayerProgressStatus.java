package com.example.thirdtool.LearningFacade.domain.model;

/**
 * LearningLayer 진행 상태 (LT E6 · M5 · Story 6-4).
 *
 * <p>Layer 자체는 상태 컬럼을 갖지 않으며, 하위 axis의 {@link AxisProgressStatus} 상태를 집계한 파생값.
 * {@link LearningLayer#progressStatus()} 도메인 메서드가 계산한다.
 *
 * <p>파생 규칙 (SDD):
 * <ul>
 *   <li>하위 axis 0건 · 모두 NOT_STARTED → {@link #NOT_STARTED}</li>
 *   <li>하위 axis 모두 COMPLETED → {@link #COMPLETED}</li>
 *   <li>그 외 (혼재 · 하나 이상 IN_PROGRESS 존재) → {@link #IN_PROGRESS}</li>
 * </ul>
 *
 * <p>DB 컬럼 없음 · 응답 DTO에만 노출. milestone.md V35 예약분은 파생 정책으로 인해 스킵 (SDD S6-4 준수).
 */
public enum LayerProgressStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}
