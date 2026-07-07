# [Product] AI Interactive Roadmap — 세션 상태기계 + Selection 정책

## Product Vision

> **`RoadmapInteractionSession` 도메인이 `axisDraft → chaptersDraft → subtreesDraft → axisConsensus` 흐름을 상태기계로 관리하며, `product-ai-suggestion` 의 6-Port 를 호출해 초안을 얻고 사용자 결정으로 확정한다.**
> `AxisSelection` 컨테이너는 name 유니크·created_at DESC·in-place update·hard delete 정책을 따르며, 세션은 최종 저장 시 `LearningFacade` Aggregate 행위를 통해 챕터 노드로 저장한다. Roadmap/Selection 챕터 노드는 챕터 단위 재생성 API(hint 포함)로 사용자 의견을 반영한다.

---

## 🔄 Fix 개정 (2026-07-02) — 이슈 #17/#18 반영

**개정 사유**: 이슈 #9의 세션 흐름(4-Port 통짜 호출)이 AI 품질 3가지 부족(분량·부분수정·의견반영)을 낳음. 세션을 **outline + subtree 2단계**로 확장하고 **챕터 노드 단위 재생성 API**를 세션 상태 안에 통합.

### 개정 범위 요약

| 영역 | 이전 (#9) | 개정 후 (#17/#18) |
|---|---|---|
| 세션 상태 머신 | `INIT → DRAFTED → REVIEWING → COMMITTED / ABANDONED` | `INIT → LAYERS_DRAFTED → AXES_DRAFTED → CHAPTERS_DRAFTED → SUBTREES_DRAFTED → REVIEWING → COMMITTED / ABANDONED` |
| AxisDraftItem | `AxisDraftItem(name, rationale, roadmapDraft)` — roadmap이 통짜 문자열 | `AxisDraftItem(name, rationale, chapters: List<ChapterDraft>)` — chapters는 outline 단계 title·rationale + subtree 단계에서 bodyAsciiTree 채워짐 |
| 세션 → 저장 매핑 | `axis.setRoadmap(content)` — axis당 1개 통짜 | `axis.addRoadmapNode(title, rationale, body)` — 각 챕터마다 노드 row 생성 (이슈 #15) |
| 노드 재생성 | 없음 | `POST /roadmap-nodes/{nodeId}/regenerate { hint? }` — 세션 밖에서도 유효한 챕터 단위 재생성 (이슈 #18) |
| Static fallback | 4-Port 각각 | 6-Port 각각 outline/subtree 단계 모두 지원 |

### 새 세션 상태 머신

```
INIT
  → LAYERS_DRAFTED       (LayerSuggestionPort)
  → AXES_DRAFTED         (AxisSuggestionPort, layer별)
  → CHAPTERS_DRAFTED     (ChaptersOutlinePort, axis별) ★ 신규 단계
  → SUBTREES_DRAFTED     (ChapterSubtreePort 병렬, axis별) ★ 신규 단계
  → REVIEWING            (사용자 액션 keep/remove/rename/add/reorder + 챕터별 재생성)
  → COMMITTED            (LearningFacade에 챕터 노드 row로 저장)
  → ABANDONED            (사용자 이탈 · TTL)
```

Selection 세션도 유사:
```
INIT
  → SELECTION_OUTLINE_DRAFTED   (SelectionOutlinePort)
  → SELECTION_SUBTREES_DRAFTED  (SelectionSubtreePort 병렬)
  → REVIEWING
  → COMMITTED (axis_selection 컨테이너 + axis_selection_node row 생성)
```

### 세션 API 확장 (요약)

| 메서드 | 경로 | 목적 |
|---|---|---|
| POST | `/roadmap-sessions/{id}/chapters-outline/draft` | 각 axis의 챕터 outline 요청 (`CHAPTERS_DRAFTED` 진입) |
| POST | `/roadmap-sessions/{id}/chapters-outline/refresh` | outline 재요청 (freeformHint 포함 가능) |
| POST | `/roadmap-sessions/{id}/chapters-subtrees/draft` | outline 승인된 챕터들에 대해 subtree 병렬 요청 (`SUBTREES_DRAFTED` 진입) |
| POST | `/roadmap-sessions/{id}/chapters/{chapterIndex}/regenerate` | 세션 내 챕터 노드 통짜 재생성 (hint 포함) |
| PATCH | `/roadmap-sessions/{id}/chapters/{chapterIndex}` | 세션 내 챕터 노드 부분 편집 (title/rationale/body) |

### 저장 매핑

`COMMITTED` 진입 시:
- `LearningFacade.addLayer(layerName)` → `Layer.addAxis(axisName, axisReason)`
- 각 챕터에 대해 `Axis.addRoadmapNode(title, rationale, body)` 호출 → `axis_roadmap_node` row 생성
- Selection도 동일 패턴 (`Axis.addSelection(name)` → `Selection.addNode(title, rationale, body)`)

### Story 상태

Epic 1(Session Aggregate + 상태기계) — 상태 머신 확장 반영 필요. Story 1-2(`SessionState` enum)는 새 상태 값(`LAYERS_DRAFTED`, `AXES_DRAFTED`, `CHAPTERS_DRAFTED`, `SUBTREES_DRAFTED`) 추가.
Epic 2(axisDraft 흐름) — 4-Port → 6-Port 로 확장, `AxisDraftItem` 구조 변경, chapters outline + subtree 병렬 로직 신설.
Epic 3+(사용자 액션 · 저장 · Selection) — 저장 매핑을 챕터 노드 row 생성으로 재편.
챕터 노드 재생성 API(이슈 #18)는 세션 밖에서도 별도 엔드포인트로 유효.

---

## 배경 및 문제

- 현재 상황 (As-Is)
  - 기존 `product-ai-interactive-roadmap.md` 는 5 Epic 25+ Story 로 설계되어 있으나 `AxisTopic` · `TopicRefining` · `SAVED` 등 폐기된 용어를 광범위하게 참조.
  - 신설 `product-ai-suggestion.md` 는 4-Port(Layer/Axis/Roadmap/Selections) 로 재편됐고, 신설 `product-learning-tower.md` 는 `AxisTopic` 을 폐기하고 `AxisRoadmap` + `AxisSelection[]` 이중 축을 도입.
  - 세션 상태(`STARTED / AXIS_DRAFTED / TOPIC_REFINING / READY_TO_SAVE / SAVED / ABANDONED`) 은 ADR022 에서 폐기되고 `INIT / DRAFTED / REVIEWING / COMMITTED / ABANDONED` 로 재정의.
  - `AxisSelection` 정책(#11) 도 신규 도입 사항.
- 발생하는 문제
  - 기존 SDD 로는 신규 도메인 재편(Layer/Roadmap/Selection)과 정합이 안 맞음.
  - AI 초안이 그대로 저장 경로로 흘러가면 사용자 소유감·설명 가능성 부족. 반드시 사용자 명시 액션(keep/remove/rename/add/reorder)을 거쳐야 함.
  - 세션 도메인이 4-Port 를 직접 소유하면 관심사 분리 실패. `product-ai-suggestion` 을 무상태 인프라로, 본 Product 는 세션 상태기계로 명확히 분리 필요.
- 왜 지금 해결해야 하는가
  - `product-learning-tower.md` Epic 1~4 완료 시 axisDraft 저장 대상(`LearningFacade.concepts[]`, `Layer`, `Axis`, `AxisRoadmap`, `AxisSelection`) 이 존재하는 상태.
  - `product-ai-suggestion.md` 의 4-Port 완료 시 초안 생성 인프라 준비 완료.
  - 두 선행 Product 가 준비되면 본 Product 가 세션 흐름을 완성해 사용자 UX 를 열 수 있음.

## 목표 (To-Be)

- **`RoadmapInteractionSession` Aggregate 신설** — 상태기계 `INIT → LAYERS_DRAFTED → AXES_DRAFTED → CHAPTERS_DRAFTED → SUBTREES_DRAFTED → REVIEWING → COMMITTED / ABANDONED` (2026-07-02 개정, 이슈 #17).
- **`axisDraft` / `chaptersDraft` / `subtreesDraft` VO 도입** — 단계별 초안 스냅샷과 사용자 확정본을 도메인 값으로 관리. `AxisDraftItem(name, rationale, chapters: List<ChapterDraft>)` 구조 (roadmapDraft 통짜 필드 폐기, 챕터별 outline·body로 분리).
- **`AxisSelection` 컨테이너 정책 강제 (#11)** — `(axis_id, name) UNIQUE`, `created_at DESC` 정렬, in-place update 허용, hard delete. 컨테이너 하위 노드는 이슈 #16의 CRUD 정책.
- **`product-ai-suggestion` 6-Port 재사용** — Port 를 세션 서비스에서 호출. 자체 LLM Client · Prompt · Fallback 는 소유하지 않음. 챕터 노드 재생성(#18)도 세션 서비스에서 subtree Port 호출.
- **결정형 검증 vs 확률형 추천 경계 명시** — 저장 판정은 결정형(blank/duplicate/order/consensus-ready)만. AI 응답도 이 검증을 통과해야 저장 진입.
- **저장은 항상 사용자 합의 후만** — AI 결과 그대로 저장 경로 폐쇄. `LearningFacade.addAxis()` / `Layer.addAxis()` / `Axis.setRoadmap()` / `Axis.addSelection()` Aggregate 행위 호출.
- **`DraftChangeLog` 로 diff 저장** — 초안 대비 최종안 구조 로그 보존. 자유 텍스트 원문 저장 안 함.
- **동기 응답 + Static Fallback** — 스트리밍 미도입. `product-ai-suggestion` Cascade 재사용.
- **사이드이펙트 명시** — `LearningFacade.addAxis()` 호출 시 `LearningAxisCreatedEvent` 발행 → `product-learning-tower.md` 에서 Deck 는 이미 폐기(Epic 5), Layer 자동 생성 없음(사용자 명시 Layer 선택 필수).

## 설계 결정 (Design Decisions)

- **상호작용 단위 = `RoadmapInteractionSession` (Stateful)** — 2026-07-02 개정
  - 매번 stateless LLM 호출 대안은 사용자 수정 이력 자산화 불가.
  - 세션 상태: `INIT`(생성됨) → `LAYERS_DRAFTED`(LayerSuggestionPort 결과) → `AXES_DRAFTED`(AxisSuggestionPort 결과) → `CHAPTERS_DRAFTED`(ChaptersOutlinePort 결과, 챕터 title·rationale만) → `SUBTREES_DRAFTED`(ChapterSubtreePort 병렬 결과, 챕터별 bodyAsciiTree) → `REVIEWING`(사용자 액션 keep/remove/rename/add/reorder + 챕터 재생성 반영 중) → `COMMITTED`(axisConsensus 확정, LearningFacade에 챕터 노드 row 생성) → `ABANDONED`(사용자 이탈 · TTL 만료).
  - `COMMITTED` 이후 draft/refresh 재요청 불가(`SESSION_ALREADY_COMMITTED` 409).
  - **outline/subtree 2단계 분리 근거**: one-shot 통짜 생성이 낳는 3부족(분량·부분수정·의견반영) 해결. outline 단계에서 사용자가 챕터 리스트를 미리 편집/거절 가능 → 분량 제어. 챕터 노드가 first-class로 승격됐으니 재생성·편집 API가 세션 안팎에서 유효.
- **`axisDraft` refresh 는 명시적 사용자 액션에만 허용**
  - 자동 무한 재호출 금지. 사용자가 "다시 초안 받기" 액션 시에만 4-Port 재호출.
  - refresh context (예: "운영 축 빼고", "입문 범위") 를 사용자가 명시.
- **AI 추천은 "저장" 이 아니라 "초안 + 사용자 합의 후 저장"**
  - AI 결과를 그대로 LearningFacade 에 쓰는 경로 폐쇄.
  - DoD 에 "AI 가 생성한 초안은 언제나 사용자 수정/합의 단계를 거친 뒤에만 저장 가능" 명문화.
- **`SuggestionPort` / `ChatClient` / `BeanOutputConverter` 는 도메인에 들이지 않음**
  - Interaction 도메인은 LLM 의존을 모름. AI 호출은 Application Service 가 `product-ai-suggestion` 의 4-Port 를 통해 수행.
- **결정형 vs 확률형 경계 명시**
  - 저장 판정 = 결정형만 (blank/duplicate/order/consensus-ready 검증).
  - 확률형(4-Port 응답) 은 항상 "제안", 자동 적용 없음.
- **`DraftChangeLog` 로 diff 보존 (전체 transcript X)**
  - 채택/거부/추가/리네임/순서 변경의 구조 로그만 보존.
  - 자유 텍스트 원문 저장 안 함(개인정보·저장소 폭발 방지).
- **`AxisSelection` 정책 (#11)**
  - `(axis_id, name) UNIQUE` — 같은 이름 중복 방지.
  - 정렬: `ORDER BY created_at DESC` (신규가 위).
  - Update: in-place 허용 (같은 name 유지). 새 name 은 새 row.
  - Delete: hard delete (softDelete 없음).
- **AI 실패 ≠ 기능 실패** — `product-ai-suggestion` Cascade 로 Static 폴백 자동. Static 도 실패 시 세션은 사용자 수동 입력만으로 진행 가능.
- **LearningFacade 결합 — Aggregate 행위 메서드 호출**
  - `LearningFacade.addAxis(...)` / `Layer.addAxis(...)` / `Axis.setRoadmap(...)` / `Axis.addSelection(...)` 호출.
  - JPA 우회 저장 금지. `displayOrder`·`coverageStatus`·`revisionCount` 등 불변식 상속.
  - **사이드이펙트**: axis 추가 시 `LearningAxisCreatedEvent` 발행. Deck 폐기(learning-tower Epic 5) 이후 Deck 자동 생성 없음. 관측은 `product-log.md`.
- **동기 응답 + 동기 LLM 호출 (스트리밍 미도입)**
  - v1 응답 P95 임계치(product-ai-suggestion 정책 준수).
- **ADR022 용어 준수** — `axisDraft`, `axisConsensus`, `AxisRoadmap`, `AxisSelection` 표기.

## 대안 검토 (Alternatives Considered)

### 갈림길 A. 상호작용 형태

**Option A — 추천 리스트형 (N개 던지고 사용자가 체크)**
- 거부 이유: 사용자 참여가 체크/언체크로 축소, 소유감 약함, "제거·범위 축소" 표현 불가.

**Option B — 인터뷰형 (LLM 질문·사용자 답변)**
- 거부 이유: 첫 진입 사용자에게 대화 부담, 회차당 LLM 호출 폭증.

**Option C (선택) — 초안 + 비판형 (guided interaction)**
- 비용: 초안 품질에 사용자 만족도 의존.
- 보상: 막막함 즉시 해소, 사용자 수정이 주 행위, 소유감·합의성 자연 형성.

### 갈림길 B. 초안 저장 정책

**Option A — 매 요청마다 LLM 재호출, 초안 미저장**
- 거부 이유: 세션 재개 불가, diff 기준 소실.

**Option B (선택) — `RoadmapDraft` 를 도메인 엔티티로 영속화**
- 비용: 스키마·정리 정책 필요.
- 보상: 재개 가능, diff 추적, 실험 자료.

### 갈림길 C. 세션 상태 표현

**Option A — 도메인 서비스 stateless 다형 흐름**
- 거부 이유: 세션 라이프사이클 · TTL · 재개 · diff 관리 모두 도메인 필요.

**Option B (선택) — `RoadmapInteractionSession` Aggregate + 상태 enum**
- 비용: Aggregate 트랜잭션 · Repository 필요.
- 보상: 상태 전이 명시적, 검증 규칙 응집, diff 추적 자연스러움.

### 갈림길 D. LearningFacade 통합 방식

**Option A — LearningFacade BC 안에 세션 도메인 신설**
- 거부 이유: LearningFacade 는 확정 구조 담당인데 임시 초안 상태가 섞이면 책임 흐려짐. LLM 의존 인지.

**Option B (선택) — 별도 인터랙션 계층 + LearningFacade Aggregate 행위 호출**
- 비용: 새 계층 도입.
- 보상: LearningFacade 는 확정 구조만 유지, 세션은 자유롭게 모델링, 저장은 Aggregate 행위 규약.

### 갈림길 E. Selection 정책 (Update / Delete)

**Option A — softDelete + revision 체인**
- 장점: 이력 완전 보존.
- 거부 이유: Selection 은 사용자 사례 관리, 재작성 권장 UX. softDelete 유지 부담이 가치 초과.

**Option B (선택) — in-place update + hard delete (#11)**
- 비용: 이전 값 보존 없음.
- 보상: 단순, 사용자 재작성 UX 자연.

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 배치

```
[React SPA]
    │
    │  POST /api/v1/roadmap-sessions
    │  POST /api/v1/roadmap-sessions/{id}/axis-drafts
    │  POST /api/v1/roadmap-sessions/{id}/axis-drafts/refresh
    │  PATCH /api/v1/roadmap-sessions/{id}/axes  (keep/remove/rename/add/reorder)
    │  POST /api/v1/roadmap-sessions/{id}/roadmap-drafts
    │  POST /api/v1/roadmap-sessions/{id}/selection-drafts
    │  POST /api/v1/roadmap-sessions/{id}/commit
    ▼
┌─ Presentation ────────────────────────────────────────────────┐
│  RoadmapInteractionController                                  │
└────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─ Application / Interaction ──────────────────────────────────┐
│  RoadmapInteractionCommandService                             │
│   ├ start / resume / commit / abandon                        │
│   ├ createAxisDraft   → LayerSuggestionPort · AxisSuggestion │
│   ├ refreshAxisDraft  → 4-Port 재호출                        │
│   ├ applyUserAction   (keep/remove/rename/add/reorder)       │
│   ├ 결정형 검증 (blank/duplicate/order/consensus-ready)      │
│   ├ createRoadmapDraft → RoadmapSuggestionPort               │
│   ├ createSelectionsDraft → SelectionsSuggestionPort         │
│   └ commit → LearningFacade Aggregate 행위 호출              │
│                                                              │
│  RoadmapInteractionQueryService                              │
│   └ 세션 조회 · diff 조회                                    │
└────────────────────────────────────────────────────────────────┘
    │                                    │
    │                                    │
    ▼                                    ▼
┌─ Domain — Interaction ─────────┐   ┌─ product-ai-suggestion (재사용) ─┐
│  RoadmapInteractionSession (AR)│   │  LayerSuggestionPort              │
│   ├ scope: sessionScope         │   │  AxisSuggestionPort               │
│   ├ state: SessionState         │   │  RoadmapSuggestionPort            │
│   ├ axisDraft: AxisDraftSnapshot│   │  SelectionsSuggestionPort         │
│   ├ axisConsensus: AxisConsensus│   │  (Cascade LLM→Static)             │
│   └ changeLog: List<DraftLog>   │   └────────────────────────────────────┘
│  DraftChangeLog (Entity)        │
│  AxisDraftSnapshot (VO)         │
│  AxisConsensusSnapshot (VO)     │
└────────────────────────────────┘
    │
    ▼ commit 시점에만
┌─ product-learning-tower ─────────────────────────────────────┐
│  LearningFacade / Layer / LearningAxis / AxisRoadmap /       │
│  AxisSelection Aggregate 행위 호출                            │
└────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─ MySQL ───────────────────────────────────────────────────────┐
│  roadmap_interaction_session                                   │
│  axis_draft_snapshot / axis_consensus_snapshot                 │
│  draft_change_log                                              │
│  (기존) learning_facade / learning_layer / learning_axis /     │
│         axis_roadmap / axis_selection                          │
└────────────────────────────────────────────────────────────────┘
```

### 핵심 플로우

**1. 세션 시작 + 축 초안 생성**
```
Client ─POST /roadmap-sessions {facadeId, layerId}─▶ Controller
                                                      └ start()
                                                          └ Session.start() [INIT]
                                                          └ persist
Client ◀ 201 {sessionId, state:"INIT"} ─

Client ─POST /{id}/axis-drafts─▶ Controller
                                  └ Service.createAxisDraft()
                                      ├ AxisSuggestionPort.suggest(...) (via ai-suggestion Cascade)
                                      ├ Session.saveAxisDraft(snapshot) [state=DRAFTED]
                                      └ persist
Client ◀ 200 {axisDraft:{axes:[{name,rationale,roadmapDraft}]}, state:"DRAFTED"} ─
```

**2. 사용자 액션 적용**
```
Client ─PATCH /{id}/axes {action:"keep", axisName:"Java"}─▶ Controller
                                                              └ Service.applyUserAction()
                                                                  ├ Session.applyAction(action) [state=REVIEWING]
                                                                  ├ DraftChangeLog.append(action, timestamp)
                                                                  └ persist
Client ◀ 200 {consensusPreview:{axes:[...]}, changeLog:[...]} ─
```

**3. 최종 커밋**
```
Client ─POST /{id}/commit─▶ Controller
                            └ Service.commit()
                                ├ Session.validate() (결정형 검증)
                                ├ Session.buildConsensus() [axisConsensus 확정]
                                ├ LearningFacade.addLayer(...) or Layer.addAxis(...) 반복 호출
                                │  (Aggregate 행위, LearningAxisCreatedEvent 발행)
                                ├ Axis.setRoadmap(...) 반복
                                ├ Axis.addSelection(...) 반복
                                └ Session.commit() [state=COMMITTED]
Client ◀ 200 {committedAt, facadeId, addedAxisIds:[...], addedRoadmapCount, addedSelectionCount} ─
```

### Out-of-Process 의존

- **`product-ai-suggestion`** — 4-Port(무상태 인프라).
- **`product-learning-tower`** — LearningFacade / Layer / Axis / Roadmap / Selection Aggregate.
- **MySQL** — session · snapshot · changeLog 저장.
- **외부 시스템 없음** (LLM 은 product-ai-suggestion 이 소유).

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | ErrorCode | HTTP | 클라이언트 권장 동작 |
| --- | --- | --- | --- |
| 세션 TTL 만료 후 요청 | `SESSION_EXPIRED` | 410 | 새 세션 시작 안내 |
| 이미 COMMITTED 세션에 draft/action 요청 | `SESSION_ALREADY_COMMITTED` | 409 | 새 세션 시작 |
| 이미 ABANDONED 세션 재사용 | `SESSION_ABANDONED` | 410 | 새 세션 시작 |
| axisDraft 없는 상태에서 action 시도 | `SESSION_DRAFT_REQUIRED` | 400 | axisDraft 먼저 요청 |
| Roadmap 없는 axis 에 Selection draft 시도 | `AXIS_ROADMAP_REQUIRED` | 400 | Roadmap 먼저 확정 |
| 사용자 액션 결과 axis 0건 (consensus-ready 실패) | `AXIS_CONSENSUS_EMPTY` | 400 | 최소 1개 axis 유지 |
| commit 시 축 이름 중복 | `AXIS_CONSENSUS_NAME_DUPLICATE` | 400 | 이름 수정 유도 |
| 다른 유저의 session 접근 | `SESSION_FORBIDDEN` | 403 | 로그아웃/재로그인 |
| 4-Port 모두 실패 (Cascade suggestionsAvailable=false) | (없음) | 200 (본문 flag) | 사용자 수동 입력 유도, 세션 유지 |
| Layer 미선택 상태에서 축 저장 시도 | `SESSION_LAYER_REQUIRED` | 400 | Layer 선택 UI 유도 |

### 로깅 정책

- **항상 기록**:
  - 세션 시작·종료 (`session_id`, `user_id`, `state_from`, `state_to`, `reason`)
  - 사용자 액션 (`session_id`, `action_type`, `target`)
  - 4-Port 호출 (`session_id`, `port_type`, `provider`, `latency_ms`)
  - Commit 반영 결과 (`session_id`, `added_axis_count`, `added_roadmap_count`, `added_selection_count`)
- **debug**: axisDraft snapshot 요약 (dev only, 축 이름 · 개수 만)
- **절대 금지**: axisDraft 자유 텍스트 원문, 사용자 학습 내용 상세

### 관측 지표

- `thirdtool.roadmap_session.started_total` — Counter
- `thirdtool.roadmap_session.committed_total` — Counter
- `thirdtool.roadmap_session.abandoned_total{reason}` — Counter (`ttl_expired`, `user_abandon`, `error`)
- `thirdtool.roadmap_session.commit_axis_count` — Histogram (세션당 커밋된 axis 개수)
- `thirdtool.roadmap_session.action_count` — Histogram (세션당 사용자 액션 개수)

## 롤아웃 / 마이그레이션 (Rollout)

### 전제

- 선행 Product 완료 후 진입.
- 초기 배포는 `provider=static` (product-ai-suggestion 설정). LLM 활성화는 별도.
- 세션 TTL: 30분 (v1 기본).

### Product 의존성

- **선행**:
  - `product-learning-tower.md` Epic 1~4 (필수) — Layer/Axis/Roadmap/Selection 도메인.
  - `product-ai-suggestion.md` Epic 1~5 (필수) — 4-Port 구현.
- **후행**: 없음.

### Epic·Story 의존성 그래프

```
Epic 1 (Session Aggregate + 상태기계)
    │
    ▼
Epic 2 (axisDraft 흐름: 4-Port 호출 + refresh)
    │
    ▼
Epic 3 (Roadmap/Selection draft 흐름 · Selection 정책)
    │
    ▼
Epic 4 (사용자 액션 · diff · 결정형 검증)
    │
    ▼
Epic 5 (Commit · Aggregate 행위 호출 · 실패 응답)
```

### 환경별 설정 분기

| 항목 | dev | prod |
| --- | --- | --- |
| 세션 TTL | 30분 (변경 가능) | 30분 |
| Rate limit | 관대 | product-ai-suggestion 정책 사용 |
| DraftChangeLog 보존 | 무기한 | 90일 (배치 정리) |
| Static 카탈로그 provider | ai-suggestion 설정 상속 | 상속 |

## 성공 지표 (KPI)

| 지표 | 목표 값 | 측정 방법 |
| --- | --- | --- |
| 세션 시작 → COMMITTED 도달률 | ≥ 40% (v1 관측 후 목표 조정) | `thirdtool.roadmap_session.committed_total / started_total` |
| 세션 시작 → ABANDONED (TTL) 비율 | ≤ 30% | Product-op 지표 |
| Commit 시 결정형 검증 실패 비율 | ≤ 5% | 로그 카운트 |
| 세션당 사용자 액션 개수 P50 | ≥ 3 (사용자가 실질 참여했다는 신호) | 히스토그램 |
| 4-Port 호출 실패로 세션 진행 불가율 | ≤ 1% (Cascade 폴백 후에도) | 로그 |
| `AxisSelection` 정책 위반 (409) 발생률 | 안정 상태에서 ≤ 1% | 로그 |

## Scope

**In Scope**:
- `RoadmapInteractionSession` Aggregate + 상태기계 (`INIT/DRAFTED/REVIEWING/COMMITTED/ABANDONED`)
- `AxisDraftSnapshot` / `AxisConsensusSnapshot` VO
- `DraftChangeLog` Entity
- 4-Port(product-ai-suggestion) 호출 오케스트레이션
- 사용자 액션 API (keep/remove/rename/add/reorder)
- 결정형 검증 (blank/duplicate/order/consensus-ready)
- Commit → LearningFacade Aggregate 행위 호출
- `AxisSelection` 정책 (#11): UNIQUE, DESC 정렬, in-place update, hard delete — Epic 3 에서 세션 UI 관점으로 강제
- 세션 TTL · 재개 · 아카이브
- REST API 6~7 엔드포인트

**Out of Scope**:
- 무상태 4-Port 인프라 — `product-ai-suggestion` 소유
- Layer/Axis/Roadmap/Selection 도메인 자체 — `product-learning-tower` 소유
- 사용자 피드백 학습 루프 — Phase 2+
- Streaming/SSE 응답 — v1 동기 유지
- 세션 diff 기반 A/B 실험 프레임워크 — v2

## 대상 사용자

- **학습자 (신규)** — AI 초안을 비판·수정해 자신의 지도를 만든다. 소유감·설명 가능성 확보.
- **학습자 (기존 지도 확장)** — 기존 Layer 에 새 Axis 를 세션 흐름으로 추가.
- **운영자** — 세션 · commit · abandoned 지표로 UX 실효성 관측.

## 연결된 Epic 목록

- [ ] Epic 1: RoadmapInteractionSession Aggregate + 상태기계
- [ ] Epic 2: axisDraft 흐름 + refresh
- [ ] Epic 3: Roadmap/Selection draft 흐름 + Selection 정책 (#11)
- [ ] Epic 4: 사용자 액션 + DraftChangeLog + 결정형 검증
- [ ] Epic 5: Commit + LearningFacade Aggregate 행위 호출 + 관측

## 관련 문서

- **선행 fix 이슈**:
  - `workflow/task/fix/brainstorming/version/0.0.2v/issue-06-roadmap-selections-dualaxis.md` — Roadmap/Selection dual-axis (세션 파트)
  - `workflow/task/fix/brainstorming/version/0.0.2v/issue-09-ai-suggestion-3layer.md` — 4-Port + Session 상태기계
  - `workflow/task/fix/brainstorming/version/0.0.2v/issue-11-selections-version-policy.md` — Selection 버전 정책
- **선행 Product**:
  - [`product-learning-tower.md`](product-learning-tower.md) Epic 1~4 (필수)
  - [`product-ai-suggestion.md`](product-ai-suggestion.md) (4-Port 인프라)
- **관련 ADR**:
  - [ADR010](../../../../../docs/adr/ADR010.md) — Fallback 정책
  - [ADR021](../../../../../docs/adr/ADR021-axis-deck-full-integration.md) — Axis Soft Delete (commit 시 axis 저장 사이드이펙트)
  - [**ADR022**](../../../../../docs/adr/ADR022-learning-tower-terminology.md) — 학습 계층 용어 (axisDraft/axisConsensus/Roadmap/Selection)
- **DOMAIN.md / PACKAGE.md 갱신 예정**:
  - `docs/DOMAIN.md` — `RoadmapInteractionSession` 절 신설
  - `docs/PACKAGE.md` — `ai/interaction/domain/`, `ai/interaction/application/`, `ai/interaction/presentation/` 절 신설
- **폐기 문서**:
  - 기존 `product-ai-interactive-roadmap.md` (본 파일이 전면 재작성으로 대체)

## 열린 질문 (Open Questions)

1. **세션 TTL 30분 vs 1시간 vs 사용자 활동 기반** — v1 은 30분 고정.
2. **`RoadmapDraft` 를 축 개별 vs 세션 통합** — v1 은 세션 통합 (`AxisDraftSnapshot` 이 축 목록 전체).
3. **Commit 실패 시 세션 상태** — 자동 rollback vs 상태 유지 재시도 허용. v1 은 자동 rollback + 재시도 가능.
4. **사용자 액션의 순서** — 순차 vs 병렬. v1 은 순차(트랜잭션 단순).
5. **다중 Layer 걸친 세션** — v1 은 단일 Layer 스코프 세션만.
6. **세션 재개 UX** — 목록 조회 UI 필요 여부.
7. **Layer 도 axisDraft 흐름에 포함할지** — v1 은 사용자 명시 Layer 선택 후 세션 시작. Layer 자체 draft 는 v1.5.
8. **AxisSelection 정책 (#11) 위반 시 사용자 안내 UX** — 409 응답 시 UI 가 어떻게 재작성 유도할지.
9. **세션 아카이브** — COMMITTED / ABANDONED 세션 삭제 정책. v1 은 90일 배치 정리.
10. **DraftChangeLog 최대 개수** — 무제한 vs 상한. v1 은 상한 없음(정리 배치가 해결).

## 제품 수준 완료 기준 (Product-level DoD)

- [ ] Epic 1~5 완료
- [ ] 통합 테스트: 시작 → axisDraft → 액션 → Roadmap/Selection draft → commit 전체 흐름
- [ ] Flyway 마이그레이션 (session/snapshot/changeLog 테이블)
- [ ] 관측 지표 5종 · Runbook (`docs/runbook/ai-interactive-roadmap.md`)
- [ ] `docs/DOMAIN.md` · `docs/PACKAGE.md` 갱신
- [ ] product-ai-suggestion 4-Port 재사용 검증

---

# [Epic 1] RoadmapInteractionSession Aggregate + 상태기계

> **⚠️ 상태 머신 확장 필요 (2026-07-02) — 이슈 #17 반영.**
> `SessionState` enum(Story 1-2)에 `LAYERS_DRAFTED`, `AXES_DRAFTED`, `CHAPTERS_DRAFTED`, `SUBTREES_DRAFTED` 4개 상태 추가. 기존 단일 `DRAFTED`는 폐기.
> 상태 전이 매핑도 확장 필요 (본 문서 상단 Fix 개정 섹션의 상태 머신 다이어그램 참조).
> 스키마 `state` CHECK 제약(현재 `IN ('INIT','DRAFTED','REVIEWING','COMMITTED','ABANDONED')`)도 새 값 추가로 재작성.

## 목표

세션 도메인의 뼈대(`RoadmapInteractionSession` Aggregate + 상태 enum + 상태 전이) 를 구현해 후속 Epic 이 이 위에서 자란다. **상태 값은 2026-07-02 개정으로 8개로 확장** (이슈 #17).

## 배경

세션 상태기계가 확정되지 않으면 각 API 가 자체 상태 검증 로직을 중복. Aggregate 로 응집.

## 포함 Story

- Story 1-1: `roadmap_interaction_session` 테이블 + `RoadmapInteractionSession` Aggregate
- Story 1-2: `SessionState` enum (`INIT/DRAFTED/REVIEWING/COMMITTED/ABANDONED`) + 상태 전이 검증
- Story 1-3: 세션 시작 API (`POST /roadmap-sessions`)
- Story 1-4: 세션 조회 · abandon · TTL 만료 관리

## Epic 인수 시나리오

- Given 사용자 로그인 상태 / When `POST /roadmap-sessions {facadeId, layerId}` / Then 세션 생성, state=INIT
- Given 세션 존재 / When 30분 후 재접근 / Then `SESSION_EXPIRED` 410
- Given INIT 상태 세션 / When abandon / Then state=ABANDONED

## Epic 완료 기준 (DoD)

- [ ] Story 1-1~1-4 완료
- [ ] Flyway V23 (또는 다음 번호) + Rollback
- [ ] ErrorCode: `SESSION_NOT_FOUND`, `SESSION_EXPIRED`, `SESSION_ABANDONED`, `SESSION_FORBIDDEN`, `SESSION_LAYER_REQUIRED`
- [ ] 단위 테스트 (상태 전이 각 케이스)

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **`SessionState` 저장 방식** — ADR002 VARCHAR + CHECK
- **TTL 관리** — 세션의 `expires_at` 컬럼 + 조회 시 검증 (배치 정리는 아카이브)

---

## [Story 1-1] `roadmap_interaction_session` 테이블 + Aggregate

### User Story

- As a 백엔드 엔지니어
- I want RoadmapInteractionSession 을 도메인 Aggregate 로 정의하기를
- so that 세션 상태·라이프사이클·검증이 응집된다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

`roadmap_interaction_session` 테이블:
- `id BIGINT PK`
- `user_id BIGINT NOT NULL`
- `facade_id BIGINT NOT NULL`
- `layer_id BIGINT NOT NULL`
- `state VARCHAR(10) NOT NULL CHECK (state IN ('INIT','DRAFTED','REVIEWING','COMMITTED','ABANDONED'))`
- `axis_draft_json TEXT NULL` (AxisDraftSnapshot 직렬화)
- `axis_consensus_json TEXT NULL` (AxisConsensusSnapshot 직렬화)
- `expires_at DATETIME(6) NOT NULL`
- `committed_at DATETIME(6) NULL`
- `created_at`, `updated_at`
- INDEX `(user_id, state)`, `(expires_at)`

**핵심 파일**:
- 신규: `db/migration/V23__roadmap_interaction_session.sql`
- 신규: `db/migration/R23__rollback_roadmap_interaction_session.sql`
- 신규: `ai/interaction/domain/model/RoadmapInteractionSession.java`

### 완료 기준 (AC)

- Given V23 실행 / When 스키마 / Then 테이블 존재 + CHECK 제약
- Given `RoadmapInteractionSession.start(userId, facadeId, layerId)` / When 생성 / Then state=INIT, expires_at=now+30분

### Definition of Done

- [ ] V23 + R23
- [ ] Aggregate 클래스
- [ ] 단위 테스트

### 스토리 포인트

2d

### 의존성

- 선행: 없음
- 후행: Story 1-2

---

## [Story 1-2] `SessionState` enum + 상태 전이 검증

### User Story

- As a 도메인 코드 작성자
- I want 상태 전이 규칙이 도메인에 응집되기를
- so that Application Service 가 상태 검증을 재구현하지 않는다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`, ADR022

```java
public enum SessionState {
    INIT, DRAFTED, REVIEWING, COMMITTED, ABANDONED;

    private static final Map<SessionState, Set<SessionState>> ALLOWED = Map.of(
        INIT,       Set.of(DRAFTED, ABANDONED),
        DRAFTED,    Set.of(REVIEWING, ABANDONED, DRAFTED),   // refresh 는 DRAFTED → DRAFTED
        REVIEWING,  Set.of(REVIEWING, COMMITTED, ABANDONED),
        COMMITTED,  Set.of(),
        ABANDONED,  Set.of()
    );

    public void requireTransitionTo(SessionState next) {
        if (!ALLOWED.get(this).contains(next))
            throw new SessionStateTransitionException(this, next);
    }
}
```

### 완료 기준 (AC)

- Given state=INIT / When transitionTo(DRAFTED) / Then 허용
- Given state=INIT / When transitionTo(COMMITTED) / Then `SESSION_STATE_TRANSITION_INVALID`
- Given state=COMMITTED / When transitionTo(anything) / Then 예외

### Definition of Done

- [ ] enum + 검증
- [ ] 단위 테스트 (전체 전이 매트릭스 커버)

### 스토리 포인트

1d

### 의존성

- 선행: Story 1-1
- 후행: Story 1-3

---

## [Story 1-3] 세션 시작 API (`POST /roadmap-sessions`)

### User Story

- As a FE 개발자
- I want 세션을 시작하는 엔드포인트를 사용하기를
- so that 사용자가 새 axisDraft 흐름에 진입할 수 있다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- Request: `{ facadeId, layerId }`
- Response: `{ sessionId, state:"INIT", expiresAt }`
- Application Service: `RoadmapInteractionCommandService.start(userId, facadeId, layerId)` — Layer 소유권 확인 후 세션 생성

**핵심 파일**:
- 신규: `ai/interaction/presentation/RoadmapInteractionController.java`
- 신규: `ai/interaction/application/service/RoadmapInteractionCommandService.java`
- 신규: `ai/interaction/application/service/RoadmapInteractionQueryService.java`

### 완료 기준 (AC)

- Given 로그인 사용자, layer 존재 / When 요청 / Then 201 + sessionId
- Given 다른 유저의 layer / When 요청 / Then 403 `SESSION_FORBIDDEN`
- Given layer 존재하지 않음 / When / Then 404 `LAYER_NOT_FOUND`

### Definition of Done

- [ ] Controller + Service
- [ ] Controller Slice + 통합 테스트

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 1-2
- 후행: Story 1-4

---

## [Story 1-4] 세션 조회 · abandon · TTL 만료

### User Story

- As a 사용자
- I want 세션 진입 후 언제든 abandon 하거나 재개할 수 있기를
- so that 흐름 중단 시 새 세션 시작 부담 없이 되돌아온다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `GET /roadmap-sessions/{id}` — 세션 상태 · axisDraft/Consensus 스냅샷 조회
- `DELETE /roadmap-sessions/{id}` — abandon (state=ABANDONED)
- TTL 만료 세션 접근 시 자동 `SESSION_EXPIRED` 응답

### 완료 기준 (AC)

- Given 세션 조회 / When 요청 / Then 상태 · draft · changeLog 반환
- Given abandon / When / Then state=ABANDONED, 이후 접근 시 `SESSION_ABANDONED` 410
- Given TTL 만료 세션 / When 접근 / Then `SESSION_EXPIRED` 410

### Definition of Done

- [ ] GET/DELETE 엔드포인트
- [ ] TTL 검증 로직
- [ ] 통합 테스트

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 1-3
- 후행: Epic 2

---

# [Epic 2] axisDraft 흐름 (6-Port 호출 + refresh + 챕터 재생성)

> **⚠️ 6-Port 확장 반영 필요 (2026-07-02) — 이슈 #17/#18 반영.**
> `AxisDraftItem` 구조 변경: 기존 `(name, rationale, roadmapDraft: String)` → 신규 `(name, rationale, chapters: List<ChapterDraft(title, rationale, body?)>)`. body는 SUBTREES_DRAFTED 단계에서 채워짐.
> `createAxisDraft` → outline 병렬 호출로 변경. `createChaptersOutline`, `createChaptersSubtrees` 액션 신설.
> 챕터 노드 재생성(`POST /roadmap-sessions/{id}/chapters/{i}/regenerate`) API 신설. 세션 밖에서도 유효한 `POST /roadmap-nodes/{id}/regenerate`는 이슈 #18 참조.
> 저장 매핑: `axis.setRoadmap(content)` 폐기 → `axis.addRoadmapNode(title, rationale, body)` 반복 호출로 챕터 노드 row 생성.

## 목표

`product-ai-suggestion` 의 Layer/Axis Port 를 호출해 초안을 생성하고, 사용자 명시 액션에 의한 refresh 를 지원한다.

## 배경

axisDraft 는 세션 흐름의 진입 자산. 4-Port 호출을 Interaction Application Service 가 조율하고, refresh 는 사용자 액션 시에만 허용.

## 포함 Story

- Story 2-1: `AxisDraftSnapshot` VO + 저장 로직
- Story 2-2: `POST /roadmap-sessions/{id}/axis-drafts` — 최초 초안 생성
- Story 2-3: `POST /roadmap-sessions/{id}/axis-drafts/refresh` — refresh 요청
- Story 2-4: 4-Port 호출 실패 시 처리 (Cascade 결과 반영)

## Epic 인수 시나리오

- Given 세션 state=INIT / When axis-drafts 요청 / Then Layer/Axis Port 호출 → snapshot 저장, state=DRAFTED
- Given state=DRAFTED / When refresh / Then 4-Port 재호출 → snapshot 갱신, state 유지
- Given state=REVIEWING / When refresh / Then `SESSION_STATE_TRANSITION_INVALID` (사용자 액션 진행 중)

## Epic 완료 기준 (DoD)

- [ ] Story 2-1~2-4 완료
- [ ] 통합 테스트 (Static 폴백 시나리오 포함)

---

## [Story 2-1] `AxisDraftSnapshot` VO + 저장

### User Story

- As a 도메인 코드 작성자
- I want axisDraft 스냅샷을 VO 로 표현하기를
- so that 세션이 상태와 함께 저장·조회된다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`, ADR022

```java
public record AxisDraftSnapshot(
    List<AxisDraftItem> axes,
    String refreshContext,
    Instant generatedAt
) {}

public record AxisDraftItem(
    String name, String rationale, String roadmapDraft
) {}
```

`roadmap_interaction_session.axis_draft_json` 컬럼에 직렬화 저장 (Jackson).

### 완료 기준 (AC)

- Given snapshot / When 저장 / Then JSON 컬럼 저장
- Given DB 조회 / When 역직렬화 / Then VO 복원

### Definition of Done

- [ ] VO + Jackson 직렬화 세팅
- [ ] Repository Slice 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Epic 1
- 후행: Story 2-2

---

## [Story 2-2] `POST /roadmap-sessions/{id}/axis-drafts` — 최초 생성

### User Story

- As a 사용자
- I want axisDraft 초안을 받기를
- so that 나의 지도 초안을 확인·수정할 수 있다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

Application Service 흐름:
1. session state=INIT 확인
2. facade 의 concepts, layer 의 name 로드
3. `AxisSuggestionPort.suggest(ctx)` 호출 (via ai-suggestion Cascade)
4. `AxisDraftSnapshot` 생성 후 세션에 저장
5. state=DRAFTED 로 전이

Cascade 결과 `suggestionsAvailable=false` 이면 empty snapshot + 알림.

### 완료 기준 (AC)

- Given session INIT / When 요청 / Then snapshot 저장, state=DRAFTED
- Given AxisSuggestion Cascade suggestionsAvailable=false / When / Then 빈 snapshot 저장, 응답 flag 노출
- Given session DRAFTED / When 두 번째 요청 / Then `SESSION_STATE_TRANSITION_INVALID` (refresh 는 다른 엔드포인트)

### Definition of Done

- [ ] Controller + Service
- [ ] Controller Slice + 통합 테스트 (WireMock Port stub)

### 스토리 포인트

2d

### 의존성

- 선행: Story 2-1, product-ai-suggestion Epic 5
- 후행: Story 2-3

---

## [Story 2-3] `POST /roadmap-sessions/{id}/axis-drafts/refresh` — 재요청

### User Story

- As a 사용자
- I want 같은 세션에서 refresh context 를 바꿔 초안을 다시 받기를
- so that 컨셉을 유지한 채 다양한 축 조합을 탐색한다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

Request body: `{ refreshContext: "운영 축 빼고" }`. Application 흐름:
1. state=DRAFTED 만 허용 (REVIEWING 은 사용자 액션 중)
2. ai-suggestion Port 재호출 (refreshContext 를 프롬프트 컨텍스트로 주입)
3. snapshot 갱신
4. state=DRAFTED 유지

### 완료 기준 (AC)

- Given state=DRAFTED, refreshContext="입문 범위" / When 요청 / Then snapshot 갱신
- Given state=REVIEWING / When / Then `SESSION_STATE_TRANSITION_INVALID`
- Given state=COMMITTED / When / Then `SESSION_ALREADY_COMMITTED`

### Definition of Done

- [ ] Controller + Service
- [ ] 통합 테스트

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 2-2
- 후행: Story 2-4

---

## [Story 2-4] 4-Port 호출 실패 응답 처리

### User Story

- As a 사용자
- I want 4-Port 호출이 실패해도 세션이 유지되기를
- so that 수동 입력으로 axisDraft 를 만들어 진행할 수 있다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`, ADR010

Cascade 최종 실패 시 응답:
- HTTP 200
- Body: `{ axisDraft: null, suggestionsAvailable: false, message: "AI 초안 생성 실패 — 수동으로 axis 를 입력해주세요" }`
- 세션 state=DRAFTED (빈 snapshot)

### 완료 기준 (AC)

- Given 4-Port Cascade suggestionsAvailable=false / When 요청 / Then 200 + suggestionsAvailable=false, session state=DRAFTED
- Given 다음 사용자 액션 (수동 axis 추가) / When 요청 / Then state=REVIEWING (Epic 4)

### Definition of Done

- [ ] Cascade 결과 처리
- [ ] Controller Slice 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Story 2-2
- 후행: Epic 3

---

# [Epic 3] Roadmap/Selection draft 흐름 + Selection 정책 (#11)

## 목표

세션 흐름에서 확정 축에 대한 Roadmap draft, Selection draft 를 생성하고, Selection 정책(#11: name UNIQUE, DESC 정렬, in-place update, hard delete) 을 세션 UI 관점에서 강제한다.

## 배경

axisDraft 만으로는 축의 헌법(Roadmap) 과 사례(Selection) 가 초안화되지 않음. 사용자가 축을 확정한 후 Roadmap/Selection 초안을 요청할 수 있어야 함.

## 포함 Story

- Story 3-1: `POST /roadmap-sessions/{id}/roadmap-drafts { axisName }` — 축 확정 후 Roadmap 초안
- Story 3-2: `POST /roadmap-sessions/{id}/selection-drafts { axisName }` — Selection 초안
- Story 3-3: Selection 정책(#11) 강제 — draft 상태에서도 name 중복 방지
- Story 3-4: draft 편집 API — 세션 안에서 Roadmap/Selection content 수정

## Epic 인수 시나리오

- Given state=REVIEWING, 축 확정 / When roadmap-drafts 요청 / Then RoadmapSuggestionPort 호출 → draft 반환 (세션에 임시 저장)
- Given roadmapDraft 존재 / When selection-drafts 요청 / Then SelectionsSuggestionPort 호출
- Given roadmap draft 없이 selection-drafts 요청 / When / Then `AXIS_ROADMAP_REQUIRED` 400
- Given 같은 axis 내 selection name 중복 / When draft 저장 / Then `AXIS_SELECTION_NAME_DUPLICATE`

## Epic 완료 기준 (DoD)

- [ ] Story 3-1~3-4 완료
- [ ] Selection 정책 통합 테스트

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **Draft 저장 위치** — `AxisDraftItem.roadmapDraft` (기존) + 추가 필드 `selectionDrafts: List<{name, content}>` — VO 확장.
- **Draft 편집** — in-place update 가능, 최종 저장 시 정책 재검증.

---

## [Story 3-1] `POST /roadmap-drafts { axisName }`

### User Story

- As a 사용자
- I want 확정한 축에 대한 로드맵 초안을 AI 로 받기를
- so that 축의 헌법 트리를 작성할 부담을 줄인다

### 설명

> 출처: `issue-06-roadmap-selections-dualaxis.md`, `issue-09-ai-suggestion-3layer.md`

Application 흐름:
1. state ∈ {REVIEWING} 검증
2. axisDraft.axes 에 axisName 존재 확인
3. `RoadmapSuggestionPort.suggest(RoadmapSuggestionContext)` 호출
4. 결과 content 를 `AxisDraftItem.roadmapDraft` 에 저장

### 완료 기준 (AC)

- Given state=REVIEWING, 축 존재 / When 요청 / Then draft.content 갱신
- Given 축 이름 매칭 실패 / When / Then `AXIS_NAME_NOT_IN_DRAFT` 400
- Given Cascade 실패 / When / Then draft.content=null, suggestionsAvailable=false

### Definition of Done

- [ ] Controller + Service
- [ ] 통합 테스트

### 스토리 포인트

1.5d

### 의존성

- 선행: Epic 2
- 후행: Story 3-2

---

## [Story 3-2] `POST /selection-drafts { axisName }`

### User Story

- As a 사용자
- I want 확정 로드맵 기반 selection 초안을 받기를
- so that 사례/응용을 초안화한다

### 설명

> 출처: `issue-06-roadmap-selections-dualaxis.md`, `issue-09-ai-suggestion-3layer.md`

Application 흐름:
1. state ∈ {REVIEWING} 검증
2. axisDraft.axes 에 axisName 존재 + roadmapDraft.content != null 확인
3. `SelectionsSuggestionPort.suggest(SelectionsSuggestionContext)` 호출
4. 결과 selections 목록을 `AxisDraftItem.selectionDrafts` 에 저장

### 완료 기준 (AC)

- Given roadmapDraft 존재 / When 요청 / Then selectionDrafts 저장
- Given roadmapDraft 없음 / When / Then `AXIS_ROADMAP_REQUIRED` 400

### Definition of Done

- [ ] Controller + Service
- [ ] 통합 테스트

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 3-1
- 후행: Story 3-3

---

## [Story 3-3] Selection 정책(#11) 강제 — draft 상태

### User Story

- As a 도메인 코드 작성자
- I want draft 상태에서도 같은 axis 내 selection name 이 유니크하기를
- so that commit 시점에 정책 위반이 발견되지 않는다

### 설명

> 출처: `issue-11-selections-version-policy.md`

`AxisDraftItem.selectionDrafts` 에 새 selection 추가 시 name 중복 검사. `session.addSelectionDraft(axisName, {name, content})` 도메인 메서드에서 검증.

### 완료 기준 (AC)

- Given axis 에 draft selection ["A","B"] / When "A" 추가 / Then `AXIS_SELECTION_NAME_DUPLICATE`
- Given `updateSelectionDraft(axisName, "A", newContent)` / When / Then in-place update

### Definition of Done

- [ ] 도메인 검증
- [ ] 단위 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Story 3-2
- 후행: Story 3-4

---

## [Story 3-4] Draft 편집 API

### User Story

- As a 사용자
- I want draft roadmap/selection content 를 세션 안에서 편집하기를
- so that AI 초안을 수동으로 다듬을 수 있다

### 설명

> 출처: `issue-11-selections-version-policy.md`

- `PATCH /roadmap-sessions/{id}/axes/{axisName}/roadmap-draft { content }` — Roadmap draft 편집
- `PATCH /roadmap-sessions/{id}/axes/{axisName}/selection-drafts/{name} { content }` — Selection draft 편집
- `DELETE /roadmap-sessions/{id}/axes/{axisName}/selection-drafts/{name}` — Selection draft 제거

### 완료 기준 (AC)

- Given draft 존재 / When PATCH / Then content 갱신
- Given DELETE / When / Then selection draft 제거

### Definition of Done

- [ ] Controller + Service
- [ ] 통합 테스트

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 3-3
- 후행: Epic 4

---

# [Epic 4] 사용자 액션 + DraftChangeLog + 결정형 검증

## 목표

사용자 액션(keep/remove/rename/add/reorder) 을 axisDraft 에 적용하고 `DraftChangeLog` 로 diff 를 보존하며, commit 진입 전 결정형 검증을 강제한다.

## 배경

세션의 핵심은 사용자 참여. 액션 하나하나가 저장 가능한 axisConsensus 로 나아가는 단계이며, 검증 실패 시 저장 진입 불가.

## 포함 Story

- Story 4-1: `DraftChangeLog` Entity 신설 (V24)
- Story 4-2: `PATCH /roadmap-sessions/{id}/axes { action, target }` — 사용자 액션 적용
- Story 4-3: `applyUserAction` 도메인 메서드 (keep/remove/rename/add/reorder)
- Story 4-4: 결정형 검증 (blank/duplicate/order/consensus-ready)

## Epic 인수 시나리오

- Given state=DRAFTED / When action="keep", axisName="Java" / Then log 추가, state=REVIEWING
- Given action="rename", from="X", to="Y" / When / Then draft axis 이름 변경 + log
- Given action="add", axisName="새 축" / When / Then draft 에 축 추가 + log
- Given commit 요청 / When 검증 결과 axis 0건 / Then `AXIS_CONSENSUS_EMPTY`

## Epic 완료 기준 (DoD)

- [ ] Story 4-1~4-4 완료
- [ ] Flyway V24
- [ ] ErrorCode: `AXIS_CONSENSUS_EMPTY`, `AXIS_CONSENSUS_NAME_DUPLICATE`, `AXIS_NAME_NOT_IN_DRAFT`

---

## [Story 4-1] `DraftChangeLog` Entity + 테이블

### User Story

- As a 백엔드 엔지니어
- I want 사용자 액션이 시간순 로그로 저장되기를
- so that diff 조회 · 실험 분석 가능

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

`draft_change_log`:
- `id BIGINT PK`
- `session_id BIGINT FK NOT NULL`
- `action VARCHAR(20) NOT NULL CHECK (action IN ('keep','remove','rename','add','reorder'))`
- `payload_json TEXT NOT NULL`
- `created_at DATETIME(6) NOT NULL`
- INDEX `(session_id, created_at)`

### 완료 기준 (AC)

- Given 액션 발생 / When append / Then log row 저장
- Given 세션 조회 / When changeLog / Then 시간순 목록

### Definition of Done

- [ ] V24 + Entity
- [ ] Repository Slice

### 스토리 포인트

1d

### 의존성

- 선행: Epic 3
- 후행: Story 4-2

---

## [Story 4-2] `PATCH /axes { action, target }`

### User Story

- As a 사용자
- I want axisDraft 에 액션을 적용하기를
- so that 초안을 나의 지도로 다듬는다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

Body: `{ action:"keep|remove|rename|add|reorder", target:{...} }`. Application 흐름:
1. state ∈ {DRAFTED, REVIEWING} 검증
2. `session.applyAction(action)` 호출 (도메인)
3. `DraftChangeLog.append(session, action)`
4. state=REVIEWING

### 완료 기준 (AC)

- Given action=keep / When / Then log 추가, state=REVIEWING
- Given action=remove, axisName="X" / When / Then draft 에서 X 제거
- Given action=add, {name, rationale} / When / Then draft 에 신규 axis 추가
- Given action=rename, {from,to} / When / Then draft 갱신
- Given action=reorder, {orderedNames} / When / Then draft 순서 재부여

### Definition of Done

- [ ] Controller + Service
- [ ] 통합 테스트

### 스토리 포인트

2d

### 의존성

- 선행: Story 4-1
- 후행: Story 4-3

---

## [Story 4-3] `applyUserAction` 도메인 메서드

### User Story

- As a 도메인 코드 작성자
- I want 사용자 액션이 도메인 메서드로 응집되기를
- so that Application 이 draft 컬렉션을 직접 조작하지 않는다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

```java
public class RoadmapInteractionSession {
    public void applyAction(UserAction action) {
        state.requireTransitionTo(REVIEWING);
        var updated = switch (action) {
            case Keep k -> axisDraft.keep(k.axisName());
            case Remove r -> axisDraft.remove(r.axisName());
            case Rename rn -> axisDraft.rename(rn.from(), rn.to());
            case Add a -> axisDraft.add(a.item());
            case Reorder ro -> axisDraft.reorder(ro.orderedNames());
        };
        this.axisDraft = updated;
        this.state = REVIEWING;
    }
}
```

### 완료 기준 (AC)

- Given 각 액션 / When 실행 / Then draft 갱신
- Given 이미 존재하는 이름으로 add / When / Then `AXIS_CONSENSUS_NAME_DUPLICATE`

### Definition of Done

- [ ] 도메인 메서드
- [ ] 단위 테스트 (액션 5종 각각)

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 4-2
- 후행: Story 4-4

---

## [Story 4-4] 결정형 검증 (consensus-ready)

### User Story

- As a 도메인 코드 작성자
- I want commit 진입 전 확정형 검증을 강제하기를
- so that AI 초안이라도 검증 통과 후에만 저장된다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

`Session.validateForCommit()` 검증 규칙:
- `axes.size() >= 1` — 최소 1개
- 축 이름 blank 없음, 100자 이내
- 축 이름 중복 없음
- (선택) 각 축에 roadmapDraft.content 존재
- displayOrder 는 commit 시점에 도메인이 자동 부여

### 완료 기준 (AC)

- Given axes 0건 / When validate / Then `AXIS_CONSENSUS_EMPTY`
- Given axes 이름 중복 / When / Then `AXIS_CONSENSUS_NAME_DUPLICATE`
- Given axes 이름 blank / When / Then `AXIS_CONSENSUS_NAME_BLANK`
- Given 모두 통과 / When / Then 통과

### Definition of Done

- [ ] 검증 메서드
- [ ] 단위 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Story 4-3
- 후행: Epic 5

---

# [Epic 5] Commit + LearningFacade Aggregate 행위 호출 + 관측

## 목표

`POST /roadmap-sessions/{id}/commit` 로 axisConsensus 를 확정하고 LearningFacade / Layer / Axis Aggregate 행위를 호출해 저장. 관측 지표와 Runbook 발행.

## 배경

세션의 최종 결과가 실제 도메인에 반영되어야 사용자 가치가 발생. 저장은 도메인 규약(Aggregate 행위) 준수. 관측 지표로 UX 실효성 확인.

## 포함 Story

- Story 5-1: `POST /commit` 엔드포인트 + Application 흐름
- Story 5-2: `AxisConsensusSnapshot` VO + `session.buildConsensus()`
- Story 5-3: LearningFacade / Layer / Axis / AxisRoadmap / AxisSelection Aggregate 행위 호출 조율
- Story 5-4: 관측 지표 5종 + Runbook 발행

## Epic 인수 시나리오

- Given 유효한 axisDraft / When commit / Then Layer.addAxis N회, Axis.setRoadmap N회, Axis.addSelection M회 호출, state=COMMITTED, `LearningAxisCreatedEvent` 발행
- Given validate 실패 / When commit / Then 400 (해당 ErrorCode)
- Given 이미 COMMITTED 세션 / When commit / Then 409 `SESSION_ALREADY_COMMITTED`
- Given axis 저장 중 예외 (Layer 삭제됨 등) / When / Then 전체 트랜잭션 롤백, session state 원상

## Epic 완료 기준 (DoD)

- [ ] Story 5-1~5-4 완료
- [ ] 통합 테스트: 전체 흐름
- [ ] 관측 지표 · Runbook

---

## [Story 5-1] `POST /commit` 엔드포인트

### User Story

- As a 사용자
- I want 세션을 확정 저장하기를
- so that 정련한 axisDraft 가 나의 LearningFacade 에 반영된다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

Application 흐름:
1. state 검증 (REVIEWING 만 허용)
2. `session.validateForCommit()` 결정형 검증
3. `session.buildConsensus()` → AxisConsensusSnapshot
4. LearningFacade / Layer Aggregate 행위 호출 (Story 5-3)
5. session.state=COMMITTED, committed_at 기록

### 완료 기준 (AC)

- Given REVIEWING + valid draft / When commit / Then 200 + committed 결과
- Given DRAFTED / When commit / Then `SESSION_STATE_TRANSITION_INVALID` (REVIEWING 진입 필요)
- Given COMMITTED / When commit / Then `SESSION_ALREADY_COMMITTED`

### Definition of Done

- [ ] Controller + Service
- [ ] 통합 테스트

### 스토리 포인트

1.5d

### 의존성

- 선행: Epic 4
- 후행: Story 5-2

---

## [Story 5-2] `AxisConsensusSnapshot` VO + `buildConsensus()`

### User Story

- As a 도메인 코드 작성자
- I want axisDraft 를 axisConsensus 로 승격하는 로직이 응집되기를
- so that Application 이 draft 를 직접 저장 로직으로 변환하지 않는다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`, ADR022

```java
public record AxisConsensusSnapshot(
    List<AxisConsensusItem> axes,
    Instant committedAt
) {}

public record AxisConsensusItem(
    String name,
    String roadmapContent,
    List<AxisConsensusSelection> selections
) {}
```

`session.buildConsensus()` — draft 를 consensus VO 로 변환. `AxisDraftItem.roadmapDraft.content` → `AxisConsensusItem.roadmapContent`, `AxisDraftItem.selectionDrafts` → `AxisConsensusItem.selections`.

### 완료 기준 (AC)

- Given valid draft / When buildConsensus / Then AxisConsensusSnapshot 반환
- Given axisConsensus_json 컬럼 저장 / When 조회 / Then 역직렬화 성공

### Definition of Done

- [ ] VO + build 메서드
- [ ] 단위 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Story 5-1
- 후행: Story 5-3

---

## [Story 5-3] Aggregate 행위 호출 조율

### User Story

- As a Application 조율자
- I want 세션 commit 이 Layer/Axis/Roadmap/Selection Aggregate 행위를 호출하기를
- so that 저장이 도메인 규약을 준수한다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

```java
@Transactional
public CommitResult commit(Long userId, Long sessionId) {
    var session = repository.findById(sessionId);
    session.validateForCommit();
    var consensus = session.buildConsensus();

    var facade = facadeRepository.findByUserId(userId);
    var layer = facade.findLayer(session.layerId());

    var addedAxisIds = new ArrayList<Long>();
    int roadmapCount = 0, selectionCount = 0;
    for (var item : consensus.axes()) {
        var axis = layer.addAxis(item.name());  // LearningAxisCreatedEvent 발행
        if (item.roadmapContent() != null) {
            axis.setRoadmap(item.roadmapContent());
            roadmapCount++;
        }
        for (var sel : item.selections()) {
            axis.addSelection(sel.name(), sel.content());
            selectionCount++;
        }
        addedAxisIds.add(axis.getId());
    }
    facadeRepository.save(facade);

    session.commit(consensus);
    repository.save(session);

    return new CommitResult(sessionId, addedAxisIds, roadmapCount, selectionCount);
}
```

### 완료 기준 (AC)

- Given 3 axis · 2 roadmap · 4 selection consensus / When commit / Then 저장 완료, 이벤트 발행, session COMMITTED
- Given Layer 삭제된 상태 / When commit / Then `LAYER_NOT_FOUND`, 트랜잭션 롤백
- Given Axis 이름이 이미 존재 (다른 세션에서 저장됨) / When / Then `LEARNING_AXIS_DUPLICATE_NAME`, 롤백

### Definition of Done

- [ ] Service 조율
- [ ] 통합 테스트 (Aggregate 행위 · 이벤트 발행 검증)

### 스토리 포인트

2.5d

### 의존성

- 선행: Story 5-2, product-learning-tower Epic 1~4 완료
- 후행: Story 5-4

---

## [Story 5-4] 관측 지표 5종 + Runbook

### User Story

- As a 운영자
- I want 세션 지표와 대응 절차가 문서화되기를
- so that UX 실효성 관찰 · 문제 발생 시 대응 가능

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

**지표**:
- `thirdtool.roadmap_session.started_total`
- `thirdtool.roadmap_session.committed_total`
- `thirdtool.roadmap_session.abandoned_total{reason}`
- `thirdtool.roadmap_session.commit_axis_count` (히스토그램)
- `thirdtool.roadmap_session.action_count` (히스토그램)

**Runbook** (`docs/runbook/ai-interactive-roadmap.md`):
1. 세션 시작 후 draft 생성 실패 급증 → product-ai-suggestion 상태 확인
2. abandon 비율 급증 → TTL 조정 · UX 회고
3. Commit 시 롤백 급증 → Aggregate 검증 규칙 회고

### 완료 기준 (AC)

- Given 지표 등록 / When Prometheus scrape / Then 5종 노출
- Given Runbook / When 조회 / Then 3시나리오 존재

### Definition of Done

- [ ] MeterBinder
- [ ] MDC 필드 추가 (`session_id`, `session_state`)
- [ ] Runbook 문서

### 스토리 포인트

1d

### 의존성

- 선행: Story 5-3
- 후행: 없음

---

*작성일: 2026-07-01 | 상태: **5 Epic · 21 Story 전체 pending** | Milestone 후보: milestone 3 (learning-tower + ai-suggestion 완료 후). 기존 `product-ai-interactive-roadmap.md` 전면 재작성.*
