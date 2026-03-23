package com.example.thirdtool.Review.infrastructure;


import com.example.thirdtool.Review.domain.model.ReviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewSessionRepository
        extends JpaRepository<ReviewSession, Long>, ReviewSessionRepositoryCustom {

    // 특정 사용자의 전체 세션 목록 (최신순 정렬은 QueryDSL로 처리)
    List<ReviewSession> findByUserId(Long userId);

    // 특정 덱의 세션 목록
    List<ReviewSession> findByDeckId(Long deckId);

    // 특정 사용자 + 특정 덱의 세션 (가장 최근 세션 조회용)
    Optional<ReviewSession> findTopByUserIdAndDeckIdOrderByStartedAtDesc(Long userId, Long deckId);
}