# [Product] 인증 인프라 FE — AT 쿠키 + RT 메모리 + 인터셉터 refresh 1회 재시도

## Product Vision

> 백엔드가 "AT = HttpOnly Cookie 30분 / RT = React 메모리 7일"으로 확정한 토큰 라이프사이클을, FE에서 자동 첨부(쿠키) + 메모리 관리(RT) + 인터셉터 refresh 1회 재시도 + 비복구 에러 코드 세션 클리어 4가지 실체로 정확히 구현한다. FE 어느 컴포넌트도 AT를 인지하지 않는다. RT는 `refreshTokenStore` 한 곳에서만 관리되며, 401 처리는 `lib/api/client.ts` 인터셉터가 유일하게 담당한다.

## 배경 및 문제

- **현재 상황 (As-Is)**
  - `lib/api/client.ts`가 이미 인터셉터 구현. `REFRESH_RECOVERABLE_CODES = ['AUTH002']`, `REFRESH_TERMINAL_CODES = ['AUTH001','AUTH003','AUTH004','AUTH101','AUTH102','AUTH103','AUTH104']`로 분류
  - `features/auth/refreshTokenStore.ts` — 메모리 RT (getter/setter)
  - `withCredentials: true` — AT 쿠키 자동 전송 설정 완료
  - `X-Request-Id` 헤더 자동 부착
- **발생하는 문제**
  - refresh race condition 잠재: 동시 AUTH002 여러 요청 시 refresh 다중 발행 위험 (`refreshPromise` 싱글턴으로 방어하지만 검증 부족)
  - `AUTH002` 이외의 401 코드가 새로 추가되면 매핑 누락 위험
  - `withCredentials` 설정이 CORS 서버 응답과 정확히 일치해야 함 (dev/prod 각 도메인 검증 필요)
  - 개발 환경(localhost:8080 vs :5173) SameSite=Strict일 때 쿠키 미전송 이슈 재현 필요
  - 신규 유저(RT 없음 상태)에서 인증 필요 API 호출 시 401 흐름이 로그인 페이지로 회복되는지 검증 부족
- **왜 지금 해결해야 하는가**
  - 백엔드 Product 1 (인증 인프라) Epic 3 (필터·예외 통일) 완료 직후가 FE 인터셉터를 최종 검증할 시점
  - `product-fe-cdn.md`가 CloudFront 도메인 도입 → CORS·쿠키 정책이 프로덕션 도메인에서도 정합해야 함

## 목표 (To-Be)

- `lib/api/client.ts` 인터셉터가 AUTH002 발생 시 1회 refresh + 재시도, 성공률 ≥ 99%
- 다중 병렬 AUTH002 요청 시 refresh는 정확히 1회만 실행 (refreshPromise 싱글턴)
- REFRESH_TERMINAL_CODES 발생 시 즉시 세션 클리어 → `SessionWatcher` → `/login` replace
- MSW handler 시나리오로 AT 만료/RT 만료/RT reused 재현 가능
- CORS + SameSite=None (prod) 설정이 백엔드와 정합 (`api.thirdtool.dev` ↔ `thirdtool.dev` 오리진)
- FE 어느 컴포넌트에서도 `Authorization` 헤더를 수동 삽입하지 않는다 (grep 0건)

## 설계 결정 (Design Decisions)

- **AT는 HttpOnly Cookie — FE 코드에서 인지·저장 금지**
  - `withCredentials: true` 설정으로 axios가 자동 첨부
  - `refreshTokenStore`는 AT 저장 API를 노출하지 않음
- **RT는 메모리 전용 (`refreshTokenStore`)** — localStorage/sessionStorage 금지
  - 새로고침 시 유실. AT가 30분 살아있으므로 그 안에 필요 API 호출은 정상 (백엔드 정책 정합)
  - AT 만료 후 새로고침 → RT 없음 → 로그인 강제 (수용된 트레이드오프)
- **refresh 1회 재시도 + refreshPromise 싱글턴**
  - `RetriableConfig._retry` 플래그로 무한 루프 방지
  - 병렬 AUTH002 시 `refreshPromise ?? runRefresh()` 패턴으로 단일 refresh 보장
