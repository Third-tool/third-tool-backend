# Issue: Card에 createdMode 필드 추가 + M3 하이브리드 규칙 (down=cap, up=새 card만)

## 배경
사용자 지시 (2026-07-02 fix 회의):
> "만약 1,3,7 로 유지하고 있던 사용자가 14를 뚫었을 때 1,3,7,14 Card가 어떻게 유지해야할지"
> "만약 20이 버거워서 버전을 다운했을 때 reviewSession에서 14 까지 가져오던 것을 어떻게 멈추게 해야하는지"

사용자 mode 변경(up/down) 시 이미 field에 있는 카드의 스케줄을 어떻게 다룰지가 오답노트 컨셉의 핵심 결정.

**선택된 규칙 (M3 하이브리드)**:
- **Down = 기존 card 즉시 cap** — 다운의 사용자 의도("너무 힘들어") 정확히 반영, load 즉시 감소
- **Up = 새 card만 확장** — 진행 중 card는 원래 스케줄 유지, "이미 archive될 예정이던 카드가 되살아나는" 이상 UX 방지

이 규칙이 성립하려면 **Card가 자기 원래 mode를 기억해야 함** (F1 결정).

## 조사 결과 — 현재 상태

- `Card`에 mode 관련 필드 없음. Card 도메인은 UserSchedule 참조 없음.
- OnFieldBudget 매 호출 주입으로 lifecycle 판정 — 이슈 #22에서 폐기.
- Mode 변경 시점 처리 로직 없음.

## 옵션 비교

**Option A — Card에 `createdMode: LearningMode` enum 필드 추가 + M3 하이브리드 (채택)**
- 스키마: `card.created_mode VARCHAR(10) NOT NULL CHECK (MODE_7D/14D/28D/60D)`.
- 도메인 메서드: `card.effectiveMaxDays(userCurrentMode)`, `card.isDueOn(date, userCurrentMode)`, `card.hasScheduleExhausted(userCurrentMode, today)`.
- `effectiveMaxDays = min(card.createdMode.maxDays(), userCurrentMode.maxDays())` — 이 한 줄이 M3 down cap의 핵심.

**Option B — Card는 mode 저장 안 함, 시각으로만 판정 (F3)**
- User mode 변경 이력 별도 테이블. Card 생성 시각 → 그 시점 user mode 조회로 매번 계산.
- Fragile. 사용자가 여러 번 mode 변경 시 이력 조회 복잡.

**Option C — Card에 `originalMaxDays: int` 저장 (F2 축소판)**
- Mode enum이 아니라 int 하나만. 마이그레이션 단순.
- 단점: intervals list를 얻으려면 int → mode 역매핑 필요. 결국 mode enum 저장이 더 명료.

## 선택: Option A (F1 확정 결과 반영)

## 부속 결정

### 스키마 변경 (Flyway)

```sql
V{N}__add_card_created_mode.sql
--
-- 1) 컬럼 nullable로 추가
ALTER TABLE card ADD COLUMN created_mode VARCHAR(10) NULL;

-- 2) 기존 카드 backfill — 생성 당시 user mode를 알 수 없으므로
--    현재 user_schedule_config.mapped_mode 값으로 채움
UPDATE card c
JOIN user_schedule_config usc ON c.user_id = usc.user_id
SET c.created_mode = usc.mapped_mode
WHERE c.created_mode IS NULL;

-- 3) user_schedule_config가 없는 유저의 카드는 default (MODE_14D — 중간값)
UPDATE card SET created_mode = 'MODE_14D' WHERE created_mode IS NULL;

-- 4) NOT NULL + CHECK 전환
ALTER TABLE card
  MODIFY COLUMN created_mode VARCHAR(10) NOT NULL,
  ADD CONSTRAINT chk_card_created_mode
    CHECK (created_mode IN ('MODE_7D','MODE_14D','MODE_28D','MODE_60D'));

CREATE INDEX idx_card_created_mode ON card(created_mode);
```

- 3단계 분리 (nullable → backfill → NOT NULL) — conventions.md §3.8 준수.
- 롤백 `R{N}__rollback_add_card_created_mode.sql` 동반.

### 도메인 메서드 (`Card`)

