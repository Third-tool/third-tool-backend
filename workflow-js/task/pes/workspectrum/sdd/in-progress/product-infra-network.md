## [Product] AWS 네트워크·도메인·DB 토대 — VPC · ALB · Route53 · ACM · RDS

# [Product] AWS 네트워크·도메인·DB 토대 — VPC · ALB · Route53 · ACM · RDS

## Product Vision

> ThirdTool 전용 VPC를 AWS 계정에 명시 구성하고, 도메인으로 들어온 HTTPS 요청이 ALB를 거쳐 ECS Task에 도달하고 RDS MySQL에 저장되기까지의 외부 진입·데이터 영속 토대를 박는다.
현재 Default VPC + 수동 EC2 + 단일 RDS 추정 구성을 명시 VPC + ALB + Route53 + ACM + Multi-AZ RDS(prod) + single-AZ RDS(staging)로 재정의해, 후속 인프라 Product(컨테이너 배포·운영 자동화)와 부하 테스트 Product의 "스테이징 전용" 전제가 비로소 성립하게 만든다.
관측성·로깅 Product가 이미 운영 중인 트래픽 위에서 의미를 가지려면, 그 트래픽이 거치는 경로 자체가 명확하게 그려져 있어야 한다.
>

## 배경 및 문제

- 현재 상황 (As-Is)
    - AWS 계정에 ECR + EC2 + RDS + S3가 손으로 띄워져 작동 중이지만 **VPC 구성이 명시되지 않음** — Default VPC 사용 추정
    - 트래픽이 EC2 인스턴스의 public IP로 직접 들어옴 — ALB가 앞단에 없어 무중단 배포·헬스체크·HTTPS 종단점이 일관되지 않음
    - 도메인이 EC2 IP에 A 레코드로 직접 연결되었거나 Route53 외부 DNS 사용 — `application-prod.yml`의 OAuth 콜백 URL(`/login/oauth2/code/{provider}`)이 IP/임시 도메인에 의존
    - HTTPS 인증서 발급·갱신이 수동 — Let's Encrypt 갱신 누락 시 인증서 만료 사고 위험
    - RDS가 single-AZ로 추정 — 가용성 한 자리 수 9 수준에 머무름
    - 스테이징 환경이 명시적으로 분리되지 않아 `product-load-test.md` ADR-LOAD-002("프로덕션 금지") 전제가 사실상 깨진 상태
- 발생하는 문제
    - 무중단 배포가 불가능 — EC2 단일 인스턴스에 docker run으로 컨테이너 교체 시 다운타임 발생
    - HTTPS 인증서·도메인 변경이 SSH 접속 + nginx 수동 설정으로 분산 — 단일 진입점 부재
    - RDS 장애 시 수동 스냅샷 복구로 RTO/RPO가 시간 단위 — 토이라도 신고 1건만 받아도 회복 불가
    - 부하 테스트가 프로덕션 외 갈 곳이 없어 측정 자체를 보류하거나 위험 감수 후 강행 — 둘 다 사고 패턴
    - VPC 흐름도가 머릿속에만 존재 — 신규 합류자(혹은 미래의 본인)가 트래픽 경로를 5분에 파악하지 못함
- 왜 지금 해결해야 하는가
    - 후속 Product(컨테이너 배포 = ECS Fargate)의 ALB Target Group이 본 Product의 ALB·VPC 출력에 직접 의존 — 일직선 의존성
    - RDS Multi-AZ로 전환하는 시점이 데이터 누적 전이 가장 싸다. 운영 데이터 100GB가 쌓인 후 전환은 다운타임 + 비용 증가
    - 인증서·도메인 관리를 ACM + Route53 자동 갱신으로 이관하지 않으면 1년 안에 인증서 만료 사고 발생 확률 높음
    - 면접에서 "VPC 어떻게 설계했나, ALB는 왜 썼나, RDS Multi-AZ는 왜 채택했나"는 인프라 단골 질문 3종

## 목표 (To-Be)

- `10.0.0.0/16` ThirdTool 전용 VPC가 ap-northeast-2a/c 2 AZ에 public/private subnet 각 2개로 구성된다
- 인터넷 트래픽이 ALB(internet-facing, public subnet) → ECS Task(private subnet) → RDS(private subnet)로 단방향 흐른다
- `api.thirdtool.dev`의 HTTPS 요청이 Route53 + ACM(ap-northeast-2) + ALB 80→443 리다이렉트로 자동 처리된다. `thirdtool.dev` / `www.thirdtool.dev` → CloudFront(FE CDN)는 **product-fe-cdn.md** 관할
- 보안 그룹 5종(`alb-sg`, `app-sg`, `db-sg`, `bastion-sg`, `vpc-endpoint-sg`)이 최소 권한으로 분리된다
- RDS MySQL 8.0이 prod Multi-AZ(db.t3.medium) + staging single-AZ(db.t3.small)로 분리 운영된다
- 자동 백업 7일 + 파라미터 그룹(timezone=UTC, charset=utf8mb4) + 서브넷 그룹(private subnet only) 표준이 적용된다
- ACM 인증서가 DNS validation으로 자동 갱신된다 (만료 60일 전 알람)
- VPC 흐름도 다이어그램이 `docs/architecture/vpc-topology.md`(또는 README)에 커밋된다

## 설계 결정 (Design Decision)

> **VPC CIDR `10.0.0.0/16` + 2 AZ + single-NAT 전략을 채택한다.**
가용성과 비용의 균형점.
>
> - 2 AZ(ap-northeast-2a/c) — RDS Multi-AZ 전제 + ALB cross-zone 부하 분산의 최소 요건
> - public subnet 2개 (`10.0.0.0/24`, `10.0.1.0/24`) — ALB · NAT Gateway · Bastion 배치
> - private subnet (app) 2개 (`10.0.10.0/24`, `10.0.11.0/24`) — ECS Task 배치
> - private subnet (data) 2개 (`10.0.20.0/24`, `10.0.21.0/24`) — RDS Multi-AZ 배치
> - **NAT Gateway는 single-AZ에 1개만** — Multi-NAT은 월 ~$45 추가, 토이 규모에 과잉. NAT 장애 시 AZ-a→b 임시 전환 절차를 Runbook으로
> - 이 결정은 ADR로 별도 기록한다 (`ADR-INFRA-001: VPC CIDR & AZ Strategy`)

> **ALB Target Type을 `ip`로 고정한다. `instance`는 사용하지 않는다.**
ECS Fargate 호환의 전제.
>
> - Product B(ECS Fargate)의 Task는 ENI 단위로 private IP를 받음 → ALB Target Type=ip가 필수
> - Target Type=instance는 EC2 인스턴스 ID 매핑 — Fargate에서 동작 불가
> - 본 결정이 Product B의 컴퓨트 선택(ECS Fargate)을 잠금 — EC2 회귀 시 Target Type 재설계 필요
> - 이 결정은 ADR로 별도 기록한다 (`ADR-INFRA-002: ALB Target Type=ip`)

> **Route53 hosted zone은 apex 단일 호스팅 영역으로 두고 역할·환경 모두 subdomain으로 분기한다.**
FE/BE 역할 분리 + 환경 라벨 모델.
>
> - `api.thirdtool.dev` → prod ALB (BE API) — **v1 실채택**
> - `thirdtool.dev` / `www.thirdtool.dev` → CloudFront (FE CDN) — **product-fe-cdn.md에서 관리**
> - `staging.thirdtool.dev` → staging ALB (BE staging)
> - apex → ALB 직결 대신 `api.` 서브도메인 분리를 채택한 이유: apex와 www는 FE CDN(CloudFront)이 점유하므로 BE는 `api.` 서브도메인이 자연스러운 분기점. CloudFront는 us-east-1 ACM을 별도 요구하므로 BE(ap-northeast-2)와 인증서 리전이 분리됨 — 리전이 다른 리소스를 같은 레코드가 가리킬 수 없음
> - 별도 호스팅 영역(`staging-thirdtool.dev` 같은 분리 도메인)은 도메인 비용 + NS 위임 복잡도 증가
> - 단점: 계정 격리가 아니므로 staging 실수가 prod에 영향 줄 가능성 잔존 → IAM 권한 경계와 보안 그룹으로 보완. JSON spec 기반 관리로 변경 추적
> - 이 결정은 ADR로 별도 기록한다 (`ADR-INFRA-003: Route53 Hosted Zone Topology`)

> **RDS는 prod Multi-AZ(db.t3.medium) + staging single-AZ(db.t3.small)로 비대칭 운영한다.**
비용과 SLA의 절충.
>
> - prod Multi-AZ: 가용성 99.95%+, 자동 페일오버 ~60초, 월 ~$140
> - staging single-AZ: 99.5%, 다운 시 수동 복구, 월 ~$30
> - 두 환경 모두 Multi-AZ면 월 +$110 비용 — 토이 규모에 과잉
> - 단점: staging이 prod와 정확히 동일하지 않음 → load-test 결과를 prod에 1:1로 외삽 불가. baseline.md에 환경 차이 명시
> - 이 결정은 ADR로 별도 기록한다 (`ADR-INFRA-004: Asymmetric RDS Topology`)

> **ACM 인증서는 DNS validation으로 발급하고 자동 갱신을 활성화한다.**
Email validation은 사용하지 않는다.
>
> - DNS validation은 1회 CNAME 레코드 추가 후 영구 자동 갱신 — 60일 전 ACM이 자동 갱신
> - Email validation은 매년 수동 클릭 필요 → 누락 시 인증서 만료
> - Route53과 동일 계정이라 CNAME 자동 추가 가능 (`aws_acm_certificate_validation`)
> - 갱신 실패 알람 (CloudWatch + SNS)을 Product C와 연동 — 만료 30/14/7일 전 다단 알람
> - 이 결정은 ADR로 별도 기록한다 (`ADR-INFRA-005: ACM Auto-Renewal via DNS Validation`)

## 대안 검토 (Alternatives Considered)

> 큰 갈림길마다 "왜 이것이 아니고 저것인가"를 남긴다. 거부된 안에도 합리적 근거가 있었음을 보임으로써 현재 선택의 트레이드오프를 명확히 한다.

### VPC CIDR 대역 선정

**Option A — `192.168.0.0/16`**
- 장점: RFC1918 사설 대역 중 가장 짧음, 외울 만함
- 거부 이유:
    - 사용자/사무실 가정용 라우터(공유기) 대부분이 `192.168.0.x` / `192.168.1.x` 자동 할당 — 향후 Site-to-Site VPN / Client VPN 도입 시 CIDR 충돌
    - 사내 네트워크가 같은 대역이면 라우팅 모호성 발생

**Option B — `172.16.0.0/12`**
- 장점: AWS 안내 문서 예시 대역, 거의 안 겹침
- 거부 이유:
    - Docker 기본 bridge가 `172.17.0.0/16` 사용 — 로컬 개발 환경 충돌 가능
    - 가독성이 떨어짐 (`172.16~172.31`이 사설 대역인지 즉시 떠오르지 않음)

