package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardRelationFinder;
import com.example.thirdtool.Card.domain.model.MainNote;
import com.example.thirdtool.Card.domain.model.Summary;
import com.example.thirdtool.Card.domain.model.Tag;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.Card.presentation.dto.CardResponse;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CardQueryService.findByTag (Story 5-2) — Repository 위임 + DTO 매핑 검증.
 */
@DisplayName("CardQueryService — findByTag (Story 5-2)")
class CardQueryServiceFindByTagTest {

    private CardRepository cardRepository;
    private CardRelationFinder cardRelationFinder;
    private CardQueryService service;

    private UserEntity user;
    private Deck deck;

    @BeforeEach
    void setUp() {
        cardRepository     = mock(CardRepository.class);
        cardRelationFinder = mock(CardRelationFinder.class);
        service = new CardQueryService(cardRepository, cardRelationFinder);

        user = UserEntity.ofLocal("u", "pw", "n", "u@e.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        deck = Deck.createFromAxis(user, 10L, "DDD");
        ReflectionTestUtils.setField(deck, "id", 500L);
    }

    private Card sampleCard(Long id, Tag... tags) {
        Card card = Card.create(
                deck,
                MainNote.of("본문", null),
                Summary.of("한 문장."),
                List.of("k"),
                List.of(tags)
        );
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }

    @Test
    @DisplayName("Repository에 tagId·userId 그대로 위임 + 결과를 CardResponse.Summary로 매핑")
    void findByTag_delegatesAndMapsToSummary() {
        Tag tag = Tag.of("백엔드");
        ReflectionTestUtils.setField(tag, "id", 77L);
        Card c1 = sampleCard(1000L, tag);
        Card c2 = sampleCard(1001L, tag);

        when(cardRepository.findByTagIdAndUserIdAndDeletedFalse(eq(77L), eq(1L)))
                .thenReturn(List.of(c1, c2));

        List<CardResponse.Summary> result = service.findByTag(77L, 1L);

        verify(cardRepository, times(1)).findByTagIdAndUserIdAndDeletedFalse(77L, 1L);
        assertThat(result).extracting(CardResponse.Summary::cardId)
                          .containsExactly(1000L, 1001L);
    }

    @Test
    @DisplayName("결과 0건이면 빈 리스트 — 예외 미발생 (미존재 Tag도 동일 분기)")
    void findByTag_emptyResult_returnsEmptyList() {
        when(cardRepository.findByTagIdAndUserIdAndDeletedFalse(eq(9_999L), eq(1L)))
                .thenReturn(List.of());

        List<CardResponse.Summary> result = service.findByTag(9_999L, 1L);

        assertThat(result).isEmpty();
    }
}
