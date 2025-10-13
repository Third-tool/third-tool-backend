package com.example.thirdtool.Recommendation.domain;

import com.example.thirdtool.Deck.domain.model.Deck;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

//vo임-Deck에 대한
@Getter
public class DeckRecommendation {

    private final Long deckId;
    private final String deckName;

    private final double avgScore;
    private final double badRatio;
    private final long daysSinceAccess;
    private final long totalCards;

    private double priorityScore;
    private double scoreFactor;
    private double badRatioFactor;
    private double lastAccessFactor;
    private String reason;

    public DeckRecommendation(Long deckId,
                              String deckName,
                              double avgScore,
                              double badRatio,
                              long daysSinceAccess,
                              long totalCards) {
        this.deckId = deckId;
        this.deckName = deckName;
        this.avgScore = avgScore;
        this.badRatio = badRatio;
        this.daysSinceAccess = daysSinceAccess;
        this.totalCards = totalCards;
    }

    /** ✅ 도메인 행동: 가중치 및 추천 사유 계산 */
    public DeckRecommendation calculateRecommendation() {
        this.scoreFactor = (1 - avgScore / 100.0) * 0.5;
        this.badRatioFactor = badRatio * 0.3;
        this.lastAccessFactor = Math.min(1.0, daysSinceAccess / 30.0) * 0.2;

        this.priorityScore = scoreFactor + badRatioFactor + lastAccessFactor;

        this.reason = String.format(
                "평균 점수 %.1f, BAD 비율 %.1f%%, 마지막 학습 %d일 전, 카드 수 %d",
                avgScore, badRatio * 100, daysSinceAccess, totalCards
                                   );
        return this;
    }
}