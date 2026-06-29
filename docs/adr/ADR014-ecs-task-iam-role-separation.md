# ADR014: ECS Task IAM Role을 Execution / Task / GHA Deploy 3종으로 분리한다 (Story-047)

- **상태**: Accepted
- **날짜**: 2026-06-29
- **관련**: Product 5 컨테이너 배포 Epic 2 (ECS Fargate 런타임) · milestone 0.0.1v item #11 (`workflow/task/milestones/version/0.0.1v/milestone.md`) · `docs/operations/troubleshooting/ts008-ecs-cluster-setup.md` · `infra/iam/ecs-task-*-role-*.json` · ADR013 (GHA OIDC AssumeRole)

## 컨텍스트

milestone item #11에서 ECS Fargate Task를 정의·실행하려면 **IAM Role이 최소 3종 필요**하다. AWS ECS의 권한 모델 자체가 다음을 분리한다:

- **ECS Agent (`ecs-tasks.amazonaws.com`)가 Task 시작 전에 사용**: ECR pull · CloudWatch Logs stream 생성 · Secrets Manager 시크릿 fetch
- **Task 안의 컨테이너 코드가 런타임에 사용**: S3 · 외부 AWS API 호출
- **GitHub Actions runner가 외부에서 사용 (Story-046)**: ECR push · ECS Service update · `iam:PassRole`

이 셋을 단일 Role로 통합하면 **컨테이너 침해 시 권한 확대 위험**이 발생한다. 예:
- ECS 에이전트와 컨테이너 코드 권한이 합쳐지면 → 컨테이너에서 임의 ECR repo에 image push 가능
- GHA와 Task 권한이 합쳐지면 → 컨테이너에서 자기 자신을 다른 Task Definition으로 update 가능

product-infra-deploy.md Epic 2 §"핵심 설계 결정"에서 IAM Role 3종 분리를 권고하고, ADR013(OIDC) follow-up에서 본 ADR을 예고했다.

## 결정

**ECS 운영에 사용되는 IAM Role을 3종으로 분리**한다:

| Role | 신뢰 주체 | 사용 시점 | 권한 |
| --- | --- | --- | --- |
| `gha-deploy-role` | GHA OIDC Provider (ADR013) | workflow 실행 중 | ECR push · ECS RegisterTaskDefinition/UpdateService · `iam:PassRole`(두 Task Role 한정) |
| `ecs-task-execution-role` | `ecs-tasks.amazonaws.com` | Task 시작 전 (ECS 에이전트) | ECR pull (두 repo 한정) · CloudWatch Logs 쓰기 (두 group 한정) · Secrets Manager read (`thirdtool/{prod,staging}/*` prefix 한정) |
| `ecs-task-role` | `ecs-tasks.amazonaws.com` | 컨테이너 코드 런타임 | S3 (`third-tool-s3-server` 한정) · SSM Messages (ECS Exec 채널) |

### 핵심 권한 경계

1. **`gha-deploy-role`의 `iam:PassRole`은 두 Task Role ARN으로 정확히 한정** + Condition `iam:PassedToService = ecs-tasks.amazonaws.com`:
   - 다른 ARN의 Role을 ECS Task에 PassRole 차단
   - 다른 서비스(`ec2.amazonaws.com`, `lambda.amazonaws.com` 등)에 PassRole 차단

2. **`ecs-task-execution-role`과 `ecs-task-role`은 동일 신뢰 주체(`ecs-tasks.amazonaws.com`)지만 별도 Role**:
   - AWS ECS가 이 둘을 의도적으로 분리 사용 — Execution Role은 컨테이너 *밖*(에이전트)에서, Task Role은 컨테이너 *안*(애플리케이션)에서
   - 합치면 컨테이너 안에서 ECR/Logs 쓰기 권한 노출

3. **각 Role의 권한이 resource ARN으로 한정**:
   - Execution Role의 Secrets Manager read는 `thirdtool/prod/*` + `thirdtool/staging/*`만 (다른 시크릿 차단)
   - Task Role의 S3는 `third-tool-s3-server` bucket만 (다른 버킷 차단)

### `gha-deploy-role` ECS actions resource 한정 분석

5 ECS actions가 모두 `Resource: "*"`를 사용한다. AWS IAM 문서 기준 한정 가능성:

