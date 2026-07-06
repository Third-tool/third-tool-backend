# [Product] 성능 baseline 측정 및 병목 식별 — k6 부하 테스트

## Product Vision

> ThirdTool의 주요 API 시나리오에 k6로 점진적 부하를 가하고, Product 1의 Grafana 대시보드를 동시 관찰하며 기준 TPS · P95 응답 시간을 정량 수치로 확보한다.
측정 결과를 baseline 문서로 코드베이스에 보존하고, 병목 API · 원인 가설 · 개선 액션 아이템을 분석 문서로 정리한다.
후속 튜닝 작업(쿼리·캐시·풀 사이즈)이 "감"이 아닌 before/after 비교 가능한 수치 위에서 진행될 수 있게 한다.
>

## 배경 및 문제

- 현재 상황 (As-Is)
    - Product 0(로그)과 Product 1(메트릭)로 관측성 두 레이어가 깔려 있지만, "실제 부하를 가했을 때 무슨 일이 일어나는지"는 한 번도 측정된 적이 없다
    - P95 응답 시간 · TPS · 동시 사용자 한계가 모두 미지수 — VU 10명에서 깨질지 100명에서 깨질지 알 수 없음
    - ReviewSession 시작 / 카드 목록 조회가 직관적으로 병목 후보지만 검증된 적이 없음
    - HikariCP 기본값(`maximum-pool-size: 10`)이 적절한지, EC2 인스턴스 사이즈가 충분한지 결정 근거가 없다
    - 부하 테스트 도구가 코드베이스에 없어, 신규 기능 배포 후 회귀 검증을 매번 새로 세팅해야 함
- 발생하는 문제
    - 캐시 / 쿼리 튜닝의 효과를 "체감" 외에는 검증할 수 없다 — 면접에서 "TPS 160 → 1190" 같은 정량 답변 불가능
    - 운영에 풀린 후 트래픽 증가 시점에 어디서 먼저 깨질지 예측 불가 → 사고 대응이 사후 대증 요법으로만 가능
    - 메트릭과 로그가 깔려 있는데 "정상 범위 = 무엇인가"의 baseline이 없어, Product 1 대시보드의 threshold(P95 500ms, 에러율 1%)가 임의 추정치에 머무름
    - 채용 포트폴리오에서 "성능 개선했다"는 주장이 근거 없는 자기 평가로 보임 — 측정 → 분석 → 개선의 사이클이 끊김
- 왜 지금 해결해야 하는가
    - Product 1로 메트릭 수집기가 살아 있을 때 측정해야 의미가 있다 — 부하만 가하고 내부 상태를 못 보면 표면적 검증에 그침
    - baseline은 첫 측정 시점이 가장 깨끗하다 — 코드가 더 복잡해진 후에 측정하면 어느 변경이 영향을 줬는지 추적 비용이 커짐
    - Product 1 threshold 임계치를 baseline 수치로 캘리브레이션해야 v2 알림(Alertmanager)이 의미를 가짐
    - 면접에서 "TPS는 얼마였나, 병목은 어디였나, 어떻게 해결했나"는 백엔드 단골 3단 질문

## 목표 (To-Be)

- `load-test/` 디렉토리에 k6 스크립트 구조가 정착하고, smoke test 1개 + 부하 시나리오 4개가 코드베이스에 커밋된다
- 주요 4개 API의 baseline 수치(TPS · P50 · P95 · 에러율)가 `load-test/results/baseline.md`에 기록된다
- 부하 테스트 중 Grafana에서 P95 추이 · DB 커넥션 풀 사용률 · 활성 스레드 수의 변화가 관찰되고 캡처된다
- 병목 API가 식별되고, 원인 가설과 개선 액션 아이템이 `load-test/results/analysis.md`에 정리된다
- 분석 결과의 액션 아이템이 GitHub Issue로 등록되어 다음 스프린트로 이어진다
- Product 1 대시보드의 threshold 임계치(P95 · 풀 사용률 · 활성 스레드)가 baseline 수치 기반으로 캘리브레이션된다

## 설계 결정 (Design Decision)

> **k6는 Docker로 실행한다. 로컬 설치를 표준으로 잡지 않는다.**
환경 일관성과 CI 통합 경로 확보.
>
> - `docker run --rm -i grafana/k6 run - < script.js` 한 줄로 실행 → 로컬 환경별 버전 차이 없음
> - 향후 GitHub Actions의 CI 단계에서 동일 명령으로 실행 가능 — 자동 부하 테스트 경로 확보 (v2)
> - 단점: 호스트 머신과 컨테이너 간 네트워크 경로 한 단계 추가 → 스테이징 환경 IP / DNS만 정확히 잡으면 영향 없음
> - 이 결정은 ADR로 별도 기록한다 (`ADR-LOAD-001: k6 Execution Environment`)

> **부하 테스트는 스테이징 환경에서만 실행한다. 프로덕션 금지.**
토이 프로젝트라도 데이터·서비스 무결성 보호 원칙은 동일.
>
> - 프로덕션에 부하를 가하면 실제 결제 데이터·유저 데이터 손상 위험 + 다른 유저의 서비스 경험 저해
> - 스테이징은 프로덕션과 동일 구성(DB 사양 · JVM 옵션 · HikariCP 설정)을 유지해야 baseline 의미가 있음
> - 실수로 프로덕션에 부하를 가하지 않도록 k6 스크립트 상단에 `__ENV.TARGET_URL` 강제 + 기본값 미설정 + `localhost`/`staging` 외 URL은 명시적 경고
> - 이 결정은 ADR로 별도 기록한다 (`ADR-LOAD-002: Load Test Environment Restriction`)

> **부하 패턴은 Load Test 1종만. Stress / Spike / Soak는 v2.**
v1은 baseline 확보가 목표.
>
> - **Load Test**: 워밍업 1분 (VU 10) → 목표 부하 3분 (VU 50) → 쿨다운 1분 — 일반 사용자 트래픽 시뮬레이션
> - Stress Test (한계 도달까지 점진 증가) · Spike Test (순간 폭증) · Soak Test (장시간 유지) 는 baseline 확보 후 의미 있음
> - VU 50은 토이 프로젝트의 가상 트래픽 상한 — DAU 5,000명 가정 시 동시 활성 사용자 추정치
> - 이 결정은 ADR로 별도 기록한다 (`ADR-LOAD-003: Load Pattern v1 Scope`)

> **측정 대상은 4개 핵심 API로 제한한다.**
전체 API를 다 측정하지 않는다.
>
> - 선정 4개: 로그인 · 카드 목록 조회 · ReviewSession 시작 · 학습 에너지 설정 저장
> - 선정 기준: (1) 사용 빈도 높음 (2) DB 조인·트랜잭션 복잡도 높음 (3) 도메인 핵심 워크플로
> - 나머지 API는 baseline에서 제외 — 4개 핵심을 깊게 보는 게 전체를 얕게 보는 것보다 가치
> - 이 결정은 ADR로 별도 기록한다 (`ADR-LOAD-004: Target API Selection`)

> **분석 결과는 액션 아이템 GitHub Issue 등록까지 한 사이클로 완성한다.**
"측정만 한 사람"과 "원인 분석까지 한 사람"의 격차.
>
> - 병목 API 식별 → 원인 가설 (Slow Query · N+1 · 풀 포화 · 스레드 포화 중 어느 것인지) → 개선 액션 아이템 → GitHub Issue 등록
> - Issue 1건당 라벨(`performance`)과 baseline 수치 인용 필수 → 다음 스프린트에서 before/after 비교 가능
> - 채용 포트폴리오에서 "TPS 160 → 1190" 같은 서사의 출발점이 이 Issue 트래킹

## 대안 검토 (Alternatives Considered)

> 큰 갈림길마다 "왜 이것이 아니고 저것인가"를 남긴다. 거부된 안에도 합리적 근거가 있었음을 보임으로써 현재 선택의 트레이드오프를 명확히 한다.

### 부하 도구 선택

**Option A — JMeter (GUI 기반 시나리오 작성)**
- 장점: GUI로 시나리오 편집, 풍부한 플러그인 생태계, HTTP 외 프로토콜 지원
- 거부 이유:
    - 시나리오 파일이 XML(`.jmx`) — 코드 리뷰 / git diff 가독성 매우 낮음
    - GUI에 의존하면 CI 통합 비용이 큼 (`-n` non-GUI 모드 별도 학습)
    - 자원 사용량이 무거움 — VU 50 시뮬레이션에도 로컬에서 메모리 1GB+ 소비

**Option B — Gatling (Scala DSL)**
- 장점: 함수형 DSL의 표현력, HTML 리포트가 미려, 동시성 모델이 우수
- 거부 이유:
    - Scala 학습 비용 — 본 프로젝트는 Java/Kotlin이 아닌 Spring Boot Java 단일 스택
    - 1인 개발 환경에서 신규 언어 도입 정당화 어려움

**Option C — Locust (Python)**
- 장점: Python으로 시나리오 작성, 분산 부하 발생 자연스러움
- 거부 이유:
    - 백엔드 스택에 Python이 없음 — 신규 런타임 추가
    - k6 대비 정확도(스레드 기반)가 떨어짐 — 고RPS 측정 시 발생기 자체 병목 우려

