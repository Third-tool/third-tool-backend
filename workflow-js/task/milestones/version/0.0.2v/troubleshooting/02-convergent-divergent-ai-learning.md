# Deep-dive · Part 02 — 왜 AI 학습 사이클을 "수렴·발산"으로 묶었을 때 더 좋았는가

> **본 편의 성격**: AI 제안(4-Port · 6-Port로 재편) 아키텍처의 밑바닥 사고. **각 단계를 왜 divergent(발산) → convergent(수렴)의 짝으로 묶었는지**, 통합안(한 번에 다 생성)이 왜 무너졌는지.

---

## 0. 문제 상황 — "한 번에 다 생성" 안이 왜 죽었나

`product-ai-suggestion.md`의 SUPERSEDED 표기 근거를 다시 뜯어보면:

> 이슈 #9의 4-Port(Layer/Axis/Roadmap/Selection)에서 `RoadmapSuggestionPort`·`SelectionsSuggestionPort`가 **한 번에 axis 전체 트리 통짜 생성** 방식이었음. AI 품질 검증 결과 (1) 분량 과대 (2) 부분 수정 어려움 (3) 사용자 의견 반영 통로 부재 세 부족 확인.

이게 실측된 3 부족. 이걸 뜯어보면:

1. **분량 과대** — LLM이 통짜로 축 전체 트리(챕터 5개 · 각 챕터별 하위 개념 10개 · 그리고 판례 5개)를 한 번에 만들면 응답이 4~8k 토큰. 렌더링·리뷰·저장 모두 부담.
2. **부분 수정 어려움** — 사용자가 "챕터 3만 다시 만들어줘"라고 하려면 전체 트리를 새로 생성 요청 · 나머지가 함께 바뀔 위험. 안정된 부분이 흔들림.
3. **사용자 의견 반영 통로 부재** — 사용자가 "챕터 5는 좋지만 챕터 2가 뒤로 가야 해" 같은 재구성 피드백을 줄 인터랙션 지점이 없다. 통짜 결과는 accept-or-reject의 이분법.

---

## 1. 발상 전환 — "생성 사이클을 여러 개의 발산·수렴 짝으로 분리"

한 축의 학습 콘텐츠 완성을 다음 사이클로 재구성:

```
사이클 1: Layer 도출
  ㄴ 발산: LLM이 concepts→role→Layer 후보 5개 제안 (다양)
  ㄴ 수렴: 사용자가 그중 몇 개 선택 · 이름 편집 · 순서 조정 (좁힘)

사이클 2: Axis 도출 (Layer 컨텍스트)
  ㄴ 발산: 선택된 Layer마다 Axis 후보 여러 개 제안
  ㄴ 수렴: 사용자가 축 선택 · 편집

사이클 3: Roadmap outline (Axis 헌법)
  ㄴ 발산: 각 Axis마다 챕터 title 후보 outline 여러 안 제안
  ㄴ 수렴: 사용자가 outline 하나 선택 · 순서·이름 편집

사이클 4: Chapter subtree (Roadmap 챕터별)
  ㄴ 발산: 각 챕터마다 하위 개념 ASCII 트리 병렬 생성
  ㄴ 수렴: 사용자가 챕터별로 subtree 편집

사이클 5: Selections outline (Axis 판례 컨테이너)
  ㄴ 발산: Selection 컨테이너 후보 여러 안 제안
  ㄴ 수렴: 사용자가 컨테이너 확정

사이클 6: Selection subtree (사례별)
  ㄴ 발산: 각 사례 서브트리 병렬 생성
  ㄴ 수렴: 사용자가 사례 편집
```

이렇게 짝짓기 후 각 사이클을 **Port 1개**로 매핑 → 6-Port 아키텍처.

---

## 2. 왜 짝짓기가 좋은가 — 4개 관점

### 2.1 인지 관점 — "너무 많은 선택지" 문제

인간은 발산된 결과를 앞에 두고 수렴할 수 있어야 한다. 한 번에 200개 선택지가 나오면 결정 못 함 (analysis paralysis).

