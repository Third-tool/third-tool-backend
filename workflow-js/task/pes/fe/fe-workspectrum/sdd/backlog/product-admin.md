# [Product] Admin FE — ADMIN role 전용 경량 운영 UI

## Product Vision

> 백엔드가 `/api/v1/admin/**` 경량 endpoint 묶음을 노출할 때, ADMIN role 사용자에게만 운영 화면을 표시한다. 별도 admin SPA를 만들지 않고 본 FE 앱 안에 `/admin/**` 라우트 그룹으로 격리. 라우트 가드 + IP 화이트리스트(백엔드 ALB)로 3중 방어의 클라이언트 층을 담당.

## 배경 및 문제

- **현재 상황 (As-Is)**
  - User role enum이 `USER` 단일 → FE에 ADMIN 개념 없음
  - `/admin/**` 라우트·페이지 없음
- **발생하는 문제**
  - 운영자가 계정 정지·role 변경·AI 한도 조정을 위해 DB 직접 접속에 의존
  - 감사 로그 없이 상태 변경 → 사후 추적 불가
- **왜 지금 해결해야 하는가 (backlog)**
  - 백엔드 User BC 안정 + `product-aisuggestion.md` Epic 3 활성 직전에 프롬프트 hot-swap·AI 한도 API가 필요 → 그 시점에 FE도 진입

## 목표 (To-Be)

- ADMIN role 사용자만 `/admin/**` 진입 가능. 그 외 403 처리
- 계정 정지/재활성/role 변경/강제 로그아웃 UI
- AI 토큰 한도 조정 UI
- 프롬프트 버전 hot-swap UI
- 모든 admin 액션에 감사 로그가 백엔드에 남음(사실 확인은 백엔드 책임, FE는 요청 발행만)
- 각 변경 액션은 confirm dialog + before/after 표시 (실수 방지)

## 설계 결정 (Design Decisions)

- **`/admin/**` 라우트 그룹 = ProtectedRoute 확장 (`requireRole: 'ADMIN'`)**
  - 기존 `ProtectedRoute`에 role 검사 옵션 추가
  - USER가 접근 시 `/home` replace + 토스트
- **AdminLayout = 별도 레이아웃 (App shell과 시각적 구분)**
  - 상단에 "관리자 모드" 배지 (실수 방지)
  - 사이드 nav: Users / AI / System
- **각 변경 액션 = 2단계 confirm**
  - "정말 정지하시겠어요?" → 대상 username 재확인 → 실행
  - before/after 값 표시
- **멱등성 UX = 이미 같은 상태면 조용히 성공**
  - 백엔드 정합 (같은 상태로 요청 시 no-op + 200)
  - FE도 오류 처리 안 함
- **감사 로그는 사용자에게 노출 안 함**
  - 백엔드가 CloudWatch에 별도 로깅. FE는 요청만 발행
- **IP 화이트리스트 미통과 시 애플리케이션 도달 전 ALB 403**
  - FE는 401/403 인터셉터가 이미 처리 → 별도 UX 필요 없음

## 대안 검토 (Alternatives Considered)

### 배포 토폴로지

**Option A (선택) — 본 FE 앱 안에 `/admin/**` 격리**
- 비용: 라우트 가드·레이아웃 분리 유지
- 보상: 별도 SPA 유지 불필요. FE 스택·인프라 재사용

**Option B — 별도 admin FE SPA**
- 거부 이유: 1인 운영 단계 비용 정당화 불가

### role 검사 위치

**Option A — 각 admin 페이지에서 직접 검사**
- 거부 이유: 중복·누락

**Option B (선택) — ProtectedRoute `requireRole` 옵션 확장**
- 비용: ProtectedRoute 시그니처 확장
- 보상: 단일 지점

### 변경 액션 confirm

**Option A — 단일 confirm**
- 거부 이유: 실수 클릭

**Option B (선택) — 2단계 confirm + before/after**
- 비용: 흐름 길어짐
- 보상: 실수 방지, 감사 로그와 정합

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 / 라우트 배치

