# [SDD] FE 양식 정의 (풀버전)

> FE 작업 양 스펙트럼 최상위. 1~2개월 단위 Feature 도메인 재설계·라우팅/상태 전체 정합성 개선·인프라+UI 묶음 작업의 표준 양식.
> 본 디렉토리(`fe-workspectrum/sdd/`)의 `Product.md`, `product-*.md`가 실제 사례 — 본 양식은 그 패턴을 정의 문서로 정리한 것이다.

---

## 사용 시점 (트리거)

다음 조건 중 **3개 이상**을 만족할 때 본 양식을 쓴다.

- [ ] 추정 작업 기간 **1~2개월 이상** (Story 15+개, Epic 3+개)
- [ ] **Feature 도메인 전체** 정합성 개선 또는 **다중 Feature 도메인** 협력 재설계
- [ ] 설계 갈림길 **3건 이상** — 각 갈림길마다 Option A/B/C 비교가 필요
- [ ] 컴포넌트 트리·라우팅·상태 흐름 다이어그램이 **필수** (텍스트만으론 설명 안 됨)
- [ ] **실패 모드 매트릭스**(401/403/5xx/MAINTENANCE/오프라인)·UX 회복 정책·Web Vitals 임계를 사전 설계해야 함
- [ ] 마이그레이션 단계가 있음 (Product 의존성 그래프, Epic·Story 의존성, 환경별 설정 분기 — dev/stage/prod, VITE_API_BASE_URL 등)
- [ ] 인프라(CloudFront/CDN/GHA) + Feature 도메인이 한 묶음으로 진행

**졸업 신호 → 완료/아카이브**:
- 모든 Epic 완료 + 핵심 ADR 작성 끝
- 운영 진실 소스(코드/`src/lib/api/schemas/`/`router.tsx`/`backend-boundary/`)에 결과 반영 완료
- 본 파일은 그 시점에 `done/` 디렉토리로 이동하거나, 다음 Product의 의존 문서로 인용된다

---

## 양식 골격

### Product 레벨 (` # [Product] {이름} `)

```
# [Product] {이름}

## Product Vision
> {2~3줄 인용 블록 — 본 Product가 달성하려는 사용자 경험·기술 그림}

## 배경 및 문제
- 현재 상황 (As-Is)
  - {코드/구조의 현 상태 1 — 예: features/auth가 login·signup·oauth-callback에 분산}
  - {현 상태 2}
- 발생하는 문제
  - {문제 1 — 측정 가능하거나 사례 동반}
  - {문제 2}
- 왜 지금 해결해야 하는가
  - {타이밍 사유 — 의존 Product 완료, 백엔드 인계, 외부 트리거 등}

## 목표 (To-Be)
- {목표 1 — 어떤 컴포넌트·hook·라우트가 어떻게 바뀌는가}
- {목표 2}

## 설계 결정 (Design Decisions)
> 큰 갈림길의 결정. 거부된 옵션도 합리적 근거가 있었음을 명시.

- **{결정 1 제목 — 한 줄}**
  - {핵심 근거 1줄}
  - {추가 맥락 1줄}
- **{결정 2 제목 — 한 줄}**
  - ...

## 대안 검토 (Alternatives Considered)
> 갈림길마다 Option A/B/C 비교. 1차 권장 + 거부 사유 명시.

### {갈림길 1 제목 — 예: 상태 관리 방식}
**Option A — {요지 — 예: Context API 유지}**
- 장점: ...
- 거부 이유: ...

**Option B (선택) — {요지}**
- 비용: ...
- 보상: ...

**Option C — {요지}**
- 거부 이유: ...

### {갈림길 2 제목}
...

## 전체 아키텍처 (High-Level Architecture)
> 본문 N페이지보다 다이어그램 1장이 더 강하다.

### 컴포넌트 / 라우트 배치
```
{ASCII 박스 다이어그램 — app/router → features → lib/api/{client,endpoints,schemas} 흐름}
```

### 핵심 플로우
**1. {플로우명 — 예: 로그인 → 토큰 저장 → /home 이동}**
```
{ASCII 시퀀스 — 사용자 입력 → 폼 검증 → mutation → 응답 처리 → 라우팅}
```

**2. {플로우명}**
...

### 외부 의존
- **{외부 시스템 1 — 예: 백엔드 `/api/v1/...`}**: {역할, 어느 endpoint 모듈이 위임 호출}
- **{외부 시스템 2 — 예: FCM Service Worker, CloudFront, MSW}**: ...

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답
| 시나리오 | ApiError code | HTTP | 클라이언트 권장 동작 (UX) |
| --- | --- | --- | --- |
| ... | ... | ... | 인라인 / 토스트 / 라우팅 회복 |

### 로깅 정책 (FE)
- **항상 기록**:
  - {Sentry 또는 console — 예: ApiError 발생 시 code + status + requestId}
- **debug**: {조건 — 예: refresh 흐름 진입}
- **절대 금지**:
  - {토큰 원문 / 사용자 입력 본문 reflection 등}

### 관측 지표 (해당 시 — Web Vitals / RUM 도입 시)
- `{metric_name}` — {타입 — LCP/FID/CLS/INP/TTFB} — {의미}

## 롤아웃 / 마이그레이션 (Rollout)

### 전제
{현재 사용자 / 트래픽 상황. 일괄/단계 중 어떻게 진행하는지}

### Product 의존성
- {선행 Product} → 본 Product
- 본 Product → {후행 Product}

### Epic·Story 의존성 그래프
```
Epic 1 ──► Epic 2 ──► Epic 3
              │
              └─► Epic 4
