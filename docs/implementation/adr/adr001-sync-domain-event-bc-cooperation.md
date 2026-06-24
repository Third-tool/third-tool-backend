# adr001: BC 간 협력에 동기 도메인 이벤트를 도입한다

**영역**: implementation | **상태**: Accepted | **날짜**: 2026-05-14

> **면접 포인트**
> "@EventListener와 @TransactionalEventListener의 차이는 무엇이고, 왜 동기 이벤트를 선택했나요?"
> → @TransactionalEventListener는 AFTER_COMMIT 이후 비동기로 동작해 같은 트랜잭션 내 롤백 보장이 없다. 이 시나리오는 자료 등록과 Deck 생성이 원자적이어야 하고 응답에 deckId를 즉시 포함해야 했으므로 동기 @EventListener를 선택했다.

---

## 왜 이 결정이 필요했나 (Context)

`LearningMaterial` 등록 시 동명의 `Deck`이 자동 생성되어 축에 귀속되어야 하는 요구사항 (LearningFacade BC → Deck BC 첫 협력).

두 제약이 동시에 걸렸다:

1. **동기 트랜잭션 보장**: 동명 Deck 존재 시 `409 + DECK_NAME_DUPLICATE`를 즉시 반환해야 하며, 자료 저장도 같은 트랜잭션에서 롤백되어야 한다.
2. **BC 결합도 최소화**: 도메인 설계 메모에 "이벤트 기반 단방향 연동"이 명시되어 있고, 자료 등록이 Deck 생성의 진입점임을 코드에 명시적으로 표현하고 싶었다.

추가로 자료 등록 응답이 생성된 Deck의 `deckId`/`deckName`을 포함해야 했다 (FE의 "Deck으로 이동" 버튼에 사용).

---

## 무엇을 결정했나 (Decision)

**Spring `@EventListener` 동기 도메인 이벤트**로 BC 협력을 구현한다.

### 구조

- 이벤트 정의: 발행 BC (`LearningFacade/domain/event/LearningMaterialCreatedEvent`) — 수신 BC(Deck)가 import
- 단방향 의존: `Deck → LearningFacade`
- 핸들러 위치: 수신 BC application 계층 (`Deck/application/event/LearningMaterialCreatedEventHandler`)

### 핵심 선택: @EventListener (동기)

```java
@EventListener  // @TransactionalEventListener 아님 — 호출자 트랜잭션 안에서 동기 실행
public void handle(LearningMaterialCreatedEvent event) {
    Deck deck = createDeck(event);
    event.setResult(deck.getId(), deck.getName());  // 결과를 이벤트 객체에 주입
}
```

이벤트 객체를 **mutable**로 만들어 핸들러가 결과를 설정하면, 호출자가 발행 직후 이벤트 객체에서 `deckId`를 읽어 응답에 포함한다.

---

## 대안과 거부 이유 (Alternatives)

| 대안 | 장점 | 거부 이유 |
|------|------|-----------|
| 직접 주입 (`DeckCommandService` 주입) | 기존 BC 협력 패턴과 일관. 결과 통신 자연스러움 | "이벤트 기반 단방향" 설계 의도와 어긋남. BC 협력 의도가 코드에 덜 명시적 |
| `@TransactionalEventListener(AFTER_COMMIT)` | 일반 도메인 이벤트 표준 | 응답에 409를 즉시 반환 불가. deckId 응답에 포함 불가 |
| Coordinator Service 신설 | 협력 의도를 한 객체에 응집 | 새 추상화 도입. 두 BC를 모두 알아야 해 결합도 더 높음 |
| Immutable 이벤트 + 결과 재조회 | 이벤트 표준 보존 | LearningFacade BC가 DeckRepository 직접 주입 필요 → BC 경계 위반 |

---

## 결과와 트레이드오프 (Consequences)

**긍정적**
- BC 간 협력 의도가 코드 레벨에서 명시적으로 분리
- 동기 처리로 동명 충돌이 자료 응답에 즉시 반영
- 같은 트랜잭션 → 원자성 보장 (자료 저장 후 Deck 저장 실패 시 자료도 함께 롤백)
- Deck BC가 Response DTO를 알 필요 없음 (양방향 의존 회피)

**트레이드오프**
- **이벤트 객체가 mutable** — 일반 도메인 이벤트 표준(immutable record)에서 벗어남. 비동기 처리로 전환 시 결과 통신 메커니즘 재설계 필요
- **새 BC 의존 도입** — Deck → LearningFacade (이벤트 import)
- 핸들러 예외가 호출자(LearningFacade BC)까지 전파 → 스택 트레이스가 BC 경계를 두 번 가로질러 디버깅 시 맥락 파악 필요

---

## 재검토 시점

- BC 협력이 3개 이상 본 패턴을 따르게 된 시점 → 가이드라인으로 표준화
- 비동기 처리 요구 발생 시 → 별도 ADR로 비동기 패턴 분리
- BC 의존 그래프가 복잡해져 사이클 위험이 보이는 시점
