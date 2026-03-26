package com.example.thirdtool.Review.infrastructure;

import com.example.thirdtool.Review.domain.model.ReviewSession;
import com.example.thirdtool.Review.infrastructure.dto.ReviewSessionSearchCondition;
import com.example.thirdtool.Review.infrastructure.dto.ReviewSessionSummaryRow;

import java.util.List;
import java.util.Optional;

public interface ReviewSessionRepositoryCustom {

    /**
     * 사용자 세션 목록을 조건에 따라 동적으로 조회한다.
     * 목록 응답에는 요약 정보(Row)만 포함한다.
     */
    List<ReviewSessionSummaryRow> searchSessions(ReviewSessionSearchCondition condition);

    /**
     * 세션 ID와 사용자 ID로 세션을 조회한다.
     * 본인 세션이 아니면 빈 Optional을 반환한다.
     */
    Optional<ReviewSession> findByIdAndUserId(Long sessionId, Long userId);
}