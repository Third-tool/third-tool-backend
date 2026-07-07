# [Fix/pes] Axis→Deck 이벤트 트리거 리팩토링 (2026-06-30)

## 대상 (원본)

- 원본 Product: `workflow/task/pes/workspectrum/sdd/done/product-learningFacade.md`
- 영향 받는 Epic / Story: Epic 6 / Story 6-1 (Material 등록 시 Deck 자동 생성), Story 6-2 (Deck 진입 연동)
- 본 fix 발견 시점: 2026-06-30
- 연계 브레인스토밍: `workflow/task/fix/brainstorming/version/0.0.1v/fix-axis-deck-refactor.md`
- 연계 보고서: `workflow/task/fix/brainstorming/version/0.0.1v/issue2b-facade-multicontext-deck-arc-report.md`

## 트리거

`issue2b-facade-multicontext-deck-arc-report.md` §5에서 Axis = 학습 실행 공간 단위임을 확인. 기존 설계(Material 등록 → `LearningMaterialCreatedEvent` → Deck 생성)는 Deck이 Material 단위로 생성되어 "1 Axis = 1 Deck" 경계가 깨진다. Axis 1개에 Material을 여러 개 등록하면 Deck이 여러 개 생기거나 중복 방지 로직이 필요해지는 구조. `LearningAxisCreatedEvent → Deck` 흐름으로 교체해 Roadmap BC(Facade/Axis/Topic/Material)와 Learning BC(Deck/Card) 경계를 명확히 한다.

---

## 변경 내역

### Story별 명세 변경 이력

#### Fix-Story 1: LearningAxisCreatedEvent + Handler + Deck.createFromAxis()

- **신규 AC**:
  - Given LearningAxis가 생성됨
  - When `LearningAxisCreatedEvent(userId, axisId, axisName)` 발행
  - Then 해당 axisId 기준 Deck이 1개 생성됨
  - And 동일 axisId로 핸들러가 두 번 호출되어도 Deck은 1개 (멱등)
  - And userId가 존재하지 않으면 `USER_NOT_FOUND` 예외
- **DoD**:
  - [ ] `LearningFacade/domain/event/LearningAxisCreatedEvent.java` — `record(Long userId, Long axisId, String axisName)`
  - [ ] `Deck/application/event/LearningAxisCreatedEventHandler.java` — `@EventListener`, `existsByAxisIdAndDeletedFalse` 멱등 체크
  - [ ] `Deck/domain/model/Deck.java` — `createFromAxis(UserEntity user, Long axisId, String name)` 정적 팩토리
  - [ ] `Deck/infrastructure/persistence/DeckRepository.java` — `existsByAxisIdAndDeletedFalse(Long axisId)` 추가
  - [ ] `LearningAxisCreatedEventHandlerTest` (해피/멱등/USER_NOT_FOUND 3케이스)
- **사유**: Axis 생성 측 이벤트 인프라가 먼저 존재해야 Story 3에서 기존 이벤트를 안전하게 삭제 가능

#### Fix-Story 2: LearningFacadeCommandService.addAxis() 이벤트 발행

- **변경 전 AC**: `addAxis()` → `facadeRepository.save()` 후 종료
- **변경 후 AC**: `addAxis()` → `facadeRepository.save()` → `LearningAxisCreatedEvent` 발행
- **DoD**:
  - [ ] `LearningFacadeCommandService`에 `ApplicationEventPublisher` 주입
  - [ ] `addAxis()` 내 `eventPublisher.publishEvent(new LearningAxisCreatedEvent(...))` 추가
  - [ ] `LearningFacadeCommandServiceAxisEventTest` — `addAxis()` 호출 시 이벤트 발행 검증
- **사유**: Story 1 완료 후 진행 (핸들러가 먼저 있어야 이벤트 발행 시 정상 처리됨)
- **미결 결정**: `addAxis()` 응답 `AddAxis`에 `deckId` 포함 여부 (아래 §결정 항목 참조)

#### Fix-Story 3: Material 등록에서 Deck 생성 제거 + API 계약 변경

