package com.example.thirdtool.Review.application;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.MainNote;
import com.example.thirdtool.Card.domain.model.Summary;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.LearningFacade.application.service.LearningFacadeQueryService;
import com.example.thirdtool.Review.domain.model.ReviewSession;
import com.example.thirdtool.Review.domain.model.StateRecommendationDistributor;
import com.example.thirdtool.Review.infrastructure.ReviewSessionRepository;
import com.example.thirdtool.Review.presentation.dto.ReviewResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.UserSchedule.application.service.UserScheduleQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ReviewQueryService — Story-CARD-E2-S2-4 재작성.
 *
 * <p>OnFieldBudget 폐기(PR#2)에 따라 {@code isLastView} 판정이 사라진다.
 * API 필드 호환을 위해 응답 DTO에는 남지만 항상 {@code false}로 반환된다.
 */
@DisplayName("ReviewQueryService — Story-CARD-E2-S2-4 (isLastView 항상 false)")
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
    @DisplayName("findById — 소유자 정상 조회 시 isLastView는 항상 false")
    void findById_owner_isLastViewAlwaysFalse() {
        Card card = sampleCard();
        // viewCount를 임의로 늘려도 isLastView는 false로 고정된다 (OnFieldBudget 폐기).
        card.recordView();
        card.recordView();
        card.recordView();
        assertThat(card.getViewCount()).isEqualTo(3);
        persistedSession(card);

        ReviewResponse.SessionDetail response = service.findById(700L, owner);

        assertThat(response.currentCard()).isNotNull();
        assertThat(response.currentCard().isLastView()).isFalse();
    }

    @Test
    @DisplayName("findById — 미존재 sessionId면 REVIEW_SESSION_NOT_FOUND")
    void findById_notFound_throws() {
        when(sessionRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(9999L, owner))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REVIEW_SESSION_NOT_FOUND);
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
    }

    @Test
    @DisplayName("findById — 종료된 세션은 currentCard=null")
    void findById_finishedSession_currentCardNull() {
        Card card = sampleCard();
        ReviewSession s = persistedSession(card);
        ReflectionTestUtils.setField(s, "finished", true);

        ReviewResponse.SessionDetail response = service.findById(700L, owner);

        assertThat(response.isFinished()).isTrue();
        assertThat(response.currentCard()).isNull();
    }
}
