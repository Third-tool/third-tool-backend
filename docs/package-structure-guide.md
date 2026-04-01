# Third Tool — 패키지 구조 문서

> 경계 설계 및 BC 간 책임 분리 기준을 지속 관리하기 위한 레퍼런스 문서.
> 패키지 추가·이동·분리 시 이 문서를 먼저 업데이트한다.

---

## 전체 구조 개요
```
com.example.thirdtool
├── ThirdToolApplication.java
│
├── Card/                     # 학습 카드 BC
├── Deck/                     # 덱 관리 BC
├── Review/                   # 리뷰 세션 BC
├── User/                     # 사용자 BC
├── RemindSearch/             # 재탐색 BC (진행 예정)
├── Notification/             # 알림 BC (진행 예정)
├── Health/                   # 헬스체크 BC
├── Dashboard/                # 대시보드 BC (진행 예정)
└── Common/                   # 공통 인프라 (BC 아님)
```

---

## BC별 내부 구조 규칙

모든 BC는 아래 레이어 구조를 따른다.
```
{BC}/
├── application/
│   └── service/          # UseCase 단위 서비스 (Command / Query 분리)
├── domain/
│   ├── exception/        # BC 전용 도메인 예외
│   └── model/            # Aggregate, Entity, VO, Enum, Domain Service
├── infrastructure/
│   ├── dto/              # QueryDSL 조회 전용 DTO (SearchCondition, SummaryRow)
│   └── persistence/      # JPA Repository, Adapter, Custom 구현체
└── presentation/
    ├── dto/              # Request / Response DTO
    └── {BC}Controller
```

---

<details>
<summary><strong>Card BC</strong> — 회상 가능한 학습 단위의 생성·수정·상태 전환·관계 탐색</summary>
```
Card/
├── application/
│   └── service/
│       ├── CardCommandService
│       └── CardQueryService
├── domain/
│   ├── exception/
│   │   └── CardDomainException
│   └── model/
│       ├── Card                         # Aggregate Root
│       ├── MainNote                     # Value Object
│       ├── Summary                      # Value Object
│       ├── KeywordCue                   # Entity
│       ├── Tag                          # Entity
│       ├── CardTag                      # Entity (연결 테이블 명시화)
│       ├── CardStatus                   # Enum  — ON_FIELD / ARCHIVE
│       ├── MainContentType              # Enum  — TEXT_ONLY / IMAGE_ONLY / MIXED
│       ├── CardStatusHistory            # Entity
│       ├── CardStatusHistoryAppender    # Domain Service
│       ├── CardRelationFinder           # Domain Service
│       └── RelatedCardCandidate         # Value Object
├── infrastructure/
│   ├── dto/
│   │   ├── CardSearchCondition          # QueryDSL 동적 조건
│   │   └── CardSummaryRow               # 목록 조회 Projection
│   └── persistence/
│       ├── CardRepository               # Port (interface)
│       ├── CardRepositoryAdapter        # Adapter (Port 구현)
│       ├── CardJpaRepository            # Spring Data JPA (interface)
│       ├── CardJpaRepositoryImpl        # QueryDSL Custom 구현체
│       ├── CardRepositoryCustom         # QueryDSL Custom (interface)
│       ├── CardStatusHistoryRepository  # Port (interface)
│       └── CardStatusHistoryJpaRepository
└── presentation/
    ├── dto/
    └── CardController
```

### 경계 원칙

| 구분 | 내용 |
|------|------|
| **Aggregate 경계** | Card가 MainNote / KeywordCue / Summary / Tag를 포함하는 단일 Aggregate |
| **이력 분리** | CardStatusHistory는 Card와 별도 Entity. Card 삭제 시만 cascade |
| **도메인 서비스** | CardStatusHistoryAppender, CardRelationFinder는 model 패키지에 위치 |
| **Repository Port** | CardRepository (interface) ↔ CardRepositoryAdapter (구현). 도메인이 JPA에 직접 의존하지 않음 |
| **QueryDSL 조건** | CardSearchCondition은 infrastructure/dto에만 위치. domain 레이어에 노출하지 않음 |

