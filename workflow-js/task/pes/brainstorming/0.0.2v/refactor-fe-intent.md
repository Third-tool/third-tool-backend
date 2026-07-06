# Brainstorming · BE 리팩토링 / FE 의도 표현 카탈로그 (0.0.2v)

> **목적**: FE brainstorming `FE:/workflows/task/fix/brainstorming/version/0.0.1v/002.md`(Card↔Deck↔Axis 흐름 + 모드/maxDuration)에서 드러난 사용자 의도를, **BE가 계약·구조상 표현하지 못하는 지점**을 정리한다. 두 갈래로 나눈다.
>
> - **Part A — FE에 의도가 드러나게 하려면 BE가 열어줘야 하는 것** (계약/엔드포인트 공백). FE가 "축에 카드를 넣는다 / 축의 카드를 본다 / 전체를 복습한다 / 캐싱 기간을 정한다"를 표현하려면 BE 응답·경로가 그 개념을 노출해야 한다.
> - **Part B — 내부 리팩토링** (FE와 무관하게 코드 구조상 정리가 필요한 것). 이번 M1 추적 중 반복적으로 FE 버그를 유발한 구조적 원인들.
>
> **판단 기준**: ① FE 의도 표현에 직접 기여하는가 ② 기존 BC 경계를 흔드는가 ③ 선행 의존이 없는가 ④ 재발 방지(드리프트 차단) 효과가 있는가.
>
> **트래킹 기준**: FE 002.md (2026-06-30) + 2026-06-29~30 카드 생성 흐름 추적.

---

## 추천 요약 (한눈에)

| # | 갈래 | 후보 | Tier | 권장 | 핵심 이유 |
|---|------|------|------|------|-----------|
| A1 | 계약 | Deck 응답에 `axisId`/`axisName` 노출 + Deck↔Axis 명시화 | 1 | ✅ | FE가 "이 덱이 어느 축"인지 알 길이 없음 |
| A2 | 계약 | axis-연결 Deck 생성 경로 (deck create가 axisId 수용) | 1 | ✅ | 일반 덱 생성이 고아 덱만 만듦 → 카드가 사라짐 |
| A3 | 계약 | axis별 Card 조회 엔드포인트 | 1 | ✅ | 카드 조회가 덱별/태그별만 — 축 단위 부재 |
| A4 | 계약 | 전체-axis Review 세션 시작 경로 | 2 | 🔶 | today는 전체 집계인데 StartReview는 덱 단위 |
| B1 | 리팩 | UserScheduleController 인증 일관화 | 1 | ✅ 즉시 | 유일하게 `X-User-Id` 헤더 인증 → 500, 모드 설정 차단 |
| B2 | 리팩 | ID 직렬화 표준화 (Long→String 전역) | 1 | ✅ | FE가 `z.coerce.string()`로 우회 중, 드리프트 상시 |
| B3 | 리팩 | 응답 DTO 계약 드리프트 차단(스냅샷/OpenAPI) | 2 | 🔶 | enum·필드 불일치 반복(이번 M1만 4건) |
| B4 | 리팩 | Card 축-스코프 조회 로직 단일화 | 2 | 🔶 | today·축뷰가 같은 집계를 따로 구현하게 됨 |
| B5 | 리팩 | CORS 설정 단일 소스화 | 3 | ⏸ | MvcConfig + SecurityConfig 이중 정의 |

---

# Part A — FE에 의도가 드러나게 하려면 BE가 열어줘야 하는 것

---

## [후보 A1] Deck 응답에 `axisId`/`axisName` 노출 + Deck↔Axis 관계 명시화

> "이 카드가 어느 축에 쌓이는가"를 FE가 화면에 드러내려면, 덱이 어느 축인지부터 응답에 있어야 한다.

