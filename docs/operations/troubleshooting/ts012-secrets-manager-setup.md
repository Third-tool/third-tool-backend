# ts012: AWS Secrets Manager 5종 비밀 등록·검증·트러블슈팅

Story-052로 `infra/secrets/`에 5종 비밀(`db-credential`, `jwt-secret`, `oauth-kakao`, `oauth-naver`, `gemini-api-key`)의 페이로드 구조 + IAM Task Role 권한 patch + ADR019가 추가됐다. 본 가이드는 AWS 콘솔(또는 CLI)에서 1회성 등록 + 검증 + 자주 보는 에러 6건을 한 곳에 모았다.

ts007(GHA OIDC)·ts008(ECS)·ts011(RDS)·ts014(ALB-HTTPS, 후속)와 cross-link. 1인 운영자가 env마다 1회만 실행.

---

## 1. 전제

- **VPC 셋업 완료** (ts009)
- **ECS Task Role 생성 완료** (ts008 §3 — `arn:aws:iam::<AWS_ACCOUNT_ID>:role/ecs-task-role`)
- **RDS 인스턴스 완료** (ts011 §5 — `db-credential`의 HOST/PORT/NAME 출처)
- **카카오/네이버 developers 등록 완료** (ts006 §2.1 / §2.2 — OAuth client_id/secret 출처)
- **Google AI Studio 가입 (선택)** — `gemini-api-key`는 M1 placeholder OK, AI Epic 3 진입 전엔 미사용
- AWS 콘솔 Secrets Manager 관리 권한 (`secretsmanager:*`, `kms:Decrypt` for `aws/secretsmanager`)
- **결제**: 시크릿당 월 $0.40 + API call $0.05/10K. 5 secrets × 1 env(dev) × $0.40 = **$2/월 baseline** (prod 전환 시 $4)

> **GitHub Secrets 폐기 안전망**: 본 가이드 완주 + Task Def 부팅 검증 통과(§6) 후에만 GitHub Actions Secrets에서 `DB_PASSWORD`/`JWT_SECRET_KEY`/`KAKAO_CLIENT_SECRET`/`NAVER_CLIENT_SECRET` 4건 삭제. 미리 지우면 dev-cicd.yml 롤백 시점에 부팅 실패.

---

## 2. 사전 값 준비 (운영자 보관소)

§3 실행 전 다음 7건의 값을 안전한 곳(예: `~/thirdtool-aws-ids.txt` gitignore 또는 1Password)에 모은다:

```
# RDS 출력 (ts011 §5)
RDS_ENDPOINT=______________.ap-northeast-2.rds.amazonaws.com
DB_NAME=thirdtool
DB_USERNAME=admin
DB_PASSWORD=______________               # ts011 §4에서 결정한 master password

# JWT 신규 생성
JWT_SECRET_KEY=$(openssl rand -base64 48)   # HS512 충분, 384 bit ≥ 256 bit 최소

# 카카오 developers
KAKAO_CLIENT_ID=______________            # REST API 키
KAKAO_CLIENT_SECRET=______________

# 네이버 developers
NAVER_CLIENT_ID=______________
NAVER_CLIENT_SECRET=______________

# Google AI Studio (선택 — M1 placeholder OK)
GEMINI_API_KEY=PLACEHOLDER_FOR_M2          # 실제 발급 전엔 이 값 그대로 등록
```

---

## 3. 시크릿 5종 등록 — CLI

> 콘솔 절차는 §4. CLI는 자동화·재현성 우선. 본 절차는 `dev` env 기준. `prod`/`staging`은 env 부분만 치환하여 동일 반복.

### 3.1 db-credential

```bash
ENV=dev
aws secretsmanager create-secret \
  --name "thirdtool/${ENV}/db-credential" \
  --description "ThirdTool ${ENV} — RDS MySQL connection (Story-052)" \
  --secret-string "$(jq -n \
    --arg host "$RDS_ENDPOINT" \
    --arg port "3306" \
    --arg name "$DB_NAME" \
    --arg user "$DB_USERNAME" \
    --arg pass "$DB_PASSWORD" \
    '{HOST: $host, PORT: $port, NAME: $name, USERNAME: $user, PASSWORD: $pass}')" \
  --tags Key=Project,Value=ThirdTool Key=ManagedBy,Value=Story-052 Key=Environment,Value="$ENV" \
  --region ap-northeast-2

# 응답의 ARN 끝 random 6 char suffix 메모 → DB_SUFFIX
```

### 3.2 jwt-secret

