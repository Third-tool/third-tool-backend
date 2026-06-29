# ADR017: Spring `forward-headers-strategy=native` + `server.shutdown=graceful` 채택 (Story-050)

- **상태**: Accepted
- **날짜**: 2026-06-29
- **관련**: ADR016 (ALB Listener + Target Group) follow-up · Story-050 · `src/main/resources/application.yml` · `src/main/resources/application-prod.yml`

## 컨텍스트

ADR016(Story-049)가 ALB 1개 공유 + Listener 80→443 HTTPS redirect + Target Group `deregistration_delay=30s`를 결정했다. 이 결정의 application 측 정합 부재가 ADR016 §결과·트레이드오프 + ts010-3/ts010-4 트러블슈팅 + ADR016 follow-up "Story-TBD(Spring Forward Headers + Graceful Shutdown) ⚠ 즉시 후속 권장"으로 명시되었다.

본 ADR이 발사하는 두 부재:

### 1. `X-Forwarded-Proto` 미인식

ALB가 HTTPS termination 수행 → ECS Task로는 HTTP(8080) forward + `X-Forwarded-Proto: https` 헤더 전달. Spring Boot **default는 이 헤더를 무시**:

- self-referential URL 생성 시(`UriComponentsBuilder.fromCurrentRequest()`, Spring Security redirect 등) scheme이 `http://`로 잘못 생성 → ALB Listener 80이 다시 `https://`로 redirect → **무한 loop** (ts010-3)
- `jwt.cookie.secure: true` (application-prod.yml line 39)인데 Spring이 HTTPS 인지 못하면 `Set-Cookie: Secure` flag origin scheme mismatch → **브라우저가 쿠키 거부** (ts010-4)

### 2. Spring Boot 3.x default `server.shutdown=immediate`

ADR016이 Target Group `deregistration_delay.timeout_seconds: 30s` 채택한 근거는 "Spring Boot default graceful shutdown 30s와 매칭". 그러나 Spring Boot 3.x **default는 `immediate`** (즉시 SIGTERM 응답):

- ECS rolling update 시 Task가 SIGTERM 받자마자 종료 → ALB가 deregistration_delay 30s 동안 in-flight request 받은 상태로 Task에 라우팅 → **5xx 발생**
- ADR016의 deregistration_delay 30s 결정이 default 동작에 의존 → 명시 필요

## 결정

### 모든 profile 공통 — `application.yml`

```yaml
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s

server:
  shutdown: graceful
```

**근거**:
- `graceful` 활성 시 SIGTERM 수신 후 in-flight request 완료까지 대기 (web request thread만, 신규 요청은 503 응답)
- `timeout-per-shutdown-phase: 30s` — **각 shutdown phase마다 30s** 적용 (Spring Boot 3.x 명세). 단일 phase 30s가 아니라 phase 수만큼 잠재적 누적. 일반 web request 1-phase 가정 시 30s 매칭, executor service 등 추가 phase 존재 시 30s 초과 가능 — ADR016 deregistration_delay 30s는 단일 phase 기준 매칭이며, 다중 phase 시 ECS가 SIGKILL 후속 trigger
- **모든 profile 공통**: local/dev에서 30s 대기가 다소 답답하지만 운영 일관성 우선 — 디버깅 패턴이 production과 동일

### prod 한정 — `application-prod.yml`

```yaml
server:
  forward-headers-strategy: native
```

**근거**:
- `native` 사용 시 Tomcat `RemoteIpValve`가 활성화 → `X-Forwarded-Proto`/`X-Forwarded-Host`/`X-Forwarded-For` 헤더 인식해 `HttpServletRequest.getScheme()` 등이 정확한 값 반환
- 옵션 비교: `native`(Tomcat) vs `framework`(Spring) vs `none`(default)
  - `native` 선택 — Tomcat embedded라 자연스럽고, valve 단계 처리로 application 코드 무영향
  - `framework`는 `ForwardedHeaderFilter` 등록 — 동일 결과지만 추가 filter chain
- **prod 한정**: dev/local은 localhost 직접 접속이라 적용 무관. 명시성 측면에서 prod만 활성

### 본 ADR이 다루지 않는 범위

