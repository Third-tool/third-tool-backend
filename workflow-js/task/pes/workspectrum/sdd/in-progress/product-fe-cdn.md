## [Product] FE CDN — S3 · CloudFront · Route53 · GHA FE CI/CD

# [Product] FE CDN — S3 · CloudFront · Route53 · GHA FE CI/CD

## Product Vision

> ThirdTool FE(React/Vite SPA)의 빌드 산출물을 S3에 저장하고 CloudFront를 거쳐 전세계에 캐싱·배포한다.
`thirdtool.dev` 도메인으로 접속하면 CloudFront가 S3에서 HTML/JS/CSS를 서빙하고, FE가 `api.thirdtool.dev`(ALB → ECS)를 호출하는 완전한 프론트엔드 delivery 파이프라인을 만든다.
GHA FE CI/CD가 FE 레포의 main push를 감지해 빌드 → S3 sync → CloudFront 캐시 무효화를 자동으로 완결한다.
`.dev` TLD의 HSTS preload 정책상 HTTPS가 강제되므로 CloudFront에 us-east-1 ACM 인증서를 붙여 TLS를 종단한다.
>

## 배경 및 문제

- 현재 상황 (As-Is)
    - FE 레포에 빌드 가능한 React/Vite 코드가 있으나 자동 배포 파이프라인이 없음 — 빌드 산출물을 어디에, 어떻게 올릴지 미결
    - API 엔드포인트가 `localhost` 하드코딩 또는 `thirdstool.com` 구형 도메인 — 운영 도메인 `api.thirdtool.dev`로 연결 안 됨
    - BE는 `api.thirdtool.dev`(ALB)를 통해 서빙되지만 FE를 브라우저에서 접속할 수 있는 URL이 없음 — 1차 배포가 BE only 상태
    - `.dev` TLD = HSTS preloaded → HTTP 불허, HTTPS 필수. S3 직접 접근 / HTTP 서빙 모두 불가
- 발생하는 문제
    - 브라우저에서 `thirdtool.dev`로 접속하면 아무것도 뜨지 않음 → "완전히 동작하는" 상태가 아님
    - API 도메인이 하드코딩이면 FE 레포 배포 시마다 URL 수정 필요 → 사람 실수 매개체
    - FE 변경사항이 운영에 반영되려면 수동 빌드 + 수동 업로드 → 자동화 없어 배포 마찰
    - OAuth 소셜 로그인이 `api.thirdtool.dev` 콜백을 기대하는데 FE가 다른 도메인을 가리키면 CORS + 쿠키 불일치
- 왜 지금 해결해야 하는가
    - BE(`api.thirdtool.dev`)가 ALB + ECS로 준비됐지만 사용자가 접속할 FE entry point가 없으면 서비스 완성 불가 — 1차 배포의 마지막 퍼즐 조각
    - `thirdtool.dev` apex 도메인을 ALB에 직결하지 않기로 결정(FE/BE 역할 분리) → apex가 CloudFront를 가리키는 것이 자연스러운 귀결
    - FE 배포 자동화 없이는 BE 배포와 FE 배포가 항상 동기화되지 않아 버전 불일치 사고 가능

## 목표 (To-Be)

- `thirdtool.dev` / `www.thirdtool.dev` 로 접속하면 FE React SPA가 로드된다 (CloudFront → S3)
- `https://thirdtool.dev` → HTTP/2, TLS 1.2+, 정적 자산 CDN 캐싱 (Cache-Control `immutable` for hashed assets)
- `index.html`은 no-cache — SPA 진입점이 항상 최신 버전을 가져옴
- FE SPA 내 모든 라우팅이 `/index.html`로 폴백 (CloudFront 404/403 → 200 index.html)
- FE 레포 main push → GHA가 자동으로 빌드·S3 sync·CF 무효화 완결 (배포 리드타임 < 5분)
- `VITE_API_BASE_URL=https://api.thirdtool.dev` 환경변수로 API 도메인 하드코딩 0건
- CloudFront Distribution ARN이 GitHub Variables `CF_DIST_ID`에 등록되어 GHA가 무효화를 자동 실행
- FE 소셜 로그인 → `api.thirdtool.dev/oauth/{kakao,naver}/callback` 정상 호출 (CORS OK, SameSite=Strict 쿠키 전달)

