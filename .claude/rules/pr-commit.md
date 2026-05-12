# Rule: 브랜치 / 커밋 / Push / PR 문서 규칙

> Epic 1개의 시작(브랜치 생성)부터 끝(머지)까지 전 과정을 정의한다.
> **Epic 1개 = 브랜치 1개.** Epic 안의 모든 Story 작업은 같은 브랜치 위에서 누적된다 — Story별 브랜치는 만들지 않는다.
> **PR 생성·머지는 사용자 영역.** Claude Code는 push까지 수행하고 PR 본문 초안을 텍스트로 제공한다.

---

## 1. 브랜치 전략

### 1.1 영구 브랜치

| 브랜치 | 역할 | 직접 커밋 |
| --- | --- | --- |
| `main` | 프로덕션 배포 브랜치 | 금지 |
| `develop` | 통합 브랜치. Epic이 머지되어 누적되는 곳 | 금지 |

배포 흐름: `{type}/{NNN}-{slug} → develop → main`.

### 1.2 작업 브랜치 (Epic 1개 = 브랜치 1개)

```
main
 └── develop
      └── {type}/{NNN}-{epic-slug}     ← Epic 단위 단일 작업 브랜치
                                          (Epic 안의 모든 Story가 이 위에서 작업됨)
```

### 1.3 Epic 브랜치 이름 규칙

```
{type}/{NNN}-{epic-slug}
```

- **`{type}`**: Epic의 성격에 맞는 prefix. 커밋 type 어휘와 동일하게 사용.

  | type | Epic 성격 |
  | --- | --- |
  | `feat` | 새 기능·도메인 행위 도입 (대부분의 Epic이 이에 해당) |
  | `refac` | 리팩토링 — 동작 변경 없는 구조 변경 |
  | `fix` | 버그 수정 위주 Epic |
  | `docs` | 문서·명세·ADR 정비 위주 Epic |
  | `chore` | 빌드·의존성·인프라 정비 위주 Epic |
  | `perf` | 성능 최적화 위주 Epic |
  | `test` | 테스트 보강·전략 변경 위주 Epic |

- **`{NNN}`**: Epic 번호 3자리 zero-pad. `workflow/epics/Epic-{NNN}.md` 파일명에서 추출.
- **`{epic-slug}`**: kebab-case Epic 슬러그. Epic 문서 제목에서 도출.

### 1.4 예시

| Epic 성격 | 브랜치 이름 |
| --- | --- |
| `Epic-002.md`: AxisAction → AxisTopic 도메인 전환 | `feat/002-axis-topic-migration` |
| `Epic-003.md`: LearningFacade 인프라 레이어 정리 | `refac/003-learning-facade-cleanup` |
| `Epic-004.md`: API 명세 v2 전면 개정 | `docs/004-api-spec-v2` |
| `Epic-005.md`: ReviewSession 응답 지연 개선 | `perf/005-review-session-latency` |

### 1.5 자동 분기 절차 (Epic의 첫 Story 명령 수신 시)

```
1. 현재 Epic 식별 (workflow/epics/Epic-{NNN}.md 파일명 + 본문).
2. Epic 본문에서 작업 성격 추정 → type prefix 결정.
   - 추정이 모호하면 사용자에게 한 번 확인. 기본값은 feat.
3. {type}/{NNN}-{epic-slug} 브랜치가 로컬·원격에 존재하는지 확인.
   - 존재 (이미 진행 중인 Epic) → checkout만 한다.
   - 미존재 → develop fetch 후 develop 최신 위에서 **로컬 분기만** 한다 (원격 등록 `git push -u origin`은 §6 판단 기반 첫 push 시점에 함께 실행).
4. 그 브랜치에서 Story 작업을 시작한다 (Story별 분기 없이 동일 브랜치에 커밋 누적).
```

### 1.6 Epic 브랜치 생명주기