</details>

---

<details>
<summary><strong>Deck BC</strong> — 카드를 주제 단위로 묶는 덱의 생성·계층 관리·조회</summary>
```
Deck/
├── application/
│   └── service/
│       ├── DeckCommandService
│       ├── DeckQueryService
│       └── DeckHierarchyService      # depth 재귀 재계산 전용 서비스
├── domain/
│   └── model/
│       ├── Deck                      # Aggregate Root (self-reference)
│       └── DeckMode                  # Enum
├── infrastructure/
│   ├── dto/
│   │   ├── DeckSearchCondition
│   │   └── DeckSummaryRow
│   └── repository/
│       ├── DeckRepository            # Port (interface)
│       └── DeckQueryRepository       # QueryDSL Custom
└── presentation/
    ├── dto/
    │   ├── DeckRequest
    │   └── DeckResponse
    └── DeckController
```

### 경계 원칙

| 구분 | 내용 |
|------|------|
| **계층 책임** | depth 재계산은 Deck Aggregate 내부에서 수행. 하위 덱 재귀 갱신은 DeckHierarchyService 담당 |
| **서비스 분리** | Command / Query / Hierarchy 3분리로 단일 책임 유지 |
| **Repository 위치** | Card BC와 달리 infrastructure/repository 로 명명 (persistence → repository). 향후 통일 검토 필요 |

</details>

---

<details>
<summary><strong>Review BC</strong> — ReviewSession 생성·진행·종료 및 카드 노출 흐름 관리</summary>
```
Review/
├── application/
│   ├── ReviewCommandService
│   └── ReviewQueryService
├── domain/
│   ├── exception/
│   │   └── ReviewSessionException
│   └── model/
│       ├── ReviewSession             # Aggregate Root
│       ├── CardReview                # Entity — 세션 내 카드별 리뷰 기록
│       ├── CardVisibleContent        # Record — 카드 노출 시 렌더링 데이터
│       └── ReviewStep                # Enum — 리뷰 단계
├── infrastructure/
│   ├── dto/
│   │   ├── ReviewSessionSearchCondition
│   │   └── ReviewSessionSummaryRow
│   ├── ReviewSessionRepository          # Port (interface)
│   ├── ReviewSessionRepositoryCustom    # QueryDSL Custom (interface)
│   └── ReviewSessionRepositoryImpl      # 구현체
└── presentation/
    ├── dto/
    │   ├── ReviewRequest
    │   └── ReviewResponse
    └── ReviewController
```

### 경계 원칙

| 구분 | 내용 |
|------|------|
| **Card 의존** | ReviewSession은 Card ID를 참조하되 Card Aggregate를 직접 포함하지 않는다 |
| **viewCount 증가** | ReviewSession 진행 중 `Card.incrementViewCount()` 호출 책임은 ReviewCommandService가 가진다 |
| **CardVisibleContent** | Record 타입. 도메인 모델이지만 렌더링 목적의 읽기 전용 값 객체 |

</details>

---

<details>
<summary><strong>User BC</strong> — 소셜 로그인 기반 사용자 가입·인증·정보 관리</summary>
```
User/
├── application/
│   └── UserService
├── domain/
│   ├── model/
│   │   ├── UserEntity
│   │   ├── SocialMember
│   │   ├── CustomOAuth2User
│   │   ├── SocialProviderType        # Enum
│   │   └── UserRoleType              # Enum
│   └── repository/
│       └── UserRepository            # Port (interface)
├── dto/                              # ⚠️ application 레이어 미분리 상태
│   ├── LoginRequestDTO
│   ├── TokenResponse
│   ├── UserDeleteRequestDTO
│   ├── UserExistRequestDTO
│   ├── UserRequestDTO
│   ├── UserResponseDTO
│   ├── UserSignUpRequestDTO
│   └── UserUpdateRequestDTO
└── infrastructure/
```

