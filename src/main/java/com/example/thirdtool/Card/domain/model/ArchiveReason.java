package com.example.thirdtool.Card.domain.model;

/**
 * 카드 ARCHIVE 전환 사유.
 *
 * <p>Story-CARD-E2-S2-3 — 이중 게이트(maxView + maxDuration) 폐기에 따라 재정의.
 * OnFieldBudget 개념이 사라지고 스케줄 소진(SCHEDULE_EXHAUSTED)과 모드 다운시프트(MODE_DOWNGRADED)만 남는다.
 * Archive 판정 자체는 M5 DailyLearningBatch로 이관되며, 도메인은 사유만 표현한다.
 */
public enum ArchiveReason {

    /**
     * 유저가 직접 Archive로 이동한 경우.
     * 수동 Archive API 호출 시 기록된다.
     */
    MANUAL,

    /**
     * 카드 생성 시점의 mode 인터벌이 소진된 경우.
     * 예: `createdMode = MODE_7D`인 카드가 7일 경과 → 스케줄 소진.
     * M5 DailyLearningBatch가 lazy로 판정한다.
     */
    SCHEDULE_EXHAUSTED,

    /**
     * 사용자 모드가 다운시프트되어 카드 스케줄이 즉시 만료된 경우.
     * 예: 사용자가 MODE_28D → MODE_7D로 축소했는데 이미 7일 초과한 카드.
     * PR#3 (Story-CARD-E3) Card.effectiveMaxDays 하이브리드 로직과 연동된다.
     */
    MODE_DOWNGRADED
}
