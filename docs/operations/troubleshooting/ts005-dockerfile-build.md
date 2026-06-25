# ts005: Dockerfile 통합 빌드 가이드 + 환경별 이슈

Story 1-1로 통합된 루트 `Dockerfile` 사용·검증 절차. 기존 `Dockerfile-dev`는 삭제되었고 CI(`dev-cicd.yml`)도 동일 파일로 갱신됨.

## 구성

```
프로젝트 루트/
├── Dockerfile                  # ThirdTool Spring Boot 서버 (Story 1-1 신규 통합)
├── Dockerfile-elasticsearch    # ES + nori (별도 인프라, 본 가이드 외)
├── .dockerignore               # 컨텍스트 < 1MB 목표 (Story 1-1로 확장)
└── .github/workflows/dev-cicd.yml   # deploy-app job이 본 Dockerfile 사용
```

## 사전 조건

- Docker Desktop (Mac/Windows) 또는 Docker Engine 20.10+ (Linux)
- BuildKit 활성화 (`DOCKER_BUILDKIT=1` 환경변수 또는 Docker Desktop 기본)
- 별도 secret/yaml 파일 준비 불요 — 빌드 단에선 `.dockerignore`로 제외, 런타임은 환경변수 주입

## 로컬 빌드

```bash
# BuildKit 사용 권장 (layered jar 캐시 효율)
DOCKER_BUILDKIT=1 docker build -t thirdtool:local -f Dockerfile .
```

빌드 단계:
1. **builder** — `gradle:8.8-jdk21-alpine`에서 의존성 다운로드(`./gradlew dependencies`) + `bootJar -x test`
2. **extractor** — bootJar를 `jarmode=tools extract --layers`로 4 layer 분리
3. **runtime** — `eclipse-temurin:21-jre-alpine` + Asia/Seoul + 비-root user + layer 복사

## 검증

### 1. 이미지 크기 (AC: < 200MB)

```bash
docker images | grep thirdtool
# thirdtool   local   <hash>   <created>   ~150MB
```

목표: < 200MB. 초과 시 base image 변경 또는 의존성 audit.

### 2. 빌드 컨텍스트 크기 (AC: < 1MB)

```bash
# .dockerignore 적용 후 컨텍스트 dry-run 측정
DOCKER_BUILDKIT=1 docker build --no-cache --progress=plain -t test:context . 2>&1 | grep "transferring context"
# 예: => transferring context: 850kB  (< 1MB)
```

`.dockerignore`에서 `docs/`, `workflow/`, `meta/`, `monitoring/`, `src/test/`, `.git`, `build/` 등 제외 — 컨텍스트의 90% 이상이 차단됨.

### 3. 비-root user 검증

```bash
docker run --rm thirdtool:local id
# 기대: uid=<N>(app) gid=<N>(app) groups=<N>(app)
```

uid=0(root)가 나오면 Dockerfile의 `USER app` 누락 또는 base image 변경 영향.

### 4. 부팅 검증 (환경변수 주입)

```bash
docker run -d --name t -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e JWT_SECRET_KEY=any-32char-or-longer-secret-for-test \
  -e KAKAO_CLIENT_ID=test -e KAKAO_CLIENT_SECRET=test \
  -e NAVER_CLIENT_ID=test -e NAVER_CLIENT_SECRET=test \
  -e AWS_ACCESS_KEY_ID=test -e AWS_SECRET_ACCESS_KEY=test \
  thirdtool:local

# 30초 대기 후 health 확인
sleep 30
curl -s localhost:8080/health
# 기대: 200 OK (Spring Boot 부팅 + health endpoint 응답)

docker logs t | tail -20
docker stop t && docker rm t
```

actuator 노출은 ts001 가이드 참조 — 본 가이드는 부팅 검증만.

### 5. layer 캐시 효율 검증

소스 1줄 수정 후 재빌드:

```bash
# 1차 빌드 (캐시 없음)
DOCKER_BUILDKIT=1 docker build -t thirdtool:local -f Dockerfile .

# 한 java 파일에 1줄 추가 후 재빌드
echo '// touch' >> src/main/java/com/example/thirdtool/ThirdToolApplication.java
DOCKER_BUILDKIT=1 docker build -t thirdtool:local -f Dockerfile . 2>&1 | grep "CACHED"
# 기대: dependencies·spring-boot-loader·snapshot-dependencies layer는 CACHED, application layer만 재빌드
```

## CI 영향

`.github/workflows/dev-cicd.yml` 라인 90이 `-f ./Dockerfile-dev` → `-f ./Dockerfile`로 갱신됨. 다른 CI 단계는 그대로:
- AWS ECR 로그인 → docker build → tag → push → EC2 SSH 배포

머지 후 첫 push에서 CI가 통합 Dockerfile로 빌드·배포 진행. ECR repository 이름·EC2 배포 절차는 변경 없음.

## 트러블슈팅

### ts005-1: builder stage에서 `./gradlew dependencies` 실패

원인: 네트워크 또는 Gradle wrapper 권한.

해결:
```bash
# wrapper 권한 부여
chmod +x gradlew

# Gradle 캐시 mount로 재시도 (BuildKit 필수)
DOCKER_BUILDKIT=1 docker build --no-cache -t thirdtool:local -f Dockerfile .
```

### ts005-2: extractor stage에서 `jarmode=tools` 미지원

원인: Spring Boot 3.2 미만은 `jarmode=tools` 미존재. 본 프로젝트는 3.5.5이므로 정상.

확인:
```bash
docker run --rm eclipse-temurin:21-jre-alpine \
  java -Djarmode=tools -jar /dev/null 2>&1 | head -5
# 기대: Usage: java -Djarmode=tools ... (사용법 출력)
```

### ts005-3: 런타임 부팅 시 placeholder 미해결 오류

```
Could not resolve placeholder 'JWT_SECRET_KEY' in value "${JWT_SECRET_KEY}"
APPLICATION FAILED TO START
```

해결: `docker run` 시 `-e` 환경변수 6종 명시 — ts002 가이드 참조.

### ts005-4: timezone이 UTC로 보임

원인: Asia/Seoul 설정 미적용 — Dockerfile 변경 후 재빌드 필요.

확인:
```bash
docker run --rm thirdtool:local date
# 기대: KST 시각 출력
```

### ts005-5: 이미지 크기가 200MB를 초과

원인 가능성:
- `.dockerignore`가 적용 안 됨 (BuildKit 미활성화 등)
- base image가 `21-jre-alpine`이 아닌 `21-jdk-alpine`으로 잘못 변경됨
- 새 의존성이 비대(e.g. spring-cloud-aws 도입)

해결:
```bash
# 이미지 안 layer별 크기 분석
docker history thirdtool:local
docker run --rm -it wagoodman/dive thirdtool:local
```

## 후속 (별도 Story)

- **git_sha 기반 이미지 태깅** — milestone Tier 2 #17 / Story 1-2. ECR push 태그를 `<sha:7>` prefix로 변경
- **ECR 라이프사이클 정책 JSON 커밋** — 최근 10개 tagged 보존, untagged 7일 후 삭제. AWS 인프라 작업
- **Enhanced scanning 활성화** — AWS 콘솔에서 ECR repository 설정
- **GHA cache backend** — `docker/build-push-action@v5 cache-from=gha cache-to=gha`로 CI 빌드 시간 단축
- **Distroless 전환** — `gcr.io/distroless/java21-debian12:nonroot` 운영 안정화 후 별도 Story
- **Spring Boot AOT/GraalVM native image** — v2 startup·메모리 최적화

## 관련

- Story 1-1 (Product 5 Epic 1) — Dockerfile 통합·multi-stage·JDK 21·이미지 < 200MB
- ts001 — `/actuator/prometheus` 노출을 위한 application.yml 갱신 (부팅 후 actuator 사용 시)
- ts002 — 환경변수 설정 가이드 (docker run -e ...)
- ts003 — profile 분리 (SPRING_PROFILES_ACTIVE)
- 후속: Story 2-1 (ECS Task Definition이 본 이미지 ECR URI 참조)
