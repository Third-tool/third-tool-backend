package com.example.thirdtool.Scoring.domain.model.algorithm;


import com.example.thirdtool.Card.domain.model.FeedbackType;
import com.example.thirdtool.Scoring.domain.model.LearningProfile;
import com.example.thirdtool.Scoring.domain.model.LeitnerLearningProfile;
import org.springframework.stereotype.Component;

@Component
public class LeitnerAlgorithm implements ScoringAlgorithm {

    @Override
    public void updateScore(LearningProfile profile, FeedbackType feedback) {
        if (!(profile instanceof LeitnerLearningProfile leitner)) {
            throw new IllegalArgumentException("Leitner 알고리즘은 LeitnerLearningProfile에만 적용 가능합니다.");
        }

        int newRepetition = leitner.getRepetition();
        double newEasinessFactor = leitner.getEasinessFactor();

        // ✅ Leitner 규칙
        switch (feedback) {
            case GREAT, GOOD -> newRepetition++;
            case NORMAL -> newRepetition = Math.max(newRepetition - 1, 0);
            case BAD -> newRepetition = 0;
        }

        // ✅ 점수 계산 (피드백 카운트는 profile.incrementFeedback()으로 이미 반영됨)
        int newScore = newRepetition * 100
                + (leitner.getGreatCount() * 10)
                + (leitner.getGoodCount() * 5)
                - (leitner.getNormalCount() * 2)
                - (leitner.getBadCount() * 5);

        // ✅ 난이도 계수(EF) 조정
        if (feedback == FeedbackType.GREAT) {
            newEasinessFactor = Math.min(newEasinessFactor + 0.05, 3.0);
        } else if (feedback == FeedbackType.BAD) {
            newEasinessFactor = Math.max(newEasinessFactor - 0.1, 1.3);
        }

        if (newScore < 0) {
            newScore = 0;
        }

        // ✅ LeitnerLearningProfile 상태 갱신
        leitner.applyScore(newScore, newRepetition, newEasinessFactor);
    }
}
