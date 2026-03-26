package com.example.thirdtool.Review.infrastructure;

import com.example.thirdtool.Deck.domain.model.QDeck;
import com.example.thirdtool.Review.domain.model.QReviewSession;
import com.example.thirdtool.Review.domain.model.ReviewSession;
import com.example.thirdtool.Review.infrastructure.dto.QReviewSessionSummaryRow;
import com.example.thirdtool.Review.infrastructure.dto.ReviewSessionSearchCondition;
import com.example.thirdtool.Review.infrastructure.dto.ReviewSessionSummaryRow;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ReviewSessionRepositoryImpl implements ReviewSessionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QReviewSession reviewSession = QReviewSession.reviewSession;
    private final QDeck deck = QDeck.deck;

    @Override
    public List<ReviewSessionSummaryRow> searchSessions(ReviewSessionSearchCondition condition) {
        return queryFactory
                .select(new QReviewSessionSummaryRow(
                        reviewSession.id,
                        deck.id,
                        deck.name,
                        reviewSession.cardReviews.size(),
                        reviewSession.startedAt
                ))
                .from(reviewSession)
                .join(reviewSession.deck, deck)
                .where(
                        userIdEq(condition.getUserId()),
                        deckIdEq(condition.getDeckId())
                      )
                .orderBy(reviewSession.startedAt.desc())
                .fetch();
    }

    @Override
    public Optional<ReviewSession> findByIdAndUserId(Long sessionId, Long userId) {
        ReviewSession result = queryFactory
                .selectFrom(reviewSession)
                .where(
                        reviewSession.id.eq(sessionId),
                        reviewSession.user.id.eq(userId)
                      )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    // -------------------------------------------------------
    // 동적 조건 메서드
    // -------------------------------------------------------

    private BooleanExpression userIdEq(Long userId) {
        return userId != null ? reviewSession.user.id.eq(userId) : null;
    }

    private BooleanExpression deckIdEq(Long deckId) {
        return deckId != null ? reviewSession.deck.id.eq(deckId) : null;
    }
}