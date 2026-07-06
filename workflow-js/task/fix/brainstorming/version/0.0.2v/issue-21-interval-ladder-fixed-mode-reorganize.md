# Issue: Interval ladder 고정(1,3,7,14,28,60) + Mode enum 재편(MODE_7D/14D/28D/60D)

## 배경
사용자 지시 (2026-07-02 fix 회의):
> "interval ladder(1,3,7,14,28,60...) 정하고 싶은게, 사용자마다 사실 interval ladder 가용량도 다르지만 interval day도 다르게 하고 싶은 사람이 많자나요? 이 때 개인적인 판단이 interval day 1,3,7 이것도 다르게 하면 운영 난이도가 급증한다고 생각"

**핵심 컨셉**: 사용자는 "1 card 를 며칠 유지 가능?"만 선언 → 몇 단계 climb 하는지만 결정. **days 자체는 시스템 고정**. 초기 3명 사용자 규모에서 관찰 데이터로 v2 프리셋·커스터마이징 여부를 판단.

**뒤집는 결정**: 기존 `LearningMode.MODE_10D/20D/30D`는 폐기. 새 `MODE_7D/14D/28D/60D` 4개로 재편.

## 조사 결과 — 현재 상태

| 항목 | 현재 |
|---|---|
| Enum | `LearningMode` = `{MODE_10D, MODE_20D, MODE_30D}` |
| Intervals | `MODE_10D` = [1,3,7] / `MODE_20D` = [1,3,7,14] / `MODE_30D` = [1,3,7,14,21] |
| maxView | 각각 3 / 5 / 7 |
| maxDuration | 각각 10일 / 20일 / 30일 |
| Threshold (`LearningModeMappingPolicy`) | `≤14 → 10D` / `15~24 → 20D` / `≥25 → 30D` |
| 파일 | `UserSchedule/domain/model/LearningMode.java`, `LearningModeMappingPolicy.java` |

## 옵션 비교

**Option A — v1 고정 ladder + 4 mode 재편 (채택)**
- Interval ladder = **[1, 3, 7, 14, 28, 60]** (6 steps).
- Mode = 사용자가 climb 할 단계 수만 선택 → 4개 modes로 재정의.
- Days 자체는 절대 커스터마이징 없음 (v1).

**Option B — 프리셋 여러 개 (standard/light/heavy)**
- Mode enum 값 수 급증. 프리셋 세트를 관찰 데이터 없이 결정 어려움.

