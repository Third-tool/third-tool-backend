``# [Product] Card — 오답노트 Card + Fixed Interval Queue Lifecycle

## Product Vision

> **Card는 사용자가 "모르는 지식"만 Cornell 노트 형태로 저장하는 오답노트다. Lifecycle은 사용자의 학습 mode(1,3,7,14,28,60 중 몇 단계 climb)에 따라 fixed interval queue로 관리되며, 못 본 카드는 누적 없이 그냥 지나간다.**
> Mode 변경은 M3 하이브리드 — down = 기존 card 즉시 cap, up = 새 card만 확장 — 으로 처리해 사용자 의도(load 즉시 감소 vs 진행 중 card 보호)를 정확히 반영한다.

## 배경 및 문제

- 현재 상황 (As-Is)
  - Card lifecycle이 `OnFieldBudget(maxView, maxDuration)` 이중 게이트 (`src/main/java/com/example/thirdtool/Card/domain/model/OnFieldBudget.java`).
  - `LearningMode` = `{MODE_10D, MODE_20D, MODE_30D}`. intervals `[1,3,7]/[1,3,7,14]/[1,3,7,14,21]` (`src/main/java/com/example/thirdtool/UserSchedule/domain/model/LearningMode.java`).
  - `ReviewCommandService.incrementViewAndHandleMaxView()`가 세션 진행 중 인라인으로 MAX_VIEW archive 결정.
  - 야간 `CardExpiryBatchService`가 MAX_DURATION archive 결정.
  - Mode 변경 시 기존 card 처리 규칙 미정의 — `UserScheduleConfig.updateMode()` 호출이 이미 진행 중이던 card의 스케줄에 어떻게 영향 주는지 불명확.
