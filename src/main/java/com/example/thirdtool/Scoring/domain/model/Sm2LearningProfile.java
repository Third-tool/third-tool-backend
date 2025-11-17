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
@DiscriminatorValue("SM2")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sm2LearningProfile extends LearningProfile {

    private int repetition;
    private double easinessFactor;

    public static Sm2LearningProfile init() {
        Sm2LearningProfile p = new Sm2LearningProfile();
        LearningProfile.initCommon(p); // ★ 공통 초기화
        p.repetition = 0;
        p.easinessFactor = 2.5;
        return p;
    }

    @Override
    public void applyFeedback(ScoringAlgorithm algorithm, Card card, FeedbackType feedback) {
        ensureFeedbackAllowed();   // 모드 체크
        incrementFeedback(feedback); // 공통 카운팅
        algorithm.updateScore(this, feedback); // 이제 profile 기준 업데이트
    }

    public void applyScore(int score, int repetition, double easinessFactor) {
        this.score = score;
        this.repetition = repetition;
        this.easinessFactor = easinessFactor;
    }

    @Override
    public void reset(int newScore) {
        super.reset(newScore);  // 공통 초기화
        this.repetition = 0;
        this.easinessFactor = 2.5;
    }

    @Override
    public String getAlgorithmType() {
        return "SM2";
    }
}