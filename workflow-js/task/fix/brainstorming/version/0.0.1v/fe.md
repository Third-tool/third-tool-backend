# [Fix · Brainstorming] FE 측 — 0.0.1v (2026-06-27)

> **본 파일의 역할**: M1 planning 표류 신호 중 **FE 책임 영역**의 가설·결정 누적. 자유 형식.
> **짝 파일**: `./be.md` — BE 측 책임 영역. 양쪽 책임 이슈는 양 파일에 모두 등장.
> **FE 저장소**: `C:\study\System_Author\third-tool-fe\untitled` (별도 git). 본 brainstorming은 BE 저장소에 인계서로만 작성 — FE 측 작업 지시는 사용자가 옮겨서 진행.
> **다음 단계**: 각 이슈가 결정·발견을 거쳐 tier 확정되면 `workflow/task/fix/{tier}/version/0.0.1v/` 양식으로 정식 fix 작성.

작성 시점: 2026-06-27 (M1 D5)

---

## 트래킹 컨텍스트

| 영역 | 사실 |
| --- | --- |
| FE 스택 | React 18 + Vite + TypeScript + TanStack Query 5 + React Router 6 + Tailwind 4 + MSW + Zod |
| FE 라우트 (15개) | `/`, `/login`, `/signup`, `/onboarding`(6-step), `/home`, `/study`, `/cards/new`, `/map`, `/archive`, `/archive/:cardId`, `/tags`, `/tags/:tagId`, `/me`, `/maintenance`, `*`(404) |
| FE 인증 토큰 | AT는 HttpOnly Cookie 자동 전송, RT는 메모리(`refreshTokenStore.ts`) |
| BE 핸드오프 | `workflow/task/pes/fe-handoff/0.0.1v.md` — API 인벤토리·계약·CORS·Runbook |
| 발견 시점 | M1 진행 중 사용자 테스트에서 표류 신호 3건 발견 |

---

### Issue 1 — 소셜 로그인 (Naver / Kakao) 미동작 — **FE 측**

**현상 / 트리거**
사용자 테스트: 카카오·네이버 로그인 둘 다 실패. 정확한 실패 지점(어디서·어떤 응답)이 아직 미확인. BE 가설은 `./be.md` Issue 1 참조.

**원인 가설 (FE 측 책임 영역)**

| # | 가설 | 가능성 사유 |
| --- | --- | --- |
| (a) | `/oauth/kakao/callback`, `/oauth/naver/callback` 라우트가 라우터에 미배선 또는 미구현 | BE `application-dev.yml`이 redirect_uri를 이 경로로 고정. FE 라우트 15개 목록(상기 표)에 없음 → 가능성 높음 |
| (b) | OAuth 시작 버튼이 BE가 아닌 provider 인증 URL로 직접 이동 — provider는 redirect_uri로 콜백 보내는데 FE 콜백 페이지가 token 교환 흐름을 안 함 | provider 흐름 전형. 콜백 라우트만 있어도 거기서 `code`/`state` 받아 `POST /social/login/{provider}` 호출 코드가 필요 |
| (c) | `POST /social/login/{provider}` 호출 시 `credentials: include` 누락 → 발급된 AT 쿠키가 브라우저에 안 셋 | FE `lib/api/client.ts` 인터셉터가 `withCredentials: true`로 통일되어 있는지 확인 필요 |
| (d) | 콜백 페이지에서 RT 메모리 저장(`refreshTokenStore.ts`) 흐름 누락 — 응답 바디 `{"refreshToken": "..."}` 처리 미구현 | 자체 로그인은 동작하므로 패턴은 있음. 단지 소셜 콜백에서 재사용 안 되는 경우 |

**가능성 큰 가설**: (a) + (b) 동시. 콜백 라우트 자체가 미구현이면 카카오/네이버 모두 동일 증상.

**영향 범위**
신규 사용자 진입 차단 (소셜 가입). 기존 사용자는 자체 로그인으로 우회 가능.

**결정해야 할 것**
- FE staging/prod에서 콜백 도메인 확정 — 현재 dev `http://localhost:5173/oauth/{provider}/callback` 고정. prod는 `https://thirdstool.com/oauth/{provider}/callback` (BE `application-prod.yml`).
- 콜백 페이지의 UX — 자동 토큰 교환 진행 표시 / 실패 시 로그인 페이지 리다이렉트.

