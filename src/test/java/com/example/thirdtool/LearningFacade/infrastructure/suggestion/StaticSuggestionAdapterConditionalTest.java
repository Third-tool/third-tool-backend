package com.example.thirdtool.LearningFacade.infrastructure.suggestion;

import com.example.thirdtool.LearningFacade.application.port.out.suggestion.AxisSuggestionPort;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.AxisTopicSuggestionPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story 1-2 AC #1 — {@code thirdtool.suggestion.provider=static} 상태에서 Static Adapter
 * Bean이 활성화되어 Port 주입 시 Static 인스턴스가 반환되는지 검증.
 *
 * <p>{@code matchIfMissing=true}이므로 미설정 케이스도 본 케이스와 동일하게 동작한다.
 * 다른 값(예: {@code llm}) 케이스의 비활성화 검증은 LLM Adapter가 도입되는 Story 2-1에서
 * 자연스럽게 함께 검증된다.</p>
 */
@SpringBootTest(properties = "thirdtool.suggestion.provider=static")
@ActiveProfiles("dev")
@DisplayName("Static Suggestion Adapter — provider=static 조건 활성화")
class StaticSuggestionAdapterConditionalTest {

    @Autowired
    private AxisSuggestionPort axisPort;

    @Autowired
    private AxisTopicSuggestionPort axisTopicPort;

    @Test
    @DisplayName("provider=static 환경에서 AxisSuggestionPort는 Static 구현체로 주입된다")
    void axisPort_static_구현체_주입() {
        assertThat(axisPort).isInstanceOf(StaticAxisSuggestionAdapter.class);
    }

    @Test
    @DisplayName("provider=static 환경에서 AxisTopicSuggestionPort는 Static 구현체로 주입된다")
    void axisTopicPort_static_구현체_주입() {
        assertThat(axisTopicPort).isInstanceOf(StaticAxisTopicSuggestionAdapter.class);
    }
}
