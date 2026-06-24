# adr002: Access Token은 HttpOnly Cookie, Refresh Token은 React 메모리에 저장한다

**영역**: implementation | **상태**: Accepted | **날짜**: 2026-05-27

> **면접 포인트**
> "업계 표준(AT 메모리, RT Cookie)을 왜 반대로 했나요? XSS와 CSRF는 어떻게 방어했나요?"
> → AT를 HttpOnly Cookie에 두면 JavaScript가 AT에 접근 자체가 불가(XSS 원천 차단)하고, 브라우저가 자동 첨부하므로 FE 코드에서 Authorization 헤더 주입이 불필요하다. CSRF는 SameSite=Strict로 방어한다.

---

## 왜 이 결정이 필요했나 (Context)

이전 인증 인프라의 핵심 위험:
- AT와 RT 모두 응답 바디로 전달 → AT가 프론트엔드 로그·브라우저 확장·네트워크 패널에서 평문으로 관측 가능
- XSS 공격 시 FE 메모리 덤프나 fetch interception으로 AT 탈취 가능
- 소셜 로그인은 RT를 쿠키로 발급하지만 `Secure=false`, `SameSite` 미설정 등 보안 속성 미비

**업계 표준**은 "AT 메모리, RT HttpOnly Cookie"이지만 본 프로젝트는 반대 방향을 선택했다.

---

## 무엇을 결정했나 (Decision)

**AT = HttpOnly Cookie (30분 TTL) / RT = React 메모리 (7일 TTL)**

### AT (HttpOnly Cookie 30분)

- XSS로 탈취 불가 — JavaScript에서 접근 자체 불가능 (`HttpOnly=true`)
- 브라우저가 모든 API 요청에 자동 첨부 → FE는 `Authorization` 헤더 주입 코드 불필요
- CSRF는 `SameSite=Strict` + `/api/**` 경로 한정으로 방어

### RT (React 메모리 7일)

- 페이지 새로고침 시 유실되지만 AT가 쿠키로 살아있으므로 AT 만료 전까지 자동 재인증 가능
- AT 만료 후 새로고침 발생 시 재로그인 요구 → **보안 관점에서 수용 가능한 트레이드오프**
- `localStorage`/`sessionStorage` 절대 금지 — React 컴포넌트 상태/변수만 허용

### 쿠키 보안 속성

| 속성 | local/dev | prod |
|------|-----------|------|
| `HttpOnly` | `true` | `true` |
| `SameSite` | `Strict` | `Strict` |
| `Secure` | `false` | `true` |
| `Path` | `/` | `/` |
| `Max-Age` | AT TTL | AT TTL |

`JwtCookieProperties` `@ConfigurationProperties`가 부팅 시 `SameSite` 값을 `Strict|Lax|None`으로 제한 (그 외 값이면 `BeanCreationException`).

---

## 대안과 거부 이유 (Alternatives)

| 대안 | 거부 이유 |
|------|-----------|
| AT 메모리 + RT HttpOnly Cookie (업계 표준) | FE가 매 요청에 `Authorization` 헤더 주입 코드 필요. XSS로 AT 탈취 가능. 본 프로젝트의 FE 단순화 우선순위에 부적합 |
| AT/RT 모두 HttpOnly Cookie | RT가 모든 요청에 자동 첨부되어 불필요 노출. `/jwt/refresh` 외 경로에서도 RT가 보임 → 공격 표면 확대 |
| AT/RT 모두 응답 바디 (기존 상태) | XSS 위험 + FE 메모리 덤프 노출 — 본 ADR이 해결하려는 문제 자체 |

---

## 결과와 트레이드오프 (Consequences)

**긍정적**
- XSS로 AT 탈취 경로 원천 차단 (HttpOnly)
- FE 코드에서 토큰 관리 로직 제거
- 보안 경계가 서버 측에 집중 → 토큰 정책 변경 시 클라이언트 배포 불요

**트레이드오프**
- **AT 만료(30분) 후 새로고침 → 재로그인**: RT가 메모리에 없어 재로그인 필요. 사용자 경험 trade-off
- **모바일 앱 대응 불가**: 쿠키는 브라우저 컨텍스트 의존. 향후 모바일 앱 도입 시 별도 토큰 전달 경로 필요
- **CSRF 방어가 SameSite=Strict에 의존**: 이메일 링크 같은 외부 링크에서 쿠키 전송이 막혀 일부 UX 시나리오 추가 처리 필요

---

## 재검토 시점

- 모바일 앱 도입 시 → 별도 토큰 전달 경로 결정 (ADR 신규)
- SameSite=Strict로 인한 UX 시나리오 문제 누적 시 → Lax로 완화 + CSRF 토큰 추가 검토
