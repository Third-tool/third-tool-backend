# AI Eval (Living)

> **성격**: living-docs — 항상 최신본. AI 제안 계층의 평가 **방법론·골든 set 관리·실행 가이드** 지도.
> **성장 방향**: golden set이 늘면 Port별 파일로 분화 (`ai-eval/layer-port.md`, `ai-eval/roadmap-port.md` …).
> **관련 topology**: `workflow/topologys/ai-eval/v1-ai-eval.md` (SLO·프레임 pin).
> **관련 milestones**: `workflow/task/milestones/version/{Nv}/ai-eval.md` — 각 버전 실행 결과 박제 (진행 시점 등장).
> **원본**: `src/test/java/.../ai/eval/` (evaluation runner) + `src/test/resources/ai-eval/` (golden set, 아직 신설 안 됨).

---

## 규칙 요약 (topology 발췌)

- 모든 6 Port(Layer / Axis / Roadmap / Selections / ChaptersOutline / ChapterSubtree / SelectionOutline / SelectionSubtree) eval 대상.
- Port별 golden set 최소 3개 (해피/엣지/예외).
- metric은 정량화. schema 준수율·필드 커버리지·응답 시간·fallback 발동 비율.
- 실행 결과는 milestones로 박제.
- AI 실패 5xx 미노출 (ADR010).

---

## 1. Port × Adapter × role catalog × golden set 매트릭스 (**M4 role 4종 확장**)

| Port | Static Adapter | LLM Adapter (M6 예정) | role catalog | golden set 상태 |
| --- | --- | --- | --- | --- |
| `LayerSuggestionPort` | `StaticLayerSuggestionAdapter` | — | 4종 (M4) | ⚪ 미준비 |
| `AxisSuggestionPort` | `StaticAxisSuggestionAdapter` · `StaticAxisTopicSuggestionAdapter` | — | 4종 (M4) | ⚪ 미준비 |
| `RoadmapSuggestionPort` | (미확인) | — | 4종 | ⚪ 미준비 |
| `SelectionsSuggestionPort` | (미확인) | — | 4종 | ⚪ 미준비 |
| `ChaptersOutlineSuggestionPort` | `StaticChaptersOutlineAdapter` | — | 4종 (M4) | 🟡 M4 baseline (Sceptical Reviewer 판정 통과 · `../task/milestones/version/0.0.4v/eval.md`) |
| `ChapterSubtreeSuggestionPort` | `StaticChapterSubtreeAdapter` | — | 4종 | 🟡 M4 baseline |
| `SelectionOutlineSuggestionPort` | `StaticSelectionOutlineAdapter` | — | 4종 | 🟡 M4 baseline |
| `SelectionSubtreeSuggestionPort` | `StaticSelectionSubtreeAdapter` | — | 4종 | 🟡 M4 baseline |

**role catalog 4종** (M4 AS E3 · classpath JSON): `backend-developer.json` (M3 · 하네스 엔지니어링 확장) + `planner.json` + `designer.json` + `problem-solver.json` (M4 신설 3종).

**상태**: golden set 자동 runner는 미구현 (M6 LLM Adapter 도입 이전). M4 baseline은 Sceptical Reviewer 수동 판정으로 축적 (`../task/milestones/version/0.0.4v/eval.md` §M4 baseline 참조).

---

## 2. Metric 정의 (topology pin 아래 실전 지도)

| Metric | 계산법 | 초기 SLO 후보 |
| --- | --- | --- |
| `schema-conformance-rate` | 응답이 예상 JSON schema를 만족하는 케이스 비율 | ≥ 99% |
| `field-coverage-rate` | 필수 필드가 존재하고 non-null 비율 | ≥ 98% |
| `response-time-p95` | AI 호출 → 응답 반환 p95 (ms) | Static: < 100ms · LLM: < 3000ms |
| `fallback-trigger-rate` | ADR010 fallback (빈 목록 + suggestionsAvailable=false) 발동 비율 | < 5% (LLM 도입 후 관찰) |
| `role-match-rate` | RoleDetector가 concept → role 매핑 성공 비율 (4-role 대비) | ≥ 90% |
| `catalog-hit-rate` | Static Catalog에서 role 매칭 hit 비율 | ≥ 85% (backend-developer fallback 15% 이하) |
| `role-diversity` | **M4 신설** — 동일 요청에 role별 다른 응답 반환 여부 (Sceptical 판정) | 4-role 상호 겹침 없음 |

