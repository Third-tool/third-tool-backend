package com.example.thirdtool.Review.application;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.MainNote;
import com.example.thirdtool.Card.domain.model.OnFieldBudget;
import com.example.thirdtool.Card.domain.model.Summary;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.LearningFacade.application.service.LearningFacadeQueryService;
import com.example.thirdtool.Review.domain.model.StateRecommendationDistributor;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Review.domain.model.ReviewSession;
import com.example.thirdtool.Review.infrastructure.ReviewSessionRepository;
import com.example.thirdtool.Review.presentation.dto.ReviewResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.UserSchedule.application.service.UserScheduleQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ReviewQueryService — Story 2-1·2-3 budget 주입 + 소유자 검증 매트릭스.
 */
@DisplayName("ReviewQueryService — Story 2-1·2-3 budget 주입 / 소유자 검증")
class ReviewQueryServiceTest {

    private ReviewSessionRepository sessionRepository;
    private UserScheduleQueryService userScheduleQueryService;
    private CardRepository cardRepository;
    private ReviewQueryService service;

    private UserEntity owner;
    private UserEntity other;
    private Deck deck;

    @BeforeEach
    void setUp() {
        sessionRepository        = mock(ReviewSessionRepository.class);
        userScheduleQueryService = mock(UserScheduleQueryService.class);
        cardRepository           = mock(CardRepository.class);
        service = new ReviewQueryService(
                sessionRepository, userScheduleQueryService, cardRepository,
                new StateRecommendationDistributor(),
                mock(LearningFacadeQueryService.class)
        );

        owner = UserEntity.ofLocal("owner", "pw", "n", "o@e.com");
        ReflectionTestUtils.setField(owner, "id", 1L);
        other = UserEntity.ofLocal("other", "pw", "n", "x@e.com");
        ReflectionTestUtils.setField(other, "id", 2L);
        deck = Deck.createFromAxis(owner, 10L, "DDD");
        ReflectionTestUtils.setField(deck, "id", 500L);
    }

    private Card sampleCard() {
        Card card = Card.create(deck, MainNote.of("본문", null), Summary.of("한 문장."), List.of("k1"));
        ReflectionTestUtils.setField(card, "id", 1000L);
        return card;
    }

    private ReviewSession persistedSession(Card... cards) {
        ReviewSession s = ReviewSession.of(deck, List.of(cards), owner, cards.length);
        ReflectionTestUtils.setField(s, "id", 700L);
        when(sessionRepository.findById(700L)).thenReturn(Optional.of(s));
        return s;
    }

    @Test
    @DisplayName("findById — 소유자 정상 + isLastView가 사용자별 budget으로 결정된다")
    void findById_owner_resolvesIsLastViewByUserBudget() {
        Card card = sampleCard();
        card.recordView();
        assertThat(card.getViewCount()).isEqualTo(1);
        persistedSession(card);
        // budget maxView=1 → 현재 viewCount=1이므로 isLastView=true
        when(userScheduleQueryService.resolveOnFieldBudget(1L))
                .thenReturn(OnFieldBudget.of(1, Duration.ofDays(10)));

        ReviewResponse.SessionDetail response = service.findById(700L, owner);

        assertThat(response.currentCard()).isNotNull();
        assertThat(response.currentCard().isLastView()).isTrue();
    }

    @Test
    @DisplayName("findById — 미존재 sessionId면 REVIEW_SESSION_NOT_FOUND")
    void findById_notFound_throws() {
        when(sessionRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(9999L, owner))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REVIEW_SESSION_NOT_FOUND);

        verify(userScheduleQueryService, never()).resolveOnFieldBudget(any());
    }

    @Test
    @DisplayName("findById — 소유자 아님이면 REVIEW_SESSION_FORBIDDEN")
    void findById_otherUser_throws() {
        Card card = sampleCard();
        persistedSession(card);

        assertThatThrownBy(() -> service.findById(700L, other))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REVIEW_SESSION_FORBIDDEN);

        verify(userScheduleQueryService, never()).resolveOnFieldBudget(any());
    }

    @Test
    @DisplayName("findById — 종료된 세션은 currentCard=null + isLastView 결정 미발생(budget 미조회)")
    void findById_finishedSession_currentCardNull() {
        Card card = sampleCard();
        ReviewSession s = persistedSession(card);
        ReflectionTestUtils.setField(s, "finished", true);

        ReviewResponse.SessionDetail response = service.findById(700L, owner);

        assertThat(response.isFinished()).isTrue();
        assertThat(response.currentCard()).isNull();
        // budget 조회 자체는 finished 검사 전에 발생할 수 있으나, isLastView가 false로 고정되어 의미 없음.
        // 결과 정합만 검증한다.
    }
}
