## ADR023: Roadmap = 헌법, Selection = 판례로 용어를 재정의한다

- **상태**: Accepted
- **날짜**: 2026-07-02
- **관련**: `workflow/task/fix/brainstorming/version/0.0.2v/issue-08-sdd-terminology.md`, `workflow/task/pes/workspectrum/sdd/in-progress/product-learning-tower.md` (Layer/Roadmap/Selection), `workflow/task/pes/workspectrum/sdd/in-progress/product-ai-suggestion.md` (4-Port · 6-Port), `docs/DOMAIN.md` §1 Ubiquitous Language, `docs/adr/ADR021-axis-deck-full-integration.md` (선행 결정 — Axis Soft Delete 승격 + 축=덱 통합), [ADR003](ADR003.md) (Soft Delete 정책)

## 컨텍스트

0.0.2v D2 pivot(2026-07-02, `workflow/task/milestones/version/0.0.2v/milestone.md`) 결과, LT Epic 1·2 착지 후 남은 SDD·API·프롬프트 전반이 **"Roadmap"과 "Selection"이라는 두 용어**를 서로 다른 의미로 병용하고 있음이 드러났다.

- **사용자 정의** (사용자가 대화·회의에서 반복해서 사용) — Roadmap = 축의 학습 순서 청사진(헌법), Selection = 축 안에서 마주치는 구체 사례·문제(판례).
- **SDD/코드 정의** (2026-06 이전 초기 초안) — Roadmap = "축의 초기 학습 초안", Selection = "축의 합의된 최종 학습 안".

두 정의는 서로 반대 방향(Roadmap=초안 vs Roadmap=헌법)에서 만나 다음 문제를 유발한다.

