# M4 / 0.0.4v — Local UX 확인 가이드

> **파일의 역할**: M4 (2026-07-15 ~ 07-21) 완료 시점에 **배포 없이 로컬에서 BE + FE를 켜서 확인 가능한 것**을 정리한다. 사용자님이 릴리스 없이 진척 상황을 직접 눈으로 볼 수 있는 창.
>
> **본 M4의 특성 — M4 하이라이트**: "Card lifecycle 새 정책 착지 + AI role 다각화". `LearningMode` 4개 값 재정의 + `OnFieldBudget` 폐기 + Card `createdMode` 하이브리드 도메인 메서드 + Card→Axis 직접 매핑 + AI Static Adapter role 4종 (backend-developer + planner + designer + problem-solver) 완주. Flyway V23~V30 8건 착지 · 3-phase 마이그레이션 2쌍 (V25/V26/V27 · V28/V29/V30).

---

## 확인 목적

- BE·FE가 새 스키마·API·컴포넌트로 **여전히 로컬에서 부팅되는가** — Flyway V23~V30 정상 착지
- **6개 축 실 기능 확인**:
  1. `LearningMode` 4개 값 (`MODE_7D/14D/28D/60D`) + `raw_input_days` 60일 clamp (CARD E1)
  2. `OnFieldBudget` 폐기 + `ArchiveReason` 재편 (`MANUAL/SCHEDULE_EXHAUSTED/MODE_DOWNGRADED`) (CARD E2)
  3. Card `createdMode` 필드 + `effectiveMaxDays` 하이브리드 메서드 + `returnToField` fresh 재시작 (CARD E3)
  4. Card→Axis 직접 매핑 (`card.axis_id NOT NULL` + `card.topic_id` soft-deprecate) + Coverage 축 스코프 이관 (LT E4)
  5. **AI Static Adapter role 4종 완주** — `providerContext: "static:planner"` · `"static:designer"` · `"static:problem-solver"` (AS E3)
  6. 기존 M3 시나리오 회귀 없음 — concepts[]·Layer/Axis·Roadmap 노드·Selection 노드 CRUD 여전히 동작

---

## 사전 조건

- Java 21 toolchain 활성 (`./gradlew --version` 통과)
- Node 20+ + pnpm 활성 (FE 프로젝트 기준)
- 브라우저 (Chrome/Edge — DevTools Network 탭 활용)
- 로컬 포트 열림: BE 8080, FE 3000 or 5173
- Postman or `curl` (BE API 확인용)
- **M3 로컬 상태 초기화** — Card 도메인 스키마 대재편으로 인해 dev H2를 `./gradlew clean`으로 초기화 후 재부팅 권장 (기존 V16~V22 이력만 남아있으면 V23~V30이 순차 착지)

---

## BE 로컬 부팅 절차 (dev 프로필 · H2)

```bash
# 1. 저장소 정리 (M3 dev H2 초기화 · Card 대재편으로 인해 필수 권장)
./gradlew clean

# 2. 부팅 (dev 프로필 자동, H2 in-memory)
./gradlew bootRun

# 3. 정상 부팅 확인 (별도 터미널)
curl -s http://localhost:8080/actuator/health
# → {"status":"UP"} 응답

# 4. Flyway 이력 V30까지 도달 확인
curl -s http://localhost:8080/actuator/flyway
# → migrations 배열에 V23 ~ V30 성공 상태 (8건 신규)
```

**주의**: M4에서 다음 8 V버전 신규 착지.
- V23 `reorganize_learning_mode.sql` — `LearningMode` 재정의 + 기존 `MODE_10D` 자동 마이그레이션
- V24 `abolish_onfieldbudget.sql` — `OnFieldBudget` VO 컬럼 제거 + `ArchiveReason` CHECK 재작성 + 기존 데이터 재매핑
- V25/V26/V27 `add_card_created_mode.sql` — 3-phase (nullable → 백필 → NOT NULL)
- V28/V29/V30 `card_axis_id.sql` — 3-phase (NULL 컬럼 → 백필 → NOT NULL + topic_id soft-deprecate)

## FE 로컬 부팅 절차

```bash
# FE 프로젝트 루트에서
pnpm install
pnpm dev
# → http://localhost:5173 부팅
```