- **Spring Security HSTS 활성** (`httpStrictTransportSecurity`): HTTPS 강제 강화. 별도 Story
- **`X-Forwarded-For` 기반 client IP 로깅 정합**: LogstashEncoder MDC에 `clientIp` 추가 등 — `logback-spring.xml` 변경 필요. 별도 Story
- **WebSocket / SSE graceful shutdown 처리**: 본 ADR은 일반 web request만 가정. WebSocket 도입 시 별도 검토
- **WebClient (reactor-netty) graceful shutdown 정합성 검증**: 본 프로젝트는 `spring-boot-starter-webflux` 의존(KakaoOAuthClient/NaverOAuthClient용 WebClient 등록). OAuth token 교환 in-flight 시 SIGTERM 수신 시 reactor-netty가 graceful 단계와 어떻게 정합하는지 명시 부재. 현재 OAuth 호출은 로그인 시점 request-scoped + short-lived(~ms 단위)라 운영 위험 낮으나, **별도 Story로 reactor-netty `ConnectionProvider` shutdown 정책 + WebClient timeout 명시 검토**
- **liveness/readiness probe 분리** (Spring Boot Actuator `management.endpoint.health.probes.enabled`): graceful shutdown 중 readiness=DOWN 응답 → ALB Target unhealthy → 자동 deregister. 본 ADR 결정만으로는 graceful 중에도 `/health` Controller가 200 응답 → ALB false-positive healthy 지속 → 신규 요청 라우팅 → 503 — **즉 본 Story로는 "다운타임 0초" 달성 못함**. 별도 Story (`/actuator/health/readiness` 노출 + ALB health check path 전환)와 묶음
- **SecurityConfig `requiresChannel().anyRequest().requiresSecure()` 이중화**: ALB SG 외 진입 경로(예: VPC 내부 service mesh)가 임의 `X-Forwarded-Proto: https` 전송 시 신뢰 표면 증가. 본 ADR은 SG가 ALB만 허용한다는 L3 가정에 의존. application-layer 이중화는 별도 Story
- **graceful shutdown 효과 검증 통합 테스트**: `@SpringBootTest + MockMvc + header("X-Forwarded-Proto","https")` → `request.getScheme() == "https"` 단언으로 native 동작 입증 가능. 본 ADR은 설정만 적용, 입증 통합 테스트는 별도 Story
- **PACKAGE.md / DOMAIN.md 갱신**: 본 Story는 application 설정 변경 — 도메인 의존 방향·도메인 의도 변경 0건. 갱신 불요

## 결과 (Consequences)

### 긍정적

- **ts010-3·ts010-4 application 측 전제 차단**: ALB Listener HTTPS redirect 무한 loop + 쿠키 거부 두 시나리오의 application 측 원인(scheme 미인식) 해결. **단, 실제 발화 차단 입증은 ECS Task 배포 + ALB Listener 통합 환경에서만 검증 가능 — 본 Story는 application 설정 측 전제만 충족**
- **ECS rolling update 5xx 종료 측 0건 차단의 application 측 전제 충족**: graceful + timeout-per-shutdown-phase 매칭으로 SIGTERM 후 in-flight request 완료 보장. **단, 최종 "다운타임 0초" KPI(product-infra-deploy.md)는 본 Story 단독 책임 아님** — `minimumHealthyPercent=100` + `maximumPercent=200` (product Epic 2 §) + `healthCheckGracePeriod=90s` (ADR016) + readiness probe(별도 Story) + 새 Task healthy 도달까지 모두 정합해야 보장
- **운영 일관성**: local/dev에서도 30s graceful shutdown 적용 — prod와 동일 종료 패턴, 디버깅 reproducible
- **Tomcat valve 단계 처리**: application 코드 무영향. `@RestController`/`Filter`에서 `request.isSecure()` 등이 자연스럽게 https 인식
- **ADR016 deregistration_delay 결정의 실효성 확보**: default 동작 의존 → 명시 의존으로 강화

### 효과 발현 시점

본 Story 머지로 application 설정은 즉시 적용되지만, **실제 효과 발현은 다음 ECS Task 배포 시점부터**:
- **staging Task**: Story-047 `SPRING_PROFILES_ACTIVE=prod` 사용 중이라 다음 staging Task 배포 시점에 즉시 발현
- **prod Task**: milestone item #11/#13 (ECS Service 등록 + ALB Target 등록) 완료 후 prod Task 첫 배포 시점에 발현
- 본 Story만 단독 머지된 상태에서는 **dev-cicd.yml EC2 배포에 영향 0건** (EC2는 8080 직접 노출, ALB 미사용)

### 트레이드오프 / 부정적

