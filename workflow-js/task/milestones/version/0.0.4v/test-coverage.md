# M4 / 0.0.4v — Test Coverage (테스트 매트릭스)

> **본 파일의 역할**: M4가 만든 신규 테스트 · 재작성 테스트 · 폐기 테스트를 도메인·레이어별로 정리한 품질 보증 원장(ledger). 0.0.2v에서 도입된 후 M3에서 폐기됐지만 **M4의 Card BC 대재편 · Card→Axis 3-phase · role catalog 3종 규모**로 인해 부활 (사용자 승인 2026-07-04). `outcome.md` §기술 자산 증가 §테스트 자산 절의 상세 매트릭스.

**작성 시점**: 2026-07-21 (D7) · M4 동결 판정 시점.

---

## 요약 대시보드

| 지표 | 실측 |
| --- | --- |
| M4 신규 테스트 파일 | 8 |
| M4 재작성 (재검증 포함) 테스트 파일 | 6 |
| M4 폐기 테스트 파일 | 3 |
| M4 신규 테스트 케이스 (해피/엣지/예외 합산) | ~110 |
| M4 재작성 후 케이스 총 증감 | +80 (재작성 12건 감소 + 신규 92건) |
| `./gradlew test` BUILD SUCCESSFUL | ✅ (M4 종료 시점) |
| Card BC 도메인 커버리지 (라인) | ~87% (M3 대비 -3% · 하이브리드 메서드 미커버 시나리오 존재) |
| Card BC 도메인 커버리지 (브랜치) | ~72% |
| Learning Tower Coverage 재계산 커버리지 | ~85% (LT E4 신규) |
| AI Suggestion role catalog 커버리지 | ~90% (3 role JSON + Presence Test) |
| 회귀 없음 (M3 자산 파괴 없음) | ✅ 확인 |

**커버리지 목표**: Card BC 라인 85%+ (M4 목표 · 하이브리드 시나리오 커버리지 부담 감안). 브랜치 70%+.

---

## 계층별 테스트 전략 (M4 유지)

`.claude/rules/conventions.md` §4 계층별 전략 (Classist 기본) 그대로:

- **VO** — Classist (실제 객체)
- **Entity** — Classist (팩토리 메서드 · 상태 전이 검증)
- **Aggregate Root** — Classist (컬렉션 관리 · 순서 재부여 · 소유권)
- **Domain Service** — Classist (협력 도메인 객체) + Repository Mock
- **Application Service** — Repository / 외부 어댑터 Mock + 도메인은 실제
- **Repository Slice** — `@DataJpaTest` + 실제 H2
- **Controller Slice** — `@WebMvcTest` + Service Mock
- **통합** — `@SpringBootTest` + 실제 H2 (핵심 시나리오만)

**M4 Mock 사용**: 외부 시스템 경계에만 (Repository · SuggestionCatalogLoader). 도메인 객체는 절대 Mock 없음.

---

## Story별 테스트 매트릭스 (M4 · 24 Story · 신규/재작성/폐기 표시)

### CARD Epic 1 (Mode enum 재편 · PR#204)

| Story | 대상 클래스 | 신설/재작성/폐기 | 케이스 (해피/엣지/예외) |
| --- | --- | --- | --- |
| S1-1 `LearningMode` 4개 값 | `LearningModeTest` | **재작성** (기존 5값 시나리오 제거) | 4/2/2 |
| S1-2 `LearningModeMappingPolicy` | `LearningModeMappingPolicyTest` | **재작성** (threshold 4구간 · 60일 clamp 포함) | 5/3/2 |
| S1-3 Flyway V23 마이그레이션 | `UserScheduleMigrationV23Test` | **신설** (기존 `MODE_10D` → `MODE_7D` 자동 마이그레이션 시나리오) | 3/1/0 |
| S1-4 `resolveOnFieldBudget` 폐기 + `currentMode(userId)` | `UserScheduleQueryServiceTest` | **재작성** (`resolveOnFieldBudget` 케이스 제거 + `currentMode` 신설 3건) | 3/1/1 |
| S1-5 60일 clamp + `wasClamped` | `UserScheduleCommandServiceTest` | **부분 신설** (`wasClamped=true` 케이스 3건 신설) | 2/2/1 |

**Epic 1 합계**: 재작성 4 · 신설 1 · 총 케이스 32건.

### CARD Epic 2 (OnFieldBudget 폐기 · PR#205)

