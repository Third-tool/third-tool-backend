# [Product] 알림 FE — FCM 웹 푸시 구독 + 토스트/배너 + 설정 UI

## Product Vision

> 백엔드가 FCM 웹 푸시로 복습 예약 도래·dailyTarget 미달·장기 미접속 알림을 발송할 때, FE에서 Service Worker 등록·토큰 등록·preference 관리·인앱 표시(토스트/배너)를 통합 지원한다. 알림은 SRS 학습 루프의 마지막 연결 고리 — FE는 사용자가 앱을 열지 않아도 복습 타이밍을 인지하도록 하고, 앱을 열었을 때는 미확인 알림을 배너로 자연스럽게 이어준다.

## 배경 및 문제

- **현재 상황 (As-Is)**
  - FE에 Service Worker 없음
  - FCM SDK 미도입, 토큰 등록 API 호출 없음
  - 알림 설정 UI 없음
  - 인앱 토스트(`Toast.tsx`)는 존재하지만 서버 발화 알림 수신 채널이 없음
- **발생하는 문제**
  - SRS 복습 타이밍이 사용자 자발성에만 의존 → 이탈률 리스크
  - dailyTarget 미달 인지 채널 없음
  - 채용 관점 즉시 감점 (SRS 서비스인데 리마인더 없음)
- **왜 지금 해결해야 하는가 (backlog)**
  - UserSchedule v2 interval 계산 안정화 직후가 트리거 명세 확정 시점
  - FCM 토큰 관리·opt-in 모델은 한 번 결정하면 마이그레이션 비용 큼

## 목표 (To-Be)

- Service Worker 등록 + FCM SDK 초기화
- 사용자가 알림 권한 부여 시 토큰 발급 → `POST /notifications/tokens`
- `/notifications/preferences` PATCH로 opt-in/opt-out 관리 (`REVIEW.WEB_PUSH` / `DAILY_TARGET.WEB_PUSH` / `INACTIVE.WEB_PUSH` / `MARKETING.*`)
- 웹 푸시 클릭 시 관련 카드/화면으로 진입
- 앱 활성 상태에서도 백엔드 알림을 수신해 인앱 배너(`Toast.tsx` 확장) 표시
- 알림 설정 페이지 (`/me/notifications` 또는 `/me` 하위)

## 설계 결정 (Design Decisions)

- **FCM 웹 푸시 = 유일 채널 (v1)**
  - 이메일(SES)은 v2
  - 모바일 앱은 v2 (동일 FCM 토큰 모델 재사용 예정)
- **Service Worker 등록 시점 = 최초 알림 권한 요청 시**
  - 앱 로드마다 자동 등록 X (사용자 개입 후 등록)
  - "알림 받기" 버튼 클릭 → `Notification.requestPermission()` → SW 등록 → 토큰 발급 → 서버 등록
- **토큰 서버 등록은 사용자 로그인 상태에서만**
  - 미로그인 상태에서 SW 등록/토큰 발급하지 않음
- **preference 저장 = 낙관 update (실패 시 롤백)**
  - 사용자 체감 즉각 반응. 실패 시 이전 상태 복원 + 토스트
- **인앱 배너 vs OS 알림 = 앱 활성 여부에 따라 분리**
  - 앱 활성(Document.hidden === false): 인앱 배너만
  - 앱 비활성 or 백그라운드: OS 알림
- **알림 클릭 심층 링크**
  - 복습 예약 알림 → `/study?cardId={id}` 또는 `/archive/{cardId}`
  - dailyTarget 미달 → `/home`
  - 미접속 복귀 → `/home`
- **토큰 disabled 처리 = 로컬 인지 없음**
  - 백엔드가 FCM `UNREGISTERED` 응답 시 토큰 disable
  - FE는 다음 SW 등록 시 새 토큰 발급 → 서버 재등록으로 자동 회복

## 대안 검토 (Alternatives Considered)

### 알림 채널

**Option A — 이메일 먼저**
- 거부 이유: SRS 타이밍 지연