**Option D — wrk2 (HTTP 벤치마크 CLI)**
- 장점: 정확한 RPS 모델 (constant throughput), 매우 가벼움
- 거부 이유:
    - 시나리오 표현력 부족 — 로그인 → 토큰 → API 호출 같은 다단계 워크플로 작성 비용 큼
    - 결과 시각화 미흡

**Option E (선택) — k6 (JavaScript 스크립트)**
- 비용: 정밀 RPS 모델은 별도 옵션(`constant-arrival-rate`) 학습 필요, 분산 부하는 k6 Cloud 또는 별도 구성
- 보상: JS 스크립트라 FE 개발자도 읽음. Docker 이미지(`grafana/k6`) 단일 명령 실행. Grafana Labs 생태계와 자연스러운 정합. summary JSON / InfluxDB 출력 옵션 풍부
- 트레이드오프 수용 근거: 1인 개발, Spring Boot 단일 스택, Grafana 대시보드 운용 중 — k6가 모든 결합 비용을 최소화

### 실행 환경

**Option A — 로컬 머신에 k6 직접 설치 (`brew install k6` / `choco install k6`)**
- 거부 이유: OS별 설치 경로 차이 + 버전 휘발성. CI(GitHub Actions Runner) 재현이 보장되지 않음

**Option B — k6 Cloud (SaaS)**
- 장점: 대규모 부하 발생, 결과 자동 시각화
- 거부 이유: 토이 프로젝트에 유료 SaaS 도입 정당화 불가. 데이터가 외부 클라우드로 흘러 데이터 통제력 약화

**Option C — 별도 부하 서버 (EC2)**
- 거부 이유: 발생기 인프라 비용 추가. v1에서는 로컬 컨테이너로 충분 (VU 50 규모)

**Option D (선택) — Docker 로컬 실행 (`docker run --rm grafana/k6:0.53.0`)**
- 비용: 호스트↔컨테이너 네트워크 한 단계 추가. localhost target은 `host.docker.internal` 우회 필요
- 보상: 버전 고정 + CI 동일 명령 재사용 + 로컬 OS 의존성 0. ADR-LOAD-001로 결정 기록

### 테스트 종류 (단계)

**Option A — 모든 종류(smoke / load / stress / spike / soak)를 v1에 포함**
- 거부 이유: 5종 모두 작성 시 시나리오당 패턴이 다르고, 분석 비용이 baseline 확보보다 큼. baseline 없이 stress 결과는 의미가 약함

**Option B (선택) — v1은 smoke + load 2종. stress / spike / soak는 v2**
- 비용: 한계 부하 / 메모리 누수 / 트래픽 급증 시나리오는 v1에서 미검증
- 보상: 분석 1주 내 완료 가능. baseline 확보가 후속 단계의 입력이 되어 측정→분석 사이클이 끊기지 않음. ADR-LOAD-003 기록

### 메트릭 수집 / 결과 저장

**Option A — k6 native summary(stdout JSON) 만 사용**
- 거부 이유: 시계열 데이터 손실. 부하 진행 중 시점별 메트릭 확인 불가

**Option B — k6 → InfluxDB → Grafana 별도 대시보드**
- 장점: 시계열 시각화, 부하 도구 측 상세 메트릭
- 거부 이유: 본 Product의 가치는 **Target 측 메트릭(JVM, HikariCP)**과의 동시 관찰. 부하 발생기 측 시계열은 보너스. 인프라 1개 더 늘리는 비용 정당화 어려움

**Option C — CloudWatch 연동**
- 거부 이유: AWS 종속 + 비용. 로컬 발생기 모델과 충돌

**Option D (선택) — k6 summary(stdout) + Product 1 Grafana 동시 관찰**
- 비용: 시점별 k6 메트릭은 캡처 시각 기준 수동 매칭 필요
- 보상: 추가 인프라 0. 측정 직후 Grafana 캡처가 분석 근거가 됨. v2에서 InfluxDB 추가 시 동일 스크립트로 이행 가능 (k6 옵션 추가만)

### 테스트 데이터 전략

**Option A — 운영 DB snapshot 복제 → 스테이징 시딩**
- 장점: 실제 분포 반영
- 거부 이유: (1) 현재 사용자 0명이라 분포 자체가 없음 (2) 개인정보 익명화 비용 (3) Flyway 정합성 깨짐

**Option B — Faker 등으로 synthetic 대량 생성 스크립트**
- 거부 이유: v1 baseline 확보에 과대. 카드 100장 / 덱 5개 시드면 4개 API 측정에 충분

**Option C (선택) — 수동 시드 데이터 (카드 100장 / 덱 5개 / 유저 1명)**
- 비용: 스테이징 데이터 규모가 운영 예상 대비 작음 — N+1 같은 규모 의존 병목은 과소 추정 가능
- 보상: 재현 가능. `baseline.md`에 데이터 규모를 명시해 결과 해석 시 보정 가능

### 시나리오 모델링 (VU vs RPS, open vs closed)

**Option A — RPS 기반(constant-arrival-rate, open model)**
- 장점: 응답 지연이 발생해도 도착률 일정 유지 → 실제 트래픽 모델에 가까움
- 거부 이유: baseline 단계에서는 시스템 한계를 점진 측정하기보다 "동시 사용자 N명일 때 어떻게 되나"가 의도. open model은 stress 단계에 적합

**Option B (선택) — VU 기반 (ramping-vus, closed model)**
- 비용: 시스템 응답이 느려지면 도착률도 함께 떨어짐 — RPS는 종속 변수
- 보상: 동시 사용자 시뮬레이션의 직관과 일치. baseline 확보 후 v2에서 RPS 모델로 교차 검증 가능

### 베이스라인 환경 동결

**Option A — 매 측정 시 동적 환경 (인스턴스 가변)**
- 거부 이유: 환경이 변하면 baseline의 의미 소실. 비교 불가능

**Option B (선택) — 측정 환경을 `baseline.md`에 고정 명시 (인스턴스 스펙, JVM 옵션, HikariCP, 시드 데이터)**
- 비용: 환경 변경 시 baseline 재측정 필요
- 보상: before/after 튜닝 비교의 정합성 확보. ADR-LOAD-002와 결합해 "스테이징 환경 = baseline 환경" 원칙 확립

### 회귀 감지 / CI 통합

**Option A — 매 PR마다 자동 부하 테스트 + 임계치 초과 시 머지 차단**
- 거부 이유: 5분짜리 부하 테스트가 PR마다 돌면 CI 비용 폭증. 1인 개발에서 머지 차단의 운영 부담 큼

**Option B (선택) — v1은 수동 실행. v2에서 nightly 또는 release-gate로 자동화 검토**
- 비용: 회귀 감지가 사람 trigger 의존
- 보상: 스크립트 자산이 코드베이스에 살아 있어 v2 자동화 진입 비용 낮음. `docker run` 단일 명령 = GitHub Actions step 1줄

## 전체 아키텍처 (High-Level Architecture)

> 부하 발생기 / Target / 메트릭 수집의 3축. 본문 5페이지보다 다이어그램 1장이 더 강하다.

### 컴포넌트 배치

```
┌─────────────────────────────────────────────────────────────────┐
│  개발자 로컬 머신 (또는 향후 GitHub Actions Runner)             │
│                                                                 │
│  ┌──────────────────────────────────────┐                       │
│  │  Docker: grafana/k6:0.53.0           │                       │
│  │  ├─ smoke/auth.js                    │                       │
│  │  ├─ scenarios/login.js               │                       │
│  │  ├─ scenarios/cards.js               │                       │
│  │  ├─ scenarios/review.js              │                       │
│  │  ├─ scenarios/schedule.js            │                       │
│  │  └─ utils/{auth,options}.js          │                       │
│  │                                      │                       │
│  │  __ENV.TARGET_URL = staging URL only │                       │
│  │  (ADR-LOAD-002 가드)                 │                       │
│  └──────────────────────┬───────────────┘                       │
│                         │ summary JSON                          │
│                         ▼                                       │
│  ┌──────────────────────────────────────┐                       │
│  │  stdout + load-test/results/         │                       │
│  │  ├─ baseline.md                      │                       │
│  │  ├─ analysis.md                      │                       │
│  │  └─ screenshots/*.png (Grafana 캡처) │                       │
│  └──────────────────────────────────────┘                       │
└────────────────────────────┬────────────────────────────────────┘
                             │  HTTPS / 가상 사용자 VU 10→50
                             │  (closed model, ramping-vus)
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  Staging Environment (prod 설정 미러)                           │
│                                                                 │
│  ┌──────────────────────────────────────┐                       │
│  │  Spring Boot (prod profile)          │                       │
│  │  ├─ Controllers (CardController …)   │                       │
│  │  ├─ HikariCP (max-pool-size=10)      │                       │
│  │  ├─ JVM (-Xmx512m)                   │                       │
│  │  └─ Micrometer → Prometheus scrape   │  ◄─── Product 1       │
│  └──────────────────────┬───────────────┘                       │
│                         │                                       │
│              ┌──────────┴──────────┐                            │
│              ▼                     ▼                            │
│  ┌──────────────────┐   ┌──────────────────┐                    │
│  │  MySQL (RDS)     │   │  Logback         │                    │
│  │  refresh_entity, │   │  + traceId       │  ◄─── Product 0    │
│  │  card, deck …    │   │  → CloudWatch    │                    │
│  └──────────────────┘   └──────────────────┘                    │
└─────────────────────────────────────────────────────────────────┘
                             │
                             │ Prometheus scrape (15s)
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  Product 1 Grafana 대시보드 (동시 관찰)                         │
│  ├─ API P50/P95/P99 패널                                        │
│  ├─ hikaricp_connections_active                                 │
│  ├─ jvm_threads_live_threads                                    │
│  └─ http_server_requests_seconds_count                          │
└─────────────────────────────────────────────────────────────────┘
```

