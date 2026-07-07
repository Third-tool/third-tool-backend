# [Product] 알림(Notification) — SRS 복습 타이밍 이탈 방지 루프

## Product Vision

> ThirdTool의 SRS 학습 루프(`FRESH → 1D → 3D → 7D → 14D → 21D`)가 사용자의 행동을 능동적으로 끌어당기는 상태를 만든다.
> 복습 예약 시점·일일 목표 미달성·장기 미접속을 FCM 웹 푸시로 즉시 전달해, 사용자가 앱을 직접 열지 않아도 "지금이 복습 타이밍"임을 인지할 수 있다.
> SRS 본질("까먹기 직전 복습")이 알림 인프라 없이는 작동하지 않는다 — 알림은 부가 기능이 아니라 학습 루프의 마지막 연결 고리다.

## 배경 및 문제

- 현재 상황 (As-Is)
    - `UserSchedule`이 `dailyTarget`·`mode(10D/20D/30D)`·interval 계산까지 보유하지만, 그 결과를 사용자에게 능동 전달하는 채널이 없다
    - `Card.softSchedule`이 `FRESH / 1D / 3D / 7D / 14D / 21D` 단계를 내부적으로 계산하지만, 다음 복습 시점은 사용자가 직접 앱을 열어 확인해야 한다
    - 알림 전용 BC·Port·Adapter가 없어 어디에 어떻게 발송 책임을 두는지 결정되지 않음
    - 디바이스 토큰을 관리할 저장소·VO도 없음
- 발생하는 문제
    - SRS 핵심 가치("까먹기 직전 복습")가 사용자 자발성에 100% 의존 → 7일 이내 미접속이 누적되면 학습 루프 자체가 끊어진다
    - dailyTarget을 채우지 못한 사용자가 그 사실을 그날 안에 인지할 방법이 없어, 다음 날 복습이 밀리고 SoftSchedule 단계가 강제 후퇴
    - 채용 포트폴리오 관점에서 "SRS 서비스인데 리마인더 없음"은 즉시 감점 신호 — UX 패턴 누락
    - Product 알림 채널 미정 상태에서 향후 마케팅·운영 알림이 들어오면 채널 일관성을 잃게 됨
- 왜 지금 해결해야 하는가
    - UserSchedule v2의 interval 계산이 안정화된 직후가 알림 트리거 명세에 가장 좋은 시점 — 계산 로직이 굳기 전에 트리거 인터페이스를 함께 결정해야 후속 변경 비용이 작다
    - FCM 토큰 관리·동의(opt-in) 모델은 한 번 결정되면 이후 사용자 데이터 마이그레이션 비용이 크게 든다. 사용자 수가 작을 때 결정하는 게 가장 싸다
    - 면접 단골 질문: "SRS인데 사용자가 안 돌아오면 어떻게 합니까" — Notification BC 설계 근거가 답이 된다

## 목표 (To-Be)

- 모든 복습 예약 도래 시점에 FCM 웹 푸시가 발송되고, 사용자가 알림 클릭 시 해당 카드 복습 화면으로 진입한다
- 매일 23:00 기준 dailyTarget 미달성 사용자에게 "오늘 N장 남음" 알림이 자동 발송된다
- 7일 이상 미접속 사용자에게 "복귀 알림"이 1회 발송되고, 14일·21일·28일 시점에 추가 발송된다 (중복 발송 차단)
- 사용자가 알림 채널별로 opt-in/opt-out을 명시적으로 제어할 수 있다 — 옵션 미설정 시 기본값은 "복습 알림 ON, 마케팅 OFF"
- 디바이스 토큰의 등록·만료·갱신을 안정적으로 관리하고, 만료된 토큰으로의 발송 실패가 사용자 경험을 저해하지 않는다
- 알림 발송 실패율이 운영 메트릭(Product 0-b)에 노출되어 SLO 위반 시 감지된다

## 설계 결정 (Design Decision)

