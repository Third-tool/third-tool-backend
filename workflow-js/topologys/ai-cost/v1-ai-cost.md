# Pinned Topology — `ai-cost` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님 (실제 모델·토큰 가격·예산 상한 값은 여기 없다)

**목적**: LLM API 호출 비용 관리의 반복 원칙을 pin. v1은 **관찰 지표만** · v2 이후 자동 cap. `llm-cascade`·`observability` 협력.

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

1. **LLM 호출 관련 작업 시 이 파일을 먼저 읽는다.** §2의 관찰·상한·개발 스크립트 원칙은 본 구간 **고정 제약**.
2. **LLM 호출당 tokens·cost 로깅 없이 배선하려는 정황이 보이면 STOP하고 보고한다.** 관측 실종.
3. **dev 스크립트에서 LLM 호출을 반복 자동 실행하려는 정황이 보이면 보고**한다. 예산 폭주 위험.
4. **`max-output-tokens` 상한 없이 LLM 호출 정황이 보이면 보고**한다. 예산 예측 불가.
5. **v1에 자동 예산 cap을 도입하려는 정황이 보이면 보고**한다. v2 이관 유지.
6. **이 파일에 실제 가격·예산 값을 적지 않는다.** 그건 provider 문서·`cost.md`에.

---

## 2. The pinned topology

### Nodes
- `LLM 호출` — 외부 LLM API 요청 · 프롬프트 tokens + 응답 tokens 소비
- `호출당 tokens 로깅` — `prompt_tokens` · `completion_tokens` · `cost.estimate` 필수 로그
- `max-output-tokens 상한` — 응답 tokens의 상한 config (예산 예측 가능성)
- `dev 예방 스위치` — dev 프로필에서 LLM 비활성 옵션 (`llm-cascade` 계승)
- `dev 스크립트 반복 금지` — 개발 반복 자동화가 LLM 호출을 반복 트리거 X
- `provider daily budget` — LLM provider 콘솔에서 설정하는 일일 예산 알림
- `v1 관찰 지표` — 호출 수 · tokens 합계 · 예상 비용 (관찰만)
- `v2 자동 cap` — 사용자·전역 예산 초과 시 자동 컷 (v2 이관 · v1 미도입)
- `수동 개입` — v1은 관찰 후 사용자가 수동 대응 (스코프 축소·rate 강화 등)
- `프로덕션 vs dev 예산 격리` — 각 환경별 예산 스코프 분리
- `Cascade 협력` — Cascade 폴백은 LLM 실패 시 · 예산 초과는 별도 이슈

### Edges
- `LLM 호출` → `호출당 tokens 로깅` : 모든 호출에 tokens·cost 로그
- `LLM 호출` → `max-output-tokens 상한` : config로 응답 tokens 제한
- dev 프로필 → `dev 예방 스위치` → LLM 비활성 또는 dry-run
- 개발자 반복 실행 → `dev 스크립트 반복 금지` : 자동화 스크립트가 LLM API 미호출
- 로컬·프로덕션 → `provider daily budget` (콘솔 알림) : 예산 임계값 초과 시 알림
- 관찰 지표 (`v1 관찰 지표`) → `수동 개입` : 사용자 결정 (스코프 축소·rate 튜닝 등)
- 비용 관찰 → `프로덕션 vs dev 예산 격리` : 각 환경 별도 관찰
- `v2 자동 cap` : v1은 미도입 · 이관 마커

### Boundaries
- **관측 경계**: LLM 호출당 `prompt_tokens`·`completion_tokens`·`cost.estimate` **로그·메트릭 필수** (observability topology 계승).
- **상한 경계**: LLM 호출에 `max-output-tokens` 상한 **필수**. 무제한 응답 금지.
- **dev 스위치 경계**: dev 프로필에 LLM 비활성 스위치 필수 (`llm-cascade` §Boundaries 계승).
- **dev 반복 금지 경계**: 개발 반복·CI·로컬 스크립트에서 LLM API를 자동 반복 호출 금지. 명시적 수동 트리거만.
- **환경 격리 경계**: 프로덕션 크레덴셜과 dev 크레덴셜 예산 스코프 분리 (`secrets-management` 계승).
- **v1 관찰만 경계**: v1은 **자동 예산 cap 도입 안 함**. 관찰 지표 수집·사용자 수동 개입만.
- **Cascade 협력 경계**: Cascade 폴백은 LLM 실패 시 (llm-cascade). 예산 초과는 별도 이슈 (Cascade와 무관).
- **provider daily budget 경계**: LLM provider 콘솔에서 daily budget 알림 필수 설정. 첫 실 배선 (M6) 시점에 확립.

### Invariants
- LLM 호출 성공 시 `prompt_tokens`·`completion_tokens`·`cost.estimate` 로그 없는 사례 0건
  - 감지법: LLM Adapter 응답 처리 코드 리뷰 · 로그 sample 확인
- LLM 호출에 `max-output-tokens` 상한 없는 사례 0건
  - 감지법: `application.yml` LLM config grep · SDK 호출 코드 리뷰
- dev 프로필에 LLM 비활성 스위치 없는 사례 0건 (llm-cascade와 중복 강제)
  - 감지법: `application-dev.yml` config 리뷰
- 개발 반복·CI에서 LLM API 자동 호출 사례 0건
  - 감지법: CI workflow · 로컬 스크립트 grep · 반복 호출 패턴 확인
- v1에 자동 예산 cap 배선 사례 0건 (v2 이관 유지)
  - 감지법: cap 관련 코드 grep · 배선 이력
- 프로덕션 daily budget 알림 미설정 상태에서 프로덕션 배포 사례 0건 (M6 이후)
  - 감지법: LLM provider 콘솔 설정 · 배포 pre-check
- dev 크레덴셜이 프로덕션 예산 스코프에 접근한 사례 0건 (secrets-management 계승)
  - 감지법: 크레덴셜 스코프 리뷰

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 LLM 모델명·토큰 가격 → provider 공식 문서 (Vertex AI·OpenAI 등)
- 실제 `max-output-tokens` 값 → `application.yml` externalize
- 실제 daily budget 알림 임계값 → LLM provider 콘솔
- 실제 dev 스위치 config → `application-dev.yml`
- 실제 비용 실측 데이터 → `workflow/task/milestones/version/{Nv}/cost.md`
- v2 자동 cap 정책 상세 → `fix/brainstorming/version/0.0.2v/issue-20-*.md` (이관 대기)
- 비용 관측 지표 정의 → `observability` topology · `ai-eval` 협력
- 왜 관찰만 · cap은 v2인가 → `docs/adr/ADR010.md` · issue-20

---

## 4. Re-pin trigger

- v1에 자동 예산 cap 도입 결정 (v2 이관 정책 폐기)
- LLM provider 교체 · 가격 모델 변경 (예: pay-per-request → subscription)
- `max-output-tokens` 상한 폐기 (무제한 응답 허용)
- dev 자동 반복 호출 허용 (실측 데이터 확보 목적 등)
- 예산 관리 위임 (다른 시스템·서비스에 이관)
- 사용자별 예산 분리 도입 (프리미엄 사용자 등)
- 비용 관측 지표 재정의 (tokens 외 다른 단위 도입)
- Cascade 폴백을 예산 초과 시에도 발동 (llm-cascade re-pin과 연동)
