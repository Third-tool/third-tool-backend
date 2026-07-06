# Issue: Roadmap = 수렴/기준 저장소 · Selections = 발산/가능성 저장소 — 개념 명세 + AI 프롬프트 embed

## 배경
사용자 지시 (2026-07-02 fix 회의):
> "여기서 '헌법'은 법률 용어가 아니라, '자주 안 바뀌고, 바뀌더라도 천천히 바뀌며, 하위 의사결정의 기준이 되는 지식'이라는 비유로 이해하면 됩니다. 즉 roadmap은 '무엇을 계속 기준으로 삼을 것인가'를 정리하고, selections는 '그 기준을 들고 실제로 어떤 방향들로 뻗어나가며 비교할 것인가'를 다룹니다."
> "이 concept도 issue, product 설계에 확실하게 ai로 만들 때 박고 싶습니다."

이슈 #6에서 roadmap = 헌법, selections = 판례로 개념 정의만 넣었지만, 실제 AI 프롬프트에는 few-shot example 정도만 있었음. **결과적으로 AI가 roadmap에 발산 지식(특정 도구 이름·옵션 비교)을 섞거나, selections에 수렴 원리를 재복사하는 오염**이 예상됨.

**목표**: roadmap/selections의 성격 차이를 AI가 항상 준수하도록 **명시적 콘텐츠 카탈로그 + 판별 기준**을 AI 프롬프트 시스템 프롬프트에 embed. 도메인 spec에도 이식용 정의문으로 포함해 팀·리뷰어 판단 근거화.

## 조사 결과 — 사용자 명세 5장 원문 구조

| 장 | 핵심 |
|---|---|
| 1. roadmap에 들어가는 정보 | 콘텐츠 카탈로그 6종 (변하지 않는 핵심 개념 / 반복되는 질문의 축 / 선택 기준 / 트레이드오프 골격 / 안티패턴 / 계층 구조·의존 순서) |
| 2. selections에 들어가는 정보 | 콘텐츠 카탈로그 5종 (분기 가능한 관점들 / 적용 시나리오별 변형 / 비교표·옵션 라이브러리 / 실험 패턴 / 파생 규칙·운영 방법) |
| 3. 수렴 point vs 발산 point 구분 | 판별 기준 각 5개 |
| 4. 구체 예시 | 문서 설계 체계 / 학습 로드맵 체계 / 기술 선택 체계 3가지 사례 (각 roadmap형·selections형 대비) |
| 5. 이식용 실무 설명 | 정의문: roadmap = 기준의 저장소 / selections = 가능성의 저장소 |

## 옵션 비교

**Option A — 프롬프트엔 압축 카탈로그·판별 기준만, 도메인 spec엔 정의문·예시 포함 (채택)**
- AI 프롬프트: 카탈로그(6종·5종) + 판별 기준(5개·5개) 각 항목 1줄로 압축.
- 도메인 spec: 1~5장 전체를 도메인 의도 섹션에 요약 embed (리뷰어·후속 팀원용).
- 프롬프트 토큰 부담 최소 + 정의문·이식용 설명은 문서로 유지.

**Option B — 5장 원문 통짜 프롬프트 embed**
- 매 호출마다 큰 토큰 부담. cached prompt로 완화되어도 output에는 영향 X.
- 이식용 정의문은 AI에겐 노이즈.

**Option C — 프롬프트 embed 없이 few-shot example만**
- AI가 개념 오염(도구 이름 → roadmap 잘못 들어감 등) 낼 가능성 높음.

## 선택: Option A

## 부속 결정

### AI 프롬프트 embed 대상 (static asset — `resources/prompts/concept-spec.txt`)

**Roadmap 콘텐츠 카탈로그 (6종, 각 1줄)**
1. 변하지 않는 핵심 개념 — 정규화/비정규화, 문제 정의, 우선순위 기준 등 프로젝트가 달라도 유효한 개념
2. 반복되는 질문의 축 — "언제 분리해야 하는가", "어떤 안티패턴이 반복되는가" 같은 사고 질문 구조
3. 선택 기준 — 옵션 자체가 아닌 옵션 평가 기준 (추상화 비용, 팀 숙련도, 변경 빈도 등)
4. 트레이드오프 골격 — 속도 vs 안정성, 재사용성 vs 맥락 적합성 같은 오래가는 축
5. 안티패턴/함정 카탈로그 — 시간이 지나도 반복 재발하는 실수 패턴
6. 계층 구조/의존 순서 — 원리 → 패턴 → 사례 → 안티패턴 같은 backbone

**Selections 콘텐츠 카탈로그 (5종, 각 1줄)**
1. 분기 가능한 관점들 — 같은 문제의 여러 갈래 (예: screen-sequence-first / domain-first / package-first)
2. 적용 시나리오별 변형 — 작은 팀 vs 큰 팀, AI 독자 vs 사람 독자 등 조건별
3. 비교표/옵션 라이브러리 — A안/B안/C안 + 언제 채택할지
4. 실험 패턴 — staging 도입, 3회 반복 후 승격 등 실험·확장 규칙
5. 파생 규칙/운영 방법 — roadmap 승격 정책, 세션 관리 규칙 등 기준을 굴리는 방법

