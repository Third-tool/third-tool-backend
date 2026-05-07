# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## 첫 레이어: `.claude/rules/`

**모든 작업은 `.claude/rules/`의 규칙 파일을 먼저 참조한 뒤 시작한다.** 본 CLAUDE.md는 진입 포인터와 코드베이스 개요만 제공한다. 실제 절차·컨벤션은 규칙 파일이 정의한다.

| 규칙 파일 | 언제 참조하나 |
| --- | --- |
| [`.claude/rules/workflow.md`](.claude/rules/workflow.md) | Story 명령 수신 시 — 작업 단위·실행 순서·브랜치 |
| [`.claude/rules/private-docs.md`](.claude/rules/private-docs.md) | Story가 어떤 reference docs를 읽어야 하는지 매칭 |
| [`.claude/rules/pr-commit.md`](.claude/rules/pr-commit.md) | 브랜치 전략(Epic/Story), 커밋 단위·메시지, push 타이밍, PR 본문 생성 |
| [`.claude/rules/adr.md`](.claude/rules/adr.md) | 작업 중 아키텍처 결정 감지 시 자동 작성 |
| [`.claude/rules/domain-conventions.md`](.claude/rules/domain-conventions.md) | 도메인 객체 작성·수정 시 |
| [`.claude/rules/api-conventions.md`](.claude/rules/api-conventions.md) | Controller/DTO 작성 시 |
| [`.claude/rules/db-conventions.md`](.claude/rules/db-conventions.md) | JPA 매핑·Flyway 작성 시 |
| [`.claude/rules/test-conventions.md`](.claude/rules/test-conventions.md) | 테스트 작성 시 |

규칙 파일은 다시 다음 두 자료원으로 안내한다:
- **`workflow/`** (프로젝트 루트, gitignored) — 현재 Epic / Story / Product 컨텍스트 (`workflow.md`가 정의)
- **`private-docs/{adr,api,domain,table,test}/`** — 운영 reference 패키지 (`private-docs.md`가 매칭 프로토콜 정의)

CLAUDE.md의 다른 섹션(아키텍처 핵심, 명령어, 데이터베이스)은 규칙 파일과 모순될 경우 **규칙 파일이 우선**한다 — CLAUDE.md는 빠른 개요이지 단일 진실 소스가 아니다.

---

## 명령어

빌드 도구는 Gradle Wrapper(`gradlew`/`gradlew.bat`)이며, Java 21 toolchain을 사용합니다.

```bash
./gradlew build                                                # 전체 빌드 + 테스트
./gradlew bootRun                                              # 로컬 실행 (dev 프로필, H2)
./gradlew test                                                 # 전체 테스트
./gradlew test --tests "com.example.thirdtool.Card.domain.model.CardTest"   # 단일 클래스
./gradlew test --tests "com.example.thirdtool.Card.domain.model.CardTest.create_*"  # 단일 메서드 패턴
./gradlew clean                                                # build/ 와 src/main/generated/ (QClass) 삭제
```

QueryDSL Q클래스는 `src/main/generated/`에 자동 생성되며 `clean` 시 함께 삭제됩니다. 도메인 클래스를 추가/이동한 직후 IDE에서 Q클래스가 보이지 않으면 한 번 컴파일하세요.

Swagger UI: 실행 후 `http://localhost:8080/swagger-ui.html`.

---

## 아키텍처 핵심 (코드를 어디에 둘지 결정)

상세 가이드: [`docs/architecture/package-structure-guide.md`](../docs/architecture/package-structure-guide.md), [`docs/architecture/adr/ADR001.md`](../docs/architecture/adr/ADR001.md), 도메인 사전: [`docs/domain/용어사전.md`](../docs/domain/용어사전.md), [`docs/domain/도메인모델.md`](../docs/domain/도메인모델.md), 진입 문서: [`PROJECT_OVERVIEW.md`](../PROJECT_OVERVIEW.md).

### Bounded Context 단위로 패키지가 잘려 있다