- **시작**: Epic의 첫 Story 명령 수신 시 **로컬 브랜치만** 자동 생성. 원격 등록(`git push -u origin`)은 §6 판단 기반 첫 push 시점에 함께.
- **진행**: Epic의 Story 1, 2, 3, ... 모두 같은 브랜치에 의미 단위 커밋으로 누적.
- **각 Story 종료**: §6.1 신호로 push 판단. push 권장 시 사용자에게 1줄 보고·확인 후 진행하며, push 실제 실행 시점에 한해 PR 본문 초안을 누적 모드로 갱신해 출력. 신호 약하면 push 없이 다음 Story로 넘어간다.
- **Epic 종료**: 사용자가 GitHub UI에서 PR 머지 (`{type}/{NNN}-{slug} → develop`).
- 머지 후 브랜치는 사용자가 GitHub에서 정리 (Claude Code는 브랜치 삭제에 관여하지 않음).

---

## 2. 커밋 단위 (의미 단위로 즉시 커밋)

코드 변경이 의미 단위로 일단락될 때마다 **즉시** 커밋한다. Story 끝에 몰아 커밋하지 않는다.

**의미 단위 = "이 변경분만으로 컴파일과 의미가 통하는가?"**

| 의미 단위 | 1 커밋 |
| --- | --- |
| 도메인 엔티티 / Aggregate / VO 추가·수정 | 1 |
| Domain Service 추가 | 1 (호출 측 변경과 분리) |
| Repository Port + Adapter (JPA + QueryDSL) | 1 |
| Application Service (Command/Query 묶음 또는 분리) | 1 |
| Controller + Request/Response DTO | 1 |
| 단위 테스트 | 1 |
| Slice / 통합 테스트 | 1 |
| Flyway 마이그레이션 1건 | 1 |
| ErrorCode 신규 등록 | 도메인/서비스 변경에 합류 가능, 단독이면 분리 |
| ADR 추가 | **반드시 별도 커밋**, scope = `adr` ([`update-docs/adr.md`](./update-docs/adr.md) §5 참조) |

**한 커밋에 묶지 않을 것**:
- 서로 다른 BC의 변경.
- 도메인 모델 변경 + Flyway 마이그레이션 — 같은 Story라도 분리.
- 리팩토링(동작 변경 없음) + 새 기능 — 분리.
- 무관한 import 정리·포맷 — `chore` scope로 분리.

---

## 3. 커밋 시점

다음을 모두 만족할 때 즉시 커밋한다.

- **컴파일 통과** — `./gradlew compileJava`가 깨지지 않는다.
- **의미 단위 완결** — 반쪽짜리 변경(시그니처만 바꾸고 호출 측 미수정 등) 금지.
- **테스트 동반 변경**이면 같은 커밋 또는 직후 커밋에 테스트 포함.

**커밋 직전 검증** (자동 수행):
1. `git status`로 staged/unstaged 확인.
2. 의도하지 않은 파일이 포함되지 않았는지 검토 (`.env`, `application-*.yml` 비밀, IDE 설정).
3. 변경 파일을 명시적으로 add (`git add -A` / `git add .` 자제).

---

## 4. 메시지 형식

```
<type>(<scope>): <subject> [Story-{NNN}-{N}]
```

> Epic 1개에 여러 Story가 같은 브랜치에 누적되므로, 각 커밋이 어느 Story에 속하는지 식별자(`[Story-{NNN}-{N}]`)로 표시한다. PR 본문 "해결 방법" 섹션이 이 식별자로 Story별 그룹핑을 한다.

### 4.1 type
| type | 용도 |
| --- | --- |
| `feat` | 새 기능 (도메인 행위, 새 API, 새 검증 규칙) |
| `fix` | 버그 수정 |
| `refactor` | 동작 변경 없는 구조 변경 |
| `test` | 테스트 추가·수정만 |
| `docs` | ADR 추가, 코드 주석 변경 (private-docs 외부 관리 영역은 수정하지 않음) |
| `chore` | 빌드 스크립트, 의존성, 포맷, import 정리 |
| `perf` | 성능 최적화 (동작 동등) |

