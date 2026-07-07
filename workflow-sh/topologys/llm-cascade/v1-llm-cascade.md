# Pinned Topology — `llm-cascade` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님 (실제 Adapter 클래스명·프롬프트 내용·Cascade 판정 임계값은 여기 없다)

**목적**: AI 제안 계층의 Static ↔ LLM 폴백 원칙을 pin. `ai-eval`이 평가 SLO를 다룬다면, 본 파일은 **Cascade 폴백 판정 · 응답 shape 유지 · 실패 사유 로깅** 원칙을 다룬다. v1 릴리스의 사용자 신뢰성 핵심.

---

## 0. 유효 구간 (Validity)

| 항목 | 값 |
| --- | --- |
| Pinned at | 2026-07-21 |
| Valid for | 본 워크플로우 v1 구간 — re-pin trigger 발생 전까지 |
| Owner | 메인 Claude + 사용자 합의 |
| Re-pin trigger | 아래 §4 |

---

## 1. How Claude Code must use this file — 행동 계약

1. **AI Suggestion Cascade·Adapter 관련 작업 시 이 파일을 먼저 읽는다.** §2의 폴백 판정·응답 shape는 본 구간 **고정 제약**.
2. **LLM 응답 파싱 실패 시 예외를 그대로 5xx로 전파하려는 정황이 보이면 STOP하고 보고한다.** ADR010 위반.
3. **Adapter마다 Response DTO shape가 다르게 되어가는 정황이 보이면 보고**한다. shape 유지 원칙 위반.
4. **`cascadeFallbackReason` 로깅 없이 폴백을 실행하려는 정황이 보이면 보고**한다. 관측 실종.
5. **Rate limit (429) 를 Cascade 폴백 대상으로 삼으려는 정황이 보이면 보고**한다. rate limit은 사용자 응답이지 폴백 트리거 아님.
6. **이 파일에 어휘를 추가하지 않는다.** 실제 Adapter·프롬프트는 §3 포인터로.

---

## 2. The pinned topology

### Nodes
- `SuggestionPort` — 6-Port 인터페이스 (Layer·Axis·ChaptersOutline·ChapterSubtree·SelectionOutline·SelectionSubtree)
- `Static Adapter` — classpath JSON 카탈로그 기반 (in-memory · 무비용)
- `LLM Adapter` — 외부 LLM API 호출 담당 (예: 상용 · self-hosted 무관) · 실배선 시점은 별도 마일스톤
- `SuggestionCascade` — LLM 우선 시도 → 실패 시 Static 폴백 (Application 서비스)
- `providerContext` — 응답 필드 (`static:{role}` · `llm:{model}` 접두어 강제)
- `suggestionsAvailable` — 응답 flag · 폴백 판정 (ADR010 계승)
- `cascadeFallbackReason` — 폴백 실행 시 필수 로그 필드 (`llm_timeout` · `llm_error` · `llm_parse_error` · `llm_quota_exceeded` · `llm_disabled` 등)
- `응답 shape` — Adapter 무관 동일 Response DTO 구조 (`layers` · `chapters` 등 6-Port 필드 정합)
- `LLM 실패 신호` — timeout · quota · parse error · auth 실패 · 5xx (폴백 트리거)
- `Rate limit` — 429 응답 · Cascade 폴백 대상 **아님** (사용자에게 재시도 안내)
- `parse 실패 판정` — LLM 응답이 JSON 스키마 위반 시 fallback (ADR010)
- `비용 관측` — LLM 호출당 `cost.estimate=Xtokens` 로깅 (observability 계승)
- `dev disable 스위치` — dev 프로필에서 LLM 비활성 시 즉시 Static 폴백

### Edges
- 사용자 요청 → `SuggestionCascade` → `LLM Adapter` (1차 시도)
- `LLM Adapter` 성공 → `응답 shape` (llm:{model} providerContext) → 사용자
- `LLM Adapter` 실패 (`LLM 실패 신호` 감지) → `cascadeFallbackReason` 로깅 → `Static Adapter` (폴백) → `응답 shape` (static:{role} providerContext) → 사용자
- `parse 실패 판정` → `cascadeFallbackReason=llm_parse_error` → `Static Adapter`
- `dev disable 스위치` → `cascadeFallbackReason=llm_disabled` → `Static Adapter` : dev 프로필 비용 방지
- `Rate limit` → 429 응답 · 폴백 없음 : 사용자에게 재시도 안내
- `LLM 호출` → `비용 관측` → 로그 (`cost.estimate=Xtokens`)
- 6-Port 각각 → `SuggestionCascade` → 동일 shape 응답 : Adapter 무관 shape 유지

