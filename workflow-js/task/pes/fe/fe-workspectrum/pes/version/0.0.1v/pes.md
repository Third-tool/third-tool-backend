# [PES] FE 양식 정의

> Product / Epic / Story 3계층 명세. 1~3주 단위로 한 주제(예: 인증 UX 통일, Deck 페이지 묶음)를 깊이 있게 풀어낸다.
> 백엔드 동일 양식의 FE 버전. "BC"는 "Feature 도메인"으로, "Aggregate Root·VO·Application Service"는 "페이지 컴포넌트·Zod 스키마·custom hook"으로 치환.

---

## 사용 시점 (트리거)

다음 조건을 **모두** 만족할 때 본 양식을 쓴다.

- [ ] 추정 작업 시간 **1~3주** (Story 5~15개)
- [ ] **단일 Feature 도메인** (`src/features/{x}/`) 또는 **인접 Feature 도메인 2개**가 한 주제로 묶임
- [ ] Story 5+개로 자연스럽게 갈라지지만, 큰 그림(라우팅 그래프·전역 상태 흐름·실패 모드 매트릭스)까지 그릴 필요는 없음
- [ ] 정량 KPI를 정의할 가치가 있는 Outcome이 있다 ("LCP P95 ≤ 1.5s", "401 처리 후 자동 재시도 성공률 ≥ 95%", "Lighthouse 90+")
- [ ] 설계 갈림길 0~2건 (Epic 인수 시나리오로 표현 가능)

**졸업 신호 → sdd-lite로**:
- 외부 동기화 / 핸드오프 / 검증 Runbook이 본 명세 안에 들어와야 함
- 백엔드 또는 다른 팀에게 인계할 인벤토리·계약·체크리스트가 필요 (예: 새 endpoint 소비 계약, MSW handler 정리, 마일스톤 검증 절차)

**졸업 신호 → sdd(풀)로**:
- 설계 갈림길 **3건 이상** 발생, Option A/B/C 비교를 정식 섹션으로 다뤄야 함
- 컴포넌트 트리·라우팅·상태 흐름 다이어그램이 본질적으로 필요
- 실패 모드 매트릭스(401/403/5xx/MAINTENANCE/네트워크 단절)·로깅 정책·Web Vitals 임계 모니터링을 사전 설계해야 함
- 마이그레이션 단계(prerequisite Product / Epic 그래프 / 환경별 설정 분기 — dev/stage/prod)가 필요

---

## 양식 골격

### Product 레벨 (` # [Product] {이름} `)

```
# [Product] {이름}

## 성과 (Outcome)
**{한 문장 — 핵심 사용자 가치 볼드}**

## 성공 지표
- {정량 지표 1: "≥/=/0건/100%" 형태 — Web Vitals / 회귀 0 / E2E 통과율 등}
- {정량 지표 2}
- {정량 지표 3}

## 범위 (Scope)
- {포함 1 — 어느 라우트·컴포넌트·hook이 변하나}
- {포함 2}

## 비범위 (Out of Scope)
- {제외 1} — 사유 한 줄
- {제외 2} — 사유 한 줄

## Epic 목록
- [ ] Epic 1: {제목}
- [ ] Epic 2: {제목}

## 제품 완료 기준
- [ ] 모든 Epic 완료
- [ ] ADR {NNN} 작성 (해당 시)
- [ ] Zod 스키마 / API endpoint 모듈 / 라우트 일관성 점검
- [ ] 백엔드 ErrorCode 매핑 갱신 (`workflows/backend-boundary/error-codes.md`)
```

### Epic 레벨 (` # [Epic N] {이름} `)

```
# [Epic N] {이름}

## 목표
{한 문장 — Epic이 끝났을 때 어떤 화면·플로우가 가능해지는가}

## 포함 Story
- [ ] Story N-1: {제목}
- [ ] Story N-2: {제목}

## Epic 인수 시나리오
{시나리오를 → 화살표 흐름으로 서술}
예: 사용자가 X 입력 → 폼 Zod 검증 → mutation 호출 → 성공 시 페이지 이동 + 토스트, 실패 시 인라인 에러 + 토스트

## Epic 완료 기준
- [ ] 포함 Story 모두 완료
- [ ] 통합 E2E / Testing Library 통합 1건 그린
- [ ] ADR {NNN} 작성 (해당 시)
```

