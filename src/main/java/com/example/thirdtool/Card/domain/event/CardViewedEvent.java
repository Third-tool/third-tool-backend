package com.example.thirdtool.Card.domain.event;

/**
 * 카드 열람 이벤트.
 *
 * <p>Story-LT-E4-S4-5 — ReviewCommandService가 카드 노출 시점에 발행한다.
 * axisId 필드로 축 소유권을 명시 (M5 Deck 폐기 시 Deck 참조를 통해 유도할 필요 없음).
 *
 * <p><b>소비처</b>: M4에는 소비처 없음. M5 DailyLearningBatch가 축별 최근 활동 집계에 소비 예정.
 *
 * @param cardId 열람된 카드 ID
 * @param userId 열람 사용자 ID
 * @param axisId 카드가 소속된 축 ID (card.axis_id 스냅샷)
 */
public record CardViewedEvent(Long cardId, Long userId, Long axisId) {
}
