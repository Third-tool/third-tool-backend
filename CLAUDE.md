# CLAUDE.md

> Claude Code의 단일 진입 포인터. 작업 흐름·구조·컨벤션의 본질은 본 파일에서 시작해 해당 룰·문서로 연결된다.

---

## 진입 (이 5개 파일 + workflow/만 알면 충분)

| 무엇을 알고 싶을 때 | 어디를 보나 |
| --- | --- |
| **작업 흐름 / Story 실행 절차** | [`.claude/rules/workflow.md`](.claude/rules/workflow.md) |
| **도메인 의도·용어·불변식** | [`docs/DOMAIN.md`](docs/DOMAIN.md) |
| **패키지·BC·레이어 구조** | [`docs/PACKAGE.md`](docs/PACKAGE.md) |
| **아키텍처 결정 기록 (ADR)** | [`docs/adr/`](docs/adr/) |
| **코드 작성 컨벤션** (도메인·API·DB·테스트) | [`.claude/rules/conventions.md`](.claude/rules/conventions.md) |
| **Story Reviewer 세션** (5관점 병렬 검토) | [`.claude/rules/review.md`](.claude/rules/review.md) |
| **브랜치·커밋·push·PR 규칙** | [`.claude/rules/pr-commit.md`](.claude/rules/pr-commit.md) |
| **ADR 작성 트리거** | [`.claude/rules/adr.md`](.claude/rules/adr.md) |
| **현재 진행 중인 Epic / Story** | `workflow/epics/Epic.md` · `workflow/stories/Story.md` |

본 CLAUDE.md의 다른 섹션은 위 룰·문서와 모순될 경우 **룰·문서가 우선**한다 — CLAUDE.md는 빠른 개요이지 단일 진실 소스가 아니다.

---

## 코드가 진실 소스 (Source of Truth)

다음 영역은 **별도 docs 파일을 두지 않는다** — 코드/Flyway/테스트가 단일 진실 소스이기 때문이다. Plan mode가 매번 코드에서 추출한다.

| 영역 | 진실 소스 |
| --- | --- |
| API 명세 | Controller + Request/Response DTO + Swagger UI (`/swagger-ui.html`) |
| DB 스키마 | Flyway `V*.sql` + JPA 매핑 (`@Entity`, `@Column`) |
| 테스트 매트릭스 | 테스트 코드 + 메서드명 (`{대상행위}_{상황}_{기대결과}`) |
| 패키지·디렉토리 사실 | `src/main/java/com/example/thirdtool/` 디렉토리 트리 |
| 인프라 사실 (AWS 리소스·CI/CD 파이프라인) | `.github/workflows/*.yml` + `application.yml` + AWS 콘솔 |

별도 명세 문서를 만들지 말 것. 위 영역의 변경은 docs 갱신을 묻지 않는다 ([`.claude/rules/workflow.md`](.claude/rules/workflow.md) Step 5 표 참조).

`docs/`에 두는 것은 **코드만으론 알 수 없는 의도와 결정**뿐이다:
- `docs/DOMAIN.md` — 도메인 의도·용어·불변식 (코드에는 "what"만 있고 "why"가 없음)
- `docs/PACKAGE.md` — 패키지·BC·레이어 의존 규칙 (코드 구조는 사실이지만 그 *규칙*은 의도)
- `docs/adr/` — 아키텍처 결정 기록 (대안 비교·거부 사유는 코드에 없음)

---

## 명령어

빌드 도구는 Gradle Wrapper(`gradlew`/`gradlew.bat`), Java 21 toolchain.

```bash
./gradlew build                                                              # 전체 빌드 + 테스트
./gradlew bootRun                                                            # 로컬 실행 (dev 프로필, H2)
./gradlew test                                                               # 전체 테스트
./gradlew test --tests "com.example.thirdtool.Card.domain.model.CardTest"    # 단일 클래스
./gradlew test --tests "com.example.thirdtool.Card.domain.model.CardTest.create_*"  # 단일 메서드 패턴
./gradlew clean                                                              # build/ 와 src/main/generated/ (QClass) 삭제
```

QueryDSL Q클래스는 `src/main/generated/`에 자동 생성. `clean` 시 함께 삭제. 도메인 클래스 추가/이동 직후 IDE에서 Q클래스가 보이지 않으면 한 번 컴파일.

Swagger UI: 실행 후 `http://localhost:8080/swagger-ui.html`.

---

## 아키텍처 핵심

`com.example.thirdtool` 하위는 **6개 BC** + 비-BC 공통 모듈로 구성. 모든 BC는 동일한 **4-레이어 구조**를 강제.

```
{BC}/
├── presentation/   # Controller, Request/Response DTO
├── application/    # dto/ (Command/Query record, ADR005) + service/ (Command/Query 분리)
├── domain/         # model/, exception/ — Aggregate, Entity, VO, Domain Service, BC 전용 예외
└── infrastructure/ # persistence/ (Repository Port + Adapter + JPA + QueryDSL), dto/ (조회 전용)
```

상세 — 패키지 트리·의존 방향·Command/Query record 패턴: [`docs/PACKAGE.md`](docs/PACKAGE.md)
도메인 의도·불변식·v4 결정: [`docs/DOMAIN.md`](docs/DOMAIN.md)

