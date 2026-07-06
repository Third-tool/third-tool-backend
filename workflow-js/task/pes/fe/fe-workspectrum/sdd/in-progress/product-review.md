# [Product] Review FE — Daily Batch + Cross-Layer Queue + 대시보드 최소판

## Product Vision

> **Review FE는 사용자가 매일 접근해 그날 due 카드(cross-layer 짬뽕)를 순차로 소진하는 UI다. Layer 경계 없이 하나의 큐에서 카드를 뽑고, 못 본 카드는 그냥 지나가는 원칙을 화면에서 자연스럽게 표현한다.**
> 대시보드는 v1엔 오늘/최근 7일/streak 3개 지표만 노출 (L1~L2 수준). L3 규칙 기반 추천 배지·T3 주간 알림은 v2.

## 배경 및 문제

- **현재 상황 (As-Is)**
  - `features/review/ReviewSessionPage` — deck 스코프 세션 시작. `<ReviewScopeToggle mode="AXIS|LAYER">` 도입 예정이었으나 미착지.
  - `features/decks/*` — Deck 라우트 존재 (폐기 예정).
  - Daily 개념 자체가 FE에 없음. 세션마다 매번 카드 pool 재조회.
  - Streak · completion rate 지표 없음.
  - 사용자에게 "오늘 얼마 남았지" 표시할 UI 없음.