### 핵심 플로우

**1. baseline 측정 1회 (단일 시나리오)**
```
개발자 ─docker run─► k6 컨테이너 ─setup()─► POST /login → token
                                            │
                                            ▼
                            ramping-vus (1m@10 → 3m@50 → 1m@0)
                                            │
                            VU별 iteration: GET /api/v1/decks/1/cards
                                            │  + sleep(1) thinkTime
                                            ▼
                            check(status === 200) + 메트릭 자동 수집
                                            │
        ┌───────────────────────────────────┼─────────────────────────┐
        ▼                                   ▼                         ▼
   k6 summary                       Staging Spring Boot       Grafana 동시 관찰
   (TPS / P95 / errors)             (HikariCP / JVM threads)  (개발자 캡처 3장)
```

**2. 분석 → 가설 → Issue (Epic 3)**
```
baseline.md ─► 위반 API 식별 ─► Grafana 캡처 + traceId 로그 ─► 가설 분류
                                                             ├─ Slow Query / N+1
                                                             ├─ DB 풀 포화
                                                             ├─ 스레드 풀 포화
                                                             └─ 외부 의존성
                                                                  │
                                                                  ▼
                                                    analysis.md + GitHub Issue
                                                    + Grafana threshold PR
```

### Out-of-Process 의존

- **Staging Spring Boot**: prod 프로파일 미러 (HikariCP 10, JVM 옵션 동결)
- **MySQL (RDS)**: 시드 데이터 카드 100장 / 덱 5개 / 유저 1명
- **Prometheus / Grafana**: Product 1의 메트릭 파이프라인 — 동시 관찰의 입력
- **CloudWatch Logs**: Product 0의 traceId 검색 — 가설 검증의 입력
- **Docker Daemon**: 로컬 발생기 런타임. v2에서 GitHub Actions Runner로 이전 가능

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 부하 테스트 자체의 실패 시나리오

| 시나리오 | 증상 | 검출 / 대응 |
| --- | --- | --- |
| k6 컨테이너 OOM | iteration 중간에 exit code 137 + Docker `OOMKilled` 플래그 | Docker `--memory` 옵션 점검. VU 50 기준 256MB 권장. 초과 시 발생기 분리(v2) |
| 호스트↔컨테이너 네트워크 throttling | k6 측 dial timeout 폭증, Target은 정상 | `--network=host` 또는 `host.docker.internal` 검증. 발생기와 Target 간 RTT 측정 |
| Target 다운 (스테이징 OOM / DB 단절) | k6 5xx 비율 급등, Grafana 메트릭 단절 | 측정 즉시 중단. `analysis.md`에 "측정 중단 — Target 다운"으로 기록 후 원인 추적 |
| 토큰 만료 (setup 토큰이 측정 중 만료) | 401 비율이 측정 후반부에 점진 상승 | AT TTL(30분) > 측정 시간(5분) 검증. 토큰 만료 401은 에러율 집계에서 제외 |
| 테스트 데이터 부족 (시드 카드 100장 < VU 50 동시) | 동일 row 잠금 경합 폭증, P95 비정상 상승 | 시드 데이터 규모 재검토. `baseline.md`에 데이터 규모 명시로 해석 보정 |
| 결과 손실 (Docker 종료 시 컨테이너 산출물 소실) | `docker run --rm` 후 summary 미보존 | stdout 리다이렉트 (`| tee load-test/results/run-YYYYMMDD.log`) 또는 `-out json=...` |
| Grafana 캡처 누락 | 분석 시 시점별 메트릭 단서 부재 | 측정 전 캡처 스크립트 또는 수동 트리거 체크리스트. Prometheus 보존 기간(15일) 내 재측정 |
| 우연히 production URL 박힘 | 운영 데이터 손상 | ADR-LOAD-002 가드 — `__ENV.TARGET_URL` 강제 + `localhost`/`staging` 외 도메인 경고 |
| 측정 중 다른 사용자가 스테이징 사용 | 백그라운드 트래픽으로 baseline 오염 | 평일 야간 / 주말 권장. `baseline.md`에 측정 시각 기록 |

### 운영 관측 지표 (측정 중 동시 수집)

**부하 발생기 측 (k6 summary)**
- `http_req_duration` — P50 / P95 / P99 (필수 3종)
- `http_reqs` — 총 요청 수 + RPS
- `http_req_failed` — 에러율 (rate)
- `vus` / `vus_max` — 실제 활성 VU
- `iteration_duration` — 가상 사용자 1 사이클 시간 (thinkTime 포함)
- `data_received` / `data_sent` — 대역폭

**Target 측 (Product 1 Grafana 동시 관찰)**
- `http_server_requests_seconds{quantile="0.5|0.95|0.99"}` — Target 측 latency (k6 측정과 교차 검증)
- `hikaricp_connections_active` / `hikaricp_connections_pending` — DB 풀 포화 여부
- `hikaricp_connections_acquire_seconds` — 커넥션 획득 대기
- `jvm_threads_live_threads` / `tomcat_threads_busy` — 스레드 포화 여부
- `jvm_memory_used_bytes` — 메모리 누수 / GC 부하 (단발 측정에서는 약한 신호, soak 단계 v2)
- `process_cpu_usage` — CPU 포화 여부
- `mysql_global_status_threads_running` (가능 시) — DB 측 부하

**캡처 시점 (시나리오당 3장 권장)**
- 워밍업 → 목표 부하 전환 시점 (90초 부근)
- 목표 부하 진행 중 (3분 중간)
- 쿨다운 진입 시점 (4분 30초 부근)

### 로깅 정책 (부하 중)

- Target 측 INFO 레벨은 baseline 측정 동안 유지 (traceId가 분석 입력)
- Target 측 DEBUG는 비활성 — 부하 중 로그 IO가 측정 자체를 왜곡할 수 있음
- k6 측 verbose 로그(`--verbose`)는 디버깅 시에만. 정식 측정에서는 summary만

## 롤아웃 / 마이그레이션 (Rollout)

### 부하 테스트 단계 확장 로드맵

```
[v1 — 본 Product 범위]
  ① Smoke Test  (VU 1, 30초)
       │ "스테이징이 부하를 받을 준비가 됐나"
       ▼
  ② Load Test   (VU 10→50, 5분)
       │ "예상 동시 사용자에서 P95 / 에러율은?"
       ▼
  ③ baseline.md / analysis.md / Issue / threshold 캘리브레이션

[v2 — 후속 Product 또는 스프린트]
  ④ Stress Test (VU 50→200, 한계 도달까지 ramp-up)
       │ "어느 시점에 무엇이 먼저 깨지나"
       ▼
  ⑤ Spike Test  (VU 0→200 순간, 1분 유지, 0으로 복귀)
       │ "급증 트래픽에 회복 시간은?"
       ▼
  ⑥ Soak Test   (VU 30, 6시간 이상)
       │ "메모리 누수 / 커넥션 leak / 스케줄러 충돌?"
       ▼
  ⑦ CI 통합 / nightly 자동 회귀
```

### 운영 환경 실행 시 안전 가이드 (영구 제외 원칙이지만 가이드는 유지)

- **본 Product 범위에서는 프로덕션 부하 금지** — ADR-LOAD-002 원칙. 아래는 향후 정책 변경 시를 위한 가이드.
- 만약 미래에 프로덕션 측정이 정당화될 경우:
    - 별도 부하 격리 환경(canary 클러스터) 필수
    - 측정 시간대는 트래픽 최저 시점 (예: 새벽 4-5시)
    - rate limiter / circuit breaker가 활성 상태인지 사전 검증
    - 측정 1회당 사전 공지 + 사후 보고 — 책임 추적 가능하도록
    - 실패 시 즉시 중단 가능한 kill switch (k6는 `Ctrl+C` 즉시 종료)

### 환경별 차이

| 항목 | local (dev) | staging (prod 미러) | production |
| --- | --- | --- | --- |
| 부하 테스트 허용 | smoke만 | smoke + load (본 Product) | 금지 (ADR-LOAD-002) |
| DB | H2 in-memory | MySQL (RDS) | MySQL (RDS) |
| HikariCP max-pool | n/a | 10 | 10 |
| JVM 옵션 | 기본값 | -Xmx512m | -Xmx512m |
| 발생기 위치 | 동일 머신 | 동일 머신(Docker) | n/a |
| 시드 데이터 | 자유 | 카드 100/덱 5/유저 1 | 실데이터 |
| `__ENV.TARGET_URL` 가드 | localhost 허용 | staging 도메인만 | 거부 |

### 회귀 측정 트리거 (v1 수동)

