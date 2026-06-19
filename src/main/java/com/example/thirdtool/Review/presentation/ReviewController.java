package com.example.thirdtool.Review.presentation;

import com.example.thirdtool.Review.application.ReviewCommandService;
import com.example.thirdtool.Review.application.ReviewQueryService;
import com.example.thirdtool.Review.presentation.dto.ReviewRequest;
import com.example.thirdtool.Review.presentation.dto.ReviewResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class ReviewController {

    private final ReviewCommandService reviewCommandService;
    private final ReviewQueryService reviewQueryService;

    // ─── 1. 리뷰 세션 시작 ───────────────────────────────
    @PostMapping("/api/v1/reviews")
    public ResponseEntity<ReviewResponse.StartSession> startReview(
            @Valid @RequestBody ReviewRequest.StartSession request,
            @AuthenticationPrincipal UserEntity currentUser
                                                                  ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reviewCommandService.startReview(request, currentUser));
    }

    // ─── 2. 세션 단건 조회 ────────────────────────────────
    @GetMapping("/api/v1/reviews/{sessionId}")
    public ResponseEntity<ReviewResponse.SessionDetail> findById(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserEntity currentUser
                                                                ) {
        return ResponseEntity.ok(reviewQueryService.findById(sessionId, currentUser));
    }

    // ─── 3. 현재 카드 COMPARING 전환 ─────────────────────
    @PatchMapping("/api/v1/reviews/{sessionId}/comparing")
    public ResponseEntity<ReviewResponse.CardReviewDto> startComparing(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserEntity currentUser
                                                                      ) {
        return ResponseEntity.ok(reviewCommandService.startComparing(sessionId, currentUser));
    }

    // ─── 4. 다음 카드로 이동 ──────────────────────────────
    @PatchMapping("/api/v1/reviews/{sessionId}/next")
    public ResponseEntity<ReviewResponse.NextCard> moveToNext(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserEntity currentUser
                                                             ) {
        return ResponseEntity.ok(reviewCommandService.moveToNext(sessionId, currentUser));
    }

    // ─── 5. 세션 목록 조회 ────────────────────────────────
    @GetMapping("/api/v1/reviews")
    public ResponseEntity<List<ReviewResponse.SessionSummary>> searchSessions(
            @RequestParam(required = false) Long deckId,
            @AuthenticationPrincipal UserEntity currentUser
                                                                             ) {
        return ResponseEntity.ok(reviewQueryService.searchSessions(deckId, currentUser));
    }

    // ─── 6. 오늘의 학습 후보 (Story 6-1·6-2·6-3) ──────────
    // 사용자의 ON_FIELD + soft schedule 통과 카드를 state별 분류해 반환.
    // target 미입력 시 사용자 dailyTarget 사용. target 입력 시 그 값으로 분배(Story 6-3 "+N장").
    @GetMapping("/api/v1/review-session/today")
    public ResponseEntity<ReviewResponse.TodayCandidates> getTodayCandidates(
            @AuthenticationPrincipal UserEntity currentUser,
            @RequestParam(required = false) @Min(1) Integer target
                                                                            ) {
        ReviewResponse.TodayCandidates response = (target == null)
                ? reviewQueryService.getTodayCandidates(currentUser)
                : reviewQueryService.getTodayCandidatesWithTarget(currentUser, target);
        return ResponseEntity.ok(response);
    }
}
