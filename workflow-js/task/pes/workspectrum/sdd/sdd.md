# [SDD] 양식 정의 (풀버전)

> 작업 양 스펙트럼 최상위. 1~2개월 단위 도메인 재설계·BC 전체 정합성 개선·인프라+도메인 묶음 작업의 표준 양식.
> 본 디렉토리(`workflow/task/pes/workspectrum/sdd/`)의 `in-progress/`에 있는 실제 사례들이 본 양식의 gold standard이다.
> 본 문서는 그 파일들이 공통으로 사용하는 섹션 구조·서브섹션 · 예시를 규범으로 정리한 것이다.

---

## 사용 시점 (트리거)

다음 조건 중 **3개 이상**을 만족할 때 본 양식을 쓴다.

- [ ] 추정 작업 기간 **1~2개월 이상** (Story 15+개, Epic 3+개)
- [ ] **BC 전체** 정합성 개선 또는 **다중 BC** 협력 재설계
- [ ] 설계 갈림길 **3건 이상** — 각 갈림길마다 Option A/B/C 비교가 필요
- [ ] 컴포넌트 배치 다이어그램·핵심 플로우 시퀀스가 **필수** (텍스트만으론 설명 안 됨)
- [ ] **실패 모드 매트릭스**·로깅 정책·관측 지표를 사전 설계해야 함
- [ ] 마이그레이션 단계가 있음 (Product 의존성 그래프, Epic·Story 의존성, 환경별 설정 분기)
- [ ] 인프라 + 도메인이 한 묶음으로 진행

**졸업 신호 → 완료/아카이브**:
- 모든 Epic 완료 + 핵심 ADR 작성 끝
- 운영 진실 소스(코드/Flyway/Swagger/DOMAIN.md/PACKAGE.md)에 결과 반영 완료
- 본 파일은 그 시점에 `done/` 디렉토리로 이동하거나, 다음 Product의 의존 문서로 인용된다

---

## 섹션 순서 (요약 표)

한 파일 안에 다음 순서로 Product 15섹션 → Epic N개 → Story N-M개가 이어진다. 순서 벗어남 자체가 리뷰 지적 대상.

### Product 레벨 (필수 15섹션)

| # | 섹션 | 필수/선택 | 목적 |
|---|---|---|---|
| 1 | `# [Product N] {이름}` | 필수 | 파일당 1개. Product 식별자 + 이름 |
| 2 | `## Product Vision` | 필수 | 2~3줄 인용 블록. "무엇을 달성" 그림 |
| 3 | `## 배경 및 문제` | 필수 | As-Is / 발생 문제 / 왜 지금 3단 구성 |
| 4 | `## 목표 (To-Be)` | 필수 | 어떤 코드/구조가 어떻게 바뀌는가 |
| 5 | `## 설계 결정 (Design Decisions)` | 필수 | 큰 갈림길의 결정 · 거부된 옵션 근거 명시 |
| 6 | `## 대안 검토 (Alternatives Considered)` | 필수 | 갈림길마다 Option A/B/C 비교 |
| 7 | `## 전체 아키텍처 (High-Level Architecture)` | 필수 | 컴포넌트 배치 · 핵심 플로우 · Out-of-Process 의존 |
| 8 | `## 실패 모드 / 운영 관측 (Failure Modes & Observability)` | 필수 | 실패 시나리오 표 · 로깅 정책 · 관측 지표 |
| 9 | `## 롤아웃 / 마이그레이션 (Rollout)` | 필수 | 전제 · Product 의존성 · Epic·Story 그래프 · 환경별 설정 분기 |
| 10 | `## 성공 지표 (KPI)` | 필수 | 정량 지표 표(`지표 · 목표 값 · 측정 방법`) |
| 11 | `## Scope` | 필수 | In Scope / Out of Scope |
| 12 | `## 대상 사용자` | 필수 | 역할별 가치 |
| 13 | `## 연결된 Epic 목록` | 필수 | 체크박스 리스트 |
| 14 | `## 관련 문서` | 필수 | 의존 Product · ADR · DOMAIN.md/PACKAGE.md 갱신 지점 |
| 15 | `## 열린 질문 (Open Questions)` | 필수 | 미결 항목 · 다음 의사결정 트리거 |
| 15+ | `## 제품 수준 완료 기준 (Product-level DoD)` | 선택 | 대형 Product는 §Scope 뒤에 추가 가능 |

