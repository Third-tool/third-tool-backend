# API Spec (Living)

> **성격**: living-docs — 항상 최신본. 코드의 Controller가 진실 소스, 본 문서는 팀·프론트 참조용 요약.
> **성장 방향**: 엔드포인트가 늘면 BC별 파일로 분화 (`api-spec/card.md`, `api-spec/deck.md` …).
> **관련 topology**: `workflow/topologys/versions/{Nv}/api-spec.md` — HTTP graph 위상·계약 규칙은 topology.
> **자동 문서**: 실행 후 `http://localhost:8080/swagger-ui.html` (springdoc 기반).
> **응답 래퍼**: 모든 응답은 `ApiResponse<T>` 래핑, 에러는 `GlobalExceptionHandler`가 `{code, message}`로 변환.

---

## 공통 규약

- **Base URL**: `/api/v1` 으로 시작 (예외: `/login`, `/user*`, `/jwt/refresh`, `/social/*`, `/health`).
- **Content-Type**: `application/json`.
- **인증**: `access_token` 쿠키 (HttpOnly, SameSite=Strict). Refresh는 `/jwt/refresh` 재발급.
- **HTTP 매핑 원칙** (conventions.md §2.2):
  - `POST` — 생성 · 상태 전이 액션 · 201
  - `GET` — 조회 · 200
  - `PATCH` — 부분 수정 · 200
  - `PUT` — 전체 교체 (컬렉션 · 순서 재부여) · 200
  - `DELETE` — 삭제 · 204

---

## 엔드포인트 카탈로그 (BC별)

### Auth · User

| Method | Path | 목적 | Controller |
| --- | --- | --- | --- |
| POST | `/login` | 로컬 로그인 | `UserController` |
| POST | `/user/exist` | 이메일 존재 확인 | `UserController` |
| POST | `/user` | 회원가입 | `UserController` |
| GET | `/user` | 내 프로필 조회 | `UserController` |
| PUT | `/user` | 내 프로필 수정 | `UserController` |
| DELETE | `/user` | 회원 탈퇴 | `UserController` |
| POST | `/social/login/{provider}` | 소셜 로그인 (kakao/naver) | `SocialLoginController` |
| POST | `/jwt/refresh` | Refresh Token으로 재발급 | `JwtController` |
| GET | `/api/auth/test` | 인증 테스트 | `TestController` (개발용) |

### Card — **M4 재편**

Prefix: `/api/v1`

**M4 응답 변화**:
- `CardResponse`에 `axisId` (M4 LT E4) · `createdMode` (M4 CARD E3) 필드 추가 노출
- `budget` 관련 필드 완전 폐기 (OnFieldBudget 삭제)
- `topicId` 필드는 soft-deprecate (여전히 노출 · 다음 릴리스 삭제 예정)
- `archiveReason` 값이 3종 재정의 (`MANUAL` / `SCHEDULE_EXHAUSTED` / `MODE_DOWNGRADED`)

| Method | Path | 목적 |
| --- | --- | --- |
| POST | `/decks/{deckId}/cards` | 카드 생성 (**M4**: axisId 파라미터 필수 · createdMode 자동 저장) |
| GET | `/cards/{cardId}` | 단건 조회 |
| GET | `/decks/{deckId}/cards` | 덱의 카드 목록 |
| PATCH | `/cards/{cardId}/main-note` | MainNote 수정 |
| PATCH | `/cards/{cardId}/summary` | Summary 수정 |
| PUT | `/cards/{cardId}/keywords` | 키워드 전체 교체 |
| POST | `/cards/{cardId}/keywords` | 키워드 단건 추가 |
| DELETE | `/cards/{cardId}/keywords/{keywordCueId}` | 키워드 삭제 |
| POST | `/cards/{cardId}/tags` | 태그 단건 추가 (find-or-create) |
| PUT | `/cards/{cardId}/tags` | 태그 전체 교체 |
| DELETE | `/cards/{cardId}/tags/{tagId}` | 태그 연결 해제 |
| GET | `/cards/{cardId}/related` | 관련 카드 조회 |
| DELETE | `/cards/{cardId}` | 카드 삭제 (soft) |
| POST | `/cards/{cardId}/archive` | 아카이브 상태 전이 |
| POST | `/cards/{cardId}/return-to-field` | ON_FIELD로 복귀 |
| GET | `/tags/{tagId}/cards` | 태그에 연결된 카드 목록 |
| GET | `/cards/{cardId}/related-archive` | 관련 카드 (아카이브 포함) |

