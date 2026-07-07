# [Product] Card 상태와 스케줄링

## 성과 (Outcome)

> **Card를 "학습 성공/실패"가 아닌 "지금 어디에 있어야 하는가"의 운영 위치(ON_field / Archive)로 재정의해, WIP 예산 안에서 순환하는 학습 시스템을 만든다.**
> 카드 퇴장은 실패가 아니라 운영 규칙이라는 점이 도메인·UI·운영 메시지 전반에 일관되게 드러난다.

## 설계 결정 (Design Decisions)

> Card BC 전반의 큰 결정. 세부 결정은 각 Epic의 "Epic 기술 결정 / 대안" 섹션에서 다룬다.

- **상태 모델 — ON_FIELD / ARCHIVE 2값 단일 전이축**
    - SRS의 "정답률·난이도" 평가 모델 대신 "현재 어느 운영 위치인가"라는 **2값 단일 전이축**으로 단순화한다
    - `ARCHIVE` 사유(`ArchiveReason`)는 `MANUAL` / `MAX_VIEW` / `MAX_DURATION` 3종 — 사유는 이력에만 남기고 상태 자체는 분기하지 않는다 (`ARCHIVE` 카드의 후속 행위가 동일)
    - 멱등성 보장 — 같은 상태로의 `archive()` / `returnToField()`는 no-op (이력도 생성하지 않음)
- **노출 예산 — OnFieldBudget (maxView + maxDuration) 동시 두기**
    - 횟수와 기간 중 하나가 먼저 도달하면 `AUTO_ARCHIVE` — 동시 도달 시 `MAX_VIEW` 우선 (도메인 ADR로 기록 예정)
    - 둘 다 시스템이 아닌 **유저별 `UserScheduleConfig`에서 파생** — Card BC는 budget을 직접 보유하지 않고 매 호출마다 주입받음
- **Soft Schedule — 약한 interval, 부채 미발생**
    - SRS의 "오늘 봐야 할 카드(due)" 모델 대신 "마지막 노출 이후 N일 지났으면 노출 후보가 됨" 모델
    - 간격을 지나도 안 봐도 누적 부채(밀린 카드) 발생하지 않음 — `state` 자체가 통과 여부만 표현
- **Tag — 시스템 전역 유니크 + find-or-create**
    - `tag.value`는 시스템 전역 UNIQUE. 같은 텍스트는 어느 유저가 만들었든 단일 row 재사용
    - 사용자 입력 시 사전 생성 없이 즉시 부착 — Application Service가 find-or-create로 흡수
    - 최대 3개/카드 (밀도 의도)
- **ReviewSession — Layer 1(LearningFacade) 단위 수집 + state 비율 추천**
    - Deck 단위로 수집하면 한 Deck에 집중되는 starvation 발생 — Layer 1 전체에서 수집해 Deck 분포 균형 확보
    - v1은 **100% rule-based** — Spring AI 호출은 매일 발생하는 ReviewSession에 비용·결정성 손실이 누적되어 부적합. v2 도입 시에도 rule-based가 항상 폴백
- **Card 자산성 — Soft Delete (ADR003)**
    - Card / Deck 등 사용자가 직접 만든 자산은 `deletedAt` Soft Delete. CardStatusHistory · CardTag · KeywordCue 등 종속 엔티티는 Hard Delete (orphanRemoval)
- **외부 주입 금지 필드 — 도메인 행위 메서드를 통해서만 변경**
    - `displayOrder` / `enteredFieldAt` / `viewCount` / `lastViewedAt` / `status` — setter 미노출. 외부에서 직접 값 주입 금지. `recordView()` / `archive()` / `returnToField()` 같은 행위 호출로만 변경

## 대안 검토 (Alternatives Considered)

> 큰 갈림길마다 "왜 이것이 아니고 저것인가"를 남긴다. 거부된 안에도 합리적 근거가 있었음을 보임으로써 현재 선택의 트레이드오프를 명확히 한다.

### 상태 모델 — 단계 수

**Option A — SRS 정답률·간격 다단계 (Anki/SuperMemo 패턴)**
- 장점: 학계 검증, 사용자 인지도 높음, 망각곡선과 정합
- 거부 이유:
    - "정답률"이라는 개념이 본 도메인의 자기 회상(자기 채점)과 충돌 — 객관적 채점 기준 불가
    - 단계가 많을수록 도메인 코드의 상태 전이 매트릭스가 폭발 (현재 2x2 = 4 전이만 다루면 됨)
    - 사용자가 "성공/실패" 평가를 받는다는 압박감이 학습 의욕 저해 (서비스의 운영 위치 철학과 정반대)

**Option B — ON_FIELD / ARCHIVE / SUSPENDED 3단계**
- 장점: "잠시 멈춤" 상태로 사용자가 직접 다중 컨텍스트 관리 가능
- 거부 이유:
    - SUSPENDED와 ARCHIVE의 의미 경계가 불명확 — 사용자가 둘 중 어디로 보낼지 매번 결정해야 함 (UX 부담)
    - SUSPENDED를 ARCHIVE로 자동 전이시키는 규칙이 또 필요해짐 (Epic 2와 동일한 정책을 2번 만들어야 함)

**Option C (선택) — ON_FIELD / ARCHIVE 2값 + ArchiveReason 이력**
- 비용: "잠시 멈춤"이라는 중간 상태 표현이 없음 — 사용자는 "보관" 또는 "안 보관" 둘 중 하나만 선택
- 보상: 상태 전이 매트릭스 단순 (4건 = ON→ARC, ARC→ON, ON→ON, ARC→ARC 중 후자 2건은 멱등 no-op). 사유는 `CardStatusHistory.reason`으로 보존되어 회고·분석에서 손실 없음
- 트레이드오프 수용 근거: 본 서비스의 "운영 위치" 철학과 가장 정합. SUSPENDED가 필요한 사용자 시나리오가 등장하면 v2에서 ArchiveReason에 새 값 추가만으로 흡수 가능

### displayOrder — 시작 인덱스

**Option A — 0-based (개발자 친화)**
- 장점: 자바 List/배열과 정합, 코드 변환 부담 0
- 거부 이유:
    - 도메인 의미가 "첫 번째, 두 번째"인데 코드에서는 `displayOrder == 0`이 "첫 번째" — 도메인 표현/구현 불일치
    - 응답 DTO가 그대로 노출되면 FE에서 +1 변환 필요 — 변환 누락 시 사용자가 "0번째 카드"를 봄

**Option B (선택) — 1-based + DB CHECK >= 0 안전망**
- 비용: 자바 `List.get(displayOrder - 1)` 같은 변환 코드가 Repository 경계에서 1회 필요
- 보상: 도메인·DTO·FE가 모두 "1, 2, 3" 의미 그대로 사용. DB CHECK `>= 0`은 "도메인이 0을 부여하지 않음"을 강제하는 마지막 안전망 (음수·갭 차단)

### maxView / maxDuration — 어디서 결정하는가

**Option A — 시스템 전역 단일 상수 (`OnFieldBudget` 도메인 상수)**
- 장점: 구현 단순, DB 컬럼 불필요, 모든 유저 동일 → 운영 메시지 일관
- 거부 이유:
    - 사용자별 학습 에너지(주 10시간 vs 주 1시간)가 달라 동일 budget이 부적합
    - "내가 14일을 원하는데 시스템이 강제 7일"이라는 불만이 즉시 발생 — 개인화 요구 시점에 v2 마이그레이션이 큰 작업

**Option B — Card 컬럼으로 카드별 오버라이드 (`card.max_view`, `card.max_duration`)**
- 장점: 카드별 세밀한 제어
- 거부 이유:
    - 카드마다 budget을 설정하는 UX 부담이 비현실적 (수백 장 카드 × 2개 필드)
    - 일관된 운영 정책이 무너짐 — "왜 이 카드만 30일이고 저 카드는 7일?" 설명 비용 발생
    - v3 자동 추천 도입 시에도 카드별 컬럼은 동기화 부담만 키움

**Option C (선택) — UserScheduleConfig가 모드(10D/20D/30D)로 매핑 → Application Service가 `OnFieldBudget`을 매 호출 주입**
- 비용: Card 도메인이 budget을 직접 보유하지 않으므로 호출자가 매번 주입해야 함 (메서드 시그니처가 늘어남)
- 보상: 유저당 1개의 모드 매핑만 관리 (단순). 모드 추가는 enum 1줄. v2 자동 추천은 `LearningModeMappingPolicy` 1곳만 교체

### Tag — 시스템 전역 유니크 vs 유저 스코프

**Option A — 유저별 Tag 스코프 (`UNIQUE(user_id, value)`)**
- 장점: 사용자 간 Tag 격리 — "내 Tag"가 명확. 같은 단어를 다른 의미로 쓰는 사용자 충돌 없음
- 거부 이유:
    - 시스템 전체로 Tag 통계·트렌드 분석 불가
    - v2 LLM 자동 Tag 부여 시 학습 데이터가 유저별로 파편화
    - 사용자 입장에서 "다른 사람이 만든 '백엔드' Tag와 내 '백엔드' Tag가 별개"인 것이 직관과 어긋남 (Tag = 보편적 라벨)

**Option B (선택) — 시스템 전역 유니크 (`UNIQUE(tag.value)`) + find-or-create**
- 비용: 유저 A가 만든 의미의 "스프링"과 유저 B가 만든 의미의 "스프링(계절)"이 같은 Tag로 묶임 — 사용자 인지 부담
- 보상: 인덱스/조인 단순. v2 LLM 학습 데이터 집약. 시스템 전체 Tag 트렌드 분석 즉시 가능

**Option C — 모든 Tag를 사전에 어드민이 발행, 사용자는 선택만**
- 거부 이유: 사용자 자유도 0. 본 서비스는 사용자 주도 학습 도구이므로 어드민 큐레이션 모델이 부적합

### ReviewSession 수집 단위

**Option A — Deck 단위 수집 (사용자가 Deck 하나 선택해 학습)**
- 장점: 사용자 의도 명확, 학습 흐름 직관적
- 거부 이유:
    - 한 Deck에만 카드가 집중되면 다른 Deck의 카드가 영원히 ReviewSession에 등장 못 함 (starvation)
    - Layer 1 전체 균형이 깨짐 — 사용자가 의식하지 못한 채 한 영역만 학습

**Option B (선택) — Layer 1(LearningFacade) 단위 수집 + state 비율 배분**
- 비용: 사용자가 "오늘은 이 Deck만"이라는 의도 표현 못 함 (Deck 단위 학습은 별도 메뉴로 제공해 보완)
- 보상: 모든 Deck 카드가 공평하게 등장. state(1일/3일/7일) 비율로 학습 우선순위 자연 반영

### Soft Schedule — due vs interval

**Option A — 강한 due (반드시 그날 봐야 함, SRS 패턴)**
- 거부 이유: 못 본 날의 카드가 누적되어 부채(밀린 카드)가 되고, 부채가 쌓이면 사용자가 포기 — 본 서비스 철학(부담 없는 순환)과 정면 충돌

**Option B (선택) — 약한 interval (지나면 후보가 됨, 안 봐도 부채 없음)**
- 비용: 카드 회수율 측정이 어려움 (사용자가 안 보면 그냥 후보로 머무름)
- 보상: 사용자 부담 0. 학습 에너지가 있는 날 자연스럽게 처리. ReviewSession state 비율로 우선순위는 별도 표현

## 전체 아키텍처 (High-Level Architecture)

> 컴포넌트 다이어그램과 핵심 플로우. Card BC는 6개 BC 중 ReviewSession·UserSchedule·LearningFacade와 협력 표면을 가진다.

### 컴포넌트 배치

```
[FE — React SPA]
    │
    │  POST /api/v1/cards        | GET /api/v1/review-session/today
    │  POST /api/v1/cards/{id}/keywords   | GET /api/v1/cards/archive?tags=...
    ▼
┌──────────────────────────────────────────────────────────────────┐
│  Card/presentation/                                               │
│  ├─ CardController              (CRUD, 카드 부속물 조작)         │
│  ├─ TagController               (find-or-create, Tag 관리)       │
│  └─ Request/Response DTO        (Bean Validation)                │
└──────────────────────────────────────────────────────────────────┘
    │
    │  Command/Query record (ADR005)
    ▼
┌──────────────────────────────────────────────────────────────────┐
│  Card/application/                                                │
│  ├─ CardCommandService          (생성·수정·archive·returnToField)│
│  ├─ CardQueryService            (조회·필터링·페이징)             │
│  └─ Coordinator                                                   │
│      ├─ UserScheduleConfig 조회 ──► OnFieldBudget 주입           │
│      ├─ CardStatusHistoryAppender   (상태 이력 누적)             │
│      └─ deck.markInProgress()       (Deck Aggregate 행위 호출)   │
└──────────────────────────────────────────────────────────────────┘
    │
    │  도메인 행위 호출
    ▼
┌──────────────────────────────────────────────────────────────────┐
│  Card/domain/model/                                               │
│  ├─ Card (AR)                                                    │
│  │   ├─ MainNote (VO) · KeywordCue (E, ≥1) · Summary (VO, 1~3문장)│
│  │   ├─ CardStatus(ON_FIELD/ARCHIVE) · ArchiveReason             │
│  │   └─ enteredFieldAt · viewCount · lastViewedAt                │
│  ├─ Tag (E, 시스템 전역 UNIQUE) · CardTag (E, ≤3/card)           │
│  ├─ CardStatusHistory (E) · CardStatusHistoryAppender (Service)  │
│  ├─ OnFieldBudget (VO) · CardExpiryPolicy (Service)              │
│  └─ CardRelationFinder (Service, Tag 기반 연결 후보)             │
└──────────────────────────────────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────────────────────────────────┐
│  Card/infrastructure/persistence/                                 │
│  ├─ CardRepository (Port) ◄ CardRepositoryAdapter (구현)         │
│  ├─ CardJpaRepository (Spring Data) + Custom (QueryDSL)          │
│  └─ CardStatusHistoryRepository                                   │
└──────────────────────────────────────────────────────────────────┘
                          │
                          ▼
       ┌──────────────────────────────────────────────┐
       │  MySQL                                        │
       │  ├─ card (status, entered_field_at, ...)     │
       │  ├─ card_status_history (reason, changed_at) │
       │  ├─ keyword_cue · tag (UNIQUE value)         │
       │  ├─ card_tag (UNIQUE(card_id, tag_id))       │
       │  └─ user_schedule_config (1:1 user)          │
       └──────────────────────────────────────────────┘


[BC 협력 표면 — 호출 방향]

  Review BC ──► Card.recordView() (ReviewSession 내부 전용)
  Card BC   ──► Deck.markInProgress() / recalculateProgressStatus() (Aggregate 행위)
  Card BC   ◄── UserSchedule.UserScheduleConfig (OnFieldBudget 파생)
  Card BC   ◄── LearningFacade.Layer1Id (ReviewSession 수집 범위 결정)
  aisuggestion BC ──► Card.CardStalenessQuery (read-only, v1.5)
```

### 핵심 플로우

**1. Card 생성 → ON_FIELD 진입**
```
Client ─POST /api/v1/cards─► CardController
                                │
                                ▼
                            CardCommandService.create(CreateCard)
                                │
                                ├─► userScheduleConfigRepo.findByUser(userId)
                                │      → OnFieldBudget (maxView/maxDuration 매핑)
                                │
                                ├─► Card.create(...)
                                │      ├─ status=ON_FIELD, enteredFieldAt=now()
                                │      ├─ viewCount=0, keywords ≥1 검증
                                │      └─ summary 1~3문장 검증
                                │
                                ├─► tagRepo.findOrCreateAll(tagValues)  (find-or-create)
                                ├─► cardRepository.save(card)
                                └─► deck.markInProgress()  (NOT_STARTED → IN_PROGRESS)

Client ◄─ 201 + CardResponse { id, status, enteredFieldAt, keywords, tags } ─
```

