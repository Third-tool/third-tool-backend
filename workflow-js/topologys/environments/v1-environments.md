# Pinned Topology — `environments` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님 (실제 URL·인스턴스 이름·profile 상세는 여기 없다)

**목적**: dev/staging/prod 3환경 분리 원칙을 pin. 프로덕션 첫 착지 (M7) 부터 v1 릴리스 이후까지 계속 반복 지켜져야 함. `delivery`가 브랜치·PR 프로세스를 다룬다면, 본 파일은 **환경별 격리·프로필·마이그레이션 순차 적용·branch protection** 원칙을 다룬다.

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

1. **환경 분리·프로필 변경·배포 파이프라인 관련 작업 시 이 파일을 먼저 읽는다.** §2의 3환경 격리·순차 배포는 본 구간 **고정 제약**.
2. **staging 우회 · 프로덕션 직접 배포 정황이 보이면 STOP하고 보고한다.** 순차 배포 위반.
3. **dev 프로필이 프로덕션 리소스 (RDS·Secrets Manager prod) 에 접근하려는 정황이 보이면 보고**한다. 격리 위반.
4. **`main`·`develop`에 직접 push (PR 우회) 정황이 보이면 보고**한다. branch protection 위반.
5. **한 환경의 배포가 다른 환경의 실행 중 서비스에 영향을 주려는 정황이 보이면 보고**한다.
6. **이 파일에 실제 인스턴스명·URL·프로필 상세를 적지 않는다.** 그건 §3 포인터로.

---

## 2. The pinned topology

### Nodes
- `dev 환경` — 로컬 개발자 머신 · in-memory DB · 외부 AI API dry-run 또는 실호출 (개발자 스코프)
- `staging 환경` — 클라우드 · 관계형 DB single-AZ · 프로덕션 유사 config · 실사용자 접근 없음
- `prod 환경` — 클라우드 · 관계형 DB Multi-AZ · 실사용자 접근 · 관측·알람 활성
- `Spring profile` — `dev` · `staging` · `prod` 3개 프로필 분리
- `profile-specific config` — `application-{profile}.yml` (Secrets 참조·DB endpoint·log level 등)
- `Flyway 순차 적용` — 새 V파일은 dev → staging → prod 순서로 착지
- `staging dry-run` — 프로덕션 배포 전 staging에서 사전 검증 (migration-policy topology 계승)
- `branch protection` — `main`·`develop` 브랜치 직접 push 금지 · PR review 필수 · CI status 필수
- `환경별 secrets 스코프` — 각 환경마다 Secrets 분리 (secrets-management topology 계승)
- `배포 트리거` — GHA workflow · 브랜치 → 환경 매핑 (`develop → staging` · `main → prod`)
- `프로덕션 rollback` — 이전 git_sha로 즉시 rollback 가능 (ECS Task Definition 이력 활용)
- `환경별 관측` — 각 환경마다 CloudWatch Logs·Metrics·Alarm 분리
- `데이터 격리` — dev·staging·prod DB가 서로 데이터 공유 없음

### Edges
- 코드 변경 → PR → `develop` 머지 → `배포 트리거` → `staging 환경` : 자동
- `staging 환경` 검증 완료 → PR → `main` 머지 → `배포 트리거` → `prod 환경` : 자동 (사용자 최종 승인)
- 새 Flyway V파일 → `dev 환경` (H2) 검증 → `staging 환경` (MySQL) `staging dry-run` → `prod 환경` 착지 : `Flyway 순차 적용`
- 각 환경 → `profile-specific config` → `Spring profile` : 부팅 시 활성 프로필로 로드
- 각 환경 → `환경별 secrets 스코프` → Secrets Manager (환경별 IAM 격리)
- 프로덕션 이상 감지 → `프로덕션 rollback` → 이전 git_sha 재배포
- `main`·`develop` 브랜치 → `branch protection` : PR 리뷰·CI 통과 없이 push 불가
- `데이터 격리` : dev H2 데이터 · staging RDS 데이터 · prod RDS 데이터 상호 접근 없음

