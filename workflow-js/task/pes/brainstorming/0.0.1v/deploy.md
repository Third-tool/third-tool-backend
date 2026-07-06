# Brainstorming · 배포 (Deploy) 횡단 카탈로그

> 배포 — 무중단 전략 · feature flag · DB 마이그레이션 안전성 · 롤백 · 릴리스 노트 · 환경 분리 · CI 빌드 시간 — 횡단 관심사를 모은다.

---

## [후보 1] Blue-Green 또는 Canary 배포

> 사용자 영향 0인 무중단 배포 + 일부 사용자에게 먼저 노출.

### 배경
- 현재 배포 전략은 `product-infra-deploy.md` 명세 기준으로 진행 중 (구체 전략 별도 확인 필요).
- 1인 운영이라 다운타임이 발생해도 즉각 인지·복구 절차가 매뉴얼.
- 사용자 N명 증가 시점에 무중단 배포가 필수가 됨.

### 후보
- **A안**: Blue-Green — 두 환경 운영 + ALB target group 전환. 장점: 단순·빠른 롤백. 비용: 인프라 비용 2배.
- **B안**: Canary — 5% / 50% / 100% 점진. 장점: 위험 작음. 비용: 트래픽 분리 인프라 + 관측 시스템 필요.
- **C안**: Rolling (기존). 장점: 비용 최소. 비용: 일시적 가용성 저하.

### 1차 권장
C안 유지 → B안 점진. 사용자 N명 임계까지는 rolling 충분. Canary는 후보 4 (롤백 전략) ADR과 함께 도입.

### PES 승격 경로
- `product-infra-deploy.md`에 "무중단 배포 전략" Epic 추가
- ADR 후보: 배포 전략 선택

### 미해결 질문
- 현재 실측 다운타임은 몇 초? 사용자 N명 임계는?

---

## [후보 2] Feature Flag (GrowthBook 등)

> 코드 머지 ≠ 사용자 노출. 기능별 점진 노출 + 즉시 끄기 가능.

### 배경
- CLAUDE.md의 ultrareview 컨텍스트에 GrowthBook 언급 — 검토된 흔적은 있으나 도입 명세는 없음.
- AI 제안 v1.5 / AI Roadmap v2처럼 **백엔드 준비됐는데 FE에서 켜고 끄고 싶은 기능**이 누적되고 있음.
- 환경별 설정(application.yml)으로 제어 중인 항목(`thirdtool.suggestion.provider`)을 사용자 단위로 분기하기는 어려움.

### 후보
- **A안**: GrowthBook self-hosted. 장점: 무료·오픈소스. 비용: 인프라 1대 추가.
- **B안**: GrowthBook Cloud / LaunchDarkly. 장점: 운영 부담 0. 비용: 사용자당 월 요금.
- **C안**: 자체 `feature_flag` 테이블 + Spring `@ConditionalOnProperty` 패턴 확장. 장점: 의존성 0. 비용: 사용자별 분기 UI·SDK 자체 작성.

### 1차 권장
C안으로 시작 → 기능이 5개 이상 누적되면 A안 격상. 1인 사이드 프로젝트 규모에서 외부 도구 비용 vs 가치 비교.

### PES 승격 경로
- 신규 Product: `product-feature-flag.md`
- ADR 후보: feature flag 정책·도구 선택

---

## [후보 3] DB 마이그레이션 + 배포 동시 안전성

> 마이그레이션이 진행 중인데 기존 인스턴스가 옛 스키마로 동작하는 문제.

### 배경
- conventions.md §3.8에 마이그레이션 작성 규칙 명시: 컬럼 추가 + NOT NULL 전환은 3단계 분리 (ADD NULL → 백필 → MODIFY NOT NULL).
- 그러나 **배포 흐름과의 결합 규칙은 미명시** — 마이그레이션 1단계 후 신 코드 배포, 그 사이 트래픽이 어디로 가는가?
- 후보 1 (Blue-Green) 도입과 결합해서 정밀화 필요.

### 후보
- **A안**: 모든 마이그레이션은 forward-only + 2단계 배포 (스키마 호환 → 코드 → 정리). 장점: 안전. 비용: 마이그레이션 1건당 배포 2회.
- **B안**: 마이그레이션 즉시 강행 + 짧은 다운타임 수용. 장점: 단순. 비용: 사용자 영향.
- **C안**: 현재 상태 (Flyway 자동 + 운에 맡김). 비용: 사고 시 원인 복잡.

### 1차 권장
A안. 후보 1 (Blue-Green)과 함께 ADR화 — "마이그레이션은 항상 N-1 호환 + 2단계 배포".

