package com.example.thirdtool.Card.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.thirdtool.support.DomainFixture.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("CardExpiryPolicy")
class CardExpiryPolicyTest {

    private final CardExpiryPolicy policy = new CardExpiryPolicy();

    @Test
    @DisplayName("이미 ARCHIVE 상태인 카드는 Optional.empty()를 반환하고 상태가 변경되지 않는다")
    void expire_alreadyArchivedCard_returnsEmpty() {
        // given
        Card card = archivedCard();
        OnFieldBudget budget = sampleBudget();

        // when
        Optional<ArchiveReason> reason = policy.expire(card, budget);

        // then
        assertThat(reason).isEmpty();
        assertThat(card.isArchived()).isTrue();
    }

    @Test
    @DisplayName("viewCount가 maxView에 도달하면 MAX_VIEW를 반환하고 카드를 ARCHIVE로 전환한다")
    void expire_maxViewReached_returnsMaxViewReasonAndArchives() {
        // given
        Card card = sampleCard();
        OnFieldBudget budget = OnFieldBudget.of(3, Duration.ofDays(10));
        card.recordView();
        card.recordView();
        card.recordView(); // viewCount=3 = maxView

        // when
        Optional<ArchiveReason> reason = policy.expire(card, budget);

        // then
        assertThat(reason).hasValue(ArchiveReason.MAX_VIEW);
        assertThat(card.isArchived()).isTrue();
    }

    @Test
    @DisplayName("체류 기간이 maxDuration을 초과하면 MAX_DURATION을 반환하고 카드를 ARCHIVE로 전환한다")
    void expire_durationExceeded_returnsMaxDurationReasonAndArchives() {
        // given
        Card card = sampleCardWithEnteredFieldAt(LocalDateTime.now().minusDays(11));
        OnFieldBudget budget = OnFieldBudget.of(3, Duration.ofDays(10));

        // when
        Optional<ArchiveReason> reason = policy.expire(card, budget);

        // then
        assertThat(reason).hasValue(ArchiveReason.MAX_DURATION);
        assertThat(card.isArchived()).isTrue();
    }

    @Test
    @DisplayName("만료 조건이 없으면 Optional.empty()를 반환하고 카드가 ON_FIELD를 유지한다")
    void expire_noConditionMet_returnsEmptyAndKeepsOnField() {
        // given
        Card card = sampleCard();
        OnFieldBudget budget = OnFieldBudget.of(3, Duration.ofDays(10));
        card.recordView(); // viewCount=1 < maxView=3

        // when
        Optional<ArchiveReason> reason = policy.expire(card, budget);

        // then
        assertThat(reason).isEmpty();
        assertThat(card.isOnField()).isTrue();
    }
}
