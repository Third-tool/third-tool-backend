package com.example.thirdtool.Card.presentation.dto.request;

public record CardRankUpdateRequestDto(
        String name, // 수정할 랭크의 이름 (예: "SILVER")
        int minScore,
        int maxScore
) {}
