# Card 테이블 정의서

> Card BC가 다루는 카드 본체 + 회상 단서 + 태그 + 상태 이력 데이터 영역.

> 작성 규칙은 [`_rules.md`](_rules.md) 참조. 도메인 모델은 [`docs/domain/card.md`](../domain/card.md) 참조.

---

## 테이블 목록

| 테이블 | 역할 | 도메인 매핑 | 마이그레이션 |
|---|---|---|---|
| `card` | 학습 카드 본체. MainNote/Summary 인라인 | `Card` (Aggregate Root) | V1 |
| `keyword_cue` | 카드의 회상 단서 (1:N) | `KeywordCue` | V1 |
| `card_tag` | 카드-태그 N:M 매핑 | `CardTag` | _미반영_ |
| `tag` | 전역 태그 마스터 | `Tag` | _미반영_ |
| `card_status_history` | 카드 상태 전환 시계열 | `CardStatusHistory` | _미반영_ |

> _미반영_ 표시 항목은 V1 마이그레이션에 정의되어 있지 않습니다. JPA `ddl-auto=update` 또는 향후 V2~ 마이그레이션 추가가 필요합니다.

---

## `card`

> 학습 카드 본체. MainNote(`@Embedded`)와 Summary(`@Embedded`)가 인라인됩니다.

### 컬럼

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | N | - | PK |
| `deck_id` | `BIGINT` | N | - | 소속 덱 (BC: Deck) |
| `main_text_content` | `TEXT` | Y | `NULL` | MainNote 본문 텍스트 |
| `main_image_url` | `VARCHAR(2048)` | Y | `NULL` | MainNote 이미지 URL |
| `main_content_type` | `VARCHAR(20)` | N | - | `TEXT_ONLY` / `IMAGE_ONLY` / `MIXED` |
| `summary_value` | `TEXT` | N | - | Summary 본문 |
| `deleted` | `TINYINT(1)` | N | `0` | Soft Delete 플래그 |
| `created_date` | `DATETIME(6)` | N | - | 생성 시각 |
| `updated_date` | `DATETIME(6)` | Y | `NULL` | 수정 시각 |

### 키 / 인덱스

| 종류 | 컬럼 | 설명 |
|---|---|---|
| PK | `id` | |
| FK | `deck_id` → `deck.id` | |

### 향후 추가 예정 컬럼 (도메인 모델에는 존재)

- `status` (`ON_FIELD` / `ARCHIVE`) — `CardStatus`
- `entered_field_at` (`DATETIME(6)`) — ON_FIELD 진입 시각
- `view_count` (`INT`) — 현재 ON_FIELD 구간 노출 횟수
- `last_viewed_at` (`DATETIME(6)`) — 마지막 노출 시각
- `deleted_at` (`DATETIME(6)`) — Soft Delete 시각

### 제약 / 정책
- Soft Delete: `deleted = 1`이면 조회 대상에서 제외(애플리케이션 레벨 필터).
- `main_text_content`와 `main_image_url`은 둘 중 최소 하나가 NOT NULL이어야 함 (도메인 검증).
- Summary는 1-3문장 범위 (도메인 검증).

### 도메인 매핑
| 컬럼 | 도메인 |
|---|---|
| `main_text_content`, `main_image_url`, `main_content_type` | `Card.mainNote` (`MainNote @Embedded`) |
| `summary_value` | `Card.summary.value` (`Summary @Embedded`) |

---

## `keyword_cue`

> 카드의 회상 단서. 카드당 최소 1개 강제(도메인 규칙).

### 컬럼

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `keyword_cue_id` | `BIGINT AUTO_INCREMENT` | N | - | PK |
| `value` | `VARCHAR(200)` | N | - | 단서 텍스트 |
| `card_id` | `BIGINT` | N | - | 소속 카드 |

### 키 / 인덱스

| 종류 | 컬럼 | 설명 |
|---|---|---|
| PK | `keyword_cue_id` | |
| FK | `card_id` → `card.id` | CASCADE ALL (Card 삭제 시 함께 제거) |

---

## `card_tag` _(미반영)_

> Card-Tag N:M 매핑을 명시화한 중간 테이블.

### 컬럼 (도메인 모델 기준 예상)

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | N | PK |
| `card_id` | `BIGINT` | N | FK → `card.id` |
| `tag_id` | `BIGINT` | N | FK → `tag.tag_id` |
| `linked_at` | `DATETIME(6)` | N | 연결 시각 |

### 제약
- UNIQUE: `(card_id, tag_id)` — 중복 연결 방지.
- 카드당 최대 3건 (도메인 검증).

---

## `tag` _(미반영)_

> 전역 태그 마스터. 사용자/덱 무관하게 전체 시스템에서 공유.

### 컬럼 (도메인 모델 기준 예상)

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| `tag_id` | `BIGINT AUTO_INCREMENT` | N | PK |
| `value` | `VARCHAR(100)` | N | 태그 이름 |

### 제약
- UNIQUE: `value` — 시스템 전체 유니크.

---

## `card_status_history` _(미반영)_

> 카드 상태 전환 이력 시계열.

### 컬럼 (도메인 모델 기준 예상)

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | N | PK |
| `card_id` | `BIGINT` | N | FK → `card.id` |
| `from_status` | `VARCHAR(20)` | N | 전환 전 |
| `to_status` | `VARCHAR(20)` | N | 전환 후 |
| `changed_at` | `DATETIME(6)` | N | 전환 시각 |

### 제약
- `from_status != to_status` (도메인 검증).
- CASCADE: Card 삭제 시 함께 제거(orphanRemoval).

---

## ER 관계

```
deck (외부)
  └── card (deck_id FK)
        ├── keyword_cue (card_id FK, CASCADE)
        ├── card_tag (card_id FK)        ── 미반영
        │     └── tag (tag_id FK)         ── 미반영
        └── card_status_history (card_id FK, CASCADE) ── 미반영
```

> 반영 마이그레이션: V1 (card, keyword_cue). 나머지는 추가 V 버전 필요.
