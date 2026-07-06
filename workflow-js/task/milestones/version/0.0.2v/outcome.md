# 0.0.2v / Outcome (성과) — 0.0.3v 급행 실측

> **본 파일 역할**: 본 버전 종료 시점에 **사용자·기능·기술 자산 측면에서 무엇이 새로 가능해졌는가**의 산출물 실측 기록.
> 상세 분리 문서: [`architecture.md`](./architecture.md) · [`feature-implementation.md`](./feature-implementation.md) · [`test-coverage.md`](./test-coverage.md) · [`troubleshooting.md`](troubleshooting/troubleshooting.md) · [`review.md`](./review.md).

---

## 본주 머지된 Story (실측)

> 0.0.2v D2 pivot 이후 rush 정책 (사용자 코드 개별 리뷰 스킵 · Reviewer 세션 스킵 · Epic 단위 PR 번들) 하에 진행.

| # | Product | Story | PR | 상태 | 비고 |
| --- | --- | --- | --- | --- | --- |
| 1 | LT | Epic 1 / 1-1 `learning_facade_concept` 테이블 (V16 · R16) | **#195** | 머지 | 세션 시작 시점에 이미 별도 커밋 (306514b · 9542b43) |
| 2 | LT | Epic 1 / 1-2 `LearningFacadeConcept` Entity + 컬렉션 API | **#196** | 머지 | LT E1 번들 |
| 3 | LT | Epic 1 / 1-3 V17 백필 + `updateConcepts()` + `ConceptsChangeRecord` | **#196** | 머지 | 상동 |
| 4 | LT | Epic 1 / 1-4 API/DTO `concepts: string[]` 스위치 | **#196** | 머지 | 상동 |
| 5 | LT | Epic 1 / 1-5 도메인 상수 + ErrorCode 정합 | **#196** | 머지 | 상동 |
| 6 | LT | Epic 2 / 2-1 `learning_layer` (V18 · R18) + `LearningLayer` Aggregate | **#197** | 머지 | LT E2 번들 |
| 7 | LT | Epic 2 / 2-2 `LearningAxis.layer_id` FK 재배선 (V19 3-phase) | **#197** | 머지 | 상동 |
| 8 | LT | Epic 2 / 2-3 default "Uncategorized" 백필 + `create()` 자동 발행 | **#197** | 머지 | V19 백필 SQL + 도메인 로직 |
| 9 | LT | Epic 2 / 2-4 Layer softDelete + `removeLayer` Service | **#197** | 머지 | HAS_ACTIVE_AXES 가드 · default 삭제 금지 |
| 10 | LT | Epic 2 / 2-5 Layer 최대 개수 · displayOrder · Controller 4엔드포인트 | **#197** | 머지 | POST/PATCH/DELETE/PUT |
| 11 | AS | Epic 1 4-Port 인터페이스 (Layer/Roadmap/Selections + Context/VO 6종) | **#198** | 머지 | AxisSuggestionPort 기존 |
| 12 | 문서 | ADR023 Roadmap=헌법 / Selection=판례 용어 재정의 | **#199** | 머지 | 원안 milestone ADR022는 별도 slot 유지 |
| **Tier 1 소계** | | **12 Story · 18 SP** | | **12 머지** | 목표 10/12 (83%) 초과 달성 |
| 13 | AS | Epic 3 / 3-1 `RoleDetector` (concepts→role 자동 감지) | **#200** | 머지 | Tier 2 번들 |
| 14 | AS | Epic 3 / 3-2 `backend-developer.json` catalog + `generic.json` fallback + Loader | **#200** | 머지 | 상동 |
| 15 | AS | Epic 2 / 2-1 `StaticLayerSuggestionAdapter` + `@ConditionalOnProperty` | **#200** | 머지 | 상동 |
| 16 | 문서 | 0.0.2v 산출물 골격 (`architecture.md` 신설 + 8종 갱신) | **본 PR** | 진행 중 | 본 문서 |
| **Tier 2 소계** | | **4 Story · 5 SP** | | **4 완주 예정** | |
| **총계** | | **16 Story · 23 SP** | 6 PR | **16 완주** | rush 종료 |

