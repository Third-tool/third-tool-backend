package com.example.thirdtool.LearningFacade.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProficiencyLevel {

    UNRATED(
            "미평가",
            "등록 직후 기본 상태. 아직 자가 평가를 하지 않은 상태"
    ),

    UNFAMILIAR(
            "낯섦",
            "자료를 접했지만 아직 내용이 낯선 상태"
    ),

    GETTING_USED(
            "익숙해지는 중",
            "자료의 내용이 점점 체화되고 있는 상태"
    ),

    MASTERED(
            "마스터",
            "자료의 핵심 내용을 충분히 소화했다고 자가 평가한 상태"
    );

    private final String displayName;
    private final String description;
}