**Option B (선택) — FCM 웹 푸시**
- 비용: SW + FCM SDK 도입
- 보상: 즉시성

**Option C — 인앱만 (폴링)**
- 거부 이유: 앱 열어야 보임

### SW 등록 시점

**Option A — 앱 로드 자동 등록**
- 거부 이유: 권한 요청 스팸 UX

**Option B (선택) — 명시 액션 시 등록**
- 비용: 진입 UX 1회 클릭 추가
- 보상: 사용자 통제

### preference 저장

**Option A — 서버 응답 대기**
- 거부 이유: 지연

**Option B (선택) — 낙관 update**
- 비용: 롤백 로직 추가
- 보상: 즉시 반응

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 / 라우트 배치

```
[Router]
  /me/notifications → NotificationSettingsPage

public/sw.js  (Service Worker — FCM 백그라운드 핸들러)
public/firebase-messaging-sw.js  (FCM 표준)

features/notifications/
├─ NotificationSettingsPage.tsx    (preference matrix 표시)
├─ NotificationPermissionCTA.tsx   (알림 권한 요청 진입점)
├─ hooks/
│    ├─ useNotifications()          (FCM SDK + 토큰 관리)
│    ├─ useNotificationPreferences() (preference GET/PATCH)
│    └─ useInAppNotifications()     (앱 활성 시 배너 스트림)
└─ components/
     ├─ InAppBanner.tsx           (Toast 확장 — 지속형)
     ├─ CategoryToggle.tsx        (REVIEW/DAILY_TARGET/INACTIVE/MARKETING)
     └─ ChannelToggle.tsx         (WEB_PUSH/EMAIL)

lib/api/schemas/notification.ts
lib/api/endpoints/notification.ts
lib/notifications/firebaseConfig.ts (VAPID 키 등 환경 분리)
```

### 핵심 플로우

**1. 알림 권한 요청 → 토큰 등록**
```
사용자가 NotificationPermissionCTA "알림 받기" 클릭
   │
   ▼
useNotifications.requestPermission()
   ├─ Notification.requestPermission()
   │    ├─ granted: 계속
   │    ├─ denied: 안내 dialog "브라우저 설정에서 허용"
   │    └─ default: 다시 시도 CTA
   ├─ getFCMToken(vapidKey)  ← @firebase/messaging
   ├─ POST /notifications/tokens { token }
   │    ├─ 201 (신규 등록)
   │    └─ 200 (이미 등록, no-op)
   └─ 토스트 "알림이 켜졌습니다"
```

**2. 웹 푸시 수신 (백그라운드)**
```
백엔드 → FCM Admin SDK → 브라우저 SW
   │
   ▼
firebase-messaging-sw.js onBackgroundMessage
   ├─ payload.data.category, cardId 등 추출
   ├─ registration.showNotification(title, options)
   │    ├─ icon / body / data.deepLink
   │    └─ actions (선택적: "지금 복습", "나중에")
```

**3. 알림 클릭 → 심층 링크**
```
사용자가 OS 알림 클릭
   │
   ▼
SW notificationclick 이벤트
   ├─ clients.openWindow(payload.deepLink)
   └─ 예: `/study?cardId=123`
      → 카드 스터디 즉시 진입
```

**4. 앱 활성 시 인앱 배너**
```
FCM foreground message (앱 활성)
   │
   ▼
useInAppNotifications
   ├─ Toast queue에 push
   └─ InAppBanner 렌더 (사라짐 8s + 클릭 시 심층 링크)
```

**5. preference 변경**
```
사용자가 NotificationSettingsPage에서 REVIEW.WEB_PUSH 토글
   │
   ▼
useNotificationPreferences.mutate({ category: 'REVIEW', channel: 'WEB_PUSH', enabled: false })
   ├─ onMutate: 로컬 상태 즉시 갱신
   ├─ PATCH /notifications/preferences
   ├─ onError: 이전 상태 롤백 + 토스트
```

### 외부 의존

