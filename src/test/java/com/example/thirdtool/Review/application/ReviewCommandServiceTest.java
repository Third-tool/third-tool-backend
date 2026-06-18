package com.example.thirdtool.Review.application;

import com.example.thirdtool.Card.domain.model.ArchiveReason;
import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardStatus;
import com.example.thirdtool.Card.domain.model.CardStatusHistoryAppender;
import com.example.thirdtool.Card.domain.model.MainNote;
import com.example.thirdtool.Card.domain.model.OnFieldBudget;
import com.example.thirdtool.Card.domain.model.Summary;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.application.service.DeckQueryService;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.model.DeckProgressStatus;
import com.example.thirdtool.Review.domain.exception.ReviewSessionException;
import com.example.thirdtool.Review.domain.model.ReviewSession;
import com.example.thirdtool.Review.infrastructure.ReviewSessionRepository;
import com.example.thirdtool.Review.presentation.dto.ReviewRequest;
import com.example.thirdtool.Review.presentation.dto.ReviewResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.UserSchedule.application.service.UserScheduleQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;

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
 * ReviewCommandService 매트릭스 (Story-2-1·2-3 사용자별 budget 주입 + maxView 자동 archive).
 *
 * <p>전략 (`.claude/rules/conventions.md` §4.7): Repository·Cross-BC Service Mock + 도메인 객체(Card, Deck,
 * ReviewSession, UserEntity)는 실제 인스턴스로 생성해 상태 변화를 그대로 관찰한다.
 */
@DisplayName("ReviewCommandService — Story 2-1·2-3 (사용자별 budget + maxView archive)")
class ReviewCommandServiceTest {

    private ReviewSessionRepository sessionRepository;
    private ReviewQueryService queryService;
    private DeckQueryService deckQueryService;
    private CardRepository cardRepository;
    private CardStatusHistoryAppender historyAppender;
    private UserScheduleQueryService userScheduleQueryService;
    private ReviewCommandService service;

    private UserEntity user;
    private Deck deck;

    @BeforeEach
    void setUp() {
        sessionRepository        = mock(ReviewSessionRepository.class);
        queryService             = mock(ReviewQueryService.class);
        deckQueryService         = mock(DeckQueryService.class);
        cardRepository           = mock(CardRepository.class);
        historyAppender          = mock(CardStatusHistoryAppender.class);
        userScheduleQueryService = mock(UserScheduleQueryService.class);

        service = new ReviewCommandService(
                sessionRepository, queryService, deckQueryService,
                cardRepository, historyAppender, userScheduleQueryService
        );

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
        return card;
    }

    // ─── startReview ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("startReview()")
    class StartReview {

        @Test
        @DisplayName("정상 시작 — 첫 카드 viewCount+1, budget 사용자별로 조회, isLastView=false")
        void startReview_happy_incrementsView_noArchive() {
            Card card = persistedCard();
            when(deckQueryService.getActiveDeck(500L)).thenReturn(deck);
            when(cardRepository.findAllByDeckIdAndDeletedFalse(500L)).thenReturn(List.of(card));
            // budget maxView=3 — 첫 진입(viewCount=1)은 isLastView=false
            when(userScheduleQueryService.resolveOnFieldBudget(1L))
                    .thenReturn(OnFieldBudget.of(3, Duration.ofDays(10)));

            ReviewResponse.StartSession response =
                    service.startReview(new ReviewRequest.StartSession(500L), user);

            assertThat(card.getViewCount()).isEqualTo(1);
            assertThat(card.getStatus()).isEqualTo(CardStatus.ON_FIELD);
            assertThat(response.currentCard().isLastView()).isFalse();
            verify(historyAppender, never()).append(any(), any(), any(), any());
            verify(cardRepository, times(1)).save(card);
        }

        @Test
        @DisplayName("maxView=1 budget — 첫 노출이 곧 마지막 노출 → MAX_VIEW archive + history append + Deck recalc")
        void startReview_maxViewReached_archivesWithHistory() {
            Card card = persistedCard();
            ReflectionTestUtils.setField(deck, "progressStatus", DeckProgressStatus.IN_PROGRESS);

            when(deckQueryService.getActiveDeck(500L)).thenReturn(deck);
            when(cardRepository.findAllByDeckIdAndDeletedFalse(500L)).thenReturn(List.of(card));
            when(userScheduleQueryService.resolveOnFieldBudget(1L))
                    .thenReturn(OnFieldBudget.of(1, Duration.ofDays(10)));

            ReviewResponse.StartSession response =
                    service.startReview(new ReviewRequest.StartSession(500L), user);

            assertThat(card.getStatus()).isEqualTo(CardStatus.ARCHIVE);
            assertThat(response.currentCard().isLastView()).isTrue();
            verify(historyAppender, times(1))
                    .append(eq(card), eq(CardStatus.ON_FIELD), eq(CardStatus.ARCHIVE), eq(ArchiveReason.MAX_VIEW));
            // deck.cards 컬렉션이 빈 상태 → recalculate 호출되면 NOT_STARTED로 회귀 (호출 검증 대체)
            assertThat(deck.getProgressStatus()).isEqualTo(DeckProgressStatus.NOT_STARTED);
        }