- 발생하는 문제
  - AI roadmap 리팩토링(이슈 #15~#20) 결과 Card = 사용자가 진짜 모르는 것(오답노트)만 저장이라는 컨셉이 명확해짐 → lifecycle도 이 컨셉에 맞춰 재정의 필요.
  - `OnFieldBudget` 이중 게이트는 fixed interval queue 컨셉과 개념 중복 (interval 마지막 도달 = archive이므로 maxDuration 별도 관리 불필요, viewCount는 관찰 지표 이상 의미 없음).
  - 사용자 mode 변경 시 "진행 중 card"의 스케줄이 어떻게 되는지 미정 → 사용자 UX 예측 불가.
  - 카드 노출·미노출 이력 부재 → 사용자 학습 캐시 용량 측정 불가 (`product-review.md` 목적 달성 불가).
- 왜 지금 해결해야 하는가
  - AI roadmap 리팩토링(이슈 #15~#20)이 학습 대상 구조를 확정 → 그 아래 일일 학습 루틴 재정의가 다음 단계.
  - 초기 3명 사용자 규모라 breaking change 감내 가능.
  - `product-review.md` 신설과 동시에 Card lifecycle도 갱신해야 정합.

## 목표 (To-Be)

- **`OnFieldBudget` 이중 게이트 폐기** — fixed interval queue로 완전 대체. `maxView`·`maxDuration` 개념 제거 (이슈 #22).
- **Card lifecycle 트리거 재정의**: `MANUAL / SCHEDULE_EXHAUSTED / MODE_DOWNGRADED` 3개. 기존 `MAX_VIEW`·`MAX_DURATION` 폐기.
- **Card에 `createdMode: LearningMode` 필드 추가** — M3 하이브리드 규칙의 근거. `min(card.createdMode.max, user.currentMode.max)`로 effectiveMax 계산 (이슈 #23).
- **Interval ladder 고정 (1,3,7,14,28,60)** — Mode = `MODE_7D/14D/28D/60D` 4개로 재편 (이슈 #21).
- **`returnToField` fresh 재시작** — archive → field 복귀 시 `createdMode = 사용자 현재 mode`, `enteredFieldAt = 지금`.
- **Cornell 노트 구조 유지** — MainNote (text/image), Summary, KeywordCue (≥1개), Tag (≤3개, 시스템 unique) 그대로.
- **관찰 지표 로깅 강화** — mode별 실측 완료율, `MODE_DOWNGRADED` archive 비율, view 시각 등 (`product-review.md` 데이터 소스).

## 설계 결정 (Design Decisions)

> 큰 갈림길의 결정. 거부된 옵션도 합리적 근거가 있었음을 명시.

- **Card lifecycle은 fixed interval queue (vs 이중 게이트 유지)**
  - 이중 게이트(maxView+maxDuration)는 fixed interval 원칙과 개념 중복 — interval 마지막 도달 = archive 하나로 통일.
  - `viewCount`·`lastViewedAt`는 관찰 지표로 유지, lifecycle 결정에는 무관.
- **Card는 `createdMode: LearningMode` enum 하나만 저장 (F1)**
  - Card에 intervals list를 스냅샷 저장(F2)하는 대안은 스키마 부담·마이그레이션 복잡.
  - Mode enum이 안정적이라는 전제 (튜닝은 별도 릴리스 단위로 통제).
- **Mode 변경 시 M3 하이브리드 (down=cap, up=새 card만)**
  - 완전 글로벌(M1) — up 시 이미 archive될 예정이던 card가 되살아나는 UX 이상.
  - 완전 frozen(M2) — down 시 load 즉시 감소 안 되어 사용자 의도 배치.
  - M3의 비대칭 규칙 = down/up 각각의 사용자 의도 정확 반영.
- **Interval ladder는 v1 고정 (D1)**
  - 커스터마이징(D3) — 운영 난이도 급증. 사용자 우려 명시적.
  - 프리셋 여러 개(D2) — 관찰 데이터 없이 프리셋 세트 결정 어려움.
- **`returnToField` fresh 재시작**
  - 재학습 = 새 학습 계약. 사용자 현재 mode 반영이 자연.
- **Cornell 노트 구조는 유지**
  - MainNote / Summary / KeywordCue / Tag 구조는 이번 리팩토링 대상 아님. 안정.

## 대안 검토 (Alternatives Considered)

### 갈림길 A. Mode 변경 시 기존 card 처리

**Option M1 — 완전 글로벌 (모든 card가 현 user mode 따름)**
- 장점: 스키마 심플 (Card에 mode 필드 X). Down/up 즉시 반영.
- 거부 이유: up 시 이미 archive됐어야 할 card가 되살아나는 UX 이상.

**Option M2 — 카드별 frozen (생성 시점 mode 스냅샷)**
- 장점: 예측 가능. Card 계약 안정.
- 거부 이유: down 시 load 즉시 감소 안 됨 → 사용자 down 의도와 배치 ("힘들어서 down"인데 여전히 14일차 카드가 옴).

**Option M3 (선택) — 하이브리드 (down=cap, up=새 card만)**
- 비용: 규칙 설명 필요 (프론트 UX 문구). 비대칭 규칙 학습.
- 보상: down/up 각각의 사용자 의도 정확 반영. 자율성 + load 통제 양립.

### 갈림길 B. Card가 스케줄을 어떻게 저장하나

**Option F1 (선택) — createdMode enum 저장**
- 비용: Mode enum 조정이 기존 card 스케줄에 영향. 통제된 릴리스 필요.
- 보상: 스키마 최소 (컬럼 1개). 마이그레이션 단순.

**Option F2 — createdIntervals 스냅샷 통짜**
- 장점: Mode enum 조정에도 기존 card 불변.
- 거부 이유: 스키마 부담 (JSON 컬럼 또는 별도 테이블). v1 초기 3명 규모엔 오버스펙.

**Option F3 — Card는 저장 안 함, 시각으로만 판정**
- 장점: Card 스키마 무변경.
- 거부 이유: Fragile — mode 변경 이력 별도 테이블 필요. 조회 비용·복잡도 증가.

### 갈림길 C. `OnFieldBudget` 폐기 여부

**Option (선택) — 완전 폐기**
- 비용: `CardExpiryPolicy`·`CardExpiryBatchService`·`ReviewCommandService` 세 지점 재작성. `ArchiveReason` enum 재정의 + DB 마이그레이션.
- 보상: 개념 단순화. Fixed interval queue 하나로 통일.

**Option — 유지 병행**
- 거부 이유: 이중 게이트 + fixed interval 병행 시 개념 이중화. 사용자 요구("queue 느낌", "누적 없음")와 배치.

### 갈림길 D. Interval ladder 유연성

**Option D1 (선택) — v1 고정 (1,3,7,14,28,60)**
- 비용: 커스텀 원하는 사용자에 제약.
- 보상: 운영 난이도 최저. 초기 3명 관찰 데이터로 v2 프리셋 근거 확보.

**Option D2 — 프리셋 여러 개**
- 거부 이유: 프리셋 세트 자체를 관찰 데이터 없이 결정 어려움. Mode enum 값 폭증.

**Option D3 — 완전 커스텀**
- 거부 이유: Mode 개념 붕괴. F1과 배치. 사용자 우려("운영 난이도 급증") 명시.

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 배치

```
┌──────────────────────────────────────────────────────────────┐
│ Presentation                                                 │
│   CardController                                             │
│   ├ POST /cards                       (Cornell 노트 생성)     │
│   ├ PATCH /cards/{id}                 (편집)                  │
│   ├ POST /cards/{id}/archive          (수동 archive)          │
│   └ POST /cards/{id}/return-to-field  (fresh 재시작)          │
├──────────────────────────────────────────────────────────────┤
│ Application                                                  │
│   CardCommandService                                         │
│   ├ createCard(userId, axisId, ..., userCurrentMode)         │
│   ├ archive(userId, cardId, reason)                          │
│   └ returnToField(userId, cardId, userCurrentMode, today)    │
│   CardQueryService (조회 전용)                                │
├──────────────────────────────────────────────────────────────┤
│ Domain                                                       │
│   Card (Aggregate Root)                                      │
│   ├ createdMode: LearningMode                    ★ 신규       │
│   ├ enteredFieldAt: LocalDate                                │
│   ├ status: {ON_FIELD, ARCHIVE}                              │
│   ├ MainNote / Summary / KeywordCue[] / CardTag[] (유지)     │
│   ├ effectiveMaxDays(userCurrentMode): int       ★ 신규       │
│   ├ effectiveIntervals(userCurrentMode): List<Integer>       │
│   ├ isDueOn(today, userCurrentMode): boolean                 │
│   ├ hasScheduleExhausted(userCurrentMode, today): boolean    │
│   └ returnToField(userCurrentMode, today): void              │
│                                                              │
│   CardStatusHistory (append-only 이력)                        │
│   └ ArchiveReason {MANUAL, SCHEDULE_EXHAUSTED, MODE_DOWNGRADED} ★ 재편 │
│                                                              │
│   CardStatusHistoryAppender (Domain Service)                 │
├──────────────────────────────────────────────────────────────┤
│ Infrastructure                                               │
│   CardJpaRepository                                          │
│   CardStatusHistoryJpaRepository                             │
└──────────────────────────────────────────────────────────────┘
```

### 핵심 플로우

**1. 카드 생성 (오답노트 저장)**
```
Client → POST /cards { axisId, mainNote, summary, keywords, tags }
Controller → CardCommandService.createCard(userId, axisId, ..., userCurrentMode)
Service   ├ UserScheduleQueryService.currentMode(userId) → LearningMode
          ├ Card.create(..., createdMode = userCurrentMode) [enteredFieldAt=today, status=ON_FIELD]
          ├ CardRepository.save(card)
          └ return CardResponse
Client ◀ 201 { cardId, createdMode }
```

**2. Mode 변경 후 자연 down cap (다음 batch 생성 시 lazy)**
```
Day D:  User mode = MODE_14D  → cards with createdMode=MODE_14D 생성
Day D+3: User가 MODE_7D로 다운
Day D+3 사용자 첫 접근 → DailyLearningBatch.generateFor(userId, today, userCurrentMode=MODE_7D)
         ├ 각 card에 대해:
         │   effectiveMax = min(card.createdMode.maxDays=14, userMode.maxDays=7) = 7
         │   card.hasScheduleExhausted(MODE_7D, today) ?
         │      ├ 이미 7일 넘긴 카드 → batch 제외 + Application Service가 archive(MODE_DOWNGRADED)
         │      └ 안 넘긴 카드 → isDueOn(today, MODE_7D)로 batch 담김 여부 판정
         └ Batch entries 확정
```

**3. `returnToField` fresh 재시작**
```
Client → POST /cards/{id}/return-to-field
Controller → CardCommandService.returnToField(userId, cardId, userCurrentMode, today)
Service   ├ card = repository.findById(cardId) [status=ARCHIVE]
          ├ card.returnToField(userCurrentMode, today)
          │   ├ status = ON_FIELD
          │   ├ enteredFieldAt = today
          │   ├ createdMode = userCurrentMode  (fresh)
          │   ├ viewCount = 0
          │   └ lastViewedAt = null
          ├ historyAppender.append(card, ARCHIVE → ON_FIELD, reason=null)
          └ repository.save(card)
```

### Out-of-Process 의존

- **UserSchedule BC**: `UserScheduleQueryService.currentMode(userId)` — Card 생성·`returnToField`·batch 판정 시 사용자 현재 mode 조회 위임.
- **LearningFacade BC**: `LearningAxisQueryService.exists(axisId)` — Card 생성 시 axis 유효성 검증 (이슈 #07 이후 axis 직접 매핑).
- **Review BC (Product review)**: Card 도메인 메서드(`isDueOn`, `hasScheduleExhausted`)를 `DailyLearningBatch.generateFor()`가 호출 → batch 큐 구성.

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | ErrorCode | HTTP | 클라이언트 권장 동작 |
| --- | --- | --- | --- |
| 카드 생성 시 axis 미존재 | `AXIS_NOT_FOUND` | 404 | axis 선택 재요청 |
| Cornell MainNote text/image 둘 다 empty | `CARD_MAIN_NOTE_EMPTY` | 400 | 필드 입력 UI 강조 |
| Summary blank | `CARD_SUMMARY_EMPTY` | 400 | 요약 입력 UI 강조 |
| Keyword 미입력 (최소 1개 요구) | `CARD_KEYWORD_MIN_REQUIRED` | 400 | keyword 최소 1개 강제 UI |
| Tag 4개 이상 첨부 | `CARD_TAG_LIMIT_EXCEEDED` | 400 | tag 선택 UI에 3개 제한 |
| Card 조회 시 미존재 or 소유권 불일치 | `CARD_NOT_FOUND` | 404 | 카드 목록으로 이동 |
| Archive 된 card에 `recordView()` | 무시 (idempotent, 도메인 no-op) | — | — |
| 이미 ARCHIVE인 카드 `archive()` | 무시 (idempotent) | — | — |
| 이미 ON_FIELD인 카드 `returnToField()` | 무시 (idempotent) | — | — |

### 로깅 정책

- **항상 기록**:
  - Card 생성 이벤트 (`cardId`, `userId`, `axisId`, `createdMode`, `createdAt`)
  - Archive 이벤트 (`cardId`, `userId`, `reason`, `fromStatus`, `toStatus`, `changedAt`)
  - `returnToField` 이벤트 (`cardId`, `userId`, `newCreatedMode`, `newEnteredFieldAt`)
  - Card 조회 시 소유권 위반 시도 (`userId`, `cardId`, `path`)
- **debug**: 카드 편집 diff (변경 필드·값). PII 최소화.
- **절대 금지**:
  - 사용자 컨텐츠 원문의 통째 로깅 (MainNote 텍스트, imageUrl) — 스니펫 100자 이하로만
  - 사용자 이메일·전화번호 등 PII 노출 (userId만 사용)

### 관측 지표

- `thirdtool.card.created_total{mode}` — Counter — Mode별 카드 생성량
- `thirdtool.card.archived_total{reason}` — Counter — Reason별 archive 건수 (`MANUAL`/`SCHEDULE_EXHAUSTED`/`MODE_DOWNGRADED`)
- `thirdtool.card.effective_max_days_histogram` — Histogram — Batch 시점 effectiveMax 분포
- `thirdtool.card.view_count_at_archive_histogram` — Histogram — Archive 시점 viewCount (관찰 지표)
- `thirdtool.card.return_to_field_total` — Counter — `returnToField` 이벤트

## 롤아웃 / 마이그레이션 (Rollout)

### 전제

- 초기 3명 사용자 규모. 트래픽 극소량 — 데이터 마이그레이션 breaking 감내 가능.
- 이슈 #21 (Mode enum 재편) → 이슈 #22 (OnFieldBudget 폐기) → 이슈 #23 (Card `createdMode` 필드) **동일 릴리스에서 함께** — 부분 반영 시 lifecycle 판정 불가.

### Product 의존성

- **선행**: `product-learning-tower.md` (Layer/Axis/Roadmap/Selection 구조, Card axis 직접 매핑 #07)
- **동시**: `product-review.md` (Daily batch·Review 세션 — `Card.isDueOn`/`hasScheduleExhausted` 소비)
- **후행**: `product-op.md` (관측 지표 대시보드)

### Epic·Story 의존성 그래프

```
Epic 1 (Mode enum 재편)  ──►  Epic 2 (OnFieldBudget 폐기)  ──►  Epic 3 (Card createdMode + M3)
     │                              │                              │
     └─► Story 1-1 → 1-2 → 1-3       └─► Story 2-1 ~ 2-6            └─► Story 3-1 → 3-2 → 3-3 → 3-4 → 3-5
                          │                    │                              │
                          └─► Story 1-4        └─► ArchiveReason 이관         └─► M3 시나리오 테스트
                          └─► Story 1-5           (2-3) 완료 후에만
                                                  Epic 3 시작 가능

병렬 가능: Epic 1 Story 1-4, 1-5는 Story 1-1~1-3와 병렬
```

### 환경별 설정 분기

| 항목 | dev | prod |
| --- | --- | --- |
| DB | H2 (in-memory) | MySQL 8.0 + RDS |
| Flyway 실행 | 매 부팅 clean+migrate | 배포 파이프라인에서 migrate만 |
| `ArchiveReason` CHECK 제약 | H2 CHECK 지원 | MySQL CHECK 지원 |
| Mode enum 재편 마이그레이션 | 자동 (H2 clean) | Flyway V{N} 배포 시 1회 |
| `Card.createdMode` backfill | 새 DB이므로 backfill 대상 없음 | 기존 카드 backfill UPDATE 필수 |

## 성공 지표 (KPI)

| 지표 | 목표 값 | 측정 방법 |
| --- | --- | --- |
| 카드 archive reason 분포 | `SCHEDULE_EXHAUSTED` > 60% (자연 만료가 다수) | `thirdtool.card.archived_total{reason}` 30일 롤링 |
| `MODE_DOWNGRADED` 비율 | 관찰 (v1 baseline 없음) | 대시보드 (`product-review.md`) |
| Card 생성 후 첫 노출까지 latency | ≤ 24시간 (1일차 interval) | Daily batch 로그 vs card.created_at |
| Card 도메인 단위 테스트 커버리지 | ≥ 90% | JaCoCo (Card BC domain 스코프) |
| `OnFieldBudget` 참조 잔존 | 0건 | Grep `OnFieldBudget` 리팩토링 완료 후 |

## Scope

**In Scope**:
- Card lifecycle 재정의 (Fixed interval queue)
- `createdMode` 필드 + M3 하이브리드 규칙
- `OnFieldBudget` 폐기 및 관련 코드 제거
- `ArchiveReason` 재편 + 데이터 마이그레이션
- `returnToField` fresh 재시작
- Mode enum 재편 (`MODE_7D/14D/28D/60D`)

**Out of Scope**:
- Daily batch·Review 세션 — `product-review.md`에 분리
- 대시보드·주간 요약 — `product-review.md`에 분리
- Card 검색·필터 개선 — `product-search.md`
- 미디어 업로드 (이미지) — `product-media.md`
- Cornell 노트 UI 개편 — 이번 리팩토링 대상 아님, 안정 유지
- 자동 mode 조정 — v2 이관

## 대상 사용자

- **오답노트 학습자 (내부 테스트 3명)** — 모르는 것만 저장하고 규칙적으로 다시 보고 싶은 사람. 학습 자료(video·notion·AI)는 외부에서 준비하고 우리 시스템은 "다시 봐야 할 것" 규칙적 노출만.
- **자기 학습 캐시 크기 궁금해하는 사람** — mode 다운 시 즉시 load 감소·up 시 예측 가능한 확장을 원함.

## 연결된 Epic 목록

- [ ] Epic 1: Mode enum 재편 (이슈 #21)
- [ ] Epic 2: OnFieldBudget 폐기 + `ArchiveReason` 재편 (이슈 #22)
- [ ] Epic 3: Card `createdMode` 필드 + M3 하이브리드 (이슈 #23)

## 관련 문서

- 의존 Product:
  - `workflow/task/pes/workspectrum/sdd/in-progress/product-learning-tower.md` — Layer/Axis/Roadmap/Selection 구조
  - `workflow/task/pes/workspectrum/sdd/in-progress/product-review.md` — Daily batch가 Card 도메인 메서드 소비
- 참조 (원본 v0.0.1): `workflow/task/pes/workspectrum/sdd/done/versions/0.0.1v/product-card.md`
- 관련 이슈: `workflow/task/fix/brainstorming/version/0.0.2v/issue-21` ~ `#23`
- 관련 ADR: ADR002 (Enum 저장), ADR003 (Soft Delete 적용), ADR021 (Card·Deck·Axis 통합 결정) — 본 리팩토링으로 ADR 신설 검토 (`ADR-CANDIDATES.md` 등록 예정)
- DOMAIN.md / PACKAGE.md 갱신 예정: `docs/DOMAIN.md` §Card 섹션 (createdMode·M3 규칙 추가), 패키지 규칙 무변경

## 열린 질문 (Open Questions)

1. **`Card.viewCount` 필드 유지 여부** — 관찰 지표로 유지 vs 폐기. v1 default: 유지. 대시보드(`product-review.md`) 활용 후 재검토.
2. **Card 도메인의 `axisId` 직접 매핑 이슈 #07 완료 여부** — 완료 전이면 병렬 진행 or 이후 단계로 이관 결정 필요.
3. **대량 mode 변경 시 성능** — 사용자 카드 수 수천 규모 도달 시 lazy batch 생성에서 `hasScheduleExhausted` 스캔 비용 증가. 초기 3명 규모엔 무의미하지만 v2 튜닝 지점.
4. **`SCHEDULE_EXHAUSTED` vs `MODE_DOWNGRADED` reason 판정 규칙 명확화** — batch 생성 시 두 reason 판정을 `card.createdMode.max > user.currentMode.max` 조건으로 분기. 엣지 케이스(둘 다 해당) 다중 사유 로깅 여부.

## 제품 수준 완료 기준 (Product-level DoD)

- [ ] 이슈 #21, #22, #23 모두 구현·테스트 완료
- [ ] `OnFieldBudget` 관련 코드 전체 제거 (`Grep OnFieldBudget` 결과 0건)
- [ ] `ArchiveReason` enum 재정의 + DB CHECK 재작성 + 기존 history 데이터 마이그레이션 성공
- [ ] Card 도메인 단위 테스트 — M3 하이브리드 down/up 시나리오 명시적 검증
- [ ] `Card.createdMode` 컬럼 3단계 Flyway 마이그레이션 (nullable → backfill → NOT NULL) 무사고
- [ ] Reviewer 5관점 세션 통과
- [ ] `product-review.md`·이슈들과 정합 검증

---

# [Epic 1] Mode enum 재편 (MODE_7D/14D/28D/60D)

## 목표

`LearningMode` enum을 4개 값(`MODE_7D`, `MODE_14D`, `MODE_28D`, `MODE_60D`)으로 재편해 fixed interval ladder (1,3,7,14,28,60)와 정합시키고, 기존 `MODE_10D/20D/30D` 데이터를 안전 이관한다.

## 배경

이슈 #15~#20의 AI roadmap 리팩토링이 학습 대상 구조를 확정했지만, `LearningMode`는 여전히 `[1,3,7,14,21]`처럼 ladder와 정합되지 않은 마지막 interval(21)을 갖고 있음. `product-card.md` 리팩토링 시작점에서 mode를 새 ladder와 맞춰야 후속 Epic이 자란다.

## 포함 Story

- Story 1-1: `LearningMode` enum 재정의 (4개 값, intervals 필드만)
- Story 1-2: `LearningModeMappingPolicy` threshold 재작성
- Story 1-3: Flyway `V{N}__reorganize_learning_mode.sql` + 롤백
- Story 1-4: `UserScheduleConfig.resolveOnFieldBudget()` 시그니처 폐기 → `currentMode(userId)` 축소
- Story 1-5: `raw_input_days` 60일 상한 clamp 로직

## Epic 인수 시나리오

- Given user_schedule_config에 `mapped_mode = 'MODE_20D'` 사용자 존재
- When `V{N}__reorganize_learning_mode.sql` 실행
- Then 해당 row가 `mapped_mode = 'MODE_14D'`로 이관 + CHECK 제약이 새 값 4개만 허용

*(엣지)* Given raw_input_days=100 입력 / When `LearningModeMappingPolicy.resolve(100)` / Then `MODE_60D` 반환 + 정보성 로그 (상한 clamp)

## Epic 완료 기준 (DoD)

- [ ] 포함 Story 모두 완료
- [ ] 기존 `MODE_10D/20D/30D` 참조 코드 전체 제거 (Grep 검증)
- [ ] Flyway 마이그레이션 + 롤백 스크립트 검증
- [ ] `LearningModeMappingPolicyTest` 새 threshold 테스트 통과
- [ ] 마이그레이션 데이터 정합 (10D→7D, 20D→14D, 30D→28D) 스팟 체크

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **Interval ladder 6-step 고정 vs 사용자 커스터마이징**: 6-step 고정 (D1, 이슈 #21 결정). 운영 난이도 급증 우려 반영.
- **Mode enum 값 개수**: 4개(7/14/28/60D). 사용자 mental model 상 "몇 일 유지"의 자연 breakpoint. 3개(짧음/중간/김)는 세분화 부족, 5개+는 mode 스위치 복잡.

## [Story 1-1] `LearningMode` enum 재정의 (4개 값, intervals 필드만)

### User Story
- As a Card BC 개발자
- I want `LearningMode`가 `MODE_7D/14D/28D/60D` 4개 값을 갖고 `intervals: List<Integer>` 필드만 노출하기를
- so that fixed ladder (1,3,7,14,28,60)와 정합 + `maxView`·`maxDuration` 폐기 준비

### 설명
`UserSchedule/domain/model/LearningMode.java` 재작성. 기존 `maxView`·`maxDuration` 필드 삭제. `intervals: List<Integer>` (immutable) + `maxDays()`, `stepCount()` 파생 메서드.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.UserSchedule.domain.model.LearningMode` — enum 재정의

**주요 메서드**:
- `LearningMode.intervals(): List<Integer>` — 노출 순차 (immutable)
- `LearningMode.maxDays(): int` — 마지막 interval 값
- `LearningMode.stepCount(): int` — climb 단계 수

### 완료 기준 (AC)
- Given `LearningMode.MODE_7D` / When `intervals()` / Then `[1, 3, 7]`
- Given `LearningMode.MODE_14D` / When `maxDays()` / Then `14`
- Given `LearningMode.MODE_60D` / When `stepCount()` / Then `6`
- *(엣지)* Given `LearningMode.MODE_7D` / When intervals().add(99) 시도 / Then `UnsupportedOperationException` (immutable)

### Definition of Done
- [ ] 구현 (`LearningMode.java` 4개 값, `maxView`·`maxDuration` 필드 삭제)
- [ ] 단위 테스트 (`LearningModeTest`, 4개 값 × 3개 메서드 × 해피 경로 + immutable 예외)
- [ ] ErrorCode 등록 없음 (enum 재정의)
- [ ] Flyway 미변경 (Story 1-3에서 처리)

### 스토리 포인트
0.5d

### 의존성
- 선행: 없음
- 후행: Story 1-2 (mapping policy가 새 enum 값 참조), Story 1-3 (Flyway가 새 값 CHECK 등록)

## [Story 1-2] `LearningModeMappingPolicy` threshold 재작성

### User Story
- As a User BC 개발자
- I want `LearningModeMappingPolicy.resolve(rawInputDays)`가 새 4개 mode에 매핑되기를
- so that 사용자가 "며칠 유지"를 입력하면 새 mode enum으로 변환

### 설명
`UserSchedule/domain/model/LearningModeMappingPolicy.java` 재작성. threshold: `≤7 → 7D` / `8~14 → 14D` / `15~28 → 28D` / `29~60 → 60D`. 60 초과는 60으로 silently clamp + 로그.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.UserSchedule.domain.model.LearningModeMappingPolicy`

**주요 메서드**:
- `LearningModeMappingPolicy.resolve(int rawInputDays): LearningMode`

### 완료 기준 (AC)
- Given rawInputDays=7 / When `resolve(7)` / Then `MODE_7D`
- Given rawInputDays=8 / When `resolve(8)` / Then `MODE_14D`
- Given rawInputDays=15 / When `resolve(15)` / Then `MODE_28D`
- Given rawInputDays=29 / When `resolve(29)` / Then `MODE_60D`
- *(엣지)* Given rawInputDays=100 / When `resolve(100)` / Then `MODE_60D` + 정보성 로그 (60 clamp)
- *(예외)* Given rawInputDays=0 / When `resolve(0)` / Then `USER_SCHEDULE_INPUT_TOO_SHORT` 400

### Definition of Done
- [ ] 구현 (`LearningModeMappingPolicy.java` threshold 재작성)
- [ ] 단위 테스트 (`LearningModeMappingPolicyTest`, 각 boundary + clamp + 예외)
- [ ] ErrorCode 등록 (`USER_SCHEDULE_INPUT_TOO_SHORT` 400)

### 스토리 포인트
0.5d

### 의존성
- 선행: Story 1-1
- 후행: Story 1-3

## [Story 1-3] Flyway `V{N}__reorganize_learning_mode.sql` + 롤백

### User Story
- As a 운영자
- I want Flyway 마이그레이션이 기존 `user_schedule_config.mapped_mode` 값을 안전 이관하기를
- so that 3명 사용자 데이터 손실 없이 새 enum 값으로 전환

### 설명
`src/main/resources/db/migration/V{N}__reorganize_learning_mode.sql` 신설. 3단계: (1) UPDATE로 값 이관 (10D→7D, 20D→14D, 30D→28D), (2) CHECK 제약 변경 (새 4개 값만 허용). 롤백 스크립트 동반.

**핵심 파일**:
- `V{N}__reorganize_learning_mode.sql`
- `R{N}__rollback_reorganize_learning_mode.sql`

### 완료 기준 (AC)
- Given `mapped_mode='MODE_10D'` row 3건 / When migrate / Then 3건 모두 `MODE_7D`
- Given `mapped_mode='MODE_20D'` row / When migrate / Then `MODE_14D`
- Given `mapped_mode='MODE_30D'` row / When migrate / Then `MODE_28D`
- Given `mapped_mode='MODE_INVALID'` row (외부 오염) / When migrate / Then CHECK 위반 예외 → 마이그레이션 롤백
- *(엣지)* migrate 후 CHECK 제약이 `IN ('MODE_7D','MODE_14D','MODE_28D','MODE_60D')`

### Definition of Done
- [ ] 구현 (V·R 두 스크립트)
- [ ] Slice 테스트 (`@DataJpaTest`) — 마이그레이션 후 CHECK 위반 시도 검증
- [ ] 통합 테스트 (`@SpringBootTest`) — 롤백 스크립트 성공 검증

### 스토리 포인트
1d

### 의존성
- 선행: Story 1-1, 1-2
- 후행: Story 1-4, 1-5

## [Story 1-4] `UserScheduleConfig.resolveOnFieldBudget()` 시그니처 폐기 → `currentMode(userId)` 축소

### User Story
- As a Card BC 개발자
- I want `UserScheduleQueryService.currentMode(userId): LearningMode`만 남기고 `resolveOnFieldBudget()`을 제거하기를
- so that OnFieldBudget 폐기(Epic 2)의 전제 만족

### 설명
`UserSchedule/application/service/UserScheduleQueryService.java` 시그니처 정리. `resolveOnFieldBudget(userId)` 삭제. 호출부 (Review, Batch)는 Epic 2·3에서 새 시그니처로 재배선.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.UserSchedule.application.service.UserScheduleQueryService`

**주요 메서드**:
- `currentMode(userId: Long): LearningMode` — 유지·명세 명시
- ~~`resolveOnFieldBudget(userId)`~~ — 삭제

### 완료 기준 (AC)
- Given user_schedule_config에 mapped_mode='MODE_14D' 사용자 / When `currentMode(userId)` / Then `LearningMode.MODE_14D`
- Given user_schedule_config 없는 신규 사용자 / When `currentMode(userId)` / Then lazy 생성 후 default `MODE_14D` (중간값)
- *(엣지)* 삭제된 `resolveOnFieldBudget` 호출부가 없어야 함 (Grep 검증)

### Definition of Done
- [ ] 구현 (`UserScheduleQueryService` 시그니처 축소)
- [ ] 슬라이스 테스트 (@WebMvcTest는 대상 X, 도메인 단위 + Repository 슬라이스)
- [ ] 호출부 (ReviewCommandService, CardExpiryBatchService) 삭제·재배선은 Epic 2에서

### 스토리 포인트
0.5d

### 의존성
- 선행: Story 1-1
- 후행: Epic 2 Story 2-4 (ReviewCommandService 재배선), Epic 3 Story 3-4 (Card 생성 시 사용)

## [Story 1-5] `raw_input_days` 60일 상한 clamp 로직 + 정보성 응답

### User Story
- As a 사용자
- I want mode 설정 시 100일 등 상한 초과 입력해도 60일로 clamp되면서 안내를 받기를
- so that 시스템 상한 인지 + mode 설정 UX 마찰 최소

### 설명
`UserScheduleCommandService.updateMode(userId, rawInputDays)`가 60 초과 시 60으로 clamp하고 응답 DTO에 `wasClamped: true` 포함.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.UserSchedule.application.service.UserScheduleCommandService`
- `UserScheduleUpdateResponse(mode: LearningMode, rawInputDays: int, wasClamped: boolean)`

**주요 메서드**:
- `UserScheduleCommandService.updateMode(userId, rawInputDays): UserScheduleUpdateResponse`

### 완료 기준 (AC)
- Given rawInputDays=100 / When `updateMode(userId, 100)` / Then response `{mode: MODE_60D, rawInputDays: 60, wasClamped: true}`
- Given rawInputDays=30 / When `updateMode(userId, 30)` / Then response `{mode: MODE_60D, rawInputDays: 30, wasClamped: false}`
- *(예외)* rawInputDays=0 or 음수 / Then `USER_SCHEDULE_INPUT_TOO_SHORT` 400

### Definition of Done
- [ ] 구현 (`UserScheduleCommandService` clamp + `UserScheduleUpdateResponse` DTO)
- [ ] 슬라이스 테스트 (`@WebMvcTest` — 응답 DTO wasClamped 검증)
- [ ] ErrorCode `USER_SCHEDULE_INPUT_TOO_SHORT` 등록

### 스토리 포인트
1d

### 의존성
- 선행: Story 1-2
- 후행: FE UX

---

# [Epic 2] OnFieldBudget 폐기 + ArchiveReason 재편

## 목표

`OnFieldBudget(maxView, maxDuration)` 이중 게이트를 완전 폐기하고 lifecycle 트리거를 `MANUAL / SCHEDULE_EXHAUSTED / MODE_DOWNGRADED` 3개로 재편해 fixed interval queue와 정합시킨다.

## 배경

이슈 #22의 핵심. 이중 게이트가 fixed interval 원칙과 개념 중복이고, `viewCount` 카운트가 lifecycle 결정에 관여할 이유 없음. 이 Epic이 완료돼야 Epic 3(Card `createdMode`)이 새 lifecycle을 담을 수 있다.

## 포함 Story

- Story 2-1: `OnFieldBudget` VO 클래스 삭제 + 모든 참조부 제거
- Story 2-2: `CardExpiryPolicy`·`CardExpiryBatchService` 폐기 (또는 축소)
- Story 2-3: `ArchiveReason` enum 재정의 + `CardStatusHistory.reason` DB CHECK 재작성 + 기존 데이터 마이그레이션
- Story 2-4: `ReviewCommandService.incrementViewAndHandleMaxView()` 재작성 (MAX_VIEW 인라인 archive 제거)
- Story 2-5: Flyway `V{N}__abolish_onfieldbudget.sql`
- Story 2-6: 테스트 코드 재작성 (MAX_VIEW/MAX_DURATION 기반 → SCHEDULE_EXHAUSTED 기반)

## Epic 인수 시나리오

- Given ARCHIVE 상태 카드 (원인=MAX_VIEW로 기록) / When 마이그레이션 실행 / Then reason='SCHEDULE_EXHAUSTED'로 이관 + CHECK 제약 통과
- Given ON_FIELD 카드에 `card.recordView()` 반복 호출 / When viewCount=100 도달 / Then 여전히 ON_FIELD (자동 archive 없음)
- Given `OnFieldBudget` import 문 / When 컴파일 / Then 컴파일 오류 (클래스 부재)

## Epic 완료 기준 (DoD)

- [ ] `OnFieldBudget` 참조 코드 전체 제거 (Grep 결과 0)
- [ ] `ArchiveReason` 신 3개 값만 코드에서 사용 (`MAX_VIEW`·`MAX_DURATION` 참조 0)
- [ ] `CardStatusHistory.reason` DB CHECK 재작성 + 기존 데이터 이관
- [ ] Flyway 마이그레이션 + 롤백 성공
- [ ] Card·Review 단위 테스트 재작성 (해피/엣지/예외 각각 대체 케이스)

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **MAX_VIEW/MAX_DURATION history 이관 방식**: 두 이전 값 모두 `SCHEDULE_EXHAUSTED`로 통합. 세분화(MAX_VIEW→SCHEDULE_EXHAUSTED_BY_VIEW 등)는 이력 세밀도 vs 스키마 복잡도 트레이드오프에서 세밀도 손실 감수.
- **`viewCount`·`lastViewedAt` 필드 유지 여부**: 관찰 지표로 유지 (Card 도메인엔 남지만 lifecycle 결정 무관).

## [Story 2-1] `OnFieldBudget` VO 클래스 삭제 + 참조부 제거

### User Story
- As a Card BC 개발자
- I want `OnFieldBudget` VO와 모든 참조부가 코드에서 사라지기를
- so that 이중 게이트 개념 흔적 없이 fixed interval queue 하나로 통일

### 설명
`Card/domain/model/OnFieldBudget.java` 삭제. `Card`·`CardCommandService`·`ReviewCommandService`·`CardExpiryBatchService` 등의 참조 모두 정리. 컴파일 오류가 참조 지점 안내.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Card.domain.model.OnFieldBudget` — 삭제

### 완료 기준 (AC)
- Given OnFieldBudget import 문 / When compile / Then 오류 (클래스 부재)
- Given `Grep OnFieldBudget src/main/java` / When 실행 / Then 결과 0건

### Definition of Done
- [ ] 삭제 (`OnFieldBudget.java`)
- [ ] 참조부 정리 (`Grep OnFieldBudget src/main` 0건 확인)
- [ ] 단위 테스트 (`OnFieldBudgetTest` 삭제)

### 스토리 포인트
1d

### 의존성
- 선행: Epic 1 완료 (LearningMode에서 maxView/maxDuration 필드 삭제 완료)
- 후행: Story 2-2, 2-4

## [Story 2-2] `CardExpiryPolicy`·`CardExpiryBatchService` 폐기 또는 축소

### User Story
- As a Card BC 개발자
- I want `CardExpiryPolicy`·`CardExpiryBatchService` 야간 배치 로직이 사라지기를
- so that Archive 결정을 `DailyLearningBatch.generateFor()` (product-review.md)가 lazy로 소유

### 설명
두 클래스 삭제. 야간 배치 대신 사용자 첫 접근 시 `DailyLearningBatch.generateFor()`가 `hasScheduleExhausted` 감지 → Application Service가 archive orchestration.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Card.domain.service.CardExpiryPolicy` — 삭제
- `com.example.thirdtool.Card.application.service.CardExpiryBatchService` — 삭제

### 완료 기준 (AC)
- Given `CardExpiryBatchService` 참조 / When compile / Then 오류
- Given `@Scheduled` cron job `card.expiry.cron` 설정 / When 서버 부팅 / Then 관련 cron 실행 안 됨 (bean 삭제)

### Definition of Done
- [ ] 삭제 (`CardExpiryPolicy.java`, `CardExpiryBatchService.java`)
- [ ] `application.yml`에서 `card.expiry.cron` 설정 제거
- [ ] 관련 테스트 삭제 (`CardExpiryBatchServiceTest`)

### 스토리 포인트
1d

### 의존성
- 선행: Story 2-1
- 후행: `product-review.md` Epic 1 (DailyLearningBatch가 archive 소유)

## [Story 2-3] `ArchiveReason` enum 재정의 + DB CHECK 재작성 + 기존 데이터 마이그레이션

### User Story
- As a 운영자
- I want `ArchiveReason` enum과 DB의 기존 이력 데이터가 새 3개 값(MANUAL/SCHEDULE_EXHAUSTED/MODE_DOWNGRADED)으로 이관되기를
- so that 이력·현재 코드 정합

### 설명
`Card/domain/model/ArchiveReason.java` 재정의. Flyway 마이그레이션이 `card_status_history.reason` UPDATE (`MAX_VIEW`·`MAX_DURATION` → `SCHEDULE_EXHAUSTED`) + CHECK 제약 변경.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Card.domain.model.ArchiveReason` — 재정의

### 완료 기준 (AC)
- Given `ArchiveReason.MANUAL` / When `name()` / Then `"MANUAL"`
- Given `ArchiveReason` values / When `values().length` / Then `3`
- Given 기존 history row `reason='MAX_VIEW'` / When migrate / Then `reason='SCHEDULE_EXHAUSTED'`
- Given 기존 row `reason='MAX_DURATION'` / When migrate / Then `reason='SCHEDULE_EXHAUSTED'`
- *(예외)* migrate 후 `reason='MAX_VIEW'` INSERT 시도 / Then CHECK 위반

### Definition of Done
- [ ] 구현 (`ArchiveReason.java` 재정의)
- [ ] 단위 테스트 (`ArchiveReasonTest`, 3개 값 검증)
- [ ] Flyway 마이그레이션 (Story 2-5에 포함)

### 스토리 포인트
1d

### 의존성
- 선행: Story 2-1
- 후행: Story 2-4, 2-5

## [Story 2-4] `ReviewCommandService.incrementViewAndHandleMaxView()` 재작성

### User Story
- As a Review BC 개발자
- I want `ReviewCommandService.recordView(sessionId, cardId)`가 viewCount 기록만 하고 archive 결정 안 하기를
- so that lifecycle 결정은 batch만 소유

### 설명
`incrementViewAndHandleMaxView()` 삭제 → `recordView()`로 단순화. `card.recordView()` 호출 + `dailyBatch.markViewed(cardId, now)` 호출 (batch 동기화, product-review.md).

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Review.application.ReviewCommandService`

**주요 메서드**:
- `recordView(sessionId: Long, cardId: Long): void` — Cornell view 기록 + batch 마킹

### 완료 기준 (AC)
- Given ON_FIELD 카드 + viewCount=99 / When `recordView(sessionId, cardId)` 100회 반복 / Then 여전히 ON_FIELD (archive 없음)
- Given card.recordView() 결과 viewCount 증가 + lastViewedAt 갱신
- Given batch가 open 상태 / When recordView / Then `batch.markViewed(cardId, now)` 호출됨

### Definition of Done
- [ ] 구현 (`ReviewCommandService.recordView` 재작성, MAX_VIEW 로직 제거)
- [ ] 단위 테스트 (viewCount 반복 증가 시 archive 안 되는 것 명시 검증)
- [ ] 슬라이스 테스트 (`@WebMvcTest` — recordView API 정상 200)

### 스토리 포인트
1d

### 의존성
- 선행: Story 2-1, 2-2
- 후행: `product-review.md` Epic 2 (batch 마킹 소비)

## [Story 2-5] Flyway `V{N}__abolish_onfieldbudget.sql`

### User Story
- As a 운영자
- I want DB 스키마와 데이터가 `OnFieldBudget` 흔적 없이 정리되기를
- so that 이력·CHECK 정합

### 설명
- 기존 `card_status_history.reason` UPDATE (MAX_VIEW/MAX_DURATION → SCHEDULE_EXHAUSTED)
- `card_status_history.reason` CHECK 제약 재작성 (`IN ('MANUAL','SCHEDULE_EXHAUSTED','MODE_DOWNGRADED')`)
- Card 테이블에는 maxView/maxDuration 컬럼 없었으므로 (VO였음) 무변경

**핵심 파일**:
- `V{N}__abolish_onfieldbudget.sql`
- `R{N}__rollback_abolish_onfieldbudget.sql`

### 완료 기준 (AC)
- Given 기존 history 3건 (MAX_VIEW/MAX_DURATION/MANUAL) / When migrate / Then reason 전부 신 값
- Given migrate 후 `INSERT ... reason='MAX_VIEW'` / Then CHECK 위반
- Given 롤백 스크립트 / When 실행 / Then CHECK 원복 + 데이터 원복 불가 (Best-effort, 원본 값 소실 명시)

### Definition of Done
- [ ] 구현 (V·R 두 스크립트)
- [ ] Slice 테스트 (`@DataJpaTest`) — migrate 결과 검증
- [ ] 통합 테스트 (`@SpringBootTest`) — 롤백 스크립트 성공

### 스토리 포인트
1d

### 의존성
- 선행: Story 2-3
- 후행: Epic 3

## [Story 2-6] 테스트 코드 재작성 (MAX_VIEW/MAX_DURATION → SCHEDULE_EXHAUSTED)

### User Story
- As a Card·Review BC 개발자
- I want 기존 테스트가 신 lifecycle 원칙으로 재작성되기를
- so that 리팩토링 이후 안정성 유지

### 설명
Card 도메인·Review 도메인·Application 테스트에서 MAX_VIEW/MAX_DURATION 케이스를 SCHEDULE_EXHAUSTED (Epic 3에서 배포)와 관찰 지표 케이스로 재작성.

### 완료 기준 (AC)
- Given 기존 `Card_maxView도달시_archive` 테스트 / When 리팩토링 / Then 삭제
- Given `Card_recordView_반복호출_archive되지않음` 신규 케이스 추가
- Given `CardStatusHistory_reason_SCHEDULE_EXHAUSTED_저장` 통합 케이스 추가

### Definition of Done
- [ ] 관련 단위·슬라이스 테스트 재작성
- [ ] JaCoCo 커버리지 유지 (≥ 이전 수준)

### 스토리 포인트
1d

### 의존성
- 선행: Story 2-1 ~ 2-5

---

# [Epic 3] Card `createdMode` 필드 + M3 하이브리드 도메인 메서드

## 목표

Card에 `createdMode: LearningMode` 필드를 신설하고 M3 하이브리드 규칙(`min(card.createdMode.max, user.currentMode.max)`)을 도메인 메서드로 표현한다. Down cap = 즉시 반영, Up = 새 card만 확장 규칙이 성립하는 종점.

## 배경

Epic 1·2 완료로 lifecycle이 fixed interval queue만 남았고 mode enum도 재편됨. 이제 Card가 자기 원래 mode를 알아야 M3 하이브리드가 성립. 이 Epic이 `product-review.md`의 `DailyLearningBatch.generateFor()`가 소비할 도메인 메서드(`isDueOn`, `hasScheduleExhausted`) 원천.

## 포함 Story

- Story 3-1: Flyway `V{N}__add_card_created_mode.sql` (3단계: nullable → backfill → NOT NULL)
- Story 3-2: Card 도메인 `createdMode` 필드 + `effectiveMaxDays`·`effectiveIntervals`·`isDueOn`·`hasScheduleExhausted` 메서드
- Story 3-3: `Card.returnToField(userCurrentMode, today)` fresh 재시작
- Story 3-4: `Card.create(...)` 팩토리에 `createdMode` 파라미터 + `CardCommandService.createCard()` 조율
- Story 3-5: 도메인 단위 테스트 (M3 하이브리드 down/up 시나리오 명시)

## Epic 인수 시나리오

- Given user_currentMode=MODE_7D + card `createdMode=MODE_14D` + card `enteredFieldAt=today-8일`
- When `card.hasScheduleExhausted(MODE_7D, today)` / Then `true` (effectiveMax=7, 8일 지남)

- Given user_currentMode=MODE_14D + card `createdMode=MODE_7D` (up 시나리오)
- When `card.effectiveMaxDays(MODE_14D)` / Then `7` (createdMode에 cap됨, up 시 확장 안 함)

- Given ARCHIVE 카드 + user_currentMode=MODE_28D
- When `card.returnToField(MODE_28D, today)` / Then `status=ON_FIELD`, `createdMode=MODE_28D`, `enteredFieldAt=today`, `viewCount=0`

## Epic 완료 기준 (DoD)

- [ ] `card.created_mode` 컬럼 NOT NULL + CHECK
- [ ] 3단계 Flyway 마이그레이션 무사고 (backfill 데이터 정합)
- [ ] `Card` 도메인 신규 메서드 단위 테스트 (M3 down/up 시나리오 명시)
- [ ] `returnToField` fresh 재시작 통합 테스트

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **backfill 시 default mode**: `user_schedule_config.mapped_mode` 있으면 그 값 사용, 없으면 `MODE_14D` (중간값). 이유: down이든 up이든 사용자 즉시 데이터 유효.
- **effectiveMax 계산 시점**: 매 batch 생성 시 계산 (lazy) vs Card 필드에 캐시. 매 계산 채택 — user mode 변경 즉시 반영 필요, Card 필드에 캐시하면 stale 위험.

## [Story 3-1] Flyway `V{N}__add_card_created_mode.sql` (3단계)

### User Story
- As a 운영자
- I want Card 테이블에 `created_mode` 컬럼이 안전 backfill되기를
- so that 3명 사용자 카드 데이터 정합

### 설명
3단계 마이그레이션: (1) `ADD COLUMN created_mode VARCHAR(10) NULL`, (2) UPDATE로 `user_schedule_config.mapped_mode` 조인 backfill + 없는 사용자는 MODE_14D, (3) `MODIFY COLUMN NOT NULL + CHECK`. 롤백 스크립트 동반.

**핵심 파일**:
- `V{N}__add_card_created_mode.sql`
- `R{N}__rollback_add_card_created_mode.sql`

### 완료 기준 (AC)
- Given 카드 A + user_schedule_config.mapped_mode='MODE_14D' / When migrate / Then card.created_mode='MODE_14D'
- Given 카드 B + user_schedule_config 없음 / When migrate / Then card.created_mode='MODE_14D' (default)
- Given migrate 후 `INSERT ... created_mode=NULL` / Then NOT NULL 위반
- Given migrate 후 `INSERT ... created_mode='MODE_INVALID'` / Then CHECK 위반

### Definition of Done
- [ ] 구현 (V·R 두 스크립트, 3단계 분리)
- [ ] Slice 테스트 (`@DataJpaTest`) — backfill 결과 정합
- [ ] 롤백 검증 (통합 테스트)

### 스토리 포인트
1d

### 의존성
- 선행: Epic 1, Epic 2 완료
- 후행: Story 3-2

## [Story 3-2] Card 도메인 `createdMode` 필드 + M3 하이브리드 메서드

### User Story
- As a Card BC 개발자
- I want Card 도메인이 `createdMode` 필드와 M3 하이브리드 메서드를 노출하기를
- so that batch가 이 메서드로 큐 판정 가능

### 설명
`Card` Aggregate Root에 `createdMode: LearningMode` 필드 + `effectiveMaxDays`/`effectiveIntervals`/`isDueOn`/`hasScheduleExhausted` 메서드 추가.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Card.domain.model.Card`

**주요 메서드**:
- `Card.effectiveMaxDays(userCurrentMode: LearningMode): int` — `min(card.createdMode.max, userMode.max)`
- `Card.effectiveIntervals(userCurrentMode): List<Integer>` — createdMode intervals 중 effectiveMax 이하만
- `Card.isDueOn(today: LocalDate, userCurrentMode): boolean` — `daysSinceEntered ∈ effectiveIntervals`
- `Card.hasScheduleExhausted(userCurrentMode, today: LocalDate): boolean` — `daysSinceEntered > effectiveMaxDays`

### 완료 기준 (AC)
- Given card `createdMode=MODE_14D` + user `MODE_7D` / When `effectiveMaxDays(MODE_7D)` / Then `7`
- Given card `createdMode=MODE_7D` + user `MODE_14D` / When `effectiveMaxDays(MODE_14D)` / Then `7` (up 시 확장 안 함)
- Given card enteredFieldAt=today-3 + createdMode=MODE_14D + user=MODE_14D / When `isDueOn(today, MODE_14D)` / Then `true` (3일차)
- Given card enteredFieldAt=today-15 + createdMode=MODE_14D + user=MODE_7D / When `hasScheduleExhausted(MODE_7D, today)` / Then `true` (15 > 7)
- *(엣지)* Given card enteredFieldAt=today (당일 생성) / When `isDueOn(today, ...)` / Then `false` (0일차는 intervals에 없음, 1일차부터 시작)

### Definition of Done
- [ ] 구현 (`Card.java` 필드·메서드 추가)
- [ ] 단위 테스트 (`CardScheduleTest`, 해피/엣지/예외 각 케이스, M3 down/up 시나리오 명시)
- [ ] JPA 매핑 (`@Enumerated(EnumType.STRING)`)

### 스토리 포인트
2d

### 의존성
- 선행: Story 3-1
- 후행: Story 3-3, 3-4, 3-5, `product-review.md` Epic 1

## [Story 3-3] `Card.returnToField(userCurrentMode, today)` fresh 재시작

### User Story
- As a 사용자
- I want ARCHIVE된 카드를 다시 학습하기로 결정하면 현재 mode로 fresh 시작하기를
- so that 재학습 = 새 학습 계약이라는 도메인 의도 반영

### 설명
`Card.returnToField(userCurrentMode, today)` 재작성. `status=ON_FIELD`, `enteredFieldAt=today`, `createdMode=userCurrentMode`, `viewCount=0`, `lastViewedAt=null`. Idempotent (이미 ON_FIELD면 no-op).

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Card.domain.model.Card`

**주요 메서드**:
- `Card.returnToField(userCurrentMode: LearningMode, today: LocalDate): void`

### 완료 기준 (AC)
- Given ARCHIVE 카드 + userMode=MODE_28D / When `returnToField(MODE_28D, today)` / Then status=ON_FIELD, createdMode=MODE_28D, enteredFieldAt=today, viewCount=0, lastViewedAt=null
- Given ON_FIELD 카드 / When `returnToField(MODE_7D, today)` / Then no-op (idempotent, 필드 불변)
- *(엣지)* Given ARCHIVE 카드 + previous createdMode=MODE_7D + userMode=MODE_28D / When `returnToField` / Then createdMode=MODE_28D (fresh, 원래 mode 잃음)

### Definition of Done
- [ ] 구현 (`Card.returnToField` 재작성)
- [ ] 단위 테스트 (`CardReturnToFieldTest`, ARCHIVE→ON_FIELD·idempotent 케이스)
- [ ] `CardStatusHistoryAppender.append()` 호출 (transition 이력 기록)

### 스토리 포인트
1d

### 의존성
- 선행: Story 3-2
- 후행: 없음

## [Story 3-4] `Card.create(...)` 팩토리에 `createdMode` 파라미터 + `CardCommandService.createCard()` 조율

### User Story
- As a Card BC 개발자
- I want Card 생성 시 사용자 현재 mode를 스냅샷으로 저장하기를
- so that M3 하이브리드 규칙의 원천 데이터 확보

### 설명
`Card.create(...)` 정적 팩토리에 `createdMode: LearningMode` 파라미터 추가. `CardCommandService.createCard(userId, ...)`가 `UserScheduleQueryService.currentMode(userId)` 조회 후 주입.

**핵심 클래스/인터페이스**:
- `com.example.thirdtool.Card.domain.model.Card` (정적 팩토리)
- `com.example.thirdtool.Card.application.service.CardCommandService`

**주요 메서드**:
- `Card.create(..., createdMode: LearningMode, today: LocalDate): Card`
- `CardCommandService.createCard(userId, axisId, ...): Card`

### 완료 기준 (AC)
- Given userMode=MODE_28D / When `createCard(userId, ...)` / Then card.createdMode=MODE_28D
- Given userMode=MODE_7D / When `createCard(userId, ...)` / Then card.createdMode=MODE_7D
- *(예외)* Given user_schedule_config 없는 사용자 / When `createCard` / Then default MODE_14D + 로그

### Definition of Done
- [ ] 구현 (`Card.create` 시그니처 확장, `CardCommandService` 조율)
- [ ] 단위 테스트 (`CardCreateTest`, mode 스냅샷 검증)
- [ ] 슬라이스 테스트 (`@WebMvcTest` — POST /cards 응답 DTO에 createdMode 포함)

### 스토리 포인트
2d

### 의존성
- 선행: Story 3-2, Epic 1 Story 1-4 (currentMode(userId))
- 후행: Story 3-5

## [Story 3-5] 도메인 단위 테스트 (M3 하이브리드 down/up 시나리오 명시)

### User Story
- As a Card BC 개발자
- I want M3 하이브리드 규칙이 down/up 시나리오별로 명시적 테스트로 검증되기를
- so that 리팩토링 이후 규칙 회귀 방지

### 설명
`CardHybridScenarioTest` 신설. Down cap 시나리오 (사용자가 mode down 후 이미 넘긴 카드 exhausted 판정), up 시 유지 시나리오 (이미 창설된 카드의 스케줄이 확장 안 됨), returnToField fresh 시나리오 커버.

### 완료 기준 (AC)
- Given `downCap_이미넘긴카드_hasScheduleExhausted_true`
- Given `up_기존카드_effectiveMaxDays_변화없음`
- Given `down_아직안넘긴카드_새max로cap_isDueOn_반영`
- Given `returnToField_모드바뀐후_새createdMode적용`

### Definition of Done
- [ ] 구현 (`CardHybridScenarioTest.java`)
- [ ] 명명 규칙 준수 (`{대상행위}_{상황}_{기대결과}`)
- [ ] 커버리지 시나리오: down cap (즉시/지연), up 유지, returnToField fresh

### 스토리 포인트
1d

### 의존성
- 선행: Story 3-2, 3-3, 3-4
- 후행: 없음