```
[Router — /admin/** ProtectedRoute + requireRole=ADMIN]
  /admin/users          → AdminUsersPage
  /admin/users/:userId  → AdminUserDetailPage
  /admin/ai/limits      → AdminAiLimitsPage
  /admin/ai/prompts     → AdminPromptsPage
  /admin/system         → AdminSystemPage

features/admin/
├─ AdminLayout.tsx          (관리자 모드 배지 + 사이드 nav)
├─ AdminUsersPage.tsx       (목록 + 검색 + 필터)
├─ AdminUserDetailPage.tsx  (정지/재활성/role 변경/강제 로그아웃)
├─ AdminAiLimitsPage.tsx    (사용자별 AI 한도 조정)
├─ AdminPromptsPage.tsx     (프롬프트 버전 hot-swap)
├─ AdminSystemPage.tsx      (metrics 링크 등)
└─ components/
     ├─ AdminActionDialog.tsx  (2단계 confirm + before/after)
     ├─ AuditIndicator.tsx     (백엔드에 감사 로그 남음 안내)
     └─ RoleBadge.tsx

features/auth/components/ProtectedRoute.tsx
  └─ requireRole 옵션 확장

lib/api/schemas/admin.ts
lib/api/endpoints/admin.ts
```

### 핵심 플로우

**1. 관리자 진입**
```
사용자가 /admin/users 이동
   │
   ▼
ProtectedRoute (requireRole=ADMIN)
   ├─ useCurrentUser → user.role === 'ADMIN' 확인
   ├─ true → AdminLayout + AdminUsersPage 렌더
   └─ false → navigate('/home', replace) + 토스트 "권한 없음"
```

**2. 계정 정지**
```
사용자가 /admin/users/{userId}에서 "정지" 클릭
   │
   ▼
AdminActionDialog(1단계)
   "정말 정지하시겠어요? 대상: {targetUsername}"
   [계속] [취소]
   │
   ▼
AdminActionDialog(2단계)
   "확인을 위해 {targetUsername}을 입력하세요"
   [실행] disabled until match
   │
   ▼
POST /api/v1/admin/users/{userId}/suspend
   ├─ 성공: 토스트 "정지되었습니다" + 상세 페이지 갱신
   ├─ 이미 정지: (멱등) 조용히 성공
   └─ 403: 인터셉터 처리
```

**3. AI 한도 조정**
```
AdminAiLimitsPage.PATCH /admin/ai/limits/{userId}
   body: { dailyTokenLimit: 10000 }
   ├─ before/after 표시
   ├─ 성공 토스트
   └─ AuditIndicator "이 액션은 감사 로그에 기록됩니다"
```

### 외부 의존

- **백엔드 `/api/v1/admin/**`**: `endpoints/admin.ts` 위임
- **ALB IP 화이트리스트**: 백엔드 인프라 (FE는 3중 방어의 클라이언트 층만 담당)
- **CloudWatch Logs 감사**: 백엔드가 별도 기록
- **product-auth**: 401/403 인터셉터 재사용

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | ApiError code | HTTP | 클라이언트 권장 동작 (UX) |
| --- | --- | --- | --- |
| USER role이 /admin 접근 | (사전 차단) | - | `/home` replace + 토스트 "권한 없음" |
| IP 화이트리스트 미통과 | ALB 응답 | 403 | 인터셉터 처리 → "네트워크에서 접근할 수 없습니다" |
| 대상 유저 미존재 | `USER_NOT_FOUND` | 404 | 목록 새로고침 |
| 프롬프트 버전 미존재 | `PROMPT_VERSION_NOT_FOUND` | 400 | 인라인 에러 |
| 이미 같은 상태 (멱등) | (no-op) | 200 | 조용히 성공, 오류 처리 없음 |
| Confirm dialog 우회 시도 | (FE 사전 차단) | - | 버튼 disabled 유지 |
| AI 한도 값 범위 초과 | `AI_LIMIT_OUT_OF_RANGE` | 400 | 인라인 에러 |

MSW handler로 5개 시나리오 재현. `msw/handlers/admin.ts`.

### 로깅 정책 (FE)

- **항상 기록 (Sentry)**:
  - USER role의 /admin 진입 시도 이벤트 (보안 시그널)
  - AdminActionDialog 우회 시도 (DOM 조작 감지 시)
  - admin action 실패 code + targetUserId (개인정보 아님)
