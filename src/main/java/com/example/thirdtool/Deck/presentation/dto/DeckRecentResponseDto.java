package com.example.thirdtool.Deck.presentation.dto;


import com.example.thirdtool.Deck.domain.model.Deck;

import java.time.LocalDateTime;

public record DeckRecentResponseDto(
        Long id,
        String name,
        String scoringAlgorithmType,
        int depth,
        LocalDateTime lastAccessed,
        Long parentDeckId,
        String thumbnailUrl // ✅ 최근 덱 조회를 위한 썸네일 필드
) {
    /**
     * @param deck Deck 엔티티
     * @param thumbnailUrl 서비스 레이어에서 조회한 썸네일 URL (없으면 null)
     */
    public static DeckRecentResponseDto from(Deck deck, String thumbnailUrl) {
        return new DeckRecentResponseDto(
                deck.getId(),
                deck.getName(),
                deck.getScoringAlgorithmType(),
                deck.getDepth(),
                deck.getLastAccessed(),
                deck.getParentDeck() != null ? deck.getParentDeck().getId() : null,
                thumbnailUrl // ✅ 썸네일 URL 주입
        );
    }
}
