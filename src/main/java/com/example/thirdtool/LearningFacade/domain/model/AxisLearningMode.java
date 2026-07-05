package com.example.thirdtool.LearningFacade.domain.model;

/**
 * LearningAxis 학습 모드.
 *
 * <p>Deck 폐기 (LT E5 · M5)에 따라 {@code DeckMode}에서 이관되며, SDD 우선 정책으로
 * <strong>재정의</strong>되었다: 기존 {@code ON_FIELD/ARCHIVE} → 신규 {@code STUDY/REVIEW}.
 *
 * <ul>
 *   <li>{@link #STUDY} — 학습 모드 (기존 ON_FIELD 계승). 카드 학습 진행 중.</li>
 *   <li>{@link #REVIEW} — 복습 모드 (기존 ARCHIVE 계승). 이미 학습 완료된 축의 복습.</li>
 * </ul>
 *
 * <p>V31 백필 정책: {@code deck.mode ON_FIELD → axis.mode STUDY}, {@code ARCHIVE → REVIEW}.
 *
 * <p>저장 방식: {@code @Enumerated(EnumType.STRING)} + VARCHAR(20) CHECK 제약 (ADR002).
 */
public enum AxisLearningMode {
    STUDY,
    REVIEW
}
