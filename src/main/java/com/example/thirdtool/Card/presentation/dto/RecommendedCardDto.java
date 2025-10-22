package com.example.thirdtool.Card.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class RecommendedCardDto {
    private Long id;
    private String question;
    private String answer;
    private String thumbnailUrl; // 대표 이미지 (sequence 1번 혹은 첫 번째)
}