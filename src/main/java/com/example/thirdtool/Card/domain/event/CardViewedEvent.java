package com.example.thirdtool.Card.domain.event;

/**
 * 카드 열람 이벤트.
 *
 * <p>Story-LT-E4-S4-5 — ReviewCommandService가 카드 노출 시점에 발행한다.
 * axisId 필드로 축 소유권을 명시 (M5 Deck 폐기 시 Deck 참조를 통해 유도할 필요 없음).
 *
 * <p><b>트랜잭션 계약</b>: 이벤트는 트랜잭션 내 발행되며, M5 이후 리스너는
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)}로 커밋 후 처리를 계약한다.
 * 커밋 전 다른 리스너가 걸리면 rollback 시 사이드이펙트 유출 위험 있음.
 *
 * <p><b>소비처</b>: M4에는 소비처 없음. M5 DailyLearningBatch가 축별 최근 활동 집계에 소비 예정.
 *
 * @param cardId 열람된 카드 ID
 * @param userId 열람 사용자 ID
 * @param axisId 카드가 소속된 축 ID (card.axis_id 스냅샷)
 */
public record CardViewedEvent(Long cardId, Long userId, Long axisId) {
}
