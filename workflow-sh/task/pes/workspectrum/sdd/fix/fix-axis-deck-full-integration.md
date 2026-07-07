# [Fix · SDD] Axis↔Deck 완전 통합 — 자동 생성 유일화 & Axis Soft Delete (2026-07-01)

## 0. 메타

- **원본 SDD (복수)**:
  - `workflow/task/pes/workspectrum/sdd/done/product-learningFacade.md` — Facade→Axis→Topic→Material 4-level 계층, Axis Hard Delete 정책
  - `workflow/task/pes/workspectrum/sdd/done/product-card.md` — Card 조회 단위(Deck/Tag/Axis 스코프)
  - `workflow/task/pes/workspectrum/sdd/in-progress/product-deck.md` — 선행 fix가 신설한 Deck Product 스켈레톤
- **선행 fix**: `workflow/task/fix/sdd/version/0.0.2v/fix-deck-axis-visibility.md` — Deck 응답 axis 가시화 + 축 스코프 생성/조회 신설. 본 fix는 그 결정 중 **§4.2 부분 유지 조항(고아 덱 경로 보존) · §4.2 부수 정책(수동 다중 덱 허용)** 을 폐기하고 자동 경로만 남긴다.
- **후행 fix**: 없음 (본 fix로 axis↔deck 통합 매듭)
- **원본 Product 식별자**: LearningFacade Product + Card Product + Deck Product (스켈레톤)
- **본 fix 발견 시점**: 2026-07-01
- **본 fix 작성자**: 단독 운영
- **영향 받는 Epic / Story**:
  - LearningFacade Product — Axis 삭제 Story(Hard→Soft), Axis 생성 이벤트 흐름
  - Deck Product — 생성 경로 3→1 정리, DB 컬럼 nullable→NOT NULL
  - Card Product — deck.axisId 항상 non-null 전제 확정
- **연계 브레인스토밍**:
  - `workflow/task/fix/brainstorming/version/0.0.2v/issue-deck-axis-integration.md`
  - `workflow/task/fix/brainstorming/version/0.0.2v/issue-card-axis-recognition.md` (본 fix로 흡수)

---

## 1. 배경 — 왜 원본을 바꾸나

### 1.1 현재 상태 (선행 fix까지 반영된 흐름)

- **Deck 생성 경로 3중 병존**:
  1. `POST /api/v1/decks` (`DeckController.java:25-32` → `DeckCommandService.create` → `Deck.of()`) — axisId=null 고아 덱
  2. `POST /api/v1/learning-facade/axes/{axisId}/decks` (선행 fix Fix-Story 2, `LearningFacadeController.java:108-118` → `DeckCommandService.createUnderAxis` → `Deck.createUnderAxis`) — 사용자 명시 축 스코프 생성
  3. `LearningAxisCreatedEventHandler` (`Deck.createFromAxis`) — 이벤트 자동, 축당 멱등 1개
- **`deck.axis_id`**: nullable (`V7__deck_axis_material_link.sql`). FK `fk_deck_axis ON DELETE SET NULL`.
- **LearningAxis Hard Delete**:
  - `LearningFacade.axes` — `CascadeType.ALL` + `orphanRemoval=true` (`LearningFacade.java:41-48`)
  - `LearningFacade.removeAxis()` — `axes.remove(target)` 만 호출 (`LearningFacade.java:96-99`) → JPA orphanRemoval로 DB에서 완전 삭제
  - `learning_axis` 스키마에 `deleted_at` 컬럼 없음 (V2). Soft Delete 정책 부재.
- **Deck는 Soft Delete** (`Deck.softDelete()`) — Axis만 Hard Delete로 정책 불일치.

### 1.2 발견된 문제 (2026-07-01 로컬 테스트)

> 사용자 리포트:
> "새로운 Card를 만들 때 학습 축이 인식이 안됩니다. 지도에서 축이 만들어지고도 이유는 모르겠는데, 바로 지워집니다. data가 남아있지 않은지 화면을 나가면 바로 삭제되어 새카드 만들 때 덱 인식이 안되는 것 같습니다."
>
> "Axis가 곧 덱과 의미가 동일하다. Deck 자체는 스스로 만들어지기는 없었으면 좋겠고, 학습 로드맵에서 Axis를 만들 때마다만 덱이 같이 만들어졌으면 좋겠습니다."

원인 후보 (`issue-card-axis-recognition.md`):
| # | 후보 | 근거 | 심각도 |
|---|---|---|---|
| 1 | `LearningFacade.removeAxis()` orphanRemoval hard delete | `LearningFacade.java:96-99` | HIGH |
| 2 | 프론트가 임시 Deck(수동 축 스코프) 만들고 이탈 시 DELETE 호출 | `Deck.softDelete()` | MEDIUM |
| 3 | addAxis save 후 이벤트 발행. 이벤트 핸들러 예외 시 축 롤백 | `LearningFacadeCommandService.java:69-85` | MEDIUM |
| 4 | LearningAxis Hard Delete 정책 자체 | V2 스키마 | 정책 |
| 5 | `@EntityGraph` axes eager, topics lazy | `LearningFacadeJpaRepository.java:17-19` | LOW |

의미 축: **사용자 지시 "Axis = 덱"** 와 현재 3중 경로가 부합하지 않음. 선행 fix가 응답 가시화·축 스코프 조회는 해결했지만, **생성 경로 · Axis Soft Delete · axis_id NOT NULL** 은 미완.

### 1.3 왜 지금 바꾸나

- 선행 fix(0.0.2v/fix-deck-axis-visibility.md)가 응답 가시화까지 매듭지었고, **다음 단계는 생성/삭제 경로 정리**로 명확.
- 이슈 2(축 사라짐)의 최유력 후보 1·2가 본 fix의 두 결정(Axis Soft Delete + 수동 Deck 생성 경로 제거)으로 **동시 원천 봉쇄** — 별도 hot-fix보다 묶음 처리가 비용-효율적.
- Deck Product 스켈레톤(선행 fix가 생성)이 in-progress 상태로 열려있어 결정 박제 위치 확보됨.

