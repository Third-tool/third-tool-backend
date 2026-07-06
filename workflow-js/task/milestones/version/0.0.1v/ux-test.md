# 0.0.1v UX 테스트 — 화면 직접 사용 점검표

> **목적**: v0.0.1에서 done 처리된 세 Product(Auth·Card·LearningFacade) + 지원 BC(Deck·Review·UserSchedule)를 **브라우저 화면에서 직접 조작**해 사용자가 실제로 겪을 흐름을 점검한다.
>
> **진실 소스**: `product-auth.md` / `product-card.md` / `product-learningFacade.md` — 본 파일에서 기대 결과를 도출했으며, 도메인 불변식·에러 코드는 해당 SDD 기준이다.
>
> **환경**: 로컬(`localhost:8080` + `localhost:5173`) 또는 dev(`https://{dev_alb_domain}`). 두 환경 모두 동일 체크리스트 사용. dev는 섹션 8에서 추가 확인 항목 별도 명시.
>
> **방법**: 각 항목을 브라우저에서 직접 수행 후 `- [ ]` → `- [x]` 체크. 실패 시 실제 결과와 오류 코드를 인라인에 기록.

---

## 테스트 범위 & 우선순위

| 섹션 | 대상 | 우선순위 | 항목 수 |
|------|------|---------|---------|
| 1. 인증 / 사용자 | product-auth.md (done) | P0 | 12 |
| 2. LearningFacade 구성 | product-learningFacade.md (done) | P0 | 14 |
| 3. 카드 라이프사이클 | product-card.md (done) | P0 | 16 |
| 4. 복습 세션 | Review BC (지원) | P1 | 7 |
| 5. 덱 관리 | Deck BC (지원) | P1 | 8 |
| 6. UserSchedule 설정 | UserSchedule BC (지원) | P1 | 4 |
| 7. 에러 · 엣지 케이스 | 도메인 불변식 경계 | P0 | 14 |
| 8. 운영 관측성 | 인프라 (infra done) | P2 | 6 |

---

## 사전 준비

테스트 시작 전 다음을 확인한다.

- [ ] BE 부팅 통과: `curl http://localhost:8080/health` → 200 OK
- [ ] Swagger UI 접근: `http://localhost:8080/swagger-ui.html` → 화면 진입
- [ ] FE 실행: `npm run dev` → `http://localhost:5173` → 화면 로드 (빈 화면이라도 404/500 없어야)
- [ ] H2 콘솔 접근(선택): `http://localhost:8080/h2-console` → 연결 성공

---

## 1. 인증 / 사용자 관리 (product-auth.md)

> AT = HttpOnly Cookie(30분), RT = 응답 바디(7일, 메모리 보관) — ADR009

### 1-1. 자체 회원가입 → 로그인

- [ ] `POST /user`에 `username / password / nickname / email` 입력 → 201, `{userEntityId: N}` 반환
- [ ] 바로 `POST /login` → 200
  - 응답 바디에 `refreshToken` 존재
  - 응답 헤더에 `Set-Cookie: access_token=...; HttpOnly; SameSite=Strict`
- [ ] 로그인 직후 `GET /user` → 200, 방금 가입한 nickname/email 반환 (AT 쿠키 자동 전송 확인)

실제 결과: ___

### 1-2. 중복 가입 차단

- [ ] 동일 `username`으로 다시 `POST /user` → 409, code=`USER003`
- [ ] 에러 메시지가 화면에 toast / inline으로 표시됨

실제 결과: ___

### 1-3. 잘못된 자격증명 로그인

- [ ] 존재하지 않는 username으로 `POST /login` → 401, code=`USER001` 또는 `USER005` (보안상 메시지 통일)
- [ ] 올바른 username + 틀린 password → 동일 에러 코드 (구분 불가 의도)

실제 결과: ___

### 1-4. 회원 정보 수정