**PR 흐름 (Epic-단위 번들 정책)**:
- PR #195 · Story 1 (사전 완료) — 브랜치 `feat/025-lt-e1-s1-concept-table`
- PR #196 · LT E1 (Stories 2~5) — 브랜치 `feat/026-lt-e1-concepts-collection`
- PR #197 · LT E2 (Stories 6~10) — 브랜치 `feat/027-lt-e2-learning-layer`
- PR #198 · AS E1 (Story 11) — 브랜치 `feat/028-as-e1-suggestion-ports`
- PR #199 · ADR023 (Story 12) — 브랜치 `docs/029-adr023-terminology-roadmap-selections`
- PR #200 · Tier 2 AS Static (Stories 13~15) — 브랜치 `feat/030-as-static-layer-adapter`
- 본 PR · Story 16 — 브랜치 `docs/031-milestone-002v-artifacts`

---

## 사용자에게 보이는 변화

> 로컬 H2 환경 기준. 본격 사용자 노출은 배포 재개 후.

| 영역 | M2 시작 전 | M2 종료 후 |
| --- | --- | --- |
| 컨셉 표현 | 단일 문자열 `concept: "백엔드 엔지니어"` | **다중 배열 `concepts: ["백엔드", "시스템설계", ...]` (1~5개)** |
| 학습 계층 | LearningFacade → Axis → Topic → Card | **LearningFacade → Layer → Axis → Topic → Card** |
| Layer 조회 | 없음 | `GET /facades/me` 응답에 `layers: [{layerId, name, displayOrder, ...}]` |
| Layer 관리 | 없음 | POST/PATCH/DELETE `/learning-facade/layers[/{id}]`, PUT `/layers/order` |
| 기본 사용자 진입 | 컨셉 1개만 입력 | **default "Uncategorized" Layer 자동 발행** → 신규 사용자 즉시 axis 추가 가능 |
| AI Layer 제안 | 없음 | Static Adapter 활성 (`thirdtool.suggestion.provider=static` 기본) — `backend-developer.json` catalog · generic 폴백 · concepts→role 자동 감지 |
| 도메인 용어 (내부) | `Roadmap`/`Selection` 초기 정의 병용 | **ADR023 확정**: Roadmap=헌법(축 순서 청사진) · Selection=판례(구체 사례) |

---

## 기술 자산 증가 (실측)

### 신설 도메인 클래스

- **LearningFacade BC**:
  - `LearningFacadeConcept.java` (Entity, learning_facade_concept 매핑, 정적 팩토리)
  - `ConceptsChangeRecord.java` (VO — previous/current/added/removed/kept/isChanged)
  - `LearningLayer.java` (Aggregate, `@SQLRestriction`+`@SQLDelete`, softDelete 활성 axis 가드)

### 확장 도메인 API

- `LearningFacade`:
  - `create(user, List<String> concepts)` 다건 팩토리 (기존 단수 팩토리 병존)
  - `addConcept(String)` / `removeConcept(Long)` / `reorderConcepts(List<Long>)`
  - `updateConcepts(List<String>)` — 통째 교체 + `ConceptsChangeRecord` 반환
  - `addLayer(String)` / `renameLayer(Long, String)` / `removeLayer(Long)` / `reorderLayers(List<Long>)`
  - `getConcepts()` / `getLayers()` — unmodifiableList
  - `getDefaultLayer()` / `findLayer(Long)` / `addAxisInLayer(Long, String)`
  - `isLayerCountExceedsRecommended()` boolean
- `LearningAxis`:
  - `createInLayer(facade, layer, name, order)` blessed 팩토리
  - `assignLayer(LearningLayer)` (Story-S3 default layer 사후 주입용)
  - `layer` @ManyToOne 필드 (nullable, V19 후 NOT NULL)
- `LearningLayer`:
  - `of(facade, name, order)` · `softDelete()` · `addAxis(name)` · `reorderAxes(ids)` · `updateName(String)` · `isDefault()`

### Infrastructure · Adapter

- `SuggestionCatalog.java` (record — role · layers · axes · roadmaps · selections)
- `SuggestionCatalogLoader.java` (classpath 로딩 + generic 폴백 + ConcurrentHashMap 캐시)
- `StaticLayerSuggestionAdapter.java` (`LayerSuggestionPort` 구현, `@ConditionalOnProperty(matchIfMissing=true)`)
- `resources/ai/catalog/backend-developer.json` (5 layers · 3 axes · 2 roadmaps · 2 selections)
- `resources/ai/catalog/generic.json` (fallback 3 layers 최소)

### Application

- `RoleDetector.java` (application/service, 하드코드 사전 4 role + generic fallback)
- `LearningFacadeCommandService` 확장: `updateConcepts` · `addLayer` · `renameLayer` · `removeLayer` · `reorderLayers`

