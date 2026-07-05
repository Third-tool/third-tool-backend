package com.example.thirdtool.Review.infrastructure;

import com.example.thirdtool.Review.domain.model.DailyLearningBatch;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * DailyLearningBatch Repository port (REV E1 · M5 · Story 1-3).
 */
public interface DailyLearningBatchRepository {

    DailyLearningBatch save(DailyLearningBatch batch);

    Optional<DailyLearningBatch> findById(Long id);

    /** 특정 사용자 · 특정 날짜의 batch 조회 (UNIQUE 기반). */
    Optional<DailyLearningBatch> findByUserIdAndBatchDate(Long userId, LocalDate batchDate);

    /** Story 1-5 · 자정 close cron — 특정 날짜에 open 상태인 batch 목록. */
    List<DailyLearningBatch> findAllOpenByBatchDate(LocalDate batchDate);

    /** Story 1-3 · queryHistory — 사용자 · 기간 batch 목록 (batchDate DESC). */
    List<DailyLearningBatch> findByUserIdAndBatchDateBetweenOrderByBatchDateDesc(
            Long userId, LocalDate from, LocalDate to);
}
