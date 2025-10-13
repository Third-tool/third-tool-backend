package com.example.thirdtool.Card.presentation.dto;

import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.example.thirdtool.User.domain.model.UserEntity;

public record RankBasedLearningRequestDto(Long userId,
                                          Long deckId,
                                          String rankName,
                                          DeckMode mode,
                                          int count) {
    public static RankBasedLearningRequestDto of(UserEntity user,
                                                   Long deckId,
                                                   String rankName,
                                                   DeckMode mode,
                                                   int count) {
        return new RankBasedLearningRequestDto(
                user.getId(),
                deckId,
                rankName,
                mode,
                count
        );
    }
}