`VITE_API_BASE_URL=http://localhost:8080` 설정 확인 (`.env.development`).

---

## 로컬 확인 체크리스트

### ✅ BE 단독 확인 (15분)

- [ ] **부팅 성공** — `./gradlew bootRun` 로그에 `Started ThirdToolApplication` 출력
- [ ] **Actuator health** — `curl localhost:8080/actuator/health` → UP
- [ ] **Swagger UI 진입** — `http://localhost:8080/swagger-ui.html` 페이지 렌더 · **M3 6그룹 + suggestion 그룹에 새 role 응답 지원 확인**
- [ ] **H2 콘솔** — `http://localhost:8080/h2-console` 진입 · JDBC URL `jdbc:h2:mem:testdb`
- [ ] **Flyway V23~V30 이력** — H2 콘솔:
  ```sql
  SELECT version, description, success FROM flyway_schema_history
  WHERE version IN ('23','24','25','26','27','28','29','30')
  ORDER BY installed_rank;
  ```
  → 8건 모두 `success=true`
- [ ] **Card BC 스키마 정합** (H2 콘솔 `SHOW COLUMNS FROM card`):
  - `card.created_mode` — VARCHAR(20) NOT NULL · CHECK IN ('MODE_7D','MODE_14D','MODE_28D','MODE_60D')
  - `card.axis_id` — BIGINT NOT NULL · FK to `learning_axis(id)`
  - `card.topic_id` — 여전히 존재 (soft-deprecate · 다음 릴리스 DROP 예정)
  - `card` 테이블에 `budget_*` 계열 컬럼 (기존 `on_field_budget_*`) **없음** — grep pass 0건
- [ ] **`ArchiveReason` CHECK 제약 재작성 확인** — H2 콘솔:
  ```sql
  SELECT CHECK_CLAUSE FROM information_schema.check_constraints
  WHERE constraint_name LIKE '%archive_reason%';
  ```
  → `('MANUAL','SCHEDULE_EXHAUSTED','MODE_DOWNGRADED')` 3개 값
- [ ] **`LearningMode` CHECK 제약 재작성 확인** — H2 콘솔:
  ```sql
  SELECT CHECK_CLAUSE FROM information_schema.check_constraints
  WHERE constraint_name LIKE '%learning_mode%';
  ```
  → `('MODE_7D','MODE_14D','MODE_28D','MODE_60D')` 4개 값 · 기존 `MODE_10D` 잔재 없음
- [ ] **기존 데이터 마이그레이션 성공** — H2 콘솔:
  ```sql
  -- 기존 archived 카드가 새 ArchiveReason으로 재매핑됨
  SELECT archive_reason, COUNT(*) FROM card
  WHERE archive_reason IS NOT NULL GROUP BY archive_reason;
  -- → MAX_VIEW/MAX_DURATION 계열 없음 · SCHEDULE_EXHAUSTED 등으로만 존재
  ```
- [ ] **`OnFieldBudget` grep pass 0건** — 프로덕션 코드에서:
  ```bash
  grep -rn "OnFieldBudget" src/main/java
  # → 0건 (또는 주석/폐기 마커만 남음)
  ```
- [ ] **AI Static Adapter 리소스 파일 4종 존재**:
  - `src/main/resources/ai/catalog/backend-developer.json` (M3 유지)
  - `src/main/resources/ai/catalog/planner.json` **신규**
  - `src/main/resources/ai/catalog/designer.json` **신규**
  - `src/main/resources/ai/catalog/problem-solver.json` **신규**
  - 각 파일 유효 JSON · 6-Port 필드 (layers · axes · chapters · selectionOutlines) 모두 존재

### ✅ BE API 실행 확인 (30분, curl 예시)

이하 예시는 로그인 후 `{JWT}` 획득한 상태 전제.

