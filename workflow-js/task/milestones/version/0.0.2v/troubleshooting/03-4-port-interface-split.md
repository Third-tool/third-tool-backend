# Deep-dive · Part 03 — Port 인터페이스 4개로 나누기까지의 고민 과정

> **본 편의 성격**: `LayerSuggestionPort` / `AxisSuggestionPort` / `RoadmapSuggestionPort` / `SelectionsSuggestionPort` 4-Port 구조가 지금은 자연스러워 보이지만, **1-Port 통합안 · 2-Port 이원안 · N-Port 세분안 등 대안이 있었다**. 왜 4로 결정되고, 왜 이후 6으로 재편됐는지의 사고 흐름.

---

## 0. 기본 질문 — "Port를 왜 여러 개로 나누는가"

Part 02에서 논의한 발산·수렴 사이클(현재 6, 초기 4)이 있다. 하나의 `LearningSuggestionPort` 인터페이스 안에 여러 메서드로 담을 수도 있었다:

```java
// 통합 Port 안
interface LearningSuggestionPort {
    List<LayerSuggestion> suggestLayers(...);
    List<AxisSuggestion> suggestAxes(...);
    RoadmapSuggestion suggestRoadmap(...);
    SelectionsSuggestion suggestSelections(...);
}
```

vs

```java
// 분리 Port
interface LayerSuggestionPort { ... }
interface AxisSuggestionPort { ... }
interface RoadmapSuggestionPort { ... }
interface SelectionsSuggestionPort { ... }
```

**둘의 차이가 실제 코드·유지보수·확장성에 어떤 영향?** 이걸 실측 근거로 뜯어본다.

---

## 1. 첫 시안 (1-Port 통합)

가장 간단한 진입:

```java
interface AiSuggestionPort {
    Object suggest(SuggestionType type, Map<String, Object> params);
}
```

- 하나의 메서드 · 하나의 Adapter · 하나의 property.
- 단점 명확:
  - **타입 안전성 상실** — `Object` 반환. 호출측이 캐스팅.
  - **파라미터 spec 모호** — `Map<String, Object>` · 컴파일러가 field 검증 못 함.
  - **Adapter 폭발** — 한 Adapter가 4가지 케이스 다 처리. `switch(type)` 파열.
  - **테스트 매트릭스 폭발** — 하나의 Adapter가 4 타입 × 여러 시나리오 = 매트릭스 커짐.

기각. 그러나 이 시안이 명확히 드러낸 원칙: **"통합이 편한 게 아니라 spec이 흩어지는 게 편하다."**

---

## 2. 두번째 시안 (2-Port 이원)

발산의 성격에 따라 두 개 분리:

```java
interface HierarchySuggestionPort { // 구조 제안 (Layer / Axis)
    List<LayerSuggestion> suggestLayers(...);
    List<AxisSuggestion> suggestAxes(...);
}
interface ContentSuggestionPort { // 컨텐츠 제안 (Roadmap / Selection)
    RoadmapSuggestion suggestRoadmap(...);
    SelectionsSuggestion suggestSelections(...);
}
```

- 성격 분리: 구조 vs 컨텐츠.
- 장점: 시그니처 typed · Adapter 2개 · property 2개로 세밀 제어.
- 문제:
  - Layer와 Axis는 발산 형태(List<X>)는 같지만 context가 다름 (Layer=concepts만, Axis=Layer 컨텍스트 추가).
  - Roadmap과 Selection도 성격 다름 (헌법 안정 vs 판례 변동).
  - **정리해보면 성격 분리 자체가 모호** — 왜 Layer/Axis가 한 성격이고 Roadmap/Selection이 다른 성격인가? 답: "구조 vs 컨텐츠"는 후반 임의 분류.

기각. 그러나 여기서도 원칙 드러남: **"성격 분류는 사이클 단위로."**

---

## 3. 판단 지점 · Q1 — 4-Port로 갔던 첫 결정

이슈 #9(원안)가 채택한 안:

| Port | 목적 | 반환 |
| --- | --- | --- |
| `LayerSuggestionPort` | Layer 후보 발산 | `List<LayerSuggestion>` |
| `AxisSuggestionPort` | Axis 후보 발산 (Layer 컨텍스트) | `List<AxisSuggestion>` |
| `RoadmapSuggestionPort` | Axis 로드맵 발산 (통짜 ASCII 트리) | `RoadmapSuggestion` |
| `SelectionsSuggestionPort` | Axis 판례 리스트 발산 | `SelectionsSuggestion` |

각 Port는 사이클 1개와 대응. **1:1 매핑**.

**왜 4-Port가 매력적이었는가**:

