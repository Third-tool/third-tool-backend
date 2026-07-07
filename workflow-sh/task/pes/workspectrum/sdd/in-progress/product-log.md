## [Product] 운영 관측성의 첫 레이어 — 구조화 로깅 · MDC 컨텍스트 · 에러 로깅 표준

# [Product] 운영 관측성의 첫 레이어 — 구조화 로깅 · MDC 컨텍스트 · 에러 로깅 표준

## Product Vision

> ThirdTool의 모든 런타임 동작(요청·예외·외부 호출·배치)이 남기는 로그를 JSON 구조와 MDC 기반 traceId 위에 올린다.
동시 요청이 섞여도 한 요청의 흐름을 traceId 하나로 추적할 수 있고, 예외 유형이 적절한 로그 레벨로 자동 분류되는 상태를 만든다.
후속 Product(모니터링·부하 테스트)에서 "지표가 튀는 순간 어떤 요청에서 무슨 일이 있었는지"를 즉시 역추적할 수 있도록, 관측성의 가장 아래 레이어를 안정적으로 깐다.
>

## 배경 및 문제

- 현재 상황 (As-Is)
    - 로그가 Spring Boot 기본 평문 포맷으로 출력되고 있어 `grep` 외에 검색·집계 수단이 없다
    - 요청 단위 식별자(traceId/requestId)가 없어 동시 요청 2개가 발생하면 어느 로그가 어느 요청의 것인지 구분 불가능
    - 인증 컨텍스트(userId)가 로그에 자동으로 실리지 않아, 특정 유저 신고 시 해당 유저의 행동 흐름을 따라가려면 시간 + 경로 + 상태코드를 수동 조합해야 한다
    - `@RestControllerAdvice`의 GlobalExceptionHandler가 존재하지만 모든 예외를 동일하게 `log.error(...)`로 찍고 있어, 사용자 검증 실패(4xx)와 실제 시스템 장애(5xx)가 ERROR 로그에 섞여 있다
    - 민감정보 로깅 정책이 코드 컨벤션으로만 존재하고 문서화되지 않아, 신규 기능 추가 시 비밀번호·토큰이 로그에 노출될 위험이 잠재 상태
- 발생하는 문제
    - 후속 Product인 k6 부하 테스트에서 "P95가 800ms 튀는 순간"을 발견해도, 그 시점의 어떤 요청이 원인인지 로그에서 골라낼 수 없다 — 관측성 단절
    - ReviewSession / 카드 만료 배치에서 예외가 발생했을 때 재현에 필요한 컨텍스트(어느 유저의 어느 세션이었는지)가 로그에 없다
    - ERROR 로그가 진짜 5xx 장애와 단순 검증 실패를 구분 못 하는 상태에서 Product 1의 알림(v2)을 붙이면 알림 신호가 노이즈에 묻혀 무력화된다
    - 토이 프로젝트라도 채용 포트폴리오에서 "로그 보면 운영 관점 있는지 5초 만에 드러난다" — 평문 + ERROR 남발은 즉시 감점 신호
- 왜 지금 해결해야 하는가
    - Product 1(모니터링) · Product 2(부하 테스트)가 traceId 기반 상관관계 분석을 전제로 설계된다 — 순서를 바꾸면 부하 테스트 결과 해석 단계에서 막힌다
    - 로그 포맷은 한 번 prod에서 굳어지면 호환성 때문에 바꾸기 어렵다 (수집기·대시보드·알림 룰이 포맷에 의존하게 됨). 첫 트래픽이 들어오기 전 표준화가 가장 싸다
    - GlobalExceptionHandler의 로그 레벨 정책은 예외 클래스가 추가될 때마다 새 결정이 들어가야 하는데, 정책 없이 코드만 늘어나면 일관성이 깨진 채 굳는다
    - 면접에서 "로그 레벨은 어떤 기준으로 정했나, 요청 추적은 어떻게 했나, 비밀번호 로깅은 어떻게 막았나"는 단골 질문이다

## 목표 (To-Be)

- 모든 prod 로그가 JSON 한 줄 포맷으로 출력되고 `timestamp · level · logger · message · traceId · requestId · userId · exception` 필드를 표준 스키마로 갖는다
- 모든 HTTP 요청에 requestId가 자동 부여되고, 인증된 요청에는 userId가 함께 MDC에 실린다
- 응답 헤더 `X-Request-Id`로 클라이언트도 동일 식별자로 문의할 수 있다
- 동시 요청이 섞여도 로그의 requestId가 교차 오염되지 않는다 (스레드 풀 재사용 시 MDC clear 보장)
- GlobalExceptionHandler가 예외 유형별로 ERROR / WARN / INFO 레벨을 자동 분리한다 — ERROR는 진짜 5xx만
- 비밀번호·accessToken·refreshToken은 어떠한 경로로도 로그에 출력되지 않는다
- 로그 레벨 사용 가이드(`docs/logging.md`)가 코드베이스에 존재해, 신규 기능 추가 시 가이드만 보고 레벨 결정이 가능하다

## 설계 결정 (Design Decision)

> **Logback + logstash-logback-encoder를 채택한다. Log4j2로 갈아타지 않는다.**
Spring Boot 기본 스택 위에서 JSON 포맷터만 얹는 최소 침습 접근.
>
> - Spring Boot 기본 로깅 스택이 Logback이고, `logstash-logback-encoder`는 production 검증된 안정적인 JSON 인코더
> - Log4j2가 비동기 로깅에서 더 빠르다는 벤치마크가 있으나, 토이 프로젝트 트래픽에서는 차이가 무의미하며 의존성 추가·CVE 관리 부담만 늘어남
> - dev 프로파일은 평문 콘솔 포맷 유지(개발자 가독성), prod/stage는 JSON — 한 `logback-spring.xml`에서 profile 분기로 처리
> - 이 결정은 ADR로 별도 기록한다 (`ADR-LOG-001: Logging Stack Selection`)

> **MDC 기반 requestId 전파를 채택하고, OpenTelemetry / 분산 트레이싱은 도입하지 않는다.**
단일 서비스(모놀리스) 구조에서 분산 트레이싱은 비용 대비 가치가 낮다.
>
> - 현재 시스템은 EC2 단일 인스턴스 + RDS 구조로 서비스 경계가 1개. trace context를 전파할 외부 서비스 호출도 Toss Payments 정도뿐
> - MDC만으로도 단일 프로세스 내부 요청 추적은 완전히 커버됨. 외부 호출 상관관계가 필요해지는 시점(여러 서비스로 쪼개질 때)에 OpenTelemetry 도입
> - traceId 필드명은 OpenTelemetry 표준(`traceId`)을 미리 따라가 향후 마이그레이션 비용을 낮춘다 (필드명만 호환되면 수집기 룰 재사용 가능)
> - 이 결정은 ADR로 별도 기록한다 (`ADR-LOG-002: Tracing Scope`)

> **GlobalExceptionHandler에서 예외 유형별 로그 레벨을 강제 분리한다.**
ERROR는 즉시 대응이 필요한 신호로만 예약한다.
>
> - **ERROR**: 예상치 못한 5xx (NPE · DB 연결 실패 · 외부 API 타임아웃 등) → stack trace 포함, 향후 알림 대상
> - **WARN**: BusinessException 계열 (재고 부족 · 만료 토큰 · 권한 없음 등) → 예상 가능한 비정상, stack trace 미포함
> - **INFO**: 4xx 검증 실패 (`MethodArgumentNotValidException` · `ConstraintViolation`) → 메시지만
> - **DEBUG**: 외부 API 요청/응답 본문 (prod 비활성)
> - 이유: Product 1의 알림(v2)을 ERROR에 붙였을 때 신호가 노이즈에 묻히지 않게 하기 위함. "ERROR 로그 = 사람이 봐야 할 것"이 깨지면 알림 자체가 무의미해짐
> - 이 결정은 ADR로 별도 기록한다 (`ADR-LOG-003: Exception-to-LogLevel Mapping`)

> **민감정보는 application 레이어에서 차단한다. 자동 마스킹 어노테이션은 v2.**
v1은 "절대 로그에 안 들어가도록" 코드 컨벤션 + 문서로 강제.
>
> - 자동 마스킹(`@Loggable` 커스텀 어노테이션 · 리플렉션 기반 PII 필터)은 토이 규모에 오버 엔지니어링
> - 대신 다음 3개 규칙을 docs/logging.md에 강제 명시:
    >     1. `User` 엔티티 `toString()`은 비밀번호 필드 제외 (`@ToString.Exclude`)
    >     2. `LoginRequest` DTO는 `toString()` 오버라이드로 password 필드를 `**`로 출력
>     3. JWT 토큰은 prefix 8자 + `**`로만 로깅 (디버깅 식별용)
> - 단위 테스트로 "비밀번호 필드가 포함된 요청 검증 실패 시 비밀번호 값이 로그에 노출되지 않는다"를 보장

## 대안 검토 (Alternatives Considered)

> 큰 갈림길마다 "왜 이것이 아니고 저것인가"를 남긴다. 거부된 안에도 합리적 근거가 있었음을 보임으로써 현재 선택의 트레이드오프를 명확히 한다.

### 로그 포맷

**Option A — Spring Boot 기본 평문 유지**
- 장점: 추가 의존성 없음. 로컬 콘솔에서 즉시 읽힘
- 거부 이유:
    - `grep` 외 검색 수단 부재. 수집기(Loki/CloudWatch Insights/ELK)는 모두 JSON 키 단위 인덱싱을 전제
    - 멀티라인 스택트레이스가 라인 단위로 깨져 수집기에서 파싱 오류
    - traceId/userId를 텍스트에 박아도 파싱 룰을 별도 작성해야 함 — 포맷이 바뀌면 룰 전체 재작성

**Option B — ECS (Elastic Common Schema) 풀 호환**
- 장점: Elastic 생태계와 1:1 매핑. 향후 ELK 도입 시 추가 매핑 불필요
- 거부 이유:
    - 필드명이 길고(`@timestamp`, `log.level`, `service.name`...) 토이 규모에선 가독성 손해가 큼
    - Elastic 종속 — Loki/CloudWatch로 갈 때 다시 매핑해야 함
    - 운영 환경 백엔드가 미정인 시점에 한 생태계에 고정하는 것은 이르다