**① `LearningMode` 재편 + 60일 clamp (CARD E1)**
```bash
# 기본: rawInputDays → Mode 매핑
curl -X POST http://localhost:8080/api/v1/schedule-config \
  -H "Authorization: Bearer {JWT}" -H "Content-Type: application/json" \
  -d '{"rawInputDays": 10}'
# → 200 + { mode: "MODE_14D", clamped: false }
# (10일 → threshold에 따라 MODE_14D로 매핑)

# 60일 상한 clamp (정보성 응답)
curl -X POST http://localhost:8080/api/v1/schedule-config \
  -d '{"rawInputDays": 100}'
# → 200 + { mode: "MODE_60D", clamped: true, code: "MODE_60D_CLAMPED" }

# 기존 MODE_10D 유저 자동 마이그레이션 확인 (H2 콘솔)
# SELECT user_id, mode FROM user_schedule_config WHERE mode = 'MODE_10D';
# → 0건 (V23 마이그레이션이 MODE_10D → MODE_7D로 자동 변환)
```

**② `OnFieldBudget` 폐기 + `ArchiveReason` 재편 (CARD E2)**
```bash
# 카드 조회 시 budget 필드 없음 확인
curl http://localhost:8080/api/v1/cards/1 -H "Authorization: Bearer {JWT}"
# → 200 + { id: 1, ... } · Response DTO에 budget/onFieldBudget 필드 없음

# 반복 조회 → SCHEDULE_EXHAUSTED archive
# (스케줄 소진 기준 · Budget 참조 제거된 새 lifecycle)
# 사용자 mode의 스케줄이 소진되면 카드가 자동 archive
curl http://localhost:8080/api/v1/cards/1
# → { id: 1, status: "ARCHIVE", archiveReason: "SCHEDULE_EXHAUSTED", ... }

# Mode 다운시프트 시 MODE_DOWNGRADED archive
# (사용자가 MODE_28D → MODE_7D로 변경 시 잔여 스케줄이 잘림)
curl -X PUT http://localhost:8080/api/v1/schedule-config -d '{"rawInputDays": 5}'
# → 이후 조회 시 잘린 카드 → archiveReason: "MODE_DOWNGRADED"
```

**③ Card `createdMode` 하이브리드 (CARD E3)**
```bash
# 신규 카드 생성 시 createdMode 저장 확인
curl -X POST http://localhost:8080/api/v1/cards \
  -H "Authorization: Bearer {JWT}" -H "Content-Type: application/json" \
  -d '{
    "axisId": 5,
    "summary": "test",
    "mainNote": {"text": "..."},
    "keywords": [{"value": "K1"}]
  }'
# → 201 + { id: 100, createdMode: "MODE_28D", ... }
# (유저 현재 mode가 카드에 pinned 저장)

# effectiveMaxDays 하이브리드 확인 (내부 계산 · Review 진행 시 소비)
# createdMode = MODE_28D, userCurrentMode = MODE_7D 상태에서:
# card.effectiveMaxDays(userCurrentMode) → min(28, 7) = 7일 인터벌 적용

# returnToField fresh 재시작
curl -X POST http://localhost:8080/api/v1/cards/100/return-to-field
# → 200 · 이후 조회 시 { createdMode: "MODE_7D" (유저 현재 mode 재부여),
#                        enteredFieldAt: "2026-07-XX" (오늘로 갱신) }

# H2 콘솔에서 백필 확인:
# SELECT id, created_mode FROM card WHERE created_mode IS NULL;
# → 0건 (V26 백필 완료 · 기본값 MODE_28D 부여)
```

**④ Card→Axis 직접 매핑 (LT E4)**
```bash
# 카드 조회에 axisId 직접 노출
curl http://localhost:8080/api/v1/cards/1 -H "Authorization: Bearer {JWT}"
# → { id: 1, axisId: 5, topicId: 12, ... }
# (topicId는 soft-deprecate · 아직 노출됨 · 다음 릴리스 제거 예정)

# axis 스코프로 카드 목록 조회
curl "http://localhost:8080/api/v1/axes/5/cards" -H "Authorization: Bearer {JWT}"
# → 200 + [ { id: 1, axisId: 5, ... }, ... ] (기존 topic 스코프 대체)

# Coverage 재계산 축 스코프 트리거 확인
# (카드 view · 상태 전이 시 axis 단위 재계산)
curl -X POST http://localhost:8080/api/v1/reviews/1/view
# → 200 · 이후 Axis coverage summary 재조정
curl http://localhost:8080/api/v1/axes/5
# → { id: 5, coverageStatus: "PARTIAL" or "COVERED", ... }

# H2 콘솔에서 백필 검증:
# SELECT COUNT(*) FROM card WHERE axis_id IS NULL;
# → 0건 (V29 백필 · V30 NOT NULL 승격 후 전량 존재)
```