- **신규 기능 배포 직전** — 4개 시나리오 전체 재실행, baseline.md `이전`/`이번` 컬럼 추가
- **인프라 변경 시** (인스턴스 사이즈 / DB 사양 / HikariCP 변경) — baseline.md 환경 블록 갱신 후 재측정
- **튜닝 작업 PR 머지 후** — 해당 시나리오만 재실행, before/after 표 PR 본문에 첨부

## 성공 지표 (KPI)

| 지표 | 현재 값 | 목표 값 | 측정 방법 |
| --- | --- | --- | --- |
| k6 스크립트 코드베이스 포함 | 0 | smoke 1 + 시나리오 4 | `load-test/` 디렉토리 |
| baseline 측정 완료 API 수 | 0 | 4개 (로그인 · 카드 · 리뷰 · 스케줄) | `baseline.md` 표 |
| baseline 문서 커밋 여부 | — | 커밋됨 | `load-test/results/baseline.md` |
| 부하 테스트 중 Grafana 캡처 | 0 | 4건 이상 (시나리오별) | `analysis.md` 첨부 |
| 병목 API 식별 수 | — | 1개 이상 또는 "병목 없음" 명시 | `analysis.md` |
| GitHub Issue 등록 (`performance` 라벨) | 0 | 액션 아이템 건수만큼 | GitHub Issues |
| Product 1 threshold 캘리브레이션 | 임의 추정치 | baseline 기반 재설정 | `thirdtool.json` 변경 PR |
| 스테이징 외 URL 사용 차단 | — | 0건 (스크립트 가드 통과) | `__ENV.TARGET_URL` 검증 |

## Scope

- **In Scope**
    - `load-test/` 디렉토리 구조 정의 (smoke · scenarios · utils · results)
    - k6 환경: Docker 실행 표준 + 공통 옵션 (thresholds · stages) 모듈
    - smoke test 1종 (로그인 + 카드 조회 health check)
    - Load Test 시나리오 4종 (로그인 · 카드 목록 · ReviewSession 시작 · 스케줄 저장)
    - baseline 수치 측정 및 `baseline.md` 작성
    - 부하 테스트 중 Grafana 대시보드 동시 관찰 및 캡처
    - 병목 분석 및 `analysis.md` 작성 (원인 가설 + 액션 아이템)
    - GitHub Issue 등록 (`performance` 라벨 + baseline 인용)
    - Product 1 threshold 캘리브레이션 (대시보드 JSON 업데이트)
- **Out of Scope**
    - Stress Test · Spike Test · Soak Test (v2)
    - 실제 튜닝 작업 적용 (캐시 도입 · 쿼리 수정 · 풀 사이즈 조정) — 별도 Product 또는 후속 스프린트
    - CI 자동 부하 테스트 (PR 머지 시 자동 실행) — v2
    - 프로덕션 환경 측정 (보안·데이터 무결성 이유로 영구 제외)
    - APM 도구 도입 (DataDog · NewRelic 등 — v2)
    - 가상 사용자 시나리오 다양화 (지역별 분포 · 디바이스별 분포 등 — 도메인 요구사항 발생 시점)
    - WebSocket / SSE / 파일 업로드 등 비-HTTP API (현재 도메인에 없음)

## 대상 사용자

- 주요 사용자: ThirdTool 백엔드 개발자 (1인 운영)
- 사용 맥락:
    - 신규 기능 배포 전 → 기존 시나리오로 회귀 테스트 실행하여 P95 회귀 여부 확인
    - 쿼리 / 캐시 / 풀 사이즈 튜닝 후 → before/after 비교를 위한 재측정
    - 채용 포트폴리오 작성 시 → baseline 표 + analysis 문서를 정량 근거로 인용
    - 면접 답변 시 → "VU 50명, P95 850ms에서 N+1 쿼리 발견" 같은 구체 수치 + 원인 가설 진술

## 연결된 Epic 목록

- [ ]  Epic 1. k6 환경 구성 및 smoke test — Docker 실행 표준 · `load-test/` 구조 · smoke test 1종
- [ ]  Epic 2. 주요 4개 API 부하 시나리오 작성 및 baseline 측정 — Load Test 시나리오 · `baseline.md` 기록 · Grafana 캡처
- [ ]  Epic 3. 병목 분석 및 액션 아이템 등록 — `analysis.md` · GitHub Issue 등록 · Product 1 threshold 캘리브레이션

## 관련 문서

- 상위 문서: ThirdTool 도메인 모델링 · 백엔드 컨벤션
- 선행 Product:
    - Product 0 (로그 관리 기반 구축) — 부하 중 traceId 기반 원인 추적
    - Product 1 (모니터링 기반 구축) — Grafana 대시보드 동시 관찰의 입력
- 후속 작업:
    - 성능 튜닝 Product (별도 Product 또는 후속 스프린트) — `analysis.md`의 액션 아이템이 입력
    - v2 자동 부하 테스트 (CI 통합) — 이 Product의 스크립트가 자산
- 참고 자료: k6 공식 문서 · Grafana k6 Cloud 시나리오 가이드
- ADR 후보: `ADR-LOAD-001` Execution Environment · `ADR-LOAD-002` Environment Restriction · `ADR-LOAD-003` Load Pattern v1 Scope · `ADR-LOAD-004` Target API Selection

## 열린 질문 (Open Questions)

> v1 완료 후 / v2 진입 시점에 결정해야 할 사항. 본 Product의 범위는 아니지만 후속 작업의 입력.

- **CI 통합 시점**: 본 Product 완료 후 GitHub Actions에 nightly 부하 테스트를 도입할지, 아니면 release-gate(태그 푸시 시점)로만 돌릴지. Runner의 OS 자원 한계로 VU 50을 안정 발생할 수 있는지 사전 검증 필요.
- **임계치 초과 시 자동 차단 정책**: k6 thresholds 실패 시 GitHub Actions 워크플로를 fail로 만들어 PR 머지를 차단할지, 아니면 경고만 띄울지. 1인 개발 환경에서 차단의 운영 부담 vs 회귀 방치 위험의 트레이드오프.
- **Chaos engineering 도입**: 부하 테스트와 결합해 DB 단절 / 네트워크 지연 주입(Toxiproxy, Chaos Mesh)을 v3 단계에서 시도할 가치가 있는가. 토이 프로젝트 규모에서 ROI가 정당화되는가.
- **InfluxDB / xk6-output-prometheus-remote 도입 시점**: k6 시점별 메트릭을 Grafana에서 직접 보고 싶을 때. v1 4개 시나리오 분석 후 "시점 매칭이 정말 필요한가" 답이 나옴.
- **부하 발생기 격리**: 측정 정확도 향상이 필요해지면 별도 EC2 / GitHub Hosted Runner / k6 Cloud 중 어디로 이전할지. 비용 대비 효과 평가가 v2 진입 조건.
- **테스트 데이터 다양화**: 시드 카드 100장이 작은 규모의 측정만 가능하게 함. Faker 기반 대량 데이터(10,000+) 시드 스크립트가 별도 산출물이 될지, 또는 운영 데이터의 익명화 snapshot 전략으로 옮길지.
- **APM 도입 시점**: Datadog / New Relic 같은 APM은 분산 트레이스로 가설 검증 비용을 크게 낮춤. 토이 단계에서 무료 티어로 검증 → 운영 진입 후 유료 전환의 단계적 도입 가능성.
- **SLO 정의의 정합성**: baseline 캘리브레이션 후 Product 1 threshold가 baseline 기반으로 재설정되지만, 비즈니스 SLO(예: "사용자가 카드 목록을 200ms 이내에 봐야 한다")가 정의되어 있지 않음. SLO 정의 자체가 별도 Product가 될 수 있음.
- **분산 부하 발생 (k6 cluster mode)**: VU 200+ 단계에서 단일 발생기가 한계에 도달할 때, k6 distributed execution / k6 Cloud / Locust master-worker 중 어디로 이행할지.
- **결과 영구 보존 정책**: `load-test/results/` 디렉토리를 git에 영구 커밋할지(저장소 비대화 우려), 별도 storage(S3) + 인덱스 파일만 git에 둘지.

---

# [Epic 1] k6 환경 구성 및 smoke test — Docker 실행 표준 · 디렉토리 구조 · smoke test 1종

## Epic 목표

> `load-test/` 디렉토리 구조를 코드베이스에 정착시키고, k6를 Docker 컨테이너로 실행하는 표준 명령을 README에 명시한다.
smoke test 1종(VU 1 / 30초)을 작성해 스테이징 환경 health check가 자동화된 상태를 만든다.
공통 thresholds · stages · auth 토큰 발급 유틸을 모듈화해 후속 Epic의 시나리오 작성 비용을 낮춘다.
>

## 배경

- Epic 2의 부하 시나리오 작성 전 환경·구조가 먼저 잡혀 있어야 시나리오 작성에 집중 가능 — 일직선 의존성
- smoke test가 먼저 동작해야 "스테이징 환경이 부하 테스트를 받을 준비가 되었는지" 검증 가능
- 공통 유틸(로그인 토큰 발급)을 모듈화하지 않으면 4개 시나리오에 동일 코드가 복붙되어 유지보수 비용 증가

## 핵심 설계 결정

