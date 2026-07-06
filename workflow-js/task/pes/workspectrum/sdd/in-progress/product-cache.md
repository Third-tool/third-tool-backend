# [Product] 캐시 레이어 — Caffeine MVP → ElastiCache 단계적 전환

## Product Vision

> ThirdTool의 반복 조회 부하(LearningFacade 전체 계층, JWT 블랙리스트, 향후 LLM 응답)를 캐시 레이어 하나로 흡수한다.
> 단일 ECS task 단계에서는 인프로세스 Caffeine으로 비용 0에서 시작하고, LLM 도입 + 다중 인스턴스 전환이 일어나는 시점에 ElastiCache(Redis)로 점진 전환할 수 있도록 캐시 추상화를 미리 깔아둔다.
> 캐시는 "지금 필요한 만큼만, 한 곳에서" — 캐시 인프라가 사용자 수보다 먼저 부풀어 비용을 잠식하지 않도록 ADR로 전환 기준을 명시한다.

## 배경 및 문제

- 현재 상황 (As-Is)
    - `LearningFacade` 전체 계층 조회(Facade → Axis → Topic → Material)가 매 요청마다 JOIN + N+1 잠재 부하 — 카드 수 증가 시 응답 지연 폭증 가능
    - JWT 블랙리스트 저장소가 미명세 — `product-auth.md`가 in-memory 또는 DB 중 어디로 갔는지 사후 확인 필요
    - LLM 어댑터(`product-aisuggestion.md` Epic 3)가 활성화되면 동일 프롬프트 반복 요청 비용이 사용자 수에 비례 — 캐시 없이는 호출당 직접 비용 발생
    - 캐시 추상화(`CachePort`)가 없어 향후 백엔드 전환 시 호출부 전체를 수정해야 함
- 발생하는 문제
    - 단일 인스턴스에서도 LearningFacade 조회 응답 시간이 데이터 누적과 함께 선형 증가 — 사용자 체감 지연으로 직결
    - 다중 ECS task 전환 시 in-memory JWT 블랙리스트가 인스턴스 간 불일치 → 토큰 무효화 누락이라는 보안 사고로 직결
    - LLM 호출 비용이 캐시 없이 누적되면 사용자 N명 미만에서도 월 비용이 인프라 비용 상한을 위협
    - 캐시 추상화 부재로 백엔드 결정을 뒤로 미룬다는 것은 결정 자체를 회피하는 것 — 코드 곳곳에 캐시가 박혀버리면 회수 불가
- 왜 지금 해결해야 하는가
    - LLM 어댑터 활성화 시점(`product-aisuggestion.md` Epic 3) 직전이 캐시 인프라 결정의 마지노선. 활성화 후엔 호출 비용이 즉시 누적
    - JWT 블랙리스트 위치는 ECS task 수와 직결 — `product-infra-deploy.md`의 task 수 결정(M1 후 안정화)과 같이 가야 함
    - 캐시 추상화는 한 번 깔리면 향후 백엔드 전환의 가역성이 매우 높아짐 — 작을 때 깔아두는 것이 가장 싸다
    - 면접 관점: "왜 Redis가 아닌가, 언제 Redis로 가는가" — ADR이 답이 된다

## 목표 (To-Be)

- 모든 캐시 사용처가 `CachePort` 인터페이스 한 곳을 통해서만 캐시에 접근하고, 백엔드(Caffeine ↔ ElastiCache)는 프로필 설정으로 교체 가능
- 단일 ECS task 단계에서 LearningFacade 전체 계층 조회·JWT 블랙리스트가 Caffeine 인프로세스 캐시로 처리된다
- LLM 어댑터 활성화 시 동일 프롬프트 입력 → 캐시 hit 시 LLM 호출 생략, miss 시에만 LLM 호출 + 캐시 저장
- ElastiCache 전환은 단일 ADR 결정으로 진행 가능 — 캐시 사용처는 코드 변경 없음
- 캐시 hit/miss 비율·메모리 사용량·TTL 만료 비율이 운영 메트릭으로 노출된다
- 다중 인스턴스 환경에서 캐시 일관성 위반(블랙리스트 누락·stale facade)이 0건

## 설계 결정 (Design Decision)

> **v1은 Caffeine 인프로세스 캐시로 시작한다. ElastiCache 전환은 단일 ADR로 트리거한다.**
> 단일 ECS task + 사용자 수 작은 단계에서 ElastiCache 상시 비용($15~30/월)은 정당화 불가.
>
> - Caffeine은 별도 인프라 추가 0, JVM 힙 안에서 LRU + TTL 동작
> - 전환 기준 ADR 명시: (1) ECS task 수 2개 이상 결정 시, (2) LLM 어댑터 활성화 + 월 호출량 임계 초과 시, (3) JWT 블랙리스트 분산 일관성 요구 발생 시
> - 이 결정은 ADR로 별도 기록한다 (`ADR-CACHE-001: Backend Selection — Caffeine First, ElastiCache Trigger`)