**⑤ 🌟 AI Static Adapter role 3종 신설 (AS E3 · M4 하이라이트)**
```bash
# planner role — concepts에 "기획자" 포함 시 감지
curl -X POST http://localhost:8080/api/v1/suggestions/chapters-outline \
  -H "Authorization: Bearer {JWT}" -H "Content-Type: application/json" \
  -d '{
    "concepts": ["기획자"],
    "layerName": "기능의 구현",
    "axisName": "요구사항 정의",
    "axisReason": "PM 관점의 요구사항 프레이밍"
  }'
# → 200
# {
#   "chapters": [ ... 5개 챕터 ... ],
#   "providerContext": "static:planner",   ★ role 감지 검증
#   "suggestionsAvailable": true
# }

# designer role — concepts에 "디자이너" 포함 시 감지
curl -X POST http://localhost:8080/api/v1/suggestions/chapters-outline \
  -d '{"concepts": ["디자이너"], "layerName": "...", ...}'
# → providerContext: "static:designer"

# problem-solver role — concepts에 "문제해결" or "알고리즘" 포함 시 감지
curl -X POST http://localhost:8080/api/v1/suggestions/chapters-outline \
  -d '{"concepts": ["알고리즘"], "layerName": "...", ...}'
# → providerContext: "static:problem-solver"

# subtree 응답도 role별 정합
curl -X POST http://localhost:8080/api/v1/suggestions/chapter-subtree \
  -d '{
    "concepts": ["기획자"],
    "chapter": {"title": "...", "rationale": "..."},
    ...
  }'
# → providerContext: "static:planner" + role별 bodyAsciiTree

# 알 수 없는 role → backend-developer fallback (M3 default)
curl -X POST ... -d '{"concepts": ["미지의 컨셉"], ...}'
# → providerContext: "static:backend-developer"
```

**⑥ 회귀 검증: M3 시나리오 여전히 동작 (concepts[]·Layer·Axis·Roadmap·Selection)**
```bash
# concepts 다중 저장 (M3 유지)
curl -X POST http://localhost:8080/api/v1/facades/me/concepts \
  -d '["백엔드 개발자", "기획자"]'
# → 201 · concepts 2건

# Layer/Axis 생성 (M3 유지)
curl -X POST http://localhost:8080/api/v1/facades/me/layers -d '{"name":"..."}'
curl -X POST http://localhost:8080/api/v1/layers/1/axes -d '{"name":"..."}'

# Roadmap 노드 CRUD (M3 유지)
curl -X POST http://localhost:8080/api/v1/axes/5/roadmap-nodes -d '{...}'
curl -X PUT http://localhost:8080/api/v1/axes/5/roadmap-nodes/order -d '[11, 10]'

# Selection 컨테이너 · 노드 CRUD (M3 유지)
curl -X POST http://localhost:8080/api/v1/axes/5/selections -d '{"name":"..."}'
curl -X POST http://localhost:8080/api/v1/selections/20/nodes -d '{...}'

# → 모두 M3 응답과 동일 결과 · M4 재편이 M3 자산 파괴 없음 확인
```

### ✅ FE 단독 확인 (15분)