**2. ReviewSession 중 카드 노출 → maxView 도달 시 자동 archive**
```
Review BC ─reviewSession.recordCurrentCardView()─► CardReview.recordView()
                                                       │
                                                       ▼
                                                  Card.recordView()
                                                       ├─ viewCount += 1
                                                       ├─ lastViewedAt = now()
                                                       └─ isLastView() == true?
                                                              │
                                                  ┌───────────┴───────────┐
                                                  ▼ true                  ▼ false
                                          Card.archive(MAX_VIEW)      그대로 진행
                                          + CardStatusHistory append
                                          + deck.recalculateProgressStatus()
```

**3. 야간 배치 — maxDuration 만료 카드 자동 archive**
```
[매일 03:00 BatchScheduler] ─► CardExpiryBatchService.processExpired()
                                    │
                                    ├─► cardRepo.findOnFieldExpired(now - maxDuration)
                                    │
                                    └─► for each card:
                                            ├─ OnFieldBudget.resolveReason(card)
                                            │     → Optional<ArchiveReason>
                                            ├─ if present:
                                            │     ├─ Card.archive(reason)
                                            │     ├─ CardStatusHistoryAppender.append(...)
                                            │     └─ deck.recalculateProgressStatus()
                                            └─ trace 로그 (count, reason 분포)
```

**4. ReviewSession 일일 수집 (Layer 1 단위, state 비율)**
```
Client ─GET /api/v1/review-session/today─► ReviewController
                                              │
                                              ▼
                                          ReviewQueryService.getToday(userId)
                                              │
                                              ├─► layer1Id = learningFacadeRepo.findByUser(userId).id
                                              │
                                              ├─► cardRepo.findEligibleByLayer1(
                                              │       layer1Id,
                                              │       now - softScheduleInterval
                                              │   )   ← ON_FIELD + soft schedule 통과만
                                              │
                                              ├─► state별 분류 (1일/3일/7일/...)
                                              │
                                              └─► dailyTarget × state 비율 배분
                                                  → 추천 카드 리스트
```

### Out-of-Process 의존

- **MySQL** — `card`, `card_status_history`, `tag`, `card_tag`, `keyword_cue`, `user_schedule_config`. JPA + QueryDSL 조합. `card_tag (card_id, tag_id)` UNIQUE 인덱스로 중복 부착 차단
- **JVM Scheduler** — `CardExpiryBatchService` 매일 03:00 만료 카드 일괄 처리 (`@Scheduled` cron)
- **(향후) aisuggestion BC** — v1.5에서 `CardStalenessQuery`를 read-only로 호출 (P95 ≤ 100ms). Card BC는 Port 정의·LLM 통합에 관여하지 않음

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### Card 행위 실패 시나리오와 응답

| 시나리오 | ErrorCode | HTTP | 클라이언트 권장 동작 |
| --- | --- | --- | --- |
| 존재하지 않는 카드 조회·수정·삭제 | `CARD_NOT_FOUND` | 404 | 목록 화면으로 복귀 + 토스트 |
| Keyword 미입력 (0개) 생성 시도 | `CARD_KEYWORD_MIN_REQUIRED` | 400 | 입력 폼에 인라인 에러 표시 |
| 마지막 Keyword 제거 시도 | `CARD_KEYWORD_LAST_CANNOT_REMOVE` | 400 | 삭제 버튼 비활성 + 사유 툴팁 |
| Keyword blank 입력 | `CARD_KEYWORD_BLANK` | 400 | 입력 폼 인라인 에러 |
| Summary 문장 수 1~3 범위 초과 | `CARD_SUMMARY_SENTENCE_OUT_OF_RANGE` | 400 | 입력 폼 인라인 에러 + 현재 문장 수 표시 |
| Summary 빈 입력 | `CARD_SUMMARY_EMPTY` | 400 | 입력 폼 인라인 에러 |
| MainNote 텍스트·이미지 모두 빈 입력 | `CARD_MAIN_NOTE_EMPTY` | 400 | 입력 폼 인라인 에러 |
| Tag 4개 이상 부착 시도 | `CARD_TAG_LIMIT_EXCEEDED` | 400 | "최대 3개" 안내 + 기존 Tag 제거 유도 |
| 이미 부착된 Tag 재부착 | `CARD_TAG_ALREADY_EXISTS` | 409 | 무시 (find-or-create는 정상이므로 발생률 낮음) |
| Tag value blank | `TAG_VALUE_BLANK` | 400 | 입력 폼 인라인 에러 |
| 다른 유저의 카드 접근 | `AUTH_FORBIDDEN` (Common) | 403 | 목록으로 복귀 + 권한 안내 |
| 만료 배치 실패 (JVM 예외) | (배치 로그) | - | 다음 day 배치에서 자동 재시도 (idempotent — 같은 카드 두 번 archive해도 멱등) |

### 로깅 정책

- **항상 기록**: 카드 상태 전환 (`archive` / `returnToField`) — `cardId + fromStatus + toStatus + reason + actor(userId|SYSTEM)`
- **info 레벨**: 만료 배치 결과 — 처리 건수 + reason 분포 (MAX_VIEW vs MAX_DURATION)
- **debug 레벨**: `recordView()` 호출 (대량 발생 — 운영 시 info로 두면 로그 폭주)
- **절대 금지**: Card 본문(`mainNote`) 전체 페이로드, Summary 본문 — 사용자 학습 콘텐츠 (PII 준)

### 관측 지표 (v2 — Metrics 도입 시 합류)

- `card_state_transition_total{from, to, reason}` — 카운터 (상태 전이 분포)
- `card_auto_archive_total{reason}` — 카운터 (MAX_VIEW vs MAX_DURATION 비율)
- `card_expiry_batch_duration_seconds` — 히스토그램 (배치 비용 추적)
- `card_expiry_batch_failures_total` — 카운터 (재시도 추적)
- `review_session_starvation_ratio` — 게이지 (한 Deck 비중 ≥80% 세션 비율)
- `tag_find_or_create_collision_total` — 카운터 (find 비율 vs create 비율 — Tag 재사용 지표)

### 자가 진단 (성공 지표와 매핑)

- "ON_field 예산 위반 카드 잔존 = 0건" → 야간 배치 + `cardRepo.findOnFieldExpired()` 통합 테스트로 검증
- "자동 만료 누락률 ≤ 0.1%" → `card_auto_archive_total` / 만료 후보 모집단 비율로 산출
- "Layer 1 starvation ≤ 5% 세션" → `review_session_starvation_ratio`로 알람
- "퇴장 메시지 '실패' 어휘 = 0건" → CI에 UI 텍스트 grep 단계 추가 (Epic 8 Story 8-1)

## 롤아웃 / 마이그레이션 (Rollout)

### 전제 — 프로덕션 트래픽 없음

현재 사용자 0명. 따라서 **Epic 간 병용 기간 없이 순차 출시**한다. 만약 사용자가 있었다면:
- 신규 컬럼(`entered_field_at`, `view_count`, `last_viewed_at`) 추가 후 백필 1주
- maxView/maxDuration 도입 전 1주는 자동 archive 비활성화 (운영 모니터링)
- ReviewSession Layer 1 수집은 기존 Deck 수집과 1주 병용 후 전환

이 단계 전환을 생략한 것은 **수용 가능한 단순화 선택**이며, 향후 트래픽 발생 후 동일 변경이 필요할 경우 위 패턴을 재사용한다.

### Epic 의존성 그래프

```
Epic 1 (Card 상태 도메인 재정의)
  Story 1-1 (status 컬럼) ──┬─► Story 1-2 (UX 분리)
                            │
                            ├─► Epic 2 (ON_field 예산)
                            │     Story 2-1 (enteredFieldAt/viewCount/lastViewedAt)
                            │       ├─► Story 2-2 (maxDuration 자동 archive)
                            │       └─► Story 2-3 (maxView 자동 archive)
                            │             │
                            │             ▼
                            │       Epic 3 (Soft Schedule, lastViewedAt 재사용)
                            │         Story 3-1 (interval 필터) ─► Story 3-2 (state)
                            │             │
                            │             ▼
                            │       Epic 4 (UserScheduleConfig)
                            │         Story 4-1 (mapping) ─► 4-2 (저장) ─► 4-3 (수정)
                            │             │
                            │             ▼
                            │       Epic 6 (ReviewSession)
                            │         Story 6-1 (Layer 1 수집) ─► 6-2 (비율 추천) ─► 6-3 (+N장)
                            │
                            └─► Epic 5 (Tag)
                                  Story 5-1 (find-or-create) ─► 5-2 (탐색) ─► 5-3 (Archive 연결)
                                                                                    │
                                                                                    ▼
                                                                              Epic 7 (Archive → ON_field 복귀)
                                                                                Story 7-1

  Epic 8 (UI 문구) — 전 Epic 횡단, Epic 2 자동 archive 안내 등록 직후부터 진입 가능
```

### 환경별 설정 분기

- `application-local.yml`: 배치 스케줄 `@Scheduled` 비활성 (테스트 격리)
- `application-prod.yml`: `card.expiry.cron=0 0 3 * * *` (매일 03:00)
- `card.batch.chunk-size=500` — 한 배치 트랜잭션당 처리 건수. 운영 부하 따라 조정

### Flyway 마이그레이션 단계

본 Product 범위 신설/변경 테이블:
- `V?__card_status_and_budget.sql` — `card.status`, `entered_field_at`, `view_count`, `last_viewed_at` 컬럼 추가 + CHECK + 인덱스 (Epic 1·2 합산 단일 마이그레이션)
- `V?__user_schedule_config.sql` — `user_schedule_config` 테이블 신설 (Epic 4)
- `V?__tag_and_card_tag.sql` — `tag` (UNIQUE value), `card_tag` (UNIQUE(card_id, tag_id)) 테이블 신설 (Epic 5)
- `V?__card_status_history.sql` — `card_status_history` 테이블 신설 (Epic 2)

각 마이그레이션은 단일 PR + 단일 Story 단위로 분리해 롤백 가능하게 격리. 기존 데이터 백필 필요 시 별도 `R?__rollback_*.sql` 동반.

### 롤백 계획

- Card 도메인 행위는 멱등 — 코드 revert만으로 복구. DB 컬럼은 즉시 DROP하지 않고 1주 보존
- 자동 archive 배치는 `card.expiry.enabled=false` flag로 즉시 중단 가능 (수동 archive는 영향 없음)
- ReviewSession Layer 1 수집 실패 시 Deck 단위 fallback 쿼리로 우회 (성능 저하만 발생, 기능 유지)

## 성공 지표 (측정 가능)

| 지표 | 목표 값 | 측정 방법 |
| --- | --- | --- |
| ON_field에 maxDuration·maxView 예산 위반 카드 잔존 | = 0건 | 야간 배치 + 통합 테스트 |
| 카드 자동 퇴장(만료) 처리 누락률 | ≤ 0.1% | `CardStatusHistory(reason=MAX_VIEW/MAX_DURATION)` 카운트 / 만료 후보 |
| ReviewSession 추천 카드의 soft schedule 위반 | = 0건 | 추천 쿼리 정적 분석 + 통합 테스트 |
| Layer 1 기준 추천 시 Deck starvation (한 Deck 비중 ≥ 80%) | ≤ 5% 세션 | 세션별 Deck 분포 집계 |
| 퇴장 메시지에 "실패" 어휘 사용 | = 0건 | UI 문구 grep |
| Archive → ON_field 복귀 시 이전 이력 보존 | = 100% | `CardStatusHistory` 회귀 테스트 |
| 사용자별 설정값(maxDuration/dailyTarget) 독립 저장 | = 100% | `user_schedule_config` 테이블 통합 테스트 |

## Scope

- **In Scope**
  - Card `status` (ON_field / Archive) 도메인 모델 + DB 반영
  - `enteredFieldAt`, `viewCount`, `lastViewedAt` 기록 및 갱신
  - maxDuration / maxView 기반 자동 만료 (`CardExpiryPolicy`, `OnFieldBudget`)
  - Soft Schedule state 계산 및 ReviewSession 노출 후보 필터링
  - 사용자별 `maxDuration` 입력 → 내부 모드(10/20/30일) 매핑
  - 사용자별 `dailyTarget` + state 비율 기반 추천
  - Tag 도메인 (시스템 전역 유니크, find-or-create), Tag 기반 탐색 및 Archive 연결 후보
  - Archive → ON_field 복귀 (새 사이클 시작, 이력 보존)
  - 퇴장·복귀 안내 문구 (UI 텍스트 정책)
  - 운영 규칙 요약 인앱 문서

- **Out of Scope** (※ AI 기반 항목은 `product-aisuggestion.md`의 Spring AI 인프라 — `ChatClient`/`PromptTemplate`/`BeanOutputConverter`/`SuggestionPort` 패턴 — 을 그대로 재사용한다. Card BC 도메인은 Spring AI에 직접 의존하지 않고 Card 측 Port를 정의해 `infrastructure/.../gemini/` Adapter로 흡수)
  - 개인별 maxDuration 자동 추천 (학습 이력 기반) — v2. AI/ML 도입 시 Spring AI `ChatClient` 또는 별도 ML 추론 Port로 처리
  - Archive 카드 자동 만료·정리 정책 — v2 (rule-based)
  - **자동 Tag 부여 (LLM 기반)** — v2. Spring AI `ChatClient` + Card 측 `TagSuggestionPort` 신설 예정. 현재는 유저 직접 부착
  - **Tag 간 계층·의존 관계 (의미 기반 그룹핑)** — v2. Spring AI 임베딩 또는 LLM 추론 Port 재사용
  - state 비율 커스터마이징 — v2 (유저 설정 기반, AI 무관)
  - 카드 단위 maxDuration 개별 오버라이드 — v2
  - 복귀 횟수 제한·쿨다운 정책 — v2
  - **학습 속도 기반 dailyTarget 자동 추천** — v2. 학습 이력 통계 + Spring AI 보조 가능
  - **ReviewSession 카드 우선순위 알고리즘 고도화** — v2. 현재 rule-based(state 비율). AI 기반 도입 시 Spring AI `ChatClient` 재사용
  - **외부 기술 트렌드 기반 카드 추천** — v3. Spring AI + 외부 데이터 소스 결합
  - **갭 인지형 개인화 컨텍스트 자체** (`PersonalizationContextPort` 정의 / LLM 통합 / 메트릭 태깅) — `product-aisuggestion.md` Epic 6 (v1.5) 범위. Card BC는 stale 축 판정의 **데이터 원천**(`lastViewedAt` 기반 cardIds → 최신 노출 시각 집계)만 책임지고, Port 정의·LLM 프롬프트 통합·갭 보완 검증에는 관여하지 않음. 노출 수단은 Card BC가 제공하는 inbound 쿼리(`CardStalenessQuery` 또는 동등)로, aisuggestion BC의 outbound Adapter가 호출

## Epic 목록

- [ ] Epic 1. Card 상태(ON_field / Archive) 도메인 재정의
- [ ] Epic 2. ON_field 무한 반복 방지 — maxDuration & maxView
- [ ] Epic 3. 짧은 기간 과도 반복 방지 — Soft Schedule
- [ ] Epic 4. 사용자별 학습 에너지 개인 설정 (maxDuration 입력 & 모드 매핑)
- [ ] Epic 5. Tag 클릭 탐색 및 Tag 목록 관리
- [ ] Epic 6. ReviewSession — 일일 학습 카드 수집 및 비율 추천
- [ ] Epic 7. Archive → ON_field 복귀 규칙 설계
- [ ] Epic 8. WIP 모델 기반 사용자 설명 체계

## 제품 수준 완료 기준 (DoD)

