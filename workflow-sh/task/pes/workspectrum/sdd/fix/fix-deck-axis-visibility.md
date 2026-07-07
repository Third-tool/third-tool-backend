# [Fix · SDD] Deck↔Axis 가시화 & 축 스코프 카드 조회 (2026-06-30)

## 0. 메타

- **원본 SDD (복수)**:
  - `workflow/task/pes/workspectrum/sdd/done/product-learningFacade.md` — Facade→Axis→Topic→Material 4-level 계층, Deck 자동 생성 결정
  - `workflow/task/pes/workspectrum/sdd/done/product-card.md` — Card 조회 단위(Deck/Tag), ReviewSession Layer 1 수집
  - `workflow/task/pes/workspectrum/sdd/in-progress/product-deck.md` — **본 fix가 Fix-Story 5로 신설** (Deck BC를 자체 Product로 분리)
- **선행 fix**: `workflow/task/fix/pes/version/0.0.1v/fix-axis-deck-refactor.md` — Axis→Deck 이벤트 트리거 정착. 본 fix가 그 흐름을 이어받아 응답/생성/조회 3축을 가시화.
- **후행 fix**: `workflow/task/fix/sdd/version/0.0.2v/fix-axis-deck-full-integration.md` (2026-07-01, ADR021) — 본 fix의 §4.2 부분 유지 조항(고아 덱 보존) · §4.2 수동 축 스코프 생성 · §12 Q3(팩토리 분리) 폐기. §13 명세 변경 이력 표 참조.
- **원본 Product 식별자**: LearningFacade Product + Card Product (+ 신규 Deck Product)
- **본 fix 발견 시점**: 2026-06-30
- **본 fix 작성자**: 단독 운영 (FE M1 추적 결과 기반)
- **영향 받는 Epic / Story**:
  - LearningFacade Product — Axis 생성 흐름, Deck 자동 생성 (이미 `fix-axis-deck-refactor.md`로 1차 정정 진행 중)
  - Card Product — Epic "Card 조회"(현재 Deck/Tag 단위), Epic 6 today 집계
  - 신규 Deck Product — 본 fix가 정의를 시작
- **연계 브레인스토밍**: `workflow/task/pes/brainstorming/0.0.2v/refactor-fe-intent.md` (후보 A1·A2·A3·B4)
- **연계 fix 트랙**:
  - 분리: B1 (UserScheduleController 인증) — hot-fix 트랙. §7 참조
  - 후속: B2·B3·A4·B5 — §10 Open Questions 참조

---

## 1. 배경 — 왜 원본을 바꾸나

### 1.1 현재 상태 (원본 SDD가 결정했던 흐름)

- **LearningFacade Product**: Facade → Axis → Topic → Material 4-level 계층은 정합. 단, Axis와 Deck의 관계는 `Deck.axisId` raw `Long` 컬럼 하나로만 표현 — Deck↔Axis는 FK가 아니라 "약한 연결".
- **Card Product**: Card 조회는 Deck 단위 (`GET /api/v1/decks/{deckId}/cards`)와 Tag 단위 (`/tags/{tagId}/cards`)만 정의. ReviewSession은 Layer 1 전체 수집(starvation 방지)으로 결정.
- **Deck 생성 경로**: 현행은 (a) 일반 `POST /api/v1/decks`(고아 덱 — `axisId=null`) + (b) `LearningMaterialCreatedEvent` 이벤트로 자료 등록 시 axis 결합 덱 자동 생성. 직전 fix(`fix-axis-deck-refactor.md`)로 (b)가 `LearningAxisCreatedEvent` 흐름으로 교체 진행 중이지만, **(a) 경로는 여전히 항상 고아 덱을 만든다**.
- **Deck 응답 DTO**: `DeckResponse.Summary` / `Detail` 어디에도 `axisId`/`axisName` 없음 — FE는 덱을 받아도 어느 축인지 판별 불가.
- **인증 표준**: `@AuthenticationPrincipal UserEntity user` (전 컨트롤러 표준).
- **ID 직렬화**: `Long` → JSON 숫자 (Jackson 기본). FE는 일부 스키마만 `z.coerce.string()`로 우회.

### 1.2 발견된 문제 (운영/테스트/리뷰/명세 표류에서 드러난 사실)

> 2026-06-29~30 카드 생성 흐름 추적 + FE `workflows/task/fix/brainstorming/version/0.0.1v/002.md` 기반.

1. **카드 생성 후 "사라짐" 현상** — 카드 에디터가 새 덱을 만들면 `POST /api/v1/decks`(axisId 수용 불가) 경로를 타 항상 고아 덱이 되고, today(`d.axisId IN :axisIds`) / 축 뷰가 그 덱의 카드를 집계 대상에서 제외. → 사용자 입장에선 "방금 만든 카드가 안 보임".
2. **Deck 응답에 `axisId` 부재** — FE가 덱 목록을 받아도 "이 덱이 어느 축인가" 표시 불가. 축별 그룹핑 UI(002.md Issue 4 (b)) 봉쇄.
3. **축 스코프 카드 조회 부재** — "내 축을 눌렀더니 그 축의 카드가 보인다"는 의도가 API에 표현될 경로 없음(002.md Issue 5). FE가 축의 덱들을 모아 덱별 조회 N회 호출하면 N+1.
4. **today와 축 뷰의 쿼리 분기** — `ReviewQueryService.collectToday()`가 `findOnFieldEligibleByUserIdAndAxisIds(...)`로 축 스코프 카드를 모으는 로직을 이미 보유. 축 뷰가 추가되면 동일 집계가 두 곳에서 따로 자라 드리프트 위험.

### 1.3 왜 지금 바꾸나 (타이밍 사유)

- 직전 fix(`fix-axis-deck-refactor.md`)가 **Axis→Deck 이벤트 흐름**까지 교정했지만 **일반 Deck 생성 API의 axis 결합**은 손대지 못함 — 본 fix가 후속.
- FE M1이 카드 생성 흐름을 검증하다 위 4건이 동시 노출. 한 흐름의 같은 원인이므로 묶음 처리가 비용-효율적.
- `product-deck.md`가 별도 Product로 분리될 적절한 시점 — Deck BC가 LearningFacade Product 안에 종속 결정으로 묻혀있던 구조를 본 fix가 끄집어낸다.

