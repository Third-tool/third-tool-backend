# [Product] User FE — 프로필·세션·다중 소셜 진입 UI

## Product Vision

> 백엔드 User BC 리팩토링(SRP 분리 + 예외 통일 + Principal 단일화)에 대응해, FE에서 로그인·회원가입·프로필·세션 손실 처리를 단일한 UX 흐름으로 정리한다. 사용자는 자체/소셜(카카오·네이버) 어느 진입점을 골라도 동일한 세션 결과와 동일한 에러 응답 UX를 경험한다.

## 배경 및 문제

- **현재 상황 (As-Is)**
  - `features/auth/`가 LoginPage / SignupPage / OAuthCallbackPage로 분산
  - `AuthProvider` Context가 현재 사용자·세션 로딩 상태 보유. `SessionWatcher`가 refresh token store 이벤트로 세션 손실 회복 트리거
  - `features/me/MePage`가 프로필 표시. 수정/삭제 UI 미구현 or 부분
  - 소셜 로그인 콜백 처리(`OAuthCallbackPage`)에 provider별 분기가 있으나, 백엔드가 provider 확장 시 FE 코드 수정 없음이 이상적
- **발생하는 문제**
  - 백엔드 `USER_IS_SOCIAL`(소셜 유저가 자체 로그인 시도) / `USER_LOCKED` / `PASSWORD_NOT_MATCHED` 에러 응답 UX 매핑이 흐릿하거나 인라인 vs 토스트 정책 불일치
  - 프로필 수정 시 `currentUser.username`을 기준으로 함(백엔드 정책)이 FE에서 명시 안내가 없어 "다른 사람 계정 수정" 오해 우려
  - 계정 삭제 UI 부재 → 사용자가 탈퇴 flow를 완결하지 못함
- **왜 지금 해결해야 하는가**
  - 백엔드 User BC 리팩토링(Product 2)이 진행 중 — Principal 단일화·예외 체계 정착 직후가 FE UX를 정합하는 가장 싼 시점
  - 소셜 제공자 확장(구글 등 v2) 전에 FE에서도 제공자 추가 시 코드 변경 최소 구조 확립 필요

## 목표 (To-Be)

- LoginPage가 자체 + 소셜(카카오/네이버) 3 진입점을 통합 표시. 소셜은 provider slug 매핑으로 자동 확장
- 회원가입 → 첫 진입 → onboarding → home 단일 성공 경로가 명확
- 프로필 수정/삭제가 `/me`에 노출. 삭제는 재확인 dialog + confirm
- USER_IS_SOCIAL / USER_LOCKED / PASSWORD_NOT_MATCHED 에러 UX 표준화 (인라인 or 토스트 결정)
- OAuthCallbackPage는 provider slug만 알고 상세 흐름은 백엔드 위임

## 설계 결정 (Design Decisions)

- **AuthProvider Context = 현재 사용자·로딩 상태 유일 진실. useCurrentUser hook으로 소비**
  - `features/auth/hooks/useCurrentUser.ts` 이미 존재
  - Context 안에서 `useLearningFacade`도 함께 노출되어 concept 유무를 파생 계산 → ProtectedRoute의 `requireConcept` 판단 근거
- **소셜 provider 확장은 FE 상수 파일 1곳만 수정**
  - `features/auth/social-providers.ts` (신규): `{ slug, label, icon, oauthUrl }[]`
  - LoginPage가 이 배열을 map으로 렌더 → 구글 추가 시 항목 1개 추가만
  - OAuthCallbackPage는 slug만 확인, 나머지 흐름은 백엔드 위임
- **PASSWORD_NOT_MATCHED / USER_NOT_FOUND = 동일 UX (enumeration 방지)**
  - 백엔드가 이미 동일 메시지 응답 정책. FE는 그 정책 준수 — 로그인 실패 시 항상 "아이디 또는 비밀번호가 올바르지 않습니다" 표시
