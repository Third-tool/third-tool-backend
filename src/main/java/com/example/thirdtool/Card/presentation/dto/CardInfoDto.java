package com.example.thirdtool.Card.presentation.dto;

import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@ToString
public class CardInfoDto {

    private Long id;
    private String question;
    private String answer;

    @QueryProjection
    public CardInfoDto(Long id, String question, String answer) {
        this.id = id;
        this.question = question;
        this.answer = answer;
    }
}