- **FCM SDK (`@firebase/messaging`)**: 브라우저 웹 푸시 표준
- **Service Worker (`public/firebase-messaging-sw.js`)**: 백그라운드 수신
- **Firebase 프로젝트 + VAPID 키**: 환경별 분리 (`firebaseConfig.ts`)
- **백엔드 `/notifications/**`**: 토큰 등록·preference·발송 로그
- **product-auth**: 로그인 상태 필수 (미로그인 시 SW 등록 안 함)

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | ApiError code | HTTP | 클라이언트 권장 동작 (UX) |
| --- | --- | --- | --- |
| 권한 denied | (Browser API) | - | 안내 dialog + 브라우저 설정 링크 |
| SW 등록 실패 | (SW API) | - | 재시도 CTA + Sentry 로그 |
| 토큰 발급 실패 | (FCM SDK) | - | 재시도, 반복 실패 시 안내 |
| FCM 미지원 브라우저 (Safari 구버전) | (사전 감지) | - | "이 브라우저는 지원하지 않습니다" 안내 |
| preference PATCH 실패 | (server) | 5xx | 낙관 롤백 + 토스트 |
| 토큰 disabled (백엔드 자동) | `TOKEN_DISABLED` | 200 (background sync) | 다음 SW 등록 시 새 토큰 발급 자동 회복 |
| 토큰 등록 중복 (idempotent) | (idempotent) | 200 | 조용히 성공, 에러 처리 없음 |
| 심층 링크 잘못된 URL | (사전 검증) | - | `/home` fallback + Sentry |

MSW handler로 서버 시나리오 4개 재현. FCM/SW는 실제 브라우저 API 테스트 (E2E).

### 로깅 정책 (FE)

- **항상 기록 (Sentry)**:
  - SW 등록 실패 이벤트 + 브라우저 정보
  - 토큰 발급 실패 이벤트
  - preference 낙관 롤백 발생 카운터
  - 심층 링크 fallback 이벤트 (URL 유효성 실패)
- **debug**: FCM foreground/background message 수신 이벤트
- **절대 금지**:
  - FCM 토큰 원문 (일회성 시크릿 성격)
  - 알림 body 본문 (사용자 데이터 포함 가능)
  - VAPID private key

### 관측 지표

- 알림 권한 부여율 (관측만, 목표 없음)
- 토큰 등록 성공률 ≥ 95%
- 백엔드 발송 후 인앱/OS 표시 지연 P95 ≤ 2s
- 심층 링크 클릭 → 올바른 화면 진입 = 100%
- preference 낙관 롤백 발생률 ≤ 1%
- SW 등록 실패율 ≤ 3%
- NotificationSettingsPage LCP P95 ≤ 1.5s

## 롤아웃 / 마이그레이션 (Rollout)

### 전제

- 현재 사용자: 알림 UI 부재 (첫 도입)
- 백엔드 `/notifications/**` API 확정 완료가 선행 조건
- UserSchedule v2 interval 계산 안정화 완료
- Firebase 프로젝트 생성 + VAPID 키 발급

### Product 의존성

- 선행: `../in-progress/product-auth.md` (로그인 상태 확인)
- 선행: `./product-user-schedule.md` (UserSchedule v2 트리거 명세)
- 후행: (없음)

### Epic·Story 의존성 그래프

```
Epic 1 (SW + FCM 초기화)
  Story 1-1 (public/sw.js + firebase-messaging-sw.js) ─► 1-2 (firebaseConfig)

Epic 2 (권한 + 토큰 등록)
  Story 2-1 (NotificationPermissionCTA) ─► 2-2 (useNotifications hook)

Epic 3 (preference 관리)
  Story 3-1 (NotificationSettingsPage matrix UI) ─► 3-2 (낙관 update)

Epic 4 (수신 + 심층 링크)
  Story 4-1 (백그라운드 수신) ─► 4-2 (심층 링크 매핑)
                                 ─► 4-3 (인앱 배너)

Epic 5 (fallback UX)
  Story 5-1 (권한 denied 안내) ─► 5-2 (지원 안 하는 브라우저 안내)
```

### 환경별 설정 분기

