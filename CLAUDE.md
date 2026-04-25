# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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

## 아키텍처 핵심

상세 가이드: [`docs/architecture/package-structure-guide.md`](docs/architecture/package-structure-guide.md), [`docs/architecture/adr/ADR001.md`](docs/architecture/adr/ADR001.md), 도메인 사전: [`docs/domain/용어사전.md`](docs/domain/용어사전.md), [`docs/domain/도메인모델.md`](docs/domain/도메인모델.md), 진입 문서: [`PROJECT_OVERVIEW.md`](PROJECT_OVERVIEW.md).

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

### Repository는 Port/Adapter로 분리한다

도메인은 Spring Data JPA에 직접 의존하지 않습니다. 패턴(Card BC가 표준):

```
domain/        ──→ infrastructure/persistence/CardRepository           (Port, interface)
                                              CardRepositoryAdapter    (Port 구현 — domain ↔ JPA 변환)
                                              CardJpaRepository        (Spring Data JPA)
                                              CardRepositoryCustom     (QueryDSL interface)
                                              CardJpaRepositoryImpl    (QueryDSL 구현)
```

새 Repository를 만들 때 도메인 서비스는 Port만 의존하고, Adapter가 JPA·QueryDSL을 캡슐화합니다.

### Application Service는 Command/Query를 분리한다

각 BC는 `{BC}CommandService`(상태 변경, 트랜잭션 경계)와 `{BC}QueryService`(읽기 전용 조회)를 분리합니다. Deck처럼 추가 책임이 있는 경우 `DeckHierarchyService`처럼 별도 서비스로 더 쪼갭니다.

### BC 간 의존 방향 (엄격)

```
Card   ←── Review     (ReviewSession이 Card ID만 참조, Aggregate 직접 포함 금지)
Card   ←── Deck       (Deck이 Card를 포함)
User   ←── Card, Deck (UserEntity 참조)
Common ←── 모든 BC    (역방향 금지)
```

- **BC 간에는 객체가 아니라 ID로 참조**하는 것이 기본. Cross-BC 조회가 필요하면 Application Service에서 각 BC Repository를 조합합니다.
- `Common`은 어떤 BC도 import하지 않습니다. 위반은 회귀로 간주.

### 도메인 규칙은 도메인 모델 안에서 강제한다

- Aggregate Root는 자신의 불변식을 자기 메서드(`Card.create()`, `Card.archive()`, `Card.recordView()`, `LearningFacade.addAxis()`, `AxisAction.updateDescription()` 등)에서 검증합니다. Application Service에 비즈니스 규칙을 분산시키지 않습니다.
- VO(`MainNote`, `Summary`, `OnFieldBudget`, `SoftScheduleTemplate`, `ConceptChangeRecord`, `ActionChangeRecord`)는 Embeddable로 도메인에 인라인됩니다. Setter 대신 정적 팩토리(`of(...)`)와 `requireNonNull` 류 검증을 씁니다.
- 컬렉션 노출은 `Collections.unmodifiableList(...)`로 감쌉니다.
- Soft Delete를 쓰는 도메인(Card, Deck)은 `softDelete()`를 통해서만 제거합니다.

### 예외와 ErrorCode

- BC 전용 도메인 예외는 `{BC}/domain/exception/{BC}DomainException`에 두고 `Common/Exception/BusinessException`을 상속합니다. 범용 예외만 `Common/Exception`에 둡니다.
- HTTP 변환은 `Common/Exception/GlobalExceptionHandler`가 일괄 처리.
- 사용자에게 노출되는 모든 비즈니스 에러는 `Common/Exception/ErrorCode/ErrorCode` enum에 코드를 정의해서 던집니다(예: `CARD_KEYWORD_MIN_REQUIRED`, `LEARNING_AXIS_DUPLICATE_NAME`). 새 도메인 룰을 추가하면 ErrorCode부터 등록하세요.

### 인증

OAuth2(Kakao, Naver) 로그인 후 자체 JWT 발급. 토큰 발급/갱신 책임은 `Common/security/auth/jwt`에 있고, `User` BC는 사용자 도메인 로직만 담당합니다. Refresh 토큰 저장(`RefreshEntity`)이 현재 `Common/security`에 있는 것은 알려진 위치 이슈로, 옮기지 말고 그대로 두세요(가이드에 "이동 검토 여지" 명시).

## 테스트 전략

가이드의 매트릭스를 그대로 따릅니다.

| 대상 | 전략 |
|---|---|
| `domain/model` | Classist — 실제 인스턴스, Mock 최소화 |
| `infrastructure/persistence` | `@DataJpaTest` 슬라이스 |
| `application/service` | 선택적 Mockist — 외부 의존만 Mock |
| `presentation` | `@WebMvcTest` 슬라이스 |
| `test/.../support/` | `DomainFixture` 등 테스트 픽스처/베이스 클래스 공용 모듈 |

도메인 테스트는 ReflectionTestUtils로 ID/내부 상태를 주입하는 패턴을 사용합니다. 새 테스트도 `support/DomainFixture`를 우선 사용하세요.

## 데이터베이스

- dev: H2 (런타임만 의존)
- prod: MySQL + Flyway (`org.flywaydb:flyway-mysql`)
- 마이그레이션은 `src/main/resources/db/migration/V*__*.sql`. 스키마 변경은 새 V 버전 파일을 추가합니다(기존 파일 수정 금지 — Flyway 정합성 깨짐).

## 협업 흐름 (README 발췌)

`main`에서 `feature/*` 분기 → 각자 작업 → 동료 머지 후 `git pull origin main`으로 본인 브랜치 동기화 → 충돌 해결 후 PR. 상세는 [`README.md`](README.md).