---

## 2. 원본 대비 delta (뒤집는 지점)

### 폐기 (Deprecate)

- **선행 fix §4.2 "고아 덱 경로 보존"** — `POST /api/v1/decks` 엔드포인트 + `DeckCommandService.create()` + `Deck.of()` 팩토리 폐기. 자유 덱 미래 사용 사례 미실증 + 이슈 2 후보 2 재현 표면 잔존.
- **선행 fix §4.2 "수동 축 스코프 Deck 생성"** — `POST /learning-facade/axes/{axisId}/decks` 엔드포인트 + `LearningFacadeCommandService.createDeckUnderAxis()` + `Deck.createUnderAxis()` 팩토리 폐기. 축=덱 1:1 정책 명시 지시와 배치.
- **원본 product-learningFacade.md "LearningAxis Hard Delete 정책"** — `LearningFacade.removeAxis()`의 `axes.remove()` (orphanRemoval hard delete) 폐기. 이슈 2 최유력 원인 봉쇄.
- **선행 fix §12 Q3 유예 결정 "`createFromAxis` vs `createUnderAxis` 분리 유지"** — 명시 생성 경로 소멸로 `createUnderAxis` 팩토리 자체 폐기.

### 승격 (Promote)

- **`deck.axis_id` nullable → `NOT NULL`** — 스키마 레벨에서 "axis_id null인 Deck"이 불가능. Card 조회 로직에서 null 분기 삭제 가능.
- **`Deck.createFromAxis()`가 유일 팩토리로 승격** — 사용자 직접 진입점 0개. `LearningAxisCreatedEventHandler`가 유일 호출자.
- **선행 fix에서 스텁으로 남긴 Deck Product를 "axis 결합 정책 확정" 상태로 승격** — `product-deck.md`의 Design Decisions에 본 fix 결정 박제.

### 신설 (Introduce)

- **`LearningAxis.deleted_at DATETIME(6) NULL` 컬럼** — Flyway V14. `@SQLRestriction("deleted_at IS NULL")` + `softDelete()` 메서드.
- **`LearningAxis.softDelete()` 도메인 메서드** — 이미 삭제된 축 재삭제 시 `LEARNING_AXIS_ALREADY_DELETED` 예외.
- **`DeckCommandService.softDeleteByAxisId(Long axisId)` Application Service 메서드** — Axis softDelete 트리거 시 소속 Deck 연쇄 soft delete. 동일 트랜잭션 원자성.
- **Flyway V14** — `learning_axis.deleted_at` + 인덱스.
- **Flyway V15** — 고아 Deck soft delete 백필 + `deck.axis_id` NOT NULL 승격.
- **롤백 스크립트 R14 / R15** — 컬럼 drop, NOT NULL 해제.

### 유지 (Keep)

- **`LearningFacade.axes` `orphanRemoval=true`** — 안전망으로 유지. `axes.remove()` 호출을 코드에서 제거하므로 실질 발동 안 함.
- **FK `fk_deck_axis ON DELETE SET NULL`** — Axis Hard Delete 발생 경로 없어졌으므로 실제 발동 없음. 안전망만 유지.
- **`LearningAxisCreatedEvent` + Handler → `Deck.createFromAxis()` 이벤트 흐름** — 선행 fix 상태 그대로 유지, 이제 유일 진입점.
- **선행 fix가 만든 `DeckResponse.axisId/axisName` read-model** — 그대로 유효.
- **Deck / Card의 기존 Soft Delete 정책** — 그대로.

---

## 3. 새 목표 (To-Be)

### 3.1 최종 아키텍처 요약

```
┌─ LearningFacade BC ───────────────────────────────────┐
│  Facade(AR) ─ axes(List<LearningAxis>)                │
│                orphanRemoval=true (유지, 안전망)      │
│                deleted_at DATETIME(6) NULL  ◀ ★ 추가  │
│                @SQLRestriction("deleted_at IS NULL")  │
│                                                        │
│  Controller                                            │
│   POST /learning-facade/axes                          │
│   (axes/{axisId}/decks 제거)                          │
│                                                        │
│  Service                                               │
│   addAxis()      → save → publish AxisCreatedEvent    │
│   removeAxis()   → axis.softDelete()  ◀ ★ 변경        │
│                  → deckCommandService                 │
│                      .softDeleteByAxisId(axisId) ★신설│
│   (createDeckUnderAxis 제거)                          │
└────────────────────────────────────────────────────────┘

┌─ Deck BC ─────────────────────────────────────────────┐
│  Deck(AR)                                              │
│   axisId: Long (NOT NULL)  ◀ ★ 승격                    │
│                                                        │
│  Controller                                            │
│   (POST /decks 제거)                                   │
│   DELETE /decks/{id}, GET, PATCH ... (유지)            │
│                                                        │
│  Service                                               │
│   softDeleteByAxisId(axisId) ◀ ★ 신설                  │
│    └ axis에 속한 모든 Deck softDelete                  │
│   (create · createUnderAxis 제거)                     │
│                                                        │
│  Domain Factory                                        │
│   Deck.createFromAxis()  ✓ (유일 진입점)               │
│   (Deck.of · Deck.createUnderAxis 제거)               │
└────────────────────────────────────────────────────────┘

┌─ 이벤트 흐름 ─────────────────────────────────────────┐
│  LearningAxisCreatedEvent                              │
│    → LearningAxisCreatedEventHandler                  │
│       → Deck.createFromAxis()  (유일 Deck 생성 경로)   │
└────────────────────────────────────────────────────────┘
```

### 3.2 핵심 플로우 (변경 후)

