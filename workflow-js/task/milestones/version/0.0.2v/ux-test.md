# 0.0.2v UX 테스트 — 로컬 직접 사용 점검표

> **목적**: 0.0.2v에서 신설/변경되는 도메인(concepts[] · Layer)과 유지되는 기존 도메인이 **로컬 환경(H2 + `./gradlew bootRun`)에서 실제 사용자 흐름으로** 통과하는지 점검한다.
>
> **진실 소스**: 신규 SDD `product-learning-tower.md` (Epic 1·2) + 기존 done SDD `product-auth.md` / `product-card.md`. 도메인 불변식·에러 코드는 해당 SDD 기준.
>
> **환경**: 로컬만 (`http://localhost:8080`). 배포는 본 버전 스코프 밖. FE 검증은 로컬 `npm run dev` (선택).
>
> **방법**: 각 항목을 curl / Swagger UI / 브라우저에서 직접 수행 후 `- [ ]` → `- [x]` 체크. 실패 시 실제 결과와 오류 코드를 인라인 기록.
>
> **회귀 원칙**: 신규 기능만이 아니라 **기존 인증·카드 흐름이 깨지지 않았는지** 회귀 검증도 필수.

---

## 테스트 범위 & 우선순위

| 섹션 | 대상 | 우선순위 | 항목 수 |
|------|------|---------|---------|
| 1. 사전 준비 (부팅·마이그레이션·시드) | H2 부팅 + Flyway | P0 | 5 |
| 2. LearningFacade `concepts[]` (Epic 1) | product-learning-tower Epic 1 | **P0** | 10 |
| 3. Layer 도메인 (Epic 2) | product-learning-tower Epic 2 | **P0** | 12 |
| 4. AI Suggestion Port 골격 (AS Epic 1) | product-ai-suggestion Epic 1 | P1 | 3 |
| 5. 회귀 — 인증 / 카드 / Deck (기존) | 이전 done Products | P0 | 8 |
| 6. 에러·엣지 케이스 (신규 ErrorCode) | 신규 도메인 불변식 | P0 | 10 |
| 7. 문서 정합 (ADR022 · DOMAIN.md) | 문서 산출 | P1 | 4 |

---

## 사전 준비

- [ ] BE 부팅 통과: `./gradlew clean bootRun` → `Started ThirdToolApplication in X.XXX seconds`
- [ ] Flyway 마이그레이션 성공: 로그에 "Successfully applied N migrations" (신규 V버전 포함)
- [ ] 시드 사용자 로그인 성공: `POST /login` → 200 + AT 쿠키
- [ ] Swagger UI 진입: `http://localhost:8080/swagger-ui.html` → 신규 엔드포인트 (`/facades/me/concepts`, `/facades/me/layers/*`) 노출 확인
- [ ] H2 콘솔 접근(선택): `http://localhost:8080/h2-console` → `learning_facade_concept` · `learning_layer` 테이블 존재 확인

실제 결과: ___

---

## 1. LearningFacade concepts[] (Epic 1)

> 단일 문자열 `concept` → 다중 배열 `concepts[]` (1~5) 승격 검증.

### 1-1. concepts 조회 (기존 사용자 백필 확인)

- [ ] `GET /api/v1/facades/me` → 200 + `concepts: string[]` 응답
- [ ] 기존 단일 `concept` 값이 `concepts[0]` 로 백필됨
- [ ] `concepts.length >= 1` (최소 1개 보장)

실제 결과: ___

### 1-2. concepts 신규 저장 (chip 입력 시나리오)

- [ ] `POST /api/v1/facades/me/concepts` `{"concepts":["백엔드","시스템설계"]}` → 201
- [ ] 응답에 `concepts: ["백엔드","시스템설계"]` 순서·개수 일치
- [ ] `GET` 재조회 → 동일 결과

실제 결과: ___

### 1-3. concepts 부분 갱신 (`updateConcepts` 다건 행위)

