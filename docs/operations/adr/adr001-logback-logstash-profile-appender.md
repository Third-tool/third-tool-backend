# adr001: 로깅 인프라 — profile별 Appender 분기와 LogstashEncoder를 채택한다

**영역**: operations | **상태**: Accepted | **날짜**: 2026-05-17

> **면접 포인트**
> "프로덕션 로그를 어떻게 구성했나요? MDC와 PII 보호는 어떻게 처리했나요?"
> → 프로파일별 Appender 분기로 local은 컬러 평문, dev/prod는 LogstashEncoder JSON으로 분리했다. MDC 화이트리스트 5개 키만 노출을 허용해 코드가 임의로 MDC.put한 PII가 로그에 찍히는 것을 원천 차단했다.

---

## 왜 이 결정이 필요했나 (Context)

기존 상태: Spring Boot 기본 로깅 설정만 사용. 환경별 분기 없음, `application.yml`에 공통 `logging.level` 블록 하나만 존재.

세 가지 제약이 동시에 걸렸다:

1. **수집기 호환성**: 향후 Loki/CloudWatch Logs/OpenSearch 등 로그 수집기를 붙일 때 평문에서는 전체 포맷 마이그레이션이 필요하다. JSON으로 미리 표준화해두면 수집기 도입 시 필드 귀속 점검만 하면 된다.
2. **개발자 가독성 유지**: 로컬 개발 중에는 사람이 빠르게 읽어야 하므로 컬러 평문이 효율적이다. JSON은 콘솔에서 직접 읽기 어렵다.
3. **MDC 필드 누수 방지**: 향후 `requestId`·`userId` 같은 컨텍스트 필드를 MDC로 전파할 예정이지만, 화이트리스트 없이 LogstashEncoder를 쓰면 임의 키(`MDC.put("email", userEmail)`)가 모두 JSON에 노출되어 **PII 유출 사고 위험**이 있다.

또한 `application.yml`에 `org.hibernate.orm.jdbc.bind: TRACE`가 공통 설정으로 들어가 있어, prod에서 쿼리 파라미터(개인정보 포함)가 그대로 로그에 노출될 잠재 위험이 있었다.

---

## 무엇을 결정했나 (Decision)

**3축을 하나의 ADR에 결합** — 셋이 동시에 만족되어야 위 세 제약이 모두 풀린다.

### 1. profile별 Appender 분기 — 단일 파일 `logback-spring.xml`

환경별 별도 파일 분리는 drift 위험으로 채택하지 않음. 한 파일의 `<springProfile>` 태그로 분기:

- `local` → `CONSOLE_PLAIN` Appender (컬러 평문)
- `dev,prod` → `CONSOLE_JSON` Appender (LogstashEncoder)
- `application.yml`의 공통 `logging.level.*` 블록은 제거 → `logback-spring.xml`이 단일 진실 소스

### 2. JSON 인코더: `logstash-logback-encoder:8.0`

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <customFields>{"application":"thirdtool"}</customFields>
    <includeMdcKeyName>traceId</includeMdcKeyName>
    <includeMdcKeyName>requestId</includeMdcKeyName>
    <includeMdcKeyName>userId</includeMdcKeyName>
    <includeMdcKeyName>method</includeMdcKeyName>
    <includeMdcKeyName>path</includeMdcKeyName>
    <includeContext>false</includeContext>
</encoder>
```

표준 JSON 필드: `@timestamp · level · logger_name · thread_name · message · application · stack_trace`. 멀티라인 예외는 `\n` 이스케이프로 한 라인에 직렬화.

### 3. MDC 화이트리스트 5개 키로 노출 제한

| 키 | 의미 |
|----|------|
| `traceId` | OpenTelemetry Semantic Conventions 호환 (분산 트레이스 도입 대비) |
| `requestId` | HTTP 요청 단위 식별자 |
| `userId` | 인증된 사용자 식별자 |
| `method` | HTTP 메서드 |
| `path` | 요청 경로 |

이 외에 코드가 임의로 `MDC.put`한 키는 JSON에 노출되지 않는다. 회귀 방지: `LogbackJsonFormatTest`에서 검증.

### prod 부속 결정 — PII 차단 + 장애 escape hatch

- `org.hibernate.SQL=WARN` — 쿼리 텍스트는 보존 (장애 대응 시 SQL 추적 escape hatch)
- `org.hibernate.orm.jdbc.bind=OFF` — 바인딩 파라미터는 차단 (PII 유출 차단)

"쿼리 텍스트는 보고, 파라미터는 가린다"는 의도를 명시적으로 분리.

---

## 대안과 거부 이유 (Alternatives)

| 대안 | 장점 | 거부 이유 |
|------|------|-----------|
| 별도 설정 파일 분리 (`logback-prod.xml`) | 환경별 의도 명확 | 파일 간 drift 위험. 공통 정책이 한 곳에 모이지 않음 |
| 수집기 측에서 평문 → JSON 변환 (Filebeat parser) | 애플리케이션은 평문 유지 | 수집기 변경 시 파서 재작성. 멀티라인 예외 파싱 불안정 |
| Custom JsonLayout 직접 구현 | 필드 완전 제어 | 직접 유지보수 부담. LogstashEncoder가 이미 표준 필드 + MDC 지원 |
| MDC 화이트리스트 없는 LogstashEncoder | 단순 | 임의 MDC 키 노출. `application` 식별자 없어 멀티 서비스 환경에서 구분 불가 |

---

## 결과와 트레이드오프 (Consequences)

**긍정적**
- 수집기 도입 시 포맷 마이그레이션 불필요
- MDC 화이트리스트로 의도치 않은 PII 필드 노출 차단
- `hibernate.orm.jdbc.bind` PII 노출 위험 prod에서 코드 레벨로 차단
- 개발자가 `--spring.profiles.active=...`만으로 로그 포맷 전환 가능

**트레이드오프**
- 평문 대비 JSON 로그 라인 길이 약 1.5~2배 증가
- 새 MDC 키 추가는 ADR 수정 + `logback-spring.xml` 갱신 필요
- `LogbackJsonFormatTest`가 Spring Boot 내부 `LogbackLoggingSystem`에 의존 → 메이저 업그레이드 시 API 변경 위험
- CI가 `-x test` 옵션이라 회귀 방지 테스트가 CI에서 실행되지 않음 (로컬·PR 단계에서만 검증)

---

## 재검토 시점

- 외부 수집기(CloudWatch Logs/Loki/OpenSearch) 도입 시 → 필드명 귀속 정합성 점검
- MDC 화이트리스트가 7개 이상으로 확장되는 시점 → PII·카디널리티 정책 별도 ADR
- Spring Boot 메이저 업그레이드 시 → `LogbackLoggingSystem` API 호환성 회귀 점검