**축 삭제 → Axis + 소속 Deck 연쇄 소프트**:
```
FE ─DELETE /learning-facade/axes/{axisId}─▶ LearningFacadeController
                                              └ Service.removeAxis(userId, axisId)
                                                  ├ facade.findAxis(axisId).softDelete()
                                                  │  (deleted_at=NOW(), row 유지)
                                                  └ deckCommandService
                                                      .softDeleteByAxisId(axisId)
                                                      └ deckRepository.findByAxisIdAndDeletedFalse()
                                                          .forEach(Deck::softDelete)
                                              → 이후 조회는 @SQLRestriction으로 자동 필터
```

**Deck 생성 — 자동 이벤트 유일 경로**:
```
FE ─POST /learning-facade/axes  {name}─▶  LearningFacadeController
                                          └ Service.addAxis()
                                              ├ facade.addAxis(name) → save
                                              └ publish LearningAxisCreatedEvent(userId, axisId, axisName)
                                                  └ LearningAxisCreatedEventHandler
                                                      ├ existsByAxisIdAndDeletedFalse(axisId)? → skip
                                                      └ Deck.createFromAxis(user, axisId, axisName)
                                                        deckRepository.save(deck)

FE ─(카드 에디터에서 "새 덱" 진입점 없음)─▶ 이미 존재하는 축의 자동 Deck을 선택.
```

**Card 생성 — axisId 항상 non-null 전제**: 스키마 `NOT NULL` 승격으로 `deck.axisId` 항상 정수. Card → Deck → Axis 관계 그래프가 완전 연결 (모든 Card가 어떤 축에 속함이 스키마 보장).

### 3.3 마이그레이션 단계 (요약)

| # | 단계 | Flyway | 도메인 변경 |
|---|---|---|---|
| 1 | LearningAxis Soft Delete 도입 | V14 (컬럼 추가 + 인덱스) | `LearningAxis` (deleted_at + @SQLRestriction + softDelete()), `LearningFacade.removeAxis()`, `DeckCommandService.softDeleteByAxisId` 신설 |
| 2 | Deck 생성 경로 단일화 + axis_id NOT NULL | V15 (고아 백필 + NOT NULL) | Controllers/Services/Factories 제거, `Deck.axisId` `@Column(nullable=false)` |
| 3 | 문서 갱신 | (마이그 없음) | ADR·DOMAIN.md·PACKAGE.md·product-deck.md·선행 fix [명세 변경 이력]·done Products |

상세 실행 절차는 §5 Fix-Story 3개 참조.

### 3.4 환경별 설정 분기

| 항목 | dev | prod |
|---|---|---|
| Flyway V14, V15 실행 | 즉시 | 즉시 (사용자 0명) |
| 신규 엔드포인트 활성화 | (제거만 있음) | 동일 |
| FE 호환 | 카드 에디터 "새 덱" 진입점 제거 필요 (별도 PR) | 동일 |
| OSIV / READ_COMMITTED | 기존 그대로 | 기존 그대로 |

---

## 4. 대안 검토 (Alternatives Considered)

### A. Deck 생성 경로 정리

**Option A — 자동 이벤트 유일 경로만 (선택)**
- 비용: 자유 덱 미래 사용 사례 등장 시 재개설 필요. 축당 1 Deck 제약 확정.
- 보상:
  - 사용자 지시("Axis=덱")와 1:1 일치.
  - 이슈 2 후보 2(임시 Deck 이탈 시 사라짐) 원천 봉쇄 — 진입점 자체 부재.
  - 도메인 표면적 대폭 축소 (엔드포인트 -2, 팩토리 -2, 서비스 메서드 -3).

**Option B — 자동 + 수동 축 스코프 병존 (선행 fix가 채택했던)**
- 장점: "축에 새 덱을 명시 추가"라는 향후 UX 유연성 보존.
- 거부 이유: 사용자 지시 "축=덱 1:1" 위배. 이슈 2 후보 2 재현 표면 잔존. 선행 fix가 유예했으나 본 fix가 폐기.

**Option C — 3개 다 유지 (@Deprecated 표기만)**
- 거부 이유: 이슈 본질(축=덱) 미해결. `Deprecated` 어노테이션은 코드 정리를 나중으로 미루는 부채. 트래픽 0명 지금이 삭제 최적기.

### B. LearningAxis 삭제 정책

**Option A — Soft Delete + Axis→Deck 연쇄 소프트 (선택)**
- 비용:
  - Flyway 마이그레이션 1건 (V14 `deleted_at` 컬럼 추가).
  - 축 조회 쿼리 전수 `deleted_at IS NULL` 필터 검증 필요 (`@SQLRestriction` 대부분 해결하나 native query 사용처 확인).
  - `topics` 자식 엔티티는 미적용 도메인 그대로 두고 축 필터로 자연 차단.
- 보상:
  - 이슈 2 후보 1 원천 봉쇄. `learning_axis` DB row 유지, 조회만 필터.
  - Card/Deck/Facade와 정책 통일 — "삭제된 축" 개념 명확.
  - 향후 축 복원 UX 여지 확보.

**Option B — Axis Hard Delete + Deck CASCADE**
- 장점: 스키마 심플. 삭제 == 삭제.
- 거부 이유: `fk_deck_axis`를 `ON DELETE CASCADE`로 변경 시 Card까지 사라짐. Card는 Soft Delete인데 부모 삭제로 조용히 사라지는 정책 불일치. 복원 불가.

**Option C — Axis 삭제 자체 금지**
- 거부 이유: 축이 자산이라도 "정말 삭제 못함"은 UX 불편. 삭제 UX 유지가 자연스러움.

### C. `deck.axis_id` 스키마 승격

**Option A — 고아 Deck을 soft delete로 아카이브 후 NOT NULL 승격 (선택)**
- 비용: 개발 DB에 axisId=null Deck 잔존 시 마이그레이션에서 조용히 soft delete됨 — 예상 동작이지만 사용자 사전 인지 필요.
- 보상:
  - "axis_id null인 Deck"이 스키마 레벨에서 불가능.
  - 기존 데이터 손실 없음(soft delete 보존).
  - Card 조회 로직에서 null 분기 삭제.

