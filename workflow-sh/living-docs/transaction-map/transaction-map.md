# Transaction Map (Living)

> **성격**: living-docs — 항상 최신본. 지금 각 Application Service의 `@Transactional` 위치·성격 지도.
> **성장 방향**: Service가 많아지면 BC별 파일로 분화 (`transaction-map/card.md`, `transaction-map/learning-facade.md` …).
> **관련 topology**: `workflow/topologys/transaction/v1-transaction.md` (경계·전파·isolation 규칙 pin).
> **원본**: `src/main/java/com/example/thirdtool/*/application/service/*Service.java` (@Transactional 어노테이션).

---

## 규칙 요약 (topology 발췌)

- `@Transactional`은 **Application Service 층에만**.
- Query Service는 항상 `@Transactional(readOnly = true)`.
- Command Service는 default `@Transactional` (readOnly 미지정).
- 전파는 REQUIRED 기본. `REQUIRES_NEW`는 이력·감사 append 용도만.
- isolation READ_COMMITTED 기본.
- OSIV=false 고정.
- 동기 이벤트(`@EventListener`)는 발행자 트랜잭션에 참여.

---

## Service 카탈로그 (24개)

### Card BC (5개)
| Service | 성격 | 전파·isolation | 협력 |
| --- | --- | --- | --- |
| `CardCommandService` | Command (@Transactional) | REQUIRED / RC | Deck.markInProgress·recalculateProgressStatus 호출 (같은 트랜잭션) |
| `CardQueryService` | Query (readOnly) | REQUIRED / RC | — |
| `CardExpiryBatchService` | 배치 Command | REQUIRED / RC | 만료 카드 archive 처리, dirty checking |
| `TagCommandService` | Command | REQUIRED / RC | find-or-create 태그 |
| `TagQueryService` | Query (readOnly) | REQUIRED / RC | — |

### Deck BC (3개 · Application Service + Domain Service)
| Service | 성격 | 전파·isolation | 협력 |
| --- | --- | --- | --- |
| `DeckCommandService` | Command | REQUIRED / RC | `softDeleteByAxisId` — LearningFacade가 Axis 삭제 시 조율 호출 |
| `DeckQueryService` | Query (readOnly) | REQUIRED / RC | `findAxisNamesByIds` 배치 조회 (LearningFacadeQueryService용) |
| `DeckHierarchyService` | Domain Service | (트랜잭션 참여) | 계층 depth 계산 · 순환 참조 검증 |

### Deck BC — 이벤트 핸들러 (2개)
| Handler | 이벤트 | 트랜잭션 참여 |
| --- | --- | --- |
| `LearningAxisCreatedEventHandler` | `LearningAxisCreatedEvent` (LF발행) | `@EventListener` — LF 트랜잭션 참여, 실패 시 rollback 전파 |
| `LearningMaterialDeletedEventHandler` | `LearningMaterialDeletedEvent` (LF발행) | 동상 |

### Review BC (2개)
| Service | 성격 | 전파·isolation | 협력 |
| --- | --- | --- | --- |
| `ReviewCommandService` | Command | REQUIRED / RC | Card.recordView·archive · Deck.recalculateProgressStatus |
| `ReviewQueryService` | Query (readOnly) | REQUIRED / RC | Card/Deck/LF 조회 조합 |

### LearningFacade BC (8개 · 대형 BC)
| Service | 성격 | 전파·isolation | 협력 |
| --- | --- | --- | --- |
| `LearningFacadeCommandService` | Command | REQUIRED / RC | `LearningAxisCreatedEvent` 발행 (동기) |
| `LearningFacadeQueryService` | Query (readOnly) | REQUIRED / RC | Deck 조합 (`DeckQueryService.findByAxisIds`) |
| `LearningMaterialCommandService` | Command | REQUIRED / RC | `LearningMaterialDeletedEvent` 발행 (delete **전**) |
| `AxisRoadmapNodeCommandService` | Command | REQUIRED / RC | — |
| `AxisRoadmapNodeQueryService` | Query (readOnly) | REQUIRED / RC | — |
| `AxisSelectionCommandService` | Command | REQUIRED / RC | — |
| `AxisSelectionQueryService` | Query (readOnly) | REQUIRED / RC | — |
| `TopicRevisionQueryService` | Query (readOnly) | REQUIRED / RC | — |
| `SuggestionAppService` | Command · AI 조율 | REQUIRED / RC | AI Port 호출, 실패 시 fallback (ADR010) |

