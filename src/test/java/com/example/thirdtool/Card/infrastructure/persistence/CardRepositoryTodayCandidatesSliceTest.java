package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.MainNote;
import com.example.thirdtool.Card.domain.model.Summary;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.support.QuerydslTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story 6-1 — findOnFieldEligibleByUserId 슬라이스.
 *
 * <p>검증
 *   - 본인 ON_FIELD 활성 카드만 (다른 유저·ARCHIVE·soft delete 제외)
 *   - lastViewedAt이 null이거나 threshold 이전이면 포함
 *   - lastViewedAt이 threshold 이후이면 제외
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslTestConfig.class)
@DisplayName("CardRepository slice — Story 6-1 findOnFieldEligibleByUserId")
class CardRepositoryTodayCandidatesSliceTest {

    @Autowired TestEntityManager em;
    @Autowired CardJpaRepository cardJpaRepository;

    private UserEntity owner;
    private UserEntity other;
    private Deck deckOwner;
    private Deck deckOther;

    @BeforeEach
    void setUp() {
        owner = UserEntity.ofLocal("owner", "pw", "n", "o@e.com");
        other = UserEntity.ofLocal("other", "pw", "n", "x@e.com");
        em.persist(owner);
        em.persist(other);

        deckOwner = Deck.createFromAxis(owner, 1L, "o");
        deckOther = Deck.createFromAxis(other, 2L, "x");
        em.persist(deckOwner);
        em.persist(deckOther);
        em.flush();
    }

    private Card persistCard(Deck deck, boolean archived, LocalDateTime lastViewedAt) {
        Card card = Card.create(deck, MainNote.of("본문", null), Summary.of("한 문장."), List.of("k"));
        if (archived) card.archive();
        if (lastViewedAt != null) ReflectionTestUtils.setField(card, "lastViewedAt", lastViewedAt);
        em.persist(card);
        em.flush();
        return card;
    }

    @Test
    @DisplayName("정상: lastViewedAt NULL인 본인 ON_FIELD 카드 포함, 다른 유저·ARCHIVE 제외")
    void eligible_includesNullLastViewedAt_ownerOnly() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);
        Card fresh = persistCard(deckOwner, false, null);                            // 신규(null) — 포함
        persistCard(deckOwner, true, null);                                          // ARCHIVE — 제외
        persistCard(deckOther, false, null);                                         // 다른 유저 — 제외
        em.clear();

        List<Card> result = cardJpaRepository.findOnFieldEligibleByUserId(owner.getId(), threshold);

        assertThat(result).extracting(Card::getId).containsExactly(fresh.getId());
    }

    @Test
    @DisplayName("lastViewedAt이 threshold 이전이면 포함, 이후면 제외")
    void eligible_thresholdFiltering() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);
        Card oldView    = persistCard(deckOwner, false, LocalDateTime.now().minusDays(2));  // threshold 이전 — 포함
        Card recentView = persistCard(deckOwner, false, LocalDateTime.now().minusHours(2)); // threshold 이후 — 제외
        em.clear();

        List<Card> result = cardJpaRepository.findOnFieldEligibleByUserId(owner.getId(), threshold);

        assertThat(result).extracting(Card::getId).containsExactly(oldView.getId());
    }

    @Test
    @DisplayName("soft delete된 카드는 제외")
    void eligible_excludesSoftDeleted() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);
        Card alive = persistCard(deckOwner, false, null);
        Card removed = persistCard(deckOwner, false, null);
        removed.softDelete();
        em.flush();
        em.clear();

        List<Card> result = cardJpaRepository.findOnFieldEligibleByUserId(owner.getId(), threshold);

        assertThat(result).extracting(Card::getId).containsExactly(alive.getId());
    }

    @Test
    @DisplayName("후보 0건은 빈 리스트")
    void eligible_emptyWhenNoMatches() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);

        List<Card> result = cardJpaRepository.findOnFieldEligibleByUserId(owner.getId(), threshold);

        assertThat(result).isEmpty();
    }
}
