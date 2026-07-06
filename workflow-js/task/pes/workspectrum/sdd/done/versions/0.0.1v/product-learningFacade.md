# [Product] LearningFacade — 직업적 컨셉 기반 학습 지도

## 성과 (Outcome)

> **유저가 "내가 무엇을 하고 싶은 사람인가"를 시스템 안에서 정의(Layer 1)하고, 그 아래에 학습 축·하위 주제·학습 자료를 연결한 살아있는 학습 지도를 가질 수 있게 한다.**
> 지도는 한 번 만들고 끝나는 산출물이 아니라, 유저가 주제를 단련하고 자료를 채워가며 자기 사고 변화를 회고할 수 있는 도구다.

## 설계 결정 (Design Decisions)

> LearningFacade BC는 6개 핵심 결정으로 구성된다. 각 결정은 "유저 표현 자유 vs 시스템 정합성" / "도메인 응집 vs Application 조율" / "단순 v1 vs 확장 v2" 셋 중 하나의 축에서 갈렸다.

- **유저당 LearningFacade 1개 (v1 단일 컨셉)**
    - DB `UNIQUE(user_id) on learning_facade` + Application Service 선체크의 이중 방어. 다중 컨셉 동시 관리(v2)는 명시적 보류
    - 근거: "내가 무엇을 하고 싶은 사람인가"는 한 시점에 하나의 답이어야 단련 의미가 살아남. 다중 컨셉을 v1에 두면 회고 단위가 흐려지고 커버리지 집계 의미가 흐려짐
- **4-level hierarchy: Facade → Axis → Topic → Material**
    - `LearningFacade`(Aggregate Root) → `LearningAxis`(Entity) → `AxisTopic`(Entity) → `LearningMaterial`(Entity) + `TopicMaterial`(매핑 Entity)
    - Topic ↔ Material은 다대다 — surrogate `id` + `UNIQUE(topic_id, material_id)`로 매핑 중복 방지 (도메인 + DB 이중 방어)
    - 자식 컬렉션은 모두 `Collections.unmodifiableList(...)`로 노출, 추가/삭제는 Aggregate 행위로만
- **`coverageStatus` 역정규화 + 동일 트랜잭션 동기 갱신**
    - `axis_topic.coverage_status` 역정규화 컬럼 보유 (`NO_MATERIAL` / `PARTIAL` / `COVERED`)
    - `CoverageRecalculator`(Application Service)가 `TopicMaterial` 연결·해제 또는 `proficiencyLevel` 변경과 동일 트랜잭션 안에서 갱신 — **이벤트 기반 비동기 금지**
    - 갱신 트리거는 두 가지로 제한 (ADR004): TopicMaterial 연결/해제 + proficiencyLevel 변경. 주제 이름/설명 수정은 트리거 아님
- **`displayOrder` 1-based + 외부 주입 금지**
    - 첫 항목 `displayOrder = 1`, 신규 추가 시 `addX()`가 `현재 max + 1` 자동 계산
    - Application Service / Controller가 직접 부여 금지. 정적 분석으로 누락 검증
    - 재배치는 `reorderX(List<Long> orderedIds)`로만 — 전달 id 집합 ↔ 현재 컬렉션 id 집합 불일치 시 예외
- **다건 입력 트랜잭션 = 전체 롤백**
    - `LearningAxis.addTopics(List<TopicCommand>)`는 한 건 실패 시 전체 롤백. 부분 성공 허용 안 함
    - 빈 리스트는 정상 (no-op). null은 예외
    - 근거: AI 제안 3건 중 2건만 저장되는 어중간한 상태는 회고 가치가 없음
- **권장 한도 = boolean 노출 + 강제 제한 없음**
    - `RECOMMENDED_AXIS_COUNT_LIMIT = 5`, `RECOMMENDED_TOPIC_COUNT_LIMIT = 10`, `FOCUS_TOP_N = 3`
    - Aggregate가 `isAxisCountExceedsRecommended()` / `isTopicCountExceedsRecommended()` boolean 제공, Application Service가 응답 DTO에 플래그 포함
    - 6번째 축 추가 자체는 허용 — 안내만, 저장 차단 아님

이 결정들은 향후 ADR로 정착한다 (Product DoD §ADR 목록 참조).

## 대안 검토 (Alternatives Considered)

> 큰 갈림길마다 "왜 이것이 아니고 저것인가"를 남긴다. 거부된 안에도 합리적 근거가 있었음을 보임으로써 현재 선택의 트레이드오프를 명확히 한다.

### 커버리지 동기화 방식

**Option A — 이벤트 기반 비동기 갱신 (`@TransactionalEventListener(AFTER_COMMIT)`)**
- 장점: BC 협력 표준 패턴. 자료 연결 트랜잭션과 분리 → 응답 지연 최소화
- 거부 이유:
    - 자료 연결 직후 화면이 갱신된 커버리지를 즉시 보여줘야 하는데 비동기는 race condition 위험
    - Epic 5의 "주제 저장 직후 자료 추가 유도" 인라인 메시지는 새 주제의 `coverageStatus = NO_MATERIAL`이 같은 요청에서 확정되어야 함
    - 야간 배치 안전망이 있어도 "낮 동안 잘못된 커버리지가 보이는" UX 결함 수용 불가

**Option B (선택) — 동일 트랜잭션 내 `CoverageRecalculator` 동기 갱신**
- 비용: TopicMaterial 연결·해제 한 번에 N개 주제 갱신 시 트랜잭션 비용 증가 (현 트래픽에서는 무시 가능)
- 보상: 응답에 정확한 커버리지 즉시 반영. 야간 배치는 안전망으로만 유지

### LearningFacade 개수 정책

**Option A — 유저당 다중 Facade (v2 검토)**
- 장점: "백엔드 + 디자이너" 같은 멀티 트랙 학습자 즉시 지원
- 거부 이유: 회고 단위가 흐려짐 ("어느 방향이 더 진행됐나" 가 매번 컨텍스트). 권장 한도·집중 영역 의미가 약해짐. v1 사용자 사례 검증 전 과한 일반화

**Option B (선택) — v1 유저당 1개, DB UNIQUE 강제**
- 비용: 멀티 트랙 사용자는 컨셉 수정으로 우회해야 함 (수정 시 Layer 2 보존)
- 보상: 도메인 단순. 회고 단위 명확

### `AxisTopic` ↔ `LearningMaterial` 매핑 구조

**Option A — JPA `@ManyToMany` 직접 매핑 (조인 테이블 자동 생성)**
- 거부 이유:
    - 조인 테이블에 audit 컬럼(`linked_at`) 추가 불가능 — 추후 "언제 연결됐나" 추적 봉쇄
    - JPA의 `@ManyToMany`는 매핑 entity에 식별자 부여 불가 → 향후 부가 속성(예: 메모) 추가 시 마이그레이션 비용 큼
    - 매핑 중복 방지를 도메인 레벨에서 단언하기 어려움

**Option B (선택) — `TopicMaterial` 매핑 엔티티 + surrogate `id` + `UNIQUE(topic_id, material_id)`**
- 비용: 엔티티 1개 추가, JPA 매핑이 양방향 `@OneToMany` 두 번으로 풀려 복잡
- 보상: `linked_at` 등 audit 추가 자유. 매핑 중복은 도메인(Application Service 선체크) + DB UNIQUE 이중 방어. 향후 확장 시 마이그레이션 비용 낮음 (ADR001 PK 정책과도 일관)

### 권장 한도(축 5개·주제 10개) 위반 처리

**Option A — 도메인 예외로 저장 차단**
- 장점: 데이터 정합성 강제
- 거부 이유: "5개·10개"는 가이드라인이지 도메인 불변식이 아님. 차단하면 유저 표현 자유 침해. 권장 한도는 운영 중 변경될 가능성 높은 값이므로 도메인 예외 코드로 박제하면 ErrorCode 폭증

**Option B (선택) — 도메인 boolean + 응답 플래그**
- 비용: FE가 플래그를 안내 UX에 반영해야 함
- 보상: 저장 허용 + 권장만 표시. 한도 값 변경은 도메인 상수 1곳 수정으로 끝. ErrorCode 미사용

### `AxisAction`(단일 동사) → `AxisTopic`(명사구) 도메인 재설계

**Option A — `AxisAction` 유지 + `Description.validateSingleVerb()` 제거만**
- 장점: 점진적 변경. 마이그레이션 부담 최소
- 거부 이유: 엔티티 이름·테이블 이름이 더 이상 도메인을 표현하지 않게 됨 ("Action"인데 명사구가 들어감). `revisionCount`·`ActionRevision`도 의미 상실. ADR004에서 거부

**Option B (선택) — `AxisTopic` 엔티티 신규 + 데이터 이관 + 단일 동사 검증 폐지 (ADR004)**
- 비용: Flyway 마이그레이션 + 롤백 스크립트 + 통합 테스트 (Story 2-0)
- 보상: 유저 학습 맵 표현 자유 회복. 커버리지 재계산 트리거가 두 가지로 단순화. 명사구 표현이 v4 도메인 결정으로 정착

### 자료 등록 → Deck 자동 생성 통신 방식

**Option A — `LearningMaterialCommandService`가 `DeckCommandService` 직접 주입 호출**
- 장점: 기존 BC 협력 패턴(직접 주입)과 일관. 결과 통신 자연스러움
- 거부 이유: Epic 6 메모의 "이벤트 기반 단방향" 의도와 어긋남. BC 협력 의도가 코드 레벨에서 덜 명시적

**Option B — `@TransactionalEventListener(AFTER_COMMIT)` 비동기 이벤트 (도메인 이벤트 표준)**
- 거부 이유: 자료 등록 응답이 `409 + DECK_NAME_DUPLICATE` 또는 생성된 `deckId`/`deckName`을 포함해야 함. 비동기는 두 요구 모두 불가능

**Option C (선택) — `@EventListener` 동기 + mutable 이벤트 객체로 결과 통신 (ADR007)**
- 비용: 이벤트가 mutable — 도메인 이벤트 표준에서 벗어남. 비동기 전환 시 결과 통신 메커니즘 재설계 필요
- 보상: 동기 트랜잭션 + 호출자가 이벤트 객체에서 `deckId`/`deckName` 읽어 응답 빌드. Deck BC가 Response DTO를 알 필요 없음

## 전체 아키텍처 (High-Level Architecture)

> LearningFacade BC는 4-level 계층(Facade → Axis → Topic → Material)과 커버리지 동기 갱신 파이프라인, 그리고 Deck BC와의 동기 이벤트 단방향 협력으로 구성된다.

### 4-Level 도메인 계층

```
┌────────────────────────────────────────────────────────────────────┐
│  LearningFacade (Aggregate Root)                                    │
│  ├─ userId (UNIQUE — v1 유저당 1개)                                 │
│  ├─ concept (Layer 1 직업적 컨셉, trim 정규화)                       │
│  └─ List<LearningAxis>  ◄──── unmodifiableList 노출                 │
│       │   addAxis / removeAxis / reorderAxes                        │
│       │   updateAxisName / updateConcept                            │
│       ▼                                                             │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  LearningAxis (Entity)                                    │       │
│  │  ├─ facade (FK)                                           │       │
│  │  ├─ name (UNIQUE within facade)                           │       │
│  │  ├─ displayOrder (1-based, addAxis 자동 부여)             │       │
│  │  └─ List<AxisTopic>  ◄──── unmodifiableList 노출          │       │
│  │       │   addTopic / addTopics(List, 전체 롤백)           │       │
│  │       │   updateTopicName / removeTopic                   │       │
│  │       │   reorderTopics(orderedIds, 집합 검증)            │       │
│  │       ▼                                                   │       │
│  │  ┌────────────────────────────────────────────────┐      │       │
│  │  │  AxisTopic (Entity)                             │      │       │
│  │  │  ├─ axis (FK)                                   │      │       │
│  │  │  ├─ name (UNIQUE within axis)                   │      │       │
│  │  │  ├─ description (nullable, "" → null)           │      │       │
│  │  │  ├─ displayOrder (1-based)                      │      │       │
│  │  │  ├─ coverageStatus (역정규화 컬럼)              │      │       │
│  │  │  └─ revisionCount (Epic 3)                      │      │       │
│  │  └─ ▲ ────────────────────────────────────────────┘      │       │
│  │     │ (다대다)                                              │       │
│  │     │                                                      │       │
│  │  ┌──┴─────────────────────────────┐                       │       │
│  │  │ TopicMaterial (매핑 Entity)     │                       │       │
│  │  │ ├─ id (surrogate, BIGINT)       │                       │       │
│  │  │ ├─ UNIQUE(topic_id, material_id)│                       │       │
│  │  │ └─ linked_at                    │                       │       │
│  │  └─ ▼ ──────────────────────────── ┘                       │       │
│  └─────│ ───────────────────────────────────────────────────┘       │
└────────│ ───────────────────────────────────────────────────────────┘
         ▼
┌────────────────────────────────────────────────────────────────────┐
│  LearningMaterial (Entity, Facade 외부에서도 보존 — Soft Delete)    │
│  ├─ type: BOOK / COURSE / AI_CONVERSATION / WEB_RESOURCE            │
│  ├─ proficiencyLevel: UNRATED / UNFAMILIAR / GETTING_USED / MASTERED│
│  └─ axisId (nullable, Story-005-1, Deck 자동 생성 귀속용)           │
└────────────────────────────────────────────────────────────────────┘
```

### 핵심 플로우

**1. Axis 추가 (displayOrder 자동 부여)**
```
Client ─POST /api/learning-facade/axes─► Controller
                                         │
                                         ▼
                              LearningFacadeCommandService.addAxis(facadeId, name)
                                         │
                                         ▼
                              LearningFacade.addAxis(trimmedName)
                                         ├─ 동일 facade 내 이름 중복 검증 (도메인)
                                         ├─ displayOrder = 현재 max + 1
                                         └─ new LearningAxis(...)
                                         │
                                         ▼
                              facadeRepository.save(facade)
                                         │ (DB UNIQUE(facade_id, name) 이중 방어)
                                         ▼
                              Response { axisId, isAxisCountExceedsRecommended }
```

**2. AxisTopic 다건 추가 (한 건 실패 = 전체 롤백)**
```
Client ─POST /api/learning-facade/axes/{axisId}/topics─► Controller
                                                          │
                                                          ▼
                                  LearningAxis.addTopics(List<TopicCommand>)
                                                          ├─ for each: 이름 검증 / displayOrder 부여
                                                          ├─ 한 건이라도 실패 → 도메인 예외 throw
                                                          └─ 전체 롤백 (@Transactional)
                                                          │
                                                          ▼
                                  Response { topicIds[], isTopicCountExceedsRecommended }
```

**3. Material 등록 + Coverage 동기 갱신 + Deck 자동 생성**
```
Client ─POST /api/learning-facade/materials─► LearningMaterialCommandService.createMaterial
                                              │
                                              ├─ materialRepository.save(material)
                                              ├─ for each linkedTopicId:
                                              │     topicMaterialRepository.save(mapping)
                                              │     CoverageRecalculator.recalculate(topic)
                                              │         └─ axis_topic.coverage_status 갱신 (동일 TX)
                                              │
                                              ├─ ApplicationEventPublisher.publishEvent(
                                              │     new LearningMaterialCreatedEvent(material, linkedTopicIds))
                                              │
                                              │     ◄── [Deck BC] LearningMaterialCreatedEventHandler
                                              │         ├─ Deck UNIQUE(user_id, name) 검증
                                              │         ├─ 동명 충돌 시 → DECK_NAME_DUPLICATE throw → 자료 트랜잭션 롤백
                                              │         └─ deck = deckRepository.save(...)
                                              │         └─ event.setResult(deckId, deckName)  [mutable event]
                                              │
                                              └─ Response { materialId, deckCreated, deckId, deckName }
```