- **USER_LOCKED / USER_IS_SOCIAL = 인라인 에러 (사용자 안내 필요)**
  - 로그인 폼 하단에 명시적 문구 노출. USER_IS_SOCIAL 시 소셜 로그인 CTA로 유도
- **프로필 수정 UI = 본인만. 수정 대상 명시 표시**
  - "현재 계정: {username}"을 폼 헤더에 표시 → username 필드 미제공(백엔드 DTO 정합)
  - nickname / email만 수정 가능
- **계정 삭제 = 2단계 confirm + 백엔드 soft delete 안내**
  - Dialog 1: "정말 삭제하시겠어요?"
  - Dialog 2: username 입력 재확인
  - 성공 시 세션 클리어 → `/` landing 이동 + 토스트 "탈퇴가 완료되었습니다"

## 대안 검토 (Alternatives Considered)

### 소셜 provider 확장 방식

**Option A — LoginPage에 provider별 하드코딩**
- 거부 이유: 신규 provider 추가마다 LoginPage 수정. OCP 위반

**Option B (선택) — social-providers.ts 상수 배열 + map 렌더**
- 비용: 상수 파일 1개 추가
- 보상: 신규 provider = 배열 항목 1개 + icon import만

### 로그인 실패 UX

**Option A — 인라인 vs 토스트 혼용**
- 거부 이유: 사용자 인지 부담. 검증 실패인지 시스템 실패인지 구분 못 함

**Option B (선택) — 사용자 입력 검증 실패는 인라인, 시스템 실패는 토스트**
- 비용: 매핑 표 유지 필요
- 보상: 명확한 축. PASSWORD_NOT_MATCHED/USER_LOCKED/USER_IS_SOCIAL은 인라인(입력 관련), MAINTENANCE/INTERNAL_ERROR는 토스트

### 계정 삭제 재확인

**Option A — 단일 confirm dialog**
- 거부 이유: 실수 클릭으로 계정 손실

**Option B (선택) — 2단계 confirm + username 재입력**
- 비용: 흐름 길어짐
- 보상: 실수 방지, 재활성화 정책과 정합

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 / 라우트 배치

```
[Router]
  /              → LandingPage
  /login         → LoginPage
  /signup        → SignupPage
  /oauth/:provider/callback → OAuthCallbackPage
  /onboarding    → OnboardingPage    (ProtectedRoute, redirectIfConcept)
  /home          → HomePage          (ProtectedRoute + requireConcept)
  /me            → MePage            (ProtectedRoute)

Global:
  <AuthProvider>
    <SessionWatcher />  (session lost 감지 → login redirect)
    <ProtectedRoute />  (routes wrapper)
```

### 핵심 플로우

**1. 자체 로그인 → 홈 or 온보딩**
```
LoginPage.submit(id, pw)
   ├─ POST /login  → 200 + Set-Cookie AT + Body { refreshToken }
   ├─ setMemoryRefreshToken(rt)
   ├─ AuthProvider가 useCurrentUser 캐시 refetch
   └─ concept 유무 판단 → /onboarding or /home 이동
```

**2. 소셜 로그인 콜백**
```
사용자가 provider 승인 → GET /oauth/kakao/callback?code=...&state=...
   │
   ▼
OAuthCallbackPage
   ├─ provider slug 검증 (social-providers.ts에 있는지)
   ├─ POST /social/login/{provider} { code, state }
   ├─ 성공: setMemoryRefreshToken + 세션 진입
   ├─ SOCIAL_PROVIDER_NOT_SUPPORTED (400): "지원하지 않는 provider" + /login
   └─ SOCIAL_MEMBER_ALREADY_LINKED (409): "이미 연동된 계정" 안내
```

**3. 세션 손실 → 로그인 회복**
```
apiClient가 AUTH001/003/004/101-104 감지
   ├─ handleSessionLost() → setMemoryRefreshToken(null)
   ├─ onSessionLost() 훅 → SessionWatcher → navigate('/login', replace)
   └─ 토스트 "세션이 종료되었습니다"
```

