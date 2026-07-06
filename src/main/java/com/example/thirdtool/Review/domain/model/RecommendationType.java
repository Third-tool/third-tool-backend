package com.example.thirdtool.Review.domain.model;

/**
 * REV E3 · Story 3-2 — 규칙 기반 추천 유형.
 *
 * <ul>
 *   <li>{@link #SUGGEST_DOWNGRADE} — 최근 저완료율 · 한 단계 아래 mode 제안</li>
 *   <li>{@link #SUGGEST_UPGRADE} — 최근 완벽 클리어 · 한 단계 위 mode 제안</li>
 * </ul>
 */
public enum RecommendationType {
    SUGGEST_DOWNGRADE,
    SUGGEST_UPGRADE
}
