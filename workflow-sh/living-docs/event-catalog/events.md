# Domain Event Catalog (Living)

> **성격**: living-docs — 항상 최신본. 현재 시스템에서 발행·수신되는 도메인 이벤트 카탈로그.
> **성장 방향**: 이벤트가 늘면 발행 BC별 파일로 분화 (`event-catalog/learning-facade.md` …).
> **관련 규범**: [ADR007](../../../docs/adr/ADR007.md) (BC 간 협력 = 동기 도메인 이벤트).
> **관련 living-docs**: [`boundary-trace/boundary.md`](../boundary-trace/boundary.md) (BC 간 전체 흐름).

---

## 1. 이벤트 규약 (ADR007)

| 항목 | 규칙 |
| --- | --- |
| **자료구조** | Java `record` — immutable. mutable 필드 · setter 금지 |
| **패키지** | `{발행 BC}/domain/event/` |
| **발행 API** | `ApplicationEventPublisher.publishEvent(new XxxEvent(...))` |
| **수신 API** | `@EventListener` (Spring). `@Async` **금지**. `@TransactionalEventListener` **금지** |
| **실행 모델** | **동기** — 발행자 트랜잭션 안에서 즉시 실행. 핸들러가 throw하면 발행자 트랜잭션도 전체 롤백 |
| **멱등** | 핸들러가 재수신 안전하게 설계 (`existsBy...` 사전 체크, 상태 no-op 처리) |
| **결과 통신** | 핸들러 → 발행자 결과 반환 **불가** (event 반환값 없음). 결과가 필요하면 이벤트 X, 직접 Service 호출 |
| **필드 명명** | id는 `xxxId` (Long) — 도메인 객체 참조 금지 |

**왜 동기인가**: BC 간 참조를 명시적으로 유지하되 트랜잭션 원자성을 확보. 비동기는 재시도·순서·실패 복구 복잡도가 폭증하며 v1 요구사항 대비 과잉.

**왜 record인가**: 이벤트는 사실 통지 — 상태·행위 없음. immutable record가 최소 표현. 값 비교·직렬화·테스트 fixture 작성 편의.

---

## 2. 현재 이벤트 (3건 · **M4에 CardViewedEvent 신설**)

### 2.1 `LearningAxisCreatedEvent`

| 항목 | 값 |
| --- | --- |
| **소속 BC** | LearningFacade |
| **파일** | `LearningFacade/domain/event/LearningAxisCreatedEvent.java` |
| **필드** | `Long userId`, `Long axisId`, `String axisName` |
| **발행자** | `LearningFacade/application/service/LearningFacadeCommandService.java:81` |
| **발행 트리거** | `POST /api/v1/learning-facade/axes` — Axis 생성 트랜잭션의 저장 직후 |
| **수신자** | Deck BC — `Deck/application/event/LearningAxisCreatedEventHandler.java` |
| **수신 부수효과** | Axis 1개당 Deck 1개 자동 생성 (`Deck.createFromAxis(user, axisId, axisName)`) |
| **멱등** | `deckRepository.existsByAxisIdAndDeletedFalse(axisId)`로 재생성 방지. no-op |
| **실패 시** | Axis 트랜잭션 전체 롤백 (동기 특성). `USER_NOT_FOUND` 예외도 포함 |
| **협력자** | `DeckRepository`, `UserRepository` |
| **관련 결정** | [ADR007](../../../docs/adr/ADR007.md), [ADR021](../../../docs/adr/ADR021-axis-deck-full-integration.md) |
| **비고** | Fix-Story 2 이후 addAxis 응답에 deckId 미포함 — record가 결과 통신 없이 사실 통지만 함 |

```java
public record LearningAxisCreatedEvent(
        Long userId,
        Long axisId,
        String axisName
) {}
```

**흐름**:
```
LearningFacadeCommandService.addAxis
    ├─ facade.addAxis(...)
    ├─ facadeRepository.save
    └─ eventPublisher.publishEvent(new LearningAxisCreatedEvent(userId, axisId, axisName))
                                      │
                                      ▼ (동기)
        LearningAxisCreatedEventHandler.handle
            ├─ existsByAxisIdAndDeletedFalse(axisId)  → 멱등
            ├─ userRepository.findById(userId)  → USER_NOT_FOUND
            └─ deckRepository.save(Deck.createFromAxis(...))
```

---

### 2.2 `LearningMaterialDeletedEvent`