**예외와 ErrorCode**:
- BC 전용 도메인 예외는 `{BC}/domain/exception/{BC}DomainException`에 두고 `Common/Exception/BusinessException` 상속.
- HTTP 변환은 `Common/Exception/GlobalExceptionHandler`가 일괄 처리.
- 사용자 노출 비즈니스 에러는 `Common/Exception/ErrorCode/ErrorCode` enum에 코드를 등록해 던진다 (예: `CARD_KEYWORD_MIN_REQUIRED`, `LEARNING_AXIS_DUPLICATE_NAME`). 새 도메인 룰 추가 시 ErrorCode부터 등록.

---

## Issue 작성 규칙

### 제목 형식
`[<domain>] <action>: <one-line summary>`

예시:
- `[Card] feat: 카드 일괄 비활성화 API 추가`
- `[LearningFacade] refactor: AxisAction → AxisTopic 마이그레이션`
- `[Infra] chore: Redisson 분산 락 설정 외부화`

### 본문 템플릿
```
## 배경
<왜 이 작업이 필요한가>

## 목표
<무엇을 달성하나>

## 완료 기준 (DoD)
- [ ] 구현
- [ ] 단위 테스트
- [ ] 슬라이스 테스트 (필요 시)
- [ ] 문서 업데이트 (ADR/DOMAIN/PACKAGE)

## 관련 ADR/문서
- docs/adr/ADR{NNN}.md

## 영향받는 컴포넌트
- <bounded context / 파일>
```

### 라벨 규칙
- type: feat / fix / refactor / chore / docs / test
- domain: card / deck / learning / axis / infra / payments
- priority: P0 / P1 / P2

---

## PR 작성 규칙

### 제목 형식
이슈 제목과 동일 형식. 끝에 `(#<이슈번호>)` 추가.

### 본문 템플릿
```
## What
<무엇을 했는가>

## Why
<왜 했는가 - 이슈/ADR 링크>

## How
<주요 설계 결정 요약>

## Tradeoff
<작업 중 있었던 트레이드오프>

## Reviewer 종합 (review.md §4 결과)
<Critical/Major 건수 + 사용자 의사결정 결과 1줄>

## Test
- [ ] 단위 테스트 추가/통과
- [ ] 통합 테스트 결과
- [ ] 성능 영향 (해당 시)

## Checklist
- [ ] DOMAIN.md / PACKAGE.md / ADR 업데이트 반영
- [ ] OSIV=false, READ_COMMITTED 등 프로젝트 원칙 준수
- [ ] BC 간 의존 방향 위반 없음
- [ ] Reviewer 세션 통과 (또는 의식적 스킵 사유 명시)

Closes #<이슈번호>
```

### 브랜치 명명
`<type>/<NNN>-<short-kebab-case>` (Epic 1개 = 브랜치 1개, 상세는 [`pr-commit.md`](.claude/rules/pr-commit.md))

예시:
- `feat/006-card-bulk-deactivation`
- `refactor/007-deck-persistence-naming`

---

## Commit 규칙
- Conventional Commits 형식: `<type>(<scope>): <subject> [Story-{NNN}-{N}]`
- 예: `feat(card): 카드 일괄 비활성화 도메인 로직 추가 [Story-006-1]`

---

## 머지 정책
- 머지는 사람이 직접. Claude는 PR 생성·머지 명령을 실행하지 않는다.

---

## 작업 흐름 (요약)

Story 명령 수신 시 [`workflow.md`](.claude/rules/workflow.md) Step 1~5 강제:

1. **읽기** — Story → Epic → `docs/DOMAIN.md` 해당 BC → 코드.
2. **Plan mode** — 비단순 Story는 Plan mode 진입, 사용자와 합의.
3. **실행** — 코드·테스트, 의미 단위 커밋.
4. **Reviewer 세션 (강제)** — 5관점 병렬 subagent로 의심 부분 지적, 사용자 의사결정.
5. **Push 판단** — pr-commit.md §6 신호로 사용자 확인 후 push.

ADR 트리거는 Step 1~5와 독립으로 작업 중 즉시 — `docs(adr): ...` 별도 커밋.

---

## 데이터베이스

- dev: H2 (런타임만 의존)
- prod: MySQL + Flyway (`org.flywaydb:flyway-mysql`)
- 마이그레이션은 `src/main/resources/db/migration/V*__*.sql`. 스키마 변경은 새 V 버전 파일을 추가한다 (기존 파일 수정 금지 — Flyway 정합성).

---

## 폐기된 패턴 (참고)

다음 패키지·문서는 **이전 체계의 잔재**로 모두 폐기되었다. 새로 만들지 말 것:

- `private-docs/` — 정책 입력 reference (api·table·test·domain). 폐기. 모든 영속 자료는 `docs/`로 통합됨.
- `update-docs/` — Claude Code 기록 출력 (architecture·dict·table-spec·test·adr). 폐기. ADR은 `docs/adr/` tracked로 정착.
- `docs/architecture/`, `docs/database/` — 흡수/폐기 후 `docs/PACKAGE.md` 단일 파일로 정착.
- `.claude/rules/private-docs.md`, `update-docs.md`, `update-docs/*.md`, `{domain,api,db,test}-conventions.md` — 모두 폐기. `conventions.md` + `review.md` + `adr.md`로 압축.