---

## 2. 원본 대비 delta (뒤집는 지점)

### 폐기 (Deprecate)

- **(LearningFacade Product) "Deck↔Axis는 raw `Long` 컬럼 1개로만 표현"** — 도메인 연관 매핑 승격이 아닌 **read-model 응답 노출로 한정 폐기**. `DeckResponse`에 `axisId`·`axisName` 필드 추가로 FE가 축을 식별 가능.
- **(LearningFacade Product) "Deck 자동 생성만으로 axis 결합 일원화"** — 사용자가 명시적으로 "이 카드를 어느 축에 묶고 싶다"는 UX 진입점 부재. 명시 축 스코프 Deck 생성 경로 신설로 부분 폐기 (기존 고아 덱 경로 `POST /decks`는 부분 유지).
- **(Card Product) "Card 조회는 Deck/Tag 단위만"** — 축 단위 사용자 표현 흐름이 API에 없음. 축 스코프 카드 조회 엔드포인트 신설.
- **(암묵 결정) "today 집계와 축 카드 조회는 각자 쿼리"** — read-model 단일화로 드리프트 원천 봉쇄.

### 승격 (Promote)

- **Deck BC를 자체 Product로 승격 (스켈레톤)** — 그동안 LearningFacade Product의 종속 결정으로 묻혀있던 Deck BC를 `product-deck.md`로 신설. 본 fix가 Deck Product의 첫 결정을 담는 트리거.
- **Card read-model `findByAxisIds`를 도메인 조회 표준 인터페이스로 승격** — today Epic 6 + 신규 축 뷰가 공유.

### 신설 (Introduce)

- **엔드포인트**: `POST /api/v1/learning-facade/axes/{axisId}/decks` (축 스코프 Deck 생성), `GET /api/v1/learning-facade/axes/{axisId}/cards?status=` (축 스코프 카드 조회).
- **DTO 필드**: `DeckResponse.Summary.axisId`, `DeckResponse.Summary.axisName`, `DeckResponse.Detail.axisId`, `DeckResponse.Detail.axisName`.
- **팩토리**: `Deck.createUnderAxis(user, axisId, name)` — 이벤트 핸들러용 `createFromAxis`와 분리(멱등 정책 차이).
- **Application Service**: `CardQueryService.findByAxisIds(userId, axisIds, statuses, includeArchive)`, `LearningFacadeQueryService.findAxisCards`, `DeckCommandService.createUnderAxis`.
- **컨트롤러**: `LearningFacadeController`에 두 신규 엔드포인트 (§12 Q1 잠정 결정).
- **ErrorCode 재사용**: `LEARNING_FACADE_FORBIDDEN`, `LEARNING_AXIS_NOT_FOUND` — 계획 시점 `FACADE_AXIS_NOT_OWNED`을 신규 도입하려 했으나 실행 결과 재사용으로 확정.
- **ADR020** — "Deck↔Axis 가시화 — read-model 노출 vs 도메인 연관 승격" 발행.

### 유지 (Keep)

- **`Deck.axisId` raw `Long` 컬럼** — 도메인 연관 승격 없음. BC 결합도 유지.
- **기존 `POST /api/v1/decks`(고아 덱 경로)** — 부분 유지, 자유 덱 사용 사례 확인 시까지. *(본 결정은 후행 fix `fix-axis-deck-full-integration` 2026-07-01로 폐기됨. §13 명세 변경 이력 참조)*
- **자료 등록 이벤트 자동 덱 생성** — 선행 fix가 `LearningAxisCreatedEvent → Deck.createFromAxis` 흐름으로 이미 정착.
- **덱·태그 단위 카드 조회 엔드포인트** — 그대로 유지.
- **ReviewSession Layer 1 수집** — 학습 흐름 결정 그대로.
- **ID 직렬화(Long → JSON 숫자)** — 본 fix는 표준화하지 않음. B2 트랙으로 분리.
- **Deck 응답 필드**: `axisId`/`axisName` 신규는 nullable — 고아 덱은 둘 다 null.

---

## 3. 새 목표 (To-Be)

### 3.1 최종 아키텍처 요약

```
┌─ LearningFacade BC ──────────────────────────────────┐
│  Facade(AR) → Axis(E) → Topic(E) → Material(E)       │
│             │                                         │
│             │ (raw axisId Long, FK 없음 — 유지)       │
│             │                                         │
│  ┌─ Deck BC (점진 분리 → product-deck.md 신설) ─┐    │
│  │  Deck(AR) ──▶ axisId(Long, nullable)         │    │
│  │  ├─ POST /decks                  (유지: 고아)│    │
│  │  ├─ POST /learning-facade/axes/  ◀ ★ 신설   │    │
│  │  │       {axisId}/decks                      │    │
│  │  └─ DeckResponse                             │    │
│  │      └ axisId/axisName 노출 ◀ ★ 추가        │    │
│  └──────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────┘

┌─ Card BC ────────────────────────────────────────────┐
│  Card(AR) → Deck(AR.id)                              │
│  조회 read-model (단일화):                            │
│   CardQueryService.findByAxisIds(userId,axisIds,    │
│                                  status...) ◀ ★ 신설 │
│  엔드포인트:                                          │
│   GET /decks/{deckId}/cards               (기존)     │
│   GET /tags/{tagId}/cards                 (기존)     │
│   GET /learning-facade/axes/{axisId}/cards ◀ ★ 신설  │
│        ?status=ON_FIELD|ARCHIVE                      │
└──────────────────────────────────────────────────────┘

┌─ Review BC ──────────────────────────────────────────┐
│  ReviewQueryService.collectToday()                   │
│   └ CardQueryService.findByAxisIds() 호출 ◀ ★ 변경   │
│     (자체 쿼리 제거 → Card BC 위임)                  │
└──────────────────────────────────────────────────────┘
```

### 3.2 핵심 플로우 (변경 후)