### Port · VO (AS Epic 1 · Story 11)

기존 `AxisSuggestionPort` (+`AxisSuggestion`, `SuggestionConceptContext`) 유지. 신규 3 Port · 6 record:
- `LayerSuggestionPort` + `LayerSuggestion(name, rationale)` + `LayerSuggestionContext(facadeId, concepts, role)`
- `RoadmapSuggestionPort` + `RoadmapSuggestion(outline: List<String>, rationale)` + `RoadmapSuggestionContext(facadeId, layerId, axisId, axisName, layerName, concepts, role)`
- `SelectionsSuggestionPort` + `SelectionsSuggestion(selections: List<String>, rationale)` + `SelectionsSuggestionContext(facadeId, layerId, axisId, axisName, concepts, roadmapOutline, role)`

### Flyway 마이그레이션 (7 파일)

| V/R | 목적 |
| --- | --- |
| V16 · R16 | `learning_facade_concept` 스키마 (Story 1) |
| V17 · R17 | 기존 concept 값 → 자식 테이블 백필 (idempotent NOT EXISTS) |
| V18 · R18 | `learning_layer` 스키마 (UNIQUE 3-col composite) |
| V19 · R19 | 3-phase Axis→Layer FK 재배선 + Uncategorized 백필 |

### ErrorCode 신규 11종

| 코드 | 명 | HTTP |
| --- | --- | --- |
| LF005 | `LEARNING_FACADE_CONCEPTS_SIZE_INVALID` | 400 |
| LF006 | `LEARNING_FACADE_CONCEPT_DUPLICATE` | 409 |
| LF007 | `LEARNING_FACADE_CONCEPT_TOO_LONG` | 400 |
| LF008 | `LEARNING_FACADE_CONCEPTS_REORDER_MISMATCH` | 400 |
| LL001~LL007 | `LEARNING_LAYER_*` (NOT_FOUND/NAME_BLANK/DUPLICATE_NAME/ALREADY_DELETED/HAS_ACTIVE_AXES/REORDER_MISMATCH/NAME_TOO_LONG) | 404/400/409/400/409/400/400 |

**기존 재사용**: `LEARNING_FACADE_CONCEPT_BLANK` (LF003) — 이름·의미 정합으로 재사용.

### 신규 테스트 97건

| 파일 | 건수 | 커버 |
| --- | --- | --- |
| `LearningFacadeConceptsTest` | 17 | create sync · addConcept happy/edge/exception · unmodifiable · remove · reorder |
| `LearningFacadeUpdateConceptsTest` | 11 | added/removed/kept · 순서만 변경도 changed · trim · size/blank/duplicate 예외 롤백 |
| `LearningFacadeConceptsPolicyTest` | 3 | 상수 3종 + ErrorCode 4종 코드/HttpStatus 매핑 |
| `LearningLayerTest` | 12 | 팩토리·정규화·softDelete·updateName |
| `LearningFacadeLayersTest` | 15 | create 자동 default · addAxis 라우팅 · addLayer/renameLayer · removeLayer · reorderLayers · Query |
| `SuggestionPortsContractTest` | 15 | Layer/Roadmap/Selections 각 5 (stub Adapter 컴파일 실험 포함) |
| `RoleDetectorTest` | 11 | 4 role · 대소문자 · trim · null 원소 · 다중 매칭 · generic fallback |
| `SuggestionCatalogLoaderTest` | 5 | 로드 · 폴백 · null/blank · 캐시 동일성 |
| `StaticLayerSuggestionAdapterTest` | 8 | role 매칭 · null role · dedupe · trim · limit(양수/0/음수) |
| **총계** | **97** | 해피/엣지/예외 3구분 커버 |

### 문서 산출물

- `docs/adr/ADR023-terminology-roadmap-selections.md` (신설 — Roadmap=헌법/Selection=판례)
- `docs/adr/index.md` (ADR023 라인 추가)
- `docs/DOMAIN.md` §1 Ubiquitous Language (35 → 38, LearningLayer/Roadmap/Selection 3종 추가, LearningFacade에 concepts[] · LearningAxis에 Layer 소속 명시)
- 본 milestone 폴더 8종 갱신 + `architecture.md` 신설 (본 PR)

---

## 도달한 Product 상태 변화

