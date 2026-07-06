# Brainstorming · Generic 도메인 추천 카탈로그 (0.0.2v)

> **목적**: 현재 SDD(`workspectrum/sdd/`)에 설계된 6개 핵심 BC(Card·Deck·Review·LearningFacade·User·UserSchedule) 외부에 있는 **범용 백엔드 도메인**을 평가하고, v0.0.2v에 도입할 후보를 추천한다.
>
> **판단 기준**:
> 1. 서비스 연관성 — 현재 SRS 학습 플랫폼 UX에 직접 기여하는가
> 2. 구현 타이밍 — 사용자 기반 N명 전에도 ROI가 있는가
> 3. 아키텍처 충격 — 기존 BC 경계·단일 사용자 모델을 크게 흔드는가
> 4. 의존 완료 조건 — 선행 Story/ADR이 없는가

---

## 추천 요약 (한눈에)

| 후보 | 도메인 | Tier | v0.0.2v 권장 | 핵심 이유 |
|-----|--------|------|--------------|-----------|
| 1 | **알림(Notification)** | 1 | ✅ 강추 | SRS 이탈 방지 핵심, UserSchedule 직결 |
| 2 | **검색(Search)** | 1 | ✅ 강추 | 카드 수 증가 시 자연 필수, MySQL FULLTEXT MVP |
| 3 | **Redis 캐시 레이어** | 2 | 🔶 조건부 | LLM 도입 타이밍에 함께, Caffeine MVP 먼저 |
| 4 | **Admin 경량 엔드포인트** | 2 | 🔶 조건부 | 운영 필수 도구, 별도 서버 없이 ADMIN role |
| 5 | **파일·이미지 업로드** | 3 | ⏸ 보류 | 텍스트 기반으로 충분, Card BC 변경 부담 |
| 6 | **결제(Payment)** | 3 | ⏸ 보류 | MAU 없는 상태 ROI 낮음, 수익화 전략 미확정 |
| 7 | **소셜/공유(Social)** | 3 | ⏸ 보류 | 단일 사용자 아키텍처 패러다임 전환 필요 |

---

## Tier 1 — v0.0.2v 포함 강추

---

## [후보 1] 알림(Notification) 도메인 도입

> 학습 리마인더·복습 타이밍 알림으로 SRS 핵심 이탈 방지 루프를 닫는다.

### 배경
- `UserSchedule`에 `dailyTarget`·`mode(10D/20D/30D)`·interval 계산이 있지만, **그 시점을 사용자에게 알려주는 수단이 없음**.
- SRS 서비스는 "까먹기 직전에 복습" 패러다임 — 리마인더 없으면 사용자가 직접 앱을 열어야 하므로 이탈 최대 원인.
- `Card.softSchedule` 상태(`FRESH / INTERVAL_1D / 3D / 7D / 14D / 21D`)가 복습 예약 시점을 내부적으로 계산하는데, 이를 외부화하는 알림 트리거가 없음.
- 현재 알림 전용 BC/Port/Adapter 없음.

### 후보
- **A안**: AWS SES 이메일 MVP → FCM 푸시 알림 추후. 장점: 빠르게 구현, 비용 낮음. 비용: 이메일은 스팸 필터·수신 지연으로 SRS 타이밍 민감도와 상성 나쁨.
- **B안**: FCM(Firebase Cloud Messaging) 먼저. 장점: 즉시 알림 → SRS 타이밍 정확도 높음, 이탈 방지 효과 더 큼. 비용: FCM 디바이스 토큰 관리 + 웹 푸시(Service Worker) 필요.
- **C안**: 인앱 배너(서버 폴링 기반)만. 장점: 인프라 추가 없음. 비용: 앱을 열지 않으면 의미 없어 SRS 핵심 목적 달성 불가.

### 1차 권장
**B안 (FCM 먼저)**.
SRS에서 "언제 복습하라"는 타이밍 정보가 가장 중요하다. 이메일은 지연이 있어 SRS 특성과 맞지 않는다. FCM 웹 푸시로 시작하고, 이메일은 B안 이후 onboarding·주간 리포트 용도로 A+B 통합.

알림 트리거 우선순위:
1. 복습 예약 도래 (SoftSchedule interval 완료 시점)
2. dailyTarget 미달성 경보 (당일 23:00 기준)
3. 장기 미접속 경보 (7일 이상)

### PES 승격 경로
- 신규 Product: `product-notification.md`
- UserSchedule BC와 강결합 — interval 계산 결과 → 알림 트리거 Event 발행
- ADR 후보: "알림 채널 우선순위 — FCM vs SES 결정"
- 의존: UserSchedule v2 명세 확정 (복습 예약 타임라인 API 필요)

