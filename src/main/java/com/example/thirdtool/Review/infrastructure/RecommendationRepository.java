package com.example.thirdtool.Review.infrastructure;

import com.example.thirdtool.Review.domain.model.Recommendation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * RecommendationRepository port — REV E3 · Story 3-2/3-5.
 */
public interface RecommendationRepository {

    Recommendation save(Recommendation recommendation);

    Optional<Recommendation> findById(Long id);

    /** REV E3 · Story 3-3 · 중복 발송 방지 — 사용자·기간 내 최신 추천 조회. */
    Optional<Recommendation> findLatestByUserIdSince(Long userId, LocalDateTime since);

    /** REV E3 · Story 3-1 · 대시보드 미해결 추천 노출. */
    Optional<Recommendation> findLatestUnresolvedByUserId(Long userId);

    List<Recommendation> findAllByUserId(Long userId);
}