### User BC (2개 + 인증)
| Service | 성격 | 전파·isolation | 협력 |
| --- | --- | --- | --- |
| `UserCommandService` | Command | REQUIRED / RC | 로컬·소셜 통합 |
| `UserQueryService` | Query (readOnly) | REQUIRED / RC | — |
| `JwtService` | Command · 인증 | REQUIRED / RC | Refresh Token whitelist |

### UserSchedule BC (2개)
| Service | 성격 | 전파·isolation | 협력 |
| --- | --- | --- | --- |
| `UserScheduleCommandService` | Command | REQUIRED / RC | `UserScheduleConfigHistoryAppender` 이력 append |
| `UserScheduleQueryService` | Query (readOnly) | REQUIRED / RC | — |

---

## 이벤트 트랜잭션 흐름 (2건)

### `LearningAxisCreatedEvent`
```
LearningFacadeCommandService.addAxis (@Transactional, REQUIRED)
  │ facade.addAxis(...)
  │ facadeRepository.save
  │ eventPublisher.publishEvent(LearningAxisCreatedEvent)
  │     │
  │     ▼ (같은 트랜잭션)
  │  Deck.LearningAxisCreatedEventHandler.handle (@EventListener)
  │     ├ existsByAxisIdAndDeletedFalse (멱등)
  │     ├ userRepository.findById
  │     └ deckRepository.save (Deck.createFromAxis)
  │
  └ (commit or rollback 함께)
```

### `LearningMaterialDeletedEvent`
```
LearningMaterialCommandService.deleteMaterial (@Transactional, REQUIRED)
  │ material.softDelete()
  │ eventPublisher.publishEvent(LearningMaterialDeletedEvent)  ← delete 이전
  │     │
  │     ▼ (같은 트랜잭션)
  │  Deck.LearningMaterialDeletedEventHandler.handle
  │     ├ findByLearningMaterialIdAndDeletedFalse
  │     └ Deck.markMaterialDeleted() * N (dirty checking)
  │
  └ materialRepository.delete(material)  ← 참조 해제 후
  └ (commit or rollback 함께)
```

**FK 안전 원칙**: 참조를 먼저 끊고, 물리 삭제는 그 뒤. 이벤트 발행 순서가 중요.

---

## `REQUIRES_NEW` 사용처

| 위치 | 이유 |
| --- | --- |
| (현재 사용처 명시 없음) | 이력·감사 append 도메인 서비스에서 사용 검토 대상 (`CardStatusHistoryAppender`, `UserScheduleConfigHistoryAppender`)이나 현재 코드에선 상위 트랜잭션에 그대로 참여 (REQUIRED). 별도 트랜잭션 필요 시 이 표에 추가 |

---

## OSIV / lazy loading 처리

- `spring.jpa.open-in-view: false` (application.yml)
- Presentation에서 lazy loading 발생하면 `LazyInitializationException` — DTO 팩토리에서 Aggregate Root 로딩 시점에 필요한 그래프 fetch (fetch join · `@EntityGraph`)

---

## 감시 포인트

topology `v1-transaction.md` §2 Invariants와 일치. 다음 발견 시 리뷰:
- Domain / Presentation 층에 `@Transactional` 등장
- `@Async` · `@TransactionalEventListener` 등장
- Query Service에 `readOnly` 빠짐
- `noRollbackFor` 사용
- OSIV=true 변경

---

## 참조

- 코드 (진실 소스): `src/main/java/com/example/thirdtool/*/application/service/*Service.java`
- 관련 topology: `workflow/topologys/transaction/v1-transaction.md`
- 관련 living-docs: `event-catalog/events.md`, `boundary-trace/boundary.md`, `architecture-system-design/architecture.md`

*최신 갱신: 2026-07-03 · 24 Application Service + 2 이벤트 핸들러 반영*
