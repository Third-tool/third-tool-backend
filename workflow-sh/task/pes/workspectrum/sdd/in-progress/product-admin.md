# [Product] Admin 경량 엔드포인트 — ADMIN role 기반 운영 API

## Product Vision

> ThirdTool의 운영 행위(사용자 role 변경·계정 정지·AI 한도 조정·프롬프트 버전 전환·강제 로그아웃)를 DB 직접 접근 없이 ADMIN role 기반 API로 표준화한다.
> 별도 admin 서비스를 띄우지 않고 본 애플리케이션 안에 격리된 엔드포인트 묶음으로 두되, Security Group IP 제한 + CloudWatch Logs 감사로 보안 표면을 닫는다.
> Admin은 "운영자가 콘솔보다 빠르게, 더 안전하게" — DB 직접 접근을 끝내고 모든 운영 행위를 감사 가능한 API 호출로 통합한다.

## 배경 및 문제

- 현재 상황 (As-Is)
    - 사용자 관리가 DB 직접 조회 수준 — role 변경·계정 정지·강제 로그아웃 API 부재
    - `product-aisuggestion.md` Epic 3 활성 시 프롬프트 버전 관리(`ai.md` 후보 2)의 hot-swap을 운영 중 적용할 수단이 없음
    - 사용자별 AI 한도 조정(`ai.md` 후보 5)이 도입되어도 운영자가 한도를 변경할 인터페이스가 없음
    - DB 직접 접근은 감사 로그 부재, SQL 실수 위험, 보안 취약 (DB 자격증명을 운영자가 보유)
    - User 도메인의 role enum이 `USER` 단일 — ADMIN role 자체가 미정의
- 발생하는 문제
    - 사용자 신고 대응에 운영자가 DB에 직접 접속 → SQL 1줄 실수가 데이터 손상으로 직결
    - "누가 언제 무엇을 바꿨나"를 추적할 수 없음 — 감사 의무 위반
    - DB 자격증명을 운영자가 보유하는 한 보안 표면이 코드 변경 없이 확장됨
    - 프롬프트 hot-swap·AI 한도 조정은 운영자가 자주 만질 표면 — DB 직접 조작으로는 운영 자체가 불가능
    - 면접 관점: "운영자가 사용자 정지하는 절차는 어떻게 되나요" — DB 접속이 답이면 즉시 감점
- 왜 지금 해결해야 하는가
    - `product-aisuggestion.md` Epic 3(LLM 어댑터) 활성 직전에 프롬프트 hot-swap·AI 한도 API가 필요 — 시퀀스 역행
    - User BC가 안정화된 직후가 ADMIN role 추가의 가장 싼 시점 — 도메인 모델이 흔들리는 중에 role 모델 변경은 비용 누적
    - 운영자 1인 단계에서 절차·감사 패턴을 정착시켜야, 운영자 추가 시 표준이 이미 깔려 있음
    - 보안 표면(DB 직접 접근) 폐기 비용은 미루면 미룰수록 커진다

## 목표 (To-Be)

- 모든 운영 행위가 `/api/v1/admin/**` 엔드포인트를 통해서만 가능하고, 각 호출이 CloudWatch Logs에 감사 라인으로 남는다
- User 도메인에 `ADMIN` role이 추가되고, ADMIN 사용자만 `/admin/**`에 접근 가능 (`USER`는 403)
- AWS Security Group에서 `/admin/**` 경로 호출은 화이트리스트 IP에서만 가능 (운영자 사무실·개인 IP)
- 사용자 계정 정지·재활성화·role 변경·강제 로그아웃(JWT 블랙리스트 즉시 등록)이 API로 가능
- AI 토큰 한도·프롬프트 버전 hot-swap이 API로 즉시 반영되고, 변경 사실이 감사 로그에 남는다
- DB 직접 접근 운영 절차가 폐기되고, DB 자격증명 보유자가 0명이 된다 (애플리케이션 IAM role + Secrets Manager만)
- 모든 admin 호출에 변경 전/후 값이 감사 로그에 기록되어, 사후 복원이 가능하다

## 설계 결정 (Design Decision)

