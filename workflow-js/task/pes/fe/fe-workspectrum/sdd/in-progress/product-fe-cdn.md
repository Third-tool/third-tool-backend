# [Product] FE CDN FE — S3 + CloudFront + Route53 + GHA 배포 파이프라인

## Product Vision

> `thirdtool.dev` 진입 시 CloudFront → S3에서 React SPA를 서빙하고, FE가 `api.thirdtool.dev`(ALB → ECS 백엔드)를 호출하는 완전한 delivery 파이프라인을 구축한다. GHA가 FE 레포 main 커밋을 감지해 build → S3 sync → CloudFront invalidation을 자동으로 완결한다. `.dev` TLD의 HSTS preload 정책상 HTTPS 강제 → CloudFront + us-east-1 ACM.

## 배경 및 문제

- **현재 상황 (As-Is)**
  - FE 레포에 Vite build 산출물 있으나 자동 배포 파이프라인 없음
  - `VITE_API_BASE_URL`이 `.env.*` 파일로 분리되어 있지만 CI/CD 파이프라인이 없어 수동 배포에 의존
  - `thirdtool.dev`로 접속하면 아무것도 없음 (BE는 `api.thirdtool.dev`로 접속 가능)
- **발생하는 문제**
  - "완전 동작하는" 서비스 상태에 못 도달 (FE 접근 URL 없음)
  - `VITE_API_BASE_URL` 하드코딩되면 배포 시마다 수정 필요
  - 소셜 로그인 콜백 URI가 프로덕션 도메인에서 정상 동작하는지 검증 표면 없음
- **왜 지금 해결해야 하는가**
  - 백엔드가 ALB + ECS로 배포됨. FE 진입점 없이는 1차 배포 미완결
  - `product-auth.md`의 CORS + 쿠키 정합성 검증이 프로덕션 도메인에서만 가능

## 목표 (To-Be)

- `https://thirdtool.dev` / `https://www.thirdtool.dev` 접속 시 React SPA 로드
- 정적 자산은 immutable 1년, `index.html`은 no-cache
- SPA 라우팅 폴백 (404/403 → `/index.html` 200)
- GHA workflow가 FE 레포 main push (또는 workflow_dispatch)로 빌드 → S3 sync → CF invalidation 자동
- `VITE_API_BASE_URL=https://api.thirdtool.dev` 환경 분기 (`.env.production` vs `.env.development`)
- 소셜 로그인 → `api.thirdtool.dev/social/login/{provider}` 정상 (CORS + SameSite=None + Secure 쿠키)
- 배포 리드타임 < 5분

## 설계 결정 (Design Decisions)

- **S3 + CloudFront (OAC) + Route53 A alias**
  - S3 Block Public Access all=true. CloudFront OAC로만 접근
  - Route53 A alias: apex + www → CloudFront distribution
- **CloudFront 인증서 = us-east-1 ACM만**
  - CloudFront는 글로벌 서비스, us-east-1에서 인증서 로딩
  - BE ALB용(ap-northeast-2 ACM)과 별도 발급
- **SPA 라우팅 폴백 = CloudFront Custom Error Response**
  - 404/403 → `/index.html` (200)
  - `ErrorCachingMinTTL=10`
- **캐시 정책 이중화**
  - 해시 자산(`assets/main.a1b2c3d4.js`): `Cache-Control: public,max-age=31536000,immutable`
  - `index.html`: `Cache-Control: no-cache,no-store,must-revalidate`
  - S3 sync 시 `--exclude "index.html"` 분리 업로드
- **GHA trigger v1 = workflow_dispatch (수동) + FE 레포 push (조건부)**
  - 초기 안전 시작. v2에서 완전 자동 트리거
- **Vite 환경변수 = `.env.production` / `.env.development` / `.env.local`**
  - `VITE_API_BASE_URL`이 유일 필수 값
  - MSW enable 여부는 개발 전용 (`import.meta.env.DEV` 분기)

