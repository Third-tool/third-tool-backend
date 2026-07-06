## [Product] 컨테이너 배포 파이프라인 — Docker · ECR · ECS Fargate · GHA OIDC

# [Product] 컨테이너 배포 파이프라인 — Docker · ECR · ECS Fargate · GHA OIDC

## Product Vision

> Spring Boot 코드가 컨테이너 이미지로 빌드되고 ECR에 푸시되어 ECS Fargate Task로 배포되고 ALB가 무중단 라우팅하기까지의 파이프라인을 GitHub Actions OIDC 기반으로 견고화한다.
기존 EC2 + SSH/appleboy 배포는 폐기하고, IAM Task Role + Secrets Manager 호환 + autoscaling 표준 위에 dev/staging/prod 3환경을 분리해 운영한다.
배포 1건이 ALB 헬스체크 통과 → 자동 롤백 가능 → traceId 추적 가능까지의 사이클이 한 GHA 워크플로에서 완결되도록 만든다.
>

## 배경 및 문제

- 현재 상황 (As-Is)
    - `.github/workflows/dev-cicd.yml`이 main 브랜치 push에 Docker 빌드 + ECR push + EC2 SSH(`appleboy/ssh-action`) + `docker run` 재시작 흐름으로 동작
    - `Dockerfile-dev`(개발용)와 `Dockerfile-elasticsearch`(Nori 이미지) 2종이 분리되어 있고 운영 이미지 `Dockerfile`이 별도 명확하지 않음
    - 이미지 태깅이 `latest` 또는 단순 빌드 번호 추정 — git_sha 고정 태깅이 명시 안 됨
    - EC2 SSH 의존으로 GHA가 EC2의 비밀번호/키를 GitHub Secrets에 저장 → 키 회전 어렵고 사고 시 영향 큼
    - 단일 EC2 인스턴스에서 컨테이너 교체 시 다운타임 발생 (`docker stop && docker run`) → 무중단 배포 불가
    - ALB가 앞단에 없어 헬스체크 게이트 자동화 미비, 배포 실패 시 수동 SSH 롤백
    - 환경 분기가 `dev-cicd.yml` 하나에 박혀 있어 staging이 사실상 존재하지 않음
- 발생하는 문제
    - 배포 1건마다 5~30초 다운타임 → 부하 테스트 중 또는 트래픽 spike 중 배포 시 유저 신고 직결
    - EC2 인스턴스 ID·SSH 키·IP가 GitHub Secrets에 박혀 있어 EC2 교체 시 시크릿 재설정 누락 사고 가능
    - 자동 롤백 경로 없음 → 잘못된 배포가 들어가면 prod 다운 후 SSH 접속하여 이전 이미지 수동 pull
    - 이미지 태깅이 `latest`면 어떤 코드가 배포됐는지 추적 불가 — Product 0의 traceId와 git_sha 연결 단절
    - 부하 테스트 baseline 측정 중 동일 인스턴스에 배포 시 측정 오염
    - 채용 포트폴리오에서 "무중단 배포 어떻게 했나, IAM Role은 어떻게 분리했나, 자동 롤백은?"에 답변 불가
- 왜 지금 해결해야 하는가
    - Product A(네트워크)가 ALB Target Group(type=ip)을 깔았으므로 ECS Fargate 전환의 인프라 전제는 충족됨
    - Product C(IaC + Secrets Manager)가 ECS Task의 IAM Task Role을 전제로 함 — 본 Product가 먼저
    - EC2 SSH 의존은 한 번 굳어지면 떼기 어려움 — 트래픽이 더 늘기 전에 ECS 전환이 가장 쌈
    - Spring AI Vertex AI Gemini 인증(GCP ADC)이 ECS Task의 IAM Role + Workload Identity Federation으로만 깔끔하게 풀림 — EC2 SSH로는 비밀 관리 부담 증가
    - 면접·포트폴리오 단골 질문 (배포 패턴 / OIDC / IAM Role 분리 / 자동 롤백)에 정량 답변 가능해짐

## 목표 (To-Be)

- 단일 `Dockerfile` 통합 (multi-stage / JDK 21 / Distroless 검토) + 빌드 빠른 캐시 + 이미지 < 200MB
- ECR 라이프사이클 정책 적용 (최근 10개 + untagged 7일 후 삭제) + 이미지 스캔(enhanced)
- ECS Fargate Cluster 2개 (prod / staging) + Task Definition + Service + Task Role + Execution Role
- ALB Target Group에 ECS Service가 자동 등록 + 헬스체크 통과 후 배포 완료
- GHA OIDC AssumeRole 인증 (IAM 사용자 키 0건) → `aws-actions/amazon-ecs-deploy-task-definition`로 배포
- 자동 롤백 (배포 후 5분 내 5xx > 1% 또는 헬스체크 실패 시 이전 Task Definition revision으로 복귀)
- dev(로컬 docker-compose) / staging(`develop` branch 자동) / prod(`main` branch 자동) 분리
- 배포 1건당 다운타임 0초 (Rolling Update + connection draining)
- 모든 이미지 태그가 `git_sha`로 고정 — `latest` 사용 0건

## 설계 결정 (Design Decision)

> **이미지 태그는 `git_sha` 7자 prefix를 필수로 한다. `latest` 태그는 사용하지 않는다.**
배포된 이미지 ↔ 코드 commit 추적성을 절대 끊지 않음.
>
> - 태그 예: `thirdtool:8f3c4a2`, `thirdtool:8f3c4a2-staging`
> - `latest`는 ECR에 푸시 자체를 막음 (라이프사이클 정책)
> - 이유: 운영 사고 시 어떤 commit이 배포됐는지 즉시 확인 가능 → Product 0의 traceId와 git_sha를 cross-reference 가능
> - Task Definition에도 `:latest` 사용 금지 — 매 배포마다 새 revision
> - 이 결정은 ADR로 별도 기록한다 (`ADR-DEPLOY-001: Image Tagging — git_sha Required`)

> **EC2 SSH 배포를 폐기하고 ECS Fargate로 전환한다.**
SSH 의존 제거 + IAM Task Role 도입 + autoscaling 표준화의 3종 동시 효과.
>
> - 단점: EC2 대비 ECS Fargate는 시간당 ~30% 비싸지만 autoscaling 표준 · IAM Task Role · SSH 제거의 운영 가치가 큼
> - FARGATE_SPOT 50% 혼합으로 비용 절감 (interruption 시 ALB가 정상 Task로 라우팅)
> - 단점: 컨테이너 SSH 디버깅 불가 → CloudWatch Logs Insights · ECS Exec(SSM)로 대체
> - 이 결정은 ADR로 별도 기록한다 (`ADR-DEPLOY-002: EC2 → ECS Fargate Migration`)

> **배포 패턴은 ECS Rolling Update (v1). CodeDeploy Blue/Green은 v2.**
운영 학습 곡선 vs 즉시 적용성의 절충.
>
> - Rolling Update: `minimumHealthyPercent=100`, `maximumPercent=200` — 신규 Task 떠서 헬스체크 통과 후 기존 Task 종료
> - Blue/Green (CodeDeploy): 별도 Target Group 2개 + traffic shift 자동화 → v1에는 인프라 복잡도 증가
> - Rolling으로도 무중단 배포 충족 + 자동 롤백은 이전 Task Definition revision으로 단순 복귀
> - 이 결정은 ADR로 별도 기록한다 (`ADR-DEPLOY-003: Rolling Update v1 / Blue/Green v2`)

> **GHA → AWS 인증은 OIDC AssumeRole. IAM 사용자 access key는 0건.**
시크릿 영구 회전 가능 상태.
>
> - GHA가 OIDC ID token 발급 → AWS IAM Role(`gha-deploy-role`)이 `sts:AssumeRoleWithWebIdentity` 신뢰 → 임시 자격증명 발급
> - GitHub Secrets에 `AWS_ACCESS_KEY_ID` · `AWS_SECRET_ACCESS_KEY` 저장 0건
> - IAM Role 신뢰 정책에 `repo:<owner>/<repo>:ref:refs/heads/main` 같은 조건으로 권한 경계
> - 이 결정은 ADR로 별도 기록한다 (`ADR-DEPLOY-004: GHA OIDC AssumeRole`)

> **FARGATE 100% prod / FARGATE_SPOT 50%+FARGATE 50% staging.**
비용 절감과 가용성의 환경별 비대칭.
>
> - prod: FARGATE 100% — 안정성 우선, SPOT interruption 회피
> - staging: FARGATE_SPOT 50% — 비용 70% 절감, interruption 시 부하 테스트가 잠시 영향 받는 건 수용 가능
> - dev (로컬 docker-compose) — Fargate 사용 X
> - 이 결정은 ADR로 별도 기록한다 (`ADR-DEPLOY-005: FARGATE_SPOT Mix Strategy`)

## 대안 검토 (Alternatives Considered)

> 큰 갈림길마다 "왜 이것이 아니고 저것인가"를 남긴다. 거부된 안에도 합리적 근거가 있었음을 보임으로써 현재 선택의 트레이드오프를 명확히 한다.

### 컨테이너 런타임 (Compute Substrate)

**Option A — EC2 + docker run (현 상태 유지)**
- 장점: 시간당 비용 최저, SSH 디버깅 친숙, 신규 학습 곡선 0
- 거부 이유:
    - SSH 키가 GitHub Secrets에 박혀 회전 불가 (사실상 영구 자격증명)
    - 단일 EC2의 `docker stop && docker run` 패턴은 다운타임 5~30초 — 무중단 배포 원천 불가
    - 자동 롤백 경로가 없어 잘못된 배포 후 SSH 접속 → `docker pull <prev>` → 수동 재시작 (MTTR 분 단위)
    - autoscaling을 EC2 ASG로 직접 짜는 비용 vs ECS Service 기본 기능

**Option B — EKS (Kubernetes)**
- 장점: 업계 표준, 멀티 클러스터·서비스 메시·HPA·CronJob 등 풍부한 컴포넌트
- 거부 이유:
    - 1인 운영 + 트래픽 0명 규모에 컨트롤 플레인 비용($73/월) + EKS 학습 곡선이 과도
    - YAML 매니페스트·Helm 차트 도입은 PRD 본문의 "운영 단순화" 방향과 충돌
    - 향후 트래픽·도메인 복잡도 증가 시 ECS → EKS 전환 path 자체는 막혀있지 않음 (별 Product)

**Option C (선택) — ECS Fargate**
- 비용: 시간당 EC2 대비 ~30% 비싸지만 FARGATE_SPOT 혼합으로 staging 비용 70% 절감 가능
- 보상: 서버리스 컨테이너 (호스트 OS 패치·SSH 키 관리 0건) + IAM Task Role 표준 + ALB Target Group 자동 등록 + Deployment Circuit Breaker 기본 제공
- 트레이드오프 수용 근거: 채용 포트폴리오·운영 표준 가치 > 시간당 비용 절감

### 컨테이너 레지스트리

**Option A — Docker Hub**
- 거부 이유: pull rate limit (익명 100/6h) + 이미지 스캐닝 기본 미포함 + IAM 통합 부재 → ECS Task가 pull할 때 Docker Hub 자격증명을 Secrets Manager로 한 번 더 우회 필요

**Option B — GitHub Container Registry (GHCR)**
- 장점: GHA와 자연스러운 통합, 무료 (public)
- 거부 이유: ECS Task가 GHCR pull 시 Personal Access Token 또는 GITHUB_TOKEN 필요 → IAM Task Execution Role의 깔끔한 ECR pull 권한 (관리형 정책)이 더 단순. AWS 동일 리전 pull로 ECR 비용은 사실상 무료에 가까움 (스토리지만)

**Option C (선택) — Amazon ECR**
- 비용: 스토리지 $0.10/GB·월 — 라이프사이클 정책으로 통제
- 보상: IAM 관리형 정책 1개로 Task Execution Role pull 권한 끝남, enhanced scanning · 라이프사이클 정책 · cross-region replication 기본 제공
- 트레이드오프 수용 근거: AWS 단일 클라우드 가정에서 GHCR 대비 운영 마찰이 훨씬 적음

### 이미지 태깅 전략

**Option A — `latest` 단일 태그**
- 거부 이유: 어느 commit이 배포됐는지 추적 불가 → Product 0의 traceId와 git_sha 연결 단절. 롤백 시 "이전 latest"가 존재하지 않음

**Option B — semver (`v1.2.3`)**
- 거부 이유: 1인 운영에서 매 main push마다 semver bump는 과도. 자동 bump 도구 도입 비용. hotfix·revert 시 버전 관리 복잡

