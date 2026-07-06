# Pinned Topology — `context-engineering` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님 (실제 파일 위치·명명은 여기 없음)

**목적**: Claude Code(및 유사 에이전트)가 이 프로젝트에 진입했을 때 **최소 컨텍스트로 최대 판단**을 내릴 수 있도록 문서 배치·명명·참조 흐름을 고정한다.

---

## 0. 유효 구간 (Validity)

| 항목 | 값 |
| --- | --- |
| Pinned at | 2026-07-03 |
| Valid for | 본 워크플로우 v1 구간 — re-pin trigger 발생 전까지 |
| Owner | 메인 Claude + 사용자 합의 |
| Re-pin trigger | 아래 §4 |

---

## 1. How Claude Code must use this file — 행동 계약

1. **새 문서 신설 · 문서 위치 이동 · 명명 변경 시 이 파일을 먼저 읽는다.** §2의 원칙은 본 구간 **고정 제약**.
2. **문서는 4-anchor 규칙(milestones · living-docs · topologys · quration)에 배치.** 소속 불명확 시 상위 회의.
3. **CLAUDE.md는 단일 진입 포인터.** 상세는 룰·문서로 링크. CLAUDE.md에 내용을 붓지 않는다.
4. **원본과 인덱스를 분리.** 코드가 진실 소스인 영역은 별도 docs 만들지 않음 (Flyway·Controller·enum 등).
5. **문서 성장 정책 준수.** living-docs는 폴더당 1파일 시작 → 역할별 자연 분화.
6. **참조 링크는 상대경로.** repo root 기준 절대경로 지양 (workflow/ 이동 시 취약).

---

## 2. The pinned topology

### Nodes
- `단일 진입 포인터` — CLAUDE.md
- `4-anchor 구간` — `milestones/version/{Nv}/` · `living-docs/` · `topologys/` · `quration/`
- `규범 문서` — `docs/DOMAIN.md`, `docs/PACKAGE.md`, `docs/adr/*`
- `개인 룰` — `.claude/rules/*.md` (workflow · conventions · review · adr · pr-commit · git · Domain ownership)
- `메모리` — `~/.claude/projects/{project}/memory/` (개인, 세션 간 유지)
- `plan file` — `~/.claude/plans/*.md` (계획 mode 스크래치패드)
- `코드 진실 소스` — 코드·Flyway·테스트가 있는 영역 (별도 docs 만들지 않음)
- `Front matter` — 각 living-docs 파일 상단의 목적·성격 3~5줄 헤더
- `상호 참조 링크` — `[[name]]` 또는 상대경로 markdown 링크

### Edges
- Claude 진입 → `단일 진입 포인터` → 상세 룰·문서
- 새 결정 → `docs/adr/` → `docs/adr/index.md` 갱신
- 새 자산 인덱스 → `living-docs/{topic}/` → `living-docs/README.md` 반영
- 새 수평 규칙 → `topologys/{topic}/v1-{topic}.md` → `references/toplogy.md`가 방법론 소스
- 새 박제 결과 → `milestones/version/{Nv}/` → 다음 milestone에 지속되면 topology 재pin 여부 검토
- 코드 변경 → `코드 진실 소스` 영역이면 docs 갱신 묻지 않음 (workflow.md Step 5 규칙)
- 문서 진입 → 상단 `Front matter` (성격·시간축·성장 방향)로 3초 판단

### Boundaries
- **CLAUDE.md 크기 경계**: 진입 포인터 성격 유지. 200줄 초과 시 룰 파일로 분리 후 링크.
- **코드가 진실 소스인 영역 경계**: API 명세 · DB 스키마 · 테스트 매트릭스 · 패키지 트리 · 인프라 사실 — **별도 docs 만들지 않음**.
- **living-docs 성장 경계**: 폴더당 초기 1파일. 커지면 역할·서브도메인별 자연 분화. 버전 폴더 X.
- **topology 명명 경계**: `{topic}/v{n}-{topic}.md` 형식 고정. 폴더명 == 파일명 prefix.
- **참조 경계**: 원본은 창고에 그대로. 인덱스·발췌·서사만 다른 anchor로.

### Invariants
- CLAUDE.md는 단일 진입 포인터 성격 유지 (룰·docs 위치 표만 있고 상세 없음)
  - 감지법: CLAUDE.md에 100줄 초과 상세 서사 등장 시 리뷰
- living-docs 각 폴더는 최소 1개 `.md` 파일 또는 명시적 "빈 상태 유지" 목적 (README에 기재)
  - 감지법: 빈 폴더 목록과 README 대조
- topology 파일은 `{topic}/v1-{topic}.md` 명명 100%
  - 감지법: `topologys/*/v*-*.md` glob vs 폴더명 매칭
- 상호 참조 링크는 상대경로 사용 (`../` · `./`), 절대경로 사용 0건
  - 감지법: `C:/` 또는 `/Users/` grep
- 각 living-docs 파일 상단에 성격·시간축·성장 방향 명시된 3~5줄 blockquote 헤더 존재
  - 감지법: 각 파일 첫 5줄 grep `> `
- 새 ADR 등록 시 `docs/adr/index.md` 표에 반영 100%
  - 감지법: ADR 파일 vs index.md diff

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 파일 위치 · 명명 · 상호 참조 관계 → `living-docs/docs-conventions/` (실전 예시·현재 지도)
- 각 룰 파일 상세 → `.claude/rules/*.md`
- 4-anchor 역할 경계 상세 → `workflow/living-docs/README.md` + `workflow/quration/README.md`
- CLAUDE.md 진입 포인터 표 → `CLAUDE.md` 자체
- 왜 이렇게 박혔는지 → 사용자 회의 이력, memory 파일

---

## 4. Re-pin trigger

- CLAUDE.md가 단일 진입 포인터를 벗어나 자체 서사로 성장하기 시작
- 4-anchor 외 5번째 anchor 도입 (예: 별도 `research/`, `experiments/` 등)
- 규범 문서 위치를 `docs/`에서 다른 곳으로 이관
- 개인 룰(.claude/rules/)을 팀 공유로 승격 (개인 → 팀 boundary 이동)
- 자동 인덱싱 도구 도입 (예: 의미 검색 벡터 DB로 문서 참조 방식 변경)
- 다국어 문서 도입 (한국어·영어 병행 관리 정책)