> **k6 스크립트는 ES6 모듈로 분리해 재사용성을 높인다.**
단일 파일 형태로 작성하지 않는다.
>
> - `utils/auth.js`: 로그인 → 토큰 발급 공통 함수
> - `utils/options.js`: 공통 thresholds · stages 옵션 (시나리오별 import)
> - `smoke/*.js`: smoke test 진입점
> - `scenarios/*.js`: Load Test 시나리오 진입점 (Epic 2)
> - 이유: 4개 시나리오에서 동일 로그인 로직을 복붙하지 않고 import 한 줄로 재사용

> **`__ENV.TARGET_URL` 환경변수 강제 + 기본값 미설정.**
실수로 프로덕션 URL이 박히는 사고 방지.
>
> - 스크립트 진입 시 `if (!__ENV.TARGET_URL) throw new Error('TARGET_URL 환경변수 필수')`
> - README에 스테이징 URL만 예시로 제공 (`e TARGET_URL=https://staging.thirdtool.dev`)
> - 프로덕션 도메인이 우연히 박힐 경로를 사전 차단

## 완료 기준 (Definition of Done)

- [ ]  `load-test/` 디렉토리 구조가 코드베이스에 커밋된다
- [ ]  `docker run --rm -i -e TARGET_URL=... grafana/k6 run - < load-test/smoke/auth.js`가 오류 없이 실행된다
- [ ]  smoke test 실행 결과 summary가 터미널에 출력되고 에러율 0%다
- [ ]  공통 thresholds(`http_req_duration p95 < 500ms`, `http_req_failed rate < 0.01`)가 적용된다
- [ ]  README에 k6 실행 가이드가 추가된다
- [ ]  연결된 Story가 모두 Done 상태다

## 산출물 (Deliverables)

- `load-test/` 디렉토리 (smoke · scenarios · utils · results 하위 구조)
- `load-test/smoke/auth.js`
- `load-test/utils/auth.js`
- `load-test/utils/options.js`
- `load-test/README.md` (실행 가이드)
- 메인 `README.md`의 부하 테스트 섹션 링크

## 연결된 Story 목록

- [ ]  Story 1-1. `load-test/` 디렉토리 구조 + 공통 모듈 + smoke test 1종 작성 및 실행 (3 SP)

## 내부 메모 / 제약 사항

- k6 버전: `grafana/k6:0.53.0` 고정 (재현성 확보)
- smoke test 실행 시간 기준: 30초 (CI에서도 부담 없는 길이)
- 토큰 발급 유틸은 `setup()` 단계에서 1회만 호출하여 VU별 중복 로그인 방지
- 연기 항목: GitHub Actions CI 통합 — v2

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

### k6 버전 고정 전략

- **A — `grafana/k6:latest` 사용**: 거부. 같은 명령이 시점에 따라 다른 결과 — 재현성 손상
- **B (선택) — `grafana/k6:0.53.0` 명시 고정**: 명령 / README / 향후 CI step에 동일 태그 박힘. 0.5x → 0.6 메이저 변경 시 의식적 업그레이드 + baseline 재측정 트리거
- **C — `:0.53`(minor만 고정)**: 패치 자동 반영의 이득 < 패치 자체에서 메트릭 산정 변경의 리스크. 거부

### 디렉토리 레이아웃 (utils / smoke / scenarios / results)

- **A — 단일 파일(`load-test/k6.js`)에 모든 시나리오를 옵션으로 분기**: 거부. 시나리오 추가 시 파일 비대 + diff 가독성 최악
- **B — `load-test/{smoke,load,stress,spike,soak}/` 부하 종류별 디렉토리**: 거부. v1은 smoke + load만이라 빈 디렉토리 다수 — 미래 비용 선지급
- **C (선택) — `smoke/` + `scenarios/` + `utils/` + `results/`**: 종류 대신 역할 기준. utils 재사용 / results 산출물 명확 분리. v2 stress 추가는 `scenarios/stress-*.js`로 자연스럽게 확장

### 공통 옵션 모듈 (`utils/options.js`) 설계

- **A — 모든 시나리오가 자체 `options` 객체 직접 정의**: 거부. thresholds 4곳 중복 → 임계치 변경 시 4곳 모두 손수정
- **B (선택) — `baseThresholds` + `smokeStages` / `loadStages` export, 시나리오는 import만**: 임계치 변경 1곳. 시나리오는 본질(API 호출)에 집중
- **C — JSON 파일로 외부화**: ES6 import의 정적 검증 / IDE 자동완성 손실. 거부

### `setup()` vs VU별 로그인 (smoke 단계)

- **A — VU iteration마다 로그인**: 거부. smoke는 health check 목적인데 로그인 latency가 매 호출에 섞임
- **B (선택) — `setup()`에서 1회 로그인 → 토큰 주입**: smoke 본질(인증된 API 응답 확인) 정확히 측정. Epic 2 시나리오 패턴과 일관
- **C — 사전 토큰을 ENV로 주입**: 외부 의존(토큰 발급 시점 사람 손) — 자동화 손상. 거부

### TARGET_URL 가드 위치

- **A — Dockerfile / docker-compose에서 검증**: 거부. 스크립트가 단독 실행될 때 우회 가능
- **B (선택) — 스크립트 진입부에서 `if (!__ENV.TARGET_URL) throw`**: 발사 매체와 무관하게 가드 적용. ADR-LOAD-002 1차 방어선
- **C — production 도메인 패턴 매칭으로 거부**: B의 보완으로 검토 가능 (v2 — 경고 메시지)

---

## [Story 1-1] `load-test/` 디렉토리 구조 + 공통 모듈 + smoke test 1종 작성 및 실행

### User Story

> As a 백엔드 개발자,
I want `load-test/` 디렉토리에 표준 구조와 공통 모듈, smoke test가 준비되길,
So that Epic 2의 부하 시나리오를 즉시 작성·실행할 수 있고, 스테이징 환경의 health check가 한 명령으로 가능해진다.
>

### 설계 노트

- 디렉토리 구조

    ```
    load-test/
    ├── README.md
    ├── smoke/
    │   └── auth.js
    ├── scenarios/        # Epic 2에서 채워짐
    ├── utils/
    │   ├── auth.js
    │   └── options.js
    └── results/          # Epic 2/3에서 채워짐
    ```

- `utils/options.js`

    ```jsx
    export const baseThresholds = {
      http_req_duration: ['p(95)<500'],
      http_req_failed: ['rate<0.01'],
    };
    
    export const smokeStages = [
      { duration: '30s', target: 1 },
    ];
    
    export const loadStages = [  // Epic 2에서 사용
      { duration: '1m', target: 10 },   // 워밍업
      { duration: '3m', target: 50 },   // 목표 부하
      { duration: '1m', target: 0 },    // 쿨다운
    ];
    ```

- `utils/auth.js`

    ```jsx
    import http from 'k6/http';
    import { check } from 'k6';
    
    export function login(baseUrl, email, password) {
      const res = http.post(`${baseUrl}/login`,
        JSON.stringify({ email, password }),
        { headers: { 'Content-Type': 'application/json' } }
      );
      check(res, { 'login 200': (r) => r.status === 200 });
      return res.json('accessToken');
    }
    ```

- `smoke/auth.js`

    ```jsx
    import { login } from '../utils/auth.js';
    import { baseThresholds, smokeStages } from '../utils/options.js';
    import { check, sleep } from 'k6';
    import http from 'k6/http';
    
    if (!__ENV.TARGET_URL) {
      throw new Error('TARGET_URL 환경변수가 필수입니다. (예: -e TARGET_URL=https://staging.thirdtool.dev)');
    }
    
    export const options = {
      stages: smokeStages,
      thresholds: baseThresholds,
    };
    
    export function setup() {
      const token = login(__ENV.TARGET_URL, __ENV.TEST_EMAIL, __ENV.TEST_PASSWORD);
      return { token };
    }
    
    export default function (data) {
      const res = http.get(`${__ENV.TARGET_URL}/api/v1/users/me`, {
        headers: { Authorization: `Bearer ${data.token}` },
      });
      check(res, { 'me 200': (r) => r.status === 200 });
      sleep(1);
    }
    ```

- 실행 명령 (README에 명시)

    ```bash
    docker run --rm -i \
      -e TARGET_URL=https://staging.thirdtool.dev \
      -e TEST_EMAIL=loadtest@thirdtool.dev \
      -e TEST_PASSWORD=*** \
      grafana/k6:0.53.0 run - < load-test/smoke/auth.js
    ```


### 완료 기준 (Acceptance Criteria)

- [ ]  `load-test/` 디렉토리 구조가 커밋된다 (smoke · scenarios · utils · results · README)
- [ ]  docker 명령으로 smoke test가 실행되고 summary가 출력된다
- [ ]  smoke test 결과 에러율이 0%다
- [ ]  `TARGET_URL` 미설정 시 스크립트가 명확한 에러 메시지로 실행 거부된다
- [ ]  공통 thresholds(`p(95)<500`, `rate<0.01`)가 적용되어 summary에 표시된다
- [ ]  README에 docker 실행 명령이 명시된다

### 엣지 케이스