**Option C (선택) — git_sha 7자 prefix 필수, `latest` 금지**
- 비용: ECR 태그 카운트 증가 → 라이프사이클 정책으로 10개 유지로 관리
- 보상: 이미지 ↔ commit 1:1 추적, 롤백은 "직전 revision" 또는 "특정 sha"로 명확, traceId/X-Build-Sha 응답 헤더와 cross-reference 가능

### Dockerfile 베이스 (런타임 이미지)

**Option A — `eclipse-temurin:21-jdk-alpine`** (Dockerfile-dev 현 상태)
- 거부 이유: JDK 전체 포함 (~400MB) → 운영 이미지에 컴파일러·jshell 등 불필요 도구. 공격 표면 ↑

**Option B — `eclipse-temurin:21-jre-alpine`**
- 장점: JRE only, ~150MB
- 거부 이유: shell + apk 패키지 매니저 존재 → 컨테이너 침해 시 임의 명령 실행 가능. glibc 미지원으로 일부 native 라이브러리 호환성 이슈 (Spring AI 등)

**Option C (선택) — `gcr.io/distroless/java21-debian12:nonroot`**
- 비용: shell 없음 → `docker exec sh` 디버깅 불가, ECS Exec + 별도 debug 이미지로 우회 필요
- 보상: ~150MB + shell·apk·apt 부재 → 침해 시 공격 표면 최소 + nonroot 사용자 기본
- 트레이드오프 수용 근거: 보안 표면 축소 + 이미지 크기 동시 만족. 디버깅은 CloudWatch Logs Insights + ECS Exec debug 이미지(v2)로 대체

### GHA → AWS 인증

**Option A — IAM 사용자 Access Key (현 상태)**
- 거부 이유:
    - GitHub Secrets에 영구 자격증명이 박혀 회전 비용 (수동 절차) + 유출 시 폭발 반경 큼
    - 키 회전 시 GHA workflow 다운타임 또는 dual-key 운영 필요
    - 권한 경계가 IAM User 단위로만 가능 → branch별 권한 분리 불가

**Option B (선택) — GHA OIDC AssumeRole**
- 비용: OIDC Provider 등록 + IAM Role 신뢰 정책 작성의 초기 비용
- 보상:
    - GitHub Secrets에 AWS 자격증명 0건
    - 임시 자격증명 TTL 1시간 → 유출 폭발 반경 ↓
    - 신뢰 정책의 `sub` 조건으로 `refs/heads/main` 한정 → prod 배포는 main에서만 (fork PR 자동 차단)

### 배포 트리거

**Option A — Manual approval (Production)**
- 거부 이유: 1인 운영에서 매 배포마다 수동 승인은 마찰. 신뢰 정책 + branch protection으로 자동화 가능

**Option B — Tag push (`v*`)**
- 거부 이유: semver 운영을 강제하지 않기로 한 결정과 충돌. tag push까지의 추가 한 단계 마찰

**Option C (선택) — Branch push 기반 자동 트리거**
- 비용: 잘못된 main 머지가 즉시 prod로 흐를 위험 → branch protection + PR + CI status check로 게이트
- 보상: develop → staging / main → prod의 인지 부담이 가장 낮음. concurrency group으로 동시 머지 직렬화

### 배포 패턴 (Service Update Strategy)

**Option A — Recreate (downtime)**
- 거부 이유: 다운타임 0초 목표와 충돌

**Option B — Rolling Update (선택, v1)**
- 비용: 신·구 Task 동시 실행 기간(<5분) 발생 → DB 마이그레이션 forward-only 필수
- 보상: ECS 기본 기능, `minimumHealthyPercent=100` + `maximumPercent=200`으로 다운타임 0초 보장, 별도 인프라 추가 없음

**Option C — Blue/Green (CodeDeploy, v2 보류)**
- 보류 이유: Target Group 2개 + Listener Rule shift + CodeDeploy AppSpec 등 초기 구성 비용. Rolling으로도 무중단 충족하므로 v1에는 과잉

**Option D — Canary (v3 보류)**
- 보류 이유: 트래픽 splitting과 메트릭 기반 자동 promote 인프라(Application Signals / CloudWatch Synthetics)가 트래픽 0명 단계에서 불필요

### 헬스체크 조합

**Option A — ALB Target Group health check만**
- 거부 이유: ALB는 컨테이너 상태만 봄 → 앱이 기동 중 DB 연결 못해도 "200 OK 빈 응답"이면 healthy로 판정 가능

**Option B (선택) — ALB TG + Spring Actuator `/actuator/health` + ECS container `healthCheck`**
- 비용: Actuator health endpoint 노출 + ECS container healthCheck 명령 정의의 약간의 boilerplate
- 보상: 3중 게이트 — ECS는 Task healthCheck 실패 시 자동 재시작, ALB는 unhealthy Task로 트래픽 안 보냄, Spring은 DB/Redis liveness까지 검증

### 로그 수집 (Logging Sink)

**Option A — Firelens + Fluent Bit → S3 / OpenSearch**
- 보류 이유: v1 트래픽 규모에서 추가 컨테이너 사이드카(Fluent Bit) 비용 정당화 어려움. CloudWatch Logs로 시작하고 v2에서 OpenSearch / Loki 통합

**Option B (선택) — awslogs driver → CloudWatch Logs**
- 비용: CloudWatch Logs 수집 비용 $0.50/GB (수집) + $0.03/GB·월 (저장)
- 보상: Task Definition에 logConfiguration 한 블록으로 끝남, Product 0의 JSON 로그가 그대로 흐르고 CloudWatch Logs Insights로 쿼리, IAM Task Execution Role의 관리형 정책으로 권한 끝남

## 전체 아키텍처 (High-Level Architecture)

> 컨테이너 빌드부터 사용자 요청 처리까지의 전체 파이프라인. 다이어그램 한 장이 본문 5페이지보다 강하다.

### CI/CD 파이프라인 단계

```
[개발자] git push origin main
    │
    ▼
┌───────────────────────────────────────────────────────────────┐
│  GitHub Actions: deploy-prod.yml (branch=main 트리거)         │
│                                                               │
│  1. checkout                                                  │
│  2. configure-aws-credentials@v4 (OIDC AssumeRole)           │
│        └─► STS AssumeRoleWithWebIdentity                     │
│            └─► gha-deploy-role-prod (TTL 1h)                 │
│  3. amazon-ecr-login@v2                                       │
│  4. docker build -t $ECR/thirdtool-app:${SHA::7} .           │
│  5. docker push (ECR)                                         │
│  6. amazon-ecs-render-task-definition                         │
│        └─► task-definition-prod.json + new image tag         │
│  7. amazon-ecs-deploy-task-definition (wait-for-stability)   │
│        └─► ECS UpdateService                                  │
│  8. monitor-5xx.sh (5분 폴링, ALB 5xx > 1% 검증)              │
│  9. if failure → aws ecs update-service --task-definition    │
│                    <previous-revision> (auto rollback)        │
└───────────────────────────────────────────────────────────────┘
```

### 런타임 토폴로지

```
                     [사용자 브라우저]
                            │
                            │  HTTPS (443)
                            ▼
                   ┌──────────────────┐
                   │      Route53     │  api.thirdtool.dev / staging.thirdtool.dev  (BE 전용 서브도메인)
                   └────────┬─────────┘   ※ thirdtool.dev(apex) → CloudFront(FE): product-fe-cdn.md
                            ▼
                   ┌──────────────────┐
                   │       ALB        │  TLS termination
                   │  (Multi-AZ)      │  Listener :443
                   └────────┬─────────┘
                            │
                            ▼
                   ┌──────────────────┐
                   │  Target Group    │  type=ip, healthCheck=/actuator/health
                   │  (Product A)     │
                   └────────┬─────────┘
                            │ (자동 등록 by ECS Service.loadBalancers)
                            ▼
        ┌───────────────────────────────────────────┐
        │  ECS Service "thirdtool-app"              │
        │  - desiredCount=2 (prod) / 1 (staging)    │
        │  - launchType=FARGATE / FARGATE_SPOT mix  │
        │  - deploymentConfiguration                │
        │      minimumHealthyPercent=100            │
        │      maximumPercent=200                   │
        │      circuitBreaker { enable, rollback }  │
        │  - enableExecuteCommand=true              │
        └─────┬──────────────────────────┬──────────┘
              │                          │
              ▼                          ▼
       ┌──────────────┐           ┌──────────────┐
       │ Fargate Task │           │ Fargate Task │
       │  AZ: 2a      │           │  AZ: 2c      │
       │  awsvpc ENI  │           │  awsvpc ENI  │
       │              │           │              │
       │  Container:  │           │  Container:  │
       │   thirdtool  │           │   thirdtool  │
       │   :git_sha   │           │   :git_sha   │
       │              │           │              │
       │  TaskRole ──────────┐    │              │
       └──────┬───────┘      │    └──────┬───────┘
              │              │           │
              ▼              ▼           ▼
       ┌──────────────┐  ┌──────────┐  ┌──────────┐
       │ CloudWatch   │  │   RDS    │  │   S3     │
       │ Logs         │  │  MySQL   │  │  bucket  │
       │ /ecs/...     │  │ (Multi-  │  │          │
       │              │  │   AZ)    │  │          │
       └──────────────┘  └──────────┘  └──────────┘
                            ▲
                            │  Secrets Manager (Product C)
                            │  thirdtool/prod/* (DB · JWT · OAuth · Gemini)
                            │  ↑ TaskRole secretsmanager:GetSecretValue
```

### IAM Role 경계

```
┌─────────────────────────────────────────────────────────────┐
│ GHA OIDC Provider (token.actions.githubusercontent.com)     │
└───────────────┬─────────────────────────────────────────────┘
                │  AssumeRoleWithWebIdentity
                │  Condition: sub=repo:org/repo:ref:refs/heads/main
                ▼
       ┌────────────────────┐
       │ gha-deploy-role    │  ◄── GHA workflow가 사용 (TTL 1h)
       │  - ecr:*           │
       │  - ecs:Update*     │
       │  - iam:PassRole    │
       │     (Task/Exec Role)│
       └────────────────────┘
                │  PassRole
                ▼
   ┌────────────────────────────────────────┐
   │ ECS Task launch                         │
   └────────┬───────────────────┬───────────┘
            │                   │
            ▼                   ▼
   ┌──────────────────┐  ┌──────────────────┐
   │ Execution Role   │  │   Task Role      │
   │ (ECS Agent용)    │  │ (앱 코드용)      │
   │                  │  │                  │
   │ - ecr:Get*       │  │ - s3:*           │
   │ - logs:*         │  │   (특정 버킷)    │
   │ - secrets:Get*   │  │ - secretsmanager:│
   │   (envFrom용)    │  │   GetSecretValue │
   │                  │  │   (특정 prefix)  │
   └──────────────────┘  └──────────────────┘
            ▲                   ▲
            │                   │
       (ECS 에이전트만)   (컨테이너 코드만)
```

### 핵심 플로우

**1. 정상 배포 (main push → prod)**
```
git push main ─► GHA(OIDC) ─► ECR push ─► ECS UpdateService
                                              │
                                              ▼
                                      [Deployment 시작]
                                      신규 Task 2개 기동
                                      ALB HC PENDING → HEALTHY
                                              │
                                              ▼
                                      maximumPercent=200 한계까지
                                      신·구 Task 공존 (~2분)
                                              │
                                              ▼
                                      구 Task drain (connection draining 30s)
                                              │
                                              ▼
                                      STEADY_STATE 도달
                                      GHA wait-for-service-stability 통과
                                              │
                                              ▼
                                      monitor-5xx.sh 5분 폴링
                                              │
                                              ▼
                                      ✅ 배포 완료
```

**2. 자동 롤백 (헬스체크 실패)**
```
신규 Task 기동 → ALB HC 실패 반복 → ECS Deployment Circuit Breaker 발동
                                          │
                                          ▼
                                  rollback=true 설정으로
                                  자동으로 직전 Task Definition revision으로 복귀
                                          │
                                          ▼
                                  GHA wait-for-service-stability 실패
                                          │
                                          ▼
                                  GHA workflow failure로 종료
                                  (이미 ECS가 롤백 처리함)
```

**3. 5xx 폭증 롤백 (애플리케이션 레벨 실패)**
```
배포는 STEADY_STATE 도달 (Circuit Breaker는 통과)
        │
        ▼
GHA monitor-5xx.sh 1분 단위 5회 폴링
        │
        ▼
ALB 5xx > 1% 검출 → exit 1
        │
        ▼
GHA `if: failure()` step 발동
        │
        ▼
aws ecs describe-task-definition → 직전 revision 산출
aws ecs update-service --task-definition <prev>
        │
        ▼
ECS가 다시 Rolling 배포로 이전 코드 복귀
```

