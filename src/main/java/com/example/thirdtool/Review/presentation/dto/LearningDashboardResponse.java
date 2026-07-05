package com.example.thirdtool.Review.presentation.dto;

import com.example.thirdtool.Review.domain.model.DailyLearningBatch;
import com.example.thirdtool.Review.domain.model.Recommendation;
import com.example.thirdtool.UserSchedule.domain.model.LearningMode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * REV E3 · Story 3-1/3-4 — 대시보드 응답.
 */
public record LearningDashboardResponse(
        TodaySummary today,
        Window recent7Days,
        Window recent30Days,
        Streak streak,
        RecommendationDto recommendation  // nullable
) {
    public record TodaySummary(
            Long batchId,
            LocalDate batchDate,
            int totalCards,
            int viewedCards,
            double completionRatio,
            boolean isClosed
    ) {
        public static TodaySummary of(DailyLearningBatch batch) {
            return new TodaySummary(
                    batch.getId(),
                    batch.getBatchDate(),
                    batch.totalCount(),
                    batch.viewedCount(),
                    batch.completionRatio(),
                    batch.isClosed()
            );
        }
    }

    public record Window(
            int windowDays,
            int batchCount,
            double avgCompletionRatio,
            int perfectClearDays
    ) {}

    public record Streak(
            int current,
            int longest
    ) {}

    public record RecommendationDto(
            Long recommendationId,
            String type,           // SUGGEST_DOWNGRADE / SUGGEST_UPGRADE
            LearningMode fromMode,
            LearningMode toMode,
            String reason,
            LocalDateTime triggeredAt
    ) {
        public static RecommendationDto of(Recommendation r) {
            return new RecommendationDto(
                    r.getId(),
                    r.getType().name(),
                    r.getFromMode(),
                    r.getToMode(),
                    r.getReason(),
                    r.getTriggeredAt()
            );
        }
    }
}
