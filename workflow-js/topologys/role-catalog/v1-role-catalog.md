# Pinned Topology — `role-catalog` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님 (실제 role 이름·감지 키워드·catalog 값은 여기 없다)

**목적**: AI 제안의 role 감지·catalog JSON 관리의 반복 원칙을 pin. `llm-cascade`·`prompt-engineering`·`ai-eval` 협력. role 확장 (새 catalog 추가) 시 반복 지켜져야 할 원칙.

---

## 0. 유효 구간 (Validity)

| 항목 | 값 |
| --- | --- |
| Pinned at | 2026-07-21 |
| Valid for | 본 워크플로우 v1 구간 — re-pin trigger 발생 전ује 전까지 |
| Owner | 메인 Claude + 사용자 합의 |
| Re-pin trigger | 아래 §4 |

---

## 1. How Claude Code must use this file — 행동 계약

1. **새 role catalog 추가·수정 시 이 파일을 먼저 읽는다.** §2의 감지·6-Port 필드·fallback·Sceptical 판정은 본 구간 **고정 제약**.
2. **catalog JSON에 6-Port 필드 중 일부만 채우려는 정황이 보이면 STOP하고 보고한다.** 응답 shape 파괴.
3. **RoleDetector 로직에 하드코딩된 우선순위를 임의 변경하려는 정황이 보이면 보고**한다.
4. **알 수 없는 concept에 대한 fallback (기본 role) 을 우회하려는 정황이 보이면 보고**한다.
5. **Sceptical Reviewer 판정 없이 catalog 콘텐츠를 커밋하려는 정황이 보이면 보고**한다.
6. **이 파일에 실제 role 이름·감지 키워드를 적지 않는다.** 그건 catalog JSON에.

---

## 2. The pinned topology

### Nodes
- `role catalog JSON` — classpath resource (`src/main/resources/ai/catalog/{role}.json`)
- `RoleDetector` — concepts[] → role 매핑 로직 · concepts 배열에서 키워드 감지
- `6-Port 필드 정합` — 각 catalog가 `layers`·`axes`·`chapters`·`selectionOutlines` 등 필드 전량 정합
- `기본 role fallback` — 알 수 없는 concept 또는 감지 실패 시 반환할 기본 role (예: `backend-developer`)
- `role 우선순위` — concepts에 여러 role 키워드 동시 존재 시 판정 규칙
- `concept-spec 태깅 정합` — catalog의 chapters·selectionOutlines가 concept-spec 6+5 카탈로그 태깅과 정합
- `Sceptical Reviewer 판정` — catalog 콘텐츠의 수렴 판단 프레임 여부 · 도구 이름·비교 침투 여부 판정
- `catalog Presence Test` — 부팅·테스트 시 catalog JSON 존재·6-Port 필드 정합 자동 검증
- `role 확장 지점` — 새 role 추가 시 (a) catalog JSON 신설 (b) RoleDetector 감지 로직 확장 (c) Sceptical 판정 (d) eval baseline 축적
- `providerContext 접두어` — 응답 필드 `static:{role}` · `llm:{model}` (llm-cascade 계승)
- `4-role vs N-role` — v1 4-role 확립 (backend-developer·planner·designer·problem-solver) · 확장은 신중

### Edges
- concepts[] 요청 → `RoleDetector` → 키워드 매칭 → `role catalog JSON` 조회
- 감지 실패 → `기본 role fallback` (backend-developer 등)
- 여러 role 키워드 동시 → `role 우선순위` → 하나 선택
- 매칭된 role → `role catalog JSON` → `6-Port 필드 정합` 응답 반환
- 응답 필드 → `providerContext 접두어` (`static:{role}`)
- catalog 콘텐츠 → `concept-spec 태깅 정합` : concept-spec 6+5와 태깅 정합
- 새 catalog 추가 → `Sceptical Reviewer 판정` → 통과 시 커밋 (`ai-eval` § baseline)
- 부팅 → `catalog Presence Test` → 자동 무결성 검증
- 새 role 요구 → `role 확장 지점` → 4 단계 프로세스