> **CachePort 추상화를 v1부터 강제한다. 호출부는 Caffeine을 직접 import하지 않는다.**
> 추상화 비용을 미리 지불해 전환 비용을 회수.
>
> - `CachePort` 인터페이스: `get(key, type) / put(key, value, ttl) / evict(key) / evictByPrefix(prefix)`
> - 어댑터 2종: `CaffeineCacheAdapter` (v1 기본), `RedisCacheAdapter` (v2, 동일 인터페이스)
> - Spring `@ConditionalOnProperty(name = "thirdtool.cache.backend", havingValue = "caffeine")`로 빈 등록 분기
> - 이 결정은 ADR로 별도 기록한다 (`ADR-CACHE-002: Port Abstraction — Single Interface, Two Adapters`)

> **캐시 영역(region) 단위로 TTL·최대 크기·정책을 분리한다.**
>
> - Region 정의:
>     - `learning-facade` — TTL 5분, max 1,000 entry (사용자별 1 entry 가정)
>     - `jwt-blacklist` — TTL = 토큰 만료 시각, max 10,000 entry
>     - `llm-response` — TTL 24시간, max 5,000 entry (프롬프트 해시 키)
>     - `axis-topic-tree` — TTL 10분, max 500 entry
> - 영역별 통계 노출 (hit/miss/evict 카운터)
> - 영역 추가 시 ADR 없이 코드 변경만 — 영역 정의는 도메인 결정에 가까움

> **JWT 블랙리스트는 v1 Caffeine, ECS task 2개 결정 직전에 강제 ElastiCache 전환.**
> 보안 일관성은 캐시 영역 중 가장 민감.
>
> - 다중 task에서 in-memory 블랙리스트는 토큰 무효화 누락 → 사고로 직결
> - `product-infra-deploy.md` task 수 결정(Story 2-2 ECS Task Definition + Service)이 ADR-CACHE-001 trigger와 연동
> - v1 단일 task에서는 Caffeine으로도 일관성 보장됨

> **LLM 응답 캐시는 프롬프트 해시 키로 정확 매칭만. 의미 기반 유사도 캐시는 v2.**
>
> - 입력 프롬프트의 SHA-256 해시를 key로 사용
> - 동일 프롬프트 반복 호출만 hit — "비슷한 질문" 매칭은 LLM 응답 의미 보존 보장이 어려워 v1 범위 외
> - 사용자별 격리: key prefix에 `userId` 포함

## 대안 검토 (Alternatives Considered)

### 캐시 백엔드

**Option A — Amazon ElastiCache Serverless (Redis)**
- 장점: 인프라 관리 최소, 사용량 비례 과금
- 거부 이유:
    - Serverless 단가가 provisioned보다 높음 ($50+/월부터 시작)
    - 사용량 낮은 단계에서는 상시 비용이 ROI를 떨어뜨림
    - 향후 트래픽 폭증 시 재선택 후보로 보존

**Option B — Amazon ElastiCache Provisioned (t3.micro)**
- 장점: 비용 예측 가능 ($15~30/월)
- 거부 이유:
    - 미사용 시간 낭비 발생 — 토이 단계에서 24/7 상시 비용은 부담
    - v2 다중 인스턴스 전환 시 첫 선택지로 채택 예정

**Option C (선택) — Caffeine 인프로세스 캐시**
- 비용: 다중 인스턴스 전환 시 일관성 보장 불가 → 전환 강제
- 보상: 인프라 추가 비용 0. 추상화만 깔리면 전환 가역성 보장
- 트레이드오프 수용 근거: 단일 task 단계에서 가장 합리적. 전환 기준 ADR로 외부화

### 추상화 시점

**Option A — 캐시 추상화 없이 Caffeine 직접 사용**
- 거부 이유:
    - 전환 시점에 호출부 전체 수정 필요 — 가역성 손실
    - 캐시 사용처가 코드 곳곳에 흩어지면 회수 불가

**Option B (선택) — v1부터 CachePort 추상화**
- 비용: 추가 인터페이스·어댑터 코드
- 보상: 전환 시 코드 변경 = 어댑터 빈 등록 분기 1곳

### 캐시 영역 관리

**Option A — 전역 단일 캐시**
- 거부 이유:
    - TTL·max size 한 값으로 모든 사용처 커버 불가
    - 한 영역의 폭주가 다른 영역을 밀어냄 (eviction 오염)

