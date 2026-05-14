package com.example.thirdtool.Deck.domain.model;

/**
 * Deck 학습 진행 상태 (Story-005-2).
 *
 * <p>자동 전환 (CardCommandService 진입점에서 호출):
 * <ul>
 *   <li>{@link #NOT_STARTED} — Card 0개 (초기 상태)</li>
 *   <li>{@link #IN_PROGRESS} — Card 1개 이상 + ARCHIVE 아닌 Card 존재</li>
 *   <li>{@link #COMPLETED} — Card 1개 이상 + 모든 Card가 ARCHIVE</li>
 * </ul>
 *
 * <p>Deck 운영 모드({@code DeckMode}: ON_FIELD/ARCHIVE)와는 별개 개념.
 */
public enum DeckProgressStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}
