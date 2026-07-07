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

## 설계 결정 (Design Decisions)

> User BC를 SRP·OCP 관점에서 분해하면서 마주친 큰 갈림길의 결정. 거부된 옵션도 합리적 근거가 있었음을 명시해 트레이드오프를 드러낸다.

- **OAuth2 로딩 분리: `UserService extends DefaultOAuth2UserService` 폐기 → Controller가 `SocialOAuthFlow` 직접 호출**
  - Spring Security `oauth2Login` 자동 흐름을 쓰지 않고, `SocialLoginController`가 `SocialOAuthFlow`를 호출하는 명시적 흐름으로 단순화.
  - `CustomOAuth2UserService` 어댑터(Story-4-1 원안)는 OAuth2 표준 진입점을 사용하지 않는 본 프로젝트 구조에서 dead code로 판명되어 폐기.
- **제공자 추가 OCP: `Map<SocialProviderType, SocialOAuthFlow>` + `Map<SocialProviderType, SocialMemberFactory>` 자동 주입**
  - 신규 제공자(구글 등)는 `SocialOAuthFlow` 구현체 1개 + `SocialMemberFactory` 구현체 1개를 `@Component`로 등록하면 끝. `SocialLoginController` / `UserCommandService` / `SocialMemberRegistrar` 무변경.
  - 두 Map은 `List<...>` 주입 후 `getProviderType()`을 key로 collect하는 동일 패턴을 공유.
- **소셜 멤버 저장 책임을 도메인 서비스로 격상: `SocialMemberRegistrar` + `SocialMemberFactory`**
  - `UserCommandService`가 `KakaoMemberRepository` / `NaverMemberRepository`를 직접 알지 않는다. Card BC의 `CardStatusHistoryAppender` 패턴 답습.
  - 중복 `socialId` 사전 검증 + DB UNIQUE 제약으로 이중 방어 (conventions.md §1.6).
- **Command/Query 분리: `UserCommandService` + `UserQueryService`**
  - Query 측에 클래스 레벨 `@Transactional(readOnly = true)` 적용으로 dirty checking 차단을 정적 보장.
  - 단일 `UserService` 대비 의존성·트랜잭션 경계가 명확해져 단위 테스트 단위가 작아진다.
- **예외 체계 단일화: `UserDomainException extends BusinessException`**
  - `IllegalArgumentException` / `UsernameNotFoundException` / `RuntimeException` 직접 throw 0건 목표.
  - 정적 팩토리 `UserDomainException.of(ErrorCode)`만 외부 진입점. 응답 형식은 `GlobalExceptionHandler`가 `{code, message}`로 통일.
- **인증 주체 주입 타입: `@AuthenticationPrincipal UserEntity currentUser` 단일 타입**
  - `JWTFilter`가 이미 `UsernamePasswordAuthenticationToken(user, ...)`에 `UserEntity`를 Principal로 넣고 있으므로 기술적 변경 비용이 작다.
  - `String username` 주입 시 Controller마다 추가 `userRepository.findByUsername` 호출이 누적되는 문제를 제거.
- **권한 검증 위치: Controller에서 `currentUser` vs `dto.username` + `isAdmin` 분기**
  - Story-5-3 사용자 합의 옵션 B. Service는 `SecurityContextHolder`를 모르고 순수 비즈니스 로직만. 권한 검증은 Controller에서 명시적으로 처리.
  - `@PreAuthorize` SpEL 도입은 보류 (테스트 복잡도 + `@EnableMethodSecurity` 부담). ADR 후보로 검토 중.
- **수정 대상자 결정: `currentUser.username` (Principal 기반)**
  - 요청 바디의 `dto.username`로 수정 대상을 결정하던 구조는 타 계정 우회 수정 표면을 만든다. Story-5-4에서 `UserUpdateRequestDTO.username` 제거.

## 대안 검토 (Alternatives Considered)

> 큰 갈림길마다 "왜 이것이 아니고 저것인가"를 남긴다. 거부된 안에도 합리적 근거가 있었음을 보여 현재 선택의 트레이드오프를 명확히 한다.

### OAuth2 로딩 진입점

**Option A — `UserService extends DefaultOAuth2UserService` 유지**
- 장점: Spring Security `oauth2Login` 표준 흐름과 정합. `userInfoEndpoint(userService)`만 설정하면 프레임워크가 인증·세션을 자동 처리.
- 거부 이유:
    - OAuth2 표준 진입점(`/oauth2/authorization/{provider}`)을 본 프로젝트는 사용하지 않음 — FE가 Authorization Code를 직접 받아 백엔드 `/social/login/{provider}`에 POST하는 흐름.
    - 결과적으로 `loadUser()`가 한 번도 실행되지 않는 dead code가 됨에도 SRP 위반(OAuth2UserService + 도메인 로직)은 그대로 남는다.

