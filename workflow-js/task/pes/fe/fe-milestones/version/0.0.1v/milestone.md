# FE M1 / 0.0.1v — Week of 2026-06-23 ~ 2026-06-28

> **마일스톤의 역할**: FE 큰 작업 단위(Product)는 `workflow/task/pes/fe/fe-workspectrum/`에 정의되어 굴러가고 있다. 본 문서는 **이번 주에 그 Product들에서 얼마만큼을 잡아 갈지의 분배 결정** + **버전 단위 산출물 묶음**.
>
> 한 주 = 한 버전 = `version/0.0.X v/` 폴더 하나. 본 버전(0.0.1v)에는 다음 7 파일이 들어간다:
> - `milestone.md` *(본 문서)* — 잡힌 양 + 일정 + 의존
> - [`infra.md`](./infra.md) — FE 배포 인프라 (CloudFront/S3/GHA/env/도메인)
> - [`performance.md`](./performance.md) — Web Vitals · Lighthouse · 번들 baseline
> - [`outcome.md`](./outcome.md) — 사용자 경험·기능·기술 자산 변화
> - [`cost.md`](./cost.md) — FE 관점 비용 성과
> - [`review.md`](./review.md) — 회고 + 다음 버전 진입 신호
> - [`ux-test.md`](./ux-test.md) — 브라우저 UX 시나리오 검증

---

## 백엔드 M1 (0.0.1v) 참조

FE M1은 백엔드 M1과 동일 주간이며, 다음 백엔드 산출물이 FE의 이번 주 진입점이다.

**BE M1 완료 항목 (FE 관점)** — 참조: `workflow/task/milestones/version/0.0.1v/milestone.md` "Tier 1-확장 · 1차 배포 완성"

| BE 항목 | 상태 | FE 진입점 |
| --- | --- | --- |
| D8 BE CORS + OAuth URI → `thirdtool.dev` | ✅ 코드 머지 | FE `.env.production`을 `api.thirdtool.dev`로 확정 |
| D9 GHA FE 배포 워크플로우 (`.github/workflows/deploy-fe.yml`) | ✅ 코드 머지 (BE 레포) | FE 레포에도 이관 또는 정합 확인 |
| D2 ACM 인증서 spec 2개 (ap-northeast-2 + us-east-1) | ✅ spec 머지 | 사용자 액션 후 CF 활성 대기 |
| D3 ALB HTTPS 전환 spec | ✅ spec 머지 | BE 도메인 완성 후 CORS·쿠키 검증 |
| D4 Route53 `api.thirdtool.dev` → ALB spec | ✅ spec 머지 | FE fetch base URL 확정 |
| D5 S3 FE 버킷 spec | ✅ spec 머지 | S3 sync 대상 확정 |
| D6 CloudFront 배포 spec | ✅ spec 머지 | FE 도메인 진입 완성 |
| D7 Route53 `thirdtool.dev`/www → CF spec | ✅ spec 머지 | FE 도메인 활성 |
| D1 도메인 등록 (Route53) | ⏳ 사용자 직접 | 등록 완료 시점에 FE 배포 진입 |
| D10 FE `.env.production` 갱신 | ⏳ 사용자 직접 (FE 레포) | **FE M1 첫 액션** |

**BE M1 코드 머지 중 FE 계약 영향** — 참조: BE Story 5-4

| BE Story | 영향 | FE 대응 |
| --- | --- | --- |
| 2 5-4 `UserUpdateRequestDTO` `username` 필드 제거 | FE의 `PUT /user` 요청 payload에 `username` 있으면 무시됨(혹은 400 가능) | `features/me/` 및 `lib/api/endpoints/user.ts`에서 `username` 필드 grep + 제거 |
| 3 2-1 · 2-2 AI Suggestion Static 응답 활성 (Controller 미노출) | FE 통합 미가능 (M2 대기) | 대응 없음. Zod 스키마·MSW handler 준비만 |

---

## 진행 중 FE Product 잔여 인벤토리 (M1 시점)

FE Product는 `fe-workspectrum/sdd/`에 정착. `done/`은 완료 상태로 이관됨(백엔드 done 상태와 동기).

