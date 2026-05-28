package com.example.thirdtool.Review.application;

import com.example.thirdtool.Card.domain.model.OnFieldBudget;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewQueryService {

    private final ReviewSessionRepository reviewSessionRepository;
    private final OnFieldBudget systemBudget;

    // Story-5-2: Long userId → UserEntity user 시그니처 통일.

    // ─── 1. 세션 단건 조회 ────────────────────────────────
    public ReviewResponse.SessionDetail findById(Long sessionId, UserEntity user) {
        ReviewSession session = getSessionByOwner(sessionId, user);
        boolean isLastView = resolveIsLastView(session);
        return ReviewResponse.SessionDetail.of(session, isLastView);
    }


    // ─── 2. 세션 목록 조회 ────────────────────────────────
    /**
     * 세션 목록 조회.
     * deckId 지정 시 해당 덱의 세션만 반환, 미지정 시 전체 반환.
     * startedAt 내림차순 정렬은 QueryDSL에서 처리한다.
     */
    public List<ReviewResponse.SessionSummary> searchSessions(Long deckId, UserEntity user) {
        ReviewSessionSearchCondition condition = ReviewSessionSearchCondition.builder()
                                                                             .userId(user.getId())
                                                                             .deckId(deckId)
                                                                             .build();

        return reviewSessionRepository.searchSessions(condition)
                                      .stream()
                                      .map(ReviewResponse.SessionSummary::of)
                                      .toList();
    }

    // ─── 내부 공용 메서드 ─────────────────────────────────
    public ReviewSession getSessionByOwner(Long sessionId, UserEntity user) {
        // 세션 존재 여부: 없으면 REVIEW001
        ReviewSession session = reviewSessionRepository.findById(sessionId)
                                                       .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_SESSION_NOT_FOUND));

        // 소유자 검증: 본인 세션이 아니면 REVIEW005
        if (!session.isOwner(user.getId())) {
            throw new BusinessException(ErrorCode.REVIEW_SESSION_FORBIDDEN);
        }

        return session;
    }

    private boolean resolveIsLastView(ReviewSession session) {
        if (session.isFinished()) return false;
        return session.currentCardReview().getCard().isLastView(systemBudget.getMaxView());
    }
}