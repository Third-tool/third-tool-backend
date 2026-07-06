# living-docs

> **성격**: 백엔드팀이 상시 참고하는 **최신 상태 자산** = **백엔드 인덱스**. 버전 스냅샷 아님, 항상 덮어쓰기.
> **성장 규칙**: 폴더당 초기 파일 1개 → 커지면 역할·서브도메인 기준으로 자연 분화. 버전 경계 없음.
> **관리 주체**: 백엔드 개발팀. 코드가 진실 소스인 영역은 여기가 지도 역할.
> **최종 목적**: 프로젝트 전반 자산은 궁극적으로 `workflow/quration/` 포트폴리오 큐레이션의 재료.

---

## workflow 4-anchor 역할 경계

| 구간 | 의미 | 시간축 | 예시 |
| --- | --- | --- | --- |
| `workflow/task/milestones/version/{Nv}/` | 그 버전이 만들어낸 결과·수치 **스냅샷** (박제) | 버전 확정 시점의 사진 | outcome.md, cost.md, performance.md, ai-eval.md, troubleshooting/ |
| `workflow/living-docs/` (본 폴더) | 버전을 지나며 갱신되는 **현재 상태 자산** (덮어쓰기) | 항상 최신, 팀이 함께 소유 | 현재 ERD, 현재 아키텍처, 현재 API/에러코드 카탈로그, 트랜잭션·인덱스 지도 |
| `workflow/topologys/` | 그 구간 동안은 **고정되는 수평 제약** | pin된 그래프·불변식 | BC 경계, 의존 방향, 트랜잭션·인덱스·context-engineering·ai-eval 규칙 |
| `workflow/quration/` | 위 3 anchor를 재료 삼는 포트폴리오·자소서 큐레이션 | 지원할 때마다 재구성 | milestone별 후보 등재, portfolio/selected.md |

**규칙 위반이 발생하면**: living-docs가 아니라 topology를 재pin (re-phase). living-docs는 topology 하위에서 "현재 상태"만 반영.

---

## Tier 리스트

### Tier 1 — 코어 인덱스 (Tier 1a는 자산·Tier 1b는 규칙 대응 지도)

**Tier 1a — 자산 인덱스** (5)

| 폴더 | 파일 | 무엇을 담나 |
| --- | --- | --- |
| [`erd/`](./erd/) | `erd.md` | 22개 테이블 관계 지도, Flyway **V1~V30** 스택 (M4 Card BC 재편 · Card→Axis 3-phase 반영) |
| [`architecture-system-design/`](./architecture-system-design/) | `architecture.md` | 백엔드 4-레이어 · 6-BC · 헥사고날 · 도메인 패턴 카탈로그 |
| [`infra-map/`](./infra-map/) | `infra.md` | AWS · EC2 · ECR · CI/CD · 컨테이너 스택 |
| [`api-spec/`](./api-spec/) | `api.md` | Controller 12종 · 엔드포인트 ~90개 카탈로그 |
| [`error-code/`](./error-code/) | `error-code.md` | 전체 ErrorCode 카탈로그 + HTTP 매핑 |

**Tier 1b — topology 규칙 대응 지도** (3 + 신설 예정)

| 폴더 | 파일 | 무엇을 담나 · 대응 topology |
| --- | --- | --- |
| [`transaction-map/`](./transaction-map/) | `transaction-map.md` | 24 Application Service `@Transactional` 지도 · `topologys/transaction/` |
| [`index-catalog/`](./index-catalog/) | `index-catalog.md` | 현재 인덱스 ~30개 카탈로그 · `topologys/index/` |
| [`ai-eval/`](./ai-eval/) | `ai-eval.md` | AI eval 방법론·golden set 관리 프레임 · `topologys/ai-eval/` · `topologys/llm-cascade/` |

**신설 topology 15종 (2026-07-21 · M4 이후 확장) 대응 living-docs 후보** — 신설 필요 시 개별 폴더로 분화:

**HIGH 우선순위 6종** (1차 신설):
- `topologys/domain-modeling/` → `domain-glossary/glossary.md` (도메인 결정·Aggregate 카탈로그로 이미 흡수)
- `topologys/migration-policy/` → `erd/erd.md` (Flyway V번호 스택으로 이미 흡수) · `docs/runbooks/db-recovery.md` (M7 신설 예정)
- `topologys/observability/` → **`ops-health-board/` (M7 신설 예정)** · `infra-map/infra.md`
- `topologys/secrets-management/` → `infra-map/infra.md` §Secrets 참조 · `docs/runbooks/secrets-rotation.md` (M8 신설 예정)
- `topologys/environments/` → `infra-map/infra.md` §환경 분리 (M7 확장 예정)
- `topologys/llm-cascade/` → `ai-eval/ai-eval.md` §Cascade 흐름 (M6 실측 시 확장 예정)

**MEDIUM 우선순위 7종** (2차 신설):
- `topologys/security-authn/` → `domain-glossary/glossary.md` §User BC · `error-code/error-code.md` §Auth
- `topologys/rate-limiting/` → `api-spec/api.md` §429 응답 · `observability` 협력
- `topologys/runbook-authoring/` → **`docs/runbooks/` (M7~M8 신설 예정)**
- `topologys/release-gate/` → `workflow/task/milestones/release/version/{Nv}/release.md`
- `topologys/prompt-engineering/` → `src/main/resources/ai/prompts/` (classpath) · `ai-eval` §M4 baseline
- `topologys/query-guidelines/` → `index-catalog/index-catalog.md` · `transaction-map/transaction-map.md`
- `topologys/ai-cost/` → `workflow/task/milestones/version/{Nv}/cost.md` (M6~ 실측 반영)

