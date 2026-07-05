package com.example.thirdtool.Review.infrastructure.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * REV E2 · Story 2-6 — deckId/deckName 필드 폐기 · batchId + finishedAt 신설.
 */
@Getter
public class ReviewSessionSummaryRow {

    private final Long          sessionId;
    private final Long          batchId;
    private final int           totalCardCount;
    private final int           availableCardCount;
    private final LocalDateTime startedAt;
    private final LocalDateTime finishedAt;

    @QueryProjection
    public ReviewSessionSummaryRow(
            Long sessionId,
            Long batchId,
            int totalCardCount,
            int availableCardCount,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
                                  ) {
        this.sessionId          = sessionId;
        this.batchId            = batchId;
        this.totalCardCount     = totalCardCount;
        this.availableCardCount = availableCardCount;
        this.startedAt          = startedAt;
        this.finishedAt         = finishedAt;
    }
}
