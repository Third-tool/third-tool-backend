# [Product] Review — Daily Learning Batch + Cross-Layer Queue + 캐시 측정 대시보드

## Product Vision

> **Review는 사용자의 "학습 캐시 용량"을 실측하는 시스템이다. 오답노트 카드를 fixed interval queue로 흘려보내면서, 그날 clear율·연속 clear 일수로 사용자가 자기 실제 학습 능력을 데이터로 파악하도록 돕는다.**
> 카드는 layer 경계 없이 하나의 daily 큐에 짬뽕되고, 못 본 카드는 그냥 지나간다. Mode 과대 선택 사용자에게는 3주 관찰 후 규칙 기반으로 다운 안내를 자동 발송한다.

## 배경 및 문제

- 현재 상황 (As-Is)
  - `ReviewSession(INITIAL → COMPARING)` — deck 단위 세션 (`Review/domain/model/ReviewSession.java`). `findAllByDeckIdAndDeletedFalse(deckId)` 로 카드 로드.
  - 카드 노출은 매 세션 시작 시 재조회. 하루 안에 여러 세션 진행 시 카드 재노출 가능 (dedup 없음).
  - 일일 clearance·미완료 카드 추적 스키마 없음.
  - Streak 개념 없음. 사용자가 자기 학습 캐시 용량을 알 방법 없음.
  - `StateRecommendationDistributor`가 SoftScheduleState(1D/3D/7D/14D/21D) 카테고리별 LRM 배분 — 그날 due 판정 없이 배분만 (`Review/domain/model/StateRecommendationDistributor.java`).
  - Layer 경계 개념이 이슈 #14에서 도입 예정이지만 실질 사용 미미.
