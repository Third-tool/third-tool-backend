# Brainstorming · 개발 (Dev) 횡단 카탈로그

> 개발 과정 — 코드 작성·테스트·로컬 환경·디자인 시스템·아키텍처 검증·PR 흐름 — 에서 추가하면 좋을 횡단 관심사를 모은다.

---

## [후보 1] 테스트 커버리지 자동 게이트

> PR 단위로 커버리지 임계치를 강제해서 도메인 변경이 테스트 없이 머지되는 일을 막는다.

### 배경
- `.claude/rules/conventions.md` §4에 테스트 컨벤션은 명세되어 있으나 **CI에서 임계치를 자동 검증하지 않음**.
- 도메인 행위 추가 시 "테스트 추가 트리거" 4.8 규칙이 사람 판단에 의존.
- 머지된 PR 중 도메인 클래스 추가/수정과 테스트 누락이 동시에 잡힌 적이 있는지 git log로 후속 측정 필요.

### 후보
- **A안**: JaCoCo + Gradle plugin + GitHub Actions에서 PR diff에 한해 임계치(예: 변경 라인 80%) 강제. 장점: 변경된 코드에만 압박. 비용: diff 기반 측정 도구 도입 (예: Codecov).
- **B안**: 전체 프로젝트 라인 커버리지 N% 임계. 장점: 단순. 비용: 레거시 부분 때문에 임계 낮춰 무력화 가능.
- **C안**: PR 본문 체크리스트 "단위 테스트 추가 □"만 두고 자동 게이트 X. 장점: 도입 비용 0. 비용: 누락 발견 늦음 — 현재 상태.

### 1차 권장
A안. 변경 라인에 집중하면 레거시 부담 없이 신규 코드부터 견고화. Codecov 무료 티어로 시작 가능.

### PES 승격 경로
- 신규 Product 후보: `product-quality-gate.md` (테스트·정적 분석·아키텍처 테스트 묶음)
- 또는 `product-infra-ops.md`에 CI 품질 게이트 Epic 추가

### 미해결 질문
- 현재 BUILD SUCCESSFUL 외에 어떤 CI 신호가 머지 차단에 쓰이고 있나? 사용자 확인 필요.

---

## [후보 2] API 컨트랙트 자동 동기화 (BE ↔ FE)

> FE-BE DTO drift를 컴파일 타임에 잡는다.

### 배경
- BE는 Spring `@RestController` + record DTO, FE는 Zod 스키마 + `lib/api/endpoints/*` 수기 작성.
- 본 작업 진행 중 `fe-user-senario` 갱신 시 `/api/` vs `/api/v1/` 같은 prefix 불일치 / `POST /api/cards` vs `POST /api/v1/decks/{deckId}/cards` 같은 시그니처 drift 다수 발견.
- BE가 Controller를 바꿔도 FE 코드는 침묵 — 런타임에서야 깨짐.

### 후보
- **A안**: BE에 springdoc OpenAPI 활성 → `openapi.yaml` 산출 → FE `openapi-typescript`로 타입 생성 → `lib/api/endpoints/*`가 그 타입 import. 장점: BE 변경 시 FE 타입 빌드 깨짐으로 즉시 감지. 비용: openapi.yaml 정확도 유지 (어노테이션 부담).
- **B안**: Pact 컨트랙트 테스트. 장점: BE/FE 양쪽 테스트로 양방향 검증. 비용: 도구 학습 + 컨트랙트 broker 인프라.
- **C안**: 수기 sync + Reviewer 세션에 "FE 영향" 항목 추가. 장점: 도구 추가 0. 비용: 사람 누락에 의존.

### 1차 권장
A안. springdoc는 이미 Spring 생태계 표준이고 자동 산출이라 BE 부담이 가장 작음. FE는 타입만 사용하므로 MSW mock과 자연스럽게 결합.

### PES 승격 경로
- 신규 Product: `product-api-contract.md` (BE OpenAPI + FE 타입 생성 + msw mock 동기화)
- 또는 `product-infra-deploy.md`에 CI 산출물 단계 추가

---

## [후보 3] 로컬 개발 환경 표준화 (Docker Compose dev stack)

> 신규 입사자 onboard 시간 단축 + dev/prod 환경 차이 최소화.

