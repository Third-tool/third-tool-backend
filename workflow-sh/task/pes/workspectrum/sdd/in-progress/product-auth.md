# [Product ] 인증 인프라 재설계 — 토큰 라이프사이클 및 경계 통일

# [Product 1] 인증 인프라 재설계 — 토큰 라이프사이클 및 경계 통일

## Product Vision

> Access Token과 Refresh Token의 수명·저장 위치·전달 경로를 명확히 분리하고,
인증 실패 응답을 GlobalExceptionHandler 계열로 통일해
프로덕션 수준의 JWT 인증 인프라를 구축한다.
>

## 배경 및 문제

- 현재 상황 (As-Is)
    - Access Token과 Refresh Token이 모두 응답 바디로 전달된다 (`TokenResponse(accessToken, refreshToken)`)
    - Refresh Token은 경로에 따라 쿠키로도 발급되고(소셜 로그인), 응답 바디로도 전달된다(자체 로그인). 경로별로 전달 매체가 다르다
    - 쿠키 보안 속성이 느슨하다 (`setSecure(false)`, `SameSite` 미설정, `MaxAge=10`)
    - `JWTUtil.createJWT()` 직접 호출이 `UserController` / `UserService` / `SocialLoginController` 세 곳에 분산되어 있다
    - `JWTFilter`가 `writeUnauthorizedResponse()`로 응답을 직접 write해 다른 API의 `{ code, message }` 형식과 불일치한다
    - `JwtService.refreshRotate()`에 디버그 로그 문자열(`"리프레쉬가 진짜 없지롱"`)과 `RuntimeException` 직접 throw가 남아 있다
    - `/jwt/exchange` 엔드포인트가 쿠키 → 헤더 변환 임시 다리로 존재해 토큰 전달 경로가 이원화되어 있다
- 발생하는 문제
    - RT가 응답 바디로 직접 노출되는 순간 프론트엔드 로그·브라우저 확장·네트워크 패널 어디서든 평문으로 관측된다
    - 토큰 발급 정책 변경 시 세 곳을 동시에 수정해야 해 누락 리스크가 크다
    - 인증 실패 응답 형식이 API마다 달라 FE의 에러 핸들러가 분기 처리를 해야 한다
    - Rotate 실패 원인을 클라이언트가 구분할 수 없다 (쿠키 없음 / 파싱 실패 / DB 미존재가 전부 같은 `RuntimeException`)
- 왜 지금 해결해야 하는가
    - User BC 도메인 리팩토링(Product 2)이 `TokenIssuer` / `ErrorCode` 통일을 전제로 한다. 인프라가 먼저 정돈되지 않으면 도메인 리팩토링이 중복 작업을 만든다

## 목표 (To-Be)

- Access Token은 30분 TTL로 HttpOnly Cookie로만 발급된다
- Refresh Token은 7일 TTL로 응답 바디로 전달되며, 클라이언트(React)가 메모리에서만 관리한다
- 토큰 발급 로직이 `TokenIssuer` 단일 진입점에서 실행된다
- 인증 관련 예외가 `BusinessException(ErrorCode)` 계층으로 통일된다
- `/jwt/exchange` 엔드포인트가 제거되고, 소셜 로그인도 동일한 발급 경로를 사용한다

## 설계 결정 (Design Decision)

> **AT = HttpOnly Cookie / RT = React 메모리** — 업계 표준(AT 메모리 / RT HttpOnly Cookie)의 역방향 선택.
>
> - AT (30분, HttpOnly Cookie)
    >     - XSS로 탈취 불가. 브라우저 자동 전송으로 FE는 Authorization 헤더 주입 코드가 불필요하다
>     - CSRF는 `SameSite=Strict` + `/api/**` 경로 한정으로 방어한다
> - RT (7일, React 메모리)
    >     - 페이지 새로고침 시 유실되지만 AT가 쿠키로 살아 있으므로 AT 만료 전까지는 자동 재인증 가능
>     - AT 만료 후 새로고침이 발생하면 재로그인 요구 (보안 관점에서 수용 가능한 트레이드오프)
>     - RT를 XSS로 탈취해도 fetch가 새 오리진에서 실행되므로 CORS로 2차 방어 가능
> - 이 결정은 ADR로 별도 기록한다 (`ADR-00X: Token Storage Strategy`)

## 대안 검토 (Alternatives Considered)

> 큰 갈림길마다 "왜 이것이 아니고 저것인가"를 남긴다. 거부된 안에도 합리적 근거가 있었음을 보임으로써 현재 선택의 트레이드오프를 명확히 한다.

### 토큰 저장 위치

**Option A — AT 메모리 / RT HttpOnly Cookie (업계 표준)**
- 장점: RT가 XSS에 안전, FE가 RT를 인지하지 않음
- 거부 이유:
    - AT를 매 요청 Authorization 헤더에 직접 주입 — FE 전체에 보일러플레이트 누적
    - 새로고침 시 AT 유실 → 매 새로고침마다 RT 재발급 호출 (UX 끊김 + 트래픽 증가)
    - RT 쿠키가 매 요청 자동 전송되어 CSRF 표면이 확대됨

**Option B — AT / RT 모두 HttpOnly Cookie**
- 장점: FE는 토큰 존재 자체를 모름. 자동 인증
- 거부 이유:
    - RT를 매 요청마다 자동 전송 → 재발급 엔드포인트가 아닌 모든 API에 RT가 노출되는 표면
    - 쿠키 Path 제한은 약함 (`/jwt/refresh` 외 차단이 어려움)
    - SameSite=Strict 동일 오리진 XSS 시 RT 탈취 위험은 그대로

**Option C (선택) — AT HttpOnly Cookie / RT React 메모리**
- 비용: 새로고침 직후 RT 유실. AT 만료 시점이 도래하면 재로그인 필요
- 보상: AT가 30분이라 새로고침 직후 즉시 끊기지 않음. RT는 fetch 응답 바디로만 흐르므로 자동 전송 표면이 없음
- 트레이드오프 수용 근거: 현재 React SPA 단일 클라이언트 + 사용자 트래픽 0명. UX 부담은 작고, 보일러플레이트 절감과 CSRF 표면 축소 이득이 큼

### 토큰 발급 진입점

**Option A — 호출처별 JWTUtil.createJWT 직접 호출 유지**
- 거부 이유: 발급 정책 변경(TTL / 쿠키 속성 / 화이트리스트 저장 정책)이 3곳에 분산 → 누락 리스크. 단위 테스트 단위가 흐려짐

**Option B (선택) — TokenIssuer 단일 컴포넌트**
- 비용: 호출처가 `HttpServletResponse`를 주입받아야 하는 시그니처 결합
- 보상: 발급 정책 변경이 1곳. 호출자는 user + response만 알면 됨

### 인증 실패 응답 형식

**Option A — 필터 단계 응답 직접 write 유지** (현 상태)
- 거부 이유: API 응답 형식이 두 갈래로 분기 (필터 JSON / Handler `{code, message}`) → FE 에러 핸들러 코드 중복

**Option B (선택) — AuthenticationEntryPoint를 통해 GlobalExceptionHandler 형식으로 통일**
- 비용: 필터에서 throw한 예외를 EntryPoint가 받기 위해 `request.setAttribute` 패턴 도입 필요
- 보상: 인증 실패 응답 형식이 다른 API와 동일

### Refresh Token 화이트리스트 저장소

**Option A — Redis** (v2 검토)
- 장점: TTL 자동 만료 + Read 성능. RT 탈취 감지에 유리
- 보류 이유: 현재 트래픽 규모에서 추가 인프라 비용 정당화 어려움. MySQL + 야간 cleanup으로 충분

**Option B (선택) — MySQL `refresh_entity` + 야간 cleanup 스케줄러**
- 비용: 만료 RT가 한동안 DB에 남음 (최대 1일)
- 보상: 인프라 단순화. 트래픽 증가 시 Redis 전환을 Epic 단위로 분리 가능

## 전체 아키텍처 (High-Level Architecture)

> 컴포넌트 다이어그램과 핵심 플로우 2~3개. 본문 5페이지보다 다이어그램 1장이 더 강하다.

### 컴포넌트 배치

```
[React SPA]
    │
    │  POST /login (id/pw)   |   OAuth2 callback (code)
    ▼
┌──────────────────────────────────────────────────────────┐
│  Controller Layer                                         │
│  ├─ UserController.loginLocal                            │
│  └─ SocialLoginController.socialLogin                    │
└──────────────────────────────────────────────────────────┘
    │
    │  tokenIssuer.issue(user, response)
    ▼
┌──────────────────────────────────────────────────────────┐
│  Common/security/auth/token                              │
│  ├─ TokenIssuer  (★ 단일 발급 진입점 — Story 1-2)        │
│  │     ├─ JWTUtil.createJWT (AT 30m / RT 7d)            │
│  │     ├─ Set-Cookie: access_token (HttpOnly, Strict)   │
│  │     └─ JwtService.addRefresh (DB 화이트리스트)        │
│  └─ Response Body: TokenResponse { refreshToken }        │
└──────────────────────────────────────────────────────────┘
                          │
                          ▼
       ┌──────────────────────────────────────┐
       │  MySQL: refresh_entity                │
       │  (username UNIQUE, 야간 cleanup)      │
       └──────────────────────────────────────┘


[후속 인증 요청 — 자동 쿠키 전송]
    │
    ▼
┌──────────────────────────────────────────────────────────┐
│  Filter Chain                                             │
│  ├─ BlockListFilter        (악성 URL 즉시 404)            │
│  ├─ JWTFilter              (Cookie → JWT → SecurityContext)│
│  │     실패 시 request.setAttribute("authError", code)    │
│  └─ JwtAuthenticationEntryPoint                          │
│        → ResponseBody: { code, message }                 │
└──────────────────────────────────────────────────────────┘
```

