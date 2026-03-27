package com.example.thirdtool.Deck.infrastructure.dto;

import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DeckSummaryRow {

    private final Long          deckId;
    private final String        name;
    private final DeckMode mode;
    private final int           depth;
    private final LocalDateTime lastAccessed;
    private final int           cardCount;
    private final int           subDeckCount;

    @QueryProjection
    public DeckSummaryRow(
            Long deckId,
            String name,
            DeckMode mode,
            int depth,
            LocalDateTime lastAccessed,
            int cardCount,
            int subDeckCount
                         ) {
        this.deckId       = deckId;
        this.name         = name;
        this.mode         = mode;
        this.depth        = depth;
        this.lastAccessed = lastAccessed;
        this.cardCount    = cardCount;
        this.subDeckCount = subDeckCount;
    }
}
