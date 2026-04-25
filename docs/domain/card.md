# Card 도메인 모델

> 회상 가능한 학습 단위(MainNote · KeywordCue · Summary)를 묶고, 운영 위치(`ON_FIELD` / `ARCHIVE`) 전환과 ON_FIELD 체류 예산을 관리하는 BC.

> 이 문서는 [`도메인모델.md`](도메인모델.md#card-bc--회상-가능한-학습-단위의-생성수정상태-전환관계-탐색)의 Card 섹션을 도메인별 파일로 분리한 신규 위치입니다. 작성 규칙은 [`_rules.md`](_rules.md) 참조.

---

## 구성 요소

| 구분 | 클래스 | 역할 |
|---|---|---|
| Aggregate Root | `Card` | 학습 단위 일관성, 상태 전환, ON_FIELD 체류 추적 |
| Entity | `KeywordCue` | 회상 단서 |
| Entity | `Tag` | 카드 간 재회 통로 (전역 공유) |
| Entity | `CardTag` | Card-Tag 연결 명시화 |
| Entity | `CardStatusHistory` | 상태 전환 이력 |
| Value Object | `MainNote` | 학습 맥락 (텍스트/이미지) |
| Value Object | `Summary` | 1-3문장 압축 요약 |
| Value Object | `OnFieldBudget` | ON_FIELD 체류 예산 (조회수·기간) |
| Value Object | `SoftScheduleTemplate` | 복습 간격 템플릿 |
| Value Object | `RelatedCardCandidate` | 태그 공유 기반 관련 카드 후보 |
| Domain Service | `CardStatusHistoryAppender` | 상태 전환 이력 생성 중개 |
| Domain Service | `CardRelationFinder` | 관련 카드 후보 구성·정렬 |
| Domain Service | `CardExpiryPolicy` | ON_FIELD 만료 판단 및 자동 Archive |
| Enum | `CardStatus` | `ON_FIELD` / `ARCHIVE` |
| Enum | `MainContentType` | `TEXT_ONLY` / `IMAGE_ONLY` / `MIXED` |
| Enum | `ArchiveReason` | Archive 사유 분류 |
| Enum | `SoftScheduleState` | 복습 스케줄 상태 |
| Exception | `CardDomainException` | BC 전용 예외 |

---

## Card (Aggregate Root)

> 회상 가능한 학습 단위(Main / Keywords / Summary)와 운영 위치 + 체류 예산을 보유.

### 설명
→ [`도메인모델.md` Card 섹션](도메인모델.md#card-aggregate-root) 참조.

### 속성
→ 위 링크 참조. 신규 변경분은 본 섹션에 추가합니다.

### 행위
→ 위 링크 참조.

### 규칙
→ 위 링크 참조.

### 검증
→ 위 링크 참조.

---

## MainNote (Value Object)
→ [`도메인모델.md` MainNote 섹션](도메인모델.md#mainnote-value-object) 참조.

## Summary (Value Object)
→ [`도메인모델.md` Summary 섹션](도메인모델.md#summary-value-object) 참조.

## KeywordCue (Entity)
→ [`도메인모델.md` KeywordCue 섹션](도메인모델.md#keywordcue-entity) 참조.

## Tag (Entity)
→ [`도메인모델.md` Tag 섹션](도메인모델.md#tag-entity) 참조.

## CardTag (Entity)
→ [`도메인모델.md` CardTag 섹션](도메인모델.md#cardtag-entity) 참조.

## CardStatus (Enum)
→ [`도메인모델.md` CardStatus 섹션](도메인모델.md#cardstatus-enum) 참조.

## CardStatusHistory (Entity)
→ [`도메인모델.md` CardStatusHistory 섹션](도메인모델.md#cardstatushistory-entity) 참조.

## OnFieldBudget (Value Object)
→ [`도메인모델.md` OnFieldBudget 섹션](도메인모델.md#onfieldbudget-value-object) 참조.

## RelatedCardCandidate (Value Object)
→ [`도메인모델.md` RelatedCardCandidate 섹션](도메인모델.md#relatedcardcandidate-value-object) 참조.

---

## SoftScheduleTemplate (Value Object)

> 카드 복습 주기 템플릿. (예: 1일·3일·7일·14일 …)

### 설명
_TBD_ — `Card/domain/model/SoftScheduleTemplate.java` 기준으로 채워주세요.

### 속성 / 행위 / 규칙 / 검증
_TBD_

## SoftScheduleState
_TBD_

## ArchiveReason (Enum)
_TBD_ — Archive 전환 사유 분류값. 자동 만료(`MAX_VIEW`, `MAX_DURATION`)와 수동(`MANUAL`)을 구분.

## MainContentType (Enum)
→ [`도메인모델.md` MainNote 검증 섹션](도메인모델.md#mainnote-value-object) 참조 (값 결정 규칙 포함).

---

## CardStatusHistoryAppender (Domain Service)
→ [`도메인모델.md`](도메인모델.md#cardstatushistoryappender-domain-service) 참조.

## CardRelationFinder (Domain Service)
→ [`도메인모델.md`](도메인모델.md#cardrelationfinder-domain-service) 참조.

## CardExpiryPolicy (Domain Service)
→ [`도메인모델.md`](도메인모델.md#cardexpirypolicy-domain-service) 참조.

---

## 책임 분리

→ [`도메인모델.md` 책임 분리 섹션](도메인모델.md#책임-분리) 참조 (Card Aggregate / CardStatusHistory / OnFieldBudget / CardExpiryPolicy / CardRelationFinder / CardStatusHistoryAppender).