**4. Coverage 재계산 트리거 (ADR004 — 두 가지로 한정)**
```
TopicMaterial 연결/해제 ──┐
                         ├──► CoverageRecalculator.recalculate(topic)
proficiencyLevel 변경  ──┘         │
                                   ▼
                         topic.coverageStatus =
                           materials.isEmpty()                  ? NO_MATERIAL
                           : materials.allMastered()            ? COVERED
                                                                : PARTIAL

(주제 이름·설명 수정은 트리거가 아님 — ADR004)
```

### Out-of-Process 의존

- **MySQL**: `learning_facade` (UNIQUE user_id), `learning_axis` (UNIQUE facade_id + name), `axis_topic` (UNIQUE axis_id + name + 인덱스 coverage_status), `learning_material` (Soft Delete), `topic_material` (UNIQUE topic_id + material_id, surrogate id), `topic_revision`, `revision_reason_option` (DB 관리 선택지)
- **Deck BC** (동기 도메인 이벤트 핸들러): `LearningMaterialCreatedEvent` / `LearningMaterialDeletedEvent` 수신. 단방향 의존 (`Deck → LearningFacade`, ADR007)
- **AI Suggestion BC** (`product-aisuggestion.md`): `AxisSuggestionPort` / `AxisTopicSuggestionPort`. LearningFacade는 결과를 받아 도메인 행위(`addAxis`/`addTopics`)로 흡수만 — Spring AI 직접 import 금지
- **v1.5 갭 인지형 개인화 컨텍스트**: LearningFacade가 `LearningFacadePersonalizationQuery` 인터페이스 제공 (read-only, P95 ≤ 200ms). aisuggestion BC가 소비

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 도메인 실패 시나리오와 응답

| 시나리오 | ErrorCode | HTTP | 클라이언트 권장 동작 |
| --- | --- | --- | --- |
| LearningFacade 미존재 (facadeId 잘못됨) | `LEARNING_FACADE_NOT_FOUND` | 404 | 홈으로 리디렉션 |
| Concept 미설정 상태에서 축 추가 시도 | `LF_CONCEPT_REQUIRED` | 400 | Layer 1 입력 화면 유도 |
| Concept 입력값이 blank | `LF_CONCEPT_REQUIRED` | 400 | 입력 검증 메시지 |
| 동일 facade 내 축 이름 중복 | `LEARNING_AXIS_DUPLICATE_NAME` | 409 | "이미 존재합니다" + 기존 축 강조 |
| 축 이름 blank | `LEARNING_AXIS_NAME_BLANK` | 400 | 입력 검증 메시지 |
| 축 미존재 (axisId 잘못됨) | `LEARNING_AXIS_NOT_FOUND` | 404 | 축 목록 새로고침 |
| 동일 축 내 주제 이름 중복 | `AXIS_TOPIC_DUPLICATE_NAME` | 409 | "이미 존재합니다" |
| 주제 이름 blank | `AXIS_TOPIC_NAME_BLANK` | 400 | 입력 검증 메시지 |
| 주제 미존재 | `AXIS_TOPIC_NOT_FOUND` | 404 | 주제 목록 새로고침 |
| reorder 시 id 집합 불일치 (유령 id / 누락 id) | (도메인 예외 → 400) | 400 | 현재 목록 재조회 후 재시도 |
| 다건 주제 추가 중 1건 실패 | 해당 검증 ErrorCode | 400 / 409 | **전체 롤백** — 부분 성공 없음. FE 재시도 시 한 건씩 진단 |
| 비활성/미존재 RevisionReasonOption id | `REVISION_REASON_NOT_FOUND` | 404 | 선택지 목록 재조회 |
| Deck 자동 생성 시 동명 Deck 존재 | `DECK_NAME_DUPLICATE` | 409 | 자료 저장 자체도 롤백 → 확인 팝업 (Story 6-1) |
| TopicMaterial 매핑 중복 추가 | (도메인 예외 → 409) | 409 | "이미 연결된 자료" 안내 |

### 권장 한도 안내 (예외 아님 — 응답 플래그)

| 조건 | 응답 플래그 | UI 동작 |
| --- | --- | --- |
| 축 6개 이상 | `isAxisCountExceedsRecommended: true` | 안내 메시지, 저장 허용 |
| 주제 11개 이상 (축당) | `isTopicCountExceedsRecommended: true` | 안내 메시지, 저장 허용 |
| 상위 `FOCUS_TOP_N = 3` 주제 | `isFocused: true` (각 topic) | "지금 집중 중" 뱃지 |
| 주제 `revisionCount >= 3` | `isRefinementSuggested: true` | "이 주제가 아직 단련 중이에요" 안내 |

### 무결성 안전망 (DB 제약 — 도메인 검증 누락 시 마지막 보루)

- `UNIQUE(user_id) on learning_facade` — 유저당 1개 강제
- `UNIQUE(facade_id, name) on learning_axis` — 축 이름 중복 차단
- `UNIQUE(axis_id, name) on axis_topic` — 주제 이름 중복 차단
- `UNIQUE(topic_id, material_id) on topic_material` — 매핑 중복 차단
- `CHECK(display_order >= 0) on (learning_axis, axis_topic)` — 도메인은 1 이상만 부여하지만 안전망
- `CHECK(coverage_status IN ('NO_MATERIAL','PARTIAL','COVERED'))` (ADR002)
- `CHECK(type IN ('BOOK','COURSE','AI_CONVERSATION','WEB_RESOURCE'))` on `learning_material`

### 로깅 정책

- **항상 기록**: ErrorCode 응답 시 `facadeId + axisId/topicId + ErrorCode`
- **WARN**: 다건 추가 중 부분 실패 (전체 롤백된 입력 건수 + 실패 사유)
- **debug**: CoverageRecalculator 진입 시 갱신 전/후 status (info로 두면 자료 등록당 N로그 폭주)
- **절대 금지**: 유저 입력 컨셉 원문 (PII 우려 — 직업 방향은 민감 정보일 수 있음)

### 관측 지표 (v2 — Metrics 도입 시 합류)

- `learning_facade_axis_count{facade_id}` — 게이지 (권장 한도 초과 분포 추적)
- `learning_facade_coverage_recalc_total{trigger=link|unlink|proficiency}` — 카운터
- `learning_facade_coverage_recalc_latency_seconds` — 히스토그램 (역정규화 갱신 비용)
- `learning_facade_topic_revision_total{reason_label}` — 카운터 (어떤 이유로 주제가 자주 단련되는지)
- `learning_facade_personalization_query_latency_seconds{query=gaps|weak|axisCard}` — P95 ≤ 200ms 임계 모니터링 (v1.5)

## 롤아웃 / 마이그레이션 (Rollout)

### 전제 — 프로덕션 트래픽 없음

현재 사용자 0명. 따라서 `AxisAction → AxisTopic` 같은 도메인 재설계도 일괄 마이그레이션으로 처리한다. 트래픽 발생 후 동일 변경이 필요할 경우 다음 패턴을 재사용:
- 1주차: 신규 엔티티 추가 + 기존 엔티티 dual write
- 2주차: 읽기 경로 전환
- 3주차: 기존 엔티티 RENAME 후 아카이브 (DROP 보류)

### Epic 의존성 그래프

```
Epic 2 (Layer 2 축/주제 — Story 2-0 마이그레이션 선행)
  Story 2-0 (AxisAction → AxisTopic 마이그레이션) ──┐
       │                                              │
       ▼                                              │
  Story 2-1 (축 추가/관리) ─┐                         │
                            │                         │
                            ▼                         │
  Story 2-2 (주제 추가/관리) ─► Story 2-3 (Focus Top N) │
       │                                              │
       │                                              ▼
       │            Epic 1 (Layer 1 컨셉) ── 독립
       │              Story 1-1 → Story 1-2
       │
       ▼
  Epic 3 (주제 단련)
    Story 3-1 (이름 수정 + 커버리지 재초기화) → 3-2 (이력 + RevisionReasonOption) → 3-3 (단련 안내)
       │
       ▼
  Epic 4 (학습 자료 + 4종 타입)
    Story 4-1 (자료 등록 + Topic 연결) → 4-2 (숙련도 슬라이더) → 4-3 (정적/동적 비중)
       │
       ▼
  Epic 5 (커버리지 공백 시각화)
    Story 5-1 → 5-2 (주제 추가 시 자료 추가 유도)
       │
       ▼
  Epic 6 (LearningFacade ↔ Deck 이벤트 연동 — ADR007)
    Story 6-1 (Deck 자동 생성) → 6-2 (축 카드 Deck 목록)
```

### Flyway 마이그레이션 단계

1. **V{n} — `axis_topic` / `topic_material` 신규** (Epic 2 Story 2-0 선행)
    - 기존 `axis_action` → `axis_topic` 데이터 1:1 이관 (description → name)
    - `action_material` → `topic_material` RENAME
    - `action_revision` → `axis_action_description_history` 아카이브 (FK 제거)
    - 롤백 스크립트 동반 (단, `axis_action.description` 30자 → `axis_topic.name` 100자 확장이라 롤백 시 `SUBSTRING` + 수동 확인 절차)
2. **V{n+1} — `topic_revision` / `revision_reason_option` seed** (Epic 3)
    - `revision_reason_option` 초기 4개 seed ("기존 표현이 너무 좁았다" 등)
3. **V{n+2} — `learning_material.type` 컬럼 + `proficiency_level` 컬럼** (Epic 4)
    - 기존 `top_down` / `bottom_up` 값 → `BOOK` / `WEB_RESOURCE` 기본값 이관 후 유저 수정 유도
    - 컬럼 추가 → 백필 → NOT NULL 3단계 분리 (db-conventions §3.8)
4. **V{n+3} — `axis_topic.coverage_status` 인덱스** (Epic 5 / v1.5 Personalization Query 인덱스 활용)

### 환경별 설정

- `application-local.yml` / `application-dev.yml`: H2 호환 모드, Flyway baseline
- `application-prod.yml`: MySQL 8.0, Flyway 자동 실행
- 도메인 상수 (`RECOMMENDED_AXIS_COUNT_LIMIT=5` 등)는 외부 설정 미노출 — 변경은 코드 + ADR 동반

### 롤백 계획

각 Story는 단일 PR + 단일 Flyway 마이그레이션. Story 2-0 같이 데이터 이관이 동반된 마이그레이션은 롤백 스크립트(`R{n}__rollback_*.sql`)를 함께 작성. Epic 6의 `@EventListener` 도입은 코드 revert만으로 롤백 가능 (스키마 영향 없음).

## 성공 지표 (측정 가능)

| 지표 | 목표 값 | 측정 방법 |
| --- | --- | --- |
| LearningFacade 1개 = 유저 1명 (v1 단일 컨셉) | UNIQUE(user_id) 위반 = 0건 | DB 제약 + 통합 테스트 |
| 컨셉 미설정 LearningFacade에서 축 추가 시도 | `LF_CONCEPT_REQUIRED` 응답 = 100% | Controller Slice |
| `displayOrder` 도메인 외부 주입 | = 0건 | 정적 분석 + 도메인 Setter 미노출 검증 |
| 동일 LearningFacade 내 축 이름 중복 | = 0건 (도메인 + DB UNIQUE 이중 방어) | 통합 테스트 |
| 동일 축 내 AxisTopic 이름 중복 | = 0건 | 통합 테스트 |
| 자료 4종 타입(`BOOK`/`COURSE`/`AI_CONVERSATION`/`WEB_RESOURCE`) 분류 누락 | = 0건 | DB CHECK + 통합 |
| 학습 자료 등록 시 동명 Deck 자동 생성 누락 | = 0건 | 통합 테스트 (이벤트 → Deck 생성) |
| 주제 수정 횟수 임계(3회) 초과 시 단련 안내 누락 | = 0건 | 단위 테스트 |
| TopicMaterial 다대다 매핑 중복 | = 0건 (UNIQUE(topic_id, material_id)) | Repository Slice |

## Scope

- **In Scope**
  - LearningFacade Aggregate Root (단일 유저 + 단일 컨셉)
  - Layer 1 직업적 컨셉 입력·수정 (`updateConcept` 정규화 + `ConceptChangeRecord`)
  - Layer 2 축(`LearningAxis`) 추가·수정·삭제·재배치 (`displayOrder` 1-based 자동 부여)
  - 하위 주제(`AxisTopic`) 추가·수정·삭제·재배치 (단건/다건)
  - AxisTopic 단련 이력 (`TopicRevision` + `RevisionReasonOption` DB 관리)
  - 학습 자료(`LearningMaterial`) — 4종 타입 (`BOOK` / `COURSE` / `AI_CONVERSATION` / `WEB_RESOURCE`)
  - TopicMaterial 다대다 연결 + `CoverageStatus` (`NO_MATERIAL` / `PARTIAL` / `COVERED`) 역정규화
  - 숙련도(`proficiencyLevel`) 자가 평가 슬라이더
  - 커버리지 공백 시각화 + 자료 추가 유도
  - 자료 등록 → 동명 Deck 자동 생성 (Card BC 이벤트 연동)
  - 권장 한도 안내 — 축 5개 초과 / 주제 10개 초과 / 집중 영역 상위 3개

- **Out of Scope** (※ AI 기반 항목은 `product-aisuggestion.md`의 Spring AI 인프라 — `spring-ai-starter-model-vertex-ai-gemini` + `ChatClient`/`PromptTemplate`/`BeanOutputConverter`/`SuggestionPort` 패턴 — 을 그대로 재사용한다. LearningFacade 도메인은 Spring AI에 직접 의존하지 않고 자체 Port를 정의해 `infrastructure/.../gemini/` Adapter로 흡수)
  - 복수 Layer 1 컨셉 동시 관리 — v2 (현재는 유저당 단일 컨셉)
  - **AI 제안 (Axis / AxisTopic)** — v1은 `product-aisuggestion.md` Product 범위에서 별도 제공 (Spring AI `ChatClient` + Vertex AI Gemini). LearningFacade는 제안 결과를 받아 도메인 행위(`addAxis`/`addTopics`)로 흡수만 담당
  - **Concept(직업적 컨셉) AI 제안** — v1은 하드코딩 제안 목록. v2 도입 시 Spring AI `ChatClient` + Card 측 패턴 재사용
  - **갭 인지형 개인화 컨텍스트 자체** (`PersonalizationContextPort` 정의 및 LLM 통합) — `product-aisuggestion.md` Epic 6 (v1.5) 범위. LearningFacade는 컨텍스트의 **데이터 원천**(Read Model로 커버리지 공백·낯섦 자료 노출)만 책임지고, Port 정의·LLM 프롬프트 통합·메트릭 태깅에는 관여하지 않음
  - 행동 간 의존 관계 (선행 행동 개념) — v2
  - 주제 간 의존 관계 — v2
  - **자료 자동 크롤링 / 썸네일 / URL → 메타데이터 자동 추출** — v2. Spring AI `ChatClient`로 페이지 요약·메타데이터 추출 도입 가능. `MaterialMetadataExtractionPort` 신설 + Adapter는 `product-aisuggestion.md` 인프라 재사용
  - **외부 기술 트렌드 감지 기반 자료 추천 (Roadmap Drift)** — v2. Spring AI + 외부 검색 API 결합
  - **AxisTopic 의미 기반 중복 감지·자동 그룹핑** — v2. Spring AI 임베딩 API 활용 (v1은 정확 문자열 비교만)
  - **숙련도 자동 추정 (학습 이력·자료 진행도 기반)** — v2. 통계 우선, 필요 시 Spring AI 보조
  - **주제 단련 이유 자동 추천** — v2. Spring AI `ChatClient`로 이전→이후 이름 차이 분석해 가장 가까운 `revision_reason_option` 추천. v1은 유저 직접 선택만
  - 삭제된 주제 복원 — v2 (AI 무관)
  - 학습 자료 파일 첨부 (URL만 지원) — v2
  - 주제 템플릿 복사 (타 유저 구조 참고) — Library Product
  - 자료 완독 후 자동 Card화 유도 — v2
  - 행동 단위 Deck 연결 (현재 축 단위) — v2
  - LearningFacade 밖에서 독립 Deck 생성 — v2 논의

## Epic 목록

