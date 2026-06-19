package com.example.thirdtool.Review.application;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.MainNote;
import com.example.thirdtool.Card.domain.model.SoftScheduleState;
import com.example.thirdtool.Card.domain.model.SoftScheduleTemplate;
import com.example.thirdtool.Card.domain.model.Summary;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Review.domain.model.StateRecommendationDistributor;
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
        service = new ReviewQueryService(
                sessionRepository, userScheduleQueryService, cardRepository,
                new StateRecommendationDistributor()
        );

        user = UserEntity.ofLocal("u", "pw", "n", "u@e.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        deck = Deck.createFromLearningMaterial(user, 10L, 200L, "DDD");
        ReflectionTestUtils.setField(deck, "id", 500L);

        // 기본 dailyTarget stub — 각 테스트가 override 가능
        when(userScheduleQueryService.resolveDailyTarget(eq(1L))).thenReturn(20);
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

    @Test
    @DisplayName("Story 6-2 — dailyTarget=3 + 풀 FRESH:1·1D:1·7D:1 → 각 state 1장씩 권장")
    void getTodayCandidates_recommendation_evenSmallPool() {
        when(userScheduleQueryService.resolveSoftScheduleTemplate(eq(1L)))
                .thenReturn(SoftScheduleTemplate.DEFAULT);
        when(userScheduleQueryService.resolveDailyTarget(eq(1L))).thenReturn(3);

        Card fresh  = cardWith(1000L, null);
        Card oneD   = cardWith(1001L, LocalDateTime.now().minusDays(2));
        Card sevenD = cardWith(1002L, LocalDateTime.now().minusDays(10));
        when(cardRepository.findOnFieldEligibleByUserId(eq(1L), any()))
                .thenReturn(List.of(fresh, oneD, sevenD));

        ReviewResponse.TodayCandidates result = service.getTodayCandidates(user);

        assertThat(result.total()).isEqualTo(3);
        assertThat(result.dailyTarget()).isEqualTo(3);
        assertThat(result.recommendedTotal()).isEqualTo(3);
        assertThat(result.recommendedByState()).containsOnly(
                org.assertj.core.api.Assertions.entry(SoftScheduleState.FRESH, 1),
                org.assertj.core.api.Assertions.entry(SoftScheduleState.INTERVAL_1D, 1),
                org.assertj.core.api.Assertions.entry(SoftScheduleState.INTERVAL_7D, 1)
        );
    }

    @Test
    @DisplayName("Story 6-2 — dailyTarget > total 이면 풀 전체 권장")
    void getTodayCandidates_recommendation_dailyTargetExceedsPool() {
        when(userScheduleQueryService.resolveSoftScheduleTemplate(eq(1L)))
                .thenReturn(SoftScheduleTemplate.DEFAULT);
        when(userScheduleQueryService.resolveDailyTarget(eq(1L))).thenReturn(50);

        Card fresh = cardWith(1000L, null);
        Card oneD  = cardWith(1001L, LocalDateTime.now().minusDays(2));
        when(cardRepository.findOnFieldEligibleByUserId(eq(1L), any()))
                .thenReturn(List.of(fresh, oneD));

        ReviewResponse.TodayCandidates result = service.getTodayCandidates(user);

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.recommendedTotal()).isEqualTo(2);
    }

    @Test
    @DisplayName("Story 6-3 — target override (+10장) — 분배가 override 값을 따른다")
    void getTodayCandidatesWithTarget_overridesDailyTarget() {
        when(userScheduleQueryService.resolveSoftScheduleTemplate(eq(1L)))
                .thenReturn(SoftScheduleTemplate.DEFAULT);
        // dailyTarget=20이 stub되어 있지만 명시 target은 그를 무시한다
        when(userScheduleQueryService.resolveDailyTarget(eq(1L))).thenReturn(20);

        // 풀 5장 — target=30이면 풀 cap으로 5장만 분배되고, target=3이면 3장만 분배되어야 함
        Card c1 = cardWith(1001L, null);
        Card c2 = cardWith(1002L, null);
        Card c3 = cardWith(1003L, LocalDateTime.now().minusDays(2));
        Card c4 = cardWith(1004L, LocalDateTime.now().minusDays(2));
        Card c5 = cardWith(1005L, LocalDateTime.now().minusDays(10));
        when(cardRepository.findOnFieldEligibleByUserId(eq(1L), any()))
                .thenReturn(List.of(c1, c2, c3, c4, c5));

        ReviewResponse.TodayCandidates result = service.getTodayCandidatesWithTarget(user, 3);

        assertThat(result.total()).isEqualTo(5);
        assertThat(result.dailyTarget()).isEqualTo(20);   // 사용자 설정 그대로 노출
        assertThat(result.recommendedTotal()).isEqualTo(3); // target=3 적용
    }

    @Test
    @DisplayName("Story 6-3 — target이 풀보다 크면 풀 전체만 분배 + recommendedTotal < target")
    void getTodayCandidatesWithTarget_poolSmallerThanTarget() {
        when(userScheduleQueryService.resolveSoftScheduleTemplate(eq(1L)))
                .thenReturn(SoftScheduleTemplate.DEFAULT);
        when(userScheduleQueryService.resolveDailyTarget(eq(1L))).thenReturn(20);

        Card c1 = cardWith(1001L, null);
        Card c2 = cardWith(1002L, null);
        when(cardRepository.findOnFieldEligibleByUserId(eq(1L), any()))
                .thenReturn(List.of(c1, c2));

        ReviewResponse.TodayCandidates result = service.getTodayCandidatesWithTarget(user, 30);

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.recommendedTotal()).isEqualTo(2);
    }

    @Test
    @DisplayName("Story 6-2 — 후보 0건이면 dailyTarget 노출 + recommended 빈 분배")
    void getTodayCandidates_noCandidates_exposesDailyTarget() {
        when(userScheduleQueryService.resolveSoftScheduleTemplate(eq(1L)))
                .thenReturn(SoftScheduleTemplate.DEFAULT);
        when(userScheduleQueryService.resolveDailyTarget(eq(1L))).thenReturn(15);
        when(cardRepository.findOnFieldEligibleByUserId(eq(1L), any()))
                .thenReturn(List.of());

        ReviewResponse.TodayCandidates result = service.getTodayCandidates(user);

        assertThat(result.total()).isZero();
        assertThat(result.dailyTarget()).isEqualTo(15);
        assertThat(result.recommendedTotal()).isZero();
        assertThat(result.recommendedByState()).isEmpty();
    }
}
