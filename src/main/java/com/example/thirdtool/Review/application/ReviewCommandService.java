package com.example.thirdtool.Review.application;

import com.example.thirdtool.Card.domain.model.*;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.application.service.DeckQueryService;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Review.domain.exception.ReviewSessionException;
import com.example.thirdtool.Review.domain.model.ReviewSession;
import com.example.thirdtool.Review.infrastructure.ReviewSessionRepository;
import com.example.thirdtool.Review.presentation.dto.ReviewRequest;
import com.example.thirdtool.Review.presentation.dto.ReviewResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewCommandService {

    private final ReviewSessionRepository reviewSessionRepository;
    private final ReviewQueryService      reviewQueryService;
    private final DeckQueryService        deckQueryService;
    private final UserRepository          userRepository;

    // Card BC 의존 — port interface를 통한 접근 (ADR-006)
    private final CardRepository cardRepository;
    private final CardStatusHistoryAppender historyAppender;

    // 시스템 수준 ON_FIELD 체류 예산 (SystemBudgetConfig에서 주입)
    private final OnFieldBudget systemBudget;

    // ─── 1. 리뷰 세션 시작 ───────────────────────────────

    public ReviewResponse.StartSession startReview(ReviewRequest.StartSession request, Long userId) {
        Deck deck = deckQueryService.getActiveDeck(request.deckId());

        if (!deck.getUser().getId().equals(userId)) {
            throw ReviewSessionException.of(ErrorCode.REVIEW_SESSION_FORBIDDEN);
        }

        UserEntity user = userRepository.findById(userId)
                                        .orElseThrow(() -> new IllegalStateException(
                                                "인증된 사용자를 찾을 수 없습니다: " + userId));

        // 카드 목록을 Application Service에서 조회해 ReviewSession에 전달한다.
        // ReviewSession이 deck.getCards()를 직접 호출하지 않도록 해 N+1 제어권을 유지한다.
        List<Card> cards = cardRepository.findAllByDeckIdAndDeletedFalse(deck.getId());

        // 카드 0개 검증은 ReviewSession.of() 도메인 내부에서 처리 (REVIEW002)
        ReviewSession session = ReviewSession.of(deck, cards, user);
        reviewSessionRepository.save(session);

        // 첫 번째 카드 진입 처리 (viewCount 증가 + maxView 도달 시 즉시 ARCHIVE)
        boolean isLastView = incrementViewAndHandleMaxView(session.currentCardReview().getCard());

        return ReviewResponse.StartSession.of(session, isLastView);
    }

    // ─── 2. 현재 카드 COMPARING 전환 ─────────────────────

    public ReviewResponse.CardReviewDto startComparing(Long sessionId, Long userId) {
        ReviewSession session = reviewQueryService.getSessionByOwner(sessionId, userId);
        session.startComparingCurrentCard();

        // isLastView는 카드 진입 시 이미 결정된 viewCount 상태를 그대로 읽는다.
        boolean isLastView = resolveIsLastView(session);
        return ReviewResponse.CardReviewDto.of(session.currentCardReview(), isLastView);
    }

    // ─── 3. 다음 카드로 이동 ──────────────────────────────

    public ReviewResponse.NextCard moveToNext(Long sessionId, Long userId) {
        ReviewSession session = reviewQueryService.getSessionByOwner(sessionId, userId);

        // 종료 여부 + COMPARING 검증은 도메인 내부에서 처리
        session.moveToNext();

        boolean isLastView = false;
        if (!session.isFinished()) {
            isLastView = incrementViewAndHandleMaxView(session.currentCardReview().getCard());
        }

        return ReviewResponse.NextCard.of(session, isLastView);
    }

    // ─── 내부 처리 ────────────────────────────────────────
    private boolean incrementViewAndHandleMaxView(Card card) {
        card.recordView();

        boolean isLastView = card.isLastView(systemBudget.getMaxView());
        if (isLastView) {
            // viewCount가 maxView에 도달 → 즉시 ARCHIVE 전환
            CardStatus before = card.getStatus();  // 항상 ON_FIELD (incrementViewCount는 ARCHIVE 무시)
            card.archive();
            CardStatus after = card.getStatus();
            historyAppender.append(card, before, after, ArchiveReason.MAX_VIEW);
        }
        cardRepository.save(card);
        return isLastView;
    }

    private boolean resolveIsLastView(ReviewSession session) {
        if (session.isFinished()) return false;
        return session.currentCardReview().getCard().isLastView(systemBudget.getMaxView());
    }
}