**카드 에디터 — 새 덱을 만들면서 축에 묶기**:
```
FE  ─POST /learning-facade/axes/{axisId}/decks {name}─▶  LearningFacadeController
                                                           └ DeckCommandService
                                                              .createUnderAxis(userId, axisId, name)
                                                              ├ facadeOwnership.verify(userId, axisId)
                                                              └ Deck.createUnderAxis(user, axisId, name)
FE  ◀── 201 {deckId, name, axisId, axisName}
↓
카드를 이 덱에 넣음
↓
today / 축 뷰 ← axisId IN :axisIds 필터에 포함 → 정상 표시 ✅
```

**축 뷰 — 내 축을 누르면 그 축 카드가 보임**:
```
FE  ─GET /learning-facade/axes/{axisId}/cards?status=ON_FIELD─▶
        LearningFacadeController
         └ LearningFacadeQueryService.findAxisCards(userId, axisId, status)
            ├ facadeOwnership.verify(userId, axisId)
            └ CardQueryService.findByAxisIds(userId, [axisId], ON_FIELD)
               └ SELECT c.* FROM card c
                   JOIN deck d ON c.deck_id = d.id
                   WHERE d.axis_id IN (:axisIds)
                     AND c.user_id = :userId
                     AND c.status = :status
                     AND c.deleted_at IS NULL
                     AND d.deleted_at IS NULL
FE  ◀── 200 {cards: [...]} (1회 호출)
```

**today 집계 — read-model 위임**:
```
ReviewQueryService.collectToday(userId)
 ├ facadeQueryService.findAxisIds(userId)
 └ cardQueryService.findByAxisIds(userId, axisIds, ON_FIELD)
                                     ↑ 동일 read-model을 축 뷰와 공유
```

### 3.3 마이그레이션 단계 (요약)

| # | 단계 | 마이그레이션 | 도메인 변경 |
|---|---|---|---|
| 1 | Deck 응답 `axisId`/`axisName` 노출 | 없음 | `DeckSummaryRow`/`DeckQueryService` 조인 + `DeckResponse.Summary/Detail` 필드 |
| 2 | 축 스코프 Deck 생성 경로 신설 | 없음 | `Deck.createUnderAxis`, `DeckCommandService.createUnderAxis`, `LearningFacadeController` 엔드포인트 |
| 3 | Card read-model 단일화 | 없음 | `CardQueryService.findByAxisIds`, `ReviewQueryService.collectToday` 위임 |
| 4 | 축 스코프 Card 조회 엔드포인트 | 없음 | `LearningFacadeController` 엔드포인트, `LearningFacadeQueryService.findAxisCards` |
| 5 | 문서 갱신 | 없음 | ADR020, DOMAIN.md/PACKAGE.md, done SDD 명세 변경 이력, `product-deck.md` 스켈레톤 신설 |

상세 절차는 §5 Fix-Story 5개 참조.

### 3.4 환경별 설정 분기

| 항목 | dev | prod |
| --- | --- | --- |
| DB 스키마 변경 | 없음 (deck.axis_id 기존 nullable) | 없음 |
| Flyway 마이그레이션 | 없음 | 없음 |
| 신규 엔드포인트 활성화 | 즉시 | 즉시 (피처 플래그 미사용 — 사용자 0명 단계) |
| 기존 `POST /api/v1/decks` 동작 | 유지 (고아 덱) | 유지 |
| OSIV / READ_COMMITTED | 기존 그대로 | 기존 그대로 |

---

## 4. 대안 검토 (Alternatives Considered)

### A. Deck↔Axis 가시화 (읽기)

**Option A — 응답 DTO 필드만 추가 (선택)**
- 비용: `axisName` 조인 1회. N+1 회피 위해 fetch join 또는 read-model VO 보강.
- 보상:
  - 도메인 변경 없음. read-model만.
  - FE가 응답만으로 축별 덱 그룹핑 가능.
  - Deck↔Axis 가시화의 첫 진입점.

**Option B — 도메인 연관 승격 (`Deck.axisId` → `LearningAxis` `@ManyToOne`)**
- 거부 이유: BC 결합도 상승. Deck Aggregate가 LearningAxis를 알게 되면 4-level 응집(LearningFacade Product 결정)이 흐려짐. LearningFacade BC와 Deck BC 경계 위반.

**Option C — facade 응답에 axis별 deck 요약 동봉**
- 거부 이유: facade 응답이 비대화 — 지도 초기 로드는 가벼워야 함. 별도 조회로 충분.

### B. Deck 생성 경로 (축 결합)

**Option A — `POST /api/v1/decks` 요청에 `axisId` 옵션 추가**
- 보류 이유: 경로 1개로 단순하지만 axisId 옵션의 소유권 검증이 본문 검증으로 들어가야 함 — path가 의미를 표현하지 못함.

**Option B — 축 스코프 Deck 생성 경로 신설 `POST /learning-facade/axes/{axisId}/decks` (선택)**
- 비용:
  - 엔드포인트 1개 추가, Controller·DTO·Service 신규.
  - 기존 `POST /api/v1/decks`와 의미 분기 — FE 호출 분기.
- 보상:
  - "축에 덱을 단다"는 사용자 의도가 API에 명시.
  - axis 소유권(userId) 검증을 path가 자연스럽게 강제.
- 부수 정책: 한 축에 여러 덱 허용 vs 축당 기본 덱 1개 — 본 fix에서는 **다중 허용**. *(단, 후행 fix `fix-axis-deck-full-integration`에서 축당 1개로 확정. §13 참조.)*

**Option C — 덱 생성을 자료 생성 흐름으로만 유도**
- 거부 이유: 카드 ≠ 자료라 의미 왜곡, UX 우회.

### C. 축 스코프 Card 조회

**Option A — `GET /learning-facade/axes/{axisId}/cards?status=` 엔드포인트 (선택)**
- 비용: 조인 쿼리(axis→deck→card) 1개 추가. status 기본값 `ON_FIELD`, 명시 시 따름.
- 보상:
  - axis 뷰 사용자 의도가 경로에 명시.
  - 단일 호출 — FE가 덱 N개를 모아 N회 호출하는 N+1 회피.
  - today 집계 쿼리와 read-model 공유(§D).

**Option B — FE가 덱별 조회 N회**
- 거부 이유: N+1 + FE 복잡도.