- [ ] 로그인 상태에서 `PUT /user` `{nickname: "새닉네임", email: "new@example.com"}` → 200, userId 반환
- [ ] 이후 `GET /user` → 수정된 nickname/email 반영
- [ ] **수정 불가 검증**: `username` 또는 `password` 필드 포함해 `PUT /user` 전송 시 → BE가 무시하거나 필드 반영 안 됨 (Story 5-4 결과 — `UserUpdateRequestDTO`에 없는 필드)

실제 결과: ___

### 1-5. 소셜 로그인 (카카오)

- [ ] `POST /social/login/kakao` 흐름 시작 → 카카오 OAuth 동의 화면 이동
- [ ] 동의 완료 후 콜백 → `localhost:5173/oauth/kakao/callback`로 리다이렉트
- [ ] 콜백에서 `TokenResponse { refreshToken }` + `Set-Cookie: access_token` 수신
- [ ] `GET /user` → 소셜 계정 정보(nickname 등) 반환

실제 결과: ___

### 1-6. AT 만료 후 RT 갱신

- [ ] 로그인 후 AT TTL(30분) 경과 또는 쿠키 수동 삭제 후 인증 API 호출 → 401 `AUTH002`
- [ ] FE axios interceptor가 `POST /jwt/refresh` 자동 호출 → 새 AT 쿠키 발급
- [ ] 실패한 원래 요청 재시도 → 200 성공

실제 결과: ___

### 1-7. 만료된 RT로 refresh 시도

- [ ] 7일 지난 RT로 `POST /jwt/refresh` → 401, code=`AUTH101`~`AUTH104` 중 하나
- [ ] FE가 세션 종료 처리 (로그인 페이지 리다이렉트 등)

실제 결과: ___

### 1-8. 인증 없이 보호 API 호출

- [ ] 로그아웃 상태(AT 쿠키 없음)에서 `GET /user` → 401, code=`AUTH001` 또는 `USER002`
- [ ] FE 인터셉터가 refresh 시도 → RT도 없으면 세션 종료

실제 결과: ___

---

## 2. LearningFacade 구성 (product-learningFacade.md)

> 유저당 1 Facade. 4-level 계층: Facade → Axis(축) → Topic(주제) → Material(자료)

### 2-1. Facade 최초 생성

- [ ] `POST /api/v1/learning-facade` → `{ concept: "나는 백엔드 엔지니어가 되고 싶다" }` → 201 Created
- [ ] `GET /api/v1/learning-facade` → 생성된 Facade + concept 반환, axis 목록 빈 배열

실제 결과: ___

### 2-2. 유저당 1개 제한

- [ ] 로그인한 상태에서 `POST /api/v1/learning-facade` 재호출 → 409, code=`LF001` 또는 유사 중복 에러

실제 결과: ___

### 2-3. 학습 축(Axis) 추가

- [ ] `POST /api/v1/learning-facade/axes` → `{ name: "분산 시스템" }` → 201
  - 응답에 `displayOrder: 1` 포함
- [ ] 두 번째 축 추가 `{ name: "아키텍처 설계" }` → `displayOrder: 2`
- [ ] 권장 한도(5개) 도달: 5번째 추가 시 응답에 `isAxisCountExceedsRecommended: false`
- [ ] 6번째 추가 시 → 저장 성공 (차단 아님) + `isAxisCountExceedsRecommended: true`

실제 결과: ___

### 2-4. Axis 이름 수정

- [ ] `PATCH /api/v1/learning-facade/axes/{axisId}/name` → `{ name: "분산 시스템 2.0" }` → 200
- [ ] 동일 이름으로 다시 수정 → 200 (변경 없음 반환, `isChanged: false` 또는 유사)
- [ ] 같은 Facade 내 다른 Axis와 중복 이름 → 409, code=`LA001`(또는 `LEARNING_AXIS_DUPLICATE_NAME`)

실제 결과: ___

### 2-5. Axis 순서 재배치

- [ ] Axis 2개 이상 상태에서 `PUT /api/v1/learning-facade/axes/reorder` → `{ orderedIds: [2, 1] }` → 200
- [ ] 이후 조회 시 displayOrder 1, 2가 역순으로 재부여됨

