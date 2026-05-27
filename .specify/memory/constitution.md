<!--
SYNC IMPACT REPORT
==================
Version change: (initial template) → 1.0.0
Bump rationale: First materialized constitution from existing project ruleset
  (CLAUDE.md, .claude/rules/*.md, docs/DOMAIN.md, docs/PACKAGE.md, docs/adr/).
  Treated as MAJOR (1.0.0) per semver convention for initial ratification.

Modified principles: N/A (initial fill — all 5 principles newly declared)

Added sections:
  - Core Principles (I–V)
  - Technology & Persistence Constraints
  - Development Workflow & Quality Gates
  - Governance

Removed sections: N/A

Templates requiring updates:
  - .specify/templates/plan-template.md
      ⚠ pending — "Constitution Check" section is a placeholder ("[Gates
      determined based on constitution file]"). Should be filled with explicit
      gates derived from Principles II/III/IV/V on next plan-template revision.
  - .specify/templates/spec-template.md
      ✅ no update required — story-driven spec format is compatible with
      Principle V; no mandatory section is added/removed by this constitution.
  - .specify/templates/tasks-template.md
      ✅ no update required — task categorization remains user-story-grouped;
      Principle IV's test matrix is enforced by reviewer session (Principle V),
      not by tasks template structure.
  - .specify/templates/checklist-template.md
      ✅ no update required — generic checklist scaffold is unaffected.
  - .specify/templates/constitution-template.md
      ✅ no update required — placeholder template kept as-is for future
      projects; this constitution.md is the project-specific instantiation.
  - CLAUDE.md
      ✅ no update required — CLAUDE.md is the entry pointer; this
      constitution is downstream of it and references its rule files.

Follow-up TODOs:
  - Update plan-template.md "Constitution Check" gates to reference
    Principles II/III/IV/V explicitly (separate task; out of scope for
    constitution amendment itself).
-->

# third-tool Constitution

본 헌법은 백엔드 코드베이스 `com.example.thirdtool`의 모든 작업을 지배하는 비협상 원칙을 선언한다. 본 문서가 `.claude/rules/*.md`, `docs/*.md`와 모순될 경우 **본 헌법이 우선**한다. 룰·문서는 본 헌법의 운영 매뉴얼이다.

## Core Principles

### I. Code as Source of Truth

코드·Flyway·테스트가 단일 진실 소스다. 별도 명세 문서는 두지 않는다.

- API 명세는 Controller + Request/Response DTO + Swagger UI에서만 추출한다. 별도 API 명세 마크다운을 만들지 않는다.
- DB 스키마는 `src/main/resources/db/migration/V*.sql` + JPA 매핑(`@Entity`, `@Column`)이 권위적이다. 스키마 문서를 따로 두지 않는다.
- 테스트 매트릭스는 테스트 코드 자체이며, 메서드명은 `{대상행위}_{상황}_{기대결과}` 형식으로 의도를 자체 문서화한다.
- 패키지·디렉토리 사실은 `src/main/java/com/example/thirdtool/` 트리가 결정한다.

`docs/` 디렉토리에는 **코드만으론 알 수 없는 의도와 결정**만 둔다 — `DOMAIN.md`(불변식·도메인 의도), `PACKAGE.md`(BC·레이어 의존 규칙), `adr/`(대안 비교·거부 사유). 코드에서 추출 가능한 사실을 마크다운에 복제하는 행위는 금지한다.

**Rationale**: 명세 문서와 코드의 이중 관리는 필연적으로 drift를 만든다. 코드만으로 도출되지 않는 *왜*(의도·결정)만 문서화하면 진실 소스 분기점이 사라진다.

### II. Bounded Context + Strict Layered Dependency (NON-NEGOTIABLE)

모든 Bounded Context는 동일한 4-레이어 구조를 강제하며, 의존 방향은 단방향이다.

- 6개 BC(`Card`, `Deck`, `Review`, `LearningFacade`, `User`, `UserSchedule`)는 각각 `presentation/`, `application/`, `domain/`, `infrastructure/` 4-레이어로 구성한다.
- `domain`은 다른 레이어를 import하지 **않는다** — 순수 자바 객체.
- `application`은 `presentation`을 import하지 **않는다** — Request DTO 누수 차단 (ADR005).
- `application`은 `infrastructure`의 **Port 인터페이스에만** 의존한다 — JPA·QueryDSL 구체 타입은 `infrastructure` 안에 가둔다.
- `Common`·`infra`는 어떤 BC도 import하지 **않는다** — 단방향 의존 (BC → Common).
- BC 간 협력은 `application/service`에서 **동기 도메인 이벤트**(ADR007)로만. Aggregate가 다른 Aggregate의 Repository를 직접 호출하지 않는다.
- Controller↔Service 경계의 입출력은 `application/dto`의 Command/Query record로만 전달한다 (ADR005).

**Rationale**: BC 경계와 의존 방향이 무너지면 도메인 응집이 와해되고 리팩토링 비용이 폭증한다. 동일 4-레이어 강제는 BC 간 인지 비용을 일정하게 유지한다.

### III. Domain Encapsulation & Double Defense

도메인 객체는 자기 불변식을 강제하고, DB 제약이 이를 이중으로 보강한다.

- VO·Entity·Aggregate Root는 정적 팩토리(`of(...)`, `create(...)`)로만 외부 진입을 노출한다. 생성자는 `protected`/`package-private`. 외부 `new` 금지.
- 자식 Entity는 부모 Aggregate의 행위를 통해서만 생성된다 (`addAxis`, `addTopic`, `addKeyword`).
- 자식 컬렉션은 `Collections.unmodifiableList(...)`로 캡슐화하여 노출. 추가/삭제는 Aggregate 행위로만.
- 입력 정규화(trim, 빈 문자열 → null)는 **도메인 메서드 내부**에서 수행. Application Service는 정규화하지 않는다.
- 상태 전환은 **멱등**(이미 같은 상태이면 예외 없이 no-op). 이력 기록 측도 `from == to`이면 이력을 만들지 않는다.
- `displayOrder`는 **1-based**이며 외부 주입 금지. 신규 추가는 `현재 max + 1` 자동 계산, 재배치는 `reorderX(orderedIds)` 메서드로만.
- 다건 입력은 한 건 실패 시 전체 롤백 — 부분 성공 불허.
- 도메인 규칙은 도메인 메서드 검증 + DB 제약(`UNIQUE`, `CHECK`) **둘 다** 둔다.
- 비즈니스 에러는 `Common/Exception/ErrorCode/ErrorCode` enum에 등록한 후 `{BC}DomainException`으로 throw하며, HTTP 변환은 `GlobalExceptionHandler`가 단일 진입점이다 — Controller에서 try-catch·응답 가공 금지.

**Rationale**: 도메인 객체가 자기 불변식을 강제하지 못하면 Application Service에 규칙이 펼쳐지고 동일 규칙이 여러 곳에서 재구현된다. 이중 방어는 도메인 검증 누락을 DB 제약이 막아 데이터 무결성을 최종 보장한다.

### IV. Test Discipline — Classist with 해피/엣지/예외 Matrix

도메인은 절대 Mock하지 않으며, 모든 도메인 행위는 해피·엣지·예외 세 구분을 가진다.

- JUnit 5 + AssertJ만 사용한다. Hamcrest, JUnit 4, Mockito-only assertion 금지.
- 도메인 객체(VO·Entity·Aggregate·Domain Service의 협력 도메인)는 **Classist** 전략으로 실제 객체를 사용한다. Mock은 Repository 인터페이스·외부 어댑터 등 **시스템 경계**에만.
- 각 도메인 메서드는 **해피·엣지·예외 세 구분** 모두 한 케이스 이상을 가진다. 엣지에는 trim/null 정규화, 멱등성, 경계값(권장 한도 ±1), `displayOrder` 첫 항목·재배치·id 집합 불일치를 포함한다.
- 메서드 명명은 `{대상행위}_{상황}_{기대결과}` 형식(한글 단어 허용).
- 같은 규칙을 여러 계층에서 중복 검증하지 않는다 — 도메인 규칙은 도메인 단위 테스트, DB UNIQUE는 `@DataJpaTest` Slice, HTTP 직렬화는 `@WebMvcTest` Slice가 각각 한 번씩.
- 픽스처는 정적 팩토리(`Card.create(...)`) 호출로만 만들며 `new` 금지. 픽스처 빌더는 BC별 테스트 패키지에 둔다 (프로덕션 코드 침범 금지).
- 시각 의존 테스트는 `Clock` 주입 또는 허용 범위 비교(`isAfter`, `isCloseTo`)로 처리한다.

**Rationale**: 도메인을 Mock하면 검증 대상 자체가 시뮬레이션으로 대체되어 회귀 안전망이 무너진다. 해피·엣지·예외 3구분은 누락 빈도가 가장 높은 멱등성·정규화·경계값을 강제로 가시화한다.

### V. Story-Driven Workflow with Parallel Reviewer Session (NON-NEGOTIABLE)

모든 작업은 Story 단위로 수행되며 push 전 5관점 Reviewer 세션을 강제로 통과한다.

- Story 명령 수신 시 `.claude/rules/workflow.md`의 Step 1~5를 강제 적용한다: 읽기(Story→Epic→DOMAIN.md→코드) → Plan mode(비단순 Story) → 실행(의미 단위 커밋) → Reviewer 세션 → push 판단.
- Reviewer 세션은 **5관점 병렬 subagent**(Domain / Architecture / API·Exception / Test / Sceptical)를 단일 메시지에서 동시 발사한다. 단일 Claude의 자가 점검으로 대체할 수 **없다**. 직렬 호출 금지.
- Reviewer 발사 **직전에** 메인 Claude가 변경 요약본을 사용자에게 제시하고 확인을 받는다 (사용자 통찰을 Sceptical Reviewer에 반영하기 위함). 요약본 사후 보고로 대체 금지.
- Reviewer는 **read-only**다. 코드 수정은 메인 Claude가 사용자 의사결정 후 별도 커밋으로 수행한다.
- 스킵은 문서 전용 변경 / 단순 chore / 자동 생성 산출물 갱신 한정. 사용자에게 스킵 사유 1줄 명시 의무.
- 아키텍처 결정(ADR 트리거: 새 BC 도입, BC 간 의존 추가, PK 전략·Enum 저장·Soft Delete 정책 변경, 트랜잭션 경계 변경, 외부 시스템 통합 등)은 작업 흐름과 **독립**으로 즉시 `docs/adr/ADR{NNN}.md`를 생성하고 `docs(adr): ...` 별도 커밋한다.
- 머지는 사람이 수행한다. Claude는 push·머지 명령을 자율 실행하지 않으며, push 직전 사용자 확인을 받는다.

**Rationale**: 단일 Claude의 자가 점검은 캡슐화 위반·NPE·BC 의존 위반 같은 다관점 누락을 막지 못한다. 5관점 병렬 검토는 토큰 비용 대비 누락 발견 가치가 명백히 높다. 사용자 사전 확인 단계는 Sceptical Reviewer에 사용자 통찰을 주입할 유일한 기회다.

## Technology & Persistence Constraints

본 섹션은 위 원칙의 운영 환경을 고정한다.

- **언어·런타임**: Java 21 (Gradle Wrapper toolchain). Kotlin·Scala 등 JVM 대안 도입 금지.
- **빌드**: Gradle Wrapper(`gradlew`/`gradlew.bat`)만 사용한다. 로컬 `gradle` 호출 금지.
- **프레임워크**: Spring Boot + JPA (Hibernate) + QueryDSL. QueryDSL Q클래스는 `src/main/generated/`에 자동 생성.
- **DBMS**: prod는 MySQL 8.0 + Flyway, dev는 H2(런타임 의존). 스키마 변경은 **새 `V{n}__*.sql` 파일 추가만** 허용하며 기존 V 파일 수정은 Flyway 정합성 위반이다. 컬럼 추가 + NOT NULL 전환은 (1) ADD COLUMN NULL → (2) 백필 UPDATE → (3) MODIFY NOT NULL 3단계로 분리한다.
- **PK**: 모든 테이블은 `BIGINT NOT NULL AUTO_INCREMENT` (ADR001). UUID/ULID 금지.
- **Enum 저장**: `VARCHAR + CHECK 제약 + @Enumerated(EnumType.STRING)` (ADR002). `@Enumerated(EnumType.ORDINAL)` 사용 금지.
- **Soft Delete**: 사용자 자산성 도메인(`Card`/`Deck`/`LearningFacade`/`LearningMaterial`/`User`)에만 적용 (ADR003). `@SQLRestriction` + `@SQLDelete` 패턴.
- **트랜잭션**: OSIV=false 유지, 격리 수준 READ_COMMITTED.
- **에러 변환**: `Common/Exception/GlobalExceptionHandler` 단일 진입점. ErrorCode 네이밍은 `{BC_PREFIX}_{도메인_요소}_{상태}` SCREAMING_SNAKE_CASE.
- **API URL**: `/api/v1` prefix, `application/json`. 부분 수정은 PATCH, 컬렉션 전체 교체는 PUT.
- **로깅** (ADR008): profile별 Appender 분기 + LogstashEncoder + MDC 화이트리스트. 민감 필드 로깅 금지.

## Development Workflow & Quality Gates

- **브랜치**: `<type>/<NNN>-<short-kebab-case>` 형식. **Epic 1개 = 브랜치 1개**. main 브랜치 직접 커밋 금지.
- **커밋**: Conventional Commits — `<type>(<scope>): <subject> [Story-{NNN}-{N}]`. 의미 단위 분리. ADR 갱신은 `docs(adr): ...` 별도 커밋.
- **PR**: 본문은 `## What / ## Why / ## How / ## Tradeoff / ## Reviewer 종합 / ## Test / ## Checklist` 형식으로 작성하고 마지막에 `Closes #<이슈번호>`.
- **Plan Mode**: 비단순 Story는 Plan mode에 진입해 사용자와 접근을 합의한 후 구현을 시작한다.
- **Reviewer Gate**: Principle V의 5관점 세션을 통과하지 않은 변경은 push되지 않는다. 스킵 사유 명시 의무.
- **머지 정책**: 머지는 사람이 직접 수행. Claude는 PR 생성·머지 명령을 자율 실행하지 않는다.
- **Push 직전 확인**: `.claude/rules/pr-commit.md` §6의 push 신호 체크 + 사용자 확인 후에만 push 진행.
- **CLAUDE.md 우선순위**: 본 헌법의 원칙과 CLAUDE.md의 안내가 일치하지 않을 경우 본 헌법이 우선한다. CLAUDE.md는 빠른 진입 인덱스이며 단일 진실 소스가 아니다.

## Governance

- 본 헌법은 프로젝트의 모든 ad-hoc 결정·관행에 **우선**한다. PR·Story·Reviewer 세션은 본 원칙을 준수해야 통과된다.
- **수정 절차**: 본 헌법의 수정은 (1) 관련 ADR 추가 또는 갱신 → (2) `.specify/memory/constitution.md` 수정과 함께 위의 SYNC IMPACT REPORT 갱신 → (3) `docs(governance): amend constitution to vX.Y.Z` 별도 커밋으로 진행한다. 본 절차를 우회한 원칙 변경은 무효다.
- **버전 정책** (semantic versioning):
  - **MAJOR**: 원칙의 후방 호환성 깨지는 제거·재정의(예: NON-NEGOTIABLE 원칙 해제, 4-레이어 구조 폐기 등).
  - **MINOR**: 신규 원칙 또는 신규 섹션 추가, 가이드의 실질적 확장.
  - **PATCH**: 표현 정정·오타·비의미적 정제.
- **컴플라이언스 검토**: 모든 Story Reviewer 세션(Principle V)이 컴플라이언스 검토 지점이다. Domain·Architecture·API·Test·Sceptical 5관점이 각자 본 헌법의 원칙에 비추어 변경을 평가한다.
- **ADR 우선순위**: 본 헌법은 ADR보다 상위 규범이다. 다만 본 헌법의 원칙을 구체화·운영화하는 ADR은 헌법의 부속 문서로서 동일한 강제력을 갖는다. 원칙과 ADR이 충돌할 경우 원칙이 우선하며, ADR은 그에 맞춰 갱신되어야 한다.
- **런타임 지침**: 일상 개발 지침은 `CLAUDE.md` + `.claude/rules/*.md` + `docs/DOMAIN.md` + `docs/PACKAGE.md` + `docs/adr/` 체계를 사용한다. 본 헌법은 그 체계의 상위 규범이다.

**Version**: 1.0.0 | **Ratified**: 2026-05-26 | **Last Amended**: 2026-05-26