- [ ] Epic 1. Layer 1 직업적 컨셉 설정
- [ ] Epic 2. Layer 2 축(`LearningAxis`) 및 하위 주제(`AxisTopic`) 구성
- [ ] Epic 3. AxisTopic 단련 — 수정 이력 및 단련 안내
- [ ] Epic 4. 학습 자료 연결 및 커버리지 — 4종 타입 확장
- [ ] Epic 5. 커버리지 점검 및 공백 알림
- [ ] Epic 6. LearningFacade ↔ Deck 자동 연결

## 제품 수준 완료 기준 (DoD)

- [ ] 6개 Epic이 모두 Done 상태다
- [ ] LearningFacade Aggregate의 캡슐화·정적 팩토리·VO/Record 패턴이 일관 적용된다 (`updateConcept`, `addAxis`, `LearningAxis.addTopics`, `reorderAxes`, `reorderTopics`)
- [ ] `displayOrder` 외부 주입 금지 — 도메인 행위만으로 결정 (단위 테스트 + 정적 분석)
- [ ] `coverageStatus` 자동 갱신 — 동일 트랜잭션 내 동기 (이벤트 비동기 불허)
- [ ] OSIV=false, READ_COMMITTED 등 프로젝트 원칙 준수
- [ ] OpenAPI 스펙(`/swagger-ui.html`)에 LearningFacade / Axis / Topic / Material 엔드포인트 반영
- [ ] Flyway 마이그레이션 (이전 `AxisAction` 도메인 → `AxisTopic` 데이터 이관 포함)
- [ ] ADR 작성
  - ADR: "AxisAction(단일 동사) → AxisTopic(명사구) 도메인 재설계 사유"
  - ADR: "TopicMaterial 매핑 — 다대다 + surrogate id + UNIQUE(topic_id, material_id)"
  - ADR: "coverageStatus 역정규화 — 동기 갱신, 야간 배치 안전망"
  - ADR: "AxisTopic 단련 이유 선택지 — DB 관리(`revision_reason_option`) + 이력 레이블 스냅샷"
  - ADR: "LearningMaterial 4종 타입 분류 + Deck 자동 생성 이벤트 연동"
  - ADR: "권장 한도 — Aggregate boolean 노출, 강제 제한 없음"
- [ ] API 스펙 갱신 (`docs/api/` 또는 Swagger Schema) — 요청/응답 DTO, ErrorCode 표

## 대상 사용자

- 주요 사용자: 직업적 방향을 정의하고 학습 지도를 구성·단련하는 Third Tool 유저
- 사용 맥락
  - 가입 직후 온보딩 (Layer 1 컨셉 설정)
  - 축·주제 구성 시 (방향 막막함을 구조로 해소)
  - 자료 등록·연결 시 (커버리지 공백 인식)
  - 주제 단련 시 (수정·재배치·이유 기록)

## 명세 변경 이력 (Spec Drift Log)

> done 상태 Product의 결정이 후속 fix로 부분 폐기될 때 in-place 수정 대신 본 블록에 누적 기록한다.

- **fix-deck-axis-visibility (0.0.2v) — 2026-06-30** (`workflow/task/fix/sdd/version/0.0.2v/fix-deck-axis-visibility.md`, [ADR020](../../../../../../../../docs/adr/ADR020-deck-axis-visibility.md))
  - **폐기 §3.1 — "Deck↔Axis는 raw `Long` 컬럼 1개로만 표현 / 응답 미노출"**: read-model 노출로 **한정** 폐기. `DeckResponse.Summary`/`Detail`에 `axisId`/`axisName` 노출(`axisName`은 `LearningFacadeQueryService.findAxisNamesByIds` 배치 보강). **도메인 연관 승격(Option B)은 거부** — `deck.axisId`는 raw `Long` 유지, 4-level 응집 보존(ADR020).
  - **폐기 §3.2 — "Deck 자동 생성을 통한 axis 결합 일원화"**: 사용자가 축에 명시적으로 덱을 추가하는 경로 부재를 해소. `POST /api/v1/learning-facade/axes/{axisId}/decks` + `Deck.createUnderAxis` 신설. 자동 생성 1개(`createFromAxis`, 멱등) + 명시 추가 N개 + 고아 덱(`Deck.of`) 보존 정책으로 전환. 일반 `POST /api/v1/decks`(고아 덱)는 보존.
  - 신규 BC 의존 엣지 `LearningFacade → Card`(Application 경유 read, 축 카드 조회) 추가 — `docs/PACKAGE.md` §6 반영.
- **fix-axis-deck-full-integration (0.0.2v) — 2026-07-01** (`workflow/task/fix/sdd/version/0.0.2v/fix-axis-deck-full-integration.md`, [ADR021](../../../../../../../../docs/adr/ADR021-axis-deck-full-integration.md))
  - **폐기 — "LearningAxis Hard Delete만 지원"** (`learning_axis` 스키마 V2의 암묵 결정): 사용자 지시 "카드 만들 때 축 인식 실패, 화면 이탈 시 사라짐"의 근본 원인이 `LearningFacade.removeAxis()`의 orphanRemoval hard delete로 확인됨. LearningAxis도 Card·Deck·LearningFacade와 동일한 Soft Delete 정책으로 승격(`deleted_at DATETIME(6) NULL`, `@SQLRestriction("deleted_at IS NULL")`, Flyway V14). `axes` OneToMany는 `orphanRemoval=false`로 낮춤(회귀 트랩 차단). `getAxes/findAxis/addAxis/validateAxisNameDuplicate/reorderAxes/isAxisCountExceedsRecommended/getCoverageSummary/hasUncoveredTopics` 8개 지점이 활성 축만 필터.
  - **폐기 — 선행 fix §4.2가 신설한 "축 스코프 Deck 사용자 명시 생성 경로"**: `POST /learning-facade/axes/{axisId}/decks` + `LearningFacadeCommandService.createDeckUnderAxis` + `Deck.createUnderAxis` 팩토리를 축=덱 1:1 정책으로 재정의하며 폐기. Deck 생성은 `LearningAxisCreatedEventHandler → Deck.createFromAxis` 유일 진입점.
  - **확정 — Axis 삭제 시 Deck 연쇄 소프트 삭제 조율**: `LearningFacadeCommandService.removeAxis`가 `facade.removeAxis(axisId) → facadeRepository.save(facade) → deckCommandService.softDeleteByAxisId(axisId)` 순서로 조율. 동일 `@Transactional` 원자성, Axis flush 후 Deck 연쇄로 관측 순서와 코드 순서 일치. `docs/PACKAGE.md` §6에 `LearningFacade → Deck` Application 호출 경로로 반영.
  - **확정 — `learning_axis` UNIQUE 재정의**: `(facade_id, name)` → `(facade_id, name, deleted_at)` 3-column composite (Flyway V14). MySQL이 NULL 조합을 unique 검사에서 서로 다른 값으로 취급하므로 활성 축끼리만 유일 보장, 소프트 삭제된 축 이름 재사용 가능.

## 관련 문서

- 상위 도메인 문서: `docs/DOMAIN.md` § LearningFacade
- 연관 Product
  - `product-aisuggestion.md` — **Spring AI 1.0(`spring-ai-starter-model-vertex-ai-gemini` + `ChatClient` + `BeanOutputConverter`) 공통 인프라의 정본**이자 LearningFacade가 주 소비자인 AI 제안 보조 Product. LearningFacade가 v1 Axis/AxisTopic 제안과 v2 추가 AI 기능(메타데이터 추출·의미 기반 중복 감지·단련 이유 추천 등)을 도입할 때 동일 starter, 동일 `PromptTemplate`/`BeanOutputConverter` 패턴, 동일 에러 체계(`LF_SUGGEST_*`), 동일 Static Fallback, 동일 ADR(Spring AI 채택 / Structured Output / Fallback 정책)을 그대로 따른다 (재발명 금지)
    - **v1.5 (갭 인지형 개인화 컨텍스트, `product-aisuggestion.md` Epic 6)**: LearningFacade는 컨텍스트의 **데이터 원천** — Epic 5의 커버리지 공백(`NO_MATERIAL` / `PARTIAL`)과 Epic 4의 낯섦 숙련도 자료를 Read Model로 노출. `PersonalizationContextPort` 정의·LLM 통합·메트릭 태깅은 `product-aisuggestion.md` Epic 6 책임. LearningFacade 도메인은 Spring AI / Port를 직접 import 금지
  - `product-card.md` — ReviewSession이 Layer 1 기준 카드 수집을 위해 LearningFacade를 의존
- 연관 ADR: ADR001 (PK), ADR002 (Enum), ADR003 (Soft Delete), ADR005 (Command/Query record)
- v2 AI 도입 시 참고 ADR: `product-aisuggestion.md` Product DoD의 "Spring AI 1.0 + Vertex AI Gemini 채택", "Structured Output — BeanOutputConverter", "프롬프트 템플릿 — StringTemplate(`{var}`)", "Fallback 정책"

## 열린 질문 (Open Questions)

> 본 Product를 통과시키되 v2 또는 별도 Story에서 답을 내야 하는 미결 항목.

- **Q1. 컨셉 변경 폭이 "큰지" 판단 기준** — Story 1-2는 "컨셉 변경 폭이 크면 축 재검토를 권장합니다"라고 명시하지만 "큰 변경"의 정의가 없다. v1은 FE가 자체 판단 (예: 글자 차이 N% 이상) 또는 항상 안내. v2에서 Spring AI 의미 유사도로 정량화 검토. ADR 필요 시점: FE가 자체 기준을 도입한 직후.
- **Q2. AxisTopic 의미 기반 중복 감지** — 현재 v1은 정확 문자열 비교만 (UNIQUE 제약). "캐릭터 설계" vs "인물 설계" 같은 의미 중복은 허용된다. v2 Spring AI 임베딩 도입 시 (1) 저장 차단할지 (2) 경고만 띄울지 미결. 권장 한도 정책(Option B)과 일관성 유지 필요.
- **Q3. 다중 LearningFacade(멀티 트랙) 도입 시 회고 단위** — v1은 유저당 1개로 회고 단위 명확. 다중 도입 시 "회고는 facade 단위" vs "user 단위 합산" vs "facade별 + 통합" 셋 중 선택 필요. 멀티 트랙 사용자 사례가 누적된 후 결정.
- **Q4. 자료 삭제 시 Deck 보존 정책의 장기 운영 영향** — Epic 6은 자료 삭제 후 Deck을 '자료 미연결' 뱃지로 보존한다. 6개월/1년 뒤 미연결 Deck이 화면 노이즈가 될지, 회고 가치로 정착할지 모니터링 필요. v2 검토 항목: "자료 미연결 6개월 경과 Deck 자동 아카이브".
- **Q5. v1.5 `LearningFacadePersonalizationQuery` P95 ≤ 200ms 보장 임계 트래픽** — 현재 트래픽 0명에서는 자명하게 충족하나, 동시 사용자 N명 / 평균 facade의 axis·topic 개수 M 시점에서 인덱스만으로 충분한지 미검증. v2 부하 테스트 + 필요 시 Read Model 캐시 도입 검토.
- **Q6. `revision_reason_option` 운영 화면 부재** — 현재 v1은 DB 직접 수정으로 선택지 변경. Admin BC 구현 전까지 운영자 부담. ADR 필요 시점: 운영자가 변경 빈도 월 1회 이상으로 증가한 시점.

---

# [Epic 1] Layer 1 직업적 컨셉 설정

## 목표

> 유저가 "나는 무엇을 하고 싶은 사람인가?"를 시스템 안에서 명확히 정의하고, 이 설정이 이후 모든 축·주제·학습 자료 구성의 출발점이 된다.

## 배경

- LearningFacade의 모든 구조는 Layer 1에서 출발한다
- 직업적 컨셉이 설정되지 않으면 Layer 2 축 구성이 시작될 수 없다
- "정체성 선택"이 아닌 "하고 싶은 행동의 방향" 설정임을 유저가 이해해야 한다

## 포함 Story

- [ ] Story 1-1. 직업적 컨셉 최초 설정
- [ ] Story 1-2. 직업적 컨셉 수정

## Epic 인수 시나리오

`신규 유저가 LearningFacade 최초 진입` → `온보딩 화면 (직접 입력 + 제안 목록)` → `유저가 "백엔드 개발자" 또는 "사람의 이야기를 설계하는 사람" 입력` → `LearningFacade.updateConcept(trimmedValue)` → `LearningFacade 홈에 컨셉 표시` → `이후 컨셉 수정 시 기존 Layer 2 구조는 보존됨 + "축 재검토를 권장합니다" 안내` → `동일 값 저장 시 unchanged 반환, updatedAt 불변`

## Epic 완료 기준 (DoD)

- [ ] 유저가 직업적 컨셉을 직접 입력하거나 제안 목록에서 선택할 수 있다
- [ ] 설정된 컨셉이 LearningFacade 홈에 표시된다
- [ ] 컨셉 수정 및 삭제가 가능하다
- [ ] `LearningFacade.updateConcept()`가 trim 정규화 + `ConceptChangeRecord` 반환을 보장한다
- [ ] ADR 갱신: "Layer 1 컨셉 — 단일 컨셉 정책, 수정 시 Layer 2 보존"
- [ ] 연결된 Story 모두 Done

## 내부 메모 / 제약 사항

- 기술 제약: 초기 제안 목록은 하드코딩 (AI 제안은 `product-aisuggestion.md`에서 별도)
- 기획 원칙: "직업" 선택이 아니라 "방향" 설정임을 UI 문구에 명확히 전달
- 연기된 항목: 여러 Layer 1 컨셉 동시 관리 — v2

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **컨셉 수정 시 Layer 2 자식 트리 처리**
    - 선택: **보존** — 컨셉이 바뀌어도 기존 축·주제·자료는 그대로 유지
    - 거부 — 자식 트리 강제 초기화: "새 컨셉에는 새 축이 어울린다" 가설은 가능하나 유저 학습 자산을 손실시키는 결정. 대신 "축 재검토를 권장합니다" 안내(Story 1-2)로 유저 판단에 위임
- **`updateConcept()` 동일 값 입력 시 동작**
    - 선택: **`ConceptChangeRecord.unchanged()` 반환 + 저장 쿼리 미발생 + `updatedAt` 불변**
    - 거부 — 항상 저장: trim 정규화 후 동일 값이면 의미 있는 변경이 아님. unnecessary `updatedAt` 갱신은 회고 노이즈 (`conventions.md` §1.1)
- **Concept 입력 정규화 위치**
    - 선택: **`LearningFacade.updateConcept(input)` 도메인 메서드 내부에서 trim**
    - 거부 — Application Service에서 trim: 도메인 메서드는 항상 정규화된 상태를 보장해야 함 (conventions §1.1). Application Service에서 trim하면 도메인 메서드를 다른 진입점에서 호출 시 정규화가 누락될 위험
- **Concept AI 제안 v1 처리**
    - 선택: **하드코딩 제안 목록** + v2에 Spring AI 도입 (`product-aisuggestion.md` 인프라 재사용)
    - 거부 — v1부터 Spring AI: AI 의존성 도입은 별도 Product 범위. Concept은 입력 빈도가 매우 낮아 (유저당 1회) ROI 낮음

---

## [Story 1-1] 직업적 컨셉 최초 설정

### 사용자 가치

> As a **신규 유저**,
> I want **내가 되고 싶은 방향(직업적 컨셉)을 처음 설정하길**,
> So that **이후 세부 축과 학습 자료가 이 방향을 기준으로 구성될 수 있다.**

### 설명

- 가입 직후 또는 LearningFacade 최초 진입 시 온보딩 플로우로 진입
- 직접 입력(자유 텍스트) 또는 제안 목록 선택 두 가지 방식
- "백엔드 개발자", "UX 디자이너"처럼 직업명이 아니어도 됨. "사람의 이야기를 설계하는 사람"처럼 방향 표현 허용
- 도메인 행위: `LearningFacade.updateConcept(String input)` → trim 정규화 + 빈 문자열 거부 + `ConceptChangeRecord` 반환
- ErrorCode: `LF_CONCEPT_REQUIRED` (입력 blank)

