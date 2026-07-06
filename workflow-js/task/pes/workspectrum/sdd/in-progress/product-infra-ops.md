## [Product] 운영·보안 자동화 — Secrets Manager · 백업·관측 통합

# [Product] 운영·보안 자동화 — Secrets Manager · 백업·관측 통합

## Product Vision

> GitHub Secrets에 박혀 있던 모든 비밀(DB · JWT · OAuth · Gemini API key)을 AWS Secrets Manager 단일 진실 소스로 이관한다.
RDS PITR · S3 버전 관리 · CloudWatch Logs/Alarms로 운영 안전망을 깔아, "비밀이 회전 가능하며 / 장애가 알람으로 먼저 감지되는" 상태를 만든다.
A·B가 "인프라를 띄우는 작업"이라면, C는 "그 인프라에 비밀 관리·백업·관측 안전망을 더하는 작업"이다.
>

## 배경 및 문제

- 현재 상황 (As-Is)
    - Product A·B 완료 시점에 VPC · ALB · ECS · RDS · IAM Role이 콘솔/CLI로 손으로 떠 있는 상태 → 변경 추적 불가
    - DB 비밀번호 · JWT secret · Kakao/Naver OAuth client secret · Gemini API key가 GitHub Secrets에 산재 → 회전 시 GHA 워크플로 다수 동시 수정 필요
    - RDS 자동 백업 7일은 활성화되어 있으나 **PITR 복구 드릴 미실행** → 실제 복구 가능성 미검증
    - CloudWatch Logs에 awslogs로 출력은 되지만 Alarm이 없음 → CPU/메모리/에러율 이상이 발생해도 사람이 봐야만 인지
    - S3 (이미지 · 자료 첨부) 버전 관리 미활성 → 실수 삭제 시 복구 불가
- 발생하는 문제
    - 인프라 변경이 PR/리뷰 없이 콘솔에서 진행 → 의도하지 않은 설정 변경이 운영 사고로 직결
    - GitHub Secrets 회전이 사실상 불가능 → 1년 묵은 비밀이 prod에서 그대로 사용 중일 가능성
    - RDS 백업이 "있다"고 가정만 하고 실제 복구 가능 여부 미확인 → 진짜 장애 시 RTO 측정 불가
    - 면접에서 "Secrets Manager는 어떻게 통합했나, RDS 백업 복구 드릴은?"에 정량 답변 불가
- 왜 지금 해결해야 하는가
    - 비밀 회전은 사고 발생 후 회전하는 것보다 평상시에 체계화하는 것이 훨씬 쌈
    - Vertex AI Gemini ADC 인증을 Product B의 Task Role + Secrets Manager 패턴으로 정착해야 `product-aisuggestion.md` Story 2-1의 "GCP ADC" 가정이 운영 환경에서 깔끔하게 동작
    - 향후 도메인 Product의 시크릿 추가(예: 결제 PG · 외부 API)가 본 Product의 패턴 위에서 이뤄짐 → 표준 부재 시 도메인 Product마다 시크릿 관리 방식이 갈림
    - 면접·포트폴리오에서 "Secrets Manager + 백업 드릴"이라는 정량 근거 확보

## 목표 (To-Be)

- AWS Secrets Manager에 5종 비밀(DB credential · JWT · Kakao OAuth · Naver OAuth · Gemini API key)이 prod/staging prefix로 분리 저장된다
- Spring Boot가 부팅 시 Secrets Manager에서 값을 로딩하고 `application-{env}.yml`의 placeholder를 채운다 (`${secret-name}`)
- ECS Task Role이 `secretsmanager:GetSecretValue`로 특정 prefix만 허용된다
- GitHub Secrets에 영구 저장된 시크릿 0건 (OIDC AssumeRole 외 모두 Secrets Manager로 이관)
- RDS PITR 7일 복구 드릴 1회 실행 + RTO 측정 결과 Runbook에 기록
- S3 버전 관리 + 라이프사이클(30일 후 IA, 90일 후 Glacier Deep Archive) 적용
- CloudWatch Alarm 4종(ECS Task CPU > 80% / RDS Connections > 80% / ALB 5xx > 1% / NAT data transfer 이상) 활성
- Alarm → SNS 토픽 구독 (이메일 · v2 Slack)

## 설계 결정 (Design Decision)

> **Secrets Manager 비밀은 부팅 시 Spring Boot가 로딩한다. SDK 런타임 조회는 사용하지 않는다.**
회전 시 재기동 비용 vs 런타임 호출 비용의 절충.
>
> - `spring-cloud-aws-secrets-manager`(또는 `awssdk` 직접) 부팅 시 Secrets Manager 호출 → `@Value` placeholder 치환
> - 비밀 회전 시 ECS Task 재기동(`aws ecs update-service --force-new-deployment`) → 무중단 배포 패턴 재사용 (Product B Story 3-1)
> - 런타임 SDK 호출은 도메인 코드에 AWS SDK 침투 + 호출 비용·실패 처리 부담 증가
> - 단점: 회전 자동 감지 불가 (재기동 수동 트리거) → v2에서 EventBridge + Lambda로 자동화 검토
> - 이 결정은 ADR로 별도 기록한다 (`ADR-SEC-002: Secret Loading — Boot-time only, no SDK at runtime`)

> **Secrets Manager는 환경별 prefix로 격리한다.**
실수로 prod 비밀을 staging에서 읽는 사고 차단.
>
> - prod: `thirdtool/prod/db-credential` · `thirdtool/prod/jwt-secret` · ...
> - staging: `thirdtool/staging/db-credential` · ...
> - Task Role 권한 정책에 `Resource: arn:aws:secretsmanager:*:*:secret:thirdtool/${env}/*` 명시 → cross-env 접근 차단
> - 이 결정은 ADR로 별도 기록한다 (`ADR-SEC-001: Secret Naming & Env Isolation`)

> **RDS 백업: PITR 7일 + 수동 스냅샷 월 1회 30일 보존.**
짧은 윈도우는 PITR · 장기는 스냅샷.
>
> - PITR (Point-In-Time Recovery): 자동 백업 보존 기간 내 임의 시각 복구 (Product A에서 prod 7일 / staging 1일)
> - 수동 스냅샷: 월 1회 + 30일 보존 → 7일 초과 시점의 데이터 복구 가능
> - 자동화: EventBridge + Lambda 또는 AWS Backup Service (단순 운영 → Backup Service 채택)
> - PITR 복구 드릴 1회 필수 → RTO 측정 + Runbook 기록
> - 이 결정은 ADR로 별도 기록한다 (`ADR-OPS-001: RDS Backup Retention`)

## 대안 검토 (Alternatives Considered)

> 5종 갈림길마다 "왜 이것이 아니고 저것인가"를 남긴다. 1인 운영 + 토이 트래픽 + 채용 가시성이라는 제약이 어떤 안을 거부하게 만들었는지가 핵심이다.

### Secrets 저장소

**Option A — `.env` + GitHub Secrets 유지 (현 상태)**
- 장점: 추가 인프라 비용 0, 학습 비용 0
- 거부 이유:
    - 비밀 회전 시 GHA 워크플로 다수 수정 → 사실상 회전 안 됨 (1년 묵은 비밀이 prod에서 사용 중일 가능성)
    - 비밀이 평문으로 git history와 환경 변수에 분산 → 감사 추적 불가
    - "어디서 누가 언제 읽었는지" 로그 부재

**Option B — HashiCorp Vault (자체 호스팅)**
- 장점: 멀티 클라우드, dynamic secrets, fine-grained 정책
- 거부 이유:
    - 1인 운영에 Vault 서버 자체의 운영 부담 (HA 구성 · seal/unseal · 백업) > 얻는 이득
    - AWS 단일 클라우드 + 5종 비밀이라는 규모에 과잉
    - SaaS Vault는 월 $200+ — 토이 규모 정당화 불가