**짝짓기 효과**: 각 사이클에서 발산 개수를 5~7개로 제한 (Miller's Law) → 사용자가 인지 부담 없이 수렴 가능.

### 2.2 검증 관점 — "부분 검증"

통짜 생성은 전체 검증 or 전체 재생성뿐. 짝짓기는 **사이클별 완료 상태를 저장**해 다음 사이클 진입.

- Layer 확정 → 저장 → Axis 진입 (Layer는 다시 안 흔들림)
- Axis 확정 → 저장 → Roadmap 진입 (Axis는 다시 안 흔들림)
- ...

이 순서로 **stability 확보**. Part 01 §4에서 논의한 stability 경계 사고의 반복.

### 2.3 계산 관점 — 병렬화

Chapter subtree 생성 사이클(4)은 **챕터별로 독립적**. 각 챕터의 subtree는 서로 참조 안 함.

- 통짜 생성 시: 순차. 5분 대기.
- 짝짓기 + 병렬: 챕터 5개를 병렬 요청 → 1분 대기.

LLM 응답 시간이 자원 병목이므로 병렬화 여지는 UX에 직접 반영.

### 2.4 학습 관점 — "발산 vs 수렴이 학습 행위 자체를 만듦"

교육학의 **Bloom's Taxonomy** 관점:
- 발산 (Divergent thinking): 지식 확장 · 응용 · 창의
- 수렴 (Convergent thinking): 지식 조합 · 분석 · 평가

**핵심 통찰**: 사용자가 발산-수렴 사이클을 통과하는 것 자체가 **학습**. 통짜 결과를 accept하는 것은 학습이 아니라 소비. 짝짓기는 학습 사이클을 만드는 UX 프리미티브.

---

## 3. 판단 지점 · Q1 — "수렴·발산 분리"가 실제로 사용자 경험을 개선한 시점

사용자가 이 결정을 실증한 시점이 있었을 것. 아마 다음 중 하나:

- (a) 이전 버전에서 통짜 생성 후 "너무 커서 정리 못 하겠다" 경험
- (b) 다른 학습 도구(Notion AI, ChatGPT 등) 경험 · 통짜 vs 단계별 차이 실감
- (c) 자기 학습 실측 · 발산-수렴이 자연스러웠던 순간
- (d) 이론적 도출 (Bloom's · 인지 부담) 근거로만 결정

**📌 실제 판단 (사용자 답변)**: **이전 통짜 생성 경험**. 즉 (a) 실측이 근거.

이 답의 함의:
- 이 아키텍처 결정은 **이론(Bloom's/인지 부담)에서 도출된 게 아니라 실전에서 밟은 트랩의 대응**. 이론 근거는 사후에 재구성한 정당화(justification).
- **실전 경험 우선 원칙**: 이론상 가능성 검토(dry-run)로는 발견하기 어려운 UX 부담을 실측을 통해 발견 → 아키텍처 결정으로 정착.
- **면접 답변 카드**: "왜 이 아키텍처를 선택했나?" → "이전 버전에서 통짜 결과가 너무 커서 정리 불가한 UX 실측 → 원인 분리 (분량 · 부분 수정 · 피드백 통로 부재) → 각 원인을 사이클 분리로 해결." 실측 → 원인 분해 → 아키텍처 대응이라는 사고 흐름.

---

## 4. 판단 지점 · Q2 — 발산/수렴 짝짓기가 뒤집힐 시나리오

이 아키텍처가 최선이 아닐 미래 상황을 상상해보면:

- **고급 사용자** — "다 알려주고 빨리 확정하고 싶다" — 짝짓기가 오히려 느림
- **작은 축** — Roadmap 챕터 2~3개짜리는 통짜 생성이 나음 (짝짓기 오버헤드 > 이득)
- **일괄 편집** — 사용자가 여러 axis를 한 번에 생성/편집하는 경우
- **LLM 성능 폭발** — GPT-5나 다음 세대가 통짜 생성 품질 · 부분 수정 능력 모두 강해지면 짝짓기 근거 약화

**📌 실제 판단 (사용자 답변)**: **(β) 작은 축 자동 fallback** — 챕터 2~3개짜리 작은 축은 짝짓기 오버헤드가 이득을 넘어감. 자동 감지 후 통짜 모드 진입.

이 답의 함의:
- **"모두에게 이상적"이 아니라 "대다수 케이스에 이상적"** — 아키텍처는 dominant case를 우선하고 edge case에 fallback 제공.
- **"자동 감지"의 함의**: 사용자가 수동으로 짝짓기/통짜를 선택하는 UI가 아니라, 시스템이 컨텐츠 크기·성격 관찰 후 자동 결정. 사용자 인지 부담 없음.
- **감지 로직 후보** (이슈로 발행 후보):
  - Roadmap outline 응답에서 챕터 개수 < 3 → subtree 사이클을 통짜 모드로 스킵
  - Selection outline 응답에서 컨테이너 개수 < 2 → 사례 subtree를 통짜 병렬 (사이클 스킵)
  - 사용자가 이전 axis에서 통짜 편집을 선호했던 이력 → hint 반영
- **면접 답변 카드**: "이 아키텍처가 뒤집힐 조건?" → "작은 축·짧은 시나리오에서 짝짓기 오버헤드 > 이득. fallback으로 대응." 아키텍처가 하나의 정답인 척하지 않고 조건부 유효를 명시.

---

## 5. 판단 지점 · Q3 — 사이클 수는 왜 6개인가

6-Port 재편 결과 아래 6 사이클이 확정:

1. LayerSuggestion
2. AxisSuggestion
3. ChaptersOutline (Roadmap outline)
4. ChapterSubtree (Roadmap subtree, 챕터별)
5. SelectionOutline
6. SelectionSubtree (사례별)

다른 세분화 가능:

- **7 사이클**: Selection 컨테이너와 개별 사례 outline을 별도 분리 (컨테이너 · 사례 outline · 사례 subtree)
- **5 사이클**: Roadmap outline과 subtree를 통합 (챕터 title + 하위 개념을 한 번에 · outline 사이클 스킵)
- **4 사이클**: 원안 (Layer · Axis · Roadmap · Selection 4-Port · 각각 통짜)

**📌 실제 판단 (사용자 답변)**: **(c) 병렬화 이득** — subtree 사이클(챕터별 · 사례별)이 outline 사이클과 분리돼야 챕터/사례 수만큼 병렬 발동 가능.

이 답의 함의:
- **6은 stability나 인지 부담 관점이 아니라 계산 관점 (병렬화)에서 도출된 수**.
- **outline vs subtree 분리의 이유**: subtree는 병렬 발동에 최적. outline은 순서·구조가 있어 순차 필요. 두 성격이 분리돼야 각각 최적화.
- **LLM 응답 시간 vs 사용자 대기**: 4-Port(통짜)는 사용자 대기가 순차 합 (5분). 6-Port + 병렬은 outline 1분 + subtree 병렬 1분 = 2분. **~60% 시간 절감**.
- **컴퓨팅 아키텍처의 도메인 반영**: "이 사이클은 독립 병렬 가능"이라는 계산 특성이 Port 계약(interface)에까지 스며듬. Port 하나에 여러 axis subtree 병렬 요청 시그니처.
- **면접 답변 카드**: "Port를 왜 이 수로 나눴나?" → "각 사이클이 독립 병렬 가능한지 여부로 분리. 순차 필요한 outline과 병렬 가능한 subtree는 다른 Port. 병렬 발동으로 사용자 대기 시간 ~60% 절감."

---

## 7. 각 사이클 Port 시그니처가 왜 그런 모양인가

Q1~Q3 답변 근거를 도메인·infrastructure에 실제로 어떻게 녹였는지.

### LayerSuggestionPort (사이클 1)

```java
List<LayerSuggestion> suggest(
    LayerSuggestionContext context,   // (facadeId, concepts, role)
    List<String> existingLayerNames,   // dedupe hint
    int limit                          // 발산 개수 제한 (Miller's Law)
);
```

- **발산 개수 제한 `limit`**: 5~7 권장 · 인지 부담 방어
- **`existingLayerNames`**: 이미 있는 후보 제외 · 재제안 방지 (수렴 후 재발산 시 새 후보만)
- **`context.role`**: RoleDetector가 사전에 감지한 role hint 전달 · Adapter가 catalog 조회에 활용

### RoadmapSuggestionPort (사이클 3 · outline)

```java
RoadmapSuggestion suggest(RoadmapSuggestionContext context);
// RoadmapSuggestion(outline: List<String>, rationale: String)
```

- **List<String>만 반환**: 챕터 title outline만 · subtree는 별도 사이클
- **`rationale` nullable**: LLM 안정 생성 보장 안 됨 · Adapter가 empty로 응답 가능
- **context에 `axisName`, `layerName`, `concepts`, `role`**: 프롬프트 embed 재료 총집합

### SelectionsSuggestionPort (사이클 5)

```java
SelectionsSuggestion suggest(SelectionsSuggestionContext context);
// SelectionsSuggestionContext에 roadmapOutline: List<String> (nullable)
```

- **`roadmapOutline` nullable**: Roadmap 미생성 상태에서도 Selections 단독 호출 가능 (사이클 순서 유연화)
- **contract**: roadmapOutline이 제공되면 챕터별 사례로 정렬 · null이면 자유 사례 생성

### 미도입 Port (M4 이후)

- `ChaptersOutlinePort` / `ChapterSubtreePort` — Roadmap 재편 (이슈 #17)
- `SelectionOutlinePort` / `SelectionSubtreePort` — Selection 재편

**공통 패턴**: 각 Port는 **한 사이클 = 한 발산-수렴 짝의 발산 절반만 담당**. 수렴은 사용자 UI · Application Service 조율.

---

## 8. Static Adapter가 발산·수렴 짝을 어떻게 실현하나 (0.0.3v Tier 2 착지)

0.0.3v 급행에서 `StaticLayerSuggestionAdapter` 구현. 발산·수렴 아키텍처를 Static이 어떻게 실현하는가?

### 발산의 원천

- LLM은 Runtime에 발산 (탐색 · 랜덤 · creative)
- Static은 **미리 큐레이션된 catalog가 발산의 원천**
- `backend-developer.json` layers 절 5개 · `generic.json` 3개 · role별로 다른 발산 폭

### 수렴의 위임

- Adapter는 **수렴 담당 X** — 순수 제안만
- 수렴은 사용자 UI + Application Service (`existingLayerNames` dedupe · limit)
- Adapter는 "제안 리스트를 반환"까지만 · 이후는 상위가 조율

### 사이클 저장

- Adapter는 stateless — 사이클 상태는 도메인이 소유 (`LearningFacade.layers` 등)
- Adapter를 호출하기 전에 이전 사이클 결과가 도메인에 저장돼야 함 (Facade에 concepts, Layer에 name)
- 이는 헥사고날의 순수함 · Adapter는 외부 세계와의 계약, 도메인 상태는 안쪽에서 관리

**교훈**: Static Adapter가 완주하는 시점에 발산·수렴 사이클의 인프라 절반은 완성. LLM Adapter로 교체하면 발산 원천만 바뀌고 사이클 구조는 그대로.

---

## 9. 미래 대비 — 아키텍처가 살아있음을 어떻게 관찰할까

### 실측 metrics 후보

이슈 #26 (관측 지표) 이관 시 다음 metric 검토:

- **사이클별 완료율**: Layer 사이클 진입 사용자 / Axis 사이클 진입 사용자 (사용자가 어디에서 이탈?)
- **재발산 횟수**: 사용자가 사이클마다 몇 번 재요청하는지 (첫 발산으로 확정 vs 반복)
- **사이클 소요 시간**: LLM Adapter의 각 사이클 응답 시간 (병렬화 이득 실증)
- **fallback 발동율**: 작은 축 자동 통짜 모드 진입 빈도 (Q2 답변 근거)

### 재검토 시점

- **일괄 편집 요구 부상** — Q2 답변 (γ)이 재검토 신호. 여러 axis 편집이 반복적이면 batch API 검토.
- **LLM 성능 폭발** — GPT-5.x 이후 통짜 생성 품질 · 부분 수정 능력 검증. 짝짓기 무의미 판정 시 재편.
- **사용자 사이클 이탈** — 완료율이 사이클 후반부에 급격히 떨어지면 사이클 수(6→5) 축소 검토.

### 핵심 통찰 3개 요약

1. **아키텍처는 이론 아니라 실측에서 도출** — Q1 답변. 통짜 생성 실측이 결정 근거. 이론은 사후 정당화.
2. **아키텍처는 dominant case를 우선하고 edge에 fallback** — Q2 답변. 작은 축 fallback은 "이 아키텍처가 항상 최선이 아님"의 명시.
3. **컴퓨팅 특성이 도메인 인터페이스에 스며듬** — Q3 답변. 병렬 가능성이 Port 분리 근거. 도메인은 계산 특성을 인지해야 함.

---

*작성: 2026-07-02 | 편성: Part 02/06 | 사용자 실제 판단 반영 · Q1(실측 근거) · Q2(작은 축 fallback) · Q3(병렬화 이득)*


---

## 6. 이후 이어질 §7~9 (답변 후 완성)

Q1~Q3 답변 주시면:

- §7. 각 사이클 Port의 **인터페이스 시그니처**가 어떻게 결정됐는가 (Context 크기 · 응답 크기 · 실패 처리)
- §8. Static Adapter가 발산·수렴 짝을 어떻게 실현하나 (JSON catalog가 사이클을 어떻게 만족시키나) — 0.0.3v Tier 2 착지 결과 반영
- §9. 미래 대비: LLM Adapter 도입 시 짝짓기가 실측되면 어떻게 되나. 사용자·시스템 관점의 metrics 후보.

를 이어서 작성하겠습니다.
