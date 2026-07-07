# [Product] AI Suggestion — 무상태 6-Port 오케스트레이션 + Role Catalog

## Product Vision

> **AI 제안은 무상태 6-Port(`LayerSuggestionPort` / `AxisSuggestionPort` / `ChaptersOutlinePort` + `ChapterSubtreePort` / `SelectionOutlinePort` + `SelectionSubtreePort`) 로 정리되고, Static + LLM 두 어댑터를 동시에 지원한다.**
> Role Catalog 는 개발자 외 기획자·디자이너·문제해결자를 포함하도록 확장해, 사용자 컨셉에 맞춰 자동 폴백을 선택한다. AI 프롬프트는 roadmap/selections 개념 명세(카탈로그 6종·5종 + 수렴/발산 판별 기준)를 시스템 프롬프트로 embed해 개념 오염을 방지한다.

---

## 🔄 Fix 개정 (2026-07-02) — 이슈 #17/#19/#20 반영

**개정 사유**: 이슈 #9의 4-Port(Layer/Axis/Roadmap/Selection)에서 `RoadmapSuggestionPort`·`SelectionsSuggestionPort`가 **한 번에 axis 전체 트리 통짜 생성** 방식이었음. AI 품질 검증 결과 (1) 분량 과대 (2) 부분 수정 어려움 (3) 사용자 의견 반영 통로 부재 세 부족 확인. **Roadmap/Selection Port를 outline + subtree 2단계로 분리 → 4-Port → 6-Port**.

### 개정 범위 요약

