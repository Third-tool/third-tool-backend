# [Task · Fix] 정의 — Fix 워크플로우 전체 안내

> 본 문서는 fix 작업의 **수집(brainstorming) → 계획(planning) → 아카이브** 세 단계가 어느 폴더에 어떤 형태로 놓이는지 정리한다.
> fix는 마일스톤(`backlog → ready → in-progress → done`)의 순차 궤도가 **굴러가는 도중** 발생하는 계획 변경·리팩토링·요구사항 변화를 별도 축으로 흡수하는 장치다. 마일스톤을 대체하지 않고 옆에서 조정을 담아 마일스톤 궤도가 계속 굴러가도록 한다.

---

## 1. 두 위치의 역할 분담

fix 작업은 **두 곳**에서 진행된다. 헷갈리지 않도록 역할을 분리한다.

### 1.1 수집 (Brainstorming) — `workflow/task/fix/brainstorming/version/<version>/`

- 목적: 진행 중 발견된 fix 이슈의 **초안·원본 지시·현상·옵션 초기 비교**를 모아두는 곳
- 형식: `issue-<핵심주제>.md`
- 성격: 아직 마일스톤에 접목되지 않은 미확정 아이디어·재검토 초안
- 파일 예시: `issue-deck-axis-integration.md`, `issue-card-axis-recognition.md`, `issue-maxduration-mode-ui.md`
- **버전 규약**: `X.Y.Zv` 형식. 새 버전은 fix 그룹이 완결되어 아카이브로 넘어갈 때 승격 (예: `0.0.1v`, `0.0.2v`)

### 1.2 계획 (Planning) — `workflow/task/pes/workspectrum/<layer>/fix/`

- 목적: 브레인스토밍 이슈를 **실제 fix 계획 문서로 승격**해 마일스톤에 접목하는 곳
- 형식: `fix-<핵심주제>.md`
- 성격: 마일스톤의 어떤 Epic·Story를 어떻게 뒤집을지 명시하는 실행 계획
- 5개 레이어에 각각 `fix/` 폴더가 있고, 레이어 스케일에 맞는 규칙이 그 안의 `fix.md`에 정의되어 있다

**핵심**: 브레인스토밍은 "이런 fix가 필요할 것 같다"의 수집소, 계획은 "이 fix를 이렇게 실행한다"의 실행소.

---

## 2. 전체 흐름도

```
[1] 진행 중 이슈 발견
        │
        ▼
[2] brainstorming/version/<v>/issue-*.md 초안 작성
        │  (원본 지시 · 현상 · 옵션 초기 비교)
        │
        ▼
[3] workspectrum/<layer>/fix/fix-*.md 로 승격
        │  (delta · Fix-Epic/Fix-Story 분할 · 반영 계획)
        │
        ▼
[4] Fix-Story 순차 처리 (마일스톤 궤도와 병행)
        │
        ▼
[5] 원본 SDD / SDD-Lite / PES / Feature-Story / One-Line-Spec 갱신
        │  (진실 소스: 코드 · Flyway · DOMAIN.md · PACKAGE.md · Swagger)
        │
        ▼
[6] 아카이브
    - brainstorming issue: 해당 버전 폴더에 그대로 유지 (이력)
    - workspectrum fix: done/ 또는 version/<v>/ 로 이동
    - task/fix/<layer>/version/<v>/ 아카이브 폴더에 최종본 스냅샷 보관
```

---

## 3. 레이어별 계획 문서 위치

각 workspectrum 레이어의 `fix/fix.md`가 그 레이어의 fix 규칙을 정의한다.

| 레이어 | 스케일 | 계획 폴더 | 규칙 문서 |
| --- | --- | --- | --- |
| sdd | 1~2개월 대형 재설계 | `workflow/task/pes/workspectrum/sdd/fix/` | [`sdd/fix/fix.md`](../pes/workspectrum/sdd/fix/fix.md) |
| sdd-lite | 2~4주 경량 재설계 | `workflow/task/pes/workspectrum/sdd-lite/fix/` | [`sdd-lite/fix/fix.md`](../pes/workspectrum/sdd-lite/fix/fix.md) |
| pes | Product-Epic-Story 실행 | `workflow/task/pes/workspectrum/pes/fix/` | [`pes/fix/fix.md`](../pes/workspectrum/pes/fix/fix.md) |
| feature-story | 1~2일 단일 기능 | `workflow/task/pes/workspectrum/feature-story/fix/` | [`feature-story/fix/fix.md`](../pes/workspectrum/feature-story/fix/fix.md) |
| one-line-spec | 한 줄 명세 정정 | `workflow/task/pes/workspectrum/one-line-spec/fix/` | [`one-line-spec/fix/fix.md`](../pes/workspectrum/one-line-spec/fix/fix.md) |

---

## 4. 기존 `task/fix/<layer>/version/` 아카이브 폴더의 역할

`workflow/task/fix/` 아래에는 이미 각 레이어 이름의 폴더(`sdd/`, `sdd-lite/`, `pes/`, `feature-story/`, `one-line-spec/`, `integration/`)가 존재하며, 각각 `version/<v>/` 하위에 완결된 fix의 최종본 스냅샷을 보관한다.

- **진행 중 fix**: `workflow/task/pes/workspectrum/<layer>/fix/` (workspectrum 계획 폴더)
- **완결된 fix**: `workflow/task/fix/<layer>/version/<v>/` (본 폴더의 아카이브)

즉 workspectrum은 "지금 굴리는 fix", `task/fix/<layer>/version/`은 "끝난 fix의 스냅샷 보관소".

**integration/**: 여러 레이어에 걸친 통합 fix가 발생할 때 사용. 단일 레이어로 매핑되지 않는 경우만.

---

## 5. 버전 규약 (`X.Y.Zv`)

- `X` — 대형 fix 그룹 세대 (예: 큰 방향 전환)
- `Y` — 중간 그룹 (해당 세대 안의 fix 묶음)
- `Z` — 세부 fix 번호 (그룹 안의 순서)
- **승격 시점**: 진행 중 fix 그룹이 완결되어 아카이브로 넘어갈 때 새 버전 폴더 생성

예:
- `0.0.1v` — 첫 fix 그룹 (예: LearningFacade 초기 정합성 정리)
- `0.0.2v` — 두 번째 fix 그룹 (예: Axis↔Deck 통합)

---

## 6. 마일스톤과의 관계 재강조

fix는 마일스톤을 대체하지 않는다.

- **마일스톤**: `backlog → ready → in-progress → done` 순차 진행. 원본 SDD/SDD-Lite/PES/Feature-Story/One-Line-Spec의 궤도.
- **fix**: 그 궤도가 굴러가는 동안 옆에서 튀어나오는 정정을 흡수. 원본을 통째로 재작성하지 않고 delta로만 처리.
- **원본 유지**: 원본은 최종본으로 유지되고, fix는 조정 이력으로만 남는다.

fix가 원본보다 커지거나 원본 전면 재작성이 필요해지면 → 새 Product·Epic·Story를 만들어 마일스톤 궤도로 편입 판단. 이때 fix는 그 판단 근거로만 남는다.

---

## 7. 참고 사례

- 브레인스토밍 초안: `workflow/task/fix/brainstorming/version/0.0.2v/issue-deck-axis-integration.md`
- 승격된 계획: `workflow/task/pes/workspectrum/sdd/fix/fix-axis-deck-full-integration.md`
- 선행 fix 사례: `workflow/task/pes/workspectrum/sdd/fix/fix-deck-axis-visibility.md`
