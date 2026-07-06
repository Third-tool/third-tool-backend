# Deep-dive · Part 01 — Layer→Axis→Roadmap→Selection 계층이 왜 이 모양으로 도출됐는가

> **본 편의 성격**: 실측 트러블슈팅([`troubleshooting.md`](./troubleshooting.md))과 별도로, **의사결정 그 자체를 뜯어보는 회고**.
> 코드가 최종적으로 착지한 모양(Facade → Layer → Axis → Roadmap/Selection)이 자연스러워 보이지만, **경로 상에는 여러 갈림길**이 있었고 각 갈림길마다 미묘하게 다른 세계가 열렸다.
> 실제 실수를 고친 게 아니라, "지금 착지한 형태가 정말 최선인가"를 뜯어보는 편이다.

---

## 0. 왜 이 회고가 필요한가

`docs/DOMAIN.md`·`product-learning-tower.md`·`issue-04/05/15/16`을 지금 시점에서 뒤로 재구성해보면 하나의 매끄러운 이야기(concept → concepts[] → Layer → Axis → Roadmap/Selection)로 읽힌다. 그러나 D2 pivot(2026-07-02) 시점의 실측 브레인스토밍에는 다음 신호가 뒤엉켜 있었다:

- 사용자 대화: "저는 백엔드지만 시스템 설계도 하고, 도메인 언어도 다뤄요" → concepts 다중화 시그널
- 카드 사용 실측: 사용자가 "카드 만들 때 축이 인식이 안 되고 화면 나가면 사라진다" → Axis Soft Delete 시그널 (ADR021로 이어짐)
- AI 회의 결과: "축 로드맵과 판례 사례가 뒤섞여 있어 프롬프트가 커진다" → Roadmap/Selection 분리 시그널
- SDD 재작성 검토: "계층이 3단(Facade→Axis→Topic)인데 축이 너무 많아지면 그룹핑이 필요하다" → Layer 시그널

이 4개 신호가 각각 다른 도메인 형상을 요구할 수 있었다. 지금 착지한 형상은 그중 **하나의 조합**일 뿐. 이 편은 그 갈림길들을 다시 벌려서 재확인한다.

---

## 1. 원본 형상 (M2 pivot 전)

```
LearningFacade  {  concept: String,  axes: List<LearningAxis>  }
   │
   └── LearningAxis  {  name, topics: List<AxisTopic>  }
           │
           └── AxisTopic  {  name, description, materials  }
```

**특징**:
- concept이 단일 문자열. 사용자의 "여러 관점" 표현 불가.
- Facade가 axes를 직접 소유. 축이 많아지면 UI에서 flat list로 노출.
- AxisTopic이 축 학습의 유일 단위. "학습 순서(로드맵)"와 "사례(판례)"가 topic 하나에 뭉쳐있음.

---

## 2. 첫 아이디어 (직관적이지만 무너진 안)

concepts와 Layer 문제를 각각 별개 대응하려는 첫 시안이 있었다.

**시안 A** — concepts만 다중화 (Layer는 도입 X):
```
LearningFacade  {  concepts: List<String>,  axes: List<LearningAxis>  }
```

- 장점: 변경 최소. 축 UI는 flat 유지.
- 문제: 사용자가 "백엔드 + 시스템설계 + 도메인언어" 3개 concepts를 넣으면 축이 30개 넘게 늘어나는 시나리오가 실측 뻔했음. flat list UX 붕괴.

**시안 B** — Layer만 도입 (concepts는 여전히 단일):
```
LearningFacade  {  concept: String,  layers: List<Layer>  }
       └── Layer  {  name,  axes: List<LearningAxis>  }
```

- 장점: 그룹핑 해결.
- 문제: concept이 단일이면 사용자가 여러 개 관점을 원할 때 여러 facade를 만들어야 함. 그런데 facade는 v1당 1개 정책. → 정책 자체가 파열.

**교훈**: concepts 다중화와 Layer 도입은 **한 세트**여야 한다. 한 축만 손대면 다른 축이 무너진다.

---

## 3. 판단 지점 · Q1 — Layer의 위치 (Facade 안 vs 별도 BC)

지금 착지한 형태는 `LearningFacade BC 안의 domain/model/LearningLayer`. 그러나 다음 대안이 있었다.

