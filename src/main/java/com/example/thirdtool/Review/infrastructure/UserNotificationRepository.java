package com.example.thirdtool.Review.infrastructure;

import com.example.thirdtool.Review.domain.model.UserNotification;

import java.util.List;
import java.util.Optional;

/**
 * UserNotificationRepository port — REV E3 · Story 3-8.
 */
public interface UserNotificationRepository {

    UserNotification save(UserNotification notification);

    Optional<UserNotification> findById(Long id);

    List<UserNotification> findAllByUserIdAndUnread(Long userId);

    List<UserNotification> findAllByUserId(Long userId);
}
