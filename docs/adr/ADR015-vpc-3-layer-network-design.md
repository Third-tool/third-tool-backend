# ADR015: VPC 10.0.0.0/16 + 2 AZ × 3-layer subnet + 단일 NAT Gateway (Story-048)

- **상태**: Accepted
- **날짜**: 2026-06-29
- **관련**: Product 6 AWS 네트워크 Epic 1 (VPC 구축) · milestone 0.0.1v item #12 (`workflow/task/milestones/version/0.0.1v/milestone.md`) · `docs/architecture/vpc-topology.md` · `docs/operations/troubleshooting/ts009-vpc-setup.md` · `infra/vpc/vpc-spec.json` · `infra/vpc/security-groups.json`

## 컨텍스트

milestone 0.0.1v 의존 chain에서 **VPC가 가장 큰 unblocker**다. #13 ALB·#14 RDS·#11 ECS Service(Story-047 `service-prod.json`의 `<APP_SUBNET_*>`/`<APP_SG>` placeholder) 모두 VPC 자원 ID 선행 필요. ThirdTool은 portfolio 프로젝트로 트래픽 0명 상태이지만, 1인 운영자가 다음을 만족하는 네트워크 토폴로지를 정의해야 한다:

- **보안 경계 명확화**: RDS가 인터넷에서 직접 접근 불가, ECS Task가 ALB 외 inbound 차단, 각 layer 간 트래픽이 SG로 제어
- **multi-AZ 분산 가능성**: Fargate Task가 2 AZ에 분산되어 AZ 장애 시 부분 가용성 유지
- **IP 주소 여유**: 향후 신규 service 추가(예: ElastiCache, Bastion, VPC Endpoint) 시 CIDR 재설계 불요
- **비용 최소화**: M1 트래픽 미발생 단계에서 NAT Gateway 과도 운영 방지

기존 dev-cicd.yml은 EC2 단일 인스턴스 + default VPC에서 운영 중 — 보안 경계 부재, 향후 ECS Fargate 전환 시 재현 불가. 본 ADR이 새 VPC의 1차 설계 결정을 기록한다.

## 결정

**VPC `10.0.0.0/16` + 2 AZ(ap-northeast-2a/2c) × 3 layer(public/app/data) = 6 subnet + IGW 1 + NAT Gateway 1(public-2a 단일) + 5종 SG**.

### 핵심 구성

| 항목 | 결정 | 근거 |
| --- | --- | --- |
| Region | `ap-northeast-2` (서울) | 기존 dev-cicd / S3 / ECR 모두 동일 region. 한국 사용자 latency |
| VPC CIDR | `10.0.0.0/16` (65,536 IP) | 향후 service 확장 여유. /20보다 큰 표준 |
| AZ 수 | 2 (2a, 2c) | 서울 region 사용 가능 AZ — 2a/2c/2d 중 가장 안정 2개. 최소 multi-AZ 구성 |
| Subnet | 3 layer × 2 AZ = 6개 | layer 분리로 보안 경계 명확. /24(251 호스트)로 충분 |
| public subnet (`10.0.0.0/24`, `10.0.1.0/24`) | ALB target + NAT Gateway | 인터넷 직접 노출 자원만 |
| app subnet (`10.0.10.0/24`, `10.0.11.0/24`) | ECS Fargate Task | 외부 inbound 차단, NAT 경유 outbound |
| data subnet (`10.0.20.0/24`, `10.0.21.0/24`) | RDS MySQL | 외부·ALB 어디서도 직접 접근 불가 |
| Internet Gateway | 1개 | VPC 표준 |
| **NAT Gateway** | **1개 (public-2a 단일)** | multi-AZ NAT 비용 대비 M1 가용성 요구 낮음. **trade-off**: 2a 장애 시 private 트래픽 전체 차단 |
| Route Table | 2개 (public-rt + private-rt) | layer 분리에 정합 |
| Security Group | 5종 명확 분리 (alb/app/db/bastion/vpc-endpoint) | 참조 체인 alb→app→db로 데이터 흐름 일치 |

### SG 참조 체인 (단방향)

