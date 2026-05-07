# Rule: API 컨벤션

> private-docs/api/* 전반에서 일관된 REST API 작성 규칙. Controller / Request·Response DTO / GlobalExceptionHandler 작성 시 적용한다.
> CLAUDE.md "예외와 ErrorCode" 섹션을 보강한다.

---

## 1. URL 컨벤션

- **Base URL**: 모든 엔드포인트는 `/api/v1` 으로 시작한다.
- **Content-Type**: `application/json` (요청·응답).
- **리소스 식별자는 path variable**: `/cards/{cardId}`, `/decks/{deckId}/cards`, `/cards/{cardId}/keywords/{keywordCueId}`.
- **소유 BC 단위 prefix 사용**: 카드 → `/cards`, 덱 → `/decks`, 학습 자료 → `/materials`, LearningFacade → `/facades` (또는 `/facade/me`).
- **계층 표현은 path 중첩**: `/cards/{cardId}/keywords`, `/cards/{cardId}/tags` — 부모-자식 관계가 명확할 때.
- **검색·필터는 쿼리 파라미터**: `GET /decks/{deckId}/cards?available=true`. 의미 없는 값(`available=false` 같은)은 미입력과 동일하게 처리한다.

---

## 2. HTTP 메서드 ↔ 도메인 행위 매핑

| 메서드 | 용도 | 응답 코드 |
| --- | --- | --- |
| `POST` | 신규 생성, 단건 추가 | `201 Created` |
| `GET` | 조회 (단건·목록) | `200 OK` |
| `PATCH` | **부분 수정** (단일 필드 또는 필드 그룹) | `200 OK` |
| `PUT` | **전체 교체** (컬렉션 또는 리소스 전체) | `200 OK` |
| `DELETE` | 삭제 (Soft Delete 포함) | `204 No Content` |

**원칙**: 부분 필드 수정은 `PATCH`, 컬렉션 전체 교체는 `PUT`. 둘을 섞지 않는다. 예: `PATCH /cards/{id}/main-note`(부분), `PUT /cards/{id}/keywords`(전체 교체), `POST /cards/{id}/keywords`(단건 추가), `DELETE /cards/{id}/keywords/{keywordCueId}`(단건 제거).

---

## 3. 에러 응답 형식 (전역 고정)

```json
{
  "code": "CARD_KEYWORD_MIN_REQUIRED",
  "message": "키워드는 최소 1개 이상이어야 합니다."
}
```

- **변환은 `Common/Exception/GlobalExceptionHandler`가 단일 진입점**으로 처리. Controller에서 try-catch 또는 응답 가공 금지.
- `code`는 항상 `Common/Exception/ErrorCode/ErrorCode` enum의 값과 일치한다.
- `message`는 ErrorCode enum에 정의된 한국어 메시지를 그대로 사용한다 (Controller에서 메시지 가공 금지).

---

## 4. ErrorCode 네이밍 컨벤션

`{BC_PREFIX}_{도메인_요소}_{상태}` 형식의 SCREAMING_SNAKE_CASE.

| 패턴 | 예시 |
| --- | --- |
| 존재하지 않음 | `CARD_NOT_FOUND`, `DECK_NOT_FOUND`, `CARD_KEYWORD_NOT_FOUND` |
| 최소 개수 미충족 | `CARD_KEYWORD_MIN_REQUIRED` |
| 최대 개수 초과 | `CARD_TAG_LIMIT_EXCEEDED` |
| 빈 값 / blank | `CARD_KEYWORD_BLANK`, `TAG_VALUE_BLANK`, `CARD_SUMMARY_EMPTY` |
| 마지막 항목 제거 시도 | `CARD_KEYWORD_LAST_CANNOT_REMOVE` |
| 형식 범위 초과 | `CARD_SUMMARY_SENTENCE_OUT_OF_RANGE` |
| 중복 (이미 존재) | `CARD_TAG_ALREADY_EXISTS`, `LEARNING_AXIS_DUPLICATE_NAME` |
| 잘못된 필드 조합 | `CARD_MAIN_NOTE_EMPTY` |

**HTTP 상태 매핑 가이드**:
- `404` — `*_NOT_FOUND`
- `400` — 검증 실패 전반 (`*_BLANK`, `*_MIN_REQUIRED`, `*_OUT_OF_RANGE`, `*_LIMIT_EXCEEDED` 등)
- `409` — 중복 충돌 (`*_ALREADY_EXISTS`, `*_DUPLICATE_*`)

---

## 5. 새 ErrorCode 등록 절차

새 도메인 검증 규칙을 추가할 때:

1. `Common/Exception/ErrorCode/ErrorCode` enum에 **먼저 코드 등록** (이름 + 메시지 + HttpStatus).
2. 도메인 또는 Application Service에서 BC 전용 도메인 예외(`{BC}DomainException`)를 그 코드로 던진다.
3. API 명세 문서(`private-docs/api/{bc}.md`)의 "에러" 표에 코드 한 줄 추가는 **사용자 영역** — Claude Code는 코드만 추가하고 문서 갱신은 사용자에게 위임 (`private-docs.md` 규칙 §3.2).

---

## 6. Request / Response DTO 패턴

### Request
- **검증 어노테이션은 DTO에서**: `@NotBlank`, `@Size`, `@Valid` 등 Bean Validation. 도메인 검증과 별개로 입력 단계 가드.
- **선택 필드는 nullable** + 명시적으로 "선택"을 API 명세에 적는다. 예: `tags`, `description`, `imageUrl`.
- **빈 컬렉션 vs null** — 컬렉션은 입력 안 하면 빈 컬렉션으로 처리 (예: `tags` 미입력 → `[]`로 처리).

### Response
- **id 필드는 항상 포함** (`cardId`, `deckId`, `axisId`, `topicId`).
- **enum은 문자열로 직렬화** (도메인 측에서 `@Enumerated(EnumType.STRING)` 사용 — `db-conventions.md` 참고).
- **timestamp는 ISO-8601** (`LocalDateTime` 직렬화 기본값).
- **목록 응답은 본문 무거운 필드 제외**: 목록 조회는 `mainNote.textContent` 같은 큰 필드를 제외하고 요약 형태로 반환 (예: Card 목록은 `summary`, `keywords`, `tags`만, mainNote 본문 제외).
- **권장 한도 초과 플래그는 응답 DTO에 포함**: `isAxisCountExceedsRecommended`, `isTopicCountExceedsRecommended`, `isFocused` 같은 boolean 플래그를 Application Service가 채운다.

---

## 7. 클라이언트 비노출 필드 (서버 내부 전용)

다음 필드는 **클라이언트가 직접 설정하는 경로를 노출하지 않는다**. ReviewSession 등 정해진 흐름 안에서 서버 내부적으로만 갱신한다.

| 필드 | 갱신 경로 |
| --- | --- |
| `Card.lastViewedAt` | `Card.recordView()` (ReviewSession에서만 호출) |
| `Card.viewCount` | `Card.recordView()` |
| `Card.enteredFieldAt` | `Card.create()` / `Card.returnToField()` |
| `AxisTopic.coverageStatus` | TopicMaterial 연결·해제 / proficiencyLevel 변경 시 Application Service가 갱신 |
| audit 필드 (`createdAt`, `updatedAt`, `linkedAt`, `changedAt`) | JPA Auditing 또는 생성 시점 자동 기록 |

이 필드들에 대한 별도 수정 엔드포인트(`PATCH /cards/{id}/last-viewed-at` 같은)를 만들지 않는다.

---

## 8. find-or-create 패턴

태그처럼 시스템 전역 유니크한 리소스를 카드에 연결할 때:

- 클라이언트는 **value(이름)만 전달**한다.
- 서버는 시스템에 해당 value의 리소스가 있으면 **재사용**, 없으면 **새로 생성** 후 연결한다.
- API 명세에 `(findOrCreate)` 표기를 명시한다 (예: `POST /cards/{cardId}/tags`).

---

## 9. v1 응답 호환성

도메인이 변경되어도 API 응답 스키마는 **하위 호환**을 유지한다 (필드 추가는 OK, 제거·이름 변경은 Migration 시점에서만).

- 새 필드 추가 시 응답 DTO에 nullable로 추가 → API 명세 "변경 이력" 표에 기재.
- 필드 제거가 필요하면 새 엔드포인트 버전(`/api/v2`)으로 분리하거나 Story로 명시해 사용자 검토를 받는다.