**Option C — IPv6 / Dual stack**
- 장점: 주소 고갈 영구 회피, 미래 호환
- 거부 이유:
    - ALB · RDS 일부 기능이 IPv6-only에서 제약 (RDS는 dual-stack 지원, RDS Proxy 등 제한)
    - 운영 부담 증가 (ACL · SG가 양쪽 다 관리)
    - 토이 규모에 과잉 — `10.0.0.0/16` 65,536개 IP면 향후 10년 충분

**Option D (선택) — `10.0.0.0/16`**
- 비용: 특별한 비용 없음. 향후 multi-VPC 시 `10.1.0.0/16`, `10.2.0.0/16`으로 순차 확장
- 보상: 익숙한 대역 + Docker 충돌 회피 + 충분한 host bit

### AZ 분산 전략

**Option A — 단일 AZ**
- 장점: NAT Gateway 1개 + cross-AZ data transfer 비용 0
- 거부 이유:
    - RDS Multi-AZ가 불가능 — Multi-AZ는 2 AZ 이상 서브넷 그룹 필수
    - AZ 전체 장애 시 서비스 중단 — 면접 단골 질문에 답할 근거 없음

**Option B — 3+ AZ**
- 장점: 가장 높은 가용성, AZ 2개 동시 장애에도 견딤
- 거부 이유:
    - subnet 9개(3 layer × 3 AZ) 관리 부담 + 비용 증가
    - 토이 규모에 비해 SLA 목표가 과도 (3개 AZ 동시 장애는 ap-northeast-2 역사상 발생 사례 적음)

**Option C (선택) — 2 AZ (ap-northeast-2a, 2c)**
- 비용: cross-AZ data transfer (GB당 $0.01) — 미미
- 보상: RDS Multi-AZ 최소 요건 충족 + ALB cross-zone 부하 분산 + 비용 균형

### NAT 전략

**Option A — Multi-NAT (2 AZ 각 1개)**
- 장점: NAT 1개 장애 시 다른 AZ로 자동 전환
- 거부 이유:
    - 시간당 $0.062 × 2 AZ × 24h × 30d = 월 +$90 추가
    - 토이 규모에 과잉. NAT 장애 자체가 드물고, 발생 시 수동 라우팅 변경으로 5~10분 내 회복 가능

**Option B — NAT Instance (EC2 self-managed)**
- 장점: 월 $5~$10로 저렴
- 거부 이유:
    - SPoF + 패치 · 모니터링 · HA 직접 관리 부담
    - 트래픽 증가 시 성능 병목 (단일 EC2)
    - 인프라 단순화 원칙에 역행

**Option C — NAT 미사용 (private subnet 인터넷 차단)**
- 장점: 가장 안전 + 비용 0
- 거부 이유:
    - ECS Task가 ECR · Secrets Manager · CloudWatch Logs에 도달할 수 없음
    - 모든 외부 호출을 VPC Endpoint로 대체 시 Endpoint 비용이 NAT 비용을 초과 가능 + 일부 서비스는 Endpoint 미지원

**Option D (선택) — Single NAT Gateway (public-2a 1개)**
- 비용: 월 ~$45 + 데이터 전송 + AZ-a 장애 시 인터넷 단절 (Runbook으로 임시 NAT 추가 절차 보유)
- 보상: 관리형 + 충분한 가용성 + 비용 절반

### ALB vs CloudFront 진입점

**Option A — 모든 트래픽을 CloudFront 거쳐 ALB로**
- 장점: 정적 자산 캐싱 + DDoS 1차 방어 (AWS Shield Standard 자동)
- 거부 이유:
    - 현재 ThirdTool은 SPA + API 서버 — 정적 자산은 별도 S3+CloudFront로 분리 예정 (Out of Scope)
    - CloudFront → ALB 경로는 별도 인증서(us-east-1 ACM)가 추가로 필요 → 복잡도 증가
    - 토이 트래픽에 캐싱 효익 적음

**Option B — ALB만 (선택)**
- 비용: 정적 자산 캐싱 효익 포기, DDoS 방어는 ALB level만
- 보상: 단일 진입점 + 단일 인증서(ap-northeast-2 ACM) + 디버깅 단순

### Route53 vs 외부 DNS

**Option A — Cloudflare DNS + ACM**
- 장점: Cloudflare 무료 + 별도 WAF 옵션
- 거부 이유:
    - ACM DNS validation 시 CNAME 자동 추가가 불가 → 수동 입력 + 인증서 갱신 시 매번 반복
    - Alias 레코드 미지원 (Cloudflare는 CNAME flattening 사용) → ALB DNS의 IP 변경 시 추적 부담

**Option B (선택) — Route53 hosted zone**
- 비용: hosted zone $0.50/월 + 쿼리당 $0.40/백만건 = 토이 규모 $1 미만
- 보상: ACM 자동 갱신 + Alias 레코드 + AWS 통합

### ACM 인증서 발급 방식

**Option A — Email validation**
- 장점: DNS 권한 없는 도메인에도 발급 가능
- 거부 이유:
    - 매년 수동 클릭 필요 → 누락 시 인증서 만료 사고
    - 인증 이메일이 도메인 관리자 5종 주소로만 전송 → 운영자 부재 시 발급 실패

**Option B — 도메인별 단일 인증서 N개**
- 장점: 인증서별 운영 환경 분리
- 거부 이유:
    - 관리 대상 인증서가 N배 증가 → 만료 알람 N배
    - prod·staging이 같은 운영자 책임이므로 분리 효익 적음

**Option C (선택) — `*.thirdtool.dev` 와일드카드 + DNS validation**
- 비용: subdomain 추가 시마다 새 인증서 발급 불요
- 보상: 1회 발급 + 영구 자동 갱신 + 모든 subdomain 자동 커버

### RDS 환경 대칭성

**Option A — prod·staging 모두 Multi-AZ**
- 장점: staging이 prod와 완전 동일 구조 → load-test 결과 1:1 외삽
- 거부 이유:
    - 월 +$110 추가 비용 — staging은 데이터 보존 의무 약함
    - 동기 복제 비용을 staging이 부담할 가치 적음

**Option B — prod·staging 모두 single-AZ**
- 장점: 비용 최소
- 거부 이유:
    - prod RTO/RPO가 시간 단위로 악화 — 인프라 격상 동기 자체가 무너짐

**Option C (선택) — 비대칭 (prod Multi-AZ · staging single-AZ)**
- 비용: load-test 결과 외삽 시 환경 차이 보정 필요 → baseline.md에 명시
- 보상: 비용 절감 + prod SLA 확보 균형

### 보안 그룹 분리 입도

**Option A — 단일 SG로 통합 (모든 리소스 동일 SG)**
- 장점: 관리 대상 SG 1개로 단순
- 거부 이유:
    - 단일 inbound 규칙 실수가 전체 노출 (RDS가 인터넷에 노출되는 사고)
    - "누가 누구를 호출하나" 의도가 코드에 보이지 않음

**Option B — 서비스별 SG (Card-svc-sg, Deck-svc-sg 등)**
- 장점: 서비스 단위 격리
- 거부 이유:
    - 단일 ECS Task에 모든 BC가 들어 있는 모놀리스 구조 → 서비스별 분리 의미 없음
    - SG 수가 BC 수만큼 증가 (6개+) → AWS quota 부담

**Option C (선택) — Layer별 5종 SG (alb / app / db / bastion / vpc-endpoint)**
- 비용: 신규 layer 추가 시 SG 추가 (드물)
- 보상: SG 간 참조 관계가 곧 트래픽 의도 다이어그램 → 신규 합류자 이해 용이

### VPC Endpoint 도입 시점

**Option A — v1부터 S3 · ECR · Secrets Manager · CloudWatch endpoint 전부 도입**
- 장점: NAT 통과 트래픽 절감 (NAT GB당 $0.062 → endpoint GB당 $0.01)
- 거부 이유:
    - Interface Endpoint는 ENI당 시간당 $0.01 × 24h × 30d × N개 = 월 +$25~$70
    - 토이 트래픽에서 NAT GB 비용 < endpoint 고정 비용 → 손익분기점 미달

**Option B (선택) — Product C로 이연**
- 비용: v1 기간 NAT 통과로 약간의 비용 부담 + 일부 보안 표면 잔존 (NAT 통한 인터넷 경유)
- 보상: 토이 규모에 적정. 트래픽 증가 + 비용 측정 후 Product C에서 손익분기점 검증 후 도입

## 전체 아키텍처 (High-Level Architecture)

> 트래픽이 외부 → 도메인 → ALB → ECS Task → RDS로 흐르는 단방향 토폴로지를 한 장으로. SG 간 화살표가 곧 호출 의도.

### 네트워크 토폴로지

```
                       Internet
                          │
                          │  HTTPS (443)
                          ▼
            ┌──────────────────────────────┐
            │  Route53 (Hosted Zone)        │  ─── api. A alias    ──► prod-alb          (BE, 본 Product)
            │  thirdtool.dev               │  ─── apex/www A alias──► CloudFront         (FE, product-fe-cdn.md)
            │                              │  ─── staging.* A     ──► staging-alb        (BE staging)
            └──────────────────────────────┘
                          │
                          ▼
            ┌──────────────────────────────────────────────────────────────────┐
            │                  VPC  10.0.0.0/16  (ap-northeast-2)             │
            │                                                                  │
            │   ┌─────────────── AZ ap-northeast-2a ─────────┐  ┌── 2c ──┐    │
            │   │                                             │  │         │   │
            │   │  public-2a 10.0.0.0/24                      │  │ public  │   │
            │   │  ┌───────────────────────────┐              │  │  -2c   │   │
            │   │  │  ALB (alb-sg)             │◄────HTTP 443─┤  │ 10.0.  │   │
            │   │  │   80 → 301 redirect 443   │              │  │ 1.0/24 │   │
            │   │  │   443 + ACM cert          │              │  │        │   │
            │   │  └───────────┬───────────────┘              │  │        │   │
            │   │              │                              │  │        │   │
            │   │  ┌───────────────────────────┐              │  │        │   │
            │   │  │  NAT GW (public-2a only)  │              │  │ (no    │   │
            │   │  │  for private→Internet     │              │  │  NAT)  │   │
            │   │  └───────────▲───────────────┘              │  │        │   │
            │   │              │ outbound                     │  │        │   │
            │   │  ┌───────────┴───────────────┐              │  │        │   │
            │   │  │  IGW (VPC attach)         │              │  │        │   │
            │   │  └───────────────────────────┘              │  │        │   │
            │   │                                             │  │        │   │
            │   │  app-2a 10.0.10.0/24                        │  │ app-2c │   │
            │   │  ┌───────────────────────────┐              │  │10.0.   │   │
            │   │  │  ECS Fargate Task (Pdt B) │◄──TG type=ip─┤  │11.0/24 │   │
            │   │  │  app-sg : alb-sg → 8080   │              │  │        │   │
            │   │  └───────────┬───────────────┘              │  │        │   │
            │   │              │ 3306                         │  │        │   │
            │   │              ▼                              │  │        │   │
            │   │  data-2a 10.0.20.0/24                       │  │ data-2c│   │
            │   │  ┌───────────────────────────┐              │  │10.0.   │   │
            │   │  │  RDS MySQL prod (primary) │◄──sync repl──┤──┤RDS std │   │
            │   │  │  db-sg : app-sg → 3306    │              │  │by      │   │
            │   │  │  Multi-AZ · backup 7d     │              │  │        │   │
            │   │  └───────────────────────────┘              │  │        │   │
            │   │                                             │  │        │   │
            │   └─────────────────────────────────────────────┘  └────────┘   │
            │                                                                  │
            │   staging-rds (single-AZ) — data-2a 또는 2c 중 1개에만 배치     │
            │                                                                  │
            └──────────────────────────────────────────────────────────────────┘
                          ▲
                          │ DNS validation (CNAME)
                          │
            ┌──────────────────────────────┐
            │  ACM (ap-northeast-2)        │
            │   *.thirdtool.dev            │
            │   auto-renew 60d before exp  │
            └──────────────────────────────┘
```

