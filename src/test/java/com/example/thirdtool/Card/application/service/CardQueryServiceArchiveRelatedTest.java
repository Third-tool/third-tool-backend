package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardRelationFinder;
import com.example.thirdtool.Card.domain.model.MainNote;
import com.example.thirdtool.Card.domain.model.Summary;
import com.example.thirdtool.Card.domain.model.Tag;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.Card.presentation.dto.CardResponse;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * CardQueryService.findArchiveRelated (Story 5-3) — 매트릭스.
 *
 * <p>Mockist + CardRelationFinder 실제 인스턴스 (도메인 서비스는 Classist).
 * Repository만 Mock으로 후보 풀을 통제, 정렬·자기제외는 도메인 서비스가 검증한다.
 */
@DisplayName("CardQueryService — findArchiveRelated (Story 5-3)")
class CardQueryServiceArchiveRelatedTest {

    private CardRepository cardRepository;
    private CardRelationFinder cardRelationFinder;
    private CardQueryService service;

    private UserEntity user;
    private Deck deck;

    @BeforeEach
    void setUp() {
        cardRepository     = mock(CardRepository.class);
        cardRelationFinder = new CardRelationFinder();   // 도메인 서비스는 실제
        service = new CardQueryService(cardRepository, cardRelationFinder);

        user = UserEntity.ofLocal("u", "pw", "n", "u@e.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        deck = Deck.createFromAxis(user, 10L, "DDD");
        ReflectionTestUtils.setField(deck, "id", 500L);
    }

    private Tag tag(Long id, String value) {
        Tag t = Tag.of(value);
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    private Card cardWith(Long id, Tag... tags) {
        Card c = Card.create(deck, MainNote.of("본문", null), Summary.of("한 문장."), List.of("k"), List.of(tags));
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    @Test
    @DisplayName("정상: 후보 카드 중 공통 Tag 수 내림차순 정렬되어 RelatedCard로 반환")
    void findArchiveRelated_sortedBySharedTagCountDesc() {
        Tag alpha = tag(10L, "백엔드");
        Tag beta  = tag(20L, "프론트");
        Tag gamma = tag(30L, "인프라");

        Card current = cardWith(1000L, alpha, beta, gamma);
        Card a1 = cardWith(2001L, alpha);                // 공통 1
        Card a2 = cardWith(2002L, alpha, beta);          // 공통 2
        Card a3 = cardWith(2003L, alpha, beta, gamma);   // 공통 3
        a1.archive(); a2.archive(); a3.archive();

        when(cardRepository.findById(1000L)).thenReturn(Optional.of(current));
        when(cardRepository.findArchivedBySharedTagIdsAndUserId(
                eq(List.of(alpha.getId(), beta.getId(), gamma.getId())),
                eq(1000L),
                eq(1L)
        )).thenReturn(List.of(a1, a2, a3));

        List<CardResponse.RelatedCard> result = service.findArchiveRelated(1000L, 1L);

        assertThat(result).extracting(CardResponse.RelatedCard::cardId)
                          .containsExactly(2003L, 2002L, 2001L);
        assertThat(result).extracting(CardResponse.RelatedCard::sharedTagCount)
                          .containsExactly(3, 2, 1);
    }

    @Test
    @DisplayName("현재 카드 Tag 0개면 Repository 호출 없이 빈 리스트 반환 (Spec 엣지 케이스)")
    void findArchiveRelated_noTags_emptyList_noRepoCall() {
        Card current = cardWith(1000L); // no tags
        when(cardRepository.findById(1000L)).thenReturn(Optional.of(current));

        List<CardResponse.RelatedCard> result = service.findArchiveRelated(1000L, 1L);

        assertThat(result).isEmpty();
        // findArchivedBySharedTagIdsAndUserId 미호출 검증
        org.mockito.Mockito.verify(cardRepository, never())
                .findArchivedBySharedTagIdsAndUserId(any(), any(), any());
    }

    @Test
    @DisplayName("미존재 cardId면 CARD_NOT_FOUND")
    void findArchiveRelated_notFound_throws() {
        when(cardRepository.findById(9_999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findArchiveRelated(9_999L, 1L))
                .isInstanceOf(CardDomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CARD_NOT_FOUND);
    }

    @Test
    @DisplayName("soft delete된 카드는 CARD_NOT_FOUND")
    void findArchiveRelated_softDeleted_throws() {
        Card current = cardWith(1000L);
        current.softDelete();
        when(cardRepository.findById(1000L)).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service.findArchiveRelated(1000L, 1L))
                .isInstanceOf(CardDomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CARD_NOT_FOUND);
    }
}
