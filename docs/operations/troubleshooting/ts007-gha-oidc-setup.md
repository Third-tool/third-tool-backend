# ts007: GHA OIDC AssumeRole 1회성 셋업·검증·트러블슈팅

Story-046로 `.github/workflows/dev-cicd.yml`이 `secrets.AWS_ACCESS_KEY_ID/SECRET` 대신 OIDC AssumeRole 패턴으로 전환됐다. 본 가이드는 AWS 콘솔 측 1회성 셋업 절차 + 검증 + 자주 보는 에러 5건을 한 곳에 모았다.

ts005(Dockerfile)·ts006(로컬 실행)과 cross-link. 본 절차는 repository 합당 권한자(또는 1인 운영자) 1회만 실행하면 됨.

---

## 1. 전제

- AWS 콘솔 IAM 관리 권한 (`iam:CreateOpenIDConnectProvider` · `iam:CreateRole` · `iam:PutRolePolicy`)
- GitHub repository `Third-tool/third-tool-backend` Settings 접근 권한 (Actions Variables 등록)
- repository 코드에 `infra/iam/gha-deploy-role-trust-policy.json` + `gha-deploy-role-permissions-policy.json` 존재 확인 (Story-046 머지 후)

---

## 2. AWS 콘솔 — OIDC Identity Provider 생성

AWS 계정당 **1회만**. 이미 다른 GHA workflow에서 등록한 적이 있으면 skip.

1. AWS 콘솔 → **IAM** → **Identity providers** → **Add provider**
2. Provider type: **OpenID Connect**
3. Provider URL: `https://token.actions.githubusercontent.com`
4. **Get thumbprint** 클릭 → 자동 fetch (현재 AWS는 thumbprint 검증을 더 이상 강제하지 않지만 콘솔 입력은 필수)
5. Audience: `sts.amazonaws.com`
6. **Add provider** 클릭

생성된 Provider ARN 예: `arn:aws:iam::<ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com`

---

## 3. AWS 콘솔 — `gha-deploy-role` IAM Role 생성

1. AWS 콘솔 → **IAM** → **Roles** → **Create role**
2. Trusted entity type: **Custom trust policy**
3. Custom trust policy 입력란에 **`infra/iam/gha-deploy-role-trust-policy.json` 내용 붙여넣기**
   - **`<AWS_ACCOUNT_ID>` 12자리 자기 계정 ID로 치환** (콘솔 우측 상단에서 확인)
4. Permission policies: 다음 단계에서 별도 정의 (skip)
5. Role name: `gha-deploy-role`
6. **Create role** 클릭
7. 생성된 Role 상세 → **Permissions** → **Add permissions** → **Create inline policy** → JSON
8. **`infra/iam/gha-deploy-role-permissions-policy.json` 내용 붙여넣기** + `<AWS_ACCOUNT_ID>` 치환
9. Policy name: `gha-deploy-role-permissions`
10. **Create policy** 클릭

> **치환 누락 검증 (필수)**: 두 JSON 모두 콘솔에 붙여넣기 **직전에** `<` 문자가 0건임을 시각 확인. `<AWS_ACCOUNT_ID>` 그대로 콘솔에 입력 시 AWS가 invalid ARN으로 거부 → Role/Policy 생성 자체가 실패. (CLI 사용 시: `grep -c '<' /tmp/role-policy.json` → 0이어야 함)

---

## 4. GitHub repo Settings — `AWS_ACCOUNT_ID` Variable 등록

1. GitHub repo `Third-tool/third-tool-backend` → **Settings** → **Secrets and variables** → **Actions** → **Variables** tab
2. **New repository variable** 클릭
3. Name: `AWS_ACCOUNT_ID`
4. Value: 12자리 AWS 계정 ID (Secret이 아닌 Variable — 비밀 아님)
5. **Add variable** 클릭

