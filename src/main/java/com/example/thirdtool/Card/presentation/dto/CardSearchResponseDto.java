package com.example.thirdtool.Card.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CardSearchResponseDto {
    private Long cardId;
    private String question;
    private String answer;
    private String thumbnailUrl;
}