- **local/dev 종료 시 30s 대기**: `Ctrl+C` 응답이 느려짐. dev 생산성 약간 손실. 우회: 개발자가 local profile에서 `server.shutdown: immediate` override 가능 (application-local.yml에서)
- **WebSocket 미고려**: 현재 WebSocket 미사용 — 영향 없음. 도입 시 별도 graceful 정책 필요
- **`X-Forwarded-*` 헤더 신뢰 표면 증가**: ALB가 신뢰 가능한 upstream이라는 가정. ALB 외 직접 호출(예: VPC 내부 다른 service)이 임의 `X-Forwarded-Proto: https` 전송 시 Spring이 신뢰 → SG로 ALB만 진입 허용(ts009 §7.1 alb-sg → app-sg)이므로 차단됨. 보안 표면 증가는 SG에서 막힘
- **prod 한정 적용으로 staging Task에 미적용**: Story-047 staging Task가 `SPRING_PROFILES_ACTIVE=prod` 사용 중이므로 staging에도 자동 적용됨 (의도된 결과). 향후 `application-staging.yml` 신설 시 staging profile에도 명시 필요 — ADR014 follow-up `application-staging.yml`에 포함
- **30s 초과 query/migration 강제 종료 위험**: `timeout-per-shutdown-phase: 30s` phase 도달 시 in-flight thread interrupt → `@Transactional` rollback. 현 시점 도메인 코드 중 30s 초과 query 없음 (OLTP single-card CRUD 위주). Flyway migration은 부팅 시점 1회로 shutdown phase와 무관. 발견 시 phase timeout 재검토

## 대안 비교

| 대안 | 장점 | 거부 사유 |
| --- | --- | --- |
| **A. `forward-headers-strategy=native` (선택)** | Tomcat valve 단계 — application 무영향, 자연스러움 | Tomcat 외 embedded server(Undertow/Jetty) 사용 시 미적용 — 현재 Tomcat 사용이라 무영향 |
| **B. `forward-headers-strategy=framework`** | Tomcat 외 server 호환 | `ForwardedHeaderFilter` 추가 등록 — filter chain 길어짐, 미미한 부담 |
| **C. `forward-headers-strategy=none` (default 유지)** | 변경 0건 | ts010-3/ts010-4 즉시 발화 — 본 Story 진입 사유 |
| **D. Spring Security `requiresChannel().anyRequest().requiresSecure()` 추가** | HTTPS 강제 application 단 | ALB가 이미 80→443 redirect — 중복. application 단 redirect는 ALB 단보다 비용 큼 |
| **E. `server.shutdown=graceful` 모든 profile (선택)** | 운영 일관성 | local Ctrl+C 응답 30s 지연 — 우회: application-local.yml override 가능 |
| **F. `server.shutdown=graceful` prod 한정** | local dev 친화 | local/dev에서 graceful 테스트 불가 — production-only 동작이 staging까지만 검증됨 |
| **G. `lifecycle.timeout-per-shutdown-phase=10s`** | 빠른 종료 | ADR016 deregistration_delay 30s와 mismatch — 30s 필수 |
| **H. `lifecycle.timeout-per-shutdown-phase=60s`** | 더 안전한 마진 | ADR016 30s와 mismatch — 60s 대기는 과도 |

## 알려진 follow-up

- **`application-staging.yml` 신설** (ADR014 follow-up): staging Task가 prod profile 사용 중인 transitional 해소 시 staging profile에 본 ADR 설정도 명시 적용
- **HSTS** (`httpStrictTransportSecurity`): Spring Security 설정 추가 — HTTPS 재방문 강제
- **`/actuator/health/readiness` 노출 + ALB health check path 전환**: graceful shutdown 중 readiness=DOWN → ALB 자동 deregister. 현재 `/health` 단순 200 → false-positive 위험 (Story-047 ts008-2 + Story-049 ts010-1 known issue) 동시 해결
- **clientIp MDC 추가**: `logback-spring.xml`에 `X-Forwarded-For` 기반 clientIp MDC 필드 추가 — 로그 분석 정합
- **Undertow 전환 시점 검토** (M3+): Tomcat → Undertow 시 `forward-headers-strategy: framework` 전환 필요

## 다시 검토할 시점

- **embedded server 교체 시점**: Tomcat → Undertow/Jetty 전환 시 `native` → `framework` 검토
- **WebSocket / SSE 도입 시점**: graceful shutdown 정책 재설계 (in-flight WebSocket 처리)
- **liveness/readiness probe 도입 시점**: graceful shutdown ↔ readiness=DOWN 연동
- **ALB 외부 직접 호출 발생 시점** (예: VPC 내부 service mesh): `X-Forwarded-Proto` 신뢰 모델 재검토
- **local/dev 종료 지연 불만 누적 시점**: `application-local.yml`에 `server.shutdown: immediate` override 도입
