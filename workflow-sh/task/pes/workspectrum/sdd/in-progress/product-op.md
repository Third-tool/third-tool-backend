## [Product] 운영 메트릭 가시화 — Actuator · Prometheus · Grafana 모니터링 스택

# [Product] 운영 메트릭 가시화 — Actuator · Prometheus · Grafana 모니터링 스택

## Product Vision

> ThirdTool의 런타임 상태(API 응답 시간 · JVM · DB 커넥션 풀)를 Spring Boot Actuator + Micrometer로 메트릭화하고, Prometheus가 스크랩, Grafana가 시각화하는 단일 모니터링 파이프라인을 구축한다.
Product 0의 traceId 기반 로그와 함께 "지표가 튀는 순간 어떤 요청이 왜 그랬는지"를 5초 안에 역추적할 수 있는 관측성 두 번째 레이어를 만든다.
후속 Product(부하 테스트)가 측정·비교할 수 있는 정량 baseline을 확보하는 인프라 토대가 된다.
>

## 배경 및 문제

- 현재 상황 (As-Is)
    - Product 0이 traceId 기반 구조화 로그를 깔아두었지만, 지표는 여전히 "로그를 수동으로 집계해서 추정"하는 상태 — `grep | wc -l`로 RPS를 세는 수준
    - JVM 힙 사용량 · 활성 스레드 수 · DB 커넥션 풀 사용률 같은 런타임 내부 상태를 외부에서 확인할 방법이 없다 — heap dump를 떠야 알 수 있음
    - ReviewSession 시작 / 카드 목록 조회 API의 P95 응답 시간을 알 수 없다 — 평균은 의미 없고 분위수가 필요한데 측정 도구가 없음
    - HikariCP 커넥션 풀이 포화 직전인지 여유 있는지 알 수 없어 EC2 ASG 확장 / 인스턴스 사이즈 결정 근거가 없다
    - `application.yml`에 actuator 설정이 전혀 없어 `/actuator/*` 엔드포인트가 비활성 상태
- 발생하는 문제
    - Product 2(k6 부하 테스트)에서 "VU 50명 부하 시 어디서 병목이 났는지"를 측정할 도구가 없으면, 부하 테스트 자체가 "에러율"만 보는 표면적 검증으로 격하된다
    - DB 커넥션 풀 포화는 발생 후 30초~몇 분간 모든 요청이 timeout으로 깨지는 사고로 직결되는데, 사전 감지 수단이 없다
    - Slow Query · N+1 쿼리가 도메인 모델 확장과 함께 누적되는데도 보이지 않아, 운영 단계에서 누적된 성능 부채가 한 번에 터진다
    - JVM 메모리 누수가 발생해도 OOM이 터지기 직전까지 알 수 없다 — heap 추이 그래프가 없으므로
    - 채용 포트폴리오에서 "성능 측정은 어떻게 했나"에 대한 정량 답변(P95, RPS, 커넥션 풀 사용률)이 불가능해 코드만 보여줘야 한다
- 왜 지금 해결해야 하는가
    - Product 2(부하 테스트)의 baseline 측정은 메트릭 수집기가 먼저 살아 있어야 의미가 있다 — k6의 출력만으로는 서버 내부 상태를 알 수 없음
    - 메트릭 수집은 첫 트래픽이 들어가기 전부터 켜져 있어야 의미가 있다. "어제와 비교해서 오늘 P95가 얼마나 늘었는지"는 어제 데이터가 있어야 가능
    - actuator 엔드포인트 노출 정책은 한 번 운영에 풀리면 보안 검토 없이 바꾸기 어렵다 (`/actuator/env`로 환경변수 유출 사고는 단골 패턴) — 표준을 먼저 박는 게 싸다
    - 면접에서 "P95 응답 시간은 어떻게 측정했나, 커넥션 풀 모니터링은 어떻게 했나, 알림은 어떻게 줬나"는 단골 질문

## 목표 (To-Be)

- `/actuator/prometheus` 엔드포인트가 표준 메트릭을 반환하고, JWT 인증 없이 (단, 내부망 또는 IP 제한 전제로) 접근 가능하다
- Prometheus가 10초 간격으로 ThirdTool 메트릭을 스크랩하며 `up` 메트릭이 1로 유지된다
- Grafana 대시보드에서 다음 4개 섹션이 한 화면에 표시된다
    - API 성능 (P50/P95/P99 응답 시간 · RPS · 에러율)
    - JVM (힙 사용량 · 활성 스레드 · GC pause)
    - DB 커넥션 풀 (활성/유휴/사용률 · 커넥션 대기 시간)
    - 상태 요약 (현재값 + 임계치 색상)
- 모니터링 스택 전체가 `docker compose -f monitoring/docker-compose.monitoring.yml up` 단일 명령으로 실행된다
- 컨테이너 재시작 후에도 Grafana 대시보드 · Datasource 설정이 유지된다 (provisioning + 볼륨 영속화)
- `/actuator/env`, `/actuator/beans`, `/actuator/heapdump` 같은 민감 엔드포인트는 노출되지 않는다

## 설계 결정 (Design Decision)

> **Prometheus pull 방식을 채택한다. Push Gateway는 도입하지 않는다.**
메트릭 수집기와 애플리케이션의 결합을 약하게 유지.
>
> - pull 방식: Prometheus가 `/actuator/prometheus`를 주기적으로 호출 → 애플리케이션은 "메트릭을 노출만" 하고 어디로 보내는지 모름 → 수집기 교체 자유도 높음
> - Push Gateway는 배치/단명 작업(cron · serverless)에 한해 의미가 있는 패턴 — ThirdTool은 상시 가동 모놀리스라 pull로 충분
> - 단점: 애플리케이션 노드 IP를 Prometheus가 알아야 함 → 로컬은 `host.docker.internal`, 스테이징은 고정 IP 또는 DNS 이름으로 해결
> - 이 결정은 ADR로 별도 기록한다 (`ADR-MON-001: Metrics Collection Pattern`)

> **Spring Boot Actuator의 endpoint 노출은 화이트리스트 방식으로 한다.**`include: "*"`는 절대 사용하지 않는다.
>
> - `management.endpoints.web.exposure.include: prometheus, health, info` — 세 개만 명시 노출
> - 이유: `/actuator/env`는 환경변수(DB 비밀번호 등)를 평문 노출, `/actuator/heapdump`는 메모리 덤프 전체를 다운로드 가능 — 공개 노출 시 즉시 보안 사고
> - `/actuator/health`의 상세 정보는 `show-details: when-authorized`로 인증된 호출에만 노출 (DB 상태 정보가 health에 포함되므로 익명 노출 시 내부 구조 유출)
> - 이 결정은 ADR로 별도 기록한다 (`ADR-MON-002: Actuator Endpoint Exposure Policy`)

> **`http_server_requests_seconds`의 URI 라벨은 템플릿화한다.**
path variable이 라벨에 그대로 들어가면 카디널리티가 폭발한다.
>
> - 잘못된 예: `/api/v1/cards/123`, `/api/v1/cards/124`, ... → 카드 ID마다 별도 시계열 → Prometheus 메모리·스토리지 폭발
> - 올바른 예: `/api/v1/cards/{id}` → 카드 API 전체가 1개 시계열로 집계
> - Spring Boot Actuator의 기본 동작이 URI 템플릿화이지만, `@PathVariable`이 아닌 수동 파싱 경로(예: `request.getRequestURI()`로 잡아서 분기)는 라벨이 깨질 수 있어 검증 필요
> - 카디널리티 상한은 약 1만 시계열 — 그 이상은 알람 후 수동 조사
> - 이 결정은 ADR로 별도 기록한다 (`ADR-MON-003: Metric Cardinality Control`)

> **도메인 ID 라벨 카디널리티 방침 — 후행 Product 요청에 대응**
>
> `product-learning-tower.md` · `product-ai-suggestion.md` · `product-ai-interactive-roadmap.md` 가 관측 지표에 `axis_id` · `layer_id` · `facade_id` · `session_id` 등 도메인 ID 를 라벨로 요청할 수 있다. 다음 방침으로 카디널리티 사고를 예방한다.
>
> - **도메인 ID 는 라벨로 직접 승격하지 않는다** — user_id 와 동일. 요청 시 반드시 bucket 로 요약.
> - **허용 라벨 조합**:
>   - `scope`: `AXIS` / `LAYER` enum — 카디널리티 2 (허용)
>   - `provider`: `static` / `llm` / `fallback:llm→static` — 카디널리티 ≤ 5 (허용)
>   - `port_type`: `layer` / `axis` / `roadmap` / `selections` — 카디널리티 4 (허용)
>   - `role`: `backend-developer` / `planner` / `designer` / `problem-solver` / `generic` — 카디널리티 5 (허용)
> - **거부 라벨**:
>   - `axis_id`, `layer_id`, `facade_id`, `session_id`, `user_id` — 무한 성장, 즉시 거부
>   - 대안: `axis_bucket` (예: top-N axis 만 직접 라벨, 나머지 `other`) — 필요 시 별도 결정
> - 로그(MDC) 에는 위 ID 를 자유롭게 실을 수 있음 (`product-log.md` MDC 화이트리스트 참조). 지표 라벨만 엄격.

> **v1은 수집 + 시각화만. 알림(Alertmanager)은 v2.**
알림은 메트릭 안정화 후 도입.
>
> - 이유: 알림 룰은 "정상 범위"를 알아야 임계치 설정이 가능 — 1~2주간 메트릭 추이를 본 후에야 합리적 임계치(P95 500ms, 에러율 1%, 풀 사용률 80%) 결정 가능
> - Grafana의 threshold 색상 표시(녹색/주황/빨강)는 v1에 포함 — "사람이 대시보드를 볼 때" 임계 위반을 알 수 있게
> - Alertmanager + Slack 알림은 Product 2 부하 테스트로 임계치 캘리브레이션 완료 후 v2 Product로 분리
> - 이 결정은 ADR로 별도 기록한다 (`ADR-MON-004: Alerting Out of v1 Scope`)