### 핵심 플로우

**1. 로그인 (자체 / 소셜 동일 경로)**
```
Client ─POST /login─► Controller ─TokenIssuer.issue─► [원자적 트랜잭션]
                                                       ├─ AT 쿠키 헤더 추가
                                                       ├─ RT 생성
                                                       └─ refresh_entity UPSERT
                                                       
Client ◄─ 200 + Set-Cookie: access_token + Body { refreshToken } ─
```
실패 시 트랜잭션 롤백 → 쿠키 발급 + DB 저장 모두 무효 (Story 1-2 AC).

**2. AT 만료 후 재발급**
```
Client ─POST /jwt/refresh + Body { refreshToken } ─► JwtService.refreshRotate
                                                      ├─ refresh_entity 조회
                                                      ├─ 일치 / 미만료 확인
                                                      ├─ 새 AT 쿠키 발급
                                                      └─ 새 RT 바디 반환
                                                      
실패 케이스별 ErrorCode 분기 (REFRESH_TOKEN_INVALID / NOT_FOUND / REUSED)
```

**3. 인증 실패**
```
JWTFilter 도중 예외 발생 ──► request.setAttribute("authError", ErrorCode)
                              │
                              ▼ (필터 체인 진행 → SecurityContext 비어 있음)
                              │
        AuthenticationEntryPoint.commence()
                              │
                              ▼
        ResponseBody: { code: "AUTH_TOKEN_EXPIRED", message: "..." }
        HTTP 401
```
형식이 다른 API의 GlobalExceptionHandler 응답과 1:1 동일 (Story 3-1, 3-2).

### Out-of-Process 의존

- **MySQL**: `refresh_entity` 테이블 — RT 화이트리스트. UNIQUE(username) 제약으로 동시 rotate 충돌 방지 (Story 2-3)
- **카카오 / 네이버 OAuth2 Provider**: Authorization Code 발급 (Product 2의 `SocialOAuthFlow`가 위임 호출)
- **JVM Scheduler**: `RefreshTokenCleanupScheduler` — 매일 04:00 만료 RT 일괄 삭제 (Story 2-4)

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 인증 실패 시나리오와 응답

| 시나리오 | ErrorCode | HTTP | 클라이언트 권장 동작 |
| --- | --- | --- | --- |
| AT 쿠키 미존재 | `AUTH_TOKEN_MISSING` | 401 | 무인증 영역이면 정상. 인증 영역이면 RT로 재발급 |
| AT 만료 | `AUTH_TOKEN_EXPIRED` | 401 | `/jwt/refresh` 자동 호출 |
| AT 서명 / 형식 불일치 | `AUTH_TOKEN_INVALID` | 401 | 재로그인 강제 (변조 가능성) |
| AT는 유효하지만 유저 삭제됨 | `AUTH_USER_NOT_FOUND` | 401 | 재로그인 (계정 상태 변경) |
| RT 미존재 / 만료 | `REFRESH_TOKEN_INVALID` | 401 | 재로그인 |
| RT가 이미 rotate됨 | `REFRESH_TOKEN_REUSED` | 401 | 재로그인 + 보안 알림 (탈취 의심) |
| TokenIssuer DB 저장 실패 | (500, 트랜잭션 롤백) | 500 | FE가 재로그인 재시도 |

### 로깅 정책

- **항상 기록**: 인증 실패 시 `username + ErrorCode + IP`
- **debug 레벨**: 정상 요청 URI 추적 (`JWTFilter` 진입) — info로 두면 로그 볼륨 폭주
- **절대 금지**: Refresh Token 원문, Access Token 원문, JWT payload 전체, 디버그용 한국어 농담 문자열(`"리프레쉬가 진짜 없지롱"`)

### 관측 지표 (v2 — Metrics 도입 시 합류)

- `auth_login_total{type=local|social, result=success|fail}` — 카운터
- `auth_token_refresh_total{result=success|reused|invalid|not_found}` — 카운터
- `auth_token_refresh_latency_seconds` — 히스토그램 (Rotate DB 비용 추적)
- `refresh_token_cleanup_deleted_count` — 게이지 (스케줄러 산출, 비정상 폭증 시 알림)

## 롤아웃 / 마이그레이션 (Rollout)

### 전제 — 프로덕션 트래픽 없음

현재 사용자 0명. 따라서 **헤더 방식 ↔ 쿠키 방식 병용 기간을 두지 않고 일괄 전환**한다. 만약 사용자가 있었다면:
- 1주차: AT를 쿠키 + 헤더 동시 발급 (FE는 헤더 우선)
- 2주차: FE 배포로 쿠키 우선 전환, 헤더 제거
- 3주차: 서버에서 헤더 발급 중단

이 단계 전환을 생략한 것은 **수용 가능한 단순화 선택**이며, 향후 트래픽 발생 후 동일 변경이 필요할 경우 위 3단계 패턴을 재사용한다.

### Epic 의존성 그래프

```
Epic 1 (토큰 발급 전략 재설계)
  Story 1-1 (TTL 외부화) ──┐
                           ├─► Story 1-2 (TokenIssuer)
                           │       │
                           │       ├─► Story 1-3 (AT HttpOnly Cookie)
                           │       │       │
                           │       │       └─► Story 1-4 (JWTFilter Cookie 추출)
                           │       │
                           │       └─► Story 1-5 (DTO 통합)
                           │
                           └─► Epic 2 / Epic 3 진입 가능
                                      │
       ┌──────────────────────────────┴────────────────────┐
       ▼                                                    ▼
Epic 2 (Rotate 흐름)                          Epic 3 (필터 / 예외 통일)
  Story 2-1 (예외 BusinessException 통일)        Story 3-1 (EntryPoint 도입)
  Story 2-2 (/jwt/exchange 제거)  [Epic1 1-2,1-3]  Story 3-2 (ErrorCode 세분화) [3-1]
  Story 2-3 (Save 무결성)                        Story 3-3 (BlockList 분리)
  Story 2-4 (만료 cleanup 스케줄러)
```

### 환경별 설정 분기

- `application-local.yml`: 쿠키 `Secure=false`, `SameSite=Lax` (로컬 HTTPS 부담 회피)
- `application-prod.yml`: 쿠키 `Secure=true`, `SameSite=Strict`
- `jwt.access-token-ttl` / `jwt.refresh-token-ttl` 누락 시 `BeanCreationException`으로 부팅 실패 — 운영자가 누락을 즉시 인지 (Story 1-1 AC)

### 롤백 계획

각 Story는 단일 PR + 단일 Flyway 마이그레이션 / 코드 변경으로 격리. 문제 발생 시 직전 커밋으로 revert 가능. `refresh_entity` 테이블 스키마 변경은 본 Product 범위에서 없음 — 기존 테이블 재사용.

## 성공 지표 (KPI)

| 지표 | 현재 값 | 목표 값 | 측정 방법 |
| --- | --- | --- | --- |
| 토큰 발급 호출 진입점 수 | 3 (`JWTUtil.createJWT` 직접 호출) | 1 (`TokenIssuer.issue`) | 코드 grep |
| 응답 바디 내 `refreshToken` 필드 노출 | 모든 로그인 경로 | 0 | API 응답 스키마 검사 |
| 인증 실패 응답 형식 일관성 | 2종 (필터 JSON / Handler `{code, message}`) | 1종 | 에러 응답 E2E 테스트 |
| `RuntimeException` 직접 throw (auth 패키지) | 3건 이상 | 0건 | 정적 분석 |
| 토큰 전달 경로 종류 | 3 (바디 / 쿠키 / `/jwt/exchange`) | 2 (AT 쿠키 / RT 바디) | 엔드포인트 명세 검수 |

## Scope

- **In Scope**
    - JWTUtil / TokenIssuer / JwtService / JWTFilter 인프라 레이어
    - Refresh Token 화이트리스트(`RefreshEntity`) 저장·갱신 로직
    - 로그인 응답 DTO (`TokenResponse` / `JWTResponseDTO`) 통합
    - `/jwt/exchange` 엔드포인트 제거 및 OAuth2 성공 핸들러 경로 재설계
    - 쿠키 보안 속성 프로파일 분기 (`local` / `prod`)
- **Out of Scope**
    - Redis 기반 Refresh Token 저장소 전환 → v2
    - 토큰 블랙리스트 / 기기별 세션 관리 → v2
    - OAuth2 제공자 추가(구글 등) → Product 2에서 다룸

## 대상 사용자

