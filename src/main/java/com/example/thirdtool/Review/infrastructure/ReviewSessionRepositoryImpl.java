package com.example.thirdtool.Review.infrastructure;

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

/**
 * REV E2 · Story 2-6 — QueryDSL 재편. deck 조인 제거 · batch 참조 기반으로 단순화.
 */
@RequiredArgsConstructor
public class ReviewSessionRepositoryImpl implements ReviewSessionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QReviewSession reviewSession = QReviewSession.reviewSession;

    @Override
    public List<ReviewSessionSummaryRow> searchSessions(ReviewSessionSearchCondition condition) {
        return queryFactory
                .select(new QReviewSessionSummaryRow(
                        reviewSession.id,
                        reviewSession.batch.id,
                        reviewSession.totalCardCount,
                        reviewSession.availableCardCount,
                        reviewSession.startedAt,
                        reviewSession.finishedAt
                ))
                .from(reviewSession)
                .where(
                        userIdEq(condition.getUserId()),
                        batchIdEq(condition.getBatchId())
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

    private BooleanExpression batchIdEq(Long batchId) {
        return batchId != null ? reviewSession.batch.id.eq(batchId) : null;
    }
}
