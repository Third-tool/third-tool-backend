# [Product] Card FE — 오답노트 편집기 + Mode 매핑 UI + M3 하이브리드 반영

## Product Vision

> **Card FE는 사용자가 "모르는 것"만 Cornell 노트 형태로 저장하는 오답노트 UI다. Fixed interval queue lifecycle을 사용자에게 부드럽게 안내하고, mode 변경(up/down) 시 M3 하이브리드 규칙 — down=cap 즉시 반영, up=새 card만 확장 — 을 사용자가 예측 가능하게 인지하도록 표현한다.**
> Cornell 편집기(MainNote·Summary·Keyword·Tag)는 기존 구조 유지. Lifecycle·mode·archive reason만 재편.

## 배경 및 문제

- **현재 상황 (As-Is)**
  - `features/cards/CardEditor` — Cornell 노트 편집기 (MainNote text/image, Summary, KeywordCue ≥1, CardTag ≤3) 완성.
  - `features/user-schedule/ModeSelector` — `MODE_10D / MODE_20D / MODE_30D` 3개 옵션.
  - Card 상세 화면 — status(ON_FIELD/ARCHIVE)와 archive reason(MAX_VIEW/MAX_DURATION/MANUAL) 표시.
  - `useReturnToField(cardId)` hook — 단순 status 전환만.