실제 결과: ___

### 2-6. 주제(Topic) 추가

- [ ] `POST /api/v1/learning-facade/axes/{axisId}/topics` → `{ name: "합의 알고리즘" }` → 201, `displayOrder: 1`
- [ ] 두 번째 주제 추가 → `displayOrder: 2`
- [ ] 권장 한도(10개) 초과 시 `isTopicCountExceedsRecommended: true`

실제 결과: ___

### 2-7. 학습 자료(Material) 추가 및 Topic 연결

- [ ] `POST /api/v1/materials` → `{ name: "Designing Data-Intensive Applications", type: "BOOK" }` → 201
- [ ] `POST /api/v1/learning-facade/axes/{axisId}/topics/{topicId}/materials/{materialId}` → 연결 성공
  - 응답에 해당 Topic의 `coverageStatus: PARTIAL` (자료 1개 연결, proficiencyLevel 기본값)

실제 결과: ___

### 2-8. coverageStatus 동기 갱신 확인

- [ ] Topic에 Material 1개 연결 → `coverageStatus: NO_MATERIAL → PARTIAL` (즉시 응답에 반영)
- [ ] Material 연결 해제 → `coverageStatus: PARTIAL → NO_MATERIAL` (즉시)
- [ ] proficiencyLevel 갱신 → coverageStatus 재계산 반영 확인

실제 결과: ___

### 2-9. Material 중복 연결 방지

- [ ] 동일 Topic에 동일 Material을 두 번 연결 → 409, code=`TM001`(또는 중복 에러)

실제 결과: ___

### 2-10. Axis 삭제

- [ ] `DELETE /api/v1/learning-facade/axes/{axisId}` → 204
- [ ] 이후 `GET /api/v1/learning-facade` → 해당 Axis + 하위 Topic 모두 사라짐

실제 결과: ___

---

## 3. 카드 라이프사이클 (product-card.md)

> 상태: ON_FIELD / ARCHIVE. 멱등 전이. OnFieldBudget(maxView + maxDuration)은 UserSchedule에서 파생.

### 3-1. 카드 생성

- [ ] `POST /api/v1/decks/{deckId}/cards` →
  ```json
  {
    "mainNote": {"text": "Spring Security FilterChain의 책임"},
    "summary": "SecurityFilterChain은 인증·인가의 단일 진입점이다.",
    "keywords": [{"value": "SecurityFilterChain"}]
  }
  ```
  → 201, `{ cardId: N, status: "ON_FIELD", displayOrder: 1 }`

실제 결과: ___

### 3-2. 키워드 추가 / 단건 삭제

- [ ] `POST /api/v1/cards/{cardId}/keywords` → `{"value": "AuthenticationManager"}` → 201
  - 응답에 keywordCue 2개, displayOrder 1·2 순서 확인
- [ ] `DELETE /api/v1/cards/{cardId}/keywords/{keywordCueId}` 첫 번째 키워드 삭제 → 200, 남은 1개 반환

실제 결과: ___

### 3-3. 키워드 전체 교체

- [ ] `PUT /api/v1/cards/{cardId}/keywords` → `{"keywords": [{"value": "A"}, {"value": "B"}, {"value": "C"}]}`
  → 200, 응답에 3개 키워드, displayOrder 1·2·3

실제 결과: ___

### 3-4. 태그 부착 (find-or-create)

- [ ] `POST /api/v1/cards/{cardId}/tags` → `{"value": "백엔드"}` → 201
  - 시스템에 "백엔드" 태그가 없으면 생성 후 연결, 있으면 재사용
- [ ] 같은 카드에 `{"value": "스프링"}`도 부착 → 태그 2개

실제 결과: ___

### 3-5. 태그 중복 부착 방지

- [ ] 동일 카드에 동일 태그 value 두 번 부착 → 409, code=`CARD_TAG_ALREADY_EXISTS`

실제 결과: ___

### 3-6. 카드 수동 Archive (ON_FIELD → ARCHIVE)

