package com.example.thirdtool.Review.domain.model;

import com.example.thirdtool.Review.domain.exception.ReviewSessionException;
import com.example.thirdtool.UserSchedule.domain.model.LearningMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * REV E3 · Story 3-2/3-5 · Recommendation Aggregate 도메인 단위 테스트.
 */
@DisplayName("Recommendation (REV E3 · Story 3-2/3-5)")
class RecommendationTest {

    @Nested
    @DisplayName("of 팩토리 · 방향 검증")
    class Create {

        @Test
        @DisplayName("해피: SUGGEST_DOWNGRADE MODE_14D→MODE_7D · 정상 생성")
        void downgrade_해피() {
            Recommendation r = Recommendation.of(1L, RecommendationType.SUGGEST_DOWNGRADE,
                    LearningMode.MODE_14D, LearningMode.MODE_7D,
                    "3주 평균 42%", LocalDateTime.now());

            assertThat(r.getUserId()).isEqualTo(1L);
            assertThat(r.getType()).isEqualTo(RecommendationType.SUGGEST_DOWNGRADE);
            assertThat(r.isResolved()).isFalse();
            assertThat(r.getAction()).isNull();
        }

        @Test
        @DisplayName("해피: SUGGEST_UPGRADE MODE_14D→MODE_28D · 정상 생성")
        void upgrade_해피() {
            Recommendation r = Recommendation.of(1L, RecommendationType.SUGGEST_UPGRADE,
                    LearningMode.MODE_14D, LearningMode.MODE_28D,
                    "4주 perfect", LocalDateTime.now());

            assertThat(r.getFromMode()).isEqualTo(LearningMode.MODE_14D);
            assertThat(r.getToMode()).isEqualTo(LearningMode.MODE_28D);
        }

        @Test
        @DisplayName("예외: SUGGEST_DOWNGRADE인데 from < to (방향 반대)")
        void downgrade_방향_예외() {
            assertThatThrownBy(() -> Recommendation.of(1L, RecommendationType.SUGGEST_DOWNGRADE,
                    LearningMode.MODE_7D, LearningMode.MODE_14D,
                    "잘못된 방향", LocalDateTime.now()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SUGGEST_DOWNGRADE는 fromMode > toMode");
        }

        @Test
        @DisplayName("예외: SUGGEST_UPGRADE인데 from > to (방향 반대)")
        void upgrade_방향_예외() {
            assertThatThrownBy(() -> Recommendation.of(1L, RecommendationType.SUGGEST_UPGRADE,
                    LearningMode.MODE_28D, LearningMode.MODE_14D,
                    "잘못된 방향", LocalDateTime.now()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SUGGEST_UPGRADE는 fromMode < toMode");
        }
    }

    @Nested
    @DisplayName("resolve · accept/dismiss")
    class Resolve {

        @Test
        @DisplayName("해피: ACCEPTED resolve · action + resolvedAt 세팅")
        void accepted_해피() {
            Recommendation r = downgradeRec();
            LocalDateTime now = LocalDateTime.now();

            r.resolve(RecommendationAction.ACCEPTED, now);

            assertThat(r.isResolved()).isTrue();
            assertThat(r.getAction()).isEqualTo(RecommendationAction.ACCEPTED);
            assertThat(r.getResolvedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("해피: DISMISSED resolve · action DISMISSED 세팅")
        void dismissed_해피() {
            Recommendation r = downgradeRec();

            r.resolve(RecommendationAction.DISMISSED, LocalDateTime.now());

            assertThat(r.getAction()).isEqualTo(RecommendationAction.DISMISSED);
        }

        @Test
        @DisplayName("예외: 이미 resolved에 재호출 · RECOMMENDATION_ALREADY_RESOLVED")
        void 재호출_예외() {
            Recommendation r = downgradeRec();
            r.resolve(RecommendationAction.ACCEPTED, LocalDateTime.now());

            assertThatThrownBy(() -> r.resolve(RecommendationAction.DISMISSED, LocalDateTime.now()))
                    .isInstanceOf(ReviewSessionException.class)
                    .hasMessageContaining("이미 처리된 추천");
        }
    }

    @Nested
    @DisplayName("isOwner")
    class Owner {

        @Test
        @DisplayName("해피/예외: 소유권 검증")
        void 소유권() {
            Recommendation r = downgradeRec();
            assertThat(r.isOwner(1L)).isTrue();
            assertThat(r.isOwner(2L)).isFalse();
        }
    }

    private Recommendation downgradeRec() {
        return Recommendation.of(1L, RecommendationType.SUGGEST_DOWNGRADE,
                LearningMode.MODE_14D, LearningMode.MODE_7D,
                "test", LocalDateTime.now());
    }
}