**Option C — facade 응답에 축별 카드 요약 동봉**
- 거부 이유: facade 응답 비대화. 카드 미리보기/상세는 결국 별도 호출 필요.

**범위 결정**: 축 직속 카드 평면 목록 (Topic 단위 그룹핑 X). Topic 단위는 사용 사례 검증 후 v0.0.3v로 분리(§10).

### D. today 집계 vs 축 뷰 read-model

**Option A — `CardQueryService.findByAxisIds` 단일 read-model (선택)**
- 비용: Review BC → Card BC 의존 방향 컨벤션 확인 필요.
- 보상:
  - 분류·필터(삭제 제외, 상태, axisId IN) 규칙 한 곳.
  - today는 "공용 조회 + 스케줄 분류"만 얹는 형태로 단순화.

**Option B — 각자 쿼리 유지**
- 거부 이유: 규칙 분기·드리프트가 본 fix가 차단하려는 핵심.

### E. ID 응답 직렬화 (본 fix 범위 밖)

- 본 fix는 Deck↔Axis 가시화에 집중. ID 직렬화 표준화(B2)는 별도 fix 트랙으로 분리(§10).
- 본 fix가 추가하는 응답 필드(`axisId`, `axisName`)는 현행 Jackson 기본 직렬화(`Long` → JSON 숫자)를 따른다. FE는 기존 `z.coerce.string()` 우회 패턴 유지.

---

## 5. Fix-Epic / Fix-Story 분할

본 fix는 단일 Fix-Epic "Deck↔Axis 가시화 & 축 스코프 카드 조회" 아래 5개 Fix-Story로 구성. 순차 실행.

### Fix-Story 1: Deck 응답 DTO에 `axisId` + `axisName` 노출

#### User Story
- As a FE 개발자
- I want Deck 응답에서 축 식별자와 이름을 받기를
- so that 별도 조회 없이 덱을 축별로 그룹핑해 UI에 표시할 수 있다

#### 설명
`DeckSummaryRow` / `DeckQueryService`에 `LearningAxis` 조인 추가. `DeckResponse.Summary` / `Detail`에 `axisId: Long?` · `axisName: String?` 필드 추가(고아 덱은 null). QueryDSL Q클래스 재생성 필요.

**핵심 파일/메서드**:
- `src/main/java/.../Deck/infrastructure/dto/DeckSummaryRow.java` — axisId·axisName 컬럼 투영
- `src/main/java/.../Deck/application/service/DeckQueryService.java` — 조인 결과 매핑
- `src/main/java/.../Deck/presentation/dto/DeckResponse.java` — `Summary`/`Detail`에 필드 추가

#### 완료 기준 (AC)
- Given axis 결합 덱 1건 (`axisId=42`) / When `GET /decks/{id}` / Then 응답 `axisId=42, axisName="algorithms"`
- Given 고아 덱 1건 / When `GET /decks/{id}` / Then 응답 `axisId=null, axisName=null`
- *(엣지 - 다중 덱)* Given axis 결합 3건 + 고아 2건 / When `GET /decks` / Then 각 덱 응답 정확 매핑, N+1 미발생

#### Definition of Done
- [ ] 구현: `DeckSummaryRow`, `DeckQueryService`, `DeckResponse.Summary/Detail`
- [ ] QueryDSL Q클래스 재생성 확인
- [ ] 슬라이스 `@DataJpaTest`: join 결과 axis 결합·고아 양쪽 검증
- [ ] Controller slice: 응답 JSON 필드 검증
- [ ] 원본 SDD 반영: (Fix-Story 5에서 일괄)

#### 스토리 포인트
1d

#### 의존성
- 선행: 선행 fix `fix-axis-deck-refactor.md` Fix-Story 1~4 완료 (Axis 이벤트 흐름 정착)
- 후행: Fix-Story 2 (DTO 검증 공유)

---

### Fix-Story 2: 축 스코프 Deck 생성 경로 신설

#### User Story
- As a 학습 사용자
- I want 카드 에디터에서 새 덱을 만들 때 축에 묶기를
- so that 그 덱의 카드가 today / 축 뷰에서 즉시 노출된다

#### 설명
`POST /api/v1/learning-facade/axes/{axisId}/decks` 신설. path에 axis 소유권이 드러나 검증 단순. `Deck.createUnderAxis(user, axisId, name)` 정적 팩토리 (이벤트 핸들러용 `createFromAxis`와 멱등 정책 차이로 분리). `DeckCommandService.createUnderAxis()` 신설, axis 소유권은 `FacadeOwnershipVerifier` 협력.

**핵심 파일/메서드**:
- `src/main/java/.../LearningFacade/presentation/LearningFacadeController.java` — `POST /axes/{axisId}/decks`
- `src/main/java/.../Deck/domain/model/Deck.java` — `createUnderAxis` 정적 팩토리
- `src/main/java/.../Deck/application/service/DeckCommandService.java` — `createUnderAxis(userId, axisId, name)`

**멱등 정책**: 명시 신규 생성. 동일 `(userId, axisId, name)` 중복은 `DECK_NAME_DUPLICATE` (기존 ErrorCode).

**ErrorCode 계획 vs 실제**: 계획은 `FACADE_AXIS_NOT_OWNED` 신설이었으나 실행 시 `LEARNING_FACADE_FORBIDDEN` 재사용으로 확정. §12 실행 결과 참조.

#### 완료 기준 (AC)
- Given 유저 A의 axisId / When 유저 A 인증 후 `POST .../axes/{axisId}/decks {name:"backend"}` / Then 201, 응답에 `axisId`/`axisName` 포함
- Given 유저 A 인증 + 유저 B의 axisId / When `POST` / Then 403 `LEARNING_FACADE_FORBIDDEN`
- Given 없는 axisId / When `POST` / Then 404 `LEARNING_AXIS_NOT_FOUND`
- *(엣지 - name blank)* Given `{name:"  "}` / When `POST` / Then 400 (도메인 검증)
- *(엣지 - 이름 중복)* Given 동일 (userId, axisId, name) 존재 / When 재요청 / Then `DECK_NAME_DUPLICATE`

