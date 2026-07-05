package com.example.thirdtool.Review.infrastructure;


import com.example.thirdtool.Review.domain.model.ReviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ReviewSessionRepository — REV E2 · Story 2-6 재편.
 *
 * <p>PR#2에서 심었던 deck / axis / layer 스코프 조회 메서드는 batch 참조 기반으로 완전 대체.
 */
public interface ReviewSessionRepository
        extends JpaRepository<ReviewSession, Long>, ReviewSessionRepositoryCustom {

    // 특정 사용자의 전체 세션 목록 (최신순 정렬은 QueryDSL로 처리)
    List<ReviewSession> findByUserId(Long userId);

    /**
     * REV E2 · Story 2-3 + 2-6 — 사용자의 진행 중인 세션 조회.
     * 자동 finish 정책 트리거용. finished_at IS NULL 최신.
     */
    Optional<ReviewSession> findFirstByUserIdAndFinishedFalseOrderByStartedAtDesc(Long userId);

    /**
     * REV E2 · Story 2-6 — 특정 batch로 시작된 세션 목록 (통계용).
     */
    List<ReviewSession> findAllByBatchId(Long batchId);

    /**
     * REV E2 · Story 2-6 — 특정 사용자 · 날짜 세션 목록 (batch.batchDate 기준).
     * 대시보드 · 이력 조회용.
     */
    @Query("""
            SELECT s FROM ReviewSession s
            WHERE s.user.id = :userId
              AND s.batch.batchDate = :date
            ORDER BY s.startedAt DESC
            """)
    List<ReviewSession> findAllByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);
}