**Option C — AWS Systems Manager Parameter Store (SecureString)**
- 장점: Secrets Manager보다 저렴 (Standard tier 무료), KMS 암호화 동일
- 거부 이유:
    - JSON 구조 비밀(`db-credential`의 user/pass 묶음)에 1차 비지원 → 키별 분리 등록 필요
    - 회전 통합 부재 (Secrets Manager는 RDS 회전 통합 제공)
    - Spring Cloud AWS 통합 측면에서 `spring-cloud-aws-starter-secrets-manager`가 더 표준
    - 비용 차이: 10개 비밀 × $0.40/월 = $4/월 — 절감 가치보다 통합 편의가 큼

**Option D (선택) — AWS Secrets Manager (env prefix 격리)**
- 비용: 월 $4 (10개 비밀) + API 호출 비용 (부팅 시점만이라 무시 가능)
- 보상: RDS 회전 통합 · KMS 암호화 · CloudTrail 감사 로그 · Spring 표준 통합 · ARN prefix로 IAM 권한 경계
- 트레이드오프 수용 근거: 비용은 토이 규모에서도 흡수 가능, 채용 가시성·운영 표준화 이득이 큼

### Secrets 로딩 시점

**Option A — 런타임 lazy 로딩 (요청마다 SDK 호출)**
- 거부 이유: 도메인 코드에 AWS SDK 침투 + 호출 비용·실패 처리 부담 + 응답 지연
- 캐싱 도입 시 만료·갱신 로직이 또 다른 복잡도

**Option B — Sidecar 컨테이너 (init container로 파일 주입)**
- 거부 이유: ECS Fargate에서 sidecar 추가 시 Task 정의 복잡도 + 추가 메모리 비용. 부팅 일괄 로딩 대비 이득 작음

**Option C (선택) — Spring Boot 부팅 시 일괄 로딩**
- 비용: 비밀 회전 시 ECS `force-new-deployment` 수동 트리거 필요 (회전 자동 감지 불가)
- 보상: 도메인 코드는 `@Value` placeholder만 알면 됨. SDK 호출은 부팅 1회로 한정
- v2 — EventBridge + Lambda로 회전 감지 자동화 검토

### Spring Boot 통합 방식

**Option A — 자체 SDK 통합 (awssdk + ApplicationContextInitializer)**
- 거부 이유: 표준 라이브러리가 있는데 자체 구현은 유지 비용만 추가. 버그 시 본인이 책임

**Option B (선택) — `spring-cloud-aws-starter-secrets-manager` 3.x**
- 비용: Spring Cloud AWS 버전과 Spring Boot 버전 호환 매트릭스 관리
- 보상: `spring.config.import: aws-secretsmanager:...` 한 줄로 placeholder 치환. 커뮤니티 관리

### 백업 정책

**Option A — RDS 자동 백업 7일 only (PITR만)**
- 거부 이유: 7일 초과 시점의 데이터 손상이 늦게 발견되면 복구 불가. 컴플라이언스성 장기 보존 부재

**Option B — Manual snapshot + cross-region replication**
- 거부 이유: 토이 규모에서 멀티 리전 비용 정당화 어려움. v3에서 DR로 분리

**Option C (선택) — PITR 7일 (단기) + AWS Backup Service 월간 스냅샷 30일 (장기)**
- 비용: 스냅샷 스토리지 ∼$10/월 (100GB 기준)
- 보상: 단기 윈도우는 PITR 임의 시각 복구, 장기 윈도우는 월간 snap, 자동화는 AWS Backup이 담당

### 백업 보존 기간

- **7일 only**: PITR과 동일. 장기 손상 미커버
- **30일 (선택)**: AWS Backup 월간 스냅샷 보존. 토이 규모 적정
- **90일+**: 컴플라이언스 요구 없는 한 비용 vs 가치 불균형. 거부
- **컴플라이언스 (SOC2/PCI-DSS의 1년)**: 토이 규모 적용 안 함 (Out of Scope)

### 관측 통합

**Option A — Datadog / NewRelic SaaS**
- 거부 이유: 월 $15+/host × 노드 수 → 토이 규모 정당화 불가. 면접 가시성은 있으나 비용 대비 효율 낮음

**Option B — Prometheus + Grafana 자체 호스팅**
- 거부 이유: 메트릭 서버 자체의 운영 부담 + 1인 운영에 과잉. Product 0 v2 / `product-op.md` 범위

**Option C (선택) — CloudWatch Logs + Alarm + SNS (AWS 내부 stack only)**
- 비용: Log 수집 GB당 $0.50, Alarm 메트릭당 $0.10/월. 토이 규모 ∼$5/월
- 보상: VPC 내부 트래픽 · IAM 통합 · 추가 인프라 0. Alarm threshold는 `product-load-test.md` analysis.md baseline 활용

### 알람 채널

**Option A — PagerDuty**
- 거부 이유: 1인 운영에 on-call rotation 부재. 월 $19+/user는 토이 규모 과잉

**Option B — Slack webhook (SNS → Lambda → Slack)**
- 보류 이유: v2로 분리. v1은 이메일로 충분 (응답 SLA 없음)

**Option C (선택) — SNS → 이메일 구독 (v1)**
- 비용: SNS 알림 발송 100건 = $0.06. 사실상 무료
- 보상: 최소 인프라로 알람 도달 검증 가능. v2에서 Slack/PagerDuty 자연 확장

### IAM 권한 경계

**Option A — 단일 통합 Role (`thirdtool-task-role`)에 모든 권한 부여**
- 거부 이유: 최소 권한 원칙 위배. 한 비밀 노출 시 전체 비밀 노출

**Option B — 서비스별 Role 완전 분리 (Card · Deck · Learning 각각)**
- 거부 이유: 1인 운영 단일 ECS Service이므로 과잉. 모놀리스 단계 적합 X

**Option C (선택) — env prefix 기반 권한 경계 (`Resource: arn:aws:secretsmanager:*:*:secret:thirdtool/${env}/*`)**
- 비용: prod/staging Task Role 2개 분리 관리
- 보상: cross-env 접근 자동 차단. 환경 단위 격리는 보안 사고 폭발 반경 축소에 가장 효과적

## 전체 아키텍처 (High-Level Architecture)

> Secrets Manager 부팅 로딩 + 백업·관측 stack의 컴포넌트 배치와 핵심 플로우.

### 컴포넌트 배치

```
┌─────────────────────────────────────────────────────────────────┐
│ AWS Account (ap-northeast-2)                                     │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Secrets Manager                                            │  │
│  │  thirdtool/prod/{db-credential,jwt-secret,kakao-oauth,    │  │
│  │                  naver-oauth,gemini-api-key}              │  │
│  │  thirdtool/staging/{...same 5 keys}                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│         ▲                                                        │
│         │ secretsmanager:GetSecretValue                          │
│         │  (prefix-scoped IAM)                                   │
│         │                                                        │
│  ┌──────┴───────────────────────────────────────────────────┐  │
│  │ VPC (private subnets)                                     │  │
│  │  ┌───────────────────────────┐    ┌────────────────────┐ │  │
│  │  │ ECS Fargate Task           │    │ VPC Endpoint       │ │  │
│  │  │  Spring Boot               │◄───┤ - Secrets Manager  │ │  │
│  │  │  ├─ bootstrap.yml          │    │   (Interface)      │ │  │
│  │  │  │   spring.config.import: │    │ - ECR (Interface)  │ │  │
│  │  │  │   aws-secretsmanager:.. │    │ - S3 (Gateway)     │ │  │
│  │  │  └─ @Value placeholder     │    └────────────────────┘ │  │
│  │  │     자동 치환 (부팅 1회)     │                          │  │
│  │  └───────────────────────────┘                            │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Backup Stack                                              │  │
│  │  RDS PITR 7일 (자동)                                      │  │
│  │  AWS Backup Plan (월 1회 RDS snapshot, 30일 보존)          │  │
│  │  S3 versioning + lifecycle (30d IA → 90d Deep Archive)    │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Observability Stack                                        │  │
│  │  CloudWatch Logs (awslogs driver)                          │  │
│  │  CloudWatch Alarm × 4                                      │  │
│  │   ├─ ECS Task CPU > 80%                                    │  │
│  │   ├─ RDS Connections > 80%                                 │  │
│  │   ├─ ALB 5xx > 1%                                          │  │
│  │   └─ NAT BytesOutToDestination > 10GB/일                   │  │
│  │             │                                              │  │
│  │             ▼                                              │  │
│  │  SNS topic `thirdtool-ops-alerts` → 이메일 구독             │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 핵심 플로우

**1. Spring Boot 부팅 — Secrets Manager 일괄 로딩**
```
ECS Task 시작
   │
   ▼
