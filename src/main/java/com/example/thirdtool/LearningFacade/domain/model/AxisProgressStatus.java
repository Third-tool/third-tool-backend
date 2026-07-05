package com.example.thirdtool.LearningFacade.domain.model;

/**
 * LearningAxis 진행 상태.
 *
 * <p>Deck 폐기 (LT E5 · M5)에 따라 {@code DeckProgressStatus}에서 이관된 enum.
 * 축에 속한 카드 상태 집계로 파생된다. Application Service가 카드 카운트를 계산하여
 * {@link LearningAxis#recalculateProgressStatus(int, int)} 를 호출.
 *
 * <ul>
 *   <li>{@link #NOT_STARTED} — 축에 활성/아카이브 카드 0개</li>
 *   <li>{@link #IN_PROGRESS} — 활성 카드 1개 이상 존재</li>
 *   <li>{@link #COMPLETED} — 활성 카드 0개 + 아카이브 카드 1개 이상 (모든 카드가 아카이브)</li>
 * </ul>
 *
 * <p>저장 방식: {@code @Enumerated(EnumType.STRING)} + VARCHAR(20) CHECK 제약 (ADR002).
 */
public enum AxisProgressStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}