## 대안 검토 (Alternatives Considered)

### 호스팅

**Option A — Vercel / Netlify**
- 거부 이유: AWS 통합 이탈, 트래픽 한도 비용 예측 불가

**Option B — GitHub Pages**
- 거부 이유: apex 도메인 CNAME 제약, Cache-Control 커스터마이즈 불가

**Option C (선택) — S3 + CloudFront + Route53 A alias**
- 비용: 설정 복잡도 (Epic 1~3)
- 보상: 완전 제어, AWS 통합, 면접 답변 가능

### 캐시 정책

**Option A — 단일 max-age=0**
- 거부 이유: CDN 효율 0

**Option B (선택) — 해시 assets immutable + index.html no-cache**
- 비용: sync 명령 이중화
- 보상: CDN Cache Hit Rate 극대화

### GHA trigger

**Option A — main push 자동 트리거만**
- 거부 이유: 초기 검증 부담

**Option B (선택) — workflow_dispatch(v1) → 자동(v2)**
- 비용: 수동 트리거 단계 필요
- 보상: 안전한 초기 검증, v2에서 자동화

## 전체 아키텍처 (High-Level Architecture)

### 트래픽 흐름

```
[사용자 브라우저]
   │
   │  https://thirdtool.dev
   ▼
[Route53]  A alias
   │
   ▼
[CloudFront]
   ├─ Aliases: thirdtool.dev, www.thirdtool.dev
   ├─ Viewer Protocol: redirect-to-https
   ├─ TLS: us-east-1 ACM (*.thirdtool.dev)
   ├─ Custom Error: 404/403 → /index.html (200)
   ├─ Cache: CachingOptimized (AWS managed)
   └─ OAC → S3
   │
   ▼
[S3 bucket: thirdtool-fe-prod]
   ├─ /index.html      ← no-cache
   └─ /assets/*.{js,css,ico}   ← immutable 1y

[별도 API 흐름]
FE JS → fetch('https://api.thirdtool.dev/api/v1/...')
       → ALB → ECS (백엔드)
```

### GHA 파이프라인

```
[운영자 workflow_dispatch (v1)]
   │
   ▼
.github/workflows/deploy-fe.yml
   1. checkout@v4
   2. setup-node@v4 (Node 20 + npm cache)
   3. npm ci
   4. npm run build   (VITE_API_BASE_URL=https://api.thirdtool.dev)
   5. configure-aws-credentials (OIDC → gha-deploy-role)
   6. aws s3 sync dist/ s3://thirdtool-fe-prod --delete
        --exclude "index.html"
        --cache-control "public,max-age=31536000,immutable"
   7. aws s3 cp dist/index.html s3://thirdtool-fe-prod/index.html
        --cache-control "no-cache,no-store,must-revalidate"
   8. aws cloudfront create-invalidation --distribution-id $CF_DIST_ID --paths "/*"
```

### 외부 의존

- **AWS S3 / CloudFront / Route53 / us-east-1 ACM**: 인프라 리소스
- **GitHub Actions OIDC → AWS IAM (gha-deploy-role)**: 무자격 배포
- **백엔드 `api.thirdtool.dev` (ALB → ECS)**: FE 별 API 진입점
- **모든 다른 FE Product**: 배포 인프라 소비자

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | 감지 방법 | HTTP | 즉시 대응 · 근본 대책 |
| --- | --- | --- | --- |
| 캐시로 구버전 index.html | 새 배포 후 구 bundle 로드 | 200 (stale) | CF invalidation 수동 실행 · GHA step 8 자동화 (완료) |
| 직접 URL 접근 404 | 브라우저 404 | 404 | CustomErrorResponses 설정 확인 · cloudfront-dist.json 검증 |
| CORS blocked | 콘솔 CORS error | (browser) | 백엔드 allowedOrigins에 `thirdtool.dev` 등록 확인 |
| S3 bucket policy 오류 | CF origin 403 | 403 | OAC ID 정합 확인 · bucket-policy.json 검토 |
| CF 인증서 만료 | 브라우저 HTTPS 경고 | (TLS) | us-east-1 ACM 재검증 · DNS validation CNAME 존재 확인 |
| GHA OIDC AccessDenied | STS 실패 | (STS) | gha-deploy-role FE 권한 확인 · permissions-policy.json FeSyncS3/FeCdnInvalidate |
| Route53 DNS 전파 지연 | 배포 후 접속 실패 | (DNS) | dig 조회 · TTL 확인 · 최대 5분 대기 |

