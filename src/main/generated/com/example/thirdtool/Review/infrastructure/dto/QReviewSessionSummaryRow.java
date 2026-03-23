package com.example.thirdtool.Review.infrastructure.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.example.thirdtool.Review.infrastructure.dto.QReviewSessionSummaryRow is a Querydsl Projection type for ReviewSessionSummaryRow
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QReviewSessionSummaryRow extends ConstructorExpression<ReviewSessionSummaryRow> {

    private static final long serialVersionUID = 1792943406L;

    public QReviewSessionSummaryRow(com.querydsl.core.types.Expression<Long> sessionId, com.querydsl.core.types.Expression<Long> deckId, com.querydsl.core.types.Expression<String> deckName, com.querydsl.core.types.Expression<Integer> totalCardCount, com.querydsl.core.types.Expression<java.time.LocalDateTime> startedAt) {
        super(ReviewSessionSummaryRow.class, new Class<?>[]{long.class, long.class, String.class, int.class, java.time.LocalDateTime.class}, sessionId, deckId, deckName, totalCardCount, startedAt);
    }

}

