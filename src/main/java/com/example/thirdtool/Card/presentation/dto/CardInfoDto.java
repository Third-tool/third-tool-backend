package com.example.thirdtool.Card.presentation.dto;

import com.example.thirdtool.Deck.domain.model.DeckMode;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter // ✅ getter를 자동으로 생성
@NoArgsConstructor // ✅ 기본 생성자를 자동으로 생성
@ToString
public class CardInfoDto {

    private Long id;
    private String question;
    private String answer;
    private Integer score;
    private DeckMode mode;

    // ✅ 모든 필드를 포함하는 생성자 추가
    @Builder
    public CardInfoDto(Long id, String question, String answer, Integer score, DeckMode mode) {
        this.id = id;
        this.question = question;
        this.answer = answer;
        this.score = score;
        this.mode = mode;
    }
}