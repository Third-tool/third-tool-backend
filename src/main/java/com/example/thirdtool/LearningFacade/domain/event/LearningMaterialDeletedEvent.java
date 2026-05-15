package com.example.thirdtool.LearningFacade.domain.event;

/**
 * 학습 자료 삭제 시 발행되는 동기 도메인 이벤트 (Story-005-2).
 *
 * <p>Deck BC의 핸들러가 수신해 연결된 Deck들의 {@code learningMaterialId}를 null로 세팅한다
 * — Deck 자체는 보존되고 "자료 미연결 Deck"으로 유지된다.
 *
 * <p>Story 5-1의 {@code LearningMaterialCreatedEvent}와 의도적으로 다른 형태(immutable record)
 * — 핸들러가 호출자에게 돌려보낼 결과가 없기 때문이다. ADR007의 동기 이벤트 패턴은 유지.
 */
public record LearningMaterialDeletedEvent(Long userId, Long materialId) {
}
