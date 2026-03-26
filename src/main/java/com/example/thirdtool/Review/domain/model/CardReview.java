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

    @Column(name = "comparing_started_at")
    private LocalDateTime comparingStartedAt;

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
        cardReview.comparingStartedAt = null;
        cardReview.cardOrder = cardOrder;
        return cardReview;
    }

    // -------------------------------------------------------
    // 행위
    // -------------------------------------------------------

    /**
     * COMPARING 상태로 전환하고 비교 시작 시각을 기록한다.
     * 이미 COMPARING 상태이면 무시한다. (멱등성 보장)
     * comparingStartedAt은 최초 전환 시각을 보존하며 덮어쓰지 않는다.
     */
    public void startComparing() {
        if (isComparing()) {
            return;
        }
        this.reviewStep = ReviewStep.COMPARING;
        this.comparingStartedAt = LocalDateTime.now();
    }


    /**
     * COMPARING 상태 여부를 반환한다.
     */
    public boolean isComparing() {
        return this.reviewStep == ReviewStep.COMPARING;
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