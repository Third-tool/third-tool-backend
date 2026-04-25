# UserSchedule 도메인 모델

> 사용자별 학습 스케줄/모드 설정과 그 변경 이력을 관리하는 BC.

> 작성 규칙은 [`_rules.md`](_rules.md) 참조.

---

## 구성 요소

| 구분 | 클래스 | 역할 |
|---|---|---|
| Aggregate Root | `UserScheduleConfig` | 사용자별 스케줄 설정 |
| Entity | `UserScheduleConfigHistory` | 설정 변경 이력 |
| Domain Service | `UserScheduleConfigHistoryAppender` | 이력 생성 중개 |
| Domain Service | `LearningModeMappingPolicy` | 학습 모드 매핑 규칙 |
| Enum | `LearningMode` | 학습 모드 분류값 |
| Exception | `UserScheduleDomainException` | BC 전용 예외 |

---

## UserScheduleConfig (Aggregate Root)

> 사용자 한 명의 학습 스케줄 설정 본체.

### 설명
_TBD_ — 설정의 의미, 사용자당 1건 vs N건 정책, 적용 시점 등을 서술.

### 속성 / 행위 / 규칙 / 검증
_TBD_ — `UserSchedule/domain/model/UserScheduleConfig.java` 기준으로 채워주세요.

---

## UserScheduleConfigHistory (Entity)

> `UserScheduleConfig` 변경의 시계열 이력.

### 규칙
- 외부에서 직접 생성하지 않고 `UserScheduleConfigHistoryAppender`를 통해서만 생성.
- 이력은 삭제하지 않습니다(`UserScheduleConfig` 삭제 시에만 cascade).

### 속성 / 행위 / 검증
_TBD_

---

## UserScheduleConfigHistoryAppender (Domain Service)

> 설정 변경 시 이력 생성을 중개. Aggregate가 이력 저장소에 직접 의존하지 않도록 분리.

### 행위
| 행위 | 설명 |
|---|---|
| `append(...)` | 변경 이력을 생성하고 저장소에 기록 |

### 규칙
- 동일 값으로의 변경은 이력을 생성하지 않습니다(중복 차단).
- 트랜잭션 경계는 Application Service가 결정합니다(이력 실패가 본 변경을 롤백하지 않음).

---

## LearningModeMappingPolicy (Domain Service)

> 사용자의 상황·설정을 입력받아 적절한 `LearningMode`를 결정하는 도메인 정책.

### 행위 / 규칙
_TBD_ — `LearningModeMappingPolicy.java` 기준으로 채워주세요.

---

## LearningMode (Enum)

> 학습 모드 분류값.

### 값
_TBD_ — `LearningMode.java` 기준으로 옵션 나열.

### 규칙
- 외부에서 직접 변경하지 않고 `LearningModeMappingPolicy`를 통해서만 결정.

---

## 책임 분리

| 클래스 | 책임 |
|---|---|
| `UserScheduleConfig` | 사용자별 현재 설정 보유 + 변경 규칙 강제 |
| `UserScheduleConfigHistory` | 변경 시계열 이력 보존 |
| `UserScheduleConfigHistoryAppender` | 이력 생성 중개·중복 차단 |
| `LearningModeMappingPolicy` | 입력 컨텍스트 → `LearningMode` 결정 |

### 다른 BC와의 관계
- `userId`(Long)로 User BC를 참조합니다.
- Card BC의 `SoftScheduleTemplate` / `OnFieldBudget` 정책 결정 시 본 BC의 설정을 참고할 수 있으나, 직접 객체 참조는 하지 않습니다(Application Service에서 조합).
