package com.example.thirdtool.Review.domain.service;

import com.example.thirdtool.Review.domain.model.DailyLearningBatch;
import com.example.thirdtool.Review.infrastructure.DailyLearningBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * REV E1 · M5 · Story 1-6 — 사용자 batch streak realtime 계산.
 *
 * <p>대시보드 캐시 필드 없이 realtime · Repository 쿼리 최적화 (dateRange bulk 조회 N+1 방지).
 */
@Component
@RequiredArgsConstructor
public class BatchStreakCalculator {

    private final DailyLearningBatchRepository repository;

    /** 기본 조회 창 · 최대 365일 소급. */
    private static final int MAX_LOOKBACK = 365;

    /**
     * asOf부터 뒤로 하루씩 조회 · isPerfectClear() 연속 카운트.
     * break 되는 순간 종료.
     */
    public int currentStreak(Long userId, LocalDate asOf) {
        LocalDate from = asOf.minusDays(MAX_LOOKBACK);
        List<DailyLearningBatch> history = repository
                .findByUserIdAndBatchDateBetweenOrderByBatchDateDesc(userId, from, asOf);

        Map<LocalDate, DailyLearningBatch> byDate = history.stream()
                .collect(Collectors.toMap(DailyLearningBatch::getBatchDate, Function.identity()));

        int streak = 0;
        LocalDate cursor = asOf;
        while (streak <= MAX_LOOKBACK) {
            DailyLearningBatch batch = byDate.get(cursor);
            if (batch == null || !batch.isPerfectClear()) break;
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    /**
     * asOf 기준 최근 window 일 내 최장 perfect clear 연속.
     */
    public int longestStreak(Long userId, LocalDate asOf, int window) {
        LocalDate from = asOf.minusDays(window);
        List<DailyLearningBatch> history = repository
                .findByUserIdAndBatchDateBetweenOrderByBatchDateDesc(userId, from, asOf);

        Map<LocalDate, DailyLearningBatch> byDate = history.stream()
                .collect(Collectors.toMap(DailyLearningBatch::getBatchDate, Function.identity()));

        int longest = 0;
        int current = 0;
        for (int i = 0; i <= window; i++) {
            LocalDate day = from.plusDays(i);
            DailyLearningBatch batch = byDate.get(day);
            if (batch != null && batch.isPerfectClear()) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 0;
            }
        }
        return longest;
    }
}