### 인수 조건 (Given/When/Then)

- [ ] Given 신규 유저가 LearningFacade에 처음 진입, When 온보딩 화면 표시, Then 직업적 컨셉 입력창 + 제안 목록 함께 표시
- [ ] Given 유저가 제안 목록 항목 선택, When 선택, Then 입력창에 해당 텍스트 자동 입력
- [ ] Given 유저가 직접 텍스트 입력 후 저장, When 저장, Then LearningFacade 홈에 설정된 컨셉 표시
- [ ] Given trim 후 동일 값으로 저장, When 호출, Then `ConceptChangeRecord.unchanged()` 반환, `updatedAt` 불변, 저장 쿼리 미발생
- [ ] *(엣지 케이스 — blank)* 입력값이 공백이거나 빈 문자열일 때 `LF_CONCEPT_REQUIRED` 예외 + 안내 메시지
- [ ] *(엣지 케이스 — trim 정규화)* `"  백엔드 개발자  "` 입력 시 `"백엔드 개발자"`로 저장

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: `updateConcept()` 정상 / blank 거부 / 동일 값 unchanged / trim 정규화
- [ ] `ConceptChangeRecord` VO 단위 테스트 (`changed()` / `unchanged()`)
- [ ] Controller Slice: `LF_CONCEPT_REQUIRED` 응답 형식 검증
- [ ] 디자인 QA 통과 (FE: 온보딩 플로우)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 비목표

- AI 기반 컨셉 제안 — v1은 하드코딩, AI는 `product-aisuggestion.md` (Concept은 명시적 비목표)
- 복수 컨셉 — v2

### INVEST 점검

- **Independent**: LearningFacade Aggregate의 컨셉 필드만
- **Negotiable**: 제안 목록 항목은 PO 합의
- **Valuable**: Product 전체의 출발점
- **Estimable**: 도메인 행위 + 정규화 + UI
- **Small**: 5 SP
- **Testable**: 도메인 + Slice + 화면

### 참고

- 관련 API 스펙: `POST /api/learning-facade/concept`

---

## [Story 1-2] 직업적 컨셉 수정

### 사용자 가치

> As a **기존 유저**,
> I want **내 직업적 컨셉을 언제든지 수정할 수 있길**,
> So that **기술 변화나 방향 전환이 생겼을 때 로드맵 전체를 처음부터 다시 만들지 않아도 된다.**

### 설명

- LearningFacade 홈에서 컨셉 수정 진입 가능
- 수정 시 기존 Layer 2 축·주제·자료는 유지 (삭제되지 않음)
- 컨셉 변경 폭이 크면 "축 재검토를 권장합니다" 안내 (강제 X)
- 도메인 행위: 1-1과 동일한 `updateConcept()` 재사용

### 인수 조건 (Given/When/Then)

- [ ] Given LearningFacade 홈 컨셉 수정 버튼, When 수정 화면 열림, Then 기존 컨셉 텍스트가 입력창에 채워져 있음
- [ ] Given 유저가 컨셉 수정 후 저장, When 저장, Then 홈 컨셉이 새 값으로 업데이트
- [ ] Given 컨셉 변경, When 저장, Then 기존 Layer 2 축·주제·자료는 그대로 유지
- [ ] Given 컨셉을 기존 값과 동일하게 저장, When 호출, Then `unchanged` 반환 / 변경 없음 처리
- [ ] *(엣지 케이스 — 큰 변경 안내)* 컨셉 변경 폭이 크면 "축 재검토를 권장합니다" 안내 (UI 측 판단 가능)

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: 수정 시 자식 컬렉션 비파괴 확인
- [ ] 통합 테스트: 컨셉 변경 후 축·주제·자료 보존
- [ ] 디자인 QA 통과 (FE: 수정 화면 + 권장 안내)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 3 SP

### 비목표

- 컨셉 변경 이력 — v2

### INVEST 점검

- **Independent**: 1-1 도메인 재사용
- **Negotiable**: 권장 안내 임계는 UX 합의
- **Valuable**: 방향 전환 흡수
- **Estimable**: UI 진입점만 추가
- **Small**: 3 SP
- **Testable**: 비파괴성 통합 테스트

---

# [Epic 2] Layer 2 축(LearningAxis) 및 하위 주제(AxisTopic) 구성

## 목표

> 유저가 직업적 컨셉 아래에 세부 축을 구성하고, 각 축 아래에 하위 주제(AxisTopic)를 동적으로 추가·삭제·재배치할 수 있다. 이 주제 목록이 이후 학습 자료 커버리지 판단의 기준이 된다.

## 배경

- 이전 버전은 축 아래에 "단일 동사 행동"을 두었다 (예: 데이터 모델링 → "설계하다", "구현하다")
- 단일 동사 구조는 도메인 규칙 강제가 과했고, 유저가 "이 축에서 뭘 배우고 싶은가"를 표현하기 어려웠다
- 새 구조는 "하위 주제(AxisTopic)"로 변경 — 짧은 명사구 제목 + 선택적 설명으로 자유롭게 표현
- 주제는 책 목차처럼 고정되지 않는다. 유저가 AI 제안을 받거나 직접 입력해 언제든 맵을 다시 그린다

## 포함 Story

- [ ] Story 2-0. AxisAction → AxisTopic 도메인 마이그레이션
- [ ] Story 2-1. 세부 축 추가 및 관리 (단건 + 재배치)
- [ ] Story 2-2. 축 하위 주제(AxisTopic) 추가 및 관리 (단건 + 다건)
- [ ] Story 2-3. 주제 우선순위 관리 및 집중 영역 표시 (`FOCUS_TOP_N = 3`)

## Epic 인수 시나리오

`기존 AxisAction 데이터 → AxisTopic으로 1:1 마이그레이션 (Flyway)` → `유저가 축 추가 ("데이터 모델링")` → `LearningFacade.addAxis(name) → displayOrder=1 자동 부여` → `유저가 두 번째 축 추가 → displayOrder=2` → `유저가 축 1 하위 AxisTopic 다건 추가 ("도메인 설계", "정규화")` → `LearningAxis.addTopics(List<TopicCommand>) → 한 건 실패 시 전체 롤백` → `유저가 드래그로 주제 재배치 → reorderTopics(orderedIds)` → `id 집합 불일치면 예외` → `상위 3개 주제는 "지금 집중 중" 뱃지`

## Epic 완료 기준 (DoD)

- [ ] 유저가 축을 추가·수정·삭제·재배치할 수 있다
- [ ] 각 축 아래에 AxisTopic을 추가·수정·삭제·재배치할 수 있다
- [ ] AxisTopic은 이름(필수) + 설명(선택) + 순서로 구성된다
- [ ] 주제 목록이 커버리지 점검의 기준으로 연결된다 (Epic 4)
- [ ] `displayOrder`가 1-based 자동 부여되고 외부 주입 금지가 강제된다
- [ ] 동일 LearningFacade 내 축 이름 / 동일 축 내 주제 이름 중복 금지 (도메인 + DB UNIQUE 이중 방어)
- [ ] ADR 갱신: "AxisTopic 도메인 재설계 — 마이그레이션 절차"
- [ ] 연결된 Story 모두 Done

## 내부 메모 / 제약 사항

- 기술 제약: 기존 `AxisAction` 엔티티는 `AxisTopic`으로 완전 교체. 데이터 마이그레이션 (Story 2-0)
- 기획 원칙: 주제 이름에 단일 동사 규칙 같은 형태 강제는 두지 않는다 — 유저의 표현 자유 우선
- 기획 원칙: 주제 개수 제한은 두지 않는다. 10개 초과 시 안내만 (강제 X)
- 권장 한도: `RECOMMENDED_AXIS_COUNT_LIMIT = 5`, `RECOMMENDED_TOPIC_COUNT_LIMIT = 10`, `FOCUS_TOP_N = 3`
- **AI 제안 흐름의 경계 (도메인 격리)**: AI 제안 자체는 `product-aisuggestion.md` Product에서 `AxisSuggestionPort`·`AxisTopicSuggestionPort`로 제공. LearningFacade 도메인은 Spring AI(`ChatClient`/`PromptTemplate`/`BeanOutputConverter`)를 직접 import하지 않는다. 도메인은 제안 결과를 받아 `LearningFacade.addAxis()` / `LearningAxis.addTopics(List<TopicCommand>)`로 흡수만 담당. 다건 추가의 트랜잭션 경계(한 건 실패 시 전체 롤백)는 도메인 행위가 책임지고, AI 호출 실패는 상위 Application Service가 빈 목록으로 graceful degrade
- 연기된 항목
  - 주제 간 의존 관계 표시 — v2
  - 주제 템플릿 복사 — Library Product
  - **AxisTopic 의미 기반 중복 감지** — v2. Spring AI 임베딩으로 "캐릭터 설계" vs "인물 설계" 같은 의미 중복 후보 표시 (v1은 정확 문자열 비교만). `product-aisuggestion.md` 인프라 재사용

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **`displayOrder` 부여 위치**
    - 선택: **도메인 자동 계산** — `addAxis()` / `addTopic()`이 `현재 max + 1` 자동 부여, 외부 주입 금지
    - 거부 — 클라이언트 명시 입력: 부분 갱신·중복·구멍(gap) 발생. Application Service / Controller가 직접 부여하면 부여 시점 race condition. 도메인 외부 주입 금지가 Product DoD 항목으로 박힘
- **재배치(`reorderTopics`) 입력 검증 엄격도**
    - 선택: **전달 id 집합 ↔ 현재 컬렉션 id 집합 완전 일치 강제** — 불일치 시 도메인 예외
    - 거부 — 부분 reorder 허용: "주어진 id만 재정렬"은 유령 id 삽입·타 부모 자식 오염 위험. 빈 리스트는 정상 (no-op)으로 별도 처리
- **다건 주제 추가(`addTopics`) 트랜잭션 경계**
    - 선택: **한 건 실패 시 전체 롤백** (부분 성공 불허)
    - 거부 — 부분 성공 허용 + 실패 건만 보고: AI 제안 3건 중 2건만 저장되는 상태는 회고 가치가 없음. FE가 재시도 시 한 건씩 진단 가능
- **주제 이름 중복 검증 레벨**
    - 선택: **도메인(`LearningAxis.addTopic` 선체크) + DB UNIQUE(axis_id, name) 이중 방어**
    - 거부 — DB UNIQUE만: 친절한 에러 메시지 불가. 도메인만: 동시성 race에서 빠져나갈 수 있음. 둘 다 둠 (conventions §1.6)
- **권장 한도 안내 노출 방식**
    - 선택: **Aggregate가 `isAxisCountExceedsRecommended()` boolean 제공 + 응답 DTO 플래그**
    - 거부 — 도메인 예외로 저장 차단: 가이드라인이지 불변식 아님. 한도 값 변경 시 ErrorCode 폭증

---

## [Story 2-0] AxisAction → AxisTopic 도메인 마이그레이션

### 사용자 가치

> As a **LearningFacade 백엔드 개발자**,
> I want **기존 AxisAction 엔티티를 AxisTopic으로 교체하고 데이터를 마이그레이션하길**,
> So that **이후 스토리가 새 도메인 모델 위에서 구현될 수 있다.**

### 설명

- `AxisAction` 엔티티 제거, `AxisTopic` 엔티티 신규 도입
- AxisTopic 구조: `id`, `axis`(FK), `name`(필수), `description`(선택), `displayOrder`, `coverageStatus`, `createdAt`, `updatedAt`
- 단일 동사 검증 로직(`Description.validateSingleVerb()`) 제거
- 기존 `AxisAction.description`(동사형) 데이터는 `AxisTopic.name`으로 직접 이관
- 커버리지 연결(`action_material`)은 `topic_material` 테이블로 재명명
- 기존 `action_revision` 테이블은 `axis_action_description_history`로 아카이브 (조회만)

### 인수 조건 (Given/When/Then)

- [ ] Given Flyway 마이그레이션 실행, When 완료, Then 기존 AxisAction 데이터가 AxisTopic에 1:1 이관
- [ ] Given 단일 동사 검증 호출 코드, When 컴파일, Then 참조 모두 제거되어 컴파일 에러 없음
- [ ] Given 기존 `action_material` 매핑, When 마이그레이션, Then `topic_material`로 이관 + 연결 유지
- [ ] *(엣지 케이스 — 이력 아카이브)* 기존 `action_revision` 이력은 `axis_action_description_history` 테이블로 아카이브 (조회만 가능)

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] Flyway 마이그레이션 + 롤백 스크립트 작성
- [ ] 통합 테스트: 마이그레이션 전후 데이터 일치 검증
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 비목표

- 기존 단련 이력 UI 노출 — v2
- 데이터 마이그레이션 후 자동 보정(주제 이름 정제) — 명시적 비목표 (유저 직접)

### INVEST 점검

- **Independent**: DB 마이그레이션 + 엔티티 교체. 기능 흐름은 후속 Story
- **Negotiable**: 아카이브 테이블 이름은 합의
- **Valuable**: 후속 모든 Story의 전제
- **Estimable**: 마이그레이션 + 검증 통합 테스트
- **Small**: 5 SP
- **Testable**: 검증 SQL + 통합

---

## [Story 2-1] 세부 축 추가 및 관리 (단건 + 재배치)

### 사용자 가치

> As a **LearningFacade 설정 중인 유저**,
> I want **직업적 컨셉 아래에 세부 축을 추가하고 관리하길**,
> So that **내 학습 방향이 어떤 영역으로 나뉘는지 구조적으로 볼 수 있다.**

### 설명

- 축은 3~5개로 시작 권장 (온보딩 가이드 메시지). 6개 이상 시 `isAxisCountExceedsRecommended=true` 응답
- 축 이름은 자유 입력 (trim 정규화, 빈 문자열 거부, 동일 LearningFacade 내 중복 금지)
- 도메인 행위
  - `LearningFacade.addAxis(name)` — displayOrder 자동 부여
  - `LearningFacade.updateAxisName(axisId, name)` — 동일 값 unchanged
  - `LearningFacade.removeAxis(axisId)` — orphanRemoval + 자식 주제·자료 매핑 정리
  - `LearningFacade.reorderAxes(List<Long> orderedIds)` — id 집합 불일치면 예외
- ErrorCode: `LEARNING_AXIS_DUPLICATE_NAME`, `LEARNING_AXIS_NAME_BLANK`, `LEARNING_AXIS_NOT_FOUND`

### 인수 조건 (Given/When/Then)

- [ ] Given LearningFacade 홈에서 '축 추가' 버튼, When 입력창 열림, Then 축 이름 입력 후 저장 가능
- [ ] Given 첫 번째 축 추가, When 저장, Then `displayOrder = 1`
- [ ] Given 두 번째 축 추가, When 저장, Then `displayOrder = 마지막 + 1`
- [ ] Given 축 삭제, When 확인, Then 해당 축과 연결된 주제·자료 매핑 함께 정리 (orphanRemoval)
- [ ] Given 축 6개 이상 보유, When 응답, Then `isAxisCountExceedsRecommended = true` 플래그 포함
- [ ] Given 동일 이름으로 축 추가 시도, When 호출, Then `LEARNING_AXIS_DUPLICATE_NAME` 예외
- [ ] Given reorder 요청 시 id 집합 불일치, When 호출, Then 예외
- [ ] Given 빈 리스트로 reorder, When 호출, Then 정상 (no-op)
- [ ] *(엣지 케이스 — trim 정규화)* `"  데이터 모델링  "` → `"데이터 모델링"` 저장

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: `addAxis()` 첫번째/두번째 displayOrder / `reorderAxes` id 집합 검증 / 중복 이름 예외 / trim 정규화
- [ ] Repository Slice: `UNIQUE(facade_id, name)` 동작
- [ ] Controller Slice: 권장 한도 플래그 응답 형식 검증
- [ ] 디자인 QA 통과 (FE: 축 카드 UI + 드래그 앤 드롭)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 비목표

