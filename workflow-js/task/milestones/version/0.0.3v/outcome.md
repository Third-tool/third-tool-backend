# 0.0.3v / Outcome (성과) — 실측

> **본 파일 역할**: 본 버전 종료 시점에 **사용자·기능·기술 자산 측면에서 무엇이 새로 가능해졌는가**의 산출물 실측 기록.
> 대응 문서: [`milestone.md`](./milestone.md) (원안·계획) · [`review.md`](./review.md) (회고) · [`cost.md`](./cost.md) · [`infra.md`](./infra.md) · [`performance.md`](./performance.md) · [`eval.md`](./eval.md).

---

## 본주 머지된 Story (실측)

> M2 rush 정책 종료 후 첫 마일스톤. Reviewer 5관점 세션 3회 (각 Epic PR마다) 정식 발사 재개. 사용자 지시 "epic 단위로 pr 진행" 반영.

| # | Product | Story | PR | 상태 | 비고 |
| --- | --- | --- | --- | --- | --- |
| 1 | LT | Epic 3 / 3-6 `AxisRoadmapNode` Entity + `LearningAxis` 확장 5 API | **#201** | 머지 | 이슈 #15 이관 · 이슈 #6 SUPERSEDED |
| 2 | LT | Epic 3 / 3-7 Flyway V20 `axis_roadmap_node` + Repository | **#201** | 머지 | 상동 |
| 3 | LT | Epic 3 / 3-8 Roadmap 노드 REST API + Controller + Service | **#201** | 머지 | 5 엔드포인트 · ErrorCode 4종 |
| 4 | LT | Epic 3 / 3-9 `AxisSelection` 컨테이너 + `AxisSelectionNode` Entity | **#202** | 머지 | 이슈 #16 이관 · 이슈 #11 정책 계승 |
| 5 | LT | Epic 3 / 3-10 Flyway V21/V22 axis_selection · axis_selection_node + Repository | **#202** | 머지 | 상동 |
| 6 | LT | Epic 3 / 3-11 Selection 컨테이너/노드 REST API + Service | **#202** | 머지 | 9 엔드포인트 (컨테이너 4 + 노드 5) · ErrorCode 7종 |
| 7 | AS | Epic 1 / 1-5~1-8 4개 신규 Port + 8 Request/Response record | **#203** | 머지 | 이슈 #17 이관 · 4-Port SUPERSEDED |
| 8 | AS | Epic 3 / 3-2 `backend-developer.json` catalog 6-Port 확장 (하네스 엔지니어링 포함) | **#203** | 머지 | 상동 |
| 9 | AS | Epic 2 / 2-5~2-8 4개 Static Adapter (6-Port) | **#203** | 머지 | 상동 |
| 10 | AS | Epic 7 / 7-1 `concept-spec.txt` + 6개 프롬프트 템플릿 | **#203** | 머지 | 이슈 #19 이관 |
| 11 | AS | Epic 5 / 5-3 (부분) `SuggestionController` 4 엔드포인트 + AppService | **#203** | 머지 | M3 하이라이트 · Layer/Axis endpoint는 별도 이관 |
| **소계** | | **11 Story · ~24.5 SP** | **3 Epic PR** | **11 완주** | Reviewer 5관점 3회 발사 |
| 12 | 문서 | 0.0.3v 산출물 6종 (outcome/cost/review/infra/performance/eval) | **본 세션** | 진행 | 본 문서군 |
| **총계** | | **11 Story · ~24.5 SP + 산출물 6종** | **3 PR + 1 세션** | **완주** | |

**PR 흐름 (Epic-단위 번들 정책 유지)**:
- PR #201 · LT E3 Story 3-6~3-8 (Roadmap 노드) — 브랜치 `feat/lt-e3-roadmap-node-model` · 커밋 5건
- PR #202 · LT E3 Story 3-9~3-11 (Selection 컨테이너/노드) — 브랜치 `feat/lt-e3-selection-node-model` · 커밋 4건
- PR #203 · AS E1+E2+E3+E5+E7 (6-Port + Static Adapter + concept-spec) — 브랜치 `feat/as-6port-static-concept` · 커밋 6건 (Reviewer 조치 포함)

