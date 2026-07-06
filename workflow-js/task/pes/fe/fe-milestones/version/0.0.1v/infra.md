# FE 0.0.1v / Infra 진척

> FE M1 종료 시점에 **FE 배포·인프라 산출물이 어디까지 깔렸는가**의 기록.
> 백엔드 `infra.md`가 VPC/ECS/RDS/Secrets를 다루는 반면, 본 파일은 **CloudFront/S3/GHA/env/도메인 라우팅** 등 FE 진영의 인프라를 다룬다.
> 본 파일은 본주 진행 중 매일 또는 D6 종료 시점에 채운다.

---

## 목표 환경 토폴로지 (M1 종료 후 ↓ 이 모양)

```
[사용자 브라우저]
       │
       │  https://thirdtool.dev  (또는 www.)
       ▼
┌─────────────────────────────┐
│ Route53                     │  apex A alias → CloudFront
│ (Hosted Zone)               │  www  A alias → CloudFront
│                             │  api  A alias → ALB (BE 담당)
└─────────────┬───────────────┘
              │
              ▼
┌──────────────────────────────────────────────────┐
│  CloudFront Distribution                         │
│  - Aliases: thirdtool.dev, www.thirdtool.dev     │
│  - Viewer Protocol: redirect-to-https            │
│  - TLS: us-east-1 ACM (*.thirdtool.dev)          │
│  - Custom Error: 404/403 → /index.html (200)     │
│  - Cache: CachingOptimized                       │
└─────────────┬────────────────────────────────────┘
              │  SigV4 (OAC)
              ▼
┌──────────────────────────┐
│  S3: thirdtool-fe-prod   │  ap-northeast-2
│  - Block Public Access   │
│  - Bucket policy: CF OAC │
│  - /index.html   ← no-cache                 │
│  - /assets/*.js  ← immutable 1y             │
│  - /assets/*.css ← immutable 1y             │
└──────────────────────────┘

[별도 API 호출 흐름 — BE 담당]
FE JS → fetch('https://api.thirdtool.dev/api/v1/...')
             → ALB (백엔드 M1 인프라)
             → ECS Task (Spring Boot)

[GHA CI/CD]
GitHub Actions (workflow_dispatch)
   → .github/workflows/deploy-fe.yml (BE 레포 D9 spec)
   → npm ci + npm run build (VITE_API_BASE_URL 주입)
   → aws-actions/configure-aws-credentials (OIDC → gha-deploy-role)
   → aws s3 sync dist/ ... (index.html 분리 + cache-control 이중 정책)
   → aws cloudfront create-invalidation --paths "/*"
```

---

## 본주 완료 체크리스트

### FE 레포 (`third-tool-fe/untitled`)

- [ ] **FE-1** `.env.production` 생성 + `VITE_API_BASE_URL=https://api.thirdtool.dev` — 커밋 SHA: `_____`
- [ ] **FE-1b** `.env.development` 확인 (`VITE_API_BASE_URL=http://localhost:8080`) — 이미 존재 여부: `_____`
- [ ] **FE-1c** `.gitignore`에 `.env.local` 포함 확인 — `_____`
- [ ] **FE-2** GHA `deploy-fe.yml` FE 레포 이관 or BE 레포 workflow 그대로 사용 결정 — 결정: `_____`
- [ ] **FE-2b** GHA workflow 첫 성공 실행 — Run URL: `_____` / 실행 시간: `_____ 분`
- [ ] **FE-3** 브라우저 `https://thirdtool.dev` 200 OK + FE React SPA 마운트 — 브라우저 스크린샷 or DevTools 캡처 참조: `_____`

### 환경변수 매트릭스 (실측 확인)

| 항목 | dev (.env.development) | prod (.env.production) | M1 실측 |
| --- | --- | --- | --- |
| `VITE_API_BASE_URL` | http://localhost:8080 | https://api.thirdtool.dev | `_____` |
| `VITE_ENABLE_MSW` (optional) | true | (미설정 또는 false) | `_____` |
| `.env.local` git 포함 여부 | 미포함 | 미포함 | `_____` |

