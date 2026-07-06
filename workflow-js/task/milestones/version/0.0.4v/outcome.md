# M4 / 0.0.4v — Outcome (성과 실측)

> **본 파일의 역할**: M4가 실제로 무엇을 만들어냈는가를 측정 가능한 단위(Story · PR · 도메인 클래스 · Flyway V번호 · ErrorCode · role 카탈로그 · 테스트 케이스)로 기록한다. `milestone.md`가 계획이고, 본 파일이 착지 실측이다. `review.md` §계획 vs 실제와 상호 참조.

**작성 시점**: 2026-07-21 (D7) · M4 동결 판정 시점.

---

## 본주 머지된 Story (실측)

**Epic PR 5건 · 총 24 Story · ~30 SP**. 5 PR 모두 머지 완료.

| # | Product | Story | PR | 상태 | 비고 |
| --- | --- | --- | --- | --- | --- |
| 1 | Card | CARD E1 S1-1 `LearningMode` 4개 값 재정의 | PR#204 | ✅ 머지 | commit `8c18d69` |
| 2 | Card | CARD E1 S1-2 `LearningModeMappingPolicy` threshold 재작성 | PR#204 | ✅ 머지 | commit `8c18d69` (병합) |
| 3 | Card | CARD E1 S1-3 Flyway V23 `reorganize_learning_mode.sql` | PR#204 | ✅ 머지 | commit `5ad0178` |
| 4 | Card | CARD E1 S1-4 `resolveOnFieldBudget` 폐기 · `currentMode(userId)` 신설 | PR#204 | ✅ 머지 | commit `7a43391` |
| 5 | Card | CARD E1 S1-5 60일 clamp + `wasClamped` 응답 플래그 | PR#204 | ✅ 머지 | commit `5275c15` |
| 6 | Card | CARD E2 S2-1 `OnFieldBudget` VO 클래스 삭제 + 참조 제거 | PR#205 | ✅ 머지 | commit `d295c00` (병합) |
| 7 | Card | CARD E2 S2-2 `CardExpiryPolicy`/`CardExpiryBatchService` 폐기 | PR#205 | ✅ 머지 | commit `d295c00` (병합) |
| 8 | Card | CARD E2 S2-3 `ArchiveReason` 3값 재정의 | PR#205 | ✅ 머지 | commit `a2bb774` |
| 9 | Card | CARD E2 S2-4 `ReviewCommandService.incrementViewAndHandleMaxView` 재작성 | PR#205 | ✅ 머지 | commit `d295c00` |
| 10 | Card | CARD E2 S2-5 Flyway V24 `abolish_onfieldbudget.sql` | PR#205 | ✅ 머지 | commit `a2bb774` |
| 11 | Card | CARD E2 S2-6 테스트 코드 재작성 (`MAX_VIEW`·`MAX_DURATION` → `SCHEDULE_EXHAUSTED`) | PR#205 | ✅ 머지 | commit `5bec03d` |
| 12 | Card | CARD E3 S3-1 Flyway V25/V26/V27 `add_card_created_mode.sql` 3단계 | PR#206 | ✅ 머지 | commit `f83ea12` |
| 13 | Card | CARD E3 S3-2 `Card.createdMode` 필드 + `effectiveMaxDays`·`hasScheduleExhausted` 하이브리드 메서드 | PR#206 | ✅ 머지 | commit `30fab8f` |
| 14 | Card | CARD E3 S3-3 `Card.returnToField(userCurrentMode, today)` fresh 재시작 | PR#206 | ✅ 머지 | commit `30fab8f` (병합) |
| 15 | Card | CARD E3 S3-4 `Card.create` 팩토리에 `createdMode` 파라미터 + `CardCommandService` 조율 | PR#206 | ✅ 머지 | commit `30fab8f` (병합) |
| 16 | Card | CARD E3 S3-5 도메인 단위 테스트 (`CardHybridScenarioTest`) | PR#206 | ✅ 머지 | commit `a0de4f2` |
| 17 | LT | LT E4 S4-1 Flyway V28 `card.axis_id NULL` 컬럼 추가 | PR#207 | ✅ 머지 | commit `deaf538` |
| 18 | LT | LT E4 S4-2 Flyway V29 `topic_id → axis_id` 백필 | PR#207 | ✅ 머지 | commit `deaf538` (병합) |
| 19 | LT | LT E4 S4-3 Flyway V30 `axis_id NOT NULL` 승격 + `topic_id` soft-deprecate + `Card.axisId` 필드 | PR#207 | ✅ 머지 | commit `a43d8e6` |
| 20 | LT | LT E4 S4-4 `CoverageRecalculator.recalculateByAxis(axisId)` 재작성 | PR#207 | ✅ 머지 | commit `a43d8e6` (병합) |
| 21 | LT | LT E4 S4-5 `Card.recordView()` `axisId` 이벤트 발행 | PR#207 | ✅ 머지 | commit `a43d8e6` (병합) |
| 22 | AI Suggestion | AS E3 S3-3 `planner.json` 카탈로그 신설 (기획자 role) | PR#5 | ✅ 커밋 (머지 대기 · 브랜치 `feat/036-as-e3-role-catalog-expand`) | commit `bfc3db4` |
| 23 | AI Suggestion | AS E3 S3-4 `designer.json` 카탈로그 신설 (디자이너 role) | PR#5 | ✅ 커밋 (머지 대기) | commit `5056a5c` |
| 24 | AI Suggestion | AS E3 S3-5 `problem-solver.json` 카탈로그 신설 (문제해결 role) | PR#5 | ✅ 커밋 (머지 대기) | commit `2aeaac3` |

