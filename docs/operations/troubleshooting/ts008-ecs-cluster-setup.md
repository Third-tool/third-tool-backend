# ts008: ECS Cluster · Task Definition · Service 셋업·검증·트러블슈팅

Story-047로 `infra/ecs/`와 `infra/iam/`에 ECS Fargate 구동에 필요한 JSON 정의가 추가됐다. 본 가이드는 AWS 콘솔(또는 CLI)에서 1회성 셋업하는 절차 + 검증 + 자주 보는 에러 6건을 한 곳에 모았다.

ts005(Dockerfile)·ts007(OIDC)와 cross-link. 본 절차는 1인 운영자가 1회만 실행하면 됨.

---

## 1. 전제

- ts007 §2-4 완료 — OIDC Provider · `gha-deploy-role` · GitHub Variables `AWS_ACCOUNT_ID` 등록
- AWS 콘솔 IAM 관리 권한 (`iam:CreateRole` · `iam:PutRolePolicy`)
- AWS 콘솔 ECS 관리 권한 (`ecs:CreateCluster` · `ecs:RegisterTaskDefinition` · `ecs:CreateService`)
- AWS CLI 설치 (선택 — 콘솔 대신 명령으로도 진행 가능)

**placeholder 의존 상태 (후속 Story로 치환됨)**:
- `<APP_SUBNET_2A>` · `<APP_SUBNET_2C>` · `<APP_SG>` → milestone 0.0.1v item #12 VPC 완료 후
- `<PROD_TARGET_GROUP_ARN>` · `<STAGING_TARGET_GROUP_ARN>` → milestone 0.0.1v item #13 ALB 완료 후
- `<RDS endpoint>` (Task Def secrets의 `DB_HOST`) → milestone 0.0.1v item #14 RDS 완료 후
- `secrets.valueFrom` ARN suffix `<SUFFIX>` → milestone 0.0.1v item #15 Secrets Manager 완료 후

본 ts008은 위 4 placeholder가 모두 치환된 시점에 end-to-end 실행 가능. 그 전까지는 §1-2 (Log group + IAM Role)까지만 진입하고 §3-5는 dry-run 가이드로 유지.

---

## 2. CloudWatch Log group 2개 생성

```bash
aws logs create-log-group --log-group-name /ecs/thirdtool-prod --region ap-northeast-2
aws logs put-retention-policy --log-group-name /ecs/thirdtool-prod --retention-in-days 30 --region ap-northeast-2

aws logs create-log-group --log-group-name /ecs/thirdtool-staging --region ap-northeast-2
aws logs put-retention-policy --log-group-name /ecs/thirdtool-staging --retention-in-days 7 --region ap-northeast-2
```

retention: prod 30일 / staging 7일 (product Epic 2 §대안 비교 Q "로그 보존 정책" Option B 선택).

`awslogs-create-group: "false"`를 Task Def에 명시했기 때문에 사전 생성 필수. 누락 시 컨테이너 부팅 시점에 `logConfiguration` 에러로 STOPPED.

---

## 3. IAM Role 3종 생성

### 3.1 `gha-deploy-role` 확장 (Story-046에서 이미 생성)

기존 Role에 inline policy 갱신 (`infra/iam/gha-deploy-role-permissions-policy.json` 내용으로 교체):
1. AWS 콘솔 → IAM → Roles → `gha-deploy-role` → Permissions → inline policy `gha-deploy-role-permissions` → Edit
2. JSON 탭 → `infra/iam/gha-deploy-role-permissions-policy.json` 내용으로 교체
3. `<AWS_ACCOUNT_ID>` 12자리 치환
4. **치환 누락 검증**: 입력 직전 `<` 문자 0건 확인

신규 Statement 2건 (`EcsDeploy` + `IamPassRoleToEcsTasks`)이 보임. 기존 `EcrAuthToken` + `EcrPushPull`은 보존.

### 3.2 `ecs-task-execution-role` 신규 생성

