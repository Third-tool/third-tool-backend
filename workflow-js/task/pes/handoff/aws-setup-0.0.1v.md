# [AWS Handoff] 0.0.1v — AWS 콘솔 1회성 셋업 통합 가이드라인

> **본 문서의 역할**: M1(2026-06-23 ~ 06-28, 19 Story) 인프라 Story들이 코드베이스에 spec JSON / runbook / ADR로 deliver한 모든 항목 중 **AWS 콘솔 측 사용자 작업이 필요한 자원**을 한 파일에 통합 정리. 각 항목이 어떤 Story 산출물에 매핑되고 어떤 placeholder를 채우는지 단방향 추적.
>
> **단일 진실 소스**: 각 구성 요소의 상세 절차는 `docs/operations/troubleshooting/ts0NN-*.md` runbook에 있다. 본 문서는 **순서·의존·placeholder 매핑** 통합 뷰 — runbook 분산으로 인한 누락 차단.
>
> **대상 독자**: AWS root 계정만 발급받은 운영자(본인). Claude는 본 문서를 *실행* 못 함 — 작업 체크리스트와 출력 ID 저장소만 제공.
>
> **본 문서 방향성 (v2)**: AWS Web Console UI 절차를 1차 경로로 기재. AWS CLI 명령은 검증·자동화·대안 경로로 병기. 운영자가 콘솔에 익숙해진 뒤 CLI 자동화로 점진적 전환 가능.

작성 시점: 2026-06-29 (M1 종료 직후) · 대상 환경: prod + staging (shared VPC) · 대상 region: `ap-northeast-2` (서울)

---

## 0. 사전 준비

### 0.1 현재 상태 전제

- **AWS root 계정만 보유**. IAM User / SSO Role 없음. AWS CLI 인증 미설정.
- 본 문서는 root → Admin IAM User 발급 → CLI configure까지를 **Stage 0 (§2)** 으로 포함하므로, 별도 사전 준비 없이 Stage 0부터 순차 진입한다.

### 0.2 도구 준비 (로컬 머신)

```bash
aws --version                     # AWS CLI v2 권장 — Stage 0 끝에 `aws configure`로 인증
gh auth status                    # GitHub CLI (Variables 등록용)
jq --version                      # JSON 파싱
```

> `aws configure`는 Stage 0.4에서 Admin User access key 발급 후 실행한다. 본 단계에선 CLI 설치 여부만 확인.

### 0.3 region 고정

**모든 작업 region은 `ap-northeast-2` (서울)**. 다른 region에 자원 생성 시 placeholder 매핑이 깨진다. 콘솔 우측 상단 region selector 항상 확인 — 모든 화면에서 `Asia Pacific (Seoul) ap-northeast-2` 표시 유지.

### 0.4 출력 ID 보관소 (운영자 본인용)

본 문서 진행 중 발행되는 ID(AWS_ACCOUNT_ID, Access key, VPC ID, subnet ID, SG ID, Role ARN, RDS endpoint, Target Group ARN 등)를 안전한 곳에 저장. 예: `~/thirdtool-aws-ids.txt` (gitignore) 또는 1Password vault. **`infra/` 디렉토리에는 절대 commit 금지** (실 계정 ID + 자원 ID 노출).

각 Stage 끝의 "출력 메모" 블록을 그대로 복사해서 보관소에 채워 넣는다.

---

## 1. 셋업 순서 (의존 chain)

각 단계가 다음 단계의 placeholder를 채우거나 자원 의존성을 해결한다. 순서 어기면 후속 단계 실패.

```
[0] root 첫 셋업 (MFA + Budget + Admin IAM User + CLI configure)  — §2 (Stage 0)
       │ AWS_ACCOUNT_ID 파악 + Admin User access key 발행 + CLI 인증 완료
       ▼
[1] OIDC Provider + gha-deploy-role           — ts007 §2-4 / §3 (Stage 1)
       │ GitHub Variables `AWS_ACCOUNT_ID` 등록
       ▼
[2] VPC + 6 subnet + 5 SG + IGW + NAT + 2 RT  — ts009 §2-8 / §4 (Stage 2)
       │ 6 subnet ID + 5 SG ID 발행
       ▼
[3] CloudWatch Log groups (prod + staging)     — ts008 §2 / §5 (Stage 3)
       │ 사전 생성 필수 (Task Def awslogs-create-group=false)
       ▼
[4] ECR repository (third-tool-server + elaticsearch) — §6 (Stage 4)
       │ 기존 dev-cicd가 자동 생성한 상태면 skip
       ▼
[5] Secrets Manager 5종 시크릿 (후속 Story #15) — §7 (Stage 5)
       │ SUFFIX 발행
       ▼
[6] RDS MySQL prod single-AZ (후속 Story #14)  — §8 (Stage 6)
       │ DB endpoint → Secrets Manager DB_HOST 값 갱신
       ▼
[7] ALB + Target Group (Story-049 완료, ts010 존재) — §9 (Stage 7)
       │ TargetGroupArn 발행
       ▼
[8] IAM Role 3종 (ecs-task-execution + ecs-task + gha-deploy 확장) — ts008 §3 / §10 (Stage 8)
       │ Role ARN 발행
       ▼
[9] ECS Cluster 2개 + Task Def 등록 + Service 생성 — ts008 §4-6 / §11 (Stage 9)
       │ placeholder 모두 치환
       ▼
[10] Route53 도메인 등록 (사용자 직접 D1) — §14A (Stage 10)
       │ Hosted Zone + NS 자동 생성
       ▼
[11] ACM 2-region 인증서 신청 (D2) — §14B (Stage 11)
       │ ACM_AP_ARN(ap-ne-2) + ACM_US_ARN(us-e-1) ISSUED
       ▼
[12] ALB HTTPS 리스너 + 443 SG (D3) — §14C (Stage 12)
       │ 443 listener + 80→443 301 redirect 활성
       ▼
[13] S3 FE 버킷 + CloudFront 배포 + OAC (D5+D6) — §14D (Stage 13)
       │ CF_DIST_ID + CF_DOMAIN 발행, OAC로 S3 GetObject scope 잠금
       ▼
[14] Route53 alias 3건 (D4+D7) + 카카오/네이버 OAuth redirect URI 갱신 — §14E (Stage 14)
       │ api.thirdtool.dev + thirdtool.dev + www.thirdtool.dev 활성, FE↔BE E2E 통과
```

