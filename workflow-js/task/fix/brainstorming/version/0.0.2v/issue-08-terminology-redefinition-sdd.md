# Issue: 용어 재정의 — roadmap/selections 사용자 정의로 SDD 개정

## 배경
사용자 지시 (사용자 정의):
- **roadmap = 헌법**: 원칙·spine·금지·판단 축. axis당 1개.
- **selections = 판례**: 상황별 선택 조합·variant. axis당 N개.

현재 `sdd/in-progress/product-ai-interactive-roadmap.md`는 같은 두 단어를 **완전히 다른 뜻**으로 사용 중:
- 문서상 `roadmap` = AI가 만든 **축 초안**
- 문서상 `selections` = 사용자 합의 **최종안 (diff 결과)**

용어가 조용히 충돌하면 이후 도메인·Port·서비스·프롬프트 명명이 모두 오해된다. 사용자 정의를 표준으로 확정하고 기존 문서를 전면 개정한다.

## 조사 결과 — 충돌 지점

| 대상 파일 | 현재 사용 | 사용자 정의 | 개정 필요 |
|---|---|---|---|
| `sdd/in-progress/product-ai-interactive-roadmap.md` | roadmap=AI초안, selections=합의최종안 | roadmap=헌법, selections=판례 | ✅ 전면 개정 |
| `sdd/in-progress/product-aisuggestion.md` | 명시적 roadmap/selections 언급 낮음, axis/topic 위주 | — | ⚠️ Layer/roadmap/selections 확장분 반영 (#9와 연동) |
| `sdd/done/versions/0.0.1v/product-card.md` | "Layer 1" = LearningFacade 전체 수집, Deck 도메인 상세 | Layer는 신 개념(백엔드→기능의 구현), Deck 폐기 | ✅ "Layer 1" 어휘 개정 + Deck 폐기 반영 ([#13](./issue-13-deck-abolition-axis-absorption.md), [#14](./issue-14-review-strategy-axis-layer-scope.md)) |
| `docs/DOMAIN.md` | LearningFacade 개념만, roadmap/selections 미등장 | 신규 등록 | ✅ 개념 추가 + Deck 개념 제거 |
| `docs/PACKAGE.md` | 현 계층 기준 | Layer/Roadmap/Selection BC 재정의 | ✅ 개정 + Deck BC 제거 |
| `docs/adr/` | 관련 ADR 부재 | — | ✅ 신규 ADR (사용자 정의 표준화 결정 기록 + Deck 폐기 결정 기록) |

## 옵션 비교

**Option A — 사용자 정의 우선 + 기존 문서 개정 (채택)**
- roadmap/selections = 헌법/판례로 고정.
- 문서상 기존 의미(AI 초안 / 사용자 합의 최종안)는 `draft` / `consensus`로 재명명.
- Port/Service 명명도 함께 정리 (예: 기존 문서상 "roadmap draft" 표현 → `AxisDraft`, "selections consensus" → `AxisConsensus`).

**Option B — 기존 문서 용어 유지 + 사용자 정의는 다른 이름으로**
- roadmap/selections는 초안/합의 유지, 사용자 정의는 `axisConstitution`/`axisPrecedents` 등 신 이름 부여.
- 사용자 어휘를 코드·도메인에서 못 쓰게 됨. 사용자 지시 반영 목적과 배치.

**Option C — 문맥별 의미 다중화**
- 문서에서 "이 문맥의 roadmap은 X"로 표기.
- 유지보수 지옥. 반대.

## 선택: Option A

## 부속 결정

### 개정 매핑 (기존 문서 → 신 어휘)

| 기존 문서상 표현 | 개정 후 표현 |
|---|---|
| `roadmap` (AI 축 초안) | `axisDraft` 또는 `axisInteractionDraft` |
| `selections` (사용자 합의 최종안 diff) | `axisConsensus` 또는 `axisAgreement` |
| `RoadmapInteractionSession` | 이름 유지 (세션 자체 명칭). 단, 세션 내부 필드명은 draft/consensus 어휘로 통일 |
| `AxisSuggestionPort` | 이름 유지 (axis 추천 자체) |

새로 도입되는 개념 어휘:
- `AxisRoadmap` (사용자 정의 헌법)
- `AxisSelection` (사용자 정의 판례 1개)
- `RoadmapSuggestionPort` (헌법 초안 생성)
- `SelectionsSuggestionPort` (판례 초안 생성)

### 개정 순서
1. 신규 ADR 작성 (`docs/adr/ADR{N}-roadmap-selections-terminology.md`) — 결정 기록.
2. `docs/DOMAIN.md`에 Layer, AxisRoadmap, AxisSelection 개념 추가.
3. `product-ai-interactive-roadmap.md` 전면 개정 (기존 roadmap/selections 어휘 → draft/consensus).
4. `product-aisuggestion.md`에 LayerSuggestion / RoadmapSuggestion / SelectionsSuggestion 확장분 반영 ([#9](./issue-09-ai-suggestion-3layer.md)과 함께).
5. `docs/PACKAGE.md`에 신 계층·BC 관계 갱신.

## 이관 산출물

- **Docs-Story #8-1**: 신규 ADR 작성 (roadmap/selections 사용자 정의 표준화 결정 기록).
- **Docs-Story #8-2**: `docs/DOMAIN.md` 개정 — Layer / AxisRoadmap / AxisSelection 개념 추가, concepts[] 반영.
- **Docs-Story #8-3**: `product-ai-interactive-roadmap.md` 전면 개정 — roadmap/selections → draft/consensus 치환, 사용자 정의 어휘 도입.
- **Docs-Story #8-4**: `docs/PACKAGE.md` 계층/BC 갱신 (Layer 계층 추가, axis 하부 재구조).
- **Docs-Story #8-5**: `product-aisuggestion.md`에 3층 확장 개정분 반영 ([#9](./issue-09-ai-suggestion-3layer.md)에서 상세).

## 관련 이슈 / 문서

- 관련: [#4](./issue-04-concept-list.md), [#5](./issue-05-layer-server-domain.md), [#6](./issue-06-roadmap-selections-dualaxis.md), [#9](./issue-09-ai-suggestion-3layer.md) — 이 이슈들의 도메인 변경이 SDD 개정 필요분.
- 원본 충돌 문서: `workflow/task/pes/workspectrum/sdd/in-progress/product-ai-interactive-roadmap.md`.
