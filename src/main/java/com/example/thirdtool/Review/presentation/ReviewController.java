package com.example.thirdtool.Review.presentation;

import com.example.thirdtool.Review.application.ReviewCommandService;
import com.example.thirdtool.Review.application.ReviewQueryService;
import com.example.thirdtool.Review.presentation.dto.ReviewResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REV E2 · Story 2-4 — 신규 review-session 엔드포인트 세트.
 *
 * <p>DailyLearningBatch (E1) 원천으로 시작하는 세션. deck/axis/layer 스코프 개념 폐기.
 */
@RestController
@RequestMapping("/api/v1/review-sessions")
@RequiredArgsConstructor
@Validated
public class ReviewController {

    private final ReviewCommandService reviewCommandService;
    private final ReviewQueryService reviewQueryService;

    /**
     * POST /api/v1/review-sessions — 신 세션 시작.
     *
     * <p>진행 중 세션 있으면 자동 finish (Story 2-3). 오늘 batch에서 미완료 카드로 세션 구성.
     */
    @PostMapping
    public ResponseEntity<ReviewResponse.StartSession> startSession(
            @AuthenticationPrincipal UserEntity currentUser
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reviewCommandService.startSession(currentUser));
    }

    /** GET /api/v1/review-sessions/{id} — 세션 단건 조회. */
    @GetMapping("/{sessionId}")
    public ResponseEntity<ReviewResponse.SessionDetail> findById(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserEntity currentUser
    ) {
        return ResponseEntity.ok(reviewQueryService.findById(sessionId, currentUser));
    }

    /** POST /api/v1/review-sessions/{id}/start-comparing — 현재 카드 COMPARING 전환. */
    @PostMapping("/{sessionId}/start-comparing")
    public ResponseEntity<ReviewResponse.CardReviewDto> startComparing(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserEntity currentUser
    ) {
        return ResponseEntity.ok(reviewCommandService.startComparing(sessionId, currentUser));
    }

    /** POST /api/v1/review-sessions/{id}/record-view — 현재 카드 view 기록 (batch 동기화). */
    @PostMapping("/{sessionId}/record-view")
    public ResponseEntity<Void> recordView(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserEntity currentUser
    ) {
        reviewCommandService.recordView(sessionId, currentUser);
        return ResponseEntity.noContent().build();
    }

    /** POST /api/v1/review-sessions/{id}/next — 다음 카드로 이동. */
    @PostMapping("/{sessionId}/next")
    public ResponseEntity<ReviewResponse.NextCard> moveToNext(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserEntity currentUser
    ) {
        return ResponseEntity.ok(reviewCommandService.moveToNext(sessionId, currentUser));
    }

    /** POST /api/v1/review-sessions/{id}/finish — 세션 명시 finish. */
    @PostMapping("/{sessionId}/finish")
    public ResponseEntity<ReviewResponse.FinishSession> finish(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserEntity currentUser
    ) {
        return ResponseEntity.ok(reviewCommandService.finish(sessionId, currentUser));
    }

    /**
     * GET /api/v1/review-sessions?batchId= — 세션 목록 조회.
     * batchId 미입력 시 사용자 전체 세션 반환 (startedAt DESC).
     */
    @GetMapping
    public ResponseEntity<List<ReviewResponse.SessionSummary>> searchSessions(
            @RequestParam(required = false) Long batchId,
            @AuthenticationPrincipal UserEntity currentUser
    ) {
        return ResponseEntity.ok(reviewQueryService.searchSessions(batchId, currentUser));
    }
}