### 경계 원칙

| 구분 | 내용 |
|------|------|
| **DTO 위치 개선 필요** | 현재 dto/가 BC 루트에 위치. presentation/dto/ 로 이동 검토 |
| **인증 책임 분리** | JWT 발급·갱신은 Common/security/auth 에 위임. UserService는 유저 도메인 로직만 담당 |

</details>

---

<details>
<summary><strong>Common</strong> — BC가 공유하는 인프라 설정, 보안, 예외 처리, 유틸 (BC 아님)</summary>
```
Common/
├── config/
│   ├── MvcConfig
│   ├── QuerydslConfig
│   ├── S3Config
│   ├── SecurityConfig
│   ├── SwaggerConfig
│   └── WebClientConfig
├── Exception/
│   ├── ErrorCode/
│   │   └── ErrorCode                 # Enum
│   ├── BusinessException
│   └── GlobalExceptionHandler
├── init/                             # 초기 데이터 로딩
├── security/
│   ├── auth/
│   │   ├── dto/
│   │   │   ├── JWTResponseDTO
│   │   │   └── RefreshRequestDTO
│   │   └── jwt/
│   │       ├── JwtController
│   │       ├── JwtService
│   │       ├── TestController
│   │       ├── RefreshEntity
│   │       └── RefreshRepository
│   └── filter/
│       └── JWTFilter
└── Util/
    ├── mapper/
    ├── JWTUtil
    └── UUIDUtil
```

### 경계 원칙

| 구분 | 내용 |
|------|------|
| **BC 의존 금지** | Common은 어떤 BC도 import하지 않는다 |
| **BC → Common** | 단방향 의존만 허용. Common → BC 의존은 금지 |
| **예외 계층** | BC 전용 예외는 각 BC/domain/exception에 위치. Common/Exception은 범용 예외만 |
| **RefreshEntity 위치** | 현재 Common/security에 위치. User BC로 이동 검토 여지 있음 |

</details>

---

## BC 간 의존 방향
```
Card  ←─── Review        (ReviewSession이 Card ID 참조)
Card  ←─── Deck          (Deck이 Card를 포함)
User  ←─── Card          (Card가 UserEntity 참조)
User  ←─── Deck          (Deck이 UserEntity 참조)

Common ←─── 모든 BC      (단방향)
```

> **원칙**: BC 간 직접 객체 참조 대신 ID 참조를 기본으로 한다.
> Cross-BC 조회가 필요하면 Application Service에서 각 BC Repository를 조합한다.

---

## 리소스 및 테스트
```
resources/
├── db/
│   └── migration/
│       └── V1__init.sql              # Flyway 마이그레이션
├── templates/
└── application.yml

test/
└── java/com/example/thirdtool/
    ├── Card/
    ├── Common/
    ├── Deck/
    ├── Review/
    ├── support/                      # 테스트 공통 지원 (fixture, base class 등)
    └── ThirdToolApplicationTests
```

### 테스트 원칙

| 대상 | 전략 |
|------|------|
| **domain/model** | Classist — 실제 인스턴스, Mock 최소화 |
| **infrastructure/persistence** | `@DataJpaTest` 슬라이스 테스트 |
| **application/service** | 선택적 Mockist — 외부 의존만 Mock |
| **presentation** | `@WebMvcTest` 슬라이스 테스트 |
| **support/** | TestFixture 팩토리, BaseIntegrationTest 등 공용 지원 클래스 |

---

## 패키지 추가 체크리스트

새 클래스·패키지 추가 시 아래를 확인한다.

- [ ] 어느 BC에 속하는가? 또는 Common인가?
- [ ] domain / application / infrastructure / presentation 중 어느 레이어인가?
- [ ] BC 간 의존 방향을 역전시키지 않는가?
- [ ] Common에 BC 의존이 생기지 않는가?
- [ ] DTO가 올바른 레이어(presentation/dto 또는 infrastructure/dto)에 위치하는가?
- [ ] 이 문서에 반영했는가?