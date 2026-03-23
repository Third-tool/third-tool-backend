package com.example.thirdtool.Review.domain.model;

public enum ReviewStep {
    RECALLING("회상중"),
    REVEALED("공개됨");

    private final String label;

    ReviewStep(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
