# Package & Architecture — third-tool

> **백엔드 코드의 패키지·레이어·BC 간 의존 규칙**의 단일 진실 소스. 새 BC·기능을 어디에 둘지, 의존 방향이 정합한지 결정할 때 참조한다.
> 도메인 의도는 `docs/DOMAIN.md`, 결정의 영속 기록은 `docs/adr/`.

---

## 1. 전체 구조

```
com.example.thirdtool
├── ThirdToolApplication.java
│
├── Card/                     # Bounded Context — 학습 카드
├── Deck/                     # Bounded Context — 카드 분류 컨테이너
├── Review/                   # Bounded Context — 리뷰 세션
├── LearningFacade/           # Bounded Context — 학습 목표·자료
├── User/                     # Bounded Context — 인증·소셜 로그인
├── UserSchedule/             # Bounded Context — 학습 모드 설정
│
├── Common/                   # BC 횡단 공통 (BC 아님)
├── infra/                    # BC 횡단 인프라 어댑터 (BC 아님)
│
├── Health/                   # 헬스 체크 (BC 아님, 단일 컨트롤러)
├── Dashboard/                # (골격만, 도입 보류)
├── Notification/             # (빈 디렉토리, v2)
└── RemindSearch/             # (빈 디렉토리, v2 — 검색 인프라는 운영 중)
```

신규 BC를 추가할 때는 §2의 4-레이어 구조를 그대로 따른다. 빈 BC 디렉토리는 의도 모호를 유발하므로 도입 시점에 생성한다.

---

## 2. BC 4-레이어 구조 (표준)

모든 BC는 동일한 4-레이어로 분리한다.

```
{BC}/
├── presentation/
│   ├── dto/                  # Request / Response DTO
│   └── {BC}Controller.java
├── application/
│   ├── dto/                  # Command / Query record (ADR005)
│   └── service/              # CommandService / QueryService 분리
├── domain/
│   ├── model/                # Aggregate Root · Entity · VO · Enum · Domain Service
│   └── exception/            # BC 전용 도메인 예외
└── infrastructure/
    ├── dto/                  # QueryDSL 조회 전용 DTO (SearchCondition, SummaryRow)
    └── persistence/          # Repository Port + Adapter + JPA + QueryDSL Custom
```

**레거시 예외 (점진 통일 대상)**:
- `Deck/infrastructure/repository/` (다른 BC는 `persistence/`)
- `User/dto/` (BC 루트에 위치, 표준은 `presentation/dto/`)

신규 코드는 표준을 따른다. 기존 레거시는 별도 리팩토링 Story로 분리.

---

## 3. 레이어 의존 방향 (단방향 강제)

```
presentation  ──►  application  ──►  domain
                       │
                       └──►  infrastructure
                             (Port 인터페이스는 application/infrastructure에 선언,
                              Adapter는 infrastructure에서 구현)
```

- **domain은 다른 레이어를 import하지 않는다** — 외부에서 호출되는 순수 자바 객체.
- **application은 presentation을 import하지 않는다** — Request DTO 누수 차단 (ADR005).
- **application은 infrastructure의 Port 인터페이스에만 의존**한다 — JPA 어노테이션·QueryDSL 구체 타입은 infrastructure 안에 가둔다.
- **infrastructure는 application의 Port를 implements**한다 (DI 컨테이너 주입).
- **Common·infra는 어떤 BC도 import하지 않는다** — 단방향 의존 (BC → Common).

---

## 4. Controller ↔ Service 경계 (ADR005)

`refactor/controller-service-command-query` 이후 BC 공통 표준.

### 4.1 application/dto/ 네임스페이스

각 BC는 `application/dto/`에 Command/Query record 묶음을 둔다.

```
{BC}/application/dto/
├── {BC}Command.java       # nested record: Create*, Update*, Add*, Remove*, Reorder*, Link*, Unlink*, Delete*
└── {BC}Query.java         # nested record: Get*, Find*, List* (인자 없는 조회도 빈 marker record)
```

LearningFacade BC 사례 (총 21 record, Controller 21 엔드포인트와 매핑):
- `LearningFacadeCommand` — CreateFacade / UpdateConcept / AddAxis / UpdateAxisName / RemoveAxis / ReorderAxes / AddTopic / UpdateTopic / RemoveTopic / ReorderTopics (10)
- `LearningFacadeQuery` — GetFacade / GetTopicRevisions / GetTopicDeletions / GetActiveReasonOptions (4)
- `LearningMaterialCommand` — CreateMaterial / UpdateMaterialName / UpdateProficiency / LinkTopic / UnlinkTopic / DeleteMaterial (6)
- `LearningMaterialQuery` — GetMaterials (1)

### 4.2 Service 시그니처 규칙