**Option B — `CustomOAuth2UserService` 독립 추출 + `userInfoEndpoint`에 주입**
- 장점: SRP는 해결. 표준 OAuth2 흐름 사용 시 정공법.
- 거부 이유: Option A와 동일한 dead code 문제. 표준 흐름을 사용하지 않는 한 어댑터를 만드는 비용만 늘어난다.

**Option C (선택) — `SocialLoginController`가 `SocialOAuthFlow` 직접 호출**
- 비용: Spring Security `oauth2Login` 자동화 미사용. CSRF·state 검증을 직접 구현해야 함.
- 보상: 흐름이 명시적이고 디버깅 용이. 표준 OAuth2 진입점에 의존하지 않으므로 FE가 어디서든 Authorization Code를 받아 백엔드에 전달 가능.

### 제공자 추가 OCP 패턴

**Option A — `if/switch (registrationId)` 분기 누적**
- 거부 이유: 제공자 추가 시 `UserService.loadUser()` + `socialLogin()` 두 곳을 동시 수정. SRP·OCP 모두 위반.

**Option B (선택) — `Map<SocialProviderType, SocialOAuthFlow>` + `Map<SocialProviderType, SocialMemberFactory>`**
- 비용: 동일 `SocialProviderType` 키 중복 시 런타임 실패. `SocialMemberRegistrar` 생성자에서 `IllegalStateException`으로 fail-fast.
- 보상: 신규 제공자는 2개 `@Component` 등록만으로 완결. 기존 클래스 무변경.

**Option C — 어노테이션 스캐닝 (`@SocialProvider(KAKAO)`)**
- 거부 이유: 커스텀 어노테이션 + Bean 메타데이터 처리 부담. Map dispatch와 동일 OCP 달성하면서 코드량만 늘어남.

### 소셜 멤버 저장 책임 위치

**Option A — `UserCommandService.socialLogin()` 내부에서 `if (KAKAO) kakaoRepo.save() else if (NAVER) ...`**
- 거부 이유: Service가 제공자별 Repository를 직접 의존. 단위 테스트 시 Repository 2종 Mock 필요. 신규 제공자 추가가 Service 수정.

**Option B (선택) — `SocialMemberRegistrar` 도메인 서비스 + `SocialMemberFactory` Strategy**
- 비용: 도메인 서비스 + Strategy 인터페이스 + 제공자별 Factory 구현체로 클래스 수 증가.
- 보상: Service는 Registrar 1개만 의존. 중복 검증 + 등록을 도메인 서비스가 응집. Card BC `CardStatusHistoryAppender`와 패턴 통일.

### Command/Query 분리

**Option A — 단일 `UserService` 유지 + 메서드별 `@Transactional(readOnly)` 적용**
- 거부 이유: 메서드 단위 `readOnly` 누락 시 dirty checking이 조용히 발생. 클래스 책임이 9개 메서드로 비대.

**Option B (선택) — `UserCommandService` (메서드 레벨 `@Transactional`) + `UserQueryService` (클래스 레벨 `@Transactional(readOnly=true)`)**
- 비용: 의존성 주입 지점이 Controller에서 2개로 분기.
- 보상: 읽기 전용 트랜잭션이 정적 보장. 각 Service의 단위 테스트가 작아짐.

### 예외 체계

**Option A — 도메인별 `IllegalArgumentException` / `UsernameNotFoundException` / `RuntimeException` 혼용**
- 거부 이유: HTTP 매핑이 `GlobalExceptionHandler`마다 분기. 응답 형식이 일관되지 않아 FE 분기 처리 부담.

**Option B (선택) — `UserDomainException extends BusinessException` + ErrorCode enum**
- 비용: 새 에러 추가 시 ErrorCode 등록 + 도메인 예외 throw 2단계.
- 보상: 응답 형식이 `{code, message}` 단일. Card BC `CardDomainException`과 패턴 통일.

### 권한 검증 위치

**Option A — Service에서 `SecurityContextHolder.getContext().getAuthentication()` 참조**
- 거부 이유: Service 단위 테스트에서 SecurityContext Mock 강제. 인증 컨텍스트가 도메인 로직과 결합.

**Option B (선택) — Controller에서 `currentUser` vs `dto.username` + `isAdmin` 분기 + `AccessDeniedException` throw**
- 비용: Controller에 분기 코드가 명시적으로 노출.
- 보상: Service는 Security를 모름. 권한 정책이 진입점(Controller)에 집중되어 가시성↑.

**Option C — `@PreAuthorize("hasRole('ADMIN') or #dto.username == authentication.name")`**
- 보류 이유: `@EnableMethodSecurity` 활성화 부담 + SpEL 테스트 복잡도. v2에서 권한 정책이 다층화되면 재검토. ADR 후보.

### 수정 대상자 결정

**Option A — `UserUpdateRequestDTO.username` 바디로 수정 대상자 명시**
- 거부 이유: 본인 검증을 Controller가 누락하면 타 계정 수정 표면이 노출. 보안 사고 1건이 곧 전 계정 침해.