#### Definition of Done
- [ ] 구현: 컨트롤러·서비스·팩토리
- [ ] 단위: `Deck.createUnderAxis_해피/name_blank_예외`
- [ ] Application Service: `DeckCommandServiceCreateUnderAxisTest`
- [ ] Controller slice: 4개 에러 응답 형식
- [ ] 통합: axis 결합 덱 생성 + today 노출 검증

#### 스토리 포인트
1.5d

#### 의존성
- 선행: Fix-Story 1 (Deck 응답 DTO 필드 확립)
- 후행: Fix-Story 3 (독립 가능하나 동일 세션 권장)

---

### Fix-Story 3: Card read-model 단일화 (`findByAxisIds`)

#### User Story
- As a 백엔드 Story 작성자
- I want today 집계와 축 뷰가 같은 조회 로직을 공유하기를
- so that 필터·상태 규칙이 두 곳에서 자라 드리프트되지 않는다

#### 설명
`CardQueryService.findByAxisIds(userId, axisIds, statuses, includeArchive)` 신설. Repository 메서드 신설. `ReviewQueryService.collectToday()` 자체 쿼리 호출을 위 메서드로 교체.

**핵심 파일/메서드**:
- `src/main/java/.../Card/application/service/CardQueryService.java` — `findByAxisIds`
- `src/main/java/.../Card/infrastructure/persistence/CardRepositoryAdapter.java` — `findByUserIdAndAxisIdsAndStatus`
- `src/main/java/.../Review/application/service/ReviewQueryService.java` — `collectToday` 위임 변경
- **삭제**: 구 `findOnFieldEligibleByUserIdAndAxisIds`

**회귀 테스트 필수**: 기존 today 통합 테스트 그대로 통과 + 동일 결과 어설션.

#### 완료 기준 (AC)
- Given axis 3개, 각 축에 카드 5개 씩 (ON_FIELD 3 + ARCHIVE 2) / When `findByAxisIds(u, [1,2,3], [ON_FIELD])` / Then 9건 반환
- Given `axisIds` 빈 리스트 / When 조회 / Then 0건 반환
- Given soft delete 카드 1건 + 활성 4건 / When 조회 / Then 4건 반환 (soft delete 제외)
- Given today Epic 6 통합 시나리오 / When `collectToday()` 호출 / Then 위임 변경 후에도 기존 동일 결과 통과

#### Definition of Done
- [ ] 구현: `CardQueryService.findByAxisIds`, Repository 메서드, `ReviewQueryService.collectToday` 위임
- [ ] 구 메서드 제거
- [ ] 슬라이스: `CardRepositoryAxisCardsSliceTest`(@DataJpaTest)
- [ ] 단위: `CardRepositoryAdapterAxisCardsTest`
- [ ] 회귀: `ReviewQueryServiceTodayCandidatesTest` 등 today 관련 통합·슬라이스 전 통과
- [ ] 원본 SDD 반영: (Fix-Story 5에서 일괄)

#### 스토리 포인트
1d

#### 의존성
- 선행: Fix-Story 1 (DTO 정합 후 안전)
- 후행: Fix-Story 4 (read-model 재사용)

---

### Fix-Story 4: 축 스코프 Card 조회 엔드포인트 신설

#### User Story
- As a 학습 사용자
- I want 축을 누르면 그 축의 카드가 한 번의 조회로 보이기를
- so that N+1 없이 축 단위 학습/탐색이 가능하다

#### 설명
`GET /api/v1/learning-facade/axes/{axisId}/cards?status=ON_FIELD|ARCHIVE`. `LearningFacadeQueryService.findAxisCards`(소유권 검증 후 `CardQueryService.findByAxisIds` 위임).

**핵심 파일/메서드**:
- `src/main/java/.../LearningFacade/presentation/LearningFacadeController.java` — `GET /axes/{axisId}/cards`
- `src/main/java/.../LearningFacade/application/service/LearningFacadeQueryService.java` — `findAxisCards(userId, axisId, status)`

**status 파라미터**: 기본 `ON_FIELD`. 잘못된 enum 값은 GlobalExceptionHandler 표준 처리.

#### 완료 기준 (AC)
- Given axis 1개에 덱 2개, 각 덱에 카드 3개(ON_FIELD) + 1개(ARCHIVE) / When `?status=ON_FIELD` / Then 6건
- Given 동일 상태 / When `?status=ARCHIVE` / Then 2건
- Given `?status` 미명시 / When 조회 / Then `ON_FIELD` 기본, 6건
- Given 유저 A 인증 + 유저 B의 axisId / When 조회 / Then 403
- Given 잘못된 status enum / When 조회 / Then 400

#### Definition of Done
- [ ] 구현: 컨트롤러·QueryService
- [ ] Controller slice: 응답·에러 형식
- [ ] 통합: `AxisCardsQueryIntegrationTest`(@SpringBootTest)

#### 스토리 포인트
1d

#### 의존성
- 선행: Fix-Story 3 (read-model 준비)
- 후행: Fix-Story 5

---

### Fix-Story 5: docs (ADR020 · DOMAIN.md · PACKAGE.md · product-deck.md 스켈레톤 · 명세 변경 이력)

#### User Story
- As a Claude 및 미래 협업자
- I want 본 fix의 결정이 진실 소스에 박제되고 Deck BC가 자체 Product로 승격되기를
- so that Deck 관련 새 Story 진입 시 규범 문서에서 결정을 확인할 수 있고 스켈레톤에 이어 채울 수 있다

#### 설명
새 ADR 발행 + DOMAIN.md/PACKAGE.md 갱신 + done Product 2건 [명세 변경 이력] 블록 + `product-deck.md` 스켈레톤 신설.

**작업 파일**:
- `docs/adr/ADR020-deck-axis-visibility.md` — 신규 (Option A/B 비교)
- `docs/adr/index.md` — ADR020 추가
- `docs/DOMAIN.md` — §2.1 Card / §2.2 Deck · 변경 이력
- `docs/PACKAGE.md` — §6 `LearningFacade → Card` BC 의존
- `workflow/task/pes/workspectrum/sdd/done/product-learningFacade.md` — [명세 변경 이력]
- `workflow/task/pes/workspectrum/sdd/done/product-card.md` — [명세 변경 이력]
- `workflow/task/pes/workspectrum/sdd/in-progress/product-deck.md` — **신규 스켈레톤** (Outcome/Design Decisions/Architecture 섹션 비워두고 본 fix reference 인용)

