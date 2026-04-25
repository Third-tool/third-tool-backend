# Review 도메인 모델

> 리뷰 세션을 생성·진행·종료하고, 카드 한 장의 공개 단계(RECALLING / COMPARING)를 관리하는 BC.

> 이 문서는 [`도메인모델.md`](도메인모델.md#review-bc--리뷰-세션-진행-및-카드-공개-흐름-관리)의 Review 섹션을 도메인별 파일로 분리한 신규 위치입니다. 작성 규칙은 [`_rules.md`](_rules.md) 참조.

---

## 구성 요소

| 구분 | 클래스 | 역할 |
|---|---|---|
| Aggregate Root | `ReviewSession` | 한 회차 리뷰의 진행 상태 관리 |
| Entity | `CardReview` | 세션 내 카드 한 장의 공개 단계 추적 |
| Value Object | `CardVisibleContent` | 현재 단계에서 노출할 영역(record) |
| Enum | `ReviewStep` | `RECALLING` / `COMPARING` |
| Exception | `ReviewSessionException` | BC 전용 예외 |

---

## ReviewSession (Aggregate Root)

> 한 회차의 리뷰 진행을 관리하는 Aggregate. 여러 `CardReview`를 보유.

### 설명
_TBD_ — 세션 시작/현재 카드 진행/종료 흐름을 정리.

### 속성 / 행위 / 규칙 / 검증
_TBD_ — `Review/domain/model/ReviewSession.java` 기준으로 채워주세요. 행위명 변경은 [`도메인모델.md`의 Review 섹션 변경 요약](도메인모델.md#변경-요약) 참조.

---

## CardReview (Entity)
→ [`도메인모델.md` CardReview 섹션](도메인모델.md#cardreview-entity--변경) 참조.

## CardVisibleContent (Value Object)
→ [`도메인모델.md` CardVisibleContent 섹션](도메인모델.md#cardvisiblecontent-value-object--추가) 참조.

## ReviewStep (Enum)
→ [`도메인모델.md` ReviewStep 섹션](도메인모델.md#reviewstep-enum--변경) 참조.

---

## 책임 분리

→ [`도메인모델.md` 책임 분리 섹션](도메인모델.md#책임-분리)의 CardReview / CardVisibleContent 항목 참조. ReviewSession 자체의 책임 분리는 본 문서에서 보강 예정.

### Card BC 협력 규칙
- ReviewSession은 Card ID만 참조하고 Card Aggregate를 직접 포함하지 않습니다 (BC 간 객체 참조 금지).
- ON_FIELD `viewCount` 증가 책임은 ReviewCommandService가 가집니다 (`Card.incrementViewCount()` 호출). 자세한 내용은 [`docs/architecture/package-structure-guide.md`](../architecture/package-structure-guide.md) 참조.
