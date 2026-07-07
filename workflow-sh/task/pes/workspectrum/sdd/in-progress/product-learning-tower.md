# [Product] Learning Tower — 학습 모델 재편

## Product Vision

> **LearningFacade 하위 학습 모델을 `concepts[] → Layer → Axis → {AxisRoadmap, AxisSelection[]} → Card` 로 재편해 AI 제안·Review 재조회의 축을 통일한다.**
> Deck BC 는 Axis 로 완전 흡수되어 폐기하고, Review 는 `AXIS` / `LAYER` 두 스코프를 명시적으로 지원한다.

---

## 🔄 Fix 개정 (2026-07-02) — 이슈 #15~#20 반영

**개정 사유**: AI가 뽑는 초안 자체의 품질은 사람이 만족 가능하지만, (1) 통짜 트리가 방대 (2) 부분 수정 어려움 (3) 사용자 의견 반영 통로 부재 세 부족이 확인됨. 이슈 #6/#11에서 굳혔던 `AxisRoadmap = axis 단위 1개 content TEXT`, `AxisSelection = 컨테이너 하나에 content TEXT` 결정을 **챕터 노드 first-class 승격 (Coarse + body TEXT ASCII 통짜)** 로 뒤집는다.

### 개정 범위 요약

| 영역 | 이전 (#6/#11) | 개정 후 (#15/#16) |
|---|---|---|
| Roadmap 저장 | `axis_roadmap(axis_id UNIQUE, content TEXT)` axis당 1개 | `axis_roadmap_node(axis_id, display_order, title, rationale, body TEXT)` — 챕터 row N개, body에 챕터 subtree ASCII 통짜 |
| Selection 저장 | `axis_selection(axis_id, name, content TEXT)` | `axis_selection(id, axis_id, name)` 컨테이너 + `axis_selection_node(selection_id, display_order, title, rationale, body TEXT)` 자식 |
| Selection 정책 (#11) | name UNIQUE, created_at DESC, in-place, hard delete | 컨테이너 레벨에서 **그대로 유지**. 노드 CRUD 정책만 신설 |
| AI 생성 | `RoadmapSuggestionPort` 1회 → axis 전체 트리 통짜 | `ChaptersOutlinePort`(챕터 리스트) → `ChapterSubtreePort`(챕터별 병렬) 2단계 (이슈 #17) |
| 노드 재생성 API | 없음 | `POST /roadmap-nodes/{id}/regenerate { hint? }` (챕터 통짜 재생성, v1) — 이슈 #18 |
| roadmap vs selections 개념 | ASCII 저장 원칙만 명시 | 명시적 **콘텐츠 카탈로그 6종·5종 + 수렴/발산 판별 기준** AI 프롬프트 embed + 도메인 의도 섹션 삽입 (이슈 #19) |
| AI 비용 통제 | 미검토 | v1엔 관찰 지표 로깅만, 예산 상한은 v2 backlog (이슈 #20) |

### roadmap/selections 개념 명세 (도메인 의도 — 이슈 #19 embed)

**정의**  
- **roadmap = 기준의 저장소, 수렴된 원리** — "자주 안 바뀌고, 바뀌더라도 천천히 바뀌며, 하위 의사결정의 기준이 되는 지식". axis당 여러 챕터 노드로 나뉘어 축적되지만 성격은 수렴.
- **selections = 가능성의 저장소, 발산되는 적용안** — 그 기준을 들고 실제로 어떤 방향들로 뻗어나가며 비교할 것인가. axis당 여러 컨테이너 × 컨테이너당 여러 챕터 노드.

**roadmap 콘텐츠 카탈로그 (6종)**
1. 변하지 않는 핵심 개념 — 정규화/비정규화·문제 정의·우선순위 기준 등 프로젝트 무관하게 유효
2. 반복되는 질문의 축 — "언제 분리해야 하는가", "어떤 안티패턴이 반복되는가" 같은 사고 질문 구조
3. 선택 기준 — 옵션 자체가 아닌 옵션 평가 기준 (추상화 비용·팀 숙련도·변경 빈도)
4. 트레이드오프 골격 — 속도 vs 안정성, 재사용성 vs 맥락 적합성 같은 오래가는 축
5. 안티패턴/함정 카탈로그 — 시간이 지나도 반복 재발하는 실수 패턴
6. 계층 구조/의존 순서 — 원리 → 패턴 → 사례 → 안티패턴 같은 backbone

**selections 콘텐츠 카탈로그 (5종)**
1. 분기 가능한 관점들 — 같은 문제의 여러 갈래 (screen-sequence-first / domain-first / package-first 등)
2. 적용 시나리오별 변형 — 작은 팀 vs 큰 팀, AI 독자 vs 사람 독자 등 조건별
3. 비교표/옵션 라이브러리 — A안/B안/C안 + 언제 채택할지
4. 실험 패턴 — staging 도입, 3회 반복 후 승격 등
5. 파생 규칙/운영 방법 — roadmap 승격 정책, 세션 관리 규칙 등

**수렴 point 판별 (→ roadmap 후보)** — 시간 지나도 안 바뀜 / 여러 사례 관통하는 상위 기준 / 개별 사례보다 판단축 / 팀 바뀌어도 재사용 / backbone 역할

**발산 point 판별 (→ selections 후보)** — 상황 따라 답 달라짐 / 여러 대안 공존 / 실험·비교 중요 / 조직·도메인 맥락 민감 / 아직 일반화 이름

**리뷰어·팀원 판단 기준**: 필드·API가 어느 축에 속하는지 애매할 때 위 카탈로그·판별기준을 참조. 다른 팀 이식 시에도 이 섹션이 유일한 개념 원천.

### Epic 3 스토리 상태

Epic 3 (`AxisRoadmap`/`AxisSelection` 이중 축)의 Story 3-1 ~ 3-4는 **SUPERSEDED** 상태. 실제 구현 태스크는 이슈 #15~#20의 이관 산출물로 재편성 예정. Story 3-5(AxisTopic 폐기)는 유효.

---

## 배경 및 문제

- 현재 상황 (As-Is)
  - `LearningFacade.concept: String` (단일 컨셉) — 여러 관점(예: "백엔드 + 시스템 설계 + 도메인 언어") 을 함께 표현할 수 없음.
  - `LearningFacade → Axis → AxisTopic → Card` 트리 — Topic 계층은 대부분 축과 카드 사이의 잉여 계층. 실제 학습 단위는 축 그 자체.
  - `Deck` BC 가 Axis 와 사실상 1:1 (ADR021 로 자동 생성 유일화 완료) — 별도 BC 로 유지할 정당성 소멸.
  - `ReviewSession` 은 `deckId` 를 참조 — Axis 스코프·Layer 스코프 리뷰 표현 불가능.
  - "Layer 1" 이라는 UI 라벨이 facade-wide review 를 지시하지만 코드에는 그런 개념이 없음 → 사용자가 이해하기 어렵고 개발자도 매핑 혼란.
- 발생하는 문제
  - **AI 제안이 다중 관점을 못 잡음** — 컨셉이 단일 문자열이라 Layer 추천을 만들 입력이 부족(#04).
  - **Topic 계층의 존재 의문** — Card 는 실제로는 축 스코프에서 조회되고 Topic 은 대부분 축과 동어반복. `card.topic_id` 조회 시마다 `topic.axis_id` 로 다시 매핑하는 join 이 반복(#07).
  - **Deck 유지 부담** — ADR021 이후 Deck 의 자체 책임이 사라졌으나 BC 는 남아 있어 조회·API·이벤트 3중 경로가 잉여로 유지됨(#13).
  - **Layer 스코프 리뷰 부재** — 사용자가 "이 그룹 축 전체를 오늘 훑고 싶다" 는 요구를 하지만 axis 개별로만 리뷰 진입 가능. Facade-wide 는 "Layer 1" 이라는 이상한 라벨로 표기(#14).
  - **Roadmap/Selection 없음** — 축의 "헌법(로드맵)" 과 "사례(선정안)" 을 구분해 저장할 방법이 없음. Topic 이 둘을 억지로 겸함(#06).
- 왜 지금 해결해야 하는가
  - `fix-axis-deck-full-integration/0.0.2v` (ADR021, 2026-07-01) 로 Deck 자동 생성 유일화 · LearningAxis Soft Delete 승격이 확정되어 다음 단계 재편의 발판이 완비됨.
  - `product-ai-suggestion.md` (재작성 예정) 이 4-Port 오케스트레이션을 요구하는데, 그 입력이 `concepts[]` · Layer · Axis · Roadmap · Selection 인 상태에서만 성립. 학습 모델 재편이 AI 재편의 선행 조건.
  - `product-ai-interactive-roadmap.md` (재작성 예정) 이 `axisDraft → axisConsensus → LearningFacade.addAxis()` 흐름을 요구. 여기서도 Layer/Roadmap/Selection 신설이 선행.
  - 트래픽 0 상태 — 데이터 마이그레이션 breaking 을 감내 가능한 유일한 시점.

## 목표 (To-Be)

- **`LearningFacade.concepts: List<String>`** (1~5) — 자식 테이블 `learning_facade_concept` 로 정규화. UNIQUE `(facade_id, value)` 로 중복 방지(#04, ADR022).
- **`Layer` 도메인 신설** — LearningFacade 하위, Axis 상위. `learning_layer` 테이블. Soft Delete (ADR003 사용자 자산성 재분류). 기존 데이터는 default "Uncategorized" Layer 로 백필(#05).
- **`AxisRoadmapNode` (챕터 노드 first-class) 도입** — `axis_roadmap_node(axis_id, display_order, title, rationale, body TEXT, audit + soft delete)`. body에 챕터 subtree(1-1~1-N + 리프 본문 포함) ASCII 통짜. 이슈 #6의 `axis_roadmap.content TEXT` 통짜 방식 뒤집힘 (이슈 #15).
- **`AxisSelection` 컨테이너 + `AxisSelectionNode` 자식 노드 도입** — 컨테이너 정책(name UNIQUE per axis, `created_at DESC` 정렬, in-place update, hard delete)는 이슈 #11에서 계승. 노드 CRUD 정책은 이슈 #16에서 신설. `AxisTopic` / `TopicMaterial` soft-deprecate(#06, ADR022) 유지.
- **`Card.axis_id NOT NULL` 직접 매핑** — 3단계 마이그레이션(nullable → 백필 → NOT NULL). `card.topic_id` soft-deprecate. Coverage 재계산은 axis 스코프로 이관(#07).
- **Deck BC 폐기 + Axis 흡수** — 8개 책임(`progressStatus`, `mode`, `lastAccessed`, `learningMaterialId`, `onLibrary`, `publishedAt`, 서브 필드 2건) Axis 로 이전. `/decks/*` 엔드포인트는 `/axes/*` 로 통합. `deck` 테이블은 `_archived_deck` 로 RENAME 후 다음 릴리스에서 DROP(#13).
- **`ReviewSession.scope: ReviewScope` enum 도입** — `AXIS` / `LAYER` 이중 스코프. `findAllByAxisId(axisId)` / `findAllByLayerId(layerId)` Repository 쿼리 신설. `/layers/{id}/review-sessions` 엔드포인트 신설. `Layer.progressStatus` 는 하위 axis statuses 로부터 3-state(`NOT_STARTED` / `IN_PROGRESS` / `COMPLETED`) 파생(#14).
- **모든 신규 파일은 ADR022 용어를 준수** — `AxisTopic` 은 폐기 문맥에서만 등장, `concept` 단수 표기 신규 문서에서 금지.

## 설계 결정 (Design Decisions)

- **Layer 를 축 그룹핑 계층으로 승격 (vs Axis 태그 유지)**
  - 축 이름 앞에 그룹 태그를 붙이는 대안은 그룹 조회 성능 저하(`WHERE name LIKE 'group:%'` 필터) + UI 그룹핑 로직이 문자열 파싱에 의존.
  - Layer 는 정식 Aggregate 로 승격. `learning_layer` 테이블 소유. Soft Delete + display_order 관행 그대로.
  - 비용: 마이그레이션 시점에 기존 axis 를 default `Uncategorized` Layer 에 소속시키는 백필 필요.
  - 보상: Layer 스코프 리뷰(#14) · Layer 별 progressStatus 파생(#14) · Layer 단위 축 추천(#09) 등 후속 확장의 자연스러운 지지대.
- **Roadmap/Selection 은 챕터 노드(Coarse) + body TEXT ASCII 통짜 (이슈 #15/#16, 2026-07-02 개정)**
  - 노드 = 챕터 subtree 통짜. 스키마는 `axis_roadmap_node` / `axis_selection_node` — 챕터 row 하나에 body TEXT ASCII 통짜.
  - JSON 저장 대안 거부 — 사용자 ASCII 편집·diff 강점 손실, 프론트 렌더러 복잡화, v1엔 라인별 metadata 니즈 없음.
  - Roadmap은 axis당 챕터 노드 N개, Selection은 axis당 컨테이너 N개 × 컨테이너당 챕터 노드 N개.
  - body는 `TEXT` (VARCHAR(2000) 상한 폐기 — 사용자 예시 하네스 로드맵 챕터 subtree가 이미 상한에 근접).
  - AI Adapter 응답(챕터 subtree ASCII)을 파싱 없이 body에 그대로 저장(이슈 #17).
  - **원래 이슈 #6의 "axis당 content TEXT 1개" 결정은 뒤집혔음**. ASCII 저장 원칙은 body 필드 안에서 계승.
- **Deck 는 완전 폐기 (vs 별칭 유지)**
  - "Deck 라벨을 axis 조회 응답에 유지" 대안은 이중 진실 소스. 사용자 UI 에서 축 = 덱 이 이미 확정된 상황에서 별칭은 오히려 혼란.
  - `_archived_deck` 로 RENAME 후 다음 릴리스에서 물리 DROP. 기존 이력(ADR020/021) 은 본 Product Epic 5 "명세 변경 이력" 블록에 보존.
- **Review scope 는 열거형 (vs 다형 세션 클래스)**
  - `ReviewSession` 서브클래스(AxisReviewSession / LayerReviewSession) 대안은 슬라이스 테스트 폭발 + Repository 쿼리 이중화.
  - 단일 `ReviewSession` + `scope: ReviewScope` enum + `scopeId: Long` (axisId 또는 layerId) 조합.
  - `findAllByAxisId(axisId)` / `findAllByLayerId(layerId)` 는 `WHERE scope = ? AND scope_id = ?` 로 단일 쿼리 구현.
- **Card→Axis 직접 매핑 (vs Topic 계층 유지)**
  - Topic 계층 유지 대안은 축 스코프 조회 시 `card → topic → axis` 이중 join 반복. Coverage 재계산도 topic 단위 → axis 집계 이중.
  - Direct 매핑 후 topic 은 실제 학습 콘텐츠 없이 이름만 유지되던 상태 → 삭제 안전.
  - Roadmap/Selection 이 Topic 이 하던 "축 하위 구조 표현" 역할을 흡수.
- **concepts 는 1~5 배열, 자식 테이블로 정규화 (vs JSON 컬럼)**
  - JSON 컬럼 대안은 중복 방지 UNIQUE 강제 어려움, 개별 concept 값 검색 어려움.
  - 자식 테이블 + UNIQUE `(facade_id, value)` + `display_order` — 표준 관행 그대로 따른다.

## 대안 검토 (Alternatives Considered)

### 갈림길 A. Layer 위치 (그룹 계층 승격 vs 태그)

**Option A — Axis 문자열 태그 (`axis.groupTag: String?`)**
- 장점: 스키마 변경 최소. 기존 axis 조회 그대로 재사용.
- 거부 이유: 그룹 조회 시 `LIKE` / `substring` 필터, UI 그룹핑이 문자열 파싱 의존, Layer 자체의 진행률 파생 불가.

**Option B (선택) — Layer Aggregate 승격 + `learning_layer` 테이블**
- 비용: Flyway 마이그레이션 + Axis FK 재배선 + 기본 Layer 백필. Layer softDelete 시 소속 Axis 연쇄 정책 결정.
- 보상: Layer 스코프 리뷰·진행률 파생·AI Layer 추천 등 후속 확장이 자연스럽게 지지됨.

**Option C — LearningFacade 하위에 Axis + 별도 `layer_group` matrix 테이블 (다대다)**
- 거부 이유: Axis 는 개념적으로 하나의 Layer 에만 소속. 다대다는 요구 사항 초과. 조회 복잡도 상승.

### 갈림길 B. Roadmap/Selection 저장 형식 (2026-07-02 개정)

**Option A — JSON 구조화 (`roadmap_tree: JSON`)**
- 장점: 프로그램 파싱 용이. Frontend 트리 렌더링 편함.
- 거부 이유: 사용자가 직접 편집하기 어려움(별도 JSON 편집 UI 필요). Diff 표현이 프로그램적임.

**Option B — axis당 단일 ASCII 텍스트 (`axis_roadmap.content VARCHAR(2000)` 또는 TEXT) [이슈 #6 원래 선택, 개정 후 뒤집힘]**
- 장점: 사용자가 즉시 편집 가능. Diff 는 텍스트 diff 로 자연스러움. AI 응답도 ASCII 그대로 사용 가능.
- 거부 사유 (2026-07-02): 통짜 트리 방대 → 소화 어려움 / 부분 수정 어려움 / 사용자 의견 반영 통로 부재. AI 품질 검증 결과 세 부족 확인.

**Option C — Markdown**
- 거부 이유: Markdown 렌더러 의존. 사용자 학습 부담. ASCII 트리로 충분.

**Option D (선택, 2026-07-02) — 챕터 노드 first-class (Coarse) + body TEXT ASCII 통짜**
- 스키마: `axis_roadmap_node(axis_id, display_order, title, rationale, body TEXT)` — 챕터 row 하나에 subtree(1-1~1-N + 리프 본문) ASCII 통짜.
- Option B의 ASCII 편집·diff 강점 body 필드 안에서 계승.
- 챕터 단위 조작(재생성·추가·삭제·순서변경) 자연스러움.
- 섹션 단위 재생성은 v1 미포함 (body TEXT 직접 편집으로 커버), v2에서 서버 파싱 기반 검토.
- 이슈 #15/#16 참조. Selection도 동일 형태.

### 갈림길 C. Deck 폐기 방식

**Option A — 즉시 물리 DROP**
- 거부 이유: 이력·로그·metrics label 등 잔존 참조가 소리 없이 실패할 위험. 롤백 여지 소멸.

**Option B (선택) — soft-deprecate(`_archived_deck` RENAME) 후 다음 릴리스 DROP**
- 비용: 두 릴리스에 걸쳐 마이그레이션 필요. 임시 아카이브 테이블 존재.
- 보상: 잔존 참조 발견 시 되돌리기 가능. 이력 보존.

**Option C — Deck 를 Axis 로 rename (view/alias)**
- 거부 이유: 두 이름이 공존하면 리팩토링 시 매번 매핑 확인. 사용자 표기는 이미 축.

### 갈림길 D. Review 스코프 표현

**Option A — 다형 세션 클래스(`AxisReviewSession`, `LayerReviewSession`)**
- 장점: Type-safe. 스코프별 로직 완전 분리.
- 거부 이유: Repository 이중화, 슬라이스 테스트 매트릭스 폭발, 공유 로직 상속 트리 부담.

**Option B (선택) — 단일 `ReviewSession` + `ReviewScope` enum + `scopeId`**
- 비용: `scopeId` 의미가 scope 에 따라 다름(axisId or layerId) — 코드 문서화 부담.
- 보상: 단일 Repository, 단일 슬라이스 테스트, Query 는 `WHERE scope = ? AND scope_id = ?` 로 단순.

**Option C — Layer 스코프 세션을 axis 세션 N개로 자동 분해**
- 거부 이유: 사용자에게 "Layer 리뷰 1건 시작" 이 "Axis 리뷰 N건 시작" 으로 노출됨. 진행률·중단·재개 UX 가 축 단위로 파편화.

### 갈림길 E. Card→Axis 매핑 마이그레이션 순서

**Option A — `card.axis_id` 추가 + `card.topic_id` 즉시 삭제**
- 거부 이유: 백필 실패 시 카드 참조 소실 위험. Rollback 여지 없음.

**Option B (선택) — 3단계 마이그레이션 (nullable → 백필 → NOT NULL)**
- 비용: 세 릴리스 걸쳐 진행. 중간 상태에서 `axis_id IS NULL` 카드 처리 정책 필요.
- 보상: 안전. 백필 검증 후 NOT NULL 승격. `topic_id` 는 별도 릴리스에서 DROP.

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 배치

```
┌─ LearningFacade BC ─────────────────────────────────────────┐
│                                                              │
│  LearningFacade (Aggregate Root)                             │
│   ├ concepts: List<LearningFacadeConcept> (1~5)              │
│   ├ layers: List<Layer>                                      │
│   └ audit + softDelete                                       │
│                                                              │
│  Layer (Aggregate/Entity)                                    │
│   ├ id, name, displayOrder, deleted_at                       │
│   ├ axes: List<LearningAxis>                                 │
│   └ progressStatus (derived)                                 │
│                                                              │
│  LearningAxis (Aggregate/Entity)                             │
│   ├ id, layer_id, name, displayOrder, deleted_at             │
│   ├ absorbed Deck fields:                                    │
│   │    progressStatus / mode / lastAccessed                  │
│   │    learningMaterialId / onLibrary / publishedAt          │
│   ├ roadmap: AxisRoadmap? (1)                                │
│   └ selections: List<AxisSelection> (N)                      │
│                                                              │
│  AxisRoadmap (Aggregate)                                     │
│   ├ id, axis_id (UNIQUE), content (ASCII TEXT)               │
│   └ audit                                                    │
│                                                              │
│  AxisSelection (Entity)                                      │
│   ├ id, axis_id, name (UNIQUE per axis), content             │
│   └ audit                                                    │
└──────────────────────────────────────────────────────────────┘

┌─ Card BC ───────────────────────────────────────────────────┐
│  Card (Aggregate)                                            │
│   ├ axis_id (NOT NULL after Epic 4)                          │
│   ├ topic_id (soft-deprecated, DROP planned next release)    │
│   ├ deck_id (removed after Epic 5 - deleted column)          │
│   └ recordView() emits event with axisId                     │
└──────────────────────────────────────────────────────────────┘

┌─ Review BC ─────────────────────────────────────────────────┐
│  ReviewSession (Aggregate)                                   │
│   ├ scope: ReviewScope { AXIS, LAYER }                       │
│   ├ scopeId: Long (axisId or layerId)                        │
│   ├ cards: List<CardReview>                                  │
│   └ findAllByAxisId / findAllByLayerId                       │
└──────────────────────────────────────────────────────────────┘

┌─ Deck BC (POLARIZED - to be removed) ───────────────────────┐
│  Epic 5 완료 시점에 BC 전체 삭제.                             │
│  이력: ADR020 (read-model 노출), ADR021 (자동 생성 유일화)     │
└──────────────────────────────────────────────────────────────┘
```

### 핵심 플로우

**1. LearningFacade 생성 → default Layer 자동 생성**
```
FE ─POST /learning-facade { concepts[] }─▶ Controller
                                            └ Service.create()
                                                ├ facade = LearningFacade.of(userId, concepts)
                                                ├ facade.addLayer("Uncategorized")
                                                └ facadeRepository.save(facade)
FE ◀ 201 { facadeId, concepts, layers: [{id, name:"Uncategorized"}] }
```

**2. Axis 추가 → default 로 첫 Layer 에 소속**
```
FE ─POST /layers/{layerId}/axes { name }─▶ Controller
                                            └ Service.addAxis()
                                                ├ layer.addAxis(name) → displayOrder auto
                                                └ save + publish LearningAxisCreatedEvent
FE ◀ 201 { axisId, layerId, name, displayOrder }
```

**3. Roadmap 설정 (수동 또는 axisConsensus 반영)**
```
FE ─PUT /axes/{axisId}/roadmap { content:"..." }─▶ Controller
                                                    └ Service.updateRoadmap()
                                                        └ axis.setRoadmap(content)
FE ◀ 200 { roadmapId, content }
```

**4. Selection 추가 (name UNIQUE per axis)**
```
FE ─POST /axes/{axisId}/selections { name, content }─▶ Controller
                                                        └ Service.addSelection()
                                                            └ axis.addSelection(name, content)
                                                              (validates UNIQUE)
FE ◀ 201 { selectionId, axisId, name, content }
```

**5. Card 생성 → axis_id 직접**
```
FE ─POST /axes/{axisId}/cards { summary, keywords[] }─▶ Controller
                                                        └ Service.createCard()
                                                            └ Card.create(userId, axisId, ...)
FE ◀ 201 { cardId, axisId, summary, keywords }
```

**6. Layer 스코프 Review 세션 시작**
```
FE ─POST /layers/{layerId}/review-sessions─▶ Controller
                                              └ Service.startLayerReview()
                                                  ├ layer.axes → axisIds
                                                  ├ Card.findByAxisIds(axisIds)
                                                  └ ReviewSession.of(LAYER, layerId, cards)
FE ◀ 201 { sessionId, scope:"LAYER", scopeId:layerId, cards:[...] }
```

### Out-of-Process 의존

- **MySQL** — 전 도메인 저장. Flyway V16~V22 (예정) 로 신규 스키마 도입.
- **product-ai-suggestion** — Layer/Axis/Roadmap/Selection 어댑터 4-Port 호출자. 본 Product 완료 후 진입.
- **product-ai-interactive-roadmap** — `axisDraft → axisConsensus` 흐름 저장 시 `LearningFacade.addAxis` / `Layer.addAxis` / `Axis.setRoadmap` 호출.
- **외부 시스템 없음** — 순수 도메인 BC.

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | ErrorCode | HTTP | 클라이언트 권장 동작 |
| --- | --- | --- | --- |
| `concepts` 배열 크기 0 또는 5 초과 | `LEARNING_FACADE_CONCEPTS_SIZE_INVALID` | 400 | 입력 재확인 |
| 같은 `(facade_id, value)` concept 중복 | `LEARNING_FACADE_CONCEPT_DUPLICATE` | 409 | 이미 등록됨 안내 |
| Layer 소속되지 않은 Axis 생성 시도 | `LEARNING_AXIS_LAYER_REQUIRED` | 400 | Layer 선택 UI 유도 |
| `layer_id` 존재하지 않음 | `LAYER_NOT_FOUND` | 404 | 새로고침 |
| Roadmap 없는 축에 Review 시작 시도 | `AXIS_ROADMAP_NOT_FOUND` | 404 | Roadmap 생성 유도 |
| Selection name 중복 (같은 axis 내) | `AXIS_SELECTION_NAME_DUPLICATE` | 409 | 이름 변경 유도 |
| Card 생성 시 `axis_id NULL` | `CARD_AXIS_REQUIRED` | 400 | axis 선택 필수 |
| 기존 topic_id 참조 카드 조회 | `LEGACY_TOPIC_REFERENCE` | 410 Gone (마이그레이션 완료 후) | 새 axis 기반 URL 이동 |
| 폐기된 `/decks/*` 엔드포인트 호출 | (없음, 라우팅 삭제) | 404 | axis 엔드포인트 안내 |
| `ReviewScope.LAYER` 세션 시작 시 소속 axis 0건 | `LAYER_HAS_NO_AXES` | 400 | Layer 에 axis 추가 유도 |
| Layer softDelete 시 하위 axis 존재 | `LAYER_HAS_ACTIVE_AXES` | 409 | 축을 먼저 이동/삭제 유도 |

### 로깅 정책

- **항상 기록**:
  - Layer 생성·삭제 (`layer_id`, `facade_id`, `user_id`)
  - Roadmap 설정·수정 (`roadmap_id`, `axis_id`, `content_length`, `changed_by=user|ai`)
  - Selection 추가·삭제 (`selection_id`, `axis_id`, `name`)
  - Card 카드 축 매핑 마이그레이션 (`card_id`, `from_topic_id`, `to_axis_id`)
  - Review 세션 시작 (`session_id`, `scope`, `scope_id`, `card_count`)
- **debug**: displayOrder 재부여, softDelete 연쇄 처리 개수
- **절대 금지**: Roadmap/Selection content 본문 로그 (사용자 학습 내용)

### 관측 지표 (Product-op Product 협력)

- `thirdtool.learning.layer_count{facade_id}` — Layer 개수 (facade_id 는 bucket 처리, product-op §카디널리티 참조).
- `thirdtool.learning.axis_per_layer_p95` — Layer 당 axis 개수 P95.
- `thirdtool.learning.axis_roadmap_coverage_ratio` — Roadmap 이 설정된 axis 비율.
- `thirdtool.review.session_started_total{scope}` — scope=AXIS|LAYER 별 세션 시작 카운터.
- `thirdtool.migration.card_axis_backfill_total{state}` — Epic 4 마이그레이션 진행 관측 (state=pending|migrated|failed).

## 롤아웃 / 마이그레이션 (Rollout)

### 전제

- 사용자 0명. Breaking migration 허용.
- Epic 1~6 은 순차 진행. Epic 3 완료 전까지 Roadmap/Selection 은 admin API 로만 제어(사용자 노출 안 함).
- 각 Epic 별 Flyway 마이그레이션 스크립트 + Rollback 스크립트 동반.

### Product 의존성

- **선행 Product**:
  - `Product.md` (User BC) — 무영향, 병렬 진행 가능.
  - `product-log.md` / `product-op.md` — `axis_id`·`layer_id`·`review_session_scope` 관측 필드 추가 필요 (Task 8, 최소 패치).
- **후행 Product**:
  - `product-ai-suggestion.md` (재작성) — 본 Product Epic 1~4 완료 후 진입. Layer/Axis/Roadmap/Selection Adapter 구현.
  - `product-ai-interactive-roadmap.md` (재작성) — 본 Product 전체 완료 + ai-suggestion 완료 후 진입.

### Epic·Story 의존성 그래프

```
Epic 1: concepts[] (#04)
  │
  ▼
Epic 2: Layer 도메인 (#05)
  │
  ▼
Epic 3: Roadmap/Selection dual-axis (#06 + #11)
  │
  ▼
Epic 4: Card→Axis 직접 매핑 (#07)
  │
  ▼
Epic 5: Deck 폐기 + Axis 흡수 (#13)
  │
  ▼
Epic 6: Review 이중 스코프 (#14)
```

Epic 순서는 데이터 마이그레이션 의존성 순 — Layer 없이 Roadmap/Selection 만 도입은 불가(축 그룹핑 컨텍스트 부재), Card→Axis 없이 Deck 폐기는 잔존 참조 처리 불가.

### 환경별 설정 분기

| 항목 | dev | prod |
| --- | --- | --- |
| Flyway V16~V22 (신규) | 배포 시 자동 적용 | 배포 시 자동 적용 |
| Legacy `axis_topic` / `deck` 테이블 | Epic 3/5 완료 시 RENAME → `_archived_*` | 동일 |
| `topic_id` / `deck_id` NOT NULL 제거 시점 | Epic 4/5 종료 | Epic 4/5 종료 |
| `/decks/*` 엔드포인트 | Epic 5 완료 시 404 | 동일 |

## 성공 지표 (KPI)

| 지표 | 목표 값 | 측정 방법 |
| --- | --- | --- |
| `LearningFacade.concepts.size()` 분포 | 1~5 (모든 신규 facade) | Repository 슬라이스 · 통합 테스트 |
| Layer 잔존 참조 (기본 "Uncategorized" 외) | 사용자 지정 Layer ≥ 1 (Epic 2 완료 후 6개월) | Product-op 대시보드 |
| `AxisTopic` 신규 참조 | = 0 (Epic 3 완료 이후) | `grep -r "AxisTopic\|axis_topic" src/main/java/com/example/thirdtool/ --include="*.java"` |
| `card.topic_id` NULL 비율 | = 100% (Epic 4 마이그레이션 완료) | `SELECT COUNT(*) FROM card WHERE topic_id IS NOT NULL AND deleted_at IS NULL` |
| Deck 도메인 참조 | = 0 (Epic 5 완료 이후) | 정적 분석 + 소스 grep |
| `/decks/*` 엔드포인트 접근 | 404 (Epic 5 완료 이후) | 통합 테스트 |
| `ReviewSession.scope` 분포 | AXIS · LAYER 양쪽 사용 관측 | Product-op 대시보드 |
| `Layer.progressStatus` 정확도 | 하위 axis statuses 로부터 정확히 파생 | 통합 테스트 |
| Roadmap 설정 축 비율 (선택 KPI) | ≥ 60% (Epic 3 완료 3개월 후) | 통계 쿼리 |

## Scope

**In Scope**:
- `LearningFacade.concepts[]` (1~5) 정규화 도입 (#04)
- `Layer` 도메인 신설, Axis 상위 재배선 (#05)
- `AxisRoadmap` (1개/축) + `AxisSelection[]` (N개/축) 이중 축 도입 (#06)
- `AxisSelection` 정책 (name UNIQUE, created_at DESC, in-place update, hard delete) (#11)
- `Card.axis_id NOT NULL` 직접 매핑, `topic_id` soft-deprecate (#07)
- Deck BC 폐기 + 8 책임 Axis 흡수, `/decks/*` → `/axes/*` 통합 (#13)
- `ReviewSession` `AXIS` / `LAYER` 이중 스코프, `Layer.progressStatus` 파생 (#14)
- 관련 Flyway V16~V22 (예정 번호) + Rollback
- ADR022 용어 준수 (Layer, AxisRoadmap, AxisSelection, ReviewScope)

**Out of Scope**:
- AI 로 Layer/Roadmap/Selection 을 자동 생성 — `product-ai-suggestion.md` 로 분리
- `RoadmapInteractionSession` 상태기계 — `product-ai-interactive-roadmap.md` 로 분리
- `AxisTopic` / `deck` 테이블 물리 DROP — 다음 릴리스(0.1.0v 예정)
- UI 문구 매핑 (`Layer 1` 대체 명명 등) — `docs/ux/wip-language.md` 갱신은 별도
- Layer 간 축 이동 (`Axis.changeLayer`) — 후속 UX 검토

## 대상 사용자

- **학습자** — 여러 관점(concepts) 을 한 지도에 담고, Layer 로 그룹핑해 스코프 리뷰 가능.
- **강사/코치** — Layer 별 진행률 관측, Roadmap/Selection 로 축의 헌법과 사례 명시.
- **백엔드 엔지니어** — BC 수 축소(Deck 폐기), Topic 계층 폐기로 join 단순화, ReviewSession 단일화.
- **AI 어댑터 저자 (`product-ai-suggestion.md`)** — 명확한 Layer/Axis/Roadmap/Selection 인터페이스 제공.

## 연결된 Epic 목록

- [ ] Epic 1: LearningFacade.concepts[] 도입 (#04)
- [ ] Epic 2: Layer 도메인 승격 (#05)
- [ ] Epic 3: Roadmap/Selection Dual-Axis + Selection 정책 (#06 + #11)
- [ ] Epic 4: Card→Axis 직접 매핑 (#07)
- [ ] Epic 5: Deck BC 폐기 및 Axis 흡수 (#13)
- [ ] Epic 6: Review 이중 스코프 (Axis/Layer) (#14)

## 관련 문서

- **선행 fix 이슈**:
  - `workflow/task/fix/brainstorming/version/0.0.2v/issue-04-concept-list.md` — concepts[]
  - `workflow/task/fix/brainstorming/version/0.0.2v/issue-05-layer-server-domain.md` — Layer 도메인
  - `workflow/task/fix/brainstorming/version/0.0.2v/issue-06-roadmap-selections-dualaxis.md` — Roadmap/Selection
  - `workflow/task/fix/brainstorming/version/0.0.2v/issue-07-card-axis-direct-mapping.md` — Card→Axis
  - `workflow/task/fix/brainstorming/version/0.0.2v/issue-11-selections-version-policy.md` — Selection 정책
  - `workflow/task/fix/brainstorming/version/0.0.2v/issue-13-deck-abolition-axis-absorption.md` — Deck 폐기
  - `workflow/task/fix/brainstorming/version/0.0.2v/issue-14-review-strategy-axis-layer-scope.md` — Review 이중 스코프
- **관련 ADR**: [ADR003](../../../../../docs/adr/ADR003.md) (Soft Delete), [ADR004](../../../../../docs/adr/ADR004.md) (주제 명사구), [ADR007](../../../../../docs/adr/ADR007.md) (동기 도메인 이벤트), [ADR020](../../../../../docs/adr/ADR020-deck-axis-visibility.md), [ADR021](../../../../../docs/adr/ADR021-axis-deck-full-integration.md), [**ADR022**](../../../../../docs/adr/ADR022-learning-tower-terminology.md) (본 Product 용어 표준)
- **후행 Product**: `product-ai-suggestion.md` (재작성), `product-ai-interactive-roadmap.md` (재작성)
- **DOMAIN.md / PACKAGE.md 갱신 예정**:
  - `docs/DOMAIN.md` §2 학습 도메인 — Layer / Roadmap / Selection 절 신설, Deck 절 삭제, Topic 절 삭제
  - `docs/PACKAGE.md` §BC 구성 — `Deck/` 삭제, `Layer/` 신설(또는 `LearningFacade/domain/model/` 하위)
- **폐기 문서**:
  - `workflow/task/pes/workspectrum/sdd/in-progress/product-deck.md` — 본 Product Epic 5 로 흡수 후 삭제
  - `workflow/task/pes/workspectrum/sdd/in-progress/product-aisuggestion.md` — `product-ai-suggestion.md` 로 재작성 후 삭제

## 열린 질문 (Open Questions)

1. **Layer 개수 상한** — LearningFacade 당 몇 개까지 허용? 권장 5개? (`RECOMMENDED_LAYER_COUNT_LIMIT`)
2. **Roadmap content 최대 길이** — `VARCHAR(2000)` 로 시작하되 `TEXT` 승격 필요 시점?
3. **Selection content 최대 길이** — Roadmap 과 동일 정책? 아니면 별도?
4. **Layer softDelete 시 하위 axis 처리** — Layer softDelete → 하위 axis 도 자동 softDelete? 아니면 이동 강제? (Story 2-5 결정 대상)
5. **`Layer 1` UI 라벨 대체 명명** — `Layer{id}` vs `FacadeWide` vs 사용자 지정? `docs/ux/wip-language.md` 후속.
6. **Card 이동 UX** — Card 를 다른 Axis 로 이동하는 사용자 액션 필요 여부. 현재 Scope 밖.
7. **Layer 간 Axis 이동** — Axis 를 다른 Layer 로 이동. 현재 Scope 밖.
8. **`AxisTopic` 물리 DROP 시점** — 다음 릴리스(0.1.0v) 확정 vs 관찰 후 결정.
9. **Legacy `deck` 테이블 물리 DROP 시점** — Epic 5 완료 후 1개월 관찰 후 DROP.
10. **`Layer.progressStatus` 파생 로직 3-state 경계** — 하위 axis 상태 → Layer 상태 매핑 규칙 확정 (Story 6-4).

## 제품 수준 완료 기준 (Product-level DoD)

- [ ] Epic 1~6 모두 DoD 통과
- [ ] ADR022 발행 완료 (선행 완료 · 2026-07-01)
- [ ] `docs/DOMAIN.md` §학습 도메인 갱신 반영
- [ ] `docs/PACKAGE.md` §BC 구성 Deck 삭제·Layer 신설 반영
- [ ] 신규 Flyway V16~V22 + Rollback 모두 통과
- [ ] `product-deck.md` 삭제 (Epic 5 완료 시)
- [ ] product-op 대시보드에 신규 지표 반영 (Task 8 협력)
- [ ] product-log 로그 필드 반영 (Task 8 협력)
- [ ] 성공 지표 각 지표 초기 관측값 확보

---

# [Epic 1] LearningFacade.concepts[] 도입

## 목표

`LearningFacade.concept: String` 단수 필드를 `concepts: List<String>` (1~5) 로 승격해 다중 관점 표현을 지원하고, 후속 AI Layer 추천의 입력 자산을 확보한다.

## 배경

`concept` 단일 문자열로는 "백엔드 + 시스템 설계 + 도메인 언어" 처럼 여러 관점을 함께 표현할 수 없어 AI 가 Layer/Axis 를 추천할 근거가 얕음. 자식 테이블 정규화로 개별 concept 값 검색·중복 방지·display_order 지원.

## 포함 Story

- Story 1-1: `learning_facade_concept` 자식 테이블 신설 + Flyway V16
- Story 1-2: `LearningFacadeConcept` 자식 Entity + `LearningFacade.concepts()` 컬렉션 API
- Story 1-3: 기존 단수 `concept` 값 backfill (V16 백필 스크립트 + 도메인 API `updateConcepts`)
- Story 1-4: API/DTO 응답 `concepts: string[]` 로 스위치 (Request/Response DTO)
- Story 1-5: 도메인 상수·검증(1~5, trim, blank 거부, 중복 거부) + ErrorCode 등록

## Epic 인수 시나리오

- Given 신규 사용자 / When `POST /learning-facade { concepts:["백엔드","시스템설계"] }` / Then facade + 2건의 `learning_facade_concept` row 생성
- Given 기존 `concept="백엔드"` facade / When V16 마이그레이션 실행 / Then `learning_facade_concept` row 1건 생성 + `learning_facade.concept` 컬럼 유지(soft-deprecate)
- *(엣지)* Given `concepts: []` / When 요청 / Then `LEARNING_FACADE_CONCEPTS_SIZE_INVALID` 400
- *(엣지)* Given `concepts: ["백엔드","백엔드"]` / When 요청 / Then `LEARNING_FACADE_CONCEPT_DUPLICATE` 409
- *(엣지)* Given `concepts: [" 백엔드 ", "백엔드"]` / When trim 후 비교 / Then 동일값으로 판정, 예외

## Epic 완료 기준 (DoD)

- [ ] 5개 Story 모두 완료
- [ ] Flyway V16 + Rollback R16
- [ ] 단위 · Repository Slice · Controller Slice 테스트 통과
- [ ] ErrorCode 신규 등록: `LEARNING_FACADE_CONCEPTS_SIZE_INVALID`, `LEARNING_FACADE_CONCEPT_DUPLICATE`, `LEARNING_FACADE_CONCEPT_BLANK`
- [ ] `docs/DOMAIN.md` §LearningFacade 갱신 (concept → concepts[])
- [ ] ADR022 용어 준수 검증 (`grep "concept "` 잔존 문서 리네이밍)

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **자식 테이블 vs JSON 컬럼** — 자식 테이블 선택. 이유: UNIQUE `(facade_id, value)` 강제, 개별 검색, 표준 관행 준수.
- **단수 `concept` 컬럼 즉시 삭제 vs soft-deprecate** — soft-deprecate. 이유: 롤백 여지 확보. 다음 릴리스에서 DROP.

---

## [Story 1-1] `learning_facade_concept` 테이블 신설 + Flyway V16

### User Story

- As a 백엔드 엔지니어
- I want LearningFacade.concept 단수 필드를 자식 테이블로 정규화하기를
- so that 다중 concept 를 표현·검색·중복 방지할 수 있다

### 설명

> 출처: `issue-04-concept-list.md`

Flyway `V16__learning_facade_concept.sql` 신설. 컬럼: `id BIGINT PK`, `facade_id BIGINT FK NOT NULL`, `value VARCHAR(100) NOT NULL`, `display_order INT NOT NULL DEFAULT 0 CHECK (>=0)`, `created_at DATETIME(6) NOT NULL`. UNIQUE `(facade_id, value)`. FK `ON DELETE CASCADE`. 기존 `learning_facade.concept` 컬럼은 soft-deprecate (그대로 유지, `@Deprecated` 도메인 필드).

**핵심 파일**:
- 신규: `src/main/resources/db/migration/V16__learning_facade_concept.sql`
- 신규: `src/main/resources/db/migration/R16__rollback_learning_facade_concept.sql`

### 완료 기준 (AC)

- Given V16 미적용 / When `./gradlew bootRun` / Then V16 자동 적용, 테이블 존재
- Given V16 적용 상태 / When `learning_facade_concept` 에 같은 `(facade_id, value)` 두 번 insert / Then UNIQUE 위반
- Given `deleted_at != NULL` 인 facade / When 조회 / Then `learning_facade_concept` row 는 유지 (부모 softDelete 관행)
- *(엣지)* Given `display_order = -1` insert 시도 / When 실행 / Then CHECK 위반

### Definition of Done

- [ ] Flyway V16 + R16
- [ ] Repository Slice: UNIQUE / CHECK 위반 검증
- [ ] 통합 테스트: 마이그레이션 후 스키마 검증

### 스토리 포인트

1d

### 의존성

- 선행: 없음
- 후행: Story 1-2 (Entity 매핑 진행)

---

## [Story 1-2] `LearningFacadeConcept` Entity + `LearningFacade.concepts()` 컬렉션 API

### User Story

- As a 백엔드 엔지니어
- I want LearningFacade 가 concepts 자식 컬렉션을 캡슐화된 방식으로 노출하기를
- so that Application Service 가 컬렉션을 직접 조작하지 않고 도메인 API 로만 접근한다

### 설명

> 출처: `issue-04-concept-list.md`

`LearningFacadeConcept` Entity 신설. `LearningFacade.concepts` 필드는 `@OneToMany(orphanRemoval=true)`. 노출은 `Collections.unmodifiableList()`. 신규/수정 API: `addConcept(value: String)`, `removeConcept(value: String)`, `updateConcepts(values: List<String>)`. 정적 팩토리 원칙(new 금지) 준수.

**핵심 파일**:
- 신규: `src/main/java/com/example/thirdtool/LearningFacade/domain/model/LearningFacadeConcept.java`
- 수정: `src/main/java/com/example/thirdtool/LearningFacade/domain/model/LearningFacade.java`

**주요 메서드**:
- `LearningFacade.addConcept(String value)` — trim, blank 거부, size≤5 검증, 중복 거부
- `LearningFacade.updateConcepts(List<String> values)` — 전체 교체 (1~5 검증)
- `LearningFacade.getConcepts(): List<LearningFacadeConcept>` — unmodifiableList

### 완료 기준 (AC)

- Given empty facade / When `facade.addConcept("백엔드")` / Then concepts.size()=1, displayOrder=1
- Given facade with 5 concepts / When `addConcept("추가")` / Then `LEARNING_FACADE_CONCEPTS_SIZE_INVALID`
- Given `addConcept("  백엔드  ")` on facade with "백엔드" / When 실행 / Then `LEARNING_FACADE_CONCEPT_DUPLICATE` (trim 후 비교)
- Given `addConcept("")` / When 실행 / Then `LEARNING_FACADE_CONCEPT_BLANK`
- Given `updateConcepts(["A","B","C"])` on facade with [X,Y] / When 실행 / Then 기존 삭제 · 신규 3건 생성, displayOrder 1~3
- *(엣지 - unmodifiable)* Given `facade.getConcepts()` / When list.add() 시도 / Then `UnsupportedOperationException`

### Definition of Done

- [ ] 구현 완료
- [ ] 단위 테스트 (해피 · 엣지 · 예외 각 케이스, 총 8건+)
- [ ] ErrorCode 등록: `LEARNING_FACADE_CONCEPTS_SIZE_INVALID`, `LEARNING_FACADE_CONCEPT_DUPLICATE`, `LEARNING_FACADE_CONCEPT_BLANK`
- [ ] `docs/DOMAIN.md` §LearningFacade 갱신

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 1-1 (테이블 필요)
- 후행: Story 1-3 (backfill)

---

## [Story 1-3] 단수 `concept` 값 backfill + `updateConcepts` 도메인 API

### User Story

- As a 시스템 운영자
- I want 기존 facade 의 단수 concept 값이 자동으로 자식 테이블에 이전되기를
- so that 마이그레이션 후 UI 가 concepts 배열 그대로 표시 가능하다

### 설명

> 출처: `issue-04-concept-list.md`

V16 마이그레이션에 backfill INSERT 포함:
```sql
INSERT INTO learning_facade_concept (facade_id, value, display_order, created_at)
SELECT id, concept, 1, NOW(6)
  FROM learning_facade
 WHERE concept IS NOT NULL AND TRIM(concept) <> '';
```
기존 `learning_facade.concept` 컬럼은 유지 (다음 릴리스에서 DROP). 도메인은 자식 테이블만 참조.

**핵심 파일**:
- 수정: `V16__learning_facade_concept.sql` (backfill 절 추가)

### 완료 기준 (AC)

- Given `concept="백엔드"` facade 1건 / When V16 실행 / Then `learning_facade_concept` row 1건 (value="백엔드", displayOrder=1)
- Given `concept=NULL` facade / When V16 실행 / Then `learning_facade_concept` row 미생성
- Given `concept="   "` (whitespace only) facade / When V16 실행 / Then `learning_facade_concept` row 미생성 (`TRIM(...) <> ''` 필터)

### Definition of Done

- [ ] V16 backfill INSERT 추가
- [ ] 통합 테스트: backfill 결과 검증

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 1-1 (테이블), Story 1-2 (Entity)
- 후행: Story 1-4 (API)

---

## [Story 1-4] API/DTO 응답 `concepts: string[]` 스위치

### User Story

- As a FE 개발자
- I want 응답에서 concepts 를 배열로 받기를
- so that 다중 concept UI 가 즉시 구현 가능하다

### 설명

> 출처: `issue-04-concept-list.md`

`LearningFacadeRequest.Create` / `Update` 에 `concepts: List<String>` 필드. `LearningFacadeResponse.Detail` / `Summary` 에 `concepts: string[]` 응답. 기존 `concept: String` 응답 필드는 v1 호환용으로 제거.

**핵심 파일**:
- 수정: `LearningFacade/presentation/dto/LearningFacadeRequest.java`
- 수정: `LearningFacade/presentation/dto/LearningFacadeResponse.java`
- 수정: `LearningFacade/presentation/LearningFacadeController.java`
- 수정: `LearningFacade/application/service/LearningFacadeCommandService.java`

### 완료 기준 (AC)

- Given `POST /learning-facade { concepts:["백엔드","시스템설계"] }` / When 요청 / Then 201 + 응답에 `concepts:["백엔드","시스템설계"]`
- Given `PATCH /learning-facade/{id}/concepts { concepts:["A","B"] }` / When 요청 / Then 200 + `concepts:["A","B"]`
- Given `POST { concepts:[] }` / When 요청 / Then 400 `LEARNING_FACADE_CONCEPTS_SIZE_INVALID`

### Definition of Done

- [ ] Controller Slice 테스트 (해피 · 예외 케이스)
- [ ] 통합 테스트: 전체 흐름
- [ ] Swagger UI 응답 스키마 갱신

### 스토리 포인트

1d

### 의존성

- 선행: Story 1-2, 1-3
- 후행: 없음

---

## [Story 1-5] 도메인 상수 · 검증 · ErrorCode 통합

### User Story

- As a 도메인 코드 작성자
- I want concepts 관련 규칙을 도메인 상수로 응집하기를
- so that Application Service / Controller / FE 어디도 상수를 재정의하지 않는다

### 설명

> 출처: `issue-04-concept-list.md`

`LearningFacade` 클래스에 상수:
- `MIN_CONCEPT_COUNT = 1`
- `MAX_CONCEPT_COUNT = 5`
- `MAX_CONCEPT_VALUE_LENGTH = 100`

ErrorCode enum 갱신:
- `LEARNING_FACADE_CONCEPTS_SIZE_INVALID` — "컨셉은 1~5개 이내여야 합니다." · 400
- `LEARNING_FACADE_CONCEPT_DUPLICATE` — "이미 등록된 컨셉입니다." · 409
- `LEARNING_FACADE_CONCEPT_BLANK` — "컨셉 값은 비어있을 수 없습니다." · 400
- `LEARNING_FACADE_CONCEPT_TOO_LONG` — "컨셉 값은 100자 이내여야 합니다." · 400

### 완료 기준 (AC)

- Given ErrorCode enum / When 조회 / Then 4개 코드 등록됨
- Given `addConcept("a".repeat(101))` / When 실행 / Then `LEARNING_FACADE_CONCEPT_TOO_LONG`
- Given 도메인 상수 참조 / When Application Service 코드 정적 분석 / Then 재정의 0건

### Definition of Done

- [ ] ErrorCode 4개 등록
- [ ] 도메인 상수 3개 정의
- [ ] Controller Slice 테스트: 4개 ErrorCode 응답 형식 검증

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 1-2
- 후행: 없음

---

# [Epic 2] Layer 도메인 승격

## 목표

Axis 상위 그룹핑 계층으로 `Layer` Aggregate 를 신설해 축 그룹핑·Layer 스코프 리뷰·진행률 파생을 지지하는 지지대를 확보한다.

## 배경

Axis 는 학습 단위지만 여러 축을 개념적으로 묶어 학습하려는 요구(예: "백엔드 계열 축 3개") 를 표현할 방법이 없음. Layer 승격은 후속 Layer 스코프 리뷰(#14) · Layer 별 진행률 · AI Layer 추천의 자연스러운 전제.

## 포함 Story

- Story 2-1: `learning_layer` 테이블 신설 + `Layer` Aggregate (Flyway V17)
- Story 2-2: `Axis.facade_id` → `Axis.layer_id` FK 재배선 (V18)
- Story 2-3: default `Uncategorized` Layer 자동 생성 + 기존 axis 백필 (V18)
- Story 2-4: `Layer` softDelete 지원 (ADR003, ADR021 관행)
- Story 2-5: Layer 최대 개수 · displayOrder · softDelete 시 axis 처리 정책

## Epic 인수 시나리오

- Given 신규 LearningFacade / When 생성 / Then `Uncategorized` Layer 자동 1건 생성
- Given axis 3건 있는 facade / When Story 2-3 백필 실행 / Then 3건 모두 `Uncategorized` Layer 로 이관
- Given `POST /layers { name:"백엔드" }` / When 요청 / Then Layer 신규 생성, displayOrder auto
- Given `PATCH /axes/{axisId}/layer { layerId: X }` / When 요청 / Then axis.layer_id 변경 (열린 질문 6 확정 시 도입, v1 은 Out of Scope)

## Epic 완료 기준 (DoD)

- [ ] Story 2-1~2-5 완료
- [ ] Flyway V17 + V18 + Rollback
- [ ] ErrorCode 등록: `LAYER_NOT_FOUND`, `LAYER_NAME_DUPLICATE`, `LAYER_HAS_ACTIVE_AXES`, `LAYER_ALREADY_DELETED`
- [ ] `docs/DOMAIN.md` §LearningFacade 하위 계층 갱신
- [ ] `docs/PACKAGE.md` — Layer 파일 배치 결정 (LearningFacade/domain/model/ 하위 vs 별도 BC)

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **Layer 위치 — LearningFacade BC 안 vs 별도 BC**: LearningFacade BC 안(`LearningFacade/domain/model/Layer.java`) 배치. 이유: Layer 는 LearningFacade 의 자식이며 별도 BC 로 승격할 응집도 부족.
- **default Layer 자동 생성 시점 — LearningFacade.create() vs migration**: LearningFacade.create() 에 통합. 마이그레이션은 backfill 만.
- **Layer softDelete 시 하위 axis 처리 — 자동 연쇄 vs 이동 강제**: v1 은 이동 강제(`LAYER_HAS_ACTIVE_AXES` 409). 이유: 자동 연쇄는 대량 데이터 소실 위험.

---

## [Story 2-1] `learning_layer` 테이블 + `Layer` Aggregate

### User Story

- As a 백엔드 엔지니어
- I want Layer 를 정식 Aggregate 로 정의하기를
- so that 축 그룹핑 · Layer 스코프 리뷰 · 진행률 파생의 지지대를 확보한다

### 설명

> 출처: `issue-05-layer-server-domain.md`

`learning_layer` 테이블: `id BIGINT PK`, `facade_id BIGINT FK NOT NULL`, `name VARCHAR(100) NOT NULL`, `display_order INT NOT NULL DEFAULT 0 CHECK (>=0)`, `deleted_at DATETIME(6) NULL`, `created_at`, `updated_at`. UNIQUE `(facade_id, name, deleted_at)` (ADR021 관행). Layer Aggregate 는 `@SQLRestriction("deleted_at IS NULL")` + `softDelete()`.

**핵심 파일**:
- 신규: `src/main/resources/db/migration/V17__learning_layer.sql`
- 신규: `src/main/resources/db/migration/R17__rollback_learning_layer.sql`
- 신규: `src/main/java/com/example/thirdtool/LearningFacade/domain/model/Layer.java`

**주요 메서드**:
- `Layer.of(name, facadeId): Layer` (정적 팩토리)
- `Layer.softDelete()`
- `Layer.addAxis(name): LearningAxis` (displayOrder auto)
- `Layer.reorderAxes(List<Long> orderedIds)`

### 완료 기준 (AC)

- Given V17 실행 / When 스키마 확인 / Then `learning_layer` 존재 + UNIQUE 제약
- Given `Layer.of("백엔드", facadeId)` / When 실행 / Then Layer 생성
- Given softDeleted layer / When `Layer.of("백엔드", facadeId)` 로 새 layer 생성 시도 / Then UNIQUE 통과 (deleted_at 로 구분)
- *(엣지 - 이름 blank)* Given `Layer.of("", facadeId)` / When / Then `LAYER_NAME_BLANK` 예외

### Definition of Done

- [ ] Flyway V17 + R17
- [ ] Layer 클래스 + 단위 테스트 (해피 · 엣지 · 예외)
- [ ] Repository Slice: UNIQUE 검증
- [ ] ErrorCode: `LAYER_NAME_BLANK`, `LAYER_NAME_DUPLICATE`, `LAYER_ALREADY_DELETED`

### 스토리 포인트

2d

### 의존성

- 선행: Epic 1 완료 (concepts[] 정착 후 Layer 신설이 자연스러움)
- 후행: Story 2-2

---

## [Story 2-2] `Axis.facade_id` → `Axis.layer_id` FK 재배선

### User Story

- As a 백엔드 엔지니어
- I want LearningAxis 가 Layer 를 직접 참조하기를
- so that Layer 스코프 조회가 join 1회로 처리된다

### 설명

> 출처: `issue-05-layer-server-domain.md`

Flyway V18:
1. `learning_axis` 에 `layer_id BIGINT NULL` 컬럼 추가
2. 백필 (Story 2-3 이 담당)
3. `layer_id NOT NULL` 승격 (V18 후반부)
4. `learning_axis.facade_id` 컬럼 soft-deprecate (컬럼 유지, FK 제거)
5. FK `fk_learning_axis_layer(layer_id) REFERENCES learning_layer(id) ON DELETE RESTRICT`

`LearningAxis` 도메인은 `layer: Layer` `@ManyToOne` (JPA 매핑). `LearningFacade` 는 `layers: List<Layer>` 를 소유, axis 는 layer 를 통해서만 조회.

**핵심 파일**:
- 신규: `src/main/resources/db/migration/V18__axis_layer_fk.sql`
- 신규: `src/main/resources/db/migration/R18__rollback_axis_layer_fk.sql`
- 수정: `LearningFacade/domain/model/LearningAxis.java`
- 수정: `LearningFacade/domain/model/LearningFacade.java` (axes 필드 제거, layers 만)

### 완료 기준 (AC)

- Given V18 실행 / When 스키마 확인 / Then `learning_axis.layer_id NOT NULL` + FK 존재
- Given `layer.getAxes()` / When 조회 / Then Layer 소속 axis 만 반환
- Given `LearningFacade.findAxis(axisId)` / When 조회 / Then layer 를 통해 axis 반환
- Given `facade.axes` 직접 접근 시도 / When 컴파일 / Then 컴파일 오류 (필드 제거)

### Definition of Done

- [ ] V18 + R18
- [ ] 도메인 재배선 완료
- [ ] Repository Slice: 소속 관계 검증
- [ ] 통합 테스트: Axis 생성·조회·삭제 시나리오

### 스토리 포인트

2.5d

### 의존성

- 선행: Story 2-1, 2-3 (백필 선행)
- 후행: Story 2-4

---

## [Story 2-3] default `Uncategorized` Layer 자동 생성 + 기존 axis 백필

### User Story

- As a 시스템 운영자
- I want 기존 axis 가 자동으로 default Layer 에 소속되기를
- so that 마이그레이션 후 UI 에 축이 사라지지 않는다

### 설명

> 출처: `issue-05-layer-server-domain.md`

Flyway V18 백필 절:
```sql
-- 1. 각 facade 에 default Layer 생성
INSERT INTO learning_layer (facade_id, name, display_order, created_at, updated_at)
SELECT id, 'Uncategorized', 1, NOW(6), NOW(6)
  FROM learning_facade
 WHERE deleted_at IS NULL;

-- 2. 각 axis 를 그 facade 의 default Layer 에 매핑
UPDATE learning_axis a
   JOIN learning_layer l ON l.facade_id = a.facade_id AND l.name = 'Uncategorized'
   SET a.layer_id = l.id
 WHERE a.deleted_at IS NULL AND a.layer_id IS NULL;
```

`LearningFacade.create()` 도 default Layer 자동 생성 로직 포함(신규 facade 도 백필 상태와 동일).

**핵심 파일**:
- 수정: `V18__axis_layer_fk.sql` (백필 절 추가)
- 수정: `LearningFacade/domain/model/LearningFacade.java` (`create()` 내부에서 `addLayer("Uncategorized")` 호출)

### 완료 기준 (AC)

- Given 3개 axis 소유 facade / When V18 실행 / Then 3 axis 모두 `Uncategorized` layer 로 layer_id 매핑
- Given 신규 `LearningFacade.create()` / When 실행 / Then default Layer 1개 자동 생성
- Given softDeleted facade / When V18 실행 / Then default Layer 미생성
- *(엣지)* Given 이미 백필된 axis (`layer_id != NULL`) / When V18 재실행 / Then no-op (WHERE `layer_id IS NULL`)

### Definition of Done

- [ ] V18 백필 절 완료
- [ ] LearningFacade.create() 수정
- [ ] 통합 테스트: 마이그레이션 결과 검증

### 스토리 포인트

1d

### 의존성

- 선행: Story 2-1
- 후행: Story 2-2

---

## [Story 2-4] `Layer` softDelete 지원

### User Story

- As a 학습 사용자
- I want Layer 삭제 시 데이터가 유지되고 조회에서만 필터되기를
- so that 복원 여지가 있고 잘못 삭제한 경우 되돌릴 수 있다

### 설명

> 출처: `issue-05-layer-server-domain.md`

Layer 를 사용자 자산성 도메인으로 분류 (ADR003 v2 갱신). `@SQLRestriction("deleted_at IS NULL")` + `softDelete()`. `Layer.softDelete()` 는 하위 axis 가 있으면 `LAYER_HAS_ACTIVE_AXES` 예외 (v1 은 강제 이동, 자동 연쇄 안 함).

**핵심 파일**:
- 수정: `Layer.java` — `softDelete()`, `isDeleted()`, `@SQLRestriction`
- 수정: `LearningFacadeCommandService.java` — `removeLayer(layerId)`

### 완료 기준 (AC)

- Given axis 0건 layer / When `removeLayer` / Then softDelete 성공, `deleted_at != null`
- Given axis 3건 layer / When `removeLayer` / Then `LAYER_HAS_ACTIVE_AXES` 409
- Given softDeleted layer / When `GET /layers` / Then 미반환 (자동 필터)
- *(멱등)* Given 이미 softDeleted layer / When `softDelete()` 재호출 / Then `LAYER_ALREADY_DELETED` 예외

### Definition of Done

- [ ] Layer softDelete 구현
- [ ] 단위 테스트
- [ ] 통합 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Story 2-1
- 후행: Story 2-5

---

## [Story 2-5] Layer 최대 개수 · displayOrder · 정책 확정

### User Story

- As a 도메인 코드 작성자
- I want Layer 관련 규칙을 도메인 상수로 응집하기를
- so that 어디서도 재정의하지 않고 정책이 일관된다

### 설명

> 출처: `issue-05-layer-server-domain.md`

- `Layer.RECOMMENDED_LAYER_COUNT_LIMIT = 5` (권장 상한, 초과 시 응답 flag `isLayerCountExceedsRecommended: true`)
- `Layer.MAX_LAYER_NAME_LENGTH = 100`
- displayOrder: `addLayer` 는 마지막 + 1, `reorderLayers(List<Long>)` 은 id 집합 일치 검증 + 1부터 재부여
- 이름 정규화: trim, blank 거부, 중복 거부

**핵심 파일**:
- 수정: `LearningFacade.java` — `addLayer(name)`, `reorderLayers(ids)`, `isLayerCountExceedsRecommended()`

### 완료 기준 (AC)

- Given 5개 Layer / When `addLayer("추가")` / Then `isLayerCountExceedsRecommended()` returns true
- Given 3개 Layer 순서 [1,2,3] / When `reorderLayers([3,1,2])` / Then displayOrder 1=id3, 2=id1, 3=id2
- Given `reorderLayers([99])` (존재하지 않는 id) / When / Then `LAYER_REORDER_ID_MISMATCH` 예외

### Definition of Done

- [ ] 도메인 상수 · 검증
- [ ] 단위 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Story 2-1
- 후행: 없음

---

# [Epic 3] Roadmap/Selection Dual-Axis + Selection 정책

> **⚠️ SUPERSEDED (2026-07-02) — Story 3-1 ~ 3-4는 이슈 #15/#16/#17/#18/#19 이관 산출물로 재편성 예정.**
> Story 3-1(axis_roadmap), Story 3-2(axis_selection), Story 3-4(Roadmap/Selection API)의 스키마·API는 챕터 노드 first-class 승격 결정으로 뒤집힘. Story 3-3(AxisTopic soft-deprecate + Topic → Roadmap 백필)의 백필 대상 스키마도 `axis_roadmap` → `axis_roadmap_node`로 변경.
> Story 3-5(AxisTopic 폐기 문서화 + ADR022 용어 준수)는 유효 상태 유지.
> 재편성된 태스크는 아래 이슈 산출물 참고:
> - 이슈 #15: `axis_roadmap_node` 스키마·도메인·API
> - 이슈 #16: `axis_selection`(컨테이너) + `axis_selection_node` 스키마·도메인·API
> - 이슈 #17: 6-Port AI 생성 flow
> - 이슈 #18: 챕터 노드 재생성 API + hint
> - 이슈 #19: roadmap/selections 개념 명세 embed

## 목표

`AxisTopic` 을 폐기하고 축 당 **`AxisRoadmapNode[]` (챕터 노드 N개) + `AxisSelection`(컨테이너) × `AxisSelectionNode[]`** 형태로 재편해 축의 헌법과 판례를 명확히 분리하고, Selection 컨테이너의 name UNIQUE · created_at DESC · in-place update · hard delete 정책을 정착한다. 각 챕터 노드의 body는 사용자 예시(하네스 로드맵) 형태 ASCII 트리 통짜.

## 배경

기존 `AxisTopic` 은 축의 하위 계층이자 카드의 상위 계층이었으나, 축의 "헌법(로드맵)" 과 "사례(선정안)" 을 구분해 표현하지 못하고 카드 매핑도 잉여 join 을 강제. Roadmap/Selection 이중 축으로 표현 명확화 + Card 직접 매핑(Epic 4) 의 전제 확보.

## 포함 Story

- Story 3-1: `axis_roadmap` 테이블 + `AxisRoadmap` Aggregate (V19)
- Story 3-2: `axis_selection` 테이블 + `AxisSelection` Entity + 정책(#11) (V19)
- Story 3-3: `AxisTopic` / `TopicMaterial` soft-deprecate (V19 후반) + 백필 (기존 topic 을 Roadmap 초안으로 이관)
- Story 3-4: Roadmap/Selection API 신설 (`/axes/{id}/roadmap`, `/axes/{id}/selections`)
- Story 3-5: `AxisTopic` 폐기 문서화 + ADR022 용어 확인

## Epic 인수 시나리오

- Given axis 존재 / When `PUT /axes/{id}/roadmap { content }` / Then AxisRoadmap 1건 생성/갱신
- Given 이미 roadmap 있는 axis / When 두 번째 roadmap 생성 시도 / Then UNIQUE 위반
- Given `POST /axes/{id}/selections { name:"A", content }` / When 요청 / Then AxisSelection 생성 (displayOrder 등 없음, created_at DESC 정렬)
- Given 같은 axis 에 `name:"A"` selection 재생성 / When 요청 / Then `AXIS_SELECTION_NAME_DUPLICATE` 409
- Given `DELETE /axes/{id}/selections/{selectionId}` / When 요청 / Then hard delete (softDelete 없음)

## Epic 완료 기준 (DoD)

- [ ] Story 3-1~3-5 완료
- [ ] Flyway V19 + Rollback
- [ ] ErrorCode: `AXIS_ROADMAP_NOT_FOUND`, `AXIS_ROADMAP_ALREADY_EXISTS`, `AXIS_ROADMAP_CONTENT_TOO_LONG`, `AXIS_SELECTION_NAME_DUPLICATE`, `AXIS_SELECTION_NAME_BLANK`, `AXIS_SELECTION_CONTENT_TOO_LONG`
- [ ] `docs/DOMAIN.md` §Axis 하위 절 갱신 (Topic 절 삭제, Roadmap/Selection 절 신설)
- [ ] `AxisTopic` 코드 참조 = 0 (신규 도입 금지, 폐기 문맥만)

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **AxisTopic 즉시 삭제 vs soft-deprecate**: soft-deprecate. `_archived_axis_topic` 로 RENAME 후 다음 릴리스 물리 DROP.
- **AxisSelection softDelete vs hard delete**: hard delete (사용자 사례 관리이며 재작성 권장, softDelete 유지 부담이 이득 초과).
- **Roadmap content 크기 — VARCHAR(2000) vs TEXT**: VARCHAR(2000) 시작. TEXT 승격은 열린 질문 2.

---

## [Story 3-1] `axis_roadmap` 테이블 + `AxisRoadmap` Aggregate

### User Story

- As a 학습 사용자
- I want 축 당 1개의 로드맵(헌법) 을 저장·수정하기를
- so that 그 축이 다루는 개념 트리를 명시적으로 표현하고 AI 제안·리뷰의 축 정체성으로 활용한다

### 설명

> 출처: `issue-06-roadmap-selections-dualaxis.md`

`axis_roadmap` 테이블: `id BIGINT PK`, `axis_id BIGINT FK NOT NULL UNIQUE`, `content VARCHAR(2000) NOT NULL`, `created_at`, `updated_at`. FK `ON DELETE CASCADE`. `AxisRoadmap` Aggregate (또는 Axis 소속 Entity) 는 `setRoadmap(content)` / `updateRoadmap(content)` API.

**핵심 파일**:
- 신규: `V19__axis_roadmap_selection.sql`
- 신규: `R19__rollback_axis_roadmap_selection.sql`
- 신규: `LearningFacade/domain/model/AxisRoadmap.java`
- 수정: `LearningAxis.java` — `roadmap: AxisRoadmap?` 필드 + `setRoadmap(content)` 메서드

### 완료 기준 (AC)

- Given axis 존재 / When `axis.setRoadmap(content)` / Then AxisRoadmap 저장
- Given roadmap 이미 있는 axis / When `axis.setRoadmap(newContent)` / Then 기존 갱신 (신규 row 만들지 않음)
- Given `content` 2001자 / When / Then `AXIS_ROADMAP_CONTENT_TOO_LONG` 400
- Given `content` blank / When / Then `AXIS_ROADMAP_CONTENT_BLANK` 400

### Definition of Done

- [ ] V19 + R19 (roadmap 부분)
- [ ] 단위 테스트
- [ ] Repository Slice: UNIQUE 검증

### 스토리 포인트

1.5d

### 의존성

- 선행: 없음 (Epic 2 완료 후 Layer 는 있지만 Roadmap 은 axis 하위라 독립)
- 후행: Story 3-4

---

## [Story 3-2] `axis_selection` 테이블 + `AxisSelection` Entity + 정책

### User Story

- As a 학습 사용자
- I want 축의 사례/응용을 여러 개 이름 지어 저장하기를
- so that 같은 축에 대한 다양한 관점·시나리오를 관리할 수 있다

### 설명

> 출처: `issue-06-roadmap-selections-dualaxis.md`, `issue-11-selections-version-policy.md`

`axis_selection`: `id`, `axis_id NOT NULL`, `name VARCHAR(100) NOT NULL`, `content VARCHAR(2000) NOT NULL`, `created_at`, `updated_at`. UNIQUE `(axis_id, name)`. FK `ON DELETE CASCADE`. **softDelete 없음, hard delete 정책 (#11)**. 정렬: `ORDER BY created_at DESC`.

**핵심 파일**:
- 신규: `LearningFacade/domain/model/AxisSelection.java`
- 수정: `V19` — `axis_selection` 테이블 절 추가
- 수정: `LearningAxis.java` — `selections: List<AxisSelection>` 필드 + `addSelection(name, content)` / `removeSelection(id)` / `updateSelection(id, name, content)` (in-place update)

**정책 (#11)**:
- name UNIQUE per axis — 같은 이름 재생성 시 `AXIS_SELECTION_NAME_DUPLICATE`
- 정렬: `created_at DESC` (신규가 위에)
- 수정: in-place update 허용, 그러나 사용자에게는 신규 생성 권장 UX (UI 안내)
- 삭제: hard delete (softDelete 미도입)

### 완료 기준 (AC)

- Given axis 존재 / When `axis.addSelection("A", contentA)` / Then AxisSelection 생성
- Given 같은 axis 에 name "A" 이미 있음 / When `addSelection("A", contentB)` / Then `AXIS_SELECTION_NAME_DUPLICATE` 409
- Given 3건의 selection 생성 시간순 [T1, T2, T3] / When `selections` 조회 / Then [T3, T2, T1] 순
- Given selection 존재 / When `removeSelection(id)` / Then row 물리 삭제 (DB 조회 시 없음)
- Given selection `updateSelection(id, "A", newContent)` / When 실행 / Then row 갱신 (신규 생성 X)

### Definition of Done

- [ ] V19 (selection 부분) + R19
- [ ] 단위 테스트 (해피 · 엣지 · 예외 각 케이스)
- [ ] Repository Slice: UNIQUE 검증
- [ ] ErrorCode: `AXIS_SELECTION_NAME_DUPLICATE`, `AXIS_SELECTION_NAME_BLANK`, `AXIS_SELECTION_CONTENT_TOO_LONG`, `AXIS_SELECTION_CONTENT_BLANK`

### 스토리 포인트

2d

### 의존성

- 선행: 없음 (Roadmap 과 병렬 진행 가능)
- 후행: Story 3-4

---

## [Story 3-3] `AxisTopic` / `TopicMaterial` soft-deprecate + Topic → Roadmap 백필

### User Story

- As a 시스템 운영자
- I want 기존 AxisTopic 데이터가 Roadmap 초안으로 이관되기를
- so that 축의 컨텐츠가 유실되지 않고 다음 진화 단계로 이어진다

### 설명

> 출처: `issue-06-roadmap-selections-dualaxis.md`, ADR022

V19 백필:
```sql
-- 각 axis 의 topic 목록을 ASCII 트리로 조합해 roadmap.content 로 이관
INSERT INTO axis_roadmap (axis_id, content, created_at, updated_at)
SELECT axis_id,
       GROUP_CONCAT(CONCAT('- ', name) SEPARATOR '\n') AS content,
       NOW(6), NOW(6)
  FROM axis_topic
 WHERE deleted_at IS NULL
 GROUP BY axis_id;
```

`axis_topic` 테이블은 **soft-deprecate**: `RENAME TO _archived_axis_topic` (다음 릴리스에서 물리 DROP). `topic_material` 도 동일. 도메인 코드에서 `AxisTopic` / `TopicMaterial` 신규 참조 금지 (ADR022).

**핵심 파일**:
- 수정: `V19` (백필 + RENAME 절)
- 삭제 예정: `LearningFacade/domain/model/AxisTopic.java` (실제 삭제는 Epic 4/5 완료 후, 잔존 참조 정리 후)

### 완료 기준 (AC)

- Given axis 에 topic 3건 [X, Y, Z] / When V19 실행 / Then AxisRoadmap 생성됨, content = "- X\n- Y\n- Z"
- Given topic 없는 axis / When V19 실행 / Then AxisRoadmap 미생성
- Given V19 완료 후 / When `SHOW TABLES LIKE 'axis_topic'` / Then 결과 없음, `_archived_axis_topic` 만 존재

### Definition of Done

- [ ] V19 백필 + RENAME 완료
- [ ] 통합 테스트: 이관 결과 검증
- [ ] `AxisTopic.java` 새 파일 참조 없음 확인 (`grep -r "AxisTopic" src/main/java/`)

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 3-1, 3-2
- 후행: Epic 4 (Card→Axis 마이그레이션이 topic_id 처리 전에 이 백필 완료돼야 함)

---

## [Story 3-4] Roadmap / Selection API 신설

### User Story

- As a FE 개발자
- I want Roadmap · Selection CRUD 엔드포인트를 사용하기를
- so that UI 에서 축의 헌법과 사례를 관리할 수 있다

### 설명

> 출처: `issue-06-roadmap-selections-dualaxis.md`

REST 엔드포인트:
- `GET /axes/{axisId}/roadmap` — 조회 (없으면 404 `AXIS_ROADMAP_NOT_FOUND`)
- `PUT /axes/{axisId}/roadmap { content }` — 생성/갱신 (idempotent set)
- `DELETE /axes/{axisId}/roadmap` — 삭제
- `GET /axes/{axisId}/selections` — 목록 (created_at DESC)
- `POST /axes/{axisId}/selections { name, content }` — 신규
- `GET /selections/{selectionId}` — 단건 상세
- `PATCH /selections/{selectionId} { name?, content? }` — in-place update
- `DELETE /selections/{selectionId}` — hard delete

**핵심 파일**:
- 신규: `LearningFacade/presentation/AxisRoadmapController.java`
- 신규: `LearningFacade/presentation/AxisSelectionController.java`
- 신규: `LearningFacade/presentation/dto/AxisRoadmapRequest.java`, `AxisRoadmapResponse.java`, `AxisSelectionRequest.java`, `AxisSelectionResponse.java`
- 신규: `LearningFacade/application/service/AxisRoadmapCommandService.java`, `AxisRoadmapQueryService.java`, `AxisSelectionCommandService.java`, `AxisSelectionQueryService.java`

### 완료 기준 (AC)

- Given axis 존재 / When `PUT /axes/{id}/roadmap { content:"트리..." }` / Then 200 { roadmapId, content }
- Given roadmap 없는 axis / When `GET /axes/{id}/roadmap` / Then 404 `AXIS_ROADMAP_NOT_FOUND`
- Given selection 3건 / When `GET /axes/{id}/selections` / Then created_at DESC 정렬
- Given `POST /axes/{id}/selections { name:"A", content }` 두 번 / When 두 번째 / Then 409 `AXIS_SELECTION_NAME_DUPLICATE`
- Given `DELETE /selections/{id}` / When / Then 204, DB 물리 삭제

### Definition of Done

- [ ] Controller Slice 테스트 (해피 · 엣지 · 예외 각각)
- [ ] 통합 테스트: 전체 흐름
- [ ] Swagger UI 업데이트

### 스토리 포인트

2d

### 의존성

- 선행: Story 3-1, 3-2
- 후행: 없음

---

## [Story 3-5] `AxisTopic` 폐기 문서화 + ADR022 용어 준수 확인

### User Story

- As a 문서 유지자
- I want AxisTopic 폐기가 DOMAIN.md / PACKAGE.md 에 반영되기를
- so that 신규 개발자가 AxisTopic 을 신규 도입하지 않도록 명확한 신호가 있다

### 설명

> 출처: `issue-06-roadmap-selections-dualaxis.md`, ADR022

- `docs/DOMAIN.md` §Axis 하위 절 재작성: Topic 절 삭제, Roadmap/Selection 절 신설
- `docs/PACKAGE.md` — AxisTopic 파일 삭제 예정 명시 (Epic 4/5 완료 후 실제 삭제)
- ADR022 인용 문구 신규 문서에 명시

### 완료 기준 (AC)

- Given DOMAIN.md / When 조회 / Then §학습 도메인 절에 Roadmap/Selection 정의 존재, Topic 절 없음
- Given `grep -r "AxisTopic\|axis_topic" workflow/task/pes/workspectrum/sdd/in-progress/*.md` / When 실행 / Then 폐기 문맥에서만 등장

### Definition of Done

- [ ] DOMAIN.md 갱신
- [ ] PACKAGE.md 갱신
- [ ] 신규 SDD 3개(learning-tower / ai-suggestion / ai-interactive-roadmap) ADR022 인용 확인

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 3-1, 3-2, 3-3, 3-4
- 후행: 없음

---

> **Epic 3 재정의 판 (2026-07-02, 이슈 #15/#16 이관)** — Story 3-1~3-4 SUPERSEDED 이후 신설된 노드 first-class 승격 스토리들. Story 3-5 (문서화)와 병존.

## [Story 3-6] `AxisRoadmapNode` 도메인 신설

### User Story

- As a 백엔드 개발자
- I want `LearningAxis` Aggregate에 `AxisRoadmapNode` 자식 Entity + 컬렉션 API가 신설되기를
- so that Roadmap이 챕터 노드 first-class 스키마로 저장·조회·순서변경 가능하다

### 설명

> 출처: `issue-15-roadmap-node-model.md` §부속 결정 §도메인 모델

- `LearningFacade/domain/model/AxisRoadmapNode.java` 신설 (Entity)
- 필드: `id`, `axisId`, `displayOrder`, `title VARCHAR(200)`, `rationale VARCHAR(500)?`, `body TEXT NOT NULL`, audit + soft delete
- `LearningAxis.roadmapNodes: List<AxisRoadmapNode>` 컬렉션 (Aggregate 확장)
- `LearningAxis.addRoadmapNode(title, rationale, body)` — display_order = max + 1
- `LearningAxis.reorderRoadmapNodes(orderedIds)` — id 집합 검증 (`ROADMAP_NODE_ORDER_MISMATCH`)
- `LearningAxis.removeRoadmapNode(nodeId)` — Soft Delete
- `AxisRoadmapNode.updateTitle/updateRationale/updateBody` — trim + blank 검증
- 이슈 #6의 `AxisRoadmap` (`content TEXT` 통짜) SUPERSEDED

### 완료 기준 (AC)

- Given axis 존재 / When `axis.addRoadmapNode("1. 하네스 엔지니어링 기초", "AI 에이전트 기본 프레임", "├── 1-1. 정의와 본질\n│       모델 + 하네스 — ...")` / Then AxisRoadmapNode 저장, display_order=1
- Given axis에 노드 2개 / When 세 번째 추가 / Then display_order=3
- Given noderderedIds가 현재 컬렉션 id 집합과 불일치 / When `reorderRoadmapNodes(mismatched)` / Then `ROADMAP_NODE_ORDER_MISMATCH` 예외
- *(엣지)* body가 blank / Then `ROADMAP_NODE_BODY_BLANK` 예외

### Definition of Done

- [ ] 구현 (`AxisRoadmapNode.java`, `LearningAxis.java` 확장)
- [ ] 단위 테스트 (`AxisRoadmapNodeTest` 해피/엣지/예외 · `LearningAxisTest.addRoadmapNode_*`)
- [ ] `@Enumerated` · `@OneToMany` cascade + orphanRemoval JPA 매핑

### 스토리 포인트

2d

### 의존성

- 선행: Epic 2 완료 (axis가 Layer 아래 안정)
- 후행: Story 3-7 (Flyway 스키마), Story 3-8 (API)

## [Story 3-7] `axis_roadmap_node` Flyway V18 + `axis_roadmap.content` 아카이브 + 파싱 마이그레이션

### User Story

- As a 운영자
- I want `axis_roadmap_node` 테이블 신설 + 기존 `axis_roadmap.content` best-effort 파싱 마이그레이션되기를
- so that 스키마 정합 + 데이터 손실 최소화

### 설명

> 출처: `issue-15-roadmap-node-model.md` §스키마 §데이터 이관

- `V18__axis_roadmap_node.sql` + `R18__rollback_axis_roadmap_node.sql`
  - `axis_roadmap_node` 신설 (PK, axis_id FK CASCADE, display_order CHECK ≥ 1, title, rationale, body TEXT NOT NULL, audit + deleted_at, `idx_roadmap_node_axis_order` · `idx_roadmap_node_deleted`)
  - `axis_roadmap` 테이블을 `_archived_axis_roadmap`로 RENAME (아카이브 · 후속 릴리스에서 DROP)
- 기존 `axis_roadmap.content` 파싱 마이그레이션 (best-effort)
  - 챕터 단위 (`├── N.` 정규식) 분할 → 각 챕터를 `AxisRoadmapNode`로 저장
  - 파싱 실패 시 통짜를 "1. (전체)" 단일 노드로 저장 + 실패 로깅
- M3 초기 3명 사용자 규모라 데이터 극소량 → best-effort로 충분

### 완료 기준 (AC)

- Given `V18` migrate / Then `axis_roadmap_node` 테이블 존재 + 인덱스 2개 + CHECK 통과 + `_archived_axis_roadmap` 존재
- Given axis_roadmap 3건 (파싱 가능 데이터) / When migrate / Then 각각 챕터 단위 axis_roadmap_node로 분할
- Given axis_roadmap 1건 (파싱 실패 데이터) / When migrate / Then 통짜 단일 노드로 저장 + 로그 기록
- Given 롤백 스크립트 / When 실행 / Then `axis_roadmap_node` DROP + `_archived_axis_roadmap` 원복

### Definition of Done

- [ ] Flyway V·R 두 스크립트
- [ ] 파싱 마이그레이션 로직 (Java `@Component` or SQL 함수 — 결정 후 명시)
- [ ] Slice 테스트 (`@DataJpaTest`) — UNIQUE 없음 · CASCADE · CHECK 검증
- [ ] 통합 테스트 (`@SpringBootTest`) — 롤백 검증

### 스토리 포인트

2d

### 의존성

- 선행: Story 3-6 (`AxisRoadmapNode` 도메인)
- 후행: Story 3-8

## [Story 3-8] Roadmap 노드 API + ErrorCode + Slice 테스트

### User Story

- As a 프론트엔드 개발자
- I want Roadmap 노드 CRUD + 순서변경 REST API가 제공되기를
- so that FE `<RoadmapNodeList>` 컴포넌트가 노드를 조작 가능

### 설명

> 출처: `issue-15-roadmap-node-model.md` §API 표면 §이관 산출물

- `AxisRoadmapNodeController` 신설
  - `POST /api/v1/axes/{axisId}/roadmap-nodes` — 챕터 노드 추가 (201)
  - `GET /api/v1/axes/{axisId}/roadmap-nodes` — 노드 목록 (display_order ASC)
  - `PATCH /api/v1/roadmap-nodes/{nodeId}` — 부분 필드 편집
  - `DELETE /api/v1/roadmap-nodes/{nodeId}` — Soft Delete (204)
  - `PUT /api/v1/axes/{axisId}/roadmap-nodes/order` — 순서 재부여 (200)
- 기존 `PUT /api/v1/axes/{axisId}/roadmap { content }` 폐기 → 410 Gone
- ErrorCode 신설 (`Common/Exception/ErrorCode/ErrorCode` enum):
  - `ROADMAP_NODE_NOT_FOUND` (404)
  - `ROADMAP_NODE_TITLE_BLANK` (400)
  - `ROADMAP_NODE_BODY_BLANK` (400)
  - `ROADMAP_NODE_ORDER_MISMATCH` (400)
- Application Service — `AxisRoadmapNodeCommandService` (add/update/delete/reorder) + `AxisRoadmapNodeQueryService` (list)
- Request/Response DTO — `AxisRoadmapNodeCreateRequest`, `AxisRoadmapNodeUpdateRequest`, `AxisRoadmapNodeReorderRequest`, `AxisRoadmapNodeResponse`

### 완료 기준 (AC)

- Given axis / When `POST /api/v1/axes/{axisId}/roadmap-nodes { title, rationale, body: "├── 1-1. ..." }` / Then 201 + Location header + Response body
- Given 노드 3개 / When `PUT .../order [id3, id1, id2]` / Then 200 + display_order 재부여 (1, 2, 3)
- Given 기존 `PUT .../roadmap { content }` / When 호출 / Then 410 Gone
- *(예외)* body 없이 POST / Then `ROADMAP_NODE_BODY_BLANK` 400
- *(예외)* 존재하지 않는 nodeId 조회 / Then `ROADMAP_NODE_NOT_FOUND` 404

### Definition of Done

- [ ] Controller · Service · DTO 구현
- [ ] ErrorCode 4종 등록
- [ ] Controller Slice 테스트 (`@WebMvcTest`) — 6 엔드포인트 각각 해피/엣지/예외
- [ ] Repository Slice 테스트 (`@DataJpaTest`) — 정렬 · Soft Delete 필터 검증
- [ ] Postman 컬렉션 업데이트 (선택)

### 스토리 포인트

3d

### 의존성

- 선행: Story 3-7 (Flyway V18)
- 후행: 없음 (Epic 3 Roadmap 노드 완주)

## [Story 3-9] `AxisSelectionNode` 도메인 신설 + 컨테이너 정책 계승

### User Story

- As a 백엔드 개발자
- I want `AxisSelection` 컨테이너 아래 `AxisSelectionNode` 자식 Entity가 신설되기를
- so that Selection도 챕터 노드로 조작 가능 + 이슈 #11 컨테이너 정책(name UNIQUE · created_at DESC · hard delete)은 그대로 유지

### 설명

> 출처: `issue-16-selection-node-model.md` §부속 결정 §도메인 모델

- `LearningFacade/domain/model/AxisSelectionNode.java` 신설 (Entity)
- 필드: `id`, `selectionId`, `displayOrder`, `title`, `rationale?`, `body TEXT NOT NULL`, audit
- `AxisSelection.nodes: List<AxisSelectionNode>` 컬렉션 (Aggregate 확장)
- `AxisSelection.addNode(title, rationale, body)`, `reorderNodes(orderedIds)`, `removeNode(nodeId)` (hard delete)
- 이슈 #11 컨테이너 정책 계승:
  - `axis_selection` 스키마 유지 (name UNIQUE per axis, created_at DESC)
  - `content` 컬럼 폐기 (Story 3-10에서 archive)
  - 컨테이너 hard delete (자식 노드 CASCADE)
- `AxisSelectionNode.updateTitle/updateRationale/updateBody` — trim + blank 검증

### 완료 기준 (AC)

- Given selection 컨테이너 / When `addNode("1. IoC & DI 핵심 원리", "...", "├── 1-1. ...")` / Then AxisSelectionNode 저장, display_order=1
- Given selection의 노드 2개 / When `removeNode(id1)` / Then hard delete + display_order 재계산 없음 (사용자 명시 reorder 필요)
- Given 컨테이너 name UNIQUE 위반 / Then `AXIS_SELECTION_NAME_ALREADY_EXISTS` (이슈 #11 계승)

### Definition of Done

- [ ] 구현 (`AxisSelectionNode.java`, `AxisSelection.java` 확장)
- [ ] 단위 테스트 해피/엣지/예외
- [ ] `@OneToMany` cascade + orphanRemoval (자식 노드 hard delete)

### 스토리 포인트

2d

### 의존성

- 선행: Story 3-8 (Roadmap 노드 인프라 확립)
- 후행: Story 3-10, 3-11

## [Story 3-10] `axis_selection_node` Flyway V19 + `axis_selection.content` V20 archive + 파싱 마이그레이션

### User Story

- As a 운영자
- I want `axis_selection_node` 신설 + 기존 content 파싱 마이그레이션 + content 컬럼 archive되기를
- so that Selection도 노드 스키마로 정착

### 설명

> 출처: `issue-16-selection-node-model.md` §스키마 §데이터 이관

- `V19__axis_selection_node.sql` + `R19__rollback_axis_selection_node.sql`
  - `axis_selection_node` 신설 (PK, selection_id FK CASCADE, display_order CHECK ≥ 1, title, rationale, body TEXT NOT NULL, audit, `idx_selection_node_selection_order`)
  - Soft Delete 없음 (컨테이너 hard delete 정책 계승)
- `V20__archive_axis_selection_content.sql` + `R20__` 롤백
  - `axis_selection.content` 컬럼 → `_archived_content`로 RENAME (아카이브)
- 기존 `axis_selection.content` best-effort 파싱 → 챕터 단위 `AxisSelectionNode`로 분할

### 완료 기준 (AC)

- Given `V19` + `V20` migrate / Then `axis_selection_node` 존재 + `axis_selection._archived_content` 존재
- Given `axis_selection.content` 데이터 / When migrate / Then 챕터 단위 node로 분할 or 통짜 단일 노드
- Given 롤백 / When 실행 / Then 두 스크립트 원복 성공

### Definition of Done

- [ ] Flyway 4개 스크립트 (V19·R19 · V20·R20)
- [ ] 파싱 마이그레이션 로직 (Story 3-7 재사용 · Roadmap 파서 리팩터링)
- [ ] Slice 테스트 · 통합 테스트 (롤백)

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 3-9 (도메인)
- 후행: Story 3-11

## [Story 3-11] Selection 노드 API + ErrorCode + Slice 테스트

### User Story

- As a 프론트엔드 개발자
- I want Selection 컨테이너 아래 노드 CRUD API가 제공되기를
- so that FE `<SelectionNodeList>` 컴포넌트가 조작 가능

### 설명

> 출처: `issue-16-selection-node-model.md` §API 표면 §이관 산출물

- `AxisSelectionNodeController` 신설
  - `POST /api/v1/selections/{selectionId}/nodes` — 노드 추가 (201)
  - `GET /api/v1/selections/{selectionId}/nodes` — 노드 목록 (display_order ASC)
  - `PATCH /api/v1/selection-nodes/{nodeId}` — 부분 필드 편집
  - `DELETE /api/v1/selection-nodes/{nodeId}` — hard delete (204)
  - `PUT /api/v1/selections/{selectionId}/nodes/order` — 순서 재부여
- 기존 `PATCH /api/v1/axes/{axisId}/selections/{selectionId}` content 부분 폐기 → content 필드 무시
- ErrorCode 신설:
  - `SELECTION_NODE_NOT_FOUND` (404)
  - `SELECTION_NODE_TITLE_BLANK` (400)
  - `SELECTION_NODE_BODY_BLANK` (400)
- Application Service · DTO 세트

### 완료 기준 (AC)

- Given selection 컨테이너 / When `POST /api/v1/selections/{selectionId}/nodes` / Then 201
- Given 컨테이너 hard delete / When `DELETE /api/v1/axes/{axisId}/selections/{selectionId}` / Then 컨테이너 + 자식 노드 CASCADE 삭제
- *(예외)* 존재하지 않는 nodeId / Then `SELECTION_NODE_NOT_FOUND` 404

### Definition of Done

- [ ] Controller · Service · DTO 구현
- [ ] ErrorCode 3종 등록
- [ ] Controller Slice · Repository Slice 테스트

### 스토리 포인트

2.5d

### 의존성

- 선행: Story 3-10 (Flyway)
- 후행: 없음 (Epic 3 Selection 노드 완주)

---

# [Epic 4] Card→Axis 직접 매핑

## 목표

`Card.topic_id` 를 폐기하고 `Card.axis_id NOT NULL` 로 직접 매핑해 축 스코프 조회의 join 을 단순화하고, Coverage 재계산을 축 스코프로 이관한다.

## 배경

기존 `Card → Topic → Axis` 체인은 카드 축 조회 시마다 이중 join 을 강제. Epic 3 이 Topic 계층을 폐기하기로 결정한 이상 Card 도 Axis 직접 매핑으로 이행. 3단계 마이그레이션(nullable 컬럼 추가 → 백필 → NOT NULL 승격) 으로 안전 이관.

## 포함 Story

- Story 4-1: `card.axis_id BIGINT NULL` 컬럼 추가 (V20 단계 1)
- Story 4-2: `topic_id → axis_id` 백필 (V20 단계 2)
- Story 4-3: `card.axis_id NOT NULL` 승격 + `topic_id` soft-deprecate (V20 단계 3)
- Story 4-4: Coverage 재계산을 axis 스코프로 이관
- Story 4-5: `Card.recordView()` axis_id 로 이벤트 발행 (기존 topicId → axisId 로 변경)

## Epic 인수 시나리오

- Given topic_id=T1 인 카드 3건 (T1.axis_id=A1) / When V20 백필 / Then 3건 모두 axis_id=A1 설정
- Given V20 완료 후 / When `Card.of(userId, axisId, summary, keywords)` / Then Card 생성 (topic_id 는 필수 아님)
- Given `axis_id=NULL` insert 시도 / When / Then `DataIntegrityViolationException`
- Given axis 스코프 Coverage 조회 / When `axis.coverage()` / Then 카드 상태 집계 반환

## Epic 완료 기준 (DoD)

- [ ] Story 4-1~4-5 완료
- [ ] Flyway V20 (3단계) + Rollback
- [ ] ErrorCode: `CARD_AXIS_REQUIRED`, `CARD_TOPIC_LEGACY_REFERENCE` (전환기)
- [ ] `Card.topic_id` 새 코드에서 참조 0건
- [ ] `docs/DOMAIN.md` §Card 갱신 (topic_id 제거 문서화)

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **`topic_id` 컬럼 물리 삭제 시점** — 다음 릴리스(0.1.0v). 본 Epic 은 컬럼 유지 + 도메인에서만 제거.
- **Coverage 재계산 전략** — 축 단위 즉시 재계산 vs 배치. 즉시 재계산(카드 상태 변경 트랜잭션 내).

---

## [Story 4-1] `card.axis_id BIGINT NULL` 컬럼 추가

### User Story

- As a 백엔드 엔지니어
- I want card 에 axis_id 컬럼을 안전하게 추가하기를
- so that 백필 진행 중에도 기존 카드 조회가 유지된다

### 설명

> 출처: `issue-07-card-axis-direct-mapping.md`

Flyway V20 단계 1:
```sql
ALTER TABLE card ADD COLUMN axis_id BIGINT NULL AFTER topic_id;
ALTER TABLE card ADD INDEX idx_card_axis (axis_id);
```

FK 는 백필 완료 후 추가(Story 4-3 에서).

### 완료 기준 (AC)

- Given V20-1 실행 / When 스키마 확인 / Then `card.axis_id` 컬럼 존재, NULL 허용
- Given 기존 카드 / When 조회 / Then 정상 동작 (axis_id=NULL 상태)

### Definition of Done

- [ ] V20-1
- [ ] Repository Slice: NULL 허용 확인

### 스토리 포인트

0.5d

### 의존성

- 선행: Epic 2 완료 (Axis 계층 준비), Epic 3 Story 3-3 (Topic 백필 완료)
- 후행: Story 4-2

---

## [Story 4-2] `topic_id → axis_id` 백필

### User Story

- As a 시스템 운영자
- I want 기존 카드의 axis_id 를 topic 참조에서 유도하기를
- so that 카드가 소속 축을 잃지 않는다

### 설명

> 출처: `issue-07-card-axis-direct-mapping.md`

V20 단계 2:
```sql
UPDATE card c
   JOIN _archived_axis_topic t ON c.topic_id = t.id
   SET c.axis_id = t.axis_id
 WHERE c.deleted_at IS NULL AND c.axis_id IS NULL;
```

_archived_axis_topic 참조 이유: Epic 3 Story 3-3 에서 RENAME 됨. 백필은 아카이브 테이블에서 읽음.

### 완료 기준 (AC)

- Given topic_id=T (T.axis_id=A) 인 카드 5건 / When V20-2 실행 / Then 5건 모두 axis_id=A
- Given topic_id=NULL 카드 (레거시 고아) / When V20-2 실행 / Then axis_id=NULL 유지 (Story 4-3 에서 별도 처리)
- Given softDeleted 카드 / When V20-2 / Then 무영향

### Definition of Done

- [ ] V20-2 backfill
- [ ] 통합 테스트: 이관 결과 검증
- [ ] 백필 실패율 = 0 (통계 쿼리)

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 4-1, Epic 3 Story 3-3
- 후행: Story 4-3

---

## [Story 4-3] `card.axis_id NOT NULL` 승격 + `topic_id` soft-deprecate

### User Story

- As a 백엔드 엔지니어
- I want card.axis_id 가 NOT NULL 로 강제되기를
- so that axis_id 없는 카드가 존재할 수 없고 도메인에서 null 분기가 사라진다

### 설명

> 출처: `issue-07-card-axis-direct-mapping.md`

V20 단계 3:
```sql
-- 1. 고아 카드(axis_id 여전히 NULL) 처리 — softDelete 로 아카이브
UPDATE card SET deleted_at = NOW(6)
 WHERE axis_id IS NULL AND deleted_at IS NULL;

-- 2. NOT NULL 승격
ALTER TABLE card MODIFY axis_id BIGINT NOT NULL;

-- 3. FK 추가
ALTER TABLE card ADD CONSTRAINT fk_card_axis
    FOREIGN KEY (axis_id) REFERENCES learning_axis (id) ON DELETE RESTRICT;

-- 4. topic_id 는 컬럼 유지 (soft-deprecate). @Deprecated 도메인 필드
```

`Card.java` 도메인 필드 `@Column(name="axis_id", nullable=false) private Long axisId`. `Card.of(userId, axisId, ...)` 정적 팩토리 로 axisId 필수. `topic_id` 는 도메인 참조 제거.

**핵심 파일**:
- 수정: `V20` (단계 3 절 추가)
- 수정: `Card.java` — axisId 필드 + 팩토리 갱신, topicId 필드 제거
- 수정: `CardCommandService.java` — 생성 시 axisId 인자 필수

### 완료 기준 (AC)

- Given V20-3 실행 / When 스키마 확인 / Then `card.axis_id NOT NULL` + FK 존재
- Given axisId=null 로 Card 저장 시도 / When / Then `DataIntegrityViolationException`
- Given 기존 고아 카드(axis_id=NULL) / When V20-3 실행 / Then softDelete 로 아카이브

### Definition of Done

- [ ] V20-3
- [ ] Card 도메인 리팩토링
- [ ] Repository Slice
- [ ] 통합 테스트

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 4-2
- 후행: Story 4-4, 4-5

---

## [Story 4-4] Coverage 재계산 축 스코프 이관

### User Story

- As a 백엔드 엔지니어
- I want Coverage 재계산이 축 스코프로 이관되기를
- so that Topic 계층 제거 후에도 진행률 · 커버리지가 정확히 갱신된다

### 설명

> 출처: `issue-07-card-axis-direct-mapping.md`

기존 `CoverageRecalculator` 가 Topic 단위로 카드 상태 집계 → Axis 단위 집계로 이관. Card 상태 변경(recordView / archive / returnToField) 시 `axis.recalculateCoverage()` 호출.

**핵심 파일**:
- 수정: `LearningFacade/domain/service/CoverageRecalculator.java`
- 수정: `LearningAxis.java` — `coverageStatus` 필드 + `recalculateCoverage()` (topic 이 아닌 자신의 axis_id 참조 카드 집계)
- 삭제 예정: `AxisTopic.coverageStatus` (이관 후 불필요)

### 완료 기준 (AC)

- Given axis 에 카드 [ON_FIELD 3, ARCHIVE 2] / When `axis.recalculateCoverage()` / Then coverageStatus 갱신 (`PARTIAL` 예상)
- Given 카드 0건 axis / When 재계산 / Then `NO_MATERIAL`
- Given 카드 모두 ARCHIVE 축 / When 재계산 / Then 정책 확정 (예: `COVERED`)

### Definition of Done

- [ ] 재계산 이관 완료
- [ ] 단위 테스트
- [ ] 통합 테스트

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 4-3
- 후행: 없음

---

## [Story 4-5] `Card.recordView()` axis_id 이벤트 발행

### User Story

- As a 관측 소비자
- I want Card recordView 이벤트가 axisId 를 포함하기를
- so that ReviewSession · 통계 집계가 축 스코프에서 정확하다

### 설명

> 출처: `issue-07-card-axis-direct-mapping.md`

기존 `CardViewedEvent { cardId, userId, topicId }` → `CardViewedEvent { cardId, userId, axisId }` 로 변경. 이벤트 소비자(ReviewQueryService 등) 모두 axisId 참조로 갱신.

### 완료 기준 (AC)

- Given card.recordView() / When 이벤트 발행 / Then event.axisId != null, event.topicId 없음
- Given ReviewQueryService.updateStats(event) / When 처리 / Then axisId 기반 집계

### Definition of Done

- [ ] 이벤트 필드 변경
- [ ] 소비자 리팩토링
- [ ] 통합 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Story 4-3
- 후행: 없음

---

# [Epic 5] Deck BC 폐기 및 Axis 흡수

## 목표

Deck BC 를 완전 폐기하고 8개 책임(progressStatus, mode, lastAccessed, learningMaterialId, onLibrary, publishedAt + 파생 2건) 을 LearningAxis 로 흡수한다. `/decks/*` 엔드포인트는 `/axes/*` 로 통합, `deck` 테이블은 soft-deprecate.

## 배경

ADR021 이후 Deck 는 Axis 이벤트 자동 생성만 남았고 도메인 표면적이 사실상 0. 자체 BC 로 유지할 정당성 부재. Card→Axis 직접 매핑(Epic 4) 이 완료된 이후 Deck 은 참조 대상 없이 유령 상태 → 폐기 안전.

## 포함 Story

- Story 5-1: Deck 8 책임 필드 Axis 로 이전 (도메인 리팩토링)
- Story 5-2: `card.deck_id` FK 폐기 (V21)
- Story 5-3: `/decks/*` 엔드포인트 폐기, `/axes/*` 통합
- Story 5-4: `deck` / `sub_deck` 테이블 soft-deprecate (RENAME → `_archived_deck`, `_archived_sub_deck`) (V21)
- Story 5-5: `docs/DOMAIN.md` §Deck 절 삭제, §Axis 로 병합 + product-deck.md 삭제 준비

## Epic 인수 시나리오

- Given axis 에 progressStatus=IN_PROGRESS / When `GET /axes/{id}` / Then 응답에 progressStatus 포함
- Given 카드 조회 / When `GET /axes/{id}/cards` (기존 `/decks/{id}/cards` 대체) / Then 카드 목록 반환
- Given 폐기된 `/decks/*` 호출 / When 요청 / Then 404
- Given V21 완료 후 / When `SHOW TABLES LIKE 'deck'` / Then 결과 없음, `_archived_deck` 존재

## Epic 완료 기준 (DoD)

- [ ] Story 5-1~5-5 완료
- [ ] Flyway V21 + Rollback
- [ ] Deck 도메인 코드 참조 = 0 (`grep -r "Deck\b" src/main/java/com/example/thirdtool/Deck/`)
- [ ] `/decks/*` 엔드포인트 = 0
- [ ] `product-deck.md` 삭제 준비 완료 (Task 6 실행 대기)
- [ ] `docs/DOMAIN.md` §Deck 절 삭제, §Axis 로 병합
- [ ] `docs/PACKAGE.md` — Deck/ 패키지 삭제

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **Deck 물리 DROP 시점** — 본 Epic 은 soft-deprecate 만. 다음 릴리스(0.1.0v) 에서 물리 DROP.
- **Axis 로의 필드 이전 방식** — 8 필드 개별 이전 vs Value Object 로 그룹핑. 개별 이전(단순, VO 도입은 후속 리팩토링).

## 내부 메모 / 제약 사항

**명세 변경 이력 (product-deck.md 폐기에 따른 이력 흡수)**:
- 2026-06-30 (ADR020, `fix-deck-axis-visibility`): Deck↔Axis 가시화가 read-model 노출로 확정, 도메인 연관 승격 거부.
- 2026-07-01 (ADR021, `fix-axis-deck-full-integration`): Deck 생성 = Axis 이벤트 자동 경로 유일화, `deck.axis_id NOT NULL` 승격, LearningAxis Soft Delete 승격. FK `fk_deck_axis ON DELETE SET NULL` 안전망 유지.
- 2026-07-01 (본 Epic 5): Deck BC 완전 폐기 결정. 8 책임 Axis 로 흡수, `/decks/*` → `/axes/*` 통합, `deck` 테이블 soft-deprecate → 다음 릴리스 물리 DROP.
- 폐기 결정의 근거는 ADR021 이 Deck 을 axis 이벤트 자동 생성으로만 남기면서 자체 BC 유지 정당성이 소멸한 점, Card→Axis 직접 매핑(Epic 4) 이후 Deck 이 잔존 참조 없이 유령이 된 점.

---

## [Story 5-1] Deck 8 책임 필드 Axis 로 이전

### User Story

- As a 백엔드 엔지니어
- I want Deck 이 담당하던 8 필드(progressStatus, mode, lastAccessed, learningMaterialId, onLibrary, publishedAt + 파생 2건) 가 LearningAxis 로 이전되기를
- so that Deck BC 를 삭제해도 사용자 UX 가 유지된다

### 설명

> 출처: `issue-13-deck-abolition-axis-absorption.md`

각 필드를 `LearningAxis` 로 이전. 각 필드의 getter · setter · 도메인 규칙도 함께 이관.

**필드**:
1. `progressStatus: AxisProgressStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }` — 카드 상태 집계로 파생 (Epic 6 Story 6-4 와 연결)
2. `mode: AxisLearningMode { STUDY, REVIEW }` — 학습 모드
3. `lastAccessedAt: DateTime` — 마지막 접근 시각
4. `learningMaterialId: Long?` — 대표 자료 참조 (선택)
5. `onLibrary: Boolean` — 라이브러리 공개 여부
6. `publishedAt: DateTime?` — 공개 시각
7. `parentAxisId: Long?` (파생 1) — 상위 축 참조 (subDeck 개념 대체, Layer 도입으로 대부분 필요 없음)
8. `subAxisCount: Int` (파생 2) — 하위 axis 개수 캐시 (Layer 내 하위 axis 그룹핑 시)

**핵심 파일**:
- 수정: `LearningAxis.java` — 필드 8건 추가
- 수정: `V21__deck_abolition.sql` — Deck 테이블에서 컬럼 SELECT → learning_axis UPDATE
- 수정: 관련 응답 DTO

### 완료 기준 (AC)

- Given deck 테이블에 progressStatus=IN_PROGRESS row 1건 / When V21 백필 / Then 해당 axis.progress_status=IN_PROGRESS
- Given axis.mode 갱신 API / When `PATCH /axes/{id}/mode { mode:"REVIEW" }` / Then axis.mode=REVIEW
- Given axis 응답 / When `GET /axes/{id}` / Then progressStatus·mode 포함

### Definition of Done

- [ ] 필드 8건 이전
- [ ] V21 백필
- [ ] 통합 테스트

### 스토리 포인트

3d

### 의존성

- 선행: Epic 4 완료 (Card→Axis 직접)
- 후행: Story 5-2

---

## [Story 5-2] `card.deck_id` FK 폐기

### User Story

- As a 백엔드 엔지니어
- I want card.deck_id 참조가 완전 제거되기를
- so that Deck 테이블 폐기가 안전하다

### 설명

> 출처: `issue-13-deck-abolition-axis-absorption.md`

V21:
```sql
ALTER TABLE card DROP FOREIGN KEY fk_card_deck;
ALTER TABLE card DROP COLUMN deck_id;
```

Card 도메인의 `deckId` 필드 제거. 관련 조회 쿼리 axis_id 기반으로 완전 이관 (Epic 4 결과 위에서 진행).

### 완료 기준 (AC)

- Given V21 실행 / When 스키마 / Then `card.deck_id` 컬럼 없음
- Given `Card.of()` 호출 / When / Then deckId 인자 없음 (컴파일)

### Definition of Done

- [ ] V21 절
- [ ] Card 도메인 리팩토링
- [ ] 통합 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Story 5-1
- 후행: Story 5-3, 5-4

---

## [Story 5-3] `/decks/*` 엔드포인트 폐기, `/axes/*` 통합

### User Story

- As a FE 개발자
- I want deck 관련 엔드포인트가 axis 엔드포인트로 통합되기를
- so that 축=덱 정책이 URL 에서도 일관된다

### 설명

> 출처: `issue-13-deck-abolition-axis-absorption.md`

폐기 엔드포인트:
- `GET /decks`, `GET /decks/{id}`, `GET /decks/{id}/cards`, `PATCH /decks/{id}`, `DELETE /decks/{id}` — 모두 삭제

신설/유지 axis 엔드포인트:
- `GET /axes/{id}/cards` (기존 유지, Epic 4 결과)
- `PATCH /axes/{id} { name?, mode?, onLibrary?, ... }` — Axis 수정 통합
- `DELETE /axes/{id}` — Axis 삭제 (ADR021 정책 그대로)

**핵심 파일**:
- 삭제 예정: `Deck/presentation/DeckController.java`
- 수정: `LearningFacade/presentation/LearningAxisController.java`
- 수정: 응답 DTO

### 완료 기준 (AC)

- Given `GET /decks` / When 요청 / Then 404
- Given `PATCH /axes/{id} { mode:"REVIEW" }` / When 요청 / Then 200, mode 갱신
- Given `GET /axes/{id}/cards` / When 요청 / Then 카드 목록

### Definition of Done

- [ ] Deck Controller 삭제
- [ ] LearningAxisController 확장
- [ ] Controller Slice 테스트
- [ ] Swagger UI 갱신

### 스토리 포인트

2d

### 의존성

- 선행: Story 5-1, 5-2
- 후행: Story 5-5

---

## [Story 5-4] `deck` / `sub_deck` 테이블 soft-deprecate

### User Story

- As a 시스템 운영자
- I want deck 테이블을 즉시 삭제하지 않고 archive 로 이관하기를
- so that 예상치 못한 잔존 참조가 있으면 롤백 가능하다

### 설명

> 출처: `issue-13-deck-abolition-axis-absorption.md`

V21:
```sql
RENAME TABLE deck TO _archived_deck;
RENAME TABLE sub_deck TO _archived_sub_deck;
```

`_archived_*` 는 조회·수정 대상이 아님(폴리시). 다음 릴리스(0.1.0v) 에서 물리 DROP.

### 완료 기준 (AC)

- Given V21 실행 / When `SHOW TABLES` / Then `deck` 없음, `_archived_deck` 존재
- Given 통합 테스트 실행 / When 모든 카드/axis 시나리오 / Then 정상 (deck 참조 없음)

### Definition of Done

- [ ] V21 RENAME 절
- [ ] 통합 테스트 회귀 검증

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 5-1, 5-2, 5-3
- 후행: 없음

---

## [Story 5-5] `docs/DOMAIN.md` §Deck 절 삭제 및 병합

### User Story

- As a 문서 유지자
- I want Deck 관련 문서가 Axis 로 병합되기를
- so that 신규 개발자가 Deck 을 도입하지 않는다

### 설명

> 출처: `issue-13-deck-abolition-axis-absorption.md`

- `docs/DOMAIN.md` §2.2 Deck 절 삭제, 내용 중 유지 가치 있는 것(예: mode, progressStatus 정의) 은 §Axis 로 병합
- `docs/PACKAGE.md` — Deck/ 패키지 항목 삭제
- `product-deck.md` — Task 6 에서 삭제 준비 완료. 본 Story 는 문서상 흔적 정리.

### 완료 기준 (AC)

- Given DOMAIN.md / When 조회 / Then §Deck 절 없음
- Given PACKAGE.md / When 조회 / Then Deck/ 패키지 없음
- Given `grep -r "Deck\b" docs/` / When 실행 / Then 폐기 이력·이력 인용에서만 등장

### Definition of Done

- [ ] DOMAIN.md 갱신
- [ ] PACKAGE.md 갱신

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 5-1~5-4
- 후행: 없음

---

# [Epic 6] Review 이중 스코프 (Axis / Layer)

## 목표

`ReviewSession` 을 `AXIS` / `LAYER` 이중 스코프로 확장해 사용자가 축 단위 또는 Layer 단위로 리뷰를 시작 · 관리할 수 있게 하고, `Layer.progressStatus` 를 하위 axis 상태 로부터 파생한다.

## 배경

기존 ReviewSession 은 Deck 을 참조 → Deck 폐기(Epic 5) 로 재편 필요. 사용자 요구 "이 그룹 축 전체를 오늘 훑고 싶다" 를 Layer 스코프 세션으로 지지. "Layer 1" 이라는 모호한 UI 라벨을 명시적 `ReviewScope.LAYER` 로 대체.

## 포함 Story

- Story 6-1: `ReviewSession.scope: ReviewScope` enum + `scopeId: Long` 필드 추가 (V22)
- Story 6-2: `findAllByAxisId(axisId)` / `findAllByLayerId(layerId)` Repository 쿼리
- Story 6-3: `POST /layers/{layerId}/review-sessions` 엔드포인트 신설
- Story 6-4: `Layer.progressStatus` 파생 로직 (하위 axis 3-state → Layer 3-state)
- Story 6-5: FE 용어 "Layer 1" 대체 명명 (docs/ux/wip-language.md 갱신)

## Epic 인수 시나리오

- Given axis 존재 / When `POST /axes/{id}/review-sessions` / Then session.scope=AXIS
- Given layer 에 axis 3건, 각각 카드 여러 개 / When `POST /layers/{id}/review-sessions` / Then session.scope=LAYER, cards 통합 목록
- Given axis 3건 progressStatus=[IN_PROGRESS, IN_PROGRESS, COMPLETED] / When `Layer.progressStatus` / Then IN_PROGRESS
- Given axis 3건 모두 COMPLETED / When `Layer.progressStatus` / Then COMPLETED
- Given layer 에 axis 0건 / When `POST /layers/{id}/review-sessions` / Then 400 `LAYER_HAS_NO_AXES`

## Epic 완료 기준 (DoD)

- [ ] Story 6-1~6-5 완료
- [ ] Flyway V22 + Rollback
- [ ] ErrorCode: `LAYER_HAS_NO_AXES`, `REVIEW_SCOPE_INVALID`, `LAYER_REVIEW_ACCESS_DENIED`
- [ ] `docs/ux/wip-language.md` 갱신 ("Layer 1" 대체)
- [ ] 통합 테스트: Axis / Layer 스코프 세션 각각

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **ReviewSession.scope 표현** — 단일 세션 + enum + scopeId (vs 다형 세션). 단일 선택.
- **Layer.progressStatus 파생 규칙**:
  - 하위 axis 모두 NOT_STARTED → Layer NOT_STARTED
  - 하나 이상 IN_PROGRESS 또는 (NOT_STARTED + COMPLETED 혼재) → Layer IN_PROGRESS
  - 하위 axis 모두 COMPLETED → Layer COMPLETED
  - 하위 axis 0건 → Layer NOT_STARTED (또는 undefined? 열린 질문 10 확정 필요)

---

## [Story 6-1] `ReviewSession.scope` / `scopeId` 필드 추가

### User Story

- As a 백엔드 엔지니어
- I want ReviewSession 이 스코프와 대상 ID 를 명시적으로 표현하기를
- so that AXIS / LAYER 두 리뷰 흐름이 단일 도메인에서 조율된다

### 설명

> 출처: `issue-14-review-strategy-axis-layer-scope.md`

V22:
```sql
ALTER TABLE review_session
    ADD COLUMN scope VARCHAR(10) NOT NULL DEFAULT 'AXIS'
        CHECK (scope IN ('AXIS','LAYER')),
    ADD COLUMN scope_id BIGINT NOT NULL DEFAULT 0,
    ADD INDEX idx_review_scope (scope, scope_id);

-- 백필: 기존 세션은 AXIS scope, deck_id 를 axis_id 로 (Epic 5 결과 참조)
UPDATE review_session
   SET scope='AXIS', scope_id = axis_id
 WHERE scope IS NULL OR scope_id = 0;

ALTER TABLE review_session
    ALTER COLUMN scope DROP DEFAULT,
    ALTER COLUMN scope_id DROP DEFAULT;

-- deck_id 컬럼은 Epic 5 에서 이미 처리됨. 여기서는 axis_id 참조를 scope_id 로 통합
ALTER TABLE review_session DROP COLUMN axis_id;
```

`ReviewSession.java` — `scope: ReviewScope` + `scopeId: Long` 필드.

### 완료 기준 (AC)

- Given V22 실행 / When 스키마 / Then scope/scope_id 컬럼 존재
- Given `ReviewSession.ofAxis(axisId, cards)` / When 생성 / Then scope=AXIS, scopeId=axisId
- Given `ReviewSession.ofLayer(layerId, cards)` / When 생성 / Then scope=LAYER, scopeId=layerId

### Definition of Done

- [ ] V22
- [ ] ReviewSession 필드 추가
- [ ] 단위 테스트

### 스토리 포인트

1.5d

### 의존성

- 선행: Epic 5 완료 (Deck 폐기 후 리뷰 세션이 axis 참조로 이관됨)
- 후행: Story 6-2

---

## [Story 6-2] `findAllByAxisId` / `findAllByLayerId` Repository 쿼리

### User Story

- As a 백엔드 엔지니어
- I want ReviewSessionRepository 가 스코프별 조회를 제공하기를
- so that FE 가 축/Layer 각 스코프의 최근 세션을 확인할 수 있다

### 설명

> 출처: `issue-14-review-strategy-axis-layer-scope.md`

`ReviewSessionRepository`:
- `findAllByAxisId(axisId: Long): List<ReviewSession>` — `WHERE scope='AXIS' AND scope_id=?`
- `findAllByLayerId(layerId: Long): List<ReviewSession>` — `WHERE scope='LAYER' AND scope_id=?`

QueryDSL Predicates 또는 JPA method-name query.

### 완료 기준 (AC)

- Given axis 세션 3건 · layer 세션 2건 / When `findAllByAxisId(A)` / Then 3건 반환
- Given `findAllByLayerId(L)` / When / Then 2건 반환

### Definition of Done

- [ ] Repository 메서드
- [ ] Repository Slice 테스트

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 6-1
- 후행: 없음

---

## [Story 6-3] `POST /layers/{layerId}/review-sessions` 엔드포인트

### User Story

- As a 학습 사용자
- I want Layer 스코프 리뷰 세션을 시작하기를
- so that 그룹핑된 여러 축의 카드를 한 세션에서 훑을 수 있다

### 설명

> 출처: `issue-14-review-strategy-axis-layer-scope.md`

엔드포인트:
- `POST /axes/{axisId}/review-sessions` — 기존 유지
- `POST /layers/{layerId}/review-sessions` — 신설
- `GET /review-sessions/{id}` — scope 필드 포함 응답

Application Service `ReviewCommandService.startLayerReview(userId, layerId)`:
1. Layer 소유권 확인
2. Layer 소속 axis 목록 조회
3. axis 0건 시 `LAYER_HAS_NO_AXES`
4. `Card.findByAxisIds(axisIds, ON_FIELD)` 로 카드 통합
5. `ReviewSession.ofLayer(layerId, cards)` 생성

### 완료 기준 (AC)

- Given layer 에 axis 3건, 각각 카드 [3, 2, 4] / When `POST /layers/{id}/review-sessions` / Then 세션 생성, cards.size()=9
- Given axis 0건 layer / When 요청 / Then 400 `LAYER_HAS_NO_AXES`
- Given 다른 유저의 layer / When 요청 / Then 403 `LAYER_REVIEW_ACCESS_DENIED`
- Given axis 세션 시작 (기존 흐름) / When `POST /axes/{id}/review-sessions` / Then scope=AXIS

### Definition of Done

- [ ] Controller
- [ ] Command Service
- [ ] Controller Slice + 통합 테스트

### 스토리 포인트

2d

### 의존성

- 선행: Story 6-1, 6-2
- 후행: Story 6-4

---

## [Story 6-4] `Layer.progressStatus` 파생 로직

### User Story

- As a 학습 사용자
- I want Layer 응답에 진행 상태가 포함되기를
- so that Layer 단위 진행률을 UI 에 표시할 수 있다

### 설명

> 출처: `issue-14-review-strategy-axis-layer-scope.md`

Layer 는 자체 `progressStatus` 컬럼 없음. 도메인 파생 메서드 `Layer.progressStatus(): LayerProgressStatus`. 하위 axis progressStatus 상태 3-state 를 다음 규칙으로 집계:

- 모두 NOT_STARTED → LayerProgressStatus.NOT_STARTED
- 모두 COMPLETED → LayerProgressStatus.COMPLETED
- 그 외(혼재 · IN_PROGRESS 존재) → LayerProgressStatus.IN_PROGRESS
- 축 0건 → LayerProgressStatus.NOT_STARTED (열린 질문 10 잠정 확정)

### 완료 기준 (AC)

- Given axis [NOT_STARTED, NOT_STARTED] / When `layer.progressStatus()` / Then NOT_STARTED
- Given axis [IN_PROGRESS, COMPLETED] / When / Then IN_PROGRESS
- Given axis [COMPLETED, COMPLETED, COMPLETED] / When / Then COMPLETED
- Given axis 0건 / When / Then NOT_STARTED

### Definition of Done

- [ ] 도메인 파생 메서드
- [ ] 단위 테스트 (4개 케이스 최소)
- [ ] Layer 응답 DTO 에 progressStatus 포함

### 스토리 포인트

1d

### 의존성

- 선행: Story 5-1 (Axis progressStatus 이전)
- 후행: 없음

---

## [Story 6-5] FE 용어 "Layer 1" 대체 명명

### User Story

- As a UX 라이터
- I want "Layer 1" 이라는 모호한 라벨이 명시적 이름으로 대체되기를
- so that 사용자가 리뷰 스코프를 즉시 이해한다

### 설명

> 출처: `issue-14-review-strategy-axis-layer-scope.md`, ADR022 열린 질문

- API 응답의 `scope: "LAYER"` 는 그대로 유지 (기술 필드)
- UI 라벨은 다음 매핑 확정:
  - `scope=AXIS` → "축 리뷰: {axisName}"
  - `scope=LAYER` → "그룹 리뷰: {layerName}"
- `docs/ux/wip-language.md` 갱신: 위 매핑 명시

### 완료 기준 (AC)

- Given docs/ux/wip-language.md / When 조회 / Then AXIS/LAYER 라벨 매핑 정의
- Given FE 리뷰 세션 상세 페이지 / When scope=LAYER / Then "그룹 리뷰" 라벨 표시

### Definition of Done

- [ ] docs/ux/wip-language.md 갱신
- [ ] FE 매핑 반영은 Task 밖 (본 SDD 는 백엔드 스코프)

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 6-3
- 후행: 없음

---

*작성일: 2026-07-01 | 상태: **6 Epic · 30 Story 전체 pending** | Milestone 후보: 본 Product 를 milestone 1 로 확정 예정. Epic 순차 진행, Epic 1~2 완료 시 중간 review 세션.*
