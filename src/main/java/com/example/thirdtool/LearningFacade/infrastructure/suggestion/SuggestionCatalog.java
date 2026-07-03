package com.example.thirdtool.LearningFacade.infrastructure.suggestion;

import java.util.List;
import java.util.Map;

/**
 * Role별 6-Port(Layer/Axis/ChaptersOutline/ChapterSubtree/SelectionOutline/SelectionSubtree)
 * 응답 데이터 카탈로그 (Story-AS-E3-S3-2, 이슈 #17).
 *
 * <p>classpath JSON에서 로드. 필드 순서·구조는 파일 스키마와 일치.
 * 이전 4-Port용 필드(roadmaps, selections)는 SUPERSEDED 상태이나 backward compat 유지.
 * 신규 6-Port용 필드: chapters, selectionOutlines.
 */
public record SuggestionCatalog(
        String role,
        List<LayerEntry> layers,
        Map<String, List<AxisEntry>> axes,
        // 4-Port 계승 (deprecated, backward compat)
        Map<String, RoadmapEntry> roadmaps,
        Map<String, SelectionsEntry> selections,
        // 6-Port 신설 (Story-AS-E3-S3-2)
        List<ChapterEntry> chapters,
        List<SelectionOutlineEntry> selectionOutlines
) {

    public record LayerEntry(String name, String rationale) {}

    public record AxisEntry(String description, String rationale) {}

    /** @deprecated 이슈 #17로 SUPERSEDED. chapters 필드로 대체. */
    @Deprecated
    public record RoadmapEntry(List<String> outline, String rationale) {}

    /** @deprecated 이슈 #17로 SUPERSEDED. selectionOutlines 필드로 대체. */
    @Deprecated
    public record SelectionsEntry(List<String> selections, String rationale) {}

    /**
     * 6-Port용 챕터 카탈로그 원소.
     * axisName: 이 챕터가 속하는 axis (매칭 키).
     * title: 챕터 이름 (예: "1. 하네스 엔지니어링 기초").
     * rationale: 이 챕터의 의도 (nullable).
     * subtree: 챕터 subtree ASCII 통짜 (예: "├── 1-1. 정의와 본질\n│       ...").
     */
    public record ChapterEntry(String axisName, String title, String rationale, String subtree) {}

    /**
     * 6-Port용 Selection outline 원소.
     * axisName: 매칭 키.
     * variantHint: 사용자 힌트에 매칭할 태그 (nullable).
     * nameCandidate: 컨테이너 name 후보 (예: "능 아키텍처 selections v2").
     * chapters: 하위 챕터 outline (title/rationale).
     * subtrees: chapters와 1:1 (subtree ASCII 저장, 옵션).
     */
    public record SelectionOutlineEntry(
            String axisName,
            String variantHint,
            String nameCandidate,
            List<ChapterEntry> chapters
    ) {}
}