| 항목 | 값 |
| --- | --- |
| **소속 BC** | LearningFacade |
| **파일** | `LearningFacade/domain/event/LearningMaterialDeletedEvent.java` |
| **필드** | `Long userId`, `Long materialId` |
| **발행자** | `LearningFacade/application/service/LearningMaterialCommandService.java:196` |
| **발행 트리거** | `DELETE /api/v1/learning-facade/materials/{materialId}` — 물리 삭제 **직전** |
| **수신자** | Deck BC — `Deck/application/event/LearningMaterialDeletedEventHandler.java` |
| **수신 부수효과** | 자료를 참조하는 Deck들의 `learningMaterialId = null` 세팅 (dirty checking) |
| **멱등** | `Deck.markMaterialDeleted()`가 이미 null이면 무영향 |
| **실패 시** | 자료 삭제 트랜잭션 전체 롤백 |
| **FK 안전** | Deck.learning_material_id가 자료 참조 중이면 FK violation — 이벤트를 delete **전에** 발행해 참조 해제 후 삭제 |
| **관련 결정** | [ADR007](../../../docs/adr/ADR007.md), Story-005-2 |
| **비고** | Fix-Story 1 이후 새 Deck의 `learningMaterialId`는 항상 null — 본 이벤트는 **레거시 Deck(V7 이전 흐름)에만 영향**. 후속 Story에서 컬럼·이벤트·핸들러 일괄 제거 검토 |

```java
public record LearningMaterialDeletedEvent(Long userId, Long materialId) {}
```

**흐름**:
```
LearningMaterialCommandService.deleteMaterial
    ├─ material.softDelete()
    ├─ eventPublisher.publishEvent(new LearningMaterialDeletedEvent(userId, materialId))
    │                                    │
    │                                    ▼ (동기, delete 전)
    │      LearningMaterialDeletedEventHandler.handle
    │          ├─ findByLearningMaterialIdAndDeletedFalse(materialId)
    │          └─ Deck.markMaterialDeleted() * N (dirty checking)
    │
    └─ materialRepository.delete(material)   ── (참조 해제 후 → FK 안전)
```

---

## 3. 발행·수신 매트릭스 (**M4 반영**)

```
                             수신
                    ┌────────┬──────┬──────┬───────┬────────┬─────────────┐
                    │  User  │ Card │ Deck │Review │Learn.F.│UserSchedule │
       ─────────────┼────────┼──────┼──────┼───────┼────────┼─────────────┤
       User         │        │      │      │       │        │             │
       Card         │        │      │      │       │  📢1   │             │  ← M4 신규 (CardViewedEvent · axisId)
발행   Deck         │        │      │      │       │        │             │
       Review       │        │      │      │       │        │             │
       Learning.F.  │        │      │ 📢2  │       │        │             │
       UserSchedule │        │      │      │       │        │             │
       ─────────────┴────────┴──────┴──────┴───────┴────────┴─────────────┘

       📢2 = 이벤트 2건 (LearningAxisCreatedEvent, LearningMaterialDeletedEvent)
       📢1 = 이벤트 1건 (CardViewedEvent · M4 LT E4 신설)
```

**관찰**:
- **M3까지**: LearningFacade → Deck 한 방향.
- **M4에 신설**: Card → LearningFacade (Coverage 재계산 축 스코프 트리거).
- 이벤트 발행 BC = 2개 (Card · LearningFacade), 수신 BC = 2개 (LearningFacade · Deck).
- fan-out (같은 이벤트를 여러 BC가 수신) **없음**.
- 사이클 (A→B→A) **없음** — Card → LearningFacade 단방향, LearningFacade → Card 이벤트 없음.
- **주의**: `card.deck_id` FK도 여전히 존재 (M5 Deck 폐기 예정). `card.axis_id`가 M4 주 참조.

---

### 2.3 `CardViewedEvent` (**M4 신설 · LT E4 S4-5**)

| 항목 | 값 |
| --- | --- |
| **소속 BC** | Card |
| **파일** | `Card/domain/event/CardViewedEvent.java` |
| **필드** | `Long userId`, `Long cardId`, `Long axisId` (M4에서 기존 `topicId` → `axisId`로 변경) |
| **발행자** | `Card/domain/model/Card.recordView()` — Application Service가 dispatch |
| **발행 트리거** | Card 열람 (`POST /api/v1/reviews/{sessionId}/comparing` 등 view 기록 액션) |
| **수신자** | LearningFacade BC — `LearningFacade/application/event/CardViewedEventHandler.java` |
| **수신 부수효과** | `CoverageRecalculator.recalculateByAxis(axisId)` 트리거 · Layer coverage summary 재조정 |
| **멱등** | Coverage 재계산은 동일 axisId 재수신에도 idempotent (전량 재계산 대신 대상 axis만 재조회) |
| **실패 시** | Card view 트랜잭션 전체 롤백 (동기 특성) — Coverage 갱신 실패 시 view 기록도 중단 |
| **협력자** | `LearningAxisRepository`, `TopicMaterialRepository`, `CoverageRecalculator` |
| **관련 결정** | [ADR007](../../../docs/adr/ADR007.md), M4 LT E4 (Card→Axis 직접 매핑) |
| **비고** | M3까지는 topic 스코프 트리거였음. M4 LT E4에서 축 스코프로 이관. `card.axis_id NOT NULL` 승격(V30)이 선행 |

```java
public record CardViewedEvent(
        Long userId,
        Long cardId,
        Long axisId
) {}
```

