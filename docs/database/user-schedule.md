# UserSchedule 테이블 정의서

> UserSchedule BC가 다루는 사용자별 학습 스케줄 설정 + 변경 이력 데이터 영역.

> 작성 규칙은 [`_rules.md`](_rules.md) 참조. 도메인 모델은 [`docs/domain/user-schedule.md`](../domain/user-schedule.md) 참조.

---

## 테이블 목록

| 테이블 | 역할 | 도메인 매핑 | 마이그레이션 |
|---|---|---|---|
| `user_schedule_config` | 사용자별 스케줄 설정 본체 | `UserScheduleConfig` (Aggregate Root) | _미반영_ |
| `user_schedule_config_history` | 설정 변경 이력 시계열 | `UserScheduleConfigHistory` | _미반영_ |

> V1 마이그레이션에 본 BC의 테이블은 정의되어 있지 않습니다. JPA `ddl-auto=update`로 생성 중이거나 V 버전 추가 필요.

---

## `user_schedule_config` _(미반영)_

> 사용자 한 명의 현재 스케줄 설정.

### 컬럼 (도메인 모델 기준 예상)

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | N | PK |
| `user_id` | `BIGINT` | N | 소유 사용자 (BC: User, ID 참조) |
| `learning_mode` | `VARCHAR(30)` | N | 현재 적용 학습 모드 |
| `created_at` | `DATETIME(6)` | N | 생성 시각 |
| `updated_at` | `DATETIME(6)` | Y | 수정 시각 |

### 제약
- UNIQUE: `user_id` 후보 — 사용자당 활성 설정 1건만 허용할지 정책 확인 필요.

### 도메인 매핑
- `learning_mode` → `UserScheduleConfig.learningMode` (`LearningMode @Enumerated`)
- 추가 정책 컬럼(예: `daily_card_limit`, `review_window`)은 도메인 정리 후 본 표 보강.

---

## `user_schedule_config_history` _(미반영)_

> 설정 변경 이력 시계열.

### 컬럼 (도메인 모델 기준 예상)

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | N | PK |
| `user_schedule_config_id` | `BIGINT` | N | FK → `user_schedule_config.id` |
| `previous_learning_mode` | `VARCHAR(30)` | N | 변경 전 모드 |
| `new_learning_mode` | `VARCHAR(30)` | N | 변경 후 모드 |
| `changed_at` | `DATETIME(6)` | N | 변경 시각 |

### 제약
- 외부에서 직접 INSERT 금지(도메인 규칙). `UserScheduleConfigHistoryAppender` 경유.
- 동일 값 변경(`previous == new`)은 이력 미생성.
- 이력 삭제 금지(설정 삭제 시에만 cascade).

---

## ER 관계

```
user_entity (외부)
  └── user_schedule_config (user_id 외부 참조)
        └── user_schedule_config_history (user_schedule_config_id FK, CASCADE)
```

> 반영 마이그레이션: 없음. V 버전 추가 시 본 문서 갱신.
