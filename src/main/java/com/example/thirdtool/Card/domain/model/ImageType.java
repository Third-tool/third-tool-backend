package com.example.thirdtool.Card.domain.model;

public enum ImageType {
    QUESTION("질문용 사진"),
    ANSWER("답변용 사진");

    private String text;

    ImageType(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
