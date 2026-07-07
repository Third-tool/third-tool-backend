# Pinned Topology — `rate-limiting` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님 (실제 bucket 값·라이브러리 API·구체 임계값은 여기 없다)

**목적**: 요청 rate 제어의 반복 원칙을 pin. 외부 AI API 호출 반복·비용 폭주·오남용 방어의 1차 라인. `llm-cascade`와 협력 (429는 폴백 대상 아님) 관계.

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

1. **rate 제어 관련 작업 시 이 파일을 먼저 읽는다.** §2의 스코프·응답·임계값 externalize는 본 구간 **고정 제약**.
2. **rate 초과를 5xx로 반환하려는 정황이 보이면 STOP하고 보고한다.** 429가 표준.
3. **rate 초과 시 자동 Static 폴백 정황이 보이면 보고**한다 (llm-cascade topology 위반).
4. **임계값을 코드에 하드코딩하려는 정황이 보이면 보고**한다. externalize (config) 원칙 위반.
5. **스코프 (IP·User·session) 판정 없이 단일 bucket으로 통합하려는 정황이 보이면 보고**한다.
6. **이 파일에 어휘를 추가하지 않는다.** 실제 라이브러리·API는 §3 포인터로.

---

## 2. The pinned topology

### Nodes
- `Rate Bucket` — 정해진 시간창에 허용되는 요청 수의 저장 단위
- `IP 스코프 Bucket` — 익명·미인증 요청 제어
- `User 스코프 Bucket` — 인증된 사용자별 제어
- `concurrent 제한` — 동일 사용자·IP의 동시 진행 중 요청 수 상한
- `임계값 externalize` — `application.yml`에 노출되는 config 값
- `429 응답` — Too Many Requests · rate 초과 표준 응답
- `Retry-After 힌트` — 사용자·클라이언트에 재시도 안내 (선택 헤더)
- `ErrorCode` — `SUGGESTION_RATE_EXCEEDED` 계열 · api-spec/common-core topology 계승
- `Rate 진입 지점` — Application Service 진입 전 필터/interceptor
- `Cascade 협력` — 429는 `llm-cascade`의 폴백 대상 아님 (llm-cascade §Boundaries 참조)
- `계측 메트릭` — `rate_limit_exceeded_total{bucket,scope}` (observability 계승)

### Edges
- 요청 진입 → `Rate 진입 지점` → 스코프 판정 (`IP 스코프 Bucket` · `User 스코프 Bucket`)
- 스코프 판정 → `Rate Bucket` 잔량 감소 → 통과 or 초과
- `Rate Bucket` 초과 → `429 응답` + `ErrorCode` (`SUGGESTION_RATE_EXCEEDED` 등) → 사용자
- `429 응답` → `Retry-After 힌트` 헤더 (선택) : 사용자 재시도 지침
- `Rate Bucket` 초과 → `Cascade 협력` : 폴백 실행 X (llm-cascade 원칙 유지)
- `임계값 externalize` → `Rate Bucket` : config 값이 bucket 크기·refill 결정
- `Rate Bucket` 초과 → `계측 메트릭` (`rate_limit_exceeded_total`)

### Boundaries
- **스코프 경계**: `IP 스코프 Bucket`·`User 스코프 Bucket`·`concurrent 제한` 3종 스코프 분리. 단일 스코프 통합 금지.
- **응답 경계**: rate 초과는 **429 Too Many Requests** 표준. 5xx 반환 금지.
- **폴백 경계**: rate 초과는 llm-cascade 폴백 트리거 아님 (llm-cascade §Boundaries 계승). 사용자 재시도 지침으로 응답.
- **externalize 경계**: 임계값 (bucket 크기·refill·period) 은 **`application.yml`** 에 노출. 코드 하드코딩 금지.
- **ErrorCode 경계**: rate 초과 코드는 `ErrorCode 레지스트리` 등록 필수 (common-core topology 계승).
- **계측 경계**: rate 초과 발동은 메트릭·로그 필수 (observability topology 계승). 무기록 초과 처리 금지.
- **진입 위치 경계**: rate 판정은 Application Service 진입 **이전** (필터/interceptor). Application Service 안에서 재판정 금지.

### Invariants
- rate 초과 시 5xx 응답 사례 0건
  - 감지법: rate limit 코드 리뷰 · Controller Slice 테스트 응답 코드 확인
- rate 초과가 Static Adapter 폴백을 트리거한 사례 0건 (llm-cascade와 중복 강제)
  - 감지법: `SuggestionCascade` 코드 · rate 배선 리뷰
- 임계값 (bucket 크기 등) 이 코드에 하드코딩된 사례 0건
  - 감지법: bucket 초기화 코드 grep · `application.yml` config 참조 확인
- rate 초과 발동 시 메트릭·로그 없는 사례 0건
  - 감지법: rate 초과 처리 코드 리뷰
- 단일 스코프 (IP만·User만) 로 rate 제어를 통합한 사례 0건
  - 감지법: bucket 정의 코드 리뷰 · 스코프 개수 확인
- Application Service 안에서 rate 재판정한 사례 0건
  - 감지법: Application Service 코드 grep · rate 판정 위치 리뷰

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 rate limit 라이브러리 (Bucket4j 등) 사용 코드 → `Common/RateLimit/` 또는 관련 config 코드
- 실제 임계값 값 (rpm·concurrent) → `application.yml` externalize
- 실제 필터·interceptor 배선 → Spring Security config · Servlet filter chain
- 실제 ErrorCode 식별자 → `Common/Exception/ErrorCode/`
- Retry-After 헤더 세팅 상세 → response 세팅 코드
- rate 초과 로그·메트릭 세팅 → `observability` topology + 실제 Micrometer 등록 코드
- 왜 이렇게 박혔는지 → `docs/adr/` (rate limit 관련 ADR · 미신설)

---

## 4. Re-pin trigger

- rate 제어 라이브러리 교체 (Bucket4j → Resilience4j 등) 로 기반 변경
- 단일 스코프 통합으로 정책 변경 (IP·User 통합)
- rate 초과 응답 5xx 로 정책 변경 (표준 이탈)
- rate 초과 시 폴백 허용 (llm-cascade re-pin 함께)
- Application Service 안에서 rate 판정 허용 (진입 위치 이동)
- config externalize 폐기 (코드 하드코딩 허용)
- 분산 rate (Redis 기반 등) 도입 (in-memory bucket에서 전환)
- 사용자별 커스텀 rate 정책 도입 (관리자가 특정 사용자 한도 조정)