### 배경
- `Card`는 `Deck`에만 속하고, `Deck`은 `axis_id`(raw `Long` 컬럼)로만 `Axis`에 간접 연결된다. 도메인상 Deck↔Axis는 FK 관계가 아니라 `Long axisId` 필드 하나뿐.
- `DeckResponse.Summary`/`Detail` 어디에도 `axisId`가 없음 → FE는 덱 목록을 받아도 "축별 그룹핑"을 할 수 없다 (002.md Issue 4 (b)).
- 결과: 카드 생성·축 뷰 어느 쪽도 "축↔덱" 관계를 화면에 표현 불가.

### 후보
- **A안**: `Summary`/`Detail`에 `axisId`(+조인으로 `axisName`) 필드만 추가. 장점: 최소 변경, 즉시 FE 그룹핑 가능. 비용: axisName 조인 1회.
- **B안**: `Deck.axisId`(raw Long)를 `LearningAxis` 연관(또는 read-model VO)으로 승격. 장점: 도메인 관계 명시·정합. 비용: 매핑·마이그레이션 부담, BC 경계(LearningFacade↔Deck) 결합도 상승.
- **C안**: facade 조회 응답(`FacadeDetail.axes[]`)에 각 axis의 deck 요약을 동봉. 장점: 축→덱 트리를 한 번에. 비용: facade 응답 비대화.

### 1차 권장
**A안 (필드만 추가)** 우선. 도메인 관계 승격(B)은 별 Story로 분리. FE는 `deck.ts` 스키마에 `axisId` 추가만으로 수용 가능.
*(확정 아님 — Deck↔Axis를 read-model로 둘지 PES에서 재검토)*

### PES 승격 경로
- `product-card.md` 또는 신규 `product-deck.md`의 Epic — "Deck-Axis 가시화" Story
- ADR 후보: "Deck↔Axis 관계 표현 — raw Long 유지 vs 연관 승격"

### 미해결 질문
- axisName을 매 덱 응답에 조인할지, FE가 facade의 axis 목록과 클라이언트 조인할지?

---

## [후보 A2] axis-연결 Deck 생성 경로 (deck create가 `axisId`를 수용)

> "이 축에 카드를 넣겠다"는 의도를 표현하려면, 그 축에 묶인 덱을 만들 수 있어야 한다.

### 배경
- 현재 axis에 연결된 덱은 **오직 자료(material) 생성 이벤트**(`Deck.createFromLearningMaterial(user, axisId, materialId, name)` / `LearningMaterialCreatedEventHandler`)로만 생성된다.
- 일반 `POST /api/v1/decks`(`DeckRequest.Create { name, parentDeckId }`)는 `axisId`를 받지 않아 **항상 고아 덱(`axisId=null`)** 을 만든다.
- 그래서 카드 에디터가 새 덱을 만들면 무조건 축 밖으로 떨어지고, today(`d.axisId IN :axisIds`)·축 뷰에서 사라진다. (이번 "카드 안 뜸"의 직접 뿌리)

### 후보
- **A안**: `POST /api/v1/decks` 요청에 `axisId`(옵션) 추가 — 지정 시 axis-연결, 미지정 시 기존 고아. 장점: 경로 1개 확장. 비용: 덱 생성 시 axis 소유권 검증 필요.
- **B안**: facade 하위에 `POST /api/v1/learning-facade/axes/{axisId}/decks` 신설. 장점: 소유·스코프가 경로에 드러남, 검증 자연스러움. 비용: 엔드포인트 추가.
- **C안**: 덱 생성을 막고, 카드 생성을 "자료 생성(material) → 자동 덱" 흐름으로만 유도. 장점: 기존 이벤트 흐름 재사용. 비용: 카드 ≠ 자료라 의미 왜곡, UX 우회.

### 1차 권장
**B안 (axis 스코프 경로 신설)**. 소유권·스코프가 path에 드러나 검증이 단순하고, "축에 덱을 단다"는 의도가 API에 명시됨.
*(A안도 가능 — PES에서 검증 비용 비교)*