**Option B (선택) — `currentUser.username` Principal 기반**
- 비용: 관리자가 타 유저를 수정하는 별도 엔드포인트가 필요할 경우 분리 설계 필요.
- 보상: 본인 외 수정 표면이 원천 차단. Principal 외 의존 0건.

## 전체 아키텍처 (High-Level Architecture)

> User BC의 컴포넌트 배치와 3가지 핵심 플로우. 본문 5페이지보다 다이어그램 1장이 더 강하다.

### 컴포넌트 배치

```
[React SPA / 클라이언트]
    │
    │  POST /login (id/pw)   |  POST /user (signup)
    │  POST /social/login/{provider} (Authorization Code)
    │  PUT /user, DELETE /user (with AT Cookie)
    ▼
┌────────────────────────────────────────────────────────────────┐
│  User/presentation/                                             │
│  ├─ UserController                                              │
│  │    ├─ loginLocal / joinApi / userMeApi                       │
│  │    ├─ updateUserApi    (★ 본인 검증 — Story-5-3·5-4)         │
│  │    └─ deleteUserApi    (★ 본인/관리자 분기 — Story-5-3)      │
│  └─ SocialLoginController                                       │
│       └─ socialLogin      (★ Map<Type, SocialOAuthFlow> — 4-2)  │
└────────────────────────────────────────────────────────────────┘
    │                                            │
    │  UserCommandService / UserQueryService     │  SocialOAuthFlow.authenticate()
    ▼                                            ▼
┌─────────────────────────────────┐   ┌────────────────────────────────────┐
│  User/application/              │   │  User/infrastructure/{provider}/   │
│  ├─ UserCommandService (4-4)    │   │  ├─ KakaoOAuthFlow                 │
│  │    addUser / loginLocal /    │   │  ├─ NaverOAuthFlow                 │
│  │    updateUser / deleteUser / │   │  ├─ KakaoMemberFactory             │
│  │    socialLogin               │   │  └─ NaverMemberFactory             │
│  └─ UserQueryService (4-4)      │   │     (★ SocialMemberFactory Strategy)│
│       existUser / readUser      │   └────────────────────────────────────┘
│       @Transactional(readOnly)  │                  │
└─────────────────────────────────┘                  │
        │                                            │
        │  TokenIssuer.issue(user, response)         │
        │  SocialMemberRegistrar.register(...)       │
        ▼                                            ▼
┌────────────────────────────────────────────────────────────────┐
│  User/domain/                                                   │
│  ├─ model/                                                      │
│  │    ├─ UserEntity            (Aggregate Root)                 │
│  │    ├─ SocialMember          (Abstract Entity)                │
│  │    ├─ SocialProviderType    (Enum: KAKAO, NAVER)             │
│  │    ├─ SocialUserInfo        (VO — 인증 결과 표준)            │
│  │    ├─ UserRoleType          (Enum: USER, ADMIN)              │
│  │    └─ SocialMemberRegistrar (★ Domain Service — Story-4-3)   │
│  ├─ exception/                                                  │
│  │    └─ UserDomainException extends BusinessException          │
│  └─ repository/UserRepository                                   │
└────────────────────────────────────────────────────────────────┘
        │
        ▼
   MySQL: users / social_member / refresh_entity
```

### 핵심 플로우

**1. 자체 로그인 (`POST /login`)**
```
Client ─POST /login {id, pw}─► UserController.loginLocal
                                  │
                                  ├─ UserCommandService.loginLocal(id, pw)
                                  │     ├─ findByUsername (없음 → PASSWORD_NOT_MATCHED)
                                  │     ├─ isSocial? → USER_IS_SOCIAL
                                  │     ├─ isLock?   → USER_LOCKED
                                  │     └─ passwordEncoder.matches (불일치 → PASSWORD_NOT_MATCHED)
                                  │
                                  └─ TokenIssuer.issue(user, response)
                                        ├─ Set-Cookie: access_token (HttpOnly)
                                        └─ refresh_entity UPSERT
                                        
Client ◄─ 200 + Set-Cookie + Body { refreshToken } ─
```
"사용자 없음"과 "비밀번호 불일치"는 동일 응답으로 통일 (enumeration attack 방지).

**2. 소셜 로그인 (`POST /social/login/{provider}`)**
```
Client ─POST /social/login/kakao {code, state}─► SocialLoginController
                                                   │
                                                   ├─ resolveProvider(provider) → SocialProviderType
                                                   │     (실패 → SOCIAL_PROVIDER_NOT_SUPPORTED)
                                                   │
                                                   ├─ oauthFlows.get(KAKAO).authenticate(code, state)
                                                   │     ├─ KakaoOAuthClient.exchangeToken(code)
                                                   │     ├─ KakaoOAuthClient.getUserInfo(at)
                                                   │     └─ → SocialUserInfo VO
                                                   │
                                                   └─ UserCommandService.socialLogin(...)
                                                         ├─ findByUsername (없으면)
                                                         │    ├─ UserEntity.ofSocial(...)
                                                         │    └─ SocialMemberRegistrar.register
                                                         │         ├─ socialId 중복 → SOCIAL_MEMBER_ALREADY_LINKED
                                                         │         └─ KakaoMemberFactory.register
                                                         └─ TokenIssuer.issue(user, response)

Client ◄─ 200 + Set-Cookie + Body { refreshToken } ─
```
단일 `@Transactional` 경계 — Factory 실패 시 UserEntity 저장도 롤백 (Story-4-3 AC3).

