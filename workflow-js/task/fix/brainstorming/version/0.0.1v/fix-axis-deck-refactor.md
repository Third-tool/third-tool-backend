# [Fix 문서] Axis→Deck 이벤트 트리거 리팩토링
> 0.0.1v Fix 범위 확정 문서 (2026-06-30)

**배경**: `issue2b-facade-multicontext-deck-arc-report.md`에서 확인한 장기 방향성에 따라
Deck 자동 생성 트리거를 **Material 등록 → Axis 생성**으로 변경한다.
**연계 보고서**: `./issue2b-facade-multicontext-deck-arc-report.md` §5 (Axis→Deck 연동 설계)
**ADR 연관**: ADR007 (BC 간 단방향 동기 이벤트)

---

## 1. 변경 이유

### 기존 설계 (Material → Deck)
```
LearningMaterial 등록
  → LearningMaterialCreatedEvent 발행
  → Deck 자동 생성
```

**문제**: Deck의 학습 실행 공간은 **자료(Material)** 단위가 아니라 **축(Axis)** 단위로 관리되어야 한다.
- Axis 1개 = 하나의 학습 차원 (예: "Java 심화")
- Deck = 그 차원에서의 학습 실행 공간
- Material은 Deck 안의 Card를 만드는 재료이지, Deck을 만드는 단위가 아님

### 새 설계 (Axis → Deck)
```
LearningAxis 생성
  → LearningAxisCreatedEvent 발행
  → Deck 자동 생성 (이름 = Axis.name)
```

**효과**:
- Axis가 생기면 학습 공간(Deck)이 즉시 열림
- Material을 여러 개 등록해도 Deck은 1개 (Axis 당 1 Deck 1:1)
- 로드맵(Axis/Topic) 구성과 학습 실행(Deck/Card) 사이 경계가 명확해짐

---

## 2. 현재 코드 현황

### 이벤트 흐름 (현재)
```
LearningMaterialCommandService.createMaterial()
  → eventPublisher.publishEvent(new LearningMaterialCreatedEvent(
      userId, materialId, materialName, firstAxisId, deckName, forceCreateDeck))
  → LearningMaterialCreatedEventHandler.handle()   [@EventListener 동기]
  → Deck.createFromLearningMaterial(user, axisId, materialId, deckName)
  → event.setResult(deckId, deckName)
  → 응답: CreateMaterial (deckId, deckName 포함)
```

### 관련 파일 (현재)

| 파일 | 역할 |
| --- | --- |
| `LearningFacade/domain/event/LearningMaterialCreatedEvent.java` | Deck 생성 용도 이벤트 (mutable — setResult 패턴) |
| `Deck/application/event/LearningMaterialCreatedEventHandler.java` | @EventListener — Deck 생성 로직 |
| `Deck/domain/model/Deck.java` — `createFromLearningMaterial()` | materialId 기반 Deck 생성 팩토리 |
| `LearningFacade/application/service/LearningMaterialCommandService.java` | 이벤트 발행 + 결과(deckId) 응답 |
| `LearningFacade/application/dto/LearningMaterialCommand.java` — `CreateMaterial` | `deckName`, `forceCreateDeck` 필드 |
| `LearningFacade/presentation/dto/LearningMaterialRequest.java` | `deckName`, `forceCreateDeck` request 필드 |
| `LearningFacade/presentation/dto/LearningMaterialResponse.java` — `CreateMaterial` | `deckId`, `deckName` 응답 필드 |
| `LearningFacade/application/service/LearningFacadeCommandService.java` — `addAxis()` | ApplicationEventPublisher 없음 |

---

## 3. 변경 대상 파일

### 신규 생성

| 파일 | 내용 |
| --- | --- |
| `LearningFacade/domain/event/LearningAxisCreatedEvent.java` | `userId`, `axisId`, `axisName` 담는 immutable record |
| `Deck/application/event/LearningAxisCreatedEventHandler.java` | @EventListener — `Deck.createFromAxis()` 호출 |

### 수정

| 파일 | 변경 내용 |
| --- | --- |
| `LearningFacade/application/service/LearningFacadeCommandService.java` | `ApplicationEventPublisher` 주입 + `addAxis()`에서 이벤트 발행 |
| `Deck/domain/model/Deck.java` | `createFromAxis(user, axisId, name)` 정적 팩토리 추가 |
| `LearningFacade/application/service/LearningMaterialCommandService.java` | `LearningMaterialCreatedEvent` 발행 제거, 응답 단순화 |
| `LearningFacade/application/dto/LearningMaterialCommand.java` | `CreateMaterial`에서 `deckName`, `forceCreateDeck` 제거 |
| `LearningFacade/presentation/dto/LearningMaterialRequest.java` | Request에서 `deckName`, `forceCreateDeck` 제거 |
| `LearningFacade/presentation/dto/LearningMaterialResponse.java` | `CreateMaterial` 응답에서 `deckId`, `deckName` 제거 |