- [ ] `POST /api/v1/cards/{cardId}/archive` → 200 또는 204
- [ ] `GET /api/v1/cards/{cardId}` → `status: "ARCHIVE"`, `archiveReason: "MANUAL"`

실제 결과: ___

### 3-7. Archive 멱등성

- [ ] 이미 ARCHIVE인 카드에 `POST /api/v1/cards/{cardId}/archive` 재호출 → 에러 없이 성공 (no-op)
- [ ] 상태·이력 변화 없음 확인

실제 결과: ___

### 3-8. Archive → ON_FIELD 복귀 (returnToField)

- [ ] ARCHIVE 카드에 `POST /api/v1/cards/{cardId}/return-to-field` → 200
- [ ] `GET /api/v1/cards/{cardId}` → `status: "ON_FIELD"`

실제 결과: ___

### 3-9. returnToField 멱등성

- [ ] 이미 ON_FIELD인 카드에 `POST /api/v1/cards/{cardId}/return-to-field` → 에러 없이 성공 (no-op)

실제 결과: ___

### 3-10. 복습 후 viewCount 증가 확인

- [ ] ReviewSession을 통해 카드를 한 번 본 뒤 `GET /api/v1/cards/{cardId}` → `viewCount` 1 증가
- [ ] `lastViewedAt` 갱신됨

실제 결과: ___

### 3-11. maxView 도달 시 자동 Archive

- [ ] UserSchedule MODE_10D는 `maxView = 3`. 해당 카드를 ReviewSession으로 3번 조회
- [ ] 3번째 이후 `GET /api/v1/cards/{cardId}` → `status: "ARCHIVE"`, `archiveReason: "MAX_VIEW"`

> **주의**: ReviewSession을 통한 `recordView()` 호출이 필요. 단순 GET 조회는 viewCount를 올리지 않음.

실제 결과: ___

### 3-12. ARCHIVE 상태 카드 recordView 무시

- [ ] ARCHIVE 상태 카드에 ReviewSession에서 노출 시도 → 에러 없이 무시 (viewCount·lastViewedAt 불변)

실제 결과: ___

### 3-13. summary 수정

- [ ] `PATCH /api/v1/cards/{cardId}/summary` → `{"summary": "수정된 요약 1~3문장"}` → 200

실제 결과: ___

### 3-14. 카드 삭제 (Soft Delete)

- [ ] `DELETE /api/v1/cards/{cardId}` → 204
- [ ] `GET /api/v1/cards/{cardId}` → 404 (또는 404 처리)
- [ ] `GET /api/v1/decks/{deckId}/cards` → 해당 카드 목록에서 제거

실제 결과: ___

---

## 4. 복습 세션 (Review BC)

> Layer 1(LearningFacade) 전체에서 카드 수집. state 비율 배분. RECALLING → COMPARING 단계.

### 4-1. ReviewSession 시작

- [ ] `POST /api/v1/review/sessions` (또는 해당 엔드포인트) → 201, sessionId 반환
- [ ] 응답에 오늘 학습할 카드 목록(ON_FIELD 카드 중 soft schedule 조건 통과) 포함
- [ ] 모든 카드가 ARCHIVE면 세션 시작 시 빈 목록 or 안내 메시지

실제 결과: ___

### 4-2. RECALLING 단계 — mainNote만 노출

- [ ] 세션의 첫 카드에서 mainNote(텍스트)만 표시
- [ ] keywords·summary는 화면에 없음 (UI 검증)

실제 결과: ___

### 4-3. COMPARING 단계 — 전체 정보 노출

- [ ] "비교하기" 버튼 등 단계 전환 후 → mainNote + keywords + summary 전부 표시

실제 결과: ___

### 4-4. 세션 완료 후 viewCount 갱신

- [ ] 카드 1장 복습 완료 후 `GET /api/v1/cards/{cardId}` → `viewCount` 1 증가, `lastViewedAt` 갱신

실제 결과: ___

### 4-5. 소프트 스케줄 — 복습 후 interval 상태 확인

