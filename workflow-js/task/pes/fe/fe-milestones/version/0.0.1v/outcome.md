# FE 0.0.1v / 성과 (Outcome)

> FE M1 종료 시점에 **사용자 경험·UX 자산·기술 자산 측면에서 무엇이 새로 가능해졌는가**의 산출물 기록.
> 인프라 산출물은 `infra.md`, 성능 수치는 `performance.md`, 비용 영향은 `cost.md`. 본 파일은 그 외 "결과" 전반.
> 백엔드 `outcome.md`가 서버 도메인 자산 변화를 다루는 반면, 본 파일은 **화면 접근성 · UX 회복 흐름 · 컴포넌트 자산** 등 FE 진영의 결과를 다룬다.

---

## 본주 머지된 FE Story (실측)

> D6 종료 시 git log 기반 채움.

| # | Product | Story | PR | 머지 commit | 비고 |
| --- | --- | --- | --- | --- | --- |
| 1 | product-fe-cdn | D10 `.env.production` 확정 | #___ | _____ | 사용자 직접 (FE 레포) |
| 2 | product-fe-cdn | GHA 첫 배포 검증 | #___ | _____ | BE D1~D7 완료 후 |
| 3 | product-fe-cdn | CORS + SameSite E2E | #___ | _____ | 로그인 flow 검증 |
| 4 | Product (User) | `username` 필드 grep · 제거 | #___ | _____ | BE Story 5-4 대응 |
| 5 | Product (User) | LoginPage `social-providers.ts` 상수화 | #___ | _____ | 카카오·네이버 |
| 6 | product-auth | MSW 8종 401 handler | #___ | _____ | AUTH001~104 재현 |
| 7 | product-card (done) | 실패 어휘 CI grep | #___ | _____ | `src/features/cards` 정합 |
| 8 | product-learningFacade (done) | CoverageBadge 상수 정착 | #___ | _____ | `features/map/constants.ts` |
| (Want) 9 | product-fe-cdn | Lighthouse baseline 1회 | #___ | _____ | performance.md 기입 |
| (Want) 10 | product-auth | ApiError DevTools 로깅 확장 | #___ | _____ | 401 코드 포함 |
| (Want) 11 | product-learningFacade | dnd-kit FE-ADR 초안 | #___ | _____ | 도입 결정 근거 |

---

## 사용자에게 보이는 변화 (M1 종료 시)

> 프로덕션 도메인 기준. FE M1이 성공하면 처음으로 브라우저에서 도메인 주소로 접속 가능한 상태가 된다.

| 영역 | M1 전 | M1 후 |
| --- | --- | --- |
| 접근 가능 URL | 로컬만 (`http://localhost:5173`) | **`https://thirdtool.dev`** + 로컬 |
| 인증 흐름 | 로컬 dev API 대상 | **프로덕션 `api.thirdtool.dev`로 실제 로그인 가능** |
| 배포 흐름 | 수동 빌드 + 수동 확인 | **GHA `workflow_dispatch` 1회 클릭으로 자동 배포 (< 5분)** |
| 로그인 페이지 provider 확장성 | 코드 하드코딩 | **`social-providers.ts` 상수 배열 + 카카오·네이버 자동 렌더** |
| BE 계약 정합성 | `UserUpdateRequestDTO.username` 참조 잔존 | **`username` 필드 grep 0건** (BE Story 5-4 대응 완료) |
| 401 처리 검증 | dev에서만 부분 확인 | **MSW로 8종 401 코드 각각 재현 가능** (Vitest 단위 검증) |
| Card 문구 정합 | "실패" 어휘 부분 존재 가능 | **CI grep 0건 강제** |
| Coverage 색상 매핑 | 컴포넌트 산개 가능 | **`features/map/constants.ts` 단일 상수 + snapshot** |

---

## 기술 자산 증가

### 산출 코드 (파일 단위)

