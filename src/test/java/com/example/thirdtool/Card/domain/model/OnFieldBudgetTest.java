package com.example.thirdtool.Card.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.thirdtool.support.DomainFixture.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("OnFieldBudget")
class OnFieldBudgetTest {

    @Nested
    @DisplayName("of() — 생성 및 유효성 검증")
    class Create {

        @Test
        @DisplayName("유효한 파라미터로 OnFieldBudget을 생성할 수 있다")
        void of_validParams_success() {
            // given
            int maxView = 3;
            Duration maxDuration = Duration.ofDays(10);

            // when & then
            assertThatCode(() -> OnFieldBudget.of(maxView, maxDuration))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("maxView가 0이면 IllegalArgumentException이 발생한다")
        void of_zeroMaxView_throwsIllegalArgumentException() {
            // given - no setup

            // when & then
            assertThatThrownBy(() -> OnFieldBudget.of(0, Duration.ofDays(10)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("maxDuration이 null이면 IllegalArgumentException이 발생한다")
        void of_nullMaxDuration_throwsIllegalArgumentException() {
            // given - no setup

            // when & then
            assertThatThrownBy(() -> OnFieldBudget.of(3, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("maxDuration이 0이면 IllegalArgumentException이 발생한다")
        void of_zeroMaxDuration_throwsIllegalArgumentException() {
            // given - no setup

            // when & then
            assertThatThrownBy(() -> OnFieldBudget.of(3, Duration.ZERO))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("resolveReason() — 만료 판단")
    class ResolveReason {

        @Test
        @DisplayName("이미 ARCHIVE 상태인 카드는 항상 Optional.empty()를 반환한다")
        void resolveReason_archivedCard_returnsEmpty() {
            // given
            Card card = archivedCard();
            OnFieldBudget budget = sampleBudget();

            // when
            Optional<ArchiveReason> reason = budget.resolveReason(card);

            // then
            assertThat(reason).isEmpty();
        }

        @Test
        @DisplayName("viewCount가 maxView에 도달하면 MAX_VIEW를 반환한다")
        void resolveReason_maxViewReached_returnsMaxViewReason() {
            // given
            Card card = sampleCard();
            OnFieldBudget budget = OnFieldBudget.of(3, Duration.ofDays(10));
            card.recordView();
            card.recordView();
            card.recordView(); // viewCount=3 = maxView

            // when
            Optional<ArchiveReason> reason = budget.resolveReason(card);

            // then
            assertThat(reason).hasValue(ArchiveReason.MAX_VIEW);
        }

        @Test
        @DisplayName("체류 기간이 maxDuration을 초과하면 MAX_DURATION을 반환한다")
        void resolveReason_durationExceeded_returnsMaxDurationReason() {
            // given
            Card card = sampleCardWithEnteredFieldAt(LocalDateTime.now().minusDays(11));
            OnFieldBudget budget = OnFieldBudget.of(3, Duration.ofDays(10));

            // when
            Optional<ArchiveReason> reason = budget.resolveReason(card);

            // then
            assertThat(reason).hasValue(ArchiveReason.MAX_DURATION);
        }

        @Test
        @DisplayName("MAX_VIEW와 MAX_DURATION이 동시에 성립하면 MAX_VIEW가 우선 반환된다")
        void resolveReason_bothConditionsMet_prioritizesMaxView() {
            // given
            Card card = sampleCardWithEnteredFieldAt(LocalDateTime.now().minusDays(11));
            OnFieldBudget budget = OnFieldBudget.of(3, Duration.ofDays(10));
            card.recordView();
            card.recordView();
            card.recordView(); // viewCount=maxView + duration 초과 동시 성립

            // when
            Optional<ArchiveReason> reason = budget.resolveReason(card);

            // then
            // 우선순위: MAX_VIEW → MAX_DURATION. 역전 시 이력 reason 오염
            assertThat(reason).hasValue(ArchiveReason.MAX_VIEW);
        }

        @Test
        @DisplayName("만료 조건이 없으면 Optional.empty()를 반환한다")
        void resolveReason_noConditionMet_returnsEmpty() {
            // given
            Card card = sampleCard();
            OnFieldBudget budget = OnFieldBudget.of(3, Duration.ofDays(10));
            card.recordView(); // viewCount=1 < maxView=3

            // when
            Optional<ArchiveReason> reason = budget.resolveReason(card);

            // then
            assertThat(reason).isEmpty();
        }
    }
}