```

### 환경별 설정 분기
| 항목 | dev (`.env.development`) | prod (`.env.production`) |
| --- | --- | --- |
| `VITE_API_BASE_URL` | http://localhost:8080 | https://api.thirdtool.dev |
| MSW | enabled | disabled |
| Sentry | local | enabled |

## 성공 지표 (KPI)
- {정량 지표 1: "≥/=/0건/100%" — 예: LCP P95 ≤ 1.5s, AUTH002 refresh 성공률 ≥ 99%}
- {정량 지표 2}

## Scope
**In Scope**:
- {포함 1}

**Out of Scope**:
- {제외 1} — 사유
- {제외 2} — 사유

## 대상 사용자
- {역할 1 — 어떤 가치 받는가}
- {역할 2}

## 연결된 Epic 목록
- [ ] Epic 1: {제목}
- [ ] Epic 2: {제목}
- [ ] Epic 3: {제목}

## 관련 문서
- 의존 Product: {링크}
- 관련 ADR: {FE-ADR-NNN, NNN}
- backend-boundary 갱신 예정 섹션: {경로}

## 열린 질문 (Open Questions)
- {미결 항목 1 — 다음 의사결정 트리거}
- {미결 항목 2}
```

### Epic 레벨 (` # [Epic N] {이름} `)

```
# [Epic N] {이름}

## Epic 목표
{한 문장 — Epic이 끝났을 때 어떤 화면·플로우가 가능}

## 배경
{본 Epic이 Product 전체에서 차지하는 위치 + 선행 의존}

## 완료 기준
- [ ] 포함 Story 모두 완료
- [ ] 통합 / E2E 테스트 통과
- [ ] FE-ADR {NNN} 작성

## 내부 메모 (선택)
{진행 중 발견한 사항·재조정 사항. `[명세 변경 이력]` 블록으로 원안과의 편차 기록}

## Epic 기술 결정 / 대안 (해당 시)
{Product 수준 결정과 별개로 Epic 내부에서 갈렸던 선택}
- **{결정 1}**: ...
```

### Story 레벨 (` ## [Story N-M] {이름} `)

```
## [Story N-M] {이름}

### User Story
- As a {역할}
- I want {원하는 행위}
- so that {얻는 가치}

### 설명
{구현 단서 — 컴포넌트 시그니처·hook·Zod 스키마 필드·endpoint 함수·ApiError 처리·라우트 경로}

### 완료 기준 (AC)
- Given ... / When ... / Then ...
- *(엣지 - 사유)* Given ... / When ... / Then ...

### Definition of Done
- [ ] 구현 (구체 컴포넌트·hook·파일명)
- [ ] 단위 테스트 ({N건}, Vitest + Testing Library)
- [ ] MSW handler (해당 시)
- [ ] 통합 / E2E 테스트 (해당 시)
- [ ] ADR 작성 (해당 시)

### 스토리 포인트
{0.5d / 1d / 2d / 3d — Estimable 미달이면 분할}

### 의존성
- 선행: Story N-K (이유)
- 후행: Story N-L (이유)

### [명세 변경 이력] (해당 시)
- YYYY-MM-DD: {원안 → 실제 구현} 차이 + 변경 사유
```