| Action | resource 한정 가능 여부 | 본 ADR 선택 사유 |
| --- | --- | --- |
| `ecs:RegisterTaskDefinition` | **불가** — task definition은 등록 *전에는* ARN 미존재 | `*` 사용 강제. 동일 family에 한정하려면 `Condition.StringLike."ecs:taskdefinition-family"` 사용 가능하나 GHA workflow가 본 Story에서 prod·staging 두 family 모두 다루므로 효과 작음 |
| `ecs:DescribeTaskDefinition` | **불가** — 어떤 family든 read는 계정 단위로 풀림 | `*` 사용 표준 |
| `ecs:DeregisterTaskDefinition` | **불가** — 동일 사유 | `*` 사용. 단 IAM 위험 작음 (deregister는 revision 단위 비활성화) |
| `ecs:DescribeServices` | 한정 가능 — `arn:aws:ecs:region:acct:service/cluster/service-name` | M2에서 cluster-level 한정 검토. M1은 GHA가 새 service 생성 가능성 있어 `*` 유지 |
| `ecs:UpdateService` | 한정 가능 — 동일 service ARN | M2에서 `arn:aws:ecs:ap-northeast-2:<acct>:service/thirdtool-{prod,staging}/thirdtool-app` 두 ARN으로 한정 권장. **본 Story 범위 내에서는 보안 표면이 크지만 GHA OIDC sub=main 잠금이 보조 가드** |

→ **현재 결정**: 5 actions 모두 `*` 유지 + GHA OIDC sub 조건(`refs/heads/main`)으로 priviege escalation 표면 보조 차단. UpdateService/DescribeServices 한정은 **M2 follow-up Story** (deploy automation 도입 시 동시).

### 본 ADR이 다루지 않는 범위

- **GCP Workload Identity Federation**: Spring AI Gemini용 Task Role의 GCP 인증은 Product 7 (Secrets·Terraform)에서 구체화
- **staging IAM Role 분리**: 본 ADR은 prod/staging이 동일 Task Role 사용. 환경별 권한 분리는 환경 분리 Epic
- **관리형 정책(`AmazonECSTaskExecutionRolePolicy`) 부착 vs inline 명시**: 본 ADR은 inline 명시 채택 — 감사·재현성·prefix 한정 가능
- **`application-staging.yml` 코드 부재로 인한 staging Task Def `SPRING_PROFILES_ACTIVE=prod` transitional 결정**: staging profile 신설 + Task Def env 정정은 별도 Story (환경 분리 Epic). 본 Story는 코드 변경 없이 ts008 §1에 함정 명시로만 처리
- **`/health` endpoint의 DB 의존성 미체크**: 단순 200 응답이라 DB down 시에도 healthCheck pass. ECS가 false-positive healthy 판정. `/actuator/health` + DB HealthIndicator 활성 전환은 별도 Story (ts008 §9 follow-up)

## 결과 (Consequences)

### 긍정적

