package com.example.thirdtool.Review.presentation;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REV E2 · Story 2-4 — 폐기된 엔드포인트 세트. 410 Gone 응답 강제.
 *
 * <p>PR#1 (Deck 폐기) · PR#2 (Layer 스코프) 궤적을 이 PR에서 완전 supersede.
 * SDD 우선 정책: 프론트엔드 이관 유도용 410. 404보다 명확한 "완전 폐기" 시그널.
 */
@RestController
public class LegacyReviewEndpointsController {

    /** LT-E5 이전 궤적: deck 스코프 startReview. */
    @PostMapping("/api/v1/reviews")
    public ResponseEntity<Map<String, String>> legacyStartReview() {
        return gone("POST /api/v1/reviews", "POST /api/v1/review-sessions");
    }

    @GetMapping("/api/v1/reviews")
    public ResponseEntity<Map<String, String>> legacySearchReviews() {
        return gone("GET /api/v1/reviews", "GET /api/v1/review-sessions");
    }

    @GetMapping("/api/v1/reviews/{sessionId}")
    public ResponseEntity<Map<String, String>> legacyFindReview(@PathVariable Long sessionId) {
        return gone("GET /api/v1/reviews/{id}", "GET /api/v1/review-sessions/{id}");
    }

    @PatchMapping("/api/v1/reviews/{sessionId}/comparing")
    public ResponseEntity<Map<String, String>> legacyComparing(@PathVariable Long sessionId) {
        return gone("PATCH /api/v1/reviews/{id}/comparing",
                    "POST /api/v1/review-sessions/{id}/start-comparing");
    }

    @PatchMapping("/api/v1/reviews/{sessionId}/next")
    public ResponseEntity<Map<String, String>> legacyNext(@PathVariable Long sessionId) {
        return gone("PATCH /api/v1/reviews/{id}/next",
                    "POST /api/v1/review-sessions/{id}/next");
    }

    /** LT-E6 이전 궤적: layer 스코프 세션 시작. */
    @PostMapping("/api/v1/layers/{layerId}/review-sessions")
    public ResponseEntity<Map<String, String>> legacyLayerStartSession(@PathVariable Long layerId) {
        return gone("POST /api/v1/layers/{id}/review-sessions", "POST /api/v1/review-sessions");
    }

    /** SDD 명시 폐기 엔드포인트 · 실제로는 활성화되지 않았지만 client 이관 대응. */
    @PostMapping("/api/v1/decks/{deckId}/review-sessions")
    public ResponseEntity<Map<String, String>> legacyDeckStartSession(@PathVariable Long deckId) {
        return gone("POST /api/v1/decks/{id}/review-sessions", "POST /api/v1/review-sessions");
    }

    /** Story 2-5 · TodayCandidates 엔드포인트는 DailyBatch로 흡수. */
    @GetMapping("/api/v1/review-session/today")
    public ResponseEntity<Map<String, String>> legacyTodayCandidates() {
        return gone("GET /api/v1/review-session/today", "POST /api/v1/daily-batch/today");
    }

    private ResponseEntity<Map<String, String>> gone(String oldPath, String newPath) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(Map.of(
                        "code", "REVIEW_ENDPOINT_GONE",
                        "message", oldPath + " 는 폐기되었습니다. " + newPath + " 를 사용하세요.",
                        "supersededBy", newPath
                ));
    }
}