> Variable로 등록하는 이유: AWS Account ID는 비밀이 아님 (CloudTrail 등에서 노출). 비밀이 아닌 값은 `secrets.*`가 아니라 `vars.*`로 관리하는 것이 GitHub Actions 권장 패턴.

---

## 5. 검증

### 5.1 workflow_dispatch 트리거 (Elasticsearch 이미지 빌드)

```bash
gh workflow run dev-cicd.yml --ref main -f build_es=true
```

또는 GitHub 콘솔 → Actions → Deploy To EC2 → Run workflow → `build_es=true` 체크 → Run.

이 시도는 OIDC 인증 + ECR push만 검증하고 EC2 SSH 배포는 발생 안 함.

### 5.2 GHA workflow 로그 확인

`Configure AWS credentials (OIDC)` step 출력에서 다음 라인 확인:

```
Configuring AWS credentials...
Assuming role with OIDC
Authenticated as assumedRoleId: AROA...:GitHubActions
```

`AccessDenied` 또는 `Could not retrieve OIDC token` 발생 시 §6 트러블슈팅.

### 5.3 CloudTrail에서 AssumeRoleWithWebIdentity 이벤트 확인

AWS 콘솔 → **CloudTrail** → **Event history** → Filter: `Event name = AssumeRoleWithWebIdentity`

직전 GHA run 시각에 1건 이벤트가 떠야 한다. 상세에서:
- `userIdentity.principalId`에 `token.actions.githubusercontent.com` 포함
- `requestParameters.roleArn`이 `arn:aws:iam::<ACCT>:role/gha-deploy-role`
- `responseElements.assumedRoleUser.assumedRoleId`로 임시 자격증명 발급 확인

### 5.4 ECR push 검증 + 리포명 정합성

`Build and Push ES Image` step이 성공하면 push가 발생한 것. AWS 콘솔 → **ECR** → `third-tool-elaticsearch` 리포 → 최신 image tag 시각 확인.

> **리포명 오타 주의**: `infra/iam/gha-deploy-role-permissions-policy.json`의 `Resource`와 `dev-cicd.yml`의 docker push tag가 **`third-tool-elaticsearch`** (오타 — "elasticsearch"가 아님)로 일치돼 있다. 실제 AWS ECR 리포 이름과 정확히 매치되는지 사전 확인:
> ```bash
> aws ecr describe-repositories --region ap-northeast-2 \
>   | jq -r '.repositories[].repositoryName' | sort
> ```
> 출력에 `third-tool-elaticsearch`가 그대로 있으면 OK. `third-tool-elasticsearch`(정확 철자)로 존재하면 ECR 리포 이름과 정책/workflow 양쪽 정합성 정정 필요 — 별도 Story로 분리.

### 5.5 End-to-end 검증 (main branch push 배포)

§5.1-5.4는 ECR push까지만 검증한다. EC2 SSH 배포 step + 컨테이너 부팅 + S3 접근까지의 end-to-end는 **main branch 실제 push로만** 검증된다:

1. small one-line change(예: README 1줄)로 main push trigger
2. GHA workflow 로그에서 `Configure AWS credentials (OIDC)` step 성공 (§5.2 동일)
3. `Push to Amazon ECR` step 성공
4. `Deploy to EC2 via SSH` step에서 `🎉 Deployment Success!` 라인 확인
5. `curl https://<dev-domain>/health` 200 OK (또는 EC2 host:8080)
6. EC2 컨테이너 안에서 S3 접근 동작 확인 — 파일 업로드 endpoint 호출 1회 (transitional env var pass-through가 정상 작동하는지)

본 5단계 통과해야 Story-046이 진짜로 끝난 것. §5.1-5.4까지만 통과 후 머지하면 EC2 배포 회귀를 catch 못 함.

---

## 6. GitHub Secrets `AWS_ACCESS_KEY_ID/SECRET` 제거 시점

