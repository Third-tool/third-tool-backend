# Issue: DailyLearningBatch Aggregate + DailyCardEntry 자식 신설

## 배경
사용자 지시 (2026-07-02 fix 회의):
> "새로운 session 하나 추가 그날 나온 card를 전부 다 보고 있는지 track 할 수 있는 기능 추가, 그날 Card는 한번에 짬뽕해서 1,3,7,14 치 카드는 전부 나올 예정 예를 들어 50장이 나왔다고 하면 그것을 그날의 컨디션, 생활에 따라서 다 볼수도 못볼수도 있다고 생각"
> "이 daily session의 의도 자체가 (...) 캐시의 양이 예를 들어 뇌용량의 크기가 1,3,7 밖에 안되는 사람인데, 1,3,7,14,30을 유지하고 있으면 이런식으로 card가 계속 남게 되잖아? (...) 이 daily session은 그 날 학습 reviewSession에 날라오는 card를 얼마나 clear를 하는지, clear를 했다면 며칠 연속으로 완벽하게 clear를 했는지 이 clear 여부를 통해서 자기의 캐시의 양을 측정한다"

**핵심 목적 (도메인 의도)**:
1. 하루당 card 큐 관리 (사용자 첫 접근 시 lazy 생성)
2. 큐 완료율 (그날 clear율) track — **사용자 학습 캐시 용량의 실측치**
3. Streak (연속 완벽 clear 일수) 계산 근거
4. 카드 관점 노출 이력 (`DailyCardEntry.findByCardId`)
5. 이슈 #26 대시보드·주간 요약의 데이터 소스

## 조사 결과 — 현재 상태

- Daily 개념 없음. 세션 단위 (`ReviewSession(INITIAL/COMPARING)`) 만 존재.
- 그날 clear 여부 추적 로직 없음.
- 카드 노출 이력 없음 (사용자가 실제 카드를 언제 봤는지 재구성 불가).
- Card queue는 세션 시작 시 매번 `findAllByDeckIdAndDeletedFalse(deckId)` 로 즉시 조회 — 세션 간 dedup 없음.

## 옵션 비교

**Option A — DailyLearningBatch Aggregate + DailyCardEntry 자식 신설 (채택)**
- 하루당 1개 Aggregate per user. 그날 큐·완료 상태 소유.
- 자식 `DailyCardEntry(card_id, exposed_at, viewed_at?)` — 그날 카드 하나하나.
- `ReviewSession`은 유지 — 여러 세션 인스턴스가 같은 batch 참조.

**Option B — ReviewSession을 daily로 재해석 (하루당 1 세션)**
- 기존 세션 개념 뒤집힘. INITIAL/COMPARING 상태 의미 애매.

**Option C — CardExposureLog 이벤트 테이블만 (Aggregate 없음)**
- 상태 관리 흩어짐. 그날 큐·완료 판정을 매번 쿼리로 재계산.

## 선택: Option A

## 부속 결정

### 스키마 (Flyway)

```sql
V{N}__daily_learning_batch.sql
--
CREATE TABLE daily_learning_batch (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  batch_date DATE NOT NULL,
  generated_at DATETIME(6) NOT NULL,
  closed_at DATETIME(6) NULL,
  user_mode_at_generation VARCHAR(10) NOT NULL,   -- 관찰 지표
  CONSTRAINT uq_daily_batch_user_date UNIQUE (user_id, batch_date),
  CONSTRAINT chk_daily_batch_mode
    CHECK (user_mode_at_generation IN ('MODE_7D','MODE_14D','MODE_28D','MODE_60D'))
);
CREATE INDEX idx_daily_batch_user_date ON daily_learning_batch(user_id, batch_date DESC);

CREATE TABLE daily_card_entry (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  batch_id BIGINT NOT NULL,
  card_id BIGINT NOT NULL,
  card_interval_day INT NOT NULL,      -- 이 카드가 batch에 오른 이유 (1일차/3일차/... 어느 interval)
  exposed_at DATETIME(6) NOT NULL,
  viewed_at DATETIME(6) NULL,          -- null = 미완료
  CONSTRAINT fk_entry_batch FOREIGN KEY (batch_id) REFERENCES daily_learning_batch(id) ON DELETE CASCADE,
  CONSTRAINT uq_entry_batch_card UNIQUE (batch_id, card_id)
);
CREATE INDEX idx_entry_card ON daily_card_entry(card_id);
CREATE INDEX idx_entry_batch_viewed ON daily_card_entry(batch_id, viewed_at);
```