- [ ] **FE 부팅 성공** — `pnpm dev` 로그에 `Local: http://localhost:5173/` 출력
- [ ] **홈 진입 · 콘솔 에러 없음** — 브라우저 DevTools Console에서 red error 없음
- [ ] **M3 신규 컴포넌트 회귀 없음** — `<ConceptsInput>`·`<LayersListPage>`·`<AxisSelect>`·`<RoadmapNodeList>`·`<RoadmapNodeCard>`·`<SelectionContainerList>`·`<ChaptersOutlineButton>`·`<ConceptSpecTooltip>` 모두 M3 동작 유지
- [ ] **`<CardModeSelector>` 4개 값 UI** — 사용자 스케줄 설정 화면에서 `MODE_7D/14D/28D/60D` 4개 옵션 · `rawInputDays` 입력 시 자동 매핑 프리뷰
- [ ] **`<RawInputDaysInput>` 60일 clamp 안내** — 60일 초과 입력 시 `MODE_60D_CLAMPED` 정보성 배지 표시
- [ ] **`<ArchiveReasonBadge>` 새 값 3종 매핑** — 카드 archive 화면에서 `MANUAL`·`SCHEDULE_EXHAUSTED`·`MODE_DOWNGRADED` 3개 라벨 문구 정합 (`docs/ux/wip-language.md` 참조)
- [ ] **`<CardCreatedModeBadge>` 표시** — 카드 상세 화면에서 `createdMode` 배지 (예: "생성 시 mode: 28일 인터벌") + `userCurrentMode`와 diff 있으면 하이브리드 안내
- [ ] **`<CardEditor>` axisId 매핑 재정합** — 카드 생성·수정 시 topicId 아닌 axisId로 저장 요청 (DevTools Network 확인)
- [ ] **`<ChaptersOutlineDialog>` role 4종 지원** — `<ProviderContextIndicator>` 배지 화면에 `static:planner`·`static:designer`·`static:problem-solver`·`static:backend-developer` 4종 표시 가능
- [ ] **Zod 스키마 갱신** — DevTools Network에서 `card` 응답에 `createdMode`·`axisId` 필드 존재 · `budget` 필드 없음 · Zod 파싱 에러 없음

### ✅ BE+FE 통합 확인 (40분 · 사용자 시점 시나리오)

**시나리오 A — 사용자 스케줄 재설정 후 자동 마이그레이션**

1. FE 홈 진입 · 로그인 (M3 완주 상태 유지)
2. 사용자 스케줄 설정 페이지 진입 → 기존 `MODE_10D` 유저는 이미 `MODE_7D`로 자동 마이그레이션됨을 배지로 확인
3. `<RawInputDaysInput>` 5일 입력 → `<CardModeSelector>` 프리뷰 `MODE_7D` 표시 → 저장
4. `<RawInputDaysInput>` 100일 입력 → `MODE_60D_CLAMPED` 정보성 안내 표시 → 저장 시 `PUT /schedule-config` payload `rawInputDays: 100` · 응답 `{ mode: "MODE_60D", clamped: true }`
5. **관찰**: 사용자 대시보드에 현재 mode "60일 인터벌 (사용자 요청은 100일이었음)" 문구 표시

**시나리오 B — Card 생성 → createdMode pinned 저장 → Mode 다운시프트 후 lifecycle**

1. 시나리오 A에서 `MODE_28D` 상태로 조정
2. LT 시나리오 흐름 (M3 완주 상태) → axis 아래 카드 생성 → `POST /cards { axisId: 5, ... }` (topicId 아닌 axisId 전송)
3. 응답 `{ id: 100, axisId: 5, createdMode: "MODE_28D", ... }` 확인
4. `<CardCreatedModeBadge>` "생성 시 28일 인터벌" 표시
5. 사용자 스케줄 → `MODE_7D`로 다운시프트 저장
6. 카드 상세 재진입 → 하이브리드 배지 "생성 시 28일 → 현재 7일 인터벌 적용" 안내
7. 스케줄 소진 시점 도달 (직접 로컬에서 반복 view or 시각 조작) → `<ArchiveReasonBadge>` "MODE_DOWNGRADED" 표시
8. **관찰**: 카드가 자동 archive · `enteredFieldAt` 이 원 시점 유지 · Budget 관련 로그 없음 (`OnFieldBudget` 폐기 확인)

**시나리오 C — Card `returnToField` fresh 재시작**

1. 시나리오 B에서 archived 상태 카드
2. 카드 상세 → [필드로 복귀] 버튼 → `POST /cards/100/return-to-field`
3. 응답 · 화면 갱신 확인:
   - `status: ON_FIELD`
   - `createdMode: MODE_7D` (유저 현재 mode 재부여)
   - `enteredFieldAt: 오늘`
4. `<CardCreatedModeBadge>` "생성 시 7일 인터벌" 갱신 (하이브리드 안내 사라짐)
5. **관찰**: fresh 재시작 → 다시 새 lifecycle 진입

**시나리오 D — Card→Axis 직접 매핑 + Coverage 재계산 축 스코프**

