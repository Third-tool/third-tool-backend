package com.example.thirdtool.Card.domain.model;

import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SoftScheduleTemplate {

    public record IntervalStep(Duration minDuration, SoftScheduleState state) {

        public IntervalStep {
            if (minDuration == null || minDuration.isZero() || minDuration.isNegative()) {
                throw new IllegalArgumentException(
                        "IntervalStep: minDuration은 양수여야 합니다. minDuration=" + minDuration);
            }
            if (state == null) {
                throw new IllegalArgumentException("IntervalStep: state는 null일 수 없습니다.");
            }
            if (state == SoftScheduleState.FRESH || state == SoftScheduleState.NOT_YET) {
                throw new IllegalArgumentException(
                        "IntervalStep: FRESH / NOT_YET은 간격 단계로 사용할 수 없습니다. state=" + state);
            }
        }
    }

    // ─── 기본 템플릿 상수 ─────────────────────────────────
    public static final SoftScheduleTemplate DEFAULT = SoftScheduleTemplate.of(List.of(
            new IntervalStep(Duration.ofDays(1), SoftScheduleState.INTERVAL_1D),
            new IntervalStep(Duration.ofDays(3), SoftScheduleState.INTERVAL_3D),
            new IntervalStep(Duration.ofDays(7), SoftScheduleState.INTERVAL_7D)
                                                                                      ));

    private final List<IntervalStep> intervalSteps;

    private SoftScheduleTemplate(List<IntervalStep> intervalSteps) {
        this.intervalSteps = intervalSteps;
    }

    public static SoftScheduleTemplate of(List<IntervalStep> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("SoftScheduleTemplate: intervalSteps는 1개 이상이어야 합니다.");
        }
        List<IntervalStep> sorted = steps.stream()
                                         .sorted(Comparator.comparing(IntervalStep::minDuration))
                                         .toList();
        return new SoftScheduleTemplate(sorted);
    }

    // ─── 핵심 행위 ─────────────────────────────────────────────

    public SoftScheduleState resolveState(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("SoftScheduleTemplate: card는 null일 수 없습니다.");
        }

        // 1. 한 번도 열람되지 않은 카드 → 즉시 노출 가능
        if (card.getLastViewedAt() == null) {
            return SoftScheduleState.FRESH;
        }

        Duration elapsed = Duration.between(card.getLastViewedAt(), java.time.LocalDateTime.now());

        // 2. 최소 간격 미충족 → 노출 불가
        if (elapsed.compareTo(intervalSteps.get(0).minDuration()) < 0) {
            return SoftScheduleState.NOT_YET;
        }

        // 3. 내림차순 순회 — elapsed >= step.minDuration인 가장 높은 단계 반환
        for (int i = intervalSteps.size() - 1; i >= 0; i--) {
            if (elapsed.compareTo(intervalSteps.get(i).minDuration()) >= 0) {
                return intervalSteps.get(i).state();
            }
        }

        // 도달 불가 경로 (방어 코드)
        return SoftScheduleState.NOT_YET;
    }

    public boolean isAvailable(Card card) {
        return resolveState(card).isAvailable();
    }

    public List<IntervalStep> getIntervalSteps() {
        return Collections.unmodifiableList(intervalSteps);
    }
}