- **즉시 제거 가능한 부분**: 없음. workflow 측은 OIDC로 전환됐지만 **EC2 SSH 배포 step**의 컨테이너 env var pass-through(`-e AWS_ACCESS_KEY_ID="${{ secrets.AWS_ACCESS_KEY_ID }}" ...`)에서 여전히 참조.
- **완전 제거 시점**: milestone #11 (ECS Task Definition + Service) 이행 + S3Config가 `StaticCredentialsProvider` → `DefaultCredentialsProvider`로 전환된 이후. 그 시점에 EC2 SSH 배포 step 자체가 폐기 → 두 GitHub Secret 영구 삭제.
- 그 전까지는 Variable `AWS_ACCOUNT_ID` + Secret `AWS_ACCESS_KEY_ID/SECRET` **공존** 상태가 정상.

---

## 7. 트러블슈팅

### ts007-1: `Not authorized to perform sts:AssumeRoleWithWebIdentity`

**원인**: 신뢰 정책 `sub` 조건이 실제 GHA workflow context와 불일치.
- repo 슬러그 오타 (`Third-tool/third-tool-backend` 대문자/하이픈 정확)
- branch 다름 (main 외 branch에서 워크플로 실행했는데 신뢰 정책은 `refs/heads/main`만 허용)
- `aud` 조건 불일치 (`sts.amazonaws.com` 아닌 다른 값)

**해결**:
1. CloudTrail `AssumeRoleWithWebIdentity` 이벤트 상세에서 `requestParameters.subjectFromWebIdentityToken` 값 확인
2. trust policy `sub` 조건과 정확히 매치하는지 검토 (`StringLike`이므로 와일드카드 사용 시 패턴 점검)
3. 핫픽스 branch 등 main 외에서 배포 필요 시 staging Role을 별도로 만드는 것이 정석 (Story-046은 main 잠금 의도)

**검증**: 수정 후 §5.1 재실행 → §5.3 CloudTrail에서 새 이벤트 successful 확인.

### ts007-2: `Could not assume role with OIDC: AccessDenied`

**원인**: AWS 계정에 OIDC Identity Provider가 등록 안 됨 또는 ARN 불일치.

**해결**:
1. AWS 콘솔 → IAM → Identity providers → `token.actions.githubusercontent.com` 존재 확인
2. 없으면 §2 절차로 등록
3. Provider ARN이 신뢰 정책 `Principal.Federated` 값과 정확히 일치하는지 비교 (`<AWS_ACCOUNT_ID>` 치환 누락 여부)

### ts007-3: `Error: Could not retrieve OIDC token`

**원인**: workflow 또는 job 레벨에 `permissions: id-token: write` 누락. workflow runner가 OIDC token 발급 자체를 못 함.

**해결**: `.github/workflows/dev-cicd.yml` 상단 `permissions:` 블록에 `id-token: write` 명시 (Story-046 적용 상태 — 본 에러 발생 시 머지 후 브랜치 stale 의심).

**검증**:
```bash
grep -A2 '^permissions:' .github/workflows/dev-cicd.yml
# 기대 출력: id-token: write
```

### ts007-4: `vars.AWS_ACCOUNT_ID` 미설정 → 빈 ARN → AssumeRole 실패

GHA workflow 로그에서 다음 형태:
```
role-to-assume: arn:aws:iam:::role/gha-deploy-role
                              ^^ 빈 account id
```

**원인**: §4 GitHub Variables에 `AWS_ACCOUNT_ID` 미등록. `vars.AWS_ACCOUNT_ID`가 빈 문자열로 해석돼 ARN이 깨짐.

**해결**: §4 절차로 Variable 등록 → workflow 재실행.

**검증**: workflow 로그에서 `role-to-assume: arn:aws:iam::123456789012:role/...` 형태 정상 ARN 확인 (Variable은 로그에 마스킹 없이 출력 — 비밀 아님).

### ts007-5: ECR push 시 `denied: ... ecr:PutImage` 권한 부족