| 항목 | dev (`.env.development`) | prod (`.env.production`) |
| --- | --- | --- |
| `VITE_API_BASE_URL` | http://localhost:8080 | https://api.thirdtool.dev |
| `VITE_FIREBASE_CONFIG` | (test project) | (prod project) |
| `VITE_VAPID_PUBLIC_KEY` | (test) | (prod) |
| MSW | enabled (`msw/handlers/notification.ts`) | disabled |
| Sentry | local | enabled |
| SW 등록 | 조건부 (사용자 액션 시) | 조건부 (사용자 액션 시) |

## 성공 지표 (KPI)

| 지표 | 목표 |
| --- | --- |
| 알림 권한 부여율 | 텔레메트리 (관측만) |
| 토큰 등록 성공률 | ≥ 95% |
| 백엔드 발송 후 인앱/OS 표시 지연 | P95 ≤ 2s |
| 심층 링크 클릭 → 올바른 화면 진입 | 100% |
| preference 낙관 롤백 발생률 | ≤ 1% |
| SW 등록 실패율 | ≤ 3% |
| NotificationSettingsPage LCP | P95 ≤ 1.5s |

## Scope

**In Scope (backlog)**:
- `public/firebase-messaging-sw.js` + SW 등록 로직
- `features/notifications/` 전체 신규
- Firebase SDK 도입 (FE-ADR 필요)
- `Toast.tsx` 확장 (인앱 배너 지원)
- Zod 스키마 / endpoint / MSW handler

**Out of Scope**:
- 이메일 알림 UI → v2
- 모바일 앱 알림 → v2
- 알림 히스토리 페이지 → v2 (`/me/notifications/history`)
- 알림 스누즈 → v2

## 대상 사용자

- **활성 학습자** — 복습 예약 도래 시 OS 알림 수신 → 즉시 복습 진입
- **간헐적 학습자** — dailyTarget 미달·미접속 알림으로 복귀 유도
- **알림 opt-out 선호 사용자** — preference 페이지에서 세부 카테고리 조정
- **미지원 브라우저 사용자 (Safari 구버전)** — 안내 후 인앱 배너로 fallback

## 연결된 Epic 목록 (진행 순서)

| 순서 | Epic | 제목 | Story 수 | 선행 의존 |
| --- | --- | --- | --- | --- |
| 1 | Epic 1 | SW + FCM 초기화 | 2 | (없음) |
| 2 | Epic 2 | 권한 + 토큰 등록 | 2 | Epic 1 |
| 3 | Epic 3 | preference 관리 | 2 | Epic 2 |
| 4 | Epic 4 | 수신 + 심층 링크 | 3 | Epic 2 |
| 5 | Epic 5 | fallback UX | 2 | Epic 1, 2 |

- [ ] Epic 1: SW + FCM 초기화
- [ ] Epic 2: 권한 + 토큰 등록
- [ ] Epic 3: preference 관리
- [ ] Epic 4: 수신 + 심층 링크
- [ ] Epic 5: fallback UX

## 관련 문서

- 백엔드 원본: `workflow/task/pes/workspectrum/sdd/backlog/product-notification.md`
- 백엔드 ADR: ADR-NOTIFICATION-001~003
- FE-ADR 후보: `FE-NOTIF-001: FCM SDK 도입 및 SW 위치`, `FE-NOTIF-002: 인앱 배너 vs OS 알림 분리 규칙`
- 인접 FE: `./product-user-schedule.md` (트리거 명세), `../in-progress/product-auth.md` (로그인)

## 열린 질문

- **Safari 데스크탑 웹 푸시 지원** — Safari 16.4+ 지원, 그 이하 브라우저 fallback
- **알림 카테고리 세분화** — REVIEW를 "1D/3D/7D/14D/21D"로 세분화할지, 통합 유지할지
- **오프라인 시 알림 큐** — SW가 오프라인 시 나중에 표시 vs 폐기
- **다중 디바이스 알림 동기화** — 한 곳에서 확인 시 다른 곳에서 dismiss (v2 백엔드 지원 대기)

---

# [Epic 1] SW + FCM 초기화

## Epic 목표