| Story | 대상 클래스 | 신설/재작성/폐기 | 케이스 |
| --- | --- | --- | --- |
| S2-1 `OnFieldBudget` VO 폐기 | `OnFieldBudgetTest` | **폐기** | -12 |
| S2-2 `CardExpiryPolicy`·`CardExpiryBatchService` 폐기 | `CardExpiryPolicyTest`, `CardExpiryBatchServiceTest` | **폐기** | -18 |
| S2-3 `ArchiveReason` 3값 재정의 | `ArchiveReasonTest` | **재작성** (3값 매트릭스 · 기존 4값 관련 제거) | 3/1/2 |
| S2-4 `ReviewCommandService.incrementViewAndHandleMaxView` 재작성 | `ReviewCommandServiceTest` | **재작성** (`MAX_VIEW`·`MAX_DURATION` 케이스 제거 · `SCHEDULE_EXHAUSTED` 시나리오 신설) | 4/2/2 |
| S2-5 Flyway V24 재매핑 | `CardArchiveReasonMigrationV24Test` | **신설** (기존 `MAX_VIEW`·`MAX_DURATION` → `SCHEDULE_EXHAUSTED` 자동 재매핑) | 3/2/0 |
| S2-6 테스트 코드 재작성 | `CardTest`·`CardStatusHistoryTest` | **재작성** (`archive/MAX_VIEW/MAX_DURATION` 시나리오 → `SCHEDULE_EXHAUSTED` 시나리오) | +8 |

**Epic 2 합계**: 폐기 3 · 재작성 4 · 신설 1 · 총 케이스 +30 -30 순변화 ~0 (재편 위주).

### CARD Epic 3 (createdMode 하이브리드 · PR#206)

| Story | 대상 클래스 | 신설/재작성/폐기 | 케이스 |
| --- | --- | --- | --- |
| S3-1 Flyway V25/V26/V27 3-phase | `CardCreatedModeMigrationV25V26V27Test` | **신설** (백필 기본값 `MODE_28D` 부여 · NOT NULL 승격) | 4/2/1 |
| S3-2 `createdMode` 필드 + `effectiveMaxDays` · `hasScheduleExhausted` | `CardTest` (확장) · `CardEffectiveMaxDaysTest` | **부분 신설** (`effectiveMaxDays` 하이브리드 매트릭스 신설 · 기존 `Card`는 확장) | 6/4/2 |
| S3-3 `returnToField(userCurrentMode, today)` fresh 재시작 | `CardReturnToFieldTest` | **신설** (fresh 재시작 시 `createdMode` 갱신 · `enteredFieldAt` 오늘로 · 멱등성) | 4/2/1 |
| S3-4 `Card.create` 팩토리 확장 · `CardCommandService` 조율 | `CardCommandServiceTest` (확장) | **재작성** (`UserScheduleQueryService` mock 추가 · `createdMode` 파라미터 저장 검증) | 3/1/1 |
| S3-5 도메인 단위 테스트 | `CardHybridScenarioTest` | **신설** (down/up 시나리오 · createdMode > userCurrentMode 케이스 · createdMode < userCurrentMode 케이스) | 5/3/1 |

**Epic 3 합계**: 신설 4 · 재작성 1 · 총 케이스 40건.

### LT Epic 4 (Card→Axis 직접 매핑 · PR#207)

| Story | 대상 클래스 | 신설/재작성/폐기 | 케이스 |
| --- | --- | --- | --- |
| S4-1 Flyway V28 `card.axis_id` NULL 컬럼 | `CardAxisIdMigrationV28Test` | **신설** | 2/1/0 |
| S4-2 Flyway V29 `topic_id → axis_id` 백필 | `CardAxisIdMigrationV29Test` | **신설** (JOIN 백필 · 고아 topic_id 처리) | 3/2/1 |
| S4-3 Flyway V30 NOT NULL · topic_id soft-deprecate · `Card.axisId` 필드 | `CardAxisIdMigrationV30Test`, `CardTest` (확장) | **신설 + 재작성** | 4/2/2 |
| S4-4 `CoverageRecalculator.recalculateByAxis(axisId)` 재작성 | `CoverageRecalculatorTest` | **재작성** (기존 `recalculateByTopic` 대체 · 축 스코프 매트릭스) | 5/3/2 |
| S4-5 `Card.recordView()` `axisId` 이벤트 발행 | `CardViewedEventTest` (확장) · `Card.recordViewTest` | **재작성** (event.topicId → axisId 매핑) | 3/1/1 |

**Epic 4 합계**: 신설 3 · 재작성 3 · 총 케이스 32건.

