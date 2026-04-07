package com.example.thirdtool.Review.domain.model;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.OnFieldBudget;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @OneToMany(mappedBy = "reviewSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("cardOrder ASC")
    private List<CardReview> cardReviews = new ArrayList<>();

    @Column(name = "current_index", nullable = false)
    private int currentIndex;

    @Column(name = "is_finished", nullable = false)
    private boolean finished = false;

    @Column(name = "total_card_count", nullable = false)
    private int totalCardCount;

    @Column(name = "available_card_count", nullable = false)
    private int availableCardCount;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    // -------------------------------------------------------
    // 정적 팩토리
    // -------------------------------------------------------

    public static ReviewSession of(Deck deck, List<Card> availableCards, UserEntity user, int totalCardCount) {
        validateDeck(deck);
        validateUser(user);
        validateCards(availableCards);

        ReviewSession session        = new ReviewSession();
        session.deck                 = deck;
        session.user                 = user;
        session.currentIndex         = 0;
        session.finished             = false;
        session.totalCardCount       = totalCardCount;
        session.availableCardCount   = availableCards.size();
        session.startedAt            = LocalDateTime.now();

        for (int i = 0; i < availableCards.size(); i++) {
            session.cardReviews.add(CardReview.of(availableCards.get(i), session, i));
        }

        return session;
    }

    // -------------------------------------------------------
    // 행위
    // -------------------------------------------------------

    public CardReview currentCardReview() {
        validateNotFinished();
        return cardReviews.get(currentIndex);
    }

    public void startComparingCurrentCard() {
        validateNotFinished();
        currentCardReview().startComparing();
    }

    public boolean recordCurrentCardView(OnFieldBudget budget) {
        validateNotFinished();
        CardReview current = currentCardReview();
        current.recordView();
        return current.isCardLastView(budget.getMaxView());
    }

    public void moveToNext() {
        validateNotFinished();
        if (!currentCardReview().isComparing()) {
            throw ReviewSessionException.of(
                    ErrorCode.REVIEW_COMPARING_REQUIRED,
                    "currentIndex=" + currentIndex);
        }
        this.currentIndex++;
        if (this.currentIndex >= cardReviews.size()) {
            this.finished = true;
        }
    }

    /** is_finished 컬럼을 직접 읽는다. cardReviews 컬렉션을 로딩하지 않는다. */
    public boolean isFinished() {
        return finished;
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
        if (deck == null) throw new IllegalArgumentException("ReviewSession 생성 실패: deck은 null일 수 없습니다.");
    }

    private static void validateUser(UserEntity user) {
        if (user == null) throw new IllegalArgumentException("ReviewSession 생성 실패: user는 null일 수 없습니다.");
    }

    private static void validateCards(List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            throw ReviewSessionException.of(ErrorCode.REVIEW_DECK_HAS_NO_CARDS);
        }
    }
}