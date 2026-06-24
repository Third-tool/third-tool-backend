# ts002: dev/local 실행 시 환경변수 누락으로 부팅 실패

## 상황

`application.yml`이 git에 추적되기 시작하면서(.gitignore 해제) 비밀 default 값을 모두 제거했다. 모든 secret은 환경변수(`${ENV_NAME}`)로만 주입되므로, 환경변수가 설정되지 않은 상태에서 `./gradlew bootRun`을 실행하면 Spring Boot가 placeholder 해석 실패로 부팅하지 못한다.

## 증상

```
java.lang.IllegalArgumentException: Could not resolve placeholder 'JWT_SECRET_KEY' in value "${JWT_SECRET_KEY}"
...
APPLICATION FAILED TO START
```

또는 `KAKAO_CLIENT_ID`, `NAVER_CLIENT_SECRET`, `AWS_ACCESS_KEY_ID` 등에서 동일 오류.

## 원인

- `application.yml` 라인 22: `jwt.secret-key: ${JWT_SECRET_KEY}` (default 제거)
- dev profile kakao/naver/AWS: 모두 `${...}` 환경변수 의존 (default 제거)
- prod profile은 원래부터 default 없음 (변화 없음)

SecretManager(Secrets Manager) 도입 전까지 임시 운영 방식으로, 환경변수 export가 책임이 된다.

## 해결 — 로컬 개발자 환경변수 설정

### IntelliJ Run Configuration

`Run/Debug Configurations` → `ThirdToolApplication` → `Environment variables`에 다음 추가:

```
JWT_SECRET_KEY=any-32char-or-longer-secret-for-dev-local
KAKAO_CLIENT_ID=<카카오 developers 콘솔에서 발급>
KAKAO_CLIENT_SECRET=<카카오 developers 콘솔에서 발급>
NAVER_CLIENT_ID=<네이버 developers 콘솔에서 발급>
NAVER_CLIENT_SECRET=<네이버 developers 콘솔에서 발급>
AWS_ACCESS_KEY_ID=<S3 업로드용 IAM 사용자 키>
AWS_SECRET_ACCESS_KEY=<S3 업로드용 IAM 사용자 시크릿>
```

### CLI / Gradle bootRun

```bash
# bash/zsh
export JWT_SECRET_KEY=...
export KAKAO_CLIENT_ID=...
# ...
./gradlew bootRun

# PowerShell
$env:JWT_SECRET_KEY = "..."
$env:KAKAO_CLIENT_ID = "..."
# ...
./gradlew bootRun
```

### .env 파일 + direnv (선택)

`direnv` 사용 시 프로젝트 루트의 `.env`에 변수 정의 후 `direnv allow`. `.env`는 `.gitignore` 대상.

## 검증

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
# 정상 부팅 → "Started ThirdToolApplication in N.NNN seconds"
```

부팅 후 `http://localhost:8080/swagger-ui.html` 접속 가능.

## 후속 — SecretManager 마이그레이션

본 ts는 임시 가이드. 후속 Product/Story에서 AWS Secrets Manager + Spring Cloud AWS Starter로 마이그레이션 예정:

- secret 등록은 콘솔에서 1회 수행
- 애플리케이션은 IAM Role로 인증 후 부팅 시 SecretManager에서 secret 자동 fetch
- 본 ts는 SecretManager 도입 시점에 deprecate

## 관련

- `.gitignore`에서 `application.yml`, `application-dev.yml` 제거 — SecretManager 마이그레이션 이후 환경 간 drift 차단이 목적
- `application.yml` 공통/dev 섹션 — default 평문 secret 제거 후 환경변수만
- 후속: Secrets Manager 도입 Story (Product 7 — `product-infra-ops.md`)