| Option | 배치 | 근거 | 함의 |
| --- | --- | --- | --- |
| **A (채택)** | LearningFacade BC 내부, `LearningLayer.java` | Layer는 Facade의 자식 · lifecycle 종속 · 응집도 부족해서 별도 BC 승격 못 함 | Facade Aggregate가 concepts + layers + axes 다 소유 → 트랜잭션 단순 · Aggregate 크기 증가 |
| B | 별도 BC (`LearningTaxonomy` 등) | Layer가 여러 Facade에서 재사용될 가능성 (템플릿 개념) | BC 경계 명확 · 이벤트 협력 필요 · 응집도 부족 |
| C | Layer 없이 tag 방식 | Axis에 `layerName: String` 태그. Layer는 순수 조회 파생 | 스키마 최소 · 이름 변경/삭제 시 모든 axis 업데이트 필요 · 순서·중복·softDelete 정책 없음 |

**결정 근거 (SDD Epic 2 §"Layer 위치")**: Option A. "Layer는 LearningFacade의 자식이며 별도 BC로 승격할 응집도 부족."

**📌 실제 판단 (사용자 답변)**: **응집도 부족 (유직어)** — Layer는 Facade가 없으면 존재 이유가 없다. 스스로 lifecycle을 가진 도메인이 아니며, "무엇을 담을 축들의 상위 그룹"이라는 정의가 Facade에 종속. Facade에서 떨어져 나가면 Layer는 껍데기.

**이 답의 함의**:
- **BC 분리 기준으로 "재사용 가능성"보다 "독립 lifecycle"이 앞선다** — Option B(별도 BC)는 미래 재사용 가능성이 열려있어도 현재 lifecycle 종속이 강해 응집도 부족으로 판정.
- **트랜잭션 경계와 정합**: Facade 변경 · Layer 변경이 한 트랜잭션에서 자주 함께 일어나므로 (예: 사용자 학습 개편) Aggregate 내부에 두는 편이 자연스러움.
- **미래 유연성**: 재사용 니즈가 실증되면 그때 BC로 승격 (선-확실-Option A → 후-필요시-Option B). YAGNI 원칙에 순응.
- **비교**: Option C(tag 방식)는 순서·중복·softDelete 정책 부재로 처음부터 기각. 이는 도메인이 담을 규칙이 있으면 그것을 표현할 자리(Aggregate)가 필요하다는 인식.

**면접용 질문 카드**: "Aggregate를 별도 BC로 승격할 기준은?" → "재사용 가능성이 아니라 독립 lifecycle. lifecycle이 부모에 종속되면 응집도 부족으로 BC 승격을 미룬다."

---

## 4. 판단 지점 · Q2 — Roadmap / Selection의 위치

이슈 #15/#16이 정의하려는 `axis_roadmap_node` / `axis_selection_node`는 **AxisTopic을 폐기**하고 두 개 별도 스키마로 재편. 다음 대안이 있었다.

### Option A (채택 예정, ADR023 근거) — Axis 하위 두 원자 축

```
LearningAxis  {  name,
                 roadmap: AxisRoadmap  { nodes: List<AxisRoadmapNode> },
                 selection: AxisSelection  { nodes: List<AxisSelectionNode> }  }
```

- **의미**: Axis 하나에 "학습 순서 청사진(헌법)"과 "구체 사례(판례)" 두 축이 나란히 붙는다.
- **장점**: 둘의 개념적 성격이 다르므로 (헌법 vs 판례) 별개 스키마·별개 AI Port로 처리. 프롬프트 embed 안정.
- **단점**: Axis 하나에 두 개 자식 aggregate. 조회 시 fetch join 부담.

### Option B — 여전히 AxisTopic 유지, Roadmap/Selection은 tag

```
LearningAxis  {  name,
                 topics: List<AxisTopic>  { name, kind: ROADMAP | SELECTION }  }
```

- **의미**: 하나의 컬렉션에 kind enum으로 구분.
- **장점**: 스키마 최소 · 기존 AxisTopic 재활용.
- **단점**: 두 종류가 실제로 다른 성격(헌법 vs 판례)인데 한 테이블에 섞임 · 프롬프트가 kind 조건부 로직 · 사례가 챕터에 대응된다는 관계 표현 부족.

### Option C — Roadmap만 정식화, Selection은 나중

```
LearningAxis  {  name,
                 roadmap: AxisRoadmap  { nodes },
                 topics: List<AxisTopic>  }  // topic = 판례 대체
```

