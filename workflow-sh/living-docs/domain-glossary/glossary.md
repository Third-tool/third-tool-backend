# Domain Glossary (Living)

> **성격**: living-docs — 항상 최신본. **빠른 색인·참조용** 도메인 용어사전.
> **DOMAIN.md와의 관계**: `docs/DOMAIN.md`가 서사·의도·불변식 상세 소스. 본 문서는 **이름 → 1~3줄 정의 + 진입점** 조회용 발췌.
> **성장 방향**: 용어가 늘면 BC별 파일로 분화 (`domain-glossary/card.md`, `domain-glossary/learning-facade.md` …).
> **사용법**: 코드에서 낯선 이름 발견 → 여기서 의미 확인 → 필요시 DOMAIN.md 해당 BC 섹션으로 이동.

---

## 목차

- [1. BC별 Aggregate·Entity·VO 카탈로그](#1-bc별-aggregateentityvo-카탈로그)
- [2. Enum 카탈로그](#2-enum-카탈로그)
- [3. Domain Service · Policy 카탈로그](#3-domain-service--policy-카탈로그)
- [4. 결과 VO 패턴 카탈로그](#4-결과-vo-패턴-카탈로그)
- [5. 전역 용어 색인 (알파벳순 · 38+)](#5-전역-용어-색인-알파벳순--38)
- [6. 상태 전이 요약](#6-상태-전이-요약)
- [7. 도메인 결정 15건 (v5 · M4 반영)](#7-도메인-결정-15건-v5--m4-반영)

---

## 1. BC별 Aggregate·Entity·VO 카탈로그

### Card BC — 회상 가능한 학습 단위
| 이름 | 종류 | 한 줄 정의 |
| --- | --- | --- |
| `Card` | Aggregate Root | 회상 가능한 최소 구조 (Main + Keywords + Summary) · `axisId` NOT NULL (M4 · LT E4) + `createdMode` (M4 · CARD E3) |
| `MainNote` | VO | 학습 맥락 (텍스트/이미지/혼합) |
| `KeywordCue` | Entity | 회상 단서, 카드당 최소 1개 필수 |
| `Summary` | VO | 핵심 압축 (1~3문장 강제) |
| `Tag` | Entity | 카드 간 관련 통로. 시스템 전역 유니크 value |
| `CardTag` | Entity | 카드↔태그 명시적 연결, 카드당 최대 3개 |
| `CardStatusHistory` | Entity | 상태 전환 이력 |
| ~~`OnFieldBudget`~~ | ~~VO~~ | ~~ON_FIELD 체류 예산 (maxView + maxDuration)~~ — **M4 CARD E2에서 폐기**. `ArchiveReason.SCHEDULE_EXHAUSTED` 판정으로 대체 |
| `SoftScheduleTemplate` | VO | 간격 단계 정의 및 상태 계산 (M4 후 LearningMode 4개 값 반영: 7/14/28/60일 스케줄) |

### Deck BC — 카드 분류 컨테이너
| 이름 | 종류 | 한 줄 정의 |
| --- | --- | --- |
| `Deck` | Aggregate Root | 카드를 계층화한 컨테이너. **Axis 이벤트가 유일 생성 진입점** |

### Review BC — 리뷰 세션 순회·공개 흐름
| 이름 | 종류 | 한 줄 정의 |
| --- | --- | --- |
| `ReviewSession` | Aggregate Root | 덱 내 카드 순차 순회 세션 |
| `CardReview` | Entity | 리뷰 내 카드 상태 (단계, 비교 시작 시각) |
| `CardVisibleContent` | VO | 현 단계에서 노출할 영역 캡슐화 |

### LearningFacade BC — 직업적 컨셉과 학습 축
| 이름 | 종류 | 한 줄 정의 |
| --- | --- | --- |
| `LearningFacade` | Aggregate Root | 직업 방향 정의 (v1당 1개, concepts[] 1~5개) |
| `LearningFacadeConcept` | Entity | 다중 컨셉 값 (displayOrder 1-based) |
| `LearningLayer` | Entity | Facade 하위·Axis 상위 그룹핑. `default Uncategorized` 앵커 자동 발행 |
| `LearningAxis` | Entity | 축 (예: "데이터 모델링"). softDelete (ADR021) |
| `AxisTopic` | Entity | 주제 (명사구, 이전 AxisAction 대체) |
| `AxisRoadmapNode` | Entity | Roadmap 챕터 노드 (헌법, ADR023) |
| `AxisSelection` | Entity | Selection 컨테이너 (판례, ADR023). name UNIQUE per axis, hard delete |
| `AxisSelectionNode` | Entity | Selection 노드 (컨테이너 hard delete 정책 계승) |
| `LearningMaterial` | Entity | 학습 자료 (별도 도메인, softDelete) |
| `TopicMaterial` | Entity | 주제↔자료 M:N 매핑 |
| `TopicRevision` | Entity | 주제 명 수정 이력 |
| `RevisionReasonOption` | Entity | 수정 사유 옵션 마스터 |
| `TopicDeletionRecord` | Entity | 주제 삭제 스냅샷 (archive 패턴, ADR003) |
| `CoverageSummary` | VO | Facade 전체 커버리지 집계 (총주제·미커버·hasGap) |
| `TopicCommand` | VO | 다건 주제 입력 record |
| `ConceptChangeRecord` | VO | 컨셉 변경 결과 (changed/unchanged) |
| `ConceptsChangeRecord` | VO | 컨셉 다건 변경 결과 (added/removed/kept) |

### User BC — 자체·소셜 인증 통합
| 이름 | 종류 | 한 줄 정의 |
| --- | --- | --- |
| `UserEntity` | Aggregate Root | 사용자 식별 + 인증 경로 + 소셜 연동 |
| `SocialMember` | Abstract Entity | 소셜 제공자 연동 |
| `KakaoMember` | Entity | 카카오 소셜 계정 |
| `NaverMember` | Entity | 네이버 소셜 계정 |
| `RefreshEntity` | Entity | Refresh Token 화이트리스트 |

### UserSchedule BC — 유저별 학습 모드 설정
| 이름 | 종류 | 한 줄 정의 |
| --- | --- | --- |
| `UserScheduleConfig` | Aggregate Root | 매핑된 모드 보관 + 예산·간격 파생 |
| `UserScheduleConfigHistory` | Entity | 설정 변경 이력 |

---

## 2. Enum 카탈로그

| Enum | 값 | 소속 BC |
| --- | --- | --- |
| `CardStatus` | `ON_FIELD` / `ARCHIVE` | Card |
| `ArchiveReason` | `MANUAL` / `SCHEDULE_EXHAUSTED` / `MODE_DOWNGRADED` (**M4 CARD E2에서 재정의** · 기존 `MAX_VIEW`/`MAX_DURATION` 폐기) | Card |
| `SoftScheduleState` | `FRESH` / `INTERVAL_1D` / `INTERVAL_3D` / `INTERVAL_7D` / `INTERVAL_14D` / `INTERVAL_21D` / `NOT_YET` | Card |
| `DeckProgressStatus` | `NOT_STARTED` / `IN_PROGRESS` / `COMPLETED` | Deck |
| `ReviewStep` | `RECALLING` / `COMPARING` | Review |
| `MaterialType` | `BOOK` / `COURSE` / `AI_CONVERSATION` / `WEB_RESOURCE` (+ `isStatic()` / `isDynamic()`) | LearningFacade |
| `ProficiencyLevel` | `UNRATED` / `UNFAMILIAR` / `GETTING_USED` / `MASTERED` | LearningFacade |
| `CoverageStatus` | `NO_MATERIAL` / `PARTIALLY_COVERED` / `FULLY_COVERED` | LearningFacade |
| `SocialProviderType` | `KAKAO` / `NAVER` | User |
| `UserRoleType` | `USER` / `ADMIN` | User |
| `LearningMode` | `MODE_7D` / `MODE_14D` / `MODE_28D` / `MODE_60D` (**M4 CARD E1에서 재정의** · 기존 10D/20D/30D 폐기 · V23 기존 `MODE_10D` → `MODE_7D` 자동 마이그레이션) | UserSchedule |

**저장 방식** (ADR002): 모든 Enum은 `VARCHAR + CHECK 제약` + `@Enumerated(EnumType.STRING)`. ORDINAL 금지.

---

## 3. Domain Service · Policy 카탈로그

| 이름 | BC | 책임 |
| --- | --- | --- |
| `CardRelationFinder` | Card | 카드 간 관련 카드 계산 (Tag 기반) |
| `CardStatusHistoryAppender` | Card | 상태 전이 시 이력 append |
| ~~`CardExpiryPolicy`~~ | ~~Card~~ | ~~만료(maxView/maxDuration) 판정~~ — **M4 CARD E2에서 폐기** (`Card.hasScheduleExhausted()` 도메인 메서드로 흡수) |
| `SoftScheduleTemplate` | Card | 소프트 스케줄 상태 인메모리 계산 |
| `LearningModeMappingPolicy` | UserSchedule | 입력 일수 → LearningMode 매핑 (**M4에서 4개 값 · 60일 clamp** 재작성) |
| `UserScheduleConfigHistoryAppender` | UserSchedule | 설정 변경 이력 append |
| `CoverageRecalculator` | LearningFacade (Application) | **M4 LT E4에서 축 스코프로 재작성** (`recalculateByAxis(axisId)` · 기존 `recalculateByTopic` 대체) |
| `RoleDetector` | LearningFacade (Application) | concepts[] → role 매핑 (**M4 AS E3에서 4-role 확장** · backend-developer + planner + designer + problem-solver · 알 수 없는 concept → backend-developer fallback) |
| `SuggestionCatalogLoader` | LearningFacade (Infra) | AI 제안 카탈로그 classpath JSON 로드 (4-role JSON · M3+M4) |

---

## 4. 결과 VO 패턴 카탈로그

상태 변경 결과를 명시적으로 표현하는 VO. `isChanged()`가 `false`면 Application Service는 저장 쿼리 생략.

| VO | 어디서 반환 | 무엇을 알림 |
| --- | --- | --- |
| `ConceptChangeRecord` | `LearningFacade.updateConcept()` | 단일 컨셉 변경 여부 |
| `ConceptsChangeRecord` | `LearningFacade.updateConcepts()` | 컨셉 다건 (added/removed/kept) |
| ~~`Optional<ArchiveReason>`~~ | ~~`OnFieldBudget.resolveReason(card)`~~ | ~~만료 사유 (MAX_VIEW > MAX_DURATION 우선)~~ — **M4 CARD E2에서 폐기** (OnFieldBudget 자체 삭제) |
| `TopicDeletionRecord` | AxisTopic 삭제 시 | archive 스냅샷 (soft delete 미적용 대신) |

---

## 5. 전역 용어 색인 (알파벳순 · 38+)

BC 횡단 용어. BC 내부 미세 용어는 §1 카탈로그에.

| 용어 | 정의 |
| --- | --- |
| **ARCHIVE** | 후방 대기 구간. 배경 지식으로 보관되는 카드 저장소 |
| **ArchiveReason** | Archive 전환 사유 (**M4 재정의**: `MANUAL` / `SCHEDULE_EXHAUSTED` / `MODE_DOWNGRADED`) |
| **AxisTopic** | 축 아래 학습 주제 (명사구, 이전 AxisAction 대체) |
| **Card.axisId** | Card가 소속된 Axis의 FK (M4 LT E4에서 `card.axis_id NOT NULL` 승격 · 기존 `topic_id` 경유 폐기 · `topic_id` 컬럼은 soft-deprecate 상태) |
| **Card.createdMode** | 카드 생성 시점의 유저 LearningMode 저장 (M4 CARD E3 · 하이브리드 판정 소스). `effectiveMaxDays(userCurrentMode) = min(createdMode.days, userCurrentMode.days)` |
| **CardStatusHistory** | 카드 상태 전환 이력. reason은 ON_FIELD→ARCHIVE만 기록, 복귀는 null |
| **COMPARING** | 카드 공개 단계 — Main + Keywords + Summary 함께 노출, 기억과 정답 비교 |
| **CoverageStatus** | 주제의 자료 커버리지 (`NO_MATERIAL` / `PARTIALLY_COVERED` / `FULLY_COVERED`) |
| **CoverageSummary** | Facade 전체 커버리지 집계 (총주제수, 미커버수, hasGap) |
| **Deck** | 카드를 주제별로 계층화한 컨테이너. Axis 1개당 Deck 1개 (1:1) |
| **displayOrder** | 화면 표시 순서 (1-based, 낮을수록 우선, 외부 주입 금지) |
| **effectiveMaxDays()** | Card 하이브리드 계산 (M4 CARD E3): `min(createdMode 스케줄 일수, userCurrentMode 스케줄 일수)`. mode 다운 시 즉시 반영, 업 시 새 카드만 확장 |
| **enteredFieldAt** | ON_FIELD 진입 시각. 복귀 시 재기록, 스케줄 소진 판정 기준 |
| **hasScheduleExhausted()** | Card가 유저 현재 mode의 스케줄을 소진했는지 (M4 CARD E3 도메인 메서드). `SCHEDULE_EXHAUSTED` archive 판정에 사용 |
| **hasUncoveredTopics()** | 미커버 주제 존재 여부 판정 (축·Facade 경고 뱃지) |
| **isFocused()** | 주제가 상위 N개(=3) 우선순위 내인가 |
| ~~**isLastView()**~~ | ~~recordView() 후 "이번이 maxView 도달인가" 판정~~ — **M4 CARD E2에서 deprecated** (OnFieldBudget 폐기와 함께) |
| **isScheduleAvailable()** | Card의 soft schedule 가용 여부 (schedule 필터 기준) |
| **KeywordCue** | 정답 직결 전 회상을 유도하는 단서 (카드당 최소 1개) |
| **lastViewedAt** | 최후 열람 시각. schedule 판단 기준, 복귀 시 null 초기화 |
| **LearningAxis** | 컨셉 아래 세부 축 (Layer 소속, softDelete since ADR021) |
| **LearningFacade** | 직업적 컨셉 정의 (v1당 1개). concepts[] 1~5개 지원 |
| **LearningLayer** | Facade 하위·Axis 상위 그룹핑 계층. `default Uncategorized` 앵커 자동 발행 |
| **LearningMaterial** | 학습 자료 (BOOK / COURSE / AI_CONVERSATION / WEB_RESOURCE) |
| **LearningMode** | 유저 에너지 수준 (**M4 재정의**: `MODE_7D` / `MODE_14D` / `MODE_28D` / `MODE_60D` · 60일 이상 입력 자동 clamp) |
| **MainNote** | 학습 맥락 영역 (텍스트/이미지/혼합). RECALLING/COMPARING 항상 노출 |
| **MaterialType** | 자료 타입. 정적(BOOK/COURSE) vs 동적(AI_CONVERSATION/WEB_RESOURCE) |
| ~~**maxDuration**~~ | ~~최대 허용 체류 기간. 초과 시 AUTO_ARCHIVE~~ — **M4 CARD E2에서 폐기** (스케줄 소진 판정으로 대체) |
| ~~**maxView**~~ | ~~최대 허용 노출 횟수. 초과 시 AUTO_ARCHIVE~~ — **M4 CARD E2에서 폐기** |
| **ON_FIELD** | 전면 노출 구간. 현재 집중 학습이 필요한 카드 스테이징 공간 |
| ~~**OnFieldBudget**~~ | ~~ON_FIELD 체류 예산 (maxView + maxDuration, 유저별 매핑)~~ — **M4 CARD E2에서 완전 폐기** (VO · Repository 참조 · CardExpiryPolicy 서비스 삭제) |
| **ProficiencyLevel** | 자료 자가 평가 숙련도 (`UNRATED` / `UNFAMILIAR` / `GETTING_USED` / `MASTERED`) |
| **RECALLING** | 카드 공개 단계 — Main만 노출, 회상 시도 |
| **ReviewSession** | 덱 내 카드 순차 순회 세션 |
| **revisionCount** | AxisTopic 명 수정 누적 횟수 (동일 값 재입력 제외). `>= 3`이면 "단련 중" 안내 |
| **Roadmap** | 축의 학습 순서 청사진 ("헌법", ADR023). 챕터 outline + 각 챕터 subtree |
| **Selection** | 축 안의 구체 사례·응용 ("판례", ADR023). Roadmap 챕터에 대응되는 응용 예제 |
| **SoftSchedule** | 카드 재노출 최소 간격 정책 (1/3/7/14/21일 단계) |
| **SoftScheduleState** | 카드 현재 간격 단계 상태 (`FRESH` / `INTERVAL_*` / `NOT_YET`) |
| **Summary** | 핵심을 자기 언어로 압축한 1~3문장 |
| **Tag** | 카드 간 관련 통로 (카드당 최대 3개, 밀도 의도) |
| **viewCount** | 현 ON_FIELD 구간 노출 횟수. 복귀 시 0 초기화 |

---

## 6. 상태 전이 요약

### Card (ON_FIELD ↔ ARCHIVE) — **M4 재편**
```
ON_FIELD  ─archive(reason: MANUAL / SCHEDULE_EXHAUSTED / MODE_DOWNGRADED)─▶  ARCHIVE
ARCHIVE   ─returnToField(userCurrentMode, today)  (reason 미기록)─▶          ON_FIELD (fresh 재시작)
                                                                              │
                                                                              └─ createdMode = userCurrentMode
                                                                                 enteredFieldAt = today
```
멱등: 같은 상태로의 호출은 no-op. `returnToField()`는 fresh 재시작 (M4 CARD E3 · createdMode 갱신).

### Review (Card 단위)
```
세션 생성 → currentIndex=0, RECALLING
  → recordCurrentCardView()
  → startComparingCurrentCard()  (COMPARING)
  → moveToNext()  (finished=true if 마지막)
```
`moveToNext()`는 COMPARING 상태일 때만. RECALLING에서 호출 시 예외.

### Deck.progressStatus (Story-005-2)
```
NOT_STARTED ─markInProgress()─▶ IN_PROGRESS ─recalculateProgressStatus()─▶ COMPLETED (전부 ARCHIVE)
```
- `markInProgress()`는 `NOT_STARTED → IN_PROGRESS` 전용 마커 (멱등).
- `recalculateProgressStatus()`는 활성 Card 컬렉션 기준: 0개→NOT_STARTED / 전부 ARCHIVE→COMPLETED / 그 외→IN_PROGRESS.

### CoverageStatus (AxisTopic)
```
NO_MATERIAL ─TopicMaterial 연결─▶ PARTIALLY_COVERED ─proficiencyLevel↑─▶ FULLY_COVERED
                                          └─TopicMaterial 해제─▶ NO_MATERIAL
```

### LearningAxis Soft Delete (ADR021)
```
활성 (deleted_at IS NULL)  ─softDelete()─▶  삭제됨 (deleted_at SET)
```
- `orphanRemoval=false` (회귀 트랩 차단).
- 소속 Deck은 Application Service가 `DeckCommandService.softDeleteByAxisId`로 연쇄 처리.

---

## 7. 도메인 결정 15건 (v5 · M4 반영)

코드 레벨에서 추출 불가능한 의도·디폴트 결정. M4에서 결정 5 폐기 · 결정 13/14/15 신설.

| # | 결정 |
| --- | --- |
| 1 | **AxisTopic 명사구 표현** — AxisAction 동사 강제 제거 (ADR004). 유저 자유도 확대 |
| 2 | **revisionCount 추적** — AxisTopic 명만. 설명 변경은 제외. `>= 3`이면 "단련 중" 안내 |
| 3 | **description 변경 시 커버리지 미초기화** — 설명은 보조. 자료 유효성은 유저 판단 |
| 4 | **LearningMode 4개 값 확정** (**M4 재정의**) — MODE_7D/14D/28D/60D · 60일 상한 clamp · V23 자동 마이그레이션. v2 이후 확장 없음 (60일까지 충분) |
| 5 | ~~**OnFieldBudget 유저별 매핑**~~ — **M4 CARD E2에서 폐기**. `Card.hasScheduleExhausted(userCurrentMode, today)` 도메인 메서드로 대체. `SCHEDULE_EXHAUSTED` 사유로 archive |
| 6 | **CardStatusHistory reason 필수** — ON_FIELD→ARCHIVE만. 복귀는 null |
| 7 | **Card·Review 캡슐화** — ReviewSession 통해서만 열람 기록 갱신. Card 직접 접근 금지 |
| 8 | **Material 타입별 부가속성 검증 없음** — BOOK에 platform, COURSE에 author 입력 가능. FE가 필수 표시 관리 |
| 9 | **CoverageSummary 인메모리 집계** — 별도 집계 쿼리 미도입. Aggregate Root 로드 시 온메모리 순회. **Coverage 재계산은 M4에서 축 스코프로 이관** (`recalculateByAxis`) |
| 10 | **UserScheduleConfigHistory 동일값 재입력 포함** — 유저 "확인" 행위도 이력으로 기록 |
| 11 | **AxisTopic 삭제는 archive 패턴** — soft delete 미적용. `TopicDeletionRecord` 스냅샷 별도 저장 (ADR003) |
| 12 | **Command/Query record 표준** — Service public 메서드는 record 단일 인자만 (ADR005) |
| 13 | **Card `createdMode` 하이브리드** (**M4 신설**) — 생성 시점 mode pin. mode 다운 시 즉시 load 감소, 업 시 새 카드만 확장 (`effectiveMaxDays = min(createdMode, userCurrentMode)`) |
| 14 | **Card→Axis 직접 매핑** (**M4 신설**) — `card.axis_id NOT NULL`. `topic_id` 경유 폐기. Coverage 재계산 축 스코프 · Deck 폐기(M5) 선행 완료 |
| 15 | **AI role 4종 catalog** (**M4 신설**) — backend-developer + planner + designer + problem-solver. `RoleDetector`가 concepts에서 감지 · 알 수 없는 concept → backend-developer fallback |

---

## 8. 참조

- **원본 서사**: `docs/DOMAIN.md` — 각 BC 섹션의 책임·불변식·주의메모·결정메모 전문
- **패키지 규칙**: `docs/PACKAGE.md`
- **컨벤션**: `.claude/rules/conventions.md` §1 (도메인 객체)
- **ADR**: `docs/adr/index.md` (ADR001~ADR023)
- **관련 living-docs**:
  - `erd/erd.md` — 도메인 개체의 DB 매핑
  - `architecture-system-design/architecture.md` — 레이어·헥사고날 구조
  - `boundary-trace/boundary.md` — BC 간 흐름
  - `error-code/error-code.md` — 도메인 예외 카탈로그

*최신 갱신: 2026-07-21 · **M4 반영** — LearningMode 4값 재정의 · ArchiveReason 재정의 · OnFieldBudget 폐기 · Card.createdMode/axisId 신설 · CoverageRecalculator 축 스코프 · RoleDetector 4-role 확장 · 결정 12→15건*
