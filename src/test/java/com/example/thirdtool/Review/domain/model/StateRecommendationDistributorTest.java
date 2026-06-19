package com.example.thirdtool.Review.domain.model;

import com.example.thirdtool.Card.domain.model.SoftScheduleState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * StateRecommendationDistributor — Story 6-2 분배 알고리즘.
 *
 * <p>Pure function 단위 테스트 — 입력 풀·dailyTarget → 출력 분배 결과 검증.
 */
@DisplayName("StateRecommendationDistributor — Story 6-2")
class StateRecommendationDistributorTest {

    private final StateRecommendationDistributor distributor = new StateRecommendationDistributor();

    private Map<SoftScheduleState, Integer> pool(int fresh, int oneD, int sevenD) {
        Map<SoftScheduleState, Integer> m = new EnumMap<>(SoftScheduleState.class);
        if (fresh > 0)  m.put(SoftScheduleState.FRESH, fresh);
        if (oneD > 0)   m.put(SoftScheduleState.INTERVAL_1D, oneD);
        if (sevenD > 0) m.put(SoftScheduleState.INTERVAL_7D, sevenD);
        return m;
    }

    @Test
    @DisplayName("정확한 비례 — 풀 10·10·10 + 6 → 2·2·2")
    void exactProportional() {
        Map<SoftScheduleState, Integer> result = distributor.distribute(pool(10, 10, 10), 6);

        assertThat(result).containsOnly(
                entry(SoftScheduleState.FRESH, 2),
                entry(SoftScheduleState.INTERVAL_1D, 2),
                entry(SoftScheduleState.INTERVAL_7D, 2)
        );
    }

    @Test
    @DisplayName("largest remainder — 풀 3·3·4 + 5 → 1·1·2 (4쪽 분수 큼 +1)")
    void largestRemainderBoosts() {
        Map<SoftScheduleState, Integer> result = distributor.distribute(pool(3, 3, 4), 5);

        // FRESH/1D: 3*5/10 = 1.5, 7D: 4*5/10 = 2.0
        // base: 1, 1, 2 (sum=4, remaining=1) → 분수 가장 큰 FRESH/1D 중 하나에 +1 (둘 다 0.5)
        // EnumMap 순회 순서가 결정적: FRESH 먼저 → FRESH=2
        assertThat(result.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(5);
        assertThat(result.get(SoftScheduleState.INTERVAL_7D)).isEqualTo(2);
    }

    @Test
    @DisplayName("풀 부족 — 풀 2·1 + dailyTarget 10 → 풀 전체 2·1 분배")
    void poolSmallerThanTarget() {
        Map<SoftScheduleState, Integer> result = distributor.distribute(pool(2, 1, 0), 10);

        assertThat(result).containsOnly(
                entry(SoftScheduleState.FRESH, 2),
                entry(SoftScheduleState.INTERVAL_1D, 1)
        );
    }

    @Test
    @DisplayName("특정 state 풀 0 — 빈 state는 0장 분배 (응답에서 제외)")
    void zeroPoolStateExcluded() {
        Map<SoftScheduleState, Integer> result = distributor.distribute(pool(5, 0, 5), 4);

        assertThat(result).containsOnly(
                entry(SoftScheduleState.FRESH, 2),
                entry(SoftScheduleState.INTERVAL_7D, 2)
        );
        assertThat(result).doesNotContainKey(SoftScheduleState.INTERVAL_1D);
    }

    @Test
    @DisplayName("총 풀 0 → 빈 분배")
    void emptyPool() {
        Map<SoftScheduleState, Integer> result = distributor.distribute(new EnumMap<>(SoftScheduleState.class), 20);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("dailyTarget 0 또는 음수 → 빈 분배 (방어)")
    void nonPositiveTarget() {
        assertThat(distributor.distribute(pool(5, 5, 0), 0)).isEmpty();
        assertThat(distributor.distribute(pool(5, 5, 0), -3)).isEmpty();
    }

    @Test
    @DisplayName("불균형 풀 — 100·1·1 + 10 → FRESH 10 (분수 압도적, 0 state는 응답 제외)")
    void unbalancedPool_dominantStateAbsorbsAll() {
        Map<SoftScheduleState, Integer> result = distributor.distribute(pool(100, 1, 1), 10);

        // FRESH: 100*10/102 ≈ 9.80 (floor 9, fraction 0.80) — 분수 최대
        // 1D/7D: 1*10/102 ≈ 0.098 (floor 0) — 분수 작음
        // base=9, remaining=1 → 분수 큰 FRESH +1 → FRESH=10
        // 0 allocation은 응답에서 제외 → FRESH만 남음
        assertThat(result.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(10);
        assertThat(result).containsOnly(entry(SoftScheduleState.FRESH, 10));
    }
}