**3. 유저 수정·삭제 (`PUT /user` / `DELETE /user`)**
```
Client ─DELETE /user {username} + AT Cookie─► JWTFilter
                                                │  (Cookie → JWT → UserEntity Principal)
                                                ▼
                                              UserController.deleteUserApi
                                                ├─ @AuthenticationPrincipal UserEntity currentUser
                                                ├─ isAdmin = currentUser.roleType == ADMIN
                                                ├─ isSelfDelete = currentUser.username == dto.username
                                                ├─ !isSelfDelete && !isAdmin
                                                │     └─► AccessDeniedException → AUTH_FORBIDDEN(403)
                                                │
                                                └─ UserCommandService.deleteUser(dto)
                                                      ├─ userRepository.deleteByUsername (소프트 삭제)
                                                      └─ jwtService.removeRefreshUser
```
Service는 권한 검증 없이 단순 삭제만 (Story-5-3). `SecurityContextHolder` Service 참조 0건.

### Out-of-Process 의존

- **카카오 OAuth2 Provider**: `KakaoOAuthClient` (token + userinfo). `KakaoOAuthFlow`가 위임 호출.
- **네이버 OAuth2 Provider**: `NaverOAuthClient` (token + userinfo). `NaverOAuthFlow`가 위임 호출.
- **MySQL**: `users` / `social_member` / `refresh_entity`. UserEntity는 소프트 삭제 (ADR003).
- **JWTFilter**: User BC 외부(`Common/security/`)에서 `UserEntity`를 Principal로 주입.

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | ErrorCode | HTTP | 클라이언트 권장 동작 |
| --- | --- | --- | --- |
| 회원가입 시 username 중복 | `USER_ALREADY_EXISTS` | 409 | 다른 username 안내 |
| 로그인 / 조회 시 미존재 유저 | `USER_NOT_FOUND` | 404 | 로그인은 PASSWORD_NOT_MATCHED로 통일됨 |
| 자체 로그인 비밀번호 불일치 (또는 미존재) | `PASSWORD_NOT_MATCHED` | 401 | 동일 메시지 (enumeration 방지) |
| 잠긴 계정 자체 로그인 시도 | `USER_LOCKED` | 401 | 관리자 문의 안내 |
| 소셜 유저가 자체 로그인 엔드포인트 사용 | `USER_IS_SOCIAL` | 401 | 소셜 로그인 안내 |
| 지원하지 않는 소셜 제공자 요청 | `SOCIAL_PROVIDER_NOT_SUPPORTED` | 400 | provider 값 확인 |
| 동일 socialId 중복 연동 시도 | `SOCIAL_MEMBER_ALREADY_LINKED` | 409 | 이미 연동된 계정 안내 |
| 본인/관리자 외 계정 수정·삭제 시도 | `AUTH_FORBIDDEN` | 403 | 권한 안내 |
| 미인증 요청으로 인증 영역 접근 | `AUTH_TOKEN_MISSING` | 401 | 로그인 흐름 진입 |
| OAuth Provider 토큰 교환 실패 | (현재 500 — v2에서 `SOCIAL_AUTH_FAILED` 분리) | 500 | 재시도 / Provider 상태 확인 |

### 로깅 정책

- **항상 기록**:
    - 회원가입 / 로그인 / 탈퇴 시 `username + 결과 + IP`
    - 소셜 로그인 시 `provider + socialId 해시 + 결과`
    - `AUTH_FORBIDDEN` / `USER_LOCKED` 발생 시 `currentUser + targetUsername`
- **debug**: `SocialOAuthFlow.authenticate` 진입 / Provider 응답 status
- **절대 금지**:
    - 비밀번호 평문 / Access Token / Refresh Token 원문
    - OAuth Provider의 access_token / refresh_token / id_token
    - 사용자가 제공한 `provider` 문자열을 응답 메시지에 echo (input reflection)
    - `socialId` 원문 (해시 또는 마지막 4자리만)

### 관측 지표 (v2 — Metrics 도입 시)

- `user_signup_total{type=local|social, result=success|conflict|fail}` — 카운터
- `user_login_total{type=local|social, result=success|password_mismatch|locked|provider_fail}` — 카운터
- `social_oauth_latency_seconds{provider=kakao|naver}` — 히스토그램 (Provider 응답 지연)
- `user_delete_total{actor=self|admin}` — 카운터
- `auth_forbidden_total{endpoint}` — 카운터 (비정상 폭증 시 권한 우회 시도 의심)

## 롤아웃 / 마이그레이션 (Rollout)

### 전제 — 프로덕션 트래픽 없음