### AWS FE 인프라 (BE 레포 spec 참조 — 사용자 액션 결과)

BE `infra.md` Tier 1-확장 참조.

- [ ] **BE-D1** Route53 `thirdtool.dev` 도메인 등록 (사용자 직접) — 등록 완료 시점: `_____`
- [ ] **BE-D2** ACM ap-northeast-2 인증서 발급 (ALB용) — ARN: `_____`
- [ ] **BE-D2b** ACM us-east-1 인증서 발급 (CloudFront용) — ARN: `_____`
- [ ] **BE-D3** ALB HTTPS 리스너 활성 — 실측: `curl -I https://api.thirdtool.dev` → `_____`
- [ ] **BE-D4** Route53 `api.thirdtool.dev` A alias → ALB — DNS 확인: `dig api.thirdtool.dev` → `_____`
- [ ] **BE-D5** S3 `thirdtool-fe-prod` 버킷 생성 + Block Public Access — 버킷 ARN: `_____`
- [ ] **BE-D5b** S3 bucket policy (OAC 만 허용) 적용 — 정책 SHA: `_____`
- [ ] **BE-D6** CloudFront distribution 생성 + OAC 연결 — Distribution ID: `_____`
- [ ] **BE-D6b** CloudFront Custom Error Response (404/403 → /index.html 200) 설정 — 캡처: `_____`
- [ ] **BE-D6c** CloudFront Cache Behavior (CachingOptimized) 확인 — `_____`
- [ ] **BE-D7** Route53 `thirdtool.dev` + `www.thirdtool.dev` A alias → CloudFront — DNS 확인: `_____`

### FE 레포 코드 정합 (BE 도메인 결정 대응)

- [ ] **FE-4** `PUT /user` payload `username` 필드 grep — `grep -r "username" src/features/me src/lib/api/endpoints/user.ts` → 참조 건수: `_____` (기대: 0건)
- [ ] **FE-4b** `UserUpdateRequestDTO` Zod 스키마에서 `username` 필드 제거 확인 — 커밋 SHA: `_____`
- [ ] **FE-4c** MSW handler에서 `PUT /user` 스텁이 `username` 없이도 동작 확인 — `_____`

### CORS + SameSite 프로덕션 정합 검증 준비

- [ ] **FE-3-2** `https://thirdtool.dev/login` → `POST https://api.thirdtool.dev/login` E2E — 성공/실패: `_____`
- [ ] **FE-3-2b** DevTools Network 탭에서 응답에 `Set-Cookie: access_token=...; SameSite=Strict; Secure; HttpOnly` 존재 확인 — 캡처: `_____`
- [ ] **FE-3-2c** DevTools Application 탭 Cookies에서 `access_token` 쿠키 저장 확인 (HttpOnly 표시) — `_____`
- [ ] **FE-3-2d** 후속 인증 API(`GET /user`) 자동 첫 응답 200 — `_____`

---

## 첫 배포 시도 결과

> D5 또는 D6에 첫 배포 시도 결과 기록.

| 항목 | 값 | 비고 |
| --- | --- | --- |
| GHA workflow_dispatch 첫 트리거 시점 | `_____` | 커밋 SHA `_____` |
| `npm ci` 소요 시간 | `_____ 초` | Node 20 + 캐시 |
| `npm run build` 소요 시간 | `_____ 초` | Vite build |
| `aws s3 sync` 소요 시간 | `_____ 초` | `--delete --exclude index.html` |
| `aws s3 cp` index.html 시간 | `_____ 초` | `--cache-control "no-cache,..."` |
| `aws cloudfront create-invalidation` 요청 시각 | `_____` | Invalidation ID `_____` |
| CloudFront invalidation 완료 시점 | `_____` | 소요: `_____ 분` |
| 배포 리드타임 총합 (커밋 → 브라우저 반영) | `_____ 분` | 목표: < 5분 |
| `https://thirdtool.dev` HTML 응답 시각 | `_____` | curl `-I` 결과 |
| `curl -I https://thirdtool.dev` 응답 헤더 | `_____` | `Content-Type` / `Cache-Control` |
| 첫 실패·디버깅 사이클 횟수 | `_____` | 회고 입력 |

