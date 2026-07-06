# M4 / 0.0.4v — Week of 2026-07-15 ~ 2026-07-21 (D1 = 2026-07-15 Wed)

> **마일스톤의 역할**: M3가 07-14에 동결됐다 (LT E3 Roadmap/Selection 노드 first-class 착지 + AS 6-Port + Static Adapter + AI 첫 응답 도착 · `chapters-outline` 로컬 실현). 본 M4는 M2·M3의 도메인 재편에 이어 **Card BC의 대규모 재편(Mode·Budget·createdMode) + Card→Axis 직접 매핑 완주 + AI Static Adapter role 다각화**를 목표로 한다.
>
> 한 주 = 한 버전 = `version/0.0.X v/` 폴더 하나. 본 버전(0.0.4v)에는 다음 7 파일이 들어간다:
> - `milestone.md` *(본 문서)* — 잡힌 양 + 일정 + 의존 + Epic PR 매트릭스
> - `infra.md` — (본 버전 배포 미포함 사유 명시)
> - `performance.md` — (본 버전 성능 측정 미수행 사유 명시)
> - `outcome.md` — 사용자·기능 성과 (Card Mode 재편 + OnFieldBudget 폐기 + Card→Axis 직접 매핑 + AI Static Adapter role 4종 완주)
> - `cost.md` — 비용 성과 (본 버전 로컬 개발 한정 → 비용 발생 0)
> - `review.md` — 회고 + 다음 버전 진입 신호
> - `eval.md` — AI 응답 품질 평가 프레임 확장 (M3 신설 후 첫 확장 · role catalog 3종 신설과 정합)

**릴리스 대응**: 본 M4는 첫 사용자 릴리스(0.1.0v, ~2026-08-19) 6주 로드맵의 **세 번째** 마일스톤. 자세한 스코프는 `workflow/task/milestones/release/version/0.0.1v/release.md` §Product별 릴리스 스코프 순회 참조.

**FE 대응 마일스톤**: `workflow/task/pes/fe/fe-milestones/version/0.0.4v/milestone.md` — 동일 주간, Card Mode·Budget 재편 UI + Card→Axis 조회 흐름 재정합.

---

## 0.0.4v 스코프 결정 (M3 이후, 2026-07-15)

**M3 착지 결과 요약**:
- ✅ LT Epic 3 재정의판 (Roadmap/Selection 노드 first-class · Flyway V20~V22) — 07-08 ~ 07-14
- ✅ AS 6-Port 확립 (Chapters/Chapter Outline·Subtree + Selection Outline·Subtree)
- ✅ Static Adapter 4개 + backend-developer.json 카탈로그 확장 (하네스 엔지니어링 5 챕터 포함)
- ✅ concept-spec.txt + 6개 프롬프트 템플릿 (v2 LLM Adapter 대비 자산)
- ✅ AI 첫 응답 도착 (M3 하이라이트): `POST /api/v1/suggestions/chapters-outline` → 200 + 5개 챕터
- ✅ Reviewer 5관점 세션 3회 재개 (M2 rush 이후 첫 정식 재개)
- ✅ 마일스톤 산출물 6종 (`outcome.md` · `cost.md` · `review.md` · `infra.md` · `performance.md` · `eval.md`) 신설

**M4 축 결정 — Card BC 대재편 + Card→Axis 직접 매핑 + AI role 다각화**:

M3 review.md §다음 마일스톤 결정 보정에서 확정된 우선순위 반영. 사용자 지시 "epic 단위로 pr 진행" 유지 · Reviewer 5관점 세션 정식 진행.

이 지시를 반영해 본 M4는 다음 3축으로 구성:

1. **Card BC Mode·Budget·createdMode 재편 완주** — 이슈 #21~#23 이관. `LearningMode` enum 재정의 + `OnFieldBudget` 폐기 + Card `createdMode` 필드 도입. **주력 (~60%)**.
2. **Card→Axis 직접 매핑 완주** — LT Epic 4 Story 4-1~4-5. `card.topic_id` 폐기 + `card.axis_id NOT NULL` + Coverage 재계산 축 스코프 이관. Card BC 재편의 후속 필수 작업. **부차 (~25%)**.
3. **AI Static Adapter role catalog 3종 신설** — AS Epic 3 Story 3-3~3-5. planner/designer/problem-solver JSON 카탈로그 큐레이션. LLM Adapter (M6) 대비 role 다각화. **경량 (~15%)**.

**Epic 단위 PR 5건 예정 (본주 목표)**:

| Epic PR | 대응 | 예상 SP | 종료 목표 |
|---|---|---|---|
| PR#1 (CARD-E1-MODE-REORG) | Card Epic 1 Story 1-1~1-5 (Mode enum 재편) | 5 | D3 |
| PR#2 (CARD-E2-BUDGET-ABOLISH) | Card Epic 2 Story 2-1~2-6 (OnFieldBudget 폐기 + ArchiveReason 재편) | 8 | D5 |
| PR#3 (CARD-E3-CREATEDMODE) | Card Epic 3 Story 3-1~3-5 (createdMode + M3 하이브리드) | 6 | D6 |
| PR#4 (LT-E4-CARD-AXIS) | LT Epic 4 Story 4-1~4-5 (Card→Axis 직접 매핑 + Coverage 이관) | 8 | D7 |
| PR#5 (AS-E3-ROLE-CATALOG-EXPAND) | AS Epic 3 Story 3-3~3-5 (planner/designer/problem-solver.json) | 3 | D3 |