- 스테이징 서버 미실행 상태에서 smoke test 실행 → k6가 connection error 출력 + non-zero exit code 반환
- 잘못된 로그인 자격증명 → `login 200` check 실패 + summary에 표시
- `TARGET_URL`이 `localhost` 또는 `staging`을 포함하지 않는 도메인 → 경고 메시지 출력 (선택, fail-safe)
- k6 컨테이너에서 호스트 머신의 localhost 접근 → README에 `host.docker.internal` 안내 (Linux는 `-add-host` 옵션)

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  smoke test 실행 결과 summary 스크린샷 첨부
- [ ]  README 실행 가이드 커밋 확인
- [ ]  공통 모듈 import 검증 (`utils/auth.js`, `utils/options.js`)
- [ ]  스테이징 환경에서 smoke test 1회 성공

### 의존성

- 선행: 없음 (Product 2 시작 Story)
- 후속: Story 2-1 (Epic 2의 시나리오가 이 구조 위에서 작성됨)

### 스토리 포인트

- 추정: 3 SP

---

# [Epic 2] 주요 4개 API 부하 시나리오 작성 및 baseline 측정

## Epic 목표

> 4개 핵심 API(로그인 · 카드 목록 조회 · ReviewSession 시작 · 학습 에너지 설정 저장)에 Load Test 패턴(워밍업 1분 VU 10 → 목표 부하 3분 VU 50 → 쿨다운 1분)을 가하고, baseline 수치(TPS · P50 · P95 · 에러율)를 `load-test/results/baseline.md`에 기록한다.
테스트 실행 중 Product 1의 Grafana 대시보드를 동시 관찰하며 스레드 · DB 커넥션 풀 추이를 캡처한다.
>

## 배경

- Epic 1의 환경이 깔린 후 핵심 작업 — 이 Epic의 산출물이 Product 2의 가장 큰 가치
- 4개 API는 도메인 핵심 워크플로를 대표 — 사용 빈도 · 복잡도 기준으로 선정
- baseline은 측정 후 며칠 안에 분석으로 이어져야 의미가 살아남 — 메트릭이 휘발성이라 Prometheus 보존 기간(기본 15일) 내에 분석 완료 필요

## 핵심 설계 결정

> **시나리오별로 별도 k6 실행을 한다. 한 스크립트에서 4개를 동시 실행하지 않는다.**
시나리오 간 간섭을 배제하고 baseline 수치의 깨끗함을 확보.
>
> - 각 시나리오 5분(워밍업 1분 + 목표 3분 + 쿨다운 1분), 총 4번 실행 = 20분
> - 시나리오 간 2분 대기 시간 → 서버 상태 정상화 후 다음 측정
> - 동시 실행은 v2의 복합 시나리오 단계에서 도입

> **각 시나리오의 setup()에서 로그인 1회만 수행한다.**
VU별 매번 로그인하지 않는다.
>
> - 토큰을 setup() 단계에서 발급 후 `default` 함수의 데이터로 주입
> - 이유: 측정 대상 API의 P95에 로그인 latency가 섞이지 않게 함 (로그인 자체는 시나리오 1에서 별도 측정)
> - 토큰 만료 5분 미만일 경우 측정 중 만료 위험 → 토큰 TTL이 측정 시간(5분)보다 충분히 길어야 함 (`accessToken` TTL 30분 가정)

## 완료 기준 (Definition of Done)

- [ ]  4개 시나리오 스크립트가 `load-test/scenarios/`에 커밋된다
- [ ]  스테이징 환경에서 4회 Load Test가 모두 실행되고 summary가 수집된다
- [ ]  `load-test/results/baseline.md`에 4개 API의 TPS · P50 · P95 · 에러율이 표로 기록된다
- [ ]  부하 테스트 중 Grafana 대시보드 캡처가 4건 이상 첨부된다
- [ ]  P95 > 500ms 또는 에러율 > 1%인 API가 식별되어 Epic 3의 입력으로 표시된다
- [ ]  연결된 Story가 모두 Done 상태다

## 산출물 (Deliverables)

- `load-test/scenarios/login.js`
- `load-test/scenarios/cards.js`
- `load-test/scenarios/review.js`
- `load-test/scenarios/schedule.js`
- `load-test/results/baseline.md`
- `load-test/results/screenshots/` (Grafana 캡처 4건 이상)

## 연결된 Story 목록

- [ ]  Story 2-1. 4개 API Load Test 시나리오 작성 (3 SP)
- [ ]  Story 2-2. baseline 측정 실행 · Grafana 동시 관찰 · `baseline.md` 작성 (3 SP)

## 내부 메모 / 제약 사항

- 측정 시간대는 외부 트래픽 영향이 없는 평일 야간 또는 주말 권장 (스테이징도 다른 사용자가 있을 수 있음)
- 측정 전 스테이징 서버 재시작 1회 → JVM warm-up 영향 균질화
- DB 데이터 사전 시딩: 카드 100장 / 덱 5개 / 유저 1명 — `baseline.md`에 데이터 규모 명시
- 토큰 만료로 인한 401 응답은 에러율 집계에서 제외 (별도 처리)

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

### 시나리오 실행 모드 — 순차 vs 동시

- **A — 4개 시나리오 동시 실행 (k6 scenarios 옵션)**: 거부. 시나리오 간 자원 경합으로 단일 API의 baseline이 오염 — 다른 시나리오의 부하가 background traffic이 됨
- **B (선택) — 시나리오별 별도 docker run, 2분 간격**: 시나리오 간 서버 상태 정상화. baseline의 "깨끗함" 확보
- **C — 같은 docker run 안에서 sequential scenarios 옵션**: 컨테이너 재시작 비용은 아끼지만 한 시나리오 OOM 시 나머지 손실. 거부 (재현성 < 안전성)

### 부하 패턴 모델 — ramping-vus vs constant-vus vs constant-arrival-rate

- **A — `constant-vus` (고정 VU 50, 5분)**: 거부. 워밍업 없이 0→50으로 점프하면 cold start 영향이 측정 전반에 섞임
- **B (선택) — `ramping-vus` (1m@10 → 3m@50 → 1m@0)**: 워밍업 → 목표 → 쿨다운 3단. JVM JIT / 커넥션 풀 워밍업 후 측정. 응답 지연으로 도착률이 낮아지는 closed model이지만 baseline 목적에 적합
- **C — `constant-arrival-rate` (RPS 고정, open model)**: 시스템 지연과 무관하게 도착률 유지. 정확한 RPS 측정에는 우월하지만 baseline 단계의 직관과 어긋남. v2 stress 단계에서 도입

### 토큰 발급 위치 — `setup()` vs VU별

- **A — VU별 매 iteration 로그인**: 거부. 측정 대상 API P95에 로그인 latency 섞임 (`login.js` 외 시나리오)
- **B (선택) — `setup()` 1회 발급 → 토큰을 `default(data)`로 전달**: 측정 대상 API의 순수 latency. AT TTL 30분 > 측정 5분이라 만료 위험 없음
- **C — Pre-issued long-lived token**: 측정 안정성은 높지만 보안 표면 확대 + 운영 토큰 정책과 분리. 거부

### thinkTime — sleep(1) vs 0 vs 변동

- **A — `sleep(0)` (정지 없음)**: 거부. 실제 사용자 패턴 아님. 단위 시간당 호출 폭증 → 측정값이 비현실적으로 보수
- **B (선택) — `sleep(1)` 고정 1초**: 가상 사용자의 다음 행동 대기를 단순 모델로. 시나리오 간 일관성
- **C — `sleep(Math.random()*2)` 변동**: 더 현실적이지만 측정 재현성 손상. v2 검토

### Grafana 캡처 자동화

- **A — 수동 캡처 3장/시나리오**: 사람 트리거에 의존 (현재 선택). 시점 정확도 ±10초 — baseline 단계에서 허용
- **B — Grafana Image Renderer 플러그인으로 측정 시점에 자동 캡처**: 인프라 1개 더. v2 검토
- **C — Prometheus raw 데이터 export → 사후 차트 재구성**: 가장 정확하지만 분석 비용 큼. 거부

### 측정 데이터 규모 — 카드 100장 적정성

- **A — 카드 10장**: 거부. N+1 같은 규모 의존 병목 검출 불가
- **B (선택) — 카드 100장 / 덱 5개 / 유저 1**: 4개 API 측정에 충분. 작은 규모의 한계는 `baseline.md` 명시
- **C — 카드 10,000장 + 덱 500개**: Faker 시드 스크립트 별도 작성. v2 stress 단계에서 검토

---

## [Story 2-1] 4개 API Load Test 시나리오 작성

### User Story

> As a 백엔드 개발자,
I want 4개 핵심 API에 Load Test 패턴을 적용하는 k6 시나리오 스크립트가 준비되길,
So that Story 2-2에서 즉시 실행만 하면 baseline 수치를 수집할 수 있다.
>

### 설계 노트

- 4개 시나리오 공통 골격

    ```jsx
    import http from 'k6/http';
    import { check, sleep } from 'k6';
    import { login } from '../utils/auth.js';
    import { baseThresholds, loadStages } from '../utils/options.js';
    
    if (!__ENV.TARGET_URL) throw new Error('TARGET_URL 필수');
    
    export const options = {
      stages: loadStages,
      thresholds: baseThresholds,
    };
    
    export function setup() {
      return { token: login(__ENV.TARGET_URL, __ENV.TEST_EMAIL, __ENV.TEST_PASSWORD) };
    }
    ```