### 로깅 정책 (인프라)

- **항상 기록 (CloudWatch)**:
  - CloudFront access log (S3 저장)
  - S3 access log (별도 log 버킷)
  - GHA workflow duration + step 결과
- **debug**: OIDC assume role 실패 시 STS error
- **절대 금지**:
  - S3 bucket policy에 public read 부여
  - GHA에 AWS access key 하드코딩 (OIDC만)
  - CloudFront TLS 정책 1.2 미만 허용

### 관측 지표

| 지표 | 측정 | 목표 |
| --- | --- | --- |
| 배포 리드타임 | GHA duration | < 5분 |
| CF Cache Hit Rate | CloudWatch `CacheHitRate` | > 95% (정적 자산) |
| index.html Origin Latency | CloudWatch `OriginLatency` | < 200ms |
| 4xx 비율 | `4xxErrorRate` | < 0.1% |
| 5xx 비율 | `5xxErrorRate` | < 0.01% |
| S3 스토리지 | AWS Console | < 50MB |
| CF invalidation 발생 횟수 (월) | CloudWatch | ≤ 30건 (무료 한도) |

## 롤아웃 / 마이그레이션 (Rollout)

### 전제

- 현재 사용자: FE 진입 URL 부재 (첫 배포)
- 도메인 `thirdtool.dev` Route53 hosted zone 확보 완료
- 백엔드 `api.thirdtool.dev` 배포 완료 (ALB + ECS)
- GHA repository OIDC provider 설정 완료
- AWS IAM `gha-deploy-role` 사전 준비

### Product 의존성

- 선행: (없음, 인프라 Product)
- 후행: **모든 FE Product** — CORS/CDN 없이 배포 불가
  - `Product.md` (User FE)
  - `./product-auth.md`
  - `./product-deck.md`, `./product-media.md`, `./product-search.md`
  - `./product-aisuggestion.md`, `./product-ai-interactive-roadmap.md`
- 병존: 백엔드 인프라 Product (별도 관리)

### Epic·Story 의존성 그래프

```
Epic 1 (인증서 + S3 + 버킷 정책)
  Story 1-1 (us-east-1 ACM 발급) ─► 1-2 (S3 버킷 + Block Public Access)
                                    ─► 1-3 (bucket policy OAC)

Epic 2 (CloudFront distribution)
  Story 2-1 (distribution 생성) ─► 2-2 (aliases + TLS) ─► 2-3 (custom error response)
                                                          ─► 2-4 (cache behavior)

Epic 3 (Route53)
  Story 3-1 (apex + www A alias)

Epic 4 (GHA workflow)
  Story 4-1 (deploy-fe.yml 초기) ─► 4-2 (OIDC gha-deploy-role 권한)
                                    ─► 4-3 (cache-control 분리 sync)
                                    ─► 4-4 (invalidation step)

Epic 5 (환경변수 + 검증)
  Story 5-1 (.env.production / .env.development) ─► 5-2 (E2E: 소셜 로그인 콜백)
                                                    ─► 5-3 (CORS + SameSite 검증)
```

### 환경별 설정 분기

| 항목 | dev (`.env.development`) | prod (`.env.production`) |
| --- | --- | --- |
| `VITE_API_BASE_URL` | http://localhost:8080 | https://api.thirdtool.dev |
| MSW | active (`import.meta.env.DEV`) | inactive |
| CloudFront | N/A | active |
| Cookie SameSite | Lax | None + Secure |
| Sentry | local (dev DSN) | prod DSN |

