package com.example.thirdtool.Review.application;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Review.domain.model.ReviewSession;
import com.example.thirdtool.Review.infrastructure.ReviewSessionRepository;
import com.example.thirdtool.Review.infrastructure.dto.ReviewSessionSearchCondition;
import com.example.thirdtool.Review.presentation.dto.ReviewResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ReviewQueryService — REV E2 · Story 2-4/2-5 재편.
 *
 * <p>PR#2 궤적 TodayCandidates 흐름은 DailyBatch 엔드포인트로 흡수 폐기.
 * StateRecommendationDistributor 삭제와 함께 관련 필드·메서드도 제거.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewQueryService {

    private final ReviewSessionRepository reviewSessionRepository;

    // ─── 1. 세션 단건 조회 ────────────────────────────────
    public ReviewResponse.SessionDetail findById(Long sessionId, UserEntity user) {
        ReviewSession session = getSessionByOwner(sessionId, user);
        return ReviewResponse.SessionDetail.of(session);
    }


    // ─── 2. 세션 목록 조회 ────────────────────────────────
    /**
     * REV E2 · Story 2-6 — deck 필터 폐기 · batch 필터로 대체.
     * batchId 미입력 시 사용자 전체 세션 반환 (startedAt DESC).
     */
    public List<ReviewResponse.SessionSummary> searchSessions(Long batchId, UserEntity user) {
        ReviewSessionSearchCondition condition = ReviewSessionSearchCondition.builder()
                                                                             .userId(user.getId())
                                                                             .batchId(batchId)
                                                                             .build();

        return reviewSessionRepository.searchSessions(condition)
                                      .stream()
                                      .map(ReviewResponse.SessionSummary::of)
                                      .toList();
    }

    // ─── 내부 공용 메서드 ─────────────────────────────────
    public ReviewSession getSessionByOwner(Long sessionId, UserEntity user) {
        ReviewSession session = reviewSessionRepository.findById(sessionId)
                                                       .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_SESSION_NOT_FOUND));

        if (!session.isOwner(user.getId())) {
            throw new BusinessException(ErrorCode.REVIEW_SESSION_FORBIDDEN);
        }

        return session;
    }
}
