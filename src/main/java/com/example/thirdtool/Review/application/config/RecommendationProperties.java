package com.example.thirdtool.Review.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * REV E3 · Story 3-6 — 추천 임계값 config.
 *
 * <p>{@code application.yml}에서 주입. 관찰 데이터 축적 후 튜닝 여지 확보.
 *
 * <pre>{@code
 * app:
 *   learning:
 *     recommendation:
 *       downgrade-threshold-ratio: 0.5
 *       downgrade-window-weeks: 3
 *       upgrade-window-weeks: 4
 * }</pre>
 */
@ConfigurationProperties(prefix = "app.learning.recommendation")
public record RecommendationProperties(
        double downgradeThresholdRatio,
        int downgradeWindowWeeks,
        int upgradeWindowWeeks
) {
    public RecommendationProperties {
        if (downgradeThresholdRatio < 0.0 || downgradeThresholdRatio > 1.0) {
            throw new IllegalArgumentException(
                    "downgrade-threshold-ratio는 0.0~1.0 사이여야 합니다. 값=" + downgradeThresholdRatio);
        }
        if (downgradeWindowWeeks <= 0 || upgradeWindowWeeks <= 0) {
            throw new IllegalArgumentException(
                    "window-weeks는 1 이상이어야 합니다.");
        }
    }
}
