# Pinned Topology — `observability` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님 (Runbook 원칙은 별도 · 본 파일은 로깅·메트릭·알람 topology)
> - vocabulary 아님 (실제 메트릭명·로그 필드명은 여기 없다)

**목적**: 로깅·메트릭·알람의 반복 원칙을 pin. 프로덕션 진입 (M7) 이전에도 원칙은 유효 · 로컬 로그 축적이 프로덕션 관측의 baseline.

---

## 0. 유효 구간 (Validity)

| 항목 | 값 |
| --- | --- |
| Pinned at | 2026-07-21 |
| Valid for | 본 워크플로우 v1 구간 — re-pin trigger 발생 전까지 |
| Owner | 메인 Claude + 사용자 합의 |
| Re-pin trigger | 아래 §4 |

---

## 1. How Claude Code must use this file — 행동 계약

1. **로깅·메트릭·알람 추가·수정 시 이 파일을 먼저 읽는다.** §2의 노드·경계는 본 구간 **고정 제약**.
2. **로그 형식·MDC 필드 어휘는 자유롭게 채우되, 로그 진입점·level mapping·MDC propagation 원칙은 건드리지 않는다.**
3. **Controller에서 `try-catch` 후 자체 로깅하려는 정황이 보이면 STOP하고 보고한다.** GlobalExceptionHandler 단일 진입 위반.
4. **Prometheus 메트릭을 등록 절차 없이 임의 추가하려는 정황이 보이면 보고**한다.
5. **개인 정보 (PII: 이메일·비밀번호·JWT) 를 로그에 그대로 출력하는 정황이 보이면 보고**한다.
6. **이 파일에 어휘를 추가하지 않는다.** 실제 메트릭·로그 필드는 §3 포인터로.

---

## 2. The pinned topology

### Nodes
- `구조화 로그` — JSON logstash-encoder 형식 (프로덕션) · 로컬은 stdout
- `MDC 컨텍스트` — 요청 스코프 필드 (`requestId`·`userId` 등) · thread-safe propagation
- `로그 레벨 매핑` — HTTP status → log level (5xx=ERROR·4xx=WARN·2xx=INFO)
- `GlobalExceptionHandler` — 사용자 노출 오류의 단일 로깅·응답 변환 진입점 (common-core topology 계승)
- `Prometheus 메트릭` — Micrometer 기반 (`/actuator/prometheus` 노출)
- `메트릭 등록 절차` — 명명 규칙 (`{project}.{area}.*`) · label 신중 · 카디널리티 관리
- `CloudWatch Alarm` — 프로덕션 임계값 초과 감지 (SNS → Slack)
- `Slack 배포 이벤트` — GHA post-deploy 채널 알림 (`#thirdtool-deploy`)
- `Slack Alarm 채널` — 장애 알림 (`#thirdtool-alerts`)
- `Async 로그 propagation` — 비동기 컨텍스트에서 MDC 유실 방지
- `PII 마스킹` — 이메일·비밀번호·토큰의 로그 노출 금지
- `dev vs prod 로깅 프로필` — `logback-spring.xml` 프로필별 config

### Edges
- HTTP 요청 진입 → `MDC 컨텍스트` 초기화 (`requestId`·`userId`) : 요청 스코프 시작
- 도메인/서비스 예외 → `GlobalExceptionHandler` → `로그 레벨 매핑` → `구조화 로그` : 단일 진입점
- 상태 변경·중요 이벤트 → `구조화 로그` : Application Service·핸들러 직접 발행 (예외 아님)
- 요청 종료 → `MDC 컨텍스트` 해제 : leak 방지
- `Async` 실행 컨텍스트 → `MDC 컨텍스트` propagation : thread-safe 강제
- 메트릭 추가 요구 → `메트릭 등록 절차` → `Prometheus 메트릭` : label 카디널리티 사전 검토
- `Prometheus 메트릭` → 스크랩 → 대시보드 (Grafana · living-docs 스코프)
- 프로덕션 이상 상태 → `CloudWatch Alarm` → SNS → `Slack Alarm 채널`
- GHA 배포 완료 → `Slack 배포 이벤트` : 버전·git_sha·배포자 필드 포함
- 로그 필드 채우기 → `PII 마스킹` : 이메일·비밀번호·토큰은 절대 raw 출력 X

