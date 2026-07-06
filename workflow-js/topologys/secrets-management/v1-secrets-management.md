# Pinned Topology — `secrets-management` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님 (rotation 절차 상세는 별도 runbook)
> - vocabulary 아님 (실제 secret 이름·ARN·프로젝트 ID는 여기 없다)

**목적**: 크레덴셜·비밀·API 키의 저장·로드·순환 원칙을 pin. 로컬 개발 · 프로덕션 배포 · CI/CD 각 환경에서 원칙이 반복 지켜져야 함. 유출 위험은 프로젝트 파산 위험.

---

## 0. 유효 구간 (Validity)

| 항목 | 값 |
| --- | --- |
| Pinned at | 2026-07-21 |
| Valid for | 본 워크플로우 v1 구간 — re-pin trigger 발생 전까지 |
| Owner | 메인 Claude + 사용자 합의 |
| Re-pin trigger | 아래 §4 |

---

## 1. How Claude Code must use this file — 행동 계약

1. **크레덴셜·비밀 파일·API 키 관련 작업 시 이 파일을 먼저 읽는다.** §2의 저장·로드·경계는 본 구간 **고정 제약**.
2. **`application.yml`에 credentials 원문을 하드코딩하려는 정황이 보이면 STOP하고 보고한다.** 위반.
3. **`.gitignore` 등록 없이 크레덴셜 파일을 저장소에 두려는 정황이 보이면 보고**한다.
4. **로컬 dev 크레덴셜과 프로덕션 크레덴셜을 동일 저장소에 두려는 정황이 보이면 보고**한다.
5. **CI/CD에 long-lived AWS access key를 사용하려는 정황이 보이면 보고**한다. OIDC 원칙 위반.
6. **이 파일에 실제 secret 이름·값·ARN을 적지 않는다.** 그건 Secrets Manager와 GHA secrets에.

---

## 2. The pinned topology

### Nodes
- `Secrets Manager` — 프로덕션 크레덴셜의 단일 진실 소스 (AWS)
- `IAM task role` — ECS Task가 Secrets Manager 접근 시 사용하는 role
- `GHA OIDC` — GitHub Actions ↔ AWS 인증의 short-lived 방식 (long-lived access key 미사용)
- `로컬 dev 크레덴셜 파일` — 개발자 로컬에만 존재 · `.gitignore` 등록 · 절대 커밋 X
- `credentials 참조점` — `application-prod.yml` 등에서 Secrets Manager 참조 (직접 값 없음)
- `Spring Boot 부팅 로드` — 부팅 시점에 Secrets를 한 번에 로드 · 이후 재조회 없음
- `프로덕션 vs 로컬 크레덴셜 격리` — 완전 분리 저장소 · 로컬 크레덴셜은 프로덕션 리소스 접근 불가
- `rotation 절차` — v1 수동 rotation · 자동은 v2 이관
- `GHA secrets` — GHA workflow에서 사용하는 short-lived 값 (Slack webhook 등)
- `pre-commit hook` — 커밋 전 credential scan
- `PII / Secret 마스킹` — 로그·에러 응답에 절대 노출 X

### Edges
- 로컬 개발 → `로컬 dev 크레덴셜 파일` : 개발자 개인 환경에만 존재
- 로컬 dev → `application-dev.yml` → 환경변수 참조 (`GOOGLE_APPLICATION_CREDENTIALS` 등)
- 프로덕션 배포 → `Spring Boot 부팅 로드` → `Secrets Manager` : IAM task role 인증
- `credentials 참조점` → `Secrets Manager` : 직접 값 없이 참조만
- GHA workflow → `GHA OIDC` → AWS IAM temporary credentials : short-lived (long-lived access key 없음)
- GHA workflow → `GHA secrets` (외부 알림·외부 AI API 등 short-lived secret) : 저장소 secret 별도 관리
- 커밋 전 → `pre-commit hook` → credential pattern grep : 유출 사전 차단
- 로그 발행 → `PII / Secret 마스킹` : Secret 값이 로그 진입하지 않음 (observability topology와 중복 강제)
- 수동 rotation 요구 → `rotation 절차` : v1은 사용자 수동 · v2는 자동

