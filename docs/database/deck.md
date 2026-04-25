# Deck 테이블 정의서

> Deck BC가 다루는 카드 컨테이너 데이터 영역.

> 작성 규칙은 [`_rules.md`](_rules.md) 참조. 도메인 모델은 [`docs/domain/deck.md`](../domain/deck.md) 참조.

---

## 테이블 목록

| 테이블 | 역할 | 도메인 매핑 | 마이그레이션 |
|---|---|---|---|
| `deck` | 카드 컨테이너. 자기 참조로 계층 구조 표현 | `Deck` (Aggregate Root) | V1 |

---

## `deck`

> 사용자별 카드 컨테이너. 부모-자식 자기 참조로 트리 구조를 표현하고, 공개 라이브러리 등록 상태를 보유.

### 컬럼

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | N | - | PK |
| `name` | `VARCHAR(100)` | N | - | 덱 이름 |
| `last_accessed` | `DATETIME(6)` | Y | `NULL` | 마지막 접근 시각 |
| `scoring_algorithm_type` | `VARCHAR(50)` | N | - | 학습 점수 알고리즘 타입 |
| `on_library` | `TINYINT(1)` | N | `0` | 공개 라이브러리 등록 여부 |
| `published_at` | `DATETIME(6)` | Y | `NULL` | 공개 시각(정렬용) |
| `parent_deck_id` | `BIGINT` | Y | `NULL` | 부모 덱 (최상위면 `NULL`) |
| `depth` | `INT` | N | `0` | 계층 깊이 (최상위 = 0) |
| `user_id` | `BIGINT` | N | - | 소유 사용자 (BC: User) |

### 키 / 인덱스

| 종류 | 컬럼 | 설명 |
|---|---|---|
| PK | `id` | |
| UNIQUE | `uk_deck_user_name` (`user_id`, `name`) | 사용자별 이름 중복 방지 |
| FK | `parent_deck_id` → `deck.id` | 자기 참조 |
| FK | `user_id` → `user_entity.id` | 외부 BC(User) 참조 |

### 제약 / 정책
- `parent_deck_id IS NULL` ↔ `depth = 0` (도메인 검증).
- `parent_deck_id IS NOT NULL` ↔ `depth = parent.depth + 1` (도메인 검증).
- 부모 변경 시 `depth`는 `DeckHierarchyService`가 재귀 재계산.
- `on_library = 0`인 동안 `published_at`은 `NULL` (도메인 규칙).

### 도메인 매핑
| 컬럼 | 도메인 |
|---|---|
| `scoring_algorithm_type` | `Deck.deckMode` (`DeckMode @Enumerated`) |

> Soft Delete를 사용하지 않는 경우 본 테이블에는 `deleted` 컬럼이 없습니다. 삭제 정책 확인 후 본 표 갱신 필요.

---

## ER 관계

```
user_entity (외부)
  └── deck (user_id FK)
        └── deck (parent_deck_id FK, 자기 참조)
              └── card (deck_id FK)  → docs/database/card.md
```

> 반영 마이그레이션: V1.