Spring Boot bootstrap phase
   │  spring.config.import: aws-secretsmanager:thirdtool/${ENV}/db-credential;...
   ▼
spring-cloud-aws-starter-secrets-manager
   │  → AWS SDK GetSecretValue × 5
   │  (VPC Endpoint 경유, NAT 미경유)
   ▼
PropertySource 등록
   │  username/password/jwt-secret/client-id/...
   ▼
application-prod.yml placeholder 치환
   │  ${username} → 실제 값
   ▼
DataSource · JWT · OAuth 클라이언트 정상 초기화
   │
   ▼  실패 시: BeanCreationException → Task 종료 → ECS 재시작
   ▼                                  → CloudWatch Logs에 명시적 에러
Task RUNNING
```
부팅 시 비밀 미존재 또는 IAM 권한 부족이면 즉시 fail-fast — 비정상 부팅을 운영자가 5분 안에 인지 (Alarm 미설정 시점에도 ECS Service 이벤트로 노출).

**2. 알람 전파 — CloudWatch Alarm → 이메일**
```
ECS Task CPU 평균 80% 초과 (5분 윈도우, 2회 연속)
   │
   ▼
CloudWatch Alarm 상태 OK → ALARM 전이
   │
   ▼
alarm_actions → SNS topic `thirdtool-ops-alerts`
   │
   ▼
SNS → 이메일 구독자 발송
   │
   ▼  목표: 임계 초과 후 < 5분에 도달
운영자: Runbook 따라 대응 (PITR 복구 / Task 스케일 / 비용 조정)
```

### Out-of-Process 의존

- **AWS Secrets Manager**: 5종 × 2환경 = 10개 비밀. 부팅 시점에만 호출 (런타임 X)
- **AWS Backup Service**: 월간 RDS 스냅샷 자동화 (30일 보존)
- **AWS CloudWatch**: Logs · Metric · Alarm
- **AWS SNS**: 알람 fan-out
- **AWS KMS**: Secrets Manager · S3 · DynamoDB 암호화 키 (AWS 관리형 사용)
- **VPC Endpoint** (Secrets Manager · ECR · S3): NAT 비용 절감 + 비공개 경로
- **GitHub OIDC Provider**: GHA에서 정적 키 없이 AWS 자격증명 발급

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 대응

| 시나리오 | 원인 | 감지 경로 | 클라이언트/운영자 권장 동작 |
| --- | --- | --- | --- |
| Secrets 로딩 실패 (비밀 미등록) | 부팅 전 비밀 등록 누락 | ECS Service 이벤트 + CloudWatch Logs `BeanCreationException` | 비밀 등록 후 `force-new-deployment` |
| Secrets 로딩 실패 (IAM AccessDenied) | Task Role prefix 권한 미부여 또는 잘못된 ARN | CloudWatch Logs `AccessDeniedException` | IAM policy 검증 + 적용 후 재기동 |
| Secrets 로딩 실패 (VPC Endpoint 누락) | Interface Endpoint SG·DNS 미설정 → NAT 경유 실패 (격리된 VPC에서) | Boot timeout (SDK 호출 실패) | Endpoint SG 인바운드 443 + private_dns_enabled 검증 |
| JSON 키 이름 불일치 | `db-credential` JSON의 키와 application.yml placeholder 명 불일치 | 부팅 시 placeholder 미치환 + 빈 값으로 DataSource 초기화 실패 | naming convention 표 재검증 |
| Secrets 회전 직후 기존 값 사용 | force-new-deployment 미트리거 | 정상 동작 (다음 회전까지 인지 불가) | 회전 절차 Runbook의 force-new-deployment 단계 강제 |
| RDS 백업 실패 (AWS Backup) | IAM `rds:CreateDBSnapshot` 권한 부재 | AWS Backup job FAILED + CloudWatch Event | IAM policy 보강 + 재실행 |
| PITR 윈도우 초과 (7일+) 시점 복구 요청 | 손상 발견 지연 | — | 월간 snapshot (30일)로 fallback → RPO 최대 30일 감수 |
| Alarm 누락 (false negative) | threshold가 너무 높음 / metric 미수집 | 사고 발생 후 사후 인지 | analysis.md baseline으로 threshold 재캘리브레이션 |
| Alarm 폭주 (false positive) | threshold가 너무 민감 | 운영자 이메일 폭증 | 첫 1주 운영 후 조정. evaluation_periods 증가 |
| SNS 이메일 미도달 | 구독 미확인 (`Pending Confirmation`) | 알람 발동했으나 메일 0건 | 구독 confirm + 백업 채널 추가 |
| NAT 비용 폭증 | VPC Endpoint 누락 또는 외부 호출 폭주 | NAT BytesOut Alarm | VPC Endpoint 추가 / 외부 호출 코드 점검 |

### 로깅 정책

- **항상 기록**: Secrets 로딩 결과 (`secret-name + 성공/실패`), Terraform apply 결과 (state version), Alarm 상태 전이
- **debug 레벨**: Spring Cloud AWS PropertySource resolution trace — info로 두면 부팅 로그 폭주
- **절대 금지**: Secrets 값 평문, JWT 서명 키, OAuth client-secret, DB password, KMS data key (마스킹 필터로 이중 방어)

### 관측 지표

| 지표 | 측정 위치 | v1 목표 | v2 비고 |
| --- | --- | --- | --- |
| Secrets 회전 주기 | 운영 Runbook 기록 | 비밀별 마지막 회전 일자 추적 | 자동 회전 (EventBridge + Lambda) |
| 백업 성공률 | AWS Backup job 성공/실패 | 100% (월 1회) | 실패 시 SNS 자동 알림 |
| Alarm 발동 후 알림 도달 시간 | 의도적 임계 초과 시뮬레이션 | < 5분 | Slack 통합 시 < 1분 |
| Secrets 로딩 시간 (부팅) | CloudWatch Logs 시간차 | < 3초 | 캐싱 도입 검토 |
| VPC Endpoint NAT 절감액 | NAT BytesOut 비교 (24h 전후) | 30%+ 감소 | 실측 후 Runbook 기록 |
| RDS PITR 복구 RTO | 드릴 1회 측정 | Runbook 기록 (예: 12분) | 분기별 재드릴 |

## 롤아웃 / 마이그레이션 (Rollout)

### 전제 — 프로덕션 트래픽 없음 + 손으로 띄운 인프라 존재

현재 사용자 0명. Product A·B 산출물(VPC · ALB · RDS · ECS · IAM Role)이 콘솔/CLI로 살아 있는 상태. 따라서 **무중단 전환 불필요** — 비밀 이관과 백업·관측 활성화는 기존 리소스에 영향 없이 추가된다.

### 5종 비밀 이관 단계 (Epic 1)

```
Phase 1 (1일차) — 비밀 등록
  ├─ Secrets Manager에 10개 비밀 등록 (콘솔 또는 CLI)
  │   thirdtool/{prod,staging}/{db-credential,jwt-secret,kakao-oauth,naver-oauth,gemini-api-key}
  └─ JSON 키 이름 vs application.yml placeholder 매핑 표 확정

