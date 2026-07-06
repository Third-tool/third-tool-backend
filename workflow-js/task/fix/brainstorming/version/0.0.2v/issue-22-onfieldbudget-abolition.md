# Issue: OnFieldBudget(maxView + maxDuration) 이중 게이트 폐기 → fixed interval queue로 대체

## 배경
사용자 지시 (2026-07-02 fix 회의):
> "soft schedule 전략 변경 예정, Card 기준으로 maxview, maxDuration이 존재하는데, 지금 회의하면서 결정할게, Card 단위로 예시 1,3,7 전략이 있었다면 1일차에 그날 못봤으면 그냥 지나가버리는 전략으로 수정하려고 해, 굳이 대기를 해주는 형태가 아니고 queue 느낌으로 각 Card 전략 자체가 1,3,7 로 지정을 했으면 Card를 만들어지고 1일 뒤, 3일 뒤, 7일 뒤에 무조건 나오고 못본다고 누적 없이 그냥 7일차에 archive로 바뀌는 전략"

현재 `OnFieldBudget(maxView, maxDuration)` **이중 게이트** — 두 조건 병렬 카운트, 먼저 걸리는 쪽이 archive. 이 구조는 fixed interval queue 모델과 중복·충돌:
- interval days가 노출 시점을 결정하는데 `maxView`가 별도로 카운트 → 개념 이중화
- `maxDuration`이 마지막 interval == archive와 동일 의미 → 개념 중복

**목표**: `OnFieldBudget` 개념·클래스·필드 완전 폐기. Card lifecycle은 **fixed interval queue** 하나로 통일.

## 조사 결과 — 현재 이중 게이트

| 컴포넌트 | 역할 |
|---|---|
| `OnFieldBudget(maxView, maxDuration)` VO | Card lifecycle 이중 게이트 정의 (Card/domain/model/OnFieldBudget.java) |
| `CardExpiryPolicy` | `OnFieldBudget.resolveReason(card)` — 어느 조건 도달했는지 판정 (Card/domain/service/CardExpiryPolicy.java) |
| `CardExpiryBatchService` | 야간 배치 — MAX_DURATION 도달 카드 archive (Card/application/service/CardExpiryBatchService.java) |
| `ReviewCommandService.incrementViewAndHandleMaxView()` | 인라인 archive — MAX_VIEW 도달 시 즉시 archive (ReviewCommandService.java:94~108) |
| `UserScheduleQueryService.resolveOnFieldBudget(userId)` | User mode → OnFieldBudget VO 반환 (UserSchedule/application/service/UserScheduleQueryService.java:47~51) |
| `ArchiveReason` enum | `MANUAL, MAX_VIEW, MAX_DURATION` (Card/domain/model/ArchiveReason.java) |

Review·Batch 두 경로가 OnFieldBudget를 매 호출 주입받아 archive 결정을 각자 수행 → **두 경로 각각 재작성 필요**.

## 옵션 비교

