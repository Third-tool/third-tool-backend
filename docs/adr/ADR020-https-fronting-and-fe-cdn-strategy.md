# ADR020: HTTPS fronting + FE CDN 전략 — 1 ALB 공유 + CloudFront/S3 OAC + .dev HSTS 강제 (Story-056)

- **상태**: Accepted
- **날짜**: 2026-06-30
- **관련**: milestone 0.0.1v Tier 1-확장 (D2~D9) (`workflow/task/milestones/version/0.0.1v/milestone.md`) · `infra/acm/` · `infra/alb/alb-https-listener.json` · `infra/alb/alb-http-to-https-redirect.json` · `infra/cloudfront/` · `infra/s3/s3-fe-*.json` · `infra/route53/route53-{api,fe}-record.json` · `infra/vpc/security-group-alb-443.json` · `infra/DEPLOY-RUNBOOK.md` · `.github/workflows/deploy-fe.yml` · ADR013(OIDC) · ADR016(ALB) · ADR018(RDS)

## 컨텍스트

Story-049(ALB) + Story-050(Spring forward-headers) + Story-051(RDS) 완료 후, M1 종료 신호 8건은 "코드 머지 + dev 환경 떠 있는 상태"까지를 합격선으로 두었다. **본주 마무리에 사용자가 1차 배포를 도메인 단위(`https://api.thirdtool.dev` + `https://thirdtool.dev`)까지 확장하기로 결정** — milestone.md "스코프 변경 메모"에 따라 Tier 1-확장 D1~D10 신설.

핵심 결정 영역:
- **도메인 등록 위치**: Route53 vs 외부 registrar (Namecheap/GoDaddy 등)
- **TLD 선택**: `.dev`(HSTS 강제) vs `.com`/`.io`
- **ALB 분할**: API + FE 동일 ALB vs 분리 ALB
- **FE 정적 호스팅**: CloudFront + S3 vs ECS Task에서 정적 자산 서빙 vs Vercel/Netlify
- **S3 접근 제어**: OAC (Origin Access Control) vs OAI(legacy) vs public bucket
- **HTTPS 강제**: HSTS 헤더 vs ALB redirect만 vs 둘 다
- **us-east-1 ACM 필수성** (CloudFront): 자체 요구사항 회피 가능 여부
- **CORS allowed origins**: 단일 도메인 vs subdomain 다중

## 결정

### 도메인: Route53 + `.dev` TLD

| 항목 | 결정 | 근거 |
| --- | --- | --- |
| Registrar | **Route53** | Hosted Zone 자동 생성 + ACM DNS 검증 CNAME 자동 추가 + IAM 일관 권한. 외부 registrar는 NS delegation 1단계 추가 + 비용 차이 ~$1/년 미만 |
| TLD | **`.dev`** | Google이 관리 + 모든 `.dev` 도메인은 **HSTS preload 강제** — HTTP 접속 시 브라우저가 차단(서버 redirect 의존성 0). 보안 baseline 자동. `.com` 대비 차별화 + 개발자 친화 |
| 비용 | $12/년 + Hosted Zone $0.5/월 | 1년 baseline ~$18. 갱신 시 cost.md에 추가 |
| Subdomain 구조 | `api.thirdtool.dev` (BE) + `thirdtool.dev`(apex) / `www.thirdtool.dev` (FE) | API/FE 분리 — CloudFront(FE) vs ALB(BE) origin 격리 + CORS 명확화 |

### ALB 공유 + Listener 443 신설

