# [Product] Learning Tower FE — 학습 모델 재편 UI

## Product Vision

> **학습 모델을 `concepts[] → Layer → Axis → {AxisRoadmapNode[], AxisSelection[{AxisSelectionNode[]}]} → Card` 로 재편한 백엔드 변화(BE product-learning-tower)를 FE 화면에 그대로 노출한다.**
> Deck 라우트·UI 는 폐기되고, Axis 가 학습의 기본 컨테이너가 된다. Review 는 layer 경계 폐기 후 하나의 daily 큐로 통합.

---

## 🔄 Fix 개정 (2026-07-02) — BE 07-02 pivot 반영

**개정 사유**: BE product-learning-tower/ai-suggestion/ai-interactive-roadmap의 07-02 pivot (이슈 #15/#16/#17/#19/#25)으로 FE 대응 필요.

### 개정 범위 요약

| 영역 | 이전 (~2026-06) | 개정 후 (2026-07-02) |
|---|---|---|
| Roadmap 편집 UI | `<AxisRoadmapEditor>` — 단일 textarea (`content: string`) | **`<RoadmapNodeList>` + `<RoadmapNodeCard>`** — 챕터 노드 카드 리스트, 각 카드에 `title` + `rationale` + `body` (ASCII 트리 textarea) |
| Selection 편집 UI | `<AxisSelectionList>` + `<AxisSelectionEditor>` — 컨테이너의 `content: string` textarea | **컨테이너 유지 + 컨테이너 안에 `<SelectionNodeList>` + `<SelectionNodeCard>`** (Roadmap과 동일 구조) |
| Review scope 토글 | `<ReviewScopeToggle mode="AXIS|LAYER">` (이슈 #14 대응) | **폐기** — layer 경계 폐기(이슈 #25). Cross-layer 짬뽕 큐가 자동 노출되므로 사용자 선택 없음 |
| 카드 편집기 axis 선택 | `<AxisSelect>` — Layer 안 axis 목록만 | 유지 (이슈 #07 방향과 정합) |
| Zod 스키마 | `AxisRoadmap({content: string})`, `AxisSelection({content: string})` | **`AxisRoadmapNode({title, rationale, body})` 배열 + `AxisSelection({containers: {name, nodes: NodeSchema[]}})`** |
| roadmap/selections 개념 툴팁 | 없음 (백엔드 개념만) | **`<ConceptSpecTooltip>`** — 각 편집 화면에 "수렴/발산" 안내 문구 (이슈 #19 개념 명세 UI화) |

### 뒤집히는 기존 UI 결정

- **`<ReviewScopeToggle>` 폐기** (Design Decisions §ReviewScope UI) — layer 경계 폐기로 사용자가 선택할 축 없음. Cross-layer daily 큐가 기본.
- **`<AxisRoadmapEditor>` textarea 단일** → 챕터 노드 리스트. Roadmap 편집 = 챕터 CRUD + 각 챕터 body 편집.
- **`<AxisSelectionEditor>` 통짜 편집** → 컨테이너 + 노드 리스트 이중 구조.
- **`components/LayerProgressBadge`** — v1 스코프에서 유지 여부 재검토. Layer 시각화는 릴리스 문서 v1 out of scope. **v1 미도입, v2 이관.**

### 새로 필요한 컴포넌트

- `<RoadmapNodeList>` / `<RoadmapNodeCard>` — 챕터 노드 리스트 + 추가·편집·삭제·순서변경
- `<SelectionContainerList>` / `<SelectionNodeList>` / `<SelectionNodeCard>` — Selection도 노드화
- `<ConceptSpecTooltip mode="roadmap|selection">` — "수렴/발산" 개념 안내 툴팁
- `<NodeBodyEditor>` — ASCII 트리 편집 (기본 textarea, monospace font, tab indent hint)

### v1 out of scope (릴리스 문서 참조)

- 챕터 노드 재생성 API + hint UI (이슈 #18 이관) — v2
- roadmap/selections 개념 명세 확장 UI (예: 판별 기준 헬퍼) — v2
- 대량 노드 재정렬 UI — v2

---

## 배경 및 문제

- **현재 상황 (As-Is)**
  - `features/learning-facade/` — 컨셉 입력이 단일 문자열(`concept`) input.
  - `features/decks/` — Deck 라우트(`/decks`) 와 Deck 관련 컴포넌트가 실제 사용자 경로.
  - `features/axes/` — Axis 하위 `AxisTopic` 트리(2-depth) 를 렌더.
  - `features/study/` (또는 `features/review/`) — Review 는 `deckId` 를 참조해 시작.
  - `features/cards/` — Card 편집기가 `topicId` 를 선택.
  - `lib/api/schemas/`: `concept: z.string()`, `topic: z.object({...})`, `deck: z.object({..., axisId?, axisName?})` 스키마.
  - "Layer 1" 이라는 애매한 라벨이 FE 리뷰 카드 상단에 노출 (그룹 리뷰 = 전체 리뷰 의미 혼재).
- **발생하는 문제**
  - 다중 관점 학습(백엔드 + 시스템 설계 등) 을 표현할 컨셉 입력 UI 부재.
  - Deck 라우트가 Axis 와 1:1 이라 사용자 UX 가 이중.
  - Topic 트리가 실제 카드와 축 사이 잉여 계층 표시 — 사용자가 "왜 이 계층이 있지" 라는 인지 부담.
  - Roadmap(축의 헌법) 과 Selection(사례) 을 구분해 표시할 UI 없음. Topic 목록 하나가 두 개념을 겸함.
  - Review 세션 시작 시 "축 리뷰만 vs 전체 리뷰(=Layer 1)" 두 옵션이지만 라벨이 모호. Layer 스코프 리뷰가 없음.
  - 카드 생성 시 topic 선택 → 축 조회 이중 클릭.
- **왜 지금 해결해야 하는가**
  - BE product-learning-tower Epic 1~6 이 이 재편을 확정함. FE 가 뒤따르지 않으면 API breaking(응답 필드 변경) 이 화면 오류로 전파.
  - 신규 milestone 진입 전 FE 스키마·라우트·컴포넌트 일제 재편이 안전 (트래픽 0).
  - FE 만 남은 Deck 라우트는 사용자 혼란 유발 — 백엔드 폐기와 동시 정리.

## 목표 (To-Be)

- **`features/learning-facade/`** — 다중 컨셉 입력 UI: chip input(태그 추가/제거), 1~5 제약 검증, 중복 방지 (BE Epic 1 대응, #04).
- **`features/layers/`** (신규) — Layer 목록·생성·소프트삭제 UI. LearningFacade 하위 계층으로 렌더(#05).
- **`features/axes/`** — **AxisRoadmap 챕터 노드 리스트 UI (이슈 #15)** — `<RoadmapNodeList>` + 각 노드에 `<RoadmapNodeCard>` (title·rationale·body TEXT ASCII 통짜 편집) + 노드 추가·삭제·순서변경. **AxisSelection 컨테이너 리스트 (이슈 #11 정책 유지) + 각 컨테이너 안 `<SelectionNodeList>` (이슈 #16)** — 노드도 챕터 단위.
- **`features/cards/`** — Card 편집기가 topic 대신 axis 직접 선택. Topic Picker 컴포넌트 삭제(#07). **Card lifecycle 재편 반영은 `product-card.md` (FE)로 분리** (이슈 #21/#22/#23).
- **`features/decks/` 삭제** — `/decks` 라우트 삭제, `/axes/*` 로 통합(#13). 남은 사용자 저장 URL 은 `/decks/:id` → `/axes/:id` 리디렉트로 6개월 유지.
- **`features/review/` 재편** — **layer 경계 폐기 (이슈 #25). `<ReviewScopeToggle>` 폐기.** Cross-layer daily 큐 UI로 통합 (상세는 `product-review.md` (FE)로 분리).
- ~~**`components/LayerProgressBadge`**~~ — **v1 out of scope**. Layer 시각화는 v2로 이관.
- **UI 문구** — "Layer 1" 폐기, "오늘 학습" / "축 · {axisName}" 표기. layer 경계 UI 소멸에 따라 "그룹 리뷰" 라벨도 폐기.
- **모든 Zod 스키마 재편** — `concepts: z.array(z.string())`, `Layer`, **`AxisRoadmapNode({title, rationale, body})[]`**, **`AxisSelection({name, nodes: AxisSelectionNode[]})[]`**. `topic`/`AxisTopic` 스키마는 삭제. 기존 `AxisRoadmap({content: string})` / `AxisSelection({content: string})` 폐기.
- **`<ConceptSpecTooltip>`** (신설) — Roadmap/Selection 편집 진입 시 "수렴(헌법) vs 발산(판례)" 개념 안내 (이슈 #19 개념 명세 UI 반영 최소판).

## 설계 결정 (Design Decisions)

- **Chip Input 방식 (컨셉 입력)**
  - `features/learning-facade/components/ConceptsInput.tsx` — 태그 추가/삭제/재정렬 UI.
  - 1~5 범위 검증은 Zod (`z.array(z.string().min(1).max(100)).min(1).max(5)`).
  - 저장 전 trim + 대소문자 정규화(선택). 백엔드 검증(`LEARNING_FACADE_CONCEPT_DUPLICATE`) 에러 응답을 인라인 표시.
- **Layer 를 별도 features 폴더로 분리**
  - `features/layers/` — LayerListPage, LayerFormDialog, LayerCard 등.
  - LearningFacadePage 안에서도 소비될 수 있으나 Layer 자체 관심사는 별도 모듈.
  - Layer softDelete 는 다이얼로그 confirm 필요(하위 axis 존재 시 `LAYER_HAS_ACTIVE_AXES` 안내).
- **Roadmap 은 챕터 노드 카드 리스트 + 각 노드 body ASCII 편집기 (2026-07-02 개정, 이슈 #15)**
  - `<RoadmapNodeList>` — 챕터 노드 카드 리스트. 순서변경(drag-and-drop) + 추가·삭제.
  - 각 카드 `<RoadmapNodeCard>` — title (short input) + rationale (single-line input) + body (`<NodeBodyEditor>` — textarea monospace, ASCII 트리 통짜 편집).
  - placeholder: 사용자 예시(하네스 로드맵 형태 서브트리) 인용.
  - 저장은 즉시 (PATCH idempotent per node). 사용자 명시 저장 버튼도 병행.
  - **폐기**: 단일 통짜 textarea (이전 결정).
- **Selection 정책 UX 강제 (이슈 #11 컨테이너 정책 유지 + 이슈 #16 노드 CRUD 신규)**
  - 컨테이너 리스트 정렬 항상 `created_at DESC` (백엔드 계약 그대로).
  - 같은 name 재저장 시도 → 백엔드 409 응답 → 인라인 에러 "이미 존재. 다른 이름 사용" + 신규 생성 유도 문구.
  - 컨테이너 hard delete 이므로 `<ConfirmDialog>` 필수 ("삭제하면 자식 노드까지 복원 불가").
  - **컨테이너 내부 노드 CRUD**: Roadmap 노드와 동일 UI (`<SelectionNodeList>`, `<SelectionNodeCard>`).
- **개념 명세 툴팁 (이슈 #19)**
  - `<ConceptSpecTooltip mode="roadmap">` — "수렴된 판단 프레임 (오래가는 원리)" 안내.
  - `<ConceptSpecTooltip mode="selection">` — "발산된 적용안 (상황별 대안)" 안내.
  - 편집 진입 시 첫 1회 자동 표시 (`sessionStorage` flag), 이후 아이콘 클릭으로 재표시.
- **`/decks/*` 리디렉트 6개월 유지**
  - 저장된 URL 이 소리없이 실패하는 것 방지. `Navigate` 컴포넌트로 `/decks/:id` → `/axes/:id` (같은 id 매핑).
  - 6개월 후 리디렉트 제거 (다음 릴리스에서 결정).
- ~~**ReviewScope UI**~~ **폐기 (2026-07-02 개정, 이슈 #25)** — layer 경계 폐기로 사용자가 선택할 스코프 없음. Cross-layer daily 큐가 자동 노출. 상세 UI는 `product-review.md` (FE) 참조.
- ~~**LayerProgressBadge**~~ **v1 out of scope** — Layer 시각화(진행률 파생 API)가 릴리스 문서 v1 out of scope. v2 이관.
- **Card 편집기의 축 직접 매핑**
  - `<AxisSelect>` — Layer 안 axis 목록만 노출. Topic Picker 제거.
  - v0.0.2v 마이그레이션 기간(BE Epic 4 진행 중) 은 `card.topicId` fallback 렌더 지원, 이후 삭제.
- **Zod 스키마 강타입 이관**
  - `topic` 스키마 삭제 → `AxisRoadmap` / `AxisSelection` 스키마 신설.
  - `deck` 스키마 소프트 폐기 (내부 코드 참조 0건 될 때 파일 삭제).
  - `concepts: string[]` / `layerId: number` / `axisId: number` 로 정규화.

## 대안 검토 (Alternatives Considered)

### 갈림길 A. 컨셉 입력 UI

**Option A — Multi-line textarea (한 줄에 한 컨셉)**
- 장점: 구현 단순.
- 거부 이유: 개별 컨셉 검증/삭제 UX 어려움. 중복 방지 유도 부재.

**Option B (선택) — Chip Input (태그 UI)**
- 비용: 컴포넌트 자체 상태 관리(입력값·추가·삭제) 필요.
- 보상: 각 컨셉이 시각적으로 독립. 삭제/편집 즉시. UX 표준 패턴.

**Option C — 5개 input 슬롯 고정**
- 거부 이유: 사용자가 3개만 넣고 싶어도 5개 슬롯 노출 부담. min=1 표현 어려움.

### 갈림길 B. Roadmap 에디터

**Option A — Rich Text Editor (Slate, Tiptap)**
- 거부 이유: ASCII 트리 표현에 과잉. 사용자에게 편집 부담. 저장 형식 변환 필요.

**Option B (선택) — 순수 `<textarea>` + placeholder**
- 비용: 트리 시각화 부재.
- 보상: 즉시 편집. 저장 형식이 ASCII 그대로. Diff 도 자연스러움.

**Option C — Monaco/CodeMirror 트리 언어 커스텀**
- 거부 이유: 컴포넌트 로드 무거움 (`~200KB+`). ASCII 트리에 과잉.

### 갈림길 C. Selection 편집 UX

**Option A — Modal 편집 창**
- 장점: 목록 UI 단순.
- 거부 이유: 모달-리스트 왕복 UX 부담. 편집 결과 즉시 목록에 반영 어려움.

**Option B (선택) — 인라인 편집 (`<AxisSelectionCard editable>`)**
- 비용: 카드마다 편집/저장 상태 관리.
- 보상: 목록 안에서 즉시 편집. 저장/취소 명확.

**Option C — 별도 페이지 (`/axes/:id/selections/:selectionId/edit`)**
- 거부 이유: URL 부담. 목록과 편집이 다른 페이지라 컨텍스트 상실.

### 갈림길 D. Deck 라우트 처리

**Option A — 즉시 삭제**
- 거부 이유: 저장된 사용자 URL(북마크·공유 링크) 이 소리없이 404.

**Option B (선택) — 6개월 리디렉트 유지**
- 비용: 리디렉트 코드가 6개월 코드베이스에 잔존.
- 보상: 저장 URL 은 유지. 후속 릴리스에서 삭제 결정 유예.

**Option C — 별칭 라우트 (`/decks` 와 `/axes` 병존)**
- 거부 이유: 두 이름이 공존하면 개발자·문서가 어느 것을 쓸지 매번 결정. 이슈-08 용어 표준화 위반.

### 갈림길 E. ReviewScope 표현

**Option A — 별도 URL (`/axes/:id/review` vs `/layers/:id/review`)**
- 장점: URL 로 스코프 명시.
- 거부 이유: 진입 화면(SessionStartPage) 에서 두 URL 을 사전에 선택해야 함. 스코프 토글 UX 자연스러움 저해.

**Option B (선택) — 단일 URL(`/review/start`) + Scope Toggle 컴포넌트**
- 비용: 진입 후 스코프 선택 상태 관리.
- 보상: 단일 진입점. 스코프 전환 즉시.

**Option C — 두 화면 병렬 렌더 (탭)**
- 거부 이유: 화면 폭 부담. 두 스코프 동시 시작 유즈케이스 없음.

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 / 라우트 배치

```
[Router]
  /learning-facade                              → LearningFacadePage
  /learning-facade/concepts                     → ConceptsEditPage
  /layers                                       → LayersListPage
  /layers/:layerId                              → LayerDetailPage
  /axes/:axisId                                 → AxisDetailPage (Roadmap · Selection · Cards · Review)
  /axes/:axisId/roadmap/edit                    → RoadmapEditPage
  /axes/:axisId/selections                      → SelectionsListPage
  /axes/:axisId/cards                           → AxisCardsPage
  /review/start                                 → ReviewStartPage (scope toggle)
  /review/sessions/:sessionId                   → ReviewSessionPage
  /decks/*                                      → <Navigate replace to="/axes/*" />

features/learning-facade/
├─ LearningFacadePage.tsx
├─ ConceptsEditPage.tsx
├─ components/
│   ├─ ConceptsInput.tsx           (chip input, 1~5)
│   └─ ConceptCard.tsx
└─ hooks/
    ├─ useLearningFacade()
    └─ useConceptsMutation()

features/layers/                                (신규)
├─ LayersListPage.tsx
├─ LayerDetailPage.tsx
├─ components/
│   ├─ LayerCard.tsx
│   ├─ LayerFormDialog.tsx
│   ├─ LayerProgressBadge.tsx
│   └─ LayerConfirmDeleteDialog.tsx (하위 axis 존재 시)
└─ hooks/
    ├─ useLayers()
    └─ useLayerMutations()

features/axes/
├─ AxisDetailPage.tsx              (Roadmap · Selections · Cards 3-탭)
├─ RoadmapEditPage.tsx
├─ SelectionsListPage.tsx
├─ AxisCardsPage.tsx
├─ components/
│   ├─ AxisRoadmapEditor.tsx       (textarea + preview)
│   ├─ AxisSelectionCard.tsx       (인라인 편집)
│   ├─ AxisSelectionsList.tsx      (created_at DESC)
│   └─ AxisSelectionDeleteConfirm.tsx
└─ hooks/
    ├─ useAxis()
    ├─ useAxisRoadmap()
    ├─ useAxisSelections()
    └─ useAxisMutations()  (mode/onLibrary/publishedAt 등 흡수된 필드)

features/cards/
├─ CardEditor.tsx                  (topic → axis 직접 선택)
└─ components/
    └─ AxisSelect.tsx              (Layer→Axis 계층 선택)

features/review/
├─ ReviewStartPage.tsx
├─ ReviewSessionPage.tsx
├─ components/
│   ├─ ReviewScopeToggle.tsx       (AXIS | LAYER)
│   ├─ LayerSelect.tsx             (Layer scope 선택 시)
│   ├─ AxisSelect.tsx              (Axis scope 선택 시)
│   └─ ReviewScopeLabel.tsx        ("축 리뷰: X" / "그룹 리뷰: Y")
└─ hooks/
    ├─ useReviewSession()
    └─ useReviewMutations()

features/decks/                    (삭제 예정)
└─ DecksRedirect.tsx               (/decks/* → /axes/* 리디렉트)

lib/api/schemas/
├─ learningFacade.ts               (concepts: string[])
├─ layer.ts                        (신규)
├─ axis.ts                         (mode/progressStatus/onLibrary/publishedAt 등 흡수)
├─ axisRoadmap.ts                  (신규, axis_id UNIQUE, content)
├─ axisSelection.ts                (신규, name UNIQUE per axis, DESC)
├─ card.ts                         (axis_id NOT NULL, topic_id 삭제)
├─ reviewSession.ts                (scope, scopeId)
└─ deck.ts                         (삭제 예정)

lib/api/endpoints/
├─ learningFacade.ts
├─ layer.ts                        (신규)
├─ axis.ts
├─ axisRoadmap.ts                  (신규)
├─ axisSelection.ts                (신규)
├─ card.ts
├─ reviewSession.ts
└─ deck.ts                         (삭제 예정)
```

### 핵심 플로우

**1. 신규 사용자 진입 → concepts 입력 → default Layer 자동**
```
LoginSuccess → GET /api/v1/learning-facade
  응답 { concepts: [], layers: [{id, name:"Uncategorized"}] }
    ├─ concepts.length === 0 → <ConceptsEditPage> 안내
    └─ layers.length === 1 && name === "Uncategorized" → LearningFacadePage default Layer 표시
사용자 <ConceptsInput> 에 컨셉 3개 입력
  PATCH /learning-facade/concepts { concepts: [...] }
    응답 201 { concepts: [...] }
FE → /layers 로 이동
```

**2. Axis 상세 (Roadmap · Selections · Cards 3-탭)**
```
사용자 → /axes/:axisId (기본 탭: Roadmap)
  useAxis() → GET /axes/{id}
  useAxisRoadmap() → GET /axes/{id}/roadmap (없으면 404, 빈 상태 노출)
  useAxisSelections() → GET /axes/{id}/selections (DESC)
Tab "Roadmap" → <AxisRoadmapEditor>
Tab "Selections" → <AxisSelectionsList>
Tab "Cards" → <AxisCardsPage>
```

**3. Selection 추가 → name 중복 방지 UX**
```
<AxisSelectionsList> → [+ 새 사례 추가] 버튼
  <AxisSelectionForm> 모달
사용자 입력 { name:"A", content:"..." }
  POST /axes/{id}/selections { name, content }
    응답 409 AXIS_SELECTION_NAME_DUPLICATE
      → 인라인 에러 "이미 존재. 다른 이름으로 추가하세요"
    응답 201
      → 목록 상단에 새 카드 (DESC 정렬 자연스러움)
```

**4. Card 생성 (topic 삭제, axis 직접)**
```
사용자 → /axes/:axisId → [+ 새 카드] 버튼
  <CardEditor> 모달 오픈, axisId 기본값 = 현재 축
    (또는 /cards/new 진입 시 <AxisSelect> 로 축 선택 강제)
사용자 입력 → POST /cards { axisId, summary, keywords[] }
```

**5. Review 세션 시작 (스코프 토글)**
```
사용자 → /review/start
  <ReviewScopeToggle mode="AXIS">
    ├─ AXIS 선택 → <AxisSelect>
    └─ LAYER 선택 → <LayerSelect>
[시작] 버튼
  scope=AXIS: POST /axes/{id}/review-sessions
  scope=LAYER: POST /layers/{id}/review-sessions
    응답 201 { sessionId, scope, scopeId, cards:[...] }
  → /review/sessions/:sessionId 로 이동
```

**6. Deck URL 리디렉트 (레거시 지원)**
```
사용자 저장 URL → /decks/42
  <DecksRedirect> match /decks/:id
    → <Navigate replace to="/axes/42" />
     (같은 id 매핑 규칙, ADR021 이후 축=덱 1:1)
```

### 외부 의존

- **백엔드 `/api/v1/*`**: `product-learning-tower` (BE) Epic 1~6 신설/변경 엔드포인트.
- **product-auth**: 세션·인증(JWT) — 변화 없음, 그대로 소비.
- **product-ai-suggestion (FE)**: Layer/Axis/Roadmap/Selection 초안 요청 버튼이 본 Product 화면에서 노출.
- **product-ai-interactive-roadmap (FE)**: `/roadmap-sessions` 세션 흐름 진입 버튼이 LayerDetailPage 상단.

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | ApiError code | HTTP | 클라이언트 권장 동작 (UX) |
| --- | --- | --- | --- |
| `concepts.length < 1` | `LEARNING_FACADE_CONCEPTS_SIZE_INVALID` | 400 | 인라인 "최소 1개 이상 컨셉이 필요합니다" |
| `concepts.length > 5` | `LEARNING_FACADE_CONCEPTS_SIZE_INVALID` | 400 | Chip Input 이 6번째 추가 시 사전 차단(백엔드까지 안 감) |
| concept 중복 | `LEARNING_FACADE_CONCEPT_DUPLICATE` | 409 | 인라인 "이미 등록된 컨셉입니다" |
| Layer 이름 blank | `LAYER_NAME_BLANK` | 400 | Form 검증(Zod) 로 사전 차단 |
| Layer 이름 중복 | `LAYER_NAME_DUPLICATE` | 409 | 인라인 "이미 존재하는 Layer 이름입니다" |
| Layer softDelete 시 하위 axis 존재 | `LAYER_HAS_ACTIVE_AXES` | 409 | 다이얼로그 "축 X개가 있어 삭제할 수 없습니다. 먼저 이동/삭제해주세요" |
| Roadmap content > 2000 | `AXIS_ROADMAP_CONTENT_TOO_LONG` | 400 | 문자 카운터로 사전 안내 |
| Selection name 중복 | `AXIS_SELECTION_NAME_DUPLICATE` | 409 | 인라인 "다른 이름으로 시도해주세요" |
| Selection delete (hard delete) | (성공 204) | — | ConfirmDialog "삭제하면 복원할 수 없습니다" 필수 |
| Card axis_id 누락 | `CARD_AXIS_REQUIRED` | 400 | AxisSelect 사전 검증 |
| Legacy topic_id 참조 카드 조회 | `LEGACY_TOPIC_REFERENCE` | 410 | 안내 배너 "해당 카드는 마이그레이션 대상입니다" + 관리자 문의 |
| `/decks/*` 접근 (리디렉트 후 재접근) | 404 (원격) | — | DecksRedirect 가 <Navigate> 로 라우팅 (원격 404 안 감) |
| Layer 스코프 리뷰 시작, axis 0건 | `LAYER_HAS_NO_AXES` | 400 | 다이얼로그 "Layer 에 축이 없습니다. 먼저 축을 추가해주세요" |
| ReviewScope enum 잘못됨 (오래된 클라이언트) | `REVIEW_SCOPE_INVALID` | 400 | Sentry 로 캡처 · 새로고침 안내 (앱 강제 업데이트) |

### 로깅 정책 (FE)

- **항상 기록** (Sentry / console):
  - ApiError code + status + requestId (모든 mutation 실패)
  - Zod parse fail (스키마 mismatch — 백엔드 breaking 감지)
- **debug**:
  - concepts input 입력 흐름 (dev only)
- **절대 금지**:
  - Roadmap/Selection content 본문
  - 사용자 학습 이력 본문

### 관측 지표 (Web Vitals + 도메인 이벤트)

- LCP P75 (`ReviewStartPage`, `AxisDetailPage`) ≤ 2s
- Layer 스코프 세션 시작 성공률 (product-op 협력)
- `/decks/*` 리디렉트 발생 빈도 (하락하면 리디렉트 제거 시점 판단)
- concepts 입력 폼 이탈률 (UX 실패 신호)

## 롤아웃 / 마이그레이션 (Rollout)

### 전제

- 트래픽 0. BE product-learning-tower Epic 별로 순차 배포 → FE 도 Epic 별 순차 대응.
- 배포 단위: BE 각 Epic 완료 후 FE 대응 Epic 진입.

### Product 의존성

- **선행 Product**:
  - BE `product-learning-tower` Epic 1~6 (필수, 각 Epic 완료 후 FE 대응 Epic 진행)
  - FE `product-auth.md` (세션 · JWT, 무영향, 그대로 소비)
  - FE `Product.md` (User 프로필, 무영향)
- **후행 Product**:
  - FE `product-ai-suggestion.md` (재작성) — Layer/Axis/Roadmap/Selection 초안 버튼이 본 Product 화면에서 노출
  - FE `product-ai-interactive-roadmap.md` (재작성) — 세션 진입 버튼이 LayerDetailPage 에서 노출

### Epic·Story 의존성 그래프

```
Epic 1 (concepts[] 입력 UI)
  │
  ▼
Epic 2 (Layer features + LayerProgressBadge)
  │
  ▼
Epic 3 (AxisDetailPage 재구성 · Roadmap 에디터 · Selection UI + 정책)
  │
  ▼
Epic 4 (Card 편집기 axis 직접 · Topic Picker 삭제)
  │
  ▼
Epic 5 (Deck 라우트 리디렉트 · features/decks 삭제)
  │
  ▼
Epic 6 (Review scope 토글 · Layer 스코프 세션 진입)
```

### 환경별 설정 분기

| 항목 | dev (`.env.development`) | prod (`.env.production`) |
| --- | --- | --- |
| `VITE_API_BASE_URL` | http://localhost:8080 | https://api.thirdtool.dev |
| MSW handlers | 신규 6개 (Layer/Roadmap/Selection/Review-Layer 등) | disabled |
| Deck 리디렉트 | enabled | enabled (6개월) |
| Sentry breadcrumbs | verbose (concepts input flow) | ApiError + Zod fail만 |

## 성공 지표 (KPI)

- `concepts` 입력 완료율 ≥ 90% (신규 사용자 진입 후 1분 이내)
- Layer 생성 (사용자 지정, "Uncategorized" 외) ≥ 1건 / 활성 사용자 (Epic 2 완료 6개월 후)
- Roadmap 설정 축 비율 ≥ 60% (Epic 3 완료 3개월 후, 백엔드 KPI 와 정합)
- Selection name 중복 시도 → 성공 재입력율 ≥ 95% (인라인 UX 효과)
- Card 편집기의 Topic Picker 참조 = 0 (grep `TopicPicker`, Epic 4 완료 후)
- `/decks/*` 리디렉트 발생 빈도 6개월 후 ≤ 1건/일 (제거 시점 판단)
- Review Layer 스코프 세션 시작 성공률 = 100%
- Layer 스코프 세션 완료율 ≥ 30% (Epic 6 완료 6개월 후, UX 학습 후 조정)

## Scope

**In Scope**:
- `features/learning-facade/` 리팩토링 (concepts[])
- `features/layers/` 신설
- `features/axes/` 재구성 (Roadmap/Selection 도입, Topic 삭제)
- `features/cards/` CardEditor axis 직접 선택
- `features/decks/` 폐기 + 리디렉트
- `features/review/` ReviewScope 토글 + Layer 스코프 진입
- `lib/api/schemas/` 재편 (topic 삭제, Layer/Roadmap/Selection 신설, deck 삭제 예정)
- `lib/api/endpoints/` 재편
- Zod 스키마 breaking migration
- ReviewScope 라벨 매핑

**Out of Scope**:
- Roadmap/Selection AI 생성 UI — `product-ai-suggestion.md` (FE)
- RoadmapInteractionSession 상태 UI — `product-ai-interactive-roadmap.md` (FE)
- Card 를 다른 Axis 로 이동하는 UX — 후속 검토
- Layer 간 Axis 이동 UX — 후속
- UI 문구 다국어(`docs/ux/wip-language.md` 참조)
- Deck 라우트 물리 삭제 — 6개월 후 다음 릴리스

## 대상 사용자

- **신규 학습자** — 여러 컨셉을 한 지도에 담고, Layer 로 그룹핑, Axis 스코프 리뷰.
- **기존 학습자** — 마이그레이션 이후에도 카드 소실 없음, Deck URL 리디렉트로 북마크 유지.
- **강사/코치** — Layer 별 진행률 뱃지로 학습자 진도 관찰.
- **FE 개발자** — Zod 스키마 단순화, Topic 제거로 화면 계층 얕아짐.

## 연결된 Epic 목록

- [ ] Epic 1: concepts[] 입력 UI (Chip Input + Zod min(1).max(5))
- [ ] Epic 2: Layer features 신설 + LayerProgressBadge
- [ ] Epic 3: AxisDetailPage 재구성 + Roadmap 에디터 + Selection 정책 UI
- [ ] Epic 4: Card 편집기 axis 직접 + Topic Picker 삭제
- [ ] Epic 5: Deck 라우트 리디렉트 + features/decks 삭제
- [ ] Epic 6: Review scope 토글 + Layer 스코프 세션 진입

## 관련 문서

- **선행 fix 이슈** (BE 정합):
  - `workflow/task/fix/brainstorming/version/0.0.2v/issue-04-concept-list.md`
  - `issue-05-layer-server-domain.md`
  - `issue-06-roadmap-selections-dualaxis.md`
  - `issue-07-card-axis-direct-mapping.md`
  - `issue-11-selections-version-policy.md`
  - `issue-13-deck-abolition-axis-absorption.md`
  - `issue-14-review-strategy-axis-layer-scope.md`
- **선행 BE Product**: `workflow/task/pes/workspectrum/sdd/in-progress/product-learning-tower.md`
- **관련 BE ADR**: [ADR022](../../../../../docs/adr/ADR022-learning-tower-terminology.md) — 용어 표준. FE 컴포넌트/스키마 명명 준수.
- **FE-ADR 후보**:
  - `FE-ADR-{NNN}: Deck 라우트 6개월 리디렉트 정책`
  - `FE-ADR-{NNN}: Zod 스키마 breaking migration 관행` (schema.ts 파일 삭제·리네임 시 검색어)
- **폐기 문서**:
  - `product-deck.md` (본 Product Epic 5 에서 흡수)
  - `product-aisuggestion.md` (`product-ai-suggestion.md` 로 재작성)

## 열린 질문 (Open Questions)

1. **`concepts` 정규화 규칙** — trim 만 vs 소문자 통일(예: "백엔드" vs "백엔드 "). v1 은 trim 만, 중복 방지는 백엔드 검증에 의존.
2. **Layer 삭제 시 하위 axis 이동 UX** — 다이얼로그 안에서 다른 Layer 로 이동 선택하도록 할지 vs "먼저 이동 후 재시도" 안내만.
3. **Roadmap content 힌트 형식** — placeholder ASCII 트리 예시를 어떻게 표현할지. 예시 여러 개 로테이션?
4. **Selection 정렬 방향 사용자 커스터마이즈** — 현재 DESC 고정 (백엔드 계약). ASC 옵션 필요? v1 은 고정.
5. **Card 이동 UX** — 다른 Axis 로 카드 이동은 Scope 밖. `<CardEditor>` 에서 axis 변경 허용할지 (제한적 이동 UX)?
6. **`/decks/*` 리디렉트 종료 시점** — 6개월 후 관측 지표(리디렉트 발생 빈도) 로 결정.
7. **Layer 스코프 세션 진입 시 카드 정렬** — Layer 내 axis 순서 유지 vs 카드 갯수 균등 인터리브. v1 은 Layer 순서 유지.
8. **ReviewScopeToggle 기본값** — AXIS vs LAYER vs 최근 사용. v1 은 AXIS.

---

# [Epic 1] concepts[] 입력 UI

## Epic 목표

`LearningFacade.concept: string` 단수 필드 UI 를 `concepts: string[]` (1~5) Chip Input 으로 승격해 다중 관점 표현을 지원한다.

## 배경

BE Epic 1 완료 시 `learning_facade_concept` 자식 테이블이 존재하고 API 응답도 `concepts: string[]`. FE 는 스키마 이관 + Chip Input UI 신설.

## 완료 기준

- [ ] 아래 Story 5건 완료
- [ ] Zod 스키마 `concept: string` → `concepts: z.array(...).min(1).max(5)` 이관
- [ ] E2E: 신규 사용자 진입 → 컨셉 3개 입력 → 저장 → 페이지 이동
- [ ] `grep "concept: " src/` 결과 0건 (단수 표기 제거 확인)

## 내부 메모

- BE Epic 1 Story 1-4 완료 후 FE 대응.
- 단수 `concept` 필드 이관 기간(1주) 동안 백엔드가 두 필드 응답 지원 시 FE 는 우선순위 `concepts` → `concept` fallback (1주 후 fallback 제거).

## Epic 기술 결정 / 대안

- **Chip 컴포넌트** — 자체 구현. Third-party(react-tag-input 등) 는 지양 (경량 유지).
- **정규화 규칙** — trim 만. 소문자 통일은 백엔드에 위임(현재 없음, 열린 질문 1).

---

## [Story 1-1] `lib/api/schemas/learningFacade.ts` 이관

### User Story

- As a FE 개발자
- I want Zod 스키마가 `concepts: string[]` 로 이관되기를
- so that 백엔드 응답 breaking 이 파싱 실패로 조기 감지된다

### 설명

> 출처: `issue-04-concept-list.md`

- `learningFacadeSchema` — `concept: z.string()` 삭제, `concepts: z.array(z.string().min(1).max(100)).min(1).max(5)` 추가
- `LearningFacadeRequest.Update` 도 동일 이관
- 단수 필드 fallback (1주간): `concept: z.string().optional()` (deprecated 표시)

**핵심 파일**:
- 수정: `src/lib/api/schemas/learningFacade.ts`

### 완료 기준 (AC)

- Given 백엔드 응답 `{concepts:["A","B"]}` / When 파싱 / Then 성공
- Given 백엔드 응답 `{concepts:[]}` / When 파싱 / Then Zod min(1) 실패 → Sentry 캡처
- Given 응답 `{concept:"legacy"}` (fallback 기간) / When 파싱 / Then optional 수용

### Definition of Done

- [ ] 스키마 수정
- [ ] Vitest 단위: 응답 파싱 케이스 4건
- [ ] MSW handler 갱신

### 스토리 포인트

0.5d

### 의존성

- 선행: BE Epic 1 Story 1-4 완료
- 후행: Story 1-2

---

## [Story 1-2] `<ConceptsInput>` Chip Input 컴포넌트

### User Story

- As a 신규 학습자
- I want 컨셉을 태그처럼 하나씩 추가·삭제하기를
- so that 여러 관점의 컨셉을 시각적으로 관리한다

### 설명

> 출처: `issue-04-concept-list.md`

`<ConceptsInput value onChange maxCount={5}>`:
- 입력 필드 + [엔터] / [쉼표] / [탭] 로 태그 추가
- 각 태그는 `<ConceptChip label onRemove />` 로 렌더
- 5개 도달 시 입력 비활성 + "최대 5개까지" 안내
- 중복 시도 시 인라인 flash ("이미 존재")
- 빈 태그 (`""`, `"   "`) 는 추가 무시

**핵심 파일**:
- 신규: `src/features/learning-facade/components/ConceptsInput.tsx`
- 신규: `src/features/learning-facade/components/ConceptChip.tsx`

### 완료 기준 (AC)

- Given empty input / When 사용자 "백엔드" 입력 + Enter / Then chip 1개 추가
- Given 5개 chip / When 6번째 입력 시도 / Then 입력 비활성 + 안내
- Given "백엔드" chip 있음 / When 사용자 "백엔드" 재입력 / Then 인라인 flash + 추가 무시
- Given trim 후 blank ("   ") 입력 / When / Then 추가 무시

### Definition of Done

- [ ] 컴포넌트 구현
- [ ] Vitest + Testing Library 단위 테스트 (6~8건)
- [ ] Storybook 스토리 (2가지 상태)

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 1-1
- 후행: Story 1-3

---

## [Story 1-3] `<ConceptsEditPage>` + `useConceptsMutation()` hook

### User Story

- As a 학습자
- I want 컨셉을 수정·저장하기를
- so that 학습 지도가 나의 관점을 반영한다

### 설명

> 출처: `issue-04-concept-list.md`

- `ConceptsEditPage` — `<ConceptsInput>` + [저장] 버튼
- `useConceptsMutation()` — `PATCH /learning-facade/concepts { concepts }`
- 저장 성공 → `/layers` 이동 (또는 이전 라우트)
- 409 (중복) → 인라인 에러 표시 후 chip 유지
- 400 (size invalid) → 인라인 안내

**핵심 파일**:
- 신규: `src/features/learning-facade/ConceptsEditPage.tsx`
- 신규: `src/features/learning-facade/hooks/useConceptsMutation.ts`
- 수정: `src/router.tsx` — `/learning-facade/concepts` 라우트 추가

### 완료 기준 (AC)

- Given 3 chip / When 저장 / Then 성공 → 이동
- Given 백엔드 409 응답 / When / Then 인라인 에러 노출, chip 유지
- Given 저장 중 / When 사용자 재클릭 / Then 중복 mutation 차단

### Definition of Done

- [ ] 페이지 · hook 구현
- [ ] MSW handler + Vitest 통합 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Story 1-2
- 후행: Story 1-4

---

## [Story 1-4] `LearningFacadePage` 상단에 concepts 노출 (읽기)

### User Story

- As a 학습자
- I want 지도 상단에서 내 컨셉을 즉시 확인하기를
- so that 현재 학습 방향을 상기한다

### 설명

> 출처: `issue-04-concept-list.md`

- `LearningFacadePage.tsx` 헤더에 `<ConceptsBar>` 컴포넌트
- concepts 를 chip 목록으로 read-only 렌더
- [편집] 아이콘 → `/learning-facade/concepts`

**핵심 파일**:
- 수정: `src/features/learning-facade/LearningFacadePage.tsx`
- 신규: `src/features/learning-facade/components/ConceptsBar.tsx`

### 완료 기준 (AC)

- Given concepts 3건 / When 페이지 진입 / Then chip 3개 read-only 표시
- Given [편집] 클릭 / When / Then `/learning-facade/concepts` 이동

### Definition of Done

- [ ] 컴포넌트 구현
- [ ] Vitest 렌더 테스트

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 1-3
- 후행: Story 1-5

---

## [Story 1-5] 단수 `concept` 필드 fallback 제거

### User Story

- As a FE 유지자
- I want 백엔드 breaking 이 완료된 후 단수 fallback 을 제거하기를
- so that 코드베이스에 폐기 표기가 잔존하지 않는다

### 설명

> 출처: `issue-04-concept-list.md`, ADR022

BE Epic 1 완료 + 1주 안정 관찰 후:
- `concept: z.string().optional()` 스키마 항목 삭제
- fallback 처리 로직 제거

### 완료 기준 (AC)

- Given 스키마 / When 코드 검사 / Then `concept:` (단수 field) 참조 0건
- Given 백엔드 응답 (`concept` 필드 없음) / When 파싱 / Then 성공

### Definition of Done

- [ ] fallback 제거
- [ ] `grep "concept:" src/` = 0

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 1-1~1-4 안정 관찰 1주
- 후행: 없음

---

# [Epic 2] Layer features 신설 + LayerProgressBadge

## Epic 목표

Layer 도메인 UI 를 `features/layers/` 로 신설하고, Layer 진행률 뱃지를 파생 표시.

## 배경

BE Epic 2 완료 시 `Layer` Aggregate + `learning_layer` 테이블 + `layer_id` FK 재배선. FE 는 `features/layers/` 신설, Axis 상위 계층으로 렌더.

## 완료 기준

- [ ] Story 6건 완료
- [ ] Zod 스키마 `layer.ts` 신설
- [ ] E2E: default "Uncategorized" Layer 표시 → Layer 생성 → Axis 이동
- [ ] LayerProgressBadge 3-state 색상 매핑 검증

## 내부 메모

- default "Uncategorized" Layer 는 BE 마이그레이션 결과로 자동 존재. FE 는 특별 표기 없이 목록 첫 번째로 렌더.

---

## [Story 2-1] `lib/api/schemas/layer.ts` + `useLayers()` hook

### User Story

- As a FE 개발자
- I want Layer Zod 스키마와 조회 hook 을 정의하기를
- so that 하위 컴포넌트가 Layer 를 표준 타입으로 소비한다

### 설명

> 출처: `issue-05-layer-server-domain.md`

`layerSchema`:
```ts
z.object({
  id: z.number(),
  name: z.string(),
  displayOrder: z.number(),
  progressStatus: z.enum(['NOT_STARTED','IN_PROGRESS','COMPLETED']),
  createdAt: z.string(),
})
```

`useLayers()` — `GET /layers` 목록, TanStack Query.

**핵심 파일**:
- 신규: `src/lib/api/schemas/layer.ts`
- 신규: `src/lib/api/endpoints/layer.ts`
- 신규: `src/features/layers/hooks/useLayers.ts`

### 완료 기준 (AC)

- Given API 응답 / When 파싱 / Then Layer[] 반환
- Given `useLayers()` / When 렌더 / Then 목록 로드

### Definition of Done

- [ ] 스키마 · endpoint · hook
- [ ] MSW handler + Vitest

### 스토리 포인트

1d

### 의존성

- 선행: BE Epic 2 Story 2-3 완료 (Layer 백필 후)
- 후행: Story 2-2

---

## [Story 2-2] `<LayersListPage>` — Layer 목록

### User Story

- As a 학습자
- I want Layer 목록을 확인하고 새 Layer 를 만들기를
- so that 축을 그룹핑해 학습을 조직화한다

### 설명

> 출처: `issue-05-layer-server-domain.md`

- `LayersListPage.tsx` — Layer 카드 그리드 + [+ 새 Layer] 버튼
- 각 Layer 카드: 이름 + progressStatus 뱃지 + 하위 axis 개수
- 정렬: displayOrder ASC
- `default "Uncategorized"` Layer 도 일반 카드로 렌더

**핵심 파일**:
- 신규: `src/features/layers/LayersListPage.tsx`
- 신규: `src/features/layers/components/LayerCard.tsx`
- 수정: `src/router.tsx` — `/layers` 라우트

### 완료 기준 (AC)

- Given Layer 3건 / When 페이지 진입 / Then 3 카드 렌더
- Given [+ 새 Layer] 클릭 / When / Then `<LayerFormDialog>` 열림

### Definition of Done

- [ ] 페이지 + 카드 컴포넌트
- [ ] Vitest 렌더 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Story 2-1
- 후행: Story 2-3

---

## [Story 2-3] `<LayerFormDialog>` — Layer 생성 / 수정

### User Story

- As a 학습자
- I want Layer 를 생성·수정하기를
- so that 축 그룹을 명명한다

### 설명

> 출처: `issue-05-layer-server-domain.md`

- 다이얼로그 + name input + 저장/취소
- 저장 시 `POST /layers { name }` 또는 `PATCH /layers/{id} { name }`
- 409 (`LAYER_NAME_DUPLICATE`) → 인라인 "이미 존재"
- 400 (`LAYER_NAME_BLANK`) → Zod 로 사전 차단

**핵심 파일**:
- 신규: `src/features/layers/components/LayerFormDialog.tsx`
- 신규: `src/features/layers/hooks/useLayerMutations.ts`

### 완료 기준 (AC)

- Given name="백엔드" / When 저장 / Then 201 + 다이얼로그 닫힘 + 목록 갱신
- Given 중복 이름 / When 저장 / Then 인라인 에러 + 다이얼로그 유지

### Definition of Done

- [ ] 다이얼로그 + hook
- [ ] MSW + Vitest

### 스토리 포인트

1d

### 의존성

- 선행: Story 2-2
- 후행: Story 2-4

---

## [Story 2-4] `<LayerProgressBadge>` — 진행률 뱃지

### User Story

- As a 학습자
- I want Layer 진행률을 색상으로 즉시 확인하기를
- so that 어느 Layer 에 집중해야 할지 판단한다

### 설명

> 출처: `issue-14-review-strategy-axis-layer-scope.md`, BE Story 6-4

- `<LayerProgressBadge status="NOT_STARTED|IN_PROGRESS|COMPLETED">`
- 색상: NOT_STARTED=회색 / IN_PROGRESS=주황 / COMPLETED=녹색
- 라벨: "미시작" / "진행 중" / "완료"
- 접근성: `aria-label={status}`

**핵심 파일**:
- 신규: `src/features/layers/components/LayerProgressBadge.tsx`

### 완료 기준 (AC)

- Given `status="NOT_STARTED"` / When 렌더 / Then 회색 뱃지 + "미시작"
- Given 각 status 렌더 / When Storybook / Then 3가지 색상 확인

### Definition of Done

- [ ] 컴포넌트
- [ ] Storybook 3 스토리
- [ ] Vitest 스냅샷

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 2-1
- 후행: 없음

---

## [Story 2-5] `<LayerDetailPage>` — Layer 상세

### User Story

- As a 학습자
- I want Layer 상세에서 하위 축 목록을 보기를
- so that 그룹 안의 학습 구조를 파악한다

### 설명

> 출처: `issue-05-layer-server-domain.md`

- `/layers/:layerId`
- 상단: Layer 이름 + progressStatus 뱃지 + [편집] · [삭제]
- 본문: 하위 Axis 카드 그리드 + [+ 새 축] 버튼
- 하단: [그룹 리뷰 시작] 버튼 → `/review/start?scope=LAYER&layerId=X`

**핵심 파일**:
- 신규: `src/features/layers/LayerDetailPage.tsx`

### 완료 기준 (AC)

- Given Layer 존재 / When 진입 / Then 상세 렌더
- Given [삭제] 클릭 + 하위 axis 존재 / When / Then `LAYER_HAS_ACTIVE_AXES` 다이얼로그 안내
- Given [그룹 리뷰 시작] / When / Then 리뷰 시작 페이지 이동

### Definition of Done

- [ ] 페이지
- [ ] 통합 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Story 2-1, 2-4
- 후행: Story 2-6

---

## [Story 2-6] `<LayerConfirmDeleteDialog>` — 하위 axis 있을 때 안내

### User Story

- As a 학습자
- I want Layer 삭제 시 하위 축이 있으면 안내 받기를
- so that 실수로 데이터를 손실하지 않는다

### 설명

> 출처: `issue-05-layer-server-domain.md`

- 다이얼로그: "축 X개가 있어 Layer 를 삭제할 수 없습니다. 먼저 다른 Layer 로 이동하거나 축을 삭제해주세요."
- [확인] 만 (닫기)
- 백엔드 409 응답을 트리거로 열림

**핵심 파일**:
- 신규: `src/features/layers/components/LayerConfirmDeleteDialog.tsx`

### 완료 기준 (AC)

- Given hasActiveAxes=true / When 삭제 시도 / Then 다이얼로그 열림
- Given hasActiveAxes=false / When 삭제 / Then 다이얼로그 없이 즉시 삭제 성공

### Definition of Done

- [ ] 다이얼로그
- [ ] 통합 테스트

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 2-5
- 후행: 없음

---

# [Epic 3] AxisDetailPage 재구성 + Roadmap 에디터 + Selection 정책 UI

## Epic 목표

`AxisTopic` 하위 계층을 폐기하고 Axis 상세에 Roadmap 에디터(1) + Selection 목록(N, 정책 준수) 을 배치한다.

## 배경

BE Epic 3 완료 시 `AxisTopic` soft-deprecate, `AxisRoadmap` / `AxisSelection` 도입. FE 는 AxisDetailPage 재구성 + Topic 관련 컴포넌트 삭제.

## 완료 기준

- [ ] Story 8건 완료
- [ ] `TopicPicker`, `TopicTree`, `TopicCard` 등 컴포넌트 삭제
- [ ] Zod `topic.ts` 삭제, `axisRoadmap.ts` / `axisSelection.ts` 신설
- [ ] E2E: Axis 상세 → Roadmap 편집 → Selection 3건 추가 → Selection 삭제 confirm

---

## [Story 3-1] `lib/api/schemas/axisRoadmap.ts` + `axisSelection.ts`

### User Story

- As a FE 개발자
- I want Roadmap · Selection Zod 스키마를 정의하기를
- so that 하위 컴포넌트가 표준 타입으로 소비한다

### 설명

> 출처: `issue-06-roadmap-selections-dualaxis.md`, `issue-11-selections-version-policy.md`

`axisRoadmapSchema`:
```ts
z.object({
  id: z.number(),
  axisId: z.number(),
  content: z.string().min(1).max(2000),
  updatedAt: z.string(),
})
```

`axisSelectionSchema`:
```ts
z.object({
  id: z.number(),
  axisId: z.number(),
  name: z.string().min(1).max(100),
  content: z.string().min(1).max(2000),
  createdAt: z.string(),
})
```

정렬: `list.sort((a,b) => b.createdAt.localeCompare(a.createdAt))` — 백엔드가 DESC 반환이지만 재검증.

**핵심 파일**:
- 신규: `src/lib/api/schemas/axisRoadmap.ts`
- 신규: `src/lib/api/schemas/axisSelection.ts`
- 신규: `src/lib/api/endpoints/axisRoadmap.ts`
- 신규: `src/lib/api/endpoints/axisSelection.ts`
- 삭제 예정: `src/lib/api/schemas/topic.ts` (Story 3-8 에서)

### 완료 기준 (AC)

- Given 응답 파싱 / When 정상 / Then 스키마 통과
- Given content 2001자 / When 파싱 / Then Zod 실패

### Definition of Done

- [ ] 스키마 2개 + endpoint 2개
- [ ] Vitest

### 스토리 포인트

1d

### 의존성

- 선행: BE Epic 3 완료
- 후행: Story 3-2

---

## [Story 3-2] `<AxisDetailPage>` 재구성 (3-탭)

### User Story

- As a 학습자
- I want Axis 상세에서 Roadmap / Selection / Cards 3-탭을 확인하기를
- so that 축의 헌법·사례·카드를 한 곳에서 관리한다

### 설명

> 출처: `issue-06-roadmap-selections-dualaxis.md`

- `/axes/:axisId` — 3-탭 (Roadmap 기본)
- 상단: Axis 이름 + progressStatus + mode 표시
- 탭 전환은 URL query (`?tab=roadmap|selections|cards`) 로 상태 유지

**핵심 파일**:
- 수정: `src/features/axes/AxisDetailPage.tsx`
- 신규: `src/features/axes/components/AxisTabsHeader.tsx`

### 완료 기준 (AC)

- Given axis 존재 / When 진입 / Then Roadmap 탭 기본 렌더
- Given ?tab=selections / When / Then Selection 탭 렌더
- Given 탭 전환 / When / Then URL 갱신

### Definition of Done

- [ ] 페이지 재구성
- [ ] 통합 테스트

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 3-1
- 후행: Story 3-3, 3-4

---

## [Story 3-3] `<AxisRoadmapEditor>` — ASCII 텍스트 에디터

### User Story

- As a 학습자
- I want 축의 로드맵을 자유 텍스트로 편집하기를
- so that 축이 다루는 개념 트리를 명시적으로 표현한다

### 설명

> 출처: `issue-06-roadmap-selections-dualaxis.md`

- `<textarea rows={12} maxLength={2000}>` + placeholder ASCII 트리 예시
- 상단: 문자 카운터 (`{n} / 2000`)
- 하단: [저장] · [취소] 버튼
- 저장: `PUT /axes/{id}/roadmap { content }` (idempotent)
- 없는 상태: 빈 에디터 + "로드맵을 입력해 축의 헌법을 표현하세요" 힌트
- AI 초안 요청 버튼 (`product-ai-suggestion.md` 협력, Epic 3 3-Story 결과)

**핵심 파일**:
- 신규: `src/features/axes/components/AxisRoadmapEditor.tsx`
- 신규: `src/features/axes/hooks/useAxisRoadmap.ts`

### 완료 기준 (AC)

- Given roadmap 없음 / When 진입 / Then 빈 에디터 + 힌트
- Given content 저장 / When / Then 성공 + 문자 카운터 유지
- Given 2001자 입력 시도 / When / Then maxLength 로 사전 차단
- Given 저장 후 재진입 / When / Then content 로드

### Definition of Done

- [ ] 에디터 컴포넌트 + hook
- [ ] MSW + Vitest

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 3-2
- 후행: 없음

---

## [Story 3-4] `<AxisSelectionsList>` — DESC 정렬 목록

### User Story

- As a 학습자
- I want 축의 사례/응용 목록을 최신순으로 보기를
- so that 최근 등록한 사례부터 확인한다

### 설명

> 출처: `issue-06-roadmap-selections-dualaxis.md`, `issue-11-selections-version-policy.md`

- `<AxisSelectionsList axisId={id}>` — `useAxisSelections(axisId)` 목록
- 각 항목: `<AxisSelectionCard>` (name + content preview + [편집] · [삭제])
- 정렬: `createdAt DESC` (백엔드 계약 + 재검증)
- 빈 상태: "아직 사례가 없습니다. [+ 새 사례 추가]"
- 상단: [+ 새 사례 추가] 버튼

**핵심 파일**:
- 신규: `src/features/axes/components/AxisSelectionsList.tsx`
- 신규: `src/features/axes/components/AxisSelectionCard.tsx`
- 신규: `src/features/axes/hooks/useAxisSelections.ts`

### 완료 기준 (AC)

- Given selection 3건 / When 렌더 / Then DESC 정렬 확인
- Given 빈 상태 / When / Then 빈 안내 표시

### Definition of Done

- [ ] 목록 + 카드
- [ ] Vitest

### 스토리 포인트

1d

### 의존성

- 선행: Story 3-2
- 후행: Story 3-5, 3-6

---

## [Story 3-5] `<AxisSelectionForm>` — 추가/편집 (in-place, 중복 방지)

### User Story

- As a 학습자
- I want 사례를 추가하거나 in-place 편집하기를
- so that 사례 관리가 흐름 안에서 이루어진다

### 설명

> 출처: `issue-11-selections-version-policy.md`

- 추가: [+ 새 사례 추가] → `<AxisSelectionForm mode="create">` 인라인 폼
- 편집: `<AxisSelectionCard>` 의 [편집] → `<AxisSelectionForm mode="edit">` 인라인 폼 (같은 카드 자리)
- Submit → `POST /selections` 또는 `PATCH /selections/{id}`
- 409 `AXIS_SELECTION_NAME_DUPLICATE` → 인라인 에러 "다른 이름으로 시도해주세요"
- 취소 → 원래 카드로 복귀

**핵심 파일**:
- 신규: `src/features/axes/components/AxisSelectionForm.tsx`

### 완료 기준 (AC)

- Given 새 사례 추가 / When 저장 / Then 목록 상단에 추가 (DESC)
- Given 편집 저장 / When / Then in-place 갱신
- Given 중복 이름 / When 저장 / Then 인라인 에러 + 폼 유지

### Definition of Done

- [ ] Form 컴포넌트
- [ ] MSW + Vitest

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 3-4
- 후행: Story 3-6

---

## [Story 3-6] `<AxisSelectionDeleteConfirm>` — hard delete 확인

### User Story

- As a 학습자
- I want 사례 삭제 시 복원 불가 안내를 받기를
- so that 실수 삭제를 방지한다

### 설명

> 출처: `issue-11-selections-version-policy.md`

- [삭제] 버튼 클릭 → `<ConfirmDialog>` 열림
- "삭제하면 복원할 수 없습니다. 정말 삭제하시겠어요?"
- [삭제] · [취소]
- 확인 → `DELETE /selections/{id}` → 204 → 목록에서 제거

**핵심 파일**:
- 신규: `src/features/axes/components/AxisSelectionDeleteConfirm.tsx`

### 완료 기준 (AC)

- Given [삭제] 클릭 / When / Then confirm 다이얼로그 열림
- Given [삭제] 확인 / When / Then 204 + 목록에서 제거
- Given [취소] / When / Then 다이얼로그 닫힘, 항목 유지

### Definition of Done

- [ ] Confirm 다이얼로그
- [ ] 통합 테스트

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 3-4, 3-5
- 후행: 없음

---

## [Story 3-7] AxisTopic 관련 컴포넌트 삭제

### User Story

- As a FE 유지자
- I want Topic 관련 폐기 컴포넌트가 완전 제거되기를
- so that 코드베이스에 폐기 참조가 잔존하지 않는다

### 설명

> 출처: `issue-06-roadmap-selections-dualaxis.md`, ADR022

삭제:
- `src/features/axes/components/TopicPicker.tsx`
- `src/features/axes/components/TopicTree.tsx`
- `src/features/axes/components/TopicCard.tsx`
- 관련 hook `useAxisTopics.ts` 등
- `src/lib/api/schemas/topic.ts`
- `src/lib/api/endpoints/topic.ts`

### 완료 기준 (AC)

- Given `grep -r "AxisTopic\|topic_id\|useAxisTopics" src/` / When 실행 / Then 폐기 마이그레이션 문맥 외 0건

### Definition of Done

- [ ] 파일 삭제
- [ ] 잔존 참조 grep 검증

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 3-1~3-6 (신규 UI 정착)
- 후행: 없음

---

## [Story 3-8] AI 초안 요청 버튼 배치 (product-ai-suggestion 협력)

### User Story

- As a 학습자
- I want Roadmap/Selection 편집 UI 에서 AI 초안 요청 버튼을 사용하기를
- so that 편집 부담이 낮아진다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`, `product-ai-suggestion.md` (FE) 협력

- `<AxisRoadmapEditor>` 상단에 [🪄 AI 초안 요청] 버튼
- `<AxisSelectionsList>` 상단에 [🪄 사례 초안 요청] 버튼
- 실제 API 호출은 `product-ai-suggestion.md` (FE) 의 `<RoadmapSuggestButton>` / `<SelectionsSuggestButton>` 재사용

### 완료 기준 (AC)

- Given 버튼 클릭 / When / Then Suggestion 흐름 진입 (별도 Product 담당)

### Definition of Done

- [ ] 버튼 배치
- [ ] `product-ai-suggestion.md` (FE) Epic 참조 완료

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 3-3, 3-4, `product-ai-suggestion.md` (FE) 완료
- 후행: 없음

---

# [Epic 4] Card 편집기 axis 직접 + Topic Picker 삭제

## Epic 목표

Card 편집기에서 topic 계층을 제거하고 axis 를 직접 선택하도록 이관.

## 배경

BE Epic 4 완료 시 `card.axis_id NOT NULL` + `topic_id` soft-deprecate. FE 는 Card 편집 UI 를 axis 직접 매핑으로 이관.

## 완료 기준

- [ ] Story 4건 완료
- [ ] Zod `cardSchema` — `topicId` 삭제, `axisId: number` (required)
- [ ] `<TopicPicker>` 관련 참조 0건 (Epic 3 Story 3-7 과 연동)

---

## [Story 4-1] `lib/api/schemas/card.ts` 이관

### User Story

- As a FE 개발자
- I want Card 스키마의 topicId 를 삭제하고 axisId 를 required 로 이관하기를
- so that 스키마가 백엔드 계약과 정합

### 설명

> 출처: `issue-07-card-axis-direct-mapping.md`

- `cardSchema.topicId: z.number().optional()` → 삭제
- `cardSchema.axisId: z.number()` (required)
- 마이그레이션 기간(BE Epic 4 Story 4-3 완료 전) 동안 optional 유지, 완료 후 required 승격

### 완료 기준 (AC)

- Given 응답 `{axisId: 42}` / When 파싱 / Then 성공
- Given 응답 `{axisId: null}` / When 파싱 / Then Zod 실패

### Definition of Done

- [ ] 스키마 이관
- [ ] Vitest

### 스토리 포인트

0.5d

### 의존성

- 선행: BE Epic 4 완료
- 후행: Story 4-2

---

## [Story 4-2] `<CardEditor>` axis 직접 선택

### User Story

- As a 학습자
- I want 카드 작성 시 축을 직접 선택하기를
- so that Topic 계층 없이 즉시 카드 등록

### 설명

> 출처: `issue-07-card-axis-direct-mapping.md`

- `<CardEditor>` 안의 `<TopicPicker>` 제거
- `<AxisSelect layerId={currentLayerId}>` 배치 (Layer 내 축 목록)
- 축 상세 페이지 진입 시 axis 기본값 = 현재 축 (편집 불가)
- `/cards/new` 진입 시 `<LayerSelect> + <AxisSelect>` 계층 선택

**핵심 파일**:
- 수정: `src/features/cards/CardEditor.tsx`
- 신규: `src/features/cards/components/AxisSelect.tsx`

### 완료 기준 (AC)

- Given AxisDetailPage → [+ 새 카드] / When / Then axis 기본값=현재 축 (편집 불가)
- Given /cards/new / When / Then LayerSelect + AxisSelect 선택 흐름
- Given 저장 / When axis 미선택 / Then 400 `CARD_AXIS_REQUIRED` 인라인 안내

### Definition of Done

- [ ] Editor 리팩토링
- [ ] AxisSelect 컴포넌트
- [ ] 통합 테스트

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 4-1
- 후행: Story 4-3

---

## [Story 4-3] Legacy `topic_id` 참조 카드 안내 배너

### User Story

- As a 학습자
- I want 마이그레이션 과도기 카드에 대해 명확한 안내 받기를
- so that 오류 화면 없이 상황을 이해한다

### 설명

> 출처: `issue-07-card-axis-direct-mapping.md`

- 백엔드가 `LEGACY_TOPIC_REFERENCE` 410 을 반환하는 경우 (Epic 4 마이그레이션 기간)
- `<CardDetailPage>` 에서 안내 배너: "이 카드는 마이그레이션 대상입니다. 관리자에게 문의하세요."
- Sentry 로 캡처

### 완료 기준 (AC)

- Given 410 응답 / When 페이지 진입 / Then 배너 표시
- Given Sentry / When 상황 발생 / Then 이벤트 캡처

### Definition of Done

- [ ] 배너 컴포넌트
- [ ] Sentry 통합

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 4-1
- 후행: 없음

---

## [Story 4-4] Topic Picker · 관련 잔존 참조 최종 삭제

### User Story

- As a FE 유지자
- I want Topic 관련 파일이 완전 제거되기를
- so that grep 오탐 없이 코드가 깨끗해진다

### 설명

> 출처: `issue-07-card-axis-direct-mapping.md`

- Epic 3 Story 3-7 에서 `features/axes/` 하위 Topic 컴포넌트 삭제 완료.
- 본 Story 는 `features/cards/`, `features/study/` 등 잔존 참조 최종 정리.

### 완료 기준 (AC)

- Given `grep -r "topicId\|TopicPicker\|Topic\b" src/` / When 실행 / Then 폐기 문맥 외 0건

### Definition of Done

- [ ] 잔존 참조 정리
- [ ] CI grep 검증

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 4-2, Epic 3 Story 3-7
- 후행: 없음

---

# [Epic 5] Deck 라우트 리디렉트 + features/decks 삭제

## Epic 목표

`/decks/*` 라우트를 `/axes/*` 로 리디렉트하고 `features/decks/` 를 삭제한다.

## 배경

BE Epic 5 완료 시 Deck BC 폐기. FE 도 라우트·컴포넌트·스키마 정리.

## 완료 기준

- [ ] Story 4건 완료
- [ ] `/decks/*` 라우트 <Navigate> 로 대체
- [ ] `features/decks/` 삭제 (Redirect 컴포넌트만 잔존)
- [ ] Zod `deck.ts` 삭제

---

## [Story 5-1] `<DecksRedirect>` — `/decks/*` → `/axes/*`

### User Story

- As a 저장 URL 을 가진 사용자
- I want 기존 Deck URL 을 열면 자동으로 Axis 로 이동하기를
- so that 북마크가 소리없이 실패하지 않는다

### 설명

> 출처: `issue-13-deck-abolition-axis-absorption.md`

- `<DecksRedirect>` — `useParams()` 로 `:id` 추출 후 `<Navigate replace to={`/axes/${id}`}>`
- `/decks` (목록) → `<Navigate replace to="/layers">` (Layer 목록)
- Sentry 로 리디렉트 발생 카운트 로그

**핵심 파일**:
- 신규: `src/features/decks/DecksRedirect.tsx`
- 수정: `src/router.tsx` — `/decks/*` 라우트 → `<DecksRedirect>`

### 완료 기준 (AC)

- Given `/decks/42` 진입 / When / Then `/axes/42` 로 이동
- Given `/decks` 진입 / When / Then `/layers` 로 이동
- Given Sentry / When 리디렉트 발생 / Then breadcrumb 기록

### Definition of Done

- [ ] Redirect 컴포넌트
- [ ] 라우트 갱신
- [ ] Vitest

### 스토리 포인트

0.5d

### 의존성

- 선행: BE Epic 5 Story 5-3 완료
- 후행: Story 5-2

---

## [Story 5-2] `features/decks/` 컴포넌트 삭제

### User Story

- As a FE 유지자
- I want Deck 관련 컴포넌트가 코드베이스에서 제거되기를
- so that 폐기 컴포넌트가 남지 않는다

### 설명

> 출처: `issue-13-deck-abolition-axis-absorption.md`

삭제:
- `DecksPage.tsx`, `DeckDetailPage.tsx`
- `components/AxisDeckGroup.tsx`, `OrphanDeckSection.tsx`, `DeckCard.tsx`
- `hooks/useDecks.ts`, `useDeckMutations.ts`
- `DeckProvider` 관련 (App.tsx 갱신)

`DecksRedirect.tsx` 만 잔존.

### 완료 기준 (AC)

- Given `grep -r "features/decks" src/` / When 실행 / Then Redirect 만 참조

### Definition of Done

- [ ] 파일 삭제
- [ ] App.tsx 정리

### 스토리 포인트

1d

### 의존성

- 선행: Story 5-1
- 후행: Story 5-3

---

## [Story 5-3] Axis 흡수 필드 UI 반영

### User Story

- As a 학습자
- I want Deck 에 있던 mode/onLibrary/publishedAt/progressStatus 를 Axis 상세에서 확인·수정하기를
- so that Deck 라우트 없이도 기존 기능이 유지된다

### 설명

> 출처: `issue-13-deck-abolition-axis-absorption.md`

- `<AxisDetailPage>` 상단에 mode 토글, onLibrary 체크박스, publishedAt 표시
- `PATCH /axes/{id}` 로 갱신 (백엔드 Epic 5 Story 5-1 결과)

### 완료 기준 (AC)

- Given axis 상세 진입 / When 렌더 / Then mode/onLibrary/publishedAt 정보 표시
- Given `PATCH /axes/{id} { mode:"REVIEW" }` / When / Then 갱신 성공

### Definition of Done

- [ ] UI 통합
- [ ] 통합 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Story 5-2
- 후행: Story 5-4

---

## [Story 5-4] Zod `deck.ts` 스키마 삭제

### User Story

- As a FE 개발자
- I want Deck 스키마가 완전 제거되기를
- so that 잔존 타입 참조가 없다

### 설명

> 출처: `issue-13-deck-abolition-axis-absorption.md`

- `src/lib/api/schemas/deck.ts` 삭제
- `src/lib/api/endpoints/deck.ts` 삭제
- 타입스크립트 컴파일 에러 → 잔존 참조 발견

### 완료 기준 (AC)

- Given TypeScript 컴파일 / When / Then 성공 (참조 0건)

### Definition of Done

- [ ] 파일 삭제
- [ ] tsc 성공

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 5-3
- 후행: 없음

---

# [Epic 6] Review scope 토글 + Layer 스코프 세션 진입

## Epic 목표

Review 세션 시작 화면에 `<ReviewScopeToggle>` 을 배치하고 Layer 스코프 세션 진입을 지원한다.

## 배경

BE Epic 6 완료 시 `ReviewSession.scope: AXIS|LAYER` + `/layers/{id}/review-sessions` 엔드포인트. FE 는 UI 이중 스코프 지원.

## 완료 기준

- [ ] Story 5건 완료
- [ ] Zod `reviewSession.ts` 에 scope 필드 추가
- [ ] E2E: Axis 스코프 세션 + Layer 스코프 세션 각각 시작

---

## [Story 6-1] `lib/api/schemas/reviewSession.ts` 이관

### User Story

- As a FE 개발자
- I want ReviewSession 스키마가 scope 필드를 포함하기를
- so that AXIS/LAYER 두 스코프를 타입 안전하게 소비한다

### 설명

> 출처: `issue-14-review-strategy-axis-layer-scope.md`

- `reviewSessionSchema.scope: z.enum(['AXIS','LAYER'])`
- `.scopeId: z.number()` (axisId 또는 layerId)
- 기존 `deckId` 필드 삭제 (Epic 5 결과)

### 완료 기준 (AC)

- Given 응답 파싱 / When 정상 / Then 성공
- Given scope 잘못됨 / When 파싱 / Then Zod 실패 + Sentry

### Definition of Done

- [ ] 스키마 이관
- [ ] Vitest

### 스토리 포인트

0.5d

### 의존성

- 선행: BE Epic 6 Story 6-1 완료
- 후행: Story 6-2

---

## [Story 6-2] `<ReviewScopeToggle>` 컴포넌트

### User Story

- As a 학습자
- I want 리뷰 시작 시 축 또는 그룹 스코프를 선택하기를
- so that 원하는 학습 범위로 리뷰를 시작한다

### 설명

> 출처: `issue-14-review-strategy-axis-layer-scope.md`

- `<ReviewScopeToggle value onChange>` — 2개 토글 버튼 (축 / 그룹)
- 값에 따라 `<AxisSelect>` 또는 `<LayerSelect>` 렌더
- 기본값: "AXIS"

**핵심 파일**:
- 신규: `src/features/review/components/ReviewScopeToggle.tsx`
- 신규: `src/features/review/components/LayerSelect.tsx`

### 완료 기준 (AC)

- Given 토글 AXIS / When / Then AxisSelect 렌더
- Given 토글 LAYER / When / Then LayerSelect 렌더

### Definition of Done

- [ ] 컴포넌트
- [ ] Storybook 2 스토리
- [ ] Vitest

### 스토리 포인트

1d

### 의존성

- 선행: Story 6-1
- 후행: Story 6-3

---

## [Story 6-3] `<ReviewStartPage>` 재구성

### User Story

- As a 학습자
- I want 리뷰 시작 화면에서 스코프·대상·시작을 한 화면에서 처리하기를
- so that 진입 후 즉시 세션 시작

### 설명

> 출처: `issue-14-review-strategy-axis-layer-scope.md`

- `/review/start`
- `<ReviewScopeToggle>` + 스코프별 Select + [시작] 버튼
- 시작 시:
  - AXIS → `POST /axes/{id}/review-sessions`
  - LAYER → `POST /layers/{id}/review-sessions`
- 성공 시 `/review/sessions/:sessionId` 이동
- 400 `LAYER_HAS_NO_AXES` → 다이얼로그 "Layer 에 축이 없습니다"

**핵심 파일**:
- 수정: `src/features/review/ReviewStartPage.tsx`

### 완료 기준 (AC)

- Given AXIS 선택 + axis 선택 → 시작 / When / Then 세션 생성 + 이동
- Given LAYER 선택 + layer(axis 0건) 선택 → 시작 / When / Then 400 다이얼로그
- Given LAYER 선택 + layer(axis N건) → 시작 / When / Then 세션 생성 + 이동

### Definition of Done

- [ ] 페이지 재구성
- [ ] MSW + 통합 테스트

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 6-2
- 후행: Story 6-4

---

## [Story 6-4] `<ReviewScopeLabel>` — 세션 헤더 라벨

### User Story

- As a 학습자
- I want 리뷰 세션 상단에 스코프 라벨을 확인하기를
- so that 현재 리뷰 범위를 즉시 인지한다

### 설명

> 출처: `issue-14-review-strategy-axis-layer-scope.md`, ADR022 열린 질문 5

- `<ReviewScopeLabel scope scopeName>` — "축 리뷰: {axisName}" 또는 "그룹 리뷰: {layerName}"
- `<ReviewSessionPage>` 상단에 배치
- "Layer 1" 등 애매한 표기 폐기 (문구 정책 반영)

**핵심 파일**:
- 신규: `src/features/review/components/ReviewScopeLabel.tsx`
- 수정: `src/features/review/ReviewSessionPage.tsx`

### 완료 기준 (AC)

- Given scope=AXIS, axisName="Java" / When 렌더 / Then "축 리뷰: Java"
- Given scope=LAYER, layerName="백엔드" / When / Then "그룹 리뷰: 백엔드"
- Given "Layer 1" 표기 검색 / When grep / Then 폐기 문맥 외 0건

### Definition of Done

- [ ] 라벨 컴포넌트
- [ ] 페이지 통합
- [ ] "Layer 1" 잔존 참조 grep 검증

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 6-3
- 후행: 없음

---

## [Story 6-5] `docs/ux/wip-language.md` 갱신 협력

### User Story

- As a UX 라이터
- I want 리뷰 라벨 매핑이 문서화되기를
- so that 향후 UI 변경 시 매핑 근거가 있다

### 설명

> 출처: `issue-14-review-strategy-axis-layer-scope.md`, ADR022 열린 질문 5

- `docs/ux/wip-language.md` 갱신: AXIS/LAYER 스코프 라벨 매핑
- "Layer 1" 폐기, "그룹 리뷰: X" 로 대체

### 완료 기준 (AC)

- Given wip-language.md / When 조회 / Then AXIS/LAYER 매핑 존재

### Definition of Done

- [ ] 문서 갱신

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 6-4
- 후행: 없음

---

*작성일: 2026-07-01 | 상태: **6 Epic · 33 Story 전체 pending** | Milestone 후보: FE milestone 1 (BE product-learning-tower 대응). Epic 순차 진행, BE Epic 별 배포 후 FE 대응.*