### PES 승격 경로
- `product-infra-deploy.md`에 "DB 마이그레이션 안전 배포" Epic
- ADR 후보 (강력 권장): 본 정책

---

## [후보 4] 롤백 전략 (코드 ≠ DB)

> 코드를 되돌릴 순 있어도 DB를 되돌릴 순 없을 때 어떻게 하나.

### 배경
- 현재 코드 롤백은 git revert + 재배포로 가능.
- DB 롤백은 forward-only Flyway에서 명시적 절차 없음 — 후보 3과 결합.
- 운영 사고 시 즉시 안전 상태로 돌릴 수 없는 위험.

### 후보
- **A안**: 항상 forward-fix. 롤백 대신 "수정 commit 재배포". 장점: DB 복잡도 0. 비용: 사고 시 분석 + 수정 + 배포 시간.
- **B안**: Repeatable Flyway (`R__`) 활용 + 양방향 마이그레이션. 장점: 진정한 롤백 가능. 비용: 작성 부담·테스트 부담.
- **C안**: Blue-Green 환경에서 이전 환경 유지. 장점: 즉시 트래픽 전환. 비용: 스키마 호환 강제 (후보 3과 결합).

### 1차 권장
A안 + C안. 코드는 forward-fix, 인프라는 Blue-Green으로 빠른 트래픽 복귀. B안은 운영 부담 큼.

### PES 승격 경로
- ADR 후보: "롤백 정책 — forward-only + Blue-Green 빠른 전환"
- 후보 1·3과 묶음 진행

---

## [후보 5] 릴리스 노트 자동 생성 + 변경 이력

> 사용자에게 "이번 주 변경사항" 공지가 자동.

### 배경
- Conventional Commits 규칙은 정착됨 (`.claude/rules/git.md`).
- 그러나 CHANGELOG.md / Release notes는 없음.
- 포트폴리오 어필 측면에서 "이번 달 변경 내역" 같은 가시화가 가치 있음.

### 후보
- **A안**: `release-please` GitHub Action (dev.md 후보 8과 동일). 장점: GitHub 네이티브. 비용: 학습.
- **B안**: `git-cliff` 로컬 CLI. 장점: 가벼움. 비용: 자동화 약함.
- **C안**: 수기 CHANGELOG. 비용: 사람 노동.

### 1차 권장
A안. dev.md 후보 8과 통합 진행.

### PES 승격 경로
- dev.md 후보 8과 같은 Product 또는 신규 `product-release-flow.md`

---

## [후보 6] 환경별 설정 / 비밀 관리

> dev/staging/prod 설정 분리 + 비밀 안전 보관.

### 배경
- 현재 `application.yml` 단일 파일에서 프로파일별 설정.
- 비밀(DB 비밀번호 / JWT secret / 소셜 OAuth client secret)이 어떻게 주입되는지 명시 필요.
- 1인 운영이라 실수로 비밀이 git에 커밋되면 사고.

### 후보
- **A안**: AWS Secrets Manager + Spring Cloud AWS. 장점: 표준. 비용: 인프라 비용 작음 + 학습.
- **B안**: AWS Parameter Store (SSM). 장점: 더 싸다. 비용: Secrets Manager보다 기능 적음.
- **C안**: GitHub Actions Secrets + 환경변수 주입. 장점: 인프라 0. 비용: 회전 매뉴얼.

### 1차 권장
B안. 단가 낮고 Spring Cloud AWS와 호환. 후보 1 (Blue-Green) 진입 전 정착.

### PES 승격 경로
- `product-infra-deploy.md`에 "비밀 관리" Epic 추가
- ADR 후보: Secrets Manager vs Parameter Store

---

## [후보 7] CI 빌드 시간 최적화

> PR 머지 대기 시간 단축.

### 배경
- Gradle 빌드 시간이 길어지면 1인 운영에서 컨텍스트 스위칭 비용 증가.
- 현재 CI 빌드 시간 미측정.
- QueryDSL Q클래스 생성 / Flyway / 테스트 병렬화 모두 최적화 여지.

### 후보
- **A안**: Gradle remote cache + GitHub Actions cache + 테스트 병렬화. 장점: 표준. 비용: 캐시 무효화 디버깅 가끔 어려움.
- **B안**: 통합 테스트와 단위 테스트 분리 — 단위 빠르게 + 통합 nightly. 장점: PR 빠름. 비용: PR 단위에서 통합 회귀 못 잡음.
- **C안**: 현재 상태. 비용: 시간 누적.

### 1차 권장
A안. B안은 PR 단위 안전성을 깎아 risky.

### PES 승격 경로
- dev.md 후보 1 (테스트 게이트) + 본 후보 묶어서 신규 `product-ci-quality.md` 가능