> **FCM(Firebase Cloud Messaging) 웹 푸시를 1차 채널로 채택한다. AWS SES(이메일)는 v2.**
> SRS의 "타이밍" 가치는 즉시성에 비례한다.
>
> - 이메일은 스팸 필터·수신 지연으로 SRS 복습 알림 타이밍에 부적합 (분 단위 지연 빈번)
> - FCM 웹 푸시는 Service Worker만 등록되면 OS 알림 센터로 즉시 노출 → SRS 타이밍 정확도 보장
> - 모바일 앱은 v1 범위 외 — 웹 푸시(VAPID 키 기반)로 시작하고, 모바일 추가 시 동일 FCM 토큰 모델 재사용
> - 이메일은 onboarding 환영·주간 리포트 등 지연 허용 용도로 v2 추가
> - 이 결정은 ADR로 별도 기록한다 (`ADR-NOTIFICATION-001: Channel Selection — FCM First`)

> **Notification은 신규 BC로 독립시키고, 트리거는 도메인 이벤트로 받는다.**
> UserSchedule·Card BC에 알림 책임을 섞지 않는다.
>
> - UserSchedule이 알림 발송까지 책임지면 BC 경계가 모호해지고 후속 채널 추가 시 변경 폭이 커짐
> - 도메인 이벤트(`CardReviewDueEvent`, `DailyTargetMissedEvent`, `UserInactiveEvent`)를 발행하면 Notification BC가 구독해서 채널 결정을 위임받음
> - 단일 인스턴스 단계에서는 Spring `ApplicationEventPublisher` (동기 → 비동기 전환 가능). 다중 인스턴스 전환 시 SQS 또는 EventBridge로 변경 — 트리거 인터페이스는 동일
> - 이 결정은 ADR로 별도 기록한다 (`ADR-NOTIFICATION-002: Trigger Coupling — Domain Event`)

> **NotificationPreference를 신규 VO로 분리한다. UserSchedule에 박지 않는다.**
> 채널·카테고리·opt-in 시점·디바이스별 동의는 UserSchedule의 책임이 아니다.
>
> - UserSchedule = 학습 일정 계산. NotificationPreference = 채널·카테고리 동의. 두 책임을 한 Aggregate에 두면 후속 채널 추가마다 UserSchedule이 흔들림
> - `NotificationPreference`는 User Aggregate에 매달리는 1:1 VO (또는 별도 Entity) — 카테고리(`REVIEW`, `DAILY_TARGET`, `INACTIVE`, `MARKETING`) × 채널(`WEB_PUSH`, `EMAIL`)의 boolean 매트릭스
> - 기본값: `REVIEW.WEB_PUSH = true`, `DAILY_TARGET.WEB_PUSH = true`, `MARKETING.* = false`
> - 이 결정은 ADR로 별도 기록한다 (`ADR-NOTIFICATION-003: Preference Modeling`)

> **디바이스 토큰은 `DeviceToken` Entity로 사용자당 N개 보유, 발송 실패 시 토큰을 비활성화한다.**
>
> - 1 user N device (PC + 모바일 브라우저 + 태블릿) — 단일 토큰 모델은 다중 디바이스 사용자를 차단
> - FCM `UNREGISTERED` / `INVALID_ARGUMENT` 응답 시 해당 토큰을 즉시 `disabled = true` 처리
> - 토큰 재등록은 frontend가 SW 등록 직후 항상 `POST /notifications/tokens`로 보냄 → 서버는 중복 토큰을 무시
> - 만료된 토큰으로의 재발송이 누적되지 않도록 발송 직전에 `disabled = false` 필터

