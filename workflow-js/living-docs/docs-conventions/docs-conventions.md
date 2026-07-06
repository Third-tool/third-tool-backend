# Docs Conventions (Living)

> **성격**: living-docs — 항상 최신본. Context 엔지니어링 실전 지도 (문서가 지금 어떻게 배치·참조되고 있는지).
> **성장 방향**: 관행이 늘면 주제별 파일로 분화 (`docs-conventions/naming.md`, `docs-conventions/linking.md` …).
> **관련 topology**: `workflow/topologys/context-engineering/v1-context-engineering.md` (수평 제약 pin).
> **목적**: Claude Code(및 유사 에이전트)가 이 프로젝트에 진입해서 **최소 컨텍스트로 최대 판단**을 내리도록 문서 흐름을 유지.

---

## 1. 4-anchor 진입 흐름

새 세션 진입 시 Claude가 자연스럽게 아래 순서로 컨텍스트를 로딩:

```
1. CLAUDE.md (자동 로드)
    ├─ 5개 rules 파일 위치
    ├─ 5개 docs 파일 위치
    └─ workflow/ 4-anchor 소개

2. 사용자 요청 분석
    ├─ "리팩토링"·"박제"·"큐레이션" 키워드 → anchor 결정
    └─ 4-anchor 판단:
        ├─ 결과 박제? → workflow/task/milestones/version/{Nv}/
        ├─ 지금 상태 인덱스? → workflow/living-docs/
        ├─ 수평 규칙 pin? → workflow/topologys/
        └─ 포트폴리오? → workflow/quration/

3. 해당 anchor의 README 또는 index 파일 참조
    ├─ living-docs/README.md → Tier 리스트로 관련 폴더 진입
    ├─ topologys/references/toplogy.md → 방법론 확인
    └─ quration/README.md → 사용 흐름

4. 실제 작업 anchor의 파일 진입
    └─ 각 파일 상단 Front matter (성격·시간축·성장 방향)로 3초 판단
```

---

## 2. 파일별 진입점 표 (CLAUDE.md 매핑)

| 필요한 것 | 진입점 |
| --- | --- |
| 작업 흐름 · Story 실행 절차 | `.claude/rules/workflow.md` |
| 도메인 의도·용어·불변식 | `docs/DOMAIN.md` |
| 패키지·BC·레이어 구조 | `docs/PACKAGE.md` |
| ADR (아키텍처 결정) | `docs/adr/index.md` |
| 코드 작성 컨벤션 | `.claude/rules/conventions.md` |
| Story Reviewer 5관점 | `.claude/rules/review.md` |
| 브랜치·커밋·PR 규칙 | `.claude/rules/pr-commit.md` |
| ADR 작성 트리거 | `.claude/rules/adr.md` |
| UI 문구 정책 | `docs/ux/wip-language.md` |
| 현재 Epic/Story | `workflow/epics/Epic.md` · `workflow/stories/Story.md` |

---

## 3. Front matter 3~5줄 헤더 규칙

모든 living-docs 파일은 상단에 blockquote 헤더:

```markdown
# {제목} (Living)

> **성격**: living-docs — 항상 최신본. {한 줄 설명}
> **성장 방향**: {단일 파일 → 서브 파일 분화 규칙}
> **관련 topology**: `workflow/topologys/{topic}/v{n}-{topic}.md` (있으면)
> **원본**: {진실 소스 코드/파일 위치}
> **사용법**: {언제 이 파일을 여는가}

---
```

topology 파일도 유사 헤더:

```markdown
# Pinned Topology — `{topic}` (Phase: `v{n}`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님

**목적**: {한 줄}
```

---

## 4. 코드가 진실 소스인 영역 (별도 docs 만들지 않음)

| 영역 | 진실 소스 |
| --- | --- |
| API 명세 | Controller + Request/Response DTO + Swagger UI |
| DB 스키마 | Flyway `V*.sql` + JPA 매핑 |
| 테스트 매트릭스 | 테스트 코드 + 메서드명 |
| 패키지·디렉토리 사실 | `src/main/java/com/example/thirdtool/` 트리 |
| 인프라 사실 | `.github/workflows/*.yml` + `application.yml` + AWS 콘솔 |

**규칙**: 위 영역은 별도 명세 docs를 만들지 않음. living-docs는 **지도·인덱스** 성격만.

---

## 5. 상호 참조 링크 규칙

