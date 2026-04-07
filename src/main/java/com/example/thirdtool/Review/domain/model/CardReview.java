package com.example.thirdtool.Review.domain.model;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.OnFieldBudget;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "card_review")
@Getter
@NoArgsConstructor
public class CardReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Card BC → 읽기 전용 참조 (cascade 없음, 소유하지 않음)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_session_id", nullable = false)
    private ReviewSession reviewSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_step", nullable = false)
    private ReviewStep reviewStep;

    @Column(name = "comparing_started_at")
    private LocalDateTime comparingStartedAt;

    @Column(name = "card_order", nullable = false)
    private int cardOrder;

    // -------------------------------------------------------
    // 정적 팩토리
    // -------------------------------------------------------

    public static CardReview of(Card card, ReviewSession reviewSession, int cardOrder) {
        validateCard(card);
        validateReviewSession(reviewSession);

        CardReview cardReview         = new CardReview();
        cardReview.card               = card;
        cardReview.reviewSession      = reviewSession;
        cardReview.reviewStep         = ReviewStep.RECALLING;
        cardReview.comparingStartedAt = null;
        cardReview.cardOrder          = cardOrder;
        return cardReview;
    }

    // -------------------------------------------------------
    // 행위
    // -------------------------------------------------------

    public void startComparing() {
        if (isComparing()) return;
        this.reviewStep         = ReviewStep.COMPARING;
        this.comparingStartedAt = LocalDateTime.now();
    }

    void recordView() {
        card.recordView();
    }

    boolean isCardLastView(int maxView) {
        return card.isLastView(maxView);
    }

    public boolean isComparing() {
        return this.reviewStep == ReviewStep.COMPARING;
    }

    public boolean isMainVisible() {
        return true;
    }

    public boolean isAnswerVisible() {
        return this.reviewStep == ReviewStep.COMPARING;
    }

    public CardVisibleContent visibleContent() {
        if (isComparing()) {
            return CardVisibleContent.comparing(
                    card.getMainNote(),
                    card.getKeywordCues(),
                    card.getSummary()
                                               );
        }
        return CardVisibleContent.recalling(card.getMainNote());
    }

    // -------------------------------------------------------
    // 검증
    // -------------------------------------------------------

    private static void validateCard(Card card) {
        if (card == null) throw new IllegalArgumentException("CardReview 생성 실패: card는 null일 수 없습니다.");
    }

    private static void validateReviewSession(ReviewSession reviewSession) {
        if (reviewSession == null) throw new IllegalArgumentException("CardReview 생성 실패: reviewSession은 null일 수 없습니다.");
    }
}