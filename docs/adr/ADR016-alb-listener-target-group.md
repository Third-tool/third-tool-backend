# ADR016: ALB 1개 공유 + Listener 80→443 redirect + Target Group type=ip + TLS 1.3 정책 (Story-049)

- **상태**: Accepted
- **날짜**: 2026-06-29
- **관련**: Product 6 AWS 네트워크 Epic 2 (ALB + 도메인 + TLS) · milestone 0.0.1v item #13 (`workflow/task/milestones/version/0.0.1v/milestone.md`) · `docs/operations/troubleshooting/ts010-alb-setup.md` · `infra/alb/alb-spec.json` · `infra/alb/target-group-spec.json`

## 컨텍스트

Story-048 VPC 셋업 + Story-047 ECS Task Definition·Service 정의 후 다음 의존은 **인터넷 진입점 ALB**. Story-047의 `service-prod.json`/`service-staging.json`이 `<PROD_TARGET_GROUP_ARN>`/`<STAGING_TARGET_GROUP_ARN>` placeholder로 본 Story 산출물을 기다리고 있다.

application-prod.yml은 `jwt.cookie.secure: true`로 HTTPS 강제. ALB가 HTTPS termination 수행해야 브라우저가 쿠키 수락. 또한 Fargate는 awsvpc 네트워크 모드 — ENI 단위 IP 할당이라 ALB Target은 `target-type=ip`만 가능 (instance 등록 불가).

추가 결정 영역:
- ALB를 prod/staging 환경별 2개 분리 vs 1개 공유 + host header 라우팅
- TLS 1.2-2021 vs 1.3 (구형 브라우저 호환 vs 보안)
- HTTP 80을 그대로 forward하지 않고 HTTPS 443으로 강제 redirect

## 결정

### ALB 1개 공유 + host header 라우팅

ALB 1개(`thirdtool-alb`)에 prod·staging Target Group 2개 등록. Listener 443의 default action은 prod TG forward, priority 10 rule이 `host-header: staging.thirdstool.com`을 staging TG로 분기.

| 항목 | 결정 | 근거 |
| --- | --- | --- |
| ALB 개수 | 1개 공유 | 시간 비용 절반 절감 ($0.0225/시간 × 1 vs 2). M1 트래픽 0명에 ALB 2개 운영은 과도 |
| 분기 방식 | host header (`api.thirdstool.com` vs `staging.thirdstool.com`) | path-based보다 환경 격리 명확. DNS 단에서 분기 |
| scheme | internet-facing | 사용자 진입 |
| subnets | public-2a + public-2c | multi-AZ — ALB 자체는 AZ 장애 시 다른 AZ로 자동 fail-over |
| security group | alb-sg (80/443 ingress from 0.0.0.0/0) | ts009 §7.1 정합 |

### Listener 80 → 443 HTTPS Redirect (HTTP 직접 forward 거부)

Listener 80은 default action으로 `redirect 301 HTTPS:443` (host/path/query 보존). HTTP를 ECS로 forward하지 않음.

**근거**:
- `application-prod.yml` `jwt.cookie.secure: true` → HTTPS 없으면 쿠키 동작 불가
- 사용자가 `http://` 입력해도 자동 HTTPS 승격
- HSTS 정책 추후 적용 시 1단 게이트로 단일화

### Target Group `target-type=ip` 강제

Fargate awsvpc 네트워크 모드는 Task당 ENI에 IP 할당. instance 단위 등록 불가 — `target-type=ip` 외 선택지 없음. ECS Service의 `loadBalancers` config가 자동 register/deregister 수행.

healthCheck `path=/health, interval=30s, timeout=5s, threshold 2/2, matcher 200` — Story-047 Task Definition healthCheck command(`wget /health`)와 정합.

`deregistration_delay.timeout_seconds: 30` (default 300 → 30 단축): ECS rolling update 시 기존 Task drain 30s 후 종료 → 다운타임 단축. ECS Task graceful shutdown 30s 가정 (Spring Boot default).

### TLS 1.3 정책 (`ELBSecurityPolicy-TLS13-1-2-2021-06`)

TLS 1.2 + 1.3 모두 지원, 약한 cipher 제거. portfolio 프로젝트라 구형 브라우저 호환 요구 낮음 — 보안 우선.

### ALB attributes 4종

- `routing.http.drop_invalid_header_fields.enabled: true` — RFC 7230 위반 헤더 차단 (HTTP Smuggling 방지)
- `routing.http.preserve_host_header.enabled: true` — ECS Task가 정확한 Host 헤더 수신 (Spring redirect URI 생성 시 정합)
- `routing.http.xff_client_port.enabled: true` — `X-Forwarded-For` + port 정보 전달 (감사 추적)
- `idle_timeout.timeout_seconds: 60` — default 그대로

### 본 ADR이 다루지 않는 범위

- **WAF**: AWS WAF web ACL (Rate-based + SQLi/XSS) 별도 Story (M2 보안 강화)
- **Custom error pages**: 503/504 default 페이지 — M2 UX 개선 필요 시
- **ALB access logs S3**: 트래픽 감사 — 보안 사고 추적 시 활성 (cost trade-off)
- **HTTP/2 enable_default** vs HTTP/3 (QUIC) — 본 ADR은 HTTP/2 (default), HTTP/3은 M3
- **Sticky sessions**: JWT stateless라 불요. 세션 기반 전환 시 검토
- **Mutual TLS (mTLS)**: API 클라이언트 인증 강화 — M2

## 결과 (Consequences)

