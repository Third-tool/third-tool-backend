package com.example.thirdtool.UserSchedule.presentation.dto;

import jakarta.validation.constraints.NotNull;

public class UserScheduleRequest {

    public record Save(
            @NotNull(message = "inputDays는 필수입니다.")
            Integer inputDays
    ) {}

    public record HistoryQuery(
            Integer limit
    ) {}
}