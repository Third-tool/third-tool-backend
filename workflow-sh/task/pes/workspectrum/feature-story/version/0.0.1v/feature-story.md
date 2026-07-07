# [Feature-story] 양식 정의

> 단일 feature = 단일 PR 단위. 상위 Epic이 굳이 필요 없는 독립 작업.
> references/001.md의 Story 골격을 그대로 차용한다.

---

## 사용 시점 (트리거)

다음 조건을 **모두** 만족할 때 본 양식을 쓴다.

- [ ] 추정 작업 시간 **1~3일**
- [ ] 단일 PR로 완결 (Story 분할 불필요)
- [ ] 영향 받는 BC **1개** (또는 BC 하나의 새 endpoint 하나)
- [ ] 도메인 검증 규칙 / 새 메서드 / 새 ErrorCode 등 **단위 테스트 매트릭스**가 필요
- [ ] 설계 갈림길이 **있더라도 1건 이하** (1건이면 PR 본문에 결정 메모로 충분)

**졸업 신호 → pes로**:
- 묶을 Story가 **2~3개 이상** 식별됨 (예: 도메인 모델 추가 + Repository + Controller + 통합 테스트)
- Product 수준 KPI나 Outcome을 정의할 가치가 생김 (단일 feature가 큰 그림 안의 한 조각이 됨)
- 의존 Story 순서·우선순위가 PR 1개로 표현 어려움

**졸업 신호 → sdd-lite로**:
- 외부 어댑터·핸드오프 문서가 필요 (예: BE↔FE 정합성, BC 어댑터 검증, 마일스톤 검증 Runbook)
- 변경 사실 + 작업 체크 + 검증 절차의 **3축 통합**이 필요

---

## 양식 골격

```
# [Story] {이름}

## 사용자 가치
- As a {역할}
- I want {원하는 행위}
- so that {얻는 가치}

## 설명
{도메인 메서드 시그니처·정적 팩토리·불변식·예외 패턴(ErrorCode)·포트명을 한 단락으로}

## 인수 조건
- Given {전제} / When {행위} / Then {결과}
- Given ... / When ... / Then ...
- *(엣지 케이스 — 사유)* Given ... / When ... / Then ...

## Definition of Done
- [ ] 구현 (클래스명·메서드명 구체적으로)
- [ ] 단위 테스트 — {N건, 해피/엣지/예외 매트릭스}
- [ ] 슬라이스 테스트 (해당 시)
- [ ] 통합 테스트 (해당 시)
- [ ] ADR 작성 (해당 시 — 새 결정·새 패턴이면 필수)

## 비범위
- {범위 밖 1~3줄, 사유 동반}

## INVEST 점검
- Independent: ...
- Negotiable: ...
- Valuable: ...
- Estimable: ...
- Small: ...
- Testable: ...
```

### 섹션 가이드

| 섹션 | 채울 것 |
| --- | --- |
| **사용자 가치** | As/I want/so that 3줄 고정. "어떤 사용자(클라이언트·관리자·시스템)"가 명시되어야 가치 검증 가능 |
| **설명** | 구현 단서 — 메서드 시그니처·불변식·ErrorCode 키. "추정 식별자" 금지. 실제 코드에서 가져온 이름만 |
| **인수 조건** | Given/When/Then 블록. 엣지는 `*(엣지 - 사유)*`로 태깅. 해피 1~2건 + 엣지 1~2건 + 예외 1~2건이 표준 |
| **DoD** | 체크리스트. **구현 + 테스트 + (해당 시)ADR**은 항상 포함. 빠뜨리면 PR 생성 보류 |
| **비범위** | 사유 동반. "X도 같이 하자"는 제안 차단의 근거가 됨 |
| **INVEST** | 6개 항목 한 줄씩. Estimable이 안 떨어지면 Story 분할 |

---

## 예시

```
# [Story] Card recordView 멱등 처리

## 사용자 가치
- As a 학습자
- I want ARCHIVE 상태 카드를 ReviewSession에서 무심코 열어도 시스템이 조용히 무시하길
- so that 의도치 않은 viewCount 증가나 예외 페이지로 학습 흐름이 끊기지 않는다

## 설명
- Card.recordView(Clock clock): ARCHIVE 상태이면 no-op (예외 X)
- ON_FIELD 상태일 때만 viewCount += 1, lastViewedAt = now(clock)
- enteredFieldAt은 변경하지 않음 (Card.create() / returnToField() 시점 전용 — conventions.md §1.11)
- ErrorCode 신규 추가 없음

## 인수 조건
- Given ON_FIELD 상태 Card / When recordView 호출 / Then viewCount += 1 + lastViewedAt = now
- Given ARCHIVE 상태 Card / When recordView 호출 / Then 상태·필드 모두 불변
- *(엣지 - 멱등 연속 호출)* Given ON_FIELD 상태 Card / When recordView 5번 연속 / Then viewCount = 기존 + 5

## Definition of Done
- [ ] Card.recordView(Clock) 구현
- [ ] CardTest.recordView_*: 단위 테스트 3건 (해피 / ARCHIVE 무시 / 5회 연속 누적)
- [ ] ADR 미작성 (멱등성은 conventions.md §1.5에 이미 명시)

## 비범위
- CardReview·UserSchedule의 view 누계 동기화는 별 Story (Card 자체 행위만 본 Story에서 다룸)
- Soft-deleted Card에 대한 recordView 차단은 @SQLRestriction이 이미 막음 — 본 Story 검증 X

## INVEST
- Independent: 다른 Story 의존 없음
- Negotiable: ARCHIVE 처리 방식만 협상 여지 (예외 던질지 vs 무시) — 본 Story는 무시 채택
- Valuable: 학습 흐름 연속성 확보
- Estimable: 0.5일
- Small: 단일 메서드 + 단위 테스트 3건
- Testable: AssertJ로 상태·viewCount·lastViewedAt 검증
```

---

## 참조

- 빈 스켈레톤: `./template.md`
- 이전 스펙트럼: `../../../one-line-spec/version/0.0.1v/one-line-spec.md`
- 다음 스펙트럼: `../../../pes/version/0.0.1v/pes.md`
- 정본 Story 가이드: `../../../../references/001.md`
- 양식 진화: 본 양식은 SemVer로 진화. 변경 시 `../0.0.2v/`에 새 버전을 두고 본 버전은 보존
