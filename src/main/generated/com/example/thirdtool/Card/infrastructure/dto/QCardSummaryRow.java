package com.example.thirdtool.Card.infrastructure.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.example.thirdtool.Card.infrastructure.dto.QCardSummaryRow is a Querydsl Projection type for CardSummaryRow
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QCardSummaryRow extends ConstructorExpression<CardSummaryRow> {

    private static final long serialVersionUID = -680668256L;

    public QCardSummaryRow(com.querydsl.core.types.Expression<Long> cardId, com.querydsl.core.types.Expression<String> summaryValue, com.querydsl.core.types.Expression<com.example.thirdtool.Card.domain.model.MainContentType> contentType, com.querydsl.core.types.Expression<Long> keywordCount) {
        super(CardSummaryRow.class, new Class<?>[]{long.class, String.class, com.example.thirdtool.Card.domain.model.MainContentType.class, long.class}, cardId, summaryValue, contentType, keywordCount);
    }

}

