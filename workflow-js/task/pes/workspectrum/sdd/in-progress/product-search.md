# [Product] 검색(Search) — 카드·자료 전문 검색 인프라

## Product Vision

> ThirdTool의 학습 자산(Card · KeywordCue · LearningMaterial · AxisTopic)이 사용자의 자연어 질의에 빠르고 정확하게 응답한다.
> 카드 수가 50개를 넘어 전체 스크롤이 무의미해진 시점부터, "그 카드 어디 있더라"의 탐색 비용을 한국어 형태소 분석 기반 전문 검색으로 흡수한다.
> 검색은 부가 기능이 아니라 학습 자산이 누적될수록 자연 필수가 되는 접근성 인프라다 — 30~50개 임계를 넘기는 사용자가 떠나지 않으려면 검색이 먼저 있어야 한다.

## 배경 및 문제

- 현재 상황 (As-Is)
    - `Card` BC는 덱 단위 전체 조회·태그 정확 일치 필터만 지원
    - 카드의 `summary`·`mainNote` 본문에 대한 전문 검색이 없다
    - `KeywordCue.value` 검색은 정확 일치만 — 부분 문자열·한국어 형태소 분석 불가
    - `LearningMaterial.name`·URL 검색이 없어, "이 주제에 자료 뭐 연결했었지"를 사용자 기억에만 의존
    - 검색 전용 BC·인덱스·동기화 파이프라인이 없음
- 발생하는 문제
    - 카드 30~50개 임계를 넘기는 사용자가 "내가 만든 그 카드"를 못 찾아 같은 카드를 다시 만들기 시작 → 학습 자산이 중복되고 SRS 단계가 분산
    - 자료 검색이 없어 LearningMaterial을 적극적으로 연결할 동기가 약함 (찾을 수 없으니 안 모음)
    - 한국어 형태소 미지원으로 "프로그래밍"을 검색해도 "프로그래머"가 매칭되지 않아 사용자 멘탈 모델과 불일치
    - 채용 포트폴리오 관점에서 "데이터가 쌓이는 서비스인데 검색이 없음"은 도메인 이해 부족 신호
    - 향후 AI 제안(`product-aisuggestion.md`)이 사용자 기존 카드와 유사도 비교를 한다고 가정하면, 그 비교의 기반 인덱스가 부재
- 왜 지금 해결해야 하는가
    - Card BC의 도메인 모델(summary·mainNote 타입·keyword 컬렉션 캡슐화)이 안정된 시점이 인덱스 설계의 베이스라인. 모델이 흔들리면 인덱스 스키마도 흔들림
    - OpenSearch 인스턴스 비용($50~100/월)은 토이 프로젝트에서 가장 큰 비용 결정 — 도입 전 ADR로 전환 기준을 못 박지 않으면 비용 통제가 흐려진다
    - Card 수가 누적되기 전에 인덱스 동기화 메커니즘을 결정해야 backfill 비용이 작다
    - 면접 단골 질문: "데이터 검색은 어떻게 합니까, 왜 OpenSearch입니까, 왜 RDS FULLTEXT가 아닙니까" — 의식적 결정으로 답할 수 있어야 한다

## 목표 (To-Be)

- 사용자가 검색창에 한국어 자연어 질의를 입력하면 200ms 이내에 카드 · 자료 · 주제가 통합 검색 결과로 반환된다
- 한국어 형태소 분석(Nori)이 적용되어 "프로그래밍" 검색이 "프로그래머"·"프로그래밍 언어"·"프로그래밍하다"를 매칭한다
- 검색 결과에 매칭 위치 하이라이팅(`<mark>` 태그)이 포함되어 사용자가 어디서 매칭됐는지 즉시 확인할 수 있다
- 카드 생성·수정·삭제가 OpenSearch 인덱스에 도메인 트랜잭션 커밋 후 5초 이내로 반영된다
- 인덱스 동기화 실패가 도메인 트랜잭션을 차단하지 않는다 (검색 인덱스는 eventually consistent)
- OpenSearch 장애 시 검색 API가 503을 반환하지만 카드 생성·수정 같은 핵심 도메인 행위는 정상 작동한다
- 검색 인덱스 부정합(MySQL과 OpenSearch 카드 수 불일치)이 운영 메트릭으로 노출된다

