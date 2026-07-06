# Issue: AI 비용 예산 상한 정책 — backlog (v1 미구현, 관찰 지표 로깅만 v1 포함)

> **⚠️ v1 개발 스코프 아님. 이슈로만 backlog 기록.**
> 초기 3명 사용자 규모에서는 예산 상한을 구현하지 않는다. UX·데이터 품질 검증 후 사용자 규모 확대 시점에 재검토한다.
> 초기 3명 사용자 관찰 데이터를 통해 v2 시점의 상한 값·정책 세부를 근거 있게 산정하는 게 목적이다.

## 배경
사용자 지시 (2026-07-02 fix 회의):
> "이슈에 월간 ai 예산을 잡을건데, 초기에 바로 적용하는 것은 아니고 이슈로만 우선 적어둘게요, 사용자가 많지는 않고 우선 3명에서 올려놓고서 계속 테스트할 예정이라 상한의 크기보다 사용자 3명이서 계속 활용해보면서 만족할만한 데이터로 ux로 되는지가 더 중요해서"

이슈 #17에서 6-Port 확장 시 초기 세션당 호출 폭발 지점 존재:
- 초기 학습 탑 = Layer 1 + Axis 3 + ChaptersOutline 3 + ChapterSubtree 15 ≈ 22회
- Selection 축적 = outline 1 + subtree 5 = 6회/개
- 재생성 남용 = 사용자 성에 안 차서 반복 재생성

**초기 3명 규모 계산**  
Gemini 2.5 Pro 기준 대략:
- 초기 세션 1회 ≈ $0.25
- Selection 1개 ≈ $0.07
- 챕터 재생성 1회 ≈ $0.014

3명 × (초기 세션 2회 + Selection 20개 + 재생성 30회) ≈ $9/월 규모. 부담 없음.
→ 상한 크기보다 **UX·데이터 품질 검증**이 우선. 예산 통제는 사용자 규모 확대 시점에 재검토.

## 조사 결과 — 비용 폭발 지점

| 지점 | 시나리오 | 완화 (미래) |
|---|---|---|
| Selection 축적 폭주 | 사용자가 판례 20개+ 축적 | 월간 Selection 개수 상한 |
| 재생성 남용 | 성에 안 차서 반복 재생성 | 세션당 재생성 하드 캡 |
| rate limit 접촉 | 세션 진입 시 (outline 1 + subtree N) 폭발 | rate limit 상향 or 세션 예산 |
| LLM 장애 | 예산 상한 미도달인데 LLM 실패 | Static Adapter fallback (v1 유지) |

## v1 미구현 vs 관찰 지표 v1 포함 구분

| 항목 | v1 스코프 | 근거 |
|---|---|---|
| 사용자별 월간 예산 상한 | ❌ 미구현 | 초기 3명 규모 부담 없음. 상한 값 산정 근거 없음 |
| 세션당 재생성 하드 캡 (예: 20회) | ❌ 미구현 | 재생성 패턴 관찰 데이터 없음 |
| rate limit 10rpm → 세션 예산 전환 | ❌ 미구현 | rate limit 접촉 여부 관찰 지표에서 확인 후 결정 |
| **Static Adapter fallback (LLM 실패 시)** | ✅ **v1 유지** | 3명 테스트 세션이 LLM 장애로 끊기지 않게 하는 안정성 요구 (이슈 #17에서 확정) |
| **관찰 지표 로깅** | ✅ **v1 포함** | v2 예산 값 산정 근거 데이터 확보 |

## 옵션 비교

**Option A — 예산 상한 v1 미구현 + 관찰 지표만 로깅 (채택)**
- 예산 상한·재생성 하드 캡·rate limit 재검토 모두 v2로 연기.
- v1엔 관찰 지표를 세션·사용자 단위로 로깅.
- 3명 사용자 관찰 데이터로 v2 시점에 근거 있는 상한 값 결정.

**Option B — v1부터 예산 상한 도입**
- 사용자 수 3명 규모에 오버스펙. 상한 값 산정 근거 없음. UX 검증 우선순위와 배치.

**Option C — 관찰 지표도 v1 미포함**
- v2 시점에 상한 값 산정 근거 데이터 없음. 뒷단 결정 지연.

## 선택: Option A

## 부속 결정

### v1 관찰 지표 로깅

세션·사용자 단위로 다음을 로깅 (구조화 로그 or 별도 테이블):

| 지표 | 스코프 | 목적 |
|---|---|---|
| 세션당 AI 호출 수 (Port별 분해) | 세션 | 초기 세션 22회 예측 검증 |
| 세션당 총 토큰 (input/output 분해, cached/uncached 분해) | 세션 | 실제 비용 산정 |
| 세션당 재생성 횟수 (챕터별) | 세션 | 재생성 남용 패턴 관찰 |
| 챕터 재생성 hint 텍스트 원문 | 노드 | 사용자 의견 반영 니즈 관찰 |
| Static fallback 발동 비율 | 사용자 | LLM 실패율·예산 소진율 감지 |
| Selection 축적 개수 (사용자별 월간) | 사용자 | 축적 폭주 감지 |
| rate limit 접촉 여부 | 사용자 | rate limit 재검토 근거 |

### 로깅 구조 (초안)

- 실시간 대시보드 필수 아님 — v1엔 log aggregation(예: 애플리케이션 로그)만.
- 필요 시 배치로 집계해 CSV/Grafana 확인.
- 별도 테이블 신설은 v2 (`ai_usage_log(session_id, user_id, port, tokens_in, tokens_out, cached, hint_text, fallback, created_at)`)에서.

### v2 재검토 트리거

다음 중 하나가 관찰되면 v2 우선순위 상승:
- 3명 세션당 평균 비용이 $1 초과
- 특정 사용자가 월간 Selection 30개 초과 축적
- 재생성 남용 (세션당 30회 초과)
- rate limit 접촉 빈발 (일 5회 이상)

### v2 예상 구현 (아이디어 수준)

- 사용자별 월간 예산 상한 (`user_ai_budget(user_id, month, cap_usd, used_usd)`).
- 세션당 재생성 하드 캡 (세션 상태 안에 카운터).
- rate limit → 세션 예산 전환 (`session_ai_budget(session_id, cap_tokens, used_tokens)`).
- 예산 소진 시 Static Adapter 자동 승격.

## 이관 산출물

**v1 개발 스코프 (관찰 지표 로깅만)**:
- **BE-Story #20-1**: AI Port Adapter 공통 로깅 인터셉터 — 호출 수·토큰·cached 여부·fallback 여부 기록.
- **BE-Story #20-2**: 재생성 API(#18)에서 hint 텍스트 원문 로깅.
- **BE-Story #20-3**: Selection 컨테이너 생성 이벤트 로깅 (사용자별 월간 개수 집계 근거).

**v2 backlog (구현 안 함)**:
- 사용자별 월간 예산 상한 스키마·API.
- 세션당 재생성 하드 캡.
- rate limit → 세션 예산 전환.

## 관련 이슈 / 문서

- 연동: [#17 AI 2단계 생성](./issue-17-ai-two-step-generation.md) — Static Adapter fallback v1 유지 결정, rate limit 재검토 근거.
- 연동: [#18 노드 재생성 API](./issue-18-node-regeneration-with-hint.md) — hint 텍스트 로깅.
- SDD 개정: `product-ai-suggestion.md` (관찰 지표 로깅 명세).