```bash
aws secretsmanager create-secret \
  --name "thirdtool/${ENV}/jwt-secret" \
  --description "ThirdTool ${ENV} — JWT HS512 signing key (Story-052)" \
  --secret-string "$(jq -n --arg k "$JWT_SECRET_KEY" '{SECRET_KEY: $k}')" \
  --tags Key=Project,Value=ThirdTool Key=ManagedBy,Value=Story-052 Key=Environment,Value="$ENV" \
  --region ap-northeast-2

# suffix 메모 → JWT_SUFFIX
```

### 3.3 oauth-kakao

```bash
aws secretsmanager create-secret \
  --name "thirdtool/${ENV}/oauth-kakao" \
  --description "ThirdTool ${ENV} — Kakao OAuth client (Story-052)" \
  --secret-string "$(jq -n \
    --arg id "$KAKAO_CLIENT_ID" \
    --arg sec "$KAKAO_CLIENT_SECRET" \
    '{CLIENT_ID: $id, CLIENT_SECRET: $sec}')" \
  --tags Key=Project,Value=ThirdTool Key=ManagedBy,Value=Story-052 Key=Environment,Value="$ENV" \
  --region ap-northeast-2

# suffix 메모 → KAKAO_SUFFIX
```

### 3.4 oauth-naver

```bash
aws secretsmanager create-secret \
  --name "thirdtool/${ENV}/oauth-naver" \
  --description "ThirdTool ${ENV} — Naver OAuth client (Story-052)" \
  --secret-string "$(jq -n \
    --arg id "$NAVER_CLIENT_ID" \
    --arg sec "$NAVER_CLIENT_SECRET" \
    '{CLIENT_ID: $id, CLIENT_SECRET: $sec}')" \
  --tags Key=Project,Value=ThirdTool Key=ManagedBy,Value=Story-052 Key=Environment,Value="$ENV" \
  --region ap-northeast-2

# suffix 메모 → NAVER_SUFFIX
```

### 3.5 gemini-api-key

```bash
aws secretsmanager create-secret \
  --name "thirdtool/${ENV}/gemini-api-key" \
  --description "ThirdTool ${ENV} — Gemini API key (Story-052, AI Epic 3 진입 전 placeholder)" \
  --secret-string "$(jq -n --arg k "$GEMINI_API_KEY" '{API_KEY: $k}')" \
  --tags Key=Project,Value=ThirdTool Key=ManagedBy,Value=Story-052 Key=Environment,Value="$ENV" \
  --region ap-northeast-2

# suffix 메모 → GEMINI_SUFFIX
```

---

## 4. 시크릿 5종 등록 — 콘솔 (대안)

CLI 미사용 시 각 시크릿마다 1회씩:

1. **Secrets Manager** → **Store a new secret**
2. Secret type: **Other type of secret**
3. Key/value pairs 탭에서 JSON 키별로 입력 (§3.1~3.5 페이로드 구조 참조)
4. Encryption key: `aws/secretsmanager` (AWS managed, 비용 0) → Next
5. Secret name: `thirdtool/dev/db-credential` 등 (정확한 이름 — Spring Cloud AWS bootstrap이 이 이름으로 fetch)
6. Description: 자유 / Tags: Project=ThirdTool, Environment=dev, ManagedBy=Story-052 → Next
7. Configure rotation: **Disable automatic rotation** (M1은 수동, ADR019 §1.3) → Next
8. Review → **Store**
9. 생성된 시크릿 상세 페이지에서 ARN 끝 random suffix(`-AbCd9X`) 메모

---

## 5. 출력 메모 보관

본 가이드 끝에서 다음 5건의 ARN suffix를 보관 (Task Def `<SUFFIX>` 치환에 사용):

```
SECRETS_SUFFIX_DEV_DB_CREDENTIAL=______________
SECRETS_SUFFIX_DEV_JWT_SECRET=______________
SECRETS_SUFFIX_DEV_OAUTH_KAKAO=______________
SECRETS_SUFFIX_DEV_OAUTH_NAVER=______________
SECRETS_SUFFIX_DEV_GEMINI_API_KEY=______________
```

이 값을 `infra/ecs/task-definition-prod.json`의 secrets ARN 끝 `<SUFFIX>` 자리에 치환한다 (실 등록 후 ts008 §6 Task Def register 단계).

---

## 6. 검증

### 6.1 시크릿 5건 존재 확인

```bash
aws secretsmanager list-secrets \
  --filters Key=name,Values=thirdtool/dev/ \
  --region ap-northeast-2 \
  | jq -r '.SecretList[].Name'
```

다음 5건이 나와야 한다:
```
thirdtool/dev/db-credential
thirdtool/dev/jwt-secret
thirdtool/dev/oauth-kakao
thirdtool/dev/oauth-naver
thirdtool/dev/gemini-api-key
```

### 6.2 시크릿 값 1건 fetch 검증