> **발송은 비동기로 처리하고, 도메인 트랜잭션 결과를 차단하지 않는다.**
>
> - 알림 발송 실패가 카드 상태 전이·일정 갱신 같은 도메인 트랜잭션을 롤백시키면 안 됨
> - 도메인 이벤트는 `@TransactionalEventListener(phase = AFTER_COMMIT)`로 받아, 트랜잭션 커밋 후 발송
> - 발송 자체는 `@Async` Executor에서 실행 — FCM API 응답 지연이 사용자 요청 응답 시간에 영향 X
> - 실패한 발송은 재시도 큐(v1 단순 in-memory) → v2 `notification_dispatch` 테이블 + 배치 재시도

## 대안 검토 (Alternatives Considered)

### 알림 채널

**Option A — AWS SES 이메일 먼저**
- 장점: 인프라 친숙, FCM 외부 의존 없음
- 거부 이유:
    - SRS 복습 알림에서 분 단위 지연은 학습 가치 손실로 직결
    - 사용자가 이메일을 보는 시점이 복습 타이밍과 무관해 핵심 가치 미달
    - 향후 모바일 앱으로 확장 시 채널 통합 비용 발생

**Option B (선택) — FCM 웹 푸시 먼저**
- 비용: VAPID 키 발급·Service Worker 등록·Firebase 프로젝트 운영 부담
- 보상: SRS 타이밍 정확도 우위. 모바일 앱 추가 시 동일 토큰 모델 재사용
- 트레이드오프 수용 근거: SRS 본질에 가장 부합. 채용 관점에서도 "왜 FCM인가" 답변이 SRS 도메인 이해와 직결

**Option C — 인앱 배너만 (서버 폴링)**
- 거부 이유: 앱을 열어야 보임 → SRS "복습 타이밍을 알려주는" 목적 자체 미달

### 트리거 결합

**Option A — UserSchedule이 직접 알림 발송**
- 거부 이유: BC 경계 흐릿. 후속 채널 추가 시마다 UserSchedule이 흔들림

**Option B (선택) — 도메인 이벤트로 분리, Notification BC가 구독**
- 비용: 이벤트 발행/구독 추가 코드 + 트랜잭션 경계 명시 부담
- 보상: 채널 추가·재시도·실패 처리를 Notification BC 안에 격리

**Option C — 별도 메시지 큐(SQS/EventBridge)부터 도입**
- 거부 이유: 단일 인스턴스 단계에서 비용 대비 가치 낮음. v2 다중 인스턴스 전환 시 도입

### 디바이스 토큰 모델

**Option A — User에 단일 토큰 컬럼**
- 거부 이유: 다중 디바이스 사용자 차단. 토큰 교체 빈도가 높음

**Option B (선택) — DeviceToken Entity, 1 user N device**
- 비용: 토큰 만료 관리 로직 추가
- 보상: 다중 디바이스 자연 지원, 발송 실패 시 토큰 단위 비활성화

**Option C — Refresh Token처럼 Redis에 저장**
- 거부 이유: 영속성·감사 요구 (사용자가 동의한 디바이스 목록)에 부적합

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 배치

```
┌─────────────────────────────────────────────────────────────┐
│ Presentation                                                 │
│   NotificationController (POST /notifications/tokens,        │
│                           PATCH /notifications/preferences)  │
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────────┐
│ Application                                                  │
│   NotificationCommandService (token 등록·삭제·preference 갱신)│
│   NotificationDispatchService (이벤트 구독·발송 조율)          │
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────────┐
│ Domain (Notification BC)                                     │
│   DeviceToken (Entity)                                       │
│   NotificationPreference (VO, User 매달림)                    │
│   NotificationCategory enum (REVIEW/DAILY_TARGET/INACTIVE)   │
│   PushPort (outbound port)                                   │
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────────┐
│ Infrastructure                                               │
│   FcmPushAdapter (PushPort 구현, FCM Admin SDK)              │
│   DeviceTokenJpaRepository, NotificationPreferenceJpa...     │
│   ReviewDueEventListener (@TransactionalEventListener)       │
└─────────────────────────────────────────────────────────────┘
```

### 핵심 플로우