- **장점**: 절반만 도입 · MVP.
- **단점**: 판례를 topic으로 계속 부르면 어휘 혼동 유지 · ADR023 정합 실패.

### Option D — Roadmap과 Selection을 Layer 레벨에 두기

```
LearningFacade
   └── LearningLayer  {  roadmap, selections  }  // Axis 없이
```

- **의미**: 축을 없애고 Layer가 학습 단위. Layer 하나가 헌법+판례 갖는 구조.
- **장점**: 계층 하나 줄어듬 (Facade→Layer→Roadmap/Selection).
- **단점**: 축의 재활용성 상실 (한 축이 여러 Layer에 속할 수 없음). AxisSuggestionPort 무의미해짐.

**📌 실제 판단 (사용자 답변)** — Option A 채택. 4개 근거를 모두 인정했지만 **가장 결정적인 것은 다음 5번째 근거**:

> **"둘이 연관관계가 그 한 영역에 대해서 어느정도 학습해야 하는지를 이 2개의 조합이 같이 있어야 확인이 가능하다.**  
> **이 2개를 합쳐놓고 진행했던 옛날 버전에서는 너무 무자비하게 업데이트 빈도가 많았는데, 가장 중요한 roadmap을 따로 떼어두고, selections를 위주로 업데이트 하니까, 해당 영역의 관련 topic에 대한 학습 정도를 확인하기 편하기 때문."**

이건 (a)~(d)의 상위 개념이다 — **stability boundary(안정성 경계) 결정**이다.

### 실측 근거 재정렬 (사용자 답변 기반)

| 순위 | 근거 | 성격 |
| --- | --- | --- |
| ① | **stability 분리 · 관찰성** — Roadmap(변경 적음)과 Selection(변경 많음)을 분리하면 학습 진행도를 Selection의 변화로 관찰 가능. 합쳐 두면 어떤 변경이 "구조 편집"이고 어떤 게 "학습 축적"인지 뒤섞임 | **실전 관찰** |
| ② | 헌법 ↔ 판례 대응 관계 (챕터 ↔ 사례) | 개념적 대칭 |
| ③ | AI 프롬프트 컨텍스트 (axisId가 부모로 필요) | 인프라 요구 |
| ④ | 관심사 분리 (Layer=그룹 · Axis=학습 단위) | 아키텍처 원칙 |
| ⑤ | 축=학습의 자연 단위 (Option D 기각) | 도메인 인식 |

**결정적인 통찰(①)의 함의**:

- **"업데이트 빈도"가 도메인 경계의 신호**가 될 수 있다. 두 데이터가 같이 담겨 있는데 update rate가 크게 다르면 (한쪽은 매일, 한쪽은 월 1회), 그건 **분리하라는 시그널**.
- 이는 CQRS의 write/read 분리 근거와 유사한 사고 — 접근·변경 패턴이 다른 데이터는 다른 저장소·다른 aggregate·다른 API로.
- **학습 진행도의 관찰성**: Selections가 자주 갱신될수록 "이 축에 대해 학습이 진행 중"이라는 신호. Roadmap이 그대로면 "구조는 안정, 사례가 쌓이는 중" 판정 가능.

**면접용 질문 카드**:
- "두 데이터를 한 테이블에 넣을지 분리할지 어떤 신호로 판단?" → "update frequency 차이가 크고 access pattern이 다르면 분리. CQRS의 원칙과 유사"
- "도메인 경계는 언제 확정?" → "실전 사용 중 관찰 어려움·update 병목·개념 혼동이 축적되면 분리 신호. 초기 완벽 설계보다 사용 후 재편이 진실"

---

---

## 5. 판단 지점 · Q3 — 왜 `concepts[]`를 Facade 레벨에 (Layer 레벨이 아니라)

또 다른 갈림길이 있었다. 사용자의 concepts는 Facade에 붙어야 할까, Layer에 붙어야 할까?

### Option A (채택) — `concepts[]`가 Facade 필드

```
LearningFacade  {  concepts: List<String>,  layers: List<LearningLayer>  }
```

- concepts는 "내가 누구인가"의 자기 정의. Facade 전체를 정의하는 태그.
- 예: `["백엔드", "시스템설계", "도메인언어"]` = 사용자 자체 정체성.

### Option B — 각 Layer마다 concepts

```
LearningFacade  {  layers: List<LearningLayer  { concepts: List<String>, axes }>  }
```

