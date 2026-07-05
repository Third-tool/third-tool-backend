package com.example.thirdtool.Review.presentation;

import com.example.thirdtool.Review.application.service.DailyLearningBatchService;
import com.example.thirdtool.Review.domain.model.DailyLearningBatch;
import com.example.thirdtool.Review.presentation.dto.DailyBatchResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REV E1 · M5 · Story 1-7 — DailyBatch API.
 */
@RestController
@RequestMapping("/api/v1/daily-batch")
@RequiredArgsConstructor
public class DailyBatchController {

    private final DailyLearningBatchService service;

    /**
     * POST /api/v1/daily-batch/today — 오늘 batch 조회 · 없으면 lazy 생성.
     * 첫 호출 시 201 · 재호출 시 200 (SDD Story 1-7).
     * SDD 정합 위해 항상 201로 통일 (단순화 · lazy vs cache 구분은 응답 필드 아님).
     */
    @PostMapping("/today")
    public ResponseEntity<DailyBatchResponse.Detail> getOrCreateToday(
            @AuthenticationPrincipal UserEntity currentUser) {
        DailyLearningBatch batch = service.getOrCreateToday(currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DailyBatchResponse.Detail.of(batch));
    }

    /**
     * GET /api/v1/daily-batch/history?from=YYYY-MM-DD&to=YYYY-MM-DD — 이력 조회.
     */
    @GetMapping("/history")
    public ResponseEntity<List<DailyBatchResponse.HistorySummary>> queryHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserEntity currentUser) {
        List<DailyLearningBatch> history = service.queryHistory(currentUser.getId(), from, to);
        List<DailyBatchResponse.HistorySummary> body =
                history.stream().map(DailyBatchResponse.HistorySummary::of).toList();
        return ResponseEntity.ok(body);
    }

    /**
     * GET /api/v1/daily-batch/{batchId} — batch 상세 조회.
     */
    @GetMapping("/{batchId}")
    public ResponseEntity<DailyBatchResponse.Detail> findById(
            @PathVariable Long batchId,
            @AuthenticationPrincipal UserEntity currentUser) {
        DailyLearningBatch batch = service.findByIdOwned(batchId, currentUser.getId());
        return ResponseEntity.ok(DailyBatchResponse.Detail.of(batch));
    }
}