### 섹션 가이드

| 원칙 | 적용 |
| --- | --- |
| **한 파일 = 한 Product** | Product/Epic/Story 수직 통합. 별도 spec/plan/tasks 파일로 쪼개지 않음 |
| **거부된 옵션도 합리적 근거 명시** | "왜 이것이 아니고 저것인가"를 남겨 트레이드오프를 드러냄 |
| **결정 1줄 + 보상 1줄 + 비용 1줄** | Option B(선택)는 항상 비용과 보상을 명시 |
| **다이어그램은 ASCII로** | 외부 도구 의존 X. 텍스트 검색 가능. PR 리뷰 시 diff에 잘 잡힘 |
| **실패 시나리오 표를 사전 설계** | 구현이 끝난 뒤 끼워 넣지 않음. ApiError code 매핑·UX 회복 행위도 본 표를 근거로 |
| **명세 변경 이력 블록** | Story 진행 중 발견한 원안 편차는 코드만 바꾸고 끝내지 않음 — 본 블록에 기록 |
| **추정 식별자 금지** | 실제 컴포넌트명·hook 시그니처·endpoint 경로·ErrorCode 접두사는 코드에서 확인해서 쓴다 |
| **FE-ADR 트리거** | 새 라이브러리 채택·새 상태 패턴·새 라우팅 결정은 별도 FE-ADR 발행 |

---

## 예시

본 디렉토리에 풀버전 실제 사례가 있다. 본 양식의 모든 섹션이 어떻게 채워지는지 다음 파일을 참조:

| 파일 | 특징 |
| --- | --- |
| `./done/product-card.md` | Card 표시·스터디 UI. 상태(ON_FIELD/ARCHIVE) UX 동기화 |
| `./done/product-learningFacade.md` | 축/주제/자료 트리, 커버리지 색상, displayOrder reorder UX |
| `./in-progress/Product.md` | User 프로필·세션 상태·SessionWatcher |
| `./in-progress/product-auth.md` | AT 쿠키 / RT 메모리 / 인터셉터 refresh 1회 재시도 |
| `./in-progress/product-fe-cdn.md` | S3+CloudFront+Route53+GHA, VITE_API_BASE_URL |
| `./in-progress/product-media.md` | presigned URL 업로드 + 진행률 + 미리보기 |
| `./in-progress/product-search.md` | 검색 입력 디바운스 + 결과 카드 + 하이라이트 |
| `./in-progress/product-learning-tower.md` | 학습 모델 재편 FE — concepts[] Chip Input, Layer/Roadmap/Selection UI, Card→Axis, Deck 라우트 리디렉트, Review scope 토글. Epic 6개 · 대형 사례 (0.0.2v fix issues #04·#05·#06·#07·#11·#13·#14 반영) |
| `./in-progress/product-ai-suggestion.md` | 무상태 4-Port SuggestButton · SuggestionResultDialog · RoleBadge · FallbackBanner · RateLimitToast (0.0.2v fix issues #09·#10 반영) |
| `./in-progress/product-ai-interactive-roadmap.md` | RoadmapInteractionSession 상태기계 UI(INIT/DRAFTED/REVIEWING/COMMITTED) · 낙관적 UI · DraftChangeLog · Selection 정책 강제 (0.0.2v fix issues #06·#09·#11 반영) |
| `../FE-ADR-CANDIDATES.md` | FE-ADR 후보 우선순위 추적 (메타) |

새 풀 SDD를 작성할 때는 위 사례 중 가장 가까운 패턴을 골라 섹션 구조를 그대로 복사한 뒤 내용만 채우는 것을 권장.

---

## 참조

- 이전 스펙트럼: `../sdd-lite/version/0.0.1v/sdd-lite.md`
- 전체 스펙트럼: `../` (one-line-spec / feature-story / pes / sdd-lite)
- 백엔드 원본: `../../../workspectrum/sdd/sdd.md`
- 양식 진화: 본 양식은 SemVer로 진화. 변경 시 새 버전을 두고 본 버전은 보존