현재 사용자 0명. 마이그레이션은 일괄 배포로 진행한다. 만약 트래픽이 있었다면:
- 이전 호환 응답 형식(`IllegalArgumentException` 기반 plain text)과 신규 형식(`{code, message}`)을 1주차에 병행 발급
- FE 배포 후 신규 형식 단독 전환

이 단계를 생략한 것은 **수용 가능한 단순화**다.

### Product 의존성

- **선행**: **Product 1 (인증 인프라)** 완료 필수.
    - `TokenIssuer` (Story 1-2) — `UserCommandService.loginLocal` / `socialLogin`이 의존
    - `ErrorCode` enum 등록 체계 (Product 1에서 정착)
    - `JWTFilter`의 `UserEntity` Principal 주입 (Story-5-2 전제)
    - `AUTH_TOKEN_MISSING` / `AUTH_FORBIDDEN` ErrorCode 인프라

### Epic·Story 의존성 그래프

```
Epic 4 (UserService 책임 분리)
  Story 4-1 (CustomOAuth2UserService 추출)  
                  │
                  └─► Story 4-2 (SocialOAuthFlow 제공자 분리)
                            │
                            └─► Story 4-3 (SocialMemberRegistrar)
                                      │
                                      └─► Story 4-4 (Command/Query 분리)
                                                │
                                                └─► Epic 5 진입 가능
                                                
Epic 5 (예외·인증 주체 통일)
  Story 5-1 (UserDomainException 도입) ──┐
                                          ├─► Story 5-2 (@AuthenticationPrincipal UserEntity 통일)
                                          │           │
                                          │           ├─► Story 5-3 (SecurityContextHolder 제거)
                                          │           │           │
                                          │           │           └─► (AUTH_FORBIDDEN ErrorCode 신규)
                                          │           │
                                          │           └─► Story 5-4 (UserUpdateRequestDTO 정리)
                                          │
                                          └─► (User BC 전 예외 BusinessException 통일)
```

### 환경별 설정 분기

- `application-local.yml`: 소셜 Provider redirect URI = `http://localhost:8080/...`
- `application-prod.yml`: redirect URI = 운영 도메인. 카카오·네이버 콘솔과 정합 필수.
- 클라이언트 secret은 `${KAKAO_CLIENT_SECRET}` / `${NAVER_CLIENT_SECRET}` 환경변수 주입.

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

## 열린 질문 (Open Questions)

> 본 Product 범위 외이지만 v2에서 의사결정해야 할 항목. 결정이 다른 BC에 영향을 미치므로 명시한다.

- **회원 탈퇴 시 연관 데이터 정리 정책** — `users` 소프트 삭제 후 `Deck` / `Card` / `LearningFacade`를 어떻게 처리하나?
    - 옵션 A: 같이 소프트 삭제 (사용자는 복원 시 데이터 복원 가능)
    - 옵션 B: 익명화 후 보관 (학습 데이터 가치 보존, 개인정보 제거)
    - 옵션 C: 30일 유예 후 하드 삭제 (GDPR-style)
    - **결정 시점**: Card BC / Deck BC가 v2 검토 진입할 때.
- **2FA / 이메일 인증** — v2 검토. 현재 자체 로그인은 단일 패스워드만.
- **다중 소셜 계정 연동** — 한 `UserEntity`가 카카오 + 네이버 동시 연동을 어떻게 허용할지. `SocialMember`는 이미 다중 보유 가능하나 UX 흐름·연동 해제 API 미정의.
- **소셜 → 자체 전환 / 자체 → 소셜 추가 연동** — 현재 `password = "SOCIAL_USER"` 더미값 고정. 소셜 유저가 자체 비밀번호를 설정해 자체 로그인도 사용할 수 있게 할지.
- **`@PreAuthorize` 도입 시점** — 권한 정책이 다층화되면 (예: 관리자·매니저·일반 유저 + 리소스별 권한) ADR 후보. 현재는 Controller 분기로 충분.
- **OAuth Provider 장애 시 사용자 안내** — Provider 자체 5xx 시 현재 500 응답. `SOCIAL_AUTH_FAILED` ErrorCode 신설 + retry-after 헤더 도입 여부.
- **`SocialMember` Aggregate 재설계** — v2. 현재는 `UserEntity`의 자식 컬렉션. 소셜 계정이 1:1 비즈니스 단위가 되면 별도 Aggregate Root로 격상 검토.

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

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

> Epic 4 진행 중 마주친 갈림길. Story 실행 과정에서 원안과 다른 결정이 나온 항목 포함.

### OAuth2 진입점 선택

**Option A — `UserService extends DefaultOAuth2UserService` 유지**
- 거부 이유: 본 프로젝트는 Spring Security `oauth2Login` 표준 흐름을 사용하지 않음 (FE가 Authorization Code를 받아 백엔드 `/social/login/{provider}`에 POST). `loadUser()`가 dead code.

**Option B — `CustomOAuth2UserService` 어댑터 독립**
- 거부 이유: Option A와 동일한 dead code 문제. Story-4-1 원안이었으나 실제 흐름과 정합하지 않아 폐기.

