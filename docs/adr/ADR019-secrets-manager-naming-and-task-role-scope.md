# ADR019: Secrets Manager 5종 비밀 명명 + env-prefix 격리 + Task Role Resource scope (Story-052)

- **상태**: Accepted
- **날짜**: 2026-06-30
- **관련**: Product 7 Secrets·Terraform Epic 1 (`workflow/task/pes/workspectrum/sdd/in-progress/product-infra-ops.md`) · milestone 0.0.1v item #15 (`workflow/task/milestones/version/0.0.1v/milestone.md`) · `docs/operations/troubleshooting/ts012-secrets-manager-setup.md` · `infra/secrets/` · `infra/iam/ecs-task-role-permissions-policy.json` · `infra/ecs/task-definition-prod.json` · ADR014(IAM Role 분리) · ADR018(RDS) · 후속 Story-053 (Spring Cloud AWS bootstrap)

## 컨텍스트

Story-046(GHA OIDC) + Story-047(ECS Task Def secrets ARN placeholder) + Story-051(RDS) 후 다음 의존 단계는 **실제 비밀의 영속 저장소** — AWS Secrets Manager. 본 Story가 만들어지면:

- task-definition-prod.json의 `<SUFFIX>` placeholder 해소 (ts012 출력 메모로 채움)
- GitHub Actions Secrets에 보관되던 `DB_PASSWORD`, `JWT_SECRET_KEY`, `KAKAO_CLIENT_SECRET`, `NAVER_CLIENT_SECRET` 5건 폐기 (GitHub Secrets 의존 0건 달성)
- 후속 Story-053이 Spring Cloud AWS Secrets Manager bootstrap으로 application-prod.yml placeholder를 자동 치환할 진입점
- AI Epic 3 (Story-058+) 진입 시 Gemini API key 사전 자리 마련

추가 결정 영역:
- **시크릿 분할 단위**: 모든 비밀 1개 시크릿에 통합 vs 도메인별 분할 vs 5종 분할 vs JSON multi-key 활용
- **환경 분리 방식**: 시크릿 이름 prefix(`thirdtool/{env}/...`) vs Tag 기반 ABAC vs Role 분리(env마다 별도 Role)
- **회전 정책**: M1 활성 vs M2 활성 vs 비활성
- **AWS IAM User access key (S3Config용)**: Secrets Manager에 저장 유지 vs DefaultCredentialsProvider 전환 후 폐기
- **암호화 키**: AWS managed `aws/secretsmanager` vs Customer Managed KMS Key (CMK)

## 결정

### 5종 비밀 분할 + JSON multi-key

| 시크릿 | 키 | 값 출처 |
| --- | --- | --- |
| `thirdtool/{env}/db-credential` | HOST, PORT, NAME, USERNAME, PASSWORD | RDS endpoint (ts011) + 운영자 결정 |
| `thirdtool/{env}/jwt-secret` | SECRET_KEY | `openssl rand -base64 48` (384 bit ≥ HS512 최소 256 bit) |
| `thirdtool/{env}/oauth-kakao` | CLIENT_ID, CLIENT_SECRET | 카카오 developers 콘솔 |
| `thirdtool/{env}/oauth-naver` | CLIENT_ID, CLIENT_SECRET | 네이버 developers 콘솔 |
| `thirdtool/{env}/gemini-api-key` | API_KEY | Google AI Studio (M1은 placeholder 등록만) |

- **단일 통합 시크릿 거부**: 키 회전 단위가 비밀별로 다름(JWT 회전 vs OAuth 회전 vs DB 회전). 단일 통합은 한 키 회전이 다른 비밀 cache invalidation을 트리거 → 부팅 jitter.
- **kakao+naver 통합 거부**: 향후 oauth provider 추가(애플/구글) 시 시크릿 분리 패턴 강제. 통합 시 키 충돌(`KAKAO_CLIENT_ID` vs `GOOGLE_CLIENT_ID`) 회피용 prefix 강제 → JSON 키가 길어짐.
- **5종 분할 채택**: 회전 단위 + 발급 출처가 모두 비밀별로 다름. JSON multi-key는 동일 출처의 key/secret pair는 함께 두어 운영 일관성 유지.

### env-prefix 격리 (`thirdtool/{env}/...`)

- 격리 방식: 비밀 이름 prefix (`thirdtool/dev/...` · `thirdtool/staging/...` · `thirdtool/prod/...`).
- Task Role IAM 정책의 `Resource` 배열에 env별 prefix를 ARN 와일드카드로 명시.
- **dev/prod 통합 운영 (M1)**: 단일 Task Role이 두 env 모두 접근(`dev/*` + `prod/*`). M1 합격선은 dev only지만 prod 진입 시 즉시 사용 가능하도록 미리 등록. env별 Role 분리는 환경 분리 Epic에서.
- staging은 별도 Role(`staging-task-role` — 본 ADR 범위 외) — env 격리 보장.

**Tag 기반 ABAC 거부**: IAM Condition `aws:ResourceTag/Environment` 방식은 시크릿마다 Tag 누락 시 위험. ARN prefix는 시크릿 이름이 자체 검증.