### 외부 의존

- **백엔드 `/login`, `/social/login/{provider}`, `/user` (수정/삭제), `/user/me`**: `endpoints/user.ts` 위임
- **`lib/api/client.ts` 인터셉터**: auth 흐름 실체 (product-auth 소유)
- **`features/auth/refreshTokenStore.ts`**: 메모리 토큰 (product-auth 소유)

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | ApiError code | HTTP | 클라이언트 권장 동작 (UX) |
| --- | --- | --- | --- |
| 회원가입 username 중복 | `USER_ALREADY_EXISTS` | 409 | 인라인 "이미 사용 중" |
| 로그인 실패 (미존재/비밀번호 불일치) | `USER_NOT_FOUND` / `PASSWORD_NOT_MATCHED` | 401 | 동일 인라인 문구 (enumeration 방지) |
| 잠긴 계정 로그인 | `USER_LOCKED` | 401 | 인라인 "관리자 문의" |
| 소셜 유저가 자체 로그인 | `USER_IS_SOCIAL` | 401 | 인라인 + 소셜 로그인 CTA |
| 지원하지 않는 provider | `SOCIAL_PROVIDER_NOT_SUPPORTED` | 400 | 토스트 + `/login` replace |
| 동일 socialId 중복 연동 | `SOCIAL_MEMBER_ALREADY_LINKED` | 409 | 안내 dialog |
| 본인/관리자 외 수정·삭제 | `AUTH_FORBIDDEN` | 403 | 토스트 + `/` replace |
| 미인증 요청 | `AUTH_TOKEN_MISSING` | 401 | SessionWatcher 처리 → `/login` |

MSW handler로 8개 시나리오 재현. `msw/handlers/user.ts`.

### 로깅 정책 (FE)

- **항상 기록 (Sentry)**:
  - 로그인/회원가입 성공·실패 이벤트 (code만, PII 없음)
  - SessionWatcher redirect 발생 이벤트
  - 계정 삭제 완료 이벤트
- **debug**: LoginPage 폼 검증 실패 사유
- **절대 금지**:
  - 비밀번호 원문
  - 소셜 access_token / code
  - 사용자 이메일 · nickname 본문 reflection (username은 code에 포함 가능)

### 관측 지표

- LoginPage LCP P95 ≤ 1.5s (Web Vitals)
- INP (로그인 버튼 클릭 → 응답) P95 ≤ 200ms
- 세션 손실 후 재로그인 성공률 ≥ 99%
- PASSWORD_NOT_MATCHED 응답 문구 통일 100%
- 계정 삭제 미확인 클릭 → 실제 삭제 = 0건

## 롤아웃 / 마이그레이션 (Rollout)

### 전제

- 현재 사용자: 로그인 흐름 사용자 (기존 UI 부분 재구성)
- 백엔드 User BC 리팩토링(Product 2) 완료가 선행 조건
- `product-auth.md`의 인터셉터·refresh 흐름이 병렬 진입

### Product 의존성

- 선행: `./product-auth.md` (토큰 라이프사이클·인터셉터)
- 선행: `./product-fe-cdn.md` (도메인/CORS)
- 후행: 모든 인증 필요 Product (media, deck, search, ai-*)

### Epic·Story 의존성 그래프

```
Epic 1 (LoginPage 통합 + provider 상수화)
  Story 1-1 (social-providers.ts) ─► 1-2 (LoginPage refactor) ─► 1-3 (SignupPage 정리)

Epic 2 (OAuthCallbackPage 정합)
  Story 2-1 (provider slug 검증) ─► 2-2 (에러 UX 표준)

Epic 3 (프로필 수정/삭제)
  Story 3-1 (MePage 표시 확장) ─► 3-2 (수정 폼) ─► 3-3 (2단계 삭제)

Epic 4 (세션 손실 회복)
  Story 4-1 (SessionWatcher + terminal code 매핑 검증) ─► 4-2 (Redirect UX)
```

