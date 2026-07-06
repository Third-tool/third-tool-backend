# 0.0.3v / Eval — AI 응답 품질 평가 프레임 초석 (신설)

> **본 파일 역할**: M3 하이라이트 "AI 첫 응답 도착"으로 Static Adapter가 실제 사용자 초안을 발행하기 시작. 이 응답의 품질을 판정하는 프레임을 **본 버전부터 착수**한다. v1은 관찰 지표만 · v2는 golden dataset 방향.
> 이 파일은 **greenfield** (기존 규범 없음). 사용자 명시 요청 ("AI로 결과 나오기 시작하면 eval도 만들기 시작해야 하는데") 반영.

---

## 왜 지금 시작하는가

M3 하이라이트로 다음 응답이 실제 발행 시작:

```
POST /api/v1/suggestions/chapters-outline
{ concepts:["백엔드","기획자"], layerName:"기능의 구현", axisName:"하네스 엔지니어링" }
→ 200
{
  chapters: [
    { title:"1. 하네스 엔지니어링 기초", rationale:"AI 에이전트를 실전으로 만드는 기본 프레임" },
    { title:"2. 메모리와 컨텍스트 관리", rationale:"..." },
    ... (5개)
  ],
  providerContext: "static:backend-developer",
  suggestionsAvailable: true
}
```

이는 **사용자에게 노출되는 실제 학습 초안**. 다음 두 가지가 눈에 띈다:

1. **품질 판정 프레임 없이는 회귀 감지 불가** — catalog(`backend-developer.json`)가 확장되면 응답 콘텐츠가 변한다. 어느 방향이 "좋은 방향인지" 판별할 규범이 없으면 유지·확장 결정이 임의화된다.
2. **LLM Adapter (M6~) 도입 전 baseline 확립 필요** — Static Adapter의 응답 품질을 baseline으로 확정해야 LLM 도입 후의 실제 향상 여부를 판정 가능.

**첫 신호**: M3 PR#5 Sceptical Reviewer가 catalog 오타 "능 아키텍처" → "기능 아키텍처"를 감지 (`review.md` T#004). 이는 **비공식 eval의 첫 사례**. 정식 프레임을 세워 이런 감지를 재현 가능하게 한다.

---

## v1 관찰 지표 (본 버전 착수 · 실측 가능)

### v1-지표 1: `providerContext` 응답 필드

- **위치**: 4개 신규 Suggestion Port 응답 모두 (`ChaptersOutlineResponse` · `ChapterSubtreeResponse` · `SelectionOutlineResponse`).
- **값 형식**: `"static:{role}"` (예: `"static:backend-developer"`, `"static:generic"`). 향후 LLM 도입 시 `"llm:vertex-gemini-2.5-flash"` 등.
- **판정 활용**:
  - 응답 소스가 Static인지 LLM인지 사용자·개발자 모두 로그로 추적 가능
  - Static fallback 발동 비율 (LLM 실패 → Static) 향후 계측 근거
  - 각 role catalog 매칭 정확도 실측 (`static:backend-developer` vs `static:generic` 비율)

### v1-지표 2: `suggestionsAvailable` 응답 플래그 (ADR010 fallback)

- **위치**: 모든 Suggestion 응답.
- **판정 활용**:
  - `false` 응답의 사용자 UX 노출 방식 판정 근거 (빈 상태 안내 · 재시도 유도)
  - 향후 성공률 지표 (`Sum(available=true) / Sum(all)`) 산정

### v1-지표 3: Sceptical Reviewer 세션 판정 (M3 PR#5에서 이미 실행)

- **위치**: `.claude/rules/review.md` §Sceptical Reviewer.
- **M3 실측 사례**:
  - catalog↔milestone 예시 불일치 감지 → catalog 확장 조치 (`review.md` R5)
  - "능 아키텍처" 오타 → "기능 아키텍처" 수정
  - subtree 콘텐츠에 "Stripe/Github" 등 도구 이름 등장 → selectionOutlines에 배치돼 정합 (roadmap 부분엔 없음)
