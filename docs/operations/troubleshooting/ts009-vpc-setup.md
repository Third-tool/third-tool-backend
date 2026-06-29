# ts009: VPC 1회성 셋업·검증·트러블슈팅

Story-048로 `infra/vpc/`에 VPC 10.0.0.0/16 + 6 subnet + 5 SG + IGW + NAT + 2 route table의 단일 진실 소스 spec JSON과 토폴로지 다이어그램이 추가됐다. 본 가이드는 AWS 콘솔(또는 CLI)에서 1회성 셋업하는 절차 + 검증 + 자주 보는 에러 6건을 한 곳에 모았다.

ts007(OIDC)·ts008(ECS)과 cross-link. 본 절차는 1인 운영자가 1회만 실행.

---

## 1. 전제

- AWS 콘솔 VPC 관리 권한 (`ec2:CreateVpc`, `ec2:CreateSubnet`, `ec2:CreateInternetGateway`, `ec2:CreateNatGateway`, `ec2:CreateRouteTable`, `ec2:CreateSecurityGroup`)
- AWS CLI 또는 콘솔. 본 가이드는 CLI 명령 위주 (재현성 + 자동화 진입점)
- region 고정: **ap-northeast-2** (모든 명령에 `--region ap-northeast-2`)
- 결제: NAT Gateway 시간 + 데이터 전송 비용 발생 (~$0.06/시간 + $0.045/GB). 셋업 후 즉시 과금 시작 — cost.md 추적
- **본 셋업은 single-AZ NAT** (public-2a). 2a 장애 시 private 트래픽 차단 — 수용된 trade-off (ADR015)

---

## 2. VPC 생성

```bash
aws ec2 create-vpc \
  --cidr-block 10.0.0.0/16 \
  --tag-specifications 'ResourceType=vpc,Tags=[{Key=Name,Value=thirdtool-vpc},{Key=Project,Value=ThirdTool},{Key=Environment,Value=shared},{Key=ManagedBy,Value=Story-048}]' \
  --region ap-northeast-2
```

응답 `Vpc.VpcId`(`vpc-xxxxxxxx`) 메모.

DNS hostname + DNS resolution 활성화 (RDS 내부 도메인 해석 필수):
```bash
VPC_ID=vpc-xxxxxxxx
aws ec2 modify-vpc-attribute --vpc-id $VPC_ID --enable-dns-support  --region ap-northeast-2
aws ec2 modify-vpc-attribute --vpc-id $VPC_ID --enable-dns-hostnames --region ap-northeast-2
```

---

## 3. 6 Subnet 생성

각 subnet 명령. `<VPC_ID>` 치환 + `--map-public-ip-on-launch` (public만):

```bash
# public-2a
aws ec2 create-subnet --vpc-id $VPC_ID --cidr-block 10.0.0.0/24 --availability-zone ap-northeast-2a \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=public-2a},{Key=Layer,Value=public}]' \
  --region ap-northeast-2

# public-2c
aws ec2 create-subnet --vpc-id $VPC_ID --cidr-block 10.0.1.0/24 --availability-zone ap-northeast-2c \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=public-2c},{Key=Layer,Value=public}]' \
  --region ap-northeast-2

# app-2a
aws ec2 create-subnet --vpc-id $VPC_ID --cidr-block 10.0.10.0/24 --availability-zone ap-northeast-2a \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=app-2a},{Key=Layer,Value=app}]' \
  --region ap-northeast-2

# app-2c
aws ec2 create-subnet --vpc-id $VPC_ID --cidr-block 10.0.11.0/24 --availability-zone ap-northeast-2c \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=app-2c},{Key=Layer,Value=app}]' \
  --region ap-northeast-2

# data-2a
aws ec2 create-subnet --vpc-id $VPC_ID --cidr-block 10.0.20.0/24 --availability-zone ap-northeast-2a \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=data-2a},{Key=Layer,Value=data}]' \
  --region ap-northeast-2

# data-2c
aws ec2 create-subnet --vpc-id $VPC_ID --cidr-block 10.0.21.0/24 --availability-zone ap-northeast-2c \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=data-2c},{Key=Layer,Value=data}]' \
  --region ap-northeast-2
```