**Option C (선택) — LogstashEncoder JSON + OpenTelemetry 호환 필드명**
- 비용: ECS 완전 호환은 아니라 ELK 도입 시 필드 매핑 작업 1회 발생
- 보상: `traceId`/`spanId`는 OTel 표준명으로 미리 정렬 — 향후 분산 트레이싱 도입 시 무비용 합류. 수집 백엔드는 JSON만 받으면 됨
- 트레이드오프 수용 근거: 수집 백엔드 미확정 + 모놀리스 단일 서비스. "포맷 표준 → 백엔드 선택 가능" 순서가 가역성이 가장 높다

### 로그 라이브러리

**Option A — Log4j2 (비동기 LMAX Disruptor)**
- 장점: 비동기 처리량 벤치마크에서 Logback 대비 우위
- 거부 이유:
    - Spring Boot 기본 스택이 Logback — 의존성 교체에 따른 `spring-boot-starter-logging` exclude 부담
    - Log4Shell 사후 CVE 모니터링 비용. 토이 프로젝트에 보안 패치 부담 정당화 불가
    - 토이 트래픽(0 RPS)에서 비동기 처리량 차이는 측정 불가능

**Option B — java.util.logging (JUL)**
- 거부 이유: 설정 표현력 부족, 외부 라이브러리(`slf4j-jdk14`) 어댑터 필요, 커뮤니티 노하우 빈약

**Option C (선택) — SLF4J Facade + Logback (Spring Boot 기본 + LogstashEncoder)**
- 비용: 비동기 처리 시 `AsyncAppender` 추가 설정 필요 (v2)
- 보상: 의존성 추가 0건. CVE 대응은 Spring Boot BOM이 끌고 옴

### profile별 포맷 분기

**Option A — 모든 환경 JSON 통일**
- 장점: 환경 간 drift 0. local에서 prod 로그 형태를 미리 검증 가능
- 거부 이유: 로컬 개발 중 한 줄 JSON은 사람이 읽기 어렵다. 개발 속도 저하

**Option B (선택) — local 평문 / dev·prod JSON, 한 `logback-spring.xml` 안에서 `<springProfile>` 분기**
- 비용: 환경 간 미세 drift 가능성 (예: 평문에는 들어가지만 JSON에는 빠진 필드)
- 보상: 개발자 경험 + 운영 호환을 동시에. 별도 파일 분리(`logback-{profile}.xml`)는 drift 위험이 더 크다고 판단해 단일 파일 분기로 한정

### MDC 컨텍스트 수집 시점

**Option A — Spring Interceptor**
- 거부 이유: 인증 실패 / Spring Security 필터 단에서 발생한 예외는 Interceptor 도달 전에 GlobalExceptionHandler로 빠짐 → requestId 없는 로그가 인증 실패 경로에서 생성됨

**Option B — AOP `@Aspect`**
- 거부 이유: HTTP 요청 경계와 메서드 경계가 일치하지 않음. 비동기·배치 진입점마다 별도 `@Pointcut` 정의 필요

**Option C (선택) — Servlet Filter (OncePerRequestFilter), FilterChain 최상단**
- 비용: Spring Security FilterChain 안 어느 지점에 끼울지 명시 설정 필요 (`addFilterBefore(mdcLoggingFilter, JWTFilter.class)`)
- 보상: 인증 실패 경로 포함 모든 HTTP 진입에서 requestId가 살아 있음. `finally` 블록에서 `MDC.clear()` 호출 위치가 단일

### traceId 생성 / 전파

**Option A — Spring Cloud Sleuth (현 Micrometer Tracing)**
- 거부 이유: Spring Cloud BOM 합류 비용. 단일 서비스에서 Sleuth가 제공하는 부가가치(분산 trace 전파) 활용처 없음

**Option B — OpenTelemetry Java Agent 자동 계측**
- 거부 이유: 외부 호출이 Toss Payments 정도뿐인 시점에 OTel Collector 인프라 구축 정당화 어려움. Agent attach + Collector + 백엔드(Tempo/Jaeger) 3종 셋업 = 본 Product 범위 초과

**Option C (선택) — 자체 UUID + MDC 필드명만 OTel 표준 호환 (`traceId`)**
- 비용: 실제 W3C `traceparent` 헤더 파싱·전파는 미구현
- 보상: 필드명이 표준이라 v2에서 OTel 도입 시 수집 룰·대시보드 무수정. 단일 서비스에서 traceId == requestId로 v1 단순화 (의도적 동일값)

### 에러 로그 표준 (스택트레이스 정책)

**Option A — 모든 예외에 stack trace 항상**
- 거부 이유: BusinessException은 예상 가능한 정상 흐름의 일부. 매 호출마다 50줄 스택을 찍으면 진짜 5xx 신호가 묻힘 + 로그 볼륨 폭증

**Option B — BusinessException은 message만, fallback Exception은 stack 포함 (분기)**
- 채택. `GlobalExceptionHandler` 진입 직후 로그 레벨로 강제 (ERROR/WARN/INFO)
- 비용: 신규 예외 추가 시마다 `@ExceptionHandler` 분기 필요 — fallback `Exception.class`에 의존하지 않는다는 컨벤션
- 보상: ERROR == 사람이 봐야 할 신호 불변식 유지. Product 1 알림(v2) 연결 시 노이즈 차단의 직접 수단

### 민감 정보 마스킹

**Option A — 미들웨어 자동 마스킹 (정규식 sanitizer)**
- 거부 이유: 정규식 false positive(키워드 "password"가 컨텐츠에 등장)·false negative(필드명 변형)가 둘 다 발생. 신뢰 못 함

**Option B — `@Loggable` 커스텀 어노테이션 + 리플렉션**
- 거부 이유: 토이 규모에서 오버 엔지니어링. 어노테이션 누락 시 노출 위험은 정규식과 동일하게 잔존

**Option C (선택) — DTO `toString()` 직접 정비 + 단위 테스트 강제**
- 비용: 새 DTO 추가 시 마다 컨벤션 적용 책임 (PR 리뷰에서 catch)
- 보상: 검증 가능 (단위 테스트로 "password 값이 toString 결과에 포함되지 않는다"). v2 자동 마스킹 도입 시 본 패턴을 검증 케이스로 재사용

### 로그 수집 백엔드 (v2)

**보류 옵션 비교**: CloudWatch Logs (AWS 종속, EC2와 동일 계정으로 IAM 단순) vs Loki+Grafana (오픈소스, 자체 호스팅 비용) vs ELK (가장 풍부한 검색, 운영 비용 가장 큼).
- 본 Product에서는 **선택하지 않는다**. 단, JSON 포맷·OTel 필드명 정렬은 세 옵션 모두 무수정 수용하도록 설계
- 결정 시점: 트래픽 발생 또는 Product 1(모니터링) 진입 시

## 전체 아키텍처 (High-Level Architecture)

> 컴포넌트 다이어그램과 핵심 플로우. 본문 5페이지보다 다이어그램 1장이 더 강하다.

### 컴포넌트 배치

```
[Client]
   │
   │  HTTP Request (선택적 X-Request-Id 헤더 포함)
   ▼
┌───────────────────────────────────────────────────────────┐
│  Servlet FilterChain (Spring Security)                    │
│  ┌──────────────────────────────────────────────────────┐ │
│  │ 1. MdcLoggingFilter (OncePerRequestFilter, 최상단)   │ │
│  │    ├─ requestId 결정 (헤더 echo / UUID 생성)         │ │
│  │    ├─ MDC.put: requestId / traceId / method / path   │ │
│  │    ├─ Response.setHeader("X-Request-Id", ...)        │ │
│  │    ├─ log.info("request.start ...")                  │ │
│  │    └─ finally: log.info("request.end ...") + clear() │ │
│  └──────────────────────────────────────────────────────┘ │
│              │ chain.doFilter                              │
│              ▼                                             │
│  ┌──────────────────────────────────────────────────────┐ │
│  │ 2. BlockListFilter                                   │ │
│  └──────────────────────────────────────────────────────┘ │
│              │                                             │
│              ▼                                             │
│  ┌──────────────────────────────────────────────────────┐ │
│  │ 3. JWTFilter                                         │ │
│  │    인증 성공 분기 → MDC.put("userId", ...)           │ │
│  │    인증 실패 → request.setAttribute("authError", ec) │ │
│  └──────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘
              │
              ▼
┌───────────────────────────────────────────────────────────┐
│  DispatcherServlet → Controller → Service                 │
│              │                                             │
│              │  log.info(...) / log.warn(...) — SLF4J     │
│              ▼                                             │
│  org.slf4j.Logger  (ch.qos.logback.classic.Logger)        │
└───────────────────────────────────────────────────────────┘
              │
              ▼  (예외 발생 시)
┌───────────────────────────────────────────────────────────┐
│  GlobalExceptionHandler (@RestControllerAdvice)           │
│  ├─ BusinessException  → log.warn  + WARN 레벨            │
│  ├─ MethodArgNotValid  → log.info  + INFO 레벨            │
│  ├─ AccessDenied       → log.warn  (Story 5-3 기 구현)    │
│  └─ Exception fallback → log.error + stack trace          │
│  + MDC.put("errorCode", ec.getCode())                     │
└───────────────────────────────────────────────────────────┘
              │
              ▼
┌───────────────────────────────────────────────────────────┐
│  Logback Pipeline                                         │
│  ├─ <springProfile name="local"> → ConsoleAppender 평문   │
│  └─ <springProfile name="dev,prod"> → LogstashEncoder JSON│
│     ├─ customFields: {"application":"thirdtool"}          │
│     └─ includeMdcKeyName: traceId/requestId/userId/...    │
└───────────────────────────────────────────────────────────┘
              │
              ▼  stdout (한 줄 JSON)
              │
              ▼
       [ 수집 백엔드 — v2, 본 Product에서 미선택 ]
       CloudWatch Logs  /  Loki + Promtail  /  ELK
```

### 핵심 플로우

**1. 정상 인증 요청 — 로그 흐름**

```
Client ─GET /api/v1/cards (Cookie: access_token)─►
   MdcLoggingFilter.doFilterInternal
      ├─ MDC.put requestId=8f3c-... traceId=8f3c-... method=GET path=/api/v1/cards
      ├─ Response.setHeader X-Request-Id: 8f3c-...
      ├─ log.info("request.start clientIp=...")  ─► JSON 라인 1 (requestId 포함)
      │
      └─ chain.doFilter
            JWTFilter (인증 성공)
               └─ MDC.put userId=1042                 ─► 이후 모든 로그에 userId 자동 포함
                  Controller.list ─ log.info(...)     ─► JSON 라인 2 (userId 포함)
                  Service.execute ─ log.debug(...)    ─► JSON 라인 3 (prod 미출력)
            return 200
   finally
      ├─ log.info("request.end status=200 durationMs=42") ─► JSON 라인 4
      └─ MDC.clear()                                       ─► 스레드 풀 재사용 누수 방지
```