### Out-of-Process 의존

- **AWS STS**: GHA OIDC AssumeRole의 STS endpoint
- **Amazon ECR**: 이미지 저장소 (`thirdtool-app`, `thirdtool-elasticsearch`)
- **Amazon ECS**: Cluster · Service · Task Definition · Deployment Circuit Breaker
- **CloudWatch Logs**: awslogs driver의 sink. Log group `/ecs/thirdtool-{env}`
- **CloudWatch Metrics**: ALB 5xx, ECS Service CPU/Memory — 자동 롤백 폴링 대상
- **ALB Target Group** (Product A): ECS Service의 `loadBalancers`가 자동 등록
- **Secrets Manager** (Product C): Task Role이 `secretsmanager:GetSecretValue`로 읽음
- **GCP Workload Identity Federation** (Product C, AI 연계): Task Role이 IdP로 동작해 GCP 자격증명 교환 (Vertex AI Gemini)

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 배포 단계별 실패 시나리오와 대응

| 단계 | 실패 시나리오 | 감지 | 자동 대응 | 운영자 권장 동작 |
| --- | --- | --- | --- | --- |
| GHA checkout/build | Gradle 빌드 실패 (테스트·컴파일) | GHA failure | 없음 (배포 미진행) | PR로 되돌리기 + 재시도 |
| OIDC AssumeRole | sub 조건 불일치 (fork PR · 잘못된 branch) | STS AccessDenied | 없음 (의도된 차단) | 정상. 권한이 의도된 경계 |
| OIDC AssumeRole | IAM Role 신뢰 정책 오설정 | STS AccessDenied | 없음 | 신뢰 정책 점검 |
| ECR push | 권한 부족 / 네트워크 오류 | docker push 실패 | 없음 | gha-deploy-role의 `ecr:*` 점검 |
| Render Task Def | 이미지 placeholder 치환 실패 | action 실패 | 없음 | task-definition JSON의 container-name 일치 확인 |
| ECS UpdateService | iam:PassRole 권한 부족 | API 오류 | 없음 | gha-deploy-role에 PassRole 권한 추가 |
| Task 기동 | 이미지 pull 실패 (ECR 권한) | ECS Event `CannotPullContainer` | Circuit Breaker 롤백 | Task Execution Role의 ECR pull 권한 점검 |
| Task 기동 | OOM Killed (Memory 부족) | ECS Event `OutOfMemoryError` | Task 자동 재시작 + Circuit Breaker 발동 | Task Definition memory ↑ 또는 앱 메모리 누수 조사 |
| ALB Health Check | `/actuator/health` 500 (DB 연결 실패 등) | Target unhealthy | Circuit Breaker 롤백 | Secrets Manager · RDS 보안그룹 점검 |
| ALB Health Check | Task 기동이 늦어 HC grace period 초과 | Target unhealthy | Circuit Breaker 롤백 | Task Definition healthCheckGracePeriodSeconds ↑ |
| 배포 후 트래픽 | 5xx 폭증 (애플리케이션 레벨 버그) | monitor-5xx.sh 검출 | GHA failure step이 ecs update-service로 직전 revision 복귀 | 직전 revision의 commit 확인 + 디버깅 |
| FARGATE_SPOT | Interruption 2분 사전 알림 (staging 한정) | ECS Event `SpotInterruption` | ALB drain + 신규 Task 기동 | 정상. 비용 절감의 대가로 수용 |
| ECR 라이프사이클 | 라이프사이클 정책으로 사용 중 이미지 삭제 | Task 재시작 시 pull 실패 | 없음 | 라이프사이클 룰이 "최근 10개"이므로 사실상 발생 어려움. 발생 시 즉시 빌드 후 새 push |
| OIDC token 만료 | 배포가 1시간 초과 (사실상 없음) | AssumeRole 거부 | 없음 | wait-for-minutes 단축 또는 재시도 |
| 동시 배포 | 두 PR이 main에 거의 동시 머지 | 두 GHA workflow가 동시 실행 | concurrency group으로 직렬화 (cancel-in-progress=false) | 정상 |
| DB 마이그레이션 비호환 | Flyway forward-only 위반 (롤백 시점에 Task가 구 스키마 기대) | Task 기동 시 Hibernate validation 실패 | Circuit Breaker 롤백되어도 신규 스키마는 남아있음 | Flyway 정책 준수 — backward-incompatible 마이그레이션 금지 |

### 로깅 정책

- **항상 기록 (INFO+)**:
    - ECS Event Stream (Task 생애주기 — RUNNING/STOPPED/STOPPING + 사유) → CloudWatch Logs
    - 애플리케이션 INFO 로그 (Product 0의 JSON 한 줄, traceId 포함)
- **WARN/ERROR**: 헬스체크 실패, OOM, Secret 접근 거부, DB 연결 실패
- **절대 금지**:
    - Task Definition 환경변수에 secret 평문 (반드시 `secrets` 블록 + Secrets Manager 참조)
    - ECR 이미지 push 시 자격증명 echo (GHA 로그에 노출)
    - `enableExecuteCommand` ON 상태에서 prod 컨테이너에 인터랙티브 진입한 명령 이력 (audit 검토)

### 관측 지표 (v1)

| 지표 | 출처 | 목적 |
| --- | --- | --- |
| 배포 lead time (commit→prod) | GHA workflow duration | 배포 자동화 효율 |
| 배포 성공률 | GHA workflow success rate | 배포 신뢰도 |
| 자동 롤백 발생 횟수 | GHA `if: failure()` step 실행 카운트 | 배포 품질 — 0에 수렴해야 함 |
| MTTR (배포 실패→자동 회복) | GHA failure → previous revision STEADY_STATE까지 | 회복 속도 |
| ECS Service CPU/Memory Utilization | CloudWatch Container Insights | autoscaling 임계 캘리브레이션 (v2) |
| ALB 5xxCount / RequestCount | CloudWatch Metrics | 배포 직후 5분 폴링 대상 + 일상 모니터링 |
| 이미지 크기 추이 | ECR ImageSizeInBytes | < 200MB 유지 검증 |
| ECR storage 사용량 | ECR Metrics | 라이프사이클 효율 (10개 유지 검증) |
| Fargate vCPU·Memory 비용 | Cost Explorer | FARGATE_SPOT 혼합의 절감액 검증 |
| FARGATE_SPOT interruption 빈도 | ECS Events | staging 안정성 — 너무 잦으면 SPOT 비율 ↓ |

### 알림 트리거 (v2 — Product C에서 구체화)

- 배포 자동 롤백 발생 → Slack 즉시
- ECS Service `RUNNING_COUNT < desiredCount` 5분 지속 → Slack
- ALB 5xx > 1% 5분 → Slack + PagerDuty (prod만)
- Fargate 월 비용이 예산 초과 예측 → Slack

## 롤아웃 / 마이그레이션 (Rollout)

### 전제 — 프로덕션 트래픽 0명

현재 사용자 0명, EC2 기반 dev-cicd만 운영 중. 따라서 **EC2 → ECS 병행 운영 기간을 두지 않고 일괄 전환** 가능. 만약 사용자가 있었다면:
- 1주차: Route53 weighted routing으로 ECS:EC2 = 10:90 → ECS 안정성 관측
- 2주차: 50:50 → 부하 분포 검증
- 3주차: 100:0 → EC2 인스턴스 종료

이 단계 전환을 생략한 것은 **수용 가능한 단순화 선택**이며, 향후 트래픽 발생 후 동일한 점진 전환이 필요할 때 위 3단계 패턴을 재사용한다.

### Epic 의존성 그래프

```
Epic 1 (Docker 이미지·ECR 표준화)
  Story 1-1 (단일 Dockerfile + ECR 라이프사이클)
        │
        │  단일 ECR 이미지 + git_sha 태그
        ▼
Epic 2 (ECS Fargate 런타임)                  Product A Epic 2 (ALB Target Group)
  Story 2-1 (Cluster + Task Def + Service ◄──────────┘
             + IAM Role 3종 + CloudWatch Logs)
        │
        │  ECS Service 운영, ALB HC 통과
        ▼
Epic 3 (GHA OIDC → ECS 무중단 배포)
  Story 3-1 (OIDC AssumeRole + ECS Deploy
             + Circuit Breaker 롤백
             + 5분 5xx 모니터링)
        │
        │  prod 배포 자동화 완성
        ▼
Epic 4 (환경 분리)
  Story 4-1 (워크플로 3개 + docker-compose dev
             + branch protection
             + staging 부하 테스트 1회)
        │
        └─► Product C (Secrets Manager 통합)
            └─► product-load-test baseline 측정
```

### 배포 전략 단계 (Story 3-1 실제 배포 흐름)

1. **Dry-run 배포 (Story 2-1 직후)**: `aws ecs update-service --task-definition thirdtool-prod:1`을 수동으로 1회 실행 — Task Definition·Service 정합성 확인. 트래픽은 ALB가 신규 Task로 라우팅.
2. **Circuit Breaker 시뮬레이션**: 의도적으로 `/actuator/health`가 500 반환하는 이미지를 push → ECS Circuit Breaker가 자동 롤백하는지 검증 + 스크린샷.
3. **5xx 폴링 시뮬레이션**: 의도적으로 5xx를 반환하는 endpoint 추가 → monitor-5xx.sh가 GHA failure 발생 → 자동 롤백 검증.
4. **운영 전환**: 위 3단계 통과 후 main push로 정상 배포 1회. EC2 인스턴스 종료 (또는 ALB Target Group에서 deregister).

### 환경별 설정 분기

- **dev (로컬)**: `docker compose up` + `SPRING_PROFILES_ACTIVE=dev` + H2 in-memory + 로컬 OAuth (`localhost:5173`)
- **staging**: `deploy-staging.yml` (develop push) + Task Definition `staging` + RDS single-AZ + Secrets Manager `thirdtool/staging/*` + FARGATE_SPOT 50%
- **prod**: `deploy-prod.yml` (main push) + Task Definition `prod` + RDS Multi-AZ + Secrets Manager `thirdtool/prod/*` + FARGATE 100% + desiredCount=2 (Multi-AZ 분산)

### 롤백 절차

| 케이스 | 자동 / 수동 | 명령 |
| --- | --- | --- |
| 헬스체크 실패 | 자동 (ECS Circuit Breaker) | 없음 — ECS가 직전 revision으로 복귀 |
| 5xx 폭증 | 자동 (GHA `if: failure()`) | `aws ecs update-service --task-definition <prev>` |
| 운영 중 사후 발견 | 수동 | `aws ecs update-service --cluster thirdtool-prod --service thirdtool-app --task-definition thirdtool-prod:<N>` |
| DB 마이그레이션 비호환 | 수동 | 원칙적으로 발생 안 함 (Flyway forward-only). 발생 시 hotfix 새 마이그레이션으로 forward 회복 |

### 폐기 자산 마이그레이션 체크리스트

- `.github/workflows/dev-cicd.yml` (SSH/appleboy) → 제거 (Story 4-1 DoD)
- `Dockerfile-dev` → 통합 또는 명시적 "로컬 전용" 주석 (Story 1-1)
- `Dockerfile-elasticsearch` → `thirdtool-elasticsearch` ECR 리포 빌드용으로 분리 (Story 1-1)
- GitHub Secrets `AWS_ACCESS_KEY_ID` · `AWS_SECRET_ACCESS_KEY` → OIDC 전환 후 제거 (Story 3-1)
- GitHub Secrets `EC2_HOST` · `EC2_USERNAME` · `EC2_PRIVATE_KEY` → 제거
- EC2 인스턴스 종료 (ECS prod 안정화 후 1주 보류 → 종료)

## 성공 지표 (KPI)

| 지표 | 현재 값 | 목표 값 | 측정 방법 |
| --- | --- | --- | --- |
| 배포 1건당 다운타임 | 5~30초 | 0초 | 배포 중 `/actuator/health` 폴링 200 OK 비율 |
| `latest` 태그 ECR 푸시 건수 | 미상 | 0 | ECR 라이프사이클 정책 + GHA workflow grep |
| GitHub Secrets에 저장된 AWS access key | 2 (`AWS_ACCESS_KEY_ID` · `AWS_SECRET_ACCESS_KEY`) | 0 | GitHub repo settings 검사 |
| 자동 롤백 발동 가능 여부 | 불가능 | 가능 (이전 Task Definition revision으로 복귀) | 의도적 실패 배포 시뮬레이션 |
| 환경별 GHA 워크플로 분리 | 1 (`dev-cicd.yml`) | 3 (dev / staging / prod) | `.github/workflows/` 디렉토리 |
| ECS Service `desiredCount` (prod) | — | 2 (Multi-AZ 분산) | `aws ecs describe-services` |
| FARGATE_SPOT 비율 (staging) | — | 50% | Task Definition capacity provider strategy |
| 이미지 크기 | 미상 | < 200MB | `docker images` |
| SSH 배포 발생 건수 (Story B-3 완료 후) | — | 0 | GHA workflow grep |