- **판정 활용**:
  - PR 단위 catalog 콘텐츠 품질 검증의 표준
  - 새 catalog role (planner/designer/problem-solver) 신설 시 필수 관문

### v1-지표 4: concept-spec.txt 카탈로그 6종 · 5종 판별 기준

- **위치**: `src/main/resources/prompts/concept-spec.txt`.
- **Roadmap 콘텐츠 카탈로그 6종**:
  1. 변하지 않는 핵심 개념 (정규화/비정규화·문제 정의·우선순위 기준)
  2. 반복되는 질문의 축 ("언제 분리해야 하는가", "어떤 안티패턴이 반복되는가")
  3. 선택 기준 (추상화 비용·팀 숙련도·변경 빈도)
  4. 트레이드오프 골격 (속도 vs 안정성, 재사용성 vs 맥락 적합성)
  5. 안티패턴/함정 카탈로그
  6. 계층 구조/의존 순서
- **Selections 콘텐츠 카탈로그 5종**:
  1. 분기 가능한 관점들
  2. 적용 시나리오별 변형
  3. 비교표/옵션 라이브러리
  4. 실험 패턴
  5. 파생 규칙/운영 방법
- **수렴 point 판별 5개** (roadmap 후보 신호)
- **발산 point 판별 5개** (selections 후보 신호)
- **판정 활용**:
  - catalog 확장 시 "각 챕터가 어느 카탈로그 유형인가?" 명시적 태깅
  - roadmap 필드에 selections 성격 콘텐츠 (예: 특정 도구 비교) 침투 여부 감지

### v1-지표 5: `PromptTemplatesPresenceTest` (배치 무결성)

- **위치**: `src/test/java/.../infrastructure/suggestion/PromptTemplatesPresenceTest.java`.
- **검증 대상**:
  - 7개 파일 classpath 존재 (`concept-spec.txt` + 6개 Port 프롬프트)
  - 각 Port 프롬프트에 `{{include:concept-spec.txt}}` 마커 존재
  - roadmap Port 프롬프트에 "도구 이름·특정 옵션 비교 금지" 문구 grep 통과
  - selections Port 프롬프트에 "기준은 roadmap에 있다고 가정" 문구 grep 통과
- **판정 활용**:
  - v2 LLM Adapter 도입 시 프롬프트 include resolver 배선 검증 근거
  - concept-spec.txt 갱신 시 6개 Port 프롬프트 재정합 자동 검사

---

## 현재 M3 baseline 실측 (backend-developer.json 기준)

M3 종료 시점 catalog가 concept-spec 6+5 카탈로그·수렴/발산 판별에 얼마나 정합하는지 실측:

### chapters (10개 · axis별)

| Axis | 챕터 수 | roadmap 카탈로그 정합 판정 (v1 관찰) |
| --- | --- | --- |
| REST 원칙 | 4 (`1. REST 아키텍처 개요` · `2. 리소스 모델링` · `3. 메서드와 상태 코드 표준` · `4. 버전 관리와 진화`) | ✅ 4/6 카탈로그 대응 (핵심 개념 · 질문 축 · 트레이드오프 · 계층 구조) · 도구 이름 미등장 · **정합** |
| Aggregate 설계 | 1 (`1. Aggregate 경계 판정`) | ✅ 트레이드오프 골격 + 선택 기준 · **정합** (챕터 개수 부족은 다음 확장에서 보강) |
| 하네스 엔지니어링 | 5 (`1. 기초` · `2. 메모리와 컨텍스트` · `3. 툴 통합 패턴` · `4. 판단·계획·실행 루프` · `5. 안전·신뢰 경계`) | ✅ 6/6 카탈로그 대응 (핵심 개념 · 안티패턴/함정 · 계층 구조 · 트레이드오프) · **정합** |

