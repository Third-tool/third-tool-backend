# syntax=docker/dockerfile:1.7
#
# ThirdTool 단일 운영 Dockerfile (Story 1-1, Product 5 Epic 1)
#
# 3-stage 구조:
#   1) builder   — Gradle + JDK 21로 Spring Boot bootJar
#   2) extractor — bootJar의 layered jar를 layer별로 분리
#   3) runtime   — JRE 21 alpine + 비-root user + Asia/Seoul + layer COPY
#
# 목표:
#   - 최종 이미지 < 200MB
#   - layered jar로 docker layer 캐시 효율 ↑ (deps 변경 없으면 application layer만 재빌드)
#   - 비-root user (보안 baseline)
#
# 빌드:
#   DOCKER_BUILDKIT=1 docker build -t thirdtool:local -f Dockerfile .
# 검증/실행 가이드: docs/operations/troubleshooting/ts005-dockerfile-build.md

# ===== Stage 1: build =====
FROM gradle:8.8-jdk21-alpine AS builder
WORKDIR /workspace

# 의존성 사전 다운로드 — 소스 변경과 무관하게 캐시
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon || true

# 전체 소스 복사 + bootJar (test 스킵)
COPY . .
# application.yml은 .dockerignore로 차단되지만 안전망
RUN rm -f src/main/resources/application.yml || true
RUN ./gradlew clean bootJar -x test --no-daemon

# ===== Stage 2: layered jar 추출 =====
FROM eclipse-temurin:21-jre-alpine AS extractor
WORKDIR /workspace
COPY --from=builder /workspace/build/libs/*SNAPSHOT.jar app.jar
# Spring Boot 3.x: jarmode=tools + extract --layers
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# ===== Stage 3: runtime =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Asia/Seoul timezone (기존 Dockerfile-dev 동작 보존)
RUN apk add --no-cache tzdata \
 && cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime \
 && echo "Asia/Seoul" > /etc/timezone \
 && apk del tzdata

# 비-root user — 보안 baseline
RUN addgroup -S app && adduser -S -G app app

# Layered jar 4개 layer를 변경 빈도가 낮은 순으로 COPY (캐시 효율 ↑)
COPY --from=extractor --chown=app:app /workspace/extracted/dependencies/ ./
COPY --from=extractor --chown=app:app /workspace/extracted/spring-boot-loader/ ./
COPY --from=extractor --chown=app:app /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extractor --chown=app:app /workspace/extracted/application/ ./

USER app
EXPOSE 8080

# Spring Boot 3.x 표준 launcher — java -jar app.jar보다 약간 빠른 부팅
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
