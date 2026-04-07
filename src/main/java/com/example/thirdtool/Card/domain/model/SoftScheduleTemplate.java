package com.example.thirdtool.Card.domain.model;

import java.time.Duration;
import java.time.LocalDateTime;
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

    public static final SoftScheduleTemplate DEFAULT = SoftScheduleTemplate.of(List.of(
            new IntervalStep(Duration.ofDays(1), SoftScheduleState.INTERVAL_1D),
            new IntervalStep(Duration.ofDays(3), SoftScheduleState.INTERVAL_3D),
            new IntervalStep(Duration.ofDays(7), SoftScheduleState.INTERVAL_7D)
                                                                                      ));

    // ─── 상태 ──────────────────────────────────────────────────────

    private final List<IntervalStep> intervalSteps;

    private SoftScheduleTemplate(List<IntervalStep> intervalSteps) {
        this.intervalSteps = intervalSteps;
    }

    // ─── 생성 ──────────────────────────────────────────────────────

    /**
     * 간격 단계 목록으로 템플릿을 생성한다.
     * 내부에서 minDuration 기준 오름차순으로 정렬한다.
     *
     * @param steps 1개 이상의 IntervalStep 목록
     */
    public static SoftScheduleTemplate of(List<IntervalStep> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException(
                    "SoftScheduleTemplate: intervalSteps는 1개 이상이어야 합니다.");
        }
        List<IntervalStep> sorted = steps.stream()
                                         .sorted(Comparator.comparing(IntervalStep::minDuration))
                                         .toList();
        return new SoftScheduleTemplate(sorted);
    }

    // ─── 핵심 행위 ─────────────────────────────────────────────────

    /**
     * 카드의 현재 간격 단계 상태를 계산한다.
     *
     * <ul>
     *   <li>lastViewedAt == null → {@link SoftScheduleState#FRESH}
     *   <li>elapsed < 최소 간격   → {@link SoftScheduleState#NOT_YET}
     *   <li>그 외                 → elapsed 기준으로 도달한 가장 높은 단계
     * </ul>
     *
     * <p>ARCHIVE 상태 카드에 대해서도 호출 가능하다. 상태 필터링은 호출자 책임이다.
     */
    public SoftScheduleState resolveState(Card card) {
        if (card == null) {
            throw new IllegalArgumentException(
                    "SoftScheduleTemplate: card는 null일 수 없습니다.");
        }

        // 1. 한 번도 열람되지 않은 카드 → 즉시 노출 가능
        if (card.getLastViewedAt() == null) {
            return SoftScheduleState.FRESH;
        }

        Duration elapsed = Duration.between(card.getLastViewedAt(), LocalDateTime.now());

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

    /**
     * 노출 후보 포함 여부만 필요한 호출자를 위한 편의 메서드.
     * {@code resolveState(card) != NOT_YET}에 위임한다.
     */
    public boolean isAvailable(Card card) {
        return resolveState(card).isAvailable();
    }

    public List<IntervalStep> getIntervalSteps() {
        return Collections.unmodifiableList(intervalSteps);
    }
}