- [ ] `PATCH /api/v1/facades/me/concepts` `{"concepts":["A","B","C"]}` → 200
- [ ] 응답 결과 VO `{"changed": true}` 확인 (`ConceptsChangeRecord`)
- [ ] 동일 값 재저장 → `{"changed": false}` (멱등)
- [ ] `updatedAt` 이 동일값 저장 시 갱신 안 됨

실제 결과: ___

### 1-4. 중복 concept 거부

- [ ] `POST /api/v1/facades/me/concepts` `{"concepts":["백엔드","백엔드"]}` → 409 `LEARNING_FACADE_CONCEPT_DUPLICATE`
- [ ] `{"concepts":["  백엔드  ","백엔드"]}` (trim 후 동일) → 409 (정규화 후 비교)

실제 결과: ___

### 1-5. blank concept 거부

- [ ] `{"concepts":[""]}` → 400 `LEARNING_FACADE_CONCEPT_BLANK`
- [ ] `{"concepts":["   "]}` (whitespace only) → 400 (trim 후 blank)

실제 결과: ___

### 1-6. 크기 상한 (5개) 초과

- [ ] `{"concepts":["A","B","C","D","E","F"]}` → 400 `LEARNING_FACADE_CONCEPTS_SIZE_INVALID`
- [ ] 5개 정확히 → 201 성공
- [ ] 빈 배열 `{"concepts":[]}` → 400 `LEARNING_FACADE_CONCEPTS_SIZE_INVALID`

실제 결과: ___

### 1-7. 각 concept 길이 제한 (100자)

- [ ] `concepts` 항목 1개가 101자 → 400 `LEARNING_FACADE_CONCEPT_TOO_LONG`
- [ ] 100자 정확히 → 201 성공

실제 결과: ___

### 1-8. displayOrder 자동 부여

- [ ] `{"concepts":["A","B","C"]}` 저장 후 DB 조회 → 각각 `display_order` = 1, 2, 3
- [ ] Request 에 `displayOrder` 필드 강제 포함 → 서버가 무시 (외부 주입 금지)

실제 결과: ___

### 1-9. 백필 검증 (Story 1-3)

- [ ] 마이그레이션 이전 상태에서 단일 `concept="기존값"` 저장한 facade 1건 준비
- [ ] `./gradlew clean bootRun` (신규 V 버전 적용)
- [ ] `GET /facades/me` → `concepts: ["기존값"]` 배열 응답 (1건 자동 이관)
- [ ] `learning_facade.concept` 컬럼은 soft-deprecate (유지)

실제 결과: ___

### 1-10. Swagger UI 스키마 갱신

- [ ] Swagger UI `LearningFacadeResponse` 스키마에서 `concepts: string[]` 필드 확인
- [ ] 이전 `concept: string` 은 (fallback 기간 표기 or) 제거

실제 결과: ___

---

## 2. Layer 도메인 (Epic 2)

> LearningFacade → Layer → Axis 계층 확인. default "Uncategorized" Layer 백필.

### 2-1. default Uncategorized Layer 자동 존재 확인

- [ ] `GET /api/v1/facades/me` → `layers[0].name === "Uncategorized"` (default 1건)
- [ ] 기존 axis 전체가 default Layer 하위로 이관됨

실제 결과: ___

### 2-2. 신규 사용자 회원가입 시 default Layer 자동 생성

- [ ] 신규 사용자 회원가입 → facade 자동 생성 → default Layer 1건 자동 생성
- [ ] `GET /facades/me` → `layers.length === 1`, name "Uncategorized"

실제 결과: ___

### 2-3. Layer 생성

- [ ] `POST /api/v1/facades/me/layers` `{"name":"백엔드"}` → 201
- [ ] 응답에 `displayOrder: 2` (default + 1)
- [ ] `GET /facades/me` → `layers.length === 2`

실제 결과: ___

### 2-4. Layer 이름 중복 방지

- [ ] `{"name":"Uncategorized"}` 로 재생성 → 409 `LAYER_NAME_DUPLICATE`
- [ ] `{"name":"백엔드"}` 로 재생성 → 409

실제 결과: ___

