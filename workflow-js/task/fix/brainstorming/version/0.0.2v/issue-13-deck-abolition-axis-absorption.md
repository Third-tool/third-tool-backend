# Issue: Deck 도메인 완전 폐기 → LearningAxis 흡수

## 배경
사용자 지시:
> "Axis 아래에 Card가 들어가게 되면 Deck도 필요없어지는지 설계 확인 부탁"

이슈 [#1](./issue-deck-axis-integration.md)에서 "axis=deck"으로 이미 통합됐고, [#7](./issue-07-card-axis-direct-mapping.md)에서 Card가 axis에 직접 매달림으로 확정됐다. 이 상태에서 **Deck.cards 집합과 Axis.cards 집합은 완전 동일**하고, Axis 1 : Deck 1 관계이므로 Deck의 독립 존재 이유가 소멸한다. Deck은 관심사 분리(SRP) 관점으로만 정당화 가능하지만, 이번 리팩토링에서 두 개념을 통합해 도메인·API·마이그레이션 부담을 줄인다.

## 조사 결과 — 현재 Deck 책임 목록

| # | 책임 | 위치 | 신 계층에서 |
|---|---|---|---|
| 1 | `progressStatus` (NOT_STARTED/IN_PROGRESS/COMPLETED) | `Deck.java:187-200` | Axis로 이동 |
| 2 | `axisId` NOT NULL 종속 | `Deck.java:83-84` | Axis 자신, 불필요 |
| 3 | `learningMaterialId` (nullable, `markMaterialDeleted()`) | `Deck.java:86-87, 161-163` | Axis로 이동 |
| 4 | Soft Delete cascade (Card orphanRemoval) | `Deck.java:247-255` | Axis 소속 Card로 재정의 |
| 5 | `lastAccessed` | `Deck.java:42` | Axis로 이동 |
| 6 | `DeckMode` 운영 모드 | `Deck.java:53` | `AxisMode`로 rename 이동 |
| 7 | `parentDeck / depth / subDecks` 계층 | Deck.java | **미사용 확인 — 완전 drop** |
| 8 | `onLibrary / publishedAt` 공유 라이브러리 | Deck.java | Axis로 rename 이동 (미출시 기능, 향후 재설계) |

Card FK: `card.deck_id` NOT NULL. [#7](./issue-07-card-axis-direct-mapping.md)의 `card.axis_id` 신설과 함께 `card.deck_id` 제거.

## 옵션 비교

**Option A — 완전 폐기 → Axis 흡수 (채택)**
- Deck 도메인 삭제. axis에 8개 책임 흡수 (subDecks는 drop).
- API prefix `/decks` → `/axes` 일괄 통합.
- 도메인 단순화 극대, 마이그레이션 1회로 종료.

**Option B — Slim Deck 유지 (SRP)**
- Axis=학습 정체성 / Deck=학습 진행 상태 분리.
- 두 Aggregate 1:1 payload 조율 매 트랜잭션마다 발생. 오버헤드 지속.

**Option C — 결정 보류**
- 이슈 대기 상태로 두면 하위 이슈(#14 Review 재설계)가 진행 불가.

## 선택: Option A

## 부속 결정

### Axis 흡수 컬럼 스키마 (초안)
```sql
ALTER TABLE learning_axis
  ADD COLUMN progress_status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
  ADD COLUMN axis_mode VARCHAR(20) NOT NULL DEFAULT '<기존 DeckMode 기본값>',
  ADD COLUMN last_accessed DATETIME(6) NULL,
  ADD COLUMN learning_material_id BIGINT NULL,
  ADD COLUMN on_library BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN published_at DATETIME(6) NULL,
  ADD CONSTRAINT fk_axis_material FOREIGN KEY (learning_material_id) REFERENCES learning_material(id),
  ADD CONSTRAINT chk_axis_progress_status CHECK (progress_status IN ('NOT_STARTED','IN_PROGRESS','COMPLETED')),
  ADD CONSTRAINT chk_axis_mode CHECK (axis_mode IN ('<enum values>'));
```
- 컬럼명은 SDD 단계에서 확정 (필요 시 축약).
- `axis_mode` enum 값은 기존 `DeckMode` 그대로 계승.

### Card FK 변경
- `card.deck_id` 컬럼 폐기 (soft-deprecate: nullable 전환 + 신규 미사용, 후속 story에서 DROP).
- `card.axis_id`는 [#7](./issue-07-card-axis-direct-mapping.md) Story에서 이미 신설.

### API 통합 매핑
| 기존 | 신 |
|---|---|
| `POST /api/v1/decks` (이미 폐기됨, #1) | 폐기 유지 |
| `GET /api/v1/decks/{id}` | `GET /api/v1/axes/{id}` (axis 응답에 흡수 필드 포함) |
| `PATCH /api/v1/decks/{id}` | `PATCH /api/v1/axes/{id}` |
| `DELETE /api/v1/decks/{id}` | `DELETE /api/v1/axes/{id}` (기존 axis soft delete와 통합) |
| `GET /api/v1/decks/{id}/cards` | `GET /api/v1/axes/{axisId}/cards` (이미 존재, 172409c) |
| `POST /api/v1/axes/{axisId}/decks` (Fix-Story 2) | 자기 참조라 폐기 (axis 생성 자체가 옛 deck 생성) |

### Soft Delete cascade 재정의
- 기존: Deck soft delete → 소속 Card cascade.
- 신: Axis soft delete → 소속 Card cascade (도메인 행위 `LearningAxis.softDelete()`에서 orchestration).

### 이벤트 정리
- 기존 `LearningAxisCreatedEventHandler`(axis 생성 시 deck 자동 생성) 폐기.
- Axis 생성 자체가 옛 deck 역할이므로 이벤트·핸들러 제거.

## 이관 산출물

- **BE-Story #13-1**: `Deck` Aggregate 폐기 준비 — 흡수 대상 필드를 `LearningAxis`로 이관 (`axis.progressStatus`, `axis.mode`, `axis.lastAccessed`, `axis.learningMaterialId`, `axis.onLibrary`, `axis.publishedAt`). `LearningAxis.recalculateProgressStatus()`, `updateLastAccessed()`, `markMaterialDeleted()`, `updateMode()` 행위 추가.
- **BE-Story #13-2**: Flyway `V{N}__axis_absorbs_deck.sql` — 위 컬럼 추가·백필 (`UPDATE learning_axis a JOIN deck d ON d.axis_id = a.id SET a.progress_status = d.progress_status, a.axis_mode = d.mode, ...`).
- **BE-Story #13-3**: `Card.deckId` FK 제거 마이그레이션 (`V{N+1}__card_drop_deck_id.sql`). Repository 쿼리 (`findAllByDeckIdAndDeletedFalse` 등) axis 스코프로 재작성.
- **BE-Story #13-4**: `deck` 테이블 soft-deprecate — `V{N+2}__deprecate_deck_table.sql`로 전 행에 `deleted_at = NOW(6)` 세팅. 롤백 스크립트 `R{N+2}__rollback_deprecate_deck.sql`. 최종 DROP은 후속 Story.
- **BE-Story #13-5**: `DeckController`·`DeckService`·`DeckRepository` 삭제. Deck 관련 도메인 예외·ErrorCode → axis 스코프로 rename (예: `DECK_NOT_FOUND` → `AXIS_NOT_FOUND` 통합).
- **BE-Story #13-6**: `LearningAxisCreatedEventHandler`·관련 이벤트 클래스 폐기.
- **BE-Story #13-7**: API 통합 매핑 — 기존 `/decks/*` 엔드포인트 제거 또는 axis 리다이렉트.
- **FE-Story #13-8**: FE에서 deck 참조·훅 제거, axis 응답으로 통합. progress·mode·lastAccessed 표시 UI를 axis 컴포넌트로 이동.
- **Docs-Story #13-9**: `docs/DOMAIN.md`에서 Deck 개념 제거, Axis 확장 서술. `docs/PACKAGE.md`에서 Deck BC 제거. 신규 ADR: "Deck 폐기 결정 기록".

## 결정 대기 (TBD)

- **`axis_mode` enum 값 최종 결정**: 기존 DeckMode 그대로 rename만 vs 통합 기회에 재정의.
- **`learning_material_id`의 axis 직접 매달림 vs 별도 매핑 (`AxisMaterialLink`)**: 매핑 다대다 필요 여부. 초안은 axis 직접 (기존 Deck과 동일).

## 관련 이슈 / 문서

- 이전: [#7 Card ↔ axis 직접 매핑](./issue-07-card-axis-direct-mapping.md) — Card FK 변경과 함께 하나의 마이그레이션 클러스터.
- 후속: [#14 Review 전략 재설계](./issue-14-review-strategy-axis-layer-scope.md) — Deck 폐기가 Review 도메인 재설계의 전제.
- 관련: [#1 deck-axis 통합](./issue-deck-axis-integration.md) — 이 이슈가 자연스러운 종착점.
- 관련: [#5 Layer 서버 도메인](./issue-05-layer-server-domain.md) — Layer.progressStatus는 [#14](./issue-14-review-strategy-axis-layer-scope.md)에서 도입.
- 관련: [#12 기존 이슈 정합](./issue-12-existing-issues-alignment.md) — 클러스터 순서 반영.
- 관련 문서: `product-card.md`(0.0.1v Done)의 Deck·Layer 1 어휘 개정은 [#8](./issue-08-terminology-redefinition-sdd.md) 목록에 편입.
