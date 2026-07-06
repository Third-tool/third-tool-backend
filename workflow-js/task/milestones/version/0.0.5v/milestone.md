# M5 / 0.0.5v — Week of 2026-07-22 ~ 2026-07-28 (D1 = 2026-07-22 Wed)

> **마일스톤의 역할**: M4가 07-21에 동결됐다 (Card BC 대재편 완주 · Mode enum 재정의 + OnFieldBudget 폐기 + createdMode 하이브리드 + Card→Axis 직접 매핑 + AS Static Adapter role 4종 catalog). 본 M5는 Card BC 안정화에 이어 **Deck BC 완전 폐기 (LT E5) + Layer 진행률·이중 스코프 SDD 착지 (LT E6) + Review BC 신설 3 Epic 완주 (E1 DailyLearningBatch · E2 ReviewSession cross-layer · E3 캐시 대시보드)** 를 목표로 한다.
>
> 한 주 = 한 버전 = `version/0.0.X v/` 폴더 하나. 본 버전(0.0.5v)에는 다음 7 파일이 들어간다:
> - `milestone.md` *(본 문서)* — 잡힌 양 + 일정 + 의존 + Epic PR 매트릭스
> - `infra.md` — (본 버전 배포 미포함 사유 명시)
> - `performance.md` — (본 버전 성능 측정 미수행 사유 명시)
> - `outcome.md` — 사용자·기능 성과 (Deck 폐기 + Review BC 완주 + Dashboard 첫 도착)
> - `cost.md` — 비용 성과 (본 버전 로컬 개발 한정 → 비용 발생 0)
> - `review.md` — 회고 + 다음 버전 진입 신호
> - `eval.md` — AI 응답 품질 평가 프레임 확장 (M4 role 4종 완주 반영 · AS Epic 3 안정화 관찰 흡수)

**릴리스 대응**: 본 M5는 첫 사용자 릴리스(0.1.0v, ~2026-08-19) 6주 로드맵의 **네 번째** 마일스톤. 자세한 스코프는 `workflow/task/milestones/release/version/0.0.1v/release.md` §Product별 릴리스 스코프 순회 참조.

**FE 대응 마일스톤**: `workflow/task/pes/fe/fe-milestones/version/0.0.5v/milestone.md` — 동일 주간, Deck→Axis 엔드포인트 이관 UI + Review BC 신설 화면(DailyBatch 큐 · 진행률 대시보드) 착지.

---

## 0.0.5v 스코프 결정 (M4 이후, 2026-07-22)

**M4 착지 결과 요약**:
- ✅ Card BC 대재편 완주 (Epic 1·2·3) — `LearningMode` 4개 값 · `OnFieldBudget` 폐기 · `ArchiveReason` 3개 값 재정의 · `createdMode` 하이브리드 (M3 down/up 시나리오)
- ✅ Card→Axis 직접 매핑 (LT E4) — `card.axis_id NOT NULL` 승격 · Coverage 재계산 축 스코프 이관 · `CardViewedEvent` axisId 발행
- ✅ AS Static Adapter role catalog 4종 완주 (E3 Story 3-3~3-5) — `planner.json` · `designer.json` · `problem-solver.json` + M3 `backend-developer.json` = 4 role
- ✅ Flyway V23~V30 정합 (Mode 재편 + Budget 폐기 + createdMode 3-phase + Card axis_id 3-phase)
- ✅ Reviewer 5관점 세션 5회 진행 (M3 재개 이후 첫 표준 스케일 세션)

**M5 축 결정 — Deck 폐기 + Layer 진행률·이중 스코프 + Review BC 신설 3 Epic 완주**:

M4 review.md §다음 마일스톤 결정 예고에서 확정된 순위 반영. 사용자 지시 "Epic 단위 PR 진행" · "Review E3 dashboard까지 M5에 포함" 반영. Reviewer 5관점 세션 유지.

이 지시를 반영해 본 M5는 다음 3축으로 구성:

1. **Deck BC 완전 폐기 완주** — LT Epic 5 Story 5-1~5-5 + LT Epic 3 잔여 S3-5 (AxisTopic 폐기 문서화) 병합. Deck 8 책임 필드 Axis 이전 + `/decks/*` 엔드포인트 폐기 + `deck` 테이블 soft-deprecate. **부차 (~18%)**.
2. **LT E6 이중 스코프 SDD 착지** — Story 6-1~6-5 (ReviewSession scope enum · Repository 이중 조회 · Layer.progressStatus 파생 · FE 용어). S6-1·2·3은 후속 issue-25에 의해 PR#4가 supersede하므로 궤적을 명시적 관리. **경량 (~13%)**.
3. **Review BC 신설 3 Epic 완주** — Review Epic 1 (DailyLearningBatch Aggregate + DailyCardEntry) + Epic 2 (ReviewSession cross-layer 재편 · scope 개념 폐기) + Epic 3 (캐시 대시보드 · 규칙 기반 추천 · 주간 요약 cron). **주력 (~69%)**.

**Epic 단위 PR 5건 예정 (본주 목표)**:

| Epic PR | 대응 | 예상 SP | 종료 목표 |
|---|---|---|---|
| PR#1 (LT-E5-DECK-ABOLISH) | LT Epic 5 Story 5-1~5-5 + LT Epic 3 Story 3-5 병합 (Deck 폐기 + AxisTopic 폐기 문서화) | 7.5 | D3 |
| PR#2 (LT-E6-DUAL-SCOPE) | LT Epic 6 Story 6-1~6-5 (ReviewScope enum + Layer.progressStatus + FE 명명) | 5.5 | D4 |
| PR#3 (REV-E1-DAILY-BATCH) | Review Epic 1 Story 1-1~1-8 (DailyLearningBatch Aggregate + 자정 close cron) | 10.5 | D5 |
| PR#4 (REV-E2-SESSION-REORG) | Review Epic 2 Story 2-1~2-7 (ReviewSession cross-layer 재편 · scope 폐기) | 7.5 | D6 |
| PR#5 (REV-E3-DASHBOARD) | Review Epic 3 Story 3-1~3-8 (캐시 대시보드 + 규칙 추천 + 주간 요약 cron) | 11.5 | D7 |

**Total: 5 Epic PR · 총 42.5 SP** (M4 실 착지 ~30 SP 대비 ~42% 상승. **도전적 목표**. Review BC 신설 3 Epic 완주가 M5의 중심축이고 domain 심층도 대. Reviewer 5관점 세션 5회 필수 · M4 표준 스케일 유지).

