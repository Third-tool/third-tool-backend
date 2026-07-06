# Issue: AxisRoadmap 단일 content TEXT → 챕터 노드(Coarse) + body TEXT ASCII 통짜로 재승격

## 배경
사용자 지시 (2026-07-02 fix 회의):
> "요구사항 수정 자체가 ai 품질 증가 때문에 요구하겠습니다. (...) 페르소나(백엔드/기획자/기능의 구현 + 하네스 엔지니어링) + 관점(수렴/헌법 vs 발산/판례) 프레임으로 뽑아내면 그 자체 품질은 사람이 보고서 만족할 수 있겠다고 생각합니다. (...) 단점은, 중간중간 수정에 대한 부족, 너무 많은 양의 roadmap이 나오는 문제, 그 전체적으로 수정에 대해서 의견을 넣는 기능에 대해서도 부족할 것이라고 생각했습니다."

이슈 #6의 결정("AxisRoadmap = axis 단위 1개 `content TEXT` 통짜 저장")은 다음 세 부족을 안고 있음:
- **분량** — 통짜 트리가 방대해 소화 어려움
- **중간 수정** — 부분만 고치기 어렵고 통짜 편집으로 몰림
- **의견 반영** — "이 부분은 이렇게" 사용자 의견을 AI에 다시 태울 통로 없음

**목표**: 이슈 #6의 ASCII 편집·diff 강점은 유지하되, **챕터 단위를 first-class 노드로 승격**해 챕터 단위 재생성·추가·삭제·순서변경이 자연스러운 스키마로 재정의.

## 조사 결과 — 현재 상태 (이슈 #6 확정본)

| 항목 | 현재 |
|---|---|
| `axis_roadmap` | `id, axis_id UNIQUE, content TEXT, timestamps` — axis당 1개 |
| 편집 API | `PUT /api/v1/axes/{axisId}/roadmap { content }` 통짜 덮어쓰기 |
| 재생성 단위 | 축 전체 통짜 (`RoadmapSuggestionPort.draft` 1회) |
| 챕터 단위 조작 | 없음 (문자열 splice로만 가능) |

## 옵션 비교

**Option A — 챕터 노드 first-class 승격 + body TEXT ASCII 통짜 (채택)**
- `axis_roadmap_node(id, axis_id, display_order, title, rationale, body TEXT)` — 챕터 row 하나에 subtree(1-1~1-N + 리프 본문 포함) ASCII 통짜.
- 사용자 mental model("노드 하나 = 챕터 subtree 통짜")과 일치.
- 이슈 #6의 강점(ASCII 직접 편집·diff 자연) 유지.
- 챕터 단위 재생성·추가·삭제·순서변경 자연스러움.
- 섹션 단위 재생성은 v1엔 body TEXT 직접 편집(PATCH)으로 커버.

**Option B — Medium 관계형 (챕터 row + 섹션 row 각각)**
- `axis_roadmap_node(id, axis_id, parent_id NULL, display_order, title, body)` — 챕터·섹션 각각 row.
- 섹션 단위 재생성 API가 정갈해지지만 스키마·API·프론트 편집기 모두 복잡도 상승.
- 사용자가 재확인 질문에서 이 방향을 명시적으로 정정함.

**Option C — 이슈 #6 유지 (단일 content TEXT)**
- 부분 조작 API 없음. 세 부족 해결 못함.

## 선택: Option A (Coarse + body TEXT ASCII 통짜)

**이슈 #6 뒤집기 명시**: `axis_roadmap` 테이블(단일 `content TEXT`) 폐기. `axis_roadmap_node`로 대체. 이슈 #6의 "ASCII 그대로 저장" 원칙은 body 필드 안에서 유지.

## 부속 결정

### 스키마

```sql
CREATE TABLE axis_roadmap_node (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  axis_id BIGINT NOT NULL,
  display_order INT NOT NULL,
  title VARCHAR(200) NOT NULL,
  rationale VARCHAR(500) NULL,
  body TEXT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  deleted_at DATETIME(6) NULL,
  CONSTRAINT fk_roadmap_node_axis FOREIGN KEY (axis_id)
    REFERENCES learning_axis(id) ON DELETE CASCADE,
  CONSTRAINT chk_roadmap_node_order CHECK (display_order >= 1)
);
CREATE INDEX idx_roadmap_node_axis_order ON axis_roadmap_node(axis_id, display_order);
CREATE INDEX idx_roadmap_node_deleted ON axis_roadmap_node(deleted_at);
```

- `display_order` 1-based (conventions.md §1.4).
- `title` = "1. 하네스 엔지니어링 기초" 같은 챕터 제목.
- `rationale` = AI가 왜 이 챕터를 넣었는지 짧은 이유. 사용자 편집 가능.
- `body` = 챕터 subtree ASCII 통짜. 사용자 예시(하네스 로드맵 챕터 subtree)와 동일 형태:
  ```
  ├── 1-1. 정의와 본질
  │       모델 + 하네스 — 에이전트 = 추론 엔진 + 런타임
  │       "모델 아니면 하네스" — 비모델 코드 전체가 하네스
  │       ...
  ```
