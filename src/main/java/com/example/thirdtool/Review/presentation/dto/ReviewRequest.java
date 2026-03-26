package com.example.thirdtool.Review.presentation.dto;

import jakarta.validation.constraints.NotNull;

public class ReviewRequest {

    /**
     * POST /api/v1/reviews — 리뷰 세션 시작 요청.
     */
    public record StartSession(
            @NotNull(message = "deckId는 필수입니다.")
            Long deckId
    ) {}

    // PATCH /comparing, PATCH /next 는 Path Variable만 사용하므로 Request body 없음.
}