- 시나리오별 `default` 함수
    - **`login.js`**: `setup()` 없이 매 VU iteration마다 `POST /login` 호출 (로그인 자체가 측정 대상)
    - **`cards.js`**: `GET /api/v1/decks/{deckId}/cards` — 시드 데이터의 `deckId=1` 사용, 카드 100장 응답 기대
    - **`review.js`**: `POST /api/v1/review/sessions` — `deckId=1`로 세션 시작, 응답으로 sessionId 받아 thinkTime 후 종료 호출
    - **`schedule.js`**: `PUT /api/v1/users/me/schedule` — 학습 에너지 값을 매번 다른 값(`Math.floor(Math.random()*10)+1`)으로 PUT
- thinkTime: 각 시나리오 끝에 `sleep(1)` — 가상 사용자가 다음 행동까지 1초 대기 (현실적 트래픽 패턴)

### 완료 기준 (Acceptance Criteria)

- [ ]  4개 시나리오 스크립트(`login.js · cards.js · review.js · schedule.js`)가 `load-test/scenarios/`에 커밋된다
- [ ]  각 스크립트가 `loadStages`(워밍업 1분 VU 10 → 목표 3분 VU 50 → 쿨다운 1분)를 사용한다
- [ ]  각 스크립트가 `baseThresholds`(P95 < 500ms, 에러율 < 1%)를 사용한다
- [ ]  각 스크립트가 `TARGET_URL` 미설정 시 실행 거부된다
- [ ]  4개 모두 docker 명령으로 단독 실행 가능하다 (smoke 수준의 VU 1 단축 모드 옵션 권장)

### 엣지 케이스

- 카드 0개 덱으로 ReviewSession 시작 요청 → API 정의에 따라 4xx 응답 → 에러율에 정상 반영
- 토큰 만료 → 401 응답 → check 실패로 별도 카운트
- 동일 deckId에 동시 ReviewSession 생성 시 비즈니스 규칙(중복 세션 금지)으로 409 응답 → 에러가 아닌 정상 동작 — `check` 함수가 200/409 둘 다 인정

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  4개 스크립트가 smoke 수준 단축 실행(VU 1 / 10초)에서 오류 없이 동작 확인
- [ ]  공통 모듈(`utils/auth.js`, `utils/options.js`) import 검증
- [ ]  스테이징 환경 검증

### 의존성

- 선행: Story 1-1
- 후속: Story 2-2

### 스토리 포인트

- 추정: 3 SP

---

## [Story 2-2] baseline 측정 실행 · Grafana 동시 관찰 · `baseline.md` 작성

### User Story

> As a 백엔드 개발자,
I want 4개 시나리오를 스테이징에서 순차 실행하고 결과를 baseline 문서로 기록하길,
So that 향후 튜닝 작업의 before/after 비교 기준이 코드베이스에 보존되고, 면접·포트폴리오에서 정량 근거로 인용할 수 있다.
>

### 설계 노트

- 실행 순서 (총 ~30분)
    1. 스테이징 서버 재시작 → JVM warm-up
    2. `login.js` 실행 (5분) → 2분 대기
    3. `cards.js` 실행 (5분) → 2분 대기
    4. `review.js` 실행 (5분) → 2분 대기
    5. `schedule.js` 실행 (5분)
- 각 시나리오 실행 시 Grafana 대시보드를 별도 모니터에 띄우고
    - 워밍업 → 목표 부하 전환 시점
    - 목표 부하 진행 중 (1분 30초 시점)
    - 쿨다운 시점
    - 의 3개 캡처를 시나리오당 수집 (총 12장 권장, 최소 4장)
- `baseline.md` 양식

    ```markdown
    # ThirdTool API Baseline (측정일: 2026-MM-DD)
    
    ## 측정 환경
    - 스테이징: <URL>
    - 인스턴스: <스펙>
    - DB: <스펙>
    - HikariCP: maximum-pool-size=10
    - JVM: -Xmx512m
    - 시드 데이터: 카드 100장 / 덱 5개 / 유저 1명
    
    ## Load Test 패턴
    워밍업 1분 (VU 10) → 목표 부하 3분 (VU 50) → 쿨다운 1분
    
    ## 결과 요약
    
    | API | TPS | P50 | P95 | 에러율 |
    | --- | --- | --- | --- | --- |
    | POST /login | ... | ... | ... | ... |
    | GET /decks/{id}/cards | ... | ... | ... | ... |
    | POST /api/v1/review/sessions | ... | ... | ... | ... |
    | PUT /api/v1/users/me/schedule | ... | ... | ... | ... |
    
    ## Threshold 위반 API
    - <목록 또는 "없음">
    
    ## Grafana 캡처
    - !cards 부하 진행 중
    - ...
    ```

- 캡처 저장: `load-test/results/screenshots/` (PNG)

### 완료 기준 (Acceptance Criteria)

- [ ]  스테이징에서 4개 시나리오가 모두 실행되고 k6 summary가 수집된다
- [ ]  `baseline.md`가 작성되어 4개 API의 TPS · P50 · P95 · 에러율이 표로 기록된다
- [ ]  Grafana 캡처가 최소 4장 (시나리오당 1장 이상) `screenshots/`에 저장된다
- [ ]  Threshold 위반(P95 > 500ms 또는 에러율 > 1%) API가 baseline.md에 명시된다
- [ ]  측정 환경(인스턴스 스펙 · DB · HikariCP · JVM · 시드 데이터)이 baseline.md에 기록된다

### 엣지 케이스

- 모든 API가 threshold를 만족하는 경우 → "Threshold 위반 API: 없음" 명시 + Epic 3에서 "더 높은 VU로 한계 측정 권장" 액션 아이템 도출
- 측정 중 스테이징 서버가 다운 → 측정 중단, 원인 기록 후 재시작 후 재측정
- VU 50에 도달하기 전 에러율 폭증 → 워밍업/쿨다운 시점에 인스턴스 사이즈 부족 가설 기록 → Epic 3 분석 입력

### Definition of Done

- [ ]  baseline.md 커밋 확인
- [ ]  Grafana 캡처 4장 이상 커밋 확인
- [ ]  k6 summary 4건 (각 시나리오별) baseline.md에 포함 또는 별도 파일로 첨부
- [ ]  스테이징 측정 1회 완주

### 의존성

- 선행: Story 2-1, Product 1 Epic 3 (Grafana 대시보드 가동 중)
- 후속: Story 3-1

### 스토리 포인트

- 추정: 3 SP

---

# [Epic 3] 병목 분석 및 액션 아이템 등록 · Product 1 threshold 캘리브레이션

## Epic 목표

> baseline 측정 결과를 분석해 병목 API의 원인 가설(Slow Query · N+1 · 풀 포화 · 스레드 포화)을 도출하고, 개선 액션 아이템을 GitHub Issue로 등록한다.
baseline 수치를 근거로 Product 1 대시보드의 threshold 임계치를 재설정해 v2 알림 도입 시 합리적 기준값 위에서 작동하게 한다.
>

## 배경

- 측정만 한 사람과 원인 분석까지 한 사람의 격차 — 채용 포트폴리오에서 가장 임팩트 있는 산출물
- baseline 수치는 휘발성이 아니지만 분석은 측정 직후 메모리가 신선할 때 가장 정확함
- Product 1 threshold 캘리브레이션이 없으면 v2 알림이 임의 추정치 위에서 작동 → false positive 폭증 위험

## 핵심 설계 결정

> **병목 원인 가설은 4가지 분류 중에서 명시한다.**
"느림"이 아닌 구체적 분류로 답.
>
> - **Slow Query / N+1**: 응답 시간 급등 + DB 커넥션 사용률 상승 + 단일 요청 latency가 길어짐
> - **DB 커넥션 풀 포화**: hikaricp_connections_active가 max에 도달 + 신규 요청이 acquire_seconds에 대기
> - **스레드 풀 포화**: jvm_threads_live_threads가 Tomcat max에 도달 + 신규 요청이 큐 대기
> - **외부 의존성 latency**: 외부 API (Toss Payments 등) 응답 지연
> - 가설을 검증하려면 Product 0 로그의 traceId로 부하 시각의 요청 추적 → 어느 단계가 오래 걸렸는지 확인

> **액션 아이템은 GitHub Issue로 등록한다. 문서에만 두지 않는다.**
Issue가 다음 스프린트의 작업 단위.
>
> - 라벨: `performance` + 가설별 (`db-query` · `connection-pool` · `cache`)
> - Issue 본문: baseline 수치 인용 + 원인 가설 + 제안 해결책 (예: 카드 목록 N+1 해결 → `JOIN FETCH` 적용)
> - 우선순위: P95 위반 큰 순으로 정렬

## 완료 기준 (Definition of Done)

- [ ]  `load-test/results/analysis.md`가 작성되고 커밋된다
- [ ]  병목 API별 원인 가설이 4가지 분류 중에서 명시된다
- [ ]  GitHub Issue가 액션 아이템 건수만큼 등록된다 (`performance` 라벨)
- [ ]  Product 1 대시보드의 threshold가 baseline 기반으로 재설정된 PR이 머지된다
- [ ]  연결된 Story가 모두 Done 상태다

