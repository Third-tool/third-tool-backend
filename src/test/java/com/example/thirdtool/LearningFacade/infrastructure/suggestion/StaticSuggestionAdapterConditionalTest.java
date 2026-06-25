package com.example.thirdtool.LearningFacade.infrastructure.suggestion;

import com.example.thirdtool.LearningFacade.application.port.out.suggestion.AxisSuggestionPort;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.AxisTopicSuggestionPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story 1-2 AC #1 — {@code thirdtool.suggestion.provider}의 양방향 동작 검증.
 *
 * <ul>
 *   <li>{@code provider=static} 또는 미설정 → 두 Static Adapter Bean 활성화 (matchIfMissing=true)</li>
 *   <li>{@code provider=llm} (또는 알 수 없는 값) → 두 Static Adapter Bean 비활성화</li>
 * </ul>
 *
 * <p>현 시점 LLM Adapter는 미구현이라 비활성화 케이스에선 Port Bean이 0개. Application Service가
 * 아직 Port를 주입받지 않으므로 컨텍스트 로딩은 정상. Story 2-1에서 LLM Adapter가 도입되면
 * 동일 패턴으로 LLM 활성화 케이스도 함께 검증된다.</p>
 */
@DisplayName("Static Suggestion Adapter — provider 조건부 활성화")
class StaticSuggestionAdapterConditionalTest {

    @Nested
    @SpringBootTest(properties = "thirdtool.suggestion.provider=static")
    @ActiveProfiles("test")
    @DisplayName("provider=static (또는 미설정 matchIfMissing)")
    class WhenStaticProvider {

        @Autowired
        private AxisSuggestionPort axisPort;

        @Autowired
        private AxisTopicSuggestionPort axisTopicPort;

        @Test
        @DisplayName("AxisSuggestionPort는 Static 구현체로 주입된다")
        void axisPort_static_구현체_주입() {
            assertThat(axisPort).isInstanceOf(StaticAxisSuggestionAdapter.class);
        }

        @Test
        @DisplayName("AxisTopicSuggestionPort는 Static 구현체로 주입된다")
        void axisTopicPort_static_구현체_주입() {
            assertThat(axisTopicPort).isInstanceOf(StaticAxisTopicSuggestionAdapter.class);
        }
    }

    @Nested
    @SpringBootTest(properties = "thirdtool.suggestion.provider=llm")
    @ActiveProfiles("test")
    @DisplayName("provider=llm (비-static)")
    class WhenNonStaticProvider {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("StaticAxisSuggestionAdapter Bean이 컨텍스트에 등록되지 않는다")
        void staticAxisAdapter_bean_비활성화() {
            assertThat(context.getBeansOfType(StaticAxisSuggestionAdapter.class)).isEmpty();
        }

        @Test
        @DisplayName("StaticAxisTopicSuggestionAdapter Bean이 컨텍스트에 등록되지 않는다")
        void staticAxisTopicAdapter_bean_비활성화() {
            assertThat(context.getBeansOfType(StaticAxisTopicSuggestionAdapter.class)).isEmpty();
        }
    }
}