- **`REFRESH_TERMINAL_CODES` 발생 시 즉시 세션 클리어 + login 유도**
  - 회복 불가 상태를 정확히 표현. 자동 재시도 하지 않음
  - AUTH001(missing) / AUTH003(invalid) / AUTH004(user not found) / AUTH101(rt invalid) / AUTH102(rt not found) / AUTH103(rt reused) / AUTH104(rt missing)
- **에러 응답 표준화 = `ApiError` 커스텀 클래스**
  - `code`/`status`/`requestId`/`path`/`timestamp` 필드
  - 개발 환경에서 5xx + requestId는 console.warn (진단 편의)
- **503 = 유지보수 페이지 진입**
  - `ApiError('MAINTENANCE', ...)`로 wrap → SessionWatcher 또는 라우팅 시스템이 `/maintenance` replace
- **CORS 정책 = 도메인 상수화**
  - dev: `http://localhost:8080` ↔ `http://localhost:5173` (SameSite=Lax OK)
  - prod: `https://api.thirdtool.dev` ↔ `https://thirdtool.dev` (SameSite=None + Secure)
  - `VITE_API_BASE_URL` 환경변수로 base URL만 분기

## 대안 검토 (Alternatives Considered)

### AT 저장 위치

**Option A — AT localStorage / RT HttpOnly Cookie**
- 거부 이유: 백엔드 정책 역방향. AT XSS 취약

**Option B (선택) — AT HttpOnly Cookie / RT React 메모리** (백엔드 결정 정합)
- 비용: 새로고침 시 RT 유실
- 보상: FE는 AT 인지 0건. XSS로 RT 탈취 어려움 (fetch 오리진 제약)

### refresh race 방어

**Option A — 매 요청마다 refresh 호출**
- 거부 이유: refresh 폭증

**Option B (선택) — refreshPromise 싱글턴 + `_retry` 플래그**
- 비용: 첫 실패 요청 재시도 로직 필요
- 보상: 병렬 AUTH002 케이스 안전. 이미 구현됨

### 세션 손실 알림 채널

**Option A — 각 페이지에서 개별 처리**
- 거부 이유: 중복·누락. 인터셉터가 유일 진입점이어야 함

**Option B (선택) — `setSessionLostHandler` 전역 등록 + SessionWatcher가 라우팅**
- 비용: 전역 handler 등록 순서 관리
- 보상: 단일 지점. 이미 구현됨

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 / 라우트 배치

```
[사용자 요청]
    │
    ▼
components → useMutation/useQuery → apiClient (axios instance)
                                          │
                                          ▼
                            ┌─────────────────────────────────┐
                            │ Request Interceptor              │
                            │  ├─ X-Request-Id 부착            │
                            │  └─ withCredentials: true (AT 쿠키)│
                            └─────────────────────────────────┘
                                          │
                                          ▼
                              [Backend api.thirdtool.dev]
                                          │
                                          ▼
                            ┌─────────────────────────────────┐
                            │ Response Interceptor              │
                            │  ├─ 200 → passthrough            │
                            │  ├─ 401 + AUTH002 → refresh 1회  │
                            │  │    └─ 성공 → 원 요청 재시도    │
                            │  │    └─ 실패 → handleSessionLost │
                            │  ├─ 401 + TERMINAL → handleSession│
                            │  ├─ 503 → MAINTENANCE            │
                            │  ├─ 5xx → INTERNAL_ERROR         │
                            │  └─ ApiError로 wrap 후 reject   │
                            └─────────────────────────────────┘
                                          │
                                          ▼
                              components onError 핸들러
                              (인라인 / 토스트 / 라우팅)

Session 관리:
  refreshTokenStore (메모리)
    ├─ getMemoryRefreshToken()
    ├─ setMemoryRefreshToken(rt)
    └─ subscribe(handler)  ← SessionWatcher가 구독
       │
       ▼
  SessionWatcher (mount at App root)
    └─ session lost → navigate('/login', replace) + queryClient.clear
```

### 핵심 플로우

