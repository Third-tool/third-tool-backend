package com.example.thirdtool.Review.infrastructure.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewSessionSearchCondition {

    private Long userId;   // 필수 (본인 세션만 조회)
    private Long deckId;   // 선택: 특정 덱 세션만 필터링
}