## 설계 결정 (Design Decision)

> **S3 정적 호스팅 + CloudFront OAC 패턴을 채택한다. S3 퍼블릭 접근은 완전 차단.**
보안과 CDN 효익의 표준 조합.
>
> - S3 버킷 `thirdtool-fe-prod`에 퍼블릭 접근 차단(Block Public Access all=true) + 버킷 정책은 OAC(Origin Access Control)만 허용
> - CloudFront가 S3 객체를 SigV4 서명으로 요청 → S3가 CloudFront만 허용
> - OAC는 OAI(Origin Access Identity)의 후계자로 AWS 권장 방식 (OAI는 deprecated 예정)
> - S3 정적 웹 호스팅 엔드포인트 사용 안 함 — OAC는 REST API 엔드포인트 직접 사용
> - 이 결정은 ADR로 별도 기록한다 (`ADR-CDN-001: S3+CloudFront OAC Pattern`)

> **CloudFront 인증서는 us-east-1 ACM만 허용 — ap-northeast-2 인증서 재사용 불가.**
AWS 설계 제약에 따른 필수 선택.
>
> - CloudFront는 글로벌 엣지 서비스 특성상 인증서를 us-east-1 ACM에서만 로딩
> - BE ALB용(ap-northeast-2 ACM)과 FE CloudFront용(us-east-1 ACM)을 반드시 별도 발급
> - DNS validation CNAME은 동일 Route53 hosted zone에 1회 추가하면 두 리전 인증서 모두 검증됨 — 중복 작업 없음
> - 이 결정은 ADR로 별도 기록한다 (`ADR-CDN-002: us-east-1 ACM for CloudFront`)

> **SPA 라우팅 폴백은 CloudFront 커스텀 에러 응답 404/403 → /index.html (200)으로 처리한다.**
React Router(History API) SPA의 필수 설정.
>
> - 사용자가 `thirdtool.dev/deck/123` 직접 접근 시 S3에 해당 파일 없음 → 403/404
> - CloudFront가 이를 가로채 `/index.html`로 재서빙 (HTTP 200) → React Router가 클라이언트 라우팅
> - `ErrorCachingMinTTL=10` — 에러 응답 캐싱을 최소화해 배포 직후 신규 index.html이 바로 노출
> - 이 결정은 ADR로 별도 기록한다 (`ADR-CDN-003: SPA Routing Fallback via CloudFront Error Response`)

> **정적 자산은 해시 기반 파일명 + `max-age=31536000,immutable`, index.html은 `no-cache`로 이중 정책을 적용한다.**
캐싱 효율과 즉시 배포의 균형.
>
> - Vite 빌드는 JS/CSS를 `assets/main.a1b2c3d4.js`처럼 콘텐츠 해시 포함 파일명으로 출력 → 파일명이 바뀌면 자동으로 캐시 무효화
> - 해시 포함 파일: `Cache-Control: public,max-age=31536000,immutable` → CDN 1년 캐싱
> - `index.html`: `Cache-Control: no-cache,no-store,must-revalidate` → 항상 최신 index.html 반환
> - S3 sync 시 `--exclude "index.html"`로 분리 업로드하여 각각 다른 Cache-Control 헤더 설정
> - 이 결정은 ADR로 별도 기록한다 (`ADR-CDN-004: Split Cache-Control for SPA`)

> **GHA FE 배포 트리거는 `workflow_dispatch`(수동)로 시작하고 auto-trigger(FE 레포 push)는 v2로.**
독립 레포 구조에서 자동 트리거 설정의 점진적 도입.
>
> - FE와 BE가 별도 레포 → BE 레포의 `deploy-fe.yml`이 FE 레포 push를 자동 감지하려면 cross-repo trigger 또는 FE 레포에 직접 workflow 추가 필요
> - v1: `workflow_dispatch` 수동 실행 + 수동 트리거로 안전하게 시작
> - v2: FE 레포에 `deploy-fe.yml` 이식(또는 FE 레포 내 독립 workflow 파일)하여 자동 배포
> - 이 결정은 ADR로 별도 기록한다 (`ADR-CDN-005: FE Deploy Trigger Strategy`)

## 대안 검토 (Alternatives Considered)

### FE 호스팅 방식

