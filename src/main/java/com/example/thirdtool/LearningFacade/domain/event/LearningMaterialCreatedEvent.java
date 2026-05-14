package com.example.thirdtool.LearningFacade.domain.event;

/**
 * 학습 자료가 등록되었을 때 발행되는 도메인 이벤트 (Story-005-1).
 *
 * <p>{@code LearningMaterialCommandService.createMaterial}이 자료 저장 직후 발행한다.
 * Deck BC의 {@code LearningMaterialCreatedEventHandler}가 {@code @EventListener}(동기)로 수신해
 * <strong>같은 트랜잭션 안에서</strong> 동명의 Deck을 자동 생성한다.
 *
 * <p>BC 간 협력 패턴은 ADR007 참조 — 단방향 동기 도메인 이벤트.
 *
 * @param userId             자료 소유 사용자 ID
 * @param materialId         생성된 자료 ID
 * @param materialName       자료 이름 (기본 Deck 이름의 원천)
 * @param axisId             자료가 연결된 축 ID. {@code linkedTopicIds}가 비어있거나 축 식별 불가면 null.
 *                           다중 축에 걸치면 첫 주제의 축 ID로 단순 결정 (Story 5-1 한정).
 * @param requestedDeckName  사용자가 자료 등록 폼에서 별도 지정한 Deck 이름. null이면 {@code materialName} 사용.
 * @param forceCreateDeck    동명 Deck 존재 시 자동 suffix `(2)` 부여로 회피할지 여부.
 *                           false면 동명 발견 시 {@code DECK_NAME_DUPLICATE} 예외로 트랜잭션 롤백 → 409.
 */
public record LearningMaterialCreatedEvent(
        Long userId,
        Long materialId,
        String materialName,
        Long axisId,
        String requestedDeckName,
        boolean forceCreateDeck
) {
}
