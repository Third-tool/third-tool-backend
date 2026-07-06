# Issue: AI 추천 3층 확장 — Layer / Axis / Roadmap / Selection 4-Port

## 배경
사용자 지시:
> "learningFacade 예시 백엔드 개발자, 기획자 이런 컨셉을 조합을 같이 고민하고 만들어진 조합을 통해서 어울리는 layer를 만들고 (...) 그 layer 하부는 그 layer topic을 커버하기 위한 (...) axis (...) 세부 로드맵들"

현재 aisuggestion은 **단발 `AxisSuggestionPort` 하나만** 존재한다. concepts[] → layer 조합 추천, layer → axis 추천, axis → roadmap 초안, roadmap → selection 초안까지 **4단계 세션 흐름**이 필요.

세션 축 결정은 이미 확정 — **세션 기반 (RoadmapInteractionSession 확장)**.

## 조사 결과 — 현재 AI 추천 상태

| 항목 | 현재 |
|---|---|
| Port | `AxisSuggestionPort` (concept 1개 → axis 후보) |
| 프롬프트 컨텍스트 | concept + composition reason + desired outcome + 기존 축 이름 |
| Static Fallback | 개발자 중심 소수 예시 |
| 세션 | `RoadmapInteractionSession` (문서상 초안-합의 flow) |
| Layer 추천 | 부재 |
| Roadmap 추천 | 부재 |
| Selection 추천 | 부재 |

## 옵션 비교

**Option A — 4개 Port로 분리 + 세션 오케스트레이션 (채택)**
- `LayerSuggestionPort` (concepts[] → layer 후보)
- `AxisSuggestionPort` (layer + concepts → axis 후보) — 기존 확장
- `RoadmapSuggestionPort` (axis + concepts + layer → ASCII 트리 헌법 초안)
- `SelectionsSuggestionPort` (axis + **roadmap content 필수** + variantHint → 판례 초안)
- 세션이 단계별 상태 관리, 각 Port는 단발 stateless 유지.

**Option B — 단일 만능 Port (`MapDraftPort`)**
- 인터페이스 단순하지만 프롬프트가 거대해지고 static fallback 구조 어려움. 각 단계 독립 테스트/캐싱 불가.

**Option C — 단발 API로 FE가 각각 호출**
- 세션 자산(단계 간 컨텍스트) 상실. 사용자가 이전 결정을 되짚어 수정할 때 서버가 몰라 프롬프트 컨텍스트 재구성이 매번 필요. 세션 축과 배치.

## 선택: Option A

## 부속 결정

### 4개 Port 시그니처 초안

```java
interface LayerSuggestionPort {
  List<LayerCandidate> suggest(LayerSuggestionRequest req);
  // req: concepts[], compositionReason, desiredOutcome, existingLayerNames
}

interface AxisSuggestionPort { // 기존 확장
  List<AxisCandidate> suggest(AxisSuggestionRequest req);
  // req: concepts[], layerName, layerReason, desiredOutcome, existingAxisNames
}

interface RoadmapSuggestionPort {
  RoadmapDraft draft(RoadmapDraftRequest req);
  // req: concepts[], layerName, axisName, axisReason
  // returns: ASCII 트리 텍스트
}

interface SelectionsSuggestionPort {
  SelectionDraft draft(SelectionDraftRequest req);
  // req: concepts[], layerName, axisName, roadmapContent (필수), variantHint (e.g. "능 아키텍처")
  // returns: name 후보 + ASCII 트리 텍스트
}
```

### 세션(RoadmapInteractionSession) 상태 머신
```
[INIT] → CONCEPTS_SET
CONCEPTS_SET → LAYER_DRAFTED (LayerSuggestionPort)
LAYER_DRAFTED → LAYER_CONFIRMED (사용자 편집·수락)
LAYER_CONFIRMED → AXIS_DRAFTED (AxisSuggestionPort, layer별로 반복 가능)
AXIS_DRAFTED → AXIS_CONFIRMED
AXIS_CONFIRMED → ROADMAP_DRAFTED (RoadmapSuggestionPort)
ROADMAP_DRAFTED → ROADMAP_SAVED
ROADMAP_SAVED → SELECTIONS_DRAFTED (선택, on-demand)
```

- 각 단계에서 사용자가 되돌아가 재추천 가능 (`refresh`).
- 최종 저장은 도메인 명령 (`LearningFacade.addLayer(...)` → `Layer.addAxis(...)` → `Axis.upsertRoadmap(...)` → `Axis.addSelection(...)`).

### Adapter 다중화
- **Static Adapter** (fallback): role-agnostic 카탈로그. 개발자/기획자/디자이너 예시 다각화([#10](./issue-10-role-catalog-expansion.md)와 연동).
- **LLM Adapter**: Spring AI 기반. 실패 시 Static으로 폴백.

### 캐싱·비용
- 각 단발 호출은 (input hash) 기준 캐싱 검토. Roadmap 초안은 axis 정체성이 유지되면 재사용 가능.
- 세션 상태는 서버 세션 저장(구체 매체는 SDD 단계에서 결정 — 초안: `roadmap_interaction_session` 테이블 또는 Redis).

## 이관 산출물

- **BE-Story #9-1**: 4개 Port 인터페이스 정의 + Static Adapter 구현 + LLM Adapter 스켈레톤.
- **BE-Story #9-2**: `RoadmapInteractionSession` Aggregate 확장 — 위 상태 머신 + 각 단계별 draft/confirm/refresh 행위.
- **BE-Story #9-3**: Session 저장소 결정 (RDB vs Redis) + 매핑.
- **BE-Story #9-4**: 세션 API — `POST /sessions` (concepts 입력), `POST /sessions/{id}/layers/draft`, `POST /sessions/{id}/layers/confirm`, `POST /sessions/{id}/axes/draft`, `POST /sessions/{id}/roadmap/draft`, `POST /sessions/{id}/selections/draft` 등.
- **BE-Story #9-5**: 세션 종료 시 도메인 저장 orchestration (facade.addLayer → layer.addAxis → axis.upsertRoadmap → axis.addSelection).
- **FE-Story #9-6**: 세션 UI 흐름 — 단계별 화면(concepts → layer 후보 → axis 후보 → roadmap 편집 → selection 추가).
- **SDD 개정** (#8-5): `product-aisuggestion.md` 4-Port 확장분 반영.

## 관련 이슈 / 문서

- 이전: [#4](./issue-04-concept-list.md), [#5](./issue-05-layer-server-domain.md), [#6](./issue-06-roadmap-selections-dualaxis.md).
- 관련: [#10 Role 확장](./issue-10-role-catalog-expansion.md) — Static Adapter 카탈로그.
- SDD 개정: [#8](./issue-08-terminology-redefinition-sdd.md).