#### 완료 기준 (AC)
- Given ADR020 발행 / When ADR 인덱스 조회 / Then Option A(read-model) 채택 + Option B(도메인 연관 승격) 거부 사유 명시
- Given DOMAIN.md 갱신 / When Deck BC 섹션 조회 / Then axis 결합 정책 명시
- Given PACKAGE.md 갱신 / When BC 의존 그래프 조회 / Then `LearningFacade → Card` 명시
- Given done SDD 2건 갱신 / When [명세 변경 이력] 블록 조회 / Then 본 fix 인용

#### Definition of Done
- [ ] ADR020 발행 + index
- [ ] DOMAIN.md · PACKAGE.md 갱신
- [ ] done Product 2건 [명세 변경 이력]
- [ ] product-deck.md 스켈레톤 신설

#### 스토리 포인트
0.5d

#### 의존성
- 선행: Fix-Story 4 (코드 안정 후)
- 후행: (없음)

---

## 6. ADR 승격 후보

- **ADR020 — Deck↔Axis 가시화 — read-model 노출 vs 도메인 연관 승격** (필수 발행 · Fix-Story 5 완료)
  - 채택된 Option A(read-model 노출)의 트레이드오프: axis 조인 1회 비용, 도메인 변경 없음.
  - 거부된 Option B(도메인 연관 승격)의 사유 박제: BC 결합도 상승, 4-level 응집 흐림.

- **ADR-후속(선택) — 축당 덱 정책** — 다중 vs 단일. 본 fix는 다중 허용. 후행 fix `fix-axis-deck-full-integration`(ADR021)이 축당 1개로 확정. 실제 후속 ADR021로 승격됨.

- **ADR-후속(선택) — API 계약 드리프트 차단 정책 (B3)** — 신규 엔드포인트 2개부터 OpenAPI 스냅샷 계약 검증 도입 여부. 후속 fix 트랙.

---

## 7. 진실 소스 반영 계획

### 7.1 코드 반영

| 대상 | 파일 | 변경 |
|---|---|---|
| Domain | `Deck/domain/model/Deck.java` | `createUnderAxis` 정적 팩토리 |
| Application | `Deck/application/service/DeckCommandService.java` | `createUnderAxis(userId, axisId, name)` |
| Application | `Card/application/service/CardQueryService.java` | `findByAxisIds(userId, axisIds, statuses)` |
| Application | `LearningFacade/application/service/LearningFacadeQueryService.java` | `findAxisCards(userId, axisId, status)` |
| Application | `Review/application/service/ReviewQueryService.java` | `collectToday()` 위임 변경 (자체 쿼리 → `findByAxisIds`) |
| Infrastructure | `Card/infrastructure/persistence/CardRepositoryAdapter.java` | `findByUserIdAndAxisIdsAndStatus` 신설, 구 `findOnFieldEligibleByUserIdAndAxisIds` 제거 |
| Infrastructure | `Deck/infrastructure/dto/DeckSummaryRow.java` | axisId/axisName 컬럼 투영 |
| Presentation | `LearningFacade/presentation/LearningFacadeController.java` | 2 신규 엔드포인트 |
| Presentation | `Deck/presentation/dto/DeckResponse.java` | Summary/Detail 필드 추가 |
| ErrorCode | (기존 재사용) | `LEARNING_FACADE_FORBIDDEN`, `LEARNING_AXIS_NOT_FOUND`, `DECK_NAME_DUPLICATE` |

### 7.2 Flyway

- 없음 (`deck.axis_id`는 이미 nullable 컬럼으로 존재)

### 7.3 문서 반영

| 문서 | 섹션 | 반영 내용 |
|---|---|---|
| `docs/adr/ADR020-deck-axis-visibility.md` | 신규 파일 | Option A/B 비교, 채택 근거 |
| `docs/adr/index.md` | 엔트리 | ADR020 추가 |
| `docs/DOMAIN.md` | §2.1 Card | 축 스코프 조회는 ReviewSession Layer 1과 별개 사용자 표현 흐름 |
| `docs/DOMAIN.md` | §2.2 Deck | axis 결합 정책(자동 1개 + 명시 추가 N개 + 고아 덱 부분 유지) |
| `docs/DOMAIN.md` | 변경 이력 | 본 fix 일자 추가 |
| `docs/PACKAGE.md` | §6 | `LearningFacade → Card` BC 의존 명시 |
| `workflow/.../done/product-learningFacade.md` | [명세 변경 이력] | 본 fix로 §3.1/§3.2 부분 폐기 |
| `workflow/.../done/product-card.md` | [명세 변경 이력] | 본 fix로 §3.3/§3.4 폐기 |
| `workflow/.../in-progress/product-deck.md` | 신규 스켈레톤 | Deck BC 자체 Product 승격 · 본 fix reference |

### 7.4 Swagger

- 신규 엔드포인트 2개는 Controller 등록으로 자동 노출.

### 7.5 FE 저장소 (third-tool-fe · 별도 git)

- **실행 결과**: `createAxisDeck`·`listAxisCards`·Deck 스키마(`axisId`/`axisName`)가 이미 FE에 구현되어 있어 본 fix 완료 시 즉시 계약 충족. 추가 작업 없음.

---

## 8. (선택) 실패 모드 / 관측 갱신

| 시나리오 | 변경 전 | 변경 후 |
| --- | --- | --- |
| 카드 에디터 → 신규 덱 → 카드 추가 → today 노출 | 고아 덱이 되어 today 미노출 (사용자 인지: "사라짐") | `POST /axes/{axisId}/decks` 호출 시 axis 결합 → today/축뷰 정상 노출 |
| FE 덱 목록에서 axisId 그룹핑 | 불가능 (응답에 axisId 없음) | `axisId`/`axisName` 응답 필드로 즉시 그룹핑 가능 |
| 축 카드 뷰 N+1 | (경로 없음) FE가 임시 구현 시 N회 호출 | 단일 호출, axis→deck→card 조인 1회 |
| today와 축 뷰의 카드 필터 규칙 드리프트 | 별도 쿼리 — 한 곳만 수정되면 분기 | 공유 read-model — 한 곳 수정 |
| axis 소유권 위반 | (경로 없음) | Application Service 소유권 검증 → `LEARNING_FACADE_FORBIDDEN` → 403 |
| axis 미존재 | (경로 없음) | `LEARNING_AXIS_NOT_FOUND` → 404 |
| 다른 유저의 axis 카드 조회 시도 | (경로 없음) | 소유권 검증 → 403 |

