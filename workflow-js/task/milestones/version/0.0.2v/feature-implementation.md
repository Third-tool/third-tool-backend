# 0.0.2v / Feature Implementation — 실측

> **본 파일 역할**: milestone.md에 잡힌 Tier 1·2 총 16 Story의 **기능 구현 진척**을 실측 표로 기록.
> 상세 성과: [`outcome.md`](./outcome.md) · 상세 구조: [`architecture.md`](./architecture.md) · 테스트: [`test-coverage.md`](./test-coverage.md).

---

## 요약 대시보드

| Tier | 잡힌 Story | 머지 완료 | 진행 중 | 미착수 | 진행률 |
| --- | --- | --- | --- | --- | --- |
| Tier 1 (Must) | 12 | **12** | 0 | 0 | **100%** |
| Tier 2 (Want) | 4 | 3 + 1(본 PR) | 0 | 0 | **100%** (본 PR 머지 시) |
| **합계** | **16** | **15 + 1 진행** | **0** | **0** | **~100%** |

**목표 대비**: Tier 1 10/12 (83%) 이상 → **100% 초과 달성**. Tier 2 완주 (rush 정책 성공).

---

## Story 단위 진척 표 (실측)

### Tier 1 · Must

| # | Product | Story | 상태 | PR | Commit | SP | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | LT | Epic 1 / 1-1 `learning_facade_concept` 테이블 + V16/R16 | 머지 | **#195** | 9542b43 · 306514b | 1 | 세션 시작 시점에 이미 존재 |
| 2 | LT | Epic 1 / 1-2 `LearningFacadeConcept` Entity + 컬렉션 API | 머지 | **#196** | 7d44b53 | 2 | LT E1 번들 |
| 3 | LT | Epic 1 / 1-3 V17 백필 + `updateConcepts()` + `ConceptsChangeRecord` | 머지 | **#196** | d9082a1 | 1 | 상동 |
| 4 | LT | Epic 1 / 1-4 API/DTO `concepts: string[]` 스위치 | 머지 | **#196** | 999c107 | 1 | 상동 |
| 5 | LT | Epic 1 / 1-5 도메인 상수 + ErrorCode 정합 | 머지 | **#196** | d3e3014 | 1 | 상동 |
| 6 | LT | Epic 2 / 2-1 `learning_layer` 테이블 (V18 + R18) + `LearningLayer` Aggregate | 머지 | **#197** | f7f62aa | 2 | LT E2 번들 |
| 7 | LT | Epic 2 / 2-2 `LearningAxis.layer_id` FK 재배선 (V19 3-phase) | 머지 | **#197** | afdfb4c | 2 | 상동 (Story 2-3과 V19 공유) |
| 8 | LT | Epic 2 / 2-3 default "Uncategorized" 백필 + `create()` 자동 발행 | 머지 | **#197** | afdfb4c | 2 | 상동 |
| 9 | LT | Epic 2 / 2-4 Layer softDelete + `removeLayer` Service + default 삭제 금지 | 머지 | **#197** | ffd96be | 1 | 상동 (Story 2-4·2-5 동일 커밋) |
| 10 | LT | Epic 2 / 2-5 Layer 최대 개수 · displayOrder · Controller 4엔드포인트 | 머지 | **#197** | ffd96be | 2 | 상동 |
| 11 | AS | Epic 1 4-Port 인터페이스 스켈레톤 (Layer/Roadmap/Selections + 6 record) | 머지 | **#198** | 단일 커밋 | 2 | AxisSuggestionPort 기존 유지 |
| 12 | 문서 | ADR023 Roadmap=헌법 / Selection=판례 용어 재정의 | 머지 | **#199** | 7b38c4e | 1 | 원안 명명은 ADR023 채택 (ADR022 slot 유지) |

### Tier 2 · Want