**1. 정상 API 호출 (AT 유효)**
```
useQuery → apiClient.get('/cards')
   ├─ Cookie 자동 첨부 (AT)
   ├─ 200 응답 → Zod parse → return
   └─ TanStack Query 캐시 저장
```

**2. AT 만료 → 자동 refresh 재시도**
```
apiClient.get('/cards')
   ├─ 401 AUTH002
   ├─ Response Interceptor
   │    ├─ REFRESH_RECOVERABLE ('AUTH002') 확인
   │    ├─ original._retry === undefined → refresh 시도
   │    ├─ refreshPromise = runRefresh()
   │    │    ├─ POST /jwt/refresh { refreshToken: 메모리 RT }
   │    │    ├─ 성공: 새 RT 저장 + resolve
   │    │    └─ 실패: reject → handleSessionLost + throw ApiError
   │    ├─ 병렬 요청은 같은 refreshPromise await
   │    ├─ original._retry = true → apiClient.request(original)
   │    └─ 재시도 결과 반환
```

**3. RT 없음 or Terminal 에러 → 세션 클리어**
```
apiClient.get('/cards')
   ├─ 401 AUTH104 (RT missing)
   ├─ Response Interceptor
   │    ├─ REFRESH_TERMINAL 확인
   │    ├─ getMemoryRefreshToken() !== null → handleSessionLost()
   │    │    ├─ setMemoryRefreshToken(null)
   │    │    └─ onSessionLost() → SessionWatcher → navigate('/login', replace)
   │    └─ reject(ApiError('AUTH104', ...))
```

**4. Maintenance / 5xx**
```
apiClient.get('/cards')
   ├─ 503 → ApiError('MAINTENANCE', ...) → SessionWatcher가 /maintenance replace
   ├─ 500~502 → ApiError('INTERNAL_ERROR', ...) → 컴포넌트 onError → 토스트 "일시적 오류"
```

### 외부 의존

- **백엔드 `/login`, `/social/login/{provider}`, `/jwt/refresh`**: apiClient 위임
- **CORS 정책**: `api.thirdtool.dev` ↔ `thirdtool.dev` (product-fe-cdn 소유)
- **MSW handler** (`mocks/handlers/auth.ts`): dev/test용, 8개 401 코드 재현

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답 (401 코드 매핑 핵심 매트릭스)

| ErrorCode | 의미 | HTTP | 인터셉터 행동 (UX) |
| --- | --- | --- | --- |
| `AUTH001` | AT 쿠키 미존재 | 401 | terminal → 세션 클리어 → /login |
| `AUTH002` | AT 만료 | 401 | recoverable → refresh 1회 재시도 |
| `AUTH003` | AT 서명/형식 불일치 | 401 | terminal → 세션 클리어 (변조 의심) |
| `AUTH004` | AT는 유효하나 유저 삭제 | 401 | terminal → 세션 클리어 |
| `AUTH101` | RT invalid | 401 | terminal → 세션 클리어 |
| `AUTH102` | RT not found | 401 | terminal → 세션 클리어 |
| `AUTH103` | RT reused | 401 | terminal → 세션 클리어 + 보안 알림 (v2) |
| `AUTH104` | RT missing | 401 | terminal → 세션 클리어 |
| `MAINTENANCE` | 백엔드 503 | 503 | ApiError wrap → `/maintenance` replace |
| `INTERNAL_ERROR` | 5xx (500~502) | 5xx | 토스트 "일시적 오류" |

MSW handler로 위 8개 401 + 2개 5xx 재현. `mocks/handlers/auth.ts`.

### 로깅 정책 (FE)

- **항상 기록**:
  - 개발 환경: 5xx + requestId를 `console.warn`
  - 프로덕션(Sentry): `ApiError.code + status + requestId + path` 기록
- **debug**: refresh 진입/성공/실패 이벤트
- **절대 금지**:
  - RT/AT 원문 (심각)
  - 요청 payload 전체 reflection
  - 사용자 비밀번호 · 소셜 access_token

### 관측 지표

