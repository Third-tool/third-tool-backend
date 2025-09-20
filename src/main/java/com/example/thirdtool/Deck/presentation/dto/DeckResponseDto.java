package com.example.thirdtool.Deck.presentation.dto;

import com.example.thirdtool.Deck.domain.model.Deck;

import java.util.List;
import java.util.stream.Collectors;

// 예시: DeckResponseDto.java
public record DeckResponseDto(
    Long id,
    String name,
    List<DeckResponseDto> subDecks,
    List<String> tagNames
) {
    // 팩토리 메서드: 엔티티를 DTO로 변환
    public static DeckResponseDto from(Deck deck) {
        // 하위 덱이 있다면 재귀적으로 DTO로 변환
        List<DeckResponseDto> subDecks = deck.getSubDecks().stream()
            .map(DeckResponseDto::from)
            .collect(Collectors.toList());
        List<String> tagNames = deck.getTags().stream()
            .map(tag -> tag.getDisplayName())
            .collect(Collectors.toList());
        return new DeckResponseDto(deck.getId(), deck.getName(), subDecks, tagNames);
    }
}