### Tag

| Method | Path | 목적 |
| --- | --- | --- |
| GET | `/api/v1/tags` | 태그 검색·목록 |
| DELETE | `/api/v1/tags/{tagId}` | 태그 삭제 (시스템 전역) |

### Deck

Prefix: `/api/v1/decks`

| Method | Path | 목적 |
| --- | --- | --- |
| GET | `/{deckId}` | 단건 조회 |
| GET | `/{deckId}/sub-decks` | 하위 덱 목록 |
| PATCH | `/{deckId}/name` | 덱 이름 수정 |
| PATCH | `/{deckId}/parent` | 상위 덱 재지정 |
| DELETE | `/{deckId}` | 덱 삭제 (soft) |
| PATCH | `/{deckId}/last-accessed` | 마지막 접근 시각 갱신 |

### Review

| Method | Path | 목적 |
| --- | --- | --- |
| POST | `/api/v1/reviews` | 리뷰 세션 시작 |
| GET | `/api/v1/reviews/{sessionId}` | 세션 조회 |
| PATCH | `/api/v1/reviews/{sessionId}/comparing` | COMPARING 단계로 전환 |
| PATCH | `/api/v1/reviews/{sessionId}/next` | 다음 카드로 이동 |
| GET | `/api/v1/reviews` | 세션 목록 |
| GET | `/api/v1/review-session/today` | 오늘 세션 |

### UserSchedule — **M4 재편**

Prefix: `/api/v1/users/me/schedule` (일부는 `/api/v1/schedule-config`)

**M4 변화**:
- `LearningMode` 4개 값 확정 (`MODE_7D` / `MODE_14D` / `MODE_28D` / `MODE_60D`)
- `rawInputDays` 60일 초과 자동 clamp · 응답에 `wasClamped: true` + `code: MODE_60D_CLAMPED`
- 기존 `MODE_10D` 유저는 V23 마이그레이션으로 `MODE_7D` 자동 재매핑

| Method | Path | 목적 |
| --- | --- | --- |
| POST | `/schedule-config` | LearningMode 설정 (**M4**: `rawInputDays` 입력 · 자동 매핑 + clamp) |
| GET | `/history` | 스케줄 변경 이력 |
| PATCH | `/daily-target` | 일일 목표 카드 수 조정 |

### LearningFacade — 주 컨트롤러

Prefix: `/api/v1/learning-facade`

| Method | Path | 목적 |
| --- | --- | --- |
| PATCH | `/concepts` | concepts[] 전체 갱신 |
| POST | `/axes` | 축 생성 (default Layer 자동 배정) |
| PATCH | `/axes/{axisId}` | 축 이름 수정 |
| DELETE | `/axes/{axisId}` | 축 삭제 (soft) |
| PUT | `/axes/order` | 축 순서 재부여 |
| POST | `/layers` | Layer 생성 |
| PATCH | `/layers/{layerId}` | Layer 이름 수정 |
| DELETE | `/layers/{layerId}` | Layer 삭제 (soft, 활성 axis 있으면 409) |
| PUT | `/layers/order` | Layer 순서 재부여 |
| POST | `/axes/{axisId}/topics` | 주제 생성 |
| PATCH | `/axes/{axisId}/topics/{topicId}` | 주제 수정 (revision 기록) |
| DELETE | `/axes/{axisId}/topics/{topicId}` | 주제 삭제 |
| PUT | `/axes/{axisId}/topics/order` | 주제 순서 재부여 |
| POST | `/materials` | 학습 자료 등록 |
| GET | `/materials` | 자료 목록 조회 |
| PATCH | `/materials/{materialId}/name` | 자료명 수정 |
| PATCH | `/materials/{materialId}/proficiency` | 숙련도 수정 (UNRATED 되돌리기 금지) |
| POST | `/materials/{materialId}/topics` | 자료↔주제 연결 |
| DELETE | `/materials/{materialId}/topics/{topicId}` | 연결 해제 |
| DELETE | `/materials/{materialId}` | 자료 삭제 (soft) |
| GET | `/topics/{topicId}/revisions` | 주제 수정 이력 |
| GET | `/revision-reason-options` | 수정 사유 옵션 목록 |
| GET | `/axes/{axisId}/topic-deletions` | 축 하위 주제 삭제 이력 |
| GET | `/axes/{axisId}/cards` | 축 하위 카드 조회 |

