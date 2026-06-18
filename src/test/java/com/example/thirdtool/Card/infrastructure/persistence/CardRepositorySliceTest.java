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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * CardRepository slice 테스트 (@DataJpaTest + H2).
 *
 * <p>Story 1-1 — V9 마이그레이션으로 추가된 운영 위치·체류 추적 컬럼이 JPA round-trip에서 정합한지 검증한다.
 * 검증 범위
 *   - status / enteredFieldAt / viewCount / lastViewedAt 저장·로드
 *   - archive() / returnToField() 호출 후 다시 로드해도 상태가 보존되는지
 *   - findAllByStatusAndDeletedFalse 쿼리가 status 기준으로 정확히 필터링하는지
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslTestConfig.class)
@DisplayName("CardRepository slice — Story 1-1 운영 위치 컬럼")
class CardRepositorySliceTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    CardJpaRepository cardJpaRepository;

    private UserEntity user;
    private Deck deck;

    @BeforeEach
    void setUp() {
        user = UserEntity.ofLocal("tester-1", "encoded-pw", "닉네임", "tester1@example.com");
        em.persist(user);
        deck = Deck.of("테스트 덱", null, user);
        em.persist(deck);
        em.flush();
    }

    @Test
    @DisplayName("Card 저장 시 status는 ON_FIELD, viewCount는 0, enteredFieldAt은 set된 상태로 round-trip된다")
    void save_initialOnFieldFields_roundTrip() {
        // given
        Card card = Card.create(
                deck,
                MainNote.of("스택은 LIFO 구조다.", null),
                Summary.of("스택은 LIFO 구조다."),
                List.of("LIFO", "push", "pop")
        );

        // when
        Card saved = cardJpaRepository.save(card);
        em.flush();
        em.clear();

        Card found = cardJpaRepository.findById(saved.getId()).orElseThrow();

        // then
        assertThat(found.getStatus()).isEqualTo(CardStatus.ON_FIELD);
        assertThat(found.getViewCount()).isZero();
        assertThat(found.getEnteredFieldAt()).isNotNull();
        assertThat(found.getLastViewedAt()).isNull();
    }

    @Test
    @DisplayName("recordView() 후 viewCount + lastViewedAt이 영속·재조회된다")
    void recordView_persistsViewCountAndLastViewedAt() {
        // given
        Card card = persistCard();

        // when
        card.recordView();
        em.flush();
        em.clear();

        Card found = cardJpaRepository.findById(card.getId()).orElseThrow();

        // then
        assertThat(found.getViewCount()).isEqualTo(1);
        assertThat(found.getLastViewedAt()).isNotNull();
    }

    @Test
    @DisplayName("archive() 후 status=ARCHIVE가 영속·재조회된다 — enteredFieldAt은 보존")
    void archive_persistsStatus_preservesEnteredFieldAt() {
        // given
        Card card = persistCard();
        LocalDateTime originalEnteredFieldAt = card.getEnteredFieldAt();

        // when
        card.archive();
        em.flush();
        em.clear();

        Card found = cardJpaRepository.findById(card.getId()).orElseThrow();

        // then — DATETIME(6) microsecond 정밀도 + DB 측 반올림 가능성으로 1μs 허용 오차.
        assertThat(found.getStatus()).isEqualTo(CardStatus.ARCHIVE);
        assertThat(found.getEnteredFieldAt())
                .isCloseTo(originalEnteredFieldAt, within(Duration.ofNanos(1_000)));
    }

    @Test
    @DisplayName("returnToField() 후 status=ON_FIELD + viewCount/lastViewedAt 재초기화가 영속·재조회된다")
    void returnToField_persistsResetFields() {
        // given
        Card card = persistCard();
        card.recordView();
        card.archive();
        em.flush();

        // when
        card.returnToField();
        em.flush();
        em.clear();

        Card found = cardJpaRepository.findById(card.getId()).orElseThrow();

        // then
        assertThat(found.getStatus()).isEqualTo(CardStatus.ON_FIELD);
        assertThat(found.getViewCount()).isZero();
        assertThat(found.getLastViewedAt()).isNull();
        assertThat(found.getEnteredFieldAt()).isNotNull();
    }

    @Test
    @DisplayName("findAllByStatusAndDeletedFalse — status 기준 필터링이 동작한다")
    void findAllByStatus_filtersCorrectly() {
        // given
        Card onField1 = persistCard();
        Card onField2 = persistCard();
        Card archived = persistCard();
        archived.archive();
        em.flush();
        em.clear();

        // when
        List<Card> onFieldCards = cardJpaRepository.findAllByStatusAndDeletedFalse(CardStatus.ON_FIELD);
        List<Card> archivedCards = cardJpaRepository.findAllByStatusAndDeletedFalse(CardStatus.ARCHIVE);

        // then
        assertThat(onFieldCards)
                .extracting(Card::getId)
                .containsExactlyInAnyOrder(onField1.getId(), onField2.getId());
        assertThat(archivedCards)
                .extracting(Card::getId)
                .containsExactly(archived.getId());
    }

    @Test
    @DisplayName("soft delete된 카드는 findAllByStatusAndDeletedFalse 결과에서 제외된다")
    void findAllByStatus_excludesSoftDeleted() {
        // given
        Card alive = persistCard();
        Card removed = persistCard();
        removed.softDelete();
        em.flush();
        em.clear();

        // when
        List<Card> result = cardJpaRepository.findAllByStatusAndDeletedFalse(CardStatus.ON_FIELD);

        // then
        assertThat(result)
                .extracting(Card::getId)
                .containsExactly(alive.getId());
    }

    // ─── helper ────────────────────────────────────────────────────────────────

    private Card persistCard() {
        Card card = Card.create(
                deck,
                MainNote.of("스택은 LIFO 구조다.", null),
                Summary.of("스택은 LIFO 구조다."),
                List.of("LIFO", "push", "pop")
        );
        em.persist(card);
        em.flush();
        return card;
    }
}