> **별도 admin Spring Boot 서비스를 띄우지 않고, 본 애플리케이션 안에 `/admin/**` 경로로 분리한다.**
> 1인 운영 단계에서 별도 서비스 운영 비용 정당화 불가.
>
> - 별도 서비스 = ECS task + ALB listener rule + 배포 파이프라인 + 의존성 동기화 비용 (≥ 3배)
> - 보안 격리는 ADMIN role + Security Group IP 제한 + CloudWatch Logs 감사 3중 방어로 대체
> - v2 다중 운영자 단계에서 분리 검토 — Admin 트래픽이 일반 트래픽과 자원 경합하거나, 별도 SLA가 필요해지면 분리
> - 이 결정은 ADR로 별도 기록한다 (`ADR-ADMIN-001: Deployment Topology — In-Process Endpoint`)

> **ADMIN role을 User 도메인의 role enum에 추가한다. 별도 admin 사용자 테이블은 만들지 않는다.**
> User Aggregate 안에서 권한 모델을 단일화.
>
> - 별도 admin 테이블 = User-Admin 매핑 + 권한 동기화 부담
> - role enum 확장: `USER`, `ADMIN` (v1). v2에서 `SUPER_ADMIN` 추가 검토
> - ADMIN으로 승격은 별도 마이그레이션 (Flyway 시드 + 수동 SQL — 1회성)
> - Spring Security `@PreAuthorize("hasRole('ADMIN')")` 강제
> - 이 결정은 ADR로 별도 기록한다 (`ADR-ADMIN-002: Role Model — In-User Enum`)

> **Admin API 호출은 모두 감사 로그를 강제로 남긴다. 감사 누락은 PR 차단 사유.**
>
> - 감사 로그 필드: `actorUserId`, `actorIp`, `targetUserId`/`targetResource`, `action`, `beforeValue`, `afterValue`, `requestId`, `timestamp`
> - 감사 로그는 INFO 레벨 별도 logger(`audit.admin`)로 출력 → CloudWatch Logs에 별도 log group으로 분리
> - 단위 테스트로 "admin endpoint 호출 시 감사 로그가 정확히 1건 출력된다"를 강제
> - 이 결정은 ADR로 별도 기록한다 (`ADR-ADMIN-003: Audit Logging — Required, Test-Enforced`)

> **Security Group IP 화이트리스트를 ALB listener rule로 적용한다.**
>
> - `/api/v1/admin/**` 경로는 ALB listener rule에서 운영자 IP CIDR만 통과
> - 다른 IP에서 접근 시 ALB가 403 응답 → 애플리케이션에 도달하지 않음
> - 운영자 IP 추가는 Terraform 변경 + ADR 기록
> - VPN 도입 전 단계에서 가장 단순한 격리. VPN 도입(v2) 시 listener rule 제거 + VPN 종속

> **운영 행위는 멱등성을 보장한다. 같은 호출을 두 번 해도 시스템 상태가 일관.**
>
> - 계정 정지: 이미 정지된 사용자에게 호출 → no-op + 200
> - role 변경: 같은 role로 변경 → no-op + 200
> - 강제 로그아웃: 이미 블랙리스트된 토큰 → no-op + 200
> - 멱등 키는 별도 도입하지 않음 (상태 비교로 충분)

## 대안 검토 (Alternatives Considered)

### 배포 토폴로지

**Option A (선택) — Spring Security ADMIN role + `/admin/**` 내부 엔드포인트 + IP 화이트리스트**
- 비용: 같은 포트 노출 → 보안 설정 철저 필요, 화이트리스트 누락 시 노출
- 보상: 별도 서버 없음, 기존 인프라 재사용, 1인 운영에 적합
- 트레이드오프 수용 근거: 3중 방어(role + IP + 감사)로 보안 표면 충분히 닫힘

**Option B — 별도 admin Spring Boot 서비스**
- 장점: 완전 분리, 독립 배포, SLA 격리
- 거부 이유:
    - ECS task + ALB listener rule + 배포 파이프라인 추가 비용
    - 1인 운영 단계에서 운영 부담 정당화 불가
    - User 도메인 의존 코드 중복 (User Aggregate 재사용 vs 복사)
    - v2 재검토 후보로 보존

**Option C — DB 직접 접근 유지 (현 상태)**
- 거부 이유: 감사 불가, 실수 위험 지속, DB 자격증명 노출 표면