- 신규: `src/features/auth/social-providers.ts` (provider 상수 배열)
- 신규: `src/features/map/constants.ts` (CoverageStatus → Tailwind class 매핑)
- 신규: `src/mocks/handlers/auth-errors.ts` (MSW 8종 401 handler)
- 수정: `src/features/me/MePage.tsx` (username 참조 제거)
- 수정: `src/lib/api/endpoints/user.ts` (UpdateUserRequest 스키마에서 username 제거)
- 수정: `src/lib/api/schemas/user.ts` (Zod 스키마 정합)
- 수정: `src/lib/api/client.ts` (DevTools 로깅 확장 — Tier 2)

### 산출 인프라 (FE 관점)

- `.env.production` 파일 정착 (커밋됨)
- `.env.development` 파일 (이미 존재 → 확인만)
- GHA workflow `deploy-fe.yml` 첫 성공 실행 이력 (BE 레포)
- CloudFront distribution 활성 (BE spec + 사용자 액션)
- 도메인 라우팅 활성: `thirdtool.dev` → CF, `www.thirdtool.dev` → CF, `api.thirdtool.dev` → ALB

### 산출 문서

- `FE-ADR-CANDIDATES.md`에 P0/P1 항목 추가 상태 갱신
- (Want) FE-ADR 초안 파일 (dnd-kit 도입 근거)

### 신규 FE-ADR 후보

- **FE-ADR-P0-1** — 인증 토큰 보관 (AT HttpOnly Cookie / RT React 메모리) 확정
- **FE-ADR-P1-7** — CloudFront 도메인 + Cache-Control 이중 정책 (BE ADR-CDN-004 정합)
- **FE-ADR-P1-9** — GHA `deploy-fe.yml` OIDC + Cache-Control 분리 sync (BE ADR-CDN-005 정합)
- **FE-ADR-P1-2** (Tier 2) — dnd-kit 도입 결정

> 본 FE-ADR들은 M1 종료 후 실제 문서로 별도 작성 (마일스톤 외 작업).

---

## 도달한 FE Product 상태 변화

| FE Product | M1 시작 | M1 종료 | 다음 마일스톤 |
| --- | --- | --- | --- |
| product-card (done) | done + 잔여 정합 3건 | **done + 정합 완료** | 유지 · 필요시 IMAGE 진입 (product-media) |
| product-learningFacade (done) | done + 정합 2건 | **done + 정합 완료** | dnd-kit 도입 (M2) + AI Suggestion 통합 UI (M2) |
| Product.md (User FE) | in-progress (기본만) | **LoginPage 통합 · username 정합 완료** | MePage 수정/삭제 UI (M2) |
| product-auth | in-progress | **MSW 8종 handler + 인터셉터 검증** | Sentry 도입 검토 (M2 이후) |
| product-fe-cdn | in-progress | **첫 배포 완료 · 도메인 활성** | auto-trigger 전환 (v2) · Lighthouse CI (M2) |
| product-deck | 스켈레톤 | 스켈레톤 유지 (BE 확정 대기) | BE M2 확정 후 첫 UI |
| product-media | in-progress (backlog 신호) | 대기 | BE 우선순위 도래 후 |
| product-search | in-progress | 대기 | BE M2 검색 VO 진입 후 |
| product-aisuggestion | in-progress | 대기 | BE M2 Controller 노출 후 진입 |
| product-ai-interactive-roadmap | in-progress | 대기 | 후속 |
| product-admin (backlog) | backlog | backlog | 우선순위 결정 후 |
| product-notification (backlog) | backlog | backlog | 우선순위 결정 후 |

---

## UX 디자인 성과 (정성)

- [ ] **첫 도메인 접속 경험** — 사용자가 처음으로 `thirdtool.dev`를 브라우저 주소창에 입력하고 앱을 로드하는 경험을 검증
- [ ] **인증 흐름 연속성** — 로그인 → HomePage 도달까지 페이지 깜빡임·에러 노출 0건
- [ ] **소셜 provider 확장 정합** — 카카오·네이버 진입점이 동일 UX 패턴으로 표시. 향후 구글 추가 시 코드 수정 1건
- [ ] **`실패` 어휘 제거 완료** — Card 스터디/아카이브 UX에서 "실패" 어휘 노출 0건. 백엔드 `docs/ux/wip-language.md` 정책과 정합
- [ ] **Coverage 색상 언어 통일** — `NO_MATERIAL`(회색) → `PARTIAL`(주황) → `COVERED`(초록)의 시각 언어가 학습자에게 즉시 인지되는 색상 계층
- [ ] **에러 회복 UX 검증 기반** — MSW 8종으로 인터셉터의 refresh/session-clear 흐름을 시뮬레이션 가능. 실제 사용자가 세션 만료를 겪었을 때 어떤 UX가 나올지 사전 검증됨

