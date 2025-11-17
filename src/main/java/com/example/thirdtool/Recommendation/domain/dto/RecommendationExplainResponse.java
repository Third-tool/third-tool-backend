package com.example.thirdtool.Recommendation.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecommendationExplainResponse {
    private final Long deckId;
    private final String reason;
}