- [ ] 8개 Epic이 모두 Done 상태다
- [ ] Card 상태 전환 멱등성 (`archive()`/`returnToField()`/`recordView()`)이 단위 테스트로 보장된다
- [ ] OSIV=false, READ_COMMITTED 등 프로젝트 원칙 준수
- [ ] OpenAPI 스펙(`/swagger-ui.html`)에 Card / Tag / ReviewSession 엔드포인트 반영
- [ ] Flyway 마이그레이션: `card.status`, `card.entered_field_at`, `card.view_count`, `card.last_viewed_at`, `user_schedule_config`, `card_tag`, `tag` 테이블 정합
- [ ] ADR 작성
  - ADR: "Card 상태 모델 — ON_field/Archive 운영 위치 vs SRS 정답률 비교"
  - ADR: "maxDuration 입력 → 내부 모드 매핑 정책 (10/20/30일)"
  - ADR: "ReviewSession Layer 1 기준 수집 — Deck 단위 starvation 회피"
  - ADR: "Tag — 시스템 전역 유니크 + find-or-create 패턴"
- [ ] UI 텍스트 정책 문서(`docs/ux/wip-language.md`)에 퇴장·복귀 문구 가이드 등록

## 대상 사용자

- 주요 사용자: 학습 중인 Third Tool 유저 (Layer 1 컨셉 설정 완료)
- 사용 맥락: 매일 ReviewSession 진입 / Card 학습 / Tag 기반 탐색 / Archive 참조
- 운영 측면: Card 자동 만료·이력 보존이 자동 동작해야 유저 신뢰 유지

## 명세 변경 이력 (Spec Drift Log)

> done 상태 Product의 결정이 후속 fix로 부분 폐기될 때 in-place 수정 대신 본 블록에 누적 기록한다.

- **fix-deck-axis-visibility (0.0.2v) — 2026-06-30** (`workflow/task/fix/sdd/version/0.0.2v/fix-deck-axis-visibility.md`, [ADR020](../../../../../../../../docs/adr/ADR020-deck-axis-visibility.md))
  - **폐기 §3.3 — "Card 조회는 Deck / Tag 단위만"**: 사용자가 "축을 누르면 그 축 카드가 보인다"는 표현 흐름을 표현할 경로가 없어 폐기. `GET /api/v1/learning-facade/axes/{axisId}/cards` 신설 (축 직속 카드 평면 목록, Topic 그룹핑은 v0.0.3v 분리).
  - **폐기 §3.4 — today 집계와 축 카드 조회의 쿼리 분기(암묵 기본값)**: 같은 `axis→deck(axisId)→card` 집계가 두 곳에서 복제될 위험을 차단. `CardRepository.findByUserIdAndAxisIdsAndStatus` 단일 read-model을 today(`ReviewQueryService.collectToday`)와 축 카드 뷰(`CardQueryService.findByAxisIds`)가 공유. eligibility(최소 간격) 재판정은 `SoftScheduleTemplate` 인메모리 책임 — read-model은 threshold 미보유.
  - ReviewSession Layer 1 수집은 변경 없음 — 축 스코프 조회는 별개의 "사용자 표현 흐름"으로 분리(§3.3 사유).
- **fix-axis-deck-full-integration (0.0.2v) — 2026-07-01** (`workflow/task/fix/sdd/version/0.0.2v/fix-axis-deck-full-integration.md`, [ADR021](../../../../../../../../docs/adr/ADR021-axis-deck-full-integration.md))
  - **확정 — Card→Deck→Axis 관계 그래프의 스키마 완전 연결**: `deck.axis_id`가 `NOT NULL`로 승격(Flyway V15)되면서 모든 Card가 반드시 어떤 축에 속함이 스키마 레벨에서 보장. 이전 "고아 Deck을 통한 카드 존재 가능성"은 불가능해짐. today/축뷰 read-model(`findByUserIdAndAxisIdsAndStatus`)이 미묘하게 다뤘던 "axisId=null Deck 필터" 케이스도 자동 제거.
  - Card 도메인·상태 전이·SoftSchedule 정책 자체는 변경 없음. 사용자 지시 "Axis가 곧 덱과 의미가 동일하다"의 스키마 반영에 따른 파생 효과만 기록.

## 관련 문서

- 상위 도메인 문서: `docs/DOMAIN.md` § Card / Tag / ReviewSession
- 연관 Product
  - `product-learningFacade.md` — Layer 1 기준 ReviewSession 추천이 LearningFacade에 의존
  - `product-aisuggestion.md` — **Spring AI(`ChatClient` + Vertex AI Gemini) 공통 인프라의 정본**. Card BC가 v2에서 자동 Tag 부여·Tag 의미 그룹핑·카드 추천 등을 도입할 때 동일 starter(`spring-ai-starter-model-vertex-ai-gemini`), 동일 `PromptTemplate`/`BeanOutputConverter` 패턴, 동일 에러 체계(`LF_SUGGEST_*`와 동형 `CARD_SUGGEST_*` 신설), 동일 Static Fallback 원칙을 그대로 재사용한다 (재발명 금지)
    - **v1.5 (갭 인지형 개인화 컨텍스트, `product-aisuggestion.md` Epic 6)**: Card BC는 stale 축 판정의 **데이터 원천** — Epic 2의 `lastViewedAt` 데이터를 cardIds 단위로 집계해 노출(`CardStalenessQuery` inbound 쿼리). aisuggestion BC의 outbound `CardStalenessQueryPort` Adapter가 본 쿼리를 호출. `PersonalizationContextPort` 정의·LLM 통합·메트릭 태깅은 `product-aisuggestion.md` Epic 6 책임. Card BC 도메인은 Spring AI / `PersonalizationContextPort`를 직접 import 금지
- 연관 ADR: ADR001 (PK 전략), ADR002 (Enum 저장), ADR003 (Soft Delete)
- v2 도입 시 참고 ADR: `product-aisuggestion.md` Product DoD의 "Spring AI 1.0 + Vertex AI Gemini 채택", "Structured Output — BeanOutputConverter", "Fallback 정책" ADR을 그대로 따른다

## 열린 질문 (Open Questions)

> 현 시점에서 결정을 보류한 항목. Story 진입 직전에 사용자 의사결정으로 재해석한다.

1. **MAX_VIEW와 MAX_DURATION 동시 도달 시 reason 우선순위** — 현재 잠정 `MAX_VIEW > MAX_DURATION` (Epic 2 메모). 동률 케이스가 실제 야간 배치에서 얼마나 자주 발생하는지 운영 데이터 부재 → ADR 작성 시점에 한 번 결정하고 이후 고정
2. **자동 만료 처리 시점 — 야간 배치 vs 세션 진입 시 동기 점검** — 야간 배치(03:00)만으로 충분한지, ReviewSession 진입 시 점검을 함께 둘지 (Epic 2 Story 2-2 메모). 사용자 체감 지연(최대 24h)과 트래픽 부담 트레이드오프
3. **같은 세션 내 동일 카드 중복 노출 시 viewCount 중복 카운트 처리** — 현재 정책 미정 (Story 2-1 엣지 메모). "세션 단위 1회만" vs "노출 단위 매번 +1"
4. **dailyTarget 미설정 유저 기본값** — 잠정 20장. 실 사용자 학습량 데이터 누적 후 재조정 (Story 6-2 메모)
5. **Tag 삭제 시 시스템 전역 효과 정책** — Tag는 시스템 전역 UNIQUE인데 한 유저가 만든 Tag를 다른 유저가 사용 중이면 삭제 권한 처리 미결 (Story 5-1 엣지). v1은 "본인이 부착한 카드에서 해제"만 허용하고 row 자체는 보존하는 방향이 유력
6. **`CardStalenessQuery` 인터페이스 위치** — Card BC의 application 레이어 inbound로 둘지, aisuggestion BC가 정의하는 outbound Port를 Card가 구현할지 미결. ADR007(BC 간 동기 도메인 이벤트) 패턴과의 정합성 검토 필요

---

# [Epic 1] Card 상태(ON_field / Archive) 도메인 재정의

## 목표

> Card 상태가 "학습 성공/실패"가 아닌 "지금 어디에 위치해야 하는가"로 재정의되어 ON_field와 Archive가 명확히 분리된 운영 구조를 갖춘다.

## 배경

- 기존 플래시카드 시스템은 한 번 올린 Card를 익숙해질 때까지 끝까지 반복하는 구조
- 이 서비스에서는 지금 전면 노출할 Card와 후방 대기할 Card를 나누는 것이 핵심
- Card 상태가 평가가 아닌 운영 위치임을 명확히 해야 퇴장에 대한 심리적 부담이 없어짐

## 포함 Story

- [ ] Story 1-1. Card ON_field / Archive 상태 정의 및 DB 반영
- [ ] Story 1-2. Archive UX 진입 경로를 ON_field 학습 흐름과 분리

## Epic 인수 시나리오

`유저가 신규 Card 생성` → `Card.status = ON_field 초기화 + enteredFieldAt 기록` → `Card 상세 화면 'ON_field' 뱃지 노출` → `유저가 '보관하기' 액션` → `Card.archive() 호출, status=Archive 전환` → `학습 홈 진입 시 ON_field 목록만 노출, Archive는 별도 탭` → `Archive 탭 진입 시 안내 문구` → `상태 전환 후에도 연결된 Tag/Deck 귀속 유지`

## Epic 완료 기준 (DoD)

- [ ] Card 상태가 ON_field / Archive로 구분되고 DB에 반영된다
- [ ] ON_field와 Archive의 의미가 문서화되고 UX에 반영된다
- [ ] 상태 전환(ON_field → Archive / Archive → ON_field)이 동작한다
- [ ] ADR 갱신: "Card 상태 모델 결정 사유"
- [ ] 연결된 Story 모두 Done

## 내부 메모 / 제약 사항

- 기술 제약: Card 상태값(ON_field / Archive)이 DB 스키마에 반영되어야 함 (`@Enumerated(STRING)` + CHECK)
- 기획 원칙: 상태는 학습 성공/실패가 아닌 노출 위치 — 이 원칙이 UX 문구에도 반영
- 연기된 항목: Archive 카드 자동 만료·정리 정책 — v2

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

### 상태 Enum 저장 방식

**Option A — Enum ORDINAL (정수 저장)**
- 거부 이유: Enum 선언 순서 변경 시 데이터가 조용히 오염 (ADR002와 동일 논거)

**Option B (선택) — VARCHAR(20) + `@Enumerated(EnumType.STRING)` + CHECK 제약** (ADR002 준수)
- 비용: 정수 대비 디스크 사용량 미세 증가
- 보상: DB 직접 조회 가독성 + 새 상태 추가 시 Flyway에 CHECK 갱신만 필요 (코드 자동 정합)

### 상태 전이 멱등 정책

**Option A — 같은 상태 재전이 시 예외 throw (방어적)**
- 거부 이유: ReviewSession 동시성·재시도 로직에서 "이미 archive된 카드 다시 archive"가 자연 발생 (배치 재실행). 매번 예외 처리하면 호출자 코드 복잡도 폭증

**Option B (선택) — `archive()` / `returnToField()` 멱등 — 같은 상태면 no-op + 이력 미생성**
- 비용: 호출자가 "이미 그 상태였는지" 알 수 없음 (필요 시 반환값으로 표현)
- 보상: 배치 재실행·동시성 충돌·이벤트 중복 수신이 모두 안전

### Archive UX 진입 경로

**Option A — 학습 홈에 ON_field / Archive 탭 동등 노출**
- 거부 이유: Archive 탐색 충동이 ON_field 학습 흐름을 끊음. 본 서비스의 "현재 집중" 철학과 충돌

**Option B (선택) — Archive는 별도 진입 경로 + ON_field 카드 N개 안내로 학습 복귀 유도**
- 비용: 사용자가 Archive를 의도적으로 찾아야 함 (탐색 비용)
- 보상: 학습 흐름 보호. Tag 기반 Archive 연결 후보는 학습 화면 안에서 제공해 탐색 자연성 보완 (Epic 5 Story 5-3)

---

## [Story 1-1] Card ON_field / Archive 상태 정의 및 DB 반영

### 사용자 가치

> As a **Card를 학습 중인 유저**,
> I want **Card가 ON_field 상태인지 Archive 상태인지 명확히 구분되길**,
> So that **지금 집중해야 할 Card와 배경 지식으로 보관된 Card를 혼동하지 않는다.**

### 설명

- ON_field: 현재 전면 노출 구간 — 지금 모르고 익숙하지 않은 것을 올려두는 스테이징 공간
- Archive: 전면 루프에서 내려갔지만 재호출 가능한 배경 지식층
- 상태 의미: 익숙해서 내려갈 수도 있고, 지금 가치가 낮아서 내려갈 수도 있음
- 도메인 행위: `Card.create()` → ON_field 초기, `Card.archive()` (멱등), `Card.returnToField()` (멱등)
- DB: `card.status VARCHAR(20) NOT NULL` + CHECK (`ON_FIELD`, `ARCHIVE`)

### 인수 조건 (Given/When/Then)

- [ ] Given 유저가 새 Card 생성, When 저장, Then `status = ON_field` 초기화 + `enteredFieldAt = now()`
- [ ] Given Card 상세 화면에서 ON_field 상태일 때, When 렌더링, Then 'ON_field' 뱃지 표시
- [ ] Given ON_field 카드에서 '보관하기' 액션 확인, When 호출, Then `Card.archive()` → status=Archive 전환
- [ ] Given Archive 카드 목록 진입, When 표시, Then ON_field 목록과 시각적으로 분리된 화면
- [ ] *(엣지 케이스 — 상태 전환 시 연결 유지)* Tag와 Deck 귀속 관계는 변경되지 않음
- [ ] *(엣지 케이스 — 멱등성)* 이미 Archive인 Card에 `archive()` 호출 시 상태 무변·이력 미생성

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: `Card.create()` 초기 상태 / `archive()` 멱등성 / `returnToField()` 멱등성
- [ ] Repository Slice 테스트: status 컬럼 CHECK 제약 + 인덱스 동작
- [ ] FE: ON_field/Archive 상태 뱃지 표시 UI
- [ ] 디자인 QA 통과
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 비목표

- 자동 만료(maxDuration/maxView) — Epic 2
- Archive → ON_field 복귀 UI — Epic 7
- Soft Delete 정책 — ADR003 범위 (이미 결정됨)

### INVEST 점검

- **Independent**: 도메인 모델 + DB 컬럼 + 기본 UI 뱃지만
- **Negotiable**: 상태값 이름(ON_field/Archive)은 도메인 합의, 사용자 노출 문구는 협의
- **Valuable**: Product 전체의 토대 — 이후 모든 Epic이 이 상태에 의존
- **Estimable**: 도메인 + DB + 멱등성 테스트로 명확
- **Small**: 5 SP
- **Testable**: 상태 전환 단위 테스트로 자동 검증

### 참고

- 관련 ADR: ADR002 (Enum 저장 — VARCHAR + CHECK)
- 관련 도메인: `docs/DOMAIN.md` § Card

---

## [Story 1-2] Archive UX 진입 경로를 ON_field 학습 흐름과 분리

### 사용자 가치

> As a **Card를 학습 중인 유저**,
> I want **Archive가 기본 학습 화면에서 바로 보이지 않길**,
> So that **ON_field 카드에 집중하는 흐름이 Archive 탐색 충동으로 방해받지 않는다.**

### 설명

- Archive는 기본 학습 홈에서 바로 노출되지 않고 별도 탭 또는 하위 메뉴로 분리
- ON_field 카드 학습 중 Tag 연결을 통한 Archive 접근은 허용 (Epic 5에서 상세 설계)

### 인수 조건 (Given/When/Then)

- [ ] Given 학습 홈 기본 진입, When 렌더링, Then ON_field 카드 목록이 먼저 표시되고 Archive는 별도 진입 경로
- [ ] Given Archive 탭 직접 진입, When 표시, Then "Archive는 ON_field 학습 중 연결이 필요할 때 참조하는 공간입니다" 안내 문구
- [ ] Given ON_field 카드 N개 존재 + Archive 진입, When 표시, Then "현재 ON_field 카드 N개가 있습니다. 학습으로 돌아갈까요?" 유도 메시지
- [ ] *(엣지 케이스 — ON_field 0개)* Archive 직접 탐색 제한 없음

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 테스트 코드 작성 (BE: ON_field/Archive 분리 조회 쿼리)
- [ ] 디자인 QA 통과 (FE: 네비게이션 구조 및 안내 문구)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 비목표