**1. 복습 예약 도래 알림**
```
Card.softSchedule interval 완료 시점
   │
   ▼
CardReviewDueEvent 발행 (Card BC, 도메인 이벤트)
   │
   ▼ AFTER_COMMIT
ReviewDueEventListener.handle()
   ├─ NotificationPreference 조회 (REVIEW.WEB_PUSH = true 확인)
   ├─ DeviceToken 활성 목록 조회
   └─ NotificationDispatchService.dispatch(REVIEW, payload)
                  │
                  ▼ @Async
            FcmPushAdapter.send()
                  ├─ FCM API 호출
                  ├─ UNREGISTERED 응답 → DeviceToken.disable()
                  └─ 메트릭 기록 (notification_sent_total{category, status})
```

**2. dailyTarget 미달성 알림 (스케줄러)**
```
매일 23:00 cron (Spring @Scheduled)
   │
   ▼
DailyTargetCheckJob.run()
   ├─ UserSchedule 전체 조회 → 미달성 사용자 필터
   ├─ DailyTargetMissedEvent 발행 (사용자별)
   └─ (동일 dispatch 경로)
```

**3. 토큰 등록 — frontend가 SW 등록 직후**
```
POST /notifications/tokens { token: "..." }
   │
   ▼
NotificationCommandService.registerToken(userId, token)
   ├─ 동일 token 중복 시 (deleted_at IS NULL) → no-op
   ├─ 신규 → DeviceToken.create(userId, token, activeFlag=true)
   └─ 응답: 201 Created
```

### Out-of-Process 의존

- **FCM Admin SDK** — Firebase 프로젝트 + Service Account 키 (Secrets Manager 저장)
- **MySQL** — `device_token`, `notification_preference`, (v2) `notification_dispatch` 테이블
- **Card / UserSchedule BC** — 도메인 이벤트 발행 측 (Notification은 구독자 입장)
- **Spring Scheduler** — 일일 cron (dailyTarget 미달성, 장기 미접속)

### 핵심 컴포넌트

| 컴포넌트 | 위치 | 책임 |
| --- | --- | --- |
| `DeviceToken` | `Notification/domain/model/` | 디바이스 토큰 Entity, 활성화·만료 처리 |
| `NotificationPreference` | `Notification/domain/model/` | 카테고리×채널 동의 VO |
| `PushPort` | `Notification/domain/port/` | 발송 outbound port |
| `FcmPushAdapter` | `Notification/infrastructure/push/` | FCM Admin SDK 구현체 |
| `NotificationDispatchService` | `Notification/application/` | 이벤트 구독·preference 검사·발송 조율 |
| `NotificationController` | `Notification/presentation/` | 토큰 등록·preference 갱신 API |
| `ReviewDueEventListener` | `Notification/infrastructure/event/` | Card 이벤트 구독 진입점 |

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | ErrorCode | HTTP | 클라이언트 권장 동작 |
| --- | --- | --- | --- |
| 토큰 미등록 사용자에게 발송 시도 | (내부) `NO_DEVICE_TOKEN` | — | 발송 skip + WARN 로그 |
| FCM `UNREGISTERED` 응답 | (내부) 토큰 disable | — | 다음 발송에서 자동 제외 |
| FCM `INVALID_ARGUMENT` | (내부) | — | 토큰 disable + 에러 메트릭 +1 |
| FCM 503 / 타임아웃 | (내부) 재시도 큐 진입 | — | v1 in-memory 재시도 (max 3), v2 배치 |
| Preference에 카테고리가 OFF | (정상 흐름) | — | 발송 skip, 메트릭 `preference_blocked_total` |
| 사용자가 잘못된 토큰 등록 | `NOTIFICATION_TOKEN_INVALID` | 400 | 형식 검증 실패, 재시도 권장 |
| 동의하지 않은 카테고리 임의 발송 | (내부 버그) | — | 단위 테스트로 차단, 발견 시 즉시 fix |

### 로깅 정책