### 보안 그룹 호출 그래프

```
                 0.0.0.0/0
                      │ 443, 80
                      ▼
                  ┌────────┐
                  │ alb-sg │ ─── (outbound: any) ───┐
                  └────────┘                        │
                      │  8080                       │
                      ▼                             ▼
                  ┌────────┐                  ┌──────────────────┐
                  │ app-sg │ ─── 443 ──►      │ vpc-endpoint-sg  │ (Product C)
                  └────────┘                  └──────────────────┘
                      │  3306
                      ▼
                  ┌────────┐
                  │  db-sg │
                  └────────┘

bastion-sg : (SSM Session Manager 채택 시 inbound 비어 있음, outbound only)
```

원칙: alb-sg만 0.0.0.0/0 inbound를 허용. 그 외 SG는 **다른 SG를 source로 참조**하여 트래픽 의도를 코드에 박는다.

### 핵심 플로우

**1. 외부 사용자 API 요청 (HTTPS)**
```
Client ─https://api.thirdtool.dev/api/v1/cards─►
   ├─ Route53 A alias 조회 → prod-alb DNS
   ├─ ALB:443 (ACM ap-northeast-2 cert TLS 종단)
   ├─ Target Group(type=ip) → app-sg ECS Task IP 라운드 로빈
   ├─ ECS Task:8080 (Spring Boot, 평문)
   └─ JDBC → db-sg RDS:3306 (primary)
        ↑↓ sync repl (Multi-AZ standby)
```

**2. HTTP → HTTPS 자동 리다이렉트 (BE 도메인)**
```
Client ─http://api.thirdtool.dev─►
   ALB:80 default action = redirect 301 to https:443
   Client ─https://api.thirdtool.dev─►  (위 플로우 재진입)
```

**3. FE 요청 (thirdtool.dev / www) → CloudFront (product-fe-cdn.md)**
```
Client ─https://thirdtool.dev─►
   Route53 apex A alias → CloudFront distribution
   CloudFront ← S3 thirdtool-fe-prod (OAC)
   [본 Product의 VPC·ALB와 무관]
```

**3. ECS Task → ECR Pull (배포 시)**
```
ECS Task ─ECR API─►
   private-rt: 0.0.0.0/0 → NAT GW(public-2a)
   NAT GW → IGW → ECR endpoint
   (v2: VPC Interface Endpoint로 NAT 우회 검토)
```

**4. RDS Multi-AZ Failover (장애 시)**
```
primary(2a) 장애 감지
   ├─ RDS가 30~60초 내 standby(2c) 승격
   ├─ RDS DNS endpoint가 standby IP로 자동 갱신
   └─ ECS Task의 HikariCP가 stale connection 폐기 → 재연결
         (애플리케이션 재기동 불요)
```

### Out-of-Process 의존

- **AWS 계정 단일**: 모든 리소스가 동일 계정. IAM + 환경 태그로 격리 (Product C에서 IAM 권한 경계 강화)
- **Domain Registrar**: `thirdtool.dev` 등 보유 도메인 NS 위임 (Route53 hosted zone NS 4개를 Registrar에 등록)
- **ACM (ap-northeast-2)**: `*.thirdtool.dev` — ALB용. DNS validation Route53 자동 추가
- **ACM (us-east-1)**: `*.thirdtool.dev` — CloudFront용 (CloudFront는 us-east-1 ACM만 허용). product-fe-cdn.md에서 관리하되 DNS validation CNAME은 ap-northeast-2와 공유 — Route53에 1회만 추가하면 양쪽 검증
- **CloudWatch**: ACM 만료 알람 · ALB 5xx 메트릭 · RDS Performance Insights (Product C에서 통합)

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 대응

| 시나리오 | 감지 신호 | 즉시 대응 | 근본 대책 |
| --- | --- | --- | --- |
| ACM 인증서 만료 | CloudWatch `DaysToExpiry < 30/14/7` 알람 + 사용자 브라우저 경고 | DNS validation CNAME 존재 확인, 자동 갱신 수동 트리거 | 60일 전 알람 + Route53 zone NS 위임 모니터링 |
| RDS primary 장애 (AZ-a) | RDS event + CloudWatch `DatabaseConnections` 급락 + ECS 5xx 증가 | Multi-AZ 자동 failover 대기 (30~60초). 수동 개입 불요 | Multi-AZ + HikariCP `connection-test-query` 활성 |
| NAT Gateway (AZ-a) 장애 | private subnet 인터넷 outbound 전부 실패 + ECR pull 실패 + 외부 OAuth provider 호출 실패 | private-rt를 임시 AZ-c NAT로 변경 (Runbook) | NAT를 v2에서 Multi-NAT로 격상 검토 |
| DNS propagation 지연 | Route53 변경 후 일부 사용자에게 옛 IP 보임 | TTL 단축 (60s) 후 변경 적용 | 변경 전 TTL 미리 단축 (Routine) |
| Security Group 잘못된 규칙 (예: db-sg에 0.0.0.0/0 추가) | CloudTrail event + Config Rule 위반 | 즉시 룰 revoke + 노출 시간 동안 RDS audit log 검토 | JSON spec(security-groups.json) 수정 + CLI 적용 원칙 유지 |
| ALB Target unhealthy | Target Group `UnHealthyHostCount > 0` 알람 + ALB 503 | ECS Task 로그 확인 (Health endpoint 응답 코드) | Health check 임계값 보정 + ECS deployment circuit breaker (Product B) |
| ACM 갱신 실패 (CNAME 삭제됨) | `RenewalEligibility: INELIGIBLE` + `DaysToExpiry` 감소 | CNAME 재추가 후 강제 갱신 | DNS validation record를 Route53 CLI로 관리 + infra/ spec에 기록 |
| RDS 스토리지 한도 도달 | CloudWatch `FreeStorageSpace < 10%` 알람 | gp3 autoscaling 이미 활성 (max 500GB) — 자동 확장 대기 | 백업 보존 기간 검토 + 오래된 데이터 archive 전략 (Product C) |
| Route53 health check 실패 | hosted zone health check status DOWN | apex가 ALB 외 다른 대상 가리키는지 확인 → ALB 자체 장애 시 staging ALB로 임시 전환 (Runbook) | 멀티 리전 active-passive (v2) |

### 관측 지표 (Metrics)

**ALB 지표 (CloudWatch · AWS/ApplicationELB)**
- `RequestCount` — 트래픽 절대량
- `HTTPCode_ELB_5XX_Count` — ALB 자체 오류 (target 미응답 등)
- `HTTPCode_Target_5XX_Count` — ECS Task 오류 — Product op·load-test와 합류
- `TargetResponseTime` p50/p95/p99 — 응답 지연
- `HealthyHostCount` · `UnHealthyHostCount` — Target Group 상태

**RDS 지표 (CloudWatch · AWS/RDS + Performance Insights)**
- `CPUUtilization` — 인스턴스 부하
- `DatabaseConnections` — 커넥션 풀 사용량 (HikariCP `maximum-pool-size` 대비)
- `FreeStorageSpace` · `FreeableMemory`
- `ReadLatency` · `WriteLatency`
- Performance Insights: `db.load.avg` + Top SQL — slow query 단위 추적

**VPC 흐름 (Flow Logs · v2)**
- private subnet에서 외부로의 ACCEPT/REJECT 로그
- 비정상 outbound (예: 사설 IP가 외부 미상 IP에 다량 요청) 탐지

**Route53 (Health Check)**
- `HealthCheckStatus` — apex · staging 각각
- 실패 시 SNS 알람

**ACM**
- `DaysToExpiry` — 30 / 14 / 7일 다단 알람

### 로깅 정책

- **VPC Flow Logs**: Product C에서 활성 (CloudWatch Logs에 적재) — REJECT 트래픽만 보존하여 비용 절감
- **ALB Access Logs**: S3에 적재 (Product C) — 비용/볼륨 큰 편이므로 retention 14일
- **RDS Slow Query Log**: 파라미터 그룹 `slow_query_log=1` + `long_query_time=1` → CloudWatch Logs export
- **CloudTrail**: 계정 전역 활성 (Product C) — SG 변경 · RDS 삭제 시도 등 추적
- **절대 금지**: RDS endpoint 자체를 public log에 노출, SG 규칙 변경 로그를 검토 없이 무시

## 롤아웃 / 마이그레이션 (Rollout)

### 전제 — 운영 트래픽 0명 (점진 전환 불요)

현재 ThirdTool은 사용자 0명. Default VPC + 수동 EC2 + 단일 RDS에서 명시 VPC + ALB + Multi-AZ RDS로 **일괄 전환**한다. 트래픽이 있었다면 다음 단계가 필요했을 것:
- 1주차: 신 VPC + ALB + RDS 병행 생성 (도메인 DNS는 옛 EC2 IP 유지)
- 2주차: Route53 weighted routing으로 신·구 트래픽 10/90 → 50/50 → 90/10 점진 전환
- 3주차: 옛 EC2 종료 + 옛 RDS 데이터 dump→ 신 RDS import 완료
- 4주차: 옛 자원 해체

위 점진 전환을 생략한 것은 **수용 가능한 단순화 선택**. v2에서 트래픽 발생 후 동일 변경 필요 시 위 4주차 패턴을 재사용한다.

### Epic 실행 순서 (의존성 그래프)

```
Epic 1 (VPC 토대)
  Story 1-1 (VPC + 6 subnet + IGW + NAT + 5 SG)
       │
       ├─► Epic 2 (ALB + 도메인 + TLS)
       │     Story 2-1 (ALB + Target Group + Route53 + ACM)
       │           │
       │           └─► Product B Epic B-2 (ECS Service → Target Group 등록)
       │
       └─► Epic 3 (RDS MySQL)
             Story 3-1 (prod Multi-AZ + staging single-AZ + Flyway baseline)
                   │
                   └─► Product C Epic C-3 (Secrets Manager에 RDS credential 이관)
```

