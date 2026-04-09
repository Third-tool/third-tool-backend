package com.example.thirdtool.Card.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.example.thirdtool.support.DomainFixture.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("SoftScheduleTemplate")
class SoftScheduleTemplateTest {

    @Nested
    @DisplayName("of() — 생성 및 유효성 검증")
    class Create {

        @Test
        @DisplayName("유효한 intervalSteps로 SoftScheduleTemplate을 생성할 수 있다")
        void of_validSteps_success() {
            // given
            List<SoftScheduleTemplate.IntervalStep> steps = List.of(
                    new SoftScheduleTemplate.IntervalStep(Duration.ofDays(1), SoftScheduleState.INTERVAL_1D),
                    new SoftScheduleTemplate.IntervalStep(Duration.ofDays(3), SoftScheduleState.INTERVAL_3D)
                                                                   );

            // when & then
            assertThatCode(() -> SoftScheduleTemplate.of(steps))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("빈 intervalSteps로 생성하면 IllegalArgumentException이 발생한다")
        void of_emptySteps_throwsIllegalArgumentException() {
            // given - no setup

            // when & then
            assertThatThrownBy(() -> SoftScheduleTemplate.of(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null intervalSteps로 생성하면 IllegalArgumentException이 발생한다")
        void of_nullSteps_throwsIllegalArgumentException() {
            // given - no setup

            // when & then
            assertThatThrownBy(() -> SoftScheduleTemplate.of(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("역순으로 입력해도 내부적으로 minDuration 오름차순 정렬된다")
        void of_unsortedSteps_sortedByMinDurationAscending() {
            // given
            List<SoftScheduleTemplate.IntervalStep> unsortedSteps = List.of(
                    new SoftScheduleTemplate.IntervalStep(Duration.ofDays(7), SoftScheduleState.INTERVAL_7D),
                    new SoftScheduleTemplate.IntervalStep(Duration.ofDays(1), SoftScheduleState.INTERVAL_1D),
                    new SoftScheduleTemplate.IntervalStep(Duration.ofDays(3), SoftScheduleState.INTERVAL_3D)
                                                                           );

            // when
            SoftScheduleTemplate template = SoftScheduleTemplate.of(unsortedSteps);

            // then
            List<Long> durations = template.getIntervalSteps().stream()
                                           .map(s -> s.minDuration().toDays())
                                           .toList();
            assertThat(durations).containsExactly(1L, 3L, 7L);
        }
    }

    @Nested
    @DisplayName("resolveState() — 간격 단계 판단")
    class ResolveState {

        private final SoftScheduleTemplate template = defaultTemplate(); // [1일, 3일, 7일]

        @Test
        @DisplayName("한 번도 열람하지 않은 카드는 FRESH를 반환한다")
        void resolveState_neverViewed_returnsFresh() {
            // given
            Card card = sampleCard(); // lastViewedAt=null

            // when
            SoftScheduleState state = template.resolveState(card);

            // then
            assertThat(state).isEqualTo(SoftScheduleState.FRESH);
        }

        @Test
        @DisplayName("최소 간격 미충족 시 NOT_YET을 반환한다")
        void resolveState_belowMinInterval_returnsNotYet() {
            // given
            Card card = sampleCardWithLastViewedAt(LocalDateTime.now().minusHours(12));

            // when
            SoftScheduleState state = template.resolveState(card);

            // then
            assertThat(state).isEqualTo(SoftScheduleState.NOT_YET);
        }

        @Test
        @DisplayName("1일 경과 시 INTERVAL_1D를 반환한다")
        void resolveState_oneDayElapsed_returnsInterval1D() {
            // given
            Card card = sampleCardWithLastViewedAt(LocalDateTime.now().minusDays(1));

            // when
            SoftScheduleState state = template.resolveState(card);

            // then
            assertThat(state).isEqualTo(SoftScheduleState.INTERVAL_1D);
        }

        @Test
        @DisplayName("3일 경과 시 INTERVAL_3D를 반환한다")
        void resolveState_threeDaysElapsed_returnsInterval3D() {
            // given
            Card card = sampleCardWithLastViewedAt(LocalDateTime.now().minusDays(3));

            // when
            SoftScheduleState state = template.resolveState(card);

            // then
            assertThat(state).isEqualTo(SoftScheduleState.INTERVAL_3D);
        }

        @Test
        @DisplayName("5일 경과 시 간격을 건너뛰어 INTERVAL_3D를 반환한다 — 핵심 규칙: 최근 도달 단계 반환")
        void resolveState_fiveDaysElapsed_returnsInterval3D() {
            // given
            // 5일은 3일 이상이지만 7일 미충족 → INTERVAL_3D
            Card card = sampleCardWithLastViewedAt(LocalDateTime.now().minusDays(5));

            // when
            SoftScheduleState state = template.resolveState(card);

            // then
            assertThat(state).isEqualTo(SoftScheduleState.INTERVAL_3D);
        }

        @Test
        @DisplayName("7일 경과 시 INTERVAL_7D를 반환한다")
        void resolveState_sevenDaysElapsed_returnsInterval7D() {
            // given
            Card card = sampleCardWithLastViewedAt(LocalDateTime.now().minusDays(7));

            // when
            SoftScheduleState state = template.resolveState(card);

            // then
            assertThat(state).isEqualTo(SoftScheduleState.INTERVAL_7D);
        }
    }

    @Nested
    @DisplayName("isAvailable() — 노출 가능 여부")
    class IsAvailable {

        private final SoftScheduleTemplate template = defaultTemplate();

        @Test
        @DisplayName("FRESH 상태의 카드는 노출 가능하다")
        void isAvailable_freshCard_returnsTrue() {
            // given
            Card card = sampleCard(); // lastViewedAt=null → FRESH

            // when
            boolean available = template.isAvailable(card);

            // then
            assertThat(available).isTrue();
        }

        @Test
        @DisplayName("NOT_YET 상태의 카드는 노출 불가하다")
        void isAvailable_notYetCard_returnsFalse() {
            // given
            Card card = sampleCardWithLastViewedAt(LocalDateTime.now().minusHours(12));

            // when
            boolean available = template.isAvailable(card);

            // then
            assertThat(available).isFalse();
        }
    }
}