### Role 모델

**Option A — 별도 admin 사용자 테이블**
- 거부 이유: User-Admin 매핑 비용, 권한 동기화 부담, 같은 사람의 두 계정 관리 부담

**Option B (선택) — User role enum에 ADMIN 추가**
- 비용: enum 확장 시 마이그레이션
- 보상: 단일 도메인 모델, Spring Security 통합 단순

**Option C — 별도 권한 시스템(Casbin·OPA)**
- 거부 이유: 권한 정책이 단순(2~3종) — 외부 정책 엔진 도입 ROI 음수

### IP 격리 방식

**Option A (선택) — ALB listener rule + Security Group IP 화이트리스트**
- 비용: 운영자 IP 변경 시 Terraform 변경 필요
- 보상: 애플리케이션에 도달 전 차단 → 가장 안전

**Option B — VPN (Client VPN / Site-to-Site)**
- 장점: IP 변경에 강함
- 거부 이유: VPN 인프라 비용 + 운영자 1인 단계에서 ROI 음수. v2 운영자 다수 시 도입

**Option C — 공개 + JWT만 의존**
- 거부 이유: JWT 탈취 시 즉시 admin 접근 — 표면 너무 큼

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 배치

```
┌─────────────────────────────────────────────────────────────┐
│ Presentation                                                 │
│   AdminUserController     (/admin/users/**)                  │
│   AdminAiController       (/admin/ai/**)                     │
│   AdminAuthController     (/admin/auth/**)                   │
│   AdminSystemController   (/admin/system/**, metrics 등)      │
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────────┐
│ Application (Admin 전용 서비스)                              │
│   AdminUserCommandService (role 변경·계정 정지·강제 로그아웃)  │
│   AdminAiCommandService   (AI 한도·프롬프트 hot-swap)         │
│   AdminAuditLogger        (감사 로그 발행 전용 컴포넌트)        │
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────────┐
│ Domain                                                       │
│   User Aggregate (role enum 확장)                            │
│   User.suspend() / activate() / changeRole()                 │
│   JwtBlacklistService (강제 로그아웃 경로)                     │
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────────┐
│ Infrastructure                                               │
│   AdminEndpointSecurityConfig (@PreAuthorize hasRole(ADMIN)) │
│   AdminAuditFilter (요청 컨텍스트 → MDC actor* 주입)           │
│   audit.admin logger → CloudWatch log group                  │
└─────────────────────────────────────────────────────────────┘
```

### 핵심 플로우

**1. 계정 정지 흐름**
```
POST /api/v1/admin/users/{targetUserId}/suspend
   │ (ADMIN role + IP 화이트리스트 통과)
   ▼
AdminUserController.suspend(targetUserId, AdminContext)
   │
   ▼
AdminUserCommandService.suspend(actorUserId, targetUserId, reason)
   ├─ before = userRepository.findById(targetUserId)
   ├─ user.suspend(reason)   ─► User Aggregate 행위 (멱등)
   ├─ userRepository.save(user)
   ├─ jwtBlacklistService.invalidateAllTokens(targetUserId) → CachePort
   └─ AdminAuditLogger.log(action="SUSPEND_USER", before=before, after=user, ...)
                  │
                  ▼
            audit.admin INFO 라인 → CloudWatch
                  │
                  ▼
            응답: 200 (멱등) or 403 (권한) or 404 (대상 없음)
```

**2. AI 한도 조정 흐름**
```
PATCH /api/v1/admin/ai/limits/{userId}  { dailyTokenLimit: 10000 }
   │
   ▼
AdminAiCommandService.updateLimit(actorUserId, targetUserId, newLimit)
   ├─ 검증: newLimit ∈ [0, 100000]
   ├─ before = aiLimitRepository.find(targetUserId)
   ├─ aiLimitRepository.upsert(targetUserId, newLimit)
   └─ AdminAuditLogger.log(action="UPDATE_AI_LIMIT", ...)
```

**3. 프롬프트 hot-swap (product-aisuggestion 통합)**
```
POST /api/v1/admin/ai/prompts/{promptKey}/activate-version  { version: "v3" }
   │
   ▼
AdminAiCommandService.activatePromptVersion(actorUserId, key, version)
   ├─ promptVersionRepository.findByKeyAndVersion(key, version) → 존재 검증
   ├─ promptActivationRepository.setActive(key, version) (단일 활성 보장)
   ├─ promptCache.evict(key)   ─► CachePort
   └─ AdminAuditLogger.log(action="ACTIVATE_PROMPT", before=oldVersion, after=version)
```

