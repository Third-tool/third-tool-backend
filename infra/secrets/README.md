# Secrets Manager — 5종 비밀 spec (Story-052)

> ECS Task가 부팅 시점에 환경변수로 주입받는 5종 비밀의 명명 규칙·페이로드 구조·env 분리 spec.
> 실제 값(value)은 `infra/`에 commit하지 않는다 — 본 디렉토리의 `*.template.json`은 **구조 예시만** 보여주는 placeholder.

ADR019 참조: `docs/adr/ADR019-secrets-manager-naming-and-task-role-scope.md`
운영 절차: `docs/operations/troubleshooting/ts012-secrets-manager-setup.md`
통합 흐름: `workflow/task/pes/handoff/aws-setup-0.0.1v.md` §7 (Stage 5)

---

## 1. 명명 규칙

```
thirdtool/{env}/{type}
```

- `{env}`: `dev` · `staging` · `prod` (3종, 환경별 완전 격리)
- `{type}`: 5종 — `db-credential` · `jwt-secret` · `oauth-kakao` · `oauth-naver` · `gemini-api-key`

총 5 type × 3 env = **15 secrets** (env별 Task Role이 자기 env prefix에만 접근).

> M1 합격선은 `dev` env 5종 등록(milestone item #15 + infra.md). `staging`·`prod`는 환경 분리 Epic 진행 시 동일 패턴으로 확장.

## 2. 5종 비밀 페이로드 구조

| Secret name | 페이로드 형식 | 키 | 값 출처 |
| --- | --- | --- | --- |
| `thirdtool/{env}/db-credential` | JSON multi-key | `HOST`, `PORT`, `NAME`, `USERNAME`, `PASSWORD` | RDS endpoint (ts011 §5 출력) + 운영자가 결정한 DB user/password |
| `thirdtool/{env}/jwt-secret` | JSON 1 key | `SECRET_KEY` | `openssl rand -base64 48` (HS512 충분, 384 bit ≥ 256 bit 최소 요구) |
| `thirdtool/{env}/oauth-kakao` | JSON 2 keys | `CLIENT_ID`, `CLIENT_SECRET` | 카카오 developers 콘솔 (ts006 §2.1) |
| `thirdtool/{env}/oauth-naver` | JSON 2 keys | `CLIENT_ID`, `CLIENT_SECRET` | 네이버 developers 콘솔 (ts006 §2.2) |
| `thirdtool/{env}/gemini-api-key` | JSON 1 key | `API_KEY` | Google AI Studio 발급 (M1은 사용 안 함, AI Epic 3 진입 시 활성) |

**환경별 격리 원칙**: env마다 별도 비밀. dev secret을 prod에서 참조하지 않는다. Task Role이 prefix 단위 권한으로 강제.

## 3. ECS Task Def secrets 참조 패턴

Task Definition의 `containerDefinitions[].secrets`에서 ARN + JSON 키 추출:

```
arn:aws:secretsmanager:ap-northeast-2:<AWS_ACCOUNT_ID>:secret:thirdtool/prod/db-credential-<SUFFIX>:HOST::
arn:aws:secretsmanager:ap-northeast-2:<AWS_ACCOUNT_ID>:secret:thirdtool/prod/jwt-secret-<SUFFIX>:SECRET_KEY::
arn:aws:secretsmanager:ap-northeast-2:<AWS_ACCOUNT_ID>:secret:thirdtool/prod/oauth-kakao-<SUFFIX>:CLIENT_ID::
arn:aws:secretsmanager:ap-northeast-2:<AWS_ACCOUNT_ID>:secret:thirdtool/prod/oauth-naver-<SUFFIX>:CLIENT_SECRET::
arn:aws:secretsmanager:ap-northeast-2:<AWS_ACCOUNT_ID>:secret:thirdtool/prod/gemini-api-key-<SUFFIX>:API_KEY::
```

- `<SUFFIX>`: AWS가 시크릿 생성 시 자동 부여하는 random 6 char (`-AbCd9X`). 시크릿마다 다르며, ts012 §4의 출력 메모 블록에 저장 후 Task Def에 치환.
- 마지막 `::` 2개 colon은 version stage / version id 생략 (AWS Task Def secrets 문법).

## 4. Task Role 권한 범위

각 env의 ECS Task Role은 **자기 env prefix의 비밀만** 읽을 수 있다 (cross-env 차단).

```
prod Task Role  → "Resource": "arn:aws:secretsmanager:*:*:secret:thirdtool/prod/*"
staging Task Role → "Resource": "arn:aws:secretsmanager:*:*:secret:thirdtool/staging/*"
dev Task Role   → "Resource": "arn:aws:secretsmanager:*:*:secret:thirdtool/dev/*"
```

본 디렉토리의 `infra/iam/ecs-task-role-permissions-policy.json`은 **현 시점 prod·dev 통합 운영(M1)** 기준 — env 분리 Epic 진입 시 env별 Role 분리 + Resource prefix scope 분리.

## 5. 회전 정책 (M1)

- **자동 회전 비활성** — M1 트래픽 0 + 운영자 1인. 수동 회전으로 충분.
- M2 진입 시 RDS multi-AZ + Secrets Manager rotation Lambda 동시 검토.

## 6. M1 합격 조건 (milestone.md item #15)

- [ ] `thirdtool/dev/db-credential` 등록 (5 keys)
- [ ] `thirdtool/dev/jwt-secret` 등록 (1 key)
- [ ] `thirdtool/dev/oauth-kakao` 등록 (2 keys)
- [ ] `thirdtool/dev/oauth-naver` 등록 (2 keys)
- [ ] `thirdtool/dev/gemini-api-key` 등록 (1 key) — 값은 placeholder OK (AI Epic 3 진입 전엔 미사용)
- [ ] Task Role policy에 `secretsmanager:GetSecretValue` + `thirdtool/dev/*` Resource scope 적용
- [ ] GitHub Secrets에서 `DB_PASSWORD` · `JWT_SECRET_KEY` · `KAKAO_*` · `NAVER_*` 폐기 (대체된 후)

상세 절차: ts012-secrets-manager-setup.md.
