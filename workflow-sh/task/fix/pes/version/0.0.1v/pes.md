# [Fix · PES] 양식 정의

> **fix 트랙**: Story 여러 개 또는 다른 BC에 영향이 번지는 **명세 표류 정정** 트래킹 문서.
> planning은 `../../../../pes/workspectrum/pes/version/0.0.1v/pes.md`(시간 기반 1~3주, Story 5+개 기획) 참조.
> fix는 **수정 범위·복잡도** 기준 5단계 중 3단계 — 원본 PES 명세의 일부 정정·재조정.

---

## 사용 시점 (트리거)

다음 조건 중 **2개 이상**을 만족할 때 본 양식을 쓴다.

- [ ] 원본 PES(Product / Epic / Story) 진행 중 **Story 1~3개 명세 표류** 발견
- [ ] ErrorCode 신규 등록 / DTO 필드 변경 / Repository 메서드 신설이 동반
- [ ] AC가 원본과 어긋남 — 인수 조건 재정의 필요
- [ ] 다른 BC 1개가 본 변경을 함께 반영해야 함 (양방향 변경 X — 다른 BC는 받는 측만)
- [ ] 원본 Product 아키텍처·롤아웃은 그대로 (큰 결정 번복 없음)

**졸업 신호 → fix/sdd-lite로**:
- 인터페이스 시그니처 변경 / 응답 래퍼 도입 / 인증 매체 변경 같은 **계약 변경**
- BC 간 어댑터 계약을 새로 정의해야 함
- 다른 저장소·다른 팀에 인계할 핸드오프 문서가 필요

**졸업 신호 → fix/sdd로**:
- Product 수준 아키텍처·롤아웃 결정 번복
- 원본 명세 폐기·재작성

---

## 양식 골격

```
# [Fix] {수정 요지} ({YYYY-MM-DD})

## 대상 (원본)
- 원본 Product: {파일 경로}
- 영향 받는 Epic / Story: {Epic N / Story N-M, N-K, ...}
- 본 fix 발견 시점: {YYYY-MM-DD}

## 트리거
{왜 이 변경이 필요해졌는가 — 1~3 문장. 명세 표류 발생 경로·외부 입력·인터랙션 결과}

## 변경 내역

### Story별 명세 변경 이력
#### Story N-M: {원본 제목}
- **변경 전 AC** (원본): Given ... / When ... / Then ...
- **변경 후 AC**: Given ... / When ... / Then ... + *(엣지 추가 — 사유)*
- **DoD 추가/변경**: {체크 항목}
- **사유**: {왜}

#### Story N-K: {원본 제목}
- ...

### ErrorCode / DTO 변경 (해당 시)
- 신규 ErrorCode: `{CODE}` ({HTTP}, "{message}") — {사유}
- DTO 필드 변경: {DTO명}.{field} {추가/제거/타입변경}

## 검증
- 단위: `./gradlew test --tests "..."` 그린
- 슬라이스: {결과}
- 통합: {결과}

## 영향
- 원본 명세 (`{경로}`) — [명세 변경 이력] 블록에 본 fix 식별자 기재
- DOMAIN.md / PACKAGE.md 갱신 — {섹션 명}
- 다른 BC: {경로} — 받는 측 변경 (별도 fix 또는 본 fix 안)
- ADR 트리거 — {ADR NNN 후보 또는 N/A}
```

### 섹션 가이드

| 섹션 | 채울 것 |
| --- | --- |
| **대상** | 원본 Product 파일 경로 + 영향 Story 번호 정확히. "여러 Story" 같은 모호 표현 금지 |
| **트리거** | 명세 표류의 출처 (테스트·리뷰·실 운영·인터랙션). 모호하면 한 단계 위(원본 명세 자체 문제)로 격상해 fix/sdd-lite 또는 sdd 검토 |
| **Story별 이력** | Story마다 별도 블록. 변경 전/후 AC 명시 — diff 명확성 |
| **검증** | 다 계층 테스트 필요 시 모두 명시 |
| **영향** | 원본 PES 파일에 [명세 변경 이력] 블록을 다는 것이 의무 (sdd 양식의 패턴과 동일) |

---

## 예시

```
# [Fix] LearningAxis addTopics 부분 성공 정책 정정 (2026-06-25)

## 대상 (원본)
- 원본 Product: `workflow/task/pes/sdd/Product.md` Product 2 (User BC 도메인 정합성 개선) — 인접 LearningFacade에도 영향
- 영향 받는 Story: Story 3-2 (LearningAxis addTopics), Story 3-3 (TopicMaterial 매핑)
- 본 fix 발견 시점: 2026-06-25

## 트리거
통합 테스트 중 addTopics(List<TopicCommand>) 입력 중 한 건만 검증 실패 시 나머지가 부분 성공으로 저장됨 발견. conventions.md §1.8 "다건 입력 부분 성공 불허" 룰 위반. 원본 AC가 "한 건이라도 실패 시 전체 롤백"을 명시하지 않음.

## 변경 내역

### Story별 명세 변경 이력

#### Story 3-2: LearningAxis addTopics
- **변경 전 AC**: Given 3건 입력 / When 2건째 검증 실패 / Then *(미명시)*
- **변경 후 AC**: Given 3건 입력 / When 2건째 검증 실패 / Then 전체 롤백 (0건 저장) + LEARNING_TOPIC_BATCH_VALIDATION_FAILED 응답
- **DoD 추가**: 단위 테스트 1건 (`addTopics_2건째_실패_전체_롤백`)
- **사유**: conventions.md §1.8 부분 성공 불허 룰 누락

### ErrorCode 변경
- 신규: `LT004` (400, "다건 입력 중 검증 실패가 발생했습니다.") — 부분 성공 차단용 응답

## 검증
- 단위: `./gradlew test --tests "*LearningAxisTest*"` 그린 (신규 1건 포함)
- 통합: `LearningFacadeIntegrationTest.addTopics_부분실패_전체롤백` 그린 (트랜잭션 경계 검증)

## 영향
- 원본 명세 `pes/sdd/Product.md` Story 3-2에 [명세 변경 이력] 블록 추가 (2026-06-25 본 fix 식별자)
- DOMAIN.md §LearningFacade 갱신 — addTopics 부분 성공 불허 명시
- 다른 BC: N/A (LearningFacade 내부 행위)
- ADR: ADR-CANDIDATES.md에 "다건 입력 정책" 후보 추가 — 정식 ADR 발행 보류
```

---

## 참조

- 빈 스켈레톤: `./template.md`
- pes 대응 (planning): `../../../../pes/workspectrum/pes/version/0.0.1v/pes.md`
- 이전 fix 단계: `../../../feature-story/version/0.0.1v/feature-story.md`
- 다음 fix 단계: `../../../sdd-lite/version/0.0.1v/sdd-lite.md`, `../../../sdd/version/0.0.1v/sdd.md`
- 양식 진화: SemVer 진화. 변경 시 `../0.0.2v/`에 새 버전
