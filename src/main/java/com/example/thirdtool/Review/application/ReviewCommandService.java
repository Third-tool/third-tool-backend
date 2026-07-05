package com.example.thirdtool.Review.application;

import com.example.thirdtool.Card.domain.event.CardViewedEvent;
import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Review.application.service.DailyLearningBatchService;
import com.example.thirdtool.Review.domain.exception.ReviewSessionException;
import com.example.thirdtool.Review.domain.model.DailyCardEntry;
import com.example.thirdtool.Review.domain.model.DailyLearningBatch;
import com.example.thirdtool.Review.domain.model.ReviewSession;
import com.example.thirdtool.Review.infrastructure.ReviewSessionRepository;
import com.example.thirdtool.Review.presentation.dto.ReviewResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ReviewCommandService — REV E2 · Story 2-1/2-2/2-3 재편.
 *
 * <p>PR#2 deck/axis/layer 스코프 startReview 폐기 · DailyLearningBatch 원천 세션 흐름으로 대체.
 * 트랜잭션 안에서 batch entry viewed 동기화 · 진행 중 세션 자동 finish 정책 강제.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewCommandService {

    private final ReviewSessionRepository reviewSessionRepository;
    private final ReviewQueryService      reviewQueryService;
    private final DailyLearningBatchService dailyLearningBatchService;

    private final CardRepository cardRepository;

    // Story-LT-E4-S4-5 — CardViewedEvent 발행용. 소비처는 M5 이관.
    private final ApplicationEventPublisher eventPublisher;

    // ─── 1. 리뷰 세션 시작 (Story 2-1 + 2-3) ─────────────────

    /**
     * REV E2 · Story 2-1 + 2-3 · POST /api/v1/review-sessions.
     *
     * <p>진행 중 세션 있으면 자동 finish → DailyLearningBatch(오늘) 조회/생성 → batch.unviewedEntries 기반 세션 생성.
     */
    public ReviewResponse.StartSession startSession(UserEntity user) {
        Long userId = user.getId();
        LocalDateTime now = LocalDateTime.now();

        // Story 2-3 — 진행 중 세션 자동 finish (멱등)
        reviewSessionRepository.findFirstByUserIdAndFinishedFalseOrderByStartedAtDesc(userId)
                .ifPresent(existing -> existing.finish(now));

        // Story 1-3 lazy 생성 계승 · 없으면 생성
        DailyLearningBatch batch = dailyLearningBatchService.getOrCreateToday(userId);

        // batch 미완료 entries → Card 리스트로 변환 (순서 유지, 삭제/archive 카드 제외)
        List<DailyCardEntry> unviewed = batch.unviewedEntries();
        List<Card> cards = new ArrayList<>();
        for (DailyCardEntry entry : unviewed) {
            cardRepository.findById(entry.getCardId())
                    .filter(c -> !c.isDeleted() && !c.isArchived())
                    .ifPresent(cards::add);
        }

        if (cards.isEmpty()) {
            throw ReviewSessionException.of(ErrorCode.DAILY_BATCH_HAS_NO_CARDS);
        }

        // 도메인 팩토리 — batch closed/soap 검증 내부
        ReviewSession session = ReviewSession.startFrom(batch, cards, user, now);
        reviewSessionRepository.save(session);

        // 첫 카드 view 기록 (batch entry viewedAt 동기화 포함)
        recordViewOnCurrent(session, now);

        return ReviewResponse.StartSession.of(session);
    }

    // ─── 2. 현재 카드 COMPARING 전환 ─────────────────────

    public ReviewResponse.CardReviewDto startComparing(Long sessionId, UserEntity user) {
        ReviewSession session = reviewQueryService.getSessionByOwner(sessionId, user);
        session.startComparingCurrentCard();
        return ReviewResponse.CardReviewDto.of(session.currentCardReview());
    }

    // ─── 3. 다음 카드로 이동 ──────────────────────────────

    public ReviewResponse.NextCard moveToNext(Long sessionId, UserEntity user) {
        ReviewSession session = reviewQueryService.getSessionByOwner(sessionId, user);

        session.moveToNext();

        if (!session.isFinished()) {
            recordViewOnCurrent(session, LocalDateTime.now());
        }

        return ReviewResponse.NextCard.of(session);
    }

    // ─── 4. 현재 카드 view 명시 기록 (Story 2-2) ─────────

    /**
     * REV E2 · Story 2-2 · POST /api/v1/review-sessions/{id}/record-view.
     *
     * <p>세션 상태(finished) 검증 후 card.recordView + batch.entry.viewedAt 동시 갱신.
     * batch가 closed 상태면 batch.markViewed가 DAILY_BATCH_CLOSED 던짐 → 전체 트랜잭션 롤백.
     */
    public void recordView(Long sessionId, UserEntity user) {
        ReviewSession session = reviewQueryService.getSessionByOwner(sessionId, user);
        recordViewOnCurrent(session, LocalDateTime.now());
    }

    // ─── 5. 세션 명시 finish (Story 2-3) ─────────────────

    public ReviewResponse.FinishSession finish(Long sessionId, UserEntity user) {
        ReviewSession session = reviewQueryService.getSessionByOwner(sessionId, user);
        session.finish(LocalDateTime.now());
        return ReviewResponse.FinishSession.of(session);
    }

    // ─── 내부 처리 ────────────────────────────────────────

    private void recordViewOnCurrent(ReviewSession session, LocalDateTime now) {
        Card card = session.currentCardReview().getCard();
        card.recordView();
        cardRepository.save(card);

        // Story 2-2 · batch entry viewedAt 동기화 · batch closed면 예외 → 트랜잭션 롤백
        session.getBatch().markViewed(card.getId(), now);

        // Story-LT-E4-S4-5 · axisId 이벤트 발행 궤적 유지
        eventPublisher.publishEvent(new CardViewedEvent(
                card.getId(),
                session.getUser().getId(),
                card.getAxisId()
        ));
    }
}