## 대안 검토 (Alternatives Considered)

> 큰 갈림길마다 "왜 이것이 아니고 저것인가"를 남긴다. 거부된 안에도 합리적 근거가 있었음을 보임으로써 현재 선택의 트레이드오프를 명확히 한다.

### 메트릭 노출 방식

**Option A — 자체 메트릭 endpoint 직접 구현** (Controller에 `@GetMapping("/internal/metrics")` 만들기)
- 장점: 의존성 0, 노출 포맷·필드 완전 제어
- 거부 이유:
    - JVM 힙·GC·스레드·HikariCP 풀 같은 런타임 내부 지표를 직접 수집하는 코드를 매번 짜야 함 — 본질적 복잡도가 아닌 우발적 복잡도
    - Prometheus exposition format(`# HELP`, `# TYPE`, histogram bucket) 직렬화를 직접 구현하면 호환성 회귀가 발생
    - HTTP 지연 메트릭은 매 Controller에 AOP/Interceptor 부착이 필요 — Micrometer는 `WebMvcMetricsFilter`로 무료
- 검토 시점에 망설인 부분: "actuator는 무겁다"는 통념. 실측 의존성 크기(~3MB)는 무시 가능 수준이며, starter는 사용한 endpoint만 로드한다는 점을 확인 후 기각

**Option B (선택) — Spring Boot Actuator + Micrometer Prometheus Registry**
- 비용: Actuator endpoint 보안 정책을 별도 관리 (화이트리스트 노출 + Security 등록)
- 보상: JVM·HikariCP·Tomcat·Logback 메트릭이 무설정으로 표준화. HTTP 지연 히스토그램은 `percentiles-histogram: true` 한 줄로 활성화

### 수집 방식 (pull vs push)

**Option A — Push Gateway** (앱이 메트릭을 Pushgateway로 push)
- 장점: 메트릭 수집기가 앱의 IP·DNS를 몰라도 됨. Fargate처럼 IP가 매번 바뀌는 환경에 친화적
- 거부 이유:
    - ThirdTool은 상시 가동 모놀리스이지 배치/단명 job이 아님 — Push Gateway는 본래 단명 작업을 위한 패턴이며 상시 서비스에 쓰면 "마지막 push 시점"의 게이지가 영원히 남아 오해를 유발
    - Gateway 자체가 추가 SPOF가 됨

**Option B (선택) — Prometheus pull (scrape)**
- 비용: 앱 노드 IP/DNS를 Prometheus가 알아야 함 — 현재는 `host.docker.internal` / static target으로 단순화
- 보상: 앱은 메트릭 노출만 하고 수집 책임은 외부로 분리 — 수집기 교체(다른 Prometheus 인스턴스, AMP) 자유도 확보

### Service Discovery

**Option A — ECS Service Discovery / AWS CloudMap**
- 장점: Fargate에서 task 교체 시 IP 변경을 자동 추종
- 보류 이유: 현재 단일 EC2 / 단일 docker compose 환경. 미래 Fargate 전환 시점에 도입 (Out of Scope)

**Option B (선택) — Static Config (`targets: ["host.docker.internal:8080"]`)**
- 비용: 호스트 IP가 변하면 `prometheus.yml`을 직접 수정해야 함
- 보상: 인프라 단순화. 토이 규모에서 동적 SD의 인지 비용이 더 큼

### 메트릭 저장소

**Option A — AMP (Amazon Managed Prometheus)**
- 장점: 저장소 운영 부담 0, 멀티 AZ HA, 장기 보관
- 보류 이유: 월 비용·인입 throughput 과금 모델이 토이 규모엔 과잉. 단일 사용자·소량 메트릭에선 self-host가 압도적으로 저렴

**Option B — Datadog / NewRelic 등 SaaS APM**
- 보류 이유: 비용 외에도 메트릭/로그/APM이 한 벤더에 잠기는 lock-in. 학습 가치 측면에서도 Prometheus 직접 운영이 우선

**Option C (선택) — Self-Hosted Prometheus (Docker Compose)**
- 비용: 디스크 가득·재시작 시 시계열 손실 책임을 본인이 짐
- 보상: 비용 0, Promtool/PromQL 학습 가능, 추후 AMP로 마이그레이션 시 동일 쿼리·동일 대시보드 재사용

### 시각화 도구

**Option A — Grafana Cloud / AMG (Amazon Managed Grafana)**
- 보류 이유: AMP와 동일 — Managed 비용 정당화 불가. v2 트래픽 발생 후 재검토

**Option B (선택) — Self-Hosted Grafana + Provisioning**
- 비용: 컨테이너 재시작 시 설정 유실 방지를 위한 볼륨·provisioning 셋업 필요
- 보상: 무료, 대시보드 JSON을 Git으로 버전관리 (Dashboard as Code)

### 대시보드 코드화 방식

**Option A — Grafana JSON 수동 export/commit** (선택)
- 비용: GUI 수정 → JSON export → commit 한 단계 수동 워크플로
- 보상: 가장 단순. v1 단일 대시보드 규모에서 자동화 가치 < 자동화 비용

**Option B — Terraform Grafana Provider / Jsonnet+Grafonnet**
- 보류 이유: 대시보드가 2-3개를 넘어 5개+로 성장하고 환경별(prod/staging) 분기가 생기면 도입 검토. 현 시점엔 과잉 추상화

### 메트릭 종류 범위 (어디까지)

**Option A — JVM + HTTP만 (v1)**
- 거부 이유: HikariCP 풀 사용률은 가장 흔한 운영 장애 사전 신호 — 빼면 안 됨

**Option B (선택) — JVM + HTTP + HikariCP (v1)**
- 비용: 4개 섹션 대시보드 작성 부담
- 보상: 사고 직전 신호(풀 포화·heap 누수·P95 회귀)를 모두 커버

**Option C — 비즈니스 메트릭 (`review_session_started_total`, `login_success_total`) 까지 (v2)**
- 보류 이유: 인프라 메트릭이 안정화되고 임계치 캘리브레이션이 끝난 v2 시점에 도메인별로 점진 추가. 첫 도입부터 비즈니스 메트릭을 강제하면 카디널리티 사고 위험

### 알람 도구

**Option A — Prometheus Alertmanager**
- 장점: Prometheus와 자연스러운 통합. 라우팅·억제 룰이 강력
- 보류 이유: v1에선 임계치를 모름 — 1-2주 추이를 본 후에야 합리적 임계치 도출 가능 (설계 결정 §v1 Scope 참조). v2에서 도입

**Option B — Grafana Alerting** (보류)
**Option C — CloudWatch Alarms** (보류)

### Actuator endpoint 노출 정책

**Option A — ALB로 공개 + IAM auth**
- 거부 이유: `/actuator/prometheus`에 IAM 서명을 매번 붙이는 Prometheus 사이드카가 필요 — 의존성 증가

**Option B — `/actuator/prometheus`만 Spring Security `permitAll` + 내부망/보안 그룹 제한** (선택)
- 비용: 보안 그룹 / NACL을 인프라 레이어에서 관리해야 함 (별도 Product)
- 보상: 앱 코드는 단순. 메트릭 endpoint 자체엔 비밀 정보 없음 — 노출되어도 카디널리티·요청 수·내부 URI 정도 (수용 가능한 표면)

## 전체 아키텍처 (High-Level Architecture)

> 컴포넌트 다이어그램과 핵심 플로우. 본문 5페이지보다 다이어그램 1장이 더 강하다.

### 컴포넌트 배치

```
[운영자 브라우저]
    │
    │  GET http://grafana:3000  (대시보드 조회)
    ▼
┌──────────────────────────────────────────────────────────┐
│  Grafana (self-hosted, Docker)                           │
│  ├─ provisioning/datasources/datasource.yml              │
│  ├─ provisioning/dashboards/dashboard.yml                │
│  └─ dashboards/thirdtool.json (4섹션)                    │
└──────────────────────────────────────────────────────────┘
                          │
                          │  PromQL query (proxy)
                          ▼
┌──────────────────────────────────────────────────────────┐
│  Prometheus (self-hosted, Docker)                        │
│  ├─ scrape_interval: 10s                                 │
│  ├─ targets: host.docker.internal:8080                   │
│  └─ TSDB volume: prometheus-data                          │
└──────────────────────────────────────────────────────────┘
                          │
                          │  HTTP GET /actuator/prometheus (10s 주기 pull)
                          ▼
┌──────────────────────────────────────────────────────────┐
│  ThirdTool Spring Boot Application                       │
│  ├─ Spring Security                                       │
│  │     └─ permitAll: /actuator/prometheus,/health,/info  │
│  │     └─ denyAll:   /actuator/**                        │
│  ├─ MdcLoggingFilter.shouldNotFilter(/actuator/*)        │
│  └─ Spring Boot Actuator + Micrometer Prometheus         │
│        ├─ /actuator/prometheus (text exposition format)  │
│        ├─ http_server_requests_seconds_bucket (P95/P99)  │
│        ├─ jvm_memory_* / jvm_threads_* / jvm_gc_*        │
│        └─ hikaricp_connections_*                         │
└──────────────────────────────────────────────────────────┘

[v2 — Alertmanager 분기 (Out of Scope)]
       Prometheus ──rule eval──► Alertmanager ──route──► Slack / Email
                  (recording rule)         (silence/group)
```

### 핵심 플로우

