# User 도메인 모델

> 소셜 로그인(Kakao, Naver) 기반 사용자 가입·인증·식별을 담당하는 BC. JWT 발급·갱신은 `Common/security/auth/jwt`에 위임하고, 본 BC는 사용자 도메인 자체만 책임집니다.

> 작성 규칙은 [`_rules.md`](_rules.md) 참조.

---

## 구성 요소

| 구분 | 클래스 | 역할 |
|---|---|---|
| Aggregate Root | `UserEntity` | 사용자 본체 |
| Entity | `SocialMember` | 소셜 프로바이더별 식별 정보 |
| Adapter | `CustomOAuth2User` | Spring Security OAuth2 통합용 어댑터 |
| Enum | `SocialProviderType` | `KAKAO` / `NAVER` … |
| Enum | `UserRoleType` | 권한 분류값 |

> 본 BC는 `domain/exception/`이 별도로 정의되어 있지 않습니다. 인증/인가 관련 예외는 `Common/Exception`에서 처리합니다.

---

## UserEntity (Aggregate Root)

> 시스템 내 사용자를 표현하는 본체. 다른 BC(Card, Deck 등)는 `userId`로만 참조합니다.

### 설명
_TBD_ — 가입 흐름, 활성/비활성 상태, 닉네임 정책 등을 정리.

### 속성 / 행위 / 규칙 / 검증
_TBD_ — `User/domain/model/UserEntity.java` 기준으로 채워주세요.

---

## SocialMember (Entity)

> 한 사용자가 가진 소셜 프로바이더별 식별 정보. (한 사용자가 여러 프로바이더를 연결할 수 있는 경우 1:N)

### 속성 / 행위 / 규칙 / 검증
_TBD_

---

## CustomOAuth2User

> Spring Security `OAuth2User` 인터페이스를 구현해, 도메인 `UserEntity`를 인증 컨텍스트에 노출하는 어댑터.

> 도메인 모델이라기보다 통합 어댑터입니다. 향후 `infrastructure/security/`로 이동을 검토할 여지가 있습니다(현재 `domain/model/`에 위치).

### 책임
- 인증 컨텍스트 → 도메인 사용자 매핑.
- `getAttributes()`, `getName()` 등 Spring Security 계약 충족.

---

## SocialProviderType (Enum)
| 값 | 설명 |
|---|---|
| `KAKAO` | 카카오 OAuth |
| `NAVER` | 네이버 OAuth |

> 신규 프로바이더 추가 시 본 enum과 `User/infrastructure/`의 OAuth 어댑터를 함께 갱신.

## UserRoleType (Enum)
_TBD_ — 권한 등급(예: `USER`, `ADMIN`) 정의.

---

## 책임 분리

| 클래스 | 책임 |
|---|---|
| `UserEntity` | 사용자 정체성, 닉네임 등 도메인 속성 관리 |
| `SocialMember` | 소셜 프로바이더별 외부 식별자 보유 |
| `CustomOAuth2User` | Spring Security OAuth2 어댑터 |
| `Common/security/auth/jwt` | JWT 발급/갱신/필터링 (별도 모듈) |

### 다른 BC와의 관계
- 모든 BC(Card, Deck, Review, LearningFacade, UserSchedule)는 `UserEntity`를 직접 참조하지 않고 `userId`(Long)로만 참조합니다.
- 사용자 단위 조회가 필요한 cross-BC 쿼리는 각 BC의 Application Service에서 Repository를 조합해 처리합니다.
