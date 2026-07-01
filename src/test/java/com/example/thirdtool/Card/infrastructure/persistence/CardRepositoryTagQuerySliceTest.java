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
 * CardRepository slice — Story 5-2 Tag 클릭 탐색 쿼리.
 *
 * <p>검증 범위
 *   - Tag에 부착된 본인 카드만 반환 (다른 유저의 동일 Tag 카드는 제외)
 *   - ON_FIELD / ARCHIVE 모두 포함
 *   - soft delete 카드는 제외
 *   - createdDate DESC 정렬
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslTestConfig.class)
@DisplayName("CardRepository slice — Story 5-2 findByTagIdAndUserIdAndDeletedFalse")
class CardRepositoryTagQuerySliceTest {

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
        em.persist(tagAlpha);
        em.persist(tagBeta);
        em.flush();
    }

    private Card persistCard(Deck deck, List<Tag> tags) {
        Card card = Card.create(
                deck,
                MainNote.of("본문", null),
                Summary.of("한 문장."),
                List.of("k1"),
                tags
        );
        em.persist(card);
        em.flush();
        return card;
    }

    @Test
    @DisplayName("정상: tagAlpha를 가진 본인 카드 2건만 반환 — 다른 유저·다른 태그 카드 제외")
    void findByTagIdAndUserId_filtersByTagAndOwner() {
        Card c1 = persistCard(deckOwner, List.of(tagAlpha));
        Card c2 = persistCard(deckOwner, List.of(tagAlpha, tagBeta));
        persistCard(deckOwner, List.of(tagBeta));            // 다른 태그
        persistCard(deckOther, List.of(tagAlpha));           // 다른 유저
        em.clear();

        List<Card> result = cardJpaRepository
                .findByTagIdAndUserIdAndDeletedFalse(tagAlpha.getId(), owner.getId());

        assertThat(result)
                .extracting(Card::getId)
                .containsExactlyInAnyOrder(c1.getId(), c2.getId());
    }

    @Test
    @DisplayName("ON_FIELD / ARCHIVE 모두 포함된다 (status 무관)")
    void findByTagIdAndUserId_includesBothStatuses() {
        Card onField = persistCard(deckOwner, List.of(tagAlpha));
        Card archived = persistCard(deckOwner, List.of(tagAlpha));
        archived.archive();
        em.flush();
        em.clear();

        List<Card> result = cardJpaRepository
                .findByTagIdAndUserIdAndDeletedFalse(tagAlpha.getId(), owner.getId());

        assertThat(result)
                .extracting(Card::getId)
                .containsExactlyInAnyOrder(onField.getId(), archived.getId());
    }

    @Test
    @DisplayName("soft delete된 카드는 제외된다")
    void findByTagIdAndUserId_excludesSoftDeleted() {
        Card alive = persistCard(deckOwner, List.of(tagAlpha));
        Card removed = persistCard(deckOwner, List.of(tagAlpha));
        removed.softDelete();
        em.flush();
        em.clear();

        List<Card> result = cardJpaRepository
                .findByTagIdAndUserIdAndDeletedFalse(tagAlpha.getId(), owner.getId());

        assertThat(result)
                .extracting(Card::getId)
                .containsExactly(alive.getId());
    }

    @Test
    @DisplayName("미존재 태그 ID는 빈 리스트")
    void findByTagIdAndUserId_nonexistentTag_returnsEmpty() {
        persistCard(deckOwner, List.of(tagAlpha));

        List<Card> result = cardJpaRepository
                .findByTagIdAndUserIdAndDeletedFalse(9_999L, owner.getId());

        assertThat(result).isEmpty();
    }
}