- [ ] 복습 완료 후 카드 softSchedule 상태가 `FRESH → INTERVAL_1D` 로 전이 (1일 후 복습 후보)
- [ ] 전이 전 이미 INTERVAL_1D이면 INTERVAL_3D로 진행 확인

실제 결과: ___

### 4-6. 복습 부채 미발생 확인

- [ ] INTERVAL_1D 상태 카드를 3일 뒤에도 안 봐도 → 에러·경고 없음, 여전히 복습 후보 상태

실제 결과: ___

### 4-7. Layer 1 전체 균등 수집 확인

- [ ] Deck A(카드 5장) + Deck B(카드 5장) 상태에서 ReviewSession 시작 → 양쪽 Deck 카드가 골고루 포함 (한쪽 starvation 없음)

실제 결과: ___

---

## 5. 덱 관리 (Deck BC)

### 5-1. 덱 생성 (루트)

- [ ] `POST /api/v1/decks` → `{ name: "백엔드 기본", parentDeckId: null }` → 201, `{ deckId, name, depth: 0 }`

실제 결과: ___

### 5-2. 하위 덱 생성

- [ ] `POST /api/v1/decks` → `{ name: "Spring Security", parentDeckId: {루트덱Id} }` → 201
  - 응답에 `depth: 1`, `parentDeckId` 설정 확인

실제 결과: ___

### 5-3. 덱 이름 변경

- [ ] `PATCH /api/v1/decks/{deckId}/name` → `{ name: "변경된 덱 이름" }` → 200

실제 결과: ___

### 5-4. 덱 이동 (parent 변경)

- [ ] `PATCH /api/v1/decks/{deckId}/parent` → `{ parentDeckId: {다른 덱 Id} }` → 200
- [ ] 순환 참조 시도 (A → B, B → A) → 409, code=`DECK006`

실제 결과: ___

### 5-5. 덱 진행 상태 (progress)

- [ ] ON_FIELD 카드 없는 덱 → `progressStatus: NOT_STARTED`
- [ ] ON_FIELD 카드 1장 추가 → `progressStatus: IN_PROGRESS`
- [ ] 모든 카드 Archive → `progressStatus: COMPLETED`

실제 결과: ___

### 5-6. 덱 삭제

- [ ] `DELETE /api/v1/decks/{deckId}` → 204 (Soft Delete)
- [ ] 이후 `GET /api/v1/decks/{deckId}` → 404

실제 결과: ___

---

## 6. UserSchedule 설정

### 6-1. 학습 모드 조회

- [ ] `GET /api/v1/schedules` (또는 해당 엔드포인트) → 현재 모드(`MODE_10D` / `MODE_20D` / `MODE_30D`) + dailyTarget 반환

실제 결과: ___

### 6-2. 학습 모드 변경

- [ ] `PATCH /api/v1/schedules` → `{ targetDays: 20 }` → MODE_20D 매핑 확인 (15~24일 입력 → MODE_20D)
- [ ] `{ targetDays: 7 }` → MODE_10D (1~14일)
- [ ] `{ targetDays: 30 }` → MODE_30D (25일+)

실제 결과: ___

### 6-3. 모드별 OnFieldBudget 확인

- [ ] MODE_10D 상태에서 ReviewSession 카드 조회 → maxView=3, intervals=[1, 3, 7]
- [ ] MODE_20D → maxView=5, intervals=[1, 3, 7, 14]

실제 결과: ___

### 6-4. dailyTarget 확인

- [ ] 오늘 학습 완료 카드 수가 dailyTarget 도달 → 화면에 목표 달성 안내 (FE UX 확인)

실제 결과: ___

---

## 7. 에러 · 엣지 케이스 (도메인 불변식)

> 모든 에러는 `{ code, message, path, timestamp }` 형식이어야 함 (GlobalExceptionHandler)

### 7-1. 마지막 키워드 삭제 시도

- [ ] 키워드 1개인 카드에서 `DELETE /api/v1/cards/{cardId}/keywords/{keywordCueId}` → 400, code=`CARD033` (또는 `CARD_KEYWORD_LAST_CANNOT_REMOVE`)
- [ ] 화면에 친화적인 에러 메시지 표시