**권장 fix 방향 (1차)**
1. FE 측 콜백 라우트 신설:
   - `/oauth/kakao/callback` 페이지 (`src/features/auth/oauth-callback.tsx` 추정)
   - `/oauth/naver/callback` 페이지
   - 두 라우트가 query string에서 `code` (+ naver는 `state`) 추출
2. 추출 후 `POST /social/login/{provider}` 호출 (`provider`는 path에서 결정)
3. 응답 `TokenResponse {refreshToken}` 수신 → `refreshTokenStore` 저장 + AT 쿠키 자동 셋 확인
4. 성공 시 `/home`(또는 onboarding 진입 여부에 따라 분기) 라우트로

**workspectrum tier 추천 (FE 측)**
- 콜백 라우트 신설 + 토큰 교환 훅 추가 동반 → **`feature-story`** (1 메서드/1 PR 단위, 단위 테스트 동반)
- 만약 BE redirect_uri 정합·CORS 동시 조정 필요 → **`sdd-lite`** (계약 인계 동반)

대안 tier: 단순히 라우트 1개 추가만 누락이면 **`one-line-spec`** 가능하지만, 콜백 페이지는 토큰 교환 호출이 동반되므로 거의 항상 `feature-story` 이상.

---

### Issue 2 — LearningFacade 유저당 1개 제약 — **FE 측 (BE 결정 종속)**

**현상 / 트리거**
사용자 테스트: facade 생성 시도 → BE `LF002 LEARNING_FACADE_ALREADY_EXISTS` (409) → FE는 그대로 에러 토스트로 노출.

**원인 가설 (FE 동작 현황)**
- 현재 FE 흐름 (추정): `POST /api/v1/learning-facade` 호출 → 200/201이면 성공, 4xx면 에러 토스트.
- `LF002` 받았을 때 **자동으로 `GET /api/v1/learning-facade`로 폴백**해 기존 facade 가져오는 분기가 있는지 확인 필요 (FE `features/map/` 또는 `features/onboarding/` 코드).
- 직전 대화 BE 답변(이 문서 작성자 직전 답): "`LF002`는 회복 가능한 에러가 아님 — GET으로 폴백하라" 권장.

**영향 범위**
- 사용자가 onboarding 도중 facade 생성 재시도(예: 페이지 새로고침 후 다시 제출) 또는 이미 가입된 계정으로 onboarding 진입 시 즉시 막힘.
- 신규 사용자 진입 첫 화면 차단.

**결정해야 할 것 (BE 결정 종속)**
BE Issue 2의 결정(A/B/C) 결과에 따라 FE 작업 범위 결정:
- **BE (A) 1개 유지**: FE는 `LF002` 받으면 자동 GET 폴백 분기 추가 + onboarding 흐름에서 "이미 facade 있음 → 바로 home으로" UX 조정
- **BE (B) 다수 허용**: FE는 facade 목록·선택·전환 화면 신설 (`/facades` 라우트 + 전환 훅 + map에 활성 facade 표시)
- **BE (C) 중간형(active 전환)**: FE는 active facade 전환 토글 신설 + archived 목록 화면

**권장 fix 방향 (1차 — BE 결정 종속)**
- 결정 전 단계: **결정 X. BE 결정 대기.**
- BE (A) 결정 시: `useCreateLearningFacade` 훅 에러 핸들러에 `LF002` → GET 폴백 분기 1건 추가. 1 훅 변경 + 단위 테스트.
- BE (B) 결정 시: facade 선택 UX 신설 — 다수 화면·훅·라우트.
- BE (C) 결정 시: facade 전환 UX 신설 + archived 화면.

**workspectrum tier 추천 (FE 측)**
- BE (A) 시: **`feature-story`** (1 훅 + 1 단위 테스트 + AC 1줄 추가)
- BE (B) 시: **`pes`** (Epic 1개 — facade 관리, Story 3-5개)
- BE (C) 시: **`pes`** (active 전환 흐름 Story 2-3개)

대안 tier: BE (A) + 동시에 onboarding 흐름 일부 재설계가 필요하면 **`pes`** (다른 Story 영향 2-3건).

---

### Issue 2 — 실제 관측 및 에러 시나리오 (2026-06-27 사용자 테스트 추가)

> Issue 2 원문의 "가설" 단계에서 실제 재현으로 전환. 아래는 코드 확인 + BE 로그 기반 사실.

**BE 로그 관측 (2026-06-27 22:28)**

