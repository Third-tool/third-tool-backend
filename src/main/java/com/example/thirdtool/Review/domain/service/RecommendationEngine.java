package com.example.thirdtool.Review.domain.service;

import com.example.thirdtool.Review.application.config.RecommendationProperties;
import com.example.thirdtool.Review.domain.model.DailyLearningBatch;
import com.example.thirdtool.Review.domain.model.RecommendationType;
import com.example.thirdtool.Review.infrastructure.DailyLearningBatchRepository;
import com.example.thirdtool.UserSchedule.application.service.UserScheduleQueryService;
import com.example.thirdtool.UserSchedule.domain.model.LearningMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * RecommendationEngine — REV E3 · Story 3-2 · 규칙 기반 mode 조정 판정.
 *
 * <p>규칙:
 * <ul>
 *   <li>최근 N주 batch 평균 완료율 &lt; downgrade-threshold-ratio → SUGGEST_DOWNGRADE (한 단계 아래 mode)</li>
 *   <li>최근 M주 모든 batch perfect + 사용자 mode &lt; MODE_60D → SUGGEST_UPGRADE (한 단계 위 mode)</li>
 *   <li>MODE_7D 사용자에게 DOWNGRADE 없음 (더 낮은 mode 없음)</li>
 *   <li>MODE_60D 사용자에게 UPGRADE 없음 (더 높은 mode 없음)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class RecommendationEngine {

    private final DailyLearningBatchRepository batchRepository;
    private final UserScheduleQueryService userScheduleQueryService;
    private final RecommendationProperties properties;

    public Optional<Evaluation> evaluate(Long userId, LocalDate asOf) {
        LearningMode userMode = userScheduleQueryService.currentMode(userId);

        // 1) DOWNGRADE 판정 우선
        Optional<Evaluation> downgrade = evaluateDowngrade(userId, asOf, userMode);
        if (downgrade.isPresent()) return downgrade;

        // 2) UPGRADE 판정
        return evaluateUpgrade(userId, asOf, userMode);
    }

    private Optional<Evaluation> evaluateDowngrade(Long userId, LocalDate asOf, LearningMode userMode) {
        LearningMode downgraded = downgradeOf(userMode);
        if (downgraded == null) return Optional.empty();  // MODE_7D — 더 낮은 mode 없음

        LocalDate from = asOf.minusWeeks(properties.downgradeWindowWeeks());
        List<DailyLearningBatch> batches = batchRepository
                .findByUserIdAndBatchDateBetweenOrderByBatchDateDesc(userId, from, asOf);
        if (batches.isEmpty()) return Optional.empty();

        double avgCompletion = batches.stream()
                .mapToDouble(DailyLearningBatch::completionRatio)
                .average()
                .orElse(0.0);

        if (avgCompletion < properties.downgradeThresholdRatio()) {
            String reason = String.format(
                    "최근 %d주 평균 완료율 %.0f%% (%.0f%% 미만)",
                    properties.downgradeWindowWeeks(),
                    avgCompletion * 100,
                    properties.downgradeThresholdRatio() * 100);
            return Optional.of(new Evaluation(
                    RecommendationType.SUGGEST_DOWNGRADE, userMode, downgraded, reason));
        }
        return Optional.empty();
    }

    private Optional<Evaluation> evaluateUpgrade(Long userId, LocalDate asOf, LearningMode userMode) {
        LearningMode upgraded = upgradeOf(userMode);
        if (upgraded == null) return Optional.empty();  // MODE_60D — 더 높은 mode 없음

        LocalDate from = asOf.minusWeeks(properties.upgradeWindowWeeks());
        List<DailyLearningBatch> batches = batchRepository
                .findByUserIdAndBatchDateBetweenOrderByBatchDateDesc(userId, from, asOf);
        if (batches.isEmpty()) return Optional.empty();

        boolean allPerfect = batches.stream().allMatch(DailyLearningBatch::isPerfectClear);
        if (allPerfect) {
            String reason = String.format(
                    "최근 %d주 모든 batch 완벽 클리어", properties.upgradeWindowWeeks());
            return Optional.of(new Evaluation(
                    RecommendationType.SUGGEST_UPGRADE, userMode, upgraded, reason));
        }
        return Optional.empty();
    }

    /** MODE_60D → MODE_28D → MODE_14D → MODE_7D → null. */
    static LearningMode downgradeOf(LearningMode mode) {
        return switch (mode) {
            case MODE_60D -> LearningMode.MODE_28D;
            case MODE_28D -> LearningMode.MODE_14D;
            case MODE_14D -> LearningMode.MODE_7D;
            case MODE_7D -> null;
        };
    }

    /** MODE_7D → MODE_14D → MODE_28D → MODE_60D → null. */
    static LearningMode upgradeOf(LearningMode mode) {
        return switch (mode) {
            case MODE_7D -> LearningMode.MODE_14D;
            case MODE_14D -> LearningMode.MODE_28D;
            case MODE_28D -> LearningMode.MODE_60D;
            case MODE_60D -> null;
        };
    }

    /** 판정 결과 · Application Service가 Recommendation Entity로 저장. */
    public record Evaluation(
            RecommendationType type,
            LearningMode fromMode,
            LearningMode toMode,
            String reason
    ) {}
}