- 유저·날짜 유니크 (하루당 1 batch 강제).
- `card_interval_day` — 관찰 지표 (interval별 clear율 분석 용).
- `user_mode_at_generation` — batch 생성 시점 사용자 mode 스냅샷. Mode 변경 후 이력 분석 근거.
- Soft Delete 없음 (append-only 성격).

### 도메인 (`DailyLearningBatch` Aggregate)

```java
public class DailyLearningBatch {
    private Long id;
    private Long userId;
    private LocalDate batchDate;
    private LocalDateTime generatedAt;
    private LocalDateTime closedAt;      // null = OPEN
    private LearningMode userModeAtGeneration;
    private List<DailyCardEntry> entries;

    /** Factory: 그날 due 카드 pool을 받아 batch 생성 */
    public static DailyLearningBatch generateFor(
        Long userId,
        LocalDate today,
        LearningMode userCurrentMode,
        List<Card> allUserCards       // cross-layer: user의 모든 axis card
    ) {
        List<DailyCardEntry> entries = allUserCards.stream()
            .filter(card -> card.getStatus() == CardStatus.ON_FIELD)
            .filter(card -> !card.hasScheduleExhausted(userCurrentMode, today))
            .filter(card -> card.isDueOn(today, userCurrentMode))
            .map(card -> DailyCardEntry.of(card.getId(),
                                            daysBetween(card.enteredFieldAt, today),
                                            LocalDateTime.now()))
            .toList();
        // hasScheduleExhausted 감지된 카드는 별도 archive 처리 (아래 참조)
        return new DailyLearningBatch(userId, today, userCurrentMode, entries);
    }

    /** 사용자가 카드를 봄 (COMPARING 도달) — Review 세션에서 호출 */
    public void markViewed(Long cardId, LocalDateTime viewedAt) {
        if (isClosed()) throw new DailyBatchClosedException();
        entries.stream()
            .filter(e -> e.getCardId().equals(cardId))
            .findFirst()
            .ifPresent(e -> e.markViewed(viewedAt));
    }

    /** 자정 cron이 호출 — 이후 markViewed 거절 */
    public void close(LocalDateTime closedAt) {
        if (this.closedAt != null) return; // idempotent
        this.closedAt = closedAt;
    }

    public boolean isClosed() { return closedAt != null; }

    public int totalCount() { return entries.size(); }
    public int viewedCount() { return (int) entries.stream().filter(DailyCardEntry::isViewed).count(); }
    public double completionRatio() { return totalCount() == 0 ? 1.0 : (double) viewedCount() / totalCount(); }
    public boolean isPerfectClear() { return totalCount() > 0 && viewedCount() == totalCount(); }
}
```

### Batch 생성 부수 처리 — `SCHEDULE_EXHAUSTED` / `MODE_DOWNGRADED` archive

- `generateFor()`가 필터링 중 `hasScheduleExhausted(userCurrentMode, today) == true` 카드 발견 시:
  - 이 카드들은 batch에 넣지 않음.
  - **동시에 archive 명령** — `Application Service`가 batch 생성 후 이 카드 리스트를 `CardCommandService.archiveMany(cardIds, reason)` 로 처리.
  - Reason 판정:
    - `card.createdMode.maxDays() > userCurrentMode.maxDays()` 이고 이미 새 max 지남 → `MODE_DOWNGRADED`
    - 그 외 (`card.createdMode.maxDays() ≤ userCurrentMode.maxDays()`, 즉 원래 max 도달) → `SCHEDULE_EXHAUSTED`