## 설계 결정 (Design Decision)

> **AWS OpenSearch Service를 1차 검색 백엔드로 채택한다. MySQL FULLTEXT MVP를 거치지 않고 직행한다.**
> 한국어 형태소 분석 부재가 SRS 학습 UX에 미치는 영향이 크다.
>
> - MySQL InnoDB FULLTEXT는 한국어 형태소 미지원 — `ngram` 파서는 동작하지만 정확도·랭킹이 OpenSearch Nori에 비해 현저히 낮음
> - MVP로 FULLTEXT를 거친 뒤 OpenSearch로 마이그레이션하면 인덱스 스키마·쿼리 빌더·검색 API 응답 형식을 두 번 작성해야 함 — 사용자 수 작을 때 단번에 정착이 합리적
> - OpenSearch 상시 비용($50~100/월)은 토이 단계에서 가장 큰 결정 — t3.small.search 단일 노드(약 $25/월)로 시작하고, 이후 노드 수·인스턴스 타입을 ADR로 관리
> - 이 결정은 ADR로 별도 기록한다 (`ADR-SEARCH-001: Backend Selection — OpenSearch over MySQL FULLTEXT`)

> **인덱스 동기화는 도메인 이벤트 + AFTER_COMMIT 방식이고, CDC(Debezium)는 도입하지 않는다.**
> 단일 인스턴스 모놀리스에서 CDC는 운영 부담 대비 가치 낮음.
>
> - 도메인 이벤트(`CardCreatedEvent`, `CardUpdatedEvent`, `CardDeletedEvent`, `LearningMaterialUpdatedEvent`)를 발행 → `SearchIndexer`가 `@TransactionalEventListener(phase = AFTER_COMMIT)`로 구독 → OpenSearch 색인
> - CDC는 다중 서비스·zero-downtime 마이그레이션이 필요한 시점에 도입. 현재 단계에서 Debezium + Kafka는 과함
> - 인덱스 부정합이 발생할 수 있는 시점: (1) AFTER_COMMIT 이후 OpenSearch 호출 실패, (2) 애플리케이션 재시작 중 미발송 이벤트. → 안전망: `search_reindex` 야간 배치로 전체 재색인
> - 이 결정은 ADR로 별도 기록한다 (`ADR-SEARCH-002: Sync Strategy — Domain Event + Nightly Reindex`)

> **인덱스 구조: 통합 인덱스 1개(`thirdtool_search`)에 doc type 필드로 카드/자료/주제 구분.**
> 작은 데이터 규모에서는 인덱스 분리의 운영 비용이 더 크다.
>
> - 검색 결과가 카드·자료·주제를 통합으로 보여줘야 하므로 단일 인덱스 + `docType` 필터가 가장 단순
> - 인덱스 수가 늘면 alias·rollover·매핑 동기화 비용이 누적 — v1에서 회피
> - doc 구조: `{ docType, userId, refId, title, body, keywords[], tags[], updatedAt, _highlight }`
> - userId를 모든 doc에 박아 사용자별 데이터 격리 — 검색 쿼리에 `userId` 필터 강제
> - 이 결정은 ADR로 별도 기록한다 (`ADR-SEARCH-003: Index Design — Single Index, docType Discriminator`)

> **검색 API는 사용자 BC에 종속되지 않는 `Search` 신규 BC로 분리한다.**
>
> - 검색은 여러 BC(Card·LearningFacade)의 데이터를 가로지름 → 어느 BC에도 속하지 않는 통합 검색 책임
> - Search BC는 outbound port (`SearchQueryPort`, `SearchIndexPort`) + OpenSearch 어댑터만 보유
> - 다른 BC는 Search BC를 직접 호출하지 않음 — 도메인 이벤트 발행이 유일한 통합 경로
> - 검색 API (`GET /search?q=...`)는 Search BC가 직접 노출

