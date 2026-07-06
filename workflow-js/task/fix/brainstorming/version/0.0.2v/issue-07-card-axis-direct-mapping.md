# Issue: Card ↔ axis 직접 매핑 (topic 개념 완전 폐기)

## 배경
[#6](./issue-06-roadmap-selections-dualaxis.md)에서 `AxisTopic`이 폐기되고 axis 하부가 `AxisRoadmap` + `AxisSelection`으로 재구조된다. 현재 `Card`는 topic 계층에 매달려 있어 매달릴 곳이 사라진다.

사용자는 도메인 단순화를 요청함 — Card는 신 구조에서 axis에 직접 매달림.

## 조사 결과 — 현재 Card 도메인 상태

| 항목 | 현재 |
|---|---|
| Card FK | `card.topic_id → axis_topic.id` |
| TopicMaterial 매핑 | `topic_material { topic_id, material_id }` |
| ReviewSession 집계 | topic 단위 (LearningFacade Layer 1 커버리지) |
| Card 조회 API | `GET /api/v1/axes/{axisId}/cards` (신설됨, 172409c) — 이미 axis 스코프 조회 존재 |

이미 축 스코프 카드 조회 API가 최근 신설(172409c)되어 있어, 신 매핑 방향(Card↔axis 직접)과 부합하는 흐름이 이미 시작되어 있다.

## 옵션 비교

**Option A — Card.axisId 직접 FK, topic 완전 폐기 (채택)**
- `card.topic_id` → `card.axis_id` 마이그레이션.
- `topic_material` 삭제(또는 soft-deprecate; #6과 정합).
- Review session 집계 로직을 axis 단위로 재작성.
- 도메인 단순화, 조회 성능 유리.

**Option B — Card가 roadmap/selection의 서브 노드 텍스트 참조 (soft link)**
- Card에 `node_key VARCHAR` (예: "1-1-1") 필드로 트리 노드 참조.
- content가 원시 텍스트라 리네이밍/구조 변경 시 link가 조용히 깨짐. 정합성 취약.

**Option C — Card 도메인 완전 독립 (map 계층과 무관)**
- 사용자 지시(카드는 학습 진행의 산출물)와 배치.

## 선택: Option A

## 부속 결정

### 기존 카드 데이터 이관
- 현재 `card.topic_id`에서 각 topic의 소속 axis id로 백필. 데이터 손실 없음.
- 마이그레이션 순서:
  1. `card.axis_id BIGINT NULL` 컬럼 추가
  2. `UPDATE card c JOIN axis_topic t ON c.topic_id = t.id SET c.axis_id = t.axis_id` (백필)
  3. `card.axis_id NOT NULL` 강제
  4. `card.topic_id` 컬럼 유지(soft-deprecate) 또는 DROP 결정 — 기존 axis_topic이 soft-deprecate(#6)이므로 `card.topic_id`도 함께 소프트 유지 권장 (백업 정합성 유지).

### TopicMaterial 폐기 정책
- **결정**: `topic_material` 테이블 소프트 유지(#6과 동일 정책). 신규 매핑 미생성. 신 운영에서는 Card ↔ LearningMaterial 관계가 필요하면 별도 이슈(카드 자료 매핑 재설계) 도출.

### Review Session 집계 재정의
- 현재 topic 커버리지 기반 집계 → **axis 커버리지 기반**으로 전환.
- coverage_status는 axis 단위로 이동 (roadmap 존재 여부·selections 개수 등으로 재정의 가능 — SDD 단계에서 상세).
- ADR005의 UserScheduleConfig 관계는 변경 없음.

### #3 maxDuration UI와의 관계
- 기존 이슈 #3(maxDuration 모드 매핑 화면)이 Card 매핑 계층 변경에 영향받음. #12에서 정합 재검토.

## 이관 산출물

- **BE-Story #7-1**: Card 도메인 (`card.topicId` → `card.axisId`). `Card.create(axisId, ...)` 팩토리 시그니처 변경. 도메인 검증(axis 존재 확인)은 Application Service.
- **BE-Story #7-2**: Flyway `V{N}__card_axis_direct.sql` — 컬럼 추가·백필·NOT NULL 3단계.
- **BE-Story #7-3**: `topic_material`·`axis_topic` soft-deprecate는 [#6-3](./issue-06-roadmap-selections-dualaxis.md)에서 이미 처리 (본 이슈에서는 참조만).
- **BE-Story #7-4**: ReviewSession 집계 로직 axis 단위 재작성. `CoverageRecalculator` 시그니처·트리거 재정의.
- **BE-Story #7-5**: 기존 topic 관련 Card 엔드포인트 정리 (`.../topics/{topicId}/cards` 등이 있다면 제거 또는 axis 리다이렉트). QueryDSL Q클래스 재생성.
- **FE-Story #7-6**: 카드 목록·생성·수정 UI에서 topic 참조 제거, axis 스코프로 통일. AxisCardsPanel(이미 axis 스코프)는 유지, 나머지 화면 정리.

## 관련 이슈 / 문서

- 이전: [#6 Roadmap/Selection 이원 축](./issue-06-roadmap-selections-dualaxis.md) — Topic 폐기가 이 이슈의 전제.
- 후속: [#13 Deck 폐기 → Axis 흡수](./issue-13-deck-abolition-axis-absorption.md) — 본 이슈의 `card.axis_id` 신설과 `card.deck_id` 제거가 하나의 마이그레이션 클러스터.
- 후속: [#14 Review 전략 재설계](./issue-14-review-strategy-axis-layer-scope.md) — Card→axis 매핑이 Review 도메인 재편의 전제.
- 관련: [#3 maxDuration UI](./issue-maxduration-mode-ui.md), [#12 기존 이슈 정합](./issue-12-existing-issues-alignment.md).
- 관련 문서: `docs/adr/ADR005.md` (Card ↔ UserScheduleConfig) — 관계 유지, 별도 개정 불요.