- Tag 기반 Archive 연결 후보 표시 — Epic 5 Story 5-3
- Archive 카드 정렬·필터 정교화 — v2

### INVEST 점검

- **Independent**: Story 1-1 완료 후 UI 분리만
- **Negotiable**: 안내 문구는 카피라이팅 협의
- **Valuable**: WIP 집중 흐름의 핵심 UX
- **Estimable**: 라우팅 + 화면 + 안내 메시지
- **Small**: 5 SP
- **Testable**: 라우팅·문구 노출 슬라이스 테스트

---

# [Epic 2] ON_field 무한 반복 방지 — maxDuration & maxView

## 목표

> ON_field에 들어온 Card가 기간(maxDuration)과 횟수(maxView) 예산 안에서만 머물고, 예산이 소진되면 자동으로 Archive로 이동하는 규칙이 동작한다.

## 배경

- ON_field에 Card가 무한 누적되면 학습 에너지가 분산되어 정작 중요한 Card를 못 봄
- "완벽히 익힐 때까지"가 아니라 "WIP 예산 안에서" 운영하는 것이 핵심
- ON_field → Archive 퇴장은 실패가 아니라 운영 규칙임이 명확해야 함

## 포함 Story

- [ ] Story 2-1. ON_field 진입 시점 및 노출 횟수 기록
- [ ] Story 2-2. maxDuration 초과 시 Archive 자동 이동
- [ ] Story 2-3. maxView 도달 시 Archive 즉시 이동

## Epic 인수 시나리오

`Card.create() → enteredFieldAt 기록 + viewCount=0` → `학습 화면 노출 시 Card.recordView() → viewCount+1, lastViewedAt 갱신` → `enteredFieldAt 기준 경과일 ≥ maxDuration 도달` → `야간 배치 또는 다음 세션 시 OnFieldBudget.resolveReason(card) → MAX_DURATION` → `Card.archive() + CardStatusHistory(reason=MAX_DURATION) 기록 + UX "순환 완료" 안내` → `OR viewCount = maxView 도달 → 즉시 archive + reason=MAX_VIEW`

## Epic 완료 기준 (DoD)

- [ ] Card의 ON_field 진입 시점(`enteredFieldAt`)과 노출 횟수(`viewCount`)가 기록된다
- [ ] maxDuration 초과 시 Archive 이동이 동작한다
- [ ] maxView 도달 시 Archive 이동이 동작한다
- [ ] 퇴장이 실패가 아닌 운영 규칙임이 UX 문구에 반영된다 (Epic 8과 연계)
- [ ] ADR 갱신: "maxDuration/maxView 우선순위 — MAX_VIEW 우선, 동시 도달 시 MAX_VIEW"
- [ ] 연결된 Story 모두 Done

## 내부 메모 / 제약 사항

- 기술 제약: 배치 또는 이벤트 기반으로 maxDuration 초과 감지 처리
- 기획 원칙: 퇴장 문구는 "실패"가 아닌 "순환" 언어로 — "이 카드는 잠시 쉬러 갑니다"
- **갭 컨텍스트 데이터 원천 책임**: 본 Epic이 기록·갱신하는 `lastViewedAt`은 `product-aisuggestion.md` Epic 6 (v1.5 갭 인지형 개인화 컨텍스트)의 **stale 축 판정 신호 원천**이다. 노출 수단은 Card BC가 제공하는 inbound 쿼리 `CardStalenessQuery.findLastViewedAtByCardIds(Set<Long> cardIds)` 또는 동등 — aisuggestion BC의 outbound Adapter가 호출해 cardIds → 최신 노출 시각을 받은 뒤 LearningFacade의 축 매핑과 결합. 쿼리 응답 시간 P95 ≤ 100ms 목표 (`card.last_viewed_at` 인덱스 활용). 본 쿼리는 read-only이며 Card 도메인 행위에 영향을 주지 않음. Card BC 도메인은 Spring AI / `PersonalizationContextPort`를 직접 import 금지
- 연기된 항목: 개별 Card별 maxDuration 커스터마이징 — v2

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

### 자동 만료 처리 시점

**Option A — 야간 배치만 (`@Scheduled` cron 03:00)**
- 장점: 운영 단순, 트래픽 영향 0
- 거부 측면: 사용자가 03:00 직후 학습하면 만료된 카드를 최대 24시간 이전에 봤을 수 있음

**Option B — ReviewSession 진입 시 동기 점검만**
- 장점: 사용자 체감 즉시성 (방금 만료된 카드 안 봄)
- 거부 측면: 세션 진입 트래픽이 매번 만료 점검 비용 부담. 미접속 사용자의 카드가 영원히 만료 안 됨 (분석·통계 왜곡)

**Option C (선택) — 야간 배치 기본 + Story 2-2 출시 후 운영 데이터로 동기 점검 추가 여부 결정** (Open Question 2)
- 비용: 사용자 체감 지연 최대 24h (수용 가능 — 카드 학습 주기 자체가 일 단위)
- 보상: 배치 단일 진입점으로 만료 정책 변경 시 1곳만 수정. 트래픽 부담 0

### `recordView()` 위치 — Card vs CardReview

**Option A — Application Service에서 viewCount += 1 / archive 분기 직접 처리**
- 거부 이유: 도메인 규칙(maxView 도달 → archive)이 Application에 누수. Card 도메인이 자기 불변식을 지키지 못함

**Option B (선택) — `Card.recordView()` 도메인 메서드 내부에서 `isLastView()` 판정 + `archive(MAX_VIEW)` 자동 호출**
- 비용: 메서드 1개에 "노출 기록 + 자동 archive" 두 책임이 들어감 (응집도 검토 필요)
- 보상: 카드 자체 불변식("maxView 초과 시 ON_FIELD 유지 금지")을 도메인이 자기 보호. 호출자(Review BC)는 분기 없이 단일 호출

**Option C — `CardReview.recordView()` (Review BC 내부 전용) → Card 노출 위임**
- 비용 + 보상: Option B와 병행 채택됨 — Review BC가 Card 직접 접근 금지 + Card 도메인 자기 불변식 동시 만족. 현재 구조

### 사용자 직접 보관 vs 자동 만료 사유 구분

**Option A — 모든 archive를 단일 reason으로 통합**
- 거부 이유: 운영 회고에서 "사용자가 직접 보관한 비율" vs "예산 소진 자동 archive 비율"을 구분 못 함. UX 문구(Epic 8)에서도 사유별 차별화 불가

**Option B (선택) — `ArchiveReason` Enum 3종 (`MANUAL` / `MAX_VIEW` / `MAX_DURATION`) + 이력 필수 기록**
- 비용: Enum + 컬럼 1개 추가
- 보상: 운영 통계 + UX 문구 분기 + 디버그 추적성 모두 확보

### `enteredFieldAt` / `viewCount` / `lastViewedAt` 초기화 정책

**Option A — Archive → ON_field 복귀 시 누적값 유지 (이전 viewCount 합산)**
- 거부 이유: "새 사이클"이라는 운영 의미가 무너짐. 복귀 직후 즉시 maxView 초과로 재 archive되는 무한 루프 위험

**Option B (선택) — 복귀 시 `enteredFieldAt = now()`, `viewCount = 0`, `lastViewedAt = null` 재초기화 + 이전 이력은 `CardStatusHistory`로 보존**
- 비용: "이 카드를 총 몇 번 봤는가" 누적 카운트가 카드 컬럼에 없음 (필요 시 이력 합산)
- 보상: 사이클 의미 명확. 새 사이클이 이전 사이클 영향 없이 시작 (Epic 7과 정합)

---

## [Story 2-1] ON_field 진입 시점 및 노출 횟수 기록

### 사용자 가치

> As a **Card 스케줄링 시스템**,
> I want **Card의 ON_field 진입 시점과 노출 횟수를 정확히 기록하길**,
> So that **maxDuration과 maxView 기반 퇴장 규칙이 정확하게 동작할 수 있다.**

### 설명

- `enteredFieldAt`: Card가 ON_field 상태로 전환된 시점 (Archive → ON_field 복귀 시 재기록)
- `viewCount`: ON_field 상태에서 학습 화면에 노출된 횟수
- 도메인 메서드: `Card.recordView()` — Archive 상태면 무시 (멱등), ON_field면 viewCount +1, lastViewedAt 갱신
- 외부 주입 금지 필드 (setter 없음)

### 인수 조건 (Given/When/Then)

- [ ] Given Card가 ON_field 상태로 전환, When 저장, Then `enteredFieldAt = now()`
- [ ] Given ON_field Card가 학습 화면에 노출, When `recordView()` 호출, Then `viewCount += 1`, `lastViewedAt = now()`
- [ ] Given Archive → ON_field 복귀, When 호출, Then `enteredFieldAt = now()`, `viewCount = 0` 재초기화
- [ ] Given Archive 상태 Card에 `recordView()`, When 호출, Then 무시 (예외 미발생, 카운트 미증가)
- [ ] *(엣지 케이스 — 동일 세션 중복 노출)* 같은 세션에서 동일 Card 반복 노출 시 viewCount 중복 카운트 방지 기준 정의 필요 (세션 ID 또는 마지막 노출 시각 비교)

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: `recordView()` 멱등성 / Archive 무시 / 복귀 시 재초기화
- [ ] Repository Slice: 컬럼 인덱스 동작
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 비목표

- 노출 이력 상세 (어떤 세션에서) — v2
- 자동 만료 처리 — Story 2-2 / 2-3

### INVEST 점검

- **Independent**: Card 도메인 행위만. 만료 로직과 분리
- **Negotiable**: 동일 세션 중복 카운트 정책은 합의 필요
- **Valuable**: 이후 만료 규칙의 데이터 토대
- **Estimable**: 메서드 3종 + 멱등 테스트
- **Small**: 5 SP
- **Testable**: 단위 테스트로 결정적 검증

---

## [Story 2-2] maxDuration 초과 시 Archive 자동 이동

### 사용자 가치

> As a **Card를 학습 중인 유저**,
> I want **ON_field의 Card가 maxDuration을 넘기면 자동으로 Archive로 이동되길**,
> So that **오래된 Card가 ON_field를 점유하는 상황이 자동으로 해소된다.**

### 설명

- `enteredFieldAt` 기준 경과 일수 계산, maxDuration 초과 시 Archive 이동
- `viewCount`와 무관하게 기간 초과만으로 퇴장 가능
- 도메인 서비스: `OnFieldBudget.resolveReason(card)` → `Optional<ArchiveReason>` (MAX_DURATION / MAX_VIEW / empty)
- 자동화: 야간 배치 또는 세션 진입 시 동기 점검 (정책 ADR 결정 필요)

### 인수 조건 (Given/When/Then)

- [ ] Given `enteredFieldAt` 기준 maxDuration 초과 Card, When 점검 실행, Then `status = Archive` 자동 전환 + `CardStatusHistory(reason=MAX_DURATION)` 기록
- [ ] Given 자동 이동 후, When 유저에게 알림, Then "N개의 카드가 순환을 완료했습니다" 안내 (Epic 8 문구 정책)
- [ ] Given 퇴장 사유 기록, When 조회, Then `MAX_DURATION`이 이력에 명시
- [ ] *(엣지 케이스 — 유저 직접 보관과 구분)* 유저가 직접 Archive로 이동한 카드는 `reason=USER_ACTION`으로 별도 구분

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: `OnFieldBudget.resolveReason()` 경계값 (maxDuration-1, maxDuration, maxDuration+1)
- [ ] 통합 테스트: 자동 만료 → archive() → 이력 기록 흐름
- [ ] 디자인 QA 통과 (FE: 퇴장 안내 문구)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 8 SP

### 비목표

- 카드 단위 maxDuration 오버라이드 — v2
- 자동 복귀 — Epic 7 (유저 명시 액션만)

### INVEST 점검

- **Independent**: 도메인 서비스 + 배치/이벤트 트리거
- **Negotiable**: 동기 vs 비동기 점검 방식은 ADR
- **Valuable**: WIP 예산 자동 회수의 핵심
- **Estimable**: 도메인 + 배치 또는 세션 훅
- **Small**: 8 SP (배치 인프라 포함)
- **Testable**: 시각 의존성을 `Clock` 주입으로 격리

---

## [Story 2-3] maxView 도달 시 Archive 즉시 이동

### 사용자 가치

> As a **Card를 학습 중인 유저**,
> I want **ON_field의 Card가 maxView에 도달하면 즉시 Archive로 이동되길**,
> So that **같은 Card를 과하게 반복 노출하는 상황이 방지된다.**

### 설명

- `viewCount`가 maxView에 도달하는 순간 즉시 Archive 이동 (기간 미소진이라도)
- 목적은 "완벽히 외울 때까지 반복"이 아닌 "노출 예산 제한"
- `Card.recordView()` 내부에서 maxView 도달 시 자동 `archive()` 호출 (도메인 응집)

### 인수 조건 (Given/When/Then)

- [ ] Given `viewCount`가 maxView 도달, When `recordView()` 종료, Then `archive(reason=MAX_VIEW)` 즉시 호출
- [ ] Given maxView 도달 퇴장, When 이력 기록, Then `reason=MAX_VIEW`로 기록
- [ ] Given maxView 도달 직전 Card 노출, When 표시, Then "이 카드는 이번이 마지막 노출입니다" 안내
- [ ] *(엣지 케이스 — 동시 도달)* maxDuration과 maxView가 같은 시점에 도달하면 `MAX_VIEW` 우선 (ADR)

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: `recordView()` 임계 도달 시 자동 archive
- [ ] 단위 테스트: 동시 도달 우선순위 (MAX_VIEW > MAX_DURATION)
- [ ] 디자인 QA 통과 (FE: 마지막 노출 안내)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 비목표

- 카드 단위 maxView 오버라이드 — v2

### INVEST 점검

- **Independent**: `recordView()` 내부 확장만
- **Negotiable**: 동시 도달 우선순위는 ADR
- **Valuable**: 반복 노출 방지의 즉시 안전망
- **Estimable**: 도메인 행위 1개 + 임계 테스트
- **Small**: 5 SP
- **Testable**: 임계 경계값 단위 테스트

---

# [Epic 3] 짧은 기간 과도한 반복 방지 — Soft Schedule

## 목표

> Card가 마지막 노출 이후 정해진 최소 간격이 지나야 다시 노출 후보가 되어 짧은 시간 내 같은 Card를 반복해서 보는 뺑뺑이 현상이 방지된다.

## 배경

- ON_field에서 같은 Card를 짧은 시간 안에 여러 번 보는 것은 에너지 낭비
- 강한 due(반드시 그날 봐야 하는) 방식은 밀린 카드가 부채처럼 쌓이는 문제 발생
- "그 시점 이후부터 다시 볼 수 있는" 약한 interval 모델이 적절

## 포함 Story

- [ ] Story 3-1. Card 마지막 노출 시각 기록 및 soft schedule 간격 적용
- [ ] Story 3-2. 초기 Soft Schedule state 정의

## Epic 인수 시나리오

`Card 노출 → lastViewedAt 갱신` → `다음 ReviewSession 진입 시 (now - lastViewedAt) 기준 state 계산` → `1일/3일/7일 간격 통과 여부 판정` → `통과 카드만 노출 후보` → `미통과 카드는 다음 통과 시점까지 휴식 (부채 미발생)`

## Epic 완료 기준 (DoD)

- [ ] Card의 `lastViewedAt`이 기록된다
- [ ] soft schedule 간격 미충족 Card는 노출 후보에서 제외된다
- [ ] overdue가 부채처럼 쌓이지 않는 운영 원칙이 적용된다
- [ ] 초기 soft schedule 템플릿이 정의된다
- [ ] ADR 갱신: "Soft Schedule 정책 — 약한 interval, 부채 미발생"
- [ ] 연결된 Story 모두 Done

## 내부 메모 / 제약 사항

- 기술 제약: `lastViewedAt` 기준 경과 시간 계산으로 노출 후보 필터링
- 기획 원칙: 1일/3일/7일은 강한 due가 아닌 최소 간격 — 지나도 안 봐도 부채 없음
- 연기된 항목: 개인 학습 패턴 기반 간격 자동 조정 — v2

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

