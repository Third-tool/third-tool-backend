# FE 0.0.1v UX 테스트 — 브라우저 직접 사용 점검표 (FE 관점)

> **목적**: FE M1에서 완결된 배포·정합 작업을 **프로덕션 도메인(`https://thirdtool.dev`)에서 직접 조작**해 사용자가 실제로 겪을 흐름을 점검한다.
>
> **진실 소스**: FE Product 명세 (`fe-workspectrum/sdd/done/product-card.md`, `product-learningFacade.md`, `in-progress/Product.md`, `product-auth.md`, `product-fe-cdn.md`). 백엔드 UX 테스트(`workflow/task/milestones/version/0.0.1v/ux-test.md`)와 상보. 백엔드는 도메인 흐름을 다루고 본 파일은 FE 관점 흐름·시각·성능·회복을 다룬다.
>
> **환경**: 프로덕션(`https://thirdtool.dev` + `https://api.thirdtool.dev`) — BE M1 도메인 신호 통과 후. BE 미통과 상태에서는 로컬(`localhost:5173` + `localhost:8080`)로 대체 가능하나 본 파일의 목적은 프로덕션 검증.
>
> **방법**: 각 항목을 브라우저에서 직접 수행 후 `- [ ]` → `- [x]` 체크. 실패 시 실제 결과와 오류 코드를 인라인에 기록.

---

## 테스트 범위 & 우선순위

| 섹션 | 대상 | 우선순위 | 항목 수 |
| --- | --- | --- | --- |
| 1. FE 배포·도메인 로드 | product-fe-cdn | P0 | 8 |
| 2. 인증 UX 흐름 | Product (User) + product-auth | P0 | 10 |
| 3. Card 화면 (done 정합) | product-card | P0 | 6 |
| 4. LearningFacade 화면 (done 정합) | product-learningFacade | P0 | 6 |
| 5. 에러 · 엣지 UX | product-auth + 라우팅 | P0 | 8 |
| 6. 접근성 · 시각 · 반응형 | 횡단 | P1 | 6 |
| 7. 성능 · 캐시 관찰 | product-fe-cdn + 브라우저 | P1 | 5 |

---

## 사전 준비

테스트 시작 전 다음을 확인한다.

- [ ] BE M1 도메인 신호 통과: `curl -I https://api.thirdtool.dev/actuator/health` → 200 OK
- [ ] BE HTTP → HTTPS 리다이렉트: `curl -I http://api.thirdtool.dev` → 301
- [ ] FE CF 활성: `curl -I https://thirdtool.dev` → 200 (Content-Type: text/html)
- [ ] 브라우저 캐시 clear (Chrome DevTools → Application → Clear storage)
- [ ] Chrome DevTools 열고 시작 (Network 탭 활성, Preserve log 체크)

---

## 1. FE 배포·도메인 로드

### 1-1. `https://thirdtool.dev` 첫 로드

- [ ] 주소창에 `https://thirdtool.dev` 입력 → LandingPage 렌더
- [ ] Network 탭에서 `index.html` 200 OK 수신
- [ ] `Cache-Control: no-cache,no-store,must-revalidate` 헤더 확인 (index.html)
- [ ] 후속 asset(`.js`/`.css`) 요청이 `Cache-Control: public,max-age=31536000,immutable`로 응답

실제 결과: ___

### 1-2. `https://www.thirdtool.dev` 대체 도메인

- [ ] 주소창에 `https://www.thirdtool.dev` → 동일 LandingPage 렌더 (CF 두 alias 모두 활성)

실제 결과: ___

### 1-3. 잘못된 URL → SPA fallback

- [ ] `https://thirdtool.dev/random/nonexistent` 접속 → CloudFront 404 → `/index.html` 200으로 fallback → React Router가 NotFoundPage 렌더
- [ ] 브라우저 URL은 여전히 `/random/nonexistent` 유지 (SPA 라우팅 정상)

실제 결과: ___

### 1-4. HTTPS 강제

- [ ] `http://thirdtool.dev` 접속 시도 → CF 301 → `https://thirdtool.dev` 이동
- [ ] `.dev` TLD HSTS preload에 의해 브라우저가 HTTPS 강제

실제 결과: ___

