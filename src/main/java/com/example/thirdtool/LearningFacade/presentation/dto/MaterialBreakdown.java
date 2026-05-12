package com.example.thirdtool.LearningFacade.presentation.dto;

import com.example.thirdtool.LearningFacade.domain.model.MaterialType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 주제별 자료 타입 비중 응답 VO (Story-003-4 / Story 4-3).
 *
 * <p>api §3 GET /learning-facade 응답의 각 주제 항목에 포함된다.
 * table-spec §3-1 (4) 결정에 따라 저장하지 않으며 Application Service가 인메모리 그룹핑한다.
 *
 * <p>{@code byType}은 항상 4종 키를 모두 포함한다 (count=0이어도 키 누락 X) — FE 회색 처리 신호.
 * {@code totalCount}=0이면 FE는 게이지 대신 {@code ⚠️ 자료 없음} 라벨을 표시한다.
 */
public record MaterialBreakdown(
        Map<MaterialType, Long> byType,
        long staticCount,
        long dynamicCount,
        long totalCount
) {

    public static MaterialBreakdown from(List<MaterialType> types) {
        Map<MaterialType, Long> counts = new EnumMap<>(MaterialType.class);
        for (MaterialType type : MaterialType.values()) {
            counts.put(type, 0L);
        }
        for (MaterialType t : types) {
            counts.merge(t, 1L, Long::sum);
        }

        long staticCount = counts.get(MaterialType.BOOK) + counts.get(MaterialType.COURSE);
        long dynamicCount = counts.get(MaterialType.AI_CONVERSATION) + counts.get(MaterialType.WEB_RESOURCE);
        long totalCount = staticCount + dynamicCount;

        return new MaterialBreakdown(counts, staticCount, dynamicCount, totalCount);
    }

    public static MaterialBreakdown empty() {
        return from(List.of());
    }
}
