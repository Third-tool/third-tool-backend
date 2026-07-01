# ADR021: Deck 생성은 Axis 이벤트 자동 경로로 유일화하고 LearningAxis를 Soft Delete 대상으로 승격한다

- **상태**: Accepted
- **날짜**: 2026-07-01
- **관련**: fix-axis-deck-full-integration (0.0.2v) — `workflow/task/fix/sdd/version/0.0.2v/fix-axis-deck-full-integration.md`, 선행 [ADR020](ADR020-deck-axis-visibility.md), [ADR003](ADR003.md) (Soft Delete 정책), [ADR007](ADR007.md) (동기 도메인 이벤트), `docs/DOMAIN.md` §2.1 LearningFacade / §2.2 Deck, `docs/PACKAGE.md` §6 BC 의존 규칙

## 컨텍스트

선행 [ADR020](ADR020-deck-axis-visibility.md)과 fix-deck-axis-visibility(0.0.2v)는 Deck↔Axis 가시화를 read-model로 해결하면서, 축 결합 정책으로 **한 축당 자동 생성 1개 + 사용자 명시 추가 N개 + 고아 덱 보존**의 세 진입점을 병존시켰다.

로컬 테스트에서 두 가지 문제가 드러났다.

1. **사용자 지시("Axis = 덱, Deck 자체는 스스로 만들어지지 않았으면 좋겠다")와 정책의 괴리** — 사용자 시각에서는 축이 곧 덱인데 코드는 여전히 사용자 명시 진입점(`POST /api/v1/decks`, `POST /learning-facade/axes/{axisId}/decks`)을 열어둔다. Deck.of / createUnderAxis 두 팩토리도 함께 남아있다.
2. **"카드 만들 때 축이 인식 안 되고 화면 나가면 사라진다"의 근본 원인 — LearningAxis Hard Delete** — `LearningFacade.removeAxis()`가 `axes.remove(target)` + `orphanRemoval=true`로 `learning_axis` row 자체를 지운다. `deck.axis_id`는 `fk_deck_axis ON DELETE SET NULL`이라 Deck은 유지되지만 자동 축과의 연결만 조용히 끊어져 today/축뷰가 카드를 인식하지 못한다.

Deck×Axis 정책을 "축=덱"으로 완성하고, LearningAxis도 Card/Deck/LearningFacade와 동일한 Soft Delete 정책으로 통일할 시점이다.

## 결정

두 갈래로 결정한다.

### D1. Deck 생성 = Axis 생성 이벤트 자동 경로만

**폐기**: `POST /api/v1/decks`, `POST /api/v1/learning-facade/axes/{axisId}/decks`, `Deck.of(name, parentDeck, user)`, `Deck.createUnderAxis(user, axisId, name)`, `DeckCommandService.create/createUnderAxis`, `LearningFacadeCommandService.createDeckUnderAxis`, `LearningFacadeRequest.CreateAxisDeck`, `LearningFacadeCommand.CreateAxisDeck`, `DeckRequest.Create`.

**남는 유일 진입점**: `LearningAxisCreatedEventHandler` → `Deck.createFromAxis(user, axisId, name)`.

- `deck.axis_id`를 `NOT NULL`로 승격 (Flyway V15). 기존 고아 Deck(`axis_id IS NULL, deleted = FALSE`)은 soft delete로 자동 아카이브 후 NULL row는 hard delete로 정리 (관측 영향 0 — 이미 축 뷰/today에서 필터되던 상태).
- `DeckQueryService`의 null axisId 방어 코드(`resolveAxisNames` / `resolveAxisName` / `lookupAxisName`) 제거.
- FK `fk_deck_axis ON DELETE SET NULL`은 안전망으로 유지 — D2 결정으로 Axis Hard Delete 자체가 발생하지 않으므로 실제 발동 경로는 없다.

### D2. LearningAxis에 `deleted_at` 도입, Axis→Deck 연쇄 소프트 삭제

**LearningAxis**를 `Card`, `Deck`, `LearningFacade`, `LearningMaterial`, `User`와 동일한 Soft Delete 정책으로 승격한다. [ADR003](ADR003.md)의 "구조 편집·연결 사실 기록. 복원 요구 낮음" 분류에서 사용자 자산성 도메인으로 재분류.

- `learning_axis`에 `deleted_at DATETIME(6) NULL` 컬럼 + `idx_learning_axis_deleted` 추가 (Flyway V14).
- 도메인 클래스에 `@SQLRestriction("deleted_at IS NULL")` + `softDelete()` + `isDeleted()` 추가.
- `LearningFacade.axes` OneToMany는 **`orphanRemoval=false`**로 낮춤 — `axes.remove()` 호출 자체가 코드에서 제거됐고 미래 리팩토링(`axes.removeIf(LearningAxis::isDeleted)` 등)에서 조용히 hard delete되는 회귀 트랩을 원천 차단.
- `LearningFacade.removeAxis()`는 `target.softDelete()`만 호출. `getAxes`, `findAxis`, `addAxis`, `validateAxisNameDuplicate`, `reorderAxes`, `isAxisCountExceedsRecommended`, `getCoverageSummary`, `hasUncoveredTopics` 8개 지점은 활성 축만 필터.
- Application Service(`LearningFacadeCommandService.removeAxis`)는 다음 순서로 조율한다: `facade.removeAxis(axisId)` → `facadeRepository.save(facade)` → `deckCommandService.softDeleteByAxisId(axisId)`. Axis softDelete flush 후 Deck 연쇄가 실행되도록 관측 순서와 코드 순서를 일치시켰다.
- `LearningAxis`의 `(facade_id, name)` UNIQUE 제약은 `(facade_id, name, deleted_at)` 3-column composite로 재정의 (V14 + `@Table` 어노테이션). MySQL이 NULL 조합을 unique 검사에서 서로 다른 값으로 취급하므로 활성 축끼리만 유일 보장하면서 소프트 삭제된 축 이름 재사용을 허용한다.

