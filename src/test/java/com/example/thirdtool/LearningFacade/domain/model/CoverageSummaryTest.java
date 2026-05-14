package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CoverageSummary")
class CoverageSummaryTest {

    @Nested
    @DisplayName("팩토리")
    class Factory {

        @Test
        @DisplayName("of(u, p, c)는 total = u+p+c로 계산해 생성한다")
        void of_total_자동합산() {
            CoverageSummary summary = CoverageSummary.of(3, 4, 7);

            assertThat(summary.uncoveredTopics()).isEqualTo(3);
            assertThat(summary.partialTopics()).isEqualTo(4);
            assertThat(summary.coveredTopics()).isEqualTo(7);
            assertThat(summary.totalTopics()).isEqualTo(14);
        }

        @Test
        @DisplayName("empty()는 모든 카운트가 0인 인스턴스를 반환한다")
        void empty_모든카운트_0() {
            CoverageSummary summary = CoverageSummary.empty();

            assertThat(summary.totalTopics()).isZero();
            assertThat(summary.uncoveredTopics()).isZero();
            assertThat(summary.partialTopics()).isZero();
            assertThat(summary.coveredTopics()).isZero();
        }
    }

    @Nested
    @DisplayName("검증")
    class Validation {

        @Test
        @DisplayName("음수 카운트로 생성 시 예외가 발생한다")
        void 음수카운트_예외() {
            assertThatThrownBy(() -> new CoverageSummary(0, -1, 0, 0))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("uncovered + partial + covered가 total과 다르면 예외가 발생한다")
        void 합산불일치_예외() {
            assertThatThrownBy(() -> new CoverageSummary(10, 3, 4, 7))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    @Nested
    @DisplayName("hasGap")
    class HasGap {

        @Test
        @DisplayName("uncoveredTopics가 0이면 hasGap은 false")
        void hasGap_uncovered0_false() {
            assertThat(CoverageSummary.of(0, 2, 3).hasGap()).isFalse();
        }

        @Test
        @DisplayName("uncoveredTopics가 1 이상이면 hasGap은 true")
        void hasGap_uncovered양수_true() {
            assertThat(CoverageSummary.of(1, 2, 3).hasGap()).isTrue();
        }

        @Test
        @DisplayName("empty()는 hasGap이 false")
        void hasGap_empty_false() {
            assertThat(CoverageSummary.empty().hasGap()).isFalse();
        }
    }
}
