package com.example.thirdtool.Card.domain.model;

import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Arrays;

@Embeddable
public class Summary {

    //
    private static final int MIN_SENTENCES = 1;
    private static final int MAX_SENTENCES = 3;
    private static final String SENTENCE_DELIMITER_REGEX = "[.!?]+";

    @Column(name = "summary_value", nullable = false, columnDefinition = "TEXT")
    private String value;

    protected Summary() {}

    private Summary(String value) {
        this.value = value;
    }

    public static Summary of(String value) {
        String trimmed = trim(value);

        if (trimmed == null || trimmed.isBlank()) {
            throw CardDomainException.of(ErrorCode.CARD_SUMMARY_EMPTY);
        }

        int count = countSentences(trimmed);
        if (count < MIN_SENTENCES || count > MAX_SENTENCES) {
            throw CardDomainException.of(
                    ErrorCode.CARD_SUMMARY_SENTENCE_OUT_OF_RANGE,
                    "현재 " + count + "문장 (허용 범위: " + MIN_SENTENCES + "~" + MAX_SENTENCES + "문장)"
                                        );
        }
        return new Summary(trimmed);
    }

    public String getValue() { return value; }

    static int countSentences(String text) {
        return (int) Arrays.stream(text.split(SENTENCE_DELIMITER_REGEX))
                           .map(String::trim)
                           .filter(s -> !s.isBlank())
                           .count();
    }

    private static String trim(String v) { return v == null ? null : v.trim(); }
}
