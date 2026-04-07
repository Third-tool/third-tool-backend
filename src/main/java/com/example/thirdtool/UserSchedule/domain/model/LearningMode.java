package com.example.thirdtool.UserSchedule.domain.model;

import com.example.thirdtool.Card.domain.model.OnFieldBudget;
import com.example.thirdtool.Card.domain.model.SoftScheduleState;
import com.example.thirdtool.Card.domain.model.SoftScheduleTemplate;

import java.time.Duration;
import java.util.List;

public enum LearningMode {

    MODE_10D,
    MODE_20D,
    MODE_30D;

    public OnFieldBudget toOnFieldBudget() {
        return switch (this) {
            case MODE_10D -> OnFieldBudget.of(3, Duration.ofDays(10));
            case MODE_20D -> OnFieldBudget.of(5, Duration.ofDays(20));
            case MODE_30D -> OnFieldBudget.of(7, Duration.ofDays(30));
        };
    }

    public SoftScheduleTemplate toSoftScheduleTemplate() {
        return switch (this) {
            case MODE_10D -> SoftScheduleTemplate.of(List.of(
                    new SoftScheduleTemplate.IntervalStep(Duration.ofDays(1),  SoftScheduleState.INTERVAL_1D),
                    new SoftScheduleTemplate.IntervalStep(Duration.ofDays(3),  SoftScheduleState.INTERVAL_3D),
                    new SoftScheduleTemplate.IntervalStep(Duration.ofDays(7),  SoftScheduleState.INTERVAL_7D)
                                                            ));
            case MODE_20D -> SoftScheduleTemplate.of(List.of(
                    new SoftScheduleTemplate.IntervalStep(Duration.ofDays(1),  SoftScheduleState.INTERVAL_1D),
                    new SoftScheduleTemplate.IntervalStep(Duration.ofDays(3),  SoftScheduleState.INTERVAL_3D),
                    new SoftScheduleTemplate.IntervalStep(Duration.ofDays(7),  SoftScheduleState.INTERVAL_7D),
                    new SoftScheduleTemplate.IntervalStep(Duration.ofDays(14), SoftScheduleState.INTERVAL_14D)
                                                            ));
            case MODE_30D -> SoftScheduleTemplate.of(List.of(
                    new SoftScheduleTemplate.IntervalStep(Duration.ofDays(1),  SoftScheduleState.INTERVAL_1D),
                    new SoftScheduleTemplate.IntervalStep(Duration.ofDays(3),  SoftScheduleState.INTERVAL_3D),
                    new SoftScheduleTemplate.IntervalStep(Duration.ofDays(7),  SoftScheduleState.INTERVAL_7D),
                    new SoftScheduleTemplate.IntervalStep(Duration.ofDays(14), SoftScheduleState.INTERVAL_14D),
                    new SoftScheduleTemplate.IntervalStep(Duration.ofDays(21), SoftScheduleState.INTERVAL_21D)
                                                            ));
        };
    }

    /**
     * 유저에게 표시할 모드 이름을 반환한다.
     *
     * <p>default 분기 없음 — 새 모드 추가 시 반드시 케이스를 추가해야 컴파일된다.
     */
    public String getDisplayName() {
        return switch (this) {
            case MODE_10D -> "10일 모드";
            case MODE_20D -> "20일 모드";
            case MODE_30D -> "30일 모드";
        };
    }
}
