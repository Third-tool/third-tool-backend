package com.example.thirdtool.Review.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DailyCardEntry — DailyLearningBatch 자식 Entity (REV E1 · M5 · Story 1-2).
 *
 * <p>batch 안 카드 노출·조회 이력을 담는 자식. DailyLearningBatch를 통해서만 생성·변경.
 * UNIQUE (batch_id, card_id) — 하나의 batch 안에서 카드 중복 없음.
 */
@Entity
@Table(
        name = "daily_card_entry",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_daily_entry_batch_card",
                columnNames = {"batch_id", "card_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyCardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private DailyLearningBatch batch;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(name = "card_interval_day", nullable = false)
    private int cardIntervalDay;

    @Column(name = "exposed_at", nullable = false, updatable = false)
    private LocalDateTime exposedAt;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    private DailyCardEntry(DailyLearningBatch batch, Long cardId, int cardIntervalDay) {
        this.batch = batch;
        this.cardId = cardId;
        this.cardIntervalDay = cardIntervalDay;
        this.exposedAt = LocalDateTime.now();
        this.viewedAt = null;
    }

    /**
     * REV E1 · Story 1-2 — batch 큐에 카드 노출 등록 (package-private · Aggregate 통해서만).
     */
    static DailyCardEntry expose(DailyLearningBatch batch, Long cardId, int cardIntervalDay) {
        return new DailyCardEntry(batch, cardId, cardIntervalDay);
    }

    /**
     * REV E1 · Story 1-2 — view 기록 (멱등 · 이미 viewed면 시각 유지).
     */
    void markViewed(LocalDateTime viewedAt) {
        if (this.viewedAt != null) return;  // 멱등
        this.viewedAt = viewedAt;
    }

    public boolean isViewed() {
        return this.viewedAt != null;
    }
}
