package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.MainNote;
import com.example.thirdtool.Card.domain.model.Summary;
import com.example.thirdtool.Card.domain.model.Tag;
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
 * CardRepository slice — Story 5-3 ARCHIVE 연결 후보 쿼리.
 *
 * <p>검증 범위
 *   - 본인 ARCHIVE 카드만 (ON_FIELD·다른 유저 카드 제외)
 *   - 자기 자신 제외
 *   - tagIds 중 하나만 일치해도 후보 포함 (OR 의미)
 *   - soft delete 제외
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslTestConfig.class)
@DisplayName("CardRepository slice — Story 5-3 findArchivedBySharedTagIdsAndUserId")
class CardRepositoryArchiveRelatedSliceTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    CardJpaRepository cardJpaRepository;

    private UserEntity owner;
    private UserEntity other;
    private Deck deckOwner;
    private Deck deckOther;
    private Tag tagAlpha;
    private Tag tagBeta;
    private Tag tagGamma;

    @BeforeEach
    void setUp() {
        owner = UserEntity.ofLocal("owner", "pw", "n", "o@e.com");
        other = UserEntity.ofLocal("other", "pw", "n", "x@e.com");
        em.persist(owner);
        em.persist(other);

        deckOwner = Deck.createFromAxis(owner, 1L, "owner 덱");
        deckOther = Deck.createFromAxis(other, 2L, "other 덱");
        em.persist(deckOwner);
        em.persist(deckOther);

        tagAlpha = Tag.of("백엔드");
        tagBeta  = Tag.of("프론트");
        tagGamma = Tag.of("인프라");
        em.persist(tagAlpha);
        em.persist(tagBeta);
        em.persist(tagGamma);
        em.flush();
    }

    private Card persistCard(Deck deck, boolean archived, List<Tag> tags) {
        Card card = Card.create(
                deck,
                MainNote.of("본문", null),
                Summary.of("한 문장."),
                List.of("k1"),
                tags
        );
        if (archived) card.archive();
        em.persist(card);
        em.flush();
        return card;
    }

    @Test
    @DisplayName("ARCHIVE 상태 본인 카드만 — ON_FIELD·다른 유저 카드 제외")
    void findArchivedShared_filtersByStatusAndOwner() {
        Card current = persistCard(deckOwner, false, List.of(tagAlpha, tagBeta));   // 호출 시작점
        Card a1 = persistCard(deckOwner, true,  List.of(tagAlpha));                 // ARCHIVE + Alpha 공유
        persistCard(deckOwner, false, List.of(tagAlpha));                            // ON_FIELD → 제외
        persistCard(deckOther, true,  List.of(tagAlpha));                            // 다른 유저 → 제외
        em.clear();

        List<Card> result = cardJpaRepository.findArchivedBySharedTagIdsAndUserId(
                List.of(tagAlpha.getId(), tagBeta.getId()),
                current.getId(),
                owner.getId()
        );

        assertThat(result).extracting(Card::getId).containsExactly(a1.getId());
    }

    @Test
    @DisplayName("자기 자신은 제외된다")
    void findArchivedShared_excludesCurrent() {
        // 현재 카드도 ARCHIVE이고 tag를 공유해도 자기 자신은 후보 아님
        Card current = persistCard(deckOwner, true, List.of(tagAlpha));
        em.clear();

        List<Card> result = cardJpaRepository.findArchivedBySharedTagIdsAndUserId(
                List.of(tagAlpha.getId()),
                current.getId(),
                owner.getId()
        );

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("tagIds 중 하나만 일치해도 후보 — OR 의미 + DISTINCT")
    void findArchivedShared_anyTagMatch() {
        Card current = persistCard(deckOwner, false, List.of(tagAlpha, tagGamma));
        Card a1 = persistCard(deckOwner, true,  List.of(tagAlpha));                 // alpha만
        Card a2 = persistCard(deckOwner, true,  List.of(tagGamma));                 // gamma만
        Card a3 = persistCard(deckOwner, true,  List.of(tagAlpha, tagGamma));       // 둘 다 — DISTINCT로 1건
        persistCard(deckOwner, true, List.of(tagBeta));                              // beta만 → 제외
        em.clear();

        List<Card> result = cardJpaRepository.findArchivedBySharedTagIdsAndUserId(
                List.of(tagAlpha.getId(), tagGamma.getId()),
                current.getId(),
                owner.getId()
        );

        assertThat(result).extracting(Card::getId)
                .containsExactlyInAnyOrder(a1.getId(), a2.getId(), a3.getId());
    }

    @Test
    @DisplayName("soft delete된 ARCHIVE 카드는 제외")
    void findArchivedShared_excludesSoftDeleted() {
        Card current = persistCard(deckOwner, false, List.of(tagAlpha));
        Card alive = persistCard(deckOwner, true, List.of(tagAlpha));
        Card removed = persistCard(deckOwner, true, List.of(tagAlpha));
        removed.softDelete();
        em.flush();
        em.clear();

        List<Card> result = cardJpaRepository.findArchivedBySharedTagIdsAndUserId(
                List.of(tagAlpha.getId()),
                current.getId(),
                owner.getId()
        );

        assertThat(result).extracting(Card::getId).containsExactly(alive.getId());
    }
}