**Option A — Vercel / Netlify**
- 장점: 즉시 HTTPS + CDN + 자동 PR 미리보기, 설정 거의 없음
- 거부 이유:
    - AWS 단일 클라우드 전략 이탈 → 포트폴리오 일관성 저하
    - 무료 티어 트래픽 한도 초과 시 비용 예측 불가
    - CORS 설정이 도메인 기반이라 추가 고려 필요
    - "S3+CloudFront 어떻게 설정했나"라는 인프라 면접 질문에 답변 불가

**Option B — GitHub Pages**
- 장점: 무료, 설정 간단
- 거부 이유:
    - HTTPS 강제지만 커스텀 도메인 HTTPS는 Let's Encrypt 의존 (자동 갱신 있지만 AWS ACM과 무관)
    - `github.io` 도메인 또는 커스텀 도메인 CNAME — Route53 A alias 미지원 (apex 도메인 연결 불가)
    - CDN 캐싱 정책 제어 불가 (Cache-Control 헤더 커스터마이즈 없음)

**Option C (선택) — S3 + CloudFront + Route53 A alias**
- 비용: CloudFront 무료 티어 1TB/월 + S3 ~10MB 정적 자산 = 무시할 수준
- 보상: AWS 통합 + ACM 자동 갱신 + CDN 글로벌 엣지 + 완전한 Cache-Control 제어 + 면접 답변 가능

### CloudFront 캐시 정책

**Option A — AWS managed CachingDisabled**
- 거부 이유: CDN의 핵심 효익 포기. 매 요청 S3로 origin fetch → 성능 저하 + S3 비용 증가

**Option B — 단일 정책으로 통일 (max-age=0)**
- 거부 이유: 정적 자산까지 매 요청 재검증 → 캐시 효율 0

**Option C (선택) — 해시 자산 `immutable` + index.html `no-cache` 이중 정책**
- 비용: S3 sync 시 파일 분리 업로드 단계 추가
- 보상: 배포 즉시 반영 (index.html) + CDN 최대 효율 (해시 자산) 동시 달성

### FE 레포 API URL 관리

**Option A — 하드코딩 (`https://api.thirdtool.dev`)**
- 거부 이유: 로컬 개발 시 prod API 호출, 환경별 분기 불가

**Option B — `.env.{environment}` 파일 (선택)**
- 비용: 파일 추가 필요
- 보상: Vite의 환경별 import.meta.env 지원, 로컬·prod 분기 자연스러움
- `.env.production`: `VITE_API_BASE_URL=https://api.thirdtool.dev`
- `.env.development`: `VITE_API_BASE_URL=http://localhost:8080`

## 전체 아키텍처 (High-Level Architecture)

### 트래픽 흐름

```
[사용자 브라우저]
       │
       │  https://thirdtool.dev (또는 www.)
       ▼
┌──────────────────────────┐
│     Route53              │  apex A alias → CloudFront (Z2FDTNDATAQYW2)
│     (Hosted Zone)        │  www A alias  → CloudFront (Z2FDTNDATAQYW2)
└────────────┬─────────────┘
             │
             ▼
┌──────────────────────────────────────────────────┐
│  CloudFront Distribution                         │
│  - Aliases: thirdtool.dev, www.thirdtool.dev     │
│  - Viewer Protocol: redirect-to-https            │
│  - TLS: us-east-1 ACM (*.thirdtool.dev)          │
│  - HTTP/2 + HTTP/3                               │
│  - PriceClass_200 (북미·유럽·아시아 엣지)         │
│  - Custom Error: 404/403 → /index.html (200)     │
│                                                  │
│  Cache Behavior:                                 │
│   GET/HEAD + CachingOptimized policy             │
│   (658327ea... AWS managed)                      │
└────────────┬─────────────────────────────────────┘
             │  SigV4 (OAC)
             ▼
┌──────────────────────────┐
│  S3: thirdtool-fe-prod   │  ap-northeast-2
│  - Block Public Access   │
│  - Bucket policy: CF OAC │
│  - 구조:                  │
│    /index.html           │  ← no-cache
│    /assets/*.js          │  ← immutable 1y
│    /assets/*.css         │  ← immutable 1y
│    /favicon.ico 등       │
└──────────────────────────┘

[별도 BE 호출 흐름]
FE JS → fetch('https://api.thirdtool.dev/api/v1/...')
             │
             ▼
        ALB → ECS (product-infra-network.md · product-infra-deploy.md)
```

