# ADR013: GitHub Actions → AWS 인증은 OIDC AssumeRole로 전환한다 (Story-046)

- **상태**: Accepted
- **날짜**: 2026-06-29
- **관련**: Product 5 컨테이너 배포 Epic 3 (GHA OIDC → ECS 무중단 배포) · milestone item #10 (`workflow/task/milestones/version/0.0.1v/milestone.md`) · `docs/operations/troubleshooting/ts007-gha-oidc-setup.md` · `.github/workflows/dev-cicd.yml`

## 컨텍스트

`.github/workflows/dev-cicd.yml`은 ECR push를 위해 `aws-actions/configure-aws-credentials@v4` step에 `secrets.AWS_ACCESS_KEY_ID` · `secrets.AWS_SECRET_ACCESS_KEY`를 넘기는 IAM User access key 패턴을 사용 중이다. 이 패턴은 다음 한계를 가진다:

- **long-lived 자격증명**: 회전(rotate) 작업이 정책 의지에 의존 → 실무에서 1~2년 방치 흔함
- **노출 영향 큼**: GitHub Secret이 외부에 유출되면 AWS 리소스에 대한 무제한 시간 접근 허용 (TTL 없음). revoke까지 발견·조치 시간 동안 무방비
- **GitHub Secrets에 영구 보관**: milestone "종료 신호 — 비밀 신호: GitHub Secrets 의존 0건" 요건과 직접 충돌
- **branch별 권한 경계 부재**: 어떤 branch에서 워크플로 실행하든 동일한 IAM User 권한 사용 → fork PR · 임의 branch에서의 prod 자격증명 획득 가능 (current workflow는 `push: main`만 트리거이지만 IAM 수준 경계는 없음)

`product-infra-deploy.md` KPI "GitHub Secrets에 저장된 AWS access key = 0" 목표 + milestone Tier 1 #10 "EC2 SSH 키 폐기 진입점"이 본 ADR의 의사결정 트리거다.

## 결정

**GitHub Actions → AWS 인증은 OIDC AssumeRoleWithWebIdentity로 단독 전환**한다.

### 인증 흐름

1. GitHub Actions runner가 workflow 시작 시 OIDC ID token 발급 (workflow에 `permissions: id-token: write` 명시 필수)
2. workflow의 `aws-actions/configure-aws-credentials@v4` step이 `role-to-assume`만 입력으로 받음
3. AWS STS가 OIDC token 검증 (issuer `token.actions.githubusercontent.com` + 신뢰 정책 `sub`/`aud` 조건 매치)
4. 검증 성공 시 1시간 TTL 임시 자격증명 발급 → workflow에서 ECR push 등 수행

### 신뢰 정책 sub 조건

`gha-deploy-role` 신뢰 정책의 `Condition.StringLike."token.actions.githubusercontent.com:sub"`를 다음으로 잠근다:

```
repo:Third-tool/third-tool:ref:refs/heads/main
```

→ main branch에서의 workflow 실행만 허용. fork PR · 임의 branch에서의 prod 자격증명 획득 차단.

### 권한 정책 최소화

`gha-deploy-role`에 부여하는 inline policy는 현 ECR push 동작에 필요한 최소 actions만 포함:
- `ecr:GetAuthorizationToken` (resource `*` — STS endpoint 호출용)
- `ecr:BatchCheckLayerAvailability` · `BatchGetImage` · `GetDownloadUrlForLayer` · `InitiateLayerUpload` · `UploadLayerPart` · `CompleteLayerUpload` · `PutImage` (resource: `third-tool-server` · `third-tool-elaticsearch` 두 ECR repo로 한정)

milestone #11 (ECS Task Def + Service) 이행 시 `ecs:UpdateService` · `iam:PassRole`을 동일 Role에 추가 — 별도 Story로 위임.

### Account ID 노출 정책

`vars.AWS_ACCOUNT_ID`를 GitHub Actions **Variable**(Secret 아님)로 등록. AWS 계정 ID는 CloudTrail 등에서 자연 노출되는 값이라 비밀이 아니며, workflow ARN(`arn:aws:iam::${{ vars.AWS_ACCOUNT_ID }}:role/gha-deploy-role`) 가독성을 우선한다.

### 본 ADR이 다루지 않는 범위

- **EC2 컨테이너 → AWS S3 인증**: EC2 SSH 배포 step에서 `-e AWS_ACCESS_KEY_ID=... -e AWS_SECRET_ACCESS_KEY=...` env var pass-through는 transitional 유지. 실제 제거는 milestone #11 ECS Task Role 이행 시.
- **`S3Config.java`의 `StaticCredentialsProvider`**: Task Role 도입과 묶어 `DefaultCredentialsProvider`로 전환하는 별도 Story로 위임.
- **staging IAM Role**: `refs/heads/develop` 잠금된 별도 Role은 환경 분리 Epic(Product 5 Epic 4)에서 추가.

## 결과 (Consequences)

### 긍정적

