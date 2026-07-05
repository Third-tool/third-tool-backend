package com.example.thirdtool.Review.infrastructure;

import com.example.thirdtool.Review.domain.model.DailyLearningBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyLearningBatchJpaRepository extends JpaRepository<DailyLearningBatch, Long> {

    Optional<DailyLearningBatch> findByUserIdAndBatchDate(Long userId, LocalDate batchDate);

    @Query("""
            SELECT b FROM DailyLearningBatch b
            WHERE b.batchDate = :batchDate
              AND b.closedAt IS NULL
            """)
    List<DailyLearningBatch> findAllOpenByBatchDate(@Param("batchDate") LocalDate batchDate);

    List<DailyLearningBatch> findByUserIdAndBatchDateBetweenOrderByBatchDateDesc(
            Long userId, LocalDate from, LocalDate to);
}