- **AI 축 제안 (Spring AI `ChatClient` 호출·프롬프트·파싱)** — `product-aisuggestion.md` Epic 3. 본 Story는 축 도메인 행위만, 제안 자체에는 관여하지 않음
- 축 권장 한도 강제 (저장 거부) — 명시적 비목표 (안내만)

### INVEST 점검

- **Independent**: 도메인 행위 + Repository + UI
- **Negotiable**: 권장 한도 5는 ADR로 정착
- **Valuable**: Layer 2 구성의 출발점
- **Estimable**: 도메인 + 슬라이스
- **Small**: 5 SP
- **Testable**: 모든 분기 단위 테스트

---

## [Story 2-2] 축 하위 주제(AxisTopic) 추가 및 관리 (단건 + 다건)

### 사용자 가치

> As a **세부 축을 구성한 유저**,
> I want **각 축 아래에 학습 주제를 자유롭게 추가·수정·삭제하고 싶고**,
> So that **이 축에서 내가 실제로 공부하고 싶은 영역이 명시적으로 보이고 커버리지를 관리할 수 있다.**

### 설명

- 주제 추가 방식
  - **직접 입력**: 이름(필수) + 설명(선택)
  - **AI 제안에서 선택** (`product-aisuggestion.md` Epic 4의 Spring AI `ChatClient` + Gemini Flash 2.5 + `BeanOutputConverter` 인프라가 5개 후보를 제공): FE는 체크박스 다중 선택 후 본 Story의 다건 추가 API(`POST /api/learning-facade/axes/{axisId}/topics`) 한 번 호출. 본 Story는 AI 호출 자체에는 관여하지 않으며, 도메인은 일반 다건 추가 흐름과 동일하게 동작 (한 건 실패 시 전체 롤백)
- 주제 이름 예시: "스토리의 본질 이해", "구조 설계 (Top-down)", "캐릭터 설계"
- 주제는 언제든 수정·삭제·재배치 가능
- 주제 추가 시 초기 `coverageStatus = NO_MATERIAL`
- 주제 10개 초과 시 `isTopicCountExceedsRecommended = true` 응답 (강제 X)
- 도메인 행위
  - `LearningAxis.addTopic(TopicCommand)` (단건) / `addTopics(List<TopicCommand>)` (다건, 한 건 실패 시 전체 롤백)
  - `LearningAxis.updateTopicName(topicId, name)`, `updateTopicDescription(topicId, desc)` — 동일 값 unchanged
  - `LearningAxis.removeTopic(topicId)` — orphanRemoval + 자료 매핑 정리
  - `LearningAxis.reorderTopics(List<Long> orderedIds)` — id 집합 검증
- ErrorCode: `AXIS_TOPIC_DUPLICATE_NAME`, `AXIS_TOPIC_NAME_BLANK`, `AXIS_TOPIC_NOT_FOUND`

### 인수 조건 (Given/When/Then)

- [ ] Given 축 카드 '주제 추가', When 입력창 열림, Then 이름·설명 입력 필드 + 'AI 제안 받기' 버튼 함께 표시
- [ ] Given 이름만 입력 후 저장, When 호출, Then 주제 추가 + 초기 `NO_MATERIAL`
- [ ] Given AI 제안 3개 체크 후 "선택한 주제 추가", When 호출, Then 3개가 다건으로 한 번에 추가 (한 건 실패 시 전체 롤백)
- [ ] Given 주제 드래그로 순서 변경, When 저장, Then `displayOrder` 재계산 (1-based 연속)
- [ ] Given 주제 삭제, When 확인, Then 연결된 자료 매핑 정리 (자료 자체는 보존)
- [ ] Given 동일 축 내 동일 이름 주제 추가, When 호출, Then `AXIS_TOPIC_DUPLICATE_NAME` 예외
- [ ] *(엣지 케이스 — description 빈 문자열)* description trim 후 빈 문자열은 null로 정규화 저장
- [ ] *(엣지 케이스 — 다건 빈 리스트)* 빈 리스트 다건 추가는 정상 처리 (no-op)

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: 단건/다건 추가 / 다건 부분 실패 시 전체 롤백 / reorder id 검증 / trim 정규화 / unchanged 반환
- [ ] Repository Slice: `UNIQUE(axis_id, name)` 동작
- [ ] 디자인 QA 통과 (FE: 드래그 앤 드롭 + AI 제안 체크박스 다중 선택)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 8 SP

### 비목표

- 주제 단련 이력 (수정 이유 기록) — Epic 3
- **AI 제안 자체 (호출·프롬프트·파싱)** — `product-aisuggestion.md` Epic 4. 본 Story는 제안 결과를 받아 다건 추가 API로 흡수만. Spring AI에 직접 의존하지 않음
- 의미 기반 중복 감지 (Spring AI 임베딩) — v2

### INVEST 점검

- **Independent**: 도메인 + 슬라이스
- **Negotiable**: 권장 한도 10은 ADR
- **Valuable**: 주제 단위 학습의 기반
- **Estimable**: 도메인 행위 4종 + 단건/다건
- **Small**: 8 SP
- **Testable**: 분기별 단위 테스트

### 참고

- 관련 API 스펙
  - `POST /api/learning-facade/axes/{axisId}/topics` (단건/다건)
  - `PATCH /api/learning-facade/topics/{topicId}`
  - `DELETE /api/learning-facade/topics/{topicId}`
  - `PATCH /api/learning-facade/axes/{axisId}/topics/reorder`

---

## [Story 2-3] 주제 우선순위 관리 및 집중 영역 표시 (FOCUS_TOP_N = 3)

### 사용자 가치

> As a **학습 방향을 단련 중인 유저**,
> I want **여러 주제 중 내가 지금 집중하고 싶은 주제를 상위에 두고 싶고**,
> So that **내 지도를 볼 때 "지금 여기에 에너지를 쓰고 있다"는 방향이 한눈에 드러난다.**

### 설명

- 주제 순서(`displayOrder`)가 곧 우선순위 — 상위에 있을수록 지금 집중하는 주제
- 축 카드에서 주제 목록은 `displayOrder` 오름차순
- 상위 `FOCUS_TOP_N = 3`개 주제는 "지금 집중 중" 시각 표시 (뱃지)
- 도메인 보조: `AxisTopic.isFocused(int focusTopN)` boolean. Application Service가 응답 DTO에 포함

### 인수 조건 (Given/When/Then)

- [ ] Given 축 카드 펼침, When 주제 목록 렌더링, Then `displayOrder` 오름차순 표시
- [ ] Given 상위 3개 주제, When 표시, Then "지금 집중 중" 뱃지 부착
- [ ] Given 4번째 주제를 최상위로 드래그, When 저장, Then 새 순서 저장 + 해당 주제 "지금 집중 중"에 포함
- [ ] *(엣지 케이스 — 주제 3개 미만)* 주제 개수가 3개 미만이면 모든 주제가 "지금 집중 중" 표시

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: `isFocused` 경계값 (focusTopN=3, 주제 0/1/3/5개)
- [ ] 통합 테스트: 재배치 → 응답 DTO에 `isFocused` 갱신
- [ ] 디자인 QA 통과 (FE: 뱃지 + 드래그 UX)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 3 SP

### 비목표

- 집중 영역 자동 추천 — v2
- 집중 영역별 학습 로그 — v2

### INVEST 점검

- **Independent**: 도메인 boolean + UI 뱃지
- **Negotiable**: `FOCUS_TOP_N` 값은 ADR
- **Valuable**: 우선순위 표현
- **Estimable**: 메서드 + 뱃지
- **Small**: 3 SP
- **Testable**: 경계값 단위 테스트

---

# [Epic 3] AxisTopic 단련 — 수정 이력 및 단련 안내

## 목표

> 주제를 단순 편집이 아니라 "방향 단련"의 과정으로 다룬다. 주제 수정·삭제·추가 이력을 보존해 유저가 자신의 사고 변화를 돌아볼 수 있게 한다.

## 배경

- 기존 Epic "행동 단련"은 단일 동사를 바꿀 때 이유를 기록하는 구조였다 (폐기됨)
- 새 구조에서는 주제(명사구)가 단위다. 이유 기록의 의미는 유지하되 더 가볍게 운영한다
- "주제가 여러 번 바뀐다"는 사실 자체가 유저에게 "내가 아직 이 축에서 무엇을 공부하려는지 명확하지 않다"는 신호가 된다

## 포함 Story

- [ ] Story 3-1. 주제 이름 수정 및 커버리지 재평가
- [ ] Story 3-2. 수정 이유 기록 및 이력 조회 (`RevisionReasonOption` DB 관리)
- [ ] Story 3-3. 수정 빈도 기반 단련 안내 (`REFINEMENT_THRESHOLD = 3`)

## Epic 인수 시나리오

`유저가 주제 "구현하다" → "설계" 수정 (옵션: 이유 "더 정확한 표현을 찾았다" 선택)` → `AxisTopic.updateName() → TopicChangeRecord.changed()` → `coverageStatus = NO_MATERIAL 재초기화 + TopicRevision 이력 저장 (revisionReasonLabel 스냅샷)` → `유저가 재방문 → 이력 타임라인 토글로 "구현하다 → 설계 / 더 정확한 표현을 찾았다 / 3일 전"` → `같은 주제 3회 이상 수정 시 isRefinementSuggested=true` → `"이 주제가 아직 단련 중이에요. 이 축에서 진짜 하고 싶은 것이 무엇인지 다시 생각해보세요" 안내`

## Epic 완료 기준 (DoD)

- [ ] 주제 이름 수정 시 이력이 저장된다 (이전 이름 / 이후 이름 / 선택 이유 / 수정 시각)
- [ ] 수정 이유 선택지는 DB로 관리되어 운영 중 코드 배포 없이 변경 가능하다 (`revision_reason_option`)
- [ ] 수정 횟수가 임계값(`REFINEMENT_THRESHOLD = 3`) 초과 시 "이 주제가 아직 단련 중이에요" 안내가 표시된다
- [ ] 주제 삭제 이력도 별도 보존된다 (회고 가능)
- [ ] ADR 갱신: "수정 이유 선택지 DB 관리 + 레이블 스냅샷"
- [ ] 연결된 Story 모두 Done

## 내부 메모 / 제약 사항

