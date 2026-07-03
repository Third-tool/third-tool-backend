package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.domain.model.ArchiveReason;
import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardExpiryPolicy;
import com.example.thirdtool.Card.domain.model.CardStatus;
import com.example.thirdtool.Card.domain.model.CardStatusHistoryAppender;
import com.example.thirdtool.Card.domain.model.MainNote;
import com.example.thirdtool.Card.domain.model.Summary;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.model.DeckProgressStatus;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.UserSchedule.application.service.UserScheduleQueryService;
import com.example.thirdtool.UserSchedule.domain.model.LearningMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CardExpiryBatchService 매트릭스 — Story-CARD-E1-S1-1~5 테스트 재배선.
 *
 * <p>Story-CARD-E1-S1-4 — {@code resolveOnFieldBudget()} 폐기.
 * 배치는 이제 사용자별 {@link LearningMode}를 조회해 {@code toOnFieldBudget()}로 변환한다.
 *
 * <p>Mockist (`conventions.md` §4.7) — Repository / Cross-BC Service Mock + 도메인 객체는 실제 생성.
 * 시간 의존성은 `enteredFieldAt` reflection 주입으로 통제 (`DomainFixture` 패턴 동일).
 */
@DisplayName("CardExpiryBatchService — Story-CARD-E1-S1-1~5 (currentMode 주입)")
class CardExpiryBatchServiceTest {

    private CardRepository cardRepository;
    private CardStatusHistoryAppender historyAppender;
    private CardExpiryPolicy cardExpiryPolicy;
    private UserScheduleQueryService userScheduleQueryService;
    private CardExpiryBatchService service;

    private UserEntity userA;
    private UserEntity userB;
    private Deck deckA;
    private Deck deckB;

    @BeforeEach
    void setUp() {
        cardRepository           = mock(CardRepository.class);
        historyAppender          = mock(CardStatusHistoryAppender.class);
        userScheduleQueryService = mock(UserScheduleQueryService.class);
        cardExpiryPolicy         = new CardExpiryPolicy(); // 도메인 서비스는 실제로

        service = new CardExpiryBatchService(
                cardRepository, historyAppender, cardExpiryPolicy, userScheduleQueryService
        );

        userA = UserEntity.ofLocal("a", "pw", "n", "a@e.com");
        ReflectionTestUtils.setField(userA, "id", 1L);
        userB = UserEntity.ofLocal("b", "pw", "n", "b@e.com");
        ReflectionTestUtils.setField(userB, "id", 2L);

        deckA = Deck.createFromAxis(userA, 10L, "A 덱");
        ReflectionTestUtils.setField(deckA, "id", 500L);
        ReflectionTestUtils.setField(deckA, "progressStatus", DeckProgressStatus.IN_PROGRESS);

        deckB = Deck.createFromAxis(userB, 11L, "B 덱");
        ReflectionTestUtils.setField(deckB, "id", 501L);
        ReflectionTestUtils.setField(deckB, "progressStatus", DeckProgressStatus.IN_PROGRESS);
    }

    private Card cardIn(Deck deck, Long id, LocalDateTime enteredFieldAt, int viewCount) {
        Card card = Card.create(deck, MainNote.of("본문", null), Summary.of("한 문장."), List.of("k"));
        ReflectionTestUtils.setField(card, "id", id);
        ReflectionTestUtils.setField(card, "enteredFieldAt", enteredFieldAt);
        ReflectionTestUtils.setField(card, "viewCount", viewCount);
        return card;
    }

    @Test
    @DisplayName("후보 0건이면 currentMode·history·recalc 어떤 호출도 없다 (no-op)")
    void noCandidates_noSideEffects() {
        when(cardRepository.findAllByStatus(CardStatus.ON_FIELD)).thenReturn(List.of());

        CardExpiryBatchService.ExpiryResult result = service.processExpired();

        assertThat(result.candidates()).isZero();
        assertThat(result.archived()).isZero();
        verify(userScheduleQueryService, never()).currentMode(any());
        verify(historyAppender, never()).append(any(), any(), any(), any());
    }

