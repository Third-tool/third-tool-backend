# FE M2 / 0.0.2v — Week of 2026-07-01 ~ 2026-07-07 (2차 재편 D2 = 2026-07-02 Thu)

> **마일스톤의 역할**: FE 큰 작업 단위(Product)는 `workflow/task/pes/fe/fe-workspectrum/sdd/in-progress/`에 정의되어 굴러가고 있다. 본 문서는 **이번 버전에 그 Product들에서 얼마만큼을 잡아 갈지의 분배 결정** + **버전 단위 산출물 묶음**.
>
> 한 주 = 한 버전 = `version/0.0.X v/` 폴더 하나. 본 버전(0.0.2v)에는 다음 7 파일이 들어간다:
> - `milestone.md` *(본 문서)* — 잡힌 양 + 일정 + 의존
> - `infra.md` — (본 버전 배포 미포함 사유 명시)
> - `performance.md` — (본 버전 성능 측정 미수행 사유 명시)
> - `outcome.md` — 사용자 경험·기능·기술 자산 변화 (concepts[] + Layer UI + AI/Card/Review FE spec 리팩토링 착지)
> - `cost.md` — FE 관점 비용 성과 (본 버전 로컬 개발 한정 → 비용 발생 0)
> - `review.md` — 회고 + 다음 버전 진입 신호
> - `ux-test.md` — 브라우저 UX 시나리오 검증 (로컬 dev server 한정)

**본 버전의 릴리스 대응**: 본 FE M2에서 착지된 FE spec 5건 (개정 3 + 신설 2)이 **첫 사용자 릴리스(0.1.0v, ~2026-08-19)** UX 원천. 릴리스 스코프는 `workflow/task/milestones/release/version/0.0.1v/release.md` (백엔드와 공유) 참조.

---

## 0.0.2v FE 2차 재편 (D2 pivot, 2026-07-02) — AI/Card/Review 도큐먼트 FE 대응

**07-02 pivot 트리거**: BE M2가 D2에 세 회의(AI Roadmap 회의 · Card+Review 회의 · SDD 양식 재정합)로 대규모 pivot 됨. FE도 동일 시점에 spec 대응 착수. 세부는 `workflow/task/milestones/version/0.0.2v/milestone.md` §0.0.2v 2차 재편 참조.

**FE 07-02 pivot 산출물 요약**:

| 산출물 | 수량 | 위치 |
|---|---|---|
| FE Product spec 개정 (🔄 Fix 개정 섹션 삽입 + inline 갱신) | 3건 | `product-learning-tower.md` (FE) · `product-ai-suggestion.md` (FE) · `product-ai-interactive-roadmap.md` (FE) |
| FE Product spec 신설 (SDD 최소 양식 준수, 15섹션 + Epic outline) | 2건 | `product-card.md` (FE) · `product-review.md` (FE) |

**FE 개정·신설 핵심 정합**:

