package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import java.util.List;

/**
 * Selection outline 생성 요청 (Story-AS-E1-S1-7).
 *
 * <p>Roadmap의 outline과 유사하나 Selection은 컨테이너 name 후보도 함께 반환한다.
 * roadmapContent(nullable)는 참조용 요약 — Selection이 "roadmap 기준을 굴리는 갈래"임을 강조.
 * variantHint는 사용자 의도(예: "능 아키텍처")를 컨테이너 이름 후보에 반영.
 */
public record SelectionOutlineRequest(
        List<String> concepts,
        String layerName,
        String axisName,
        String roadmapContent,
        String variantHint,
        Integer chapterCountHint
) {

    public SelectionOutlineRequest {
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
        if (roadmapContent != null) {
            String t = roadmapContent.trim();
            roadmapContent = t.isEmpty() ? null : t;
        }
        if (variantHint != null) {
            String t = variantHint.trim();
            variantHint = t.isEmpty() ? null : t;
        }
    }
}
