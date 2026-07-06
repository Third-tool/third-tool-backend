package com.example.thirdtool.Review.application.service;

import com.example.thirdtool.Review.domain.model.NotificationType;
import com.example.thirdtool.Review.domain.model.UserNotification;
import com.example.thirdtool.Review.infrastructure.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * NotificationService — REV E3 · Story 3-3/3-8 (v1 minimal).
 *
 * <p>DB row 삽입만 담당. WebSocket / FCM push는 v2 이관.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final UserNotificationRepository repository;

    /**
     * REV E3 · Story 3-3 · 알림 발송. payload는 호출자가 직접 직렬화한 JSON 문자열.
     * recommendationId는 SUGGEST_* 유형일 때만 세팅 (그 외 null).
     */
    public UserNotification send(Long userId, NotificationType type, String payloadJson, Long recommendationId) {
        UserNotification notification = UserNotification.of(
                userId, type, payloadJson, recommendationId, LocalDateTime.now());
        return repository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<UserNotification> listUnread(Long userId) {
        return repository.findAllByUserIdAndUnread(userId);
    }

    @Transactional(readOnly = true)
    public List<UserNotification> listAll(Long userId) {
        return repository.findAllByUserId(userId);
    }
}