| BE 변경 (07-02) | FE 대응 (07-02) |
|---|---|
| Roadmap/Selection **노드 first-class 승격** (이슈 #15/#16, Coarse + body TEXT ASCII 통짜) | `<AxisRoadmapEditor>` textarea 폐기 → **`<RoadmapNodeList>` + `<RoadmapNodeCard>`** 카드 리스트 (learning-tower FE) |
| AI **4-Port → 6-Port** 확장 (이슈 #17, outline + subtree 2단계) | `<RoadmapSuggestButton>` 폐기 → **`<ChaptersOutlineButton>` + `<ChapterSubtreeButton>`** 신설 (ai-suggestion FE) |
| roadmap/selections 개념 명세 프롬프트 embed (이슈 #19) | **`<ConceptSpecTooltip>`** 신설 — 편집 진입 시 수렴/발산 안내 (learning-tower FE) |
| Session 상태 머신 **4상태 → 7상태** 확장 (이슈 #17) | Wizard 라우팅 **7단계 확장** (`.../layers` `.../axes` `.../chapters` `.../subtrees` 신규) + `<SessionStateStepper>` 확장 (ai-interactive-roadmap FE) |
| Card lifecycle **M3 하이브리드** + Mode enum 재편 (이슈 #21/#23) | **`product-card.md` (FE) 신설** — `<ModeSelector>` 4옵션 · `<CardScheduleBadge>` · `<ModeChangeConfirmDialog>` |
| Daily Batch + Cross-layer + 대시보드 (이슈 #24/#25/#26) | **`product-review.md` (FE) 신설** — `<DailyBatchLandingPage>` · `<ReviewScopeToggle>` 폐기 · 대시보드 v1 minimal |

**정책 재확정 (D2 pivot 이후)**:
- **본 FE M2의 실제 산출물은 FE spec 5건 (개정 3 + 신설 2)이 주력**. 원안 Tier 1 11 Story (concepts[] · Layer · 4-Port Zod + hook)의 실 구현은 대부분 M3 이관 (기존 계획 유지, 시점만 밀림).
- **BE와 동일한 속도감 유지**: BE도 D2 pivot 후 실제 구현은 M3 이후 이관. FE도 동일 리듬.
- **원안 Tier 1 미완주분은 M3에 이관 + FE 노드 UI Zod (이슈 #15/#16 대응) 병행 착수**.
- **6-Port로 재편된 AI hook 스켈레톤 (기존 4-Port 대체)** — M3에 재정의 후 M4~M5에 완주.

---

## 0.0.2v FE 스코프 메모 (mid-week 재편, 2026-07-01)

원래 FE M2는 "AI Suggestion 통합 UI + Lighthouse 개선 + backend-boundary/error-codes 갱신"이 후보였으나, **백엔드 M2가 배포·LLM 실제 활성화·성능 baseline 3축에서 도메인 리팩토링(concepts[] + Layer + AI Port 골격) 3축으로 전면 재편**됨. FE도 동일 시점에 스코프 재편.

이번 FE M2의 축:

1. **BE 도메인 첫 조각 착지 대응 (concepts[] + Layer)** — BE Learning Tower Epic 1·2가 로컬 API로 붙는 시점에 맞춰 FE의 `<ConceptsInput>`·`<LayersListPage>`·`<LayerFormDialog>` 착지. MSW handler로 BE 대기 없이 병렬 진행.
2. **AI 4-Port 계약 잠금 (Zod + hook 스켈레톤)** — BE가 Port 인터페이스+record만 확정하는 시점에 FE의 Zod schema 4종 + `useSuggest*` hook 4종을 동일 시그니처로 정착. BE Adapter가 붙는 다음 버전에 즉시 호출 가능.
3. **용어 정합 (ADR023 FE 반영)** — Roadmap/Selection 어휘가 사용자 정의(헌법/판례)로 확정. FE의 라벨·설명·i18n 문구가 이 어휘를 따르도록 grep 후 정합.

**본 버전 제외 사유**:
- **원안 3축(AI Controller 통합 UI · Lighthouse · error-codes 갱신)** → BE Controller 노출이 다음 버전. Lighthouse는 배포 재개 시점.
- **AxisDetailPage 재구성 · Roadmap/Selection UI (FE LT Epic 3)** → BE LT Epic 3(Roadmap/Selection 이원 축)가 M3 이후.
- **Deck 폐기 FE · Card→Axis FE · Review scope 토글** → BE M3 이후 클러스터에 대응.
- **RoadmapInteractionSession UI** → BE product-ai-interactive-roadmap이 M3 이후.
- **product-fe-cdn 배포 재개** → BE 배포 재개 시점 동조.

---

## 백엔드 M2 (0.0.2v) 참조

FE M2는 백엔드 M2와 동일 주간이며, 다음 백엔드 산출물이 FE의 이번 주 진입점이다.

**BE M2 스코프 (FE 관점 요약)** — 참조: `workflow/task/milestones/version/0.0.2v/milestone.md`

| BE 항목 | 상태 | FE 진입점 |
| --- | --- | --- |
| BE LT Epic 1 (concepts[] 다중화 · 5 Story) | 본주 진행 | FE는 `learningFacadeSchema` `concepts: string[]` 스위치 + `<ConceptsInput>` Chip Input 준비 |
| BE LT Epic 2 (Layer 서버 도메인 · 5 Story) | 본주 진행 | FE는 `layerSchema` 신설 + `<LayersListPage>` + `<LayerFormDialog>` + `useLayers()` hook |
| BE AS Epic 1 (4-Port 인터페이스 + record) | 본주 진행 | FE는 4-Port Zod schema + `useSuggest*()` hook 4종 스켈레톤 (MSW로 stub 응답) |
| BE ADR023 (Roadmap = 헌법 / Selection = 판례 용어 확정) | 본주 진행 | FE 라벨·i18n·주석에서 `roadmap`/`selection` 어휘 정합 grep |
| BE LT Epic 3~6 (Roadmap/Selection · Card→Axis · Deck 폐기 · Review 재편) | **M3 이후** | 본주 FE 대응 없음. `<AxisDetailPage>` 재구성·Card 편집기 axis 직접 선택은 유예 |
| BE AS Epic 2~5 (Static Adapter 4종 · Role Catalog · LLM Adapter · Cascade) | **M3 이후** | 본주 FE 대응 없음. `<SuggestionResultDialog>`는 Tier 2(스켈레톤)만 |
| BE 배포 라인 (product-infra-deploy·network·ops) | **다음 배포 재개 마일스톤** | 본주 fe-cdn 배포 미포함. `<LighthouseCI>` 유예 |

**BE M2 D2 pivot 추가 이슈 (07-02) — FE spec 대응**

| BE 이슈 (07-02 신설) | 상태 | FE 대응 (07-02 spec 착지) |
|---|---|---|
| BE 이슈 #15 (Roadmap 노드 first-class 승격) | 명세 확정 · 구현 M3~ | `product-learning-tower.md` (FE) 🔄 개정 — `<AxisRoadmapEditor>` textarea 폐기, `<RoadmapNodeList>` + `<RoadmapNodeCard>` 명세 삽입 |
| BE 이슈 #16 (Selection 노드 컨테이너 + 자식) | 명세 확정 · 구현 M3~ | 동일 파일 — 컨테이너 정책 유지 + `<SelectionNodeList>` 명세 |
| BE 이슈 #17 (4-Port → 6-Port, outline + subtree 2단계) | 명세 확정 · 구현 M4~M5 | `product-ai-suggestion.md` (FE) 🔄 개정 — 6개 SuggestButton + `<ChaptersOutlineDialog>` + `<ChapterSubtreeProgress>` 명세 |
| BE 이슈 #17 (세션 상태 머신 확장) | 명세 확정 · 구현 M7 | `product-ai-interactive-roadmap.md` (FE) 🔄 개정 — Wizard 7단계 확장 명세 |
| BE 이슈 #18 (챕터 재생성 API + hint) | 릴리스 v2 이관 | FE 대응 v2 (`<RegenerateChapterButton>` + hint 입력) |
| BE 이슈 #19 (roadmap/selections 개념 명세 embed) | 명세 확정 · 구현 M3~ | `product-learning-tower.md` (FE) — `<ConceptSpecTooltip>` 명세 삽입 |
| BE 이슈 #20 (AI 비용 예산 cap) | v1 미구현 backlog | v1 관찰용 dev 배지만 (`<UsageIndicator>`) |
| BE 이슈 #21 (Mode enum 재편 MODE_7D/14D/28D/60D) | 명세 확정 · 구현 M4 | **`product-card.md` (FE) 신설** — `<ModeSelector>` 4옵션 · `<RawInputDaysMappingHint>` 명세 |
| BE 이슈 #22 (OnFieldBudget 폐기) | 명세 확정 · 구현 M4 | 동일 파일 — `<ArchiveReasonBadge>` 3-reason (`MANUAL/SCHEDULE_EXHAUSTED/MODE_DOWNGRADED`) 명세 |
| BE 이슈 #23 (Card createdMode + M3 하이브리드) | 명세 확정 · 구현 M4 | 동일 파일 — `<CardScheduleBadge>` (createdMode + effectiveMax) · `<ModeChangeConfirmDialog>` · `<ReturnToFieldConfirmDialog>` 명세 |
| BE 이슈 #24 (DailyLearningBatch Aggregate) | 명세 확정 · 구현 M5 | **`product-review.md` (FE) 신설** — `<DailyBatchLandingPage>` · `<DailyBatchProgress>` · `<CardQueueList>` · `<BatchClosedBanner>` 명세 |
| BE 이슈 #25 (ReviewSession cross-layer 재편) | 명세 확정 · 구현 M5 | 동일 파일 — `<ReviewScopeToggle>` 완전 폐기 명세 · `<AutoFinishNoticeToast>` 명세 |
| BE 이슈 #26 (캐시 측정 대시보드 L3) | 명세 확정 · 구현 M6 (v1은 L2까지만) | 동일 파일 — 대시보드 v1 minimal (Today · Recent7 · Streak) 명세 · L3 추천 배지는 v2 이관 |

**BE M2 Story 중 FE 계약 정합 영향** — 참조: BE Story 4 (`LearningFacadeUpdateRequest`의 `concept` 단일 필드 → `concepts: string[]` 스위치)

| BE Story | 영향 | FE 대응 |
| --- | --- | --- |
| BE LT 1-4 API/DTO `concepts: string[]` 스위치 | FE의 `PUT /facades/me` 요청 payload에 `concept` 단일값 있으면 400 가능 | `lib/api/schemas/learningFacade.ts`·`features/learning-facade/`·MSW handler에서 `concept` 단일 참조 grep + `concepts` 배열로 이관 |
| BE LT 2-5 Layer Controller (`/facades/me/layers/*`) | FE fetch base URL 확정 | `lib/api/endpoints/layer.ts` 신설 + 5 엔드포인트 정합 |
| BE AS Epic 1 4-Port record 확정 | FE Zod schema 시그니처가 BE record와 정합해야 함 | BE Story 11 머지 직후 FE Zod 시그니처 조회 · 정합 확인 |

---

## 진행 중 FE Product 잔여 인벤토리 (M2 재편 시점)

| FE Product | 상태 | 잔여 UX 작업 | M2 대상 |
| --- | --- | --- | --- |
| product-card (`done/`) | done | 계약 정합 유지 | — |
| product-learningFacade (`done/`) | done | 계약 정합 유지 · concepts[] 스위치 반영 확인 | **작은 정합 작업 1건** (concept 단일 grep) |
| **product-learning-tower** (`in-progress/`, 신설) | in-progress | Epic 1~6 전체 | **9 Story** (Epic 1 · 1-1~1-5 + Epic 2 · 2-1~2-4) |
| **product-ai-suggestion** (`in-progress/`) | in-progress | Epic 1~5 전체 | **1 Story** (Epic 1 · 4-Port Zod + hook 스켈레톤) |
| product-ai-interactive-roadmap (`in-progress/`) | in-progress | 세션 상태 UI 전체 | — (BE M3 이후) |
| Product.md (User FE, `in-progress/`) | in-progress | BC 리팩토링 대응 | — (BE User Product M1 수준 유지) |
| product-auth (`in-progress/`) | in-progress | MSW 401 handler 확장 | — |
| product-fe-cdn (`in-progress/`) | in-progress | 배포 워크플로우 | — (본 버전 배포 미포함) |
| product-media / search | in-progress / backlog | BE 진입 대기 | — |
| **합계** | | | **10 Story (Tier 1) + 5 Story (Tier 2)** |

> **신설 신호**: `product-learning-tower.md`가 FE 사이드에도 신설됨(BE와 동기). Epic 1(concepts[]) + Epic 2(Layer) 착수.
> **재구조 신호**: `product-ai-suggestion.md`가 4-Port 아키텍처 반영. FE Epic 1(Zod+hook)만 본주.

---

## 본주 잡힌 양 (FE M2 재편 — 15 Story)

### Tier 1 · Must (M2 합격선 — 10 Story)

| # | Product | Story | 한 줄 | SP |
| --- | --- | --- | --- | --- |
| 1 | LT | **Epic 1 / 1-1** `learningFacadeSchema` Zod 이관: `concept:string` → `concepts:z.array(string).min(1).max(5)` | 스키마 갱신 + Type export + 단수 참조 grep | 1 |
| 2 | LT | **Epic 1 / 1-2** `<ConceptsInput>` Chip Input 컴포넌트 | 추가 · 삭제 · 중복 방지 · 5개 상한 시 안내 + 접근성(role/aria) | 2 |
| 3 | LT | **Epic 1 / 1-3** `<ConceptsEditPage>` + `useConceptsMutation()` PATCH 훅 | TanStack Query + `PUT /facades/me` payload `concepts:[...]` + optimistic update | 2 |
| 4 | LT | **Epic 1 / 1-4** `<LearningFacadePage>` 헤더 `<ConceptsBar>` (read-only chip 목록) | 기존 concept 단일 표시 자리에 chip 목록 마운트 + 편집 진입 | 1 |
| 5 | LT | **Epic 1 / 1-5** 단수 `concept` fallback grep · 제거 + MSW handler 정합 | `features/**`, `lib/api/**` grep → `concept` 단일 참조 0건 + MSW handler payload 이관 | 1 |
| 6 | LT | **Epic 2 / 2-1** `lib/api/schemas/layer.ts` Zod + `useLayers()` hook | Zod(id/name/displayOrder/deletedAt) + fetch(GET /facades/me/layers) + TanStack Query | 1 |
| 7 | LT | **Epic 2 / 2-2** `<LayersListPage>` + [Layer 추가] 버튼 진입 | Layer 목록 카드 UI + 진입 트리거 (버튼) + MSW handler 신규 | 2 |
| 8 | LT | **Epic 2 / 2-3** `<LayerFormDialog>` — Layer 생성/수정 폼 | 이름 필수(1~50자) + 중복 검증 (BE UNIQUE 응답 시 인라인 에러) + MSW handler | 2 |
| 9 | LT | **Epic 2 / 2-4** `<LayerConfirmDeleteDialog>` — softDelete + 하위 axis 존재 시 안내 | 2단계 confirm + 하위 axis N개일 때 정책 문구 + MSW handler | 1 |
| 10 | AS | **Epic 1** 4-Port Zod schema 4종 + `useSuggest*()` hook 4종 스켈레톤 | `layerSuggestionSchema` · `axisSuggestionSchema` · `roadmapSuggestionSchema` · `selectionsSuggestionSchema` + 각 hook + MSW stub 응답 | 3 |
| 11 | 문서 | **FE-ADR** ADR023 대응 — 라벨·i18n 어휘 정합 (roadmap=헌법, selection=판례) | FE 어휘 grep → 라벨·툴팁·주석 정합 + `FE-ADR-CANDIDATES.md` P1로 신설 검토 | 1 |

**Tier 1 합계: 11 Story · ~17 SP**

### Tier 2 · Want (도전 — 5 Story)

| # | Product | Story | 한 줄 | SP |
| --- | --- | --- | --- | --- |
| 12 | LT | **Epic 2 / 2-5** `<LayerReorderList>` dnd-kit drag-and-drop reorder | Axis 순서 재배치 UI (BE PUT /facades/me/layers/order 호출) + dnd-kit 의존 도입 | 2 |
| 13 | AS | **Epic 2 / 2-1** `<SuggestionResultDialog>` 뼈대 (Port별 렌더 분기) | 4-Port 응답 공통 다이얼로그 shell (실제 렌더는 다음 버전) + MSW stub 확인 | 2 |
| 14 | LT (done 정합) | 문구 grep 실체화 (concept 단일 · deck 노출) | `src/features/**` deck 어휘 · concept 단일 grep 결과 CI job 등록 | 1 |
| 15 | 문서 | 0.0.2v `infra.md`·`performance.md` 스킵 사유 + `outcome.md`·`cost.md`·`review.md`·`ux-test.md` 골격 | 배포·측정 미포함 명시 + 종료 신호 판정 기록지 준비 | 1 |

**Tier 2 합계: 4 Story · ~6 SP**

### 카테고리별 합계

| 카테고리 | Story 수 | SP | 비중 |
| --- | --- | --- | --- |
| FE 도메인 (LT Epic 1 concepts[]) | 5 | 7 | 30% |
| FE 도메인 (LT Epic 2 Layer) | 4 | 6 | 26% |
| AI Port 계약 (AS Epic 1) | 1 | 3 | 13% |
| FE-ADR / 어휘 정합 | 1 | 1 | 4% |
| **Tier 1 소계** | **11** | **17** | **73%** |
| Tier 2 (Layer reorder · Suggestion shell · CI grep · 산출물 골격) | 4 | 6 | 27% |
| **합계** | **15** | **23** | 100% |

**분배 근거**:
- BE M2와 동일하게 SP 절반(~23) 목표. 도메인 UI는 컴포넌트 · Zod · MSW handler 3중 정합이라 SP 총량을 낮추고 완성도를 높인다.
- **FE 도메인 56% + AI 계약 13% + 문서 4% + Want 27%** — BE의 3축(도메인·AI 계약·어휘)에 그대로 정합.
- **AI Port Zod만 Tier 1**: BE Port record 시그니처와 정합. Dialog·Button 등 실제 렌더는 다음 버전.
- **MSW handler 신규 다수**: BE 실제 로컬 API 대기 없이 병렬 진행 가능. BE 붙는 시점에 handler 제거 또는 dev/mock 분기.

---

## 종료 신호 — "로컬 dev server에서 concepts[] Chip + Layer CRUD가 그림처럼 돈다"

본주 종료 시점에 다음이 모두 성립해야 한다. (Tier 1 기준, 8 신호 중 6개 이상 → 0.0.2v 동결)

- [ ] **머지 신호**: Tier 1 11 Story 중 최소 9 머지 (82%)
- [ ] **concepts[] 신호**: 로컬 `pnpm dev`에서 `<LearningFacadePage>` 진입 시 `<ConceptsBar>` chip 3개 렌더링 + `<ConceptsEditPage>`에서 추가/삭제/재배치 후 `PUT /facades/me` payload에 `concepts:["A","B","C"]` 확인 (DevTools Network)
- [ ] **Layer UI 신호**: 로컬에서 `<LayersListPage>` 진입 시 default "Uncategorized" Layer + 사용자 추가 Layer 목록 렌더 + `<LayerFormDialog>` 생성/수정 flow가 정상 close/reload
- [ ] **MSW 신호**: MSW handler 신규 (concepts PATCH · Layer 5 엔드포인트 · 4-Port suggestion stub)가 Vitest에서 각각 1회 이상 히트 + BUILD SUCCESSFUL
- [ ] **계약 정합 신호**: `PUT /facades/me` 요청 payload에 `concept` 단일 필드 grep 0건 + `layerSchema` Zod 시그니처가 BE Response DTO와 필드 일치
- [ ] **AI 계약 신호**: 4개 `useSuggest*()` hook이 각각 Zod parse 성공 → 응답 타입 export → 컴포넌트에서 임포트 가능 (실제 렌더 미포함 OK)
- [ ] **어휘 정합 신호**: `src/**` 어휘 grep에서 `roadmap`을 "초안"으로 지칭하는 라벨·주석 0건. `selection`을 "합의최종안"으로 지칭하는 라벨·주석 0건 (사용자 정의 헌법/판례 어휘로 통일)
- [ ] **테스트 신호**: `pnpm test` (Vitest)에서 concepts[] 컴포넌트 · Layer 컴포넌트 · Suggestion hook 각각 해피/엣지/예외 케이스 최소 1건씩 통과

**Tier 2 추가 신호** (Want, 미달 시 M2 판정에 영향 없음):
- [ ] **Layer reorder 신호**: dnd-kit 도입 후 `<LayerReorderList>`에서 드래그 → `PUT /facades/me/layers/order` 호출 → MSW handler에서 새 순서 반영 확인
- [ ] **Suggestion shell 신호**: `<SuggestionResultDialog>`가 4-Port stub 응답으로 open/close 정상 (내용 렌더는 다음 버전)

**미합격 처리**: 위 8 신호 중 6개 미만 성립 시 FE M2를 0.0.2v로 동결하지 않고 0.0.2.1v 패치 발행 → 다음주 초까지 연장.

---

## 의존 chain

```
[BE 대기 없이 병렬 시작 가능 — D1부터]
FE-1 (learningFacadeSchema Zod 이관) ── FE-5 (단수 concept grep 제거)
                                         │
FE-6 (layer.ts Zod + useLayers hook) ────┤
                                         │
FE-10 (4-Port Zod + hook 스켈레톤) ──────┤
                                         │
FE-11 (ADR023 어휘 정합 grep) ───────────┘

[FE-1 완료 후]
FE-1 ── FE-2 (ConceptsInput Chip) ── FE-3 (ConceptsEditPage + useConceptsMutation)
                                            │
                                            └── FE-4 (LearningFacadePage 헤더 ConceptsBar)

[FE-6 완료 후]
FE-6 ── FE-7 (LayersListPage) ── FE-8 (LayerFormDialog) ── FE-9 (LayerConfirmDeleteDialog)

[Tier 2 — Tier 1 완료 후]
FE-9 ── FE-12 (Layer reorder dnd-kit)
FE-10 ── FE-13 (SuggestionResultDialog shell)
FE-14 (grep CI job — 독립)
FE-15 (산출물 골격 — D7)

[BE 의존 관계 — 병렬이지만 BE 붙기 전엔 MSW로 대체]
FE Story ⇄ BE Story
FE-1/2/3/4/5 ⇄ BE 1~5 (concepts[]) — BE 미완이어도 MSW로 진행
FE-6/7/8/9 ⇄ BE 6~10 (Layer) — 동일
FE-10 ⇄ BE 11 (4-Port 인터페이스) — BE record 확정 후 시그니처 정합 확인 권장
FE-11 ⇄ BE 12 (ADR023) — BE ADR 초안 확정 후 어휘 정합
```

**병렬 진입 가능 묶음** (BE 실제 API 대기 없이 D1부터 시작):
- **A** (D1): FE-1 (concepts Zod) + FE-6 (layer Zod + hook) + FE-10 (4-Port Zod) + FE-11 (어휘 grep) 동시 착수
- **B** (D2-D3): FE-2 (ConceptsInput) + FE-7 (LayersListPage) 병렬. FE-5 (단수 concept 제거) 진행 중
- **C** (D4-D5): FE-3 (ConceptsEditPage) + FE-8 (LayerFormDialog) + FE-4 (ConceptsBar) + FE-9 (ConfirmDeleteDialog)
- **D** (D6): 통합 로컬 검증 (dev server + Vitest + MSW handler 히트 확인) + Tier 2 진입
- **E** (D7): FE-15 (문서) + 0.0.2v 동결 판정

**직렬 (M2 합격선까지)**: A → B → C 두 병렬 chain(concepts / Layer) → D 통합 검증 → E 동결.

---

## 작업 일정 (체크리스트)

재편 D1 = 2026-07-01 (수). 종료 D7 = 2026-07-07 (화). 7일 안에 15 Story. BE M2와 동일 주기.

| 일 | 날짜 | 잡힌 작업 (FE) |
| --- | --- | --- |
| D1 (수) | 07-01 | **독립 착수**: FE-1 (concepts Zod) + FE-6 (layer Zod + hook) + FE-10 (4-Port Zod + hook 스켈레톤) + FE-11 (어휘 grep) / **BE 대기**: BE Story 1·6·11 (MSW handler로 대체 시작) |
| D2 (목) | 07-02 | **컴포넌트**: FE-1 머지 + FE-2 (ConceptsInput Chip) + FE-6 머지 + FE-7 (LayersListPage) 시작 / **정합**: FE-5 (단수 concept grep) 진행 |
| D3 (금) | 07-03 | **컴포넌트**: FE-2 머지 + FE-3 (ConceptsEditPage + useConceptsMutation) + FE-7 머지 + FE-8 (LayerFormDialog) 시작 |
| D4 (토) | 07-04 | **컴포넌트**: FE-3 머지 + FE-4 (ConceptsBar) + FE-8 머지 + FE-9 (ConfirmDeleteDialog) 시작 |
| D5 (일) | 07-05 | **컴포넌트**: FE-4 머지 + FE-9 머지 + FE-5 머지 / **AI 계약**: FE-10 머지 (BE Story 11 record 시그니처 정합 확인) / **Want**: FE-12 (Layer reorder dnd-kit) 시작 |
| D6 (월) | 07-06 | **검증**: 통합 로컬 검증 (dev server + Vitest + MSW handler 히트 확인) + FE-11 머지 / **Want**: FE-13 (SuggestionResultDialog shell) + FE-14 (grep CI job) |
| D7 (화) | 07-07 | **마무리**: FE-15 (`infra.md` 배포 미포함 사유 + `performance.md` 측정 미수행 사유 + `outcome.md`·`cost.md`·`review.md`·`ux-test.md` 골격) + 0.0.2v 동결 판정 |

> **Tier 2 처리 규칙**: D5-D6에 여력 남으면 진입. Tier 1 11개 통과가 우선. Tier 2 미완은 다음 버전으로 이관.

> **BE 의존 관찰**: BE Story 6·7·8 (Layer 테이블·FK 재배선·default 백필)의 마이그레이션 순서 결정에 따라 FE Zod schema의 `deletedAt`·`facadeId` 필드 표기가 바뀔 수 있음. D2 종료 시 BE V버전 배치 확인.

---

## 리스크와 관찰 포인트

| 영역 | 리스크 | 관찰 포인트 |
| --- | --- | --- |
| BE 실제 API 대기 | BE Story 5(1-5 검증)와 Story 10(Layer Controller) 머지 지연 시 MSW handler 응답과 실제 응답 간 drift 위험 | D5 종료 시점에 BE Story 5·10 머지 확인 → 미완 시 MSW handler를 실제 응답 기준으로 재정렬 |
| Zod schema drift | BE record 시그니처가 최종화 전까지 FE Zod가 앞서 확정될 위험 | FE-10 (4-Port Zod) 착수 시 BE Story 11 record 스켈레톤 대기 · D3 종료 시 시그니처 정합 재확인 |
| MSW handler 폭증 | Layer 5 엔드포인트 + concepts PATCH + 4-Port stub → handler 파일 관리 부담 | `mocks/handlers/` 분할 (learning-facade / layer / suggestion / user) 유지. 각 파일당 100라인 미만 |
| dnd-kit 도입 (Tier 2) | dnd-kit 의존 신규 도입 시 번들 사이즈 증가 + FE-ADR 결정 필요 | Tier 2 진입 시 `FE-ADR-CANDIDATES.md` P1-2 항목 초안 병행. 미결정 시 Tier 2에서 제외 |
| 어휘 반전 | BE ADR023 확정 전 FE 어휘 grep 진행하면 재작업 발생 | FE-11 (어휘 grep)은 BE Story 12(ADR023) 초안 확정 후 D2-D3에 진행 |
| 컴포넌트·MSW handler·Zod 3중 정합 | 세 축이 함께 움직이는 Story에서 한 축이 어긋나면 dev server 부팅 오류 | 매 Story별로 Vitest 1회+ dev server 부팅 1회 확인. PR 병합 전 3중 정합 체크리스트 |
| SP 여유 부족 | Tier 1 11건이 촘촘함. 컴포넌트 접근성(a11y) 요구 추가 시 슬립 위험 | D3 종료 시점에 6개 이상 머지 진행률 (55%) 확인. 미달 시 D5부터 Tier 2 진입 중단 |
| 배포 검증 부재 | FE M1의 배포 검증(도메인·CORS·CF)이 재개되지 않음 → 사용 실체 확인 불가 | 본 버전은 dev server(`localhost:3000` 또는 `5173`)에서 UX 확인만. 다음 배포 재개 버전에서 재점검 |

---

## 다음 마일스톤 (FE M3 / 0.0.3v) 후보 — 07-02 pivot 반영판

07-02 pivot으로 후속 마일스톤 전면 재구성. **BE 마일스톤(M3~M8)과 정확히 동일 주기**로 착지 · **첫 릴리스(0.1.0v, ~08-19)를 6주 뒤로 잡고** 그 사이 M3~M8을 순차 진행. BE 릴리스 스코프 (`release/version/0.0.1v/release.md`)와 정합.

**FE M3 / 0.0.3v (07-08 ~ 07-14) — 원안 Tier 1 미완주 + 노드 UI Zod 착수**
- **원안 Tier 1 잔여** (FE M2 미완주분): FE-2/3/4/5 (concepts[] Chip UI 완주) + FE-7/8/9 (Layer CRUD 완주) + FE-10 (기존 4-Port Zod는 폐기, **6-Port Zod로 재편** 착수)
- **이슈 #15/#16 대응 Zod + Node UI 스켈레톤**: `axisRoadmapNodeSchema` · `axisSelectionSchema` + `axisSelectionNodeSchema` 신설. `<RoadmapNodeList>` · `<RoadmapNodeCard>` · `<SelectionContainerList>` · `<SelectionNodeCard>` 컴포넌트 shell.
- **`<ConceptSpecTooltip>`** (이슈 #19) 신설 — 개념 명세 안내 UI.
- **BE M3 완주 대응**: LT Epic 1·2 완주 + 이슈 #15/#16 이관 착수와 동기.

**FE M4 / 0.0.4v (07-15 ~ 07-21) — product-card.md (FE) 완주**
- **product-card.md (FE) Epic 1·2·3 완주**: `<ModeSelector>` 4옵션 · `<CardScheduleBadge>` (createdMode + effectiveMax) · `<ArchiveReasonBadge>` 3-reason · `<ModeChangeConfirmDialog>` · `<ReturnToFieldConfirmDialog>`.
- **Zod 스키마 재편**: `learningMode.ts` 4개 enum · `card.ts` `createdMode` 필드 · `archiveReason.ts` 3개 enum.
- **BE M4 대응**: 이슈 #21/#22/#23 이관.

**FE M5 / 0.0.5v (07-22 ~ 07-28) — product-review.md (FE) Epic 1·2 완주**
- **product-review.md (FE) Epic 1**: `<DailyBatchLandingPage>` · `<DailyBatchProgress>` · `<CardQueueList>` · `<BatchClosedBanner>` · `useDailyBatch()` hook.
- **product-review.md (FE) Epic 2**: `<ReviewSessionPage>` 재편 · `<CardReviewSequence>` (RECALLING → COMPARING 유지) · `<AutoFinishNoticeToast>` · `<ReviewScopeToggle>` 완전 삭제 · `/decks/*` 리디렉트.
- **AI 6-Port `useSuggest*()` hook 완주** (M3 스켈레톤 → M5 완전 활성): `useSuggestChaptersOutline` · `useSuggestChapterSubtree` · `useSuggestSelectionOutline` · `useSuggestSelectionSubtree`.
- **BE M5 대응**: 이슈 #24/#25 이관 + AI Static Adapter 완주.

**FE M6 / 0.0.6v (07-29 ~ 08-04) — 대시보드 v1 + Roadmap/Selection 노드 UI 완주**
- **product-review.md (FE) Epic 3**: 대시보드 v1 minimal (`<TodayCompletionCard>` · `<Recent7DaysCard>` · `<CurrentStreakCard>`) · `useLearningDashboard()` hook.
- **learning-tower FE의 노드 UI 완주**: `<RoadmapNodeList>` 완전 활성 · `<SelectionContainerList>` + 자식 노드 · `<NodeBodyEditor>` (ASCII 트리 monospace textarea) · 노드 순서변경(drag-and-drop, dnd-kit 도입 결정 이후).
- **BE M6 대응**: 이슈 #19 (프롬프트 embed) + 이슈 #26 (대시보드) + LLM Adapter 도입 (FE엔 영향 최소 — 응답 형식 동일).

**FE M7 / 0.0.7v (08-05 ~ 08-11) — 배포 재개 + Wizard 7단계 완주**
- **product-fe-cdn 배포 재개**: S3 + CloudFront + Route53 + GHA · `VITE_API_BASE_URL` prod 스위치.
- **product-ai-interactive-roadmap.md (FE) Epic 1·2 완주**: `<RoadmapSessionWizard>` 7단계 (`.../init` `.../layers` `.../axes` `.../chapters` `.../subtrees` `.../review` `.../commit`) · `<SessionStateStepper>` 확장 · `<ChapterOutlineList>` · `<ChapterSubtreeGrid>`.
- **Web Vitals baseline 측정**: LCP · INP · CLS 3개 지표 프로덕션 측정 시작.
- **BE M7 대응**: 배포 라인 완주 + 관측 baseline.

**FE M8 / 0.0.8v (08-12 ~ 08-18) — 릴리스 대비 UX 테스트 + fix 이슈 소진**
- **UX 테스트 3명 사용자 시나리오 5개** (릴리스 문서 §사전 검증 시나리오 참조):
  - 시나리오 1: 첫 진입 · 학습 대상 정의
  - 시나리오 2: 카드 저장 · 다음날 노출
  - 시나리오 3: Mode 다운 · load 즉시 감소
  - 시나리오 4: Cross-layer 짬뽕
  - 시나리오 5: Streak · 대시보드
- **fix 이슈 잔여 소진**: 5주간 관찰된 세부 이슈 처리.
- **Lighthouse CI 도입**: PR별 Performance/Accessibility/Best Practices/SEO 4카테고리 자동 측정.
- **릴리스 파이프라인 dry-run** (BE와 동기).
- **→ 첫 릴리스 0.1.0v 발행 준비** (2026-08-19, BE와 동시).

**릴리스 제외 (0.1.0v v1 out of scope, FE 관점)**:
- `<LayerProgressBadge>` (이슈 #14 Layer 진행률 UI) — v2 (layer 시각화가 v1 out)
- L3 규칙 기반 추천 배지 (`<RecommendationBadge>`) · T3 주간 요약 알림 UI (`<WeeklySummaryModal>`) — v2 (0.2.0v)
- Notification 폴링·WebSocket 인프라 — v2
- 챕터 노드 재생성 UI (`<RegenerateChapterButton>` + hint 입력) — v2 (0.1.1v)
- 대시보드 30일 그래프·차트 — v2
- 카드 검색 (`product-search.md` FE) · 미디어 업로드 세부 UX (`product-media.md` FE) — v2 이후

---

## Product 상태 전환 신호 (FE M2 종료 시) — 07-02 pivot 반영

- `in-progress/product-learning-tower.md` (FE) — **🔄 Fix 개정 섹션 삽입 완료** (2026-07-02). Zod/컴포넌트 노드 first-class 재편 명세. 실 구현은 M3 이후 (원안 Tier 1 미완주 + 노드 UI Zod)
- `in-progress/product-ai-suggestion.md` (FE) — **🔄 Fix 개정 섹션 삽입 완료** (2026-07-02). 4-Port → 6-Port 확장 반영. `useSuggest*` hook 스켈레톤은 6-Port로 재편해서 M3에 착수
- `in-progress/product-ai-interactive-roadmap.md` (FE) — **🔄 Fix 개정 섹션 삽입 완료** (2026-07-02). Wizard 7단계 확장 명세. 실 구현은 M7 이관
- `in-progress/product-card.md` (FE) — **신설 완료** (2026-07-02). SDD 15섹션 + 3 Epic outline. 실 구현은 M4 이관
- `in-progress/product-review.md` (FE) — **신설 완료** (2026-07-02). SDD 15섹션 + 3 Epic outline. 실 구현은 M5~M6 이관
- `done/product-learningFacade.md` — 정합 유지 (concept 단일 → concepts[] 이관 반영, M3 완주 시 최종)
- `in-progress/product-fe-cdn.md` — 상태 유지 (배포 재개는 M7)
- 원안 M2 Tier 1 Story 인벤토리 — FE-1 스키마 이관 D1에 시작, FE-2~5 · FE-7~9 · FE-10 (6-Port로 재편)는 M3에 이관

---

## brainstorming 트리거 — 07-02 pivot 반영

본 FE M2 완료 후 `workflow/task/pes/brainstorming/0.0.3v/` (신설 예정)에 FE 관점 후보 상태 전이 반영:

- **promoted**: FE spec 3개 개정 + 2개 신설 → 5개 spec 착지 완료 (D2 pivot 산출물)
- **신규 후보**: 노드 UI ↔ 통짜 body TEXT ASCII 편집기 조합 UX (`brainstorming/0.0.3v/node-ui.md`) — 노드 카드 리스트 + 각 노드 body ASCII 편집기 인터랙션 (드래그, 순서변경, 편집 저장 타이밍)
- **신규 후보**: 6-Port outline → subtree 2단계 요청 UX (`brainstorming/0.0.3v/two-stage-ai.md`) — outline 승인 후 subtree 병렬 진행률 표시, 부분 실패 처리, 재시도
- **신규 후보**: `<ConceptSpecTooltip>` UX (`brainstorming/0.0.3v/concept-tooltip.md`) — 수렴/발산 개념 안내 최초 진입 시 동선
- **신규 후보**: Wizard 7단계 확장 시각화 (`brainstorming/0.0.3v/wizard-stepper.md`) — 대분류 4~5단계 breadcrumb + 하위 상세 스텝
- **신규 후보**: Mode down 예상 archive 사전 계산 UX (`brainstorming/0.0.3v/mode-change.md`) — dry-run API 도입 여부, "N개 카드가 자동 보관됩니다" 문구 처리
- **신규 후보**: Daily batch Landing UX (`brainstorming/0.0.3v/daily-landing.md`) — 첫 화면 대안 (카드 리스트 vs Landing vs 대시보드), Batch 크기 0장일 때 CTA
- **신규 후보**: 자정 close banner 표시 정책 (`brainstorming/0.0.3v/batch-closed.md`) — sessionStorage 닫기 vs 하루 한 번 표시
- **신규 후보**: Streak 시각화 (`brainstorming/0.0.3v/streak-visual.md`) — 숫자만 vs 캘린더 히트맵 vs 원형 progress
- **신규 후보**: dnd-kit 도입 FE-ADR (노드 순서변경에 실체 도입 결정) — M6에 필요
- **신규 후보**: BE Zod schema drift 탐지 자동화 (BE record → FE Zod 대조 CI) — 07-02 pivot으로 스키마 재편 규모 커짐, 자동화 우선순위 상승
- **신규 후보**: 릴리스(0.1.0v) 대비 UX 테스트 3명 시나리오 확정 (`brainstorming/0.0.3v/ux-test-scenarios.md`) — release.md §사전 검증 시나리오 5개 계승

---

## 참고

- 잔여 FE Story 인벤토리 출처: `workflow/task/pes/fe/fe-workspectrum/sdd/in-progress/` **10개** Product 파일 (learning-tower · ai-suggestion · ai-interactive-roadmap 3종 개정 반영 + **product-card · product-review 2종 신설**)
- 재편 근거:
  - 07-01 1차 재편: BE 마일스톤 재편(`workflow/task/milestones/version/0.0.2v/milestone.md`) + `workflow/task/fix/brainstorming/version/0.0.2v/issue-04 ~ 14` 11건
  - **07-02 2차 재편**: BE D2 pivot + `workflow/task/fix/brainstorming/version/0.0.2v/issue-15 ~ 26` **12건 신설**
- FE 마일스톤 패키지 의도: `workflow/task/pes/fe/fe-milestones/references/001.md`
- 백엔드 대응 마일스톤: `workflow/task/milestones/version/0.0.2v/milestone.md`
- **첫 릴리스 계획 (BE와 공유)**: `workflow/task/milestones/release/version/0.0.1v/release.md` — 0.1.0v (~2026-08-19) 스코프·성공 기준·사전 검증 시나리오 5개
- 본 버전의 산출물 6종: `infra.md`(스킵 사유만), `performance.md`(스킵 사유만), `outcome.md`, `cost.md`, `review.md`, `ux-test.md`
- 양식 진화: 본 milestone은 0.0.1v FE의 milestone.md 양식을 그대로 답습. 양식 변경이 필요하면 별도 FE-ADR로 결정
- 원안 FE M2(AI Controller 통합 UI + Lighthouse + error-codes 갱신)의 후보 목록은 배포 재개 마일스톤(FE M7 / 08-05~08-11)에서 참조용으로 사용 가능