**2. BusinessException 발생 — 4xx/409 분기**

```
Controller throws CardKeywordMinRequiredException (BusinessException)
   │
   ▼
GlobalExceptionHandler.handleBusiness
   ├─ MDC.put errorCode=CARD_KEYWORD_MIN_REQUIRED
   ├─ log.warn("business.exception code={} message={}")  ─► WARN, stack 미포함
   └─ return ResponseEntity(status=ec.status, body=ErrorResponse)
   │
   ▼  (필터 finally로 복귀)
MdcLoggingFilter.finally
   ├─ log.info("request.end status=400 durationMs=15")
   └─ MDC.clear()
```

**3. 예상치 못한 5xx — fallback 경로**

```
Service throws NullPointerException
   │
   ▼
GlobalExceptionHandler.handleUnknown
   ├─ MDC.put errorCode=INTERNAL_ERROR
   ├─ log.error("unexpected.exception", ex)  ─► ERROR + stack_trace 필드
   └─ return 500
```
ERROR 라인은 Product 1의 알림(v2)이 구독하는 채널. WARN/INFO는 알림 대상에서 배제.

### Out-of-Process 의존

- **stdout** — 모든 로그의 1차 전송 채널. EC2/컨테이너 stdout이 호스트 로그 드라이버에 캡쳐됨
- **(v2) 수집 백엔드** — CloudWatch Logs Agent 또는 Promtail/Fluent Bit. 본 Product 범위 외
- **MySQL / 외부 API** — 본 Product가 직접 의존하지 않음. 단, 외부 API 호출 본문의 DEBUG 로깅 컨벤션은 docs/logging.md가 강제

### 핵심 컴포넌트

| 컴포넌트 | 위치 | 책임 |
| --- | --- | --- |
| `MdcLoggingFilter` | `Common/logging/` (신규) | requestId 결정·MDC 주입·응답 헤더 echo·`MDC.clear()` |
| `JWTFilter` (기존, 수정) | `Common/security/auth/filter/` | 인증 성공 시 `MDC.put("userId", ...)` 추가 |
| `GlobalExceptionHandler` (기존, 정비) | `Common/Exception/` | 예외 유형별 로그 레벨 분기 + `MDC.put("errorCode", ...)` |
| `logback-spring.xml` (기존, 확장) | `src/main/resources/` | profile별 Appender + MDC 화이트리스트 + 로거별 레벨 |
| `docs/logging.md` (신규) | `docs/` | 표준 스키마·레벨 정책·민감정보 금지 항목 가이드 |

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 로깅 인프라 실패 시나리오

| 시나리오 | 영향 | 완화 / 대응 |
| --- | --- | --- |
| stdout 버퍼 오버플로 (트래픽 폭증) | 라인 단위 drop, 일부 로그 유실 | 단일 컨테이너는 OS 기본 buffer로 충분. v2에서 `AsyncAppender` + `discardingThreshold=0` 도입 검토 |
| MDC 누수 (스레드 풀 재사용) | 이전 요청의 userId가 다음 요청에 잘못 기록 → 감사 추적 불가 | `MdcLoggingFilter.finally`의 `MDC.clear()` 강제 + 단위 테스트로 보장 (Story 2-1 AC) |
| 비동기 스레드(`@Async`)에서 traceId 끊김 | 비동기 작업 로그에 requestId 부재 | v1 범위: 비동기 진입점에 명시적 `MDC.getCopyOfContextMap()` 전달 컨벤션 (docs/logging.md). v2: `TaskDecorator` 자동 전파 |
| 민감정보 누출 (신규 DTO에 toString 누락) | 비밀번호·토큰이 prod JSON에 노출 → 사고 | 단위 테스트 `SensitiveDataLoggingTest`가 신규 DTO 추가를 catch하지 못함 → PR 체크리스트에 명시 (Epic 4) |
| 로그 볼륨 폭주 (DEBUG가 prod에서 켜짐) | 디스크/네트워크 비용 + 수집기 처리 지연 | logback-spring.xml `<springProfile name="prod">`에서 `com.example.thirdtool: INFO` 강제 + Hibernate SQL: WARN, bind: OFF |
| LogstashEncoder NPE / JSON 파싱 에러 | Encoder 자체 예외로 로그 라인 drop | 의존성을 Spring Boot BOM 호환 안정 버전(8.x)으로 고정. 부팅 시 단위 테스트로 1라인 JSON 출력 검증 |
| 수집 백엔드 다운 (v2) | stdout은 정상이지만 수집기로 흐르지 않음 | v2 책임. 본 Product는 stdout까지가 책임선 — "stdout이 SoT" 원칙 |
| `MDC.put`에 큰 객체(예: 컬렉션) 삽입 | JSON 라인 비대화, 수집기 파싱 부담 | 화이트리스트로 차단 — 등록되지 않은 키는 JSON에 노출되지 않음 (Epic 1 AC) |
| traceId 헤더에 악성 입력(128자 초과·CRLF) | 응답 헤더 인젝션 / 로그 인젝션 | `resolveRequestId`에서 길이 제한 + 비정상이면 서버 UUID로 대체 (Story 2-1 AC) |

### 관측 지표 (v2 — Product 1 합류 시)

| 지표 | 형식 | 의미 |
| --- | --- | --- |
| `log_lines_total{level}` | 카운터 (수집기 산출) | INFO/WARN/ERROR 비율. ERROR 급증 시 알림 |
| `error_log_rate_5m` | 게이지 | 5분 평균 ERROR 라인/분. 임계 초과 시 알림 |
| `traceid_coverage_pct` | 게이지 | HTTP 요청 로그 중 requestId 보유 비율. 100% 미만이면 누수 의심 |
| `userid_coverage_pct` | 게이지 | 인증 요청 로그 중 userId 보유 비율 |
| `avg_log_line_bytes` | 게이지 | 평균 라인 크기. 폭증 시 stack trace 남발 또는 MDC 화이트리스트 위반 의심 |
| `log_retention_days` | 정적 설정값 | 보존 정책 준수 확인 (목표: 30일 v1 / 90일 v2) |

### 로깅 정책 요약

- **항상 기록**: 요청 시작·종료 (INFO, requestId+method+path+status+durationMs), 인증 실패 (WARN, ErrorCode)
- **레벨별 분류**: ERROR=5xx만 / WARN=BusinessException·AccessDenied / INFO=4xx 검증·정상 흐름 / DEBUG=외부 API 본문(prod 비활성)
- **절대 금지**: 비밀번호 원문, AccessToken·RefreshToken 원문, JWT payload 전체, 이메일 본문, 한국어 농담 문자열

### MDC 화이트리스트 확장 — 후행 Product 요청 사항

다음 3~7개 필드는 후속 도메인 SDD 진입 시 화이트리스트에 추가되어야 한다. Epic 1(화이트리스트 확정) DoD 에 병합.

| 필드 | 요청 Product | 목적 |
| --- | --- | --- |
| `axis_id` | `product-learning-tower.md` (Epic 4·6) | Card·Review 로그 축 스코프 필터 |
| `layer_id` | `product-learning-tower.md` (Epic 2·6) | Layer 스코프 리뷰·진행률 로그 필터 |
| `review_session_scope` | `product-learning-tower.md` (Epic 6) | Review 세션 `AXIS`/`LAYER` 구분 로그 |
| `port_type` | `product-ai-suggestion.md` (Epic 6) | 4-Port 어떤 Port 호출 로그 |
| `provider` | `product-ai-suggestion.md` (Epic 6) | Static/LLM 어댑터 구분 로그 |
| `role` | `product-ai-suggestion.md` (Epic 6) | Role Catalog 감지 결과 로그 |
| `session_id`, `session_state` | `product-ai-interactive-roadmap.md` (Epic 5) | RoadmapInteractionSession 상태 추적 |

카디널리티는 낮음(enum · Long ID 로그 필드용, 지표 라벨 아님). 라벨로는 절대 승격하지 않음 — `product-op.md` 카디널리티 방침 참조.

## 롤아웃 / 마이그레이션 (Rollout)

### 전제 — 프로덕션 트래픽 없음 + 로깅 인프라 ADR008 일부 선반영

`logback-spring.xml`이 ADR008로 이미 일부 깔려 있다 (LogstashEncoder + profile 분기 + MDC 화이트리스트). 본 Product는 **그 위에 MDC 주입 주체·에러 로깅 정책·민감정보 차단·운영 가이드를 얹는다**. 따라서 "포맷 전환" 단계는 비용이 작고, "MDC 주입과 예외 핸들러 정비"가 실질 작업.

### Epic 의존성 그래프

```
Epic 1 (JSON 포맷 — ADR008 기 반영 + 화이트리스트 확정)
   Story 1-1 ─┐
              │
              ▼
Epic 2 (MDC 컨텍스트 전파)
   Story 2-1 (MdcLoggingFilter) ──► Story 2-2 (userId 주입)
              │
              ▼
Epic 3 (에러 로깅 표준화)
   Story 3-1 (GlobalExceptionHandler 레벨 분리 + 민감정보 차단)
              │
              ▼
Epic 4 (운영 가이드)
   Story 4-1 (docs/logging.md + README + PR 체크리스트)
```

순서가 강제되는 이유:
- Epic 1의 MDC 화이트리스트가 없으면 Epic 2가 키를 put해도 JSON에 노출되지 않음
- Epic 2의 userId·errorCode 화이트리스트 등록이 선행되어야 Epic 3 ExceptionHandler가 `MDC.put("errorCode", ...)`로 가치 발생
- Epic 4는 Epic 1~3의 표준이 코드로 굳은 후 작성해야 가이드가 코드와 일치

### 단계별 전환 (트래픽 발생 후 가설)

현재는 트래픽 0명 → 일괄 전환 OK. 만약 prod 트래픽 + 기존 평문 로그가 수집되고 있는 환경이었다면:
- 1주차: `local` 평문 유지 + `prod`는 JSON과 평문을 동시 출력 (`<root>`에 두 appender)
- 2주차: 수집기 파싱 룰을 JSON로 전환, 대시보드·알림 룰 마이그레이션
- 3주차: prod에서 평문 appender 제거

본 Product에서는 이 3단계를 생략하나, **패턴은 v2에서 수집 백엔드 전환(CloudWatch ↔ Loki ↔ ELK) 시 그대로 재활용**한다.

### 환경별 설정 분기

