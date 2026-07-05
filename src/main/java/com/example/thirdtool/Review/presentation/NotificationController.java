package com.example.thirdtool.Review.presentation;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Review.application.service.NotificationService;
import com.example.thirdtool.Review.domain.exception.ReviewSessionException;
import com.example.thirdtool.Review.domain.model.UserNotification;
import com.example.thirdtool.Review.infrastructure.UserNotificationRepository;
import com.example.thirdtool.Review.presentation.dto.NotificationResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REV E3 · Story 3-8 · 알림 API (v1 minimal — polling).
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserNotificationRepository repository;

    /**
     * GET /api/v1/notifications?unread=true — 알림 목록.
     * unread=true 시 미확인만 반환. false or 미입력 시 전체.
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(
            @RequestParam(required = false, defaultValue = "false") boolean unread,
            @AuthenticationPrincipal UserEntity currentUser) {
        Long userId = currentUser.getId();
        List<UserNotification> notifications = unread
                ? notificationService.listUnread(userId)
                : notificationService.listAll(userId);
        List<NotificationResponse> body = notifications.stream()
                .map(NotificationResponse::of)
                .toList();
        return ResponseEntity.ok(body);
    }

    /** POST /api/v1/notifications/{id}/read — 읽음 표시. 소유권 검증. */
    @PostMapping("/{notificationId}/read")
    @Transactional
    public ResponseEntity<Void> markRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal UserEntity currentUser) {
        UserNotification notification = repository.findById(notificationId)
                .orElseThrow(() -> ReviewSessionException.of(
                        ErrorCode.NOTIFICATION_NOT_FOUND, "id=" + notificationId));
        if (!notification.isOwner(currentUser.getId())) {
            throw ReviewSessionException.of(ErrorCode.NOTIFICATION_FORBIDDEN);
        }
        notification.markRead(LocalDateTime.now());
        repository.save(notification);
        return ResponseEntity.noContent().build();
    }
}