> **OpenSearch 연결은 Spring Data Elasticsearch가 아닌 `opensearch-java` 공식 클라이언트를 직접 사용한다.**
>
> - Spring Data Elasticsearch는 Elastic 종속이고 OpenSearch 호환성 보장 X (라이센스 분기 이후 점진 단절)
> - 인덱스 구조가 단순하고 쿼리 종류가 제한적이라 ORM 추상화 가치 낮음 — 어댑터 1개로 충분
> - 이 결정은 ADR로 별도 기록한다 (`ADR-SEARCH-004: Client Library — opensearch-java`)

## 대안 검토 (Alternatives Considered)

### 검색 백엔드

**Option A — MySQL InnoDB FULLTEXT (ngram 파서)**
- 장점: 인프라 추가 없음, 즉시 구현, 비용 0
- 거부 이유:
    - 한국어 형태소 미지원 — "프로그래밍" / "프로그래머" 같은 어간 매칭 불가
    - 랭킹 알고리즘이 단순 BM25 변형으로 키워드 가중치·필드별 boost 표현 한계
    - 하이라이팅이 약함 — `MATCH ... AGAINST`로 위치 추출 시 한계
    - 향후 OpenSearch 전환 시 응답 형식·쿼리 빌더·인덱스 동기화를 모두 다시 작성

**Option B (선택) — AWS OpenSearch Service**
- 비용: 단일 노드 $25~30/월 (t3.small.search) 또는 멀티 노드 $50~100/월. 데이터 동기화 파이프라인 필요
- 보상: 한국어 Nori 형태소 분석, 필드별 boost, 하이라이팅, 집계(facet) 자유. 검색 품질이 SRS 학습 UX에 직접 기여
- 트레이드오프 수용 근거: 토이 단계라도 검색 품질은 사용자 유지율 핵심 요소. OpenSearch 비용 절감은 인스턴스 크기·노드 수 조정으로 단계적 대응 가능

**Option C — Elasticsearch self-managed (EC2 직접 운영)**
- 거부 이유:
    - EC2 노드 백업·업그레이드·노드 교체·디스크 확장 직접 관리 → 1인 운영 부담 폭증
    - Elastic 라이센스 변경 추적 부담
    - AWS OpenSearch가 동일 기능을 매니지드로 제공하므로 운영 비용 절감 효과 미미

### 인덱스 동기화 전략

**Option A — 동기 색인 (`save()` 직후 OpenSearch 호출)**
- 거부 이유:
    - OpenSearch 응답 지연이 카드 생성 API 응답 시간에 직접 영향
    - 트랜잭션 안에서 외부 시스템 호출 시 롤백 의미가 흐려짐 (이미 색인된 doc 어떻게 되나)

**Option B (선택) — 도메인 이벤트 + AFTER_COMMIT + 야간 재색인 안전망**
- 비용: 이벤트 발행·구독 코드 + 재색인 배치 + 부정합 가능성 (메트릭으로 노출)
- 보상: 도메인 트랜잭션 격리, 검색 인덱스 장애가 핵심 행위 차단 안 함, 야간 배치로 정합성 회복

**Option C — CDC (Debezium + Kafka)**
- 거부 이유:
    - 단일 모놀리스에서 Kafka 운영 부담이 인덱스 동기화 가치 초과
    - Debezium binlog 읽기 권한·정합성 모니터링 학습 곡선 가파름
    - v2 다중 서비스 전환 시 재검토

### 인덱스 구조

**Option A — BC별 인덱스 분리 (`thirdtool_cards`, `thirdtool_materials`, ...)**
- 거부 이유:
    - 통합 검색 시 multi-index search 쿼리가 복잡해짐
    - 매핑 변경 시 인덱스 수만큼 작업 반복
    - 작은 데이터 규모에서 인덱스 분리 이득 미미

**Option B (선택) — 단일 인덱스 + docType 필드**
- 비용: doc 구조에 모든 BC의 공통 필드 합집합 필요 (NULL 허용)
- 보상: 단일 쿼리로 통합 검색, alias·매핑 관리 단순화

### 클라이언트 라이브러리