브랜치 prefix와 커밋 type은 **독립**이다 (`feat/002-...` 브랜치 안에서 `refactor`, `test`, `docs` 커밋이 자유롭게 발생).

### 4.2 scope
BC 이름 소문자: `card` / `deck` / `learningfacade` / `review` / `user` / `userschedule` / `common` / `infra`. ADR 커밋만 예외로 `adr`.

여러 BC 동시 수정은 BC 단위로 커밋을 분리하는 것이 원칙.

### 4.3 subject
- 50자 이내, 마침표 없음.
- 한국어/영어 자유. 명령형 동사로 시작 ("추가", "수정", "도입", "분리", "전환").

### 4.4 [Story-{NNN}-{N}]
- 현재 Story 식별자 (예: `Story-002-2`). Epic 번호도 zero-pad.
- subject 끝, 대괄호 안. 빠뜨리지 않는다.

### 4.5 예시

```
feat(learningfacade): AxisTopic 엔티티 추가 [Story-002-2]
feat(learningfacade): addTopics 다건 추가 행위 도입 [Story-002-2]
test(learningfacade): AxisTopic reorder 정합성 통합 테스트 [Story-002-2]
refactor(learningfacade): 인프라 레이어 패키지 정리 [Story-002-3]
docs(adr): ADR005 비동기 이벤트 발행 결정 [Story-002-3]
```

---

## 5. 커밋 본문 (선택)

다음 중 하나에 해당할 때만 본문을 추가한다.

- subject 50자에 변경 이유가 담기지 않을 때.
- private-docs 또는 ADR을 근거로 한 결정 — 인용 출처를 적는다.
- BC 간 의존 위반을 사용자 동의 후 도입한 경우 — 동의 사실과 사유.
- Breaking change 또는 마이그레이션 영향 — 영향 범위와 롤백 경로.

본문은 subject 다음 빈 줄을 두고 시작. 한 줄 72자 권장. heredoc으로 전달.

```
feat(learningfacade): AxisTopic 명사구 표현으로 전환 [Story-002-0]

근거: domain/learning-facade.md §AxisTopic, ADR004
구 AxisAction의 단일 동사 강제 검증을 폐지하고 name(필수) +
description(선택) 구조로 재정의.
```

**Co-Authored-By trailer**: 사용자 명시 요청 없는 한 추가하지 않는다.

---

## 6. Push 타이밍 (판단 기반)

Push는 **자동·강제 행위가 아니다.** Story가 끝났다고 해서 무조건 push하지 않는다. 의미 있는 누적이 만들어졌거나 push가 필요해 보이는 순간에 **사용자에게 한 줄 보고하고 확인 후 진행**한다.

### 6.1 Push 권장 판단 신호

다음 중 하나 이상이 충족되면 **push 권장**으로 판단하고 사용자에게 1줄 보고 + 확인 요청.

- Story가 1개 이상 완료됐고 **여러 의미 단위 커밋이 누적**되었다 (통상 3개 이상).
- 새 도메인·API·스키마 변경처럼 **외부 가시성이 큰 변경**이 포함됐다.
- 마지막 push 이후 **상당한 분량**이 누적됐다 (여러 Story 분량 또는 큰 단일 Story).
- Epic의 **마지막 Story가 끝났다** (Epic 종료 직전 — PR 머지 준비).
- 사용자가 곧 **컨텍스트 전환·중단**을 알린다 (백업 목적).
- 위험성·되돌리기 어려움이 있는 다음 작업 진입 직전 (안전 백업).

신호가 명확하지 않으면 push를 **거론하지 않고** 다음 Story 작업으로 넘어간다. 매 Story마다 push 권유를 반복하지 않는다.

### 6.2 Push 확인 절차

1. 판단 후 사용자에게 한 줄 보고. 예:
   - `Story-002-2 완료, 누적 4커밋. 지금 push할까요? (이유: 외부 가시성 큰 API 추가 포함)`
   - `Epic-002 마지막 Story 완료. 머지 직전 push 권장합니다.`
