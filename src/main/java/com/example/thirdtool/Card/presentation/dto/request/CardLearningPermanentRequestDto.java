package com.example.thirdtool.Card.presentation.dto.request;


import lombok.Getter;

import java.util.Optional;

@Getter
public class CardLearningPermanentRequestDto {
    private Long userId;
    private Long deckId;
    private Optional<Long> cardId; // 특정 카드 학습에만 사용되는 선택적 필드

    // cardId를 포함하는 생성자
    public CardLearningPermanentRequestDto(Long userId, Long deckId, Optional<Long> cardId) {
        this.userId = userId;
        this.deckId = deckId;
        this.cardId = cardId;
    }

    // cardId가 없는 경우를 위한 생성자
    public CardLearningPermanentRequestDto(Long userId, Long deckId) {
        this.userId = userId;
        this.deckId = deckId;
        this.cardId = Optional.empty(); // cardId가 없음을 명시적으로 표현
    }
}