```bash
aws secretsmanager get-secret-value \
  --secret-id thirdtool/dev/jwt-secret \
  --region ap-northeast-2 \
  | jq -r '.SecretString | fromjson | .SECRET_KEY' | wc -c
# 65 (base64 48 byte → ~65 char)
```

### 6.3 Task Role에서 GetSecretValue 권한 검증

ECS Task가 부팅 시점 자신의 Role로 fetch 가능한지 시뮬레이션 — 로컬에서 Task Role을 임시 AssumeRole 후 호출:

```bash
ROLE_ARN=arn:aws:iam::<AWS_ACCOUNT_ID>:role/ecs-task-role
CREDS=$(aws sts assume-role --role-arn "$ROLE_ARN" --role-session-name verify-secrets-fetch)

AWS_ACCESS_KEY_ID=$(echo "$CREDS" | jq -r '.Credentials.AccessKeyId') \
AWS_SECRET_ACCESS_KEY=$(echo "$CREDS" | jq -r '.Credentials.SecretAccessKey') \
AWS_SESSION_TOKEN=$(echo "$CREDS" | jq -r '.Credentials.SessionToken') \
aws secretsmanager get-secret-value \
  --secret-id thirdtool/dev/db-credential \
  --region ap-northeast-2 \
  | jq -r '.SecretString | fromjson | .USERNAME'
# admin (또는 ts011 §4에서 입력한 값)
```

> 401/AccessDenied 발생 시 ts012-3 참조.

### 6.4 ECS Task 부팅 시 env 주입 확인 (PR-A 머지 + 첫 배포 후)

ECS Task가 실제 부팅된 뒤 Task의 환경변수에 시크릿 값이 주입되었는지:

```bash
TASK_ID=$(aws ecs list-tasks --cluster thirdtool-cluster-dev --service-name thirdtool-service-dev --query 'taskArns[0]' --output text --region ap-northeast-2)

aws ecs execute-command --cluster thirdtool-cluster-dev \
  --task "$TASK_ID" --container app --interactive --command "sh -c 'env | grep -E \"DB_HOST|JWT_SECRET\" | head -2'" \
  --region ap-northeast-2
```

`DB_HOST=...rds.amazonaws.com` / `JWT_SECRET_KEY=...` 라인이 출력되어야 한다 (실 값은 마스킹 X — execute-command session은 운영자만 접근).

---

## 7. 자주 보는 에러 6건

### ts012-1 — `InvalidRequestException: A secret with this name is already scheduled for deletion`

이전 등록 + 삭제 시도 시 발생. AWS는 시크릿을 7-30일 recovery window로 보관.

**해결**:
```bash
# 즉시 영구 삭제
aws secretsmanager delete-secret --secret-id thirdtool/dev/db-credential \
  --force-delete-without-recovery --region ap-northeast-2
# 또는 recovery 취소
aws secretsmanager restore-secret --secret-id thirdtool/dev/db-credential --region ap-northeast-2
```

### ts012-2 — `ResourceNotFoundException: Secrets Manager can't find the specified secret`

Task Def에서 시크릿 fetch 실패 — 시크릿 이름이 정확히 일치하지 않음.

**원인**:
- 시크릿 이름에 추가 prefix/suffix가 붙음 (예: `thirdtool/Dev/...` 대소문자 차이)
- env 분리 누락 — `thirdtool/db-credential`로 등록되어 prefix 없음
- ARN suffix가 Task Def `<SUFFIX>` placeholder에 미치환 (등록 후 보관소에 적은 suffix를 잊고 그대로 둠)

**해결**: §6.1으로 정확한 이름 확인 → Task Def revision 갱신.

### ts012-3 — `AccessDeniedException: User: arn:aws:sts::...:assumed-role/ecs-task-role/... is not authorized to perform: secretsmanager:GetSecretValue`

IAM Task Role의 Resource scope가 시크릿 ARN과 매칭 안 됨.

**원인**:
- `infra/iam/ecs-task-role-permissions-policy.json` 패치 누락 (Story-052 안 머지)
- Resource ARN에 region 불일치 (`secretsmanager:us-east-1` vs `secretsmanager:ap-northeast-2`)
- ARN 패턴 `thirdtool/dev/db-credential-*` 와일드카드에 누락된 시크릿 type 존재

**해결**:
```bash
# 1) 현재 정책 확인
aws iam get-role-policy --role-name ecs-task-role --policy-name ecs-task-role-permissions
# 2) ADR019 정책 재적용
aws iam put-role-policy --role-name ecs-task-role --policy-name ecs-task-role-permissions \
  --policy-document file://infra/iam/ecs-task-role-permissions-policy.json
```

### ts012-4 — KMS `Decrypt` 권한 부족 — `KMSAccessDeniedException`

`aws/secretsmanager` (AWS managed key)는 기본 Decrypt 권한 부여되지만, CMK 전환 후엔 `kms:Decrypt` 별도 부여 필요.

