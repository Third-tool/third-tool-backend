# [PES] 양식 정의

> Product / Epic / Story 3계층 명세. 1~3주 단위로 한 주제를 깊이 있게 풀어내는 표준 양식.
> 정본 가이드는 `../../../../references/001.md`. 본 파일은 그 정본을 양식 문서로 재구성한 것이며 새 형식을 발명하지 않는다.

---

## 사용 시점 (트리거)

다음 조건을 **모두** 만족할 때 본 양식을 쓴다.

- [ ] 추정 작업 시간 **1~3주** (Story 5~15개)
- [ ] **단일 BC** 또는 **인접 BC 2개**가 한 주제로 묶임
- [ ] Story 5+개로 자연스럽게 갈라지지만, 큰 그림(아키텍처/플로우/실패모드/롤아웃)까지 그릴 필요는 없음
- [ ] 정량 KPI를 정의할 가치가 있는 Outcome이 있다 ("0건", "≥ 95%", "응답 P95 ≤ 100ms")
- [ ] 설계 갈림길 0~2건 (Epic 인수 시나리오로 표현 가능)

**졸업 신호 → sdd-lite로**:
- 외부 동기화 / 핸드오프 / 검증 Runbook이 본 명세 안에 들어와야 함
- 다른 저장소(예: FE)나 다른 팀에게 인계할 인벤토리·계약·체크리스트가 필요

**졸업 신호 → sdd(풀)로**:
- 설계 갈림길 **3건 이상** 발생, Option A/B/C 비교를 정식 섹션으로 다뤄야 함
- 컴포넌트 배치 다이어그램·핵심 플로우 시퀀스·Out-of-Process 의존을 명시해야 함
- 실패 모드 매트릭스·로깅 정책·관측 지표를 사전 설계해야 함
- 마이그레이션 단계(prerequisite Product / Epic 그래프 / 환경별 설정 분기)가 필요

---

## 양식 골격

### Product 레벨 (` # [Product] {이름} `)

```
# [Product] {이름}

## 성과 (Outcome)
**{한 문장 — 핵심 책임 볼드}**

## 성공 지표
- {정량 지표 1: "≥/=/0건/100%" 형태}
- {정량 지표 2}
- {정량 지표 3}

## 범위 (Scope)
- {포함 1}
- {포함 2}

## 비범위 (Out of Scope)
- {제외 1} — 사유 한 줄
- {제외 2} — 사유 한 줄

## Epic 목록
- [ ] Epic 1: {제목}
- [ ] Epic 2: {제목}

## 제품 완료 기준
- [ ] 모든 Epic 완료
- [ ] ADR {NNN} 작성
- [ ] API 스펙 (Swagger) 갱신
- [ ] 운영 진실 소스 (DOMAIN.md / PACKAGE.md) 반영
```

### Epic 레벨 (` # [Epic N] {이름} `)

```
# [Epic N] {이름}

## 목표
{한 문장 — Epic이 끝났을 때 무엇이 가능해지는가}

## 포함 Story
- [ ] Story N-1: {제목}
- [ ] Story N-2: {제목}

## Epic 인수 시나리오
{시나리오를 → 화살표 흐름으로 서술}
예: 사용자 입력 X → Controller 검증 → Service Z 호출 → Domain Aggregate Y 상태 전이 → 응답 W

## Epic 완료 기준
- [ ] 포함 Story 모두 완료
- [ ] 통합 테스트 통과
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
{도메인 메서드 시그니처·정적 팩토리·불변식·예외 패턴(ErrorCode)·포트명을 구체적으로}

### 인수 조건
- Given ... / When ... / Then ...
- Given ... / When ... / Then ...
- *(엣지 케이스 — 사유)* Given ... / When ... / Then ...

### Definition of Done
- [ ] 구현
- [ ] 단위 테스트 ({N건})
- [ ] 슬라이스 테스트 (해당 시)
- [ ] 통합 테스트 (해당 시)
- [ ] ADR (해당 시)

### 비범위
- {범위 밖 + 사유}

### INVEST
- Independent / Negotiable / Valuable / Estimable / Small / Testable (각 한 줄)
```

### 섹션 가이드 (전 레벨 공통)

| 원칙 | 적용 |
| --- | --- |
| 한글 서술 + 영문 도메인 용어 | Aggregate, VO, UseCase, inbound/outbound port 등 |
| 설계 선택지가 갈리면 A/B/C 나열 + 1차 권장 + 선택 사유 | Story `설명` 또는 Epic `인수 시나리오`에 단락으로 |
| 멱등성 / 트랜잭션 경계 / 외부 호출 분리 / 보상 | 해당 Story에서 반드시 검토 |
| 모든 레벨에 Out of Scope를 두어 범위를 닫는다 | Product/Epic/Story 각각 비범위 섹션 |
| 추정 식별자 금지 | 실제 클래스명·포트명·ErrorCode 접두사는 코드에서 확인해서 쓴다 |