### Out-of-Process 의존

- **ALB listener rule + Security Group** — `/admin/**` 경로 IP 화이트리스트 (Terraform 관리)
- **CloudWatch Logs** — `audit.admin` 별도 log group, 보존 1년 (감사 요구)
- **MySQL** — User 도메인, AI 한도·프롬프트 버전 저장소

### 핵심 컴포넌트

| 컴포넌트 | 위치 | 책임 |
| --- | --- | --- |
| `AdminUserController` | `Admin/presentation/` | 사용자 관리 API |
| `AdminAiController` | `Admin/presentation/` | AI 한도·프롬프트 API |
| `AdminAuthController` | `Admin/presentation/` | 강제 로그아웃·JWT 관리 |
| `AdminSystemController` | `Admin/presentation/` | 시스템 상태·메트릭 보조 API |
| `AdminUserCommandService` | `Admin/application/` | User Aggregate 행위 조율 + 감사 |
| `AdminAiCommandService` | `Admin/application/` | AI 도메인 조율 + 감사 |
| `AdminAuditLogger` | `Admin/application/` | 감사 라인 단일 출구 |
| `AdminEndpointSecurityConfig` | `Admin/infrastructure/security/` | `@PreAuthorize` 강제 + 경로 매칭 |
| `AdminAuditFilter` | `Admin/infrastructure/filter/` | actor 컨텍스트 → MDC 주입 |

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | ErrorCode | HTTP | 클라이언트 권장 동작 |
| --- | --- | --- | --- |
| 일반 사용자가 `/admin/**` 접근 | `ACCESS_DENIED` | 403 | 권한 없음 안내 |
| 화이트리스트 IP 외 접근 | (ALB 응답) | 403 | ALB 단계 차단 — 애플리케이션 로그 없음 |
| ADMIN이 자기 자신을 정지 시도 | `ADMIN_SELF_SUSPEND_FORBIDDEN` | 400 | 잠금 방지 |
| 마지막 ADMIN 강등 시도 | `LAST_ADMIN_DEMOTION_FORBIDDEN` | 400 | 운영 불가 상황 방지 |
| 존재하지 않는 사용자 대상 | `USER_NOT_FOUND` | 404 | 식별자 확인 |
| 멱등 — 이미 같은 상태 | (정상) | 200 | 감사 로그는 남기되 상태 변경 없음 |
| 감사 로그 출력 실패 (CloudWatch 다운) | (내부) | 500 | 보수적 처리 — 상태 변경 트랜잭션 롤백 |
| AI 한도 범위 위반 (음수·100,000 초과) | `AI_LIMIT_OUT_OF_RANGE` | 400 | 범위 안 값으로 재시도 |
| 프롬프트 버전 미존재 | `PROMPT_VERSION_NOT_FOUND` | 404 | 버전 등록 선행 |

### 로깅 정책

- **감사 로그 (audit.admin)** — 모든 admin 호출 1라인 INFO. 필드 강제: `actorUserId`·`actorIp`·`action`·`targetResource`·`beforeValue`·`afterValue`·`requestId`·`timestamp`
- **일반 로그**:
    - 권한 위반 (WARN, actor·target·path)
    - 화이트리스트 외 IP 접근 (WARN, ALB → 별도 수단으로 감지)
- **DEBUG**: admin 요청·응답 본문 (prod 비활성)
- **절대 금지**:
    - 정지 사유에 사용자 입력 PII 그대로 출력 (마스킹)
    - 변경 전/후 값에 비밀번호·토큰·이메일 본문 (해시 또는 마스킹)

### 관측 지표