**Option C — 완전 커스텀 (사용자 days 자유)**
- Mode 개념 붕괴, `createdMode` enum(이슈 #23) 방향과 배치. 운영 난이도 급증.

## 선택: Option A

## 부속 결정

### 새 Mode enum 정의

```java
public enum LearningMode {
    MODE_7D  (List.of(1, 3, 7)),
    MODE_14D (List.of(1, 3, 7, 14)),
    MODE_28D (List.of(1, 3, 7, 14, 28)),
    MODE_60D (List.of(1, 3, 7, 14, 28, 60));

    private final List<Integer> intervals;

    public int maxDays() {
        return intervals.get(intervals.size() - 1);
    }

    public int stepCount() {
        return intervals.size();
    }
}
```

- 기존 `maxView`, `maxDuration` 필드 폐기 (이슈 #22와 연동 — `OnFieldBudget` 폐기).
- `IntervalStep` 개념도 재검토 (Card BC 참조에서 fixed ladder 상수로 대체 가능한지).

### 새 매핑 threshold (`LearningModeMappingPolicy`)

```java
public LearningMode resolve(int rawInputDays) {
    if (rawInputDays <= 7)  return MODE_7D;
    if (rawInputDays <= 14) return MODE_14D;
    if (rawInputDays <= 28) return MODE_28D;
    return MODE_60D; // upper bound: 60 clamp 별도 검증
}
```

- `raw_input_days` 하한 검증: 1 이상 (`USER_SCHEDULE_INPUT_TOO_SHORT` 400).
- 상한 검증: 60일 초과는 60으로 clamp (경고 표시), 또는 예외 (`USER_SCHEDULE_INPUT_TOO_LONG`) — v1 default: **60으로 silently clamp + 프론트에서 정보성 안내**.

### 데이터 마이그레이션 (Flyway)

```
V{N}__reorganize_learning_mode.sql
--
UPDATE user_schedule_config SET mapped_mode = 'MODE_7D'  WHERE mapped_mode = 'MODE_10D';
UPDATE user_schedule_config SET mapped_mode = 'MODE_14D' WHERE mapped_mode = 'MODE_20D';
UPDATE user_schedule_config SET mapped_mode = 'MODE_28D' WHERE mapped_mode = 'MODE_30D';

-- Card 테이블에 created_mode 컬럼 추가는 이슈 #23에서 처리.
-- 여기선 UserScheduleConfig만 이관.

ALTER TABLE user_schedule_config
  MODIFY COLUMN mapped_mode
  VARCHAR(10) NOT NULL
  CHECK (mapped_mode IN ('MODE_7D','MODE_14D','MODE_28D','MODE_60D'));
```

- 롤백 스크립트 `R{N}__rollback_reorganize_learning_mode.sql` 동반.
- v1 초기 3명 사용자 규모라 데이터 소량 — 안전 이관.

### 매핑 안전성 근거

| 기존 | 새 | 손실 |
|---|---|---|
| 10D (max 10, intervals [1,3,7]) | 7D (max 7, intervals [1,3,7]) | 3일 max 축소, intervals 동일. `MAX_VIEW`/`MAX_DURATION` 폐기(#22)로 실제 영향 없음 |
| 20D (max 20, intervals [1,3,7,14]) | 14D (max 14, intervals [1,3,7,14]) | 6일 축소, intervals 동일 |
| 30D (max 30, intervals [1,3,7,14,21]) | 28D (max 28, intervals [1,3,7,14,28]) | 마지막 interval 21→28로 확장. **사용자에게 좋은 변화** (21일차만 봤던 카드가 28일차에 한 번 더 봄) |

### 관찰 지표 (v1 로깅 — 이슈 #20 계승)

- 사용자별 mode 변경 이력 (up/down 시각·직전·직후 mode)
- Mode별 daily clear율 (배포 후 축적 데이터로 프리셋 후보 도출)
- `raw_input_days` 히스토그램 (사용자가 실제 어떤 수치를 입력하는지)

이 데이터가 v2 시점의 프리셋(standard/light/heavy) 또는 완전 커스텀 결정 근거.

## 이관 산출물

- **BE-Story #21-1**: `LearningMode` enum 재정의 (MODE_7D/14D/28D/60D). 기존 `maxView`, `maxDuration` 필드 제거. `intervals: List<Integer>` 필드만 유지.
- **BE-Story #21-2**: `LearningModeMappingPolicy` threshold 재작성.
- **BE-Story #21-3**: Flyway `V{N}__reorganize_learning_mode.sql` + 롤백. `user_schedule_config.mapped_mode` VARCHAR CHECK 업데이트.
- **BE-Story #21-4**: `UserScheduleConfig.resolveOnFieldBudget()` 삭제 (이슈 #22와 함께 폐기) — 이 이슈에서는 시그니처 변경만, 실제 로직 제거는 #22.
- **BE-Story #21-5**: `raw_input_days` 60일 상한 clamp 로직 + 정보성 안내.
- **BE-Story #21-6**: 관찰 지표 로깅 (mode 변경 이력, raw_input_days 히스토그램) — 이슈 #26의 대시보드 데이터 소스.
- **FE-Story #21-7**: 사용자 mode 선택 UI에 새 mode 4개 표시. `raw_input_days` 입력 UX (예: "며칠 유지 가능?" 슬라이더 or 입력).
- **SDD 개정**: `product-review.md`에 mode enum·매핑 명세.

## 관련 이슈 / 문서

- 짝 이슈: [#22 OnFieldBudget 폐기](./issue-22-onfieldbudget-abolition.md), [#23 Card createdMode 하이브리드](./issue-23-card-createdmode-hybrid.md).
- 후속: [#24 DailyLearningBatch](./issue-24-daily-learning-batch.md) — mode 데이터 사용처.
- 데이터 소스: [#26 캐시 측정 대시보드](./issue-26-cache-measurement-dashboard.md).
- SDD 신설: `product-review.md`, `product-card.md`.