`public/firebase-messaging-sw.js` 배치 + FCM SDK 초기화 로직 확립. 후속 Epic이 SW·SDK를 소비할 수 있는 기반.

## 배경

Product의 인프라 Epic. 후속 Epic(권한·토큰·preference·수신·fallback)은 본 Epic이 확립한 SW·SDK 초기화 위에서 동작.

## 완료 기준

- [ ] Story 1-1, 1-2 완료
- [ ] SW 파일 배포 확인 (`/firebase-messaging-sw.js` 접근 가능)
- [ ] FCM SDK 초기화 성공 (Vitest + jsdom)
- [ ] FE-ADR-NOTIF-001 (FCM SDK 도입 및 SW 위치) 작성

## [Story 1-1] public/sw.js + firebase-messaging-sw.js

### User Story
- As a FE 개발자
- I want SW 파일을 `public/` 에 배치해 브라우저가 등록 가능하기를
- so that FCM 백그라운드 수신이 동작한다

### 설명
- `public/firebase-messaging-sw.js` — FCM 표준 이름 필수
- FCM SDK importScripts 로드
- `onBackgroundMessage` handler 구현
- Vite dev server가 정적 파일로 서빙

### 완료 기준 (AC)
- Given SW 파일 배포 / When `/firebase-messaging-sw.js` GET / Then 200
- Given 브라우저 등록 / When SW register / Then active state
- *(엣지 - 개발 vs 프로덕션)* Given dev vs prod / When 등록 / Then 동일 동작 (VAPID 키만 다름)

### 의존성
- 선행: (없음)
- 후행: Story 1-2

## [Story 1-2] firebaseConfig

### User Story
- As a FE 개발자
- I want VAPID 키·프로젝트 ID를 환경변수로 관리하기를
- so that dev/prod 환경 분리가 안전하다

### 설명
- `lib/notifications/firebaseConfig.ts`
- `import.meta.env.VITE_FIREBASE_CONFIG` JSON parse
- `VITE_VAPID_PUBLIC_KEY` 별도 관리
- Firebase App 싱글턴 초기화

### 완료 기준 (AC)
- Given 환경변수 설정 / When import / Then Firebase App 인스턴스 반환
- Given 미설정 / When import / Then 명확한 에러 메시지
- *(엣지)* Given VAPID 키 형식 오류 / Then Sentry 로깅 + fallback UX

### 의존성
- 선행: Story 1-1
- 후행: Epic 2

---

# [Epic 2] 권한 + 토큰 등록

## Epic 목표

NotificationPermissionCTA로 사용자가 명시적으로 알림 권한 요청 → FCM 토큰 발급 → 서버 등록.

## 배경

Epic 1의 SW·SDK를 소비. 사용자 진입점 확립.

## 완료 기준

- [ ] Story 2-1, 2-2 완료
- [ ] 권한 부여 → 토큰 등록 E2E
- [ ] 권한 denied → 안내 dialog E2E

## [Story 2-1] NotificationPermissionCTA

### User Story
- As a 사용자
- I want 명시적인 "알림 받기" CTA로 권한 요청을 시작하기를
- so that 브라우저 권한 스팸 없이 통제할 수 있다

### 설명
- `features/notifications/NotificationPermissionCTA.tsx`
- 로그인 사용자에게만 노출
- 클릭 시 `Notification.requestPermission()` → SW 등록 → 토큰 발급

### 완료 기준 (AC)
- Given 로그인 사용자 / When 렌더 / Then CTA 표시
- Given granted / When 완료 / Then 토스트 "알림이 켜졌습니다"
- *(엣지 - denied)* Given denied / When 완료 / Then 안내 dialog "브라우저 설정에서 허용"

### 의존성
- 선행: Epic 1
- 후행: Story 2-2

## [Story 2-2] useNotifications hook

### User Story
- As a FE 컴포넌트
- I want 알림 권한 상태·토큰·등록 mutation을 hook으로 위임하기를
- so that 컴포넌트가 FCM 세부를 몰라도 되도록