| 항목 | 결정 | 근거 |
| --- | --- | --- |
| ALB 분할 | **API/FE 동일 ALB 사용 안 함 → API는 ALB, FE는 CloudFront** | FE는 정적 자산 → ALB origin은 부적합 (TLS 종료 + bandwidth 비용). CloudFront edge cache + S3가 표준. ALB는 API 전용 |
| HTTPS 리스너 | **443 신설 + TLS 1.3 정책 (`ELBSecurityPolicy-TLS13-1-2-2021-06`)** | ADR016에서 ALB spec 이미 설계됨. 본 ADR은 실 ACM cert(ap-northeast-2)를 attach + 443 listener 활성화 |
| HTTP 80 처리 | **301 redirect → HTTPS** (보존 host/path/query) | `.dev` HSTS 강제이지만 첫 1회 HTTP 시도는 발생 가능 → 서버 측 redirect로 graceful fallback |
| ALB SG 443 인바운드 | **별도 SG rule 추가** (`security-group-alb-443.json`) | 기존 ALB SG가 80만 열려 있음. 443 인바운드 0.0.0.0/0 신설 |

### FE CDN: CloudFront + S3 + OAC

| 항목 | 결정 | 근거 |
| --- | --- | --- |
| 정적 호스팅 | **CloudFront + S3** (Vercel/Netlify 거부) | AWS 단일 cloud 비용 통합 + IAM 통합 + 외부 SaaS 종속 회피. SPA Vite build → S3 sync → CloudFront invalidation 패턴 표준 |
| S3 접근 제어 | **OAC (Origin Access Control)** | OAI는 legacy + KMS·SSE-S3 미지원 한계. OAC가 2022+ 권장. S3 bucket은 퍼블릭 접근 완전 차단 + CloudFront principal만 GetObject 허용 |
| SPA 404 처리 | **CloudFront error responses 404/403 → 200 `/index.html`** | SPA 라우팅은 client-side. S3는 path가 파일 매칭 안 되면 404 — `/dashboard` URL 직접 접근 시 fallback 필수 |
| ACM cert region | **us-east-1 필수** | CloudFront는 글로벌 service — ACM cert가 us-east-1에 있어야만 attach 가능. ap-northeast-2 cert는 ALB 전용 |
| TLS 정책 | **TLS 1.2+ (`TLSv1.2_2021`)** | CloudFront 권장 default. TLS 1.0/1.1 차단 + 구형 브라우저 호환 OK |
| Price class | `PriceClass_200` | 아시아·북미·유럽 edge — 한국 + 글로벌 사용자 커버. PriceClass_All은 남미·아프리카까지 — 비용 1.5배 |
| Cache TTL | default 1일 / max 1년 | SPA index.html은 invalidation으로 갱신, 정적 asset(JS/CSS)은 build hash로 immutable |

### Route53 Alias 레코드 3건

```
A   thirdtool.dev          → CloudFront Distribution (alias)
A   www.thirdtool.dev      → CloudFront Distribution (alias)
A   api.thirdtool.dev      → ALB internal-facing (alias)
```

A alias는 IPv4. AAAA(IPv6)는 CloudFront/ALB 모두 자동 활성 — 별도 레코드 불요.

### CORS allowed origins 3건

`SecurityConfig.corsConfigurationSource()` + `MvcConfig.addCorsMappings()` 모두 다음 명시:
- `https://thirdtool.dev` (apex)
- `https://www.thirdtool.dev` (CloudFront alt)
- `http://localhost:5173` · `http://localhost:8080` · `http://localhost:3000` (local dev 유지)

`api.thirdtool.dev`는 API 자신이라 CORS allowed origins 불요 (preflight 제외).
`thirdstool.com` (구 도메인) 완전 제거 — clean break.

### OAuth Redirect URI

`application-prod.yml`:
- Kakao: `https://api.thirdtool.dev/oauth/kakao/callback`
- Naver: `https://api.thirdtool.dev/oauth/naver/callback`

운영자가 카카오·네이버 developers 콘솔에서 등록 도메인을 `api.thirdtool.dev`로 갱신 필수 — ts006 §2.1·2.2.

### FE 배포 GHA 워크플로우

`deploy-fe.yml`:
- OIDC AssumeRole `gha-deploy-role` (ADR013) — long-lived key 없음
- npm build → `dist/` 산출
- `aws s3 sync dist/ s3://thirdtool-fe-prod/` 
- `aws cloudfront create-invalidation --paths '/index.html' '/assets/*'`