**1. 메트릭 노출 (앱 부팅 시)**
```
Spring Boot 부팅
    │
    ├─ spring-boot-starter-actuator 자동 구성
    ├─ MicrometerPrometheusRegistry 빈 등록
    ├─ MeterFilter: tag application=thirdtool 강제 적용
    ├─ WebMvcMetricsFilter 가 모든 요청을 측정
    │     └─ URI 템플릿(`/api/v1/cards/{id}`)으로 라벨 정규화
    │
    ▼
GET /actuator/prometheus
    → 200 OK
    → http_server_requests_seconds_bucket{uri="/api/v1/cards/{id}",method="GET",status="200",le="0.1"} 42
    → ...
```

**2. 정기 스크랩 (Prometheus 10초 주기)**
```
Prometheus scheduler ─every 10s─► HTTP GET /actuator/prometheus
                                    ├─ 응답 시계열 파싱
                                    ├─ TSDB 청크에 append (2시간 단위 block)
                                    └─ up{job="thirdtool"} 게이지 기록 (성공=1, 실패=0)

스크랩 실패 시 (앱 다운/네트워크 단절) → up=0 + 직전 시계열 보존 (gap이 그래프에 표시)
```

**3. 대시보드 조회 (운영자)**
```
운영자 ─브라우저─► http://localhost:3000/d/thirdtool
                     │
                     ▼
                Grafana Panel ─PromQL query─► Prometheus /api/v1/query_range
                                                 │
                                                 ▼
                                            histogram_quantile(0.95, sum by(le,uri)(rate(...[1m])))
                                                 │
                                                 ▼
                                            time series JSON
                     ◄──────── 패널 렌더 (threshold 색상 적용) ──────────
```

**4. 카디널리티 방어 (요청 처리 시 자동)**
```
HTTP GET /api/v1/cards/123 ─►  WebMvcMetricsFilter
                                  │
                                  ▼  Spring HandlerMapping에 등록된 패턴 매칭
                                  ▼
                            uri 라벨 = "/api/v1/cards/{id}"  (✓ 1개 시계열)
                                  │
                                  ▼
HTTP GET /foo/bar (미존재) ──►   uri 라벨 = "UNKNOWN"      (✓ 폭발 방지)
```

### Out-of-Process 의존

- **Docker Engine**: `monitoring/docker-compose.monitoring.yml` 단일 명령으로 Prometheus + Grafana 컨테이너 기동
- **Docker named volumes**: `prometheus-data` (TSDB 시계열), `grafana-data` (Grafana 설정·세션) — `docker compose down` 후에도 유지
- **`host.docker.internal` DNS**: Mac/Windows에서 컨테이너 → 호스트 통신용. Linux는 `extra_hosts: ["host.docker.internal:host-gateway"]`로 동등 처리
- **운영 환경 보안 그룹 / NACL**: Prometheus 노드 IP만 `/actuator/prometheus` 접근 허용 — 인프라 책임 (별도 Product)

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 대응

| 시나리오 | 증상 | 1차 방어 | 2차 방어 / 탐지 |
| --- | --- | --- | --- |
| `/actuator/env` 공개 노출 사고 | 환경변수(DB 비밀번호) 평문 응답 | `exposure.include`에 `env` 누락 + `exposure.exclude: env`로 이중 차단 | Story 1-1 AC: 통합 테스트로 `/actuator/env` 404 검증 |
| `/actuator/heapdump` 노출 | 메모리 덤프 파일 외부 다운로드 가능 | 화이트리스트 방식 + `denyAll(/actuator/**)` | Security 슬라이스 테스트로 401/403 검증 |
| Prometheus TSDB 디스크 가득 | 신규 스크랩 실패, 기존 시계열 손실 | retention 기본 15일 (volume 크기 산정 시 고려) | `prometheus_tsdb_storage_blocks_bytes` 자체 메트릭으로 사용량 추적 (v2 self-monitoring) |
| URI 라벨 카디널리티 폭발 | Prometheus 메모리 OOM, 쿼리 지연 | URI 템플릿화 (Spring 기본 동작) + 미존재 경로는 `UNKNOWN`으로 집계 | `count(count by(uri)(http_server_requests_seconds_count))` 알람 임계 (현재 100, 상한 1만 — v2 알람) |
| Prometheus → 앱 scrape 실패 | `up{job="thirdtool"}=0`, 그래프에 gap | 네트워크 확인 + 앱 health check | `up == 0` 1분 지속 시 알람 (v2) |
| `MdcLoggingFilter`가 actuator 호출도 로깅 | scrape 10초마다 로그 노이즈 → 로그 디스크 압박 | `shouldNotFilter(/actuator/**)` (Story 1-1 AC) | 로그 볼륨 추이 (Product 0 연계) |
| Grafana admin 비밀번호 기본값 (`admin/admin`) | 외부 노출 시 대시보드 변조·datasource 정보 유출 | `.env.monitoring`에서 `GF_SECURITY_ADMIN_PASSWORD` 강제 주입 + `.env.monitoring.example`만 커밋 | Grafana 로그인 실패 감사 (v2) |
| Grafana `allow_sign_up: true` 방치 | 임의 사용자 자가 가입 | `GF_USERS_ALLOW_SIGN_UP: "false"` 명시 (Story 2-1 docker-compose) | 인프라 코드 리뷰 |
| 컨테이너 재시작 시 시계열·대시보드 유실 | 부하 테스트 전후 비교 불가능 | named volume (`prometheus-data`, `grafana-data`) + provisioning | `docker compose down && up` 회귀 테스트 (Story 2-1 AC) |
| Histogram bucket 누락 (`percentiles-histogram: false`) | `histogram_quantile()` 계산 불가, P95 패널 빈 화면 | `application.yml`에 `percentiles-histogram: true` 명시 (Story 1-1 AC) | Story 3-1 AC: `http_server_requests_seconds_bucket` 존재 확인 |
| Grafana 인증 우회 (anonymous 활성화 실수) | 비공개 대시보드가 익명 노출 | `GF_AUTH_ANONYMOUS_ENABLED` 미설정(기본 false) | provisioning yaml 코드 리뷰 |

### 관측 지표 (메타 — 모니터링 스택 자체)

- `up{job="thirdtool"}` — scrape 성공률 (v1 핵심 KPI, 99% 목표)
- `scrape_duration_seconds{job="thirdtool"}` — 스크랩 응답 시간 (앱 측 actuator 성능 지표)
- `scrape_samples_scraped{job="thirdtool"}` — 회당 노출 시계열 수 (카디널리티 추세)
- `prometheus_tsdb_head_series` — Prometheus가 보관 중인 활성 시계열 총 수 (카디널리티 폭발 조기 감지)
- `prometheus_tsdb_storage_blocks_bytes` — TSDB 디스크 사용량
- Grafana 자체 메트릭(`/metrics` endpoint): 활성 세션 수, 알람 평가 시간 (v2 Alerting 도입 시 합류)
- 알람 발생률 / false positive 비율 — v2에서 Alertmanager 도입 후 정량화

### 로깅 정책 (Product 0 연계)

- `/actuator/**` 경로는 `MdcLoggingFilter.shouldNotFilter`로 traceId MDC 미주입 → Product 0의 `request.start/end` 로그가 찍히지 않음
- 단, Prometheus scrape 실패가 의심되면 임시로 actuator 경로 필터를 풀고 1회 진단 (수정 후 즉시 복구) — 운영 절차 문서화 대상

## 롤아웃 / 마이그레이션 (Rollout)

### 전제 — 프로덕션 트래픽 없음 + 단일 운영자

현재 사용자 0명. 따라서 **메트릭 노출 정책 변경에 따른 클라이언트 영향이 없으며, 일괄 전환**한다. 만약 외부 모니터링 시스템이 이미 연결되어 있었다면 다음 패턴을 재사용한다:
- 1주차: 기존 endpoint 병행 노출 + 새 actuator endpoint 추가
- 2주차: 외부 시스템 endpoint 전환 안내
- 3주차: 구 endpoint 제거

### 단계별 도입 순서 (Epic 의존성과 일치)

```
[Phase 1 — Epic 1: 메트릭 노출]
  Story 1-1 (Actuator + Micrometer Prometheus 구성)
    ├─ build.gradle 의존성 추가 (micrometer-registry-prometheus)
    ├─ application.yml management 블록 화이트리스트 노출
    ├─ SecurityConfig: /actuator/prometheus permitAll + /actuator/** denyAll
    ├─ MdcLoggingFilter.shouldNotFilter(/actuator/**)
    └─ 검증: curl /actuator/prometheus 200 / curl /actuator/env 404
                    │
                    ▼
[Phase 2 — Epic 2: 수집·시각화 스택]
  Story 2-1 (Prometheus + Grafana Docker Compose)
    ├─ monitoring/ 디렉토리 + docker-compose.monitoring.yml
    ├─ prometheus.yml: scrape_interval 10s, target host.docker.internal:8080
    ├─ Grafana provisioning (datasource + dashboard provider)
    ├─ named volume (prometheus-data / grafana-data)
    ├─ .env.monitoring.example (GRAFANA_ADMIN_PASSWORD)
    └─ 검증: targets UP / 재시작 후 데이터 유지
                    │
                    ▼
[Phase 3 — Epic 3: Grafana 대시보드 표준화]
  Story 3-1 (thirdtool.json 4섹션 대시보드)
    ├─ 상태 요약 (Stat 4개, threshold 색상)
    ├─ API 성능 (P50/P95/P99, RPS, 에러율)
    ├─ JVM (heap, threads, GC pause)
    ├─ DB 커넥션 풀 (active/idle, 사용률, 대기 시간 P95)
    └─ JSON export → monitoring/grafana/dashboards/thirdtool.json 커밋
                    │
                    ▼
[Phase 4 — v2 (Out of Scope)]
  - Alertmanager + Slack 라우팅 (Product 2 부하 테스트 후 임계치 캘리브레이션)
  - Loki + Promtail 로그 집계 (Product 0과 통합)
  - 비즈니스 메트릭 카운터 (`review_session_started_total` 등 도메인별 PR)
```