1. **사이클과 Port 1:1** — 개념 단순. 새 사이클 = 새 Port. 기존 Port는 안 흔들림.
2. **Adapter도 1:1** — StaticLayerAdapter · LlmLayerAdapter · StaticAxisAdapter · ... 각 4개씩. 각 Adapter는 하나의 관심사만.
3. **property로 세밀 제어** — `thirdtool.suggestion.layer.provider=static` `thirdtool.suggestion.axis.provider=llm` 같은 개별 스왑 가능성.
4. **테스트 격리** — 각 Port를 별도 테스트. mocking·stub 단순.

**📌 실제 판단 (사용자 답변)**: **(a) 사이클 1:1 명료성** — 개념 단순함이 최고 가치. SRP 순응.

이 답의 함의:
- **"명료성 우선"의 함의**: 아키텍처 결정에서 확장성·성능·테스트 편의보다 **개념 지도의 단순함**이 우선. 이는 팀 규모가 작을수록·리뷰 부담이 클수록 중요한 원칙.
- **1:1 매핑의 강력함**: 사이클 개념이 확립되면 Port는 그것의 코드 표현. "이 사이클을 구현하려면 이 Port"라는 대응이 명료 · 신규 사이클 추가 시 신규 Port 신설이 자연스러움.
- **면접 답변 카드**: "왜 Port를 여러 개로 나눴나?" → "각 사이클이 독립적 개념이고 시그니처가 달라 통합의 이점 < 명료성의 이점. SRP + 사이클-Port 1:1 매핑."

---

## 4. 재편 신호 — 왜 6-Port로 넘어가야 했나 (D2 pivot)

`product-ai-suggestion.md`의 SUPERSEDED 표기:

> 이슈 #9의 4-Port(Layer/Axis/Roadmap/Selection)에서 `RoadmapSuggestionPort`·`SelectionsSuggestionPort`가 **한 번에 axis 전체 트리 통짜 생성** 방식이었음. **Roadmap/Selection Port를 outline + subtree 2단계로 분리 → 4-Port → 6-Port**.

Part 02에서 논의한 발산·수렴 짝짓기의 실전 확장:
- Roadmap은 사실 두 사이클 — outline(챕터 title 리스트) + subtree(각 챕터 하위 트리)
- Selection도 두 사이클 — outline(사례 컨테이너) + subtree(각 사례 하위)

**📌 참고**: Part 02 Q1의 답변("이전 통짜 생성 경험")과 정합. 4→6 재편은 (a)+(b)+(c) 복합 신호. 실측 근거는 통짜 생성이 UX 문제였다는 것.

---

## 5. 판단 지점 · Q2 — 왜 6-Port로 재편했을 때 Layer/Axis Port는 안 흔들렸나

재편 결과 Roadmap → 2개 (outline + subtree) · Selections → 2개 (outline + subtree)로 각 분열. 그러나 Layer/Axis Port는 그대로. 왜?

**가설 1**: Layer/Axis는 이미 "outline 성격" — List<X>로 여러 후보 반환 · subtree 개념 없음 · 재분열 필요 없음.

**가설 2**: Layer/Axis는 응답 크기가 작음 (5~7 items × 이름+rationale) · 통짜 방식이 지속 유효.

**가설 3**: Layer/Axis는 사용자 결정 후 저장이 원자적 · subtree 병렬화 이득 없음.

**📌 실제 판단 (사용자 답변)**: 3개 근거 모두 성립 — **(α) 이미 outline, (β) 응답 크기 작음, (δ) 결정 후 저장 원자적**.

이 답의 함의:
- **분열의 조건이 3개 동시 성립해야 함**: 어떤 사이클을 outline+subtree로 분열할지 판단할 때 3가지 관찰 (형태·크기·병렬 가능성)이 모두 필요.
- **Layer/Axis는 조건 미충족**: 이미 List<X> outline · 5~7 items 크기 · 원자적 저장 · 병렬 이득 없음 → 그대로 유지.
- **Roadmap/Selection은 조건 충족**: 통짜 트리 · 4~8k 토큰 · 챕터별 저장 가능 · 병렬 이득 60%.
- **면접 답변 카드**: "어떤 사이클을 세분화할지 어떻게 판단?" → "3조건 (형태 · 크기 · 병렬 가능) 모두 성립할 때. 하나라도 미충족이면 그대로 통짜 유지."

---

## 6. 판단 지점 · Q3 — Port 4개 아니라 12개, 24개는 왜 안 나갔나

극단으로 세분화 가능:

**12-Port 안** (사용자 액션 단위):
- LayerSuggestion (외부 후보)
- LayerRefinement (사용자 요청 후 재제안)
- LayerNaming (이름만 다시)
- AxisSuggestion / AxisRefinement / AxisNaming
- ... (Roadmap/Selection도 각각 세분)

**24-Port 안** (사용자 편집 세부):
- LayerReorder / AxisReorder / RoadmapChapterMove / SelectionCaseSwap ...
- 각 편집 액션마다 Port

