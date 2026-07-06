# Backend Architecture (Living)

> **성격**: living-docs — 항상 최신본. "지금 이 코드베이스의 백엔드 아키텍처는 이렇게 생겼다".
> **성장 방향**: 파일이 커지면 레이어별(`architecture/layers.md`) 또는 BC별(`architecture/learning-facade.md` …)로 분화.
> **관련 규범**: `docs/PACKAGE.md`(BC·의존 규칙) · `docs/DOMAIN.md`(도메인 의도) · `.claude/rules/conventions.md`.
> **관련 topology**: `workflow/topologys/versions/{Nv}/bounded-context.md`(BC 위상 pin).

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
                        Flyway V1~V22 (+ R1~R19 rollback)
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
│   │   └── suggestion/              # AI 제안 4-Port
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
    └── suggestion/                  # AI Static Adapter
        ├── SuggestionCatalog.java
        ├── SuggestionCatalogLoader.java
        └── StaticLayerSuggestionAdapter.java
```

**의존 방향**: `presentation → application → domain`, `infrastructure → domain` (Port 구현).
**금지**: `domain → 다른 레이어`, `infrastructure → application`, `Aggregate → Repository`.

---

## 4. LearningFacade BC의 학습 계층도

```
LearningFacade  (Aggregate Root, softDelete)
├── concepts: List<LearningFacadeConcept>       # 1~5개, displayOrder 1-based
│                                                # legacy `concept: String` 컬럼과 동기화 (첫 항목)
├── layers: List<LearningLayer>                  # softDelete, @SQLRestriction
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

**불변식 예시** (도메인 자기 강제):
- `LearningFacade.MIN_CONCEPT_COUNT=1`, `MAX_CONCEPT_COUNT=5` (`LEARNING_FACADE_CONCEPTS_SIZE_INVALID` 400)
- `LearningLayer` 이름 trim + blank/100자 검증 + facade 내 중복 거부 (`LEARNING_LAYER_DUPLICATE_NAME` 409)
- `default Uncategorized Layer 삭제 금지` — 자동 라우팅의 앵커
- `Layer.softDelete()`는 활성 axis 존재 시 `LEARNING_LAYER_HAS_ACTIVE_AXES` (409) — cascade 자동 금지, 사용자 명시 요구

---

## 5. AI 제안 헥사고날 아키텍처

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
│  ─ AxisSuggestionPort            │
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
- `resources/ai/catalog/{role}.json` (backend-developer / generic).
- `SuggestionCatalogLoader`가 lazy load + `ConcurrentHashMap` 캐시.
- Role 매칭 실패 → generic 폴백 → 없으면 빈 카탈로그.

---

## 6. 데이터 지속성 (Flyway + JPA)

Flyway 진행 상태: V1 ~ V22 (본 문서 작성 시점).

**주요 마이그레이션 스택**:

| V 버전 | 목적 |
| --- | --- |
| V1 | 초기 — user_entity, social_member, deck, card, keyword_cue, jwt_refresh |
| V2 | learning_facade, learning_axis, axis_topic, learning_material, topic_material |
| V3 | topic_revision + revision_reason_option |
| V4~V6 | axis_topic revision count · axis_topic 삭제 · learning_material type 확장 |
| V7~V8 | deck_axis_material_link · deck_progress_status |
| V9~V11 | card_status_history · card_status_budget · tag/card_tag |
| V12~V14 | user_schedule_config · daily_target · learning_axis soft delete |
| V15 | deck_axis_not_null + orphan soft delete |
| V16~V17 | learning_facade_concept 신설 + 백필 |
| V18 | learning_layer 신설 (soft delete + composite unique) |
| V19 | learning_axis.layer_id FK 재배선 (3-phase) |
| V20~V22 | axis_roadmap_node, axis_selection, axis_selection_node |

**3-phase 마이그레이션 예시 (V19)**:
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

**JPA 매핑 규칙** (전 프로젝트 통일):

| 규칙 | 근거 |
| --- | --- |
| PK = `BIGINT NOT NULL AUTO_INCREMENT` | ADR001 (단일 RDS, 샤딩 없음) |
| Enum = `@Enumerated(EnumType.STRING)` + CHECK 제약 | ADR002 (ORDINAL 절대 금지) |
| Soft Delete = `deleted_at DATETIME(6) NULL` + `@SQLRestriction` + `@SQLDelete` | ADR003 (사용자 자산성 도메인만 · Layer는 ADR021 확장) |
| UNIQUE = `(facade_id, name, deleted_at)` 3-col composite | MySQL NULL 취급 활용 · softDeleted 이름 재사용 허용 |
| 3-phase 마이그레이션 (nullable → 백필 → NOT NULL) | conventions.md §3.8 |
| audit = JPA Auditing (`@CreatedDate` / `@LastModifiedDate`) 또는 `@CreationTimestamp` | 외부 주입 금지 |

---

## 7. 예외·에러 응답 파이프라인

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

**ErrorCode 규약**:
- 이름: `{BC_PREFIX}_{도메인_요소}_{상태}` SCREAMING_SNAKE_CASE
- HTTP: 404 (NOT_FOUND), 400 (검증 실패), 409 (중복·상태 충돌)
- 신규 ErrorCode 등록 절차 (conventions.md §2.5):
  1. ErrorCode enum에 코드 + 메시지 + HttpStatus 등록
  2. 도메인/Service에서 `{Bc}DomainException.of(코드)` throw
- 전체 카탈로그: `workflow/living-docs/error-code/error-code.md`

---

## 8. 테스트 전략 (계층별)

| 계층 | 전략 | 도구 |
| --- | --- | --- |
| VO · Entity · Aggregate | **Classist** (실 객체) | JUnit5 + AssertJ |
| Application Service | Repository Mock + 도메인 실 객체 | Mockito |
| Repository | `@DataJpaTest` + H2 | Spring Boot Test |
| Controller | `@WebMvcTest` + Service Mock | Spring MVC Test |
| Port 계약 | stub Adapter로 시그니처 확정 | JUnit5 |
| 통합 | `@SpringBootTest` + H2 | |

**해피/엣지/예외 3구분** 필수 (conventions.md §4.2):
- 해피: 정상 입력 → 정상 결과
- 엣지: 경계값·trim·null 정규화·멱등
- 예외: 검증 실패·중복·최소/최대·잘못된 id

---

## 9. 도메인 패턴 카탈로그

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

## 10. 참조

- 규범: `docs/PACKAGE.md`(BC 의존 규칙) · `docs/DOMAIN.md`(Ubiquitous Language · BC별 의도) · `.claude/rules/conventions.md`
- ADR 인덱스: `docs/adr/index.md`
- 관련 living-docs: `erd/`, `api-spec/`, `error-code/`, `infra-map/`
- 관련 topology: `workflow/topologys/versions/{Nv}/bounded-context.md`, `persistence.md`, `common-core.md`
- API: `/swagger-ui.html` (실행 후) — 코드가 진실 소스

*최신 갱신: 2026-07-03 (0.0.2v/architecture.md 시드 승격) · Flyway V22 반영*
