package com.example.thirdtool.Deck.presentation.dto;


import java.util.List;

public record DeckCreateRequestDto(String name,
                                   Long parentDeckId,
                                   String scoringAlgorithmType,
                                   List<Long> tagIds) {
    //우선
}
