# [Product] AI Interactive Roadmap FE — 세션 상태기계 UI + Selection 정책

## Product Vision

> **`RoadmapInteractionSession` 의 `INIT → LAYERS_DRAFTED → AXES_DRAFTED → CHAPTERS_DRAFTED → SUBTREES_DRAFTED → REVIEWING → COMMITTED / ABANDONED` 상태를 화면으로 노출하고, outline → subtree 2단계 초안 흐름을 사용자 액션으로 정련해 최종 저장한다.**
> `product-ai-suggestion.md` (FE) 의 6-Port hook 을 세션 흐름에서 재사용하고, `AxisSelection` 컨테이너 정책(name UNIQUE · DESC · in-place update · hard delete confirm) + 노드 CRUD 정책을 UX 로 강제한다.

---

## 🔄 Fix 개정 (2026-07-02) — BE 07-02 pivot 반영

**개정 사유**: BE product-ai-interactive-roadmap의 세션 상태 머신 확장(이슈 #17), `AxisDraftItem` 구조 변경 (roadmapDraft 통짜 → chapters 배열), 저장 매핑 변경 (`axis.setRoadmap` → `axis.addRoadmapNode` 반복 호출).

### 개정 범위 요약

| 영역 | 이전 (~2026-06) | 개정 후 (2026-07-02) |
|---|---|---|
| 세션 상태 머신 | `INIT → DRAFTED → REVIEWING → COMMITTED` (4상태) | **`INIT → LAYERS_DRAFTED → AXES_DRAFTED → CHAPTERS_DRAFTED → SUBTREES_DRAFTED → REVIEWING → COMMITTED`** (7상태) |
| `<SessionStateStepper>` | 4단계 시각화 | **7단계 시각화** (또는 대분류 4단계 + 하위 단계) |
| `AxisDraftItem` 구조 | `{name, rationale, roadmapDraft: string}` — 통짜 문자열 | **`{name, rationale, chapters: ChapterDraft[]}`** — 챕터 outline + subtree 배열 |
| `<AxisDraftPanel>` | axisDraft 카드 목록 + refresh option | 유지하되 **각 카드가 챕터 outline 리스트 + subtree 진행률** 표시 |
| `<RoadmapDraftEditor>` | 인라인 통짜 편집 | **`<ChapterOutlineList>` + `<ChapterSubtreeEditor>`** 2단계 편집 |
| `<SelectionDraftList>` | 컨테이너 리스트 통짜 | 컨테이너 리스트 + 각 컨테이너 안 노드 리스트 |
| Wizard 라우팅 | `.../init` `.../draft` `.../review` `.../commit` 4단계 | **`.../init` `.../layers` `.../axes` `.../chapters` `.../subtrees` `.../review` `.../commit`** 7단계 |
| 저장 결과 표시 | "추가된 axis N건 + roadmap M건 + selection K건" | "추가된 axis N건 + 챕터 노드 M건 + Selection 컨테이너 K건 × 노드 L건" |

### 새 세션 상태 머신 (Wizard 스텝)

```
INIT
  → LAYERS_DRAFTED       (LayerSuggestionPort 결과)
  → AXES_DRAFTED         (AxisSuggestionPort 결과, layer별)
  → CHAPTERS_DRAFTED     (ChaptersOutlinePort 결과, 챕터 title·rationale 리스트)
  → SUBTREES_DRAFTED     (ChapterSubtreePort 병렬 결과, 챕터별 bodyAsciiTree)
  → REVIEWING            (사용자 액션 keep/remove/rename/add/reorder + 챕터 재생성)
  → COMMITTED            (LearningFacade에 챕터 노드 row 반복 저장)
  → ABANDONED
```

### 새로 필요한 컴포넌트

- `<ChapterOutlineList>` — CHAPTERS_DRAFTED 단계에서 챕터 outline 리스트 표시 + 승인·편집·거절 + `<ChapterCountHintInput>` (사용자가 챕터 수 힌트 주입)
- `<ChapterSubtreeGrid>` — SUBTREES_DRAFTED 단계에서 챕터별 subtree 병렬 요청 진행률 + 완료된 body 편집기
- `<SessionStepBreadcrumb>` — 7단계 확장에 대응하는 대분류 breadcrumb (Layer / Axis / Roadmap / Selection / Review / Commit)

### v1 out of scope (릴리스 문서 참조)

- 챕터 노드 재생성 API + hint UI (이슈 #18) — v2 (0.1.1v)
- Selection 컨테이너 자동 생성 UI — v1은 수동 생성만
- Advanced 세션 상태 (다중 axis 병렬 draft 등) — v2

---

## 배경 및 문제

- **현재 상황 (As-Is)**
  - 기존 `product-ai-interactive-roadmap.md` (FE) 는 `STARTED/AXIS_DRAFTED/TOPIC_REFINING/READY_TO_SAVE/SAVED/ABANDONED` 상태를 참조.
  - `AxisTopic` 관련 diff/consensus UI 가 광범위. Roadmap/Selection dual-axis 구조 미반영.
  - Selection 정책(#11) UI 미정.
  - `axisDraft`/`axisConsensus` 신 용어(ADR022) 미반영.
- **발생하는 문제**
  - BE product-ai-interactive-roadmap 재작성 반영 없이 진행하면 스키마 mismatch 폭발.
  - 사용자 액션(keep/remove/rename/add/reorder) 이후 결과 시각화 표준 부재.
  - 저장 시 결정형 검증 실패(`AXIS_CONSENSUS_EMPTY` 등) UX 미정.
- **왜 지금 해결해야 하는가**
  - `product-learning-tower.md` (FE) Epic 1~4 완료 후 저장 대상(Layer/Axis/Roadmap/Selection) UI 가 존재하는 상태.
  - `product-ai-suggestion.md` (FE) 4-Port 완료 후 hook 재사용 가능.
  - 재작성 없이 진행하면 milestone 3 (본 Product) 진입 불가.

## 목표 (To-Be)

- **`<RoadmapSessionWizard>`** — 세션 상태기계 7단계를 스텝퍼로 노출 (2026-07-02 개정, 이슈 #17):
  - INIT → 컨텍스트 입력 (facadeId)
  - LAYERS_DRAFTED → Layer 후보 리스트 표시 + 승인
  - AXES_DRAFTED → 승인된 Layer 하위 Axis 후보 리스트 + 승인
  - **CHAPTERS_DRAFTED** → Axis별 챕터 outline 리스트 + `<ChapterOutlineList>` 편집
  - **SUBTREES_DRAFTED** → 승인된 챕터별 subtree 병렬 요청 결과 + `<ChapterSubtreeGrid>` 편집
  - REVIEWING → 사용자 액션 (keep/remove/rename/add/reorder) 반영 확인
  - COMMITTED → 저장 결과 확인
- **`<SessionStateStepper>`** — 7단계 시각화 (또는 대분류 4~5단계 breadcrumb).
- **`<AxisDraftPanel>`** — axisDraft 카드 목록 + refresh context input. 각 카드에 챕터 outline·subtree 진행률.
- **`<UserActionBar>`** — keep / remove / rename / add / reorder 액션 버튼.
- **`<ChapterOutlineList>`** (신설) — CHAPTERS_DRAFTED 단계, 챕터 title·rationale 편집 + 챕터 수 hint 주입.
- **`<ChapterSubtreeGrid>`** (신설) — SUBTREES_DRAFTED 단계, 챕터별 subtree 병렬 요청 진행률 + body 편집기.
- **`<SelectionDraftList>`** — Selection 컨테이너 리스트 (name UNIQUE 검증) + 각 컨테이너 안 노드 리스트.
- **`<DraftChangeLogPanel>`** — 사용자 액션 시간순 로그.
- **`<ConsensusValidationBanner>`** — commit 진입 전 결정형 검증 결과 노출 (blank/duplicate/order).
- **`<CommitResultPage>`** — 저장 완료 후 결과 (추가된 axis N건 + 챕터 노드 M건 + Selection 컨테이너 K건 × 노드 L건) 표시.
- **ADR022 용어 준수** — `axisDraft` / `axisConsensus` 표기 (UI 문구는 "축 초안" / "축 합의").
- **`product-ai-suggestion.md` (FE) 6-Port hook 재사용** — `useSuggestLayer` / `useSuggestAxis` / `useSuggestChaptersOutline` / `useSuggestChapterSubtree` / `useSuggestSelectionOutline` / `useSuggestSelectionSubtree` 를 세션 흐름 내 호출.

## 설계 결정 (Design Decisions)

- **Session Wizard 는 다단계 라우팅 (하위 URL) — 7단계 확장 (2026-07-02 개정)**
  - `/roadmap-sessions/:id/init` / `.../layers` / `.../axes` / `.../chapters` / `.../subtrees` / `.../review` / `.../commit`
  - 브라우저 뒤로가기 자연스러움. 각 단계 URL 공유 가능.
  - 사용자 이탈 후 재진입 시 세션의 현재 state로 자동 라우팅.
- **상태 전이는 서버 계약 준수** — 클라이언트가 임의 전이 금지.
- **User Actions 는 낙관적 UI + 서버 검증**
  - 액션 클릭 시 즉시 로컬 draft 업데이트 (UX 즉시 반응).
  - 백엔드 응답 도착 후 서버 상태로 재정합.
  - 실패 시 로컬 롤백 + 인라인 에러.
- **DraftChangeLog 는 사이드 패널** — 우측 사이드바로 항상 노출.
- **axisDraft refresh 는 명시적 액션만**
  - 자동 재요청 금지. `<RefreshContextInput>` + [다시 초안 받기] 명시 클릭.
  - refreshContext 필드 값 유지.
- **Selection 정책 UI 반영**
  - draft 단계에서 selection name 중복 시도 → 인라인 에러.
  - hard delete confirm 다이얼로그 필수.
- **Commit 전 결정형 검증 우선**
  - `<ConsensusValidationBanner>` 로 검증 결과 표시.
  - 실패 시 [Commit] 버튼 비활성 + 이유 인라인.
- **Cascade fallback** — `product-ai-suggestion.md` (FE) 의 `<FallbackBanner>` 재사용.
- **동기 응답 유지** — 스트리밍/SSE 미도입.

## 대안 검토 (Alternatives Considered)

### 갈림길 A. Wizard 라우팅

**Option A — 단일 URL + 내부 상태**
- 거부 이유: 뒤로가기 부자연. 상태별 딥링크 불가.

**Option B (선택) — 다단계 URL**
- 비용: 라우트 4개.
- 보상: 뒤로가기 자연. 재진입 시 상태 라우팅 자동.

### 갈림길 B. User Actions UX

**Option A — 서버 응답 대기 후 로컬 업데이트 (비관적 UI)**
- 거부 이유: 클릭 → 반응 지연 UX 부담.

**Option B (선택) — 낙관적 UI + 서버 재정합**
- 비용: 실패 시 롤백 처리.
- 보상: 즉시 반응. TanStack Query optimistic update 표준.

### 갈림길 C. DraftChangeLog 노출

**Option A — 별도 페이지 / 모달**
- 거부 이유: 변경 이력이 세션 흐름의 핵심. 별도 이동은 컨텍스트 상실.

**Option B (선택) — 사이드 패널 (상시 노출)**
- 비용: 화면 폭 부담.
- 보상: 이력 즉시 확인.

### 갈림길 D. Commit 실패 UX

**Option A — 서버 응답 후 에러 토스트**
- 거부 이유: 사용자가 어떤 규칙 위반인지 명확히 모름.

**Option B (선택) — Commit 전 결정형 검증 + Banner**
- 비용: 클라이언트 검증 로직.
- 보상: 실패 이유 명확. [Commit] 상태로 즉시 판단.

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 / 라우트 배치

```
[Router]
  /roadmap-sessions/new                              → RoadmapSessionInitPage
  /roadmap-sessions/:sessionId                       → RoadmapSessionWizard (자동 상태 → 하위 라우팅)
  /roadmap-sessions/:sessionId/draft                 → AxisDraftPage (INIT → DRAFTED)
  /roadmap-sessions/:sessionId/review                → RoadmapReviewPage (REVIEWING)
  /roadmap-sessions/:sessionId/commit                → RoadmapCommitPage (COMMITTED)
  /roadmap-sessions/:sessionId/abandoned             → SessionAbandonedPage

features/roadmap-sessions/
├─ RoadmapSessionWizard.tsx
├─ RoadmapSessionInitPage.tsx
├─ AxisDraftPage.tsx
├─ RoadmapReviewPage.tsx
├─ RoadmapCommitPage.tsx
├─ SessionAbandonedPage.tsx
├─ components/
│   ├─ SessionStateStepper.tsx
│   ├─ AxisDraftPanel.tsx
│   ├─ AxisDraftCard.tsx
│   ├─ RefreshContextInput.tsx
│   ├─ UserActionBar.tsx
│   ├─ RoadmapDraftEditor.tsx
│   ├─ SelectionDraftList.tsx
│   ├─ SelectionDraftForm.tsx
│   ├─ SelectionDraftDeleteConfirm.tsx
│   ├─ DraftChangeLogPanel.tsx
│   ├─ ConsensusValidationBanner.tsx
│   ├─ CommitButton.tsx
│   └─ CommitResultPanel.tsx
└─ hooks/
    ├─ useRoadmapSession.ts
    ├─ useAxisDraftMutation.ts
    ├─ useUserActionMutation.ts
    ├─ useRoadmapDraftMutation.ts
    ├─ useSelectionDraftMutation.ts
    ├─ useConsensusValidation.ts
    └─ useCommitMutation.ts

lib/api/schemas/
├─ roadmapSession.ts               (신규)
├─ axisDraftSnapshot.ts            (신규)
├─ axisConsensusSnapshot.ts        (신규)
├─ draftChangeLog.ts               (신규)
└─ userAction.ts                   (신규, keep/remove/rename/add/reorder)

lib/api/endpoints/
└─ roadmapSession.ts               (신규, 통합 파일)
```

### 핵심 플로우

**1. 세션 시작 → axisDraft 생성**
```
사용자 → /roadmap-sessions/new
  <RoadmapSessionInitPage>
     facadeId + layerId 선택
     [세션 시작] 클릭
       POST /roadmap-sessions {facadeId, layerId}
         응답 { sessionId, state:"INIT", expiresAt }
       → Router push /roadmap-sessions/{id}/draft
  <AxisDraftPage>
     [초안 요청] 클릭
       POST /{id}/axis-drafts
         응답 { axisDraft:{axes:[...]}, state:"DRAFTED" }
       → <AxisDraftPanel> 렌더
       → [사용자 액션 시작] → /review
```

**2. 사용자 액션 반복 (낙관적 UI)**
```
<RoadmapReviewPage>
  <UserActionBar> 액션 선택 (예: rename)
    Optimistic: draft.axes 로컬 갱신
    PATCH /{id}/axes { action:"rename", target:{from,to} }
      응답 { updatedDraft, changeLog, state:"REVIEWING" }
    → <AxisDraftPanel> 서버 상태로 재정합
    → <DraftChangeLogPanel> 로그 추가
```

**3. Roadmap draft (Selection 정책 강제)**
```
<AxisDraftCard axis={X}> [Roadmap 초안 요청]
  POST /{id}/roadmap-drafts { axisName:"X" }
    응답 { draft.axes[X].roadmapDraft:"content" }
  → <RoadmapDraftEditor> content 인라인 편집

<AxisDraftCard axis={X}> [+ Selection 초안]
  <SelectionDraftForm> name + content
    submit → 로컬 name 중복 검증 → PATCH /{id}/axes/{X}/selection-drafts
      성공 → 목록 추가
      실패 409 → 인라인 "다른 이름으로 시도"

<SelectionDraftCard> [삭제]
  <SelectionDraftDeleteConfirm> "복원 불가" 안내
  확인 → DELETE
```

**4. Consensus 검증 → Commit**
```
사용자 → [Commit 검토]
  useConsensusValidation() 로컬 검증
    axes.length >= 1
    axes 이름 blank 없음
    axes 이름 중복 없음
  → <ConsensusValidationBanner>
    성공 → [Commit] 활성
    실패 → 실패 이유 인라인 + [Commit] 비활성

[Commit] 클릭
  POST /{id}/commit
    응답 { committedAt, addedAxisIds, roadmapCount, selectionCount, state:"COMMITTED" }
  → Router push /roadmap-sessions/{id}/commit
  → <CommitResultPanel> 결과 표시
  → [완료] → /layers/{layerId}
```

**5. 세션 만료 / 중단**
```
사용자 → 30분 후 재진입
  GET /roadmap-sessions/{id}
    응답 410 SESSION_EXPIRED
  → Router push /roadmap-sessions/new
  → 안내 토스트 "세션이 만료되었습니다"

사용자 → [세션 중단]
  DELETE /roadmap-sessions/{id}
  → Router push /layers
```

### 외부 의존

- **BE `/api/v1/roadmap-sessions/*`**
- **`product-ai-suggestion.md` (FE)** — `useSuggestAxes` / `useSuggestRoadmap` / `useSuggestSelections` hook 재사용
- **`product-learning-tower.md` (FE)** — Layer/Axis/Roadmap/Selection mutation 은 commit 트랜잭션에서 백엔드가 담당 (FE 는 결과만 확인)

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | ApiError code | HTTP | 클라이언트 권장 동작 (UX) |
| --- | --- | --- | --- |
| 세션 TTL 만료 | `SESSION_EXPIRED` | 410 | 새 세션 시작 안내 + 라우팅 |
| 이미 COMMITTED 세션 재사용 | `SESSION_ALREADY_COMMITTED` | 409 | `/layers` 로 라우팅 |
| ABANDONED 세션 접근 | `SESSION_ABANDONED` | 410 | 새 세션 안내 |
| DRAFTED 상태에서 잘못된 전이 | `SESSION_STATE_TRANSITION_INVALID` | 400 | 새로고침 (상태 재로딩) |
| axisDraft 없음 + action 시도 | `SESSION_DRAFT_REQUIRED` | 400 | [초안 요청] 강조 |
| Roadmap 없이 Selection 요청 | `AXIS_ROADMAP_REQUIRED` | 400 | "Roadmap 을 먼저 초안화하세요" |
| Consensus axes 0건 | `AXIS_CONSENSUS_EMPTY` | 400 | Banner "최소 1개 축이 필요합니다" |
| Consensus 이름 중복 | `AXIS_CONSENSUS_NAME_DUPLICATE` | 400 | Banner "축 이름 중복" |
| Commit 트랜잭션 실패 (동시 Layer 삭제 등) | `LAYER_NOT_FOUND` | 404 | 다이얼로그 "Layer 가 삭제됨. 새 세션 시작" |
| Cascade 완전 실패 (AI 응답 불가) | (없음) | 200 (플래그) | `<FallbackBanner variant="unavailable">` + 수동 입력 흐름 |
| 다른 유저 세션 접근 | `SESSION_FORBIDDEN` | 403 | 로그아웃/재로그인 유도 |
| Layer 미선택 세션 시도 | `SESSION_LAYER_REQUIRED` | 400 | Layer 선택 UI |

### 로깅 정책 (FE)

- **항상 기록** (Sentry):
  - 세션 시작·종료·중단
  - 상태 전이 (from → to)
  - Commit 성공/실패 (addedAxisCount, addedRoadmapCount)
  - `<ConsensusValidationBanner>` 발생 (실패 이유)
- **debug**:
  - 사용자 액션 상세 (dev)
- **절대 금지**:
  - axisDraft content 본문
  - Roadmap/Selection content 본문

### 관측 지표

- 세션 시작 → COMMITTED 도달률 (기대 ≥ 40%)
- 세션 abandon 비율 (기대 ≤ 30%)
- 사용자 액션 개수 P50 (기대 ≥ 3)
- Commit 성공률 (검증 통과 후)
- Fallback 발동 → 세션 완주율

## 롤아웃 / 마이그레이션 (Rollout)

### 전제

- BE product-ai-interactive-roadmap 완료 후 진입.
- FE product-learning-tower · product-ai-suggestion 완료 후 진입.
- 초기 배포 provider=static (BE 정책).

### Product 의존성

- **선행 Product**:
  - BE `product-ai-interactive-roadmap.md` Epic 1~5
  - FE `product-learning-tower.md` Epic 1~4 (Layer/Axis/Roadmap/Selection UI 존재)
  - FE `product-ai-suggestion.md` Epic 1~5 (4-Port hook 재사용)
- **후행 Product**: 없음

### Epic·Story 의존성 그래프

```
Epic 1 (Zod + hook · Session 조회 · 상태 라우팅)
  │
  ▼
Epic 2 (Session 시작 + axisDraft 생성)
  │
  ▼
Epic 3 (User Actions + DraftChangeLog + 낙관적 UI)
  │
  ▼
Epic 4 (Roadmap/Selection draft · Selection 정책 강제)
  │
  ▼
Epic 5 (Consensus 검증 + Commit + 결과)
```

### 환경별 설정 분기

| 항목 | dev | prod |
| --- | --- | --- |
| 세션 TTL 로컬 카운트다운 | 30분 | 30분 |
| MSW handler | 신규 다수 | disabled |
| optimistic UI | enabled | enabled |

## 성공 지표 (KPI)

- Session 시작 성공률 = 100% (API 성공 시)
- Session commit 도달률 ≥ 40%
- 사용자 액션 개수 P50 ≥ 3
- Consensus 검증 실패로 인한 [Commit] 재시도율 (UX 학습)
- Fallback → 완주 도달률 (AI 실패에도 사용자 진행 가능한지)
- 세션 상태 라우팅 정확도 = 100% (사용자가 재진입 시 올바른 페이지)

## Scope

**In Scope**:
- `<RoadmapSessionWizard>` + 4 페이지
- `<SessionStateStepper>` / `<AxisDraftPanel>` / `<UserActionBar>` / `<RoadmapDraftEditor>` / `<SelectionDraftList>` / `<DraftChangeLogPanel>` / `<ConsensusValidationBanner>`
- 낙관적 UI + 서버 재정합
- Selection 정책 (name UNIQUE + hard delete confirm)
- Zod 스키마 5개
- `product-ai-suggestion.md` (FE) 4-Port hook 재사용

**Out of Scope**:
- 세션 diff 시각화 (draft vs consensus) — v2 (본 Product 는 텍스트 로그만)
- 세션 A/B 실험 UI — v2
- 실시간 협업 세션 (다중 사용자) — 범위 초과
- Streaming/SSE 응답 — 동기 유지

## 대상 사용자

- **신규 학습자** — AI 초안을 비판·수정해 자신의 지도 완성.
- **기존 학습자** — 특정 Layer 확장 시 세션 흐름 사용.

## 연결된 Epic 목록

- [ ] Epic 1: Zod + hook + 세션 상태 라우팅
- [ ] Epic 2: 세션 시작 + axisDraft 생성 페이지
- [ ] Epic 3: User Actions + DraftChangeLog + 낙관적 UI
- [ ] Epic 4: Roadmap/Selection draft + Selection 정책 강제
- [ ] Epic 5: Consensus 검증 + Commit + 결과

## 관련 문서

- **선행 fix 이슈**:
  - `workflow/task/fix/brainstorming/version/0.0.2v/issue-06-roadmap-selections-dualaxis.md`
  - `issue-09-ai-suggestion-3layer.md`
  - `issue-11-selections-version-policy.md`
- **선행 BE Product**: `workflow/task/pes/workspectrum/sdd/in-progress/product-ai-interactive-roadmap.md`
- **선행 FE Product**: `product-learning-tower.md`, `product-ai-suggestion.md`
- **관련 ADR**:
  - [ADR010](../../../../../docs/adr/ADR010.md) — Fallback
  - [ADR021](../../../../../docs/adr/ADR021-axis-deck-full-integration.md) — Axis Soft Delete
  - [ADR022](../../../../../docs/adr/ADR022-learning-tower-terminology.md) — 용어 표준
- **폐기 문서**:
  - 기존 `product-ai-interactive-roadmap.md` (FE) — 본 파일이 전면 재작성

## 열린 질문 (Open Questions)

1. **세션 재개 UI** — 사용자가 진행 중 세션 목록을 어디서 보나. LayerDetailPage 상단 링크?
2. **낙관적 UI 실패 시 rollback UX** — 인라인 flash vs 토스트. v1 은 인라인.
3. **DraftChangeLog 최대 표시 개수** — 20건 초과 시 접힘 vs 스크롤. v1 은 스크롤.
4. **Consensus 검증 실행 시점** — 실시간 vs [Commit 검토] 클릭 시. v1 은 실시간(폴백).
5. **Roadmap 없이 Commit 허용?** — v1 은 허용 (Roadmap 은 선택). Selection 은 무관.
6. **다중 Layer 세션 지원** — v1 은 단일 Layer. v2 다중.

## 제품 수준 완료 기준 (Product-level DoD)

- [ ] Epic 1~5 완료
- [ ] E2E: 시작 → axisDraft → 액션 5회 → Roadmap 편집 → Selection 3건 → Commit 전체 흐름
- [ ] Storybook 최소 15 스토리
- [ ] Sentry breadcrumb 정착

---

# [Epic 1] Zod + hook + 세션 상태 라우팅

## Epic 목표

세션 도메인 Zod 스키마와 조회 hook 을 정의하고, `<RoadmapSessionWizard>` 가 세션 상태에 맞춰 자동 라우팅.

## 완료 기준

- [ ] Story 4건 완료
- [ ] `SESSION_EXPIRED` / `SESSION_ALREADY_COMMITTED` 등 에러 응답 처리 통합

---

## [Story 1-1] `roadmapSessionSchema` + 도메인 타입

### User Story

- As a FE 개발자
- I want 세션 Zod 스키마 정의하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`, ADR022

```ts
export const sessionStateSchema = z.enum(
  ['INIT','DRAFTED','REVIEWING','COMMITTED','ABANDONED']
);

export const axisDraftItemSchema = z.object({
  name: z.string(),
  rationale: z.string(),
  roadmapDraft: z.string().nullable(),
  selectionDrafts: z.array(z.object({
    name: z.string(),
    content: z.string(),
  })),
});

export const roadmapSessionSchema = z.object({
  id: z.number(),
  facadeId: z.number(),
  layerId: z.number(),
  state: sessionStateSchema,
  axisDraft: z.object({
    axes: z.array(axisDraftItemSchema),
    refreshContext: z.string().nullable(),
  }).nullable(),
  axisConsensus: z.object({
    axes: z.array(z.any()),
    committedAt: z.string(),
  }).nullable(),
  expiresAt: z.string(),
  createdAt: z.string(),
});
```

### 완료 기준 (AC)

- Given 응답 파싱 / When 정상 / Then 통과
- Given ABANDONED 상태 / When / Then 파싱 성공 + Sentry 캡처

### Definition of Done

- [ ] 스키마
- [ ] 타입 export
- [ ] Vitest

### 스토리 포인트

1d

### 의존성

- 선행: BE Epic 1 완료
- 후행: Story 1-2

---

## [Story 1-2] `useRoadmapSession(sessionId)` hook + 상태 라우팅

### User Story

- As a FE 개발자
- I want 세션 조회 hook 이 상태에 따라 자동 라우팅하기를
- so that 사용자가 재진입 시 올바른 페이지에서 계속

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `useRoadmapSession(sessionId)` — GET /roadmap-sessions/{id}
- 상태별 라우팅:
  - INIT → `.../draft`
  - DRAFTED → `.../draft`
  - REVIEWING → `.../review`
  - COMMITTED → `.../commit`
  - ABANDONED → `.../abandoned`
- 410/409 응답 → 새 세션 안내

**핵심 파일**:
- 신규: `src/features/roadmap-sessions/hooks/useRoadmapSession.ts`

### 완료 기준 (AC)

- Given DRAFTED 세션 / When 진입 / Then draft 페이지 라우팅
- Given SESSION_EXPIRED / When / Then 새 세션 안내

### Definition of Done

- [ ] hook
- [ ] MSW handler
- [ ] Vitest 각 상태 케이스

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 1-1
- 후행: Story 1-3

---

## [Story 1-3] `<SessionStateStepper>` 시각화

### User Story

- As a 사용자
- I want 세션 진행 단계를 시각적으로 확인하기를
- so that 지금 어디에 있고 다음 단계가 무엇인지 안다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `<SessionStateStepper current="INIT|DRAFTED|REVIEWING|COMMITTED">`
- 4단계 stepper. 현재 단계 강조.
- ABANDONED 상태는 별도 alert 뷰.

**핵심 파일**:
- 신규: `src/features/roadmap-sessions/components/SessionStateStepper.tsx`

### 완료 기준 (AC)

- Given current="REVIEWING" / When / Then REVIEWING 강조

### Definition of Done

- [ ] 컴포넌트
- [ ] Storybook 5 스토리

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 1-1
- 후행: Story 1-4

---

## [Story 1-4] `<RoadmapSessionWizard>` — 라우트 오케스트레이션

### User Story

- As a 사용자
- I want /roadmap-sessions/:id 접근 시 자동으로 올바른 하위 페이지로 이동하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `<RoadmapSessionWizard>` — `useRoadmapSession()` 로 상태 확인 후 `<Navigate>`
- 상단 `<SessionStateStepper>` 상시 표시
- URL 파라미터 유지

### 완료 기준 (AC)

- Given /roadmap-sessions/42 (state=REVIEWING) / When 진입 / Then /roadmap-sessions/42/review 라우팅

### Definition of Done

- [ ] Wizard 컴포넌트
- [ ] 통합 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Story 1-2, 1-3
- 후행: Epic 2

---

# [Epic 2] 세션 시작 + axisDraft 생성

## Epic 목표

`<RoadmapSessionInitPage>` 와 `<AxisDraftPage>` 를 구현해 세션 시작부터 초안 생성까지 완주.

## 완료 기준

- [ ] Story 5건 완료
- [ ] E2E: 세션 시작 → axisDraft 생성 → refresh → 사용자 액션 진입

---

## [Story 2-1] `<RoadmapSessionInitPage>`

### User Story

- As a 학습자
- I want facadeId + Layer 를 선택해 세션을 시작하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `/roadmap-sessions/new`
- `<LayerSelect facadeId>` (learning-tower FE 재사용)
- [세션 시작] → `POST /roadmap-sessions { facadeId, layerId }`
- 응답 후 라우팅

### 완료 기준 (AC)

- Given Layer 선택 → 시작 / When / Then 세션 생성 + `/roadmap-sessions/{id}/draft` 라우팅
- Given Layer 미선택 / When 시작 시도 / Then `SESSION_LAYER_REQUIRED` 인라인

### Definition of Done

- [ ] 페이지
- [ ] 통합 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Epic 1, `product-learning-tower.md` (FE) Epic 2
- 후행: Story 2-2

---

## [Story 2-2] `<AxisDraftPage>` (INIT + DRAFTED 통합)

### User Story

- As a 학습자
- I want 초안 요청 버튼과 결과를 한 페이지에서 처리하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- INIT 상태: [초안 요청] 버튼 노출
- DRAFTED 상태: `<AxisDraftPanel>` 렌더 + [사용자 액션 시작] 버튼
- [초안 요청] → POST /axis-drafts → 상태 DRAFTED

### 완료 기준 (AC)

- Given INIT / When [초안 요청] / Then 초안 생성 + DRAFTED 렌더
- Given DRAFTED / When 재진입 / Then Panel 즉시 렌더

### Definition of Done

- [ ] 페이지
- [ ] MSW + 통합 테스트

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 2-1
- 후행: Story 2-3

---

## [Story 2-3] `<AxisDraftPanel>` + `<AxisDraftCard>`

### User Story

- As a 학습자
- I want 초안 axis 목록을 카드로 확인하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `<AxisDraftPanel axes>` — 카드 목록
- `<AxisDraftCard axis>` — name, rationale, roadmapDraft preview, selection drafts count
- 각 카드에 [Roadmap 초안] / [Selection 초안] 버튼 (Epic 4)

### 완료 기준 (AC)

- Given axes 4건 / When 렌더 / Then 4 카드

### Definition of Done

- [ ] 컴포넌트
- [ ] Storybook

### 스토리 포인트

1d

### 의존성

- 선행: Story 2-2
- 후행: Story 2-4

---

## [Story 2-4] `<RefreshContextInput>` + refresh 액션

### User Story

- As a 학습자
- I want refreshContext 를 입력해 초안을 다시 받기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `<RefreshContextInput value onChange>` — 텍스트 필드 + [다시 초안 받기] 버튼
- 클릭 → POST /axis-drafts/refresh { refreshContext }
- REVIEWING 상태에서는 비활성 + 안내 ("사용자 액션 중에는 refresh 불가")

### 완료 기준 (AC)

- Given DRAFTED / When refresh / Then 새 draft 로 갱신
- Given REVIEWING / When / Then 비활성

### Definition of Done

- [ ] 컴포넌트
- [ ] 통합 테스트

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 2-3
- 후행: Story 2-5

---

## [Story 2-5] Cascade 실패 fallback 통합

### User Story

- As a 학습자
- I want AI 실패에도 세션이 유지되기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`, ADR010

- Cascade `suggestionsAvailable=false` 시 `<FallbackBanner variant="unavailable">` (ai-suggestion FE 재사용)
- 사용자에게 수동 axis 추가 유도
- 세션 상태는 DRAFTED 유지 (빈 draft)

### 완료 기준 (AC)

- Given Cascade 실패 / When 초안 요청 / Then Banner + 빈 draft
- Given 수동 [+ 축 추가] / When (Epic 3) / Then draft 갱신

### Definition of Done

- [ ] Banner 통합
- [ ] 통합 테스트

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 2-2, `product-ai-suggestion.md` (FE)
- 후행: Epic 3

---

# [Epic 3] User Actions + DraftChangeLog + 낙관적 UI

## Epic 목표

사용자 액션 5종을 지원하고 DraftChangeLog 로 이력 노출.

## 완료 기준

- [ ] Story 5건 완료
- [ ] 낙관적 UI + 실패 시 rollback

---

## [Story 3-1] `<UserActionBar>` — 액션 5종

### User Story

- As a 학습자
- I want axis 카드마다 keep/remove/rename/add/reorder 액션을 사용하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- 카드마다 액션 버튼: [유지] / [제외] / [이름 변경] / [순서 변경]
- 목록 상단: [+ 축 추가]
- 각 액션 → PATCH /axes { action, target }
- 낙관적 UI: 로컬 draft 즉시 갱신

**핵심 파일**:
- 신규: `src/features/roadmap-sessions/components/UserActionBar.tsx`
- 신규: `src/features/roadmap-sessions/hooks/useUserActionMutation.ts`

### 완료 기준 (AC)

- Given keep / When / Then draft.axes[X].kept = true (로컬), 즉시 반영
- Given remove / When / Then draft.axes 에서 제거 + log 추가
- Given rename with 중복 이름 / When / Then 서버 409 → 로컬 rollback + 인라인 에러

### Definition of Done

- [ ] Action bar + hook
- [ ] MSW + Vitest (해피 + rollback)

### 스토리 포인트

2d

### 의존성

- 선행: Epic 2
- 후행: Story 3-2

---

## [Story 3-2] Add / Reorder 액션

### User Story

- As a 학습자
- I want 새 축을 추가하거나 순서를 변경하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- Add: `<AxisDraftForm>` 인라인 폼 (name + rationale) → PATCH action="add"
- Reorder: drag-and-drop 또는 [↑][↓] 버튼 → PATCH action="reorder"

### 완료 기준 (AC)

- Given Add / When 저장 / Then 목록 추가
- Given 이미 존재하는 이름 add / When / Then 409 → 인라인
- Given Reorder / When [↓] / Then 순서 변경

### Definition of Done

- [ ] 폼 + 순서 변경 UI
- [ ] 통합 테스트

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 3-1
- 후행: Story 3-3

---

## [Story 3-3] `<DraftChangeLogPanel>` — 사이드바

### User Story

- As a 학습자
- I want 사용자 액션 이력을 사이드바에서 확인하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `<DraftChangeLogPanel logs>` — 시간순 목록
- 각 항목: 액션 종류 + 대상 + 타임스탬프
- 예: "이름 변경: X → Y (5초 전)"

**핵심 파일**:
- 신규: `src/features/roadmap-sessions/components/DraftChangeLogPanel.tsx`

### 완료 기준 (AC)

- Given 액션 3회 / When / Then 3건 로그
- Given 최신순 렌더 / When / Then 위에서 아래

### Definition of Done

- [ ] Panel
- [ ] Storybook

### 스토리 포인트

1d

### 의존성

- 선행: Story 3-1
- 후행: Story 3-4

---

## [Story 3-4] `<RoadmapReviewPage>` — 통합 뷰

### User Story

- As a 학습자
- I want REVIEWING 단계의 통합 화면에서 액션 + 로그를 한번에 확인하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `/roadmap-sessions/:id/review`
- 좌측: `<AxisDraftPanel>` + `<UserActionBar>`
- 우측: `<DraftChangeLogPanel>` 사이드바
- 상단: [초안 재요청 안 됨 안내] + [저장 검토 →] 링크

### 완료 기준 (AC)

- Given /review / When 진입 / Then 통합 뷰 렌더

### Definition of Done

- [ ] 페이지 레이아웃
- [ ] 통합 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Story 3-1, 3-2, 3-3
- 후행: 없음

---

## [Story 3-5] 낙관적 UI rollback UX 표준화

### User Story

- As a FE 개발자
- I want 낙관적 UI 실패 시 일관된 rollback UX 를 제공하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- TanStack Query `onMutate` (optimistic) → `onError` (rollback)
- 실패 시 카드 배경 짧게 빨강 flash + 인라인 에러
- 로그도 rollback (log 추가 무효화)

### 완료 기준 (AC)

- Given optimistic 성공 / When / Then 서버 상태로 최종 재정합
- Given 서버 실패 / When / Then 로컬 rollback + flash + 로그 미반영

### Definition of Done

- [ ] rollback util
- [ ] 통합 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Story 3-1
- 후행: Epic 4

---

# [Epic 4] Roadmap / Selection draft + Selection 정책 강제

## Epic 목표

축별 Roadmap draft 편집과 Selection draft (name UNIQUE, hard delete confirm) 관리 UI.

## 완료 기준

- [ ] Story 4건 완료

---

## [Story 4-1] `<RoadmapDraftEditor>` — 인라인 편집

### User Story

- As a 학습자
- I want 축별 Roadmap 초안을 인라인으로 편집하기를

### 설명

> 출처: `issue-06-roadmap-selections-dualaxis.md`

- `<AxisDraftCard>` 안 [Roadmap 초안 요청] 클릭 → `useSuggestRoadmap` (ai-suggestion FE)
- 응답 시 `<RoadmapDraftEditor content editable>` textarea 표시
- 편집 후 [저장] → PATCH /axes/{name}/roadmap-draft { content }

**핵심 파일**:
- 신규: `src/features/roadmap-sessions/components/RoadmapDraftEditor.tsx`

### 완료 기준 (AC)

- Given 초안 요청 / When / Then textarea 노출
- Given 편집 후 저장 / When / Then draft.axes[X].roadmapDraft 갱신
- Given 2001자 / When / Then maxLength 사전 차단

### Definition of Done

- [ ] Editor
- [ ] MSW + Vitest

### 스토리 포인트

1d

### 의존성

- 선행: Epic 3, `product-ai-suggestion.md` (FE)
- 후행: Story 4-2

---

## [Story 4-2] `<SelectionDraftList>` + `<SelectionDraftCard>`

### User Story

- As a 학습자
- I want 축별 선정안 초안 목록을 확인하기를

### 설명

> 출처: `issue-06-roadmap-selections-dualaxis.md`, `issue-11-selections-version-policy.md`

- `<SelectionDraftList axis>` — 카드 목록
- 각 카드: name + content preview + [편집] · [삭제]
- 정렬: 생성순 (draft 안에서는 실용상 배열 순)

### 완료 기준 (AC)

- Given selections 3건 / When 렌더 / Then 3 카드

### Definition of Done

- [ ] 목록 + 카드
- [ ] Storybook

### 스토리 포인트

1d

### 의존성

- 선행: Story 4-1
- 후행: Story 4-3

---

## [Story 4-3] `<SelectionDraftForm>` — name 중복 방지

### User Story

- As a 학습자
- I want 새 selection 을 추가할 때 name 중복을 즉시 인지하기를

### 설명

> 출처: `issue-11-selections-version-policy.md`

- `<SelectionDraftForm axisName>` — name + content input
- Submit → 로컬 name 중복 검증 → PATCH /axes/{X}/selection-drafts
- 서버 409 `AXIS_SELECTION_NAME_DUPLICATE` → 인라인 에러 유지

### 완료 기준 (AC)

- Given name="A" 이미 있음 / When submit / Then 로컬 검증 → 인라인 즉시
- Given 새 이름 / When submit / Then 성공 + 목록 추가

### Definition of Done

- [ ] Form
- [ ] Vitest 로컬/서버 검증 케이스

### 스토리 포인트

1d

### 의존성

- 선행: Story 4-2
- 후행: Story 4-4

---

## [Story 4-4] `<SelectionDraftDeleteConfirm>` — hard delete 안내

### User Story

- As a 학습자
- I want selection 삭제 시 복원 불가 안내를 받기를

### 설명

> 출처: `issue-11-selections-version-policy.md`

- 다이얼로그: "복원 불가"
- 확인 → DELETE 요청

### 완료 기준 (AC)

- Given [삭제] / When / Then confirm
- Given [확인] / When / Then 삭제 성공

### Definition of Done

- [ ] Confirm
- [ ] 통합 테스트

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 4-2
- 후행: 없음

---

# [Epic 5] Consensus 검증 + Commit + 결과

## Epic 목표

Commit 진입 전 결정형 검증 시각화 + 저장 후 결과 표시.

## 완료 기준

- [ ] Story 4건 완료

---

## [Story 5-1] `useConsensusValidation` — 로컬 검증

### User Story

- As a 학습자
- I want [Commit] 전에 검증 상태를 실시간으로 확인하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `useConsensusValidation(session)` — 로컬 검증 결과 반환 (백엔드 계약 정합)
- 검증:
  - axes.length >= 1
  - 각 axis.name blank/duplicate 없음
  - (선택) roadmapDraft.content 존재

### 완료 기준 (AC)

- Given valid draft / When 검증 / Then `{valid: true}`
- Given empty axes / When / Then `{valid: false, code: "AXIS_CONSENSUS_EMPTY"}`
- Given 중복 이름 / When / Then `{valid: false, code: "AXIS_CONSENSUS_NAME_DUPLICATE"}`

### Definition of Done

- [ ] Hook
- [ ] Vitest 검증 케이스

### 스토리 포인트

0.5d

### 의존성

- 선행: Epic 3
- 후행: Story 5-2

---

## [Story 5-2] `<ConsensusValidationBanner>`

### User Story

- As a 학습자
- I want 검증 결과를 배너로 확인하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `<ConsensusValidationBanner status message>`
- 성공: 녹색 "저장 준비 완료"
- 실패: 빨강 + 이유 + 해결 안내

### 완료 기준 (AC)

- Given valid / When / Then 녹색
- Given empty axes / When / Then 빨강 + "최소 1개 축이 필요"

### Definition of Done

- [ ] Banner
- [ ] Storybook

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 5-1
- 후행: Story 5-3

---

## [Story 5-3] `<CommitButton>` + `useCommitMutation`

### User Story

- As a 학습자
- I want 검증 통과 시 [Commit] 활성화되기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `<CommitButton disabled={!valid}>`
- 클릭 → POST /commit
- 성공 → /roadmap-sessions/{id}/commit 라우팅
- 실패 (`LAYER_NOT_FOUND` 등) → 다이얼로그 안내

**핵심 파일**:
- 신규: `src/features/roadmap-sessions/components/CommitButton.tsx`
- 신규: `src/features/roadmap-sessions/hooks/useCommitMutation.ts`

### 완료 기준 (AC)

- Given valid / When / Then 활성
- Given invalid / When / Then 비활성
- Given 성공 / When / Then 라우팅
- Given LAYER_NOT_FOUND / When / Then 다이얼로그

### Definition of Done

- [ ] Button + hook + 통합 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Story 5-2
- 후행: Story 5-4

---

## [Story 5-4] `<RoadmapCommitPage>` + `<CommitResultPanel>`

### User Story

- As a 학습자
- I want 저장 완료 후 무엇이 추가되었는지 확인하기를

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `/roadmap-sessions/:id/commit` (COMMITTED 상태 필수)
- `<CommitResultPanel result>` — 추가된 axis 목록 + roadmap N건 + selection K건
- [내 지도 보기] → `/layers/{layerId}`

**핵심 파일**:
- 신규: `src/features/roadmap-sessions/RoadmapCommitPage.tsx`
- 신규: `src/features/roadmap-sessions/components/CommitResultPanel.tsx`

### 완료 기준 (AC)

- Given COMMITTED / When 진입 / Then 결과 렌더
- Given [내 지도 보기] / When / Then /layers/{layerId} 라우팅

### Definition of Done

- [ ] 페이지 + 결과 패널
- [ ] E2E: 전체 흐름

### 스토리 포인트

1d

### 의존성

- 선행: Story 5-3
- 후행: 없음

---

*작성일: 2026-07-01 | 상태: **5 Epic · 22 Story 전체 pending** | Milestone: FE milestone 3 (BE ai-interactive-roadmap + FE learning-tower + FE ai-suggestion 완료 후). 기존 `product-ai-interactive-roadmap.md` (FE) 전면 재작성.*
