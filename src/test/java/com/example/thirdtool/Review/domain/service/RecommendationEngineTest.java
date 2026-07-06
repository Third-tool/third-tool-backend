package com.example.thirdtool.Review.domain.service;

import com.example.thirdtool.Review.application.config.RecommendationProperties;
import com.example.thirdtool.Review.domain.model.DailyLearningBatch;
import com.example.thirdtool.Review.domain.model.DailyCardEntry;
import com.example.thirdtool.Review.domain.model.RecommendationType;
import com.example.thirdtool.Review.infrastructure.DailyLearningBatchRepository;
import com.example.thirdtool.UserSchedule.application.service.UserScheduleQueryService;
import com.example.thirdtool.UserSchedule.domain.model.LearningMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * REV E3 · Story 3-2 · RecommendationEngine 규칙 판정 단위 테스트.
 */
@DisplayName("RecommendationEngine (REV E3 · Story 3-2)")
class RecommendationEngineTest {

    private DailyLearningBatchRepository batchRepository;
    private UserScheduleQueryService userScheduleQueryService;
    private RecommendationEngine engine;

    private static final RecommendationProperties PROPS =
            new RecommendationProperties(0.5, 3, 4);

    @BeforeEach
    void setUp() {
        batchRepository = mock(DailyLearningBatchRepository.class);
        userScheduleQueryService = mock(UserScheduleQueryService.class);
        engine = new RecommendationEngine(batchRepository, userScheduleQueryService, PROPS);
    }

    @Nested
    @DisplayName("DOWNGRADE 판정")
    class Downgrade {

        @Test
        @DisplayName("해피: 3주 평균 40% + MODE_14D → SUGGEST_DOWNGRADE(14D→7D)")
        void 저완료율_MODE_14D() {
            Long userId = 1L;
            LocalDate today = LocalDate.of(2026, 8, 1);
            List<DailyLearningBatch> pool = batches(0.4, 0.4, 0.4);
            when(userScheduleQueryService.currentMode(userId)).thenReturn(LearningMode.MODE_14D);
            when(batchRepository.findByUserIdAndBatchDateBetweenOrderByBatchDateDesc(
                    eq(userId), any(), eq(today)))
                    .thenReturn(pool);

            Optional<RecommendationEngine.Evaluation> result = engine.evaluate(userId, today);

            assertThat(result).isPresent();
            assertThat(result.get().type()).isEqualTo(RecommendationType.SUGGEST_DOWNGRADE);
            assertThat(result.get().fromMode()).isEqualTo(LearningMode.MODE_14D);
            assertThat(result.get().toMode()).isEqualTo(LearningMode.MODE_7D);
        }

        @Test
        @DisplayName("엣지: MODE_7D 사용자 · 저완료율여도 DOWNGRADE 없음 (더 낮은 mode 없음)")
        void MODE_7D_저완료율_no_op() {
            Long userId = 1L;
            LocalDate today = LocalDate.now();
            List<DailyLearningBatch> pool = batches(0.3, 0.3);
            when(userScheduleQueryService.currentMode(userId)).thenReturn(LearningMode.MODE_7D);
            when(batchRepository.findByUserIdAndBatchDateBetweenOrderByBatchDateDesc(
                    eq(userId), any(), eq(today)))
                    .thenReturn(pool);

            Optional<RecommendationEngine.Evaluation> result = engine.evaluate(userId, today);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("엣지: 3주 평균 70% (임계값 이상) · 판정 없음")
        void 정상_완료율() {
            Long userId = 1L;
            LocalDate today = LocalDate.now();
            List<DailyLearningBatch> pool = batches(0.7, 0.75, 0.8);
            when(userScheduleQueryService.currentMode(userId)).thenReturn(LearningMode.MODE_14D);
            when(batchRepository.findByUserIdAndBatchDateBetweenOrderByBatchDateDesc(
                    eq(userId), any(), eq(today)))
                    .thenReturn(pool);

            Optional<RecommendationEngine.Evaluation> result = engine.evaluate(userId, today);

            // 70~80%는 downgrade 임계 아님 · 4주 UPGRADE도 부족 (not perfect)
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("UPGRADE 판정")
    class Upgrade {

        @Test
        @DisplayName("해피: 4주 batch 모두 perfect + MODE_14D → SUGGEST_UPGRADE(14D→28D)")
        void 완벽_MODE_14D() {
            Long userId = 1L;
            LocalDate today = LocalDate.now();
            List<DailyLearningBatch> pool = batches(1.0, 1.0, 1.0, 1.0);
            when(userScheduleQueryService.currentMode(userId)).thenReturn(LearningMode.MODE_14D);
            when(batchRepository.findByUserIdAndBatchDateBetweenOrderByBatchDateDesc(
                    eq(userId), any(), eq(today)))
                    .thenReturn(pool);

            Optional<RecommendationEngine.Evaluation> result = engine.evaluate(userId, today);

            assertThat(result).isPresent();
            assertThat(result.get().type()).isEqualTo(RecommendationType.SUGGEST_UPGRADE);
            assertThat(result.get().toMode()).isEqualTo(LearningMode.MODE_28D);
        }

        @Test
        @DisplayName("엣지: MODE_60D + 완벽 · UPGRADE 없음")
        void MODE_60D_완벽_no_op() {
            Long userId = 1L;
            LocalDate today = LocalDate.now();
            List<DailyLearningBatch> pool = batches(1.0, 1.0, 1.0, 1.0);
            when(userScheduleQueryService.currentMode(userId)).thenReturn(LearningMode.MODE_60D);
            when(batchRepository.findByUserIdAndBatchDateBetweenOrderByBatchDateDesc(
                    eq(userId), any(), eq(today)))
                    .thenReturn(pool);

            Optional<RecommendationEngine.Evaluation> result = engine.evaluate(userId, today);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Empty 판정")
    class Empty {

        @Test
        @DisplayName("엣지: batch 없음 · Optional.empty")
        void batch_없음() {
            Long userId = 1L;
            LocalDate today = LocalDate.now();
            when(userScheduleQueryService.currentMode(userId)).thenReturn(LearningMode.MODE_14D);
            when(batchRepository.findByUserIdAndBatchDateBetweenOrderByBatchDateDesc(
                    eq(userId), any(), eq(today)))
                    .thenReturn(List.of());

            Optional<RecommendationEngine.Evaluation> result = engine.evaluate(userId, today);

            assertThat(result).isEmpty();
        }
    }

    private static List<DailyLearningBatch> batches(double... ratios) {
        List<DailyLearningBatch> list = new ArrayList<>();
        for (double ratio : ratios) {
            list.add(batchWithRatio(ratio));
        }
        return list;
    }

    /** ratio에 맞게 entries · viewed 개수를 세팅해 completionRatio가 정확히 나오도록 구성. */
    private static DailyLearningBatch batchWithRatio(double ratio) {
        DailyLearningBatch batch = mock(DailyLearningBatch.class);
        when(batch.completionRatio()).thenReturn(ratio);
        when(batch.isPerfectClear()).thenReturn(ratio >= 1.0);
        return batch;
    }
}
