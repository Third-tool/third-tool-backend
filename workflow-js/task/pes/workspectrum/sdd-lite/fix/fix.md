# [SDD-Lite · Fix] 정의 — 경량 SDD 진행 중 계획 변경 흡수

> `workflow/task/pes/workspectrum/sdd-lite/` 아래의 `fix/`는 **경량 SDD 마일스톤이 굴러가는 도중** 발생하는 계획 변경·리팩토링·요구사항 변화를 담는 폴더다.
> 원본 SDD-Lite는 2~4주 규모의 중형 재설계를 다루며, fix는 그 중간에 튀어나온 정책 충돌·범위 조정을 **원본을 통째로 재작성하지 않고** delta로 흡수한다.

---

## 1. 마일스톤 vs fix

| 축 | 마일스톤 (`backlog / ready / in-progress / done`) | fix (`fix/`) |
| --- | --- | --- |
| **진행 방식** | 순서대로 (backlog → ready → in-progress → done) | 마일스톤 진행 도중 필요할 때 삽입 |
| **트리거** | 사전 합의된 SDD-Lite Product 계획 | 진행 중 발견된 정책 충돌·요구사항 변경·리팩토링 필요 |
| **문서 성격** | 계획 단위 (앞으로 뭘 만들지) | 조정 단위 (원본 계획의 어느 결정을 뒤집을지) |
| **원본과의 관계** | 원본 그 자체 | 원본을 참조하고 **결정 1~2개를 뒤집음** |
| **폐기 여부** | done으로 이동 후 진실 소스 인용 | 원본에 반영 완료 시 아카이브 (`done/` 또는 `version/`) |

---

## 2. SDD-Lite 레이어에서 fix가 필요한 순간

SDD-Lite는 SDD 풀버전보다 작지만 여전히 여러 Story를 묶어 진행하는 스케일이라 진행 중 다음 시나리오가 나온다.

- **Product 내 정책 1~2개가 실사용/테스트에서 무너짐** — 원본 결정을 부분 폐기하고 대체안 삽입
- **원본에서 예측 못 한 마이그레이션 필요 발생** — Flyway·API 스키마 조정을 Fix-Story로 분리
- **후행 Story가 선행 Story 결과에 의존하는데 실제 산출이 어긋남** — 순서 재정렬 + 인터페이스 재정의
- **SDD 풀버전으로 승격이 필요한 규모 확장 발견** — fix에서 승격 판단만 기록하고, 실제 재작성은 sdd/ 레이어로 이관

**SDD 풀버전과의 차이**: SDD-Lite fix는 원본이 짧으므로 delta 서술도 짧다. **원본 결정 1~2개** + **신규 결정 1~2개**로 압축.

---

## 3. 파일 명명 규칙

```
fix-<핵심주제>.md              # 진행 중 fix (본 폴더 직속)
version/<X.Y.Zv>/fix-*.md     # 완료된 fix 스냅샷 (선택)
```

- `<핵심주제>`는 kebab-case, 원본 SDD-Lite Product 이름과 겹치지 않는 delta 키워드로
  - 예: `fix-onboarding-shortcut-drop.md` (원본은 온보딩 SDD-Lite, fix는 단축 경로 정책 폐기)

---

## 4. 필수 섹션 (SDD-Lite Fix 문서 골격)

```
# [Fix · SDD-Lite] {제목} ({YYYY-MM-DD})

## 0. 메타
- 원본 SDD-Lite: {경로}
- 선행 fix / 후행 fix: {경로 or 없음}
- 본 fix 발견 시점 / 작성자
- 영향 받는 Epic / Story
- 연계 브레인스토밍: {task/fix/brainstorming/version/<v>/issue-*.md 경로}

## 1. 배경 — 왜 원본을 바꾸나
- 현재 상태 (원본 SDD-Lite + 선행 fix까지 반영된 흐름)
- 발견된 문제 (재현 시나리오 1~2개)
- 왜 지금 뒤집어야 하는가

## 2. 원본 대비 delta (뒤집는 지점)
- 폐기 (Deprecate): 원본의 어떤 결정을 버리는가
- 신설 (Introduce): 무엇으로 대체하는가
- 유지 (Keep): 뒤집지 않는 결정을 명시 (범위 오해 방지)

## 3. Fix-Story 분할
- 원본 Epic·Story에 어떻게 접목되는가
- 신규 Fix-Story가 필요하면 명시

## 4. 승격 판단
- SDD 풀버전으로 승격 필요 여부 (yes/no + 판단 근거 1~2줄)

## 5. 진실 소스 반영 계획
- 코드/Flyway/DOMAIN.md/PACKAGE.md/Swagger 어디에 어떻게 반영
```

---

## 5. fix의 수명

```
발견/작성 → in-progress → 원본 SDD-Lite 결정 갱신 반영 → 아카이브(done/ or version/<v>/)
```

- fix가 원본 SDD-Lite를 대체하지 않는다. 원본은 원본대로 최종본으로 유지되고, fix는 조정 이력.
- 사이즈가 커지면 SDD 풀버전으로 승격 판단.

---

## 6. fix를 만들지 말아야 할 경우

- 원본 SDD-Lite의 오탈자·문법 정정 → 원본 직접 수정
- 원본 결정을 뒤집지 않는 단순 리팩토링 → Story 하나로 처리, fix 불요
- 아직 확정되지 않은 아이디어 → `workflow/task/fix/brainstorming/version/<v>/issue-*.md`에 두고 결정 후 승격
