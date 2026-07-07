# [Workspectrum] 0.0.1v — 작업 양 스펙트럼 기반 양식 체계 도입

> **버전 범위**: 0.0.1v 최초 도입
> **적용 시점**: 2026-06-28
> **다음 버전 신호**: 양식 트리거 기준 변경, 새 양식 추가, 상태 단계 재정의 시 `0.0.2v` 발행

---

## 1. 도입 배경 — 해결한 문제

### 이전 상황 (As-Is)

- 모든 작업을 동일한 PES(Product/Epic/Story) 양식 하나로만 다뤘다
- 1시간짜리 오타 수정부터 2개월짜리 아키텍처 재설계까지 같은 양식 → 과명세(overkill) 또는 무명세
- 어디까지 기획하고 어디서 코드를 시작하는지 경계가 불분명
- 여러 Product가 동시 진행될 때 어느 것이 현재 진행 중이고 어느 것이 대기 중인지 디렉토리만 봐서는 알 수 없었다

### 해결한 문제 (To-Be)

- 작업 크기(시간·영향 범위·설계 복잡도)에 따라 **5개 양식**으로 분리 → 적정 명세 수준 자동 결정
- 각 양식마다 명확한 트리거 조건을 정의 → "이걸 쓸까 저걸 쓸까" 판단 비용 제거
- 양식 간 졸업 신호를 명시 → 작업이 커질 때 상향 기준이 생김
- 상태 폴더 체제(backlog/ready/in-progress/done)로 디렉토리만 봐도 현재 흐름 파악 가능

---

## 2. 도입된 양식 5종 — 작업 양 스펙트럼

작업 시간과 설계 복잡도를 기준으로 선형 스펙트럼을 구성한다.

```
one-line-spec ──► feature-story ──► pes ──► sdd(풀)
                       │                       ▲
                       └──► sdd-lite ──────────┘
                            (인계·핸드오프 필요 시)
```

### 2.1 One-line-spec (최소 단위)

**트리거**: 다음 **모두** 만족
- 추정 작업 시간 1시간 이내
- 영향 파일 1개
- 외부 시스템·인프라 변경 0건
- 도메인 규칙·불변식 변경 없음 (단순 표기·메시지·복사값 갱신)

**해결한 문제**: 소규모 수정에 PES 전체 양식을 쓰는 과명세 제거. 1~3개 완료 신호만으로 검증 완결.

**핵심 구조**:
```
# [Spec] {한 줄 요구사항 — 동사로 시작}
## 타깃         — 파일/함수 경로
## 완료 신호    — 체크박스 1~3개
## 비범위
## 관련 (선택)
```

**정의 문서**: `one-line-spec/version/0.0.1v/one-line-spec.md`

---

### 2.2 Feature-story (단일 PR 기능)

**트리거**: 다음 **모두** 만족
- 추정 작업 시간 1~3일
- 단일 PR 완결
- 영향 BC 1개
- 단위 테스트 매트릭스 필요
- 설계 갈림길 0~1건 이하

**해결한 문제**: 1~3일짜리 기능 개발을 사용자 가치 중심으로 명세화. 해피/엣지/예외 AC 매트릭스와 DoD를 강제해 PR 완결성 검증.

**핵심 구조**:
```
# [Story] {이름 — 동사로 시작}
## 사용자 가치   — As a / I want / so that
## 설명          — 메서드 시그니처·불변식·ErrorCode
## 인수 조건     — Given/When/Then (해피 + 엣지 + 예외)
## Definition of Done
## 비범위
## INVEST
```

**정의 문서**: `feature-story/version/0.0.1v/feature-story.md`

---

### 2.3 PES (Product/Epic/Story 3계층)

**트리거**: 다음 **모두** 만족
- 추정 작업 시간 1~3주 (Story 5~15개)
- 단일 BC 또는 인접 BC 2개가 한 주제로 묶임
- Story 5+개로 자연스럽게 갈라짐
- 정량 KPI 정의 가치 있는 Outcome 존재
- 설계 갈림길 0~2건 이하

**해결한 문제**: 중규모 기능을 Product Outcome → Epic 목표 → Story AC 3계층으로 연결. 단순 task 목록을 만들 때 놓치는 제품 수준 성과 지표를 강제.

**핵심 구조**: Product(성과·KPI·범위·Epic 목록) → Epic(목표·인수 시나리오·Story 목록) → Story(AC·DoD·INVEST)

**정의 문서**: `pes/version/0.0.1v/pes.md`

---

### 2.4 SDD-lite (팀·계층 간 핸드오프)

**트리거**: 다음 중 **2개 이상** 만족
- 변경 사실을 다른 저장소/팀/계층에 인계 (BE→FE, BC→BC)
- 여러 Story 묶음 결과를 한 번에 검증할 Runbook 필요
- 호출 가능/금지 인벤토리 화이트리스트 필요
- 계약 명세화 필요 (응답 래퍼·에러 포맷·헤더·인증)
- PES만으로 부족하지만 풀 SDD까지는 과함

**해결한 문제**: 여러 Story가 끝난 뒤 결과를 다른 팀·계층에 인계할 때 정합성 보장. 변경 사실 + 작업 체크리스트 + 검증 절차 3축을 단일 문서로 통합. 작업 체크리스트는 path·what·why·check 4축 강제.

**핵심 구조**: Context → 스냅샷 → 인벤토리(화이트리스트) → 계약 → 작업 체크리스트(4축) → 검증 Runbook → Open Questions → 자가 점검

**정의 문서**: `sdd-lite/version/0.0.1v/sdd-lite.md`

---

### 2.5 SDD (System Design Document, 풀버전)