### 환경별 설정 분기

| 항목 | dev (`.env.development`) | prod (`.env.production`) |
| --- | --- | --- |
| `VITE_API_BASE_URL` | http://localhost:8080 | https://api.thirdtool.dev |
| `VITE_KAKAO_OAUTH_URL` | (test app id) | (prod app id) |
| `VITE_NAVER_OAUTH_URL` | (test app id) | (prod app id) |
| MSW | enabled (`msw/handlers/user.ts`) | disabled |
| Sentry | local | enabled |

## 성공 지표 (KPI)

| 지표 | 목표 값 | 측정 |
| --- | --- | --- |
| LoginPage에 provider 하드코딩 | 0건 (상수 파일 참조만) | 코드 grep |
| PASSWORD_NOT_MATCHED / USER_NOT_FOUND 응답 문구 통일 | 100% | 스냅샷 |
| 삭제 미확인 클릭 → 실제 삭제 | 0건 | E2E |
| 세션 손실 후 재로그인 성공률 | ≥ 99% | 텔레메트리 |
| LoginPage LCP | P95 ≤ 1.5s | Web Vitals |

## Scope

**In Scope**:
- `features/auth/` (Login, Signup, OAuthCallback, SessionWatcher, ProtectedRoute, hooks)
- `features/auth/social-providers.ts` (신규)
- `features/me/` (프로필 표시/수정/삭제)
- AuthProvider + useCurrentUser
- MSW handler 8개 시나리오

**Out of Scope**:
- 인증 인프라 자체(인터셉터 refresh 흐름) → `product-auth.md`
- 다중 디바이스 세션 → 백엔드 v2 대응
- 2FA → 백엔드 v2
- 관리자 타 유저 수정 → backlog `product-admin.md`

## 대상 사용자

- **신규 자체 사용자** — 회원가입 후 온보딩 → 홈 진입
- **신규 소셜 사용자** — 카카오/네이버 승인 후 진입
- **기존 사용자 (프로필 관리)** — /me 진입, 수정·탈퇴
- **세션 만료 사용자** — SessionWatcher 감지 → 재로그인 후 원래 경로 복귀

## 연결된 Epic 목록 (진행 순서)

| 순서 | Epic | 제목 | Story 수 | 선행 의존 |
| --- | --- | --- | --- | --- |
| 1 | Epic 1 | LoginPage 통합 + provider 상수화 | 3 | (없음) |
| 2 | Epic 2 | OAuthCallbackPage 정합 | 2 | Epic 1 |
| 3 | Epic 3 | 프로필 수정/삭제 | 3 | Epic 1 |
| 4 | Epic 4 | 세션 손실 회복 | 2 | Epic 1 |

- [ ] Epic 1: LoginPage 통합 + provider 상수화
- [ ] Epic 2: OAuthCallbackPage 정합
- [ ] Epic 3: 프로필 수정/삭제
- [ ] Epic 4: 세션 손실 회복

## 관련 문서

- 백엔드 원본: `workflow/task/pes/workspectrum/sdd/in-progress/Product.md`
- 인접 FE Product: `./product-auth.md` (토큰 라이프사이클), `./product-fe-cdn.md` (도메인/CORS)
- FE-ADR 후보: `FE-USER-001: 소셜 provider 상수화 규칙`, `FE-USER-002: 로그인 실패 UX 축(인라인/토스트)`

## 열린 질문

- **다중 소셜 계정 연동 UI** — 한 UserEntity가 카카오 + 네이버 동시 연동 시 프로필 표시 방식 (v2 백엔드 결정 대기)
- **관리자 타 유저 수정 flow** — backlog `product-admin.md`에서 별도 처리
- **모바일 딥링크 OAuth callback** — 앱 딥링크 처리 시점

