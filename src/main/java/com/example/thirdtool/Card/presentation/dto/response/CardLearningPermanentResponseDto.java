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
public class CardLearningPermanentResponseDto {

    private CardInfo mainCard;
    private List<CardInfo> recommendedCards;

    public static CardLearningPermanentResponseDto of(Card mainCard,
                                                      List<Card> recommended,
                                                      List<CardImageDto> images) {
        return CardLearningPermanentResponseDto.builder()
                                               .mainCard(CardInfo.of(mainCard, images))
                                               .recommendedCards(recommended.stream()
                                                                            .map(CardInfo::ofBasic)
                                                                            .toList())
                                               .build();
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CardInfo {
        private Long id;
        private String question;
        private String answer;
        private List<CardImageDto> images;

        public static CardInfo of(Card card, List<CardImageDto> images) {
            return CardInfo.builder()
                           .id(card.getId())
                           .question(card.getQuestion())
                           .answer(card.getAnswer())
                           .images(images)
                           .build();
        }

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