**Option A — OnFieldBudget 완전 폐기 + fixed interval queue 단일화 (채택)**
- `OnFieldBudget` VO 삭제.
- Card lifecycle 결정은 `Card.createdMode` (이슈 #23) + `user.currentMode` + `enteredFieldAt` 세 값으로 완전 표현.
- Archive는 "마지막 interval 도달" 하나의 자연 트리거 (+ MANUAL + MODE_DOWNGRADED).

**Option B — OnFieldBudget 유지 + fixed interval queue 병행**
- 이중 게이트 유지. 사용자 요구("queue 느낌", "그냥 지나가버리는 전략")와 배치.

**Option C — OnFieldBudget 유지하되 maxDuration만 폐기**
- 부분 통합. 이중 게이트 개념 여전히 남음. 반쪽 해결.

## 선택: Option A

## 부속 결정

### `ArchiveReason` 재정의

```java
public enum ArchiveReason {
    MANUAL,             // 사용자가 명시 archive
    SCHEDULE_EXHAUSTED, // 마지막 interval 도달 → 자동 archive (신)
    MODE_DOWNGRADED;    // 사용자 mode down 시 이미 새 max 지난 카드 (이슈 #23)
}
```

- 폐기: `MAX_VIEW`, `MAX_DURATION`.
- 신규: `SCHEDULE_EXHAUSTED`, `MODE_DOWNGRADED`.

### Archive 트리거 재편

| 트리거 | 판정 시점 | 판정 주체 |
|---|---|---|
| `MANUAL` | 사용자가 archive API 호출 | `CardCommandService.archive(userId, cardId)` |
| `SCHEDULE_EXHAUSTED` | Batch가 그날 due 카드 pick 시 마지막 interval 도달 카드 감지 | `DailyLearningBatch.generateFor(...)` — 이슈 #24 |
| `MODE_DOWNGRADED` | User mode down 시점 + 다음 batch 생성 시 새 max 지난 카드 감지 | `DailyLearningBatch.generateFor(...)` — 이슈 #23 & #24 |

### `Card` 도메인 필드 정리

**제거**:
- ~~`Card.maxView`~~ — 도메인 필드 없었지만 참조되던 값. 이제 no-op.
- ~~`Card.viewCount`~~ — **일부 유지 판단 필요**: 관찰 지표 (실제 view 통계)로 유지할지 폐기할지. **v1엔 유지** (관찰 지표) but Card lifecycle 결정에는 무관.
- ~~`Card.lastViewedAt`~~ — **관찰 지표로 유지**. Card lifecycle 결정에는 무관.

**유지**:
- `Card.enteredFieldAt` — 스케줄 계산의 근원. `isDueOn(date, userMode)` 계산에 필수.
- `Card.status` (ON_FIELD/ARCHIVE) 유지.

### `CardExpiryBatchService` 재편성

- 기존: 야간 배치가 `maxDuration` 초과 카드를 archive.
- 신: **폐기**. Archive는 `DailyLearningBatch.generateFor()`가 lazy로 처리 (이슈 #24).
- 사용자가 안 오는 경우 → batch 생성 안 되고, `SCHEDULE_EXHAUSTED` 카드도 archive 안 됨. **의도된 동작**: 사용자 첫 접근 시점에 lazy 정리.
- 만약 archive된 상태가 다른 조회에서 필요하면 `Card.isScheduleExhausted(userMode, today)` 도메인 메서드로 실시간 판정.

### `ReviewCommandService.incrementViewAndHandleMaxView()` 재편성

- 기존: `card.recordView()` + viewCount 확인 + MAX_VIEW 도달 시 즉시 archive.
- 신: `card.recordView()`는 관찰 지표(viewCount, lastViewedAt) 갱신만. Archive 결정은 하지 않음. `DailyLearningBatch.markViewed(cardId, viewedAt)` 호출로 batch 진행 상태 갱신.
- MAX_VIEW 인라인 archive 로직 완전 제거.

### `UserScheduleQueryService.resolveOnFieldBudget()` 폐기

- 시그니처 제거. 호출부 (Review, Batch) 모두 정리.
- 대체 API: `UserScheduleQueryService.currentMode(userId): LearningMode` — mode enum 하나만 반환.

## 이관 산출물

- **BE-Story #22-1**: `OnFieldBudget` VO 클래스 삭제. 모든 참조부 제거 (Grep으로 스캔 필수).
- **BE-Story #22-2**: `CardExpiryPolicy`, `CardExpiryBatchService` 클래스 삭제 (또는 `SCHEDULE_EXHAUSTED` 판정 도메인 서비스로 축소).
- **BE-Story #22-3**: `ReviewCommandService.incrementViewAndHandleMaxView()` 재작성 — MAX_VIEW 로직 제거, viewCount 기록·batch 통보만.
- **BE-Story #22-4**: `UserScheduleQueryService.resolveOnFieldBudget()` 시그니처 제거 → `currentMode(userId)` 로 축소.
- **BE-Story #22-5**: `ArchiveReason` enum 재정의 (MANUAL/SCHEDULE_EXHAUSTED/MODE_DOWNGRADED). 기존 데이터 마이그레이션: `MAX_VIEW` → `SCHEDULE_EXHAUSTED`, `MAX_DURATION` → `SCHEDULE_EXHAUSTED`. `CardStatusHistory.reason` DB CHECK 재정의.
- **BE-Story #22-6**: `CardStatusHistoryAppender.append()` 시그니처는 유지. 신 reason enum 값으로 호출.
- **BE-Story #22-7**: 테스트 재작성 — MAX_VIEW/MAX_DURATION 기반 테스트 케이스 폐기, SCHEDULE_EXHAUSTED 기반 재작성.
- **BE-Story #22-8**: Flyway `V{N}__abolish_onfieldbudget.sql` — `ArchiveReason` CHECK 재정의 + 기존 history 데이터 reason 마이그레이션. 롤백 스크립트 동반.
- **BE-Story #22-9**: ErrorCode 정리 — MAX_VIEW/MAX_DURATION 관련 코드 제거.
- **SDD 개정**: `product-card.md` (신설 in-progress) — lifecycle 트리거 재편, `product-review.md` — batch가 archive 결정 소유.

## 관련 이슈 / 문서

- 뒤집는 결정: 이전 Card lifecycle 이중 게이트 (product-card.md 0.0.1v).
- 짝 이슈: [#21 Interval ladder 고정](./issue-21-interval-ladder-fixed-mode-reorganize.md), [#23 Card createdMode 하이브리드](./issue-23-card-createdmode-hybrid.md).
- Archive 트리거 이관: [#24 DailyLearningBatch](./issue-24-daily-learning-batch.md).
- SDD 신설: `product-card.md`, `product-review.md`.