---

# [Epic 1] LoginPage 통합 + provider 상수화

## Epic 목표

`features/auth/social-providers.ts` 상수 도입 → LoginPage가 그 배열을 map으로 렌더. 신규 provider 추가 시 배열 항목 1개만으로 확장 가능한 구조 확립.

## 배경

Product의 진입 Epic. 후속 Epic(OAuth callback, 프로필, 세션)이 본 Epic의 provider 상수를 공유.

## 완료 기준

- [ ] Story 1-1, 1-2, 1-3 완료
- [ ] `social-providers.ts` 상수 배열 + map 렌더 확인
- [ ] LoginPage에 provider 하드코딩 = 0건
- [ ] FE-ADR-USER-001 (소셜 provider 상수화) 작성

## [Story 1-1] `social-providers.ts` 상수 도입

### User Story
- As a FE 개발자
- I want 소셜 provider 목록을 상수 파일로 관리하기를
- so that 신규 provider 추가 시 배열 1개 항목만 수정하면 되도록

### 설명
- `features/auth/social-providers.ts` — `{ slug, label, icon, oauthUrl }[]`
- slug: kebab-case ('kakao', 'naver', 'google')
- oauthUrl은 환경변수 참조 (`import.meta.env.VITE_KAKAO_OAUTH_URL`)

### 완료 기준 (AC)
- Given social-providers.ts 정의 / When import / Then 2개 provider(kakao, naver) 배열
- Given 신규 provider 추가 필요 / When 배열 1건 추가 / Then LoginPage 자동 노출
- *(엣지)* Given 알 수 없는 slug 참조 / When OAuthCallbackPage / Then Story 2-1의 검증에서 차단

### 의존성
- 선행: (없음)
- 후행: Story 1-2

## [Story 1-2] LoginPage refactor

### User Story
- As a 사용자
- I want 자체 로그인과 소셜 로그인 진입점을 한 화면에서 보기를
- so that 어느 진입 방식을 선호하든 즉시 접근할 수 있다

### 설명
- `features/auth/LoginPage.tsx`
- 자체 로그인 폼 + `<SocialProviderButtons>` (social-providers.ts map)
- 인라인 에러: PASSWORD_NOT_MATCHED / USER_NOT_FOUND / USER_LOCKED / USER_IS_SOCIAL
- 토스트: MAINTENANCE / 5xx

### 완료 기준 (AC)
- Given valid 로그인 / When 제출 / Then AuthProvider refetch + concept 유무별 라우팅
- Given PASSWORD_NOT_MATCHED / When 응답 / Then 인라인 "아이디 또는 비밀번호가 올바르지 않습니다"
- *(엣지 - USER_IS_SOCIAL)* Given USER_IS_SOCIAL / When 응답 / Then 인라인 + 소셜 CTA highlight

### 의존성
- 선행: Story 1-1
- 후행: Story 1-3

## [Story 1-3] SignupPage 정리

### User Story
- As a 신규 사용자
- I want 회원가입 후 자연스럽게 온보딩으로 이어지기를
- so that 회원가입 이후 무엇을 해야 할지 즉시 인지한다

### 설명
- `features/auth/SignupPage.tsx`
- 회원가입 성공 시 자동 로그인 + `/onboarding` navigate
- USER_ALREADY_EXISTS 인라인

### 완료 기준 (AC)
- Given valid 폼 / When 제출 / Then 회원가입 → 자동 로그인 → `/onboarding`
- Given USER_ALREADY_EXISTS / When 응답 / Then 인라인 "이미 사용 중"
- *(엣지)* Given 회원가입 성공 후 concept 이미 있음 (재가입 시나리오) / Then `/home` navigate

### 의존성
- 선행: Story 1-2
- 후행: (없음)

---

# [Epic 2] OAuthCallbackPage 정합

## Epic 목표