## Scope

- **In Scope**
    - 단일 `Dockerfile` (multi-stage JDK 21 → Distroless or Alpine) — `Dockerfile-dev` / `Dockerfile-elasticsearch` 정리
    - `.dockerignore` 정비 (빌드 컨텍스트 < 1MB)
    - ECR 리포 2개 (`thirdtool-app`, `thirdtool-elasticsearch`) + 라이프사이클 정책 + enhanced scanning
    - ECS Cluster 2개 (`thirdtool-prod`, `thirdtool-staging`) + Service + Task Definition
    - IAM 3종: GHA Deploy Role (OIDC) · Task Execution Role · Task Role
    - GHA OIDC Provider 설정 + 신뢰 정책 (branch별 권한 경계)
    - GHA 워크플로 3개: `deploy-prod.yml`(main push) · `deploy-staging.yml`(develop push) · `image-build.yml`(공통 빌드 + ECR push)
    - 자동 롤백 (CloudWatch Alarm + ECS Service auto-revert 또는 GHA 후속 step)
    - CloudWatch Logs awslogs driver (`/ecs/thirdtool-prod`, `/ecs/thirdtool-staging`)
    - 배포 가이드 README + 롤백 Runbook
- **Out of Scope**
    - Terraform 코드화 — Product C Epic C-2
    - Secrets Manager 통합 — Product C Epic C-1 (단, Task Role에 `secretsmanager:GetSecretValue` 권한 미리 부여)
    - FE 정적 호스팅 (S3 + CloudFront + GHA FE CI/CD) → **product-fe-cdn.md**
    - WAF · Shield — v2
    - CodeDeploy Blue/Green — v2 (`ADR-DEPLOY-003`)
    - ECS Service Auto Scaling (CPU/Memory/Custom Metric 기반) — v2 (`desiredCount` 고정으로 시작)
    - 다중 리전 active-active — v3
    - ECR Replication (다중 리전) — v2
    - 컨테이너 런타임 보안 (Falco · Sysdig) — v2

## 대상 사용자

- 주요 사용자: ThirdTool 백엔드 개발자 (1인 운영)
- 사용 맥락:
    - `main` 브랜치 push 후 → 자동 prod 배포 + ALB 헬스체크 통과 알림 (Slack 또는 GHA 결과)
    - `develop` 브랜치 push 후 → staging 자동 배포 + 부하 테스트 가능 상태
    - 운영 사고 발생 시 → GHA로 이전 Task Definition revision 자동 롤백 또는 `aws ecs update-service` 수동 명령
    - 신규 기능 회귀 확인 시 → `git_sha`로 배포 시점·코드 1:1 매칭
    - 면접·포트폴리오 → "OIDC + ECS Rolling + 자동 롤백 + git_sha 태깅"이라는 4단 답변

## 연결된 Epic 목록

- [ ]  Epic 1. Docker 이미지·ECR 표준화 — 단일 Dockerfile · git_sha 태깅 · 라이프사이클
- [ ]  Epic 2. ECS Fargate 런타임 — Task Definition · Service · IAM Role 3종 · CloudWatch Logs
- [ ]  Epic 3. GHA OIDC → ECS 무중단 배포 — OIDC AssumeRole · Rolling Update · 자동 롤백
- [ ]  Epic 4. 환경 분리 — dev(로컬) · staging(develop) · prod(main) + 워크플로 3개

## 관련 문서

- 상위 문서: ThirdTool 백엔드 컨벤션 · 빌드 가이드
- 선행 Product:
    - `product-infra-network.md` (Product A) — ALB Target Group · subnet · SG · RDS endpoint
- 후속 Product:
    - `product-infra-ops.md` (Product C) — Secrets Manager 통합 · CloudWatch Alarm · 백업 안전망
    - `product-fe-cdn.md` — FE 정적 자산 배포 파이프라인. GHA OIDC + S3 sync + CloudFront invalidation (gha-deploy-role에 FE 권한 추가됨)
- 영향 받는 Product:
    - `product-load-test.md` — staging 환경 자동 배포로 baseline 측정 시 동일 commit 보장
    - `product-op.md` — ECS Service / Task CPU/Memory 메트릭이 Grafana 대시보드에 추가됨
    - `product-log.md` — CloudWatch Logs awslogs driver로 출력 → Product C에서 Loki 통합 검토
    - `product-aisuggestion.md` — Vertex AI Gemini ADC 인증이 Task Role + GCP Workload Identity Federation 패턴으로 정착
- 참고 자료: AWS ECS Best Practices · GHA OIDC with AWS · ECR Lifecycle Policy
- ADR 후보: `ADR-DEPLOY-001~005`

## 열린 질문 (Open Questions)

> 이 Product 범위에서 결론을 내리지 않고 v2 이후로 미루는 질문들. 트래픽·운영 상황이 답을 결정하면 ADR로 격상한다.

### 멀티 리전 / DR
- **Q1.** 단일 리전(ap-northeast-2) 가용성으로 충분한가, ap-northeast-1 (Tokyo) DR 리전이 필요한가?
    - 보류 사유: 트래픽 0명. RTO/RPO 목표가 정해지면 결정. ECR Replication + Route53 health-based failover 패턴 후보
- **Q2.** ECR cross-region replication을 미리 깔아둘 것인가, DR 결정 시점에 일괄 도입할 것인가?

### 자동 롤백 임계 캘리브레이션
- **Q3.** 5xx > 1% / 5분 폴링 임계가 실제 부하 패턴에서 적정한가? (false positive · false negative 데이터 부재)
    - 보류 사유: `product-load-test.md` baseline 측정 후 캘리브레이션
- **Q4.** Latency 기반 자동 롤백도 추가할 것인가? (예: p99 > 1000ms 5분)
    - 보류 사유: CloudWatch ApplicationSignals · X-Ray 통합 비용. v2에서 검토

### Blue/Green vs Canary
- **Q5.** Rolling → Blue/Green (CodeDeploy) 승격 시점은 언제인가?
    - 트리거 후보: DAU > 1000명, 배포 실패 영향이 사용자 단위로 확장될 때
- **Q6.** Canary (트래픽 5% → 50% → 100%) 도입 시점은? Application Signals 기반 자동 promote 인프라 구축 의지?

### 비용 최적화
- **Q7.** prod에도 FARGATE_SPOT을 일부(예: 30%) 도입할 것인가?
    - 보류 사유: SPOT interruption 2분 알림에 대한 graceful shutdown 핸들러 검증 필요
- **Q8.** ARM (Graviton, `cpu_architecture=ARM64`)으로 전환 시 ~20% 비용 절감 가능한가? Spring Boot · Spring AI 의존성 호환성 검증 비용은?
- **Q9.** Fargate 1 vCPU / 2GB는 보수적 추정. CloudWatch Container Insights로 실측 후 right-sizing은 언제?

### Autoscaling
- **Q10.** ECS Service Auto Scaling을 어떤 메트릭(CPU / Memory / Custom Metric / ALB RequestCountPerTarget)으로 깔 것인가?
    - 보류 사유: 트래픽 패턴이 안정화돼야 임계값 결정 가능
- **Q11.** Scheduled Scaling (출근 시간대만 desiredCount ↑)이 1인 운영 + B2C 트래픽에 의미가 있는가?

### 보안 강화 (v2+)
- **Q12.** ECR Repository Policy로 `latest` 태그 push 자체를 거부할 것인가? (GHA workflow grep만으로는 우회 가능)
- **Q13.** 컨테이너 런타임 보안 (Falco · Sysdig · GuardDuty for ECS) 도입 시점?
- **Q14.** ECS Exec 활성 상태(`enableExecuteCommand=true`)를 prod에서 유지할 것인가, 디버깅 필요 시에만 일시 활성화할 것인가?

### 관측성 확장
- **Q15.** CloudWatch Logs → Loki / OpenSearch 통합은 언제? (`product-log.md` 연계)
- **Q16.** ECS Service Auto Scaling 도입 시 어느 CloudWatch Dashboard에 통합할 것인가? (`product-op.md` Grafana 연계)
- **Q17.** 배포 lead time · MTTR을 DORA Metric으로 정착시킬 것인가? 측정 도구는?

### Spring AI / GCP 인증
- **Q18.** GCP Workload Identity Federation의 audience · subject mapping이 ECS Task Metadata 엔드포인트와 충돌 없이 동작하는가? (Product C에서 구체 검증)

---

| Epic | Story 수 | SP 합계 |
| --- | --- | --- |
| Epic 1. Docker 이미지·ECR 표준화 | 1 | 3 |
| Epic 2. ECS Fargate 런타임 | 1 | 5 |
| Epic 3. GHA OIDC → ECS 무중단 배포 | 1 | 5 |
| Epic 4. 환경 분리 | 1 | 4 |
| **합계** | **4** | **17 SP** |

**진행 순서 (필수):** Epic 1 → 2 → 3 → 4. 이미지 표준 → ECS 런타임 → 배포 자동화 → 환경 분리.

**ECS 전환으로 SP 증가**: EC2 SSH 패턴 유지 시 ~12 SP였으나, IAM Role 3종 · OIDC 설정 · Task Definition 작성으로 +5 SP.

---

## Epic 1. Docker 이미지·ECR 표준화 — 단일 Dockerfile · git_sha 태깅 · 라이프사이클

# Epic 1. Docker 이미지·ECR 표준화 — 단일 Dockerfile · git_sha 태깅 · 라이프사이클

## Epic 목표

> 운영 이미지 빌드의 단일 진실 소스를 `Dockerfile`(루트) 한 파일로 만들고, `Dockerfile-dev` · `Dockerfile-elasticsearch`의 역할을 명시 정리한다.
ECR에 푸시되는 모든 이미지가 `git_sha` 태그를 가지며, 라이프사이클 정책으로 최근 10개만 유지된다.
>

## 배경

- 운영 이미지가 어느 Dockerfile에서 빌드되는지 명확하지 않음 → 신규 합류자가 빌드 재현에 시간 소모
- `latest` 태그가 ECR에 푸시되고 있다면 어느 commit이 배포됐는지 추적 불가
- ECR 무제한 누적은 비용 + 검색 비효율 — 라이프사이클 정책 필수

## 핵심 설계 결정

> **Multi-stage Dockerfile + Distroless 최종 이미지를 채택한다.**
빌드와 런타임 이미지 분리 + 보안 표면 최소화.
>
> - Stage 1: `eclipse-temurin:21-jdk-alpine` — Gradle 빌드
> - Stage 2: `gcr.io/distroless/java21-debian12:nonroot` — 런타임만
> - 이유: Distroless는 shell · package manager 없음 → 컨테이너 침해 시 공격 표면 ↓. 이미지 크기 ↓ (~150MB)
> - 단점: 컨테이너 내부 디버깅 불가 (shell 없음) → ECS Exec로 별도 디버그 이미지 사용

> **ECR 라이프사이클: 최근 10개 + untagged 7일 후 삭제.**
스토리지 비용 + 룩업 속도.
>
> - tagged 이미지 11번째부터 expire → 약 1주일 분량 유지
> - untagged (실패한 빌드 등) 7일 후 삭제
> - 라이프사이클 위반: `latest` 같은 mutable 태그 푸시 자체를 금지 (Repository policy로 차단 가능)

## 완료 기준 (Definition of Done)

- [ ]  루트 `Dockerfile`이 단일 운영 이미지 빌드용으로 정의된다 (multi-stage)
- [ ]  `Dockerfile-dev`는 로컬 개발 전용으로 명시되거나 제거된다
- [ ]  `Dockerfile-elasticsearch`는 별도 ECR 리포(`thirdtool-elasticsearch`)에 명시 분리된다
- [ ]  ECR 라이프사이클 정책이 두 리포에 적용된다
- [ ]  ECR enhanced scanning이 활성화된다
- [ ]  이미지 크기 < 200MB
- [ ]  `.dockerignore` 정비 (빌드 컨텍스트 < 1MB)
- [ ]  연결된 Story가 모두 Done 상태다

## 산출물 (Deliverables)

- 단일 `Dockerfile` (루트)
- `.dockerignore`
- ECR 리포 2개 + 라이프사이클 정책 JSON
- 빌드 가이드 README 섹션

