package com.example.thirdtool.Review.infrastructure.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReviewSessionSummaryRow {

    private final Long sessionId;
    private final Long deckId;
    private final String deckName;
    private final int totalCardCount;
    private final LocalDateTime startedAt;

    @QueryProjection
    public ReviewSessionSummaryRow(
            Long sessionId,
            Long deckId,
            String deckName,
            int totalCardCount,
            LocalDateTime startedAt
                                  ) {
        this.sessionId = sessionId;
        this.deckId = deckId;
        this.deckName = deckName;
        this.totalCardCount = totalCardCount;
        this.startedAt = startedAt;
    }
}