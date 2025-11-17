package com.example.thirdtool.Card.presentation.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
public class CardRankInfoDto {

    private Long id;
    private String question;
    private String answer;
    private String thumbnailUrl; // ✅ 추가

    @QueryProjection
    public CardRankInfoDto(Long id, String question, String answer, String thumbnailUrl) {
        this.id = id;
        this.question = question;
        this.answer = answer;
        this.thumbnailUrl = thumbnailUrl;
    }
}