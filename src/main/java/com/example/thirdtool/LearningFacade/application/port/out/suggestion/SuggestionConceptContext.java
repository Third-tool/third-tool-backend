package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import java.util.List;

/**
 * AI 제안에 전달되는 사용자 LearningFacade의 조합형 concept context.
 *
 * <p>단일 직업 문자열이 아니라 {@code conceptSet(2~3개) + compositionReason + desiredOutcome}로
 * 사용자의 학습 목표 의도를 압축한다. 빈 입력·blank 입력은 Adapter 도달 전에 차단한다.</p>
 *
 * <p>도메인 레이어는 본 VO를 import하지 않는다 — Application Service만 참조.</p>
 */
public record SuggestionConceptContext(
        List<String> conceptSet,
        String compositionReason,
        String desiredOutcome
) {

    public SuggestionConceptContext {
        if (conceptSet == null || conceptSet.isEmpty()) {
            throw new IllegalArgumentException("conceptSet은 최소 1개 이상이어야 합니다.");
        }
        conceptSet = conceptSet.stream()
                .map(value -> {
                    if (value == null || value.isBlank()) {
                        throw new IllegalArgumentException("conceptSet의 각 원소는 blank일 수 없습니다.");
                    }
                    return value.trim();
                })
                .toList();

        if (compositionReason == null || compositionReason.isBlank()) {
            throw new IllegalArgumentException("compositionReason은 blank일 수 없습니다.");
        }
        compositionReason = compositionReason.trim();

        if (desiredOutcome == null || desiredOutcome.isBlank()) {
            throw new IllegalArgumentException("desiredOutcome은 blank일 수 없습니다.");
        }
        desiredOutcome = desiredOutcome.trim();
    }
}