### 배경
- 현재 dev 프로필은 H2(인메모리). 프로덕션은 MySQL — Flyway 마이그레이션 / 트랜잭션 격리 / 제약 동작이 미세하게 다름.
- 사용자 1인 운영이라 큰 문제는 없지만, 통합 테스트 신뢰도가 H2 → MySQL 차이로 떨어질 수 있음.

### 후보
- **A안**: Docker Compose로 MySQL + LocalStack(S3·SQS) + 메일 더미(Mailpit) 일괄 기동. 장점: 본 PC가 prod에 가깝다. 비용: Docker Desktop 자원 사용 + 초기 컴포즈 파일 작성.
- **B안**: Testcontainers를 통합 테스트에만 도입. 장점: 평소 개발은 H2 그대로 가벼움. 비용: H2 vs MySQL 차이는 통합 테스트에서만 잡힘.
- **C안**: 현 상태 유지. 비용: 마이그레이션 회귀 위험 잠재.

### 1차 권장
B안 → A안 점진. 통합 테스트 먼저 Testcontainers로 격상해서 즉각적 가치 확보, 개발 stack은 필요해질 때 Compose로 확장.

### PES 승격 경로
- `product-infra-deploy.md`에 "로컬 개발 인프라" Epic 추가
- ADR 후보: H2 vs Testcontainers vs Compose dev — 어디까지 prod에 맞출 것인가

---

## [후보 4] 테스트 픽스처 / 시드 데이터 패턴

> 테스트마다 `Card.create(...)` 풀 인자 호출이 반복되는 비용을 줄인다.

### 배경
- conventions.md §4.6에 "픽스처 빌더(`CardFixture`)를 BC별 테스트 패키지에" 명시됨.
- 실제 적용 정도는 BC마다 다를 수 있음 (확인 필요).
- 신규 BC 추가 시 픽스처도 같이 만드는지의 일관성이 PR 리뷰 시 매번 체크되어야 함.

### 후보
- **A안**: 모든 BC에 `{BC}Fixture` 클래스 강제 + ArchUnit으로 검증. 장점: 일관성 보장. 비용: 작은 BC에는 과함.
- **B안**: 자주 쓰는 도메인(Card·LearningFacade)만 픽스처 우선. 장점: 비용 작음. 비용: 누락 BC가 생김.
- **C안**: Instancio / EasyRandom 같은 자동 빌더 도입. 장점: 코드량 작음. 비용: 도메인 불변식 위반 가능, 의도 불분명한 fixture.

### 1차 권장
B안. 큰 BC만 우선 정착시키고 나머지는 필요할 때 추가.

### PES 승격 경로
- 후보 1 (테스트 게이트) Product에 흡수 가능
- 또는 단독 ADR: "도메인 픽스처 패턴 표준"

---

## [후보 5] FE Storybook / 디자인 시스템 갤러리

> `src/components/`의 재사용 컴포넌트(AppShell·Button·Card·Dialog·EmptyState·Toast 등)를 갤러리화하여 시각·접근성 회귀를 잡는다.

### 배경
- FE 코드(`third-tool-fe/untitled`)에 디자인 시스템 단위 컴포넌트가 20+ 개 존재.
- 화면(Page)에서 한 번씩 사용되고 끝나는 구조 — 다른 화면에서 같은 컴포넌트의 다른 상태(loading/error/disabled)가 어떻게 보이는지 한눈에 확인 불가.
- Claude Design으로 새 화면을 짜올 때 기존 컴포넌트 재사용 vs 신규 작성 판단 어려움.

### 후보
- **A안**: Storybook 7 도입 + 각 컴포넌트의 `*.stories.tsx` 작성. 장점: 표준. 비용: 초기 스토리 작성 비용 + 빌드 의존성 증가.
- **B안**: 단순 라우트 `/__styleguide`에 컴포넌트 일괄 렌더링 페이지. 장점: 의존성 0. 비용: 상태 분기·인터랙션 표시가 빈약.
- **C안**: 현재 그대로. 비용: 컴포넌트 재사용 판단을 매번 코드 검사로 해야 함.

