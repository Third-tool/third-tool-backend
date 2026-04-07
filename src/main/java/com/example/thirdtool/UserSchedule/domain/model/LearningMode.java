package com.example.thirdtool.UserSchedule.domain.model;

import com.example.thirdtool.Card.domain.model.OnFieldBudget;
import com.example.thirdtool.Card.domain.model.SoftScheduleState;
import com.example.thirdtool.Card.domain.model.SoftScheduleTemplate;

import java.time.Duration;
import java.util.List;

public enum LearningMode {

    MODE_10D(10, 3, "10일 모드", List.of(
            new SoftScheduleTemplate.IntervalStep(Duration.ofDays(1), SoftScheduleState.INTERVAL_1D),
            new SoftScheduleTemplate.IntervalStep(Duration.ofDays(3), SoftScheduleState.INTERVAL_3D),
            new SoftScheduleTemplate.IntervalStep(Duration.ofDays(7), SoftScheduleState.INTERVAL_7D)
                                     )),

    MODE_20D(20, 5, "20일 모드", List.of(
            new SoftScheduleTemplate.IntervalStep(Duration.ofDays(1),  SoftScheduleState.INTERVAL_1D),
            new SoftScheduleTemplate.IntervalStep(Duration.ofDays(3),  SoftScheduleState.INTERVAL_3D),
            new SoftScheduleTemplate.IntervalStep(Duration.ofDays(7),  SoftScheduleState.INTERVAL_7D),
            new SoftScheduleTemplate.IntervalStep(Duration.ofDays(14), SoftScheduleState.INTERVAL_14D)
                                     )),

    MODE_30D(30, 7, "30일 모드", List.of(
            new SoftScheduleTemplate.IntervalStep(Duration.ofDays(1),  SoftScheduleState.INTERVAL_1D),
            new SoftScheduleTemplate.IntervalStep(Duration.ofDays(3),  SoftScheduleState.INTERVAL_3D),
            new SoftScheduleTemplate.IntervalStep(Duration.ofDays(7),  SoftScheduleState.INTERVAL_7D),
            new SoftScheduleTemplate.IntervalStep(Duration.ofDays(14), SoftScheduleState.INTERVAL_14D),
            new SoftScheduleTemplate.IntervalStep(Duration.ofDays(21), SoftScheduleState.INTERVAL_21D)
                                     ));

    // ─── 모드 속성 ────────────────────────────────────────────────

    private final int durationDays;
    private final int maxView;
    private final String displayName;

    private final List<SoftScheduleTemplate.IntervalStep> intervalSteps;

    LearningMode(
            int durationDays,
            int maxView,
            String displayName,
            List<SoftScheduleTemplate.IntervalStep> intervalSteps
                ) {
        this.durationDays  = durationDays;
        this.maxView       = maxView;
        this.displayName   = displayName;
        this.intervalSteps = intervalSteps;
    }

    // ─── 행위 ─────────────────────────────────────────────────────

    public OnFieldBudget toOnFieldBudget() {
        return OnFieldBudget.of(maxView, Duration.ofDays(durationDays));
    }

    public SoftScheduleTemplate toSoftScheduleTemplate() {
        return SoftScheduleTemplate.of(intervalSteps);
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDurationDays() { return durationDays; }
    public int getMaxView()      { return maxView; }
}