package com.example.thirdtool.Card.domain.model;

public enum CardRankType {
    SILVER("실버"),
    GOLD("골드"),
    DIAMOND("다이아몬드");

    private final String koreanName;

    CardRankType(String koreanName) {
        this.koreanName = koreanName;
    }

    public String getKoreanName() {
        return koreanName;
    }
}