| FE Product | 상태 | 잔여 UX 작업 | M1 대상 |
| --- | --- | --- | --- |
| product-card (`done/`) | done | 문구 grep · Zod 정합 재검증 | **작은 정합 작업 3건** |
| product-learningFacade (`done/`) | done | Zod 정합 재검증 · dnd-kit 도입 결정 | **작은 정합 작업 2건** |
| Product.md (User FE, `in-progress/`) | in-progress | LoginPage 통합 · MePage 수정/삭제 · 소셜 provider 상수화 | **2 Story** (LoginPage 통합, MePage username 참조 제거) |
| product-auth (`in-progress/`) | in-progress | CORS 프로덕션 정합성 검증 · MSW 8종 401 코드 재현 | **1 Story** (CORS+SameSite 검증 준비) |
| product-fe-cdn (`in-progress/`) | in-progress | `.env.production` · GHA workflow · 첫 배포 검증 | **3 Story** (D10, GHA 첫 실행, E2E) |
| product-deck (스켈레톤) | 스켈레톤 | BE 확정 대기 | — (BE M2에 확정) |
| product-media/search/aisuggestion/roadmap | in-progress/backlog | 각각 BE 진입 대기 | — |
| product-admin/notification | backlog | BE 진입 대기 | — |

**M1 FE 집중 = product-fe-cdn · Product(User) · product-auth · done 정합 grep**

---

## 본주 잡힌 양 (FE M1 — 11 Story)

### Tier 1 · Must (M1 합격선 — 8 Story)

| # | Product | Story | 한 줄 | SP |
| --- | --- | --- | --- | --- |
| 1 | product-fe-cdn | **D10** `.env.production` 확정 | `VITE_API_BASE_URL=https://api.thirdtool.dev` + `.env.development=http://localhost:8080` 분기. 로컬 `.env.local` 미포함 | 1 |
| 2 | product-fe-cdn | **CF 활성 후 첫 배포 검증** | BE D1·D2·D3·D5·D6·D7 완료 대기 → GHA workflow 수동 트리거(`workflow_dispatch`) → CF invalidation → 브라우저 검증 | 2 |
| 3 | product-fe-cdn | **CORS + SameSite=Strict E2E 검증** | `https://thirdtool.dev`에서 `https://api.thirdtool.dev/login` 호출 시 쿠키 정상 전달 + 응답 200 | 2 |
| 4 | Product (User) | **`username` 필드 grep 및 제거** | BE Story 5-4 대응. `features/me/`, `lib/api/endpoints/user.ts`, MSW handler에서 `UserUpdateRequestDTO.username` 관련 참조 0건 확인 | 1 |
| 5 | Product (User) | **LoginPage provider 상수화** (`social-providers.ts`) | 소셜 provider 확장 시 코드 수정 최소화. 카카오 + 네이버 2건 반영 | 2 |
| 6 | product-auth | **MSW handler 8종 401 코드 재현** | `AUTH001/002/003/004/101/102/103/104` 각각 모킹 handler 정착 → Vitest에서 인터셉터 검증 | 2 |
| 7 | product-card (done 정합) | **실패 어휘 CI grep 실체화** | `src/features/cards`, `src/features/card-editor`에서 "실패" 어휘 grep → CI job 등록 | 1 |
| 8 | product-learningFacade (done 정합) | **CoverageBadge 색상 상수 단일화** | `NO_MATERIAL/PARTIAL/COVERED` 매핑 상수 `features/map/constants.ts`에 정착. snapshot 테스트 | 1 |

**Tier 1 합계: 8 Story · ~12 SP**

### Tier 2 · Want (도전 — 3 Story)

