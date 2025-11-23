package com.example.thirdtool.Deck.presentation.dto;

import com.example.thirdtool.Deck.domain.model.Deck;

public record DeckNameUpdateResponseDto(Long id, String name) {

    public static DeckNameUpdateResponseDto from(Deck deck) {
        return new DeckNameUpdateResponseDto(deck.getId(), deck.getName());
    }

}
