# Issue: Deck×Axis 완전 통합 — Axis 생성이 곧 Deck 생성

## 배경
사용자 지시:
> "Axis가 만들어지면 Deck과 연동되어서 같이 만들어져야한다. Axis가 곧 덱과 의미가 동일하다.
> 학습지도에서 로드맵에서 새로운 Axis가 만들어졌으면 덱도 같이 만들어져야 한다.
> Deck 자체는 스스로 만들어지기는 없었으면 좋겠고, 학습 로드맵에서 Axis를 만들 때마다만 덱이 같이 만들어졌으면 좋겠습니다."

최근 `fix/axis-scoped-deck-create` 브랜치에서 Fix-Story 1(Deck 응답 axisId 노출) · Fix-Story 2(축 스코프 Deck 생성 API)까지 진행했지만, **고아 Deck 생성 경로 · 팩토리가 여전히 존재**해 의미 일원화가 미완이다.

## 조사 결과 — 현재 Deck 생성 3중 경로

| # | 진입점 | 팩토리 | axisId |
|---|---|---|---|
| 1 | `POST /api/v1/decks` (`DeckController.java:25-32`) | `Deck.of()` (`Deck.java:116-130`) | null 허용 |
| 2 | `POST /learning-facade/axes/{axisId}/decks` (Fix-Story 2) | `Deck.createUnderAxis()` (`Deck.java:175-194`) | 필수 |
| 3 | `LearningAxisCreatedEventHandler` (이벤트 자동) | `Deck.createFromAxis()` (`Deck.java:140-159`) | 필수 |

DB: `deck.axis_id BIGINT NULL`, FK `fk_deck_axis ON DELETE SET NULL`.

## 옵션 비교

**Option A — 축 자동 생성만 남기기 (채택)**
- 사용자 지시와 1:1 일치. `Deck.of()` / `createUnderAxis()` 제거. `axis_id NOT NULL` 강제.
- 마이그레이션 부담 있음.

**Option B — 자동 + 수동 축 스코프 유지**
- 고아 경로만 제거, 수동 축 스코프 유지. 사용자가 한 축에 Deck 추가 여지.
- 축=덱 의미와 어긋남. 사용자 지시와 배치.

**Option C — 3개 다 유지, deprecation만 표기**
- 안전하지만 이슈 본질(축=덱) 미해결.

## 선택: Option A

## 부속 결정

### 기존 고아 Deck 마이그레이션 정책
- 채택: **Soft delete 자동 적용** (`deleted=true, deleted_at=now()`) 후 `axis_id NOT NULL` 강제.
- 데이터 보존, 복원 의미는 없음 (Axis 무연결이므로).

### Axis 삭제 시 Deck 처리
- 현재: LearningAxis Hard Delete만 지원 (`deleted_at` 없음). orphanRemoval + FK `ON DELETE SET NULL`이라 Axis 삭제 시 Deck의 axisId만 null이 되고 Deck은 유지 → 축=덱 정책과 모순.
- 채택: **LearningAxis에 `deleted_at` 추가, Soft Delete 정책 통일**. `axis.softDelete()` 시 소속 Deck들도 연쇄 soft delete.
- 이슈 2의 최유력 원인(orphanRemoval hard delete)을 원천 봉쇄하는 부수 효과.

## 이관 산출물

- **BE-Story 1** (`fix/axis-soft-delete-cascade`): LearningAxis Soft Delete 전환 + Axis→Deck 연쇄 soft delete
- **BE-Story 2** (`fix/deck-single-create-path`): Deck 생성 경로 통합 + axis_id NOT NULL + 고아 Deck 마이그레이션
- 순서: Story 1 → Story 2 (Story 2 마이그레이션이 Story 1의 Soft Delete를 전제로 하기 때문)