- 기술 제약: `topic_revision` 테이블 신규. `revision_reason_option` 테이블은 기존 재활용 (선택지 레이블은 주제 맥락에 맞게 갱신)
- 기획 원칙: 이유 선택은 강제하지 않는다. 선택하지 않아도 수정 허용
- 기획 원칙: 수정 이유는 이력에 **레이블 스냅샷**으로 저장 — 선택지 비활성화/삭제돼도 과거 이력 영구 보존
- 연기된 항목
  - 삭제된 주제 복원 기능 — v2 (AI 무관)
  - **주제 단련 이유 자동 추천** — v2. Spring AI `ChatClient`로 이전→이후 이름 차이를 분석해 가장 가까운 `revision_reason_option`을 기본값으로 추천 (유저는 그대로 두거나 변경 가능). LearningFacade 측 `RevisionReasonSuggestionPort` 신설 + Adapter는 `product-aisuggestion.md` Spring AI 인프라(`PromptTemplate` + `BeanOutputConverter` + Static Fallback) 재사용. 도메인은 Spring AI 직접 의존 금지
  - **수정 횟수 임계 초과 시 AI 기반 단련 메시지 개인화** — v2. 현재는 정적 문구 "이 주제가 아직 단련 중이에요". Spring AI로 이력 패턴 기반 맞춤 안내 생성

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **주제 이름 변경 시 커버리지 처리**
    - 선택: **`coverageStatus = NO_MATERIAL` 강제 초기화** (Story 3-1) — 이름이 바뀌면 기존 자료가 실제로 새 주제를 커버하는지 유저가 다시 판단해야 함
    - 거부 — 커버리지 유지: 주제 의미가 바뀌었을 가능성 있는데 ✅ 표시가 그대로 남으면 회고 가치 손상. 단, ADR004는 "주제 이름·설명 수정은 재계산 트리거가 아니다"라고 단순화했음 — Epic 3은 이름 수정의 의미를 다시 살려 NO_MATERIAL 초기화로 결정 (단련 의도의 표현)
    - 단, **설명(`description`) 변경**은 보조 정보이므로 커버리지 미영향 (v4 도메인 결정 #3)
- **`revisionCount` 추적 범위**
    - 선택: **이름 변경만 카운트, 설명 변경 제외** (v4 도메인 결정 #2)
    - 거부 — 모두 카운트: "단련 신호"의 의미가 흐려짐. 설명 미세 수정으로 임계 도달 시 안내 노이즈
- **`revision_reason_option` 운영 방식**
    - 선택: **DB 테이블로 관리** (`label`, `display_order`, `active`) + 운영 중 직접 수정으로 코드 배포 없이 변경
    - 거부 — Enum 하드코딩: 운영 중 선택지 추가/변경 시 매번 배포 필요. Epic 3 핵심 기능 (운영 유연성)이 배포 라이프사이클에 묶임
- **수정 이유 이력 저장 방식**
    - 선택: **`revisionReasonLabel` 레이블 스냅샷 저장 (FK 아님, nullable)** — 선택지 비활성화/삭제돼도 과거 이력 영구 보존
    - 거부 — FK 참조: 선택지 삭제 시 ON DELETE SET NULL이면 과거 이력 손실. 보존하려면 CASCADE 금지가 필요한데, 스냅샷이 더 단순
- **이유 선택의 강제성**
    - 선택: **선택 사항 (이유 미선택해도 수정 허용 + `revisionReasonLabel = null` 이력 생성)**
    - 거부 — 필수: 유저 흐름 단절. "이유 모름" 옵션을 강제로 추가하면 통계 의미 손상
- **`REFINEMENT_THRESHOLD = 3` 임계 위반 처리**
    - 선택: **`isRefinementSuggested` boolean 응답 플래그 + UI 안내**
    - 거부 — 수정 차단: "단련 중"은 가치 판단 아닌 신호. 차단하면 정작 더 단련해야 할 때 막힘

---

## [Story 3-1] 주제 이름 수정 및 커버리지 재평가

### 사용자 가치

> As a **자신의 학습 방향을 단련 중인 유저**,
> I want **주제 이름을 더 정확한 표현으로 수정하고 싶고**,
> So that **내가 이 축에서 실제로 공부하고 싶은 것이 더 명확하게 보인다.**

### 설명

- 유저는 주제를 처음 정의할 때 완벽한 이름을 고르지 않는다
- 시스템은 수정 자체를 자연스러운 행위로 수용하되, 수정 사실을 도메인 수준에서 기록하고 커버리지를 재평가
- 도메인 행위: `AxisTopic.updateName(String name)` → `TopicChangeRecord.changed()` / `unchanged()`
- 결과: 변경 시 `coverageStatus = NO_MATERIAL`로 초기화 (FE는 "주제가 변경되어 연결된 자료를 다시 확인해보세요" 안내)

### 도메인 VO

`TopicChangeRecord` — 정적 팩토리 `changed(prev, next)` / `unchanged(name)`. `isChanged()` 노출. 동일 값 입력 시 `unchanged`로 저장 쿼리 미발생.

### 인수 조건 (Given/When/Then)

- [ ] Given 주제 이름 수정, When 다른 표현으로 저장, Then 이름 변경 + `coverageStatus = NO_MATERIAL` 재초기화 + `TopicChangeRecord.changed()` 반환
- [ ] Given 동일 값 저장, When 호출, Then 상태 변경 없이 `unchanged` 반환 + 저장 쿼리 미발생
- [ ] Given 수정 후 커버리지 초기화, When 응답, Then FE는 "주제가 변경되어 연결된 자료를 다시 확인해보세요" 안내
- [ ] *(엣지 케이스 — trim)* 앞뒤 공백 입력 시 trim 후 비교 — 정규화된 동일 값이면 `unchanged`

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: `updateName()` 변경 / 동일 / trim 정규화 / 커버리지 재초기화
- [ ] `TopicChangeRecord` 단위 테스트 (`isChanged()` true/false)
- [ ] Application Service 테스트
- [ ] FE: 수정 인라인 편집 UI + 커버리지 초기화 안내
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 비목표

- 자동 자료 재매핑 (의미 유사도 기반) — v2

### INVEST 점검

- **Independent**: 도메인 행위 + Application 조율
- **Negotiable**: 커버리지 초기화 vs 유지는 ADR (초기화 결정)
- **Valuable**: 방향 단련의 도메인 표현
- **Estimable**: 도메인 + 슬라이스
- **Small**: 5 SP
- **Testable**: 모든 분기 단위 테스트

---

## [Story 3-2] 수정 이유 기록 및 이력 조회 (RevisionReasonOption DB 관리)

### 사용자 가치

> As a **자신의 생각이 어떻게 변해왔는지 돌아보고 싶은 유저**,
> I want **이 주제의 이름을 왜, 어떻게 바꿔왔는지 흐름을 보고 싶고**,
> So that **나의 변경이 합리적이었는지 스스로 판단하고 더 나은 방향을 찾을 수 있다.**

### 설명

- 수정 이유는 유저의 사고 흔적이다
- 이유 선택은 강제하지 않는다. 선택하지 않아도 수정은 허용
- 수정 이유 선택지는 운영 중 계속 바뀔 수 있다 — 코드 배포 없이 DB로 관리

### DB 모델

`revision_reason_option`: `id`, `label`, `display_order`, `active`(false면 신규 노출 제외), `created_at`. v1에서는 DB 직접 수정으로 운영. Admin BC 구현 시 관리 화면으로 이관.

**초기 Flyway seed** (4개): "기존 표현이 너무 좁았다", "기존 표현이 너무 넓었다", "더 정확한 표현을 찾았다", "방향 자체가 바뀌었다"

`topic_revision`: `id`, `topic`(@ManyToOne), `previousName`, `newName`, `revisionReasonLabel`(스냅샷, FK 아님, nullable), `revisedAt`(@CreationTimestamp). 이력 삭제 안 함.

### 인수 조건 (Given/When/Then)

- [ ] Given 주제 이름 수정 + 이유 선택, When 저장, Then `topic_revision`에 `previousName`, `newName`, `revisionReasonLabel`(레이블 스냅샷) 저장
- [ ] Given 이유 미선택, When 저장, Then 이력 생성 + `revisionReasonLabel = null` (수정 자체 허용)
- [ ] Given 이력 조회, When 응답, Then 수정 시각 오름차순 이전→이후 이름 + 이유 레이블 목록
- [ ] Given 이유 선택지가 DB에서 비활성화, When 기존 이력 조회, Then 레이블 스냅샷 그대로 유지 (변경 없음)
- [ ] Given 수정 이력 없는 주제 조회, When 응답, Then 빈 목록
- [ ] *(엣지 케이스 — orphanRemoval)* 주제 삭제 시 연결된 이력도 함께 삭제
- [ ] *(엣지 케이스 — 비활성 선택지 시도)* 존재하지 않거나 비활성화된 선택지 id 전달 시 `REVISION_REASON_NOT_FOUND` 예외

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] `revision_reason_option` Flyway seed (초기 4개)
- [ ] `topic_revision` 엔티티 + Repository
- [ ] Application Service 이력 저장 테스트 — 이유 있음 / 없음 케이스
- [ ] Application Service 이력 조회 테스트
- [ ] 비활성 선택지 예외 테스트
- [ ] FE: 수정 모달 라디오 (선택 사항) + 이력 타임라인 토글
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 비목표

- 이력 검색·필터링 — v2
- 이력 통계 대시보드 — v2

### INVEST 점검

- **Independent**: 이력 엔티티 + Repository + UI
- **Negotiable**: 초기 선택지 4종은 PO 합의
- **Valuable**: 회고 가능성 — 유저 사고 변화 가시화
- **Estimable**: 테이블 + 엔티티 + 화면
- **Small**: 5 SP
- **Testable**: 저장/조회 슬라이스

### 의존성

- 선행: Story 3-1

---

## [Story 3-3] 수정 빈도 기반 단련 안내 (REFINEMENT_THRESHOLD = 3)

### 사용자 가치

> As a **같은 주제를 여러 번 수정하고 있는 유저**,
> I want **이 주제가 아직 명확히 정의되지 않았다는 신호를 받고 싶고**,
> So that **더 근본적인 질문("내가 이 축에서 진짜 하고 싶은 것이 무엇인가")을 스스로에게 던질 수 있다.**

### 설명

- 같은 주제를 반복 수정 = "방향 단련" 또는 "탐색 신호" 둘 중 하나
- 시스템은 구분하지 않고 임계값 초과 시 "이 주제가 아직 단련 중이에요" 안내
- 강제 제한 없음. 안내는 유저의 결정을 돕는 맥락 정보
- 도메인 필드: `AxisTopic.revisionCount` (생성 시 0, 동일 값 입력 시 미증가)
- 도메인 보조: `AxisTopic.isRefinementSuggested()` (`revisionCount >= REFINEMENT_THRESHOLD` boolean)
- 임계값은 도메인 상수 (`REFINEMENT_THRESHOLD = 3`) — 변경 시 한 곳만 수정

### 인수 조건 (Given/When/Then)

- [ ] Given 주제 수정, When 저장, Then `revisionCount += 1`
- [ ] Given `revisionCount >= 3`인 주제 수정, When 저장, Then 응답에 `isRefinementSuggested: true` 플래그
- [ ] Given `isRefinementSuggested: true` 응답, When 수정 완료 후, Then "이 주제가 아직 단련 중이에요" 안내 표시
- [ ] *(엣지 케이스 — 동일 값 재입력)* 기존 값과 동일한 수정 시 `revisionCount` 미증가

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: `isRefinementSuggested()` 경계값 (2 / 3 / 4)
- [ ] 단위 테스트: `revisionCount` 증가 + 동일 값 미증가
- [ ] FE: 수정 완료 후 단련 안내 토스트
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 3 SP

### 비목표

- 자동 주제 제안 (수정 빈도 기반) — Phase 2 (학습 루프와 함께)
- 임계 임의 조정 UI — v2

### INVEST 점검

- **Independent**: 도메인 필드 + boolean + 응답 플래그
- **Negotiable**: 임계값(3)은 ADR
- **Valuable**: 탐색 신호 가시화
- **Estimable**: 필드 + 메서드 + UI
- **Small**: 3 SP
- **Testable**: 경계값 단위 테스트

---

# [Epic 4] 학습 자료 연결 및 커버리지 — 4종 타입 확장

## 목표

> 각 주제에 다양한 형태의 학습 자료(책 / 강의 / AI 대화 / 웹 리소스)를 연결해 "내 주제들이 어떻게 커버되고 있는가"를 로드맵 위에서 확인할 수 있다.

## 배경

- 이전 버전은 자료 타입을 Top-down / Bottom-up 두 가지로만 구분했다
- 실제 유저의 학습 소스는 훨씬 다양 — 책, 유료 강의, ChatGPT/Claude 대화, Notion 메모, 블로그 글
- 정적 자료(책·강의)와 동적 자료(AI 대화·웹 리소스)의 성격이 다르다 — 타입으로 구분해 커버리지 해석에 반영
- 세상이 빠르게 바뀌는 영역일수록 동적 자료의 비중이 커진다. 유저가 "나는 주로 어떤 소스로 공부하는가"를 인식할 수 있어야 한다

## 포함 Story

- [ ] Story 4-1. 학습 자료 등록 및 주제 연결 (4종 타입)
- [ ] Story 4-2. 자료 숙련도 자가 평가 슬라이더
- [ ] Story 4-3. 주제별 자료 비중 표시 (정적/동적)

## Epic 인수 시나리오

`유저가 자료 등록 폼 → 타입 라디오 (BOOK/COURSE/AI_CONVERSATION/WEB_RESOURCE) 선택 + 타입별 필수 필드 동적 표시` → `자료명 + 연결 주제 선택 (다대다) + 저장` → `TopicMaterial 매핑 생성 + coverageStatus 자동 갱신 (NO_MATERIAL → COVERED, 동기)` → `유저가 자료별 숙련도 슬라이더 (미평가 → 낯섦 → 익숙해지는 중 → 마스터)` → `숙련도에 따라 주제 카드 커버리지 표시 갱신` → `주제 카드 하단 "📖 2 🎓 1 💬 3 🌐 5" 분포 + 정적/동적 비중 게이지`

## Epic 완료 기준 (DoD)

- [ ] 주제에 학습 자료를 연결할 수 있다 (다대다)
- [ ] 자료 타입 4종 분류가 가능하다 — `BOOK`, `COURSE`, `AI_CONVERSATION`, `WEB_RESOURCE`
- [ ] 자료별 숙련도 자가 평가 슬라이더가 작동한다
- [ ] 주제 카드에 "정적 자료 / 동적 자료 비중" 표시가 보인다
- [ ] `coverageStatus` 역정규화가 동일 트랜잭션 내 동기 갱신된다
- [ ] ADR 갱신: "자료 4종 타입 + 정적/동적 분류 결정 사유"
- [ ] 연결된 Story 모두 Done

## 내부 메모 / 제약 사항

- 기술 제약: `LearningMaterial.type` 컬럼 추가. 기존 `top_down` / `bottom_up` 값은 각각 `BOOK`/`WEB_RESOURCE` 기본값으로 이관 후 유저 수정
- 기획 원칙: 정적 자료(BOOK, COURSE) = 목차가 고정된 자료, 동적 자료(AI_CONVERSATION, WEB_RESOURCE) = 맥락에 따라 살아있는 자료
- 연기된 항목
  - **자료 자동 크롤링·썸네일·URL → 메타데이터(제목/저자/요약) 자동 추출** — v2. Spring AI `ChatClient`로 URL 본문 요약·메타데이터 추출. `MaterialMetadataExtractionPort` 신설 + Adapter는 `product-aisuggestion.md` 인프라(`spring-ai-starter-model-vertex-ai-gemini` + `PromptTemplate` + `BeanOutputConverter` + Static Fallback) 재사용. 도메인은 Spring AI 직접 의존 금지
  - **숙련도 자동 추정 (학습 이력·자료 진행도 기반)** — v2. 통계 우선 + 필요 시 Spring AI 보조 가능. v1은 유저 직접 슬라이더 입력만
  - **외부 자료 추천 (주제별 책·강의·웹 리소스 검색)** — v2/v3. Spring AI + 외부 검색 API 결합
  - 파일 첨부 — v2 (AI 무관)

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **자료 타입 분류 단위**
    - 선택: **4종 (`BOOK` / `COURSE` / `AI_CONVERSATION` / `WEB_RESOURCE`) + Enum `isStatic()` / `isDynamic()` 보조**
    - 거부 — 2종(Top-down/Bottom-up): 실제 학습 소스(AI 대화·웹 리소스)가 표현 불가. 추상 차원이 너무 높아 유저가 자기 패턴을 인식 불가
    - 거부 — N종 자유 태깅: 정적/동적 비중 게이지(Story 4-3) 같은 집계 의미가 약해짐. 4종으로 한정해 패턴 자각 효과 확보
- **타입별 부가 속성(author/platform/aiProvider/webSource) 검증**
    - 선택: **모든 부가 속성 nullable + 도메인 검증 없음** (v4 도메인 결정 #8) — FE가 필수 표시 관리
    - 거부 — 타입별 필수 강제: 유저 자유도 침해. 책 정보를 모르는 상태에서도 "지금 기록"이 더 중요
- **`TopicMaterial` 다대다 매핑 구조**
    - 선택: **매핑 엔티티 + surrogate `id` + `UNIQUE(topic_id, material_id)`** (ADR001 PK 정책)
    - 거부 — JPA `@ManyToMany` 자동 조인 테이블: `linked_at` audit 추가 불가, 향후 부가 속성 확장 비용
    - 거부 — 복합 PK `(topic_id, material_id)`: ADR001과 일관성 깨짐. JPA 엔티티 식별자 다루기 복잡
- **`coverageStatus` 갱신 트리거 응집**
    - 선택: **`CoverageRecalculator` Application Service에 단일 집결** — TopicMaterial 연결/해제 + proficiencyLevel 변경 두 트리거만
    - 거부 — Aggregate가 직접 갱신: Aggregate가 다른 Aggregate 의존 (TopicMaterial 조회 필요). 도메인 응집 위반. `CoverageRecalculator`는 Application Service 위치 (conventions §1.7)
- **숙련도(`proficiencyLevel`) 단계 수**
    - 선택: **4단계 (`UNRATED` / `UNFAMILIAR` / `GETTING_USED` / `MASTERED`)** + 슬라이더 3단계 스냅 (미평가 별도)
    - 거부 — 0~100 연속값: "엄밀한 측정이 아닌 자기 인식 기록"이라는 Story 4-2 정의와 맞지 않음. 슬라이더 미세 조정에 인지 부담 증가
- **자료-주제 매핑 0건 허용 (자료명만 입력)**
    - 선택: **저장 허용 + "연결 주제가 없으면 이 자료는 어디에도 나타나지 않아요" 경고** (Story 4-1 엣지 케이스)
    - 거부 — 저장 차단: 자료 등록 흐름이 끊김. 추후 주제에 연결 가능

---

## [Story 4-1] 학습 자료 등록 및 주제 연결 (4종 타입)

### 사용자 가치

> As a **주제를 정의한 유저**,
> I want **각 주제에 다양한 형태의 학습 자료를 등록하고 연결하길**,
> So that **이 주제를 어떤 소스로 채우고 있는지 명확히 알고 커버리지를 관리할 수 있다.**

### 설명

- 자료 등록 시 필수 입력: 자료명, 타입, 연결 주제 (하나 이상)
- 자료 타입
  - `BOOK` — 책 (제목 + 저자)
  - `COURSE` — 강의 (플랫폼 + 강의명 + URL)
  - `AI_CONVERSATION` — AI 대화 (어떤 AI / 대화 주제 / 대화 링크 또는 요약)
  - `WEB_RESOURCE` — 웹 리소스 (Notion 페이지, 블로그, 문서 + URL)
- 선택 입력: URL, 저자/출처, 메모
- 하나의 자료가 여러 주제에 연결 가능 (예: Notion 메모 하나가 "캐릭터 설계"와 "감정 설계"에 모두 연결)
- 매핑 엔티티: `TopicMaterial` — surrogate `id` + `UNIQUE(topic_id, material_id)`. 매핑 중복 방지 도메인 + DB 이중 방어
- `coverageStatus` 동일 트랜잭션 내 동기 갱신 (`CoverageRecalculator`)

### 인수 조건 (Given/When/Then)

- [ ] Given 주제 카드 '자료 추가', When 폼 열림, Then 자료 타입 라디오(4종) + 타입별 필수 필드 동적 표시
- [ ] Given 자료 저장, When 호출, Then 연결된 모든 주제의 `coverageStatus`가 `NO_MATERIAL → PARTIAL` 또는 `COVERED`로 갱신
- [ ] Given `AI_CONVERSATION` 선택, When 폼 표시, Then "어떤 AI (Claude / ChatGPT / Gemini 등)" + "대화 주제 또는 요약" 필드
- [ ] Given `WEB_RESOURCE` 선택, When 폼 표시, Then "소스 (Notion / 블로그 / 문서 등)" + URL 필드
- [ ] Given 동일 (topic_id, material_id) 매핑 중복 추가 시도, When 호출, Then 도메인 예외 + DB UNIQUE 이중 방어
- [ ] *(엣지 케이스 — 연결 주제 미선택)* 자료명만 입력 + 연결 주제 미선택 시 "연결 주제가 없으면 이 자료는 어디에도 나타나지 않아요" 경고 (저장 허용)

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: 타입별 필수 필드 검증 / 매핑 중복 방지 / `coverageStatus` 갱신
- [ ] Repository Slice: `topic_material` UNIQUE 동작 + 자료 타입 CHECK 제약
- [ ] 통합 테스트: 등록 → coverageStatus 동기 갱신
- [ ] 디자인 QA 통과 (FE: 타입별 동적 폼 + 커버리지 변경)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 8 SP

### 비목표

- 자료 검색·필터 — v2
- **자료 자동 메타데이터 추출 (URL → 제목/저자/요약)** — v2. Spring AI `ChatClient` + `MaterialMetadataExtractionPort` (Adapter는 `product-aisuggestion.md` 인프라 재사용). 도메인 직접 의존 금지
- **외부 자료 추천 (주제별 자료 검색)** — v2/v3. Spring AI + 검색 API 결합

### INVEST 점검

- **Independent**: 자료 엔티티 + 매핑 + 폼
- **Negotiable**: 4종 타입 외 추가는 v2
- **Valuable**: 커버리지 모델의 기반
- **Estimable**: 도메인 + 슬라이스 + UI
- **Small**: 8 SP
- **Testable**: 타입별 단위 + Slice

---

## [Story 4-2] 자료 숙련도 자가 평가 슬라이더

### 사용자 가치

> As a **학습 자료를 등록한 유저**,
> I want **자료에 대한 내 숙련도를 슬라이더로 대략적으로 표현하길**,
> So that **이 자료가 해당 주제를 얼마나 실질적으로 커버하고 있는지를 로드맵 위에서 직관적으로 파악할 수 있다.**

### 설명

- 자료별로 유저가 직접 숙련도 자가 평가
- 슬라이더 범위: **낯섦 → 익숙해지는 중 → 마스터** (3단계 스냅)
- 정확한 수치가 아닌 "대략 이 정도 느낌"의 표현 — 엄밀한 측정이 아닌 자기 인식 기록
- 숙련도는 등록 직후 기본값 없음(미평가) 상태로 시작
- 숙련도에 따른 커버리지 표시
  - 미평가: ⚠️ 자료 있음 (미평가)
  - 낯섦~익숙해지는 중: ✅ 자료 있음 (학습중)
  - 마스터: ✅ 자료 있음 (마스터)
- 숙련도 변경 시 동일 트랜잭션 내 동기 커버리지 재계산

### 인수 조건 (Given/When/Then)

- [ ] Given 자료 카드 슬라이더 드래그, When 놓음, Then 낯섦 / 익숙해지는 중 / 마스터 중 가장 가까운 단계로 스냅 저장
- [ ] Given 숙련도 변경, When 저장, Then 연결된 주제의 커버리지 표시 즉시 업데이트 (동기)
- [ ] Given 자료 등록 직후, When 카드 표시, Then 슬라이더 미평가 + ⚠️ 자료 있음 (미평가)
- [ ] *(엣지 케이스 — 마스터 안내)* 슬라이더를 '마스터'로 이동 시 "이 자료로 학습하며 생긴 Card가 있나요?" 안내 (닫기 가능, 강제 아님)

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: 숙련도 저장 + 커버리지 동기 갱신
- [ ] Repository Slice: 숙련도 컬럼 CHECK 제약
- [ ] 디자인 QA 통과 (FE: 슬라이더 UX + 3단계 스냅)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 비목표

- **숙련도 자동 추정 (학습 이력·자료 진행도 기반)** — v2. 통계 우선, 필요 시 Spring AI 보조 가능 (`product-aisuggestion.md` 인프라 재사용)
- 숙련도 변경 이력 — v2

### INVEST 점검

- **Independent**: 자료 필드 + 슬라이더
- **Negotiable**: 3단계 스냅 정책은 ADR
- **Valuable**: 커버리지의 의미 강화
- **Estimable**: 필드 + UX + 커버리지 갱신
- **Small**: 5 SP
- **Testable**: 단위 + Slice

### 의존성

- 선행: Story 4-1

---

## [Story 4-3] 주제별 자료 비중 표시 (정적/동적)

### 사용자 가치

> As a **자기 학습 패턴을 인식하고 싶은 유저**,
> I want **각 주제가 어떤 타입의 자료로 커버되고 있는지 한눈에 보고 싶고**,
> So that **"나는 이 주제를 주로 책으로 배우는구나" 또는 "AI 대화로만 채웠구나"를 자각하고 균형을 검토할 수 있다.**

### 설명

- 주제 카드 하단에 4종 타입 자료 개수 + 아이콘
- 예: `📖 2 🎓 1 💬 3 🌐 5`
- 정적 자료(BOOK+COURSE) / 동적 자료(AI_CONVERSATION+WEB_RESOURCE) 비중을 퍼센트 게이지로 표시
- 유도용일 뿐 특정 비중을 강제하지 않음

### 인수 조건 (Given/When/Then)

- [ ] Given 주제 카드 렌더링, When 표시, Then 4종 타입별 자료 개수 + 아이콘
- [ ] Given 정적·동적 자료 비중, When 표시, Then "정적 30% / 동적 70%" 형태의 게이지
- [ ] Given 한 타입 자료 0개, When 표시, Then 해당 아이콘 회색 처리 (숨김 X — 빈 영역이 보여야 균형 인식)
- [ ] *(엣지 케이스 — 자료 0개)* 자료가 하나도 없을 때 게이지 대신 `⚠️ 자료 없음`만 표시

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: 타입별 카운트 집계 + 정적/동적 비중 계산
- [ ] Repository Slice: 집계 쿼리 인덱스 동작
- [ ] 디자인 QA 통과 (FE: 아이콘 + 게이지)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 3 SP

### 비목표

- 비중 권장값·경고 — 명시적 비목표 (유도용)
- 학습 패턴 통계 대시보드 — v2

### INVEST 점검

- **Independent**: 집계 쿼리 + UI
- **Negotiable**: 게이지 비주얼은 디자인 QA
- **Valuable**: 자기 인식 도구
- **Estimable**: 집계 + 표시
- **Small**: 3 SP
- **Testable**: 분포 단위 테스트

---

# [Epic 5] 커버리지 점검 및 공백 알림

## 목표

> 주제가 추가되거나 자료가 연결되지 않은 공백이 생겼을 때, 시스템이 커버리지 공백을 시각화하고 자료 추가를 유도한다.

## 배경

- 주제가 늘어나도 자료가 함께 늘지 않으면 로드맵이 공허해짐
- Third Tool의 역할은 학습 스케줄 관리가 아니라 커버리지 공백을 드러내는 것
- 시스템이 능동적으로 공백을 보여줘야 유저가 로드맵을 유동적으로 관리할 수 있음

## 포함 Story

- [ ] Story 5-1. 커버리지 공백 시각화
- [ ] Story 5-2. 주제 추가 시 자료 추가 유도

## Epic 인수 시나리오

`LearningFacade 홈 진입` → `각 주제 옆 ✅/⚠️ 표시` → `상단 "14개 주제 중 3개 미커버" 요약` → `미커버 주제 있는 축 카드는 상단 경고 뱃지` → `유저가 새 주제 추가 → 자료 미연결 → "이 주제를 커버할 자료가 없습니다. 지금 추가하시겠어요?" 인라인 메시지` → `'지금 추가' 클릭 → 자료 등록 폼 (연결 주제 자동 선택)` → `'나중에' → ⚠️ 공백 표시 유지`

## Epic 완료 기준 (DoD)

- [ ] 커버되지 않은 주제가 시각적으로 구분된다 (⚠️ 표시)
- [ ] 주제 추가 시 자료 추가 유도 메시지가 표시된다
- [ ] LearningFacade 홈에서 전체 커버리지 요약이 확인된다
- [ ] 연결된 Story 모두 Done

## 내부 메모 / 제약 사항

- 기술 제약: 알림은 인앱 알림 우선, 이메일 알림은 v2
- 기획 원칙: 알림은 강제가 아닌 유도 — 무시해도 기능 사용 제한 없음
- 기획 원칙: 주기 기반 점검 알림은 Third Tool의 관심사가 아님 — 커버리지 공백(자료 미연결 주제)만 알린다
- **갭 컨텍스트 데이터 원천 책임**: 본 Epic이 집계하는 커버리지 공백(`NO_MATERIAL` / `PARTIAL` 주제)과 Epic 4의 낯섦 숙련도 자료, Epic 2·3의 축 ↔ Card 매핑(`AxisTopic` → `TopicMaterial` → `LearningMaterial` → `Deck` → `Card`)은 `product-aisuggestion.md` Epic 6 (v1.5 갭 인지형 개인화 컨텍스트)의 **데이터 원천**으로 노출된다. 노출 수단은 LearningFacade가 제공하는 **inbound 쿼리 인터페이스** `LearningFacadePersonalizationQuery` (Application Layer Public Read API). 시그니처:

```java
public interface LearningFacadePersonalizationQuery {
    // 커버리지 공백 (NO_MATERIAL > PARTIAL 우선순위, displayOrder 보조 정렬)
    List<CoverageGapView> findCoverageGaps(Long facadeId,
                                            Optional<Long> scopedAxisId,
                                            int limit);

    // 낯섦 숙련도 자료 (Epic 4의 proficiencyLevel = UNFAMILIAR)
    List<WeakTopicView> findWeakTopics(Long facadeId,
                                       Optional<Long> scopedAxisId,
                                       int limit);

    // staleAxes 결합용 — aisuggestion BC Adapter가 본 매핑을 받아
    // Card BC의 CardStalenessQuery.findLastViewedAtByCardIds(cardIds)와 결합
    Map<AxisRef, Set<Long>> findAxisToCardIdsMapping(Long facadeId);
}

public record CoverageGapView(String axisName, String topicName, CoverageStatus status) {}
public record WeakTopicView(String axisName, String topicName, String materialName) {}
public record AxisRef(Long axisId, String axisName) {}
```

  - **성능 목표**: 각 쿼리 P95 ≤ 200ms (전체 갭 컨텍스트 수집 P95 ≤ 200ms 임계 내)
  - **인덱스 활용**: `axis_topic.coverage_status` (Epic 4 역정규화 컬럼) / `topic_material.proficiency_level` / `learning_material.deck_id` / 커버링 인덱스 (Story 4-3·5-1에서 이미 설계됨, 재사용)
  - **read-only 보장**: 본 쿼리는 모두 read-only — `@Transactional(readOnly = true)`. 도메인 행위(Aggregate 메서드)에 영향을 주지 않음. dirty checking 미발생
  - **BC 경계**: `PersonalizationContextPort` 정의·LLM 통합·갭 보완 검증·메트릭 태깅은 `product-aisuggestion.md` Epic 6 책임. LearningFacade는 데이터만 제공, AI 호출에는 관여하지 않음. 도메인은 Spring AI / `PersonalizationContextPort`를 직접 import 금지. aisuggestion BC가 staleAxes 산정 시 `findAxisToCardIdsMapping`으로 받은 cardIds를 Card BC의 `CardStalenessQuery`에 전달해 두 BC 데이터를 본 BC가 모르게 결합
- 연기된 항목: 외부 기술 트렌드 감지 기반 주제 추가 제안 (Roadmap Drift) — v2

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **커버리지 집계 방식 (`CoverageSummary`)**
    - 선택: **Aggregate Root 로드 시 인메모리 순회** (v4 도메인 결정 #9) — 별도 집계 쿼리 미도입
    - 거부 — DB COUNT 쿼리: 유저당 facade가 작고(축 5개 / 주제 50개 내외) 인메모리 순회로 충분. 별도 쿼리는 코드 복잡도 + 캐시 동기화 부담
    - 한계: facade가 100개 주제 이상으로 커지면 재검토. 현 v1 트래픽 충분
- **공백 알림 채널**
    - 선택: **인앱 인라인 메시지만 (Story 5-1, 5-2)**
    - 거부 — 이메일/푸시: v2. 알림 인프라가 별도 의존성 + 유저 옵트아웃 흐름 필요. v1은 "유저가 facade 진입했을 때만 인지"로 충분
- **공백 알림 강제성**
    - 선택: **유도(suggestion) — 무시해도 기능 제한 없음** ("나중에" 버튼 보존)
    - 거부 — 강제: "주제를 추가하려면 자료를 먼저 연결하세요" 같은 차단은 학습 흐름 단절. 공백 자체가 회고 가치
- **`LearningFacadePersonalizationQuery` 인터페이스 위치**
    - 선택: **LearningFacade BC의 Application Layer Public Read API** (inbound query)
    - 거부 — aisuggestion BC가 LearningFacade Repository 직접 조회: BC 경계 위반 (PACKAGE §6). LearningFacade가 자기 데이터를 어떻게 노출할지 통제권 가짐
    - 거부 — 이벤트 발행으로 비동기 푸시: 컨텍스트는 LLM 호출 시점 정합성이 중요 → pull 방식이 자연
- **공백 우선순위 정렬**
    - 선택: **`NO_MATERIAL` > `PARTIAL` 우선순위 + `displayOrder` 보조 정렬**
    - 거부 — `displayOrder`만: 유저가 우선순위로 둔 상위 주제가 이미 커버됐을 때 공백이 안 보임. 공백 우선 정렬이 본 Epic 의도와 일치

---

## [Story 5-1] 커버리지 공백 시각화

### 사용자 가치

> As a **LearningFacade를 관리하는 유저**,
> I want **어떤 주제가 학습 자료로 커버되지 않는지 한눈에 보길**,
> So that **내 로드맵의 빈 곳이 어디인지 파악하고 자료를 추가할 수 있다.**

### 설명

- 각 주제 옆에 `coverageStatus` 시각 표시: ✅ 자료 있음 / ⚠️ 자료 없음 (`NO_MATERIAL` / `PARTIAL` / `COVERED`)
- LearningFacade 홈 상단에 전체 커버리지 요약 (예: "14개 주제 중 3개 미커버")
- 미커버 주제 있는 축 카드는 상단 경고 뱃지

### 인수 조건 (Given/When/Then)

- [ ] Given LearningFacade 홈 진입, When 미커버 주제 존재, Then 해당 주제 ⚠️ + "자료 없음" 문구
- [ ] Given 전체 커버리지 요약, When 홈 로딩, Then "N개 주제 중 M개 미커버" 형태 요약
- [ ] Given 자료 연결로 커버됨, When 저장, Then 해당 주제 상태 ✅로 즉시 변경 (동일 트랜잭션)
- [ ] *(엣지 케이스 — 완전 커버)* 모든 주제가 커버된 축은 "완전 커버" 상태로 표시

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: 커버리지 집계 로직
- [ ] 디자인 QA 통과 (FE: 시각화 UI)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 비목표

- 커버리지 트렌드 (시간 추이) — v2

### INVEST 점검

- **Independent**: 집계 + UI
- **Negotiable**: 표시 임계는 ADR로 합의
- **Valuable**: 공백 인식의 출발점
- **Estimable**: 집계 + 표시
- **Small**: 5 SP
- **Testable**: 집계 단위 테스트

---

## [Story 5-2] 주제 추가 시 자료 추가 유도

### 사용자 가치

> As a **새로운 주제를 추가한 유저**,
> I want **주제 저장 직후 자료 추가 유도를 받길**,
> So that **새로운 주제가 공백으로 방치되지 않고 바로 학습 자료와 연결되는 흐름이 만들어진다.**

### 설명

- 주제 저장 직후 인라인 메시지: "이 주제를 커버할 자료가 없습니다. 지금 추가하시겠어요?"
- '지금 추가' 클릭 → 자료 등록 폼 (연결 주제 자동 선택)
- '나중에' → 메시지 닫힘, ⚠️ 공백 유지

### 인수 조건 (Given/When/Then)

- [ ] Given 새 주제 저장, When 연결 자료 없음, Then "이 주제를 커버할 자료가 없습니다" 인라인 메시지
- [ ] Given '지금 추가' 클릭, When 자료 등록 폼 열림, Then 연결 주제 필드에 해당 주제 자동 선택
- [ ] Given '나중에' 클릭, When 메시지 닫힘, Then 해당 주제 ⚠️ 상태 표시
- [ ] *(엣지 케이스 — 기존 자료 연결)* 기존 자료에서 해당 주제를 추가 연결할 수 있는 경우 "기존 자료에 연결하기" 옵션도 함께 표시

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: 유도 메시지 노출 조건
- [ ] 디자인 QA 통과 (FE: 인라인 유도 UI)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 비목표

- 이메일·푸시 알림 — v2 (인앱 우선)

### INVEST 점검

- **Independent**: UI 흐름 + 폼 자동 선택
- **Negotiable**: 메시지 카피라이팅
- **Valuable**: 공백 방치 방지
- **Estimable**: 메시지 + 라우팅
- **Small**: 5 SP
- **Testable**: 화면 E2E

---

# [Epic 6] LearningFacade ↔ Deck 자동 연결

## 목표

> LearningFacade에 학습 자료를 등록하는 순간 동명의 Deck이 자동 생성되어 축 → 학습 자료 → Deck → Card의 연결이 끊김 없이 이어진다.

## 배경

- Deck 이름은 학습 자료를 기준으로 한다 — "도메인 주도 설계 Deck", "클린 아키텍처 Deck"
- 유저가 Deck을 별도로 만드는 것이 아니라, 학습 자료를 등록하는 행위 자체가 Deck 생성으로 이어짐
- 이 구조가 완성되어야 "어떤 자료로 공부하면서 어떤 Card를 만들었는가"가 로드맵 위에서 추적됨
- Deck이 LearningFacade 밖에서 독립적으로 만들어지면 축·주제와의 연결이 끊어짐

## 포함 Story

- [ ] Story 6-1. 학습 자료 등록 시 Deck 자동 생성 (이벤트 기반)
- [ ] Story 6-2. 축 카드에서 연결된 Deck 목록 확인

## Epic 인수 시나리오

`유저가 자료 등록` → `LearningFacade BC가 MaterialRegisteredEvent 발행` → `Card BC가 이벤트 수신 → 동명 Deck 자동 생성` → `Deck이 해당 자료가 연결된 축에 귀속` → `자료명 변경 가능 + Deck 이름 동기 (필요 시)` → `축 카드 펼침 시 Deck 목록 + 진행 상태 표시` → `유저가 Deck 클릭 → Deck 상세 이동` → `자료 삭제 시 Deck은 보존 ('자료 미연결' 뱃지)`

## Epic 완료 기준 (DoD)

- [ ] 학습 자료 등록 시 동명의 Deck이 자동 생성된다
- [ ] 자동 생성된 Deck이 해당 자료가 연결된 축에 귀속되어 표시된다
- [ ] LearningFacade 축 카드에서 연결된 Deck 목록을 확인할 수 있다
- [ ] Deck 완료 시 해당 축의 주제 커버리지 상태가 업데이트된다
- [ ] ADR 갱신: "LearningFacade ↔ Deck 이벤트 기반 단방향 연동"
- [ ] 연결된 Story 모두 Done

## 내부 메모 / 제약 사항

- 기술 제약: LearningFacade와 Deck은 이벤트 기반 단방향 연동 (학습 자료 등록 이벤트 → Deck 생성)
- 기획 원칙: Deck 이름은 학습 자료명을 기본값으로 하되 수정 가능
- 기획 원칙: 학습 자료 삭제 시 연결된 Deck은 삭제되지 않고 '자료 미연결 Deck'으로 유지
- 연기된 항목: 주제 단위 Deck 연결 (현재는 축 단위) — v2 / 외부 Deck 허용 — v2 논의

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

- **BC 협력 통신 방식 (ADR007)**
    - 선택: **Spring `@EventListener` 동기 + mutable 이벤트 객체로 결과 통신** — 호출자가 이벤트 객체에서 `deckId`/`deckName` 읽음
    - 거부 — `@TransactionalEventListener(AFTER_COMMIT)` 비동기: 자료 응답에 `409 + DECK_NAME_DUPLICATE`를 줄 수 없음. 응답에 deckId 포함 불가
    - 거부 — `DeckCommandService` 직접 주입: Epic 6 메모의 "이벤트 기반 단방향" 의도와 어긋남. BC 협력 의도가 코드 레벨에서 덜 명시적
- **이벤트 의존 방향**
    - 선택: **단방향 `Deck → LearningFacade`** — 이벤트 정의는 발행 BC(LearningFacade)에 위치, 핸들러는 수신 BC(Deck)의 application 계층
    - 거부 — `LearningFacade → Deck`: LearningFacade가 Deck 도메인을 알아야 함. 책임 역전
    - 거부 — Coordinator Service 신설: 양쪽 BC 둘 다 알아야 해 결합도 더 높음. 본 흐름 한정으로 무게 과함
- **자료 삭제 시 Deck 처리**
    - 선택: **Deck 보존 + `learningMaterialId` null 처리 + '자료 미연결' 뱃지** (`axisId`는 로드맵 추적성을 위해 유지)
    - 거부 — Deck 자동 삭제: 유저가 자료에서 만든 Card 자산이 함께 사라짐. 회고 가치 손실
    - 거부 — 양방향 동기화(Deck 이름 변경 → 자료명 갱신): 단방향 원칙 위반. 자료가 Deck의 진입점이라는 의도와 어긋남
- **Deck UNIQUE(user_id, name) 위반 처리**
    - 선택: **자료 등록 응답에 `409 + DECK_NAME_DUPLICATE` 즉시 반환 + 자료 트랜잭션 전체 롤백** + FE 확인 팝업
    - 거부 — 서버가 자동 suffix 부여 (`"Deck (2)"`): 의도하지 않은 이름이 박힘. 유저가 의식적 선택 필요
- **자료 등록 응답에 Deck 정보 포함**
    - 선택: **응답 DTO에 `deckCreated` / `deckId` / `deckName` 포함** (이벤트 mutable 결과 채널 활용)
    - 거부 — 분리된 후속 GET: 응답 직후 "Deck으로 이동" 버튼이 추가 호출 없이 동작해야 함 (UX 끊김 회피)

---

## [Story 6-1] 학습 자료 등록 시 Deck 자동 생성 (이벤트 기반)

### 사용자 가치

> As a **LearningFacade에 학습 자료를 등록하는 유저**,
> I want **자료를 등록하는 순간 해당 자료명으로 Deck이 자동 생성되길**,
> So that **별도로 Deck을 만들지 않아도 자료 학습을 시작하자마자 Card를 쌓을 공간이 준비된다.**

### 설명

- 학습 자료 저장 시 자동으로 동명의 Deck 생성
- 생성된 Deck은 해당 자료가 연결된 축에 자동 귀속
- Deck 이름 기본값은 자료명. 자료 등록 폼에서 수정 가능
- 유저는 자료 등록 완료 후 "Deck이 생성되었습니다 — 바로 이동할까요?" 안내
- 구현: LearningFacade BC가 `MaterialRegisteredEvent` 발행 → Card BC가 수신 → Deck 생성
- 중복 방지: Deck UNIQUE(user_id, name) 위반 시 확인 팝업

### 인수 조건 (Given/When/Then)

- [ ] Given 학습 자료 등록 + 저장, When 호출, Then 해당 자료명으로 Deck 자동 생성 (이벤트)
- [ ] Given Deck 자동 생성, When 생성, Then 자료가 연결된 축 아래에 Deck 귀속 표시
- [ ] Given 자료 등록 폼에서 Deck 이름 수정, When 저장, Then 수정된 이름으로 Deck 생성
- [ ] Given Deck 생성 직후, When 완료 안내, Then "Deck으로 이동" 버튼 함께 표시
- [ ] *(엣지 케이스 — Deck명 중복)* 동일 자료명이 이미 Deck으로 존재할 때 "동일한 이름의 Deck이 있습니다. 새로 만드시겠어요?" 확인

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: 이벤트 발행 + Deck 생성 흐름
- [ ] 통합 테스트: 등록 → 이벤트 → Deck 생성 + 트랜잭션 경계
- [ ] 디자인 QA 통과 (FE: 자동 생성 안내 + 이동 버튼)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 8 SP

### 비목표

- 양방향 동기화 (Deck 이름 변경 → 자료명 갱신) — 단방향 원칙
- Deck 자동 삭제 (자료 삭제 시) — '자료 미연결' 보존 정책

### INVEST 점검

- **Independent**: 이벤트 + 핸들러
- **Negotiable**: 이벤트 동기/비동기는 ADR (동기 채택)
- **Valuable**: 축 ↔ Deck 연결의 핵심
- **Estimable**: 이벤트 + 핸들러 + 통합 테스트
- **Small**: 8 SP
- **Testable**: 통합 테스트로 전 흐름

### 참고

- 관련 API 스펙: `POST /api/learning-facade/materials` → 내부 이벤트로 `POST /api/decks` 트리거

---

## [Story 6-2] 축 카드에서 연결된 Deck 목록 확인

### 사용자 가치

> As a **LearningFacade를 관리하는 유저**,
> I want **각 축 카드에서 해당 축에 연결된 Deck 목록을 확인하길**,
> So that **이 축을 위해 어떤 자료로 공부하고 있고 얼마나 진행됐는지를 로드맵 위에서 한눈에 볼 수 있다.**

### 설명

- 축 카드 하단에 연결된 Deck 목록 (Deck명 + 진행 상태)
- 진행 상태: 시작 전 / 진행중 / 완료
- Deck 클릭 시 해당 Deck 상세로 이동

### 인수 조건 (Given/When/Then)

- [ ] Given 축 카드 펼침, When 연결 Deck 존재, Then Deck명 + 진행 상태 목록 표시
- [ ] Given Deck 목록에서 특정 Deck 클릭, When 이동, Then 해당 Deck 상세 페이지로
- [ ] Given 연결 Deck 0개, When 축 카드 펼침, Then "등록된 학습 자료가 없습니다" 안내
- [ ] *(엣지 케이스 — 자료 미연결 Deck)* 학습 자료가 삭제되어 자료 미연결 상태인 Deck은 목록에 '자료 미연결' 뱃지와 함께 표시

### Definition of Done

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트: 축별 Deck 목록 조회
- [ ] Repository Slice: 축 → 자료 → Deck 조인 쿼리 인덱스 동작
- [ ] 디자인 QA 통과 (FE: 축 카드 내 Deck 목록 UI)
- [ ] PO 검수 완료
- [ ] 스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 비목표

- Deck 진행률 정교화 (Card 완료율) — v2

### INVEST 점검

- **Independent**: 조회 + UI
- **Negotiable**: 진행 상태 분류는 PO 합의
- **Valuable**: 축 ↔ Deck 가시화
- **Estimable**: 쿼리 + UI
- **Small**: 5 SP
- **Testable**: Slice + 화면

### 의존성

- 선행: Story 6-1

---

## 자가 점검

- **레퍼런스 1:1 대응**: Product/Epic/Story 헤더 `[Product]/[Epic N]/[Story N-M]` 컨벤션. Outcome / 성공 지표 / Scope / Epic 목록 / 제품 DoD 5요소 충족. Epic마다 목표·포함 Story·인수 시나리오·DoD 4요소. Story마다 사용자 가치·설명·G/W/T 인수 조건·DoD·비목표·INVEST 6요소 충족 ✓
- **엣지 케이스 태깅**: 모든 Story에서 `*(엣지 케이스 — 사유)*` 형태로 명시 ✓
- **DoD의 테스트+ADR**: 모든 Story DoD가 단위/슬라이스/통합 테스트 중 해당 항목 포함. Product·Epic DoD에 ADR 항목 명시 (AxisTopic 재설계, TopicMaterial 매핑, coverageStatus 역정규화, RevisionReasonOption DB 관리, 4종 자료 타입, Deck 이벤트 연동, 권장 한도 정책) ✓
- **Spring AI 정합 (LearningFacade = AI 제안의 주 소비자, v1 도메인 직접 비의존 + v2 인프라 재사용 원칙)**: v1 LearningFacade 도메인은 Spring AI(`ChatClient`/`PromptTemplate`/`BeanOutputConverter`)에 직접 의존하지 않는다. v1의 Axis/AxisTopic AI 제안은 `product-aisuggestion.md` Product가 자체 Port(`AxisSuggestionPort`/`AxisTopicSuggestionPort`)로 제공하며, LearningFacade는 제안 결과를 받아 도메인 행위(`addAxis`/`addTopics`)로 흡수만 담당. v2 도입 항목(Concept AI 제안 / 메타데이터 자동 추출 / 의미 기반 중복 감지 / 숙련도 자동 추정 / 단련 이유 자동 추천 / 외부 자료 추천 / 단련 메시지 개인화)은 `product-aisuggestion.md` 인프라(`spring-ai-starter-model-vertex-ai-gemini` + `ChatClient` + `PromptTemplate` + `BeanOutputConverter` + Static Fallback + `LF_SUGGEST_*` 에러 체계 + 관련 ADR)를 그대로 재사용하며, LearningFacade 측은 `MaterialMetadataExtractionPort`·`RevisionReasonSuggestionPort` 등 자체 Port만 신설. 다건 추가의 트랜잭션 경계와 AI 호출 실패 시 빈 목록 graceful degrade는 도메인·Application Service가 책임지는 경계 명확 ✓
- **v1.5 갭 인지형 개인화 컨텍스트 — LearningFacade = 데이터 원천 (Port 정의·LLM 통합은 별도 Product 책임)**: `product-aisuggestion.md` Epic 6의 `PersonalizationContextPort`는 LearningFacade가 제공하는 inbound 쿼리 인터페이스 `LearningFacadePersonalizationQuery` 3종(`findCoverageGaps` / `findWeakTopics` / `findAxisToCardIdsMapping`)으로 커버리지 공백(`NO_MATERIAL`/`PARTIAL`, Epic 5) + 낯섦 숙련도 자료(Epic 4) + 축↔Card 매핑(Epic 2·3·6)을 노출한다. 각 쿼리 read-only / `@Transactional(readOnly=true)` / P95 ≤ 200ms / `axis_topic.coverage_status`·`topic_material.proficiency_level`·`learning_material.deck_id` 인덱스 활용. 본 Product는 데이터만 제공하고, Port 인터페이스 정의·LLM 프롬프트 통합·메트릭 `provider_context` 태깅·갭 인지 응답 검증은 `product-aisuggestion.md` Epic 6 책임. LearningFacade 도메인은 Spring AI / `PersonalizationContextPort`를 직접 import 금지. staleAxes 산정 시 aisuggestion BC Adapter가 `findAxisToCardIdsMapping`으로 받은 cardIds를 Card BC의 `CardStalenessQuery.findLastViewedAtByCardIds(cardIds)`에 전달해 두 BC 데이터를 본 BC가 모르게 결합 (LearningFacade ↔ Card BC 직접 의존 0건 유지) ✓
- **실제 코드 정합**: 도메인 행위 (`LearningFacade.updateConcept`, `addAxis`, `LearningAxis.addTopics`, `reorderTopics`, `AxisTopic.updateName`, `isRefinementSuggested`), VO (`ConceptChangeRecord`, `TopicChangeRecord`), 엔티티 (`LearningFacade`, `LearningAxis`, `AxisTopic`, `LearningMaterial`, `TopicMaterial`, `TopicRevision`, `RevisionReasonOption`), Enum (`CoverageStatus { NO_MATERIAL, PARTIAL, COVERED }`), ErrorCode 접두사 (`LF_*`, `LEARNING_AXIS_*`, `AXIS_TOPIC_*`, `REVISION_REASON_NOT_FOUND`), 도메인 상수 (`RECOMMENDED_AXIS_COUNT_LIMIT=5`, `RECOMMENDED_TOPIC_COUNT_LIMIT=10`, `FOCUS_TOP_N=3`, `REFINEMENT_THRESHOLD=3`) 모두 기존 표기와 일치 ✓
- **측정 가능 지표**: UNIQUE 위반 = 0건 / 이중 방어로 중복 = 0건 / 4종 타입 분류 누락 = 0건 / Deck 자동 생성 누락 = 0건 / displayOrder 외부 주입 = 0건 / 권장 한도 안내 누락 = 0건 등 모두 정량 ✓
