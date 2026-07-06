# [Fix] FE — OnBoarding LF002 루프 즉시 수정
> 0.0.1v 즉시 단독 fix (2026-06-30) · FE 전용 · BE 변경 없음

**배경**: M1 사용자 테스트에서 기존 facade 보유 유저가 `/onboarding` 재진입 시
`POST /api/v1/learning-facade` 409 LF002를 무한 반복(×7 관측)하는 증상 확인.
BE는 (A) 1 Facade 유지로 결정 확정(→ `issue2b-facade-multicontext-deck-arc-report.md`).
FE 단독으로 즉시 해결 가능.

**연계 보고서**: `./fe.md` Issue 2 에러 시나리오 S-1~S-4

---

## 에러 시나리오 요약

| # | 진입 경로 | 증상 | 심각도 |
|---|-----------|------|--------|
| S-1 | 기존 facade 보유 유저가 URL 직접 `/onboarding` 입력 → 제출 | 409 loop, 유저 고착 | 높음 |
| S-2 | 온보딩 완료 직후 새로고침 → 캐시 없음(concept: null) → 재진입 → 재제출 | 409 반복 | 높음 |
| S-3 | 소셜 로그인 후 stale 캐시로 /onboarding 재진입 → 제출 | 409, 유저 고착 | 중간 |
| S-4 | LF002 onError 후 버튼 재활성화 → 연타 | 동일 POST ×N | 낮음 (UX) |

---

## 수정 지점 3건

### 수정 1 — `OnboardingPage.tsx` onError 핸들러 (S-1·S-2·S-3 커버)

```ts
// 현재 (문제)
onError: (err) => {
  setInline(err instanceof ApiError ? err.message : '지금 저장이 어려워요...');
  // pending=false → 버튼 재활성화 → 재제출 가능 → S-4
}

// 변경 후
onError: (err) => {
  if (err instanceof ApiError && err.code === 'LF002') {
    // facade 이미 있음 → 캐시 무효화 후 /map 이동
    void qc.invalidateQueries({ queryKey: LEARNING_FACADE_KEY });
    navigate('/map', { replace: true });
    return;  // 이하 에러 노출 스킵
  }
  setInline(err instanceof ApiError ? err.message : '지금 저장이 어려워요...');
},
```

- `await` 불필요 — navigate 이후 백그라운드 refetch로 충분
- S-1·S-2·S-3 모두 이 분기로 처리됨
- 의존: `qc` = `useQueryClient()`, `LEARNING_FACADE_KEY` 기존 key 재사용

---

### 수정 2 — `/onboarding` ProtectedRoute reverse guard (S-1 완전 차단)

**목표**: facade를 이미 보유한 유저가 `/onboarding` URL 직접 입력 시 `/map`으로 즉시 리다이렉트.

```tsx
// 방법 A — ProtectedRoute에 prop 추가 (권장)
<Route
  path="/onboarding"
  element={
    <ProtectedRoute redirectIfConcept="/map">
      <OnboardingPage />
    </ProtectedRoute>
  }
/>

// ProtectedRoute 내부
if (redirectIfConcept && facade?.concept) {
  return <Navigate to={redirectIfConcept} replace />;
}
```

```tsx
// 방법 B — OnboardingPage 내부에서 처리 (ProtectedRoute 수정 최소화)
const { data: facade } = useLearningFacade();

useEffect(() => {
  if (facade?.concept) {
    navigate('/map', { replace: true });
  }
}, [facade]);
```

**권장**: 방법 A (ProtectedRoute 패턴 일관성). B는 flicker 가능성 있음.

---

### 수정 3 — S-4 버튼 연타 UX (낮음 — 병행 가능)

```ts
// LF002 수신 후 버튼을 재활성화하지 않고 메시지로 전환
onError: (err) => {
  if (err instanceof ApiError && err.code === 'LF002') {
    // 수정 1 로직 (위)
    return;
  }
  // 일반 에러: 버튼 재활성화는 자연스럽게 유지
  // 단, 반복 제출 방지는 mutation.isPending으로 이미 처리됨
  setInline(err instanceof ApiError ? err.message : '지금 저장이 어려워요...');
},
```

수정 1이 LF002를 navigate로 처리하면 버튼 재활성화 자체가 발생하지 않으므로 S-4는 수정 1로 자동 해소됨.

---

## Fix tier

**`feature-story`** — 1 훅(onError) + 1 컴포넌트(ProtectedRoute or OnboardingPage 내부 guard) + 단위 테스트

**테스트 케이스 최소 세트**:
- `onError_LF002_mapNavigate`: LF002 에러 → navigate('/map') + invalidateQueries 호출 확인
- `onError_기타에러_inline노출`: LF002 외 에러 → inline 텍스트 노출 확인
- `ProtectedRoute_facadeExists_redirectToMap`: facade 보유 유저 → /map 리다이렉트

---

## 실행 순서

1. `OnboardingPage.tsx` onError 수정 (수정 1 + 수정 3 동시)
2. ProtectedRoute reverse guard (수정 2)
3. 단위 테스트 3건 작성
4. FE 통합 테스트: 기존 facade 보유 계정으로 /onboarding URL 직접 접근 → /map 리다이렉트 확인

---

## 참조

- FE 에러 시나리오: `./fe.md` Issue 2 (S-1~S-4 원문)
- BE 결정 사실: `./issue2b-facade-multicontext-deck-arc-report.md` §3.3 — "(A) 1 Facade 유지 확정"
- FE 저장소: `C:\study\System_Author\third-tool-fe\untitled`
- ErrorCode LF002: `src/main/java/com/example/thirdtool/Common/Exception/ErrorCode/ErrorCode.java`
