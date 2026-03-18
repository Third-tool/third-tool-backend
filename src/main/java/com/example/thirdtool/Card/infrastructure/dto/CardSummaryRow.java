package com.example.thirdtool.Card.infrastructure.dto;

import com.example.thirdtool.Card.domain.model.MainContentType;
import com.querydsl.core.annotations.QueryProjection;

public record CardSummaryRow(Long cardId, String summaryValue, MainContentType contentType, long keywordCount) {
    @QueryProjection
    public CardSummaryRow {}
}