- `auth_refresh_success_ratio` — AUTH002 대비 refresh 성공률 ≥ 99%
- `auth_refresh_race_saved_total` — 병렬 refresh 중복 방지 카운트 (0에 가까울수록 완벽)
- `session_lost_total{code}` — 세션 클리어 트리거 코드 분포
- `interceptor_latency` — Response Interceptor 처리 지연 P95 ≤ 20ms
- CORS 오류 발생 카운터 (0건 유지)

## 롤아웃 / 마이그레이션 (Rollout)

### 전제

인터셉터 실체는 이미 구현. 본 Product는 **검증 강화 + MSW 시나리오 정착 + 문서화** 중심.

- 현재 사용자: 인증 필요 API 소비자 (기존 로직 유지, 검증만 추가)
- 백엔드 Product 1 (인증 인프라) Epic 3 완료가 선행 조건
- product-fe-cdn.md CloudFront 도메인 확정

### Product 의존성

- 선행: `product-fe-cdn.md` (CORS·SameSite 프로덕션 도메인 확정)
- 후행: **모든 인증 필요 Product** — 인터셉터 안정성이 상위 UX 신뢰의 기반
  - `Product.md` (User FE), `product-deck.md`, `product-media.md`, `product-search.md`, `product-aisuggestion.md`, `product-ai-interactive-roadmap.md`

### Epic·Story 의존성 그래프

```
Epic 1 (에러 코드 매핑 검증)
  Story 1-1 (REFRESH_RECOVERABLE / TERMINAL 상수 리뷰) ─► 1-2 (MSW handler로 8개 코드 재현)

Epic 2 (refresh race 방어 검증)
  Story 2-1 (병렬 요청 Vitest) ─► 2-2 (refreshPromise 싱글턴 검증)

Epic 3 (세션 손실 → 라우팅 회복)
  Story 3-1 (SessionWatcher 이벤트 구독) ─► 3-2 (login replace + queryClient.clear)

Epic 4 (CORS + 쿠키 정합성 검증)
  Story 4-1 (dev localhost 케이스) ─► 4-2 (prod thirdtool.dev 케이스 E2E)
```

### 환경별 설정 분기

| 항목 | dev (`.env.development`) | prod (`.env.production`) |
| --- | --- | --- |
| `VITE_API_BASE_URL` | http://localhost:8080 | https://api.thirdtool.dev |
| Cookie SameSite | Lax | None (Secure 필수) |
| Cookie Secure | false | true |
| MSW | active (`mocks/handlers/auth.ts`) | inactive |
| Sentry | local | enabled |

## 성공 지표 (KPI)

| 지표 | 목표 값 | 측정 |
| --- | --- | --- |
| AUTH002 refresh 성공률 | ≥ 99% | 텔레메트리 |
| 병렬 AUTH002 시 refresh 중복 호출 | 0건 | Vitest |
| REFRESH_TERMINAL 매핑 누락 | 0건 | 코드 리뷰 + 단위 테스트 |
| `Authorization` 헤더 수동 삽입 | 0건 | 코드 grep |
| MSW로 8개 401 코드 재현 가능 | 100% | 테스트 매트릭스 |
| Response Interceptor 처리 지연 | P95 ≤ 20ms | 벤치마크 |

## Scope

**In Scope**:
- `lib/api/client.ts` (인터셉터 실체 + ApiError)
- `features/auth/refreshTokenStore.ts` (메모리 RT)
- `features/auth/SessionWatcher.tsx` (세션 손실 라우팅)
- `features/auth/components/ProtectedRoute.tsx`
- MSW handler (`mocks/handlers/auth.ts`) — 8개 401 + 2개 5xx 재현

**Out of Scope**:
- 로그인/회원가입 UI → `Product.md` (User)
- 소셜 provider 진입점 → `Product.md` (User)
- 다중 디바이스 세션 → 백엔드 v2
- CSRF 토큰 명시 도입 → 백엔드 v2 (열린 질문)

## 대상 사용자

- **모든 인증 요청 주체** — 로그인 후 API를 호출하는 모든 사용자 (직접적 UX 인식은 없음, 침묵의 인프라)
- **FE 개발자** — 인터셉터 실체를 이해하고 컴포넌트에서 onError 처리 규칙 준수
- **QA / 보안 담당** — MSW handler로 8개 401 시나리오 재현 및 회귀 방지