- 주요 사용자: Third Tool 백엔드 개발자 / 프론트엔드 개발자
- 사용 맥락: 로그인·토큰 재발급 경로가 흔들리면 전 BC의 API 호출이 영향받으므로 최상위 기반 레이어에 해당

## 연결된 Epic 목록

- [ ]  Epic 1. 토큰 발급 전략 재설계 — TTL 분리 + TokenIssuer 단일화 + 저장 위치 재정의
- [ ]  Epic 2. Refresh Rotate 및 재발급 흐름 견고화
- [ ]  Epic 3. JWT 필터 및 인증 예외 체계 통일

## 관련 문서

- 상위 문서: User 도메인 모델링
- 참고 문서: CardDomainException 패턴 (Card BC 예외 계층)
- 후속 ADR(예정): `ADR-00X: Token Storage Strategy` (AT 쿠키 / RT 메모리 결정 근거)

## 열린 질문 (Open Questions)

> 아직 정해지지 않은 항목. 정직하게 남겨두고, 결정되면 ADR로 이관한다. "다 정해진 척"하는 SDD가 가장 의심스럽다.

- **RT 탈취 감지 후 대응 강도** — `REFRESH_TOKEN_REUSED` 발생 시 해당 username의 모든 활성 세션을 즉시 무효화할지(공격 차단 우선), 알림만 발행할지(UX 보호 우선). v2 보안 정책에서 결정
- **다중 디바이스 세션** — 현재 `refresh_entity`는 username 단위로 1건만 보관. 모바일 + 웹 동시 로그인 시 한쪽이 rotate되면 다른 쪽이 끊긴다. 디바이스별 RT 분리는 v2
- **CSRF 토큰 명시 도입** — `SameSite=Strict`만으로 충분한지, 명시적 CSRF 토큰까지 둘지. 외부 결제 콜백 등 다른 오리진 진입이 생기는 시점에 재검토
- **소셜 제공자별 RT TTL 정책** — 카카오·네이버 정책 변경 시 우리 RT TTL을 따라가야 하는가, 독립 유지인가
- **`auth_*` 메트릭 인프라** — Micrometer + Prometheus를 도입할지, Spring Actuator 노출까지만 갈지. 운영 환경 합류 시점 (Infra Product) 의존

## Epic 1. 토큰 발급 전략 재설계 — TTL 분리 + TokenIssuer 단일화

# Epic 1. 토큰 발급 전략 재설계 — TTL 분리 + TokenIssuer 단일화

## Epic 목표

> Access Token은 30분 TTL의 HttpOnly Cookie로, Refresh Token은 7일 TTL의 응답 바디로
발급되도록 저장 위치와 전달 경로를 재정의하고, 모든 발급 호출을 TokenIssuer 단일 컴포넌트로 수렴시킨다.
>

## 배경

- `JWTUtil.createJWT(username, role, isAccess)` 직접 호출이 `UserController.loginLocal()`,
  `UserService.socialLogin()`, `JwtService.refreshRotate()`, `JwtService.cookie2Header()` 네 곳에 분산되어 있다
- AT·RT 모두 응답 바디로 전달되고 있어, XSS 상황에서 FE 메모리 덤프로 RT가 평문 노출될 수 있다
- 현재 쿠키 발급 경로(`JwtService.cookie2Header`)는 `Secure=false`, `SameSite` 미설정으로 프로덕션 수준에 미달한다
- AT·RT TTL이 Magic Number로 `JWTUtil` 내부에 하드코딩되어 있어 정책 변경 시 추적이 어렵다

## Epic 수준 완료 기준 (Definition of Done)

- [ ]  Access Token TTL 30분, Refresh Token TTL 7일이 `application.yml`로 외부화된다
- [ ]  `TokenIssuer` 단일 컴포넌트에서만 토큰이 발급된다 (`JWTUtil.createJWT` 직접 호출 제거)
- [ ]  로그인 응답에서 `accessToken`이 HttpOnly Cookie로 발급되고, 응답 바디에는 `refreshToken`만 포함된다
- [ ]  쿠키 보안 속성이 프로파일별로 분기된다 (`prod`: `Secure=true`, `SameSite=Strict`)
- [ ]  `TokenResponse` / `JWTResponseDTO` 두 DTO가 단일 DTO로 통합된다
- [ ]  연결된 스토리가 모두 Done 상태다

## 내부 메모 / 제약 사항

- 기술 제약: `setSecure(true)` 전환 시 로컬 개발 환경 HTTPS 설정 필요 — 프로파일별 분기 처리
- 기획 원칙: FE는 RT를 localStorage·sessionStorage에 저장하지 않는다. React 컴포넌트 메모리(상태·변수)에서만 관리한다
- 기획 원칙: AT 쿠키는 `HttpOnly=true`, `SameSite=Strict`, `Path=/`로 고정한다
- 연기된 항목: Refresh Token Rotation 시 기기별 세션 분리 — v2

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

> Product 레벨 "대안 검토"가 큰 갈림길이라면, 이 Epic 안에서 마주친 작은 갈림길의 결정·트레이드오프를 남긴다.

- **`JWTUtil` 정적 메서드 vs `@Component`** — 정적 유지 시 테스트가 어렵고 `JwtProperties` 주입이 불가. `@Component` 전환을 택함. 비용: 기존 호출처 시그니처 변경
- **`isAccess: boolean` vs `TokenType` enum** — boolean은 호출처에서 의미 추적이 어렵고 향후 `REMEMBER_ME` 같은 토큰 종류 추가 시 분기 폭발. enum 선택. 비용: 마이그레이션 시 모든 호출처 수정
- **TokenIssuer 반환값 설계 — `void` vs `TokenResponse`** — `void` + `HttpServletResponse` 부수효과 방식은 테스트 시 응답 mock 필요. 그러나 AT 쿠키 발급이 response 헤더 조작을 강제하므로 `void` + response 주입을 수용. 대신 RT는 명시 반환으로 호출처 가독성 보존
- **쿠키 vs 헤더 병용 전환 기간** — 트래픽 0명이라 일괄 전환 선택. 트래픽이 있었다면 1주차 병용 → 2주차 FE 우선 전환 → 3주차 헤더 제거의 3단계 패턴 적용

## Story 1-1. Access/Refresh Token TTL 외부화 및 JWTUtil 경량화

## Story 1-1. Access/Refresh Token TTL 외부화 및 JWTUtil 경량화

### User Story

> As a **백엔드 개발자**,
I want **토큰 TTL이 application.yml로 분리되고 JWTUtil이 순수 인코딩/디코딩만 담당하길**,
So that **TTL 정책이 바뀔 때 코드 수정 없이 설정만으로 대응할 수 있다**.
>

### 설명

- `application.yml`에 `jwt.access-token-ttl: 30m`, `jwt.refresh-token-ttl: 7d` 도입
- `JwtProperties` `@ConfigurationProperties` 클래스 신설
- `JWTUtil`에서 TTL 하드코딩 제거, 발급 시 `Duration`을 외부에서 받도록 시그니처 변경
  `createJWT(String username, String role, Duration ttl, TokenType type)`
- `TokenType` enum 도입 (`ACCESS`, `REFRESH`) — 현재의 `isAccess: boolean` 대체

### 완료 기준 (Acceptance Criteria)

- [ ]  `application-local.yml` / `application-prod.yml`에 TTL 값이 외부화된다
- [ ]  `JWTUtil`이 정적 메서드가 아닌 `@Component`로 전환되고 `JwtProperties` 주입을 받는다
- [ ]  `isAccess: boolean` 파라미터가 `TokenType` enum으로 교체된다
- [ ]  엣지 케이스: TTL 값이 설정되지 않은 경우 부팅 시 `BeanCreationException`으로 실패한다 (런타임 오류 방지)

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  테스트 코드 작성 완료 (BE: TTL 설정별 만료 시각 검증, TokenType별 발급 검증)
- [ ]  PO 검수 완료
- [ ]  스테이징 배포 확인

### 스토리 포인트

- 추정: 3 SP

```smalltalk

```

## Story 1-2. TokenIssuer 단일 컴포넌트 도입 및 직접 호출 제거

## Story 1-2. TokenIssuer 단일 컴포넌트 도입 및 직접 호출 제거

### User Story

> As a **백엔드 개발자**,
I want **토큰 발급이 단일 진입점에서만 실행되길**,
So that **발급 정책 변경 시 한 곳만 수정하면 되고, 발급 경로별 불일치가 원천 차단된다**.
>

### 설명

- `TokenIssuer` 인터페이스 신설 (`Common/security/auth/token/`)
- `TokenIssuer.issue(UserEntity user, HttpServletResponse response)` 메서드 정의
    - AT 생성 → `Set-Cookie` 헤더로 HttpOnly Cookie 발급
    - RT 생성 → `TokenResponse.refreshToken` 필드로 바디 반환
    - RT DB 화이트리스트 저장 (`jwtService.addRefresh`) 내부화
- 기존 호출자 전환
    - `UserController.loginLocal()` → `tokenIssuer.issue(user, response)`
    - `UserService.socialLogin()` → `tokenIssuer.issue(user, response)`
    - `JwtService.refreshRotate()` → 내부에서 `TokenIssuer` 위임