- **Soft Delete 적용** (`deleted_at` + `@SQLRestriction`) — axis-level soft delete 자산 대열에 합류 (ADR021 계열 확장).

### 도메인 모델

- `AxisRoadmapNode` Entity — `axis` Aggregate에 속함.
- `LearningAxis`가 `roadmapNodes: List<AxisRoadmapNode>` 컬렉션 소유.
- 행위:
  - `LearningAxis.addRoadmapNode(title, rationale, body)` — 새 챕터 노드 추가, display_order = max + 1
  - `LearningAxis.reorderRoadmapNodes(orderedIds)` — 순서 재부여 (conventions.md §1.4)
  - `LearningAxis.removeRoadmapNode(nodeId)` — Soft Delete
  - `AxisRoadmapNode.updateTitle(newTitle)` / `updateRationale(newRationale)` / `updateBody(newBody)` — 입력 정규화(trim) + blank 검증
- 이슈 #6에서 예정된 `LearningAxis.upsertRoadmap(content)` 폐기.

### API 표면 (신 규격)

| 메서드 | 경로 | 목적 |
|---|---|---|
| GET | `/api/v1/axes/{axisId}/roadmap-nodes` | 챕터 노드 목록 (display_order ASC) |
| POST | `/api/v1/axes/{axisId}/roadmap-nodes` | 챕터 노드 추가 (`title, rationale?, body`) |
| PATCH | `/api/v1/roadmap-nodes/{nodeId}` | 부분 필드 편집 (`title?, rationale?, body?`) |
| DELETE | `/api/v1/roadmap-nodes/{nodeId}` | Soft Delete |
| PUT | `/api/v1/axes/{axisId}/roadmap-nodes/order` | `orderedIds: [long]` 순서 재부여 |

기존 `PUT /api/v1/axes/{axisId}/roadmap { content }` 폐기 — 신규 요청 시 410 Gone.

### 이슈 #6 데이터 이관

- 이미 저장된 `axis_roadmap.content` (있을 경우) — 챕터 단위로 파싱해서 노드 row로 분할하는 마이그레이션. 파싱 실패 시 통짜 "1. (전체)" 단일 노드로 보관 후 사용자가 수동 재구조화.
- v1 초기(3명 사용자) 시점엔 저장된 데이터가 없거나 극소량 → 이관 스크립트는 best-effort로 작성하고 통과 못한 데이터는 별도 로깅.
- `axis_roadmap` 테이블은 RENAME으로 `_archived_axis_roadmap`으로 아카이브 (conventions.md §3.8).

## 이관 산출물

- **BE-Story #15-1**: `AxisRoadmapNode` 도메인 신설. `LearningAxis` Aggregate 확장 (`addRoadmapNode`, `reorderRoadmapNodes`, `removeRoadmapNode` 등).
- **BE-Story #15-2**: Flyway `V{N}__axis_roadmap_node.sql` — 신설 + `V{N+1}__archive_axis_roadmap.sql` (기존 테이블 아카이브). 롤백 `R{N}__` / `R{N+1}__` 동반.
- **BE-Story #15-3**: 파싱 마이그레이션 (기존 `axis_roadmap.content` → 챕터 노드 분할). best-effort + 실패 로깅.
- **BE-Story #15-4**: 신규 엔드포인트 세트 (위 표) + 이슈 #6 엔드포인트 410 Gone.
- **BE-Story #15-5**: ErrorCode 신설 (`ROADMAP_NODE_NOT_FOUND` 404, `ROADMAP_NODE_TITLE_BLANK` 400, `ROADMAP_NODE_BODY_BLANK` 400 등).
- **FE-Story #15-6**: Roadmap 편집기 리팩토링 — 챕터 노드 카드 UI, 각 카드는 body TEXT 편집기 + 재생성 버튼(이슈 #18).
- **SDD 개정** (이슈 #8): `product-learning-tower.md` 스키마·도메인 섹션 개정.

## 관련 이슈 / 문서

- 뒤집는 이슈: [#6 Roadmap/Selection dual-axis](./issue-06-roadmap-selections-dualaxis.md) — `content TEXT` 통짜 결정 뒤집힘. ASCII 저장 원칙은 body 필드 안에서 계승.
- 관련: [#16 Selection 노드 모델](./issue-16-selection-node-model.md), [#17 AI 2단계 생성](./issue-17-ai-two-step-generation.md), [#18 노드 재생성 API](./issue-18-node-regeneration-with-hint.md), [#19 개념 명세](./issue-19-roadmap-selection-concept-spec.md).
- SDD 개정: `product-learning-tower.md`.