`/oauth/:provider/callback`이 provider slug 검증 후 백엔드 `/social/login/{provider}` 위임. FE는 slug 화이트리스트만 관리.

## 배경

Epic 1의 provider 상수를 소비. FE 코드는 provider별 분기 없이 slug 매개변수로 백엔드 위임.

## 완료 기준

- [ ] Story 2-1, 2-2 완료
- [ ] provider slug 검증 E2E
- [ ] SOCIAL_PROVIDER_NOT_SUPPORTED 응답 UX E2E

## [Story 2-1] provider slug 검증

### User Story
- As a OAuthCallbackPage
- I want URL의 provider slug가 화이트리스트에 있는지 확인하기를
- so that 알 수 없는 provider를 조기 차단할 수 있다

### 설명
- `features/auth/OAuthCallbackPage.tsx`
- URL param `:provider` → social-providers.ts 화이트리스트 확인
- 미포함 시 `/login` replace + 토스트 "지원하지 않는 provider"

### 완료 기준 (AC)
- Given provider="kakao" (화이트리스트 있음) / When 진입 / Then 백엔드 요청 진행
- Given provider="unknown" / When 진입 / Then `/login` replace + 토스트
- *(엣지)* Given code/state 파라미터 부재 / Then `/login` replace

### 의존성
- 선행: Epic 1
- 후행: Story 2-2

## [Story 2-2] 에러 UX 표준

### User Story
- As a 사용자
- I want OAuth 콜백 실패 시 명확한 안내와 회복 경로를
- so that 다시 로그인 시도할 수 있다

### 설명
- SOCIAL_PROVIDER_NOT_SUPPORTED: 토스트 + `/login`
- SOCIAL_MEMBER_ALREADY_LINKED: 안내 dialog "이미 연동된 계정"
- 5xx: 재시도 CTA + Sentry
- 성공: setMemoryRefreshToken + concept 유무별 navigate

### 완료 기준 (AC)
- Given SOCIAL_PROVIDER_NOT_SUPPORTED / When 응답 / Then 토스트 + `/login`
- Given SOCIAL_MEMBER_ALREADY_LINKED / When 응답 / Then dialog + `/login`
- *(엣지 - 5xx)* Given 5xx / When 응답 / Then 인라인 재시도 CTA + Sentry log

### 의존성
- 선행: Story 2-1
- 후행: (없음)

---

# [Epic 3] 프로필 수정/삭제

## Epic 목표

`/me`에서 프로필 표시 + nickname/email 수정 + 2단계 confirm 삭제.

## 배경

계정 라이프사이클 완결. Epic 1/2의 로그인 기반 위에 프로필 관리 진입.

## 완료 기준

- [ ] Story 3-1, 3-2, 3-3 완료
- [ ] 2단계 삭제 E2E (미확인 클릭 → 실제 삭제 = 0건)
- [ ] AUTH_FORBIDDEN 응답 UX 확인

## [Story 3-1] MePage 표시 확장

### User Story
- As a 사용자
- I want /me에서 내 프로필 정보를 한눈에 보기를
- so that 계정 상태를 인지할 수 있다

### 설명
- `features/me/MePage.tsx`
- useCurrentUser로 { username, nickname, email, socialProvider? } 표시
- "현재 계정: {username}" 헤더로 소유권 명시

### 완료 기준 (AC)
- Given 로그인 사용자 / When 진입 / Then 프로필 필드 표시
- Given 소셜 사용자 / When 진입 / Then provider 아이콘 + label 표시
- *(엣지 - 미로그인)* Given 세션 없음 / When 진입 / Then ProtectedRoute가 `/login` redirect

### 의존성
- 선행: Epic 1
- 후행: Story 3-2

## [Story 3-2] 수정 폼

### User Story
- As a 사용자
- I want nickname과 email을 수정하기를
- so that 정보를 최신화할 수 있다

