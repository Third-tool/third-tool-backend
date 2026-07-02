package com.example.thirdtool.LearningFacade.infrastructure.suggestion;

import java.util.List;
import java.util.Map;

/**
 * Role별 4-Port(Layer/Axis/Roadmap/Selections) 응답 데이터를 담는 카탈로그 (Story-AS-E3-S2).
 *
 * <p>classpath JSON에서 로드 — 필드 순서·구조는 파일 스키마와 일치.
 * Story-15 스코프에서는 {@code layers} 절만 실제 사용 (나머지는 Adapter 확장 대비 사전 정의).
 */
public record SuggestionCatalog(
        String role,
        List<LayerEntry> layers,
        Map<String, List<AxisEntry>> axes,
        Map<String, RoadmapEntry> roadmaps,
        Map<String, SelectionsEntry> selections
) {

    public record LayerEntry(String name, String rationale) {}

    public record AxisEntry(String description, String rationale) {}

    public record RoadmapEntry(List<String> outline, String rationale) {}

    public record SelectionsEntry(List<String> selections, String rationale) {}
}
