# ts003: application-{dev,prod}.yml 분리 + local override 체계

## 변경 요약

기존 `application.yml` 한 파일에 multi-document(`---`)로 묶여 있던 dev/prod 섹션을 별도 파일로 분리. 로컬 개발자 편의 override는 별도 `local` profile.

```
application.yml          공통 — servlet/jwt 공통/swagger/actuator 기본
application-dev.yml      dev — H2 + dev secrets(env vars) + jwt.cookie.secure=false
application-prod.yml     prod — RDS MySQL + prod secrets(env vars) + jwt.cookie.secure=true + swagger 비활성화
application-local.yml    local — H2 console + show-sql + format_sql + batch off (dev/prod와 조합 활성화)
application-test.yml     test — 자체 H2 + dummy secrets + actuator (test 전용, 다른 profile과 독립)
```

## Spring Boot profile 합본 규칙

`spring.profiles.active`에 콤마로 나열된 순서는 **나중 것이 우선**. 즉 `dev,local`이면 local이 마지막 → local의 override가 dev 위에 적용.

| `SPRING_PROFILES_ACTIVE` | 의도 | 결과 |
| --- | --- | --- |
| `dev,local` | dev base + local override (권장 로컬) | H2 + dev secrets + local의 show-sql·h2-console 활성화 |
| `prod,local` | prod base + local override | RDS/로컬 MySQL + prod secrets + local의 show-sql·h2-console 활성화 |
| `dev` | dev 단독 (CI/dev 서버) | H2 + dev secrets, show-sql 없음 |
| `prod` | prod 단독 (운영 서버) | RDS + prod secrets, show-sql 없음, swagger 비활성화 |
| `test` (`@ActiveProfiles("test")`) | 테스트 컨텍스트 | 자체 H2 + dummy secrets — env vars 불요 |

`application.yml` 기본 `active`: `${SPRING_PROFILES_ACTIVE:dev,local}`. 환경변수 미설정 시 로컬 친화 default 적용. CI/서버는 환경변수로 `dev` 또는 `prod` override.

## 로컬 개발자 환경변수 설정

### IntelliJ Run Configuration

`Run/Debug Configurations` → `ThirdToolApplication`:
- `Active profiles`: 비워둠 (application.yml default `dev,local` 사용) 또는 `dev,local` 명시
- `Environment variables`:
  ```
  JWT_SECRET_KEY=any-32char-or-longer-secret-for-dev-local
  KAKAO_CLIENT_ID=<카카오 콘솔에서 발급>
  KAKAO_CLIENT_SECRET=<카카오 콘솔에서 발급>
  NAVER_CLIENT_ID=<네이버 콘솔에서 발급>
  NAVER_CLIENT_SECRET=<네이버 콘솔에서 발급>
  AWS_ACCESS_KEY_ID=<S3 IAM 키>
  AWS_SECRET_ACCESS_KEY=<S3 IAM 시크릿>
  ```

### CLI / Gradle bootRun

```bash
# bash/zsh — application.yml default(dev,local) 그대로 사용
export JWT_SECRET_KEY=...
export KAKAO_CLIENT_ID=...
# ... (kakao, naver, AWS)
./gradlew bootRun

# PowerShell
$env:JWT_SECRET_KEY = "..."
$env:KAKAO_CLIENT_ID = "..."
./gradlew bootRun

# profile 명시 override (예: local 끄고 dev만)
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

### local + prod 시뮬레이션

로컬에서 prod profile 동작을 확인하려면 (예: HikariCP·Flyway·Hibernate validate 검증):

```bash
export SPRING_PROFILES_ACTIVE=prod,local
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=thirdtool_local
export DB_USERNAME=root
export DB_PASSWORD=...
# kakao/naver/AWS prod 환경변수
./gradlew bootRun
```

로컬 MySQL 실행 필요(`docker run -p 3306:3306 mysql:8`). `ddl-auto: validate`라 Flyway 마이그레이션이 먼저 실행되어야 스키마 검증 통과.

## 검증

```bash
# 기본 (dev,local) — H2 + show-sql + console
./gradlew bootRun
# 로그: SQL 콘솔 출력. http://localhost:8080/h2-console 접속 가능

# dev 단독 (CI 모사) — H2 + show-sql false
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
# 로그: SQL 출력 없음. h2-console 접근 불가

# prod 활성화 — RDS(또는 로컬 MySQL) + Flyway
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```

## test profile은 독립

`@ActiveProfiles("test")`는 `application-test.yml`만 로드 (application.yml의 default `dev,local`은 적용 안 됨 — Spring Test가 ActiveProfiles로 override). test profile은 자체 H2 + dummy secrets + actuator 노출 설정을 포함해 환경변수 의존 없이 부팅.

## 관련

- 분리 트리거: 사용자 요청 — application.yml gitignore 해제 후 환경별 분리 + 로컬에서 dev/prod 시뮬레이션 둘 다 지원
- 환경변수 가이드: [`ts002-environment-variables.md`](ts002-environment-variables.md)
- 후속: AWS Secrets Manager 도입 시 환경변수 패턴이 SecretManager fetch로 전환 (Product 7)