- **상대경로 사용**. 절대경로 (`C:/` · `/Users/`) 금지.
- **repo root 기준**: `docs/adr/ADR021.md` (CLAUDE.md·workflow 파일에서)
- **인접 폴더**: `../boundary-trace/boundary.md` (다른 living-docs 폴더로)
- **memory 상호참조**: `[[name]]` (memory 시스템 내부)
- **깨진 링크는 이슈**. 이동 시 links grep으로 갱신.

---

## 6. living-docs 파일 성장 정책

- **초기**: 폴더당 초기 파일 1개 (`{topic}.md`)
- **성장**: 역할·서브도메인 기준으로 자연 분화
- **인덱스**: 파일이 여러 개가 되면 폴더별 `README.md` 신설 가능 (초기엔 불필요)
- **버전 폴더 X**: `versions/{Nv}/` 하위 스냅샷 유지하지 않음 (최신본 1개 정책)

**예시 성장**:
```
erd/
├── erd.md              ← 초기 1개
├── card.md             ← 성장 후
├── deck.md
├── learning.md
└── README.md           ← 파일 여러 개 되면 신설
```

---

## 7. Topology와 겹치는 이름 처리

일부 이름이 topology와 living-docs 둘 다에 존재. 성격 구분:

| 이름 | topology (pin) | living-docs (인덱스) |
| --- | --- | --- |
| api-spec | HTTP graph 위상 · 계약 규칙 | 현재 엔드포인트 카탈로그 |
| error-code / common-core | ErrorCode registry 위상 · 등록 규칙 | 현재 등록된 코드 카탈로그 |
| bounded-context | BC 경계 · 의존 방향 pin | 실제 in/out 흐름 지도 |
| persistence · transaction · index | 스키마 · 실행 · 인덱스 원칙 | 실제 매핑·서비스·인덱스 카탈로그 |
| context-engineering | 문서 배치·명명 원칙 pin | 실전 흐름 지도 (본 파일) |
| ai-eval | 평가 SLO·프레임 | 골든 set·방법론 |

**원칙**: topology 위반 발생 시 living-docs 아니라 topology를 재pin. living-docs는 topology 하위에서 "지금 상태" 반영만.

---

## 8. Front matter 실전 patterns

각 파일이 스캔되는 순간 다음을 3초 안에 파악할 수 있어야 함:

- **어느 anchor인지** — 파일 경로 (workflow/task/…, living-docs/…, topologys/…, quration/…)
- **성격** — 박제 / 인덱스 / pin / 큐레이션
- **원본** — 진실 소스 어디에 있나
- **관련 파일** — 참조 흐름

이 4가지가 첫 5줄 blockquote에 다 들어가면 이상적.

---

## 9. 세션 오프닝 시 Claude가 자동 훑는 우선순위

1. **auto memory** (`~/.claude/projects/{project}/memory/MEMORY.md`) → 개인 컨텍스트
2. **CLAUDE.md** → 프로젝트 진입 포인터
3. **사용자 요청** → anchor 결정
4. **해당 anchor의 README 또는 index** → 목표 파일 특정
5. **목표 파일 상단 Front matter** → 3초 판단
6. **필요 시 원본 (코드·Flyway·ADR)** 진입

이 순서로 접근하면 컨텍스트 절약 + 판단 정확도↑.

---

## 10. 감시 포인트 (topology `v1-context-engineering.md` §2 Invariants)

작업 중 다음이 보이면 문서 관행 위반 가능성 → 리뷰:

- CLAUDE.md에 100줄 초과 상세 서사 등장 (진입 포인터 성격 상실)
- topology 파일 명명 규칙 (`{topic}/v{n}-{topic}.md`) 위반
- 절대경로 링크 (`C:/`, `/Users/`) 등장
- Front matter 없는 living-docs 파일 등장
- ADR 신설했는데 `docs/adr/index.md` 갱신 누락
- 코드가 진실 소스인 영역에 별도 명세 docs 신설 시도

---

## 참조

- 관련 topology: `workflow/topologys/context-engineering/v1-context-engineering.md`
- 관련 living-docs: `tooling/skill-catalog.md`
- 관련 rules: `.claude/rules/workflow.md`, `.claude/rules/adr.md`
- 4-anchor 정의: `workflow/living-docs/README.md`, `workflow/quration/README.md`

*최신 갱신: 2026-07-03 · 초기 지도. 실전에서 발견되는 관행을 계속 반영*