## 산출물 (Deliverables)

- `load-test/results/analysis.md`
- GitHub Issue (액션 아이템 건수만큼)
- Product 1 `monitoring/grafana/dashboards/thirdtool.json` threshold 업데이트 PR

## 연결된 Story 목록

- [ ]  Story 3-1. 병목 분석 · `analysis.md` 작성 · GitHub Issue 등록 · threshold 캘리브레이션 (3 SP)

## 내부 메모 / 제약 사항

- 분석은 baseline 측정 후 1주일 내 완료 권장 — Prometheus 시계열 데이터 보존 기간 + 메모리 신선도
- 모든 액션 아이템이 v2 작업이라도 Issue 등록은 v1 완료 기준 — 트래킹 자체가 가치

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

### 가설 분류 체계 — 4분류 vs 자유 서술

- **A — 자유 서술 ("느려 보임", "DB 쪽 같음")**: 거부. 분석가 주관에 의존, 다음 사람이 검증 불가
- **B (선택) — Slow Query/N+1 / DB 풀 포화 / 스레드 풀 포화 / 외부 의존성 4분류**: 각 분류는 Grafana 패널과 1:1 대응 — Product 1의 자산을 그대로 검증 도구로 사용
- **C — APM 분산 트레이스 기반 자동 분류**: APM 미도입 단계. v2

### 액션 아이템 매체 — Issue vs 문서

- **A — `analysis.md` 안에 체크박스 리스트만**: 거부. 문서가 잊혀지면 작업도 잊혀짐
- **B (선택) — `analysis.md` + GitHub Issue 1:1 등록 (`performance` 라벨)**: Issue가 next sprint 진입 매체. baseline 인용으로 before/after 비교 출발점
- **C — GitHub Projects 칸반 자동 등록**: 1인 개발에 칸반 운영 부담 큼. v2

### Issue 라벨 체계

- **A — `performance` 단일 라벨**: 거부. 가설 분류별 검색 / 우선순위 부여 어려움
- **B (선택) — `performance` + 가설별 부 라벨 (`db-query`, `connection-pool`, `cache`)**: 분류 검색 가능. Grafana 패널 그룹과 호환
- **C — 라벨 + Milestone + Assignee**: 1인 개발에 과대. milestone은 v2부터

### threshold 캘리브레이션 트리거

- **A — baseline 평균값에 맞춰 매번 자동 조정**: 거부. baseline이 threshold가 되면 회귀 감지 불가능 (자기 충족 예언)
- **B (선택) — baseline 정상 API의 1.5~2배를 threshold로**: 회귀 감지 여지 확보 + false positive 억제. analysis.md에 근거 명시
- **C — 비즈니스 SLO에서 역산**: SLO 미정의 상태. v2로 이관 (열린 질문)

### "병목 없음" 결론 처리

- **A — Issue 등록 없이 종료**: 거부. 분석 자체의 가치가 트래킹에서 사라짐
- **B (선택) — "병목 없음" 명시 + "더 높은 VU로 한계 측정 권장" Issue 1건 등록**: v2 stress 단계의 진입 트리거가 됨
- **C — Issue 없이 ADR로 결정 기록**: ADR과 Issue는 목적이 다름. Issue가 더 적합 (작업 단위)

---

## [Story 3-1] 병목 분석 · `analysis.md` 작성 · GitHub Issue 등록 · threshold 캘리브레이션

### User Story

> As a 백엔드 개발자,
I want baseline 결과를 분석해 병목 원인 가설과 액션 아이템을 문서·Issue로 정리하고 Product 1 threshold를 재설정하길,
So that 다음 스프린트의 튜닝 작업이 우선순위 있는 Issue 단위로 진행되고, 향후 알림 도입이 합리적 임계치 위에서 작동한다.
>

### 설계 노트

- `analysis.md` 양식

    ```markdown
    # ThirdTool 부하 테스트 분석 (분석일: 2026-MM-DD)
    
    ## 1. baseline 요약
    (baseline.md 표 인용)
    
    ## 2. 병목 API별 원인 가설
    
    ### 2-1. GET /decks/{id}/cards (P95: 820ms, 위반)
    **가설**: N+1 쿼리
    **근거**:
    - 부하 진행 중 hikaricp_connections_active가 8/10까지 상승
    - traceId 추적 결과 단일 요청당 SELECT 호출이 101회 (카드 100장 + 덱 1회)
    - Product 0 로그에서 동일 요청에 SELECT card_* 100건 확인
    **제안 해결책**: CardRepository에 @EntityGraph 또는 JOIN FETCH 적용
    **예상 효과**: P95 200ms 이하
    
    ### 2-2. POST /api/v1/review/sessions (P95: 1,250ms, 위반)
    **가설**: ...
    
    ## 3. 액션 아이템 (GitHub Issue로 등록)
    - [ ] #123 카드 목록 API N+1 해결 (performance, db-query)
    - [ ] #124 ReviewSession 시작 API 트랜잭션 분리 (performance)
    - [ ] #125 HikariCP maximum-pool-size 10→20 검토 (performance, connection-pool)
    
    ## 4. Product 1 threshold 캘리브레이션 제안
    | 패널 | 기존 임계치 | 제안 임계치 | 근거 |
    | --- | --- | --- | --- |
    | API P95 | 500ms | 300ms | baseline 정상 API P95가 100ms 수준 |
    | 에러율 | 1% | 0.5% | baseline 평상시 0% |
    | 커넥션 풀 사용률 | 80% | 70% | 부하 시 80% 도달 = 여유 부족 신호 |
    
    ## 5. 추가 권장 사항
    - 더 높은 VU(100, 200)로 한계 측정 권장 (v2)
    - Soak Test로 메모리 누수 검증 권장 (v2)
    ```

- GitHub Issue 템플릿 적용 (수동 등록)
    - 제목: `[performance] {API 경로} {병목 유형}`
    - 본문: baseline 수치 + 가설 + 제안 + 예상 효과
    - 라벨: `performance` + 가설별 라벨
- Product 1 threshold 캘리브레이션 PR
    - `monitoring/grafana/dashboards/thirdtool.json`의 threshold 값 수정
    - PR 본문에 baseline.md / analysis.md 링크

### 완료 기준 (Acceptance Criteria)

- [ ]  `analysis.md`가 작성되고 코드베이스에 커밋된다
- [ ]  Threshold 위반 API별로 원인 가설이 4가지 분류 중 하나로 명시된다
- [ ]  각 가설의 근거가 Grafana 캡처 / Product 0 로그 / k6 summary 중 최소 하나를 인용한다
- [ ]  GitHub Issue가 액션 아이템 건수만큼 등록되고 `performance` 라벨이 부착된다
- [ ]  Product 1 대시보드 threshold 업데이트 PR이 머지된다

### 엣지 케이스

- 모든 API가 threshold를 만족 → analysis.md에 "병목 없음" 명시 + "더 높은 VU로 한계 측정 권장" 액션 아이템 1건 등록
- 가설을 단정할 수 없는 경우 → "복합 원인 의심" 명시 + 추가 측정 액션 아이템 등록
- threshold 캘리브레이션 결과 기존 임계치가 적절한 경우 → "변경 없음" 명시 PR (의사결정 기록 자체가 가치)

### Definition of Done

- [ ]  analysis.md 커밋 확인
- [ ]  GitHub Issue 등록 확인 (스크린샷 또는 링크)
- [ ]  threshold 업데이트 PR 머지 확인
- [ ]  PO(또는 본인) 셀프 검수 완료

### 의존성

- 선행: Story 2-2
- 후속: 없음 (Product 2 마지막 Story)

### 스토리 포인트

- 추정: 3 SP

---

## Product 2 요약

| Epic | Story 수 | SP 합계 |
| --- | --- | --- |
| Epic 1. k6 환경 구성 및 smoke test | 1 | 3 |
| Epic 2. 부하 시나리오 작성 및 baseline 측정 | 2 | 6 |
| Epic 3. 병목 분석 및 액션 아이템 | 1 | 3 |
| **합계** | **4** | **12 SP** |

**진행 순서 (필수):** Epic 1 → 2 → 3. 일직선 의존성. 환경 → 측정 → 분석 순서.

**Product 0/1과의 연결 포인트**

- Story 2-2의 Grafana 동시 관찰은 Product 1 Epic 3 대시보드를 직접 활용
- Story 3-1의 가설 검증은 Product 0 로그의 traceId 기반 요청 추적 사용
- Story 3-1의 threshold 캘리브레이션이 Product 1로 피드백되어 관측성 사이클이 완성됨

**3개 Product 전체 흐름**

```
Product 0 (로그)  →  Product 1 (메트릭)  →  Product 2 (부하 테스트)
   traceId 발급        대시보드 시각화           baseline 측정
       ↑                    ↑                       ↓
       └────────────────────┴───── 피드백 ─────────┘
                  (traceId 검색 + threshold 캘리브레이션)
```

채용 포트폴리오에서 "로그 → 메트릭 → 부하 테스트 순으로 관측성 레이어를 쌓고, baseline 수치 기반으로 임계치를 캘리브레이션했다"는 서사가 깔끔하게 완성됩니다.