**Total: 5 Epic PR · 총 30 SP** (M3 실 착지 ~24.5 SP 대비 ~22% 상승. Reviewer 5관점 세션 정식 진행 감안하면 도전적 목표 · rush 잔재 완전 종결).

**본 버전 제외 사유**:
- **LLM Adapter (Vertex AI)** — M6 이관 유지. v1 릴리스 시점엔 Static만.
- **Review 재편 (이슈 #24~#26)** — M5 이관 유지. Card가 선행되어야 Daily batch가 소비 가능.
- **AI Interactive Roadmap 진입** — M6~M7 이관 유지.
- **배포 라인 · 성능 baseline** — M7 이관 유지.
- **Roadmap 노드 재생성 API (이슈 #18)** — v2 이관 유지.
- **AI 비용 예산 cap (이슈 #20)** — v1은 관찰 지표만. Cap 자체 v2 유지.
- **AS Epic 4 LLM Adapter** — M6 이관 (Spring AI + Vertex AI Gemini Flash 2.5 배선).
- **AS Epic 5 SuggestionCascade + Rate Limit** — M6 이관 (Layer/Axis 엔드포인트 확장 포함).
- **AS Epic 6 관측성** — M7 이관 (`thirdtool.suggestion.*` 메트릭 배선).

---

## 진행 중 Product 잔여 인벤토리 (Before/After — M4 진입 시 vs 종료 후 예상)

**해결율 계산**: `M4 대상 / M4 진입 시 잔여 × 100%` — Product별 M4가 얼마나 소진하는지 즉시 파악.

| Product | 총 Story | M4 진입 시 완료 | M4 진입 시 잔여 | M4 대상 | **M4 종료 후 예상 잔여** | **해결율** |
| --- | --- | --- | --- | --- | --- | --- |
| 1. 인증 (`product-auth.md`) | 10 | 10 | 0 | — | 0 | ✅ 완주 (M1) |
| 2. User BC (`Product.md`) | 8 | 7 | 1 | — | 1 | 0% (M5~) |
| 3. **Learning Tower** (`product-learning-tower.md`, 6개 Epic + E3 재정의판) | **42** | 16 (E1·E2·E3-재정의판) | 26 | **5 Story (E4)** | **21** | **19%** ↑ |
| 4. **AI Suggestion** (`product-ai-suggestion.md`, 7 Epic 재편) | **34** | 11 (E1·E2·E3-부분·E5-부분·E7) | 23 | **3 Story (E3-3~3-5)** | **20** | **13%** ↑ |
| 5. AI Interactive Roadmap (`product-ai-interactive-roadmap.md`) | ~15 | 0 | ~15 | — | ~15 | 0% (M6~M7) |
| 6. **Card** (`product-card.md`, 3 Epic 재편) | **16** | 0 | 16 | **16 Story (E1·E2·E3 완주)** | **0** | **100%** ✅ 완주 |
| 7. Review (`product-review.md`) | 23 | 0 | 23 | — | 23 | 0% (M5) |
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
| **합계** | **~206** | **~60** | **~146** | **24 Story · 5 Epic PR · 30 SP** | **~122** | **16%** ↑ |

### 📊 M4 예상 성과 카드

- **총 해결 대상**: 24 Story (전체 잔여 ~146의 **16% 소진**)
- **완주 예상 SDD Product**:
  - `product-card.md` Epic 1·2·3 — **완주 100%** (Card BC 재편 종결)
  - `product-learning-tower.md` Epic 4 (Card→Axis 직접 매핑) — 완주
- **부분 진행 SDD Product**:
  - `product-ai-suggestion.md` Epic 3 Story 3-3~3-5 (planner/designer/problem-solver catalog 3종 · 4 role 완주)
- **M4 종료 후 남는 것** (다음 마일스톤 트라젝토리):
  - Learning Tower **21 Story 잔여** → M5 Epic 5 (Deck 폐기 · 5 Story) + M5 Epic 6 (Review 재편 · 5 Story) + Epic 3 문서화 잔여 (E3S3-5 AxisTopic 폐기) + Epic 3 원안 Story 3-1~3-4 (SUPERSEDED 잔재)
  - AI Suggestion **20 Story 잔여** → M6 Epic 4 (LLM Adapter · 5 Story) + M6~M7 Epic 5 (Cascade · Rate Limit · Controller 확장 · 3 Story) + M7 Epic 6 (관측성 · 4 Story) + Epic 1~2 신설 Story SUPERSEDED 잔재
  - Review — M5 진입 대기 (Card가 M4에 완주되므로 M5 Daily batch 소비 가능)
  - AI Interactive Roadmap — M6~M7 진입 대기
  - 배포·관측·로깅·부하 — M7 배포 재개 대기
  - v2 이관: 검색 · 미디어 · 캐시 · 알림 · Admin

---

## Epic PR 매트릭스 (본주 잡힌 양)

Epic 단위 PR 진행 원칙 (M3 유지):
- **한 Epic PR = 한 논리 단위 = 한 base 브랜치**. Squash merge 또는 rebase merge.
- Epic PR 안의 Story 커밋은 순차 누적 (`feat(scope): ...`).
- Reviewer 세션은 Epic PR 단위 (PR 하나에 5관점 병렬 발사).

### Epic PR #1 — `CARD-E1-MODE-REORG` (Mode enum 재편 · MODE_7D/14D/28D/60D)

**Base 브랜치**: `feat/032-card-e1-mode-reorganize`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-card.md`
- Epic: `# [Epic 1] Mode enum 재편 (MODE_7D/14D/28D/60D)` (line 340~554)
- Story 범위: `## [Story 1-1]` (line 379) ~ `## [Story 1-5]` (line 519)
- 상위 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-21-card-mode-reorganization.md`

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | CARD E1 S1-1 | `LearningMode` enum 재정의 (4개 값: MODE_7D/14D/28D/60D, `intervals` 필드만 유지) | 1 |
| 2 | CARD E1 S1-2 | `LearningModeMappingPolicy` threshold 재작성 (`raw_input_days → Mode` 매핑) | 1 |
| 3 | CARD E1 S1-3 | Flyway V23 `reorganize_learning_mode.sql` + 롤백 · 기존 데이터 마이그레이션 (`MODE_10D → 7D` 등) | 1 |
| 4 | CARD E1 S1-4 | `UserScheduleConfig.resolveOnFieldBudget()` 폐기 → `currentMode(userId)` 축소 시그니처 | 1 |
| 5 | CARD E1 S1-5 | `raw_input_days` 60일 상한 clamp 로직 + 정보성 응답 (`MODE_60D_CLAMPED`) | 1 |

**PR 종료 신호**:
- `POST /schedule-config { rawInputDays: 100 }` → 200 + `{ mode: "MODE_60D", clamped: true }`
- 기존 `MODE_10D` 유지 유저의 자동 마이그레이션 → `MODE_7D` (7일 인터벌 정합)
- `./gradlew test --tests "com.example.thirdtool.UserSchedule.*"` BUILD SUCCESSFUL

**병렬 진입 조건**: PR#5 (AS-E3-ROLE-CATALOG) 병행. 다른 Card PR과는 순차.

**Reviewer 세션**: 5관점 발사 (Domain / Architecture / API-Exception / Test / Sceptical).

### Epic PR #2 — `CARD-E2-BUDGET-ABOLISH` (OnFieldBudget 폐기 + ArchiveReason 재편)

**Base 브랜치**: `feat/033-card-e2-onfieldbudget-abolish`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-card.md`
- Epic: `# [Epic 2] OnFieldBudget 폐기 + ArchiveReason 재편` (line 555~776)
- Story 범위: `## [Story 2-1]` (line 593) ~ `## [Story 2-6]` (line 750)
- 상위 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-22-onfieldbudget-abolish.md`

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | CARD E2 S2-1 | `OnFieldBudget` VO 클래스 삭제 + 참조부 제거 (Card·Deck·Review·Application) | 1 |
| 2 | CARD E2 S2-2 | `CardExpiryPolicy`·`CardExpiryBatchService` 폐기 또는 축소 (batch 진입점 제거) | 1 |
| 3 | CARD E2 S2-3 | `ArchiveReason` enum 재정의 (`MANUAL / SCHEDULE_EXHAUSTED / MODE_DOWNGRADED`) + DB CHECK 재작성 + 기존 데이터 마이그레이션 | 2 |
| 4 | CARD E2 S2-4 | `ReviewCommandService.incrementViewAndHandleMaxView()` 재작성 (Budget 참조 제거) | 1 |
| 5 | CARD E2 S2-5 | Flyway V24 `abolish_onfieldbudget.sql` + 롤백 (컬럼·CHECK·기존 데이터 정합) | 2 |
| 6 | CARD E2 S2-6 | 테스트 코드 재작성 (기존 `MAX_VIEW`·`MAX_DURATION` → `SCHEDULE_EXHAUSTED`) | 1 |

**PR 종료 신호**:
- `Card` 도메인에 `budget: OnFieldBudget` 필드 검색 → 0건 (grep pass)
- `POST /reviews/{id}/view` 반복 시 카드가 SCHEDULE_EXHAUSTED로 자동 archive (Budget 대신 스케줄 소진 기준)
- 기존 archived 카드의 `archiveReason` 재매핑 (`MAX_VIEW → SCHEDULE_EXHAUSTED` 등) V24 마이그레이션 성공
- `./gradlew test` BUILD SUCCESSFUL

**의존**: PR#1 완주 (Mode enum이 SCHEDULE_EXHAUSTED 판정 기준을 제공하기 위해 stable 필요).

**Reviewer 세션**: 5관점 발사. **특히 Sceptical Reviewer가 "OnFieldBudget 폐기가 실제 정책 회귀를 안 만드는가"를 검증** (기존 카드 lifecycle 유지 여부).

### Epic PR #3 — `CARD-E3-CREATEDMODE` (Card `createdMode` + M3 하이브리드)

**Base 브랜치**: `feat/034-card-e3-created-mode`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-card.md`
- Epic: `# [Epic 3] Card createdMode 필드 + M3 하이브리드 도메인 메서드` (line 777~989)
- Story 범위: `## [Story 3-1]` (line 818) ~ `## [Story 3-5]` (line 956)
- 상위 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-23-card-created-mode-hybrid.md`

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | CARD E3 S3-1 | Flyway V25/V26/V27 `add_card_created_mode.sql` 3단계 (nullable → 백필 → NOT NULL) | 2 |
| 2 | CARD E3 S3-2 | `Card` 도메인 `createdMode` 필드 + `effectiveMaxDays`·`isDueOn(today)`·`hasScheduleExhausted()` 하이브리드 메서드 | 2 |
| 3 | CARD E3 S3-3 | `Card.returnToField(userCurrentMode, today)` fresh 재시작 로직 | 1 |
| 4 | CARD E3 S3-4 | `Card.create(...)` 팩토리에 `createdMode` 파라미터 + `CardCommandService.createCard()` 조율 | 0.5 |
| 5 | CARD E3 S3-5 | 도메인 단위 테스트 (M3 하이브리드 down/up 시나리오 명시 · `createdMode`가 `userCurrentMode`보다 큰/작은 경우) | 0.5 |

**PR 종료 신호**:
- 신규 카드 생성 시 `card.created_mode` 컬럼에 유저 현재 mode 저장 확인
- `card.effectiveMaxDays(userCurrentMode)` → `min(createdMode 인터벌, userCurrentMode 인터벌)` 반환 (down-shift 시)
- `card.returnToField(userCurrentMode, today)` 호출 시 `entered_field_at = today` + `created_mode = userCurrentMode` 갱신
- 기존 카드는 V26 백필에서 `createdMode = MODE_28D` (기본값) 부여

**의존**: PR#2 완주 (OnFieldBudget 폐기 완료 후에만 `createdMode` 하이브리드가 의미 있음).

**Reviewer 세션**: 5관점 발사.

### Epic PR #4 — `LT-E4-CARD-AXIS` (Card→Axis 직접 매핑 + Coverage 이관)

**Base 브랜치**: `feat/035-lt-e4-card-axis-direct-mapping`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-learning-tower.md`
- Epic: `# [Epic 4] Card→Axis 직접 매핑` (line 1604~1900)
- Story 범위: `## [Story 4-1]` (line 1644) ~ `## [Story 4-5]` (line 1831)
- 상위 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-07-card-axis-direct-mapping.md`

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | LT E4 S4-1 | Flyway V28 `card.axis_id BIGINT NULL` 컬럼 추가 (3-phase 1단계) | 1 |
| 2 | LT E4 S4-2 | Flyway V29 `topic_id → axis_id` 백필 (JOIN `axis_topic`) + 검증 SQL | 2 |
| 3 | LT E4 S4-3 | Flyway V30 `card.axis_id NOT NULL` 승격 + `card.topic_id` soft-deprecate + 도메인 `Card.axisId` 필드 | 2 |
| 4 | LT E4 S4-4 | Coverage 재계산 축 스코프 이관 (`CoverageRecalculator.recalculateByAxis(axisId)` 재작성) | 2 |
| 5 | LT E4 S4-5 | `Card.recordView()` axis_id 이벤트 발행 (기존 `topicId` → `axisId`로 변경 · Deck·Layer coverage 갱신 하위 소비자 대응) | 1 |

**PR 종료 신호**:
- `card.axis_id` NOT NULL 승격 성공 · 전량 카드 `axis_id` 값 존재 확인 (V29 백필 검증 SQL 통과)
- `card.topic_id` 컬럼 여전히 존재 (soft-deprecate · 다음 릴리스 DROP 예정)
- Coverage 재계산이 축 스코프로 트리거 → Layer coverage summary 재조정
- `LearningAxisCreatedEvent`·`CardViewedEvent`가 axisId 필드로 발행 (기존 topicId 필드 제거 or 병존 판정)
- `./gradlew test` BUILD SUCCESSFUL

**의존**: PR#3 완주 이후 진입 (Card `createdMode` 확정 · Card BC 재편 종료 후에만 `card.axis_id` 마이그레이션 안전).

**Reviewer 세션**: 5관점 발사. **특히 Test Reviewer가 "3-phase 마이그레이션 백필 SQL 실검증"을 검토** (M3의 Flyway 테스트 disabled 관행 재검토).

### Epic PR #5 — `AS-E3-ROLE-CATALOG-EXPAND` (planner/designer/problem-solver.json 3종)

**Base 브랜치**: `feat/036-as-e3-role-catalog-expand`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-ai-suggestion.md`
- Epic: `# [Epic 3] Role Catalog 확장 (4 role) + RoleDetector` (line 1193~1450)
- Story 범위: `## [Story 3-3]` (line 1352) ~ `## [Story 3-5]` (line 1418)
- 이미 완료: Story 3-1 (RoleDetector · M2) · Story 3-2 (backend-developer.json · M3)
- 상위 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-10-role-catalog-expansion.md`

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | AS E3 S3-3 | `planner.json` 카탈로그 신설 (기획자 role · 4 layer × ~3 chapters × selectionOutlines 2) | 1 |
| 2 | AS E3 S3-4 | `designer.json` 카탈로그 신설 (디자이너 role · 유사 규모) | 1 |
| 3 | AS E3 S3-5 | `problem-solver.json` 카탈로그 신설 (문제해결 role · 알고리즘/트러블슈팅 중심) | 1 |

**PR 종료 신호**:
- `POST /api/v1/suggestions/chapters-outline { concepts:["기획"], ... }` → `providerContext: "static:planner"` + 챕터 리스트 반환
- 각 role catalog에 6-Port 필드 (layers · axes · chapters · selectionOutlines) 모두 존재 · concept-spec.txt 카탈로그 6+5 정합 (eval.md §M3 baseline 판정 기준 적용)
- Sceptical Reviewer가 각 role 챕터가 "수렴된 판단 프레임"인지 판정 (도구 이름·특정 옵션 비교 침투 없음)
- `./gradlew test --tests "com.example.thirdtool.LearningFacade.infrastructure.suggestion.*"` BUILD SUCCESSFUL

**의존**: 독립. D1에 착수 가능 (Card BC PR과 병렬).

**Reviewer 세션**: 5관점 발사. **특히 Sceptical Reviewer가 catalog 콘텐츠 품질 판정** (`eval.md` §v1 관찰 지표 활용 · M3 backend-developer 정합 검증 절차 반복).

---

## Story 카테고리별 합계 (SDD Story 단위)

| 카테고리 | SDD Epic/Story | Story 수 | SP | 비중 |
| --- | --- | --- | --- | --- |
| Card Epic 1 (Mode enum 재편) | CARD E1 S1-1~S1-5 | 5 | 5 | 17% |
| Card Epic 2 (OnFieldBudget 폐기) | CARD E2 S2-1~S2-6 | 6 | 8 | 27% |
| Card Epic 3 (createdMode 하이브리드) | CARD E3 S3-1~S3-5 | 5 | 6 | 20% |
| LT Epic 4 (Card→Axis 직접 매핑) | LT E4 S4-1~S4-5 | 5 | 8 | 27% |
| AS Epic 3 (role catalog 3종 신설) | AS E3 S3-3~S3-5 | 3 | 3 | 10% |
| **합계** | | **24** | **30** | 100% |

**분배 근거**:
- M3 실 착지 ~24.5 SP 대비 ~22% 상승. Reviewer 5관점 세션이 정식 진행하는 대신 밀스톤 문서-코드 정합 지연(R1) · Flyway V번호 재할당(R2) 같은 조정 부담이 사라진 정합. Card BC 대재편(Mode·Budget·createdMode) 이 M4 주력.
- **Card 카테고리 64%** (E1+E2+E3) — 이슈 #21~#23 이관. Card lifecycle 전체 재편.
- **Learning Tower Epic 4 27%** (LT E4) — Card 재편의 후속 필수 작업. topic_id 폐기와 Coverage 재계산이 Card에 의존.
- **AI Suggestion Epic 3 10%** (AS E3) — Static Adapter role 다각화 · 상대적으로 경량 (JSON 카탈로그만).
- **5개 Epic PR**: 각 PR = SDD Epic 하위 Story 묶음. Reviewer 세션 5관점 필수.
- **SDD Story 규칙 준수**: 모든 M4 작업이 `product-card.md`·`product-learning-tower.md`·`product-ai-suggestion.md`의 명시된 Epic/Story에 대응. 신규 Story 추가 없음 (M3에 12건 신설했던 것과 대조 · 안정).

---

## 종료 신호 — "Card lifecycle 새 정책 + Card→Axis 직접 매핑 + AI role 4종 완주"

본주 종료 시점에 다음이 모두 성립해야 한다. (7 신호 중 6개 이상 → 0.0.4v 동결)

- [ ] **머지 신호**: Epic PR 5개 중 최소 4개 머지 (80%)
- [ ] **Mode enum 신호**: `LearningMode` 4개 값 (`MODE_7D/14D/28D/60D`) 확정 + 기존 `MODE_10D` 자동 마이그레이션 완료 + `raw_input_days` 60일 clamp 정합
- [ ] **OnFieldBudget 폐기 신호**: `OnFieldBudget` 클래스·필드 grep pass 0건 + `ArchiveReason` 3개 값 (`MANUAL / SCHEDULE_EXHAUSTED / MODE_DOWNGRADED`) 재정의 + 기존 archived 카드 재매핑
- [ ] **`createdMode` 신호**: 신규 카드 `createdMode` 저장 + `effectiveMaxDays` 하이브리드 로직 실동작 + `returnToField()` fresh 재시작 검증
- [ ] **Card→Axis 신호**: `card.axis_id NOT NULL` 승격 + 전량 백필 검증 성공 + Coverage 재계산 축 스코프 트리거 (Layer coverage 재조정)
- [ ] **AI role 4종 신호**: `POST /api/v1/suggestions/chapters-outline` 이 `providerContext: "static:planner"` · `"static:designer"` · `"static:problem-solver"` 각각 응답 발행 · Sceptical Reviewer가 각 catalog 콘텐츠 정합 판정 통과
- [ ] **테스트 신호**: 5개 Epic PR 각각 도메인 단위 · Repository Slice · Controller Slice 통과. `./gradlew test` BUILD SUCCESSFUL. 신규 테스트가 해피/엣지/예외 3구분 각각 1건 이상. **M3 R7 조치 (Roadmap/Selection PATCH partial 3 시나리오 보강) 포함 여부 확인**.

**미합격 처리**: 위 7 신호 중 5개 미만 성립 시 M4를 0.0.4v로 동결하지 않고 0.0.4.1v 패치 발행 → 다음 주 초까지 연장.

---

## 의존 chain

```
[선행: M3 착지]
LT E3 재정의판 (Roadmap/Selection 노드 first-class) ✅ 완료
AS E1·E2·E7 (6-Port · Static Adapter · concept-spec) ✅ 완료

[D1: 병렬 착수]
PR#1 (CARD-E1-MODE-REORG)  ──►  PR#5 (AS-E3-ROLE-CATALOG · 독립)
  CARD E1 S1-1 ~ S1-5             AS E3 S3-3 ~ S3-5
     │                                │
     ▼                                ▼
PR#2 (CARD-E2-BUDGET-ABOLISH)      머지 완료
  CARD E2 S2-1 ~ S2-6
     │
     ▼
PR#3 (CARD-E3-CREATEDMODE)
  CARD E3 S3-1 ~ S3-5
     │
     ▼
PR#4 (LT-E4-CARD-AXIS)
  LT E4 S4-1 ~ S4-5
     │
     ▼
  Card BC 완주 · Card→Axis 직접 매핑
```

**병렬 진입 가능 묶음**:
- **A** (D1 · 07-15 Wed): PR#1 (CARD E1 S1-1 착수) + PR#5 (AS E3 S3-3 착수 · planner.json) 동시 시작
- **B** (D2 · 07-16 Thu): PR#1 CARD E1 S1-2/S1-3 진행 + PR#5 S3-4/S3-5 (designer·problem-solver) 완주 준비
- **C** (D3 · 07-17 Fri): PR#1 CARD E1 S1-4/S1-5 완주 → PR#1 머지 · PR#5 머지 · PR#2 착수 (CARD E2 S2-1 OnFieldBudget 삭제)
- **D** (D4 · 07-18 Sat): PR#2 CARD E2 S2-2/S2-3 (ArchiveReason 재편) 진행
- **E** (D5 · 07-19 Sun): PR#2 CARD E2 S2-4/S2-5/S2-6 완주 → PR#2 머지 · PR#3 착수 (CARD E3 S3-1 Flyway V25~V27)
- **F** (D6 · 07-20 Mon): PR#3 CARD E3 S3-2/S3-3/S3-4/S3-5 완주 → PR#3 머지 · PR#4 착수 (LT E4 S4-1 card.axis_id NULL)
- **G** (D7 · 07-21 Tue): PR#4 LT E4 S4-2/S4-3/S4-4/S4-5 완주 → PR#4 머지 (Card→Axis 직접 매핑 · Coverage 이관) · 통합 로컬 검증 · `outcome.md`·`review.md`·`eval.md` 골격 · 0.0.4v 동결 판정

**직렬 (M4 합격선까지)**: PR#5 (D1-D3 독립) + PR#1 (D1-D3) → PR#2 (D3-D5) → PR#3 (D5-D6) → PR#4 (D6-D7). 순차 chain이 D7에 종료.

---

## 작업 일정 (Epic PR 단위 체크리스트)

D1 = 2026-07-15 (Wed). 종료 D7 = 2026-07-21 (Tue). 7일 안에 5 Epic PR.

| 일 | 날짜 | 잡힌 작업 (Epic PR 진행) — SDD Story 기준 |
| --- | --- | --- |
| D1 (수) | 07-15 | PR#1 착수 (**CARD E1 S1-1** `LearningMode` 4개 값 재정의) + PR#5 착수 (**AS E3 S3-3** `planner.json` 카탈로그) |
| D2 (목) | 07-16 | PR#1 **CARD E1 S1-2** (`LearningModeMappingPolicy` threshold) + **S1-3** (Flyway V23) + PR#5 **AS E3 S3-4/S3-5** (designer · problem-solver) |
| D3 (금) | 07-17 | PR#1 **CARD E1 S1-4** (`resolveOnFieldBudget` 폐기) + **S1-5** (60일 clamp) 완주 → **PR#1 머지 + Reviewer 세션** · PR#5 완주 → **PR#5 머지 + Reviewer 세션 (Sceptical 강조 · role 3종 콘텐츠 판정)** · PR#2 착수 (**CARD E2 S2-1** OnFieldBudget 삭제) |
| D4 (토) | 07-18 | PR#2 **CARD E2 S2-2** (CardExpiryPolicy 폐기) + **S2-3** (ArchiveReason 재정의 + DB CHECK 재작성 + 기존 데이터 마이그레이션) |
| D5 (일) | 07-19 | PR#2 **CARD E2 S2-4** (ReviewCommandService 재작성) + **S2-5** (Flyway V24) + **S2-6** (테스트 재작성) 완주 → **PR#2 머지 + Reviewer 세션 (Sceptical 강조 · Budget 폐기 정책 회귀 검증)** · PR#3 착수 (**CARD E3 S3-1** Flyway V25/V26/V27 3단계) |
| D6 (월) | 07-20 | PR#3 **CARD E3 S3-2** (createdMode 필드 + M3 하이브리드 메서드) + **S3-3** (returnToField fresh 재시작) + **S3-4** (Card.create 팩토리 확장) + **S3-5** (도메인 단위 테스트) 완주 → **PR#3 머지 + Reviewer 세션** · PR#4 착수 (**LT E4 S4-1** V28 card.axis_id NULL) |
| D7 (화) | 07-21 | PR#4 **LT E4 S4-2** (V29 백필) + **S4-3** (V30 NOT NULL + topic_id soft-deprecate) + **S4-4** (Coverage 재계산 축 스코프) + **S4-5** (`Card.recordView()` axis_id 이벤트) 완주 → **PR#4 머지 + Reviewer 세션 (Test 강조 · 3-phase 백필 실검증)** · 통합 로컬 검증 3시나리오 + `outcome.md` · `review.md` · `eval.md` 골격 · 0.0.4v 동결 판정 |

**Reviewer 세션 규칙**: 각 Epic PR 머지 직전 5관점 병렬 발사 (`.claude/rules/review.md` §3~§7). 사용자 확인 후 머지. M3 재개 세션과 동일 강도 유지.

**Flyway V 버전 순서 관리** (M3에서 V20~V22 소모):
- V23: `reorganize_learning_mode.sql` (PR#1)
- V24: `abolish_onfieldbudget.sql` (PR#2)
- V25: `add_card_created_mode.sql` 1단계 nullable (PR#3)
- V26: `add_card_created_mode.sql` 2단계 백필 (PR#3)
- V27: `add_card_created_mode.sql` 3단계 NOT NULL (PR#3)
- V28: `card_axis_id.sql` 1단계 NULL 컬럼 (PR#4)
- V29: `card_axis_id.sql` 2단계 백필 (PR#4)
- V30: `card_axis_id.sql` 3단계 NOT NULL + topic_id soft-deprecate (PR#4)

D1 시작 전 V 버전 할당표를 사용자 확인 필수 (M3 R2 조치 반영). 충돌 방지.

---

## 리스크와 관찰 포인트

| 영역 | 리스크 | 관찰 포인트·완화 |
| --- | --- | --- |
| Card BC 대재편 (Mode·Budget·createdMode) | 3 Epic PR 순차 · 도메인 변경 폭 대. Mode enum 재정의 + Budget VO 삭제 + createdMode 신설이 모두 Card 도메인 안에서 얽힘 | 각 Epic PR별 도메인 단위 테스트 · 기존 테스트 회귀 감지 · Reviewer Sceptical이 정책 회귀 판정 |
| Flyway 3-phase 마이그레이션 (V25·V26·V27 · V28·V29·V30) | 8 V버전 신설 · 3-phase가 2 쌍 얽힘 · 백필 SQL 실패 시 rollback 필수 | M3 R2 조치 (사전 V번호 할당표 확인) 유지 · 각 V버전 R페어 필수 · dev 환경에서 실제 실행 검증은 M7 이후지만 M4에서 로컬 H2 부팅 검증 (Reviewer Test 관점) |
| ArchiveReason 재정의 · 기존 데이터 마이그레이션 | `MAX_VIEW`·`MAX_DURATION` → `SCHEDULE_EXHAUSTED` 등 재매핑 · CHECK 제약 재작성 실패 시 기존 카드 참조 소실 | V24 마이그레이션 안에 명시적 SELECT 통계 로깅 + 롤백 SQL 준비 |
| Card→Axis 직접 매핑 3-phase | LT E4 S4-1/S4-2/S4-3 (V28~V30). 백필 SQL의 `card ← topic ← axis` JOIN 실패 시 카드 axis_id NULL 잔재 | V29 백필 검증 SQL을 마이그레이션 주석에 명시 · Reviewer Test 관점이 3-phase 순서 실검증 |
| Coverage 재계산 axis 스코프 이관 | 기존 topic 스코프 트리거가 axis 스코프로 변경. Layer coverage summary 계산 로직 회귀 위험 | `CoverageRecalculator` 재작성 후 통합 테스트로 기존 축 커버리지 결과 비교 |
| AI role catalog 3종 콘텐츠 품질 | planner/designer/problem-solver JSON 3개의 concept-spec 정합 판정. Sceptical Reviewer 판정이 catalog 콘텐츠 품질 결정 | eval.md §v1 관찰 지표 활용 · M3 backend-developer.json 정합 판정 절차 반복 · concept-spec 카탈로그 6+5 태깅 강제 |
| PATCH JSON null 판별 반복 지적 (M3 R3) | Card `updateName` · `updateDescription` 등 PATCH 있는 경우 동일 문제 재발 위험 | M4 착수 전 JsonNullable 도입 결정 · 각 PR PATCH 신설 시 첫 지적 시점에 즉시 조치 (M3 재발 방지 프로토콜 준수) |
| Reviewer 세션 병목 (M3 실측) | 5관점 Reviewer 세션이 각 PR마다 필요. 5 PR × 5관점 = 25 reviewer 발사 · M3 실측 PR당 40~90초 | 병렬 발사 유지 · Sceptical Reviewer가 반복 catalog 판정하는 경우 이전 판정 참조 문서화 (eval.md §M4 baseline 확장) |
| DOMAIN.md Aggregate 4단계 계층 문서화 (M3 R6) | 미조치 상태. M4에 Card BC 재편이 얽히면서 도메인 문서 정합 요구 증가 | M4 D5~D7 여유 시점에 DOMAIN.md §LearningAxis 갱신 (Facade → Layer → Axis → (Topic|RoadmapNode|Selection) → SelectionNode 4단계) |
| Roadmap/Selection PATCH partial 테스트 (M3 R7) | 미조치 상태. M4 착수 전 backlog 해소 여부 결정 필요 | M4 D1~D2 여유 시점에 별도 커밋으로 PR#4 브랜치 안에 포함 검토 |
| 스코프 확장 압박 | 30 SP 목표. Card BC 대재편이 예상보다 얽힐 위험 | D3 진행률 (PR#1·PR#5 머지) 확인 → 미달 시 PR#4 축소 (S4-3 NOT NULL 승격만 M5 이관 검토) |

---

## 다음 마일스톤 (M5 / 0.0.5v) 후보

본주 결과를 보고 결정하지만, 릴리스 문서에 잠긴 트라젝토리 그대로:

**M5 / 0.0.5v (07-22 ~ 07-28) — Deck 폐기 + Review 재편**
- **LT Epic 5 (Deck 폐기 · 이슈 #13 이관)** — `deck` 테이블을 `_archived_deck`로 RENAME · Deck BC의 자체 책임 (`progressStatus`, `mode`, `lastAccessed`, `learningMaterialId`, `onLibrary`, `publishedAt`) 을 Axis로 이전
- **LT Epic 6 (Review 재편 · 이슈 #14 이관)** — `ReviewSession.scope: ReviewScope` enum (`AXIS` / `LAYER`) 도입 · `findAllByAxisId(axisId)` · `findAllByLayerId(layerId)` Repository 쿼리 신설 · `/layers/{id}/review-sessions` 엔드포인트
- **Review 재편 (이슈 #24~#26 이관)** — DailyBatch 재작성 · Cross-layer 세션 재편 · `product-review.md` Epic 1~3 진입
- **AS Epic 3 완주 확인** — role catalog 4종 안정화 (M4 완주분) · role 감지 오탐 통계 (M3 실측 대비)

**Epic PR 예상 4~5건** (LT Epic 5 · LT Epic 6 · Review Epic 1~3 · AS Epic 3 안정화).

---

## Product 상태 전환 신호 (M4 종료 시)

- `in-progress/product-card.md` — **Epic 1·2·3 완주** 표기 (Card BC 재편 종결 · 다음은 재편 안정화 및 UX 반영)
- `in-progress/product-learning-tower.md` — **Epic 4 완주** 표기 (Card→Axis 직접 매핑). Epic 5(Deck 폐기) M5 이관 · Epic 6(Review 재편) M5 이관 유지
- `in-progress/product-ai-suggestion.md` — **Epic 3 완주** 표기 (Static Adapter 4 role catalog 안정). Epic 4 (LLM Adapter) M6 이관 유지 · Epic 5 (Cascade · Rate Limit) M6~M7 이관 유지
- `in-progress/product-ai-interactive-roadmap.md` — 진입 X, 상태 유지 (M6~M7 이관)
- `in-progress/product-review.md` — 진입 X, 상태 유지 (M5 진입 대기 · Card 재편이 M4에 완주되므로 M5 Daily batch 소비 가능)
- `fix/brainstorming/version/0.0.2v/issue-21/22/23` → **resolved** (Card BC 재편 M4 이관 완료)
- `fix/brainstorming/version/0.0.2v/issue-07` → **resolved** (Card→Axis 직접 매핑 M4 이관 완료)
- `fix/brainstorming/version/0.0.2v/issue-10` → **resolved** (role catalog 4종 완주)
- `fix/brainstorming/version/0.0.2v/issue-13/14` → M5 이관 유지 (Deck 폐기 · Review 재편)
- `fix/brainstorming/version/0.0.2v/issue-24/25/26` → M5 이관 유지 (Review 재편)
- `fix/brainstorming/version/0.0.2v/issue-18/20` → v2 · M7 이관 유지 (Roadmap 재생성 · 예산 cap)

---

## brainstorming 트리거

본 M4 완료 후 `workflow/task/pes/brainstorming/0.0.5v/` 신설:
- Deck 폐기 UX 시나리오 (`brainstorming/0.0.5v/deck-abolish-migration.md`) — `/decks/*` 엔드포인트 → `/axes/*` 매핑 사용자 안내 UX
- Review scope 확장 UX (`brainstorming/0.0.5v/review-scope-selection.md`) — `AXIS` / `LAYER` 스코프 선택 화면 · 진행률 파생 표시
- Card `createdMode` 하이브리드 사용자 UX (`brainstorming/0.0.5v/card-mode-shift-anouncement.md`) — 사용자가 mode 다운/업 시점의 카드 예상 archive 사전 안내
- AI role 4종 완주 후 감지 오탐 튜닝 (`brainstorming/0.0.5v/role-detection-tuning.md`) — M3~M4 실측 데이터 반영
- Reviewer 5관점 세션 catalog 정합 판정 표준화 (`brainstorming/0.0.5v/eval-sceptical-catalog-review.md`) — eval.md §Sceptical AI 응답 리뷰 세션 별도 프로토콜 착수

---

## 참고

- 잔여 Story 인벤토리 출처: `workflow/task/pes/workspectrum/sdd/in-progress/` 19개 Product 파일
- M2 pivot 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-21 ~ #23` (Card) · `issue-07` (Card→Axis) · `issue-10` (role catalog)
- 마일스톤 패키지 의도: `workflow/task/milestones/references/001.md`
- FE 대응 마일스톤: `workflow/task/pes/fe/fe-milestones/version/0.0.4v/milestone.md` (동일 주간)
- 이전 마일스톤: `workflow/task/milestones/version/0.0.3v/milestone.md` — M3 원안 · `../0.0.3v/outcome.md` · `../0.0.3v/review.md` — M3 결과 · `../0.0.3v/eval.md` — AI 응답 품질 평가 프레임 초석
- **첫 릴리스 계획**: `workflow/task/milestones/release/version/0.0.1v/release.md` — 0.1.0v (~2026-08-19) 스코프 · M3~M8 트라젝토리
- 본 버전의 산출물 7종: `milestone.md`(본 문서) · `infra.md`(스킵) · `performance.md`(스킵) · `outcome.md` · `cost.md` · `review.md` · `eval.md`
- 양식 진화: 본 milestone은 0.0.3v milestone.md 양식 그대로 답습 (Epic PR 매트릭스 · 진행 중 Product 잔여 인벤토리 · 종료 신호 · 의존 chain · 리스크 · 다음 마일스톤 후보 · Product 상태 전환 · brainstorming 트리거). **eval.md는 M3에 신설되었으므로 본 M4가 첫 확장 대상** — role 3종 catalog 콘텐츠 정합 판정에 M3 baseline 절차 반복 적용.
- **주요 SDD 참조 비율**:
  - `product-card.md` **~60%** (Epic 1·2·3 · Story 1-1~3-5 · 16 Story · SP 19)
  - `product-learning-tower.md` **~25%** (Epic 4 Story 4-1~4-5 · 5 Story · SP 8)
  - `product-ai-suggestion.md` **~15%** (Epic 3 Story 3-3~3-5 · 3 Story · SP 3)