- **debug**: AdminActionDialog 1단계·2단계 진입 이벤트
- **절대 금지**:
  - 대상 유저의 email · nickname 본문 (username은 OK)
  - AI 한도 값 (민감 운영 정보이나, requestId로 감사 로그와 correlate 가능)
  - 프롬프트 버전 본문

### 관측 지표

- USER role의 /admin 실 진입 = 0건 / 주
- 2단계 confirm 없이 destructive 액션 실행 = 0건
- 멱등 재요청 오류 노출 = 0건
- 관리자 액션 후 백엔드 감사 로그 미발생 = 0건 (백엔드 책임, FE E2E로 존재만 확인)
- AdminLayout LCP P95 ≤ 1.2s (Web Vitals)

## 롤아웃 / 마이그레이션 (Rollout)

### 전제

- 현재 사용자: ADMIN role 사용자 부재 (첫 도입)
- 백엔드 `/api/v1/admin/**` API 확정 완료가 선행 조건
- ALB IP 화이트리스트 구성 완료
- User role enum에 ADMIN 추가 완료

### Product 의존성

- 선행: `../in-progress/Product.md` (User FE — role 필드 인지)
- 선행: `../in-progress/product-auth.md` (403 인터셉터 재사용)
- 선행: `../in-progress/product-aisuggestion.md` Epic 3 (AI 한도/프롬프트 API 소비자)
- 후행: (없음, 운영 UI는 상위 프로덕트 없음)

### Epic·Story 의존성 그래프

```
Epic 1 (ProtectedRoute requireRole 확장)
  Story 1-1 (role 검사 옵션) ─► 1-2 (AdminLayout)

Epic 2 (User 관리 UI)
  Story 2-1 (AdminUsersPage 목록) ─► 2-2 (AdminUserDetailPage)
                                     ─► 2-3 (정지/재활성) ─► 2-4 (role 변경 + 강제 로그아웃)

Epic 3 (AI 한도/프롬프트 hot-swap)
  Story 3-1 (AdminAiLimitsPage) ─► 3-2 (AdminPromptsPage)

Epic 4 (감사 표시 + confirm 정합)
  Story 4-1 (AdminActionDialog 2단계) ─► 4-2 (AuditIndicator)
```

### 환경별 설정 분기

| 항목 | dev (`.env.development`) | prod (`.env.production`) |
| --- | --- | --- |
| `VITE_API_BASE_URL` | http://localhost:8080 | https://api.thirdtool.dev |
| MSW | enabled (`msw/handlers/admin.ts`) | disabled |
| Sentry | local | enabled |
| ADMIN role 시뮬레이션 | `?forceAdmin=true` (dev만) | disabled |

## 성공 지표 (KPI)

| 지표 | 목표 |
| --- | --- |
| USER role의 /admin 실 진입 | 0건 |
| 2단계 confirm 없이 destructive 액션 실행 | 0건 |
| 멱등 재요청 오류 노출 | 0건 |
| 관리자 액션 후 백엔드 감사 로그 미발생 | 0건 (백엔드 책임) |
| AdminLayout LCP | P95 ≤ 1.2s |

## Scope

**In Scope (backlog)**:
- `features/admin/` 전체 신규
- `features/auth/components/ProtectedRoute` requireRole 옵션 확장
- 5개 라우트 (`/admin/users`, `.../:userId`, `/admin/ai/limits`, `/admin/ai/prompts`, `/admin/system`)
- Zod 스키마 / endpoint 모듈
- MSW handler 5개 시나리오

**Out of Scope**:
- 별도 admin SPA
- VPN 기반 접근 → 백엔드 v2
- 대시보드 그래프(Grafana embed) → v2
- SUPER_ADMIN role 계층 → 백엔드 v2

## 대상 사용자

- **운영자 (ADMIN role)** — 계정 정지·재활성·role 변경·AI 한도 조정을 UI로 수행
- **보안 담당** — 감사 로그와 correlate 가능한 requestId 확인
- **일반 사용자 (USER role)** — /admin 진입 시도 시 즉시 차단됨을 인지

## 연결된 Epic 목록 (진행 순서)

