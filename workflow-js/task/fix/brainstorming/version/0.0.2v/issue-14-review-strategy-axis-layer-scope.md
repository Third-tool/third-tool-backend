# Issue: Review 전략 axis 단위 재설계 + Layer 진행률·학습 세션 + "Layer 1" 용어 개정

## 배경
사용자 지시:
> "이후에 Card 기반의 Review 전략 또한 Card Maxview 등과 함께 설계가 어떻게 수정되는지 확인해주세요"

[#7](./issue-07-card-axis-direct-mapping.md) Card→axis 직접 매핑과 [#13](./issue-13-deck-abolition-axis-absorption.md) Deck 폐기로, Review 도메인의 참조·집계 계층이 근본적으로 재편된다:
- ReviewSession은 Deck 참조 → **Axis 참조**로 이동
- 카드 수집 쿼리는 `findAllByDeckId...` → **`findAllByAxisId...`**
- MaxView·MaxDuration 트리거 chain 안 `deck.recalculateProgressStatus()` → **`axis.recalculateProgressStatus()`**
- 신 Layer 도입([#5](./issue-05-layer-server-domain.md))으로 **Layer 단위 진행률·학습 세션** 새 요구
- `product-card.md`의 "Layer 1" 어휘(=LearningFacade 전체 수집)가 신 개념 Layer(백엔드→기능의 구현)와 충돌 → 개정 필요

## 조사 결과 — 현재 Review 흐름 및 관측 정책

### ReviewCommandService 현 흐름
```
startReview(deckId)
  ↓ cardRepository.findAllByDeckIdAndDeletedFalse(deck.getId())
  ↓ ReviewSession.of(deck, cards, user, cards.size())

ReviewSession.recordCurrentCardView(OnFieldBudget)
  ↓ card.recordView()
  ↓ card.isLastView(maxView) 판정
  ↓ card.archive() + CardStatusHistoryAppender.append()
  ↓ card.getDeck().recalculateProgressStatus()   ← 폐기 대상
  ↓ cardRepository.save()
```

### 관측 정책
| 항목 | 현재 | 신 계층 영향 |
|---|---|---|
| `UserScheduleConfig` | 사용자 레벨 싱글톤 (`UNIQUE(user_id)`) | 변경 없음 |
| `OnFieldBudget` VO | Card 도메인. `resolveReason(card)` = MAX_VIEW > MAX_DURATION | 변경 없음 |
| `CardExpiryPolicy` Domain Service | maxView/maxDuration 판정 | 변경 없음 |
| MaxView 트리거 | Application Layer (ReviewCommandService) | 위치 유지, 집계 대상만 axis로 |
| MaxDuration 자동 archive | 배치 예정 (미구현) | 변경 없음. 별도 이슈 |
| "Layer 1" 어휘 | `product-card.md`에서 LearningFacade 전체 수집 의미 | 신 개념 Layer와 충돌 → 개정 |

### `parentDeck/subDecks` 계층 (사용 안 함, [#13](./issue-13-deck-abolition-axis-absorption.md)에서 drop)
- Review에서 계층 순회 로직 없음 → Review 재설계에 영향 없음.

## 옵션 비교

### ReviewSession 스코프 확장
**Option A — Axis 세션(기본) + Layer 세션(신설) 2단계 (채택)**
- 기본 학습 단위는 axis. Layer 세션은 layer 하위 axes의 카드를 통합 수집.
- 사용자가 "이 layer 전체를 오늘 학습" 시나리오 지원.

**Option B — Axis 세션만**
- Layer 진행률만 파생, 세션 자체는 axis 단위로만 국한.
- 사용자 결정(Layer 진행률/세션 도입)과 배치.

**Option C — LearningFacade 전체 세션 신설(현 "Layer 1" 확장)**
- 지나친 범위. 현재도 유사 요구 미확인.

### Layer 진행률 파생 규칙
**Option A — 3단계 파생 (채택)**
- 모두 COMPLETED → COMPLETED
- 하나라도 IN_PROGRESS → IN_PROGRESS
- 모두 NOT_STARTED → NOT_STARTED
- 세부 비율(percent) 필드는 후속 이슈(TBD).

**Option B — 진행률 백분율 필드 직접 계산**
- 초기 오버스펙. Axis 개수 변동 시 계산 부담. YAGNI.

**Option C — Layer 진행률 미도입**
- 사용자 결정(포함)과 배치.

## 선택
- ReviewSession 스코프: **Option A** (Axis 세션 + Layer 세션 2단계)
- Layer 진행률 파생 규칙: **Option A** (3단계, 비율은 후속)

## 부속 결정

### 신 흐름
```
startReview(axisId)  또는  startReview(layerId)
  ↓ axis 세션: cardRepository.findAllByAxisIdAndDeletedFalse(axisId)
  ↓ layer 세션: cardRepository.findAllByLayerIdAndDeletedFalse(layerId)   ← 신 쿼리
  ↓ ReviewSession.of(scopeRef, cards, user, cards.size())

ReviewSession.recordCurrentCardView(OnFieldBudget)
  ↓ card.recordView()
  ↓ card.isLastView(maxView) 판정 (OnFieldBudget) — 로직 유지
  ↓ card.archive() + CardStatusHistoryAppender.append() — 유지
  ↓ card.getAxis().recalculateProgressStatus()   ← Deck → Axis
  ↓ axis.getLayer().recalculateProgressStatus()  ← 신 cascade
  ↓ cardRepository.save()
```

### 신 엔드포인트
| 메서드 | 경로 | 목적 |
|---|---|---|
| POST | `/api/v1/axes/{axisId}/review-sessions` | axis 스코프 세션 시작 |
| POST | `/api/v1/layers/{layerId}/review-sessions` | layer 스코프 세션 시작 (신설) |
| POST | `/api/v1/review-sessions/{id}/record-view` | 현 카드 조회 기록 (기존과 동일한 body) |
| GET | `/api/v1/review-sessions/{id}` | 세션 상태 조회 |

기존 `/api/v1/decks/{deckId}/review-sessions`(있다면) 폐기 — [#13](./issue-13-deck-abolition-axis-absorption.md)에서 함께.

### ReviewSession 스키마 변경
- 참조 필드: `deckId` → `scopeType` (`AXIS` | `LAYER`) + `scopeId` (axis_id 또는 layer_id).
- 또는 두 nullable 필드(`axisId`, `layerId`) 중 하나만 세팅. **선택은 SDD 단계에서 확정** (표현 명료성 vs 스키마 단순성).

### Layer 진행률 스키마
- `learning_layer` 테이블에 `progress_status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED'` 추가.
- CHECK: `progress_status IN ('NOT_STARTED','IN_PROGRESS','COMPLETED')`.
- 도메인: `LearningLayer.recalculateProgressStatus()` — 소속 axes 상태 파생.

### 트랜잭션·성능
- `axis.recalculateProgressStatus() → layer.recalculateProgressStatus()` cascade는 동일 트랜잭션에서 처리.
- Layer가 대량 axes를 가지면 recalculate 비용 증가. 초기에는 도메인 계산 (axis 수 소량 가정). 대량 시나리오는 후속 최적화 이슈.

### "Layer 1" 용어 개정 대상
- `product-card.md` 안 "Layer 1", "Layer 1 전체 수집", "Layer 1 스코프" 등 표현.
- 개정 후 어휘: **"LearningFacade 스코프"** 또는 **"Facade-wide"** (신 개념 Layer와 명확히 구분).
- 개정 항목은 [#8 SDD 개정](./issue-08-terminology-redefinition-sdd.md) 목록에 편입.

### UserScheduleConfig / OnFieldBudget / CardExpiryPolicy
- 로직·스키마 모두 유지. 사용자 레벨 싱글톤이라 계층 이동과 무관.
- 다만 참조 코드에서 `card.getDeck()` 호출은 `card.getAxis()`로 rename.

## 이관 산출물

- **BE-Story #14-1**: `ReviewSession` Aggregate 리팩토링 — 참조를 `Deck` → `Axis` + `Layer` 스코프 표현으로 재작성. `scopeType/scopeId` 도입 결정 후 반영.
- **BE-Story #14-2**: `CardRepository`에 `findAllByAxisIdAndDeletedFalse`, `findAllByLayerIdAndDeletedFalse` 쿼리 추가. `findAllByDeckId...` 제거는 [#13](./issue-13-deck-abolition-axis-absorption.md)에서.
- **BE-Story #14-3**: `ReviewCommandService` `startReview` / `recordCurrentCardView` axis·layer 스코프 지원. MaxView chain 안 `axis.recalculate → layer.recalculate` cascade 반영.
- **BE-Story #14-4**: `LearningLayer` 도메인에 `progressStatus` 필드 + `recalculateProgressStatus()` 도메인 행위 추가. Flyway `V{N}__learning_layer_progress.sql`.
- **BE-Story #14-5**: `LearningAxis.recalculateProgressStatus()` 안에서 `layer.recalculateProgressStatus()` orchestration (Application Service에서 호출).
- **BE-Story #14-6**: 신 API 엔드포인트 추가 (`/axes/{id}/review-sessions`, `/layers/{id}/review-sessions`).
- **FE-Story #14-7**: Review 시작 UI에서 스코프 선택 (axis / layer). Progress 표시 UI를 axis·layer 각각 지원.
- **Docs-Story #14-8** ([#8](./issue-08-terminology-redefinition-sdd.md)에 편입): `product-card.md`의 "Layer 1" → "LearningFacade 스코프" 개정.

## 결정 대기 (TBD)

- **ReviewSession 스키마**: `scopeType/scopeId` 단일 컬럼 vs `axisId/layerId` 두 nullable 컬럼. SDD 단계 확정.
- **Layer.progressPercent** (세부 비율) 도입 여부. 파생 규칙 A 첫 실사용 이후 결정.
- **MaxDuration 자동 archive 배치**: 현재 미구현. 신 계층에서 배치 스코프(user × axis vs user × layer) 결정. 별도 후속 이슈 도출.

## 관련 이슈 / 문서

- 이전: [#7](./issue-07-card-axis-direct-mapping.md), [#13](./issue-13-deck-abolition-axis-absorption.md) — Card FK·Deck 폐기가 본 이슈의 전제.
- 관련: [#5](./issue-05-layer-server-domain.md) — Layer 도메인 (progressStatus 필드 추가는 본 이슈에서 처리).
- SDD 개정 편입: [#8](./issue-08-terminology-redefinition-sdd.md) — `product-card.md` 개정 대상 문서 표에 편입.
- 관련: [#3 maxDuration UI](./issue-maxduration-mode-ui.md) — maxDuration 표시 어휘가 axis/layer 스코프로 옮겨짐.
- 원본: `workflow/task/pes/workspectrum/sdd/done/versions/0.0.1v/product-card.md` (Review 도메인 · Layer 1 어휘 기원).
