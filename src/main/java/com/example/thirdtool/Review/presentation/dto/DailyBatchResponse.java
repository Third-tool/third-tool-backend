package com.example.thirdtool.Review.presentation.dto;

import com.example.thirdtool.Review.domain.model.DailyCardEntry;
import com.example.thirdtool.Review.domain.model.DailyLearningBatch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DailyBatchResponse {

    /** REV E1 · Story 1-7 — batch 상세 응답. */
    public record Detail(
            Long batchId,
            LocalDate batchDate,
            LocalDateTime generatedAt,
            LocalDateTime closedAt,
            boolean isClosed,
            int totalCards,
            int viewedCards,
            double completionRatio,
            List<EntryDto> entries
    ) {
        public static Detail of(DailyLearningBatch batch) {
            return new Detail(
                    batch.getId(),
                    batch.getBatchDate(),
                    batch.getGeneratedAt(),
                    batch.getClosedAt(),
                    batch.isClosed(),
                    batch.totalCount(),
                    batch.viewedCount(),
                    batch.completionRatio(),
                    batch.getEntries().stream().map(EntryDto::of).toList()
            );
        }
    }

    /** REV E1 · Story 1-7 — 이력 조회 응답 (entries 미포함). */
    public record HistorySummary(
            Long batchId,
            LocalDate batchDate,
            boolean isClosed,
            int totalCards,
            int viewedCards,
            double completionRatio,
            boolean isPerfectClear
    ) {
        public static HistorySummary of(DailyLearningBatch batch) {
            return new HistorySummary(
                    batch.getId(),
                    batch.getBatchDate(),
                    batch.isClosed(),
                    batch.totalCount(),
                    batch.viewedCount(),
                    batch.completionRatio(),
                    batch.isPerfectClear()
            );
        }
    }

    public record EntryDto(
            Long cardId,
            int cardIntervalDay,
            LocalDateTime exposedAt,
            LocalDateTime viewedAt,
            boolean isViewed
    ) {
        public static EntryDto of(DailyCardEntry entry) {
            return new EntryDto(
                    entry.getCardId(),
                    entry.getCardIntervalDay(),
                    entry.getExposedAt(),
                    entry.getViewedAt(),
                    entry.isViewed()
            );
        }
    }
}
