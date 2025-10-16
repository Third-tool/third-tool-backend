package com.example.thirdtool.Card.presentation.dto.request;

import com.example.thirdtool.Card.domain.model.CardRankType;
import com.example.thirdtool.Card.domain.model.FeedbackType;

public record FeedbackRequestDto(
        Long cardId,
        FeedbackType feedback,     // ex: AGAIN, HARD, NORMAL, GOOD
        CardRankType rankType      // ex: SILVER, GOLD, DIAMOND
) {}