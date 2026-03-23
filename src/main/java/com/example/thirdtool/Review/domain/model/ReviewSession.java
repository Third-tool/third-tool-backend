package com.example.thirdtool.Review.domain.model;

import com.example.thirdtool.Card.domain.model.Card;
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

    // Card BC → 읽기 전용 참조
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
     * 세션을 생성하고 리뷰 순서를 초기화한다.
     * 첫 번째 카드는 자동으로 RECALLING 상태로 초기화된다.
     * 카드가 0개인 덱으로는 세션을 시작할 수 없다.
     */
    public static ReviewSession start(Deck deck, UserEntity user) {
        validateDeck(deck);
        validateUser(user);
        validateDeckHasCards(deck);

        ReviewSession session = new ReviewSession();
        session.deck = deck;
        session.user = user;
        session.currentIndex = 0;
        session.startedAt = LocalDateTime.now();

        // 카드 목록을 등록 순으로 CardReview로 변환
        List<Card> cards = deck.getCards();
        for (int i = 0; i < cards.size(); i++) {
            CardReview cardReview = CardReview.of(cards.get(i), session, i);
            session.cardReviews.add(cardReview);
        }

        return session;
    }

    // -------------------------------------------------------
    // 행위
    // -------------------------------------------------------

    /**
     * 현재 리뷰 중인 CardReview를 반환한다.
     */
    public CardReview currentCardReview() {
        return cardReviews.get(currentIndex);
    }

    /**
     * 현재 카드를 REVEALED 상태로 전환한다.
     * Keywords·Summary가 공개된다.
     */
    public void revealCurrentCard() {
        currentCardReview().reveal();
    }

    /**
     * 다음 카드로 이동한다.
     * 현재 카드가 REVEALED 상태일 때만 호출할 수 있다.
     * RECALLING 상태에서 다음 카드로 건너뛸 수 없다.
     */
    public void nextCard() {
        if (isFinished()) {
            throw new ReviewSessionException("이미 모든 카드 리뷰가 완료된 세션입니다.");
        }
        if (!currentCardReview().isRevealed()) {
            throw new ReviewSessionException("현재 카드를 확인(REVEALED)한 후에 다음 카드로 이동할 수 있습니다.");
        }
        this.currentIndex++;
    }

    /**
     * 모든 카드 리뷰 완료 여부를 반환한다.
     */
    public boolean isFinished() {
        return currentIndex >= cardReviews.size();
    }

    // -------------------------------------------------------
    // 검증
    // -------------------------------------------------------

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

    private static void validateDeckHasCards(Deck deck) {
        if (deck.getCards() == null || deck.getCards().isEmpty()) {
            throw new ReviewSessionException("카드가 없는 덱으로는 리뷰 세션을 시작할 수 없습니다.");
        }
    }
}
