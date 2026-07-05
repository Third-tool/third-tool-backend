package com.example.thirdtool.Review.domain.model;

import com.example.thirdtool.Card.domain.model.Card;
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

    /**
     * @deprecated LT-E5-S5-3 (M5) · Deck BC 폐기 대기. axisId 직접 참조로 이관.
     * V33에서 review_session.deck_id 컬럼 소프트 폐기 · 다음 릴리스 완전 삭제 예정.
     */
    @Deprecated
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = true)  // V33 폐기 대응 · nullable 완화
    private Deck deck;

    /**
     * LT-E5-S5-3 (M5) — 세션이 소속된 Axis 직접 참조 (raw Long).
     * BC 간 직접 객체 참조 회피 (docs/PACKAGE.md §6).
     * <p>LT-E6-S6-1 (M5) — scope 도입 후 이 필드는 scope=AXIS일 때만 유효.
     * scope=LAYER 세션은 axisId=null (V34에서 nullable 승격).
     */
    @Column(name = "axis_id", nullable = true)
    private Long axisId;

    /**
     * LT-E6-S6-1 (M5) — 리뷰 스코프 (AXIS / LAYER).
     * 궤적 관리: PR#4 (Review E2)가 issue-25 supersede로 폐기 예정.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 10)
    private ReviewScope scope = ReviewScope.AXIS;

    /**
     * LT-E6-S6-1 (M5) — scope 대상 id.
     * scope=AXIS → axisId 동일 · scope=LAYER → layerId.
     */
    @Column(name = "scope_id", nullable = false)
    private Long scopeId;

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

    /**
     * LT-E5-S5-3 (M5) blessed — axisId 직접 주입.
     * <p>LT-E6-S6-1 (M5) — scope=AXIS · scopeId=axisId 자동 세팅. {@link #ofAxis} 알리아스.
     */
    public static ReviewSession of(Long axisId, List<Card> availableCards, UserEntity user, int totalCardCount) {
        return ofAxis(axisId, availableCards, user, totalCardCount);
    }

    /**
     * LT-E6-S6-1 (M5) — AXIS 스코프 세션 생성.
     */
    public static ReviewSession ofAxis(Long axisId, List<Card> availableCards, UserEntity user, int totalCardCount) {
        if (axisId == null) {
            throw new IllegalArgumentException("ReviewSession 생성 실패: axisId는 null일 수 없습니다.");
        }
        validateUser(user);
        validateCards(availableCards);

        ReviewSession session        = new ReviewSession();
        session.axisId               = axisId;
        session.scope                = ReviewScope.AXIS;
        session.scopeId              = axisId;
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

    /**
     * LT-E6-S6-1 (M5) — LAYER 스코프 세션 생성.
     * 여러 axis 카드가 통합된 큐. axisId는 null (LAYER 스코프에서 무의미).
     */
    public static ReviewSession ofLayer(Long layerId, List<Card> availableCards, UserEntity user, int totalCardCount) {
        if (layerId == null) {
            throw new IllegalArgumentException("ReviewSession 생성 실패: layerId는 null일 수 없습니다.");
        }
        validateUser(user);
        validateCards(availableCards);

        ReviewSession session        = new ReviewSession();
        session.axisId               = null;  // LAYER 스코프는 특정 axis에 종속되지 않음
        session.scope                = ReviewScope.LAYER;
        session.scopeId              = layerId;
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

    /**
     * @deprecated LT-E5-S5-3 (M5) — Deck 폐기 대응. of(Long axisId, ...) 사용 권장.
     * deck에서 axisId 뽑아서 blessed 팩토리로 위임. deck.deck 필드는 @Deprecated 유지용으로 세팅.
     */
    @Deprecated
    public static ReviewSession of(Deck deck, List<Card> availableCards, UserEntity user, int totalCardCount) {
        validateDeck(deck);
        Long axisId = deck.getAxisId();
        if (axisId == null) {
            throw new IllegalArgumentException("ReviewSession 생성 실패: deck.axisId는 null일 수 없습니다.");
        }
        ReviewSession session = of(axisId, availableCards, user, totalCardCount);
        session.deck = deck;  // @Deprecated 필드 유지용
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