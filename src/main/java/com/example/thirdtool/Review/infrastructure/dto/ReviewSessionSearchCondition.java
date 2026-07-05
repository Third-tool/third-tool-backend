package com.example.thirdtool.Review.infrastructure.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * REV E2 · Story 2-6 — deck 필터 제거 · batch 필터로 대체.
 */
@Getter
@Builder
public class ReviewSessionSearchCondition {

    private Long userId;   // 필수 (본인 세션만 조회)
    private Long batchId;  // 선택: 특정 batch 세션만 필터링
}
