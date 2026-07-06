# Bounded Context In/Out Trace (Living)

> **성격**: living-docs — 항상 최신본. "지금 BC 간에 실제로 무엇이 오가고 있는지"의 지도.
> **관점 차이**:
> - `workflow/topologys/versions/{Nv}/bounded-context.md` — BC 경계·의존 방향 **pin** (바뀌면 안 되는 위상 규칙)
> - **본 문서** — BC 간 **실제 흐름** (이벤트·참조·소유권). 코드가 진실 소스, 여기가 지도.
> **성장 방향**: 이벤트·의존이 늘면 BC별 파일로 분화 (`boundary-trace/learning-facade.md`, `boundary-trace/deck.md` …).
> **핵심 자료**: `ApplicationEventPublisher` / `@EventListener` / cross-BC import.

---

## 1. 원칙 (규범 요약)

- **BC 간 협력 = 동기 도메인 이벤트** (`ApplicationEventPublisher`, ADR007). 비동기 X.
- **도메인 객체 직접 참조 금지**. 다른 BC의 리소스는 **FK id만** 저장 (예: `Deck.axisId: Long`, `Deck.learningMaterialId: Long`).
- **User BC는 소유권 앵커** — 모든 BC가 UserEntity를 소유자로 참조.
- **Aggregate → Repository 직접 호출 금지**. Application Service만 Repository 조율.

---

## 2. BC In/Out 매트릭스 (**M4 반영**)

```
                        받는 쪽 (IN)
                ┌────────┬──────┬──────┬──────┬────────────┬─────────────┐
                │  User  │ Card │ Deck │Review│LearningFa- │UserSchedule │
                │        │      │      │      │  cade      │             │
       ────────┼────────┼──────┼──────┼──────┼────────────┼─────────────┤
       User    │        │      │      │      │            │             │  (소유권 앵커)
       Card    │  🔗    │      │  🔗  │      │  📢🔗      │             │  ← M4 CardViewedEvent (axisId)
       Deck    │  🔗    │      │      │      │            │             │
보내는 Review  │  🔗    │  🔗  │  🔗  │      │  🔗        │             │
쪽 LearningF.  │  🔗    │  🔗  │ 📢🔗 │      │            │             │  ← M4 Card 직접 참조 (axis_id)
(OUT)  Sched.  │  🔗    │  🔗  │      │      │            │             │
       ────────┴────────┴──────┴──────┴──────┴────────────┴─────────────┘

📢 = 도메인 이벤트 (동기)
🔗 = 코드 참조 (FK id 저장 · Application Service 호출 · Repository 조회)
```

**핵심 관찰**:
- **User는 모든 BC의 IN 대상** — 소유권 참조. `UserEntity` 자체는 다른 BC 참조 안 함 (역방향 없음).
- **도메인 이벤트 3건 (M4까지)**: LearningFacade → Deck 2건 (기존) · **Card → LearningFacade 1건 신설 (M4 · CardViewedEvent)**.
- **Card→LearningFacade**: `card.axis_id NOT NULL` (V30) 승격으로 Card가 Axis를 직접 참조. `CardViewedEvent`가 축 스코프 Coverage 재계산 트리거.
- **양방향 참조 없음** — LearningFacade → Card 이벤트 없음. 위상 사이클 부재.
- **M5 Deck 폐기 예정**: Deck BC 관련 참조·이벤트 모두 M5에 정리.

---

## 3. 도메인 이벤트 지도 (동기, 3건 · **M4 CardViewedEvent 신설**)

**전체 3건 상세는 [`event-catalog/events.md`](../event-catalog/events.md) 참조**. 본 문서는 흐름 다이어그램 중심.

### 3.1 `LearningAxisCreatedEvent`

| 항목 | 내용 |
| --- | --- |
| 발행 위치 | `LearningFacade/application/service/LearningFacadeCommandService.java:81` |
| 발행 트리거 | Axis 생성 (`POST /api/v1/learning-facade/axes`) |
| 수신 위치 | `Deck/application/event/LearningAxisCreatedEventHandler.java:31` |
| 수신 방식 | `@EventListener` (동기) — `@TransactionalEventListener` **아님** |
| 실행 시점 | Axis 등록 **트랜잭션 안에서 즉시** |
| 부수효과 | Axis 1개당 Deck 1개 자동 생성 (`Deck.createFromAxis(user, axisId, axisName)`) |
| 멱등 | `deckRepository.existsByAxisIdAndDeletedFalse(axisId)`로 재생성 방지 |
| 실패 시 | Axis 트랜잭션 전체 롤백 (동기 처리 특성) |
| 소요 협력자 | `DeckRepository`, `UserRepository` (User.findById → USER_NOT_FOUND 예외) |
| ADR | ADR007 |