- Layer마다 다른 concepts 세트. 예: 백엔드 Layer=[Spring, JPA], UI Layer=[React, Figma].
- 장점: 세분화된 컨텍스트. AI 프롬프트가 Layer별로 다른 concepts 참조.
- 단점: 관심사 분리 파열 — Layer는 "그룹"이 정의, "무엇에 대한 그룹인지" 정보는 Facade에 있어야 자연스러움.

### Option C — concepts는 컨셉 자체 도메인 (`concepts` BC 분리)

```
LearningFacade  { conceptIds: List<Long> }
Concept  { name, ... }
```

- concepts를 재사용 자산 (여러 사용자가 공유하는 정규화된 taxonomy).
- 장점: 검색·추천·통계에 강함.
- 단점: v1 스코프 초과. 사용자 정의 자유 문자열이 요구되는데 정규화 강제는 UX 훼손.

**실제 결정 (Option A)** — 근거:

- **사용자 정체성 = Facade 자체 정의**. concepts는 "이 Facade가 무엇인가"의 답. 개별 Layer에 붙으면 "이 사용자가 여러 관점의 총합이다"라는 표현 불가.
- **RoleDetector가 concepts 전체를 봐야 함**. Layer마다 concepts가 다르면 detect 로직이 복잡해지고 "이 사용자의 지배적 role"이 정의되지 않음.
- **UI 단순화**: 사용자가 개인 chip 입력창 하나로 concepts 관리. Layer별 chip 관리는 UX 폭발.

**면접용 질문 카드**: "사용자 태그를 어디에 붙일지 결정 기준?" → "정체성이 태그의 대상인지, 그룹의 대상인지로 판단. 정체성이면 상위 Aggregate에, 그룹이면 각 그룹에."

---

## 6. Layer의 default "Uncategorized" 자동 발행 · 삭제 금지 정책

`LearningFacade.create()`가 자동으로 Uncategorized Layer 1개 생성. 그리고 이 Layer는 사용자가 명시적으로 삭제할 수 없다 (`LEARNING_LAYER_HAS_ACTIVE_AXES` 409, default 보호).

### 왜 자동 발행?

- **legacy `addAxis(name)` 경로**가 여전히 존재 (backward compat). 이 경로는 layer를 지정하지 않는다. 기본 정착지가 필요.
- **백필의 정합**: V19 백필이 기존 axes를 어디로 이관할까? "각 facade마다 하나의 default"가 유일한 답. `create()`가 새 facade에도 같은 상태를 만들어야 백필과 신규가 대칭.
- **UX 진입**: 새 사용자가 "Layer 만들고 그 다음 축 만들기" 2단계보다 "일단 축 만들기" 1단계가 나음. 그러려면 시작점에 이미 Layer가 있어야.

### 왜 삭제 금지?

- **자동 라우팅의 앵커**. 삭제 허용 시 legacy `addAxis(name)`이 이관할 곳이 없어짐 → 예외 or hidden 재발행 필요. 둘 다 UX 이질.
- **불변식 강제**: "Facade는 최소 1개 활성 Layer를 갖는다" 도메인 규칙. 시스템 신뢰의 앵커.

### 대안 (기각)

- **자동 발행 없이 사용자가 첫 Layer 만들 때까지 axes 못 만들게**: 강제성 · UX 부담.
- **삭제 시 자동 재발행**: 사용자 명령이 뒤집힘 · 예측성 파열.
- **default Layer를 hidden 처리**: 코드 특수 케이스 증가 · 삭제 UI에서 눈에 안 보이면 삭제 시도 자체 안 함 (문제 회피지 해결이 아님).

**교훈**: **"불변식 앵커"는 코드로 강제**. 도메인 규칙(최소 1 Layer)이 있으면 그것을 지킬 앵커(default)를 명시하고, 앵커 자체를 보호(삭제 금지).

---

## 7. 결과적 형상의 3-level 대안 · Layer/Axis 통합안이 왜 무너지는가

지금 착지한 형상: `Facade → Layer → Axis → Roadmap/Selection`. 3.5 계층 (Layer가 얇음).

### 통합안 (기각) — Layer=Axis 통합

```
Facade → LayerAxis  {  name, roadmap, selection  }
```

