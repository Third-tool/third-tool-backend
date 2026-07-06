# Issue: AxisTopic 폐기 → AxisRoadmap(1) + AxisSelection(N) 이원 축 도입

## 배경
사용자 지시:
> "이 roadmap은 layer-axis - 새축 기반의 roadmap은 크게 개선 요소가 2개가 진행 예정, layer(기능의 구현) - axis(spring framework)
> -roadmap  여기에 헌법에 해당하는 roadmap을 넣고
> -selections  - 판례에 해당하는 selections를 넣습니다."

사용자 정의:
- **roadmap = 헌법**: 축의 정체성·기준선·금지·판단 spine. axis당 1개. 프로젝트가 달라도 유지되는 원칙.
- **selections = 판례집**: 상황별 선택 조합·variant·anti-pattern·비교 축. axis당 N개, 사용자 자유 name (예: "능 아키텍처 selections v2").

각 자산은 **ASCII 트리 텍스트 도큐먼트**. 사용자 제시 예시:
```
Spring Framework 심화
├── 1. IoC & DI 핵심 원리
│   ├── 1-1. IoC 컨테이너 동작 원리
│   │       BeanFactory vs ApplicationContext — 기능 차이
│   ...
```

현재 도메인의 `AxisTopic`(다수) 계층이 이 두 축과 개념적으로 충돌한다.

## 조사 결과 — 현재 axis 하부 상태