## 성공 지표 (KPI)

| 지표 | 목표 | 측정 |
| --- | --- | --- |
| `thirdtool.dev` 접속 시 SPA 로드 | 100% | E2E |
| 배포 리드타임 | < 5분 | GHA duration |
| Cache Hit Rate | > 95% | CloudWatch |
| `VITE_API_BASE_URL` 하드코딩 | 0건 | 코드 grep (문자열 `api.thirdtool.dev` 검색) |
| 소셜 로그인 콜백 성공률 | ≥ 99% | E2E |
| CF 4xx 비율 | < 0.1% | CloudWatch |

## Scope

**In Scope**:
- S3 버킷 + Block Public Access + OAC bucket policy
- CloudFront distribution + TLS + custom error + cache behavior
- Route53 apex + www A alias
- GHA `.github/workflows/deploy-fe.yml`
- `.env.production` / `.env.development` / `.env.local` 정책
- CORS + SameSite 프로덕션 검증

**Out of Scope**:
- BE 인프라(ALB/ECS/VPC) → 백엔드 인프라 Product (skip 대상)
- Sentry / Web Vitals RUM → v2 (다른 FE Product의 관측 지표는 본 Product 밖)
- 모바일 앱 배포 → v2
- Lighthouse CI 통합 → 열린 질문

## 대상 사용자

- **엔드 사용자** — `thirdtool.dev` 접속 시 즉시 SPA 로드
- **FE 개발자** — workflow_dispatch로 배포, PR 리뷰 시 preview 필요
- **oncall / 운영자** — 배포 실패 시 CloudWatch 로그·GHA 로그 확인
- **다른 FE Product 작성자** — 본 Product의 인프라 위에서 동작

## 연결된 Epic 목록 (진행 순서)

| 순서 | Epic | 제목 | Story 수 | 선행 의존 |
| --- | --- | --- | --- | --- |
| 1 | Epic 1 | 인증서 + S3 + 버킷 정책 | 3 | (없음) |
| 2 | Epic 2 | CloudFront distribution | 4 | Epic 1 |
| 3 | Epic 3 | Route53 | 1 | Epic 2 |
| 4 | Epic 4 | GHA workflow | 4 | Epic 1 |
| 5 | Epic 5 | 환경변수 + 검증 | 3 | Epic 3, 4 |

- [ ] Epic 1: 인증서 + S3 + 버킷 정책
- [ ] Epic 2: CloudFront distribution
- [ ] Epic 3: Route53
- [ ] Epic 4: GHA workflow
- [ ] Epic 5: 환경변수 + 검증

## 관련 문서

- 백엔드 원본: `workflow/task/pes/workspectrum/sdd/in-progress/product-fe-cdn.md`
- 백엔드 ADR 후보: ADR-CDN-001~005
- FE-ADR 후보: `FE-CDN-001: CF 도메인 확정`, `FE-CDN-002: env 파일 정책`, `FE-CDN-003: GHA OIDC 권한 최소화`

## 열린 질문

- **auto-trigger 시점** — FE 레포 main push 자동 배포로 전환하는 v2 timing
- **PR 미리보기(Preview URL) 도입** — Vercel 스타일 preview를 CF로 흉내낼 가치
- **Lighthouse CI 통합** — GHA step에 편입할지, 별도 workflow로 분리할지
- **CDN 지역 최적화** — 아시아 (Seoul edge) 우선 vs 글로벌 default

---

# [Epic 1] 인증서 + S3 + 버킷 정책

## Epic 목표

us-east-1 ACM 인증서 발급, S3 버킷 생성(Block Public Access), OAC bucket policy 설정. CloudFront가 소비할 origin 확립.

## 배경

Product의 인프라 초석. 후속 Epic(CloudFront, Route53, GHA)이 본 Epic의 리소스를 참조.