실제 결과: ___

### 7-2. 키워드 blank 입력

- [ ] `POST /api/v1/cards/{cardId}/keywords` → `{"value": "   "}` (공백만) → 400, code=`CARD_KEYWORD_BLANK`

실제 결과: ___

### 7-3. 태그 최대 개수(3개) 초과

- [ ] 태그 3개 이미 부착된 카드에 4번째 태그 부착 → 400, code=`CARD_TAG_LIMIT_EXCEEDED`

실제 결과: ___

### 7-4. LearningFacade 내 Axis 이름 중복

- [ ] 동일 Facade 내에 같은 이름의 Axis 2개 생성 시도 → 409, code=`LEARNING_AXIS_DUPLICATE_NAME`
- [ ] DB UNIQUE(facade_id, name) 제약이 도메인 검증 이전에 도달해도 동일 에러 형식 반환

실제 결과: ___

### 7-5. Topic 다건 추가 부분 실패 시 전체 롤백

- [ ] `POST /api/v1/learning-facade/axes/{axisId}/topics` 여러 건 중 1건이 blank → 전체 실패, 1건도 저장 안 됨
- [ ] 이후 Topic 목록 조회 → 롤백 전 상태 그대로

실제 결과: ___

### 7-6. Axis reorder id 집합 불일치

- [ ] `PUT /api/v1/learning-facade/axes/reorder` → 존재하지 않는 axisId 포함 → 400 또는 422 에러

실제 결과: ___

### 7-7. 타인 카드 접근 시도

- [ ] 사용자 A 로그인 후 사용자 B 소유의 `cardId`로 `GET /api/v1/cards/{cardId}` → 403 또는 404 (소유권 오류)
- [ ] 사용자 B 카드 수정 시도 → 동일하게 거부

실제 결과: ___

### 7-8. 인증 토큰 위변조

- [ ] AT 쿠키를 임의로 변조한 값으로 `GET /user` → 401, code=`AUTH003`

실제 결과: ___

### 7-9. 에러 응답 스키마 통일 확인

- [ ] 400 / 401 / 403 / 404 / 409 각 1건씩 유발 → 모두 `{ code, message, path, timestamp }` 형식
- [ ] Filter 단계 인증 실패(`JWTFilter`)도 동일 형식 반환 (ADR009 — AuthenticationEntryPoint 통일)

실제 결과: ___

### 7-10. 존재하지 않는 리소스 접근

- [ ] 없는 cardId → 404, code=`CARD_NOT_FOUND` (또는 BC별 NOT_FOUND 코드)
- [ ] 없는 deckId → 404, code=`DECK_NOT_FOUND`
- [ ] 없는 axisId → 404, code=`LA001` 또는 유사

실제 결과: ___

### 7-11. 소프트 삭제된 리소스 재접근

- [ ] 삭제된 카드 Id로 `GET /api/v1/cards/{cardId}` → 404 (Soft Delete 필터 동작 확인)
- [ ] 삭제된 덱 Id로 `GET /api/v1/decks/{deckId}` → 404

실제 결과: ___

### 7-12. 권장 한도 초과 안내 (차단 아님)

- [ ] Axis 6번째 추가 성공 확인
- [ ] 응답 DTO에 `isAxisCountExceedsRecommended: true` 포함 확인
- [ ] FE 화면에 "권장 수 초과" 안내 UI 표시 (저장 차단 팝업 X)

실제 결과: ___

### 7-13. displayOrder 자동 부여 확인 (외부 주입 금지)

- [ ] 키워드 3개 추가 후 `GET /api/v1/cards/{cardId}` → displayOrder 1·2·3 자동 부여
- [ ] Request DTO에 `displayOrder` 필드를 강제 포함해 전송 → BE 무시 또는 필드 없음 처리

실제 결과: ___

### 7-14. Concept 동일값 수정 (unchanged 반환)