2. 사용자 승인 → push 실행.
   - **원격 브랜치가 없으면 (첫 push)**: `git push -u origin {epic-branch}` (§1.5에서 미루어 둔 원격 등록을 이 시점에 함께 수행).
   - **이후 push**: `git push`.
3. 사용자가 "나중에"라고 하면 그대로 보류. **다음 push 후보 신호가 새로 충족되기 전까지 push를 다시 거론하지 않는다.** (반복 권유 금지)
4. 사용자가 명시적으로 "지금 push해줘"라고 요청하면 신호 평가 없이 즉시 진행 (Story 중간이어도 가능).
5. push 전 사전 점검: uncommitted 변경 없음 + `./gradlew compileJava` 통과 (가능하면 `./gradlew test`도 통과).

### 6.3 develop / main push 금지

- `develop`, `main`은 사용자 영역. Claude Code가 절대 push하지 않는다.
- Force push (`--force`, `--force-with-lease`)는 사용자 명시 동의 없이 금지 — Epic 브랜치라도.

---

## 7. PR 문서 자동 생성 (push 시점에만)

Epic 1개에 PR은 1개(`{type}/{NNN}-{slug} → develop`). **push가 실제로 일어나는 시점**에만 push와 함께 PR 본문 초안을 통째로 갱신해 출력한다 — 누적 모드: Epic 시작부터 지금까지의 완료 Story 전부를 합쳐 작성.

push를 보류한 Story 종료에는 PR 본문 초안을 생성하지 않는다 — 다음 push 시점에 그 사이 누적된 Story들이 한 번에 반영된다.

사용자는 **첫 push 시** GitHub UI에서 PR을 생성하고, 이후 push마다 출력된 새 본문으로 PR description을 덮어쓴다.

### 7.1 PR 제목

```
[{Epic 영역}] {Epic 한 줄 요약} (Epic-{NNN})
```

예: `[LearningFacade] AxisAction → AxisTopic 도메인 전환 (Epic-002)`

### 7.2 PR 본문 템플릿 (사용자 지정 형식 — 6개 섹션 고정)

```markdown
## 개요

{Epic이 무엇인지 1-2문장. 머지 대상 브랜치(develop) 명시. 진행 상태 — "Story-{NNN}-1, Story-{NNN}-2 완료, Story-{NNN}-3 진행 중" 같이 명시.}

## 연관 이슈

- Epic 문서: `workflow/epics/Epic-{NNN}.md`
- 포함된 Story 문서:
  - `workflow/stories/Story-{NNN}-1.md`
  - `workflow/stories/Story-{NNN}-2.md`
  - ...
- 관련 ADR: ADR{XXX} (Epic 진행 중 신규 추가했거나 근거로 사용한 모든 ADR)
- GitHub 이슈: #{사용자가 채움}

## 배경 및 문제

{Epic 단위 동기. Epic 문서에서 추출한 "왜 이 Epic이 필요했는가". private-docs의 도메인/테이블/API 명세에서 어떤 요구가 있었는가. 현재 코드가 그 요구를 어떻게 충족하지 못하고 있었는가.}

## 해결 방법

Story 단위로 그룹핑해 의미 단위 커밋을 정리한다.

### Story-{NNN}-1: {Story 한 줄 요약}
- `feat(...): ...` — 한 줄 설명
- `test(...): ...` — 한 줄 설명

### Story-{NNN}-2: {Story 한 줄 요약}
- `feat(...): ...` — 한 줄 설명
- `refactor(...): ...` — 한 줄 설명

### (현재 Story가 마지막인 경우) Story-{NNN}-N: {현재 Story 요약}
- ...

{Epic 단위로 도메인 모델·DB 스키마·API에 발생한 누적 변화를 한 줄씩 요약.}

## 트레이드오프

{Epic 전체에서 발생한 부정적 영향 또는 미래 부담. 없으면 "없음" 명시 — "없음"도 의도된 정보.}

- ...

## 완료 조건

Epic 안의 각 Story Acceptance Criteria를 그룹핑해 체크 표시:

### Story-{NNN}-1
- [x] 항목 1
- [x] 항목 2

### Story-{NNN}-2
- [x] 항목 1
- [ ] 미충족 항목 (있으면 사유 명시)

### Epic 단위 자체 점검
- [x] 컴파일 통과 (`./gradlew compileJava`)
- [x] 변경 범위 테스트 통과
- [x] BC 의존 방향 위반 없음 (또는 사용자 동의: ...)
- [x] 새 ErrorCode가 ErrorCode enum에 등록됨 (해당 시)
- [x] Flyway 마이그레이션 검증 통과 (해당 시)
- [x] 누적된 Story가 Epic 문서의 목표를 모두 달성 (Epic 종료 시점에만 체크)
```