**본 버전 제외 사유**:
- **LT E6 S6-1·2·3 궤적** — 포함 (전체 M5 결정). issue-25가 supersede하는 실제 코드 궤적을 그대로 재현 (issue-14 원안 착지 → issue-25가 재편 · 리스크로 관리)
- **AS Epic 3 완주 확인** — Story 신규 착수 없음. `eval.md` §M5 baseline로 흡수. milestone PR 없음
- **LLM Adapter (Vertex AI)** — M6 이관 유지 (Spring AI + Vertex AI Gemini Flash 2.5)
- **AS Epic 5 SuggestionCascade + Rate Limit** — M6 이관 유지
- **AS Epic 6 관측성** — M7 이관 유지 (`thirdtool.suggestion.*` 메트릭 배선)
- **AI Interactive Roadmap 진입** — M6~M7 이관 유지
- **배포 라인 · 성능 baseline** — M7 이관 유지
- **Roadmap 노드 재생성 API (이슈 #18)** — v2 이관 유지
- **AI 비용 예산 cap (이슈 #20)** — v1 관찰 지표만. Cap 자체 v2 이관 유지
- **Card BC 재편 잔재** — M4에 완주 상정 (미완 시 별도 M5.1 패치 발행)

---

## 진행 중 Product 잔여 인벤토리 (Before/After — M5 진입 시 vs 종료 후 예상)

**해결율 계산**: `M5 대상 / M5 진입 시 잔여 × 100%` — Product별 M5가 얼마나 소진하는지 즉시 파악.

| Product | 총 Story | M5 진입 시 완료 | M5 진입 시 잔여 | M5 대상 | **M5 종료 후 예상 잔여** | **해결율** |
| --- | --- | --- | --- | --- | --- | --- |
| 1. 인증 (`product-auth.md`) | 10 | 10 | 0 | — | 0 | ✅ 완주 (M1) |
| 2. User BC (`Product.md`) | 8 | 7 | 1 | — | 1 | 0% (M6~) |
| 3. **Learning Tower** (`product-learning-tower.md`, 6개 Epic + E3 재정의판) | **42** | 21 (E1·E2·E3-재정의판·E4) | 21 | **11 Story (E5 5 + E6 5 + E3S3-5 1)** | **10** | **52%** ↑ |
| 4. **AI Suggestion** (`product-ai-suggestion.md`, 7 Epic 재편) | **34** | 14 (E1·E2·E3 완주·E5-부분·E7) | 20 | — (관찰만) | 20 | 0% (M6~) |
| 5. AI Interactive Roadmap (`product-ai-interactive-roadmap.md`) | ~15 | 0 | ~15 | — | ~15 | 0% (M6~M7) |
| 6. Card (`product-card.md`, 3 Epic 재편) | 16 | 16 | 0 | — | 0 | ✅ 완주 (M4) |
| 7. **Review** (`product-review.md`, 3 Epic) | **23** | 0 | 23 | **23 Story (E1 8 + E2 7 + E3 8)** | **0** | **100%** ✅ 완주 |
| 8. 컨테이너 배포 (`product-infra-deploy.md`) | 9 | 5 | 4 | — | 4 | 0% (M7) |
| 9. AWS 네트워크 (`product-infra-network.md`) | 8 | 3 | 5 | — | 5 | 0% (M7) |
| 10. Secrets·백업·관측 (`product-infra-ops.md`) | 10 | 2 | 8 | — | 8 | 0% (M7) |
| 11. 로깅 (`product-log.md`) | 9 | 3 | 6 | — | 6 | 0% (M7) |
| 12. 메트릭 (`product-op.md`) | 7 | 3 | 4 | — | 4 | 0% (M7) |
| 13. 검색 (`product-search.md`) | ~18 | 0 | ~18 | — | ~18 | 0% (v2) |
| 14. 미디어 (`product-media.md`) | 보류 | 0 | — | — | — | — |
| 15. 캐시 (`product-cache.md`) | — | 0 | — | — | — | — |
| 16. 알림 (`product-notification.md`) | — | 0 | — | — | — | — |
| 17. Admin (`product-admin.md`) | — | 0 | — | — | — | — |
| 18. FE CDN (`product-fe-cdn.md`) | — | 0 | — | — | — | — |
| 19. 부하 테스트 (`product-load-test.md`) | 6 | 0 | 6 | — | 6 | 0% (v1 이후) |
| **합계** | **~206** | **~84** | **~122** | **34 Story · 5 Epic PR · 42.5 SP** | **~88** | **28%** ↑ |

### 📊 M5 예상 성과 카드

- **총 해결 대상**: 34 Story (전체 잔여 ~122의 **28% 소진**)
- **완주 예상 SDD Product**:
  - `product-review.md` Epic 1·2·3 — **완주 100%** (Review BC 3 Epic 종결 · 신설 후 최초 릴리스 준비 완료)
  - `product-learning-tower.md` Epic 5 (Deck 폐기) — 완주
  - `product-learning-tower.md` Epic 3 문서화 잔여 (S3-5 AxisTopic 폐기) — 완주
- **부분 진행 SDD Product**:
  - `product-learning-tower.md` Epic 6 Story 6-1~6-5 (이중 스코프) — 착지하되 S6-1·2·3은 PR#4가 supersede (실제 코드 궤적 반영)
- **M5 종료 후 남는 것** (다음 마일스톤 트라젝토리):
  - Learning Tower **10 Story 잔여** → Epic 3 원안 Story 3-1~3-4 SUPERSEDED 잔재 4 + 기타 backlog · M6~M7 정리 시점 결정
  - AI Suggestion **20 Story 잔여** → M6 Epic 4 (LLM Adapter · 5 Story) + M6~M7 Epic 5 (Cascade · Rate Limit · Controller 확장 · 3 Story) + M7 Epic 6 (관측성 · 4 Story) + Epic 1~2 신설 Story SUPERSEDED 잔재
  - Card — 안정화 관찰만 (미완 발견 시 별도 패치)
  - AI Interactive Roadmap — M6~M7 진입 대기
  - 배포·관측·로깅·부하 — M7 배포 재개 대기
  - v2 이관: 검색 · 미디어 · 캐시 · 알림 · Admin

---

## Epic PR 매트릭스 (본주 잡힌 양)

Epic 단위 PR 진행 원칙 (M4 유지):
- **한 Epic PR = 한 논리 단위 = 한 base 브랜치**. Squash merge 또는 rebase merge.
- Epic PR 안의 Story 커밋은 순차 누적 (`feat(scope): ...`).
- Reviewer 세션은 Epic PR 단위 (PR 하나에 5관점 병렬 발사).

### Epic PR #1 — `LT-E5-DECK-ABOLISH` (Deck BC 완전 폐기 + Axis 흡수 · S3-5 병합)

**Base 브랜치**: `feat/037-lt-e5-deck-abolish`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-learning-tower.md`
- Epic: `# [Epic 5] Deck BC 폐기 및 Axis 흡수` (line 1867~2137)
- Story 범위: `## [Story 5-1]` (line 1917) ~ `## [Story 5-5]` (line 2101)
- 병합 잔재: `## [Story 3-5]` (line 1295~1356) AxisTopic 폐기 문서화 (0.5 SP)
- 상위 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-13-deck-abolition-axis-absorption.md`

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | LT E5 S5-1 | Deck 8 책임 필드 Axis 로 이전 (`progressStatus`, `mode`, `lastAccessed`, `learningMaterialId`, `onLibrary`, `publishedAt` 등) — 도메인 리팩토링 | 3 |
| 2 | LT E5 S5-2 | Flyway V32 `card_drop_deck_id.sql` — `card.deck_id` FK 폐기 | 1 |
| 3 | LT E5 S5-3 | `/decks/*` 엔드포인트 폐기 → `/axes/*` 통합 (410 Gone or 301 redirect · v1 결정) | 2 |
| 4 | LT E5 S5-4 | Flyway V33 `deprecate_deck_table.sql` — `deck`/`sub_deck` 테이블 soft-deprecate (RENAME → `_archived_deck`, `_archived_sub_deck`) | 0.5 |
| 5 | LT E5 S5-5 | `docs/DOMAIN.md` §Deck 절 삭제 · §Axis 로 병합 · `product-deck.md` 삭제 준비 (workflow SDD Product 리스트에서 제거) | 0.5 |
| 6 | LT E3 S3-5 | `AxisTopic` 폐기 문서화 + ADR022 용어 준수 확인 (코드 zero-ref 검증) — 병합 커밋 | 0.5 |

**PR 종료 신호**:
- `deck` / `sub_deck` 테이블이 `_archived_deck` / `_archived_sub_deck`로 RENAME · Flyway V33 성공
- `card.deck_id` 컬럼 폐기 · 애플리케이션 코드에서 `deck_id` 참조 grep pass → 0건
- `/decks/*` 엔드포인트 410 Gone 응답 (재작성 정책은 SDD 원문 결정)
- `docs/DOMAIN.md`에서 `Deck` Aggregate 정의 제거 · Axis 흡수 반영
- `AxisTopic` 코드 참조 zero 검증 (grep pass) · S3-5 문서화 완주

**병렬 진입 조건**: PR#2 (LT E6) 병행 가능. Review BC PR (PR#3~#5)과는 순차 (Card BC → Deck 폐기 → Layer progressStatus → Review BC 신설).

**Reviewer 세션**: 5관점 발사 (Domain / Architecture / API-Exception / Test / Sceptical). **특히 Domain Reviewer가 "Deck 8 책임의 Axis 이전이 각 필드별로 자연스러운가"를 확인**.

### Epic PR #2 — `LT-E6-DUAL-SCOPE` (Layer.progressStatus + ReviewScope enum 착지)

**Base 브랜치**: `feat/038-lt-e6-dual-scope`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-learning-tower.md`
- Epic: `# [Epic 6] Review 이중 스코프 (Axis / Layer)` (line 2139~2409)
- Story 범위: `## [Story 6-1]` (line 2184) ~ `## [Story 6-5]` (line 2372)
- 상위 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-14-review-strategy-axis-layer-scope.md`

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | LT E6 S6-1 | Flyway V34 `review_session_scope.sql` — `ReviewSession.scope: ReviewScope` enum + `scopeId: Long` 컬럼 추가 (`AXIS`/`LAYER`) | 1.5 |
| 2 | LT E6 S6-2 | `ReviewSessionRepository.findAllByAxisId(axisId)` / `findAllByLayerId(layerId)` 이중 조회 쿼리 신설 | 0.5 |
| 3 | LT E6 S6-3 | `POST /layers/{layerId}/review-sessions` 엔드포인트 신설 (Layer 스코프 세션 진입점) | 2 |
| 4 | LT E6 S6-4 | Flyway V35 `layer_progress_status.sql` + `Layer.progressStatus` 파생 로직 (하위 axis 3-state → Layer 3-state) | 1 |
| 5 | LT E6 S6-5 | FE 용어 "Layer 1" 대체 명명 · `docs/ux/wip-language.md` 갱신 | 0.5 |

**PR 종료 신호**:
- `review_session.scope` VARCHAR 컬럼 + `scope_id` BIGINT 컬럼 존재 (V34 성공) · CHECK 제약 `scope IN ('AXIS', 'LAYER')`
- `findAllByAxisId` / `findAllByLayerId` 쿼리 실행 검증 (`@DataJpaTest`)
- `POST /layers/{layerId}/review-sessions` → 201 + Layer 스코프 세션 생성
- `Layer.progressStatus` 값이 하위 axis 상태에 따라 자동 파생 (`ALL COMPLETED → COMPLETED` / `ANY IN_PROGRESS → IN_PROGRESS` / else `NOT_STARTED`)
- `docs/ux/wip-language.md`에 새 Layer 명명 정합

**의존**: PR#1 완주 후 진입 (Deck 폐기 후에만 Layer가 Axis 컬렉션 진행률의 유일한 상위 개념이 됨).

**⚠️ Supersede 궤적 사전 명시**: S6-1이 심는 `ReviewSession.scope`·`scopeId` 컬럼과 필드는 PR#4에서 `batchId` FK로 재편되며 **폐기**됨 (issue-25의 cross-layer 짬뽕 큐 방향 전환 반영). S6-2/S6-3 Repository·엔드포인트도 PR#4 S2-4가 폐기 (410 Gone). 실제 코드 궤적을 명시적으로 관리.

**Reviewer 세션**: 5관점 발사. **특히 Sceptical Reviewer가 "S6-1·2·3이 심는 궤적이 PR#4에서 자연스럽게 폐기되는가"를 판정**.

### Epic PR #3 — `REV-E1-DAILY-BATCH` (DailyLearningBatch Aggregate + 자정 close cron)

**Base 브랜치**: `feat/039-rev-e1-daily-batch`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-review.md`
- Epic: `# [Epic 1] DailyLearningBatch Aggregate + DailyCardEntry 자식` (line 395~728)
- Story 범위: `## [Story 1-1]` (line 447) ~ `## [Story 1-8]` (line 699)
- 상위 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-24-daily-learning-batch.md`

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | REV E1 S1-1 | Flyway V36 `daily_learning_batch.sql` (테이블 + UNIQUE(user_id, batch_date)) + 롤백 | 1 |
| 2 | REV E1 S1-2 | `DailyLearningBatch` Aggregate + `DailyCardEntry` 자식 Entity 도메인 신설 | 2 |
| 3 | REV E1 S1-3 | Repository + Application Service (`getOrCreateToday`, `markViewed`, `queryHistory`) | 2 |
| 4 | REV E1 S1-4 | `generateFor()` orchestration (교차 layer 카드 큐 생성 + archive 부수 처리) | 2 |
| 5 | REV E1 S1-5 | 자정 close cron `0 5 0 * * *` KST (전일 batch finalize + streak 갱신 트리거) | 1 |
| 6 | REV E1 S1-6 | `BatchStreakCalculator` 도메인 서비스 (realtime · 연속 완주 일수 판정) | 1 |
| 7 | REV E1 S1-7 | 엔드포인트 세트 (`POST /daily-batch/today`, `GET /daily-batch/history?days=N`) | 1 |
| 8 | REV E1 S1-8 | ErrorCode 신설 (`DAILY_BATCH_CLOSED`, `DAILY_BATCH_NOT_FOUND`) | 0.5 |

**PR 종료 신호**:
- `daily_learning_batch` + `daily_card_entry` 테이블 생성 (V36 성공) · `UNIQUE(user_id, batch_date)` 존재
- `POST /daily-batch/today` → 200 + 오늘 batch (없으면 lazy 생성) + `DailyCardEntry` 리스트 (교차 layer)
- `GET /daily-batch/history?days=7` → 최근 7일 batch 완료 비율 + streak
- 자정 cron 로컬 부팅 검증 (H2 환경 · `@SchedulerLock` 확인)
- `DAILY_BATCH_CLOSED` 응답: 어제 자정 close된 batch에 `markViewed` 시도 → 409 Conflict
- `./gradlew test --tests "com.example.thirdtool.Review.*"` BUILD SUCCESSFUL

**의존**: PR#1 완주 (Deck 폐기 → Card는 Axis에만 매핑) · PR#2 완주 (Layer progressStatus 파생 → batch 진행률 상위 지표 계산 준비) 후 진입. Card BC (M4 완주) 도메인 메서드가 batch generateFor 소스.

**Reviewer 세션**: 5관점 발사. **특히 Domain Reviewer가 "DailyLearningBatch가 진짜 Aggregate root인가 (DailyCardEntry 캡슐화 · 컬렉션 접근 규칙)"를 검증**.

### Epic PR #4 — `REV-E2-SESSION-REORG` (ReviewSession cross-layer 재편 · scope 폐기)

**Base 브랜치**: `feat/040-rev-e2-session-reorg`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-review.md`
- Epic: `# [Epic 2] ReviewSession cross-layer 재편` (line 730~985)
- Story 범위: `## [Story 2-1]` (line 772) ~ `## [Story 2-7]` (line 959)
- 상위 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-25-review-cross-layer-scope.md` (**issue-14의 dual scope를 supersede**)

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | REV E2 S2-1 | `ReviewSession.startFrom(batch, now)` 팩토리 재작성 + 기존 (`startForAxis`, `startForLayer`) 폐기 | 1 |
| 2 | REV E2 S2-2 | `ReviewSession.recordView()` — batch 동기화 로직 (`DailyCardEntry.markViewed` 트리거) | 1 |
| 3 | REV E2 S2-3 | 동시 세션 정책 — 새 세션 시작 시 진행 중 세션 자동 finish (single active session per user) | 1 |
| 4 | REV E2 S2-4 | 신규 엔드포인트 세트 (`POST /review-sessions/from-batch`, `POST /review-sessions/{id}/record-view`) + 기존 `/layers/{id}/review-sessions` 폐기 (410 Gone) | 2 |
| 5 | REV E2 S2-5 | `StateRecommendationDistributor` 폐기 결정 (규칙 추천은 Epic 3의 `RecommendationEngine`으로 이관) | 0.5 |
| 6 | REV E2 S2-6 | `ReviewSessionRepository` 쿼리 재편 (batchId 기반 · `findByAxisId` / `findByLayerId` 폐기) · Flyway V37 `review_session_reorg.sql` (scope 컬럼 폐기 + batchId FK 추가) | 1 |
| 7 | REV E2 S2-7 | 통합 테스트 — batch ↔ session 동기화 시나리오 (view 기록 → 양쪽 반영) | 1 |

**PR 종료 신호**:
- `review_session.scope` / `scope_id` 컬럼 폐기 (V37 성공) · `review_session.batch_id` FK 추가 · CHECK 제약 재작성
- `POST /review-sessions/from-batch` → 200 + 오늘 batch 기반 세션 (진행 중 세션 있으면 자동 finish + 새 세션 시작)
- `GET /layers/{layerId}/review-sessions` → 410 Gone (PR#2 S6-3이 심었던 궤적 폐기)
- `ReviewSession` 도메인에 `scope`·`scopeId` 필드 grep pass → 0건
- `StateRecommendationDistributor` 클래스 삭제 · 참조 zero
- batch ↔ session 통합 테스트 시나리오 5건 이상 통과

**의존**: PR#3 완주 (DailyLearningBatch가 세션 소스로 필수).

**⚠️ PR#2 궤적 폐기**: LT E6 S6-1/S6-2/S6-3이 심은 `ReviewSession.scope`·`scopeId`·`findAllByAxis/LayerId`·`POST /layers/{id}/review-sessions`가 본 PR에서 **명시적으로 폐기**됨. 실제 코드 궤적의 issue-14 → issue-25 supersede 반영.

**Reviewer 세션**: 5관점 발사. **특히 Architecture Reviewer가 "PR#2가 심은 궤적의 폐기·마이그레이션이 안전한가"를 검토** (BC 의존 · 잔재 참조).

### Epic PR #5 — `REV-E3-DASHBOARD` (캐시 대시보드 + 규칙 추천 + 주간 요약)

**Base 브랜치**: `feat/041-rev-e3-dashboard`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-review.md`
- Epic: `# [Epic 3] 캐시 측정 대시보드 + 규칙 기반 추천 + 조건부 주간 요약` (line 987~1302)
- Story 범위: `## [Story 3-1]` (line 1028) ~ `## [Story 3-8]` (line 1268)
- 상위 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-26-cache-measurement-dashboard.md`

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | REV E3 S3-1 | `LearningDashboardQueryService` — 오늘/7일/30일 완료비율 + streak 집계 | 2 |
| 2 | REV E3 S3-2 | `RecommendationEngine` 도메인 서비스 — 규칙 판정 (3주 평균 <50% → `SUGGEST_DOWNGRADE` · 4주 완주 → `SUGGEST_UPGRADE`) | 2 |
| 3 | REV E3 S3-3 | 주간 요약 cron (일요일 21:00 KST) + 조건 판정 + notification 발송 (저성과자만) | 2 |
| 4 | REV E3 S3-4 | `LearningDashboardController` + Response DTO (`GET /dashboard/summary`) | 1 |
| 5 | REV E3 S3-5 | 추천 accept orchestration (mode 변경 트리거 · Card BC `updateMode` 호출) | 2 |
| 6 | REV E3 S3-6 | Application config 노출 (임계값 externalize · `application.yml`) | 0.5 |
| 7 | REV E3 S3-7 | 통합 테스트 — 3주 batch 데이터 시나리오 (다양한 완료비율 → 추천 판정) | 1 |
| 8 | REV E3 S3-8 | In-app notification 스키마 (v1 minimal · `type`, `title`, `body`, `metadata`) | 1 |

**PR 종료 신호**:
- `GET /dashboard/summary` → 200 + 오늘/7일/30일 완료비율 + streak + 활성 추천 (있는 경우)
- 3주 batch 완료비율 40% → `RecommendationEngine`이 `SUGGEST_DOWNGRADE`(현재 mode 하강) 판정
- 주간 요약 cron 로컬 부팅 검증 (일요일 21:00 KST 트리거 확인)
- 추천 accept API → Card BC `LearningMode` 하강 트리거 + notification `RECOMMENDATION_ACCEPTED` 발송
- `application.yml`에 `dashboard.threshold.downgrade-avg=0.5` 등 externalize
- `./gradlew test --tests "com.example.thirdtool.Review.dashboard.*"` BUILD SUCCESSFUL

**의존**: PR#3 (DailyLearningBatch가 데이터 소스) + PR#4 (session 재편이 batch 소비 정합) 완주 후 진입.

**Reviewer 세션**: 5관점 발사. **특히 Sceptical Reviewer가 "규칙 임계값이 사용자 UX 관점에서 자연스러운가 (하강 추천이 과빈발하지 않는가)"를 판정** · 임계값 튜닝 근거 명시.

---

## Story 카테고리별 합계 (SDD Story 단위)

| 카테고리 | SDD Epic/Story | Story 수 | SP | 비중 |
| --- | --- | --- | --- | --- |
| LT Epic 5 (Deck BC 폐기) | LT E5 S5-1~S5-5 | 5 | 7 | 16% |
| LT Epic 3 잔여 (AxisTopic 폐기 문서화) | LT E3 S3-5 | 1 | 0.5 | 1% |
| LT Epic 6 (Review 이중 스코프) | LT E6 S6-1~S6-5 | 5 | 5.5 | 13% |
| Review Epic 1 (DailyLearningBatch) | REV E1 S1-1~S1-8 | 8 | 10.5 | 25% |
| Review Epic 2 (ReviewSession cross-layer) | REV E2 S2-1~S2-7 | 7 | 7.5 | 18% |
| Review Epic 3 (Dashboard + 추천 + 주간요약) | REV E3 S3-1~S3-8 | 8 | 11.5 | 27% |
| **합계** | | **34** | **42.5** | 100% |

**분배 근거**:
- M4 실 착지 ~30 SP 대비 +42% 상승. Review BC 신설 3 Epic 완주 (E1+E2+E3 = 29.5 SP)가 본 M5의 축이며 이는 신설 도메인 심층도가 크기 때문 (Aggregate 신설 · 자정 cron · dashboard 집계 · 규칙 엔진 · notification 배선).
- **Review 카테고리 70%** (E1+E2+E3) — 이슈 #24~#26 이관 완주 · Review BC 처음이자 완전 착지.
- **Learning Tower 카테고리 30%** (E5 + E6 + E3S3-5) — Deck 폐기가 Review 신설의 선행 필수 · Layer progressStatus가 dashboard 상위 지표 준비.
- **AS Epic 3** — Story 신규 착수 없음. eval.md §M5 baseline로 흡수 (M4 role 4종 안정화 관찰).
- **5개 Epic PR**: 각 PR = SDD Epic 하위 Story 묶음 (또는 E3S3-5 병합). Reviewer 세션 5관점 필수.
- **SDD Story 규칙 준수**: 모든 M5 작업이 `product-learning-tower.md`·`product-review.md`의 명시된 Epic/Story에 대응. 신규 Story 추가 없음.

---

## 종료 신호 — "Deck 폐기 완주 + Review BC 3 Epic 완주 + Dashboard 첫 도착"

본주 종료 시점에 다음이 모두 성립해야 한다. (8 신호 중 6개 이상 → 0.0.5v 동결)

- [ ] **머지 신호**: Epic PR 5개 중 최소 4개 머지 (80%)
- [ ] **Deck 폐기 신호**: `deck`/`sub_deck` 테이블이 `_archived_deck`/`_archived_sub_deck`로 RENAME · `card.deck_id` 컬럼 폐기 · `/decks/*` 엔드포인트 410 Gone · 애플리케이션 코드에서 Deck 참조 grep pass 0건 · `docs/DOMAIN.md` §Deck 절 제거
- [ ] **AxisTopic 폐기 신호**: 코드에서 `AxisTopic` 참조 zero 검증 · ADR022 용어 준수
- [ ] **Layer progressStatus 신호**: `Layer.progressStatus` 파생 로직 동작 (하위 axis 상태 → Layer 3-state 매핑) · FE 용어 "Layer 1" 대체 명명 반영
- [ ] **DailyLearningBatch 신호**: `POST /daily-batch/today` → 오늘 batch (없으면 lazy 생성) + `DailyCardEntry` cross-layer 큐 · 자정 close cron 로컬 부팅 검증 · `BatchStreakCalculator` 연속 완주 일수 판정 정확
- [ ] **ReviewSession 재편 신호**: `review_session.scope`/`scope_id` 폐기 · `batch_id` FK 추가 · `POST /review-sessions/from-batch` 진입 · 동시 세션 정책 (자동 finish) 동작 · `StateRecommendationDistributor` 삭제
- [ ] **Dashboard 신호**: `GET /dashboard/summary` → 오늘/7일/30일 완료비율 + streak 반환 · 3주 저완료 시나리오 → `SUGGEST_DOWNGRADE` 추천 판정 · 주간 요약 cron 로컬 부팅 검증 · 추천 accept → Card BC mode 하강 트리거 · notification 발송
- [ ] **테스트 신호**: 5개 Epic PR 각각 도메인 단위 · Repository Slice · Controller Slice 통과. `./gradlew test` BUILD SUCCESSFUL. 신규 테스트가 해피/엣지/예외 3구분 각각 1건 이상. 통합 테스트 5건 이상 (batch ↔ session 동기화 · 3주 batch 시나리오 · 추천 accept end-to-end)

**미합격 처리**: 위 8 신호 중 6개 미만 성립 시 M5를 0.0.5v로 동결하지 않고 0.0.5.1v 패치 발행 → 다음 주 초까지 연장. Review E3 (Dashboard)가 미달일 경우 우선 M6로 이관 판단.

---

## 의존 chain

```
[선행: M4 착지]
Card BC 대재편 (Mode·Budget·createdMode) ✅ 완료
Card→Axis 직접 매핑 (LT E4) ✅ 완료
AS Static Adapter role 4종 catalog ✅ 완료

[D1: 병렬 착수]
PR#1 (LT-E5-DECK-ABOLISH)  ──►  PR#2 (LT-E6-DUAL-SCOPE · 병행 가능)
  LT E5 S5-1 ~ S5-5 + LT E3 S3-5     LT E6 S6-1 ~ S6-5
     │                                  │
     ▼                                  ▼
  Deck 폐기 완주 · V32/V33          ReviewScope enum · Layer progressStatus · V34/V35
     │                                  │
     └──────────┬───────────────────────┘
                ▼
PR#3 (REV-E1-DAILY-BATCH)
  REV E1 S1-1 ~ S1-8 · V36
     │
     ▼
PR#4 (REV-E2-SESSION-REORG)
  REV E2 S2-1 ~ S2-7 · V37 (PR#2 궤적 폐기)
     │
     ▼
PR#5 (REV-E3-DASHBOARD)
  REV E3 S3-1 ~ S3-8
     │
     ▼
  Review BC 완주 · Dashboard 첫 도착
```

**병렬 진입 가능 묶음**:
- **A** (D1 · 07-22 Wed): PR#1 (LT E5 S5-1 착수) + PR#2 (LT E6 S6-1 착수 · 병행)
- **B** (D2 · 07-23 Thu): PR#1 S5-2/S5-3 + PR#2 S6-2/S6-3 진행
- **C** (D3 · 07-24 Fri): PR#1 S5-4/S5-5 + LT E3 S3-5 병합 완주 → **PR#1 머지 + Reviewer 세션** · PR#2 S6-4 진행
- **D** (D4 · 07-25 Sat): PR#2 S6-4/S6-5 완주 → **PR#2 머지 + Reviewer 세션 (Sceptical 강조 · 궤적 폐기 예정 명시)** · PR#3 착수 (REV E1 S1-1 Flyway V36)
- **E** (D5 · 07-26 Sun): PR#3 S1-2~S1-6 (도메인·Repo·Service·cron·streak) → PR#3 S1-7/S1-8 완주 → **PR#3 머지 + Reviewer 세션 (Domain 강조 · Aggregate 판정)** · PR#4 착수 (REV E2 S2-1)
- **F** (D6 · 07-27 Mon): PR#4 S2-2~S2-6 (동기화·정책·엔드포인트·V37 궤적 폐기) → PR#4 S2-7 완주 → **PR#4 머지 + Reviewer 세션 (Architecture 강조 · 궤적 폐기 안전성)** · PR#5 착수 (REV E3 S3-1)
- **G** (D7 · 07-28 Tue): PR#5 S3-2~S3-8 (규칙 엔진·cron·컨트롤러·accept·config·통합·notification) 완주 → **PR#5 머지 + Reviewer 세션 (Sceptical 강조 · 규칙 임계값 판정)** · 통합 로컬 검증 · `outcome.md`·`review.md`·`eval.md` 골격 · 0.0.5v 동결 판정

**직렬 (M5 합격선까지)**: PR#1·PR#2 (D1-D3/D4 병렬) → PR#3 (D4-D5) → PR#4 (D5-D6) → PR#5 (D6-D7). Review BC chain이 D7에 종료.

---

## 작업 일정 (Epic PR 단위 체크리스트)

D1 = 2026-07-22 (Wed). 종료 D7 = 2026-07-28 (Tue). 7일 안에 5 Epic PR.

| 일 | 날짜 | 잡힌 작업 (Epic PR 진행) — SDD Story 기준 |
| --- | --- | --- |
| D1 (수) | 07-22 | PR#1 착수 (**LT E5 S5-1** Deck 8 책임 필드 Axis 이전 · 도메인 리팩토링) + PR#2 착수 (**LT E6 S6-1** Flyway V34 ReviewScope enum + scopeId 컬럼) |
| D2 (목) | 07-23 | PR#1 **LT E5 S5-2** (V32 card.deck_id 폐기) + **S5-3** (`/decks/*` 엔드포인트 폐기) · PR#2 **LT E6 S6-2** (`findAllByAxisId`/`findAllByLayerId` Repository) + **S6-3** (`POST /layers/{id}/review-sessions` 엔드포인트) |
| D3 (금) | 07-24 | PR#1 **LT E5 S5-4** (V33 deck/sub_deck soft-deprecate) + **S5-5** (`docs/DOMAIN.md` §Deck 삭제) + **LT E3 S3-5** (AxisTopic 폐기 문서화 병합) 완주 → **PR#1 머지 + Reviewer 세션 (Domain 강조)** · PR#2 **LT E6 S6-4** (V35 layer_progress_status + 파생 로직) 진행 |
| D4 (토) | 07-25 | PR#2 **LT E6 S6-4** 마무리 + **S6-5** (FE 용어 대체 · wip-language.md 갱신) 완주 → **PR#2 머지 + Reviewer 세션 (Sceptical 강조 · S6-1~3 폐기 궤적 예정 명시)** · PR#3 착수 (**REV E1 S1-1** Flyway V36 daily_learning_batch) |
| D5 (일) | 07-26 | PR#3 **REV E1 S1-2** (Aggregate + DailyCardEntry 도메인) + **S1-3** (Repository + Service) + **S1-4** (generateFor orchestration) + **S1-5** (자정 close cron) + **S1-6** (BatchStreakCalculator) + **S1-7** (엔드포인트) + **S1-8** (ErrorCode) 완주 → **PR#3 머지 + Reviewer 세션 (Domain 강조 · Aggregate 판정)** · PR#4 착수 (**REV E2 S2-1** ReviewSession.startFrom 재작성) |
| D6 (월) | 07-27 | PR#4 **REV E2 S2-2** (recordView batch 동기화) + **S2-3** (동시 세션 자동 finish) + **S2-4** (신규 엔드포인트 + `/layers/*/review-sessions` 410 Gone) + **S2-5** (StateRecommendationDistributor 폐기) + **S2-6** (Repository 재편 + V37 scope 컬럼 폐기 + batchId FK) + **S2-7** (통합 테스트) 완주 → **PR#4 머지 + Reviewer 세션 (Architecture 강조 · 궤적 폐기 안전성)** · PR#5 착수 (**REV E3 S3-1** LearningDashboardQueryService) |
| D7 (화) | 07-28 | PR#5 **REV E3 S3-2** (RecommendationEngine) + **S3-3** (주간 요약 cron) + **S3-4** (Dashboard Controller) + **S3-5** (추천 accept orchestration) + **S3-6** (config externalize) + **S3-7** (통합 테스트 3주 시나리오) + **S3-8** (notification 스키마) 완주 → **PR#5 머지 + Reviewer 세션 (Sceptical 강조 · 규칙 임계값 판정)** · 통합 로컬 검증 3시나리오 + `outcome.md` · `review.md` · `eval.md` 골격 · 0.0.5v 동결 판정 |

**Reviewer 세션 규칙**: 각 Epic PR 머지 직전 5관점 병렬 발사 (`.claude/rules/review.md` §3~§7). 사용자 확인 후 머지. M4 표준 스케일 유지 · 5회 진행.

**Flyway V 버전 순서 관리** (M4에서 V23~V30 소모):
- V31: (예약 · Deck 8 책임 Axis 이전 도메인 리팩토링용 · 필요 시 스키마 변경 흡수)
- V32: `card_drop_deck_id.sql` (PR#1 S5-2)
- V33: `deprecate_deck_table.sql` (PR#1 S5-4 · RENAME → `_archived_*`)
- V34: `review_session_scope.sql` (PR#2 S6-1 · scope + scopeId 컬럼)
- V35: `layer_progress_status.sql` (PR#2 S6-4)
- V36: `daily_learning_batch.sql` (PR#3 S1-1)
- V37: `review_session_reorg.sql` (PR#4 S2-6 · **V34 scope 컬럼 폐기** + batchId FK 추가 · issue-25 supersede 물리 반영)
- V38: (예약 · PR#5 S3-6 config 저장 필요 시 · 대부분 application.yml externalize로 스킵)

D1 시작 전 V 버전 할당표를 사용자 확인 필수 (M3 R2·M4 조치 반영). 충돌 방지. **V34 → V37 폐기 궤적은 R페어(`R34__rollback_*.sql` · `R37__rollback_*.sql`) 각각 준비 필수**.

---

## 리스크와 관찰 포인트

| 영역 | 리스크 | 관찰 포인트·완화 |
| --- | --- | --- |
| Deck BC 완전 폐기 (S5-1 도메인 리팩토링) | Deck 8 책임 필드의 Axis 이전이 domain 심층 리팩토링. 각 필드마다 자연스러운 위치·불변식 확인 필요 | Domain Reviewer가 각 필드(`progressStatus`·`mode`·`lastAccessed`·`learningMaterialId`·`onLibrary`·`publishedAt`)의 Axis 이전이 응집력 있는지 검증 · docs/DOMAIN.md §Axis 병합 후 재검토 |
| `/decks/*` 엔드포인트 폐기 정책 | 410 Gone vs 301 redirect vs 404. FE 코드가 여전히 참조할 위험 | FE 대응 마일스톤 (`fe-milestones/0.0.5v/milestone.md`) 동기 확인 · 폐기 방식은 SDD 원문 준수 |
| **LT E6 S6-1~3 궤적 폐기 정합** | PR#2가 심은 `ReviewSession.scope`·`scopeId` 컬럼·필드·엔드포인트가 PR#4에서 폐기됨. V34 → V37 마이그레이션 순서 실패 시 코드-DB 불일치 | Flyway 순서표 D1 사전 확인 필수 · V37 rollback SQL에 V34 상태 복원 명시 · Architecture Reviewer가 PR#4 승인 전 궤적 폐기 안전성 판정 |
| Review BC 신설 도메인 심층도 | Aggregate 신설 (DailyLearningBatch + DailyCardEntry) + Domain Service 2종 (BatchStreakCalculator · RecommendationEngine) + cron 2건 · 코드 물량 대. 42.5 SP 자체가 M4 대비 +42% | 5개 Epic PR 순차 · Reviewer 세션 5회 유지 · 각 PR별 domain 단위 테스트 우선 · 도메인 캡슐화 위반 감시 |
| 자정 close cron · 주간 요약 cron 로컬 검증 | H2 환경에서 cron 실행 검증. `@SchedulerLock` 도입 여부 결정 필요 (multi-instance 대비 v1은 미필요할 수도) | v1은 단일 인스턴스이므로 `@SchedulerLock` 없이 로컬 실행 검증만. M6~M7 배포 재개 시점에 lock 재검토 |
| 규칙 기반 추천 임계값 | `3주 평균 <50% → SUGGEST_DOWNGRADE` / `4주 완주 → SUGGEST_UPGRADE` — 사용자 UX 관점에서 하강 추천 과빈발 위험 | Sceptical Reviewer가 임계값 근거·과빈발 시나리오 판정 · `application.yml` externalize로 튜닝 유연성 확보 |
| notification 스키마 v1 minimal | 실제 발송 어댑터 (email/push)는 M7 배포 재개 시점. v1은 in-app만 · schema만 신설 | v1 스키마 확정 후 v2 이후 어댑터 배선 시 breaking 지양. `type`·`title`·`body`·`metadata` 4필드로 제약 |
| Dashboard 3주 데이터 시나리오 | DailyLearningBatch 데이터가 없는 신규 유저 시나리오 · 부분 완주 · 완주 후 mode 변경 시나리오 다양 | S3-7 통합 테스트 시나리오 5건 이상 · empty batch · 미완 batch · 완주 batch · 혼합 · 3주 저완료 판정 |
| SP +42% 도전적 목표 | M4 30 SP → M5 42.5 SP. 5 Epic PR 순차/병렬 · Reviewer 5회 시간 · 로컬 검증 3시나리오 · 각 산출물 갱신 시간 | D3 진행률 (PR#1·PR#2 머지) 확인 → 미달 시 PR#5 (Review E3 Dashboard) 축소 (`RecommendationEngine`만 M6 이관 검토) |
| PATCH JsonNullable (M3 R3 · M4 재확인) | Dashboard Response DTO 등 신규 API에서 PATCH 있을 경우 재발 | 각 신규 PATCH 신설 시점에 즉시 JsonNullable 도입 · 첫 지적 시점에 조치 (M3 재발 방지 프로토콜 M5 유지) |
| DOMAIN.md 갱신 부담 | Deck 절 삭제 · Axis 절 흡수 · Review Aggregate 신설 · DailyLearningBatch·DailyCardEntry 신설 서술. 대량 문서 갱신 | PR#1 S5-5에서 §Deck 삭제 · PR#3 S1-2에서 §Review 신설. 각 PR 안에서 담당 갱신 · Story별 분산 |
| Reviewer 세션 병목 (M4 실측) | 5관점 Reviewer 세션이 각 PR마다 필요. 5 PR × 5관점 = 25 reviewer 발사 · 병렬 유지 | 병렬 발사 유지 · Sceptical Reviewer가 SUPERSEDED 궤적 판정에 반복 참조 시 이전 판정 문서화 (review.md에 요약) |

---

## 다음 마일스톤 (M6 / 0.0.6v) 후보

본주 결과를 보고 결정하지만, 릴리스 문서에 잠긴 트라젝토리 그대로:

**M6 / 0.0.6v (07-29 ~ 08-04) — LLM Adapter + Cascade + Rate Limit + Card BC 안정화**
- **AS Epic 4 (LLM Adapter · 5 Story)** — Spring AI + Vertex AI Gemini Flash 2.5 배선 · Static→LLM cascade 진입점 · concept-spec.txt 프롬프트 구성 · 응답 정합 파싱 · v1 관찰 지표
- **AS Epic 5 (SuggestionCascade + Rate Limit · 3 Story)** — Static → LLM fallback cascade 구조화 · IP/user 기반 rate limiter · Layer/Axis 엔드포인트 확장
- **Card BC UX 안정화 잔재** — M5 Review BC 연결부 관찰 · mode 다운/업 시나리오 실측 사용자 UX 회수 · createdMode 하이브리드 사용자 안내
- **AS Epic 3 관찰 흡수** — role 4종 catalog 오탐 통계 (M3~M5 실측 데이터) · 필요 시 catalog 튜닝

**Epic PR 예상 3~4건** (AS E4 · AS E5 · Card BC 안정화 · 필요 시 AS E3 튜닝).

---

## Product 상태 전환 신호 (M5 종료 시)

- `in-progress/product-learning-tower.md` — **Epic 5 완주** 표기 (Deck BC 완전 폐기 · Axis 흡수 · `docs/DOMAIN.md` §Deck 절 삭제 반영) · **Epic 3 완주** 표기 (S3-5 AxisTopic 폐기 문서화 완주) · **Epic 6 착지** 표기 (S6-1~3의 supersede 궤적은 리스크 관리 후 폐기 확인)
- `in-progress/product-review.md` — **Epic 1·2·3 완주** 표기 (Review BC 3 Epic 완주 · DailyLearningBatch + ReviewSession cross-layer + Dashboard 완주 · 신설 후 최초 정합 릴리스 준비)
- `in-progress/product-card.md` — 상태 유지 (M4 완주 · M5는 안정화 관찰만 · 회귀 발견 시 별도 패치)
- `in-progress/product-ai-suggestion.md` — Epic 3 completion 상태 유지 (M5 대상 없음) · role 4종 관찰만 · Epic 4 (LLM Adapter) M6 이관 유지
- `in-progress/product-ai-interactive-roadmap.md` — 진입 X, 상태 유지 (M6~M7 이관)
- `fix/brainstorming/version/0.0.2v/issue-13/14` → **resolved** (Deck 폐기 + Layer 이중 스코프 M5 이관 완료 · issue-14는 issue-25가 supersede하는 궤적 명시)
- `fix/brainstorming/version/0.0.2v/issue-24/25/26` → **resolved** (Review BC 3 Epic 완주)
- `fix/brainstorming/version/0.0.2v/issue-08` → **resolved** (AxisTopic 폐기 문서화 · ADR022 용어 준수)
- `fix/brainstorming/version/0.0.2v/issue-18/20` → v2 · M7 이관 유지 (Roadmap 재생성 · 예산 cap)
- `fix/brainstorming/version/0.0.2v/issue-17` → M6~M7 이관 유지 (AI Two-Step Generation · LLM Adapter 이후)
- `workflow/task/pes/workspectrum/sdd/in-progress/product-deck.md` (있는 경우) → **삭제 준비** (Epic 5 S5-5 완주 후 Product 리스트에서 제거 예정)

---

## brainstorming 트리거

본 M5 완료 후 `workflow/task/pes/brainstorming/0.0.6v/` 신설:
- LLM Adapter 프롬프트 실측 튜닝 시나리오 (`brainstorming/0.0.6v/llm-adapter-prompt-tuning.md`) — Vertex AI Gemini Flash 2.5 응답 정합 실측 후 튜닝 포인트
- Static→LLM cascade 판정 임계값 UX (`brainstorming/0.0.6v/suggestion-cascade-threshold.md`) — Static이 catalog에 없을 때 자동 LLM 호출 판정 로직 · UX 안내
- Card BC 실측 회수 UX (`brainstorming/0.0.6v/card-mode-shift-realdata.md`) — M5 dashboard 데이터 실측 후 mode 하강/상승 안내 UX 튜닝
- Review BC UX 정합 관찰 (`brainstorming/0.0.6v/review-daily-batch-ux-check.md`) — DailyBatch 큐 화면 · Dashboard 3주 데이터 시나리오 실측 관찰
- Rate Limiter 임계값 결정 (`brainstorming/0.0.6v/rate-limiter-threshold.md`) — v1 IP/user 기반 rate 한도 결정

---

## 참고

- 잔여 Story 인벤토리 출처: `workflow/task/pes/workspectrum/sdd/in-progress/` 19개 Product 파일
- M2·M3·M4 pivot 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-13 · issue-14 · issue-24 · issue-25 · issue-26 · issue-08` (M5 착지 대상)
- 마일스톤 패키지 의도: `workflow/task/milestones/references/001.md`
- FE 대응 마일스톤: `workflow/task/pes/fe/fe-milestones/version/0.0.5v/milestone.md` (동일 주간)
- 이전 마일스톤: `workflow/task/milestones/version/0.0.4v/milestone.md` — M4 원안 · `../0.0.4v/outcome.md` · `../0.0.4v/review.md` — M4 결과 · `../0.0.4v/eval.md` — AI 응답 품질 평가 프레임 확장
- **첫 릴리스 계획**: `workflow/task/milestones/release/version/0.0.1v/release.md` — 0.1.0v (~2026-08-19) 스코프 · M3~M8 트라젝토리
- 본 버전의 산출물 7종: `milestone.md`(본 문서) · `infra.md`(스킵) · `performance.md`(스킵) · `outcome.md` · `cost.md` · `review.md` · `eval.md`
- 양식 진화: 본 milestone은 0.0.4v milestone.md 양식 그대로 답습 (Epic PR 매트릭스 · 진행 중 Product 잔여 인벤토리 · 종료 신호 · 의존 chain · 리스크 · 다음 마일스톤 후보 · Product 상태 전환 · brainstorming 트리거). **eval.md는 M4에 role 4종 완주로 첫 확장 완료 · 본 M5가 두 번째 확장 (AS Epic 3 안정화 관찰 흡수 + Dashboard 규칙 임계값 판정 프레임 추가)**.
- **주요 SDD 참조 비율**:
  - `product-review.md` **~70%** (Epic 1·2·3 · Story 1-1~3-8 · 23 Story · SP 29.5)
  - `product-learning-tower.md` **~30%** (Epic 5 Story 5-1~5-5 + Epic 6 Story 6-1~6-5 + Epic 3 Story 3-5 · 11 Story · SP 13)
- **AS Epic 3 완주 확인**: Story 신규 착수 없음. `eval.md` §M5 baseline에 role 4종 안정화 관찰 흡수. Sceptical Reviewer가 M3~M5 응답 오탐 통계 참조.