### Soft Schedule state 표현

**Option A — Card 컬럼으로 현재 state 영속화 (`soft_schedule_state VARCHAR`)**
- 거부 이유:
    - state는 `lastViewedAt + 모드별 간격`에서 결정적으로 파생되는 값 — 영속하면 동기화 책임 발생
    - 모드 변경(Epic 4 Story 4-3)이나 시각 경과만으로 state가 바뀌는데 컬럼은 stale
    - 사실상 캐시 — 캐시 무효화 트리거를 도메인 곳곳에 심어야 함

**Option B (선택) — `SoftScheduleTemplate.resolveState(card, now, mode)` 도메인 서비스로 매 호출 파생**
- 비용: 노출 후보 쿼리에서 state별 필터 시 매번 계산 (단순 산술이므로 부담 미미)
- 보상: 도메인이 항상 정합. 캐시 일관성 문제 0. 모드 변경이 자동 반영

### 간격 필터 — Repository vs Application

**Option A — Application Service가 카드 전체를 로드한 뒤 in-memory 필터**
- 거부 이유: 대규모 카드 시 메모리·네트워크 비용 폭증. ReviewSession 빈도 고려 시 비현실적

**Option B (선택) — Repository 쿼리 단계에서 `WHERE last_viewed_at IS NULL OR last_viewed_at <= now() - INTERVAL state_days DAY` 필터**
- 비용: `lastViewedAt` 인덱스 의존. QueryDSL 동적 조건 코드 필요
- 보상: DB가 필터 — Application은 결과 카드 수만 받음. 성능 안정적

### state 건너뛴 카드 처리

**Option A — 가장 작은 단계로 강제 매핑 (1일차로만 인식)**
- 거부 이유: 7일 안 본 카드와 1일 지난 카드를 동일 우선순위로 추천 — 도메인 의미 손상

**Option B (선택) — 도달한 가장 큰 state로 매핑 (가장 오래된 우선순위 표현)**
- 비용: 추천 알고리즘이 "큰 state = 우선" 가정에 결합
- 보상: 오래 안 본 카드의 우선 노출 보장. ReviewSession 비율 추천(Epic 6)에서 자연스럽게 활용

### 부채(due 누적) 처리

**Option A — 못 본 카드 누적 표시 ("밀린 카드 N장")**
- 거부 이유: 본 서비스 철학(부담 없는 순환) 정면 위배. 사용자 압박 → 학습 포기 패턴

**Option B (선택) — 부채 개념 자체 없음. 통과 카드만 후보로 풀(pool)에 들어감**
- 비용: 회수율 측정 어려움 (안 본 카드는 그냥 풀에 머무름)
- 보상: 사용자 부담 0. 학습 에너지가 있는 날 자연 처리. Epic 8의 "순환 언어"와 정합

---

## [Story 3-1] Card 마지막 노출 시각 기록 및 soft schedule 간격 적용

### 사용자 가치

> As a **Card를 학습 중인 유저**,
> I want **마지막으로 본 Card가 일정 간격이 지나야 다시 나타나길**,
> So that **같은 Card를 짧은 시간 안에 반복해서 보는 상황이 방지된다.**

### 설명

- `lastViewedAt`: Card가 학습 화면에 마지막으로 노출된 시각
- soft schedule 간격이 지나기 전까지는 노출 후보에서 제외
- 추천 쿼리에서 `WHERE lastViewedAt IS NULL OR lastViewedAt <= now() - INTERVAL state_days DAY`

### 인수 조건 (Given/When/Then)

- [ ] Given Card가 학습 화면에 노출, When `recordView()` 호출, Then `lastViewedAt = now()` 업데이트
- [ ] Given `lastViewedAt` 기준 soft schedule 간격 미통과, When 추천 쿼리 실행, Then 후보 제외
- [ ] Given 간격 통과 후, When 추천 쿼리 실행, Then 후보 포함
- [ ] *(엣지 케이스 — 신규 카드)* `lastViewedAt`이 null인 신규 Card는 즉시 후보 포함

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: `recordView()` 시 `lastViewedAt` 갱신
- [ ] Repository Slice: 간격 필터링 쿼리 + 인덱스 동작
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 8 SP

### 비목표

- 개인별 간격 조정 — v2
- 강제 due (밀린 카드 알림) — 명시적 비목표 (부채 미발생 원칙)

### INVEST 점검

- **Independent**: `recordView()` 확장 + 추천 쿼리 조건
- **Negotiable**: 간격 템플릿(1/3/7) 값은 모드별 조정 (Epic 4)
- **Valuable**: 학습 에너지 보호의 가장 큰 도구
- **Estimable**: 컬럼 + 쿼리 조건 + 인덱스
- **Small**: 8 SP
- **Testable**: 시각 기반 쿼리는 `Clock` 주입 + Repository Slice

---

## [Story 3-2] 초기 Soft Schedule state 정의

### 사용자 가치

> As a **Card 스케줄링 시스템**,
> I want **각 Card가 현재 어떤 soft schedule state에 있는지 명확히 추적되길**,
> So that **ReviewSession에서 오늘 학습 가능한 state의 Card만 정확히 수집할 수 있다.**

### 설명

- soft schedule state: 마지막 열람 이후 경과 일수 기준
- 10일 모드 기준 state 예시
  - 1일차 state: `lastViewedAt` 기준 1일 경과
  - 3일차 state: 3일 경과
  - 7일차 state: 7일 경과
- 각 state는 ReviewSession에서 비율 기반 추천의 단위가 됨

### 인수 조건 (Given/When/Then)

- [ ] Given 각 Card, When state 계산 호출, Then 현재 state(1일/3일/7일 등) 반환
- [ ] Given state 판단 기준 문서, When 조회, Then `lastViewedAt + 간격` 공식 명확히 명시
- [ ] Given state가 지난 Card, When 표시, Then "학습 가능" 상태 + ReviewSession 수집 대상
- [ ] *(엣지 케이스 — 여러 단계 건너뜀)* 간격이 여러 단계를 건너뛰어 지난 경우 가장 최근 도달 state로 처리

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: state 계산 로직 (경계값 + 건너뜀 케이스)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 비목표

- state 비율 추천 — Epic 6 Story 6-2
- state별 UI 카드 표시 색상 — v2

### INVEST 점검

- **Independent**: state 계산 도메인 서비스만
- **Negotiable**: state 값은 모드별 조정 (Epic 4 의존)
- **Valuable**: 비율 추천의 단위
- **Estimable**: 계산 공식 + 경계값
- **Small**: 5 SP
- **Testable**: 입력(now, lastViewedAt, 간격) → 출력 state 결정적

---

# [Epic 4] 사용자별 학습 에너지 개인 설정 — maxDuration 입력 & 모드 매핑

## 목표

> 유저가 maxDuration을 직접 숫자로 입력하면 시스템이 내부 모드로 자동 매핑하여 maxView와 soft schedule이 결정되고, 사용자마다 다른 학습 에너지에 맞게 개인 설정된다.

## 배경

- 사용자마다 하루에 공부 가능한 에너지가 다름 — 동일한 설정을 강요하면 안 됨
- maxView를 세밀하게 직접 고르는 것보다 maxDuration 입력이 직관적
- 13일을 입력한 유저와 10일을 입력한 유저는 같은 10일 모드로 매핑 — 내부적으로 단순 운영
- 모드 매핑 알고리즘은 초기 단순 버전에서 점진적으로 고도화 예정

## 포함 Story

- [ ] Story 4-1. maxDuration 직접 입력 및 내부 모드 자동 매핑
- [ ] Story 4-2. 사용자별 설정값 독립 저장 및 조회
- [ ] Story 4-3. 운영 중 maxDuration 수정 및 재매핑

## Epic 인수 시나리오

`유저가 설정 화면에서 maxDuration=13 입력` → `매핑 규칙 (1~14: 10일 모드, 15~24: 20일 모드, 25+: 30일 모드)` → `10일 모드 매핑 + maxView/soft schedule 자동 결정` → `"10일 모드로 운영됩니다 (soft schedule: 1/3/7일)" 안내` → `user_schedule_config 테이블에 독립 저장` → `유저 재방문 시 동일 설정 적용` → `유저가 maxDuration=22로 수정 → 20일 모드 재매핑 + ON_field 카드 재계산`

## Epic 완료 기준 (DoD)

- [ ] 유저가 maxDuration을 직접 숫자로 입력할 수 있다
- [ ] 입력값이 내부 모드(10일/20일/30일 등)로 자동 매핑된다
- [ ] 매핑된 모드에 따라 maxView와 soft schedule이 자동 결정된다
- [ ] 유저별로 개인 설정값이 독립적으로 저장된다
- [ ] 운영 중 설정 수정이 가능하다
- [ ] ADR 갱신: "maxDuration 입력 → 모드 매핑 정책"
- [ ] 연결된 Story 모두 Done

## 내부 메모 / 제약 사항

- 기술 제약: 유저별 설정값을 `user_schedule_config` 테이블에 독립 저장
- 기획 원칙: 입력값 → 가장 가까운 모드로 내림 매핑 (13일 → 10일 모드)
- 기획 원칙: 모드 분기점 초기 기준 — 1~14일: 10일 / 15~24일: 20일 / 25+: 30일
- 연기된 항목: 학습 이력 기반 maxDuration 자동 추천 — v2 / 모드 고도화 알고리즘 — v2

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

### 입력값 매핑 — 자유 입력 vs 모드 직접 선택

**Option A — 사용자가 모드(10D/20D/30D)를 직접 선택 (드롭다운)**
- 장점: 매핑 로직 불필요. UX 단순
- 거부 이유: 사용자가 "내가 며칠을 원하는가"를 모드라는 추상 개념으로 번역해야 함. 본 도메인 의도("개인 학습 에너지")와 거리가 멈

**Option B (선택) — 사용자가 일수 자유 입력 → 시스템이 모드로 매핑**
- 비용: 매핑 정책 코드(`LearningModeMappingPolicy`) + 매핑 결과 안내 UI 필요
- 보상: 사용자가 자연어("나는 13일 정도")로 표현 가능. 매핑 정책 변경이 한 곳

### 매핑 방향 — 내림 vs 올림 vs 반올림

**Option A — 올림 매핑 (13일 입력 → 20일 모드)**
- 거부 이유: 입력보다 긴 사이클 강제 → 사용자 의도 위배. 13일 원한 사람이 20일을 받는 것은 운영 부담 가중

**Option B — 반올림 매핑 (13일 → 10일, 14일 → 20일)**
- 거부 이유: 경계값(14.5일)의 모호함. 사용자 입장에서 "1일 차이로 모드가 바뀌는" 인지 불편

**Option C (선택) — 내림 매핑 (13일 → 10일 모드, "빠른 순환 습관" 우선)**
- 비용: 사용자가 "내가 원한 13일이 왜 10일이 됐냐"는 첫 인지 마찰 (안내 문구로 보완)
- 보상: 더 빠른 순환 → 학습 습관 형성에 유리. 도메인 메모 "빠른 순환 습관 유리"와 정합 (`docs/DOMAIN.md` UserSchedule 8번)

### 모드 분기점 — 외부 설정 vs 도메인 상수

**Option A — `application.yml`에서 분기점 설정 (`schedule.mode.threshold.10d=14`)**
- 거부 이유: 분기점은 도메인 규칙(에빙하우스 곡선 기반)이지 운영 튜닝 값이 아님. 외부 설정으로 두면 환경 간 불일치 가능

**Option B (선택) — `LearningMode` Enum + `LearningModeMappingPolicy` 내 상수**
- 비용: 분기점 조정 시 코드 변경 + 배포 필요
- 보상: 도메인 진실 소스 단일화. 단위 테스트로 모든 경계값 검증 (`map(14)=10D`, `map(15)=20D`, `map(24)=20D`, `map(25)=30D`)

### 운영 중 수정 — 즉시 재계산 vs 지연 재계산

**Option A — 수정 즉시 모든 ON_field 카드의 만료 재평가 (대량 archive 가능)**
- 거부 이유:
    - 사용자가 maxDuration을 14→7로 줄이면 즉시 N장 archive가 한 번에 발생 → 학습 흐름 단절
    - 트랜잭션 비용 큼

**Option B (선택) — 다음 ReviewSession 진입(또는 다음 야간 배치)부터 새 모드 적용**
- 비용: 수정 직후 잠시 옛 모드 효과 남음 (최대 24시간)
- 보상: 사용자 학습 흐름 보호. 트랜잭션 단순. "변경은 다음 사이클부터"라는 명시적 안내 가능

---

## [Story 4-1] maxDuration 직접 입력 및 내부 모드 자동 매핑

### 사용자 가치

> As a **학습 설정을 하는 유저**,
> I want **ON_field 운영 기간을 숫자로 직접 입력하길**,
> So that **나의 학습 에너지에 맞는 Card 순환 속도를 내가 직접 결정할 수 있다.**

### 설명

- 유저가 숫자를 자유 입력 (예: 7, 10, 13, 20, 28)
- 입력값 기준으로 내부 모드 자동 매핑
  - 1~14일 → 10일 모드 (soft schedule: 1/3/7일)
  - 15~24일 → 20일 모드 (soft schedule: 1/3/7/14일)
  - 25일 이상 → 30일 모드 (soft schedule: 1/3/7/14/21일)
- 매핑 결과와 함께 "N일 모드로 운영됩니다" 안내 표시
- 도메인 서비스: `ScheduleModeMapper.map(int maxDurationInput)` → `ScheduleMode`

### 인수 조건 (Given/When/Then)

- [ ] Given 설정 화면, When 진입, Then maxDuration 숫자 입력창 표시
- [ ] Given 숫자 저장, When 매핑 실행, Then 내부 모드로 자동 매핑 + 결과 화면 표시
- [ ] Given 13 입력, When 저장, Then "10일 모드로 운영됩니다 (soft schedule: 1/3/7일)" 안내
- [ ] Given 매핑된 모드, When 적용, Then maxView와 soft schedule이 자동 설정
- [ ] *(엣지 케이스 — 비정상 입력)* 0 이하 또는 비정상 입력 시 "1 이상의 숫자를 입력해주세요" 안내

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: `ScheduleModeMapper.map()` 경계값 (1, 14, 15, 24, 25, 100)
- [ ] 디자인 QA 통과 (FE: maxDuration 입력 + 매핑 결과 표시)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 8 SP

### 비목표

- 자동 추천 — v2
- 모드 고도화 (더 많은 모드 추가) — v2

### INVEST 점검

- **Independent**: 매핑 도메인 + 입력 UI
- **Negotiable**: 모드 분기점은 ADR로 정착
- **Valuable**: 개인화의 핵심
- **Estimable**: 매핑 + UI
- **Small**: 8 SP
- **Testable**: 경계값 단위 테스트로 자동

---

## [Story 4-2] 사용자별 설정값 독립 저장 및 조회

### 사용자 가치

> As a **개인 설정을 관리하는 유저**,
> I want **내 maxDuration / maxView 설정이 다른 유저와 독립적으로 저장되길**,
> So that **나의 학습 에너지 기준이 유지되고 언제든 확인하고 수정할 수 있다.**

### 설명

- 유저별 설정: `maxDuration` 입력값, 매핑된 모드, `maxView`, `softScheduleIntervals`
- 설정 화면에서 현재 설정값 전체 조회 가능
- 기본값: `maxDuration` 입력 없을 시 10일 모드로 초기화
- DB: `user_schedule_config (user_id PK FK, max_duration_input, schedule_mode, max_view, soft_intervals JSON, updated_at)`

### 인수 조건 (Given/When/Then)