### PES 승격 경로
- 신규 `product-deck.md` 또는 `product-card.md` Epic — "축 스코프 덱 생성"
- 의존: 후보 A1(덱 응답 axisId 노출)과 한 Story 묶음 권장
- ADR 후보: "덱 생성 시 axis 결합 위치 — 덱 BC vs facade BC"

### 미해결 질문
- 한 축에 여러 덱 허용인가, 축당 기본 덱 1개 정책인가? (카드 분류 UX 결정)

---

## [후보 A3] axis별 Card 조회 엔드포인트

> "내 축을 눌렀더니 그 축의 카드가 보인다"를 표현하려면 축 단위 조회가 필요하다.

### 배경
- 카드 조회는 **덱별**(`GET /api/v1/decks/{deckId}/cards`)·**태그별**(`/tags/{tagId}/cards`)만 존재. 축 단위 조회 경로 없음 (002.md Issue 5).
- 축의 카드 = `axisId`인 덱들 → 각 덱의 카드 합집합. FE가 직접 모으면 N+1.
- 내부엔 `DeckQueryService.findByAxisIds(...)`가 있으나 미노출.

### 후보
- **A안**: `GET /api/v1/learning-facade/axes/{axisId}/cards?status=ON_FIELD|ARCHIVE`. 장점: 단일 호출, 축 의도 명시. 비용: 조인 쿼리(axis→deck→card) 1개 추가.
- **B안**: 후보 A1로 덱에 axisId가 노출되면 FE가 축의 덱들을 모아 덱별 조회 N회. 장점: 신규 엔드포인트 0. 비용: N+1, FE 복잡도.
- **C안**: facade 조회 응답에 축별 카드 요약(개수/미리보기) 동봉. 장점: 지도 초기 로드 1회. 비용: facade 응답 비대화, 상세는 결국 별도 호출.

### 1차 권장
**A안 (단일 엔드포인트)**. axis 뷰의 의도가 경로에 드러나고 N+1 회피. today 집계 로직과 쿼리를 공유하면 후보 B4와 자연 결합.

### PES 승격 경로
- `product-card.md` Epic — "축 스코프 카드 조회"
- 의존: 후보 B4(축-스코프 조회 단일화)와 통합 시 시너지

### 미해결 질문
- topic 단위까지 내려가나(축→주제→카드), 축 직속 카드 평면 목록인가?

---

## [후보 A4] 전체-axis Review 세션 시작 경로

> "학습 시작하기 = 전체 축을 복습"이라는 의도가 현재 API에 없다.

### 배경
- `today`(`/review-session/today`)는 전체 axis 후보를 집계(Epic 6)하지만, 실제 세션 시작 `StartReview`는 `deckId` 단위(`StartReviewRequest { deckId }`)다. → "전체 복습"과 "한 덱 복습"이 어긋남 (002.md Issue 6 (a)).

### 후보
- **A안**: today 후보를 그대로 세션화하는 `POST /api/v1/review-session/start`(deckId 없이 전체). 장점: today와 동일 스코프, 의도 일치. 비용: 멀티덱 세션 모델 점검 필요.
- **B안**: `StartReview`가 `deckId` 대신 `axisId[]`(또는 생략=전체)를 받도록 확장. 장점: 범위 선택 유연. 비용: 세션 도메인이 멀티덱 카드 집합을 다뤄야 함.
- **C안**: 현행 덱 단위 유지 + FE가 덱을 순회. 비용: 전체 복습 의도 미표현, UX 끊김.

### 1차 권장
**확인 후 A안**. 먼저 Review 세션 도메인이 멀티덱 카드 집합을 수용하는지 점검(미해결 질문) → 가능하면 today 후보 그대로 세션 시작.

### PES 승격 경로
- `product-card.md` Review Epic 보강 — "전체/축 스코프 세션 시작"
- ADR 후보: "Review 세션 스코프 단위 — Deck vs Axis vs 전체"