**Option B — 기존 고아 데이터 hard delete 후 NOT NULL**
- 거부 이유: 개발 단계여도 복원 불가. Soft Delete가 항상 우세.

**Option C — "미분류 축" 자동 생성 후 이관**
- 거부 이유: 로직 복잡, YAGNI. axis=deck 정책과도 맞지 않음.

### D. `createFromAxis` vs `createUnderAxis` 통합 (선행 fix Q3)

**Option A — `createUnderAxis` 폐기, `createFromAxis`만 유지 (선택)**
- 비용: 테스트 픽스처 재작성.
- 보상: 팩토리 표면적 축소. "생성 진입점 = 이벤트 핸들러"라는 아키텍처 규약이 코드에 박제.

**Option B — 두 팩토리 통합해 옵션 인자로 멱등 여부 결정**
- 거부 이유: 명시 생성 경로가 사라져 옵션 인자 자체가 무의미.

---

## 5. Fix-Epic / Fix-Story 분할

본 fix는 단일 Fix-Epic "Axis↔Deck 완전 통합" 아래 3개 Fix-Story로 구성. 순차 실행 (Story 1 → Story 2 → Story 3).

### Fix-Story 1: LearningAxis Soft Delete 전환 + Axis→Deck 연쇄

#### User Story
- As a 학습 사용자
- I want 축을 삭제하더라도 데이터가 조용히 사라지지 않고 조회에서만 필터되기를
- so that 축이 사라진 후 재진입해도 상태가 예측 가능하고 향후 복원 여지가 있다

#### 설명
`LearningAxis`에 `deleted_at` 컬럼 + `@SQLRestriction` + `softDelete()` 도메인 메서드 추가. `LearningFacade.removeAxis()`가 `axes.remove()` 대신 `axis.softDelete()`만 호출. 소속 Deck은 `DeckCommandService.softDeleteByAxisId(axisId)`로 동일 트랜잭션 내 연쇄 soft delete.

**핵심 파일/메서드**:
- `LearningAxis.java` — `@Column(name = "deleted_at") private LocalDateTime deletedAt;`, 클래스 `@SQLRestriction("deleted_at IS NULL")`, `public void softDelete()`
- `LearningFacade.java:96-99` — `axes.remove(target)` 삭제, `target.softDelete()` 만 호출
- `DeckCommandService.java` — `public void softDeleteByAxisId(Long axisId)` 신설 (`deckRepository.findByAxisIdAndDeletedFalse(axisId).forEach(Deck::softDelete)`)
- `LearningFacadeCommandService.removeAxis()` — softDelete + `deckCommandService.softDeleteByAxisId(axisId)` 순차 호출, 동일 `@Transactional`
- Flyway `V14__learning_axis_soft_delete.sql` — 컬럼 + `idx_learning_axis_deleted`
- Rollback `R14__rollback_learning_axis_soft_delete.sql` — 인덱스·컬럼 drop

**신규 ErrorCode**: `LEARNING_AXIS_ALREADY_DELETED`

#### 완료 기준 (AC)
- Given 활성 축 1개 / When `LearningFacade.removeAxis(axisId)` 호출 / Then `learning_axis.deleted_at`은 non-null, 소속 Deck 모두 `deleted=true`
- Given 활성 축 + 축에 카드 3장 / When 축 삭제 / Then GET learning-facade 응답 `axes[]`에서 미포함, GET axes/{id}/cards 응답 미포함
- *(엣지 - 이미 삭제된 축 재삭제)* Given `deleted_at != null`인 축 / When `softDelete()` / Then `LEARNING_AXIS_ALREADY_DELETED` 예외
- *(엣지 - 축이 존재하지 않음)* Given 없는 axisId / When `removeAxis` / Then `LEARNING_AXIS_NOT_FOUND` 예외 (기존)
- *(엣지 - Deck 연쇄 실패)* Given DB 오류 / When softDelete Deck 실패 / Then 전체 트랜잭션 롤백, Axis도 미삭제 상태 유지
- *(엣지 - 축의 topic 조회)* Given softDelete 축의 topic / When topic 조회 / Then 축 필터로 자연 차단 (topic 자체 정책 무변경)

#### Definition of Done
- [ ] 구현: `LearningAxis.softDelete()`, `LearningFacade.removeAxis` 흐름, `DeckCommandService.softDeleteByAxisId`, `LearningFacadeCommandService.removeAxis` 조율
- [ ] Flyway V14 + R14 롤백 스크립트
- [ ] 단위: `LearningAxisTest.softDelete_해피_deletedAt_설정`, `softDelete_이미삭제_예외`, `LearningFacadeTest.removeAxis_softDelete_axes컬렉션_안보임`, `DeckCommandServiceTest.softDeleteByAxisId_다건연쇄`
- [ ] 슬라이스: `LearningAxisRepositorySliceTest` — @SQLRestriction 필터 검증
- [ ] 통합: `AxisRemovalCascadeIntegrationTest` — Axis 삭제 후 응답 검증, DB row 유지 확인
- [ ] ErrorCode 등록: `LEARNING_AXIS_ALREADY_DELETED`
- [ ] 회귀: `AxisCardsQueryIntegrationTest`, `CardRepositoryAxisCardsSliceTest`, `ReviewQueryServiceTodayCandidatesTest` 통과

#### 스토리 포인트
2d

#### 의존성
- 선행: (없음 · 단독 착수 가능)
- 후행: Fix-Story 2 (Axis Soft Delete 완료 후 Deck 생성 경로 정리 진입)

---

### Fix-Story 2: Deck 생성 경로 단일화 + `axis_id` NOT NULL

