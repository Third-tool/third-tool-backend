package com.example.thirdtool.LearningFacade.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CoverageStatus {

    NO_MATERIAL("⚠️ 자료 없음",
            "연결된 학습 자료가 없는 초기 상태. 행동 동사 수정 시에도 이 상태로 초기화된다."),

    PARTIAL("🔵 일부 커버",
            "자료가 1개 이상 연결되어 있으나 충분하지 않다고 판단되는 상태. 기준은 v2에서 정의한다."),

    COVERED("✅ 커버됨",
            "자료가 충분히 연결된 상태. 전환 기준은 v2에서 정의한다.");

    private final String displayName;
    private final String description;
}