Epic 1이 모든 후속의 선행 — subnet ID · SG ID가 Epic 2/3의 입력. Epic 2는 Epic 3와 병행 가능하나 ALB가 ECS와 결합되는 시점은 Product B에서.

### 단계별 검증 게이트

| 단계 | 검증 명령 | 통과 기준 |
| --- | --- | --- |
| Epic 1 완료 후 | `aws ec2 describe-vpcs` + `describe-subnets` + `describe-security-groups` | VPC `10.0.0.0/16` + 6 subnet + 5 SG 확인, alb-sg 외 inbound CIDR 0건 |
| Epic 2 완료 후 (ECS 없이) | `curl -I https://thirdtool.dev/actuator/health` | ALB 503 응답 (target 0개) — 인증서 자체는 valid |
| Epic 3 완료 후 | `mysql -h ${RDS_PROD_ENDPOINT} -u admin -p` (Bastion 또는 SSM Session Manager 경유) | 접속 성공 + `SELECT @@time_zone;` 결과 `+00:00` |
| Product B 합류 후 | `curl https://thirdtool.dev/actuator/health` | 200 OK + `{"status":"UP"}` |

### 롤백 계획

- **VPC · subnet 삭제 금지** — 일단 생성 후에는 후속 Epic이 의존. 삭제 필요 시 관련 aws CLI destroy 명령 순서 Runbook 참조.
- **ALB 롤백**: 기존 EC2 IP를 Route53에 임시 복원 → ALB DNS 변경 → 점진 트래픽 전환 후 ALB 삭제. 도메인 TTL 60s로 사전 단축.
- **RDS 롤백**: 자동 백업 7일 + PITR로 임의 시각 복구. Multi-AZ failover 자체는 30~60초 자동 — 롤백 개념 없음.
- **ACM 인증서 롤백**: 인증서 자체는 ARN으로 영구 보존. ALB Listener에서 attach 해제 후 옛 인증서로 교체 가능.

### 환경 분기

- `application-local.yml`: `jdbc:h2:mem:thirdtool` — RDS 미사용 (개발 부담 회피)
- `application-staging.yml`: `${RDS_STAGING_ENDPOINT}` + `HikariCP maximum-pool-size: 5`
- `application-prod.yml`: `${RDS_PROD_ENDPOINT}` + `HikariCP maximum-pool-size: 10` + `Secure=true` 쿠키

## 성공 지표 (KPI)

| 지표 | 현재 값 | 목표 값 | 측정 방법 |
| --- | --- | --- | --- |
| Default VPC 사용 리소스 수 | 미상 (>0 추정) | 0 | 콘솔 또는 `aws ec2 describe-instances --filters Name=vpc-id,Values=<default>` |
| ALB DNS 응답 가능 여부 | 없음 | 200 OK (`/actuator/health`) | `curl https://api.thirdtool.dev/actuator/health` |
| HTTPS 강제 비율 | 미상 | 100% (HTTP→HTTPS 301) | `curl -I http://api.thirdtool.dev` |
| ACM 인증서 만료까지 남은 일수 | 미상 | ≥ 60일 (자동 갱신 보장) | `aws acm describe-certificate` |
| RDS Multi-AZ 활성화 (prod) | 미상 | true | `aws rds describe-db-instances` |
| RDS 자동 백업 보존 기간 | 미상 | 7일 | 동일 |
| 보안 그룹 inbound rule 수 (alb-sg 외) | 미상 | 각 SG inbound는 다른 SG 참조 only (CIDR 0.0.0.0/0 = 0건) | `aws ec2 describe-security-groups` |
| 스테이징 RDS endpoint 분리 | 미상 | 분리 (다른 endpoint URL) | endpoint 비교 |
| VPC 흐름도 다이어그램 커밋 여부 | 없음 | 커밋됨 | `docs/architecture/vpc-topology.md` |

## Scope

- **In Scope**
    - VPC 1개 (`10.0.0.0/16`) + public/private subnet 6개 (2 AZ × 3 layer)
    - Internet Gateway + Route Table + NAT Gateway 1개 (single-AZ)
    - 보안 그룹 5종 (alb / app / db / bastion / vpc-endpoint)
    - ALB 1개 공유 (host-header staging routing) + Target Group(type=ip) 2종(prod·staging) — **ADR016 deviation: M1 트래픽 0명 단계 시간 비용 절감. M2에서 staging LCU 경합 임계 도달 시 ALB 2개 분리 follow-up**
    - Route53 public hosted zone 1개 (apex + staging subdomain)
    - ACM 인증서 2개 (`*.thirdtool.dev`, `thirdtool.dev`) + DNS validation 자동 갱신
    - RDS MySQL 8.0 2개 (prod Multi-AZ + staging single-AZ) + 파라미터 그룹 + 서브넷 그룹
    - VPC 흐름도 문서화 (`docs/architecture/vpc-topology.md`)
- **Out of Scope**
    - Terraform IaC 코드화 — v0.0.5+ 이후 별도 마일스톤 (infra/ JSON spec이 선행 문서)
    - ECS Cluster/Service/Task Definition — Product B
    - CI/CD 파이프라인 재설계 — Product B
    - Secrets Manager 이관 — Product C
    - FE CDN (S3 + CloudFront + Route53 FE 레코드 + GHA FE CI/CD) → **product-fe-cdn.md**
    - WAF · Shield Advanced (v2)
    - VPC Peering · Transit Gateway (단일 VPC라 불필요)
    - 다중 리전 (DR은 동일 리전 백업으로 v1 시작)
    - 별도 AWS 계정 분리 (단일 계정 + 환경 태그 모델, v2 검토)
    - Bastion Host (필요 시점에 별도 PR — SSM Session Manager 우선)

## 대상 사용자

- 주요 사용자: ThirdTool 백엔드 개발자 (1인 운영)
- 사용 맥락:
    - 새 도메인 또는 인증서 추가 → Route53 + ACM 표준 절차에 따라 변경
    - RDS 장애 신고 접수 → Multi-AZ 자동 페일오버 확인 + CloudWatch RDS 메트릭 즉시 확인
    - 스테이징 환경 baseline 측정 (`product-load-test.md`) → staging.thirdtool.dev 진입점으로 k6 실행
    - 면접·포트폴리오 설명 시 → VPC 다이어그램 + SG 분리도 + RDS Multi-AZ 결정 근거로 답변
    - 신규 합류자 온보딩 → `docs/architecture/vpc-topology.md`로 5분 안에 트래픽 경로 파악

## 연결된 Epic 목록

- [ ]  Epic 1. VPC 토대 — CIDR · subnet · IGW · NAT · 5종 보안 그룹
- [ ]  Epic 2. ALB + 도메인 + TLS — ALB · Target Group(type=ip) · Route53 · ACM 자동 갱신
- [ ]  Epic 3. RDS MySQL Multi-AZ + staging single-AZ — 파라미터 그룹 · 서브넷 그룹 · 자동 백업

## 관련 문서

- 상위 문서: ThirdTool 백엔드 컨벤션 · `application-prod.yml` 환경 설정
- 선행 Product: 없음 (인프라 토대의 가장 아래)
- 후속 Product:
    - Product B (`product-infra-deploy.md`) — ALB Target Group ARN · ECS Cluster 배치 subnet · IAM Task Role을 본 Product 산출물 기반으로 구성
    - Product C (`product-infra-ops.md`) — Secrets Manager 비밀 이관 + CloudWatch Alarm · 백업 정책 추가
    - `product-load-test.md` — staging 환경 제공으로 ADR-LOAD-002("프로덕션 금지") 전제 충족
    - `product-op.md` — Prometheus가 VPC 내부 위치로 이동(검토)
    - `product-auth.md` — OAuth 콜백 URL이 `https://api.thirdtool.dev`로 고정 → application-prod.yml 갱신 (✅ 완료)
    - `product-fe-cdn.md` — 본 Product의 Route53 hosted zone + CloudFront 레코드 관할. thirdtool.dev·www → CloudFront A alias
- 참고 자료: AWS VPC Best Practices · ALB Routing · ACM Auto-Renewal · RDS Multi-AZ
- ADR 후보: `ADR-INFRA-001~005`

## 열린 질문 (Open Questions)

> 본 Product에서 결정하지 않고 후속 Product/v2로 이연하는 질문들. 의식적 보류이며, 답이 정해지지 않은 채로 진행한다.

### 인프라 토폴로지

- **멀티 리전 (Disaster Recovery) 도입 시점은?**
    - 현재 단일 리전(ap-northeast-2). ap-northeast-2 전체 장애 시 서비스 중단을 수용할지, 아니면 ap-northeast-1 또는 us-east-1로 active-passive failover를 도입할지 미결.
    - 후보 기준: 월간 활성 사용자 100명 도달 시 검토. 그 전에는 RTO 수일 수용.
    - 비용 영향: 멀티 리전 RDS Read Replica + Route53 health check + S3 cross-region replication = 월 +$200 추정.

- **VPC Peering 또는 Transit Gateway 필요 시점은?**
    - 현재 단일 VPC. 향후 별도 VPC(예: 데이터 분석 전용, sandbox 등) 도입 시 peering 또는 TGW 결정 필요.
    - v1은 모든 환경이 동일 VPC 내 → 보류.

- **별도 AWS 계정 분리 (prod/staging/dev 멀티 계정)는?**
    - 현재 단일 계정 + 환경 태그 모델. AWS Control Tower 또는 Organizations로 계정 분리하면 IAM 격리 강화 가능.
    - 단점: 운영 부담 증가 (계정 N개 SSO + billing 통합). 토이 규모에 과잉. v2 검토.

### 보안

- **WAF (AWS WAF v2) 도입 시점은?**
    - ALB 앞에 WAF 부착하면 SQL injection · XSS · rate-limit 1차 방어 가능. Managed Rules 무료, custom rules 월 ~$5.
    - 현재 트래픽 0명 → 효익 측정 불가. Product C 또는 v2로 이연.

- **Bastion Host vs SSM Session Manager 최종 결정은?**
    - 본 Product는 bastion-sg만 생성하고 인스턴스는 만들지 않음. SSM Session Manager로 RDS 접속 (Port forwarding) 가능 여부 검증 필요.
    - SSM 채택 시 Bastion 인스턴스 비용 0 + IAM 기반 접근 제어. SSM 미지원 시나리오 발견되면 Bastion 생성.

- **VPC Flow Logs 보존 기간 + 분석 도구는?**
    - REJECT 트래픽만 적재 가정. 그러나 ACCEPT까지 보존하여 정상 패턴 baseline을 만들 가치는?
    - Athena · OpenSearch 둘 중 어느 도구로 분석할지 미결 (Product op·logging과 합류).

### 비용 · 운영

- **NAT Gateway → VPC Endpoint 전환 손익분기점은?**
    - 현재 NAT 단일. ECS Task가 ECR Pull · CloudWatch Logs · Secrets Manager에 도달할 때 NAT 통과.
    - Interface Endpoint 도입 시 ENI당 시간당 $0.01 × N개. NAT 통과 트래픽 측정 후 손익분기점 계산 필요 (Product C).

