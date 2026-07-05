package com.example.thirdtool.Review.domain.model;

/**
 * ReviewSession 스코프 (LT E6 · M5 · Story 6-1).
 *
 * <p>사용자가 리뷰 세션을 시작하는 대상 범위:
 * <ul>
 *   <li>{@link #AXIS} — 단일 LearningAxis 스코프 · 기존 흐름</li>
 *   <li>{@link #LAYER} — LearningLayer 하위 여러 axis 카드를 통합한 세션 (신설)</li>
 * </ul>
 *
 * <p>ReviewSession의 {@code scopeId} 필드가 각 스코프의 대상 id를 담는다.
 *
 * <p>궤적 관리 (milestone.md § M5 PR#2 리스크):
 * S6-1·S6-2·S6-3의 scope enum·엔드포인트는 후속 issue-25 supersede로 PR#4 (Review E2)가 폐기 예정.
 * cross-layer 짬뽕 큐 방향으로 재편 · DailyLearningBatch 기반 통합 흐름 예정.
 *
 * <p>저장 방식: {@code @Enumerated(EnumType.STRING)} + VARCHAR(10) CHECK 제약 (ADR002).
 */
public enum ReviewScope {
    AXIS,
    LAYER
}