## 완료 기준

- [ ] Story 1-1, 1-2, 1-3 완료
- [ ] ACM 인증서 상태 = Issued
- [ ] S3 버킷 public access = 완전 차단
- [ ] OAC bucket policy 검증 통과

## [Story 1-1] us-east-1 ACM 발급

### User Story
- As a 운영자
- I want `*.thirdtool.dev` 인증서를 us-east-1에 발급하기를
- so that CloudFront가 사용할 수 있다

### 설명
- AWS ACM 콘솔 → us-east-1 → 도메인 `*.thirdtool.dev`, `thirdtool.dev`
- DNS validation → Route53 hosted zone에 CNAME 자동 등록
- 상태 = Issued 확인

### 완료 기준 (AC)
- Given 발급 요청 / When DNS validation / Then Route53 CNAME 자동 추가
- Given DNS 전파 / When ACM 확인 / Then 상태 Issued
- *(엣지)* Given validation 실패 / When 재시도 / Then CNAME 수동 재확인 절차

### 의존성
- 선행: (없음)
- 후행: Story 1-2, Epic 2

## [Story 1-2] S3 버킷 + Block Public Access

### User Story
- As a 운영자
- I want 정적 자산 저장용 S3 버킷을 완전 사설로 만들기를
- so that OAC를 통한 CF만 접근하도록 강제한다

### 설명
- 버킷명: `thirdtool-fe-prod` (region ap-northeast-2)
- Block Public Access: 4개 옵션 모두 True
- Bucket Versioning: enabled (롤백 여지)

### 완료 기준 (AC)
- Given 버킷 생성 / When 설정 확인 / Then Block Public Access 모두 True
- Given 임의 URL로 직접 접근 / When curl / Then 403
- *(엣지)* Given 버킷 삭제 시도 (실수) / Then MFA delete 정책으로 차단 (v2 검토)

### 의존성
- 선행: Story 1-1 (버킷 정책 이후 인증서 사용)
- 후행: Story 1-3

## [Story 1-3] bucket policy OAC

### User Story
- As a 운영자
- I want CloudFront OAC ID를 bucket policy에 명시하기를
- so that CF 외에 어떤 principal도 s3:GetObject 불가

### 설명
- `bucket-policy.json` — Principal: CF service, Condition: OAC ID 매칭
- Action: `s3:GetObject`
- Resource: `arn:aws:s3:::thirdtool-fe-prod/*`

### 완료 기준 (AC)
- Given policy 등록 / When CF에서 GET / Then 200
- Given 다른 principal / When s3:GetObject / Then AccessDenied
- *(엣지 - OAC ID 오타)* Given 잘못된 OAC ID / When CF request / Then 403 → 정책 재검토

### 의존성
- 선행: Story 1-2 (Story 2-1 CF 생성 후 OAC ID 확정)
- 후행: (Epic 2 완료 후 최종 검증)

---

# [Epic 2] CloudFront distribution

## Epic 목표

CloudFront distribution 생성 + aliases + TLS + custom error response + cache behavior 완성. S3를 origin으로 두고 사용자 진입점 확립.

## 배경

Epic 1의 리소스를 소비. Product의 사용자 진입점 핵심.

## 완료 기준

- [ ] Story 2-1, 2-2, 2-3, 2-4 완료
- [ ] CF distribution 상태 = Deployed
- [ ] `/nonexistent` 접근 시 index.html 200 반환
- [ ] 정적 자산 Cache-Control 정상

## [Story 2-1] distribution 생성

### User Story
- As a 운영자
- I want S3 origin으로 CF distribution을 만들기를
- so that 사용자에게 SPA를 서빙할 수 있다

### 설명
- Origin: `thirdtool-fe-prod.s3.ap-northeast-2.amazonaws.com`
- OAC 생성 → S3 bucket policy에 반영 (Story 1-3)
- Default root object: `index.html`
- Price class: PriceClass_All (or Asia-focused, 열린 질문)

