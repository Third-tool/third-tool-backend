# Issue: ReviewSession 재편 — layer 경계 폐기, cross-layer 짬뽕 큐로 통일

## 배경
사용자 지시 (2026-07-02 fix 회의):
> "reviewSession 일일 수집 -> reviewSession을 학습할 때 우리가 layer->axis->roadmap,selections 구조인데 학습 reviewSession의 단위는 전체 layer를 합쳐서 Card 제공 예정 layer마다가 아니고 layer 아래에 있는 모든 session을 기반해서 Card 학습 예정, layer끼리 나누지 않겠다."

**핵심 결정**: Review 세션의 카드 pool = 전체 layer 하위 모든 axis의 due card 짬뽕. Layer/Axis별 별도 review 없음.

**뒤집는 결정**:
- 기존 `ReviewCommandService.startReview(deckId)` — deck 단위 세션 구성 폐기 (deck 자체가 이슈 #13에서 폐기됨).
- 이슈 #14의 `ReviewScope { AXIS, LAYER }` 이중 스코프 개념 — layer scope 실질 사용 안 됨. **폐기 또는 축소**.

**목표**: `ReviewSession`은 세션 인스턴스 개념 그대로 유지 (INITIAL/COMPARING 상태). 다만 카드 pool을 **DailyLearningBatch가 소유** (이슈 #24). 세션은 batch에서 카드를 pull.

## 조사 결과 — 현재 세션 스코프

| 스코프 방식 | 현재 |
|---|---|
| `ReviewCommandService.startReview(deckId)` | Deck 단위. Deck에 속한 카드 통짜 로드. |
| `ReviewScope { AXIS, LAYER }` (이슈 #14) | 이중 스코프 도입 예정. Axis 스코프 or Layer 스코프. |
| `ReviewQueryService.getTodayCandidates()` | axis 필터 optional. 없으면 전체 fallback. `StateRecommendationDistributor`로 카테고리 배분. |
| `findAllByDeckIdAndDeletedFalse(deckId)` | 세션 초기 카드 로드 방식. Deck 폐기 시 무의미. |

## 옵션 비교

**Option A — `ReviewSession`은 인스턴스 유지, 카드 pool을 `DailyLearningBatch`가 소유 (채택)**
- Layer/Axis 스코프 개념 폐기. 사용자 전체 daily batch가 카드 원천.
- 세션 시작 시 batch에서 미완료 entry를 pull (Fresh 세션이면 batch 전체 미완료, 재개 세션이면 아직 안 본 카드만).
- `ReviewSession(INITIAL/COMPARING)` 상태 그대로.

**Option B — Layer 스코프 유지 + Layer별 별도 batch**
- 사용자 지시("layer 끼리 나누지 않겠다")와 배치. 제거.

**Option C — 세션·batch 통합 (하루 1 세션)**
- 사용자가 하루에 여러 번 앱 재접근 시 세션 개념 필요. 통합 시 상태 관리 애매.

## 선택: Option A

## 부속 결정

### `ReviewSession` 도메인 재편

```java
public class ReviewSession {
    private Long id;
    private Long userId;
    private Long dailyBatchId;               // 신규: 참조하는 daily batch
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;        // null = 진행 중
    private List<CardReview> cardReviews;    // 이 세션 인스턴스에서 볼 카드들 (batch의 미완료 subset)
    private int currentIndex;

    /** Factory: batch의 미완료 entry로부터 세션 생성 */
    public static ReviewSession startFrom(DailyLearningBatch batch, LocalDateTime now) {
        if (batch.isClosed()) throw new DailyBatchClosedException();
        List<CardReview> reviews = batch.entries().stream()
            .filter(e -> !e.isViewed())
            .map(e -> CardReview.of(e.getCardId(), reviews.size()))
            .toList();
        return new ReviewSession(batch.userId, batch.id, now, reviews);
    }
    // startComparing, moveToNext, recordView 등 기존 로직 유지
    // recordView 시 dailyBatch.markViewed(cardId, viewedAt) 호출로 batch 상태 동기화
}
```

**변경 요약**:
- 세션 시작 = daily batch에서 미완료 entry를 카드로 담아 인스턴스화.
- 사용자가 카드 완료 시 세션이 `DailyLearningBatch.markViewed()` 호출 → batch 진행률 실시간 갱신.
- 여러 세션 인스턴스가 같은 batch를 공유 (동시 세션 방지 정책 별도).

### 동시 세션 처리

- v1 원칙: 사용자당 **동시 세션 1개**만 허용. 새 세션 시작 시 진행 중 세션이 있으면:
  - **A. 이전 세션 자동 finish** (미완료 카드는 batch에 아직 미완료 상태 그대로)
  - B. 예외 (`REVIEW_SESSION_ALREADY_ACTIVE` 409)

  **선택 A** — UX 마찰 최소. 앱 재실행 시 자연스럽게 새 세션.

### API 재편

기존 `POST /api/v1/decks/{deckId}/review-sessions` 폐기. 신규:

| 메서드 | 경로 | 목적 |
|---|---|---|
| POST | `/api/v1/review-sessions` | 새 review 세션 시작 (오늘 batch에서 미완료 카드 pull) |
| GET | `/api/v1/review-sessions/{id}` | 세션 진행 상태 조회 |
| POST | `/api/v1/review-sessions/{id}/start-comparing` | 현재 카드 COMPARING 진입 |
| POST | `/api/v1/review-sessions/{id}/record-view` | 현재 카드 view 기록 → batch markViewed 트리거 |
| POST | `/api/v1/review-sessions/{id}/next` | 다음 카드 이동 |
| POST | `/api/v1/review-sessions/{id}/finish` | 세션 명시 종료 |

### 이슈 #14 정리

- 이슈 #14 "ReviewScope AXIS/LAYER 이중 스코프" — **부분 폐기**.
- `ReviewScope` enum 자체는 아직 미구현이면 신설 안 함. 이미 구현됐다면 `LAYER` 값 데드 코드 → 정리.
- 이슈 #14의 `Layer.progressStatus` 파생 (하위 axis statuses → 3-state) — daily batch와 무관하므로 유지. Layer 시각화용 별도 API.

### `StateRecommendationDistributor` 정리

- 현재 `Review/domain/model/StateRecommendationDistributor.java` — SoftScheduleState 카테고리별 LRM 배분.
- 새 모델: batch의 entries는 이미 그날 due인 card만 담김. 카테고리 배분 불필요.
- **폐기 판정**: v1엔 배분 로직 단순화 (batch 순서 = card_interval_day ASC · exposed_at ASC).
- 필요 시 v2에 카드 정렬 전략 (예: interval 짧은 것부터, 랜덤, 카드별 view 이력 기준) 재도입.

### `ReviewSessionRepository` 쿼리 정리

- `findAllByDeckId` 폐기.
- 신규: `findLatestActiveByUserId(userId)` — 진행 중 세션 조회.
- `findAllByUserIdAndDate(userId, date)` — 하루의 모든 세션 (통계용).

## 이관 산출물

- **BE-Story #25-1**: `ReviewSession.startFrom(batch, now)` 팩토리 재작성. 기존 `startReview(deckId)` 폐기.
- **BE-Story #25-2**: `ReviewSession.recordView()` — 카드 완료 시 `DailyLearningBatch.markViewed()` 호출로 batch 동기화.
- **BE-Story #25-3**: 동시 세션 정책 — 새 세션 시작 시 진행 중 세션 자동 finish.
- **BE-Story #25-4**: 신규 엔드포인트 (위 표) + 기존 `/decks/{deckId}/review-sessions` 폐기 (410 Gone).
- **BE-Story #25-5**: `StateRecommendationDistributor` 폐기 (또는 v2 재도입 backlog).
- **BE-Story #25-6**: `ReviewSessionRepository` 쿼리 재편 (`findAllByDeckId` 삭제, `findLatestActiveByUserId` 신설).
- **BE-Story #25-7**: 이슈 #14 정리 — `ReviewScope` enum 미구현이면 신설 안 함, 구현됐다면 LAYER 값 정리.
- **BE-Story #25-8**: 슬라이스 테스트 (@DataJpaTest, @WebMvcTest) 재작성.
- **BE-Story #25-9**: 통합 테스트 — daily batch ↔ review session 동기화 (사용자가 세션 A에서 카드 X 봄 → 세션 B에서 X는 미노출) 검증.
- **BE-Story #25-10**: ErrorCode 신설 — `REVIEW_SESSION_NOT_FOUND` 404, `DAILY_BATCH_CLOSED_FOR_NEW_SESSION` 409 (자정 후 새 세션 시도).
- **FE-Story #25-11**: Review 세션 UI — 시작·재개·종료 흐름. Daily 진행률 표시.
- **SDD 개정**: `product-review.md` (신설) — 세션·batch 관계, cross-layer 짬뽕 규칙 명세.

## 관련 이슈 / 문서

- 뒤집는 이슈: v0.0.1 review 개념 (deck 스코프), [#14 Review 이중 스코프](./issue-14-review-strategy-axis-layer-scope.md) 부분 폐기.
- 짝 이슈: [#24 DailyLearningBatch](./issue-24-daily-learning-batch.md).
- 연동: [#13 Deck 폐기](./issue-13-deck-abolition-axis-absorption.md) — Deck 폐기와 정합.
- SDD 신설: `product-review.md`.