1. AWS 콘솔 → IAM → Roles → Create role
2. Trusted entity type: **Custom trust policy** → `infra/iam/ecs-task-execution-role-trust-policy.json` 내용 붙여넣기 (치환 불요)
3. Permissions: 다음 단계
4. Role name: `ecs-task-execution-role`
5. Create role
6. 생성된 Role → Permissions → Add permissions → Create inline policy → JSON
7. `infra/iam/ecs-task-execution-role-permissions-policy.json` 내용 붙여넣기 + `<AWS_ACCOUNT_ID>` 치환
8. Policy name: `ecs-task-execution-role-permissions`

### 3.3 `ecs-task-role` 신규 생성

3.2와 동일 절차로 신규 Role 생성:
- Trust policy: `infra/iam/ecs-task-role-trust-policy.json` (Execution Role과 동일 내용이지만 별도 Role로 분리 — ADR014)
- Role name: `ecs-task-role`
- Inline policy name: `ecs-task-role-permissions` / 내용: `infra/iam/ecs-task-role-permissions-policy.json` + `<AWS_ACCOUNT_ID>` 치환

> **검증**: 두 Role의 신뢰 정책은 동일하지만 권한 정책은 달라야 한다. Execution = ECR/Logs/Secrets, Task = S3/SSM. 동일하게 됐으면 ADR014 §결정의 권한 분리 의도 위반 → Role 합쳐진 셈.

---

## 4. ECS Cluster 2개 생성

```bash
aws ecs create-cluster \
  --cluster-name thirdtool-prod \
  --capacity-providers FARGATE FARGATE_SPOT \
  --default-capacity-provider-strategy capacityProvider=FARGATE,weight=100 \
  --region ap-northeast-2

aws ecs create-cluster \
  --cluster-name thirdtool-staging \
  --capacity-providers FARGATE FARGATE_SPOT \
  --region ap-northeast-2
```

staging의 capacity provider strategy는 Service 생성 시점에 명시(`service-staging.json`의 `capacityProviderStrategy`).

검증:
```bash
aws ecs describe-clusters \
  --clusters thirdtool-prod thirdtool-staging \
  --region ap-northeast-2 \
  | jq '.clusters[].status'
# 기대 출력: "ACTIVE" x 2
```

---

## 5. Task Definition 등록

### 5.1 placeholder 치환