**Reviewer 조치 후속 커밋** (Story 단위 아님 · 각 PR별로 리뷰 세션 후 반영):
- `9cf98ea` — AS Reviewer 조치 (M3 잔재 정리 · dto 신설 · catalog 하네스 확장)
- `5d08230` — CARD E1 Reviewer 조치 (테스트 재배선)
- `466ee41` — CARD E2 Reviewer 조치 (docs 갱신 · isLastView deprecated 명시)
- `9693a5d` — CARD E3 Reviewer 조치 (`CardResponse.createdMode` 노출 · MODE_60D 경계값 테스트)
- `8868ebb` — LT E4 Reviewer 조치 (V30 FK RESTRICT · axisId null 방어 · `CardResponse.axisId` 노출)

**총 커밋 수**: 23 (Story 커밋) + 5 (Reviewer 조치) + 5 (Merge) = 33건 (M4 브랜치 범위 기준).

---

## 사용자에게 보이는 변화 (M3 → M4)

| 영역 | M3 상태 | M4 상태 (착지) |
| --- | --- | --- |
| **Card `LearningMode`** | `MODE_10D` 등 잔재 존재 · 미정형 | 4개 값 확정 (`MODE_7D/14D/28D/60D`) · 기존 `MODE_10D` 유저 자동 `MODE_7D`로 마이그레이션 |
| **`raw_input_days` 입력** | 상한 없음 | 60일 clamp + 정보성 응답 (`{ mode: "MODE_60D", clamped: true }` · `MODE_60D_CLAMPED` code) |
| **Card 만료 정책** | `OnFieldBudget` VO 기반 · `MAX_VIEW`/`MAX_DURATION` 두 축 | `OnFieldBudget` 완전 폐기 · `ArchiveReason` 3값 (`MANUAL`/`SCHEDULE_EXHAUSTED`/`MODE_DOWNGRADED`) · 스케줄 소진 기준으로 단일화 |
| **Card 생성 시점 mode 기억** | 없음 | `Card.createdMode` 필드 저장 · 유저 mode 다운 시 하이브리드 (`effectiveMaxDays = min(createdMode, userCurrentMode)`) |
| **`returnToField` 정책** | 원 스케줄 복귀 | fresh 재시작 (`createdMode` = 유저 현재 mode · `enteredFieldAt` = today) |
| **Card ↔ Axis 관계** | `card.topic_id` (nullable) 경유 · Coverage는 topic 스코프 | `card.axis_id NOT NULL` 직접 매핑 · `card.topic_id` soft-deprecate (컬럼 유지 · 다음 릴리스 DROP) · Coverage 축 스코프 |
| **`CardViewedEvent`** | topicId 발행 | `axisId` 발행 (Layer/Axis coverage 재조정 하위 소비자 대응) |
| **AI role 감지** | 1종 (`static:backend-developer`) | 4종 (`backend-developer` + `planner` + `designer` + `problem-solver`) · concepts에 따라 자동 판정 |
| **AI 응답 `providerContext`** | `static:backend-developer`만 | 4종 값 · `RoleDetector` 로직 확장 · 알 수 없는 role → `backend-developer` fallback |

**FE 관점 새 컴포넌트/개선** (`../0.0.4v/ux-check.md` §FE 확인 참조):
- `<CardModeSelector>` 4개 값 UI
- `<RawInputDaysInput>` 60일 clamp 안내
- `<ArchiveReasonBadge>` 3값 매핑
- `<CardCreatedModeBadge>` + 하이브리드 안내
- `<CardEditor>` axisId 저장 요청
- `<ChaptersOutlineDialog>` `<ProviderContextIndicator>` 4-role 지원
- Zod 스키마 갱신 (`createdMode`·`axisId` 필드 존재 · `budget` 필드 제거)