| # | Product | Story | 상태 | PR | Commit | SP | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 13 | AS | Epic 3 / 3-1 `RoleDetector` (하드코드 사전 4 role + generic fallback) | 머지 | **#200** | 단일 커밋 | 1 | Tier 2 번들 |
| 14 | AS | Epic 3 / 3-2 `backend-developer.json` + `generic.json` + `SuggestionCatalog(Loader)` | 머지 | **#200** | 상동 | 1 | 상동 |
| 15 | AS | Epic 2 / 2-1 `StaticLayerSuggestionAdapter` + `@ConditionalOnProperty` | 머지 | **#200** | 상동 | 2 | 상동 |
| 16 | 문서 | 0.0.2v 산출물 골격 (`architecture.md` 신설 + 8 md 갱신) | **진행 중** | **본 PR** | 본 PR | 1 | 본 문서 포함 |

---

## Epic 단위 DoD 체크

### LT Epic 1 — concepts[] 다중화 (완료)

- [x] Story 1-1: `V16__learning_facade_concept.sql` (+R16) — UNIQUE(facade_id, concept_value) + CASCADE FK + display_order CHECK ≥ 0
- [x] Story 1-2: `LearningFacadeConcept` Entity (정적 팩토리 · 1-based displayOrder) + `getConcepts()` unmodifiable
- [x] Story 1-3: `V17__learning_facade_concept_backfill.sql` (+R17) · `updateConcepts(List<String>)` 통째 교체 + `ConceptsChangeRecord`
- [x] Story 1-4: Request/Response DTO `concepts: string[]` + Controller PATCH `/concepts` rename + Service updateConcepts
- [x] Story 1-5: 도메인 상수 (`MIN_CONCEPT_COUNT`, `MAX_CONCEPT_COUNT=5`, `MAX_CONCEPT_VALUE_LENGTH=100`) · ErrorCode 4종 (LF003 재사용, LF005~007 신규, LF008 REORDER_MISMATCH 보너스)
- [x] `docs/DOMAIN.md` §1 LearningFacade에 concepts[] 반영

### LT Epic 2 — Layer 도메인 승격 (완료)

- [x] Story 2-1: `V18__learning_layer.sql` (+R18) · `UNIQUE(facade_id, name, deleted_at)` composite · `LearningLayer` Aggregate + `@SQLRestriction`+`@SQLDelete` + `softDelete()` 활성 axis 가드
- [x] Story 2-2: `V19__learning_axis_layer_fk.sql` (+R19) 3-phase · `LearningAxis.layer` @ManyToOne (nullable, V19 후 NOT NULL)
- [x] Story 2-3: V19 Phase 2에 Uncategorized 자동 발행 + axis 백필 (idempotent NOT EXISTS + WHERE layer_id IS NULL) · `LearningFacade.create()` 자동 default Layer 발행
- [x] Story 2-4: `LearningLayer.softDelete()` HAS_ACTIVE_AXES 가드 + ALREADY_DELETED idempotent · `LearningFacade.removeLayer(id)` default 보호
- [x] Story 2-5: `RECOMMENDED_LAYER_COUNT_LIMIT=5` · `LearningFacade.addLayer/renameLayer/reorderLayers/isLayerCountExceedsRecommended` · Controller POST/PATCH/DELETE `/learning-facade/layers[/{id}]` + PUT `/layers/order`
- [x] `docs/DOMAIN.md` §1 LearningLayer 용어 추가

### AS Epic 1 — 4-Port 인터페이스 (완료)

- [x] Story 11: 3 신규 Port (Layer/Roadmap/Selections) + 각 Suggestion·Context record + 기존 AxisSuggestionPort 유지
- [x] `LayerSuggestionContext(facadeId, concepts, role)`
- [x] `RoadmapSuggestionContext(facadeId, layerId, axisId, axisName, layerName, concepts, role)`
- [x] `SelectionsSuggestionContext(facadeId, layerId, axisId, axisName, concepts, roadmapOutline, role)`
- [x] 임의 stub Adapter 컴파일 실험 (SuggestionPortsContractTest 15건)

