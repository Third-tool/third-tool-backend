package com.example.thirdtool.Review.application;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardStatus;
import com.example.thirdtool.Card.domain.model.MainNote;
import com.example.thirdtool.Card.domain.model.Summary;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.application.service.DeckQueryService;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Review.domain.exception.ReviewSessionException;
import com.example.thirdtool.Review.domain.model.ReviewSession;
import com.example.thirdtool.Review.infrastructure.ReviewSessionRepository;
import com.example.thirdtool.Review.presentation.dto.ReviewRequest;
import com.example.thirdtool.Review.presentation.dto.ReviewResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ReviewCommandService 매트릭스 — Story-CARD-E2-S2-4 재작성.
 *
 * <p>OnFieldBudget 폐기(PR#2)로 이중 게이트(maxView + maxDuration) archive 로직 제거.
 * 리뷰 세션은 viewCount만 기록하고, ARCHIVE 결정은 M5 DailyLearningBatch가 lazy 판정.
 * {@code isLastView}는 API 호환을 위해 응답 필드로 남되 항상 {@code false}.
 *
 * <p>Mock 대상: {@code ReviewSessionRepository}, {@code ReviewQueryService},
 * {@code DeckQueryService}, {@code CardRepository}. {@code CardStatusHistoryAppender} /
 * {@code UserScheduleQueryService} 의존성 제거.
 */
@DisplayName("ReviewCommandService — Story-CARD-E2-S2-4 (OnFieldBudget 폐기 후 viewCount만 기록)")
class ReviewCommandServiceTest {

    private ReviewSessionRepository sessionRepository;
    private ReviewQueryService queryService;
    private DeckQueryService deckQueryService;
    private CardRepository cardRepository;
    private ReviewCommandService service;

    private UserEntity user;
    private Deck deck;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(ReviewSessionRepository.class);
        queryService      = mock(ReviewQueryService.class);
        deckQueryService  = mock(DeckQueryService.class);
        cardRepository    = mock(CardRepository.class);

        service = new ReviewCommandService(
                sessionRepository, queryService, deckQueryService, cardRepository
        );

        user = UserEntity.ofLocal("tester", "encoded-pw", "닉네임", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        deck = Deck.createFromAxis(user, 10L, "DDD");
        ReflectionTestUtils.setField(deck, "id", 500L);
    }

    private Card persistedCard(Long id, String keyword) {
        Card card = Card.create(
                deck,
                MainNote.of("본문", null),
                Summary.of("한 문장."),
                List.of(keyword)
        );
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }

    // ─── startReview ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("startReview()")
    class StartReview {

        @Test
        @DisplayName("정상 시작 — 첫 카드 viewCount+1, isLastView=false, save 1회")
        void startReview_happy_incrementsView() {
            Card card = persistedCard(1000L, "k1");
            when(deckQueryService.getActiveDeck(500L)).thenReturn(deck);
            when(cardRepository.findAllByDeckIdAndDeletedFalse(500L)).thenReturn(List.of(card));

            ReviewResponse.StartSession response =
                    service.startReview(new ReviewRequest.StartSession(500L), user);

            assertThat(card.getViewCount()).isEqualTo(1);
            assertThat(card.getStatus()).isEqualTo(CardStatus.ON_FIELD);
            assertThat(response.currentCard().isLastView()).isFalse();
            verify(cardRepository, times(1)).save(card);
        }

        @Test
        @DisplayName("다른 유저의 deck 접근 시 REVIEW_SESSION_FORBIDDEN — save 미호출")
        void startReview_otherUserDeck_throws() {
            UserEntity otherOwner = UserEntity.ofLocal("other", "pw", "n", "o@e.com");
            ReflectionTestUtils.setField(otherOwner, "id", 99L);
            Deck otherDeck = Deck.createFromAxis(otherOwner, 10L, "DDD");
            ReflectionTestUtils.setField(otherDeck, "id", 500L);
            when(deckQueryService.getActiveDeck(500L)).thenReturn(otherDeck);

            assertThatThrownBy(() ->
                    service.startReview(new ReviewRequest.StartSession(500L), user))
                    .isInstanceOf(ReviewSessionException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REVIEW_SESSION_FORBIDDEN);

            verify(cardRepository, never()).save(any());
        }
    }

    // ─── moveToNext ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("moveToNext()")
    class MoveToNext {

        @Test
        @DisplayName("다음 카드 진입 시 viewCount+1, isLastView=false, save 1회 — archive 미발생")
        void moveToNext_happy_incrementsView() {
            Card card1 = persistedCard(1000L, "k1");
            Card card2 = persistedCard(1001L, "k2");

            ReviewSession session = buildComparingSession(List.of(card1, card2));
            when(queryService.getSessionByOwner(eq(700L), eq(user))).thenReturn(session);

            ReviewResponse.NextCard response = service.moveToNext(700L, user);

            assertThat(card2.getViewCount()).isEqualTo(1);
            assertThat(response.currentCard().isLastView()).isFalse();
            verify(cardRepository, times(1)).save(card2);
        }

        @Test
        @DisplayName("세션 종료 카드(마지막+1) — finished=true, currentCard=null, save 미호출")
        void moveToNext_lastCard_finishedSession() {
            Card only = persistedCard(1000L, "k1");
            ReviewSession session = buildComparingSession(List.of(only));
            when(queryService.getSessionByOwner(eq(700L), eq(user))).thenReturn(session);

            ReviewResponse.NextCard response = service.moveToNext(700L, user);

            assertThat(session.isFinished()).isTrue();
            assertThat(response.isFinished()).isTrue();
            assertThat(response.currentCard()).isNull();
            verify(cardRepository, never()).save(any());
        }
    }

    // ─── startComparing ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("startComparing()")
    class StartComparing {

        @Test
        @DisplayName("RECALLING → COMPARING 전환, isLastView는 항상 false")
        void startComparing_transitionsStep() {
            Card card = persistedCard(1000L, "k1");

            ReviewSession session = buildNewSession(List.of(card));
            when(queryService.getSessionByOwner(eq(700L), eq(user))).thenReturn(session);

            ReviewResponse.CardReviewDto response = service.startComparing(700L, user);

            assertThat(session.currentCardReview().isComparing()).isTrue();
            assertThat(response.isLastView()).isFalse();
        }
    }

    // ─── helper ────────────────────────────────────────────────────────────────

    private ReviewSession buildNewSession(List<Card> cards) {
        ReviewSession session = ReviewSession.of(deck, cards, user, cards.size());
        ReflectionTestUtils.setField(session, "id", 700L);
        return session;
    }

    /** 첫 카드가 COMPARING 상태인 세션 (moveToNext 호출 가능 상태) */
    private ReviewSession buildComparingSession(List<Card> cards) {
        ReviewSession session = buildNewSession(cards);
        session.startComparingCurrentCard();
        return session;
    }
}