**Option B (선택) — Region 단위 분리**
- 비용: 영역별 설정 관리 부담
- 보상: 영역별 통계·정책 독립

### LLM 응답 캐시 매칭 방식

**Option A — 의미 기반 유사도 (embedding 비교)**
- 거부 이유:
    - 응답 보존 보장 어려움 — 유사 입력의 응답이 항상 같다고 가정 못함
    - embedding 계산 비용이 캐시 절감 효과를 상쇄
    - v2 학습 기반 평가 후 도입 검토

**Option B (선택) — 프롬프트 해시 정확 매칭**
- 비용: 동일 의미·다른 표현은 hit 못함
- 보상: 응답 보존 명확, 구현 단순

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 배치

```
┌─────────────────────────────────────────────────────────────┐
│ Application (캐시 사용자)                                     │
│   LearningFacadeQueryService                                 │
│   JwtBlacklistService (Common/security/auth)                │
│   AiSuggestionService (product-aisuggestion Epic 3)         │
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────────┐
│ Domain (Cache BC — 작은 도메인)                              │
│   CachePort (outbound)                                       │
│   CacheRegion enum (LEARNING_FACADE/JWT_BLACKLIST/...)       │
│   CacheKey (VO — region + key + version)                     │
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────────┐
│ Infrastructure                                               │
│   CaffeineCacheAdapter (v1 기본)                             │
│   RedisCacheAdapter (v2, 빈 등록만 분기)                      │
│   CacheMetricsCollector (region별 hit/miss/evict)            │
└─────────────────────────────────────────────────────────────┘
```

### 핵심 플로우

**1. 캐시 hit/miss 경로 (LearningFacade 조회)**
```
LearningFacadeQueryService.getFacadeForUser(userId)
   │
   ▼
CachePort.get(CacheKey.of(LEARNING_FACADE, userId), Facade.class)
   ├─ hit  → 즉시 반환 (메트릭: hit +1)
   └─ miss → JPA 조회 → CachePort.put(...) → 반환 (메트릭: miss +1)
```

**2. 캐시 무효화 (LearningFacade 수정)**
```
LearningFacadeCommandService.updateAxis(...)
   │
   ▼ 트랜잭션 COMMIT 후
@TransactionalEventListener(AFTER_COMMIT)
   │
   ▼
CachePort.evict(CacheKey.of(LEARNING_FACADE, userId))
```

**3. LLM 응답 캐시 경로**
```
AiSuggestionService.suggest(prompt, userId)
   ├─ key = SHA-256(prompt) + userId prefix
   ├─ CachePort.get(LLM_RESPONSE, key)
   ├─ hit  → 캐시된 응답 즉시 반환 (LLM 호출 0)
   └─ miss → LLM 호출 → 응답 캐시 저장 (TTL 24h) → 반환
```

### Out-of-Process 의존 (v2)

- **AWS ElastiCache** — v2 전환 트리거 발생 시 도입. v1은 의존 없음
- **MySQL** — 진실 소스 (캐시는 보조)

### 핵심 컴포넌트

| 컴포넌트 | 위치 | 책임 |
| --- | --- | --- |
| `CachePort` | `Cache/domain/port/` | outbound port |
| `CacheRegion` | `Cache/domain/model/` | 영역 enum + 영역별 정책(VO) |
| `CacheKey` | `Cache/domain/model/` | region + key + (선택) version VO |
| `CaffeineCacheAdapter` | `Cache/infrastructure/caffeine/` | v1 기본 어댑터 |
| `RedisCacheAdapter` | `Cache/infrastructure/redis/` | v2 어댑터 (스켈레톤 v1에 포함) |
| `CacheConfig` | `Cache/infrastructure/config/` | 영역별 Cache 빈 등록, 프로필 분기 |
| `CacheMetricsCollector` | `Cache/infrastructure/metrics/` | 영역별 hit/miss/evict 카운터 |

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | 영향 | 완화 / 대응 |
| --- | --- | --- |
| Caffeine 메모리 폭주 (LearningFacade 사용자 폭증) | OOM 위험 | region별 `maximumSize` 강제 + JVM 힙 사용량 메트릭 |
| 캐시 stale (수정 후 무효화 누락) | 클라이언트가 옛 데이터 봄 | AFTER_COMMIT 이벤트 강제 + 단위 테스트로 evict 호출 검증 |
| 다중 task 환경에서 JWT 블랙리스트 누락 | 무효화된 토큰이 다른 task에서 통과 → 보안 사고 | `product-infra-deploy.md` task 수 결정 시 즉시 ElastiCache 전환 (ADR-CACHE-001 trigger) |
| LLM 캐시 키 충돌 (다른 사용자 응답 노출) | 개인정보 사고 | key prefix에 `userId` 강제 + 단위 테스트로 격리 검증 |
| ElastiCache 다운 (v2) | 캐시 의존 호출 전체 지연 | `RedisCacheAdapter`가 fallback으로 빈 결과 반환 + miss 처리 → 진실 소스 조회로 자동 회피 |
| 캐시 hit률 0% (잘못된 키 전략) | 캐시 무가치, 비용만 발생 | 메트릭 `cache_hit_ratio{region}` 임계 알림 (< 30%) |
| TTL 부적절 (너무 짧음 — miss 폭주 / 너무 김 — stale 만연) | UX 저하 | region별 TTL을 application.yml 외부화 + 운영 중 조정 |

