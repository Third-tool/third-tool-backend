# Issue: AI Roadmap/Selection 생성 flow — outline → 챕터별 subtree 2단계 (6-Port로 확장)

## 배경
사용자 지시 (2026-07-02 fix 회의):
> "A: 곳개 목록 먼저 → 병렬 채우기 (추천)"
> "한 노드의 단위가 [챕터 subtree ASCII 통짜] 이 정도로 각각 아스키 코드의 형태로 추천해주는 것을 ai 프롬프트로도 박고 싶습니다."

이슈 #9의 4-Port(Layer / Axis / Roadmap / Selection) 중 `RoadmapSuggestionPort`·`SelectionsSuggestionPort`는 **한 번에 axis 전체 트리 통짜 생성** 방식이었다. 사용자 진단에 따르면 이 one-shot 방식이 다음 세 부족을 낳음:
- 분량 과대 (통짜 트리 방대)
- 중간 수정 어려움
- 사용자 의견 반영 통로 부재

**목표**: outline(챕터 목록만) → 챕터별 subtree(1-1~1-N + 리프 본문 통짜)를 병렬로 채우는 **flow A** 도입. 사용자가 outline 단계에서 챕터 리스트를 미리 편집/거절 가능 → 분량 제어. 챕터별 재생성(#18)로 부분 수정·의견 반영 커버.

## 조사 결과 — 이슈 #9의 4-Port 현재 상태

| Port | 현재 시그니처 요약 |
|---|---|
| `LayerSuggestionPort` | concepts[] → layer 후보 |
| `AxisSuggestionPort` | layer + concepts → axis 후보 |
| `RoadmapSuggestionPort` | axis + concepts + layer → **ASCII 트리 헌법 통짜** |
| `SelectionsSuggestionPort` | axis + roadmap content 필수 + variantHint → **name 후보 + ASCII 트리 판례 통짜** |

flow A는 Roadmap/Selection Port를 각각 outline + subtree 두 단계로 쪼갬 → **6-Port**.

## 옵션 비교

**Option A — 6-Port로 확장, outline + subtree 2단계 flow (채택)**
- Layer, Axis 유지.
- Roadmap: `ChaptersOutlinePort` + `ChapterSubtreePort` 분리.
- Selection: `SelectionOutlinePort` + `SelectionSubtreePort` 분리.
- outline은 챕터 제목·rationale 리스트만. 사용자 승인 후 subtree를 병렬로 채움.

**Option B — 4-Port 유지, 프롬프트로만 flow 제어**
- Port 시그니처는 그대로 두고 프롬프트에서 "outline만" / "subtree만" 지시.
- Adapter 코드가 파라미터로 flow 분기 → Port 인터페이스가 flow-agnostic해지지만, 각 단계별 static fallback 구성이 어려움.

**Option C — 챕터 하나씩 사용자 액션으로 추가 (flow B)**
- Port 분리 없이 subtree Port 하나만. 사용자가 매번 "다음 챕터" 액션.
- 통제 최대지만 응집력(전체 챕터가 하나의 계획 하에서 뽑히는 것) 손실.

## 선택: Option A (6-Port + flow A)

## 부속 결정

### 6-Port 시그니처 (신)

```java
interface LayerSuggestionPort { /* 유지 (이슈 #9 그대로) */ }
interface AxisSuggestionPort   { /* 유지 (이슈 #9 그대로) */ }

interface ChaptersOutlinePort {
  ChaptersOutline suggest(ChaptersOutlineRequest req);
  // req: concepts[], layerName, axisName, axisReason, chapterCountHint?, freeformHint?
  // returns: List<ChapterOutlineItem(title, rationale)> — body 없음
}

interface ChapterSubtreePort {
  ChapterSubtree suggest(ChapterSubtreeRequest req);
  // req: concepts[], layerName, axisName, chapter(title, rationale), siblingChapters(title 만)
  // returns: bodyAsciiTree (챕터 subtree 통짜, 사용자 예시 하네스 로드맵 형태)
}

interface SelectionOutlinePort {
  SelectionOutline suggest(SelectionOutlineRequest req);
  // req: concepts[], layerName, axisName, roadmapContent(챕터 전체 요약), variantHint, chapterCountHint?
  // returns: nameCandidate + List<ChapterOutlineItem>
}

interface SelectionSubtreePort {
  ChapterSubtree suggest(SelectionSubtreeRequest req);
  // req: 동일 + selectionName + selectionSiblings
  // returns: bodyAsciiTree
}
```

### 프롬프트 템플릿 embed 규칙 (이슈 #19와 연동)

각 Port 시스템 프롬프트 상단에 다음을 **static asset**으로 embed:

1. **roadmap/selections 개념 명세** ([이슈 #19](./issue-19-roadmap-selection-concept-spec.md))
   - 콘텐츠 카탈로그 (roadmap 6종 / selections 5종)
   - 수렴/발산 판별 기준 (각 5개)
2. **few-shot example** — 사용자 예시(하네스 엔지니어링 심화 로드맵 챕터 subtree 형태)
3. **Port별 지시**:
   - roadmap Port(outline·subtree): "너는 수렴된 판단 프레임을 뽑는다. 도구 이름·특정 옵션 비교 금지. 발산 지식은 selections로 이관"
   - selections Port(outline·subtree): "너는 발산된 적용안을 뽑는다. 기준은 roadmap에 있다고 가정하고 그 기준을 굴리는 다양한 관점을 낸다"

### 저장 매핑

- `ChapterSubtreePort` / `SelectionSubtreePort`가 반환한 `bodyAsciiTree`는 파싱 없이 **body TEXT에 그대로 저장** (이슈 #15/#16의 body 필드).
- outline은 각 챕터의 `title`·`rationale`만 담아 사용자 승인 후 subtree Port 병렬 호출.
- 서버는 챕터 파싱·splice 최소화 — 사용자가 편집한 body는 사용자 원문 그대로 유지.

### 세션 상태 머신 확장 (product-ai-interactive-roadmap.md 개정)

```
INIT
  → LAYERS_DRAFTED (LayerSuggestionPort)
  → AXES_DRAFTED (AxisSuggestionPort, layer별)
  → CHAPTERS_DRAFTED (ChaptersOutlinePort, axis별) ← 신규 단계
  → SUBTREES_DRAFTED (ChapterSubtreePort 병렬, axis별) ← 신규 단계
  → REVIEWING
  → COMMITTED
```

Selection도 동일한 outline → subtree 2단계 (`SelectionOutlinePort` → `SelectionSubtreePort`).

### Static Adapter fallback

- 각 단계별 role-keyed static catalog:
  - `ChaptersOutlinePort` static: role별 하드코딩된 챕터 제목·rationale 리스트
  - `ChapterSubtreePort` static: role + 챕터 제목별 하드코딩된 subtree 텍스트 (또는 generic subtree template)
- LLM 실패 or 예산 소진 시 static으로 자동 폴백 (이슈 #20 로깅과 연동).
- 초기 3명 사용자 규모에서 LLM 장애 시에도 세션 진행이 끊기지 않도록 유지.

### rate limit / 세션 예산 (초기엔 미변경)

- 초기 세션 = (Layer 1 + Axis 3 + ChaptersOutline 3 + ChapterSubtree 15) ≈ 22회 호출.
- 이슈 #9의 rate limit 10rpm에 걸릴 가능성 있음 → 초기 3명 사용자 규모에서는 rate limit 재검토를 이슈 #20에서 backlog로만 기록. v1 개발 스코프에서 rate limit 상향 or 세션 예산 도입은 **하지 않음**.

## 이관 산출물

- **BE-Story #17-1**: `ChaptersOutlinePort` / `ChapterSubtreePort` / `SelectionOutlinePort` / `SelectionSubtreePort` 인터페이스 신설. 기존 `RoadmapSuggestionPort` / `SelectionsSuggestionPort` deprecate.
- **BE-Story #17-2**: 각 Port의 Static Adapter 구현 (role-keyed catalog).
- **BE-Story #17-3**: 각 Port의 LLM Adapter (Spring AI 기반) + 프롬프트 템플릿 리소스 배치 (`resources/prompts/*.txt`).
- **BE-Story #17-4**: 세션 상태 머신 확장 (`CHAPTERS_DRAFTED`, `SUBTREES_DRAFTED` 추가) + 각 단계 draft/confirm/refresh API.
- **BE-Story #17-5**: 세션 종료 시 도메인 매핑 — outline 각 챕터 → `axis_roadmap_node` row, subtree 결과 → 해당 노드의 body 필드.
- **FE-Story #17-6**: 세션 UI 흐름 확장 — outline 리스트 화면(승인·편집·거절) + 병렬 subtree 로딩 프로그레스.
- **SDD 개정** (이슈 #8): `product-ai-suggestion.md` 6-Port 확장분, `product-ai-interactive-roadmap.md` 세션 머신.

## 관련 이슈 / 문서

- 이전: [#9 AI 3층 확장](./issue-09-ai-suggestion-3layer.md) — 4-Port를 6-Port로 확장.
- 연동: [#15 Roadmap 노드 모델](./issue-15-roadmap-node-model.md), [#16 Selection 노드 모델](./issue-16-selection-node-model.md), [#18 노드 재생성 API](./issue-18-node-regeneration-with-hint.md), [#19 개념 명세](./issue-19-roadmap-selection-concept-spec.md), [#20 AI 비용 예산](./issue-20-ai-cost-budget-cap.md).
- SDD 개정: `product-ai-suggestion.md`, `product-ai-interactive-roadmap.md`.