- `application.yml` 공통 + logback-spring.xml `<springProfile>` 분기로 일원화 (ADR008)
- `local`: 평문 + `com.example.thirdtool: DEBUG`, `org.hibernate.SQL: DEBUG`, JDBC bind: TRACE
- `dev`: JSON + `com.example.thirdtool: DEBUG`
- `prod`: JSON + `com.example.thirdtool: INFO`, `org.hibernate.SQL: WARN`, JDBC bind: **OFF** (PII 방어)

### 롤백 계획

각 Epic은 단일 PR로 격리. 문제 발생 시 직전 커밋으로 revert 가능.
- Epic 1 revert: logback-spring.xml만 이전 버전으로 되돌리면 됨 (의존성은 build.gradle에 남아도 무해)
- Epic 2 revert: `MdcLoggingFilter` 제거 + SecurityConfig 등록 해제 → MDC 필드만 사라지고 로그 라인은 정상
- Epic 3 revert: GlobalExceptionHandler를 이전 단일 `log.error` 형태로 되돌림 — 응답 형식은 불변
- Epic 4 revert: 문서만 제거. 코드 영향 없음

## 성공 지표 (KPI)

| 지표 | 현재 값 | 목표 값 | 측정 방법 |
| --- | --- | --- | --- |
| prod 로그 JSON 포맷 비율 | 0% | 100% | prod 로그 샘플 100건 수동 검사 |
| traceId(requestId) 포함 비율 (HTTP 요청 로그) | 0% | 100% | `grep -v requestId` 결과 0건 |
| userId 포함 비율 (인증된 요청 로그) | 0% | 100% | 인증 요청 샘플 50건 검사 |
| 동시 요청 traceId 교차 오염 | 측정 불가 | 0건 | 동시 요청 격리 단위 테스트 |
| ERROR 로그 중 실제 5xx 비율 | ~30% (추정) | 95% 이상 | 1주일 운영 후 로그 분류 |
| 민감정보(비밀번호·토큰) 노출 건수 | 측정 불가 | 0건 | 마스킹 단위 테스트 + 로그 샘플 검사 |
| docs/logging.md 신규 예외 추가 가이드 적용 | — | 가이드만 보고 레벨 결정 가능 | 신규 예외 1건 추가 시 PR 리뷰 |
| 응답 헤더 X-Request-Id 포함 비율 | 0% | 100% | 응답 헤더 샘플 검사 |

## Scope

- **In Scope**
    - `logstash-logback-encoder` 도입 및 `logback-spring.xml` profile별 분기 (local 평문 / dev·prod JSON)
    - `MdcLoggingFilter` 구현 (requestId 생성·전파·clear, 응답 헤더 echo back)
    - 인증 후 userId의 MDC 주입 (JWTFilter 체인 이후)
    - `GlobalExceptionHandler` 정비 (예외 유형별 로그 레벨 자동 분리)
    - 민감정보 차단을 위한 DTO `toString()` 정비 (User · LoginRequest · JWT 처리부)
    - 로그 레벨 사용 가이드 문서화 (`docs/logging.md`) 및 README 링크
    - 동시 요청 격리·MDC 누수 방지 단위 테스트
- **Out of Scope**
    - Loki + Promtail · ELK · CloudWatch Logs 등 로그 수집 인프라 (Product 1 또는 v2)
    - OpenTelemetry · Zipkin · Jaeger 등 분산 트레이싱 (v2)
    - `@Loggable` 커스텀 어노테이션 기반 자동 마스킹 (v2)
    - 로그 기반 알림 룰 (Loki Alerts · CloudWatch Log Insights — v2)
    - Audit Log (감사 로그) — 별도 도메인 요구사항이라 분리 (v2)
    - 외부 호출(Toss Payments 등) trace context 전파 — 분산 트레이싱 도입 시점에 함께 (v2)

## 대상 사용자

- 주요 사용자: ThirdTool 백엔드 개발자 (1인 운영)
- 사용 맥락:
    - 신규 API · 신규 예외 추가 시 → `docs/logging.md`를 보고 로그 레벨 결정
    - 운영 중 에러 신고 접수 시 → 클라이언트가 전달한 `X-Request-Id`로 prod 로그를 traceId 검색
    - Product 2 부하 테스트 결과 분석 시 → Grafana에서 P95 튀는 시각 확인 → 같은 시각 로그를 userId·requestId로 추적
    - 면접·포트폴리오 설명 시 → "예외별 로그 레벨 정책"·"동시 요청 격리 테스트"를 근거 자료로 인용

## 연결된 Epic 목록

- [ ]  Epic 1. JSON 구조화 로그 포맷 — `logstash-logback-encoder` 도입 · profile별 Appender 분기 · 표준 필드 스키마 확정
- [ ]  Epic 2. MDC 요청 컨텍스트 전파 — `MdcLoggingFilter` · requestId 생성·echo back · userId 주입 · 동시성 안전성 보장
- [ ]  Epic 3. 에러 로깅 표준화 — `GlobalExceptionHandler` 예외별 로그 레벨 자동 분리 · 민감정보 차단
- [ ]  Epic 4. 운영 가이드 문서화 — `docs/logging.md` · README 링크 · PR 템플릿 체크리스트

## 관련 문서

- 상위 문서: ThirdTool 도메인 모델링 / 백엔드 컨벤션
- 선행 Product: 없음 (관측성 레이어의 가장 아래)
- 후속 Product:
    - Product 1 (모니터링 기반 구축) — Grafana 메트릭과 traceId 상관관계 분석의 입력
    - Product 2 (부하 테스트 & 병목 분석) — "지표가 튀는 순간의 요청"을 traceId로 역추적
- 참고 자료: Logback 공식 문서 · `logstash-logback-encoder` README · OpenTelemetry Semantic Conventions (필드명 호환 목적)
- ADR 후보: `ADR-LOG-001` Logging Stack Selection / `ADR-LOG-002` Tracing Scope / `ADR-LOG-003` Exception-to-LogLevel Mapping
- 기 채택 ADR: [`ADR008`](../../../../../docs/adr/ADR008.md) — 로깅 인프라 (profile별 Appender + LogstashEncoder + MDC 화이트리스트)

## 열린 질문 (Open Questions)

> 아직 정해지지 않은 항목. 정직하게 남겨두고, 결정되면 ADR로 이관한다. "다 정해진 척"하는 SDD가 가장 의심스럽다.

- **OpenTelemetry 도입 시점** — 외부 호출이 Toss Payments 1건뿐인 현재는 보류. 두 번째 외부 의존이 추가되는 시점(예: 결제 + 푸시 알림 + 알림톡 발송)에 재검토. 그 시점에 W3C `traceparent` 헤더 파싱·전파 + `traceId`/`spanId` 분리가 필요해짐
- **분산 트레이싱 백엔드** — Tempo(Grafana 스택) vs Jaeger(독립) vs AWS X-Ray(AWS 종속). OTel 도입 시 함께 결정. 현재 결정 보류
- **민감 정보 마스킹 자동화 표준 (v2)** — `@Loggable` 커스텀 어노테이션 vs Jackson `@JsonIgnore`/`@JsonSerialize` 활용. PII 필드가 늘어나는 시점(이메일·전화번호 등 새 도메인 추가)에 재평가
- **로그 보존 정책 — 30일 / 90일 / 컴플라이언스 기준** — 현재는 stdout 캡쳐만 책임. 수집 백엔드 도입(Product 1) 시 보존 기간을 명시. 개인정보보호법 관련 보존 의무가 도메인에 발생하면 별도 ADR 필요
- **로그 수집 백엔드 선정** — CloudWatch Logs(AWS 동일 계정·IAM 단순) vs Loki+Grafana(자체 호스팅·비용 우위) vs ELK(검색 풍부·운영 비용 최대). Product 1 모니터링 stack 합의와 함께 결정
- **`AsyncAppender` 도입 가부** — 토이 트래픽에서 동기 출력 충분. Product 2 부하 테스트 결과 GC pause나 P99 지연이 stdout I/O와 상관관계를 보이면 도입 검토
- **`X-Request-Id` 정책 — 헤더 echo back을 신뢰할 것인가** — 현재는 클라이언트 입력을 그대로 사용(길이 제한만). 향후 악성 입력(스푸핑된 ID로 다른 사용자의 로그를 오염시키려는 시도) 위협 모델이 등장하면 서버 강제 UUID로 전환
- **비동기·배치 진입점의 MDC 전파 표준** — `@Async`/`@Scheduled` 진입 시 호출 컨텍스트의 MDC를 어떻게 이어갈지. v1은 컨벤션(명시 전달), v2는 `TaskDecorator`/`MDCAdapter` 자동화 검토
- **외부 라이브러리 로그 필드 호환** — Spring Security/Hibernate가 자체적으로 로깅하는 라인은 LogstashEncoder를 거치지만 MDC 외 필드(예: `mdc.thread`)가 들어올 수 있음. 화이트리스트 외 키를 발견하면 어떻게 차단할지 (현재는 `<includeMdcKeyName>`이 화이트리스트 역할)

---

**Epic 분해는 4개로 압축했습니다.** 기존 5개 Story 중 0-1을 Epic 1, 0-2/0-3을 Epic 2로 묶었습니다 — MDC 필드 추가는 동일 필터 체인 안에서 일어나는 일이라 Epic 단위로 묶이는 게 자연스럽습니다. Epic별 Story 분해가 필요하면 동일 양식으로 이어서 만들어드릴 수 있습니다.

## Epic 1. JSON 구조화 로그 포맷 — `logstash-logback-encoder` 도입 · profile별 분기 · 표준 스키마 확정

## Epic 목표

> ThirdTool의 prod·dev 환경 로그를 JSON 한 줄 포맷으로 출력하고, 모든 로그 라인이 동일한 표준 스키마(`timestamp · level · logger · message · traceId · requestId · userId · application · exception`)를 따르게 한다.
local 프로파일은 개발자 가독성을 위해 평문 콘솔 포맷을 유지한다.
>

## 배경

- 현재 Spring Boot 기본 평문 포맷 단일 설정. 환경별 분기가 없다
- 로그 수집기(Loki·CloudWatch Logs 등)는 JSON을 전제로 동작하므로, 평문 상태에서는 v2 수집 인프라 도입 시 전체 포맷 마이그레이션이 필요
- 표준 필드명을 OpenTelemetry Semantic Conventions(`traceId`)와 미리 호환시켜두면, 향후 분산 트레이싱 도입 비용을 낮출 수 있다
- 이 Epic이 완료되어야 후속 Epic 2/3에서 MDC 필드(requestId · userId)가 실제 JSON 라인에 실릴 수 있다 — 포맷터가 먼저 있어야 필드가 보임

