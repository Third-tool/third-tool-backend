package com.example.thirdtool.UserSchedule.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class UserScheduleRequest {

    public record Save(
            @NotNull(message = "inputDays는 필수입니다.")
            Integer inputDays
    ) {}

    public record HistoryQuery(
            Integer limit
    ) {}

    /**
     * Story 6-2/6-3 — 하루 학습 목표 카드 수 수정 요청.
     */
    public record UpdateDailyTarget(
            @NotNull(message = "dailyTarget은 필수입니다.")
            @Min(value = 1, message = "dailyTarget은 1 이상이어야 합니다.")
            Integer dailyTarget
    ) {}
}