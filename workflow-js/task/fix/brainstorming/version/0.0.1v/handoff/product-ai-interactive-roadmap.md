# [Handoff] product-ai-interactive-roadmap — addAxis() 저장 시 Deck 자동 생성 사이드이펙트
> issue2b + fix-axis-deck-refactor 결론 → `sdd/in-progress/product-ai-interactive-roadmap.md` 계획 수정 권고 (2026-06-30)

**출처**: `../issue2b-facade-multicontext-deck-arc-report.md` §5 · `../fix-axis-deck-refactor.md`
**대상 product**: `workflow/task/pes/workspectrum/sdd/in-progress/product-ai-interactive-roadmap.md`

---

## 변경 사실 요약

`fix-axis-deck-refactor.md` (Story 1~3)가 실행되면 `LearningFacadeCommandService.addAxis()`에 **이벤트 발행이 추가**된다:

```java
// addAxis() 수정 후
facadeRepository.save(facade);
eventPublisher.publishEvent(
    new LearningAxisCreatedEvent(userId, axis.getId(), axis.getName())
);
// → LearningAxisCreatedEventHandler → Deck.createFromAxis() → DeckRepository.save()
```

즉 **`addAxis()` 호출 = Deck 자동 생성**이 된다.

---

## product-ai-interactive-roadmap에 미치는 영향

product-ai-interactive-roadmap.md는 consensus 검증 후 저장 시 `LearningFacade.addAxis()`를 호출한다:

```
product-ai-interactive-roadmap.md §설계결정:
"합의된 축/토픽을 저장할 때 LearningFacade.addAxis() / LearningAxis.addTopics() 등
 기존 Aggregate 행위를 호출. JPA로 우회 저장하거나 별도 SQL은 금지."
```

이 설계 결정은 유효하다. 단, fix 이후 `addAxis()` 호출에 Deck 생성 사이드이펙트가 붙는다는 점을 인지해야 한다.

### 영향 1 — Consensus 저장 시 Deck N개가 자동 생성됨

AI 상호작용 결과로 축 3개를 합의·저장하면:
- 기존: `LearningAxis` 3개만 생성
- 변경 후: `LearningAxis` 3개 + `Deck` 3개 동시 생성

Deck 생성은 멱등 처리됨 (`existsByAxisIdAndDeletedFalse` 체크):
- 같은 axisId로 이미 Deck이 있으면 재생성 안 함 → 재시도 safe

### 영향 2 — 인터랙션 세션 중 "임시 저장" 흐름 존재 시 주의

product-ai-interactive-roadmap이 "초안 단계에서 임시로 axis를 저장하고 나중에 확정하는" 패턴을 갖는다면:
- 임시 저장 시점에도 Deck이 생성됨
- 임시 axis를 삭제할 경우 → Deck은 독립 보존 (0.0.1v 정책: Axis 삭제해도 Deck 남음, `fix-axis-deck-refactor.md` §5)
- 고아 Deck이 생길 수 있음 — 설계에서 "draft 단계 axis는 도메인 저장을 미룬다"는 정책이 있다면 문제 없음

`product-ai-interactive-roadmap.md §설계결정`: "AI 결과를 그대로 LearningFacade에 쓰는 경로는 닫는다. 저장은 항상 사용자가 한 번 손댄 결과만 통과."

→ **consensus 확정 후에만 `addAxis()` 호출이면** Deck 생성은 의도된 시점에 발생 → 문제 없음.

### 영향 3 — addAxis 응답에 deckId 포함 여부 (결정 필요)

`fix-axis-deck-refactor.md` §6 결정 1:
> "addAxis() 응답에 deckId를 포함할 것인가? (A) 포함 / (B) 미포함"

product-ai-interactive-roadmap이 consensus 저장 후 FE에 deckId를 반환하고 싶다면 **(A) 포함**을 선택해야 한다. 그렇지 않으면 FE가 별도로 Deck을 조회해야 한다.

---

## 계획 수정 권고

### 권고 1 — consensus 저장 플로우 검증 포인트 추가

product-ai-interactive-roadmap.md의 Epic/Story 중 "합의 결과 저장" Story에 다음을 추가:

> **검증 항목**: consensus 저장 후 `addAxis()` 호출 횟수만큼 Deck이 생성되는지 확인.
> - Deck 이름 = 저장된 Axis.name 과 일치하는지
> - 동일 axisId 재저장 시 Deck 중복 생성 없는지 (멱등 확인)

### 권고 2 — addAxis() 응답 deckId 포함 여부 결정

`fix-axis-deck-refactor.md` §6 결정 1에 참여해서 이 product의 FE 흐름에 맞는 선택을 결정할 것.

| 선택 | 이 product FE 영향 |
|------|-------------------|
| (A) deckId 포함 | consensus 저장 응답에서 각 axis의 deckId를 받아 FE가 즉시 Deck으로 안내 가능 |
| (B) deckId 미포함 | 저장 후 FE가 별도 GET으로 Deck 조회 필요 |

### 권고 3 — Draft 단계 axis 저장 정책 명시 (있다면)

product-ai-interactive-roadmap에 "초안 단계 임시 저장" 패턴이 있다면:
- draft axis는 `addAxis()` 를 **호출하지 않고** 인터랙션 세션 도메인(`RoadmapDraft`)에만 보관
- 확정 후에만 `addAxis()` 호출 → Deck 생성
- 이 방식이면 고아 Deck 발생 없음

product-ai-interactive-roadmap.md 현행 설계 ("저장은 항상 사용자가 한 번 손댄 결과만 통과")와 이미 정합 → 설계 변경 불필요, 확인만 필요.

---

## 조율 필요 항목

| 항목 | 결정 필요 | 영향 |
|------|-----------|------|
| consensus 저장 = `addAxis()` 호출 시점이 확정 후인지 확인 | ai-interactive-roadmap 설계 검토 | 고아 Deck 방지 |
| `addAxis()` 응답 deckId 포함 여부 | fix-axis-deck-refactor §6 결정 1 | FE 흐름 |
| Draft axis가 `RoadmapDraft`에만 존재하다가 확정 시에만 도메인 저장되는지 확인 | ai-interactive-roadmap 세션 플로우 | 멱등성 |

---

## 참조

- 이벤트 변경 원문: `../fix-axis-deck-refactor.md` §4.4 (addAxis 수정) · §6 (미결 결정)
- Axis→Deck 설계 원문: `../issue2b-facade-multicontext-deck-arc-report.md` §5
- 대상 product: `workflow/task/pes/workspectrum/sdd/in-progress/product-ai-interactive-roadmap.md` — 설계결정 "합의된 축/토픽 저장 시 addAxis() 호출"
- 고아 Deck 정책: `../fix-axis-deck-refactor.md` §5 "0.0.1v: Deck은 독립 보존"
