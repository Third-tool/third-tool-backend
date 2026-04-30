package com.example.thirdtool.LearningFacade.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CoverageStatus {

    NO_MATERIAL("⚠️ 자료 없음",
            "연결된 학습 자료가 없는 상태. 주제 생성 시 초기 상태."),

    PARTIAL("✅ 자료 있음 (학습중)",
            "자료 1개 이상 연결. 연결된 자료 중 MASTERED인 것이 없는 상태."),

    COVERED("✅ 자료 있음 (마스터)",
            "연결된 자료 중 1개 이상이 MASTERED 상태인 경우.");

    private final String displayName;
    private final String description;
}