**Option A — Spring Data Elasticsearch**
- 거부 이유:
    - Elastic 종속 + OpenSearch 호환성 명시적 보장 X
    - 추상화가 쿼리 복잡도 대비 가치 낮음

**Option B (선택) — opensearch-java 공식 클라이언트**
- 비용: 쿼리 빌더 boilerplate 직접 작성
- 보상: OpenSearch 공식 지원, 버전 호환성 명확

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 배치

```
┌─────────────────────────────────────────────────────────────┐
│ Presentation                                                 │
│   SearchController (GET /search?q=...&docType=...&page=...)  │
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────────┐
│ Application                                                  │
│   SearchQueryService (사용자 격리·쿼리 빌드·결과 변환)         │
│   SearchIndexService (이벤트 구독 → 색인 위임)                │
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────────┐
│ Domain (Search BC — 작은 도메인)                              │
│   SearchQueryPort, SearchIndexPort (outbound)                │
│   SearchDocument (VO — docType / refId / title / body / ...) │
│   SearchResult (VO — 결과 + 하이라이트)                        │
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────────┐
│ Infrastructure                                               │
│   OpenSearchQueryAdapter, OpenSearchIndexAdapter             │
│     (opensearch-java 공식 클라이언트)                          │
│   CardEventListener, MaterialEventListener (AFTER_COMMIT)    │
│   SearchReindexJob (@Scheduled 02:00 야간 배치)               │
└─────────────────────────────────────────────────────────────┘
```

### 핵심 플로우

**1. 검색 쿼리 흐름**
```
GET /search?q=프로그래밍&docType=card
   │
   ▼
SearchController.search(query, userId)
   │
   ▼
SearchQueryService.search(SearchQuery(q, userId, filters))
   ├─ 입력 정규화 (trim, length ≤ 100자)
   └─ SearchQueryPort.execute(query)
                  │
                  ▼
            OpenSearchQueryAdapter
                  ├─ multi_match: title^3 + body + keywords^2
                  ├─ filter: { userId: ?, docType: ? }
                  ├─ highlight: title, body
                  └─ from/size 페이지네이션
                  │
                  ▼
            opensearch-java client
                  │
                  ▼
            OpenSearch (Nori analyzer)
                  │
                  ▼ 결과
            SearchResult VO 매핑
                  │
                  ▼
ApiResponse<SearchResultDto> (200ms 이내 목표)
```

**2. 인덱스 동기화 흐름 (카드 생성)**
```
CardCommandService.create(...)
   │
   ▼
@Transactional 안에서 Card 생성 + CardCreatedEvent 발행
   │
   ▼ COMMIT
@TransactionalEventListener(AFTER_COMMIT)
CardEventListener.handle(CardCreatedEvent)
   │
   ▼ @Async
SearchIndexService.index(SearchDocument.fromCard(...))
   │
   ▼
SearchIndexPort.upsert(doc)
   │
   ▼
OpenSearchIndexAdapter.indexAsync()
   ├─ 성공 → 메트릭 +1 (success)
   ├─ 실패 (5xx) → 재시도 큐 (in-memory, max 3)
   └─ 영구 실패 → 메트릭 +1 (failed) + ERROR 로그
```

**3. 야간 재색인 (정합성 회복)**
```
@Scheduled(cron = "0 0 2 * * *") (KST 02:00)
SearchReindexJob.run()
   ├─ MySQL Card · LearningMaterial · AxisTopic 전수 조회
   ├─ 페이지 단위(1,000건)로 OpenSearch bulk upsert
   ├─ 인덱스 vs DB 카운트 비교 → 메트릭 `search_index_drift`
   └─ 완료 통계 INFO 로그
```

### Out-of-Process 의존

- **AWS OpenSearch Service** — 단일 도메인(`thirdtool-search`), Nori 한국어 형태소 분석 플러그인 활성화
- **MySQL** — Card · LearningMaterial · AxisTopic 진실 소스 (검색 진실 소스는 MySQL, OpenSearch는 인덱스만)
- **Secrets Manager** — OpenSearch master user / endpoint 관리

### 핵심 컴포넌트

