# third-tool 프로젝트 개요

카드 기반 학습/복습 도구의 백엔드 서버입니다. 사용자가 학습 카드를 만들고, 덱으로 묶고, 복습 세션으로 회상하며, 학습 목표(LearningFacade)와 자료를 체계적으로 관리하는 기능을 제공합니다.

> 새로 합류한 개발자가 30초 안에 프로젝트의 전체 윤곽을 잡을 수 있도록 작성된 진입 문서입니다. 상세 내용은 본문 곳곳에서 `docs/` 하위 가이드로 링크합니다.

---

## 1. 한눈에 보기

| 항목 | 내용 |
|---|---|
| 프로젝트 성격 | DDD 기반 학습/복습 백엔드 (Spring Boot) |
| 핵심 흐름 | `Card` → `Deck` → `ReviewSession` (+ `LearningFacade` 학습 목표 프레임워크) |
| 진입 문서 | 이 파일 (`PROJECT_OVERVIEW.md`) |
| Claude Code 진입 | [`CLAUDE.md`](CLAUDE.md) |

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

도메인별 최상위 패키지(BC) 안에 4-Layer 구조(presentation / application / domain / infrastructure)를 강제합니다.

```
src/main/java/com/example/thirdtool/
├── Card/             # 학습 카드 BC
├── Deck/             # 카드 묶음 BC
├── Review/           # 복습 세션 BC
├── LearningFacade/   # 학습 목표 프레임워크 BC
├── User/             # 사용자 / OAuth (Kakao, Naver) BC
├── UserSchedule/     # 사용자별 학습 스케줄 BC
├── Common/           # BC 횡단 공통 (config / security / exception / util)
├── infra/            # 기술 어댑터 (S3, ai 등)
└── ThirdToolApplication.java
```

> **상세 패키지 가이드**: [`docs/PACKAGE.md`](docs/PACKAGE.md) — 4-Layer 표준 + BC별 디렉토리 매핑 + Controller↔Service Command/Query record 패턴(ADR005) + BC 간 의존 규칙.

---

## 4. 도메인 모델

각 BC의 Aggregate Root와 핵심 책임, 도메인 불변식, 결정 메모는 단일 파일에 정리되어 있습니다.

> **도메인 의도·용어·불변식**: [`docs/DOMAIN.md`](docs/DOMAIN.md) — 전역 용어 35개 + BC별 6개 섹션(Card / Deck / Review / LearningFacade / User / UserSchedule).
> **아키텍처 결정 기록**: [`docs/adr/`](docs/adr/) — ADR001(PK) / ADR002(Enum) / ADR003(Soft Delete) / ADR004(AxisTopic 명사구) / ADR005(Command/Query record).

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

브랜치 운영과 협업 흐름은 [`README.md`](README.md)·[`.claude/rules/pr-commit.md`](.claude/rules/pr-commit.md)를 참조하세요.

---

## 6. Claude Code 협업

본 프로젝트는 **Plan mode + Story 단위 협업**을 전제로 운영됩니다. Claude Code는 다음 룰을 따릅니다:

- 작업 흐름: [`.claude/rules/workflow.md`](.claude/rules/workflow.md)
- 코드 컨벤션 (도메인·API·DB·테스트): [`.claude/rules/conventions.md`](.claude/rules/conventions.md)
- Story Reviewer 세션 (5관점 병렬 검토): [`.claude/rules/review.md`](.claude/rules/review.md)
- 브랜치·커밋·push·PR: [`.claude/rules/pr-commit.md`](.claude/rules/pr-commit.md)
- ADR 작성: [`.claude/rules/adr.md`](.claude/rules/adr.md)

`.claude/`는 gitignored로 운영 룰을 개발자만 보는 영역에 둡니다. 영속 자산(도메인 의도 / 패키지 구조 / 결정 기록)은 `docs/`에 tracked로 GitHub 공개됩니다.
