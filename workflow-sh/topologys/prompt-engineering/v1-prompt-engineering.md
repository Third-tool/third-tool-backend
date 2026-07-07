# Pinned Topology — `prompt-engineering` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님 (실제 프롬프트 문구·마커·catalog 값은 여기 없다)

**목적**: AI 프롬프트 자산 관리의 반복 원칙을 pin. `llm-cascade`·`ai-eval`과 협력 (프롬프트는 AI 응답 품질의 근간). LLM Adapter 도입 (M6) 이후 계속 확장.

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

1. **프롬프트 파일 추가·수정 시 이 파일을 먼저 읽는다.** §2의 위치·마커·무결성 검증은 본 구간 **고정 제약**.
2. **프롬프트를 코드 내부 String literal 로 하드코딩하려는 정황이 보이면 STOP하고 보고한다.** classpath resource 원칙 위반.
3. **`{{include:...}}` 마커 형식을 벗어난 include 정황이 보이면 보고**한다.
4. **`PromptTemplatesPresenceTest` 검증 없이 새 프롬프트 파일 추가 정황이 보이면 보고**한다.
5. **concept-spec 카탈로그 정합 판정 없이 프롬프트를 변경하려는 정황이 보이면 보고**한다.
6. **이 파일에 실제 프롬프트 내용을 적지 않는다.** 그건 리소스 파일에.

---

## 2. The pinned topology

### Nodes
- `프롬프트 파일` — classpath resource (`src/main/resources/ai/prompts/{topic}.txt`)
- `프롬프트 위치` — `src/main/resources/ai/prompts/` 하위 UTF-8 텍스트
- `include 마커` — `{{include:concept-spec.txt}}` 형식 · 다른 파일을 embed
- `concept-spec 카탈로그` — 로드맵 6 type · 셀렉션 5 type · 판정 프레임 (`concept-spec.txt`)
- `PromptTemplatesPresenceTest` — 부팅·테스트 시 모든 프롬프트 파일 존재·마커 정합 검증
- `프롬프트 버저닝` — 파일별 변경 이력 (git 이력이 1차 · Reviewer 판정 이력이 2차)
- `Sceptical Reviewer 정합 판정` — 프롬프트 변경 시 concept-spec 카탈로그·정책 정합 확인
- `role catalog 협력` — 각 role의 프롬프트 (planner·designer 등) 별 카탈로그 반영
- `실행 시 로딩` — Static Adapter가 부팅 시 프롬프트 로드 · in-memory 캐시
- `프로덕션 리로드 없음` — 프롬프트 변경은 재배포로만 반영

### Edges
- 새 프롬프트 요구 → `프롬프트 위치` (`src/main/resources/ai/prompts/`) → `프롬프트 파일` 추가
- `프롬프트 파일` → `include 마커` → `concept-spec 카탈로그` · 다른 프롬프트 embed
- `프롬프트 파일` 추가·수정 → `Sceptical Reviewer 정합 판정` → 통과 시 커밋
- `프롬프트 파일` 추가·수정 → `PromptTemplatesPresenceTest` 자동 검증
- `role catalog 협력` → 각 role의 챕터·selectionOutline 값 → `concept-spec 카탈로그` 6+5 태깅 정합
- 부팅 → Static Adapter → `실행 시 로딩` → in-memory 캐시
- 프롬프트 변경 → `프로덕션 리로드 없음` → 재배포로만 반영 (config 리로드 X)
- `프롬프트 버저닝` : git 이력 + Reviewer 판정 이력이 진실 소스

### Boundaries
- **위치 경계**: 모든 프롬프트는 **classpath resource** (`src/main/resources/ai/prompts/`). 외부 저장소·String literal 하드코딩 금지.
- **파일 형식 경계**: UTF-8 텍스트 · 확장자 `.txt` 통일. 다른 확장자 (`.md`·`.yaml`) 시 파싱 로직 재검토.
- **include 마커 경계**: `{{include:{filename}}}` 형식 강제. 다른 마커 (`<include>`·`@import` 등) 금지.
- **무결성 검증 경계**: `PromptTemplatesPresenceTest` 로 모든 파일 존재 + include 대상 존재 자동 검증. 신설 시 test 갱신 필수.
- **정합 판정 경계**: 프롬프트 변경은 **Sceptical Reviewer 정합 판정** 필수 (`ai-eval` §baseline과 연동).
- **catalog 정합 경계**: role catalog JSON 챕터·selectionOutline과 concept-spec 6+5 카탈로그 태깅 정합.
- **리로드 경계**: 프로덕션에서 프롬프트 hot reload 없음. 재배포로만 반영 (config 안정성).
- **버저닝 경계**: git 이력이 1차 진실 · Reviewer 판정 이력은 2차 (`eval.md` § baseline).

### Invariants
- 프롬프트가 코드 내부 String literal 로 하드코딩된 사례 0건
  - 감지법: Java 코드 grep · classpath resource 참조 확인
- `{{include:...}}` 형식 외 include 마커 사용 사례 0건
  - 감지법: 프롬프트 파일 grep · 마커 형식 리뷰
- `PromptTemplatesPresenceTest` 실패 상태로 신설 프롬프트 커밋 사례 0건
  - 감지법: PR CI 이력 · 테스트 통과 확인
- 프롬프트 변경 시 Sceptical Reviewer 정합 판정 없이 머지 사례 0건
  - 감지법: Reviewer 세션 이력 vs 프롬프트 변경 커밋 매칭
- role catalog JSON과 concept-spec 6+5 태깅 정합 위반 사례 0건
  - 감지법: catalog 파일 vs concept-spec 대조 (`ai-eval` §M4 baseline 참조)
- 프로덕션에서 프롬프트 hot reload를 시도한 사례 0건
  - 감지법: 프로덕션 config 리로드 코드·핫스왑 라이브러리 grep

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 프롬프트 내용 → `src/main/resources/ai/prompts/*.txt`
- 실제 `concept-spec.txt` 카탈로그 6+5 태깅 → `src/main/resources/ai/prompts/concept-spec.txt`
- 실제 role catalog JSON → `src/main/resources/ai/catalog/*.json`
- 실제 `PromptTemplatesPresenceTest` 구현 → `src/test/java/.../PromptTemplatesPresenceTest.java`
- 실제 Adapter의 프롬프트 로딩 코드 → `LearningFacade/infrastructure/suggestion/`
- 프롬프트 변경 이력 → git log · `workflow/task/milestones/version/{Nv}/eval.md`
- Reviewer 정합 판정 이력 → `eval.md` § baseline 절
- role catalog 관리 원칙 → 향후 `role-catalog` topology (LOW 우선순위 · 별도 pin)
- AI eval SLO·metric → `ai-eval` topology
- 왜 이렇게 박혔는지 → `docs/adr/` (프롬프트 관련 ADR · 미신설)

---

## 4. Re-pin trigger

- 프롬프트 위치 이관 (classpath → 외부 저장소·DB 등)
- include 마커 형식 변경 (`{{include:...}}` 폐기)
- 프롬프트 hot reload 도입 (재배포 없이 반영)
- concept-spec 6+5 카탈로그 재정의 (다른 프레임 도입)
- Sceptical Reviewer 정합 판정 자동화 (LLM-as-judge 도입)
- role별 프롬프트 분리 방식 변경 (role catalog + 공통 프롬프트 조합 → 다른 방식)
- 프로덕션에 프롬프트 A/B 실험 도입 (동시 다중 프롬프트 관리)
- 다국어 프롬프트 도입 (한국어·영어 프롬프트 병행 관리)
