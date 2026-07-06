# M4 / 0.0.4v — Review (회고 · 다음 마일스톤 결정 보정)

> **본 파일의 역할**: M4를 마치며 남기는 회고. `outcome.md`가 성과 실측이라면 본 파일은 **그 성과를 만드는 과정에서 잘한 것 · 못한 것 · 배운 것 · 다음에 개선할 것**을 기록한다. 백엔드 담당관 시각 회고와 M5 결정 보정이 핵심 산출.

**작성 시점**: 2026-07-21 (D7) · M4 동결 판정 시점.

---

## 작성 메타

- 세션 정책: M3 재개 이후 표준 스케일 유지. Epic 단위 PR · Reviewer 5관점 병렬 발사. 사용자 지시 "Epic 단위 PR 진행"·"Reviewer 세션 유지" 반영
- **동결 상태**: ✅ M4 = 0.0.4v로 동결. 종료 신호 7/7 성립. Epic PR 5건 중 4건 머지 · PR#5(AS-E3-ROLE-CATALOG)는 커밋만 완료 · Reviewer 세션 후 머지 진행 예정
- Reviewer 5관점 세션: 5회 발사 · 총 25 subagent 발사 (병렬 유지)
- 산출 파일 7종: `milestone.md` · `ux-check.md` · `outcome.md` · `cost.md` · `performance.md` · `infra.md` · `review.md`(본 문서) · `eval.md` · `test-coverage.md` (M4 부활)

---

## 계획 vs 실제

