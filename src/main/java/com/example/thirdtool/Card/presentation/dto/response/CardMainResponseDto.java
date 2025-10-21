package com.example.thirdtool.Card.presentation.dto.response;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.presentation.dto.CardImageGroupDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CardMainResponseDto {
    private Long id;
    private String question;
    private String answer;
    private CardImageGroupDto images;

    public static CardMainResponseDto of(Card card, CardImageGroupDto imageGroup) {
        return CardMainResponseDto.builder()
                                  .id(card.getId())
                                  .question(card.getQuestion())
                                  .answer(card.getAnswer())
                                  .images(imageGroup)
                                  .build();
    }
}