trigger: FE repo push to main. **본 ADR은 BE 레포지토리 — FE 레포 측 등록은 사용자 액션**.

### 본 ADR이 다루지 않는 범위

- **CDN 이미지 최적화 (Lambda@Edge / CloudFront Functions)**: 트래픽 발생 후
- **WAF (Web Application Firewall)**: M3 보안 강화 Epic
- **CloudFront 다중 origin (API origin 추가)**: 현재 분리 유지가 명확
- **multi-region failover**: 단일 region M1
- **FE 도메인 추가 (예: `app.thirdtool.dev`)**: 단일 페이지 SPA M1
- **API rate limiting at edge**: WAF 도입 시
- **CloudFront Origin Shield**: 트래픽 발생 후 캐시 hit rate 측정 후

## 결과 (Consequences)

### 긍정적

- **HSTS 강제로 다운그레이드 공격 baseline 차단**: `.dev`는 브라우저 HSTS preload 목록 — 사용자가 `http://thirdtool.dev` 입력해도 브라우저가 HTTPS로 자동 전환. 서버 redirect 의존 0
- **CloudFront edge로 FE 응답 지연 단축**: 한국 사용자 ms 단위. ALB 직접 origin 대비 5-10배
- **S3 퍼블릭 차단 + OAC로 외부 직접 접근 0**: bucket URL 노출되어도 403. CloudFront가 유일 경로
- **CORS clean break**: `thirdstool.com` 완전 제거 — 구 도메인 cookie/session 잔존 risk 0
- **FE/BE GHA 분리**: FE 변경이 BE 배포 트리거하지 않음 + 역도 동일. 독립 deploy
- **OAuth redirect URI 단일 출처**: api.thirdtool.dev 1곳 — 카카오·네이버 등록 도메인 단순화

### 트레이드오프 / 부정적

- **`.dev` HSTS 강제로 HTTP 서비스 불가**: HTTPS 미준비 상태에서 HTTP로 서비스할 수 없음 — ACM ISSUED 전까진 도메인 접근 불가. M1 첫 배포 전 ACM 발급 완료 필수
- **us-east-1 ACM 의존**: CloudFront 자체 요구사항. 한국 region에 모든 자원 두려는 계획에 예외 1건 — 단, ACM은 비용 0
- **CloudFront propagation 5~10분**: 첫 배포 후 즉시 도메인 접속 시 캐시 미스. 운영자가 이 점 인지 + 첫 invalidation 명시 필요
- **CloudFront 비용**: PriceClass_200 + outbound 데이터 $0.085/GB. M1 트래픽 0 → 0.1~1 GB/월 ≈ $0.01~0.10. M2 트래픽 증가 시 monitoring 필요
- **S3 storage + invalidation 비용**: storage $0.023/GB·월 (dist/ ~10 MB → $0). invalidation 첫 1000개 path 무료 후 $0.005/path — M1 무료 구간
- **Route53 Hosted Zone $0.5/월**: domain 등록 즉시 발생 — cost.md 추가
- **OAuth provider 콘솔 갱신 필요**: 카카오·네이버 developers에서 redirect URI 등록 도메인을 `api.thirdtool.dev`로 변경 — 사용자 액션. 누락 시 OAuth callback 실패
- **단일 ALB로 staging + prod 혼재 (ADR016)**: API trafic spike 시 ALB 단일 SPOF. 분리 ALB는 M2 환경 분리 Epic
- **CloudFront cache key 기본 = URL만**: 쿠키/헤더 vary 미설정 — SPA에 적정. API origin이 아니라 정적 호스팅이므로 OK
- **첫 배포 검증 부담**: ACM 2개 + ALB 443 + Route53 3건 + S3 + CloudFront + FE deploy + OAuth provider 갱신 — 7단계 순차 의존. handoff §11~14 + DEPLOY-RUNBOOK.md로 절차 명시

## 대안 비교