**초기 상태**: 목표선만 pin. **M4 실측**: role-match-rate은 Sceptical Reviewer 수동 판정 (`static:planner|designer|problem-solver|backend-developer` 4종 응답 모두 실측 확인 · v1 릴리스 후 오탐 통계 축적 예정).

---

## 3. Golden Set 관리

### 위치 (예정)
- 코드: `src/test/resources/ai-eval/{port-name}/*.json`
- Case 형식:
```json
{
  "id": "layer-happy-01",
  "port": "LayerSuggestionPort",
  "input": {
    "role": "backend-developer",
    "concepts": ["백엔드", "인프라"]
  },
  "expected": {
    "schema": "..."
    // schema-only or specific-values
  },
  "notes": "해피 케이스 · role 명확"
}
```

### 케이스 3구분 (해피/엣지/예외)
- **해피**: role 명확 · catalog hit
- **엣지**: role 애매 · concept 여러 개 · generic fallback
- **예외**: LLM 실패 · timeout · schema 위반

### 추가·삭제·수정 라이프사이클
- **추가**: 자유. PR로 리뷰.
- **삭제**: 근거 문서화 (예: 케이스가 재현 안 됨).
- **수정**: 회귀 baseline 재설정 필수. 이전 스냅샷과 비교 무의미해짐 → 문서화.

---

## 4. 실행 방법 (초기 프레임)

### 로컬 실행 (예정)
```bash
./gradlew test --tests "com.example.thirdtool.ai.eval.SuggestionEvalRunnerTest"
```

### CI 실행 (예정)
- PR 트리거로 eval batch 실행
- 결과를 GitHub Check로 노출
- SLO 미달 시 PR 상태 fail

### 실행 결과 박제
- 결과 스냅샷을 `workflow/task/milestones/version/{Nv}/ai-eval.md`에 박제
- 이전 스냅샷 대비 회귀 판정

---

## 5. 회귀 판정 · 알람

- **회귀 판정 기준**: 이전 milestone 스냅샷 대비 metric의 statistically significant 하락
- **알람 채널**: 향후 결정 (Slack · GitHub Issue · milestone 등록)
- **자동 트리거**: PR CI eval 실패 시 리뷰어 알림

---

## 6. Fallback 흐름 (ADR010)

AI 호출 실패 시:
```
SuggestionAppService
  ├─ try: Port 호출 (Static or LLM)
  ├─ catch:
  │   ├─ SuggestionTimeoutException → fallback
  │   ├─ SuggestionInvalidResponseException → fallback
  │   └─ SuggestionAuthFailedException → fallback
  │
  └─ fallback: 빈 목록 + suggestionsAvailable=false 응답
       │
       └─ 사용자 UX: "제안을 지금 받을 수 없어요" 배지
```

**5xx 미노출** — HTTP 응답은 200 OK + 빈 목록. eval 관점에서는 `fallback-trigger-rate` metric으로 관찰.

---

## 7. 향후 로드맵

- [ ] Static Adapter에 대해 golden set 초안 (Port당 3~5개)
- [ ] EvalRunner 클래스 구현 (`src/test/java/.../ai/eval/`)
- [ ] metric 계산 유틸 구현
- [ ] milestone `{Nv}/ai-eval.md` 스냅샷 첫 발행
- [ ] LLM Adapter 도입 시점에 실제 SLO 관찰치 수집
- [ ] CI 통합 (PR eval trigger)

---

## 8. 감시 포인트

topology `v1-ai-eval.md` §2 Invariants:
- Port × golden set × Adapter 매트릭스 정합
- prod DB · prod LLM 실키를 eval에서 사용하지 않음
- AI 실패가 5xx로 노출 0건

---

## 참조

- 관련 topology: `workflow/topologys/ai-eval/v1-ai-eval.md`
- 관련 milestones: `workflow/task/milestones/version/{Nv}/ai-eval.md` (진행 시점 등장)
- 관련 ADR: `docs/adr/ADR010.md` (AI 실패 처리)
- 아키텍처: `workflow/living-docs/architecture-system-design/architecture.md` §5 (AI 헥사고날)

*최신 갱신: 2026-07-21 · **M4 반영** — role catalog 4종 확장 (`planner`/`designer`/`problem-solver` 신설) · Sceptical Reviewer 판정 baseline 축적 · `role-diversity` metric 추가 · M6 LLM Adapter 이전 자동 runner 미구현 유지*
