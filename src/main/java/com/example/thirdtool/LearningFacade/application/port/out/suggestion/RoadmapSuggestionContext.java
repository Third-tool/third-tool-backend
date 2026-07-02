package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import java.util.List;

/**
 * Axis별 Roadmap(헌법 — 학습 순서 청사진) 제안에 전달되는 입력 컨텍스트 (Story-AS-E1).
 *
 * <p>Roadmap은 특정 Axis 안에서 "무엇을 · 어떤 순서로 학습할지" 뼈대를 정의한다.
 * ADR023(예정) 용어 기준: Roadmap = 헌법, Selection = 판례.
 *
 * <p>{@code layerName}은 옵셔널 힌트 (LLM 컨텍스트 풍부화). {@code role}은 RoleDetector 미도입 상태에서 null.</p>
 */
public record RoadmapSuggestionContext(
        Long facadeId,
        Long layerId,
        Long axisId,
        String axisName,
        String layerName,
        List<String> concepts,
        String role
) {

    public RoadmapSuggestionContext {
        if (facadeId == null) {
            throw new IllegalArgumentException("facadeId는 null일 수 없습니다.");
        }
        if (axisId == null) {
            throw new IllegalArgumentException("axisId는 null일 수 없습니다.");
        }
        if (axisName == null || axisName.isBlank()) {
            throw new IllegalArgumentException("axisName은 blank일 수 없습니다.");
        }
        axisName = axisName.trim();
        if (layerName != null) {
            String trimmed = layerName.trim();
            layerName = trimmed.isEmpty() ? null : trimmed;
        }
        if (concepts == null || concepts.isEmpty()) {
            throw new IllegalArgumentException("concepts는 최소 1개 이상이어야 합니다.");
        }
        concepts = concepts.stream()
                .map(value -> {
                    if (value == null || value.isBlank()) {
                        throw new IllegalArgumentException("concepts의 각 원소는 blank일 수 없습니다.");
                    }
                    return value.trim();
                })
                .toList();
        if (role != null) {
            String trimmed = role.trim();
            role = trimmed.isEmpty() ? null : trimmed;
        }
    }
}