**Option C (선택) — `SocialLoginController` 직접 호출 + `SocialOAuthFlow`**
- 비용: state·CSRF 검증 직접 구현.
- 보상: dead code 제거 + 흐름 명시성.

### 제공자 dispatch 패턴

**Option A — `switch(providerType)` Controller 내부**
- 거부 이유: 제공자 추가 시 Controller 수정. OCP 위반.

**Option B (선택) — `Map<SocialProviderType, SocialOAuthFlow>` + `Map<SocialProviderType, SocialMemberFactory>`**
- 비용: 키 중복 시 런타임 실패 → `SocialMemberRegistrar` 생성자에서 fail-fast.
- 보상: 신규 제공자 = `@Component` 2개 추가만. 기존 클래스 무변경.

**Option C — `@Qualifier("kakao")` / `@Qualifier("naver")` 별도 빈 주입**
- 거부 이유: 제공자 추가마다 Controller 의존성 추가. Map 패턴 대비 OCP 약함.

### `SocialOAuthFlow` 책임 단위

**Option A — `parse(Map<String, Object> attributes)`** (Story-4-2 원안)
- 거부 이유: Spring Security `OAuth2User.getAttributes()`를 전제로 했으나 본 프로젝트는 표준 흐름 미사용. attributes Map을 만들기 위한 추가 작업 발생.

**Option B (선택) — `authenticate(String code, String state)` (Authorization Code → SocialUserInfo)**
- 비용: OAuth2 토큰 교환·userinfo 호출까지 본 인터페이스 책임.
- 보상: Controller가 Authorization Code만 넘기면 끝. 의존 표면 최소.

### 소셜 멤버 저장 위치

**Option A — `UserCommandService.socialLogin()` 내부 분기**
- 거부 이유: Service가 제공자별 Repository 직접 의존. Service 단위 테스트가 Repository 2종 Mock 강제.

**Option B (선택) — `SocialMemberRegistrar` Domain Service + `SocialMemberFactory` Strategy**
- 비용: 클래스 수 증가 (Registrar 1 + Factory 인터페이스 1 + 제공자별 구현 N).
- 보상: 중복 검증 + 등록 응집. Card BC `CardStatusHistoryAppender` 패턴 답습.

### Command/Query 분리 메서드명 일관성

**Option A (선택) — `socialLogin` 메서드명 유지** (Story-4-4)
- 거부된 대안: `findOrRegisterSocialUser`로 명명을 명확화하려 했으나 호출처(SocialLoginController + 테스트) 영향 범위가 분리 작업 본질을 초과. 작업 단위 비대화 회피.

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

## Story 4-2. SocialOAuthFlow 제공자별 분리 (구 SocialUserInfoParser)

## Story 4-2. SocialOAuthFlow 제공자별 분리

> 명세 변경 이력: Story 4-1에서 `CustomOAuth2UserService` 도입 폐기(dead code 제거로 재정의)됨에 따라
> 본 Story의 적용 위치를 `CustomOAuth2UserService` → `SocialLoginController`로 조정.
> 인터페이스 책임 단위도 "OAuth2User attributes 파싱"(`parse(Map<String,Object>)`) →
> "Authorization Code부터 SocialUserInfo까지의 전체 인증 흐름"(`authenticate(code, state)`)으로 격상.
> 사용자 합의 옵션 A (2026-05-28).

### User Story

> As a **백엔드 개발자**,
I want **카카오·네이버 소셜 OAuth 흐름이 독립 컴포넌트로 분리되길**,
So that **신규 제공자(구글 등) 추가 시 흐름 구현체 하나만 추가하면 되는 OCP 구조가 확보된다**.
>

### 설명

- `SocialOAuthFlow` 인터페이스 정의 (구 `SocialUserInfoParser`)
  - `SocialProviderType getProviderType()`
  - `SocialUserInfo authenticate(String code, String state)` — Authorization Code부터 받아 통일된 `SocialUserInfo`까지 반환
- `KakaoOAuthFlow`, `NaverOAuthFlow` 구현체 추가 (`User/infrastructure/{provider}/`)
- `SocialUserInfo(String socialId, String nickname, String email, SocialProviderType provider)` VO 도입
- `SocialLoginController`가 `Map<SocialProviderType, SocialOAuthFlow>`를 주입받아 `providerType`으로 분기 (구 `CustomOAuth2UserService`)

### 완료 기준 (Acceptance Criteria)

- [x]  `SocialLoginController` 내부의 `switch(providerType)` 분기가 제거된다
- [x]  각 OAuthFlow가 `@Component`로 등록되어 `List<SocialOAuthFlow>` → Map 변환으로 자동 주입된다
- [x]  `SocialUserInfo` VO가 인증 결과 표준 타입이 된다
- [x]  엣지 케이스: 지원하지 않는 `provider`로 요청이 오면 `SOCIAL_PROVIDER_NOT_SUPPORTED` ErrorCode 반환