## 핵심 설계 결정

> **profile별 Appender 분기를 한 `logback-spring.xml`에서 처리한다.**
별도 설정 파일 분리(`logback-spring-prod.xml` 등)는 환경 간 drift 위험이 커서 채택하지 않음. `<springProfile>` 태그로 한 파일 안에서 분기.
>
> - local: `CONSOLE_PLAIN` Appender — 기본 Spring Boot 평문, 컬러 출력
> - dev / prod: `CONSOLE_JSON` Appender — `LogstashEncoder` 사용
> - 표준 필드는 `<provider>` 또는 `customFields`로 명시적 선언, MDC는 `<includeMdcKeyName>`으로 화이트리스트 방식 — MDC 누수가 JSON에 그대로 노출되는 사고 방지

## 완료 기준 (Definition of Done)

- [ ]  `logstash-logback-encoder` 의존성이 `build.gradle`에 추가된다
- [ ]  `logback-spring.xml`이 profile별 Appender 분기로 작성된다 (local 평문 / dev·prod JSON)
- [ ]  prod 프로파일 로그 1라인이 표준 스키마를 모두 갖춘 JSON으로 출력된다
- [ ]  `application: thirdtool` 고정 필드가 모든 로그에 포함된다
- [ ]  연결된 Story가 모두 Done 상태다

## 산출물 (Deliverables)

- `build.gradle` (의존성 추가)
- `src/main/resources/logback-spring.xml`
- 로그 샘플 비교 스크린샷 (평문 vs JSON)
- `docs/logging.md` 내 "표준 필드 스키마" 섹션 (Epic 4에서 통합)

## 연결된 Story 목록

- [ ]  Story 1-1. `logstash-logback-encoder` 도입 및 profile별 JSON Appender 구성 (3 SP)

## 내부 메모 / 제약 사항

- 의존성 버전은 Spring Boot BOM과 충돌 없는 안정 버전 사용 (8.x)
- MDC 화이트리스트: `traceId`, `requestId`, `userId`, `method`, `path` — 이 외에 임의로 MDC에 들어간 키는 JSON에 노출되지 않음
- 로그 레벨 기본값: `org.springframework: INFO`, `com.thirdtool: DEBUG(dev)/INFO(prod)`, `org.hibernate.SQL: DEBUG(dev)/OFF(prod)`
- 연기 항목: 비동기 Appender(`AsyncAppender`) 도입 — 부하 테스트 후 GC pause 영향 확인 후 결정

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

> Product 레벨 "대안 검토"가 큰 갈림길이라면, 이 Epic 안에서 마주친 작은 갈림길의 결정·트레이드오프를 남긴다.

- **`logstash-logback-encoder` 버전 — 7.x vs 8.x** — 8.0+를 채택. Spring Boot 3.x / Logback 1.5 호환성 및 OTel 필드 지원 강화. 7.x는 Spring Boot 2.x 환경 잔재 케이스에만 의미. 비용: 8.0 미만에 의존하는 외부 도구 없음을 확인 필요
- **표준 필드명 — `@timestamp`(Logstash 관행) vs `timestamp`(단순)** — LogstashEncoder 기본값(`@timestamp`)을 그대로 수용. 수집기 호환을 최우선. 비용: jq 쿼리 작성 시 `.["@timestamp"]` 형태로 따옴표 필요
- **`customFields` 인코딩 — JSON 문자열 vs 별도 `<provider>` 등록** — JSON 문자열로 한 줄 압축 (`{"application":"thirdtool"}`). 향후 환경별 분기 필요 시 `<provider class="net.logstash.logback.composite.CustomFieldsJsonProvider">` 형태로 전환 가능
- **MDC 화이트리스트 vs 블랙리스트** — 화이트리스트(`<includeMdcKeyName>`). 블랙리스트는 누락 시 누출이지만 화이트리스트는 누락 시 단순 미노출 — 보안 기본값. 비용: 새 MDC 키 추가 시 logback-spring.xml도 함께 갱신해야 함 (Epic 3에서 `errorCode` 추가)
- **로그 레벨 — 패키지명 prefix** — 본 프로젝트는 `com.example.thirdtool` (CLAUDE.md 기준). Spec의 `com.thirdtool` 표기는 작성 편의상 약식. logback-spring.xml은 실제 패키지 기준으로 작성
- **Hibernate bind 파라미터 — `OFF` vs `WARN`** — prod에서 **OFF 강제**. bind 파라미터에는 PII(이메일·이름·검색어)가 그대로 들어가는 사례가 일반적. SQL 텍스트만 WARN으로 남겨 장애 추적 escape hatch 확보 (ADR008 §재검토 메모 참조)
- **JSON 라인 한 줄 vs prettyPrint** — 한 줄 강제. 수집기가 라인 단위 파싱하기 때문. prettyPrint는 stack trace 디버깅 시 가독성이 좋지만 수집기 호환 손상 — local 평문 appender로 대체

---

## Story 1-1. `logstash-logback-encoder` 도입 및 profile별 JSON Appender 구성

### User Story

> As a 백엔드 개발자,
I want 모든 prod 로그가 JSON 한 줄 포맷으로 출력되고 local 로그는 평문 가독성을 유지하길,
So that 향후 수집기 연동 시 포맷 마이그레이션 없이 그대로 흘려보낼 수 있고 개발 중에는 로그를 눈으로 빠르게 읽을 수 있다.
>

### 설계 노트

- 의존성: `net.logstash.logback:logstash-logback-encoder:8.0` (build.gradle의 `implementation`)
- `logback-spring.xml` 구조

    ```xml
    <configuration>
      <springProfile name="local">
        <appender name="CONSOLE_PLAIN" class="ch.qos.logback.core.ConsoleAppender">
          <encoder><pattern>%d{HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n</pattern></encoder>
        </appender>
        <root level="INFO"><appender-ref ref="CONSOLE_PLAIN"/></root>
      </springProfile>
    
      <springProfile name="dev,prod">
        <appender name="CONSOLE_JSON" class="ch.qos.logback.core.ConsoleAppender">
          <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"application":"thirdtool"}</customFields>
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>requestId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <includeMdcKeyName>method</includeMdcKeyName>
            <includeMdcKeyName>path</includeMdcKeyName>
          </encoder>
        </appender>
        <root level="INFO"><appender-ref ref="CONSOLE_JSON"/></root>
      </springProfile>
    </configuration>
    ```

- 표준 JSON 스키마 (예시)

    ```json
    {
      "@timestamp": "2026-05-14T03:21:08.421Z",
      "level": "INFO",
      "logger_name": "c.t.controller.CardController",
      "thread_name": "http-nio-8080-exec-3",
      "message": "Card list fetched",
      "application": "thirdtool",
      "requestId": "8f3c-...",
      "userId": "1042",
      "method": "GET",
      "path": "/api/v1/decks/{id}/cards"
    }
    ```

- logger별 레벨 정책을 `<logger>` 태그로 명시 — `com.thirdtool`, `org.hibernate.SQL`, `org.springframework.web`

### 완료 기준 (Acceptance Criteria)

- [ ]  `./gradlew bootRun --args='--spring.profiles.active=prod'` 실행 시 콘솔 로그가 JSON 한 줄로 출력된다
- [ ]  `./gradlew bootRun --args='--spring.profiles.active=local'` 실행 시 기존 평문 포맷이 유지된다
- [ ]  JSON 로그에 `timestamp · level · logger_name · thread_name · message · application` 필드가 모두 포함된다
- [ ]  예외 발생 시 `stack_trace` 필드에 풀 트레이스가 한 JSON 라인 안에 포함된다
- [ ]  MDC 화이트리스트 외의 임의 키(예: `MDC.put("foo", "bar")`)는 JSON에 노출되지 않는다

### 엣지 케이스

- 한글 메시지가 JSON 이스케이프되어 깨지지 않는다 (`message`: `"카드를 찾을 수 없습니다"`)
- logger 이름에 `$` 같은 특수문자가 포함돼도 JSON 파싱 오류가 발생하지 않는다
- 멀티라인 예외 메시지가 한 JSON 라인 안에 `\n` 이스케이프되어 들어간다 (로그 수집기가 라인 단위로 파싱할 때 안전)

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  prod·local 로그 샘플 비교 스크린샷 첨부
- [ ]  jq로 JSON 파싱 검증: `bootRun ... | jq .` 정상 출력 확인
- [ ]  스테이징 배포 확인

### 의존성

- 선행: 없음 (Product 0의 시작 Story)
- 후속: Story 2-1 (MDC 필드가 JSON에 실리려면 이 Story의 화이트리스트가 먼저 있어야 함)

### 스토리 포인트

- 추정: 3 SP

---

---

---

## Product 0 요약

| Epic | Story 수 | SP 합계 |
| --- | --- | --- |
| Epic 1. JSON 구조화 로그 포맷 | 1 | 3 |
| Epic 2. MDC 요청 컨텍스트 전파 | 2 | 5 |
| Epic 3. 에러 로깅 표준화 | 1 | 3 |
| Epic 4. 운영 가이드 문서화 | 1 | 2 |
| **합계** | **5** | **13 SP** |

**진행 순서 (필수):** Epic 1 → 2 → 3 → 4. Epic 1·2는 PR을 묶어도 무방하지만, Epic 3은 Epic 2의 MDC가 존재해야 의미가 살아납니다. Epic 4는 마지막에 일괄 작성하는 게 효율적입니다 (코드가 안정된 후 문서화).

## Epic 2. MDC 요청 컨텍스트 전파 — `MdcLoggingFilter` · requestId · userId · 동시성 안전

# Epic 2. MDC 요청 컨텍스트 전파 — `MdcLoggingFilter` · requestId · userId · 동시성 안전

## Epic 목표

> 모든 HTTP 요청에 고유 식별자(requestId)를 부여하고, 인증된 요청에는 userId를 함께 MDC에 실어 로그 JSON으로 자동 전파한다.
동시 요청이 섞이거나 스레드 풀이 재사용되어도 MDC가 교차 오염되지 않도록 격리 안전성을 보장한다.
클라이언트가 응답 헤더 `X-Request-Id`로 동일 식별자를 받아 운영 문의 시 traceId 기반 추적이 가능한 상태를 만든다.
>

## 배경