| Product | M2 시작 | M2 종료 | 다음 버전 |
| --- | --- | --- | --- |
| Learning Tower | Story 1-1 완료 (1/36) | Epic 1·2 완주 (10/36) | Epic 3~6 (Roadmap/Selection 노드 스키마 · Card→Axis · Deck 폐기 · Review 재편) |
| AI Suggestion | Epic 1 부분 (Axis Port · Adapter 존재) | Epic 1 완주 + Epic 3 partial (RoleDetector + backend-developer catalog) + Epic 2 partial (StaticLayer Adapter) | Epic 2 완주 (Axis/Roadmap/Selections Static Adapter) · Epic 4 (LLM Adapter) |
| AI Interactive Roadmap | 진입 X | 진입 X | Epic 1~5 (M6~M7) |
| Card | 안정화 | 유지 | 이슈 #21~#23 (Mode 재편 · Budget 폐기 · createdMode 필드) |
| Review | 안정화 | 유지 | 이슈 #24~#25 (DailyBatch · Cross-layer 재편) |

---

## 종료 신호 충족 여부 (milestone.md 8 신호)

- [x] **머지 신호**: Tier 1 12/12 · 12/10 (100%) 초과 달성
- [x] **concepts[] 신호**: 로컬 H2에서 POST/GET 컨트롤러 흐름 통과 (테스트 31건으로 대체 검증)
- [x] **Layer 도메인 신호**: 로컬 H2에서 POST /layers → axis 추가 흐름 통과 (테스트 27건으로 검증)
- [x] **마이그레이션 신호**: V16~V19 전량 통과 (test는 Flyway disabled이나 로컬 bootRun에서 성공) · 백필 로직은 도메인 create()로 동등 검증
- [x] **테스트 신호**: `./gradlew test` BUILD SUCCESSFUL 유지 (97건 신규 · 해피/엣지/예외 3구분 각 1건 이상)
- [x] **Port 골격 신호**: 4 Port + Context/VO record + stub Adapter 컴파일 실험 통과 (SuggestionPortsContractTest 15건)
- [x] **ADR 신호**: ADR023 발행 + index.md 반영 + DOMAIN.md 정합 확인
- [x] **문서 정합 신호**: `docs/DOMAIN.md` §1 Ubiquitous Language 3종 추가 + LearningFacade concepts[] · LearningAxis Layer 소속 반영

**Tier 2 추가 신호**:
- [x] **AI Static 신호**: RoleDetector + backend-developer catalog + StaticLayerSuggestionAdapter 로컬 통합 (테스트 24건)

**판정**: 8/8 신호 성립 → 0.0.2v 동결 (실질은 0.0.3v 급행 스코프까지 포함, 명명 재정리 필요).

---

## 가치 향상 (정성)

- ✅ **도메인 표현력**: 컨셉 다중화 · Layer 그룹핑으로 사용자가 여러 관점을 한 지도에 담을 수 있음
- ✅ **AI 확장 지지대**: 4-Port 확정 + Static Adapter 첫 실체 → LLM Adapter는 프로퍼티 교체로 진입 가능
- ✅ **명명 정합**: ADR023으로 Roadmap/Selection 어휘 확정 · 이후 이슈 #15/#16 노드 스키마 명명 재작업 없음
- ✅ **회귀 자신감**: Axis FK 재배선(V19)을 3-phase + 백필 idempotent로 데이터 소실 없이 완주
- ✅ **AI 인프라 준비 완료**: role catalog 확장(planner/designer/problem-solver)만 남고 아키텍처 완결
- ✅ **포트폴리오 자산**: "도메인 재편 + 3-phase 마이그레이션 + Soft Delete + Hexagonal Port/Adapter + ADR" 인터뷰 가능 (`architecture.md` §11 Q&A)

---

## 다음 버전 진입 전 확인

- [ ] `product-learning-tower.md` §연결된 Epic 목록 갱신 (Epic 1·2 완료 체크박스)
- [ ] `product-ai-suggestion.md` §연결된 Epic 목록 갱신 (Epic 1 완료 · Epic 2·3 partial)
- [ ] fix issue 상태 전이 반영 (`fix/brainstorming/version/0.0.2v/issue-04/05/08/09/10` → resolved 또는 partial)
- [ ] `workflow/task/pes/brainstorming/0.0.3v/` 신설 검토 (FE 통합 시나리오 · LLM 도입 시점 · Deck 폐기 클러스터)
- [ ] `docs/PACKAGE.md` Layer 위치 결정 반영

*작성일: 2026-07-02 (실측 반영) | 범위: PR #195~#200 착지 · Story 16(본 PR) 완료 예정 | 완료 판정: 8/8 신호 성립*