### 설명
- `features/me/components/ProfileEditForm.tsx`
- nickname / email 필드만 수정 가능 (username은 미제공)
- PATCH `/user/me` 위임
- 성공 시 useCurrentUser cache 갱신 + 토스트

### 완료 기준 (AC)
- Given 수정된 nickname / When 제출 / Then PATCH 성공 + 토스트
- Given email 형식 오류 / When 제출 / Then 클라이언트 검증 에러
- *(엣지 - USER_ALREADY_EXISTS on email)* Given 다른 사용자의 email / When 제출 / Then 인라인 "이미 사용 중"

### 의존성
- 선행: Story 3-1
- 후행: Story 3-3

## [Story 3-3] 2단계 삭제

### User Story
- As a 사용자
- I want 계정 삭제 시 재확인 절차를 거치기를
- so that 실수 클릭으로 계정을 잃지 않는다

### 설명
- Dialog 1: "정말 삭제하시겠어요?" [확인/취소]
- Dialog 2: username 입력 재확인 → 정확히 일치해야 [확정]
- DELETE `/user/me` 위임
- 성공 시 세션 클리어 + `/` navigate + 토스트

### 완료 기준 (AC)
- Given Dialog 1 확인 / When 확인 / Then Dialog 2 오픈
- Given Dialog 2 username 미일치 / When 확정 시도 / Then 버튼 disable
- *(엣지 - Dialog 2 취소)* Given Dialog 2 열림 / When 취소 / Then 삭제 안 됨, 페이지 유지

### 의존성
- 선행: Story 3-2
- 후행: (없음)

---

# [Epic 4] 세션 손실 회복

## Epic 목표

SessionWatcher가 terminal error code(AUTH001/003/004/101-104) 감지 시 `/login` replace + 토스트로 재로그인 유도.

## 배경

인터셉터의 handleSessionLost와 협력. product-auth의 인터셉터가 트리거하는 이벤트를 소비.

## 완료 기준

- [ ] Story 4-1, 4-2 완료
- [ ] SessionWatcher E2E (terminal code 발생 → redirect)
- [ ] 재로그인 후 원래 경로 복귀 (returnUrl)

## [Story 4-1] SessionWatcher terminal code 매핑 검증

### User Story
- As a SessionWatcher
- I want product-auth 인터셉터가 알리는 terminal code를 정확히 소비하기를
- so that 세션 손실 시 즉시 반응한다

### 설명
- `features/auth/SessionWatcher.tsx`
- refreshTokenStore 이벤트 리스너
- terminal code 리스트: AUTH001, 003, 004, 101, 102, 103, 104

### 완료 기준 (AC)
- Given terminal code 이벤트 / When 감지 / Then handleSessionLost 호출 + navigate('/login')
- Given non-terminal code / When 감지 / Then 무시 (인터셉터가 refresh 처리)
- *(엣지 - 이미 /login)* Given 사용자가 이미 /login에 있음 / When terminal code / Then redirect 안 함

### 의존성
- 선행: Epic 1
- 후행: Story 4-2

## [Story 4-2] Redirect UX + returnUrl

### User Story
- As a 사용자
- I want 세션 만료로 로그인 후 원래 있던 페이지로 돌아가기를
- so that 흐름이 끊기지 않는다

### 설명
- `/login?returnUrl={encoded}` 파라미터 지원
- 로그인 성공 시 returnUrl로 navigate (동일 origin 검증)
- 없으면 concept 유무에 따라 `/home` or `/onboarding`

### 완료 기준 (AC)
- Given /me에서 세션 만료 / When redirect / Then `/login?returnUrl=/me`
- Given 로그인 성공 / When returnUrl 있음 / Then `/me` navigate
- *(엣지 - 외부 URL)* Given returnUrl에 외부 origin / When 검증 / Then 무시하고 기본 라우팅

### 의존성
- 선행: Story 4-1
- 후행: (없음)