| # | Product | Story | 한 줄 | SP |
| --- | --- | --- | --- | --- |
| 9 | product-fe-cdn | **Lighthouse CI 1회 수동 실행** | CF 첫 배포 후 Performance/Accessibility/Best Practices/SEO 4카테고리 baseline 확보 → `performance.md` 기입 | 2 |
| 10 | product-auth | **`ApiError.code + requestId` DevTools 로깅 강화** | `import.meta.env.DEV`에서 5xx + 401 발생 시 `console.warn` 확장 (이미 5xx 있음, 401 추가) | 1 |
| 11 | product-learningFacade | **dnd-kit 도입 FE-ADR 초안** | `FE-ADR-CANDIDATES.md` P1-2 항목 → 실제 FE-ADR 초안 문서 작성 (도입 결정만) | 1 |

**Tier 2 합계: 3 Story · ~4 SP**

### 카테고리별 합계

| 카테고리 | Story 수 | SP | 비중 |
| --- | --- | --- | --- |
| 배포·인프라 (fe-cdn) | 3 + 1(T2) | 5 + 2 | 44% |
| 계약 정합 (BE Story 5-4 대응) | 1 | 1 | 6% |
| 인증·세션 UX | 2 + 1(T2) | 4 + 1 | 31% |
| done 정합 grep · 상수 정착 | 2 | 2 | 13% |
| FE-ADR | 1(T2) | 1 | 6% |
| **합계** | **11** | **16** | 100% |

**분배 근거**:
- FE M1은 백엔드가 인프라를 깔아준 위에 "실제 브라우저에서 접속 가능한 상태"까지 검증하는 것이 1차 가치. 배포·검증에 44% 집중
- LoginPage 정합·done grep은 "BE 도메인 변경이 FE에 조용히 드리프트하는 것"을 이번 주에 발견·차단
- Lighthouse는 Tier 2 — CF 배포 활성 시점에 종속. 대기 시간 있으므로 Want로 유예
- 신규 Feature 도메인은 M2 이후 (M1은 검증·정합 주간)

---

## 종료 신호 — "브라우저에서 도메인으로 접속되고 로그인이 흐른다"

본주 종료 시점에 다음이 모두 성립해야 한다. (Tier 1 기준)

- [ ] **머지 신호**: Tier 1 8 Story 중 최소 6 머지 (75%)
- [ ] **환경변수 신호**: FE 레포 `.env.production` 커밋됨 + prod build에 `VITE_API_BASE_URL=https://api.thirdtool.dev` 반영
- [ ] **첫 배포 신호**: GHA `deploy-fe.yml` workflow 1회 이상 성공 + S3 sync + CF invalidation 완료
- [ ] **도메인 신호**: 브라우저에서 `https://thirdtool.dev` 접속 시 FE React SPA 로드 (index.html 200 + 앱 마운트)
- [ ] **인증 흐름 신호**: `https://thirdtool.dev/login`에서 로그인 시도 → `https://api.thirdtool.dev/login` 호출 성공 → `Set-Cookie: access_token` 브라우저 수신 → `/home` 진입
- [ ] **계약 정합 신호**: `PUT /user` 요청 payload에 `username` 필드 grep 0건 확인 (BE Story 5-4 대응 완료)
- [ ] **인터셉터 신호**: MSW로 AUTH001~104 각각 재현 시 인터셉터가 refresh 또는 세션 클리어를 규정대로 처리
- [ ] **문구 신호**: `src/features/cards`, `src/features/card-editor`에서 "실패" 어휘 grep 0건 + CI job 등록

**미합격 처리**: 위 8 신호 중 6개 미만 성립 시 FE M1을 0.0.1v로 동결하지 않고 0.0.1.1v 패치 발행 → 다음주 초까지 연장. 특히 도메인 신호·인증 흐름 신호는 BE 사용자 액션(D1 도메인 등록) 대기에 좌우되므로 BE M1 통과 시점과 결합해서 판단.

---

## 의존 chain

```
[사용자 직접 액션 — BE 마일스톤과 공유]
D1 (BE) Route53 도메인 등록 ── D2 (BE) ACM 2개 발급
                                        │
                                        ├── D3 (BE) ALB HTTPS ── D4 (BE) api 레코드    (BE 도메인 완성)
                                        │
                                        └── D5 (BE) S3 버킷 ── D6 (BE) CloudFront ── D7 (BE) FE 레코드   (FE CDN 활성)
                                                                                       │
                                                                                       ▼
                                                                            FE-1 `.env.production` 확정
                                                                                       │
                                                                                       ▼
                                                                            FE-2 GHA workflow 첫 실행
                                                                                       │
                                                                                       ▼
                                                                            FE-3 브라우저 접속 검증 (`https://thirdtool.dev`)
                                                                                       │
                                                                                       ▼
                                                                            FE-3-2 CORS + SameSite E2E (`https://api.thirdtool.dev/login`)