| 항목 | 내용 |
| --- | --- |
| 경로 | `POST /api/v1/learning-facade` |
| 응답 | 409 `LF002 이미 LearningFacade가 존재합니다.` |
| 반복 횟수 | 브라우저 콘솔 기준 ×7 (동일 요청 반복) |
| 대상 유저 | `user_id=7` (기존 facade 보유 확인됨) |
| 병행 오류 | 별도 리소스에서 404 1건 발생 (경로 미확인) |

**FE 코드 현황 (확인)**

```
OnboardingPage.tsx — save.onError:
  LF002(409) 수신 → err.message를 inline 텍스트로 노출
  → "지도 펼치기" 버튼 재활성화(pending=false)
  → 사용자가 버튼을 다시 누를 수 있음 → ×7 반복 발생

/onboarding 라우트 — ProtectedRoute:
  requireConcept 없음 → facade 보유 여부 불문하고 진입 허용
  → 기존 facade 보유 유저도 /onboarding 직접 입력 시 막히지 않음
```

**에러 시나리오 목록**

| # | 진입 경로 | 트리거 | 결과 | 심각도 |
| --- | --- | --- | --- | --- |
| S-1 | 기존 facade 보유 유저 → URL 직접 `/onboarding` 입력 → 제출 | ProtectedRoute가 facade 유무 미체크 | 409 loop, 유저 고착 | 높음 |
| S-2 | 온보딩 완료 직후 탭 새로고침 → ProtectedRoute가 `GET /api/v1/learning-facade` → 타이밍에 따라 404 수신(캐시 아직 없음) → `concept: null` → `/onboarding` 재진입 → 재제출 | Issue 3 fix(await invalidateQueries) 이전에도 발생 가능. navigate 전에 캐시가 갱신되지 않은 상태에서 새로고침하면 동일 경로 | 409 반복, 유저 루프 | 높음 |
| S-3 | 소셜 로그인(Issue 1 fix 이후) → AT 쿠키 정상 셋 → `GET /api/v1/learning-facade` 200(기존 facade 있음) → ProtectedRoute가 `/home` or `/map`으로 보내야 하는데, facade 없을 때 캐시된 `{concept:null}` stale이 남아있으면 → `/onboarding` → 제출 → 409 | Issue 3 수정(await invalidate) 이후 환경에서도 첫 소셜 로그인 + 기존 계정이면 S-2와 동일 패스 | 409, 유저 고착 | 중간 |
| S-4 | 온보딩 제출 후 오류 인라인 노출 상태에서 버튼 연타 | pending=false 이후 버튼 재활성화 | 동일 POST ×N 반복 (×7 관측) | 낮음 (UX 문제) |

**FE 수정 필요 지점 (BE 결정 이전 즉시 적용 가능한 항목)**

1. **`OnboardingPage.tsx` save.onError — LF002 분기**
   ```ts
   onError: (err) => {
     if (err instanceof ApiError && err.code === 'LF002') {
       // facade 이미 있음 → cache 무효화 후 /map 이동 (Issue 3 결정값과 동일)
       void qc.invalidateQueries({ queryKey: LEARNING_FACADE_KEY });
       navigate('/map', { replace: true });
       return;
     }
     // 기타 에러만 inline 노출
     setInline(err instanceof ApiError ? err.message : '지금 저장이 어려워요...');
   },
   ```
   - S-1·S-2·S-3 모두 커버. 비동기 await 불필요 (navigate 이후 백그라운드 refetch로 충분).

2. **`/onboarding` ProtectedRoute — 역방향 가드 (requireNoConcept)**
   - 기존 facade 보유 유저가 `/onboarding` 직접 진입 시 `/map`으로 리다이렉트.
   - `ProtectedRoute`에 `redirectIfConcept?: string` prop 추가 또는 `OnboardingPage` 내부에서 `useLearningFacade` 상태 체크.
   - S-1 완전 차단. S-2·S-3은 onError fallback(위 1번)으로 처리.

3. **S-4 (버튼 연타) — 즉시 fix**
   - `onError` 후 `save.reset()`을 호출하지 않으면 `isPending=false`가 되어 버튼 재활성화됨. 현재 동작 자체는 정상이지만, LF002 에러 시 버튼을 비활성 상태로 유지하거나 메시지를 "이미 생성됨 → 이동 중…"으로 교체하는 UX 보강 필요.

**BE 결정 대기 항목 (변경 없음)**
- BE (A/B/C) 결정 이후 facade 복수 허용 여부에 따라 추가 UX 작업 범위 확정.
- 위 즉시 fix 항목들은 BE 결정과 무관하게 적용 가능.

---

### Issue 3 — LearningFacade 생성 직후 홈 이동 흐름 — **FE 측 (FE 단독, BE 영향 0)**