- [ ] Given 유저별 설정값, When 저장, Then `user_id` 단위로 독립 저장
- [ ] Given 설정 화면, When 진입, Then 현재 내 설정값 전체 한눈에 표시
- [ ] Given 최초 설정 미완료 유저, When 조회, Then 기본값(10일 모드) 자동 초기화
- [ ] *(엣지 케이스 — 변경 이력)* 설정값 변경 이력이 로그로 보존되어 이전 설정 확인 가능

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] Repository Slice: `user_schedule_config` UNIQUE(user_id) 검증
- [ ] 통합 테스트: 저장·조회 라운드 트립
- [ ] 디자인 QA 통과 (FE: 설정 현황 화면)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 비목표

- 변경 이력 UI 노출 — v2 (로그만 보존)

### INVEST 점검

- **Independent**: 저장소만. 매핑 로직과 분리
- **Negotiable**: 컬럼 스키마는 마이그레이션 합의
- **Valuable**: 개인화의 영속성
- **Estimable**: 테이블 + Repository + 조회 API
- **Small**: 5 SP
- **Testable**: Slice + 통합

### 의존성

- 선행: Story 4-1

---

## [Story 4-3] 운영 중 maxDuration 수정 및 재매핑

### 사용자 가치

> As a **Card를 운영 중인 유저**,
> I want **운영 중에도 maxDuration을 변경할 수 있길**,
> So that **학습 여건이 바뀌었을 때 Card 순환 속도를 조정할 수 있다.**

### 설명

- maxDuration 수정 시 새 입력값으로 모드 재매핑
- 현재 ON_field 카드의 남은 체류 기간이 새 maxDuration 기준으로 재계산
- 줄였을 때 이미 초과한 카드는 다음 세션 시작 시 자동 만료

### 인수 조건 (Given/When/Then)

- [ ] Given maxDuration 수정 저장, When 매핑, Then 새 입력값 기준 모드 재매핑
- [ ] Given 재매핑 후, When 적용, Then maxView와 soft schedule이 새 모드 기준으로 자동 업데이트
- [ ] Given maxDuration을 줄여 이미 초과한 ON_field 카드 발생, When 다음 세션 시작, Then Archive로 이동
- [ ] *(엣지 케이스 — 수정 직후 안내)* 현재 ON_field 카드 수와 예상 순환 시간을 안내 표시

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: 재매핑 후 ON_field 카드 재계산
- [ ] 통합 테스트: 수정 → 다음 세션 만료 흐름
- [ ] 디자인 QA 통과 (FE: 변경 효과 안내)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 비목표

- 수정 시 자동 마이그레이션 (즉시 모든 카드 재계산) — 정책상 다음 세션까지 지연

### INVEST 점검

- **Independent**: Story 4-1, 4-2 완료 후 수정 흐름만
- **Negotiable**: 재계산 시점(즉시 vs 다음 세션)은 합의됨
- **Valuable**: 학습 패턴 변동에 대한 적응성
- **Estimable**: 수정 API + 재매핑 + 안내
- **Small**: 5 SP
- **Testable**: 시간 진행 시뮬레이션으로 검증

### 의존성

- 선행: Story 4-1, 4-2

---

# [Epic 5] Tag 클릭 탐색 및 Tag 목록 관리

## 목표

> Tag가 클릭 가능한 탐색 진입점이 되고, 유저가 자신의 Tag 목록을 관리하며 ON_field 학습 중 Tag를 통해 관련 Archive 카드를 자연스럽게 참조할 수 있다.

## 배경

- Tag가 단순 텍스트 라벨로만 존재하면 연결 기능을 활용할 수 없음
- 유저가 어떤 Tag를 만들었는지 전체 목록을 볼 수 있어야 Tag 관리가 가능함
- Tag 클릭 → 해당 Tag를 가진 ON_field/Archive 카드 탐색이 핵심 연결 흐름

## 포함 Story

- [ ] Story 5-1. Tag 추가 및 전체 Tag 목록 관리
- [ ] Story 5-2. Tag 클릭 탐색 — ON_field/Archive 카드 목록 이동
- [ ] Story 5-3. ON_field 학습 중 Tag 기반 Archive 연결 후보 표시

## Epic 인수 시나리오

`유저가 Card에 Tag 입력 (자동완성 + 신규)` → `find-or-create로 시스템 전역 Tag 재사용 또는 신규 생성` → `Tag 관리 화면 Tag 목록 + 각 Tag 연결 Card 수` → `Tag 클릭 → 해당 Tag 카드 목록(ON_field/Archive 섹션 분리)` → `ON_field 학습 중 현재 Card Tag와 같은 Tag를 가진 Archive 카드를 학습 화면 하단 후보로 표시` → `공통 Tag 수 많을수록 상단·🔗 표시`

## Epic 완료 기준 (DoD)

- [ ] Tag 추가·삭제가 가능하다
- [ ] 유저의 전체 Tag 목록 화면이 존재한다
- [ ] Tag 클릭 시 해당 Tag를 가진 카드 목록으로 이동한다
- [ ] ON_field 학습 중 Tag 기반 Archive 연결 후보가 표시된다
- [ ] ADR 갱신: "Tag — 시스템 전역 유니크 + find-or-create 패턴 결정 사유"
- [ ] API 스펙 갱신: `GET /api/cards/archive?tags={ids}&context=on-learning`
- [ ] 연결된 Story 모두 Done

## 내부 메모 / 제약 사항

- 기술 제약: Tag-Card 다대다 관계, Tag 클릭 탐색 쿼리 성능 고려 (인덱스 + 커버링)
- 기획 원칙: Tag는 유저 직접 부착 — 자동 Tag 부여는 v2
- 연기된 항목
  - Tag 간 계층·의존 관계 — v2
  - **자동 Tag 부여 (LLM 기반)** — v2. Card 본문 + 기존 Tag 목록을 입력으로 Spring AI `ChatClient` 호출. Adapter 위치는 `Card/infrastructure/suggestion/gemini/LlmTagSuggestionAdapter`. `product-aisuggestion.md` Epic 2의 starter·`PromptTemplate`·`BeanOutputConverter` 인프라를 그대로 재사용하며 Card 측은 `TagSuggestionPort`만 신설. 도메인은 Spring AI에 직접 의존 금지
  - **Tag 의미 그룹핑** — v2. Spring AI 임베딩 API 또는 LLM 분류 호출

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

### Tag 부착 — find-or-create vs 사전 생성

**Option A — Tag 사전 생성 후 부착 (`POST /tags` → `POST /cards/{id}/tags/{tagId}`)**
- 거부 이유:
    - 사용자 입력 흐름이 2단계로 분리 → 부착 누락 빈발
    - FE가 "이 value의 Tag가 시스템에 있는가" 사전 조회 필요 → 트래픽 증가
    - 동시성 — 두 사용자가 동시에 같은 value Tag 생성 시 UNIQUE 충돌

**Option B (선택) — find-or-create — 사용자는 value만 전달, 서버가 단일 트랜잭션에서 find 또는 create 후 즉시 부착**
- 비용: 서버 측 동시성 처리 필요 (`ON DUPLICATE KEY` 또는 retry)
- 보상: 사용자는 "텍스트만 입력" 단순. FE 코드 단순. 동시성은 서버 책임으로 한정

### Tag-Card 매핑 — 복합 PK vs surrogate ID

**Option A — `card_tag (card_id, tag_id) PRIMARY KEY`**
- 장점: 중복 자동 차단, 외래 키 조회 단순
- 거부 이유: ADR001(모든 PK는 BIGINT AUTO_INCREMENT) 위반 — 일관성 깨짐

**Option B (선택) — `card_tag (id PK, card_id, tag_id) + UNIQUE(card_id, tag_id)` (ADR001 준수)**
- 비용: surrogate id 컬럼 1개 추가
- 보상: 다른 매핑 테이블(`topic_material`)과 동일 패턴. JPA 매핑 표준화

### Tag 부착 상한 — 강제 vs 안내

**Option A — 상한 없음 (사용자 자율)**
- 거부 이유: Tag 남용 시 카드 간 연결 의미 희석 (모든 카드가 모든 카드와 "관련" 표시) → Story 5-3 연결 후보 기능 무의미

**Option B (선택) — 카드당 최대 3개 강제 (`CARD_TAG_LIMIT_EXCEEDED`)**
- 비용: 사용자가 4번째 Tag 부착 시 기존 Tag 제거 필요 → 인지 부담
- 보상: 카드 간 연결 밀도 보장 (= 도메인 의도). 도메인 메모 "Tags 최대 3개 (밀도 높은 연결 의도)"와 정합

### Tag 삭제 정책

**Option A — Tag row hard delete + cascade로 모든 `card_tag` 삭제 (시스템 전체)**
- 거부 이유: 시스템 전역 UNIQUE Tag를 한 사용자가 삭제하면 다른 사용자 카드에서도 해제됨 → 권한 문제

**Option B (선택, 잠정) — 사용자 본인이 부착한 `card_tag` row만 해제. Tag row 자체는 보존** (Open Question 5)
- 비용: 사용 카드 수 0인 dead Tag가 누적될 수 있음 (운영 cleanup 필요)
- 보상: 다른 사용자 영향 0. 향후 다시 같은 value 입력 시 기존 row 재사용

### Archive 연결 후보 정렬 기준

**Option A — 가장 최근 본 카드 우선 (lastViewedAt 기준)**
- 거부 이유: Archive 연결의 의도는 "관련 배경 지식 참조"인데 최근성은 무관

**Option B (선택) — 공통 Tag 수 많을수록 상단 (연결 강도)**
- 비용: 매 조회 시 GROUP BY + COUNT 비용
- 보상: 의미적 관련성 자연 표현. 🔗 표시(2개 이상)와 직접 정합

---

## [Story 5-1] Tag 추가 및 전체 Tag 목록 관리

### 사용자 가치

> As a **Card를 관리하는 유저**,
> I want **Tag를 새로 추가하고 내가 만든 Tag 목록 전체를 볼 수 있길**,
> So that **어떤 Tag를 사용 중인지 파악하고 일관된 Tag 체계를 유지할 수 있다.**

### 설명

- Card 상세 화면에서 Tag 입력창 — 기존 Tag 목록 자동완성 + 신규 입력 가능
- Tag 관리 화면: 내가 만든 전체 Tag 목록, 각 Tag에 연결된 Card 수
- Tag 삭제 시 연결된 Card에서도 해당 Tag 해제
- Tag는 시스템 전역 유니크 (find-or-create) — 같은 `value`는 단일 row

### 인수 조건 (Given/When/Then)

- [ ] Given Card 상세 화면 Tag 입력, When 텍스트 입력, Then 기존 Tag 자동완성 표시
- [ ] Given 새로운 텍스트 입력 후 저장, When 호출, Then 새 Tag 생성 + 해당 Card 부착
- [ ] Given Tag 관리 화면, When 진입, Then 내 전체 Tag 목록 + 각 Tag 연결 Card 수 표시
- [ ] Given Tag 삭제, When 호출, Then 부착된 모든 Card에서 Tag 해제
- [ ] *(엣지 케이스 — 중복 입력)* 동일 Tag 중복 입력 시 추가 없이 기존 Tag 유지

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: Tag find-or-create / 부착 / 해제
- [ ] Repository Slice: `tag.value` UNIQUE + `card_tag` UNIQUE(card_id, tag_id) 검증
- [ ] 디자인 QA 통과 (FE: 자동완성 + Tag 관리 화면)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 8 SP

### 비목표

- **자동 Tag 부여 (LLM 기반)** — v2. Spring AI `ChatClient` + `TagSuggestionPort` Adapter로 도입 예정 (`product-aisuggestion.md` 인프라 재사용, 도메인 직접 의존 금지)
- Tag 계층 구조 — v2
- Tag 의미 기반 자동 그룹핑 (임베딩 기반) — v2

### INVEST 점검

- **Independent**: Tag 도메인 + 다대다 매핑만
- **Negotiable**: 자동완성 빈도/정렬은 UX 합의
- **Valuable**: 탐색 흐름의 출발점
- **Estimable**: 도메인 + 매핑 + UI
- **Small**: 8 SP
- **Testable**: 도메인 + Slice 조합

---

## [Story 5-2] Tag 클릭 탐색 — ON_field/Archive 카드 목록 이동

### 사용자 가치

> As a **Card를 탐색 중인 유저**,
> I want **Tag를 클릭하면 해당 Tag가 붙은 카드 목록으로 이동하길**,
> So that **같은 주제로 묶인 ON_field와 Archive 카드를 한눈에 보고 연결 관계를 파악할 수 있다.**

### 설명

- Card 상세, Tag 목록 화면 등 Tag가 표시되는 모든 곳에서 클릭 가능
- 클릭 시 해당 Tag를 가진 카드 목록 (ON_field/Archive 구분하여 표시)
- 연결 강도: 공통 Tag 수 많을수록 상단 노출

### 인수 조건 (Given/When/Then)

- [ ] Given Card 상세 Tag 뱃지 클릭, When 호출, Then 해당 Tag 카드 목록 화면으로 이동
- [ ] Given Tag 카드 목록, When 렌더링, Then ON_field와 Archive 카드가 섹션으로 구분
- [ ] Given Tag 관리 화면에서 Tag 클릭, When 호출, Then 동일하게 카드 목록 이동
- [ ] *(엣지 케이스 — 0개)* 해당 Tag 카드가 0개이면 "이 Tag에 연결된 카드가 없습니다" 안내

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: Tag 기반 카드 목록 조회
- [ ] Repository Slice: `card_tag` 조인 인덱스 동작 + 정렬
- [ ] 디자인 QA 통과 (FE: Tag 클릭 탐색 + 카드 목록 UI)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 비목표

- Tag 조합 검색(AND/OR) — v2

### INVEST 점검

- **Independent**: 조회 쿼리 + 화면
- **Negotiable**: 정렬·페이징은 UX 합의
- **Valuable**: 탐색 진입점의 실현
- **Estimable**: 쿼리 + 라우팅 + 화면
- **Small**: 5 SP
- **Testable**: Slice + 화면 E2E

### 의존성

- 선행: Story 5-1

---

## [Story 5-3] ON_field 학습 중 Tag 기반 Archive 연결 후보 표시

### 사용자 가치

> As a **ON_field 카드를 학습 중인 유저**,
> I want **현재 카드의 Tag와 연결된 Archive 카드 후보를 학습 화면에서 확인하길**,
> So that **지금 모르는 개념과 관련된 과거 지식을 학습 흐름을 끊지 않고 참조할 수 있다.**

### 설명

- ON_field 카드 학습 화면 하단에 "관련 Archive 카드" 섹션
- 공통 Tag 수 많을수록 연결 강도 높음, 높은 순 정렬
- API: `GET /api/cards/archive?tags={tagIds}&context=on-learning`

### 인수 조건 (Given/When/Then)

- [ ] Given ON_field 카드에 Tag 존재, When 학습 화면 렌더링, Then 같은 Tag Archive 후보 목록 표시
- [ ] Given 복수 Archive 후보, When 정렬, Then 공통 Tag 수 기준 높은 순
- [ ] Given 공통 Tag 2개 이상 Archive 카드, When 표시, Then 연결 강도 높음 표시(🔗)
- [ ] *(엣지 케이스 — Tag 0개)* 현재 ON_field 카드에 Tag가 없을 때 "관련 Archive 카드" 섹션 미표시

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: Tag 기반 Archive 조회 + 공통 Tag 수 정렬
- [ ] Repository Slice: 다대다 조인 + GROUP BY 성능 확인
- [ ] 디자인 QA 통과 (FE: 관련 Archive 섹션 + 🔗 표시)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 8 SP

### 비목표

- Archive 카드 미리보기 모달 — v2
- 관련성 점수 알고리즘 (의미 기반) — v2

### INVEST 점검

- **Independent**: 쿼리 + 학습 화면 섹션
- **Negotiable**: 🔗 임계(2개)는 UX 합의
- **Valuable**: ON_field ↔ Archive 흐름의 완성
- **Estimable**: 쿼리 + 정렬 + 표시
- **Small**: 8 SP
- **Testable**: Slice + 화면 E2E

### 참고

- 관련 API 스펙: `GET /api/cards/archive?tags={tagIds}&context=on-learning`

---

# [Epic 6] ReviewSession — 일일 학습 카드 수집 및 비율 추천