### API

| 메서드 | 경로 | 목적 |
|---|---|---|
| POST | `/api/v1/daily-batch/today` | 오늘 batch 조회 or 생성 (lazy). 응답에 entries + completion 통계 |
| POST | `/api/v1/daily-batch/{batchId}/entries/{cardId}/mark-viewed` | 카드 view 완료 마킹 (Review 세션에서 내부 호출) |
| GET | `/api/v1/daily-batch/history?from=YYYY-MM-DD&to=YYYY-MM-DD` | 지난 batch 이력 (streak·완료율 계산 근거) |

### 자정 close cron

```java
@Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul") // 매일 00:05 KST
public void closeYesterdaysBatches() {
    LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
    List<DailyLearningBatch> open = repository.findAllOpenByBatchDate(yesterday);
    open.forEach(b -> {
        b.close(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        repository.save(b);
    });
}
```

- 어제 batch 자동 close. 이후 `markViewed` 호출 시 예외.

### Streak 계산 (realtime, 이슈 #26에서 소비)

```java
public class BatchStreakCalculator {
    public int currentStreak(Long userId, LocalDate asOf) {
        // asOf부터 하루씩 뒤로 가며 perfectClear 확인.
        // Break되는 순간까지 카운트.
    }
}
```

- 별도 캐시 필드 없음. 매 조회 시 계산.
- v1 초기 3명 규모라 성능 부담 없음.

## 이관 산출물

- **BE-Story #24-1**: Flyway `V{N}__daily_learning_batch.sql` + 롤백.
- **BE-Story #24-2**: `DailyLearningBatch` Aggregate + `DailyCardEntry` 자식 Entity 신설. 도메인 메서드 (`generateFor`, `markViewed`, `close`, `completionRatio`, `isPerfectClear` 등).
- **BE-Story #24-3**: Repository + Application Service — `DailyLearningBatchService.getOrCreateToday(userId)`, `markViewed(userId, cardId)`, `queryHistory(userId, dateRange)`.
- **BE-Story #24-4**: Application Service에서 `generateFor()` 반환 후 `SCHEDULE_EXHAUSTED`/`MODE_DOWNGRADED` 카드 archive orchestration.
- **BE-Story #24-5**: 자정 close cron (`0 5 0 * * *` KST). Idempotent 처리.
- **BE-Story #24-6**: `BatchStreakCalculator` 도메인 서비스 — realtime streak 계산.
- **BE-Story #24-7**: 엔드포인트 세트 (위 API 표).
- **BE-Story #24-8**: ErrorCode 신설 — `DAILY_BATCH_CLOSED` 409 (자정 close 이후 markViewed 시도), `DAILY_BATCH_NOT_FOUND` 404.
- **BE-Story #24-9**: 슬라이스 테스트 (@DataJpaTest) — UNIQUE `(user_id, batch_date)` 강제 검증.
- **BE-Story #24-10**: 관찰 지표 로깅 — batch 생성·완료·close 이벤트 수집.
- **SDD 개정**: `product-review.md` 신설 — DailyLearningBatch 도메인 명세.

## 관련 이슈 / 문서

- 뒤집는 결정: v0.0.1 product-card.md의 daily 개념 부재.
- 짝 이슈: [#25 Review cross-layer scope](./issue-25-review-cross-layer-scope.md) — `generateFor()`가 이 규칙을 따라 카드 pool 구성.
- 소비처: [#26 캐시 측정 대시보드](./issue-26-cache-measurement-dashboard.md) — batch 완료율·streak·주간 요약의 소스.
- 의존: [#23 Card createdMode 하이브리드](./issue-23-card-createdmode-hybrid.md) — `isDueOn`, `hasScheduleExhausted` 도메인 메서드 호출.
- SDD 신설: `product-review.md`.