### 알려진 한계 (본 Story 외 처리)

- 신규 제공자 추가가 "단일 클래스(SocialOAuthFlow 구현체)"로 완결되지 않음 — `UserService.socialLogin()` 내부에
  `if (KAKAO) kakaoMemberRepository.save() else if (NAVER) ...` 분기 잔존. **Story 4-3 (SocialMemberRegistrar)** 에서 해소.
- `SocialOAuthFlow.authenticate(String code, String state)` 시그니처가 ADR005 Command/Query record 미적용. **Story 4-4** 에서 일괄 처리.

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

> 명세 변경 이력: Story 4-2와 일관성 위해 Strategy + Map dispatch 패턴 채택
> (사용자 합의 옵션 B, 2026-05-28). 원 명세 "Registrar 내부 switch"는 폐기.

### User Story

> As a **백엔드 개발자**,
I want **소셜 멤버 저장 로직이 UserService가 아닌 도메인 서비스에서 처리되길**,
So that **UserCommandService가 KakaoMemberRepository / NaverMemberRepository를 직접 알지 않아도 된다**.
>

### 설명

- `SocialMemberRegistrar` 도메인 서비스 신설 (`User/domain/model/`)
  - `register(UserEntity user, SocialUserInfo info)` — `Map<SocialProviderType, SocialMemberFactory>`로 dispatch
  - 중복 socialId 사전 검증 (`SOCIAL_MEMBER_ALREADY_LINKED`)
- `SocialMemberFactory` Strategy 인터페이스 + `KakaoMemberFactory` / `NaverMemberFactory` 구현체
- `UserService.socialLogin()`에서 `kakaoMemberRepository` / `naverMemberRepository` 직접 참조 제거
- 향후 구글 제공자 추가 시 `GoogleMemberFactory @Component` 1개 추가로 완결
  (Story 4-2의 `SocialOAuthFlow` 구현체와 합쳐 총 2개 클래스 신규).
  `UserService`, `SocialLoginController`, `SocialMemberRegistrar` 자체는 무변경.

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

> 명세 변경 이력 (2026-05-28):
> - 메서드명: `findOrRegisterSocialUser` 명시했으나 호출처(SocialLoginController + 테스트) 영향 범위가 분리 작업 본질을
>   초과하여 기존 `socialLogin` 이름 유지. 사용자 합의 옵션 A.
> - `CustomOAuth2UserService` 위임: Story-4-1에서 이미 폐기됐으므로 본 Story 시점에 존재하지 않음. AC2 항목에서 제외.
> - ADR005 Command/Query record 적용: 본 Story 범위 외 (옵션 A). 별도 Story 또는 후속 hygiene.

### User Story

> As a **백엔드 개발자**,
I want **UserService가 Command / Query로 분리되길**,
So that **읽기 전용 트랜잭션이 명확해지고 각 서비스가 단독 테스트 가능해진다**.
>

### 설명

- `UserCommandService` — `addUser`, `updateUser`, `deleteUser`, `socialLogin`, `loginLocal`
- `UserQueryService` — `existUser`, `readUser`
- 각각 `@Transactional`(메서드 레벨), `@Transactional(readOnly = true)`(클래스 레벨) 적용
- 레거시 `UserService` 클래스 제거

### 완료 기준 (Acceptance Criteria)

- [x]  `UserCommandService` / `UserQueryService`가 분리되고 각각 독립 테스트 가능하다
- [x]  `UserController` / `SocialLoginController`의 주입 타입이 목적에 맞게 분기된다
- [x]  `UserService` 클래스가 완전히 제거된다
- [x]  엣지 케이스: `readUser()`가 읽기 전용 트랜잭션에서 실행되어 dirty checking이 발생하지 않는다
       (클래스 레벨 `@Transactional(readOnly=true)`로 정적 보장 — 실제 동작 검증은 통합 테스트 영역, 별도 Story로 검토)

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

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

> Epic 5는 예외 체계·인증 주체·권한 검증 위치라는 3축이 얽혀 있어 갈림길마다 트레이드오프를 명시한다.

### 예외 계층

**Option A — 도메인별 `IllegalArgumentException` / `UsernameNotFoundException` 직접 throw**
- 거부 이유: HTTP 매핑·응답 형식이 `GlobalExceptionHandler`마다 분기. FE 분기 처리 부담.

**Option B (선택) — `UserDomainException extends BusinessException` + 정적 팩토리 `of(ErrorCode)`**
- 비용: 새 에러 추가 시 ErrorCode 등록 + 도메인 예외 throw 2단계.
- 보상: 응답 형식이 `{code, message}` 단일. Card BC `CardDomainException` 패턴 통일.

### 인증 주체 주입 타입

**Option A — `@AuthenticationPrincipal String username`**
- 거부 이유: Controller마다 `userRepository.findByUsername` 호출 누적. Principal에 이미 `UserEntity`가 있는데 다시 DB 조회.