## 연결된 Story 목록

- [ ]  Story 1-1. 단일 Dockerfile 통합 + `.dockerignore` + ECR 라이프사이클 + 빌드 가이드 (3 SP)

## 내부 메모 / 제약 사항

- Distroless 채택 시 JVM agent (`-javaagent`)가 정상 동작하는지 사전 검증 (Prometheus JMX Exporter 등)
- Alpine 베이스도 후보지만 glibc 미지원 → Spring AI의 일부 native 의존성에서 문제 가능
- 이미지 푸시 GHA action: `docker/build-push-action@v5` 채택

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

### Q. Dockerfile 분리 vs 통합

**Option A — `Dockerfile-dev` / `Dockerfile-prod` / `Dockerfile-elasticsearch` 3종 유지**
- 거부 이유: 동일 jar를 빌드하는데 베이스 이미지·JVM 옵션만 다른 두 Dockerfile이 분리되면 의존성 업데이트 시 동기화 누락 가능. 빌드 가이드 분기.

**Option B (선택) — 단일 루트 `Dockerfile` (운영) + Elasticsearch는 별도 ECR 리포 빌드용 Dockerfile**
- 비용: 로컬 빠른 빌드(테스트 skip)는 docker-compose의 `build:` + build arg로 분기 필요
- 보상: 운영 이미지의 단일 진실 소스가 명확. 신규 합류자가 "이게 그 이미지"라고 즉시 식별

### Q. Gradle 의존성 캐시 전략

**Option A — Docker layer caching만**
- 거부 이유: GHA 캐시 실패 시 매 빌드마다 의존성 재다운로드 → 빌드 시간 ~3분 → ~6분

**Option B — GHA actions/cache + `~/.gradle/caches`**
- 거부 이유: cache key를 build.gradle 해시로 잡아야 정합 — 도입 비용 작지만 캐시 invalidation 정책 명시 필요

**Option C (선택) — `docker/build-push-action@v5`의 cache-from/cache-to + GHA cache backend**
- 비용: 첫 빌드는 캐시 미스로 ~5분
- 보상: layer 캐시 + Gradle 캐시가 동일 메커니즘으로 관리. 캐시 hit 시 ~30초

### Q. 빌드 컨텍스트 축소

**Option A — `.dockerignore` 없음 / 모든 파일 포함**
- 거부 이유: `workflow/`, `docs/`, `monitoring/`, `load-test/` 등이 빌드 컨텍스트로 들어가 ~50MB → push 시간 ↑ + Docker daemon 부담

**Option B (선택) — `.dockerignore` 정비 (< 1MB)**
- 비용: 새 디렉토리 추가 시 ignore 룰 갱신 필요
- 보상: 빌드 컨텍스트 < 1MB → 빌드 시작 즉시 (~100ms)

### Q. ECR 라이프사이클 보존 정책

**Option A — `latest` 무제한 보존**
- 거부 이유: `latest` 자체를 사용 안 하기로 결정 (`ADR-DEPLOY-001`)

**Option B — 모든 태그 90일 보존**
- 거부 이유: 90일 = ~수백 개 이미지 → 스토리지 비용 + 검색 비효율. 90일 전 이미지로 롤백할 일은 사실상 없음

**Option C (선택) — tagged 10개 + untagged 7일**
- 비용: 11번째 오래된 이미지 expire → 그 sha로 롤백 불가
- 보상: ~1주일 분량 유지 (1일 평균 1~3회 배포 기준). 스토리지 비용 < $1/월

### Q. 이미지 스캐닝

**Option A — Basic scanning (무료)**
- 거부 이유: OS 패키지 CVE만 검출, 애플리케이션 라이브러리(Log4Shell 등) 미검출

**Option B (선택) — Enhanced scanning (Inspector V2)**
- 비용: 이미지당 $0.09 + 추가 OS 라이브러리 스캔
- 보상: 애플리케이션 라이브러리(JAR 내부 dependency) + OS + 컨테이너 런타임 통합 스캔. CRITICAL 발견 시 GHA fail 게이트 가능

---

## Story 1-1. 단일 Dockerfile 통합 + `.dockerignore` + ECR 라이프사이클 + 빌드 가이드

### User Story

> As a 백엔드 개발자,
I want 운영 이미지가 루트 `Dockerfile` 한 파일에서 빌드되고 ECR에 `git_sha` 태그로 푸시되며 오래된 이미지가 자동 정리되길,
So that 빌드 재현성·보안·비용이 한 번에 표준화되고 어느 commit이 배포됐는지가 항상 명확하다.
>

### 설계 노트

- 단일 `Dockerfile`

    ```dockerfile
    # syntax=docker/dockerfile:1.7
    FROM eclipse-temurin:21-jdk-alpine AS builder
    WORKDIR /workspace
    COPY gradle gradle
    COPY gradlew settings.gradle build.gradle ./
    RUN ./gradlew --no-daemon dependencies
    COPY src src
    RUN ./gradlew --no-daemon bootJar -x test

    FROM gcr.io/distroless/java21-debian12:nonroot
    WORKDIR /app
    COPY --from=builder /workspace/build/libs/*.jar app.jar
    EXPOSE 8080
    USER nonroot:nonroot
    ENTRYPOINT ["java", "-jar", "/app/app.jar"]
    ```

- `.dockerignore`

    ```
    .git
    .gradle
    build
    out
    *.md
    workflow/
    docs/
    monitoring/
    load-test/
    ```

- ECR 라이프사이클 정책 (`thirdtool-app`)

    ```json
    {
      "rules": [
        {
          "rulePriority": 1,
          "selection": {
            "tagStatus": "untagged",
            "countType": "sinceImagePushed",
            "countUnit": "days",
            "countNumber": 7
          },
          "action": { "type": "expire" }
        },
        {
          "rulePriority": 2,
          "selection": {
            "tagStatus": "tagged",
            "tagPrefixList": [""],
            "countType": "imageCountMoreThan",
            "countNumber": 10
          },
          "action": { "type": "expire" }
        }
      ]
    }
    ```

- 빌드 명령

    ```bash
    docker build -t <ecr-uri>/thirdtool-app:${GITHUB_SHA::7} .
    docker push <ecr-uri>/thirdtool-app:${GITHUB_SHA::7}
    ```

### 완료 기준 (Acceptance Criteria)

- [ ]  `docker build -t thirdtool:test .` 가 빌드 컨텍스트 < 1MB + 최종 이미지 < 200MB로 완료된다
- [ ]  빌드된 이미지에 shell이 없다 (`docker run --rm thirdtool:test sh` 실패)
- [ ]  ECR push 시 태그가 `git_sha` 7자 prefix를 포함한다 (예: `8f3c4a2`)
- [ ]  ECR 라이프사이클 정책이 두 리포에 적용되어 있다 (`aws ecr describe-repository-policy`)
- [ ]  ECR enhanced scanning 활성화 + CRITICAL 발견 0건 (또는 명시 허용)
- [ ]  `Dockerfile-dev`가 제거되거나 명시적으로 "로컬 전용" 주석 추가
- [ ]  `Dockerfile-elasticsearch`는 `thirdtool-elasticsearch` ECR 리포 빌드용으로 분리 명시
- [ ]  README의 "빌드 & 푸시" 섹션이 갱신된다

### 엣지 케이스

- Distroless에 shell이 없어 `docker exec` 디버깅 불가 → ECS Exec (SSM) 또는 별도 디버그 이미지(`distroless/java21-debug`) 임시 사용 명시
- GitHub Actions의 `GITHUB_SHA`가 commit SHA → 빌드/배포 시 동일 SHA 보장. 머지 커밋(`pull_request`) vs head 커밋(`push`) 차이 정리
- `latest` 태그 푸시 시도가 발생 → 라이프사이클은 차단 못 함 (정책상). 워크플로 grep으로 차단 + ECR Repository Policy로 `PutImage`에 `aws:RequestTag/Source: github-actions` 강제 가능 (v2)

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  빌드 시간 측정 (캐시 hit 시 ~30초, full ~3분 권장)
- [ ]  이미지 크기 스크린샷
- [ ]  ECR 라이프사이클 정책 JSON 커밋 (`infra/ecr/lifecycle-policy.json`)
- [ ]  보안 스캔 결과 첨부
- [ ]  PO(또는 본인) 셀프 검수 완료

### 의존성

- 선행: 없음 (Product의 시작 Story)
- 후속: Story 2-1 (Task Definition이 본 이미지를 참조)

### 스토리 포인트

- 추정: 3 SP

---

## Epic 2. ECS Fargate 런타임 — Task Definition · Service · IAM Role 3종 · CloudWatch Logs

# Epic 2. ECS Fargate 런타임 — Task Definition · Service · IAM Role 3종 · CloudWatch Logs

## Epic 목표

> ECS Cluster 2개(prod / staging)에 Task Definition + Service를 배치하고, IAM Role 3종(GHA Deploy Role · Task Execution Role · Task Role)을 최소 권한으로 분리한다.
ALB Target Group에 ECS Service가 자동 등록되어 헬스체크 통과 후 트래픽이 흐르고, CloudWatch Logs로 awslogs driver 표준 출력된다.
>

## 배경

- Epic 1로 이미지가 ECR에 있으나 그것을 띄울 컴퓨트가 없음 — ECS Fargate가 본 Epic
- IAM Role을 잘못 분리하면 보안 사고 직결: Task Role이 너무 강하면 컨테이너 침해 시 AWS 계정 전체 침해 가능
- ALB Target Group 등록은 ECS Service의 `loadBalancers` 설정으로 자동 — 수동 등록 불요

## 핵심 설계 결정

> **IAM Role 3종을 명확히 분리한다.**
권한 경계의 핵심.
>
> - **GHA Deploy Role** (`gha-deploy-role`): GHA OIDC AssumeRole 대상. `ecs:UpdateService` · `ecr:GetAuthorizationToken` · `iam:PassRole(Task Role)` 권한
> - **Task Execution Role** (`ecs-task-execution-role`): ECS 에이전트가 사용. ECR pull · CloudWatch Logs 쓰기. **컨테이너 내부에서는 사용 불가**
> - **Task Role** (`ecs-task-role`): 컨테이너 애플리케이션 코드가 사용. S3 · Secrets Manager · 그 외 도메인 권한. **Execution Role과 별개**
> - 분리 안 하면 컨테이너 코드가 ECR/CloudWatch에 접근 가능 → 침해 시 권한 확대

> **Task Definition은 환경별로 별도. 단일 정의로 환경 분기하지 않는다.**
prod와 staging의 vCPU/Memory/desiredCount 차이가 너무 크기 때문.
>
> - prod: 1 vCPU / 2GB Memory / desiredCount=2
> - staging: 0.5 vCPU / 1GB Memory / desiredCount=1
> - Task Definition 파일도 분리: `infra/ecs/task-definition-prod.json`, `task-definition-staging.json`

> **CloudWatch Logs awslogs driver를 기본 로깅으로 한다.**
Product 0의 JSON 로그가 awslogs로 흘러감.
>
> - Log group: `/ecs/thirdtool-prod`, `/ecs/thirdtool-staging`
> - 보존: prod 30일 / staging 7일 (Product C에서 정책 명시)
> - Product 0의 logstash JSON 출력이 그대로 CloudWatch Logs에 들어감 → 쿼리는 CloudWatch Logs Insights 또는 v2 Loki 통합

## 완료 기준 (Definition of Done)

- [ ]  ECS Cluster 2개(`thirdtool-prod`, `thirdtool-staging`)가 생성된다
- [ ]  Task Definition 2개가 환경별로 정의되고 ECR 이미지 + IAM Role 참조가 정합한다
- [ ]  ECS Service 2개가 ALB Target Group에 자동 등록되며 헬스체크 통과한다
- [ ]  IAM Role 3종이 최소 권한으로 분리되고 신뢰 정책이 정합한다
- [ ]  CloudWatch Log group 2개가 생성되고 컨테이너 로그가 stdout으로 출력된다
- [ ]  prod desiredCount=2 (Multi-AZ 분산) / staging desiredCount=1
- [ ]  연결된 Story가 모두 Done 상태다

## 산출물 (Deliverables)

- ECS Cluster 2개 · Service 2개 · Task Definition JSON 2개 (`infra/ecs/`)
- IAM Role 3종 + 정책 JSON (`infra/iam/`)
- CloudWatch Log group 2개
- ECS 흐름도 다이어그램 (`docs/architecture/ecs-topology.md`)

## 연결된 Story 목록