---

## 예시

```
# [Product] Card 복습 흐름

## 성과 (Outcome)
**학습자가 등록한 카드를 일정 알고리즘 기반으로 순차 노출하고, viewCount·lastViewedAt이 일관되게 누적된다.**

## 성공 지표
- ReviewSession 응답 P95 ≤ 100ms
- 카드 노출 후 viewCount 미반영 0건
- ARCHIVE 카드 노출 0건

## 범위
- ReviewSession 생성·조회·다음 카드 노출
- viewCount/lastViewedAt 누적

## 비범위
- 알고리즘 정교화 (SM-2, FSRS 등) — Product "복습 알고리즘 v2"에서 별도 처리
- 카드 자동 만료 — `CardExpiryPolicy`에서 이미 처리

## Epic 목록
- [ ] Epic 1: ReviewSession 라이프사이클
- [ ] Epic 2: 노출 시 카드 상태 동기화

## 제품 완료 기준
- [ ] 모든 Epic 완료
- [ ] ADR 015 (ReviewSession 트랜잭션 경계) 작성
- [ ] Swagger /api/v1/review/** 갱신

---

# [Epic 1] ReviewSession 라이프사이클

## 목표
사용자가 한 덱에 대해 ReviewSession을 시작·재개·종료할 수 있다.

## 포함 Story
- [ ] Story 1-1: ReviewSession 생성
- [ ] Story 1-2: ReviewSession 재개
- [ ] Story 1-3: ReviewSession 종료

## Epic 인수 시나리오
사용자 → POST /api/v1/review {deckId} → ReviewSessionService.start → ReviewSession Aggregate 생성 → 200 OK {sessionId}
→ POST /api/v1/review/{sessionId}/next → 다음 Card 노출 + recordView
→ POST /api/v1/review/{sessionId}/finish → 종료 + 통계 집계

## Epic 완료 기준
- [ ] Story 1-1·1-2·1-3 모두 완료
- [ ] 통합 테스트: 세션 1개 풀 사이클 1건 그린
- [ ] ADR 015 작성

---

## [Story 1-1] ReviewSession 생성

### 사용자 가치
- As a 학습자
- I want 특정 덱에 대해 새 복습 세션을 시작하길
- so that 끊긴 학습 흐름과 분리된 새 세션 단위로 viewCount·시간을 집계할 수 있다

### 설명
- ReviewSession.create(userId, deckId, Clock): 정적 팩토리. startedAt = now(clock)
- 동일 덱의 미종료 세션이 있으면 ACTIVE_SESSION_ALREADY_EXISTS (REVIEW003)
- ReviewSessionRepository(JpaPort) save 후 sessionId 반환

### 인수 조건
- Given userA의 ACTIVE 세션 없음 / When POST /api/v1/review {deckId=10} / Then 201 + {sessionId} 반환
- Given 동일 deckId의 ACTIVE 세션 존재 / When POST 동일 / Then 409 + REVIEW003
- *(엣지 - 동시 생성)* Given 동시에 2개 요청 / When 둘 다 POST / Then 한 건만 201, 나머지는 409

### Definition of Done
- [ ] ReviewSession Aggregate + ReviewSessionCommandService.start 구현
- [ ] 단위 테스트 4건 (해피 / 중복 / 동시 / Clock 검증)
- [ ] @WebMvcTest Slice 1건
- [ ] @DataJpaTest UNIQUE 제약 1건

### 비범위
- 카드 노출 순서 알고리즘 (Story 2-1에서)
- 종료 시 통계 계산 (Story 1-3에서)

### INVEST
- Independent: Card BC 변경 0건
- Negotiable: ACTIVE 중복 처리 방식만 (예외 vs 기존 세션 재사용) — 본 Story는 예외
- Valuable: 세션 단위 학습 흐름 추적 시작점
- Estimable: 1일
- Small: 단일 Aggregate + 단일 endpoint
- Testable: 4 단위 + 1 Slice + 1 Repository
```

---

## 참조

- 빈 스켈레톤: `./template.md`
- 정본 가이드: `../../../../references/001.md`
- 이전 스펙트럼: `../../../feature-story/version/0.0.1v/feature-story.md`
- 다음 스펙트럼: `../../../sdd-lite/version/0.0.1v/sdd-lite.md`, `../../../../sdd/version/0.0.1v/sdd.md`
- 양식 진화: 본 양식은 SemVer로 진화. 변경 시 `../0.0.2v/`에 새 버전을 두고 본 버전은 보존