- Epic 1로 JSON 포맷의 MDC 화이트리스트가 깔렸지만, 실제로 키를 채워주는 주체가 없다 — 이 Epic이 MDC를 채운다
- 동시 요청 격리 실패는 운영 1년차에 흔한 사고 패턴: 스레드 풀 재사용 시 이전 요청의 MDC가 다음 요청에 그대로 남는 누수 → 잘못된 userId로 로그 출력 → 감사 추적 불가
- 인증 컨텍스트 주입은 JWTFilter 이후 체인에서 일어나야 함 — JWTFilter 자체의 인증 실패/성공 분기 후 userId 확보 시점이 정확히 잡혀야 한다

## 핵심 설계 결정

> **MdcLoggingFilter는 FilterChain 가장 앞단에 등록한다. JWTFilter보다 먼저.**
인증 실패 로그에도 requestId가 찍혀야 운영 문의 추적이 가능하기 때문.
>
> - 흐름: `MdcLoggingFilter(requestId 부여) → JWTFilter(인증) → UserIdMdcFilter(userId 주입) → Controller`
> - `MdcLoggingFilter.doFilter()`의 `finally` 블록에서 반드시 `MDC.clear()` 호출 — 스레드 풀 재사용 시 누수 방지
> - userId 주입은 별도 작은 필터(`UserIdMdcFilter`)로 분리하지 않고, `JWTFilter` 인증 성공 분기 안에서 `MDC.put("userId", ...)` 호출로 처리 — 필터 개수를 늘리지 않고 응집도 유지

> **requestId 헤더 정책은 "있으면 사용, 없으면 신규 생성"으로 한다.**
클라이언트가 X-Request-Id를 보내면 그대로 전파, 없으면 서버가 UUID 생성.
>
> - 이유: 향후 프론트엔드가 사용자 세션 단위로 X-Request-Id를 발급해 모바일 앱 → 서버 → 백오피스까지 동일 ID로 추적할 수 있는 확장 경로 확보
> - 빈 문자열·공백·128자 초과는 비정상으로 간주, 서버 신규 UUID로 대체 (Acceptance Criteria 엣지 케이스로 검증)

## 완료 기준 (Definition of Done)

- [ ]  모든 HTTP 요청 로그에 `requestId`가 포함된다
- [ ]  인증된 요청 로그에 `userId`가 포함된다, 비인증 요청에는 포함되지 않는다
- [ ]  응답 헤더 `X-Request-Id`가 모든 응답에 포함된다
- [ ]  동시 요청 격리 단위 테스트 통과 (스레드 풀 재사용 시 MDC 누수 없음)
- [ ]  연결된 Story가 모두 Done 상태다

## 산출물 (Deliverables)

- `MdcLoggingFilter` 클래스
- `JWTFilter` 내 userId MDC 주입 코드 (또는 별도 필터)
- `SecurityConfig` 필터 체인 등록 변경
- 동시 요청 격리 단위 테스트 (`MdcLoggingFilterTest`)

## 연결된 Story 목록

- [ ]  Story 2-1. `MdcLoggingFilter` 구현 — requestId 생성·전파·응답 헤더 echo back · MDC clear (3 SP)
- [ ]  Story 2-2. 인증 컨텍스트(userId) MDC 주입 (2 SP)

## 내부 메모 / 제약 사항

- requestId 길이 제약: 최대 64자 (UUID v4가 36자, 외부 시스템 ID 호환 위한 여유)
- userId는 Long 타입을 문자열로 변환해 MDC에 저장 (`String.valueOf(userId)`)
- 이메일·이름 등 PII는 절대 MDC에 넣지 않음 (Product Vision의 보안 원칙)
- 연기 항목: `traceId`(분산 트레이싱용) 필드는 v1에서는 `requestId`와 동일 값으로 채움 — OpenTelemetry 도입 시점에 분리

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

> Product 레벨 "대안 검토"가 큰 갈림길이라면, 이 Epic 안에서 마주친 작은 갈림길의 결정·트레이드오프를 남긴다.

- **`MdcLoggingFilter` 등록 위치 — `addFilterBefore(_, JWTFilter.class)` vs `@Order` 명시** — 명시적 `addFilterBefore` 채택. SecurityFilterChain 안에서 위치가 코드로 검증 가능. `@Order`는 Spring Boot Filter 자동 등록 경로에서만 동작 — SecurityFilterChain과 섞이면 순서 추론이 어려움
- **userId 주입 위치 — 별도 `UserIdMdcFilter` vs `JWTFilter` 인증 성공 분기 내부** — 후자 채택. 필터 개수 최소화 + 인증 컨텍스트 확보 시점과 MDC.put 호출 시점이 동일 코드 위치에 응집. 비용: JWTFilter가 MDC 의존을 추가로 가짐 (현재도 SLF4J Logger 사용 중이라 큰 부담 아님)
- **`MDC.clear()` vs `MDC.remove(key)` per key** — `clear()` 채택. 화이트리스트 외 키가 어디선가 put된 경우에도 누수 차단. 비용: 같은 스레드에서 MDC를 다른 용도로 쓰는 코드가 추후 추가되면 충돌 — 컨벤션으로 "MDC는 본 필터가 단독 관리" 명시
- **`traceId == requestId` v1 동일값 vs 별도 생성** — 동일값. v1은 분산 호출 없음. OTel 도입 시 `traceId`는 외부에서 받고 `spanId`/`requestId`는 자체 생성으로 분기 — 그 시점에 한 줄 변경
- **`X-Request-Id` 입력 신뢰 정책** — "있으면 사용, 빈/blank/64자 초과면 서버 UUID" 채택. 완전 거부(항상 서버 생성)는 멀티 hop 추적의 가치를 버리고, 완전 신뢰는 인젝션·스푸핑 위협. 길이 + blank 검증으로 1차 방어
- **`clientIp` 결정 — `X-Forwarded-For` 우선 vs `RemoteAddr` 우선** — XFF 우선 (ALB/CloudFront 환경 전제). XFF 부재 시 RemoteAddr fallback. XFF 위변조 가능성은 ALB 신뢰 경계로 수용
- **요청 시작/종료 로그 INFO vs DEBUG** — INFO 채택. 운영 환경에서 "요청이 들어왔다/끝났다"는 가장 빈도 높은 추적 단위. prod 볼륨 부담은 v2 부하 테스트 결과 보고 재평가 (필요 시 sampling)

---

## Story 2-1. `MdcLoggingFilter` 구현 — requestId 생성 및 요청 컨텍스트 전파

### User Story

> As a 백엔드 개발자,
I want 모든 요청에 고유한 requestId가 부여되고 로그에 자동 포함되며 응답 헤더로도 echo back되길,
So that 동시 요청이 섞여도 특정 요청의 로그 흐름을 ID 하나로 추적할 수 있고, 클라이언트가 받은 ID로 운영 문의를 정확히 매칭할 수 있다.
>

### 설계 노트

- `MdcLoggingFilter extends OncePerRequestFilter`
- `SecurityConfig`에서 `addFilterBefore(mdcLoggingFilter, JWTFilter.class)`로 등록
- 핵심 로직 골격

    ```java
    public class MdcLoggingFilter extends OncePerRequestFilter {
        private static final String HEADER = "X-Request-Id";
        private static final int MAX_LEN = 64;
    
        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            String requestId = resolveRequestId(req.getHeader(HEADER));
            long startedAt = System.currentTimeMillis();
            try {
                MDC.put("requestId", requestId);
                MDC.put("traceId", requestId); // v1은 동일 값
                MDC.put("method", req.getMethod());
                MDC.put("path", req.getRequestURI());
                res.setHeader(HEADER, requestId);
    
                log.info("request.start clientIp={}", clientIp(req));
                chain.doFilter(req, res);
            } finally {
                long durationMs = System.currentTimeMillis() - startedAt;
                log.info("request.end status={} durationMs={}", res.getStatus(), durationMs);
                MDC.clear(); // 누수 방지 필수
            }
        }
    
        private String resolveRequestId(String incoming) {
            if (incoming == null || incoming.isBlank() || incoming.length() > MAX_LEN) {
                return UUID.randomUUID().toString();
            }
            return incoming;
        }
    }
    ```

- 요청 시작/종료 로그는 INFO 레벨, `clientIp`는 `X-Forwarded-For` 우선 → 없으면 `req.getRemoteAddr()`

### 완료 기준 (Acceptance Criteria)

- [ ]  모든 요청에 requestId가 부여되고 로그 JSON에 포함된다
- [ ]  응답 헤더 `X-Request-Id`가 echo back된다
- [ ]  클라이언트가 `X-Request-Id` 헤더로 임의 값을 보내면 그 값이 그대로 사용된다
- [ ]  요청 시작 / 종료 로그가 INFO 레벨로 출력되며, 종료 로그에 `status`와 `durationMs`가 포함된다
- [ ]  예외 발생 경로(throw 후 `@RestControllerAdvice` 처리)에서도 MDC가 정상 clear된다

### 엣지 케이스

- 클라이언트가 `X-Request-Id`를 빈 문자열로 전송 → 서버에서 UUID 신규 생성
- 클라이언트가 128자짜리 비정상 값 전송 → 64자 제약 초과로 서버 UUID로 대체
- 동시 요청 2개를 5ms 간격으로 발생 → 두 요청의 requestId가 절대 동일하지 않다 (단위 테스트로 보장)
- 스레드 풀 재사용 시 이전 요청의 MDC가 다음 요청에 남지 않는다 (스레드를 강제 재사용하는 통합 테스트로 검증)

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  동시 요청 격리 단위 테스트 통과 (`MdcLoggingFilterTest`)
- [ ]  요청 시작/종료 로그 샘플 스크린샷 첨부
- [ ]  응답 헤더 X-Request-Id 포함 확인 (curl `i` 출력 첨부)
- [ ]  스테이징 배포 확인

### 의존성

- 선행: Story 1-1 (JSON 포맷에 MDC 화이트리스트가 등록되어 있어야 함)
- 후속: Story 2-2

### 스토리 포인트

- 추정: 3 SP

---

## Story 2-2. 인증 컨텍스트(userId) MDC 주입

### User Story

> As a 백엔드 개발자,
I want 인증된 요청에 한해 userId가 MDC에 자동 포함되길,
So that 특정 유저의 행동 흐름을 로그에서 단일 필드로 필터링할 수 있고, 에러 발생 시 영향 범위(어느 유저인지)를 즉시 파악할 수 있다.
>

### 설계 노트

- `JWTFilter`의 인증 성공 분기 안에 `MDC.put("userId", String.valueOf(userId))` 추가
- 비인증 경로(`/login`, `/signup`, `/actuator/**`)는 JWTFilter를 통과하지 않거나 통과해도 userId 미주입
- userId는 Long → String 변환 (`String.valueOf`)
- 이메일·이름은 MDC에 넣지 않음 — PII 노출 방지
- `MdcLoggingFilter.finally`의 `MDC.clear()`가 userId까지 함께 정리하는지 검증