### 완료 기준 (AC)
- Given distribution 생성 / When 상태 확인 / Then Deployed
- Given CF URL 접속 / When 기본 진입 / Then index.html 로드
- *(엣지 - deploy 시간)* Given 생성 직후 / When 접속 / Then 최대 15분 대기 후 정상

### 의존성
- 선행: Epic 1
- 후행: Story 2-2

## [Story 2-2] aliases + TLS

### User Story
- As a 사용자
- I want `thirdtool.dev`로 접속 시 TLS + apex 도메인 서빙받기를
- so that 브라우저 경고 없이 진입할 수 있다

### 설명
- Aliases: `thirdtool.dev`, `www.thirdtool.dev`
- TLS: us-east-1 ACM (Story 1-1)
- Minimum TLS: TLSv1.2_2021
- Viewer Protocol: redirect-to-https

### 완료 기준 (AC)
- Given aliases 등록 / When 저장 / Then distribution deploy 재시작
- Given HTTP 접속 / When 응답 / Then 301 → HTTPS
- *(엣지 - 인증서 미매칭)* Given aliases와 ACM 불일치 / When 저장 / Then AWS 검증 실패

### 의존성
- 선행: Story 2-1
- 후행: Story 2-3

## [Story 2-3] custom error response (SPA 폴백)

### User Story
- As a 사용자
- I want React Router가 처리하는 임의 경로도 index.html이 로드되기를
- so that 새로고침·직접 URL 접근이 정상 동작한다

### 설명
- 404 → `/index.html` (200)
- 403 → `/index.html` (200)
- ErrorCachingMinTTL: 10 (짧게 유지)

### 완료 기준 (AC)
- Given `/nonexistent` 접속 / When CF 응답 / Then index.html + 200
- Given `/some/deep/route` 접속 / When CF 응답 / Then index.html + 200 (React Router가 라우팅)
- *(엣지 - 실제 404 asset)* Given `/assets/missing.js` / When CF 응답 / Then index.html (200) — 무해

### 의존성
- 선행: Story 2-2
- 후행: Story 2-4

## [Story 2-4] cache behavior

### User Story
- As a 운영자
- I want 정적 자산은 CDN 캐시, index.html은 항상 origin에 요청되기를
- so that 새 배포 후 즉시 반영된다

### 설명
- Default behavior: CachingOptimized (AWS managed) — assets
- Behavior `/index.html`: CachingDisabled or short TTL
- 실제 Cache-Control은 S3 sync 시점에 설정 (Epic 4)

### 완료 기준 (AC)
- Given asset 파일 요청 / When 응답 / Then Cache-Control immutable
- Given index.html 요청 / When 응답 / Then Cache-Control no-cache
- *(엣지 - invalidation)* Given `/*` invalidation / When 완료 / Then 다음 요청은 origin 히트

### 의존성
- 선행: Story 2-3
- 후행: Epic 3, 4

---

# [Epic 3] Route53

## Epic 목표

Route53에 apex + www A alias 등록 → 사용자가 `thirdtool.dev` 접속 시 CF distribution 도달.

## 배경

Epic 2의 CF distribution DNS name을 Route53에 매핑. 마지막 사용자 진입 연결.

## 완료 기준

- [ ] Story 3-1 완료
- [ ] `dig thirdtool.dev` 결과 CF distribution IP
- [ ] E2E: 브라우저로 `https://thirdtool.dev` 접속 성공

## [Story 3-1] apex + www A alias

### User Story
- As a 운영자
- I want `thirdtool.dev`와 `www.thirdtool.dev`를 CF distribution에 alias 등록하기를
- so that 사용자가 도메인 접속 시 즉시 SPA 로드된다

### 설명
- Route53 hosted zone `thirdtool.dev`
- A record (apex): alias → CF distribution
- A record (www): alias → CF distribution
- TTL: 300 (초기), 나중에 늘림

