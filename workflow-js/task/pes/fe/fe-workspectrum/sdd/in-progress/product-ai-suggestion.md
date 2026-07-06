# [Product] AI Suggestion FE — 무상태 6-Port 요청 UI

## Product Vision

> **BE 무상태 6-Port(Layer/Axis/ChaptersOutline/ChapterSubtree/SelectionOutline/SelectionSubtree)를 화면에서 각각 버튼 · 다이얼로그 · 결과 카드로 노출하고, Role Catalog 자동 감지 · Fallback 표시 · Rate Limit 안내를 일관 표현한다.**
> Layer/Axis/Roadmap/Selection 도메인 UI(`product-learning-tower.md` FE) 안에서 자연스럽게 소비되는 초안 요청 버튼이 본 Product 의 핵심 UX. 초기 릴리스는 Static Adapter 응답만 소비 (LLM Adapter는 v2).

---

## 🔄 Fix 개정 (2026-07-02) — BE 07-02 pivot 반영

**개정 사유**: BE product-ai-suggestion의 4-Port → 6-Port 확장(이슈 #17), roadmap/selections 개념 명세 프롬프트 embed(이슈 #19), 비용 예산 관찰만 v1(이슈 #20).

### 개정 범위 요약

| 영역 | 이전 (~2026-06) | 개정 후 (2026-07-02) |
|---|---|---|
| Port 수 | 4개 (`Layer`·`Axis`·`Roadmap`·`Selections`) | **6개** (`Layer`·`Axis`·**`ChaptersOutline`**·**`ChapterSubtree`**·**`SelectionOutline`**·**`SelectionSubtree`**) |
| SuggestButton | 4개 (`<LayerSuggestButton>` 등) | **6개** — Roadmap/Selections 각각 outline + subtree 2단계 버튼 |
| Roadmap 초안 결과 UX | `<SuggestionResultDialog port="roadmap">` — ASCII 통짜 결과 표시, 사용자 적용 or 닫기 | **2단계 UX** — outline 결과(챕터 리스트)에서 승인·편집 → subtree 결과(챕터별 body)로 병렬 요청·표시 |
| Selection 초안 결과 UX | 동일하게 통짜 표시 | Roadmap과 동일한 outline → subtree 2단계 |
| Adapter 사용 | Static + LLM 이중 (Cascade fallback) | **v1은 Static만** (릴리스 문서 §Product 5). Fallback 배너·`<ProviderContextIndicator>` 유지하지만 실질 발동 없음 |
| 개념 명세 | 없음 (프롬프트 내부에 embed는 BE만) | **`<ConceptSpecHint>`** — outline 요청 전에 "roadmap = 수렴 / selection = 발산" 안내 (BE 프롬프트와 병행 UX 층) |
| Cost cap | 미정 | **v1 미도입 (이슈 #20)** — `<UsageIndicator>` 관찰용 뱃지만 (선택적, dev 전용) |

### 새 6-Port 시그니처 (FE hook 대응)

```typescript
// 기존
useSuggestLayer(context)      // 유지
useSuggestAxis(context)       // 유지
useSuggestRoadmap(context)    // 폐기 → 2개로 분리
useSuggestSelections(context) // 폐기 → 2개로 분리

// 신규 (2026-07-02)
useSuggestChaptersOutline(context)     // 챕터 title·rationale 리스트만
useSuggestChapterSubtree(context)      // 승인된 챕터별 ASCII 통짜
useSuggestSelectionOutline(context)    // 컨테이너 name + 챕터 outline
useSuggestSelectionSubtree(context)    // Selection 챕터별 body
```

### 새로 필요한 컴포넌트

- `<ChaptersOutlineButton>` — Axis 화면에 배치. 챕터 outline 요청.
- `<ChapterSubtreeButton>` — outline 승인 후 챕터 단위 subtree 요청 (병렬 or 순차).
- `<ChaptersOutlineDialog>` — outline 결과 표시 (챕터 리스트 + 각 챕터 승인/편집/거절 액션).
- `<ChapterSubtreeProgress>` — outline 승인 후 병렬 subtree 요청 진행률 표시.
- `<SelectionOutlineButton>` / `<SelectionSubtreeButton>` — Selection도 동일.
- `<ConceptSpecHint mode="roadmap|selection">` — 요청 전 개념 안내.

### 폐기 컴포넌트

- ~~`<RoadmapSuggestButton>`~~, ~~`<SelectionsSuggestButton>`~~ — outline/subtree 2단계로 분리.
- `<SuggestionResultDialog port="roadmap|selections">` — 유지하되 outline 결과 표시 전용으로 축소, subtree는 별도 dialog.

### v1 out of scope (릴리스 문서 참조)

- LLM Adapter (Vertex AI) — v2 (0.1.1v ~ 2026-09-02 예정)
- Cascade fallback 배너 실질 발동 — Static만이므로 v1 발동 안 함 (컴포넌트는 유지, v2 대비)
- Cost budget cap UX — v1은 관찰용 dev 배지만
- Rate limit 강제 재검토 — v1 유지 (이슈 #20 v2)

---

## 배경 및 문제

- **현재 상황 (As-Is)**
  - 기존 `product-aisuggestion.md` (FE) 는 단발 축·주제 추천 다이얼로그 위주 설계. Layer/Roadmap/Selection 은 개념 자체가 없어 표현 불가.
  - Static Fallback 은 개발자 role 기준 하드코드 데이터만 반환 — 기획자/디자이너/문제해결자 사용자에게 유의미한 fallback 없음.
  - `AxisTopicSuggestButton` 등 폐기 컨셉을 반영한 컴포넌트 잔존.
  - Rate Limit 안내 UI 표준 부재.
- **발생하는 문제**
  - BE product-ai-suggestion (재작성) 이 4-Port 인터페이스로 확장되면 FE 도 각 Port 에 대응하는 버튼·결과 표시가 필요.
  - Role Catalog 확장(#10) 이후 사용자 concepts 에서 role 자동 감지되므로 FE 는 감지 결과를 UX 로 표현.
  - Fallback 발동 시(`suggestionsAvailable=false` 또는 `providerContext="fallback:llm→static"`) 사용자에게 어떻게 알릴지 표준 없음.
- **왜 지금 해결해야 하는가**
  - BE 4-Port 완료 후 FE 대응하지 않으면 화면이 미구현.
  - `product-learning-tower.md` (FE) Epic 3 (Roadmap/Selection UI) 이 본 Product 의 초안 요청 버튼을 참조.
  - `product-ai-interactive-roadmap.md` (FE) 세션 흐름이 본 Product 의 컴포넌트 재사용 예정.

## 목표 (To-Be)

- **6개 SuggestButton 컴포넌트 (2026-07-02 개정, 이슈 #17)**:
  - `<LayerSuggestButton>` — LearningFacadePage / LayersListPage 에 배치 (유지)
  - `<AxisSuggestButton>` — LayerDetailPage 에 배치 (유지)
  - **`<ChaptersOutlineButton>`** — AxisDetailPage/Roadmap 탭. 챕터 outline (title·rationale 리스트) 요청
  - **`<ChapterSubtreeButton>`** — outline 승인 이후 각 챕터 subtree(ASCII 통짜) 병렬 요청
  - **`<SelectionOutlineButton>`** — AxisDetailPage/Selections 탭. 새 Selection 컨테이너 name + 챕터 outline 요청
  - **`<SelectionSubtreeButton>`** — Selection 컨테이너 안 각 챕터 subtree 요청
- **`<SuggestionResultDialog>`** — 결과 표시 공통 컴포넌트. `<ChaptersOutlineDialog>`(outline 리스트 승인) + `<ChapterSubtreeProgress>`(subtree 병렬 진행률) 두 개로 확장.
- **`<RoleBadge>`** — 사용자 concepts 기반 감지된 role 표시 (dev/prod 공통). 배지 → hover 시 근거 설명 tooltip.
- **`<FallbackBanner>`** — `suggestionsAvailable=false` 또는 Cascade 폴백 발동 시 노출. "AI 서비스가 일시 불가합니다. 기본 추천을 표시합니다" 안내.
- **`<ProviderContextIndicator>`** — dev only. 응답 `providerContext` (llm / static / fallback:llm→static) 를 소형 배지로 표시. 실험 관측용.
- **`<RateLimitToast>`** — 429 응답 시 Retry-After 시간을 카운트다운 형태로 안내.
- **Zod 스키마** — Layer/Axis/Roadmap/Selection SuggestionResponse 각각 신설. `AxisTopicSuggestion` 은 삭제(ADR022).
- **엔드포인트 경로**: `POST /api/v1/suggestions/layers|axes|roadmaps|selections`.

## 설계 결정 (Design Decisions)

- **4-Port 를 4개 별도 버튼으로 노출 (vs 단일 통합 버튼)**
  - 각 Port 는 다른 컨텍스트에서 다른 화면 위치에 배치. 통합 버튼은 화면-Port 매핑을 사용자가 판단해야 함.
  - 컴포넌트도 4개로 분리, 그러나 내부 로직 재사용(`<SuggestionResultDialog>`).
- **결과 다이얼로그는 공통** — 4-Port 결과 표시 UX 는 카드 목록 + 액션으로 동일. `<SuggestionResultDialog>` 에 `port="layer|axis|roadmap|selections"` prop.
- **Role Badge 는 상시 표시**
  - 사용자 concepts 를 기반으로 백엔드가 role 을 자동 감지. FE 는 응답의 `providerContext` 에 role 정보 포함 시 표시.
  - Hover tooltip 으로 "왜 이 role 로 감지되었는지" 설명.
- **Fallback 배너는 세션 단위, 여러 번 반복 안 함**
  - `sessionStorage` 에 flag 저장. 세션 안에 첫 fallback 발동 시만 배너.
  - 재발동 시 조용히 결과만 표시.
- **ProviderContext 배지는 dev 전용**
  - `import.meta.env.MODE === 'development'` 조건 렌더.
  - 사용자 혼란 방지 (`llm` vs `static` 의미를 일반 사용자가 몰라도 됨).
- **Rate Limit Toast 는 카운트다운**
  - `Retry-After` 헤더 (기본 60초) 를 초 단위 카운트다운.
  - 카운트다운 종료 후 재시도 버튼 활성.
- **Zod 이관 — 폐기 스키마 삭제 필수**
  - `axisTopicSuggestionSchema`, `topicSuggestionSchema` 등 폐기.
  - 파일명 정리: `src/lib/api/schemas/aiSuggestion.ts` (통합) 또는 Port별 분리 (`layerSuggestion.ts` 등). v1 은 통합 파일.
- **Cascade providerContext 파싱**
  - 응답 `providerContext` 는 `"llm"` / `"static:{role}"` / `"fallback:llm→static:{role}"` / `"suggestionsUnavailable"` 형식.
  - FE 는 첫 세그먼트로 UX 결정.

## 대안 검토 (Alternatives Considered)

### 갈림길 A. 버튼 배치

**Option A — 단일 통합 [AI 제안] 버튼, 클릭 후 kind 선택**
- 거부 이유: 사용자가 매번 kind 를 선택해야 함. 각 화면 컨텍스트가 kind 를 이미 알고 있으므로 잉여.

**Option B (선택) — 4개 별도 버튼, 화면 컨텍스트 맞춤**
- 비용: 컴포넌트 4개.
- 보상: 사용자가 즉시 요청. 화면-Port 자연 매핑.

### 갈림길 B. Fallback 노출 방식

**Option A — 배너 없음, 결과만 표시 (조용한 fallback)**
- 거부 이유: 사용자가 "왜 결과가 뻔한지" 알 수 없음. 신뢰 부패.

**Option B (선택) — 세션 단위 첫 발동 배너 + Provider 배지(dev)**
- 비용: 배너 상태 관리.
- 보상: 사용자에게 정직. 반복 배너 피로도 없음.

**Option C — 매번 배너**
- 거부 이유: 피로도 폭증.

### 갈림길 C. Role Badge 표시

**Option A — 표시 없음**
- 거부 이유: 왜 이런 결과가 나왔는지 사용자가 이해 못 함.

**Option B (선택) — 상시 배지 + hover tooltip**
- 비용: 소형 UI.
- 보상: 결과 근거 투명.

**Option C — 사용자 명시 role 선택 UI 추가**
- 거부 이유: 자동 감지의 장점(무선택 UX) 상실. v2 후보로만 유지.

### 갈림길 D. Rate Limit UX

**Option A — 429 → 일반 에러 토스트**
- 거부 이유: 사용자가 "왜 실패했는지" 인지 못 함.

**Option B (선택) — Retry-After 카운트다운 토스트**
- 비용: 카운트다운 UI + 상태 관리.
- 보상: 사용자가 언제 재시도 가능한지 명확.

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 배치

```
[화면 컨텍스트 → 4-Port 버튼 매핑]

/learning-facade
  └─ <LayerSuggestButton>       (concepts 기반 Layer 추천)

/layers/:layerId
  └─ <AxisSuggestButton>        (Layer 컨텍스트 기반 Axis 추천)

/axes/:axisId?tab=roadmap
  └─ <RoadmapSuggestButton>     (Axis 로드맵 초안)

/axes/:axisId?tab=selections
  └─ <SelectionsSuggestButton>  (Selection 초안 목록)

lib/ai-suggestion/            (features 대신 lib 접두 — 도메인 아니고 재사용 유틸)
├─ components/
│   ├─ LayerSuggestButton.tsx
│   ├─ AxisSuggestButton.tsx
│   ├─ RoadmapSuggestButton.tsx
│   ├─ SelectionsSuggestButton.tsx
│   ├─ SuggestionResultDialog.tsx
│   ├─ SuggestionCard.tsx
│   ├─ RoleBadge.tsx
│   ├─ FallbackBanner.tsx
│   ├─ ProviderContextIndicator.tsx
│   └─ RateLimitToast.tsx
├─ hooks/
│   ├─ useSuggestLayers.ts
│   ├─ useSuggestAxes.ts
│   ├─ useSuggestRoadmap.ts
│   ├─ useSuggestSelections.ts
│   └─ useSuggestionSession.ts   (fallback 배너 상태)
└─ types.ts                       (내부 유틸 타입)

lib/api/schemas/
├─ layerSuggestion.ts
├─ axisSuggestion.ts
├─ roadmapSuggestion.ts
└─ selectionsSuggestion.ts

lib/api/endpoints/
└─ suggestion.ts                  (4 endpoint 통합, 함수별 export)
```

### 핵심 플로우

**1. Layer 추천 요청 → Cascade → 결과 표시**
```
사용자 → LearningFacadePage → [🪄 Layer 추천]
  useSuggestLayers.mutate({ concepts, facadeId })
    POST /suggestions/layers
      응답 200:
        { layers:[...], suggestionsAvailable:true, providerContext:"static:backend-developer" }
      → <SuggestionResultDialog port="layer">
         <RoleBadge role="backend-developer">
         <ProviderContextIndicator ctx="static">  (dev only)
         <SuggestionCard × N>
         [적용 선택 항목] → learning-tower Epic 2 Layer 생성 mutation
```

**2. Fallback 발동 → 배너 + 결과**
```
사용자 → RoadmapSuggestButton 클릭
  useSuggestRoadmap.mutate({ axisId })
    POST /suggestions/roadmaps
      응답 200:
        { roadmapDraft:"...", suggestionsAvailable:true, providerContext:"fallback:llm→static:generic" }
      → useSuggestionSession.notifyFallback(port="roadmap")
         (첫 발동만 배너 노출)
      → <SuggestionResultDialog>
         <FallbackBanner>  (세션 최초)
         <SuggestionCard ...>
```

**3. Rate Limit 발동**
```
사용자 → 분당 11번째 요청
  POST /suggestions/*
    응답 429 { code:"AI_SUGGESTION_RATE_LIMITED" } + Retry-After:47
  → <RateLimitToast countdown={47}>
     "잠시 후 다시 시도해주세요 (47초)"
     카운트다운 종료 → 재시도 버튼 활성
```

**4. 최종 실패 → suggestionsUnavailable**
```
Cascade 최종 실패
  응답 200 { suggestions:[], suggestionsAvailable:false, providerContext:"suggestionsUnavailable" }
  → <SuggestionResultDialog>
     <FallbackBanner variant="unavailable">
     "AI 초안을 만들지 못했습니다. 수동으로 입력해주세요."
     [닫기]
```

### 외부 의존

- **BE `/api/v1/suggestions/*`** — 4-Port 엔드포인트.
- **`product-learning-tower.md` (FE)** — 초안 적용 시 Layer/Axis/Roadmap/Selection mutation 호출.
- **`product-ai-interactive-roadmap.md` (FE)** — 세션 흐름에서 본 Product 의 `useSuggest*` hook 을 재사용.

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | ApiError code | HTTP | 클라이언트 권장 동작 (UX) |
| --- | --- | --- | --- |
| `concepts: []` 요청 | `SUGGESTION_CONCEPTS_REQUIRED` | 400 | 컨셉 입력 유도 (LearningFacadePage 이동) |
| `layerId` 없음 (Axis 추천) | `LAYER_NOT_FOUND` | 404 | 새로고침 유도 |
| `axisId` 없음 (Roadmap/Selection) | `AXIS_NOT_FOUND` | 404 | 새로고침 |
| Rate Limit 초과 | `AI_SUGGESTION_RATE_LIMITED` | 429 | Retry-After 카운트다운 토스트 |
| Provider 인증/설정 실패 (startup) | (없음, 서비스 다운) | — | `<FallbackBanner variant="unavailable">` |
| Cascade 완전 실패 | (없음, 응답 200) | 200 (플래그) | `<FallbackBanner variant="unavailable">` + 수동 입력 안내 |
| 네트워크 오프라인 | (fetch 실패) | — | 오프라인 배너 (전역 정책) |
| Zod 파싱 실패 | (프론트 자체) | — | Sentry 캡처 + 사용자에게 "일시 문제" 배너 |

### 로깅 정책 (FE)

- **항상 기록** (Sentry):
  - Suggestion 요청 성공/실패 (`port`, `providerContext`, `role`, `latency`)
  - Rate Limit 발동
  - Fallback 발동 (`from` → `to`)
- **debug**:
  - 요청 payload (dev)
- **절대 금지**:
  - 사용자 concepts 원문 (부분 마스킹)
  - LLM 응답 상세 (Roadmap/Selection content 는 hash 만)

### 관측 지표

- Suggestion 버튼 클릭 → 결과 표시까지 P75 ≤ 5초
- `suggestionsAvailable=false` 노출 비율 (기대 ≤ 1%)
- Rate Limit 발동율 (기대 ≤ 0.1%)
- 결과 [적용] 클릭율 (제안 품질 신호)

## 롤아웃 / 마이그레이션 (Rollout)

### 전제

- 초기 배포 `provider=static` (BE 정책 준수). LLM 활성화 후 자동 승격.
- Suggestion 버튼은 초기부터 활성 (Static 결과 반환).

### Product 의존성

- **선행 Product**:
  - BE `product-ai-suggestion.md` Epic 1~5 완료
  - FE `product-learning-tower.md` Epic 1~4 완료 (Layer/Axis/Roadmap/Selection UI 존재해야 결과 적용 가능)
- **후행 Product**:
  - FE `product-ai-interactive-roadmap.md` — 세션 흐름에서 본 Product 의 hook 재사용

### Epic·Story 의존성 그래프

```
Epic 1 (Zod 스키마 + hook)
  │
  ▼
Epic 2 (SuggestionResultDialog + 공통 컴포넌트)
  │
  ▼
Epic 3 (4개 SuggestButton + 각 화면 배치)
  │
  ▼
Epic 4 (Fallback UX · Role Badge · Provider 배지)
  │
  ▼
Epic 5 (Rate Limit Toast · 관측)
```

### 환경별 설정 분기

| 항목 | dev (`.env.development`) | prod (`.env.production`) |
| --- | --- | --- |
| `ProviderContextIndicator` | 표시 | 숨김 |
| MSW handlers | 신규 4-Port stub | disabled |
| Suggestion 버튼 debounce | 300ms | 300ms |

## 성공 지표 (KPI)

- Suggestion 버튼 클릭 → 결과 표시 성공률 (fallback 포함) ≥ 99%
- `suggestionsUnavailable` 노출 비율 ≤ 1%
- 결과 [적용] 클릭율 ≥ 40% (제안 품질 신호)
- Rate Limit 오탐(정상 사용자 차단) = 0
- Zod 파싱 실패로 인한 Sentry 이벤트 = 0 (스키마 정합성)

## Scope

**In Scope**:
- 4개 SuggestButton
- SuggestionResultDialog
- RoleBadge / FallbackBanner / ProviderContextIndicator / RateLimitToast
- Zod 스키마 4개
- Suggestion endpoint 함수 4개
- useSuggest* hook 4개
- useSuggestionSession (fallback 상태)

**Out of Scope**:
- RoadmapInteractionSession 상태기계 UI — `product-ai-interactive-roadmap.md`
- 사용자 명시 role 선택 UI — v2
- Suggestion 결과 캐싱 UI — v2
- 결과 다국어 — v1 한국어

## 대상 사용자

- **신규 학습자** — concepts 입력 후 Layer 추천으로 시작.
- **기존 학습자** — Layer/Axis 확장 시 추천 활용.
- **개발자** — ProviderContext 배지로 실험 관측 (dev).
- **`product-ai-interactive-roadmap.md` (FE) 세션** — 본 hook 재사용.

## 연결된 Epic 목록

- [ ] Epic 1: Zod 스키마 4개 + `useSuggest*` hook 4개
- [ ] Epic 2: `<SuggestionResultDialog>` + `<SuggestionCard>` 공통 컴포넌트
- [ ] Epic 3: 4개 SuggestButton + 화면 배치
- [ ] Epic 4: `<FallbackBanner>` + `<RoleBadge>` + `<ProviderContextIndicator>`
- [ ] Epic 5: `<RateLimitToast>` + 관측 · Sentry breadcrumb

## 관련 문서

- **선행 fix 이슈**:
  - `workflow/task/fix/brainstorming/version/0.0.2v/issue-09-ai-suggestion-3layer.md`
  - `issue-10-role-catalog-expansion.md`
- **선행 BE Product**: `workflow/task/pes/workspectrum/sdd/in-progress/product-ai-suggestion.md`
- **선행 FE Product**: `product-learning-tower.md`
- **관련 ADR**:
  - [ADR010](../../../../../docs/adr/ADR010.md) — Fallback 5xx 미노출
  - [ADR022](../../../../../docs/adr/ADR022-learning-tower-terminology.md) — 용어 표준
- **폐기 문서**:
  - 기존 `product-aisuggestion.md` (FE) — 본 파일이 대체 (파일명 대시 통일)

## 열린 질문 (Open Questions)

1. **Role Badge 표시 정책** — 상시 표시 vs 결과 다이얼로그 안에만. v1 은 다이얼로그 내부.
2. **[적용] 액션의 트랜잭션** — 모든 카드 일괄 적용 vs 개별 선택. v1 은 개별 선택 (체크박스).
3. **Fallback 배너 재노출 주기** — 세션 1회 vs 매번 새 새션. v1 은 sessionStorage 한 번.
4. **결과 다이얼로그 크기** — Modal size 정책 (max-w-2xl?). Storybook 검토.
5. **Suggestion 언어 정책** — 백엔드가 한국어 강제. FE 는 그대로 표시.

## 제품 수준 완료 기준 (Product-level DoD)

- [ ] Epic 1~5 완료
- [ ] 4개 SuggestButton 이 각 화면에서 활성
- [ ] Fallback / RoleBadge / ProviderContext / RateLimit UX 통합
- [ ] Storybook 스토리 최소 10건

---

# [Epic 1] Zod 스키마 4개 + `useSuggest*` hook

## Epic 목표

BE 4-Port 응답을 파싱하는 Zod 스키마와 TanStack Query mutation hook 을 정의.

## 배경

컴포넌트 이전 스키마·hook 이 확정되어야 SuggestButton 이 안전한 계약 위에서 자란다.

## 완료 기준

- [ ] Story 4건 완료
- [ ] MSW handler 4개 (Static + LLM 응답 시뮬)

---

## [Story 1-1] `layerSuggestionSchema` + `useSuggestLayers`

### User Story

- As a FE 개발자
- I want Layer 추천 요청/응답 타입을 정의하기를
- so that 하위 컴포넌트가 강 타입으로 소비

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

```ts
// schemas/layerSuggestion.ts
export const layerSuggestionSchema = z.object({
  name: z.string(),
  rationale: z.string(),
  suggestedAxisCount: z.number(),
});

export const layerSuggestionsResponseSchema = z.object({
  layers: z.array(layerSuggestionSchema),
  suggestionsAvailable: z.boolean(),
  providerContext: z.string().optional(),
});

// hooks/useSuggestLayers.ts
export const useSuggestLayers = () => useMutation({
  mutationFn: (req: LayerSuggestionRequest) =>
    api.post('/suggestions/layers', req)
       .then(r => layerSuggestionsResponseSchema.parse(r.data)),
});
```

**핵심 파일**:
- 신규: `src/lib/api/schemas/layerSuggestion.ts`
- 신규: `src/lib/api/endpoints/suggestion.ts` (통합)
- 신규: `src/lib/ai-suggestion/hooks/useSuggestLayers.ts`

### 완료 기준 (AC)

- Given 응답 파싱 / When 정상 / Then 성공
- Given 429 응답 / When mutate.onError / Then 에러 객체에 `Retry-After` 헤더 값 노출

### Definition of Done

- [ ] 스키마 + hook
- [ ] MSW handler
- [ ] Vitest

### 스토리 포인트

1d

### 의존성

- 선행: BE Epic 5 완료
- 후행: Story 1-2

---

## [Story 1-2] `axisSuggestionSchema` + `useSuggestAxes`

### User Story

- As a FE 개발자
- I want Axis 추천 요청/응답 타입을 정의하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

```ts
export const axisSuggestionSchema = z.object({
  name: z.string(),
  rationale: z.string(),
  roadmapDraft: z.string(),
});

export const axisSuggestionsResponseSchema = z.object({
  axes: z.array(axisSuggestionSchema),
  suggestionsAvailable: z.boolean(),
  providerContext: z.string().optional(),
});
```

### 완료 기준 (AC)

- Given 응답 / When 파싱 / Then 성공

### Definition of Done

- [ ] 스키마 + hook + MSW

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 1-1
- 후행: Story 1-3

---

## [Story 1-3] `roadmapSuggestionSchema` + `useSuggestRoadmap`

### User Story

- As a FE 개발자
- I want Roadmap 추천 응답 타입을 정의하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

```ts
export const roadmapSuggestionResponseSchema = z.object({
  roadmapDraft: z.string(),
  suggestionsAvailable: z.boolean(),
  providerContext: z.string().optional(),
});
```

### 완료 기준 (AC)

- Given 응답 / When 파싱 / Then 성공

### Definition of Done

- [ ] 스키마 + hook + MSW

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 1-1
- 후행: Story 1-4

---

## [Story 1-4] `selectionsSuggestionSchema` + `useSuggestSelections`

### User Story

- As a FE 개발자
- I want Selections 추천 응답 타입을 정의하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

```ts
export const selectionSuggestionSchema = z.object({
  name: z.string(),
  content: z.string(),
});

export const selectionsSuggestionsResponseSchema = z.object({
  selections: z.array(selectionSuggestionSchema),
  suggestionsAvailable: z.boolean(),
  providerContext: z.string().optional(),
});
```

### 완료 기준 (AC)

- Given 응답 / When 파싱 / Then 성공

### Definition of Done

- [ ] 스키마 + hook + MSW

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 1-1
- 후행: Epic 2

---

# [Epic 2] `<SuggestionResultDialog>` + `<SuggestionCard>` 공통 컴포넌트

## Epic 목표

4-Port 공통 결과 다이얼로그와 카드 컴포넌트를 정의. 각 Port SuggestButton 이 이 위에서 자란다.

## 완료 기준

- [ ] Story 3건 완료
- [ ] Storybook 5 스토리 (Port × 상태)

---

## [Story 2-1] `<SuggestionResultDialog>` 뼈대

### User Story

- As a FE 개발자
- I want 결과 다이얼로그가 4-Port 공통 뼈대이기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `<SuggestionResultDialog port="layer|axis|roadmap|selections" data onClose onApply>`
- port 에 따라 카드 · 텍스트 렌더 분기 (Roadmap 은 텍스트만 단일)
- 상태: loading / success / error / fallback

**핵심 파일**:
- 신규: `src/lib/ai-suggestion/components/SuggestionResultDialog.tsx`

### 완료 기준 (AC)

- Given port=layer, data / When 렌더 / Then 카드 목록
- Given loading / When / Then Skeleton
- Given error / When / Then 에러 상태

### Definition of Done

- [ ] 다이얼로그 컴포넌트
- [ ] Storybook 4 스토리

### 스토리 포인트

1.5d

### 의존성

- 선행: Epic 1
- 후행: Story 2-2

---

## [Story 2-2] `<SuggestionCard>` — 개별 카드

### User Story

- As a 사용자
- I want 추천 카드 별 rationale 확인 및 [적용] 선택하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `<SuggestionCard title rationale metadata selected onToggle>`
- 체크박스 UI (다건 선택 지원)
- Roadmap 은 `<SuggestionCard preview>` 로 대량 텍스트 축약

### 완료 기준 (AC)

- Given card / When 렌더 / Then 제목·rationale 노출
- Given selected=true / When 체크박스 / Then 시각적 강조

### Definition of Done

- [ ] 컴포넌트
- [ ] Storybook

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 2-1
- 후행: Story 2-3

---

## [Story 2-3] `<SuggestionResultDialog>` — [적용] 액션 통합

### User Story

- As a 학습자
- I want 선택한 카드를 즉시 적용(생성)하기를
- so that 편집 부담을 낮춘다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

`[적용]` 클릭:
- Layer → learning-tower `useLayerMutations().create(name)` 반복
- Axis → `useAxisMutations().create(...)` 반복
- Roadmap → `useAxisRoadmap().set(content)` (단일)
- Selection → `useAxisSelections().add(...)` 반복

### 완료 기준 (AC)

- Given 카드 3개 선택 / When [적용] / Then 3개 mutation → 성공 시 다이얼로그 닫힘
- Given 일부 실패 (예: name 중복) / When / Then 실패 항목 인라인 에러 표시

### Definition of Done

- [ ] 적용 로직
- [ ] 통합 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Story 2-1, 2-2, `product-learning-tower.md` (FE) mutation 사용 가능
- 후행: 없음

---

# [Epic 3] 4개 SuggestButton + 화면 배치

## Epic 목표

각 Port SuggestButton 을 4개 화면 컨텍스트에 배치.

## 완료 기준

- [ ] Story 4건 완료
- [ ] 각 화면에서 버튼 클릭 → 다이얼로그 오픈 → 결과 노출 통합 흐름 검증

---

## [Story 3-1] `<LayerSuggestButton>`

### User Story

- As a 학습자
- I want LearningFacadePage 에서 Layer 초안을 요청하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `<LayerSuggestButton concepts>` — [🪄 Layer 추천]
- 클릭 → `useSuggestLayers.mutate({concepts, facadeId})` → `<SuggestionResultDialog port="layer">`
- 배치: LearningFacadePage 상단, LayersListPage 상단

**핵심 파일**:
- 신규: `src/lib/ai-suggestion/components/LayerSuggestButton.tsx`

### 완료 기준 (AC)

- Given LearningFacadePage / When 클릭 / Then 다이얼로그 오픈

### Definition of Done

- [ ] 컴포넌트
- [ ] 통합 테스트

### 스토리 포인트

0.5d

### 의존성

- 선행: Epic 2, `product-learning-tower.md` FE Epic 1·2
- 후행: 없음

---

## [Story 3-2] `<AxisSuggestButton>`

### User Story

- As a 학습자
- I want LayerDetailPage 에서 Axis 초안을 요청하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `<AxisSuggestButton layerId>` — [🪄 Axis 추천]
- 배치: LayerDetailPage 상단

### 완료 기준 (AC)

- Given LayerDetailPage / When 클릭 / Then 다이얼로그

### Definition of Done

- [ ] 컴포넌트

### 스토리 포인트

0.5d

### 의존성

- 선행: Epic 2, `product-learning-tower.md` FE Epic 2
- 후행: 없음

---

## [Story 3-3] `<RoadmapSuggestButton>`

### User Story

- As a 학습자
- I want AxisDetailPage Roadmap 탭에서 로드맵 초안을 요청하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`, `product-learning-tower.md` FE Epic 3 Story 3-8

- `<RoadmapSuggestButton axisId>`
- 결과 → `<SuggestionResultDialog port="roadmap">` (단일 텍스트)
- [적용] → `useAxisRoadmap().set(content)`

### 완료 기준 (AC)

- Given AxisDetailPage?tab=roadmap / When 클릭 / Then 다이얼로그
- Given [적용] / When / Then 로드맵 갱신

### Definition of Done

- [ ] 컴포넌트 + 통합

### 스토리 포인트

0.5d

### 의존성

- 선행: Epic 2, `product-learning-tower.md` FE Epic 3
- 후행: 없음

---

## [Story 3-4] `<SelectionsSuggestButton>`

### User Story

- As a 학습자
- I want AxisDetailPage Selections 탭에서 사례 초안을 요청하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `<SelectionsSuggestButton axisId>` — [🪄 사례 초안 요청]
- 결과 → `<SuggestionResultDialog port="selections">` (여러 카드)
- [적용] → 선택 카드마다 `useAxisSelections().add(name, content)` 반복
- name 중복 시 실패 항목 인라인

### 완료 기준 (AC)

- Given axis 존재 / When 클릭 / Then 다이얼로그
- Given 3개 선택 [적용] / When / Then 3개 mutation
- Given 1개 중복 실패 / When / Then 실패 항목 인라인

### Definition of Done

- [ ] 컴포넌트 + 통합

### 스토리 포인트

0.5d

### 의존성

- 선행: Epic 2, `product-learning-tower.md` FE Epic 3
- 후행: 없음

---

# [Epic 4] Fallback UX · Role Badge · Provider 배지

## Epic 목표

`suggestionsAvailable=false`, Cascade fallback, role 자동 감지 상태를 시각화.

## 완료 기준

- [ ] Story 4건 완료
- [ ] Storybook 8 스토리

---

## [Story 4-1] `<FallbackBanner>` — 세션 단위 첫 발동

### User Story

- As a 학습자
- I want AI 서비스 문제를 명확하게 안내받기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`, ADR010

- `<FallbackBanner variant="fallback|unavailable">` — 다이얼로그 상단
- fallback: "AI 서비스가 잠시 불안정합니다. 기본 추천을 표시합니다"
- unavailable: "AI 초안을 만들지 못했습니다. 수동으로 입력해주세요"
- sessionStorage 로 세션당 1회 (fallback 한정)
- unavailable 은 매번 표시 (진짜 실패)

**핵심 파일**:
- 신규: `src/lib/ai-suggestion/components/FallbackBanner.tsx`
- 신규: `src/lib/ai-suggestion/hooks/useSuggestionSession.ts`

### 완료 기준 (AC)

- Given fallback 발동 첫 번째 / When 다이얼로그 오픈 / Then 배너
- Given 같은 세션 두 번째 발동 / When / Then 배너 미노출
- Given unavailable / When / Then 매번 배너

### Definition of Done

- [ ] Banner + session hook
- [ ] Vitest 세션 상태 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Epic 2
- 후행: Story 4-2

---

## [Story 4-2] `<RoleBadge>` + Tooltip

### User Story

- As a 학습자
- I want 감지된 role 을 확인하기를
- so that 왜 이런 결과가 나왔는지 이해한다

### 설명

> 출처: `issue-10-role-catalog-expansion.md`

- `<RoleBadge role="backend-developer|planner|designer|problem-solver|generic">` — 소형 배지
- Hover tooltip: "concepts 값 X, Y 기반으로 {role} 감지"
- 색상/아이콘 매핑: developer=파랑, planner=초록, designer=주황, problem-solver=보라, generic=회색

**핵심 파일**:
- 신규: `src/lib/ai-suggestion/components/RoleBadge.tsx`

### 완료 기준 (AC)

- Given role="planner" / When 렌더 / Then 배지 표시
- Given Hover / When / Then tooltip 노출

### Definition of Done

- [ ] Badge + tooltip
- [ ] Storybook 5 스토리

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 4-1
- 후행: Story 4-3

---

## [Story 4-3] `<ProviderContextIndicator>` (dev only)

### User Story

- As a 개발자
- I want 응답 providerContext 를 dev 모드에서 확인하기를
- so that 실험 관측 시 어떤 어댑터가 응답했는지 즉시 알 수 있다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `<ProviderContextIndicator context={ctx}>` — 소형 배지
- 값: `llm` (녹색) / `static:{role}` (회색) / `fallback:llm→static:{role}` (주황)
- `import.meta.env.MODE === 'development'` 조건 렌더

**핵심 파일**:
- 신규: `src/lib/ai-suggestion/components/ProviderContextIndicator.tsx`

### 완료 기준 (AC)

- Given dev + ctx="llm" / When 렌더 / Then 녹색 배지
- Given prod / When / Then null (렌더 안 함)

### Definition of Done

- [ ] Indicator + env 조건 테스트

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 4-1
- 후행: Story 4-4

---

## [Story 4-4] Fallback / Role / Provider 배지 다이얼로그 통합

### User Story

- As a 학습자
- I want 다이얼로그 상단에서 fallback · role · provider 정보를 한눈에 보기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `<SuggestionResultDialog>` 헤더에 3개 컴포넌트 배치
- Layout: FallbackBanner (전체 폭) → RoleBadge · ProviderContextIndicator (인라인 우측 정렬)

### 완료 기준 (AC)

- Given 다이얼로그 오픈 / When / Then 3개 통합 렌더

### Definition of Done

- [ ] 통합
- [ ] Storybook

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 4-1, 4-2, 4-3
- 후행: 없음

---

# [Epic 5] Rate Limit Toast + 관측 · Sentry

## Epic 목표

429 응답 UX 표준화 + Sentry breadcrumb 정착.

## 완료 기준

- [ ] Story 3건 완료

---

## [Story 5-1] `<RateLimitToast>` — 카운트다운

### User Story

- As a 학습자
- I want Rate Limit 초과 시 언제 재시도 가능한지 명확히 알기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `<RateLimitToast retryAfter={sec}>` — Toaster 확장 컴포넌트
- 카운트다운 표시 (`{remaining}초 후 다시 시도할 수 있습니다`)
- 종료 후 `[다시 시도]` 버튼 → 원 요청 재실행 (mutation 재트리거)

**핵심 파일**:
- 신규: `src/lib/ai-suggestion/components/RateLimitToast.tsx`

### 완료 기준 (AC)

- Given 429 응답 / When onError / Then 토스트 노출
- Given 카운트다운 종료 / When / Then [다시 시도] 활성

### Definition of Done

- [ ] Toast + 카운트다운
- [ ] 통합 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Epic 1, 2
- 후행: Story 5-2

---

## [Story 5-2] Sentry breadcrumbs

### User Story

- As a 개발자
- I want Suggestion 관련 이벤트가 Sentry 에 기록되기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- 각 mutation `onMutate` / `onSuccess` / `onError` 에서 breadcrumb 추가
- 카테고리: `suggestion`
- 데이터: `port`, `providerContext`, `role`, `latency_ms`

### 완료 기준 (AC)

- Given Sentry / When mutation 발생 / Then breadcrumb 기록

### Definition of Done

- [ ] Breadcrumb 통합

### 스토리 포인트

0.5d

### 의존성

- 선행: Epic 1
- 후행: Story 5-3

---

## [Story 5-3] Suggestion 사용 로그 훅

### User Story

- As a PM
- I want Suggestion 클릭 → 적용 완료 지표를 관측하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `useSuggestionSession` 에 이벤트 tracker (click / open / apply / dismiss)
- 프로덕션에서 최소 이벤트 수집기(가벼운 로컬 counter → 백엔드 push)

### 완료 기준 (AC)

- Given 클릭 이벤트 / When / Then counter 증가

### Definition of Done

- [ ] Tracker 훅

### 스토리 포인트

0.5d

### 의존성

- 선행: Epic 1
- 후행: 없음

---

*작성일: 2026-07-01 | 상태: **5 Epic · 18 Story 전체 pending** | Milestone: FE milestone 2 (BE ai-suggestion + FE learning-tower Epic 1~4 완료 후). 기존 `product-aisuggestion.md` (FE) 대체 파일.*