- **컨테이너 침해 영향 한정**: Task Role의 권한이 S3 한 bucket으로 한정. 침해 시 다른 AWS 리소스 영향 없음
- **PassRole 경계 명확**: GHA workflow가 임의 Role을 Task에 attach 못 함 → CI/CD compromise 시 prod IAM 권한 확대 차단
- **감사 가능성 향상**: 각 Role의 사용 주체가 명확 — CloudTrail에서 `assumedRoleId` 보고 어디서 호출됐는지 즉시 추적
- **Least Privilege 패턴 정착**: 후속 Story (#15 Secrets Manager, GCP WIF 등)에서 동일 패턴 답습 가능
- **inline 정책으로 코드 단일 진실 소스**: 관리형 정책 의존 없이 `infra/iam/` 5 JSON이 모든 권한을 명시 — 감사·롤백·재현 일원화

### 트레이드오프 / 부정적

- **관리 Role 3개 + inline policy 5개**: 단일 Role 대비 운영 부담 증가. ts008 runbook으로 1회 셋업 절차 표준화
- **`ecs:GetAuthorizationToken` 같은 계정 단위 권한은 resource `*` 사용**: 한정 불가능한 AWS 권한은 그대로. 명시적 inline로 다른 부분과 동일 감사 흐름 유지
- **JSON 5개의 sync 부담**: prod/staging가 동일 Role 사용이라 환경별 권한 분리는 미래 작업. 환경별 분리 시 5 → 8개 정책으로 증가
- **`iam:PassRole` 추가가 보안 표면 확장 — 다만 ARN 한정 + Condition으로 차단**: gha-deploy-role 권한이 ECR-only에서 ECR + ECS deploy + PassRole로 확장. ADR013 결정 시 예고된 follow-up이므로 적정

## 대안 비교

| 대안 | 장점 | 거부 사유 |
| --- | --- | --- |
| **A. 단일 Role (`ecs-unified-role`)** | 관리 부담 최소 | 컨테이너 침해 시 ECR push · CloudWatch Logs 쓰기 · ECS Service update 권한 모두 노출 — 권한 확대 표면이 결정적 |
| **B. Execution Role과 Task Role 통합** | Role 1개 절감 | AWS ECS가 두 Role을 의도적 분리 사용 — 통합 시 컨테이너 안에서 Execution 권한 사용 가능 → AWS 아키텍처 위반 |
| **C. Task Role 미사용 (Execution Role만 + 컨테이너 코드도 사용)** | Role 1개 절감 | ECS 에이전트가 Task Role을 사용할 수 없음 (AWS 아키텍처 제약) — 동작 자체가 불가 |
| **D. GHA Deploy Role과 Task Execution Role 통합** | OIDC Role 1개로 통합 | GHA OIDC는 외부 식별자, ECS는 AWS 내부 서비스 — 신뢰 주체가 본질적으로 다름 |
| **E (선택). 3종 분리 (gha-deploy + ecs-task-execution + ecs-task)** | 권한 경계 명확, 감사 추적성 향상 | Role 3개 관리 부담 |
| **F. 관리형 정책(`AmazonECSTaskExecutionRolePolicy`) 부착** | 입력 토큰 적음 | resource 한정 불가능 — `thirdtool/prod/*` prefix 잠금 같은 세밀한 제한 못 함 |

## 알려진 follow-up (본 ADR 범위 외)

- **milestone 0.0.1v item #15 (Secrets Manager)** 완료 후: `ecs-task-execution-role` permissions의 Secrets Manager resource ARN을 실제 시크릿 ARN(`<SUFFIX>` 포함)으로 정정
- **`S3Config.java` → `DefaultCredentialsProvider` 전환**: Task Role의 S3 권한이 자동 주입돼 Task Def의 `AWS_ACCESS_KEY_ID/SECRET` secret 2건 제거 가능. **transitional 영구화 방지** — ts008 §9 `Story-TBD(S3 Credentials)`로 트래킹
- **EC2 SSH 배포 step 제거**: ECS 운영 검증 통과 후. `dev-cicd.yml`의 `Deploy to EC2 via SSH` step + 컨테이너 env var pass-through 일괄 삭제 — ts008 §9 `Story-TBD(EC2 폐기)`
- **staging 환경별 권한 분리**: 본 ADR은 prod/staging이 동일 Task Role. staging 권한 축소(예: `s3:GetObject`만) 시 별도 `ecs-task-role-staging` Role 신설
- **`application-staging.yml` 신설 + staging Task Def env 정정**: 현재 staging Task Def `SPRING_PROFILES_ACTIVE=prod` 함정 해소 — ts008 §9 `Story-TBD(staging profile)`
- **`/health` → `/actuator/health` + DB HealthIndicator 활성**: ECS healthCheck false-positive 차단 — ts008 §9 `Story-TBD(Health Indicator)`
- **`gha-deploy-role` ECS UpdateService/DescribeServices resource 한정**: 본 ADR §대안 분석 표 — M2 deploy automation 도입 시 service ARN 2건으로 한정 권장
- **GCP Workload Identity Federation**: Spring AI Gemini용. Task Role이 GCP STS에 OIDC token 제출하는 패턴 — Product 7
- **Terraform IaC**: 본 ADR은 콘솔/CLI 셋업 가정. Terraform 모듈화는 Product 7 Epic 2

## 다시 검토할 시점

- **단일 운영자 → 다인 운영 전환 시점**: Role 분리의 감사 가치가 다인 운영에서 더 중요 — 현 결정 유지 권장
- **prod/staging 권한 분리 시점**: 환경 분리 Epic에서 `ecs-task-role-staging` 신설 검토
- **ECS Service Auto Scaling 도입 시점**: `gha-deploy-role`에 `application-autoscaling:*` 추가 필요
- **CodeDeploy Blue/Green 전환 시점**: CodeDeploy IAM Role 신설 + `gha-deploy-role` 권한 위임 패턴 검토