Phase 2 (2일차) — Spring Boot 통합
  ├─ build.gradle에 spring-cloud-aws-starter-secrets-manager:3.x 추가
  ├─ bootstrap.yml 작성 (spring.config.import)
  ├─ application-prod.yml placeholder 치환 ($DB_PASSWORD → $password)
  └─ staging Task Role policy 갱신 → thirdtool/staging/* 만 허용

Phase 3 (3일차) — staging 검증
  ├─ staging ECS force-new-deployment
  ├─ CloudWatch Logs에서 Secrets 로딩 성공 로그 확인
  ├─ 의도적으로 prod 비밀 접근 시도 → 403 확인
  └─ 의도적으로 비밀 1건 미등록 → 부팅 실패 + 명시적 에러 확인

Phase 4 (4일차) — prod 전환
  ├─ prod Task Role policy 갱신 → thirdtool/prod/* 만 허용
  ├─ prod force-new-deployment
  ├─ 로딩 성공 확인
  └─ GitHub Secrets에서 5종 키 삭제 (DB_PASSWORD · JWT_SECRET · KAKAO_CLIENT_SECRET · NAVER_CLIENT_SECRET · GEMINI_API_KEY)

Phase 5 (5일차) — 사후 검증
  ├─ application.yml grep으로 평문 비밀 0건 검증
  ├─ Secrets Manager 콘솔에서 마지막 회전 일자 기록
  └─ docs/runbook/secrets-migration.md 커밋
```

비밀 1건씩 점진 이관도 가능하나 1인 운영에서는 일괄 전환이 더 단순. 단계 분리는 staging→prod 사이에만.

### 백업·관측 활성화 단계 (Epic 2)

```
Phase 1 — PITR 드릴
  ├─ staging-rds 임의 시점 복구 명령 실행
  ├─ available까지 시각 측정
  ├─ 데이터 검증
  ├─ 복구 인스턴스 삭제 (비용)
  └─ Runbook RTO 기록

Phase 2 — S3 버전 관리 + 라이프사이클
  ├─ 도메인 버킷별 versioning 활성
  └─ lifecycle 정책 적용 (30d IA, 90d Deep Archive, 365d noncurrent expire)

Phase 3 — Alarm 4종 + SNS
  ├─ aws cloudwatch put-metric-alarm으로 alarm 4종 정의 (infra/ CLI 스크립트 or 콘솔)
  ├─ 이메일 구독 confirm
  └─ SNS 구독 활성

Phase 4 — VPC Endpoint
  ├─ S3 Gateway Endpoint (무료)
  ├─ Secrets Manager · ECR Interface Endpoint
  └─ NAT data transfer 24시간 비교

Phase 5 — AWS Backup 월간 스냅샷
  ├─ Backup Plan + Selection + IAM Role
  └─ 첫 실행 (수동 trigger로 검증) 또는 다음 1일 04:00 cron 대기

Phase 6 — 알람 도달 검증
  ├─ staging에서 의도적 부하 (CPU > 80%)
  └─ 이메일 도달 시간 측정 (목표 < 5분)
```

### 롤백 계획

| 변경 | 롤백 방법 |
| --- | --- |
| Secrets Manager 통합 | bootstrap.yml 제거 + application.yml placeholder 원복 + GitHub Secrets 복구 — 단 평문 비밀이 git history에 남는 것을 감수 |
| Alarm 활성 | aws cloudwatch delete-alarms로 제거 — 부작용 없음 |
| AWS Backup Plan | Plan 비활성화 — 기존 스냅샷은 그대로 (수동 삭제 필요 시 별도) |
| VPC Endpoint | 제거 시 NAT 경유 복귀 — 트래픽 차단 없음 |

### 환경별 설정 분기

- `bootstrap.yml`: `spring.config.import: aws-secretsmanager:thirdtool/${ENV:dev}/...` — `ENV` 변수가 ECS Task Definition에서 주입 (prod/staging)
- `application-dev.yml`: Secrets Manager 미사용 — 로컬 H2 + 평문 placeholder (개발 편의)
- `application-prod.yml` / `application-staging.yml`: Secrets Manager placeholder 사용
- Task Role: prod와 staging 분리 (`thirdtool/prod/*` vs `thirdtool/staging/*` Resource 조건)

## 성공 지표 (KPI)

| 지표 | 현재 값 | 목표 값 | 측정 방법 |
| --- | --- | --- | --- |
| GitHub Secrets에 영구 저장된 비밀 (배포 외) | 4~5건 | 0 | GitHub repo settings 검사 |
| Secrets Manager에 저장된 비밀 수 | 0 | 5종 × 2환경 = 10건 | `aws secretsmanager list-secrets` |
| Task Role의 secretsmanager 권한 prefix 조건 | 미상 | env별 prefix only | IAM policy 검사 |
| RDS PITR 복구 드릴 실행 | 0회 | 1회 (RTO 측정) | Runbook 기록 |
| S3 버전 관리 활성 버킷 수 | 미상 | 모든 도메인 버킷 | `aws s3api get-bucket-versioning` |
| CloudWatch Alarm 활성 수 | 0 | 4종 + SNS 구독 | `aws cloudwatch describe-alarms` |
| Alarm 발동 후 알림 도달 시간 | — | < 5분 | 의도적 임계 초과 시뮬레이션 |

## Scope

- **In Scope**
    - AWS Secrets Manager 비밀 10개 (5종 × prod/staging)
    - Spring Boot Secrets Manager 통합 (`spring-cloud-aws-secrets-manager` 또는 직접 SDK)
    - AWS Backup Service (RDS 월간 스냅샷 + 30일 보존)
    - PITR 복구 드릴 1회 + Runbook 작성
    - S3 버전 관리 + 라이프사이클
    - CloudWatch Alarm 4종 (ECS · RDS · ALB · NAT) + SNS 이메일 구독
    - VPC Endpoint (Secrets Manager · ECR · S3) — NAT 비용 절감
- **Out of Scope**
    - Secrets 자동 회전 (rotation Lambda) — v2 (`ADR-SEC-002` 후속)
    - Slack 알림 (SNS → Slack webhook) — v2
    - CloudWatch Logs → Loki/Elasticsearch 수집 — v2 (Product 0 v2)
    - AWS Config · Security Hub · GuardDuty 통합 — v2
    - 멀티 리전 DR — v3
    - Terraform IaC 코드화 — v0.0.5+ 이후 별도 마일스톤 (infra/ JSON spec이 선행 문서)
    - SOC2 · ISO27001 같은 컴플라이언스 — 토이 규모 적용 안 함

## 대상 사용자

- 주요 사용자: ThirdTool 백엔드 개발자 (1인 운영)
- 사용 맥락:
    - 비밀 회전 필요 (DB 비밀번호 정기 변경 · OAuth client secret 노출 등) → Secrets Manager 값 수정 + ECS force-new-deployment
    - RDS 장애 발생 → CloudWatch Alarm 알림 + PITR 복구 (Runbook 따라)
    - 신규 도메인 Product 추가 시 → 본 Product의 Secrets 패턴을 그대로 따름
    - 면접·포트폴리오 → "비밀 관리 어떻게 했나 / 복구 드릴 결과"의 정량 답변

## 연결된 Epic 목록

- [ ]  Epic 1. Secrets Manager 통합 — 5종 비밀 이관 + Spring Boot 부팅 로딩 + IAM 권한 경계
- [ ]  Epic 2. 백업·관측 통합 — RDS PITR 드릴 + S3 버전 관리 + CloudWatch Alarm 4종 + SNS

## 관련 문서

- 상위 문서: ThirdTool 백엔드 컨벤션 · 운영 표준
- 선행 Product:
    - `product-infra-network.md` (Product A) — VPC · ALB · RDS 구성 선행
    - `product-infra-deploy.md` (Product B) — Task Role · ECS Cluster · OIDC IAM Role 선행
- 후속 Product:
    - 도메인 Product의 시크릿 추가 시 본 Product 패턴 재사용
    - v2 알림 Product (Slack/PagerDuty) — Alarm SNS 입력
- 영향 받는 Product:
    - `product-auth.md` — JWT secret · OAuth client secret이 Secrets Manager로 이관
    - `product-aisuggestion.md` — Gemini API key 이관, Vertex AI ADC는 GCP Workload Identity Federation 별도 검토
    - `product-op.md` — CloudWatch Alarm threshold가 baseline 기반으로 캘리브레이션 (`product-load-test.md` analysis.md 인용)
    - `product-load-test.md` — analysis.md의 Product 1 threshold 캘리브레이션이 CloudWatch Alarm으로 자연스럽게 연결
- 참고 자료: Spring Cloud AWS Secrets Manager · AWS Backup Service
- ADR 후보: `ADR-SEC-001~002`, `ADR-OPS-001`

## 열린 질문 (Open Questions)

> 본 Product 범위에서 결정 보류 + v2 이후 의사결정 필요 항목. 의식적 보류이며 향후 Story로 분리한다.

- **Secrets 자동 회전** — DB credential은 Secrets Manager의 RDS rotation Lambda로 자동 회전 가능. JWT/OAuth client-secret/Gemini API key는 외부 provider 의존 → 자동 회전 가능 범위와 절차 미확정. v2 (`ADR-SEC-002` 후속)에서 비밀별 정책 수립.
- **Vertex AI ADC 인증 방식** — Gemini API key (Secrets Manager 등록 완료) vs GCP Workload Identity Federation. WIF가 정석이나 AWS↔GCP cross-cloud trust 설정 부담. `product-aisuggestion.md` Story 2-1에서 최종 결정.
- **Alarm threshold 캘리브레이션 합류 시점** — v1은 추정치 임계 (CPU 80% · Connections 80% 등). `product-load-test.md` analysis.md baseline 결과로 v2에서 갱신. 합류 PR을 본 Product `module.ops`에 직접 적용할지, `product-op.md`에서 분리 관리할지 미결정.
- **Slack 알림 통합 방식** — SNS → Lambda → Slack webhook vs SNS → Chatbot (AWS Chatbot) vs 외부 PagerDuty. v2 알림 Product 분리 가능성.
- **컴플라이언스 대응** — SOC2 · ISO27001 · 개인정보보호법 백업 보존 기간(예: 1년)이 필요한가. 토이 규모는 적용 안 함. 사업화 시점 재검토.
- **KMS Customer Managed Key 전환** — 현재 AWS 관리형 키 사용. CMK 전환 시 키 회전 · 키 정책 관리 부담. 컴플라이언스 요구 발생 시점 전환.
- **CloudWatch Logs → Loki/Elasticsearch 수집 전환** — `product-log.md` v2와 합류. 본 Product가 awslogs driver 입력을 표준화하고, 후속 Product가 수집 stack을 추가.
- **백업 cross-region 복제** — 멀티 리전 DR. 토이 규모에서는 v3. 사업화 + RPO 요구 명시 시 활성.

---

| Epic | Story 수 | SP 합계 |
| --- | --- | --- |
| Epic 1. Secrets Manager 통합 | 1 | 4 |
| Epic 2. 백업·관측 통합 | 1 | 3 |
| **합계** | **2** | **7 SP** |

**진행 순서 (필수):** Epic 1 → 2. Secrets 통합 → 운영 안전망.

---

## Epic 1. Secrets Manager 통합 — 5종 비밀 이관 + Spring Boot 부팅 로딩 + IAM 권한 경계

# Epic 1. Secrets Manager 통합 — 5종 비밀 이관 + Spring Boot 부팅 로딩 + IAM 권한 경계

## Epic 목표

> GitHub Secrets · application.yml에 산재된 5종 비밀(DB credential · JWT secret · Kakao/Naver OAuth client secret · Gemini API key)을 AWS Secrets Manager의 prod/staging prefix로 분리 저장하고, Spring Boot가 부팅 시 자동 로딩하는 표준을 깐다.
ECS Task Role이 본인 환경 prefix만 접근 가능하도록 권한 경계가 정합한다.
>

## 배경

- Product B Epic 2에서 Task Role에 `secretsmanager:GetSecretValue` 권한이 미리 부여되어 있으므로 본 Epic이 그 권한을 실제 사용하는 시점
- DB 비밀번호가 GitHub Secrets에 박혀 있는 한 회전 시 GHA 워크플로 동시 수정 필요 → 회전 비용이 크면 사실상 회전 안 됨
- Vertex AI Gemini의 ADC 인증은 GCP 측 패턴이지만 API key 사용 시 본 Product의 Secrets Manager로 자연스럽게 통합 가능

## 핵심 설계 결정

> **비밀 단위 분리**. JSON 하나에 묶지 않는다.
독립 회전 가능.
>
> - 5종 비밀이 각각 별도 Secret으로 등록 → 회전 시 다른 비밀과 무관
> - DB credential만 user/password 묶음 JSON (Secrets Manager의 RDS 통합 패턴)
> - 예: `thirdtool/prod/db-credential` (`{"username":"...","password":"..."}`)
> - `thirdtool/prod/jwt-secret` · `thirdtool/prod/kakao-oauth` · `thirdtool/prod/naver-oauth` · `thirdtool/prod/gemini-api-key`

> **Spring Boot 부팅 시 Secrets Manager 호출. SDK 런타임 호출 X.**
도메인 코드가 AWS SDK 모름.
>
> - `spring-cloud-aws-starter-secrets-manager` 의존성 추가
> - `bootstrap.yml`에 `spring.config.import: aws-secretsmanager:thirdtool/${ENV}/`
> - application.yml에서 `${jwt-secret}` placeholder가 자동 치환
> - 회전 시 ECS `update-service --force-new-deployment`로 재기동

## 완료 기준 (Definition of Done)

- [ ]  Secrets Manager에 5종 × 2환경 = 10개 비밀이 등록된다
- [ ]  Spring Boot가 부팅 시 Secrets Manager에서 값을 로딩하고 application.yml의 placeholder를 채운다
- [ ]  Task Role에 `Resource: arn:aws:secretsmanager:*:*:secret:thirdtool/${env}/*` 권한 경계가 적용된다
- [ ]  GitHub Secrets에 영구 저장된 비밀 0건 (OIDC AssumeRole 외)
- [ ]  staging에서 prod prefix 접근 시 403 확인
- [ ]  연결된 Story가 모두 Done 상태다

## 산출물 (Deliverables)

- Secrets Manager 비밀 10개 (콘솔 또는 CLI 등록)
- `bootstrap.yml` (Spring Cloud AWS 설정)
- Task Role 정책 JSON (`infra/iam/task-role-policy.json` 업데이트)
- 비밀 이관 절차 Runbook (`docs/runbook/secrets-migration.md`)

## 연결된 Story 목록

- [ ]  Story 1-1. Secrets Manager 5종 비밀 등록 + Spring Boot 부팅 통합 + Task Role 권한 경계 (4 SP)

## 내부 메모 / 제약 사항

- 비밀 회전은 v2 자동화 — 현재 수동 (콘솔에서 값 수정 + ECS force-new-deployment)
- Gemini API key는 Vertex AI 인증 시 사용하지 않을 수도 있음 — Workload Identity Federation 사용 여부에 따라 분기. v1은 API key 등록만 (사용 안 해도 무방)
- DB credential은 Secrets Manager의 RDS 통합 기능으로 자동 회전 가능 (v2 — `ADR-SEC-002` 후속)
- Spring Cloud AWS 버전: `3.x` (Spring Boot 3.5 호환)

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

> 본 Epic 한정 — Secrets Manager 통합 방식·비밀 단위·placeholder 매핑의 갈림길.

### 비밀 단위 (1개 묶음 JSON vs 종류별 분리)

**Option A — 모든 비밀을 단일 JSON 묶음 (`thirdtool/prod/all-secrets`)**
- 장점: Secret 개수 적음 (월 비용 절감, 2개 secret)
- 거부 이유:
    - 1종 회전 시 전체 secret이 새 version → 다른 비밀에 영향
    - IAM 권한 경계가 비밀 종류별로 분리 불가
    - 회전 추적 (마지막 회전 일자)이 비밀별로 불가

**Option B (선택) — 종류별 별도 Secret + DB credential만 JSON 묶음**
- 비용: Secret 10개 × $0.40/월 = $4/월
- 보상: 독립 회전 · 종류별 IAM 권한 분리 가능성 · 회전 추적 명확
- DB credential JSON은 Secrets Manager의 RDS 통합이 user/pass 묶음을 요구하므로 표준 따름

### Spring Boot Secrets 로딩 라이브러리

**Option A — 자체 `ApplicationContextInitializer` + `awssdk` 직접 호출**
- 거부 이유: 표준 라이브러리가 있는데 자체 구현은 유지 비용 누적

**Option B — `aws-secretsmanager-jdbc` (JDBC URL에 비밀 주입)**
- 거부 이유: DB credential에만 적용 가능. JWT/OAuth는 별도 로딩 메커니즘 필요 → 코드 두 갈래

**Option C (선택) — `spring-cloud-aws-starter-secrets-manager` 3.x**
- 비용: Spring Cloud AWS / Spring Boot / AWS SDK 버전 호환 매트릭스 관리
- 보상: `spring.config.import: aws-secretsmanager:...` 한 줄. DB·JWT·OAuth 모두 동일 메커니즘

### bootstrap.yml vs application.yml 분리

**Option A — `application.yml`에 `spring.config.import` 직접 명시**
- 거부 이유: Spring Boot 2.4+ 에서 동작은 하지만 PropertySource ordering이 미묘. dev 프로파일에서 Secrets Manager 호출 회피가 까다로움

**Option B (선택) — `bootstrap.yml` 분리 + 프로파일별 활성**
- 비용: 파일 1개 추가
- 보상: 부팅 phase 분리 명확. dev 프로파일은 `bootstrap.yml`을 비활성/누락하여 H2 평문 placeholder 그대로 사용

### Task Role 분리 vs 단일 Role + 조건부 정책

**Option A — 단일 Task Role + `Condition: aws:ResourceTag`로 환경 분기**
- 거부 이유: ResourceTag 기반 조건은 콘솔 실수로 태그 변경 시 권한 누수. 정적 prefix 조건이 더 안전

**Option B (선택) — 환경별 Task Role 분리 (prod-task-role · staging-task-role)**
- 비용: Role 2개 관리. Terraform module.app에서 env 변수로 분기
- 보상: cross-env 접근 자동 차단. ARN prefix 조건만으로 검증 가능

---

## Story 1-1. Secrets Manager 5종 비밀 등록 + Spring Boot 부팅 통합 + Task Role 권한 경계

### User Story

> As a 백엔드 개발자,
I want 모든 비밀이 Secrets Manager 단일 진실 소스로 통합되고 Spring Boot가 부팅 시 자동 로딩하길,
So that 회전 가능한 비밀 관리 표준이 정착되고, GitHub Secrets · application.yml에 평문 비밀이 남지 않는다.
>

### 설계 노트

- Secrets Manager 비밀 명명

    | Secret Name | 형태 | 사용 위치 |
    | --- | --- | --- |
    | `thirdtool/${env}/db-credential` | `{"username":"...","password":"..."}` | `spring.datasource.username/password` |
    | `thirdtool/${env}/jwt-secret` | 평문 string | JWT 서명 비밀 |
    | `thirdtool/${env}/kakao-oauth` | `{"client-id":"...","client-secret":"..."}` | Kakao OAuth |
    | `thirdtool/${env}/naver-oauth` | `{"client-id":"...","client-secret":"..."}` | Naver OAuth |
    | `thirdtool/${env}/gemini-api-key` | 평문 string | Vertex AI API key (사용 시) |

- 의존성

    ```groovy
    implementation 'io.awspring.cloud:spring-cloud-aws-starter-secrets-manager:3.1.1'
    ```

- `bootstrap.yml`

    ```yaml
    spring:
      config:
        import: aws-secretsmanager:thirdtool/${ENV:dev}/db-credential;thirdtool/${ENV:dev}/jwt-secret;...
      cloud:
        aws:
          region:
            static: ap-northeast-2
    ```

- `application-prod.yml` placeholder 사용

    ```yaml
    spring:
      datasource:
        url: jdbc:mysql://${RDS_PROD_ENDPOINT}:3306/thirdtool
        username: ${username}  # Secrets Manager db-credential JSON 키
        password: ${password}
    jwt:
      secret: ${jwt-secret}    # 평문 string secret
    spring.security.oauth2.client.registration.kakao:
      client-id: ${client-id}  # kakao-oauth JSON 키
      client-secret: ${client-secret}
    ```

- Task Role 권한 정책 추가

    ```json
    {
      "Effect": "Allow",
      "Action": "secretsmanager:GetSecretValue",
      "Resource": "arn:aws:secretsmanager:ap-northeast-2:ACCT:secret:thirdtool/prod/*"
    }
    ```

  staging Task Role은 `thirdtool/staging/*`로 분리

### 완료 기준 (Acceptance Criteria)

- [ ]  `aws secretsmanager list-secrets`에 10개 비밀 (5종 × 2환경) 확인
- [ ]  prod ECS Task가 부팅 시 Secrets Manager에서 비밀 로딩 성공 (CloudWatch Logs 확인)
- [ ]  `application.yml`에 평문 비밀 0건 (`grep -r "secret\|password"` 결과 placeholder만)
- [ ]  GitHub Secrets에서 `DB_PASSWORD` · `JWT_SECRET` · `KAKAO_CLIENT_SECRET` · `NAVER_CLIENT_SECRET` · `GEMINI_API_KEY` 제거 확인
- [ ]  staging Task가 prod prefix 비밀 접근 시도 → 403 Forbidden (테스트로 확인)
- [ ]  Secrets Manager 값 수정 후 ECS `force-new-deployment` → 새 값 반영 (1회 검증)

### 엣지 케이스

- Secrets Manager에 비밀이 없는 상태에서 ECS Task 부팅 → Spring Boot 기동 실패 + CloudWatch Logs에 명확한 에러. 모든 비밀 사전 등록 필수
- 비밀 회전 직후 ECS Task 재기동 전까지 기존 값 사용 → 회전 후 즉시 force-new-deployment 트리거 (수동, v2 자동)
- JSON 형태 secret의 키 이름이 application.yml placeholder와 불일치 시 → 기동 실패. naming convention 표 준수 필수
- VPC Endpoint 없으면 Secrets Manager 호출이 NAT 통과 → 비용 발생. Epic 2에서 VPC Endpoint 추가

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  Secrets Manager 비밀 등록 스크린샷 (10개 list)
- [ ]  prod ECS Task 부팅 로그 (Secrets 로딩 성공)
- [ ]  staging에서 prod 비밀 접근 차단 검증
- [ ]  비밀 이관 절차 Runbook 커밋 (`docs/runbook/secrets-migration.md`)
- [ ]  PO(또는 본인) 셀프 검수 완료

### 의존성

- 선행: Product B Story 2-1 (Task Role 존재)
- 후속: Epic 2 (VPC Endpoint 추가로 Secrets Manager 호출 NAT 우회)

### 스토리 포인트

- 추정: 4 SP

---

## Epic 2. 백업·관측 통합 — RDS PITR 드릴 + S3 버전 관리 + CloudWatch Alarm 4종 + SNS

# Epic 2. 백업·관측 통합 — RDS PITR 드릴 + S3 버전 관리 + CloudWatch Alarm 4종 + SNS

## Epic 목표

> RDS PITR 7일을 실제 복구 드릴로 검증해 RTO를 측정하고, S3 버전 관리 + 라이프사이클을 적용하고, CloudWatch Alarm 4종(ECS · RDS · ALB · NAT)을 SNS 이메일 구독과 함께 활성화한다.
v2 알림 Product의 임계치 캘리브레이션이 본 Epic 위에서 작동한다.
>

## 배경

- 백업이 "있다"는 사실과 "복구 가능하다"는 사실은 다르다 — 드릴 없이 PITR을 운영 자산으로 인정할 수 없음
- S3 실수 삭제 사고는 토이 규모에서도 발생 가능 — 버전 관리는 1줄 설정으로 큰 보험
- Alarm 없이 Grafana 대시보드만 있으면 사람이 봐야 인지 — 새벽 사고 회복 지연

## 핵심 설계 결정

> **PITR 복구 드릴 1회 필수. 결과는 Runbook에 RTO 명시.**
드릴 안 한 백업은 백업이 아님.
>
> - staging-rds를 임의 시점으로 복구 → 새 인스턴스 endpoint 확인 → RTO 측정 (몇 분 걸렸나)
> - 복구된 인스턴스는 검증 후 삭제 (비용)
> - 결과 기록: `docs/runbook/rds-pitr-recovery.md`에 RTO + 절차 + 주의사항

> **CloudWatch Alarm 4종 기본 임계.**
v1 임의 추정치, baseline 캘리브레이션은 `product-load-test.md` analysis.md 결과 활용.
>
> - ECS Task CPU > 80% (5분 평균)
> - RDS DatabaseConnections > 80% of max
> - ALB HTTPCode_Target_5XX_Count > 1% (5분 윈도우)
> - NAT BytesOutToDestination > 10GB/일 (비용 이상 감지)
> - 모두 SNS topic `thirdtool-ops-alerts` 구독 → 본인 이메일

> **VPC Endpoint 추가로 NAT 비용 절감.**
Secrets Manager · ECR · S3 호출이 VPC 내부로 우회.
>
> - Gateway Endpoint: S3 (무료)
> - Interface Endpoint: Secrets Manager · ECR (시간당 $0.014 × 사용된 ENI 수)
> - 비용 비교: NAT 데이터 전송 vs VPC Endpoint 시간당 — 호출량이 일정 이상이면 Endpoint가 쌈

## 완료 기준 (Definition of Done)

- [ ]  staging-rds PITR 복구 드릴 1회 완료 + RTO 측정
- [ ]  S3 버전 관리 + 라이프사이클(30일 IA, 90일 Glacier Deep Archive) 적용 (도메인 버킷 대상)
- [ ]  CloudWatch Alarm 4종이 활성화되고 SNS 구독된다
- [ ]  VPC Endpoint 3개 (S3 · Secrets Manager · ECR) 추가
- [ ]  의도적 임계 초과 시뮬레이션 1회 → 알림 도달 시간 < 5분 검증
- [ ]  AWS Backup Service로 월간 RDS 스냅샷 (30일 보존) 자동화
- [ ]  연결된 Story가 모두 Done 상태다

## 산출물 (Deliverables)

- CloudWatch Alarm 4종 + SNS topic + 이메일 구독 (AWS CLI 적용)
- AWS Backup plan (월간 RDS 스냅샷)
- VPC Endpoint 3개
- PITR 복구 Runbook (`docs/runbook/rds-pitr-recovery.md`)
- S3 라이프사이클 정책 JSON

## 연결된 Story 목록

- [ ]  Story 2-1. RDS PITR 드릴 + S3 버전 관리 + Alarm 4종 + VPC Endpoint + AWS Backup 통합 (3 SP)

## 내부 메모 / 제약 사항

- Alarm 임계는 v1 추정치 — `product-load-test.md` analysis.md의 캘리브레이션 결과로 v2에서 갱신
- SNS Slack 통합은 v2 — v1은 이메일만
- AWS Backup Service 비용: 스냅샷 GB당 ~$0.10/월. 100GB → $10/월 가산
- VPC Endpoint Interface는 ENI 단위 — Multi-AZ subnet 배치 시 비용 2배

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

> 본 Epic 한정 — 백업 자동화·Alarm 임계 설정·VPC Endpoint 배치의 갈림길.

### 백업 자동화 도구

**Option A — EventBridge + Lambda로 직접 스냅샷 cron 구현**
- 거부 이유: Lambda 코드 유지 비용 + IAM Role + 로그 관리 부담. 표준 서비스 있는데 자체 구현 비효율

**Option B — Aurora의 backup cluster snapshot policy (RDS Aurora만)**
- 거부 이유: 현재 RDS는 MySQL (Aurora 아님). 마이그레이션 부담 정당화 어려움

**Option C (선택) — AWS Backup Service**
- 비용: 스냅샷 GB당 $0.10/월 + Vault 관리 (실질 운영 부담 0)
- 보상: cron + lifecycle + cross-account/region 확장이 한 Plan에 모두 표현 가능. IAM Role 1개

### Alarm 임계 설정 방식

**Option A — 정적 임계 (CPU > 80%, Connections > 80%)**
- 비용: false positive/negative 가능 (baseline 모름)
- v1 채택 — 시작점 필요. v2에서 캘리브레이션

**Option B — Anomaly Detection (CloudWatch ML 기반)**
- 거부 이유:
    - 학습 데이터 부족 (트래픽 0)
    - 비용: 메트릭당 $0.30/월 + Detector 운영
    - 토이 규모 정당화 어려움

**Option C — `product-load-test.md` analysis.md baseline 직접 사용**
- 합류 시점: v2. v1은 임시 임계로 시작

**Option D (선택) — v1 정적 임계 + v2 baseline 캘리브레이션**
- 비용: 첫 1주 false positive 가능성
- 보상: 즉시 도입 + 점진 개선 경로

### Alarm 발동 정책 (evaluation_periods)

**Option A — `evaluation_periods: 1` (1회 초과로 즉시 발동)**
- 거부 이유: 일시 스파이크에 false positive 폭증

**Option B (선택) — `evaluation_periods: 2` (2회 연속 초과)**
- 비용: 알람 도달 시간 5분 → 10분 지연
- 보상: 일시 스파이크 노이즈 차단

**Option C — `evaluation_periods: 3`**
- 거부 이유: 실제 장애 인지 지연 위험 증가

### VPC Endpoint 배치 (subnet AZ 수)

**Option A — 단일 AZ Interface Endpoint (비용 절감)**
- 거부 이유: 해당 AZ 장애 시 Secrets/ECR 호출 불가 → ECS Task 부팅 실패

**Option B (선택) — Multi-AZ Interface Endpoint (가용성 우선)**
- 비용: ENI 2개 × $0.014/시간 × 2 endpoint (Secrets + ECR) = ∼$40/월
- 보상: AZ 장애 격리, ECS Multi-AZ 배치와 일관

**Option C — S3는 Gateway (무료) + ECR/Secrets는 NAT 경유 (Endpoint 없음)**
- 거부 이유: NAT data transfer 비용이 Interface Endpoint 시간당 비용을 초과하는 임계가 낮음 (수 GB/월). 토이 규모 호출량으로도 Endpoint가 쌈

### S3 라이프사이클 정책

**Option A — 즉시 Deep Archive (1일 후)**
- 거부 이유: 사용자 자료 접근 빈도 모름. 복원 비용 발생 위험

**Option B (선택) — 30일 STANDARD_IA → 90일 Deep Archive → noncurrent 365일 expire**
- 비용: IA $0.0125/GB · Deep Archive $0.00099/GB
- 보상: 활성 자료는 즉시 접근, 장기 미접근은 자동 cold storage

### PITR 드릴 빈도

**Option A — 1회만 (Epic 종료 시점)**
- v1 채택. 토이 규모.

**Option B (선택, v2 권장) — 분기별 1회 자동 드릴**
- 비용: 드릴마다 인스턴스 비용 (1시간 × $0.05/h = $0.05) + 인적 시간
- 보상: PITR이 항상 작동함을 보증. RTO 추세 감시

---

## Story 2-1. RDS PITR 드릴 + S3 버전 관리 + Alarm 4종 + VPC Endpoint + AWS Backup

### User Story

> As a 백엔드 개발자,
I want 백업·복구·알람·비용 절감의 운영 안전망이 한 번에 깔리고 실제 작동 검증되길,
So that 장애가 알람으로 먼저 감지되고, 복구가 Runbook 따라 5분에 시작되며, 비용 이상이 자동 알려진다.
>

### 설계 노트

- PITR 복구 드릴 절차

    ```bash
    # 1. 현재 시각 기준 1시간 전 시점으로 복구
    aws rds restore-db-instance-to-point-in-time \
      --source-db-instance-identifier staging-rds \
      --target-db-instance-identifier staging-rds-recovery-test \
      --restore-time $(date -u -d '1 hour ago' --iso-8601=seconds)

    # 2. 새 인스턴스 available 상태까지 대기 (시작 시각 기록)
    # 3. endpoint로 접속해 데이터 검증
    # 4. RTO 측정 = 명령 실행 ~ available 시각 차
    # 5. 검증 완료 후 삭제
    aws rds delete-db-instance \
      --db-instance-identifier staging-rds-recovery-test \
      --skip-final-snapshot
    ```

  Runbook에 위 절차 + 측정된 RTO + 트러블슈팅 기록

- S3 라이프사이클 정책

    ```json
    {
      "Rules": [{
        "Status": "Enabled",
        "Transitions": [
          { "Days": 30, "StorageClass": "STANDARD_IA" },
          { "Days": 90, "StorageClass": "DEEP_ARCHIVE" }
        ],
        "NoncurrentVersionExpiration": { "NoncurrentDays": 365 }
      }]
    }
    ```

- CloudWatch Alarm 4종 (AWS CLI)

    ```bash
    # SNS 토픽 생성
    aws sns create-topic --name thirdtool-ops-alerts
    # 이메일 구독 (confirm 메일 클릭 필요)
    aws sns subscribe --topic-arn <SNS_ARN> --protocol email --notification-endpoint gim05860@gmail.com

    # ECS CPU Alarm
    aws cloudwatch put-metric-alarm \
      --alarm-name "thirdtool-prod-ecs-cpu-high" \
      --metric-name CPUUtilization --namespace AWS/ECS \
      --statistic Average --period 300 --threshold 80 \
      --comparison-operator GreaterThanThreshold \
      --evaluation-periods 2 \
      --dimensions Name=ClusterName,Value=thirdtool-prod Name=ServiceName,Value=thirdtool-app \
      --alarm-actions <SNS_ARN>

    # RDS / ALB / NAT — 동일 패턴으로 반복
    ```

- AWS Backup Plan (AWS CLI)

    ```bash
    # Backup Plan JSON (infra/backup-plan.json)
    aws backup create-backup-plan --cli-input-json file://infra/backup-plan.json
    # backup-plan.json 내용:
    # { "BackupPlan": { "BackupPlanName": "thirdtool-rds-monthly",
    #   "Rules": [{ "RuleName": "monthly", "TargetBackupVaultName": "Default",
    #     "ScheduleExpression": "cron(0 4 1 * ? *)",
    #     "Lifecycle": { "DeleteAfterDays": 30 } }] } }

    aws backup create-backup-selection \
      --backup-plan-id <PLAN_ID> \
      --backup-selection '{"SelectionName":"rds","IamRoleArn":"<BACKUP_ROLE_ARN>",
        "Resources":["<RDS_ARN>"]}'
    ```

- VPC Endpoint (AWS CLI)

    ```bash
    # S3 Gateway Endpoint (무료)
    aws ec2 create-vpc-endpoint \
      --vpc-id <VPC_ID> \
      --service-name com.amazonaws.ap-northeast-2.s3 \
      --vpc-endpoint-type Gateway \
      --route-table-ids <PRIVATE_RT_ID>

    # Secrets Manager Interface Endpoint
    aws ec2 create-vpc-endpoint \
      --vpc-id <VPC_ID> \
      --service-name com.amazonaws.ap-northeast-2.secretsmanager \
      --vpc-endpoint-type Interface \
      --subnet-ids <APP_SUBNET_A> <APP_SUBNET_C> \
      --security-group-ids <VPC_ENDPOINT_SG_ID> \
      --private-dns-enabled

    # ECR (ecr.api, ecr.dkr) 동일 패턴
    ```

### 완료 기준 (Acceptance Criteria)

- [ ]  PITR 복구 드릴 결과 Runbook에 RTO 명시 (예: 12분)
- [ ]  S3 버전 관리 활성 + 라이프사이클 적용 확인 (`aws s3api get-bucket-versioning`)
- [ ]  CloudWatch Alarm 4종 `ACTIVE` 상태 + SNS 구독 `Confirmed`
- [ ]  의도적 임계 초과 (예: staging ECS Task 부하 발생) → 이메일 알림 < 5분 도달
- [ ]  VPC Endpoint 3개 추가 후 NAT data transfer 감소 측정 (24시간 비교)
- [ ]  AWS Backup Service 월간 스냅샷 첫 실행 성공 (또는 수동 trigger로 검증)

### 엣지 케이스

- PITR 복구 인스턴스가 비용 발생 — 검증 후 즉시 삭제 절차 Runbook에 강조
- 임계값이 너무 민감하게 설정되면 false positive 폭증 → 첫 1주 운영 후 조정
- VPC Endpoint Interface는 ENI 단위라 SG 설정 필수 — 누락 시 비밀 호출 timeout
- AWS Backup IAM Role에 `rds:CreateDBSnapshot` 권한 없으면 스냅샷 실패 → IAM 정책 검증

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  PITR Runbook 커밋 + RTO 기록
- [ ]  Alarm 4종 활성 스크린샷 + 1회 알림 도달 스크린샷
- [ ]  VPC Endpoint 추가 후 NAT 비용 변화 측정 (1주 후 추가 보고)
- [ ]  AWS Backup 첫 스냅샷 성공 확인
- [ ]  PO(또는 본인) 셀프 검수 완료

### 의존성

- 선행: Epic 1 (Task Role 존재 + Secrets Manager 통합 완료)
- 후속: 없음 (Product의 마지막 Story)

### 스토리 포인트

- 추정: 3 SP

---

## Product 요약

| Epic | Story 수 | SP 합계 |
| --- | --- | --- |
| Epic 1. Secrets Manager 통합 | 1 | 4 |
| Epic 2. 백업·관측 통합 | 1 | 3 |
| **합계** | **2** | **7 SP** |

**진행 순서 (필수):** Epic 1 → 2. Secrets → 안전망.

**3개 Infra Product 전체 흐름**

```
Product A (Network)  →  Product B (Deploy)  →  Product C (Ops)
   VPC + ALB + RDS         Docker + ECS Fargate       Secrets Manager
   ↑                       ↑                          + 백업·관측
   │                       │                          ↓
   │                       └─ load-test의 staging      Alarm 임계는
   │                                                  load-test analysis.md
   └─ ADR-LOAD-002 충족                               결과로 캘리브레이션
```

**기존 운영성 Product와의 연결 포인트**
- `product-log.md`의 traceId 로그가 CloudWatch Logs awslogs로 출력 → v2 Loki 통합 시 본 Product의 awslogs driver가 입력
- `product-op.md`의 Grafana threshold 캘리브레이션 결과가 본 Product의 CloudWatch Alarm 임계로 동시 적용
- `product-load-test.md` analysis.md의 baseline 수치가 Alarm 임계의 정량 근거
- `product-auth.md`의 JWT secret · OAuth client secret이 Secrets Manager로 이관
- `product-aisuggestion.md`의 Gemini API key가 Secrets Manager로 이관, Vertex AI ADC는 Workload Identity Federation 별도 검토 (v2)

**v0 잔재 — 마이그레이션 체크리스트**
- `application-prod.yml` 평문 비밀 → Secrets Manager placeholder
- GitHub Secrets의 `DB_PASSWORD` · `JWT_SECRET` · `KAKAO_CLIENT_SECRET` · `NAVER_CLIENT_SECRET` · `GEMINI_API_KEY` → 제거
