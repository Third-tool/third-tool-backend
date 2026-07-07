# [Fix] FE 온보딩 facade 생성 직후 네비게이션 정정 (2026-06-27)

## 대상 (원본)
- 원본 Product: `workflow/task/pes/fe-handoff/0.0.1v.md` (FE-BE 계약 핸드오프)
- 영향 받는 Story: OnboardingPage 완료 후 라우팅 흐름 / ProtectedRoute requireConcept 진입 보호
- 본 fix 발견 시점: 2026-06-27

## 트리거
M1 사용자 테스트 brainstorming (`workflow/task/fix/brainstorming/version/0.0.1v/fe.md` Issue 3):
facade 생성 직후 `navigate('/home')` 패턴이 두 가지 문제를 동반한다.
(1) **기술 버그**: `qc.invalidateQueries` 결과를 await하지 않고 즉시 navigate → 캐시에 stale `{concept: null}` 항목이 있으면 ProtectedRoute가 background refetch 완료 전에 null을 읽어 `/onboarding`으로 재리다이렉트하는 루프 위험.
(2) **UX 표류**: facade(+첫 axis) 생성 직후 `/home`으로 보내면 첫 주제 등록 흐름이 끊김 — `/home`에 빈 상태 CTA가 없을 경우 사용자가 다음 액션을 모름.

---

## 현재 코드 흐름 (확인)

**파일**: `src/features/onboarding/OnboardingPage.tsx` — `save` mutation

```ts
// OnboardingPage.tsx:106–126
const save = useMutation({
  mutationFn: async (payload) => {
    await createFacade(payload.statement);              // POST /api/v1/learning-facade
    if (payload.bridge.trim()) await createAxis(payload.bridge.trim()); // POST /api/v1/learning-facade/axes
    return payload;
  },
  onSuccess: (payload) => {
    qc.invalidateQueries({ queryKey: LEARNING_FACADE_KEY }); // ❌ fire & forget — not awaited
    track('onboarding_completed', { ... });
    navigate('/home', { replace: true });              // ❌ races with invalidate refetch
  },
});
```

**ProtectedRoute 진입 보호** (`src/features/auth/components/ProtectedRoute.tsx:73`):
```ts
if (!facade.data?.concept) {
  return <Navigate to="/onboarding" replace />;       // stale null → 루프
}
```

**useLearningFacade** (`src/features/auth/hooks/useLearningFacade.ts`):
- 404 → catch → `{concept: null}` 반환 (throw 안 함)
- `staleTime: 60_000` — invalidate 전까지 캐시 유지

**레이스 시나리오**:
1. 사용자가 이전 세션에서 `/home` 진입 시도 → ProtectedRoute가 `GET /api/v1/learning-facade` → 404 → `{concept: null}` 캐시 저장 → `/onboarding`으로 리다이렉트
2. 온보딩 완료 → `createFacade` 성공 → `invalidateQueries` (fire & forget) → `navigate('/home')`
3. ProtectedRoute 마운트: 캐시에 stale `{concept: null}` 존재 + background refetch 진행 중
4. TanStack Query v5: stale 캐시가 있으면 `isLoading=false`, `isFetching=true` — stale 데이터로 즉시 렌더
5. `facade.data.concept === null` → `/onboarding` 재리다이렉트 → **루프**

---

## 변경 내역

### Story별 명세 변경 이력

#### Story F-1: OnboardingPage save.onSuccess — invalidate await 처리

**변경 전 AC**:
Given facade + axis 생성 성공 / When onSuccess 실행 / Then `invalidateQueries`(fire & forget) 후 즉시 `navigate('/home')`

**변경 후 AC**:
Given facade + axis 생성 성공 / When onSuccess 실행 / Then `await invalidateQueries` (refetch 완료 보장) → `navigate('/map')` *(navigate 목적지는 Story F-2 결정 종속)*

**변경 코드 (`OnboardingPage.tsx:112`)**:
```ts
// 변경 전
onSuccess: (payload) => {
  qc.invalidateQueries({ queryKey: LEARNING_FACADE_KEY });
  navigate('/home', { replace: true });
},

// 변경 후
onSuccess: async (payload) => {
  await qc.invalidateQueries({ queryKey: LEARNING_FACADE_KEY });
  track('onboarding_completed', { method: payload.method, conceptCount: concepts.length });
  navigate('/map', { replace: true }); // Story F-2 결정값으로 교체
},
```