**Option B (선택) — `@AuthenticationPrincipal UserEntity currentUser` 단일 타입**
- 비용: JWTFilter가 `UserEntity`를 Principal로 넣는 정합 유지 필수 (이미 충족).
- 보상: Controller에서 추가 DB 조회 0건. 타입 캐스팅 실수 차단.

### 권한 검증 위치

**Option A — Service에서 `SecurityContextHolder.getContext().getAuthentication()` 참조**
- 거부 이유: Service 단위 테스트에서 SecurityContext Mock 강제. Service가 Security 인프라에 결합.

**Option B (선택) — Controller에서 `currentUser` vs `dto.username` + `isAdmin` 분기 + `AccessDeniedException`**
- 비용: Controller에 분기 코드 노출. 메서드별로 분기 패턴 반복.
- 보상: Service는 Security를 모름. 권한 정책 가시성↑. Story-5-3 사용자 합의 옵션 B.

**Option C — `@PreAuthorize("hasRole('ADMIN') or #dto.username == authentication.name")`** (Epic 메모 원안)
- 거부 이유 (보류): `@EnableMethodSecurity` 활성화 부담 + SpEL 테스트 복잡도 (`@WithMockUser` + SpEL 컨텍스트 셋업). 단순 패턴에 비해 학습 비용·디버깅 비용이 커서 v2 검토. **ADR 후보** — 권한 정책이 다층화되는 시점에 재평가.

### 수정 대상자 결정

**Option A — `UserUpdateRequestDTO.username` 바디 필드 유지**
- 거부 이유: 본인 검증을 Controller가 누락하면 타 계정 수정 표면. 보안 사고 단일 표면.

**Option B (선택) — `currentUser.username` Principal 기반 + DTO에서 `username` 제거**
- 비용: 관리자가 타 유저를 수정하는 별도 엔드포인트 필요 시 분리 설계.
- 보상: 본인 외 수정 표면 원천 차단. Story-5-4.

### AUTH_FORBIDDEN ErrorCode 신설

**Option A — 기존 `ACCESS_DENIED(LIBRARY002)` 재사용**
- 거부 이유: 도메인 prefix(`LIBRARY`)가 의미 불일치. User BC의 권한 거부에 부적합. 사용처 0건 — 안전하게 제거 가능.

**Option B (선택) — `AUTH_FORBIDDEN(AUTH005, FORBIDDEN)` 신규**
- 비용: ErrorCode enum에 1건 추가.
- 보상: AUTH prefix로 인증·인가 카테고리 일관성. `GlobalExceptionHandler`가 `AccessDeniedException` → `AUTH_FORBIDDEN`으로 매핑.

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

> 명세 변경 이력 (2026-05-29):
> - 권한 검증 이관 방식: 사용자 합의 옵션 B 채택 → Controller에서 `currentUser` vs `dto.username` + `isAdmin` 분기.
>   `@PreAuthorize` 도입은 보류 (`@EnableMethodSecurity` 부담 + SpEL 테스트 복잡도).
> - `UserCommandService.deleteUser` 시그니처: `(UserEntity currentUser, UserDeleteRequestDTO dto)` → `(UserDeleteRequestDTO dto)` 단순화.
>   Service는 권한 검증 안 함 → currentUser 인자 불필요. 호출 측(Controller)에서 검증 책임.
> - AUTH_FORBIDDEN ErrorCode 신규 (AUTH005, FORBIDDEN). 기존 ACCESS_DENIED(LIBRARY002) 사용처 0건 제거.

### User Story

> As a **백엔드 개발자**,
I want **Service가 SecurityContextHolder를 직접 참조하지 않길**,
So that **Service 단위 테스트에서 Security 설정이 불필요해지고 인증 컨텍스트 접근 책임이 Controller/Security 레이어에 집중된다**.
>

### 설명

- `UserCommandService.deleteUser()`의 `SecurityContextHolder.getContext().getAuthentication()` 제거 (이미 Story-5-2에서 처리)
- 권한 검증을 Controller로 이관: `currentUser.getRoleType() == ADMIN || currentUser.getUsername().equals(dto.getUsername())` 분기 후 `AccessDeniedException` throw
- `GlobalExceptionHandler`가 `AccessDeniedException` → `AUTH_FORBIDDEN` 응답으로 변환 (`{code, message, path, timestamp}` JSON)

### 완료 기준 (Acceptance Criteria)

- [x]  `grep "SecurityContextHolder"` 결과가 Common/security 패키지 외에서는 0건이다
- [x]  `UserCommandService.deleteUser()`가 `UserDeleteRequestDTO`만 받아서 단순 삭제만 수행 (권한 검증은 Controller 책임)
- [x]  본인 / 관리자 외 계정 삭제 시도 시 `AccessDeniedException` → `AUTH_FORBIDDEN(AUTH005, FORBIDDEN)` 응답
- [x]  엣지: SecurityContext가 비어 있는 상태에서 인증 필요 API 접근 시 `JwtAuthenticationEntryPoint` → `AUTH_TOKEN_MISSING` (Story 3-2 흐름 유지)

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

