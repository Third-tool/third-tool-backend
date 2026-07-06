package com.example.thirdtool.Review.infrastructure;

import com.example.thirdtool.Review.domain.model.UserNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserNotificationRepositoryAdapter implements UserNotificationRepository {

    private final UserNotificationJpaRepository jpa;

    @Override
    public UserNotification save(UserNotification notification) {
        return jpa.save(notification);
    }

    @Override
    public Optional<UserNotification> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public List<UserNotification> findAllByUserIdAndUnread(Long userId) {
        return jpa.findUnreadByUserId(userId);
    }

    @Override
    public List<UserNotification> findAllByUserId(Long userId) {
        return jpa.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
