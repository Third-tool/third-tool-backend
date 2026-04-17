package com.example.thirdtool.LearningFacade.application.service;

import org.springframework.stereotype.Component;

@Component
public class VerbFormValidator {

    // 우선 간단하게 - 추후 정밀하게 리팩토링 예정
    private static final String HADA_SUFFIX = "하다";

    public boolean isSuggested(String description) {
        if (description == null || description.isBlank()) {
            return false;
        }
        return !description.trim().endsWith(HADA_SUFFIX);
    }
}