| 컴포넌트 | 위치 | 책임 |
| --- | --- | --- |
| `SearchController` | `Search/presentation/` | `GET /search` 쿼리 진입점, 입력 정규화 |
| `SearchQueryService` | `Search/application/` | 사용자 격리 + 쿼리 빌드 + 결과 변환 |
| `SearchIndexService` | `Search/application/` | 이벤트 구독 → 색인 위임 |
| `SearchQueryPort` / `SearchIndexPort` | `Search/domain/port/` | outbound port |
| `SearchDocument` / `SearchResult` | `Search/domain/model/` | 검색 doc·결과 VO |
| `OpenSearchQueryAdapter` / `OpenSearchIndexAdapter` | `Search/infrastructure/opensearch/` | opensearch-java 어댑터 |
| `CardEventListener`, `MaterialEventListener` | `Search/infrastructure/event/` | 이벤트 구독 진입점 |
| `SearchReindexJob` | `Search/infrastructure/scheduler/` | 야간 재색인 배치 |

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | ErrorCode | HTTP | 클라이언트 권장 동작 |
| --- | --- | --- | --- |
| OpenSearch 클러스터 다운 | `SEARCH_BACKEND_UNAVAILABLE` | 503 | 검색만 비활성, 카드 생성·수정은 정상 |
| 쿼리 형식 오류 (길이 > 100자) | `SEARCH_QUERY_INVALID` | 400 | 입력 축소 후 재시도 |
| 빈 쿼리 (`q=`) | `SEARCH_QUERY_BLANK` | 400 | 검색어 입력 안내 |
| 인덱스 동기화 실패 (이벤트 핸들러) | (내부) 재시도 후 ERROR 로그 | — | 사용자에 영향 없음, 야간 배치로 회복 |
| 사용자 격리 누락 (`userId` 필터 빠짐) | (보안 사고) | — | 단위 테스트 + Slice 테스트로 절대 차단 |
| 인덱스 매핑 변경 누락 | (배포 사고) | — | 매핑 변경 시 신규 인덱스 + alias swap 절차 강제 |
| OpenSearch 응답 지연 ≥ 1s | (관측 신호) | — | 메트릭 `search_query_latency_p95` 임계 알림 |
| 동기화 폭주 (이벤트 큐 적체) | (내부) `@Async` Executor 포화 | — | 메트릭 `search_index_queue_depth` 임계 알림 |

### 로깅 정책

- **항상 기록**:
    - 검색 쿼리 (INFO, `userId`·`q길이`·`docType`·`hitCount`·`durationMs`)
    - 색인 실패 (ERROR, `docType`·`refId`·`reason`, stack trace 포함)
    - 야간 재색인 결과 (INFO, `indexedCount`·`driftCount`·`durationMs`)
- **DEBUG**: OpenSearch 응답 본문 (prod 비활성)
- **절대 금지**:
    - 검색 결과 본문 그대로 (사용자 데이터 노출)
    - OpenSearch master 자격증명
    - 쿼리 원문이 PII를 포함할 가능성 → 길이만 기록, 본문은 DEBUG에서만

### 관측 지표

| 지표 | 형식 | 의미 |
| --- | --- | --- |
| `search_query_total{docType, status}` | 카운터 | 쿼리 수, status=`success`/`empty`/`error` |
| `search_query_latency_seconds{docType}` | 히스토그램 | 쿼리 응답 시간. SLO: P95 < 200ms |
| `search_index_op_total{docType, op, status}` | 카운터 | 색인 작업 수, op=`upsert`/`delete`, status=`success`/`failed` |
| `search_index_lag_seconds` | 히스토그램 | 이벤트 발행 → 색인 완료 지연. SLO: P95 < 5s |
| `search_index_queue_depth` | 게이지 | `@Async` Executor 큐 적체 |
| `search_index_drift_total` | 게이지 | MySQL vs OpenSearch 카운트 차이 (야간 배치 결과) |
| `search_backend_availability` | 게이지 | OpenSearch ping 성공 여부 (0/1) |

## 롤아웃 / 마이그레이션 (Rollout)

### 전제 — Card BC 모델 안정화 + OpenSearch 도메인 사전 프로비저닝