### 삭제

| 파일 | 사유 |
| --- | --- |
| `LearningFacade/domain/event/LearningMaterialCreatedEvent.java` | Deck 생성 목적 이벤트. Material 삭제 이벤트(`LearningMaterialDeletedEvent`)는 별도이므로 유지. |
| `Deck/application/event/LearningMaterialCreatedEventHandler.java` | 이벤트 자체가 사라지므로 핸들러도 삭제 |
| `Deck/domain/model/Deck.java` — `createFromLearningMaterial()` | Axis→Deck 이후 진입점 없음. 삭제 (코드 정리) |

---

## 4. 변경 상세

### 4.1 신규: `LearningAxisCreatedEvent`

```java
// LearningFacade/domain/event/LearningAxisCreatedEvent.java
public record LearningAxisCreatedEvent(
    Long userId,
    Long axisId,
    String axisName
) {}
```

- immutable record (mutable 패턴 불필요 — Deck 생성 결과를 addAxis 응답에 포함할 필요 없음)
- `@TransactionalEventListener` 아님 — 동기 @EventListener (ADR007 동일 패턴)
- addAxis 응답(`AddAxis`)에 `deckId`를 담을지는 결정 필요 (§6 참조)

### 4.2 신규: `LearningAxisCreatedEventHandler`

```java
// Deck/application/event/LearningAxisCreatedEventHandler.java
@Component
@RequiredArgsConstructor
public class LearningAxisCreatedEventHandler {

    private final DeckRepository deckRepository;
    private final UserRepository userRepository;

    @EventListener
    public void handle(LearningAxisCreatedEvent event) {
        UserEntity user = userRepository.findById(event.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Axis당 1 Deck. 같은 axisId로 이미 Deck이 있으면 멱등 처리 (재생성 안 함).
        boolean exists = deckRepository.existsByAxisIdAndDeletedFalse(event.axisId());
        if (exists) {
            return;
        }

        Deck deck = Deck.createFromAxis(user, event.axisId(), event.axisName());
        deckRepository.save(deck);
    }
}
```

**멱등 조건**: Axis 하나에 Deck이 이미 있으면 재생성하지 않음.
→ `DeckRepository`에 `existsByAxisIdAndDeletedFalse(Long axisId)` 추가 필요.

### 4.3 수정: `Deck.createFromAxis()`

```java
// Deck/domain/model/Deck.java
/**
 * Axis 생성 흐름에서 호출되는 정적 팩토리.
 * {@code LearningAxisCreatedEventHandler}가 사용한다.
 *
 * @param user    소유 사용자
 * @param axisId  연결된 축 ID (필수)
 * @param name    Deck 이름 (= Axis.name)
 */
public static Deck createFromAxis(UserEntity user, Long axisId, String name) {
    validateName(name);
    if (user == null) throw new BusinessException(ErrorCode.INVALID_INPUT, "Deck: user는 null일 수 없습니다.");
    if (axisId == null) throw new BusinessException(ErrorCode.INVALID_INPUT, "Deck: axisId는 null일 수 없습니다.");

    Deck deck = new Deck();
    deck.name               = name.trim();
    deck.user               = user;
    deck.axisId             = axisId;
    deck.learningMaterialId = null;   // Axis 기반 생성 — 자료 미연결 상태로 시작
    deck.parentDeck         = null;
    deck.depth              = 0;
    deck.lastAccessed       = LocalDateTime.now();
    deck.mode               = DeckMode.ON_FIELD;
    return deck;
}
```

### 4.4 수정: `LearningFacadeCommandService.addAxis()`

```java
// addAxis()에 ApplicationEventPublisher 주입 + 이벤트 발행
public AddAxis addAxis(LearningFacadeCommand.AddAxis command) {
    LearningFacade facade = loadFacade(command.userId());
    LearningAxis axis = facade.addAxis(command.name());
    facadeRepository.save(facade);

    eventPublisher.publishEvent(
        new LearningAxisCreatedEvent(command.userId(), axis.getId(), axis.getName())
    );

    return AddAxis.of(axis, facade.isAxisCountExceedsRecommended());
}
```

### 4.5 수정: `LearningMaterialCommandService.createMaterial()`

- `LearningMaterialCreatedEvent` 발행 블록 **제거**
- `event.createdDeckId()` 읽는 null 체크 블록 **제거**
- `CreateMaterial` 응답 빌드에서 `deckId`, `deckName` **제거**

### 4.6 API 계약 변경

**`POST /api/v1/learning-facade/materials` Request** — 제거되는 필드:
- `deckName` (string, optional)
- `forceCreateDeck` (boolean, optional)

**`POST /api/v1/learning-facade/materials` Response** — 제거되는 필드:
- `deckId` (Long)
- `deckName` (String)