- [ ] Facade의 현재 concept 값과 동일한 값으로 `PATCH` → 200, 응답에 `isChanged: false`(또는 유사) + `updatedAt` 불변
- [ ] 다른 값으로 변경 → `isChanged: true`, `updatedAt` 갱신

실제 결과: ___

---

## 8. 운영 관측성 (인프라 done 범위)

> 로컬 또는 dev 환경. dev에서만 가능한 항목은 `[dev only]` 표기.

### 8-1. 헬스 엔드포인트

- [ ] `GET /health` → 200, `"OK"` 또는 `{"status": "UP"}`
- [ ] AT 쿠키 없이도 응답 (SecurityFilterChain whitelist 확인)

실제 결과: ___

### 8-2. Prometheus 메트릭 노출

- [ ] `GET /actuator/prometheus` → 200, text/plain 형식 메트릭 라인 다수
- [ ] `jvm_memory_used_bytes` / `http_server_requests_seconds_count` 등 키 존재 확인

실제 결과: ___

### 8-3. X-Request-Id echo back

- [ ] 임의 요청에 `X-Request-Id: test-001` 헤더 포함해 전송
- [ ] 응답 헤더에 `X-Request-Id: test-001` 그대로 돌아옴 (MdcLoggingFilter)
- [ ] `X-Request-Id` 없이 요청 → BE가 UUID 자동 생성, 응답 헤더에 포함

실제 결과: ___

### 8-4. JSON 구조화 로그 확인 (dev 환경)

- [ ] `./gradlew bootRun` 실행 후 BE 콘솔에서 로그가 JSON 라인(`{"@timestamp":..., "level":..., "message":...}`) 형식
- [ ] 5xx 발생 시 로그에 `X-Request-Id` 값 포함 (MDC 확인)

실제 결과: ___

### 8-5. [dev only] Grafana 대시보드

- [ ] Grafana URL 접근 → 대시보드 로드
- [ ] 4개 섹션 존재 확인 (상태·API·JVM·DB)
- [ ] API 몇 건 호출 후 `http_server_requests_seconds_count` 그래프에 수치 반영

실제 결과: ___

### 8-6. [dev only] Swagger UI on dev

- [ ] `https://{dev_alb_domain}/swagger-ui.html` → 접근 가능 (dev profile, prod에서는 비활성)
- [ ] 주요 BC Controller 목록 모두 표시 (User, Card, Deck, LearningFacade, Review, UserSchedule, Health)

실제 결과: ___

---

## 종합 통과 기준

> 아래 6개 묶음이 모두 통과해야 v0.0.1v UX 테스트 완료로 간주.

| 묶음 | 조건 | 상태 |
|------|------|------|
| **인증 핵심** | 1-1(회원가입·로그인) + 1-4(정보수정) + 1-6(AT 갱신) | [ ] |
| **LearningFacade 핵심** | 2-1(생성) + 2-3(Axis 추가) + 2-6(Topic 추가) + 2-7(Material 연결) + 2-8(coverage 즉시반영) | [ ] |
| **Card 핵심** | 3-1(생성) + 3-3(키워드교체) + 3-6(Archive) + 3-8(복귀) + 3-7·3-9(멱등성) | [ ] |
| **에러 형식 통일** | 7-9 + 7-1 + 7-3 (에러 스키마 일관성) | [ ] |
| **도메인 불변식** | 7-4(Axis 이름 중복) + 7-5(다건 롤백) + 7-13(displayOrder 자동부여) | [ ] |
| **관측성** | 8-1(health) + 8-2(prometheus) + 8-3(X-Request-Id) | [ ] |

---

## 미통과 항목 기록란

| 항목 번호 | 실제 결과 | 예상 원인 | 조치 필요 여부 |
|----------|---------|---------|--------------|
| | | | |
| | | | |

> 통과·미통과 결과는 [`review.md`](./review.md) "시나리오 검증 결과" 섹션에 요약 기록.

---

*작성일: 2026-06-28 | 대상 버전: 0.0.1v | 참고: product-auth.md / product-card.md / product-learningFacade.md (done)*
