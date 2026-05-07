# Rule: 도메인 객체 컨벤션

> private-docs/domain/* 전반에서 반복되는 도메인 모델링 규칙. CLAUDE.md "아키텍처 핵심"의 보강이다.
> 코드를 작성·검토할 때 매번 적용한다. 위반이 보이면 사용자에게 보고하고 수정 동의를 받는다.

---

## 1. 생성·수정 입력 정규화

| 입력 종류 | 처리 |
| --- | --- |
| 모든 String (이름·컨셉·설명·태그값·키워드값) | 저장 전 **trim** |
| 선택적(nullable) String 필드 | trim 후 **빈 문자열은 `null`로 정규화** (예: `AxisTopic.description`, `LearningMaterial.url`) |
| 필수 String 필드 | trim 후 `null` 또는 blank이면 **생성·수정 거부** |

**근거**: 동일 값 비교(`updateConcept`, `updateName`)에서 trim 전 비교 시 불필요한 변경 이력·`updatedAt` 갱신이 발생한다. 빈 문자열을 그대로 두면 조회 시 `null`/`""` 분기 처리가 코드 전반에 퍼진다.

**적용 위치**: 도메인 행위 메서드 **내부**(Application Service에서 trim하지 않는다 — 도메인 메서드는 항상 정규화된 상태를 보장해야 한다).

---

## 2. 정적 팩토리 + new 금지

- VO·Entity·Aggregate Root는 **정적 팩토리**(`of(...)`, `create(...)`)만 외부 진입점으로 노출한다. 생성자는 `protected` 또는 `package-private`.
- 자식 엔티티는 **부모 Aggregate의 행위를 통해서만 생성**한다.
  - `LearningAxis` → `LearningFacade.addAxis()`로만
  - `AxisTopic` → `LearningAxis.addTopic()` / `addTopics()`로만
  - `KeywordCue` → `Card.addKeyword()` / `Card.create()`로만
  - `CardStatusHistory` → `CardStatusHistoryAppender.append()`로만
- 매핑 엔티티(`TopicMaterial`, `CardTag`)도 **소유 Aggregate의 행위를 통해서만** 생성·삭제한다.

**예외**: 정적 팩토리 안의 `new`는 허용. Application Service는 정적 팩토리만 호출한다.

---

## 3. 컬렉션 캡슐화

- Aggregate가 보유한 자식 컬렉션은 **`Collections.unmodifiableList(...)`로 감싸서 노출**한다 (CLAUDE.md 규칙 강제).
- 자식 추가/삭제는 반드시 Aggregate의 행위(`addX`/`removeX`/`replaceX`)를 통해 수행한다.
- 자식 엔티티 삭제는 **`orphanRemoval = true`** 로 처리. 부모 컬렉션에서 제거되면 자동 삭제.
- `LearningMaterial`처럼 부모 삭제 시에도 보존되어야 하는 엔티티는 명시적으로 `orphanRemoval = false` (또는 cascade 미설정).

---

## 4. displayOrder (순서 필드) 규칙

| 측면 | 규칙 |
| --- | --- |
| 도메인 의미 | **1-based**. 첫 항목은 `displayOrder = 1` |
| DB CHECK 제약 | `>= 0` (안전망. 도메인은 0을 부여하지 않는다) |
| 외부 주입 | **금지**. Application Service / Controller가 displayOrder를 직접 설정하지 않는다 |
| 신규 추가 | Aggregate의 `addX()`가 `현재 max + 1`로 자동 계산 |
| 재배치 | `reorderX(List<Long> orderedIds)`로만. 호출 시 전달된 id 집합과 현재 컬렉션 id 집합이 **불일치하면 예외**. 일치하면 1부터 재부여 |
| 빈 컬렉션 reorder | 빈 리스트 전달은 정상 처리 (no-op) |

**근거**: displayOrder를 외부에서 주입하면 부분 갱신·중복·구멍 발생. 1-based는 도메인 의미(1번째, 2번째)와 일치, 0은 "기본값/없음" 의미로 혼선이 생긴다. id 집합 검증은 유령 id 삽입과 타 부모 자식 오염을 동시에 차단한다.

---

## 5. 상태 전환 멱등성

상태 전환 메서드는 **이미 같은 상태이면 아무 작업도 수행하지 않는다**. 예외를 던지지 않는다.

| 메서드 | 멱등 처리 |
| --- | --- |
| `Card.archive()` | 이미 `ARCHIVE`이면 무시 |
| `Card.returnToField()` | 이미 `ON_FIELD`이면 무시 |
| `Card.recordView()` | `ARCHIVE` 상태면 무시 (예외 X) |
| `CardReview.startComparing()` | 이미 `COMPARING`이면 무시 |

**이력 기록 측 처리**: `fromStatus == toStatus`인 호출은 `CardStatusHistoryAppender`가 이력을 생성하지 않는다 (전환 자체가 없으므로).

---

## 6. 이중 방어 (도메인 검증 + DB 제약)

도메인 규칙은 도메인 메서드에서 검증하고, 같은 규칙을 **DB 제약으로도 동시에 강제**한다.

| 규칙 | 도메인 검증 | DB 제약 |
| --- | --- | --- |
| 동일 LearningFacade 내 축 이름 중복 금지 | `LearningFacade.addAxis()` / `updateAxisName()` | `UNIQUE(facade_id, name)` |
| 유저당 LearningFacade 1개 (v1) | Application Service 선체크 | `UNIQUE(user_id) on learning_facade` |
| TopicMaterial 매핑 중복 금지 | Application Service 선체크 | `UNIQUE(topic_id, material_id)` |
| 동일 사용자 + 동일 이름 Deck | 도메인 검증 | DB 유니크 제약 |
| Tag value 시스템 전역 유니크 | 도메인 검증 | DB 유니크 제약 |

**원칙**: 도메인 검증을 빠뜨려도 DB 제약이 막는다. 반대로 DB 제약만 의지하면 친절한 에러 메시지를 못 준다. 둘 다 둔다.

---

## 7. 책임 분리 (Aggregate vs Domain Service vs Application Service)

| 책임 | 위치 |
| --- | --- |
| 자기 불변식 강제 (필드 검증, 상태 전환 규칙) | **Aggregate / Entity / VO** |
| 다중 Aggregate 협력이 필요한 도메인 규칙 | **Domain Service** (`CardRelationFinder`, `CardStatusHistoryAppender`, `CardExpiryPolicy`) |
| 이력 기록 / 자동 만료 처리 / 사이드 이펙트 조율 | **Domain Service** 호출은 Application Service가 한다 |
| 트랜잭션 경계 / 부분 성공 여부 결정 | **Application Service** |
| 소유권 검증 (`isOwnedBy(userId)`) | Aggregate 메서드를 **Application Service가 호출**해 검증 |
| 권장 한도 초과 안내 (예: `isAxisCountExceedsRecommended`) | Aggregate가 **boolean 제공**, Application Service가 응답 DTO에 플래그 포함 |
| 커버리지 재계산 | Application Service (`CoverageRecalculator`) — 도메인은 결과만 받아 적용 |

**금지**:
- Aggregate가 Repository를 직접 호출하지 않는다.
- Aggregate가 다른 Aggregate Root의 상태를 직접 변경하지 않는다.
- 비즈니스 규칙을 Application Service에 펼치지 않는다 (Aggregate 메서드로 응집).

---

## 8. 다건 입력 처리 (부분 성공 불허)

`LearningAxis.addTopics(List<TopicCommand>)` 같은 다건 행위는:

- **한 건이라도 검증 실패하면 전체 롤백**한다 (트랜잭션 단위).
- 부분 성공을 허용하지 않는다.
- 빈 리스트는 정상 처리 (no-op). null은 예외.
- 다건 입력은 입력 VO(예: `TopicCommand`)로 받는다 — 단건은 원시 인자 그대로 받아 입력 VO 강제하지 않는다.

---

## 9. 도메인 상수 관리

- 권장 한도·임계값은 **도메인 객체 안에 상수**로 둔다 (`RECOMMENDED_AXIS_COUNT_LIMIT = 5`, `RECOMMENDED_TOPIC_COUNT_LIMIT = 10`, `FOCUS_TOP_N = 3`).
- 변경 시 도메인 코드 한 곳만 수정한다.
- 운영 중 동적 조정이 필요한 시점에 Admin BC의 시스템 설정으로 이관한다 — 그 전에는 코드 상수를 유지한다.
- Application Service / Controller / Frontend가 상수를 **재정의하지 않는다**.

---

## 10. 결과 표현 VO 패턴

상태 변경 결과를 호출 측에 알릴 때는 **결과 VO**를 반환한다.

- `LearningFacade.updateConcept()` → `ConceptChangeRecord` (changed/unchanged 표현)
- `OnFieldBudget.resolveReason(card)` → `Optional<ArchiveReason>` (만료 사유 또는 empty)

**규칙**:
- 결과 VO는 **불변**. 정적 팩토리(`changed(...)`, `unchanged(...)`)로만 생성.
- 외부에서 직접 생성하지 않는다 — 도메인 행위의 반환값으로만 얻는다.
- `isChanged()`가 `false`이면 Application Service는 저장 쿼리를 생략한다.

---

## 11. 외부에서 직접 수정 금지 필드

다음 필드들은 **Aggregate 행위를 통해서만** 변경된다. setter 노출 금지.

- `displayOrder` — `addX` / `reorderX`에서만
- `coverageStatus` — `updateCoverageStatus()`(Application Service만 호출)
- `proficiencyLevel` — `updateProficiencyLevel()`로만
- `lastViewedAt`, `viewCount`, `enteredFieldAt` — `recordView()` / `archive()` / `returnToField()`로만
- `createdAt`, `updatedAt`, `linkedAt`, `changedAt` — JPA Auditing 또는 생성 시점 자동 기록. 외부 주입 금지.
- `contentType` (`MainNote`) — 텍스트/이미지 존재 여부에 따라 내부에서 결정. 외부 주입 금지.
