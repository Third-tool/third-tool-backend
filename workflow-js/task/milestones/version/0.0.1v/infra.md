# 0.0.1v / Infra 진척

> M1 종료 시점에 dev 환경이 어디까지 깔렸는가의 산출물 기록.
> 본 파일은 본주 진행 중 매일 또는 D6 종료 시점에 채운다.

---

## 목표 환경 토폴로지 (M1 종료 후 ↓ 이 모양)

```
Internet
   │
   ▼
[Route53 (M2)] ──┐
                 │
[ALB internet-facing]  ← Product 6 Story 1-2
   │  HTTPS (ACM은 M2 — M1은 HTTP 허용)
   ▼
[Target Group type=ip]
   │
   ▼
[ECS Fargate Service]  ← Product 5 Story 2-2
   │
   ├── Task 1 (Spring Boot, JDK 21)
   │     ↓ Secrets Manager 환경변수 주입
   │     [Spring AI: provider=static]
   │
   ▼
[VPC 10.0.0.0/16, 2 AZ]  ← Product 6 Story 1-1
   ├── public subnet (ALB)
   └── private subnet (Task, RDS)
        │
        ▼
   [RDS MySQL single-AZ]  ← Product 6 Story 2-1
        Multi-AZ는 M2

[Secrets Manager]  ← Product 7 Story 1-1
   ├── db-credentials
   ├── jwt-secret
   ├── oauth-kakao
   ├── oauth-naver
   └── gemini-api-key (LLM은 M2부터 사용)

[ECR repository]  ← Product 5 Story 1-1
   └── thirdtool-backend:{git_sha}

[Prometheus + Grafana]  ← Product 0-b Story 2-1
   별도 docker-compose 또는 EC2 (Fargate 외부)
   ↓ scrape
[https://{dev domain}/actuator/prometheus]
```

---

## 본주 완료 체크리스트

### Product 5 (ECS Fargate)
- [ ] **1-1** 단일 Dockerfile 통합 (multi-stage / JDK 21) — 이미지 크기 측정값: `_____ MB`
- [ ] **2-1** GHA OIDC AssumeRole — IAM Role ARN: `_____`
- [ ] **2-2** ECS Task Definition + Service (dev) — Task Def revision: `_____`
- [ ] (Want) 1-2 git_sha 이미지 태깅 — 첫 태그: `_____`
- [ ] (Want) 3-1 ALB target health check path: `/health`

### Product 6 (AWS 네트워크)
- [ ] **1-1** VPC 10.0.0.0/16 + 2 AZ + public·private subnet — VPC ID: `_____`
- [ ] **1-2** ALB internet-facing + Target Group type=ip — ALB DNS: `_____`
- [ ] **2-1** RDS MySQL prod single-AZ — 엔드포인트: `_____` / 인스턴스 클래스: `db._____` / 스토리지: `__ GB`

### Product 7 (Secrets·Terraform — Epic 1만)
- [x] **1-1** Secrets Manager 5종 비밀 spec — Story-052 코드 머지 완료 (2026-06-30). 사용자 AWS 등록은 ts012 절차 (handoff §7 Stage 5)
  - [ ] `thirdtool/dev/db-credential` (HOST·PORT·NAME·USERNAME·PASSWORD) — 사용자 등록 대기
  - [ ] `thirdtool/dev/jwt-secret` (SECRET_KEY) — 사용자 등록 대기
  - [ ] `thirdtool/dev/oauth-kakao` (CLIENT_ID·CLIENT_SECRET) — 사용자 등록 대기
  - [ ] `thirdtool/dev/oauth-naver` (CLIENT_ID·CLIENT_SECRET) — 사용자 등록 대기
  - [ ] `thirdtool/dev/gemini-api-key` (API_KEY, M1은 placeholder OK) — 사용자 등록 대기
- [ ] (Want) **1-2** Spring Boot 부팅 시 Secrets 일괄 로딩 (spring-cloud-aws-secrets-manager) — Story-053 (PR-B) 대기. 검증 방식: `_____`

### Product 0-b (메트릭)
- [ ] **1-1** Actuator `/actuator/prometheus` 노출 + 화이트리스트 — 노출 endpoint: `_____`
- [ ] **2-1** Prometheus + Grafana docker-compose + 영속 볼륨 — 호스팅 위치: `_____` (EC2 / 로컬 / 별도 환경)
- [ ] (Want) **3-1** Grafana 4섹션 대시보드 — 대시보드 UID: `_____`

