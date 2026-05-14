# third-tool-backend

카드 기반 학습/복습 도구의 백엔드 서버 (Spring Boot · Java 21 · DDD).

회상 기반 학습을 위한 카드(`Card`) → 덱(`Deck`) → 리뷰 세션(`ReviewSession`) 흐름과, 학습 목표·자료를 구조화하는 `LearningFacade` 프레임워크를 제공합니다.

---

## 빠른 시작

```bash
./gradlew bootRun        # 로컬 실행 (dev 프로필, H2 인메모리)
./gradlew test           # 전체 테스트
./gradlew build          # 빌드 + 테스트
```

실행 후 Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## 주요 문서

| 문서 | 내용 |
| --- | --- |
| [`PROJECT_OVERVIEW.md`](PROJECT_OVERVIEW.md) | 30초 프로젝트 개요 |
| [`docs/DOMAIN.md`](docs/DOMAIN.md) | 도메인 모델 — 전역 용어 + BC별 의도·불변식 |
| [`docs/PACKAGE.md`](docs/PACKAGE.md) | 패키지 구조 — 4-Layer + BC별 디렉토리 + 의존 규칙 |
| [`docs/adr/`](docs/adr/) | 아키텍처 결정 기록 (ADR) |
| [`CLAUDE.md`](CLAUDE.md) | Claude Code 협업 진입 |

---

## 기술 스택

Spring Boot 3.5.5 · Java 21 · Spring Data JPA + QueryDSL · MySQL 8.0 (prod) / H2 (dev) · Flyway · Spring Security + OAuth2 (Kakao / Naver) · JWT · AWS S3 SDK.

---

## 협업

- **브랜치 / 커밋 / PR**: Claude Code 운영 룰은 [`.claude/rules/pr-commit.md`](.claude/rules/pr-commit.md). 사람 협업자는 본 README의 다음 흐름을 참고:
  - 각자 `main`에서 `feature/{your-task}` 브랜치 생성 후 작업.
  - 작업 중 `main`이 변경되면 `git pull origin main`으로 동기화.
  - 충돌 해결 후 push → PR 제출.
- **머지**: PR 머지는 사람이 직접 수행.

---

## 라이센스 / 운영 환경

운영 인프라 구성(EC2 / ECR / RDS / S3 / GitHub Actions)은 내부 문서로 별도 관리됩니다.