### 7.3 자동 채울 항목 vs 사용자 보강

| 섹션 | Claude 자동 채움 | 사용자 보강 |
| --- | --- | --- |
| 개요 | Epic 식별자, 진행 중인 Story 목록, 머지 대상 브랜치 | 1-2문장 추가 컨텍스트 |
| 연관 이슈 | Epic·Story 문서 경로, 누적 ADR 번호 | GitHub 이슈 번호 |
| 배경 및 문제 | Epic 본문에서 추출한 동기 | 도메인적 추가 설명 |
| 해결 방법 | Story 그룹별 커밋 목록 자동 추출 (`git log --oneline develop..HEAD`를 [Story-...]로 그룹핑) | 도메인/스키마/API 변경 요약 |
| 트레이드오프 | 검토 결과를 그대로 적되 없으면 "없음" | 검토 누락분 보강 |
| 완료 조건 | 각 Story AC + 자체 점검 체크 | 미충족 항목 사유, Epic 단위 마지막 점검 |

자동 채움한 부분도 **사용자가 GitHub에서 편집할 수 있는 명확한 텍스트**로 출력한다 (자리 표시자 대신 실제 내용으로).

### 7.4 PR 본문 출력·저장

- 기본은 **채팅 응답에 markdown 코드 블록**으로 출력. 첫 Story push면 PR 생성용, 이후 Story push면 PR description 덮어쓰기용.
- 파일로 저장하지 않는다 (private-docs 단발 운영 원칙과 충돌 방지).
- 사용자가 명시적으로 "PR 초안 파일로 저장해줘"라고 요청하면 그때만 저장.

### 7.5 push 시점별 출력

| 시점 | Claude 출력 | 사용자 액션 |
| --- | --- | --- |
| Epic의 **첫 push** (누적 Story 1개 이상) | PR 본문 초안 (누적 Story 분량) + "GitHub에서 PR 생성: `{type}/{NNN}-{slug} → develop`" | GitHub에서 PR 생성, 본문 붙여넣기 |
| Epic의 **이후 push** | 누적 PR 본문 초안 (지금까지 모든 완료 Story 포함) + "기존 PR description을 이 본문으로 덮어쓰기" | 기존 PR description을 새 본문으로 갱신 |
| Epic **마지막 push** (Epic 종료 직전) | 최종 PR 본문 초안 + "PR 머지 가능 상태" | 최종 본문으로 갱신 후 PR 머지 |

한 번의 push가 여러 Story를 한꺼번에 반영할 수 있다 (§6 판단 기반으로 묶여 push되는 경우).

---

## 8. Epic 종료 시점 (사용자 영역)

Epic 안의 모든 Story 작업이 완료된 마지막 push 시점에서:

- Claude는 §7.2 템플릿의 **최종본**(모든 Story + 누적 ADR + Epic 단위 점검까지 체크)을 출력한다.
- 사용자는 GitHub PR description을 최종 본문으로 갱신한 뒤 PR을 머지한다 (`{type}/{NNN}-{slug} → develop`).
- 머지 후 develop → main 배포는 **사용자 release 관리** 영역. Claude Code가 관여하지 않는다.

---

## 9. 금지 사항