#### User Story
- As a 학습 사용자
- I want 축을 만들면 덱이 자동 생성되고 그것만 사용하기를
- so that "축=덱" 의미가 UX와 스키마 양쪽에서 일관되고 임시 덱 사라짐 이슈가 재현되지 않는다

#### 설명
`POST /decks` · `POST /learning-facade/axes/{axisId}/decks` 두 엔드포인트 · `DeckCommandService.create/createUnderAxis` · `LearningFacadeCommandService.createDeckUnderAxis` · `Deck.of/createUnderAxis` 팩토리 모두 제거. `Deck.axisId` `@Column(nullable=false)` 승격. Flyway V15로 고아 Deck 백필 (soft delete) + `ALTER TABLE deck MODIFY axis_id BIGINT NOT NULL`.

**제거 파일/메서드**:
- `DeckController.java:25-32` (`POST /decks`)
- `DeckCommandService.java:32-42` (`create`), `56-64` (`createUnderAxis`)
- `Deck.java:116-130` (`Deck.of`), `175-194` (`Deck.createUnderAxis`)
- `LearningFacadeController.java:108-118` (`POST /axes/{axisId}/decks`)
- `LearningFacadeCommandService.java:141-149` (`createDeckUnderAxis`)

**수정 파일**:
- `Deck.java:74-84` — `@Column(name = "axis_id", nullable = false) private Long axisId;`
- `Deck.createFromAxis()` — 유일 팩토리 표지 주석 추가

**Flyway**:
- `V15__deck_axis_not_null_and_orphan_softdelete.sql`:
  ```sql
  UPDATE deck SET deleted = TRUE, deleted_at = NOW(6)
   WHERE axis_id IS NULL AND deleted = FALSE;
  ALTER TABLE deck MODIFY axis_id BIGINT NOT NULL;
  ```
- Rollback `R15__rollback_deck_axis_not_null.sql`

#### 완료 기준 (AC)
- Given 마이그레이션 전 axisId=null 활성 Deck 1건 / When V15 실행 / Then Deck `deleted=true, deleted_at != null`, ALTER 성공
- Given V15 완료 상태 / When `POST /decks` 호출 시도 / Then 404 또는 405
- Given V15 완료 상태 / When `POST /learning-facade/axes/{axisId}/decks` 시도 / Then 404 또는 405
- Given 축 생성 / When 이벤트 핸들러 실행 / Then Deck 자동 생성 성공 (기존 회귀)
- *(엣지 - axis_id NULL 저장 시도)* Given Deck 직접 저장 / When axis_id=null / Then `DataIntegrityViolationException`
- *(엣지 - 이벤트 핸들러 멱등)* Given 이미 존재하는 축 Deck / When 이벤트 재발행 / Then `existsByAxisIdAndDeletedFalse` 체크로 skip

#### Definition of Done
- [ ] 구현: Controllers/Services/Factories 제거, `Deck.axisId` nullable=false
- [ ] Flyway V15 + R15 롤백 스크립트
- [ ] 단위: `DeckTest.axisId_필수`, `LearningAxisCreatedEventHandlerTest` 회귀
- [ ] 슬라이스: Repository — axis_id NULL 저장 → `DataIntegrityViolationException`
- [ ] Controller Slice: `DeckControllerTest` — 삭제된 엔드포인트 404
- [ ] 통합: 마이그레이션 검증 — 고아 Deck 심고 V15 실행 후 `deleted=true`, ALTER 성공
- [ ] 회귀: `LearningAxisCreatedEventHandlerIntegrationTest`, Today/축뷰 관련 쿼리 통과

#### 스토리 포인트
1.5d

#### 의존성
- 선행: Fix-Story 1 (Axis Soft Delete 도입으로 Axis Hard Delete 발생 경로 소멸이 전제)
- 후행: Fix-Story 3 (Story 2까지 코드/스키마 안정 후 문서 갱신)

---

### Fix-Story 3: 문서 갱신 (ADR·DOMAIN.md·PACKAGE.md·선행 fix 명세 변경 이력)

#### User Story
- As a Claude 및 미래 협업자
- I want 본 fix의 결정이 진실 소스(ADR/DOMAIN.md/PACKAGE.md/원본 SDD)에 박제되기를
- so that 새 Story 진입 시 "축=덱 정책"·"Axis Soft Delete"를 코드에서만이 아니라 규범 문서에서도 확인 가능

#### 설명
새 ADR 발행 + DOMAIN.md/PACKAGE.md의 관련 섹션 갱신 + 원본 SDD 3건에 [명세 변경 이력] 블록 추가 + product-deck.md의 Design Decisions에 본 fix 결정 박제.

**작업 파일**:
- `docs/adr/ADR{NNN}-axis-deck-full-integration.md` — 신규
- `docs/DOMAIN.md` — Soft Delete 정책 표 · Deck BC 섹션 · LearningAxis 섹션 갱신
- `docs/PACKAGE.md` — LearningFacade → Deck 의존 두 경로(이벤트 + `softDeleteByAxisId`) 명시
- `workflow/task/pes/workspectrum/sdd/in-progress/product-deck.md` — Design Decisions 채움, 본 fix 인용
- `workflow/task/fix/sdd/version/0.0.2v/fix-deck-axis-visibility.md` — [명세 변경 이력] 추가
- `workflow/task/pes/workspectrum/sdd/done/product-learningFacade.md` — [명세 변경 이력]
- `workflow/task/pes/workspectrum/sdd/done/product-card.md` — [명세 변경 이력]

#### 완료 기준 (AC)
- Given ADR021 발행 / When ADR 인덱스 조회 / Then Option A/B/C 비교 + 선행 fix 대비 결정 번복 지점 명시
- Given DOMAIN.md 갱신 / When Soft Delete 정책 표 조회 / Then LearningAxis 포함
- Given PACKAGE.md 갱신 / When LearningFacade BC 의존 그래프 조회 / Then Deck 의존 2경로(이벤트 + softDeleteByAxisId) 명시
- Given 원본 SDD 3건 갱신 / When [명세 변경 이력] 블록 조회 / Then 본 fix 인용 라인 존재

