package com.example.thirdtool.Card.presentation.dto.request;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Optional;

@Getter
@Builder // 외부에서 직접 builder() 호출 막기
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CardLearningPermanentRequestDto {

    private final Long userId;
    private final Long deckId;
    private final Optional<Long> cardId; // 특정 카드 학습에만 사용되는 선택적 필드

    /** ✅ 기본 요청용 (cardId 없는 경우) */
    public static CardLearningPermanentRequestDto of(Long userId, Long deckId) {
        return CardLearningPermanentRequestDto.builder()
                                              .userId(userId)
                                              .deckId(deckId)
                                              .cardId(Optional.empty())
                                              .build();
    }

    /** ✅ 특정 카드 학습용 (cardId 있는 경우) */
    public static CardLearningPermanentRequestDto of(Long userId, Long deckId, Long cardId) {
        return CardLearningPermanentRequestDto.builder()
                                              .userId(userId)
                                              .deckId(deckId)
                                              .cardId(Optional.ofNullable(cardId))
                                              .build();
    }
}