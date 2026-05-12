package com.example.thirdtool.LearningFacade.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum MaterialType {

    BOOK(
            "📖 책",
            "제목·저자 중심의 정적 자료"
    ),

    COURSE(
            "🎓 강의",
            "플랫폼·강의명 기반의 정적 자료"
    ),

    AI_CONVERSATION(
            "💬 AI 대화",
            "Claude / ChatGPT / Gemini 등 AI와의 동적 대화 자료"
    ),

    WEB_RESOURCE(
            "🌐 웹 리소스",
            "Notion / 블로그 / 공식 문서 등 살아 움직이는 동적 자료"
    );

    private final String displayName;
    private final String description;

    public boolean isStatic() {
        return this == BOOK || this == COURSE;
    }

    public boolean isDynamic() {
        return !isStatic();
    }
}