Card BC의 도메인 모델(summary·mainNote·keyword 컬렉션·태그)이 안정된 직후가 인덱스 매핑 설계에 가장 적합. OpenSearch 도메인은 Product 6(네트워크) · Product 7(Secrets)이 깔린 직후에 Terraform으로 프로비저닝.

### Product 의존성

- **선행 Product**:
    - `done/product-card.md` — 검색 대상 카드 모델 (안정)
    - `done/product-learningFacade.md` — 검색 대상 자료·주제 모델 (안정)
    - `in-progress/product-infra-network.md` — OpenSearch 도메인 VPC 배치 (private subnet)
    - `in-progress/product-infra-ops.md` — OpenSearch master user를 Secrets Manager에 저장
    - `in-progress/product-op.md` — 검색 메트릭 노출 인프라
- **후행 Product**:
    - `ready/product-ai-interactive-roadmap.md` — AI 제안이 사용자 기존 카드와 유사도 비교 시 OpenSearch `more_like_this` 활용
    - `in-progress/product-aisuggestion.md` — 자료 추천 시 LearningMaterial 검색 활용

### Epic·Story 의존성 그래프

```
Epic 1 (도메인 모델 + Search BC 골격)
  Story 1-1 SearchDocument · SearchResult · SearchQuery VO
  Story 1-2 SearchQueryPort · SearchIndexPort 인터페이스
  Story 1-3 ErrorCode 등록 + GlobalExceptionHandler 매핑
       │
       ▼
Epic 2 (OpenSearch 인프라 + 어댑터)
  Story 2-1 Terraform — OpenSearch 도메인 프로비저닝 (Nori 플러그인)
  Story 2-2 인덱스 매핑 정의 (`thirdtool_search`) + 부팅 시 자동 생성
  Story 2-3 opensearch-java 클라이언트 설정 + Secrets Manager 자격증명 주입
  Story 2-4 OpenSearchIndexAdapter (upsert/delete)
  Story 2-5 OpenSearchQueryAdapter (multi_match + filter + highlight)
       │
       ▼
Epic 3 (이벤트 구독 → 색인)
  Story 3-1 Card BC: CardCreatedEvent · CardUpdatedEvent · CardDeletedEvent 발행 (Card 측 Story)
  Story 3-2 LearningFacade BC: 자료·주제 이벤트 발행 (LearningFacade 측 Story)
  Story 3-3 SearchIndexService + 이벤트 리스너 (AFTER_COMMIT + @Async)
  Story 3-4 색인 실패 재시도 + 메트릭
       │
       ▼
Epic 4 (검색 API)
  Story 4-1 SearchController + 입력 정규화 + Slice 테스트
  Story 4-2 SearchQueryService + 사용자 격리 강제 + 단위 테스트
  Story 4-3 결과 DTO + 하이라이팅 응답 형식
  Story 4-4 페이지네이션 + size 제한
       │
       ▼
Epic 5 (재색인 + 운영)
  Story 5-1 SearchReindexJob (@Scheduled 02:00) + drift 메트릭
  Story 5-2 재색인 수동 트리거 API (ADMIN role)
  Story 5-3 docs/search.md + 운영 Runbook (장애 시 검색 차단·복구 절차)
```

### 환경별 설정 분기

| 항목 | dev | prod |
| --- | --- | --- |
| OpenSearch 도메인 | `thirdtool-search-dev` (t3.small.search × 1) | `thirdtool-search-prod` (t3.small.search × 1, v2에 멀티노드 검토) |
| Nori 플러그인 | 활성 | 활성 |
| 자격증명 | 로컬 secret + .gitignore | Secrets Manager |
| 색인 enable | true | true |
| `@Async` Executor 풀 크기 | 2 | 4 |
| 재색인 cron | `0 0 2 * * *` (KST) | 동일 |
| 인덱스 보존 | 단일 인덱스 갱신 | 매핑 변경 시 alias swap 절차 |

## 성공 지표 (KPI)