## 연결된 Epic 목록 (진행 순서)

| 순서 | Epic | 제목 | Story 수 | 선행 의존 |
| --- | --- | --- | --- | --- |
| 1 | Epic 1 | 에러 코드 매핑 검증 | 2 | (없음) |
| 2 | Epic 2 | refresh race 방어 검증 | 2 | Epic 1 |
| 3 | Epic 3 | 세션 손실 → 라우팅 회복 | 2 | Epic 1 |
| 4 | Epic 4 | CORS + 쿠키 정합성 검증 | 2 | Epic 3 (SessionWatcher 안정) |

- [ ] Epic 1: 에러 코드 매핑 검증
- [ ] Epic 2: refresh race 방어 검증
- [ ] Epic 3: 세션 손실 → 라우팅 회복
- [ ] Epic 4: CORS + 쿠키 정합성 검증

## 관련 문서

- 백엔드 원본: `workflow/task/pes/workspectrum/sdd/in-progress/product-auth.md`
- 백엔드 ADR 후보: `ADR-AUTH-001: Token Storage Strategy`
- FE-ADR 후보: `FE-AUTH-001: 인증 토큰 보관 확정` (본 Product 결과)
- 인접 FE Product: `./Product.md` (User UX), `./product-fe-cdn.md` (도메인/CORS)

## 열린 질문

- **CSRF 토큰 명시 도입 시점** — 외부 결제 콜백 등 다른 오리진 진입이 생기는 시점
- **RT 탈취 감지(AUTH103) 후 대응 강도** — 즉시 전체 세션 무효화(백엔드) + FE에서 사용자 알림 dialog 필요할지
- **세션 만료 30분 카운트다운 UI 노출 여부** — 사용자 인지 부담 vs 편의 트레이드오프
- **Sentry 도입 시 requestId를 breadcrumb에 자동 첨부** — 백엔드 로그와 correlate

---

# [Epic 1] 에러 코드 매핑 검증

## Epic 목표

`REFRESH_RECOVERABLE_CODES` / `REFRESH_TERMINAL_CODES` 상수가 백엔드가 실제 반환하는 8개 401 코드와 정확히 매핑되는지 확인. MSW handler로 각 코드를 재현.

## 배경

인터셉터 실체는 있지만, 백엔드가 새 401 코드를 추가하거나 semantics를 바꿀 때 FE 매핑 누락 위험. Product의 첫 검증 Epic.

## 완료 기준

- [ ] Story 1-1, 1-2 완료
- [ ] MSW handler로 8개 401 코드 재현 가능
- [ ] 코드 리뷰: 상수 배열이 백엔드 목록과 정확히 일치
- [ ] FE-ADR-AUTH-001 (인증 토큰 보관 확정) 작성

## [Story 1-1] REFRESH_RECOVERABLE / TERMINAL 상수 리뷰

### User Story
- As a FE 개발자
- I want 8개 401 코드가 recoverable(1) / terminal(7)로 정확히 분류되어 있는지 확인하기를
- so that 누락된 코드가 세션 클리어 없이 흘러가는 사고를 방지한다

### 설명
- `lib/api/client.ts`의 상수 배열 검토
- 백엔드 `ErrorCode/AuthErrorCode` enum과 대조 (백엔드 Product 1 참조)
- 새 코드 추가 시 상수 반영 절차 문서화

### 완료 기준 (AC)
- Given 백엔드 8개 401 코드 리스트 / When 상수 배열 확인 / Then 정확히 매핑
- Given 임의의 알 수 없는 401 코드 / When 인터셉터 / Then terminal로 안전 처리 (기본 default)
- *(엣지 - 백엔드 신규 코드)* Given AUTH005 신규 추가 / When 반영 없음 / Then Sentry alarm

### 의존성
- 선행: (없음)
- 후행: Story 1-2

## [Story 1-2] MSW handler로 8개 코드 재현

### User Story
- As a QA
- I want MSW handler로 8개 401 코드 시나리오를 각각 재현하기를
- so that 인터셉터 회귀 방지 테스트가 가능하다

