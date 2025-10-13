package com.example.thirdtool.Scoring.domain.model.algorithm;

import com.example.thirdtool.Card.domain.model.FeedbackType;
import com.example.thirdtool.Scoring.domain.model.LearningProfile;

public interface ScoringAlgorithm {
    void updateScore(LearningProfile profile, FeedbackType feedback);
}