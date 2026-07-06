package com.example.thirdtool.Review.infrastructure;

import com.example.thirdtool.Review.domain.model.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RecommendationJpaRepository extends JpaRepository<Recommendation, Long> {

    @Query("""
            SELECT r FROM Recommendation r
            WHERE r.userId = :userId
              AND r.triggeredAt >= :since
            ORDER BY r.triggeredAt DESC
            """)
    List<Recommendation> findLatestSince(@Param("userId") Long userId,
                                         @Param("since") LocalDateTime since);

    @Query("""
            SELECT r FROM Recommendation r
            WHERE r.userId = :userId
              AND r.resolvedAt IS NULL
            ORDER BY r.triggeredAt DESC
            """)
    List<Recommendation> findUnresolvedByUserId(@Param("userId") Long userId);

    List<Recommendation> findByUserIdOrderByTriggeredAtDesc(Long userId);
}