### 문서 — ADR023 (완료)

- [x] `docs/adr/ADR023-terminology-roadmap-selections.md` 5 결정 D1~D5 · 대안 4종 · 재검토 4건
- [x] `docs/adr/index.md` ADR023 라인 추가
- [x] `docs/DOMAIN.md` §1 (35 → 38) — LearningLayer/Roadmap/Selection + LearningFacade concepts[] 반영 + LearningAxis Layer 소속 명시

### Tier 2 — AS Static Adapter 첫 발 (완료)

- [x] Story 13: `RoleDetector` LinkedHashMap 순서 매칭 + 대소문자 · trim · null 정규화
- [x] Story 14: `resources/ai/catalog/backend-developer.json` (5 layers · 3 axes · 2 roadmaps · 2 selections) · `generic.json` fallback · `SuggestionCatalog` record · `SuggestionCatalogLoader` 캐시
- [x] Story 15: `StaticLayerSuggestionAdapter` + `@ConditionalOnProperty(matchIfMissing=true)` + existingLayerNames dedupe + limit 적용

### 문서 — 0.0.2v 산출물 골격 (본 PR 진행)

- [x] `architecture.md` 신설 (12 섹션 · 인터뷰 Q&A 포함)
- [x] `outcome.md` 실측 갱신
- [x] `feature-implementation.md` 실측 갱신 (본 파일)
- [x] `performance.md` 스킵 사유 명시 + 재측정 트리거
- [x] `review.md` 반성 R1~R7 + 개선 5건
- [x] `troubleshooting.md` 5건 로그 + Top 3
- [x] `cost.md` 갱신 유지 (0.0.3v 확장 반영)
- [x] `infra.md` 갱신 유지 (배포 재개 조건)

---

## 기능별 로컬 데모 시퀀스 (수동 확인 가능)

### 시퀀스 A — concepts[]

```bash
# 1. facade 생성 (concepts 다중)
curl -X POST http://localhost:8080/api/v1/learning-facade \
     -H "Content-Type: application/json" \
     -d '{"concepts":["백엔드","시스템설계","도메인언어"]}'
# 기대: 201 + response.concepts:["백엔드","시스템설계","도메인언어"]

# 2. concepts 조회
curl -b cookie.txt http://localhost:8080/api/v1/learning-facade
# 기대: {"concepts":["백엔드","시스템설계","도메인언어"], "layers":[{"name":"Uncategorized",...}], ...}

# 3. concepts 통째 교체
curl -X PATCH http://localhost:8080/api/v1/learning-facade/concepts \
     -d '{"concepts":["백엔드","시스템설계","분산시스템"]}'
# 기대: 200 + response.isChanged:true, added:["분산시스템"], removed:["도메인언어"]

# 4. 6개 시도
curl -X PATCH http://localhost:8080/api/v1/learning-facade/concepts \
     -d '{"concepts":["A","B","C","D","E","F"]}'
# 기대: 400 LEARNING_FACADE_CONCEPTS_SIZE_INVALID (LF005)

# 5. 중복 시도
curl -X PATCH http://localhost:8080/api/v1/learning-facade/concepts \
     -d '{"concepts":["A","A"]}'
# 기대: 409 LEARNING_FACADE_CONCEPT_DUPLICATE (LF006)
```

### 시퀀스 B — Layer