### 로깅 정책

- **항상 기록**:
    - 캐시 evict 일괄 (INFO, region·count)
    - 영역별 hit/miss는 메트릭으로만 — 라인 단위 로깅 금지 (볼륨 폭주)
- **DEBUG**: 캐시 key·hit/miss 라인 (prod 비활성)
- **절대 금지**:
    - 캐시된 값 본문 (사용자 데이터)
    - JWT 토큰 원문 (블랙리스트 키는 토큰 해시 8자만)

### 관측 지표

| 지표 | 형식 | 의미 |
| --- | --- | --- |
| `cache_hit_total{region}` | 카운터 | 영역별 hit 누적 |
| `cache_miss_total{region}` | 카운터 | 영역별 miss 누적 |
| `cache_hit_ratio{region}` | 게이지 | 영역별 hit률 (산출) |
| `cache_evict_total{region, reason}` | 카운터 | evict 누적. reason=`ttl`/`size`/`manual` |
| `cache_size{region}` | 게이지 | 현재 entry 수 |
| `cache_memory_bytes{region}` | 게이지 | 추정 메모리 사용량 |
| `llm_call_avoided_total` | 카운터 | LLM 캐시 hit으로 회피한 호출 수 (비용 절감 추적) |

## 롤아웃 / 마이그레이션 (Rollout)

### 전제 — 단일 ECS task 단계 + LLM 미활성

`product-infra-deploy.md` task 수 = 1, `product-aisuggestion.md` Epic 3 미활성 상태에서 시작. 이 두 조건이 깨지는 시점이 ElastiCache 전환 trigger.

### Product 의존성

- **선행 Product**:
    - `in-progress/product-auth.md` — JWT 블랙리스트 인터페이스 (현재 위치 사후 확인 후 통합)
    - `in-progress/product-op.md` — 캐시 메트릭 노출 인프라
- **후행 Product**:
    - `in-progress/product-aisuggestion.md` Epic 3 — LLM 어댑터 활성화 시 `llm-response` region 즉시 적용
    - `in-progress/product-infra-deploy.md` task 수 결정 — ECS task 2개 결정 시 ADR-CACHE-001 trigger

### Epic·Story 의존성 그래프

```
Epic 1 (Cache BC 골격 + CachePort + Caffeine 어댑터)
  Story 1-1 CachePort + CacheRegion enum + CacheKey VO
  Story 1-2 CaffeineCacheAdapter (region별 설정)
  Story 1-3 CacheConfig 빈 등록 + 프로필 분기 + 단위 테스트
       │
       ▼
Epic 2 (LearningFacade 통합 + JWT 블랙리스트 통합)
  Story 2-1 LearningFacadeQueryService에 CachePort 주입
  Story 2-2 AFTER_COMMIT 이벤트 → evict (Command 측 Story)
  Story 2-3 JWT 블랙리스트 → CachePort 마이그레이션 (Common/security 협의)
       │
       ▼
Epic 3 (LLM 응답 캐시 — product-aisuggestion Epic 3 활성화와 동기화)
  Story 3-1 AiSuggestionService에 CachePort 주입 (key = SHA-256)
  Story 3-2 사용자별 격리 검증 단위 테스트
       │
       ▼
Epic 4 (메트릭 + 운영)
  Story 4-1 CacheMetricsCollector + Micrometer 노출
  Story 4-2 docs/cache.md + region 추가 절차 명시
  Story 4-3 ElastiCache 전환 Runbook 초안 (실행은 ADR trigger 시점)
       │
       ▼
Epic 5 (v2 — ElastiCache 전환, ADR trigger 시 실행)
  Story 5-1 Terraform — ElastiCache 도메인 프로비저닝
  Story 5-2 RedisCacheAdapter 활성화 + 프로필 전환
  Story 5-3 마이그레이션 후 24h 양 백엔드 병행 모니터링
```

