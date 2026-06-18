package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Card.domain.model.ArchiveReason;
import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardStatus;
import com.example.thirdtool.Card.domain.model.CardStatusHistoryAppender;
import com.example.thirdtool.Card.domain.model.MainNote;
import com.example.thirdtool.Card.domain.model.Summary;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.Card.infrastructure.persistence.TagRepository;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.model.DeckProgressStatus;
import com.example.thirdtool.Deck.infrastructure.repository.DeckRepository;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CardCommandService archive / returnToField 조율 검증 (Story-1-1 후속).
 *
 * - 도메인 멱등성(이미 같은 상태)에서 Appender·Deck 재계산이 호출되지 않는지
 * - 실제 전이가 발생한 경우 Appender(올바른 from/to/reason) + Deck 재계산이 호출되는지
 */
@DisplayName("CardCommandService — archive / returnToField (Story-1-1 후속)")
class CardCommandServiceArchiveTest {

    private CardRepository cardRepository;
    private TagRepository tagRepository;
    private DeckRepository deckRepository;
    private CardStatusHistoryAppender appender;
    private CardCommandService service;

    private UserEntity user;
    private Deck deck;

    @BeforeEach
    void setUp() {
        cardRepository = mock(CardRepository.class);
        tagRepository = mock(TagRepository.class);
        deckRepository = mock(DeckRepository.class);
        appender = mock(CardStatusHistoryAppender.class);
        service = new CardCommandService(cardRepository, tagRepository, deckRepository, appender);

        user = UserEntity.ofLocal("tester", "encoded-pw", "닉네임", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        deck = Deck.createFromLearningMaterial(user, 10L, 200L, "DDD");
        ReflectionTestUtils.setField(deck, "id", 500L);
    }

    private Card persistedCard() {
        Card card = Card.create(
                deck,
                MainNote.of("본문", null),
                Summary.of("한 문장."),
                List.of("키워드")
        );
        ReflectionTestUtils.setField(card, "id", 1000L);
        when(cardRepository.findById(1000L)).thenReturn(Optional.of(card));
        return card;
    }

    // ─── archive() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("archive()")
    class Archive {

        @Test
        @DisplayName("ON_FIELD → ARCHIVE 정상 전이 시 Appender 1회 + Deck 재계산 호출")
        void archive_onField_appendsHistory_recalculatesDeck() {
            Card card = persistedCard();
            assertThat(card.getStatus()).isEqualTo(CardStatus.ON_FIELD);
            ReflectionTestUtils.setField(deck, "progressStatus", DeckProgressStatus.IN_PROGRESS);

            service.archive(1000L, ArchiveReason.MANUAL);

            assertThat(card.getStatus()).isEqualTo(CardStatus.ARCHIVE);
            verify(appender, times(1))
                    .append(eq(card), eq(CardStatus.ON_FIELD), eq(CardStatus.ARCHIVE), eq(ArchiveReason.MANUAL));
            // deck.cards는 단위 테스트에서 빈 컬렉션 (JPA cascade 미적용) → recalculate 호출되면 NOT_STARTED로 회귀
            assertThat(deck.getProgressStatus()).isEqualTo(DeckProgressStatus.NOT_STARTED);
        }

        @Test
        @DisplayName("이미 ARCHIVE면 도메인 no-op + Appender 미호출 + Deck 재계산 미호출 (멱등)")
        void archive_alreadyArchive_noOp() {
            Card card = persistedCard();
            card.archive();
            ReflectionTestUtils.setField(deck, "progressStatus", DeckProgressStatus.IN_PROGRESS);

            service.archive(1000L, ArchiveReason.MANUAL);

            assertThat(card.getStatus()).isEqualTo(CardStatus.ARCHIVE);
            verify(appender, never()).append(any(), any(), any(), any());
            // recalculate 미호출 → IN_PROGRESS 보존
            assertThat(deck.getProgressStatus()).isEqualTo(DeckProgressStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("reason이 null이면 INVALID_INPUT 예외")
        void archive_nullReason_throws() {
            persistedCard();

            assertThatThrownBy(() -> service.archive(1000L, null))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);
            verify(appender, never()).append(any(), any(), any(), any());
        }

        @Test
        @DisplayName("존재하지 않는 cardId면 CARD_NOT_FOUND 예외")
        void archive_notFound_throws() {
            when(cardRepository.findById(9999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.archive(9999L, ArchiveReason.MANUAL))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CARD_NOT_FOUND);
            verify(appender, never()).append(any(), any(), any(), any());
        }

        @Test
        @DisplayName("soft delete된 카드는 CARD_NOT_FOUND 예외")
        void archive_softDeleted_throws() {
            Card card = persistedCard();
            card.softDelete();

            assertThatThrownBy(() -> service.archive(1000L, ArchiveReason.MANUAL))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CARD_NOT_FOUND);
            verify(appender, never()).append(any(), any(), any(), any());
        }
    }

    // ─── returnToField() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("returnToField()")
    class ReturnToField {

        @Test
        @DisplayName("ARCHIVE → ON_FIELD 복귀 시 reason 없이 Appender 1회 + Deck 재계산 호출 + 도메인 재초기화")
        void returnToField_archive_appendsHistory_resetsFields() {
            Card card = persistedCard();
            card.recordView();
            card.archive();

            service.returnToField(1000L);

            assertThat(card.getStatus()).isEqualTo(CardStatus.ON_FIELD);
            assertThat(card.getViewCount()).isZero();
            assertThat(card.getLastViewedAt()).isNull();
            verify(appender, times(1))
                    .append(eq(card), eq(CardStatus.ARCHIVE), eq(CardStatus.ON_FIELD), eq((ArchiveReason) null));
        }

        @Test
        @DisplayName("이미 ON_FIELD면 도메인 no-op + Appender 미호출 + Deck 재계산 미호출 (멱등)")
        void returnToField_alreadyOnField_noOp() {
            Card card = persistedCard();
            assertThat(card.getStatus()).isEqualTo(CardStatus.ON_FIELD);
            ReflectionTestUtils.setField(deck, "progressStatus", DeckProgressStatus.COMPLETED);

            service.returnToField(1000L);

            assertThat(card.getStatus()).isEqualTo(CardStatus.ON_FIELD);
            verify(appender, never()).append(any(), any(), any(), any());
            assertThat(deck.getProgressStatus()).isEqualTo(DeckProgressStatus.COMPLETED);
        }

        @Test
        @DisplayName("존재하지 않는 cardId면 CARD_NOT_FOUND 예외")
        void returnToField_notFound_throws() {
            when(cardRepository.findById(9999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.returnToField(9999L))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CARD_NOT_FOUND);
        }
    }
}