| 영역 | 이전 (#9) | 개정 후 (#17) |
|---|---|---|
| Port 수 | 4-Port | **6-Port** |
| Roadmap 생성 | `RoadmapSuggestionPort.draft(axis, layer, concepts) → ASCII 통짜` | `ChaptersOutlinePort` (챕터 title·rationale 리스트만) → `ChapterSubtreePort` (챕터별 subtree ASCII 통짜 병렬) |
| Selection 생성 | `SelectionsSuggestionPort.draft(axis, roadmap, layer) → List` | `SelectionOutlinePort` (컨테이너 name + 챕터 outline) → `SelectionSubtreePort` (챕터별 subtree 병렬) |
| 프롬프트 embed | few-shot 정도 | **roadmap/selections 개념 명세 (콘텐츠 카탈로그 6종·5종 + 수렴/발산 판별 기준) + Port별 지시** static asset (`resources/prompts/concept-spec.txt`) — 이슈 #19 |
| Static Adapter | Port별 role catalog | outline/subtree 각 단계 모두 지원. LLM 실패 시 자동 폴백 (v1 유지) |
| Rate limit | 10rpm 확정 | 세션당 (outline 1 + subtree N) 호출 폭발 지점 존재. v1엔 재검토 안 함 (초기 3명 규모), 관찰 지표 로깅으로 근거 확보 (이슈 #20) |
| AI 비용 통제 | 미검토 | v1엔 관찰 지표 로깅만 (세션당 호출·토큰·재생성 횟수·hint 텍스트·fallback 발동), 예산 상한은 v2 backlog (이슈 #20) |

### 6-Port 시그니처 (신)

```java
interface LayerSuggestionPort { /* 유지 (이슈 #9) */ }
interface AxisSuggestionPort   { /* 유지 (이슈 #9) */ }

interface ChaptersOutlinePort {
  ChaptersOutline suggest(ChaptersOutlineRequest req);
  // req: concepts[], layerName, axisName, axisReason, chapterCountHint?, freeformHint?
  // returns: List<ChapterOutlineItem(title, rationale)> — body 없음
}

interface ChapterSubtreePort {
  ChapterSubtree suggest(ChapterSubtreeRequest req);
  // req: concepts[], layerName, axisName, chapter(title, rationale), siblingChapters
  // returns: bodyAsciiTree (챕터 subtree 통짜)
}

interface SelectionOutlinePort {
  SelectionOutline suggest(SelectionOutlineRequest req);
  // req: concepts[], layerName, axisName, roadmapContent(요약), variantHint, chapterCountHint?
  // returns: nameCandidate + List<ChapterOutlineItem>
}

interface SelectionSubtreePort {
  ChapterSubtree suggest(SelectionSubtreeRequest req);
  // req: 동일 + selectionName + selectionSiblings
  // returns: bodyAsciiTree
}
```

### 프롬프트 조립 규칙 (이슈 #19)

각 Port(4개 — outline·subtree) 시스템 프롬프트 상단:
1. `resources/prompts/concept-spec.txt` — roadmap 콘텐츠 카탈로그 6종 / selections 카탈로그 5종 / 수렴 판별 5개 / 발산 판별 5개 (압축 1줄씩)
2. Port별 지시:
   - roadmap Port(outline·subtree): "너는 수렴된 판단 프레임을 뽑는다. 도구 이름·특정 옵션 비교 금지. 발산 지식은 selections로 이관"
   - selections Port(outline·subtree): "너는 발산된 적용안을 뽑는다. 기준은 roadmap에 있다고 가정하고 그 기준을 굴리는 다양한 관점을 낸다"
3. few-shot example — 사용자 예시 (하네스 로드맵 챕터 subtree 형태)

### 관찰 지표 로깅 (v1, 이슈 #20)

세션·사용자 단위:
- 세션당 AI 호출 수 (Port별 분해)
- 세션당 총 토큰 (input/output/cached 분해)
- 세션당 재생성 횟수 (챕터별)
- 챕터 재생성 hint 텍스트 원문
- Static fallback 발동 비율
- Selection 축적 개수 (사용자별 월간)
- rate limit 접촉 여부

목적: v2 시점의 예산 상한 값·정책 세부 산정 근거 데이터 확보.

### Epic 상태

Epic 1(4-Port 인터페이스 정의) — SUPERSEDED. 이슈 #17의 6-Port로 재편성 예정.
Epic 3(Static Adapter role catalog) — 유효 (이슈 #10에서 확장). 다만 outline/subtree 각 단계 모두 지원하도록 catalog 재점검 필요.
Epic 4(LLM Adapter Vertex AI) — 6-Port 대응으로 프롬프트 리소스 4개(chapters-outline / chapter-subtree / selection-outline / selection-subtree) 재편 + `concept-spec.txt` include.
Epic 5(SuggestionCascade + Rate Limit) — Rate Limit 재검토는 이슈 #20으로 이관 (v1 미변경).

---

## 배경 및 문제

- 현재 상황 (As-Is)
  - 기존 `product-aisuggestion.md` 는 `AxisSuggestionPort` / `AxisTopicSuggestionPort` 두 Port 만 정의. Layer 도메인·Roadmap/Selection 이중 축이 도입되면 어댑터 인터페이스가 불충분.
  - Static Adapter 는 개발자 role 로 하드코딩된 fallback 데이터 하나만 제공. 기획자·디자이너·문제해결자 컨셉으로 진입하는 사용자에게 무의미한 fallback.
  - `AxisTopic` 이 `product-learning-tower.md` Epic 3 에서 폐기됨 → `AxisTopicSuggestionPort` 자체가 소멸.
  - LLM Adapter(Vertex AI Gemini) 는 아직 미구현. Spring AI 1.0 GA / Vertex AI Gemini Flash 2.5 인프라 결정만 완료.
- 발생하는 문제
  - `product-learning-tower.md` (신설) Layer/Roadmap/Selection 도입 이후 AI 제안 인터페이스가 4-Port 로 확장돼야 함. 기존 SDD 구조로 담기 어려움.
  - Static Adapter 의 role 하드코딩으로 개발자 아닌 사용자 대상 fallback 이 저품질.
  - 신규 milestone 을 찍기 전 AI Suggestion 재편 필요.
- 왜 지금 해결해야 하는가
  - `product-learning-tower.md` Epic 1~4 완료 후 Layer/Axis/Roadmap/Selection 이 존재하는 상태에서만 4-Port 인터페이스가 성립. learning-tower 진행과 병렬 설계 시점.
  - Spring AI 1.0 GA / Vertex AI Gemini Flash 2.5 인프라 준비 완료 → LLM Adapter 구현 착수 가능.
  - `product-ai-interactive-roadmap.md` (재작성 예정) 이 본 Product 의 4-Port 를 재사용해 세션 흐름을 구성 → 본 Product 가 선행 조건.

## 목표 (To-Be)

- **6-Port 오케스트레이션 도입 (2026-07-02 개정, 이슈 #17)**:
  - `LayerSuggestionPort` — `suggest(concepts[]) → List<LayerSuggestion>` (기본 3~5개)
  - `AxisSuggestionPort` — `suggest(layer, concepts[]) → List<AxisSuggestion>` (기본 4~6개)
  - `ChaptersOutlinePort` — `suggest(axis, layer, concepts[], chapterCountHint?, freeformHint?) → List<ChapterOutlineItem(title, rationale)>` (챕터 title·rationale 리스트만)
  - `ChapterSubtreePort` — `suggest(chapter, siblings, axis, layer, concepts[]) → bodyAsciiTree` (챕터 subtree ASCII 통짜)
  - `SelectionOutlinePort` — `suggest(axis, roadmapContent, variantHint, concepts[]) → nameCandidate + List<ChapterOutlineItem>`
  - `SelectionSubtreePort` — `suggest(chapter, selection, siblings, ...) → bodyAsciiTree`
  - **이전 `RoadmapSuggestionPort` / `SelectionsSuggestionPort` (이슈 #9 정의) 는 뒤집힘** — outline + subtree 2단계 flow로 대체.
- **Static + LLM 이중 어댑터**:
  - Static Adapter — 각 Port 별로 role 맞춤 fallback JSON 카탈로그.
  - LLM Adapter — Vertex AI Gemini Flash 2.5 (Spring AI ChatClient + BeanOutputConverter).
  - `thirdtool.suggestion.provider` (default=`static`) 로 어댑터 선택. `provider=llm` 시 실패하면 Static 폴백.
- **Role Catalog 확장** (#10):
  - 4 role: `backend-developer` / `planner` / `designer` / `problem-solver`
  - Role 자동 감지 — `concepts[]` 값에서 role hint 추출 (예: "백엔드" → backend-developer, "기획" → planner)
  - Role 미감지 시 default fallback (`generic`)
  - Static Adapter 는 role 별 JSON 카탈로그 참조 (`src/main/resources/ai/catalog/{role}.json`)
- **Fallback 원칙 유지** (ADR010) — LLM 실패 시 5xx 미노출. HTTP 200 + `suggestionsAvailable: false`.
- **Rate Limit 유지** — 유저당 분당 10회 (Bucket4j 등).
- **관측성** — Spring AI `gen_ai.*` 자동 메트릭 + 도메인 보강 `thirdtool.suggestion.*`.

## 설계 결정 (Design Decisions)

- **4-Port 무상태 오케스트레이션 (vs 단일 Port + 다형 요청)**
  - 단일 `SuggestionPort.suggest(kind, context)` 대안은 요청 kind 별 매개변수 매핑을 caller 가 책임짐 → 타입 안전성 훼손.
  - 4-Port 는 각각 강 타입 시그니처, Application Service 가 4-Port 를 조합해 caller 를 위한 오케스트레이션 API 제공.
  - 비용: 어댑터 4개를 구현·유지. 재사용 가능한 프롬프트 base 클래스 필요.
  - 보상: 각 Port 를 단독으로 재사용 가능(예: `product-ai-interactive-roadmap.md` 는 4-Port 중 필요한 것만 호출).
- **Static Adapter 를 1순위, LLM 을 2순위 (ADR010 유지)**
  - `thirdtool.suggestion.provider` default=`static`.
  - LLM 활성화는 명시적 `provider=llm` 설정 시만.
  - LLM 실패 시 Static 자동 폴백 (`SuggestionCascade` 패턴).
  - 비용: 운영 환경에서 provider=llm 설정 필요.
  - 보상: 로컬·테스트·인증 누락 시 자동 안전.
- **Role Catalog 확장 (#10)**
  - Role 자동 감지: `concepts[]` 값에서 keyword 매칭. 매칭 없으면 `generic`.
  - JSON 카탈로그 파일 위치: `src/main/resources/ai/catalog/{role}.json`
  - 카탈로그 구조: `{ layers: [...], axes: [...], roadmaps: {axisName → asciiTree}, selections: {axisName → [selectionName → content]} }`
  - 비용: 카탈로그 4개를 큐레이션·유지. 초기 데이터 품질 부담.
  - 보상: 개발자 아닌 사용자에게도 유의미한 fallback 제공. LLM 없이도 학습 시작 가능.
- **Vertex AI Gemini Flash 2.5 + GCP ADC 인증 (기존 product-aisuggestion 결정 이관)**
  - Google AI Studio 경로 폐기. Spring AI 1.0 first-class 지원.
  - GCP ADC (`gcloud auth application-default login`) 또는 Workload Identity.
  - 비용: GCP 셋업.
  - 보상: IAM 통합, 관측성 자동, 키 회전 관리 불필요.
- **Spring AI 1.0 표준 채택 (`ChatClient` + `PromptTemplate` + `BeanOutputConverter`)**
  - `.st` 프롬프트 파일. 각 Port 별 프롬프트 1개(총 4개).
  - `BeanOutputConverter<T>` 로 응답 record 자동 파싱.
  - 비용: Spring AI 1.0 GA에 묶임.
  - 보상: `gen_ai.*` 자동 메트릭 · Structured Output · Retry.
- **응답 캐싱 v1 미도입 (ADR010 관행 유지)**
  - v2 에서 캐시 키 설계 후 재검토.
- **ADR022 용어 준수** — Port/Adapter 명칭·응답 필드명 모두 `axisDraft` / `layerDraft` / `roadmapDraft` / `selectionsDraft` 표기.

## 대안 검토 (Alternatives Considered)

### 갈림길 A. Port 구조 (4-Port vs 1-Port)

**Option A — 단일 `SuggestionPort.suggest(kind, context)`**
- 장점: Port 파일 1개.
- 거부 이유: 요청/응답 유형이 kind 별로 다름 → 매개변수 매핑을 caller 가 책임. 타입 안전성 훼손.

**Option B (선택) — 4-Port 분리 (`LayerSuggestionPort` / `AxisSuggestionPort` / `RoadmapSuggestionPort` / `SelectionsSuggestionPort`)**
- 비용: Port 파일 4개, Adapter 파일 4·2=8개.
- 보상: 각 Port 강 타입, 독립 재사용 가능, 프롬프트도 분리.

**Option C — 2-Port (`StructureSuggestionPort` = Layer+Axis, `ContentSuggestionPort` = Roadmap+Selections)**
- 거부 이유: Layer 와 Axis 는 계층이 다르며 컨텍스트 요구도 다름(Axis 는 layer 컨텍스트 필수). 두 관심사 묶으면 프롬프트 복잡.

### 갈림길 B. Role 감지 방식

**Option A — 사용자 명시 role 선택 (UI)**
- 장점: 정확.
- 거부 이유: 첫 화면 진입 시 role 선택 UX 부담. 사용자 유입 저해.

**Option B (선택) — concepts[] 값에서 자동 감지, 미감지 시 generic**
- 비용: 감지 규칙 유지 필요. 애매한 concept 은 오분류 위험.
- 보상: 무선택 UX 유지. concept 값 자체가 사용자 의도의 자연 신호.

**Option C — LLM 에게 role 감지 위임**
- 거부 이유: fallback 자산이 필요한 시점(LLM 실패 시)에 LLM 이 없으면 role 감지도 실패.

### 갈림길 C. Fallback Cascade 순서

**Option A — LLM 만, 실패 시 500**
- 거부 이유: ADR010 위반. AI 는 보조.

**Option B (선택) — LLM 시도 → 실패 시 Static → 최종 실패 시 빈 목록 + suggestionsAvailable=false**
- 비용: Cascade 로직 구현.
- 보상: 사용자에게 최소 무언가 제공.

**Option C — 병렬 호출 후 유효성 검증**
- 거부 이유: LLM 비용 낭비. 병렬 응답 조합 로직 복잡.

### 갈림길 D. LLM Provider

**Option A — OpenAI GPT-4o**
- 장점: 시장 표준.
- 거부 이유: Spring AI 1.0 Vertex AI 가 first-class 이며 프로젝트가 GCP 생태계 지향.

**Option B (선택) — Vertex AI Gemini Flash 2.5 + ADC**
- 비용: GCP 셋업.
- 보상: IAM · 관측성 · Spring AI first-class.

**Option C — Anthropic Claude**
- 거부 이유: Spring AI 1.0 지원 미비. Vertex 위주 프로젝트 정책.

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 배치 (Hexagonal)

```
[React SPA]
    │
    │  POST /api/v1/suggestions/layers          { concepts[] }
    │  POST /api/v1/suggestions/axes            { layerId, concepts[] }
    │  POST /api/v1/suggestions/roadmaps        { axisId }
    │  POST /api/v1/suggestions/selections      { axisId }
    ▼
┌─ Presentation ─────────────────────────────────────────────────┐
│  SuggestionController                                           │
│   ├ POST /suggestions/layers                                    │
│   ├ POST /suggestions/axes                                      │
│   ├ POST /suggestions/roadmaps                                  │
│   └ POST /suggestions/selections                                │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─ Application ──────────────────────────────────────────────────┐
│  SuggestionCommandService                                       │
│   ├ suggestLayers(context) → LayerSuggestionPort               │
│   ├ suggestAxes(context) → AxisSuggestionPort                  │
│   ├ suggestRoadmap(context) → RoadmapSuggestionPort            │
│   ├ suggestSelections(context) → SelectionsSuggestionPort      │
│   ├ SuggestionCascade (LLM → Static → empty)                   │
│   └ RoleDetector (concepts → role)                             │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─ Ports (Domain-Facing Interfaces) ─────────────────────────────┐
│  ai/port/out/suggestion/                                        │
│   ├ LayerSuggestionPort                                         │
│   ├ AxisSuggestionPort                                          │
│   ├ RoadmapSuggestionPort                                       │
│   └ SelectionsSuggestionPort                                    │
└─────────────────────────────────────────────────────────────────┘
    │              │
    │              │ @ConditionalOnProperty(provider=static|llm)
    ▼              ▼
┌─ Static Adapter ─────────────────┐   ┌─ LLM Adapter (Vertex AI) ──────┐
│  4 Adapters, JSON catalog        │   │  4 Adapters + Spring AI         │
│   ├ StaticLayerSuggestionAdapter │   │   ├ LlmLayerSuggestionAdapter   │
│   ├ StaticAxisSuggestionAdapter  │   │   ├ LlmAxisSuggestionAdapter    │
│   ├ StaticRoadmapAdapter         │   │   ├ LlmRoadmapAdapter           │
│   └ StaticSelectionsAdapter      │   │   └ LlmSelectionsAdapter        │
│  카탈로그: resources/ai/catalog/{role}.json│   │   ChatClient + PromptTemplate  │
└──────────────────────────────────┘   │   BeanOutputConverter<T>        │
                                       │   Vertex AI Gemini Flash 2.5    │
                                       └──────────────────────────────────┘
                                                                │
                                                                ▼
                                                       [Vertex AI + ADC]
```

### 핵심 플로우

**1. Layer 제안 (사용자가 concepts 만 입력한 상태)**
```
Client ─POST /suggestions/layers {concepts:["백엔드","시스템설계"]}─▶ Controller
                                                                       └ SuggestionCommandService.suggestLayers()
                                                                           ├ RoleDetector → "backend-developer"
                                                                           ├ SuggestionCascade:
                                                                           │   ├ if provider=llm: LlmLayerAdapter.suggest(...)
                                                                           │   │     실패 시 Static 폴백
                                                                           │   └ if provider=static: StaticLayerAdapter.suggest(role="backend-developer", concepts)
                                                                           └ return SuggestionResponse
Client ◀ 200 { layers:[{name, rationale}, ...], suggestionsAvailable:true, providerContext:"static:backend-developer" } ─
```

**2. Axis 제안 (특정 Layer 하위)**
```
Client ─POST /suggestions/axes {layerId, concepts:[…]}─▶ Controller
                                                          └ Service.suggestAxes()
                                                              ├ LayerRepository.findById(layerId)  (컨텍스트 로드)
                                                              ├ RoleDetector
                                                              └ AxisSuggestionPort.suggest(layer, concepts)
Client ◀ 200 { axes:[{name, rationale, roadmapDraft:asciiTree}, ...], … } ─
```

**3. Roadmap 제안 (특정 Axis)**
```
Client ─POST /suggestions/roadmaps {axisId}─▶ Controller
                                              └ Service.suggestRoadmap()
                                                  ├ Axis 로드 (layer, concepts 컨텍스트 추적)
                                                  └ RoadmapSuggestionPort.suggest(axis, layer, concepts)
Client ◀ 200 { roadmapDraft:asciiTree, suggestionsAvailable:true } ─
```

**4. Selections 제안**
```
Client ─POST /suggestions/selections {axisId}─▶ Controller
                                                └ Service.suggestSelections()
                                                    ├ Axis + Roadmap 로드
                                                    └ SelectionsSuggestionPort.suggest(axis, roadmap, layer)
Client ◀ 200 { selections:[{name, content}, ...] } ─
```

### Out-of-Process 의존

- **Vertex AI Gemini Flash 2.5** — GCP ADC 인증. LLM Adapter 전용.
- **MySQL (LearningFacade / Layer / Axis 데이터)** — 컨텍스트 로드.
- **선행 Product**: `product-learning-tower.md` Epic 1~4 (concepts[], Layer, Roadmap/Selection, Card→Axis).
- **후행 Product**: `product-ai-interactive-roadmap.md` 이 본 Product 의 4-Port 를 세션 흐름에서 재사용.

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | ErrorCode | HTTP | 클라이언트 권장 동작 |
| --- | --- | --- | --- |
| LLM 응답 타임아웃 | (없음) — Static 폴백 자동 | 200 | `suggestionsAvailable: true` (Static 결과) 노출 |
| LLM 응답 파싱 실패 | (없음) — Static 폴백 자동 | 200 | 상동 |
| LLM + Static 모두 실패 | `AI_SUGGESTION_UNAVAILABLE` | 200 (본문 flag) | `suggestionsAvailable: false` 노출, 수동 입력 유도 |
| LLM rate limit (Vertex 429) | (없음) — Static 폴백 자동 | 200 | Static 결과 |
| 유저당 분당 10회 초과 | `AI_SUGGESTION_RATE_LIMITED` | 429 | Retry-After 헤더 |
| `concepts: []` 요청 | `SUGGESTION_CONCEPTS_REQUIRED` | 400 | 사용자 concepts 입력 유도 |
| `layerId` 존재하지 않음 (Axis 제안) | `LAYER_NOT_FOUND` | 404 | 새로고침 |
| `axisId` 존재하지 않음 (Roadmap/Selections 제안) | `AXIS_NOT_FOUND` | 404 | 새로고침 |
| GCP ADC 인증 실패 (startup) | `AI_PROVIDER_AUTH_FAILED` | (startup 실패 로그) | 운영자 대응 |
| provider=llm 설정 + LLM Bean 부재 | `AI_PROVIDER_MISCONFIGURED` | (startup 실패) | 설정 재확인 |

### 로깅 정책

- **항상 기록**:
  - Port 호출 (`port_type`, `provider`, `role`, `latency_ms`, `success`)
  - Fallback 발동 (`from=llm to=static`, `reason=timeout|parse_error|auth_error`)
  - Rate limit 발동 (`user_id`, `bucket=user_10rpm`)
- **debug**: 프롬프트 실제 값 (dev only, 마스킹된 concepts)
- **절대 금지**: LLM 응답 원문 · GCP 토큰 · 사용자 학습 내용 상세

### 관측 지표

- `thirdtool.suggestion.request_total{port, provider, role}` — 카운터
- `thirdtool.suggestion.fallback_total{from, to, reason}` — 카운터
- `thirdtool.suggestion.latency_seconds{port, provider}` — 히스토그램
- `thirdtool.suggestion.rate_limit_hit_total` — 카운터
- Spring AI 자동: `gen_ai.request.time_to_first_token` / `gen_ai.completion.tokens` / `gen_ai.prompt.tokens`

## 롤아웃 / 마이그레이션 (Rollout)

### 전제

- `product-learning-tower.md` Epic 1~4 완료 후 진입. Layer/Axis/Roadmap/Selection 이 존재하는 상태에서만 인터페이스 유효.
- 초기 배포는 `provider=static` 만. LLM 활성화는 별도 결정.

### Product 의존성

- **선행 Product**:
  - `product-learning-tower.md` Epic 1~4 (필수). Epic 5·6 은 무관.
  - `product-infra-ops.md` — GCP Secret Manager 로 ADC 자격 관리 (LLM Adapter 활성화 시).
- **후행 Product**:
  - `product-ai-interactive-roadmap.md` — 본 Product 의 4-Port 를 세션 흐름에서 호출.

### Epic·Story 의존성 그래프

```
Epic 1 (Ports & DTO)  ──▶ Epic 2 (Static Adapter 4개)
                            │
                            ▼
                          Epic 3 (Role Catalog)
                            │
                            ▼
                          Epic 4 (LLM Adapter 4개)
                            │
                            ▼
                          Epic 5 (SuggestionCascade + Rate Limit)
                            │
                            ▼
                          Epic 6 (관측성 + Runbook)
```

### 환경별 설정 분기

| 항목 | dev | prod (초기) | prod (LLM 활성화) |
| --- | --- | --- | --- |
| `thirdtool.suggestion.provider` | `static` | `static` | `llm` |
| GCP ADC | 미필요 | 미필요 | 필수 |
| Rate Limit (Bucket4j) | 관대(60rpm) | 10rpm | 10rpm |
| Spring AI 관측성 export | disabled | disabled | Prometheus scrape |
| Fallback Cascade | LLM→Static→empty | Static 전용 | 완전 Cascade |

## 성공 지표 (KPI)

| 지표 | 목표 값 | 측정 방법 |
| --- | --- | --- |
| Static Adapter 로 4-Port 모두 응답 성공률 | = 100% | 통합 테스트 |
| Role 자동 감지 (dev/planner/designer/problem-solver) 정확도 | ≥ 80% | 통합 테스트 · 실사용 관측 |
| LLM Adapter 활성화 후 응답 P95 | ≤ 8초 | Grafana 대시보드 (product-op 협력) |
| Fallback 발동 시 사용자 응답 성공률 | = 100% | 통합 테스트 |
| suggestionsAvailable=false 반환 비율 | ≤ 1% (LLM 활성화 시) | 관측 지표 |
| Rate Limit 오탐률 | 0 (정상 사용자 미차단) | 실사용 관측 |
| ErrorCode `AI_SUGGESTION_UNAVAILABLE` HTTP 500 노출 | = 0건 | 로그 |

## Scope

**In Scope**:
- 4-Port 인터페이스 정의 (Layer / Axis / Roadmap / Selections)
- Static Adapter 4개 + JSON 카탈로그 4개 (role 별)
- LLM Adapter 4개 (Vertex AI Gemini Flash 2.5, Spring AI ChatClient)
- SuggestionCascade (LLM → Static → empty)
- RoleDetector (concepts → role 자동 감지)
- Rate Limit (Bucket4j, 유저당 10rpm)
- REST API 4개 엔드포인트
- 관측성 (자동 + 도메인 보강)
- Fallback Runbook (docs/ 별도 문서로)

**Out of Scope**:
- 세션 상태기계 (`RoadmapInteractionSession`) — `product-ai-interactive-roadmap.md` 로 분리
- 응답 캐싱 (Redis) — v2 로 분리
- 사용자 피드백 학습 루프 (Phase 2+) — 본 Product 범위 외
- Streaming/SSE 응답 — 동기 응답 유지
- Layer / Axis / Roadmap / Selection 저장 로직 — `product-learning-tower.md` 도메인
- LLM 프롬프트 A/B 실험 프레임워크 — v2
- 다국어 프롬프트 — v1 은 한국어 중심

## 대상 사용자

- **학습자 (신규 진입)** — concepts 만 입력하면 Layer/Axis/Roadmap 초안 제안 받아 학습 시작 벽 낮춤.
- **학습자 (기존 지도 확장)** — 현재 Layer/Axis 컨텍스트에서 다음 확장 후보 추천.
- **AI Interactive Roadmap 세션 (`product-ai-interactive-roadmap.md`)** — 4-Port 를 세션 흐름에서 재사용.
- **운영자** — Fallback Runbook 으로 LLM 실패 시 대응.

## 연결된 Epic 목록

- [ ] Epic 1: 4-Port 인터페이스 + DTO 정의
- [ ] Epic 2: Static Adapter 4개 (역할 무관 기본 데이터)
- [ ] Epic 3: Role Catalog 4개 (backend/planner/designer/problem-solver) + RoleDetector
- [ ] Epic 4: LLM Adapter 4개 (Vertex AI Gemini Flash 2.5)
- [ ] Epic 5: SuggestionCascade + Rate Limit + REST API
- [ ] Epic 6: 관측성 + Fallback Runbook

## 관련 문서

- **선행 fix 이슈**:
  - `workflow/task/fix/brainstorming/version/0.0.2v/issue-09-ai-suggestion-3layer.md` — 4-Port 오케스트레이션
  - `workflow/task/fix/brainstorming/version/0.0.2v/issue-10-role-catalog-expansion.md` — Role Catalog
- **선행 Product**: [`product-learning-tower.md`](product-learning-tower.md) (Epic 1~4)
- **후행 Product**: [`product-ai-interactive-roadmap.md`](product-ai-interactive-roadmap.md)
- **관련 ADR**:
  - [ADR010](../../../../../docs/adr/ADR010.md) — AI 제안 호출 실패 5xx 미노출
  - [**ADR022**](../../../../../docs/adr/ADR022-learning-tower-terminology.md) — 학습 계층 용어 표준 (본 Product 준수)
- **DOMAIN.md / PACKAGE.md 갱신 예정**:
  - `docs/PACKAGE.md` — `ai/port/out/suggestion/`, `ai/adapter/out/static/`, `ai/adapter/out/llm/` 절 신설
  - `docs/DOMAIN.md` — AI 제안 절 (본 Product 요약)
- **폐기 문서**:
  - 기존 `product-aisuggestion.md` (파일명 통일 위해 삭제, 본 파일이 대체)

## 열린 질문 (Open Questions)

1. **Role 미감지 시 default 는 `generic` vs `backend-developer`** — generic 카탈로그를 별도 큐레이션할지, backend 를 default 로 둘지.
2. **RoleDetector keyword 매칭 규칙** — 정규식 vs 임베딩 vs 하드코드 사전. v1 은 하드코드 사전.
3. **LLM 응답 언어** — 한국어 강제 vs 사용자 concepts 언어 감지. v1 은 한국어 강제.
4. **Cascade 결과의 provider 노출** — 응답에 `providerContext:"llm|static"` 을 노출할지 (실험 관측용) vs 숨김(사용자 혼란 방지). v1 은 dev 만 노출.
5. **BeanOutputConverter 응답 record 위치** — `ai/port/out/suggestion/dto/` vs Adapter 하위. v1 은 Port 근처.
6. **LayerSuggestion 응답 필드** — `name, rationale, suggestedAxisCount` 로 시작, `keywords[]` 는 v2.
7. **Rate Limit 스코프** — 유저별 vs IP별 vs 유저별+글로벌. v1 은 유저별 10rpm 만.
8. **테스트 시 LLM Mock 전략** — WireMock 스텁 vs SpringBootTest MockBean. Slice 는 MockBean.

## 제품 수준 완료 기준 (Product-level DoD)

- [ ] Epic 1~6 완료
- [ ] Static Adapter 4개 + Role Catalog 4개 (총 4 catalog × 4 port = 16 항목 커버)
- [ ] LLM Adapter 4개 + Fallback Cascade 검증
- [ ] REST API 4 엔드포인트 + Rate Limit
- [ ] 관측 지표 · Fallback Runbook 발행
- [ ] `docs/PACKAGE.md` 갱신

---

# [Epic 1] 4-Port → 6-Port 인터페이스 + DTO 정의

> **⚠️ PARTIALLY SUPERSEDED (2026-07-02) — Story 1-3(`RoadmapSuggestionPort`), Story 1-4(`SelectionsSuggestionPort`)는 outline + subtree 2단계로 재편성.**
> Story 1-1(`LayerSuggestionPort`), Story 1-2(`AxisSuggestionPort`)는 유효.
> Story 1-3 대체: `ChaptersOutlinePort` + `ChapterSubtreePort` 2개 인터페이스 신설.
> Story 1-4 대체: `SelectionOutlinePort` + `SelectionSubtreePort` 2개 인터페이스 신설.
> 재편성 태스크는 이슈 #17 이관 산출물 참고.

## 목표

`LayerSuggestionPort` / `AxisSuggestionPort` / **`ChaptersOutlinePort` / `ChapterSubtreePort` / `SelectionOutlinePort` / `SelectionSubtreePort`** 6개 인터페이스와 요청/응답 DTO(record) 를 정의해 후속 Adapter · Service 가 이 계약 위에 자란다.

## 배경

Port 계약 확정 없이 Adapter 구현을 시작하면 어댑터 별로 시그니처 드리프트. 4-Port 를 먼저 확정하고 그 위에 Static / LLM Adapter 구현.

## 포함 Story

- Story 1-1: `LayerSuggestionPort` + `LayerSuggestion` (VO) + `LayerSuggestionContext` (Command)
- Story 1-2: `AxisSuggestionPort` + `AxisSuggestion` + `AxisSuggestionContext`
- Story 1-3: `RoadmapSuggestionPort` + `RoadmapSuggestion` + `RoadmapSuggestionContext`
- Story 1-4: `SelectionsSuggestionPort` + `SelectionSuggestion` + `SelectionsSuggestionContext`

## Epic 인수 시나리오

- Given 4 Port 인터페이스 / When 컴파일 / Then 통과
- Given Adapter 없는 상태 / When Spring Boot 기동 / Then Port 만 정의된 상태로 실패 없이 컴파일 (Adapter Bean 은 Epic 2 이후)

## Epic 완료 기준 (DoD)

- [ ] Port 4개 + DTO 12개 (요청/응답/컨텍스트 각 4개)
- [ ] 단위 테스트 없음 (인터페이스라 별도 로직 X)
- [ ] `docs/PACKAGE.md` — Port 위치 명시

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **Port 위치** — `ai/port/out/suggestion/`.
- **DTO 위치** — `ai/port/out/suggestion/dto/`.
- **DTO 방식** — Java record.

---

## [Story 1-1] `LayerSuggestionPort` + DTO

### User Story

- As a 백엔드 엔지니어
- I want LayerSuggestionPort 인터페이스와 응답 record 를 정의하기를
- so that Static / LLM Adapter 가 이 계약 위에서 구현된다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

```java
public interface LayerSuggestionPort {
    List<LayerSuggestion> suggest(LayerSuggestionContext context);
}

public record LayerSuggestionContext(
    Long facadeId, List<String> concepts, String role
) {}

public record LayerSuggestion(
    String name, String rationale, int suggestedAxisCount
) {}
```

**핵심 파일**:
- 신규: `ai/port/out/suggestion/LayerSuggestionPort.java`
- 신규: `ai/port/out/suggestion/dto/LayerSuggestion.java`
- 신규: `ai/port/out/suggestion/dto/LayerSuggestionContext.java`

### 완료 기준 (AC)

- Given 인터페이스 파일 / When 컴파일 / Then 통과
- Given DTO record / When JSON 직렬화 / Then 필드 노출

### Definition of Done

- [ ] 파일 3개 신설

### 스토리 포인트

0.5d

### 의존성

- 선행: 없음
- 후행: Epic 2 Story 2-1

---

## [Story 1-2] `AxisSuggestionPort` + DTO

### User Story

- As a 백엔드 엔지니어
- I want AxisSuggestionPort 를 정의하기를
- so that Layer 컨텍스트에서 Axis 추천이 가능하다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

```java
public interface AxisSuggestionPort {
    List<AxisSuggestion> suggest(AxisSuggestionContext context);
}

public record AxisSuggestionContext(
    Long facadeId, Long layerId, String layerName, List<String> concepts, String role
) {}

public record AxisSuggestion(
    String name, String rationale, String roadmapDraft  // ASCII 트리
) {}
```

### 완료 기준 (AC)

- Given 파일 3개 / When 컴파일 / Then 통과

### Definition of Done

- [ ] 파일 3개

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 1-1
- 후행: Epic 2 Story 2-2

---

## [Story 1-3] `RoadmapSuggestionPort` + DTO

### User Story

- As a 백엔드 엔지니어
- I want RoadmapSuggestionPort 를 정의하기를
- so that Axis 하위 로드맵 초안 생성이 가능하다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

```java
public interface RoadmapSuggestionPort {
    RoadmapSuggestion suggest(RoadmapSuggestionContext context);
}

public record RoadmapSuggestionContext(
    Long axisId, String axisName, String layerName, List<String> concepts, String role
) {}

public record RoadmapSuggestion(
    String content  // ASCII 트리
) {}
```

### 완료 기준 (AC)

- Given 파일 3개 / When 컴파일 / Then 통과

### Definition of Done

- [ ] 파일 3개

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 1-1
- 후행: Epic 2 Story 2-3

---

## [Story 1-4] `SelectionsSuggestionPort` + DTO

### User Story

- As a 백엔드 엔지니어
- I want SelectionsSuggestionPort 를 정의하기를
- so that 확정 Roadmap 하위 Selection 초안 생성이 가능하다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

```java
public interface SelectionsSuggestionPort {
    List<SelectionSuggestion> suggest(SelectionsSuggestionContext context);
}

public record SelectionsSuggestionContext(
    Long axisId, String axisName, String roadmapContent, String layerName,
    List<String> concepts, String role
) {}

public record SelectionSuggestion(
    String name, String content
) {}
```

### 완료 기준 (AC)

- Given 파일 3개 / When 컴파일 / Then 통과

### Definition of Done

- [ ] 파일 3개

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 1-1
- 후행: Epic 2 Story 2-4

---

> **Epic 1 재정의 판 (2026-07-02, 이슈 #17 이관)** — Story 1-3 (`RoadmapSuggestionPort`) · Story 1-4 (`SelectionsSuggestionPort`) SUPERSEDED. 아래 Story 1-5 ~ 1-8 신설로 outline + subtree 2단계 flow(A) 반영.

## [Story 1-5] `ChaptersOutlinePort` + DTO

### User Story

- As a AI 인터페이스 개발자
- I want `ChaptersOutlinePort` 인터페이스 + Request/Response DTO record가 신설되기를
- so that 챕터 outline(title·rationale 리스트) 요청·응답이 강 타입으로 명세

### 설명

> 출처: `issue-17-ai-two-step-generation.md` §부속 결정 §6-Port 시그니처

- `ai/port/out/suggestion/ChaptersOutlinePort.java` — 인터페이스 (`suggest(ChaptersOutlineRequest): ChaptersOutlineResponse`)
- Request DTO (`ai/dto/ChaptersOutlineRequest.java`): `concepts: List<String>`, `layerName`, `axisName`, `axisReason`, `chapterCountHint: Integer?`, `freeformHint: String?`
- Response DTO (`ai/dto/ChaptersOutlineResponse.java`): `chapters: List<ChapterOutlineItem(title, rationale)>`, `providerContext: String`, `suggestionsAvailable: boolean`

### 완료 기준 (AC)

- Given Port 인터페이스 컴파일 / Then 6개 Port 중 하나로 컴파일 성공
- Given `suggestionsAvailable=false` 응답 / Then chapters 빈 리스트 허용
- *(엣지)* chapterCountHint=null이면 Adapter 기본값 사용 (Static: 5개)

### Definition of Done

- [ ] Port 인터페이스 파일
- [ ] Request/Response record 2건
- [ ] 단위 테스트 (`ChaptersOutlineRequestTest` — record 필드 유효성)

### 스토리 포인트

0.5d

### 의존성

- 선행: 없음
- 후행: Epic 2 Story 2-5 (`StaticChaptersOutlineAdapter`)

## [Story 1-6] `ChapterSubtreePort` + DTO

### User Story

- As a AI 인터페이스 개발자
- I want `ChapterSubtreePort` 인터페이스 + DTO가 신설되기를
- so that 승인된 챕터의 subtree(bodyAsciiTree) 요청이 병렬 가능

### 설명

> 출처: `issue-17-ai-two-step-generation.md`

- `ai/port/out/suggestion/ChapterSubtreePort.java`
- Request: `concepts`, `layerName`, `axisName`, `chapter(title, rationale)`, `siblingChapters: List<ChapterOutlineItem>`
- Response: `bodyAsciiTree: String` (챕터 subtree 통짜), `providerContext`, `suggestionsAvailable`

### 완료 기준 (AC)

- Given Port 컴파일 성공
- Given `bodyAsciiTree`가 `├── 1-1.` 형식 문자열 통짜로 반환

### Definition of Done

- [ ] Port 인터페이스 + Request/Response record
- [ ] 단위 테스트

### 스토리 포인트

0.5d

### 의존성

- 선행: 없음
- 후행: Epic 2 Story 2-6

## [Story 1-7] `SelectionOutlinePort` + DTO

### User Story

- As a AI 인터페이스 개발자
- I want `SelectionOutlinePort`가 Selection 컨테이너 name + 챕터 outline을 반환하기를
- so that 새 Selection 생성 시 컨테이너 name 후보 + 챕터 리스트 제안

### 설명

> 출처: `issue-17-ai-two-step-generation.md`

- `ai/port/out/suggestion/SelectionOutlinePort.java`
- Request: `concepts`, `layerName`, `axisName`, `roadmapContent(요약)`, `variantHint`, `chapterCountHint?`
- Response: `nameCandidate: String`, `chapters: List<ChapterOutlineItem>`, `providerContext`, `suggestionsAvailable`

### 완료 기준 (AC)

- Given Port 컴파일 성공
- Given `nameCandidate`가 사용자 예시 형식(예: "능 아키텍처 selections v2") 반환

### Definition of Done

- [ ] Port · Request/Response record
- [ ] 단위 테스트

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 1-5 (`ChapterOutlineItem` 재사용)
- 후행: Epic 2 Story 2-7

## [Story 1-8] `SelectionSubtreePort` + DTO

### User Story

- As a AI 인터페이스 개발자
- I want `SelectionSubtreePort`가 Selection 컨테이너 내 각 챕터의 subtree를 반환하기를
- so that Roadmap과 동일한 outline → subtree 2단계 flow

### 설명

> 출처: `issue-17-ai-two-step-generation.md`

- `ai/port/out/suggestion/SelectionSubtreePort.java`
- Request: `concepts`, `layerName`, `axisName`, `chapter`, `selectionName`, `selectionSiblings`
- Response: `bodyAsciiTree`, `providerContext`, `suggestionsAvailable`

### 완료 기준 (AC)

- Given Port 컴파일 성공

### Definition of Done

- [ ] Port · Request/Response record
- [ ] 단위 테스트

### 스토리 포인트

0.5d

### 의존성

- 선행: Story 1-6 (subtree DTO 패턴 재사용)
- 후행: Epic 2 Story 2-8

---

# [Epic 2] Static Adapter 4개 (기본 데이터)

## 목표

각 Port 별 Static Adapter 를 구현해 LLM 없이도 4-Port 모두 응답 가능한 상태 확보. 카탈로그 파일은 초기 `generic.json` 만 (Role Catalog 확장은 Epic 3).

## 배경

Static Adapter 는 Fallback 자산이자 로컬·테스트·인증 미비 상태의 기본 응답 경로. 4-Port 모두 Static 구현이 있어야 LLM 없이 Product 가 동작.

## 포함 Story

- Story 2-1: `StaticLayerSuggestionAdapter` + `resources/ai/catalog/generic.json` (layers 절)
- Story 2-2: `StaticAxisSuggestionAdapter`
- Story 2-3: `StaticRoadmapSuggestionAdapter`
- Story 2-4: `StaticSelectionsSuggestionAdapter`

## Epic 인수 시나리오

- Given `provider=static`, concepts=["백엔드"] / When `LayerSuggestionPort.suggest(...)` / Then 3~5 layers 반환
- Given 4 Adapter Bean 활성 / When 각각 호출 / Then 응답 성공

## Epic 완료 기준 (DoD)

- [ ] Adapter 4개
- [ ] `generic.json` 카탈로그 (초기 4-Port 데이터)
- [ ] 단위 테스트 (각 Adapter 해피 케이스)
- [ ] Repository Slice 아닌 로직 슬라이스 테스트

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **카탈로그 로딩 시점** — startup ClassPathResource 캐시 (Adapter 필드).
- **`@ConditionalOnProperty(name="thirdtool.suggestion.provider", havingValue="static", matchIfMissing=true)`** — 기본값 static.

---

## [Story 2-1] `StaticLayerSuggestionAdapter`

### User Story

- As a 백엔드 엔지니어
- I want Static Layer Adapter 가 기본 카탈로그로 응답하기를
- so that LLM 없이도 Layer 제안이 가능하다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

```java
@Component
@ConditionalOnProperty(
    name = "thirdtool.suggestion.provider",
    havingValue = "static",
    matchIfMissing = true
)
class StaticLayerSuggestionAdapter implements LayerSuggestionPort {
    private final SuggestionCatalogLoader catalog;

    public List<LayerSuggestion> suggest(LayerSuggestionContext ctx) {
        var role = ctx.role() != null ? ctx.role() : "generic";
        return catalog.load(role).layers().stream()
                      .limit(5)
                      .toList();
    }
}
```

**핵심 파일**:
- 신규: `ai/adapter/out/static/StaticLayerSuggestionAdapter.java`
- 신규: `ai/adapter/out/static/SuggestionCatalogLoader.java`
- 신규: `src/main/resources/ai/catalog/generic.json`

### 완료 기준 (AC)

- Given `provider=static` / When `port.suggest({concepts:["A"], role:null})` / Then layers 반환 (generic 카탈로그)
- Given catalog 파일 없음 / When Bean 활성화 / Then 명확한 오류 (`AI_PROVIDER_MISCONFIGURED`)

### Definition of Done

- [ ] Adapter 구현
- [ ] generic.json 초기 데이터
- [ ] 단위 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Epic 1 Story 1-1
- 후행: Story 2-2

---

## [Story 2-2] `StaticAxisSuggestionAdapter`

### User Story

- As a 백엔드 엔지니어
- I want Static Axis Adapter 가 Layer 컨텍스트별 응답하기를
- so that Static 만으로도 Layer 별 Axis 추천이 가능하다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

Layer 이름을 카탈로그 키로 참조 → 해당 Layer 의 axes 목록 반환. Layer 미매칭 시 카탈로그의 defaultAxes 사용.

### 완료 기준 (AC)

- Given layerName="백엔드", generic catalog / When suggest / Then axes 목록
- Given layerName=매칭 실패 / When suggest / Then defaultAxes 반환

### Definition of Done

- [ ] Adapter 구현
- [ ] 단위 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Epic 1 Story 1-2
- 후행: Story 2-3

---

## [Story 2-3] `StaticRoadmapSuggestionAdapter`

### User Story

- As a 백엔드 엔지니어
- I want Static Roadmap Adapter 가 축별 ASCII 트리를 반환하기를
- so that LLM 없이도 축 로드맵 초안이 가능하다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

카탈로그의 `roadmaps: {axisName → asciiTree}` 매핑 사용. 축 이름 매칭 실패 시 fallback ASCII 트리(예: "- 개요\n- 심화\n- 실전").

### 완료 기준 (AC)

- Given axisName="Spring", catalog 에 정의 / When suggest / Then 해당 ASCII 트리 반환
- Given 매칭 실패 / When / Then fallback 트리 반환

### Definition of Done

- [ ] Adapter 구현
- [ ] 단위 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Epic 1 Story 1-3
- 후행: Story 2-4

---

## [Story 2-4] `StaticSelectionsSuggestionAdapter`

### User Story

- As a 백엔드 엔지니어
- I want Static Selections Adapter 가 축별 예시 selections 를 반환하기를
- so that LLM 없이도 사례 추천이 가능하다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

카탈로그의 `selections: {axisName → [{name, content}, ...]}` 매핑 사용.

### 완료 기준 (AC)

- Given axisName="Spring", catalog 정의 / When / Then selections 반환
- Given 매칭 실패 / When / Then 빈 리스트 반환 (Fallback 상위 로직이 suggestionsAvailable=false 처리)

### Definition of Done

- [ ] Adapter 구현
- [ ] 단위 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Epic 1 Story 1-4
- 후행: Epic 3

---

> **Epic 2 재정의 판 (2026-07-02, 이슈 #17 이관)** — Story 2-3 (`StaticRoadmapSuggestionAdapter`) · Story 2-4 (`StaticSelectionsSuggestionAdapter`) SUPERSEDED. 아래 Story 2-5 ~ 2-8 신설로 6-Port Static Adapter 완성.

## [Story 2-5] `StaticChaptersOutlineAdapter`

### User Story

- As a AI 인프라 개발자
- I want Static Adapter가 role-keyed catalog 조회로 챕터 outline 리스트를 반환하기를
- so that LLM 없이도 사용자에게 초안 노출

### 설명

> 출처: `issue-17-ai-two-step-generation.md` §부속 결정 §Static Adapter fallback

- `ai/adapter/out/suggestion/static/StaticChaptersOutlineAdapter.java` — `ChaptersOutlinePort` 구현
- `RoleCatalog.loadRole(roleHint)` → `chapters: List<ChapterOutlineItem>` 배열 반환
- Role 감지 (Epic 3 Story 3-1) 실패 시 default (`generic` 또는 `backend-developer`) 사용
- `providerContext: "static:{role}"` 응답

### 완료 기준 (AC)

- Given concepts=["백엔드", "Spring"] / When `suggest(...)` / Then role=backend-developer 감지 + chapters 5개 (`backend-developer.json`의 챕터 예시)
- Given catalog 로드 실패 / Then `suggestionsAvailable=false` + chapters 빈 리스트
- *(엣지)* chapterCountHint=3 / Then 5개 중 상위 3개만 반환

### Definition of Done

- [ ] Adapter 구현
- [ ] 단위 테스트 (`StaticChaptersOutlineAdapterTest` 해피/엣지/예외)
- [ ] 통합 테스트 (`@SpringBootTest`) — Port 주입 + 응답 형식 검증

### 스토리 포인트

1.5d

### 의존성

- 선행: Epic 1 Story 1-5 (`ChaptersOutlinePort`), Epic 3 Story 3-2 (`backend-developer.json`)
- 후행: Epic 5 Story 5-1 (Cascade)

## [Story 2-6] `StaticChapterSubtreeAdapter`

### User Story

- As a AI 인프라 개발자
- I want Static Adapter가 특정 챕터의 subtree(ASCII 통짜)를 catalog에서 조회 반환하기를
- so that 챕터별 병렬 요청도 Static으로 처리

### 설명

- `StaticChapterSubtreeAdapter.java` — `ChapterSubtreePort` 구현
- Catalog: `role.chapters[N].subtree` 필드
- `providerContext: "static:{role}"`

### 완료 기준 (AC)

- Given chapter.title = "1. 하네스 엔지니어링 기초" / When suggest / Then `bodyAsciiTree` = catalog의 해당 챕터 subtree 통짜

### Definition of Done

- [ ] Adapter 구현
- [ ] 단위 테스트 · 통합 테스트

### 스토리 포인트

1.5d

### 의존성

- 선행: Epic 1 Story 1-6, Epic 3 Story 3-2
- 후행: Epic 5

## [Story 2-7] `StaticSelectionOutlineAdapter`

### User Story

- As a AI 인프라 개발자
- I want Static Adapter가 Selection outline(name + 챕터 리스트) 반환하기를
- so that Roadmap과 동일한 outline → subtree 흐름 지원

### 설명

- `StaticSelectionOutlineAdapter.java` — `SelectionOutlinePort` 구현
- Catalog: `role.selectionOutlines[]` 배열 (예: "능 아키텍처 selections v2" 형태 name + 챕터 리스트)
- variantHint 존재 시 매칭 우선 (없으면 첫 번째 반환)

### 완료 기준 (AC)

- Given variantHint="능 아키텍처" / When suggest / Then nameCandidate = "능 아키텍처 selections v2"

### Definition of Done

- [ ] Adapter 구현 · 단위 · 통합 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Epic 1 Story 1-7, Epic 3 Story 3-2
- 후행: Epic 5

## [Story 2-8] `StaticSelectionSubtreeAdapter`

### User Story

- As a AI 인프라 개발자
- I want Static Adapter가 Selection 내 챕터의 subtree를 반환하기를
- so that Selection도 챕터별 병렬 요청 처리 가능

### 설명

- `StaticSelectionSubtreeAdapter.java` — `SelectionSubtreePort` 구현
- Catalog: `role.selectionOutlines[N].chapters[M].subtree`

### 완료 기준 (AC)

- Given selectionName + chapter / When suggest / Then bodyAsciiTree 반환

### Definition of Done

- [ ] Adapter 구현 · 단위 · 통합 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Epic 1 Story 1-8, Epic 3 Story 3-2
- 후행: Epic 5

---

# [Epic 3] Role Catalog 확장 (4 role) + RoleDetector

## 목표

Static Adapter 의 fallback 이 개발자 외 3개 role(planner, designer, problem-solver) 컨셉에도 유의미한 데이터를 반환하도록 role 별 JSON 카탈로그를 확장하고, `concepts[]` 로부터 role 을 자동 감지한다.

## 배경

기존 fallback 은 backend-developer 중심으로 큐레이션되어 있어 다른 role 사용자 진입 시 fallback 이 무의미. Role Catalog 확장으로 다양한 사용자 진입을 지원.

## 포함 Story

- Story 3-1: `RoleDetector` 구현 (concepts → role hint 매칭)
- Story 3-2: `backend-developer.json` 카탈로그 큐레이션
- Story 3-3: `planner.json` 카탈로그 신설
- Story 3-4: `designer.json` 카탈로그 신설
- Story 3-5: `problem-solver.json` 카탈로그 신설

## Epic 인수 시나리오

- Given concepts=["백엔드","시스템설계"] / When `RoleDetector.detect(concepts)` / Then "backend-developer"
- Given concepts=["기획","프로덕트","요구사항"] / When 감지 / Then "planner"
- Given concepts=["UI","디자인","와이어프레임"] / When / Then "designer"
- Given concepts=["문제해결","알고리즘","트러블슈팅"] / When / Then "problem-solver"
- Given concepts=["random"] / When / Then "generic" (fallback)

## Epic 완료 기준 (DoD)

- [ ] RoleDetector 구현
- [ ] 카탈로그 4개 파일
- [ ] 각 카탈로그가 4-Port 모두 커버
- [ ] 단위 테스트

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **RoleDetector 규칙** — 하드코드 사전 (`Map<String, Set<String>>` keyword → role).
- **Cascade** — role 매칭 실패 시 generic → 그것도 없으면 빈 리스트.

---

## [Story 3-1] `RoleDetector` 구현

### User Story

- As a 백엔드 엔지니어
- I want concepts 로부터 사용자 role 을 자동 감지하기를
- so that 명시적 role 선택 없이 fallback 이 개인화된다

### 설명

> 출처: `issue-10-role-catalog-expansion.md`

```java
@Component
public class RoleDetector {
    private static final Map<String, Set<String>> ROLE_KEYWORDS = Map.of(
        "backend-developer", Set.of("백엔드","backend","java","spring","시스템","system"),
        "planner",           Set.of("기획","planner","프로덕트","product","pm","요구사항"),
        "designer",          Set.of("디자인","design","ui","ux","와이어프레임"),
        "problem-solver",    Set.of("문제해결","problem","알고리즘","algorithm","트러블슈팅")
    );

    public String detect(List<String> concepts) {
        for (var entry : ROLE_KEYWORDS.entrySet()) {
            for (var concept : concepts) {
                var normalized = concept.toLowerCase().trim();
                if (entry.getValue().stream().anyMatch(normalized::contains)) {
                    return entry.getKey();
                }
            }
        }
        return "generic";
    }
}
```

### 완료 기준 (AC)

- Given 다양한 concepts 조합 / When detect / Then 예상 role 반환
- Given 대소문자 · trim 필요 / When / Then 정규화 후 매칭
- *(엣지)* Given 다중 role 매칭 (예: ["백엔드","디자인"]) / When / Then 첫 매칭 반환 (Map 순서)

### Definition of Done

- [ ] 구현
- [ ] 단위 테스트 (각 role · generic · 다중 매칭)

### 스토리 포인트

0.5d

### 의존성

- 선행: 없음
- 후행: Story 3-2~3-5

---

## [Story 3-2] `backend-developer.json` 카탈로그

### User Story

- As a Static Adapter 사용자
- I want backend-developer 카탈로그에 4-Port 데이터가 큐레이션되기를
- so that 백엔드 개념 사용자에게 실용적 fallback 이 제공된다

### 설명

> 출처: `issue-10-role-catalog-expansion.md`

`src/main/resources/ai/catalog/backend-developer.json` — 다음 구조:
```json
{
  "layers": [
    { "name": "백엔드 기초", "rationale": "...", "suggestedAxisCount": 4 },
    { "name": "시스템 설계", "rationale": "...", "suggestedAxisCount": 3 },
    ...
  ],
  "axes": {
    "백엔드 기초": [
      { "name": "Java", "rationale": "...", "roadmapDraft": "- 기초\n- 객체지향\n..." },
      ...
    ],
    "_default": [ ... ]
  },
  "roadmaps": {
    "Java": "- 기초 문법\n- 컬렉션\n- 동시성\n- JVM",
    ...
  },
  "selections": {
    "Java": [
      { "name": "동시성 실전", "content": "- Lock / AtomicX\n- CompletableFuture" }
    ],
    ...
  }
}
```

### 완료 기준 (AC)

- Given catalog 로드 / When `catalog.load("backend-developer")` / Then 4-Port 데이터 모두 존재
- Given Adapter 호출 / When Layer 4개 반환 / Then 각 rationale != blank

### Definition of Done

- [ ] JSON 파일 (초기 데이터 큐레이션)
- [ ] catalog 스키마 검증 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Story 3-1
- 후행: Story 3-3

---

## [Story 3-3] `planner.json` 카탈로그

### User Story

- As a Static Adapter 사용자
- I want planner 카탈로그로 기획자 컨셉에 응답하기를
- so that 기획자 진입 사용자도 유의미한 fallback 을 받는다

### 설명

> 출처: `issue-10-role-catalog-expansion.md`

`planner.json` — layers 예시: "요구사항 관리", "제품 전략", "UX 리서치". axes 예시: "사용자 인터뷰", "OKR". roadmaps · selections 도 큐레이션.

### 완료 기준 (AC)

- Given catalog / When 로드 / Then planner 데이터 반환

### Definition of Done

- [ ] JSON

### 스토리 포인트

1d

### 의존성

- 선행: Story 3-1
- 후행: Story 3-4

---

## [Story 3-4] `designer.json` 카탈로그

### User Story

- As a Static Adapter 사용자
- I want designer 카탈로그로 디자이너 컨셉에 응답하기를
- so that 디자이너 진입 사용자도 유의미한 fallback

### 설명

> 출처: `issue-10-role-catalog-expansion.md`

`designer.json` — layers 예시: "시각 디자인 기초", "UI 시스템", "인터랙션". axes 예시: "타이포그래피", "컬러 시스템", "컴포넌트 라이브러리". roadmaps · selections 큐레이션.

### 완료 기준 (AC)

- Given catalog / When 로드 / Then designer 데이터 반환

### Definition of Done

- [ ] JSON

### 스토리 포인트

1d

### 의존성

- 선행: Story 3-1
- 후행: Story 3-5

---

## [Story 3-5] `problem-solver.json` 카탈로그

### User Story

- As a Static Adapter 사용자
- I want problem-solver 카탈로그로 알고리즘/트러블슈팅 컨셉에 응답하기를
- so that 문제해결 진입 사용자도 유의미한 fallback

### 설명

> 출처: `issue-10-role-catalog-expansion.md`

`problem-solver.json` — layers 예시: "알고리즘 기초", "자료구조", "트러블슈팅 방법론". axes 예시: "그래프 알고리즘", "DP".

### 완료 기준 (AC)

- Given catalog / When 로드 / Then problem-solver 데이터 반환

### Definition of Done

- [ ] JSON

### 스토리 포인트

1d

### 의존성

- 선행: Story 3-1
- 후행: 없음

---

# [Epic 4] LLM Adapter 4개 (Vertex AI Gemini Flash 2.5)

## 목표

Spring AI 1.0 + Vertex AI Gemini Flash 2.5 로 각 Port 별 LLM Adapter 구현. Prompt Template + BeanOutputConverter 로 Structured Output 자동.

## 배경

Static Adapter 만으로는 사용자 concepts 조합의 특수성 반영 불가. LLM Adapter 활성화 시 정교한 개인화 가능. Fallback 안전망은 Cascade(Story Epic 5) 로 유지.

## 포함 Story

- Story 4-1: Spring AI + Vertex AI 인프라 세팅 (`ChatClient` Bean, GCP ADC)
- Story 4-2: `LlmLayerSuggestionAdapter` + `.st` 프롬프트
- Story 4-3: `LlmAxisSuggestionAdapter`
- Story 4-4: `LlmRoadmapSuggestionAdapter`
- Story 4-5: `LlmSelectionsSuggestionAdapter`

## Epic 인수 시나리오

- Given `provider=llm`, GCP ADC 인증 / When Layer 제안 요청 / Then Gemini 응답을 Layer record 로 파싱 반환
- Given LLM 응답 파싱 실패 / When / Then null 반환 (Cascade 가 Static 폴백)

## Epic 완료 기준 (DoD)

- [ ] LLM Adapter 4개
- [ ] `.st` 프롬프트 4개
- [ ] 통합 테스트 (WireMock 스텁으로 LLM 응답 시뮬)
- [ ] GCP Secret Manager 자격 설정 문서화

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **LLM 실패 시 반환값** — `null` 이 아닌 `Optional<T>` 또는 예외. v1 은 예외 throw + Cascade catch.
- **프롬프트 매개변수** — `.st` 단일 중괄호.

---

## [Story 4-1] Spring AI + Vertex AI 인프라

### User Story

- As a 백엔드 엔지니어
- I want ChatClient Bean 이 Vertex AI Gemini Flash 2.5 로 구성되기를
- so that 4개 LLM Adapter 가 공통 인프라 위에서 자란다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `spring.ai.vertex.ai.gemini.project-id` · `location` · `chat.options.model=gemini-2.5-flash`
- GCP ADC 인증 (dev: `gcloud auth application-default login`, prod: Workload Identity + Secret Manager)
- `ChatClient` Spring Bean 자동 구성 (Spring AI starter)

**핵심 파일**:
- 수정: `build.gradle.kts` — Spring AI Vertex AI starter 의존
- 신규: `ai/config/AiConfig.java`
- 수정: `application-{dev,prod}.yml` — Spring AI 설정

### 완료 기준 (AC)

- Given `provider=llm` + ADC 인증 / When Spring Boot 기동 / Then ChatClient Bean 활성
- Given ADC 미설정 / When 기동 / Then `AI_PROVIDER_AUTH_FAILED` 로그

### Definition of Done

- [ ] Bean 구성
- [ ] 문서 (Runbook)

### 스토리 포인트

1.5d

### 의존성

- 선행: Epic 1, `product-infra-ops.md` Secret Manager
- 후행: Story 4-2~4-5

---

## [Story 4-2] `LlmLayerSuggestionAdapter` + 프롬프트

### User Story

- As a 백엔드 엔지니어
- I want LLM 이 Layer 를 생성하기를
- so that concepts 조합의 특수성이 Layer 추천에 반영된다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

```java
@Component
@ConditionalOnProperty(name="thirdtool.suggestion.provider", havingValue="llm")
class LlmLayerSuggestionAdapter implements LayerSuggestionPort {
    private final ChatClient chat;
    private final Resource promptTemplate;  // ai/prompt/layer-suggestion.st
    private final BeanOutputConverter<LayerSuggestionResponse> converter;

    public List<LayerSuggestion> suggest(LayerSuggestionContext ctx) {
        var prompt = new PromptTemplate(promptTemplate).render(Map.of(
            "concepts", String.join(", ", ctx.concepts()),
            "role", ctx.role(),
            "format", converter.getFormat()
        ));
        try {
            var response = chat.prompt(prompt).call().content();
            return converter.convert(response).layers();
        } catch (Exception e) {
            throw new LlmAdapterException("Layer suggestion failed", e);
        }
    }
}
```

`ai/prompt/layer-suggestion.st`:
```
당신은 학습 로드맵 설계자입니다. 다음 컨셉을 기반으로 학습 Layer 3~5개를 제안하세요.
컨셉: {concepts}
사용자 역할: {role}

각 Layer 는 name, rationale, suggestedAxisCount 를 포함하세요.
{format}
```

### 완료 기준 (AC)

- Given concepts=["백엔드"] / When LLM 호출 / Then 3~5 Layer 반환
- Given LLM 응답 형식 오류 / When 파싱 / Then `LlmAdapterException`

### Definition of Done

- [ ] Adapter
- [ ] Prompt file
- [ ] 통합 테스트 (WireMock stub)

### 스토리 포인트

2d

### 의존성

- 선행: Story 4-1
- 후행: 없음

---

## [Story 4-3] `LlmAxisSuggestionAdapter`

### User Story

- As a 백엔드 엔지니어
- I want LLM 이 Layer 컨텍스트에서 Axis 를 생성하기를
- so that Layer 특성에 맞춘 Axis 추천

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

Layer 컨텍스트(name, concepts) 를 프롬프트 매개변수로 주입. Response record: `AxisSuggestionResponse { axes: List<AxisSuggestion> }`.

### 완료 기준 (AC)

- Given layerName="백엔드 기초", concepts=["백엔드"] / When LLM / Then 4~6 Axis 반환

### Definition of Done

- [ ] Adapter + prompt + 테스트

### 스토리 포인트

2d

### 의존성

- 선행: Story 4-1
- 후행: 없음

---

## [Story 4-4] `LlmRoadmapSuggestionAdapter`

### User Story

- As a 백엔드 엔지니어
- I want LLM 이 Axis 에 대한 ASCII 트리 Roadmap 을 생성하기를
- so that 축의 헌법이 자동 초안화된다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

Axis · Layer · concepts 컨텍스트. Response record: `RoadmapSuggestionResponse { content: String }`. 프롬프트가 ASCII 트리 형식(예: "- 최상위\n  - 하위") 을 명시.

### 완료 기준 (AC)

- Given axisName="Spring" / When LLM / Then ASCII 트리 반환

### Definition of Done

- [ ] Adapter + prompt + 테스트

### 스토리 포인트

2d

### 의존성

- 선행: Story 4-1
- 후행: 없음

---

## [Story 4-5] `LlmSelectionsSuggestionAdapter`

### User Story

- As a 백엔드 엔지니어
- I want LLM 이 확정 Roadmap 기반 Selection 사례를 생성하기를
- so that 축의 응용/사례가 초안화된다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

Axis · Roadmap content · Layer · concepts 컨텍스트. Response record: `SelectionsSuggestionResponse { selections: List<SelectionSuggestion> }`.

### 완료 기준 (AC)

- Given roadmapContent 존재 / When LLM / Then selections 반환

### Definition of Done

- [ ] Adapter + prompt + 테스트

### 스토리 포인트

2d

### 의존성

- 선행: Story 4-1
- 후행: 없음

---

# [Epic 5] SuggestionCascade + Rate Limit + REST API

## 목표

`SuggestionCascade` (LLM 시도 → Static 폴백 → 빈 목록) 로 안전 응답, `Bucket4j` 로 유저당 분당 10회 제한, REST 4 엔드포인트 노출.

## 배경

Adapter 만으로는 사용자에게 응답 불가. Application Service 가 Cascade + Rate Limit + Controller 매핑을 조율.

## 포함 Story

- Story 5-1: `SuggestionCascade` 구현 (LLM → Static → empty)
- Story 5-2: `Bucket4j` Rate Limit (유저당 10rpm)
- Story 5-3: `SuggestionController` 4 엔드포인트
- Story 5-4: `SuggestionCommandService` (Application 조율)

## Epic 인수 시나리오

- Given provider=llm, LLM 실패 / When 요청 / Then Static 폴백 결과 반환, 응답에 `providerContext="fallback:llm→static"`
- Given 분당 11회 요청 / When 11번째 / Then 429 + Retry-After
- Given LLM+Static 모두 실패 / When / Then 200 + `suggestionsAvailable=false`

## Epic 완료 기준 (DoD)

- [ ] Cascade + Rate Limit + Controller + Service
- [ ] Controller Slice + 통합 테스트

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **Rate Limit 저장소** — 로컬 (in-memory Bucket4j). Redis 는 v2.

---

## [Story 5-1] `SuggestionCascade`

### User Story

- As a 백엔드 엔지니어
- I want LLM 실패 시 Static 자동 폴백하기를
- so that 사용자에게 최소 무언가는 제공된다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`, ADR010

```java
public class SuggestionCascade<T> {
    private final Supplier<T> primary;   // LLM
    private final Supplier<T> fallback;  // Static

    public CascadeResult<T> execute() {
        try {
            return CascadeResult.of(primary.get(), "llm");
        } catch (Exception e) {
            try {
                return CascadeResult.of(fallback.get(), "fallback:llm→static");
            } catch (Exception e2) {
                return CascadeResult.empty("suggestionsUnavailable");
            }
        }
    }
}
```

### 완료 기준 (AC)

- Given LLM 성공 / When execute / Then `{result, providerContext:"llm"}`
- Given LLM 실패 + Static 성공 / When / Then `{result, providerContext:"fallback:llm→static"}`
- Given 둘 다 실패 / When / Then `{empty, providerContext:"suggestionsUnavailable"}`

### Definition of Done

- [ ] Cascade 유틸
- [ ] 단위 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Epic 2, Epic 4
- 후행: Story 5-2

---

## [Story 5-2] `Bucket4j` Rate Limit

### User Story

- As a 시스템 운영자
- I want 유저당 분당 10회 rate limit 이 적용되기를
- so that LLM 비용 폭증을 막고 서비스 안정성이 유지된다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

Bucket4j 로 유저별 분당 10회. `SuggestionRateLimiter.tryConsume(userId): boolean`. 실패 시 `AI_SUGGESTION_RATE_LIMITED` 429 + Retry-After 헤더.

### 완료 기준 (AC)

- Given 유저 U 의 요청 10회 / When 11번째 / Then 429
- Given 1분 경과 후 / When 재요청 / Then 200

### Definition of Done

- [ ] Rate Limiter 구현
- [ ] Controller Slice 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Epic 4
- 후행: Story 5-3

---

## [Story 5-3] `SuggestionController` 4 엔드포인트

### User Story

- As a FE 개발자
- I want 4-Port 각각 REST 엔드포인트가 노출되기를
- so that UI 에서 Layer/Axis/Roadmap/Selections 제안을 각각 요청 가능

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `POST /api/v1/suggestions/layers`
- `POST /api/v1/suggestions/axes`
- `POST /api/v1/suggestions/roadmaps`
- `POST /api/v1/suggestions/selections`

Response DTO: `{ layers/axes/... : [...], suggestionsAvailable: bool, providerContext?: string }`

### 완료 기준 (AC)

- Given 각 엔드포인트 요청 / When 정상 / Then 200 + 결과
- Given 요청 body 유효성 실패 / When / Then 400

### Definition of Done

- [ ] Controller
- [ ] Request/Response DTO
- [ ] Controller Slice 테스트
- [ ] Swagger UI 갱신

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 5-2
- 후행: Story 5-4

---

## [Story 5-4] `SuggestionCommandService`

### User Story

- As a 백엔드 엔지니어
- I want Application Service 가 Cascade / Rate Limit / Port 호출을 조율하기를
- so that Controller 는 얇게 유지되고 도메인 규칙이 응집된다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

```java
@Service
public class SuggestionCommandService {
    private final LayerSuggestionPort layerPort;      // Static or LLM (Conditional)
    private final AxisSuggestionPort axisPort;
    // ... 4개
    private final SuggestionRateLimiter rateLimiter;
    private final RoleDetector roleDetector;

    public LayerSuggestionsResponse suggestLayers(Long userId, LayerSuggestionRequest req) {
        rateLimiter.tryConsume(userId);
        var role = roleDetector.detect(req.concepts());
        var ctx = new LayerSuggestionContext(req.facadeId(), req.concepts(), role);
        var cascade = new SuggestionCascade<>(
            () -> layerPort.suggest(ctx),
            () -> new StaticLayerSuggestionAdapter(catalog).suggest(ctx)
        );
        var result = cascade.execute();
        return LayerSuggestionsResponse.from(result);
    }
    // ... 4-Port 각각
}
```

### 완료 기준 (AC)

- Given 정상 요청 / When suggestLayers / Then 성공 응답
- Given rate limit 초과 / When / Then 429

### Definition of Done

- [ ] Service
- [ ] Service 단위 테스트
- [ ] 통합 테스트

### 스토리 포인트

1.5d

### 의존성

- 선행: Story 5-3
- 후행: 없음

---

# [Epic 6] 관측성 + Fallback Runbook

## 목표

Spring AI 자동 메트릭 + 도메인 보강 메트릭, ErrorCode 5종 등록, Runbook 문서 발행.

## 배경

관측성 없이 LLM 활성화 시 원인 파악 불가. Runbook 없으면 운영 대응이 즉시 어려움.

## 포함 Story

- Story 6-1: `thirdtool.suggestion.*` 메트릭 등록
- Story 6-2: MDC 로그 필드 확장 (`port_type`, `provider`, `role`)
- Story 6-3: ErrorCode 5종 등록
- Story 6-4: Runbook 발행 (`docs/runbook/ai-suggestion.md`)

## Epic 인수 시나리오

- Given Prometheus scrape / When 지표 확인 / Then `thirdtool_suggestion_request_total` 등 노출
- Given LLM 실패 로그 / When 조회 / Then MDC 에 fallback context 포함

## Epic 완료 기준 (DoD)

- [ ] 메트릭 4종 등록
- [ ] MDC 필드 3종 추가
- [ ] ErrorCode 5종 등록
- [ ] Runbook 발행

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **메트릭 라벨 카디널리티** — `provider`, `port`, `role` 만 라벨. `user_id` 는 라벨 아님(product-op §카디널리티 준수).

---

## [Story 6-1] `thirdtool.suggestion.*` 메트릭

### User Story

- As a 운영자
- I want AI 제안 성능/실패율 관측 지표가 노출되기를
- so that LLM 활성화 후 문제 즉시 감지 가능

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `thirdtool.suggestion.request_total{port, provider, role}` — Counter
- `thirdtool.suggestion.latency_seconds{port, provider}` — Histogram
- `thirdtool.suggestion.fallback_total{from, to, reason}` — Counter
- `thirdtool.suggestion.rate_limit_hit_total` — Counter

Micrometer + Prometheus.

### 완료 기준 (AC)

- Given 지표 등록 / When Prometheus scrape / Then 4종 노출

### Definition of Done

- [ ] MeterBinder
- [ ] 통합 테스트

### 스토리 포인트

1d

### 의존성

- 선행: Epic 5, product-op 협력
- 후행: 없음

---

## [Story 6-2] MDC 로그 필드 확장

### User Story

- As a 운영자
- I want AI 제안 로그가 port/provider/role 을 포함하기를
- so that Elasticsearch 필터로 관련 로그를 즉시 찾을 수 있다

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

MDC 화이트리스트에 `port_type`, `provider`, `role` 추가. ADR011 관행.

### 완료 기준 (AC)

- Given SuggestionCommandService 진입 / When 로그 / Then MDC 3필드 포함

### Definition of Done

- [ ] MDC 필드 등록
- [ ] product-log.md 협력

### 스토리 포인트

0.5d

### 의존성

- 선행: Epic 5
- 후행: 없음

---

## [Story 6-3] ErrorCode 5종 등록

### User Story

- As a 백엔드 엔지니어
- I want AI Suggestion ErrorCode 가 통일 등록되기를
- so that HTTP 매핑이 일관되고 클라이언트가 예측 가능

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

- `AI_SUGGESTION_UNAVAILABLE` — 400 (본문 flag)
- `AI_SUGGESTION_RATE_LIMITED` — 429 (Retry-After)
- `AI_PROVIDER_AUTH_FAILED` — 500 (startup)
- `AI_PROVIDER_MISCONFIGURED` — 500 (startup)
- `SUGGESTION_CONCEPTS_REQUIRED` — 400

### 완료 기준 (AC)

- Given ErrorCode enum / When 5종 등록 / Then Controller Slice 테스트로 HTTP 매핑 검증

### Definition of Done

- [ ] ErrorCode 등록

### 스토리 포인트

0.5d

### 의존성

- 선행: Epic 5
- 후행: 없음

---

## [Story 6-4] Runbook 발행

### User Story

- As a 운영자
- I want AI 제안 관련 장애 대응 절차가 문서화되기를
- so that LLM 활성화 후 최소 대응 시간에 문제 격리 가능

### 설명

> 출처: `issue-09-ai-suggestion-3layer.md`

`docs/runbook/ai-suggestion.md` — 다음 시나리오:
1. LLM 응답 급격 지연 → `provider=static` 로 임시 전환
2. Rate Limit 오탐 → Bucket4j 설정 완화
3. GCP ADC 만료 → Secret 갱신
4. Static Catalog 오류 → 카탈로그 파일 재검증

### 완료 기준 (AC)

- Given docs/runbook/ai-suggestion.md / When 조회 / Then 4시나리오 존재

### Definition of Done

- [ ] Runbook 문서

### 스토리 포인트

0.5d

### 의존성

- 선행: Epic 5, 6-1, 6-2
- 후행: 없음

---

# [Epic 7] 개념 명세 프롬프트 embed (roadmap/selections 성격 강제)

## 목표

roadmap = 수렴/기준 저장소, selections = 발산/가능성 저장소 개념을 AI 프롬프트에 명시적 카탈로그·판별기준으로 embed해 Static Adapter catalog 튜닝의 근거 자료로 활용하고, 향후 LLM Adapter (Epic 4)의 시스템 프롬프트로 재사용한다.

## 배경

이슈 #19 이관. 사용자 회의(2026-07-02)에서 few-shot example만으로는 AI가 개념 오염(roadmap에 도구 이름·특정 옵션 비교 등장)을 낼 위험 확인. 명시적 카탈로그·판별기준을 static asset으로 배치.

## 포함 Story

- Story 7-1: `concept-spec.txt` static asset + 6개 프롬프트 템플릿 include 지시

## Epic 인수 시나리오

- Given `resources/prompts/concept-spec.txt` 배치 / When `ChaptersOutlinePort` 프롬프트 조립 / Then 시스템 프롬프트 상단에 concept-spec.txt 내용 포함
- Given roadmap Port 프롬프트 / Then "도구 이름·특정 옵션 비교 금지" 지시 포함
- Given selections Port 프롬프트 / Then "기준은 roadmap 담당, 발산된 관점만" 지시 포함

## Epic 완료 기준 (DoD)

- [ ] `concept-spec.txt` 단일 파일 배치
- [ ] 6개 Port 프롬프트 template (`chapters-outline.txt` · `chapter-subtree.txt` · `selection-outline.txt` · `selection-subtree.txt` · `layer.txt` · `axis.txt`) include 처리
- [ ] Static Adapter catalog 재점검 (backend-developer.json이 개념 명세 준수 여부 확인)

## Epic 기술 결정 / 대안

- **Static asset 위치**: `src/main/resources/prompts/concept-spec.txt` (Spring Boot 리소스 표준)
- **Include 방식**: Spring AI PromptTemplate이 파일 include를 지원하지 않으면 `@Value("classpath:...")` + 문자열 concat
- **v1 이후 확장**: LLM Adapter (Epic 4) 도입 시 그대로 재사용

## [Story 7-1] `concept-spec.txt` static asset + 6개 프롬프트 템플릿 include

### User Story

- As a AI 프롬프트 관리자
- I want 개념 명세 카탈로그·판별기준이 단일 파일로 배치되고 6개 Port 프롬프트가 include하기를
- so that 개념 명세 변경 시 파일 하나만 수정, 6개 Port에 자동 반영

### 설명

> 출처: `issue-19-roadmap-selection-concept-spec.md` §부속 결정

- `src/main/resources/prompts/concept-spec.txt` 배치. 내용:
  - **Roadmap 콘텐츠 카탈로그 (6종, 각 1줄)**: (1) 변하지 않는 핵심 개념 (2) 반복되는 질문의 축 (3) 선택 기준 (4) 트레이드오프 골격 (5) 안티패턴/함정 (6) 계층 구조/의존 순서
  - **Selections 콘텐츠 카탈로그 (5종, 각 1줄)**: (1) 분기 가능한 관점들 (2) 적용 시나리오별 변형 (3) 비교표/옵션 라이브러리 (4) 실험 패턴 (5) 파생 규칙/운영 방법
  - **수렴 point 판별 5개** (roadmap 후보 신호)
  - **발산 point 판별 5개** (selections 후보 신호)
- 6개 Port 프롬프트 템플릿 (`chapters-outline.txt` 등) 신설:
  - 시스템 프롬프트 상단에 `concept-spec.txt` include
  - roadmap Port: "너는 수렴된 판단 프레임을 뽑는다. 도구 이름·특정 옵션 비교 금지" 지시
  - selections Port: "너는 발산된 적용안을 뽑는다. 기준 자체는 roadmap 담당" 지시
- few-shot example (사용자 예시 하네스 로드맵 챕터 subtree) 각 Port 프롬프트에 추가
- **v1(M3)엔 Static Adapter만 활성** — LLM Adapter (Epic 4)는 M6 이관. 프롬프트 파일은 미리 배치 · Static Adapter catalog 튜닝 근거로 활용

### 완료 기준 (AC)

- Given `concept-spec.txt` 파일 존재 / When Static Adapter가 `RoleCatalog` 로드 / Then 카탈로그 데이터가 concept-spec 6+5 카탈로그와 정합 (backend-developer.json 검토)
- Given 6개 Port 프롬프트 파일 존재 / Then 각 파일 상단에 `{{concept-spec.txt}}` include 지시
- Given roadmap Port 프롬프트 / Then "도구 이름 금지" 지시 문자열 grep 통과
- Given selections Port 프롬프트 / Then "기준은 roadmap 담당" 지시 grep 통과

### Definition of Done

- [ ] `concept-spec.txt` 배치 (약 500자 · 카탈로그·판별기준 압축)
- [ ] 6개 프롬프트 템플릿 파일 배치
- [ ] Static Adapter catalog 재점검 · backend-developer.json 정합 확인
- [ ] 통합 테스트 (`@SpringBootTest`) — Adapter 응답에 `providerContext: "static:{role}"` + catalog 데이터 반환 검증

### 스토리 포인트

1d

### 의존성

- 선행: Epic 3 Story 3-2 (`backend-developer.json`)
- 후행: Epic 4 (LLM Adapter가 이 프롬프트 자산 재사용)

---

*작성일: 2026-07-01 | 개정: 2026-07-02 (Epic 1 재정의 + Epic 2 재정의 + Epic 7 신설, 이슈 #17/#19 이관) | 상태: **7 Epic · 34 Story 전체 pending** | Milestone 후보: M3 (learning-tower Epic 1~3 병행). `product-aisuggestion.md` (기존) 대체 파일.*
