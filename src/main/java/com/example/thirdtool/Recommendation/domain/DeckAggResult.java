package com.example.thirdtool.Recommendation.domain;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record DeckAggResult(
        Long deckId,
        String deckName,
        Double avgScore,
        Long badCount,
        Long totalFeedback,
        LocalDateTime lastAccessed,
        Long totalCards
) {

    /** ✅ 도메인 객체 변환 메서드 */
    public DeckRecommendation toDomain() {
        double badRatio = (totalFeedback == 0)
                ? 0 : (double) badCount / totalFeedback;
        long daysSinceAccess = ChronoUnit.DAYS.between(lastAccessed, LocalDateTime.now());
        return new DeckRecommendation(deckId, deckName, avgScore, badRatio, daysSinceAccess, totalCards);
    }
}