- **변경 전 AC**: `POST /materials` → 응답에 `deckId`, `deckName` 포함
- **변경 후 AC**: `POST /materials` → Deck 생성 없음, 응답에서 `deckId`/`deckName` 제거
- **DoD**:
  - [ ] `LearningFacade/domain/event/LearningMaterialCreatedEvent.java` 삭제 (Deck 생성 목적 이벤트)
  - [ ] `Deck/application/event/LearningMaterialCreatedEventHandler.java` 삭제
  - [ ] `Deck/domain/model/Deck.java` — `createFromLearningMaterial()` 삭제
  - [ ] `LearningMaterialCommandService.createMaterial()` — 이벤트 발행 블록 및 `event.createdDeckId()` 읽기 제거
  - [ ] `LearningMaterialCommand.CreateMaterial` — `deckName`, `forceCreateDeck` 필드 제거
  - [ ] `LearningMaterialRequest.CreateMaterialRequest` — `deckName`, `forceCreateDeck` 필드 제거
  - [ ] `LearningMaterialResponse.CreateMaterial` — `deckId`, `deckName` 필드 제거
  - [ ] `LearningMaterialCommandServiceCreateTest` — `deckName`/`forceCreateDeck`/`deckId` 관련 케이스 제거
  - [ ] `LearningMaterialCreatedEventHandlerTest` 삭제
- **사유**: Story 1·2 완료 확인 후 진행 (신규 이벤트 흐름이 먼저 동작해야 기존 제거 안전)

#### Fix-Story 4: ADR007 갱신 (docs)

- **변경 내용**: 이벤트 발행 주체가 `LearningMaterialCommandService` → `LearningFacadeCommandService.addAxis()`로 변경. 이벤트 클래스가 `LearningMaterialCreatedEvent` → `LearningAxisCreatedEvent`로 교체됨을 기록.
- **DoD**:
  - [ ] `docs/adr/ADR007.md` 수정 — 변경 이력 추가

---

### ErrorCode / DTO 변경

신규 ErrorCode: 없음 (기존 `USER_NOT_FOUND`, `DECK_NAME_DUPLICATE` 그대로 사용)

DTO 필드 변경:

| DTO | 필드 | 변경 |
|-----|------|------|
| `LearningMaterialRequest.CreateMaterialRequest` | `deckName` (String) | 제거 |
| `LearningMaterialRequest.CreateMaterialRequest` | `forceCreateDeck` (boolean) | 제거 |
| `LearningMaterialResponse.CreateMaterial` | `deckId` (Long) | 제거 |
| `LearningMaterialResponse.CreateMaterial` | `deckName` (String) | 제거 |
| `LearningMaterialCommand.CreateMaterial` | `deckName` (String) | 제거 |
| `LearningMaterialCommand.CreateMaterial` | `forceCreateDeck` (boolean) | 제거 |

신규 Repository 메서드:

```java
// DeckRepository
boolean existsByAxisIdAndDeletedFalse(Long axisId);
```

---

## 결정 항목 (Story 2 전 확정 필요)

| # | 항목 | 선택지 | 영향 |
|---|------|--------|------|
| 1 | `addAxis()` 응답에 `deckId` 포함 여부 | (A) 포함 — FE가 Axis 생성 직후 Deck으로 즉시 이동 가능 / (B) 미포함 — FE가 별도 Deck 조회 | (A) 선택 시 `LearningAxisCreatedEvent`에 mutable result 패턴 필요 또는 Handler 반환값 통신 방식 결정 |

> 결정 2(Axis 이름 변경 → Deck 이름 동기화), 결정 3(Axis 삭제 → Deck 처리)은 이번 Fix 범위 밖 — v1.5 이관.

---

## 검증

- 단위: `LearningAxisCreatedEventHandlerTest` — 해피/멱등/예외 3케이스
- 단위: `LearningFacadeCommandServiceAxisEventTest` — `addAxis()` 이벤트 발행 검증
- 슬라이스 (Repository): `DeckRepository.existsByAxisIdAndDeletedFalse` @DataJpaTest
- 통합 확인: `POST /facades/{facadeId}/axes` 호출 → `deck` 테이블에 axisId 기준 레코드 1개 생성

---

## 영향

- 원본 명세 (`product-learningFacade.md`) — done/ 파일이므로 이번 fix에서 미수정. 다음 planning 사이클에서 Epic 6 갱신 예정.
- ADR007 갱신 — Fix-Story 4 (docs 커밋)
- Card BC — 무관
- ReviewSession BC — 무관
- FE `third-tool-fe`:
  - `POST /materials` request에서 `deckName`, `forceCreateDeck` 필드 전송 제거
  - `POST /materials` response에서 `deckId`, `deckName` 읽기 코드 제거
- DB 마이그레이션 — 불필요 (`deck.axis_id`, `deck.learning_material_id` 모두 기존 nullable, 스키마 변경 없음)

---

## 실행 순서 제약

```
Fix-Story 1 (이벤트+핸들러+팩토리 신규)
  → Fix-Story 2 (addAxis() 이벤트 발행)
    → Fix-Story 3 (기존 Material→Deck 제거)
      → Fix-Story 4 (ADR007 docs)
```

Story 3은 Story 1·2가 완료되어 신규 이벤트 흐름이 동작하는 것을 확인한 뒤 진행.
