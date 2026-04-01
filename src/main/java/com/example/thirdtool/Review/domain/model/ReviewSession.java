package com.example.thirdtool.Review.domain.model;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Review.domain.exception.ReviewSessionException;
import com.example.thirdtool.User.domain.model.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "review_session")
@Getter
@NoArgsConstructor
public class ReviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Deck BC → 읽기 전용 참조 (cascade 없음, 소유하지 않음)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // CardReview는 ReviewSession이 소유한다.
    @OneToMany(mappedBy = "reviewSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("cardOrder ASC")
    private List<CardReview> cardReviews = new ArrayList<>();

    @Column(name = "current_index", nullable = false)
    private int currentIndex;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    // -------------------------------------------------------
    // 정적 팩토리
    // -------------------------------------------------------

    /**
     * 리뷰 세션을 생성하고 CardReview 목록을 초기화한다.
     *
     * <p>카드 목록은 Application Service가 Repository에서 조회해 전달한다.
     * ReviewSession이 {@code deck.getCards()}를 직접 호출하지 않는다.
     * (Deck Aggregate와의 직접 의존 방지, N+1 제어권을 Application Service에 위임)
     *
     * <p>첫 번째 카드는 자동으로 RECALLING 상태로 초기화된다.
     *
     * @param deck   리뷰 대상 덱
     * @param cards  덱에 속한 활성 카드 목록 (등록 순). 비어 있으면 예외 발생
     * @param user   세션 소유 사용자
     * @throws ReviewSessionException 카드 목록이 비어 있으면
     */
    public static ReviewSession of(Deck deck, List<Card> cards, UserEntity user) {
        validateDeck(deck);
        validateUser(user);
        validateCards(cards);

        ReviewSession session = new ReviewSession();
        session.deck        = deck;
        session.user        = user;
        session.currentIndex = 0;
        session.startedAt   = LocalDateTime.now();

        // 카드 목록을 등록 순으로 CardReview로 변환
        for (int i = 0; i < cards.size(); i++) {
            CardReview cardReview = CardReview.of(cards.get(i), session, i);
            session.cardReviews.add(cardReview);
        }

        return session;
    }

    // -------------------------------------------------------
    // 행위
    // -------------------------------------------------------

    public CardReview currentCardReview() {
        return cardReviews.get(currentIndex);
    }

    public void startComparingCurrentCard() {
        validateNotFinished();
        currentCardReview().startComparing();
    }

    public void moveToNext() {
        validateNotFinished();
        if (!currentCardReview().isComparing()) {
            throw ReviewSessionException.of(
                    ErrorCode.REVIEW_COMPARING_REQUIRED,
                    "currentIndex=" + currentIndex);
        }
        this.currentIndex++;
    }

    public boolean isFinished() {
        return currentIndex >= cardReviews.size();
    }

    public boolean isOwner(Long userId) {
        return this.user.getId().equals(userId);
    }

    // -------------------------------------------------------
    // 내부 검증
    // -------------------------------------------------------

    private void validateNotFinished() {
        if (isFinished()) {
            throw ReviewSessionException.of(ErrorCode.REVIEW_SESSION_ALREADY_FINISHED);
        }
    }

    private static void validateDeck(Deck deck) {
        if (deck == null) {
            throw new IllegalArgumentException("ReviewSession 생성 실패: deck은 null일 수 없습니다.");
        }
    }

    private static void validateUser(UserEntity user) {
        if (user == null) {
            throw new IllegalArgumentException("ReviewSession 생성 실패: user는 null일 수 없습니다.");
        }
    }

    private static void validateCards(List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            throw ReviewSessionException.of(ErrorCode.REVIEW_DECK_HAS_NO_CARDS);
        }
    }
}