```java
public class Card {
    private LearningMode createdMode;
    private LocalDate enteredFieldAt; // 이미 존재 (LocalDateTime? → LocalDate 축약 검토)

    /** M3 하이브리드: 이 card가 실제로 몇 일차까지 노출되는가 */
    public int effectiveMaxDays(LearningMode userCurrentMode) {
        return Math.min(createdMode.maxDays(), userCurrentMode.maxDays());
    }

    /** 이 card의 실제로 사용될 intervals (userMode에 cap된) */
    public List<Integer> effectiveIntervals(LearningMode userCurrentMode) {
        int maxDays = effectiveMaxDays(userCurrentMode);
        return createdMode.intervals().stream()
            .filter(day -> day <= maxDays)
            .toList();
    }

    /** 오늘 이 card가 due인가 (batch가 큐에 담을지 판정) */
    public boolean isDueOn(LocalDate today, LearningMode userCurrentMode) {
        long daysSinceEntered = ChronoUnit.DAYS.between(enteredFieldAt, today);
        return effectiveIntervals(userCurrentMode).contains((int) daysSinceEntered);
    }

    /** 이 card의 마지막 interval이 이미 지났는가 (SCHEDULE_EXHAUSTED 판정) */
    public boolean hasScheduleExhausted(LearningMode userCurrentMode, LocalDate today) {
        long daysSinceEntered = ChronoUnit.DAYS.between(enteredFieldAt, today);
        return daysSinceEntered > effectiveMaxDays(userCurrentMode);
    }

    /** returnToField — fresh 재시작 */
    public void returnToField(LearningMode userCurrentMode, LocalDate today) {
        if (this.status == ON_FIELD) return; // idempotent
        this.status = ON_FIELD;
        this.enteredFieldAt = today;
        this.createdMode = userCurrentMode; // 사용자 현재 mode로 재설정
        this.viewCount = 0; // v1엔 유지되지만 리셋
        this.lastViewedAt = null;
    }
}
```

**핵심 로직**: `effectiveMaxDays = min(createdMode.max, userMode.max)` 한 줄이 M3 하이브리드 down cap.
- User가 20D → 7D 다운 → 카드 `createdMode=MODE_14D`, `userMode=MODE_7D` → `effectiveMax = min(14, 7) = 7` → 카드가 7일차에 archive.
- User가 7D → 14D 업 → 카드 `createdMode=MODE_7D`, `userMode=MODE_14D` → `effectiveMax = min(7, 14) = 7` → 카드는 여전히 7일차까지만. **새 카드는 `createdMode=MODE_14D`로 생성되어 14일차까지 노출**.

### `ArchiveReason.MODE_DOWNGRADED`

- 이슈 #22에서 신규 등록.
- Down cap 발동 시 batch가 archive하며 이 reason 사용.

### `returnToField` fresh 재시작

- 원래 mode 유지 대신 사용자 현재 mode로 재설정 (F1 확정 결정).
- 재학습 = fresh 시작이라는 도메인 의도.

### Card 생성 시점 `createdMode` 결정

- `CardCommandService.createCard(userId, ...)` — 현재 user mode 조회 후 `Card.create(..., userCurrentMode)`.
- 생성 시점의 user mode 스냅샷이 곧 `card.createdMode`.

## 이관 산출물

- **BE-Story #23-1**: Flyway `V{N}__add_card_created_mode.sql` + 롤백. 3단계 (nullable → backfill → NOT NULL).
- **BE-Story #23-2**: `Card` 도메인에 `createdMode` 필드 + `effectiveMaxDays`, `effectiveIntervals`, `isDueOn`, `hasScheduleExhausted` 메서드.
- **BE-Story #23-3**: `Card.returnToField(userCurrentMode, today)` 시그니처 변경 (fresh 재시작).
- **BE-Story #23-4**: `Card.create(...)` 팩토리에 `createdMode` 파라미터 추가. `CardCommandService`가 user 현재 mode를 조회해 주입.
- **BE-Story #23-5**: 도메인 단위 테스트 — M3 하이브리드 down/up 시나리오 명시 (`downCap_이미넘긴카드_즉시archive_예외`, `up_기존카드_원스케줄유지_해피`, `down_아직안넘긴카드_새max로cap_해피` 등).
- **BE-Story #23-6**: ErrorCode 신설 — 딱히 필요 없음 (mode 변경 시 검증은 UserSchedule BC에서).
- **SDD 개정**: `product-card.md` (신설) — createdMode 필드·M3 규칙 명세.

## 관련 이슈 / 문서

- 짝 이슈: [#21 Interval ladder 고정](./issue-21-interval-ladder-fixed-mode-reorganize.md), [#22 OnFieldBudget 폐기](./issue-22-onfieldbudget-abolition.md).
- 소비처: [#24 DailyLearningBatch](./issue-24-daily-learning-batch.md) — `isDueOn`, `hasScheduleExhausted` 호출로 batch queue 구성.
- SDD 신설: `product-card.md`.