### 1-5. VITE_API_BASE_URL 정합

- [ ] DevTools Network 탭에서 FE JS가 `https://api.thirdtool.dev`를 호출하는지 확인
- [ ] `localhost:8080` 잔존 호출 0건

실제 결과: ___

### 1-6. CF Cache Hit 확인

- [ ] 새로고침 (F5) → asset 응답 헤더에 `X-Cache: Hit from cloudfront`
- [ ] Ctrl+F5 (강제 새로고침) → `X-Cache: Miss from cloudfront` (첫 요청) 후 다시 Hit

실제 결과: ___

### 1-7. 배포 재실행 검증 (Tier 2)

- [ ] GHA `workflow_dispatch` 재실행 → 다른 빌드 결과 반영
- [ ] `index.html` 새 asset 해시 참조 확인
- [ ] CF invalidation 완료 후 `https://thirdtool.dev` 강제 새로고침 → 새 빌드 로드

실제 결과: ___

### 1-8. GHA 파이프라인 관측

- [ ] GHA Actions 탭에서 workflow 실행 소요 시간 확인 (< 5분 목표)
- [ ] 각 step 성공 확인 (`checkout`, `setup-node`, `npm ci`, `npm run build`, `configure-aws-credentials`, `s3 sync`, `s3 cp index.html`, `cloudfront invalidation`)

실제 결과: ___

---

## 2. 인증 UX 흐름

### 2-1. `/login` 페이지 로드

- [ ] `https://thirdtool.dev/login` → LoginPage 렌더
- [ ] `social-providers.ts` 상수로부터 카카오 + 네이버 진입 버튼 표시 (2개)
- [ ] 자체 로그인 폼 (username + password) 표시

실제 결과: ___

### 2-2. 자체 로그인 성공 → `/home`

- [ ] 기존 시드 사용자 계정으로 로그인 시도 → `POST https://api.thirdtool.dev/login`
- [ ] DevTools Network 탭에서 응답 헤더 `Set-Cookie: access_token=...; HttpOnly; SameSite=Strict; Secure` 확인
- [ ] DevTools Application 탭 Cookies에서 `access_token` 저장됨 (HttpOnly 표시)
- [ ] 응답 바디 `{ refreshToken: "..." }` 수신
- [ ] `/home` 자동 이동 (concept 있는 경우) 또는 `/onboarding` (concept 없는 경우)

실제 결과: ___

### 2-3. 로그인 실패 (잘못된 자격)

- [ ] 존재하지 않는 username 또는 틀린 password → 401 응답 (동일 UX)
- [ ] 인라인 에러 문구 "아이디 또는 비밀번호가 올바르지 않습니다" (enumeration 방지 정합)

실제 결과: ___

### 2-4. USER_IS_SOCIAL 인라인 안내 (M1에는 인라인 안내가 있으면 통과, 없어도 M2로 유예)

- [ ] 소셜 유저 username으로 자체 로그인 시도 → 401 `USER_IS_SOCIAL`
- [ ] 인라인 에러 + 소셜 로그인 CTA 표시 (Tier 2)

실제 결과: ___

### 2-5. 카카오 소셜 로그인 flow

- [ ] "카카오로 로그인" 클릭 → 카카오 OAuth 화면 이동
- [ ] 승인 후 → `https://thirdtool.dev/oauth/kakao/callback?code=...&state=...` 리다이렉트
- [ ] OAuthCallbackPage → `POST https://api.thirdtool.dev/social/login/kakao` 호출
- [ ] `Set-Cookie: access_token` 수신 + `refreshToken` 응답 바디 수신
- [ ] `/home` 자동 이동

실제 결과: ___

### 2-6. AT 만료 → 자동 refresh (MSW로 재현 or 대기)

- [ ] AT 쿠키를 DevTools에서 수동 삭제 → 인증 API 호출 (`GET /user`) → 401 `AUTH002`
- [ ] 인터셉터가 자동으로 `POST /jwt/refresh` 호출 → 새 AT 쿠키 발급
- [ ] 원 요청 재시도 → 200 성공 + 최신 사용자 정보 렌더

실제 결과: ___

### 2-7. RT 만료 → 세션 클리어 → `/login`