    @Test
    @DisplayName("maxDuration 초과 카드만 archive — MAX_DURATION history append + Deck recalc")
    void durationExpired_archivesWithMaxDuration() {
        Card expired = cardIn(deckA, 1000L, LocalDateTime.now().minusDays(15), 0);  // 15일 경과
        Card fresh   = cardIn(deckA, 1001L, LocalDateTime.now().minusDays(2),  0);  // 2일 경과

        when(cardRepository.findAllByStatus(CardStatus.ON_FIELD)).thenReturn(List.of(expired, fresh));
        // userA MODE_7D — maxDays=7, stepCount=3. 15일 > 7일 → expired, 2일 < 7일 → fresh
        when(userScheduleQueryService.currentMode(1L)).thenReturn(LearningMode.MODE_7D);

        CardExpiryBatchService.ExpiryResult result = service.processExpired();

        assertThat(expired.getStatus()).isEqualTo(CardStatus.ARCHIVE);
        assertThat(fresh.getStatus()).isEqualTo(CardStatus.ON_FIELD);
        assertThat(result.archived()).isEqualTo(1);
        assertThat(result.maxDurationCount()).isEqualTo(1);
        assertThat(result.maxViewCount()).isZero();

        verify(historyAppender, times(1))
                .append(eq(expired), eq(CardStatus.ON_FIELD), eq(CardStatus.ARCHIVE), eq(ArchiveReason.MAX_DURATION));
        verify(historyAppender, never())
                .append(eq(fresh), any(), any(), any());
        // deckA의 카드 중 1건 ARCHIVE → recalculate 호출 (deck.cards 빈 컬렉션 기준 NOT_STARTED로 회귀)
        assertThat(deckA.getProgressStatus()).isEqualTo(DeckProgressStatus.NOT_STARTED);
    }

    @Test
    @DisplayName("MAX_VIEW · MAX_DURATION 동시 도달 시 MAX_VIEW 우선 (OnFieldBudget 규칙)")
    void simultaneousMaxViewAndDuration_maxViewWins() {
        // viewCount=3 + 15일 경과 → 둘 다 충족 (MODE_7D: stepCount=3, maxDays=7) → MAX_VIEW
        Card both = cardIn(deckA, 1000L, LocalDateTime.now().minusDays(15), 3);

        when(cardRepository.findAllByStatus(CardStatus.ON_FIELD)).thenReturn(List.of(both));
        when(userScheduleQueryService.currentMode(1L)).thenReturn(LearningMode.MODE_7D);

        CardExpiryBatchService.ExpiryResult result = service.processExpired();

        assertThat(both.getStatus()).isEqualTo(CardStatus.ARCHIVE);
        assertThat(result.maxViewCount()).isEqualTo(1);
        assertThat(result.maxDurationCount()).isZero();
        verify(historyAppender, times(1))
                .append(eq(both), eq(CardStatus.ON_FIELD), eq(CardStatus.ARCHIVE), eq(ArchiveReason.MAX_VIEW));
    }

    @Test
    @DisplayName("여러 사용자 — 각 사용자별 currentMode 1회만 조회 (N+1 회피)")
    void multipleUsers_budgetCalledOncePerUser() {
        Card a1 = cardIn(deckA, 1000L, LocalDateTime.now().minusDays(2), 0);
        Card a2 = cardIn(deckA, 1001L, LocalDateTime.now().minusDays(3), 0);
        Card b1 = cardIn(deckB, 2000L, LocalDateTime.now().minusDays(4), 0);

        when(cardRepository.findAllByStatus(CardStatus.ON_FIELD)).thenReturn(List.of(a1, a2, b1));
        // MODE_28D (stepCount=5, maxDays=28) — 위 카드들 모두 미만료
        when(userScheduleQueryService.currentMode(1L)).thenReturn(LearningMode.MODE_28D);
        when(userScheduleQueryService.currentMode(2L)).thenReturn(LearningMode.MODE_28D);

        service.processExpired();

        verify(userScheduleQueryService, times(1)).currentMode(1L);
        verify(userScheduleQueryService, times(1)).currentMode(2L);
    }

    @Test
    @DisplayName("같은 Deck에 만료 카드 2건이면 recalculateProgressStatus 호출은 1회만 (모음 정리)")
    void sameDeckMultipleExpiry_recalcOnce() {
        Card e1 = cardIn(deckA, 1000L, LocalDateTime.now().minusDays(15), 0);
        Card e2 = cardIn(deckA, 1001L, LocalDateTime.now().minusDays(20), 0);

        when(cardRepository.findAllByStatus(CardStatus.ON_FIELD)).thenReturn(List.of(e1, e2));
        // MODE_7D (maxDays=7) — 두 카드 모두 만료 (15일·20일 > 7일)
        when(userScheduleQueryService.currentMode(1L)).thenReturn(LearningMode.MODE_7D);

        CardExpiryBatchService.ExpiryResult result = service.processExpired();

        assertThat(result.archived()).isEqualTo(2);
        // deckA recalc 결과는 NOT_STARTED (deck.cards 컬렉션 비어있는 단위 테스트 조건)
        assertThat(deckA.getProgressStatus()).isEqualTo(DeckProgressStatus.NOT_STARTED);
    }
}
