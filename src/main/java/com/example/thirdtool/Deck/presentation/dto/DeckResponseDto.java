package com.example.thirdtool.Deck.presentation.dto;

import com.example.thirdtool.Deck.domain.model.Deck;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// 예시: DeckResponseDto.java
public record DeckResponseDto(
        Long id,
        String name,
        String scoringAlgorithmType,
        int depth,
        LocalDateTime lastAccessed,
        Long parentDeckId
) {
    public static DeckResponseDto from(Deck deck) {
        return new DeckResponseDto(
                deck.getId(),
                deck.getName(),
                deck.getScoringAlgorithmType(),
                deck.getDepth(),
                deck.getLastAccessed(),
                deck.getParentDeck() != null ? deck.getParentDeck().getId() : null
        );
    }
}