**현상 / 트리거**
사용자 의문: facade 생성을 막 끝낸 사용자가 곧바로 `/home`으로 이동하는 게 UX적으로 합당한가? 생성 직후엔 축·주제 등록(작성)이 자연스러운 다음 액션 아닌가?

**원인 가설 (FE 라우팅 현황 — 코드 미확인)**
- 현재 흐름 추정: `POST /api/v1/learning-facade` 성공 → `navigate('/home')`
- 또는 onboarding 6-step의 마지막 step이 facade 생성이고 거기서 `/home`으로 종료
- 정확한 코드는 `features/onboarding/` 또는 `features/map/` 또는 `features/home/` 어딘가에 있을 것 (확인 필요).

**영향 범위**
- 신규 사용자가 facade 만들고 홈으로 가면 "다음에 뭐 하라는 거지" 상태가 됨. 홈에 "축 추가하기" 같은 CTA가 없으면 학습 트리 구성 흐름이 끊김.
- 기존 사용자(이미 facade 보유)의 홈 진입 UX와 신규 사용자(첫 생성 직후)의 홈 진입 UX가 동일해도 되는지 의문.

**결정해야 할 것 (사용자 결정 — UX 영역)**

| # | 분기 | UX 가설 |
| --- | --- | --- |
| (A) | 홈 이동 유지 | 홈이 모든 진입의 시작점. 거기서 "축 추가" CTA로 다음 액션 안내. CTA가 명확하면 OK. |
| (B) | 작성/맵(`/map`)으로 직행 | facade 만들었으니 즉시 첫 축·주제 등록 UX로. 학습 트리 첫 구성 흐름이 자연스러움. |
| (C) | onboarding 6-step 흐름 재설계 | facade 생성이 onboarding 안에 있다면, 그 뒤 step에서 첫 축 1개·첫 주제 1개를 가이드 형태로 작성하게 유도. |

**권장 fix 방향 (1차 — 사용자 결정 종속)**
- (A) 유지: fix 불요. 단, 홈 페이지에 "처음이라면 축 추가하기" 같은 빈 상태 CTA가 있는지 확인 (작은 보강 가능).
- (B) 직행: `useCreateLearningFacade` 성공 콜백에서 `navigate('/home')` → `navigate('/map')` 또는 `/cards/new`로 변경. 1 라인 정도.
- (C) onboarding 재설계: 6-step 흐름 검토 + 7-step 또는 step 안에서 가이드 UI 추가.

**workspectrum tier 추천 (FE 측)**
- (A) 유지: tier 없음 (fix 불요). 홈 빈 상태 CTA 추가 검토 시 **`feature-story`** (1 컴포넌트 + 단위 테스트)
- (B) 직행: **`one-line-spec`** (라우트 1개 변경) 또는 **`feature-story`** (분기·테스트 동반 시)
- (C) onboarding 재설계: **`pes`** (Story 3-5개) — Issue 2 BE 결정 + Issue 1 소셜 콜백 흐름과도 얽힐 가능성

---

## 누적 메모 (자유 형식 영역)

이 아래는 사용자가 자유롭게 누적. 새 가설·외부 입력·관련 컨텍스트를 시간순으로 쌓아간다. Claude는 사용자 명시 요청 없으면 미리 채우지 않음.

- 2026-06-27 — 본 brainstorming 초기 작성 (Issue 1·2·3 FE 측 모두 명시)
- 2026-06-27 — Issue 3 fix 결정: (B) `/map` 직행 + await invalidateQueries. `OnboardingPage.tsx` 수정 완료 커밋.
- 2026-06-27 — Issue 2 실제 관측 추가: `POST /api/v1/learning-facade` 409 LF002 ×7 (user_id=7). 에러 시나리오 S-1~S-4 정리. 즉시 fix 방향 3건 명시 (BE 결정 독립). BE 결정 대기 항목 유지.

---

## 참조

- 짝 파일: `./be.md`
- fix tier 정의: `../../../{tier}/version/0.0.1v/{tier}.md`
- 이전 fe-handoff: `workflow/task/pes/fe-handoff/0.0.1v.md` (FE 작업자가 cold start로 읽어야 할 BE 계약)
- FE 저장소: `C:\study\System_Author\third-tool-fe\untitled`
- BE 인증 흐름 사실: `src/main/java/com/example/thirdtool/User/presentation/SocialLoginController.java`
- BE `application-dev.yml` redirect_uri 사실: `src/main/resources/application-dev.yml` (kakao/naver 블록)