### LearningFacade — AxisRoadmapNode (챕터 노드)

Prefix: `/api/v1`

| Method | Path | 목적 |
| --- | --- | --- |
| POST | `/axes/{axisId}/roadmap-nodes` | Roadmap 노드 생성 |
| GET | `/axes/{axisId}/roadmap-nodes` | Roadmap 노드 목록 |
| PATCH | `/roadmap-nodes/{nodeId}` | Roadmap 노드 수정 |
| DELETE | `/roadmap-nodes/{nodeId}` | Roadmap 노드 삭제 |
| PUT | `/axes/{axisId}/roadmap-nodes/order` | Roadmap 노드 순서 재부여 |

### LearningFacade — AxisSelection (판례 컨테이너)

Prefix: `/api/v1`

| Method | Path | 목적 |
| --- | --- | --- |
| POST | `/axes/{axisId}/selections` | Selection 생성 |
| GET | `/axes/{axisId}/selections` | Selection 목록 |
| PATCH | `/selections/{selectionId}` | Selection 이름 수정 |
| DELETE | `/axes/{axisId}/selections/{selectionId}` | Selection 삭제 (hard) |
| POST | `/selections/{selectionId}/nodes` | Selection 노드 생성 |
| GET | `/selections/{selectionId}/nodes` | Selection 노드 목록 |
| PATCH | `/selection-nodes/{nodeId}` | Selection 노드 수정 |
| DELETE | `/selection-nodes/{nodeId}` | Selection 노드 삭제 |
| PUT | `/selections/{selectionId}/nodes/order` | Selection 노드 순서 재부여 |

### LearningFacade — Suggestion (AI 6-Port) — **M4 role 4종 확장**

Prefix: `/api/v1/suggestions`

| Method | Path | 목적 |
| --- | --- | --- |
| POST | `/chapters-outline` | Roadmap 챕터 아웃라인 제안 |
| POST | `/chapter-subtree` | Roadmap 챕터 하위 트리 제안 |
| POST | `/selection-outline` | Selection 아웃라인 제안 |
| POST | `/selection-subtree` | Selection 하위 트리 제안 |

**응답 확장** (M4 · AS E3):
- `providerContext` 필드가 4개 값 지원: `static:backend-developer` · `static:planner` · `static:designer` · `static:problem-solver`
- `RoleDetector`가 concepts 배열에서 role 감지 · 알 수 없는 role → `backend-developer` fallback
- M6에 LLM Adapter 추가 예정 (`llm:vertex-ai-gemini-2.5-flash` 값 등장)

### Health

| Method | Path | 목적 |
| --- | --- | --- |
| GET | `/health` | 헬스 체크 |

---

## 응답 형식 (표준)

### 성공
```json
{
  "success": true,
  "data": { ... },
  "message": null
}
```

### 실패 (GlobalExceptionHandler)
```json
{
  "code": "CARD_KEYWORD_MIN_REQUIRED",
  "message": "카드는 키워드를 최소 1개 이상 가져야 합니다."
}
```

ErrorCode 카탈로그 전체: `workflow/living-docs/error-code/error-code.md`.

---

## 참조

- 코드 (진실 소스): `src/main/java/com/example/thirdtool/*/presentation/*Controller.java`
- Swagger: `/swagger-ui.html`
- 관련 topology: `workflow/topologys/versions/{Nv}/api-spec.md`
- 관련 living-docs: `error-code/`, `architecture-system-design/`

*최신 갱신: 2026-07-21 · **M4 반영** — Card Response `axisId`/`createdMode` 노출 · UserSchedule 4-Mode + 60일 clamp · Suggestion 4-role providerContext · `budget` 응답 필드 폐기 · `topic_id` soft-deprecate*