### 2-5. Layer 이름 blank 거부

- [ ] `{"name":""}` → 400 `LAYER_NAME_BLANK`
- [ ] `{"name":"   "}` → 400 (trim 후 blank)

실제 결과: ___

### 2-6. Axis 를 특정 Layer 하위에 생성

- [ ] `POST /api/v1/layers/{layerId}/axes` `{"name":"Spring Framework"}` → 201
- [ ] 응답에 `layerId` 포함
- [ ] `GET /facades/me` → `layers[layerIdx].axes[0].name === "Spring Framework"`

실제 결과: ___

### 2-7. Layer 소속 axis 조회

- [ ] `GET /api/v1/layers/{layerId}` → Layer 상세 + 하위 axis 목록
- [ ] softDeleted axis 는 제외

실제 결과: ___

### 2-8. Layer 이름 수정

- [ ] `PATCH /api/v1/layers/{layerId}` `{"name":"백엔드 심화"}` → 200
- [ ] `GET /layers/{layerId}` → 갱신 확인
- [ ] 동일값 재수정 → 200 (멱등, `updatedAt` 불변)

실제 결과: ___

### 2-9. Layer 재정렬 (`PUT reorder`)

- [ ] Layer 3건 상태에서 `PUT /api/v1/facades/me/layers/reorder` `{"orderedIds":[3,1,2]}` → 200
- [ ] `GET /facades/me` → displayOrder 재부여 (1=id3, 2=id1, 3=id2)
- [ ] id 집합 불일치 (`orderedIds:[99]` — 존재하지 않는 id) → 400 `LAYER_REORDER_ID_MISMATCH`

실제 결과: ___

### 2-10. Layer softDelete (하위 axis 존재 → 거부)

- [ ] Layer 에 axis 1건 이상 상태에서 `DELETE /api/v1/layers/{layerId}` → 409 `LAYER_HAS_ACTIVE_AXES`
- [ ] axis 삭제 후 재시도 → 204 (softDelete 성공)
- [ ] `GET /facades/me` → 삭제된 Layer 미포함

실제 결과: ___

### 2-11. softDeleted Layer 이름 재사용

- [ ] softDeleted Layer "백엔드" 상태에서 신규 `POST layers {"name":"백엔드"}` → 201
- [ ] UNIQUE `(facade_id, name, deleted_at)` composite 로 재사용 허용 확인

실제 결과: ___

### 2-12. Layer 개수 권장 상한 초과 안내

- [ ] Layer 6번째 생성 → 201 성공 (차단 아님)
- [ ] 응답 DTO 에 `isLayerCountExceedsRecommended: true` 포함
- [ ] 5개 이하일 때는 `false`

실제 결과: ___

---

## 3. AI Suggestion Port 골격 (AS Epic 1)

> Port 인터페이스 컴파일 + stub Adapter 실험.

### 3-1. 4 Port 파일 존재 확인

- [ ] `ai/port/out/suggestion/LayerSuggestionPort.java` 존재
- [ ] `ai/port/out/suggestion/AxisSuggestionPort.java` 존재
- [ ] `ai/port/out/suggestion/RoadmapSuggestionPort.java` 존재
- [ ] `ai/port/out/suggestion/SelectionsSuggestionPort.java` 존재
- [ ] 각 12 개 record (요청/응답/컨텍스트) 존재

실제 결과: ___

### 3-2. stub Adapter 컴파일 실험

- [ ] `test/` 또는 임시 `sandbox/` 아래에 각 Port implements 하는 stub 클래스 1개씩 작성
- [ ] `./gradlew compileJava` 통과
- [ ] 시그니처 mismatch 없음

실제 결과: ___

### 3-3. Adapter 미구현 상태의 Application 흐름 검증

- [ ] Suggestion Application Service 미구현 상태에서 부팅 성공 (Port 만 Bean 등록 안 되어도 오류 없음)
- [ ] Suggestion Controller 미노출 (다음 버전 이후)

실제 결과: ___

---

## 4. 회귀 — 인증 / 카드 / Deck (기존)