### 미해결 질문
- 웹 전용인가, 모바일 앱(iOS/Android) 병행인가? (FCM 토큰 관리 범위 결정)
- 알림 수신 동의 opt-in/opt-out을 UserSchedule BC에서 관리할 것인가, 신규 NotificationPreference VO로 분리할 것인가?

---

## [후보 2] 카드·자료 검색(Search) 도입

> 카드 수가 50개를 넘으면 전체 스크롤이 불가능해진다 — 전문 검색이 학습 접근성의 전제 조건.

### 배경
- 현재 Card BC는 덱 단위 전체 조회·태그 필터만 지원. **키워드·요약·mainNote 대상 전문 검색 없음**.
- 태그 검색은 `value` 정확 일치만 — 부분 문자열·한국어 형태소 검색 불가.
- `LearningMaterial` 자료명·URL 검색 없음.
- 사용자가 카드를 자주 만들수록(30-50개 이상) "그 카드 어디 있더라" 탐색 비용이 기하급수적으로 증가.

### 후보
- **A안**: MySQL FULLTEXT 인덱스 (card `summary`, keyword `value`). 장점: 인프라 추가 없음, 즉시 구현, 비용 0. 비용: 한국어 형태소 분석 약함(InnoDB FULLTEXT는 형태소 미지원), 부분 매칭 한계.
- **B안**: AWS OpenSearch Service. 장점: 한국어 형태소 분석(Nori), 전문 검색·랭킹·하이라이팅. 비용: 인스턴스 상시 비용($50~100/월), 데이터 동기화(CDC or 이벤트) 필요.
- **C안**: Elasticsearch self-managed (EC2). 비용: EC2 운영 부담 + 백업·업그레이드 직접 관리 — 1인 운영에 과함.

### 1차 권장
**A안 MVP → 사용자 기반 확보 후 B안 마이그레이션**.
현재 사용자 수 단계에서 OpenSearch는 인프라 비용 대비 ROI 낮다. FULLTEXT MVP로 시작하고, 카드 500개 이상 사용자가 생기면 B안 전환을 ADR에 기록.

검색 대상 우선순위:
1. `card.summary` + `keyword_cue.value` (학습 핵심 내용)
2. `learning_material.name` (자료명)
3. `axis_topic.name` (주제명)

### PES 승격 경로
- Card BC `product-card.md`에 Epic 추가 또는 신규 `product-search.md`
- ADR 후보: "검색 엔진 선택 — DB FULLTEXT vs OpenSearch 전환 기준"
- 의존: Card BC의 FULLTEXT 인덱스 Flyway 마이그레이션 추가 Story

---

## Tier 2 — v0.0.2v 후반 또는 v0.0.3v 조건부 검토

---

## [후보 3] Redis 캐시 레이어

> LLM 응답 캐시 + JWT 블랙리스트 일관성을 위한 공유 캐시 인프라.

### 배경
- **ai.md 후보 7** (AI 응답 캐싱)이 Redis 인프라를 전제. LLM 도입 시 캐시 없으면 동일 요청 반복 비용.
- JWT 블랙리스트 저장소가 현재 미명세 — 다중 ECS 인스턴스 환경에서 in-memory 블랙리스트는 인스턴스 간 불일치 위험.
- `LearningFacade` 전체 계층 조회(Facade → Axis → Topic → Material)는 카드 수 증가 시 N+1 잠재 부하.

### 후보
- **A안**: Amazon ElastiCache Serverless (Redis). 장점: 인프라 관리 최소, 사용량 비례 과금. 비용: 상시 비용 발생.
- **B안**: Amazon ElastiCache Provisioned (t3.micro). 장점: 비용 예측 가능($15~30/월). 비용: 미사용 시간 낭비.
- **C안**: Caffeine 인-프로세스 캐시 (ECS task 1개 가정). 장점: 인프라 추가 비용 0. 비용: ECS task 2개 이상 시 캐시 불일치. JWT 블랙리스트 해결 불가.

### 1차 권장
**C안 (Caffeine) MVP → LLM 실제 도입 + ECS 멀티 인스턴스 전환 시 A안**.
현재 ECS task 1개 운영 단계라면 Caffeine으로 LearningFacade 캐시부터 시작. LLM 어댑터(ai.md 후보 1) 활성화 시점에 A안으로 전환 결정.

ai.md 후보 7(AI 캐시), 후보 8(세션 저장소)과 묶어서 한 번에 ADR 결정 권장.

