package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardStatus;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * fix-deck-axis-visibility (0.0.2v) Story 3 — findByUserIdAndAxisIdsAndStatus 슬라이스.
 *
 * <p>축 카드 뷰와 today 집계가 공유하는 단일 read-model의 필터 규칙 검증:
 *   - 본인 axis 결합 Deck의 해당 status 카드만 포함
 *   - 다른 유저·다른 status·soft delete 제외
 *   - 다중 axisIds 모두 수집
 *   - threshold(최소 간격) 필터를 갖지 않음 — eligibility 재판정은 상위(Review) 인메모리 책임
 *
 * <p>BE-Story 2(2026-07-01) 이후 axisId=null Deck 자체가 스키마 레벨에서 불가능해져
 * 고아 덱 관련 필터 케이스는 제거되었다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslTestConfig.class)
@DisplayName("CardRepository slice — Story 3 findByUserIdAndAxisIdsAndStatus")
class CardRepositoryAxisCardsSliceTest {

    private static final Long AXIS_A = 10L;
    private static final Long AXIS_B = 20L;

    @Autowired TestEntityManager em;
    @Autowired CardJpaRepository cardJpaRepository;

    private UserEntity owner;
    private UserEntity other;
    private Deck ownerAxisADeck;   // axisId = 10, owner
    private Deck ownerAxisBDeck;   // axisId = 20, owner
    private Deck otherAxisADeck;   // axisId = 10, other user

    @BeforeEach
    void setUp() {
        owner = UserEntity.ofLocal("owner", "pw", "n", "o@e.com");
        other = UserEntity.ofLocal("other", "pw", "n", "x@e.com");
        em.persist(owner);
        em.persist(other);

        ownerAxisADeck = Deck.createFromAxis(owner, AXIS_A, "owner-axisA");
        ownerAxisBDeck = Deck.createFromAxis(owner, AXIS_B, "owner-axisB");
        otherAxisADeck = Deck.createFromAxis(other, AXIS_A, "other-axisA");
        em.persist(ownerAxisADeck);
        em.persist(ownerAxisBDeck);
        em.persist(otherAxisADeck);
        em.flush();
    }

    private Card persistCard(Deck deck, boolean archived) {
        Card card = Card.create(deck, MainNote.of("본문", null), Summary.of("한 문장."), List.of("k"));
        if (archived) card.archive();
        em.persist(card);
        em.flush();
        return card;
    }

    @Test
    @DisplayName("정상: 본인 axis 결합 덱의 ON_FIELD 카드만 — 다른 유저·ARCHIVE 제외")
    void onField_axisScoped_ownerOnly() {
        Card included = persistCard(ownerAxisADeck, false);  // 포함
        persistCard(ownerAxisADeck, true);                   // ARCHIVE — 제외
        persistCard(otherAxisADeck, false);                  // 다른 유저 — 제외
        em.clear();

        List<Card> result = cardJpaRepository.findByUserIdAndAxisIdsAndStatus(
                owner.getId(), List.of(AXIS_A), CardStatus.ON_FIELD);

        assertThat(result).extracting(Card::getId).containsExactly(included.getId());
    }

    @Test
    @DisplayName("status=ARCHIVE 지정 시 ARCHIVE 카드만 반환")
    void archive_statusFilter() {
        persistCard(ownerAxisADeck, false);                 // ON_FIELD — 제외
        Card archived = persistCard(ownerAxisADeck, true);  // ARCHIVE — 포함
        em.clear();

        List<Card> result = cardJpaRepository.findByUserIdAndAxisIdsAndStatus(
                owner.getId(), List.of(AXIS_A), CardStatus.ARCHIVE);

        assertThat(result).extracting(Card::getId).containsExactly(archived.getId());
    }

    @Test
    @DisplayName("다중 axisIds — 두 축의 ON_FIELD 카드 모두 수집")
    void multipleAxisIds_collectsAll() {
        Card a = persistCard(ownerAxisADeck, false);
        Card b = persistCard(ownerAxisBDeck, false);
        em.clear();

        List<Card> result = cardJpaRepository.findByUserIdAndAxisIdsAndStatus(
                owner.getId(), List.of(AXIS_A, AXIS_B), CardStatus.ON_FIELD);

        assertThat(result).extracting(Card::getId).containsExactlyInAnyOrder(a.getId(), b.getId());
    }

    @Test
    @DisplayName("soft delete된 카드는 제외")
    void excludesSoftDeleted() {
        Card alive = persistCard(ownerAxisADeck, false);
        Card removed = persistCard(ownerAxisADeck, false);
        removed.softDelete();
        em.flush();
        em.clear();

        List<Card> result = cardJpaRepository.findByUserIdAndAxisIdsAndStatus(
                owner.getId(), List.of(AXIS_A), CardStatus.ON_FIELD);

        assertThat(result).extracting(Card::getId).containsExactly(alive.getId());
    }

    @Test
    @DisplayName("해당 축에 카드가 없으면 빈 리스트")
    void emptyWhenNoMatches() {
        persistCard(ownerAxisBDeck, false); // 다른 축에만 존재
        em.clear();

        List<Card> result = cardJpaRepository.findByUserIdAndAxisIdsAndStatus(
                owner.getId(), List.of(AXIS_A), CardStatus.ON_FIELD);

        assertThat(result).isEmpty();
    }
}