#### Definition of Done
- [ ] ADR{NNN} 신규 발행
- [ ] DOMAIN.md · PACKAGE.md 갱신
- [ ] product-deck.md Design Decisions 채움
- [ ] 원본 SDD 3건 · 선행 fix에 [명세 변경 이력] 블록 추가

#### 스토리 포인트
0.5d

#### 의존성
- 선행: Fix-Story 2 (코드/스키마 안정 후)
- 후행: (없음 · 본 fix 종결)

---

## 6. ADR 승격 후보

- **ADR{NNN} — Axis↔Deck 완전 통합 — 자동 생성 유일화 & Axis Soft Delete** (필수 발행 · Fix-Story 3 DoD 포함)
  - 선행 ADR020(Deck↔Axis 가시화) 대비 결정 번복 지점 명시.
  - §4 Option A/B/C 4갈림길을 ADR "대안 검토" 섹션에 그대로 인용.
  - LearningAxis Soft Delete 도입 사유(이슈 2 재현 원천 봉쇄) 명시.
  - 폐기: 선행 fix §4.2 부분 유지 · §4.2 수동 축 스코프 · 원본 SDD Hard Delete.

- **ADR-후속(선택) — Soft Delete 관행 통일** — LearningAxis만 `@SQLRestriction`을 채택해 Card·Deck 관행(`boolean deleted`)과 갈렸다. 프로젝트 전체 Soft Delete 관행 통일 여부는 별도 리팩토링 시점에 결정.

---

## 7. 진실 소스 반영 계획

### 7.1 코드 반영

| 대상 | 파일 | 변경 |
|---|---|---|
| Domain | `src/main/java/.../LearningFacade/domain/model/LearningAxis.java` | `deleted_at` + `@SQLRestriction` + `softDelete()` |
| Domain | `src/main/java/.../LearningFacade/domain/model/LearningFacade.java` | `removeAxis()` 흐름 변경 (`axes.remove` → `axis.softDelete`) |
| Domain | `src/main/java/.../Deck/domain/model/Deck.java` | `axisId` `nullable=false`, 팩토리 2개 제거 |
| Application | `src/main/java/.../LearningFacade/application/service/LearningFacadeCommandService.java` | `removeAxis` 조율 · `createDeckUnderAxis` 제거 |
| Application | `src/main/java/.../Deck/application/service/DeckCommandService.java` | `softDeleteByAxisId` 신설 · `create`/`createUnderAxis` 제거 |
| Presentation | `src/main/java/.../Deck/presentation/DeckController.java` | `POST /decks` 제거 |
| Presentation | `src/main/java/.../LearningFacade/presentation/LearningFacadeController.java` | `POST /axes/{axisId}/decks` 제거 |
| ErrorCode | `src/main/java/.../Common/Exception/ErrorCode/ErrorCode.java` | `LEARNING_AXIS_ALREADY_DELETED` 등록 |

### 7.2 Flyway

- `src/main/resources/db/migration/V14__learning_axis_soft_delete.sql` — 컬럼 + 인덱스
- `src/main/resources/db/migration/V15__deck_axis_not_null_and_orphan_softdelete.sql` — 백필 + NOT NULL
- `src/main/resources/db/migration/R14__rollback_learning_axis_soft_delete.sql`
- `src/main/resources/db/migration/R15__rollback_deck_axis_not_null.sql`

### 7.3 문서 반영

| 문서 | 섹션 | 반영 내용 |
|---|---|---|
| `docs/adr/ADR{NNN}-axis-deck-full-integration.md` | 신규 파일 | §4 Option A/B/C 그대로 |
| `docs/DOMAIN.md` | Soft Delete 정책 표 | LearningAxis 추가 |
| `docs/DOMAIN.md` | §2.2 Deck | "Deck 생성 = Axis 생성 이벤트 유일" 명시 |
| `docs/DOMAIN.md` | LearningFacade 섹션 | Axis Soft Delete 정책 표기 |
| `docs/PACKAGE.md` | §6 BC 의존 | LearningFacade → Deck 두 경로 명시 |
| `workflow/.../in-progress/product-deck.md` | Design Decisions | 본 fix 인용, Deck 생성 이벤트 유일 경로 박제 |
| `workflow/task/fix/.../fix-deck-axis-visibility.md` | [명세 변경 이력] | "2026-07-01 fix-axis-deck-full-integration: §4.2 부분 유지·수동 축 스코프 폐기" |
| `workflow/.../done/product-learningFacade.md` | [명세 변경 이력] | "LearningAxis Hard→Soft Delete 정책 전환" |
| `workflow/.../done/product-card.md` | [명세 변경 이력] | "deck.axisId non-null 전제 확정" |

### 7.4 Swagger

- 제거된 엔드포인트 2개는 자동 사라짐 (Controller 삭제로 인해). 별도 Swagger 갱신 없음.

### 7.5 FE 저장소 (third-tool-fe · 별도 git)

- 카드 에디터 "새 덱 만들기" UI 진입점 제거.
- `POST /decks`, `POST /learning-facade/axes/{axisId}/decks` 호출 코드 제거.
- 축 삭제 시 소속 Deck도 함께 사라지는 UX 명시 (확인 다이얼로그 여부는 FE 판단).

---

## 8. (선택) 실패 모드 / 관측 갱신

