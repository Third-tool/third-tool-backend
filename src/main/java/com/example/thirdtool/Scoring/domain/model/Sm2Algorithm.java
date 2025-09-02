package com.example.thirdtool.Scoring.domain.model;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.FeedbackType;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class Sm2Algorithm implements ScoringAlgorithm {

    @Override
    public void updateScore(Card card, FeedbackType feedback) {
        int quality = feedback.getQuality();

        // 1. SM-2 지표 및 피드백 횟수 업데이트
        int newRepetition = card.getRepetition();
        double newEasinessFactor = card.getEasinessFactor();

        int newGreatCount = card.getGreatCount();
        int newGoodCount = card.getGoodCount();
        int newNormalCount = card.getNormalCount();
        int newBadCount = card.getBadCount();

        if (quality < 2) {
            newRepetition = 0;
        } else {
            newRepetition++;
        }
        newEasinessFactor = newEasinessFactor + (0.1 - (3 - quality) * (0.08 + (3 - quality) * 0.02));
        if (newEasinessFactor < 1.3) {
            newEasinessFactor = 1.3;
        }

        switch (feedback) {
            case GREAT -> newGreatCount++;
            case GOOD -> newGoodCount++;
            case NORMAL -> newNormalCount++;
            case BAD -> newBadCount++;
        }

        // 2. ✅ SM-2 및 피드백 횟수 기반 점수 계산
        int newScore = (int) (newRepetition * 100 + (newEasinessFactor - 1.3) * 50);

        // ✅ 피드백 횟수를 점수 계산에 반영
        int feedbackScore = (newGreatCount * 10) + (newGoodCount * 5) - (newNormalCount * 3) - (newBadCount * 10);
        newScore += feedbackScore;

        // 3. 시간 보정 로직
        LocalDateTime now = LocalDateTime.now();
        long daysSinceCreation = Duration.between(card.getCreatedDate(), now).toDays();
        long daysSinceLastReview = Duration.between(card.getUpdatedDate(), now).toDays();

        if (daysSinceCreation > 100) {
            newScore += 20;
        }
        if (daysSinceLastReview > 10) {
            long penalty = daysSinceLastReview / 2;
            newScore -= penalty;
        }

        if (newScore < 0) {
            newScore = 0;
        }

        // 4. Card 엔티티의 내부 메서드를 호출하여 상태 변경 요청
        card.setScoreByAlgorithm(newScore, newRepetition, newEasinessFactor, newGreatCount, newGoodCount, newNormalCount, newBadCount);
    }
}