**현재 코드베이스 상태 (2026-06-29)**:
- [0] — 본 문서가 단일 진입점. runbook 없음 (1회성 root 셋업).
- [1][2][3][4][8][9] — Story-045/046/047/048 완료. ts007/ts008/ts009 runbook 존재.
- [7] — **Story-049 완료. infra/alb/*.json + ts010 runbook 존재** (구 가이드는 "보류" 표기였으나 본 v2에서 활성화).
- [5][6][10] — 미작성 Story. AWS 셋업도 보류.
- 결과: Stage 0 → 1 → 2 → 3 → 4 → 7 → 8 까지 연결 실행 가능. ECS Service `RUNNING` 상태는 Stage 5·6 (Secrets·RDS) 완료 후.

---

## 2. [Stage 0] root 첫 셋업 — MFA + Budget + Admin IAM User + CLI

**참조**: 없음 (1회성 root 단계). 본 문서가 단일 진입점.

> **목적**: root 계정을 1회 사용 후 봉인하고, 일상 작업은 IAM Admin User로 수행. Budget 알림을 먼저 설정해서 후속 Stage에서 발생할 시간 단위 비용에 안전망을 깐다.

### 2.0 콘솔 절차

#### 2.0.1 root 로그인 + root MFA 활성화 (필수)

1. https://signin.aws.amazon.com/ → **Root user** 선택 → 가입 시 이메일 + 비밀번호 입력
2. 콘솔 우측 상단 계정명 클릭 → **Security credentials**
3. 좌측 **Multi-factor authentication (MFA)** → **Assign MFA device**
   - Device name: `root-totp`
   - MFA device: **Authenticator app** (Google Authenticator / 1Password TOTP / Authy)
   - QR 코드 스캔 → 연속 OTP 2개 입력 → **Add MFA**
4. 로그아웃 → 재로그인 시 MFA 입력되는지 확인

> **이후 root는 절대 일상 사용 금지**. 청구 정보 변경·계정 폐쇄·특정 region 활성화 등 root 전용 작업 외에는 사용하지 않는다. root 비밀번호 + MFA seed는 별도 안전 보관소(1Password vault 분리).

#### 2.0.2 결제 알림 (AWS Budget) 설정

후속 Stage(2/6/7/9)는 시간 단위 과금이라 실수로 켜둔 채 며칠 흐르면 비용이 누적된다. **셋업 첫 단계에서 반드시**.

1. 콘솔 검색창 → **Billing and Cost Management**
2. 좌측 메뉴 **Budgets** → **Create budget**
3. Budget setup:
   - Budget type: **Cost budget — Recommended**
   - Name: `thirdtool-monthly-50usd`
   - Period: **Monthly**
   - Budgeted amount: **$50.00 USD**
4. Configure alerts → **Add an alert threshold**:
   - Threshold 1: 80% of budgeted amount ($40) — Actual cost
   - Threshold 2: 100% ($50) — Actual cost
   - Email recipients: 본인 이메일
5. **Create budget**

> 한도($50)는 본 milestone 예산(`workflow/task/milestones/version/0.0.1v/cost.md`)에 정렬. 본 운영 패턴에서 full deploy 시 ~$34/월 누적 예상이라 안전 마진 충분.

#### 2.0.3 IAM Admin User 발급 (운영자 본인용)

1. 콘솔 검색 → **IAM**
2. 좌측 **Users** → **Create user**
3. User details:
   - User name: `thirdtool-admin` (본인 별칭)
   - **Provide user access to the AWS Management Console** 체크
   - Console password: **Autogenerated** (또는 직접 설정) → User must create a new password at next sign-in 체크
4. Set permissions:
   - **Attach policies directly**
   - 검색창에 `AdministratorAccess` 입력 → 체크 → Next
5. Review → **Create user**
6. 표시되는 **Console sign-in URL** + 임시 비밀번호 1Password에 즉시 저장 (페이지 이탈 시 비밀번호 재조회 불가 — 재설정 필요)

> **`AdministratorAccess` 1개 attach**는 1인 운영자 환경 한정 허용 — 본 milestone(M1)은 다인 운영 아니므로 권한 최소화 분리 작업 불필요. 다인 진입 시 BC별 IAM Group으로 권한 분리.

#### 2.0.4 Admin User access key 발급 (CLI 인증용)

1. 생성된 `thirdtool-admin` 클릭 → **Security credentials** 탭
2. 페이지 중간 **Access keys** 섹션 → **Create access key**
3. Use case: **Command Line Interface (CLI)** → 하단 동의 체크 → Next
4. Description tag (선택): `local-cli-admin` → **Create access key**
5. **Access key ID + Secret access key** 화면에서 1회만 노출:
   - **Show**으로 Secret 노출
   - **Download .csv file** 또는 직접 1Password에 저장
   - 페이지 이탈 시 Secret 재조회 불가능 — 분실하면 access key 삭제 후 재생성

#### 2.0.5 Admin User MFA 활성화 (강력 권장)

같은 user의 Security credentials 탭에서:

1. **Multi-factor authentication (MFA)** 섹션 → **Assign MFA device**
2. Device name: `thirdtool-admin-totp`
3. Authenticator app → QR 스캔 → OTP 2개 입력 → Add MFA

> AccessKey 유출 + MFA 없음 = 계정 탈취. MFA가 있으면 콘솔 로그인은 차단, CLI는 sts:GetSessionToken로 MFA 통과 후 사용 (본 milestone에선 단일 운영자라 CLI MFA는 선택).

#### 2.0.6 Admin User 콘솔 로그인 + AWS CLI 설정

1. root 로그아웃
2. 2.0.3에서 받은 Sign-in URL (예: `https://<AWS_ACCOUNT_ID>.signin.aws.amazon.com/console`) 접속 → IAM user 로그인
   - Account ID: 12자리
   - User name: `thirdtool-admin`
   - Password: 임시 비밀번호 → 첫 로그인 시 새 비밀번호로 변경
3. region을 **Asia Pacific (Seoul) ap-northeast-2**로 고정 (콘솔 우측 상단)
4. 로컬 터미널에서:

   ```bash
   aws configure
   # AWS Access Key ID:        [2.0.4에서 받은 Access key ID]
   # AWS Secret Access Key:    [2.0.4에서 받은 Secret]
   # Default region name:      ap-northeast-2
   # Default output format:    json
   ```

5. 검증:

   ```bash
   aws sts get-caller-identity
   # 출력 예:
   # {
   #   "UserId": "AIDA...",
   #   "Account": "123456789012",
   #   "Arn": "arn:aws:iam::123456789012:user/thirdtool-admin"
   # }
   ```

6. **AWS_ACCOUNT_ID**를 보관소에 기록 (`Account` 필드 12자리).

### 2.1 CLI 절차 (대안)

콘솔 절차가 1회성이라 CLI 변환 가치 낮다. root 단계는 콘솔로만 진행 권장.

CLI로 가능한 부분(예: Admin User 생성·access key 발급)은 root 로그인 후 콘솔에서 임시 IAM User 만들지 않고 직접 root credentials로 CLI를 잠시 쓰는 식 — root credentials 노출 위험이 더 크다. 콘솔 절차 유지.

### 2.2 출력 메모

```
AWS_ACCOUNT_ID=______________              # 12자리. 콘솔 우측 상단 또는 `aws sts get-caller-identity` 출력
ROOT_MFA_DEVICE=root-totp                   # TOTP seed는 1Password 별도 vault
ADMIN_USER_NAME=thirdtool-admin
ADMIN_USER_ARN=arn:aws:iam::<AWS_ACCOUNT_ID>:user/thirdtool-admin
ADMIN_ACCESS_KEY_ID=______________          # ~/.aws/credentials에 보관
# Secret access key는 1Password에만. 파일 commit 금지
ADMIN_MFA_DEVICE=thirdtool-admin-totp
BUDGET_NAME=thirdtool-monthly-50usd         # $50/월, 80%/100% 알림
```

### 2.3 비용 트리거

- 본 Stage 자체는 비용 0
- AWS Budget 알림은 무료 (월 2회 이메일까지)
- 단, Stage 2 진입 즉시 NAT Gateway 시간 과금 시작 → Budget 알림이 첫 안전망

### 2.4 검증

- `aws sts get-caller-identity` → Arn에 `user/thirdtool-admin` 확인
- 콘솔 우측 상단 계정 메뉴 → `thirdtool-admin @ <AWS_ACCOUNT_ID>` 표시
- IAM → Users → thirdtool-admin → "Last activity" 갱신 (1회 로그인 후 시간 표기)
- Billing → Budgets → `thirdtool-monthly-50usd` 표시 + ALARM 미발생 상태
- root 로그아웃 후 재로그인 시 MFA 입력 화면 표시

---

## 3. [Stage 1] OIDC Provider + gha-deploy-role

**참조**: `docs/operations/troubleshooting/ts007-gha-oidc-setup.md`, `infra/iam/gha-deploy-role-*.json`, ADR013

### 3.0 콘솔 절차

> Stage 0의 Admin User로 로그인된 상태에서 진행.

#### 3.0.1 OIDC Identity Provider 등록 (계정당 1회)

1. **IAM** → 좌측 **Identity providers** → **Add provider**
2. Provider configuration:
   - Provider type: **OpenID Connect**
   - Provider URL: `https://token.actions.githubusercontent.com`
   - **Get thumbprint** 버튼 클릭 → 자동 채워짐
   - Audience: `sts.amazonaws.com`
3. **Add provider**

기존에 등록되어 있으면 skip.

#### 3.0.2 `gha-deploy-role` IAM Role 생성

1. IAM → 좌측 **Roles** → **Create role**
2. Trusted entity type: **Web identity**
3. Web identity:
   - Identity provider: `token.actions.githubusercontent.com`
   - Audience: `sts.amazonaws.com`
   - GitHub organization / repo 필터는 비워둠 (Trust policy를 JSON으로 덮어쓸 예정) → Next
4. Add permissions: 검색 없이 그대로 Next (inline policy로 별도 attach)
5. Role details:
   - Role name: `gha-deploy-role`
   - Description: `GitHub Actions OIDC AssumeRole for ECR/ECS deploy`
   - **Create role**
6. 생성된 Role 클릭 → **Trust relationships** 탭 → **Edit trust policy**:
   - 에디터를 비우고 `infra/iam/gha-deploy-role-trust-policy.json` 내용 그대로 붙여넣기
   - `<AWS_ACCOUNT_ID>`를 Stage 0 출력의 12자리로 치환
   - **Update policy**
7. **Permissions** 탭 → **Add permissions** → **Create inline policy** → **JSON** 탭
   - `infra/iam/gha-deploy-role-permissions-policy.json` 내용 + `<AWS_ACCOUNT_ID>` 치환 → Next
   - Policy name: `gha-deploy-permissions`
   - **Create policy**
8. **치환 누락 검증**: Trust policy + Permissions policy 화면에서 `<` 문자 검색 → 0건

#### 3.0.3 GitHub Variables 등록

1. GitHub 해당 repo (`Third-tool/third-tool`) → Settings → Secrets and variables → **Actions**
2. **Variables** 탭 → **New repository variable**
3. Name: `AWS_ACCOUNT_ID` / Value: 12자리 계정 ID → **Add variable**

> 비밀이 아닌 식별자이므로 Secrets가 아닌 Variables로 등록 (workflow 로그에 표시 OK).

### 3.1 CLI 절차 (대안 / 자동화)

```bash
# 1. OIDC Provider
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1

# 2. Role 생성
sed "s/<AWS_ACCOUNT_ID>/$AWS_ACCOUNT_ID/g" infra/iam/gha-deploy-role-trust-policy.json > /tmp/trust.json
aws iam create-role --role-name gha-deploy-role --assume-role-policy-document file:///tmp/trust.json

# 3. Inline policy attach
sed "s/<AWS_ACCOUNT_ID>/$AWS_ACCOUNT_ID/g" infra/iam/gha-deploy-role-permissions-policy.json > /tmp/perm.json
aws iam put-role-policy --role-name gha-deploy-role --policy-name gha-deploy-permissions --policy-document file:///tmp/perm.json

# 4. GitHub Variables (gh CLI)
gh variable set AWS_ACCOUNT_ID --body "$AWS_ACCOUNT_ID"
```

### 3.2 출력 메모

```
OIDC_PROVIDER_ARN=arn:aws:iam::<AWS_ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com
GHA_DEPLOY_ROLE_ARN=arn:aws:iam::<AWS_ACCOUNT_ID>:role/gha-deploy-role
```

### 3.3 검증

```bash
gh workflow run dev-cicd.yml --ref main -f build_es=true
```

GHA 로그에서 `Authenticated as assumedRoleId: AROA...:GitHubActions` 라인 + AWS CloudTrail Event history 에서 `AssumeRoleWithWebIdentity` 이벤트 1건 확인.

---

## 4. [Stage 2] VPC + 6 subnet + 5 SG + IGW + NAT + 2 route table

**참조**: `docs/operations/troubleshooting/ts009-vpc-setup.md`, `infra/vpc/vpc-spec.json`, `infra/vpc/security-groups.json`, `docs/architecture/vpc-topology.md`, ADR015

### 4.0 콘솔 절차 (개별 자원 생성)

> AWS 콘솔의 **VPC and more** 마법사는 본 spec과 1:1 매핑되지 않음 (subnet 이름·CIDR·갯수 자유도 부족). 개별 자원으로 진행.

#### 4.0.1 VPC

1. **VPC** → 좌측 **Your VPCs** → **Create VPC**
2. Resources to create: **VPC only**
3. Settings:
   - Name tag: `thirdtool-vpc`
   - IPv4 CIDR: `10.0.0.0/16`
   - IPv6 CIDR block: No IPv6
   - Tenancy: Default
   - Tags: `Project=thirdtool`, `Environment=shared`, `ManagedBy=manual`
4. **Create VPC**
5. 생성된 VPC 우클릭 → **Edit VPC settings** → **Enable DNS hostnames** + **Enable DNS resolution** 체크 → Save

#### 4.0.2 6 subnet

VPC → **Subnets** → **Create subnet** → VPC: `thirdtool-vpc` → 하단 **Add new subnet**을 5회 반복해 총 6개 묶음으로 생성:

| Subnet name | AZ | CIDR | 용도 |
| --- | --- | --- | --- |
| `thirdtool-pub-2a` | ap-northeast-2a | `10.0.0.0/24` | ALB target + NAT 위치 |
| `thirdtool-pub-2c` | ap-northeast-2c | `10.0.1.0/24` | ALB target (multi-AZ) |
| `thirdtool-app-2a` | ap-northeast-2a | `10.0.10.0/24` | ECS Task 2a |
| `thirdtool-app-2c` | ap-northeast-2c | `10.0.11.0/24` | ECS Task 2c |
| `thirdtool-data-2a` | ap-northeast-2a | `10.0.20.0/24` | RDS primary |
| `thirdtool-data-2c` | ap-northeast-2c | `10.0.21.0/24` | RDS standby 예비 (M2) |

**Create subnet** 클릭 후, `thirdtool-pub-2a` / `thirdtool-pub-2c` 두 개에 대해:

- subnet 선택 → Actions → **Edit subnet settings** → **Enable auto-assign public IPv4 address** 체크 → Save

#### 4.0.3 Internet Gateway

1. VPC → **Internet gateways** → **Create internet gateway**
2. Name tag: `thirdtool-igw` → Create
3. 생성된 IGW 선택 → Actions → **Attach to VPC** → `thirdtool-vpc` → Attach

#### 4.0.4 NAT Gateway

1. VPC → **NAT gateways** → **Create NAT gateway**
2. Settings:
   - Name: `thirdtool-nat`
   - Subnet: `thirdtool-pub-2a` (public AZ-a 배치)
   - Connectivity type: **Public**
   - Elastic IP allocation ID: **Allocate Elastic IP** 클릭 → 새 EIP 자동 발급
3. **Create NAT gateway**
4. State가 `Available`이 될 때까지 1-2분 대기 (목록 새로고침)

#### 4.0.5 Route Tables (2개)

1. VPC → **Route tables** → **Create route table**
2. `thirdtool-public-rt`:
   - Name: `thirdtool-public-rt` / VPC: `thirdtool-vpc` → Create
   - 생성된 RT 선택 → **Routes** 탭 → **Edit routes** → **Add route**:
     - Destination: `0.0.0.0/0` / Target: **Internet Gateway** → `thirdtool-igw`
     - Save changes
   - **Subnet associations** 탭 → **Edit subnet associations** → `thirdtool-pub-2a`, `thirdtool-pub-2c` 체크 → Save
3. `thirdtool-private-rt` 동일 방식:
   - Route: `0.0.0.0/0` → **NAT Gateway** → `thirdtool-nat`
   - Subnet associations: `thirdtool-app-2a`, `thirdtool-app-2c`, `thirdtool-data-2a`, `thirdtool-data-2c` (4개 private 전부)

#### 4.0.6 Security Groups (생성 순서 강제)

**순서 강제 이유**: `app-sg`는 `alb-sg`를 source로 참조, `db-sg`는 `app-sg`를 source로 참조. 미존재 SG는 source 드롭다운에서 선택 불가.

VPC → **Security groups** → **Create security group** (각각 별도 호출):

1. **`thirdtool-alb-sg`** (최우선)
   - VPC: `thirdtool-vpc`
   - Inbound rules:
     - HTTP (80) / Anywhere-IPv4 (0.0.0.0/0)
     - HTTPS (443) / Anywhere-IPv4 (0.0.0.0/0)
   - Outbound rules: 기본(All traffic 0.0.0.0/0) 유지
   - Create

2. **`thirdtool-app-sg`**
   - Inbound: Custom TCP / Port 8080 / Source type: Security group → `thirdtool-alb-sg`
   - Outbound: 기본 유지 (NAT를 통한 outbound 필요)
   - Create

3. **`thirdtool-db-sg`**
   - Inbound: MySQL/Aurora (3306) / Source: `thirdtool-app-sg`
   - Outbound: 기본으로 생성 후 → **default all-egress 명시 제거** (drift 차단):
     - 생성 직후 db-sg 선택 → **Outbound rules** 탭 → **Edit outbound rules**
     - 기본 `0.0.0.0/0` 행 **Delete** → Save (egress 규칙 0건 = 명시적 차단)

4. **`thirdtool-bastion-sg`** (미사용 placeholder)
   - Inbound: 비워둠 (SSM Session Manager 채택 — SSH 22 미사용)
   - Outbound: 기본 유지
   - Create

5. **`thirdtool-vpc-endpoint-sg`** (M2 VPC Endpoint 대비)
   - Inbound: HTTPS (443) / Source: `thirdtool-app-sg`
   - Outbound: db-sg와 동일하게 `0.0.0.0/0` 삭제 → 0건

### 4.1 CLI 절차 (대안 / 자동화)

순서대로 (ts009 §2-7 그대로):

1. VPC `10.0.0.0/16` 생성 + DNS attribute 활성
2. 6 subnet 생성 — pub-2{a,c}, app-2{a,c}, data-2{a,c}
3. IGW 생성 + VPC attach
4. NAT Gateway 생성 (EIP 1개 → pub-2a 배치, AVAILABLE 대기 1-2분)
5. Route Table 2개 (public-rt → IGW, private-rt → NAT) + subnet 연결
6. 5 SG 생성 — **creationOrder 강제**: alb-sg → app-sg → db-sg → bastion-sg → vpc-endpoint-sg
7. db-sg + vpc-endpoint-sg는 `revoke-security-group-egress`로 default all-egress 명시 제거

상세 CLI 명령은 ts009-vpc-setup.md §2-7 참조.

### 4.2 출력 메모

```
VPC_ID=______________

# Subnet IDs (6개)
PUB_2A=subnet-______________   # ALB target + NAT 위치
PUB_2C=subnet-______________   # ALB target multi-AZ
APP_2A=subnet-______________   # ECS Task 2a → service-prod.json <APP_SUBNET_2A>
APP_2C=subnet-______________   # ECS Task 2c → service-prod.json <APP_SUBNET_2C>
DATA_2A=subnet-______________  # RDS primary
DATA_2C=subnet-______________  # RDS standby 예비 (M2)

# Security Group IDs (5개)
ALB_SG=sg-______________       # → 후속 Stage 7 ALB
APP_SG=sg-______________       # → service-prod.json <APP_SG>
DB_SG=sg-______________        # → 후속 Stage 6 RDS
BASTION_SG=sg-______________   # 미사용 (SSM Session Manager 채택)
VPCE_SG=sg-______________      # M2 VPC Endpoint 대비

# 기타
IGW_ID=igw-______________
NAT_ID=nat-______________
PUB_RT=rtb-______________
PRIV_RT=rtb-______________
EIP_ALLOC_ID=eipalloc-______________
```

### 4.3 비용 트리거

- NAT Gateway 시간 비용 즉시 시작: **$0.06/시간 = $1.44/일**
- 셋업 후 시간 단위 과금 — **종료 시 NAT 삭제 + EIP release 필수** (release 안 하면 EIP 유휴 비용 $0.005/시간)

### 4.4 검증

```bash
aws ec2 describe-vpcs --vpc-ids $VPC_ID --region ap-northeast-2 | jq '.Vpcs[].State'
# "available"

aws ec2 describe-nat-gateways --nat-gateway-ids $NAT_ID --region ap-northeast-2 | jq '.NatGateways[].State'
# "available"

aws ec2 describe-route-tables --route-table-ids $PUB_RT $PRIV_RT --region ap-northeast-2 \
  | jq '.RouteTables[] | {Name: (.Tags[]|select(.Key=="Name").Value), Routes: .Routes[]}'
# public-rt에 0.0.0.0/0 → IGW, private-rt에 0.0.0.0/0 → NAT
```

콘솔 검증: VPC → **Resource map** 페이지에서 1 VPC, 6 subnet, 2 RT, 1 IGW, 1 NAT의 연결 다이어그램 시각 확인.

---

## 5. [Stage 3] CloudWatch Log groups

**참조**: `docs/operations/troubleshooting/ts008-ecs-cluster-setup.md` §2

Task Def `awslogs-create-group: "false"` 명시 — 사전 생성 필수.

### 5.0 콘솔 절차

1. **CloudWatch** → 좌측 **Log groups** → **Create log group**
2. `/ecs/thirdtool-prod`:
   - Log group name: `/ecs/thirdtool-prod`
   - Retention setting: **30 days**
   - Create
3. `/ecs/thirdtool-staging`:
   - Log group name: `/ecs/thirdtool-staging`
   - Retention setting: **1 week**
   - Create

retention: prod 30일 / staging 7일 (product Epic 2 Q "로그 보존 정책" Option B).

### 5.1 CLI 절차 (대안)

```bash
aws logs create-log-group --log-group-name /ecs/thirdtool-prod --region ap-northeast-2
aws logs put-retention-policy --log-group-name /ecs/thirdtool-prod --retention-in-days 30 --region ap-northeast-2

aws logs create-log-group --log-group-name /ecs/thirdtool-staging --region ap-northeast-2
aws logs put-retention-policy --log-group-name /ecs/thirdtool-staging --retention-in-days 7 --region ap-northeast-2
```

### 5.2 검증

CloudWatch → Log groups → 두 그룹이 표시되고 Retention 컬럼이 각각 30 days / 1 week.

---

## 6. [Stage 4] ECR Repository

**참조**: 기존 `dev-cicd.yml` 이미 사용 중. 미존재 시 1회 생성.

### 6.0 콘솔 절차

1. **Amazon ECR** → 좌측 **Repositories** → 상단 **Private** 탭 → **Create repository**
2. Visibility settings: **Private**
3. Repository name: `third-tool-server`
4. Image tag mutability: **Mutable** (기본 — `latest` 태그 덮어쓰기 허용)
5. Encryption: AES-256 (기본)
6. **Create repository**
7. 동일 절차로 `third-tool-elaticsearch` (**오타 그대로** — IAM 정책/Task Def 정합)

### 6.1 CLI 절차 (대안 / 검증)

```bash
aws ecr describe-repositories --region ap-northeast-2 | jq '.repositories[].repositoryName'
# 다음 두 이름이 있어야 함:
# - third-tool-server
# - third-tool-elaticsearch  (오타 그대로)

# 미존재 시 생성:
aws ecr create-repository --repository-name third-tool-server --region ap-northeast-2
aws ecr create-repository --repository-name third-tool-elaticsearch --region ap-northeast-2
```

> **오타 보존 사유**: 기존 `dev-cicd.yml` + `infra/iam/*.json` + `infra/ecs/*.json`이 모두 `third-tool-elaticsearch`로 일치. 정정은 후속 Story (모든 의존 파일 동시 변경).

### 6.2 라이프사이클 정책 (선택)

milestone Tier 2 #17 진입 시:

```bash
aws ecr put-lifecycle-policy --repository-name third-tool-server \
  --lifecycle-policy-text file://infra/ecr/lifecycle-policy.json --region ap-northeast-2
```

콘솔: ECR → Repository → Lifecycle policy 탭 → Create rule.

---

## 7. [Stage 5] Secrets Manager 5종 시크릿 — **Story-052 활성**

**참조**: `docs/operations/troubleshooting/ts012-secrets-manager-setup.md` · `docs/adr/ADR019-secrets-manager-naming-and-task-role-scope.md` · `infra/secrets/` · `infra/iam/ecs-task-role-permissions-policy.json` patch.

> Stage 6 (RDS) 종료 후 진입 권장 — `db-credential`의 HOST/PORT/NAME에 RDS endpoint를 채워야 함. 나머지 4종(jwt-secret · oauth-kakao · oauth-naver · gemini-api-key)은 Stage 6 없이도 등록 가능.

### 7.1 등록할 시크릿 5개 (env: dev — M1 합격선)

| Secret name | JSON 키 | 값 출처 |
| --- | --- | --- |
| `thirdtool/dev/db-credential` | HOST, PORT, NAME, USERNAME, PASSWORD | RDS endpoint (Stage 6) + DB master password |
| `thirdtool/dev/jwt-secret` | SECRET_KEY | `openssl rand -base64 48` (384 bit ≥ HS512 최소) |
| `thirdtool/dev/oauth-kakao` | CLIENT_ID, CLIENT_SECRET | 카카오 developers (ts006 §2.1) |
| `thirdtool/dev/oauth-naver` | CLIENT_ID, CLIENT_SECRET | 네이버 developers (ts006 §2.2) |
| `thirdtool/dev/gemini-api-key` | API_KEY | Google AI Studio — M1은 placeholder OK (AI Epic 3 진입 전 미사용) |

prod·staging도 동일 구조로 prefix만 교체 (`thirdtool/prod/...` · `thirdtool/staging/...`). M1 합격선은 dev only — 다른 env는 환경 분리 Epic 진입 시.

### 7.2 콘솔 절차 (env마다 5회 반복)

ts012 §4 절차 그대로:

1. **Secrets Manager** → **Store a new secret**
2. Secret type: **Other type of secret**
3. Key/value pairs 탭에서 §7.1 표의 JSON 키 입력
4. Encryption key: `aws/secretsmanager` (AWS managed, 비용 0) → Next
5. Secret name: 정확히 `thirdtool/dev/db-credential` 등
6. Description + Tags (Project=ThirdTool, Environment=dev, ManagedBy=Story-052) → Next
7. **Disable automatic rotation** (ADR019 §1.3 — M1 수동) → Next
8. Store → 생성된 시크릿 ARN 끝 random 6 char suffix 메모

### 7.3 CLI 절차 (자동화 — 권장)

ts012 §3 명령 5회 (db-credential · jwt-secret · oauth-kakao · oauth-naver · gemini-api-key). `--region ap-northeast-2` 고정.

각 명령 후 응답의 ARN 끝 suffix(`-AbCd9X`)를 §7.4 보관소에 즉시 기록.

### 7.4 출력 메모

```
SECRETS_SUFFIX_DEV_DB_CREDENTIAL=______________   # 시크릿마다 다른 suffix
SECRETS_SUFFIX_DEV_JWT_SECRET=______________
SECRETS_SUFFIX_DEV_OAUTH_KAKAO=______________
SECRETS_SUFFIX_DEV_OAUTH_NAVER=______________
SECRETS_SUFFIX_DEV_GEMINI_API_KEY=______________
SECRETS_SUFFIX_PROD_*=______________              # prod 진입 시 5건
```

### 7.5 IAM Task Role 권한 patch 확인

Story-052가 `infra/iam/ecs-task-role-permissions-policy.json`에 2개 Statement를 추가했다:
- `SecretsManagerReadOwnEnvPrefixes` — 10 ARN 와일드카드(`thirdtool/dev/*-*` + `thirdtool/prod/*-*`)
- `KMSDecryptForSecretsManager` — `aws/secretsmanager` (or CMK) Decrypt 권한

Stage 8 (IAM Role 생성) 진입 시 본 정책 그대로 적용 — ts008 §3에 명시.

### 7.6 GitHub Secrets 폐기 (선행 조건: Stage 9 부팅 검증 통과 후)

Stage 9 (ECS Service RUNNING + Task healthy) 통과 후 GitHub Actions Secrets에서 다음 4건 수동 삭제:
- `DB_PASSWORD` · `JWT_SECRET_KEY` · `KAKAO_CLIENT_SECRET` · `NAVER_CLIENT_SECRET`

미리 폐기 시 dev-cicd.yml 환경변수 전달 step에서 부팅 실패. ts012 §8 절차.

### 7.7 검증 명령 (5건 등록 직후)

```bash
aws secretsmanager list-secrets \
  --filters Key=name,Values=thirdtool/dev/ --region ap-northeast-2 \
  | jq -r '.SecretList[].Name'
# thirdtool/dev/db-credential
# thirdtool/dev/jwt-secret
# thirdtool/dev/oauth-kakao
# thirdtool/dev/oauth-naver
# thirdtool/dev/gemini-api-key
```

상세 검증 + 트러블슈팅(ts012-1~6): ts012 §6·§7.

### 7.8 현재 상태

- **2026-06-30 (Story-052 머지 시점)**: spec + ts012 + ADR019 + IAM patch 코드 완료. 사용자 본 Stage 5건 등록 + Task Def `<SUFFIX>` 치환 후 Stage 9 진입 가능.
- **Stage 9 의존**: Stage 5 완료 + Stage 6 (RDS) endpoint 채움 + Task Def revision 갱신. 누락 시 Task 부팅 시 secrets 해석 실패 (ts008-1).

---

## 8. [Stage 6] RDS MySQL prod single-AZ — **후속 Story #14**

**참조**: 미작성 Story. ts011 runbook 미존재 — #14 진입 시 구체화.

### 8.0 콘솔 절차 (예상)

#### 8.0.1 DB Subnet Group 사전 생성

1. **RDS** → 좌측 **Subnet groups** → **Create DB subnet group**
2. Name: `thirdtool-db-subnet-group`
3. VPC: `thirdtool-vpc`
4. Availability Zones: `ap-northeast-2a`, `ap-northeast-2c` 체크
5. Subnets: `thirdtool-data-2a` (10.0.20.0/24), `thirdtool-data-2c` (10.0.21.0/24) 체크
6. Create

#### 8.0.2 DB 인스턴스 생성

1. **RDS** → **Databases** → **Create database**
2. Choose a database creation method: **Standard create**
3. Engine: **MySQL** / Version: 8.0.x (최신 minor)
4. Templates: **Production** (운영 기본값 적용) — 비용 최적화를 위해 다음 단계에서 hand-tune
5. Settings:
   - DB instance identifier: `thirdtool-prod-db`
   - Master username: `admin`
   - Credentials management: **Self managed** → Master password 직접 입력 (또는 Auto generate)
6. Instance configuration:
   - DB instance class: **Burstable classes** → **db.t4g.micro**
7. Storage:
   - Storage type: gp3 / Allocated 20 GiB / Storage autoscaling: 활성 (max 100 GiB)
8. Availability & durability: **Single DB instance** (M2에서 Multi-AZ로 전환)
9. Connectivity:
   - VPC: `thirdtool-vpc`
   - DB subnet group: `thirdtool-db-subnet-group`
   - Public access: **No**
   - VPC security group: **Choose existing** → `thirdtool-db-sg`
   - Availability Zone: `ap-northeast-2a` (data-2a)
10. Database authentication: **Password authentication**
11. Additional configuration:
    - Initial database name: `thirdtool`
    - DB parameter group: default-mysql8.0
    - Backup: Enable + Retention 7 days
    - Encryption: Enable + KMS key `aws/rds`
    - Performance Insights: Enable (free tier 7일 보존)
    - Enhanced monitoring: 비활성 (M1 비용 절감)
    - Maintenance window: 적정 시각 (예: sat:18:00-19:00 UTC = 토 03:00 KST)
12. **Create database** (5-10분 소요)

### 8.1 예상 구조 요약

| 항목 | 값 |
| --- | --- |
| 엔진 | MySQL 8.0 |
| 인스턴스 클래스 | `db.t4g.micro` (cost.md baseline) |
| Multi-AZ | **single-AZ (M1)** — M2에서 standby 활성 |
| 스토리지 | 20 GB gp3 |
| Subnet group | DB Subnet Group: data-2a + data-2c |
| Security Group | `$DB_SG` (Stage 2 발행) |
| 백업 | 7일 보존 + 자동 |
| 도메인 인증 | 비활성 (IAM 인증은 M2 검토) |
| 암호화 (storage) | 활성 (KMS default) |
| 퍼블릭 액세스 | **비활성** (data subnet은 private) |
| Parameter group | default-mysql8.0 (utf8mb4 변경 필요 시 커스텀) |

### 8.2 출력 메모

```
RDS_ENDPOINT=thirdtool-prod-db.cluster-______________.ap-northeast-2.rds.amazonaws.com   # → Secrets Manager `thirdtool/prod/db:HOST`
DB_NAME=thirdtool                                 # 운영자 결정
DB_USERNAME=admin
DB_PASSWORD=______________                        # 8.0.2에서 입력/발급한 값. 1Password에 즉시 저장
```

### 8.3 Flyway 마이그레이션 실행

RDS 생성 후 dev profile로 Spring Boot 1회 부팅하면 Flyway가 `V*.sql` 마이그레이션 자동 실행. ECS Task로 부팅 시 Stage 9 진입 후 자동.

또는 로컬에서 RDS endpoint 직접 가리켜 1회 실행 (Bastion 또는 SSM tunnel 필요).

### 8.4 ⚠️ 현재 상태

본 Stage 보류 — Story #14 미작성.

---

## 9. [Stage 7] ALB + Target Group — Story-049 완료, ts010 존재

**참조**: `docs/operations/troubleshooting/ts010-alb-setup.md`, `infra/alb/alb-spec.json`, `infra/alb/target-group-spec.json`, ADR016 (Story-049)

> **v2 갱신**: 구 가이드는 "Story #13 미작성, 보류"였으나 Story-049 (2026-06-29) 종료로 spec JSON + ts010 runbook 입수. 본 Stage 활성화.

### 9.0 콘솔 절차

#### 9.0.1 Target Group 먼저 생성 (Listener는 TG 참조)

1. **EC2** → 좌측 **Target groups** → **Create target group**
2. Basic configuration:
   - Choose a target type: **IP addresses** (Fargate awsvpc 모드 필수 — instance 등록 불가)
   - Target group name: `thirdtool-prod-tg`
   - Protocol: **HTTP** / Port: `8080`
   - VPC: `thirdtool-vpc`
   - Protocol version: **HTTP/1.1**
3. Health checks:
   - Health check protocol: HTTP
   - Health check path: `/health`
   - Advanced health check settings:
     - Healthy threshold: 2
     - Unhealthy threshold: 2
     - Timeout: 5s
     - Interval: 30s
     - Success codes: 200
4. Next → Register targets: **비워둠** (ECS Service가 자동 등록) → **Create target group**
5. staging도 동일 절차로 `thirdtool-staging-tg` 생성

#### 9.0.2 ACM 인증서 (선택 — 도메인 보유 시만)

도메인이 있고 HTTPS Listener 필요한 경우만:

1. **Certificate Manager (ACM)** → **Request certificate**
2. Certificate type: **Public certificate**
3. Fully qualified domain name: `*.thirdtool.dev` (와일드카드)
4. Validation method: **DNS validation**
5. Request → 발급된 인증서 클릭 → Route 53에 CNAME 자동 추가 옵션 → 활성화 (Route 53에서 도메인 보유 시)
6. Status가 **Issued**가 될 때까지 대기 (수 분 ~ 수십 분)

도메인 미보유 시 ALB 기본 도메인 (예: `thirdtool-prod-alb-NNNNN.ap-northeast-2.elb.amazonaws.com`) 사용 — HTTP만 가능.

#### 9.0.3 Application Load Balancer 생성

1. **EC2** → 좌측 **Load balancers** → **Create load balancer**
2. Load balancer types: **Application Load Balancer** → Create
3. Basic configuration:
   - Name: `thirdtool-prod-alb`
   - Scheme: **Internet-facing**
   - IP address type: **IPv4**
4. Network mapping:
   - VPC: `thirdtool-vpc`
   - Mappings: `ap-northeast-2a` → `thirdtool-pub-2a`, `ap-northeast-2c` → `thirdtool-pub-2c` (둘 다 체크)
5. Security groups:
   - default SG 제거 → `thirdtool-alb-sg` 추가
6. Listeners and routing:
   - **Listener 1**: HTTP:80
     - Default action: **Redirect to** HTTPS:443 — Status code: 301 (도메인 있을 때)
     - 도메인 없을 때: **Forward to** `thirdtool-prod-tg`
   - **Listener 2** (도메인 있을 때만 추가): HTTPS:443
     - Default action: **Forward to** `thirdtool-prod-tg`
     - SSL/TLS certificate: ACM 인증서 선택 (9.0.2에서 발급)
     - Security policy: `ELBSecurityPolicy-TLS13-1-2-2021-06` (기본)
7. **Create load balancer** (1-2분 소요)
8. staging도 동일 절차로 `thirdtool-staging-alb` 생성

### 9.1 예상 구조 요약

| 항목 | prod | staging |
| --- | --- | --- |
| ALB scheme | `internet-facing` | `internet-facing` |
| Subnets | pub-2a, pub-2c | pub-2a, pub-2c |
| Security Group | `$ALB_SG` | `$ALB_SG` (또는 staging 전용 별도 SG) |
| Listener 80 | redirect to 443 (HTTPS) — 도메인 시. 없으면 forward 80→8080 | 동일 |
| Listener 443 | forward to Target Group + ACM cert (도메인 시) | 동일 |
| Target Group | `thirdtool-prod-tg` (**target-type: ip 필수**) | `thirdtool-staging-tg` |
| Target Group port | 8080 | 8080 |
| Health check path | `/health` | `/health` |
| Health check interval | 30s, threshold 2 | 동일 |

### 9.2 CLI 절차 (대안)

ts010-alb-setup.md 참조. 주요 명령:

```bash
# Target Group (target-type=ip 명시)
aws elbv2 create-target-group \
  --name thirdtool-prod-tg \
  --protocol HTTP --port 8080 \
  --vpc-id $VPC_ID \
  --target-type ip \
  --health-check-path /health \
  --region ap-northeast-2

# ALB
aws elbv2 create-load-balancer \
  --name thirdtool-prod-alb \
  --subnets $PUB_2A $PUB_2C \
  --security-groups $ALB_SG \
  --scheme internet-facing \
  --type application \
  --region ap-northeast-2

# Listener (80 → 443 redirect 가정)
aws elbv2 create-listener \
  --load-balancer-arn $PROD_ALB_ARN \
  --protocol HTTP --port 80 \
  --default-actions 'Type=redirect,RedirectConfig={Protocol=HTTPS,Port=443,StatusCode=HTTP_301}'

aws elbv2 create-listener \
  --load-balancer-arn $PROD_ALB_ARN \
  --protocol HTTPS --port 443 \
  --certificates CertificateArn=$ACM_CERT_ARN \
  --default-actions Type=forward,TargetGroupArn=$PROD_TG_ARN
```

### 9.3 출력 메모

```
PROD_ALB_ARN=arn:aws:elasticloadbalancing:ap-northeast-2:<acct>:loadbalancer/app/thirdtool-prod-alb/______________
PROD_TG_ARN=arn:aws:elasticloadbalancing:ap-northeast-2:<acct>:targetgroup/thirdtool-prod-tg/______________
# → infra/ecs/service-prod.json <PROD_TARGET_GROUP_ARN>

STAGING_ALB_ARN=______________
STAGING_TG_ARN=______________
# → infra/ecs/service-staging.json <STAGING_TARGET_GROUP_ARN>

PROD_ALB_DNS=thirdtool-prod-alb-NNNN.ap-northeast-2.elb.amazonaws.com   # 도메인 미보유 시 그대로 사용
```

### 9.4 비용 트리거

- ALB 시간 비용: $0.0225/시간 + LCU 사용량 — ~$0.80/일
- Target Group 자체는 비용 없음

### 9.5 검증

```bash
aws elbv2 describe-load-balancers --names thirdtool-prod-alb --region ap-northeast-2 | jq '.LoadBalancers[].State.Code'
# "active"

aws elbv2 describe-target-health --target-group-arn $PROD_TG_ARN --region ap-northeast-2
# Stage 9 ECS Service 생성 전엔 target 0건이 정상
```

콘솔: EC2 → Load balancers → ALB 클릭 → **DNS name** 복사 → 브라우저 접속 → 502/504 정상 (target 미등록 상태).

---

## 10. [Stage 8] IAM Role 3종 (ecs-task-execution + ecs-task + gha-deploy 확장)

**참조**: `docs/operations/troubleshooting/ts008-ecs-cluster-setup.md` §3, `infra/iam/ecs-task-*-role-*.json`, ADR014

### 10.0 콘솔 절차

#### 10.0.1 `gha-deploy-role` permissions 확장 (Stage 1에서 생성한 Role의 inline policy 갱신)

1. IAM → Roles → `gha-deploy-role` 클릭
2. **Permissions** 탭 → 기존 inline policy `gha-deploy-permissions` 우측 ⋮ → **Edit**
3. JSON 편집기에서 `infra/iam/gha-deploy-role-permissions-policy.json` 최신 내용 + `<AWS_ACCOUNT_ID>` 치환으로 덮어쓰기
4. 기존 `EcrAuthToken` + `EcrPushPull` 보존되었는지 확인 + `EcsDeploy` + `IamPassRoleToEcsTasks` Statement 2건 추가됨
5. **Save changes**

#### 10.0.2 `ecs-task-execution-role` 신규 생성

1. IAM → Roles → **Create role**
2. Trusted entity type: **AWS service**
3. Use case: **Elastic Container Service** → 하위 옵션 **Elastic Container Service Task** → Next
4. Add permissions: 검색 없이 그대로 Next (inline policy 별도 attach)
5. Role name: `ecs-task-execution-role` → Create role
6. **Trust relationships** 탭 → **Edit trust policy** → `infra/iam/ecs-task-execution-role-trust-policy.json` 그대로 붙여넣기 → Update (치환 불요)
7. **Permissions** 탭 → **Add permissions** → **Create inline policy** → JSON
   - `infra/iam/ecs-task-execution-role-permissions-policy.json` + `<AWS_ACCOUNT_ID>` 치환
   - Policy name: `ecs-task-execution-permissions` → Create policy

#### 10.0.3 `ecs-task-role` 신규 생성

10.0.2와 동일 방식으로:
- Role name: `ecs-task-role`
- Trust: `infra/iam/ecs-task-role-trust-policy.json` (치환 불요)
- Inline policy: `ecs-task-role-permissions-policy.json` + `<AWS_ACCOUNT_ID>` 치환
- Policy name: `ecs-task-permissions`

### 10.1 CLI 절차 (대안)

ts008-ecs-cluster-setup.md §3 참조. 주요 패턴:

```bash
# 1. Role 생성 (trust policy 동시 적용)
aws iam create-role --role-name ecs-task-execution-role \
  --assume-role-policy-document file://infra/iam/ecs-task-execution-role-trust-policy.json

# 2. Inline policy attach (AWS_ACCOUNT_ID 치환된 임시 파일)
sed "s/<AWS_ACCOUNT_ID>/$AWS_ACCOUNT_ID/g" infra/iam/ecs-task-execution-role-permissions-policy.json > /tmp/perm.json
aws iam put-role-policy --role-name ecs-task-execution-role \
  --policy-name ecs-task-execution-permissions --policy-document file:///tmp/perm.json
```

### 10.2 출력 메모

```
ECS_TASK_EXECUTION_ROLE_ARN=arn:aws:iam::<acct>:role/ecs-task-execution-role
ECS_TASK_ROLE_ARN=arn:aws:iam::<acct>:role/ecs-task-role
# → Task Def `executionRoleArn` / `taskRoleArn`
```

### 10.3 검증

```bash
aws iam get-role --role-name ecs-task-execution-role | jq '.Role.AssumeRolePolicyDocument.Statement[].Principal'
# {"Service": "ecs-tasks.amazonaws.com"}

aws iam list-role-policies --role-name ecs-task-execution-role
# "PolicyNames": ["ecs-task-execution-permissions"]
```

---

## 11. [Stage 9] ECS Cluster + Task Def + Service

**참조**: `docs/operations/troubleshooting/ts008-ecs-cluster-setup.md` §4-6, `infra/ecs/*.json`

### 11.0 콘솔 절차

#### 11.0.1 ECS Cluster 2개

1. **Elastic Container Service** → **Clusters** → **Create cluster**
2. `thirdtool-prod`:
   - Cluster name: `thirdtool-prod`
   - Infrastructure: **AWS Fargate (serverless)** 체크
   - Tags: `Project=thirdtool`, `Environment=prod`
   - Create
3. `thirdtool-staging`: 동일 + Environment=staging
4. **FARGATE_SPOT capacity provider 추가** (cost 절감):
   - 생성된 cluster 클릭 → **Infrastructure** 탭 → **Update cluster**
   - Capacity providers: `FARGATE`, `FARGATE_SPOT` 둘 다 체크
   - Default capacity provider strategy: FARGATE weight 100 (prod 기본 - on-demand) / staging은 SPOT weight 50, FARGATE weight 50 (절반 SPOT)
   - Update

#### 11.0.2 Task Definition 등록 (JSON 직접)

Task Definition은 placeholder 치환 정확성이 핵심이라 콘솔 폼보다 **JSON 입력 방식** 권장.

1. ECS → **Task definitions** → **Create new task definition with JSON**
2. JSON 에디터에:
   - `infra/ecs/task-definition-prod.json` 내용 그대로 붙여넣기
   - 4종 placeholder 치환:
     - `<AWS_ACCOUNT_ID>` (Stage 0)
     - `<IMAGE_TAG>` (ECR에 push된 태그 — 보통 `latest` 또는 git_sha 7자)
     - `<SUFFIX>` 시크릿별 (Stage 5 — 시크릿마다 다름)
3. **치환 검증**: 에디터 상단에서 `<` 문자 검색 → 0건
4. **Create**
5. 동일하게 `task-definition-staging.json`로 `thirdtool-staging` 생성

#### 11.0.3 ECS Service 생성

1. 생성한 Cluster `thirdtool-prod` 클릭 → **Services** 탭 → **Create**
2. Environment:
   - Compute options: **Capacity provider strategy** (FARGATE 기본)
   - Application type: **Service**
3. Deployment configuration:
   - Family: `thirdtool-prod` (Task Definition family)
   - Revision: 최신 (`thirdtool-prod:1` 등)
   - Service name: `thirdtool-app`
   - Desired tasks: **2** (service-prod.json 기준)
4. Networking:
   - VPC: `thirdtool-vpc`
   - Subnets: `thirdtool-app-2a`, `thirdtool-app-2c` (private subnet 2개 체크)
   - Security group: **Use an existing security group** → `thirdtool-app-sg`
   - Public IP: **Off**
5. Load balancing:
   - Load balancer type: **Application Load Balancer**
   - Choose load balancer: **Use an existing load balancer** → `thirdtool-prod-alb`
   - Listener: 80 또는 443 (Stage 7에서 만든 것)
   - Target group: **Use an existing target group** → `thirdtool-prod-tg`
   - Container to load balance: `thirdtool-server:8080`
6. Service auto scaling: M1은 비활성 (M2 검토)
7. **Create**

> **Task Def + Service의 JSON spec(infra/ecs/*.json)을 그대로 등록**하려면 콘솔 폼보다 CLI가 정확하다. 콘솔은 핵심 ID·SG·subnet 매핑이 시각적으로 명확한 장점.

### 11.1 CLI 절차 (대안 / 정확)

```bash
# 1. ECS Cluster 2개
aws ecs create-cluster --cluster-name thirdtool-prod \
  --capacity-providers FARGATE FARGATE_SPOT \
  --default-capacity-provider-strategy capacityProvider=FARGATE,weight=100 \
  --region ap-northeast-2

aws ecs create-cluster --cluster-name thirdtool-staging \
  --capacity-providers FARGATE FARGATE_SPOT --region ap-northeast-2

# 2. Task Definition 등록 (prod·staging 각각)
#    infra/ecs/task-definition-prod.json 복사 후 4종 placeholder 치환:
#    <AWS_ACCOUNT_ID> / <IMAGE_TAG> / <SUFFIX>(시크릿별 다름)
#    치환 검증: grep -c '<' /tmp/td-prod.json → 0
aws ecs register-task-definition --cli-input-json file:///tmp/td-prod.json

# 3. ECS Service 생성 (prod·staging 각각)
#    infra/ecs/service-prod.json 복사 후 placeholder 치환:
#    <APP_SUBNET_2A> / <APP_SUBNET_2C> / <APP_SG> / <PROD_TARGET_GROUP_ARN>
#    치환 검증: grep -c '<' /tmp/svc-prod.json → 0
aws ecs create-service --cli-input-json file:///tmp/svc-prod.json
```

### 11.2 출력 메모

```
PROD_CLUSTER=thirdtool-prod
STAGING_CLUSTER=thirdtool-staging

PROD_TASK_DEF_ARN=arn:aws:ecs:ap-northeast-2:<acct>:task-definition/thirdtool-prod:1
STAGING_TASK_DEF_ARN=arn:aws:ecs:ap-northeast-2:<acct>:task-definition/thirdtool-staging:1

PROD_SERVICE_ARN=arn:aws:ecs:ap-northeast-2:<acct>:service/thirdtool-prod/thirdtool-app
STAGING_SERVICE_ARN=arn:aws:ecs:ap-northeast-2:<acct>:service/thirdtool-staging/thirdtool-app
```

### 11.3 검증 (ts008 §7)

- Task RUNNING + HEALTHY 상태 (1-2분 대기) — ECS → Cluster → Tasks 탭
- CloudWatch Logs `/ecs/thirdtool-prod` 최신 stream에 Spring Boot 부팅 JSON 라인
- ALB Target Group healthy 상태 — EC2 → Target groups → `thirdtool-prod-tg` → Targets 탭, Status: `healthy`
- `curl https://<ALB_DNS>/health` → 200 OK
- ECS Exec 진입 가능 (`aws ecs execute-command --cluster ... --command "/bin/sh"`)

---

## 12. Placeholder 매핑 전체 표

각 placeholder가 어느 Stage 출력으로 채워지는지 단방향 매핑.

| 파일 | placeholder | 채우는 Stage |
| --- | --- | --- |
| `infra/iam/gha-deploy-role-trust-policy.json` | `<AWS_ACCOUNT_ID>` | Stage 0 (발행) → Stage 1 (사용) |
| `infra/iam/gha-deploy-role-permissions-policy.json` | `<AWS_ACCOUNT_ID>` | Stage 0 → Stage 1 / Stage 8 (갱신) |
| `infra/iam/ecs-task-execution-role-permissions-policy.json` | `<AWS_ACCOUNT_ID>` | Stage 0 → Stage 8 |
| `infra/iam/ecs-task-role-permissions-policy.json` | `<AWS_ACCOUNT_ID>` | Stage 0 → Stage 8 |
| `infra/ecs/task-definition-prod.json` | `<AWS_ACCOUNT_ID>` | Stage 0 → Stage 9 |
| `infra/ecs/task-definition-prod.json` | `<IMAGE_TAG>` | ECR push 시점 (보통 `latest` 또는 git_sha 7자) |
| `infra/ecs/task-definition-prod.json` | `<SUFFIX>` (시크릿별 다름) | Stage 5 |
| `infra/ecs/service-prod.json` | `<APP_SUBNET_2A>` | Stage 2 (`$APP_2A`) |
| `infra/ecs/service-prod.json` | `<APP_SUBNET_2C>` | Stage 2 (`$APP_2C`) |
| `infra/ecs/service-prod.json` | `<APP_SG>` | Stage 2 (`$APP_SG`) |
| `infra/ecs/service-prod.json` | `<PROD_TARGET_GROUP_ARN>` | Stage 7 |
| `infra/ecs/service-staging.json` | (동일 패턴 — staging suffix) | Stage 2/7 |
| GitHub Variables `AWS_ACCOUNT_ID` | (Variables 등록) | Stage 0 → Stage 1 |

---

## 13. 비용 모니터링

각 Stage 진입 즉시 발생하는 시간 단위 비용:

| 자원 | 시간 비용 | 일 비용 | M1 6일 누적 | 트리거 Stage |
| --- | --- | --- | --- | --- |
| IAM User + Budget | $0 | $0 | $0 | **Stage 0** |
| NAT Gateway | $0.06 | $1.44 | $8.64 | Stage 2 |
| RDS db.t4g.micro single-AZ | $0.019 | $0.45 | $2.70 | Stage 6 |
| ALB | $0.0225 + LCU | ~$0.80 | $4.80 | Stage 7 |
| ECS Fargate (prod 1vCPU·2GB·2대) | $0.102 | $2.46 | $14.76 | Stage 9 |
| ECS Fargate (staging 0.5vCPU·1GB·SPOT 50%) | $0.013 | $0.31 | $1.86 | Stage 9 |
| ECR storage | ~$0.01/일 | $0.01 | $0.06 | Stage 4 (기존) |
| Secrets Manager | $0.40/월/secret × 5개 (dev) | $0.067 | $0.40 | Stage 5 |
| CloudWatch Logs | 수집 + 보존 | ~$0.10 | $0.60 | Stage 3 |
| EIP (NAT용 1개, NAT 동작 중이면 무료) | $0 | $0 | $0 | Stage 2 |
| **합계 (full deploy 가정)** | | **~$5.62/일** | **~$33.7** | |

`workflow/task/milestones/version/0.0.1v/cost.md`와 정합. Stage 0의 **AWS Budget $50/월 알림**이 1차 안전망 — 80% (= $40) 도달 시 이메일.

---

## 14. 셋업 폐기 절차 (개발 종료 시)

비용 누적 방지. 역순으로 삭제:

1. ECS Service `desiredCount=0` 후 삭제 (Stage 9)
2. ECS Cluster 삭제 (Stage 9)
3. Task Definition deregister (혹은 보존 — 비용 없음)
4. ALB 삭제 (Stage 7) → Target Group 자동 삭제
5. RDS 인스턴스 삭제 (Stage 6) → snapshot 보존 여부 결정
6. Secrets Manager 시크릿 삭제 (Stage 5) — recovery window 7-30일
7. NAT Gateway 삭제 (Stage 2) → EIP release
8. CloudWatch Log groups 삭제 또는 retention 1일로 단축 (Stage 3)
9. VPC 삭제 (Stage 2) — subnet/IGW/RT/SG 모두 자동 삭제. EIP은 별도 release 확인
10. ECR repository 삭제는 선택 (저장 비용 미미)
11. IAM Role / OIDC Provider 보존 (재사용 가능, 비용 없음)
12. Admin User · root 계정 자체는 보존 (재사용 가능). access key는 사용 종료 시 **Make inactive** → 검증 후 **Delete**

EIP 잔존 확인:
```bash
aws ec2 describe-addresses --region ap-northeast-2 | jq '.Addresses[] | select(.AssociationId == null) | .AllocationId'
# 출력 있으면 release:
aws ec2 release-address --allocation-id eipalloc-xxxxx --region ap-northeast-2
```

콘솔: VPC → Elastic IPs → 미연결 EIP 선택 → Actions → **Release Elastic IP addresses**.

---

## 14A. [Stage 10] Route53 도메인 등록 — **사용자 직접 (D1)**

**참조**: ADR020 · `infra/DEPLOY-RUNBOOK.md` Phase 0

### 14A.1 콘솔 절차 (~$12/년)

1. **Route53** → 좌측 **Registered domains** → **Register domain**
2. `thirdtool.dev` 검색 → 사용 가능 확인 → **Continue to checkout**
3. Contact information 입력 (whois privacy 자동 활성)
4. Auto-renew **활성** 권장 (M2-M3 운영 안전망)
5. 결제 (Credit card) → 등록 (~5-10분, 이메일 알림)

### 14A.2 등록 완료 후 자동 발생

- Hosted Zone 자동 생성 (`Z*` ID)
- 4건 NS 레코드 자동 등록 (Route53 자체 NS)
- 본 도메인은 Route53 NS만 사용 (외부 NS delegation 불요)

### 14A.3 출력 메모

```bash
DOMAIN=thirdtool.dev
HZ_ID=$(aws route53 list-hosted-zones-by-name --dns-name $DOMAIN \
  --query 'HostedZones[0].Id' --output text | cut -d/ -f3)
echo "HZ_ID=$HZ_ID"
```

```
HZ_ID=______________
DOMAIN_REGISTERED_AT=______________   # YYYY-MM-DD HH:MM
DOMAIN_AUTO_RENEW=______________      # true/false
```

---

## 14B. [Stage 11] ACM 인증서 2-region 신청 — **D2**

**참조**: ADR020 §결정 ALB 부 · `infra/acm/` · `infra/DEPLOY-RUNBOOK.md` Phase 1
**사전 조건**: Stage 10 (Route53 Hosted Zone 등록 완료) — DNS 검증 CNAME을 Route53에 자동 추가

### 14B.1 CLI 절차 — 2-region 병렬

```bash
# (1) ALB용 — ap-northeast-2
ACM_AP_ARN=$(aws acm request-certificate \
  --cli-input-json file://infra/acm/acm-ap-northeast-2.json \
  --region ap-northeast-2 \
  --query CertificateArn --output text)
echo "ACM_AP_ARN=$ACM_AP_ARN"

# (2) CloudFront용 — us-east-1 (CloudFront 자체 요구사항, ADR020)
ACM_US_ARN=$(aws acm request-certificate \
  --cli-input-json file://infra/acm/acm-us-east-1.json \
  --region us-east-1 \
  --query CertificateArn --output text)
echo "ACM_US_ARN=$ACM_US_ARN"
```

### 14B.2 DNS 검증 CNAME Route53 자동 추가

**콘솔 (권장)**: ACM → 각 cert 상세 → "Create records in Route 53" 버튼 1회 클릭 (2-region 각각 1회).

**CLI 대안**: AWS docs `acm describe-certificate` 출력의 `ResourceRecord`를 Route53에 `aws route53 change-resource-record-sets` 명시 등록 — 콘솔 1-click이 훨씬 빠름.

### 14B.3 ISSUED 대기 (~5-10분)

```bash
aws acm wait certificate-validated --certificate-arn $ACM_AP_ARN --region ap-northeast-2
aws acm wait certificate-validated --certificate-arn $ACM_US_ARN --region us-east-1
echo "✅ 인증서 2건 발급 완료"
```

### 14B.4 출력 메모

```
ACM_AP_ARN=arn:aws:acm:ap-northeast-2:______________:certificate/______________
ACM_US_ARN=arn:aws:acm:us-east-1:______________:certificate/______________
ACM_ISSUED_AT_AP=______________
ACM_ISSUED_AT_US=______________
```

---

## 14C. [Stage 12] ALB HTTPS 리스너 + 443 SG 인바운드 — **D3**

**참조**: ADR020 §결정 ALB · `infra/alb/alb-https-listener.json` · `infra/alb/alb-http-to-https-redirect.json` · `infra/vpc/security-group-alb-443.json` · `infra/DEPLOY-RUNBOOK.md` Phase 2
**사전 조건**: Stage 11 ap-northeast-2 ACM ISSUED + Stage 7 (ALB 존재)

### 14C.1 CLI 절차

```bash
ALB_ARN=$(aws elbv2 describe-load-balancers --names third-tool-alb \
  --query 'LoadBalancers[0].LoadBalancerArn' --output text)
TG_ARN=$(aws elbv2 describe-target-groups --names third-tool-tg \
  --query 'TargetGroups[0].TargetGroupArn' --output text)
HTTP_LISTENER_ARN=$(aws elbv2 describe-listeners --load-balancer-arn $ALB_ARN \
  --query 'Listeners[?Port==`80`].ListenerArn' --output text)
ALB_SG=$(aws elbv2 describe-load-balancers --names third-tool-alb \
  --query 'LoadBalancers[0].SecurityGroups[0]' --output text)

# (1) ALB SG 443 인바운드 허용 (security-group-alb-443.json 활용)
aws ec2 authorize-security-group-ingress --group-id $ALB_SG \
  --protocol tcp --port 443 --cidr 0.0.0.0/0 \
  --region ap-northeast-2 || echo "(이미 존재 시 skip)"

# (2) HTTPS 443 listener 신설 (TLS 1.3, ACM ap-northeast-2 cert)
sed -e "s|<ALB_ARN>|$ALB_ARN|g" -e "s|<TG_ARN>|$TG_ARN|g" -e "s|<ACM_AP_ARN>|$ACM_AP_ARN|g" \
  infra/alb/alb-https-listener.json > /tmp/alb-443.json
aws elbv2 create-listener --cli-input-json file:///tmp/alb-443.json

# (3) HTTP 80 → 443 301 redirect 전환
sed -e "s|<HTTP_LISTENER_ARN>|$HTTP_LISTENER_ARN|g" \
  infra/alb/alb-http-to-https-redirect.json > /tmp/alb-80-redirect.json
aws elbv2 modify-listener --cli-input-json file:///tmp/alb-80-redirect.json
```

### 14C.2 검증

```bash
ALB_DNS=$(aws elbv2 describe-load-balancers --names third-tool-alb \
  --query 'LoadBalancers[0].DNSName' --output text)

curl -I http://$ALB_DNS   # 301 + Location: https://$ALB_DNS
curl -i https://$ALB_DNS/health 2>&1 | head -2   # 200 OK (Target healthy 후)
```

### 14C.3 출력 메모

```
ALB_443_LISTENER_ARN=______________
ALB_DNS=______________.ap-northeast-2.elb.amazonaws.com
ALB_443_ACTIVATED_AT=______________
```

---

## 14D. [Stage 13] S3 FE 버킷 + CloudFront 배포 + OAC — **D5+D6**

**참조**: ADR020 §결정 FE CDN · `infra/s3/` · `infra/cloudfront/` · `infra/DEPLOY-RUNBOOK.md` Phase 4·5
**사전 조건**: Stage 11 us-east-1 ACM ISSUED

### 14D.1 S3 버킷 생성 — D5

```bash
FE_BUCKET=thirdtool-fe-prod
aws s3api create-bucket --bucket $FE_BUCKET \
  --region ap-northeast-2 \
  --create-bucket-configuration LocationConstraint=ap-northeast-2

# 퍼블릭 접근 완전 차단 (OAC만 GetObject)
aws s3api put-public-access-block --bucket $FE_BUCKET \
  --public-access-block-configuration file://infra/s3/s3-fe-bucket-public-access-block.json
```

### 14D.2 CloudFront OAC + Distribution 생성 — D6

```bash
# (1) OAC 생성
OAC_ID=$(aws cloudfront create-origin-access-control \
  --origin-access-control-config file://infra/cloudfront/cloudfront-oac.json \
  --query 'OriginAccessControl.Id' --output text)
echo "OAC_ID=$OAC_ID"

# (2) Distribution 생성 (us-east-1 ACM cert + OAC + SPA fallback)
sed -e "s|<OAC_ID>|$OAC_ID|g" -e "s|<ACM_US_EAST_1_ARN>|$ACM_US_ARN|g" \
  infra/cloudfront/cloudfront-dist.json > /tmp/cf-dist.json
CF_DIST_OUT=$(aws cloudfront create-distribution --distribution-config file:///tmp/cf-dist.json)
CF_DIST_ID=$(echo "$CF_DIST_OUT" | jq -r '.Distribution.Id')
CF_DOMAIN=$(echo "$CF_DIST_OUT" | jq -r '.Distribution.DomainName')
echo "CF_DIST_ID=$CF_DIST_ID"
echo "CF_DOMAIN=$CF_DOMAIN"

# (3) S3 bucket policy 갱신 — OAC principal만 GetObject 허용
sed -e "s|<AWS_ACCOUNT_ID>|$AWS_ACCOUNT_ID|g" -e "s|<CF_DIST_ID>|$CF_DIST_ID|g" \
  infra/s3/s3-fe-bucket-policy.json > /tmp/s3-policy.json
aws s3api put-bucket-policy --bucket $FE_BUCKET --policy file:///tmp/s3-policy.json
```

### 14D.3 Distribution Deployed 대기 (~5-10분)

```bash
aws cloudfront wait distribution-deployed --id $CF_DIST_ID
echo "✅ CloudFront distribution deployed"
```

### 14D.4 GitHub Variables 갱신 (deploy-fe.yml 사용)

```bash
gh variable set CF_DIST_ID --body "$CF_DIST_ID"
gh variable set FE_S3_BUCKET --body "$FE_BUCKET"
```

### 14D.5 출력 메모

```
FE_S3_BUCKET=thirdtool-fe-prod
OAC_ID=______________
CF_DIST_ID=______________
CF_DOMAIN=______________.cloudfront.net
CF_DEPLOYED_AT=______________
```

---

## 14E. [Stage 14] Route53 alias 레코드 3건 + OAuth provider 갱신 — **D4+D7+사용자**

**참조**: ADR020 §결정 Route53 + OAuth · `infra/route53/`
**사전 조건**: Stage 12 (ALB HTTPS 활성) + Stage 13 (CloudFront deployed)

### 14E.1 api.thirdtool.dev → ALB (D4)

```bash
ALB_HZ_ID=$(aws elbv2 describe-load-balancers --names third-tool-alb \
  --query 'LoadBalancers[0].CanonicalHostedZoneId' --output text)
ALB_DNS=$(aws elbv2 describe-load-balancers --names third-tool-alb \
  --query 'LoadBalancers[0].DNSName' --output text)

sed -e "s|<ALB_DNS>|$ALB_DNS|g" -e "s|<ALB_HOSTED_ZONE_ID>|$ALB_HZ_ID|g" \
  infra/route53/route53-api-record.json > /tmp/api-record.json
aws route53 change-resource-record-sets --hosted-zone-id $HZ_ID \
  --change-batch file:///tmp/api-record.json
```

### 14E.2 thirdtool.dev + www → CloudFront (D7)

CloudFront alias HostedZoneId는 **항상 `Z2FDTNDATAQYW2`** (AWS 고정).

```bash
sed -e "s|<CF_DOMAIN>|$CF_DOMAIN|g" \
  infra/route53/route53-fe-record.json > /tmp/fe-record.json
aws route53 change-resource-record-sets --hosted-zone-id $HZ_ID \
  --change-batch file:///tmp/fe-record.json
```

### 14E.3 DNS 전파 대기 (~1-5분 + TTL 60s 도달)

```bash
# api
dig +short api.thirdtool.dev
# apex
dig +short thirdtool.dev
# www
dig +short www.thirdtool.dev
```

각 명령이 IPv4 1건 이상 출력 → 전파 완료.

### 14E.4 카카오·네이버 developers 콘솔 갱신 (사용자 직접)

OAuth redirect URI를 `https://api.thirdtool.dev/oauth/{kakao,naver}/callback`로 갱신:
- 카카오 developers: 내 애플리케이션 → 제품 설정 → 카카오 로그인 → Redirect URI
- 네이버 developers: 내 애플리케이션 → API 설정 → 서비스 URL · Callback URL

ts006 §2.1·2.2 참조. 갱신 누락 시 OAuth callback 실패 (HTTP 400 invalid redirect_uri).

### 14E.5 출력 메모

```
ROUTE53_API_RECORD=api.thirdtool.dev → ALB ($ALB_DNS)
ROUTE53_APEX_RECORD=thirdtool.dev → CloudFront ($CF_DOMAIN)
ROUTE53_WWW_RECORD=www.thirdtool.dev → CloudFront ($CF_DOMAIN)
DNS_PROPAGATED_AT=______________
KAKAO_REDIRECT_UPDATED_AT=______________
NAVER_REDIRECT_UPDATED_AT=______________
```

### 14E.6 종료 신호 4건 (M1 1차 배포 완성)

- [ ] `curl -i https://api.thirdtool.dev/actuator/health` → 200 OK
- [ ] `curl -I http://api.thirdtool.dev` → 301 → https
- [ ] `curl https://thirdtool.dev` → HTML 응답 (FE index.html)
- [ ] 브라우저 E2E — FE → API 호출 (CORS OK, SameSite=Strict 쿠키 전달)

---

## 15. 후속 Story (코드베이스 측 미작성)

본 가이드 v2 작성 시점(2026-06-29)에 다음 Story가 미작성 — Stage 5/6 진입 전 코드/runbook 보강 필요:

| Story | 항목 | 가이드 작성 |
| --- | --- | --- |
| ~~#13~~ | ~~ALB + Target Group spec JSON + ts012 runbook + ADR016~~ | ✅ **Story-049 완료** (2026-06-29) — Stage 7 활성 |
| ~~#14~~ | ~~RDS MySQL spec + ts011 runbook + ADR017~~ | ✅ **Story-051 완료** (2026-06-29) — Stage 6 활성 |
| ~~#15~~ | ~~Secrets Manager 5종 시크릿 등록 + ts012 runbook + ADR019~~ | ✅ **Story-052 완료** (2026-06-30) — Stage 5 활성 |
| ~~Tier 2 #19~~ | ~~Spring Cloud AWS Secrets Manager bootstrap 통합 (`spring-cloud-aws-starter-secrets-manager`)~~ | ✅ **Story-053 완료** (2026-06-30) — Stage 5 등록 후 자동 fetch |
| ~~Tier 2 #17~~ | ~~git_sha 기반 이미지 태깅 + GHA workflow 갱신~~ | ✅ **Story-055 완료** (2026-06-30) — Stage 9 본격 운영 후 효과 발현 |
| Tier 2 #18 | ALB Target Group health check 통합 | Stage 7 + 9 통합 검증 |
| ~~Tier 1-확장 (D1~D10)~~ | ~~1차 배포 완성 — Route53 + ACM + ALB HTTPS + S3+CloudFront + Route53 alias + CORS·OAuth 갱신~~ | ✅ **Story-056 완료** (2026-06-30) — Stage 10~14 활성. 사용자 액션 절차 §14A~§14E |
| 별도 | S3Config `DefaultCredentialsProvider` 전환 | Stage 9 운영 검증 통과 후 |
| 별도 | EC2 SSH 배포 step 폐기 (`dev-cicd.yml`) | Stage 9 운영 검증 통과 후 |

---

## 16. 관련 자산

- **코드 spec JSON**: `infra/iam/*.json` · `infra/ecs/*.json` · `infra/vpc/*.json` · `infra/alb/*.json`
- **runbook**: `docs/operations/troubleshooting/ts007-gha-oidc-setup.md` · `ts008-ecs-cluster-setup.md` · `ts009-vpc-setup.md` · `ts010-alb-setup.md`
- **ADR**: `docs/adr/ADR013-gha-oidc-assume-role.md` · `ADR014-ecs-task-iam-role-separation.md` · `ADR015-vpc-3-layer-network-design.md` · `ADR016-alb-target-group-ip-mode.md` (Story-049)
- **아키텍처 다이어그램**: `docs/architecture/vpc-topology.md`
- **milestone**: `workflow/task/milestones/version/0.0.1v/milestone.md` (item #10-15)
- **비용 추적**: `workflow/task/milestones/version/0.0.1v/cost.md`
- **product 명세**: `workflow/task/pes/workspectrum/sdd/in-progress/product-infra-deploy.md` · `product-infra-network.md` · `product-infra-ops.md`

---

## 17. 변경 로그

| 날짜 | Story | 변경 |
| --- | --- | --- |
| 2026-06-29 | 본 문서 초안 (v1) | M1 종료 시점 — Stage 1·2·3·4·8·9 산출물 완료, Stage 5·6·7 보류 명시 |
| 2026-06-29 | v2 리팩토링 | (1) **Stage 0 신규** — root → MFA → Budget → Admin IAM User → CLI configure. (2) 각 Stage에 **콘솔 절차 (X.0)** 서브섹션 추가, CLI는 대안/자동화로 병기. (3) Stage 7 (ALB) 상태를 "보류" → "활성" (Story-049 완료, ts010 추가). (4) Placeholder 매핑·비용·폐기 표에 Stage 0 행 추가. |
| 2026-06-30 | Story-052 | Stage 5 (Secrets Manager) 본문 활성 — 5종 비밀(db-credential · jwt-secret · oauth-kakao · oauth-naver · gemini-api-key) + ts012 + ADR019 + Task Role IAM patch 매핑. §15 후속 Story 표에서 #15·#14 완료 표기 + #19(Spring Cloud AWS bootstrap) 추가. |
| 2026-06-30 | Story-056 | **Stage 10~14 신설** — Route53 도메인 등록(§14A) · ACM 2-region(§14B) · ALB HTTPS+443 SG(§14C) · S3+CloudFront+OAC(§14D) · Route53 alias 3건+OAuth provider 갱신(§14E). 종료 신호 4건(api/redirect/FE/E2E) §14E.6. ADR020 + DEPLOY-RUNBOOK.md 참조. |
| 2026-06-30 | Story-053·054·055 | Tier 2 동시 완료 — Spring Cloud AWS bootstrap · Grafana dashboard · git_sha 이미지 태깅. §15 후속 Story 표 갱신. |
