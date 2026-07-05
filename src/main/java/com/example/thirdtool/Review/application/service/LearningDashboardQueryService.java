package com.example.thirdtool.Review.application.service;

import com.example.thirdtool.Review.domain.model.DailyLearningBatch;
import com.example.thirdtool.Review.domain.model.Recommendation;
import com.example.thirdtool.Review.domain.service.BatchStreakCalculator;
import com.example.thirdtool.Review.infrastructure.DailyLearningBatchRepository;
import com.example.thirdtool.Review.infrastructure.RecommendationRepository;
import com.example.thirdtool.Review.presentation.dto.LearningDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * LearningDashboardQueryService — REV E3 · Story 3-1.
 *
 * <p>오늘 batch + 7일/30일 평균 + streak + 미해결 recommendation을 단일 응답으로 조합.
 * FE가 대시보드 화면 렌더링 시 단일 조회로 처리.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LearningDashboardQueryService {

    private final DailyLearningBatchRepository batchRepository;
    private final DailyLearningBatchService batchService;
    private final BatchStreakCalculator streakCalculator;
    private final RecommendationRepository recommendationRepository;

    public LearningDashboardResponse buildDashboard(Long userId, LocalDate asOf) {
        // 1) 오늘 batch — lazy 생성 위임
        DailyLearningBatch today = batchService.getOrCreateToday(userId);

        // 2) 최근 7일 · 30일 평균 완료율
        LearningDashboardResponse.Window recent7 = window(userId, asOf, 7);
        LearningDashboardResponse.Window recent30 = window(userId, asOf, 30);

        // 3) streak
        LearningDashboardResponse.Streak streak = new LearningDashboardResponse.Streak(
                streakCalculator.currentStreak(userId, asOf),
                streakCalculator.longestStreak(userId, asOf, 365)
        );

        // 4) 미해결 추천
        Optional<Recommendation> pending = recommendationRepository.findLatestUnresolvedByUserId(userId);
        LearningDashboardResponse.RecommendationDto recommendationDto = pending
                .map(LearningDashboardResponse.RecommendationDto::of)
                .orElse(null);

        return new LearningDashboardResponse(
                LearningDashboardResponse.TodaySummary.of(today),
                recent7,
                recent30,
                streak,
                recommendationDto
        );
    }

    private LearningDashboardResponse.Window window(Long userId, LocalDate asOf, int days) {
        LocalDate from = asOf.minusDays(days - 1);
        List<DailyLearningBatch> batches = batchRepository
                .findByUserIdAndBatchDateBetweenOrderByBatchDateDesc(userId, from, asOf);
        int count = batches.size();
        double avg = batches.stream()
                .mapToDouble(DailyLearningBatch::completionRatio)
                .average()
                .orElse(0.0);
        int perfectDays = (int) batches.stream()
                .filter(DailyLearningBatch::isPerfectClear)
                .count();
        return new LearningDashboardResponse.Window(days, count, avg, perfectDays);
    }
}