- **항상 기록**:
    - 발송 결과 (INFO, `category`·`tokenIdHash`·`fcmMessageId`·`durationMs`)
    - 토큰 disable (WARN, `tokenIdHash`·`reason`)
- **DEBUG**: FCM 응답 본문 (prod 비활성)
- **절대 금지**:
    - 디바이스 토큰 원문 (해시 8자만)
    - FCM Service Account 키 본문
    - 알림 payload 안의 개인정보 (예: 이메일 주소)

### 관측 지표

| 지표 | 형식 | 의미 |
| --- | --- | --- |
| `notification_sent_total{category, status}` | 카운터 | 카테고리·결과별 발송 누적. status=`success`/`failed`/`disabled`/`blocked` |
| `notification_dispatch_latency_seconds{category}` | 히스토그램 | 이벤트 수신→발송 완료 지연 |
| `device_token_active_total` | 게이지 | 현재 활성 토큰 수 (preference ON × disabled=false) |
| `notification_failure_rate_5m` | 게이지 | 최근 5분 실패율. SLO: <5% |
| `preference_optin_rate{category}` | 게이지 | 카테고리별 opt-in 비율 |

## 롤아웃 / 마이그레이션 (Rollout)

### 전제 — UserSchedule v2 interval 안정화 후

UserSchedule의 interval 계산이 안정화된 직후가 알림 트리거 발행 인터페이스 명세에 가장 적합. UserSchedule이 흔들리면 Notification 구독자가 함께 흔들린다.

### Product 의존성

- **선행 Product**:
    - UserSchedule v2 명세 확정 (interval 도래 시점 발행 인터페이스)
    - Product 0-b 메트릭 가시화 (발송 메트릭 노출 인프라)
    - Product 7 Secrets·Terraform (FCM Service Account 키 안전 주입)
- **후행 Product**:
    - product-admin: 운영자가 사용자 알림 강제 해제·재발송 명령 (v2)
    - product-payment(가정): 결제 알림 카테고리 추가 (v2)

### Epic·Story 의존성 그래프

```
Epic 1 (도메인 모델 + 토큰 등록 API)
  Story 1-1 DeviceToken Entity + 정적 팩토리 + 단위 테스트
  Story 1-2 NotificationPreference VO + 기본값 결정
  Story 1-3 POST /notifications/tokens API + Slice 테스트
       │
       ▼
Epic 2 (FCM Adapter + 발송 동기 경로)
  Story 2-1 PushPort + FcmPushAdapter (FCM Admin SDK)
  Story 2-2 NotificationDispatchService (preference 검사 + 발송)
  Story 2-3 발송 실패 시 토큰 disable 로직
       │
       ▼
Epic 3 (이벤트 구독 — 복습 예약 도래)
  Story 3-1 Card BC에 CardReviewDueEvent 발행 (Card 측 Story로 분리 가능)
  Story 3-2 ReviewDueEventListener (@TransactionalEventListener AFTER_COMMIT)
  Story 3-3 @Async 발송 + 에러 격리
       │
       ▼
Epic 4 (스케줄러 — dailyTarget · 미접속)
  Story 4-1 DailyTargetCheckJob (@Scheduled 23:00)
  Story 4-2 InactiveUserCheckJob (7/14/21/28일 발송 중복 차단)
  Story 4-3 발송 멱등성 키 (notification_dispatch.idempotency_key, v2 일부 선반영)
       │
       ▼
Epic 5 (Preference API + 관측)
  Story 5-1 PATCH /notifications/preferences
  Story 5-2 메트릭 노출 + 5xx 알림 SLO 등록
  Story 5-3 docs/notification.md + 운영 Runbook
```

### 환경별 설정 분기

