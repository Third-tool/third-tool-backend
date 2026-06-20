package com.example.thirdtool.LearningFacade.infrastructure.suggestion;

import com.example.thirdtool.LearningFacade.application.port.out.suggestion.AxisTopicSuggestion;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.AxisTopicSuggestionPort;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.SuggestionConceptContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AxisTopic 제안 Static Adapter.
 *
 * <p>고정 주제 목록에서 {@code existingTopicNames}와 중복되는 항목을 제거한 뒤
 * {@code limit}만큼 반환한다. 축 이름({@code axisName})은 컨텍스트로 받지만 Static 버전에선
 * 사용하지 않는다 — LLM Adapter에서 실제 컨텍스트로 활용된다.</p>
 *
 * <p>{@code thirdtool.suggestion.provider} 미설정 또는 {@code static}일 때 활성화.</p>
 */
@Component
@ConditionalOnProperty(
        name = "thirdtool.suggestion.provider",
        havingValue = "static",
        matchIfMissing = true
)
public class StaticAxisTopicSuggestionAdapter implements AxisTopicSuggestionPort {

    static final List<String> FIXED_TOPIC_NAMES = List.of(
            "기초 이해",
            "구조 설계",
            "심화 응용",
            "도구 활용",
            "실전 사례",
            "핵심 개념 정리",
            "방법론 학습",
            "사례 분석",
            "패턴 인식",
            "비교 검토"
    );

    @Override
    public List<AxisTopicSuggestion> suggest(SuggestionConceptContext conceptContext,
                                             String axisName,
                                             List<String> existingTopicNames,
                                             int limit) {
        if (limit <= 0) {
            return List.of();
        }
        Set<String> exclude = toExcludeSet(existingTopicNames);
        return FIXED_TOPIC_NAMES.stream()
                .filter(name -> !exclude.contains(name))
                .limit(limit)
                .map(name -> new AxisTopicSuggestion(name, null))
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
