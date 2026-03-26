package com.example.thirdtool.Review.application;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.application.service.DeckQueryService;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Review.domain.model.ReviewSession;
import com.example.thirdtool.Review.infrastructure.ReviewSessionRepository;
import com.example.thirdtool.Review.presentation.dto.ReviewRequest;
import com.example.thirdtool.Review.presentation.dto.ReviewResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewCommandService {

    private final ReviewSessionRepository reviewSessionRepository;
    private final ReviewQueryService reviewQueryService;
    private final DeckQueryService deckQueryService;
    private final UserRepository userRepository;

    // ─── 1. 리뷰 세션 시작 ───────────────────────────────
    public ReviewResponse.StartSession startReview(ReviewRequest.StartSession request, Long userId) {
        // 덱 존재·삭제 여부 검증은 DeckQueryService에 위임
        Deck deck = deckQueryService.getActiveDeck(request.deckId());

        // 본인 덱 여부 검증
        if (!deck.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.REVIEW_SESSION_FORBIDDEN);
        }

        UserEntity user = userRepository.findById(userId)
                                        .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다: " + userId));

        // 카드 0개 검증은 ReviewSession.start() 도메인 내부에서 처리
        ReviewSession session = ReviewSession.start(deck, user);
        reviewSessionRepository.save(session);

        return ReviewResponse.StartSession.of(session);
    }

    // ─── 2. 현재 카드 COMPARING 전환 ─────────────────────
    /**
     * 현재 카드를 RECALLING → COMPARING 단계로 전환한다.
     */
    public ReviewResponse.CardReviewDto startComparing(Long sessionId, Long userId) {
        ReviewSession session = reviewQueryService.getSessionByOwner(sessionId, userId);

        // 종료 여부 + COMPARING 전환은 도메인 내부에서 처리
        session.startComparingCurrentCard();

        return ReviewResponse.CardReviewDto.of(session.currentCardReview());
    }

    // ─── 3. 다음 카드로 이동 ──────────────────────────────
    /**
     * 현재 카드가 COMPARING 상태일 때만 다음 카드로 이동할 수 있다.
     * 마지막 카드에서 호출 시 isFinished = true, currentCard = null을 반환한다.
     *
     * <p>세션 없음 → REVIEW_SESSION_NOT_FOUND
     * <p>본인 세션 아님 → REVIEW_SESSION_FORBIDDEN
     * <p>이미 종료된 세션 → REVIEW_SESSION_ALREADY_FINISHED
     * <p>RECALLING 상태에서 호출 → REVIEW_NEXT_REQUIRES_COMPARING
     */
    public ReviewResponse.NextCard moveToNext(Long sessionId, Long userId) {
        ReviewSession session = reviewQueryService.getSessionByOwner(sessionId, userId);

        // 종료 여부 + COMPARING 검증은 ReviewSession.nextCard() 도메인 내부에서 처리
        session.nextCard();

        return ReviewResponse.NextCard.of(session);
    }
}