- 모든 Application Service public 메서드는 **`(Command)` 또는 `(Query)` record 1개**만 인자로 받는다.
- `userId` / `axisId` / `topicId` / `materialId` 등 path/auth 값은 record 필드로 포함.
- 인자 없는 조회도 marker record(`GetActiveReasonOptions()`)를 만들어 시그니처 통일을 유지.
- **예외**: `LearningFacadeCommand.CreateFacade(UserEntity user, String concept)` — `LearningFacade.create(UserEntity, String)`가 FK 엔티티를 직접 요구해 `Long userId` 대신 `UserEntity` 보유.

### 4.3 Controller 명시적 매핑

```java
@PatchMapping("/axes/{axisId}")
public LearningFacadeResponse.UpdateAxisName updateAxisName(
        @AuthenticationPrincipal UserEntity user,
        @PathVariable Long axisId,
        @Valid @RequestBody LearningFacadeRequest.UpdateAxisName request) {
    return facadeCommandService.updateAxisName(
            new LearningFacadeCommand.UpdateAxisName(user.getId(), axisId, request.name()));
}
```

- `@AuthenticationPrincipal UserEntity user` → `user.getId()`로 변환 후 record에 주입.
- presentation의 Request DTO는 Controller 안에서만 풀어 record로 다시 묶는다.
- 검증 게이트: `grep "presentation.dto" application/dto/` → **0건**.

### 4.4 후속 작업 (별도 PR)

- Service **반환 타입**은 현재 `presentation.dto.*Response`를 그대로 사용 중 — application → presentation 응답 누수가 남아 있다.
- 다음 refactor PR에서 `Result` record로 분리해 `application/dto/`로 이전 (ADR005 §결과 참조).

---

## 5. BC별 디렉토리 매핑 (요약)

각 BC의 핵심 클래스만 1회 명시. 전체 트리는 코드 디렉토리(`src/main/java/com/example/thirdtool/{BC}/`)에서 직접 확인.

### Card
- `domain/model/` — `Card`(AR), `MainNote`(VO), `Summary`(VO), `KeywordCue`, `Tag`, `CardTag`, `CardStatus`(E), `MainContentType`(E), `CardStatusHistory`, `CardStatusHistoryAppender`(Service), `CardRelationFinder`(Service), `RelatedCardCandidate`(VO)
- `application/service/` — `CardCommandService`, `CardQueryService`
- `infrastructure/persistence/` — `CardRepository`(Port) + `CardRepositoryAdapter`(Adapter) + `CardJpaRepository` + `CardRepositoryCustom`/`CardJpaRepositoryImpl`(QueryDSL) + `CardStatusHistoryRepository`/`CardStatusHistoryJpaRepository`
- `infrastructure/dto/` — `CardSearchCondition`, `CardSummaryRow`

### Deck
- `domain/model/` — `Deck`(AR, self-reference), `DeckMode`(E)
- `application/service/` — `DeckCommandService`, `DeckQueryService`, `DeckHierarchyService` (depth 재귀 재계산 전용)
- `infrastructure/repository/` — `DeckRepository`(Port), `DeckQueryRepository`(QueryDSL Custom) **← 표준은 `persistence/`, 레거시**

### Review
- `domain/model/` — `ReviewSession`(AR), `CardReview`, `CardVisibleContent`(VO/record), `ReviewStep`(E)
- `application/` — `ReviewCommandService`, `ReviewQueryService`
- `infrastructure/` — `ReviewSessionRepository`(Port), `ReviewSessionRepositoryCustom`, `ReviewSessionRepositoryImpl`

### LearningFacade
- `domain/model/` — `LearningFacade`(AR), `LearningAxis`, `AxisTopic`, `LearningMaterial`, `TopicMaterial`, `MaterialType`(E), `ProficiencyLevel`(E), `CoverageStatus`(E), `CoverageSummary`(VO), `TopicRevision`, `RevisionReasonOption`, `TopicDeletionRecord`, `ConceptChangeRecord`(VO)
- `application/dto/` — `LearningFacadeCommand`, `LearningFacadeQuery`, `LearningMaterialCommand`, `LearningMaterialQuery` (21 record)
- `application/service/` — `LearningFacadeCommandService`, `LearningFacadeQueryService`, `LearningMaterialCommandService`, `TopicRevisionQueryService`, `CoverageRecalculator`(Domain Service)
- `infrastructure/persistence/` — Repository Port + Adapter 다수 (LearningFacade/AxisTopic/LearningMaterial/TopicMaterial/TopicRevision/RevisionReasonOption/TopicDeletionRecord 각각)

### User
- `domain/model/` — `UserEntity`(AR), `SocialMember`(Abstract Entity), `KakaoMember`, `NaverMember`, `CustomOAuth2User`(Spring 어댑터), `SocialProviderType`(E), `UserRoleType`(E)
- `domain/repository/` — `UserRepository`(Port) **← 표준은 `infrastructure/persistence/`, 레거시**
- `application/` — `UserService`
- `dto/` (BC 루트) — Request/Response DTO 다수 **← 표준은 `presentation/dto/`, 레거시**

### UserSchedule
- `domain/model/` — `UserScheduleConfig`(AR), `LearningMode`(E), `LearningModeMappingPolicy`(Domain Service), `UserScheduleConfigHistory`
- `application/` — UserScheduleConfig 관련 CommandService / QueryService