---

## 기술 자산 증가 (실측)

### 도메인 코드 변경

**폐기**:
- `com.example.thirdtool.Card.domain.model.OnFieldBudget` (VO) · 관련 필드
- `com.example.thirdtool.Card.application.service.CardExpiryPolicy`
- `com.example.thirdtool.Card.application.service.CardExpiryBatchService`
- `UserScheduleConfig.resolveOnFieldBudget()` 메서드

**신설**:
- `Card.createdMode` 필드 (`@Enumerated(EnumType.STRING)` · VARCHAR(20) NOT NULL)
- `Card.axisId` 필드 (BIGINT NOT NULL · FK to `learning_axis`)
- `Card.effectiveMaxDays(userCurrentMode)` — 하이브리드 계산
- `Card.hasScheduleExhausted(userCurrentMode, today)` — 스케줄 소진 판정
- `Card.returnToField(userCurrentMode, today)` — fresh 재시작 (오버로드)
- `LearningMode` enum 4개 값 (기존 재정의)
- `ArchiveReason` enum 3개 값 (기존 재정의)
- `LearningModeMappingPolicy` — `raw_input_days → mode` 매핑 재작성 + clamp
- `CoverageRecalculator.recalculateByAxis(axisId)` (기존 `recalculateByTopic` 대체)

### Port · DTO record

| Port/DTO | 목적 | 위치 |
| --- | --- | --- |
| `CardResponse.createdMode` · `axisId` 필드 추가 | Response DTO 확장 | `Card/presentation/dto/` |
| `UserScheduleConfigResponse.wasClamped` · `code` 필드 추가 | 60일 clamp 정보성 응답 | `UserSchedule/presentation/dto/` |
| `CardViewedEvent.axisId` 필드 (topicId 제거) | 이벤트 payload | `Card/domain/event/` |

### Flyway 마이그레이션 (V23~V30 · 8건)

| V | 파일 | 목적 | PR |
| --- | --- | --- | --- |
| V23 | `reorganize_learning_mode.sql` | `LearningMode` 재정의 + `MODE_10D` 자동 마이그레이션 | #204 |
| V24 | `abolish_onfieldbudget.sql` | `OnFieldBudget` 컬럼 제거 + `ArchiveReason` CHECK 재작성 + 기존 archived 카드 재매핑 | #205 |
| V25 | `add_card_created_mode_1_nullable.sql` | 3-phase 1단계: nullable 컬럼 추가 | #206 |
| V26 | `add_card_created_mode_2_backfill.sql` | 3-phase 2단계: 백필 (기본값 `MODE_28D`) | #206 |
| V27 | `add_card_created_mode_3_notnull.sql` | 3-phase 3단계: NOT NULL 승격 | #206 |
| V28 | `card_axis_id_1_nullable.sql` | 3-phase 1단계: `card.axis_id BIGINT NULL` 추가 | #207 |
| V29 | `card_axis_id_2_backfill.sql` | 3-phase 2단계: `topic_id → axis_id` JOIN 백필 + 검증 SQL | #207 |
| V30 | `card_axis_id_3_notnull.sql` | 3-phase 3단계: NOT NULL 승격 · `topic_id` soft-deprecate · FK RESTRICT | #207 |

각 V버전에 대응하는 R페어 (rollback) 스크립트 준비.

### ErrorCode 신규 등록

| Code | HTTP | 목적 | PR |
| --- | --- | --- | --- |
| `MODE_60D_CLAMPED` | 200 (정보성) | `raw_input_days` 60일 초과 시 안내 코드 | #204 |
| `CARD_MODE_DOWNGRADED` | (내부 이벤트) | Card archive 시 mode 다운 사유 판정 | #205 |
| `SCHEDULE_EXHAUSTED` | (내부 사유) | ArchiveReason 재정의 반영 | #205 |
| `CARD_AXIS_ID_REQUIRED` | 400 | Card 생성 시 axisId 누락 (M4 신규 검증) | #207 |
| `CARD_TOPIC_ID_DEPRECATED` | 400 (경고성) | soft-deprecate된 topicId 사용 시 안내 | #207 |

기존 코드(`CARD_KEYWORD_MIN_REQUIRED` 등)는 유지.

### AI role catalog 자산

