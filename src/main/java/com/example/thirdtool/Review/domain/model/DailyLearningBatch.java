package com.example.thirdtool.Review.domain.model;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Review.domain.exception.ReviewSessionException;
import com.example.thirdtool.UserSchedule.domain.model.LearningMode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * DailyLearningBatch — 하루당 1개 Aggregate Root (REV E1 · M5 · Story 1-2).
 *
 * <p>사용자별 그날 학습 큐(cross-layer 짬뽕) · 진행 상태 · 카드 관점 이력을 단일 도메인으로 응집.
 * 자정 close cron으로 "그날 못 본 카드 = 그냥 지나감" 원칙 강제.
 *
 * <p>불변식:
 * <ul>
 *   <li>UNIQUE (user_id, batch_date) — 하루당 1개</li>
 *   <li>closed 상태에서 markViewed → DAILY_BATCH_CLOSED 예외</li>
 *   <li>close 재호출은 no-op (멱등)</li>
 *   <li>entries=0 일 때 completionRatio = 1.0 (0 나누기 방지 · perfect clear 간주)</li>
 * </ul>
 */
@Entity
@Table(
        name = "daily_learning_batch",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_daily_batch_user_date",
                columnNames = {"user_id", "batch_date"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyLearningBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "batch_date", nullable = false)
    private LocalDate batchDate;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_mode_at_generation", nullable = false, length = 20)
    private LearningMode userModeAtGeneration;

    @OneToMany(
            mappedBy = "batch",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private final List<DailyCardEntry> entries = new ArrayList<>();

    private DailyLearningBatch(Long userId, LocalDate batchDate, LearningMode userModeAtGeneration) {
        this.userId = userId;
        this.batchDate = batchDate;
        this.userModeAtGeneration = userModeAtGeneration;
        this.generatedAt = LocalDateTime.now();
        this.closedAt = null;
    }

    /**
     * REV E1 · Story 1-2 — 빈 batch 생성 (테스트·간단 시나리오용).
     * 실제 orchestration은 {@link #generateFor}를 통한 카드 큐 구성 필요.
     */
    public static DailyLearningBatch create(Long userId, LocalDate batchDate, LearningMode userModeAtGeneration) {
        Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
        Objects.requireNonNull(batchDate, "batchDate는 null일 수 없습니다.");
        Objects.requireNonNull(userModeAtGeneration, "userModeAtGeneration는 null일 수 없습니다.");
        return new DailyLearningBatch(userId, batchDate, userModeAtGeneration);
    }

    /**
     * REV E1 · Story 1-2 — batch 생성 + 카드 큐 채우기 orchestration.
     *
     * <p>Application Service가 사용자 활성 카드 목록을 전달 · 도메인이 due/exhausted 판정 · entries 구성.
     * exhausted 카드는 반환값(exhaustedCards)에 · Application Service가 archive 위임.
     *
     * @return 튜플 유사 결과 · Aggregate 자체 + exhaustedCards 리스트
     */
    public static GenerationResult generateFor(Long userId, LocalDate today, LearningMode userCurrentMode, List<Card> allUserCards) {
        Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
        Objects.requireNonNull(today, "today는 null일 수 없습니다.");
        Objects.requireNonNull(userCurrentMode, "userCurrentMode는 null일 수 없습니다.");
        Objects.requireNonNull(allUserCards, "allUserCards는 null일 수 없습니다.");

        DailyLearningBatch batch = new DailyLearningBatch(userId, today, userCurrentMode);
        List<Card> exhausted = new ArrayList<>();

        for (Card card : allUserCards) {
            if (card.isArchived() || card.isDeleted()) continue;
            if (card.hasScheduleExhausted(userCurrentMode, today)) {
                exhausted.add(card);
                continue;
            }
            if (card.isDueOn(today, userCurrentMode)) {
                int intervalDay = daysSinceEntered(card, today);
                batch.entries.add(DailyCardEntry.expose(batch, card.getId(), intervalDay));
            }
        }

        return new GenerationResult(batch, exhausted);
    }

    private static int daysSinceEntered(Card card, LocalDate today) {
        if (card.getEnteredFieldAt() == null) return 0;
        long days = java.time.temporal.ChronoUnit.DAYS.between(
                card.getEnteredFieldAt().toLocalDate(), today);
        return (int) Math.max(0, days);
    }

    /**
     * REV E1 · Story 1-2 — 카드 view 기록. closed 상태에서 예외.
     * 이미 viewed인 entry 재호출은 no-op (멱등).
     */
    public void markViewed(Long cardId, LocalDateTime viewedAt) {
        if (isClosed()) {
            throw ReviewSessionException.of(ErrorCode.DAILY_BATCH_CLOSED, "batchId=" + this.id);
        }
        Objects.requireNonNull(cardId, "cardId는 null일 수 없습니다.");
        Objects.requireNonNull(viewedAt, "viewedAt은 null일 수 없습니다.");

        entries.stream()
                .filter(e -> e.getCardId().equals(cardId))
                .findFirst()
                .ifPresent(e -> e.markViewed(viewedAt));
    }

    /**
     * REV E1 · Story 1-2 — 자정 close (00:05 KST cron).
     * 재호출은 no-op (멱등).
     */
    public void close(LocalDateTime closedAt) {
        if (this.closedAt != null) return;  // 멱등
        Objects.requireNonNull(closedAt, "closedAt은 null일 수 없습니다.");
        this.closedAt = closedAt;
    }

    public boolean isClosed() {
        return this.closedAt != null;
    }

    public int totalCount() {
        return entries.size();
    }

    public int viewedCount() {
        return (int) entries.stream().filter(DailyCardEntry::isViewed).count();
    }

    public double completionRatio() {
        int total = totalCount();
        if (total == 0) return 1.0;  // 0으로 나누기 방지 · perfect clear
        return (double) viewedCount() / total;
    }

    public boolean isPerfectClear() {
        return completionRatio() >= 1.0;
    }

    public List<DailyCardEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * REV E2 · Story 2-1 — 아직 보지 않은 entries만 필터링.
     * ReviewSession 신규 생성 시 카드 큐 원천으로 사용.
     */
    public List<DailyCardEntry> unviewedEntries() {
        return entries.stream()
                .filter(e -> !e.isViewed())
                .toList();
    }

    public boolean isOwner(Long userId) {
        return this.userId.equals(userId);
    }

    /** Story 1-2 — generateFor 결과 record. */
    public record GenerationResult(DailyLearningBatch batch, List<Card> exhaustedCards) {}
}