## 결과 (Consequences)

### 긍정적

- **사용자 지시와 코드의 정합** — 축=덱 1:1 정책이 스키마·API·팩토리·컨트롤러 전 레이어에서 일관.
- **회귀 트랩 원천 차단** — 이슈 2("축이 사라짐")의 후보 1(orphanRemoval hard delete)과 후보 2(임시 Deck 이탈 시 soft delete) 모두 봉쇄. axisId=null Deck 자체가 스키마 레벨에서 불가.
- **API 표면 축소** — Deck 관련 컨트롤러 엔드포인트 -2, 도메인 팩토리 -2, 서비스 메서드 -3, DTO record -3. 방어 코드(null axisId 브랜치) 3곳 제거.
- **Soft Delete 정책 통일** — LearningAxis도 `Card`, `Deck`, `LearningFacade`와 동일한 데이터 보존 정책. 향후 축 복원 UX 여지 확보.
- **선행 결정(ADR020)의 read-model 노출 방향 유지** — `deck.axis_id`는 raw `Long` 그대로. 도메인 연관 승격은 여전히 거부. 본 ADR은 그 위에 "생성 경로의 유일화"만 추가한다.

### 트레이드오프 / 부정적

- **자유 덱 사용 사례 미래 재개설 부담** — "축에 묶이지 않은 자유 덱"이 미래에 필요해지면 새 진입점을 다시 열어야 한다. 현재로선 사용 사례가 실증되지 않았고, 남겨두면 이슈 2 재현 표면으로 남는다는 이유로 폐지.
- **`@SQLRestriction` 벤더 종속** — Card·Deck의 관행은 `boolean deleted` + Repository 명시 필터. LearningAxis만 Hibernate `@SQLRestriction`을 채택해 관행이 갈렸다. `.claude/rules/conventions.md` §3.4가 `@SQLRestriction` 예시를 명시했고, 현대 Hibernate 관행이라 채택했지만 프로젝트 전체를 일관되게 정리하는 후속 리팩토링은 남는다.
- **UNIQUE 재정의의 MySQL NULL 취급 의존** — `(facade_id, name, deleted_at)` composite unique가 원하는 동작을 하는 이유는 MySQL이 NULL을 unique 검사에서 서로 다른 값으로 취급하기 때문. 다른 DBMS(예: PostgreSQL 표준)로 이전할 경우 partial index 사용으로 다시 검토해야 한다.
- **`LEARNING_AXIS_ALREADY_DELETED("LA005")`가 dead code** — `LearningFacade.findAxis`가 활성 축만 반환하므로 Application 경로에서 도달 불가. 도메인 안전망으로만 유지. YAGNI 관점 제거 여지가 있으나 향후 restore/직접 도메인 조작 경로를 열 때 활성화되는 안전망으로 보존.
- **축당 다중 Deck 유연성 상실** — [ADR020](ADR020-deck-axis-visibility.md)의 "축당 다중 덱 허용"이 본 ADR로 폐기된다. 축당 1 Deck 확정. 미래 UX 요구가 오면 별도 결정으로 재개.

## 대안 비교

| 대안 | 장점 | 거부 사유 |
| --- | --- | --- |
| **Option A — Axis 자동 생성만 (채택)** | 사용자 지시와 1:1. 회귀 표면 최소. `axis_id NOT NULL`로 스키마 단순화 | 자유 덱 미래 사용 사례 재개설 부담 |
| Option B — 자동 + 수동 축 스코프 병존 (선행 ADR020 채택안) | 사용자가 한 축에 Deck 추가 여지 | 축=덱 의미와 어긋남. 임시 Deck 이탈 시 soft delete로 사라지는 이슈 2 후보 2가 남음 |
| Option C — 3개 다 유지, deprecation만 표기 | 안전 | 이슈 본질(축=덱) 미해결. 정리 지연 |
| Option D — LearningAxis Hard Delete + FK ON DELETE CASCADE로 Deck·Card까지 연쇄 | 응답 즉시 정합 | Card는 Soft Delete인데 부모 삭제로 조용히 사라지는 정책 불일치 |
| Option E — 축 삭제 자체 금지(Deck 남으면 예외) | 안전 | UX 불편, "축=자산" 개념 훼손 |

Option D·E는 D2 결정 검토 시 거부.

## 다시 검토할 시점

- **자유 덱 필요성 실증** — 사용자가 "축에 묶이지 않은 개인 덱"을 요구하면 새 진입점 재개설 여부 재평가.
- **`@SQLRestriction` vs `boolean deleted` 관행 통일** — 프로젝트 전체 Soft Delete 구현을 하나의 관행으로 통일하는 리팩토링. Card·Deck을 `@SQLRestriction`으로 마이그레이션할지, LearningAxis를 `boolean deleted`로 되돌릴지.
- **`LEARNING_AXIS_ALREADY_DELETED` 활성화 시점** — LearningAxis restore UX를 도입하거나 직접 도메인 조작 경로가 열릴 때. 아니면 dead code 제거.
- **`fk_deck_axis ON DELETE SET NULL` 안전망 유지 여부** — Axis Hard Delete가 실제로 발생할 경로가 없으므로 `RESTRICT`로 강제할지, 그대로 유지할지.
- **product-deck.md 완성** — Deck BC를 자체 Product로 완성하는 planning 사이클. 본 ADR의 결정을 Design Decisions에 편입.
