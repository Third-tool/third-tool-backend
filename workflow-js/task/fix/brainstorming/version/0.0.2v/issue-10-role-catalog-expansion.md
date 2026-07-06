# Issue: Role 확장 — 개발자 외 기획자·디자이너 프롬프트/카탈로그

## 배경
사용자 지시:
> "단연 개발자의 관념만이 아님, 기획자, 디자이너의 학습 요소 모두 포함입니다."

현재 SDD 예시(`product-aisuggestion.md`, `product-ai-interactive-roadmap.md`)와 Static Fallback 데이터는 개발자 중심 (Spring / 마이크로서비스 / 클라우드). concepts에 "기획자", "디자이너"가 들어와도 AI 추천이 어색하거나, Static Fallback이 아예 없어 폴백 실패.

## 조사 결과 — 현재 예시 편중

| Port / 문서 | 예시 편중 |
|---|---|
| `AxisSuggestionPort` Static | Spring, 마이크로서비스, 클라우드, JPA (개발자만) |
| 프롬프트 템플릿 | "학습 축", "코드 아키텍처" 등 개발자 은유 다수 |
| `product-ai-interactive-roadmap.md` 예시 | 개발자 중심 |
| Layer 개념 사용자 예시 | 백엔드 개발자·기획자 조합만 언급, 실제 데이터 없음 |

## 옵션 비교

**Option A — Role-agnostic 프롬프트 + Role별 Static 카탈로그 다각화 (채택)**
- 프롬프트 템플릿에서 "코드 / 아키텍처 / framework" 같은 개발자 은유 제거, role-agnostic 어휘("학습 축", "실행 spine")로 재작성.
- Static 카탈로그를 role별 데이터 셋으로 분리·확장 (개발자 / 기획자 / 디자이너 / 문제해결 / 마케팅 등).
- 사용자 concepts 입력에서 role 힌트 감지 → 관련 카탈로그 우선.

**Option B — 개발자 카탈로그만 유지 + 기획/디자인은 LLM만**
- Static Fallback 실패 시 LLM 실패도 겹치면 UX 붕괴. YAGNI 반대 사례 — 커버해야 할 사용자군이 이미 확실함.

**Option C — 사용자가 role을 명시 입력**
- 사용자 부담. concepts 자체가 이미 role 힌트 (예: "백엔드 개발자"). 명시 입력은 중복.

## 선택: Option A

## 부속 결정

### Role별 카탈로그 최소 shape
```
role: "backend-developer" | "planner" | "designer" | "problem-solver" | ...
layerCatalog: [
  { name: "기능의 구현", reason: "...", axisCatalog: [
    { name: "Spring framework", roadmapSpineExample: "IoC & DI / Bean 등록 / ..." }
  ] }
]
```

### 예시 데이터 세트 (초안)

**backend-developer 카탈로그**
- layers: 기능의 구현 / 데이터베이스 / 인프라 / 코드 품질
- axes: Spring framework, JPA, DDD, 테스트 전략, DB 설계, 배포 파이프라인 등

**planner 카탈로그**
- layers: 기획, 문제 해결, 리서치, 커뮤니케이션
- axes: 사용자 문제 정의, 지표 설계, 요구사항 명세, 이해관계자 매핑, 우선순위 결정

**designer 카탈로그**
- layers: UX 기획, 시각 언어, 인터랙션, 리서치
- axes: 정보 구조, 컴포넌트 시스템, 프로토타이핑, 사용성 테스트, 시각 계층

**problem-solver 카탈로그 (공통)**
- layers: 문제 정의, 분석, 실행 설계, 회고
- axes: 5-why, MECE, 프레임워크 선택, 검증 지표

### Role 감지 규칙 (초안)
- concept 문자열 매칭 (사전) → role 태그 자동 부여.
- 다중 role (예: 백엔드 개발자 + 기획자)이면 layer 카탈로그 병합, axis 카탈로그는 role별 그룹핑 유지.
- 사전에 없는 concept은 role 감지 실패 → LLM 우선, Static은 공통(problem-solver) 카탈로그로 폴백.

## 이관 산출물

- **BE-Story #10-1**: `RoleCatalog` 데이터 구조 정의 (`aisuggestion.infrastructure.catalog.RoleCatalog`) + 4개 초기 role JSON/YAML 카탈로그 리소스 파일.
- **BE-Story #10-2**: Static Adapter 재작성 — Role 감지 → Role 카탈로그 조회 → 폴백 순서. 폴백은 공통(problem-solver) 카탈로그.
- **BE-Story #10-3**: 프롬프트 템플릿 role-agnostic화 (`aisuggestion.infrastructure.prompt.*.st` 파일 — 실제 위치 확인 후). 개발자 은유 제거.
- **BE-Story #10-4**: LayerSuggestionPort / AxisSuggestionPort / RoadmapSuggestionPort 각각에 role 힌트 파라미터 추가 및 프롬프트에 반영.
- **Docs-Story #10-5**: `product-aisuggestion.md`에 role 확장 정책 반영 ([#8-5](./issue-08-terminology-redefinition-sdd.md)과 함께).

## 관련 이슈 / 문서

- 관련: [#4 concepts[]](./issue-04-concept-list.md) — concepts로부터 role 감지.
- 관련: [#9 AI 3층 확장](./issue-09-ai-suggestion-3layer.md) — 4개 Port 모두 role 힌트 반영.
