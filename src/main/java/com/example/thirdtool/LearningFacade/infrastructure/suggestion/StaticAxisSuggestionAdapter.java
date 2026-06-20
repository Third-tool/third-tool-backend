package com.example.thirdtool.LearningFacade.infrastructure.suggestion;

import com.example.thirdtool.LearningFacade.application.port.out.suggestion.AxisSuggestion;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.AxisSuggestionPort;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.SuggestionConceptContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Axis 제안 Static Adapter.
 *
 * <p>고정 카테고리 목록에서 {@code existingAxisNames}와 중복되는 항목을 제거한 뒤
 * {@code limit}만큼 반환한다. LLM 호출이 없어 로컬 개발·통합 테스트·LLM 장애 상황에서도
 * 유저 입력 흐름이 끊기지 않게 한다.</p>
 *
 * <p>{@code thirdtool.suggestion.provider} 미설정 또는 {@code static}일 때 활성화.</p>
 */
@Component
@ConditionalOnProperty(
        name = "thirdtool.suggestion.provider",
        havingValue = "static",
        matchIfMissing = true
)
public class StaticAxisSuggestionAdapter implements AxisSuggestionPort {

    static final List<String> FIXED_AXIS_NAMES = List.of(
            "기초 지식",
            "핵심 방법론",
            "도구와 환경",
            "실전 응용",
            "심화 주제"
    );

    @Override
    public List<AxisSuggestion> suggest(SuggestionConceptContext conceptContext,
                                        List<String> existingAxisNames,
                                        int limit) {
        if (limit <= 0) {
            return List.of();
        }
        Set<String> exclude = toExcludeSet(existingAxisNames);
        return FIXED_AXIS_NAMES.stream()
                .filter(name -> !exclude.contains(name))
                .limit(limit)
                .map(name -> new AxisSuggestion(name, null))
                .toList();
    }

    private Set<String> toExcludeSet(Collection<String> names) {
        if (names == null || names.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>();
        for (String name : names) {
            if (name != null) {
                result.add(name.trim());
            }
        }
        return result;
    }
}