### 환경별 설정 분기

| 항목 | dev | prod (v1) | prod (v2 전환 후) |
| --- | --- | --- | --- |
| `thirdtool.cache.backend` | `caffeine` | `caffeine` | `redis` |
| `learning-facade` TTL | 1분 | 5분 | 5분 |
| `jwt-blacklist` 위치 | Caffeine | Caffeine | Redis (필수) |
| `llm-response` enable | false (v1 미적용) | false (Epic 3 활성 시 true) | true |
| ElastiCache 엔드포인트 | — | — | Secrets Manager |

## 성공 지표 (KPI)

- `learning-facade` region hit률 ≥ 70% (정상 사용 패턴 기준)
- `llm-response` region 도입 후 LLM 호출 회피율 ≥ 30% (`llm_call_avoided_total` / 전체 요청)
- 캐시 stale로 인한 데이터 부정합 사용자 신고 = 0건
- v2 전환 시 코드 변경: 빈 등록 1곳 + Adapter 의존 1곳 (호출부 0)
- JWT 블랙리스트 누락으로 인한 보안 사고 = 0건
- 단일 task 단계에서 ElastiCache 도입으로 인한 사전 비용 = 0원

## Scope

**In Scope (v1)**:
- CachePort 추상화 + CaffeineCacheAdapter
- 영역 4종: `learning-facade`, `jwt-blacklist`, `llm-response`(빈 등록만, 활성은 Epic 3 트리거), `axis-topic-tree`
- 영역별 TTL·최대 크기·메트릭
- AFTER_COMMIT 이벤트 기반 evict
- LearningFacade·JWT 블랙리스트 통합

**Out of Scope (v1)**:
- ElastiCache 실제 도입 — ADR-CACHE-001 trigger 발생 시 Epic 5 별도 실행
- 의미 기반 LLM 캐시 (embedding 유사도) — v2 학습 후 도입 검토
- 캐시 워밍업(부팅 시 사전 로드) — 필요 발생 시 v2
- 분산 캐시 무효화 이벤트 (Redis Pub/Sub) — ElastiCache 전환 시 동반 결정
- 다단계 캐시 (L1 Caffeine + L2 Redis) — 비용 대비 가치 측정 후 v3

## 대상 사용자

- **학습자 (간접)** — LearningFacade 응답 시간 단축으로 체감 UX 개선
- **운영자** — 캐시 hit률·메모리·전환 trigger 메트릭으로 비용·성능 트레이드오프 추적
- **개발자** — `CachePort` 단일 인터페이스로 신규 캐시 영역 추가 표준화

## 연결된 Epic 목록

- [ ] Epic 1: Cache BC 골격 + CachePort + Caffeine 어댑터
- [ ] Epic 2: LearningFacade 통합 + JWT 블랙리스트 통합
- [ ] Epic 3: LLM 응답 캐시
- [ ] Epic 4: 메트릭 + 운영
- [ ] Epic 5: v2 ElastiCache 전환 (ADR trigger 시 실행)

## 관련 문서

- 의존 Product: `in-progress/product-auth.md` (JWT 블랙리스트), `in-progress/product-op.md` (메트릭), `in-progress/product-aisuggestion.md` (LLM 캐시), `in-progress/product-infra-deploy.md` (task 수 결정)
- 관련 ADR (예정): `ADR-CACHE-001 ~ 002` (백엔드 선택 / 추상화 시점)
- DOMAIN.md 추가 예정 섹션: `Cache BC` 신규 절 (작은 도메인 + outbound port 위주)
- PACKAGE.md 추가 예정 섹션: `com.example.thirdtool.Cache.*` 4계층 매핑
- 연계 brainstorming: `brainstorming/0.0.2v/generic-domains.md` 후보 3 + `brainstorming/0.0.1v/ai.md` 후보 7·8

## 열린 질문 (Open Questions)

- 현재 `product-auth.md`에서 JWT 블랙리스트가 in-memory인지 DB인지 — 진입 전 코드 확인 필요
- ECS task 수가 1개 유지인지 2개 이상으로 갈지 — `product-infra-deploy.md` 안정화 후 결정
- LLM 캐시 TTL 24시간이 적정한지 — 프롬프트 응답 시효성 vs 비용 절감 측정 후 조정
- region별 TTL을 운영 중 동적으로 바꿀 수 있어야 하는가? (application.yml 외부화로 충분한지)
- ElastiCache 전환 시 마이그레이션 다운타임을 허용할 것인가, zero-downtime alias swap이 필요한가?
