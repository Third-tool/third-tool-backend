# Skill Catalog (Living)

> **성격**: living-docs — 항상 최신본. Claude Code 환경에서 이 프로젝트 작업 시 **실제로 쓰거나 검토 중인 skill** 인덱스.
> **성장 방향**: skill이 많아지면 카테고리별 파일로 분화 (`tooling/superpowers.md`, `tooling/figma.md` …).
> **관련 topology**: `workflow/topologys/context-engineering/v1-context-engineering.md` (문서·에이전트 흐름 pin).
> **원본**: Claude Code 플러그인 설치 목록 (`~/.claude/plugins/cache/`) + 각 skill의 SKILL.md.
> **사용 흐름**: 새 작업 시작 시 여기서 관련 skill을 먼저 훑고 → 필요한 것을 명시적으로 invoke.

---

## 1. 채택 · 검토 상태 색인

| 상태 | 의미 |
| --- | --- |
| 🟢 상시 | 매 작업마다 자동/거의 자동 사용 |
| 🟡 상황 | 특정 조건에서 명시 invoke |
| ⚪ 검토 | 도입 가치 확인 중, 실전 미투입 |
| 🔴 미사용 | 프로젝트 성격상 매칭 낮음 |

---

## 2. Superpowers 계열 (Skill 오케스트레이션)

| Skill | 상태 | 사용처 |
| --- | --- | --- |
| `superpowers:using-superpowers` | 🟢 상시 | 세션 진입 skill 오케스트레이션 규칙 |
| `superpowers:brainstorming` | 🟢 상시 | 창의 작업·기능 신설·설계 결정 |
| `superpowers:writing-plans` | 🟡 상황 | 다단계 구현 계획 필요할 때 |
| `superpowers:executing-plans` | 🟡 상황 | 별도 세션에서 plan 실행 시 |
| `superpowers:subagent-driven-development` | 🟡 상황 | 병렬 독립 task 실행 시 |
| `superpowers:dispatching-parallel-agents` | 🟡 상황 | 2개 이상 독립 task 동시 실행 |
| `superpowers:test-driven-development` | ⚪ 검토 | 향후 test-first 도입 검토 |
| `superpowers:systematic-debugging` | 🟡 상황 | 버그·test 실패 원인 추적 |
| `superpowers:verification-before-completion` | 🟢 상시 | "완료" 선언 전 검증 강제 |
| `superpowers:requesting-code-review` | 🟡 상황 | Reviewer 세션 (5관점 병렬) 유도 |
| `superpowers:receiving-code-review` | 🟡 상황 | 리뷰 피드백 수용 |
| `superpowers:writing-skills` | ⚪ 검토 | 우리 프로젝트용 커스텀 skill 신설 시 |
| `superpowers:using-git-worktrees` | ⚪ 검토 | 병렬 브랜치 작업 필요할 때 |
| `superpowers:finishing-a-development-branch` | 🟡 상황 | 브랜치 완료·PR 승격 시 |

**메모**: `superpowers:brainstorming`은 이 프로젝트의 4-anchor 회의 흐름에서 계속 반복 사용됨 (workflow/living-docs 셋업, quration 셋업 등).

---

## 3. Codex 계열 (외부 모델 협력)

| Skill | 상태 | 사용처 |
| --- | --- | --- |
| `codex:rescue` | ⚪ 검토 | Claude가 stuck일 때 Codex에 위임 |
| `codex:setup` | ⚪ 검토 | Codex CLI 셋업 |
| `codex:gpt-5-4-prompting` | ⚪ 검토 | 내부 프롬프트 가이드 |
| `codex:codex-result-handling` | ⚪ 검토 | 결과 handoff |
| `codex:codex-cli-runtime` | ⚪ 검토 | 런타임 계약 |

**메모**: 이 프로젝트는 단일 Claude 세션으로 대부분 처리 → Codex는 아직 실전 미투입.

---

## 4. Speckit 계열 (Spec-driven)