1. **AI 4-Port 명명 충돌** — `RoadmapSuggestionPort` / `SelectionsSuggestionPort`가 어떤 정의를 따르는지에 따라 Adapter 프롬프트, 응답 파싱, 상위 오케스트레이션이 전부 달라진다. Static Adapter 구현체가 catalog JSON에 어떤 노드를 담을지도 결정되지 않는다.
2. **SDD 재작성 비용 누적** — `product-ai-suggestion.md`가 6-Port 재편(이슈 #17) 시 outline/subtree 분리를 요구했는데, outline이 Roadmap인지 Selection인지가 정의에 따라 뒤집힌다. 정의를 확정하지 않은 채로 스토리를 추가하면 이후 명명이 다시 바뀌는 비용을 반복 지불한다.
3. **도메인 코드의 미래 아이덴티티 미확정** — `product-learning-tower.md` Epic 3(SUPERSEDED, 이슈 #15/#16으로 재편) 이후 `axis_roadmap_node`, `axis_selection_node`가 도입될 때 두 테이블·엔티티가 각각 무엇을 저장하는지가 정의에 따라 반대가 된다.
4. **UX/문구 정합** — FE의 "학습 흐름", "예제/사례 모음" 같은 UI 문구는 어느 정의를 따르든 하나로 고정되어야 재작업이 없다.

정의 확정을 미루면 코드 명명 회귀가 반복되고, 실제 이슈 #15/#16(노드 스키마) 진입 시점에 이미 잘못된 이름으로 커밋된 API·엔티티가 남는다. 0.0.3v 급행 전 확정이 필요하다.

## 결정

**사용자 정의를 채택**한다 (Roadmap = 헌법, Selection = 판례). 초기 SDD 정의(Roadmap=초안, Selection=최종안)는 폐기.

### D1. Roadmap = 헌법 — 축의 학습 순서 청사진

- Roadmap은 **하나의 Axis 안에서 학습이 진행되는 순서 뼈대**를 담는다. 챕터 title 순서 리스트(outline)와 각 챕터의 subtree(ASCII 트리 · 하위 개념)로 구성된다.
- "헌법"인 이유는 **상위 규범** 성격에 있다 — 축의 학습 흐름을 통제하는 기준이며, 개별 응용은 이를 참조한다.
- outline은 축이 살아 있는 동안 상대적으로 안정적(자주 변하지 않음)이어야 한다.
- 도메인 엔티티(이슈 #15에서 도입 예정): `AxisRoadmapNode` (챕터 노드 · Aggregate Root는 `LearningAxis`).
- AI Port: `RoadmapSuggestionPort` / (이슈 #17의 6-Port에서 `ChaptersOutlinePort` + `ChapterSubtreePort`).

### D2. Selection = 판례 — 축 안의 구체 사례·응용

- Selection은 **하나의 Axis 안에서 학습자가 마주치는 구체 사례·문제·응용**을 담는다. Roadmap의 각 챕터에 대응되는 응용 예제 리스트.
- "판례"인 이유는 **개별 사례 축적** 성격에 있다 — Roadmap(헌법)을 실제 상황에 적용한 실증 사례들이 누적된다.
- Selection은 상대적으로 자주 추가·수정된다 (판례가 판례법 체계에 계속 쌓이는 것과 유사).
- 도메인 엔티티(이슈 #16에서 도입 예정): `AxisSelection` (컨테이너) + `AxisSelectionNode` (개별 사례 노드).
- AI Port: `SelectionsSuggestionPort` / (이슈 #17의 6-Port에서 `SelectionOutlinePort` + `SelectionSubtreePort`).

### D3. Layer는 별개 상위 계층 — Roadmap/Selection과 혼동 금지

- Layer(LT-E2-S1)는 **Facade 안에서 여러 Axis를 그룹핑하는 상위 계층**. Roadmap/Selection과 다른 축의 개념이다.
- Roadmap/Selection은 **하나의 Axis 내부** 구조, Layer는 **여러 Axis의 상위** 그룹핑.
- 명명 오해 방지를 위해 API·엔티티에 다음 표기를 통일 (D4).

### D4. 코드·API·SDD의 표기 규칙

| 개념 | 코드 명명 | 표기 예시 |
| --- | --- | --- |
| 헌법(순서 청사진) | `Roadmap`, `roadmap_node`, `RoadmapSuggestionPort` | `axis_roadmap_node`, `RoadmapSuggestion.outline` |
| 판례(사례·응용) | `Selection`, `Selections`, `selection_node`, `SelectionsSuggestionPort` | `axis_selection`, `axis_selection_node`, `SelectionsSuggestion.selections` |
| 상위 그룹핑 | `Layer`, `learning_layer`, `LayerSuggestionPort` | `LearningLayer`, `Uncategorized` (default) |

- 한국어 문구 (UX / DOMAIN.md 설명)는 다음을 우선한다:
  - Roadmap → "학습 흐름", "구조 청사진", "챕터 순서"
  - Selection → "사례 모음", "적용 판례", "구체 예제"
  - Layer → "계층", "묶음"
- UI 문구 상세는 [`docs/ux/wip-language.md`](../ux/wip-language.md)에 후속 반영 (본 ADR 스코프 밖).

### D5. SUPERSEDED 표기 정책

- 기존 SDD·문서 중 초기 정의(Roadmap=초안, Selection=최종안)를 사용한 부분은 **SUPERSEDED** 헤더를 추가하고 본 ADR을 인용해 표기.
- `product-ai-suggestion.md`, `product-learning-tower.md`의 SUPERSEDED 표기는 이미 있음(6-Port 재편, Epic 3 뒤집힘). 본 ADR은 그 표기의 **용어 근거**로 인용된다.

## 결과 (Consequences)

### 긍정적

- **명명 확정 → 이후 이슈 #15~#19 진입 시 API/엔티티 이름 재작업 없음** — `axis_roadmap_node` / `axis_selection_node`의 의미가 코드·SDD·프롬프트에서 일관.
- **AI Adapter 프롬프트 embed 안정화 (이슈 #19)** — Static/LLM Adapter가 "Roadmap = 헌법" 정의를 프롬프트에 embed할 수 있음. concept-spec.txt few-shot도 확정된 정의로 작성.
- **사용자 대화 정합** — 사용자가 "Roadmap 만들어줘"라고 할 때 시스템·프론트·백엔드가 같은 개념(헌법)을 이해.
- **AS-E1(Story 11) 스켈레톤의 명명 확정** — `RoadmapSuggestion.outline` / `SelectionsSuggestion.selections` 필드명이 정의와 맞음 (0.0.3v 급행 PR #198로 이미 커밋됨).

### 트레이드오프 / 부정적

- **초기 SDD 참조 문서 유효성 축소** — 2026-06 이전 문서 중 "Roadmap = 초안" 표현이 남은 것은 SUPERSEDED 표기 필요. 문서 정합 부담.
- **AI 프롬프트 재작성 부담** — Static Adapter catalog(이슈 #14, Tier 2 예정)에 이미 "Roadmap = 초안" 정의로 초안이 만들어져 있다면 재작성.
- **UX 문구 재정합 부담** — FE 팀의 기존 문구가 초기 정의를 따랐다면 재작성. `docs/ux/wip-language.md`에서 별도 추적.

## 대안 비교

| 대안 | 장점 | 거부 사유 |
| --- | --- | --- |
| **A. 사용자 정의 채택 (채택)** | 사용자 대화·요구와 정합. 헌법/판례 은유가 강력 (개념 관계·성격이 명확) | 초기 SDD 참조 문서 SUPERSEDED 표기 부담 |
| B. 초기 SDD 정의 유지 (Roadmap=초안, Selection=최종안) | 초기 문서 재작성 없음 | 사용자와 어긋남. 이후 대화·요구 커뮤니케이션 비용 계속 |
| C. 제3의 용어 도입 (Draft / Approved 또는 Blueprint / Case) | 두 정의 회피 | 익숙한 사용자 언어(Roadmap/Selection) 상실. 도메인 전문성 낮음 |
| D. 명명 확정 미룸 | 현재 코드 즉시 변경 없음 | 이슈 #15~#17 진입 시점에 정의 정착 강요됨. 재작업 회귀 표면 유지 |

## 다시 검토할 시점

- **이슈 #15 (Roadmap 노드 스키마)**·**#16 (Selection 노드 스키마)** 진입 시 — 본 ADR 정의를 실제 스키마 컬럼명·엔티티명에 적용. 실측 후 명명이 실무에 맞지 않으면 재검토.
- **AI 프롬프트 embed 결과 관찰 (이슈 #19)** — LLM이 "Roadmap = 헌법" 프롬프트로 안정적인 outline을 생성하는지, "Selection = 판례"로 자연스러운 사례를 생성하는지 확인 후 명명 유지/변경 결정.
- **UX 사용자 테스트 (M8 이후, 0.1.0v 릴리스 대비)** — 사용자에게 "학습 흐름"과 "사례 모음"이 자연스럽게 이해되는지 확인. 이해 실패 시 UX 문구는 조정 가능하되 백엔드 명명(Roadmap/Selection)은 본 ADR 기준으로 유지.
- **Layer × Roadmap × Selection의 상호 참조 관계** — Layer 스코프 리뷰(이슈 #14, Review 재설계)가 Roadmap/Selection 진행률을 어떻게 집계하는지 결정될 때 개념 경계 재확인.