각 응답 `Subnet.SubnetId` 메모 (총 6개).

public 2개에 `mapPublicIpOnLaunch=true` 설정:
```bash
PUB_2A=subnet-xxxxxxxx  # public-2a 결과
PUB_2C=subnet-xxxxxxxx  # public-2c 결과
aws ec2 modify-subnet-attribute --subnet-id $PUB_2A --map-public-ip-on-launch --region ap-northeast-2
aws ec2 modify-subnet-attribute --subnet-id $PUB_2C --map-public-ip-on-launch --region ap-northeast-2
```

---

## 4. Internet Gateway 생성 + VPC 연결

```bash
IGW_ID=$(aws ec2 create-internet-gateway \
  --tag-specifications 'ResourceType=internet-gateway,Tags=[{Key=Name,Value=thirdtool-igw}]' \
  --region ap-northeast-2 --query 'InternetGateway.InternetGatewayId' --output text)

aws ec2 attach-internet-gateway --vpc-id $VPC_ID --internet-gateway-id $IGW_ID --region ap-northeast-2
```

> **IGW VPC attach 누락 시 ts009-4 발생**: public subnet에 0.0.0.0/0 route를 추가해도 인터넷 통신 불가. attach 명령 별도 실행 필수.

---

## 5. NAT Gateway 생성 (public-2a 단일)

EIP 할당 → NAT 생성 → 약 1-2분 PENDING:

```bash
EIP_ALLOC_ID=$(aws ec2 allocate-address --domain vpc --region ap-northeast-2 --query 'AllocationId' --output text)

NAT_ID=$(aws ec2 create-nat-gateway \
  --subnet-id $PUB_2A \
  --allocation-id $EIP_ALLOC_ID \
  --connectivity-type public \
  --tag-specifications 'ResourceType=natgateway,Tags=[{Key=Name,Value=thirdtool-nat}]' \
  --region ap-northeast-2 --query 'NatGateway.NatGatewayId' --output text)

# AVAILABLE 도달 대기
aws ec2 wait nat-gateway-available --nat-gateway-ids $NAT_ID --region ap-northeast-2
```

---

## 6. Route Table 2개 생성

```bash
# public-rt
PUB_RT=$(aws ec2 create-route-table --vpc-id $VPC_ID \
  --tag-specifications 'ResourceType=route-table,Tags=[{Key=Name,Value=public-rt}]' \
  --region ap-northeast-2 --query 'RouteTable.RouteTableId' --output text)
aws ec2 create-route --route-table-id $PUB_RT --destination-cidr-block 0.0.0.0/0 --gateway-id $IGW_ID --region ap-northeast-2
aws ec2 associate-route-table --route-table-id $PUB_RT --subnet-id $PUB_2A --region ap-northeast-2
aws ec2 associate-route-table --route-table-id $PUB_RT --subnet-id $PUB_2C --region ap-northeast-2

# private-rt (4 subnet 연결)
PRIV_RT=$(aws ec2 create-route-table --vpc-id $VPC_ID \
  --tag-specifications 'ResourceType=route-table,Tags=[{Key=Name,Value=private-rt}]' \
  --region ap-northeast-2 --query 'RouteTable.RouteTableId' --output text)
aws ec2 create-route --route-table-id $PRIV_RT --destination-cidr-block 0.0.0.0/0 --nat-gateway-id $NAT_ID --region ap-northeast-2
for SUBNET in $APP_2A $APP_2C $DATA_2A $DATA_2C; do
  aws ec2 associate-route-table --route-table-id $PRIV_RT --subnet-id $SUBNET --region ap-northeast-2
done
```

---

## 7. Security Group 5종 생성 (참조 순서 강제)

`creationOrder: ["alb-sg", "app-sg", "db-sg", "bastion-sg", "vpc-endpoint-sg"]` (`security-groups.json` 명시). 참조 SG가 먼저 존재해야 함.