**LOW 우선순위 2종** (3차 신설):
- `topologys/naming-conventions/` → `docs/adr/ADR004`·`ADR022`·`ADR023` · `domain-glossary/glossary.md`
- `topologys/role-catalog/` → `ai-eval/ai-eval.md` §role 4종 baseline · `src/main/resources/ai/catalog/`

### Tier 2 — 팀 참조 자산 (4)

| 폴더 | 파일 | 무엇을 담나 |
| --- | --- | --- |
| [`decisions/`](./decisions/) | `decisions.md` | ADR001~ADR023 카테고리별 인덱스 |
| [`boundary-trace/`](./boundary-trace/) | `boundary.md` | BC 간 실제 in/out 흐름 지도 + 이벤트 2건 |
| [`domain-glossary/`](./domain-glossary/) | `glossary.md` | 도메인 용어사전 (DOMAIN.md 발췌 · 38+ 용어) |
| [`event-catalog/`](./event-catalog/) | `events.md` | 도메인 이벤트 카탈로그 + ADR007 규약 |

### Tier 2b — Claude Code / 문서 관행 (2)

| 폴더 | 파일 | 무엇을 담나 |
| --- | --- | --- |
| [`tooling/`](./tooling/) | `skill-catalog.md` | Claude Code skill 채택·검토 상태 인덱스 |
| [`docs-conventions/`](./docs-conventions/) | `docs-conventions.md` | context 엔지니어링 실전 지도 · 4-anchor 진입 흐름 |

### Tier 3 — 0.0.4v 이후 (지금은 빈 상태 유지)

| 폴더 | 무엇을 담을 예정 |
| --- | --- |
| [`feedback/`](./feedback/) | 유저/팀 피드백 최신본 (유저 진입 후) |
| [`ops-health-board/`](./ops-health-board/) | 운영 대시보드 · 알람 지도 (프로덕션 운영 개시 후) |
| [`test/`](./test/) | 테스트 커버리지 · 계층별 현황 지도 |

---

## 파일 성장 정책

- **초기**: 폴더당 파일 1개 (`{topic}.md`).
- **성장**: 역할·서브도메인 기준으로 분할. 예:
  - `erd/erd.md` → `erd/card.md`, `erd/deck.md`, `erd/learning.md` …
  - `api-spec/api.md` → `api-spec/card.md`, `api-spec/deck.md`, `api-spec/learning-facade.md` …
  - `transaction-map/transaction-map.md` → BC별 트랜잭션 지도로 분화
- **인덱스**: 파일이 여러 개가 되면 폴더별 `README.md`를 인덱스로 신설 (초기엔 불필요).
- **버전 경계 없음**: `versions/{Nv}/` 하위 스냅샷 유지하지 않음. Git history가 진화 기록.

---

## Topology와의 관계 (재확인)

일부 이름이 topology와 living-docs 둘 다에 존재. 성격 구분:

| 관점 | topology (pin) | living-docs (인덱스) |
| --- | --- | --- |
| api-spec | HTTP graph 위상 · 계약 규칙 | 현재 엔드포인트 카탈로그 |
| error-code / common-core | ErrorCode registry 위상 · 등록 규칙 | 현재 등록된 코드 카탈로그 |
| bounded-context | BC 경계 · 의존 방향 pin | BC 간 실제 in/out 흐름 지도 |
| persistence · transaction · index | 스키마 · 트랜잭션 · 인덱스 원칙 | 실제 매핑·서비스·인덱스 카탈로그 |
| context-engineering | 문서 배치·명명 원칙 | 실전 문서 흐름 지도 |
| ai-eval | 평가 SLO·프레임 | 골든 set·방법론 |

**원칙**: topology 위반 시 living-docs 아니라 topology를 재pin. living-docs는 topology 하위에서 "지금 상태" 반영만.

---

## 관련 위치

- 규범: `docs/PACKAGE.md`, `docs/DOMAIN.md`, `.claude/rules/conventions.md`
- ADR: `docs/adr/`
- 버전 결과 스냅샷: `workflow/task/milestones/version/{Nv}/`
- 수평 제약 pin: `workflow/topologys/`
- 포트폴리오 큐레이션: `workflow/quration/`

*최신 갱신: 2026-07-21 · **topology 15종 전수 신설 반영** — HIGH 6종 (domain-modeling · migration-policy · observability · secrets-management · environments · llm-cascade) + MEDIUM 7종 (security-authn · rate-limiting · runbook-authoring · release-gate · prompt-engineering · query-guidelines · ai-cost) + LOW 2종 (naming-conventions · role-catalog) — topology 총 11 → 26. Tier 1b 대응 지도에 신설 topology 링크 추가.*
*이전 갱신: 2026-07-21 · **M4 반영** — Card BC 대재편 (Mode 4값 · OnFieldBudget 폐기 · createdMode 하이브리드) + Card→Axis 직접 매핑 (V28~V30) + AI role catalog 4종 확장 (planner/designer/problem-solver 신설) + CardViewedEvent 신설. 갱신 파일: `erd.md` · `domain-glossary/glossary.md` · `error-code.md` · `event-catalog/events.md` · `api-spec/api.md` · `index-catalog/index-catalog.md` · `ai-eval/ai-eval.md` · `boundary-trace/boundary.md`*
*이전 갱신: 2026-07-03 · 5개 신설 폴더 반영 (transaction-map, index-catalog, ai-eval, tooling, docs-conventions) + Tier 재편성*
