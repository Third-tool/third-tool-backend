package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import java.util.List;

/**
 * Layer 후보 제안에 전달되는 입력 컨텍스트 (Story-AS-E1).
 *
 * <p>{@code facadeId}는 소유 사용자 식별용 (Static Adapter에서는 fallback 카탈로그 조회 hint).
 * {@code concepts}는 LearningFacade.concepts[] 스냅샷 (trim 정규화 완료 상태 기대).
 * {@code role}은 concept로부터 파생된 역할 카테고리 hint (nullable — RoleDetector 미도입 상태에서는 null 허용).
 *
 * <p>도메인 레이어는 본 VO를 import하지 않는다 — Application Service만 참조.</p>
 */
public record LayerSuggestionContext(
        Long facadeId,
        List<String> concepts,
        String role
) {

    public LayerSuggestionContext {
        if (facadeId == null) {
            throw new IllegalArgumentException("facadeId는 null일 수 없습니다.");
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