```
alb-sg          (인터넷 → 80/443)
  ↓ 참조
app-sg          (alb-sg → 8080)
  ↓ 참조
db-sg           (app-sg → 3306, egress 없음)

bastion-sg      (inbound 없음 — SSM Session Manager 채택)
vpc-endpoint-sg (app-sg → 443, M2 VPC Endpoint 대비)
```

순방향 참조만 — 순환 없음. RDS는 ALB·인터넷 어디서도 직접 접근 불가.

### 본 ADR이 다루지 않는 범위

- **Terraform IaC 모듈화**: 본 ADR은 `infra/vpc/*.json` spec + ts009 콘솔/CLI 셋업 가정. Terraform 모듈은 M2 Product 7 Epic 2
- **VPC Endpoint 실제 생성**: 본 ADR은 `vpc-endpoint-sg` 정의만 (M2 대비). 4종 endpoint(s3·ecr·secretsmanager·logs) 실제 생성은 NAT 비용 임계 도달 시
- **VPC Flow Logs**: 네트워크 관측성 별도 Story (보안 사고 추적 필요 시점)
- **Network ACL 커스터마이즈**: default(allow all) 유지 — SG로 차단
- **staging 전용 VPC 분리**: 현재 prod/staging 동일 VPC 공유. 환경 분리 Epic
- **Bastion EC2 실제 생성**: SG만 정의. SSM Session Manager 채택 — 인스턴스 자체는 운영 진입 필요 시점에 별도 Story

## 결과 (Consequences)

### 긍정적

- **보안 경계 명확**: RDS가 외부·ALB에서 직접 접근 불가. 침해 시 폭발 반경 한정. layer 별 SG 분리로 침해 1점이 다른 layer로 확산 차단
- **multi-AZ 분산 즉시 가능**: ECS Service `availabilityZoneRebalancing: ENABLED`(Story-047)가 2a/2c에 Task 자동 분산. RDS도 multi-AZ 전환 시 data-2c 즉시 활용
- **CIDR 여유**: `/16` 65K IP 중 1.5K만 사용 — 향후 service 4-5배 확장 가능. peering 시 conflict 회피 여유 (10.x.x.x private는 흔하지만 우리 /16 한 블록만 사용)
- **single source of truth**: `vpc-spec.json` + `security-groups.json`이 단일 진실 소스. ts009 runbook이 그대로 재현. M2 Terraform 모듈화 시 입력으로 활용
- **SSH 키 폐기**: Bastion이 SSM 채택 → `bastion-sg` inbound 없음. SSH 키 관리·회전 불필요

### 트레이드오프 / 부정적

- **단일 NAT Gateway 장애점**: public-2a 장애 시 private subnet 4개 모두 outbound 트래픽 차단(ECR pull·Secrets Manager·외부 API 모두 실패). M1 트래픽 0명이라 수용. M2 multi-AZ NAT(`Story-TBD`) 전환 시점은 트래픽 발생 후
- **NAT Gateway 데이터 전송 비용**: 모든 private outbound 트래픽이 NAT 경유 → $0.045/GB. VPC Endpoint 미구축으로 AWS API 호출도 NAT 경유. M2 Endpoint 도입 시 절감
- **layer 분리 운영 부담**: 6 subnet + 5 SG + 2 route table을 사용자가 수동 생성(ts009). drift 위험 — 콘솔에서 수동 변경 시 spec과 불일치. Terraform 도입 전까지 ts009 절차 엄수 필요
- **2a 편향**: NAT가 public-2a 단독이라 트래픽 패턴이 2a 편중. 2a CloudWatch 메트릭이 다른 AZ보다 항상 높음 — 모니터링 알림 임계 조정 필요
- **bastion EC2 부재 — 디버깅 도구 부족**: SSM Session Manager는 IAM 권한 기반이지만 SSM 미설치 환경에서 즉시 접근 불가. ECS Exec 가능 시점까지는 운영 진입 채널 부족

## 대안 비교