- **발생하는 문제**
  - BE product-review 07-02 신설 (이슈 #24~#26)으로 daily batch 개념 등장 → FE가 반영 안 하면 세션 dedup 안 됨 · 진행률 표시 불가.
  - Cross-layer 짬뽕 큐 정책 (이슈 #25)으로 layer 경계 UI 소멸 → 기존 `<ReviewScopeToggle>` 폐기.
  - 사용자가 자기 학습 캐시 용량 궁금해할 UX 지점 없음 (v1엔 raw stats만이라도 있어야 v2 추천 UX 준비).
- **왜 지금 해결해야 하는가**
  - BE product-review Epic 1(DailyBatch)+2(Cross-layer)가 M5(2026-07-22~07-28)에 진입. FE는 M5~M6에 동기 착지 필요.
  - 첫 릴리스(0.1.0v, ~2026-08-19) 핵심 사이클 8~10단계(오늘 접근 → 배치 → clear → 대시보드)의 UX 원천.
  - Daily batch 소비하는 다른 FE feature (예: 카드 상세의 "다음 노출 D일") 준비 원천.

## 목표 (To-Be)

- **`features/review/DailyBatchLandingPage`** (신설) — 앱 진입 후 첫 화면. 오늘 batch (lazy 생성) → 카드 개수·진행률·[학습 시작] CTA.
- **`features/review/ReviewSessionPage` 재편** — deck 스코프 폐기. Batch 참조로 카드 리스트 pull. `<ReviewScopeToggle>` 삭제.
- **`<DailyBatchProgress>`** — 오늘 batch의 completed/total + progress bar.
- **`<CardQueueList>`** — 그날 카드 리스트. 카드별 `card_interval_day` 표시 (1일차/3일차/7일차 등).
- **`<CardReviewSequence>`** — 순차 카드 노출 화면 (RECALLING → COMPARING 흐름 유지).
- **`features/learning-dashboard/`** (신설) — 대시보드 v1 minimal.
  - `<TodayCompletionCard>` — 오늘 completed/total, completion ratio.
  - `<Recent7DaysCard>` — 최근 7일 평균 completion + perfect clear 일수.
  - `<CurrentStreakCard>` — current streak / longest streak.
- **`<BatchClosedBanner>`** — 자정 close 이후 진입 시 "오늘 학습 종료. 어제 못 본 카드는 지나갔습니다." 안내.
- **`<AutoFinishNoticeToast>`** — 이전 세션 자동 finish 시 안내 ("이전 세션이 정리되어 새 세션을 시작합니다").
- **Zod 스키마 신설** — `DailyLearningBatch`, `DailyCardEntry`, `LearningDashboardResponse`.
- **Cornell 학습 UX 유지** — RECALLING/COMPARING 카드 뒤집기 인터랙션은 기존 그대로.

## 설계 결정 (Design Decisions)

- **첫 화면은 DailyBatchLandingPage (vs 카드 리스트 direct)**
  - 사용자 매일 진입 시 "오늘 얼마 남았지"를 먼저 보게 함으로써 오답노트 사이클 리듬 형성.
  - [학습 시작] CTA 하나 → ReviewSessionPage로 이동.
  - Home = 학습 시작 지점이라는 UX 원칙.
- **Layer 경계 UI 완전 폐기 (이슈 #25)**
  - `<ReviewScopeToggle>` 제거. layer picker 제거.
  - 사용자는 mode·concepts만 관리하고, 학습 큐는 시스템이 자동 조합.
  - Layer 시각화는 v2로 이관 (릴리스 문서 v1 out).
- **자정 close 명시적 안내**
  - `<BatchClosedBanner>` — 자정 이후 진입 시 앱 상단에 표시 (예: 새벽 2시 접근).
  - "지나간 카드는 누적되지 않습니다" 원칙 강조.
- **대시보드는 별도 라우트 (vs 홈 임베드)**
  - `/dashboard` 별도 경로. 홈은 학습 CTA 중심 (진행률만 압축 표시).
  - 대시보드는 사용자가 관심 있을 때 확인.
- **Streak 실시간 계산 결과 캐시 X**
  - BE가 realtime 계산 (이슈 #24) → FE도 매번 재조회.
  - 대시보드 로드 시 latency 200ms 이하 유지 (초기 3명 규모라 여유).
- **v1은 raw stats만 (L1~L2), 추천 배지 X (L3는 v2)**
  - 사용자 자율성 우선. 관찰 지표만 로깅하고 v2에 L3 추천 도입.
- **자정 이후 진입 → 오늘 batch lazy 생성**
  - 사용자 첫 접근 시 서버가 lazy 생성 (이슈 #24).
  - FE는 `POST /daily-batch/today` 호출로 트리거.
- **Progress bar는 batch 크기 상관없이 항상 표시**
  - 20장이든 50장이든 100장이든 progress bar 하나. 크기 상관없이 오답노트 리듬 유지.

## 대안 검토 (Alternatives Considered)

### 갈림길 A. 첫 화면

**Option A — 카드 리스트 direct**
- 거부 이유: 사용자가 "오늘 얼마 남았지"를 즉시 못 인지. 리듬 형성 실패.

**Option B (선택) — DailyBatchLandingPage (batch 개요 + CTA)**
- 비용: 화면 하나 추가.
- 보상: 오답노트 사이클 리듬 명확. 매일 진입 = 학습 시작 지점.

**Option C — 대시보드 첫 화면**
- 거부 이유: 대시보드는 관심 있을 때 확인. 첫 화면이 통계면 학습 진입 마찰 큼.

### 갈림길 B. Layer 시각화

**Option A — Layer별 진행률 표시 (기존 계획)**
- 거부 이유: 이슈 #25 layer 경계 폐기 결정. 사용자 mental model이 layer로 갈라지지 않음.

**Option B (선택) — Layer 시각화 v1 out, cross-layer 통합만**
- 비용: layer 관점 데이터 손실.
- 보상: UX 단순. 사용자 리듬 방해 X. v2에 필요 시 도입.

### 갈림길 C. 대시보드 v1 스코프

**Option A — L1 raw stats만 (오늘 completed/total)**
- 거부 이유: 사용자에게 학습 캐시 크기 인사이트 부족.

**Option B (선택) — L2 대시보드 (오늘 + 최근 7일 + streak)**
- 비용: 3개 카드 컴포넌트.
- 보상: 사용자가 자기 학습 캐시 흐름 인지. L3 추천의 사전 준비.

**Option C — L3 추천 배지 v1 포함**
- 거부 이유: v1 관찰 데이터 없이 규칙 임계값 결정 어려움. 릴리스 문서 v1 out.

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 / 라우트 배치

```
┌────────────────────────────────────────────────────────────────┐
│ Router                                                         │
│  /                     → DailyBatchLandingPage (홈)            │
│  /review               → ReviewSessionPage (세션 진행)         │
│  /dashboard            → LearningDashboardPage                 │
├────────────────────────────────────────────────────────────────┤
│ features/review/                                               │
│   DailyBatchLandingPage                                        │
│     ├ DailyBatchProgress (completed/total + bar)               │
│     ├ CardQueueList (카드 리스트 + interval_day 표시)          │
│     ├ StartLearningButton                                      │
│     └ BatchClosedBanner (자정 이후 진입 시)                    │
│   ReviewSessionPage                                            │
│     ├ CardReviewSequence (RECALLING → COMPARING)               │
│     ├ SessionProgressBar (세션 내 진행률)                      │
│     └ AutoFinishNoticeToast                                    │
├────────────────────────────────────────────────────────────────┤
│ features/learning-dashboard/                                   │
│   LearningDashboardPage                                        │
│     ├ TodayCompletionCard                                      │
│     ├ Recent7DaysCard (평균 completion + perfect clear 일수)   │
│     └ CurrentStreakCard (current + longest)                    │
├────────────────────────────────────────────────────────────────┤
│ lib/api/schemas/                                               │
│   dailyLearningBatch.ts                                        │
│   learningDashboard.ts                                         │
│   reviewSession.ts (재편 — batch 참조)                         │
├────────────────────────────────────────────────────────────────┤
│ hooks/                                                         │
│   useDailyBatch()          — POST /daily-batch/today           │
│   useMarkViewed(cardId)    — session recordView 트리거         │
│   useReviewSession()       — 세션 시작/재개                    │
│   useLearningDashboard()   — 대시보드 데이터                   │
└────────────────────────────────────────────────────────────────┘
```

### 핵심 플로우

**1. 첫 접근 → 오늘 batch lazy 생성 → CTA**
```
User → / (로그인 이후)
DailyBatchLandingPage 로드:
  → useDailyBatch() → POST /daily-batch/today
     ├ 오늘 batch 없음 → 서버가 lazy 생성 (cross-layer 카드 필터 + exhausted 카드 archive orchestration)
     └ 응답 { batchDate, totalCards, viewedCards, entries: [...] }
  → DailyBatchProgress "12/47 clear (26%)"
  → CardQueueList (interval_day 표시)
  → [학습 시작] 버튼
Click [학습 시작]
  → /review (ReviewSessionPage)
```

**2. Review 세션 진행 + 카드 clear**
```
User → /review
useReviewSession() → POST /review-sessions
  ├ 진행 중 세션 있으면 자동 finish + AutoFinishNoticeToast
  └ 신규 세션 생성 (batch 미완료 카드로)
응답 { sessionId, cardReviews: [{cardId, step: RECALLING}, ...] }

Card #1 RECALLING (질문·keyword만)
  User [확인] → startComparing → POST /review-sessions/:id/start-comparing
  Card #1 COMPARING (Cornell 전체 노출)
  User [clear] → useMarkViewed(cardId)
    → POST /review-sessions/:id/record-view
      (BE: card.recordView + batch.markViewed)
    → 응답 200
    → toast "clear!" + SessionProgressBar 갱신
  Card #2로 이동 → POST /review-sessions/:id/next

... 반복 ...

Card #N 완료 → [세션 종료] → POST /review-sessions/:id/finish
→ 리디렉트 / (DailyBatchLandingPage)
```

**3. 자정 이후 진입 → BatchClosedBanner**
```
User → / (KST 02:00)
useDailyBatch() → POST /daily-batch/today
  → 오늘(현재 KST 날짜)은 batch 없음 → 신규 생성
  → 어제 batch는 자정 close (BE cron 00:05)
Landing 화면 상단 BatchClosedBanner:
  "어제 학습이 종료됐어요. 오늘 새로운 카드가 준비됐습니다."
DailyBatchProgress "0/{today total}"
```

**4. 대시보드 조회**
```
User → /dashboard
useLearningDashboard() → GET /learning-dashboard
응답 {
  today: {completed, total, ratio},
  recent7Days: {avgCompletionRatio, perfectClearDays, totalDays},
  streak: {current, longest},
  recommendation: null   ← v1은 항상 null (L3 v2)
}
TodayCompletionCard "오늘 12/47 (26%)"
Recent7DaysCard "지난 7일 평균 60% · 완벽 clear 2일"
CurrentStreakCard "현재 3일 연속 · 최장 5일"
```

### 외부 의존

- **BE `/api/v1/daily-batch/*`** — Batch 조회·이력.
- **BE `/api/v1/review-sessions/*`** — 세션 생성·view 기록·이동.
- **BE `/api/v1/learning-dashboard`** — 대시보드 통합 응답.
- **BE `/api/v1/cards/{id}`** — CardReviewSequence 안 카드 상세.
- **`product-card.md` (FE)** — CornellDisplay 재사용.
- **`product-learning-tower.md` (FE)** — AxisSelect 등 공통 컴포넌트 재사용.

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | ApiError code | HTTP | 클라이언트 권장 동작 (UX) |
| --- | --- | --- | --- |
| 자정 close 이후 markViewed 시도 | `DAILY_BATCH_CLOSED` | 409 | BatchClosedBanner 표시 + 학습 세션 종료 → Landing으로 이동 |
| Closed batch 상태에서 새 세션 시도 | `DAILY_BATCH_CLOSED_FOR_NEW_SESSION` | 409 | "오늘 새 batch를 만들까요?" 다이얼로그 → 자동 새 batch 생성 |
| 존재하지 않는 batch 조회 | `DAILY_BATCH_NOT_FOUND` | 404 | Landing으로 리디렉트 + toast |
| 존재하지 않는 세션 조회 | `REVIEW_SESSION_NOT_FOUND` | 404 | 세션 목록 재조회 or Landing |
| 이미 dismiss된 recommendation accept (v2 대비) | `RECOMMENDATION_ALREADY_RESOLVED` | 409 | 대시보드 재조회 (v1엔 미도입) |
| Batch 크기 0 (오늘 due 카드 없음) | 정상 200 | 200 | Landing "오늘 학습할 카드가 없습니다. 카드를 추가해 보세요." + [카드 추가] CTA |

### 로깅 정책 (FE)

- **항상 기록**:
  - Daily batch 생성 이벤트 (Sentry breadcrumb — batchDate, totalCards)
  - Review 세션 시작·종료 이벤트 (sessionId, startedAt, finishedAt, viewedCount)
  - Card clear 이벤트 (cardId, viewedAt, session context)
  - Landing 진입 이벤트 (batchDate, currentTime, isBatchClosed)
- **debug**:
  - `useDailyBatch()` 재조회 (실패 시)
  - AutoFinishNoticeToast 표시
  - BatchClosedBanner 표시
- **절대 금지**:
  - Cornell 노트 원문 통째 (cardId만)
  - 세션 히스토리 전체 dump (start/end만)

### 관측 지표 (Web Vitals)

- `LCP` — DailyBatchLandingPage ≤ 1.5s (P95) · 초기 3명 규모 baseline
- `INP` — Cornell 카드 flip (RECALLING → COMPARING) ≤ 150ms (P95)
- `CLS` — DailyBatchProgress 로드 시 ≤ 0.1
- `daily_batch.viewed_at_landing_total` — RUM 이벤트 (사용자 하루 진입 횟수)
- `review_session.started_total` — RUM 이벤트
- `dashboard.viewed_total` — 사용자가 대시보드 확인 비율

## 롤아웃 / 마이그레이션 (Rollout)

### 전제

- BE product-review Epic 1(DailyBatch)+2(Session 재편)+3(대시보드 최소판) 완료 후 FE 진입.
- 초기 3명 사용자 · trafic 극소량.
- FE는 M5~M6에 착지.

### Product 의존성

- **선행**:
  - BE product-review.md Epic 1~3
  - BE product-card.md Epic 1~3 (Card 도메인 메서드 · `isDueOn` 등)
- **동시**: `product-card.md` (FE) — CornellDisplay 재사용
- **후행**: `product-fe-cdn.md` (FE) — 대시보드 캐시 정책

### Epic·Story 의존성 그래프

```
Epic 1 (Daily Batch UI)  ──►  Epic 2 (Review Session 재편)  ──►  Epic 3 (Dashboard v1)
     │                              │                                    │
     ├─► DailyBatchLandingPage      ├─► ReviewSessionPage 재편            ├─► TodayCompletionCard
     ├─► DailyBatchProgress         ├─► CardReviewSequence                ├─► Recent7DaysCard
     ├─► CardQueueList              ├─► AutoFinishNoticeToast             └─► CurrentStreakCard
     └─► BatchClosedBanner          └─► /decks/* 리디렉트 폐기 지원
```

### 환경별 설정 분기

| 항목 | dev (`.env.development`) | prod (`.env.production`) |
| --- | --- | --- |
| `VITE_API_BASE_URL` | http://localhost:8080 | https://api.thirdtool.dev |
| MSW handlers | Daily batch + review-session + dashboard 활성 | disabled |
| Sentry | local | enabled |
| Timezone display | 시스템 KST | KST 강제 (`Intl.DateTimeFormat` locale=ko-KR) |
| 자정 close 시각 표시 (BatchClosedBanner) | 시뮬레이션 옵션 (개발자 툴로 시각 조작) | 실제 KST 00:00 |
| Dashboard polling 간격 | 5초 (개발 관찰용) | 60초 (부하 완화) |

## 성공 지표 (KPI)

- **일일 batch 진입률**: 활성 사용자 중 하루 한 번 이상 Landing 진입 비율 ≥ 80%
- **세션 완주율**: 세션 시작 → clear 카드 수 ≥ 50% 인 세션 비율 ≥ 60%
- **LCP · INP · CLS**: Web Vitals P95 목표 값 (§실패 모드 참조) 모두 통과
- **자정 close 안내 이해도**: 사용자 인터뷰 3명 중 3명 "지나간 카드가 왜 없는지" 이해
- **대시보드 확인율**: `dashboard.viewed_total / daily_batch.viewed_at_landing_total` ≥ 30% (활성 사용자의 30% 이상 주 1회 이상 확인)

## Scope

**In Scope**:
- DailyBatchLandingPage (홈)
- Review 세션 재편 (batch 참조)
- Cross-layer 짬뽕 큐 UI (layer 스코프 개념 삭제)
- 자정 close 안내 (BatchClosedBanner)
- 이전 세션 자동 finish 안내 (AutoFinishNoticeToast)
- 대시보드 v1 minimal (오늘 · 7일 · streak)
- Zod 스키마 3종 (batch · dashboard · session 재편)

**Out of Scope**:
- L3 규칙 기반 추천 배지 (SUGGEST_DOWNGRADE/UPGRADE 안내) — v2 (0.2.0v)
- T3 주간 요약 알림 UI + notification 폴링 — v2 (인프라도 v2)
- 자동 mode 조정 (L4) — v2 이후 결정
- 30일 통계 그래프 · 차트 — v2 (사용자 3명 규모에 오버스펙)
- Layer별 진행률 시각화 — v2 (이슈 #25 layer 경계 폐기 유지)
- 카드 재정렬 UI (interval 짧은 것부터/랜덤 등) — v2 backlog
- Streak 실시간 push 알림 — v2

## 대상 사용자

- **오답노트 학습자 (초기 3명)** — 매일 진입해 그날 due 카드를 확실히 소진하고 싶은 사람. Layer 파편화 없이 통합 큐 선호.
- **자기 학습 캐시 궁금해하는 사람** — 오늘 clear율 · streak를 대시보드에서 확인. v2 추천 이후에도 자율 판단 원함.
- **자기 능력 과대평가 성향 사용자** — v1엔 raw stats만 · v2에 규칙 기반 안내 · v3에 자동 조정 순차 도입.

## 연결된 Epic 목록

- [ ] Epic 1: Daily Batch UI (Landing · Progress · Queue · ClosedBanner)
- [ ] Epic 2: Review Session 재편 (batch 참조 · CardReviewSequence · AutoFinish)
- [ ] Epic 3: 대시보드 v1 minimal (Today · Recent7 · Streak)

## 관련 문서

- **의존 BE Product**: `../../workspectrum/sdd/in-progress/product-review.md` — Daily batch · Session · 대시보드
- **의존 BE Product**: `../../workspectrum/sdd/in-progress/product-card.md` — Card lifecycle · 도메인 메서드
- **동시 FE Product**: `product-card.md` (FE) — CornellDisplay 재사용
- **선행 FE Product**: `product-learning-tower.md` (FE) — layer/axis 구조 · Roadmap/Selection UI
- **관련 이슈**: `workflow/task/fix/brainstorming/version/0.0.2v/issue-24 ~ #26`
- **릴리스 문서**: `../../../milestones/release/version/0.0.1v/release.md` §Product 4
- **FE-ADR 후보**:
  - 첫 화면 결정 (Landing vs 카드 리스트)
  - Layer 경계 UI 완전 폐기 근거
  - 자정 close 안내 UX 방식

## 열린 질문 (Open Questions)

1. **Notification 인프라** — v1은 대시보드 방문 시에만 확인. WebSocket / FCM 등 실시간 push는 v2 인프라 이슈로 분리.
2. **자정 close banner 표시 지속 시간** — 사용자가 닫으면 다시 표시 안 함 (sessionStorage) vs 하루 한 번 표시. 사용자 인터뷰 후 결정.
3. **Batch 크기 0일 때 UX** — "카드가 없어요" 안내 + [카드 추가] CTA · v2에 학습 자극 유도 문구 검토.
4. **Streak 시각화** — 숫자만 vs 캘린더 히트맵. v1은 숫자, v2에 히트맵 도입 여지.
5. **Cross-layer 큐 정렬 전략 UX** — v1은 `card_interval_day ASC` 자동. 사용자에게 정렬 선택 옵션 v2 검토.
6. **모바일 대응** — 초기 3명 데스크톱 위주. 모바일 UX는 v2에서 별도 spec.

---

# [Epic 1] Daily Batch UI (Landing + Progress + Queue + BatchClosedBanner)

## Epic 목표
사용자가 앱 진입 시 오늘 batch를 lazy 트리거하고 진행률·카드 리스트·자정 close 상태를 명확히 확인할 수 있게 한다.

## 배경
BE 이슈 #24로 DailyLearningBatch가 신설. FE는 매일 진입 시점의 UI 원천이며 다른 모든 review UX의 시작점.

## 완료 기준
- [ ] `<DailyBatchLandingPage>` 신설 + 라우팅 `/`
- [ ] `<DailyBatchProgress>` (completed/total + bar)
- [ ] `<CardQueueList>` (interval_day 표시)
- [ ] `<BatchClosedBanner>` (자정 이후 진입 시)
- [ ] `useDailyBatch()` hook (POST /daily-batch/today)
- [ ] `dailyLearningBatch.ts` Zod 스키마
- [ ] Vitest + Testing Library 시나리오 (첫 진입 · 재진입 · 자정 이후)
- [ ] MSW handler for `/api/v1/daily-batch/today`

## Epic 기술 결정 / 대안
- **첫 화면 = Landing**: §대안 검토 갈림길 A 참조. 카드 리스트 direct 거부, 대시보드 첫 화면 거부.
- **Timezone 표시**: KST 강제. 서버·클라이언트 동일 기준.

---

# [Epic 2] Review Session 재편 (batch 참조 + Cross-Layer)

## Epic 목표
Review 세션이 daily batch를 원천으로 카드를 pull하고, layer 경계 없이 순차 노출하며, 이전 세션 자동 finish를 자연스럽게 처리한다.

## 배경
BE 이슈 #25로 세션이 batch 참조 방식으로 재편. 기존 deck 스코프 UI 폐기 + `<ReviewScopeToggle>` 폐기 + 자동 finish 정책 반영 필요.

## 완료 기준
- [ ] `<ReviewSessionPage>` 재편 (batch 참조로 카드 pull)
- [ ] `<CardReviewSequence>` RECALLING → COMPARING (기존 UX 유지)
- [ ] `<SessionProgressBar>` 세션 내 진행률
- [ ] `<AutoFinishNoticeToast>` 이전 세션 자동 finish 알림
- [ ] `useReviewSession()`, `useMarkViewed()` hooks
- [ ] `<ReviewScopeToggle>` 완전 삭제 (learning-tower FE에서 이관)
- [ ] `/decks/*` → `/` 리디렉트 지원 (기존 저장 URL 방어 6개월 유지)
- [ ] `reviewSession.ts` Zod 스키마 재편 (dailyBatchId 참조 필드 추가)
- [ ] Vitest + Testing Library + E2E 시나리오

## Epic 기술 결정 / 대안
- **Layer 경계 UI 폐기**: §대안 검토 갈림길 B 참조.
- **자동 finish 정책 vs 예외**: 자동 finish 채택 (UX 마찰 최소). AutoFinishNoticeToast로 안내.

---

# [Epic 3] 대시보드 v1 minimal (Today · Recent7 · Streak)

## Epic 목표
사용자가 자기 학습 캐시 크기를 raw stats로 파악할 수 있는 최소 대시보드를 제공한다. L3 규칙 기반 추천 배지·T3 알림은 v2로 이관.

## 배경
BE 이슈 #26 대시보드 API 응답을 소비. v1은 raw stats만, v2에 추천 배지 도입. 사용자 관찰 데이터 축적이 v2 임계값 튜닝의 근거.

## 완료 기준
- [ ] `<LearningDashboardPage>` 신설 + 라우팅 `/dashboard`
- [ ] `<TodayCompletionCard>` (오늘 completed/total + ratio)
- [ ] `<Recent7DaysCard>` (평균 completion + perfect clear 일수)
- [ ] `<CurrentStreakCard>` (current + longest)
- [ ] `useLearningDashboard()` hook
- [ ] `learningDashboard.ts` Zod 스키마 (`recommendation` 필드는 nullable, v2 대비)
- [ ] MSW handler (v1 응답에서 `recommendation: null`)
- [ ] Vitest 시나리오 (정상 로드 · 배치 0장 · streak 0일)
- [ ] Web Vitals LCP ≤ 1.5s 검증 (Lighthouse CI or 수동)

## Epic 기술 결정 / 대안
- **v1 스코프**: §대안 검토 갈림길 C 참조. L2 채택, L3 v2 이관.
- **Polling 간격**: dev 5초 (관찰용) · prod 60초 (부하 완화). §환경별 설정 분기 참조.