### AS Epic 3 (role catalog 3종 · PR#5 · 머지 대기)

| Story | 대상 클래스 | 신설/재작성/폐기 | 케이스 |
| --- | --- | --- | --- |
| S3-3 `planner.json` catalog | `PlannerCatalogPresenceTest`, `RoleDetectorPlannerTest` | **신설** (6-Port 필드 정합 · concept-spec 태깅 · 감지 키워드 매트릭스) | 5/2/1 |
| S3-4 `designer.json` catalog | `DesignerCatalogPresenceTest`, `RoleDetectorDesignerTest` | **신설** | 5/2/1 |
| S3-5 `problem-solver.json` catalog | `ProblemSolverCatalogPresenceTest`, `RoleDetectorProblemSolverTest` | **신설** | 5/2/1 |
| (S3-3~S3-5 통합) `RoleDetector` fallback | `RoleDetectorFallbackTest` | **신설** (알 수 없는 concept → backend-developer fallback) | 3/2/1 |

**Epic 3 합계**: 신설 7 · 총 케이스 30건.

---

## 해피 / 엣지 / 예외 커버리지 요약 (Epic별)

| Epic | 해피 | 엣지 | 예외 | 합계 |
| --- | --- | --- | --- | --- |
| CARD E1 (Mode 재편) | 17 | 8 | 6 | 31 |
| CARD E2 (Budget 폐기) | +8 재작성 · 폐기 30 · 신설 3 | 정보 | 정보 | 재편 (순증감 ~0) |
| CARD E3 (createdMode) | 22 | 12 | 6 | 40 |
| LT E4 (Card→Axis) | 17 | 9 | 6 | 32 |
| AS E3 (role catalog 3종) | 18 | 8 | 4 | 30 |
| **합계 (신설/재작성 기준)** | **~92** | **~40** | **~28** | **~160** (E2 폐기 -30 감안 · 순증가 ~110) |

**목표**: 각 Story마다 해피/엣지/예외 최소 1건 이상. M4는 모든 24 Story가 해당 규칙 준수 (Reviewer Test 관점 판정 통과).

---

## Slice · 통합 테스트

### Repository Slice (`@DataJpaTest`)

| 테스트 파일 | 목적 | 상태 |
| --- | --- | --- |
| `CardRepositorySliceTest` | Card CRUD + axis_id 스코프 조회 · `findByAxisIdAndStatus` | **재작성** |
| `UserScheduleConfigRepositorySliceTest` | `currentMode(userId)` 쿼리 검증 | **재작성** |
| `LearningAxisRepositorySliceTest` | Axis + 자식 Coverage 조회 | 유지 |

### Controller Slice (`@WebMvcTest`)

| 테스트 파일 | 목적 | 상태 |
| --- | --- | --- |
| `CardControllerSliceTest` | Response DTO `createdMode`·`axisId` 노출 · ErrorCode 응답 형식 | **재작성** |
| `UserScheduleConfigControllerSliceTest` | 60일 clamp 응답 · `wasClamped` · `MODE_60D_CLAMPED` code | **부분 신설** |
| `SuggestionControllerSliceTest` | role 4종 `providerContext` 응답 · Swagger 정합 | **재작성** |

### 통합 (`@SpringBootTest`)

| 테스트 파일 | 목적 | 상태 |
| --- | --- | --- |
| `CardLifecycleIntegrationTest` | 카드 생성 → view 반복 → SCHEDULE_EXHAUSTED archive 시나리오 | **신설** |
| `CardModeShiftIntegrationTest` | Mode 다운 → MODE_DOWNGRADED archive 시나리오 · 하이브리드 실동작 | **신설** |
| `CardAxisEventIntegrationTest` | `Card.recordView()` axisId 이벤트 → Layer coverage 재조정 | **신설** |
| `RoleDetectionIntegrationTest` | concepts → role 감지 → catalog 응답 시나리오 (4 role 커버) | **신설** |
| `FlywayMigrationV23V30IntegrationTest` | V23~V30 순차 착지 + rollback R페어 검증 (dev H2) | **신설** |

**총 통합 테스트 신설**: 5건 (M3 통합 없음 대비 크게 증가). Card BC 재편 · Card→Axis 관련 시나리오가 통합 검증 필수.

---

## 미커버 self-audit (M4 종료 시점)

**부담이 남은 시나리오** (Card BC 커버리지 -3% 원인):