| 순서 | Epic | 제목 | Story 수 | 선행 의존 |
| --- | --- | --- | --- | --- |
| 1 | Epic 1 | ProtectedRoute requireRole 확장 | 2 | (없음) |
| 2 | Epic 2 | User 관리 UI | 4 | Epic 1 |
| 3 | Epic 3 | AI 한도/프롬프트 hot-swap | 2 | Epic 1 |
| 4 | Epic 4 | 감사 표시 + confirm 정합 | 2 | Epic 2, 3 |

- [ ] Epic 1: ProtectedRoute requireRole 확장
- [ ] Epic 2: User 관리 UI
- [ ] Epic 3: AI 한도/프롬프트 hot-swap
- [ ] Epic 4: 감사 표시 + confirm 정합

## 관련 문서

- 백엔드 원본: `workflow/task/pes/workspectrum/sdd/backlog/product-admin.md`
- 백엔드 ADR: ADR-ADMIN-001~003
- 인접 FE: `../in-progress/Product.md`, `../in-progress/product-auth.md`, `../in-progress/product-aisuggestion.md`
- FE-ADR 후보: `FE-ADMIN-001: ProtectedRoute requireRole 확장 규칙`, `FE-ADMIN-002: 2단계 confirm UX 정합`

## 열린 질문

- **SUPER_ADMIN role 도입 시** — 백엔드 v2. FE도 role 계층 확장 준비
- **모바일 admin 지원** — 관리자가 모바일에서 정지 액션 실행할 수 있는가 (실수 위험 vs 편의)
- **관리자 액션 undo** — 정지 해제는 별도 요청, 진짜 undo(예: role 자동 롤백)는 불필요

---

# [Epic 1] ProtectedRoute requireRole 확장

## Epic 목표

기존 ProtectedRoute에 `requireRole: 'ADMIN'` 옵션을 추가하고, AdminLayout(관리자 모드 배지 + 사이드 nav)을 확립. `/admin/**` 라우트 그룹의 클라이언트 층 방어망 완성.

## 배경

Product의 진입 Epic. 후속 Epic(User 관리, AI 한도, 감사)은 본 Epic이 확립한 라우트 가드 위에서 동작.

## 완료 기준

- [ ] Story 1-1, 1-2 완료
- [ ] USER role의 `/admin` 접근 시 즉시 replace 확인 (E2E)
- [ ] AdminLayout에 "관리자 모드" 배지 항상 표시
- [ ] FE-ADR-ADMIN-001 (ProtectedRoute requireRole 규칙) 작성

## [Story 1-1] role 검사 옵션

### User Story
- As a FE 개발자
- I want ProtectedRoute에 `requireRole` prop을 추가해 특정 role만 통과시키기를
- so that admin 페이지마다 개별 검사 코드가 필요 없다

### 설명
- `features/auth/components/ProtectedRoute.tsx` 시그니처 확장
- `requireRole?: 'ADMIN' | 'USER'` (default: undefined → role 검사 안 함)
- 검사 실패 시 `/home` replace + 토스트

### 완료 기준 (AC)
- Given USER role + requireRole=ADMIN / When 렌더 / Then `/home` replace
- Given ADMIN role + requireRole=ADMIN / When 렌더 / Then 자식 렌더
- *(엣지 - 미로그인)* Given 세션 없음 / When 렌더 / Then `/login` replace (기존 동작)

### 의존성
- 선행: (없음)
- 후행: Story 1-2

## [Story 1-2] AdminLayout

### User Story
- As a 관리자
- I want 관리자 모드에서 App shell과 시각적으로 구분된 레이아웃과 배지를 보기를
- so that 실수 클릭으로 사용자 데이터를 변경할 위험을 인지한다

### 설명
- `features/admin/AdminLayout.tsx`
- 상단 "관리자 모드" 배지 (붉은 배경)
- 사이드 nav: Users / AI / System
- 하단에 로그아웃 CTA

### 완료 기준 (AC)
- Given /admin 진입 / When 렌더 / Then AdminLayout + 배지 표시
- Given 사이드 nav "Users" 클릭 / When 클릭 / Then `/admin/users`
- *(엣지 - 접근성)* Given 스크린리더 / When 진입 / Then "관리자 모드" 즉시 announced

### 의존성
- 선행: Story 1-1
- 후행: Epic 2, 3

---

# [Epic 2] User 관리 UI

