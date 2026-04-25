# LearningFacade 도메인 모델

> 사용자의 "직업적 컨셉"을 축으로 학습 목표를 구조화하는 BC. `LearningFacade`(컨셉) → `LearningAxis`(세부 축) → `AxisAction`(행동 동사) → `LearningMaterial`(학습 자료)로 계층화하고, 행동-자료 매핑의 커버리지를 추적합니다.

> 작성 규칙은 [`_rules.md`](_rules.md) 참조. 본 BC는 신규 도메인이며 [`도메인모델.md`](도메인모델.md)에 별도 정리되어 있지 않습니다 — 이 파일이 단일 진실원입니다.

---

## 구성 요소

| 구분 | 클래스 | 역할 |
|---|---|---|
| Aggregate Root | `LearningFacade` | 사용자별 학습 목표 프레임워크 |
| Entity | `LearningAxis` | 컨셉 하위 세부 축 (권장 ≤5) |
| Entity | `AxisAction` | 축마다의 행동 동사 |
| Entity | `LearningMaterial` | 학습 자료 (교재, 강좌, 논문 등) |
| Entity | `ActionMaterial` | Action ↔ Material 중간 엔티티 |
| Entity | `ActionRevision` | 행동 동사 수정 이력 |
| Entity | `RevisionReasonOption` | 수정 사유 선택지 |
| Value Object | `ConceptChangeRecord` | 컨셉 변경 이력 |
| Value Object | `ActionChangeRecord` | 행동 변경 이력 |
| Enum | `MaterialType` | `TOP_DOWN` / `BOTTOM_UP` 학습 순서 |
| Enum | `ProficiencyLevel` | `UNRATED` … `EXPERT` |
| Enum | `CoverageStatus` | `NO_MATERIAL` / `PARTIAL` / `COVERED` |
| Exception | `LearningFacadeDomainException` | BC 전용 예외 |

---

## LearningFacade (Aggregate Root)

> 사용자의 직업적 컨셉을 표현하고, 그 아래 LearningAxis 목록을 관리.

### 설명
_TBD_ — 컨셉의 의미, 권장 축 개수(≤5)의 의도, 사용자 1명당 LearningFacade 개수 정책 등을 서술.

### 속성
| 속성 | 타입 | 설명 |
|---|---|---|
| `id` | `Long` | 식별자 |
| `userId` | `Long` | 소유 사용자 (BC: User, ID 참조) |
| `concept` | `String` | 직업적 컨셉 |
| `axes` | `List<LearningAxis>` | 세부 축 목록 |
| 감사 컬럼 | `LocalDateTime` | `createdAt`, `updatedAt` |

### 행위
| 행위 | 설명 |
|---|---|
| `create(...)` | 신규 LearningFacade 생성 |
| `updateConcept(String newConcept)` | 컨셉 변경. `ConceptChangeRecord` 반환 |
| `addAxis(String name)` | 세부 축 추가 (이름 중복 불가) |
| `removeAxis(Long axisId)` | 세부 축 제거 |
| `reorderAxes(List<Long> orderedIds)` | displayOrder 1-based 재배치 |
| `isOwnedBy(Long userId)` | 소유자 확인 |
| `isAxisCountExceedsRecommended()` | 권장 수(≤5) 초과 여부 |

### 규칙
- 동일 LearningFacade 내 `LearningAxis.name`은 중복 불가.
- 권장 축 개수는 5개. 초과 시 경고만 노출(생성/추가 자체는 허용).
- 컨셉 변경 시 항상 `ConceptChangeRecord`를 생성해 이력을 남깁니다.
- 축 순서(`displayOrder`)는 1-based, 연속된 정수.

### 검증
- `concept`은 `null` 또는 blank 불가.
- `userId`는 `null` 불가.
- `reorderAxes` 호출 시 전달된 ID 목록이 현재 보유 축 목록과 정확히 일치해야 합니다(누락/추가 모두 거부).

---

## LearningAxis (Entity)

> Facade 컨셉의 하위 세부 축. (예: 컨셉 "프론트엔드 시니어" 안에서 "성능 최적화", "접근성", "디자인 시스템" 등)

### 속성 / 행위 / 규칙 / 검증
_TBD_ — `LearningAxis.java` 기준으로 채워주세요.

핵심 규칙:
- `name`은 한 LearningFacade 내에서 유니크.
- `displayOrder`는 1-based.

---

## AxisAction (Entity)