### 완료 기준 (Acceptance Criteria)

- [ ]  `grep "JWTUtil.createJWT" src/main` 결과가 `TokenIssuer` 구현체 한 곳만 남는다
- [ ]  모든 로그인 경로에서 AT는 Cookie로, RT는 응답 바디로 전달된다
- [ ]  RT DB 화이트리스트 저장이 `TokenIssuer` 내부에서 자동 수행된다 (호출자 인지 불필요)
- [ ]  엣지 케이스: `TokenIssuer` 실행 중 DB 저장 실패 시 Cookie 발급도 롤백된다 (트랜잭션 경계 확인)

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  테스트 코드 작성 완료 (BE: 발급 경로 통합 테스트, 트랜잭션 롤백 테스트)
- [ ]  PO 검수 완료
- [ ]  스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 의존성

- 선행 스토리: Story 1-1

```smalltalk

```

## Story 1-3. Access Token HttpOnly Cookie 발급 및 쿠키 보안 속성 프로파일 분기

```smalltalk

```

## Story 1-4. JWTFilter Cookie 기반 Access Token 추출 전환

## Story 1-4. JWTFilter Cookie 기반 Access Token 추출 전환

### User Story

> As a **인증이 필요한 API를 호출하는 프론트엔드**,
I want **Authorization 헤더를 직접 주입하지 않아도 쿠키로 자동 인증되길**,
So that **FE가 토큰 관리 코드를 작성하지 않아도 되고 보안 경계가 서버에 집중된다**.
>

### 설명

- `JWTFilter`가 `request.getHeader("Authorization")` 대신 `request.getCookies()`에서 `access_token`을 추출
- 기존 Bearer 헤더 기반 인증 경로 제거 (마이그레이션 기간은 Story 1-5 완료 시 함께 종료)
- 헤더 방식·쿠키 방식 병용 기간을 두지 않고 일괄 전환 (현재 프로덕션 트래픽 없음을 전제)

### 완료 기준 (Acceptance Criteria)

- [ ]  `JWTFilter`가 쿠키 배열에서 `access_token` 쿠키를 찾아 JWT를 추출한다
- [ ]  쿠키가 없는 경우 기존과 동일하게 다음 필터로 진행 (익명 요청 처리 경로 유지)
- [ ]  Authorization 헤더가 있어도 무시한다 (혼란 방지)

# 엣지 케이스: 쿠키 값이 JWT 형식이 아닌 경우 `AUTH_TOKEN_INVALID` 에러 응답 (Story 3-3과 연계)

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  테스트 코드 작성 완료 (BE: 쿠키 인증 성공/실패 케이스)
- [ ]  PO 검수 완료
- [ ]  스테이징 배포 확인

### 스토리 포인트

- 추정: 3 SP

### 의존성

- 선행 스토리: Story 1-3

```smalltalk

```

## Story 1-5. TokenResponse / JWTResponseDTO DTO 통합 및 응답 스키마 확정

## Story 1-5. TokenResponse / JWTResponseDTO DTO 통합 및 응답 스키마 확정

### User Story

> As a **FE 개발자**,
I want **로그인 응답 DTO가 단일 타입으로 통일되길**,
So that **BE API 스펙을 신뢰하고 중복 타입에 맞춰 분기 처리를 작성하지 않아도 된다**.
>

### 설명

- `User/dto/TokenResponse` 와 `Common/security/auth/dto/JWTResponseDTO` 중복 존재
- `TokenResponse(String refreshToken)` 단일 DTO로 통합 (AT는 쿠키로 이미 발급되므로 바디에 불필요)
- `Common/security/auth/dto/TokenResponse`로 패키지 이동 (공통 인증 계층 소속)
- `RefreshRequestDTO`는 유지하되, RT가 바디로 받아오므로 `/jwt/refresh`는 기존 구조 유지

### 완료 기준 (Acceptance Criteria)

- [ ]  `JWTResponseDTO`가 제거된다
- [ ]  `TokenResponse`가 `Common/security/auth/dto`에 존재하며 `refreshToken` 필드만 가진다
- [ ]  로그인 응답 JSON이 `{"refreshToken": "..."}` 단일 필드 구조가 된다
- [ ]  엣지 케이스: 기존 `accessToken` 필드를 기대하는 FE 코드가 있으면 Story 1-3에서 쿠키로 전환되었음을 배포 체크리스트에 명시

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  테스트 코드 작성 완료 (BE: 응답 스키마 검증)
- [ ]  PO 검수 완료
- [ ]  스테이징 배포 확인

### 스토리 포인트

- 추정: 2 SP

### 의존성

- 선행 스토리: Story 1-3, Story 1-4

```smalltalk

```

## Epic 2. Refresh Rotate 및 재발급 흐름 견고화

# Epic 2. Refresh Rotate 및 재발급 흐름 견고화

## Epic 목표

> Refresh Token 재발급 흐름의 예외를 BusinessException 계열로 통일하고,
소셜 로그인 이후 쿠키-헤더 변환을 위해 임시로 존재하던 `/jwt/exchange` 엔드포인트를 제거한다.
>

## 배경

- `JwtService.refreshRotate()`에서 `RuntimeException`이 직접 throw된다
- 디버그용 로그 문자열(`"리프레쉬가 진짜 없지롱"`, `"jwt가 이상하지롱"`)이 그대로 운영 환경에 노출된다
- `cookie2Header()`는 "소셜 로그인에서 쿠키로 발급받은 RT를 바디로 옮기는" 임시 브리지로,
  AT 쿠키 + RT 바디 전략이 확정되면 역할이 사라진다
- `refreshRotate()`의 `save-before-delete` 플로우가 `flush()` 없이 실행되면 유니크 제약 충돌이 가능한 구조다

## Epic 수준 완료 기준 (Definition of Done)

- [ ]  `RuntimeException` 직접 throw가 auth 패키지 전체에서 제거된다
- [ ]  재발급 실패 시 `{code, message}` 형식의 응답이 반환된다
- [ ]  `/jwt/exchange` 엔드포인트가 제거된다
- [ ]  소셜 로그인 성공 시 `TokenIssuer`가 직접 AT 쿠키 발급 + RT 바디 반환 수행
- [ ]  `RefreshEntity` 갱신 시 동일 username의 기존 엔티티를 재활용하거나 안전하게 교체한다
- [ ]  연결된 스토리가 모두 Done 상태다

## 내부 메모 / 제약 사항

- 기술 제약: `SocialLoginController`가 현재 `ResponseEntity<TokenResponse>`를 직접 리턴하므로 `HttpServletResponse` 주입 방식으로 전환 필요
- 기획 원칙: Rotate 실패 원인은 반드시 ErrorCode로 구분한다 (쿠키 없음 / 파싱 실패 / DB 미존재)
- 연기된 항목: Refresh Token 탈취 감지(동시 사용 감지 → 전체 세션 무효화) — v2

```smalltalk

```

## Story 2-1. Refresh Rotate 예외 BusinessException 통일 및 디버그 로그 제거

## Story 2-1. Refresh Rotate 예외 BusinessException 통일 및 디버그 로그 제거

### User Story

> As a **API를 호출하는 클라이언트**,
I want **Refresh Token 재발급 실패 시 명확한 ErrorCode로 응답받길**,
So that **FE가 실패 원인별로 적절한 복구 흐름(재로그인 유도 / 토큰 재획득 / 네트워크 재시도)을 분기할 수 있다**.
>

### 설명

- `JwtService.refreshRotate()` 전체를 `BusinessException(ErrorCode)` 기반으로 재작성
- 신규 ErrorCode 추가
    - `REFRESH_TOKEN_INVALID` — JWT 파싱 실패 또는 만료
    - `REFRESH_TOKEN_NOT_FOUND` — DB 화이트리스트 미존재
    - `REFRESH_TOKEN_REUSED` — 만료된 RT를 다시 쓰려는 경우 (탈취 의심)
- 디버그 로그 문자열(`"리프레쉬가 진짜 없지롱"`, `"jwt가 이상하지롱"`) 제거
- `log.info` 수준의 토큰 원문 로깅 제거 (`[REFRESH-ROTATE] 전달받은 RefreshToken: ...`)
  → 보안상 로그 수집 시스템에 RT가 남으면 탈취 경로가 된다

### 완료 기준 (Acceptance Criteria)

- [ ]  `JwtService`에서 `RuntimeException` 직접 throw가 0건이 된다
- [ ]  재발급 실패 시 `GlobalExceptionHandler`가 `{code, message}` 형식으로 응답한다
- [ ]  쿠키 없음 / 토큰 만료 / DB 미존재 / DB-토큰 불일치 네 가지가 각각 다른 ErrorCode로 구분된다
- [ ]  로그에서 RT 원문 문자열이 제거된다 (username만 남김)
- [ ]  엣지 케이스: 이미 rotate된 RT를 재사용하는 경우 `REFRESH_TOKEN_REUSED` 반환

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  테스트 코드 작성 완료 (BE: 실패 케이스별 ErrorCode 검증)
- [ ]  PO 검수 완료
- [ ]  스테이징 배포 확인

