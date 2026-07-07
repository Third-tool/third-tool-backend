# Pinned Topology — `ai-eval` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님 (실제 golden set 항목·metric 수치 임계값은 여기 없음)

**목적**: AI 제안 계층(6-Port · Static/LLM Adapter)의 응답 품질을 정량 평가하기 위한 SLO·평가 프레임 pin.

---

## 0. 유효 구간 (Validity)

| 항목 | 값 |
| --- | --- |
| Pinned at | 2026-07-03 |
| Valid for | 본 워크플로우 v1 구간 — re-pin trigger 발생 전까지 |
| Owner | 메인 Claude + 사용자 합의 |
| Re-pin trigger | 아래 §4 |

---

## 1. How Claude Code must use this file — 행동 계약

1. **AI Port · Adapter 신설·수정 시 이 파일을 먼저 읽는다.** §2의 SLO·평가 프레임은 본 구간 **고정 제약**.
2. **모든 Port는 eval 대상.** 새 Port 추가 시 golden set·metric 등록 절차 동반.
3. **AI 호출 실패는 5xx 미노출 (ADR010).** eval 실패는 별도 채널로 관찰.
4. **평가 metric은 정량화 가능해야 함.** 주관 평가는 별도 정성 채널로.
5. **Static Adapter도 eval 대상.** LLM 없다고 skip 하지 않음 (회귀 방지).
6. **eval 결과는 milestones로 박제.** `milestones/version/{Nv}/ai-eval.md`.

---

## 2. The pinned topology

### Nodes
- `Port` — `application/port/out/suggestion/` 하위 6개 인터페이스
- `Adapter` — `Static*` (classpath JSON) · 미래 `Llm*` (Vertex/OpenAI) · 미래 `Cascade*` (fallback)
- `golden set` — 입력·기대 출력 쌍. Port별 관리
- `metric` — 정량 지표 (schema 준수율, 필드 커버리지, 응답 시간, fallback 발동 비율)
- `SLO` — 각 metric의 목표 임계값
- `평가 실행` — CI 또는 로컬 gradle task로 실행되는 eval batch
- `평가 결과 스냅샷` — 실행 시점 metric 값 (milestones/{Nv}/ai-eval.md에 박제)
- `회귀 판정` — 이전 스냅샷 대비 SLO 미충족 감지
- `fallback 경로` — AI 실패 시 `suggestionsAvailable=false` + 빈 목록 (ADR010)

### Edges
- `Port` → `golden set` : 1:N 매핑 (Port마다 다수의 케이스)
- `골든 set` × `Adapter` → `metric` : 실행 결과 metric 산출
- `metric` → `SLO` : 임계값 비교
- `SLO 위반` → `회귀 판정` → 알람·리뷰
- `평가 결과 스냅샷` → `milestones/version/{Nv}/ai-eval.md` : 박제
- `평가 결과 스냅샷` → `topology 재pin 검토` : SLO를 지속 초과·미달 시 pin 조정 필요
- `AI 호출 실패` → `fallback 경로` (ADR010) : 5xx 미노출

### Boundaries
- **Adapter 커버 경계**: 모든 Adapter (Static · Llm · Cascade)는 동일 golden set으로 평가. Adapter 스왑 시 회귀 방지.
- **metric 정의 경계**: metric 이름·계산법은 pin. 새 metric 추가는 §4 재pin 사유.
- **golden set 라이프사이클 경계**: golden set 추가는 자유. 삭제는 근거 문서화. 수정은 회귀 판정 baseline 재설정 필수.
- **자동화 경계**: 평가 실행은 CI 자동 (PR 트리거) 또는 로컬 수동. prod 트래픽으로 실행 X.
- **박제 경계**: 스냅샷은 milestone 단위로 `milestones/version/{Nv}/ai-eval.md`. living-docs에 스냅샷 저장 X.

### Invariants
- 모든 Port에 golden set 최소 3개 이상 (해피/엣지/예외)
  - 감지법: Port × golden set 매트릭스 리뷰
- 모든 Adapter는 동일 Port의 동일 golden set으로 평가 (Adapter 편향 방지)
  - 감지법: 평가 runner 코드 리뷰
- SLO 정의 없이 신규 metric 도입 0건
  - 감지법: metric 추가 PR 리뷰
- 평가 실행이 prod DB · prod LLM 실키를 사용하지 않음 (분리 환경)
  - 감지법: 평가 config vs prod config diff
- AI 호출 실패가 사용자에게 5xx로 노출되지 않음 (ADR010)
  - 감지법: SuggestionAppService 예외 경로 리뷰

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 golden set 항목·기대 출력 → `living-docs/ai-eval/` (골든 set 카탈로그) 또는 `src/test/resources/ai-eval/`
- 실제 metric 계산 코드 → `src/test/java/.../ai/eval/` (evaluation runner)
- 실제 SLO 수치값 → `living-docs/ai-eval/` (표로 관리) 또는 본 파일 부록 (v2 확장 시)
- 실제 평가 실행 결과 스냅샷 → `workflow/task/milestones/version/{Nv}/ai-eval.md` (박제)
- 왜 이렇게 박혔는지 → `docs/adr/ADR010.md` (AI 실패 처리)

---

## 4. Re-pin trigger

- 새 Port 추가 (기존 6 → N)
- 새 metric 정의 (schema 준수율 외 semantic 유사도, 사람 평가 도입 등)
- LLM Adapter 도입 (실제 외부 호출 · 비용·rate limit 새 boundary)
- Cascade Adapter 도입 (Static ⇄ Llm fallback 흐름 변경)
- SLO 목표값 조정 (v1 임계값 지속 미달 or 초과)
- eval 자동화 범위 확대 (PR CI 외 nightly · canary 트래픽)
- prompt/context 관리를 별도 topology로 분리 필요