### Common (BC 아님)
- `config/` — MvcConfig, QuerydslConfig, S3Config, SecurityConfig, SwaggerConfig, WebClientConfig
- `Exception/` — `BusinessException`, `GlobalExceptionHandler`, `ErrorCode/ErrorCode`(Enum, 전 BC 에러 코드 등록처)
- `init/` — 초기 데이터 로딩
- `security/` — `auth/jwt/{JwtController, JwtService, RefreshEntity, RefreshRepository}`, `filter/JWTFilter`
- `Util/` — `JWTUtil`, `UUIDUtil`, `mapper/`
- `BaseEntity` — JPA Auditing 공통

### infra (BC 아님)
- `S3/S3StorageAdapter` — AWS S3 어댑터
- `adapter/FileStoragePort` — Port 인터페이스
- `Ai/presentation/` — AI 관련 (사용 중)
- `redis/` — 빈 디렉토리 (인프라만 docker-compose에 존재)

---

## 6. BC 간 의존 규칙

| 의존 방향 | 허용 여부 | 비고 |
| --- | --- | --- |
| `LearningFacade → User` (`UserEntity` 참조) | ✅ | `LearningFacade.create(UserEntity, ...)` FK 매핑 |
| `Card → User` (`UserEntity` 참조) | ✅ | Card가 소유 유저 보유 |
| `Card → Deck` (`Deck` 참조) | ✅ | Card는 Deck 소속 |
| `Deck → User` (`UserEntity` 참조) | ✅ | Deck이 소유 유저 보유 |
| `Review → Card, Deck` | ✅ | ReviewSession이 카드를 순회 (ID 참조 권장, Aggregate 직접 포함 지양) |
| `BC ↔ BC` 양방향 의존 | ❌ | 사이클 → 양쪽 분리 또는 Domain Event 도입 검토 |
| `Common → 특정 BC` | ❌ | Common은 BC 비의존 |
| `infra → 특정 BC` | ❌ | infra는 어댑터, BC 비의존 |

도메인 객체(Entity)가 다른 BC의 도메인 객체를 직접 참조할 수 있으나, **다른 BC의 Repository·Service를 직접 호출하지 않는다**. 협력이 필요하면 application 레이어에서 양쪽 Service를 조율한다.

**Cross-BC 조회는 ID 참조 기본** — Application Service에서 각 BC Repository를 조합한다.

---

## 7. 리소스 / 테스트 구조

```
src/main/resources/
├── db/migration/V*.sql      # Flyway (V1__init ~ V6__learning_material_type_extension)
├── templates/               # Thymeleaf (사용처 미식별)
└── application.yml          # dev/prod 프로필 분리

src/main/generated/          # QueryDSL Q클래스 자동 생성 (gradle clean 시 삭제)

src/test/java/com/example/thirdtool/
├── {BC}/                    # BC 미러링 (domain·application·infrastructure 패키지 동일)
└── support/                 # 테스트 공통 픽스처 (있다면)
```

### 테스트 위치 규칙

| 대상 | 위치 | 전략 |
| --- | --- | --- |
| Aggregate / Entity / VO / Domain Service | `{BC}/domain/model/` | Classist (실제 인스턴스) |
| Application Service | `{BC}/application/service/` | Mockist (Repository Mock) |
| Repository | `{BC}/infrastructure/persistence/` | `@DataJpaTest` 슬라이스 |
| Controller | `{BC}/presentation/` | `@WebMvcTest` 슬라이스 |
| 통합 (다중 BC 협력) | 별도 패키지 또는 BC 안 | `@SpringBootTest` |

---

## 8. 결정 근거 (ADR 인용)

| 결정 | ADR |
| --- | --- |
| Surrogate PK는 BIGINT AUTO_INCREMENT | [ADR001](adr/ADR001.md) |
| Enum 컬럼은 VARCHAR + CHECK 제약 | [ADR002](adr/ADR002.md) |
| Soft Delete는 사용자 자산성 도메인에만 선택 적용 | [ADR003](adr/ADR003.md) |
| 학습 주제는 명사구 표현 (AxisAction → AxisTopic) | [ADR004](adr/ADR004.md) |
| Controller↔Service 경계에 Command/Query record 도입 | [ADR005](adr/ADR005.md) |

---

## 9. 변경 이력

| 버전 | 날짜 | 변경 내용 |
| --- | --- | --- |
| v0.1 | 2026-05-14 | 신설. 기존 `docs/architecture/package-structure-guide.md`(LearningFacade/UserSchedule BC 누락 상태) + `update-docs/architecture/application.md`(Controller↔Service 경계 패턴)을 흡수·통합. 표준 4-레이어 구조 + ADR005 Command/Query record 패턴 + BC 간 의존 규칙 + 테스트 위치 규칙 정착. 레거시 디렉토리 명명(Deck `repository/`, User BC 루트 `dto/`) 점진 통일 대상 명시 |