### Epic 레벨 (Product 아래 이어짐)

한 Epic 블록은 다음 7~8개 서브섹션으로 구성:

| # | 섹션 | 필수/선택 |
|---|---|---|
| 1 | `# [Epic N] {이름}` | 필수 |
| 2 | `## 목표` (또는 `## Epic 목표`) | 필수 |
| 3 | `## 배경` | 필수 |
| 4 | `## 포함 Story` | 필수 |
| 5 | `## Epic 인수 시나리오` | 선택(권장) |
| 6 | `## Epic 완료 기준 (DoD)` | 필수 |
| 7 | `## 내부 메모 / 제약 사항` | 선택 |
| 8 | `## Epic 기술 결정 / 대안 (Epic-Level Alternatives)` | 선택 |

### Story 레벨 (Epic 아래 이어짐)

| # | 섹션 (H3 사용) | 필수/선택 |
|---|---|---|
| 1 | `## [Story N-M] {이름}` | 필수 (H2) |
| 2 | `### User Story` | 필수 |
| 3 | `### 설명` | 필수 |
| 4 | `### 완료 기준 (AC)` | 필수 |
| 5 | `### Definition of Done` | 필수 |
| 6 | `### 스토리 포인트` | 필수 |
| 7 | `### 의존성` | 필수 |
| 8 | `### [명세 변경 이력]` | 선택 (해당 시) |

---

## 양식 골격 상세

### Product 레벨 (` # [Product N] {이름} `)

```markdown
# [Product N] {이름}

## Product Vision
> {2~3줄 인용 블록 — 본 Product가 달성하려는 그림}

## 배경 및 문제
- 현재 상황 (As-Is)
  - {코드/구조의 현 상태 1}
  - {현 상태 2}
- 발생하는 문제
  - {문제 1 — 측정 가능하거나 사례 동반}
  - {문제 2}
- 왜 지금 해결해야 하는가
  - {타이밍 사유 — 의존 Product 완료, 인프라 변경, 외부 트리거 등}

## 목표 (To-Be)
- {목표 1 — 어떤 코드/구조가 어떻게 바뀌는가}
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

### {갈림길 1 제목}
**Option A — {요지}**
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

### 컴포넌트 배치
```
{ASCII 박스 다이어그램 — Presentation/Application/Domain/Infrastructure 4계층}
```

### 핵심 플로우
**1. {플로우명}**
```
{ASCII 시퀀스 — 클라이언트 → Controller → Service → Domain → Repository}
```

**2. {플로우명}**
...

### Out-of-Process 의존
- **{외부 시스템 1}**: {역할, 어느 컴포넌트가 위임 호출}
- **{외부 시스템 2}**: ...

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답
| 시나리오 | ErrorCode | HTTP | 클라이언트 권장 동작 |
| --- | --- | --- | --- |
| ... | ... | ... | ... |

### 로깅 정책
- **항상 기록**:
  - {로그 항목 1}
- **debug**: {조건}
- **절대 금지**:
  - {평문 비밀번호 / 토큰 원문 / input reflection 등}

### 관측 지표 (해당 시 — Metrics 도입 시)
- `{metric_name}{labels}` — {타입} — {의미}

## 롤아웃 / 마이그레이션 (Rollout)

### 전제
{현재 트래픽·사용자 수 상황. 마이그레이션을 일괄/단계 중 어떻게 진행하는지}

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
| 항목 | dev | prod |
| --- | --- | --- |
| ... | ... | ... |

## 성공 지표 (KPI)
| 지표 | 목표 값 | 측정 방법 |
| --- | --- | --- |
| {정량 지표 1} | ≥/=/0건/100% | {계산 공식 · 관측 소스} |
| {정량 지표 2} | ... | ... |

## Scope
**In Scope**:
- {포함 1}
- {포함 2}

**Out of Scope**:
- {제외 1} — 사유
- {제외 2} — 사유

## 대상 사용자
- {역할 1 — 어떤 가치 받는가}
- {역할 2 — 어떤 가치 받는가}

## 연결된 Epic 목록
- [ ] Epic 1: {제목}
- [ ] Epic 2: {제목}
- [ ] Epic 3: {제목}

## 관련 문서
- 의존 Product: {링크}
- 관련 ADR: {ADR NNN, NNN}
- DOMAIN.md / PACKAGE.md 갱신 예정 섹션: {경로}

## 열린 질문 (Open Questions)
- {미결 항목 1 — 다음 의사결정 트리거}
- {미결 항목 2}

## 제품 수준 완료 기준 (Product-level DoD)   ← 선택. 대형 Product에 권장.
- [ ] 모든 Epic DoD 통과
- [ ] 핵심 ADR N건 발행
- [ ] 관련 문서(§관련 문서) 반영 완료
- [ ] 성공 지표 지표당 초기 관측값 확보
```

