# LearningFacade 테이블 정의서

> LearningFacade BC가 다루는 학습 목표 프레임워크 데이터 영역. 컨셉 → 축 → 행동 → 자료의 4계층 + 매핑 + 변경 이력.

> 작성 규칙은 [`_rules.md`](_rules.md) 참조. 도메인 모델은 [`docs/domain/learning-facade.md`](../domain/learning-facade.md) 참조.

---

## 테이블 목록

| 테이블 | 역할 | 도메인 매핑 | 마이그레이션 |
|---|---|---|---|
| `learning_facade` | 사용자별 학습 목표(컨셉) 본체 | `LearningFacade` (Aggregate Root) | _미반영_ |
| `learning_axis` | Facade 하위 세부 축 | `LearningAxis` | _미반영_ |
| `axis_action` | 축마다의 행동 동사 | `AxisAction` | _미반영_ |
| `learning_material` | 학습 자료 메타정보 | `LearningMaterial` | _미반영_ |
| `action_material` | Action ↔ Material 매핑 | `ActionMaterial` | _미반영_ |
| `action_revision` | 행동 변경 이력 | `ActionRevision` | _미반영_ |
| `revision_reason_option` | 수정 사유 선택지 | `RevisionReasonOption` | _미반영_ |

> V1 마이그레이션에 본 BC의 테이블은 정의되어 있지 않습니다. JPA `ddl-auto=update`로 생성 중이거나 V 버전 추가 필요.

---

## `learning_facade` _(미반영)_

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| `learning_facade_id` | `BIGINT AUTO_INCREMENT` | N | PK |
| `user_id` | `BIGINT` | N | 소유 사용자 (BC: User, ID 참조) |
| `concept` | `VARCHAR(255)` | N | 직업적 컨셉 |
| `created_at` | `DATETIME(6)` | N | 생성 시각 |
| `updated_at` | `DATETIME(6)` | Y | 수정 시각 |

### 제약
- `concept`은 NOT NULL, blank 불가 (도메인 검증).

---

## `learning_axis` _(미반영)_

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| `learning_axis_id` | `BIGINT AUTO_INCREMENT` | N | PK |
| `learning_facade_id` | `BIGINT` | N | FK → `learning_facade.learning_facade_id` |
| `name` | `VARCHAR(100)` | N | 축 이름 |
| `display_order` | `INT` | N | 1-based 순서 |

### 제약
- UNIQUE: `(learning_facade_id, name)` — Facade 내 이름 중복 방지.
- 권장 개수 5개 (애플리케이션 경고만).

---

## `axis_action` _(미반영)_

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `axis_action_id` | `BIGINT AUTO_INCREMENT` | N | - | PK |
| `learning_axis_id` | `BIGINT` | N | - | FK → `learning_axis.learning_axis_id` |
| `description` | `VARCHAR(255)` | N | - | 행동 동사 |
| `coverage_status` | `VARCHAR(20)` | N | `'NO_MATERIAL'` | `NO_MATERIAL` / `PARTIAL` / `COVERED` |
| `revision_count` | `INT` | N | `0` | 수정 횟수 |

---

## `learning_material` _(미반영)_

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| `learning_material_id` | `BIGINT AUTO_INCREMENT` | N | PK |
| `learning_facade_id` | `BIGINT` | N | FK → `learning_facade.learning_facade_id` |
| `name` | `VARCHAR(255)` | N | 자료명 |
| `material_type` | `VARCHAR(20)` | N | `TOP_DOWN` / `BOTTOM_UP` |
| `url` | `VARCHAR(2048)` | Y | 외부 링크 |
| `proficiency_level` | `VARCHAR(20)` | N | `UNRATED` / `NOVICE` / `BEGINNER` / `INTERMEDIATE` / `ADVANCED` / `EXPERT` |

---

## `action_material` _(미반영)_

> Action ↔ Material 매핑 중간 테이블.

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| `action_material_id` | `BIGINT AUTO_INCREMENT` | N | PK |
| `axis_action_id` | `BIGINT` | N | FK → `axis_action.axis_action_id` |
| `learning_material_id` | `BIGINT` | N | FK → `learning_material.learning_material_id` |

### 제약
- UNIQUE: `(axis_action_id, learning_material_id)` — 중복 매핑 방지.
- 변경 시 소속 AxisAction의 `coverage_status` 재계산 필요(애플리케이션 책임).

---

## `action_revision` _(미반영)_

> AxisAction 행동 변경 이력 시계열.

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| `action_revision_id` | `BIGINT AUTO_INCREMENT` | N | PK |
| `axis_action_id` | `BIGINT` | N | FK → `axis_action.axis_action_id` |
| `previous_description` | `VARCHAR(255)` | N | 변경 전 |
| `new_description` | `VARCHAR(255)` | N | 변경 후 |
| `revision_reason_option_id` | `BIGINT` | Y | FK → `revision_reason_option.id` |
| `created_at` | `DATETIME(6)` | N | 변경 시각 |

### 제약
- 외부에서 직접 INSERT 금지(도메인 규칙). `AxisAction.updateDescription` 경유.
- 이력 삭제 금지(AxisAction 삭제 시에만 cascade).

---

## `revision_reason_option` _(미반영)_

> 변경 사유 선택지 마스터.

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | N | PK |
| `label` | `VARCHAR(100)` | N | 사용자 표시명 |
| `code` | `VARCHAR(50)` | N | 시스템 식별 코드 |

### 제약
- UNIQUE: `code`.

---

## ER 관계

```
user_entity (외부)
  └── learning_facade (user_id 외부 참조)
        ├── learning_axis (learning_facade_id FK)
        │     └── axis_action (learning_axis_id FK)
        │           ├── action_material (axis_action_id FK)
        │           │     └── learning_material (learning_material_id FK)
        │           └── action_revision (axis_action_id FK)
        │                 └── revision_reason_option (revision_reason_option_id FK, optional)
        └── learning_material (learning_facade_id FK)
```

> 반영 마이그레이션: 없음. V 버전 추가 시 본 문서 갱신.