| 항목 | dev | prod |
| --- | --- | --- |
| FCM 프로젝트 | Firebase dev 프로젝트 | Firebase prod 프로젝트 |
| Service Account 키 | 로컬 secret + .gitignore | Secrets Manager |
| 발송 enable | true (테스트 토큰만) | true |
| `@Async` Executor 풀 크기 | 2 | 8 |
| 재시도 max | 1 | 3 |
| 스케줄러 cron | `0 23 * * *` (KST) | 동일 |

## 성공 지표 (KPI)

- 복습 예약 도래 → 알림 발송 지연 P95 ≤ 30초
- 알림 발송 성공률 ≥ 95% (FCM 응답 OK 기준)
- 토큰 disable → 다음 발송에서 제외까지의 지연 = 0 (다음 발송 시점에 즉시 적용)
- REVIEW 카테고리 opt-out 비율 ≤ 10% (사용자가 의도적으로 끄는 비율)
- 7일 미접속 사용자 중 알림 수신 후 24시간 이내 재접속률 ≥ 30% (재참여 측정)
- 비밀(토큰·SA 키)이 로그에 출력되는 사고 = 0건

## Scope

**In Scope (v1)**:
- FCM 웹 푸시 단일 채널
- 카테고리 4종: `REVIEW`, `DAILY_TARGET`, `INACTIVE`, `MARKETING`
- DeviceToken Entity + 활성화 관리
- NotificationPreference VO + opt-in/out API
- 도메인 이벤트 기반 트리거 (Card·UserSchedule 발행 → Notification 구독)
- 스케줄러 2종: dailyTarget 미달성, 장기 미접속
- 메트릭 노출

**Out of Scope (v1)**:
- 모바일 앱 푸시 — v2 (동일 FCM 토큰 모델 재사용 가능)
- 이메일 채널(SES) — v2 (onboarding·주간 리포트 용도)
- SMS 알림 — 비용·국제 발송 복잡도 대비 가치 낮음
- 알림 발송 이력 영구 저장 — v1은 메트릭만, v2에서 `notification_dispatch` 테이블 도입
- 사용자 정의 알림 시간대 (예: "오후 8시에 받기") — v2

## 대상 사용자

- **학습자** — 복습 타이밍·일일 목표·복귀 시점을 능동적으로 인지하고, 앱을 여는 비용 없이 학습 루프 유지
- **운영자** — 알림 발송 메트릭·실패율로 사용자 참여도 추적, SLO 위반 시 조치

## 연결된 Epic 목록

- [ ] Epic 1: 도메인 모델 + 토큰 등록 API
- [ ] Epic 2: FCM Adapter + 발송 동기 경로
- [ ] Epic 3: 이벤트 구독 (복습 예약 도래)
- [ ] Epic 4: 스케줄러 (dailyTarget · 장기 미접속)
- [ ] Epic 5: Preference API + 관측

## 관련 문서

- 의존 Product: `in-progress/product-log.md` (MDC traceId 발송 로그 연계), `in-progress/product-op.md` (메트릭), `in-progress/product-infra-ops.md` (Secrets Manager)
- 관련 ADR (예정): `ADR-NOTIFICATION-001 ~ 003` (채널 / 트리거 / Preference)
- DOMAIN.md 추가 예정 섹션: `Notification BC` 신규 절
- PACKAGE.md 추가 예정 섹션: `com.example.thirdtool.Notification.*` 4계층 매핑

## 열린 질문 (Open Questions)

- 웹 푸시 단독으로 시작하되, 모바일 앱 일정이 언제 잡히는가? (FCM 토큰 모델은 동일하지만 토큰 만료 정책이 다를 수 있음)
- NotificationPreference를 User Aggregate 안에 두는가, 별도 Aggregate로 두는가? (User BC 의존 정책 확인 필요)
- 발송 멱등성 키를 v1에서 도입할지, v2로 미룰지 — 중복 발송 위험이 사용자 경험에 미치는 영향 측정 후 결정
- 장기 미접속 알림 발송 후 사용자가 28일까지도 복귀하지 않으면 자동 비활성화할 것인가? (Preference + DeviceToken 양쪽 영향)