### Epic 레벨 (` # [Epic N] {이름} `)

```markdown
# [Epic N] {이름}

## 목표
{한 문장 — Epic이 끝났을 때 무엇이 가능한지}

## 배경
{본 Epic이 Product 전체에서 차지하는 위치 + 선행 의존}

## 포함 Story
- Story N-1: {제목}
- Story N-2: {제목}
- Story N-3: {제목}

## Epic 인수 시나리오
- Given {선행 상태}
- When {트리거}
- Then {관측 가능한 결과}

*(엣지)* Given ... / When ... / Then ...

## Epic 완료 기준 (DoD)
- [ ] 포함 Story 모두 완료
- [ ] 통합 테스트 통과
- [ ] ADR {NNN} 작성 (해당 시)
- [ ] 성공 지표 관측 시작

## 내부 메모 / 제약 사항
{진행 중 발견한 사항·재조정 사항. `[명세 변경 이력]` 블록으로 원안과의 편차 기록}

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)
{Product 수준 결정과 별개로 Epic 내부에서 갈렸던 선택}
- **{결정 1}**: ...
- **{결정 2}**: ...
```

### Story 레벨 (` ## [Story N-M] {이름} `)

```markdown
## [Story N-M] {이름}

### User Story
- As a {역할}
- I want {원하는 행위}
- so that {얻는 가치}

### 설명
{구현 단서 — 메서드 시그니처·정적 팩토리·불변식·예외 패턴(ErrorCode)·포트명}

**핵심 클래스/인터페이스**:
- `{FullyQualifiedName}` — {역할 한 줄}

**주요 메서드**:
- `{Class}.{method}({param types})` — {행위 한 줄}

### 완료 기준 (AC)
- Given {선행 상태} / When {트리거} / Then {기대 결과}
- Given ... / When ... / Then ...
- *(엣지 - 사유)* Given ... / When ... / Then ...
- *(예외 - 사유)* Given ... / When ... / Then {ErrorCode + HTTP}

### Definition of Done
- [ ] 구현 (구체 클래스명 · 파일 경로)
- [ ] 단위 테스트 ({N건}, 해피/엣지/예외 각 케이스)
- [ ] 슬라이스 테스트 (해당 시 — `@DataJpaTest` / `@WebMvcTest`)
- [ ] 통합 테스트 (해당 시 — `@SpringBootTest`)
- [ ] ErrorCode 등록 (해당 시)
- [ ] Flyway 마이그레이션 (해당 시 — `V{N}__*.sql`)
- [ ] ADR 작성 (해당 시 — `ADR{NNN}`)
- [ ] DOMAIN.md / PACKAGE.md 갱신 (해당 시)

### 스토리 포인트
{0.5d / 1d / 2d / 3d — Estimable 미달이면 분할}

### 의존성
- 선행: Story N-K (이유)
- 후행: Story N-L (이유)

### [명세 변경 이력]   ← 선택. 원안 편차 기록.
- YYYY-MM-DD: {원안 → 실제 구현} 차이 + 변경 사유
```