---

## 사용자 시나리오 검증 결과

> M1 종료 시점에 프로덕션 환경(`https://thirdtool.dev`)에서 직접 시나리오 통과 시도.
> 상세 체크리스트: [`ux-test.md`](./ux-test.md)

| # | 시나리오 | 통과 여부 | 실패 원인 (있을 시) |
| --- | --- | --- | --- |
| 01 | 온보딩: `/` → `/login` → `/signup` → onboarding → `/home` | `_____` | `_____` |
| 02 | 로그인 → `/home` 표시 | `_____` | `_____` |
| 03 | 카카오 소셜 로그인 → `/oauth/kakao/callback` → `/home` | `_____` | `_____` |
| 04 | 세션 만료 (AT 만료 시뮬레이션) → refresh → 원 요청 재시도 | `_____` | `_____` |
| 05 | 프로필 페이지(`/me`) 진입 → nickname/email 표시 | `_____` | `_____` |
| 06 | 카드 스터디 진입(`/study`) → 카드 1장 노출 | `_____` | `_____` |
| 07 | LearningFacade 축 목록 표시(`/map`) → CoverageBadge 색상 정합 | `_____` | `_____` |
| 08 | 404(`/nonexistent`) → NotFoundPage 표시 | `_____` | `_____` |
| 09 | 503(`/maintenance`) 강제 표시 → MaintenancePage 로드 | `_____` | `_____` |

→ 통과 / 미통과 결과는 [`review.md`](./review.md)에 요약 기록

---

## 접근성(a11y) 관찰 (선택 · Tier 2)

Lighthouse Accessibility 점수 + 수동 관찰.

| 항목 | M1 관찰 |
| --- | --- |
| 색상 대비 (Coverage 색상 3종) | `_____` |
| 키보드 네비게이션 (Tab 순서) | `_____` |
| ARIA 레이블 (로그인 폼) | `_____` |
| focus-visible 표시 | `_____` |
| 스크린리더 안내 (제한적 검증) | `_____` |

→ M2 이후 접근성 개선 백로그 축적.

---

## FE-BE 정합성 확인 결과

M1 종료 시 실제 프로덕션 환경에서 확인.

| 확인 항목 | 결과 | 비고 |
| --- | --- | --- |
| Zod 스키마 파싱 실패 발생 | `_____ 건` | Sentry 없으므로 브라우저 콘솔에서 관찰 |
| BE 응답 시 ErrorCode drift (미매핑 코드) | `_____ 건` | `workflows/backend-boundary/error-codes.md` 대조 |
| `X-Request-Id` 헤더 왕복 | `_____ %` | 요청 대비 응답 확인 |
| CORS 프리플라이트 성공률 | `_____ %` | DevTools |
| SameSite=Strict 쿠키 전달 | `_____ %` | DevTools Cookies |

---

## 다음 마일스톤(M2)로 이월된 UX 후보

- MePage 수정/삭제 UI (`Product.md` Epic 3)
- 2단계 confirm dialog 컴포넌트 표준화
- USER_IS_SOCIAL 인라인 안내 + 소셜 CTA
- LoginPage 에러 UX 정합 (사용자 입력 검증 실패 = 인라인, 시스템 실패 = 토스트)
- SessionWatcher 세션 손실 UX (`/login` replace + 토스트)
- AI Suggestion Dialog 착수 (BE Controller 노출 후)
- dnd-kit 실제 도입 (`product-learningFacade.md` Epic 2)
- Lighthouse Performance 개선 첫 사이클 (M1 baseline 기반)
- 접근성 개선 백로그 (a11y 관찰 결과 반영)