1. 시나리오 B 완주 상태 · 카드 여러 건 존재
2. Axis 상세 진입 → 카드 목록 조회 (`GET /axes/5/cards`) → 응답 확인
3. `<AxisCoverageIndicator>` 값 확인 (`PARTIAL` or `COVERED`)
4. 새 카드 추가 후 axis coverage 자동 재조정 확인 (`POST /cards { axisId: 5, ... }` 후 axis 재조회)
5. **회귀 검증**: 기존 topic 기반 UI 흐름이 axisId 기반으로 대체 · topicId 필드가 응답에 여전히 존재하지만 사용되지 않음
6. **관찰**: `<LayerCoverageSummary>` 값이 axis 스코프 트리거로 갱신됨 (Layer 아래 여러 Axis의 coverage 집계)

**시나리오 E — 🌟 AI Static Adapter role 4종 (M4 하이라이트)**

1. LearningFacade 진입 → `<ConceptsInput>` chip 3개: "기획자" · "디자이너" · "알고리즘"
2. Axis "요구사항 정의" 상세 진입 → Roadmap 탭 → `<ChaptersOutlineButton>` 클릭
3. `<ChaptersOutlineDialog>` open · 로딩 → 응답 도착
4. **관찰 지표 (dev only)**: `<ProviderContextIndicator>` 배지에 role 표시 — 감지된 role에 따라 `static:planner` or `static:designer` or `static:problem-solver` (concepts에 기획자 우선 시)
5. 각 챕터가 planner role catalog에서 온 콘텐츠인지 확인 (예: "1. 요구사항 프레이밍", "2. 유저 스토리 매핑" 등 · backend-developer와 상이)
6. concepts를 "디자이너"만으로 변경 → 재요청 → `static:designer` 감지 · 다른 챕터 리스트 반환
7. concepts를 "알고리즘"으로 변경 → `static:problem-solver` 감지 · 트러블슈팅/알고리즘 중심 챕터
8. concepts를 "백엔드 개발자"로 변경 → `static:backend-developer` (M3 유지)
9. concepts를 "미지의 컨셉"으로 변경 → default `static:backend-developer` fallback
10. **관찰**: 5개 챕터 승인 → subtree 5회 병렬 호출 · 각 응답 `providerContext` 동일 role 유지 · body ASCII 트리가 role 별 콘텐츠

**시나리오 F — 개발자용 · role catalog 자산 정합**

1. BE 저장소 검색:
   - `src/main/resources/ai/catalog/backend-developer.json` (M3 유지 · Reviewer 정합 통과 확인)
   - `src/main/resources/ai/catalog/planner.json` **신규** · 6-Port 필드 모두 존재
   - `src/main/resources/ai/catalog/designer.json` **신규**
   - `src/main/resources/ai/catalog/problem-solver.json` **신규**
2. 각 catalog JSON 유효성:
   ```bash
   jq . src/main/resources/ai/catalog/planner.json
   jq . src/main/resources/ai/catalog/designer.json
   jq . src/main/resources/ai/catalog/problem-solver.json
   # → 모두 파싱 통과 · 문법 오류 없음
   ```
3. 각 catalog에 `layers` · `axes` · `chapters` · `selectionOutlines` 필드 존재 확인
4. `concept-spec.txt` 카탈로그 태그 (6+5)와 각 catalog의 챕터 정합 (Sceptical Reviewer 판정 통과 이력 확인 · `eval.md` §M4 baseline 절 참조)
5. `RoleDetector` 로직 확인 — planner/designer/problem-solver 키워드 감지 정합 · 오탐 통계는 `eval.md`에 별도 관찰

**시나리오 G — 개발자용 · Flyway 3-phase 마이그레이션 착지 검증**

1. dev H2 clean 후 `bootRun` → V23~V30 모두 착지 확인
2. `card.created_mode` 3-phase:
   - V25 실행 후 → 컬럼 존재 · NULL 허용
   - V26 실행 후 → 백필 완료 · 기본값 `MODE_28D` 부여
   - V27 실행 후 → NOT NULL 승격
3. `card.axis_id` 3-phase:
   - V28 실행 후 → 컬럼 존재 · NULL 허용
   - V29 실행 후 → `topic_id` JOIN 통해 axis_id 백필 · 검증 SQL 통과
   - V30 실행 후 → NOT NULL 승격 · `topic_id` soft-deprecate (컬럼 유지)
