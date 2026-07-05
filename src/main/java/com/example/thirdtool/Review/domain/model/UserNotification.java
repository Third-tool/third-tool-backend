package com.example.thirdtool.Review.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * UserNotification Aggregate — REV E3 · Story 3-8 (v1 minimal).
 *
 * <p>DB row + 프론트 polling 방식. WebSocket / FCM은 v2 이관.
 */
@Entity
@Table(name = "user_notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    /** v1 minimal: JSON 문자열 저장 · payload 구조는 type별 클라이언트가 파싱. */
    @Column(name = "payload_json", nullable = false, length = 2000)
    private String payloadJson;

    /** REV E3 · Story 3-5 accept flow — 추천 알림일 경우 참조. 그 외 null. */
    @Column(name = "recommendation_id")
    private Long recommendationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    private UserNotification(Long userId, NotificationType type, String payloadJson,
                             Long recommendationId, LocalDateTime createdAt) {
        this.userId = userId;
        this.type = type;
        this.payloadJson = payloadJson;
        this.recommendationId = recommendationId;
        this.createdAt = createdAt;
    }

    public static UserNotification of(Long userId, NotificationType type, String payloadJson,
                                      Long recommendationId, LocalDateTime createdAt) {
        Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
        Objects.requireNonNull(type, "type은 null일 수 없습니다.");
        Objects.requireNonNull(payloadJson, "payloadJson은 null일 수 없습니다.");
        Objects.requireNonNull(createdAt, "createdAt은 null일 수 없습니다.");
        return new UserNotification(userId, type, payloadJson, recommendationId, createdAt);
    }

    /**
     * REV E3 · Story 3-8 — 읽음 표시. 멱등 (재호출 시 readAt 유지).
     */
    public void markRead(LocalDateTime now) {
        if (this.readAt != null) return;
        Objects.requireNonNull(now, "now는 null일 수 없습니다.");
        this.readAt = now;
    }

    public boolean isRead() {
        return this.readAt != null;
    }

    public boolean isOwner(Long userId) {
        return this.userId.equals(userId);
    }
}