- [ ] 메모리 RT를 수동 clear (`localStorage`가 아니므로 개발자 도구로 접근 어려움 → 앱 재시작 or MSW로 재현)
- [ ] 인증 API 호출 → 401 `AUTH102/104` → 인터셉터 세션 클리어 → SessionWatcher → `/login` replace
- [ ] 토스트 "세션이 종료되었습니다" 표시

실제 결과: ___

### 2-8. Terminal 401 코드 (AUTH003/004) 처리

- [ ] MSW 또는 조작된 요청으로 401 `AUTH003` 재현 → 즉시 세션 클리어 (refresh 시도 X)
- [ ] 401 `AUTH001` (AT 없음) → 세션 클리어 + `/login`

실제 결과: ___

### 2-9. `/me` 프로필 표시

- [ ] 로그인 상태에서 `/me` 진입 → username, nickname, email 표시
- [ ] BE Story 5-4 대응 확인: nickname/email 수정 UI만 노출 (username 필드 없음)

실제 결과: ___

### 2-10. 로그아웃 → `/login`

- [ ] 로그아웃 버튼 or API 호출 → 세션 클리어 → `/login` replace
- [ ] 이후 인증 API 호출 시 401 → 재로그인 요구

실제 결과: ___

---

## 3. Card 화면 (done 정합)

### 3-1. `/study` 진입 (스터디 세션)

- [ ] `/study` 진입 → ReviewSession 자동 시작 (BE `POST /api/v1/review/today`)
- [ ] 카드 1장 노출 → mainNote 텍스트만 표시 (RECALLING 단계)
- [ ] "비교하기" 버튼 → keywords + summary 추가 노출 (COMPARING 단계)
- [ ] "다음" 버튼 → 다음 카드 노출

실제 결과: ___

### 3-2. `/archive` 진입 (아카이브 목록)

- [ ] ARCHIVE 상태 카드 목록 표시
- [ ] 각 카드에 summary 요약 + tag chip 표시 (mainNote 본문은 목록 응답에 없어야 함)
- [ ] 태그 필터 chip 클릭 시 필터링

실제 결과: ___

### 3-3. `/archive/:cardId` 상세

- [ ] 카드 클릭 → `/archive/{cardId}` 진입
- [ ] mainNote 본문 (마크다운 렌더 + DOMPurify sanitize)
- [ ] keywords / tags / status / lastViewedAt 등 상세 필드 표시

실제 결과: ___

### 3-4. 카드 생성 (`/cards/new`)

- [ ] `/cards/new` 진입 → CardEditorPage
- [ ] mainNote + summary + keywords (min 1) + tags (max 3) 입력
- [ ] keywords 0개 시도 → Zod 인라인 에러 "최소 1개 필요"
- [ ] 마지막 keyword 삭제 아이콘 → disabled + tooltip
- [ ] 저장 → 201 응답 → 토스트 "저장됨" + 목록 이동

실제 결과: ___

### 3-5. 실패 어휘 grep

- [ ] `/study` 화면 UI 문구에 "실패" 어휘 존재 여부 확인 (0건 목표)
- [ ] `/archive` 화면 문구에도 0건
- [ ] "정리됨" / "보관됨" / "휴식 중" 등 운영 위치 어휘로 통일

실제 결과: ___

### 3-6. 아카이브 낙관 update

- [ ] 카드 아카이브 토글 클릭 → UI 즉시 상태 갱신
- [ ] 백엔드 응답 대기 시간 동안 사용자 체감 지연 0
- [ ] 실패 시 롤백 (MSW로 재현 or 네트워크 offline 후 시도)

실제 결과: ___

---

## 4. LearningFacade 화면 (done 정합)

### 4-1. `/map` 진입 → 4-level 트리 렌더

- [ ] `/map` 진입 → concept 표시 + Axis 목록 + Topic + Material 트리 렌더
- [ ] `useLearningFacade` hook이 정상 응답

실제 결과: ___

### 4-2. CoverageBadge 색상 정합 (M1 정착 대상)

