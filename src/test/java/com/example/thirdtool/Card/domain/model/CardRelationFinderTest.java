package com.example.thirdtool.Card.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.thirdtool.support.DomainFixture.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("CardRelationFinder")
class CardRelationFinderTest {

    private final CardRelationFinder finder = new CardRelationFinder();

    @Test
    @DisplayName("taggedCards에 currentCard 자신이 포함되어 있어도 결과에서 제외된다")
    void findCandidates_currentCardInList_excludesSelf() {
        // given
        Tag tag = sampleTagWithId(1L, "트랜잭션");
        Card current = sampleCardWithId(1L);
        current.addTag(tag);

        // when
        List<RelatedCardCandidate> result = finder.findCandidates(current, List.of(current));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("공유 태그가 없는 카드는 결과에서 제외된다")
    void findCandidates_noSharedTags_excludesCard() {
        // given
        Tag tagA = sampleTagWithId(1L, "A");
        Tag tagB = sampleTagWithId(2L, "B");
        Card current = sampleCardWithId(1L);
        current.addTag(tagA);
        Card other = sampleCardWithId(2L);
        other.addTag(tagB); // 공유 태그 없음

        // when
        List<RelatedCardCandidate> result = finder.findCandidates(current, List.of(other));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("태그가 없는 카드는 결과에서 제외된다")
    void findCandidates_cardWithNoTags_excludesCard() {
        // given
        Tag tag = sampleTagWithId(1L, "트랜잭션");
        Card current = sampleCardWithId(1L);
        current.addTag(tag);
        Card noTagCard = sampleCardWithId(2L); // 태그 없음

        // when
        List<RelatedCardCandidate> result = finder.findCandidates(current, List.of(noTagCard));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("공유 태그 수가 많은 카드가 먼저 오도록 내림차순 정렬된다")
    void findCandidates_multipleCards_sortedBySharedTagCountDesc() {
        // given
        Tag tagA = sampleTagWithId(1L, "A");
        Tag tagB = sampleTagWithId(2L, "B");
        Card current = sampleCardWithId(1L);
        current.addTag(tagA);
        current.addTag(tagB);

        Card cardWith2SharedTags = sampleCardWithId(2L);
        cardWith2SharedTags.addTag(tagA);
        cardWith2SharedTags.addTag(tagB); // 공유 2개

        Card cardWith1SharedTag = sampleCardWithId(3L);
        cardWith1SharedTag.addTag(tagA); // 공유 1개

        // when
        List<RelatedCardCandidate> result = finder.findCandidates(
                current, List.of(cardWith1SharedTag, cardWith2SharedTags)
                                                                 );

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSharedTagCount()).isEqualTo(2);
        assertThat(result.get(1).getSharedTagCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("ARCHIVE 상태인 카드도 공유 태그가 있으면 결과에 포함된다 — 배경 지식 접근 허용")
    void findCandidates_archivedCardWithSharedTag_includesInResult() {
        // given
        Tag tag = sampleTagWithId(1L, "트랜잭션");
        Card current = sampleCardWithId(1L);
        current.addTag(tag);
        Card archived = sampleCardWithId(2L);
        archived.addTag(tag);
        archived.archive();

        // when
        List<RelatedCardCandidate> result = finder.findCandidates(current, List.of(archived));

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("taggedCards가 빈 리스트이면 빈 결과를 반환한다")
    void findCandidates_emptyInput_returnsEmptyList() {
        // given
        Card current = sampleCardWithId(1L);

        // when
        List<RelatedCardCandidate> result = finder.findCandidates(current, List.of());

        // then
        assertThat(result).isEmpty();
    }
}