**해결**: ADR019의 `KMSDecryptForSecretsManager` Statement가 활성화되어 있는지 확인. CMK 전환 시 Resource를 specific key ARN으로 좁힘.

### ts012-5 — JSON multi-key fetch에서 키 누락 — Task Def `valueFrom`이 `arn:...:thirdtool/dev/db-credential-SUFFIX:HOST::`인데 응답 빈 문자열

시크릿 페이로드가 JSON이 아니거나 키 이름이 다름 (예: `host` 소문자로 등록).

**해결**:
```bash
aws secretsmanager get-secret-value --secret-id thirdtool/dev/db-credential --region ap-northeast-2 \
  | jq -r '.SecretString | fromjson | keys'
# ["HOST","NAME","PASSWORD","PORT","USERNAME"] 정확한 키만 출력되어야 함
```

키가 잘못되었으면 `update-secret`로 페이로드 교체:
```bash
aws secretsmanager update-secret --secret-id thirdtool/dev/db-credential \
  --secret-string "$(jq -n --arg host "$RDS_ENDPOINT" ... '{HOST: $host, ...}')" \
  --region ap-northeast-2
```

### ts012-6 — `OperationNotPermittedException: Cannot update secret with a value containing more than 65536 characters`

페이로드 65 KB 초과. 페이로드 분할 또는 S3 객체로 외부화.

**해결**: 본 5종 비밀 페이로드는 모두 < 1 KB이므로 발생 시 페이로드 입력 실수. `jq` 출력 길이 확인 후 재입력.

---

## 8. GitHub Secrets 폐기

§3·§6 완료 + ECS Task 부팅 검증(ts008 §7 — Task RUNNING + Service Healthy) 통과 후, 다음 4건을 GitHub Actions Secrets에서 수동 삭제:

GitHub repo → Settings → Secrets and variables → Actions → Repository secrets:

- `DB_PASSWORD`
- `JWT_SECRET_KEY`
- `KAKAO_CLIENT_SECRET`
- `NAVER_CLIENT_SECRET`

> `DB_HOST`/`KAKAO_CLIENT_ID`/`NAVER_CLIENT_ID`는 비밀 아니므로 GitHub Variables로 이전하거나 Secrets Manager로 통합 가능. M1은 통합 (ADR019 §1.2 — JSON multi-key).

폐기 후 `gh secret list` 출력에서 위 4건이 안 보이는지 확인.

---

## 9. 회전 절차 (M1 수동)

특정 비밀 회전이 필요할 때 (예: JWT key 만료, OAuth client_secret 노출):

```bash
# 1) 새 값 생성 (예: JWT)
NEW_JWT=$(openssl rand -base64 48)

# 2) 시크릿 값 갱신 (versionId 자동 발급)
aws secretsmanager put-secret-value \
  --secret-id thirdtool/dev/jwt-secret \
  --secret-string "$(jq -n --arg k "$NEW_JWT" '{SECRET_KEY: $k}')" \
  --region ap-northeast-2

# 3) ECS Service force-new-deployment — Task가 부팅 시 새 버전 fetch
aws ecs update-service \
  --cluster thirdtool-cluster-dev \
  --service thirdtool-service-dev \
  --force-new-deployment \
  --region ap-northeast-2
```

> Spring Cloud AWS (Story-053)는 부팅 시점 1회만 fetch + 캐시. **회전은 Task 재배포 단위** — 캐시 무효화 자동화는 M2 Rotation Lambda에서.

---

## 10. 출력 요약 (보관소 기재 항목)

본 가이드 완주 후 운영자 보관소에 다음 9건이 누적되어야 한다:

```
# Stage 5 (Secrets Manager) — Story-052 산출물
SECRETS_REGION=ap-northeast-2
SECRETS_SUFFIX_DEV_DB_CREDENTIAL=______________      # ts012 §3.1
SECRETS_SUFFIX_DEV_JWT_SECRET=______________         # ts012 §3.2
SECRETS_SUFFIX_DEV_OAUTH_KAKAO=______________        # ts012 §3.3
SECRETS_SUFFIX_DEV_OAUTH_NAVER=______________        # ts012 §3.4
SECRETS_SUFFIX_DEV_GEMINI_API_KEY=______________     # ts012 §3.5
GITHUB_SECRETS_REVOKED_AT=______________             # §8 폐기 시점 (YYYY-MM-DD HH:MM)
TASK_DEF_REVISION_AFTER_SUFFIX_FILL=______________   # 5 SUFFIX 치환 후 Task Def revision (ts008 §6)
SECRETS_MONTHLY_BUDGET_INVOICE=______________        # 첫 월 청구 금액 — $2 baseline 검증
```
