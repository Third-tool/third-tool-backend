# Issue: AxisSelection 단일 content TEXT → 컨테이너 + 챕터 노드(Coarse) + body TEXT ASCII 통짜로 재구조

## 배경
사용자 지시 (2026-07-02 fix 회의):
> "S2: Selection도 노드화" (roadmap 노드화와 일관성 확보 + 향후 부분 편집 니즈)

이슈 #6/#11의 결정(`axis_selection.content TEXT` 통짜 저장)은 roadmap과 함께 뒤집힘 (이슈 #15와 짝). Selection도 **판례 트리 안에서 챕터 단위 조작**이 필요.

이슈 #11의 **컨테이너 정책**(name UNIQUE, created_at DESC 정렬, in-place update 허용 + 새 생성 유도, hard delete)은 컨테이너 레벨에서 유지. **노드 CRUD 정책만 새로 정의**.

## 조사 결과 — 현재 상태 (이슈 #6/#11 확정본)

| 항목 | 현재 |
|---|---|
| `axis_selection` | `id, axis_id FK, name VARCHAR(200), content TEXT, timestamps` |
| 컨테이너 정책 (#11) | `UNIQUE(axis_id, name)`, `created_at DESC` 정렬, in-place update 허용, hard delete |
| 노드 단위 조작 | 없음 |

## 옵션 비교

**Option A — 컨테이너 유지 + 챕터 노드 자식 테이블 신설 + body TEXT ASCII 통짜 (채택)**
- `axis_selection` 컨테이너(name 관리) 그대로 유지.
- `axis_selection_node` 자식 테이블 신설 — roadmap과 동일한 Coarse+body TEXT 형태.
- 이슈 #11의 컨테이너 정책은 그대로 유지.

**Option B — 컨테이너 폐기 + selection 자체가 노드 리스트**
- name UNIQUE 정책이 애매해짐. 이슈 #11 재정의 부담.

**Option C — 이슈 #11 유지 (단일 content TEXT)**
- roadmap과 일관성 손실. 사용자가 노드화 방향 선택함.

## 선택: Option A

**이슈 #11 재설계 명시**: 컨테이너 스키마(name, created_at 등)는 유지. `content TEXT` 컬럼 폐기하고 자식 노드 테이블로 이전.

## 부속 결정

### 컨테이너 스키마 (이슈 #11에서 계승 + content 제거)

```sql
CREATE TABLE axis_selection (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  axis_id BIGINT NOT NULL,
  name VARCHAR(200) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_axis_selection_axis FOREIGN KEY (axis_id)
    REFERENCES learning_axis(id) ON DELETE CASCADE,
  CONSTRAINT uq_axis_selection_name UNIQUE (axis_id, name)
);
CREATE INDEX idx_axis_selection_axis_created ON axis_selection(axis_id, created_at);
```

- 이슈 #11의 4가지 정책(name UNIQUE per axis, created_at DESC 정렬, in-place update, hard delete) 그대로.
- `content TEXT` 컬럼 폐기.

### 노드 스키마 (신규)

```sql
CREATE TABLE axis_selection_node (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  selection_id BIGINT NOT NULL,
  display_order INT NOT NULL,
  title VARCHAR(200) NOT NULL,
  rationale VARCHAR(500) NULL,
  body TEXT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_selection_node_selection FOREIGN KEY (selection_id)
    REFERENCES axis_selection(id) ON DELETE CASCADE,
  CONSTRAINT chk_selection_node_order CHECK (display_order >= 1)
);
CREATE INDEX idx_selection_node_selection_order ON axis_selection_node(selection_id, display_order);
```

- 필드 의미는 `axis_roadmap_node`와 동일 (이슈 #15 참조).
- **Soft Delete 없음** — 컨테이너 자체가 hard delete 정책이라 자식 노드도 hard delete가 자연 (conventions.md §3.4). 컨테이너 삭제 시 `ON DELETE CASCADE`로 자식 정리.

### 도메인 모델

- `AxisSelectionNode` Entity — `AxisSelection` Aggregate 소속.
- `AxisSelection`이 `nodes: List<AxisSelectionNode>` 컬렉션 소유.
- 행위:
  - `AxisSelection.addNode(title, rationale, body)` — 챕터 노드 추가, display_order = max + 1
  - `AxisSelection.reorderNodes(orderedIds)` — 순서 재부여
  - `AxisSelection.removeNode(nodeId)` — Hard delete
  - `AxisSelectionNode.updateTitle/updateRationale/updateBody` — 입력 정규화 + blank 검증
- `LearningAxis`가 `selections: List<AxisSelection>` 그대로 유지 (이슈 #6/#11 결정 계승).

### API 표면

| 메서드 | 경로 | 목적 |
|---|---|---|
| POST | `/api/v1/axes/{axisId}/selections` | selection 컨테이너 생성 (`name` 만) — 이후 노드 add로 채움 |
| GET | `/api/v1/axes/{axisId}/selections` | 컨테이너 목록 (`created_at DESC`) |
| GET | `/api/v1/axes/{axisId}/selections/{selectionId}` | 컨테이너 + 노드 목록 통합 조회 |
| PATCH | `/api/v1/axes/{axisId}/selections/{selectionId}` | name 편집 (in-place update) |
| DELETE | `/api/v1/axes/{axisId}/selections/{selectionId}` | Hard delete (자식 노드 CASCADE) |
| POST | `/api/v1/selections/{selectionId}/nodes` | 챕터 노드 추가 |
| PATCH | `/api/v1/selection-nodes/{nodeId}` | 노드 부분 편집 |
| DELETE | `/api/v1/selection-nodes/{nodeId}` | 노드 hard delete |
| PUT | `/api/v1/selections/{selectionId}/nodes/order` | 순서 재부여 |

기존 `PATCH .../selections/{id} { content }` 폐기.

### 이슈 #6/#11 데이터 이관

- 저장된 `axis_selection.content` (있을 경우) — 챕터 단위 파싱해서 `axis_selection_node`로 분할. best-effort + 실패 로깅.
- v1 초기(3명 사용자) 시점엔 데이터 극소량 → 이슈 #15와 동일 정책.
- 기존 `content` 컬럼 즉시 DROP하지 않고 컬럼 RENAME으로 아카이브 (`_archived_content`), 다음 이슈에서 DROP 결정.

## 이관 산출물

- **BE-Story #16-1**: `AxisSelectionNode` 도메인 신설. `AxisSelection` Aggregate 확장.
- **BE-Story #16-2**: Flyway `V{N}__axis_selection_node.sql` + `V{N+1}__archive_axis_selection_content.sql` (컬럼 rename). 롤백 스크립트 동반.
- **BE-Story #16-3**: 파싱 마이그레이션 (기존 `content` → 노드 분할).
- **BE-Story #16-4**: 신규 엔드포인트 세트 (위 표) + 기존 `.../content` 부분 폐기.
- **BE-Story #16-5**: ErrorCode 신설 (`SELECTION_NODE_NOT_FOUND` 404, `SELECTION_NODE_TITLE_BLANK` 400, `SELECTION_NODE_BODY_BLANK` 400 등). 컨테이너 관련(`AXIS_SELECTION_NAME_ALREADY_EXISTS`)은 #11에서 계승.
- **FE-Story #16-6**: Selections 뷰어 리팩토링 — 컨테이너 리스트 + 컨테이너 내부 챕터 노드 카드 UI + 재생성 버튼(이슈 #18).
- **SDD 개정** (이슈 #8): `product-learning-tower.md`.

## 관련 이슈 / 문서

- 뒤집는 이슈: [#6 Roadmap/Selection dual-axis](./issue-06-roadmap-selections-dualaxis.md) — Selection도 `content TEXT` 결정 뒤집힘.
- 재설계 이슈: [#11 Selections 버전 정책](./issue-11-selections-version-policy.md) — 컨테이너 정책은 유지, 노드 CRUD 정책 신설.
- 짝 이슈: [#15 Roadmap 노드 모델](./issue-15-roadmap-node-model.md) — 스키마 형태 동일.
- 관련: [#17 AI 2단계 생성](./issue-17-ai-two-step-generation.md), [#18 노드 재생성 API](./issue-18-node-regeneration-with-hint.md).
- SDD 개정: `product-learning-tower.md`.