> 신규 도메인 변경이 기존 흐름을 깨지 않는지 검증.

### 4-1. 로그인 흐름

- [ ] `POST /login` → 200 + AT 쿠키 + RT 바디
- [ ] `GET /user` → 200

실제 결과: ___

### 4-2. 기존 axis 조회

- [ ] `GET /facades/me/axes/{axisId}/cards?status=ON_FIELD` → 200 (기존 fix `axis-deck-full-integration` 회귀)

실제 결과: ___

### 4-3. 카드 생성 · 조회 · 아카이브

- [ ] `POST /decks/{deckId}/cards` → 201
- [ ] `GET /cards/{cardId}` → 200
- [ ] `POST /cards/{cardId}/archive` → 200

실제 결과: ___

### 4-4. Deck 자동 생성 (기존 ADR021)

- [ ] `POST /facades/me/axes {name:"algorithm"}` → 201 + `LearningAxisCreatedEvent` 발행 → Deck 자동 생성
- [ ] `GET /decks` → 응답에 `axisId`, `axisName` 포함

실제 결과: ___

### 4-5. axis softDelete → Deck 연쇄 (기존 ADR021)

- [ ] `DELETE /axes/{axisId}` → 204
- [ ] `GET /decks` → 해당 axis 의 Deck 미포함 (soft delete 연쇄)

실제 결과: ___

### 4-6. `layer_id` 재배선 후 기존 카드 조회 정상

- [ ] Story 2-2 (Axis FK 재배선) 후 `GET /facades/me/axes/{axisId}/cards` 여전히 정상 응답
- [ ] `layer_id` 백필된 axis 정상 처리

실제 결과: ___

### 4-7. Flyway 롤백 스크립트 검증

- [ ] `R{N}__rollback_learning_facade_concept.sql` 실행 시 오류 없음 (수동)
- [ ] `R{N+2}__rollback_learning_layer.sql` 실행 시 오류 없음
- [ ] `R{N+3}__rollback_axis_layer_fk.sql` 실행 시 axis 데이터 소실 없음

실제 결과: ___

### 4-8. 회귀 테스트 스위트 통과

- [ ] `./gradlew test` → BUILD SUCCESSFUL
- [ ] 기존 단위·슬라이스·통합 테스트 전부 통과
- [ ] 실패 테스트 0건 (있다면 원인 · 조치 기록)

실제 결과: ___

---

## 5. 에러 · 엣지 케이스 (신규 ErrorCode)

> 신규 도메인 불변식 · GlobalExceptionHandler 응답 형식 정합.

### 5-1. `LEARNING_FACADE_CONCEPTS_SIZE_INVALID`

- [ ] 빈 배열 요청 → 400 + `{code:"LEARNING_FACADE_CONCEPTS_SIZE_INVALID", message:"...", path:"...", timestamp:"..."}`
- [ ] 6개 배열 → 동일 응답 형식

실제 결과: ___

### 5-2. `LEARNING_FACADE_CONCEPT_DUPLICATE`

- [ ] 중복 값 → 409 + 응답 스키마 정합

실제 결과: ___

### 5-3. `LEARNING_FACADE_CONCEPT_BLANK`

- [ ] 빈 문자열 · whitespace only → 400 + 응답 스키마 정합

실제 결과: ___

### 5-4. `LEARNING_FACADE_CONCEPT_TOO_LONG`

- [ ] 101자 문자열 → 400 + 응답 스키마 정합

실제 결과: ___

### 5-5. `LAYER_NAME_BLANK`

- [ ] Layer 생성 시 name blank → 400 + 응답 스키마 정합

실제 결과: ___

### 5-6. `LAYER_NAME_DUPLICATE`

- [ ] Layer 이름 중복 → 409 + 응답 스키마 정합

실제 결과: ___

### 5-7. `LAYER_HAS_ACTIVE_AXES`

- [ ] 하위 axis 있는 Layer 삭제 시도 → 409 + 응답 스키마 정합

실제 결과: ___