## Epic 목표

AdminUsersPage(목록·검색·필터) + AdminUserDetailPage(정지·재활성·role 변경·강제 로그아웃). 모든 destructive 액션은 2단계 confirm.

## 배경

Product의 핵심 운영 UX. Epic 1의 라우트 가드 위에서 실제 관리 기능 제공.

## 완료 기준

- [ ] Story 2-1, 2-2, 2-3, 2-4 완료
- [ ] 정지/재활성 E2E
- [ ] role 변경 E2E
- [ ] 강제 로그아웃 후 대상 사용자 세션 무효화 확인

## [Story 2-1] AdminUsersPage 목록

### User Story
- As a 관리자
- I want 전체 사용자를 목록으로 보고 검색·필터할 수 있기를
- so that 대상 유저를 빠르게 찾을 수 있다

### 설명
- `features/admin/AdminUsersPage.tsx`
- `GET /api/v1/admin/users?query=&role=&status=`
- 페이지네이션 + 검색 + role/status 필터

### 완료 기준 (AC)
- Given ADMIN 진입 / When 렌더 / Then 사용자 100건 페이지
- Given 검색어 입력 / When query / Then 필터 결과 갱신
- *(엣지 - 결과 0건)* Given 검색 결과 없음 / When 렌더 / Then "결과 없음" 힌트

### 의존성
- 선행: Epic 1
- 후행: Story 2-2

## [Story 2-2] AdminUserDetailPage

### User Story
- As a 관리자
- I want 특정 사용자 상세 페이지에서 정보와 상태를 보기를
- so that 액션을 취하기 전 컨텍스트를 파악한다

### 설명
- `features/admin/AdminUserDetailPage.tsx`
- `GET /api/v1/admin/users/{userId}`
- 표시: username, email, role, status, createdAt, lastLoginAt

### 완료 기준 (AC)
- Given userId / When 진입 / Then 유저 정보 표시
- Given 404 / When 진입 / Then 목록으로 replace + 토스트
- *(엣지 - 자기 자신)* Given ADMIN이 자신의 상세 / Then 액션 버튼 disabled (self-modify 방지)

### 의존성
- 선행: Story 2-1
- 후행: Story 2-3

## [Story 2-3] 정지/재활성

### User Story
- As a 관리자
- I want 사용자를 정지·재활성 시킬 수 있기를
- so that 부정 사용에 대응할 수 있다

### 설명
- `POST /api/v1/admin/users/{userId}/suspend` / `.../reactivate`
- AdminActionDialog 2단계 confirm
- 성공 후 상세 페이지 status 배지 갱신

### 완료 기준 (AC)
- Given 정지 액션 → 2단계 confirm 통과 / When 실행 / Then status=SUSPENDED
- Given 이미 정지된 유저 재정지 시도 / When 실행 / Then 멱등 200, 조용히 성공
- *(엣지 - 자기 자신)* Given 자신의 정지 시도 / When 클릭 / Then 버튼 disabled

### 의존성
- 선행: Story 2-2
- 후행: Story 2-4

## [Story 2-4] role 변경 + 강제 로그아웃

### User Story
- As a 관리자
- I want 사용자 role을 변경하고 강제 로그아웃할 수 있기를
- so that 권한 오용에 즉각 대응한다

### 설명
- `PATCH /admin/users/{userId}/role { role }`
- `POST /admin/users/{userId}/force-logout`
- 강제 로그아웃 시 대상 세션 즉시 무효화

### 완료 기준 (AC)
- Given role 변경 / When 저장 / Then 백엔드 role 반영 + 감사 로그
- Given 강제 로그아웃 / When 실행 / Then 대상 유저 다음 요청부터 AUTH104
- *(엣지)* Given SUPER_ADMIN 삭제 시도 (v2) / Then 백엔드 403 → 인라인

### 의존성
- 선행: Story 2-3
- 후행: (없음)

---

# [Epic 3] AI 한도/프롬프트 hot-swap

## Epic 목표

사용자별 AI 토큰 한도 조정 + 프롬프트 버전 hot-swap. product-aisuggestion Epic 3와 협력.

## 배경

AI 비용 관리·품질 개선의 운영 조작. Epic 1 진입 후 독립적으로 진행 가능.