---

## 섹션 작성 가이드

| 원칙 | 적용 |
| --- | --- |
| **한 파일 = 한 Product** | Product/Epic/Story 수직 통합. 별도 spec/plan/tasks 파일로 쪼개지 않음 |
| **거부된 옵션도 합리적 근거 명시** | "왜 이것이 아니고 저것인가"를 남겨 트레이드오프를 드러냄 |
| **결정 1줄 + 보상 1줄 + 비용 1줄** | Option B(선택)는 항상 비용과 보상을 명시 |
| **다이어그램은 ASCII로** | 외부 도구 의존 X. 텍스트 검색 가능. PR 리뷰 시 diff에 잘 잡힘 |
| **실패 시나리오 표를 사전 설계** | 구현이 끝난 뒤 끼워 넣지 않음. ErrorCode 등록도 본 표를 근거로 진행 |
| **명세 변경 이력 블록** | Story 진행 중 발견한 원안 편차는 코드만 바꾸고 끝내지 않음 — 본 블록에 기록 |
| **추정 식별자 금지** | 실제 클래스명·포트명·ErrorCode 접두사는 코드에서 확인해서 쓴다 |
| **ADR 트리거** | 새 패턴·새 기술 결정은 별도 ADR 발행. Story DoD에 ADR 항목 포함 |
| **Product Vision 인용 블록** | `> ` 로 시작. 2~3줄 이내. Product 본문 첫 진입에서 3초 안에 그림이 잡히게 |
| **KPI는 표로** | 산문 서술 대신 `지표 · 목표 값 · 측정 방법` 3열 표 강제 |
| **Scope는 In/Out 대비** | `**In Scope**` + `**Out of Scope**` 굵기 강조. 각 Out 항목마다 "— 사유" 명시 |
| **연결된 Epic 목록** | 체크박스로. 완료된 Epic은 `- [x]`. Epic 순서는 롤아웃 그래프와 일치 |

---

## 예시 파일 (gold standard)

이미 본 디렉토리 `in-progress/`에 풀버전 실제 사례들이 있다. 본 양식의 모든 섹션이 어떻게 채워지는지 다음 파일을 참조:

| 파일 | 특징 |
| --- | --- |
| `in-progress/Product.md` | User BC 도메인 정합성 개선. 가장 균형 잡힌 사례. 모든 섹션 풀 사용 |
| `in-progress/product-auth.md` | 인증 인프라 재설계. ADR 의존 사례. Epic·Story 계층 명확 |
| `in-progress/product-infra-deploy.md` | 컨테이너 배포 파이프라인. Epic 4개 · Story 계층 가장 깊음 |
| `in-progress/product-infra-network.md` | AWS 네트워크·도메인·DB 토대. Epic 3개(VPC / ALB+도메인 / RDS) |
| `in-progress/product-infra-ops.md` | 운영·보안 자동화. Secrets Manager · 백업 · 관측 Epic 2개 |
| `in-progress/product-log.md` | 구조화 로깅·MDC·에러 로깅. 관측성 중심 (Product-only) |
| `in-progress/product-op.md` | 운영 메트릭 가시화. Actuator/Prometheus/Grafana (Product-only) |
| `in-progress/product-fe-cdn.md` | FE CDN. 대안 A/B/C 4갈림길 풀 채움 |
| `in-progress/product-media.md` | 미디어 업로드. Epic 5개 (v1 4개 + v2 1개) · Story 18+개 |
| `in-progress/product-search.md` | 검색. Epic 5개 · Story 18+개 · ADR 4건 크로스 참조 |
| `in-progress/product-load-test.md` | k6 부하 테스트. Epic 3개 · Product 2 요약 포함 |
| `in-progress/product-learning-tower.md` | 학습 모델 재편(concepts[]·Layer·Roadmap/Selection·Card→Axis·Deck 폐기·Review dual-scope). Epic 6개 · 대형 사례 (0.0.2v fix issues #04·#05·#06·#07·#11·#13·#14 반영) |
| `in-progress/product-ai-suggestion.md` | 무상태 4-Port(Layer/Axis/Roadmap/Selections) 오케스트레이션 + Role Catalog 4개(backend/planner/designer/problem-solver). Static/LLM 이중 어댑터 (0.0.2v fix issues #09·#10 반영) |
| `in-progress/product-ai-interactive-roadmap.md` | RoadmapInteractionSession 상태기계(INIT/DRAFTED/REVIEWING/COMMITTED) + Selection 버전 정책(#11) + axisDraft/axisConsensus 흐름. product-ai-suggestion 4-Port 재사용 |
| `ADR-CANDIDATES.md` | ADR 후보 우선순위 추적 (메타) |

새 풀 SDD를 작성할 때는 위 사례 중 가장 가까운 패턴을 골라 섹션 구조를 그대로 복사한 뒤 내용만 채우는 것을 권장.

---

## fix 레이어 (진행 중 계획 변경 흡수)

SDD Product 마일스톤(`backlog → ready → in-progress → done`)이 굴러가는 **도중** 발견되는 정책 충돌·리팩토링·요구사항 변화는 별도 `fix/` 폴더의 문서로 흡수한다.

- 위치: `fix/fix-<핵심주제>.md`
- 골격: `fix/fix.md` §4 필수 섹션(0. 메타 · 1. 배경 · 2. 원본 대비 delta · 3. 새 목표 · 4. 대안 검토 · 5. Fix-Epic/Fix-Story · 6. ADR 승격 후보 · 7. 진실 소스 반영 계획)
- **Fix-Story는 SDD Story 골격과 동일 형식(User Story / 설명 / AC / DoD / SP / 의존성)을 사용**
- 완료 시 `done/` 또는 `version/{X.Y.Zv}/`로 아카이브. 원본 Product SDD는 반영 결과만 남음

상세 규범: [`fix/fix.md`](fix/fix.md)

---

## 워크플로우 위치

```
Story 명령 수신
   ↓
CLAUDE.md → workflow.md → 대상 판정
   ↓                        ↓
   │        {복잡도 · 기간 · BC 폭 판단}
   │                        ↓
   │      1건 · 1~2일   ─────► one-line-spec
   │      1건 · 3~5일   ─────► feature-story
   │      3~10 Story    ─────► pes (Product·Epic·Story)
   │      Story 5+ · 다중 BC ► sdd-lite
   │      Story 15+ · 1~2M  ► ★ sdd (본 양식) ★
   │                        ↓
   │        본 sdd.md 참조 · sdd/in-progress/product-*.md에 작성
   ↓
Plan mode → Story 순차 실행 → Reviewer 세션 → PR
```

---

## 참조

- 이전 스펙트럼: `../sdd-lite/version/0.0.1v/sdd-lite.md`
- 전체 스펙트럼: `../` (one-line-spec / feature-story / pes / sdd-lite / sdd)
- PES 정본 가이드: `../../references/001.md`
- ADR 인덱스: `../../../../../docs/adr/index.md`
- 도메인 의도 / 패키지 규칙: `../../../../../docs/DOMAIN.md`, `../../../../../docs/PACKAGE.md`
- 양식 진화: 본 양식은 SemVer로 진화. 변경 시 `version/{X.Y.Zv}/`에 새 버전을 두고 본 버전은 보존
