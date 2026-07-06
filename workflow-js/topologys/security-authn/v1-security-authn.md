# Pinned Topology — `security-authn` (Phase: `v1`)

> 이 파일은 **이번 작업 구간 동안 고정되는 topology 참조**다.
> - living doc 아님
> - runbook 아님
> - vocabulary 아님 (실제 provider·claim·refresh 저장 스키마는 여기 없다)

**목적**: 인증·인가·소유권 검증의 반복 원칙을 pin. `common-core`가 예외 변환·`bounded-context`가 BC 경계를 다룬다면, 본 파일은 **자체·소셜 인증 · 토큰 수명주기 · 소유권 검증** 원칙을 다룬다.

---

## 0. 유효 구간 (Validity)

| 항목 | 값 |
| --- | --- |
| Pinned at | 2026-07-21 |
| Valid for | 본 워크플로우 v1 구간 — re-pin trigger 발생 전까지 |
| Owner | 메인 Claude + 사용자 합의 |
| Re-pin trigger | 아래 §4 |

---

## 1. How Claude Code must use this file — 행동 계약

1. **인증·인가·소유권 관련 작업 시 이 파일을 먼저 읽는다.** §2의 노드·경계는 본 구간 **고정 제약**.
2. **`@AuthenticationPrincipal` 우회 · request body에서 userId 수신 정황이 보이면 STOP하고 보고한다.** 위조 위험.
3. **`isOwnedBy(userId)` 소유권 검증 없이 Aggregate 상태 변경 정황이 보이면 보고**한다.
4. **Refresh Token 재사용을 감지하지 못하는 정황이 보이면 보고**한다. 화이트리스트 원칙 위반.
5. **비밀번호·JWT·refresh token을 raw로 로그·응답에 노출하려는 정황이 보이면 보고**한다 (observability·secrets 계승).
6. **이 파일에 어휘를 추가하지 않는다.** 실제 provider·엔드포인트는 §3 포인터로.

---

## 2. The pinned topology

### Nodes
- `자체 인증` — 로컬 이메일/비밀번호 로그인
- `소셜 인증` — 외부 provider 위임 인증 (OAuth 2.0 계열)
- `Access Token` — 짧은 수명 (분 단위) · 요청마다 검증
- `Refresh Token` — 긴 수명 (일~주 단위) · 화이트리스트 저장 · 재발급 전용
- `토큰 화이트리스트` — 유효 Refresh Token 저장소 (재사용 감지 소스)
- `재사용 감지` — 이미 사용된 Refresh Token 재요청 시 전체 세션 무효화
- `인증 진입점` — Controller의 `@AuthenticationPrincipal` 파라미터
- `소유권 검증` — Aggregate `isOwnedBy(userId)` 도메인 메서드 (Application Service가 호출)
- `403 vs 404 경계` — 존재하지만 소유권 없음 (403) vs 존재하지 않음 (404)
- `Common Security 모듈` — 인증 필터·principal 매핑 담당 (BC 밖)
- `provider 확장 지점` — 새 소셜 provider 추가 시 `SocialProviderType` enum + Entity 확장 원칙
- `쿠키 저장` — Access·Refresh 저장 방식 (HttpOnly · SameSite=Strict)
- `PII 노출 금지` — 비밀번호·토큰·이메일 원문의 로그·응답 미노출

### Edges
- 사용자 요청 (`자체 인증` · `소셜 인증`) → `Common Security 모듈` → `Access Token` + `Refresh Token` 발급
- `Refresh Token` 발급 → `토큰 화이트리스트` 저장
- 요청마다 → `Access Token` 검증 → `인증 진입점` (`@AuthenticationPrincipal`) → Controller
- Refresh 요청 → `토큰 화이트리스트` 조회 → 유효 시 재발급 · 무효 시 401 · 이미 사용된 토큰 → `재사용 감지` → 전체 세션 무효화
- Application Service → Aggregate → `소유권 검증` (`isOwnedBy(userId)`) : 상태 변경 전 필수
- `소유권 검증` 실패 → 403 (`403 vs 404 경계`) : 소유권 없음
- 대상 Aggregate 부재 → 404 (`403 vs 404 경계`) : 리소스 없음
- 인증·토큰 로그·응답 → `PII 노출 금지` 검증 : raw 값 노출 없음
- 새 provider 요구 → `provider 확장 지점` : enum + Entity 확장 원칙