| 대안 | 장점 | 거부 사유 |
| --- | --- | --- |
| **A. `.com` TLD + HSTS 헤더 수동 설정** | 일반 도메인 익숙 | HSTS preload 등록 절차 + 응답 헤더 누락 시 다운그레이드 노출. `.dev`가 baseline 강제 |
| **B. Vercel/Netlify FE 호스팅** | zero-config + git 통합 | AWS 단일 cloud 비용 통합 깨짐. SaaS 종속 + 가격 인상 risk |
| **C. ECS Task에서 정적 자산 + API 동시 서빙 (단일 origin)** | CloudFront/S3 불요 | ALB outbound 부담 + JVM이 정적 파일 서빙 — 비효율. SPA build 시점이 BE deploy와 결합 |
| **D. OAI (Origin Access Identity, legacy)** | 기존 사례 풍부 | KMS·SSE-S3 미지원 + AWS 2022+ OAC 권장. OAC 채택이 future-proof |
| **E. S3 퍼블릭 bucket + CloudFront 미사용** | 단순 | edge cache 부재 + HTTPS 자동 X (S3 도메인은 https 가능하나 도메인 alias 불가) |
| **F (선택). CloudFront + S3 + OAC + Route53** | edge cache + HTTPS + 도메인 alias + 보안 baseline | trade-off는 위에 명시 |
| **G. ALB HTTPS만 + HTTP 차단 (redirect 없음)** | 명확한 강제 | `.dev` 외 입력 사용자 경험 저하 — 첫 1회 graceful fallback이 UX 우수 |
| **H. api.thirdtool.dev + thirdtool.dev 동일 도메인 (subdomain 없음)** | 단순 | API/FE 분리 명확화 손실 + CORS 복잡화 |
| **I. WAF/CloudFront Functions로 edge 인증** | bot 차단 | M1 보안 baseline 충분. M3 추가 |
| **J. Route53 Health Check + DNS failover** | 다중 region 가용성 | 단일 region M1. M3 multi-region 검토 |

## 알려진 follow-up (본 ADR 범위 외)

- **사용자 액션 U1 (D1)**: Route53 콘솔에서 `thirdtool.dev` 구매 — handoff Stage 11 사전 단계
- **사용자 액션 U3-1 (D2)**: ACM 2-region 신청 + DNS 검증 — handoff Stage 11
- **사용자 액션 U4 (D3)**: ALB HTTPS 리스너 + 443 SG — handoff Stage 12
- **사용자 액션 U5 (D4)**: Route53 `api.thirdtool.dev` alias — handoff Stage 14
- **사용자 액션 U6+U7 (D5+D6)**: S3 버킷 + CloudFront 배포 — handoff Stage 13
- **사용자 액션 U8 (D7)**: Route53 apex/www alias — handoff Stage 14
- **사용자 액션 U9 (D10)**: FE `.env.production` 갱신 + GHA 첫 deploy
- **카카오·네이버 콘솔 OAuth redirect URI 갱신**: 사용자 측. ts006 §2.1·2.2 절차
- **Story-TBD (WAF + CloudFront Functions)**: M3 보안 강화
- **Story-TBD (ECR/CloudFront 비용 monitoring)**: 첫 월 청구 후 cost.md 갱신
- **Story-TBD (FE 도메인 분리 — app.thirdtool.dev)**: 다중 SPA 도입 시

## 다시 검토할 시점

- **CloudFront 비용이 cost.md baseline 대비 50% 초과 시점**: PriceClass 다운그레이드 또는 cache TTL 상향
- **`.dev` HSTS로 인한 사용자 cs 문의 발생 시점**: HTTPS 환경 사용자 교육 — 도메인 입력 시 자동 HTTPS 안내
- **FE 빌드 산출물 크기 > 50 MB 시점**: code splitting + lazy loading 검토
- **API/FE 별도 ALB 필요 시점 (API trafic > 1000 RPS)**: ALB 분리 + DNS round-robin
- **multi-region 진입 시점**: Route53 Health Check + Origin Failover + ACM us-east-1 외 region 추가 검토
- **WAF 도입 시점**: bot/scraping 트래픽 발견 시
