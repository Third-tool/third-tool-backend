package com.example.thirdtool.Card.presentation.dto.request;

import com.example.thirdtool.Card.domain.model.FeedbackType;

public record FeedbackRequestDto(
        Long cardId,
        FeedbackType feedback
) {}