- 검색 쿼리 응답 시간 P95 ≤ 200ms
- 카드 생성 → 검색 결과 반영 지연 P95 ≤ 5초
- 검색 인덱스 정합성: MySQL Card 수 vs OpenSearch doc 수 차이 ≤ 0.1% (야간 배치 시점)
- 한국어 형태소 매칭 정확도 (수동 테스트 케이스 30건): 정확 매칭 ≥ 28건
- 사용자 격리 위반 (다른 userId 결과 노출) = 0건 (단위/Slice 테스트로 절대 차단)
- OpenSearch 장애 시 카드 생성·수정 API 성공률 영향 = 0% (격리 검증)

## Scope

**In Scope (v1)**:
- AWS OpenSearch Service 단일 도메인 (Nori 플러그인)
- 단일 인덱스 `thirdtool_search` + docType 필드
- 검색 대상: `card.summary`, `card.mainNote`, `card.keywords`, `card.tags`, `learning_material.name`, `axis_topic.name`
- 도메인 이벤트 기반 색인 + 야간 재색인 안전망
- 검색 API: `GET /search?q=...&docType=...&page=...`
- 사용자 격리 강제 (모든 쿼리에 `userId` 필터)
- 메트릭 노출 + 장애 시 503 격리

**Out of Scope (v1)**:
- 검색 자동완성·연관 검색 — v2
- 검색 이력 저장 / 사용자 검색 패턴 분석 — v2
- 멀티노드 OpenSearch 클러스터 — 데이터 규모 증가 시 ADR로 결정
- CDC (Debezium) 기반 동기화 — v2 다중 서비스 전환 시 검토
- 검색 권한 위임 (다른 사용자의 공개 카드 검색) — `product-social.md` 도입 후
- 이미지 OCR / 첨부 자료 본문 검색 — `product-card.md` 미디어 확장 후

## 대상 사용자

- **학습자** — 누적된 카드·자료·주제를 한국어 자연어로 빠르게 찾아 학습 흐름 유지
- **AI 제안 시스템 (후행)** — 사용자 기존 카드와의 유사도 비교 기반 인덱스로 활용
- **운영자** — 인덱스 정합성·검색 응답 시간 메트릭으로 사용자 검색 UX 품질 추적

## 연결된 Epic 목록

- [ ] Epic 1: 도메인 모델 + Search BC 골격
- [ ] Epic 2: OpenSearch 인프라 + 어댑터
- [ ] Epic 3: 이벤트 구독 → 색인
- [ ] Epic 4: 검색 API
- [ ] Epic 5: 재색인 + 운영

## 관련 문서

- 의존 Product: `done/product-card.md`, `done/product-learningFacade.md`, `in-progress/product-infra-network.md`, `in-progress/product-infra-ops.md`, `in-progress/product-op.md`
- 관련 ADR (예정): `ADR-SEARCH-001 ~ 004` (백엔드 선택 / 동기화 전략 / 인덱스 구조 / 클라이언트 라이브러리)
- DOMAIN.md 추가 예정 섹션: `Search BC` 신규 절 (작은 도메인 + outbound port 위주)
- PACKAGE.md 추가 예정 섹션: `com.example.thirdtool.Search.*` 4계층 매핑

## 열린 질문 (Open Questions)

- OpenSearch 도메인 크기를 t3.small.search 단일 노드로 시작했을 때, 카드 N건 / 동시 사용자 M명 임계에서 한계가 어디인가? (Story 2-1 완료 후 부하 테스트 product-load-test로 측정)
- 야간 재색인 배치가 다중 인스턴스 환경에서 중복 실행되지 않도록 분산 락이 필요한가? (ECS task 수 결정 시 재검토)
- 검색 쿼리 빈도 제한(rate limit)을 둘 것인가? (남용 방지 vs 정상 사용 트레이드오프, v2에서 결정)
- 인덱스 매핑 변경 시 alias swap 절차를 자동화할 것인가, 수동 Runbook으로 둘 것인가? (운영 빈도에 따라 결정)
- 검색 결과 랭킹에 사용자 행동(클릭률·복습 성공률)을 가중치로 반영할 것인가? (v2 학습 기반 랭킹 영역)