- [ ]  Story 2-1. ECS Cluster + Task Definition + Service + IAM Role 3종 + CloudWatch Logs 구성 (5 SP)

## 내부 메모 / 제약 사항

- Fargate Platform Version: `LATEST` 명시 (1.4.0+) — 이전 버전은 EFS · ECS Exec 미지원
- Task Role에 `secretsmanager:GetSecretValue` 권한 미리 부여 (Product C에서 사용)
- Task Role에 GCP Workload Identity Federation을 위한 `sts:AssumeRoleWithWebIdentity` 형태 권한 (Spring AI Gemini용) 검토 — Product C에서 구체화
- ECS Service의 `enableExecuteCommand=true`로 ECS Exec 활성화 (Distroless 디버깅 대안)

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

### Q. Task Definition 환경 분기 방식

**Option A — 단일 Task Definition JSON + 환경변수 분기**
- 거부 이유: vCPU/Memory/desiredCount 등 환경별 차이가 환경변수로 표현 불가. 단일 JSON 강제 시 `placeholder` 치환 로직이 복잡

**Option B (선택) — 환경별 별도 JSON (`task-definition-prod.json` / `task-definition-staging.json`)**
- 비용: 두 파일 동기화 부담 (공통 필드 drift 가능)
- 보상: 환경 차이가 한눈에 보임. PR review 시 prod 변경만 골라서 검토 가능

### Q. IAM 권한 부여 단위

**Option A — Task Execution Role과 Task Role을 통합 (단일 Role)**
- 거부 이유: ECR pull · CloudWatch Logs 쓰기 권한이 컨테이너 코드에도 노출 → 침해 시 권한 확대

**Option B — Task Role 하나에 모든 권한 (Execution Role 미사용)**
- 거부 이유: ECS 에이전트가 Task Role을 사용할 수 없음 (AWS 아키텍처 제약)

**Option C (선택) — Execution / Task / GHA Deploy 3종 분리**
- 비용: IAM Role · 신뢰 정책 3개 관리
- 보상: 권한 경계가 명확. 침해 시 폭발 반경 한정

### Q. 컨테이너 헬스체크 정의 위치

**Option A — ALB Target Group `/actuator/health`만**
- 거부 이유: ECS는 Task가 healthy인지 모르고 단순 RUNNING으로 판단 → 앱이 죽어도 ECS는 재시작 안 함

**Option B — ECS container `healthCheck` (`curl -f http://localhost:8080/actuator/health || exit 1`)만**
- 거부 이유: ALB가 healthy로 보면 트래픽 라우팅. 컨테이너 healthCheck 실패 → ECS가 재시작하지만 그 사이 ALB는 1~2회 unhealthy 트래픽 라우팅 가능

**Option C (선택) — 3중 게이트: ALB TG + ECS container healthCheck + Spring Actuator HealthIndicator (DB·Redis liveness)**
- 비용: 헬스체크 비용 (1초마다 3주체가 polling) → 미미
- 보상: 각 레이어가 독립적으로 unhealthy 판정 → ALB drain → ECS 재시작 → 자동 복구. Spring HealthIndicator로 DB 연결 실패까지 검출

### Q. FARGATE_SPOT 혼합 비율 (staging)

**Option A — FARGATE 100% (안정성 우선)**
- 거부 이유: staging은 부하 테스트·검증용 → 비용이 prod 대비 합리적이어야 함. interruption은 부하 테스트 측에서 견딜 수 있음

**Option B — FARGATE_SPOT 100% (비용 최저)**
- 거부 이유: 부하 테스트 baseline 측정 중 동시에 interruption 발생 시 baseline 오염. 최소한의 안정성 보장 필요

**Option C (선택) — FARGATE 50% + FARGATE_SPOT 50%**
- 비용: 평균 35% 절감 (SPOT 70% 할인 × 50%)
- 보상: 한 Task는 interruption 시에도 다른 정상 Task가 받쳐줌

### Q. Multi-AZ 분산 강제 방식

**Option A — placementConstraints `distinctInstance`**
- 거부 이유: Fargate는 instance 개념이 없음 → 적용 불가

**Option B (선택) — Service `availabilityZoneRebalancing` 활성 + subnets에 두 AZ 모두 지정**
- 비용: 두 AZ subnet 사전 구성 (Product A에서 완료)
- 보상: ECS Scheduler가 AZ 균등 분산을 자동 수행. AZ 장애 시 1대는 생존

### Q. 로그 보존 정책

**Option A — prod / staging 모두 30일**
- 거부 이유: staging은 부하 테스트 로그가 폭증 가능 → 비용 부담

**Option B (선택) — prod 30일 / staging 7일**
- 비용: staging에서 1주일 전 로그 분석 불가
- 보상: staging 로그 비용 4배 절감. 디버깅은 발생 즉시 분석하는 패턴이 더 효과적

---

## Story 2-1. ECS Cluster + Task Definition + Service + IAM Role 3종 + CloudWatch Logs 구성

### User Story

> As a 백엔드 개발자,
I want ECS Fargate Cluster 2개에 환경별 Task Definition · Service가 배치되고 IAM Role이 최소 권한으로 분리되길,
So that 컨테이너가 ALB 뒤에서 무중단 라우팅되고, 보안 침해 시 권한 경계가 명확하다.
>

### 설계 노트

- ECS Cluster 2개

    ```bash
    aws ecs create-cluster --cluster-name thirdtool-prod \
      --capacity-providers FARGATE FARGATE_SPOT \
      --default-capacity-provider-strategy capacityProvider=FARGATE,weight=100

    aws ecs create-cluster --cluster-name thirdtool-staging \
      --capacity-providers FARGATE FARGATE_SPOT \
      --default-capacity-provider-strategy \
        capacityProvider=FARGATE,weight=50 \
        capacityProvider=FARGATE_SPOT,weight=50
    ```

- Task Definition (`infra/ecs/task-definition-prod.json` 일부)

    ```json
    {
      "family": "thirdtool-prod",
      "requiresCompatibilities": ["FARGATE"],
      "networkMode": "awsvpc",
      "cpu": "1024",
      "memory": "2048",
      "executionRoleArn": "arn:aws:iam::ACCT:role/ecs-task-execution-role",
      "taskRoleArn": "arn:aws:iam::ACCT:role/ecs-task-role",
      "containerDefinitions": [{
        "name": "app",
        "image": "<ECR_URI>/thirdtool-app:PLACEHOLDER",
        "portMappings": [{ "containerPort": 8080, "protocol": "tcp" }],
        "logConfiguration": {
          "logDriver": "awslogs",
          "options": {
            "awslogs-group": "/ecs/thirdtool-prod",
            "awslogs-region": "ap-northeast-2",
            "awslogs-stream-prefix": "app"
          }
        },
        "environment": [
          { "name": "SPRING_PROFILES_ACTIVE", "value": "prod" }
        ],
        "secrets": [
          // Product C에서 채워짐 - DB · JWT · OAuth · Gemini
        ]
      }]
    }
    ```

- ECS Service (`infra/ecs/service-prod.json` 일부)

    ```json
    {
      "cluster": "thirdtool-prod",
      "serviceName": "thirdtool-app",
      "taskDefinition": "thirdtool-prod",
      "desiredCount": 2,
      "launchType": "FARGATE",
      "platformVersion": "LATEST",
      "networkConfiguration": {
        "awsvpcConfiguration": {
          "subnets": ["<app-2a>", "<app-2c>"],
          "securityGroups": ["<app-sg>"],
          "assignPublicIp": "DISABLED"
        }
      },
      "loadBalancers": [{
        "targetGroupArn": "<prod-tg-arn>",
        "containerName": "app",
        "containerPort": 8080
      }],
      "deploymentConfiguration": {
        "minimumHealthyPercent": 100,
        "maximumPercent": 200
      },
      "enableExecuteCommand": true
    }
    ```

- IAM Role 3종 신뢰 정책 (요약)

    | Role | 신뢰 주체 | 핵심 권한 |
    | --- | --- | --- |
    | `gha-deploy-role` | GHA OIDC Provider (`token.actions.githubusercontent.com`) | `ecs:UpdateService` · `ecr:*` · `iam:PassRole` (Task/Execution) |
    | `ecs-task-execution-role` | `ecs-tasks.amazonaws.com` | `AmazonECSTaskExecutionRolePolicy` (관리형) — ECR pull · Logs 쓰기 |
    | `ecs-task-role` | `ecs-tasks.amazonaws.com` | `s3:*` (특정 버킷) · `secretsmanager:GetSecretValue` (특정 secret prefix) |

### 완료 기준 (Acceptance Criteria)

- [ ]  `aws ecs describe-clusters --clusters thirdtool-prod thirdtool-staging`이 두 Cluster 정상 응답
- [ ]  Task Definition 2개가 `ACTIVE` 상태로 등록된다
- [ ]  ECS Service가 ALB Target Group에 자동 등록되고 헬스체크 통과 (`healthy` 상태) 후 트래픽이 흐른다
- [ ]  `curl https://thirdtool.dev/actuator/health`가 ECS Task에서 응답
- [ ]  prod에서 Task 2개가 ap-northeast-2a / 2c에 각각 분산된다 (`describe-tasks` availabilityZone)
- [ ]  IAM Role 3종이 분리 생성되고 신뢰 정책이 정합 (cross-role 권한 없음)
- [ ]  CloudWatch Log group에 컨테이너 로그가 stdout으로 출력된다 (Product 0의 JSON 한 줄 포맷)
- [ ]  ECS Exec (`aws ecs execute-command --cluster ... --task ... --interactive --command "/bin/sh"`)이 활성 (단, Distroless라 shell 없음 — debug 이미지로 분리 가능 v2)

### 엣지 케이스

- ALB 헬스체크 통과 전 desiredCount=2 신규 Task가 모두 unhealthy면 → ECS Service가 새 deployment를 `STEADY_STATE` 도달 못함 → 자동 롤백 (Story 3-1)
- Task가 OOM Killed 시 ECS가 자동 재시작 + CloudWatch Logs에 흔적 남음 → `MemoryUtilization` 메트릭으로 추적
- FARGATE_SPOT interruption 알림(2분 사전) → ALB가 해당 Task를 deregister 후 새 Task로 라우팅. staging만 영향
- Task Role 권한 부족 시 → S3/Secrets 호출 403 → CloudWatch Logs에 명확한 에러. 권한 추가 후 Task 재시작 (immutable)
- 신규 Task Definition revision으로 update할 때 desiredCount=2가 충족되기 전 기존 Task 종료되면 다운타임 → minimumHealthyPercent=100 강제로 방지

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  Task Definition / Service JSON 코드베이스 커밋 (`infra/ecs/`)
- [ ]  IAM Role 신뢰 정책 + 권한 정책 JSON 커밋 (`infra/iam/`)
- [ ]  ECS 흐름도 다이어그램 커밋 (`docs/architecture/ecs-topology.md`)
- [ ]  `aws ecs describe-services` 응답 스크린샷
- [ ]  ALB Target Group `healthy` 상태 스크린샷
- [ ]  PO(또는 본인) 셀프 검수 완료

### 의존성

- 선행: Story 1-1 (이미지 ECR push), Product A Story 2-1 (ALB Target Group)
- 후속: Story 3-1 (GHA에서 본 Service에 deploy)

### 스토리 포인트

- 추정: 5 SP

---

## Epic 3. GHA OIDC → ECS 무중단 배포 — OIDC AssumeRole · Rolling Update · 자동 롤백

# Epic 3. GHA OIDC → ECS 무중단 배포 — OIDC AssumeRole · Rolling Update · 자동 롤백

## Epic 목표

> GHA가 OIDC ID token으로 IAM Role을 AssumeRole하여 ECS Service를 무중단 업데이트하고, 배포 실패(헬스체크 미통과 또는 5분 내 5xx 폭증) 시 이전 Task Definition revision으로 자동 롤백되는 파이프라인을 만든다.
>

## 배경

- 기존 `dev-cicd.yml`의 `appleboy/ssh-action` 패턴은 SSH 키 영구 의존 + 다운타임 + 롤백 수동 — 모든 면에서 폐기 대상
- ECS Rolling Update는 minimumHealthyPercent · maximumPercent · 헬스체크 통과 기준으로 무중단 보장
- 자동 롤백 없이는 잘못된 배포가 prod에 들어가는 순간 수동 개입 필요 — 새벽에 사고 발생 시 회복 지연

## 핵심 설계 결정