### GHA FE 배포 파이프라인

```
[운영자] GitHub Actions 수동 트리거 (workflow_dispatch)
              ※ v2: FE 레포 main push 자동 트리거
       │
       ▼
┌──────────────────────────────────────────────────────┐
│  .github/workflows/deploy-fe.yml                     │
│                                                      │
│  1. actions/checkout@v4                              │
│  2. actions/setup-node@v4 (Node 20, npm cache)       │
│  3. npm ci                                           │
│  4. npm run build                                    │
│       VITE_API_BASE_URL=https://api.thirdtool.dev    │
│  5. aws-actions/configure-aws-credentials@v4 (OIDC) │
│       role: gha-deploy-role                          │
│       권한: s3:PutObject/Delete/Get/List + CF 무효화  │
│  6. aws s3 sync dist/ s3://thirdtool-fe-prod --delete│
│       --exclude "index.html"                         │
│       --cache-control "public,max-age=31536000,      │
│                         immutable"                   │
│  7. aws s3 cp dist/index.html ... \                  │
│       --cache-control "no-cache,no-store,..."        │
│  8. aws cloudfront create-invalidation               │
│       --distribution-id $CF_DIST_ID --paths "/*"    │
└──────────────────────────────────────────────────────┘
```

### IAM 권한 (gha-deploy-role 추가분)

```json
{
  "Sid": "FeSyncS3",
  "Effect": "Allow",
  "Action": ["s3:PutObject","s3:DeleteObject","s3:GetObject","s3:ListBucket"],
  "Resource": ["arn:aws:s3:::thirdtool-fe-prod","arn:aws:s3:::thirdtool-fe-prod/*"]
},
{
  "Sid": "FeCdnInvalidate",
  "Effect": "Allow",
  "Action": "cloudfront:CreateInvalidation",
  "Resource": "*"
}
```

*(✅ infra/iam/gha-deploy-role-permissions-policy.json에 반영 완료)*

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

| 시나리오 | 감지 신호 | 즉시 대응 | 근본 대책 |
| --- | --- | --- | --- |
| index.html 캐시로 구버전 로드 | 새 배포 후에도 구 JS bundle 로드 | CF 무효화 수동 실행 + 브라우저 강제 새로고침 | GHA step 8 CF invalidation 자동화 (✅ 완료) |
| 404 — SPA 라우팅 실패 | 직접 URL 접근 시 403/404 응답 | CloudFront 커스텀 에러 응답 설정 확인 | cloudfront-dist.json CustomErrorResponses 검증 |
| CORS 오류 | FE 콘솔: `blocked by CORS policy` | SecurityConfig allowedOrigins 확인 | `thirdtool.dev` CORS 등록 확인 (✅ 완료) |
| S3 버킷 정책 오류 | CF origin 403 | OAC ID가 cloudfront-dist.json과 bucket-policy에 일치하는지 확인 | infra/s3/ spec과 실제 리소스 비교 |
| CF 인증서 만료 | 브라우저 HTTPS 경고 | us-east-1 ACM DaysToExpiry 확인 + 재검증 | DNS validation CNAME 존재 확인 (자동 갱신) |
| GHA OIDC 실패 | STS AccessDenied | gha-deploy-role의 FE 권한 확인 | gha-deploy-role-permissions-policy.json의 FeSyncS3·FeCdnInvalidate sid 확인 |

### 관측 지표

| 지표 | 측정 방법 | 목표 |
| --- | --- | --- |
| FE 배포 리드타임 | GHA workflow duration | < 5분 |
| CloudFront Cache Hit Rate | CloudWatch `CacheHitRate` 메트릭 | > 95% (정적 자산) |
| index.html 요청 지연 | CloudFront `OriginLatency` | < 200ms (Origin fetch) |
| 4xx 비율 | CloudFront `4xxErrorRate` | < 0.1% |
| S3 스토리지 | AWS Console | < 50MB (정적 자산만) |

## 롤아웃 / 마이그레이션 (Rollout)

### Epic 실행 순서

