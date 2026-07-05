package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LearningLayer.progressStatus() 도메인 파생 메서드 단위 테스트 (LT E6 · M5 · Story 6-4).
 *
 * <p>SDD 규칙 4 케이스 모두 검증:
 * <ul>
 *   <li>축 0건 → NOT_STARTED</li>
 *   <li>모두 NOT_STARTED → NOT_STARTED</li>
 *   <li>모두 COMPLETED → COMPLETED</li>
 *   <li>혼재 · IN_PROGRESS 존재 → IN_PROGRESS</li>
 * </ul>
 */
@DisplayName("LearningLayer.progressStatus 파생 (LT E6 · Story 6-4)")
class LearningLayerProgressStatusTest {

    private LearningFacade facade;
    private LearningLayer layer;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.ofLocal(
                "tester", "encoded-pw", "닉네임", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        facade = LearningFacade.create(user, "백엔드 개발자");
        // default Uncategorized layer 첫 진입점 사용
        layer = facade.getLayers().get(0);
    }

    @Nested
    @DisplayName("파생 규칙 (SDD)")
    class DerivationRules {

        @Test
        @DisplayName("해피: 활성 axis 0건 → NOT_STARTED (열린 질문 10 잠정)")
        void 축0건_NOT_STARTED() {
            assertThat(layer.getAxes()).isEmpty();

            assertThat(layer.progressStatus()).isEqualTo(LayerProgressStatus.NOT_STARTED);
        }

        @Test
        @DisplayName("해피: 모든 axis가 NOT_STARTED → NOT_STARTED")
        void 모두NOT_STARTED_NOT_STARTED() {
            LearningAxis a1 = layer.addAxis("API 설계");
            LearningAxis a2 = layer.addAxis("DB 모델링");
            // 초기 progressStatus는 NOT_STARTED default

            assertThat(a1.getProgressStatus()).isEqualTo(AxisProgressStatus.NOT_STARTED);
            assertThat(a2.getProgressStatus()).isEqualTo(AxisProgressStatus.NOT_STARTED);
            assertThat(layer.progressStatus()).isEqualTo(LayerProgressStatus.NOT_STARTED);
        }

        @Test
        @DisplayName("해피: 모든 axis가 COMPLETED → COMPLETED")
        void 모두COMPLETED_COMPLETED() {
            LearningAxis a1 = layer.addAxis("API 설계");
            LearningAxis a2 = layer.addAxis("DB 모델링");
            a1.recalculateProgressStatus(0, 5); // COMPLETED
            a2.recalculateProgressStatus(0, 3); // COMPLETED

            assertThat(a1.getProgressStatus()).isEqualTo(AxisProgressStatus.COMPLETED);
            assertThat(a2.getProgressStatus()).isEqualTo(AxisProgressStatus.COMPLETED);
            assertThat(layer.progressStatus()).isEqualTo(LayerProgressStatus.COMPLETED);
        }

        @Test
        @DisplayName("해피: 하나 이상 IN_PROGRESS → IN_PROGRESS")
        void 하나IN_PROGRESS_IN_PROGRESS() {
            LearningAxis a1 = layer.addAxis("API 설계");
            LearningAxis a2 = layer.addAxis("DB 모델링");
            LearningAxis a3 = layer.addAxis("테스트 전략");
            a1.recalculateProgressStatus(3, 0); // IN_PROGRESS
            a2.recalculateProgressStatus(0, 5); // COMPLETED
            // a3는 NOT_STARTED

            assertThat(layer.progressStatus()).isEqualTo(LayerProgressStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("엣지: NOT_STARTED · COMPLETED 혼재 (IN_PROGRESS 없음) → IN_PROGRESS")
        void 혼재_IN_PROGRESS() {
            LearningAxis a1 = layer.addAxis("API 설계");
            LearningAxis a2 = layer.addAxis("DB 모델링");
            a2.recalculateProgressStatus(0, 5); // COMPLETED
            // a1은 NOT_STARTED

            // SDD 규칙: "모두 NOT_STARTED" 도 "모두 COMPLETED" 도 아니므로 IN_PROGRESS
            assertThat(layer.progressStatus()).isEqualTo(LayerProgressStatus.IN_PROGRESS);
        }
    }
}
