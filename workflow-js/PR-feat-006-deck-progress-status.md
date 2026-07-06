# PR: feat(deck,logging): Story-005-2 Deck progressStatus + Story-001-1 로깅 인프라

- **Base**: `develop`
- **Head**: `feat/006-deck-progress-status`
- **Commits**: 16 (develop..HEAD)
- **Diff stat**: 26 files / +1241 / -9

> GitHub UI에서 PR 생성 후 아래 본문을 그대로 붙여넣기.

---

## What

본 브랜치는 두 개의 Story를 묶어서 머지한다.

### Story-005-2 — Deck progressStatus + 자료 삭제 흐름
- `Deck`에 `DeckProgressStatus` enum (`NOT_STARTED` / `IN_PROGRESS` / `COMPLETED`) 필드 추가, `markInProgress()` / `recalculateProgressStatus()` 도메인 행위 도입.
- Card 생성·리뷰 트리거에서 `IN_PROGRESS` 자동 전이, `recalculateProgressStatus()`로 카드 활성 상태 변화 시 재계산.
- `LearningMaterialDeletedEvent` 발행 → Deck 측 핸들러가 자료 미연결 Deck을 자동 처리.
- `LearningFacadeQueryService.getFacade()`가 axis별 `linkedDecks`를 일괄 주입하도록 `DeckItem` record + 조회 경로 정비.
- Flyway `V8__deck_progress_status.sql` (컬럼 추가 + 기본값 백필).

### Story-001-1 — 로깅 인프라
- `logback-spring.xml` profile별 Appender 분기 (`local` → 컬러 평문, `dev`·`prod` → `LogstashEncoder` JSON).
- `application-local.yml` 신설 (local profile 첫 도입).
- `logstash-logback-encoder:8.0` 의존성 추가.
- MDC 화이트리스트 5키(`traceId`/`requestId`/`userId`/`method`/`path`)로 PII 노출 차단.
- prod에서 `org.hibernate.SQL=WARN` + `org.hibernate.orm.jdbc.bind=OFF` 분리 적용.
- 회귀 방지 `LogbackJsonFormatTest` 추가.
- ADR008 작성 — 3축(profile 분기 / Logstash 인코더 / MDC 화이트리스트) 결합 결정 + prod PII 차단 부속 결정 + follow-up 5건 명시.

## Why

- **Story-005-2** — Deck이 자료/카드 활동에 따라 자동으로 진행 상태를 표현해야 사용자 화면에서 Deck 카드의 의미 있는 상태 표기가 가능. 자료 삭제 시 Deck이 고아 상태로 남는 문제를 이벤트 기반으로 정리.
- **Story-001-1** — 외부 로그 수집기 도입 시 포맷 마이그레이션 비용을 사전에 제거. 동시에 dev/prod에서 `hibernate.orm.jdbc.bind: TRACE` 잠재 노출 위험을 코드 레벨로 차단. 자세한 동기·대안 비교는 `docs/adr/ADR008.md`.

## How

### 도메인 / 영속
- `Deck.markInProgress()`는 멱등 — 이미 `IN_PROGRESS`이면 no-op (상태 전환 멱등성 컨벤션 준수).
- `recalculateProgressStatus(activeCardCount)`는 활성 카드 0 → `NOT_STARTED`로 복귀, 그 외에는 현재 상태 유지/승급 결정만 수행해 외부 주입 없이 도메인 안에서 일관성 확보.
- `LearningMaterialDeletedEventHandler`는 Deck BC에 위치 — Facade BC가 Deck을 직접 호출하지 않도록 이벤트 경계 유지.

### Application / Query
- `LearningFacadeQueryService.getFacade()`가 axis 묶음 단위로 `deckRepository.findByAxisIdIn(...)`을 한 번 호출 → axis별 `linkedDecks` 일괄 주입 (N+1 제거).
- `DeckQueryService.findByLearningMaterialId(...)` 신설 — 자료 삭제 이벤트 핸들러 입구.

### 로깅
- `<springProfile>` 단일 파일 분기 — 환경별 별도 logback 파일 분리는 drift 위험으로 거부 (ADR008).
- LogstashEncoder의 `customFields`로 `application=thirdtool` 고정, MDC는 `includeMdcKeyName` 화이트리스트로 5개만 노출.
- `application.yml`의 공통 `logging.level.*` 블록은 제거하고 `logback-spring.xml`이 단일 진실 소스.

## Tradeoff

