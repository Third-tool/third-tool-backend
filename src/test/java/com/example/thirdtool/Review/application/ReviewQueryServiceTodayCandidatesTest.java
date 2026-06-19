package com.example.thirdtool.Review.application;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.MainNote;
import com.example.thirdtool.Card.domain.model.SoftScheduleState;
import com.example.thirdtool.Card.domain.model.SoftScheduleTemplate;
import com.example.thirdtool.Card.domain.model.Summary;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Review.infrastructure.ReviewSessionRepository;
import com.example.thirdtool.Review.presentation.dto.ReviewResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.UserSchedule.application.service.UserScheduleQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Story 6-1 — getTodayCandidates 매트릭스.
 *
 * <p>Mockist (`conventions.md` §4.7) — Repository / Cross-BC Service Mock + Card·Deck·User는 실제 생성.
 * 도메인 SoftScheduleTemplate.DEFAULT(1·3·7일)를 사용해 state 분류 검증.
 */
@DisplayName("ReviewQueryService — getTodayCandidates (Story 6-1)")
class ReviewQueryServiceTodayCandidatesTest {

    private ReviewSessionRepository sessionRepository;
    private UserScheduleQueryService userScheduleQueryService;
    private CardRepository cardRepository;
    private ReviewQueryService service;

    private UserEntity user;
    private Deck deck;

    @BeforeEach
    void setUp() {
        sessionRepository        = mock(ReviewSessionRepository.class);
        userScheduleQueryService = mock(UserScheduleQueryService.class);
        cardRepository           = mock(CardRepository.class);
        service = new ReviewQueryService(sessionRepository, userScheduleQueryService, cardRepository);

        user = UserEntity.ofLocal("u", "pw", "n", "u@e.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        deck = Deck.createFromLearningMaterial(user, 10L, 200L, "DDD");
        ReflectionTestUtils.setField(deck, "id", 500L);
    }

    private Card cardWith(Long id, LocalDateTime lastViewedAt) {
        Card card = Card.create(deck, MainNote.of("본문", null), Summary.of("한 문장."), List.of("k"));
        ReflectionTestUtils.setField(card, "id", id);
        if (lastViewedAt != null) ReflectionTestUtils.setField(card, "lastViewedAt", lastViewedAt);
        return card;
    }

    @Test
    @DisplayName("FRESH(null) / INTERVAL_1D / INTERVAL_7D 각각 분류되고 NOT_YET은 응답에서 제외된다")
    void getTodayCandidates_groupsByState_excludesNotYet() {
        when(userScheduleQueryService.resolveSoftScheduleTemplate(eq(1L)))
                .thenReturn(SoftScheduleTemplate.DEFAULT);

        Card fresh   = cardWith(1000L, null);                                            // FRESH
        Card oneDay  = cardWith(1001L, LocalDateTime.now().minusDays(2));                // INTERVAL_1D
        Card sevenD  = cardWith(1002L, LocalDateTime.now().minusDays(10));               // INTERVAL_7D
        // notYet 카드는 후보 풀에서 이미 제외되어야 하지만 NOT_YET 안전망도 검증
        Card notYet  = cardWith(1003L, LocalDateTime.now().minusHours(2));               // NOT_YET (1일 미만)

        when(cardRepository.findOnFieldEligibleByUserId(eq(1L), any()))
                .thenReturn(List.of(fresh, oneDay, sevenD, notYet));

        ReviewResponse.TodayCandidates result = service.getTodayCandidates(user);

        assertThat(result.total()).isEqualTo(3);
        assertThat(result.byState()).doesNotContainKey(SoftScheduleState.NOT_YET);
        assertThat(result.byState().get(SoftScheduleState.FRESH))
                .extracting(ReviewResponse.TodayCandidates.CandidateItem::cardId).containsExactly(1000L);
        assertThat(result.byState().get(SoftScheduleState.INTERVAL_1D))
                .extracting(ReviewResponse.TodayCandidates.CandidateItem::cardId).containsExactly(1001L);
        assertThat(result.byState().get(SoftScheduleState.INTERVAL_7D))
                .extracting(ReviewResponse.TodayCandidates.CandidateItem::cardId).containsExactly(1002L);
    }

    @Test
    @DisplayName("후보 0건이면 total=0 + 빈 byState")
    void getTodayCandidates_noCandidates_emptyResponse() {
        when(userScheduleQueryService.resolveSoftScheduleTemplate(eq(1L)))
                .thenReturn(SoftScheduleTemplate.DEFAULT);
        when(cardRepository.findOnFieldEligibleByUserId(eq(1L), any()))
                .thenReturn(List.of());

        ReviewResponse.TodayCandidates result = service.getTodayCandidates(user);

        assertThat(result.total()).isZero();
        assertThat(result.byState()).isEmpty();
    }

    @Test
    @DisplayName("CandidateItem 매핑 — cardId / deckId / deckName / summary")
    void getTodayCandidates_itemMapping() {
        when(userScheduleQueryService.resolveSoftScheduleTemplate(eq(1L)))
                .thenReturn(SoftScheduleTemplate.DEFAULT);

        Card card = cardWith(1000L, null);
        when(cardRepository.findOnFieldEligibleByUserId(eq(1L), any()))
                .thenReturn(List.of(card));

        ReviewResponse.TodayCandidates result = service.getTodayCandidates(user);

        ReviewResponse.TodayCandidates.CandidateItem item =
                result.byState().get(SoftScheduleState.FRESH).get(0);
        assertThat(item.cardId()).isEqualTo(1000L);
        assertThat(item.deckId()).isEqualTo(500L);
        assertThat(item.deckName()).isEqualTo("DDD");
        assertThat(item.summary()).isEqualTo("한 문장.");
    }
}
