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
 * <p><strong>설계 결정 — 이벤트 객체로 결과 통신</strong>: 동기 이벤트의 특성상 핸들러가 생성한 Deck 정보가
 * 호출자에게 필요하다(응답에 deckId 포함). record 대신 mutable class를 사용해 핸들러가 {@code setResult}로
 * Deck 정보를 설정하고, 호출자가 그 값을 응답 빌드에 사용한다. 일반적 도메인 이벤트는 immutable이지만
 * 본 동기 이벤트는 BC 간 결과 통신 채널 역할도 겸한다 (ADR007 §결정 참조).
 */
public class LearningMaterialCreatedEvent {

    // ─── 입력 (immutable) ────────────────────────────────────

    private final Long userId;
    private final Long materialId;
    private final String materialName;
    private final Long axisId;
    private final String requestedDeckName;
    private final boolean forceCreateDeck;

    // ─── 결과 (핸들러가 set) ──────────────────────────────────

    private Long createdDeckId;
    private String createdDeckName;

    public LearningMaterialCreatedEvent(
            Long userId,
            Long materialId,
            String materialName,
            Long axisId,
            String requestedDeckName,
            boolean forceCreateDeck) {
        this.userId = userId;
        this.materialId = materialId;
        this.materialName = materialName;
        this.axisId = axisId;
        this.requestedDeckName = requestedDeckName;
        this.forceCreateDeck = forceCreateDeck;
    }

    public Long userId() { return userId; }
    public Long materialId() { return materialId; }
    public String materialName() { return materialName; }
    public Long axisId() { return axisId; }
    public String requestedDeckName() { return requestedDeckName; }
    public boolean forceCreateDeck() { return forceCreateDeck; }

    public Long createdDeckId() { return createdDeckId; }
    public String createdDeckName() { return createdDeckName; }

    /**
     * 핸들러가 Deck 생성 직후 호출 — 호출자가 응답에 사용한다.
     */
    public void setResult(Long deckId, String deckName) {
        this.createdDeckId = deckId;
        this.createdDeckName = deckName;
    }
}