### 설명
- `features/notifications/hooks/useNotifications.ts`
- `getFCMToken(vapidKey)` + `POST /notifications/tokens`
- 권한 상태 subscribe (permissionchange 이벤트)

### 완료 기준 (AC)
- Given granted 상태 / When useNotifications / Then { token, status: 'granted' } 반환
- Given 서버 등록 성공 / When mutate / Then 토스트
- *(엣지 - 미로그인)* Given 미로그인 / When hook 사용 / Then no-op + 안내

### 의존성
- 선행: Story 2-1
- 후행: Epic 3, 4

---

# [Epic 3] preference 관리

## Epic 목표

NotificationSettingsPage에서 category × channel matrix 토글 → 낙관 update PATCH.

## 배경

사용자 세부 제어. Epic 2 이후 진입 가능.

## 완료 기준

- [ ] Story 3-1, 3-2 완료
- [ ] 낙관 update 롤백 E2E
- [ ] preference 매트릭스 렌더 접근성 통과

## [Story 3-1] NotificationSettingsPage matrix UI

### User Story
- As a 사용자
- I want 알림 카테고리와 채널을 표 형태로 세밀하게 조정하기를
- so that 원하는 알림만 받을 수 있다

### 설명
- `features/notifications/NotificationSettingsPage.tsx`
- Row: REVIEW / DAILY_TARGET / INACTIVE / MARKETING
- Column: WEB_PUSH / (EMAIL v2)
- 각 셀 토글 스위치

### 완료 기준 (AC)
- Given 진입 / When 렌더 / Then matrix 표시 + 현재 preference 반영
- Given REVIEW.WEB_PUSH 토글 / When 클릭 / Then 즉시 UI 갱신
- *(엣지 - 미로그인)* Given 미로그인 / When 진입 / Then `/login` redirect

### 의존성
- 선행: Epic 2
- 후행: Story 3-2

## [Story 3-2] 낙관 update

### User Story
- As a 사용자
- I want 토글 즉시 UI가 반영되고 서버 실패 시 원복되기를
- so that 매번 로딩 스피너를 안 봐도 된다

### 설명
- `useNotificationPreferences` mutation
- `onMutate`: 로컬 cache 즉시 갱신
- `onError`: 이전 상태 롤백 + 토스트
- `onSuccess`: cache 확정

### 완료 기준 (AC)
- Given 토글 / When PATCH 성공 / Then cache 확정 + 토스트 없음 (silent success)
- Given 5xx / When onError / Then UI 롤백 + 토스트 "저장 실패, 다시 시도하세요"
- *(엣지 - 네트워크)* Given 오프라인 / When PATCH / Then 롤백

### 의존성
- 선행: Story 3-1
- 후행: (없음)

---

# [Epic 4] 수신 + 심층 링크

## Epic 목표

백그라운드 FCM 메시지 수신 + 클릭 시 심층 링크 이동 + 앱 활성 시 인앱 배너.

## 배경

Epic 2 이후 실제 알림 수신 UX 완성. Product의 사용자 가치 핵심.

## 완료 기준

- [ ] Story 4-1, 4-2, 4-3 완료
- [ ] 백그라운드 수신 E2E (test push)
- [ ] 심층 링크 매핑 3종 검증

## [Story 4-1] 백그라운드 수신

### User Story
- As a 사용자
- I want 앱을 닫아도 복습 예약 알림을 OS로 받기를
- so that 학습 타이밍을 놓치지 않는다

### 설명
- `public/firebase-messaging-sw.js` onBackgroundMessage
- `registration.showNotification(title, options)`
- payload.data에 category, cardId, deepLink 포함

### 완료 기준 (AC)
- Given 백엔드 push 발송 / When SW 수신 / Then OS 알림 표시
- Given 앱 활성 상태 / When push 도착 / Then OS 알림 안 뜸 (Epic 4-3 인앱 배너 대신)
- *(엣지 - payload 형식 오류)* Given payload 필드 부재 / When SW / Then 기본 title로 fallback + Sentry

### 의존성
- 선행: Epic 2
- 후행: Story 4-2

## [Story 4-2] 심층 링크 매핑

