package com.example.thirdtool.Review.integration;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.MainNote;
import com.example.thirdtool.Card.domain.model.SoftScheduleState;
import com.example.thirdtool.Card.domain.model.Summary;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.Review.application.ReviewQueryService;
import com.example.thirdtool.Review.presentation.dto.ReviewResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.UserSchedule.application.service.UserScheduleCommandService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * product-card.md Epic 6 — 오늘 학습 후보 수집 전체 흐름 통합 검증.
 *
 * <p>단위 / Mockist 테스트가 부분 검증을 담당하지만, 다음 cross-BC 협력이 Spring 컨텍스트 전체에서
 * 정합한지를 영속 흐름으로 확인한다.
 * <ul>
 *   <li>{@link com.example.thirdtool.LearningFacade.application.service.LearningFacadeQueryService}
 *       — Layer 1 axes ID 추출</li>
 *   <li>{@link UserScheduleCommandService} / Query — dailyTarget + SoftScheduleTemplate 파생</li>
 *   <li>{@link com.example.thirdtool.Card.infrastructure.persistence.CardRepository}
 *       — axisIds 적용 vs fallback 분기</li>
 *   <li>{@link com.example.thirdtool.Review.domain.model.StateRecommendationDistributor}
 *       — 풀 비례 분배</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ReviewQueryService.getTodayCandidates 통합 — Epic 6")
class ReviewTodayCandidatesIntegrationTest {

    @Autowired ReviewQueryService reviewQueryService;
    @Autowired UserScheduleCommandService userScheduleCommandService;

    @PersistenceContext EntityManager em;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = UserEntity.ofLocal("owner", "encoded-pw", "닉네임", "owner@example.com");
        em.persist(user);
        em.flush();
    }

    private Card persistCardInDeck(Deck deck, LocalDateTime lastViewedAt) {
        Card card = Card.create(
                deck,
                MainNote.of("본문", null),
                Summary.of("한 문장."),
                List.of("키워드")
        );
        if (lastViewedAt != null) {
            ReflectionTestUtils.setField(card, "lastViewedAt", lastViewedAt);
        }
        em.persist(card);
        return card;
    }

    private Deck persistDeck(String name, Long axisId) {
        Deck deck = Deck.createFromAxis(user, axisId, name);
        em.persist(deck);
        return deck;
    }

    @Test
    @DisplayName("Layer 1 한정 — Facade axes에 속한 Deck 카드만 후보로 수집되고, 외부 axis 카드는 제외")
    void layer1Filter_excludesCardsOutsideFacadeAxes() {
        // given — Facade 1개 + axis 2개 ("inFacade-A", "inFacade-B")
        LearningFacade facade = LearningFacade.create(user, "백엔드");
        LearningAxis axisA = facade.addAxis("Spring");
        LearningAxis axisB = facade.addAxis("DB");
        em.persist(facade);
        em.flush();

        // Layer 1 안의 Deck 2개
        Deck deckInA = persistDeck("Spring 덱", axisA.getId());
        Deck deckInB = persistDeck("DB 덱", axisB.getId());
        // Layer 1 밖의 Deck — facade 외 axisId (실제로는 다른 facade의 axis이거나 미연결)
        Deck deckOutside = persistDeck("외부 덱", 99_999L);
        em.flush();

        Card inA   = persistCardInDeck(deckInA, null);          // FRESH
        Card inB   = persistCardInDeck(deckInB, null);          // FRESH
        Card outsider = persistCardInDeck(deckOutside, null);   // Layer 1 밖 — 제외 대상
        em.flush();
        em.clear();

        // when
        ReviewResponse.TodayCandidates result = reviewQueryService.getTodayCandidates(user);

        // then — 후보 2건만 (외부 1건 제외)
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.byState().get(SoftScheduleState.FRESH))
                .extracting(ReviewResponse.TodayCandidates.CandidateItem::cardId)
                .containsExactlyInAnyOrder(inA.getId(), inB.getId());
    }

    @Test
    @DisplayName("LearningFacade 미보유 사용자 — fallback으로 전체 카드 후보 수집")
    void noFacade_fallbackToAllUserCards() {
        // given — facade 없이 카드 2건
        Deck deck = persistDeck("덱", 99L);
        em.flush();
        persistCardInDeck(deck, null);
        persistCardInDeck(deck, LocalDateTime.now().minusDays(5));
        em.flush();
        em.clear();

        // when
        ReviewResponse.TodayCandidates result = reviewQueryService.getTodayCandidates(user);

        // then — 전체 2건 모두 후보. fallback 경로가 axisId 무관 동작 확인
        assertThat(result.total()).isEqualTo(2);
    }

    @Test
    @DisplayName("dailyTarget=3 + 풀 5건 — recommendedTotal=3 (풀 5건 byState에 그대로 노출)")
    void dailyTarget_applied_recommendedSubsetOfPool() {
        // given — facade 미보유(fallback) + 카드 5건
        Deck deck = persistDeck("덱", 99L);
        em.flush();
        persistCardInDeck(deck, null);
        persistCardInDeck(deck, null);
        persistCardInDeck(deck, LocalDateTime.now().minusDays(2));
        persistCardInDeck(deck, LocalDateTime.now().minusDays(2));
        persistCardInDeck(deck, LocalDateTime.now().minusDays(10));
        em.flush();

        // dailyTarget=3으로 갱신
        userScheduleCommandService.updateDailyTarget(user.getId(), 3);
        em.flush();
        em.clear();

        // when
        ReviewResponse.TodayCandidates result = reviewQueryService.getTodayCandidates(user);

        // then
        assertThat(result.total()).isEqualTo(5);
        assertThat(result.dailyTarget()).isEqualTo(3);
        assertThat(result.recommendedTotal()).isEqualTo(3);
        // 풀 5건은 byState에 모두 노출 (FE가 표시), recommended는 그 일부
        int byStateTotal = result.byState().values().stream().mapToInt(List::size).sum();
        assertThat(byStateTotal).isEqualTo(5);
    }

    @Test
    @DisplayName("target override (+N장) — dailyTarget 무시하고 target만큼 추천 (풀 cap 적용)")
    void targetOverride_appliesAndCapsAtPool() {
        // given — facade 미보유, 카드 3건
        Deck deck = persistDeck("덱", 99L);
        em.flush();
        persistCardInDeck(deck, null);
        persistCardInDeck(deck, null);
        persistCardInDeck(deck, LocalDateTime.now().minusDays(2));
        em.flush();

        userScheduleCommandService.updateDailyTarget(user.getId(), 1);   // 기본 1장
        em.flush();
        em.clear();

        // when — target=10으로 override (+9장 추가 요청)
        ReviewResponse.TodayCandidates result =
                reviewQueryService.getTodayCandidatesWithTarget(user, 10);

        // then — 풀 3건이 cap, dailyTarget(1)은 응답에 그대로 노출
        assertThat(result.dailyTarget()).isEqualTo(1);
        assertThat(result.recommendedTotal()).isEqualTo(3);
        assertThat(result.total()).isEqualTo(3);
    }
}