- **RDS 백업 정책 — Multi-AZ failover 후 백업 시점 동기화는?**
    - Multi-AZ는 동기 복제이므로 백업은 standby에서 수행 → primary 부하 0. 그러나 failover 시 백업 윈도우와 충돌하면?
    - 검증: 페일오버 직후 백업 스케줄이 정상 진행되는지 1회 실측 필요.

- **RDS read replica 도입 시점 + 라우팅 방식은?**
    - 현재 단일 인스턴스(prod Multi-AZ standby는 read 불가). Read 트래픽 분리는 v2.
    - 라우팅: HikariCP routing datasource vs ProxySQL vs RDS Proxy 미결.

### 도메인 · DNS

- **도메인 자체 이관 시점은?**
    - 현재 외부 Registrar 보유 가정. Route53 Registrar로 이관 시 도메인 갱신 자동화 + DNSSEC 가능.
    - 이관 비용: 도메인당 $12 + 1년 갱신 포함. v2 검토.

- **DNSSEC 활성 여부는?**
    - 토이 규모에서 DNS hijacking 위험은 낮지만 ACM 인증서 + DNSSEC 조합으로 신뢰성 강화 가능.
    - 검토 시점: 도메인 Route53 이관 후.

### 향후 결정 트리거

| 트리거 | 재검토 항목 |
| --- | --- |
| 월간 사용자 100명 도달 | 멀티 리전 DR · WAF · RDS read replica |
| NAT 비용 월 $20 초과 | VPC Endpoint 도입 손익분기 |
| 인증서 만료 사고 발생 | ACM 갱신 모니터링 자동화 강화 |
| RDS slow query 다수 발견 | read replica 또는 caching layer (ElastiCache) |
| AZ-a 장애 1회 경험 | Multi-NAT으로 전환 |

---

| Epic | Story 수 | SP 합계 |
| --- | --- | --- |
| Epic 1. VPC 토대 | 1 | 3 |
| Epic 2. ALB + 도메인 + TLS | 1 | 4 |
| Epic 3. RDS MySQL | 1 | 5 |
| **합계** | **3** | **12 SP** |

**진행 순서 (필수):** Epic 1 → 2 → 3. VPC 위에 ALB·subnet 배치, 그 안에 RDS 배치.

---

## Epic 1. VPC 토대 — CIDR · subnet · IGW · NAT · 5종 보안 그룹

# Epic 1. VPC 토대 — CIDR · subnet · IGW · NAT · 5종 보안 그룹

## Epic 목표

> `10.0.0.0/16` VPC를 ap-northeast-2a/c 2 AZ에 6개 subnet(public/app/data × 2)으로 분할하고, Internet Gateway + single-NAT Gateway + 라우팅 테이블 + 5종 보안 그룹을 구성해 트래픽 진입·격리 경로를 명시한다.
>

## 배경

- Default VPC 위에서 손으로 띄운 EC2/RDS는 보안 경계가 모호 — SG 규칙이 0.0.0.0/0 또는 광범위한 CIDR로 박혀 있을 가능성 높음
- subnet 분리가 없으면 RDS가 인터넷에 직접 노출될 수 있는 토폴로지가 만들어짐 — 단일 SG 실수가 데이터 유출로 직결
- 후속 Epic (ALB · RDS · ECS)이 의존할 subnet ID · SG ID가 본 Epic의 산출물

## 핵심 설계 결정

> **subnet은 layer별로 그룹화한다. public(인터넷 진입) · app(ECS Task) · data(RDS).**
서비스별 분리가 아닌 layer별 분리.
>
> - public: ALB · NAT Gateway · Bastion(필요 시)
> - app: ECS Task (Fargate ENI)
> - data: RDS · ElastiCache(v2)
> - 새 서비스 추가 시 layer만 결정하면 됨 — subnet ID 새로 만들 필요 없음

> **보안 그룹 inbound는 CIDR이 아닌 다른 SG를 참조한다.**
"누가" 호출하는지가 IP가 아닌 역할로 표현.
>
> - `app-sg` inbound: `alb-sg`에서 8080만 허용 (CIDR 미사용)
> - `db-sg` inbound: `app-sg`에서 3306만 허용
> - `alb-sg` inbound: 0.0.0.0/0에서 443/80 허용 (유일하게 광범위 CIDR 허용)
> - 이렇게 하면 새 ECS Task가 추가돼도 SG 규칙은 변경 불요

## 완료 기준 (Definition of Done)

- [ ]  VPC `10.0.0.0/16`이 생성되고 ap-northeast-2a/c 2 AZ에 6개 subnet이 명시 배치된다
- [ ]  Internet Gateway가 VPC에 attach되고 public subnet route table이 0.0.0.0/0 → IGW로 설정된다
- [ ]  NAT Gateway 1개가 public-2a에 생성되고 private subnet route table이 0.0.0.0/0 → NAT으로 설정된다
- [ ]  5종 보안 그룹(`alb-sg`, `app-sg`, `db-sg`, `bastion-sg`, `vpc-endpoint-sg`)이 최소 권한으로 생성된다
- [ ]  연결된 Story가 모두 Done 상태다

## 산출물 (Deliverables)

- VPC ID · subnet ID 6개 · 보안 그룹 ID 5개 (Terraform output으로 노출, Product C에서 import)
- VPC 흐름도 다이어그램 (`docs/architecture/vpc-topology.md`)
- 보안 그룹 규칙 표 (위 문서에 포함)

## 연결된 Story 목록

- [ ]  Story 1-1. VPC + 6 subnet + IGW + single-NAT + 5종 보안 그룹 구성 (3 SP)

## 내부 메모 / 제약 사항

- VPC CIDR `10.0.0.0/16`은 다른 VPC와 향후 peering 시 충돌 회피용 — `192.168.x.x`는 가정용 라우터 충돌 가능
- NAT Gateway 비용: 시간당 $0.062 + GB당 $0.062 → 토이 규모 월 ~$45. Multi-NAT은 +$90으로 과잉
- VPC Endpoint(S3 · ECR · Secrets Manager): NAT 통과 트래픽 절감 위해 Product C에서 추가 (현재 Product 범위 외)
- bastion-sg는 SG만 미리 생성하고 인스턴스는 v2 (SSM Session Manager로 대체 가능)

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

### Subnet 분할 입도

**Option A — 2종 (Public / Private)**
- 장점: subnet 4개로 단순
- 거부 이유:
    - ECS Task와 RDS가 같은 subnet 또는 같은 layer에 위치 → 의도 분리 불가
    - RDS endpoint를 Private subnet에 두면 ECS와 동일 — SG 분리에 전적으로 의존

**Option B — 4종 (Public / Private-App / Private-Data / Isolated)**
- 장점: Isolated subnet은 인터넷 outbound도 없음 → 최고 격리
- 거부 이유:
    - RDS는 Multi-AZ failover에 NAT 경유 outbound가 불요하므로 Isolated 적용 가능하지만, Performance Insights · 자동 백업 등 일부 기능이 outbound 필요 시 부담
    - subnet 8개 (4 layer × 2 AZ) — 토이 규모 과잉

**Option C (선택) — 3종 (Public / App / Data)**
- 비용: data subnet 인터넷 outbound가 NAT를 거치는 표면 잔존
- 보상: 명확한 3-layer 의도 + subnet 6개 적정 + 신규 합류자 이해 용이

### 라우팅 테이블 분리 입도

**Option A — Public 1개 + Private 1개 통합**
- 장점: 라우팅 테이블 2개로 최소
- 거부 이유:
    - data subnet도 private-rt 사용 → 인터넷 outbound 가능 (의도 외)
    - data subnet 격리 강화를 위해 별도 RT가 더 안전

**Option B (선택) — Public 1개 + Private 1개 (data subnet도 공유) + 향후 분리**
- 비용: data subnet outbound 표면 잔존
- 보상: v1 단순화. v2에서 data-rt 분리 + NAT 제거로 격상 가능 (재작업 비용 낮음)

### IPv6 지원 활성 여부

**Option A — IPv4 + IPv6 dual stack**
- 장점: 미래 호환 + 일부 클라이언트(모바일 캐리어)는 IPv6 우선
- 거부 이유:
    - SG · ACL이 양쪽 다 관리 → 실수 표면 2배
    - 토이 규모에 효익 측정 불가

**Option B (선택) — IPv4 only**
- 비용: IPv6 전용 클라이언트는 ISP 6to4 변환에 의존
- 보상: 운영 단순. v2에서 dual stack 추가는 subnet IPv6 block 할당만 하면 됨 (재작업 작음)

### 5종 SG 분리 vs Layer당 1개 SG

**Option A — Layer당 1개 SG (3종: alb-sg, app-sg, db-sg)**
- 장점: 관리 대상 SG 3개
- 거부 이유:
    - bastion · vpc-endpoint는 layer가 아니라 역할 → 같은 layer에 두기 어색
    - 향후 endpoint 추가 시 app-sg에 endpoint 규칙 섞임

**Option B (선택) — 5종 (alb / app / db / bastion / vpc-endpoint)**
- 비용: SG 5개 관리
- 보상: 역할별 명확한 분리 + 신규 endpoint 추가 시 vpc-endpoint-sg만 수정

### NAT Gateway 배치 AZ 선택

**Option A — public-2c에 배치**
- 거부 이유: 특별한 사유 없음. 임의 선택 시 일관성 부족

**Option B — public-2a · 2c 양쪽 (Multi-NAT)**
- 거부 이유: 비용 +$45/월 — Product 레벨 결정과 일관 (single-NAT)

**Option C (선택) — public-2a에 single 배치**
- 비용: 2a 장애 시 private subnet 인터넷 단절
- 보상: 알파벳 순 일관성 + Runbook으로 임시 NAT 추가 절차 보유

---

## Story 1-1. VPC + 6 subnet + IGW + single-NAT + 5종 보안 그룹 구성

### User Story

> As a 백엔드 개발자,
I want ThirdTool 전용 VPC가 layer별 subnet으로 분할되고 SG가 최소 권한으로 설정되길,
So that 후속 ALB·ECS·RDS 배치 시 subnet/SG ID만 참조하면 되고, 새 서비스 추가 시 SG 규칙 누락으로 인한 노출 사고가 차단된다.
>

### 설계 노트

- VPC 구성

    ```
    VPC: 10.0.0.0/16 (ap-northeast-2)
    │
    ├── public-2a:   10.0.0.0/24   (ALB · NAT · Bastion)
    ├── public-2c:   10.0.1.0/24   (ALB)
    │
    ├── app-2a:      10.0.10.0/24  (ECS Task)
    ├── app-2c:      10.0.11.0/24  (ECS Task)
    │
    ├── data-2a:     10.0.20.0/24  (RDS primary)
    └── data-2c:     10.0.21.0/24  (RDS standby)
    ```

