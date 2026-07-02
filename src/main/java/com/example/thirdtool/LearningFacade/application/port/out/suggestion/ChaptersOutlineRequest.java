package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import java.util.List;

/**
 * 챕터 outline 생성 요청 (Story-AS-E1-S1-5, 이슈 #17).
 *
 * <p>사용자의 concepts + Layer/Axis 맥락 + 힌트를 받아 챕터 title/rationale 리스트만 반환한다
 * (subtree 본문 미포함 — {@link ChapterSubtreePort}에서 챕터별 병렬 요청).
 */
public record ChaptersOutlineRequest(
        List<String> concepts,
        String layerName,
        String axisName,
        String axisReason,
        Integer chapterCountHint,
        String freeformHint
) {

    public ChaptersOutlineRequest {
        if (concepts == null || concepts.isEmpty()) {
            throw new IllegalArgumentException("concepts는 최소 1개 이상이어야 합니다.");
        }
        concepts = concepts.stream()
                .map(v -> {
                    if (v == null || v.isBlank()) {
                        throw new IllegalArgumentException("concepts의 각 원소는 blank일 수 없습니다.");
                    }
                    return v.trim();
                })
                .toList();
        if (layerName == null || layerName.isBlank()) {
            throw new IllegalArgumentException("layerName은 blank일 수 없습니다.");
        }
        layerName = layerName.trim();
        if (axisName == null || axisName.isBlank()) {
            throw new IllegalArgumentException("axisName은 blank일 수 없습니다.");
        }
        axisName = axisName.trim();
        if (axisReason != null) {
            String t = axisReason.trim();
            axisReason = t.isEmpty() ? null : t;
        }
        if (freeformHint != null) {
            String t = freeformHint.trim();
            freeformHint = t.isEmpty() ? null : t;
        }
    }
}