### Boundaries
- **저장 경계**: 프로덕션 크레덴셜은 `Secrets Manager` **단일 진실 소스**. `application.yml`·`.env`·저장소 파일에 직접 값 저장 금지.
- **참조 경계**: `application-prod.yml`은 credentials 값이 없이 Secrets Manager 참조만. Spring Boot가 부팅 시 자동 로드.
- **격리 경계**: 로컬 dev 크레덴셜과 프로덕션 크레덴셜은 **완전 분리**. 로컬 크레덴셜은 프로덕션 리소스 접근 권한 없음.
- **커밋 경계**: 로컬 dev 크레덴셜 파일은 **절대 커밋 금지**. `.gitignore` 등록 + `pre-commit hook` 이중 방어.
- **CI 경계**: GHA는 `GHA OIDC` 로만 AWS 인증. long-lived access key 사용 금지. 다른 short-lived secret (Slack webhook 등) 은 GHA secrets에.
- **재조회 경계**: 부팅 시 로드 후 크레덴셜 재조회 없음. rotation 시 앱 재시작으로 반영.
- **로그·응답 경계**: Secret 값은 어떤 로그·에러 응답에도 raw 상태로 노출 X (observability topology 계승).
- **rotation 경계**: v1은 수동 rotation · 자동은 v2 이관 · 절차 문서화 (runbook 별도).

### Invariants
- `application.yml`·`application-prod.yml`에 credentials 원문 하드코딩 사례 0건
  - 감지법: yml 파일 grep · Secrets Manager 참조 문법 존재 확인
- 저장소 커밋에 크레덴셜 파일 포함 사례 0건
  - 감지법: `.gitignore` 등록 확인 · `pre-commit hook` credential scanner 존재
- GHA workflow에서 long-lived AWS access key 사용 사례 0건
  - 감지법: `.github/workflows/*.yml` grep · OIDC 방식 사용 확인
- Spring Boot 부팅 로그에 Secret 값이 stdout 출력된 사례 0건
  - 감지법: dev 로그 · 프로덕션 CloudWatch Logs sample 확인
- 로그·에러 응답에 이메일·비밀번호·JWT·refresh token·API 키 raw 노출 0건 (observability와 중복 강제)
  - 감지법: PII 마스킹 규칙 리뷰
- 로컬 dev 크레덴셜이 프로덕션 리소스에 접근한 사례 0건
  - 감지법: GCP·AWS IAM policy 리뷰 · 크레덴셜 스코프 확인
- Secrets Manager 5 secret 중 하나라도 부팅 시 로드 실패 후 fallback 값 사용한 사례 0건 (프로덕션)
  - 감지법: 부팅 로그 · CloudWatch Logs 확인

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 Secrets Manager secret 이름·ARN·값 → AWS Secrets Manager 콘솔 (M7 실물 신설)
- 실제 IAM task role 정책·GHA OIDC provider 설정 → AWS 콘솔 · GHA workflow
- 실제 로컬 크레덴셜 파일 위치 (`~/.config/gcloud/...` 등) → 개발자 개인 환경
- 실제 `application-{profile}.yml` credentials 참조 문법 → Spring Boot config 코드
- 실제 GHA secrets 목록 → `.github/workflows/*.yml` 참조 지점
- rotation 상세 절차 → `docs/runbooks/secrets-rotation.md` (M8 신설 예정)
- 왜 이렇게 박혔는지 → `docs/adr/` (secrets 관련 ADR · 미신설)

---

## 4. Re-pin trigger

- Secrets Manager → 다른 시크릿 저장소 이관 (HashiCorp Vault 등)
- GHA OIDC 폐기 · long-lived access key 재도입 (보안 재검토 필요)
- 로컬 dev와 프로덕션 크레덴셜 격리 폐기 (단일 저장소로 통합)
- 자동 rotation 도입 (v2) 로 rotation 절차·재조회 정책 변경
- Spring Boot 부팅 로드 방식 → 런타임 재조회 도입 (Secrets 갱신 즉시 반영 필요)
- 다중 클라우드 도입 (AWS + GCP + Azure) 으로 secrets 저장소 분산
- Zero-trust 모델 도입으로 IAM 정책·MFA 요구 재조정
