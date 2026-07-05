package com.example.thirdtool.Review.application.service;

import com.example.thirdtool.Card.application.service.CardCommandService;
import com.example.thirdtool.Card.domain.model.ArchiveReason;
import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Review.domain.exception.ReviewSessionException;
import com.example.thirdtool.Review.domain.model.DailyLearningBatch;
import com.example.thirdtool.Review.infrastructure.DailyLearningBatchRepository;
import com.example.thirdtool.UserSchedule.application.service.UserScheduleQueryService;
import com.example.thirdtool.UserSchedule.domain.model.LearningMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DailyLearningBatchService — Story 1-3 + 1-4 통합.
 *
 * <p>Lazy 생성 · markViewed · queryHistory · generateFor 조율.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DailyLearningBatchService {

    private final DailyLearningBatchRepository repository;
    private final CardRepository cardRepository;
    private final CardCommandService cardCommandService;
    private final UserScheduleQueryService userScheduleQueryService;

    /**
     * Story 1-3 + 1-4 — 오늘 batch 조회 · 없으면 lazy 생성 · exhausted 카드 자동 archive.
     */
    public DailyLearningBatch getOrCreateToday(Long userId) {
        LocalDate today = LocalDate.now();
        return repository.findByUserIdAndBatchDate(userId, today)
                .orElseGet(() -> generateAndPersist(userId, today));
    }

    private DailyLearningBatch generateAndPersist(Long userId, LocalDate today) {
        LearningMode userMode = userScheduleQueryService.currentMode(userId);
        // 사용자의 활성 ON_FIELD 카드 전체 (도메인이 due/exhausted 판정)
        List<Card> allActive = cardRepository.findOnFieldEligibleByUserId(userId, LocalDateTime.MAX);

        DailyLearningBatch.GenerationResult result = DailyLearningBatch.generateFor(
                userId, today, userMode, allActive);

        DailyLearningBatch saved = repository.save(result.batch());

        // Story 1-4 · exhausted 카드 archive orchestration
        if (!result.exhaustedCards().isEmpty()) {
            List<Long> exhaustedIds = result.exhaustedCards().stream().map(Card::getId).toList();
            // Reason 판정 (SDD § Story 1-4)
            // createdMode.maxDays() > userCurrentMode.maxDays() → MODE_DOWNGRADED · else SCHEDULE_EXHAUSTED
            // 단순화: 첫 카드 기준 판정. 실제 다중 reason은 v0.1.1v 정교화.
            ArchiveReason reason = result.exhaustedCards().stream()
                    .findFirst()
                    .filter(c -> c.getCreatedMode().maxDays() > userMode.maxDays())
                    .map(c -> ArchiveReason.MODE_DOWNGRADED)
                    .orElse(ArchiveReason.SCHEDULE_EXHAUSTED);
            cardCommandService.archiveMany(exhaustedIds, reason);
        }

        return saved;
    }

    /**
     * Story 1-3 · markViewed — 오늘 batch의 카드 view 기록.
     * closed 상태면 DAILY_BATCH_CLOSED 예외.
     */
    public void markViewed(Long userId, Long cardId, LocalDateTime viewedAt) {
        DailyLearningBatch batch = repository.findByUserIdAndBatchDate(userId, LocalDate.now())
                .orElseThrow(() -> ReviewSessionException.of(
                        ErrorCode.DAILY_BATCH_NOT_FOUND, "userId=" + userId));
        batch.markViewed(cardId, viewedAt);
        repository.save(batch);
    }

    /**
     * Story 1-3 · queryHistory — 사용자 · 기간 batch 목록 (batchDate DESC).
     */
    public List<DailyLearningBatch> queryHistory(Long userId, LocalDate from, LocalDate to) {
        return repository.findByUserIdAndBatchDateBetweenOrderByBatchDateDesc(userId, from, to);
    }

    public DailyLearningBatch findByIdOwned(Long batchId, Long userId) {
        DailyLearningBatch batch = repository.findById(batchId)
                .orElseThrow(() -> ReviewSessionException.of(
                        ErrorCode.DAILY_BATCH_NOT_FOUND, "batchId=" + batchId));
        if (!batch.isOwner(userId)) {
            throw ReviewSessionException.of(ErrorCode.DAILY_BATCH_FORBIDDEN);
        }
        return batch;
    }
}