- **Story-005-2**: `linkedDecks` 일괄 주입을 위해 facade 조회 시 Deck Repository 한 번을 추가 호출. axis가 1~5개로 작아 비용 무시 가능 — 카드별 N+1 회피가 더 중요.
- **Story-001-1**:
  - 평문 대비 JSON 로그 라인 길이 1.5~2배 증가 (dev/prod 디스크·전송 비용 미세 증가).
  - 통합 테스트(`LogbackJsonFormatTest`)가 Spring Boot 내부 `LogbackLoggingSystem`에 의존 — Spring Boot 메이저 업그레이드 시 API 변경 위험.
  - CI(`dev-cicd.yml`)가 `-x test`라 본 회귀 방지 테스트가 CI에서 실행되지 않음 (follow-up 명시).
  - 새 MDC 키 추가는 ADR 수정 + `logback-spring.xml` 갱신 필요.

## Reviewer 종합 (review.md §4 결과)

- 5관점 병렬 발사. Story-001-1에서 `fix(logging): Reviewer 5관점 지적 보강 [Story-001-1]` 단일 commit으로 Critical 0건 / Major 지적 흡수 완료.
- Story-005-2는 도메인 캡슐화·BC 의존 방향·테스트 매트릭스(해피/엣지/예외 3구분) 통과.

## Test

- [x] `DeckTest` — `DeckProgressStatus` 전환·재계산 단위 테스트 10건.
- [x] `DeckRepositoryTest` — `findByAxisIdIn` / `findByLearningMaterialId` Slice 테스트 5건.
- [x] `CardCommandServiceDeckProgressTriggerTest` — Card 활성 변화 시 Deck 트리거 호출 검증.
- [x] `LearningMaterialDeletedEventHandlerTest` — 이벤트 수신 시 미연결 Deck 처리 검증.
- [x] `LearningFacadeQueryServiceTest` — `linkedDecks` 일괄 주입 검증.
- [x] `LearningMaterialCommandServiceDeleteTest` — 자료 삭제 시 이벤트 발행 검증.
- [x] `LogbackJsonFormatTest` — local 평문 / dev·prod JSON 포맷 + MDC 화이트리스트 회귀 방지.
- [ ] CI 통과 (CI는 `-x test`로 테스트 스킵 — 본 PR의 테스트는 로컬 검증 기반. ADR008 follow-up에 CI 활성화 명시).

## Checklist

- [x] `docs/DOMAIN.md` — Deck 섹션에 `progressStatus` 의미·전이 규칙 반영.
- [x] `docs/PACKAGE.md` — Facade → Deck 이벤트 의존 추가 행 반영.
- [x] `docs/adr/ADR008.md` 신규 작성, `docs/adr/index.md` 갱신.
- [x] BC 의존 방향 위반 없음 — Facade → Deck은 이벤트 경계로만.
- [x] OSIV=false, READ_COMMITTED 등 프로젝트 원칙 준수.
- [x] ErrorCode 신규 등록 없음 (자동 상태 전이라 사용자 노출 예외 없음).
- [x] Flyway: V8 신규 파일만 추가, 기존 V 파일 무수정.

## Commits in this PR

### Story-005-2 (Deck progressStatus)
- `545b81a` feat(deck): Flyway V8 progress_status 컬럼 추가
- `eafd641` feat(deck): DeckProgressStatus enum + progressStatus 필드 + markInProgress/recalculateProgressStatus
- `dcc6bac` test(deck): DeckProgressStatus 전환·재계산 단위 테스트 10건 추가
- `bf0ebbf` feat(deck): findByAxisIdIn + findByLearningMaterialId 쿼리 + Slice 테스트 5건
- `5b33ea4` feat(card,review): Deck progressStatus 자동 갱신 트리거 통합
- `194c03e` feat(facade): AxisItem.linkedDecks + DeckItem record
- `dea3dfa` feat(facade): getFacade에 axis별 linkedDecks 일괄 주입
- `24fc2d3` feat(facade,deck): LearningMaterialDeletedEvent + 자료 미연결 자동 처리
- `c904d23` test(card): Deck 진행 상태 트리거 호출 검증
- `3024394` docs: Story-005-2 Deck progressStatus + 자료 삭제 흐름 + BC 의존 갱신

### Story-001-1 (로깅 인프라 — 이번에 push된 6커밋)
- `880ce84` chore(logging): logstash-logback-encoder 8.0 의존성 추가
- `800c327` feat(logging): logback-spring.xml profile별 Appender 분기 추가
- `599f1ed` feat(config): local profile 신설
- `ef936c0` test(logging): profile별 로그 포맷 검증 테스트 추가
- `d1bbe39` docs(adr): ADR008 로깅 인프라 결정 기록
- `63e2ae5` fix(logging): Reviewer 5관점 지적 보강
