# Domain Model — third-tool

> 백엔드 도메인의 **의도·용어·불변식**을 BC별로 압축 기록한다. Aggregate Root와 핵심 Entity/VO의 책임, 도메인이 보장하는 규칙, 상태 전이, 그리고 코드만 봐서는 알 수 없는 결정 메모를 담는다.
> 상세 매트릭스·필드 나열·테이블 스펙은 **본 문서에 두지 않는다** — 코드(`src/main/java/.../domain/`) + Flyway(`src/main/resources/db/migration/`) + 테스트가 단일 진실 소스다. 본 문서는 그 위의 의도 레이어다.
> Story 작업 시작 전 해당 BC 섹션을 읽고, 코드와 불일치가 보이면 사용자에게 보고한다.

---

## 목차

- [1. Ubiquitous Language — 전역 용어 (35)](#1-ubiquitous-language--전역-용어-35)
- [2. Bounded Context별 도메인 의도](#2-bounded-context별-도메인-의도)
  - [2.1 Card — 회상 가능한 학습 단위](#21-card--회상-가능한-학습-단위)
  - [2.2 Deck — 카드 분류 컨테이너](#22-deck--카드-분류-컨테이너)
  - [2.3 Review — 리뷰 세션 순회·공개 흐름](#23-review--리뷰-세션-순회공개-흐름)
  - [2.4 LearningFacade — 직업적 컨셉과 학습 축](#24-learningfacade--직업적-컨셉과-학습-축)
  - [2.5 User — 자체·소셜 인증 통합](#25-user--자체소셜-인증-통합)
  - [2.6 UserSchedule — 유저별 학습 모드 설정](#26-userschedule--유저별-학습-모드-설정)
- [3. v4 도메인 결정 사항](#3-v4-도메인-결정-사항)
- [4. 변경 이력](#4-변경-이력)

---

## 1. Ubiquitous Language — 전역 용어 (35)

BC 횡단으로 사용되는 핵심 용어. BC 내부 미세 용어는 해당 BC 섹션에서 다룬다.

| 용어 | 정의 |
| --- | --- |
| **ON_FIELD** | 전면 노출 구간. 현재 집중 학습이 필요한 카드 스테이징 공간 |
| **ARCHIVE** | 후방 대기 구간. 배경 지식으로 보관되는 카드 저장소 |
| **MainNote** | 학습 맥락 영역 (텍스트/이미지/혼합) |
| **KeywordCue** | 정답 직결 전 회상을 유도하는 단서 (최소 1개/카드) |
| **Summary** | 핵심을 자기 언어로 압축한 1~3문장 |
| **Tag** | 카드 간 관련 통로 (최대 3개/카드, 밀도 의도) |
| **ReviewSession** | 덱 내 카드 순차 순회 세션 |
| **RECALLING** | 카드 공개 단계 — Main만 노출, 회상 시도 |
| **COMPARING** | 카드 공개 단계 — Main+Keywords+Summary 함께 노출, 기억과 정답 비교 |
| **OnFieldBudget** | ON_FIELD 체류 예산 (maxView + maxDuration, 유저별 매핑) |
| **maxView** | 최대 허용 노출 횟수. 초과 시 AUTO_ARCHIVE |
| **maxDuration** | 최대 허용 체류 기간. 초과 시 AUTO_ARCHIVE |
| **enteredFieldAt** | ON_FIELD 진입 시각 (복귀 시 재기록, budget 기준) |
| **lastViewedAt** | 최후 열람 시각 (schedule 판단 기준, 복귀 시 null 초기화) |
| **viewCount** | 현 ON_FIELD 구간 노출 횟수 (복귀 시 0 초기화) |
| **SoftSchedule** | 카드 재노출 최소 간격 정책 (1/3/7/14/21일 단계) |
| **SoftScheduleState** | 카드 현재 간격 단계 상태 (FRESH / INTERVAL_* / NOT_YET) |
| **LearningFacade** | 직업적 컨셉 정의 (v1당 1개) |
| **LearningAxis** | 컨셉 아래 세부 축 (예: 데이터 모델링) |
| **AxisTopic** | 축 아래 학습 주제 (명사구, 이전 AxisAction 대체) |
| **LearningMaterial** | 학습 자료 (BOOK / COURSE / AI_CONVERSATION / WEB_RESOURCE) |
| **MaterialType** | 자료 타입. 정적(BOOK/COURSE) vs 동적(AI_CONVERSATION/WEB_RESOURCE) |
| **ProficiencyLevel** | 자료 자가 평가 숙련도 (UNRATED/UNFAMILIAR/GETTING_USED/MASTERED) |
| **CoverageStatus** | 주제의 자료 커버리지 (NO_MATERIAL / PARTIALLY_COVERED / FULLY_COVERED) |
| **CoverageSummary** | Facade 전체 커버리지 집계 (총주제수, 미커버수, hasGap) |
| **Deck** | 카드를 주제별로 계층화한 컨테이너 |
| **displayOrder** | 화면 표시 순서 (1-based, 낮을수록 우선) |
| **revisionCount** | AxisTopic 명 수정 누적 횟수 (동일 값 재입력 제외) |
| **LearningMode** | 유저 에너지 수준 (MODE_10D/20D/30D, maxView·maxDuration·간격 결정) |
| **CardStatusHistory** | 카드 상태 전환 이력 (MANUAL/MAX_VIEW/MAX_DURATION 사유) |
| **ArchiveReason** | Archive 전환 사유 구분 |
| **isLastView()** | recordView() 후 "이번이 maxView 도달인가" 판단 메서드 |
| **isScheduleAvailable()** | Card의 soft schedule 가용 여부 (schedule 필터 기준) |
| **hasUncoveredTopics()** | 미커버 주제 존재 여부 (축·Facade 경고 뱃지) |
| **isFocused()** | 주제가 상위 N개(=3) 우선순위 내인가 (집중 표시) |

---

## 2. Bounded Context별 도메인 의도

### 2.1 Card — 회상 가능한 학습 단위

**한 줄 책임**: 맥락·단서·압축을 분리해 회상 기반 학습을 모델링하고, ON_FIELD/ARCHIVE 상태와 노출 예산을 추적한다.

**Aggregate Root**: `Card` — 회상 가능한 최소 구조(Main + Keywords + Summary)를 생성·수정 시 일관되게 유지.

**Entity / VO**:
- `MainNote` (VO) — 학습 맥락 (텍스트 / 이미지 / 둘 다)
- `KeywordCue` (Entity) — 회상 단서 (최소 1개)
- `Summary` (VO) — 핵심 압축 (1~3문장)
- `CardStatus` (Enum) — `ON_FIELD` / `ARCHIVE`
- `ArchiveReason` (Enum) — `MANUAL` / `MAX_VIEW` / `MAX_DURATION`
- `CardStatusHistory` (Entity) — 상태 전환 이력
- `OnFieldBudget` (VO) — 노출 횟수·체류 기간 예산 (유저별 매핑)
- `SoftScheduleState` (Enum) — `FRESH` / `INTERVAL_1D` / `INTERVAL_3D` / `INTERVAL_7D` / `INTERVAL_14D` / `INTERVAL_21D` / `NOT_YET`
- `SoftScheduleTemplate` (VO) — 간격 단계 정의 및 상태 계산
- `Tag` (Entity) — 카드 간 관련 통로 (최대 3개/카드)
- `CardTag` (Entity) — 카드-태그 명시적 연결

**핵심 불변식**:
1. Card 생성 시 반드시 `ON_FIELD` 상태로 초기화.
2. Keywords 최소 1개 필수 — 추가·제거 후 0개로 만들 수 없음.
3. Tags 최대 3개 (밀도 높은 연결 의도).
4. Summary는 1~3문장 (짧은 압축 강제).
5. `enteredFieldAt` = ON_FIELD 진입 시각, ARCHIVE→ON_FIELD 복귀 시 재기록.
6. `viewCount` = 현재 ON_FIELD 구간의 노출 횟수, 복귀 시 0 초기화.
7. `lastViewedAt` = 최후 열람 시각, schedule 판단 기준 (복귀 시 null 초기화).
8. `recordView()` = ON_FIELD 카드만 유효 (ARCHIVE는 무시).
9. `archive()` / `returnToField()` 멱등성 보장 — 같은 상태로의 호출은 no-op.
10. `isLastView()` = `viewCount == maxView` (recordView() 이후 판단).

**상태 전이**:
```
ON_FIELD ↔ ARCHIVE
  ─archive() (사유: MANUAL / MAX_VIEW / MAX_DURATION)
  ─returnToField() (사유 기록 안 함)
```

**주의·결정 메모**:
- Keyword 변경 시 커버리지 자동 초기화 없음 — 주제 내용 변경은 tag 재점검 유저 책임.
- Card 삭제 시 CardStatusHistory 연쇄 삭제 (orphanRemoval) — 이력은 Card 생애주기에 종속.
- `recordView()` 호출 시점 = ReviewSession 안에서만 (외부 직접 접근 금지, 캡슐화).
- maxView / maxDuration은 `UserScheduleConfig.resolveOnFieldBudget()`에서 유저별 매핑.
- SoftScheduleTemplate 간격 단계는 `LearningMode`(10D/20D/30D)가 결정.

---

### 2.2 Deck — 카드 분류 컨테이너

**한 줄 책임**: 카드를 주제 단위로 계층 구조화하고, depth 일관성을 강제하며 공개 상태를 관리한다. 학습 자료 등록 흐름에서 자동 생성될 수도 있다 (Story-005-1, ADR007).

**Aggregate Root**: `Deck` — 카드 목록과 하위 덱을 계층 일관성 하에 관리.

**Entity / VO**:
- 속성 — `name`, `lastAccessed`, `onLibrary`, `publishedAt`, `parentDeck`, `depth`, `subDecks`, `cards`, `user`
- `axisId` (Long, nullable) — LearningFacade 축 연결 (Story-005-1). 자료 삭제와 무관하게 영구 보존.
- `learningMaterialId` (Long, nullable) — 원천 학습 자료 ID (Story-005-1). 자료 삭제 시 null → "자료 미연결 Deck".
- Aggregate Root 단일. 별도 VO 없음 (v1 단계).

**핵심 불변식**:
1. Deck 이름은 **동일 사용자 내** 중복 불가.
2. 최상위 덱 `depth = 0`, 하위는 `parentDeck.depth + 1` (외부 주입 금지).
3. 부모 변경 시 depth 반드시 재계산.
4. `onLibrary = false` 기본값, `publishedAt = null` 공개 전.
5. 카드 제거 시 고아 카드 자동 삭제 (orphanRemoval).
6. 부모 변경 시 순환 구조 방지 검증 필요.
7. 이름 변경 시 revisionCount 증가 없음 (revisionCount는 AxisTopic만 추적).
8. 하위 덱 및 카드 구조 삭제 시 연쇄 삭제 — depth 재계산 불필요.
9. `axisId`/`learningMaterialId`는 nullable — 사용자가 직접 만든 Deck은 둘 다 null. 자료 등록 흐름에서 만들어진 Deck은 둘 다 채워진 채로 시작 (Story-005-1).
10. 자료 삭제 시 `markMaterialDeleted()`로 `learningMaterialId`만 null 전환 (멱등). `axisId`는 로드맵 추적성을 위해 보존.

**팩토리 메서드**:
- `Deck.of(name, parentDeck, user)` — 사용자가 직접 생성 (기존 흐름).
- `Deck.createFromLearningMaterial(user, axisId, materialId, name)` — 자료 등록 이벤트 핸들러가 호출 (Story-005-1, ADR007).

**주의·결정 메모**:
- 동일 사용자 내 이름 중복 불가는 DB `uk_deck_user_name` UNIQUE 제약 + 도메인 검증 이중 방어.
- 자료 등록 흐름의 자동 생성 시 동명 Deck 존재하면 핸들러가 `forceCreateDeck=false`면 `DECK_NAME_DUPLICATE`(409)로 전체 트랜잭션 롤백, `forceCreateDeck=true`면 suffix `(2)`~`(100)` 자동 부여 (100 초과 시 `DECK_NAME_DUPLICATE`).
- Deck `axisId`·`learningMaterialId` FK는 `ON DELETE SET NULL` (Flyway V7) — 축·자료 삭제 시 Deck 자체는 보존.

---

### 2.3 Review — 리뷰 세션 순회·공개 흐름

**한 줄 책임**: 덱 내 카드를 순서대로 순회하며, RECALLING→COMPARING 단계별 공개를 제어하고 열람 기록을 갱신한다.

**Aggregate Root**: `ReviewSession` — 카드 순회 상태 + 세션 완료 플래그 관리.

**Entity / VO**:
- `ReviewStep` (Enum) — `RECALLING` (Main만 노출) / `COMPARING` (Main+Keywords+Summary 전부)
- `CardReview` (Entity) — 리뷰 내 카드 상태 (단계, 비교 시작 시각)
- `CardVisibleContent` (VO) — 현 단계에서 노출할 영역 캡슐화

**핵심 불변식**:
1. 모든 CardReview는 `RECALLING`으로 시작.
2. RECALLING → COMPARING 단방향만 허용.
3. Main은 RECALLING/COMPARING 항상 노출.
4. Keywords·Summary는 COMPARING일 때만 노출.
5. `startComparing()` 멱등성 — 이미 COMPARING이면 무시.
6. `moveToNext()` 호출은 COMPARING 상태일 때만 (RECALLING에서 호출 시 예외).
7. 마지막 카드에서 moveToNext() 시 `finished = true`.
8. `finished` 컬럼으로 저장 — cardReviews 로딩 없이 완료 판단.
9. 세션 생성 직후 + moveToNext() 이후 `recordCurrentCardView()` 호출 필수.
10. `recordCurrentCardView()` 반환값 true = maxView 소진 → 즉시 Archive 처리.

**상태 전이**:
```
세션 생성 → currentIndex=0, RECALLING
  → recordCurrentCardView()
  → startComparingCurrentCard()
  → moveToNext() (finished=true if 마지막)
```

**주의·결정 메모**:
- `CardReview.recordView()`는 ReviewSession 패키지 내부 전용 (외부 접근 금지).
- Card에 직접 접근하지 않고 ReviewSession을 통해서만 열람 기록 갱신.
- soft schedule 필터는 Application Service에서 사전 적용 — ReviewSession은 필터된 카드를 수신.
- `visibleContent()`로 노출 영역 판단 캡슐화 — 호출자가 `reviewStep`을 직접 분기하지 않는다.

---

### 2.4 LearningFacade — 직업적 컨셉과 학습 축

**한 줄 책임**: 직업 방향을 정의하고, 다중 축 아래 학습 주제를 계층화하며, 전체 커버리지를 집계한다.

**Aggregate Root**: `LearningFacade` — 축 목록 + 커버리지 집계 통합 진입점.

**Entity / VO**:
- `LearningAxis` (Entity) — 축 (예: "데이터 모델링")
- `AxisTopic` (Entity) — 주제 (명사구, 이전 AxisAction 대체)
- `LearningMaterial` (Entity) — 학습 자료
- `TopicMaterial` (Entity) — 주제-자료 다대다 연결
- `MaterialType` (Enum) — `BOOK` / `COURSE` / `AI_CONVERSATION` / `WEB_RESOURCE` + `isStatic()` / `isDynamic()`
- `ProficiencyLevel` (Enum) — `UNRATED` / `UNFAMILIAR` / `GETTING_USED` / `MASTERED`
- `CoverageStatus` (Enum) — `NO_MATERIAL` / `PARTIALLY_COVERED` / `FULLY_COVERED`
- `TopicRevision` (Entity) — 주제 명 수정 이력
- `TopicDeletionRecord` (Entity) — 주제 삭제 스냅샷 (archive 패턴, ADR003)
- `CoverageSummary` (VO) — 커버리지 집계 (총주제수, 미커버수, hasGap)
- `TopicCommand` (VO) — 다건 주제 입력
- `ConceptChangeRecord` (VO) — 컨셉 변경 결과 (changed/unchanged)

**핵심 불변식**:
1. v1 유저당 LearningFacade 1개만 (중복 생성은 Application Service에서 차단).
2. Concept 비어 있으면 안 됨.
3. 축 이름 중복 불가 (동일 Facade 내).
4. 축 삭제 시 AxisTopic 연쇄 삭제 (orphanRemoval). LearningMaterial은 보존.
5. AxisTopic 생성 시 `coverageStatus = NO_MATERIAL`.
6. 주제 명 변경 시 `revisionCount` 증가 (동일 값 재입력은 증가 안 함).
7. 주제 설명 변경 시 `revisionCount` 미증가 — 명 수정만 추적.
8. 주제 명/설명 변경 시 커버리지 자동 초기화 안 함.
9. Material 생성 시 `proficiencyLevel = UNRATED`.
10. TopicMaterial 생성 후 Application Service가 주제 커버리지 재계산.
11. 축 개수 5개 초과 시 안내 플래그 (강제 제한 아님).
12. 주제 개수 10개 초과 시 안내 플래그 (강제 제한 아님).
13. `displayOrder`는 1-based, 외부 주입 금지 — `addAxis()`/`addTopic()`가 자동 계산.

**상태 전이** (CoverageStatus):
```
NO_MATERIAL ─ TopicMaterial 연결 → PARTIALLY_COVERED ─ proficiencyLevel↑ → FULLY_COVERED
                                       └─ TopicMaterial 해제 → NO_MATERIAL
```

**주의·결정 메모**:
- AxisTopic명은 **명사구 자유 형식** — 단일 동사 강제 제거 (ADR004).
- Material 타입별 부가 속성(author, platform, aiProvider, webSource, memo)은 모두 nullable.
- 타입·부가 속성 정합성 검증 없음 — FE가 필수 표시 관리.
- `getCoverageSummary()`는 **인메모리 순회** — 별도 집계 쿼리 미도입.
- `revisionCount >= 3` 시 "단련 중" 안내 (강제 제한 아님).
- 신규 주제 저장 후 "기존 자료 연결" 옵션은 Repository 조합으로 처리 (`linkableMaterials` 응답 필드, Story-004-2).
- 주제 삭제 시 archive 패턴 — `TopicDeletionRecord` 스냅샷 보존 (ADR003: AxisTopic은 soft delete 미적용).
- **자료 등록 시 Deck 자동 생성** — `LearningMaterialCommandService.createMaterial`이 자료 저장 직후 `LearningMaterialCreatedEvent`(동기 도메인 이벤트, ADR007) 발행. Deck BC 핸들러가 같은 트랜잭션에서 동명의 Deck 생성. `linkedTopicIds`가 있으면 첫 주제의 축 ID가 Deck에 귀속, 비어있으면 axisId=null (Story-005-1).
- 자료 응답에 `deckCreated`/`deckId`/`deckName` 포함 (이벤트의 mutable 결과 통신 채널 — ADR007 설계 결정).

---

### 2.5 User — 자체·소셜 인증 통합

**한 줄 책임**: 자체 로그인과 소셜 로그인을 하나의 식별 단위로 통합하고, 소셜 연동 생명주기를 관리한다.

**Aggregate Root**: `UserEntity` — 사용자 식별 + 인증 경로 + 소셜 연동.

**Entity / VO**:
- `SocialMember` (Abstract Entity) — 소셜 제공자 연동 (제거 전용)
- `KakaoMember` / `NaverMember` (Entity) — 제공자별 구체 클래스
- `SocialProviderType` (Enum) — `KAKAO` / `NAVER`
- `UserRoleType` (Enum) — `USER` / `ADMIN`

**핵심 불변식**:
1. `username` 시스템 전역 유니크, 생성 후 변경 불가.
2. 신규 생성 계정 `isLock = false`, `roleType = USER`.
3. 소셜 로그인 사용자 password = "SOCIAL_USER" 더미값 (고정).
4. `isSocial = true`면 `socialProviderType` must be non-null.
5. `isSocial = false`면 `socialProviderType` must be null.
6. 하나의 UserEntity는 카카오/네이버 계정 **다중 연동** 가능.
7. SocialMember 삭제는 UserEntity 삭제 시만 (CascadeType.ALL, orphanRemoval).
8. `updateUser()`는 email/nickname만 수정 — 인증 필드는 변경 불가.

**주의·결정 메모**:
- 자체 로그인과 소셜 로그인이 동일 `username` 공유 불가.
- SocialMember 직접 생성 금지 — KakaoMember / NaverMember 통해서만.
- `CustomOAuth2User`는 도메인 모델 아님 (Spring Security 어댑터).
- `OAuthClient`는 Infrastructure 계층 (도메인 아님).

---

### 2.6 UserSchedule — 유저별 학습 모드 설정

**한 줄 책임**: 유저 입력값을 모드로 매핑해 카드 예산·노출 간격을 유저별로 파생하고, 설정 이력을 추적한다.

**Aggregate Root**: `UserScheduleConfig` — 매핑된 모드 보관 + 예산·간격 파생.

> **BC 디렉토리명은 `UserSchedule/`** — Aggregate Root 이름은 `UserScheduleConfig`로 구분.

**Entity / VO**:
- `LearningMode` (Enum) — `MODE_10D` (3회, 10일, 1/3/7일) / `MODE_20D` (5회, 20일, 1/3/7/14일) / `MODE_30D` (7회, 30일, 1/3/7/14/21일)
- `LearningModeMappingPolicy` (Domain Service) — 입력값→모드 매핑 규칙
- `UserScheduleConfigHistory` (Entity) — 설정 변경 이력

**핵심 불변식**:
1. 유저당 UserScheduleConfig 1개만 (1:1).
2. 미설정 유저는 기본값(MODE_10D, rawInputDays=10)으로 자동 초기화.
3. `rawInputDays` 유저 입력값 원본 보존 (매핑 후에도 유지).
4. `mappedMode`는 `LearningModeMappingPolicy.resolve()`로만 결정 (외부 주입 금지).
5. 설정 변경 시 이력 기록은 Application Service 담당 (CardStatusHistoryAppender 패턴).
6. 모드별 maxView / maxDuration / 간격 목록은 LearningMode가 정의 — 변경은 v2.
7. 매핑 분기 (v1): 1~14일 → MODE_10D, 15~24일 → MODE_20D, 25일↑ → MODE_30D.
8. **내림 매핑 원칙** — 13일 입력 → 10일 모드 (빠른 순환 습관 유리).

**주의·결정 메모**:
- maxView / maxDuration / 소프트 스케줄 간격은 모드가 유일한 결정 권자.
- 매핑 기준 분기점(14일, 24일)은 도메인 규칙 — 외부 설정값 아님.
- v1부터 SystemBudgetConfig 제거. 요청마다 configRepository에서 조회.
- 설정 변경 이력은 동일 값 재입력도 포함 — 유저 "확인" 행위 기록 가치.

---

## 3. v4 도메인 결정 사항

코드 레벨에서 추출 불가능한 의도·디폴트 결정. 새 결정은 `docs/adr/`로 합류한다.

1. **AxisTopic 명사구 표현** — AxisAction 동사 강제 제거 (ADR004). 유저 자유도 확대.
2. **revisionCount 추적** — AxisTopic 명만 추적. 설명 변경은 제외. "단련 중 안내"(>=3회) 기준.
3. **description 변경 시 커버리지 미초기화** — 설명은 보조 정보. 자료 유효성은 유저 판단.
4. **SoftScheduleTemplate 모드 결정** — LearningMode가 10D/20D/30D 간격 목록 소유. 확장은 v2.
5. **OnFieldBudget 유저별 매핑** — v1까지 시스템 단일. Epic 4부터 UserScheduleConfig로 파생.
6. **CardStatusHistory reason 필수** — ON_FIELD→ARCHIVE만 사유 기록. 복귀는 null.
7. **Card·Review 캡슐화** — ReviewSession 통해서만 열람 기록 갱신. 외부 Card 직접 접근 금지.
8. **Material 타입별 부가 속성 검증 없음** — BOOK에 platform, COURSE에 author 입력 가능. FE가 필수 표시 관리.
9. **CoverageSummary 인메모리 집계** — 별도 집계 쿼리 미도입. Aggregate Root 로드 시 온메모리 순회.
10. **UserScheduleConfigHistory 동일값 재입력 포함** — 유저 "확인" 행위도 이력으로 기록.
11. **AxisTopic 삭제는 archive 패턴** — soft delete 미적용, `TopicDeletionRecord` 스냅샷 별도 저장 (ADR003).
12. **Command/Query record 표준** — Service public 메서드는 record 단일 인자만 받음 (ADR005).

---

## 4. 변경 이력

| 버전 | 날짜 | 변경 내용 |
| --- | --- | --- |
| v0.1 | 2026-05-14 | 신설. `private-docs/domain/{도메인모델,용어사전,BC별 6개}.md`(약 8,000줄)를 단일 파일로 압축. 전역 용어 35개 + BC별 6개 섹션(책임/Aggregate Root/Entity·VO/불변식/상태전이/주의 메모) + v4 결정 12건 + 관련 ADR(003·004·005) 인용. Plan mode + Story 단위 협업 전제로 매트릭스·필드 나열을 모두 제거하고 의도 레이어만 보존 |