- [ ] Topic에 자료 없음 → 회색 (`NO_MATERIAL`)
- [ ] Topic에 자료 있으나 proficiencyLevel 미완성 → 주황 (`PARTIAL`)
- [ ] Topic에 자료 있고 모두 MASTERED → 초록 (`COVERED`)
- [ ] snapshot 테스트 통과 확인 (`features/map/constants.ts` 기반)

실제 결과: ___

### 4-3. 권장 한도 초과 뱃지

- [ ] Axis 6개 이상 상태 → 6번째 axis 옆에 "권장 한도 초과" 뱃지
- [ ] Topic 11개 이상 → 동일 뱃지
- [ ] 저장은 여전히 허용 (차단 X)

실제 결과: ___

### 4-4. Focus Top N 뱃지

- [ ] 상위 3개 Topic에 "지금 집중" 뱃지 (`isFocused: true`)

실제 결과: ___

### 4-5. Axis reorder (M2 dnd-kit 도입 후에 완전 검증. M1은 UI 그대로 활성 확인만)

- [ ] Axis 드래그 시도 → 커서 반응 (dnd-kit 미도입 시 no-op)
- [ ] reorder API가 존재하는지 개발자 도구에서 확인 (M2 도입 목표)

실제 결과: ___

### 4-6. 자료 연결 후 CoverageBadge 즉시 갱신

- [ ] Material 연결 mutation → 응답 데이터로 CoverageBadge 색상 갱신
- [ ] `queryClient.invalidateQueries(['learning-facade'])` 트리거 확인

실제 결과: ___

---

## 5. 에러 · 엣지 UX

### 5-1. 404 라우트

- [ ] `/nonexistent` 진입 → NotFoundPage 렌더 + "돌아가기" CTA

실제 결과: ___

### 5-2. Maintenance (503)

- [ ] BE MSW 또는 임시 조작으로 503 발생 시도 → `ApiError('MAINTENANCE', ...)` 감지
- [ ] SessionWatcher 또는 라우팅이 `/maintenance` replace
- [ ] MaintenancePage 표시

실제 결과: ___

### 5-3. 5xx 서버 오류 UX

- [ ] MSW 또는 임시 조작으로 500 응답 발생 → `ApiError('INTERNAL_ERROR', ...)` 감지
- [ ] 컴포넌트 onError → 토스트 "일시적 오류" + 재시도 버튼
- [ ] DevTools console.warn에 `requestId + code` 로그 (DEV mode)

실제 결과: ___

### 5-4. AUTH_FORBIDDEN (403) 처리

- [ ] 타 유저 카드 접근 시도 → 403 `AUTH_FORBIDDEN`
- [ ] `/home` replace + 토스트 "접근 권한이 없습니다"

실제 결과: ___

### 5-5. 폼 사전 검증 (Zod) 통과 후 백엔드 400 발생

- [ ] Zod 통과했지만 백엔드가 다른 이유로 400 응답 (예: 서버 측 정합 룰)
- [ ] 인라인 에러 or 토스트로 사용자 안내

실제 결과: ___

### 5-6. 네트워크 단절

- [ ] DevTools Network throttling → Offline
- [ ] API 호출 시도 → ApiError 발생 → 토스트 "네트워크에 연결할 수 없습니다"
- [ ] Online 복귀 후 재시도 성공

실제 결과: ___

### 5-7. X-Request-Id 왕복

- [ ] 임의 요청 발생 → 요청 헤더에 `X-Request-Id` 자동 부착
- [ ] 응답 헤더에 동일 값 echo back (BE MdcLoggingFilter)
- [ ] 5xx 시 `ApiError.requestId`로 전파 → DevTools console.warn에 표시

실제 결과: ___

### 5-8. MSW 401 시나리오 8종 재현

- [ ] Vitest 또는 브라우저 개발용 MSW로 다음 각각 재현:
  - AUTH001 (AT missing) → 세션 클리어
  - AUTH002 (AT expired) → refresh 재시도
  - AUTH003 (AT invalid) → 세션 클리어
  - AUTH004 (user not found) → 세션 클리어
  - AUTH101 (RT invalid) → 세션 클리어
  - AUTH102 (RT not found) → 세션 클리어
  - AUTH103 (RT reused) → 세션 클리어
  - AUTH104 (RT missing) → 세션 클리어

실제 결과: ___

---

## 6. 접근성 · 시각 · 반응형