### Boundaries
- **인증 진입점 경계**: Controller에서 `userId`를 **request body·query·path**로 받지 않는다. `@AuthenticationPrincipal` (또는 인증 컨텍스트) 만 사용.
- **소유권 검증 경계**: Aggregate 상태 변경 · 조회 응답 전에 **`isOwnedBy(userId)` 도메인 메서드** 로 검증. Application Service가 호출.
- **토큰 수명 경계**: `Access Token`은 짧은 수명. `Refresh Token`으로 재발급. Access Token 자체 화이트리스트 미유지 (수명 짧아 로컬 만료 대응).
- **재사용 감지 경계**: `Refresh Token`은 화이트리스트에 존재해야 유효. 이미 사용된 토큰의 재요청 → 전체 세션 무효화 (도난 대응).
- **에러 매핑 경계**: 존재하지만 소유권 없음 → 403 · 존재하지 않음 → 404. 정보 노출 최소화 필요 시 404로 통합 검토 (사용자 결정).
- **쿠키 경계**: 브라우저 클라이언트 대상은 HttpOnly · SameSite=Strict. XSS·CSRF 이중 방어.
- **provider 확장 경계**: 새 소셜 provider는 `SocialProviderType` enum 확장 + Entity 확장 원칙. 인증 필터에 provider-specific 로직 침투 최소화.
- **로그·응답 경계**: 비밀번호·JWT raw·refresh token raw 노출 0 (observability·secrets-management 계승).

### Invariants
- Controller가 `userId`를 request body·query·path로 받는 사례 0건
  - 감지법: Controller 코드 grep · `@RequestParam userId` · `@PathVariable userId` 등 리뷰
- Application Service가 Aggregate 상태 변경 전 소유권 검증 없는 사례 0건
  - 감지법: 상태 변경 유스케이스 리뷰 · `isOwnedBy` 호출 확인
- Refresh Token 재사용 감지 시 세션 무효화 없는 사례 0건
  - 감지법: Refresh 재요청 시나리오 테스트 · 화이트리스트 삭제 확인
- 비밀번호·JWT·Refresh Token이 log·응답에 raw로 노출된 사례 0건 (PII 마스킹)
  - 감지법: log sample · 응답 body grep
- 새 소셜 provider 추가 시 `SocialProviderType` enum 확장 없이 도입한 사례 0건
  - 감지법: SocialProviderType enum 이력 · Entity 확장 리뷰
- 존재하지만 소유권 없음에 200/500을 반환한 사례 0건 (403이어야 함 · 사용자가 404 통합 결정 시 예외)
  - 감지법: Controller Slice 테스트 · 소유권 검증 실패 케이스 확인

---

## 3. 어휘는 어디에 (이 파일이 다루지 않는 것)

- 실제 provider 이름·OAuth flow → `User/infrastructure/social/` 하위 코드
- 실제 Access·Refresh 저장 스키마 (`jwt_refresh_entity`) → Flyway V파일 + JPA 매핑
- 실제 화이트리스트 조회 쿼리 → `User/infrastructure/persistence/`
- 실제 `@AuthenticationPrincipal` 파라미터 타입 → Controller 코드
- 실제 소유권 검증 메서드명 (Aggregate별 상이) → `{BC}/domain/model/`
- 인증·인가 상세 규칙 → `.claude/rules/conventions.md` (인증 절 · 미신설이면 domain-glossary 참조)
- 쿠키·XSS·CSRF 원칙 → 향후 웹 보안 topology 검토 (LOW 우선순위)
- 왜 이렇게 박혔는지 → `docs/adr/` 인증 관련 ADR

---

## 4. Re-pin trigger

- Access/Refresh Token 이분 구조 폐기 (단일 토큰 · session 기반 등)
- 화이트리스트 방식 폐기 (stateless JWT만 유지)
- `@AuthenticationPrincipal` 외 새 인증 컨텍스트 진입점 도입
- 소유권 검증 위치 변경 (Aggregate → Application Service 완전 이관)
- 403 vs 404 정책 변경 (통합 404로)
- 다중 provider 방식 변경 (OIDC 표준 도입 등)
- 쿠키 저장 → 다른 저장 (localStorage 등 · 보안 재검토 필요)
- 세션 재발급 정책 변경 (자동 rotation 도입 등)