- 보안 그룹 규칙 표

    | SG | Inbound | Outbound |
    | --- | --- | --- |
    | `alb-sg` | 0.0.0.0/0 → 80, 443 | all |
    | `app-sg` | `alb-sg` → 8080 | all |
    | `db-sg` | `app-sg` → 3306 | (없음) |
    | `bastion-sg` | (없음, SSM 채택 시) | all |
    | `vpc-endpoint-sg` | `app-sg` → 443 | (없음) |

- 라우팅 테이블
    - `public-rt`: 0.0.0.0/0 → IGW
    - `private-rt`: 0.0.0.0/0 → NAT Gateway (public-2a)

- 태깅 표준 (Terraform default tags)
    - `Project=ThirdTool`, `Environment=shared`, `ManagedBy=terraform`, `Layer={public|app|data}`

### 완료 기준 (Acceptance Criteria)

- [ ]  `aws ec2 describe-vpcs --filters Name=tag:Project,Values=ThirdTool` 결과에 VPC 1개 + CIDR `10.0.0.0/16`이 나온다
- [ ]  `aws ec2 describe-subnets --filters Name=vpc-id,Values=<vpc-id>` 결과 6개 subnet이 각각 ap-northeast-2a/c에 분배되어 있다
- [ ]  Internet Gateway가 VPC에 attached 상태다
- [ ]  NAT Gateway 1개가 public-2a에 Available 상태로 떠 있다
- [ ]  5종 SG의 inbound 규칙이 위 표대로 설정되어 있다
- [ ]  alb-sg 외의 모든 SG inbound가 다른 SG 참조 only (`describe-security-groups`에서 CIDR `0.0.0.0/0`이 alb-sg에만 존재)

### 엣지 케이스

- NAT Gateway가 placed된 AZ(2a) 장애 시 → private subnet 트래픽 인터넷 도달 불가. Runbook에 "임시 NAT를 2c에 추가 + 라우팅 테이블 갱신" 절차 명시
- RDS endpoint는 `data-*` subnet에 두지만 ECS에서는 `app-sg` → `db-sg` 경로로만 접근 가능 — bastion 없이 로컬에서 RDS 접속 불가 (의도된 격리)
- 신규 서비스가 outbound 인터넷 접근 필요 시 → `app-sg` outbound가 이미 all 허용이므로 추가 변경 불요
- VPC Endpoint를 추가하지 않은 v1 단계에서 S3 · ECR · Secrets Manager 호출이 NAT를 통과 → 비용 발생. Product C에서 VPC Endpoint로 우회

### Definition of Done

- [ ]  코드 리뷰 완료 (단, IaC는 Product C에서. v1은 콘솔 또는 CLI 수동 생성 OK)
- [ ]  VPC 흐름도 다이어그램 (`docs/architecture/vpc-topology.md`) 커밋
- [ ]  보안 그룹 규칙 표가 위 문서에 포함
- [ ]  staging 환경 신청도 본 VPC 안에 함께 배치 (별도 VPC 분리하지 않음 — ADR-INFRA-003 정합)
- [ ]  PO(또는 본인) 셀프 검수 완료

### 의존성

- 선행: 없음 (Product의 시작 Story)
- 후속: Story 2-1 (ALB가 public subnet 사용), Story 3-1 (RDS가 data subnet 사용)

### 스토리 포인트

- 추정: 3 SP

---

## Epic 2. ALB + 도메인 + TLS — ALB · Target Group(type=ip) · Route53 · ACM 자동 갱신

# Epic 2. ALB + 도메인 + TLS — ALB · Target Group(type=ip) · Route53 · ACM 자동 갱신

## Epic 목표

> ALB 2개(prod-alb, staging-alb)를 public subnet에 배치하고, `thirdtool.dev` 및 `staging.thirdtool.dev`를 Route53 + ACM으로 HTTPS 종단점으로 만들고, HTTP 요청이 자동 HTTPS 리다이렉트되는 표준을 깐다.
ALB Target Group은 Type=ip로 생성해 Product B의 ECS Fargate Task가 자동 등록될 수 있는 상태를 만든다.
>

## 배경

- Epic 1로 VPC가 깔렸지만 외부 인터넷에서 HTTPS로 들어올 진입점이 없음 — ALB가 본 Epic의 핵심
- 도메인이 EC2 IP에 직접 박혀 있는 v0 상태에서는 EC2 교체 시 DNS 변경 + 전파 지연이 발생 — ALB DNS로 추상화 필수
- ACM 인증서가 ALB에 attach되어 있어야 ECS Task는 평문 HTTP만 처리하면 됨 — Spring Boot 측 SSL 설정 불요
- OAuth 콜백 URL(`/login/oauth2/code/kakao` 등)이 도메인에 의존하므로 도메인 확정이 인증 Product의 전제

## 핵심 설계 결정

> **ALB Target Group Type=ip로 고정한다. Type=instance는 사용하지 않는다.**
ECS Fargate 호환의 필수 조건.
>
> - Fargate Task는 ENI 단위 private IP를 받아 ALB target group에 등록됨
> - Type=instance는 EC2 인스턴스 ID 매핑이므로 Fargate에서 동작 불가
> - 본 결정이 Product B의 컴퓨트 선택(ECS Fargate)을 잠금

> **Listener는 80(HTTP) → 443(HTTPS) 강제 리다이렉트 한 줄로 끝낸다.**
ALB 규칙으로 보장하고 ECS Task는 평문만 처리.
>
> - 80 listener: default action = redirect to https:443 with status 301
> - 443 listener: ACM 인증서 attach + target group forward
> - ECS Task는 8080 평문 — Spring Boot SSL 설정 불요 (인증서 관리도 ACM 단일 진입점)

> **헬스체크 경로는 `/actuator/health` 단일 표준으로 한다.**
`product-op.md`의 Actuator 노출 정책 정합.
>
> - 경로: `/actuator/health` · 매 30초 · 임계: 5초 timeout · 정상 임계 2회 / 비정상 임계 3회
> - 익명 호출 허용 — `product-op.md` Story 1-1에 명시된 노출 정책
> - 응답 200 + `{"status":"UP"}`만 정상으로 간주

## 완료 기준 (Definition of Done)

- [ ]  prod ALB와 staging ALB가 각각 public subnet에 internet-facing으로 생성된다
- [ ]  Target Group Type=ip 2개가 생성된다 (등록된 target은 0개 — Product B에서 ECS Service가 등록)
- [ ]  Route53 hosted zone `thirdtool.dev`가 생성되고 apex A record가 prod ALB, `staging.*` A record가 staging ALB로 alias 설정된다
- [ ]  ACM 인증서 `*.thirdtool.dev`(또는 `thirdtool.dev` + `staging.thirdtool.dev` 2종)가 DNS validation으로 발급되고 ALB 443 listener에 attach된다
- [ ]  HTTP 요청이 HTTPS로 301 리다이렉트된다
- [ ]  ACM 인증서 자동 갱신이 활성화된다
- [ ]  연결된 Story가 모두 Done 상태다

## 산출물 (Deliverables)

- ALB ARN 2개 · Target Group ARN 2개 · Route53 zone ID · ACM 인증서 ARN (Product B 입력)
- ACM 인증서 만료 알람 (CloudWatch + SNS, 30/14/7일 전 다단)
- `curl -I https://thirdtool.dev` 응답 샘플

## 연결된 Story 목록

- [ ]  Story 2-1. ALB 2개 + Target Group(type=ip) + Route53 + ACM 자동 갱신 + HTTPS 리다이렉트 구성 (4 SP)

## 내부 메모 / 제약 사항

- 도메인이 이미 보유한 경우 → Route53 hosted zone 생성 후 NS 레코드를 기존 도메인 등록자에 위임. 도메인 자체 이관은 v2
- ACM 인증서는 ap-northeast-2 리전에 발급 (CloudFront 사용 시 us-east-1 필요 — 본 Product 범위 외)
- ALB 비용: prod + staging 2개 → 시간당 $0.054 × 2 + LCU. 토이 규모 월 ~$40. 단일 ALB + path-based routing으로 절약 가능하지만 prod·staging 격리 우선
- staging ALB 자체를 만들지 않고 prod ALB의 별도 Listener Rule(host header `staging.*`)로 분기하는 안도 가능 — 단, prod·staging 트래픽 혼선 + WAF 별도 적용 불가. 분리 채택

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

### ALB vs NLB vs API Gateway

**Option A — NLB (Network Load Balancer, L4)**
- 장점: 초당 수백만 요청 처리 + 정적 IP + TLS passthrough
- 거부 이유:
    - HTTP 헤더 기반 라우팅 불가 → host-header로 staging 분기 불가
    - 헬스체크가 TCP 단위 → `/actuator/health` 응답 코드 검사 불가
    - TLS 종단을 ECS Task에서 처리해야 함 → 인증서 분산

**Option B — API Gateway (REST 또는 HTTP API)**
- 장점: 인증·rate-limit·캐싱 통합. 관리형
- 거부 이유:
    - 가격 모델이 요청당 과금 → 트래픽 증가 시 ALB 대비 비쌀 수 있음
    - WebSocket · gRPC 등 추후 확장 시 제약
    - JWT 검증을 API Gateway에서 처리하려면 Lambda authorizer 추가 → 복잡도↑

**Option C (선택) — ALB (Application Load Balancer, L7)**
- 비용: prod + staging 2개 = 월 ~$40
- 보상: HTTP host/path 라우팅 + 헬스체크 + TLS 종단 + ECS Fargate 호환

### Listener Rule 구조 — prod ALB · staging ALB 분리 vs 단일 ALB host-header 분기

**Option A — 단일 ALB + host-header로 prod·staging 분기**
- 장점: ALB 1개 = 비용 절반
- 거부 이유:
    - prod·staging 트래픽이 같은 ALB의 LCU를 공유 → staging 부하 테스트가 prod LCU 소비
    - WAF 적용 시 prod에는 strict / staging에는 lenient 같은 분리 불가
    - Target Group은 각각이지만 ALB가 단일 → 장애 영향 전파

**Option B (선택) — prod-alb / staging-alb 분리**
- 비용: 시간당 $0.054 × 2 + LCU = 월 ~$40
- 보상: 완전 격리 + WAF · 알람 환경별 분리 + load-test 안전

### Target Group 헬스체크 경로

**Option A — `/` (루트)**
- 거부 이유: SPA 정적 자산이 200 응답하므로 ECS Task가 죽어도 healthy 판정 가능 (FE만 살아 있는 경우)

**Option B — 별도 `/health` 엔드포인트 직접 구현**
- 거부 이유: Spring Boot Actuator가 이미 표준 제공. 중복 구현 불필요

**Option C (선택) — `/actuator/health`**
- 비용: Actuator 노출 정책 동기화 필요 (`product-op.md` Story 1-1)
- 보상: DB · Disk · Custom indicator 자동 포함 + Spring 표준 + 노출 정책 1곳 관리

### HTTPS 강제 방식

**Option A — ECS Task에서 HTTP→HTTPS 리다이렉트 처리 (Spring Security)**
- 거부 이유:
    - 모든 ECS Task가 인증서 보유 → 인증서 분산 + 갱신 부담
    - ALB는 평문 통과만 → ALB 이점 절반 상실