### 환경별 설정 분기

- **로컬 (`application.yml` 기본 + `application-dev.yml`)**: `management.endpoints.web.exposure.include: prometheus,health,info`. `/actuator/prometheus`는 Spring Security `permitAll`. Prometheus target은 `host.docker.internal:8080`
- **prod (`application-prod.yml`)**: 동일한 노출 정책. `/actuator/prometheus`는 보안 그룹/NACL로 Prometheus 노드 IP만 접근 허용 (인프라 책임, 별도 Product). `show-details: when-authorized` 유지
- **`.env.monitoring`**: `GRAFANA_ADMIN_PASSWORD` 환경별 분리. `.env.monitoring.example`만 커밋, 실제 파일은 `.gitignore`
- 현재 `application.yml`의 `management.endpoints.web.exposure.include: []` (빈 배열) 상태 — Story 1-1에서 `prometheus, health, info`로 교체

### 롤백 계획

- 각 Story는 단일 PR + 코드 변경으로 격리
- **Phase 1 롤백**: `build.gradle`에서 `micrometer-registry-prometheus` 제거 + `application.yml` management 블록 원복 → actuator 무력화. 클라이언트 영향 0
- **Phase 2 롤백**: `docker compose down -v` → 컨테이너+볼륨 제거. 앱은 무영향 (Prometheus가 사라져도 앱은 메트릭만 노출하고 끝)
- **Phase 3 롤백**: `monitoring/grafana/dashboards/thirdtool.json` 이전 커밋으로 revert → `docker compose restart grafana`로 reprovision
- Flyway 마이그레이션 없음 — DB 스키마 변경 0건. 본 Product 전체가 인프라/설정 변경에 한정됨

## 성공 지표 (KPI)

| 지표 | 현재 값 | 목표 값 | 측정 방법 |
| --- | --- | --- | --- |
| `/actuator/prometheus` 응답 가능 여부 | 비활성 | 200 OK + 메트릭 텍스트 반환 | `curl localhost:8080/actuator/prometheus` |
| Prometheus 스크랩 성공률 (`up`) | — | 99% 이상 | `up{job="thirdtool"}` 1주일 평균 |
| 노출된 actuator 엔드포인트 수 | 0 | 3 (prometheus · health · info) | `/actuator` 디렉토리 응답 |
| 민감 엔드포인트 응답 코드 | — | 404 (env · beans · heapdump) | `curl /actuator/env` 등 |
| Grafana 대시보드 패널 수 | 0 | 10개 이상 (4개 섹션) | 대시보드 JSON 검사 |
| HTTP URI 라벨 카디널리티 | — | 100개 이하 (API 경로 수 + 여유) | `count(count by(uri)(http_server_requests_seconds_count))` |
| docker compose up 단일 명령 실행 | — | 가능 (스택 전체 기동) | 신규 환경에서 검증 |
| 컨테이너 재시작 후 대시보드 유지 | — | 유지 (provisioning + 볼륨) | `docker compose down && up` 후 확인 |

## Scope

- **In Scope**
    - `spring-boot-starter-actuator` + `micrometer-registry-prometheus` 의존성 추가 및 `application.yml` 설정
    - `/actuator/prometheus` Spring Security 화이트리스트 등록
    - 민감 엔드포인트(`env · beans · heapdump · threaddump`) 명시적 비활성화
    - `monitoring/` 디렉토리 구조 (`docker-compose.monitoring.yml` · Prometheus 설정 · Grafana provisioning)
    - Prometheus 스크랩 설정 (job · interval · target)
    - Grafana Datasource 자동 등록 (provisioning)
    - 4개 섹션 커스텀 대시보드 (`thirdtool.json`) 및 코드베이스 커밋
    - 컨테이너 볼륨 영속화 (Prometheus 시계열 데이터 · Grafana 설정)
    - README 실행 가이드
- **Out of Scope**
    - Alertmanager · Slack/PagerDuty 알림 (v2 별도 Product)
    - Loki + Promtail 로그 집계 (v2)
    - OpenTelemetry · 분산 트레이싱 (v2)
    - CloudWatch · DataDog 등 외부 SaaS 모니터링 연동 (v2)
    - APM (Application Performance Monitoring) 도구 도입 (v2)
    - 비즈니스 메트릭 커스텀 카운터 (예: `review_session_started_total`) — 필요 시점에 도메인별 PR로 추가
    - 모니터링 스택 자체의 HA 구성 (Prometheus federation 등) — 토이 규모에 과잉

## 대상 사용자

- 주요 사용자: ThirdTool 백엔드 개발자 (1인 운영)
- 사용 맥락:
    - Product 2 부하 테스트 실행 중 → Grafana 대시보드를 동시에 띄우고 P95·풀 사용률·스레드 추이를 실시간 관찰
    - 운영 중 응답 지연 신고 접수 시 → Grafana에서 시각 확인 → Product 0 로그를 같은 시각의 traceId로 추적
    - 신규 API 배포 후 → 1시간 내 P95 추이 확인하여 회귀 감지
    - 면접·포트폴리오 설명 시 → 대시보드 스크린샷과 baseline 수치로 정량 답변
    - JVM 튜닝 / HikariCP 사이즈 결정 시 → 1주일 추이 데이터를 근거로 결정

## 연결된 Epic 목록

- [ ]  Epic 1. 메트릭 노출 — Actuator + Micrometer Prometheus 엔드포인트 구성 · 민감 엔드포인트 차단
- [ ]  Epic 2. 수집·시각화 스택 — Prometheus + Grafana Docker Compose · Datasource provisioning · 볼륨 영속화
- [ ]  Epic 3. Grafana 대시보드 구성 — API 성능 · JVM · DB 커넥션 풀 · 상태 요약 4개 섹션

## 관련 문서

- 상위 문서: ThirdTool 백엔드 컨벤션 · 운영 표준
- 선행 Product: Product 0 (로그 관리 기반 구축) — traceId 기반 상관관계 분석의 토대
- 후속 Product: Product 2 (부하 테스트 & 병목 분석) — 이 Product의 대시보드를 baseline 측정에 활용
- 참고 자료: Spring Boot Actuator 공식 문서 · Micrometer Prometheus 가이드 · Grafana Dashboard ID 4701 (JVM Micrometer)
- ADR 후보: `ADR-MON-001` Metrics Collection Pattern · `ADR-MON-002` Actuator Endpoint Exposure · `ADR-MON-003` Metric Cardinality Control · `ADR-MON-004` Alerting Out of v1 Scope

## 열린 질문 (Open Questions)

> v1 출시 후 또는 트래픽·운영 데이터가 누적된 뒤 답을 정해야 하는 항목. 본 Product 범위에서는 결정 보류.

### 1. AMP / AMG (Amazon Managed Prometheus/Grafana)로의 전환 시점

- 현재 self-hosted (Docker Compose) → AMP/AMG 전환 트리거는?
- 후보 기준: (a) 일일 시계열 수가 100만 샘플 초과 (b) 운영자 2명 이상 + 24/7 알람 필요 (c) 멀티 AZ HA 요구
- 전환 시 PromQL은 호환되지만 알람 룰 마이그레이션·IAM 인증 도입 비용 발생 — 사전 견적 필요
- 결정 보류 사유: 현재 트래픽 규모에서 비용 정당화 불가

### 2. OpenTelemetry 도입 (Micrometer Tracing → OTel SDK)

- 현재: Micrometer가 메트릭 한정. 분산 트레이싱은 Product 0의 traceId(MDC)만 존재
- OpenTelemetry 도입 시 메트릭+트레이스+로그가 통합 SDK로 흐름 — 단, 다음을 확정 필요:
    - Collector를 사이드카로 두는가 (sidecar) vs DaemonSet 패턴 vs 앱 직접 export
    - 백엔드 — Tempo / Jaeger / AWS X-Ray
    - Micrometer Tracing (Brave/OTel 어댑터)로의 점진 마이그레이션 경로
- 결정 보류 사유: 단일 모놀리스에서 분산 트레이싱 가치 한정. MSA 전환 또는 외부 API 의존이 늘어날 때 재검토

### 3. SLO 정의 — 무엇을 약속할 것인가

- 후보 SLO:
    - `availability`: `/health`가 99.5% 시간 동안 UP
    - `latency`: API P95 < 500ms (어떤 API? 전체 평균? 핵심 endpoint만?)
    - `error rate`: 5xx 비율 < 1%
- 결정 필요 항목:
    - SLO 측정 윈도우 (rolling 7일? 30일?)
    - 핵심 user journey 정의 (로그인·카드 조회·ReviewSession 시작 → 어떤 것을 SLI로 채택?)
    - Error budget 정책 (소진 시 신규 기능 배포 동결? 토이 규모에선 과잉?)
- 결정 보류 사유: 사용자 0명 — SLO를 정의해도 위반/준수 판정의 의미가 빈약. Product 2 부하 테스트 baseline 확보 후 정의

### 4. Burn rate 알람 도입

- 표준 Prometheus 알람(`error_rate > 5% for 5m`)은 너무 늦거나 너무 시끄러움
- Burn rate (`SLO 오류 예산 소진 속도`) 기반 알람은 다단계 윈도우(5m + 1h, 30m + 6h)로 false positive를 줄임
- 도입 시점: SLO 정의(§3) 완료 후
- 결정 보류 사유: SLO 미정의 상태에서 burn rate는 정의 불가

### 5. 트레이싱과 메트릭의 통합 (Exemplars)

