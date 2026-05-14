package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;

/**
 * LearningFacade 수준의 커버리지 집계 스냅샷.
 * 도메인 스펙: private-docs/domain/learning-facade.md §CoverageSummary
 * 불변 VO. 응답 DTO의 "N개 주제 중 M개 미커버" 문구의 단일 진실 소스.
 */
public record CoverageSummary(
        int totalTopics,
        int uncoveredTopics,
        int partialTopics,
        int coveredTopics
) {

    public CoverageSummary {
        if (totalTopics < 0 || uncoveredTopics < 0 || partialTopics < 0 || coveredTopics < 0) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "CoverageSummary의 카운트는 음수일 수 없습니다."
            );
        }
        if (uncoveredTopics + partialTopics + coveredTopics != totalTopics) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "uncovered + partial + covered가 totalTopics와 일치해야 합니다."
            );
        }
    }

    public static CoverageSummary of(int uncoveredTopics, int partialTopics, int coveredTopics) {
        int total = uncoveredTopics + partialTopics + coveredTopics;
        return new CoverageSummary(total, uncoveredTopics, partialTopics, coveredTopics);
    }

    public static CoverageSummary empty() {
        return new CoverageSummary(0, 0, 0, 0);
    }

    public boolean hasGap() {
        return uncoveredTopics > 0;
    }
}