**Option B (선택) — ALB Listener Rule로 80 → 443 redirect 301**
- 비용: ALB 80 listener도 LCU 소비 (미미)
- 보상: 인증서 ACM 단일 진입점 + ECS Task는 평문 8080만 처리

### Route53 레코드 타입

**Option A — CNAME으로 ALB DNS 가리키기**
- 거부 이유: apex 도메인(`thirdtool.dev`)은 CNAME 불가 (RFC 위반)

**Option B (선택) — A record + Alias (ALB)**
- 비용: 특별한 비용 없음
- 보상: apex · subdomain 모두 동일 패턴 + Alias 쿼리는 Route53 내부 처리로 무료

### ACM 인증서 SAN 구성

**Option A — `thirdtool.dev` + `staging.thirdtool.dev` 명시 SAN**
- 장점: 인증서가 정확히 사용하는 도메인만 커버
- 거부 이유: 향후 `api.thirdtool.dev`, `admin.thirdtool.dev` 추가 시마다 재발급

**Option B (선택) — `thirdtool.dev` + `*.thirdtool.dev` 와일드카드**
- 비용: 와일드카드는 1단계 subdomain만 (`a.b.thirdtool.dev` 불가)
- 보상: 1회 발급 + 모든 1단계 subdomain 자동 커버

---

## Story 2-1. ALB 2개 + Target Group(type=ip) + Route53 + ACM 자동 갱신 + HTTPS 리다이렉트

### User Story

> As a 백엔드 개발자,
I want `thirdtool.dev`와 `staging.thirdtool.dev`로 들어온 모든 트래픽이 ALB를 통해 HTTPS로 강제되고 ECS Task로 라우팅되길,
So that 인증서·도메인 관리가 ACM·Route53 단일 진입점으로 자동화되고, ECS Task는 평문 HTTP만 처리하면 된다.
>

### 설계 노트

- ALB 구성 (prod 기준, staging도 동일 패턴)

    ```
    Name: thirdtool-prod-alb
    Scheme: internet-facing
    Subnets: public-2a, public-2c
    Security Group: alb-sg
    Listeners:
      80  HTTP  → redirect https:443 (301)
      443 HTTPS → ACM cert + forward to target-group
    ```

- Target Group

    ```
    Name: thirdtool-prod-tg
    Target Type: ip
    Protocol: HTTP / Port: 8080
    Health Check: GET /actuator/health, 30s interval, 5s timeout
    Healthy threshold: 2 / Unhealthy: 3
    Deregistration delay: 30s (배포 시 빠른 교체)
    ```

- Route53 레코드 (Terraform alias 사용)

    ```hcl
    resource "aws_route53_record" "apex" {
      zone_id = aws_route53_zone.main.zone_id
      name    = "thirdtool.dev"
      type    = "A"
      alias {
        name                   = aws_lb.prod.dns_name
        zone_id                = aws_lb.prod.zone_id
        evaluate_target_health = true
      }
    }
    ```

- ACM 발급 + DNS validation

    ```hcl
    resource "aws_acm_certificate" "main" {
      domain_name               = "thirdtool.dev"
      subject_alternative_names = ["*.thirdtool.dev"]
      validation_method         = "DNS"
      lifecycle { create_before_destroy = true }
    }
    resource "aws_acm_certificate_validation" "main" {
      certificate_arn         = aws_acm_certificate.main.arn
      validation_record_fqdns = [for r in aws_route53_record.validation : r.fqdn]
    }
    ```

- 만료 알람

    ```hcl
    resource "aws_cloudwatch_metric_alarm" "cert_expiry_30d" {
      metric_name         = "DaysToExpiry"
      namespace           = "AWS/CertificateManager"
      threshold           = 30
      comparison_operator = "LessThanThreshold"
      ...
    }
    ```

### 완료 기준 (Acceptance Criteria)

- [ ]  `curl -I https://thirdtool.dev`가 200 OK + valid certificate를 응답한다 (ECS Service 등록 후)
- [ ]  `curl -I http://thirdtool.dev`가 301 Moved Permanently + `Location: https://thirdtool.dev/`를 응답한다
- [ ]  `curl -I https://staging.thirdtool.dev`가 동일하게 동작한다 (staging ALB)
- [ ]  ACM 인증서가 `Status: ISSUED` 상태 + `RenewalEligibility: ELIGIBLE`이다
- [ ]  Target Group 헬스체크가 ECS Service 등록 전에는 `unhealthy` (정상 — target 0개), 등록 후 `healthy`로 전환된다
- [ ]  CloudWatch 알람 3종(인증서 30/14/7일 전)이 생성되고 SNS 토픽 구독된다

### 엣지 케이스

- ACM 발급 진행 중 (DNS 전파 5~30분) → ALB Listener 생성이 실패 → 발급 완료 후 Listener 생성. Terraform `depends_on`으로 강제
- 도메인 등록자의 NS가 Route53로 위임되지 않은 상태 → DNS validation 영구 대기. Runbook에 NS 위임 절차 명시
- staging 호스트 헤더 `staging.thirdtool.dev`가 prod ALB로 잘못 도달 시 → prod ALB는 인증서 SAN에 `staging.*` 포함하지만 Target Group은 prod ECS만 → 응답이 prod에서 옴. host-header rule 추가 또는 staging ALB 분리 (분리 채택)
- HTTP/2 또는 HTTP/3 — ALB 기본 HTTP/2 활성, HTTP/3는 v2

### Definition of Done

- [ ]  코드 리뷰 완료 (수동 구성 OK, Product C에서 Terraform import)
- [ ]  ALB DNS · Route53 레코드 · ACM ARN을 Product B 입력으로 문서화 (`docs/architecture/vpc-topology.md`)
- [ ]  `curl -I` 응답 샘플 스크린샷 첨부
- [ ]  ACM 자동 갱신 활성 확인 스크린샷 첨부
- [ ]  PO(또는 본인) 셀프 검수 완료

### 의존성

- 선행: Story 1-1 (public subnet · alb-sg)
- 후속: Story 3-1 (RDS endpoint), Product B (Target Group에 ECS Service 등록)

### 스토리 포인트

- 추정: 4 SP

---

## Epic 3. RDS MySQL — prod Multi-AZ + staging single-AZ + 백업 + 파라미터 그룹

# Epic 3. RDS MySQL — prod Multi-AZ + staging single-AZ + 백업 + 파라미터 그룹

## Epic 목표

> RDS MySQL 8.0 인스턴스 2개(prod Multi-AZ + staging single-AZ)를 data subnet에 생성하고, 파라미터 그룹(timezone=UTC, charset=utf8mb4) · 서브넷 그룹(private) · 자동 백업 7일 표준을 적용한다.
Flyway 마이그레이션이 두 환경 모두에서 동일하게 동작하는 baseline 호환성을 검증한다.
>

## 배경

- v0 RDS는 손으로 띄워졌을 가능성 높고 single-AZ 추정 → 가용성·복구 시간이 운영 표준 미달
- 파라미터 그룹이 default일 가능성 → timezone이 UTC가 아닌 경우 도메인 시각 비교 코드(예: Card `lastViewedAt`)에 미세 버그 위험
- charset이 utf8(3바이트)인 경우 → 한글 이모지(예: 👋) 저장 실패. utf8mb4 필수
- staging RDS가 없는 상태 → load-test Product의 baseline 측정이 사실상 prod에서 진행되는 사고 가능성

## 핵심 설계 결정

> **prod는 Multi-AZ · db.t3.medium · 자동 백업 7일 · PITR 활성.**
가용성과 복구를 안전 영역에 둠.
>
> - Multi-AZ: 동기 복제 standby가 다른 AZ에 위치 → primary 장애 시 60초 내 자동 페일오버
> - db.t3.medium (2 vCPU / 4GB) — 토이 규모 적정. 부하 테스트 결과로 v2 재조정
> - 자동 백업 7일 + PITR(Point-In-Time Recovery): 7일 내 임의 시각으로 복구 가능
> - 스냅샷 월 1회 수동 백업 30일 보존 (Product C)

> **staging은 single-AZ · db.t3.small · 자동 백업 1일.**
비용 절감 + load-test 용도에 최소 구성.
>
> - single-AZ: prod와 동일 구조이나 standby 없음 — 장애 시 수동 복구
> - db.t3.small (2 vCPU / 2GB) — 워밍업·smoke test 충분 (load-test VU 50까지 검증)
> - 자동 백업 1일: 부하 테스트 후 데이터 폐기 가능 — 장기 보존 불요

> **파라미터 그룹은 prod·staging 동일 사용.**
환경 차이가 파라미터에서 발생하지 않게 함.
>
> - `time_zone=+00:00` (UTC)
> - `character_set_server=utf8mb4`, `collation_server=utf8mb4_unicode_ci`
> - `max_connections=동적` (인스턴스 클래스에 따라 자동) — 단, HikariCP `maximum-pool-size`보다 충분히 커야 함
> - 변경 시 인스턴스 재기동 필요한 파라미터는 staging에서 먼저 검증 후 prod 적용

## 완료 기준 (Definition of Done)

- [ ]  prod-rds (Multi-AZ · db.t3.medium · MySQL 8.0)가 data subnet에 생성된다
- [ ]  staging-rds (single-AZ · db.t3.small · MySQL 8.0)가 data subnet에 생성된다
- [ ]  파라미터 그룹 `thirdtool-mysql-80`이 적용되고 timezone=UTC · charset=utf8mb4가 확인된다
- [ ]  서브넷 그룹이 data subnet 2개(2a, 2c)로 구성된다 (RDS는 Multi-AZ 요건상 최소 2 subnet 필요)
- [ ]  prod 자동 백업 7일 · staging 자동 백업 1일이 활성화된다
- [ ]  Flyway 마이그레이션이 두 환경 모두에서 baseline 호환되어 실행된다
- [ ]  연결된 Story가 모두 Done 상태다

## 산출물 (Deliverables)

- RDS endpoint 2개 (prod · staging) → application.yml + Product C Secrets Manager
- 파라미터 그룹 설정 표 (`docs/architecture/rds-config.md`)
- Flyway baseline 검증 결과

## 연결된 Story 목록

- [ ]  Story 3-1. RDS MySQL 2개 (prod Multi-AZ + staging single-AZ) + 파라미터/서브넷 그룹 + 자동 백업 + Flyway 호환 검증 (5 SP)

## 내부 메모 / 제약 사항

- 비용: prod ~$140/월 + staging ~$30/월 = $170/월. db.t3.micro(burstable) 사용 시 -$50 가능하나 부하 테스트에서 신뢰성 떨어짐 → t3.small 이상 권장
- Multi-AZ 페일오버 테스트: prod 안정화 후 1회 수동 reboot with failover 실행하여 검증 + 페일오버 시간 측정
- RDS Performance Insights 무료 7일 보존 활성화 권장 — query 단위 성능 추적 (Product op·load-test와 시너지)
- 백업 윈도우: prod 03:00-04:00 UTC (KST 12:00-13:00 — 트래픽 가장 적은 시간 가정. 실제 패턴 측정 후 조정)