### 완료 기준 (Acceptance Criteria)

- [ ]  인증된 요청의 모든 로그 라인에 `userId` 필드가 포함된다
- [ ]  비인증 요청(`/login`, `/signup`)의 로그에는 `userId` 필드가 없거나 null이다
- [ ]  동일 유저의 동시 요청 2개에서 requestId는 다르지만 같은 userId가 일관되게 기록된다

### 엣지 케이스

- 만료된 토큰 요청 → 인증 실패 처리되고 userId는 MDC에 주입되지 않는다 (이 시점 로그는 WARN, requestId는 있지만 userId 없음)
- 토큰은 유효하지만 DB에서 유저가 삭제된 케이스 → 인증 실패로 분기, userId 미주입
- userId가 `null`인 비정상 상태로 JWTFilter를 통과 → `MDC.put("userId", "null")` 문자열로 들어가지 않도록 가드

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  인증 / 비인증 요청 로그 비교 샘플 첨부 (`userId` 필드 유무)
- [ ]  만료 토큰 케이스 로그 샘플 첨부
- [ ]  스테이징 배포 확인

### 의존성

- 선행: Story 2-1 (MDC 인프라가 깔려 있어야 userId만 추가 주입 가능)
- 후속: Epic 3 (GlobalExceptionHandler가 userId를 로그에 자동 활용)

### 스토리 포인트

- 추정: 2 SP

## Epic 3. 에러 로깅 표준화 — 예외별 로그 레벨 자동 분리 · 민감정보 차단

# Epic 3. 에러 로깅 표준화 — 예외별 로그 레벨 자동 분리 · 민감정보 차단

## Epic 목표

> `GlobalExceptionHandler`를 정비해 예외 유형별로 ERROR / WARN / INFO 로그 레벨이 자동 부여되는 상태를 만든다.
ERROR 로그는 즉시 대응이 필요한 5xx 장애 신호로만 예약되고, BusinessException(예상 가능한 비정상)은 WARN, 4xx 검증 실패는 INFO로 분리한다.
비밀번호·accessToken·refreshToken이 어떠한 경로로도 로그에 출력되지 않도록 DTO `toString()`을 정비한다.
>

## 배경

- Product Vision의 KPI 중 "ERROR 로그 중 실제 5xx 비율 95% 이상" 달성의 직접 수단이 이 Epic
- 현재 GlobalExceptionHandler는 모든 예외를 동일하게 `log.error()` 처리 → Product 1의 알림(v2) 연결 시 노이즈로 신호 무력화
- 민감정보 차단은 컨벤션이 아닌 코드로 강제: `User.password` 필드의 `@ToString.Exclude`, `LoginRequest`의 `toString` 오버라이드, JWT 토큰의 prefix-only 로깅
- Epic 2의 MDC(userId · requestId · path)가 깔린 상태에서 예외 로그가 자동으로 풍부한 컨텍스트를 갖게 됨 — 별도 필드 추가 코드 불필요

## 핵심 설계 결정

> **`@ExceptionHandler` 메서드별로 로그 레벨을 명시적으로 분리한다.**
단일 `@ExceptionHandler(Exception.class)`로 모든 예외를 처리하지 않는다.
>
> - `BusinessException` (커스텀 부모) → `log.warn(...)`, stack trace 미포함, 메시지만
> - `MethodArgumentNotValidException` · `ConstraintViolationException` → `log.info(...)`, 메시지만
> - `AccessDeniedException` · `AuthenticationException` → `log.info(...)`
> - 그 외 `Exception` (fallback) → `log.error(...)`, stack trace 포함
> - DEBUG 레벨은 외부 API 호출 요청/응답 본문 로깅 전용 (prod에서 비활성)

> **`errorCode`를 로그 메시지가 아닌 별도 MDC 필드로 추가한다.**
향후 로그 수집기에서 `errorCode`별 집계를 위해 필드 분리.
>
> - `MDC.put("errorCode", ex.getCode())`를 ExceptionHandler 진입 직후 호출
> - `MdcLoggingFilter.finally`가 어차피 MDC.clear()를 수행하므로 누수 위험 없음
> - logback-spring.xml의 MDC 화이트리스트에 `errorCode` 추가 필요 — Epic 1 산출물에 후속 패치

> **민감정보 차단은 `@ToString.Exclude` + 단위 테스트로 강제한다.**
자동 마스킹 어노테이션(v2) 도입 전, 가장 가벼운 방법.
>
> - `User.password`, `RefreshToken.token` → `@ToString.Exclude`
> - `LoginRequest.password` → `toString()` 오버라이드로 `**` 출력
> - JWT 발급/검증 경로에서 토큰은 prefix 8자 + `**`로만 로깅
> - 단위 테스트: `LoginRequest("user@example.com", "mySecret123").toString()` 결과에 `mySecret123`이 포함되지 않는다

## 완료 기준 (Definition of Done)

- [ ]  5xx 발생 시 ERROR 레벨 + stack_trace 필드를 포함한 JSON 로그가 출력된다
- [ ]  BusinessException은 WARN 레벨로 출력되고 stack_trace는 포함되지 않는다
- [ ]  4xx 검증 실패는 INFO 레벨로 출력된다
- [ ]  비밀번호·토큰이 어떠한 예외 경로에서도 로그에 노출되지 않는다 (단위 테스트로 보장)
- [ ]  연결된 Story가 모두 Done 상태다

## 산출물 (Deliverables)

- `GlobalExceptionHandler` 리팩토링
- 민감정보 마스킹 단위 테스트 (`SensitiveDataLoggingTest`)
- `User`, `LoginRequest`, `RefreshToken` 등의 `toString()` 정비
- logback-spring.xml MDC 화이트리스트에 `errorCode` 추가

## 연결된 Story 목록

- [ ]  Story 3-1. `GlobalExceptionHandler` 예외 유형별 로그 레벨 분리 + 민감정보 차단 (3 SP)

## 내부 메모 / 제약 사항

- 예외 클래스 추가 시 ExceptionHandler에 명시적 메서드 추가 강제 — fallback `Exception.class` 매핑에 의존하지 않음
- DEBUG 레벨은 외부 API 호출 본문에 한해 사용. prod 비활성 (`com.thirdtool.external: INFO`)
- 연기 항목: `@Loggable` 커스텀 어노테이션·리플렉션 기반 자동 마스킹 (v2)

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

> Product 레벨 "대안 검토"가 큰 갈림길이라면, 이 Epic 안에서 마주친 작은 갈림길의 결정·트레이드오프를 남긴다.

- **단일 `@ExceptionHandler(Exception.class)` 분기 vs 예외별 메서드** — 예외별 메서드 채택. 단일 분기 + 내부 `instanceof` 체인은 신규 예외 추가 시 분기 누락이 조용히 발생 (fallback에 흡수). 메서드 분리 시 컴파일러·테스트가 누락을 잡아냄
- **`AccessDeniedException` 로그 레벨 — INFO vs WARN** — **WARN** 채택. 현 구현 (GlobalExceptionHandler.handleAccessDenied)이 이미 `log.warn`을 쓰고 있음 (Story 5-3 직전 커밋). spec의 "INFO" 표기는 일반론. 권한 거부는 4xx지만 보안 관점에서 추적 가치가 더 높아 WARN으로 격상이 합리적 — docs/logging.md에 명시
- **MDC `errorCode` vs 로그 메시지 인라인** — MDC 필드 분리 채택. 수집기에서 `errorCode` 기준 집계 가능. 비용: logback-spring.xml `<includeMdcKeyName>errorCode</includeMdcKeyName>` 추가 필요 (Epic 1 산출물 후속 패치)
- **stack trace 포함 정책 — fallback Exception만 vs 5xx 전부** — fallback Exception(`Exception.class`)만. 명시 분기된 4xx/409는 stack 미포함. Spring Security `AuthenticationException`도 stack 미포함 — 인증 실패는 빈번한 정상 분기
- **비밀번호 마스킹 위치 — DTO `toString()` vs Logger Encoder converter** — DTO `toString()` 채택. converter 방식은 코드 한 곳이지만 SLF4J `log.warn("user={}", user)` 호출 시 toString이 먼저 평가됨 → encoder가 받기 전에 이미 String화 → 마스킹 불가. 비용: 새 DTO 추가 시 컨벤션 적용 필요 (PR 체크리스트로 강제)
- **JWT 토큰 마스킹 — prefix 8자 vs 전체 hash** — prefix 8자. 디버깅 시 "어떤 토큰이었는지" 식별은 가능하면서 원문은 노출되지 않음. hash는 디버깅 비용이 너무 큼 (DB와 비교 불가)
- **`@RestControllerAdvice` 단일 vs BC별 분리** — 단일 유지. BC별 분리는 같은 ErrorCode 형식·응답 스키마를 중복 작성하는 비용. 본 Epic의 정비는 단일 진입점이라 가치가 더 큼
- **현재 `handleAccessDenied`와의 충돌** — 본 Epic 작업 시 기존 Story 5-3에서 추가된 `handleAccessDenied`의 로그 표현(`"[AccessDenied] {} {} - {}"`)을 새 표준(`"access.denied code={}"`)으로 통일할지 결정. 응답 동작은 불변, 로그 포맷만 정렬. 트레이드오프: 기존 로그 grep 패턴이 깨짐 (prod 트래픽 0 → 비용 무시)

---

## Story 3-1. `GlobalExceptionHandler` 예외 유형별 로그 레벨 분리 + 민감정보 차단

### User Story

> As a 백엔드 개발자,
I want 예외 유형에 따라 적절한 로그 레벨이 자동 부여되고, 비밀번호·토큰이 어떤 경로에서도 로그에 노출되지 않길,
So that 후속 Product의 알림이 진짜 신호만 다루도록 노이즈가 차단되고, 민감정보 유출 사고를 사전에 막을 수 있다.
>

### 설계 노트

- 예외 → 로그 레벨 매핑 표


    | 예외 유형 | 로그 레벨 | stack trace | HTTP status |
    | --- | --- | --- | --- |
    | `Exception` (fallback) | ERROR | 포함 | 500 |
    | `BusinessException` | WARN | 미포함 | 400~409 (예외 정의값) |
    | `MethodArgumentNotValidException` | INFO | 미포함 | 400 |
    | `ConstraintViolationException` | INFO | 미포함 | 400 |
    | `AccessDeniedException` | INFO | 미포함 | 403 |
    | `AuthenticationException` | INFO | 미포함 | 401 |
