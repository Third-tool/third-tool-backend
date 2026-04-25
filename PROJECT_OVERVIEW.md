# third-tool 프로젝트 개요

카드 기반 학습/복습 도구의 백엔드 서버입니다. 사용자가 학습 카드를 만들고, 덱으로 묶고, 복습 세션으로 회상하며, 학습 목표(LearningFacade)와 자료를 체계적으로 관리하는 기능을 제공합니다.

> 새로 합류한 개발자가 30초 안에 프로젝트의 전체 윤곽을 잡을 수 있도록 작성된 진입 문서입니다. 상세 내용은 본문 곳곳에서 `docs/` 하위 가이드로 링크합니다.

---

## 1. 한눈에 보기

| 항목 | 내용 |
|---|---|
| 프로젝트 성격 | DDD 기반 학습/복습 백엔드 (Spring Boot) |
| 핵심 흐름 | `Card` → `Deck` → `ReviewSession` (+ `LearningFacade` 학습 목표 프레임워크) |
| 현재 작업 브랜치 | `feat/card-domain-model` (Card / LearningFacade 도메인 모델 정비) |
| 진입 문서 | 이 파일 (`PROJECT_OVERVIEW.md`) |

---

## 2. 기술 스택

| 영역 | 사용 기술 |
|---|---|
| 언어 / 런타임 | Java 21 |
| 프레임워크 | Spring Boot 3.5.5 |
| 빌드 | Gradle |
| 영속성 | Spring Data JPA + QueryDSL |
| DB | H2 (dev) / MySQL + Flyway 마이그레이션 (prod) |
| 인증 | Spring Security + OAuth2 Client (Kakao, Naver), JWT (`jjwt`) |
| 스토리지 | AWS S3 SDK 2.30.0 |
| API 문서 | springdoc-openapi 2.8.8 (Swagger UI) |
| 테스트 | JUnit Platform, Spring Boot Test |
| 기타 | Lombok, ULID |

---

## 3. 아키텍처 / 패키지 구조

도메인별 최상위 패키지 안에 DDD 레이어드 구조를 그대로 둡니다.

```
src/main/java/com/example/thirdtool/
├── Card/             # 학습 카드 도메인
│   ├── presentation/      # REST Controller, Request/Response DTO
│   ├── application/       # Command/Query Service (트랜잭션 경계)
│   ├── domain/            # Aggregate, Entity, VO, Domain Exception
│   └── infrastructure/    # JPA / QueryDSL Repository, 영속성 DTO
├── Deck/             # 카드 묶음 도메인
├── Review/           # 복습 세션 도메인
├── LearningFacade/   # 학습 목표 프레임워크 도메인
├── User/             # 사용자 / OAuth (Kakao, Naver)
├── UserSchedule/     # 사용자별 학습 스케줄
├── Common/           # 공통 모듈 (config, security, exception, util)
├── infra/            # 기술 어댑터 (S3, redis, ai 등)
└── ThirdToolApplication.java
```

**레이어 역할 요약**

- **presentation** — HTTP 진입점. 요청/응답 DTO 변환만 담당.
- **application** — 트랜잭션 경계. Command/Query Service 분리.
- **domain** — 비즈니스 규칙의 본진. Aggregate/Entity/VO와 도메인 예외.
- **infrastructure** — JPA·QueryDSL Repository 구현, 외부 시스템 어댑터.

> 패키지 구조 규칙과 의존 방향은 [`docs/architecture/package-structure-guide.md`](docs/architecture/package-structure-guide.md)에 상세히 정의되어 있습니다. 아키텍처 의사결정은 [`docs/architecture/adr/ADR001.md`](docs/architecture/adr/ADR001.md)를 참고하세요.

---

## 4. 도메인 모델

각 도메인의 Aggregate Root와 핵심 책임만 요약합니다. 용어 정의와 상세 모델은 [`docs/domain/용어사전.md`](docs/domain/용어사전.md)·[`docs/domain/도메인모델.md`](docs/domain/도메인모델.md)와 [`docs/database/테이블 정의서.md`](docs/database/테이블%20정의서.md)를 보세요.

### Card — 학습 카드 (`Card/domain/model/Card.java`)
Aggregate Root. 학습 한 단위를 표현합니다.
- 구성: `MainNote`(학습 본문, 텍스트/이미지) + `Summary`(1~3문장 요약) + `KeywordCue`(회상 단서, 최소 1개) + `CardTag`(최대 3개)
- 상태: `ON_FIELD`(활성) ↔ `ARCHIVE`(보관). `OnFieldBudget`(최대 조회수·체류 기간) 기반 수명 관리
- 부가: `recordView()`로 조회 횟수/시각 추적, Soft Delete 지원

### Deck — 카드 묶음 (`Deck/domain/model/`)
사용자별 카드 컬렉션. 부모-자식 계층 구조와 공개 라이브러리 등록을 지원합니다. Soft Delete 적용.

### Review — 복습 세션 (`Review/domain/model/`)
`ReviewSession`이 Aggregate Root. 한 세션이 여러 `CardReview`를 가지며 카드별 노출 범위(`CardVisibleContent`)를 제어합니다.

### LearningFacade — 학습 목표 프레임워크 (`LearningFacade/domain/model/LearningFacade.java`)
사용자의 "직업적 컨셉"을 축으로 학습을 구조화합니다.
- 계층: `LearningFacade`(컨셉) → `LearningAxis`(세부 축, 권장 ≤5) → `AxisAction`(행동 동사) → `LearningMaterial`(학습 자료)
- `ActionMaterial` 중간 엔티티로 행동-자료 매핑을 관리하고, `CoverageStatus`(`NO_MATERIAL`/`PARTIAL`/`COVERED`)로 커버리지를 평가
- `ActionRevision`이 행동 동사 수정 이력을, `ConceptChangeRecord`가 컨셉 변경 이력을 보관

### User — 사용자 (`User/domain/model/`)
`UserEntity` Aggregate Root. Kakao/Naver OAuth 로그인 후 자체 JWT 발급(`Common/security`, `Common/Util/JWTUtil`).

### UserSchedule — 학습 스케줄 (`UserSchedule/domain/model/`)
사용자별 학습 모드와 스케줄 정책을 관리합니다.

---

## 5. 빠른 시작

```bash
# 빌드
./gradlew build

# 로컬 실행 (dev 프로필, H2)
./gradlew bootRun

# API 문서
# http://localhost:8080/swagger-ui.html
```

브랜치 운영과 협업 흐름은 [`README.md`](README.md)를 참고하세요.