## 목표

> 매일 학습 가능한 state의 ON_field 카드를 Layer 1 기준으로 수집하여 유저의 dailyTarget 수와 state 비율에 맞게 학습 카드 리스트를 추천하고, 추가 학습(+10장)도 동일 비율로 확장할 수 있다.

## 배경

- Deck 단위로 학습하면 특정 Deck에만 집중하는 starvation 효과 발생 가능
- Layer 1(직업 컨셉) 단위로 모든 Deck의 카드를 수집하면 균형 있는 학습 가능
- 오늘 학습 가능한 state의 카드는 soft schedule 간격을 통과한 카드
- 1일/3일/7일 state 비율대로 카드를 배분하면 학습 우선순위가 자연스럽게 반영됨

## 포함 Story

- [ ] Story 6-1. Layer 1 기준 오늘 학습 가능한 카드 수집
- [ ] Story 6-2. dailyTarget 설정 및 state 비율 기반 카드 추천
- [ ] Story 6-3. +N장 추가 학습 — 동일 비율 확장

## Epic 인수 시나리오

`오늘 날짜 기준 학습 가능 카드 수집 (Layer 1 전체 Deck 대상)` → `state별 분류 (1일/3일/7일)` → `유저 dailyTarget=30 + state 비율 (1:1:1)` → `1일 10 / 3일 10 / 7일 10 추천` → `유저 완료 후 "+10장" 클릭 → 동일 비율 (3·4·3) 확장` → `잔여 카드 모두 소진 시 "오늘 학습 완료" 안내`

## Epic 수준 완료 기준 (DoD)

- [ ] Layer 1 기준 오늘 학습 가능한 state의 ON_field 카드가 수집된다
- [ ] dailyTarget 설정 및 state 비율 기반 카드 추천이 동작한다
- [ ] ReviewSession 화면에서 대기 중인 카드 목록이 표시된다
- [ ] +N장 추가 학습이 동일 비율로 확장된다
- [ ] API 스펙: `GET /api/review-session/today?layer1Id={id}`
- [ ] ADR 갱신: "ReviewSession Layer 1 기준 수집 — starvation 회피"
- [ ] 연결된 Story 모두 Done

## 내부 메모 / 제약 사항

- 기술 제약: Layer 1 기준 전체 Deck 카드 조회 쿼리 성능 (커버링 인덱스)
- 기획 원칙: state별 카드 수가 불균형할 때 있는 카드 비율로 자동 조정
- 기획 원칙: 특정 Deck만 골라서 학습하는 옵션도 별도 제공 (starvation 인지 하에 선택)
- 기획 원칙: **v1 추천은 100% rule-based (state 비율 + dailyTarget 배분). Spring AI 미사용**. AI 호출은 응답 지연·비용·결정론성 손실이 매일 발생하는 ReviewSession에 부적합 — v2 도입 시에도 rule-based가 항상 폴백 경로로 유지
- 연기된 항목
  - state 비율 커스터마이징 (유저 설정) — v2 (AI 무관)
  - **학습 속도 기반 dailyTarget 자동 추천** — v2. 학습 이력 통계 우선, 필요 시 Spring AI `ChatClient` 보조 (`product-aisuggestion.md` 인프라 재사용)
  - **카드 우선순위 의미 기반 정렬** — v2. Spring AI 임베딩 또는 LLM 분류로 "지금 가장 필요한 카드" 가중치 산정

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

### 수집 단위 — Deck vs Layer 1

**Option A — Deck 선택형 (사용자가 매 세션 Deck 1개 골라 학습)**
- 장점: 사용자 의도 명확. 학습 흐름 직관적
- 거부 이유:
    - 한 Deck에만 카드가 집중되면 다른 Deck는 영원히 ReviewSession에 등장 못 함 (starvation)
    - 사용자가 무의식 중에 익숙한 Deck만 선택 → 학습 균형 깨짐

**Option B (선택) — Layer 1(LearningFacade) 전체 수집 + state 비율 배분 + Deck 단위 학습은 별도 메뉴**
- 비용: 사용자가 "오늘은 이 Deck만" 의도를 기본 흐름에서 표현 못 함 (별도 메뉴로 보완)
- 보상: 모든 Deck 카드 공평 등장. starvation ≤ 5% 세션 목표 달성. 성공 지표(Layer 1 starvation) 직결

### 추천 알고리즘 — rule-based vs AI

**Option A — Spring AI `ChatClient`로 매 세션 카드 우선순위 점수화**
- 장점: 의미 기반 가중치 (지금 학습이 필요한 카드 우선)
- 거부 이유:
    - 응답 지연(LLM 호출) — 매일 발생하는 ReviewSession 진입 비용 누적
    - 비용 — 일 N회 × 사용자 수 × LLM 토큰
    - 결정성 손실 — 같은 입력에 다른 추천. 사용자 신뢰 저하
    - v1 사용자 학습 데이터 부족 — 모델이 학습할 신호 없음

**Option B (선택) — 100% rule-based — state 비율 기반 배분, v2도 rule-based가 항상 폴백**
- 비용: "지금 가장 필요한 카드"라는 의미적 우선순위 표현력 부족
- 보상: 응답 즉시, 비용 0, 결정성 보장. v2 AI 도입 시 우선순위 가중치만 외부 호출하고 폴백은 유지 (`product-aisuggestion.md` 인프라 재사용 원칙)

### dailyTarget 미설정 기본값

**Option A — 추천 없음 (사용자 입력 강제)**
- 거부 이유: 첫 진입 사용자가 즉시 막힘 → 온보딩 마찰

**Option B (선택, 잠정) — 기본값 20장 (Open Question 4)**
- 비용: 사용자 평균 학습량 데이터 부재로 임의 결정
- 보상: 첫 사용자 진입 즉시 학습 가능. 운영 데이터 누적 후 재조정

### state 비율 — 동적 vs 고정

**Option A — 비율 고정 (예: 1일:3일:7일 = 1:1:1)**
- 거부 이유: state별 실제 카드 수가 불균형(예: 1일 100장, 7일 5장)인데 1:1:1 강제하면 7일 카드 부족으로 비율 깨짐

**Option B (선택) — 동적 비율 — 오늘 수집된 state별 카드 수에 비례하여 dailyTarget 배분**
- 비용: 비율이 매일 다름 → "왜 오늘은 1일 카드가 더 많지" 인지 마찰
- 보상: 실제 풀에 맞는 자연 분배. 카드 풀이 비어 있는 state는 자동 0장 처리

### +N장 추가 학습 N값

**Option A — 사용자가 매번 N 입력**
- 거부 이유: 매번 입력 마찰. "더 하고 싶다"는 자연스러운 흐름을 끊음

**Option B (선택) — 고정 +10장 (잔여 카드 부족 시 "남은 N장 모두 학습하기"로 동적 전환)**
- 비용: N=10이 적정한지 운영 데이터 부재 (`+5`/`+20`이 더 나을 수 있음)
- 보상: 1탭 추가 학습 → UX 단순. 잔여 부족 시 자동 적응

---

## [Story 6-1] Layer 1 기준 오늘 학습 가능한 카드 수집

### 사용자 가치

> As a **하루 학습을 시작하는 유저**,
> I want **오늘 학습 가능한 상태의 카드가 내 LearningFacade Layer 1 전체에서 수집되길**,
> So that **특정 Deck에 치우치지 않고 내 전체 학습 방향에서 균형 있는 카드를 만날 수 있다.**

### 설명

- 수집 기준: ON_field 상태 + soft schedule 간격을 통과한 카드 (학습 가능 state)
- 수집 범위: 유저의 LearningFacade Layer 1(직업 컨셉) 하위 모든 Deck 카드
- 수집된 카드는 state별로 분류되어 ReviewSession 대기열에 배치

### 인수 조건 (Given/When/Then)

- [ ] Given 오늘 날짜 기준 soft schedule 간격 통과 ON_field 카드, When 수집, Then Layer 1 전체 Deck에서 수집
- [ ] Given 수집된 카드, When 분류, Then state별(1일/3일/7일 등)로 ReviewSession 대기열 배치
- [ ] Given ReviewSession 진입, When 표시, Then 오늘 학습 가능한 카드 수가 state별로 표시
- [ ] *(엣지 케이스 — Layer 1 미설정)* LearningFacade Layer 1 미설정 유저는 전체 ON_field 카드 기준으로 수집

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] Repository Slice: Layer 1 기준 수집 쿼리 + 커버링 인덱스
- [ ] 단위 테스트: state별 분류 로직
- [ ] 디자인 QA 통과 (FE: ReviewSession 대기열 화면)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 13 SP

### 비목표

- Deck 단위 학습 옵션 — Epic 외 별도 메뉴 (이 Story 범위 아님)
- **카드 우선순위 의미 기반 정렬** — v2. Spring AI 인프라(`product-aisuggestion.md`) 재사용 도입 예정. v1은 rule-based 수집 + state 분류만

### INVEST 점검

- **Independent**: 수집 쿼리 + 분류 + 화면 표시
- **Negotiable**: 수집 범위(Layer 1)는 핵심 결정
- **Valuable**: 추천 흐름의 입력
- **Estimable**: 쿼리 복잡도 인지하여 13 SP
- **Small**: 단일 책임이지만 쿼리 복잡으로 13 SP
- **Testable**: Slice + 화면

### 참고

- 관련 API 스펙: `GET /api/review-session/today?layer1Id={id}`

---

## [Story 6-2] dailyTarget 설정 및 state 비율 기반 카드 추천

### 사용자 가치

> As a **ReviewSession을 시작하는 유저**,
> I want **하루에 학습할 카드 수(dailyTarget)를 설정하고 state 비율대로 추천받길**,
> So that **매일 일정한 학습량을 유지하면서 각 state의 카드를 균형 있게 학습할 수 있다.**

### 설명

- dailyTarget: 유저가 설정하는 하루 목표 카드 수 (예: 30장)
- state 비율 기반 배분: 오늘 수집된 state별 카드 수에 비례하여 dailyTarget 배분
- 비율 예시 (1일:3일:7일 = 2:2:1): 30장 → 12장/12장/6장

### 인수 조건 (Given/When/Then)

- [ ] Given 설정 화면, When 진입, Then dailyTarget 숫자 입력
- [ ] Given ReviewSession 시작, When 추천, Then 오늘 수집된 state별 카드 비율대로 dailyTarget 배분된 카드 리스트
- [ ] Given state별 카드 수가 dailyTarget보다 적음, When 추천, Then 있는 카드 전체를 학습 대상
- [ ] *(엣지 케이스 — 미설정)* dailyTarget 미설정 시 기본값 20장

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: state 비율 기반 배분 로직 (정확 비율 + 잔여 처리)
- [ ] 디자인 QA 통과 (FE: dailyTarget 설정 + 추천 카드 리스트)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 8 SP

### 비목표

- 비율 커스터마이징 (유저 설정) — v2 (AI 무관)
- **학습 속도 기반 dailyTarget 자동 추천** — v2. 학습 이력 통계 우선, 필요 시 Spring AI `ChatClient` 보조 (`product-aisuggestion.md` 인프라 재사용)

### INVEST 점검

- **Independent**: 비율 배분 도메인 서비스만
- **Negotiable**: 기본값(20)은 운영 데이터로 조정
- **Valuable**: 추천 결과 결정
- **Estimable**: 배분 알고리즘 + UI
- **Small**: 8 SP
- **Testable**: 배분 단위 테스트 결정적

### 의존성

- 선행: Story 6-1

---

## [Story 6-3] +N장 추가 학습 — 동일 비율 확장

### 사용자 가치

> As a **오늘 목표를 완료한 유저**,
> I want **추가로 더 학습하고 싶을 때 동일 비율로 카드를 더 추천받길**,
> So that **학습 에너지가 남을 때 부담 없이 추가 학습을 이어갈 수 있다.**

### 설명

- dailyTarget 완료 후 "+10장" 버튼 표시
- 추가 카드는 기존 state 비율과 동일하게 배분 (남은 대기 카드에서)
- 여러 번 반복 가능 (남은 카드가 있을 때까지)

### 인수 조건 (Given/When/Then)

- [ ] Given dailyTarget 완료, When 표시, Then "+10장 더 학습하기" 버튼 표시
- [ ] Given 버튼 클릭, When 추가 추천, Then 오늘 수집 비율과 동일한 비율로 10장 추가
- [ ] Given 추가 가능 카드 10장 미만, When 표시, Then "남은 카드 N장 모두 학습하기"로 버튼 변경
- [ ] *(엣지 케이스 — 모든 카드 소진)* "오늘 학습 가능한 카드를 모두 완료했습니다" 안내

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: 추가 배분 로직 + 잔여 카드 처리
- [ ] 디자인 QA 통과 (FE: +N장 버튼 + 추가 카드 리스트)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 비목표

- 무한 추가 (예산 미정의) — 명시적 비목표 (잔여 카드까지만)

### INVEST 점검

- **Independent**: 배분 재사용 + 버튼 UI
- **Negotiable**: +N의 N값(10)은 UX 합의
- **Valuable**: 학습 에너지 활용 극대화
- **Estimable**: 배분 재사용
- **Small**: 5 SP
- **Testable**: 추가 분기 단위 테스트

### 의존성

- 선행: Story 6-2

---

# [Epic 7] Archive → ON_field 복귀 규칙 설계

## 목표

> Archive의 Card가 "내려놓음 → 재회 → 재노출" 흐름으로 명시적 선택에 의해 ON_field로 복귀하고 새로운 사이클로 운영된다.

## 배경

- Archive는 버려진 Card가 아니라 필요할 때 다시 올라올 수 있는 상태여야 함
- 복귀가 없으면 "지금 내려놓고 나중에 다시 올린다"는 철학이 완성되지 않음
- 무작위 복귀를 방지하는 기준이 있어야 ON_field가 다시 혼잡해지지 않음

## 포함 Story

- [ ] Story 7-1. Archive 카드 "ON_field로 올리기" 및 새 사이클 시작

## Epic 인수 시나리오

`Archive 카드 상세 또는 Tag 기반 후보` → `"ON_field로 올리기" 명시적 액션` → `Card.returnToField()` → `status=ON_field, enteredFieldAt=now, viewCount=0 재초기화` → `이전 사이클 이력은 CardStatusHistory에 별도 보존` → `ON_field 목록에서 "복귀된 카드" 뱃지로 구분`

## Epic 완료 기준 (DoD)

- [ ] Archive 카드에 "ON_field로 올리기" 명시적 액션이 존재한다
- [ ] 복귀 시 새로운 ON_field 사이클(enteredFieldAt 재기록, viewCount 초기화)이 시작된다
- [ ] 복귀 이력이 기록된다
- [ ] ADR 갱신: "복귀는 명시적 액션만, 자동 복귀 미적용 사유"
- [ ] 연결된 Story 모두 Done

## 내부 메모 / 제약 사항

- 기술 제약: 복귀 시 enteredFieldAt 재기록·viewCount 초기화·이전 ON_field 이력 보존 구분 필요
- 기획 원칙: 복귀는 자동이 아닌 유저의 명시적 선택 또는 Tag 연결 기반
- 연기된 항목: 복귀 횟수 제한·쿨다운 정책 — v2

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

### 복귀 트리거 — 자동 vs 명시적 액션

**Option A — 의미·맥락 기반 자동 복귀 (시스템이 "지금 필요"라고 판단 시 자동 ON_field 이동)**
- 거부 이유:
    - 사용자 의도와 무관한 ON_field 카드 증가 → WIP 예산 침범
    - "왜 이 카드가 갑자기 올라왔지" 인지 마찰
    - 판단 로직 복잡 (AI/룰 기반 모두 v1 범위 초과)

**Option B (선택) — 사용자 명시적 액션만 (`POST /api/v1/cards/{id}/return-to-field`)**
- 비용: 사용자가 Archive를 탐색·선택해야 함 (탐색 비용)
- 보상: WIP 예산 사용자 통제. 의도와 결과 일치. Tag 연결 후보(Story 5-3)로 탐색 비용 자연 보완