**원인**: `gha-deploy-role` permissions 정책의 `Resource`가 실제 ECR repository ARN과 불일치.

**해결**:
1. `infra/iam/gha-deploy-role-permissions-policy.json` `Resource` 라인에서 repo 이름 확인 (`third-tool-server`, `third-tool-elaticsearch`)
2. AWS 콘솔 → ECR → 리포 목록에서 실제 이름과 정확 매치 확인 (오타 포함 — `third-tool-elaticsearch` 그대로)
3. `<AWS_ACCOUNT_ID>` 치환이 됐는지 확인 (AWS 콘솔 IAM Role policy 탭에서 직접 본 값으로 비교)
4. 다른 region에 리포가 있다면 region prefix(`ap-northeast-2`) 확인

### ts007-6: EC2 배포 step에서 `AWS_ACCESS_KEY_ID/SECRET` 빈 값 → S3 인증 실패

GHA workflow 로그에서 EC2 step은 성공했으나, EC2 컨테이너 로그에서:
```
software.amazon.awssdk.services.s3.model.S3Exception: The AWS Access Key Id you provided does not exist (Status Code: 403)
```
또는 부팅 단계에서:
```
Could not resolve placeholder 'AWS_ACCESS_KEY_ID'
```

**원인**: GitHub repo Settings에서 `AWS_ACCESS_KEY_ID` 또는 `AWS_SECRET_ACCESS_KEY` Secret이 **이미 삭제됨**. Story-046 머지 후 milestone "비밀 신호" 달성 의도로 선제 삭제했지만, EC2 컨테이너 step이 여전히 두 Secret을 참조 중 → 빈 값 주입.

**해결**:
1. GitHub repo Settings → Secrets and variables → Actions → Secrets tab에서 `AWS_ACCESS_KEY_ID` · `AWS_SECRET_ACCESS_KEY` 존재 여부 확인
2. 없으면 §6 가이드대로 milestone 0.0.1v item #11 (ECS Task Role) 이행 **전까지는 보존** — 두 Secret을 다시 등록
3. milestone item #11 머지 + S3Config가 `DefaultCredentialsProvider`로 전환 + EC2 SSH 배포 step 제거 완료 후에 영구 삭제

**검증**: ts006 §6 (진단 정보 수집)의 환경변수 length 확인 명령 EC2 host에서 실행. length 0이면 Secret 미주입 상태.

---

## 8. 후속 (별도 Story)

- **`ecs:UpdateService` · `iam:PassRole` 권한 추가** — milestone #11 (Task Definition + Service)에서 동일 `gha-deploy-role` 정책 확장
- **staging IAM Role 분리** — `refs/heads/develop` 잠금된 별도 Role, 환경 분리 Epic
- **EC2 SSH 배포 + AWS_ACCESS_KEY_ID/SECRET env var 제거** — milestone #11 이후
- **`S3Config.java` → `DefaultCredentialsProvider` 전환** — ECS Task Role 도입과 함께
- **`workflow_dispatch`로 무인 검증 step 추가** — `aws sts get-caller-identity` 호출만 하는 dry-run job. 1회성 셋업 검증용 (옵션)

---

## 관련

- [`ts005-dockerfile-build.md`](ts005-dockerfile-build.md) — 본 OIDC 적용된 workflow가 빌드하는 이미지
- [`ts006-local-run-checklist.md`](ts006-local-run-checklist.md) — 로컬 IAM 사용자 access key 발급 (개발자 본인용, 본 OIDC와 별도)
- [ADR013](../../adr/ADR013-gha-oidc-assume-role.md) — GHA OIDC AssumeRole 결정 배경 + 대안 비교
- Story-046 — milestone item #10. `workflow/task/milestones/version/0.0.1v/milestone.md`
- 후속: milestone #11 (ECS Task Def + Service)에서 동일 Role에 `ecs:*` 권한 추가
