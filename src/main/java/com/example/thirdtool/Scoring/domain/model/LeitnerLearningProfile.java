package com.example.thirdtool.Scoring.domain.model;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.FeedbackType;
import com.example.thirdtool.Scoring.domain.model.algorithm.ScoringAlgorithm;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("LEITNER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeitnerLearningProfile extends LearningProfile {

    private int repetition;
    private double easinessFactor;

    public static LeitnerLearningProfile init() {
        LeitnerLearningProfile p = new LeitnerLearningProfile();
        LearningProfile.initCommon(p); // ★ 공통 초기화
        p.repetition = 0;
        p.easinessFactor = 2.5;
        return p;
    }


    @Override
    public void applyFeedback(ScoringAlgorithm algorithm, Card card, FeedbackType feedback) {
        incrementFeedback(feedback);            // 공통 카운팅
        algorithm.updateScore(card.getLearningProfile(), feedback);  // 알고리즘 점수 산정
    }

    public void applyScore(int score, int repetition, double easinessFactor) {
        this.score = score;
        this.repetition = repetition;
        this.easinessFactor = easinessFactor;
    }
}