**흐름**:
```
POST /api/v1/reviews/{sessionId}/comparing
    │
    ▼
[Review BC] ReviewCommandService.recordView
    │
    ├─ card.recordView()  ── (Aggregate 내부 · viewCount++, lastViewedAt 갱신)
    ├─ cardRepository.save
    └─ eventPublisher.publishEvent(new CardViewedEvent(userId, cardId, axisId))
                                      │
                                      ▼ (동기)
        [LearningFacade BC] CardViewedEventHandler.handle
            ├─ learningAxisRepository.findById(axisId)  → LEARNING_AXIS_NOT_FOUND
            ├─ coverageRecalculator.recalculateByAxis(axisId)
            └─ (Layer.progressStatus M5 파생 트리거 · M5 LT E6 S6-4 예정)
```

---

## 4. 폐기 · 계보 참고

| 이전 이벤트 | 현재 상태 | 계보 |
| --- | --- | --- |
| `LearningMaterialCreatedEvent` (Story 5-1) | 폐기 — 자료 등록 시 Deck 자동 생성 흐름이 Fix-Story 1~4로 폐기됨 | Deck 생성 진입점이 Axis 이벤트로 유일화됨 (ADR021) |
| ~~자료 등록 흐름의 Deck 자동 생성~~ | Fix-Story 1~4로 폐기 | 축=덱 1:1 정책으로 대체 |
| `CardViewedEvent` (M3 이전 · `topicId`) | **M4에서 `axisId`로 필드 재편** | Card→Axis 직접 매핑(V28~V30) 연동 · Coverage 재계산 축 스코프 이관 |
| Deck BC 관련 이벤트 (LearningAxisCreatedEvent · LearningMaterialDeletedEvent) | **M5 Deck BC 폐기 예정** | Deck 자체가 없어지면 관련 이벤트도 폐기 (M5 LT E5 완주 후) |

---

## 5. 새 이벤트 신설 절차

1. **필요성 검증** — 협력하는 두 BC가 있고, 발행자가 결과를 몰라도 되고, 트랜잭션 원자성이 필요한가? 이 셋이 모두 예 → 이벤트 후보.
   - 결과가 필요하면 이벤트 X (Service 직접 호출).
   - BC 내부 협력이면 이벤트 X (Aggregate 메서드).
2. **record 정의** — `{발행 BC}/domain/event/{IntentName}Event.java`.
   - id 필드는 Long (`userId`, `xxxId`).
   - 도메인 객체 참조 금지.
   - 이름: `{과거형 동사}Event` (예: `AxisCreatedEvent`, `MaterialDeletedEvent`).
3. **발행** — Application Service에서 `ApplicationEventPublisher.publishEvent(...)`. 항상 domain state 변경 **직후** (또는 물리 delete **직전** — FK 안전).
4. **핸들러 작성** — `{수신 BC}/application/event/{EventName}Handler.java`.
   - `@Component + @EventListener` (동기).
   - 멱등 보장 로직 (`existsBy...`, 상태 no-op).
   - 실패 시 예외 throw → 발행자 트랜잭션 롤백.
5. **문서화** — 본 카탈로그(§2)에 새 이벤트 항목 추가. `boundary-trace/boundary.md` 매트릭스도 갱신.
6. **ADR 판단** — 새로운 협력 패턴이 나오면 ADR007 amended 또는 신규 ADR.

---

## 6. 감시 포인트

작업 중 다음이 보이면 규약 위반 가능성:

| 신호 | 의심 위반 |
| --- | --- |
| 이벤트에 `@Async` | ADR007 위반. 동기 강제 |
| 이벤트에 `@TransactionalEventListener` | ADR007 위반. 트랜잭션 원자성 상실 |
| 이벤트 record에 mutable 필드 · setter | immutable 규약 위반 |
| 이벤트 필드에 도메인 객체 참조 (예: `LearningAxis axis`) | id만 허용 |
| 핸들러가 여러 BC의 Repository를 조합 호출 | Application Service 성격 — 이벤트가 아니라 Service 호출로 재설계 |
| 발행자가 이벤트 결과에 의존 (예: `event.getResult()`) | 이벤트 반환값 없음. 재설계 |

---

## 7. 참조

- 규범: [ADR007](../../../docs/adr/ADR007.md), [ADR021](../../../docs/adr/ADR021-axis-deck-full-integration.md)
- 진실 소스: `src/main/java/com/example/thirdtool/*/domain/event/*.java`, `*/application/event/*Handler.java`
- 관련 living-docs: [`boundary-trace/boundary.md`](../boundary-trace/boundary.md), [`architecture-system-design/architecture.md`](../architecture-system-design/architecture.md), [`decisions/decisions.md`](../decisions/decisions.md)

*최신 갱신: 2026-07-21 · **M4 반영** — CardViewedEvent 신설 (Card → LearningFacade) · 이벤트 3건 · 발행 BC = 2 · 수신 BC = 2 · Coverage 재계산 축 스코프 이관*
