# ADR020: Deck↔Axis 가시화는 read-model 노출로 한정하고 도메인 연관 승격은 거부한다

- **상태**: Accepted
- **날짜**: 2026-06-30
- **관련**: fix-deck-axis-visibility (0.0.2v) — `workflow/task/fix/sdd/version/0.0.2v/fix-deck-axis-visibility.md`, 선행 ADR007(동기 도메인 이벤트), `docs/DOMAIN.md`(Deck/Card BC), `docs/PACKAGE.md`(BC 의존 방향)

## 컨텍스트

원본 SDD(`product-learningFacade.md`)는 4-level 계층(Facade→Axis→Topic→Material)의 응집을 지키고 Deck BC와 LearningFacade BC의 결합도를 낮추기 위해, **Deck가 Axis를 raw `Long` 컬럼(`deck.axis_id`)으로만 참조**하고 JPA 연관 매핑은 두지 않도록 결정했다. 그 결과:

- Deck 응답 DTO 어디에도 `axisId`/`axisName`이 없어 FE가 "이 덱이 어느 축인가"를 표시할 수 없었다 (축별 그룹핑 UI 봉쇄).
- "내 축을 누르면 그 축의 카드가 보인다"는 사용자 의도를 표현할 조회 경로(`GET /axes/{axisId}/cards`)가 없었다.
- today 집계(Review BC)와 새로 필요한 축 카드 뷰가 같은 `axis→deck(axisId)→card` 집계를 각자 쿼리로 복제할 위험이 있었다.

카드 에디터에서 만든 덱이 고아 덱이 되어 today/축 뷰에서 "사라지는" 현상까지 겹쳐, Deck↔Axis 관계를 응답·생성·조회 3축에서 일관되게 노출할 필요가 생겼다.

## 결정

**Deck↔Axis 가시화는 Application/조회(read-model) 레이어에서만 해결**하고, **도메인 연관 승격(`deck.axisId` → `@ManyToOne LearningAxis`)은 거부**한다.

구체적으로:

- **응답 DTO 노출**: `DeckResponse.Summary`/`Detail`에 `axisId`/`axisName`을 노출하되, `axisName`은 `DeckQueryService`가 `LearningFacadeQueryService.findAxisNamesByIds`로 **배치 조회**해 동봉한다 (cross-BC read, JPA 조인 아님). QueryDSL 검색 경로(`DeckSummaryRow`)도 raw `axisId`만 투영한다.
- **단일 read-model**: 축 스코프 카드 집계를 `CardRepository.findByUserIdAndAxisIdsAndStatus(userId, axisIds, status)` **하나의 쿼리**로 모으고, 축 카드 뷰(`CardQueryService.findByAxisIds`)와 today 집계(`ReviewQueryService.collectToday`)가 공유한다. today의 최소 간격(eligibility) 재판정은 `SoftScheduleTemplate`가 인메모리로 책임지므로 read-model 쿼리는 threshold를 갖지 않는다.
- **소유권 검증 위치**: 축 스코프 엔드포인트(`POST/GET /learning-facade/axes/{axisId}/...`)는 `LearningFacadeController`에 두고, `LearningFacadeQueryService`/`CommandService`가 facade/axis 소유권을 검증한 뒤 owning BC(Deck/Card)로 위임한다.

## 결과 (Consequences)

### 긍정적

- **도메인 무변경**: `deck.axis_id`는 raw `Long` 그대로 — 4-level 응집(LearningFacade Aggregate)이 흐려지지 않고 Deck BC가 LearningAxis 엔티티를 알지 않는다.
- **드리프트 차단**: 카드 필터 규칙(삭제 제외·상태·`axisId IN`)이 단일 Repository 메서드 한 곳에 모인다. today와 축 뷰의 카드 집합 합치도가 구조적으로 보장된다.
- **사용자 의도의 API 표현**: "축에 덱을 단다"(생성)·"축을 누르면 카드가 보인다"(조회)가 경로에 명시되고, 단일 호출로 N+1을 회피한다.

### 트레이드오프 / 부정적

- **`axisName` 조인 비용**: 덱/카드 응답마다 axis 이름 cross-BC lookup 1회. N+1은 배치 조회(`findAxisNamesByIds`)로 회피하나, 도메인 조인 대비 라운드트립이 분리된다.
- **BC 의존 신규 엣지**: `LearningFacade → Card`(조회) 의존이 추가된다. 비순환(Card는 LearningFacade 미의존)이며 `PACKAGE.md`에 명시했다.
- **검색 경로 `axisName` 미동봉**: `DeckSummaryRow`는 raw `axisId`만 보유 — 이름이 필요한 소비자는 `findRootDecks`와 동일하게 서비스 레이어에서 배치 보강해야 한다.

## 대안 비교

| 대안 | 장점 | 거부 사유 |
| --- | --- | --- |
| **Option A — 응답 DTO/read-model 노출** (채택) | 도메인 무변경, BC 결합도 유지, 드리프트 단일화 | `axisName` cross-BC lookup 1회 비용 |
| Option B — 도메인 연관 승격 (`deck.axisId` → `@ManyToOne LearningAxis`) | 조인 한 번에 axisName 동봉, FK 정합성 | **BC 경계(LearningFacade↔Deck) 결합도 상승**, Deck Aggregate가 LearningAxis를 알게 되어 4-level 응집이 흐려짐 |
| Option C — facade 응답에 축별 deck/card 요약 동봉 | 추가 엔드포인트 불필요 | facade 초기 로드 비대화 — 지도 초기 로드는 가벼워야 함, 상세는 결국 별도 호출 필요 |

## 다시 검토할 시점

- **축당 기본 덱 1개 정책 도입 시점** — 현재는 다중 덱 허용. 카드 분류 UX 결정 후 도메인 제약 추가 여부 재평가 (fix 문서 §12 Q4).
- **ID 직렬화 표준화(B2) 적용 시점** — 본 ADR이 추가한 `axisId`(Long→JSON 숫자)도 표준에 편입.
- **고아 덱 경로(`POST /api/v1/decks`) 폐쇄 시점** — 사용 빈도 관측 후 (fix 문서 §12 Q2).
