package com.example.thirdtool.Review.presentation;

import com.example.thirdtool.Review.application.ReviewCommandService;
import com.example.thirdtool.Review.application.ReviewQueryService;
import com.example.thirdtool.Review.presentation.dto.ReviewRequest;
import com.example.thirdtool.Review.presentation.dto.ReviewResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewCommandService reviewCommandService;
    private final ReviewQueryService reviewQueryService;

    // ─── 1. 리뷰 세션 시작 ───────────────────────────────
    @PostMapping("/api/v1/reviews")
    public ResponseEntity<ReviewResponse.StartSession> startReview(
            @Valid @RequestBody ReviewRequest.StartSession request,
            @AuthenticationPrincipal Long userId
                                                                  ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reviewCommandService.startReview(request, userId));
    }

    // ─── 2. 세션 단건 조회 ────────────────────────────────
    @GetMapping("/api/v1/reviews/{sessionId}")
    public ResponseEntity<ReviewResponse.SessionDetail> findById(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Long userId
                                                                ) {
        return ResponseEntity.ok(reviewQueryService.findById(sessionId, userId));
    }

    // ─── 3. 현재 카드 COMPARING 전환 ─────────────────────
    @PatchMapping("/api/v1/reviews/{sessionId}/comparing")
    public ResponseEntity<ReviewResponse.CardReviewDto> startComparing(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Long userId
                                                                      ) {
        return ResponseEntity.ok(reviewCommandService.startComparing(sessionId, userId));
    }

    // ─── 4. 다음 카드로 이동 ──────────────────────────────
    @PatchMapping("/api/v1/reviews/{sessionId}/next")
    public ResponseEntity<ReviewResponse.NextCard> moveToNext(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Long userId
                                                             ) {
        return ResponseEntity.ok(reviewCommandService.moveToNext(sessionId, userId));
    }

    // ─── 5. 세션 목록 조회 ────────────────────────────────
    @GetMapping("/api/v1/reviews")
    public ResponseEntity<List<ReviewResponse.SessionSummary>> searchSessions(
            @RequestParam(required = false) Long deckId,
            @AuthenticationPrincipal Long userId
                                                                             ) {
        return ResponseEntity.ok(reviewQueryService.searchSessions(deckId, userId));
    }
}