`infra/ecs/task-definition-prod.json`을 임시 파일로 복사 후 치환:
- `<AWS_ACCOUNT_ID>` → 12자리 계정 ID
- `<IMAGE_TAG>` → 배포할 ECR 이미지 태그 (예: `latest` 또는 git_sha 7자)
- `<SUFFIX>` → Secrets Manager 시크릿 이름의 random suffix (#15 완료 후, 예: `Ab3cD9`)

```bash
sed -e "s/<AWS_ACCOUNT_ID>/123456789012/g" \
    -e "s/<IMAGE_TAG>/latest/g" \
    -e "s/<SUFFIX>/Ab3cD9/g" \
    infra/ecs/task-definition-prod.json > /tmp/td-prod.json
grep -c '<' /tmp/td-prod.json    # 0이어야 함
```

### 5.2 등록

```bash
aws ecs register-task-definition --cli-input-json file:///tmp/td-prod.json --region ap-northeast-2
```

응답에서 `taskDefinitionArn` 확인. `ACTIVE` 상태로 revision 1.

staging도 동일 절차 (`task-definition-staging.json`).

---

## 6. ECS Service 생성

### 6.1 placeholder 치환

`infra/ecs/service-prod.json`을 임시 복사 후:
- `<APP_SUBNET_2A>`, `<APP_SUBNET_2C>` → VPC private subnet ID 2개 (#12 완료 후)
- `<APP_SG>` → 컨테이너 Security Group ID (#12 완료 후)
- `<PROD_TARGET_GROUP_ARN>` → ALB Target Group ARN (#13 완료 후)

### 6.2 생성

```bash
aws ecs create-service --cli-input-json file:///tmp/svc-prod.json --region ap-northeast-2
```

응답에서 `serviceArn` + `deployments[0].status: PRIMARY` 확인.

ECS Scheduler가 Task 2개를 배치하기까지 약 1-2분 소요. 진행 상황:
```bash
aws ecs describe-services \
  --cluster thirdtool-prod --services thirdtool-app \
  --region ap-northeast-2 \
  | jq '.services[0].deployments'
# runningCount=2, desiredCount=2, status=STEADY_STATE 도달 시 완료
```

---

## 7. 검증

### 7.1 Task 상태

```bash
aws ecs list-tasks --cluster thirdtool-prod --service-name thirdtool-app --region ap-northeast-2 \
  | jq -r '.taskArns[]' \
  | xargs aws ecs describe-tasks --cluster thirdtool-prod --region ap-northeast-2 --tasks \
  | jq '.tasks[] | {lastStatus, healthStatus, availabilityZone}'
# 기대: lastStatus=RUNNING, healthStatus=HEALTHY, AZ가 ap-northeast-2a / 2c로 분산
```

### 7.2 CloudWatch Logs 흐름

AWS 콘솔 → CloudWatch → Log groups → `/ecs/thirdtool-prod` → 최신 log stream에 Spring Boot 부팅 로그(`Started ThirdToolApplication`) JSON 한 줄 + `traceId` MDC 필드 포함되는지 확인.

### 7.3 ALB Target Group healthy

AWS 콘솔 → EC2 → Target Groups → `<PROD_TARGET_GROUP>` → Targets 탭에서 2개 Task 모두 `healthy` 상태.

### 7.4 endpoint 응답

```bash
curl -sSf https://<dev-domain>/health
# 기대: 200 OK
```

### 7.5 ECS Exec 진입

Alpine이라 shell 진입 가능:
```bash
aws ecs execute-command \
  --cluster thirdtool-prod \
  --task <TASK_ARN> \
  --container app \
  --interactive --command "/bin/sh" \
  --region ap-northeast-2
# 컨테이너 안에서: env | grep SPRING_PROFILES_ACTIVE → "prod" 확인
```

---

## 8. 트러블슈팅

### ts008-1: Task가 PENDING → STOPPED, 사유 `ResourceInitializationError: unable to pull secrets or registry auth`

**원인**: `executionRoleArn`이 잘못됐거나 권한 부족. ECR pull 또는 Secrets Manager 접근 실패.

**해결**:
1. Task Def `executionRoleArn` 값이 실제 생성된 Role ARN과 정확히 일치하는지 (대소문자·account ID·region 포함)
2. `ecs-task-execution-role` permissions policy에 ECR pull 3 actions + Secrets Manager read 2 actions이 있는지
3. Secrets Manager 시크릿 prefix가 `thirdtool/prod/` 또는 `thirdtool/staging/`로 시작하는지 (resource ARN과 매치)

### ts008-2: Task RUNNING이지만 ALB Target unhealthy

**원인 가능성**:
- ECS container `healthCheck` 명령이 실패 — `wget` 미설치 또는 Spring Boot 부팅 시간 > startPeriod(60s)
- ALB Target Group의 health check path가 `/health`가 아닌 다른 경로 (#13에서 설정)
- Security Group이 ALB → Task 8080 허용 안 함

**해결**:
1. Task 컨테이너 로그에서 `Started ThirdToolApplication in N.NNN seconds` 확인 — 60초 초과면 startPeriod 증가
2. ECS Exec로 진입 후 `wget -q -O- http://localhost:8080/health` 실행 → 200 OK 응답 확인
3. ALB Target Group health check 설정: path=`/health`, port=`traffic-port`(8080), healthy threshold 2

### ts008-3: Task STOPPED, 사유 `Essential container exited with code N`

**원인**: Spring Boot 부팅 실패 (env var 누락, DB 연결 실패, placeholder 미해석 등).

**해결**:
1. CloudWatch Logs `/ecs/thirdtool-prod` 최신 stream의 `Caused by:` 라인 확인
2. `Could not resolve placeholder 'X'` → Task Def `secrets` 배열에 누락된 환경변수가 있음. 모든 11개 secret이 valid한 ARN으로 매핑됐는지 확인
3. `Communications link failure` → RDS endpoint(`DB_HOST`)가 잘못됐거나 RDS Security Group이 Task SG의 3306 inbound 허용 안 함 (#14)

### ts008-4: `iam:PassRole` 거부 — GHA workflow에서 `UpdateService` 시 `User is not authorized to perform: iam:PassRole`

**원인**: `gha-deploy-role` permissions policy의 `IamPassRoleToEcsTasks` Statement가 두 Task Role ARN과 정확히 매치 안 됨.

**해결**:
1. `gha-deploy-role-permissions-policy.json`의 PassRole resource ARN 2건이 실제 생성된 Role ARN과 일치 확인
2. `Condition.StringEquals.iam:PassedToService`가 `ecs-tasks.amazonaws.com`인지

### ts008-5: ECS Exec `execute-command` 실패 — `An error occurred (InvalidParameterException)`

**원인**:
- Service가 `enableExecuteCommand: true`로 생성되지 않음
- Task Role에 SSM Messages 4 actions 권한 없음
- Cluster에 ECS Exec configuration이 enabled 안 됨

**해결**:
1. Service 재생성 시 `enableExecuteCommand: true` 명시 (현 `service-prod.json` 포함)
2. `ecs-task-role` permissions에 `EcsExecSsmChannel` Statement 4 actions 확인
3. 또는 `aws ecs update-service --enable-execute-command` 명령으로 기존 Service 갱신 후 새 deployment

### ts008-6: deploymentCircuitBreaker가 자동 롤백을 안 함

**원인**: Circuit Breaker는 **STEADY_STATE 도달 실패** 시에만 발동. 컨테이너가 ALB healthy로 들어왔지만 비즈니스 로직 5xx 폭증은 감지 못 함.

**해결**: 정상. ECS Circuit Breaker의 범위 (헬스체크 실패만). 5xx 폭증 감지 + GHA failure step 자동 롤백은 product Epic 3 별도 Story (Story-047 범위 외 — ADR013/ADR014 follow-up).

---

## 9. 후속 (별도 Story)

- **VPC subnet/SG 치환** — milestone 0.0.1v item #12 완료 후 service JSON placeholder 치환
- **ALB Target Group ARN 치환** — milestone 0.0.1v item #13 완료 후
- **RDS endpoint** — milestone 0.0.1v item #14 완료 후 Secrets Manager `DB_HOST` 값 갱신
- **Secrets Manager 시크릿 실제 등록** — milestone 0.0.1v item #15. Task Def secrets ARN의 `<SUFFIX>` 치환
- **GHA workflow의 `aws-actions/amazon-ecs-deploy-task-definition` 도입** — main push 시 자동 Task Def update + Service deploy. 본 Story 다음.
- **5xx 폭증 자동 롤백 + 5분 모니터링** — product Epic 3 Story 3-1 잔여
- **`S3Config.java` → `DefaultCredentialsProvider` 전환 + Task Def AWS 키 secret 제거** — Task Role 자동 주입으로 대체
- **EC2 SSH 배포 step 제거** — ECS 운영 검증 통과 후

---

## 관련

- [`ts005-dockerfile-build.md`](ts005-dockerfile-build.md) — Task Def가 참조하는 이미지 빌드
- [`ts007-gha-oidc-setup.md`](ts007-gha-oidc-setup.md) — gha-deploy-role 기반 정책 (본 Story에서 확장)
- [ADR013](../../adr/ADR013-gha-oidc-assume-role.md) — OIDC 결정 (본 Story Task IAM Role 분리의 선행 결정)
- [ADR014](../../adr/ADR014-ecs-task-iam-role-separation.md) — Task IAM Role 3종 분리 결정 배경
- Story-047 — milestone 0.0.1v item #11. `workflow/task/milestones/version/0.0.1v/milestone.md`
- 후속: #12 VPC, #13 ALB, #14 RDS, #15 Secrets Manager