## Epic 기술 결정 / 대안 (Epic-Level Alternatives)

### DB 엔진 선택

**Option A — PostgreSQL**
- 장점: JSON 타입 강력 + JSONB 인덱싱 + 향상된 동시성
- 거부 이유:
    - 기존 코드가 MySQL 8.0 가정 (`application.yml` JDBC URL · Flyway `org.flywaydb:flyway-mysql`)
    - 마이그레이션 비용 (방언 차이 · DATETIME(6) vs TIMESTAMPTZ · AUTO_INCREMENT vs SERIAL) 과대
    - 기존 도메인 모델은 PostgreSQL 강점(JSON 컬럼)을 사용하지 않음

**Option B — Aurora MySQL**
- 장점: 자동 페일오버 < 30초 + storage auto-scaling + 더 나은 성능
- 거부 이유:
    - 비용: db.t3.medium MySQL ~$140 vs Aurora db.r6g.large ~$210 (Aurora는 micro/small 미지원)
    - 토이 규모에 과잉 + Multi-AZ MySQL로도 60초 페일오버 충분

**Option C (선택) — RDS MySQL 8.0**
- 비용: Aurora 대비 페일오버 30~60초 느림
- 보상: 기존 코드 호환 + 비용 적정 + Flyway 호환 검증 불요

### Storage 타입 (gp3 vs gp2 vs io1)

**Option A — gp2 (이전 세대 SSD)**
- 거부 이유: gp3 대비 IOPS · throughput 분리 불가, 같은 가격에 성능 낮음

**Option B — io1 (Provisioned IOPS)**
- 거부 이유: IOPS당 별도 과금. 토이 규모에서 baseline IOPS 미달

**Option C (선택) — gp3**
- 비용: GB당 $0.115/월 + baseline 3000 IOPS · 125 MB/s 무료
- 보상: 비용 효율 + IOPS · throughput 부족 시 별도 구매 가능

### Multi-AZ 페일오버 방식

**Option A — Read Replica (비동기 복제)**
- 거부 이유:
    - 비동기 복제 → 데이터 손실 가능 (RPO > 0)
    - 페일오버 수동 + DNS 갱신 필요

**Option B — Aurora Multi-Master**
- 거부 이유: Aurora 의존 + 비용 과다

**Option C (선택) — RDS Multi-AZ (동기 복제 standby)**
- 비용: standby 인스턴스 비용 = primary와 동일 (월 +$70)
- 보상: RPO 0 + 자동 페일오버 30~60초 + endpoint DNS 자동 갱신

### Connection Pool 사이즈 결정

**Option A — HikariCP `maximum-pool-size: 30` 일률 적용**
- 거부 이유:
    - prod·staging 인스턴스 클래스 다름 (medium 4GB / small 2GB)
    - small 인스턴스 `max_connections` 자동값이 ~85 → pool 30개 × ECS Task N개 시 한도 초과 가능

**Option B (선택) — 환경별 차등 (prod 10 / staging 5)**
- 비용: 부하 테스트로 검증 필요 (Product load-test와 합류)
- 보상: 인스턴스 클래스 정합 + connection 부족 시 ECS Task 수직 확장보다 pool 증설이 단순

### Flyway 마이그레이션 strict vs out-of-order

**Option A — 모든 환경 strict (default)**
- 장점: 마이그레이션 순서 강제 → 환경 간 schema 일관성
- 거부 이유:
    - 여러 브랜치에서 동시 V 버전 추가 시 충돌 → 한 쪽 재명명 필요 (개발 마찰)

**Option B (선택) — staging `outOfOrder=true` 허용 + prod strict**
- 비용: staging이 prod와 미세하게 다른 마이그레이션 순서 → 재현 시 주의
- 보상: 개발 마찰 감소 + prod는 안전성 확보

### 백업 보존 기간 (prod 7일 vs 14일 vs 30일)

**Option A — 14일 또는 30일**
- 장점: 더 긴 PITR window
- 거부 이유:
    - 백업 스토리지 비용 증가 (RDS allocated storage 초과분 GB당 $0.095/월)
    - 토이 규모 + 사용자 0명 → 14일 전 데이터 복구 가치 측정 불가

**Option B (선택) — prod 7일 + 월 1회 수동 스냅샷 30일 보존**
- 비용: 스냅샷 30일 보존 분량만 추가 부담
- 보상: 7일 내 정밀 복구 + 30일 내 거친 복구 둘 다 보유

---

## Story 3-1. RDS MySQL 2개 + 파라미터/서브넷 그룹 + 자동 백업 + Flyway 호환 검증

### User Story

> As a 백엔드 개발자,
I want prod·staging RDS가 비대칭으로 분리되고 파라미터·백업·Flyway 마이그레이션이 두 환경 모두 표준대로 동작하길,
So that 도메인 데이터의 영속성·복구성이 운영 표준에 도달하고, 부하 테스트가 실제 staging에서 안전하게 실행될 수 있다.
>

### 설계 노트

- 인스턴스 구성

    ```
    prod-rds:
      engine: mysql 8.0
      instance_class: db.t3.medium
      storage: 100GB gp3 (autoscaling up to 500GB)
      multi_az: true
      backup_retention_period: 7
      preferred_backup_window: 03:00-04:00
      deletion_protection: true

    staging-rds:
      engine: mysql 8.0
      instance_class: db.t3.small
      storage: 50GB gp3
      multi_az: false
      backup_retention_period: 1
      deletion_protection: false
    ```

- 파라미터 그룹 `thirdtool-mysql-80`

    | parameter | value |
    | --- | --- |
    | `time_zone` | `+00:00` |
    | `character_set_server` | `utf8mb4` |
    | `collation_server` | `utf8mb4_unicode_ci` |
    | `slow_query_log` | `1` |
    | `long_query_time` | `1` (1초 초과 쿼리 로깅) |

- 서브넷 그룹 `thirdtool-data-subnets`: `data-2a`, `data-2c`
- 보안 그룹: `db-sg` (Epic 1 산출물, app-sg에서 3306만 허용)
- Flyway baseline 검증
    - dev(H2)에서 작동하는 마이그레이션이 staging-rds에서도 적용되는지 `./gradlew bootRun --args='--spring.profiles.active=staging'`로 검증
    - prod 적용 전 staging에서 dry-run

- application.yml 환경 분기

    ```yaml
    spring:
      profiles: prod
      datasource:
        url: jdbc:mysql://${RDS_PROD_ENDPOINT}:3306/thirdtool?serverTimezone=UTC&useUnicode=true&characterEncoding=utf8mb4
        username: ${DB_USER}
        password: ${DB_PASSWORD}
        hikari:
          maximum-pool-size: 10
    ```

### 완료 기준 (Acceptance Criteria)

- [ ]  `aws rds describe-db-instances`로 prod-rds(Multi-AZ=true) · staging-rds(Multi-AZ=false)가 확인된다
- [ ]  파라미터 그룹 `thirdtool-mysql-80`이 두 인스턴스에 attach되어 있고 timezone=UTC + charset=utf8mb4가 적용된다
- [ ]  prod 자동 백업 7일 · staging 1일이 설정된다
- [ ]  prod에는 `deletion_protection=true` 설정으로 실수 삭제가 차단된다
- [ ]  RDS endpoint(prod·staging)가 ECS Task의 `app-sg` SG에서만 3306으로 접근 가능하다 (`db-sg` inbound가 `app-sg` 외 deny)
- [ ]  Flyway 마이그레이션이 staging-rds에서 baseline부터 모두 통과한다 (`./gradlew flywayMigrate -Pspring.profiles.active=staging`)
- [ ]  Performance Insights가 두 인스턴스 모두 활성화된다 (7일 무료)

### 엣지 케이스

- Multi-AZ 인스턴스 페일오버 시 endpoint는 동일 (DNS가 standby로 가리킴) → 애플리케이션 재기동 불요. HikariCP의 connection validation으로 stale connection 자동 폐기
- 한글 이모지(👋, 🔗) 데이터가 utf8(3바이트)로 저장 시도 시 `1366 Incorrect string value` 에러 — utf8mb4(4바이트)로 전환되었으므로 정상 저장
- `time_zone=+00:00` 적용 후 기존 데이터의 `created_at` 필드가 그대로 UTC로 해석 → JPA `@CreationTimestamp` 동작과 일치하는지 확인
- staging-rds 부하 테스트 중 storage burst 한도 도달 시 → `iops` 일시 저하. 부하 테스트 시간(20분)에는 gp3 baseline 충분
- Flyway out-of-order 마이그레이션 발생 시 → `flyway.outOfOrder=true`로 허용 (staging만), prod는 엄격 적용

### Definition of Done

- [ ]  코드 리뷰 완료
- [ ]  prod·staging RDS 생성 확인 스크린샷
- [ ]  Flyway 마이그레이션 로그 (staging baseline 통과) 첨부
- [ ]  파라미터 그룹 설정 스크린샷 (timezone + charset)
- [ ]  application.yml 환경 분기 PR 머지
- [ ]  Multi-AZ 페일오버 1회 수동 테스트 (prod 안정화 후) — 페일오버 시간 측정
- [ ]  PO(또는 본인) 셀프 검수 완료

### 의존성

- 선행: Story 1-1 (data subnet · db-sg)
- 후속: Product C (Secrets Manager에 DB credential 이관), Product B (ECS Task의 app-sg에서 db-sg 호출 검증)

### 스토리 포인트

- 추정: 5 SP

---

## Product 요약

| Epic | Story 수 | SP 합계 |
| --- | --- | --- |
| Epic 1. VPC 토대 | 1 | 3 |
| Epic 2. ALB + 도메인 + TLS | 1 | 4 |
| Epic 3. RDS MySQL | 1 | 5 |
| **합계** | **3** | **12 SP** |

**진행 순서 (필수):** Epic 1 → 2 → 3. VPC 없이 ALB·RDS 배치 불가.

**Product B · C와의 연결 포인트**
- Story 1-1의 subnet ID · SG ID → Product B의 ECS Task 배치 + Product C의 Terraform import
- Story 2-1의 ALB Target Group ARN → Product B의 ECS Service `loadBalancers` 설정 입력
- Story 3-1의 RDS endpoint → Product C의 Secrets Manager에 DB credential 등록 + 도메인 Product application.yml

**기존 운영성 Product와의 연결 포인트**
- `product-load-test.md`의 ADR-LOAD-002("프로덕션 금지") 전제가 Story 3-1 staging-rds 생성으로 비로소 충족
- `product-op.md`의 Grafana 대시보드는 본 Product 완료 후 ALB 5xx · RDS connections 메트릭으로 풍부해짐
- `product-auth.md`의 OAuth 콜백 URL이 `https://thirdtool.dev/login/oauth2/code/{provider}`로 고정 → application-prod.yml 갱신