---

## 첫 배포 시도 결과

> D5 또는 D6에 첫 배포 시도 결과 기록.

| 항목 | 값 | 비고 |
| --- | --- | --- |
| Task RUNNING 도달 시점 | _____ | 부팅 로그 |
| ALB target healthy 도달 | _____ | health check 횟수 |
| `https://{dev domain}/health` 200 OK | _____ | curl 명령 결과 |
| 첫 로그인 시도 (`POST /login`) | _____ | AT 쿠키 + RT 바디 |
| `/actuator/prometheus` 200 + 메트릭 1건 | _____ | curl 결과 |
| 첫 실패·디버깅 사이클 횟수 | _____ | 회고 입력 |

---

## 인프라 산출물 링크 (M1 종료 후)

- AWS Console에서 본 환경의 리소스 ID 모음 (별도 secrets repo 또는 운영자만 접근)
- Dockerfile 위치: `Dockerfile` (단일 통합 후)
- ECR 이미지 URI: `_____`
- GitHub Actions workflow: `.github/workflows/_____.yml`
- Terraform 모듈 (M2 진입 후): `infra/terraform/`

---

## Tier 1-확장 · 1차 배포 완성 (Story-056, 2026-06-30)

- [x] **D8** BE CORS + OAuth URI → `thirdtool.dev` 전환 (SecurityConfig + MvcConfig + application-prod.yml) — Story-056 코드 머지
- [x] **D9** GHA FE 배포 워크플로우 — `.github/workflows/deploy-fe.yml` + `infra/iam/gha-deploy-role-permissions-policy.json` 갱신
- [x] **D2** ACM 인증서 spec — `infra/acm/{ap-northeast-2,us-east-1}.json` (사용자 액션 절차: handoff §14B)
- [x] **D3** ALB HTTPS 전환 spec — `infra/alb/{alb-https-listener,alb-http-to-https-redirect}.json` + `infra/vpc/security-group-alb-443.json` (사용자 액션: handoff §14C)
- [x] **D4** Route53 api.thirdtool.dev → ALB spec — `infra/route53/route53-api-record.json` (사용자 액션: handoff §14E.1)
- [x] **D5** S3 FE 버킷 spec — `infra/s3/s3-fe-bucket{,-policy,-public-access-block}.json` (사용자 액션: handoff §14D.1)
- [x] **D6** CloudFront 배포 spec — `infra/cloudfront/{cloudfront-dist,cloudfront-oac}.json` + README (사용자 액션: handoff §14D.2)
- [x] **D7** Route53 thirdtool.dev/www → CloudFront spec — `infra/route53/route53-fe-record.json` (사용자 액션: handoff §14E.2)
- [ ] **D1** Route53 도메인 등록 — 사용자 직접 (handoff §14A)
- [ ] **D10** FE `.env.production` 갱신 — FE 레포 사용자 직접

### 도메인 신호 (M1 1차 배포 완성 종료선)

- [ ] `curl -i https://api.thirdtool.dev/actuator/health` → 200
- [ ] `curl -I http://api.thirdtool.dev` → 301 → https
- [ ] `curl https://thirdtool.dev` → HTML 응답 (FE index.html)
- [ ] FE → API E2E (CORS OK, SameSite=Strict 쿠키 전달)

산출물 참조: `docs/adr/ADR020-https-fronting-and-fe-cdn-strategy.md` · `infra/DEPLOY-RUNBOOK.md` · `workflow/task/pes/handoff/aws-setup-0.0.1v.md` §14A~§14E

---

## M2 진입 시 인프라 보완 후보

- ACM 와일드카드 인증서 + HTTPS 강제 (Product 6 Story 1-4)
- Route53 hosted zone + subdomain (Product 6 Story 1-3)
- RDS Multi-AZ 전환 (Product 6 Story 2-1 확장)
- 보안 그룹 5종 분리 (Product 6 Story 2-2)
- ECR 라이프사이클 정책 + enhanced scan (Product 5 Story 1-3)
- Rolling Update + Circuit Breaker + 자동 롤백 (Product 5 Story 2-3·3-2·3-3)
- Terraform 모듈화 + State 백엔드 (Product 7 Epic 2 전체)
- CloudWatch Alarm 4종 + SNS (Product 7 Story 3-3)