두 극단은 왜 기각인가?

- **12-Port**: 사이클 확장(발산-수렴 안의 미세 액션)이 Port 세분화 근거가 안 됨. Refinement는 원 Suggestion Port의 재요청.
- **24-Port**: 편집은 도메인 액션(Aggregate 행위)이지 외부 의존 아님. LLM에게 "reorder 해줘"는 사용자 UI가 로컬로 처리. Port 필요 없음.

**핵심 원칙**: Port는 **"외부 의존 격리"** — 외부(LLM)에 무엇을 요청하는지의 인터페이스. 로컬 계산·편집은 Port 없이 도메인·Application이 담당.

**📌 실제 판단 (사용자 답변)**: **(a) 외부 의존 격리 — 핵심**. Port는 외부(LLM/DB/File)와의 계약만 · 로컬 편집·계산은 도메인/Application 영역.

이 답의 함의:
- **헥사고날의 원칙에 순응**: Port ≠ 사용자 액션. Port = 외부 세계와의 계약.
- **Reorder / Rename / Move 같은 편집은 Port 없음**: 도메인 Aggregate 행위 (`reorderLayers` · `renameLayer` 등). 외부에 아무 요청 안 함.
- **LLM에게 요청하는 것만 Port**: 발산·수렴 사이클에서 발산 절반(제안 생성)만 Port. 수렴(사용자 선택) · 편집(재배치·재명명) · 저장은 도메인.
- **12/24-Port 안이 왜 무너지나 결정적 근거**: 그 안들은 편집 액션을 Port로 만들려 함 · 그건 외부 의존 X · Port 자격 없음.
- **면접 답변 카드**: "Port를 어디까지 세분화?" → "외부 의존만 Port. 로컬 편집은 도메인. 이 경계가 헥사고날의 정의 자체."

---

## 7. 각 Port의 Context record 필드가 어떻게 결정됐는가

Q1~Q3의 답 근거를 실제 Port 시그니처에 어떻게 녹였는지 실측 정리.

### LayerSuggestionContext(facadeId, concepts, role)

- **facadeId**: 소유자 식별 · Static Adapter가 role 조회 hint로 활용 가능
- **concepts**: 사용자 정체성 스냅샷 · 프롬프트 embed 재료 · RoleDetector 이미 실행 상태
- **role**: `RoleDetector.detect(concepts)` 사전 실행 결과 · Adapter 재계산 방지 · nullable(감지 실패 시 Static은 generic 폴백)

**설계 원칙**: **Context는 재료의 스냅샷 · Port는 외부 호출**. Application Service가 도메인에서 재료 뽑아 Context 조립 → Adapter는 재료 조합만.

### RoadmapSuggestionContext(facadeId, layerId, axisId, axisName, layerName, concepts, role)

- 왜 이렇게 많은 필드? — Roadmap 생성이 axis의 학습 순서를 만드는데 컨텍스트가 부족하면 LLM 결과 품질↓
- `layerName` optional (Q2 답변 근거 · Layer가 관심사 분리로 이름만 hint) · nullable
- 원칙: **컨텍스트는 넉넉히**. LLM 프롬프트 embed 재료가 모자라는 것보다 남는 것이 나음.

### SelectionsSuggestionContext(..., roadmapOutline: List<String>, role)

- `roadmapOutline` **nullable** 처리 이유: Selection이 Roadmap 미생성 상태에서 독립 호출 가능해야 함 · 사이클 순서 유연화 (Q3의 사이클 단위 원칙과 정합)
- roadmapOutline이 제공되면 사례를 챕터별로 정렬 · 없으면 자유 사례 생성

### 공통 Contract (모든 Port)

- Adapter는 입력 Context 변경 금지 (read-only)
- limit ≤ 0 시 빈 리스트 · 예외 X (Adapter 안정 우선)
- existingNames의 null 원소 무시 (Adapter 관용)
- 실패 시 Adapter별 예외 · 상위 Service가 suggestionsAvailable=false로 변환 (ADR010)

---

## 8. stub Adapter 컴파일 실험의 실제 가치

`SuggestionPortsContractTest`의 stub Adapter (Story 11 AC):

```java
private static class LayerStub implements LayerSuggestionPort {
    @Override
    public List<LayerSuggestion> suggest(LayerSuggestionContext ctx, List<String> existing, int limit) {
        return List.of(new LayerSuggestion("UI", "화면 계층"));
    }
}
```

**이 stub이 왜 있어야 했나?**