- Prometheus exemplar 기능: histogram bucket에 traceId를 attach → Grafana에서 P95 spike 클릭 시 해당 trace로 점프
- Micrometer Tracing + Prometheus exemplar 지원 활성화로 가능
- 결정 필요: OpenTelemetry 도입(§2) 결정 후 일괄 도입할지, Micrometer Tracing 단독으로 선도입할지
- 결정 보류 사유: §2와 의존성

### 6. 비즈니스 메트릭 카운터 도입 기준

- `review_session_started_total`, `login_success_total{provider=local|kakao|naver}`, `card_archive_total{reason=MAX_VIEW|MAX_DURATION}` 등
- 도입 비용: 도메인 코드에 Micrometer 의존 침투 + 카디널리티 검토
- 결정 필요:
    - 어느 도메인부터 시작할 것인가 (인증부터? 카드부터?)
    - tag 설계 가이드 (provider·reason 같은 enum은 OK, userId·cardId는 카디널리티 사고)
    - 메트릭 vs 로그 집계의 경계 (Product 0과의 책임 분할)
- 결정 보류 사유: 인프라 메트릭이 1-2주 안정화된 후 도메인별 PR로 추가. 본 Product는 인프라 토대에 집중

### 7. `/actuator/health`의 readiness/liveness 분리

- Spring Boot Actuator는 `/actuator/health/liveness` 와 `/actuator/health/readiness`를 자동 분리 지원
- 도입 시 K8s/ECS health check가 정밀해짐 (DB 일시 단절을 liveness 실패로 오판하지 않음)
- 결정 보류 사유: 현재 단일 EC2/Docker Compose — health 분리 효용 한정. Fargate/K8s 전환 시점에 도입

---

| Epic | Story 수 | SP 합계 |
| --- | --- | --- |
| Epic 1. 메트릭 노출 | 1 | 3 |
| Epic 2. 수집·시각화 스택 | 1 | 3 |
| Epic 3. Grafana 대시보드 구성 | 1 | 5 |
| **합계** | **3** | **11 SP** |

**진행 순서 (필수):** Epic 1 → 2 → 3. 일직선 의존성으로 건너뛸 수 없습니다. Epic 1의 메트릭 노출 → Epic 2의 수집 스택 → Epic 3의 시각화 순으로만 각 단계가 의미를 가집니다.

**Product 0와의 연결 포인트**

- Story 1-1의 `MdcLoggingFilter.shouldNotFilter()`로 actuator 경로 제외 → Product 0의 로그 노이즈 방지
- Product 2에서 P95가 튀는 시각을 Grafana에서 발견 → 동일 시각의 Product 0 로그를 traceId로 검색 → 원인 요청 식별. 이 흐름이 Product 0 → 1 → 2 순서의 핵심 가치

## Epic 1. 메트릭 노출 — Actuator + Micrometer Prometheus 엔드포인트 구성

# Epic 1. 메트릭 노출 — Actuator + Micrometer Prometheus 엔드포인트 구성

## Epic 목표

> ThirdTool 애플리케이션이 `/actuator/prometheus` 엔드포인트로 표준 메트릭(`http_server_requests_seconds · jvm_* · hikaricp_*`)을 노출하고, 민감 엔드포인트는 명시적으로 차단된 상태를 만든다.
>

## 배경

- Prometheus가 스크랩할 대상이 먼저 존재해야 Epic 2의 수집 스택이 의미를 가짐 — 일직선 의존성의 출발점
- Spring Boot Actuator의 기본 노출 정책은 보안에 취약 (`/actuator/env`는 비밀번호 노출 가능) → 화이트리스트 방식으로 강제 차단
- URI 라벨 카디널리티 폭발은 운영 환경에서 Prometheus 메모리를 빠르게 고갈시키는 단골 사고 → 첫 도입 시 검증 필수

## 핵심 설계 결정

> **Spring Security 설정에서 `/actuator/prometheus`만 인증 우회를 명시한다.**`/actuator/**` 전체를 우회하지 않는다.
>
> - `/actuator/health`는 `when-authorized` 정책으로 익명 호출 시 단순 UP/DOWN만 응답, 인증 호출 시 DB 상태 포함 상세 응답
> - `/actuator/info`는 빌드 정보 (커밋 해시 · 버전) 노출 — 공개되어도 무해
> - `/actuator/prometheus`는 인증 우회 (Prometheus가 토큰 없이 스크랩하기 위함) — 단, 운영 환경에서는 내부망 또는 IP 제한 전제

## 완료 기준 (Definition of Done)

- [ ]  `GET /actuator/prometheus`가 200 OK + 메트릭 텍스트를 반환한다
- [ ]  `http_server_requests_seconds`에 `uri · method · status` 라벨이 포함된다
- [ ]  `application: thirdtool` 태그가 모든 메트릭에 포함된다
- [ ]  `/actuator/env · beans · heapdump`가 404를 반환한다
- [ ]  URI 라벨이 path variable로 채워지지 않고 템플릿(`{id}`)으로 정규화된다
- [ ]  연결된 Story가 모두 Done 상태다

## 산출물 (Deliverables)

- `build.gradle` (의존성 추가)
- `application.yml` (actuator 설정)
- `application-prod.yml` (prod 전용 노출 정책)
- `SecurityConfig` (화이트리스트 등록)
- `/actuator/prometheus` 응답 샘플 스크린샷

## 연결된 Story 목록

- [ ]  Story 1-1. Actuator + Micrometer Prometheus 엔드포인트 구성 · 민감 엔드포인트 차단 (3 SP)

## 내부 메모 / 제약 사항

- `/actuator/prometheus` 응답은 인증 없이 접근 가능 → 운영 환경에서 보안 그룹 / NACL로 Prometheus 노드 IP만 허용 (인프라 책임, 별도 Product)
- Product 0의 `MdcLoggingFilter`는 통과시키되, `request.start/end` 로그가 스크랩 호출마다 찍히지 않도록 actuator 경로 필터링 추가 검토 (Story 1-1 엣지 케이스)

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

### 노출 endpoint 선정 — 어디까지 켤 것인가

**Option A — `include: "*"` (모든 endpoint 노출)**
- 거부 이유: `/actuator/env`(환경변수)·`/actuator/heapdump`(메모리 덤프)·`/actuator/beans`(빈 그래프)는 단독 노출만으로도 즉시 보안 사고. `*`는 미래에 starter가 신규 endpoint를 추가했을 때 의도치 않은 노출을 일으킴

**Option B — `include: prometheus, health, info` + `exclude: env,beans,heapdump,threaddump,configprops,mappings`** (선택)
- 비용: 신규 endpoint 추가 시마다 include/exclude 갱신 필요 (실수 방지를 위한 의도된 마찰)
- 보상: defense in depth — include 누락 + exclude 명시로 이중 차단. 신규 starter 업그레이드 시 발생할 수 있는 자동 노출 회피

**Option C — `include: health` 만 (가장 보수적)**
- 거부 이유: 본 Product의 목적인 메트릭 수집 자체가 불가능 — 과도한 보수

### Security 정책 — `/actuator/**`을 어떻게 다룰 것인가

**Option A — `/actuator/**` 전체 permitAll**
- 거부 이유: 화이트리스트 누락 endpoint가 켜졌을 때 즉시 노출. include 정책과 이중 방어가 깨짐

**Option B — `/actuator/prometheus,/actuator/health,/actuator/info` permitAll + `/actuator/**` denyAll** (선택)
- 비용: Security 설정 라인이 길어짐
- 보상: 화이트리스트 명시 + 나머지 명시적 차단 — defense in depth. application.yml의 include 정책과 일치

**Option C — `/actuator/**` 전체 authenticated + Prometheus가 Basic Auth로 스크랩**
- 보류 이유: Prometheus 설정에 비밀번호 저장 → 시크릿 관리 복잡도 증가. 인프라 레이어(보안 그룹/내부망)에서 차단하는 편이 단순

### Histogram 활성화 vs 단순 timer

**Option A — `percentiles-histogram: false` + `percentiles: 0.5, 0.95, 0.99`** (client-side 분위수 계산)
- 거부 이유: Prometheus가 여러 인스턴스의 분위수를 집계할 때 percentile은 합칠 수 없는 통계 (incompatible aggregate). 분위수는 항상 bucket에서 계산되어야 함

**Option B — `percentiles-histogram: true`** (선택)
- 비용: bucket 시계열이 추가로 노출되어 응답 크기·저장량 증가 (URI당 약 10-20 bucket)
- 보상: `histogram_quantile()` PromQL로 정확한 P95/P99 계산 가능. 멀티 인스턴스 환경에서도 합산 가능

### `MdcLoggingFilter` actuator 경로 처리

**Option A — actuator 경로도 일반 요청처럼 traceId MDC 주입**
- 거부 이유: scrape이 10초마다 발생 → `request.start/end` 로그가 일일 8,640건 누적 (Product 0 로그 디스크 압박)

**Option B — `shouldNotFilter(/actuator/**)`로 MDC 미주입** (선택)
- 비용: actuator 호출 자체는 로그 추적이 안 됨 (디버깅 시 임시로 풀어야 함)
- 보상: 로그 노이즈 제거. Product 0과의 책임 분할 (메트릭은 Prometheus가, 로그는 Product 0가)

---

## Story 1-1. Actuator + Micrometer Prometheus 엔드포인트 구성 · 민감 엔드포인트 차단

### User Story

> As a 백엔드 개발자,
I want `/actuator/prometheus`가 표준 메트릭을 반환하고 민감 엔드포인트는 명시적으로 차단되길,
So that Prometheus가 즉시 스크랩을 시작할 수 있고, 환경변수 / 메모리 덤프 같은 내부 정보가 외부에 노출될 위험이 차단된다.
>