### 미해결 질문
- 현재 `ReviewSession` 도메인이 단일 deck 가정인가? 멀티덱 카드 묶음을 한 세션에 담을 수 있나? *(코드 확인 선행 필요)*

---

# Part B — 내부 리팩토링 (코드 구조)

---

## [후보 B1] UserScheduleController 인증 일관화 — `@RequestHeader("X-User-Id")` → `@AuthenticationPrincipal`

> 시스템 전체에서 **유일하게** 헤더로 인증하는 컨트롤러. 버그이자 일관성 부채.

### 배경
- `UserScheduleController`의 4개 메서드(`getSchedule`/`saveSchedule`/`getHistory`/`updateDailyTarget`)가 `@RequestHeader("X-User-Id") Long userId`로 사용자를 식별한다.
- 다른 모든 컨트롤러는 `@AuthenticationPrincipal UserEntity user`. FE는 `X-User-Id`를 안 보냄(보내는 건 `X-Request-Id`) → `/api/v1/users/me/schedule` 전부 실패(콘솔 `500` 반복).
- 결과: Epic 4(maxDuration/모드)가 도메인엔 구현됐는데 **호출 자체가 막혀** 사용 불가 (002.md Issue 6 (c)).

### 후보
- **A안**: 4개 메서드 시그니처를 `@AuthenticationPrincipal UserEntity user` + `user.getId()`로 교체. 장점: 즉시 정합, 위험 낮음. 비용: slice 테스트 갱신.
- **B안**: 그대로 두고 FE가 `X-User-Id` 헤더를 주입. 거부 — 보안상 클라이언트가 userId를 직접 주장하게 됨(스푸핑). 절대 불가.

### 1차 권장
**A안 — 즉시 적용.** 이건 brainstorming보다 `hot-fix`/`resolved` 후보에 가깝다. 인증 표준 위반 + 기능 차단이 동시.

### PES 승격 경로
- `hot-fix/` 또는 User/UserSchedule BC 정리 Story. ADR 불필요(표준 복귀).

### 미해결 질문
- 왜 이 컨트롤러만 헤더 인증인가? (초기 BC 분리 시 cross-BC 호출 가정의 잔재로 추정 — 동일 패턴이 다른 곳에 더 있는지 grep 필요)

---

## [후보 B2] ID 직렬화 표준화 — Long ID의 응답 표현 통일

> FE가 모든 ID를 `z.coerce.string()`으로 받아내는 중. 계약이 암묵적이라 드리프트가 상시 발생.

### 배경
- 엔티티 ID가 `Long` → Jackson 기본 직렬화로 JSON **숫자**. 전역 정책(`WRITE_NUMBERS_AS_STRINGS`/`ToStringSerializer`) 없음.
- FE는 일부 스키마만 `z.coerce.string()`이고 일부는 `z.string()`이라, 이번 M1에서 facade/axis/topic/material ID 파싱 실패가 연쇄 발생(직전 fix들).
- "ID는 문자열"이라는 계약이 코드 어디에도 단일 선언돼 있지 않음.

### 후보
- **A안**: 전역 ObjectMapper에 `SerializationFeature.WRITE_NUMBERS_AS_STRINGS`. 장점: 한 줄, 모든 ID 일괄. 비용: 모든 숫자 필드가 문자열화(개수/금액까지) → 광범위 영향, FE 숫자 필드 재검토 필요.
- **B안**: ID 필드에만 `@JsonSerialize(using = ToStringSerializer.class)` 또는 응답 DTO에서 ID를 `String`으로. 장점: 정밀(ID만). 비용: DTO마다 적용 누락 위험.
- **C안**: 현행 유지 + FE가 전부 `z.coerce.string()`. 장점: BE 변경 0. 비용: 계약이 FE에만 암묵 존재, 신규 응답마다 드리프트 반복.

### 1차 권장
**B안 (ID만 문자열)**. A안은 부작용이 큼(53비트 초과 안전성 명분은 있으나 비-ID 숫자까지 바뀜). 공통 `IdString` 직렬화 + DTO 컨벤션으로 통일하고 ArchUnit/리뷰로 강제.

