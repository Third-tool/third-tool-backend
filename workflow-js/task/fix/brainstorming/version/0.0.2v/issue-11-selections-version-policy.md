# Issue: Selections 버전·name 관리 정책

## 배경
사용자 예시:
> "그리고 각각 roadmap은 하나에서 유지되고 selections는 roadmap을 기반으로 많은 양은 아니더라도 버전 별로 추천을 해달라고 하면 능 아키텍처 selections v2 (...) 요런 식으로 추천하는 구조"

selections는 axis당 N개. 사용자 예시에서 name에 "v2"가 붙어 있다 — 사용자가 자유롭게 버전/이름을 붙여 축적하는 방식이 자연스럽다. 정책 세부는 [#6](./issue-06-roadmap-selections-dualaxis.md)에서 별도 이슈로 분리하기로 결정.

## 조사 결과 — 결정 필요 축

| 축 | 초안 |
|---|---|
| name 자유도 | 사용자 자유 문자열, 시맨틱 버전 강제 X |
| name unique 스코프 | 축 스코프 unique? |
| 정렬 정책 | 최근 생성 우선? name 순? 사용자 수동? |
| 갱신 정책 | 새 selection 생성 vs 기존 in-place update |
| 삭제 정책 | soft delete vs hard delete |

## 옵션 비교

### name unique 스코프
- **A. 축 스코프 unique (`UNIQUE(axis_id, name)`)** (권장)
  - 판례 목록 안에서 name 충돌 방지, 사용자 혼동 감소.
- B. 완전 자유 (unique 없음)
  - 사용자가 v2 두 번 등록 가능. 관리 난이도.

### 정렬 정책
- **A. 최근 생성 우선 (`created_at DESC`)** (권장)
  - 판례 축적의 시간 순 감각과 맞음. 최신 추천이 위로.
- B. name 순
  - "v1, v2, v3"처럼 명시 버전 붙일 때만 유리.
- C. 사용자 수동 정렬 (display_order)
  - 초기부터 오버스펙. YAGNI.

### 갱신 정책
- **A. 판례 정신 준수 — in-place update 허용하되 새 생성 추천** (권장)
  - name·content 수정 가능. 큰 관점 변화면 새 selection 생성 유도(UI 텍스트).
- B. 완전 immutable (수정 불가, 새 생성만)
  - 판례 정신엔 맞지만 오타 수정도 새 생성이라 사용자 마찰.
- C. 완전 자유 in-place
  - 판례 이력 상실.

### 삭제 정책
- **A. Hard delete** (권장)
  - 판례는 사용자가 명시적으로 지우면 지움. 복원 요구 낮음. axis_topic 같은 매핑성 데이터 정책과 동일.
- B. Soft delete
  - 축적 자산이라면 유리하지만 도메인 룰(ADR021)상 카드·덱·자료 같은 핵심 자산만 soft delete. selections는 axis 부속 도큐먼트라 hard delete로 충분.

## 선택
- name unique: **A. 축 스코프 unique**
- 정렬: **A. 최근 생성 우선**
- 갱신: **A. in-place update 허용 + 새 생성 유도**
- 삭제: **A. Hard delete**

## 부속 결정

### 스키마 반영 ([#6](./issue-06-roadmap-selections-dualaxis.md)의 스키마 확정)
```sql
CREATE TABLE axis_selection (
  id BIGINT PK,
  axis_id BIGINT NOT NULL FK,
  name VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_axis_selection_name UNIQUE (axis_id, name)
);
CREATE INDEX idx_axis_selection_axis_created ON axis_selection(axis_id, created_at DESC);
```

### 도메인 검증
- `AxisSelection.name`: trim, blank 금지, 최대 200자.
- axis 스코프 name 중복 시 도메인 예외 (`AXIS_SELECTION_NAME_ALREADY_EXISTS`, 409 Conflict). DB UNIQUE로도 이중 방어.

### AI 추천 시 name 후보
- SelectionsSuggestionPort는 name 후보를 함께 반환 (예: "능 아키텍처 selections v2"). 사용자가 편집 가능.
- 서버는 name 중복 검사 후 저장. 충돌 시 사용자에게 편집 요구.

### API 응답 정렬 명시
- `GET /api/v1/axes/{axisId}/selections` 응답은 `created_at DESC` 정렬.
- 페이지네이션은 초기 미도입 (판례 개수가 많지 않을 것 예상). 필요 시 이후 이슈.

## 이관 산출물

- **BE-Story #11-1**: `AxisSelection` 도메인의 name 검증 + name 유일성 도메인 예외.
- **BE-Story #11-2**: Repository·Controller 정렬(`created_at DESC`) 명시.
- **BE-Story #11-3**: ErrorCode 신설 (`AXIS_SELECTION_NAME_ALREADY_EXISTS` 409, `AXIS_SELECTION_NAME_BLANK` 400 등).
- **FE-Story #11-4**: selections 리스트 UI에 최근 생성 순 정렬 + 편집·삭제 액션.

## 관련 이슈 / 문서

- 이전: [#6 Roadmap/Selection 이원 축](./issue-06-roadmap-selections-dualaxis.md) — 본 이슈의 스키마 확정 여기서 반영.
- 관련: [#9 AI 3층 확장](./issue-09-ai-suggestion-3layer.md) — SelectionsSuggestionPort가 name 후보 반환.