> `invalidateQueries`를 await하면 활성 observer의 refetch가 완료될 때까지 대기 → navigate 시점에 캐시가 최신(`concept: non-null`) 상태 보장. ProtectedRoute bounce 없음.

**DoD**:
- [ ] `onSuccess`를 `async`로 변경 후 `await qc.invalidateQueries(...)` 처리
- [ ] track 호출이 invalidate 이후에 오도록 순서 유지
- [ ] 수동 확인: 온보딩 완료 → 대상 라우트 진입 시 `FullPageLoader` flash 없음, console.warn 없음

#### Story F-2: 온보딩 완료 후 네비게이션 목적지 (UX — 사용자 결정 대기)

**현재 코드**: `navigate('/home', { replace: true })`

**선택지**:

| 선택 | navigate 변경 | UX 가설 | 추가 작업 | tier |
|------|--------------|---------|----------|------|
| (A) `/home` 유지 | 없음 | /home 빈 상태에서 "첫 축 추가" CTA로 안내 | 홈 빈 상태 CTA 신설 필요 | `feature-story` |
| **(B) `/map` 직행** | `'/home'` → `'/map'` | onboarding에서 facade+axis 생성 완료, 맵에서 첫 주제 추가 자연스러움 | 1줄 변경 | `one-line-spec` |
| (C) onboarding 재설계 | 6-step 재구성 | 첫 axis·topic을 onboarding 안에서 가이드 형태로 등록 | Issue 2 BE 결정 이후 검토 필요 | `pes` |

**권장: (B) `/map` 직행**
- 이유: onboarding에서 이미 첫 axis까지 생성됨(`payload.bridge`). 맵은 "axis → topic 추가" CTA가 자연스러운 다음 맥락. `/home`은 현재 첫 진입 빈 상태 UX 미정.
- 전제: `/map` 라우트도 `requireConcept=true`이므로 Story F-1의 await fix가 선행돼야 bounce 없음.

**변경 후 AC (선택 B 기준)**:
Given 온보딩 6-step 완료 ("이 조합으로, 지도 펼치기" 클릭 + facade + axis 생성 성공)
When `onSuccess` 실행
Then `await invalidateQueries` 완료 → `/map`으로 이동, ProtectedRoute `requireConcept` 통과 (bounce 없음)

**DoD**:
- [ ] `navigate('/home')` → `navigate('/map')` 1줄 변경 (Story F-1 await fix와 동일 커밋)
- [ ] 브라우저 수동: onboarding 완주 → `/map` 자동 이동, 주소창 `/onboarding` 잔류 없음
- [ ] 캐시 stale 시나리오: 개발 도구 → Application → Clear storage 후 시나리오 재현 → bounce 없음

---

## 검증
- FE 단위: `OnboardingPage` save mutation mock 테스트 — onSuccess async 순서 (invalidate await → navigate) 검증
- 수동 (mocks=true): 온보딩 6-step 완주 → `/map` 진입, ProtectedRoute bounce 없음, console.warn 0건
- 수동 (mocks=false, BE 기동): 동일 시나리오, `GET /api/v1/learning-facade` 200 응답 후 navigate 확인
- 레이스 재현 테스트: DevTools에서 네트워크 throttling(Slow 3G) 후 온보딩 완료 → stale null 루프 없음 확인

---

## 영향
- `src/features/onboarding/OnboardingPage.tsx` — `save.onSuccess` async 변환 + navigate 목적지 1줄 변경
- `src/features/auth/components/ProtectedRoute.tsx` — 변경 없음 (로직 이미 정상, bounce는 FE 캐시 경쟁 문제)
- `src/features/auth/hooks/useLearningFacade.ts` — 변경 없음
- 원본 핸드오프 명세 (`workflow/task/pes/fe-handoff/0.0.1v.md`) — [명세 변경 이력] 블록에 본 fix 식별자 기재
- 다른 BC: N/A (FE 단독)
- ADR: N/A