| 항목 | 현재 |
|---|---|
| `axis_topic` | axis_id FK, name·display_order·coverage_status 보유. 다건. |
| `topic_material` | topic ↔ material 매핑. |
| Card 매달림 지점 | topic 계층 위 (이슈 #7에서 처리) |
| roadmap/selections | 도메인 부재 |

`AxisTopic`은 짧은 이름 명사구 위주의 다건 노드였고, 신 모델의 roadmap/selections(ASCII 트리 상세 도큐먼트)와 grain·의미가 다르다.

## 옵션 비교

**Option A — AxisTopic 완전 폐기 + AxisRoadmap(1) + AxisSelection(N) 신설, 원시 텍스트 저장 (채택)**
- axis 하부를 두 축(헌법·판례)으로 재구조.
- ASCII 트리는 그대로 텍스트 저장(TEXT/CLOB) — 편집·렌더·AI in/out 단순.
- 기존 axis_topic 데이터: **deprecated로 소프트 유지** (`deleted_at` 세팅). 신 운영은 roadmap/selection만.

**Option B — 구조화 재귀 엔티티(RoadmapNode 부모-자식)**
- 부분편집·검색·노드별 학습 상태 트래킹 가능.
- 스키마 복잡, ASCII 왕복 변환 필요. 초기부터 필요한 수준의 요구가 아직 확실치 않음. YAGNI.

**Option C — AxisTopic 재활용 (roadmap을 topic name 나열로)**
- 데이터 유지 유리하지만 헌법·판례 이원이 axis_topic 단일 계층에 짓눌림. 개념 표현 실패.

## 선택: Option A

## 부속 결정

### AxisRoadmap 스키마
```
axis_roadmap {
  id BIGINT PK
  axis_id BIGINT NOT NULL FK UNIQUE   -- axis당 1개 강제
  content TEXT NOT NULL               -- ASCII 트리 원문
  created_at DATETIME(6) NOT NULL
  updated_at DATETIME(6) NOT NULL
}
```
- `axis_id UNIQUE`로 1:1 강제 (도메인 검증 + DB 이중 방어).
- content는 사용자 편집 자유. AI 초안 → 사용자 수정 → 저장 흐름.
- Soft Delete 없음 (axis 삭제 시 CASCADE).

### AxisSelection 스키마
```
axis_selection {
  id BIGINT PK
  axis_id BIGINT NOT NULL FK
  name VARCHAR(200) NOT NULL          -- 사용자 자유 (예: "능 아키텍처 selections v2")
  content TEXT NOT NULL               -- ASCII 트리 원문
  created_at DATETIME(6) NOT NULL
  updated_at DATETIME(6) NOT NULL
}
CREATE INDEX idx_axis_selection_axis ON axis_selection(axis_id);
```
- 상세 정책(name unique 여부, 정렬)은 [#11 selections 버전 정책](./issue-11-selections-version-policy.md)에서 확정.
- Soft Delete 없음 (axis 삭제 시 CASCADE).

### 기존 axis_topic 이관
- **결정 확정**: **deprecated 소프트 유지** — `axis_topic.deleted_at` 일괄 세팅, 신규 UI에서 미노출. 백업 목적 보존. `topic_material`도 함께 소프트 유지.
- 신규 axis 생성 시 axis_topic 행은 만들지 않음. 신 운영은 roadmap/selection만.
- 이후 별도 이슈에서 완전 DROP 결정 가능.

### AI 초안 생성 흐름 (요약)
- roadmap: axis 확정 → `RoadmapSuggestionPort.draft(axis, concepts[], layerContext)` → ASCII 트리 초안 반환 → 사용자 편집 → 저장.
- selection: 사용자가 "새 selections 추천" → `SelectionsSuggestionPort.draft(axis, roadmapContent, variantHint)` — **roadmap 원문 필수 컨텍스트** → 초안 반환 → 사용자 편집 + name 입력 → 저장.
- Port 상세: [#9 AI 3층 확장](./issue-09-ai-suggestion-3layer.md).

## 이관 산출물

- **BE-Story #6-1**: `AxisRoadmap` · `AxisSelection` 도메인 신설. `LearningAxis.upsertRoadmap(content)`, `addSelection(name, content)`, `updateSelection(id, name?, content?)`, `removeSelection(id)` 행위.
- **BE-Story #6-2**: Flyway `V{N}__axis_roadmap_and_selection.sql` — 두 테이블 신설.
- **BE-Story #6-3**: Flyway `V{N+1}__deprecate_axis_topic.sql` — 기존 `axis_topic` 전체에 `deleted_at = NOW(6)` 세팅, `topic_material`에도 동일 적용 (soft-deprecate). 롤백 스크립트 `R{N+1}__rollback_deprecate_axis_topic.sql` 동반.
- **BE-Story #6-4**: 엔드포인트 신설:
  - `PUT /api/v1/axes/{axisId}/roadmap` (upsert)
  - `GET /api/v1/axes/{axisId}/roadmap`
  - `POST /api/v1/axes/{axisId}/selections`
  - `GET /api/v1/axes/{axisId}/selections` (목록, 정렬 기준은 #11)
  - `GET /api/v1/axes/{axisId}/selections/{selectionId}`
  - `PATCH /api/v1/axes/{axisId}/selections/{selectionId}`
  - `DELETE /api/v1/axes/{axisId}/selections/{selectionId}`
- **BE-Story #6-5**: 기존 topic 엔드포인트(`.../axes/{axisId}/topics/*`) deprecation 처리. GlobalExceptionHandler에서 신규 요청 차단(410 Gone) 여부 결정 (SDD 단계).
- **FE-Story #6-6**: AxisCardsPanel 리팩토링 — Roadmap 편집기(단일 TextArea/코드 에디터) + Selections 리스트(목록·추가·이름 편집·뷰어).
- **SDD 개정** (이슈 #8): `product-ai-interactive-roadmap.md` 개정.

## 관련 이슈 / 문서

- 이전: [#5 Layer 서버 도메인](./issue-05-layer-server-domain.md).
- 후속: [#7 Card ↔ axis 직접](./issue-07-card-axis-direct-mapping.md), [#9 AI 3층 확장](./issue-09-ai-suggestion-3layer.md), [#11 selections 버전](./issue-11-selections-version-policy.md).
- SDD 개정: [#8 용어 재정의](./issue-08-terminology-redefinition-sdd.md).