### 스토리 포인트

- 추정: 3 SP

```smalltalk

```

## Story 2-2. /jwt/exchange 엔드포인트 제거 및 OAuth2 성공 경로 단일화

## Story 2-2. /jwt/exchange 엔드포인트 제거 및 OAuth2 성공 경로 단일화

### User Story

> As a **백엔드 개발자**,
I want **소셜 로그인도 자체 로그인과 동일한 토큰 발급 경로를 사용하길**,
So that **쿠키-헤더 변환 임시 브리지가 사라지고 인증 경로가 자체/소셜 구분 없이 일원화된다**.
>

### 설명

- `JwtController.jwtExchangeApi` (`POST /jwt/exchange`) 엔드포인트 제거
- `JwtService.cookie2Header()` 메서드 제거
- `SocialLoginController.socialLogin()` 시그니처 변경
    - `ResponseEntity<TokenResponse>` → `TokenResponse` + `HttpServletResponse` 주입
    - `UserService.socialLogin()` 반환값을 `TokenIssuer.issue(user, response)`로 대체
- 소셜 로그인 콜백 이후 즉시 AT 쿠키 + RT 바디 방식으로 응답

### 완료 기준 (Acceptance Criteria)

- [ ]  `/jwt/exchange` 경로 호출 시 `404 Not Found` 반환
- [ ]  카카오·네이버 소셜 로그인 후 응답에 AT 쿠키가 포함되고 RT가 바디로 전달된다
- [ ]  `JwtService`에 쿠키 파싱 관련 로직이 남아 있지 않다
- [ ]  엣지 케이스: 소셜 로그인 중 `TokenIssuer` 실패 시 UserEntity 생성은 유지되지만 응답은 500 (재시도 가능한 상태)

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  테스트 코드 작성 완료 (BE: 소셜 로그인 E2E 테스트, 응답 쿠키/바디 검증)
- [ ]  PO 검수 완료
- [ ]  스테이징 배포 확인

### 스토리 포인트

- 추정: 3 SP

### 의존성

- 선행 스토리: Epic 1 Story 1-2, 1-3

```smalltalk

```

## Story 2-3. RefreshEntity 갱신 무결성 개선 — save-before-delete 충돌 제거

## Story 2-3. RefreshEntity 갱신 무결성 개선 — save-before-delete 충돌 제거

### User Story

> As a **서비스를 운영하는 개발자**,
I want **Refresh Rotate 과정에서 DB 유니크 제약 충돌이 발생하지 않길**,
So that **동시 rotate 요청이 들어와도 에러 없이 일관된 상태가 유지된다**.
>

### 설명

- 현재 `refreshRotate()`는 `username` 기반으로 기존 엔티티를 찾아 `id`를 재사용해 덮어쓰지만,
  `cookie2Header()`는 `removeRefresh(refreshToken)` 후 `save`하는 save-before-delete 플로우 (`flush()` 의존)
- `username`이 유니크 제약이므로 `save`로 UPDATE 처리되는 방식이 안전하다
- `refreshRepository.findEntityByUsername(username).ifPresent(e -> e.updateRefresh(token))`
  형태의 도메인 행위 기반으로 전환 (현재는 엔티티를 매번 `builder()`로 새로 만들고 있음)
- `RefreshEntity`에 `updateRefresh(String newRefreshToken)` 메서드 추가 (dirty checking 활용)

### 완료 기준 (Acceptance Criteria)

- [ ]  `RefreshEntity.updateRefresh()` 도메인 메서드가 추가된다
- [ ]  `JwtService`에서 `RefreshEntity.builder()` 호출이 신규 생성 시에만 사용된다
- [ ]  `refreshRepository.flush()` 명시 호출이 제거된다 (더 이상 필요 없음)
- [ ]  엣지 케이스: 동일 username의 동시 rotate 요청 2건이 들어와도 둘 다 성공 또는 하나만 성공 (유니크 충돌로 500은 발생하지 않음)

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  테스트 코드 작성 완료 (BE: rotate 동시성 테스트, updateRefresh 단위 테스트)
- [ ]  PO 검수 완료
- [ ]  스테이징 배포 확인

### 스토리 포인트

- 추정: 3 SP

```smalltalk

```

## Story 2-4. 만료된 Refresh Token DB 정리 스케줄러

## Story 2-4. 만료된 Refresh Token DB 정리 스케줄러

### User Story

> As a **서비스를 운영하는 개발자**,
I want **만료된 Refresh Token이 주기적으로 DB에서 삭제되길**,
So that **화이트리스트 테이블이 무한히 증가하지 않고 조회 성능이 유지된다**.
>

### 설명

- `RefreshTokenCleanupScheduler` 신설 (`Common/security/auth/jwt/`)
- 매일 새벽 4시 실행
- RT TTL(7일)이 지난 `RefreshEntity`를 일괄 삭제
- `RefreshRepository.deleteByCreatedDateBefore(now().minusDays(7))` 기존 메서드 활용

### 완료 기준 (Acceptance Criteria)

- [ ]  스케줄러가 `@Scheduled(cron = "0 0 4 * * *")`로 등록된다
- [ ]  삭제 건수를 로그로 남긴다 (운영 모니터링용)
- [ ]  `application.yml`에 스케줄러 활성화 프로파일 분기 (`local`에서는 비활성화)
- [ ]  엣지 케이스: 삭제 중 DB 오류 발생 시 다음 날 재시도되도록 예외를 catch 후 로그만 남김

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  테스트 코드 작성 완료 (BE: 스케줄러 수동 트리거 테스트, 삭제 조건 검증)
- [ ]  PO 검수 완료
- [ ]  스테이징 배포 확인

### 스토리 포인트

- 추정: 2 SP

```smalltalk

```

## 

```smalltalk

```

## Epic 3. JWT 필터 및 인증 예외 체계 통일

# Epic 3. JWT 필터 및 인증 예외 체계 통일

## Epic 목표

> JWTFilter의 인증 실패 응답을 GlobalExceptionHandler와 연동해 API 전체의
에러 응답 형식을 단일화하고, 필터 내 임시 처리 로직을 Security Config로 이전한다.
>

## 배경

- `JWTFilter`가 `writeUnauthorizedResponse()`로 JSON을 직접 write한다.
  다른 API는 `GlobalExceptionHandler`를 통해 `{code, message}` 형식을 반환하므로 불일치
- `throw new ServletException("Invalid JWT token format")`은 `GlobalExceptionHandler`가 잡지 못해 500 노출
- 악성 URL 패턴(`.php`, `.aspx`, `/wp-`, `/cgi-bin/`) 차단 로직이 필터 내부에 하드코딩되어 있어
  Security 정책 변경 시 추적이 어렵다
