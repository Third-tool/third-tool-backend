# ts001: Story 4-1 후 `application.yml`에 actuator 설정을 사용자가 직접 갱신해야 한다

## 상황

Story 4-1로 `io.micrometer:micrometer-registry-prometheus` 의존성을 추가하고 `ActuatorMetricsConfig`로 공통 태그·histogram을 Java 설정으로 적용했지만, **endpoint 노출 자체는 Spring Boot가 부트스트랩에서 properties로 읽기 때문에 Java 설정으로 대체할 수 없다**. `application.yml`은 `.gitignore` 대상이라 본 PR에서 commit되지 않으므로 사용자 환경에서 직접 갱신해야 dev/prod에서 `/actuator/prometheus`가 노출된다.

## 증상

- 본 PR 머지 후 `./gradlew bootRun` 또는 배포 환경에서 `curl localhost:8080/actuator/prometheus` → **404 Not Found**
- `curl localhost:8080/actuator` → endpoint 목록에 `prometheus` 부재
- 로그에 `PrometheusMeterRegistry` 빈은 등록되어 있으나 endpoint가 노출되지 않음

## 원인

- `application.yml`의 기본값이 `management.endpoints.web.exposure.include: []` (빈 배열)
- Spring Boot 3.5에서 PrometheusMeterRegistry 자동 등록 자체는 `micrometer-registry-prometheus` classpath만으로 작동하지만, 다음 두 properties가 명시되어야 endpoint가 노출·activation된다:
  - `management.endpoints.web.exposure.include` — endpoint 노출 화이트리스트
  - `management.prometheus.metrics.export.enabled` — Prometheus registry export 활성화 (Spring Boot 3.5에서 명시 권장 — 통합 테스트에서 이를 누락하면 빈이 등록되지 않는 사례 확인)

## 해결 — `application.yml` 갱신

`application.yml` 공통 섹션의 `management` 블록을 다음으로 교체한다.

```yaml
# Actuator / Micrometer Prometheus (Story 4-1)
management:
  endpoints:
    web:
      exposure:
        include: prometheus, health, info
        # env/beans/heapdump/threaddump 등은 환경변수·메모리·구조 정보를 노출하므로 명시 차단 (defense in depth)
        exclude: env, beans, heapdump, threaddump, configprops, mappings
  endpoint:
    prometheus:
      enabled: true
    health:
      show-details: when-authorized
      cache:
        time-to-live: 30s
  prometheus:
    metrics:
      export:
        enabled: true
```

`SecurityConfig`는 본 PR로 `/actuator/prometheus`·`/health`·`/info`를 익명 허용하고 그 외 `/actuator/**`는 `denyAll`로 명시 차단하므로 위 yaml 외 추가 작업 불요.

## 검증

```bash
# 1. 200 OK + http_server_requests_seconds_count 라인 확인
curl -s localhost:8080/actuator/prometheus | head -20

# 2. application=thirdtool 공통 태그 확인 (Java 설정에서 부착)
curl -s localhost:8080/actuator/prometheus | grep 'application="thirdtool"' | head -3

# 3. histogram bucket 확인 (P95/P99 계산 기반)
curl -s localhost:8080/actuator/prometheus | grep http_server_requests_seconds_bucket | head -3

# 4. 민감 endpoint 차단 확인 (404 또는 401/403)
curl -i localhost:8080/actuator/env
curl -i localhost:8080/actuator/heapdump
```

회귀 안전망은 `src/test/java/com/example/thirdtool/Common/observability/ActuatorMetricsIntegrationTest`가 동등 케이스 6건으로 보장.

## 관련

- Story 4-1 (Product 0-b 메트릭 Epic 1) — Prometheus scrape endpoint 첫 노출
- Story 2-1 (`MdcLoggingFilter`) — `/actuator/**` 경로는 필터 자체 스킵으로 10초 scrape 노이즈 차단
- Story 3-1 (`GlobalExceptionHandler`) — endpoint 실패 시 ERROR + INTERNAL_ERROR(C500) 응답으로 fallback. PrometheusMeterRegistry 빈 누락 시 500 응답으로 catch됨
- `docs/adr/ADR008.md` — 로깅 인프라 (logback-spring.xml은 commit되지만 application.yml은 `.gitignore` 대상이라는 패턴은 본 케이스에도 동일)