> 축마다 정의되는 구체적 행동 동사 (예: "구현하다", "설계하다", "측정하다").

### 속성
| 속성 | 타입 | 설명 |
|---|---|---|
| `id` | `Long` | |
| `axis` | `LearningAxis` | 소속 축 |
| `description` | `String` | 행동 동사 (단일 동사 권장) |
| `coverageStatus` | `CoverageStatus` | 자료 매핑 상태 |
| `revisionCount` | `int` | 수정 횟수 |

### 행위
| 행위 | 설명 |
|---|---|
| `updateDescription(String newDescription, RevisionReasonOption reason)` | 행동 변경. trim, 커버리지 초기화, revision 증가, `ActionChangeRecord` 반환 |
| `updateCoverageStatus(CoverageStatus next)` | Application Service 전용 호출. 외부 직접 호출 금지 |

### 규칙
- `description`은 단일 동사 형태(다중 동사 거부).
- 변경 시 `ActionRevision` 한 건이 반드시 생성됩니다.
- `coverageStatus`는 `ActionMaterial` 매핑 변화에 따라 Application Service가 갱신.

### 검증
_TBD_

---

## LearningMaterial (Entity)

> 외부 학습 자료(교재, 강좌, 논문, 블로그 등)의 메타정보. 사용자별 숙련도(`ProficiencyLevel`) 보유.

### 속성 / 행위 / 규칙 / 검증
_TBD_ — `LearningMaterial.java` 기준.

---

## ActionMaterial (Entity)

> AxisAction과 LearningMaterial 사이의 N:M 매핑을 명시화한 중간 엔티티. 행동의 커버리지 평가의 단위.

### 규칙
- 동일 `(axis_action_id, learning_material_id)` 조합은 유니크.
- 매핑 추가/삭제 시 소속 AxisAction의 `coverageStatus`를 재계산해야 합니다.

---

## ActionRevision (Entity)

> AxisAction 행동 동사 수정 이력. `previousDescription`, `newDescription`, `reason`, `createdAt` 보존.

### 규칙
- 외부에서 직접 생성하지 않고 `AxisAction.updateDescription`을 통해서만 생성.
- 이력은 삭제하지 않습니다.

---

## RevisionReasonOption (Entity)

> 수정 사유 선택지(전역 공유). 사용자가 임의 사유를 적는 대신 정해진 옵션 중 선택.

_TBD_ — 옵션 목록과 관리 정책 정리.

---

## ConceptChangeRecord (Value Object)
> `LearningFacade.updateConcept()` 호출 시 반환되는 변경 이력 record. `oldConcept`, `newConcept`, `changedAt` 보유.

## ActionChangeRecord (Value Object)
> `AxisAction.updateDescription()` 호출 시 반환되는 변경 이력 record.

---

## MaterialType (Enum)
| 값 | 설명 |
|---|---|
| `TOP_DOWN` | 큰 그림에서 출발해 세부로 내려가는 자료 |
| `BOTTOM_UP` | 작은 단위에서 시작해 추상화로 올라가는 자료 |

## ProficiencyLevel (Enum)
| 값 | 설명 |
|---|---|
| `UNRATED` | 미평가 |
| `NOVICE` | 입문 |
| `BEGINNER` | 초급 |
| `INTERMEDIATE` | 중급 |
| `ADVANCED` | 고급 |
| `EXPERT` | 전문가 |

## CoverageStatus (Enum)
| 값 | 설명 |
|---|---|
| `NO_MATERIAL` | 매핑된 자료 없음 |
| `PARTIAL` | 일부 자료만 매핑 |
| `COVERED` | 충분히 매핑됨 |

> 판정 임계값(예: PARTIAL ↔ COVERED 경계)은 정책 변경 가능. 현재 정책은 `LearningFacade/application/service` 참조.

---

## 책임 분리

| 클래스 | 책임 |
|---|---|
| `LearningFacade` | 사용자별 학습 목표 컨셉의 무결성과 축 목록 관리 |
| `LearningAxis` | 축 단위 정체성과 정렬 관리 |
| `AxisAction` | 행동 동사 변경 규칙과 커버리지 상태 보유 |
| `LearningMaterial` | 자료 메타정보 + 사용자 숙련도 |
| `ActionMaterial` | Action ↔ Material 매핑 보존 |
| `ActionRevision` | 행동 변경 이력 시계열 보존 |
| `*ChangeRecord` | 변경 이벤트의 결과 표현 (반환값) |
