package com.example.thirdtool.LegacyCard.Card.presentation.dto.request;

import com.example.thirdtool.LegacyCard.Card.domain.model.CardRankType;
import com.example.thirdtool.LegacyCard.Card.domain.model.FeedbackType;

public record FeedbackRequestDto(
        Long cardId,
        FeedbackType feedback,     // ex: AGAIN, HARD, NORMAL, GOOD
        CardRankType rankType      // ex: SILVER, GOLD, DIAMOND
) {}