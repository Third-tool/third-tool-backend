# Review 테이블 정의서

> Review BC가 다루는 리뷰 세션 + 카드별 공개 단계 데이터 영역.

> 작성 규칙은 [`_rules.md`](_rules.md) 참조. 도메인 모델은 [`docs/domain/review.md`](../domain/review.md) 참조.

---

## 테이블 목록

| 테이블 | 역할 | 도메인 매핑 | 마이그레이션 |
|---|---|---|---|
| `review_session` | 리뷰 세션 본체 | `ReviewSession` (Aggregate Root) | _미반영_ |
| `card_review` | 세션 내 카드별 공개 단계 | `CardReview` | _미반영_ |

> V1 마이그레이션에 본 BC의 테이블은 정의되어 있지 않습니다. JPA `ddl-auto=update`로 생성 중이거나 향후 V 버전 추가가 필요합니다.

---

## `review_session` _(미반영)_

> 한 회차 리뷰의 진행 상태와 메타정보.

### 컬럼 (도메인 모델 기준 예상)

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | N | PK |
| `user_id` | `BIGINT` | N | 소유 사용자 (BC: User, ID 참조) |
| `deck_id` | `BIGINT` | N | 리뷰 대상 덱 (BC: Deck, ID 참조) |
| `started_at` | `DATETIME(6)` | N | 세션 시작 시각 |
| `ended_at` | `DATETIME(6)` | Y | 세션 종료 시각 (진행 중이면 `NULL`) |
| `current_index` | `INT` | N | 현재 진행 중인 카드 순번 |

### 키 / 인덱스
| 종류 | 컬럼 | 설명 |
|---|---|---|
| PK | `id` | |
| INDEX | `idx_review_session_user` (`user_id`) | 사용자별 세션 목록 조회 |

---

## `card_review` _(미반영)_

> 세션 안의 카드 한 장에 대한 공개 단계.

### 컬럼 (도메인 모델 기준 예상)

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | N | PK |
| `review_session_id` | `BIGINT` | N | FK → `review_session.id` |
| `card_id` | `BIGINT` | N | 리뷰 대상 카드 ID (BC: Card, ID 참조 — FK 미설정 검토) |
| `review_step` | `VARCHAR(20)` | N | `RECALLING` / `COMPARING` |
| `comparing_started_at` | `DATETIME(6)` | Y | COMPARING 전환 시각 (`NULL`이면 아직 전환 전) |
| `display_order` | `INT` | N | 세션 내 카드 순번 |

### 제약 / 정책
- `review_step`은 `RECALLING`으로 시작.
- `RECALLING → COMPARING` 단방향 전환만 허용 (도메인 검증).
- `comparing_started_at`은 한 번 기록 후 덮어쓰지 않음.
- BC 간 FK: `card_id`는 BC 경계상 ID만 보유하는 정책. 물리 FK 부여 여부는 운영 정책에 따라 결정.

---

## ER 관계

```
user_entity (외부)
deck (외부)
card (외부)

review_session (user_id, deck_id 외부 참조)
  └── card_review (review_session_id FK, card_id 외부 참조)
```

> 반영 마이그레이션: 없음. V 버전 추가 시 본 문서 갱신.