### PES 승격 경로
- `product-infra-ops.md` Epic 추가 또는 신규 `product-cache.md`
- ai.md 후보 7 (AI 응답 캐시)와 통합
- ADR 후보: "캐시 레이어 전략 — Caffeine vs ElastiCache, 전환 기준"

### 미해결 질문
- JWT 블랙리스트 현재 구현체가 DB인가 in-memory인가? (auth.md 확인 필요)
- ECS 목표 task 수 — 1개 유지인가 2개 이상인가? (캐시 일관성 전환 기준)

---

## [후보 4] Admin 경량 엔드포인트

> 사용자 관리·AI 프롬프트 운영·이상 계정 처리를 DB 직접 접근 없이 수행.

### 배경
- 현재 사용자 관리가 DB 직접 조회 수준 — role 변경·계정 정지·강제 로그아웃 API 없음.
- **ai.md 후보 2** (프롬프트 버전 관리)의 B안(hot-swap)을 운영 중 적용하려면 Admin API 필요.
- **ai.md 후보 5** (사용자별 AI 한도)를 운영자가 조정하려면 관리 인터페이스 필요.
- DB 직접 접근은 감사 로그 부재, 실수 위험, 보안 취약.

### 후보
- **A안**: Spring Security ADMIN role + `AdminController` (`/api/v1/admin/**`) + IP 화이트리스트. 장점: 별도 서버 없음, 기존 인프라 재사용. 비용: 보안 설정 철저 필요, 같은 포트 노출.
- **B안**: 별도 admin Spring Boot 서비스 (다른 포트/ECS task). 장점: 완전 분리, 독립 배포. 비용: ECS task + ALB listener rule 추가 비용·운영 부담.
- **C안**: DB 직접 접근 유지 (현 상태). 비용: 감사 불가, 실수 위험 지속.

### 1차 권장
**A안 — ADMIN role 기반 내부 엔드포인트**.
1인 운영 단계에서 별도 서버는 과함. ADMIN role + AWS Security Group IP 제한 + CloudWatch Logs 감사로 충분. 초기 기능: 사용자 role 변경·계정 정지·AI 토큰 한도 조정·프롬프트 버전 전환.

### PES 승격 경로
- 신규 Product: `product-admin.md`
- User BC Epic 추가 (ADMIN role 관리, `UserAdminCommandService`)
- ADR 후보: "Admin 접근 제어 전략 — 모놀리스 내부 vs 별도 서비스"

---

## Tier 3 — v0.0.3v 이후 보류

---

## [후보 5] 파일·이미지 업로드

> Card mainNote에 이미지를 포함해 시각적 flashcard 지원.

### 배경
- 현재 `Card.mainNote` 타입이 `TEXT` — 이미지 첨부 불가.
- 이미지 flashcard는 강력한 기억술(시각-언어 이중 코딩 이론).

### 후보
- **A안**: S3 presigned URL + CloudFront. 장점: 표준 패턴. 비용: CloudFront 비용 + `contentType` enum 확장 필요.
- **B안**: S3 presigned URL만. 장점: CDN 없이 단순. 비용: 대역폭 비용.
- **C안**: Base64 인코딩 DB 저장. 비용: DB 비대화·조회 성능 저하.

### 1차 권장
**보류 (v0.0.3v+ 검토)**.
Card BC의 `contentType` enum(`TEXT_ONLY / IMAGE_ONLY / TEXT_AND_IMAGE`) 확장 + `mainNote` 타입 재설계가 필요해 아키텍처 충격이 큼. 텍스트 기반으로 핵심 SRS 경험을 완성한 후 도입.

### PES 승격 경로
- `product-card.md` Epic 추가 ("Card 미디어 타입 지원")
- ADR 후보: "Card mainNote 미디어 타입 범위"

---

## [후보 6] 결제(Payment) 도메인

> AI 기능 프리미엄화·구독 플랜으로 서비스 수익화.

### 배경
- **ai.md 후보 5** (사용자별 AI 한도)가 "프리미엄 = 한도 해제" 개념의 전제.
- 구독 기반 수익화는 SRS 서비스의 자연스러운 경로.
- 단, 현재 MAU·재방문률 등 기초 지표 없는 상태.

### 후보
- **A안**: Toss Payments 구독 API. 장점: 한국 서비스 최적, 자동 재결제. 비용: 신규 BC + PCI DSS 준수 + Webhook 처리.
- **B안**: 카카오페이 단건 결제. 장점: 소셜 로그인(카카오) 연계. 비용: 구독 재결제 로직 직접 구현.
- **C안**: 전면 무료 (AI 비용 서버 부담). 비용: 사용자 증가 시 비용 폭주.