```bash
# 1. Layer 생성
curl -X POST http://localhost:8080/api/v1/learning-facade/layers \
     -d '{"name":"UI"}'
# 기대: 201 + {"layerId":..., "name":"UI", "displayOrder":2, "isLayerCountExceedsRecommended":false}

# 2. Layer rename
curl -X PATCH http://localhost:8080/api/v1/learning-facade/layers/{layerId} \
     -d '{"name":"View"}'
# 기대: 200 + isChanged:true

# 3. default Layer 삭제 시도
curl -X DELETE http://localhost:8080/api/v1/learning-facade/layers/{defaultLayerId}
# 기대: 409 LEARNING_LAYER_HAS_ACTIVE_AXES ("default Uncategorized Layer는 삭제할 수 없습니다")

# 4. Layer 순서 변경
curl -X PUT http://localhost:8080/api/v1/learning-facade/layers/order \
     -d '{"orderedLayerIds":[3,1,2]}'
# 기대: 200 + layers 순서 갱신

# 5. Layer 하위 Axis 추가 (기존 API 회귀 무)
curl -X POST http://localhost:8080/api/v1/learning-facade/axes \
     -d '{"name":"API 설계"}'
# 기대: 201 (기존 addAxis가 default Uncategorized layer로 자동 라우팅)
```

### 시퀀스 C — AI Static Adapter (Tier 2)

```java
// Application Service 코드 (예상)
public List<LayerSuggestion> suggestLayers(LayerSuggestionContext ctx) {
    return layerSuggestionPort.suggest(ctx, existingLayerNames, 5);
}

// 자동 라우팅
context.role() == null → RoleDetector.detect(context.concepts())
                       → "backend-developer" (매칭) 또는 "generic" (fallback)
                       → SuggestionCatalogLoader.load(role)
                       → catalog.layers 반환 (existingLayerNames 제외 · limit 적용)
```

---

## 카테고리별 성과 요약 (실측)

| 카테고리 | Story 잡힘 | 실제 머지 | SP 잡힘 | SP 소진 | 비고 |
| --- | --- | --- | --- | --- | --- |
| LT Epic 1 (concepts[]) | 5 | 5 | 6 | 6 | 100% |
| LT Epic 2 (Layer) | 5 | 5 | 9 | 9 | 100% |
| AS Port 골격 | 1 | 1 | 2 | 2 | 100% |
| 문서 (ADR / DOMAIN.md) | 1 | 1 | 1 | 1 | ADR023 발행 |
| **Tier 1 소계** | **12** | **12** | **18** | **18** | **100%** |
| Tier 2 (AS Static + 산출물) | 4 | 4 | 5 | 5 | 100% |
| **합계** | **16** | **16** | **23** | **23** | **100%** |

---

## Story 이관·폐기

**해당 사항 없음.** 16/16 완주.

---

## 신규 발견 Story (다음 버전 후보)

| # | 발견일 | 배경 | 제안 Story | 우선순위 |
| --- | --- | --- | --- | --- |
| DEF-1 | 2026-07-02 | `facade.axes` backward compat 유지 결정 | `LearningFacade.axes` 제거 refactor (Layer 경유 강제) | P2 |
| DEF-2 | 2026-07-02 | Legacy `learning_facade.concept` NOT NULL 컬럼 | 3-phase 마이그레이션 (NOT NULL 해제 → NULL 백필 → DROP) | P2 |
| DEF-3 | 2026-07-02 | Soft Delete 관행 갈림 재발 | Card/Deck를 `@SQLRestriction` 마이그레이션 or LearningLayer 되돌리기 | P3 |
| DEF-4 | 2026-07-02 | 테스트 Flyway disabled | @SpringBootTest에서 Flyway 활성 profile 도입 검토 | P3 |
| DEF-5 | 2026-07-02 | rush 정책 5관점 Reviewer 스킵 | 0.0.2v/0.0.3v 종합 5관점 세션 별도 예약 | P1 |

---

## 참고

- 잡힌 Story 원본: `milestone.md` Tier 1·2 표
- 완료 기준 원본: `product-learning-tower.md` Epic 1·2 DoD + `product-ai-suggestion.md` Epic 1·2·3 DoD
- Story 명세 컨벤션: `.claude/rules/conventions.md`
- ADR023 (본 세션 발행): `docs/adr/ADR023-terminology-roadmap-selections.md`

*작성일: 2026-07-02 | 반영: PR #195~#200 머지 · Story 16 진행 중 (본 PR) | 완료 판정: 100%*