### User Story
- As a 사용자
- I want 알림 클릭 시 관련 카드/화면으로 이동하기를
- so that 즉시 액션 취할 수 있다

### 설명
- SW notificationclick 이벤트
- category별 매핑:
  - REVIEW → `/study?cardId={id}`
  - DAILY_TARGET → `/home`
  - INACTIVE → `/home`
- `clients.openWindow(deepLink)` 또는 기존 탭 포커스

### 완료 기준 (AC)
- Given REVIEW 알림 클릭 / When SW / Then `/study?cardId=123` 오픈
- Given 이미 열린 탭 / When 클릭 / Then 기존 탭 포커스 + navigate
- *(엣지 - 잘못된 URL)* Given deepLink 검증 실패 / When 클릭 / Then `/home` fallback

### 의존성
- 선행: Story 4-1
- 후행: Story 4-3

## [Story 4-3] 인앱 배너

### User Story
- As a 사용자 (앱 활성 상태)
- I want 앱을 열어둔 채로도 알림을 인앱 배너로 보기를
- so that OS 알림 중복 없이 자연스럽게 인지한다

### 설명
- `features/notifications/components/InAppBanner.tsx`
- FCM foreground message 수신 → Toast queue push
- 8초 후 자동 사라짐, 클릭 시 심층 링크

### 완료 기준 (AC)
- Given 앱 활성 + push 도착 / When onMessage / Then InAppBanner 표시
- Given 배너 클릭 / When 클릭 / Then 심층 링크 navigate
- *(엣지 - 여러 개 동시)* Given 3개 동시 / When 렌더 / Then queue로 순차 표시

### 의존성
- 선행: Story 4-2
- 후행: (없음)

---

# [Epic 5] fallback UX

## Epic 목표

권한 denied·미지원 브라우저·SW 등록 실패 상황에 대한 안내 UX 확립.

## 배경

Product의 안전망. 알림 사용이 불가한 사용자도 자연스럽게 대응하도록.

## 완료 기준

- [ ] Story 5-1, 5-2 완료
- [ ] denied 안내 dialog E2E
- [ ] Safari 구버전 시뮬레이션 E2E

## [Story 5-1] 권한 denied 안내

### User Story
- As a 사용자 (권한 거부 상태)
- I want 브라우저 설정에서 권한 허용하는 방법을 안내받기를
- so that 다시 활성화 가능한 경로를 안다

### 설명
- Dialog: "알림 권한이 거부되었습니다. 브라우저 주소창 왼쪽 자물쇠 아이콘 → 알림 → 허용"
- 브라우저 별 안내 (Chrome/Firefox/Edge)
- 재시도 CTA (권한 상태 재확인)

### 완료 기준 (AC)
- Given 권한 denied / When CTA 클릭 / Then Dialog 표시
- Given 사용자가 설정 변경 후 재시도 / When 재확인 / Then granted 감지
- *(엣지 - Safari)* Given Safari / When dialog / Then Safari 특유 안내

### 의존성
- 선행: Epic 2
- 후행: Story 5-2

## [Story 5-2] 지원 안 하는 브라우저 안내

### User Story
- As a 사용자 (Safari 16.4 미만 등)
- I want 이 브라우저는 웹 푸시를 지원 안 함을 명확히 안내받기를
- so that 브라우저 업그레이드 또는 대안(인앱만) 선택 가능

### 설명
- 브라우저 감지 (`'serviceWorker' in navigator && 'PushManager' in window`)
- 미지원 시 NotificationPermissionCTA 대신 "이 브라우저는 알림을 지원하지 않습니다"
- 인앱 배너는 여전히 사용 가능

### 완료 기준 (AC)
- Given 미지원 브라우저 / When 렌더 / Then 안내 문구 + 인앱 배너만 활성
- Given 지원 브라우저 / When 렌더 / Then 기존 CTA
- *(엣지 - iOS Safari 16.4+)* Given PWA 상태 필수 / When 감지 / Then "홈 화면에 추가 필요" 안내

### 의존성
- 선행: Story 5-1
- 후행: (없음)