### 회전 정책: 비활성 (M1)

- 자동 회전 disable. 수동 회전이 필요한 경우 새 시크릿 발급 후 Task Def revision 갱신 + Service 재배포.
- M2 진입 시 RDS Multi-AZ + Secrets Manager Rotation Lambda 동시 검토.

### IAM User access key는 별도 처리 (본 ADR 범위 외)

`AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`는 Secrets Manager에 보관하지 않는다. 대신 ECS Task Role IAM의 S3 권한(`S3BucketReadWriteThirdToolServer` Statement)을 통해 `software.amazon.awssdk:s3:2.30.0`의 `DefaultCredentialsProvider`가 EC2 Container Credentials Provider 경로로 자동 인증. **본 전환은 Story-053(S3Config 코드 변경)에서 수행**. 본 ADR은 5종 비밀에 한정.

### 암호화 키: AWS managed `aws/secretsmanager`

- 사용 시점 KMS 비용 없음(AWS-managed key는 시간당 무료).
- M2 트래픽 도달 후 audit log 필요성 발생 시 CMK 전환 검토. CMK 전환 시 시크릿 변경 없이 `aws kms re-encrypt` 1회 실행으로 완료.

### Task Role Statement 2개 추가

```json
{
  "Sid": "SecretsManagerReadOwnEnvPrefixes",
  "Effect": "Allow",
  "Action": ["secretsmanager:GetSecretValue", "secretsmanager:DescribeSecret"],
  "Resource": [
    "arn:aws:secretsmanager:ap-northeast-2:*:secret:thirdtool/dev/db-credential-*",
    ... (10건 — 5 type × 2 env)
  ]
}
{
  "Sid": "KMSDecryptForSecretsManager",
  "Effect": "Allow",
  "Action": ["kms:Decrypt"],
  "Resource": "*",
  "Condition": { "StringEquals": { "kms:ViaService": "secretsmanager.ap-northeast-2.amazonaws.com" } }
}
```

- `secretsmanager:DescribeSecret`는 Spring Cloud AWS bootstrap이 시크릿 메타데이터 조회 시 필요(Story-053).
- `kms:Decrypt` Condition으로 다른 AWS service 경유 decrypt는 차단 — Secrets Manager 호출 컨텍스트로만 한정.

### 본 ADR이 다루지 않는 범위

- **staging Task Role 분리**: 환경 분리 Epic에서 별도 Role + Resource scope 분리
- **S3Config DefaultCredentialsProvider 전환**: Story-053 (Spring Cloud AWS 측)
- **CMK 전환**: M2 audit log 필요성 발생 후
- **회전 Lambda**: M2 RDS Multi-AZ와 동시
- **VPC Endpoint (Secrets Manager Interface)**: NAT 트래픽 절감용 (Product 7 Epic 2)
- **Secrets Manager replication (multi-region)**: us-east-1 (CloudFront ACM) 등 별도 region 비밀 필요 시점에 검토

## 결과 (Consequences)

### 긍정적

- **5종 비밀 회전 독립성**: JWT 회전이 OAuth 클라이언트에 영향 없음. DB password 회전이 Gemini API key 캐시에 영향 없음.
- **env-prefix로 cross-env 누수 차단**: prod Task가 실수로 dev 비밀 호출해도 IAM에서 거부. **GitHub Secrets에 5종 비밀 보관 0건** — `git log -p` / `git secrets scan` 검사로 평문 비밀 누설 위험 0.
- **JSON multi-key로 동일 출처 비밀 그룹화**: OAuth provider 1곳에서 발급한 client_id + client_secret을 단일 시크릿 회전으로 동시 갱신 가능.
- **Spring Cloud AWS bootstrap (Story-053) 진입 준비**: 본 명명 규칙이 application-prod.yml placeholder와 매핑되어 boot 시점 자동 치환 가능.
- **AWS managed KMS로 비용 0**: 시크릿당 월 $0.40만 부담 (5종 × $0.40 = $2/월 dev env 단일 기준).

### 트레이드오프 / 부정적

- **시크릿당 월 $0.40 비용 누적**: 5종 × 3 env = 15 secrets × $0.40 = $6/월 (전 환경 등록 시). API call cost는 별도 ($0.05 per 10K calls — Spring Cloud AWS는 부팅 시 1회 + 캐시 → 부담 없음).
- **수동 회전 운영 부담**: 회전 자동화 부재로 운영자가 회전 주기 직접 관리. 운영자 1인 환경에서 회전 주기 트래킹이 잊혀질 위험. M2 Lambda 도입 시 해소.
- **ARN suffix(`-AbCd9X`) 관리**: 시크릿마다 다른 suffix를 ts012 출력 메모로 보관 + Task Def 치환. 운영자가 메모 누락 시 Task 부팅 실패. ts012가 절차 명시.
- **dev/prod 통합 Task Role (M1)**: M1 합격선은 dev only지만 prod 진입 시 즉시 동일 Role 사용 → cross-env 침범 위험. env 분리 Epic에서 Role 분리 필수.
- **DescribeSecret 권한 추가**: Spring Cloud AWS bootstrap이 사용하지만, M1 단계에서 사용 안 함 (Story-053 머지 전엔 dead permission). 보안 surface 약간 확대지만 list 권한 미부여로 enumeration 차단.
- **VPC Endpoint 부재 시 NAT 트래픽 발생**: ECS Task가 Secrets Manager 호출 시 NAT Gateway 경유. 부팅 시점 1회만 발생 → 비용 미미 (~$0.001/월/task). M2 Epic 2 (VPC Endpoint) 도입 후 0으로 감소.