### Boundaries
- **환경 경계**: dev · staging · prod 는 **완전 격리**. 어떤 환경도 다른 환경의 리소스 (DB·Secrets·CloudWatch) 에 직접 접근 X.
- **순차 배포 경계**: 프로덕션 배포는 항상 **staging 우선**. staging 우회 프로덕션 직접 배포 금지 (release-gate 이관 예외 없음).
- **Flyway 순서 경계**: 새 V파일은 dev → staging → prod 순차 착지. 프로덕션 첫 착지 시 staging에서 dry-run 완료 필수 (migration-policy topology 계승).
- **profile 경계**: Spring profile 3개 분리 · dev config가 프로덕션 리소스 참조 금지 · 반대도 금지.
- **secrets 스코프 경계**: 각 환경의 Secrets Manager 스코프 분리 · dev Secrets 값이 prod 리소스 접근 권한 없음 (secrets-management topology 계승).
- **branch → 환경 매핑 경계**: `develop → staging` · `main → prod` 매핑 고정. 다른 브랜치의 프로덕션 배포 금지.
- **branch protection 경계**: `main`·`develop`에 직접 push 금지 · PR review + CI status 통과 필수.
- **데이터 격리 경계**: 환경 간 데이터 이관은 명시적 프로세스 (restore drill 등) 로만. 임시 접근 금지.

### Invariants
- prod 리소스에 dev·staging 프로필로 접근한 사례 0건
  - 감지법: IAM·Secrets Manager 스코프 리뷰 · 로그 확인
- `main`·`develop`에 직접 push (PR 우회) 사례 0건
  - 감지법: GitHub branch protection settings · git log audit
- staging 우회 프로덕션 직접 배포 사례 0건
  - 감지법: GHA workflow trigger 조건 · 배포 이력
- 프로덕션에 새 V파일 착지 시 staging 사전 dry-run 없이 배포된 사례 0건
  - 감지법: staging Flyway 이력 vs prod Flyway 이력 diff
- 3환경 (dev·staging·prod) Spring profile 이외 프로필 사용 사례 0건
  - 감지법: `application-{profile}.yml` 파일 목록 · profile 활성 방식 리뷰
- 한 환경의 CloudWatch Alarm이 다른 환경의 지표를 참조한 사례 0건
  - 감지법: CloudWatch Alarm 정의 리뷰
- 프로덕션 배포 후 rollback 필요 시 이전 git_sha 재배포 실패 사례 0건
  - 감지법: ECR lifecycle policy 확인 (이전 이미지 유지) · rollback 리허설 이력

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 dev H2 URL·staging RDS endpoint·prod RDS endpoint → `application-{profile}.yml` (M7 신설)
- 실제 Spring profile 상세 config → `src/main/resources/application-{profile}.yml`
- 실제 GHA workflow 파일 → `.github/workflows/deploy.yml` (M7 신설)
- 실제 branch protection rules → GitHub Settings → Branches
- 실제 ECS Task Definition·ECR lifecycle policy → AWS 콘솔 (M7 실물)
- 실제 CloudWatch Alarm 임계값 → AWS 콘솔 · `living-docs/ops-health-board/` (M7~M8)
- staging dry-run 상세 절차 → `docs/runbooks/deployment-drydrun.md` (M7 신설 예정)
- release-gate 신호·GO/NO-GO 판정 → 향후 `release-gate` topology (MEDIUM · 재검토 예정)
- 왜 이렇게 박혔는지 → `docs/adr/` (환경 분리 관련 ADR · 미신설)

---

## 4. Re-pin trigger

- 3환경 → 2환경 (staging 폐기 or dev 폐기) 로 축소
- staging → prod 자동 진행 (사용자 승인 게이트 폐기)
- Blue/Green 배포·Canary 배포 도입으로 배포 파이프라인 재편
- 다중 리전 도입 (`ap-northeast-2` 외 확장)
- 브랜치 → 환경 매핑 변경 (`develop → prod` 등)
- Trunk-based development 도입으로 `main` 단일 브랜치 전략 전환
- Kubernetes·다른 오케스트레이션 도입 (ECS 폐기)
- 데이터 이관 자동화 도입 (production data → staging refresh 자동화)