### Boundaries
- **로그 진입 경계**: 사용자 노출 오류의 로깅은 `GlobalExceptionHandler` 단독 · Controller에서 catch 후 별도 로깅 금지. 서비스 예외는 Application Service 층까지 전파 후 GEH가 처리.
- **level mapping 경계**: 5xx → ERROR / 4xx → WARN / 2xx → INFO. 이 매핑을 우회하는 자체 level 지정 금지.
- **MDC 경계**: `requestId`·`userId`는 요청 스코프에서 항상 세팅. Async 실행 시 propagation 강제. 요청 종료 시 clear.
- **메트릭 명명 경계**: `thirdtool.{area}.{metric}` 형식 (`thirdtool.suggestion.calls_total` 등) · 프로젝트 접두어 강제.
- **카디널리티 경계**: 메트릭 label에 userId · sessionId 등 고카디널리티 필드 사용 금지 (Prometheus 폭발).
- **PII 경계**: 이메일·비밀번호·JWT·refresh token은 raw 상태로 로그·메트릭·알람에 절대 노출 X. userId(Long) 은 허용.
- **알람 채널 경계**: 배포 이벤트와 장애 알람은 **서로 다른 Slack 채널**. `#thirdtool-deploy` vs `#thirdtool-alerts`.
- **dev/prod 로깅 경계**: dev = stdout 사람 가독 · prod = JSON logstash. `logback-spring.xml` profile 활용.

### Invariants
- Controller에서 사용자 노출 비즈니스 예외를 `try-catch`한 사례 0건 (common-core topology와 중복 강제)
  - 감지법: Controller 코드에서 `catch (BusinessException e)` grep
- 5xx 응답 발생 시 로그 level이 ERROR 이외인 사례 0건
  - 감지법: `GlobalExceptionHandler` level 매핑 로직 리뷰
- MDC에 `requestId` 필드 없이 발행된 로그 0건 (Async 포함)
  - 감지법: 로그 sample 확인 · `MdcInterceptor` 존재 확인
- 로그·메트릭에 이메일·비밀번호·JWT의 raw 값이 노출된 사례 0건
  - 감지법: PII 마스킹 규칙 리뷰 · 로그 sample grep
- Prometheus 메트릭 라벨에 userId·sessionId 등 고카디널리티 필드 사용 0건
  - 감지법: 메트릭 등록 코드 리뷰 · Prometheus targets 카디널리티 관찰
- 메트릭 명명이 `thirdtool.` 접두 없이 등록된 사례 0건
  - 감지법: Micrometer 등록 코드 grep
- 배포 알림이 `#thirdtool-alerts` 채널에 · 장애 알림이 `#thirdtool-deploy` 채널에 발송된 사례 0건 (채널 뒤섞임 방지)
  - 감지법: GHA workflow 파일·SNS subscription config 리뷰

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 메트릭 이름·label·카디널리티 → `src/main/java/.../Metrics.java`·Micrometer 등록 코드
- 실제 로그 필드·MDC 세팅 → `Common/Logging/` 하위 코드 · `logback-spring.xml`
- 실제 CloudWatch Alarm 임계값·SNS topic ARN → AWS 콘솔 (M7 신설) · `living-docs/ops-health-board/`
- 실제 Slack webhook URL·채널 이름 → Secrets Manager (M7) · GHA secrets
- 실제 GlobalExceptionHandler 구현 → `Common/Exception/GlobalExceptionHandler.java`
- Runbook 원칙·장애 대응 절차 → 향후 `runbook-authoring` topology (MEDIUM 우선순위 · 재검토 예정)
- 로깅·메트릭 상세 규칙 → `.claude/rules/conventions.md` (관측 절 확장 예정)
- 왜 이렇게 박혔는지 → `docs/adr/` (관측 관련 ADR · 미신설)

---

## 4. Re-pin trigger

- JSON logstash 폐기 · 다른 로그 포맷 도입 (예: OpenTelemetry 통합)
- MDC 프레임워크 변경 (Sleuth · Micrometer Tracing 도입)
- Prometheus 폐기 · APM 도구 도입 (Datadog·NewRelic)
- CloudWatch Alarm 폐기 · 다른 알람 시스템 도입 (PagerDuty)
- Slack 채널 뒤섞임 허용 (단일 채널로 통합)
- GlobalExceptionHandler 단일성 깨야 하는 흐름 도입 (예: 스트리밍 SSE 로그 분리)
- 분산 트레이싱 도입 (트레이스 ID 추가 필요)
- PII 마스킹 정책 완화 (예: 개발 환경에서 원문 로그 허용)