### Boundaries
- **catalog 위치 경계**: 모든 role catalog는 **classpath resource** (`src/main/resources/ai/catalog/`). 외부 저장소·String literal 금지 (prompt-engineering 계승 동일 원칙).
- **6-Port 필드 경계**: catalog JSON은 `layers`·`axes`·`chapters`·`selectionOutlines` **전량 필드 정합** 필수. 일부만 채운 catalog 금지.
- **fallback 경계**: 알 수 없는 concept·감지 실패는 **기본 role fallback** (backend-developer 등) 로만 대응. 예외·500 응답 금지.
- **감지 우선순위 경계**: `role 우선순위` 는 코드에 명시. concepts 배열 순서 · 특정 키워드 우선 등 명시적 규칙. 임의 변경 금지.
- **정합 판정 경계**: 새 catalog · catalog 수정 시 **Sceptical Reviewer 판정 필수** (수렴 판단 프레임 여부 · 도구·특정 옵션 비교 침투 없음).
- **concept-spec 정합 경계**: catalog chapters·selectionOutlines가 `concept-spec.txt` 6+5 카탈로그 태깅 정합.
- **Presence Test 경계**: 새 catalog 추가 시 Presence Test 자동 검증. 미갱신 상태 커밋 금지.
- **확장 신중 경계**: role 확장 (4 → N) 은 사용자 판단 · 사용자 관찰 데이터 근거 (오탐 통계 등).

### Invariants
- catalog JSON에 6-Port 필드 중 일부만 채운 사례 0건
  - 감지법: catalog JSON 파일 필드 검사 · Presence Test 통과 확인
- 알 수 없는 concept에 대해 예외·500 응답한 사례 0건 (fallback 우회)
  - 감지법: `RoleDetector` 코드 리뷰 · fallback 케이스 테스트
- 새 catalog 추가 시 Sceptical Reviewer 판정 없이 머지된 사례 0건
  - 감지법: Reviewer 세션 이력 vs catalog 추가 커밋 매칭
- catalog와 concept-spec 6+5 태깅 정합 위반 사례 0건
  - 감지법: catalog 파일 vs concept-spec 대조 (`ai-eval` § baseline 참조)
- Presence Test 실패 상태로 catalog 추가 커밋 사례 0건
  - 감지법: PR CI 이력 확인
- `providerContext` 필드가 `static:{role}` · `llm:{model}` 이외 형식 사용 사례 0건 (llm-cascade 계승)
  - 감지법: 응답 세팅 코드 리뷰

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 role 이름·catalog 내용 → `src/main/resources/ai/catalog/*.json`
- 실제 감지 키워드·우선순위 규칙 → `LearningFacade/infrastructure/suggestion/RoleDetector.java`
- 실제 fallback 기본 role → RoleDetector 코드
- 실제 Presence Test 구현 → `src/test/java/.../CatalogPresenceTest.java`
- 실제 concept-spec 6+5 태깅 → `src/main/resources/ai/prompts/concept-spec.txt`
- Sceptical Reviewer 판정 이력 → `workflow/task/milestones/version/{Nv}/eval.md` § baseline
- role 4종 실측 (M4 baseline) → `workflow/task/milestones/version/0.0.4v/eval.md`
- 왜 이렇게 박혔는지 → `docs/adr/` (role catalog 관련 ADR · 미신설)
- 프롬프트 관리 원칙 → `prompt-engineering` topology
- Cascade 폴백·providerContext 원칙 → `llm-cascade` topology
- AI eval SLO·metric → `ai-eval` topology

---

## 4. Re-pin trigger

- catalog 위치 이관 (classpath → 외부 저장소·DB)
- 6-Port 필드 정합 폐기 (일부 필드만 있는 catalog 허용)
- fallback 개념 폐기 (감지 실패 시 예외 · 500)
- Sceptical Reviewer 판정 자동화 (LLM-as-judge 도입)
- concept-spec 6+5 카탈로그 재정의 (prompt-engineering re-pin 연동)
- role 감지를 LLM에 위임 (RoleDetector 로직 폐기 · LLM classifier 도입)
- 사용자별 커스텀 role 도입 (관리자·개인 catalog 생성)
- 다국어 catalog 도입 (한국어 vs 영어 병행)