```
POST /api/v1/learning-facade/axes
    │
    ▼
[LearningFacade BC] LearningFacadeCommandService.addAxis
    │
    ├─ facade.addAxis(...)  ── (Aggregate 내부)
    ├─ facadeRepository.save
    └─ eventPublisher.publishEvent(new LearningAxisCreatedEvent(userId, axisId, axisName))
                                          │
                                          ▼ (동기)
                        [Deck BC] LearningAxisCreatedEventHandler.handle
                                          │
                                          ├─ deckRepository.existsByAxisIdAndDeletedFalse(axisId)  → 멱등 체크
                                          ├─ userRepository.findById(userId)  → USER_NOT_FOUND 검증
                                          └─ deckRepository.save(Deck.createFromAxis(...))
```

### 3.2 `LearningMaterialDeletedEvent`

| 항목 | 내용 |
| --- | --- |
| 발행 위치 | `LearningFacade/application/service/LearningMaterialCommandService.java:196` |
| 발행 트리거 | LearningMaterial 삭제 (`DELETE /api/v1/learning-facade/materials/{materialId}`) |
| 수신 위치 | `Deck/application/event/LearningMaterialDeletedEventHandler.java:31` |
| 수신 방식 | `@EventListener` (동기) |
| 실행 시점 | 자료 삭제 **트랜잭션 안에서 즉시**, `materialRepository.delete(material)` **이전**에 발행 |
| 부수효과 | 자료를 참조하는 Deck들의 `learningMaterialId = null` (dirty checking으로 flush) |
| 멱등 | `Deck.markMaterialDeleted()`가 이미 null인 경우 무영향 처리 |
| FK 안전 | Deck.learning_material_id가 자료 참조 중이면 FK violation — 이벤트 순서 중요 (delete 전 발행) |
| Story | Story-005-2, ADR007 |

```
DELETE /api/v1/learning-facade/materials/{materialId}
    │
    ▼
[LearningFacade BC] LearningMaterialCommandService.deleteMaterial
    │
    ├─ material.softDelete()  ── (Aggregate)
    ├─ eventPublisher.publishEvent(new LearningMaterialDeletedEvent(userId, materialId))
    │                                        │
    │                                        ▼ (동기, 삭제 전에 실행)
    │                    [Deck BC] LearningMaterialDeletedEventHandler.handle
    │                                        │
    │                                        ├─ deckRepository.findByLearningMaterialIdAndDeletedFalse(materialId)
    │                                        └─ Deck.markMaterialDeleted() * N (dirty checking)
    │
    └─ materialRepository.delete(material)   ── (이 시점엔 참조 없음 → FK 안전)
```

---

## 4. BC별 상세 지도

### 4.1 User BC (소유권 앵커)

- **IN (User가 받는 요청)**: 로컬 인증 · 소셜 인증 · 프로필 CRUD · JWT 재발급.
- **OUT (User가 다른 BC로 보내는 것)**: **없음**. UserEntity 자체는 다른 BC 참조 안 함.
- **다른 BC가 User를 참조**: Card / Deck / Review / LearningFacade / UserSchedule / Common(Security).
  - 참조 방식: `UserEntity` 도메인 객체 직접 참조 (Controller `@AuthenticationPrincipal`), 또는 `UserRepository.findById(userId)`.
  - 각 BC의 Aggregate Root는 `user_id`를 FK 컬럼으로 소유.

### 4.2 Card BC — **M4 재편**

- **IN (외부 참조)**:
  - LearningFacade: `LearningFacadeController.getAxisCards(axisId)` — 축 하위 카드 조회 (`GET /axes/{axisId}/cards`) · **M4에서 `card.axis_id` 직접 조회로 재작성**.
  - Review: `ReviewCommandService`, `ReviewQueryService`, `CardReview`, `CardVisibleContent` — 리뷰 세션이 카드를 대상으로 함.
  - UserSchedule: `UserScheduleConfig`, `LearningMode` — **M4 재편**: budget 매핑 제거 · `currentMode(userId)` 조회로 축소 · Card `createdMode` 하이브리드 계산에 사용.
- **OUT** (**M4 신설**):
  - LearningFacade **이벤트 발행 (1건)**: `CardViewedEvent(userId, cardId, axisId)` — Card 열람 시 Coverage 재계산 축 스코프 트리거.
- **BC 내부 참조**: Deck(카드가 덱에 속함, `deck_id` FK · M5 폐기 예정), Tag(시스템 전역, `card_tag` 매핑), **LearningAxis(`card.axis_id NOT NULL` M4 신설)**.

### 4.3 Deck BC

