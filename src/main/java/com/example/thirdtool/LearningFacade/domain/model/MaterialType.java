package com.example.thirdtool.LearningFacade.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum MaterialType {

    TOP_DOWN(
            "🔽 Top-down",
            "개념·원리 중심 자료. 예: 책, 강의, 아티클"
    ),

    BOTTOM_UP(
            "🔼 Bottom-up",
            "실습·경험 중심 자료. 예: 프로젝트, 과제, 실습 예제"
    );

    private final String displayName;
    private final String description;
}