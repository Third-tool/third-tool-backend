# Rule: Workflow 구조 및 실행 프로토콜

> 작업 단위(Epic / Story / Product)가 어디에 있고, Claude Code가 어떤 순서로 그것을 소비하는지 정의한다.
> 본 규칙은 [`CLAUDE.md`](../../CLAUDE.md)의 "작업 흐름" 섹션을 강제 규칙으로 명문화한 것이다. CLAUDE.md와 충돌이 있으면 CLAUDE.md가 우선한다.

---

## 1. 디렉토리 구조 (단발 운영)

```
workflow/                  ← 프로젝트 루트 (gitignored, 외부 관리)
├── product/               # 프로덕트 컨텍스트 (장기 보존)
├── epics/                 # 현재 진행 중인 Epic 1개. 파일명 예: Epic-003.md
└── stories/               # 현재 진행 중인 Story 1개. 파일명 예: Story-003-2.md

private-docs/              ← 운영 reference 패키지 (gitignored, 외부 관리)
└── adr/, api/, domain/, table/, test/   # `private-docs.md` 규칙 참조
```

**단발 운영 원칙**: `epics/`와 `stories/` 하위에는 **현재 진행 중인 파일만 1개씩** 둔다. Story가 끝나면 다음 Story 파일로 교체한다. 과거 이력은 외부(Claude Desktop)에서 관리되므로 이 레포 안에서 Living 문서를 갱신·보존하지 않는다.

**Claude Code 산출물은 코드와 로컬 커밋 뿐이다.** workflow 폴더의 문서는 입력이지, Claude Code가 갱신하는 출력물이 아니다.

---

## 2. 작업 단위 계층

| 단위 | 위치 | 역할 |
| --- | --- | --- |
| Product | `workflow/product/` | 비즈니스 컨텍스트, 장기 방향, 사용자가 풀려는 문제 |
| Epic | `workflow/epics/Epic-{NNN}.md` | 여러 Story를 묶는 상위 목표 (예: "AxisAction → AxisTopic 도메인 전환") |
| Story | `workflow/stories/Story-{NNN}-{N}.md` | Claude Code가 1회 분량으로 실행하는 작업 단위 |

---

## 3. Story 실행 프로토콜 (필수 순서)

Story 명령("Story-X-X 진행해줘", "이 스토리 ㄱㄱ" 등)을 받으면 **반드시** 아래 순서로 진행한다.

### Step 1 — 읽기 (코드 작성 금지)

1. `workflow/stories/` 내 Story 문서를 읽는다.
2. `workflow/epics/` 내 Epic 문서를 상위 컨텍스트로 읽는다.
3. `workflow/product/` 의 프로덕트 컨텍스트를 필요한 만큼 참조한다.
4. **Story 문서가 명시한 reference 문서**(`private-docs/{adr,api,domain,table,test}/...`)를 전부 읽는다 — `private-docs.md` 규칙의 매칭 프로토콜을 따른다.
5. Story 본문 또는 reference 사이에 **모호함·충돌**이 있으면 **작업을 시작하지 않고 사용자에게 질문**한다.

### Step 2 — 작업 패턴 프롬프트가 동반된 경우

사용자가 별도 프롬프트로 "이 명세에 맞춰 작업 목차 짜줘" 같은 패턴을 지시하면:

- reference + Story 본문 근거로 **번호 매긴 작업 목차**를 제시한다.
- 사용자가 항목을 선택할 때까지(예: "1, 3, 5 ㄱㄱ") **코드를 작성하지 않는다**.
- 선택된 항목만 실행한다. 목차에 없던 작업이 필요해지면 멈추고 추가 항목으로 보고한다.

### Step 3 — 계획

- 변경 범위가 어느 BC × 어느 Layer인지 식별한다 (CLAUDE.md "아키텍처 핵심" 참조).
- BC 간 의존 방향 위반이 있으면 **사용자에게 보고하고 동의 후** 진행한다.
- 새 ErrorCode가 필요한지 확인 — 필요하면 `Common/Exception/ErrorCode/ErrorCode` enum 등록부터 한다.
- Flyway 마이그레이션이 필요하면 새 `V*__*.sql` 파일을 추가한다 (기존 파일 수정 금지).

### Step 4 — 실행

- 4-Layer + Repository Port/Adapter + Command/Query 분리 준수.
- 테스트 작성: `private-docs/test/{bc}.md` 매트릭스를 그대로 따른다.
- Story 본문의 Acceptance Criteria를 항목별로 자체 체크한다.

---

## 4. 브랜치 / 커밋 / Push / PR

브랜치 전략(Epic 단일 브랜치), 커밋 단위·메시지, push 타이밍, PR 본문 자동 생성은 모두 [`pr-commit.md`](./pr-commit.md)가 정의한다.

요약:
- **Epic 1개 = 브랜치 1개**. 이름 형식 `{type}/{NNN}-{epic-slug}` (예: `feat/002-axis-topic-migration`). Epic의 첫 Story 명령 수신 시 develop에서 **로컬 분기만** 자동 생성. 원격 등록은 첫 push 시점에 함께 ([`pr-commit.md`](./pr-commit.md) §6 판단 기반).
- Epic 안의 모든 Story 작업은 **같은 브랜치 위에 누적 커밋**한다 — Story별 sub-branch 만들지 않음.
- 의미 단위마다 즉시 커밋. **Story 종료 시 push는 판단 기반** ([`pr-commit.md`](./pr-commit.md) §6.1 신호) — 권장 시 사용자에게 1줄 보고·확인 후 진행. 매 Story마다 자동 push 하지 않는다. push 실제 실행 시점에 한해 누적 PR 본문 초안 출력.
- PR 생성·머지(`{type}/{NNN}-{slug} → develop`, `develop → main`)는 사용자가 GitHub UI에서 수행.

---

## 5. 금지 사항

- workflow 문서(`workflow/epics/`, `workflow/stories/`, `workflow/product/`)를 Claude Code가 직접 생성·수정·삭제하지 않는다. 입력 문서이지 산출물이 아니다. (Epic 번호 정정·Story 분리 등 사용자 명시 요청은 예외.)
- Story 문서를 읽지 않고 코드를 쓰지 않는다.
- Story가 명시하지 않은 reference 패키지를 임의로 참조하여 Story 범위를 확장하지 않는다 — 필요하면 사용자에게 추가 요청한다.
- 한 Story 안에서 `main` / `develop` 직접 커밋·push, force push, 기존 Flyway V 파일 수정, PR 자동 생성은 금지 (상세는 [`pr-commit.md`](./pr-commit.md) §9).