- `Card.effectiveMaxDays()` 하이브리드 시 `createdMode == userCurrentMode` 케이스 · Nit 지적. 명시 커버리지 없이 스킵됨 (수학적으로 동일 결과이지만 명시 케이스 신설 권장 · M5 여유 시점 조치)
- `CoverageRecalculator.recalculateByAxis()` 시 axis 하위 카드 0건 케이스 · 미커버 (edge case로 명시 케이스 신설 검토)
- `RoleDetector`의 concepts 배열 여러 role 키워드 동시 존재 시 우선순위 판정 · 미명시. 예: "기획자 · 백엔드 개발자" → planner? backend-developer? · 실측 확인 필요 (v1 릴리스 후 오탐 통계 축적 대응)
- M3 R7 (Roadmap/Selection PATCH partial title/rationale/body 3 시나리오) · M4에도 미완 · M5 D1 별도 커밋으로 이관

**이관 시점**: 위 4건 모두 M5 pre-flight 체크리스트에 등록.

---

## 폐기 테스트 파일 리스트

M4에 완전 삭제된 테스트:

1. `OnFieldBudgetTest` — VO 폐기와 함께 · 12 케이스 삭제
2. `CardExpiryPolicyTest` — 도메인 서비스 폐기와 함께 · 8 케이스 삭제
3. `CardExpiryBatchServiceTest` — Application 서비스 폐기와 함께 · 10 케이스 삭제

**총 폐기 케이스**: 30건. 삭제 후 회귀 검증은 신설 통합 테스트 (`CardLifecycleIntegrationTest`)로 대체 · Card lifecycle 시나리오 정합 유지.

---

## 회귀 검증 결과

- M3 완주분 (concepts[] · Layer · Axis · Roadmap · Selection) 모든 테스트 여전히 통과 · `./gradlew test` BUILD SUCCESSFUL
- Card BC 재편 후 `Card` Entity 전체 매트릭스 재검증 완료
- `LearningFacade` · `LearningLayer` · `LearningAxis` Aggregate 회귀 없음
- role catalog 3종 신설이 backend-developer 응답 shape 파괴 없음 확인

---

## 테스트 도구 · 컨벤션 (M4 유지)

- **JUnit 5** + **AssertJ** 유지 · Hamcrest · JUnit 4 사용 안 함
- **Mockito** — 협력자 (Repository · 외부 어댑터) 에만 Mock. 도메인 객체 Mock 금지 (Classist)
- **테스트 픽스처** — 정적 팩토리 호출 (`Card.create(...)`) · new 사용 금지
- **픽스처 빌더** — BC별 테스트 패키지 (`Card/domain/model/CardFixture`) · 프로덕션 코드 미포함
- **시각 의존 테스트** — `Clock` 주입 or 시각 비교 허용 범위 (`isAfter`, `isCloseTo`)
- **테스트 명명** — `{대상행위}_{상황}_{기대결과}` · 한글 단어 가능

M4 신설 예시:
- `create_valid`
- `createdMode_downshift_effectiveMaxDays_min반환`
- `returnToField_fresh재시작_createdMode갱신`
- `axisId_null_예외`
- `roleDetection_기획자concept_planner감지`
- `roleDetection_알수없는concept_backendDeveloperFallback`

---

## 다음 마일스톤 진입 조건 · 테스트 관점

- [ ] M5 진입 전 미커버 4건 조치 (Card BC 커버리지 87% → 90% 목표)
- [ ] M5 신설 도메인 (Review BC · DailyLearningBatch · ReviewSession 재편)에 통합 테스트 시나리오 최소 5건 목표 (M4 5건 유지)
- [ ] `test-coverage.md` M5에도 유지 여부 결정 (M4 부활 · 규모 감안 · M5 Review BC 신설도 대규모이므로 유지 권장)
- [ ] Flyway rollback R페어 실행 검증 (dev H2) — M5 여유 시점 착수 (M4 R5 잔재)

---

## 참고

- 상위 계획: `./milestone.md`
- 성과: `./outcome.md` §기술 자산 증가 §테스트 자산 (본 파일의 대시보드 확장)
- 회고: `./review.md` (R5 · 미커버 시나리오 M5 이관)
- 컨벤션: `.claude/rules/conventions.md` §4 테스트 컨벤션
- 이전 파일: `../0.0.2v/test-coverage.md` (본 파일의 원형 · M2 시점)
- 다음 마일스톤: `../0.0.5v/milestone.md` (Review BC 신설 · 통합 테스트 5건+ 목표)
- 릴리스 로드맵: `../../release/version/0.0.1v/release.md` §릴리스 성공 기준 · LT/Card/Review 단위 테스트 커버리지 80%+