| 지표 | 형식 | 의미 |
| --- | --- | --- |
| `admin_request_total{path, status}` | 카운터 | 경로·결과별 호출 수. 비정상 status 급증 시 알림 |
| `admin_request_latency_seconds{path}` | 히스토그램 | 응답 시간 |
| `admin_access_denied_total{reason}` | 카운터 | 권한 거절 누적. reason=`not_admin`/`ip_blocked` |
| `admin_audit_log_failure_total` | 카운터 | 감사 출력 실패 (즉시 알림 대상) |
| `admin_action_total{action}` | 카운터 | action별 호출 누적 (SUSPEND_USER · CHANGE_ROLE · ...) |
| `last_admin_count` | 게이지 | 현재 ADMIN role 보유자 수 (1 미만 = 운영 불가 위험) |

## 롤아웃 / 마이그레이션 (Rollout)

### 전제 — User BC 안정화 + 인프라(ALB·CloudWatch) 가용

User Aggregate 모델이 안정되고, `product-infra-network.md`의 ALB와 `product-log.md`의 CloudWatch Logs 출력이 정착된 후 진입.

### Product 의존성

- **선행 Product**:
    - `done/product-card.md`, `done/product-learningFacade.md` 외 도메인 안정 (간접)
    - `in-progress/Product.md` (User BC) — role enum 확장 baseline
    - `in-progress/product-log.md` — `audit.admin` logger를 분리해 출력
    - `in-progress/product-infra-network.md` — ALB listener rule (IP 화이트리스트)
    - `in-progress/product-op.md` — admin 메트릭 노출
    - `backlog/product-cache.md` — JWT 블랙리스트 (강제 로그아웃 통합)
- **후행 Product (Admin이 인터페이스 제공)**:
    - `in-progress/product-aisuggestion.md` Epic 3 — 프롬프트 hot-swap·AI 한도 API 제공
    - `backlog/product-notification.md` — 사용자 알림 강제 해제·재발송 (v2)

### Epic·Story 의존성 그래프

```
Epic 1 (User role 확장 + 보안 설정)
  Story 1-1 User.role enum에 ADMIN 추가 + Flyway 마이그레이션
  Story 1-2 ADMIN 시드 사용자 등록 절차 (Flyway 수동 단계 + Runbook)
  Story 1-3 AdminEndpointSecurityConfig + @PreAuthorize 강제
  Story 1-4 Slice 테스트 — 비ADMIN 403 / ADMIN 200
       │
       ▼
Epic 2 (감사 로깅 인프라)
  Story 2-1 AdminAuditLogger + audit.admin logger 등록
  Story 2-2 AdminAuditFilter — MDC actor 컨텍스트 주입
  Story 2-3 단위 테스트 — admin 호출 시 감사 라인 정확히 1건 보장
  Story 2-4 CloudWatch log group 분리 (logback-spring.xml profile)
       │
       ▼
Epic 3 (사용자 관리 API)
  Story 3-1 POST /admin/users/{id}/suspend + reactivate
  Story 3-2 PATCH /admin/users/{id}/role (last admin 방어 포함)
  Story 3-3 POST /admin/users/{id}/force-logout (JWT 블랙리스트 통합)
  Story 3-4 GET /admin/users?query=... (간단 검색)
       │
       ▼
Epic 4 (AI 운영 API — product-aisuggestion Epic 3 활성화와 동기화)
  Story 4-1 PATCH /admin/ai/limits/{userId}
  Story 4-2 GET/POST /admin/ai/prompts/** (버전 목록·등록)
  Story 4-3 POST /admin/ai/prompts/{key}/activate-version (hot-swap + 캐시 evict)
       │
       ▼
Epic 5 (인프라 + 운영)
  Story 5-1 Terraform — ALB listener rule + Security Group 화이트리스트
  Story 5-2 메트릭 노출 (admin_*) + last_admin_count 알림
  Story 5-3 docs/admin.md + 운영 Runbook (감사 라인 추적·신규 admin 등록 절차)
```

### 환경별 설정 분기

| 항목 | dev | prod |
| --- | --- | --- |
| ADMIN 시드 사용자 | 개발자 본인 1명 | 운영자 1명 (수동 등록) |
| IP 화이트리스트 | 비활성 (로컬 접근) | 운영자 IP CIDR |
| 감사 log group | `thirdtool-dev/audit/admin` | `thirdtool-prod/audit/admin` (보존 365일) |
| `last_admin_count` 알림 임계 | 비활성 | < 1 즉시 알림 |
| `@PreAuthorize` 강제 | 활성 | 활성 |

## 성공 지표 (KPI)

