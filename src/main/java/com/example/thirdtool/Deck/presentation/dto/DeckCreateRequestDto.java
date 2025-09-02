package com.example.thirdtool.Deck.presentation.dto;


public record DeckCreateRequestDto(String name,
                                   Long parentDeckId,
                                   String scoringAlgorithmType) {
    //우선
}
