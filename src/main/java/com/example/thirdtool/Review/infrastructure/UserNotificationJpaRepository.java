package com.example.thirdtool.Review.infrastructure;

import com.example.thirdtool.Review.domain.model.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserNotificationJpaRepository extends JpaRepository<UserNotification, Long> {

    @Query("""
            SELECT n FROM UserNotification n
            WHERE n.userId = :userId
              AND n.readAt IS NULL
            ORDER BY n.createdAt DESC
            """)
    List<UserNotification> findUnreadByUserId(@Param("userId") Long userId);

    List<UserNotification> findByUserIdOrderByCreatedAtDesc(Long userId);
}
