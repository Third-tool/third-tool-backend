package com.example.thirdtool.UserSchedule.domain.model;

import com.example.thirdtool.Card.domain.model.SoftScheduleState;
import com.example.thirdtool.Card.domain.model.SoftScheduleTemplate;

import java.time.Duration;
import java.util.List;

public enum LearningMode {

    MODE_7D(List.of(1, 3, 7), "7일 모드"),
    MODE_14D(List.of(1, 3, 7, 14), "14일 모드"),
    MODE_28D(List.of(1, 3, 7, 14, 28), "28일 모드"),
    MODE_60D(List.of(1, 3, 7, 14, 28, 60), "60일 모드");

    private final List<Integer> intervals;
    private final String displayName;

    LearningMode(List<Integer> intervals, String displayName) {
        this.intervals = List.copyOf(intervals);
        this.displayName = displayName;
    }

    public List<Integer> getIntervals() {
        return intervals;
    }

    public int maxDays() {
        return intervals.get(intervals.size() - 1);
    }

    public int stepCount() {
        return intervals.size();
    }

    public String getDisplayName() {
        return displayName;
    }

    public SoftScheduleTemplate toSoftScheduleTemplate() {
        List<SoftScheduleTemplate.IntervalStep> steps = intervals.stream()
                .map(day -> new SoftScheduleTemplate.IntervalStep(
                        Duration.ofDays(day),
                        mapDayToState(day)
                ))
                .toList();
        return SoftScheduleTemplate.of(steps);
    }

    private static SoftScheduleState mapDayToState(int day) {
        return switch (day) {
            case 1 -> SoftScheduleState.INTERVAL_1D;
            case 3 -> SoftScheduleState.INTERVAL_3D;
            case 7 -> SoftScheduleState.INTERVAL_7D;
            case 14 -> SoftScheduleState.INTERVAL_14D;
            case 28 -> SoftScheduleState.INTERVAL_28D;
            case 60 -> SoftScheduleState.INTERVAL_60D;
            default -> throw new IllegalStateException(
                    "LearningMode: 알 수 없는 인터벌 일수. day=" + day);
        };
    }
}