- **IN (외부 참조)**:
  - LearningFacade: `LearningFacadeQueryService`, `LearningFacadeResponse` (Facade 조회 시 덱 정보 조합).
  - Review: `ReviewSessionRepositoryImpl`, `ReviewCommandService`, `ReviewSession` (덱 단위 리뷰 세션).
  - Card: `CardCommandService`, `CardExpiryBatchService`, `Card.java` (카드가 덱에 속함).
- **OUT (Deck이 능동적으로 협력)**:
  - LearningFacade **이벤트 수신** (2건): `LearningAxisCreatedEvent`, `LearningMaterialDeletedEvent`.
  - User **조회 호출**: `UserRepository.findById(userId)` (이벤트 핸들러 안에서).
- **참조 필드**: `Deck.axisId: Long` (LearningAxis 참조), `Deck.learningMaterialId: Long` (LearningMaterial 참조) — 모두 **id만**, 도메인 객체 X.

### 4.4 Review BC

- **IN**: 없음. Review는 진입 컨트롤러만 있고 다른 BC가 Review를 참조하지 않음.
- **OUT**: Card, Deck, LearningFacade, User — 모두 조회·참조 (이벤트 발행 X).
- **참조 필드**: `ReviewSession.userId`, `deckId`, `CardReview.cardId`.

### 4.5 LearningFacade BC (대형 · 발행자 · **M4 Card 이벤트 수신 추가**)

- **IN** (**M4 신설**): Card BC — `CardViewedEvent(axisId)` 수신 → `CoverageRecalculator.recalculateByAxis(axisId)` 트리거.
- **OUT**:
  - **이벤트 발행** (2건): `LearningAxisCreatedEvent`, `LearningMaterialDeletedEvent` → Deck BC 수신 (M5에 Deck 폐기 예정).
  - Deck: `DeckQueryService`를 조회 조합에 사용 (`LearningFacadeQueryService`가 Deck 결과와 병합).
  - Review: `ReviewQueryService`가 LearningFacade 결과 참조.
- **내부 규모**: LearningFacade · LearningLayer · LearningAxis · AxisTopic · LearningMaterial · TopicMaterial · AxisRoadmapNode · AxisSelection · AxisSelectionNode · TopicRevision · RevisionReasonOption · TopicDeletionRecord · Suggestion 6-Port + **Static Adapters (4-role catalog · M4)**.

### 4.6 UserSchedule BC

- **IN**: 없음.
- **OUT**: Card (`LearningMode`, `UserScheduleConfig`가 카드 budget과 연결).
- **참조 필드**: `UserScheduleConfig.userId`.

---

## 5. 소유권 참조 (User 앵커)

```
UserEntity ──┬──▶ Card         (Card.userId)
             ├──▶ Deck         (Deck.userId)
             ├──▶ ReviewSession (session.userId)
             ├──▶ LearningFacade (facade.userId, UNIQUE)
             ├──▶ UserScheduleConfig (config.userId)
             └──▶ RefreshEntity (Common/Security)
```

각 BC의 Aggregate Root는 `isOwnedBy(userId)`류 도메인 메서드로 소유권 검증. Application Service가 호출.

---

## 6. 규칙 위반 신호 · 감시 포인트

작업 중 다음이 보이면 topology 위반 가능성 → 리뷰 필요:

| 신호 | 의심 위반 |
| --- | --- |
| Aggregate 안에서 `Repository` 필드 주입 | Aggregate → Repository 직접 호출 (금지) |
| BC-A의 도메인이 BC-B의 도메인 클래스를 필드로 소유 | 도메인 객체 직접 참조 (FK id만 허용) |
| 새 `@EventListener`를 발견했는데 `@Async` 사용 | 비동기 이벤트 금지 (ADR007) |
| 같은 이벤트를 두 BC가 수신 | fan-out — 의도 명확한지 확인 (현재 없음) |
| 새 BC에서 다른 BC로 Repository 직접 호출 | 이벤트 통해서만 협력. Repository 크로스 접근은 예외적 |
| 위 매트릭스에 없는 새 참조 | 본 문서·topology 갱신 필요 |

---

## 7. 참조

- 진실 소스: `src/main/java/com/example/thirdtool/*/application/event/*.java`, `*/domain/event/*.java`
- 규범: `docs/PACKAGE.md` (BC 의존 규칙), `.claude/rules/conventions.md` §1.7 (책임 분리)
- ADR: ADR007 (도메인 이벤트 동기 처리)
- 관련 topology: `workflow/topologys/versions/{Nv}/bounded-context.md` (BC 경계 pin)
- 관련 living-docs: `architecture-system-design/architecture.md` (BC 구성 · 4-레이어), `erd/erd.md` (FK 관계)

*최신 갱신: 2026-07-21 · **M4 반영** — CardViewedEvent 신설 (Card → LearningFacade) · Card `axis_id` 직접 참조 · 이벤트 3건 · 발행 BC 2 · 수신 BC 2 · role catalog 4종*
