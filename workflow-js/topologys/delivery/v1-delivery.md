# Pinned Topology — `delivery` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님 (브랜치 명령·gh 명령 등 실제 절차는 별도 룰)
> - vocabulary 아님 (실제 브랜치명·실제 PR 번호·실제 커밋 해시는 여기 없다)

---

## 0. 유효 구간 (Validity)

| 항목 | 값 |
| --- | --- |
| Pinned at | 2026-06-15 |
| Valid for | 본 워크플로우 v1 구간 — re-pin trigger 발생 전까지 |
| Owner | 메인 Claude + 사용자 합의 |
| Re-pin trigger | 아래 §4 |

---

## 1. How Claude Code must use this file — 행동 계약

1. **브랜치 생성·커밋·PR·머지 관련 작업 시 이 파일을 먼저 읽는다.** §2의 단위는 본 구간 동안 **고정 제약**.
2. **이 topology 안에서 실제 브랜치명·커밋 메시지·PR 본문 어휘만 채운다.** 1 Epic ↔ 1 브랜치 매핑·머지 주체·push 게이트는 건드리지 않는다.
3. **하나의 브랜치 위에 새 Epic을 얹어야 할 것 같으면 STOP하고 보고한다.** 그건 경계 위반이다.
4. **자동 머지 / 사용자 의사결정 우회 push 정황이 보이면 보고**한다.
5. **이 파일에 실제 브랜치명·커밋 메시지를 적지 않는다.** 그건 §3 포인터로.

---

## 2. The pinned topology

### Nodes
- `Epic` — 응집된 1개의 가치 묶음 (작업의 1차 단위)
- `Story` — Epic 내부의 의미 단위 작업
- `브랜치` — Epic 1개에 대응하는 작업 격리 단위
- `커밋` — Story 의미 단위로 누적되는 변경 단위
- `PR` — `브랜치` 단위로 생성되는 통합 제안
- `Reviewer 게이트` — push 직전 5관점 reviewer 통과 (참조: feedback topology)
- `사용자 의사결정 게이트` — 종합 보고 후 사용자 수용/보강 결정
- `머지 게이트` — PR 통과 후 사람이 머지하는 지점
- `ADR 커밋` — 아키텍처 결정 기록의 별도 커밋 트랙

### Edges
- `Epic` → `브랜치` : 1:1 (1 Epic당 1 브랜치)
- `Story` → `커밋` : 1:N (Story 단위로 의미 단위 누적)
- `커밋` → `브랜치` : 누적
- `브랜치` → `PR` : 1:1
- `Story 작업 완료` → `Reviewer 게이트` → `사용자 의사결정 게이트` → push → `PR` 갱신
- `PR` → `머지 게이트` : 통과 후 사람 머지
- `ADR 트리거` → `ADR 커밋` : Step 1~5 흐름과 독립, 작업 중 즉시 별도 커밋

### Boundaries
- **브랜치 경계**: 1 Epic당 1 브랜치. 브랜치 위에 다른 Epic을 얹지 않는다
- **커밋 의미 경계**: 커밋 1개는 Story 1개의 의미 단위. 무의미한 변경 묶음 금지
- **push 경계**: `Reviewer 게이트` 통과 전 push 금지
- **머지 경계**: 머지는 사람이 수행. 자동/Claude가 머지 명령 실행하지 않는다
- **ADR 경계**: ADR은 별도 커밋(`docs(adr): ...`). 다른 변경과 섞지 않는다

### Invariants
- 모든 커밋은 Conventional Commits 형식을 따른다
  - 감지법: commit-msg hook 또는 PR 리뷰
- 모든 커밋 메시지에 Story 메타데이터(예: `[Story-N-M]`)가 포함된다
  - 감지법: 커밋 메시지 검사
- `Reviewer 게이트` 통과 전 push된 사례 0건
  - 감지법: 작업 회고 / 흐름 점검
- Claude가 직접 머지를 실행한 사례 0건
  - 감지법: 머지 author 검사
- PR 본문은 What / Why / How / Tradeoff / Reviewer 종합 / Test / Checklist 섹션을 가진다
  - 감지법: PR 템플릿 + 리뷰

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 브랜치 명명 규칙 · `<type>/<NNN>-<short-kebab>` 형식 → `.claude/rules/pr-commit.md`
- 실제 커밋 메시지 형식 · scope 분류 → `CLAUDE.md` 의 Commit 규칙 + `.claude/rules/pr-commit.md`
- 실제 PR 본문 템플릿 · 라벨 규칙 → `CLAUDE.md` 의 PR 작성 규칙
- 실제 push 판단 신호 · push 절차 → `.claude/rules/pr-commit.md` §6
- 실제 Reviewer 게이트 절차 → `.claude/rules/review.md` + feedback topology
- 실제 ADR 작성 트리거 → `.claude/rules/adr.md`
- 작업 흐름 전체 → `.claude/rules/workflow.md`

---

## 4. Re-pin trigger

- 1 Epic ↔ 1 브랜치 매핑 변경 (multi-Epic 브랜치 또는 sub-branch 도입)
- 커밋 단위 정책 변경 (Story → 더 큰 단위로 묶기 등)
- `Reviewer 게이트` 폐지 또는 push 후로 이관
- 머지 주체 변경 (사람 → 자동/봇)
- ADR 커밋 정책 변경 (별도 트랙 → 통합 트랙)
- Conventional Commits 폐지 또는 다른 메시지 표준 도입
