package com.example.thirdtool.Review.domain.model;

import com.example.thirdtool.Card.domain.model.Card;
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

    // Keywords·Summary가 공개된 시각. RECALLING 상태이면 null.
    @Column(name = "revealed_at")
    private LocalDateTime revealedAt;

    // 순서 보장을 위한 인덱스
    @Column(name = "card_order", nullable = false)
    private int cardOrder;

    // -------------------------------------------------------
    // 정적 팩토리
    // -------------------------------------------------------

    /**
     * RECALLING 상태로 CardReview를 초기화한다.
     * 모든 CardReview는 반드시 RECALLING으로 시작한다.
     */
    public static CardReview of(Card card, ReviewSession reviewSession, int cardOrder) {
        validateCard(card);
        validateReviewSession(reviewSession);

        CardReview cardReview = new CardReview();
        cardReview.card = card;
        cardReview.reviewSession = reviewSession;
        cardReview.reviewStep = ReviewStep.RECALLING;
        cardReview.cardOrder = cardOrder;
        cardReview.revealedAt = null;
        return cardReview;
    }

    // -------------------------------------------------------
    // 행위
    // -------------------------------------------------------

    /**
     * REVEALED 상태로 전환하고 공개 시각을 기록한다.
     * 이미 REVEALED 상태이면 무시한다. (멱등성 보장)
     * RECALLING → REVEALED 단방향 전환만 허용한다.
     */
    public void reveal() {
        if (isRevealed()) {
            return;
        }
        this.reviewStep = ReviewStep.REVEALED;
        this.revealedAt = LocalDateTime.now();
    }

    /**
     * REVEALED 상태 여부를 반환한다.
     */
    public boolean isRevealed() {
        return this.reviewStep == ReviewStep.REVEALED;
    }

    /**
     * Main 노출 여부를 반환한다.
     * Main은 RECALLING / REVEALED 모두에서 항상 노출된다.
     */
    public boolean isMainVisible() {
        return true;
    }

    /**
     * Keywords·Summary 노출 여부를 반환한다.
     * REVEALED 상태일 때만 노출된다.
     * 외부에서 공개 여부를 직접 주입할 수 없다.
     */
    public boolean isAnswerVisible() {
        return this.reviewStep == ReviewStep.REVEALED;
    }

    // -------------------------------------------------------
    // 검증
    // -------------------------------------------------------

    private static void validateCard(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("CardReview 생성 실패: card는 null일 수 없습니다.");
        }
    }

    private static void validateReviewSession(ReviewSession reviewSession) {
        if (reviewSession == null) {
            throw new IllegalArgumentException("CardReview 생성 실패: reviewSession은 null일 수 없습니다.");
        }
    }
}