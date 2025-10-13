package com.example.thirdtool.Card.presentation.dto.request;

import com.example.thirdtool.Deck.domain.model.DeckMode;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.util.Optional;

@Getter
public class CardLearningRequestDto {
    private Long userId;
    private Long deckId;
    private DeckMode mode;
    private String rankName;
    private Optional<Long> cardId; // Optional로 변경하여 null 가능성을 명시

    public CardLearningRequestDto(Long userId, Long deckId, DeckMode mode, String rankName, Optional<Long> cardId) {
        this.userId = userId;
        this.deckId = deckId;
        this.mode = mode;
        this.rankName = rankName;
        this.cardId = cardId;
    }

    // cardId가 없는 경우를 위한 생성자
    public CardLearningRequestDto(Long userId, Long deckId, DeckMode mode, String rankName) {
        this(userId, deckId, mode, rankName, Optional.empty());
    }

}