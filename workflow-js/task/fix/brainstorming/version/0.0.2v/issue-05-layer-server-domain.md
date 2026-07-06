# Issue: Layer 서버 도메인 승격 (concept → layer → axis 3계층)

## 배경
사용자 지시:
> "그 layer 예를 들면 백엔드 개발자에 기능의 구현 layer를 만들고 그 layer 안에 axis를 통해서 기능의 구현을 잘하기 위해 axis로 spring framework를 집어넣고 (...) 백엔드 개발자, 기획자 조합이면 layer로 기능의 구현, 기획, 문제 해결, 데이터베이스 등으로 잡고, 그 layer 하부는 그 layer topic을 커버하기 위한 (...) axis"

현재 map의 3계층은 `concept → axis → topic`이고, **layer는 FE `MapGroup`으로 로컬 state만** 존재한다 (`src/features/map/MapPage.tsx` line 35-39). 서버는 layer 개념 자체가 없어 axis가 LearningFacade에 직접 매달림. 신 모델은 axis를 layer 하위로 매달아 concept 조합 컨텍스트가 layer 그룹핑을 통해 표현된다.

## 조사 결과 — 현재 계층 구조

| 계층 | BE 위치 | FE 위치 | 서버 저장 |
|---|---|---|---|
| concept | `learning_facade.concept` | `MapPage.identity` | ✅ |
| layer | **부재** | `MapGroup` (로컬 state) | ❌ |
| axis | `learning_axis` (facade_id FK) | `MapTrack` | ✅ |
| topic | `axis_topic` (axis_id FK) | `MapNode` | ✅ |

axis→FK는 현재 `learning_axis.facade_id`. Layer 도입 시 이 FK가 `layer_id`로 재지향되어야 한다.

## 옵션 비교

**Option A — `learning_layer` 신설 + axis FK 재지향 (채택)**
- `learning_layer { id, facade_id, name, display_order, deleted_at, ... }` 신설.
- `learning_axis.facade_id` → `learning_axis.layer_id`로 FK 재지향.
- 도메인: LearningFacade → LearningLayer(1:N) → LearningAxis(1:N).
- 마이그레이션: 기존 axes 각각의 소속 layer를 결정해야 함(부속 결정 참조).

**Option B — layer를 axis 메타데이터(태그)로만 표현**
- `learning_axis.layer_tag VARCHAR` 추가. FE만 그룹핑.
- 승격 아님 — 사용자 지시(서버 도메인 승격)와 배치.

**Option C — layer=facade로 매핑 (1 facade = 1 layer)**
- 도메인 단순화지만 concepts[] 조합이 여러 layer를 요구하므로 개념 무너짐.

## 선택: Option A

## 부속 결정

### 기존 axes 이관 정책 (Layer 매핑 부재)
- 기존 데이터는 layer 개념 없이 만들어졌으므로 이관 시 **facade별로 기본 layer "Uncategorized" 1개를 생성해 소속시킴**. 사용자는 이후 layer 편집으로 재배치.
- 마이그레이션 순서: (1) `learning_layer` 테이블 생성 → (2) facade별 기본 layer INSERT → (3) `learning_axis.layer_id NULL 컬럼 추가` → (4) 기본 layer id로 백필 → (5) `layer_id NOT NULL` 강제 → (6) `learning_axis.facade_id` FK 폐기(또는 유지 여부는 이슈 본문 부속 결정 참고).

### `facade_id` 컬럼 유지 여부
- **결정 대기**: axis에서 facade_id를 유지하면 조회 쿼리 단순(레이어 조인 없이 축 리스트), 단 두 FK로 정합성 부담. 원칙적으로 layer_id만 두고 조회는 join 사용이 표준. TBD — SDD 단계에서 확정.

### Layer Soft Delete
- LearningFacade·LearningAxis가 이미 Soft Delete 대상(ADR021). `learning_layer.deleted_at DATETIME(6) NULL` 동일 적용. 부모 facade Soft Delete 시 하위 layer 연쇄 soft delete.

### Layer 진행률 필드
- [#14 Review 전략 재설계](./issue-14-review-strategy-axis-layer-scope.md)에서 `learning_layer.progress_status VARCHAR(20)` 필드와 `LearningLayer.recalculateProgressStatus()` 도메인 행위를 도입한다.
- 파생 규칙: 소속 axes의 progress_status 조합 (모두 COMPLETED → COMPLETED / 하나라도 IN_PROGRESS → IN_PROGRESS / 모두 NOT_STARTED → NOT_STARTED).
- 본 이슈(#5)의 Flyway V파일은 골격만 담고, `progress_status` 컬럼은 #14의 마이그레이션과 함께 추가하거나 본 이슈 V파일에 미리 포함할지 SDD 단계에서 결정.

### Layer 정렬·중복
- `display_order` 1-based, `learning_facade.addLayer()`로만 부여.
- 동일 facade 내 layer name 중복 금지 (`UNIQUE(facade_id, name, deleted_at)` composite).

### axis-deck 통합(#1)과의 관계
- #1은 "axis가 만들어지면 deck도 함께 생성"으로 결정됨. 신 모델에서 axis는 layer 하위이지만 axis-deck 관계는 그대로 유지 (deck.axis_id 유지).
- 단, "축=덱" 정책이 layer 단위 집계 요구를 만들 수 있음(예: layer 단위 진행률). 이건 #12에서 정합 재검토.

## 이관 산출물

- **BE-Story #5-1**: `LearningLayer` 도메인 신설 (Aggregate 자식으로 LearningAxis 이관). `LearningFacade.addLayer()`, `LearningLayer.addAxis()` 행위 추가.
- **BE-Story #5-2**: Flyway `V{N}__learning_layer.sql` — 위 부속 결정의 6단계 마이그레이션.
- **BE-Story #5-3**: `learning_axis` 관련 Repository 쿼리·QueryDSL Q클래스 재정렬 (facade_id 조회 경로 → layer 경유).
- **BE-Story #5-4**: Layer CRUD 엔드포인트: `POST /facades/{id}/layers`, `PATCH /layers/{id}`, `DELETE /layers/{id}`, `PUT /facades/{id}/layers/order`, `POST /layers/{id}/axes` (기존 `/facades/{id}/axes`는 deprecation).
- **FE-Story #5-5**: `MapGroup` 로컬 state → 서버 연동. 신규 훅 `useAddLayer`, `useRenameLayer`, `useReorderLayers`, `useDeleteLayer` 추가.
- **SDD 개정 필요분** (이슈 #8 목록에 추가): `docs/DOMAIN.md`에 Layer 개념 추가, `docs/PACKAGE.md`에 계층 재정의.

## 관련 이슈 / 문서

- 이전: [#4 concepts[]](./issue-04-concept-list.md) — concepts 조합이 layer 추천의 입력.
- 다음: [#6 Roadmap/Selection 이원 축](./issue-06-roadmap-selections-dualaxis.md).
- 관련: [#12 기존 이슈 정합](./issue-12-existing-issues-alignment.md) — deck-axis 통합(#1)과 layer 계층의 관계 재검토.
- 관련 문서: `docs/adr/` 신규 ADR 필요 (Layer 계층 도입 결정 기록).