`com.example.thirdtool` 하위는 BC(Card, Deck, Review, User, LearningFacade, UserSchedule, …) + 비-BC 공통 모듈(`Common`, `infra`)로 구성됩니다. 모든 BC는 **동일한 4-레이어 구조**를 강제합니다.

```
{BC}/
├── presentation/   # Controller, Request/Response DTO
├── application/    # service/  — Command / Query 분리
├── domain/         # model/, exception/  — Aggregate, Entity, VO, Domain Service, BC 전용 예외
└── infrastructure/ # persistence/ (Repository Port + Adapter + JPA + QueryDSL), dto/ (조회 전용)
```

새 기능을 어디 둘지 고민될 때는 이 레이어 구조에 맞춥니다. 기존 BC 중 일부(Deck, User)는 디렉토리 명명이 약간 다르지만(`repository/` vs `persistence/`, BC 루트 `dto/`), **새 코드는 위 표준을 따릅니다**.

### 도메인 규칙은 도메인 모델 안에서 강제한다

- Aggregate Root는 자신의 불변식을 자기 메서드(`Card.create()`, `Card.archive()`, `Card.recordView()`, `LearningFacade.addAxis()`, `AxisTopic.updateName()` 등)에서 검증합니다. Application Service에 비즈니스 규칙을 분산시키지 않습니다.
- VO(`MainNote`, `Summary`, `OnFieldBudget`, `SoftScheduleTemplate`, `ConceptChangeRecord` 등)는 Embeddable로 도메인에 인라인됩니다. Setter 대신 정적 팩토리(`of(...)`)와 `requireNonNull` 류 검증을 씁니다.
- 컬렉션 노출은 `Collections.unmodifiableList(...)`로 감쌉니다.
- Soft Delete를 쓰는 도메인(Card, Deck)은 `softDelete()`를 통해서만 제거합니다.

### 예외와 ErrorCode

- BC 전용 도메인 예외는 `{BC}/domain/exception/{BC}DomainException`에 두고 `Common/Exception/BusinessException`을 상속합니다. 범용 예외만 `Common/Exception`에 둡니다.
- HTTP 변환은 `Common/Exception/GlobalExceptionHandler`가 일괄 처리.
- 사용자에게 노출되는 모든 비즈니스 에러는 `Common/Exception/ErrorCode/ErrorCode` enum에 코드를 정의해서 던집니다(예: `CARD_KEYWORD_MIN_REQUIRED`, `LEARNING_AXIS_DUPLICATE_NAME`). 새 도메인 룰을 추가하면 ErrorCode부터 등록하세요.

---

## 작업 흐름

작업 단위(Epic / Story / Product)와 운영 reference 패키지(adr / api / domain / table / test)는 모두 `private-docs/` 하위에 있다. 절차는 다음 규칙 파일이 정의한다.

- **Story 실행 순서**: [`.claude/rules/workflow.md`](.claude/rules/workflow.md)
- **Story → reference 패키지 매칭**: [`.claude/rules/private-docs.md`](.claude/rules/private-docs.md)
- **브랜치 / 커밋 / push / PR 본문 생성**: [`.claude/rules/pr-commit.md`](.claude/rules/pr-commit.md)
- **ADR 자동 작성 트리거**: [`.claude/rules/adr.md`](.claude/rules/adr.md)

**Epic 1개 = 브랜치 1개** (`{type}/{NNN}-{epic-slug}`). Epic 안의 모든 Story가 같은 브랜치에 누적 커밋되며, Claude Code는 각 Story 종료마다 push + 누적 PR 본문 초안을 출력한다. PR 생성·머지(`{type}/{NNN}-{slug} → develop`, `develop → main`)는 사용자가 GitHub UI에서 직접 수행한다 ([`pr-commit.md`](.claude/rules/pr-commit.md) §6-§8).

---

## 데이터베이스

- dev: H2 (런타임만 의존)
- prod: MySQL + Flyway (`org.flywaydb:flyway-mysql`)
- 마이그레이션은 `src/main/resources/db/migration/V*__*.sql`. 스키마 변경은 새 V 버전 파일을 추가합니다(기존 파일 수정 금지 — Flyway 정합성 깨짐).

상세는 [`README.md`](../README.md).