> **FE 영향**: FE `third-tool-fe`에서 Material 등록 시 `deckId`/`deckName`을 응답에서 읽는 코드가 있으면 제거 필요. `deckName`/`forceCreateDeck` request 필드 전송도 제거.

---

## 5. DB 영향

| 컬럼 | 현재 | 변경 후 | 마이그레이션 |
| --- | --- | --- | --- |
| `deck.axis_id` | nullable | nullable (유지) | 불필요 |
| `deck.learning_material_id` | nullable | nullable (유지, 자연스럽게 null) | 불필요 |

- 기존에 Material 등록으로 생성된 Deck: `learning_material_id`가 채워진 상태. 그대로 보존 (이력).
- 새로 생성되는 Deck (Axis 기반): `learning_material_id = null`, `axis_id` 세팅.
- 스키마 변경 없음. Flyway 마이그레이션 불필요.

**추가 쿼리 메서드 필요** (`DeckRepository`):
```java
boolean existsByAxisIdAndDeletedFalse(Long axisId);
```

---

## 6. 미결 결정 항목

| # | 결정 항목 | 선택지 | 영향 |
| --- | --- | --- | --- |
| 1 | `addAxis()` 응답에 `deckId` 포함할 것인가? | (A) 포함 — FE가 즉시 Deck으로 이동 가능 / (B) 미포함 — FE가 별도로 Deck 조회 | AddAxis 응답 DTO, LearningAxisCreatedEvent에 결과 통신 필요 여부 |
| 2 | Axis 이름 변경 시 Deck 이름도 변경할 것인가? | (A) 연동 변경 / (B) 독립 유지 (사용자가 Deck 이름 별도 관리) | Axis updateName() 이후 Deck 이름 동기화 이벤트 필요 |
| 3 | Axis 삭제 시 Deck 처리 | (A) Deck soft delete / (B) Deck 독립 보존 (자료 미연결 뱃지) | removeAxis() 이벤트 또는 Deck 삭제 정책 |

> 이번 Fix에서 **결정 1은 필수** (응답 설계). 결정 2·3은 v1.5로 미룰 수 있음.

---

## 7. 테스트 변경

| 기존 | 새 | 변경 유형 |
| --- | --- | --- |
| `LearningMaterialCreatedEventHandlerTest.java` | **삭제** | 핸들러 삭제 |
| (신규) | `LearningAxisCreatedEventHandlerTest.java` | 해피/엣지/예외 3구분 |
| `LearningMaterialCommandServiceCreateTest.java` | `deckName`/`forceCreateDeck`/`deckId` 관련 케이스 제거 | 기존 파일 수정 |
| (신규) | `LearningFacadeCommandServiceAxisEventTest.java` | addAxis() 이벤트 발행 검증 |

**`LearningAxisCreatedEventHandlerTest` 케이스 최소 세트**:
- `handle_valid_덱_생성됨`: 정상 Axis 이벤트 → Deck 1개 생성
- `handle_axisId_이미_Deck_존재_멱등_처리`: 동일 axisId로 핸들러 두 번 → Deck 1개만 존재
- `handle_userId_없음_예외`: USER_NOT_FOUND 발생

---

## 8. Fix Tier 및 Story 분할

**tier**: `feature-story` (Epic 1개 — Axis→Deck 이벤트 리팩토링)

| Story | 제목 | 작업 |
| --- | --- | --- |
| Story 1 | LearningAxisCreatedEvent + Handler + Deck.createFromAxis() | 신규 이벤트·핸들러·팩토리. 테스트 포함 |
| Story 2 | LearningFacadeCommandService.addAxis() 이벤트 발행 | Publisher 주입 + 이벤트 발행 + Service 단위 테스트 |
| Story 3 | Material 등록에서 Deck 생성 제거 + API 계약 변경 | LearningMaterialCreatedEvent 삭제, 핸들러 삭제, DTO 계약 변경 |
| Story 4 | ADR007 갱신 (이벤트 트리거 변경 기록) | docs/adr/ADR007.md 수정 |

**전제**: Story 3은 Story 1·2 완료 후 진행 (신규 이벤트가 먼저 동작해야 기존 제거 안전).

---

## 9. 참조

- `./issue2b-facade-multicontext-deck-arc-report.md` — Axis→Deck 설계 분석
- `src/main/java/com/example/thirdtool/Deck/application/event/LearningMaterialCreatedEventHandler.java` — 삭제 대상
- `src/main/java/com/example/thirdtool/LearningFacade/domain/event/LearningMaterialCreatedEvent.java` — 삭제 대상
- `src/main/java/com/example/thirdtool/Deck/domain/model/Deck.java` — `createFromAxis()` 추가
- `src/main/java/com/example/thirdtool/LearningFacade/application/service/LearningFacadeCommandService.java` — `addAxis()` 수정
- `docs/adr/ADR007.md` — BC 간 단방향 동기 이벤트 (갱신 대상)
- 작성일: 2026-06-30