- DB 직접 접근으로 수행되던 운영 행위 = 0건 (전수 admin API로 이관)
- 모든 admin API 호출에 대한 감사 로그 누락 = 0건 (테스트 강제)
- 일반 사용자의 `/admin/**` 접근 차단률 = 100%
- IP 화이트리스트 외 접근의 애플리케이션 도달 = 0건 (ALB 단에서 차단)
- ADMIN role 보유자 < 1인 상황 = 0회 (last admin 방어)
- 운영자 1명이 정지·복원·role 변경·로그아웃을 1분 안에 수행 가능 (반응 시간 KPI)

## Scope

**In Scope (v1)**:
- User role enum 확장 (USER, ADMIN)
- 사용자 관리 API: 계정 정지·재활성화·role 변경·강제 로그아웃·간단 검색
- AI 운영 API: 한도 조정·프롬프트 버전 hot-swap
- AdminAuditLogger + audit.admin logger + CloudWatch log group 분리
- Security Group IP 화이트리스트 (ALB listener rule)
- 멱등성 보장 (같은 호출 → 같은 결과)
- last admin 방어 + self-suspend 방어

**Out of Scope (v1)**:
- Admin Web UI — v2 (운영자 1인 단계에서 cURL/Postman으로 충분)
- VPN 기반 격리 — v2 운영자 다수 시 도입 검토
- SUPER_ADMIN / 다단계 권한 — v2
- 알림 강제 해제·재발송 API — `backlog/product-notification.md` v2 연계
- 대량 작업 (사용자 일괄 정지 등) — v2 (현재 단계는 단건)
- 운영자 활동 분석 대시보드 — v2 (CloudWatch Insights로 충분)
- 별도 admin Spring Boot 서비스 분리 — v2 운영자 다수 + SLA 요구 시 검토

## 대상 사용자

- **운영자** — DB 직접 접근 없이 사용자·AI·인증 운영 행위를 안전하게 수행
- **개발자 (보조)** — 운영 행위에 대한 감사 라인을 통해 사고 추적·재현
- **보안 감사자 (외부, v2)** — CloudWatch Logs 감사 라인으로 운영 행위 사후 검증

## 연결된 Epic 목록

- [ ] Epic 1: User role 확장 + 보안 설정
- [ ] Epic 2: 감사 로깅 인프라
- [ ] Epic 3: 사용자 관리 API
- [ ] Epic 4: AI 운영 API
- [ ] Epic 5: 인프라 + 운영

## 관련 문서

- 의존 Product: `in-progress/Product.md` (User BC), `in-progress/product-log.md`, `in-progress/product-infra-network.md` (ALB), `in-progress/product-op.md`, `backlog/product-cache.md` (JWT 블랙리스트), `in-progress/product-aisuggestion.md` (AI 한도·프롬프트)
- 관련 ADR (예정): `ADR-ADMIN-001 ~ 003` (배포 토폴로지 / Role 모델 / 감사 강제)
- DOMAIN.md 갱신 예정 섹션: `User BC — Role 확장`
- PACKAGE.md 추가 예정 섹션: `com.example.thirdtool.Admin.*` 4계층 매핑
- 연계 brainstorming: `brainstorming/0.0.2v/generic-domains.md` 후보 4 + `brainstorming/0.0.1v/ai.md` 후보 2·5

## 열린 질문 (Open Questions)

- ADMIN 시드 사용자 등록을 Flyway 마이그레이션에 박을지, 수동 SQL Runbook으로 둘지 — 비밀번호 노출 위험 고려
- 운영자 IP가 동적(가정용 통신사 IP)이면 화이트리스트 운영 부담 — VPN 도입 시점 앞당김 vs IPv6 IP 풀 등록 vs Cloudflare Zero Trust 검토
- 감사 로그 보존 기간 365일이 적정한지 — 법적/내부 정책 확인 필요
- 강제 로그아웃 후 즉시 재로그인 가능한가 — 정지(suspend) 상태가 아니라면 가능. 두 행위를 어떻게 명확히 구분할지 UX 설계 필요
- 마지막 ADMIN 강등 방어 외에, 마지막 ADMIN 정지·삭제 방어도 포함해야 하는가 (자기 자신 + 강등 외 추가)