## 완료 기준

- [ ] Story 3-1, 3-2 완료
- [ ] AI 한도 조정 E2E
- [ ] 프롬프트 hot-swap E2E (변경 즉시 반영)

## [Story 3-1] AdminAiLimitsPage

### User Story
- As a 관리자
- I want 사용자별 AI 일일 토큰 한도를 조정하기를
- so that 비용 폭증을 사전 차단한다

### 설명
- `features/admin/AdminAiLimitsPage.tsx`
- `PATCH /admin/ai/limits/{userId} { dailyTokenLimit }`
- before/after 표시 + AdminActionDialog 2단계

### 완료 기준 (AC)
- Given 새 값 / When 저장 / Then before/after 표시 + 저장 성공
- Given 값 범위 초과 / When 저장 / Then 인라인 에러
- *(엣지 - 여러 유저 동시)* Given 다른 유저 값도 조정 / When 순차 / Then 각각 감사 로그

### 의존성
- 선행: Epic 1
- 후행: Story 3-2

## [Story 3-2] AdminPromptsPage

### User Story
- As a 관리자
- I want 프롬프트 버전을 hot-swap 하기를
- so that AI 응답 품질을 실시간 개선할 수 있다

### 설명
- `features/admin/AdminPromptsPage.tsx`
- `GET /admin/ai/prompts` — 버전 목록
- `POST /admin/ai/prompts/activate { versionId }` — hot-swap
- 활성 버전 강조 표시

### 완료 기준 (AC)
- Given 새 버전 선택 → confirm / When activate / Then 활성 표시 갱신
- Given 존재하지 않는 versionId / When activate / Then 400 인라인
- *(엣지 - 동일 버전 재활성)* Given 이미 활성 / When activate / Then 멱등 200

### 의존성
- 선행: Story 3-1
- 후행: (없음)

---

# [Epic 4] 감사 표시 + confirm 정합

## Epic 목표

AdminActionDialog 2단계 confirm 컴포넌트 표준화 + AuditIndicator로 감사 로그 발생 사실 안내.

## 배경

Product 전체의 UX 일관성. Epic 2, 3의 destructive 액션이 모두 소비.

## 완료 기준

- [ ] Story 4-1, 4-2 완료
- [ ] 모든 destructive 액션이 AdminActionDialog 사용 확인 (grep)
- [ ] FE-ADR-ADMIN-002 (2단계 confirm UX 정합) 작성

## [Story 4-1] AdminActionDialog 2단계

### User Story
- As a 관리자
- I want destructive 액션 실행 시 2단계 confirm과 대상 이름 재입력을 요구받기를
- so that 실수 클릭으로 사고를 내지 않는다

### 설명
- `features/admin/components/AdminActionDialog.tsx`
- Dialog 1: "정말 {action}하시겠어요? 대상: {targetUsername}"
- Dialog 2: `{targetUsername}` 재입력 → 정확히 일치해야 [실행] 활성
- before/after 값 표시 옵션

### 완료 기준 (AC)
- Given Dialog 1 확인 / When 확인 / Then Dialog 2 오픈
- Given Dialog 2 username 미일치 / When 입력 / Then [실행] disabled
- *(엣지 - Dialog 2 취소)* Given Dialog 2 열림 / When 취소 / Then 액션 안 실행

### 의존성
- 선행: Epic 2, 3
- 후행: Story 4-2

## [Story 4-2] AuditIndicator

### User Story
- As a 관리자
- I want 각 admin 액션이 감사 로그에 남는다는 사실을 안내받기를
- so that 액션의 무거움을 인지한다

### 설명
- `features/admin/components/AuditIndicator.tsx`
- 각 액션 페이지 하단에 "이 액션은 감사 로그에 기록됩니다" 배지
- Sentry 클릭 시 backend audit log URL로 이동 (v2)

### 완료 기준 (AC)
- Given admin 페이지 / When 렌더 / Then AuditIndicator 표시
- Given 배지 클릭 (v2) / When 클릭 / Then 감사 로그 URL 이동
- *(엣지 - 접근성)* Given 스크린리더 / When 진입 / Then 배지 즉시 announced

### 의존성
- 선행: Story 4-1
- 후행: (없음)
