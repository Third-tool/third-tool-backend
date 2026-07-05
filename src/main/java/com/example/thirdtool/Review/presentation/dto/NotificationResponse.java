package com.example.thirdtool.Review.presentation.dto;

import com.example.thirdtool.Review.domain.model.NotificationType;
import com.example.thirdtool.Review.domain.model.UserNotification;

import java.time.LocalDateTime;

/**
 * REV E3 · Story 3-8 · Notification API 응답.
 */
public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        String payloadJson,
        Long recommendationId,   // nullable — SUGGEST_* 유형일 때만
        LocalDateTime createdAt,
        LocalDateTime readAt,
        boolean isRead
) {
    public static NotificationResponse of(UserNotification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getPayloadJson(),
                n.getRecommendationId(),
                n.getCreatedAt(),
                n.getReadAt(),
                n.isRead()
        );
    }
}
