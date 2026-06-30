package com.example.thirdtool.LearningFacade.domain.event;

/**
 * LearningAxis 생성 시 발행되는 도메인 이벤트 (ADR007).
 *
 * <p>{@code LearningFacadeCommandService.addAxis}가 저장 직후 발행한다.
 * Deck BC의 {@code LearningAxisCreatedEventHandler}가 {@code @EventListener}(동기)로 수신해
 * 같은 트랜잭션 안에서 Axis 단위 Deck을 자동 생성한다.
 *
 * <p>immutable record — 결과 통신 불필요 (addAxis 응답에 deckId 미포함, Fix-Story 2 결정 항목).
 */
public record LearningAxisCreatedEvent(
        Long userId,
        Long axisId,
        String axisName
) {}
