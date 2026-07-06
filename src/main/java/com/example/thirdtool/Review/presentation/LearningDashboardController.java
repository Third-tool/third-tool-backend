package com.example.thirdtool.Review.presentation;

import com.example.thirdtool.Review.application.service.LearningDashboardCommandService;
import com.example.thirdtool.Review.application.service.LearningDashboardQueryService;
import com.example.thirdtool.Review.presentation.dto.LearningDashboardResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REV E3 · Story 3-4 · LearningDashboard API.
 */
@RestController
@RequestMapping("/api/v1/learning-dashboard")
@RequiredArgsConstructor
public class LearningDashboardController {

    private final LearningDashboardQueryService queryService;
    private final LearningDashboardCommandService commandService;

    /** GET /api/v1/learning-dashboard — 오늘/7일/30일/streak/recommendation 통합 조회. */
    @GetMapping
    public ResponseEntity<LearningDashboardResponse> getDashboard(
            @AuthenticationPrincipal UserEntity currentUser) {
        LearningDashboardResponse response = queryService.buildDashboard(
                currentUser.getId(), LocalDate.now());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/learning-dashboard/recommendations/{id}/accept — 추천 수락 (mode 변경 트리거).
     */
    @PostMapping("/recommendations/{recommendationId}/accept")
    public ResponseEntity<Void> accept(
            @PathVariable Long recommendationId,
            @AuthenticationPrincipal UserEntity currentUser) {
        commandService.acceptRecommendation(currentUser.getId(), recommendationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/learning-dashboard/recommendations/{id}/dismiss — 추천 거절.
     */
    @PostMapping("/recommendations/{recommendationId}/dismiss")
    public ResponseEntity<Void> dismiss(
            @PathVariable Long recommendationId,
            @AuthenticationPrincipal UserEntity currentUser) {
        commandService.dismissRecommendation(currentUser.getId(), recommendationId);
        return ResponseEntity.noContent().build();
    }
}
