package com.example.thirdtool.Review.infrastructure;


import com.example.thirdtool.Review.domain.model.ReviewScope;
import com.example.thirdtool.Review.domain.model.ReviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewSessionRepository
        extends JpaRepository<ReviewSession, Long>, ReviewSessionRepositoryCustom {

    // 특정 사용자의 전체 세션 목록 (최신순 정렬은 QueryDSL로 처리)
    List<ReviewSession> findByUserId(Long userId);

    /**
     * @deprecated LT-E5-S5-3 (M5) · Deck 폐기 대기. axis 스코프 조회로 이관.
     */
    @Deprecated
    List<ReviewSession> findByDeckId(Long deckId);

    /**
     * @deprecated LT-E5-S5-3 (M5) · Deck 폐기 대기.
     */
    @Deprecated
    Optional<ReviewSession> findTopByUserIdAndDeckIdOrderByStartedAtDesc(Long userId, Long deckId);

    /**
     * LT-E6-S6-2 (M5) — AXIS 스코프 세션 조회.
     * Spring Data 명명 규칙: WHERE scope = ? AND scopeId = ?
     */
    List<ReviewSession> findAllByScopeAndScopeId(ReviewScope scope, Long scopeId);

    /**
     * LT-E6-S6-2 (M5) — AXIS 스코프 편의 메서드.
     */
    default List<ReviewSession> findAllByAxisId(Long axisId) {
        return findAllByScopeAndScopeId(ReviewScope.AXIS, axisId);
    }

    /**
     * LT-E6-S6-2 (M5) — LAYER 스코프 편의 메서드.
     */
    default List<ReviewSession> findAllByLayerId(Long layerId) {
        return findAllByScopeAndScopeId(ReviewScope.LAYER, layerId);
    }
}