| 시나리오 | 변경 전 | 변경 후 |
|---|---|---|
| 사용자가 축 삭제 → 새 카드 만들 때 축 인식 실패 | orphanRemoval HD로 DB row 삭제 → 조회 불가 (이슈 2 후보 1) | `deleted_at` 필터로 조회 안 됨은 동일하나 row 보존 — 복원 여지 |
| 프론트가 임시 Deck 만들고 이탈 시 sudden 사라짐 | 수동 축 스코프 생성 후 이탈 정리 시 Deck soft delete → "사라짐" (이슈 2 후보 2) | 수동 생성 진입점 자체 폐쇄 — 임시 Deck 개념 불성립 |
| axisId=null인 Deck 저장 시도 | 조용히 저장, today 미노출 | DB NOT NULL 제약 위반 → `DataIntegrityViolationException` (400 매핑) |
| 축 삭제 시 소속 Deck 처리 | FK SET NULL로 axisId만 null, Deck 유지 (고아 Deck 재발생) | 연쇄 softDelete로 Deck도 함께 사라짐 (조회에서) |
| 축 삭제된 상태에서 소속 Deck 조회 시도 | Deck의 axisId=null이라 응답에서 axisName null | `@SQLRestriction` + `deck.deleted` 필터로 목록에서 제외 |
| 마이그레이션 후 축이 없는 Deck | 그대로 유지, today 미노출 | V15 마이그레이션 시점에 `deleted=true`로 표시 (조용히 아카이브) |
| Axis softDelete 트랜잭션 중 Deck 연쇄 실패 | (경로 없음) | 전체 롤백 — Axis도 미삭제 상태 유지 (원자성 보장) |

**로깅**: `RequestLoggingFilter` 표준 유지. `LearningFacadeCommandService.removeAxis`에 `log.info("axis softDelete axisId={} affectedDecks={}", ...)` 추가 권장(트래픽 낮아 별도 metric 미도입).

---

## 9. (선택) 검증

### 통합 테스트 시나리오

- **A — 축 생성 → 자동 Deck 존재 확인**:
  - `POST /learning-facade/axes {name:"algorithms"}` → 201
  - `GET /learning-facade` → 응답 `axes[]`에 존재
  - `GET /learning-facade/axes/{axisId}/cards?status=ON_FIELD` → 카드 0건 (Deck은 자동 생성돼 있음)

- **B — 축 삭제 → 소속 Deck 연쇄 소프트**:
  - 축 생성 → 자동 Deck에 카드 1건 추가
  - `DELETE /learning-facade/axes/{axisId}` → 204
  - `GET /learning-facade` → 응답 `axes[]`에 미포함
  - `GET /decks?axisId=...` → 소속 Deck 미포함
  - DB 직접 조회: `SELECT deleted_at FROM learning_axis WHERE id=...` → non-null. `SELECT deleted FROM deck WHERE axis_id=...` → true (row 유지).

- **C — 이슈 2 재현 확인**:
  - 축 생성 → 어떤 화면 이탈 시나리오도 축을 사라지게 하지 않음
  - 실패 시 후보 3(트랜잭션)·5(EntityGraph) 재조사 트리거

- **D — Deck 생성 진입점 제거 확인**:
  - `POST /api/v1/decks {...}` → 404 또는 405
  - `POST /api/v1/learning-facade/axes/{axisId}/decks {...}` → 404 또는 405

- **E — 마이그레이션 정합**:
  - V14 이전 DB에 axisId=null Deck 1건 심고 마이그 실행
  - V15 완료 후: `deck.deleted = true, deleted_at IS NOT NULL, axis_id IS NULL(승격 전 시점)` → NOT NULL 승격 성공 확인

### 단위 / 슬라이스 (요약)

- `LearningAxisTest.softDelete_해피_deletedAt_설정` · `softDelete_이미삭제_예외`
- `LearningFacadeTest.removeAxis_softDelete_axes컬렉션_안보임`
- `DeckTest.axisId_필수`
- `DeckCommandServiceTest.softDeleteByAxisId_다건연쇄`
- `LearningFacadeCommandServiceTest.removeAxis_연쇄softDelete_트랜잭션`
- Repository slice: axis_id NULL 저장 시도 → `DataIntegrityViolationException`

### 회귀

- 선행 fix 통합 테스트 (`AxisCardsQueryIntegrationTest`, `CardRepositoryAxisCardsSliceTest`) 그대로 통과 확인.
- `ReviewQueryServiceTodayCandidatesTest` 그대로 통과.

### 롤백 절차

- V14, V15 Flyway 마이그레이션은 R14/R15 스크립트로 되돌림.
- 코드 롤백은 이전 커밋 되돌리기로 충분(비파괴 리팩토링).
- 데이터 손실 위험: soft delete는 보존이므로 없음.

---

## 10. (선택) Open Questions

1. **`LearningFacade.axes` `orphanRemoval=true` 유지 vs `false`**: 유지가 안전망이나, `axes.remove()` 호출 자체를 코드에서 삭제하면 `orphanRemoval` 발동 불가라 실질 무해. 유지 결정, Fix-Story 1 리뷰에서 재확인.
2. **`LearningAxis.topics` 자식은 Soft Delete 미적용**: 축이 softDelete되면 자식 topic 조회는 축 필터로 자연 차단. topic 자체의 Soft Delete는 별도 결정 필요. 본 fix 범위 밖.
3. **선행 fix가 유예한 §12 Q1 (AxisDeckController 위치)**: 본 fix로 컨트롤러 자체가 사라져 자동 해소.
4. **선행 fix §12 Q3 (Deck 팩토리 통합)**: 본 fix로 `createUnderAxis` 제거, `createFromAxis` 유일. 통합 이슈 자체 소멸.

---

## 11. (선택) 자가 점검

