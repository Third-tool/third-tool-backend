# [Feature-story] FE 양식 정의

> 단일 feature = 단일 PR 단위. 상위 Epic이 굳이 필요 없는 독립 작업.
> 백엔드 양식의 FE 버전. 도메인 메서드·정적 팩토리 자리에 컴포넌트·hook·Zod 스키마·endpoint 모듈이 들어간다.

---

## 사용 시점 (트리거)

다음 조건을 **모두** 만족할 때 본 양식을 쓴다.

- [ ] 추정 작업 시간 **1~3일**
- [ ] 단일 PR로 완결 (Story 분할 불필요)
- [ ] 영향 받는 Feature 도메인(`src/features/{x}/`) **1개** — 또는 단일 lib 모듈 + 그 사용처 1개
- [ ] 검증 규칙 / 새 hook / 새 ErrorCode UX 매핑 등 **단위 테스트 매트릭스**가 필요
- [ ] 설계 갈림길이 **있더라도 1건 이하** (1건이면 PR 본문에 결정 메모로 충분)

**졸업 신호 → pes로**:
- 묶을 Story가 **2~3개 이상** 식별됨 (예: Zod 스키마 + endpoint 모듈 + 페이지 컴포넌트 + 라우트 추가)
- Product 수준 KPI나 Outcome을 정의할 가치가 생김 (단일 feature가 큰 그림 안의 한 조각이 됨)
- 의존 Story 순서·우선순위가 PR 1개로 표현 어려움

**졸업 신호 → sdd-lite로**:
- 외부 어댑터·핸드오프 문서가 필요 (예: BE↔FE 계약 정합성, MSW handler 정리, 마일스톤 검증 Runbook)
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
{컴포넌트·hook·endpoint 함수 시그니처 / Zod 스키마 필드 / 에러 처리(ApiError code) / 라우트 경로를 한 단락으로}

## 인수 조건
- Given {전제} / When {행위} / Then {결과}
- Given ... / When ... / Then ...
- *(엣지 케이스 — 사유)* Given ... / When ... / Then ...

## Definition of Done
- [ ] 구현 (컴포넌트·hook·파일명 구체적으로)
- [ ] 단위 테스트 — {N건, 해피/엣지/예외 매트릭스, Vitest + Testing Library}
- [ ] MSW handler (외부 API 호출이 들어가면)
- [ ] 시각 회귀 / 수동 브라우저 확인 (해당 시)
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
| **사용자 가치** | As/I want/so that 3줄 고정. "어떤 사용자(학습자·운영자·신규 가입자)"가 명시되어야 가치 검증 가능 |
| **설명** | 구현 단서 — 컴포넌트명·hook 시그니처·Zod 스키마 필드·ApiError code 처리. "추정 식별자" 금지. 실제 코드에서 가져온 이름만 |
| **인수 조건** | Given/When/Then 블록. 엣지는 `*(엣지 - 사유)*`로 태깅. 해피 1~2건 + 엣지 1~2건 + 예외 1~2건이 표준 |
| **DoD** | 체크리스트. **구현 + 테스트 + (해당 시)ADR**은 항상 포함. 빠뜨리면 PR 생성 보류 |
| **비범위** | 사유 동반. "X도 같이 하자"는 제안 차단의 근거가 됨 |
| **INVEST** | 6개 항목 한 줄씩. Estimable이 안 떨어지면 Story 분할 |

---

## 예시

```
# [Story] CardEditor 키워드 cue 최소 1개 검증

## 사용자 가치
- As a 학습자
- I want 카드 생성 시 키워드 cue가 비어있으면 폼이 즉시 인라인 에러로 막아주길
- so that 백엔드까지 요청을 보냈다가 CARD_KEYWORD_MIN_REQUIRED 토스트로 되돌아오는 흐름이 사라진다

## 설명
- `features/card-editor/CardEditorPage.tsx`의 폼 제출 핸들러에서 `keywords.length >= 1` 클라이언트 사전 검증
- Zod 스키마(`lib/api/schemas/card.ts`) `CardCreateInputSchema`의 `keywords` 필드에 `.min(1)` 동반
- 위반 시 폼 하단에 인라인 에러 메시지 노출 (재사용: `components/...`의 ErrorText)
- API 에러(`ApiError.code === 'CARD_KEYWORD_MIN_REQUIRED'`)는 여전히 토스트로 fallback (서버 측 안전망)

## 인수 조건
- Given keywords가 0개인 폼 / When 제출 / Then 폼 제출 차단 + 인라인 에러 노출
- Given keywords 1개 입력 / When 제출 / Then 정상 POST → 201 + 토스트 "저장됨"
- *(엣지 — 공백만 입력)* Given keywords에 "  " 입력 / When 제출 / Then trim 후 빈 값으로 간주 + 인라인 에러

## Definition of Done
- [ ] CardEditorPage 폼 제출 핸들러 수정
- [ ] CardCreateInputSchema.keywords.min(1).regex(non-blank)
- [ ] Vitest 3건 (해피 / 0개 차단 / 공백만 차단)
- [ ] MSW handler는 기존 카드 생성 mock 재사용
- [ ] ADR 미작성 (기존 패턴)

## 비범위
- 키워드 cue 최대 개수 검증은 별 Story
- 같은 검증 패턴을 다른 폼(deck create 등)에 일괄 적용은 별 Story

## INVEST
- Independent: 다른 Story 의존 없음
- Negotiable: 인라인 vs 토스트만 협상 여지 — 인라인 채택
- Valuable: 잘못된 입력 차단 시 사용자 흐름 끊김 0건
- Estimable: 0.5일
- Small: 단일 페이지 + Zod 스키마 + 테스트 3건
- Testable: Testing Library `userEvent` + 폼 제출 시뮬레이션
```

---

## 참조

- 빈 스켈레톤: `./template.md`
- 이전 스펙트럼: `../../../one-line-spec/version/0.0.1v/one-line-spec.md`
- 다음 스펙트럼: `../../../pes/version/0.0.1v/pes.md`
- 백엔드 원본: `../../../../../workspectrum/feature-story/version/0.0.1v/feature-story.md`
- 양식 진화: 본 양식은 SemVer로 진화. 변경 시 `../0.0.2v/`에 새 버전을 두고 본 버전은 보존