4. 각 V버전에 대응하는 R페어(rollback) 파일 존재 확인 (프로덕션 대응 · dev에서 실행은 M7 이후)

---

## M4 확인 못하는 것 (다음 마일스톤 이관)

배포 없이 로컬에서 확인 불가한 M4 out of scope 항목:

- ❌ Deck 폐기 · Deck BC의 자체 책임 이전 (**M5 · 이슈 #13 이관 · LT Epic 5**)
- ❌ Review 재편 · `ReviewScope` enum · `/layers/{id}/review-sessions` 엔드포인트 (**M5 · 이슈 #14 · #24~#26 이관**)
- ❌ Daily Batch · Cross-layer Review · Session 재편 (**M5**)
- ❌ 캐시 측정 대시보드 v1 (오늘·7일·streak) (**M6 · 이슈 #26**)
- ❌ Static → LLM Adapter Cascade fallback (**M6 · Vertex AI Gemini Flash 2.5**)
- ❌ AS Epic 5 Rate Limit 확장 · Layer/Axis 엔드포인트 확장 (**M6~M7**)
- ❌ AS Epic 6 관측성 · `thirdtool.suggestion.*` 메트릭 배선 (**M7**)
- ❌ 챕터 노드 재생성 API + hint UI (**v2 · 이슈 #18**)
- ❌ AI 비용 예산 cap (**v2 · 이슈 #20 · v1은 관찰 지표만**)
- ❌ 배포·CloudFront·AWS 인프라 · Web Vitals baseline (**M7**)
- ❌ 관측 지표 대시보드 (Grafana) (**M7 · product-op.md**)
- ❌ 검색·미디어·부하 테스트 (**v2 · v1 이후**)
- ❌ AI Interactive Roadmap Session Wizard UI (7단계) (**M6~M7**)
- ❌ `card.topic_id` 컬럼 실제 DROP (**v1 릴리스 이후 · M4는 soft-deprecate에 머무름**)

---

## 관찰 지표 로깅 확인 (M3 계승 · M4 role 4종 반영)

M3에서 착지한 관찰 지표 로깅 인프라 (`build/logs/thirdtool.log`)가 M4의 role 3종 신설을 자동 반영해야 한다.

- **AI Static Adapter 호출 시 로그 항목** (M3 골격 유지):
  - `port=chapters-outline` or `chapter-subtree` or `selection-outline` or `selection-subtree`
  - `provider=static`
  - `role=backend-developer` | `planner` | `designer` | `problem-solver` **← M4 신규 3종 추가**
  - `userId={...}` · `duration=Xms`
- **Card 재편 관련 신규 로그**:
  - `INFO: card.archive_reason_migrated cardId={...} from=MAX_VIEW to=SCHEDULE_EXHAUSTED` (V24 마이그레이션 시)
  - `INFO: card.created_mode_backfilled cardId={...} mode=MODE_28D` (V26 백필 시)
  - `INFO: card.axis_id_backfilled cardId={...} axisId={...} fromTopicId={...}` (V29 백필 시)
- Rate limit 발동 시 (M3 유지):
  - `WARN: rate_limit_exceeded userId={...} bucket=user_10rpm`

M5~M6에 role 감지 오탐 통계·주간 요약·비용 지표 확장 예정.

---

## 문제 발생 시 (Troubleshooting)

| 증상 | 원인 후보 | 해결 |
| --- | --- | --- |
| `./gradlew bootRun` 실패 with "Flyway migration failed at V24" | `ArchiveReason` 기존 데이터 재매핑 SQL 실패 or CHECK 제약 재작성 충돌 | `./gradlew clean` (dev H2 초기화) 후 재시도. prod MySQL이면 마이그레이션 주석의 검증 SQL 재확인 · R24 롤백 준비 |
| Flyway V26 백필 실패 | `card.created_mode` 기본값 부여 실패 or 기존 카드에 null axis_id 존재 | V25 착지 확인 → 기존 카드 count 확인 → 백필 SQL 조건 재검토 |
| Flyway V29 백필 실패 | `card ← axis_topic ← learning_axis` JOIN 실패 or 고아 topic_id 존재 | 마이그레이션 주석의 검증 SQL 실행 · 고아 카드 count 확인 · 필요 시 임시 axis 부여 정책 결정 |
| API 500 with "OnFieldBudget not found" | 코드 잔재 (완전히 제거 안 됨) or 캐시된 클래스 로드 | `./gradlew clean build` · grep `OnFieldBudget` 재확인 |
| Card 응답에 `budget` 필드 여전히 노출 | Response DTO 미갱신 (S2-1~S2-6 record 실수) | `CardResponse` record 필드 재확인 · Zod schema 정합 |
| `providerContext: "static:planner"` 대신 `static:backend-developer` | `RoleDetector` 로직 감지 실패 or catalog 파일 부재 | `planner.json` 존재·유효 JSON 확인 · `RoleDetector` 감지 키워드 정합 · 로그에서 `role=` 항목 확인 |
| `<CardCreatedModeBadge>` 표시 안 됨 | Response DTO에 `createdMode` 필드 누락 or Zod schema 미갱신 | Card Response record 확인 · Zod `z.enum(["MODE_7D",...])` 정합 |
| Coverage 재계산이 여전히 topic 스코프 트리거 | `CoverageRecalculator.recalculateByAxis()` 미배선 or 이벤트 리스너 미갱신 | `LT E4 S4-4/S4-5` 착지 확인 · `CardViewedEvent`의 `axisId` 필드 발행 검증 |
| 기존 `MODE_10D` 유저가 로그인 시 500 | V23 자동 마이그레이션 누락 or 코드에서 여전히 `MODE_10D` 참조 | Flyway V23 성공 확인 · grep `MODE_10D` 잔재 검색 |

---

## 마무리 판정

M4 완료 GO/NO-GO는 아래 5개 조건 모두 만족 시 통과:

- [ ] BE 부팅 성공 + Flyway V23~V30 8건 착지 + Card·`LearningMode`·`ArchiveReason` 스키마 정합 + `OnFieldBudget` grep pass 0건
- [ ] BE API 6개 축 실행 확인 (Mode 재편 · Budget 폐기 · createdMode · Card→Axis · AI role 4종 · M3 회귀 없음)
- [ ] FE 부팅 성공 + M4 신규/갱신 컴포넌트 (`<CardModeSelector>`·`<RawInputDaysInput>`·`<ArchiveReasonBadge>`·`<CardCreatedModeBadge>`·`<ProviderContextIndicator>` 4종 role) 렌더
- [ ] 7개 시나리오 완주 — 특히 **시나리오 E (AI role 4종)** · 시나리오 B/C (Card 하이브리드) · 시나리오 D (Card→Axis)
- [ ] Flyway 3-phase 마이그레이션 검증 (V25/V26/V27 · V28/V29/V30) + `eval.md` §M4 baseline 절 role 3종 콘텐츠 정합 판정 통과

→ 조건 만족 시 M4 동결 · **M5 진입** (2026-07-22 Wed).

---

## 참고

- 마일스톤 원본: `./milestone.md` — Epic PR 매트릭스 · SDD 위치 · 종료 신호 · 의존 chain
- 이전 마일스톤 UX 확인: `../0.0.3v/ux-check.md`
- 릴리스 스코프: `../../release/version/0.0.1v/release.md` — 0.1.0v (~08-19) 사이클 10단계
- 다음 마일스톤: 07-22 진입 · `../0.0.5v/milestone.md` (M4 종료 후 신설)
- FE 대응 마일스톤: `../../../pes/fe/fe-milestones/version/0.0.4v/milestone.md`
- SDD 참조:
  - `../../../pes/workspectrum/sdd/in-progress/product-card.md` — Epic 1·2·3 (Card BC 재편)
  - `../../../pes/workspectrum/sdd/in-progress/product-learning-tower.md` — Epic 4 (Card→Axis 직접 매핑)
  - `../../../pes/workspectrum/sdd/in-progress/product-ai-suggestion.md` — Epic 3 role catalog 3종
- 07-02 pivot 이슈: `../../../fix/brainstorming/version/0.0.2v/issue-21/#22/#23` (Card) · `issue-07` (Card→Axis) · `issue-10` (role catalog)
- UI 문구 정책: `../../../docs/ux/wip-language.md` — `ArchiveReason` 3개 값 라벨 매핑