- [x] 원본 SDD에서 폐기되는 결정이 §2 폐기에 모두 명시됨 (선행 fix §4.2 두 개 + 원본 SDD Hard Delete + 팩토리 분리)
- [x] 새 결정의 트레이드오프(보상/비용)가 §4 각 갈림길에 1줄씩 명시됨
- [x] 마이그레이션 단계(§3.3 + §5 Fix-Story)가 실행 가능한 순서로 기재
- [x] 원본 명세 갱신(§7 진실 소스 반영)의 파일·섹션 매핑 명확
- [x] ADR 트리거(§6) — 새 ADR 발행 (Fix-Story 3 포함)
- [x] 이슈 2 흡수 근거가 §1.2·§8에 명시
- [x] 롤백 절차(§9)에 R 스크립트 준비 명시
- [x] FE 저장소 영향(§7.5) 별도 git이므로 명시

---

*작성일: 2026-07-01 | 기반: `issue-deck-axis-integration.md` + `issue-card-axis-recognition.md` 회의 | 상태: **완료 (2026-07-01) — 모든 Fix-Story 실행 완료 · §12 실행 결과 참조***

---

## 12. (선택) 실행 결과 (2026-07-01)

Fix-Story 3개 모두 실행 완료. 커밋 태그 `[Fix-Story-BE1-*]` / `[Fix-Story-BE1-Review-*]` / `[Fix-Story-BE2-*]`로 추적.

### Fix-Story 1: LearningAxis Soft Delete 전환 + Axis→Deck 연쇄 — ✅ 완료

- **관련 커밋**:
  - `3963044 feat(learning-facade): LearningAxis Soft Delete 도입 [Fix-Story-BE1-1]`
  - `34605ee feat(learning-facade): removeAxis를 Soft Delete로 전환 & getAxes 필터 [Fix-Story-BE1-2]`
  - `b674c6a feat(deck,facade): Axis 삭제 시 소속 Deck 연쇄 Soft Delete [Fix-Story-BE1-3]`
  - Review 반영: `fed6698` UNIQUE 재정의, `5db48ed` @SQLRestriction Slice, `7e645cb` orphanRemoval=false 재배치
- **반영 위치**: `LearningAxis.java` (@SQLRestriction + softDelete), `LearningFacade.java` (removeAxis 흐름), `DeckCommandService.softDeleteByAxisId`, `LearningFacadeCommandService.removeAxis` 조율.
- **Flyway**: V14 + R14 롤백 스크립트 작성 완료 (본 세션 마무리 시점).
- **ErrorCode**: `LEARNING_AXIS_ALREADY_DELETED` 등록.
- **테스트**: `LearningAxisTest.softDelete_*`, `LearningFacadeTest.removeAxis_softDelete_*`, `DeckCommandServiceTest.softDeleteByAxisId_*`, `AxisRemovalCascadeIntegrationTest` 통과.
- **UNIQUE 정정**: PR 리뷰 중 `(facade_id, name)` UNIQUE가 소프트 삭제 축 이름을 계속 점유해 활성 축 재생성 실패 발견 → V14에서 `(learning_facade_id, name, deleted_at)` 3-column composite로 재정의.

### Fix-Story 2: Deck 생성 경로 단일화 + `axis_id NOT NULL` 승격 — ✅ 완료

- **관련 커밋**:
  - `24da57b feat(deck): axis_id NOT NULL 승격 + Deck.of/createUnderAxis 팩토리 제거 [Fix-Story-BE2-1]`
  - `470db4a feat(deck,facade): Deck 생성 API/서비스 경로 폐지 & null 방어 코드 제거 [Fix-Story-BE2-2]`
  - `221f368 test(deck,card): 폐기된 팩토리·경로 대응 & 픽스처 마이그레이션 [Fix-Story-BE2-3]`
- **반영 위치**:
  - 엔드포인트 `POST /decks` · `POST /learning-facade/axes/{axisId}/decks` 제거 (컨트롤러 주석으로 폐기 이력 유지).
  - 팩토리 `Deck.of` · `Deck.createUnderAxis` 제거 (`Deck.createFromAxis` 유일).
  - `Deck.axisId` `@Column(nullable = false)` 승격.
- **Flyway**: V15 + R15 롤백 스크립트 작성 완료 (본 세션 마무리 시점).
- **테스트**: Repository slice — axis_id NULL 저장 시 `DataIntegrityViolationException`, Controller slice — 삭제 엔드포인트 404, 마이그레이션 검증 통합 테스트 통과.

### Fix-Story 3: 문서 갱신 — ✅ 완료

- **관련 커밋**: `7cc6980 docs(adr,domain,package): Axis↔Deck 완전 통합 결정 반영 [Fix-Story-BE2-4]`
- **반영 파일**:
  - `docs/adr/ADR021-axis-deck-full-integration.md` 신규 발행
  - `docs/DOMAIN.md` — LearningAxis Soft Delete 정책 표에 추가, §2.2 Deck 축 결합 정책 명시
  - `docs/PACKAGE.md` — LearningFacade → Deck 두 경로(이벤트 + `softDeleteByAxisId`) 문서화
  - `workflow/task/pes/workspectrum/sdd/in-progress/product-deck.md` — Design Decisions 채움 (본 세션 리팩토링에서 정착)
  - `workflow/task/pes/workspectrum/sdd/fix/fix-deck-axis-visibility.md` — §13 명세 변경 이력 표에 후행 폐기 4행 기록

### 계획 대비 편차

- **롤백 스크립트 R14 · R15 지연 반영**: 원래 Fix-Story 1·2 DoD에 포함되었으나 실제 실행 시점(2026-07-01)에는 누락되었음. 본 세션 정합화 조치로 R14 · R15 스크립트를 뒤늦게 작성해 DoD를 완결. 실질 데이터 손실은 없었음 (사용자 0명 · 트래픽 없음).
- **문서 상태 표기 정정**: 본 fix 문서 서두 상태가 "미시작"으로 표기되어 있어 실제 실행 완료 상태와 불일치했다. 본 세션에서 "완료"로 정정.

### 후속 이슈 없음

이슈 2 후보 3(트랜잭션)·5(EntityGraph)는 본 fix 완료 후 재현되지 않아 별도 hot-fix 트랙 미개시.
