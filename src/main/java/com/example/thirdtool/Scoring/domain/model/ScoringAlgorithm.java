package com.example.thirdtool.Scoring.domain.model;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.FeedbackType;

public interface ScoringAlgorithm {
    void updateScore(Card card, FeedbackType feedback);
}