# [Fix · Feature-story] 양식 정의

> **fix 트랙**: 이미 진행 중인 작업 안에서 발견된 **한 메서드 / 한 PR 내부 소규모 변경**의 트래킹 문서.
> planning은 `../../../../pes/workspectrum/feature-story/version/0.0.1v/feature-story.md`(시간 기반 1~3일, 단일 PR) 참조.
> fix는 **수정 범위·복잡도** 기준 5단계 중 2단계 — 한 단위 행위 안의 보강 변경.

---

## 사용 시점 (트리거)

다음 조건 중 **2개 이상**을 만족할 때 본 양식을 쓴다.

- [ ] 원본 작업의 **한 메서드 / 한 PR 내부**에서 발견된 수정
- [ ] 수정 범위가 **한 메서드 시그니처 / 한 분기 / 한 검증** 정도 — 2~20 라인
- [ ] 단위 테스트 **1~2건 추가/수정** 동반
- [ ] 원본 AC 한 줄이 갱신되어야 함 (한 줄 → 두 줄, 또는 Given 추가)
- [ ] 다른 Story·다른 BC 영향 **0건**

**졸업 신호 → fix/pes로**:
- Story 2+개에 영향이 번짐
- ErrorCode 신규 등록 / DTO 필드 추가 / Repository 메서드 신설
- 다른 BC가 본 변경을 함께 반영해야 함

---

## 양식 골격

```
# [Fix] {수정 요지 — 동사로 시작} ({YYYY-MM-DD})

## 대상 (원본)
- 원본 PR / Story: {식별자 + 링크}
- 본 fix 발견 시점: {YYYY-MM-DD}
- 영향 받는 메서드 / 클래스: {경로 + 메서드명}

## 트리거
- {리뷰 코멘트 / 테스트 실패 / 클로드 코드와의 인터랙션 / 명세 표류}

## 변경 내역

### 코드
- {파일:라인} {무엇} ({before → after 요지})
- {파일:라인} {무엇}

### AC 보강 (해당 시)
- *(추가 또는 갱신)* Given ... / When ... / Then ...

## 검증
- 단위 테스트: {N건 추가/수정} — `./gradlew test --tests "..."` 그린
- (해당 시) 슬라이스 / 통합 테스트: {결과}

## 영향
- 원본 명세 갱신 필요? — {DOMAIN.md 한 섹션 / N/A}
- 원본 ADR 변경? — {N/A 또는 ADR NNN 보강 메모}
- 다른 Story / BC — {N/A}
```

### 섹션 가이드

| 섹션 | 채울 것 |
| --- | --- |
| **대상** | 원본 Story와 영향 받는 메서드까지 명시. 한 메서드 단위 정확하게 |
| **트리거** | 1-2줄. "리뷰: ARCHIVE 카드 recordView 호출 시 viewCount 1 누락 발견" 같은 사실 |
| **변경 내역** | 파일:라인 + 무엇 (before/after는 한 줄 요지로). AC 추가가 있으면 별도 블록 |
| **검증** | 단위 테스트 명령 + 결과. 슬라이스/통합 테스트가 추가됐다면 같이 |
| **영향** | DOMAIN.md / ADR / 다른 Story 영향을 명시. N/A면 명시 (졸업 신호 점검에 쓰임) |

---

## 예시

```
# [Fix] Card.recordView ARCHIVE 멱등 처리 누락 보강 (2026-06-25)

## 대상 (원본)
- 원본 Story: pes/sdd/Product.md Story 5-1 (Card recordView 통합)
- 본 fix 발견 시점: 2026-06-25 (M1 통합 테스트 중)
- 영향 받는 메서드: `Card.recordView(Clock)` (Card 도메인)

## 트리거
- 통합 테스트 중 ARCHIVE 상태 카드에 recordView 호출 시 viewCount += 1 발생. 원본 conventions.md §1.5 멱등성 룰 위반.

## 변경 내역

### 코드
- `src/main/java/com/example/thirdtool/Card/domain/model/Card.java:142` recordView 진입부에 ARCHIVE 가드 추가 (`if (status == ARCHIVE) return;`)

### AC 보강
- *(추가)* Given ARCHIVE 상태 Card / When recordView 호출 / Then 상태·필드 모두 불변

## 검증
- 단위 테스트: 1건 추가 (`CardTest.recordView_ARCHIVE_무시`) — `./gradlew test --tests "*CardTest*"` 그린

## 영향
- DOMAIN.md §Card 갱신 — `recordView` 멱등성 명시 추가 (별도 fix/one-line-spec 분기로 처리)
- ADR 변경 X (conventions.md §1.5에 이미 명시된 패턴 — 단순 누락 보강)
- 다른 BC — N/A
```

---

## 참조

- 빈 스켈레톤: `./template.md`
- pes 대응 (planning): `../../../../pes/workspectrum/feature-story/version/0.0.1v/feature-story.md`
- 이전 fix 단계: `../../../one-line-spec/version/0.0.1v/one-line-spec.md`
- 다음 fix 단계: `../../../pes/version/0.0.1v/pes.md`
- 양식 진화: SemVer 진화. 변경 시 `../0.0.2v/`에 새 버전