### PES 승격 경로
- `dev.md` 계열 — "API 응답 ID 직렬화 컨벤션" + ADR
- 후보 B3(계약 스냅샷)와 함께 적용 시 재발 차단

### 미해결 질문
- JS `Number.MAX_SAFE_INTEGER` 초과 가능성(시퀀스 증가 속도) — 문자열화의 실제 필요성 측정.

---

## [후보 B3] 응답 DTO 계약 드리프트 차단 — 스냅샷/OpenAPI 계약 테스트

> 이번 M1에서만 FE↔BE 계약 불일치가 4건(enum·필드·래퍼·ID). 사람이 일일이 맞추는 한 또 난다.

### 배경
- 관측된 드리프트: `MainContentType`(BE `TEXT_ONLY` vs FE `TEXT`), `coverageSummary.axesWithUncovered` 누락, revision-reason(`optionId` vs `id`, bare 배열 vs `{options}`), ID 타입.
- FE는 MSW 목을 손으로 BE에 맞춰야 했고, 매번 사후 발견.
- BE에 응답 계약을 고정하는 **실행 가능한 진실 소스가 없음**(문서뿐).

### 후보
- **A안**: springdoc-openapi로 OpenAPI 자동 생성 → FE가 그 스키마로 타입/zod 생성. 장점: 단일 계약 소스, FE 자동화. 비용: 어노테이션 정비, 생성 파이프라인.
- **B안**: 주요 응답 DTO JSON 스냅샷 테스트(예: `@JsonTest` + 승인 테스트). 장점: 의도치 않은 형태 변경을 CI에서 차단. 비용: 스냅샷 관리.
- **C안**: 컨트랙트 테스트(Spring Cloud Contract) — BE가 stub 발행, FE가 검증. 장점: 양방향 보증. 비용: 1인 운영엔 과함.

### 1차 권장
**A안(OpenAPI) + B안(핵심 DTO 스냅샷) 병행**. OpenAPI로 FE 타입 생성 → `z.coerce`/수기 목 동기화 비용 제거. 핵심 응답엔 스냅샷으로 회귀 방지.

### PES 승격 경로
- `dev.md` — "API 계약 단일 소스화" Epic. `references/`의 PES 컨벤션과 정합.
- 의존: 후보 B2(ID 표준)와 같은 ADR로 묶기

### 미해결 질문
- FE 저장소가 별 git — OpenAPI 산출물을 어떻게 전달/버전 고정할지(아티팩트 vs 커밋).

---

## [후보 B4] Card 축-스코프 조회 로직 단일화

> "축의 카드"를 구하는 로직이 today에 한 번, 축 뷰(A3)에 또 한 번 생기면 두 곳이 갈린다.

### 배경
- `ReviewQueryService.collectToday()`가 `findOnFieldEligibleByUserIdAndAxisIds(...)`로 축 스코프 카드를 모은다.
- 후보 A3(축별 카드 조회)도 같은 "axis→deck(axisId)→card" 집계가 필요.
- 지금 분류·필터(삭제 제외, 상태, axisId IN) 규칙이 쿼리에 흩어져 있어, 새 화면마다 비슷한 쿼리가 복제될 위험.

### 후보
- **A안**: `CardQueryService.findByAxisIds(userId, axisIds, status…)` 단일 read-model 메서드로 추출, today·축뷰가 공유. 장점: 규칙 한 곳. 비용: Review BC ↔ Card BC 호출 경계 정리 필요.
- **B안**: 각자 쿼리 유지. 비용: 규칙 분기·드리프트.

### 1차 권장
**A안**. 후보 A3 구현 시 자연스럽게 함께. today는 이 공용 조회 + 스케줄 분류만 얹는 형태로 단순화.

### PES 승격 경로
- `product-card.md` Card 조회 Epic 보강 — "축 스코프 조회 read-model 단일화"
- 의존: 후보 A3와 한 Story