```bash
# 7.1 alb-sg
ALB_SG=$(aws ec2 create-security-group --vpc-id $VPC_ID --group-name alb-sg --description "ALB internet-facing" --region ap-northeast-2 --query 'GroupId' --output text)
aws ec2 authorize-security-group-ingress --group-id $ALB_SG --protocol tcp --port 80  --cidr 0.0.0.0/0 --region ap-northeast-2
aws ec2 authorize-security-group-ingress --group-id $ALB_SG --protocol tcp --port 443 --cidr 0.0.0.0/0 --region ap-northeast-2

# 7.2 app-sg (alb-sg 참조)
APP_SG=$(aws ec2 create-security-group --vpc-id $VPC_ID --group-name app-sg --description "ECS Task — alb-sg only" --region ap-northeast-2 --query 'GroupId' --output text)
aws ec2 authorize-security-group-ingress --group-id $APP_SG --protocol tcp --port 8080 --source-group $ALB_SG --region ap-northeast-2

# 7.3 db-sg (app-sg 참조)
DB_SG=$(aws ec2 create-security-group --vpc-id $VPC_ID --group-name db-sg --description "RDS MySQL — app-sg only" --region ap-northeast-2 --query 'GroupId' --output text)
aws ec2 authorize-security-group-ingress --group-id $DB_SG --protocol tcp --port 3306 --source-group $APP_SG --region ap-northeast-2
aws ec2 revoke-security-group-egress --group-id $DB_SG --protocol -1 --port all --cidr 0.0.0.0/0 --region ap-northeast-2

# 7.4 bastion-sg (SSM Session Manager — inbound 없음)
BASTION_SG=$(aws ec2 create-security-group --vpc-id $VPC_ID --group-name bastion-sg --description "Bastion via SSM — no SSH inbound" --region ap-northeast-2 --query 'GroupId' --output text)

# 7.5 vpc-endpoint-sg (M2 대비)
VPCE_SG=$(aws ec2 create-security-group --vpc-id $VPC_ID --group-name vpc-endpoint-sg --description "VPC Interface Endpoints — app-sg only" --region ap-northeast-2 --query 'GroupId' --output text)
aws ec2 authorize-security-group-ingress --group-id $VPCE_SG --protocol tcp --port 443 --source-group $APP_SG --region ap-northeast-2
aws ec2 revoke-security-group-egress --group-id $VPCE_SG --protocol -1 --port all --cidr 0.0.0.0/0 --region ap-northeast-2
```

> **순환 참조 회피**: alb-sg는 어떤 SG도 참조 안 함 (인터넷에서 직접 진입). app-sg가 alb-sg 참조, db-sg가 app-sg 참조 → 단방향 체인. 순서 어기면 ts009-3 발생.

---

## 8. 검증 — 6 subnet + 5 SG ID 출력 (후속 Story placeholder 치환용)

```bash
echo "=== VPC ID ==="
echo "VPC_ID=$VPC_ID"

echo "=== Subnet IDs (Story-047 service-prod.json 치환용) ==="
echo "PUB_2A=$PUB_2A"
echo "PUB_2C=$PUB_2C"
echo "APP_2A=$APP_2A    # <APP_SUBNET_2A> 치환"
echo "APP_2C=$APP_2C    # <APP_SUBNET_2C> 치환"
echo "DATA_2A=$DATA_2A  # 후속 Story (#14 RDS) 치환용"
echo "DATA_2C=$DATA_2C"

echo "=== Security Group IDs ==="
echo "ALB_SG=$ALB_SG       # 후속 Story (#13 ALB) 치환용"
echo "APP_SG=$APP_SG       # <APP_SG> 치환"
echo "DB_SG=$DB_SG         # 후속 Story (#14 RDS) 치환용"
echo "BASTION_SG=$BASTION_SG"
echo "VPCE_SG=$VPCE_SG     # M2 VPC Endpoint 치환용"
```

각 ID를 안전한 곳(예: 1Password vault 또는 로컬 `infra/vpc/runtime-ids.local.txt`, **gitignore 필수**)에 저장. 후속 Story에서 placeholder 치환 시 사용.

