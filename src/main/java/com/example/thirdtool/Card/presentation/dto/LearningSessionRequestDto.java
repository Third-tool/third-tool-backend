package com.example.thirdtool.Card.presentation.dto;

import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.example.thirdtool.User.domain.model.UserEntity;

// 학습 세션 요청 DTO
public record LearningSessionRequestDto(
        Long userId,
        Long deckId,
        DeckMode mode,
        int count
) {
    public static LearningSessionRequestDto of(UserEntity user, Long deckId, DeckMode mode, int count) {
        return new LearningSessionRequestDto(
                user.getId(),
                deckId,
                mode,
                count
        );
    }
}