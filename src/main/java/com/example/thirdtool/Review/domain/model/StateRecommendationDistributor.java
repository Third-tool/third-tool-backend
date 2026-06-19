package com.example.thirdtool.Review.domain.model;

import com.example.thirdtool.Card.domain.model.SoftScheduleState;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * product-card.md Epic 6 Story 6-2 — 동적 비율 추천 분배기.
 *
 * <p>오늘 수집된 state별 카드 수에 비례하여 사용자 {@code dailyTarget}을 배분한다.
 * Alternative B(Spec) — 카드 풀이 비어 있는 state는 자동 0장, 풀이 적으면 풀 크기까지만.
 *
 * <p>알고리즘 — Largest Remainder Method (LRM)
 * <ol>
 *   <li>각 state share = pool_size × target / total_pool (실수)</li>
 *   <li>floor 적용해 base allocation 산출</li>
 *   <li>잔여 = target - sum(base)</li>
 *   <li>분수 부분 큰 state 순으로 잔여를 1씩 추가 — 단 각 state의 풀 크기를 초과하지 않음</li>
 * </ol>
 *
 * <p>경계
 * <ul>
 *   <li>total_pool = 0 → 빈 분배</li>
 *   <li>dailyTarget &le; 0 → 빈 분배 (방어, 도메인은 1 이상 강제)</li>
 *   <li>dailyTarget &gt; total_pool → 풀 전체 분배</li>
 * </ul>
 */
@Component
public class StateRecommendationDistributor {

    public Map<SoftScheduleState, Integer> distribute(
            Map<SoftScheduleState, Integer> poolSizes,
            int dailyTarget
                                                     ) {
        int totalPool = poolSizes.values().stream().mapToInt(Integer::intValue).sum();
        if (totalPool == 0 || dailyTarget <= 0) {
            return new EnumMap<>(SoftScheduleState.class);
        }
        int target = Math.min(dailyTarget, totalPool);

        Map<SoftScheduleState, Integer> allocation  = new EnumMap<>(SoftScheduleState.class);
        Map<SoftScheduleState, Double>  fractionals = new EnumMap<>(SoftScheduleState.class);
        int allocated = 0;

        for (var entry : poolSizes.entrySet()) {
            double exact = (double) entry.getValue() * target / totalPool;
            int floor = (int) Math.floor(exact);
            allocation.put(entry.getKey(), floor);
            fractionals.put(entry.getKey(), exact - floor);
            allocated += floor;
        }

        int remaining = target - allocated;
        if (remaining > 0) {
            // 분수 부분 큰 순서로 +1, 풀 cap 준수.
            List<Map.Entry<SoftScheduleState, Double>> sortedByFraction = fractionals.entrySet().stream()
                    .sorted(Map.Entry.<SoftScheduleState, Double>comparingByValue().reversed())
                    .toList();

            for (Map.Entry<SoftScheduleState, Double> entry : sortedByFraction) {
                if (remaining <= 0) break;
                SoftScheduleState state = entry.getKey();
                int current = allocation.get(state);
                int cap     = poolSizes.get(state);
                if (current < cap) {
                    allocation.put(state, current + 1);
                    remaining--;
                }
            }
        }

        // 0 allocation은 응답에서 제외 — 의미 있는 state만 키로 유지.
        allocation.entrySet().removeIf(e -> e.getValue() == 0);
        return allocation;
    }
}