### 설계 노트

- 의존성 (`build.gradle`)

    ```groovy
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'
    ```

- `application.yml` 핵심 설정

    ```yaml
    management:
      endpoints:
        web:
          exposure:
            include: prometheus, health, info
            exclude: env, beans, heapdump, threaddump, configprops, mappings
      endpoint:
        prometheus:
          enabled: true
        health:
          show-details: when-authorized
      metrics:
        tags:
          application: thirdtool
        distribution:
          percentiles-histogram:
            http.server.requests: true
          percentiles:
            http.server.requests: 0.5, 0.95, 0.99
    ```

- `percentiles-histogram: true`가 핵심 — Prometheus의 `histogram_quantile()`로 P95/P99를 계산 가능한 bucket 메트릭을 생성
- `SecurityConfig` 변경

    ```java
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/actuator/prometheus", "/actuator/health", "/actuator/info").permitAll()
        .requestMatchers("/actuator/**").denyAll() // 명시적 차단 (defense in depth)
        // ... 기존 설정
    )
    ```

- `MdcLoggingFilter`의 actuator 경로 제외 로직 추가 — 10초마다 스크랩 호출이 INFO 로그로 찍히면 로그 노이즈

    ```java
    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return req.getRequestURI().startsWith("/actuator");
    }
    ```


### 완료 기준 (Acceptance Criteria)

- [ ]  `curl localhost:8080/actuator/prometheus` 응답이 200 OK이며 `http_server_requests_seconds_count{...}` 라인을 포함한다
- [ ]  메트릭에 `application="thirdtool"` 태그가 포함된다
- [ ]  `http_server_requests_seconds`에 `uri · method · status` 라벨이 포함된다
- [ ]  `http_server_requests_seconds_bucket`이 출력된다 (histogram 활성화 확인)
- [ ]  `/actuator/env · beans · heapdump · threaddump`가 404를 반환한다
- [ ]  `/actuator/health`가 익명 호출 시 `{"status":"UP"}`만, 인증 호출 시 상세 정보 응답
- [ ]  `/actuator/*` 경로 호출은 Product 0의 `request.start/end` 로그를 발생시키지 않는다

### 엣지 케이스

- `GET /api/v1/cards/123` 호출 후 `/actuator/prometheus` 메트릭에서 `uri="/api/v1/cards/{id}"`로 템플릿화되어 표시된다 (path variable이 라벨에 그대로 들어가지 않음)
- 동일 카드 100개를 다른 ID로 조회해도 메트릭 시계열은 1개만 증가한다
- 존재하지 않는 경로(`/foo/bar`) 호출 시 메트릭에 `uri="UNKNOWN"` 또는 빈 값으로 집계되어 카디널리티가 폭발하지 않는다
- 5xx 응답 시 `status="500"` 라벨로 별도 시계열 생성 — 에러율 계산용

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  `curl /actuator/prometheus` 응답 샘플 스크린샷 첨부
- [ ]  민감 엔드포인트 404 응답 스크린샷 첨부 (`/actuator/env`, `/actuator/heapdump`)
- [ ]  카디널리티 검증: `count(count by(uri)(http_server_requests_seconds_count))` 결과 화면
- [ ]  스테이징 배포 확인

### 의존성

- 선행: 없음 (Product 1의 시작 Story)
- 후속: Story 2-1 (Prometheus 스크랩 대상이 먼저 존재해야 함)

### 스토리 포인트

- 추정: 3 SP

---

## Epic 2. 수집·시각화 스택 — Prometheus + Grafana Docker Compose

# Epic 2. 수집·시각화 스택 — Prometheus + Grafana Docker Compose

## Epic 목표

> `docker compose -f monitoring/docker-compose.monitoring.yml up` 단일 명령으로 Prometheus + Grafana 스택이 실행되고, Prometheus가 ThirdTool 애플리케이션을 10초 간격으로 스크랩하며, Grafana가 Prometheus를 Datasource로 자동 등록한 상태를 만든다.
컨테이너 재시작 후에도 시계열 데이터와 Grafana 설정이 유지된다.
>

## 배경

- Epic 1로 메트릭이 노출되어도, 수집·저장·시각화 인프라가 없으면 단순 텍스트 응답에 불과
- 모니터링 스택을 로컬·스테이징에서 동일하게 띄울 수 있어야 환경 간 drift 없이 baseline 측정이 가능
- Grafana 대시보드를 GUI로만 설정하면 코드베이스에 남지 않아 재현 불가능 → provisioning 방식 필수
- 볼륨 영속화 없이는 컨테이너 재시작 시 1주일 추이가 날아감 — 부하 테스트 전후 비교가 불가능

## 핵심 설계 결정

> **Grafana 설정은 provisioning 방식으로 코드베이스에 커밋한다. GUI 설정은 금지.**
Dashboard as Code 원칙.
>
> - Datasource (`provisioning/datasources/datasource.yml`) · Dashboard (`provisioning/dashboards/dashboard.yml`) 모두 yaml로 선언
> - 대시보드 JSON 자체도 `grafana/dashboards/thirdtool.json`으로 커밋 (Epic 3 산출물)
> - 이유: 신규 환경에서 GUI로 재현하면 누락·차이 발생 → "내 로컬에선 잘 보이는데" 사고
> - 단점: GUI에서 수정한 대시보드를 코드로 다시 export하는 한 단계가 필요 → README에 명시

> **Prometheus 스크랩 인터벌은 10초로 고정한다.**
1초·5초로 짧게 하지 않는다.
>
> - 토이 규모에서 1초 인터벌은 Prometheus 부하만 증가시키고 정보 가치는 동일
> - 10초 = Grafana 대시보드 auto-refresh와 동일 주기로 맞춰 일관성 확보
> - 부하 테스트 중 더 세밀한 관찰이 필요하면 임시로 5초로 조정 — 그때만 변경

## 완료 기준 (Definition of Done)

- [ ]  `docker compose up`으로 Prometheus + Grafana가 단일 명령으로 실행된다
- [ ]  `http://localhost:9090/targets`에서 `thirdtool` job이 UP 상태로 표시된다
- [ ]  `http://localhost:3000`에서 Prometheus Datasource가 자동 등록된 상태로 보인다
- [ ]  컨테이너 재시작 후 시계열 데이터와 대시보드 설정이 유지된다
- [ ]  README에 실행 가이드가 있다
- [ ]  연결된 Story가 모두 Done 상태다

## 산출물 (Deliverables)

- `monitoring/docker-compose.monitoring.yml`
- `monitoring/prometheus/prometheus.yml`
- `monitoring/grafana/provisioning/datasources/datasource.yml`
- `monitoring/grafana/provisioning/dashboards/dashboard.yml`
- `README.md` 모니터링 실행 가이드 섹션
- `.env.monitoring.example` (Grafana admin 비밀번호 변수)

## 연결된 Story 목록

- [ ]  Story 2-1. Prometheus + Grafana Docker Compose 구성 · Datasource provisioning · 볼륨 영속화 (3 SP)

## 내부 메모 / 제약 사항

- 로컬 타겟: `host.docker.internal:8080` (Mac/Windows), Linux는 `172.17.0.1:8080` 또는 `network_mode: host`로 대체 — README에 양쪽 안내
- 스테이징 타겟: 별도 `prometheus-staging.yml`로 분리 또는 환경변수 치환 — Epic 3 후속에서 결정
- Grafana 초기 비밀번호는 `.env.monitoring`에서 주입, 기본값 `admin/admin` 사용 금지 — `.env.monitoring.example`만 커밋

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

### 모니터링 스택 배치 — 같은 compose vs 별도 compose

**Option A — 메인 앱과 같은 `docker-compose.yml`에 합본**
- 거부 이유: 앱 컨테이너 재배포(코드 변경) 시마다 Prometheus·Grafana도 함께 재시작 → 시계열 gap. 모니터링이 앱 라이프사이클에 결합

**Option B — `monitoring/docker-compose.monitoring.yml` 분리** (선택)
- 비용: 두 compose 파일 관리. `docker network`를 공유하려면 external network 선언 추가 (현재는 `host.docker.internal`로 우회)
- 보상: 모니터링 스택 라이프사이클 독립. 앱 재배포 시 메트릭이 끊기지 않음

### Datasource·Dashboard 등록 방식

**Option A — Grafana GUI 수동 설정**
- 거부 이유: 신규 환경 재현 불가능. "내 로컬에선 보이는데" 사고 단골

**Option B — Provisioning yaml (datasource.yml + dashboard.yml)** (선택)
- 비용: yaml 스키마 학습 (apiVersion: 1, providers, datasources 구조)
- 보상: 컨테이너만 띄우면 자동 로드. Git으로 버전 관리

**Option C — Grafana HTTP API + 셸 스크립트로 import**
- 거부 이유: 컨테이너 부팅 시 race condition (Grafana 준비 전 호출). provisioning이 공식 권장 패턴

### 볼륨 전략

**Option A — bind mount (`./prometheus-data:/prometheus`)**
- 거부 이유: 호스트 파일시스템 권한·user namespace 문제 빈발 (Linux UID/GID). 컨테이너 사용자(`nobody`)가 mount 디렉토리에 write 권한 없음

**Option B — Docker named volume (`prometheus-data:`)** (선택)
- 비용: 호스트에서 시계열 파일을 직접 보기 어려움 (`docker volume inspect`로 경로 확인 필요)
- 보상: 권한 자동 처리. cross-platform 호환 (Mac/Windows/Linux)

### 비밀번호 관리

**Option A — `docker-compose.yml`에 비밀번호 하드코딩**
- 거부 이유: 명백한 보안 사고

