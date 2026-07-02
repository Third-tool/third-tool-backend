package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import java.util.List;

/**
 * Axis별 Selections(판례 — 구체 사례·응용) 제안에 전달되는 입력 컨텍스트 (Story-AS-E1).
 *
 * <p>Selections는 Roadmap의 각 챕터에 대응되는 구체 사례·문제·응용 예시 리스트.
 * ADR023(예정) 용어 기준: Selection = 판례.
 *
 * <p>{@code roadmapOutline}은 Roadmap 결과의 outline 순서 리스트를 참고용으로 전달 (LLM이 챕터별로
 * 관련 판례를 생성할 때 컨텍스트).</p>
 */
public record SelectionsSuggestionContext(
        Long facadeId,
        Long layerId,
        Long axisId,
        String axisName,
        List<String> concepts,
        List<String> roadmapOutline,
        String role
) {

    public SelectionsSuggestionContext {
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
        // roadmapOutline은 nullable — Roadmap 미생성 상태에서도 Selections 단독 호출 가능.
        if (roadmapOutline != null) {
            roadmapOutline = roadmapOutline.stream()
                    .map(v -> v == null ? null : v.trim())
                    .filter(v -> v != null && !v.isEmpty())
                    .toList();
        }
        if (role != null) {
            String trimmed = role.trim();
            role = trimmed.isEmpty() ? null : trimmed;
        }
    }
}
