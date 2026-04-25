# 테이블 정의서 작성 규칙

> 도메인별 테이블 정의서(`card.md`, `deck.md`, `review.md`, `learning-facade.md`, `user.md`, `user-schedule.md`)를 작성·유지할 때 따라야 할 형식과 컨벤션입니다.
> 새 테이블을 추가하거나 기존 스키마를 변경할 때 이 파일을 먼저 확인하세요.

---

## 1. 파일 분리 원칙

- BC(Bounded Context) **하나당 파일 하나**를 둡니다 (도메인 모델 문서와 1:1 매칭).
- 파일명은 `docs/domain/`과 동일한 kebab-case (`card.md`, `learning-facade.md`).
- 파일 위치: `docs/database/{bc-name}.md`.
- 한 BC가 여러 테이블을 가지면 한 파일 안에 테이블 단위 섹션으로 나열합니다 (별도 파일로 쪼개지 않습니다).

## 2. 표준 섹션 구조

```markdown
# {BC명} 테이블 정의서

> 한 줄 요약 — 이 BC가 다루는 데이터 영역.

---

## 테이블 목록

| 테이블 | 역할 | 도메인 매핑 |
|---|---|---|
| `card` | 학습 카드 본체 | `Card` (Aggregate Root) |

---

## `{table_name}`

> 한 줄 정의.

### 컬럼

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|

### 키 / 인덱스

| 종류 | 컬럼 | 설명 |
|---|---|---|
| PK | `id` | |
| FK | `user_id` → `users.id` | |
| UNIQUE | `(user_id, name)` | 사용자별 이름 중복 방지 |
| INDEX | `idx_status` (`status`) | 상태별 조회 빈번 |

### 제약 / 정책
- Soft delete 여부, cascade 정책, 트리거 등을 글머리 기호로 명시.

### 도메인 매핑
- `card.main_text_content` → `Card.mainNote.textContent`
- 매핑이 1:1이 아닌 컬럼만 적습니다 (자명한 매핑은 생략).

---

## ER 관계

PlantUML / Mermaid / ASCII 중 한 형식으로 BC 내부 관계를 그립니다. BC 외부 참조는 화살표 끝에 `(외부)` 표기.

```
users (외부)
  └── decks (user_id FK)
        └── cards (deck_id FK)
              └── keyword_cues (card_id FK, CASCADE ALL)
```
```

## 3. 컬럼 작성 규칙

- **타입**: SQL 기준으로 적습니다 (`BIGINT`, `VARCHAR(255)`, `TEXT`, `DATETIME`, `ENUM('A','B')`).
- **NULL**: `NOT NULL`이면 `N`, nullable이면 `Y`.
- **기본값**: 명시적으로 적습니다 (`CURRENT_TIMESTAMP`, `0`, `FALSE`, `NULL`). 없으면 `-`.
- **설명**: 컬럼이 표현하는 비즈니스 의미. JPA 어노테이션 이름이 아니라 의미를 적습니다.
- 감사 컬럼(`created_date`, `updated_date`, `deleted`, `deleted_at`)은 표 상단이 아니라 **하단**에 배치해 비즈니스 컬럼이 먼저 보이게 합니다.

## 4. 명명 규칙

- 테이블명·컬럼명은 `snake_case`. 코드의 camelCase와 매핑할 때 `@Column(name="...")` 명시.
- 외래 키 컬럼은 `{참조_테이블_단수형}_id` (예: `card_id`, `user_id`).
- 중간 테이블명은 양 테이블 명을 단수형으로 연결 (`card_tag`, `action_material`).
- 인덱스 명은 `idx_{table}_{컬럼}`, 유니크는 `uk_{table}_{컬럼}`.

## 5. 마이그레이션과의 정합성

- 실제 스키마는 Flyway 마이그레이션(`src/main/resources/db/migration/V*__*.sql`)이 정답입니다. 본 문서는 그 스냅샷을 사람이 읽을 수 있게 정리한 것입니다.
- 스키마 변경 시: **새 V 버전 마이그레이션 추가** → **본 문서 갱신** 순서. 기존 V 파일은 절대 수정하지 않습니다 (Flyway 정합성 깨짐).
- 마이그레이션 버전과 본 문서의 갱신 시점을 명확히 하려면 섹션 끝에 `> 반영 마이그레이션: V3__add_archive_reason.sql` 같은 줄을 둡니다.

## 6. BC 외부 참조

- 다른 BC의 테이블을 FK로 참조할 때는 컬럼 설명에 `(BC: User)` 식으로 출처 BC를 명시합니다.
- BC 간 직접 객체 참조 대신 ID 참조를 사용한다는 아키텍처 원칙([`docs/architecture/package-structure-guide.md`](../architecture/package-structure-guide.md))과 일치하는지 확인합니다.

## 7. 기존 모놀리식 문서

- 통합 테이블 정의서는 [`테이블 정의서.md`](./테이블 정의서.md)에 있습니다 (현재는 Notion 링크 + 관계도만). 점진적으로 도메인별 파일로 옮기고 통합 문서는 인덱스 역할로 축소합니다.

## 8. 외부 단일 진실원

- 운영 중인 전체 ERD/테이블 정의는 Notion에서 관리됩니다([링크](https://alive-balance-aba.notion.site/Thirdtool-31d016c1c241802a937ad05b668912b5)). 본 문서는 코드 변경과 함께 이동하는 *코드-인접* 정의서이며, 외부 인터뷰·산출물 공유는 Notion을 우선합니다.