- **`--no-verify`**: 사용자 명시 요청 없이 사용 금지. pre-commit 훅 실패는 원인을 고쳐 새 커밋을 만든다.
- **`--amend`**: 사용자 명시 요청 없이 사용 금지.
- **`git add -A` / `git add .`**: 자제. 변경한 파일을 명시적으로 add.
- **빈 커밋**: 변경 없는 commit 생성 금지.
- **`develop` / `main` 직접 push**: 절대 금지.
- **Force push**: 사용자 명시 동의 없이 금지 (Epic 브랜치 포함).
- **Story별 별도 브랜치 생성 금지**: Epic 브랜치 1개에 모든 Story가 누적된다. Story 단위로 sub-branch를 만들지 않는다.
- **PR 생성·머지**: Claude Code가 `gh pr create`나 머지 명령을 실행하지 않는다.
- **민감 파일 커밋**: `.env`, `application-prod.yml`, `*.pem`, `credentials.*` 류는 staging 단계에서 검토하고 발견 시 사용자 경고 후 진행 중단.

---

## 10. Story 종료 보고 형식

Story 작업 종료 시 다음 항목을 포함해 보고한다.

1. 이번 Story에서 생성된 커밋 한 줄 요약 (`git log --oneline` 중 `[Story-{NNN}-{N}]` 필터).
2. 이번 Story에서 추가된 ADR이 있으면 번호와 한 줄 요약.
3. update-docs 종료 트리거 패키지(architecture, dict, table-spec, test) 갱신 후보를 사용자에게 1줄씩 보고 ([`update-docs.md`](./update-docs.md) §5 체크리스트).
4. **push 판단** (§6.1 신호 평가):
   - **신호 충족** → push 권장 1줄 보고 + 사용자 확인 요청 (§6.2 절차).
   - **신호 미충족** → push 거론 없이 누적 커밋 상태만 보고하고 다음 Story 대기.
5. **PR 본문 초안**: push가 실제 실행된 경우에만 §7.2 템플릿대로 출력 (누적 모드). push 보류 시 미출력.

### 보고 예시 — push 권장·실행 케이스 (Epic 002의 누적 push 시점)

```
Story-002-2 완료. 로컬에 3개 커밋이 추가되어 누적 6커밋 (Story-002-1+002-2).

이번 Story 커밋:
  4d50f81 feat(learningfacade): 주제 부분 수정 + 다건 추가 + 순서 변경 [Story-002-2]
  ...

push 권장합니다 — 누적 6커밋 + 새 API 추가 포함 (외부 가시성 큰 변경). 진행할까요?

(승인 받은 뒤 push 실행)

feat/002-axis-topic-migration에 push 완료.

---

PR 본문 초안 (기존 PR description을 아래 내용으로 덮어쓰세요):

```markdown
## 개요
Epic-002: AxisAction → AxisTopic 도메인 전환. develop 머지 대기.
Story-002-1, Story-002-2 완료. Story-002-3 미시작.

## 연관 이슈
...

(이하 §7.2 템플릿 누적 본문)
```

다음 단계:
- (첫 push면) GitHub에서 PR 생성 + 본문 붙여넣기. / (이후 push면) 기존 PR description을 위 본문으로 갱신.
- 다음 Story 명령을 받으면 같은 브랜치에서 Story-002-3 작업 시작.
```

### 보고 예시 — push 보류 케이스

```
Story-002-2 완료. feat/002-axis-topic-migration에 3개 커밋 추가 (로컬 누적 3커밋).

이번 Story 커밋:
  ...

push는 보류합니다 (누적 3커밋, 외부 가시성 작은 변경 — 다음 Story와 묶어 §6.1 신호 강해지면 다시 보고). PR 본문 초안은 다음 push 시점에 누적 갱신.

다음 Story 명령을 받으면 같은 브랜치에서 Story-002-3 작업 시작.
```

---

## 11. 의존 도구

- Git 2.30+ (Windows + PowerShell + Bash 둘 다 가능).
- pre-commit 훅이 있다면 그대로 신뢰 — 우회 금지.
- `gh` CLI는 GitHub 메타 조회(이슈 확인 등)에 사용 가능. **PR 생성·머지에는 사용하지 않는다**.