### 미해결 질문
- BC 경계: 축 스코프 조회를 Card BC가 소유? Review BC가 Card BC를 호출? (현재 cross-BC 호출 패턴 확인)

---

## [후보 B5] CORS 설정 단일 소스화

> CORS가 두 곳에 정의돼 있고, 핫리스타트에 취약했다(이번 M1에서 전 origin 403 발생).

### 배경
- `MvcConfig.addCorsMappings(/**)`와 `SecurityConfig.corsConfigurationSource(/**)`가 **동일 정책을 이중 정의**(origin·method·header 중복).
- 이번 M1에서 DevTools 핫리스타트 후 CorsFilter가 설정을 못 찾아 모든 preflight가 `403 Invalid CORS request` → 콜드 리스타트로만 해소. 이중 정의는 진단을 어렵게 함.

### 후보
- **A안**: SecurityConfig의 `CorsConfigurationSource` 빈 하나로 통일하고 `MvcConfig.addCorsMappings` 제거. 장점: 단일 진실 소스. 비용: 비-시큐리티 경로 영향 확인.
- **B안**: 반대로 MVC CORS만 두고 시큐리티는 `.cors(withDefaults())`로 위임. 비용: 시큐리티 필터 단계 preflight 처리 보장 확인 필요.

### 1차 권장
**A안 (SecurityConfig 단일화)**. 모든 요청이 시큐리티 체인을 통과하므로 거기에 집중. 운영 안내로 "DevTools 핫리스타트 대신 콜드 리스타트" 병기.

### PES 승격 경로
- `ops.md`/`dev.md` — "CORS 단일 소스 + 로컬 재시작 가이드". ADR 짧게.

### 미해결 질문
- 핫리스타트 시 빈 누락이 재현되는 근본 원인(DevTools 두 클래스로더) — 콜드 리스타트 가이드로 충분한가, 설정 강건화까지 갈까.

---

## v0.0.2v generic-domains.md 후보와의 연계

| 본 후보 | 연계 | 포인트 |
|--------|------|--------|
| A3/B4 (축 카드 조회) | generic 후보 2 (검색) | 축 스코프 조회와 전문 검색의 쿼리 경로 정합 |
| B2/B3 (ID·계약) | generic 후보 4 (Admin) | 신규 엔드포인트 늘수록 계약 표준 선행 이득 |
| A4 (전체 review) | product-card Epic 6 (today) | today 집계 ↔ 세션 시작 스코프 통일 |

---

## 다음 단계 제안

### 즉시 (hot-fix 성격)
```
B1  UserScheduleController 인증 교체  — 기능 차단 해소(모드/maxDuration), 위험 낮음
```

### v0.0.2v 묶음 (FE 의도 표현 — 한 SDD로)
```
A1 + A2 + A3 + B4   — Deck-Axis 가시화 & 축 스코프 카드 (한 Product/Epic으로 묶음)
                      └ A1(덱 axisId 노출) → A2(축 덱 생성) → A3(축 카드 조회)
                        B4는 A3 구현의 read-model 추출로 흡수
```

### v0.0.2v~0.0.3v (구조 부채)
```
B2  ID 직렬화 표준          — B3와 한 ADR
B3  계약 드리프트 차단(OpenAPI/스냅샷)
A4  전체-axis review        — Review 세션 도메인 점검 후
B5  CORS 단일 소스화        — ops 가이드와 함께
```

### SDD 작성 우선순위
1. `product-deck.md`(또는 product-card Epic) — A1·A2·A3·B4 (FE 의도 직결, 의존 적음)
2. `dev` 계열 ADR — B2·B3 (계약 표준)
3. Review 세션 스코프 — A4 (도메인 확인 선행)

---

*작성일: 2026-06-30 | 트래킹 기준: FE 002.md + 2026-06-29~30 카드 흐름 추적 | 상태: 전 후보 pending*