```
전제: Route53 thirdtool.dev 도메인 등록 (D1 — 사용자 직접)
        │
        ▼
Epic 1 (ACM + S3 기반)
  Story 1-1: ACM us-east-1 인증서 신청 + DNS 검증 CNAME (Route53)
             ※ ap-northeast-2 ACM(ALB용)과 동시 신청 가능 — CNAME 공유
  Story 1-2: S3 thirdtool-fe-prod 버킷 생성 + 퍼블릭 접근 차단
             ※ ACM ISSUED와 무관하게 진행 가능
        │
        ▼
Epic 2 (CloudFront 배포)
  Story 2-1: OAC 생성 → CloudFront 배포 생성 (us-east-1 ACM ISSUED 후)
  Story 2-2: S3 버킷 정책 업데이트 (CloudFront ARN 주입)
        │
        ▼
Epic 3 (Route53 + FE 환경변수)
  Story 3-1: Route53 thirdtool.dev + www A alias → CloudFront
  Story 3-2: FE 레포 .env.production 추가 (VITE_API_BASE_URL)
             ※ 사용자가 FE 레포에서 직접
        │
        ▼
Epic 4 (GHA FE CI/CD)
  Story 4-1: GitHub Variables CF_DIST_ID 등록 + deploy-fe.yml 첫 실행
             → S3 sync + CF invalidation 검증
```

### 단계별 검증 게이트

| 단계 | 검증 명령 | 통과 기준 |
| --- | --- | --- |
| ACM ISSUED | `aws acm describe-certificate --region us-east-1 --query Certificate.Status` | `"ISSUED"` |
| S3 버킷 | `aws s3 ls s3://thirdtool-fe-prod` | 빈 버킷 접근 성공 |
| CloudFront 배포 | `curl -I https://thirdtool.dev` (DNS 전파 후) | HTTP/2 200 (FE 로드) 또는 503 (S3 비어있음) |
| S3 sync 완료 | `aws s3 ls s3://thirdtool-fe-prod --recursive \| grep index.html` | index.html 존재 |
| FE 정상 로드 | 브라우저 `https://thirdtool.dev` | FE 메인 페이지 로드 |
| SPA 라우팅 | 브라우저 직접 URL `https://thirdtool.dev/some/route` | 404 없이 SPA 로드 |
| API 연동 | FE에서 로그인 시도 | CORS OK + `api.thirdtool.dev` 호출 성공 |

## 성공 지표 (KPI)

| 지표 | 현재 값 | 목표 값 | 측정 방법 |
| --- | --- | --- | --- |
| `https://thirdtool.dev` 접속 가능 여부 | 불가 | 가능 (HTTP/2 200) | `curl -I https://thirdtool.dev` |
| FE API URL 하드코딩 건수 | 미상 | 0 (`.env.production`만) | FE 레포 `grep -r "localhost\|thirdstool"` |
| GHA FE 배포 자동화 | 없음 | `workflow_dispatch` 동작 | GHA 워크플로 수동 실행 성공 |
| S3 퍼블릭 접근 차단 | 미확인 | Block All Public Access | `aws s3api get-public-access-block --bucket thirdtool-fe-prod` |
| CF Cache Hit Rate | — | > 95% | CloudWatch `CacheHitRate` |

## Scope

- **In Scope**
    - S3 버킷 `thirdtool-fe-prod` (ap-northeast-2) + 퍼블릭 접근 차단 + OAC 버킷 정책
    - CloudFront OAC + Distribution (`thirdtool.dev`, `www.thirdtool.dev` alias)
    - CloudFront TLS: us-east-1 ACM `*.thirdtool.dev` (DNS validation, 자동 갱신)
    - CloudFront 커스텀 에러 응답 (404/403 → /index.html, 200)
    - Route53 `thirdtool.dev` + `www.thirdtool.dev` A alias → CloudFront (Z2FDTNDATAQYW2)
    - GHA `deploy-fe.yml` — OIDC AssumeRole + npm build + S3 sync + CF invalidation
    - `gha-deploy-role`에 S3 + CloudFront 권한 추가 (✅ 완료, infra/iam/gha-deploy-role-permissions-policy.json)
    - FE 레포 `.env.production` (`VITE_API_BASE_URL=https://api.thirdtool.dev`)
    - CORS `thirdtool.dev` + `www.thirdtool.dev` 등록 (✅ 완료, SecurityConfig · MvcConfig)
    - OAuth redirect URI → `api.thirdtool.dev` 전환 (✅ 완료, application-prod.yml)
