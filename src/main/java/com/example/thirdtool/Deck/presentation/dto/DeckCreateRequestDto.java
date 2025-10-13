package com.example.thirdtool.Deck.presentation.dto;


import jakarta.validation.constraints.NotBlank;

public record DeckCreateRequestDto(@NotBlank(message = "덱 이름은 비어 있을 수 없습니다.")String name,
                                   Long parentDeckId,
                                   String scoringAlgorithmType
                                   ) {
    //우선
}
