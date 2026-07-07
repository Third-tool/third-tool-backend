# [Feature-Story · Fix] 정의 — 단일 기능 Story 진행 중 계획 변경 흡수

> `workflow/task/pes/workspectrum/feature-story/` 아래의 `fix/`는 **단일 기능 Story 마일스톤이 굴러가는 도중** 발생하는 정의 변경·리팩토링·수용 기준 조정을 담는 폴더다.
> Feature-Story는 1~2일 규모의 얇은 실행 단위라, fix도 원본 Story의 delta만 짧게 잡아두는 형식이다.

---

## 1. 마일스톤 vs fix

| 축 | 마일스톤 (`backlog / ready / in-progress / done`) | fix (`fix/`) |
| --- | --- | --- |
| **진행 방식** | 원본 Story 순서대로 실행 | Story 진행 도중 정의가 어긋날 때 삽입 |
| **트리거** | 사전 합의된 Feature-Story 정의 | 진행 중 AC 재정의·인터페이스 변경·의존 기능 삽입 필요 |
| **문서 성격** | 실행 단위 (한 기능의 명세) | 정정 단위 (원본 명세를 어떻게 바꿀지) |
| **원본과의 관계** | 원본 그 자체 | 원본 Story를 참조하고 **AC/입출력을 부분 재정의** |
| **폐기 여부** | done으로 이동 | 원본 갱신 완료 시 아카이브 (`done/` 또는 `version/`) |

---

## 2. Feature-Story 레이어에서 fix가 필요한 순간

- **수용 기준(AC)이 실사용/디자인 검토에서 부정확** — AC 1~2개 재정의
- **인접 기능의 결과가 본 Story에 영향을 줌** — 선행 fix 삽입
- **UX/문구/enum 값 변경 요청** — 원본 Story의 필드·문구 정의 갱신
- **테스트 픽스처·매트릭스 재조정 필요** — 픽스처 변경을 Fix-Story로 분리

Feature-Story는 얇으므로 fix도 얇게. 문서 5~15줄이면 충분하다.

---

## 3. 파일 명명 규칙

```
fix-<핵심주제>.md              # 진행 중 fix
version/<X.Y.Zv>/fix-*.md     # 완료된 fix 스냅샷
```

- 예: `fix-card-tag-multi-select.md`, `fix-deck-progress-percent-rounding.md`

---

## 4. 필수 섹션 (Feature-Story Fix 문서 골격 — 얇은 버전)

```
# [Fix · Feature-Story] {제목} ({YYYY-MM-DD})

## 0. 메타
- 원본 Feature-Story: {경로}
- 선행 fix / 후행 fix: {경로 or 없음}
- 본 fix 발견 시점 / 작성자
- 연계 브레인스토밍: {task/fix/brainstorming/version/<v>/issue-*.md 경로 or 없음}

## 1. 배경 (2~4줄)
- 원본 Story의 어느 정의가 어긋났는가
- 발견 계기

## 2. delta
- Before: 원본 Story의 정의 요약 1~3줄
- After: 새 정의 1~3줄
- 유지되는 부분 (오해 방지)

## 3. 반영 계획
- 원본 Story 문서 갱신 지점
- 코드/테스트 조정 지점
```

---

## 5. fix의 수명

```
발견/작성 → in-progress → 원본 Story 정의 갱신 → 아카이브
```

- Fix-Story도 원본과 동일한 규칙(1 PR)으로 처리.
- 얇은 fix는 원본 Story에 인라인 갱신 후 fix 문서를 짧게 아카이브.

---

## 6. fix를 만들지 말아야 할 경우

- 원본 Story의 오탈자 → 원본 직접 수정
- AC를 뒤집지 않는 구현 세부 변경 → fix 불요
- Story 자체가 부적절해 통째로 재작성이 필요하면 → fix가 아니라 원본 Story를 폐기하고 새 Feature-Story 생성 판단
