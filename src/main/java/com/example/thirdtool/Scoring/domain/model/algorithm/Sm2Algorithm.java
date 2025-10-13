package com.example.thirdtool.Scoring.domain.model.algorithm;

import com.example.thirdtool.Card.domain.model.FeedbackType;
import com.example.thirdtool.Scoring.domain.model.LearningProfile;
import com.example.thirdtool.Scoring.domain.model.Sm2LearningProfile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class Sm2Algorithm implements ScoringAlgorithm {

    @Override
    public void updateScore(LearningProfile profile, FeedbackType feedback) {
        if (!(profile instanceof Sm2LearningProfile sm2)) {
            throw new IllegalArgumentException("SM2 알고리즘은 Sm2LearningProfile에만 적용 가능합니다.");
        }

        int quality = feedback.getQuality();

        // 반복횟수, EF 조정
        int newRepetition = sm2.getRepetition();
        double newEasinessFactor = sm2.getEasinessFactor();

        if (quality < 2) {
            newRepetition = 0;
        } else {
            newRepetition++;
        }

        newEasinessFactor = newEasinessFactor + (0.1 - (3 - quality) * (0.08 + (3 - quality) * 0.02));
        if (newEasinessFactor < 1.3) {
            newEasinessFactor = 1.3;
        }

        // 점수 계산 (피드백 카운트는 Profile 공통 로직에서 이미 반영됨)
        int newScore = (int) (newRepetition * 100 + (newEasinessFactor - 1.3) * 50);
        int feedbackScore = (sm2.getGreatCount() * 10) + (sm2.getGoodCount() * 5)
                - (sm2.getNormalCount() * 3) - (sm2.getBadCount() * 10);
        newScore += feedbackScore;

        // 시간 보정 (Card 대신 Profile에서 생성/수정 시간 관리)
        LocalDateTime now = LocalDateTime.now();
        long daysSinceCreation = Duration.between(sm2.getCard().getCreatedDate(), now).toDays();
        long daysSinceLastReview = Duration.between(sm2.getCard().getUpdatedDate(), now).toDays();

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

        // 프로파일 상태 갱신
        sm2.applyScore(newScore, newRepetition, newEasinessFactor);
    }
}