| 대안 | 장점 | 거부 사유 |
| --- | --- | --- |
| **A. Default VPC 그대로 사용** | 셋업 0, 즉시 사용 | 보안 경계 부재 (모든 subnet이 public). RDS 외부 노출 위험. layer 개념 없음 — 침해 시 폭발 반경 최대 |
| **B. 1-layer subnet (public만)** | subnet 2개로 간소 | RDS·ECS Task가 인터넷 직접 노출. 보안 경계 부재 |
| **C. 2-layer subnet (public + private)** | subnet 4개. 보안 적정 | ECS Task와 RDS가 동일 private subnet. SG로만 격리 — layer 명시성 약함. data 전용 격리 의도 표현 불가 |
| **D (선택). 3-layer subnet (public/app/data) + 단일 NAT** | layer 경계 명확, SG 참조 체인 + 라우팅이 일치, M2 확장 가능 | 단일 NAT 장애점 (수용 — M1 가용성 요구 낮음). 6 subnet 셋업 부담 (ts009로 표준화) |
| **E. 4+ layer subnet (예: public/app/data/cache 분리)** | 더 세밀한 경계 | 현재 cache(Redis) 미사용. 미래에 도입 시 별도 subnet 추가 가능 — 과도한 선제 분리 |
| **F. Multi-AZ NAT Gateway (NAT 2개)** | 2a 장애 시에도 private 트래픽 유지 | NAT 시간 비용 2배 ($0.06/시간 × 2). M1 가용성 요구가 비용 대비 작음. M2 트래픽 발생 시 전환 |
| **G. VPC Endpoint 즉시 도입** | NAT 데이터 전송 비용 절감 (특히 ECR pull) | Interface Endpoint 시간 비용 ($0.014/시간 × 4 endpoint = $0.056/시간). M1 트래픽 낮아 NAT 데이터 전송도 작음 — break-even 미달. M2에서 트래픽 측정 후 결정 |

## 알려진 follow-up (본 ADR 범위 외)

- **multi-AZ NAT Gateway 전환** (Story-TBD): public-2c에 NAT 추가 + `private-rt` 2개로 분리(2a용·2c용). 트래픽 발생 후 가용성 임계 도달 시
- **VPC Interface Endpoint 4종 도입** (Story-TBD): s3·ecr·secretsmanager·logs. `vpc-endpoint-sg`가 본 ADR에서 미리 정의 — 그대로 활용. NAT 데이터 전송 비용 임계 도달 시
- **VPC Flow Logs 활성** (Story-TBD): CloudWatch Logs 송신. 보안 감사·DDoS 탐지·트래픽 분석 필요 시
- **Terraform IaC 모듈화** (M2 Product 7 Epic 2): 본 `vpc-spec.json` + `security-groups.json`을 Terraform 모듈 입력으로 변환. drift 차단 + 환경별 재현
- **staging 전용 VPC 분리** (환경 분리 Epic): 현재 prod/staging 동일 VPC 공유. cross-environment 격리 필요 시
- **Bastion EC2 인스턴스 실제 생성** (Story-TBD): `bastion-sg`만 정의됨. 운영 진입 채널 필요 시점에 SSM 활성 인스턴스 1대 띄움
- **Network ACL 강화**: default(allow all) 유지 중. layer 간 격리 추가 필요 시
- **IPv6 CIDR 추가** (M3+): 현재 IPv4 only. IPv6 트래픽 발생 시점에 검토

## 다시 검토할 시점

- **트래픽 발생 시점**: NAT 데이터 전송량 측정 → VPC Endpoint 도입 또는 multi-AZ NAT 결정
- **AZ 장애 발생 시점**: 단일 NAT 장애점이 실제 영향 — multi-AZ 즉시 전환
- **service 추가 시점**: ElastiCache·OpenSearch 등 신규 layer 도입 검토 (subnet 추가 또는 기존 layer 재사용)
- **peering 도입 시점**: 다른 VPC와 연결 시 CIDR 충돌 확인 (`10.0.0.0/16` 다른 VPC에서 사용 중이면 재설계)
- **multi-region 진입 시점**: ap-northeast-1(Tokyo) DR 리전 도입 시 IP 대역 분리 (예: `10.1.0.0/16`)