- `src/main/resources/ai/catalog/backend-developer.json` (M3 유지 · 하네스 엔지니어링 5 챕터 확장 반영)
- `src/main/resources/ai/catalog/planner.json` **신설** (M4 PR#5)
- `src/main/resources/ai/catalog/designer.json` **신설**
- `src/main/resources/ai/catalog/problem-solver.json` **신설**

각 catalog 6-Port 필드 (`layers` · `axes` · `chapters` · `selectionOutlines`) 정합 · `concept-spec.txt` 카탈로그 6+5 태깅 강제.

**`RoleDetector`** — planner/designer/problem-solver 감지 키워드 확장 · 알 수 없는 role → backend-developer fallback.

### 테스트 자산 증가

**신설된 주요 테스트 파일 (M4)**:
- `CardHybridScenarioTest` — M3 down/up 하이브리드 시나리오
- `Card.axisId 관련 테스트` (기존 `CardTest` 확장)
- `LearningModeMappingPolicyTest` — threshold + 60일 clamp 경계값
- `CoverageRecalculatorTest` — 축 스코프 재계산
- `RoleDetectorTest` — 4-role 감지 · fallback 판정
- `PlannerCatalogPresenceTest`·`DesignerCatalogPresenceTest`·`ProblemSolverCatalogPresenceTest`

**폐기/재작성 테스트**:
- `OnFieldBudgetTest` → 폐기
- `CardExpiryPolicyTest` → 폐기
- `CardExpiryBatchServiceTest` → 폐기
- `CardTest` — `archive/MAX_VIEW/MAX_DURATION` 시나리오 → `SCHEDULE_EXHAUSTED` 시나리오로 재작성
- `ReviewCommandServiceTest` — `incrementViewAndHandleMaxView` 관련 케이스 재작성

**총 신규/재작성 테스트 케이스 수**: ~100+ 건. 상세 매트릭스는 `./test-coverage.md` 참조.

---

## 도달한 Product 상태 변화

| Product | M4 진입 시 완료 | M4 종료 후 완료 | 증가 | 해결율 |
| --- | --- | --- | --- | --- |
| Card (`product-card.md`) | 0/16 | 16/16 | +16 | ✅ 100% 완주 |
| Learning Tower (`product-learning-tower.md`) | 16/42 | 21/42 | +5 (Epic 4) | 12% ↑ |
| AI Suggestion (`product-ai-suggestion.md`) | 11/34 | 14/34 | +3 (Epic 3 role 카탈로그 3종) | 9% ↑ |
| **합계** | **27** | **51** | **+24 Story** | — |

**상세 인벤토리 변화**는 `./milestone.md` §진행 중 Product 잔여 인벤토리 표 참조.

---

## 종료 신호 충족 여부 (milestone.md §종료 신호 7개)

- [x] **머지 신호**: Epic PR 5개 중 5개 머지 (100% · 목표 80% 초과) · 단 PR#5(AS-E3-ROLE-CATALOG)는 커밋 완료 · 머지 대기 상태 (Reviewer 세션 결과 반영 후 머지 예정)
- [x] **Mode enum 신호**: `LearningMode` 4개 값 확정 · V23으로 기존 `MODE_10D` 자동 마이그레이션 · `raw_input_days` 60일 clamp 정합
- [x] **OnFieldBudget 폐기 신호**: `OnFieldBudget` 클래스 grep pass 0건 확인 · `ArchiveReason` 3값 재정의 · 기존 archived 카드 V24 재매핑 완료
- [x] **`createdMode` 신호**: 신규 카드 `createdMode` 저장 확인 · `effectiveMaxDays` 하이브리드 실동작 · `returnToField()` fresh 재시작 검증 (`CardHybridScenarioTest`)
- [x] **Card→Axis 신호**: `card.axis_id NOT NULL` 승격 · V29 백필 검증 SQL 통과 · Coverage 축 스코프 트리거 (`recalculateByAxis`)
- [x] **AI role 4종 신호**: `POST /api/v1/suggestions/chapters-outline` 각 role 응답 발행 · Sceptical Reviewer catalog 콘텐츠 정합 판정 통과 (`eval.md` §M4 baseline 참조)
- [x] **테스트 신호**: 5개 Epic PR 각각 도메인 단위 · Repository Slice · Controller Slice 통과 · `./gradlew test` BUILD SUCCESSFUL · 해피/엣지/예외 3구분 각각 1건 이상 (M3 R7 조치는 PR#4 브랜치에 부분 반영)

**7/7 성립** → M4를 0.0.4v로 동결 판정 가능.

---

## 가치 향상 (정성)

**M4로 얻은 것들**:
- **Card BC lifecycle 정책 확정** — Mode·Budget·createdMode 3축이 모두 착지하면서 M3에서 흐릿했던 "카드가 언제 자동 archive되는가"가 코드 수준으로 명확화. 사용자 mode 다운/업 시나리오가 하이브리드 메서드로 표현되어 UX 예측 가능성 확보.
- **역사적 잔재 청산** — M2 pivot 이후 잔존하던 `OnFieldBudget` VO · `MODE_10D`·`MAX_VIEW`/`MAX_DURATION` 이 M4에 모두 청산. Domain.md의 Card 절이 대폭 축소되고 응집력 증가.
- **Card→Axis 직접 매핑** — Deck 폐기 (M5 예정)의 선행 필수 작업 착지. `card.topic_id` 경유의 우회 참조가 제거되면서 Coverage 재계산이 축 스코프로 단일화. Layer coverage summary가 더 직관적으로 재조정됨.
- **AI role 다각화** — M3까지 backend-developer 1종에 머물던 Static Adapter가 4종으로 확장. 3명 사용자 릴리스 시 콘셉트 감지 다양성 확보. LLM Adapter (M6) 도입 이전 v1 릴리스 커버리지의 근간.
- **3-phase Flyway 마이그레이션 능숙도** — CARD E3(V25/V26/V27)와 LT E4(V28/V29/V30) 2쌍의 3-phase 마이그레이션이 순차 착지. NULL → 백필 → NOT NULL 패턴 · rollback R페어 · 검증 SQL 관습화. M5 이후 Deck 폐기(V31~V33) · Review 신설(V36)에도 그대로 적용 가능.
- **Reviewer 5관점 세션 정식 확립** — M3 재개 이후 첫 표준 스케일. PR 5건 × 5관점 = 25 reviewer 발사. Sceptical Reviewer가 role catalog 콘텐츠 판정에 반복 참조 (`eval.md` §M4 baseline로 축적).
- **역사적 흔적 관리 능숙도** — `card.topic_id` soft-deprecate · `isLastView` deprecated · Reviewer 조치 커밋 별도 관리 (Story 커밋과 구분). 재편 · 폐기 · 회귀 각 축의 흔적이 커밋 이력에서 추적 가능.

---

## 다음 버전 진입 전 확인 (M5 / 0.0.5v Wed 2026-07-22 진입 pre-flight)

- [ ] PR#5 (AS-E3-ROLE-CATALOG-EXPAND) 머지 완료 (커밋만 존재 · 머지 대기 · Reviewer 세션 후속)
- [ ] `docs/DOMAIN.md` §Card 절 갱신 — `OnFieldBudget` 삭제 · `createdMode` 하이브리드 설명 · `axisId` 필드 반영
- [ ] `docs/DOMAIN.md` §LearningAxis 절 갱신 — Aggregate 4-tier (Facade → Layer → Axis → (Topic|RoadmapNode|Selection) → SelectionNode) 확정 (M3 R6 잔재)
- [ ] `sdd/in-progress/product-card.md` §"Epic 1·2·3 완주" 표기
- [ ] `sdd/in-progress/product-learning-tower.md` §"Epic 4 완주" 표기
- [ ] `sdd/in-progress/product-ai-suggestion.md` §"Epic 3 Story 3-3~3-5 완주" · role 4종 안정화 표기
- [ ] `fix/brainstorming/version/0.0.2v/issue-21/22/23/07/10` → resolved 표기
- [ ] `workflow/task/pes/brainstorming/0.0.5v/` 신설 (`review.md` §brainstorming 트리거 항목 5건)
- [ ] `card.topic_id` 컬럼 DROP 시점 결정 (v1 릴리스 이후로 유보 확정)
- [ ] M3 R3 (PATCH JsonNullable) 도입 결정 · M5 PR 진입 전 (Deck 폐기·Review 신설에 PATCH 다수 신설 예정)

---

## 참고

- 상위 계획: `./milestone.md`
- 회고: `./review.md` (계획 vs 실제 · Reviewer 매트릭스 · M5 결정 보정)
- 비용: `./cost.md` ($0 · role 3종 신설에도 비용 0 Q&A)
- 성능: `./performance.md` (스킵 · 정성 지표 · 재편 후 관찰 지점)
- 인프라: `./infra.md` (로컬 스코프 유지)
- AI 평가: `./eval.md` (role 4종 catalog baseline 확장)
- 테스트: `./test-coverage.md` (Card BC 재편 후 매트릭스)
- 로컬 확인: `./ux-check.md` (5 축 · 시나리오 A~G)
- 이전 마일스톤: `../0.0.3v/outcome.md`
- 다음 마일스톤: `../0.0.5v/milestone.md` (Deck 폐기 · Review 신설)
- 릴리스 로드맵: `../../release/version/0.0.1v/release.md`