- 핵심 코드 골격

    ```java
    @RestControllerAdvice
    @Slf4j
    public class GlobalExceptionHandler {
        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
            MDC.put("errorCode", ex.getCode());
            log.warn("business.exception code={} message={}", ex.getCode(), ex.getMessage());
            return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.from(ex));
        }
    
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
            MDC.put("errorCode", "VALIDATION_FAILED");
            log.info("validation.failed errors={}", ex.getBindingResult().getAllErrors().size());
            return ResponseEntity.badRequest().body(ErrorResponse.validation(ex));
        }
    
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleUnknown(Exception ex) {
            MDC.put("errorCode", "INTERNAL_ERROR");
            log.error("unexpected.exception", ex); // stack trace 포함
            return ResponseEntity.internalServerError().body(ErrorResponse.internal());
        }
    }
    ```

- 민감정보 차단 코드

    ```java
    @Entity
    public class User {
        private String email;
        @ToString.Exclude
        private String password;
    }
    
    public record LoginRequest(String email, String password) {
        @Override
        public String toString() {
            return "LoginRequest(email=" + email + ", password=***)";
        }
    }
    ```

- JWT 토큰 로깅 시 `token.substring(0, 8) + "***"` 형태로만 출력

### 완료 기준 (Acceptance Criteria)

- [ ]  5xx 발생 시 ERROR 레벨 JSON 로그에 `stack_trace` 필드가 포함된다
- [ ]  BusinessException은 WARN 레벨로 출력되고 `stack_trace`가 포함되지 않는다
- [ ]  4xx 검증 실패는 INFO 레벨로 출력된다
- [ ]  모든 예외 로그에 `errorCode` 필드가 MDC를 통해 포함된다
- [ ]  로그 라인이 Epic 2의 MDC 필드(requestId · userId · path)를 그대로 갖는다

### 엣지 케이스

- 동일 BusinessException이 5초 안에 100회 반복 → stack trace 누적 출력되지 않는다 (WARN 레벨이라 stack trace 자체가 없음으로 자연 보장)
- 비밀번호 필드를 포함한 `LoginRequest`가 `MethodArgumentNotValidException`을 일으킬 때 → 검증 실패 로그에 password 값이 노출되지 않는다 (단위 테스트로 검증)
- 외부 API 호출 실패가 `RestClientException`으로 올라올 때 → fallback ERROR 처리되며 응답 본문에 토큰이 포함돼 있어도 로그에는 prefix 8자만 노출

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  예외 유형별 로그 샘플(ERROR / WARN / INFO) 스크린샷 첨부
- [ ]  민감정보 미노출 단위 테스트 통과 (`SensitiveDataLoggingTest`)
- [ ]  스테이징 배포 확인

### 의존성

- 선행: Story 2-2 (userId가 MDC에 실려 있어야 예외 로그가 풍부한 컨텍스트를 가짐)
- 후속: Epic 4 (가이드 문서가 이 매핑 표를 인용)

### 스토리 포인트

- 추정: 3 SP

---

## Epic 4. 운영 가이드 문서화 — `docs/logging.md` · README 링크 · PR 체크리스트

# Epic 4. 운영 가이드 문서화 — `docs/logging.md` · README 링크 · PR 체크리스트

## Epic 목표

> Epic 1~3의 표준(JSON 스키마 · MDC 필드 · 예외 레벨 매핑 · 민감정보 금지 항목)을 코드베이스 내 단일 문서로 정리해, 신규 기능 추가 시 가이드만 보고 로그 레벨·필드를 결정할 수 있는 상태를 만든다.
>

## 배경

- 표준이 코드에만 박혀 있으면 1개월 뒤 본인도 잊는다 — 문서가 "미래의 나"를 위한 보험
- 채용 포트폴리오에서 `docs/logging.md`의 존재는 "운영 관점 있는 개발자"라는 강한 신호
- v2에서 자동 마스킹·로그 수집기 도입 시 이 문서가 마이그레이션의 출발점이 됨

## 완료 기준 (Definition of Done)

- [ ]  `docs/logging.md`가 작성되어 코드베이스에 커밋된다
- [ ]  레벨별 Good / Bad 코드 예시가 최소 2개씩 포함된다
- [ ]  MDC 필드 표 · 표준 JSON 스키마 · 민감정보 금지 항목이 명시된다
- [ ]  README.md에서 `docs/logging.md`로의 링크가 추가된다
- [ ]  PR 템플릿에 "로그 가이드 준수 여부" 체크박스가 추가된다
- [ ]  연결된 Story가 모두 Done 상태다

## 산출물 (Deliverables)

- `docs/logging.md`
- `README.md` 업데이트
- `.github/pull_request_template.md` 업데이트

## 연결된 Story 목록

- [ ]  Story 4-1. `docs/logging.md` 작성 + README 링크 + PR 템플릿 체크리스트 (2 SP)

## 내부 메모 / 제약 사항

- 문서 분량은 A4 2~3페이지 이내 권장 — 너무 길면 안 읽힘
- 표준 JSON 스키마는 실제 로그 샘플을 그대로 인용

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

> Product 레벨 "대안 검토"가 큰 갈림길이라면, 이 Epic 안에서 마주친 작은 갈림길의 결정·트레이드오프를 남긴다.

- **문서 위치 — `docs/logging.md` vs `docs/conventions/logging.md` vs README 임베드** — `docs/logging.md` 채택. `docs/`가 단일 진입점(ADR006). README는 링크만. conventions/ 서브 디렉토리는 폐기된 패턴(`.claude/rules` 통합 이력)이라 미사용
- **가이드 분량 — 2~3페이지 vs 풀 레퍼런스** — 짧은 가이드 채택. 풀 레퍼런스(예: Logback 공식 문서 복붙)는 안 읽힘. 본 가이드는 "이 프로젝트에 한정된 의사결정 + Good/Bad 예시"만 수록
- **PR 템플릿 체크리스트 — 자동화(GitHub Action) vs 수동 체크** — 수동 체크. CI에서 "log.error 사용 시 fallback Exception이 맞는지"를 정적 분석으로 잡으려면 별도 lint 룰 필요 — 가치 대비 비용 과다. 신뢰는 리뷰어 + 테스트로
- **신규 예외 추가 5단계 체크리스트 — 가이드 본문 vs 별도 템플릿** — 본문 포함. 별도 템플릿(`.github/issue_template`)은 신규 예외 발생 시점에 issue가 먼저 만들어지는 워크플로가 전제. 본 프로젝트는 코드 우선 → 가이드 본문에 둠
- **README 통합 vs 분리** — 분리. README는 첫 진입 페이지로 핵심 명령·구조만. 운영 가이드는 `## 운영 가이드` 섹션에서 하위 링크. 가이드 추가 시(부하 테스트·모니터링) 동일 패턴 재사용
- **ADR 채택 시점 — 본 Product 종료 시점 일괄 vs Epic별 점진** — Epic별 점진. ADR008은 Epic 1 산출물로 이미 채택됨. `ADR-LOG-002 Tracing Scope` / `ADR-LOG-003 Exception-to-LogLevel Mapping`은 각 Epic 종료 시점에 등록 (Epic 2 / Epic 3 PR과 함께)

---

## Story 4-1. `docs/logging.md` 작성 + README 링크 + PR 템플릿 체크리스트

### User Story

> As a 백엔드 개발자,
I want 로그 표준(JSON 스키마 · 레벨 정책 · MDC 필드 · 민감정보 금지)이 단일 문서에 정리되어 있길,
So that 신규 기능 추가 시 가이드만 보고 일관성 있게 로그를 작성할 수 있고, 신규 합류자(혹은 미래의 나)가 빠르게 컨텍스트를 회복할 수 있다.
>

### 설계 노트

- `docs/logging.md` 목차
    1. 로그 스택 (Logback + logstash-logback-encoder)
    2. profile별 포맷 (local 평문 / dev·prod JSON)
    3. 표준 JSON 스키마 (필드 표 + 샘플)
    4. MDC 필드 목록 (`traceId · requestId · userId · method · path · errorCode`)
    5. 로그 레벨 사용 정책 (ERROR / WARN / INFO / DEBUG / TRACE) — Good vs Bad 예시
    6. 예외 → 로그 레벨 매핑 표 (Epic 3 산출물 인용)
    7. 민감정보 금지 항목 (비밀번호 · accessToken · refreshToken · 이메일 본문 마스킹)
    8. 신규 예외 추가 가이드 (5단계 체크리스트)
- Good / Bad 코드 예시 (각 레벨당 최소 2개)
    - ERROR Good: `log.error("payment.callback.failed orderId={}", orderId, ex)` (5xx 외부 API 타임아웃)
    - ERROR Bad: `log.error("user not found")` (이건 INFO 또는 WARN)
- README.md 추가 섹션 (`## 운영 가이드` 아래에 한 줄 링크)

    ```markdown
    ## 운영 가이드
    - 로깅 가이드
    ```

- PR 템플릿 체크박스

    ```markdown
    ## 체크리스트
    - [ ] 신규 로그가 `docs/logging.md` 레벨 정책을 따른다
    - [ ] 비밀번호·토큰을 로깅하지 않는다
    - [ ] 신규 예외는 GlobalExceptionHandler에 매핑되어 있다
    ```


### 완료 기준 (Acceptance Criteria)

- [ ]  `docs/logging.md`가 코드베이스에 커밋된다
- [ ]  레벨별 Good / Bad 예시가 최소 2개씩 포함된다
- [ ]  표준 JSON 스키마 샘플이 포함된다
- [ ]  민감정보 금지 항목이 명시된다
- [ ]  README.md에서 링크가 동작한다

### 엣지 케이스

- 신규 도메인 예외 추가 시 → 가이드의 "신규 예외 추가 5단계 체크리스트"만 보고 레벨·HTTP status·errorCode 결정 가능 (검증 방법: 더미 예외 1개 추가 PR을 가이드만 보고 작성)

### Definition of Done

- [ ]  `docs/logging.md` 커밋 확인
- [ ]  README.md 링크 동작 확인
- [ ]  PR 템플릿 업데이트 확인
- [ ]  PO(또는 본인) 셀프 검수 완료

### 의존성

- 선행: Epic 1~3 (문서가 인용할 표준이 먼저 코드로 존재해야 함)
- 후속: 없음 (Product 0 마지막 Story)

### 스토리 포인트

- 추정: 2 SP