### 완료 기준 (AC)
- Given record 등록 / When 전파 / Then `dig thirdtool.dev` CF IP
- Given 사용자 브라우저 / When 접속 / Then SPA 로드
- *(엣지 - DNS 전파)* Given 등록 직후 / When 접속 실패 / Then 최대 5분 대기 (TTL 300)

### 의존성
- 선행: Epic 2
- 후행: Epic 5 (E2E 검증)

---

# [Epic 4] GHA workflow

## Epic 목표

`.github/workflows/deploy-fe.yml` 작성 → workflow_dispatch로 build → S3 sync → CF invalidation 자동. OIDC로 무자격 배포.

## 배경

배포 리드타임 < 5분 목표. Epic 1의 리소스를 대상으로 자동 배포.

## 완료 기준

- [ ] Story 4-1~4-4 완료
- [ ] workflow_dispatch 성공 → 3분 내 완료
- [ ] OIDC role 최소 권한 원칙 준수
- [ ] FE-ADR-CDN-003 (GHA OIDC 권한 최소화) 작성

## [Story 4-1] deploy-fe.yml 초기

### User Story
- As a 운영자
- I want GHA workflow_dispatch로 배포를 트리거하기를
- so that main 커밋 후 명시적으로 안전 배포한다

### 설명
- `.github/workflows/deploy-fe.yml`
- Trigger: workflow_dispatch + push (main, path: src/**)
- Steps: checkout → setup-node → npm ci → npm run build

### 완료 기준 (AC)
- Given workflow_dispatch / When 실행 / Then npm run build 성공
- Given main push / When 조건 매칭 / Then 자동 실행
- *(엣지 - build 실패)* Given 컴파일 에러 / When 실행 / Then step 실패 + notification

### 의존성
- 선행: (없음)
- 후행: Story 4-2

## [Story 4-2] OIDC gha-deploy-role 권한

### User Story
- As a 보안 담당
- I want GHA가 AWS access key 없이 OIDC로만 배포하기를
- so that 크리덴셜 유출 리스크 제거

### 설명
- IAM `gha-deploy-role` — trust: GitHub OIDC provider
- Permissions: `FeSyncS3` (s3:PutObject on thirdtool-fe-prod), `FeCdnInvalidate` (cloudfront:CreateInvalidation)
- Repository 조건: `repo:{owner}/{repo}:ref:refs/heads/main`

### 완료 기준 (AC)
- Given OIDC 설정 / When configure-aws-credentials / Then STS assume role 성공
- Given 다른 브랜치에서 실행 / When STS / Then AccessDenied
- *(엣지 - fork PR)* Given fork PR / When workflow / Then STS 실패 (보안)

### 의존성
- 선행: Story 4-1
- 후행: Story 4-3

## [Story 4-3] cache-control 분리 sync

### User Story
- As a 운영자
- I want assets와 index.html을 각각 다른 Cache-Control로 sync하기를
- so that 새 배포가 즉시 반영되면서도 CDN 효율이 높다

### 설명
- Step 1: `aws s3 sync dist/ s3://... --delete --exclude "index.html" --cache-control "public,max-age=31536000,immutable"`
- Step 2: `aws s3 cp dist/index.html s3://.../index.html --cache-control "no-cache,no-store,must-revalidate"`

### 완료 기준 (AC)
- Given sync 실행 / When S3 확인 / Then asset 파일 immutable, index.html no-cache
- Given 배포 후 브라우저 응답 / When Cache-Control 확인 / Then 정합
- *(엣지 - 새 hash asset)* Given 새 hash / When sync / Then 이전 asset은 --delete로 제거

### 의존성
- 선행: Story 4-2
- 후행: Story 4-4

## [Story 4-4] invalidation step

### User Story
- As a 운영자
- I want 배포 마지막에 CF invalidation을 자동 실행하기를
- so that 사용자가 즉시 새 버전을 받는다

### 설명
- `aws cloudfront create-invalidation --distribution-id $CF_DIST_ID --paths "/*"`
- CF_DIST_ID는 GHA secret으로 저장
- invalidation 완료 대기 (선택)

### 완료 기준 (AC)
- Given invalidation 실행 / When 30초 후 / Then CF 캐시 무효화
- Given 새 요청 / When 응답 / Then 새 index.html + 새 asset hash
- *(엣지 - 월간 한도)* Given 월 30건 초과 / When 실행 / Then 유료 카운트 시작 (성공은 유지)

### 의존성
- 선행: Story 4-3
- 후행: (없음)

---

# [Epic 5] 환경변수 + 검증

## Epic 목표

`.env.production` / `.env.development` 파일 정합 → 소셜 로그인 콜백 · CORS · SameSite 프로덕션 검증.

## 배경

인프라 완성 후 실제 사용자 흐름 E2E 검증. 이 Epic 완료가 "완전 동작" 상태 확립.

## 완료 기준

- [ ] Story 5-1, 5-2, 5-3 완료
- [ ] `thirdtool.dev`로 로그인 성공 E2E
- [ ] `VITE_API_BASE_URL` 하드코딩 grep = 0건
- [ ] FE-ADR-CDN-002 (env 파일 정책) 작성

## [Story 5-1] .env.production / .env.development

### User Story
- As a FE 개발자
- I want 환경별 API base URL을 파일로 관리하기를
- so that 배포 시 별도 수정 없이 자동 반영된다

### 설명
- `.env.production`: `VITE_API_BASE_URL=https://api.thirdtool.dev`
- `.env.development`: `VITE_API_BASE_URL=http://localhost:8080`
- `.env.local`: gitignore (개인 override)
- `.env.example`: 커밋 (문서 역할)

### 완료 기준 (AC)
- Given `npm run build` / When Vite / Then `.env.production` 자동 로드
- Given `npm run dev` / When Vite / Then `.env.development` 자동 로드
- *(엣지 - 문자열 하드코딩)* Given 소스에 `api.thirdtool.dev` 직접 기재 / When grep / Then 0건

### 의존성
- 선행: (없음)
- 후행: Story 5-2

## [Story 5-2] E2E: 소셜 로그인 콜백

### User Story
- As a QA
- I want 프로덕션 도메인에서 카카오/네이버 로그인이 정상 동작하기를
- so that 배포된 서비스가 완전 동작함을 검증한다

### 설명
- Playwright E2E — `https://thirdtool.dev/login` → 카카오 승인 → 콜백 → 세션 진입
- 각 소셜 provider별로 검증
- 실패 시 CORS·SameSite·API base URL 원인 분석

### 완료 기준 (AC)
- Given 카카오 로그인 / When 흐름 완료 / Then `/home` or `/onboarding` 진입
- Given 네이버 로그인 / When 흐름 완료 / Then 동일 결과
- *(엣지 - state 미스매치)* Given CSRF state 실패 / When 콜백 / Then 인라인 에러

### 의존성
- 선행: Story 5-1
- 후행: Story 5-3

## [Story 5-3] CORS + SameSite 검증

### User Story
- As a 보안 담당
- I want 프로덕션에서 SameSite=None + Secure 쿠키가 정상 전송되기를
- so that 크로스 서브도메인 세션이 유지된다

### 설명
- 백엔드 allowedOrigins에 `https://thirdtool.dev`, `https://www.thirdtool.dev` 포함 확인
- Set-Cookie에 SameSite=None; Secure; Path=/; Domain=.thirdtool.dev
- FE fetch 시 `credentials: 'include'` 확인

### 완료 기준 (AC)
- Given 로그인 후 쿠키 / When 브라우저 devtools / Then SameSite=None; Secure
- Given 크로스 서브도메인 요청 / When XHR / Then 쿠키 전송 성공
- *(엣지 - Safari third-party cookie)* Given Safari private mode / When 요청 / Then 알려진 제약 문서화

### 의존성
- 선행: Story 5-2
- 후행: (없음)