**수렴 point 신호 (→ roadmap 후보, 5개)**
- 시간이 지나도 자주 안 바뀌는가?
- 여러 사례를 관통하는 상위 기준인가?
- 개별 사례보다 판단축에 가까운가?
- 팀이 바뀌어도 재사용 가능한가?
- 학습/설계의 backbone 역할을 하는가?

**발산 point 신호 (→ selections 후보, 5개)**
- 상황에 따라 답이 달라지는가?
- 여러 대안이 공존 가능한가?
- 실험과 비교가 중요한가?
- 특정 조직/도메인/프로젝트 맥락에 민감한가?
- 아직 일반화하기 이른가?

### 각 Port별 프롬프트 조합

`ChaptersOutlinePort`, `ChapterSubtreePort`:
- **시스템 프롬프트 상단**: `concept-spec.txt` (위 카탈로그·판별기준) + Port별 지시 ("너는 수렴된 판단 프레임을 뽑는다. 도구 이름·특정 옵션 비교 금지. 발산 지식은 selections로 이관")
- **few-shot example**: 사용자 예시 (하네스 로드맵 챕터 subtree)
- **user 프롬프트**: concepts, layerName, axisName, axisReason, freeformHint 등

`SelectionOutlinePort`, `SelectionSubtreePort`:
- **시스템 프롬프트 상단**: `concept-spec.txt` + Port별 지시 ("너는 발산된 적용안을 뽑는다. 기준 자체는 roadmap에 있다고 가정하고 그 기준을 굴리는 다양한 관점을 낸다")
- **few-shot example**: Selection용 예시 (예: 사용자 예시 "Spring Framework 심화 selections" 형태)
- **user 프롬프트**: concepts, layerName, axisName, roadmapContent(요약), variantHint

### 도메인 spec (product-learning-tower.md) embed

`AxisRoadmap` / `AxisSelection` 도메인 의도 섹션에 다음 삽입:
- roadmap = 기준의 저장소 / selections = 가능성의 저장소 (5장 이식용 정의문)
- 콘텐츠 카탈로그 6종·5종 요약 (프롬프트와 동일 압축본)
- 3가지 구체 예시 (문서 설계 체계 / 학습 로드맵 체계 / 기술 선택 체계) 축약 인용 — 리뷰어 판단 근거

목적: 코드 리뷰어·후속 팀원이 "이 필드·이 API는 roadmap 성격인가 selections 성격인가"를 판단할 때 이 섹션을 참조하도록. 다른 팀 이식 시에도 이 섹션이 유일한 개념 원천.

### 프롬프트 관리

- `concept-spec.txt`는 `src/main/resources/prompts/` 아래 단일 파일. 4개 Port 프롬프트 리소스가 이를 include (템플릿 조립 시).
- 변경 시 4개 Port 프롬프트 동시 영향 — 변경 리뷰 필수.
- 초기 3명 사용자 관찰 기간 동안 프롬프트 튜닝 반복 예상. 매 튜닝마다 이 파일 하나만 수정 → 4개 Port에 자동 반영.

## 이관 산출물

- **BE-Story #19-1**: `resources/prompts/concept-spec.txt` static asset 배치 (위 카탈로그·판별기준).
- **BE-Story #19-2**: 4개 Port 프롬프트 템플릿(`chapters-outline.txt`, `chapter-subtree.txt`, `selection-outline.txt`, `selection-subtree.txt`)에서 `concept-spec.txt` include + Port별 지시 + few-shot example 조립.
- **BE-Story #19-3**: LLM Adapter 프롬프트 조립 로직 (Spring AI PromptTemplate 계열).
- **BE-Story #19-4**: Static Adapter의 role-keyed catalog도 이 개념을 지키도록 하드코딩 예시 재점검 (특히 이슈 #10에서 확장한 role 예시들).
- **SDD 개정** (이슈 #8):
  - `product-learning-tower.md` 도메인 의도 섹션에 정의문·카탈로그·구체 예시 embed.
  - `product-ai-suggestion.md` 프롬프트 조립 규칙 명시.

## 관련 이슈 / 문서

- 뒤집는 이슈 없음 (이슈 #6의 개념 정의를 명세로 확장).
- 연동: [#15 Roadmap 노드 모델](./issue-15-roadmap-node-model.md), [#16 Selection 노드 모델](./issue-16-selection-node-model.md), [#17 AI 2단계 생성](./issue-17-ai-two-step-generation.md), [#10 Role 확장](./issue-10-role-catalog-expansion.md).
- SDD 개정: `product-learning-tower.md`, `product-ai-suggestion.md`.