### 1차 권장
**보류 (v0.0.3v 이후)**.
결제 도입 전제 조건: MAU 목표 달성 + 수익화 전략 결정 + 프리미엄 기능 명세 확정. 현재 없음. 선행 작업: ai.md 후보 5 (AI 한도) 도입 → 한도 초과 사용자 행동 관찰 → 결제 니즈 검증.

### PES 승격 경로
- 신규 Product: `product-payment.md` (v0.0.3v+ 이후)
- ADR 후보: "수익화 모델 결정 — 구독 vs 소비"
- 의존: ai.md 후보 5 + MAU 기준 KPI 달성

---

## [후보 7] 소셜/공유(Social) 기능

> 다른 사용자의 LearningFacade 커리어 맵 참고 및 공유.

### 배경
- 현재 LearningFacade는 완전 1인 모델 (`1 user = 1 facade`).
- 타 사용자의 학습 설계 참고 → 신규 사용자 온보딩 비용 감소 기대.

### 후보
- **A안**: 읽기 전용 공개 프로필 (LearningFacade `isPublic` 토글). 장점: 쓰기 모델 안 건드림. 비용: 개인정보 범위 설계 필요.
- **B안**: Facade "가져오기" (복사). 장점: 타 사용자 설계 참고 후 커스터마이징. 비용: 복사본 ownership 명세 복잡.
- **C안**: 팔로우/피드 시스템. 비용: SNS 패러다임 전환, BC 대규모 추가.

### 1차 권장
**보류 (v1.0+ 이후)**.
소셜 기능은 도메인 모델의 패러다임 전환(1인 → 다인 관계)을 요구한다. MAU 충분 + 커뮤니티 니즈 확인 후 A안부터 소규모 실험.

### PES 승격 경로
- 신규 Product: `product-social.md` (v1.0+)
- 도메인 변경 필요: `LearningFacade.visibility` 필드, User 공개 프로필 BC

---

## v0.0.1v 브레인스토밍 후보와의 연계 맵

아래 후보들은 generic 도메인과 교차점이 있어 함께 결정해야 한다.

| generic 후보 | 연계된 기존 후보 | 연계 포인트 |
|-------------|----------------|------------|
| 알림 (후보 1) | ops.md 후보 1 (SLO/SLI) | 알림 발송 실패율을 SLO에 포함 |
| 알림 (후보 1) | ai.md 후보 5 (AI 한도) | 한도 초과 알림도 Notification 도메인에서 |
| 검색 (후보 2) | data.md 후보 5 (데이터 품질) | 검색 인덱스 동기화 일관성 점검 |
| Redis (후보 3) | ai.md 후보 7 (AI 응답 캐시) | 같은 Redis 인스턴스 공유 검토 |
| Redis (후보 3) | ai.md 후보 8 (세션 저장소) | Redis persistence 정책 통합 결정 |
| Admin (후보 4) | ai.md 후보 2 (프롬프트 버전) | Admin API로 프롬프트 hot-swap 구현 |
| Admin (후보 4) | ai.md 후보 5 (사용자별 한도) | Admin에서 한도 조정 UI 필요 |
| 결제 (후보 6) | ai.md 후보 5 (AI 한도) | 한도 초과 = 프리미엄 업그레이드 유도 연결점 |

---

## 다음 단계 제안

### v0.0.2v 권장 실행 순서

```
1. [후보 2] 검색 MVP        — Card BC에 FULLTEXT 인덱스 1 Story (의존 없음, 즉시 착수 가능)
2. [후보 4] Admin 경량      — ADMIN role + 계정 정지 API (User BC 내 Epic 추가)
3. [후보 1] 알림            — FCM + UserSchedule interval 이벤트 연결 (UserSchedule v2 확정 후)
4. [후보 3] Redis           — ai.md 후보 1 (LLM 어댑터) 활성화 시점에 함께
```

### v0.0.3v 이후 순서 (참고)

```
5. [후보 5] 이미지 업로드   — Card BC 미디어 타입 재설계와 함께
6. [후보 6] 결제            — MAU 목표 달성 + 수익화 전략 확정 후
7. [후보 7] 소셜            — v1.0 이후 커뮤니티 니즈 확인 후
```

### SDD 작성 우선순위

가장 빨리 SDD 착수가 권장되는 순서:

1. `product-notification.md` — UserSchedule·FCM 의존 명세 필요, 설계 선행 권장
2. `product-search.md` (또는 product-card.md 내 Epic) — 단순하지만 DB 인덱스 설계 명세 필요
3. `product-admin.md` — User BC 확장 범위 명세 필요

---

*작성일: 2026-06-27 | 트래킹 기준: v0.0.1v 완료 (commit `07301c6`)*