### 1차 권장
B안 먼저 시도 후 필요 시 Storybook 격상. 1인 개발 규모에서는 단순 갤러리 페이지가 가성비 좋음.

### PES 승격 경로
- 신규 Product: `product-fe-design-system.md` (FE 측 단독)
- BE PES에는 영향 없음 — FE 작업 단독 trackable

---

## [후보 6] Architecture Test (ArchUnit)

> `docs/PACKAGE.md`의 의존 규칙을 코드로 강제한다.

### 배경
- PACKAGE.md는 BC 간 의존 방향 / Aggregate↔Repository / Common→BC 금지 등을 서술적으로 명시.
- Reviewer 세션의 "Architecture Reviewer" 관점이 이를 매번 점검 — 사람 손에 의존.
- ArchUnit으로 self-enforcing 가능.

### 후보
- **A안**: ArchUnit으로 BC 의존 / 레이어 의존 / 패키지 위치 룰 작성. 장점: 매 빌드마다 자동 검증. 비용: 룰 표현 학습 + 초기 룰 정의 시간.
- **B안**: Reviewer 세션 강화로 충당. 장점: 도입 비용 0. 비용: 사람 누락.
- **C안**: SonarQube 의존 규칙. 비용: SonarQube 인프라.

### 1차 권장
A안. ArchUnit은 외부 인프라 없이 JUnit 테스트로 동작. PACKAGE.md를 코드화하는 가장 가벼운 길.

### PES 승격 경로
- 후보 1 (테스트 게이트) Product에 흡수 또는 ADR 후보 "PACKAGE.md ArchUnit 정착"

---

## [후보 7] PR 본문 템플릿 lint

> PR 본문이 CLAUDE.md 템플릿을 따르는지 자동 검증 — 체크리스트 미체크 시 머지 차단.

### 배경
- CLAUDE.md "PR 작성 규칙" 섹션에 본문 템플릿 명시됨.
- 머지 정책은 "사람이 직접" — 본문 체크리스트 누락 채로 머지될 수 있음.
- 1인 운영 환경이라 자기 자신과의 약속을 강제하는 도구가 가치 있음.

### 후보
- **A안**: GitHub Actions에서 PR 본문 정규식 검증 (각 체크박스 패턴 존재 여부). 장점: 가벼움. 비용: 자유 형식 PR 어려움.
- **B안**: `pull_request_template.md` 두기. 장점: 도입 비용 0. 비용: 체크 안 해도 머지 가능.
- **C안**: Danger.js 같은 PR 정책 봇. 장점: 풍부한 룰. 비용: 도구 학습.

### 1차 권장
B안 → A안 점진. `.github/PULL_REQUEST_TEMPLATE.md` 먼저, 누락이 반복되면 Actions 검증 추가.

### PES 승격 경로
- 단순 PR이라 Product화 불필요. `.claude/rules/pr-commit.md` 보강 + 템플릿 파일 추가 한 번으로 종료 가능.

---

## [후보 8] Conventional Commits + 릴리스 노트 자동 생성

> 머지 시점에 누적된 commit으로 CHANGELOG / 릴리스 노트가 자동 생성.

### 배경
- 현재 commit 메시지는 `feat(facade): ...` Conventional Commits 형식 준수 중 (`.claude/rules/git.md`).
- 릴리스 노트가 없음 — 누가 언제 어떤 기능이 추가됐는지 추적은 git log + PR 본문에만 의존.
- 포트폴리오 관점에서 CHANGELOG.md가 있으면 어필 포인트.

### 후보
- **A안**: `release-please` GitHub Action. PR이 머지될 때 다음 릴리스 PR을 자동 갱신. 장점: GitHub 네이티브. 비용: 학습.
- **B안**: `git-cliff` CLI 도구. 로컬에서 수동 실행. 장점: 가벼움. 비용: 자동화 약함.
- **C안**: PR 본문 모음 → 수기 CHANGELOG. 비용: 사람 노동.

### 1차 권장
A안. 1인 운영이라 수기 작업 최소화가 중요. release-please가 후보 6번(ArchUnit), 1번(테스트 게이트)과 동일 CI 위에서 동작 가능.

### PES 승격 경로
- 후보 2 (API 컨트랙트) Product 또는 신규 `product-release-flow.md`에 흡수
