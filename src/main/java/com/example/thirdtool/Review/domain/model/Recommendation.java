package com.example.thirdtool.Review.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Review.domain.exception.ReviewSessionException;
import com.example.thirdtool.UserSchedule.domain.model.LearningMode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Recommendation Aggregate — REV E3 · Story 3-2/3-5.
 *
 * <p>규칙 기반 추천 이력. RecommendationEngine이 판정 결과를 생성 · 사용자 accept/dismiss로 resolve.
 * v2 자동 조정 근거 축적을 위해 accept/dismiss 상태와 시각을 이력으로 보존.
 *
 * <p>불변식:
 * <ul>
 *   <li>triggeredAt은 생성 시 자동 · 외부 주입 금지</li>
 *   <li>resolve(action, now) 재호출은 RECOMMENDATION_ALREADY_RESOLVED (멱등 금지)</li>
 *   <li>SUGGEST_DOWNGRADE는 fromMode &gt; toMode · SUGGEST_UPGRADE는 fromMode &lt; toMode</li>
 * </ul>
 */
@Entity
@Table(name = "recommendation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private RecommendationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_mode", nullable = false, length = 20)
    private LearningMode fromMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_mode", nullable = false, length = 20)
    private LearningMode toMode;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "triggered_at", nullable = false, updatable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 20)
    private RecommendationAction action;

    private Recommendation(Long userId, RecommendationType type, LearningMode fromMode,
                           LearningMode toMode, String reason, LocalDateTime triggeredAt) {
        this.userId = userId;
        this.type = type;
        this.fromMode = fromMode;
        this.toMode = toMode;
        this.reason = reason;
        this.triggeredAt = triggeredAt;
    }

    /**
     * REV E3 · Story 3-2 — 규칙 판정 결과를 이력으로 저장.
     */
    public static Recommendation of(Long userId, RecommendationType type, LearningMode fromMode,
                                    LearningMode toMode, String reason, LocalDateTime triggeredAt) {
        Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
        Objects.requireNonNull(type, "type은 null일 수 없습니다.");
        Objects.requireNonNull(fromMode, "fromMode는 null일 수 없습니다.");
        Objects.requireNonNull(toMode, "toMode는 null일 수 없습니다.");
        Objects.requireNonNull(reason, "reason은 null일 수 없습니다.");
        Objects.requireNonNull(triggeredAt, "triggeredAt은 null일 수 없습니다.");

        // 도메인 불변식 · 방향 검증
        validateDirection(type, fromMode, toMode);

        return new Recommendation(userId, type, fromMode, toMode, reason, triggeredAt);
    }

    private static void validateDirection(RecommendationType type, LearningMode from, LearningMode to) {
        if (type == RecommendationType.SUGGEST_DOWNGRADE && from.maxDays() <= to.maxDays()) {
            throw new IllegalArgumentException(
                    "SUGGEST_DOWNGRADE는 fromMode > toMode여야 합니다. from=" + from + " to=" + to);
        }
        if (type == RecommendationType.SUGGEST_UPGRADE && from.maxDays() >= to.maxDays()) {
            throw new IllegalArgumentException(
                    "SUGGEST_UPGRADE는 fromMode < toMode여야 합니다. from=" + from + " to=" + to);
        }
    }

    /**
     * REV E3 · Story 3-5 — 사용자 accept/dismiss 처리.
     * 이미 resolved면 예외 · 멱등 금지 (재resolve는 이력 왜곡).
     */
    public void resolve(RecommendationAction action, LocalDateTime now) {
        if (this.resolvedAt != null) {
            throw ReviewSessionException.of(ErrorCode.RECOMMENDATION_ALREADY_RESOLVED,
                    "recommendationId=" + this.id);
        }
        Objects.requireNonNull(action, "action은 null일 수 없습니다.");
        Objects.requireNonNull(now, "now는 null일 수 없습니다.");
        this.action = action;
        this.resolvedAt = now;
    }

    public boolean isResolved() {
        return this.resolvedAt != null;
    }

    public boolean isOwner(Long userId) {
        return this.userId.equals(userId);
    }
}