### 긍정적

- **시간 비용 절감**: ALB 2개 대비 1개 — $0.54/일 절감 (M1 cost.md baseline $0.80 + LCU 별도)
- **HTTPS 강제**: `jwt.cookie.secure: true` 정합. 사용자가 HTTP 입력해도 자동 승격
- **Fargate 정합**: target-type=ip 강제. ECS Service `loadBalancers` config가 awsvpc IP 자동 등록
- **보안 baseline**: TLS 1.3 + drop_invalid_header_fields + xff_client_port
- **rolling update 다운타임 단축**: deregistration_delay 30s (default 300 → 90% 단축)
- **host header 라우팅으로 환경 격리**: DNS 단에서 staging vs prod 분리. URL이 곧 환경

### 트레이드오프 / 부정적

- **단일 ALB 장애점**: AWS ALB 자체는 multi-AZ 자동 — ap-northeast-2 region 전체 장애 시에만 영향. portfolio 프로젝트 가용성 요구 낮음 → 수용
- **staging LCU가 prod와 동일 ALB에서 경합**: staging 부하 테스트가 prod 응답 시간 영향 가능. M2 트래픽 발생 시 ALB 2개 분리 검토
- **구형 브라우저 호환 손실**: TLS 1.3 정책 → IE 11 미지원. portfolio 프로젝트라 영향 없음
- **`host-header` 변경 시 routing rule 재배포 필요**: 추가 환경(beta/dev) 신설 시 ALB rule 추가 — Terraform 자동화 미시점이라 수동 작업
- **ACM 인증서 발급 지연 risk**: DNS 검증 NS 전파가 최대 72시간 — 운영 시점에 미리 발급해두지 않으면 배포 차단
- **HTTPS 강제로 인한 ALB → ECS 측 HTTP에서 `X-Forwarded-Proto` 의존**: Spring Boot가 `server.forward-headers-strategy: native` 명시 안 하면 self-referential URL이 `http://`로 생성될 위험 (ts010-3·ts010-4 트러블슈팅 등록)

## 대안 비교

| 대안 | 장점 | 거부 사유 |
| --- | --- | --- |
| **A. ALB 1개 공유 + host header (선택)** | 비용 절감, 환경 격리 DNS 단 | staging이 prod LCU 경합, M2 ALB 분리 시 마이그레이션 |
| **B. ALB 2개 분리 (prod·staging 각각)** | 환경 완전 격리, LCU 독립 | 시간 비용 2배, M1 트래픽 0명 단계에서 과도 |
| **C. NLB (Network LB)** | 더 낮은 latency, raw TCP | HTTP/HTTPS 처리 불가 (path/host 라우팅 불가), HTTPS termination도 ECS에 부담 |
| **D. API Gateway** | rate limiting + WAF 통합 | 시간 비용 + 요청당 비용 증가, Lambda 외 ECS 사용 시 ALB 대비 가치 작음 |
| **E. target-type=instance** | (선택지 자체 부재) | Fargate awsvpc에서 등록 불가 — AWS 아키텍처 제약 |
| **F. HTTP 80 forward to ECS (no redirect)** | 인증서 미보유 시 즉시 운영 가능 | `jwt.cookie.secure: true` 동작 불가, 보안 baseline 미달, HSTS 적용 불가 |
| **G. CloudFront + ALB origin** | edge 캐시, WAF, DDoS 보호 | M1 단계 비용·복잡도 증가. 정적 자산 없어 캐시 가치 낮음 — M3 검토 |

## 알려진 follow-up (본 ADR 범위 외)

- **Story-TBD(ECS Service 치환)**: Story-047 `service-{prod,staging}.json`의 `<*_TARGET_GROUP_ARN>` 치환 + ECS Service 생성 시 Target Group 자동 등록
- **Story-TBD(WAF)**: AWS WAF web ACL — Rate-based + SQLi/XSS/Common rules. M2 보안 강화
- **Story-TBD(Route 53 IaC)**: Terraform 모듈로 alias record 자동 관리. M2 Product 7 Epic 2
- **Story-TBD(Spring Forward Headers)**: `application-prod.yml`에 `server.forward-headers-strategy: native` 추가 — ALB X-Forwarded-Proto 정확 인식. 본 ADR 결정의 application 측 정합. ts010-3·ts010-4 해결책 코드화
- **Story-TBD(HSTS)**: Spring Security `httpStrictTransportSecurity` 활성 — HTTPS 강제 강화
- **Story-TBD(ALB 2개 분리)**: M2 staging 트래픽 발생 시 환경별 ALB 분리
- **Story-TBD(WAF + Shield)**: DDoS 대비 — 트래픽 발생 후 검토
- **Story-TBD(ALB access logs S3)**: 보안 감사 — S3 비용 trade-off
- **Story-TBD(Mutual TLS)**: B2B API 클라이언트 인증 — 향후 외부 통합 진입 시

## 다시 검토할 시점

- **staging 트래픽이 prod LCU에 영향 미치는 시점**: ALB 2개 분리 (대안 B로 전환)
- **portfolio → production 전환 시점**: WAF + Shield + CloudFront 도입
- **구형 브라우저 사용자 확인 시점**: TLS 정책 완화 (1.2 + 1.3)
- **API Gateway 가치 임계 도달 시점**: rate limiting을 ALB rule보다 정밀하게 제어 필요 시
- **HTTP/3 (QUIC) 지원 확정 시점**: AWS ALB HTTP/3 지원되면 latency 개선 검토
