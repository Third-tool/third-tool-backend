package com.example.thirdtool.Card.presentation.dto.response;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.presentation.dto.CardImageDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CardLearningResponseDto {

    private CardInfo mainCard;
    private List<CardInfo> recommendedCards;
    private int totalRemaining;

    public static CardLearningResponseDto of(Card mainCard,
                                             List<Card> recommended,
                                             int totalRemaining,
                                             List<CardImageDto> images) {
        return CardLearningResponseDto.builder()
                                      .mainCard(CardInfo.of(mainCard, images))
                                      .recommendedCards(recommended.stream()
                                                                   .map(CardInfo::ofBasic)
                                                                   .toList())
                                      .totalRemaining(totalRemaining)
                                      .build();
    }

    /** ✅ 내부용 카드 요약 DTO */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CardInfo {
        private Long id;
        private String question;
        private String answer;
        private List<CardImageDto> images;

        // 메인 카드용 (이미지 포함)
        public static CardInfo of(Card card, List<CardImageDto> images) {
            return CardInfo.builder()
                           .id(card.getId())
                           .question(card.getQuestion())
                           .answer(card.getAnswer())
                           .images(images)
                           .build();
        }

        // 추천 카드용 (이미지 제외)
        public static CardInfo ofBasic(Card card) {
            return CardInfo.builder()
                           .id(card.getId())
                           .question(card.getQuestion())
                           .answer(card.getAnswer())
                           .images(Collections.emptyList())
                           .build();
        }
    }
}