**로깅 표준**: 신규 두 엔드포인트는 기존 `RequestLoggingFilter` 표준 따름. 별도 metric 신설 안 함(트래픽 작음).

---

## 9. (선택) 검증

### 통합 테스트
- **시나리오 A — axis 결합 덱 생성 + 카드 추가 + today/축 뷰 노출**:
  - `POST /facades/{facadeId}/axes` (선행 fix 흐름으로 자동 덱 1개 생성됨)
  - `POST /api/v1/learning-facade/axes/{axisId}/decks {name: "추가 덱"}` → 201, axisId 응답 포함
  - 해당 덱에 카드 1개 추가
  - `GET /review-session/today` → 카드 포함
  - `GET /api/v1/learning-facade/axes/{axisId}/cards?status=ON_FIELD` → 카드 포함
- **시나리오 B — 고아 덱 회귀**:
  - `POST /api/v1/decks {name: "고아", parentDeckId: null}` → 201, `axisId: null` / `axisName: null`
  - today에 고아 덱의 카드 미노출 (기존 동작 유지)
- **시나리오 C — axis 소유권 위반**:
  - 유저 A로 인증 + 유저 B의 axisId로 `POST .../axes/{B의 axisId}/decks` → 403 `LEARNING_FACADE_FORBIDDEN`

### 단위 / 슬라이스
- `DeckQueryService.findSummaries` — axis 결합 덱 / 고아 덱 양쪽에서 `axisId`/`axisName` 결과 검증 (해피·엣지 2건).
- `Deck.createUnderAxis()` — 정적 팩토리 검증 (해피 + name blank 예외).
- `CardQueryService.findByAxisIds()` — axisIds 빈 리스트, 다중 axis, status 필터, soft delete 제외 4건.
- Controller slice: 각 신규 엔드포인트 요청·응답 직렬화 + 에러 응답 형식 검증.

### 카나리 / 단계별 배포
- 사용자 0명 단계라 카나리 불필요. dev → prod 동일 시점 반영.

### 롤백 절차
- 본 fix는 **추가 위주**(신규 엔드포인트·신규 응답 필드·신규 read-model 메서드 + 기존 today 호출 변경). 롤백 시:
  1. 신규 엔드포인트 호출 무시 가능 (FE가 옛 경로로 폴백).
  2. Deck 응답의 신규 필드는 FE optional 처리되어 호환.
  3. `ReviewQueryService.collectToday()` 위임 변경은 유일한 in-place 수정 — 롤백 시 직전 직접 쿼리 호출로 복원. 회귀 테스트가 동일 결과를 보장하므로 동작 차이 0.
- 데이터 변경 없음 — DB 롤백 불필요.

---

## 10. (선택) Open Questions

다음 항목은 본 fix 범위 밖. 별도 fix 트랙 또는 다음 fix 버전(0.0.3v) 트리거.

### 본 fix 진행 중 결정 필요 (실행 시 해소됨)

1. ~~**AxisDeckController / AxisCardController 위치**~~ — 실행 결과: `LearningFacadeController` 배치로 잠정 확정.
2. ~~**`POST /api/v1/decks` (고아 덱 경로) 향후**~~ — 후행 fix `fix-axis-deck-full-integration` (2026-07-01, ADR021)로 폐기. §13 참조.
3. ~~**`Deck.createFromAxis()` vs `Deck.createUnderAxis()` 통합 여부**~~ — 후행 fix로 `createUnderAxis` 제거, `createFromAxis` 유일 팩토리로 확정.

### 후속 fix 트리거 (v0.0.3v 이후)

4. ~~**축당 기본 덱 1개 정책 vs 다중 덱 허용**~~ — 후행 fix로 축당 1개로 확정. §13 참조.
5. **Topic 단위 카드 조회** — 축→Topic→카드 트리뷰 사용 사례가 등장하면 신규 엔드포인트.
6. **Deck Product 신설 완성** — 본 fix가 만든 스켈레톤에 Outcome / Design Decisions 채우기. 별도 planning 사이클.
7. **B2 (ID 직렬화 표준화)** — 본 fix가 추가한 `axisId`도 그 표준에 포함시켜 재발행 필요.
8. **B3 (OpenAPI/스냅샷 계약 드리프트 차단)** — 본 fix가 추가한 신규 엔드포인트 2개부터 적용해 표준 정착.
9. **A4 (전체 axis review 세션)** — Review 세션 도메인 점검 후.
10. **B5 (CORS 단일화)** — 본 fix와 무관, ops 트랙.

---

## 11. (선택) 자가 점검

- [x] 원본 SDD에서 폐기되는 결정이 §2 폐기에 모두 명시됨 (LearningFacade Product 2건 + Card Product 2건)
- [x] 새 결정의 트레이드오프(보상/비용)가 §4에 1줄씩 명시됨 (Option A/B/C/D/E)
- [x] 마이그레이션 단계(§3.3 + §5 Fix-Story 5개)가 실행 가능한 순서로 기재
- [x] 원본 명세 갱신(§7 진실 소스 반영)의 파일·섹션 매핑 명확
- [x] ADR 트리거(§6) — 새 ADR020 발행 (Fix-Story 5 포함)
- [x] 분리된 트랙(B1 hot-fix, B2/B3/A4/B5 후속) §7·§10에 명시
- [x] 검증(§9) 통합 테스트 + 단위/슬라이스 + 롤백 절차 3축 충족
- [x] FE 저장소 영향 §7.5에 명시 (별도 git이므로 누락 위험)

---

