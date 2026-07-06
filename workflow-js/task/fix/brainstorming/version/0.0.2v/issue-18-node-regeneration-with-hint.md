# Issue: 챕터 노드 통짜 재생성 API + hint 파라미터 (v1)

## 배경
사용자 지시 (2026-07-02 fix 회의):
> "그런데 예를 들어 layer 중 기능의 구현 layer에 하네스 엔지니어링 파트에 roadmap을 구성해나갈 때 (...) 부분 부분 맘에 좀 안드는 부분을 수정을 요구할 수 있게"
> "이 부분에 대한 회의가 필요할 거 같아"

사용자 3가지 부족 중 **중간 수정**과 **의견 반영**을 챕터 단위 재생성 API로 커버. 챕터 노드가 first-class로 승격됐으니(#15) 재생성 대상도 챕터 노드.

**v1 범위 결정**:
- 챕터 통짜 재생성만 지원 (`POST /nodes/{nodeId}/regenerate { hint }`).
- 섹션 단위 재생성은 v1 미포함 — body TEXT 직접 편집(PATCH)으로 커버.
- hint 히스토리 v1 미저장.

## 조사 결과 — 재생성 니즈 갈래

| 니즈 갈래 | 예시 사용자 시나리오 | v1 대응 |
|---|---|---|
| 챕터 통짜 재생성 (hint 없음) | "이 챕터 뭔가 별로. 다시 뽑아줘" | ✅ API 지원 |
| 챕터 통짜 재생성 + hint | "이 챕터, 면접 대비 관점 더 강화해서 다시" | ✅ API 지원 |
| 섹션(1-2) 단위 재생성 | "1-2 하네스의 존재 이유만 다시" | ❌ v1 미지원 → body TEXT PATCH |
| 리프 한 줄만 재생성 | "이 개념 문장만 손봐" | ❌ v1 미지원 → body TEXT PATCH |
| 새 챕터 추가 | "6번 챕터 하나 더 뽑아줘" | ✅ ChapterSubtreePort 신규 호출 (이슈 #17) |

## 옵션 비교

**Option A — 챕터 노드 재생성 API + hint 파라미터 v1 (채택)**
- `POST /nodes/{nodeId}/regenerate { hint? }` — 챕터 노드 body를 새 subtree ASCII로 통짜 교체.
- 서버는 `ChapterSubtreePort.suggest(..., freeformHint = hint)`로 호출 → 반환 body를 노드에 덮어씀.
- hint는 자연어 자유 문자열. v1엔 저장하지 않음 (재생성 트리거로만).

**Option B — 재생성 API + 섹션 단위 재생성 병행 v1**
- 서버가 body TEXT 파싱해서 섹션 단위 추출·재생성 후 splice.
- 파서 견고성 부담 큼. v1 초기 3명 사용자 규모에서 니즈 검증 안 됨 → v2로 연기.

**Option C — 재생성 API 없이 사용자가 매번 삭제 후 새 챕터 추가**
- API 단순하지만 순서·rationale 유지 안 됨. 사용자 UX 마찰.

## 선택: Option A (v1은 챕터 통짜만)

## 부속 결정

### API

```
POST /api/v1/roadmap-nodes/{nodeId}/regenerate
Content-Type: application/json
{
  "hint": "면접 대비 관점 강화, 실무 예시 추가"   // 선택
}
```

응답:
```json
{
  "nodeId": 42,
  "title": "1. 하네스 엔지니어링 기초",
  "rationale": "AI 에이전트의 기본 프레임 이해",
  "body": "├── 1-1. 정의와 본질\n│       모델 + 하네스 — ...\n..."
}
```

Selection 노드도 동일 패턴:
```
POST /api/v1/selection-nodes/{nodeId}/regenerate
```

### 서버 동작

1. `nodeId`로 챕터 노드 조회 (roadmap or selection).
2. 부모 axis(또는 axis + selection 컨테이너) 컨텍스트 로드 — concepts, layer, axis 이름·rationale, sibling 챕터들의 title 리스트.
3. 해당 챕터의 기존 title·rationale + hint(있으면)를 프롬프트에 추가.
4. `ChapterSubtreePort` / `SelectionSubtreePort` 호출 (이슈 #17).
5. 반환된 bodyAsciiTree로 노드 body 덮어씀. title·rationale·display_order는 유지 (변경 필요 시 별도 PATCH).
6. `updated_at` 갱신.

### 실패 처리

- LLM 실패 시 Static Adapter 폴백 (이슈 #17). Static fallback도 실패면 노드 유지 + 400 응답 (`AI_REGENERATION_FAILED`).
- 재생성 중 노드 삭제된 경우 404 (`ROADMAP_NODE_NOT_FOUND` / `SELECTION_NODE_NOT_FOUND`).
- 동시 재생성 요청 방지: 노드별 재생성 in-progress 상태를 세션 storage 또는 노드 lock 컬럼으로 관리할지는 v2 검토 (v1은 사용자 3명 규모라 충돌 낮음, 단순 낙관적 락으로 시작).

### 섹션 단위 편집 (v1 대체 경로)

섹션(1-2 등) 재생성 니즈는 v1엔 다음으로 커버:
- 사용자가 노드 편집기에서 body TEXT를 직접 편집.
- `PATCH /api/v1/roadmap-nodes/{nodeId} { body: "..." }` 로 저장.
- v2에서 서버 파싱 기반 섹션 단위 재생성 API 검토 (사용자 관찰 데이터가 필요성 입증할 때만).

### hint 히스토리 (v1 미저장 근거)

- 재생성 트리거로만 사용. 결과물은 노드 body 자체 → 이력이 필요한 경우가 드묾.
- v1 초기 3명 사용자 규모에서 hint 히스토리 니즈 사전 관찰 안 됨.
- 이슈 #20에서 초기 관찰 지표로 hint 텍스트 로깅은 포함 → v2에 히스토리 스키마 도입 시 근거 데이터 확보 가능.

## 이관 산출물

- **BE-Story #18-1**: `AxisRoadmapNode.regenerate(hint)` / `AxisSelectionNode.regenerate(hint)` 도메인 행위. `ChapterSubtreePort` / `SelectionSubtreePort` 호출 orchestration은 Application Service에서.
- **BE-Story #18-2**: Application Service — `RoadmapNodeApplicationService.regenerate(nodeId, hint)`, `SelectionNodeApplicationService.regenerate(nodeId, hint)` 구현. 소유권 검증(요청 유저가 노드 소유 axis의 facade 소유자인지) 포함.
- **BE-Story #18-3**: 엔드포인트 신설 — `POST /api/v1/roadmap-nodes/{nodeId}/regenerate`, `POST /api/v1/selection-nodes/{nodeId}/regenerate`.
- **BE-Story #18-4**: ErrorCode 신설 — `AI_REGENERATION_FAILED` 400 (LLM + Static 둘 다 실패).
- **BE-Story #18-5**: hint 텍스트 로깅 (이슈 #20의 관찰 지표에 포함).
- **FE-Story #18-6**: 노드 카드에 "재생성" 버튼 + hint 입력창(선택). 프로그레스 표시.
- **SDD 개정** (이슈 #8): `product-ai-interactive-roadmap.md`에 재생성 flow 추가.

## 관련 이슈 / 문서

- 연동: [#15 Roadmap 노드 모델](./issue-15-roadmap-node-model.md), [#16 Selection 노드 모델](./issue-16-selection-node-model.md), [#17 AI 2단계 생성](./issue-17-ai-two-step-generation.md), [#20 AI 비용 예산](./issue-20-ai-cost-budget-cap.md).
- 뒤집는 이슈: [#6 Roadmap/Selection dual-axis](./issue-06-roadmap-selections-dualaxis.md) — 이 이슈의 재생성은 노드 단위, 이슈 #6은 통짜 재생성이었음.
- SDD 개정: `product-ai-interactive-roadmap.md`, `product-ai-suggestion.md`.