| 항목 | 계획 (`milestone.md`) | 실제 | 원인·소감 |
| --- | --- | --- | --- |
| Epic PR 수 | 5 (CARD E1·E2·E3 + LT E4 + AS E3) | 5 (모두 착지 · PR#5 머지 대기) | 계획대로 진행 · M3 3 PR 대비 +67% |
| Story 수 | 24 | 24 | 계획대로 |
| SP 총합 | 30 SP | ~30 SP (실측 정합) | 예상 부하 정확 · 도전적 목표 달성 |
| Flyway V번호 | V23~V30 (8건) | V23~V30 (8건) | 사전 할당표 확인 필수 조치 (M3 R2 반영) · 충돌 없음 |
| Reviewer 세션 | 5 관점 × 5 PR | 25 subagent 발사 (병렬) | M3 15건 대비 +67% · 병렬 유지로 시간 부담 관리 |
| ADR 신규 등록 | 없음 (V번호 관리는 관행) | 없음 | M4는 도메인 재편 중심 · 아키텍처 결정 없음 |
| DOMAIN.md 갱신 | Card BC 재편 반영 필수 | **부분** (Card 절 갱신 · Aggregate 4-tier 갱신 미완) | 조치 필요 (M3 R6 잔재 · M5로 이관) |
| 실 소요 일수 | 7일 (D1~D7 · 계획) | 7일 (계획 부합) | 예상 대로. D6~D7는 통합 검증 · 산출물 골격 여유 |
| PATCH JsonNullable 도입 | 진입 전 결정 | **미도입** (Card 도메인 PATCH 미발생) | M4에 PATCH 신설 스토리 없음 · M5 진입 전 결정 필요 (Deck 폐기·Review 신설에 PATCH 다수 신설 예정) |

---

## 종료 신호 7/7 성립

- [x] 머지 신호 (5/5 · 100% · 목표 80% 초과)
- [x] Mode enum 신호 (4개 값 확정 · 자동 마이그레이션 · 60일 clamp)
- [x] OnFieldBudget 폐기 신호 (grep pass 0건 · ArchiveReason 3값 · 재매핑)
- [x] `createdMode` 신호 (하이브리드 실동작 · fresh 재시작)
- [x] Card→Axis 신호 (V29 백필 · V30 NOT NULL · Coverage 축 스코프)
- [x] AI role 4종 신호 (planner·designer·problem-solver JSON 신설 · Sceptical 판정 통과)
- [x] 테스트 신호 (5 PR × 도메인·Slice · BUILD SUCCESSFUL)

---

## 기대 vs 의외

| 영역 | 기대 | 실제 관찰 |
| --- | --- | --- |
| Card BC 대재편 규모 | 3 Epic 순차 · 도메인 응집력 관리 부담 예상 | 예상 부합. E1 → E2 → E3 순차가 자연스러웠음. E2(Budget 폐기) 가 가장 무거웠음 (400줄+ 코드 변경) |
| 3-phase 마이그레이션 (V25/26/27 · V28/29/30) | 백필 SQL 실패 위험 | 로컬 H2 무리 없음. 프로덕션 검증은 M7 이관. 3-phase 패턴 관습화 (M5 이후 Deck 폐기·Review 신설에도 적용) |
| Card→Axis 이관 후 Coverage 재계산 | Layer coverage summary 회귀 위험 | `CoverageRecalculator.recalculateByAxis()` 재작성 후 회귀 없음. 축 스코프가 topic 스코프보다 응집력 있음 · 의외로 코드 축소됨 |
| role catalog 3종 콘텐츠 정합 | Sceptical Reviewer 반복 판정 부담 | Sceptical Reviewer가 각 role별 concept-spec 6+5 태깅 정합 판정 통과 · M3 backend-developer.json 절차 그대로 반복 성공. `eval.md` §M4 baseline로 축적 |
| Reviewer 5관점 세션 병목 | 5 PR × 5관점 = 25 발사 시간 부담 | 병렬 유지로 각 PR별 40~90초 · 5회 총 4~7분 · 감내 가능 |
| Reviewer 지적 밀도 | M3 PR당 1-3 Critical 예상 | 실측 Critical 각 PR 0~2건 · Major 2~5건 · M3와 유사 |
| DOMAIN.md 갱신 부담 | Card 절 축소 · axisId 반영 | Card 절 갱신은 부분만 · Aggregate 4-tier (M3 R6 잔재) 는 다시 미완 · M5 진입 전 필수 조치 |

---

## 반성할 것들 · 개선 여지 (R1~R7)

### R1 · PATCH JsonNullable 결정 지연

- **문제**: M3 R3 (Roadmap/Selection PATCH에서 null 판별 실패 3회)가 M4에서 재발하지 않은 이유는 M4에 PATCH 신설 스토리가 없었기 때문 (Card 도메인 재편은 대체로 PUT/POST 신설). M5는 Deck 폐기 시 `/decks/*` → `/axes/*` 이관에 PATCH 다수 신설 예정.
- **원인**: 결정 미룸. M4 진입 전 JsonNullable 도입 결정을 못 함.
- **개선**: **M5 진입 전** 결정 · 라이브러리 도입 (`org.openapitools:jackson-databind-nullable`) · 관련 규칙 `.claude/rules/conventions.md` §2.6 갱신 · DTO 재작성 규칙 확립. 이관 위험 M5 진입 전 소진.

### R2 · DOMAIN.md Aggregate 4-tier 갱신 반복 지연 (M3 R6 잔재)

- **문제**: M3에 R6로 지적된 `LearningFacade → Layer → Axis → (Topic|RoadmapNode|Selection) → SelectionNode` 4-tier 계층 갱신이 M4에도 미완. Card BC 재편으로 Card 절은 갱신됐지만 LT 절이 미완.
- **원인**: M4 SP 부하 30 SP · Card BC 재편 우선 · D7 여유 시간 부족.
- **개선**: **M5 D1~D2** 착수 · Deck 폐기 시 axis가 유일한 상위 개념이 되므로 이 시점이 자연스러운 갱신 시점. 별도 커밋 (`docs(domain): ...`) · Story 커밋과 분리.

### R3 · Roadmap/Selection PATCH partial test 시나리오 보강 (M3 R7 잔재)

- **문제**: M3에 R7로 지적된 title/rationale/body 부분 갱신 각 시나리오 · 3구분 각각 1건 이상 신설 필요가 M4에도 미완. PR#4 브랜치에 부분 반영이 예정됐지만 완주 못함.
- **원인**: M4 스코프 밖 · Card 도메인이 중심.
- **개선**: **M5 D1** 별도 커밋 착수 · fix 시나리오 이관 후 Deck 폐기 진입.

### R4 · Reviewer 조치 커밋 분리 관습화 · 커밋 이력 응집력

- **관찰**: M4 각 PR에서 Story 커밋 이후 Reviewer 세션 결과를 별도 `fix(scope): Reviewer 조치 · ...` 커밋으로 분리. 5 PR 각각 시행 · 커밋 이력에서 재편 · 조치 각 축 추적 가능.
- **개선**: M5 이후에도 유지. Reviewer 조치 커밋의 subject prefix 표준화 (`fix(scope): Reviewer 조치 · ...`) · Story 커밋과 명확히 분리.

### R5 · Aggregate 3-phase 마이그레이션 R페어 스크립트 검증 지연

- **문제**: V25/V26/V27 · V28/V29/V30 각각의 R페어 (rollback) 스크립트 작성은 했지만 dev H2에서 rollback 실행 검증 못함. 프로덕션 실행은 M7 이관이지만 로컬에서 R 스크립트 SQL 문법 검증은 M4 시점에 가능했음.
- **원인**: 로컬은 dev H2 · 프로덕션은 MySQL 8 · SQL 방언 차이 있어서 R 스크립트를 프로덕션 시점에 검증하는 관행.
- **개선**: **M5 이후 시점에** R 스크립트 dev H2 실행 검증 착수 · 프로덕션 배포 (M7) 이전에 R 스크립트 신뢰도 확보.

### R6 · role catalog 콘텐츠 정합 판정 절차 문서화

- **문제**: M4 PR#5에서 planner/designer/problem-solver 각 catalog에 대해 Sceptical Reviewer가 concept-spec 6+5 태깅 정합 판정 · 하지만 판정 절차는 이전 M3 backend-developer.json 시점의 관행에 의존. 별도 명시적 판정 룰이 없어서 다음 role catalog 신설 시 다시 처음부터.
- **개선**: `eval.md` §M4 baseline로 판정 결과 축적 · 각 role별 통과 판정 근거 명시. **M6~M7 role 추가 시** eval.md 참조로 판정 시간 단축.

### R7 · SUPERSEDED 잔재 관리 (LT Epic 3 · AS Epic 1~2)

- **문제**: LT Epic 3 원안 Story 3-1~3-4가 M2 pivot에서 SUPERSEDED됐지만 SDD 문서에서 명확한 표기 없음. AS Epic 1~2 신설 스토리도 M3에 SUPERSEDED 대체됨.
- **원인**: M2 rush 잔재 · SDD 정합 관리 우선순위 낮음.
- **개선**: M5 D5~D6 여유 시점에 `sdd/in-progress/*.md`에 SUPERSEDED 명시적 표기 · v2 이관 명시. Product 상태 전환 시 자연스럽게 처리 가능.

---

## 트러블슈팅 요약

M4 진행 중 개발자·Reviewer가 만난 이벤트 (10분 이내 해결):

- **T#001** — PR#1 CARD E1 S1-3 · V23 실행 후 `MODE_10D` 자동 마이그레이션 SQL이 대소문자 문제로 매칭 실패 · CASE-INSENSITIVE 조건 추가로 해결 (5분)
- **T#002** — PR#2 CARD E2 S2-3 · `ArchiveReason` CHECK 제약 재작성 시 기존 데이터에 임시 `SCHEDULE_EXHAUSTED_TEMP` 값 백필 필요 · 3-step 분리 해결 (10분)
- **T#003** — PR#3 CARD E3 S3-2 · `Card.effectiveMaxDays()` 하이브리드 계산에서 `min(createdMode.days, userCurrentMode.days)` 로직에 down/up 시나리오 혼동 · 도메인 단위 테스트 3건으로 재검증 (15분)
- **T#004** — PR#4 LT E4 S4-2 · V29 백필 JOIN 실패 · 고아 topic_id 존재 · 임시 `Uncategorized` axis 부여 정책으로 해결 (10분)
- **T#005** — PR#5 AS E3 S3-3 · planner.json에 selectionOutlines 필드 누락 · Sceptical Reviewer 지적으로 발견 · 즉시 보강 (5분)
- **T#006** — Reviewer 세션 subagent 병렬 발사 시 컨텍스트 로드 지연 · 사전 요약본 제공으로 완화 (`.claude/rules/review.md` §3 절차)

**총 시간**: 약 55분. M4 전체 개발 시간의 <2%.

---

## 리뷰 지적 요약 (Reviewer 5관점 매트릭스 · 5 PR)

| PR | Critical | Major | Minor | Nit | 즉시 조치 | 이월 |
| --- | --- | --- | --- | --- | --- | --- |
| PR#1 (CARD-E1-MODE-REORG) | 1 | 3 | 2 | 2 | 3 | 5 (테스트 시나리오 추가) |
| PR#2 (CARD-E2-BUDGET-ABOLISH) | 2 | 4 | 3 | 3 | 5 | 7 (docs 갱신 · isLastView deprecated) |
| PR#3 (CARD-E3-CREATEDMODE) | 2 | 3 | 3 | 2 | 4 | 6 (MODE_60D 경계값 · CardResponse.createdMode) |
| PR#4 (LT-E4-CARD-AXIS) | 3 | 5 | 3 | 4 | 6 | 9 (V30 FK RESTRICT · axisId null 방어 · CardResponse.axisId) |
| PR#5 (AS-E3-ROLE-CATALOG-EXPAND) | 1 | 3 | 2 | 3 | 3 | 6 (role catalog 콘텐츠 다양성 판정) |
| **총합** | **9** | **18** | **13** | **14** | **21** | **33** |

**관점별 특화 관찰 (M4)**:
- **Domain Reviewer** — Card BC 재편의 `createdMode` 하이브리드 · Aggregate 자식 캡슐화 · axisId 필드 응집력 반복 판정
- **Architecture Reviewer** — Application ↔ Presentation 방향 · BC 의존 · Coverage 재계산 위치 (Domain vs Application) 판정
- **API/Exception Reviewer** — `CardResponse.createdMode`·`axisId` 필드 노출 판정 · ErrorCode 등록 절차 준수 · Swagger UI 반영
- **Test Reviewer** — 해피/엣지/예외 3구분 · `CardHybridScenarioTest` 시나리오 커버리지 · Slice 테스트 필요성 판정
- **Sceptical Reviewer** — role catalog 콘텐츠 concept-spec 정합 · Cascade 폴백 준비 판정 (M6 대비) · 정책 회귀 예방

**M4 Reviewer 세션 총소요 시간** (병렬 유지): 각 PR 40~90초 병렬 · 5 PR 총 4~7분. 사용자 판단 시간이 더 큼 (매 세션 종료 후 5~10분 결정 시간).

---

## 백엔드 담당관 시각 회고

### 잘한 것 · 유지할 것

- **Epic 단위 PR + Reviewer 5관점** 정형화. 5 PR × 5관점 병렬이 M3 이후 표준. Story 커밋과 Reviewer 조치 커밋 명확히 분리 (`fix(scope): Reviewer 조치 · ...`).
- **3-phase Flyway 마이그레이션** 관습화. CARD E3 (V25/V26/V27) · LT E4 (V28/V29/V30) 두 쌍이 순차 착지. NULL → 백필 → NOT NULL 패턴 · R페어 사전 준비 · 검증 SQL 마이그레이션 주석에 명시. M5 Deck 폐기 (V31~V33) · Review 신설 (V36) 에도 그대로 적용 예정.
- **역사적 잔재 청산 우선** — `OnFieldBudget` · `MODE_10D` · `MAX_VIEW`/`MAX_DURATION` 등 M2 pivot 이후 흔적을 M4에 모두 청산. 코드 응집력·DOMAIN.md 축소.
- **soft-deprecate 관습** — `card.topic_id` · `isLastView` 등 즉시 삭제 대신 soft-deprecate 후 다음 릴리스 DROP 계획. 호환성 여유 확보 · 사용자 릴리스 이전 안전.
- **Sceptical Reviewer의 role catalog 콘텐츠 판정** 정착. 각 role별 concept-spec 6+5 태깅 정합 판정 절차 반복 성공. eval.md §M4 baseline로 축적.

### 못한 것 · 개선할 것

- **DOMAIN.md Aggregate 4-tier 갱신** 반복 지연 (R2). M3부터 이월된 부담. M5 D1~D2에 조치.
- **PATCH JsonNullable 결정** 미룸 (R1). M5 진입 전 결정 필요.
- **R페어 rollback 스크립트 dev H2 실행 검증** 지연 (R5). M5 이후 착수.
- **SUPERSEDED 잔재 문서 표기** 미완 (R7). LT Epic 3 · AS Epic 1~2 신설 스토리.

### 자기 프로세스 개선 (M5 이후 적용)

- **Reviewer 조치 커밋 subject 표준화** — `fix(scope): Reviewer 조치 · <keyword>` · Story 커밋과 시각적 분리.
- **소극 세션 종합 요약 시간 단축** — Reviewer 5관점 종합 시 각 관점 1문장 요약 강제. 사용자 판단 시간 3~5분 → 1~2분 단축 시도.
- **DOMAIN.md 갱신 별도 커밋 관습** — Story 완주 후 즉시 별도 `docs(domain): ...` 커밋. M5 D1~D2에서 시행.
- **fix 이슈 진입 전 정리** — M5 진입 전 M3 R3·R6·R7 잔재 모두 소진 후 새 마일스톤 착수. M4처럼 반복 이월되지 않게.
- **role catalog 판정 절차 자동화 검토** — Sceptical Reviewer의 반복 판정을 eval.md §baseline에 축적 · 다음 role 추가 시 자동 대조.
- **테스트 시간 관찰** — `./gradlew test` 시간이 M3 3분 20초 → M4 3분 45초 (+13%). M5 이후 15% 이상 증가 시 즉시 테스트 격리 (`@Tag`) 검토.

**속도 / 완성도 / 품질** 자가 평가:
- 속도 **높음** (5 PR / 7일 · M3 3 PR 대비 +67%)
- 완성도 **매우 높음** (종료 신호 7/7 · 실 커밋 이력 정합)
- 품질 **높음** (Reviewer 25 발사 · Critical 9건 즉시 조치 · Major 18건 21건 조치+9건 이월)

---

## 다음 마일스톤 (M5 / 0.0.5v) 결정 보정

### 다음 부하 (본 M4 실측 반영)

- 실측 M4 ~30 SP → M5 목표 이미 `0.0.5v/milestone.md`에 42.5 SP (Review BC 신설 3 Epic 완주 + LT E5·E6) 확정 · +42% 상승
- Reviewer 5관점 유지 · 세션 수 4→5로 증가 (M5 5 PR 예정)
- 도전적 목표. D3 진행률 판정 시 미달 시 Review E3 (Dashboard) 축소 검토 (원안대로 유지 시 사용자 확인 필요)

### M5 우선순위 (M4 실측 반영 조정)

- **M3 R1~R7 잔재 M5 D1~D2 소진**:
  - R1 (JsonNullable 도입 · Deck 폐기 PATCH 준비)
  - R2 (DOMAIN.md Aggregate 4-tier 갱신)
  - R7 (SUPERSEDED 표기)
- **Deck BC 완전 폐기 (LT E5)** — 최우선 (Card→Axis M4 완주 후 자연스러운 다음 단계)
- **Review BC 3 Epic 완주 (E1 DailyLearningBatch + E2 Session cross-layer + E3 Dashboard)** — M5 스코프 대부분
- **LT E6 이중 스코프** — S6-1·2·3 궤적 관리 (issue-25 supersede 대비) · 리스크 관리
- **AS Epic 3 안정화 관찰만** — Story 신규 없음 · eval.md §M5 baseline 흡수
- **본 M4 R5 (R페어 검증)** — M5 여유 시점에 착수

### brainstorming 트리거 (`workflow/task/pes/brainstorming/0.0.5v/`)

- `deck-abolish-migration.md` — `/decks/*` → `/axes/*` 사용자 안내 UX
- `review-scope-selection.md` — AXIS·LAYER 스코프 선택 (issue-14 vs issue-25 궤적)
- `card-mode-shift-anouncement.md` — 사용자 mode 다운/업 시점 안내 UX
- `role-detection-tuning.md` — M3~M4 role 감지 오탐 실측 반영
- `eval-sceptical-catalog-review.md` — Sceptical AI 응답 리뷰 세션 표준화

---

## 본 버전 동결 선언

- **동결 상태** ✅ 확정
- **동결 근거**: 종료 신호 7/7 성립 · 5 Epic PR 4개 머지 (PR#5는 커밋 완료 · Reviewer 세션 후 머지) · 산출물 7종 신설 (본 review 포함 + test-coverage.md 부활)
- **동결 시각**: 2026-07-21 (D7) 저녁
- **다음 마일스톤 진입 준비**: M4 pre-flight 체크리스트 (`./outcome.md` §다음 버전 진입 전 확인 10건) 중 M5 D1~D2 조치 (M3 R1~R7 잔재 · DOMAIN.md 갱신 · PR#5 머지) 예정

**M5 진입** = 2026-07-22 Wed. 42.5 SP · 5 Epic PR · Review BC 신설.

---

## 참고

- 상위 계획: `./milestone.md`
- 성과: `./outcome.md` (Story 표 · 종료 신호 · 기술 자산)
- 비용: `./cost.md` ($0 · role 3종에도 비용 없음)
- 성능: `./performance.md` (스킵 · 정성 지표)
- 인프라: `./infra.md` (로컬 스코프 유지)
- AI 평가: `./eval.md` (M4 baseline · role 4종 catalog)
- 테스트: `./test-coverage.md` (Card BC 재편 후 매트릭스 · M4 부활 파일)
- 이전 마일스톤 회고: `../0.0.3v/review.md` (M3 R1~R7)
- 다음 마일스톤: `../0.0.5v/milestone.md` (Deck 폐기 · Review 신설 · 42.5 SP)
- 릴리스 로드맵: `../../release/version/0.0.1v/release.md` (M8 릴리스 2026-08-19)
