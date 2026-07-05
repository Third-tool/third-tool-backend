package com.example.thirdtool.Review.application.scheduler;

import com.example.thirdtool.Review.domain.model.DailyLearningBatch;
import com.example.thirdtool.Review.infrastructure.DailyLearningBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REV E1 · M5 · Story 1-5 — 자정 close cron.
 *
 * <p>매일 00:05 KST에 어제 open 상태 batch를 모두 close.
 * 원칙: "그날 못 본 카드 = 그냥 지나감".
 * 멱등: 이미 closed batch에 대해서는 no-op.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyBatchCloseScheduler {

    private final DailyLearningBatchRepository repository;

    /**
     * Cron: 매일 00:05 KST (`0 5 0 * * *`).
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void closeYesterdaysBatches() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<DailyLearningBatch> openBatches = repository.findAllOpenByBatchDate(yesterday);

        if (openBatches.isEmpty()) {
            log.info("[DailyBatchCloseScheduler] 어제({}) open batch 없음 · no-op", yesterday);
            return;
        }

        LocalDateTime closedAt = LocalDateTime.now();
        for (DailyLearningBatch batch : openBatches) {
            batch.close(closedAt);
            repository.save(batch);
        }
        log.info("[DailyBatchCloseScheduler] 어제({}) {}건 close 완료", yesterday, openBatches.size());
    }
}
