# 패키지 구조 문서

> 기준 버전: ThirdTool v1.0  
> 최종 수정일: 2026-03-21  
> 설계 목표: DDD 기반 Bounded Context 분리 → 멀티 모듈 전환 대비

---

## 1. 전체 구조

```
com.example.thirdtool
├── Card
│   ├── application
│   ├── domain
│   │   ├── exception
│   │   └── model
│   └── infrastructure
│       ├── dto
│       └── persistence
│
├── Common
│   ├── config
│   ├── Exception
│   ├── init
│   ├── security
│   ├── Util
│   └── BaseEntity
│
├── Dashboard
│   ├── controller
│   └── domain
│
├── Deck
│   ├── application
│   │   └── service
│   │       ├── DeckCommandService
│   │       ├── DeckHierarchyService
│   │       └── DeckQueryService
│   ├── domain
│   │   └── model
│   │       ├── Deck
│   │       └── DeckMode
│   ├── infrastructure
│   │   └── repository
│   │       ├── DeckRepository (interface)
│   │       └── DeckQueryRepository
│   └── presentation
│
├── Health
│   └── HealthCheckController
│
├── infra
├── Notification
├── RemindSearch
└── User
    └── application
```

---

## 2. Bounded Context 별 책임

| Context | 핵심 책임 | Aggregate Root |
|---------|----------|---------------|
| Card | 카드 생성·수정·삭제, KeywordCue·MainNote·Summary 관리 | Card |
| Deck | 덱 계층 구조 관리, 알고리즘 할당 | Deck |
| Dashboard | 학습 현황 집계 및 조회 | - (Read Model) |
| User | 유저 인증·인가, 프로필 관리 | UserEntity |
| Notification | 복습 알림 발송 | - |
| RemindSearch | 복습 대상 카드 탐색 | - |
| Common | 공통 예외, 보안, 설정, 유틸 | - |

---

## 3. 레이어별 역할

### application
- UseCase / Service 클래스 위치
- 트랜잭션 경계 담당
- 도메인 객체를 조합하고 인프라 레이어를 호출
- **외부 레이어(presentation)에서만 호출 가능**

```
DeckCommandService   → 생성·수정·삭제 (Command)
DeckQueryService     → 단건·목록 조회 (Query)
DeckHierarchyService → 계층 구조 변경 (복잡 Command 분리)
```

### domain
- 순수 비즈니스 규칙만 존재
- 인프라·Spring 의존성 없음
- `model` : Entity, Value Object, Enum
- `exception` : 도메인 규칙 위반 전용 예외

### infrastructure
- DB 접근, 외부 API 연동
- `persistence` : JPA Repository 구현체, QueryDSL
- `dto` : QueryDSL Projection용 DTO, 외부 연동 응답 매핑

### presentation
- Controller 위치
- Request / Response DTO 정의
- application 레이어 호출만 담당, 비즈니스 로직 없음

---

## 5. 멀티 모듈 전환 시 예상 구조(아직 대기)

```
thirdtool-root
├── thirdtool-common          ← Common 패키지 → 모듈
├── thirdtool-card            ← Card Bounded Context → 모듈
├── thirdtool-deck            ← Deck Bounded Context → 모듈
├── thirdtool-user            ← User Bounded Context → 모듈
├── thirdtool-notification    ← Notification → 모듈
└── thirdtool-api             ← presentation 통합, 진입점
```