| Skill | 상태 | 사용처 |
| --- | --- | --- |
| `speckit-specify` | ⚪ 검토 | feature spec 생성 |
| `speckit-plan` | ⚪ 검토 | plan template 생성 |
| `speckit-tasks` | ⚪ 검토 | tasks.md 생성 |
| `speckit-analyze` | ⚪ 검토 | spec 정합성 분석 |
| `speckit-implement` | ⚪ 검토 | tasks 실행 |
| `speckit-checklist` | ⚪ 검토 | 커스텀 체크리스트 |
| `speckit-clarify` | ⚪ 검토 | 미명세 구간 질문 |
| `speckit-constitution` | ⚪ 검토 | 프로젝트 헌법 |
| `speckit-taskstoissues` | ⚪ 검토 | tasks → GitHub Issues |

**메모**: 이 프로젝트는 자체 workflow 3구간(→ 4-anchor)을 사용 → Speckit는 참고용, 실전 미투입.

---

## 5. Figma 계열 (디자인 연동)

| Skill | 상태 | 사용처 |
| --- | --- | --- |
| `figma:figma-use` | 🔴 미사용 | 백엔드 중심 프로젝트 (프론트 별도 리포) |
| `figma:figma-implement-design` | 🔴 미사용 | 동상 |
| `figma:figma-generate-design` | 🔴 미사용 | 동상 |
| `figma:figma-generate-diagram` | ⚪ 검토 | ERD·아키텍처 다이어그램 시각화 필요 시 |
| `figma:figma-code-connect` | 🔴 미사용 | 프론트 매핑 |
| `figma:figma-generate-library` | 🔴 미사용 | 디자인 시스템 |
| `figma:figma-create-design-system-rules` | 🔴 미사용 | 동상 |
| `figma:figma-use-figjam` | 🔴 미사용 | 동상 |

**메모**: 백엔드 리포이라 대부분 미사용. 아키텍처 다이어그램 시각화 필요할 때만 `figma-generate-diagram` 후보.

---

## 6. 기타 유틸

| Skill | 상태 | 사용처 |
| --- | --- | --- |
| `claude-api` | ⚪ 검토 | 앱 안에서 Claude API 직접 호출 시 (지금 프로젝트는 AI Static Adapter 단계라 미투입) |
| `update-config` | 🟡 상황 | settings.json · hook · 권한 조정 |
| `keybindings-help` | 🟡 상황 | 개인 keybinding 조정 |
| `simplify` | ⚪ 검토 | 변경 코드 리뷰·정리 |
| `fewer-permission-prompts` | 🟡 상황 | 자주 쓰는 read-only 명령 allowlist 확장 |
| `loop` | ⚪ 검토 | 반복 폴링 필요 시 |
| `schedule` | ⚪ 검토 | cron 스타일 원격 에이전트 |
| `init` | 🔴 미사용 | 이미 CLAUDE.md 있음 |
| `review` | 🟡 상황 | PR 리뷰 |
| `security-review` | 🟡 상황 | 보안 리뷰 (인증·인가 변경 시) |

---

## 7. 커스텀 skill 후보 (프로젝트 특화)

이 프로젝트에서 반복 관찰되는 패턴 중 커스텀 skill로 승격할 만한 것 (검토 리스트):

| 후보 | 이유 |
| --- | --- |
| `workflow-4anchor-classifier` | 새 정보가 milestones/living-docs/topologys/quration 중 어디로 갈지 매번 판단 필요 |
| `adr-writer` | ADR 트리거 발생 시 형식·번호 자동화 |
| `flyway-3phase-migration` | 3-phase 마이그레이션 템플릿 (컬럼 추가 → 백필 → NOT NULL) |
| `bc-boundary-checker` | BC 간 import·의존 방향 리뷰 자동화 |
| `errorcode-registrar` | 새 ErrorCode 등록 절차 (enum + 예외 + 문서 반영) |

**메모**: 아직 신설 안 함. superpowers:writing-skills로 만들 수 있는 후보들.

---

## 8. 참조

- Skill 원본: `~/.claude/plugins/cache/{plugin}/{version}/skills/{name}/SKILL.md`
- 세션 진입 오케스트레이션: `superpowers:using-superpowers`
- 관련 topology: `workflow/topologys/context-engineering/v1-context-engineering.md`
- 관련 living-docs: `docs-conventions/` (실전 문서 흐름)

*최신 갱신: 2026-07-03 · 초기 카탈로그. 상태(🟢/🟡/⚪/🔴)는 실전 사용 관찰 반영해 갱신*