- **GitHub Secrets에 저장된 AWS access key (workflow 측 의존) 0건**: milestone "비밀 신호" 요건의 선행 차단 항목 해결
- **1시간 TTL 임시 자격증명**: 노출 시 영향 시간이 분~시간 단위로 줄어듦. revoke 자동 (시간 경과)
- **Branch별 권한 경계**: sub 조건 잠금으로 fork PR · 임의 branch에서의 prod 자격증명 획득 원천 차단
- **회전 운영 부담 0**: long-lived key 회전 절차·캘린더 불필요
- **감사 추적 향상**: CloudTrail `AssumeRoleWithWebIdentity` 이벤트가 workflow run 1건당 1건으로 매핑 → IAM User access key 사용 시보다 훨씬 명확한 사용 이력

### 트레이드오프 / 부정적

- **AWS 콘솔 1회 셋업 비용**: OIDC Identity Provider 등록 + IAM Role 생성 + trust policy/permissions policy 입력 (수동, ~15분). ts007 runbook으로 절차 표준화.
- **GitHub Enterprise Server 환경 사용 불가**: OIDC issuer는 `token.actions.githubusercontent.com` (GitHub.com)만 지원. self-hosted GHES는 별도 OIDC issuer 등록 필요 — 본 프로젝트는 GitHub.com 사용이라 비현실적 제약은 아님.
- **`vars.AWS_ACCOUNT_ID` 미설정 시 ARN 깨짐**: workflow 신규 환경 deploy 시 한 단계 추가. ts007-4로 트러블슈팅 명시.
- **EC2 SSH 배포 step의 access key 잔존**: workflow 측은 OIDC지만 컨테이너 env var pass-through에 access key가 남아 있어 milestone "GitHub Secrets 0건"은 #11까지 부분 달성 상태. 본 ADR의 명시적 follow-up 항목.

## 대안 비교

| 대안 | 장점 | 거부 사유 |
| --- | --- | --- |
| **A. IAM User access key (기존)** | 친숙, 즉시 사용 가능 | long-lived 자격증명, 회전 의지 의존, branch 경계 부재, milestone "비밀 신호" 위반 |
| **B. AssumeRoleWithSAML** | 기업 IdP 연동 시 가치 | GitHub Actions OIDC issuer 직접 신뢰 가능하므로 SAML 어셈블리 불필요. 운영 복잡도 증가 |
| **C (선택). OIDC AssumeRoleWithWebIdentity** | TTL 1h, branch sub 잠금, 회전 0, CloudTrail 깔끔 | 1회 셋업 비용 + GHES 미지원 (본 프로젝트는 GitHub.com이므로 무영향) |
| **D. AWS IAM Identity Center (SSO) + assume-role** | 인간 사용자에는 표준 | GitHub Actions runner는 사람이 아니므로 SSO 흐름 부적합. CI 자동화에 어색 |
| **E. EC2 Instance Profile로 ECR push (현재 self-hosted runner 없음)** | OIDC 셋업 불요 | self-hosted runner 운영 부담이 OIDC 셋업 비용을 압도. 현 GitHub-hosted runner 정책과 충돌 |

## 알려진 follow-up (본 ADR 범위 외)

- **milestone #11 (ECS Task Definition + Service)**: 동일 `gha-deploy-role`에 `ecs:UpdateService` · `iam:PassRole(ecs-task-execution-role, ecs-task-role)` 추가. permissions policy JSON 확장.
- **staging IAM Role 분리**: `refs/heads/develop` 잠금된 `gha-deploy-role-staging`을 환경 분리 Epic에서 생성. trust policy `sub` 조건이 prod와 다름.
- **EC2 SSH 배포 + AWS_ACCESS_KEY_ID/SECRET env var pass-through 폐기**: milestone #11 ECS 이행 시점에 일괄. S3Config `DefaultCredentialsProvider` 전환과 함께.
- **`S3Config.java` 리팩터링**: `StaticCredentialsProvider` 제거 + `DefaultCredentialsProvider.create()`로 전환. ECS Task Role 도입 시 자동 픽업.
- **GitHub Secrets `AWS_ACCESS_KEY_ID/SECRET` 영구 삭제**: 위 두 follow-up 완료 후 GitHub repo Settings에서 직접 삭제. 그 시점에 ts007 §6 갱신.
- **GCP Workload Identity Federation (Spring AI Gemini용)**: ECS Task Role이 `sts:AssumeRoleWithWebIdentity`로 GCP 인증하는 패턴은 Product 7 (Secrets·Terraform)에서 구체화.

## 다시 검토할 시점

- **GHES로 이전하는 시점**: GitHub Enterprise Server 도입 시 OIDC issuer 변경 필요 → trust policy 재작성.
- **multi-account 운영 시점**: 현재 단일 AWS 계정. dev/prod 계정 분리 시 `gha-deploy-role`을 prod 계정에 두고 dev에서 cross-account AssumeRole 패턴 추가 필요.
- **OIDC sub claim 형식 변경 시점**: GitHub Actions의 OIDC token claim 구조 변경 가능성. 정책 검토 주기 6~12개월.
- **CodeBuild/CodePipeline 도입 시점**: GHA를 대체하거나 병행하면 OIDC issuer를 codebuild로 분기.