- 발생하는 문제
  - AI roadmap 리팩토링(#15~#20) 후 Card = 오답노트라는 컨셉이 명확해지면서, 학습 루틴이 "그날 due 카드를 확실히 소진하도록 유도"하는 방향 → 그날 진행률 개념 신설 필요.
  - 사용자가 자기 mode를 과대 선택 (예: "1,3,7,14,60까지 가능해!"). 실제 능력은 1,3,7 수준일 수 있음. 실측·안내 없이는 계속 과대 유지.
  - Layer별 별도 review는 사용자에게 리듬 파편화 → 하나의 daily 큐로 통합해야 자연.
- 왜 지금 해결해야 하는가
  - AI roadmap 리팩토링과 `product-card.md` 신설이 동시 진행 → Review 로직도 함께 갱신해야 정합.
  - 초기 3명 사용자 규모라 breaking change 감내 가능.
  - Deck 폐기(이슈 #13)로 세션 구성 기반이 어차피 재편 필요.

## 목표 (To-Be)

- **`DailyLearningBatch` Aggregate 신설** — 하루당 1개 per user. 그날 큐(cross-layer 짬뽕)와 진행 상태 소유 (이슈 #24).
- **`DailyCardEntry` 자식 Entity** — 그날 카드 하나하나. `viewed_at?` 로 완료 판정. 카드 관점 이력 조회의 원천.
- **Cross-Layer 짬뽕 규칙** — Layer 경계 폐기. User의 모든 axis card를 하나의 daily 큐로 (이슈 #25).
- **`ReviewSession` 재편** — 세션 인스턴스 개념 유지. Batch 참조로 카드 pull. `INITIAL/COMPARING` 상태 유지.
- **자정 close cron** — 매일 00:05 KST 어제 batch 자동 close. 이후 markViewed 거절 → "그날 못 본 카드 = 그냥 지나감" 원칙 강제.
- **캐시 측정 대시보드 (L3)** — 오늘/7일/30일 완료율 + streak + 규칙 기반 추천 안내 (이슈 #26).
- **주간 요약 조건부 알림 (T3)** — 3주 연속 저완료율(< 50%) 감지 시 mode 다운 추천 알림 자동 발송.
- **관찰 지표 축적** — mode별 실측 완료율·streak 분포·mode 변경 이력 등 v2 튜닝 근거 데이터.

## 설계 결정 (Design Decisions)

- **DailyLearningBatch = Aggregate (vs CardExposureLog 이벤트 테이블)**
  - "그날 큐"는 명확한 도메인 개념 → Aggregate로 표현 정합.
  - 상태 관리 응집 (완료율·완료 판정·미완료 카운트). 이벤트 테이블 방식은 흩어짐.
- **`ReviewSession` 유지 (vs daily로 재해석)**
  - 사용자가 앱 껐다 켰다 하며 여러 번 학습 재개 → 세션 인스턴스 개념 자연.
  - 여러 세션 인스턴스가 같은 batch 공유.
- **Cross-Layer 짬뽕 (vs Layer별 별도 큐)**
  - 사용자 지시 명시. Layer 파편화 = 학습 리듬 방해.
  - Layer 시각화(진행률 등)는 별도 API로.
- **자정 close = 못 본 카드 그냥 지나감 (vs 누적)**
  - 오답노트 컨셉과 정합. 학습은 하루 리듬.
- **Batch 생성 = 사용자 첫 접근 시 lazy (vs 자정 배치 미리 생성)**
  - 자원 절약. 첫 접근 시 잠깐 latency 있지만 초기 3명 규모엔 무의미.
- **Streak = realtime 계산 (vs 캐시 필드)**
  - v1 초기 3명 규모 성능 부담 X. Cache stale 위험 회피.
- **캐시 측정 = L3 (대시보드 + 규칙 기반 추천 안내)**
  - L2(대시보드만) — "난이도 조절 말씀드림" 요구와 배치.
  - L4(자동 조정) — 사용자 자율성 침해. 임계값 검증 데이터 없음.
- **주간 요약 = T3 조건부 자동 (vs 매주 정기 or 요청 시)**
  - T1 — 완료율 좋은 유저에게도 알림 = 노이즈.
  - T2 — 사용자가 신호 놓침.
  - T3 — 필요할 때만 감지.

## 대안 검토 (Alternatives Considered)

### 갈림길 A. Daily 개념 도메인 표현

**Option G1 (선택) — DailyLearningBatch Aggregate + DailyCardEntry**
- 비용: 새 Aggregate + 자식 Entity 스키마. Repository·Application Service·API 세트.
- 보상: 도메인 표현력 최고. 상태 관리 응집. `findByCardId`가 카드 관점 이력도 커버.

**Option G2 — ReviewSession을 daily로 재해석**
- 장점: 스키마 최소.
- 거부 이유: 세션 개념 뒤섞임. INITIAL/COMPARING 상태 의미 애매. 여러 인스턴스 개념 상실.

**Option G3 — CardExposureLog 이벤트 테이블만**
- 장점: 스키마 최저 부담.
- 거부 이유: 그날 완료 판정 매번 쿼리로 재계산. 상태 관리 흩어짐. 도메인 표현력 낮음.

### 갈림길 B. 캐시 측정·추천 기능 수준

**Option L1 — 관찰만 (raw stats)**
- 거부 이유: "난이도 조절 말씀드림" 사용자 요구와 배치.

**Option L2 — 대시보드 + streak만**
- 거부 이유: 추천 없음. 사용자 자기 데이터 해석 필요.

**Option L3 (선택) — 대시보드 + 규칙 기반 추천 안내**
- 비용: `RecommendationEngine` 도메인 서비스 + 임계값 config.
- 보상: 시각화 + 안내. 결정은 사용자 자율. v2 임계값 튜닝 근거 확보.

**Option L4 — 자동 조정**
- 거부 이유: 사용자 자율성 침해. v1 관찰 데이터 없이 임계값 결정 어려움.

### 갈림길 C. 주간 요약 트리거

**Option T1 — 매주 정기 알림**
- 거부 이유: 완료율 좋은 유저에게도 알림 → 노이즈.

**Option T2 — 사용자 요청 시만**
- 거부 이유: 프로액티브 신호 놓칠 위험.

**Option T3 (선택) — 조건부 자동 (3주 연속 저완료율)**
- 비용: 임계값 튜닝 필요 (v1 default 50%, 3주).
- 보상: 필요할 때만. 조용하면서도 감지.

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 배치

```
┌────────────────────────────────────────────────────────────────┐
│ Presentation                                                   │
│   ReviewSessionController                                      │
│   ├ POST /review-sessions                (세션 시작)            │
│   ├ POST /review-sessions/{id}/record-view                     │
│   └ POST /review-sessions/{id}/next                            │
│                                                                │
│   DailyBatchController                                         │
│   ├ POST /daily-batch/today              (lazy 생성 or 조회)    │
│   └ GET  /daily-batch/history                                  │
│                                                                │
│   LearningDashboardController                                  │
│   ├ GET  /learning-dashboard             (오늘·7일·30일·추천)   │
│   ├ POST /learning-dashboard/recommendations/{id}/accept       │
│   └ POST /learning-dashboard/recommendations/{id}/dismiss      │
├────────────────────────────────────────────────────────────────┤
│ Application                                                    │
│   DailyLearningBatchService                                    │
│   ├ getOrCreateToday(userId): DailyLearningBatch               │
│   ├ markViewed(userId, cardId)                                 │
│   └ queryHistory(userId, dateRange)                            │
│                                                                │
│   ReviewCommandService                                         │
│   ├ startSession(userId): ReviewSession                        │
│   ├ recordView(sessionId, cardId)   (batch.markViewed 트리거)  │
│   └ finish(sessionId)                                          │
│                                                                │
│   LearningDashboardQueryService                                │
│                                                                │
│   RecommendationEngine (Domain Service)                        │
├────────────────────────────────────────────────────────────────┤
│ Domain                                                         │
│   DailyLearningBatch (Aggregate Root)                          │
│   ├ userId, batchDate (UNIQUE per user)                        │
│   ├ generatedAt, closedAt                                      │
│   ├ userModeAtGeneration: LearningMode                         │
│   ├ entries: List<DailyCardEntry>                              │
│   └ generateFor(...), markViewed(...), close(...)              │
│                                                                │
│   DailyCardEntry (Entity — 자식)                                │
│   └ batchId, cardId, cardIntervalDay, exposedAt, viewedAt?     │
│                                                                │
│   ReviewSession (Aggregate — 세션 인스턴스)                     │
│   ├ userId, dailyBatchId, startedAt, finishedAt                │
│   ├ cardReviews: List<CardReview>                              │
│   └ startFrom(batch, now), recordView(), moveToNext()          │
│                                                                │
│   BatchStreakCalculator (Domain Service, realtime)             │
├────────────────────────────────────────────────────────────────┤
│ Infrastructure                                                 │
│   DailyLearningBatchJpaRepository                              │
│   DailyCardEntryJpaRepository                                  │
│   ReviewSessionJpaRepository                                   │
└────────────────────────────────────────────────────────────────┘
```

### 핵심 플로우

**1. 사용자 첫 접근 → Daily batch lazy 생성**
```
Client → GET /daily-batch/today
Controller → DailyLearningBatchService.getOrCreateToday(userId)
Service   ├ existing = repository.findByUserAndDate(userId, today)
          │  └ 존재 → 반환
          ├ 없음 → generate:
          │   ├ userMode = UserScheduleQueryService.currentMode(userId)
          │   ├ cards = CardRepository.findAllByUserIdAndStatusOnField(userId)
          │   ├ batch = DailyLearningBatch.generateFor(userId, today, userMode, cards)
          │   │  ├ 각 card.isDueOn / hasScheduleExhausted 필터
          │   │  ├ due 카드 → entries에 추가
          │   │  └ exhausted 감지된 카드 리스트 반환
          │   ├ exhausted 카드 → CardCommandService.archiveMany(SCHEDULE_EXHAUSTED / MODE_DOWNGRADED)
          │   └ repository.save(batch)
          └ return batch (entries + completion 통계)
```

**2. Review 세션에서 카드 view 완료**
```
Client → POST /review-sessions/{id}/record-view
Session.recordView()
  ├ card.recordView() (viewCount++, lastViewedAt=now)  [관찰 지표 갱신만]
  ├ batch = repository.findById(session.dailyBatchId)
  ├ batch.markViewed(cardId, now)   [entry.viewed_at 세팅]
  ├ batch가 closed면 DailyBatchClosedException
  └ save both
```

**3. 자정 close cron (KST 00:05)**
```
@Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
public void closeYesterdaysBatches() {
  yesterday = today - 1day (KST)
  openBatches = repository.findAllOpenByBatchDate(yesterday)
  for each batch:
    batch.close(now)   // idempotent
    save
}
```

**4. 주간 요약 조건부 알림 (매주 월 09:00 KST)**
```
@Scheduled(cron = "0 0 9 ? * MON", zone = "Asia/Seoul")
public void checkWeeklyRecommendations() {
  for each activeUser:
    recent3Weeks = batchRepository.findByUserAndDateRange(userId, today-21, today-1)
    avgCompletion = mean(batch.completionRatio() for batch in recent3Weeks)
    if avgCompletion < 0.5:
      notificationService.send(userId, "SUGGEST_DOWNGRADE", ...)
    else if avgCompletion == 1.0 for last 4 weeks and userMode < MODE_60D:
      notificationService.send(userId, "SUGGEST_UPGRADE", ...)
}
```

### Out-of-Process 의존

- **Card BC (Product card)**: `Card.isDueOn`, `Card.hasScheduleExhausted`, `Card.effectiveMaxDays` — batch 큐 구성 시 소비. `CardCommandService.archiveMany(reason)` — SCHEDULE_EXHAUSTED / MODE_DOWNGRADED 처리.
- **UserSchedule BC**: `UserScheduleQueryService.currentMode(userId)` — batch 생성 및 추천 판정 시 사용자 현재 mode 조회.
- **LearningFacade BC**: `LearningAxisQueryService.findAxisIdsByUserId(userId)` — v1에서는 사용 안 함 (모든 axis card 짬뽕). v2에서 layer 시각화 시 참조 여지.
- **Notification Infra (v1 minimal)**: DB row (`user_notification`) + 프론트 polling. 실시간 push는 v2.

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | ErrorCode | HTTP | 클라이언트 권장 동작 |
| --- | --- | --- | --- |
| 자정 close 이후 markViewed 시도 | `DAILY_BATCH_CLOSED` | 409 | 새 세션 진입 유도 |
| Closed batch 상태에서 새 세션 시작 | `DAILY_BATCH_CLOSED_FOR_NEW_SESSION` | 409 | 오늘 batch 생성 유도 |
| 존재하지 않는 batch 조회 | `DAILY_BATCH_NOT_FOUND` | 404 | 오늘 batch API 재요청 |
| 존재하지 않는 세션 조회 | `REVIEW_SESSION_NOT_FOUND` | 404 | 세션 목록 재조회 |
| 진행 중 세션 있는데 새 세션 시도 | 자동 finish + 200 (정책 A) | 200 | 새 세션 진입 |
| 존재하지 않는 recommendation accept | `RECOMMENDATION_NOT_FOUND` | 404 | 대시보드 재조회 |
| 이미 dismiss된 recommendation accept | `RECOMMENDATION_ALREADY_RESOLVED` | 409 | 대시보드 재조회 |

### 로깅 정책

- **항상 기록**:
  - Batch 생성 이벤트 (`userId`, `batchDate`, `entriesCount`, `userModeAtGeneration`, `generatedAt`)
  - Batch close 이벤트 (`userId`, `batchDate`, `viewedCount`, `totalCount`, `completionRatio`, `closedAt`)
  - Batch 생성 시 archive된 카드 (`userId`, `cardIds`, `reason`)
  - Recommendation 발동 이벤트 (`userId`, `type`, `fromMode`, `toMode`, `avgCompletionRatio`, `triggeredAt`)
  - Recommendation accept/dismiss 이벤트 (`userId`, `recommendationId`, `action`, `resolvedAt`)
- **debug**:
  - Streak realtime 계산 중간값 (반복 조회 성능 이슈 감지 시)
  - Batch generateFor 필터 통과 카드 수 (isDueOn true / hasScheduleExhausted true 각각)
- **절대 금지**:
  - Card 콘텐츠 원문 (MainNote text) — cardId만 사용
  - 사용자 PII (이메일 등) — userId만
  - 추천 알림 원문 통째 로깅 — 알림 타입·수치만

### 관측 지표

- `thirdtool.daily_batch.generated_total` — Counter — Batch 생성 이벤트
- `thirdtool.daily_batch.closed_total` — Counter — Batch close 이벤트
- `thirdtool.daily_batch.completion_ratio_histogram{user_mode}` — Histogram — Mode별 완료율 분포
- `thirdtool.daily_batch.entries_count_histogram{user_mode}` — Histogram — Mode별 batch 크기
- `thirdtool.review_session.started_total` — Counter — 세션 시작 이벤트
- `thirdtool.review_session.finished_total{reason}` — Counter — 종료 이벤트 (`user_finish` / `auto_finish_new_session`)
- `thirdtool.streak.current_histogram` — Histogram — Current streak 분포
- `thirdtool.recommendation.suggested_total{type, from_mode, to_mode}` — Counter — 추천 발동
- `thirdtool.recommendation.accepted_total` / `.dismissed_total` — Counter — 추천 응답

## 롤아웃 / 마이그레이션 (Rollout)

### 전제

- 초기 3명 사용자 규모, 트래픽 극소량 — 데이터 마이그레이션 breaking 감내 가능.
- `product-card.md` Epic 1~3 완료 필수 (Card 도메인 메서드가 batch generateFor 소스).
- Deck 폐기(이슈 #13) 완료 or 병행. 기존 `/decks/{id}/review-sessions` 엔드포인트 410 Gone.

### Product 의존성

- **선행**: `product-card.md` (Card lifecycle · `createdMode` · 도메인 메서드), `product-learning-tower.md` (axis 구조)
- **후행**: `product-fe-cdn.md` (대시보드 UX), `product-op.md` (notification 채널 + 관측)

### Epic·Story 의존성 그래프

```
Epic 1 (DailyLearningBatch)  ──►  Epic 2 (ReviewSession cross-layer 재편)
     │                                     │
     ├─► Story 1-1 (Flyway 스키마)          ├─► Story 2-1 (startFrom 팩토리)
     ├─► Story 1-2 (Aggregate + Entity)     ├─► Story 2-2 (recordView batch 동기화)
     ├─► Story 1-3 (Service·API)            ├─► Story 2-3 (동시 세션 정책)
     ├─► Story 1-4 (generateFor + archive)  ├─► Story 2-4 (엔드포인트 재편)
     ├─► Story 1-5 (자정 close cron)        ├─► Story 2-5 (StateRecommendationDistributor 폐기)
     ├─► Story 1-6 (BatchStreakCalculator)  ├─► Story 2-6 (Repository 쿼리 재편)
     ├─► Story 1-7 (엔드포인트 세트)         └─► Story 2-7 (통합 테스트)
     └─► Story 1-8 (ErrorCode)                    │
                    │                              ▼
                    │                          Epic 3 (캐시 측정 대시보드)
                    ▼                              │
                Epic 3 소비                        ├─► Story 3-1 ~ 3-8

병렬: Epic 1 Story 1-5·1-6·1-8은 Story 1-1~1-4 완료 후 병렬 가능
```

### 환경별 설정 분기

| 항목 | dev | prod |
| --- | --- | --- |
| DB | H2 (in-memory) | MySQL 8.0 + RDS |
| Timezone | 시스템 KST | KST (`spring.jackson.time-zone=Asia/Seoul`) |
| Cron: 자정 close | 활성 (매일 00:05 KST) | 활성 (매일 00:05 KST) |
| Cron: 주간 요약 | 비활성 (수동 트리거 API 노출) | 활성 (매주 월 09:00 KST) |
| Notification 발송 | 로그만 (실제 발송 X) | DB row 삽입 (프론트 polling) |
| 저완료율 임계값 (`app.learning.recommendation.downgrade-threshold-ratio`) | `0.5` | `0.5` (환경변수 오버라이드 가능) |
| Streak 계산 방식 | realtime | realtime (v2에 캐시 도입 검토) |

## 성공 지표 (KPI)

| 지표 | 목표 값 | 측정 방법 |
| --- | --- | --- |
| 사용자별 daily batch 생성률 | ≥ 80% (활성 사용자가 하루 한 번은 진입) | `thirdtool.daily_batch.generated_total` / 활성 사용자 수 |
| Batch 평균 완료율 (전체) | 관찰 (v1 baseline 없음) | 대시보드 API `avgCompletionRatio` |
| Streak 확인율 (사용자가 대시보드에서 조회 비율) | ≥ 50% | FE 이벤트 (`dashboard_view` 이벤트) |
| 추천 발동 → 수용 전환율 | 관찰 (v1 baseline 없음) | `recommendation.accepted_total / suggested_total` |
| 자정 close cron 성공률 | 100% | `thirdtool.daily_batch.closed_total` vs 예상값 (사용자 수 × active 일수) |

## Scope

**In Scope**:
- `DailyLearningBatch` Aggregate + `DailyCardEntry` 자식 Entity
- 자정 close cron
- `ReviewSession` cross-layer 재편 (batch 참조로 카드 pull)
- Streak realtime 계산
- 캐시 측정 대시보드 (오늘/7일/30일 + streak + recommendation)
- 규칙 기반 추천 안내 (SUGGEST_DOWNGRADE/UPGRADE)
- 조건부 주간 요약 알림 (매주 월 09:00 KST)
- 관찰 지표 로깅 및 Metrics 노출
- In-app notification (DB row + 프론트 polling — v1 minimal)

**Out of Scope**:
- Card lifecycle 재정의 — `product-card.md`에 분리
- Mode enum 정의 — `product-card.md` Epic 1 (이슈 #21)
- 자동 mode 조정 — 사용자 자율성·검증 데이터 필요, v2 이관
- 실시간 push notification — 인프라 별도 이슈 (v2)
- 카드별 hint 히스토리 — 이슈 #17·#18에서 다룸 (v2)
- 대시보드 그래프 차트 세부 UX — FE 별도 spec
- Layer 시각화·진행률 API — v2 (사용자 니즈 확인 후)

## 대상 사용자

- **오답노트 학습자 (내부 테스트 3명)** — 그날 due 카드를 확실히 소진하고 싶은 사람. Layer/axis 별 파편화보다 통합 큐 선호.
- **자기 학습 캐시 궁금해하는 사람** — 오늘 completed/total·streak를 대시보드에서 확인. 3주 데이터로 mode 조정 안내 받기.
- **자기 능력 과대평가 성향 사용자** — 시스템이 실측 기반으로 "너 mode 다운 필요할 수도" 안내. 결정은 사용자 자율.

## 연결된 Epic 목록

- [ ] Epic 1: `DailyLearningBatch` Aggregate + `DailyCardEntry` (이슈 #24)
- [ ] Epic 2: `ReviewSession` cross-layer 재편 (이슈 #25)
- [ ] Epic 3: 캐시 측정 대시보드 + 주간 요약 조건부 알림 (이슈 #26)

## 관련 문서

- 의존 Product:
  - `workflow/task/pes/workspectrum/sdd/in-progress/product-card.md` — Card lifecycle · `createdMode` · 도메인 메서드
  - `workflow/task/pes/workspectrum/sdd/in-progress/product-learning-tower.md` — Layer/Axis 구조 (v1엔 짬뽕이라 axis 필터만 사용)
- 관련 이슈: `workflow/task/fix/brainstorming/version/0.0.2v/issue-24` / `#25` / `#26`
- 참조 (원본 v0.0.1): `workflow/task/pes/workspectrum/sdd/done/versions/0.0.1v/product-card.md` (원래 review 개념이 붙어있었음)
- 관련 ADR: ADR021 (Card·Deck·Axis 통합), ADR022 (용어) — 본 리팩토링으로 신설 ADR 검토 (`ADR-CANDIDATES.md` 등록)
- DOMAIN.md 갱신 예정: `docs/DOMAIN.md` §Review 섹션 신설 (오답노트·캐시 용량 측정 도메인 의도)

## 열린 질문 (Open Questions)

1. **In-app notification 인프라** — v1 minimal은 DB row + 프론트 polling. 실시간 push (WebSocket / FCM)는 별도 이슈로 분리.
2. **저완료율 임계값 튜닝** — v1 default 50%·3주는 감. 3명 관찰 데이터로 v2 임계값·window 조정.
3. **Batch 크기 상한** — 사용자가 오래 활동 후 카드 수백 장 쌓이면 batch entries 100+ 될 수 있음. 큐 표시 페이지네이션 도입 시점 (관찰 지표에서 감지 시).
4. **Card 배분 전략** — v1 policy: batch 순서 = `card_interval_day ASC · exposed_at ASC`. v2에 랜덤·view 이력·난이도 기반 옵션 도입 여지.
5. **Streak realtime → 캐시 전환 시점** — 사용자 수 증가 시 매 조회 batch 시퀀스 스캔 비용 증가. 임계값(사용자 100명+)에 캐시 필드 도입 검토.
6. **User_notification 스키마 위치** — Review BC vs Common BC vs 별도 Notification BC 신설. v1엔 Review BC 안에 두고 v2 분리 판단.

## 제품 수준 완료 기준 (Product-level DoD)

- [ ] 이슈 #24, #25, #26 모두 구현·테스트 완료
- [ ] `DailyLearningBatch` UNIQUE `(user_id, batch_date)` DB 제약 검증
- [ ] 자정 close cron 정상 동작 (KST 매일 00:05)
- [ ] Cross-layer 짬뽕 규칙 통합 테스트 (여러 axis에 카드가 있을 때 batch에 모두 포함)
- [ ] M3 하이브리드 down cap 시나리오 (mode down → 다음 batch에서 이미 넘긴 card archive) 통합 테스트
- [ ] 대시보드 API 응답 형식 검증
- [ ] 주간 요약 cron 조건부 알림 로직 검증 (3주 저완료율 발동, 그 외 조용)
- [ ] Reviewer 5관점 세션 통과
- [ ] 관련 문서 정합 (`product-card.md`, 이슈들)

---

# [Epic 1] `DailyLearningBatch` Aggregate + `DailyCardEntry` 자식

## 목표

하루당 1개 `DailyLearningBatch` Aggregate를 신설해 그날 큐(cross-layer 짬뽕)·진행 상태·카드 관점 이력을 단일 도메인으로 응집시키고, 자정 close cron으로 "그날 못 본 카드 = 그냥 지나감" 원칙을 강제한다.

## 배경

이슈 #24의 핵심. 지금은 daily 개념 자체가 없어서 (i) 세션 간 카드 재노출 dedup 없음 (ii) 그날 clear율 계산 불가 (iii) 카드 관점 노출 이력 조회 불가. 이 Epic 완료 시 Epic 2가 batch를 참조해 세션 구성 가능.

## 포함 Story

- Story 1-1: Flyway `V{N}__daily_learning_batch.sql` — 스키마 신설 + 롤백
- Story 1-2: `DailyLearningBatch` Aggregate + `DailyCardEntry` 자식 Entity 도메인 신설
- Story 1-3: Repository + Application Service (`getOrCreateToday`, `markViewed`, `queryHistory`)
- Story 1-4: `generateFor()` orchestration (SCHEDULE_EXHAUSTED / MODE_DOWNGRADED archive 부수 처리)
- Story 1-5: 자정 close cron (`0 5 0 * * *` KST)
- Story 1-6: `BatchStreakCalculator` 도메인 서비스 (realtime)
- Story 1-7: 엔드포인트 세트 (`POST /daily-batch/today`, `GET /daily-batch/history`)
- Story 1-8: ErrorCode 신설 (`DAILY_BATCH_CLOSED`, `DAILY_BATCH_NOT_FOUND`)

## Epic 인수 시나리오

- Given 오늘 처음 접근한 사용자 A + user mode=MODE_14D + due 카드 5장
- When `POST /daily-batch/today`
- Then batch 신규 생성, entries 5개, completionRatio=0.0

- Given batch 생성 후 오늘 다시 `POST /daily-batch/today`
- When 조회
- Then 기존 batch 반환 (신규 생성 없음)

- Given 어제 open 상태 batch + 자정 지남
- When 자정 cron 실행 (00:05 KST)
- Then batch.closedAt 설정, isClosed() true

*(엣지)* Given closed batch에 markViewed / When 시도 / Then `DAILY_BATCH_CLOSED` 409

## Epic 완료 기준 (DoD)

- [ ] 포함 Story 모두 완료
- [ ] `daily_learning_batch` UNIQUE `(user_id, batch_date)` 강제 검증
- [ ] `daily_card_entry` FK `batch_id` CASCADE 검증
- [ ] 자정 close cron 정상 동작
- [ ] Streak realtime 계산 성능 검증 (3명 사용자 규모)
- [ ] ADR 신설 검토 (Daily batch 개념 도입 근거)

## Epic 기술 결정 / 대안

- **Batch 생성 시점: lazy vs 자정 배치**: lazy 채택. 자원 절약 + 안 오는 사용자 batch 안 만듦.
- **DailyCardEntry의 카드 관점 이력 역할**: `findByCardId(cardId)` 로 카드 노출 이력 조회 커버. 별도 `CardExposureLog` 테이블 신설 안 함.
- **generateFor의 archive orchestration 위치**: `DailyLearningBatchService`가 `CardCommandService.archiveMany()` 호출. Aggregate가 직접 다른 BC 호출 안 함 (Aggregate는 Repository도 호출 안 함 원칙).

## [Story 1-1] Flyway `V{N}__daily_learning_batch.sql` + 롤백

### User Story
- As a 운영자
- I want daily_learning_batch·daily_card_entry 두 테이블이 안전 신설되기를
- so that Aggregate 스키마 원천 확보

### 설명
2개 테이블 신설:
- `daily_learning_batch(id, user_id, batch_date, generated_at, closed_at, user_mode_at_generation)` — UNIQUE `(user_id, batch_date)`, mode CHECK
- `daily_card_entry(id, batch_id, card_id, card_interval_day, exposed_at, viewed_at?)` — UNIQUE `(batch_id, card_id)`, FK batch_id CASCADE

인덱스: `idx_daily_batch_user_date`, `idx_entry_card`, `idx_entry_batch_viewed`.

**핵심 파일**:
- `V{N}__daily_learning_batch.sql`
- `R{N}__rollback_daily_learning_batch.sql`

### 완료 기준 (AC)
- Given migrate 실행 / When 완료 / Then 두 테이블 생성 + 인덱스 3개 + UNIQUE·CHECK 제약 활성
- Given `INSERT ... user_mode_at_generation='MODE_INVALID'` / Then CHECK 위반
- Given 동일 user+date 두 번 INSERT / Then UNIQUE 위반
- Given `DELETE ... daily_learning_batch WHERE id=X` / Then 자식 entries CASCADE
- Given 롤백 스크립트 / When 실행 / Then 두 테이블 DROP

### Definition of Done
- [ ] 구현 (V·R 두 스크립트)
- [ ] Slice 테스트 (`@DataJpaTest`) — UNIQUE·CHECK·CASCADE 검증
- [ ] 통합 테스트 (`@SpringBootTest`) — 롤백 성공

### 스토리 포인트
1d

### 의존성
- 선행: `product-card.md` Epic 1~3 완료 (LearningMode 4개 값 확정)
- 후행: Story 1-2

## [Story 1-2] `DailyLearningBatch` Aggregate + `DailyCardEntry` 자식 Entity 도메인 신설

### User Story
- As a Review BC 개발자
- I want DailyLearningBatch Aggregate + DailyCardEntry 자식 Entity가 도메인 메서드로 상태 관리하기를
- so that Aggregate 응집 (generate·markViewed·close·completionRatio·isPerfectClear)

### 설명
`Review/domain/model/DailyLearningBatch.java` (Aggregate Root), `DailyCardEntry.java` (자식 Entity) 신설. `orphanRemoval=true` cascade. 도메인 메서드: `generateFor(userId, today, userMode, allUserCards)`, `markViewed(cardId, viewedAt)`, `close(closedAt)`, `completionRatio()`, `isPerfectClear()`, `totalCount()`, `viewedCount()`.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Review.domain.model.DailyLearningBatch` — Aggregate Root
- `com.example.thirdtool.Review.domain.model.DailyCardEntry` — 자식 Entity

**주요 메서드**:
- `DailyLearningBatch.generateFor(userId, today, userCurrentMode, allUserCards): DailyLearningBatch + List<Card> exhaustedCards` — 튜플 반환 (exhausted 처리는 Application에서)
- `markViewed(cardId, viewedAt): void`
- `close(closedAt): void`
- `completionRatio(): double`, `isPerfectClear(): boolean`

### 완료 기준 (AC)
- Given cards 5장 (3장 due, 2장 exhausted) / When `generateFor(userId, today, mode, cards)` / Then entries 3개 + exhaustedCards 2개
- Given batch 생성 (entries 3개) / When `markViewed(cardId, now)` 2번 (다른 카드) / Then completionRatio=2/3
- Given open batch / When `close(now)` / Then closedAt=now, isClosed()=true, 이후 `markViewed` 호출 시 예외
- *(엣지)* Given closed batch / When `close(now)` 재호출 / Then no-op (idempotent)
- *(엣지)* Given entries=0 batch / When `completionRatio()` / Then 1.0 (0으로 나누기 방지)

### Definition of Done
- [ ] 구현 (도메인 두 클래스 + 도메인 메서드)
- [ ] 단위 테스트 (`DailyLearningBatchTest`, `DailyCardEntryTest`, 해피/엣지/예외 각 케이스)
- [ ] `orphanRemoval=true` JPA 매핑

### 스토리 포인트
2d

### 의존성
- 선행: Story 1-1, `product-card.md` Epic 3 Story 3-2 (Card 도메인 메서드)
- 후행: Story 1-3

## [Story 1-3] Repository + Application Service (`getOrCreateToday`, `markViewed`, `queryHistory`)

### User Story
- As a Review BC 개발자
- I want DailyLearningBatchService가 lazy 생성·마킹·이력 조회 API 노출하기를
- so that Presentation·Review 세션이 이 서비스 통해 batch 접근

### 설명
`DailyLearningBatchRepository` port + JPA Adapter. `DailyLearningBatchService`가 `getOrCreateToday(userId)`, `markViewed(userId, cardId, viewedAt)`, `queryHistory(userId, dateRange)` 노출. Lazy 생성 시 Story 1-4의 orchestration 호출.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Review.infrastructure.persistence.DailyLearningBatchRepository` (port + adapter)
- `com.example.thirdtool.Review.application.service.DailyLearningBatchService`

**주요 메서드**:
- `Repository.findByUserAndDate(userId, date): Optional<DailyLearningBatch>`
- `Repository.findAllOpenByBatchDate(date): List<DailyLearningBatch>`
- `Repository.findByUserAndDateRange(userId, from, to): List<DailyLearningBatch>`
- `Service.getOrCreateToday(userId): DailyLearningBatch`
- `Service.markViewed(userId, cardId, viewedAt): void`
- `Service.queryHistory(userId, from, to): List<DailyLearningBatch>`

### 완료 기준 (AC)
- Given 오늘 batch 없음 / When `getOrCreateToday(userId)` / Then 신규 생성 후 반환
- Given 오늘 batch 있음 / When `getOrCreateToday(userId)` / Then 기존 반환 (신규 생성 없음)
- Given closed batch + markViewed / When 호출 / Then `DAILY_BATCH_CLOSED` 409
- Given dateRange 조회 / When `queryHistory(userId, from, to)` / Then 해당 기간 batch 리스트 (created_at DESC)

### Definition of Done
- [ ] 구현 (Repository port + JPA adapter, Service)
- [ ] Slice 테스트 (`@DataJpaTest` — Repository 쿼리)
- [ ] 단위 테스트 (Service — Repository Mock)

### 스토리 포인트
2d

### 의존성
- 선행: Story 1-2
- 후행: Story 1-4, 1-7

## [Story 1-4] `generateFor()` orchestration + archive 부수 처리

### User Story
- As a Review BC 개발자
- I want getOrCreateToday이 batch 생성 시 exhausted 카드를 자동 archive하기를
- so that lazy 처리로 별도 정리 배치 불필요

### 설명
`DailyLearningBatchService.getOrCreateToday()`가 batch 생성 후 exhaustedCards 리스트를 `CardCommandService.archiveMany(cardIds, reason)` 로 위임. Reason 판정: `card.createdMode.maxDays() > user.currentMode.maxDays()` → `MODE_DOWNGRADED`, 그 외 → `SCHEDULE_EXHAUSTED`.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Review.application.service.DailyLearningBatchService`

**주요 메서드**:
- `Service.getOrCreateToday(userId): DailyLearningBatch` — orchestration 확장
- `CardCommandService.archiveMany(cardIds: List<Long>, reason: ArchiveReason): void`

### 완료 기준 (AC)
- Given card `createdMode=MODE_14D` + user `MODE_7D` + card `enteredFieldAt=today-10` / When batch 생성 / Then card archive with `MODE_DOWNGRADED`
- Given card `createdMode=MODE_7D` + user `MODE_7D` + card `enteredFieldAt=today-8` / When batch 생성 / Then card archive with `SCHEDULE_EXHAUSTED`
- Given exhausted 카드 3개 감지 / When 배치 생성 / Then 3개 archive 후 batch entries 나머지만 포함

### Definition of Done
- [ ] 구현 (orchestration 확장, `CardCommandService.archiveMany` 신설)
- [ ] 통합 테스트 (`@SpringBootTest`) — 두 reason 시나리오 검증
- [ ] Card BC `CardCommandService.archiveMany` 시그니처 추가

### 스토리 포인트
2d

### 의존성
- 선행: Story 1-3, `product-card.md` Epic 3 Story 3-2 (Card 도메인 메서드)
- 후행: Epic 2 Story 2-1

## [Story 1-5] 자정 close cron (`0 5 0 * * *` KST)

### User Story
- As a 시스템
- I want 매일 00:05 KST 어제 open batch를 자동 close하기를
- so that "그날 못 본 카드 = 그냥 지나감" 원칙 강제

### 설명
`@Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")` — `Repository.findAllOpenByBatchDate(yesterday)` → 각 batch `close(now)` 호출.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Review.application.scheduler.DailyBatchCloseScheduler`

**주요 메서드**:
- `closeYesterdaysBatches(): void`

### 완료 기준 (AC)
- Given 어제 open batch 3개 / When cron 실행 (00:05 KST) / Then 3개 모두 closedAt 세팅
- Given 어제 이미 closed batch / When cron 실행 / Then no-op (idempotent)
- *(엣지)* Given 어제 batch 없음 / When cron 실행 / Then no-op, 로그만

### Definition of Done
- [ ] 구현 (`DailyBatchCloseScheduler`)
- [ ] 단위 테스트 (Repository Mock)
- [ ] 통합 테스트 — 실제 cron 트리거 (테스트에선 수동 호출)
- [ ] dev 환경 활성, prod 환경 활성 설정 검증

### 스토리 포인트
1d

### 의존성
- 선행: Story 1-3
- 후행: 없음

## [Story 1-6] `BatchStreakCalculator` 도메인 서비스 (realtime)

### User Story
- As a Review BC 개발자
- I want BatchStreakCalculator가 사용자별 current streak을 realtime 계산하기를
- so that 대시보드가 캐시 필드 없이 streak 표시

### 설명
`BatchStreakCalculator.currentStreak(userId, asOf)` — asOf부터 뒤로 하루씩 조회, `batch.isPerfectClear()` true 연속 카운트. break되는 순간 종료.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Review.domain.service.BatchStreakCalculator`

**주요 메서드**:
- `currentStreak(userId: Long, asOf: LocalDate): int`
- `longestStreak(userId: Long, asOf: LocalDate, window: int): int` — 최근 window 일 내 최장

### 완료 기준 (AC)
- Given 최근 3일 배치 [perfect, perfect, perfect] / When `currentStreak(userId, today)` / Then 3
- Given 최근 5일 [perfect, perfect, incomplete, perfect, perfect] / When `currentStreak(userId, today)` / Then 2 (오늘부터 역행)
- Given 오늘 batch 없음 / When `currentStreak(userId, today)` / Then 0
- *(엣지)* Given 특정 날짜 batch 없음 (break) / When 역행 조회 / Then 그 지점 이전까지만 카운트

### Definition of Done
- [ ] 구현 (`BatchStreakCalculator`)
- [ ] 단위 테스트 (streak 시나리오 3+ 케이스)
- [ ] Repository 쿼리 최적화 (dateRange bulk 조회로 N+1 방지)

### 스토리 포인트
1d

### 의존성
- 선행: Story 1-3
- 후행: Epic 3 Story 3-1

## [Story 1-7] 엔드포인트 세트

### User Story
- As a 프론트엔드
- I want batch 조회·이력 조회 API를 통해 대시보드 데이터 접근하기를
- so that 사용자에게 그날 진행 상황 표시

### 설명
`DailyBatchController` 신설. `POST /api/v1/daily-batch/today`, `GET /api/v1/daily-batch/history?from=YYYY-MM-DD&to=YYYY-MM-DD`.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Review.presentation.DailyBatchController`
- `DailyBatchResponse(batchDate, generatedAt, closedAt?, isClosed, totalCards, viewedCards, completionRatio, entries: List<DailyCardEntryResponse>)`
- `DailyCardEntryResponse(cardId, cardIntervalDay, exposedAt, viewedAt?)`

### 완료 기준 (AC)
- Given `POST /daily-batch/today` / When 첫 호출 / Then 201 + batch 생성 응답
- Given 재호출 / Then 200 + 기존 batch
- Given `GET /daily-batch/history?from=2026-06-01&to=2026-06-30` / Then 200 + 해당 기간 batch 리스트
- *(예외)* 인증 없이 호출 / Then 401

### Definition of Done
- [ ] 구현 (Controller + DTO)
- [ ] 슬라이스 테스트 (`@WebMvcTest`) — 응답 형식 · HTTP 상태 검증
- [ ] OpenAPI 문서 자동 반영

### 스토리 포인트
1d

### 의존성
- 선행: Story 1-3
- 후행: FE 대시보드 연결

## [Story 1-8] ErrorCode 신설 (`DAILY_BATCH_CLOSED`, `DAILY_BATCH_NOT_FOUND`)

### User Story
- As a 프론트엔드
- I want batch 관련 에러 상태를 명확한 ErrorCode로 받기를
- so that UI가 적절한 안내 표시

### 설명
`Common/Exception/ErrorCode/ErrorCode.java` 확장. HTTP 매핑 `GlobalExceptionHandler`에서 처리.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode`

### 완료 기준 (AC)
- Given closed batch에 markViewed / When 호출 / Then 응답 `{"code":"DAILY_BATCH_CLOSED", "message":"오늘 학습 세션이 종료되었습니다."}` 409
- Given 존재하지 않는 batchId 조회 / When 호출 / Then `DAILY_BATCH_NOT_FOUND` 404

### Definition of Done
- [ ] ErrorCode 2건 등록 + 한국어 메시지
- [ ] GlobalExceptionHandler 매핑 검증
- [ ] Controller Slice 테스트에서 응답 형식 검증

### 스토리 포인트
0.5d

### 의존성
- 선행: Story 1-2 (DailyBatchClosedException 정의)
- 후행: 없음

---

# [Epic 2] `ReviewSession` cross-layer 재편

## 목표

`ReviewSession`을 `DailyLearningBatch` 참조로 카드를 pull하도록 재편해 layer 경계 없이 하나의 daily 큐에서 세션 인스턴스가 자란다. 기존 deck 스코프·layer 스코프 개념 폐기.

## 배경

이슈 #25의 핵심. Deck 폐기(#13)와 cross-layer 짬뽕(사용자 확정) 결정으로 세션 구성 기반이 재편 필요. `ReviewSession(INITIAL/COMPARING)` 상태·CardReview 서브 도메인은 유지하되 카드 pool 원천만 batch로 바꾼다.

## 포함 Story

- Story 2-1: `ReviewSession.startFrom(batch, now)` 팩토리 재작성 + 기존 `startReview(deckId)` 폐기
- Story 2-2: `ReviewSession.recordView()` — batch 동기화 로직 추가
- Story 2-3: 동시 세션 정책 — 새 세션 시작 시 진행 중 세션 자동 finish
- Story 2-4: 신규 엔드포인트 세트 + 기존 `/decks/{deckId}/review-sessions` 410 Gone
- Story 2-5: `StateRecommendationDistributor` 폐기 (또는 v2 재도입 backlog)
- Story 2-6: `ReviewSessionRepository` 쿼리 재편 (`findAllByDeckId` 삭제, `findLatestActiveByUserId` 신설)
- Story 2-7: 통합 테스트 — batch ↔ session 동기화 검증

## Epic 인수 시나리오

- Given batch 5장 (전부 미완료) / When `POST /review-sessions` / Then 세션 신규 생성 + cardReviews 5개
- Given 세션 A 진행 중 (2장 봄) / When `POST /review-sessions` 다시 호출 / Then 세션 A 자동 finish + 세션 B 신규 (batch 미완료 3장으로)
- Given 세션 A에서 카드 X 완료 / When 세션 B 시작 / Then cardReviews에 X 미포함

*(엣지)* Given closed batch / When `POST /review-sessions` / Then `DAILY_BATCH_CLOSED_FOR_NEW_SESSION` 409

## Epic 완료 기준 (DoD)

- [ ] 포함 Story 모두 완료
- [ ] 기존 `startReview(deckId)` 참조 제거
- [ ] `/decks/{deckId}/review-sessions` 410 Gone 응답
- [ ] batch ↔ session 동기화 통합 테스트
- [ ] `StateRecommendationDistributor` 삭제 or backlog 이관 결정

## Epic 기술 결정 / 대안

- **동시 세션 정책**: 자동 finish 채택. 사용자가 앱 재실행하면 자연스럽게 새 세션 시작. 예외 (`REVIEW_SESSION_ALREADY_ACTIVE`) 대안은 마찰 큼.
- **CardReview 서브 도메인 유지**: RECALLING/COMPARING 흐름은 UX 요소로 유지. Batch 참조로 바뀌어도 세션 내부 카드 표현은 동일.
- **`StateRecommendationDistributor` 처리**: 폐기 (v1). Batch entries가 이미 그날 due 카드만 담음 → 카테고리 배분 불필요. v2에 다양한 카드 정렬 전략 재도입 여지.

## [Story 2-1] `ReviewSession.startFrom(batch, now)` 팩토리 재작성 + 기존 폐기

### User Story
- As a Review BC 개발자
- I want ReviewSession이 batch를 원천으로 세션 인스턴스 생성하기를
- so that layer 짬뽕 규칙 반영

### 설명
`ReviewSession.startFrom(batch, now)` 신설. Batch의 `entries.filter(!viewed)` → `CardReview` 리스트로 변환. 기존 `ReviewSession.of(deckId, ...)` 시그니처 폐기.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Review.domain.model.ReviewSession`

**주요 메서드**:
- `ReviewSession.startFrom(batch: DailyLearningBatch, now: LocalDateTime): ReviewSession`

### 완료 기준 (AC)
- Given batch 5장 (전부 미완료) / When `startFrom(batch, now)` / Then session 생성, cardReviews 5개, currentIndex=0
- Given batch 5장 (2장 이미 viewed) / When `startFrom(batch, now)` / Then cardReviews 3개
- *(예외)* Given closed batch / When `startFrom(closedBatch, now)` / Then `DailyBatchClosedException`

### Definition of Done
- [ ] 구현 (팩토리 재작성, 기존 시그니처 삭제)
- [ ] 단위 테스트 (해피/엣지/예외 각 케이스)
- [ ] 기존 `startReview(deckId)` 호출부 정리

### 스토리 포인트
1d

### 의존성
- 선행: Epic 1 Story 1-2
- 후행: Story 2-2, 2-4

## [Story 2-2] `ReviewSession.recordView()` — batch 동기화 로직 추가

### User Story
- As a Review BC 개발자
- I want recordView가 card + batch 두 곳에 갱신하기를
- so that batch의 진행률이 실시간 동기화

### 설명
`ReviewCommandService.recordView(sessionId, cardId)` — `card.recordView()` + `dailyBatchService.markViewed(userId, cardId, now)` 호출. 트랜잭션 하나로 묶음.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Review.application.ReviewCommandService`

**주요 메서드**:
- `recordView(sessionId: Long, cardId: Long): void`

### 완료 기준 (AC)
- Given 진행 중 세션 + open batch / When `recordView` / Then card.viewCount++ + batch.entry.viewedAt=now
- Given closed batch / When `recordView` / Then 세션은 진행되지만 batch 갱신 실패 → 트랜잭션 롤백 + `DAILY_BATCH_CLOSED` 409
- *(엣지)* Given ARCHIVE 카드에 recordView / Then no-op (idempotent)

### Definition of Done
- [ ] 구현 (Service 재작성)
- [ ] 통합 테스트 (batch ↔ session 동기화 검증)
- [ ] 트랜잭션 경계 검증 (batch 갱신 실패 시 card 갱신도 롤백)

### 스토리 포인트
1d

### 의존성
- 선행: Story 2-1, Epic 1 Story 1-3
- 후행: Story 2-4

## [Story 2-3] 동시 세션 정책 — 새 세션 시작 시 진행 중 세션 자동 finish

### User Story
- As a 사용자
- I want 앱 재실행 시 이전 세션이 자동 정리되기를
- so that 매번 새 세션 시작 UX 자연스러움

### 설명
`ReviewCommandService.startSession(userId)` — Repository로 진행 중 세션 조회 (`finishedAt IS NULL`) → 있으면 자동 finish → 새 세션 생성.

**주요 메서드**:
- `startSession(userId: Long): ReviewSession`

### 완료 기준 (AC)
- Given 진행 중 세션 A / When `startSession(userId)` / Then 세션 A `finishedAt=now`, 세션 B 신규 생성
- Given 진행 중 세션 없음 / When `startSession(userId)` / Then 세션 B 바로 생성
- Given batch 없음 / When `startSession(userId)` / Then batch 먼저 lazy 생성 후 세션 생성

### Definition of Done
- [ ] 구현 (`ReviewCommandService.startSession`)
- [ ] 통합 테스트 (동시 세션 시나리오)
- [ ] Metrics 관찰 지표 로깅 (`review_session.finished_total{reason=auto_finish_new_session}`)

### 스토리 포인트
1d

### 의존성
- 선행: Story 2-1, 2-2
- 후행: Story 2-4

## [Story 2-4] 신규 엔드포인트 세트 + 기존 폐기 (410 Gone)

### User Story
- As a 프론트엔드
- I want 신 세션 API를 통해 세션 시작·view 기록·이동 하기를
- so that layer 짬뽕 큐로부터 학습 진행

### 설명
`ReviewSessionController` 재작성. 신규:
- `POST /api/v1/review-sessions` (세션 시작)
- `GET /api/v1/review-sessions/{id}`
- `POST /api/v1/review-sessions/{id}/start-comparing`
- `POST /api/v1/review-sessions/{id}/record-view`
- `POST /api/v1/review-sessions/{id}/next`
- `POST /api/v1/review-sessions/{id}/finish`

폐기: `/api/v1/decks/{deckId}/review-sessions` → 410 Gone.

### 완료 기준 (AC)
- Given 사용자 인증 / When `POST /api/v1/review-sessions` / Then 200 + 세션 응답
- Given 폐기된 엔드포인트 호출 / When `POST /api/v1/decks/{deckId}/review-sessions` / Then 410
- *(예외)* closed batch로 세션 시작 / Then `DAILY_BATCH_CLOSED_FOR_NEW_SESSION` 409

### Definition of Done
- [ ] 구현 (Controller 재작성, 폐기 endpoint 410 반환)
- [ ] 슬라이스 테스트 (`@WebMvcTest`) — 각 엔드포인트 검증
- [ ] OpenAPI 갱신

### 스토리 포인트
2d

### 의존성
- 선행: Story 2-1, 2-2, 2-3
- 후행: FE 연결

## [Story 2-5] `StateRecommendationDistributor` 폐기 결정

### User Story
- As a Review BC 개발자
- I want StateRecommendationDistributor 삭제 or v2 backlog 이관 결정을 확정하기를
- so that 코드베이스 불필요 코드 정리

### 설명
`Review/domain/model/StateRecommendationDistributor.java` 검토 → v1엔 batch entries가 이미 필터·정렬되므로 배분 로직 불필요. **삭제** 채택. 필요 시 v2에 카드 정렬 전략(랜덤·난이도 등) 재도입 별도 이슈로.

### 완료 기준 (AC)
- Given `StateRecommendationDistributor` 참조 / When compile / Then 오류 (클래스 삭제)
- Given `Grep StateRecommendationDistributor src/main` / Then 0건

### Definition of Done
- [ ] 삭제 (`StateRecommendationDistributor.java`)
- [ ] 관련 테스트 삭제 (`StateRecommendationDistributorTest`)
- [ ] v2 backlog 항목 등록 (카드 정렬 전략 v2 이슈)

### 스토리 포인트
0.5d

### 의존성
- 선행: Story 2-1
- 후행: 없음

## [Story 2-6] `ReviewSessionRepository` 쿼리 재편

### User Story
- As a Review BC 개발자
- I want Repository가 batch 참조 기반 쿼리 세트로 재편되기를
- so that deck 참조 잔재 제거

### 설명
`ReviewSessionRepository`:
- 삭제: `findAllByDeckId`, `findByDeckIdAndUserId`
- 신설: `findLatestActiveByUserId(userId): Optional<ReviewSession>` — `finishedAt IS NULL` 최신
- 신설: `findAllByUserIdAndDate(userId, date): List<ReviewSession>` — 통계용

### 완료 기준 (AC)
- Given 진행 중 세션 1개 / When `findLatestActiveByUserId(userId)` / Then Optional.of(session)
- Given 진행 중 세션 없음 / When 호출 / Then Optional.empty()
- Given 오늘 3개 세션 (2개 finished, 1개 active) / When `findAllByUserIdAndDate(userId, today)` / Then 3개 리스트

### Definition of Done
- [ ] 구현 (Repository port + JPA adapter)
- [ ] Slice 테스트 (`@DataJpaTest`) — 신 쿼리 검증
- [ ] 기존 deck 관련 쿼리 삭제

### 스토리 포인트
1d

### 의존성
- 선행: Story 2-1, 2-3
- 후행: Story 2-7

## [Story 2-7] 통합 테스트 — batch ↔ session 동기화

### User Story
- As a Review BC 개발자
- I want 통합 테스트가 batch ↔ session 시나리오 커버하기를
- so that 리팩토링 이후 안정성 검증

### 설명
`@SpringBootTest` 통합 테스트 신설:
- 세션 A에서 카드 X 완료 → 세션 B 시작 시 X 미포함
- Mode down → 다음 batch에서 exhausted card archive → 세션에 없음
- 자정 close 후 새 세션 시작 시 오늘 batch로 전환

### 완료 기준 (AC)
- 통합 테스트 3+ 시나리오 all pass

### Definition of Done
- [ ] 구현 (`DailyBatchReviewIntegrationTest`)
- [ ] 시나리오 3+ (batch dedup, mode down, day rollover)

### 스토리 포인트
1d

### 의존성
- 선행: Story 2-1 ~ 2-6

---

# [Epic 3] 캐시 측정 대시보드 + 규칙 기반 추천 + 조건부 주간 요약

## 목표

L3 대시보드 + streak + 규칙 기반 추천 안내(SUGGEST_DOWNGRADE/UPGRADE) + T3 조건부 자동 주간 요약 알림 세트를 완성해 사용자가 자기 학습 캐시 용량을 데이터로 파악·조정할 수 있게 한다.

## 배경

이슈 #26의 핵심. Epic 1·2 완료로 batch·session 데이터가 확보되면 이 위에 대시보드·추천이 자란다. 초기 3명 사용자 관찰 데이터로 v2 임계값·자동 조정 근거 확보.

## 포함 Story

- Story 3-1: `LearningDashboardQueryService` — 오늘/7일/30일/streak 집계
- Story 3-2: `RecommendationEngine` 도메인 서비스 — 규칙 판정
- Story 3-3: 주간 요약 cron + 조건 판정 + notification 발송
- Story 3-4: `LearningDashboardController` + DTO
- Story 3-5: 추천 accept orchestration (mode 변경 트리거)
- Story 3-6: Application config (임계값 노출)
- Story 3-7: 통합 테스트 — 3주 batch 데이터 시나리오
- Story 3-8: In-app notification 스키마 (v1 minimal: DB row + polling)

## Epic 인수 시나리오

- Given 오늘 batch (5장 중 2장 view) / When `GET /learning-dashboard` / Then 응답에 today.viewedCards=2, completionRatio=0.4
- Given 최근 3주 batch 평균 완료율 42% / When 주간 cron 실행 / Then user_notification 삽입 (SUGGEST_DOWNGRADE)
- Given `POST /learning-dashboard/recommendations/{id}/accept` / When 호출 / Then user_schedule_config.mapped_mode 다운 + 다음 batch에서 M3 down cap 발동

## Epic 완료 기준 (DoD)

- [ ] 포함 Story 모두 완료
- [ ] 대시보드 API 응답 스키마 정합
- [ ] 3주 시나리오 통합 테스트 (저완료율 감지 → 알림 발동)
- [ ] 추천 accept → mode 변경 orchestration 통합 테스트
- [ ] 관찰 지표 4개 (`recommendation.suggested/accepted/dismissed_total`) 노출

## Epic 기술 결정 / 대안

- **추천 임계값 config vs enum 상수**: config (환경변수) 채택. v1 default 50%/3주. 관찰 후 튜닝 여지.
- **In-app notification v1 minimal**: DB row + 프론트 polling. WebSocket / FCM는 v2 별도 이슈. 초기 3명 규모에 오버스펙 방지.
- **Recommendation 이력 저장**: `user_recommendation(userId, type, fromMode, toMode, triggeredAt, resolvedAt?, action?)` — 이력 축적으로 v2 자동 조정 근거.

## [Story 3-1] `LearningDashboardQueryService` — 오늘/7일/30일/streak 집계

### User Story
- As a 프론트엔드
- I want 대시보드 데이터 하나의 API로 받기를
- so that 여러 번 조회 없이 UI 렌더링

### 설명
`LearningDashboardQueryService.buildDashboard(userId, asOf): LearningDashboardResponse` — 오늘 batch (getOrCreate) + 최근 7일 평균 + 최근 30일 평균 + streak (current/longest) + recommendation (nullable) 조합.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Review.application.service.LearningDashboardQueryService`

**주요 메서드**:
- `buildDashboard(userId, asOf: LocalDate): LearningDashboardResponse`

### 완료 기준 (AC)
- Given 오늘 batch + 최근 7일 5개 batch (평균 60%) / When `buildDashboard` / Then response.recent7Days.avgCompletionRatio=0.6
- Given streak 3일 / When 조회 / Then response.streak.current=3
- Given recommendation 없음 (정상 완료율) / When 조회 / Then response.recommendation=null

### Definition of Done
- [ ] 구현 (Service, DTO)
- [ ] 단위 테스트 (Repository·Calculator Mock)
- [ ] 통합 테스트 (실제 batch 데이터)

### 스토리 포인트
2d

### 의존성
- 선행: Epic 1 완료
- 후행: Story 3-4

## [Story 3-2] `RecommendationEngine` 도메인 서비스 — 규칙 판정

### User Story
- As a Review BC 개발자
- I want RecommendationEngine이 규칙 기반 다운/업 판정을 노출하기를
- so that 대시보드·주간 cron이 이 서비스 소비

### 설명
`RecommendationEngine.evaluate(userId, asOf): Optional<Recommendation>`:
- 최근 3주 batch 평균 완료율 < 50% → `SUGGEST_DOWNGRADE` (한 단계 아래 mode)
- 최근 4주 모든 batch perfect + 사용자 mode < MODE_60D → `SUGGEST_UPGRADE`
- 그 외 → Optional.empty()

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Review.domain.service.RecommendationEngine`
- `Recommendation(type: RecommendationType, fromMode, toMode, reason: String)`
- `enum RecommendationType { SUGGEST_DOWNGRADE, SUGGEST_UPGRADE }`

**주요 메서드**:
- `evaluate(userId: Long, asOf: LocalDate): Optional<Recommendation>`

### 완료 기준 (AC)
- Given 최근 3주 평균 42% + user MODE_14D / When `evaluate` / Then `SUGGEST_DOWNGRADE(MODE_14D → MODE_7D)`
- Given 최근 4주 모두 perfect + user MODE_14D / When `evaluate` / Then `SUGGEST_UPGRADE(MODE_14D → MODE_28D)`
- Given 최근 3주 평균 70% / When `evaluate` / Then Optional.empty()
- *(엣지)* Given user MODE_7D + 저완료율 / Then `SUGGEST_DOWNGRADE`는 없음 (더 이상 낮은 mode 없음, 대신 다른 안내 or empty)
- *(엣지)* Given user MODE_60D + 완벽 / Then `SUGGEST_UPGRADE` 없음

### Definition of Done
- [ ] 구현 (`RecommendationEngine`)
- [ ] 단위 테스트 (Repository Mock, 시나리오 5+ 케이스)
- [ ] 임계값 config 주입 검증

### 스토리 포인트
2d

### 의존성
- 선행: Epic 1 완료
- 후행: Story 3-3, 3-4

## [Story 3-3] 주간 요약 cron + 조건 판정 + notification 발송

### User Story
- As a 시스템
- I want 매주 월 09:00 KST 사용자별 추천 판정 → 알림 발송하기를
- so that 저완료율 사용자에게 조용한 개선 안내

### 설명
`@Scheduled(cron = "0 0 9 ? * MON", zone = "Asia/Seoul")` — 활성 사용자 순회 → `RecommendationEngine.evaluate` → 결과 있으면 `user_notification` 삽입 + `user_recommendation` 삽입.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Review.application.scheduler.WeeklyRecommendationScheduler`
- `com.example.thirdtool.Review.application.service.NotificationService`

**주요 메서드**:
- `checkWeeklyRecommendations(): void`
- `NotificationService.send(userId, type, payload): UserNotification`

### 완료 기준 (AC)
- Given 저완료율 사용자 3명 / When cron 실행 / Then 3개 user_notification 삽입
- Given 정상 완료율 사용자 / When cron 실행 / Then 알림 없음 (조용)
- *(엣지)* 이미 이번 주에 SUGGEST_DOWNGRADE 발송된 사용자 / Then 중복 발송 안 함 (같은 주에 한 번만)

### Definition of Done
- [ ] 구현 (`WeeklyRecommendationScheduler`, `NotificationService`)
- [ ] 단위 테스트 (Repository·Engine Mock)
- [ ] 통합 테스트 (3주 시나리오 데이터 → cron 트리거 → 알림 삽입 검증)

### 스토리 포인트
2d

### 의존성
- 선행: Story 3-2, 3-8 (notification 스키마)
- 후행: 없음

## [Story 3-4] `LearningDashboardController` + DTO

### User Story
- As a 프론트엔드
- I want 대시보드 API로 오늘·최근·streak·recommendation 통합 응답 받기를
- so that 대시보드 화면 한 번의 요청으로 렌더

### 설명
`LearningDashboardController` 신설:
- `GET /api/v1/learning-dashboard`
- `POST /api/v1/learning-dashboard/recommendations/{id}/accept`
- `POST /api/v1/learning-dashboard/recommendations/{id}/dismiss`

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Review.presentation.LearningDashboardController`
- `LearningDashboardResponse` (오늘·7일·30일·streak·recommendation)

### 완료 기준 (AC)
- Given 인증 사용자 / When `GET /learning-dashboard` / Then 200 + 대시보드 JSON
- Given 존재하는 recommendation / When `POST .../accept` / Then 200 + mode 변경 완료
- *(예외)* 이미 dismiss된 recommendation accept / Then `RECOMMENDATION_ALREADY_RESOLVED` 409

### Definition of Done
- [ ] 구현 (Controller + DTO)
- [ ] 슬라이스 테스트 (`@WebMvcTest`)
- [ ] OpenAPI 반영

### 스토리 포인트
1d

### 의존성
- 선행: Story 3-1, 3-5
- 후행: FE 연결

## [Story 3-5] 추천 accept orchestration (mode 변경 트리거)

### User Story
- As a 사용자
- I want 추천 accept 시 자동으로 mode 변경이 적용되기를
- so that 대시보드 배지에서 한 번의 액션으로 조정

### 설명
`LearningDashboardCommandService.acceptRecommendation(userId, recommendationId)` — `RecommendationRepository`에서 recommendation 조회 → `UserScheduleCommandService.updateMode(userId, recommendation.toMode)` 호출 → recommendation 상태 `resolvedAt=now, action=ACCEPTED` 업데이트.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Review.application.service.LearningDashboardCommandService`
- `Recommendation` (Entity — 이력 저장)

**주요 메서드**:
- `acceptRecommendation(userId, recommendationId): void`
- `dismissRecommendation(userId, recommendationId): void`

### 완료 기준 (AC)
- Given SUGGEST_DOWNGRADE 추천 (MODE_14D → MODE_7D) / When accept / Then user_schedule_config.mapped_mode='MODE_7D' + recommendation.action=ACCEPTED
- Given 이미 resolved recommendation / When accept 시도 / Then `RECOMMENDATION_ALREADY_RESOLVED` 409
- Given accept 완료 후 다음 batch 생성 / Then M3 down cap 발동 (기존 카드 exhausted 조기 archive)

### Definition of Done
- [ ] 구현 (`LearningDashboardCommandService` + `Recommendation` Entity)
- [ ] 통합 테스트 (accept → mode 변경 → batch 검증)

### 스토리 포인트
2d

### 의존성
- 선행: Story 3-2, 3-3
- 후행: Story 3-4

## [Story 3-6] Application config (임계값 노출)

### User Story
- As a 운영자
- I want 추천 임계값을 config로 조정 가능하기를
- so that 관찰 데이터 축적 후 튜닝 용이

### 설명
`application.yml`:
```yaml
app:
  learning:
    recommendation:
      downgrade-threshold-ratio: 0.5
      downgrade-window-weeks: 3
      upgrade-window-weeks: 4
```
`@ConfigurationProperties`로 주입.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Review.application.config.RecommendationProperties`

### 완료 기준 (AC)
- Given `application.yml` 값 설정 / When 서버 부팅 / Then `RecommendationProperties` bean 주입
- Given `RecommendationEngine`이 config 참조 / When evaluate / Then config 값 사용

### Definition of Done
- [ ] 구현 (`RecommendationProperties` + application.yml)
- [ ] 단위 테스트 (config 주입 검증)

### 스토리 포인트
0.5d

### 의존성
- 선행: Story 3-2
- 후행: 없음

## [Story 3-7] 통합 테스트 — 3주 batch 데이터 시나리오

### User Story
- As a Review BC 개발자
- I want 3주 시나리오 통합 테스트가 저완료율 감지·알림 발동 커버하기를
- so that 실제 사용자 흐름 안정성 검증

### 설명
`@SpringBootTest` 통합 테스트:
- Test data: 사용자 A + 3주간 매일 batch (평균 40%) → cron 트리거 → 알림 삽입 확인
- Test data: 사용자 B + 3주간 완료율 80% → cron 트리거 → 알림 없음 확인
- Test data: 사용자 C + 4주 perfect → cron 트리거 → SUGGEST_UPGRADE 알림

### 완료 기준 (AC)
- 통합 테스트 3+ 시나리오 all pass

### Definition of Done
- [ ] 구현 (`WeeklyRecommendationIntegrationTest`)
- [ ] 시나리오 3+ 케이스

### 스토리 포인트
1d

### 의존성
- 선행: Story 3-3
- 후행: 없음

## [Story 3-8] In-app notification 스키마 (v1 minimal)

### User Story
- As a 프론트엔드
- I want 미확인 알림을 polling으로 조회하기를
- so that 실시간 push 없이도 알림 표시

### 설명
`user_notification(id, user_id, type, payload_json, created_at, read_at?)` 신설. `GET /api/v1/notifications?unread=true` 폴링 API.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Review.domain.model.UserNotification`
- `com.example.thirdtool.Review.presentation.NotificationController`
- `NotificationController`

**주요 메서드**:
- `NotificationController.list(userId, unread: boolean): List<UserNotificationResponse>`
- `NotificationController.markRead(userId, notificationId): void`

### 완료 기준 (AC)
- Given 미확인 알림 3개 / When `GET /notifications?unread=true` / Then 3개 반환
- Given `POST /notifications/{id}/read` / Then read_at 세팅

### Definition of Done
- [ ] Flyway `V{N}__user_notification.sql` + 롤백
- [ ] 구현 (Entity, Repository, Controller)
- [ ] 슬라이스 테스트 (`@WebMvcTest`)

### 스토리 포인트
1d

### 의존성
- 선행: Story 3-3에서 소비
- 후행: 없음