- **Port 시그니처 확정을 실증**: Adapter가 실제로 implement 가능한지 컴파일러로 검증. 인터페이스만 있고 아무도 implement 안 하면 시그니처 실용성 불검증 상태.
- **미래 Adapter (Static/LLM) 진입 시 재작업 방지**: stub이 컴파일되면 Adapter 시그니처 확정 · 이후 Adapter 개발자가 그대로 implement 가능.
- **Story 11 AC의 정확한 의도**: "4 Port + Context/VO record 컴파일 통과 + **임의 Adapter stub 컴파일 실험 통과**".

**교훈**: 인터페이스만 있는 것과 stub까지 있는 것의 차이는 미래 부담에서 실제로 다름. Port 시그니처가 실제로 실용 가능한지 stub 컴파일로 못박음.

---

## 9. 미래 — Static/LLM 이원 · Cascade fallback

### 현재 상태 (0.0.3v 급행 완주)

- `StaticLayerSuggestionAdapter` — `@ConditionalOnProperty(matchIfMissing=true)` · 기본값 static
- 3 다른 Port(Axis · Roadmap · Selections)는 stub만 · Adapter 미도입
- `RoleDetector` + `SuggestionCatalogLoader` + `backend-developer.json` / `generic.json` 갖춤

### 이어질 시나리오 (M3~M5)

- **AS Epic 2 완주** (Static Adapter 4개) — Story 11의 3 Port 각각 Static Adapter
- **Role Catalog 확장** (Epic 3) — `planner.json` · `designer.json` · `problem-solver.json`
- **LLM Adapter 도입** (Epic 4) — `LlmLayerSuggestionAdapter` · Vertex AI Gemini
- **Cascade fallback** — LLM 실패 시 Static · Static도 실패 시 generic · generic도 실패 시 빈 리스트

### Cascade 아키텍처 (미래)

```java
@Component
public class CascadeLayerSuggestionAdapter implements LayerSuggestionPort {
    @Autowired @Qualifier("llm") LayerSuggestionPort llm;
    @Autowired @Qualifier("static") LayerSuggestionPort staticAdapter;

    @Override
    public List<LayerSuggestion> suggest(...) {
        try { return llm.suggest(...); }
        catch (Exception e) { return staticAdapter.suggest(...); }
    }
}
```

Port를 여러 개 만든 결정이 여기서 실증 — Static + LLM 두 구현체가 나란히 존재 · Cascade가 조율. Port가 통합이었다면 Cascade가 4 case switch로 복잡.

### 재검토 시점

- **6-Port 이관**: 이슈 #17이 outline/subtree 분열 Port 신설. 기존 Roadmap/Selection Port는 SUPERSEDED (또는 wrapper로 유지).
- **Adapter Cascade 정책** — 실패 threshold · Circuit Breaker (Resilience4J).
- **Role Catalog 학습 자산화** — 사용자별 role hint 저장 · Static이 학습된 role 카탈로그 우선 사용.

---

## 종합 · 면접 답변 카드

### 핵심 통찰 3개

1. **Port는 사이클 1:1 매핑** — 개념 명료성이 확장성·성능·테스트 편의보다 우선. SRP + 사이클-Port 대응.
2. **분열 조건은 3개 동시 성립** — 형태·크기·병렬 가능성 모두 성립할 때 outline/subtree 분열. Layer/Axis는 미충족 → 유지.
3. **Port ≠ 사용자 액션, Port = 외부 계약** — 로컬 편집은 도메인. 헥사고날의 정의 자체.

### 대안 3개 정리

| 대안 | 왜 기각 | 무엇을 남겼나 |
| --- | --- | --- |
| 1-Port 통합 | 타입 안전성 상실 · Adapter 폭발 | "spec이 흩어지는 게 편하다"는 원칙 |
| 2-Port 이원 | 성격 분류 모호 | "성격 분류는 사이클 단위로" 원칙 |
| 12/24-Port 세분화 | 편집 액션은 외부 의존 X | "Port = 외부 계약" 원칙 |

### 재검토 시점

- 사이클 개념이 흔들리면 Port 재편 (6-Port 재편 사례)
- 새 외부 의존 등장 시 새 Port (LLM 도입 시 각 Port에 새 Adapter · Port 자체는 그대로)
- 로컬 편집 요구가 커지면 Port 아닌 도메인 API로

---

*작성: 2026-07-02 | 편성: Part 03/06 | 사용자 실제 판단 반영 · Q1(사이클 1:1) · Q2(3조건 동시) · Q3(외부 격리 핵심)*


---

## 7. 이어질 §7~9 (답변 후 완성)

Q1~Q3 답변 주시면:

- §7. 각 Port의 **Context record 필드**가 어떻게 결정됐는가 (facadeId · role · concepts · axisName 조합)
- §8. **stub Adapter 컴파일 실험**의 실제 가치 (Story 11 acceptance criterion) — Port 시그니처 확정을 위한 실증
- §9. 미래: LLM Adapter 도입 시 6-Port · Static/LLM 이원 · Cascade fallback의 상호작용