### 설명
- `mocks/handlers/auth.ts`
- 각 코드마다 URL query 또는 header trigger로 발동
- Vitest에서 `msw/node`로 8개 시나리오 반복

### 완료 기준 (AC)
- Given `?forceCode=AUTH001` / When 요청 / Then 401 + AUTH001 응답
- Given AUTH002 발동 / When 인터셉터 / Then refresh 1회 재시도 → 성공
- *(엣지 - AUTH103)* Given AUTH103 발동 / When 인터셉터 / Then 세션 클리어 + Sentry 보안 이벤트

### 의존성
- 선행: Story 1-1
- 후행: Epic 2, 3

---

# [Epic 2] refresh race 방어 검증

## Epic 목표

병렬 AUTH002 요청 시 refresh가 정확히 1회만 발행되는지 검증. `refreshPromise` 싱글턴 패턴의 회귀 방지.

## 배경

인터셉터 실체는 있지만 race condition은 실사용에서만 드러나는 버그. Vitest로 사전 방어.

## 완료 기준

- [ ] Story 2-1, 2-2 완료
- [ ] 병렬 10개 요청 Vitest에서 refresh 1회만 발행 확인
- [ ] `auth_refresh_race_saved_total` 지표 계측 검토

## [Story 2-1] 병렬 요청 Vitest

### User Story
- As a FE 개발자
- I want 10개 병렬 요청이 모두 AUTH002 발생 시 하나의 refresh만 트리거되기를
- so that refresh 폭증 사고를 사전 차단한다

### 설명
- Vitest + MSW
- `Promise.all([apiClient.get('/a'), apiClient.get('/b'), ...])` 10개 병렬
- 모든 요청이 AUTH002 → 인터셉터 진입
- MSW handler로 refresh 호출 카운트

### 완료 기준 (AC)
- Given 10개 병렬 요청 / When 모두 AUTH002 / Then refresh handler 1회 호출
- Given refresh 성공 / When 재시도 / Then 10개 모두 정상 응답
- *(엣지 - refresh 실패)* Given refresh 실패 / When 병렬 대기 / Then 10개 모두 handleSessionLost

### 의존성
- 선행: Epic 1
- 후행: Story 2-2

## [Story 2-2] refreshPromise 싱글턴 검증

### User Story
- As a 코드 리뷰어
- I want refreshPromise 싱글턴이 정확히 구현되어 있는지 확인하기를
- so that 향후 리팩토링에서 실수로 깨지지 않는다

### 설명
- `lib/api/client.ts`의 refreshPromise 로직 리뷰
- 성공 후 refreshPromise = null 재설정 확인
- 실패 후 refreshPromise = null 재설정 확인
- 단위 테스트로 후속 요청이 새 refreshPromise를 받는지

### 완료 기준 (AC)
- Given refresh 성공 / When 다음 AUTH002 / Then 새 refreshPromise 발행
- Given refresh 실패 / When 다음 AUTH002 / Then 새 refreshPromise 발행 (즉 종료된 promise 재사용 X)
- *(엣지 - refresh 중 새 요청)* Given refresh 진행 중 / When 새 AUTH002 / Then 기존 refreshPromise await

### 의존성
- 선행: Story 2-1
- 후행: (없음)

---

# [Epic 3] 세션 손실 → 라우팅 회복

## Epic 목표

TERMINAL 코드 발생 시 SessionWatcher가 즉시 `/login` replace + queryClient.clear 수행. 재로그인 후 원래 경로 복귀.

## 배경

인터셉터의 handleSessionLost와 SessionWatcher 협력의 회귀 방지. Product.md Epic 4와 협력.

## 완료 기준

- [ ] Story 3-1, 3-2 완료
- [ ] SessionWatcher E2E (TERMINAL 발동 → redirect)
- [ ] queryClient.clear 검증 (개인 정보 유출 방지)

## [Story 3-1] SessionWatcher 이벤트 구독

### User Story
- As a SessionWatcher
- I want refreshTokenStore가 알리는 세션 손실 이벤트를 정확히 소비하기를
- so that TERMINAL 발생 시 즉시 반응한다