        @Test
        @DisplayName("다른 유저의 deck 접근 시 REVIEW_SESSION_FORBIDDEN — budget 조회 미발생")
        void startReview_otherUserDeck_throws() {
            UserEntity otherOwner = UserEntity.ofLocal("other", "pw", "n", "o@e.com");
            ReflectionTestUtils.setField(otherOwner, "id", 99L);
            Deck otherDeck = Deck.createFromLearningMaterial(otherOwner, 10L, 200L, "DDD");
            ReflectionTestUtils.setField(otherDeck, "id", 500L);
            when(deckQueryService.getActiveDeck(500L)).thenReturn(otherDeck);

            assertThatThrownBy(() ->
                    service.startReview(new ReviewRequest.StartSession(500L), user))
                    .isInstanceOf(ReviewSessionException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REVIEW_SESSION_FORBIDDEN);

            verify(userScheduleQueryService, never()).resolveOnFieldBudget(any());
            verify(cardRepository, never()).save(any());
        }
    }

    // ─── moveToNext ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("moveToNext()")
    class MoveToNext {

        @Test
        @DisplayName("다음 카드 진입 시 viewCount+1 — budget 사용자별 주입, archive 미발생")
        void moveToNext_happy_incrementsView() {
            // first card는 startReview에서 진입 처리됨. 본 테스트는 moveToNext 단독 검증.
            Card card1 = persistedCard();
            Card card2 = Card.create(deck, MainNote.of("본문2", null), Summary.of("두번째."), List.of("k2"));
            ReflectionTestUtils.setField(card2, "id", 1001L);

            ReviewSession session = buildComparingSession(List.of(card1, card2));
            when(queryService.getSessionByOwner(eq(700L), eq(user))).thenReturn(session);
            when(userScheduleQueryService.resolveOnFieldBudget(1L))
                    .thenReturn(OnFieldBudget.of(3, Duration.ofDays(10)));

            ReviewResponse.NextCard response = service.moveToNext(700L, user);

            assertThat(card2.getViewCount()).isEqualTo(1);
            assertThat(response.currentCard().isLastView()).isFalse();
            verify(historyAppender, never()).append(any(), any(), any(), any());
            verify(cardRepository, times(1)).save(card2);
        }

        @Test
        @DisplayName("세션 종료 카드(마지막+1) — finished, budget 미조회·save 미호출")
        void moveToNext_lastCard_finishedSession_noBudgetCall() {
            Card only = persistedCard();
            ReviewSession session = buildComparingSession(List.of(only));
            // currentIndex=0이 COMPARING이라 moveToNext 호출 시 currentIndex=1 → finished=true
            when(queryService.getSessionByOwner(eq(700L), eq(user))).thenReturn(session);

            ReviewResponse.NextCard response = service.moveToNext(700L, user);

            assertThat(session.isFinished()).isTrue();
            assertThat(response.isFinished()).isTrue();
            assertThat(response.currentCard()).isNull();
            verify(userScheduleQueryService, never()).resolveOnFieldBudget(any());
            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("다음 카드가 maxView에 도달 — MAX_VIEW archive + history append")
        void moveToNext_maxViewReached_archives() {
            Card card1 = persistedCard();
            Card card2 = Card.create(deck, MainNote.of("본문2", null), Summary.of("두번째."), List.of("k2"));
            ReflectionTestUtils.setField(card2, "id", 1001L);

            ReviewSession session = buildComparingSession(List.of(card1, card2));
            when(queryService.getSessionByOwner(eq(700L), eq(user))).thenReturn(session);
            // budget maxView=1 — moveToNext에서 viewCount=1로 즉시 isLastView=true
            when(userScheduleQueryService.resolveOnFieldBudget(1L))
                    .thenReturn(OnFieldBudget.of(1, Duration.ofDays(10)));

            ReviewResponse.NextCard response = service.moveToNext(700L, user);

            assertThat(card2.getStatus()).isEqualTo(CardStatus.ARCHIVE);
            assertThat(response.currentCard().isLastView()).isTrue();
            verify(historyAppender, times(1))
                    .append(eq(card2), eq(CardStatus.ON_FIELD), eq(CardStatus.ARCHIVE), eq(ArchiveReason.MAX_VIEW));
        }
    }

    // ─── startComparing ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("startComparing()")
    class StartComparing {

        @Test
        @DisplayName("RECALLING → COMPARING 전환 + isLastView는 현재 카드 상태로 결정")
        void startComparing_transitionsStep_returnsIsLastView() {
            Card card = persistedCard();
            // viewCount를 2로 만들어 두기 (startReview에서 진입했다 가정)
            card.recordView();
            card.recordView();
            assertThat(card.getViewCount()).isEqualTo(2);

            ReviewSession session = buildNewSession(List.of(card));
            when(queryService.getSessionByOwner(eq(700L), eq(user))).thenReturn(session);
            // budget maxView=2 → 현재 viewCount=2이므로 isLastView=true
            when(userScheduleQueryService.resolveOnFieldBudget(1L))
                    .thenReturn(OnFieldBudget.of(2, Duration.ofDays(10)));

            ReviewResponse.CardReviewDto response = service.startComparing(700L, user);

            assertThat(session.currentCardReview().isComparing()).isTrue();
            assertThat(response.isLastView()).isTrue();
            // startComparing은 archive를 호출하지 않는다 (자동 archive는 recordView 경로만)
            verify(historyAppender, never()).append(any(), any(), any(), any());
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