[BE 의존 없이 독립 진행 가능]
FE-4 username grep · 제거 (BE Story 5-4 이미 완료)
FE-5 LoginPage 상수화 (독립)
FE-6 MSW 8종 401 handler (독립)
FE-7 실패 어휘 CI grep (독립)
FE-8 CoverageBadge 상수 정착 (독립)
```

**병렬 진입 가능 묶음** (BE 대기 없이 D1부터 시작):
- A: FE-4 (username grep) — 30분 작업
- B: FE-5 (LoginPage 상수화)
- C: FE-6 (MSW 401 handlers)
- D: FE-7 (실패 grep) + FE-8 (Coverage 상수)

**직렬 (배포 검증까지)**: 사용자 D1 완료 → BE D2·D3·D4·D5·D6·D7 순차 → FE-1 → FE-2 → FE-3 → FE-3-2

---

## 작업 일정 (체크리스트)

오늘이 D1(화). 종료가 D6(일). BE 마일스톤과 동일 주기.

| 일 | 날짜 | 잡힌 작업 (FE) |
| --- | --- | --- |
| D1 (화) | 06-23 | **독립 작업**: FE-4 username grep + FE-8 CoverageBadge 상수 착수. **대기**: BE 도메인 등록(D1) |
| D2 (수) | 06-24 | **독립 작업**: FE-5 LoginPage 상수화(social-providers.ts) + FE-7 실패 어휘 grep CI 셋업. **대기**: BE ACM 발급 |
| D3 (목) | 06-25 | **독립 작업**: FE-6 MSW 8종 401 handler + Vitest 인터셉터 시나리오 8건. **대기**: BE ALB HTTPS + api 레코드 |
| D4 (금) | 06-26 | **연동 작업**: FE-1 `.env.production` 확정 (BE api 레코드 완료 후). **대기**: BE S3 + CloudFront |
| D5 (토) | 06-27 | **연동 작업**: FE-2 GHA workflow 첫 실행 + FE-3 브라우저 접속 검증 (BE CF 활성 후). FE-3-2 CORS E2E |
| D6 (일) | 06-28 | **검증**: 종료 신호 8개 셀프 체크 + Want Tier 시도 (Lighthouse baseline / 인터셉터 로깅 강화 / dnd-kit FE-ADR 초안) + `infra.md` / `performance.md` / `outcome.md` / `cost.md` / `review.md` / `ux-test.md` 작성 |

> 11 Story가 무리이면 Tier 2 3개를 D6에 모두 미루기 허용. Tier 1 8개만 통과해도 M1 합격.
> BE 사용자 액션(D1 도메인 등록)이 지연되면 FE-1~FE-3 자동 연장 — 이 경우 D6에는 대기 상태만 기록하고 0.0.1.1v 패치로 연장.

---

## 리스크와 관찰 포인트

| 영역 | 리스크 | 관찰 포인트 |
| --- | --- | --- |
| BE 인프라 대기 | 도메인 등록 · ACM 검증 · CF 활성이 사용자 직접 액션 → 며칠 대기 가능 | D3 종료 시점에 BE `infra.md` D1~D7 진척 확인 → 지연 시 Tier 1 재분배 |
| `.env.production` 노출 | 실수로 API 비밀·키가 커밋될 우려 | `VITE_API_BASE_URL` 외 값 미포함 확인. `.gitignore`에 `.env.local` 유지 |
| CORS + SameSite=Strict prod 첫 검증 | dev(Lax)에서는 통과하지만 prod(Strict)에서 쿠키 미전송 발생 가능 | 첫 로그인 시도 시 DevTools Network 탭에서 `Set-Cookie` 실제 수신 확인 · Application 탭 Cookies에서 쿠키 존재 확인 |
| GHA workflow 첫 실행 | OIDC AssumeRole 권한 누락 · S3 sync 실패 · CF invalidation 실패 | 각 step 로그 확인 · gha-deploy-role FE 권한(FeSyncS3/FeCdnInvalidate) 존재 확인 |
| Lighthouse baseline 왜곡 | CF 캐시 안 된 최초 로드는 지표가 나쁨. 2회차 로드부터 정상 | Lighthouse 실행 전 사전 warmup 1회 |
| BE Story 5-4 grep 누락 | `username` 참조가 여전히 남아있으면 향후 400 응답 발생 | grep 후 코드 리뷰 + Vitest 통합 테스트 1건 |
| MSW 8종 handler 오작동 | 인터셉터가 refresh 재시도를 잘못 트리거 | 각 코드별 시나리오 Vitest로 단위 검증 |
| 사용자 액션 순서 혼동 | BE D1~D7이 지연되면 FE M1 통과 불가 | D3 시점에 BE 마일스톤 리뷰 회의 (혼자여도 셀프) — 필요 시 M1 통과 기준 조정 |

---

## 다음 마일스톤 (FE M2 / 0.0.2v) 후보

본주 결과를 보고 결정하지만, 현재 시점 후보:

- **AI Suggestion 통합 UI** (`product-aisuggestion.md`): BE M2에서 Controller 노출 예정 → FE OnboardingPage에서 v1(빈 지도) 활성화 + SuggestionDialog 컴포넌트 착수
- **k6/Lighthouse 부하 반응 UX** (BE M2가 k6 부하 baseline 확보 → FE는 그 시나리오에서 UX 회복 관찰)
- **인증·세션 페이지 확장** — `Product.md` (User): MePage 수정/삭제 UI, 2단계 confirm dialog, LoginPage USER_IS_SOCIAL 인라인 안내
- **dnd-kit 도입** — `product-learningFacade.md`: Axis/Topic reorder UX (M1의 FE-ADR 결정 근거로)
- **backend-boundary/error-codes.md 갱신** — BE ErrorCode 확장에 맞춰 FE UX 매핑 갱신 · CI grep 확대
- **product-search.md 첫 발** — BE M2가 검색 baseline VO 진입하면 FE도 SearchInput 컴포넌트 초안

**기본 권장 FE M2**: AI Suggestion 통합 UI 착수 + MePage 확장 + `backend-boundary/error-codes.md` 갱신 + Lighthouse 개선 첫 사이클.

---

## brainstorming 트리거

본 FE M1 완료 후 `workflow/task/pes/brainstorming/0.0.2v/` (신설 예정)에 FE 관점 후보 상태 전이 반영:

- **promoted**: CloudFront + CORS 정합성 검증 → `product-fe-cdn.md` 첫 배포 완료
- **promoted**: LoginPage 소셜 provider 확장 패턴 → `Product.md` (User) Epic 1 완료
- **promoted**: MSW 8종 401 handler → `product-auth.md` Epic 1 완료
- **신규 후보**: Lighthouse baseline 이후 Performance 개선 우선순위 (LCP · 코드 스플리팅)
- **신규 후보**: BE 계약 drift 탐지 자동화 (Zod schema vs BE Swagger 대조)
- **신규 후보**: `.env.local` git 실수 방지 pre-commit hook

---

## 참고

- 잔여 FE Story 인벤토리 출처: `workflow/task/pes/fe/fe-workspectrum/sdd/` 10개 Product 파일
- FE 마일스톤 패키지 의도: `workflow/task/pes/fe/fe-milestones/references/001.md`
- 백엔드 대응 마일스톤: `workflow/task/milestones/version/0.0.1v/milestone.md`
- BE→FE 핸드오프 참조: `workflow/task/pes/handoff/aws-setup-0.0.1v.md` (있는 경우)
- 본 버전의 산출물 6종: `infra.md`, `performance.md`, `outcome.md`, `cost.md`, `review.md`, `ux-test.md`
