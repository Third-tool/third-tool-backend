package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.MainNote;
import com.example.thirdtool.Card.domain.model.Summary;
import com.example.thirdtool.Card.domain.model.Tag;
import com.example.thirdtool.Card.infrastructure.dto.TagSummaryRow;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.support.QuerydslTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story 5-1 — Tag 관리 화면 슬라이스.
 *
 * <ul>
 *   <li>{@code findTagSummariesByUserId}: 본인 활성 카드의 Tag만 집계, value 사전순</li>
 *   <li>{@code detachTagFromUserCards}: 본인 카드 매핑만 일괄 제거, 다른 유저 영향 없음</li>
 * </ul>
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslTestConfig.class)
@DisplayName("Tag 관리 slice — Story 5-1")
class TagManagementSliceTest {

    @Autowired TestEntityManager em;
    @Autowired TagJpaRepository tagJpaRepository;
    @Autowired CardJpaRepository cardJpaRepository;

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

        tagAlpha = Tag.of("alpha");
        tagBeta  = Tag.of("beta");
        tagGamma = Tag.of("gamma");
        em.persist(tagAlpha);
        em.persist(tagBeta);
        em.persist(tagGamma);
        em.flush();
    }

    private Card persistCard(Deck deck, List<Tag> tags) {
        Card card = Card.create(
                deck, MainNote.of("본문", null), Summary.of("한 문장."), List.of("k"), tags
        );
        em.persist(card);
        em.flush();
        return card;
    }

    // ─── findTagSummariesByUserId ─────────────────────────────────────────────

    @Nested
    @DisplayName("findTagSummariesByUserId")
    class Summaries {

        @Test
        @DisplayName("정상: 본인 카드 Tag만 집계 + 각 Tag별 본인 카드 수 + value 사전순")
        void summaries_groupByTag_owner_only_sortedByValue() {
            persistCard(deckOwner, List.of(tagAlpha));
            persistCard(deckOwner, List.of(tagAlpha, tagBeta));
            persistCard(deckOwner, List.of(tagBeta));
            persistCard(deckOther, List.of(tagAlpha)); // 다른 유저 — 집계 제외
            em.clear();

            List<TagSummaryRow> result = tagJpaRepository.findTagSummariesByUserId(owner.getId());

            assertThat(result).hasSize(2);
            assertThat(result.get(0).value()).isEqualTo("alpha");
            assertThat(result.get(0).connectedCardCount()).isEqualTo(2);
            assertThat(result.get(1).value()).isEqualTo("beta");
            assertThat(result.get(1).connectedCardCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("soft delete된 카드의 부착은 카운트에서 제외")
        void summaries_excludeSoftDeleted() {
            persistCard(deckOwner, List.of(tagAlpha));
            Card removed = persistCard(deckOwner, List.of(tagAlpha));
            removed.softDelete();
            em.flush();
            em.clear();

            List<TagSummaryRow> result = tagJpaRepository.findTagSummariesByUserId(owner.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).value()).isEqualTo("alpha");
            assertThat(result.get(0).connectedCardCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Tag가 0개면 빈 리스트")
        void summaries_emptyWhenNoCards() {
            List<TagSummaryRow> result = tagJpaRepository.findTagSummariesByUserId(owner.getId());

            assertThat(result).isEmpty();
        }
    }

    // ─── detachTagFromUserCards ───────────────────────────────────────────────

    @Nested
    @DisplayName("detachTagFromUserCards")
    class Detach {

        @Test
        @DisplayName("본인 카드 부착만 일괄 해제 + 다른 유저 부착·다른 Tag·Tag row 자체는 보존")
        void detach_ownerOnly_preservesOtherUserAndTagRow() {
            persistCard(deckOwner, List.of(tagAlpha));               // 해제 대상
            persistCard(deckOwner, List.of(tagAlpha, tagBeta));      // alpha만 해제, beta 유지
            persistCard(deckOther, List.of(tagAlpha));               // 다른 유저 — 유지
            em.flush();

            int removed = cardJpaRepository.detachTagFromUserCards(owner.getId(), tagAlpha.getId());
            em.clear();

            assertThat(removed).isEqualTo(2);

            // 본인 카드의 Tag 카운트: alpha 0, beta 1
            List<TagSummaryRow> ownerSummaries =
                    tagJpaRepository.findTagSummariesByUserId(owner.getId());
            assertThat(ownerSummaries).hasSize(1);
            assertThat(ownerSummaries.get(0).value()).isEqualTo("beta");

            // 다른 유저 카드는 alpha 1건 그대로
            List<TagSummaryRow> otherSummaries =
                    tagJpaRepository.findTagSummariesByUserId(other.getId());
            assertThat(otherSummaries).hasSize(1);
            assertThat(otherSummaries.get(0).value()).isEqualTo("alpha");

            // Tag row 자체는 보존
            assertThat(tagJpaRepository.findByValue("alpha")).isPresent();
        }

        @Test
        @DisplayName("Tag가 어디에도 부착되지 않은 경우 0 반환")
        void detach_returnsZero_whenNoMatch() {
            int removed = cardJpaRepository.detachTagFromUserCards(owner.getId(), tagGamma.getId());

            assertThat(removed).isZero();
        }
    }
}