### 설명
- `features/auth/SessionWatcher.tsx`
- `refreshTokenStore.subscribe(handler)` 로 이벤트 구독
- handler 안에서 navigate + queryClient.clear

### 완료 기준 (AC)
- Given TERMINAL 이벤트 / When subscribe handler / Then navigate('/login')
- Given 이벤트 중복 / When handler 재실행 / Then 부작용 없음 (idempotent)
- *(엣지 - unmount)* Given SessionWatcher unmount / When subscribe 해제 / Then leak 없음

### 의존성
- 선행: Epic 1
- 후행: Story 3-2

## [Story 3-2] login replace + queryClient.clear

### User Story
- As a 사용자
- I want 세션 손실 후 로그인 페이지에서 이전 데이터 흔적이 남지 않기를
- so that 프라이버시가 보장된다

### 설명
- `queryClient.clear()` 호출 → 모든 TanStack Query 캐시 무효화
- `/login` replace (history 오염 방지)
- 토스트 "세션이 종료되었습니다"

### 완료 기준 (AC)
- Given 세션 손실 / When SessionWatcher trigger / Then queryClient.clear + navigate('/login', replace)
- Given 재로그인 성공 / When 새 세션 / Then 새 fetch 시작 (캐시 없음)
- *(엣지 - 이미 login page)* Given /login에서 세션 손실 / When redirect / Then 무시 (중복 방지)

### 의존성
- 선행: Story 3-1
- 후행: Epic 4

---

# [Epic 4] CORS + 쿠키 정합성 검증

## Epic 목표

dev/prod 환경에서 CORS + SameSite + Secure 쿠키가 정확히 동작하는지 E2E 검증. `product-fe-cdn.md`의 프로덕션 도메인 확정 후 최종 검증.

## 배경

CORS 설정은 백엔드·FE·인프라 3자 협력. 하나만 어긋나도 로그인 실패. Product의 마지막 검증 Epic.

## 완료 기준

- [ ] Story 4-1, 4-2 완료
- [ ] dev/prod 각 도메인에서 로그인 E2E 통과
- [ ] SameSite 정책 회귀 방지 문서화

## [Story 4-1] dev localhost 케이스

### User Story
- As a FE 개발자
- I want localhost:5173에서 localhost:8080 API 호출 시 쿠키 정상 전송되기를
- so that 개발 환경에서 인증 흐름 검증 가능

### 설명
- `http://localhost:5173` FE → `http://localhost:8080` API
- CORS: 백엔드 allowedOrigins에 localhost:5173 등록
- Cookie: SameSite=Lax (개발 환경)
- Vite dev proxy 사용 여부 결정 (열린 질문)

### 완료 기준 (AC)
- Given localhost dev / When 로그인 / Then AT 쿠키 Set-Cookie + 이후 요청 자동 첨부
- Given cross-origin XHR / When credentials: 'include' / Then 쿠키 전송
- *(엣지 - SameSite=Strict 오설정)* Given SameSite=Strict / When cross-origin / Then 쿠키 미전송 재현 + fix

### 의존성
- 선행: Epic 3
- 후행: Story 4-2

## [Story 4-2] prod thirdtool.dev 케이스 E2E

### User Story
- As a QA
- I want 프로덕션 도메인에서 크로스 서브도메인 쿠키가 정상 동작하기를
- so that 배포된 서비스가 완전 동작한다

### 설명
- Playwright E2E — `https://thirdtool.dev/login`
- `Set-Cookie`: SameSite=None; Secure; Path=/; Domain=.thirdtool.dev
- 이후 `https://api.thirdtool.dev/...` 요청 시 자동 전송

### 완료 기준 (AC)
- Given prod 로그인 / When Set-Cookie / Then SameSite=None; Secure
- Given `/api/v1/user/me` 요청 / When XHR / Then 쿠키 전송 + 200
- *(엣지 - Safari third-party)* Given Safari private / When cross-origin cookie / Then 알려진 제약 문서화

### 의존성
- 선행: Story 4-1
- 후행: (없음)
