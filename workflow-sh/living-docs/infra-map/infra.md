# Infrastructure Map (Living)

> **성격**: living-docs — 항상 최신본. "지금 이 프로덕트는 이렇게 배포·실행된다".
> **성장 방향**: 파일이 커지면 `infra-map/network.md`, `infra-map/ci-cd.md`, `infra-map/db.md` 등으로 분화.
> **진실 소스**: `.github/workflows/*.yml` (CI/CD 흐름) + `src/main/resources/application*.yml` (런타임 설정) + AWS 콘솔.
> **관련 topology**: `workflow/topologys/versions/{Nv}/delivery.md`.

---

## 1. 한눈에 보기 (One-Screen)

```
┌──────────────────────────────────────────────────────────────────────┐
│                          GitHub                                       │
│  main branch push  ──▶  dev-cicd.yml (GHA)                            │
│                          ├─ OIDC AssumeRole (gha-deploy-role)          │
│                          ├─ docker build (Dockerfile · gradle bootJar) │
│                          ├─ ECR push (:latest + :sha7 dual tag)        │
│                          └─ SSH → EC2 deploy                          │
└──────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    AWS (ap-northeast-2)                              │
│                                                                       │
│   ┌─────────────────┐                                                 │
│   │   ECR           │  third-tool-server:{latest, sha7}                │
│   │   third-tool-   │  third-tool-elaticsearch:latest                  │
│   │   elaticsearch  │                                                 │
│   └────────┬────────┘                                                 │
│            │ docker pull                                              │
│            ▼                                                          │
│   ┌─────────────────────────────────────────────────┐                 │
│   │  EC2 (Ubuntu, /home/ubuntu/third-tool)          │                 │
│   │   docker-compose:                                │                 │
│   │     ├─ local-mysql-db  (3306, healthcheck ping)  │                 │
│   │     ├─ redis           (6379)                    │                 │
│   │     └─ elasticsearch   (9200, Nori 형태소)       │                 │
│   │   spring container:                              │                 │
│   │     └─ third-tool-server  (8080)                 │                 │
│   │        network: third-tool-net                   │                 │
│   │        SPRING_PROFILES_ACTIVE=dev                │                 │
│   │        graceful shutdown 30s (ADR017)            │                 │
│   └─────────────────────────────────────────────────┘                 │
│                                                                       │
│   ┌─────────────────┐   ┌─────────────────┐                           │
│   │  Secrets (GHA)  │   │  IAM            │                           │
│   │  DB_PASSWORD    │   │  gha-deploy-    │                           │
│   │  JWT_SECRET_KEY │   │  role (OIDC)    │                           │
│   │  KAKAO_*        │   │  ECR/S3 access  │                           │
│   │  NAVER_*        │   └─────────────────┘                           │
│   │  AWS_ACCESS_KEY │  (S3 접근용 · transitional, ECS 이행 시 제거) │
│   └─────────────────┘                                                 │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 2. CI/CD 파이프라인 (`.github/workflows/dev-cicd.yml`)

**트리거**:
- `main` 브랜치 push → 자동 배포 (`deploy-app` job)
- `workflow_dispatch` (수동) → Elasticsearch 이미지 빌드 (`build-es-image` job, 선택)

**주요 단계**:
1. **OIDC 사전 검증** — `vars.AWS_ACCOUNT_ID` 미설정 시 fail-fast (Story-046 · ts007 참조).
2. **AWS 자격증명 획득** — `aws-actions/configure-aws-credentials@v4`로 `gha-deploy-role` AssumeRole. STS 1시간 TTL 임시 자격증명.
   - 신뢰 정책 sub 조건: `repo:Third-tool/third-tool:ref:refs/heads/main` (fork PR·임의 branch 차단).
3. **ECR 로그인** — `aws-actions/amazon-ecr-login@v2`.
4. **이미지 태그 계산** — `sha7=${GITHUB_SHA::7}`, dual tag: `:latest` + `:{sha7}` (Story-055).
5. **Docker 빌드** — 단일 루트 `Dockerfile` (Story 1-1, builder stage가 gradle bootJar 수행).
6. **ECR push** — `:latest` + `:{sha7}` 둘 다 push.
7. **EC2 SSH 배포** — `appleboy/ssh-action@v1.0.3`:
   - `docker compose pull/down/up` (mysql, redis, es)
   - MySQL ready 대기 (최대 25s, ping)
   - Elasticsearch ready 대기 (최대 60s, `/_cluster/health`)
   - `docker stop/rm third-tool-server`
   - `docker pull $IMAGE_TAG_SHA7`
   - `docker image prune -af` (쓰레기 정리)
   - `docker run` 새 컨테이너 (env 주입 · `--network third-tool-net`)

**보안 관련 이슈**:
- OIDC로 GHA → AWS 인증 전환 완료 (Story-046). Long-lived access key 폐기.
- **EC2 컨테이너 env에 `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` 주입은 transitional 유지** — `S3Config`가 아직 `StaticCredentialsProvider` 사용 중. ECS 이행 + `DefaultCredentialsProvider` 전환 시점에 일괄 제거 (milestone 0.0.1v item #11, ADR013 follow-up).

---

## 3. 런타임 구성 (`application.yml` + profile)

### Profile 전략
- **기본값**: `SPRING_PROFILES_ACTIVE=dev,local`
  - `local`이 뒤에 와서 override 우선 (H2 console, show-sql 등 로컬 편의)
- **CI/서버**: `SPRING_PROFILES_ACTIVE=dev` (또는 `prod` 명시)
- **파일 분리**: `application-{dev,local,prod}.yml`

### 공통 설정 하이라이트
| 항목 | 값 · 설명 |
| --- | --- |
| Multipart | max-file-size 10MB, max-request-size 50MB |
| Graceful Shutdown | `spring.lifecycle.timeout-per-shutdown-phase: 30s` + `server.shutdown: graceful` — ECS deregistration_delay 30s와 매칭 (ADR016·017) |
| Logging | `logback-spring.xml`로 일원화 (ADR008) |
| JWT | secret `${JWT_SECRET_KEY}` (env), Access 30m TTL, Refresh 7d TTL |
| JWT Cookie | `name=access_token`, HttpOnly, SameSite=Strict, secure는 profile별 (dev: false, prod: true) |
| Actuator | 기본 미노출. profile별로 exposure 갱신 |
| Actuator Health cache | 30s TTL |
| Swagger | `/swagger-ui.html`, prod에서 비활성화 |

### 주요 env 변수
| 이름 | 용도 |
| --- | --- |
| `JWT_SECRET_KEY` | JWT 서명 키 |
| `DB_NAME` | DB 이름 |
| `DB_PASSWORD` | DB 비밀번호 |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | S3 접근 (transitional) |
| `KAKAO_CLIENT_ID` / `KAKAO_CLIENT_SECRET` | 카카오 OAuth |
| `NAVER_CLIENT_ID` / `NAVER_CLIENT_SECRET` | 네이버 OAuth |
| `TZ=Asia/Seoul` | 컨테이너 timezone |
| `SPRING_PROFILES_ACTIVE` | profile 전환 |

---

## 4. 실행 인프라 (EC2 컨테이너 스택)

**docker network**: `third-tool-net` (bridge).

| 컨테이너 | 이미지 | 포트 | 역할 |
| --- | --- | --- | --- |
| `local-mysql-db` | mysql (compose managed) | 3306 | 운영 DB. `mysqladmin ping` healthcheck |
| `redis` | redis (compose managed) | 6379 | 캐시 · 세션 저장소 |
| `elasticsearch` | `${ECR}/third-tool-elaticsearch:latest` (Nori) | 9200 | 검색 · 형태소 분석 |
| `third-tool-server` | `${ECR}/third-tool-server:{sha7}` | 8080 | Spring Boot 애플리케이션 |

**볼륨 마운트**:
- `/home/ubuntu/third-tool/application.yml` → 컨테이너 `/app/config/application.yml` (환경별 설정 override)

---

## 5. 데이터베이스

- **운영**: MySQL 8.0 (EC2 컨테이너 위 `local-mysql-db`)
- **개발**: H2 (MODE=MySQL) — 로컬·테스트 자동 활성화
- **마이그레이션**: Flyway `V*.sql` (현재 V1~V22)
  - MySQL: `org.flywaydb:flyway-mysql`
  - 파일 위치: `src/main/resources/db/migration/V*__*.sql`
  - 롤백: `R*__rollback_*.sql`
  - 원칙: forward-only, 기존 V 파일 수정 금지

---

## 6. 관측성 · 로깅

- **로깅**: `logback-spring.xml` 통일 (ADR008).
- **Actuator**: 기본 미노출. profile별로 endpoint exposure 갱신.
- **알람·모니터링**: 별도 구축 예정 (`workflow/living-docs/ops-health-board/` — Tier 3, 0.0.4v 이후).

---

## 7. 알려진 이슈 · 이행 예정

| 항목 | 상태 | 이행 시점 |
| --- | --- | --- |
| EC2 SSH 배포 → ECS Task Role | transitional 유지 | milestone 0.0.1v item #11 |
| `AWS_ACCESS_KEY_ID`/`SECRET` env 주입 → `DefaultCredentialsProvider` | transitional | ECS 이행과 함께 |
| Actuator endpoint 노출 · Grafana 대시보드 | 미구축 | 0.0.4v 이후 |

---

## 8. 참조

- CI/CD: `.github/workflows/dev-cicd.yml`
- 런타임: `src/main/resources/application*.yml`
- 관련 topology: `workflow/topologys/versions/{Nv}/delivery.md`
- ADR: ADR008 (logging), ADR013 (S3), ADR016 (deregistration_delay), ADR017 (graceful shutdown)
- Story 참조: Story-046 (OIDC), Story-055 (dual tag)

*최신 갱신: 2026-07-03 · dev-cicd.yml + application.yml 스캔 반영*