- **Out of Scope**
    - WAF (CloudFront 앞단) — v2
    - staging FE CDN (`staging.thirdtool.dev` → 별도 CF Distribution) — v2
    - FE 레포 자동 push 트리거 (workflow_dispatch에서 자동 전환) — v2
    - S3 버킷 버전 관리 / 라이프사이클 정책 — product-infra-ops.md에서 통합
    - CloudFront 액세스 로그 → S3 (v2, product-infra-ops.md와 연계)
    - 멀티 오리진 (API 캐싱 없음, FE 정적 자산만) — 단순화 선택

## 대상 사용자

- 주요 사용자: ThirdTool 풀스택 개발자 (1인 운영)
- 사용 맥락:
    - FE 기능 변경 후 → GHA `deploy-fe.yml` 수동 실행 → CF 캐시 무효화 후 `https://thirdtool.dev` 확인
    - FE가 API 호출 실패 시 → CORS 정책 · `VITE_API_BASE_URL` 환경변수 확인
    - CloudFront 인증서 만료 우려 → `aws acm describe-certificate --region us-east-1` 확인
    - 면접·포트폴리오 → "S3+CloudFront OAC, SPA 라우팅 폴백, us-east-1 ACM 제약"으로 답변

## 연결된 Epic 목록

- [ ]  Epic 1. ACM (us-east-1) + S3 버킷 — 인증서 발급·검증 + 버킷 생성·접근 차단
- [ ]  Epic 2. CloudFront 배포 — OAC 생성 + Distribution 생성 + S3 버킷 정책
- [ ]  Epic 3. Route53 FE 레코드 + FE 환경변수 — apex/www A alias + .env.production
- [ ]  Epic 4. GHA FE CI/CD — deploy-fe.yml 첫 실행 + 검증

## 관련 문서

- 선행 Product:
    - `product-infra-network.md` — Route53 hosted zone · ACM(ap-northeast-2) · DNS 검증 CNAME 공유
- 연계 Product:
    - `product-infra-deploy.md` — gha-deploy-role OIDC 패턴 재사용, FE 권한 추가됨
    - `product-infra-ops.md` — S3 버킷 버전 관리 · CloudFront 액세스 로그 (v2 연계)
- 인프라 spec 파일:
    - `infra/acm/acm-us-east-1.json` — CloudFront용 ACM 신청 spec
    - `infra/cloudfront/cloudfront-oac.json` — OAC 생성 spec
    - `infra/cloudfront/cloudfront-dist.json` — CloudFront Distribution spec
    - `infra/s3/s3-fe-bucket.json` — S3 버킷 생성 spec
    - `infra/s3/s3-fe-bucket-policy.json` — OAC 버킷 정책 spec
    - `infra/route53/route53-fe-record.json` — apex/www A alias spec
    - `.github/workflows/deploy-fe.yml` — GHA FE 배포 워크플로
    - `infra/DEPLOY-RUNBOOK.md` — Phase 4~6 전체 실행 런북
- ADR 후보: `ADR-CDN-001~005`
- 참고 자료: AWS CloudFront OAC 가이드 · Vite 환경변수 문서 · SPA 라우팅 CloudFront 패턴

## 열린 질문 (Open Questions)

- **staging FE 분기 방식**: `staging.thirdtool.dev` → 별도 CloudFront Distribution(S3 버킷 분리) vs 단일 Distribution에 Lambda@Edge로 환경 분기. v2에서 staging FE 배포 필요 시점에 결정.
- **FE 레포 자동 push 트리거**: BE 레포에 FE 레포 push를 감지하는 cross-repo trigger 패턴은 GitHub App 또는 repository_dispatch 이벤트. FE 레포 내부에 workflow를 직접 두는 것이 더 단순함. v2에서 FE 레포 구조 확정 후 결정.
- **CloudFront 앞단 WAF 필요 시점**: DDoS 방어 + 봇 차단이 필요한 트래픽 규모 임계점(MAU 1,000명?) 도달 시 검토. managed rule $5/월 + 요청당 과금.
- **S3 버킷 버전 관리**: FE 배포 실수로 잘못된 자산 올렸을 때 S3 버전 복구가 필요한 시나리오. product-infra-ops.md S3 버전 관리 범위에 포함시킬지.