- **발생하는 문제**
  - BE product-card 07-02 신설(이슈 #21~#23)으로 lifecycle이 완전 재편 — FE가 뒤따르지 않으면 API mismatch + UI 혼동.
  - Mode enum이 4개(MODE_7D/14D/28D/60D)로 재편되지만 FE는 3개 옵션 유지 중.
  - `createdMode`가 Card에 추가되어 사용자가 "이 카드가 원래 몇 일 스케줄이었는지"를 볼 수 있어야 M3 하이브리드 이해 가능.
  - Archive reason이 `MANUAL / SCHEDULE_EXHAUSTED / MODE_DOWNGRADED` 3개로 재편 → 사용자에게 어떤 이유로 archive됐는지 다르게 안내 필요.
  - Mode down 시 "왜 이 카드가 오늘 안 나오지?" 사용자 혼동 예상 → 사전 안내 필요.
- **왜 지금 해결해야 하는가**
  - BE product-card Epic 1~3이 M4(2026-07-15~07-21)에 진입. FE는 M4~M5에 동기 착지 필요.
  - `product-review.md` (FE)의 Daily batch UI가 Card 상태·mode 응답을 소비.
  - 첫 릴리스(0.1.0v, ~2026-08-19)의 핵심 사이클 4단계(Card 생성)와 6단계(mode 매핑)의 UX 원천.

## 목표 (To-Be)

- **`features/user-schedule/ModeSelector` 재편** — 4개 옵션 (`MODE_7D / MODE_14D / MODE_28D / MODE_60D`) + 사용자 입력 `raw_input_days` → mode 매핑 안내 (예: "14일이라고 하시면 1,3,7,14일차에 노출됩니다").
- **`<ModeChangeConfirmDialog>`** — Mode down 시 "지금까지 학습 중이던 카드 중 이미 새 max 지난 카드는 자동 archive됩니다. 계속하시겠어요?" 명시.
- **`<CardScheduleBadge>`** — Card 상세 화면에 `createdMode` (예: "생성 당시: 14일 스케줄") + `effectiveMax(userCurrentMode)` (예: "현재 스케줄: 7일 (다운으로 축소)") 뱃지.
- **`<ArchiveReasonBadge>`** — 재편 reason 3개 (`MANUAL / SCHEDULE_EXHAUSTED / MODE_DOWNGRADED`) 각각 색상·문구 매핑.
- **`<ReturnToFieldConfirmDialog>`** — "다시 학습하기" 시 "새 스케줄로 fresh 시작합니다 (createdMode = 현재 mode)" 명시.
- **`<UpcomingExposureIndicator>`** — Card 상세에 "다음 노출: {N}일 후 (현재 스케줄 기준)" 카운트다운 표시 (선택적).
- **Zod 스키마 재편** — `LearningMode = z.enum(["MODE_7D","MODE_14D","MODE_28D","MODE_60D"])`, `Card` 응답에 `createdMode` 추가, `ArchiveReason = z.enum(["MANUAL","SCHEDULE_EXHAUSTED","MODE_DOWNGRADED"])`.
- **Cornell 편집기 유지** — MainNote/Summary/Keyword/Tag 편집 컴포넌트 무변경.

## 설계 결정 (Design Decisions)

- **Mode 선택은 slider 아닌 4개 명시 옵션 (vs raw_input_days input)**
  - v1은 사용자가 4개 mode 중 선택 (7D/14D/28D/60D). raw_input_days 자유 입력 대신 명시.
  - 근거: 이슈 #21 v1은 mode enum이 stable. 자유 입력은 v2에서 재검토.
  - `raw_input_days` 매핑 로직은 UI에 노출하되 실질 저장은 mode enum 그대로.
- **Mode down 시 사전 확인 다이얼로그 필수**
  - Down은 M3 규칙상 즉시 cap → 다음 batch에서 이미 넘긴 카드가 자동 archive.
  - 사용자에게 "예상 archive N건" 미리 계산해서 표시 (선택적, v1엔 문구만).
  - Up은 무통보 (진행 카드에 영향 없음).
- **`createdMode` vs `effectiveMax` 표시 분리**
  - Card 상세에 두 정보 다 표시 (createdMode = "생성 당시", effectiveMax = "현재 스케줄").
  - 두 값이 다르면 down cap 발동 상태임을 사용자가 인지.
- **Archive reason별 UX 차별화**
  - `MANUAL`: 회색 뱃지 "수동 보관"
  - `SCHEDULE_EXHAUSTED`: 녹색 뱃지 "학습 완료"
  - `MODE_DOWNGRADED`: 주황 뱃지 "모드 조정으로 조기 보관"
- **`returnToField` fresh 재시작 안내**
  - "새로운 학습 시작" 문구 강조. 기존 스케줄 유지 아니라는 점 명시.
- **`<UpcomingExposureIndicator>` v1 선택적**
  - 사용자에게 "다음 노출 D일 후" 정보가 유용하지만 필수는 아님.
  - v1엔 Card 상세에만, 카드 리스트에는 v2.

## 대안 검토 (Alternatives Considered)

### 갈림길 A. Mode 선택 UI

**Option A — 슬라이더 (raw_input_days 1~60)**
- 장점: 사용자가 정확한 수치 지정.
- 거부 이유: 4개 mode로 매핑되므로 슬라이더의 세밀도가 실질 의미 없음. UX 혼동.

**Option B (선택) — 4개 명시 옵션 (MODE_7D/14D/28D/60D)**
- 비용: 사용자가 4개 중 선택. 세밀도 손실.
- 보상: mode enum stable · 매핑 안내 명확 · v1 UX 마찰 최소.

**Option C — raw_input_days input + 매핑 프리뷰**
- 거부 이유: v1엔 오버스펙. v2에서 재검토 (이슈 #21 v2 프리셋 도입 시).

### 갈림길 B. Mode down 사전 확인

**Option A — 무통보 즉시 반영**
- 거부 이유: 사용자 예상 배신. "왜 오늘 카드가 사라졌지?" 혼동.

**Option B (선택) — 확인 다이얼로그 + 예상 archive 수 표시**
- 비용: down 액션이 2단계.
- 보상: 사용자 예측 가능. down 취소 여지.

### 갈림길 C. Archive reason 시각 표현

**Option A — 단일 "보관" 라벨 (reason 숨김)**
- 거부 이유: 사용자가 "왜 보관됐는지" 이해 못함. `SCHEDULE_EXHAUSTED`와 `MODE_DOWNGRADED`가 UX상 완전히 다른 의미.

**Option B (선택) — reason별 색상·문구 매핑 뱃지**
- 비용: 뱃지 3종.
- 보상: 사용자 이해도 상승. mode down의 결과가 명시적.

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 / 라우트 배치

```
┌────────────────────────────────────────────────────────────────┐
│ Router                                                         │
│  /cards/new                → CardEditPage (신규 생성)          │
│  /cards/:id                → CardDetailPage (상세)             │
│  /cards/:id/edit           → CardEditPage (편집)               │
│  /user-schedule            → UserSchedulePage (mode 설정)      │
├────────────────────────────────────────────────────────────────┤
│ features/cards/                                                │
│   CardEditor (Cornell 편집기, 유지)                            │
│     ├ MainNoteEditor / SummaryEditor / KeywordInput / TagInput │
│   CardDetailPage                                               │
│     ├ CardHeader                                               │
│     ├ CornellDisplay                                           │
│     ├ CardScheduleBadge (createdMode + effectiveMax)  ★ 신규   │
│     ├ ArchiveReasonBadge (reason별 색상)              ★ 신규   │
│     ├ UpcomingExposureIndicator (다음 노출 D일)       ★ 신규   │
│     └ CardActionBar (archive · returnToField · edit)           │
│   CardListPage                                                 │
│     └ CardCard (요약)                                          │
├────────────────────────────────────────────────────────────────┤
│ features/user-schedule/                                        │
│   ModeSelector (4개 옵션)                             ★ 재편    │
│   ModeChangeConfirmDialog                             ★ 신규   │
│   RawInputDaysMappingHint (안내)                      ★ 신규   │
├────────────────────────────────────────────────────────────────┤
│ lib/api/schemas/                                               │
│   learningMode.ts  (4개 enum)                         ★ 재편   │
│   card.ts          (createdMode 필드 추가)            ★ 재편   │
│   archiveReason.ts (3개 enum)                         ★ 재편   │
└────────────────────────────────────────────────────────────────┘
```

### 핵심 플로우

**1. Card 생성 (오답노트 저장)**
```
User → /cards/new
Form 입력 (MainNote/Summary/Keyword/Tag) + axisId 선택 (AxisSelect)
[저장] 클릭
  → POST /cards { axisId, mainNote, summary, keywords, tags }
    (BE: user 현재 mode 조회 → Card.create(createdMode=userMode))
  → 응답 { cardId, createdMode }
  → toast "카드 저장됨. {1일차} 뒤 다시 만납니다."
  → 리디렉트 /cards/:cardId
```

**2. Mode 변경 (down, 즉시 cap 반영)**
```
User → /user-schedule
현재 mode: MODE_14D. 신규 mode: MODE_7D 선택
[다음] 클릭
  → ModeChangeConfirmDialog 열림
    ├ 문구: "1,3,7일 스케줄로 축소됩니다. 이미 7일 넘긴 카드는 자동 보관됩니다."
    ├ (예상 archive 수: GET /cards?ready-to-archive-on-downgrade=MODE_7D — 선택적)
    └ [확인] / [취소]
[확인] 클릭
  → PATCH /user-schedule { mode: MODE_7D }
  → 응답 200
  → toast "모드 변경 완료. 다음 학습 시 반영됩니다."
```

**3. Card 상세 → 다음 노출 확인**
```
User → /cards/:cardId
CardDetailPage 로드:
  → GET /cards/:cardId 응답 { ..., createdMode, effectiveMax, nextExposureDate? }
  → CardScheduleBadge 표시
    ├ "생성 당시: MODE_14D (1,3,7,14일 스케줄)"
    └ "현재 스케줄: 7일 (모드 다운으로 축소)" (createdMode !== effectiveMax 시)
  → UpcomingExposureIndicator "다음 노출: 3일 후"
```

**4. `returnToField` fresh 재시작**
```
User → /cards/:archivedCardId (ARCHIVE 상태)
[다시 학습] 버튼 클릭
  → ReturnToFieldConfirmDialog 열림
    ├ "새 학습으로 시작합니다. 현재 모드({MODE_14D}) 스케줄이 적용됩니다."
    └ [확인] / [취소]
[확인]
  → POST /cards/:cardId/return-to-field
  → 응답 { status: ON_FIELD, createdMode: MODE_14D, enteredFieldAt: today }
  → toast "학습 재시작. 내일부터 다시 노출됩니다."
```

### 외부 의존

- **BE `/api/v1/cards/*`** — Card CRUD + `return-to-field` 엔드포인트.
- **BE `/api/v1/user-schedule`** — Mode 조회·변경.
- **BE `/api/v1/axes/*`** — CardEditor의 `<AxisSelect>` 원천.
- **`product-review.md` (FE)** — Daily batch가 Card 응답 소비. `createdMode`·`effectiveMax` 정보를 batch UI에 노출할지 결정.

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | ApiError code | HTTP | 클라이언트 권장 동작 (UX) |
| --- | --- | --- | --- |
| Cornell MainNote text/image 둘 다 empty | `CARD_MAIN_NOTE_EMPTY` | 400 | 인라인 에러 · 저장 버튼 비활성 유지 |
| Summary blank | `CARD_SUMMARY_EMPTY` | 400 | 인라인 에러 |
| Keyword 미입력 | `CARD_KEYWORD_MIN_REQUIRED` | 400 | 인라인 에러 "최소 1개 필요" |
| Tag 4개 이상 | `CARD_TAG_LIMIT_EXCEEDED` | 400 | 태그 입력 비활성화 (3개 도달 시) |
| Axis 미선택 | `AXIS_NOT_FOUND` | 404 | Axis 선택 UI 강조 |
| Card 조회 시 소유권 불일치 | `CARD_NOT_FOUND` | 404 | 카드 리스트로 리디렉트 + 토스트 |
| ARCHIVE 카드 편집 시도 | `CARD_ARCHIVED_READONLY` | 409 | "다시 학습하기 먼저 눌러주세요" 안내 |
| Mode 변경 시 mode enum 불일치 | `USER_SCHEDULE_INVALID_MODE` | 400 | mode 선택 UI 재조회 (앱 재시작 안내) |

### 로깅 정책 (FE)

- **항상 기록**:
  - Card 생성/편집/archive/returnToField 이벤트 (Sentry breadcrumb — cardId, action, result)
  - Mode 변경 이벤트 (fromMode → toMode, timing)
  - ApiError 발생 시 code + status + requestId (Sentry)
- **debug**:
  - Cornell 편집기 자동 저장 시도 (실패 시)
  - Mode down 확인 다이얼로그 열림·닫힘
- **절대 금지**:
  - Cornell 노트 원문 (MainNote text, imageUrl) 통째 로깅 — cardId + 이벤트 타입만
  - 사용자 PII 반영

### 관측 지표 (Web Vitals)

- `LCP` — CardDetailPage ≤ 1.5s (P95)
- `INP` — Cornell 편집기 keystroke ≤ 200ms (P95)
- `card.saved_total` — RUM 이벤트 (FE)
- `card.archived_total{reason}` — 카드 리스트에서 reason별 archive 뱃지 클릭 이벤트

## 롤아웃 / 마이그레이션 (Rollout)

### 전제

- BE product-card Epic 1(Mode enum) + Epic 2(OnFieldBudget 폐기) + Epic 3(createdMode) 완료 후 FE 진입 (M4).
- 초기 3명 사용자 · 트래픽 극소량 · dev/prod 병행 배포.
- FE는 M4~M5에 착지.

### Product 의존성

- **선행**: BE product-card.md Epic 1~3 완료
- **동시**: `product-learning-tower.md` (FE) — `<AxisSelect>` 소비
- **후행**: `product-review.md` (FE) — Card 응답 소비

### Epic·Story 의존성 그래프

```
Epic 1 (Mode 재편 UI)  ──►  Epic 2 (Card 스케줄 뱃지) ──►  Epic 3 (returnToField + Mode change UX)
     │                              │                              │
     ├─► ModeSelector 4옵션         ├─► CardScheduleBadge          ├─► ReturnToFieldConfirmDialog
     ├─► Zod schema 재편            ├─► ArchiveReasonBadge         └─► ModeChangeConfirmDialog
     └─► RawInputDaysMappingHint    └─► UpcomingExposureIndicator
```

### 환경별 설정 분기

| 항목 | dev (`.env.development`) | prod (`.env.production`) |
| --- | --- | --- |
| `VITE_API_BASE_URL` | http://localhost:8080 | https://api.thirdtool.dev |
| MSW handlers | Card + UserSchedule 모두 활성 | disabled |
| Sentry | local | enabled |
| `<UpcomingExposureIndicator>` | 활성 (개발 확인용) | 활성 |
| Mode down 예상 archive 수 사전 계산 API 호출 | 활성 (관찰용) | v1은 미도입 (v2 결정) |

## 성공 지표 (KPI)

- **Card 저장 성공률**: ≥ 99% (v1 baseline)
- **Cornell 편집기 INP P95**: ≤ 200ms
- **Mode down 후 사용자 revert 비율**: ≤ 10% (down 확인 다이얼로그 UX 신뢰성 지표)
- **`ArchiveReasonBadge` 이해도**: 사용자 인터뷰 3명 중 3명 "왜 archive됐는지" 즉시 답변 가능
- **`createdMode !== effectiveMax` 안내 이해도**: 사용자 인터뷰 3명 중 2명 이상 "왜 스케줄이 다른지" 답변 가능

## Scope

**In Scope**:
- Cornell 편집기 유지 (기존 컴포넌트)
- Mode enum 4개 재편 UI
- `createdMode`·`effectiveMax` 뱃지
- `ArchiveReason` 3개 뱃지
- Mode down 확인 다이얼로그
- `returnToField` fresh 재시작 UX
- Zod 스키마 재편

**Out of Scope**:
- Card 챕터 노드 재생성 UI (이슈 #18) — v2
- Card viewCount 상세 대시보드 — v2 (관찰 지표만 v1 로깅)
- Card 이력 timeline UI (모든 status transition) — v2
- 대량 카드 선택 archive UI — v2
- Card 검색 (`product-search.md` FE) — v2

## 대상 사용자

- **오답노트 학습자 (초기 3명)** — 모르는 것 발견 시 즉시 Cornell 노트로 저장하고 규칙적으로 다시 보는 사람.
- **자기 학습 캐시 궁금해하는 사람** — mode 다운 시 즉시 load 감소 · up 시 예측 가능 확장 이해 필요.

## 연결된 Epic 목록

- [ ] Epic 1: Mode 선택 UI 재편 (4개 옵션 + 매핑 안내)
- [ ] Epic 2: Card 스케줄 · Archive reason 뱃지
- [ ] Epic 3: Mode change · returnToField 확인 다이얼로그

## 관련 문서

- **의존 BE Product**: `../../workspectrum/sdd/in-progress/product-card.md` — Card lifecycle 도메인
- **동시 FE Product**: `product-learning-tower.md` (FE) — AxisSelect 소비
- **후행 FE Product**: `product-review.md` (FE) — Card 응답 소비
- **관련 이슈**: `workflow/task/fix/brainstorming/version/0.0.2v/issue-21 ~ #23`
- **릴리스 문서**: `../../../milestones/release/version/0.0.1v/release.md` §Product 3
- **FE-ADR 후보**: Mode 선택 UI 방식 (4-option 채택 근거)

## 열린 질문 (Open Questions)

1. **Mode down 예상 archive 수 사전 계산 API**: BE에 dry-run 엔드포인트 추가할지 (`GET /cards?ready-to-archive-on-mode=MODE_7D`) — v1엔 미도입, 문구만으로 안내 · v2 결정.
2. **`<UpcomingExposureIndicator>` 카드 리스트 노출** — v1은 상세만. 카드 리스트에도 D-day 표시할지 v2 검토.
3. **Cornell 편집기 자동 저장** — v1은 명시 저장. draft 자동 저장은 v2.
4. **`createdMode` 표기 방식** — "MODE_14D" vs "14일 스케줄" vs "1,3,7,14일차". 사용자 이해도 A/B 검증 v1 관찰.

---

# [Epic 1] Mode 선택 UI 재편

## Epic 목표
사용자가 `MODE_7D/14D/28D/60D` 4개 옵션 중 선택하고, 각 옵션의 실제 스케줄(예: "1,3,7,14일차")을 명시적으로 확인할 수 있게 한다.

## 배경
BE 이슈 #21로 mode enum이 재편됨. FE는 이 재편의 UI 원천이며 다른 모든 FE feature(Card, Review 대시보드)의 mode 정보 소비 기반.

## 완료 기준
- [ ] `ModeSelector` 4개 옵션 활성
- [ ] `learningMode.ts` Zod 스키마 재편 (`z.enum(["MODE_7D","MODE_14D","MODE_28D","MODE_60D"])`)
- [ ] `RawInputDaysMappingHint` 안내 문구
- [ ] Vitest 단위 테스트

## Epic 기술 결정 / 대안
- **명시 옵션 vs 슬라이더**: 명시 옵션. §대안 검토 갈림길 A 참조.

---

# [Epic 2] Card 스케줄 · Archive reason 뱃지

## Epic 목표
Card 상세 화면에서 사용자가 `createdMode` · `effectiveMax` · `ArchiveReason`을 즉시 이해할 수 있게 뱃지 UI를 제공한다.

## 배경
BE 이슈 #23으로 `createdMode` 필드가 Card에 추가. M3 하이브리드 규칙이 down 시 `effectiveMax`를 축소하므로 사용자에게 두 값 차이를 명시적으로 보여줘야 예측 가능.

## 완료 기준
- [ ] `<CardScheduleBadge>` 컴포넌트 (createdMode + effectiveMax)
- [ ] `<ArchiveReasonBadge>` 3-reason 매핑
- [ ] `<UpcomingExposureIndicator>` 다음 노출 D일 계산 로직
- [ ] Card 응답 Zod 스키마에 `createdMode`·`effectiveMax` 추가
- [ ] Vitest · MSW 시나리오 (mode down 결과 반영)

## Epic 기술 결정 / 대안
- **뱃지 색상 매핑**: `MANUAL`=회색 / `SCHEDULE_EXHAUSTED`=녹색 / `MODE_DOWNGRADED`=주황. §대안 검토 갈림길 C 참조.

---

# [Epic 3] Mode change · returnToField 확인 다이얼로그

## Epic 목표
Mode down 시 사전 안내 다이얼로그 + `returnToField` 시 fresh 재시작 안내 다이얼로그로 사용자 예측 가능성 확보.

## 배경
Mode down은 M3 규칙상 진행 카드에 영향 큼. 사용자에게 사전 안내 없이 반영하면 "왜 카드가 사라졌지?" 혼동. `returnToField`도 fresh 재시작 (createdMode 새로) 규칙을 명시해야 사용자 의도 반영.

## 완료 기준
- [ ] `<ModeChangeConfirmDialog>` — Mode down 시 확인
- [ ] `<ReturnToFieldConfirmDialog>` — fresh 재시작 안내
- [ ] Vitest · Testing Library 시나리오 (사용자 취소 · 확인 · 후속 API 호출)
- [ ] E2E 최소 시나리오 (down → 다이얼로그 → 확인 → mode 변경)
