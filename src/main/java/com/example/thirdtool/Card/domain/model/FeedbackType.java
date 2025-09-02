package com.example.thirdtool.Card.domain.model;

public enum FeedbackType {
    GREAT(3),
    GOOD(2),
    NORMAL(1),
    BAD(0);

    private final int quality;

    FeedbackType(int quality) {
        this.quality = quality;
    }

    public int getQuality() {
        return quality;
    }
}