> **GHA → AWS 인증은 OIDC AssumeRole 단독 채택.**
IAM 사용자 access key 사용 전면 폐기.
>
> - `aws-actions/configure-aws-credentials@v4` + `role-to-assume` + `audience`
> - 신뢰 정책의 `Condition`에 `token.actions.githubusercontent.com:sub: repo:<org>/<repo>:ref:refs/heads/main` 명시 → prod 배포는 main에서만
> - staging은 `refs/heads/develop` 별도 IAM Role
> - 단점: GitHub Enterprise Server 등 OIDC 미지원 환경 사용 불가 → GitHub.com 사용 가정

> **자동 롤백은 ECS Deployment Circuit Breaker 활성.**
별도 CodeDeploy 도입 없이 ECS 기본 기능 활용.
>
> - `deploymentConfiguration.deploymentCircuitBreaker.enable=true`, `rollback=true`
> - 배포 중 ECS Task가 STEADY_STATE 도달 실패 시 자동으로 이전 Task Definition revision으로 복귀
> - 단점: HTTP 5xx 폭증 같은 애플리케이션 레벨 실패는 감지 못함 → CloudWatch Alarm + GHA 후속 step으로 보강

> **배포 후 5분간 5xx > 1% 발생 시 GHA가 추가 롤백.**
ECS Circuit Breaker가 못 잡는 실패를 보완.
>
> - GHA의 마지막 step: 배포 완료 후 5분 동안 ALB 5xx 메트릭 폴링
> - 임계 초과 시 `aws ecs update-service --task-definition <previous-revision>`
> - 롤백 결과 Slack 또는 GHA 결과로 통지

## 완료 기준 (Definition of Done)

- [ ]  GHA OIDC Provider가 AWS IAM에 등록된다
- [ ]  `gha-deploy-role` 2개(prod / staging)가 분리되고 신뢰 정책에 repo+branch 조건이 정합
- [ ]  GHA 워크플로가 OIDC AssumeRole로 임시 자격증명 획득
- [ ]  `aws-actions/amazon-ecs-deploy-task-definition`이 Task Definition + 신규 이미지 태그로 업데이트
- [ ]  ECS Deployment Circuit Breaker 활성 → STEADY_STATE 실패 시 자동 롤백
- [ ]  배포 후 5분 5xx 폴링 → 임계 초과 시 GHA 추가 롤백
- [ ]  배포 1건 다운타임 0초 (헬스체크 폴링 검증)
- [ ]  연결된 Story가 모두 Done 상태다

## 산출물 (Deliverables)

- `.github/workflows/deploy-prod.yml` · `deploy-staging.yml` (Epic 4와 함께)
- `.github/workflows/image-build.yml` (Epic 1 산출물 활용)
- IAM Role 신뢰 정책 JSON (OIDC)
- 배포 가이드 + 롤백 Runbook (`docs/runbook/ecs-rollback.md`)

## 연결된 Story 목록

- [ ]  Story 3-1. GHA OIDC AssumeRole + ECS Deploy + 자동 롤백 + 5분 모니터링 구성 (5 SP)

## 내부 메모 / 제약 사항

- GHA OIDC audience: `sts.amazonaws.com` (AWS 표준)
- 임시 자격증명 TTL: 1시간 (default), 배포 시간 < 10분이면 충분
- ALB 5xx 메트릭 폴링은 GHA action `dorny/wait-for-metric` 또는 inline shell script
- 롤백 자동화의 한계: 데이터베이스 마이그레이션이 backward-incompatible이면 Task Definition만 롤백으로는 불충분 → Flyway는 forward-only 원칙 명시 (`product-card.md` 등에서 이미 적용 중)

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

### Q. OIDC 신뢰 정책의 `sub` 조건 범위

**Option A — `repo:org/repo:*` (와일드카드 허용)**
- 거부 이유: fork PR · 임의 branch에서 prod 자격증명 획득 가능 → 보안 사고 표면

**Option B (선택) — `repo:org/repo:ref:refs/heads/main` (prod) / `refs/heads/develop` (staging)**
- 비용: prod 핫픽스를 main 외 branch에서 배포 불가 (의도된 제약)
- 보상: 신뢰 경계 명확. fork PR 자동 차단. branch 별 권한 분리

### Q. 자동 롤백 트리거 메커니즘

**Option A — CloudWatch Alarm + ECS Service auto-revert**
- 거부 이유: ECS 자체 기능으로는 CloudWatch Alarm 기반 자동 롤백 미지원 (CodeDeploy 도입 필요). 별도 Lambda 트리거 패턴은 운영 자산 추가

**Option B — Deployment Circuit Breaker만**
- 거부 이유: Circuit Breaker는 STEADY_STATE 도달 실패만 감지 → 200 OK 응답하지만 비즈니스 로직 5xx 폭증은 못 잡음

**Option C (선택) — Circuit Breaker (헬스체크) + GHA monitor-5xx.sh (트래픽)의 2단 게이트**
- 비용: GHA workflow 시간 +5분 (배포 후 폴링)
- 보상: 헬스체크 실패는 ECS 자체 메커니즘으로 즉시 롤백, 5xx 폭증은 GHA가 직전 revision으로 update-service. CodeDeploy 도입 비용 없이 양쪽 커버

### Q. 5xx 임계 정의

**Option A — 절대 카운트 (예: 5xx > 100건/5분)**
- 거부 이유: 트래픽 절대값에 의존 → 트래픽 적은 시간대에는 거의 발동 안 함, 부하 spike 시간대에는 false positive

**Option B (선택) — 비율 (5xx / Total > 1% over 5분)**
- 비용: ALB에 충분한 트래픽이 없으면 비율 계산이 noisy (예: 100req 중 5xx 2건 = 2%)
- 보상: 트래픽 규모와 무관한 임계. Total Request가 너무 적으면 monitor-5xx.sh가 skip 결정

### Q. 배포 concurrency

**Option A — concurrency group 없음 (동시 실행 허용)**
- 거부 이유: 두 PR이 거의 동시에 main에 머지되면 두 GHA workflow가 동시 실행 → ECS UpdateService race condition 가능

**Option B (선택) — `concurrency: deploy-prod` + `cancel-in-progress: false`**
- 비용: 두 번째 배포는 첫 번째가 끝날 때까지 대기 (+ ~10분)
- 보상: ECS UpdateService 순차 보장. 마지막 commit이 결국 prod로 흐름

### Q. 롤백 시 commit 식별

**Option A — `aws ecs describe-services`의 `taskDefinition`에서 -1 revision 계산**
- 거부 이유: revision이 단조 증가 보장 안 됨 (다른 워크플로가 동시 revision 등록 시). 계산 오류 가능

**Option B (선택) — `aws ecs list-task-definitions --family thirdtool-prod --sort DESC --max-items 2` → 두 번째가 직전 revision**
- 비용: 명령어 약간 복잡
- 보상: 실제 deployment 이력에서 직전 revision 추출 → 정확

### Q. wait-for-stability 시간 한계

**Option A — `wait-for-minutes: 30` (보수적)**
- 거부 이유: GHA workflow 시간 비용 + 실패 인지 지연

**Option B (선택) — `wait-for-minutes: 10`**
- 비용: Task 기동이 10분 초과 시 실패 처리 → false positive 가능
- 보상: 정상 배포는 ~3분 내 완료. 10분 초과는 비정상 사인 → 빠른 실패 인지

---

## Story 3-1. GHA OIDC AssumeRole + ECS Deploy + 자동 롤백 + 5분 모니터링

### User Story

> As a 백엔드 개발자,
I want GHA가 OIDC로 AWS에 접근해 ECS Service를 업데이트하고 배포 실패 시 자동 롤백되길,
So that 시크릿 영구 키가 없어지고, 잘못된 배포가 prod를 망가뜨리는 사고를 자동으로 회복할 수 있다.
>

### 설계 노트

- `.github/workflows/deploy-prod.yml` (요약)

    ```yaml
    name: Deploy Production
    on:
      push:
        branches: [main]
    permissions:
      id-token: write
      contents: read
    jobs:
      deploy:
        runs-on: ubuntu-latest
        steps:
          - uses: actions/checkout@v4
          - name: Configure AWS credentials (OIDC)
            uses: aws-actions/configure-aws-credentials@v4
            with:
              role-to-assume: arn:aws:iam::ACCT:role/gha-deploy-role-prod
              aws-region: ap-northeast-2
          - name: Login to ECR
            uses: aws-actions/amazon-ecr-login@v2
          - name: Build & Push image
            run: |
              SHA=${GITHUB_SHA::7}
              docker build -t $ECR/thirdtool-app:$SHA .
              docker push $ECR/thirdtool-app:$SHA
              echo "IMAGE_TAG=$SHA" >> $GITHUB_ENV
          - name: Render Task Definition
            id: render
            uses: aws-actions/amazon-ecs-render-task-definition@v1
            with:
              task-definition: infra/ecs/task-definition-prod.json
              container-name: app
              image: ${{ env.ECR_URI }}/thirdtool-app:${{ env.IMAGE_TAG }}
          - name: Deploy to ECS
            uses: aws-actions/amazon-ecs-deploy-task-definition@v2
            with:
              task-definition: ${{ steps.render.outputs.task-definition }}
              service: thirdtool-app
              cluster: thirdtool-prod
              wait-for-service-stability: true
              wait-for-minutes: 10
          - name: Monitor 5xx for 5 minutes
            run: ./scripts/monitor-5xx.sh thirdtool-prod-alb 5 0.01
          - name: Rollback on failure
            if: failure()
            run: |
              PREV=$(aws ecs describe-task-definition --task-definition thirdtool-prod \
                     --query 'taskDefinition.revision' --output text)
              aws ecs update-service --cluster thirdtool-prod --service thirdtool-app \
                --task-definition thirdtool-prod:$((PREV-1))
    ```

- IAM Role 신뢰 정책 (`gha-deploy-role-prod`)

    ```json
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::ACCT:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:<org>/<repo>:ref:refs/heads/main"
        }
      }
    }
    ```

- ECS Deployment Circuit Breaker 활성 (Service 갱신)

    ```json
    {
      "deploymentConfiguration": {
        "deploymentCircuitBreaker": { "enable": true, "rollback": true }
      }
    }
    ```

- 5분 모니터링 스크립트 (`scripts/monitor-5xx.sh`) — 1분 단위 5회 폴링, 임계 초과 시 exit 1

### 완료 기준 (Acceptance Criteria)

- [ ]  main push → GHA가 OIDC로 AWS 인증 후 prod ECS 배포 완료
- [ ]  배포 중 `curl https://thirdtool.dev/actuator/health` 폴링이 200 OK 비율 100% 유지 (다운타임 0초)
- [ ]  의도적 헬스체크 실패 이미지 배포 시 → ECS Circuit Breaker가 자동 롤백
- [ ]  의도적 5xx 발생 이미지 배포 시 → GHA가 5분 모니터링 후 롤백
- [ ]  GitHub Secrets에 `AWS_ACCESS_KEY_ID` · `AWS_SECRET_ACCESS_KEY` 0건 (OIDC만 사용)
- [ ]  develop push → staging 배포 (`deploy-staging.yml`이 별도 Role 사용)
- [ ]  이전 `dev-cicd.yml`의 SSH/appleboy 호출이 제거된다

### 엣지 케이스

- 동시에 2개 PR이 main에 머지되면 → GHA 워크플로가 큐잉됨 (concurrency group 명시로 직렬화)
- OIDC trust policy의 sub 조건이 너무 좁으면 → fork PR 등에서 배포 시도 시 자동 차단 (의도). 너무 넓으면 보안 사고
- ECS Service의 `wait-for-minutes: 10` 초과 시 → GHA failure → 롤백 실행
- ALB 5xx 메트릭이 부하 spike 등 정상 5xx로도 임계 도달 가능 → 운영 중 임계값 캘리브레이션 필요 (`product-load-test.md` 분석 결과 활용)

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  GHA 워크플로 prod / staging 머지 + 1회 실제 배포 성공
- [ ]  의도 실패 배포 시뮬레이션 1회 + 자동 롤백 검증 스크린샷
- [ ]  롤백 Runbook (`docs/runbook/ecs-rollback.md`) 커밋
- [ ]  기존 `dev-cicd.yml` 제거 또는 명시 deprecated 표시
- [ ]  PO(또는 본인) 셀프 검수 완료

### 의존성

- 선행: Story 2-1 (ECS Service 운영), Product A Epic 2 (ALB Target Group)
- 후속: Epic 4 (환경 분리 워크플로 확장)

### 스토리 포인트

- 추정: 5 SP

---

## Epic 4. 환경 분리 — dev(로컬) · staging(develop) · prod(main)

# Epic 4. 환경 분리 — dev(로컬) · staging(develop) · prod(main)

## Epic 목표

