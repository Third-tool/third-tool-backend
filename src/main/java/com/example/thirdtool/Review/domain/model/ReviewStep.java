package com.example.thirdtool.Review.domain.model;

public enum ReviewStep {
    RECALLING("회상중"),
    COMPARING("비교 중");

    private final String label;

    ReviewStep(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
