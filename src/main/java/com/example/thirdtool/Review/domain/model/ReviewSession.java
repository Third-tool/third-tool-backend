package com.example.thirdtool.Review.domain.model;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Review.domain.exception.ReviewSessionException;
import com.example.thirdtool.User.domain.model.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * ReviewSession — REV E2 · SDD Story 2-1 재편 (batch 원천).
 *
 * <p>PR#2에서 심었던 {@code scope}·{@code scope_id}·{@code axis_id} 컬럼은 V36에서 폐기되고,
 * batch 참조로 대체된다. DailyLearningBatch가 하루 큐의 원천 · 세션은 그 상태 위에서 자란다.
 *
 * <p>불변식:
 * <ul>
 *   <li>batch가 closed 상태면 새 세션 생성 불가 (DAILY_BATCH_CLOSED_FOR_NEW_SESSION)</li>
 *   <li>availableCards 비면 세션 생성 불가 (DAILY_BATCH_HAS_NO_CARDS)</li>
 *   <li>finish()는 멱등 (재호출 시 finishedAt 유지)</li>
 * </ul>
 */
@Entity
@Table(name = "review_session")
@Getter
@NoArgsConstructor
public class ReviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** REV E2 · Story 2-1 — DailyLearningBatch 참조. 세션 카드 큐의 원천. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private DailyLearningBatch batch;

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

    /** REV E2 · Story 2-3 — 세션 종료 시각. finish() 시점 기록 · 자동 finish 관찰에도 사용. */
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    // -------------------------------------------------------
    // 정적 팩토리
    // -------------------------------------------------------

    /**
     * REV E2 · Story 2-1 — batch 참조 기반 세션 생성.
     *
     * <p>{@code availableCards}는 호출부(Service)에서 batch.entries.filter(!viewed) 대응 카드로 정렬해서 전달.
     * cardReviews는 그 순서대로 생성된다.
     *
     * @throws ReviewSessionException DAILY_BATCH_CLOSED_FOR_NEW_SESSION · DAILY_BATCH_HAS_NO_CARDS · REVIEW_SESSION_FORBIDDEN
     */
    public static ReviewSession startFrom(
            DailyLearningBatch batch,
            List<Card> availableCards,
            UserEntity user,
            LocalDateTime now
    ) {
        Objects.requireNonNull(batch, "batch는 null일 수 없습니다.");
        Objects.requireNonNull(user, "user는 null일 수 없습니다.");
        Objects.requireNonNull(availableCards, "availableCards는 null일 수 없습니다.");
        Objects.requireNonNull(now, "now는 null일 수 없습니다.");

        if (!batch.isOwner(user.getId())) {
            throw ReviewSessionException.of(ErrorCode.REVIEW_SESSION_FORBIDDEN);
        }
        if (batch.isClosed()) {
            throw ReviewSessionException.of(
                    ErrorCode.DAILY_BATCH_CLOSED_FOR_NEW_SESSION, "batchId=" + batch.getId());
        }
        if (availableCards.isEmpty()) {
            throw ReviewSessionException.of(ErrorCode.DAILY_BATCH_HAS_NO_CARDS);
        }

        ReviewSession session       = new ReviewSession();
        session.batch               = batch;
        session.user                = user;
        session.currentIndex        = 0;
        session.finished            = false;
        session.totalCardCount      = batch.totalCount();
        session.availableCardCount  = availableCards.size();
        session.startedAt           = now;

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

    /**
     * Story-CARD-E2-S2-4 — OnFieldBudget 폐기 후 시그니처 축소.
     * viewCount만 기록하며 archive 판정은 M5 DailyLearningBatch로 이관됐다.
     */
    public void recordCurrentCardView() {
        validateNotFinished();
        currentCardReview().recordView();
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
            this.finished   = true;
            this.finishedAt = LocalDateTime.now();
        }
    }

    /**
     * REV E2 · Story 2-3 — 명시적 finish. Story 2-3 자동 finish 시나리오에서 호출.
     * 멱등: 이미 finished면 no-op (finishedAt 유지).
     */
    public void finish(LocalDateTime now) {
        if (this.finished) return;
        Objects.requireNonNull(now, "now는 null일 수 없습니다.");
        this.finished   = true;
        this.finishedAt = now;
    }

    /** is_finished 컬럼을 직접 읽는다. cardReviews 컬렉션을 로딩하지 않는다. */
    public boolean isFinished() {
        return finished;
    }

    public boolean isActive() {
        return !finished;
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
}