**Option B — `.env.monitoring` 환경변수 + `.gitignore` + `.env.monitoring.example`만 커밋** (선택)
- 비용: 신규 환경에서 `.env.monitoring` 생성 누락 시 컨테이너 부팅 실패 (의도된 마찰)
- 보상: 실제 비밀번호는 Git에 없음. 예시 파일로 가이드 제공

**Option C — Docker Secrets / Vault**
- 보류 이유: 토이 규모에 과잉. 트래픽·운영자 수 증가 시 재검토

### Scrape interval — 10s vs 5s vs 30s

**Option A — 1s 또는 5s** (고해상도)
- 거부 이유: 토이 규모에서 정보 가치는 동일, Prometheus 부하·디스크 사용량만 증가. 1s 인터벌은 일반적으로 단일 호스트에서 500개 타겟 한계

**Option B — 10s** (선택)
- 비용: 짧은 spike(<10s)는 놓칠 수 있음 — 부하 테스트 시 임시 5s로 조정 절차 필요
- 보상: Grafana auto-refresh와 동일 주기. 정보 가치 / 부하 균형

**Option C — 30s 또는 1m**
- 거부 이유: 부하 테스트 시 spike 추적 불가능. P95가 1분 평균으로 묽어짐

---

## Story 2-1. Prometheus + Grafana Docker Compose 구성 · Datasource provisioning · 볼륨 영속화

### User Story

> As a 백엔드 개발자,
I want 단일 docker compose 명령으로 모니터링 스택이 뜨고 재시작해도 설정·시계열이 유지되길,
So that 로컬 / 스테이징에서 동일하게 즉시 모니터링을 시작할 수 있고, 부하 테스트 전후 비교를 위한 1주일 추이 데이터가 보존된다.
>

### 설계 노트

- `monitoring/` 디렉토리 구조

    ```
    monitoring/
    ├── docker-compose.monitoring.yml
    ├── .env.monitoring.example
    ├── prometheus/
    │   └── prometheus.yml
    └── grafana/
        ├── provisioning/
        │   ├── datasources/datasource.yml
        │   └── dashboards/dashboard.yml
        └── dashboards/
            └── thirdtool.json   # Epic 3에서 추가
    ```

- `docker-compose.monitoring.yml`

    ```yaml
    services:
      prometheus:
        image: prom/prometheus:v2.54.0
        ports: ["9090:9090"]
        volumes:
          - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
          - prometheus-data:/prometheus
        restart: unless-stopped
    
      grafana:
        image: grafana/grafana:11.2.0
        ports: ["3000:3000"]
        environment:
          GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_ADMIN_PASSWORD}
          GF_USERS_ALLOW_SIGN_UP: "false"
        volumes:
          - ./grafana/provisioning:/etc/grafana/provisioning:ro
          - ./grafana/dashboards:/var/lib/grafana/dashboards:ro
          - grafana-data:/var/lib/grafana
        depends_on: [prometheus]
        restart: unless-stopped
    
    volumes:
      prometheus-data:
      grafana-data:
    ```

- `prometheus/prometheus.yml`

    ```yaml
    global:
      scrape_interval: 10s
      evaluation_interval: 10s
    
    scrape_configs:
      - job_name: thirdtool
        metrics_path: /actuator/prometheus
        static_configs:
          - targets: ["host.docker.internal:8080"]
            labels:
              env: local
    ```

- `grafana/provisioning/datasources/datasource.yml`

    ```yaml
    apiVersion: 1
    datasources:
      - name: Prometheus
        type: prometheus
        access: proxy
        url: http://prometheus:9090
        isDefault: true
    ```

- `grafana/provisioning/dashboards/dashboard.yml`

    ```yaml
    apiVersion: 1
    providers:
      - name: thirdtool
        folder: ThirdTool
        type: file
        options:
          path: /var/lib/grafana/dashboards
    ```

- README 추가

    ```markdown
    ## 모니터링 스택 실행
    cp monitoring/.env.monitoring.example monitoring/.env.monitoring
    # .env.monitoring 파일에서 GRAFANA_ADMIN_PASSWORD 설정
    docker compose --env-file monitoring/.env.monitoring \
                   -f monitoring/docker-compose.monitoring.yml up -d
    # Prometheus: http://localhost:9090
    # Grafana: http://localhost:3000 (admin / 위에서 설정한 비밀번호)
    ```


### 완료 기준 (Acceptance Criteria)

- [ ]  `docker compose up` 후 `prometheus` · `grafana` 컨테이너가 모두 healthy 상태로 떠 있다
- [ ]  `http://localhost:9090/targets`에서 `thirdtool` job이 `UP` 상태다
- [ ]  `http://localhost:3000` 로그인 시 Prometheus Datasource가 "default"로 등록되어 있다
- [ ]  `docker compose down && docker compose up`을 수행해도 Grafana 비밀번호 · 시계열 데이터가 유지된다
- [ ]  `.env.monitoring.example`은 커밋되고 `.env.monitoring`은 `.gitignore`에 포함된다

### 엣지 케이스

- Spring Boot 앱이 실행되지 않은 상태에서 docker compose up → Prometheus `targets` 화면에서 `thirdtool` job이 `DOWN`으로 표시되고 에러 메시지가 명확
- Linux 환경에서 `host.docker.internal` 미지원 → README에 대체 방법 (`extra_hosts: ["host.docker.internal:host-gateway"]` 추가) 안내
- `.env.monitoring` 파일이 없는 상태에서 `docker compose up` 실행 → 에러 메시지가 명확하게 출력되고 README가 안내됨

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  Prometheus `targets` UP 상태 스크린샷 첨부
- [ ]  Grafana Datasource 자동 등록 스크린샷 첨부
- [ ]  컨테이너 재시작 후 데이터 유지 검증 (스크린샷 또는 명령 출력)
- [ ]  README 실행 가이드 커밋 확인
- [ ]  스테이징 배포 확인

### 의존성

- 선행: Story 1-1 (스크랩할 메트릭 엔드포인트가 먼저 존재해야 함)
- 후속: Story 3-1 (대시보드가 Datasource를 활용)

### 스토리 포인트

- 추정: 3 SP

---

## Epic 3. Grafana 대시보드 구성 — API 성능 · JVM · DB 커넥션 풀 · 상태 요약

# Epic 3. Grafana 대시보드 구성 — API 성능 · JVM · DB 커넥션 풀 · 상태 요약

## Epic 목표

> ThirdTool 운영 상태를 한 화면에서 파악할 수 있는 4개 섹션 대시보드를 작성하고, JSON으로 코드베이스에 커밋해 docker compose up 시 자동 로드되는 상태를 만든다.
임계치 색상 표시(녹색/주황/빨강)로 사람이 대시보드를 볼 때 즉시 이상을 감지할 수 있게 한다.
>

## 배경

- Prometheus가 메트릭을 저장해도 시각화 없이는 의미 해석이 불가능 — 분위수 그래프와 색상 임계치가 의사결정을 가능하게 함
- Product 2 부하 테스트의 실시간 관찰 도구가 이 대시보드 — 4개 섹션이 한 화면에 있어야 부하 테스트 중 시선 이동이 최소화됨
- Grafana 공식 JVM 대시보드(ID 4701)는 베이스로 활용 가치가 있으나, API 성능 / DB 커넥션 풀 / 상태 요약 섹션은 ThirdTool 전용 커스텀 필요
- 대시보드 JSON을 코드베이스에 커밋해야 환경 간 동일 시각화가 보장됨

## 핵심 설계 결정

> **대시보드는 단일 파일(`thirdtool.json`)로 통합한다.**
섹션별 별도 대시보드로 분리하지 않는다.
>
> - 부하 테스트 중 P95 · 풀 사용률 · 스레드 추이를 동시에 봐야 의미 있음 → 화면 전환 비용 제거
> - Grafana의 row 기능으로 4개 섹션을 펼치고 접기 가능 → 평소엔 요약 섹션만 보고 필요 시 펼침
> - 단점: 패널 수가 많아져 초기 로딩이 느려질 수 있음 → 시간 범위 기본값 30분으로 제한해 완화

> **모든 핵심 패널에 threshold 색상을 설정한다.**
단순 그래프가 아닌 의사결정 가능한 시각화.
>
> - P95 응답 시간: 200ms 녹색 · 500ms 주황 · 1000ms 빨강
> - 에러율: 0.5% 녹색 · 1% 주황 · 5% 빨강
> - DB 커넥션 풀 사용률: 60% 녹색 · 80% 주황 · 95% 빨강
> - 활성 스레드 수: 100 녹색 · 200 주황 · 400 빨강 (HikariCP + Tomcat 합산 기준)
> - 임계치는 v1 초기값 — Product 2 부하 테스트 후 캘리브레이션 (v2에서 재조정)

## 완료 기준 (Definition of Done)

- [ ]  Grafana에서 4개 섹션이 한 대시보드에 표시된다 (API 성능 · JVM · DB 커넥션 풀 · 상태 요약)
- [ ]  API P95 패널이 uri별로 분리 표시된다
- [ ]  threshold 색상이 4개 핵심 패널에 설정되어 있다
- [ ]  대시보드 JSON이 `monitoring/grafana/dashboards/thirdtool.json`에 커밋된다
- [ ]  `docker compose up` 시 대시보드가 자동 로드된다
- [ ]  메트릭이 없는 초기 상태에서도 패널이 "No data"로 정상 렌더링된다
- [ ]  연결된 Story가 모두 Done 상태다

## 산출물 (Deliverables)

- `monitoring/grafana/dashboards/thirdtool.json`
- 4개 섹션 스크린샷 (대시보드 전체 + 각 섹션 클로즈업)
- 대시보드 작성 가이드 (`monitoring/README.md` 또는 메인 README 보강) — 향후 패널 추가 절차

## 연결된 Story 목록