## 대안 비교

| 대안 | 장점 | 거부 사유 |
| --- | --- | --- |
| **A. 모든 비밀 단일 시크릿 (`thirdtool/{env}/all`)** | API call 1회, 비용 1/5 | 회전 단위가 다른 비밀 묶임 → 1 비밀 회전이 전체 cache invalidation 트리거. JWT 회전이 DB cache까지 무효화 |
| **B. 도메인별 분할 (`thirdtool/{env}/auth`, `thirdtool/{env}/data`)** | API call 2-3회 | 회전 단위 차이는 여전. provider 추가 시 통합 시크릿 키 충돌 |
| **C (선택). 5종 분할 + JSON multi-key** | 회전 독립 + 발급 출처별 그룹화 + provider 추가 유연 | 시크릿 수 증가 → 비용 약간 |
| **D. Parameter Store (SSM Parameter — SecureString)** | 비용 0 | 회전 자동화 부재, 부팅 시점 1 API call에 1 parameter 강제(여러 keyparameter를 묶을 수 없음 → secret당 N call 필요), Cross-account 공유 미지원 |
| **E. Tag 기반 ABAC (`Environment=prod` Tag)** | env 추가 시 IAM Resource 갱신 불요 | 시크릿마다 Tag 누락 위험 + IAM Condition 디버깅 복잡. ARN prefix가 자체 검증 |
| **F. env별 Task Role 완전 분리** | cross-env 격리 강제 | M1은 단일 운영자 단일 env(dev) → 과도. M2 환경 분리 Epic에서 도입 |
| **G. 자동 회전 즉시 활성** | M1부터 회전 자동화 | Rotation Lambda 코드 작성 + 테스트 필요. M1 트래픽 0 + 운영자 1인 환경에 과도. M2 RDS Multi-AZ와 동시 도입이 효율 |
| **H. CMK (Customer Managed KMS)** | audit log + 권한 세분화 | KMS 시간당 $1/key 비용 + key rotation 관리. M1 audit 요구 부재. M2 검토 |

## 알려진 follow-up (본 ADR 범위 외)

- **Story-053 (Spring Cloud AWS bootstrap)**: 본 ADR 직후 — `spring-cloud-aws-starter-secrets-manager` 의존 + bootstrap.yml + application-prod.yml placeholder 매핑. 본 ADR §1.2의 비밀 이름 정합 필수
- **Story-TBD (S3Config DefaultCredentialsProvider 전환)**: `AWS_ACCESS_KEY_ID` 환경변수 의존 제거 + Task Role IAM 직접 사용. Story-053과 합쳐도 OK
- **Story-TBD (GitHub Secrets 폐기 verify)**: PR-A 머지 + ts012 등록 완료 후 GitHub Actions Secrets 페이지에서 `DB_PASSWORD`/`JWT_SECRET_KEY`/`KAKAO_*`/`NAVER_*` 5건 수동 삭제 + `git secrets scan`으로 평문 누설 검사
- **Story-TBD (staging Task Role 분리)**: 환경 분리 Epic — staging 전용 Task Role + `Resource: thirdtool/staging/*` 단일 prefix scope
- **Story-TBD (VPC Endpoint Secrets Manager Interface)**: Product 7 Epic 2 — NAT 트래픽 절감
- **Story-TBD (Rotation Lambda + RDS Multi-AZ)**: M2 트래픽 도달 시점
- **Story-TBD (CMK 전환)**: audit log 요구 발생 시
- **Story-TBD (cross-region replication)**: us-east-1 (CloudFront ACM) 외 region별 비밀 필요 시점

## 다시 검토할 시점

- **GitHub Secrets에 비밀 1건이라도 잔존 발견 시점**: ADR 위반 — 즉시 폐기 + ADR follow-up 진입
- **시크릿당 월 비용이 $0.40 × 30개 = $12 도달 시점**: env/팀별 통합 시크릿 분할 검토
- **회전 누락으로 인한 incident 발생 시점**: M2 Rotation Lambda 도입 가속
- **staging env 진입 시점**: 본 ADR §1.4의 Task Role 분리 + Resource scope 분리 즉시 적용
- **AI Epic 3 진입 시점 (Gemini 실제 사용)**: `thirdtool/{env}/gemini-api-key`에 placeholder 값을 실제 API key로 갱신 + 비용 모니터링 (Google AI Studio quota)
- **multi-region (us-east-1 CloudFront ACM) 비밀 필요 시점**: Secrets Manager replication 활성 검토 — 본 ADR §1.6에 추가
- **Customer Managed KMS audit log 필요 시점**: CMK 전환 (시크릿 변경 없이 re-encrypt 1회)
