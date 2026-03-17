package com.example.thirdtool.Card.infrastructure.dto;

import com.example.thirdtool.Card.domain.model.MainContentType;

record CardSummaryRow(Long cardId, String summaryValue, MainContentType contentType, long keywordCount) {}
