# 0.0.2v / Backend Architecture (0.0.3v 급행 종료 시점)

> **범위**: 본 문서는 0.0.2v/Tier 1+Tier 2 (16 Story · PR #195~#200) 착지 후 **현재 백엔드 코드 아키텍처의 실제 상태**를 요약한다.
> "무엇이 어디에 있고, 왜 그렇게 배치했는지, 그리고 면접에서 물어볼 만한 결정들"을 압축.
> 상세 규범은 `docs/PACKAGE.md`·`docs/DOMAIN.md`·`docs/adr/`를 참조.

---

## 1. 큰 그림 (One-Screen)

```
                             ┌────────────────────────────────────┐
                             │   Presentation (Controller · DTO)  │
                             │   /api/v1/learning-facade/...      │
                             │   ApiResponse<T> 래퍼 + Bean Vali. │
                             └───────────────┬────────────────────┘
                                             │  Command/Query record (ADR005)
                             ┌───────────────▼────────────────────┐
                             │   Application (Service · Port·in)  │
                             │   CommandService · QueryService    │
                             │   @Transactional 경계              │
                             │   RoleDetector · SuggestionCatalog │
                             │   조율 (도메인은 여기서 호출)      │
                             └───────────────┬────────────────────┘
                                             │
                    ┌───────────────┬────────┴──────────┐
                    ▼               ▼                   ▼
        ┌─────────────────┐  ┌─────────────┐   ┌────────────────────────┐
        │  Domain (핵심)  │  │  Port · out │   │  Infrastructure         │
        │  Aggregate·VO   │  │  interface  │◀──│  JPA Adapter · Flyway  │
        │  불변식 · 팩토리 │  │  (헥사고날)   │   │  Static AI Adapter     │
        │  self-validation │  └─────────────┘   │  Repository Adapter    │
        └─────────────────┘                     └────────────────────────┘

                        MySQL 8.0 (운영) · H2 MODE=MySQL (로컬·테스트)
                        Flyway V1~V19 (+ R1~R19 rollback)
```

**핵심 원칙 3개**:
1. **의존은 안쪽으로만** — Presentation → Application → Domain. Infrastructure는 Domain의 Port 인터페이스만 구현 (헥사고날).
2. **도메인이 진실 소스** — 불변식·검증·상태 전이는 Aggregate/VO 안에서. Application Service는 트랜잭션·조율만.
3. **트랜잭션·검증 이중 방어** — 도메인 검증(친절한 에러) + DB 제약(안전망) 병행.

---

## 2. Bounded Context 구성 (6개)

| BC | 진입 관심사 | Aggregate Root | 대표 파일 |
| --- | --- | --- | --- |
| `LearningFacade` | 사용자 학습 지도 · concepts[] · Layer · Axis · Topic · AI 제안 | `LearningFacade`, `LearningLayer`, `LearningAxis`, `AxisTopic`, `LearningMaterial` | `LearningFacade.java`, `LearningLayer.java` |
| `Card` | 카드 · MainNote · KeywordCue · Summary · Tag · CardStatus | `Card`, `CardStatusHistory` | `Card.java` |
| `Deck` | Deck (Axis 자동 생성) · progressStatus | `Deck` | `Deck.java` |
| `Review` | Review 세션 · ReviewCard · 순회·공개 흐름 | `ReviewSession` | `ReviewSession.java` |
| `User` | 로컬·소셜 인증 통합 · UserEntity | `UserEntity`, `RefreshEntity` | `UserEntity.java` |
| `UserSchedule` | 학습 모드 (10D/20D/30D) · Budget 매핑 | `UserScheduleConfig` | `UserScheduleConfig.java` |

**BC 간 협력**: 동기 도메인 이벤트 (`ApplicationEventPublisher`, ADR007). 예: `LearningAxisCreatedEvent` → Deck BC 자동 생성.

---

## 3. 4-레이어 패키지 트리 (모든 BC 동일 구조)

```
com.example.thirdtool.{BC}/
├── presentation/                    # HTTP 진입점
│   ├── {Bc}Controller.java          # REST endpoint (@RestController)
│   └── dto/
│       ├── {Bc}Request.java         # 요청 record (@NotBlank·@Size 검증)
│       └── {Bc}Response.java        # 응답 record + of(domain) 팩토리
├── application/                     # 조율 계층
│   ├── dto/
│   │   ├── {Bc}Command.java         # Controller → Service 입력 record (ADR005)
│   │   └── {Bc}Query.java           # 조회 요청 record
│   ├── port/out/                    # 헥사고날 Port (외부 의존 인터페이스)
│   │   └── suggestion/              # AI 제안 4-Port (Story-AS-E1)
│   ├── service/
│   │   ├── {Bc}CommandService.java  # @Transactional · 조율 · 이벤트 발행
│   │   └── {Bc}QueryService.java    # 조회 서비스 (readOnly)
│   └── event/                       # 도메인 이벤트 record
├── domain/                          # 핵심 (외부 의존 0)
│   ├── model/
│   │   ├── {Aggregate}.java         # Aggregate Root
│   │   ├── {Entity}.java            # 자식 Entity
│   │   ├── {VO}.java                # Value Object (record)
│   │   └── {ChangeRecord}.java      # 결과 VO 패턴
│   └── exception/
│       └── {Bc}DomainException.java # BC 전용 예외 (BusinessException 상속)
└── infrastructure/                  # 어댑터 계층
    ├── persistence/
    │   ├── {Aggregate}Repository.java         # Port (interface)
    │   ├── {Aggregate}JpaRepository.java      # Spring Data JPA
    │   └── {Aggregate}RepositoryAdapter.java  # Port 구현체
    ├── dto/
    │   └── {Bc}SummaryRow.java     # QueryDSL 조회 전용
    └── suggestion/                  # 0.0.2v 신설 — AI Static Adapter
        ├── SuggestionCatalog.java
        ├── SuggestionCatalogLoader.java
        └── StaticLayerSuggestionAdapter.java
```

**의존 방향**: `presentation → application → domain`, `infrastructure → domain` (Port 구현).
**금지**: `domain → 다른 레이어`, `infrastructure → application`, `Aggregate → Repository`.

---

## 4. LearningFacade BC의 학습 계층 계층도 (0.0.2v 신설 반영)

```
LearningFacade  (Aggregate Root, softDelete)
├── concepts: List<LearningFacadeConcept>       # 0.0.2v Story-LT-E1 신설 (1~5개, displayOrder 1-based)
│                                                # legacy `concept: String` 컬럼과 동기화 (첫 항목)
├── layers: List<LearningLayer>                  # 0.0.2v Story-LT-E2 신설 (softDelete, @SQLRestriction)
│   │
│   └── axes: List<LearningAxis>                # softDelete, layer_id FK로 재배선 (V19)
│       │                                        # facade_id FK는 backward compat로 유지
│       └── topics: List<AxisTopic>             # displayOrder 1-based
│           ├── materials: List<TopicMaterial>  # 매핑 엔티티
│           └── revisions: List<TopicRevision>  # 이름 변경 이력
│
└── LearningMaterial (별도 도메인, softDelete)   # 자료 · TopicMaterial로 topic과 M:N
    └── ProficiencyLevel                         # UNRATED · UNFAMILIAR · GETTING_USED · MASTERED
```

**0.0.2v 신설 요약**:
- `LearningFacade.concepts[]` — 단일 String에서 1~5개 컬렉션으로 확장. legacy 컬럼 유지.
- `LearningLayer` — Axis 상위 그룹핑 계층. default "Uncategorized" 자동 발행.
- `LearningAxis.layer_id` FK — 3-phase 마이그레이션 (V19) 으로 재배선.

**불변식 예시** (도메인 자기 강제):
- `LearningFacade.MIN_CONCEPT_COUNT=1`, `MAX_CONCEPT_COUNT=5` (`LEARNING_FACADE_CONCEPTS_SIZE_INVALID` 400)
- `LearningLayer` 이름 trim + blank/100자 검증 + facade 내 중복 거부 (`LEARNING_LAYER_DUPLICATE_NAME` 409)
- `default Uncategorized Layer 삭제 금지` — 자동 라우팅의 앵커
- `Layer.softDelete()`는 활성 axis 존재 시 `LEARNING_LAYER_HAS_ACTIVE_AXES` (409) — cascade 자동 금지, 사용자 명시 요구

---

## 5. AI 제안 헥사고날 아키텍처 (0.0.2v 신설)

```
Application Service (조율)
        │
        ▼
┌──────────────────────────────────┐
│  RoleDetector                    │ concepts[] → "backend-developer" / "planner" /
│  (application/service)           │ "designer" / "problem-solver" / "generic"
└──────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────┐
│  4 Port (interface)              │   application/port/out/suggestion/
│  ─ LayerSuggestionPort           │
│  ─ AxisSuggestionPort  (기존)    │
│  ─ RoadmapSuggestionPort         │
│  ─ SelectionsSuggestionPort      │
└──────────────────────────────────┘
        ▲                    ▲
        │                    │
┌───────┴────────┐   ┌───────┴──────────┐
│ StaticLayer... │   │ (미래) LlmLayer.. │
│ Adapter        │   │ Adapter          │
│                │   │                  │
│ classpath JSON │   │ Vertex AI Gemini │
│ + Role Catalog │   │ + Cascade fallback│
└────────────────┘   └──────────────────┘
     @ConditionalOnProperty(thirdtool.suggestion.provider=static, matchIfMissing=true)
```

**교체 지점**:
- Property `thirdtool.suggestion.provider=static|llm|cascade` 로 Adapter 스왑.
- ADR010: AI 호출 실패는 5xx 미노출, 상위 Service가 빈 목록 + `suggestionsAvailable=false`로 변환.

**Catalog 로딩**:
- `resources/ai/catalog/{role}.json` (backend-developer / generic 2종 · 0.0.2v).
- `SuggestionCatalogLoader`가 lazy load + `ConcurrentHashMap` 캐시.
- Role 매칭 실패 → generic 폴백 → 없으면 빈 카탈로그.

---

## 6. 데이터 지속성 (Flyway + JPA)

### 마이그레이션 스택 (V16~V19은 0.0.2v 신설)

| V 버전 | 목적 | 특징 |
| --- | --- | --- |
| V16 | `learning_facade_concept` 테이블 신설 | UNIQUE(facade_id, concept_value), CASCADE FK |
| V17 | 기존 concept 값 → 자식 테이블 백필 | NOT EXISTS 서브쿼리로 idempotent |
| V18 | `learning_layer` 테이블 신설 | UNIQUE(facade_id, name, deleted_at) 3-col composite (ADR021 관행) |
| V19 | `learning_axis.learning_layer_id` FK 재배선 | **3-phase 안전 이관**: ADD NULL → 백필 → NOT NULL + FK RESTRICT |

**3-phase 마이그레이션 (`conventions.md` §3.8)**:
```sql
-- Phase 1: 컬럼 추가 (nullable)
ALTER TABLE learning_axis ADD COLUMN learning_layer_id BIGINT NULL;

-- Phase 2: 백필 (Uncategorized layer per facade + axis 매핑)
INSERT INTO learning_layer (...) SELECT ... WHERE NOT EXISTS (...);
UPDATE learning_axis la JOIN learning_layer ll ... SET la.learning_layer_id = ll.id;

-- Phase 3: NOT NULL 승격 + FK
ALTER TABLE learning_axis MODIFY COLUMN learning_layer_id BIGINT NOT NULL;
ALTER TABLE learning_axis ADD CONSTRAINT fk_learning_axis_layer FOREIGN KEY (...);
```

### JPA 매핑 규칙 (전 프로젝트 통일)

| 규칙 | 근거 |
| --- | --- |
| PK = `BIGINT NOT NULL AUTO_INCREMENT` | ADR001 (단일 RDS, 샤딩 없음) |
| Enum = `@Enumerated(EnumType.STRING)` + CHECK 제약 | ADR002 (ORDINAL 절대 금지) |
| Soft Delete = `deleted_at DATETIME(6) NULL` + `@SQLRestriction` + `@SQLDelete` | ADR003 (사용자 자산성 도메인만 · Layer는 ADR021 확장) |
| UNIQUE = `(facade_id, name, deleted_at)` 3-col composite | MySQL NULL 취급 활용 · softDeleted 이름 재사용 허용 |
| 3-phase 마이그레이션 (nullable → 백필 → NOT NULL) | conventions.md §3.8 |
| audit = JPA Auditing (`@CreatedDate` / `@LastModifiedDate`) 또는 `@CreationTimestamp` | 외부 주입 금지 |

---

## 7. 예외·에러 응답 통일

### 파이프라인

```
Domain Aggregate ──throw──▶ {Bc}DomainException(ErrorCode)
                              │
                              ▼ (Application 통과)
                    ┌─────────────────────┐
                    │  Presentation층       │
                    │  → GlobalException   │  ← Controller에서 try-catch 금지
                    │    Handler            │
                    └─────────┬─────────────┘
                              │
                              ▼
                   HTTP {code, message, ...}
                   (ErrorCode enum에 매핑된 HttpStatus)
```

### ErrorCode 규약

- 이름: `{BC_PREFIX}_{도메인_요소}_{상태}` SCREAMING_SNAKE_CASE
- HTTP: 404 (NOT_FOUND), 400 (검증 실패), 409 (중복·상태 충돌)
- 신규 ErrorCode 등록 절차 (conventions.md §2.5):
  1. ErrorCode enum에 코드 + 메시지 + HttpStatus 등록
  2. 도메인/Service에서 `{Bc}DomainException.of(코드)` throw
- **0.0.2v 신규 등록 11종**:
  - `LF005~LF008`: `LEARNING_FACADE_CONCEPTS_SIZE_INVALID` / `_CONCEPT_DUPLICATE` / `_CONCEPT_TOO_LONG` / `_REORDER_MISMATCH`
  - `LL001~LL007`: `LEARNING_LAYER_NOT_FOUND` / `_NAME_BLANK` / `_DUPLICATE_NAME` / `_ALREADY_DELETED` / `_HAS_ACTIVE_AXES` / `_REORDER_MISMATCH` / `_NAME_TOO_LONG`

---

## 8. 테스트 전략 (계층별)

| 계층 | 전략 | 도구 | 0.0.2v 예시 |
| --- | --- | --- | --- |
| VO · Entity · Aggregate | **Classist** (실 객체) | JUnit5 + AssertJ | `LearningFacadeConceptsTest` (17건), `LearningLayerTest` (12건) |
| Application Service | Repository Mock + 도메인 실 객체 | Mockito | (기존 유지) |
| Repository | `@DataJpaTest` + H2 | Spring Boot Test | (기존 유지) |
| Controller | `@WebMvcTest` + Service Mock | Spring MVC Test | (기존 유지) |
| Port 계약 | stub Adapter로 시그니처 확정 | JUnit5 | `SuggestionPortsContractTest` (15건 · Layer/Roadmap/Selections 각 5) |
| 통합 | `@SpringBootTest` + H2 | | 기존 재활용 |

**해피/엣지/예외 3구분** 필수 (conventions.md §4.2):
- 해피: 정상 입력 → 정상 결과
- 엣지: 경계값·trim·null 정규화·멱등
- 예외: 검증 실패·중복·최소/최대·잘못된 id

**0.0.2v 신규 테스트 97건** — 상세는 `test-coverage.md` / `feature-implementation.md`.

---

## 9. 도메인 패턴 카탈로그 (인터뷰 좋은 소재)

| 패턴 | 예시 | 근거 |
| --- | --- | --- |
| **정적 팩토리 + new 금지** | `LearningLayer.of(facade, name, order)` | 생성자 protected, Aggregate 외부는 팩토리로만 |
| **컬렉션 캡슐화** | `LearningFacade.getConcepts()` → `Collections.unmodifiableList` | 외부에서 add/remove 차단, Aggregate 행위로만 |
| **결과 VO 패턴** | `ConceptsChangeRecord(previous, current, added, removed, kept, isChanged)` | 상태 변경 결과를 명시적으로 표현. Service는 저장 여부 결정 근거 |
| **displayOrder 1-based** | `addConcept()`는 `size+1`, `reorderConcepts(ids)`는 id 집합 일치 검증 후 1..N 재부여 | 도메인 의미와 일치 · DB CHECK는 ≥ 0 안전망 |
| **다건 통째 교체** | `updateConcepts(List<String>)` — 트랜잭션 내 clear + rebuild, 부분성공 불허 | `ConceptsChangeRecord` VO로 added/removed 계산 |
| **멱등 상태 전이** | `Card.archive()` 이미 ARCHIVE면 무시, `Layer.softDelete()` 재호출 시 `ALREADY_DELETED` 예외 | 예외 vs no-op 정책은 도메인별 결정 |
| **이중 방어** | 도메인 검증(친절한 에러) + DB UNIQUE 제약(안전망) | 사용자 노출 UX + 데이터 정합 |
| **`@SQLRestriction` + `@SQLDelete`** | Soft Delete를 JPA/Hibernate 관행으로 통일 | `deleted_at IS NULL` 자동 필터 · Repository 명시 조건 불요 |
| **default 앵커 도메인** | Uncategorized Layer는 삭제 금지 + 자동 발행 | 자동 라우팅 · 백필 · 신규 사용자 진입점 안정 보장 |
| **결과 record + Command/Query 분리** | Presentation Request → Command record → Application Service → 도메인 → Response record | ADR005 · Presentation·Application 경계 명시 |
| **헥사고날 Port + Adapter 스왑** | `LayerSuggestionPort` (interface) → Static / LLM 교체 (`@ConditionalOnProperty`) | 외부 의존 격리 · 테스트 stub 용이 |

---

## 10. 트레이드오프 & 결정 로그 (0.0.2v 착지 후)

### T1. Facade.axes 컬렉션 유지 (SDD의 "compile error" AC 미충족)

- **결정**: `LearningFacade.axes`를 유지 (backward compat). SDD가 원한 "facade.axes 직접 접근 컴파일 오류"는 달성 X.
- **왜**: 기존 테스트·Controller·QueryService가 `facade.getAxes()` 사용 다수. 제거 시 40+ 파일 refactor.
- **트레이드오프**: SDD의 강제 시그널 상실. 미래 리팩토링 시점(별도 릴리스)에 재검토. Layer 도입 이점은 그대로.
- **면접 질문 유형**: "레거시 유지 vs 강제 마이그레이션 트레이드오프를 어떻게 판단했나?"

### T2. Legacy `learning_facade.concept` 컬럼 유지

- **결정**: `concept` NOT NULL 컬럼 유지. `concepts[0]`과 자동 동기화.
- **왜**: 컬럼 DROP은 별도 릴리스 (Flyway 3-phase 원칙). V17 백필로 자식 테이블에도 넣지만 legacy 컬럼도 인정.
- **트레이드오프**: 도메인·DB에 두 표현 공존 (일관성 유지 부담). Adapter로 대응.

### T3. 3-phase 마이그레이션 (V19)

- **결정**: ADD NULL → 백필 → NOT NULL + FK를 한 V 파일 안에 순차 실행.
- **왜**: prod 배포 안전성. V19 한 번의 실행이 완결. 롤백 R19도 순서 준수.
- **면접 질문 유형**: "Flyway로 breaking schema 변경을 어떻게 안전하게 배포하나?"

### T4. Layer.softDelete cascade 금지

- **결정**: 활성 axis가 있는 Layer는 삭제 거부 (`LEARNING_LAYER_HAS_ACTIVE_AXES` 409).
- **왜**: 자동 연쇄 삭제는 대량 데이터 소실 위험. 사용자가 명시적으로 axis 이동/삭제 요구.
- **면접 질문 유형**: "부모 삭제 시 자식 처리 정책 (Cascade / Restrict / Set Null) 어떻게 결정?"

### T5. default Uncategorized Layer 자동 발행

- **결정**: `LearningFacade.create()` 시 자동으로 Uncategorized Layer 1개 생성.
- **왜**: 신규 facade가 백필 결과와 동일 상태. addAxis(name) legacy 경로가 자동 라우팅 가능.
- **트레이드오프**: 사용자가 유일 Layer를 갖고 시작 (UI에서 숨길지 노출할지 별도 결정).

### T6. `@SQLRestriction` + `@SQLDelete` 채택

- **결정**: LearningLayer는 Hibernate 어노테이션으로 Soft Delete 처리. 기존 Card·Deck의 `boolean deleted` 관행과 다름.
- **왜**: 현대 Hibernate 관행 · Repository 명시 필터 코드 감소. conventions.md §3.4가 이 방향 예시 명시.
- **트레이드오프**: 벤더 종속 · 프로젝트 내 두 관행 공존 (ADR021의 미해결 항목 재발). 후속 리팩토링 대상.

### T7. RoleDetector 하드코드 사전 (Regex/Embedding 대신)

- **결정**: `Map<String, Set<String>>`으로 keyword substring 매칭. LinkedHashMap 순서로 첫 매칭 우선.
- **왜**: 초기 4 role 커버에 충분 + 유지보수 쉬움 + 외부 의존 없음.
- **다시 검토 시점**: 사용자 concepts가 5개 role로 부족하거나 매칭 오탐이 UX 문제로 부상 시.

### T8. Static Adapter `@ConditionalOnProperty(matchIfMissing=true)`

- **결정**: 기본값 static. LLM Adapter 도입 전까지 항상 Static 사용.
- **왜**: SDD Epic 2 결정. 로컬·테스트·인증 미비 상태 fallback 자산.
- **교체 방법**: application.yml에 `thirdtool.suggestion.provider=llm` 추가하면 Static Bean 비활성.

---

## 11. "면접에서 물어볼 만한" 질문·답변 가이드

| Q | A 요지 |
| --- | --- |
| DDD Aggregate 경계를 어떻게 정했나? | LearningFacade가 concepts/layers/axes/topics를 다 소유 (단일 트랜잭션 · 소유권 검증 · Aggregate Root). 별도 BC 승격은 응집도 부족. Card/Deck/Review는 별도 BC (독립 lifecycle) |
| Layer를 별도 BC로 안 만든 이유는? | Layer는 Facade의 자식이며 lifecycle이 facade에 종속. 별도 BC로 승격할 응집도 부족. `docs/PACKAGE.md`·SDD Epic 2 명시 |
| Soft Delete를 왜 어떤 도메인에만 적용했나? | ADR003: 사용자 자산성 도메인만 (Card/Deck/Facade/Material/Axis/Layer). 매핑 엔티티(TopicMaterial, CardTag)는 X. 복원 요구·데이터 가치로 판단 |
| Enum을 왜 VARCHAR + CHECK로 저장하나? | ADR002: ORDINAL은 enum 순서 바뀌면 데이터 조용히 오염. STRING + CHECK 제약이 안전. 성능 차이 무의미 |
| BC 간 협력은 어떻게 하나? | 동기 도메인 이벤트 (ApplicationEventPublisher, ADR007). 예: LearningAxisCreatedEvent → Deck 자동 생성. 비동기는 원칙적으로 X |
| 다건 통째 교체를 왜 clear + rebuild로? | 부분성공 불허 · orphanRemoval=true가 detach된 자식 자동 삭제. 트랜잭션 내 원자성 · 순서 재부여 · added/removed 계산 명확 |
| N+1 문제 어떻게 감지·해결하나? | Repository fetch join · @EntityGraph · QueryDSL projection. 조회 전용 DTO(SummaryRow)로 계층 flat화. 통합 테스트 실행 시 SQL count 관찰 |
| Hexagonal Architecture를 왜 도입? | 외부 의존 (AI · DB · 파일)을 도메인에서 격리. Port interface + Adapter 구현. 테스트 stub 용이 · Adapter 교체 가능 (Static → LLM) |
| 3-phase 마이그레이션의 근거는? | conventions.md §3.8. 컬럼 추가 + NOT NULL 전환은 (1) ADD NULL → (2) 백필 → (3) MODIFY NOT NULL. 백필 실패 시 부팅 실패 방지. V19가 이 패턴 실증 |
| ErrorCode enum + GlobalExceptionHandler의 장점? | (1) HTTP 응답 형식 통일 (`{code, message}`) (2) 도메인은 HTTP 몰라도 됨 (3) 코드-메시지-HttpStatus 매핑 단일 진실 (4) Controller에서 try-catch 금지 |
| RoleDetector가 왜 LinkedHashMap? | 다중 role 매칭 시 첫 매칭이 우선 (예상 가능한 순서). HashMap은 매칭 순서 예측 불가 |
| Static AI Adapter 왜 @ConditionalOnProperty? | LLM Adapter 도입 시 property로 교체. Bean 자체를 조건부 등록해 명시성·재현성 확보. `matchIfMissing=true`로 기본값 안전 (개발자가 잊어도 static 동작) |
| ADR을 왜 문서로 남기나? | 코드는 "무엇을"만 남기고 "왜"는 남기지 않음. 결정의 대안·트레이드오프·재검토 시점을 명시. ADR023의 Roadmap=헌법 / Selection=판례 결정 예시 |

---

## 12. 참조

- 규범: `docs/PACKAGE.md` (BC 의존 규칙) · `docs/DOMAIN.md` (Ubiquitous Language 38 · BC별 의도) · `.claude/rules/conventions.md`
- ADR 인덱스: `docs/adr/index.md` (ADR001~ADR023)
- 산출물 상세: `feature-implementation.md` (Story 진척) · `outcome.md` (성과) · `test-coverage.md` (테스트 매트릭스) · `troubleshooting.md` (막힘 로그) · `review.md` (회고)
- API: `/swagger-ui.html` (실행 후) — 코드가 진실 소스

*작성일: 2026-07-02 | 반영 범위: PR #195~#200 착지 후 · 0.0.3v 급행 종료 시점 | 규범 갱신 시 즉시 반영*