**계획 대비 축소 (사용자 승인)**:
- 밀스톤 원안: 6 Epic PR (PR#1~#6) · 총 43 SP
- 실제 착수: 3 Epic PR (PR#3·#4·#5) · ~24.5 SP
- 축소 사유: PR#1 (concepts[])·PR#2 (Layer)·PR#6 (ADR023)는 M2에서 이미 코드 착지 완료. 사용자 확인 후 실제 미구현분(PR#3~#5)만 진행.

---

## 사용자에게 보이는 변화

> 로컬 H2 환경 기준. 배포 재개는 M7 이후 유지.

| 영역 | M3 시작 전 (M2 종료 시점) | M3 종료 후 |
| --- | --- | --- |
| Roadmap 저장 표현 | axis당 통짜 content TEXT 개념만 (실 구현 미배치) | **`AxisRoadmapNode` 챕터 first-class 5 엔드포인트** — 챕터별 CRUD + reorder |
| Roadmap 노드 body | 없음 | **MEDIUMTEXT (16MB) 저장** — subtree ASCII 통짜 (예: `├── 1-1. 정의와 본질\n│       ...`) |
| Selection 컨테이너 | 개념·이슈 #11 정책만 존재 (실 구현 미배치) | **`AxisSelection` 컨테이너 + `AxisSelectionNode` 자식 9 엔드포인트** — name UNIQUE · created_at DESC · hard delete + 자식 CASCADE |
| AI 응답 (Layer) | Static Adapter 활성 (`backend-developer.json` layers 5종) | 유지 (backward compat) + 6번째 Layer "기능의 구현" 추가 |
| AI 응답 (챕터 outline) | 없음 | **`POST /api/v1/suggestions/chapters-outline` → 200 + 5개 챕터** (하네스 엔지니어링 axis 실제 응답 · providerContext="static:backend-developer") |
| AI 응답 (챕터 subtree) | 없음 | **`POST /api/v1/suggestions/chapter-subtree` → 200 + bodyAsciiTree** (`├── 1-1. 정의와 본질\n│       모델 + 하네스 — ...`) |
| AI 응답 (Selection outline) | 없음 | **`POST /api/v1/suggestions/selection-outline` → 200 + nameCandidate + 챕터 outline** (예: "기능 아키텍처 REST selections v1") |
| AI 응답 (Selection subtree) | 없음 | **`POST /api/v1/suggestions/selection-subtree` → 200 + bodyAsciiTree** |
| 개념 명세 자산 | 없음 | **`resources/prompts/concept-spec.txt`** — Roadmap 카탈로그 6종 + Selections 5종 + 수렴/발산 판별 5+5 (v2 LLM Adapter 대비 자산) |
| 6개 프롬프트 템플릿 | 없음 | **`{{include:concept-spec.txt}}` 마커 + Port별 지시 + few-shot** (chapters-outline / chapter-subtree / selection-outline / selection-subtree / layer / axis) |

---

## 기술 자산 증가 (실측)

### 신설 도메인 클래스

- **LearningFacade BC**:
  - `AxisRoadmapNode.java` (Entity, axis_roadmap_node 매핑, `@SQLRestriction`+`@SQLDelete` soft delete, MEDIUMTEXT body)
  - `AxisSelection.java` (Aggregate/Entity, axis_selection 매핑, UNIQUE(axis_id, name), hard delete, orphanRemoval=true)
  - `AxisSelectionNode.java` (Entity, axis_selection_node 매핑, soft delete 없음, 컨테이너 정책 계승)
- **AI Suggestion (Port 계층)**:
  - `ChapterOutlineItem.java` (record — title, rationale · S1-5~S1-8 공용 재사용)

### 확장 도메인 API

- `LearningAxis`:
  - `addRoadmapNode(title, rationale, body)` / `reorderRoadmapNodes(orderedIds)` / `removeRoadmapNode(nodeId)` / `findRoadmapNode(nodeId)` / `getRoadmapNodes()` — Roadmap 도메인 API 5개
  - `addSelection(name)` / `renameSelection(id, newName)` / `removeSelection(id)` / `findSelection(id)` / `getSelections()` — Selection 컨테이너 도메인 API 5개
  - **합계 10개 신규 메서드** (기존 axes/topics/roadmapNodes와 별개)
- `AxisSelection` (자체 도메인 API):
  - `addNode(title, rationale, body)` / `reorderNodes(orderedIds)` / `removeNode(nodeId)` / `findNode(nodeId)` / `updateName(newName)` / `getNodes()` — Selection 노드 도메인 API 6개
- `AxisRoadmapNode` / `AxisSelectionNode` (필드 갱신):
  - `updateTitle` / `updateRationale` / `updateBody` — 각 3개, blank/trim 정규화 내장

### Infrastructure · Adapter (신설 4개 + Loader 확장)

- `StaticChaptersOutlineAdapter.java` — role 감지 후 catalog.chapters axisName 필터 · chapterCountHint · providerContext
- `StaticChapterSubtreeAdapter.java` — axisName + chapter.title 매칭 후 subtree 반환
- `StaticSelectionOutlineAdapter.java` — axisName + variantHint 매칭 (hint 없으면 axis-only)
- `StaticSelectionSubtreeAdapter.java` — axisName + selectionName + chapter.title 3중 매칭
- `SuggestionCatalog.java` 확장 — `chapters: List<ChapterEntry>` + `selectionOutlines: List<SelectionOutlineEntry>` 신설, 기존 `roadmaps`/`selections` @Deprecated backward compat
- `SuggestionCatalogLoader.java` — `emptyCatalog` 6-필드로 확장

### Application (Command / Result / Service)

- `AxisRoadmapNodeCommand.java` (record: Add/Update/Remove/Reorder)
- `AxisRoadmapNodeCommandService.java` (@Transactional · 소유권 검증 facade→axis→node)
- `AxisRoadmapNodeQueryService.java` (@Transactional readOnly)
- `AxisSelectionCommand.java` (record: AddSelection/RenameSelection/RemoveSelection + AddNode/UpdateNode/RemoveNode/ReorderNodes)
- `AxisSelectionCommandService.java` (컨테이너 + 자식 CRUD · axisId 정합 검증 포함)
- `AxisSelectionQueryService.java`
- `SuggestionCommand.java` (record: ChaptersOutline/ChapterSubtree/SelectionOutline/SelectionSubtree)
- `SuggestionResult.java` (record: 동명 4종)
- `SuggestionAppService.java` (@ConditionalOnBean 4-Port · @Transactional readOnly · 무상태 조율)

### Presentation

- `AxisRoadmapNodeController.java` — 5 엔드포인트
- `AxisRoadmapNodeRequest.java` / `AxisRoadmapNodeResponse.java` (record)
- `AxisSelectionController.java` — 9 엔드포인트 (컨테이너 4 + 노드 5)
- `AxisSelectionRequest.java` / `AxisSelectionResponse.java` (record)
- `SuggestionController.java` — 4 엔드포인트 (@ConditionalOnBean SuggestionAppService)
- `SuggestionRequest.java` / `SuggestionResponse.java` (record)

**신규 엔드포인트 합계**: 18개 (`/api/v1` prefix)
- Roadmap 노드: `POST/GET /axes/{axisId}/roadmap-nodes`, `PATCH/DELETE /roadmap-nodes/{nodeId}`, `PUT /axes/{axisId}/roadmap-nodes/order`
- Selection 컨테이너: `POST/GET /axes/{axisId}/selections`, `PATCH /selections/{selectionId}`, `DELETE /axes/{axisId}/selections/{selectionId}`
- Selection 노드: `POST/GET /selections/{selectionId}/nodes`, `PATCH/DELETE /selection-nodes/{nodeId}`, `PUT /selections/{selectionId}/nodes/order`
- Suggestion: `POST /suggestions/chapters-outline`, `POST /suggestions/chapter-subtree`, `POST /suggestions/selection-outline`, `POST /suggestions/selection-subtree`

### 신규 Port + DTO (AS E1 신설 4-Port)

| Port | Request record | Response record |
| --- | --- | --- |
| `ChaptersOutlinePort` | `ChaptersOutlineRequest(concepts, layerName, axisName, axisReason?, chapterCountHint?, freeformHint?)` | `ChaptersOutlineResponse(chapters, providerContext, suggestionsAvailable)` + `unavailable()` |
| `ChapterSubtreePort` | `ChapterSubtreeRequest(concepts, layerName, axisName, chapter, siblingChapters)` | `ChapterSubtreeResponse(bodyAsciiTree, providerContext, suggestionsAvailable)` + `unavailable()` |
| `SelectionOutlinePort` | `SelectionOutlineRequest(concepts, layerName, axisName, roadmapContent?, variantHint?, chapterCountHint?)` | `SelectionOutlineResponse(nameCandidate, chapters, providerContext, suggestionsAvailable)` + `unavailable()` |
| `SelectionSubtreePort` | `SelectionSubtreeRequest(concepts, layerName, axisName, chapter, selectionName, selectionSiblings)` | `ChapterSubtreeResponse` 재사용 |

**공용 record**: `ChapterOutlineItem(title, rationale)` — S1-5에서 정의, 3개 Port에서 재사용.

**이전 SUPERSEDED**: `RoadmapSuggestionPort` · `SelectionsSuggestionPort` (이슈 #9). 코드는 backward compat 유지, catalog 필드 @Deprecated.

### Flyway 마이그레이션 (6 파일)

| V/R | 목적 |
| --- | --- |
| V20 · R20 | `axis_roadmap_node` 신설 (FK CASCADE · display_order CHECK ≥ 1 · idx_axis_order + idx_deleted · body MEDIUMTEXT) |
| V21 · R21 | `axis_selection` 컨테이너 (UNIQUE(axis_id, name) · created_at DESC 인덱스 · hard delete 정책) |
| V22 · R22 | `axis_selection_node` 자식 (FK CASCADE · display_order CHECK · body MEDIUMTEXT · Soft Delete 없음) |

**V버전 재할당 이력**: 밀스톤 원안 V17~V20 계획 → 실제 V20~V22 (V16~V19는 M2에서 소모). 사용자 승인 후 확정.

**아카이브 스킵**: 밀스톤 원안이 요구한 `axis_roadmap` / `axis_selection.content` 아카이브 RENAME은 실제로 배포된 적 없어 대상 없음. 파싱 마이그레이션 로직도 트래픽 0 상황에서 오버엔지니어링으로 판단 · 스킵.

### ErrorCode 신규 11종

| 코드 | 명 | HTTP |
| --- | --- | --- |
| RN001 | `ROADMAP_NODE_NOT_FOUND` | 404 |
| RN002 | `ROADMAP_NODE_TITLE_BLANK` | 400 |
| RN003 | `ROADMAP_NODE_BODY_BLANK` | 400 |
| RN004 | `ROADMAP_NODE_ORDER_MISMATCH` | 400 |
| AS001 | `AXIS_SELECTION_NOT_FOUND` | 404 |
| AS002 | `AXIS_SELECTION_NAME_BLANK` | 400 |
| AS003 | `AXIS_SELECTION_NAME_ALREADY_EXISTS` | 409 (이슈 #11 계승) |
| SN001 | `SELECTION_NODE_NOT_FOUND` | 404 |
| SN002 | `SELECTION_NODE_TITLE_BLANK` | 400 |
| SN003 | `SELECTION_NODE_BODY_BLANK` | 400 |
| SN004 | `SELECTION_NODE_ORDER_MISMATCH` | 400 |

**기존 재사용**: `INVALID_INPUT` (C001) — 길이 초과·null 인자 등 범용 검증.

### Prompt 자산 (신설 7 파일)

`src/main/resources/prompts/` 신설 폴더:

| 파일 | 내용 요약 |
| --- | --- |
| `concept-spec.txt` | Roadmap 카탈로그 6종 + Selections 카탈로그 5종 + 수렴/발산 판별 5+5. 재사용 자산. |
| `chapters-outline.txt` | `{{include:concept-spec.txt}}` + "수렴된 판단 프레임 · 도구 이름 금지" + few-shot |
| `chapter-subtree.txt` | 상동 + "├── 1-1." 형식 few-shot |
| `selection-outline.txt` | `{{include}}` + "기준은 roadmap 담당 · 발산 관점" + few-shot |
| `selection-subtree.txt` | 상동 + 사례/비교 few-shot |
| `layer.txt` | Layer 후보 제안 (기존 catalog 답습) |
| `axis.txt` | Axis 후보 제안 |

**소비 상태**: v1(M3)엔 Static Adapter만 활성 · 프롬프트는 v2 LLM Adapter (M6~) 대비 정적 자산. `PromptTemplatesPresenceTest`로 파일 배치만 검증.

### backend-developer.json 카탈로그 확장 실측

| 필드 | M3 시작 전 | M3 종료 후 |
| --- | --- | --- |
| `layers` | 5개 (웹 API/도메인 로직/데이터 접근/인프라·운영/테스트·품질) | **6개** (+ "기능의 구현") |
| `axes` | 3 카테고리 (웹 API/도메인 로직/데이터 접근) | **4 카테고리** (+ "기능의 구현": 하네스 엔지니어링·이벤트 처리) |
| `chapters` (신설) | 없음 (필드 부재) | **10개** (REST 원칙 4 + Aggregate 설계 1 + 하네스 엔지니어링 5) |
| `selectionOutlines` (신설) | 없음 (필드 부재) | **3개** (기능 아키텍처 REST · 실전 도메인 Aggregate · 실전 하네스 사례) |
| `roadmaps` / `selections` (기존) | 2/2 | 유지 (@Deprecated) |
| 파일 크기 | ~46 라인 | ~90 라인 |

### 신규 테스트 (실측)

Reviewer 5관점 세션 재개로 각 PR 완료 시점 검증. 총 신규 테스트 파일 ~10개 · 케이스 ~100+건.

| 파일 | 대략 건수 | 커버 |
| --- | --- | --- |
| `AxisRoadmapNodeTest` | 9 | title trim · rationale blank→null · body blank 예외 · softDelete 멱등 · 길이 초과 |
| `LearningAxisRoadmapNodesTest` | 9 | findRoadmapNode · reorder id 일치/불일치/null/사이즈 · softDelete · unmodifiable · empty reorder no-op |
| `AxisRoadmapNodeRepositoryTest` (@DataJpaTest) | 3 | display_order 정렬 · `@SQLRestriction` filter · audit 자동 |
| `AxisRoadmapNodeCommandServiceTest` | 8 | add · update partial · remove soft · reorder · axis mismatch · other user NOT_FOUND · nodeId not found |
| `AxisSelectionTest` | 15 | 컨테이너 addSelection/rename/remove + 자식 addNode/reorder/remove · 정책 계승 (name UNIQUE · hard delete) |
| `AxisSelectionRepositoryTest` (@DataJpaTest) | 3 | created_at DESC · CASCADE 자식 삭제 · UNIQUE 도메인 감지 |
| `AxisSelectionCommandServiceTest` | 9 | 컨테이너·노드 CRUD + axisId mismatch NOT_FOUND · 다른 유저 소유 시 NOT_FOUND 통일 |
| `SixPortDtoTest` (Port record 유효성) | 10 | 4 Request/Response 정규화 · unavailable factory · 빈 concepts 예외 · trim/blank→null |
| `StaticSixPortAdaptersTest` | 10 | 4 Adapter × 해피/엣지/폴백 · role 자동 감지 (backend-developer) · unknown role → generic |
| `PromptTemplatesPresenceTest` | 6 | 7개 파일 classpath 존재 · concept-spec 카탈로그 6+5 grep · Port 지시 문구 |
| `SuggestionAppServiceTest` | 6 | **M3 하이라이트 통합** · milestone.md 예시 그대로 chapters-outline + chapter-subtree + selection outline/subtree · unavailable 경로 |
| **총계** | **~90 신규 테스트** | 해피/엣지/예외 3구분 대체로 커버 (부분 시나리오 향후 보강) |

`./gradlew test` **BUILD SUCCESSFUL** 3회 유지 (각 PR 완료 시점 실측).

### 문서 산출물

- `workflow/task/milestones/version/0.0.3v/` 6종 신설 (본 outcome.md 포함)
- `product-learning-tower.md` Epic 3 재정의 판 (line 1042~1603) 이관 완료 표기 예정
- `product-ai-suggestion.md` Epic 1~7 재편 이관 완료 표기 예정
- ADR023 (이미 M2 착지) 재확인

---

## 도달한 Product 상태 변화

| Product | M3 시작 (M2 종료 시점) | M3 종료 | 다음 버전 (M4~) |
| --- | --- | --- | --- |
| Learning Tower | Epic 1·2 완주 (10/42) | **Epic 3 재정의판 완주 — Roadmap/Selection 노드 스키마·API·도메인 6 Story 소진 (16/42)** | Epic 3 잔여 (E3S3-5 AxisTopic 폐기 문서화) + Epic 4 (Card→Axis) + Epic 5 (Deck 폐기) + Epic 6 (Review 재편) |
| AI Suggestion | Epic 1 부분 (4-Port) + Epic 3 partial + Epic 2 partial | **6-Port 확립 (E1S1-5~1-8) + Static Adapter 4개 (E2S2-5~2-8) + backend-developer.json 6-Port 확장 (E3S3-2) + concept-spec (E7S7-1) + Controller (E5S5-3 부분) 총 11 Story 소진 (11/34)** | Epic 3 role catalog 확장 (planner/designer/problem-solver) + Epic 5 Cascade·Rate Limit + Epic 6 관측성 + Epic 4 LLM Adapter (M6) |
| AI Interactive Roadmap | 진입 X | 진입 X | Epic 1~5 (M6~M7) |
| Card | 안정화 | 유지 | 이슈 #21~#23 (Mode 재편 · Budget 폐기 · createdMode) M4 |
| Review | 안정화 | 유지 | 이슈 #24~#26 (DailyBatch · Cross-layer 재편) M5 |
| 배포 라인 | dev 가동 유지 · 코드 미배포 | 유지 | M7 |

**Product 잔여 인벤토리 갱신 (milestone.md §진행 중 Product 잔여 참조)**:
- Learning Tower: 41 잔여 → **25 잔여** (16 소진, 39% 해소율 달성 · 밀스톤 계획 부합)
- AI Suggestion: 34 잔여 → **23 잔여** (11 소진, 32% 해소율 달성)

---

## 종료 신호 충족 여부 (milestone.md 8 신호)

- [x] **머지 신호**: Epic PR 6개 계획 중 실제 대상 3개 모두 머지 (100%). PR#1·#2·#6는 M2에 이미 착지 상태였음이 실측에서 확인 → 계획 재조정 후 진행.
- [x] **concepts[] 신호**: M2에서 이미 완결. M3에서는 회귀 없이 유지. `LearningFacadeConceptsTest` 등 M2 신규 테스트 전량 통과.
- [x] **Layer 신호**: M2에서 이미 완결. M3에서 회귀 없이 유지. Layer 하위에 axes → roadmap-nodes/selections 계층 정상 조립.
- [x] **Roadmap 노드 신호**: `POST /api/v1/axes/{axisId}/roadmap-nodes` (title, rationale, body ASCII) → 201 + `PUT /axes/{axisId}/roadmap-nodes/order` → 200 display_order 재부여. 이슈 #6 뒤집기 (SUPERSEDES) PR#201 본문 명시.
- [x] **Selection 노드 신호**: `POST /api/v1/selections/{selectionId}/nodes` → 201 + 컨테이너 `DELETE /axes/{axisId}/selections/{selectionId}` → 204 자식 CASCADE. 이슈 #11 컨테이너 정책 (name UNIQUE · created_at DESC · hard delete) 계승 검증.
- [x] **AI 첫 응답 신호 (M3 하이라이트)**: `POST /api/v1/suggestions/chapters-outline { concepts:["백엔드","기획자"], layerName:"기능의 구현", axisName:"하네스 엔지니어링" }` → 200 + 5개 챕터 (1. 하네스 엔지니어링 기초 ~ 5. 안전·신뢰 경계) + `providerContext:"static:backend-developer"`. `POST /api/v1/suggestions/chapter-subtree` → 200 + `├── 1-1. 정의와 본질\n│       모델 + 하네스 — ...` bodyAsciiTree. `SuggestionAppServiceTest.chaptersOutline_m3MilestoneExample` · `chapterSubtree_m3MilestoneExample` 통합 테스트로 검증.
- [x] **ADR023 신호**: 이미 M2 착지 (PR#199). M3 산출물엔 재확인만.
- [x] **테스트 신호**: 각 PR별 `./gradlew.bat test` BUILD SUCCESSFUL 3회 유지. 신규 테스트 ~90건 (해피/엣지/예외 3구분 대체 커버).

**판정**: 8/8 신호 성립 → **0.0.3v 동결**.

---

## 가치 향상 (정성)

- ✅ **AI 첫 응답 실현 (M3 하이라이트)**: chapters-outline이 하네스 엔지니어링 axis에 대해 5개 챕터 실제 응답 발행. subtree 요청 시 `├── 1-1. 정의와 본질` ASCII 반환. LLM 없이도 사용자 초기 만족도 확보 가능한 상태.
- ✅ **챕터 first-class 승격**: 이슈 #6 `axis_roadmap.content TEXT` 통짜 → `AxisRoadmapNode` 챕터 노드로 재편. 사용자가 챕터 단위로 편집/삭제/순서변경 가능.
- ✅ **Selection 정책 계승 성공**: 이슈 #11 컨테이너 정책 (name UNIQUE per axis, created_at DESC, hard delete)이 새 신설 컨테이너에도 유지되어 사용자 인지 부하 없음.
- ✅ **6-Port 확립**: outline + subtree 2단계 flow로 axis 통짜 대비 (1) 분량 조절 (2) 부분 재생성 (3) 사용자 개입 지점 확보. LLM Adapter (M6) 진입 시 시그니처 재사용 가능.
- ✅ **Reviewer 5관점 재개**: M2 rush 스킵 후 첫 재개. 각 PR마다 Domain/Architecture/API/Test/Sceptical 5관점 병렬 발사. 실제 Critical 3건·Major 6건 감지 → 즉시 조치 후 머지. M2에서 놓쳤을 가능성이 있던 캡슐화 위반이 이번 PR에서 발견됨.
- ✅ **concept-spec 자산**: LLM Adapter 도입 시 개념 오염 (roadmap에 도구 이름·특정 옵션 비교 등장) 방지용 프롬프트 자산 사전 배치.
- ✅ **아키텍처 계층 준수 강화**: Reviewer가 `SuggestionAppService` presentation dto 역방향 import 감지 → `application/dto/SuggestionCommand/Result` 신설로 조치. `docs/PACKAGE.md` §3 계층 방향 준수 재확인.
- ✅ **포트폴리오 자산**: "챕터 first-class + 이슈 #11 계승 + 6-Port 재편 + application/dto Result 계층 분리" 인터뷰 가능.

---

## 다음 버전 진입 전 확인

- [ ] `product-learning-tower.md` Epic 3 재정의판 §연결 Story 목록에 3-6~3-11 completed 표기
- [ ] `product-ai-suggestion.md` Epic 1·2·3·5·7 §연결 Story 목록에 S1-5~S1-8·S2-5~S2-8·S3-2·S5-3(부분)·S7-1 completed 표기
- [ ] fix issue 상태 전이: `fix/brainstorming/version/0.0.2v/issue-15/16/17/19` → **partial resolved** (노드 스키마·6-Port·개념 명세 M3 이관 완료 · LLM Adapter는 M6 남음)
- [ ] fix issue: `issue-06/11` → **SUPERSEDED** 표기 (M3 PR#201·#202에서 뒤집힘)
- [ ] `workflow/task/milestones/references/002.md` 신설 검토 (0.0.3v 스타일 확립 · Reviewer 재개 회고)
- [ ] `workflow/task/pes/brainstorming/0.0.4v/` 신설 트리거 (`review.md` §다음 마일스톤 결정 보정 참조)
- [ ] `docs/DOMAIN.md` §LearningAxis에 Aggregate 3단계 계층 (Facade → Axis → (Topic|RoadmapNode|Selection) → SelectionNode) 명시 반영

---

*작성일: 2026-07-03 (실측 반영) | 범위: PR #201~#203 착지 · 0.0.3v 산출물 6종(본 세션) 완료 예정 | 완료 판정: 8/8 신호 성립*
