package com.example.thirdtool.UserSchedule.presentation;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.UserSchedule.application.service.UserScheduleCommandService;
import com.example.thirdtool.UserSchedule.application.service.UserScheduleQueryService;
import com.example.thirdtool.UserSchedule.domain.exception.UserScheduleDomainException;
import com.example.thirdtool.UserSchedule.presentation.dto.UserScheduleRequest;
import com.example.thirdtool.UserSchedule.presentation.dto.UserScheduleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/schedule")
public class UserScheduleController {

    private final UserScheduleCommandService commandService;
    private final UserScheduleQueryService   queryService;

    // ─── 1. 현재 설정 조회 ───────────────────────────────────────

    @GetMapping
    public ResponseEntity<UserScheduleResponse.Get> getSchedule(
            @RequestHeader("X-User-Id") Long userId
                                                               ) {
        return ResponseEntity.ok(queryService.getSchedule(userId));
    }

    // ─── 2. 설정 저장 (최초 생성 또는 변경) ──────────────────────
    @PutMapping
    public ResponseEntity<UserScheduleResponse.Save> saveSchedule(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UserScheduleRequest.Save request
                                                                 ) {
        validateInputDays(request.inputDays());
        return ResponseEntity.ok(commandService.save(userId, request.inputDays()));
    }

    // ─── 3. 설정 변경 이력 조회 ──────────────────────────────────
    @GetMapping("/history")
    public ResponseEntity<List<UserScheduleResponse.HistoryItem>> getHistory(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) Integer limit
                                                                            ) {
        return ResponseEntity.ok(queryService.getHistory(userId, limit));
    }

    // ─── 내부 유틸 ───────────────────────────────────────────────

    private void validateInputDays(int inputDays) {
        if (inputDays < 1) {
            throw UserScheduleDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "inputDays는 1 이상의 숫자를 입력해주세요.");
        }
    }
}