**전반 판정**: 3개 axis 총 10 챕터가 roadmap 카탈로그 6종에 대응하는 콘텐츠. 도구 이름·특정 옵션 비교 없음. **개념 명세 정합 ✅**.

### selectionOutlines (3개)

| nameCandidate | 챕터 수 | selections 카탈로그 정합 판정 |
| --- | --- | --- |
| 기능 아키텍처 REST selections v1 | 2 (`1. 큰 조직의 API-First 사례 (Stripe/Github)` · `2. gRPC로 넘어간 케이스`) | ✅ 비교표/옵션 라이브러리 + 적용 시나리오별 변형 · 도구 이름 등장하나 selections이라 정합 |
| 실전 도메인 Aggregate selections v1 | 2 (`1. 결제 도메인 사례` · `2. 주문 · 배송 사례`) | ✅ 분기 가능한 관점들 + 적용 시나리오별 변형 · **정합** |
| 실전 하네스 사례 selections v1 | 2 (`1. AutoGPT 초기 사례` · `2. Claude Code 사례`) | ✅ 분기 가능한 관점들 (성공 vs 실패) + 안티패턴 → 파생 규칙 · **정합** |

**전반 판정**: 3개 selectionOutline 모두 selections 카탈로그 5종에 대응. 사례·비교·시나리오 중심 · 원리 재정의 없음. **개념 명세 정합 ✅**.

### providerContext 응답 정합

M3 실측 (SuggestionAppServiceTest):
- concepts=["백엔드", "기획자"] → `providerContext:"static:backend-developer"` (RoleDetector로 백엔드 매칭)
- concepts=["아무개 특수 분야"] → `providerContext:"static:generic"` (매칭 실패 → generic 폴백)

**판정**: role 감지 로직 정상 · fallback 안전.

---

## eval 실행 트리거 (본주 실측 사례 + M3 종료 신호)

### 트리거 1: 새 catalog role 신설 시

- **대상**: 향후 M4~M5 `planner.json` · `designer.json` · `problem-solver.json` 신설.
- **필수 검사**:
  - concept-spec 6+5 카탈로그별 태깅 (각 챕터가 어느 유형인지)
  - roadmap 챕터에 도구 이름·특정 옵션 비교 침투 없음
  - selections는 비교/사례/시나리오 중심 · 원리 재정의 없음
  - Sceptical Reviewer 세션 필수 통과

### 트리거 2: catalog 확장/갱신 시 (M3 실측 근거)

- **대상**: 기존 role catalog에 axes/chapters/selectionOutlines 추가·수정.
- **감지 사례**: PR#5에서 milestone.md 예시 (`axisName:"하네스 엔지니어링"`)가 catalog에 부재 → Sceptical Reviewer 감지 → 즉시 catalog 확장.
- **재발 방지 프로토콜**: milestone.md 종료 신호 예시는 catalog 커밋과 동시 갱신 (`review.md` R5).

### 트리거 3: LLM Adapter 도입 (M6~)

- **대상**: `LlmChaptersOutlineAdapter` 등 LLM 구현 첫 배선.
- **필수 검사**:
  - Static baseline과 LLM 응답 비교 (같은 concepts+axis 입력)
  - providerContext 필드로 소스 명확화
  - Static fallback 발동 비율 계측

### 트리거 4: 릴리스 단위 (v1 · 0.1.0v · 2026-08-19)

- **대상**: 첫 사용자 릴리스 · v1 스코프 AI 응답 총 검증.
- **필수 검사**:
  - 초대된 사용자 3명이 실제로 chapters-outline · chapter-subtree · selection-outline · selection-subtree 응답을 사용해 학습 대상을 조립할 수 있는가
  - 응답이 concept-spec 정합인가 (roadmap에 도구 침투 없음 · selections에 원리 재정의 없음)
  - `suggestionsAvailable:false` 응답 시 UX 노출이 적절한가

