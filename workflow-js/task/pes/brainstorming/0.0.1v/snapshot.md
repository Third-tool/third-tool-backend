# Snapshot — 0.0.1v

> 본 버전의 brainstorming 후보들이 작성될 때 **트래킹한 문서·코드 상태의 스냅샷**.
> 새 버전(0.0.2v 이상)을 만들 때, 본 파일을 보고 "그 사이에 무엇이 바뀌었나" 비교하여 후보 상태(promoted / resolved / deprecated / merged)를 결정한다.

---

## 기준 시점

| 항목 | 값 |
| --- | --- |
| 작성일 | 2026-06-22 |
| BE 기준 commit | `07301c6` — `docs(adr): ADR010 — AI 제안 호출 실패는 5xx 미노출 [Story-1-3]` |
| BE 브랜치 | `develop` |
| FE 기준 경로 | `C:/study/System_Author/third-tool-fe/untitled` (별도 git 추적) |

---

## 트래킹한 BE 문서 (PES)

### `workflow/product/pes/ready/`
- `product-auth.md` — 인증 인프라 (Epic 1·2·3·4·5, Story 머지 다수)
- `product-aisuggestion.md` — Spring AI 단발 제안 (Port + Static Adapter 머지, Controller 미노출)
- `product-ai-interactive-roadmap.md` — AI 대화형 로드맵 (PES만 — 구현 0%)
- `ADR-CANDIDATES.md` — ADR 후보 목록

### `workflow/product/pes/done/`
- `product-card.md` — Card BC (Epic 1~8 머지)
- `product-learningFacade.md` — LearningFacade BC (Epic 1~6 머지)

### `workflow/product/pes/` (루트)
- `Product.md` — User BC 도메인 정합성 개선 (진행 중)
- `product-infra-deploy.md` / `product-infra-network.md` / `product-infra-ops.md`
- `product-load-test.md` / `product-log.md` / `product-op.md`

### `workflow/product/pes/hot-fix/` · `references/` · `feedbacks/`
- `hot-fix/001.md` — 짧은 메모 (사용자 + AI 로드맵 동반 고민)
- `references/001.md` — PES 저자 컨벤션
- `feedbacks/` — 비어있음

---

## 트래킹한 BE 코드 상태 (요약)

- **Controllers (8개)**: `UserController`, `SocialLoginController`, `JwtController`, `LearningFacadeController` (21 endpoints), `CardController` (17), `TagController` (2), `ReviewController` (6), `UserScheduleController` (4), `DeckController`, `HealthCheckController`
- **AI 제안**: `StaticAxisSuggestionAdapter` + `StaticAxisTopicSuggestionAdapter` 활성 (`@ConditionalOnProperty thirdtool.suggestion.provider=static|미설정`), LLM 어댑터 없음
- **Card 도메인**: `archive() / returnToField() / recordView()` 멱등 + `CardStatusHistory` 이력
- **Review**: `OnFieldBudget` 사용자별 주입(`feat/029`), Layer 1 한정 필터(`feat/041`), 야간 만료 배치(`feat/031`)
- **UserSchedule**: dailyTarget 도입(`feat/037`) + 변경 API(`feat/040`), `X-User-Id` 헤더 사용 (인증 통일 미완)
- **Auth**: JWT AT 쿠키 / RT 바디, ErrorCode 세분화(`feat/017`), JwtAuthenticationEntryPoint(`feat/016`), BlockList 분리(`feat/018`), AccessDenied → AUTH_FORBIDDEN(`feat/025`)

---

## 트래킹한 FE 코드 상태 (요약)

- React 18 + Vite + TanStack Query 5 + React Router 6 + Tailwind 4 + MSW + Zod
- 라우트 (15개): `/`(Landing), `/login`, `/signup`, `/onboarding`(6-step), `/home`(Main Page), `/study`(Cornell), `/cards/new`, `/map`(3-lens), `/archive`, `/archive/:cardId`, `/tags`, `/tags/:tagId`, `/me`, `/maintenance`, `*`(404)
- 핵심 컴포넌트 (`src/components/`): AppShell, Button, Card, Dialog, EmptyState, ErrorScreen, Sidebar, Skeleton, TagChip, Toast, MarkdownView 등 20+
- 디자인 시스템 트랙: amber / clay / sage / cream / canvas / paper / edge 색조 체계 (Tailwind tokens)
- BootstrapGate: 4-tier 로딩 구현됐으나 router 미연결
- mock 데이터: `/home`의 "되어가는 나" 40%, "이번 주 흐름" 차트, "12일째 연속", MePage 멤버십·결제

---

## 트래킹한 living-docs (정합 대상)

- `docs/DOMAIN.md`, `docs/PACKAGE.md`, `docs/adr/` (ADR001~ADR010)
- `docs/ux/wip-language.md` (Story 8-1 정착)
- `workflow/living-docs/fe-user-senario/` (7 시나리오 갱신 완료)
- `workflow/living-docs/fe-boundary-trace/` (API 표 — 별도 트래킹)
- `workflow/living-docs/architecture-system-design/`, `boundary-trace/`, `decisions/`, `infra-outcome/`, `ops-health-board/`, `performance-outcome/`, `test/`, `trouble-shooting-outcome/`

---

## 카탈로그 후보 인덱스 (0.0.1v 시점)

본 버전이 처음 적은 후보들. 상태는 모두 `pending`이다.

| 카탈로그 | 후보 수 | 후보 제목 |
| --- | --- | --- |
| [dev.md](./dev.md) | 8 | 1) 테스트 커버리지 게이트 / 2) API 컨트랙트 자동 동기화 / 3) 로컬 개발 환경 표준화 / 4) 테스트 픽스처 패턴 / 5) FE Storybook · 디자인 시스템 갤러리 / 6) ArchUnit / 7) PR 본문 lint / 8) 릴리스 노트 자동화 |
| [data.md](./data.md) | 7 | 1) 백업/복원 RTO·RPO / 2) GDPR 내보내기 · 영구 삭제 / 3) Soft-delete 영구 삭제 배치 / 4) 분석 웨어하우스 / 5) 데이터 품질 모니터링 / 6) Audit log 확장 / 7) 실시간 vs 배치 분기 정책 |
| [ops.md](./ops.md) | 8 | 1) SLO/SLI · 대시보드 / 2) 인시던트 runbook / 3) 헬스체크 깊이 / 4) AWS 비용 모니터링 / 5) 용량 산정 모델 / 6) 보안 모니터링 / 7) Chaos / 8) actuator v1 활성 |
| [deploy.md](./deploy.md) | 7 | 1) Blue-Green / Canary / 2) Feature Flag / 3) DB 마이그레이션 + 배포 동시 안전성 / 4) 롤백 전략 / 5) 릴리스 노트 / 6) 환경별 설정 · 비밀 / 7) CI 빌드 시간 최적화 |
| [ai.md](./ai.md) | 9 | 1) LLM 어댑터 추상화 / 2) 프롬프트 버전 관리 / 3) 출력 품질 평가 / 4) 환각 가드 / 5) AI 비용 + 사용자별 한도 / 6) 사용자 피드백 루프 / 7) 캐싱 / 8) 세션 저장소 / 9) 컨텍스트 윈도우 |

**총 후보 39개**.

---

## 이전 버전 (없음)

본 버전은 brainstorming 영역의 **최초 스냅샷**이다. 0.0.2v 부터 본 섹션에 "0.0.1v 대비 변경" 표가 들어간다 (어떤 후보가 promoted / resolved / deprecated / merged 되었는지).