### 5-8. `LAYER_NOT_FOUND`

- [ ] 존재하지 않는 `layerId` 로 조회 → 404

실제 결과: ___

### 5-9. `LAYER_REORDER_ID_MISMATCH`

- [ ] `orderedIds` 에 존재하지 않는 id 포함 → 400

실제 결과: ___

### 5-10. GlobalExceptionHandler 응답 스키마 통일

- [ ] 위 9종 ErrorCode 응답이 모두 `{code, message, path, timestamp}` 4필드 형식
- [ ] 신규 code 가 `ErrorCode` enum 에 정확히 등록 (grep 검증)

실제 결과: ___

---

## 6. 문서 정합 (ADR022 · DOMAIN.md · PACKAGE.md)

### 6-1. ADR022 확인

- [ ] `docs/adr/ADR022-learning-tower-terminology.md` 존재
- [ ] `docs/adr/index.md` 에 ADR022 행 등록

실제 결과: ___

### 6-2. `docs/DOMAIN.md` §LearningFacade concepts[] 반영

- [ ] `concept: String` 단수 표기 삭제 (또는 legacy 표기)
- [ ] `concepts: List<String>` (1~5) 표기 명시
- [ ] `LearningFacadeConcept` Entity 설명

실제 결과: ___

### 6-3. `docs/DOMAIN.md` §Layer 절 신설

- [ ] Layer Aggregate 설명
- [ ] LearningFacade → Layer → Axis 계층 다이어그램
- [ ] default "Uncategorized" 정책 명시

실제 결과: ___

### 6-4. `docs/PACKAGE.md` 갱신

- [ ] Layer 파일 위치 결정 명시 (`LearningFacade/domain/model/Layer.java`)
- [ ] BC 의존 방향 재확인 (LearningFacade 안 계층)

실제 결과: ___

---

## 종합 통과 기준

> 아래 6 묶음이 모두 통과해야 0.0.2v UX 테스트 완료로 간주.

| 묶음 | 조건 | 상태 |
|------|------|------|
| **concepts[] 핵심** | 1-2(저장) + 1-4(중복) + 1-6(크기) + 1-9(백필) | [ ] |
| **Layer 핵심** | 2-1(default 존재) + 2-3(생성) + 2-6(axis 소속) + 2-10(softDelete) | [ ] |
| **Port 골격** | 3-1(4 Port 존재) + 3-2(stub 컴파일) | [ ] |
| **회귀 안정성** | 4-1(로그인) + 4-2(axis 조회) + 4-3(카드 생성) + 4-7(롤백) + 4-8(테스트 통과) | [ ] |
| **에러 형식 통일** | 5-1 · 5-6 · 5-10 (신규 ErrorCode 응답 통일) | [ ] |
| **문서 정합** | 6-1(ADR022) + 6-2·6-3(DOMAIN.md) | [ ] |

---

## 미통과 항목 기록란

| 항목 번호 | 실제 결과 | 예상 원인 | 조치 필요 여부 |
|----------|---------|---------|--------------|
| | | | |
| | | | |

> 통과·미통과 결과는 [`review.md`](./review.md) "시나리오 검증 결과" 섹션에 요약 기록.
> 상세 원인 분석은 [`troubleshooting.md`](troubleshooting/troubleshooting.md) 로.

---

## FE 로컬 검증 (선택 · P2)

> FE 재편은 별도 milestone. 본 M2 에서 FE 로컬 검증은 선택.

- [ ] FE `npm run dev` 실행 → 로그인 화면 로드
- [ ] `<ConceptsInput>` chip UI 로 컨셉 3개 입력 → 저장 성공
- [ ] `<LayersListPage>` → default "Uncategorized" + 신규 Layer 렌더
- [ ] FE 에서 axis 생성 → BE 반영 확인

FE 이슈 발견 시: 별도 FE milestone 트래킹.

---

*작성일: 2026-07-01 | 대상 버전: 0.0.2v | 참고: product-learning-tower.md Epic 1·2 / product-ai-suggestion.md Epic 1 / ADR022*