---

## v2 golden dataset 방향 (M6~ 진입 시 배선)

`workflow/task/pes/brainstorming/0.0.1v/ai.md` §후보 3 "출력 품질 평가 파이프라인" 참조. 두 접근이 검토됨:

### A안 (권장 · v1.5~v2 진입 시): Gold dataset + LLM-as-judge

- **구성**:
  - concept 100건 × 예상 axis pair (예: `["백엔드"] → "REST 원칙"` · `["기획자"] → "요구사항 정의"`)
  - 각 chapters-outline · chapter-subtree 응답의 concept-spec 카탈로그 태깅 (roadmap 6종 / selections 5종)
- **자동 채점**:
  - LLM-as-judge (별도 모델이 판정)
  - 판정 지표: (1) concept-spec 정합 여부 (2) 도구 이름 침투 여부 (roadmap 검사) (3) 원리 재정의 여부 (selections 검사) (4) subtree 3~5 서브항목 개수
- **회귀 감지**: catalog 커밋 · LLM Adapter 프롬프트 변경 시 gold dataset 배치 실행 → 이전 대비 판정 점수 하락 감지

### B안 (v2+): Implicit signals + user feedback

- **구성**:
  - 채널 선택율 (chapters-outline 응답 채택률)
  - 사용자 재생성 요청 횟수 (같은 axis에 subtree 재요청)
  - 명시적 👍/👎 피드백 (product-ai-suggestion.md Epic 6 관측성과 배선)
- **적용 시점**: v2 (다수 사용자 확보 후)

---

## 다음 버전 (M4~) 진입 조건 · eval 프레임 확장

- [ ] **eval 프레임을 별도 Product spec으로 승격** → `product-ai-evaluation.md` 신설 (v2 이관)
- [ ] **Static Adapter role 다각화 시점의 catalog 품질 판정 절차 명세** → M4 planner/designer/problem-solver JSON 신설 시 필수
- [ ] **ADR 후보: "AI 출력 품질 평가 메트릭 정의"** — 어떤 지표를 릴리스 관문으로 삼을 것인가
- [ ] **Sceptical Reviewer의 AI 응답 리뷰 세션 별도 프로토콜** — `.claude/rules/review.md` §Sceptical가 코드 리뷰용. AI 응답 리뷰용으로 확장 (concept-spec 정합 · catalog 커버리지 · 도구 침투 등)
- [ ] **Gold dataset 초안 착수** — concept 20건 × 예상 axis pair (v1.5 진입 시)

---

## 참고

- `workflow/task/pes/brainstorming/0.0.1v/ai.md` §후보 3 (품질 평가 파이프라인) — A안·B안 검토
- `.claude/rules/review.md` §Sceptical Reviewer — 코드 리뷰용 5관점 (AI 응답 리뷰용 확장 대비)
- `product-ai-suggestion.md` Epic 6 (관측성) — M7 이관, `thirdtool.suggestion.*` 지표 배선 계획
- `product-ai-suggestion.md` §관찰 지표 로깅 v1 (이슈 #20) — 세션당 호출·토큰·재생성·fallback 비율
- `src/main/resources/prompts/concept-spec.txt` — 카탈로그 6종·5종 판별 5+5 (재사용 자산)
- `src/main/resources/ai/catalog/backend-developer.json` — M3 baseline catalog (chapters 10 + selectionOutlines 3)
- `../0.0.2v/review.md` — M2 rush 정책과 Reviewer 스킵의 트레이드오프 (M3 재개의 배경)

---

*작성일: 2026-07-03 | 신설 사유: M3 AI 첫 응답 도착 · 사용자 명시 요청 | v1은 관찰 지표만 · v2 방향 언급 · 실행 SOP는 별도 브랜치 이관 | greenfield 영역 (기존 프로젝트 규범 없음)*