### Story 레벨 (` ## [Story N-M] {이름} `)

```
## [Story N-M] {이름}

### 사용자 가치
- As a {역할}
- I want {원하는 행위}
- so that {얻는 가치}

### 설명
{컴포넌트 시그니처·hook 시그니처·Zod 스키마 필드·endpoint 함수·라우트 경로를 구체적으로}

### 인수 조건
- Given ... / When ... / Then ...
- Given ... / When ... / Then ...
- *(엣지 케이스 — 사유)* Given ... / When ... / Then ...

### Definition of Done
- [ ] 구현
- [ ] 단위 테스트 ({N건}) — Vitest + Testing Library
- [ ] MSW handler (외부 API 호출 시)
- [ ] 통합 / E2E 테스트 (해당 시)
- [ ] ADR (해당 시)

### 비범위
- {범위 밖 + 사유}

### INVEST
- Independent / Negotiable / Valuable / Estimable / Small / Testable (각 한 줄)
```

### 섹션 가이드 (전 레벨 공통)

| 원칙 | 적용 |
| --- | --- |
| 한글 서술 + 영문 라이브러리/도메인 용어 | React Router / TanStack Query / Zod / Axios / MSW / Testing Library 등 |
| 설계 선택지가 갈리면 A/B/C 나열 + 1차 권장 + 선택 사유 | Story `설명` 또는 Epic `인수 시나리오`에 단락으로 |
| 사용자 인터랙션 / 비동기 경계 / 인증 토큰 흐름 / 캐싱 무효화 (TanStack Query invalidate) | 해당 Story에서 반드시 검토 |
| 모든 레벨에 Out of Scope를 두어 범위를 닫는다 | Product/Epic/Story 각각 비범위 섹션 |
| 추정 식별자 금지 | 실제 컴포넌트명·hook 시그니처·endpoint 경로·ErrorCode 접두사는 코드에서 확인해서 쓴다 |

---

## 예시

```
# [Product] 인증 토큰 UX 통일

## 성과 (Outcome)
**AT는 HttpOnly 쿠키 자동 첨부, RT는 React 메모리에서 1회 자동 갱신, 비복구 코드는 즉시 세션 클리어로 통일된다.**

## 성공 지표
- AUTH002 발생 후 자동 refresh 성공률 ≥ 99%
- AUTH001/003/004/101-104 발생 시 세션 클리어 누락 0건
- 401 처리 도중 UI 깜빡임 (전환 페이지 노출) 0건

## 범위
- `src/lib/api/client.ts` 인터셉터
- `src/features/auth/refreshTokenStore.ts` (메모리 토큰)
- `src/features/auth/components/ProtectedRoute.tsx` 가드
- `src/features/auth/SessionWatcher.tsx` 세션 손실 처리

## 비범위
- AT 메모리 보관 옵션 (백엔드 ADR-AUTH-001과 정합) — Out
- 다중 디바이스 RT — 백엔드 v2 의존

## Epic 목록
- [ ] Epic 1: ApiError 변환 + 코드 분류 통일
- [ ] Epic 2: refresh 인터셉터 1회 재시도 + race 방지
- [ ] Epic 3: 세션 손실 시 라우팅 회복 (login 진입)

## 제품 완료 기준
- [ ] Epic 모두 완료
- [ ] ADR FE-AUTH-001 작성
- [ ] workflows/backend-boundary/error-codes.md 갱신
- [ ] Vitest 인터셉터 시나리오 8건 그린
```

---

## 참조

- 빈 스켈레톤: `./template.md`
- 이전 스펙트럼: `../../../feature-story/version/0.0.1v/feature-story.md`
- 다음 스펙트럼: `../../../sdd-lite/version/0.0.1v/sdd-lite.md`, `../../../sdd/sdd.md`
- 백엔드 원본: `../../../../../workspectrum/pes/version/0.0.1v/pes.md`
- 양식 진화: 본 양식은 SemVer로 진화. 변경 시 `../0.0.2v/`에 새 버전을 두고 본 버전은 보존