### 복귀 시 viewCount/enteredFieldAt 정책

**Option A — 누적 (이전 viewCount + 새 노출 합산)**
- 거부 이유: "새 사이클"이라는 의미 손상. 복귀 직후 즉시 maxView 초과로 재 archive 위험

**Option B (선택) — 재초기화 (`enteredFieldAt = now()`, `viewCount = 0`, `lastViewedAt = null`)** (Epic 2 결정과 정합)
- 비용: 평생 누적 카운트가 카드에 없음 (필요 시 `CardStatusHistory` 합산)
- 보상: 새 사이클 의미 명확. ON_field 운영이 이전 사이클 영향 없음

### 복귀 이력 기록 위치

**Option A — Card 도메인에 `restoreCount` 컬럼 추가**
- 거부 이유: 카드 정체성과 무관한 운영 메타데이터. 카드 행위가 늘어남 (응집도 저하)

**Option B (선택) — `CardStatusHistory`에 reason=null + `fromStatus=ARCHIVE, toStatus=ON_FIELD` 행 누적**
- 비용: 복귀 횟수 조회 시 이력 카운트 필요 (직접 조회보다 비용)
- 보상: 도메인은 "현재 상태"만 보유. 이력은 시계열 진실 (Single Source of Truth)

### "복귀된 카드" UI 구분 표시 정책

**Option A — 표시하지 않음 (복귀 후엔 일반 ON_field 카드와 동일 취급)**
- 거부 이유: 사용자가 "어떤 카드를 다시 올렸는지" 추적 못 함. 학습 회고 가치 손실

**Option B (선택) — ON_field 목록에서 "복귀된 카드" 뱃지 N일간 표시 (이력 기반)**
- 비용: 매 카드 조회 시 최신 status 전환 이력 1건 조회
- 보상: 사용자 회고 가치 + Epic 8 "순환 언어"와 정합 ("다시 만나요" 흐름의 시각화)

---

## [Story 7-1] Archive 카드 "ON_field로 올리기" 및 새 사이클 시작

### 사용자 가치

> As a **Archive를 참조 중인 유저**,
> I want **필요하다고 판단된 Archive 카드를 ON_field 상태로 올릴 수 있길**,
> So that **필요한 카드만 선별해서 현재 학습 전면에 두고 새로운 학습 사이클을 시작할 수 있다.**

### 설명

- Archive 카드 상세 또는 Tag 기반 연결 후보에서 "ON_field로 올리기" 버튼 제공
- 복귀 시 `enteredFieldAt = now()`, `viewCount = 0` 재초기화, 이전 이력은 `CardStatusHistory`에 별도 보존
- 도메인 행위: `Card.returnToField()` — 멱등성 (이미 ON_field면 무시)

### 인수 조건 (Given/When/Then)

- [ ] Given Archive 카드 상세 "ON_field로 올리기" 확인, When 호출, Then ON_field 전환 + `enteredFieldAt = now()` 재기록
- [ ] Given 복귀, When 호출, Then `viewCount = 0` 초기화 + 이전 ON_field 체류 이력은 별도 로그 보존
- [ ] Given ON_field로 복귀된 카드, When 목록 표시, Then "복귀된 카드" 표시로 구분
- [ ] *(엣지 케이스 — 이미 ON_field)* 이미 ON_field 상태인 카드에는 "ON_field로 올리기" 버튼 미표시

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: `returnToField()` 멱등성 + 초기화 + 이력 보존
- [ ] 통합 테스트: 복귀 → ReviewSession 노출 흐름
- [ ] 디자인 QA 통과 (FE: 복귀 카드 구분 표시)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 비목표

- 자동 복귀 (의미·맥락 기반) — v2
- 복귀 횟수 제한·쿨다운 — v2

### INVEST 점검

- **Independent**: 도메인 행위 + UI 액션
- **Negotiable**: "복귀된 카드" 표시 방식은 UX 합의
- **Valuable**: 순환 모델의 완성
- **Estimable**: 도메인 + UI
- **Small**: 5 SP
- **Testable**: 단위 + 통합

---

# [Epic 8] WIP 모델 기반 사용자 설명 체계

## 목표

> ON_field·Archive·maxDuration·maxView·soft schedule·ReviewSession이 "지금 몇 장을 얼마 동안 붙잡고 있는가"라는 WIP 언어로 유저에게 납득 가능하게 전달된다.

## 배경

- 복잡한 스케줄링 규칙이 잘 설명되지 않으면 유저는 Card 퇴장을 실패로 인식함
- "완벽히 외울 때까지 반복"이 아닌 "WIP 예산 안에서 순환"임을 이해시켜야 함
- ReviewSession의 state 비율 추천 구조도 유저가 납득해야 일관되게 사용함

## 포함 Story

- [ ] Story 8-1. 카드 퇴장 문구를 순환 언어로 적용
- [ ] Story 8-2. 초기 운영 규칙 요약 — 한 장짜리 정책 문서 (인앱 노출)

## Epic 인수 시나리오

`Card 자동 퇴장 (MAX_DURATION)` → `"N개의 카드가 순환을 완료했습니다. 잠시 후 다시 만나요 👋" 안내` → `Card 자동 퇴장 (MAX_VIEW)` → `"이 카드는 충분히 노출되었습니다. 배경 지식으로 이동합니다"` → `유저 직접 보관` → `"카드를 Archive로 이동했습니다. 필요하면 언제든 다시 꺼내올 수 있어요"` → `유저가 설정 화면 → "운영 규칙 보기" → 한 페이지 요약 + state 비율 예시 확인`

## Epic 완료 기준 (DoD)

- [ ] ON_field가 WIP 구간으로 설명되는 UI 문구가 적용된다
- [ ] 카드 퇴장 문구가 "실패"가 아닌 "순환" 언어로 적용된다
- [ ] 초기 운영 규칙 한 장짜리 요약이 인앱에서 확인 가능하다
- [ ] ReviewSession 추천 구조가 유저 언어로 설명된다
- [ ] UI 텍스트 정책 문서(`docs/ux/wip-language.md`) 등록
- [ ] 연결된 Story 모두 Done

## 내부 메모 / 제약 사항

- 기획 원칙: 전문 용어(maxDuration, viewCount 등) 대신 유저 친화적 언어 사용
- 기획 원칙: 퇴장 문구 예시 — "이 카드는 잠시 쉬러 갑니다 👋" / "순환 완료"
- 연기된 항목: WIP 현황 시각화 대시보드 — v2

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

### 문구 — 코드 하드코딩 vs 메시지 번들

**Option A — Controller/Service 응답에 한국어 문구 직접 포함**
- 거부 이유:
    - 다국어 지원(v2) 시 전 코드베이스 대상 grep + 교체 필요 → 누락 위험
    - PO/디자이너의 문구 변경마다 코드 PR 발생 → 협업 마찰
    - 운영 메시지와 도메인 코드가 결합 → 단위 테스트 의존성 증가

**Option B (선택) — FE 메시지 번들 + Server는 `ArchiveReason` enum 코드만 반환**
- 비용: FE 측 메시지 매핑 1곳 유지 필요
- 보상: 다국어 확장 비용 0. PO 문구 변경이 FE PR 1건. 도메인 코드 안정

### "실패" 어휘 차단 — 규약 vs 자동 검증

**Option A — 코드 리뷰 시 사람이 검토 (규약)**
- 거부 이유: 누락 빈발. 성공 지표("'실패' 어휘 = 0건")를 사람 눈으로 보장 불가

**Option B (선택) — CI에 grep 단계 추가 (`grep -r "실패" frontend/src/messages/` → 0이어야 통과)**
- 비용: CI 시간 미세 증가. false positive 가능 (도메인 외 문맥의 "실패")
- 보상: 자동 강제. 성공 지표 직접 검증. 신규 문구 추가 시 즉시 차단

### 운영 규칙 요약 페이지 위치

**Option A — 별도 `/help` 라우트만 (사용자가 의도적으로 찾아야 함)**
- 거부 이유: 신규 사용자가 즉시 접근 못 함. 온보딩 시점에 필요

**Option B (선택) — 온보딩 흐름 중 1회 노출 + 설정 화면 진입점 항시 제공**
- 비용: 온보딩 흐름 변경 필요
- 보상: 신규 사용자가 운영 규칙을 학습 시작 전에 인지. 재방문 시 설정 화면에서 다시 참조 가능

### 묶음 퇴장 안내 정책

**Option A — 카드 단건마다 안내 알림 1건씩**
- 거부 이유: 야간 배치로 N장 동시 archive 시 알림 폭주 → 사용자 차단

**Option B (선택) — 일정 시간 내 다건 퇴장은 묶음 안내 ("N개의 카드가 순환을 완료했습니다")**
- 비용: 묶음 그룹핑 로직 + 시간 윈도우 결정 필요
- 보상: 알림 1건. 사용자 인지 부담 최소화. "순환" 언어 자연 표현

---

## [Story 8-1] 카드 퇴장 문구를 순환 언어로 적용

### 사용자 가치

> As a **Card가 Archive로 이동되는 상황을 경험하는 유저**,
> I want **카드 퇴장이 실패처럼 느껴지지 않는 문구로 안내되길**,
> So that **ON_field → Archive 이동을 학습 실패가 아닌 자연스러운 순환으로 받아들일 수 있다.**

### 설명

- maxDuration 초과 퇴장: "N개의 카드가 순환을 완료했습니다. 잠시 후 다시 만나요 👋"
- maxView 도달 퇴장: "이 카드는 충분히 노출되었습니다. 배경 지식으로 이동합니다"
- 유저 직접 보관: "카드를 Archive로 이동했습니다. 필요하면 언제든 다시 꺼내올 수 있어요"

### 인수 조건 (Given/When/Then)

- [ ] Given maxDuration 초과 퇴장, When 안내, Then "순환 완료" 언어 문구
- [ ] Given maxView 도달 퇴장, When 안내, Then 성공/실패가 아닌 중립적 완료 문구
- [ ] Given 유저 직접 보관, When 안내, Then "언제든 다시 꺼낼 수 있다" 함께 표시
- [ ] *(엣지 케이스 — 연속 퇴장)* 연속으로 여러 카드가 퇴장될 때 묶음 안내

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] UI 문구 grep으로 "실패" 어휘 0건 확인
- [ ] 디자인 QA 통과 (FE: 퇴장 문구 전체 적용)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 3 SP

### 비목표

- 다국어 — v2
- 문구 A/B 테스트 — v2

### INVEST 점검

- **Independent**: 문구만. 도메인 변경 없음
- **Negotiable**: 카피라이팅은 PO 합의
- **Valuable**: 유저 인식의 핵심
- **Estimable**: 문구 + 적용 위치
- **Small**: 3 SP
- **Testable**: grep + 화면 확인

---

## [Story 8-2] 초기 운영 규칙 요약 — 한 장짜리 정책 문서 (인앱 노출)

### 사용자 가치

> As a **처음 Card 스케줄링을 접하는 유저**,
> I want **이 시스템이 어떻게 돌아가는지 한눈에 보길**,
> So that **왜 카드가 올라가고 내려가는지, ReviewSession이 왜 이렇게 카드를 추천하는지 납득하고 사용할 수 있다.**

### 설명

- 온보딩 또는 설정 화면에서 접근 가능한 운영 규칙 요약 페이지
- ON_field / Archive / maxDuration / soft schedule / ReviewSession state 비율 추천을 유저 언어로 1페이지 요약

### 인수 조건 (Given/When/Then)

- [ ] Given 설정 화면 "운영 규칙 보기" 진입, When 표시, Then 전체 규칙이 한 페이지에 요약
- [ ] Given 요약 페이지 각 항목, When 표시, Then 전문 용어 없이 일상 언어로 설명
- [ ] Given ReviewSession state 비율 추천, When 설명, Then 예시와 함께 설명
- [ ] Given 온보딩 중 maxDuration 입력 화면, When 표시, Then 요약 페이지 링크 함께 제공
- [ ] *(엣지 케이스 — 규칙 업데이트)* 규칙 업데이트 시 요약 페이지도 함께 업데이트되는 관리 방식 정의 필요

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 디자인 QA 통과 (FE: 운영 규칙 요약 페이지 UI)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 3 SP

### 비목표

- 인터랙티브 튜토리얼 — v2
- 동영상 가이드 — v2

### INVEST 점검

- **Independent**: 정적 페이지
- **Negotiable**: 페이지 길이·예시 수는 PO 합의
- **Valuable**: 신규 유저 이해도 결정
- **Estimable**: 페이지 + 라우팅
- **Small**: 3 SP
- **Testable**: 화면 확인

---

## 자가 점검

- **레퍼런스 1:1 대응**: Product/Epic/Story 헤더 `[Product]/[Epic N]/[Story N-M]` 컨벤션. Outcome / 성공 지표 / Scope / Epic 목록 / 제품 DoD 5요소. Epic마다 목표·포함 Story·인수 시나리오·DoD. Story마다 사용자 가치·설명·G/W/T 인수 조건·DoD·비목표·INVEST 6요소 ✓
- **엣지 케이스 태깅**: 모든 Story가 `*(엣지 케이스 — 사유)*` 형태로 명시 ✓
- **DoD의 테스트+ADR**: 모든 Story DoD가 테스트 항목 포함. Product·Epic DoD에 ADR 항목 명시 (Card 상태 모델, maxDuration 매핑, Layer 1 수집, Tag 정책) ✓
- **실제 코드 정합**: 도메인 메서드 (`Card.create()`, `archive()`, `returnToField()`, `recordView()`), 도메인 서비스 (`OnFieldBudget.resolveReason()`, `CardStatusHistoryAppender`, `CardExpiryPolicy`), 테이블명 (`card`, `card_tag`, `tag`, `user_schedule_config`, `card_status_history`), `CoverageStatus` Enum, ErrorCode 패턴 (CARD_*) 모두 기존 표기와 일치 ✓
- **Spring AI 정합 (v1 비목표 + v2 인프라 재사용 원칙 명시)**: v1 Card BC는 Spring AI에 의존하지 않음을 모든 AI 관련 Out of Scope/비목표에 명시. v2 도입 항목(자동 Tag 부여, Tag 의미 그룹핑, dailyTarget 자동 추천, 카드 우선순위 의미 기반 정렬, 트렌드 기반 추천)은 `product-aisuggestion.md`의 Spring AI 1.0 인프라(`spring-ai-starter-model-vertex-ai-gemini` + `ChatClient` + `PromptTemplate` + `BeanOutputConverter` + Static Fallback)를 그대로 재사용하며, Card 도메인은 Spring AI에 직접 의존하지 않고 자체 `*SuggestionPort`를 신설하는 원칙 명시. ReviewSession은 v1·v2 모두 rule-based 폴백을 항상 유지 ✓
- **v1.5 갭 인지형 개인화 컨텍스트 — Card BC = stale 축 판정 데이터 원천 (Port 정의·LLM 통합은 별도 Product 책임)**: `product-aisuggestion.md` Epic 6의 갭 컨텍스트 중 **stale 축** 판정 신호 원천은 Epic 2의 `lastViewedAt`. 노출 수단은 Card BC가 제공하는 inbound 쿼리(`CardStalenessQuery.findLastViewedAtByCardIds(...)` 또는 동등) — read-only, 도메인 행위 영향 없음, P95 ≤ 100ms 목표. aisuggestion BC의 outbound `CardStalenessQueryPort` Adapter가 본 쿼리를 호출. Card BC 도메인은 Spring AI / `PersonalizationContextPort`를 직접 import 금지. LearningFacade와 Card BC가 각각 데이터 원천을 분담하고, BC 경계는 `CardStalenessQueryPort`(또는 동등 inbound 쿼리)로만 교차하는 헥사고날 분리 유지 ✓
- **측정 가능 지표**: ON_field 예산 위반 = 0건 / 자동 만료 누락률 ≤ 0.1% / starvation ≤ 5% 세션 / "실패" 어휘 = 0건 / 이력 보존 = 100% 등 모두 정량 ✓