### 6-1. Tab 키 네비게이션

- [ ] LoginPage에서 Tab 키 순서: username → password → 로그인 버튼 → 소셜 카카오 → 소셜 네이버 → 회원가입 링크
- [ ] focus-visible 표시 명확

실제 결과: ___

### 6-2. 색상 대비 (CoverageBadge)

- [ ] Chrome DevTools Lighthouse Accessibility → 색상 대비 위반 0건 (또는 최소화)

실제 결과: ___

### 6-3. 반응형 (Mobile / Desktop)

- [ ] Chrome DevTools Toolbar → iPhone SE 프로필 → 주요 페이지 렌더 확인
- [ ] Desktop 1920 → 레이아웃 정상

실제 결과: ___

### 6-4. Dark mode (도입되어 있으면)

- [ ] OS dark mode 토글 → 색상 반전 정상 (또는 미도입 명시)

실제 결과: ___

### 6-5. 스크린리더 안내 (Chrome VoiceOver 등)

- [ ] LoginPage aria-label 존재 확인 (수동 관찰)
- [ ] Button 컴포넌트 aria-pressed / aria-disabled 정확

실제 결과: ___

### 6-6. 커스텀 스크롤·애니메이션 (있는 경우)

- [ ] Framer Motion 등 애니메이션 라이브러리 존재 확인 (`lib/motion/`)
- [ ] prefers-reduced-motion 대응 여부

실제 결과: ___

---

## 7. 성능 · 캐시 관찰

### 7-1. LCP 관찰

- [ ] DevTools Performance 탭 → `/` 로드 → LCP 지표 확인
- [ ] `_____ ms` 기록 → `performance.md`에 반영

실제 결과: ___

### 7-2. CLS 관찰

- [ ] `/home` 로드 후 5초 동안 layout shift 발생 여부
- [ ] `_____` 기록

실제 결과: ___

### 7-3. TanStack Query DevTools

- [ ] React Query DevTools 활성 (개발자 모드) → 캐시된 쿼리 목록 확인
- [ ] `['user']`, `['learning-facade']`, `['cards']` 등 fresh/stale 상태 관찰

실제 결과: ___

### 7-4. CloudFront Cache Hit

- [ ] 두 번째 로드 시 `X-Cache: Hit from cloudfront` 응답 헤더 확인
- [ ] `Age` 헤더로 캐시 나이 확인

실제 결과: ___

### 7-5. 번들 사이즈 확인

- [ ] `npm run build` 후 `dist/` 크기 실측
- [ ] `performance.md` 번들 baseline 섹션에 기입

실제 결과: ___

---

## 종합 통과 기준

> 아래 6개 묶음이 모두 통과해야 FE M1 UX 테스트 완료로 간주.

| 묶음 | 조건 | 상태 |
| --- | --- | --- |
| **배포·도메인 핵심** | 1-1 + 1-3 + 1-4 + 1-5 (SPA fallback + HTTPS + API URL 정합) | [ ] |
| **인증 핵심** | 2-1 + 2-2 + 2-6 (로그인 + refresh 흐름) | [ ] |
| **Card done 정합** | 3-1 + 3-4 + 3-5 (스터디 + 폼 검증 + 실패 어휘 grep) | [ ] |
| **LearningFacade done 정합** | 4-1 + 4-2 (트리 + 색상 정합) | [ ] |
| **에러 회복 UX** | 5-1 + 5-2 + 5-3 + 5-8 (라우팅 fallback + terminal 401 + MSW) | [ ] |
| **성능 baseline** | 7-1 + 7-4 + 7-5 (LCP + CF Cache + 번들) | [ ] |

---

## 미통과 항목 기록란

| 항목 번호 | 실제 결과 | 예상 원인 | 조치 필요 여부 |
| --- | --- | --- | --- |
| | | | |
| | | | |

> 통과·미통과 결과는 [`review.md`](./review.md) "시나리오 검증 결과" 섹션에 요약 기록.

---

*작성일: 2026-06-28 (예정) | 대상 버전: 0.0.1v | 참고: fe-workspectrum/sdd/done/product-card.md · product-learningFacade.md · in-progress/Product.md · product-auth.md · product-fe-cdn.md*