### Boundaries
- **폴백 트리거 경계**: 폴백은 `LLM 실패 신호` (timeout·quota·parse error·auth 실패·5xx) 에서만 발동. Rate limit (429·bucket 초과) 은 폴백 대상 X → 사용자 응답으로 전달.
- **응답 shape 경계**: Static ↔ LLM Adapter 응답이 동일 shape 유지. Response DTO는 `providerContext` 접두어만 다르고 필드 구조 동일.
- **providerContext 접두어 경계**: `static:{role}` 또는 `llm:{model}` 접두어 강제. 접두어 없는 값 · 다른 형식 금지.
- **로깅 경계**: 폴백 발동 시 `cascadeFallbackReason` 필수 로깅. 무기록 폴백 금지 (관측 실종).
- **에러 노출 경계**: LLM 호출 실패는 5xx 미노출 (ADR010) · `suggestionsAvailable: false` 또는 폴백 응답으로 사용자에 전달.
- **비용 관측 경계**: LLM 호출당 tokens·cost 로깅 필수. 무기록 호출 금지 (예산 관리 실종).
- **dev 스위치 경계**: dev 프로필은 LLM 비활성 옵션 제공 (`spring.ai.vertex.ai.gemini.enabled=false`). 개발 반복 시 비용 폭주 방지.
- **Adapter 스왑 경계**: LLM Adapter 교체 (Vertex → OpenAI 등) 시에도 응답 shape·providerContext 원칙 유지. Application 코드 변경 최소화.

### Invariants
- LLM 호출 실패 (timeout·quota·parse error) 가 5xx 로 사용자에게 노출된 사례 0건
  - 감지법: `SuggestionAppService` 예외 경로 리뷰 · ADR010 정합 확인
- 폴백 발동 시 `cascadeFallbackReason` 로그 필드가 없는 사례 0건
  - 감지법: 로그 sample grep · `SuggestionCascade` 코드 리뷰
- Static ↔ LLM Adapter 응답의 Response DTO 필드 구조가 다른 사례 0건
  - 감지법: DTO 정의 비교 · Controller Slice 테스트 shape 검증
- `providerContext` 필드에 `static:*` · `llm:*` 이외 값 사용 사례 0건
  - 감지법: providerContext 세팅 코드 grep · Response DTO 문서화 확인
- Rate limit (429) 응답 시 Static Adapter 폴백 실행 사례 0건
  - 감지법: `SuggestionCascade` 코드 · rate-limiting 배선 리뷰
- LLM 호출 성공 시 `cost.estimate` 로깅 없는 사례 0건
  - 감지법: LLM Adapter 응답 처리 코드 리뷰 · Micrometer 등록 확인
- dev 프로필에서 LLM 비활성 스위치 없는 사례 0건 (개발자 비용 폭주 방지)
  - 감지법: `application-dev.yml` 설정 리뷰

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 Adapter 클래스명 (`StaticChaptersOutlineAdapter` 등) → `LearningFacade/infrastructure/suggestion/`
- 실제 프롬프트 파일 (`concept-spec.txt` 등) → `src/main/resources/ai/prompts/`
- 실제 role catalog JSON 파일 → `src/main/resources/ai/catalog/*.json`
- 실제 `SuggestionCascade` 구현 → `LearningFacade/application/service/SuggestionCascade.java` (M6 신설 예정)
- 실제 Vertex AI 모델명·config → `application-{profile}.yml`·`spring.ai.vertex.ai.gemini.*`
- 실제 폴백 판정 임계값 (timeout ms 등) → `application.yml` externalize
- role catalog·프롬프트 관리 원칙 상세 → 향후 `prompt-engineering` topology (MEDIUM · 재검토 예정)
- LLM 비용 관리 상세 (rate·cap) → 향후 `ai-cost` topology (MEDIUM · 재검토 예정)
- Cascade 폴백 원칙의 근거 → `docs/adr/ADR010.md` (AI 실패 처리)
- AI eval SLO·metric 관리 → `workflow/topologys/ai-eval/v1-ai-eval.md`

---

## 4. Re-pin trigger

- LLM 실패를 5xx 로 노출하는 정책 전환 (ADR010 재검토)
- Cascade 방향 반전 (Static 우선 → LLM 폴백) 로 정책 변경
- Adapter shape 불일치 허용 (Adapter별 다른 Response DTO)
- providerContext 접두어 정책 폐기 (`static:*`·`llm:*` 자유 형식)
- Rate limit 응답 시 폴백 허용으로 정책 변경 (429 → Static 응답)
- 다중 LLM Adapter 동시 실행·비교 방식 도입 (병렬 Cascade)
- 스트리밍 응답 (SSE) 도입으로 shape 개념 확장
- 사용자별 Cascade 커스터마이징 도입 (해당 사용자만 Static·LLM 강제 선택)