라우팅 검증:
```bash
aws ec2 describe-route-tables --route-table-ids $PUB_RT $PRIV_RT --region ap-northeast-2 \
  | jq '.RouteTables[] | {Name: (.Tags[]|select(.Key=="Name").Value), Routes: .Routes[]}'
# public-rt에 0.0.0.0/0 → IGW, private-rt에 0.0.0.0/0 → NAT 확인
```

NAT 상태:
```bash
aws ec2 describe-nat-gateways --nat-gateway-ids $NAT_ID --region ap-northeast-2 | jq '.NatGateways[].State'
# 기대: "available"
```

---

## 9. 트러블슈팅

### ts009-1: NAT Gateway 생성 시 EIP 한도 초과 (`AddressLimitExceeded`)

**원인**: AWS 계정 기본 EIP 한도 5개. 다른 리전·서비스에서 이미 사용 중일 수 있음.

**해결**:
1. 기존 EIP 사용 현황 확인:
   ```bash
   aws ec2 describe-addresses --region ap-northeast-2 | jq '.Addresses[] | {AllocationId, AssociationId, Domain}'
   ```
2. 사용 안 하는 EIP가 있으면 release (`aws ec2 release-address --allocation-id eipalloc-xxxx`)
3. 또는 AWS Support → Service Quotas → "EC2-VPC Elastic IPs" 한도 증설 요청 (1-2일 소요)

### ts009-2: private subnet에서 outbound 트래픽 실패 (`Connection timed out`)

**원인 가능성**:
- private-rt에 `0.0.0.0/0 → NAT` 라우트 누락
- NAT Gateway가 PENDING 상태 (생성 직후 1-2분)
- private subnet이 private-rt에 연결 안 됨 (default rt 사용 중)

**해결**:
1. private-rt 라우트 확인:
   ```bash
   aws ec2 describe-route-tables --route-table-ids $PRIV_RT --region ap-northeast-2 | jq '.RouteTables[].Routes'
   ```
2. NAT 상태 확인 (§8)
3. subnet → route table 연결 확인:
   ```bash
   aws ec2 describe-route-tables --filters Name=association.subnet-id,Values=$APP_2A --region ap-northeast-2 | jq '.RouteTables[].Tags'
   ```

### ts009-3: SG 생성 시 순환 참조 (`InvalidGroup.NotFound`)

**원인**: app-sg 생성 시 alb-sg가 아직 미생성. 또는 db-sg 생성 시 app-sg가 미생성.

**해결**: §7 creationOrder 엄수 — alb-sg → app-sg → db-sg → bastion-sg → vpc-endpoint-sg 순서. 이미 잘못 생성했으면 SG 삭제 후 순서대로 재생성.

### ts009-4: IGW를 VPC에 attach 안 함 → public subnet 인터넷 통신 실패

**원인**: `create-internet-gateway`만 실행하고 `attach-internet-gateway` 누락. IGW가 어디에도 연결 안 된 상태.

**해결**:
```bash
aws ec2 describe-internet-gateways --internet-gateway-ids $IGW_ID --region ap-northeast-2 \
  | jq '.InternetGateways[].Attachments'
# 빈 배열이면 attach 누락
aws ec2 attach-internet-gateway --vpc-id $VPC_ID --internet-gateway-id $IGW_ID --region ap-northeast-2
```

### ts009-5: subnet AZ 오타로 인한 다중 AZ 분산 실패

**원인**: subnet 생성 시 `--availability-zone`을 `ap-northeast-2a`/`2c`가 아닌 다른 값(`2b`, `2d`)으로 입력. ap-northeast-2는 2a/2c/2d 사용 가능하지만 본 설계는 2a/2c만.

**해결**: 잘못 만든 subnet 삭제 후 재생성. ECS Service 생성 시 subnet 2개가 같은 AZ에 몰리면 Multi-AZ 분산 실패.