- 디버그 로그(`log.info("[JWTFilter] 요청 URI: {}")`가 매 요청마다 찍혀 로그 볼륨을 과도하게 차지

## Epic 수준 완료 기준 (Definition of Done)

- [ ]  `JWTFilter` 내부에서 응답을 직접 write하는 코드가 제거된다
- [ ]  인증 실패 시 `AuthenticationEntryPoint`를 통해 `{code, message}` 형식으로 응답된다
- [ ]  악성 URL 패턴 차단이 Security Config 또는 별도 `BlockListFilter`로 이동한다
- [ ]  필터 내 과도한 로그가 정리되고, 로그 레벨이 `debug`로 조정된다
- [ ]  연결된 스토리가 모두 Done 상태다

## 내부 메모 / 제약 사항

- 기술 제약: `OncePerRequestFilter`는 예외를 throw해도 Spring Security의 `ExceptionTranslationFilter`가 처리하지 못하는 경우가 있어 `AuthenticationEntryPoint` 연동 필요
- 기획 원칙: 화이트리스트 경로(`/health`, `/actuator`, `/swagger`)는 필터 자체를 우회시키고 인증 실패 응답 대상이 아니다

```smalltalk

```

## Story 3-1. AuthenticationEntryPoint 구현 및 writeUnauthorizedResponse 제거

## Story 3-1. AuthenticationEntryPoint 구현 및 writeUnauthorizedResponse 제거

### User Story

> As a **API를 호출하는 클라이언트**,
I want **인증 실패 응답이 다른 API의 에러 응답과 동일한 형식이길**,
So that **FE의 에러 핸들러가 인증 여부와 무관하게 단일 로직으로 처리할 수 있다**.
>

### 설명

- `JwtAuthenticationEntryPoint implements AuthenticationEntryPoint` 신설
- `commence()` 내부에서 `BusinessException` 정보를 꺼내 `{code, message}` JSON 응답 생성
- `JWTFilter`는 예외 상황에서 `SecurityContext`를 비우고 `throw new BusinessException(...)` 또는
  `request.setAttribute("exception", ex)` 후 다음 필터로 진행
- `SecurityConfig`에서 `.exceptionHandling(e -> e.authenticationEntryPoint(jwtAuthenticationEntryPoint))` 등록

### 완료 기준 (Acceptance Criteria)

- [ ]  `JWTFilter`에서 `response.getWriter().write(...)` 호출이 제거된다
- [ ]  인증 실패 시 응답 형식이 `{"code": "AUTH_TOKEN_EXPIRED", "message": "..."}` 형태로 통일된다
- [ ]  `ServletException` 직접 throw가 제거된다
- [ ]  엣지 케이스: 토큰이 아예 없는 경우와 만료된 경우가 각각 다른 ErrorCode로 응답된다

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  테스트 코드 작성 완료 (BE: 인증 실패 케이스별 응답 검증)
- [ ]  PO 검수 완료
- [ ]  스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

```smalltalk

```

## Story 3-2. 인증 실패 ErrorCode 세분화

## Story 3-2. 인증 실패 ErrorCode 세분화

### User Story

> As a **FE 개발자**,
I want **인증 실패 원인을 ErrorCode로 구분받길**,
So that **토큰 만료와 토큰 누락을 다르게 처리해 UX(재로그인 vs 자동 재발급)를 분기할 수 있다**.
>

### 설명

- 신규 ErrorCode
    - `AUTH_TOKEN_MISSING` — 쿠키 자체가 없음
    - `AUTH_TOKEN_EXPIRED` — JWT 만료
    - `AUTH_TOKEN_INVALID` — JWT 파싱 실패 / 서명 불일치
    - `AUTH_USER_NOT_FOUND` — JWT는 유효하지만 DB에서 유저 삭제됨
- `JWTFilter`가 각 조건별로 다른 ErrorCode를 `AuthenticationEntryPoint`에 전달

### 완료 기준 (Acceptance Criteria)

- [ ]  네 가지 실패 케이스가 각각 다른 ErrorCode로 응답된다
- [ ]  HTTP status는 모두 `401 Unauthorized`로 통일 (ErrorCode가 세부 원인 구분)
- [ ]  엣지 케이스: JWT 서명이 바뀐 경우 (secret key 변경) `AUTH_TOKEN_INVALID`로 분류

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  테스트 코드 작성 완료 (BE: ErrorCode별 시나리오 테스트)
- [ ]  PO 검수 완료
- [ ]  스테이징 배포 확인

### 스토리 포인트

- 추정: 3 SP

### 의존성

- 선행 스토리: Story 3-1

```smalltalk

```

## Story 3-3. 악성 URL 패턴 차단 필터 분리 및 로그 레벨 조정

## Story 3-3. 악성 URL 패턴 차단 필터 분리 및 로그 레벨 조정

### User Story

> As a **백엔드 개발자**,
I want **JWTFilter가 JWT 인증만 담당하고 악성 URL 차단은 분리된 필터에서 처리되길**,
So that **필터 별 책임이 명확해지고 정책 변경 시 수정 포인트가 단일화된다**.
>

### 설명

- `BlockListFilter` 신설 — `.php`, `.aspx`, `/wp-`, `/cgi-bin/` 패턴 차단
- `SecurityConfig`에서 `BlockListFilter`를 `JWTFilter` 앞에 등록
- `JWTFilter`의 `log.info("[JWTFilter] 요청 URI: ...")` → `log.debug(...)`로 격하
- 화이트리스트 경로 매칭 실패 시에만 로그 출력하도록 축소

### 완료 기준 (Acceptance Criteria)

- [ ]  `JWTFilter`에서 악성 URL 차단 로직이 제거된다
- [ ]  `BlockListFilter`가 JWT 필터보다 먼저 실행되고 404를 즉시 반환한다
- [ ]  `JWTFilter`의 정상 요청당 로그 출력이 `info` → `debug`로 조정된다
- [ ]  엣지 케이스: 차단 패턴이 `application.yml`로 외부화되어 신규 패턴 추가 시 배포 없이 반영 가능 (선택)

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  테스트 코드 작성 완료 (BE: 악성 URL 차단 테스트, 필터 순서 검증)
- [ ]  PO 검수 완료
- [ ]  스테이징 배포 확인

### 스토리 포인트

- 추정: 2 SP

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

# 

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

# [Product ] User BC 도메인 정합성 개선

# [Product 2] User BC 도메인 정합성 개선

## Product Vision

> UserService에 혼재된 OAuth2 사용자 로딩·JWT 발급·도메인 로직을 역할별로 분리하고,
예외 처리와 인증 주체 주입 방식을 User BC 전반에서 단일화한다.
>

## 배경 및 문제

- 현재 상황 (As-Is)
    - `UserService extends DefaultOAuth2UserService` — OAuth2 로딩, 자체 로그인, 소셜 로그인, 정보 수정, JWT 발급을 한 클래스가 담당
    - `IllegalArgumentException`, `UsernameNotFoundException`, `RuntimeException`이 혼용되어 응답 형식 불일치
    - `@AuthenticationPrincipal`이 `String username`과 `UserEntity user` 두 타입으로 혼용
    - `SecurityContextHolder.getContext().getAuthentication()`이 `deleteUser()` 내부에서 직접 호출됨
    - `UserUpdateRequestDTO`에 `username` 필드가 포함되어 수정 대상을 요청 바디로 받는 구조
    - `loadUser()`가 카카오·네이버 원본 응답 파싱을 한 메서드에서 직접 처리
    - `UserService`가 `kakaoMemberRepository` / `naverMemberRepository`를 직접 참조
- 발생하는 문제
    - `UserService`의 단위 테스트 단위가 없다 (의존성 폭발)
    - 신규 소셜 제공자(구글 등) 추가 시 `UserService.loadUser()`와 `socialLogin()` 두 곳을 동시에 수정해야 한다
    - 에러 응답 형식이 API마다 달라 FE 분기 처리 부담이 크다
    - 수정 대상자를 요청 바디의 `username`으로 받는 구조는 우회로 타인 계정 수정 시도를 허용할 수 있다
- 왜 지금 해결해야 하는가
    - Product 1 (인증 인프라)이 정돈된 상태에서 User BC를 리팩토링해야 `TokenIssuer`, `ErrorCode` 등 의존 타입이 안정적으로 존재한다
    - 포트폴리오 관점에서 User BC는 면접관이 가장 먼저 살펴보는 영역이다. SRP 위반 상태 그대로는 설계 역량을 어필하기 어렵다

## 목표 (To-Be)

- OAuth2 사용자 로딩이 `CustomOAuth2UserService` 독립 컴포넌트에서 실행된다
- 제공자별 파싱이 `SocialUserInfoParser` 인터페이스 구현체로 분리된다
- `UserService`가 `UserCommandService` / `UserQueryService`로 분리된다
- 모든 User 도메인 예외가 `BusinessException(ErrorCode)` 계층을 사용한다
- `@AuthenticationPrincipal`이 `UserEntity` 단일 타입으로 통일된다
- Service 레이어에서 `SecurityContextHolder` 직접 참조가 제거된다

## 성공 지표 (KPI)

| 지표 | 현재 값 | 목표 값 | 측정 방법 |
| --- | --- | --- | --- |
| UserService 공개 메서드 수 | 9개 | Command 4 + Query 3 | 클래스 분리 후 Count |
| 소셜 제공자 추가 시 수정 클래스 수 | 4곳 (Service 2, Client, Repo) | 1곳 (신규 Parser + 설정만) | 신규 구글 파서 PoC |
| User 도메인 내 RuntimeException 계열 직접 throw | 3건 | 0건 | 정적 분석 |
| `@AuthenticationPrincipal` 타입 종류 | 2종 (String, UserEntity) | 1종 (UserEntity) | 코드 grep |
| SecurityContextHolder Service 레이어 참조 | 1건 | 0건 | 코드 grep |

## Scope

- **In Scope**
    - `UserService` 책임 분리 및 Command/Query 분리
    - OAuth2 파싱 로직 제공자별 추상화
    - User 도메인 예외 체계 통일
    - 인증 주체 주입 방식 단일화
    - `UserUpdateRequestDTO` 등 DTO 정리
- **Out of Scope**
    - 신규 소셜 제공자(구글, 애플 등) 추가 → v2
    - 회원 탈퇴 시 연관 데이터(Deck, Card) 정리 정책 → v2
    - 2FA / 이메일 인증 → v2

## 대상 사용자

- 주요 사용자: Third Tool 백엔드 개발자 (유지보수 및 확장 시)
- 사용 맥락: 소셜 제공자 추가 / 인증 정책 변경 / 유저 속성 확장 시 수정 포인트가 최소화되어야 함

## 연결된 Epic 목록

- [ ]  Epic 4. UserService 책임 분리 — OAuth2UserService 독립 및 파싱 추상화
- [ ]  Epic 5. User 도메인 예외 및 인증 주체 통일

## 관련 문서

- 상위 문서: User 도메인 모델링
- 선행 Product: Product 1 (인증 인프라 재설계)
- 참고 문서: Card BC 예외 계층 (`CardDomainException` 패턴)

## Epic 4. UserService 책임 분리 — OAuth2UserService 독립 및 파싱 추상화

# Epic 4. UserService 책임 분리 — OAuth2UserService 독립 및 파싱 추상화

## Epic 목표

> OAuth2 사용자 로딩·제공자별 파싱·소셜 멤버 저장·유저 도메인 유스케이스를 각각 독립 컴포넌트로 분리해
신규 소셜 제공자 추가가 단일 클래스 추가만으로 가능한 구조를 만든다.
>

## 배경

- `UserService`가 `DefaultOAuth2UserService`를 상속해 인증 프레임워크 계층과 도메인 계층을 한 클래스에서 처리
- `loadUser()` 내부에 `"kakao".equals(registrationId)` / `"naver".equals(registrationId)` 분기가 쌓여 있어
  제공자 추가 시마다 분기가 누적되는 구조
- `socialLogin()`이 `kakaoMemberRepository` / `naverMemberRepository`를 직접 호출해 확장성이 없다

## Epic 수준 완료 기준 (Definition of Done)

- [ ]  `UserService`가 `DefaultOAuth2UserService`를 상속하지 않는다
- [ ]  `CustomOAuth2UserService`가 독립 컴포넌트로 존재한다
- [ ]  `SocialUserInfoParser` 인터페이스와 제공자별 구현체(`KakaoUserInfoParser`, `NaverUserInfoParser`)가 분리된다
- [ ]  `SocialMemberRegistrar` 도메인 서비스가 제공자별 저장 책임을 캡슐화한다
- [ ]  `UserCommandService` / `UserQueryService`가 분리되고 각각 단위 테스트 가능하다
- [ ]  연결된 스토리가 모두 Done 상태다

## 내부 메모 / 제약 사항

- 기술 제약: `CustomOAuth2UserService`는 `OAuth2UserService<OAuth2UserRequest, OAuth2User>` 인터페이스를 구현해야 한다
- 기획 원칙: 제공자별 파싱 결과는 `SocialUserInfo` 값 객체로 통일한다
- 연기된 항목: `SocialMember` Aggregate 재설계 (복수 계정 연동 정책) — v2

```smalltalk

```

## Story 4-1. CustomOAuth2UserService 독립 추출

## Story 4-1. CustomOAuth2UserService 독립 추출

### User Story

> As a **백엔드 개발자**,
I want **OAuth2 사용자 로딩 책임이 UserService에서 분리된 독립 컴포넌트로 이동되길**,
So that **UserService가 순수한 유저 도메인 유스케이스만 담당하고 Security 컨텍스트 없이 단위 테스트할 수 있다**.
>

### 설명

- `CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User>` 신규
- 내부에 `DefaultOAuth2UserService delegate`를 보유해 소셜 제공자 API 호출 위임
- `UserService extends DefaultOAuth2UserService` 상속 제거
- `loadUser()` 메서드가 `UserService`에서 `CustomOAuth2UserService`로 이동

### 완료 기준 (Acceptance Criteria)

- [ ]  `UserService`에서 `extends DefaultOAuth2UserService` 제거된다
- [ ]  `CustomOAuth2UserService.loadUser()`가 `CustomOAuth2User` 반환
- [ ]  `SecurityConfig`에서 `oauth2Login`의 `userInfoEndpoint`에 `CustomOAuth2UserService`가 주입된다
- [ ]  엣지 케이스: `delegate.loadUser()` 실패 시 `OAuth2AuthenticationException`을 그대로 전파한다

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  테스트 코드 작성 완료 (BE: `CustomOAuth2UserService` 단위 테스트, Mockito `delegate` Mock)
- [ ]  PO 검수 완료
- [ ]  스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

```smalltalk

```

## Story 4-2. SocialUserInfoParser 제공자별 분리

## Story 4-2. SocialUserInfoParser 제공자별 분리

### User Story

> As a **백엔드 개발자**,
I want **카카오·네이버 파싱 로직이 독립 컴포넌트로 분리되길**,
So that **신규 제공자(구글 등) 추가 시 파서 하나만 추가하면 되는 OCP 구조가 확보된다**.
>

### 설명

- `SocialUserInfoParser` 인터페이스 정의
    - `SocialProviderType getProviderType()`
    - `SocialUserInfo parse(Map<String, Object> attributes)`
- `KakaoUserInfoParser`, `NaverUserInfoParser` 구현체 추가
- `SocialUserInfo(String socialId, String nickname, String email, SocialProviderType provider)` 값 객체 도입
- `CustomOAuth2UserService`가 `Map<SocialProviderType, SocialUserInfoParser>`를 주입받아 `providerType`으로 분기

### 완료 기준 (Acceptance Criteria)

- [ ]  `CustomOAuth2UserService.loadUser()` 내부에 `"kakao".equals(...)` / `"naver".equals(...)` 분기가 제거된다
- [ ]  각 Parser가 `@Component`로 등록되어 `Map<SocialProviderType, SocialUserInfoParser>`로 자동 주입된다
- [ ]  `SocialUserInfo` 값 객체가 파싱 결과 표준 타입이 된다
- [ ]  엣지 케이스: 지원하지 않는 `registrationId`로 요청이 오면 `SOCIAL_PROVIDER_NOT_SUPPORTED` ErrorCode 반환

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  테스트 코드 작성 완료 (BE: Parser별 단위 테스트, 미지원 제공자 케이스 테스트)
- [ ]  PO 검수 완료
- [ ]  스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 의존성

- 선행 스토리: Story 4-1

```smalltalk

```

## Story 4-3. SocialMemberRegistrar 도메인 서비스 추출

## Story 4-3. SocialMemberRegistrar 도메인 서비스 추출

### User Story

> As a **백엔드 개발자**,
I want **소셜 멤버 저장 로직이 UserService가 아닌 도메인 서비스에서 처리되길**,
So that **UserCommandService가 KakaoMemberRepository / NaverMemberRepository를 직접 알지 않아도 된다**.
>

### 설명

- `SocialMemberRegistrar` 도메인 서비스 신설
    - `register(UserEntity user, SocialUserInfo info)` — 제공자 타입에 따라 올바른 SocialMember 저장
- `UserCommandService`에서 `kakaoMemberRepository` / `naverMemberRepository` 직접 참조 제거
- 향후 구글 제공자 추가 시 `SocialMemberRegistrar` 내부 `switch`에만 분기 추가하면 된다

### 완료 기준 (Acceptance Criteria)

- [ ]  `UserCommandService`가 `KakaoMemberRepository` / `NaverMemberRepository`를 의존하지 않는다
- [ ]  `SocialMemberRegistrar.register()`가 `SocialProviderType`에 따라 적절한 구체 엔티티를 생성·저장한다
- [ ]  소셜 유저 등록 중 DB 오류 시 UserEntity 생성도 롤백된다 (단일 트랜잭션)
- [ ]  엣지 케이스: 이미 `socialId`가 등록된 경우 `SOCIAL_MEMBER_ALREADY_LINKED` ErrorCode 반환

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  테스트 코드 작성 완료 (BE: Registrar 단위 테스트, 제공자별 저장 검증)
- [ ]  PO 검수 완료
- [ ]  스테이징 배포 확인

### 스토리 포인트

- 추정: 3 SP

### 의존성

- 선행 스토리: Story 4-2

```smalltalk

```

## Story 4-4. UserCommandService / UserQueryService 분리

## Story 4-4. UserCommandService / UserQueryService 분리

### User Story

> As a **백엔드 개발자**,
I want **UserService가 Command / Query로 분리되길**,
So that **읽기 전용 트랜잭션이 명확해지고 각 서비스가 단독 테스트 가능해진다**.
>

### 설명

- `UserCommandService` — `addUser`, `updateUser`, `deleteUser`, `findOrRegisterSocialUser`, `loginLocal`
- `UserQueryService` — `existUser`, `readUser`
- 각각 `@Transactional`, `@Transactional(readOnly = true)` 적용
- `CustomOAuth2UserService`는 `UserCommandService.findOrRegisterSocialUser()`에 위임
- 레거시 `UserService` 클래스 제거

### 완료 기준 (Acceptance Criteria)

- [ ]  `UserCommandService` / `UserQueryService`가 분리되고 각각 독립 테스트 가능하다
- [ ]  `UserController` / `SocialLoginController` / `CustomOAuth2UserService`의 주입 타입이 목적에 맞게 분기된다
- [ ]  `UserService` 클래스가 완전히 제거된다
- [ ]  엣지 케이스: `readUser()`가 읽기 전용 트랜잭션에서 실행되어 dirty checking이 발생하지 않는다

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  테스트 코드 작성 완료 (BE: `UserCommandService` / `UserQueryService` 단위 테스트)
- [ ]  PO 검수 완료
- [ ]  스테이징 배포 확인

### 스토리 포인트

- 추정: 5 SP

### 의존성

- 선행 스토리: Story 4-1, 4-2, 4-3

```smalltalk

```

## 

```smalltalk

```

## Epic 5. User 도메인 예외 및 인증 주체 통일

# Epic 5. User 도메인 예외 및 인증 주체 통일

## Epic 목표

> User 도메인 예외를 BusinessException(ErrorCode) 계층으로 통일하고,
`@AuthenticationPrincipal`을 `UserEntity` 단일 타입으로 고정하며,
Service 레이어에서 SecurityContextHolder 직접 참조를 완전히 제거한다.
>

## 배경

- User 도메인에 `IllegalArgumentException`, `UsernameNotFoundException`, `AccessDeniedException`, `RuntimeException`이 혼용
- `UserController.userMeApi`는 `@AuthenticationPrincipal UserEntity user`, `updateUserApi`는 `@AuthenticationPrincipal String username`으로 타입 불일치
- `UserService.deleteUser()`가 `SecurityContextHolder.getContext().getAuthentication()`을 직접 호출해 Service 단위 테스트에서 Security Mock이 강제됨
- `UserUpdateRequestDTO`에 `username` 필드가 포함되어 수정 대상을 요청 바디로 받는 구조

## Epic 수준 완료 기준 (Definition of Done)

- [ ]  User 도메인 전체에서 `IllegalArgumentException` / `UsernameNotFoundException` 직접 throw가 제거된다
- [ ]  `UserDomainException extends BusinessException` 계층이 도입된다
- [ ]  `@AuthenticationPrincipal`이 모든 Controller에서 `UserEntity` 단일 타입으로 통일된다
- [ ]  Service 레이어에서 `SecurityContextHolder` 직접 참조가 제거된다
- [ ]  `UserUpdateRequestDTO`에서 `username` 필드가 제거된다
- [ ]  연결된 스토리가 모두 Done 상태다

## 내부 메모 / 제약 사항

- 기술 제약: `JWTFilter`가 이미 `UsernamePasswordAuthenticationToken(user, ...)`로 `UserEntity`를 Principal로 넣고 있으므로 `@AuthenticationPrincipal UserEntity` 주입은 기술적으로 이미 가능
- 기획 원칙: 권한 검증(ADMIN 여부)은 `@PreAuthorize("hasRole('ADMIN') or #dto.username == authentication.name")` 방식으로 Security 레이어에서 처리

```smalltalk

```

## Story 5-1. User 도메인 BusinessException 계층 통일

## Story 5-1. User 도메인 BusinessException 계층 통일

### User Story

> As a **API를 호출하는 클라이언트**,
I want **User 도메인 오류가 일관된 ErrorCode로 응답되길**,
So that **중복 가입 / 비밀번호 불일치 / 잠긴 계정 / 미존재 유저를 구분해 다른 UX로 안내할 수 있다**.
>

### 설명

- `UserDomainException extends BusinessException` 도입 (Card BC `CardDomainException` 패턴 준용)
- 신규 ErrorCode
    - `USER_ALREADY_EXISTS` / `USER_NOT_FOUND` / `USER_LOCKED`
    - `PASSWORD_NOT_MATCHED` / `USER_IS_SOCIAL` (소셜 유저가 자체 로그인 시도)
    - `SOCIAL_PROVIDER_NOT_SUPPORTED` / `SOCIAL_MEMBER_ALREADY_LINKED`
- `IllegalArgumentException("이미 유저가 존재합니다.")` → `UserDomainException(USER_ALREADY_EXISTS)`
- `UsernameNotFoundException(...)` → `UserDomainException(USER_NOT_FOUND)`

### 완료 기준 (Acceptance Criteria)

- [ ]  User 도메인에서 `IllegalArgumentException` / `UsernameNotFoundException` 직접 throw가 0건이다
- [ ]  모든 User 예외가 `GlobalExceptionHandler`를 통해 `{code, message}` 형식으로 응답된다
- [ ]  로그인 실패가 `USER_NOT_FOUND`와 `PASSWORD_NOT_MATCHED`로 구분된다 (단 보안상 동일 메시지 반환)
- [ ]  엣지 케이스: 소셜 유저가 자체 로그인 엔드포인트로 요청 시 `USER_IS_SOCIAL` 응답

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  테스트 코드 작성 완료 (BE: 에러 케이스별 ErrorCode 검증)
- [ ]  PO 검수 완료
- [ ]  스테이징 배포 확인

### 스토리 포인트

- 추정: 3 SP

```smalltalk

```

## Story 5-2. @AuthenticationPrincipal UserEntity 단일 타입 통일

## Story 5-2. @AuthenticationPrincipal UserEntity 단일 타입 통일

### User Story

> As a **백엔드 개발자**,
I want **@AuthenticationPrincipal이 UserEntity 단일 타입으로 고정되길**,
So that **Controller 코드를 볼 때 주입 값이 무엇인지 즉시 파악되고 타입 캐스팅 실수가 사라진다**.
>

### 설명

- 모든 Controller에서 `@AuthenticationPrincipal String username` → `@AuthenticationPrincipal UserEntity currentUser`로 변경
- Service 시그니처 변경: `updateUser(String username, dto)` → `updateUser(UserEntity currentUser, dto)`
- `deleteUser(String username, dto)` → `deleteUser(UserEntity currentUser, dto)`
- `userMeApi(@AuthenticationPrincipal UserEntity user)`는 이미 `UserEntity` 타입 — 그대로 유지

### 완료 기준 (Acceptance Criteria)

- [ ]  `grep "@AuthenticationPrincipal String"` 결과가 0건이다
- [ ]  모든 Controller에서 `@AuthenticationPrincipal UserEntity currentUser` 형태로 통일된다
- [ ]  Service 시그니처가 `UserEntity` 기반으로 변경된다
- [ ]  엣지 케이스: 미인증 요청으로 인증 필요 API 접근 시 `AUTH_TOKEN_MISSING` ErrorCode 응답

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  테스트 코드 작성 완료 (BE: 인증 주체 주입 검증)
- [ ]  PO 검수 완료
- [ ]  스테이징 배포 확인

### 스토리 포인트

- 추정: 3 SP

### 의존성

- 선행 스토리: Epic 1 완료 (JWTFilter가 UserEntity 기반 Principal을 설정하는지 확인)

```smalltalk

```

## Story 5-3. SecurityContextHolder Service 레이어 참조 제거

## Story 5-3. SecurityContextHolder Service 레이어 참조 제거

### User Story

> As a **백엔드 개발자**,
I want **Service가 SecurityContextHolder를 직접 참조하지 않길**,
So that **Service 단위 테스트에서 Security 설정이 불필요해지고 인증 컨텍스트 접근 책임이 Controller/Security 레이어에 집중된다**.
>

### 설명

- `UserService.deleteUser()`의 `SecurityContextHolder.getContext().getAuthentication()` 제거
- 권한 검증은 Controller에서 `@PreAuthorize("hasRole('ADMIN') or #currentUser.username == #dto.username")` 또는
  Controller가 `currentUser`와 `dto.username` 비교 후 Service 호출
- `AccessDeniedException` throw는 Spring Security의 `AccessDecisionManager` 흐름에 위임

### 완료 기준 (Acceptance Criteria)

- [ ]  `grep "SecurityContextHolder"` 결과가 Common/security 패키지 외에서는 0건이다
- [ ]  `UserCommandService.deleteUser()`가 `UserEntity currentUser`만 받아서 비즈니스 로직 수행한다
- [ ]  본인 / 관리자 외 계정 삭제 시도 시 `AccessDeniedException` → `AUTH_FORBIDDEN` ErrorCode 응답
- [ ]  엣지 케이스: SecurityContext가 비어 있는 상태에서 인증 필요 API 접근 시 Story 3-2의 `AUTH_TOKEN_MISSING` 흐름과 일관되게 처리

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  테스트 코드 작성 완료 (BE: 권한 검증 테스트 — Security 설정 없이 Service 단위 테스트 가능 검증)
- [ ]  PO 검수 완료
- [ ]  스테이징 배포 확인

### 스토리 포인트

- 추정: 3 SP

### 의존성

- 선행 스토리: Story 5-2

```smalltalk

```

## Story 5-4. UserUpdateRequestDTO 정리 및 UserRequestDTO 제거

## Story 5-4. UserUpdateRequestDTO 정리 및 UserRequestDTO 제거

### User Story

> As a **보안을 고려하는 개발자**,
I want **수정 대상자를 요청 바디가 아닌 인증 주체에서 결정하길**,
So that **타인의 username을 바디에 넣어 타 계정을 수정하려는 시도가 원천 차단된다**.
>

### 설명

- `UserUpdateRequestDTO`에서 `username` 필드 제거
- `UserRequestDTO` (현재 미사용 필드 `isSocial`, `provider` 포함 통합 DTO) 제거
- `UserDeleteRequestDTO`는 유지 (관리자가 특정 유저를 삭제하는 경우 `dto.username`이 필요)
    - 단, 본인 삭제 시에는 `currentUser.username` 사용하고 `dto` 없이 처리하는 별도 엔드포인트 고려

### 완료 기준 (Acceptance Criteria)

- [ ]  `UserUpdateRequestDTO`가 `nickname`, `email`만 포함한다
- [ ]  `UserRequestDTO`가 제거된다
- [ ]  수정 API가 `currentUser.username`을 수정 대상으로 사용한다
- [ ]  엣지 케이스: 관리자가 타 유저를 삭제하는 플로우가 ADMIN 권한 검증과 함께 동작한다

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  테스트 코드 작성 완료 (BE: 수정 대상자 주입 검증, 관리자 삭제 플로우 검증)
- [ ]  PO 검수 완료
- [ ]  스테이징 배포 확인

### 스토리 포인트

- 추정: 2 SP

### 의존성

- 선행 스토리: Story 5-2, 5-3

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

# 

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```

## 

```smalltalk

```