- 계층 하나 줄여서 flat.
- **왜 무너지나**:
  - **Layer의 의미 상실**: Layer가 "학습의 자연 단위"까지 되면 group과 unit이 뒤섞임. 사용자가 "백엔드"를 Layer로 만든 순간 그 아래 "Spring", "JPA" 등을 어떻게 표현할지 불명 (또 다른 Layer? 아니면 Roadmap 챕터?).
  - **Roadmap/Selection의 대응 관계 파열**: 챕터-사례 대응은 Axis 하나 안에서만 성립. Layer가 축이 되면 대응 관계 정의가 재구성 필요.
  - **AI Port 재설계**: LayerSuggestionPort와 AxisSuggestionPort 역할 통합 필요. Port 계약 파열.
- **결론**: 3.5 계층이 짐이지만, 도메인 의미(그룹핑 vs 학습 단위)를 분리 유지하는 대가로 감수.

### Roadmap+Selection 별도 Aggregate로 승격 (미래 방향?)

지금은 이슈 #15/#16이 `AxisRoadmapNode` / `AxisSelectionNode`를 Axis 자식 Entity로 정의. 미래에는:
- Roadmap이 여러 Axis에서 공유되는 "표준 커리큘럼"이 될 수도 (재사용).
- 그 시점에 Roadmap을 별도 Aggregate로 승격 (Axis는 Roadmap의 인스턴스 참조).
- 현재는 실증되지 않아 Axis 자식으로 유지.

---

## 8. 지금 착지한 형상의 미래 부담

**Aggregate 비대화**:
- `LearningFacade`가 concepts + layers + axes(via layer) 다 소유. 조회 시 fetch join 부담.
- 대응: 조회 전용 SummaryRow 도입 (`docs/PACKAGE.md` 관행). 실제 depth 조회에만 aggregate 로드.

**스키마 4개 유지**:
- learning_facade + learning_facade_concept + learning_layer + learning_axis (+ axis_topic + material 등).
- V16~V19로 스키마 진화. 별도 릴리스에서 다음 재편 필요 시 V20~부터.

**backward compat 부담**:
- `learning_facade.concept` NOT NULL 컬럼 유지 (`concepts[0]`과 동기화).
- `LearningAxis.facade_id` FK 유지 (layer 경유가 blessed이지만 legacy 참조 안 끊음).
- **후속 3-phase 필요**: 컬럼/FK DROP은 별도 릴리스에서.

**Layer가 얇음**:
- 현재 Layer는 name + displayOrder + softDelete만 소유. "학습 통계·진도"가 붙으면 Layer가 두꺼워지고 Aggregate 재편 필요 신호.

---

## 9. 종합 · 면접 답변 카드

### "왜 이 계층으로 만들었나?" 총평

- **concepts 다중화 + Layer 도입은 한 세트** — 각각 별개로 대응하면 다른 축이 무너진다. 실측 근거는 "축 flat list UX 붕괴" + "v1당 1 Facade 정책 파열".
- **Layer는 응집도 부족으로 별도 BC 아닌 Aggregate 내부** — 재사용 가능성보다 lifecycle 종속을 앞세운다.
- **Roadmap/Selection은 Axis 하위 두 원자 축** — 헌법-판례 대응 + stability 경계(update frequency 차이) + AI 프롬프트 컨텍스트 요구.
- **`concepts[]`는 Facade 레벨** — 사용자 정체성의 대상이 그룹 아닌 전체 자체.
- **default Uncategorized 삭제 금지** — 불변식 앵커 코드 강제.

### 핵심 통찰 3개

1. **응집도 vs 재사용성**: BC 승격 기준은 재사용 가능성이 아니라 독립 lifecycle. (Layer 결정)
2. **stability 경계**: update frequency 차이가 도메인 분리 신호. Roadmap(안정) + Selections(변동) 분리로 학습 진행도 관찰. (Roadmap/Selection 결정)
3. **불변식 앵커**: 도메인 규칙("최소 1 Layer")이 있으면 그 앵커를 코드로 강제 + 앵커 자체를 보호. (default Uncategorized 결정)

### 재검토 시점

- Layer가 두꺼워지면 별도 BC 승격 검토 (통계/진도 도메인 붙는 시점).
- Roadmap이 재사용 자산이 되면 Axis 자식에서 별도 Aggregate로 승격.
- concepts 정규화 요구 부상하면 `Concept` BC 신설 (v1 out of scope).

---

*작성: 2026-07-02 | 편성: Part 01/06 | 사용자 실제 판단 반영 · Q1(응집도) · Q2(stability 경계) · Q3(정체성 대상)*