### ts009-7: VPC peering 도입 시 CIDR 충돌 (`InvalidVpcPeeringConnectionId.Malformed` 또는 `OverlappingVpcCidrBlocks`)

**원인**: 다른 VPC와 peering 시도 시 양쪽 VPC가 동일한 `10.0.0.0/16` CIDR 사용. `10.0.0.0/16`은 AWS Default VPC + 다른 회사·다른 프로젝트가 가장 흔히 사용하는 대역이라 충돌 가능성 높음.

**해결 (사전 검증 — peering 시도 전에)**:
1. peering 대상 VPC의 CIDR 확인:
   ```bash
   aws ec2 describe-vpcs --vpc-ids <PEER_VPC_ID> --region <peer-region> | jq '.Vpcs[].CidrBlock'
   ```
2. 본 VPC `10.0.0.0/16`과 겹치면 peering 불가 — 다음 중 1개 선택:
   - 다른 쪽 VPC가 CIDR 변경 가능하면 그쪽 변경 요청
   - 본 VPC를 별도 region (예: ap-northeast-1)에 `10.1.0.0/16`으로 재구축 후 peering
   - peering 대신 PrivateLink·Transit Gateway 검토 (CIDR 충돌 우회)
3. ADR015 §"다시 검토할 시점" — multi-region 진입 시 IP 대역 분리(`10.1.0.0/16` 등) 가이드

> **예방**: 본 VPC를 portfolio 외에 다른 환경과 연결할 계획이 있으면, 초기 셋업 시점에 `10.0.0.0/16` 대신 사용 빈도 낮은 대역(`10.42.0.0/16` 등) 채택 검토. 현재는 단독 운영이라 그대로 진행.

### ts009-6: RDS 호스트명 미해석 (`Unknown host`) — DNS hostname OFF 인한

**원인**: §2 단계의 `modify-vpc-attribute --enable-dns-hostnames` 누락. private VPC 안에서 RDS의 `*.rds.amazonaws.com` 도메인 해석 불가.

**해결**:
```bash
aws ec2 describe-vpc-attribute --vpc-id $VPC_ID --attribute enableDnsHostnames --region ap-northeast-2
# Value=false면 enable:
aws ec2 modify-vpc-attribute --vpc-id $VPC_ID --enable-dns-hostnames --region ap-northeast-2
```

---

## 10. 후속 (별도 Story)

- **Story-TBD(ALB)**: `<ALB_SG>` + public subnet ID 2개 사용. milestone item #13
- **Story-TBD(RDS)**: `<DB_SG>` + data subnet ID 2개 사용. milestone item #14
- **Story-TBD(ECS Service 치환)**: Story-047 `service-prod.json`/`service-staging.json`의 `<APP_SUBNET_2A>`/`<APP_SUBNET_2C>`/`<APP_SG>` 치환
- **Story-TBD(Multi-AZ NAT)**: public-2c에 NAT 2번째 생성 + `private-rt` 분리 (M2)
- **Story-TBD(VPC Endpoint)**: s3·ecr·secretsmanager·logs 4종 Interface Endpoint + `vpc-endpoint-sg` 활용 (M2 비용 검토)
- **Story-TBD(VPC Flow Logs)**: 네트워크 트래픽 로깅. CloudWatch Logs 또는 S3 송신
- **Story-TBD(Terraform IaC)**: 본 `vpc-spec.json` + `security-groups.json`을 Terraform 모듈 입력으로 변환. M2 Product 7 Epic 2
- **Story-TBD(Bastion EC2)**: 운영 진입 도구. SSM Session Manager 채택 — bastion-sg 활용

---

## 관련

- [vpc-topology.md](../../architecture/vpc-topology.md) — 다이어그램 + 표 시각화
- [ADR015](../../adr/ADR015-vpc-3-layer-network-design.md) — 설계 결정 배경
- `infra/vpc/vpc-spec.json` · `security-groups.json` — 단일 진실 소스 JSON
- [ts008](ts008-ecs-cluster-setup.md) — 본 VPC 자원을 사용하는 ECS Cluster 셋업
- Story-048 — milestone 0.0.1v item #12