*작성일: 2026-06-30 | 트래킹 기준: `refactor-fe-intent.md` (brainstorming 0.0.2v) + 직전 `fix-axis-deck-refactor.md` 완료 가정 | 상태: **완료 (2026-06-30)***

---

## 12. (선택) 실행 결과 (2026-06-30)

- **Fix-Story 1** — Deck 응답 `axisId`/`axisName` 노출 완료. 잔여(QueryDSL 검색 경로 `DeckSummaryRow`/`searchDecks`)도 본 세션에서 패치 — **raw `axisId`만 투영**(고아 덱 null). `axisName`은 BC 경계 유지를 위해 도메인 조인 대신 서비스 레이어 배치 보강에 위임(현재 `searchDecks` 소비자 없음).
- **Fix-Story 2** — `POST /learning-facade/axes/{axisId}/decks` 완료(선행 세션).
- **Fix-Story 3** — `CardRepository.findByUserIdAndAxisIdsAndStatus` 단일 read-model 신설, `ReviewQueryService.collectToday` 위임 전환. 구 `findOnFieldEligibleByUserIdAndAxisIds` 제거. **today 회귀 테스트 동일 결과 통과** — threshold 제거 가설 검증됨(eligibility는 `SoftScheduleTemplate` 인메모리 게이트가 책임).
- **Fix-Story 4** — `GET /learning-facade/axes/{axisId}/cards?status=` 완료. `LearningFacadeQueryService.findAxisCards`(소유권 검증 후 `CardQueryService.findByAxisIds` 위임). 신규 ErrorCode 미도입 — Story 2 선례대로 기존 `LEARNING_FACADE_FORBIDDEN`/`LEARNING_AXIS_NOT_FOUND` 재사용(§7의 `FACADE_AXIS_NOT_OWNED`는 구현 컨벤션으로 대체).
  - **⚠️ 실행 지연 정정 (2026-07-01)**: 초기 실행 시 read-model(`CardQueryService.findByAxisIds`)까지는 완료됐으나 **Controller 엔드포인트와 `LearningFacadeQueryService.findAxisCards` 자체가 미구현 상태로 남아있었음**을 후속 커버리지 감사에서 확인 (커밋 로그에 Fix-Story 4 태그 부재). `CardQueryService.java:56` 및 `CardJpaRepository.java:123` 주석에서만 참조되고 실제 엔드포인트 부재. 본 세션에서 다음을 추가 반영:
    - `LearningFacadeQuery.FindAxisCards(userId, axisId, status)` record 신설
    - `LearningFacadeQueryService.findAxisCards()` 메서드 신설 (소유권 검증 + Card BC 위임)
    - `LearningFacadeController` — `GET /learning-facade/axes/{axisId}/cards?status=` 엔드포인트 신설 (기본값 `ON_FIELD`)
    - 단위 테스트 `LearningFacadeQueryServiceFindAxisCardsTest` 5건 (해피 3 · 예외 2)
    - 기존 `LearningFacadeQueryServiceTest`의 생성자 시그니처 정정 (CardQueryService mock 주입)
- **Fix-Story 5** — [ADR020](../../../../../docs/adr/ADR020-deck-axis-visibility.md) 발행 + index, `docs/DOMAIN.md`(§2.1·2.2·변경 이력), `docs/PACKAGE.md`(§6 `LearningFacade → Card`), `done/product-card.md`·`done/product-learningFacade.md` 명세 변경 이력 블록, `in-progress/product-deck.md` 스켈레톤 신설.
- **FE(third-tool-fe/untitled)**: `createAxisDeck`·`listAxisCards`·Deck 스키마(`axisId`/`axisName`) 모두 **기구현** — 본 백엔드 변경이 FE가 이미 기대하던 계약을 충족. FE 추가 작업 없음.
- **테스트**: `CardRepositoryAxisCardsSliceTest`(@DataJpaTest), `CardRepositoryAdapterAxisCardsTest`(단락 가드), `AxisCardsQueryIntegrationTest`(@SpringBootTest) 신설 + 회귀(`ReviewQueryServiceTodayCandidatesTest` 등) 통과.
- **§10 Q1 해소**: 축 스코프 엔드포인트는 `LearningFacadeController` 배치로 잠정 확정(Story 2 선례).

---

## 13. (선택) 명세 변경 이력

| 후속 fix | 날짜 | 폐기된 결정 |
| --- | --- | --- |
| fix-axis-deck-full-integration (0.0.2v) — [ADR021](../../../../../docs/adr/ADR021-axis-deck-full-integration.md) | 2026-07-01 | **§4 대안 검토 B (선택) 축 스코프 Deck 생성 경로 신설** — `POST /learning-facade/axes/{axisId}/decks` + `Deck.createUnderAxis` 팩토리를 사용자 명시 진입점으로 신설했으나, 축=덱 1:1 정책으로 재정의되면서 폐기. Deck 생성은 `LearningAxisCreatedEventHandler → Deck.createFromAxis` 유일 진입점. |
| fix-axis-deck-full-integration (0.0.2v) — [ADR021](../../../../../docs/adr/ADR021-axis-deck-full-integration.md) | 2026-07-01 | **§4 대안 검토 B 부수 정책 — "한 축에 여러 덱 허용"** — 축당 다중 덱 허용 결정을 폐기. 축당 자동 1 Deck으로 확정. `deck.axis_id`는 `NOT NULL` (Flyway V15). §10 Q4가 자동 해소. |
| fix-axis-deck-full-integration (0.0.2v) — [ADR021](../../../../../docs/adr/ADR021-axis-deck-full-integration.md) | 2026-07-01 | **§2 유지 조항 — "일반 `POST /api/v1/decks`(고아 덱)는 보존"** — 자유 덱 사용 사례가 실증되지 않아 폐기. 고아 Deck 마이그레이션은 V15가 soft delete로 아카이브 후 남은 NULL row hard delete 처리. §10 Q2가 자동 해소. |
| fix-axis-deck-full-integration (0.0.2v) — [ADR021](../../../../../docs/adr/ADR021-axis-deck-full-integration.md) | 2026-07-01 | **§10 Q3 — "`createFromAxis` vs `createUnderAxis` 분리 유지"** — `createUnderAxis` 제거로 자동 해소. |
