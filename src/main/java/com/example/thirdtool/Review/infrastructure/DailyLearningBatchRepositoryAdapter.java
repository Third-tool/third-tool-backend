package com.example.thirdtool.Review.infrastructure;

import com.example.thirdtool.Review.domain.model.DailyLearningBatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DailyLearningBatchRepositoryAdapter implements DailyLearningBatchRepository {

    private final DailyLearningBatchJpaRepository jpa;

    @Override
    public DailyLearningBatch save(DailyLearningBatch batch) {
        return jpa.save(batch);
    }

    @Override
    public Optional<DailyLearningBatch> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<DailyLearningBatch> findByUserIdAndBatchDate(Long userId, LocalDate batchDate) {
        return jpa.findByUserIdAndBatchDate(userId, batchDate);
    }

    @Override
    public List<DailyLearningBatch> findAllOpenByBatchDate(LocalDate batchDate) {
        return jpa.findAllOpenByBatchDate(batchDate);
    }

    @Override
    public List<DailyLearningBatch> findByUserIdAndBatchDateBetweenOrderByBatchDateDesc(
            Long userId, LocalDate from, LocalDate to) {
        return jpa.findByUserIdAndBatchDateBetweenOrderByBatchDateDesc(userId, from, to);
    }
}