**트리거**: 다음 중 **3개 이상** 만족
- 추정 작업 기간 1~2개월 이상 (Story 15+개, Epic 3+개)
- BC 전체 정합성 개선 또는 다중 BC 협력 재설계
- 설계 갈림길 3건 이상 (Option A/B/C 비교 필요)
- 컴포넌트 배치 다이어그램·핵심 플로우 시퀀스 필수
- 실패 모드 매트릭스·로깅 정책·관측 지표 사전 설계
- 마이그레이션 단계 존재 (Product 의존성 그래프·환경별 설정)
- 인프라 + 도메인이 한 묶음

**해결한 문제**: 대형 재설계에서 설계 결정과 거부된 대안을 코드에 남기지 못하는 문제 해결. Product Vision → 배경/문제 → 설계 결정 → 대안 검토 → 아키텍처 → 실패 모드 → 롤아웃 → KPI 전 흐름을 한 파일에 담는다.

**핵심 구조**: Product Vision → 배경·문제 → 목표 → 설계 결정 → 대안 검토 → 전체 아키텍처(ASCII) → 실패 모드 매트릭스 → 롤아웃·마이그레이션 → KPI → Scope → Epic 목록

**정의 문서**: `sdd/sdd.md` (풀버전 정의 + 실제 사례 10개 `sdd/in-progress/`, `sdd/done/`)

---

## 3. 양식 선택 기준 요약

| 작업 성격 | 1차 선택 | 인계 필요 시 추가 |
|----------|---------|----------------|
| 1시간 이내, 파일 1개 | one-line-spec | — |
| 1~3일, BC 1개, 단일 PR | feature-story | sdd-lite |
| 1~3주, Story 5+개, KPI 존재 | pes | sdd-lite |
| 1~2개월+, 갈림길 3+, 다이어그램 필수 | sdd(풀) | (sdd 내장) |

**설계 복잡도 오버라이드**:
- 갈림길 3건+ → sdd(풀)로 강제 상향
- 외부 시스템 인계 필요 → sdd-lite 병행 작성

---

## 4. 상태 폴더 체제 (0.0.1v 도입)

### 4.1 4단계 상태

| 폴더 | 정의 | 진입 조건 |
|------|------|---------|
| `backlog/` | 아이디어·초안 단계. 양식 미완성 | SDD 작성 시작 전 또는 설계 진행 중 |
| `ready/` | 설계 완료. Story 정의됨. 착수 대기 | 해당 양식의 모든 Story·AC 정의 완료 |
| `in-progress/` | 현재 마일스톤에 1개 이상 Story 포함 | 마일스톤 대상으로 선정된 시점 |
| `done/` | 모든 Story 완료, 운영 진실 소스 반영 | Epic 전체 완료 + 코드/ADR 반영 확인 |

### 4.2 적용 대상

0.0.1v에서 다음 5개 양식 디렉토리에 동일한 4폴더 구조를 적용:

```
workspectrum/
├── sdd/          {backlog, ready, in-progress, done}
├── feature-story/{backlog, ready, in-progress, done}
├── one-line-spec/{backlog, ready, in-progress, done}
├── pes/          {backlog, ready, in-progress, done}
└── sdd-lite/     {backlog, ready, in-progress, done}
```

### 4.3 마일스톤과의 연결

- `milestone.md` 인벤토리 테이블의 Product 참조는 `{상태폴더}/{파일명}` 경로로 표기
- 마일스톤 전환 시 파일을 해당 상태 폴더로 이동하고 milestone.md 참조 경로도 동시 갱신
- `in-progress` 파일이 여러 개인 것은 정상 — 마일스톤에서 여러 Product 동시 진행이 표준 패턴

---

## 5. 0.0.1v 적용 현황 스냅샷 (2026-06-28)

### SDD 상태 현황

| 상태 | 파일 |
|------|------|
| `in-progress/` | product-auth, product-log, product-op, product-aisuggestion, Product(UserBC), product-infra-deploy, product-infra-network, product-infra-ops |
| `ready/` | product-ai-interactive-roadmap, product-load-test |
| `done/` | product-card, product-learningFacade |
| `backlog/` | (없음) |

### feature-story / one-line-spec / pes / sdd-lite

0.0.1v 기준 실제 작업 파일 미존재. 폴더 구조만 준비 완료. 마일스톤 M2 이후 해당 양식으로 작업 문서가 생성될 때 각 상태 폴더에 배치.

---

## 6. 다음 버전 신호 (0.0.2v 트리거)

다음 중 하나가 발생하면 `0.0.2v/` 를 신설하고 본 버전은 보존:

- 양식 트리거 조건 변경 (시간 기준·조건 수 변경)
- 새 양식 추가 (예: `epic-spec`, `runbook`)
- 상태 단계 재정의 (예: `in-progress` 세분화, `archived` 추가)
- 양식 간 졸업 경로 변경

---

## 7. 참조

| 대상 | 경로 |
|------|------|
| One-line-spec 정의 | `../../pes/workspectrum/one-line-spec/version/0.0.1v/one-line-spec.md` |
| Feature-story 정의 | `../../pes/workspectrum/feature-story/version/0.0.1v/feature-story.md` |
| PES 정의 | `../../pes/workspectrum/pes/version/0.0.1v/pes.md` |
| SDD-lite 정의 | `../../pes/workspectrum/sdd-lite/version/0.0.1v/sdd-lite.md` |
| SDD 풀버전 정의 | `../../pes/workspectrum/sdd/sdd.md` |
| 마일스톤 M1 | `../../milestones/version/0.0.1v/milestone.md` |
