# [PES · Fix] 정의 — Product-Epic-Story 진행 중 계획 변경 흡수

> `workflow/task/pes/workspectrum/pes/` 아래의 `fix/`는 **Product-Epic-Story 마일스톤이 굴러가는 도중** 발생하는 계획 변경·리팩토링·요구사항 변화를 담는 폴더다.
> PES는 실제 실행 단위(Story = 1 PR)까지 내려간 계획이라, fix는 특정 Epic·Story 조합의 재정렬·삽입·제거를 명확한 diff로 기록한다.

---

## 1. 마일스톤 vs fix

| 축 | 마일스톤 (`backlog / ready / in-progress / done`) | fix (`fix/`) |
| --- | --- | --- |
| **진행 방식** | 순서대로 Product → Epic → Story 실행 | 마일스톤 진행 도중 필요할 때 삽입 |
| **트리거** | 사전 합의된 PES Product·Epic·Story 계획 | 진행 중 발견된 Story 의존성 오류·요구사항 변경·인터페이스 정정 |
| **문서 성격** | 실행 단위 (누가·언제·무엇을) | 정정 단위 (기존 Epic·Story 순서·범위·정의를 어떻게 뒤집을지) |
| **원본과의 관계** | 원본 그 자체 | 원본을 참조하고 **Epic·Story를 재정렬·삽입·제거** |
| **폐기 여부** | done으로 이동 후 진실 소스 인용 | 원본 PES에 반영 완료 시 아카이브 (`done/` 또는 `version/`) |

---

## 2. PES 레이어에서 fix가 필요한 순간

PES는 Story 단위까지 쪼갠 실행 계획이라 실제 착수 후 다음 시나리오가 흔히 나온다.

- **Story 간 의존성이 원본과 어긋남** — Story 순서 재정렬, 또는 새 선행 Story 삽입 필요
- **Epic 스코프가 실사용에서 부족/과함으로 드러남** — Epic 범위를 잘라내거나 확장
- **Story 정의(입출력·API 스펙)가 부정확** — Story 재정의 (원본 Story는 폐기하고 Fix-Story로 대체)
- **다른 Product/Epic의 결과가 본 Product에 영향을 줌** — Epic 삽입 또는 순서 조정
- **PR 리뷰에서 재작업 요구가 반복** — Fix-Story로 별도 트래킹

---

## 3. 파일 명명 규칙

```
fix-<핵심주제>.md              # 진행 중 fix (본 폴더 직속)
version/<X.Y.Zv>/fix-*.md     # 완료된 fix 스냅샷
```

- `<핵심주제>`는 kebab-case, 뒤집는 Epic·Story의 delta 키워드로
  - 예: `fix-epic3-story-reorder.md`, `fix-payment-story-005-idempotency.md`

---

## 4. 필수 섹션 (PES Fix 문서 골격)

```
# [Fix · PES] {제목} ({YYYY-MM-DD})

## 0. 메타
- 원본 PES: {경로 — Product 문서}
- 선행 fix / 후행 fix: {경로 or 없음}
- 본 fix 발견 시점 / 작성자
- 영향 받는 Epic / Story: 원본 대비 어떻게 바뀌는지 요약
- 연계 브레인스토밍: {task/fix/brainstorming/version/<v>/issue-*.md 경로}

## 1. 배경 — 왜 원본을 바꾸나
- 현재 상태 (원본 + 선행 fix까지 반영된 흐름)
- 발견된 문제
- 왜 지금 뒤집어야 하는가

## 2. 원본 대비 delta (Epic·Story 재정렬 표)
- 재정렬 표 (원본 순서 → 새 순서)
- 폐기(Deprecate) Story: 사유
- 신설(Introduce) Fix-Story: 정의 + 수용 기준(AC)
- 재정의(Redefine) Story: before / after
- 유지(Keep) Story: 명시 (범위 오해 방지)

## 3. 새 실행 순서 (To-Be)
- Epic·Story 재정렬 후의 최종 순서
- 브랜치 명명·PR 분할 계획

## 4. 진실 소스 반영 계획
- 코드/Flyway/DOMAIN.md/PACKAGE.md/Swagger 어디에 어떻게 반영
- 원본 PES 문서에 어떤 갱신을 반영할지 (Epic·Story 체크박스 재조정 포함)
```

---

## 5. fix의 수명

```
발견/작성 → in-progress → Fix-Story들 순차 처리 → 원본 PES 갱신 반영 → 아카이브
```

- Fix-Story는 원본 PES의 Story와 동일한 규칙(=1 PR)으로 처리한다.
- 원본 PES 문서의 Epic·Story 목록은 fix 반영 시 그대로 갱신 (원본이 최종본 유지).

---

## 6. fix를 만들지 말아야 할 경우

- 원본 PES의 오탈자·문법 정정 → 원본 직접 수정
- Story 정의는 그대로 두고 구현 방식만 바꾸는 경우 → fix 불요, 그냥 Story 진행
- 아직 확정되지 않은 재정렬 아이디어 → `workflow/task/fix/brainstorming/version/<v>/issue-*.md`에 두고 결정 후 승격