---

## FE 자산 크기 실측 (D6에 채움)

| 항목 | M1 실측 | 참고 |
| --- | --- | --- |
| `dist/` 전체 크기 | `_____ MB` | build 결과물 |
| `dist/index.html` | `_____ KB` | no-cache 대상 |
| `dist/assets/*.js` 합계 | `_____ KB` | immutable 1y |
| `dist/assets/*.css` 합계 | `_____ KB` | immutable 1y |
| 최대 단일 JS chunk | `_____ KB` | 코드 스플리팅 검토 대상 |
| gzip 후 총 크기 | `_____ KB` | CF 전송 실체 |

---

## 인프라 산출물 링크 (M1 종료 후)

- FE 레포 `.env.production` 위치: `.env.production` (커밋됨)
- FE 레포 `.env.example` 위치 (있는 경우): `_____`
- GitHub Actions workflow: `.github/workflows/deploy-fe.yml` (BE 레포 D9)
- CloudFront Distribution ID (GitHub Variables `CF_DIST_ID`): `_____`
- S3 버킷 URI: `s3://thirdtool-fe-prod/`
- OIDC gha-deploy-role ARN: `_____`
- IAM policy 참조: BE 레포 `infra/iam/gha-deploy-role-permissions-policy.json` (FeSyncS3 + FeCdnInvalidate sid)

---

## Tier 2 · Lighthouse baseline (도전 — 도달 시 채움)

첫 배포 활성 후 Lighthouse 1회 수동 실행.

| 카테고리 | M1 baseline | 목표 (M2 이후) |
| --- | --- | --- |
| Performance | `_____` | ≥ 80 |
| Accessibility | `_____` | ≥ 90 |
| Best Practices | `_____` | ≥ 90 |
| SEO | `_____` | ≥ 80 |

실행 조건:
- URL: `https://thirdtool.dev/`
- 실행 모드: Mobile / Desktop 각 1회
- 사전 warmup: 홈 페이지 1회 로드 후 측정
- 결과 캡처: `_____` (스크린샷 or JSON export)

---

## M2 진입 시 인프라 보완 후보

- CloudFront Cache Hit Rate 모니터링 (CloudWatch 대시보드) — `product-fe-cdn.md` 관측 지표
- Lighthouse CI GHA step 통합 — `FE-ADR-CANDIDATES.md` P1-10
- FE 레포 auto-trigger (main push 자동 배포) 전환 — v2 계획
- Preview URL 인프라 (Vercel 스타일) 검토 — `FE-ADR-CANDIDATES.md` P2-7
- Sentry 도입 검토 — `product-auth.md` 열린 질문
- Web Vitals RUM 도입 (실사용자 성능 지표) — Sentry 또는 별도

---

## 백엔드 마일스톤 정합 확인 (D6에)

FE M1과 BE M1 종료 신호를 나란히 놓고 정합 확인.

| BE M1 신호 | 상태 | FE M1 대응 신호 | 상태 |
| --- | --- | --- | --- |
| `https://api.thirdtool.dev/actuator/health` 200 | `_____` | `https://thirdtool.dev` FE SPA 로드 | `_____` |
| `curl -I http://api...` → 301 | `_____` | (해당 없음) | — |
| FE → API E2E CORS OK | `_____` | 동일 신호 (FE-3-2) | `_____` |
| BE Story 5-4 UserUpdateRequestDTO | ✅ 완료 | FE username grep 0건 | `_____` |