- [ ]  Story 3-1. Grafana 대시보드 4개 섹션 구성 · JSON 커밋 · provisioning 자동 로드 (5 SP)

## 내부 메모 / 제약 사항

- Grafana 공식 JVM 대시보드(ID 4701)를 import 후 커스텀 패널 추가 → 별도 dashboard로 분리하지 않고 합본
- 시간 범위 기본값: Last 30 minutes, auto-refresh: 10s
- 대시보드 변경 시 워크플로: GUI에서 수정 → JSON export → `thirdtool.json`에 커밋 → README에 명시

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

### 대시보드 구조 — 단일 vs 분리

**Option A — 섹션별 4개 대시보드 분리** (`api.json`, `jvm.json`, `db.json`, `summary.json`)
- 거부 이유: 부하 테스트 중 P95 spike와 HikariCP 풀 사용률을 **동시 관찰**해야 인과 추정 가능 — 화면 전환 비용이 누적되면 의사결정 지연
- 권장 시점: 패널이 30개를 넘어 단일 대시보드 로딩이 느려지는 시점

**Option B — 단일 `thirdtool.json` + Grafana Row로 4섹션 구획** (선택)
- 비용: 초기 로딩 시 모든 패널이 동시 query → Prometheus 부하 spike (10초마다 N개 query 동시 발사)
- 보상: 한 화면 동시 관찰. Row collapse/expand로 평소엔 요약만 보고 필요 시 펼침

### Threshold 색상 — 정량 임계 vs 단순 라인 그래프

**Option A — 단순 라인 그래프 (threshold 없음)**
- 거부 이유: 운영자가 "이 값이 정상인가"를 매번 머리에서 판정 → 인지 비용. 야간/피로 상황에 사고로 직결

**Option B — 핵심 패널에 threshold 색상 (녹/주/빨)** (선택)
- 비용: 임계치 초기값이 추정치 — false positive 발생 가능 (v2에서 캘리브레이션 약속)
- 보상: 시선이 닿는 순간 "정상/주의/위험" 즉시 인지. 대시보드가 "정보 표시"에서 "의사결정 도구"로 격상

### Histogram 분위수 계산 위치

**Option A — `quantile_over_time(0.95, http_server_requests_seconds_sum[1m])`** (Summary 메트릭 분위수)
- 거부 이유: Prometheus Summary는 client-side 분위수라 합산 불가. 다인스턴스 환경에서 무의미한 값

**Option B — `histogram_quantile(0.95, sum by(le,uri)(rate(http_server_requests_seconds_bucket[1m])))`** (선택)
- 비용: 쿼리가 길고 PromQL 학습 곡선. `le` 라벨을 sum 그룹에 포함시키지 않으면 결과 깨짐 (단골 실수)
- 보상: bucket 기반 정확한 분위수. 멀티 인스턴스에서도 정합

### Top N URI 표기 — uri별 전체 시계열 vs Top 5

**Option A — uri별 전체 표시** (legend에 모든 endpoint)
- 거부 이유: endpoint가 30개+ 늘면 legend 폭주, 색상 구분 불가. 카디널리티 폭발 사고 시 대시보드가 먼저 깨짐

**Option B — `topk(5, ...)` 적용** (선택)
- 비용: 6번째 이하 endpoint는 보이지 않음 → 평균에서 묻힘 가능
- 보상: 최악 5개 endpoint를 우선 노출 → 운영 우선순위와 일치. 카디널리티 사고 시에도 패널은 살아 있음

### JVM 대시보드 — 공식 ID 4701 import vs 직접 작성

**Option A — Grafana 공식 ID 4701 그대로 import**
- 거부 이유: 패널 50개+ 의 거대 대시보드 — 본 Product 4섹션 통합 원칙과 충돌

**Option B — 4701에서 핵심 3패널만 발췌 + thirdtool 라벨 적용** (선택)
- 비용: 4701의 일부 메트릭 이름이 Spring Boot 3.x Micrometer와 다를 수 있음 — 검증 필요
- 보상: 검증된 PromQL 표현식 재사용 + 화면 점유 최소화

### Anonymous 접근 — 대시보드 공개 vs 인증 필수

**Option A — `GF_AUTH_ANONYMOUS_ENABLED: true` + Viewer role**
- 거부 이유: 대시보드는 내부 운영 상태(URI, 에러율, DB 풀)를 노출 — 외부 공개 시 정찰 도구화. 1인 운영 단계에선 자가 가입도 불필요

**Option B — 인증 필수 + admin 단일 계정** (선택)
- 비용: 운영자 추가 시 계정 발급 절차 필요
- 보상: 사용자 0/1명 단계의 최소 표면

---

## Story 3-1. Grafana 대시보드 4개 섹션 구성 · JSON 커밋 · provisioning 자동 로드

### User Story

> As a 백엔드 개발자,
I want Grafana 대시보드에서 API 성능 · JVM · DB 커넥션 풀 · 상태 요약을 한 화면에서 보고, 임계치 색상으로 즉시 이상을 감지하길,
So that 부하 테스트 / 운영 중 상태를 5초 안에 판단할 수 있고, 대시보드가 코드베이스에 보존되어 신규 환경에서도 동일하게 재현된다.
>

### 설계 노트

- 대시보드 섹션 구성

  **[1. 상태 요약]** (Stat 패널 4개, 한 줄)

    - 현재 P95 (전체 API)
    - 현재 에러율 (5xx 비율)
    - 활성 스레드 수
    - DB 커넥션 풀 사용률 (%)
    - 각 Stat에 threshold 색상 설정

  **[2. API 성능]** (Time series 패널 3개)

    - 응답 시간 P50/P95/P99 (uri별 분리, top 5)

        ```
        histogram_quantile(0.95,
          sum by (le, uri) (rate(http_server_requests_seconds_bucket{application="thirdtool"}[1m]))
        )
        ```

    - 요청 수 RPS (status code별 stacked)

        ```
        sum by (status) (rate(http_server_requests_seconds_count{application="thirdtool"}[1m]))
        ```

    - 에러율

        ```
        sum(rate(http_server_requests_seconds_count{application="thirdtool", status=~"5.."}[1m]))
        /
        sum(rate(http_server_requests_seconds_count{application="thirdtool"}[1m]))
        ```


    **[3. JVM]** (Time series 패널 3개)
    
    - 힙 메모리 사용량 (used / max)
        
        ```
        jvm_memory_used_bytes{application="thirdtool", area="heap"} / jvm_memory_max_bytes{application="thirdtool", area="heap"}
        ```
        
    - 활성 스레드 수
        - `jvm_threads_live_threads{application="thirdtool"}`
    - GC pause time
        - `rate(jvm_gc_pause_seconds_sum{application="thirdtool"}[1m])`
    
    **[4. DB 커넥션 풀]** (Time series 패널 3개)
    
    - 활성 / 유휴 커넥션
        - `hikaricp_connections_active{application="thirdtool"}`
        - `hikaricp_connections_idle{application="thirdtool"}`
    - 커넥션 풀 사용률 (%)
        
        ```
        hikaricp_connections_active{application="thirdtool"} / hikaricp_connections_max{application="thirdtool"} * 100
        ```
        
    - 커넥션 대기 시간 P95
        
        ```
        histogram_quantile(0.95, rate(hikaricp_connections_acquire_seconds_bucket{application="thirdtool"}[1m]))
        ```

- 대시보드 export 후 `monitoring/grafana/dashboards/thirdtool.json`에 커밋
- `dashboard.yml` provisioning이 `/var/lib/grafana/dashboards` 경로를 스캔하여 자동 로드

### 완료 기준 (Acceptance Criteria)

- [ ]  Grafana 대시보드 진입 시 4개 섹션이 한 화면에 표시된다
- [ ]  상태 요약 섹션의 Stat 패널 4개가 threshold 색상으로 표시된다
- [ ]  API P95 패널이 uri별로 분리되어 top 5 까지 표시된다
- [ ]  에러율 패널이 임계치(1%) 초과 시 빨간색으로 전환된다
- [ ]  JVM 힙 사용량 / GC pause / 활성 스레드 패널이 정상 표시된다
- [ ]  DB 커넥션 풀 사용률이 percent 단위(%)로 표시된다
- [ ]  대시보드 JSON이 코드베이스에 커밋되어 docker compose up 시 자동 로드된다

### 엣지 케이스

- 메트릭이 아직 누적되지 않은 초기 상태 (앱 미실행 1분 이내) → 패널이 "No data"로 정상 렌더링, 에러 표시 없음
- `http_server_requests_seconds_bucket`이 누락된 상태 (Epic 1의 `percentiles-histogram: true` 누락 시) → P95 패널이 빈 상태가 되며 Story 1-1로 회귀
- uri 라벨 카디널리티가 50개 초과 → top 5 limit가 적용되어 패널이 폭주하지 않음
- Grafana auto-refresh 10초가 Prometheus 스크랩 인터벌 10초와 동일 → 화면이 매 새로고침마다 1개 데이터 포인트씩 추가됨

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  대시보드 전체 스크린샷 첨부
- [ ]  4개 섹션 클로즈업 스크린샷 첨부 (각각)
- [ ]  threshold 색상 동작 스크린샷 첨부 (정상 상태 + 임계 초과 상태)
- [ ]  대시보드 JSON 코드베이스 커밋 확인
- [ ]  `docker compose down && up` 후 대시보드 자동 로드 확인
- [ ]  스테이징 배포 확인

### 의존성

- 선행: Story 2-1 (Prometheus + Grafana 스택이 살아 있어야 함)
- 후속: 없음 (Product 1 마지막 Story, Product 2의 baseline 측정 입력으로 활용)

### 스토리 포인트

- 추정: 5 SP

---

## Product 1 요약