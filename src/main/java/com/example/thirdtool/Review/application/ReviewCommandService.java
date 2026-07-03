package com.example.thirdtool.Review.application;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.application.service.DeckQueryService;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Review.domain.exception.ReviewSessionException;
import com.example.thirdtool.Review.domain.model.ReviewSession;
import com.example.thirdtool.Review.infrastructure.ReviewSessionRepository;
import com.example.thirdtool.Review.presentation.dto.ReviewRequest;
import com.example.thirdtool.Review.presentation.dto.ReviewResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ReviewCommandService — Story-CARD-E2-S2-4 재작성.
 *
 * <p>OnFieldBudget 폐기(PR#2)로 이중 게이트(maxView + maxDuration) archive 로직이 사라짐.
 * 리뷰 세션은 viewCount만 기록하고, ARCHIVE 결정은 M5 DailyLearningBatch가 lazy 판정한다.
 * `isLastView`는 API 호환을 위해 응답 필드로 남되 항상 {@code false}로 전달된다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewCommandService {

    private final ReviewSessionRepository reviewSessionRepository;
    private final ReviewQueryService      reviewQueryService;
    private final DeckQueryService        deckQueryService;

    // Card BC 의존 — port interface를 통한 접근 (ADR-006)
    private final CardRepository cardRepository;

    // ─── 1. 리뷰 세션 시작 ───────────────────────────────

    public ReviewResponse.StartSession startReview(ReviewRequest.StartSession request, UserEntity user) {
        Deck deck = deckQueryService.getActiveDeck(request.deckId());

        if (!deck.getUser().getId().equals(user.getId())) {
            throw ReviewSessionException.of(ErrorCode.REVIEW_SESSION_FORBIDDEN);
        }

        // 카드 목록을 Application Service에서 조회해 ReviewSession에 전달한다.
        // ReviewSession이 deck.getCards()를 직접 호출하지 않도록 해 N+1 제어권을 유지한다.
        List<Card> cards = cardRepository.findAllByDeckIdAndDeletedFalse(deck.getId());

        // 카드 0개 검증은 ReviewSession.of() 도메인 내부에서 처리 (REVIEW002)
        ReviewSession session = ReviewSession.of(deck, cards, user, cards.size());
        reviewSessionRepository.save(session);

        // 첫 번째 카드 진입 처리 (viewCount 증가만). Archive 판정은 M5로 이관.
        recordViewOnCurrent(session);

        return ReviewResponse.StartSession.of(session, false);
    }

    // ─── 2. 현재 카드 COMPARING 전환 ─────────────────────

    public ReviewResponse.CardReviewDto startComparing(Long sessionId, UserEntity user) {
        ReviewSession session = reviewQueryService.getSessionByOwner(sessionId, user);
        session.startComparingCurrentCard();
        return ReviewResponse.CardReviewDto.of(session.currentCardReview(), false);
    }

    // ─── 3. 다음 카드로 이동 ──────────────────────────────

    public ReviewResponse.NextCard moveToNext(Long sessionId, UserEntity user) {
        ReviewSession session = reviewQueryService.getSessionByOwner(sessionId, user);

        // 종료 여부 + COMPARING 검증은 도메인 내부에서 처리
        session.moveToNext();

        if (!session.isFinished()) {
            recordViewOnCurrent(session);
        }

        return ReviewResponse.NextCard.of(session, false);
    }

    // ─── 내부 처리 ────────────────────────────────────────

    private void recordViewOnCurrent(ReviewSession session) {
        Card card = session.currentCardReview().getCard();
        card.recordView();
        cardRepository.save(card);
    }
}