> 환경 3개(dev / staging / prod)를 명시 분리하고, 각 환경의 진입점 · 배포 트리거 · 데이터 격리 · 책임 경계를 README와 GHA 워크플로에 박아둔다.
부하 테스트가 staging에서만 실행되고, prod 배포가 main 머지 후에만 자동 발동되는 표준을 정착시킨다.
>

## 배경

- staging이 사실상 존재하지 않거나 prod와 혼재된 상태 → load-test가 정상 진행 불가
- 개발자가 어느 환경에서 어떤 명령을 실행해야 하는지 README에 명시 안 됨 → 신규 합류자 시간 소모
- 환경별 비밀(DB 비밀번호 · OAuth client secret 등)이 어디에 저장됐는지 추적 단절

## 핵심 설계 결정

> **dev는 docker-compose, staging/prod는 ECS Fargate. branch 기반 자동 트리거.**
환경별 진입점이 명확.
>
> - dev: `docker compose up`으로 로컬 H2 + Redis + 앱 컨테이너
> - staging: develop 브랜치 push → `deploy-staging.yml` 자동 실행
> - prod: main 브랜치 push → `deploy-prod.yml` 자동 실행
> - feature 브랜치는 PR CI(빌드 + 테스트)만 — 배포 X

> **환경별 application.yml 분기 + Secrets Manager prefix 분리 (Product C 연계).**
환경 혼선 사고 차단.
>
> - `application-dev.yml`: H2 + 로컬 시크릿
> - `application-staging.yml`: staging RDS + Secrets Manager `thirdtool/staging/*`
> - `application-prod.yml`: prod RDS + Secrets Manager `thirdtool/prod/*`
> - SPRING_PROFILES_ACTIVE 환경변수가 Task Definition에서 결정 — 컨테이너 진입 시 분기

## 완료 기준 (Definition of Done)

- [ ]  GHA 워크플로 3개 (`deploy-prod.yml`, `deploy-staging.yml`, `pr-ci.yml`)가 명시 분리된다
- [ ]  README에 환경 진입점 표(dev/staging/prod URL + 배포 트리거) 추가된다
- [ ]  branch protection rule: main · develop은 PR + status check 필수
- [ ]  staging에서 부하 테스트 1회 실행 (`product-load-test.md` 시나리오)
- [ ]  연결된 Story가 모두 Done 상태다

## 산출물 (Deliverables)

- `.github/workflows/deploy-prod.yml` · `deploy-staging.yml` · `pr-ci.yml`
- `docker-compose.yml` (dev 환경 정의 — 기존 파일 정비)
- 환경 진입점 표 (README)
- branch protection 설정

## 연결된 Story 목록

- [ ]  Story 4-1. GHA 워크플로 3개 분리 + docker-compose dev 정비 + branch protection (4 SP)

## 내부 메모 / 제약 사항

- PR CI는 빌드 + 단위 테스트만 (통합 테스트는 Testcontainers 도입 시점에) — 비용·시간 절약
- staging 배포가 실패해도 prod 배포는 차단 안 함 (독립 트리거) — 단, develop이 main에 머지될 때는 staging이 통과한 commit만 머지하도록 PR 룰 검토
- dev 환경의 `docker-compose.yml`은 기존 파일 정비 (MySQL → H2 옵션 + Redis 보존)

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

### Q. dev 환경 정의

**Option A — dev 환경도 ECS Fargate (별도 cluster)**
- 거부 이유: 1인 운영에서 dev cluster 비용 정당화 어려움. 로컬 디버깅 속도 ↓

**Option B — dev 환경 없음 (staging이 곧 dev)**
- 거부 이유: 로컬에서 빠른 반복 검증이 불가. staging 트리거가 develop push라 매 변경마다 push 필요

**Option C (선택) — dev는 로컬 `docker compose up` 전용**
- 비용: 로컬 docker 설정 분기 (H2 + 로컬 OAuth)
- 보상: 즉시 반복 (`./gradlew bootRun` 또는 docker rebuild). 비용 0

### Q. branch 전략

**Option A — main 단일 branch (trunk-based)**
- 거부 이유: staging 환경의 트리거가 모호 → main push 시 prod·staging 동시 배포? PR open 시 staging?

**Option B (선택) — main (prod) / develop (staging) 2-branch**
- 비용: hotfix 시 main → develop cherry-pick 필요
- 보상: 트리거 매핑이 직관적. branch protection으로 main 직접 push 차단

**Option C — Gitflow (feature / develop / release / main / hotfix)**
- 거부 이유: 1인 운영에 release branch 운영은 과잉. release tag로 대체 가능하나 branch 기반 자동 배포 결정과 충돌

### Q. PR CI 범위

**Option A — 빌드만**
- 거부 이유: 단위 테스트 누락이 main 머지 후 발견되면 배포 실패 → 자동 롤백 발동 가능

**Option B (선택) — 빌드 + 단위 테스트 + 슬라이스 테스트**
- 비용: PR CI 시간 ~3~5분
- 보상: 머지 전 게이트로 배포 실패율 감소. branch protection의 required check로 강제

**Option C — 빌드 + 모든 테스트 + 통합 테스트 (Testcontainers)**
- 보류: Testcontainers 도입 자체가 별 Story. v2에서 추가

### Q. staging → prod 승격 정책

**Option A — develop이 staging 통과 후에만 main에 머지 허용 (강제)**
- 거부 이유: GHA로 PR rule을 강제 구현하기 복잡 (custom check + status check 의존)

**Option B (선택) — 권장만 명시, 강제는 안 함 (운영자 판단)**
- 비용: hotfix 시 staging 미통과 commit이 main 직행 가능
- 보상: 1인 운영의 유연성 보장. 자동 롤백이 안전망

### Q. branch protection 강도

**Option A — main 직접 push 허용**
- 거부 이유: 의도치 않은 prod 배포 위험

**Option B (선택) — main: PR + status check `pr-ci` + linear history / develop: 동일하되 review 0**
- 비용: 1인 운영에서 PR 셀프 머지 마찰 (작지만 존재)
- 보상: 모든 prod 변경이 PR 단위로 기록 → 사고 시 추적 명확

### Q. docker-compose 의존 서비스

**Option A — MySQL + Redis + Elasticsearch 전부**
- 거부 이유: 로컬 disk · CPU 부담 ↑. MySQL은 H2로 충분, ES는 검색 기능 검증 시에만 필요

**Option B (선택) — Redis만 + H2 in-memory (Spring Boot 내장)**
- 비용: 로컬에서 ES 검증 시 `docker compose --profile es up`으로 별도 활성화 필요
- 보상: 일상 dev 환경 가벼움 (~512MB RAM)

---

## Story 4-1. GHA 워크플로 3개 분리 + docker-compose dev 정비 + branch protection

### User Story

> As a 백엔드 개발자,
I want dev/staging/prod 3환경이 워크플로 · 배포 트리거 · 데이터 격리 측면에서 명확히 분리되길,
So that 부하 테스트가 prod를 두드리지 않고, 새 기능 검증이 staging에서 안전하게 진행되고, prod 배포가 의도된 commit에서만 발동된다.
>

### 설계 노트

- 환경 진입점 표 (README 발췌)

    | 환경 | URL | DB | 배포 트리거 | 비밀 출처 |
    | --- | --- | --- | --- | --- |
    | dev (로컬) | `http://localhost:8080` | H2 in-memory | `docker compose up` | `.env` 로컬 파일 |
    | staging | `https://staging.thirdtool.dev` | staging RDS (single-AZ) | develop 브랜치 push | Secrets Manager `thirdtool/staging/*` |
    | prod | `https://thirdtool.dev` | prod RDS (Multi-AZ) | main 브랜치 push | Secrets Manager `thirdtool/prod/*` |

- `pr-ci.yml`

    ```yaml
    name: PR CI
    on:
      pull_request:
        branches: [main, develop]
    jobs:
      ci:
        runs-on: ubuntu-latest
        steps:
          - uses: actions/checkout@v4
          - uses: actions/setup-java@v4
            with: { distribution: temurin, java-version: 21 }
          - run: ./gradlew build
    ```

- branch protection (GitHub UI 또는 `gh` CLI)
    - main: require pull request + 1 review + status check `pr-ci` + linear history
    - develop: 동일하되 0 review (1인 운영)

- `docker-compose.yml` (dev 환경 정비)

    ```yaml
    services:
      app:
        build: .
        ports: ["8080:8080"]
        environment:
          SPRING_PROFILES_ACTIVE: dev
        depends_on: [redis]
      redis:
        image: redis:7.2-alpine
        ports: ["6379:6379"]
    ```

    (H2 in-memory는 Spring Boot 내장 — 별도 서비스 불필요)

### 완료 기준 (Acceptance Criteria)

- [ ]  `.github/workflows/` 디렉토리에 워크플로 3개 (`deploy-prod`, `deploy-staging`, `pr-ci`)가 존재한다
- [ ]  기존 `dev-cicd.yml`이 제거되고 git log에 deprecation 명시
- [ ]  main · develop branch protection이 활성화된다 (`gh api` 결과 확인)
- [ ]  README에 환경 진입점 표가 갱신된다
- [ ]  `docker compose up`으로 dev 환경이 정상 기동된다 (`http://localhost:8080/actuator/health` 200 OK)
- [ ]  develop branch push → staging 자동 배포 + 부하 테스트 가능 상태 (load-test smoke 1회 실행)
- [ ]  main branch push → prod 자동 배포

### 엣지 케이스

- main에 hotfix가 PR 없이 직접 push 시도 → branch protection이 차단
- develop과 main이 동시에 push되면 staging과 prod 배포가 동시 진행 — concurrency group 명시 또는 의도된 병렬
- dev 환경의 SPRING_PROFILES_ACTIVE 누락 → 기본값 `dev`로 fallback (`application.yml` 최상위에 명시)

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  워크플로 3개 머지 + 각 환경 1회 실제 배포 성공
- [ ]  README 환경 진입점 표 머지
- [ ]  branch protection 적용 확인 스크린샷
- [ ]  PO(또는 본인) 셀프 검수 완료

### 의존성

- 선행: Story 3-1 (배포 자동화 작동), Product A Story 3-1 (staging RDS 존재)
- 후속: Product C (Secrets Manager 통합으로 환경별 비밀 분리 완성)

### 스토리 포인트

- 추정: 4 SP

---

## Product 요약

| Epic | Story 수 | SP 합계 |
| --- | --- | --- |
| Epic 1. Docker 이미지·ECR 표준화 | 1 | 3 |
| Epic 2. ECS Fargate 런타임 | 1 | 5 |
| Epic 3. GHA OIDC → ECS 무중단 배포 | 1 | 5 |
| Epic 4. 환경 분리 | 1 | 4 |
| **합계** | **4** | **17 SP** |

**진행 순서 (필수):** Epic 1 → 2 → 3 → 4. 이미지 → 런타임 → 배포 자동화 → 환경 분리.

**Product A · C와의 연결 포인트**
- Epic 2의 ECS Service `loadBalancers`가 Product A Story 2-1의 Target Group ARN 입력 받음
- Epic 2의 Task Role에 `secretsmanager:GetSecretValue` 권한 미리 부여 → Product C Story 1-1에서 secret 등록 시 즉시 활용
- Epic 3의 OIDC IAM Role · ECS Service 설정 전체가 Product C Epic 2의 `terraform import` 대상

**기존 운영성 Product와의 연결 포인트**
- `product-load-test.md` baseline 측정이 staging 환경에서 실제 가능해짐 (ADR-LOAD-002 충족)
- `product-op.md` Grafana 대시보드에 ECS Task CPU/Memory 메트릭 추가 (CloudWatch agent 또는 cAdvisor)
- `product-log.md` JSON 로그가 awslogs driver로 CloudWatch Logs에 전송 → 수집 인프라(v2)로 자연스럽게 연결
- `product-aisuggestion.md` Vertex AI Gemini ADC 인증이 Task Role + GCP Workload Identity Federation 패턴 (Product C에서 구체화)
- `product-auth.md` OAuth 콜백 URL이 https://thirdtool.dev · https://staging.thirdtool.dev로 환경별 분리

**v0 잔재 — 마이그레이션 체크리스트**
- `.github/workflows/dev-cicd.yml` (SSH/appleboy) → 제거
- `Dockerfile-dev` → 통합 또는 명시 로컬 전용 표시
- `Dockerfile-elasticsearch` → ECR 리포 분리 + 별도 워크플로
- GitHub Secrets의 `AWS_ACCESS_KEY_ID` · `AWS_SECRET_ACCESS_KEY` → OIDC 전환 후 제거
- EC2 인스턴스 종료 (ECS 안정화 후)
