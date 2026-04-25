# User 테이블 정의서

> User BC가 다루는 사용자 본체 + 소셜 프로바이더 연동 + JWT 리프레시 토큰 데이터 영역.

> 작성 규칙은 [`_rules.md`](_rules.md) 참조. 도메인 모델은 [`docs/domain/user.md`](../domain/user.md) 참조.

---

## 테이블 목록

| 테이블 | 역할 | 도메인 매핑 | 마이그레이션 |
|---|---|---|---|
| `user_entity` | 사용자 본체 | `UserEntity` (Aggregate Root) | V1 |
| `social_member` | 소셜 프로바이더 식별 정보 (JOINED 상속 부모) | `SocialMember` | V1 |
| `kakao_member` | 카카오 식별 정보 (자식) | `SocialMember` 자식 | V1 |
| `naver_member` | 네이버 식별 정보 (자식) | `SocialMember` 자식 | V1 |
| `jwt_refresh_entity` | JWT 리프레시 토큰 저장 | `Common/security/auth/jwt/RefreshEntity` | V1 |

> `jwt_refresh_entity`는 도메인 모델 위치상 User BC가 아니라 `Common/security`에 있습니다. 본 정의서는 사용자 인증과 직결되므로 함께 다루며, 향후 위치 정리 시 본 표도 갱신.

---

## `user_entity`

> 시스템 내 사용자 본체. 다른 BC는 `id`로만 참조.

### 컬럼

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | N | - | PK |
| `username` | `VARCHAR(255)` | N | - | 로그인 ID (UNIQUE) |
| `password` | `VARCHAR(255)` | N | - | 암호화된 비밀번호 |
| `is_lock` | `TINYINT(1)` | N | `0` | 계정 잠금 여부 |
| `is_social` | `TINYINT(1)` | N | `0` | 소셜 가입 여부 |
| `social_provider_type` | `VARCHAR(20)` | Y | `NULL` | `KAKAO` / `NAVER` / `NULL`(자체) |
| `role_type` | `VARCHAR(20)` | N | - | `USER` / `ADMIN` |
| `nickname` | `VARCHAR(255)` | Y | `NULL` | 표시용 닉네임 |
| `email` | `VARCHAR(255)` | Y | `NULL` | 이메일 |
| `created_date` | `DATETIME(6)` | Y | `NULL` | 생성 시각 |
| `updated_date` | `DATETIME(6)` | Y | `NULL` | 수정 시각 |

### 키 / 인덱스

| 종류 | 컬럼 | 설명 |
|---|---|---|
| PK | `id` | |
| UNIQUE | `uk_user_entity_username` (`username`) | 로그인 ID 중복 방지 |

### 제약 / 정책
- `is_social = 1`인 경우 `social_provider_type`이 NOT NULL이어야 합니다 (애플리케이션 검증).
- `password`는 자체 가입자만 의미가 있고, 소셜 가입자는 임의 placeholder를 저장할 수 있습니다 (정책 확인 필요).

---

## `social_member`

> 사용자 ↔ 소셜 프로바이더 식별 정보 매핑. JPA JOINED 상속 전략의 부모 테이블.

### 컬럼

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | N | - | PK |
| `user_id` | `BIGINT` | Y | `NULL` | FK → `user_entity.id` |
| `social_id` | `VARCHAR(255)` | N | - | 프로바이더 발급 식별자 |
| `refresh_token` | `VARCHAR(512)` | Y | `NULL` | 프로바이더 refresh token |
| `provider_type` | `VARCHAR(31)` | N | - | JPA discriminator (`KAKAO` / `NAVER`) |

### 키 / 인덱스

| 종류 | 컬럼 | 설명 |
|---|---|---|
| PK | `id` | |
| UNIQUE | `uk_social_member_social_id` (`social_id`) | 프로바이더 ID 중복 방지 |
| FK | `user_id` → `user_entity.id` | |

---

## `kakao_member`

> JOINED 상속의 카카오 자식 테이블. 부모 `social_member`와 동일 PK.

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| `id` | `BIGINT` | N | PK & FK → `social_member.id` |

---

## `naver_member`

> JOINED 상속의 네이버 자식 테이블. 부모 `social_member`와 동일 PK.

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| `id` | `BIGINT` | N | PK & FK → `social_member.id` |

---

## `jwt_refresh_entity`

> 발급된 JWT 리프레시 토큰 저장.

### 컬럼

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | N | - | PK |
| `username` | `VARCHAR(255)` | N | - | 사용자 식별자 (UNIQUE) |
| `refresh` | `VARCHAR(512)` | N | - | 리프레시 토큰 값 |
| `created_date` | `DATETIME(6)` | Y | `NULL` | 발급 시각 |
| `updated_date` | `DATETIME(6)` | Y | `NULL` | 갱신 시각 |

### 키 / 인덱스
| 종류 | 컬럼 | 설명 |
|---|---|---|
| PK | `id` | |
| UNIQUE | `username` | 사용자당 1개 토큰 |

### 정책
- 사용자당 활성 리프레시 토큰은 1건만 보존(UPSERT).
- `refresh`는 만료 검증을 거친 후 새 액세스 토큰 발급에 사용.

---

## ER 관계

```
user_entity
  ├── social_member (user_id FK, JOINED 상속)
  │     ├── kakao_member (PK = social_member.id)
  │     └── naver_member (PK = social_member.id)
  └── (간접) jwt_refresh_entity (username 기준)

user_entity ←── 다른 BC (deck.user_id, learning_facade.user_id, …)
```

> 반영 마이그레이션: V1.
