package com.example.thirdtool.Card.infrastructure.dto;

import com.example.thirdtool.Card.domain.model.MainContentType;

// 내부 내용이 바뀌지 않을 것 같습니다!
public record CardSearchCondition(String summaryKeyword, MainContentType contentType) {
    static CardSearchCondition empty() { return new CardSearchCondition(null, null); }
}
