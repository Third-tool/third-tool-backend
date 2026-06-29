# ts010: ALB + Target Group 1회성 셋업·검증·트러블슈팅

Story-049로 `infra/alb/`에 ALB + Target Group 2종 spec JSON이 추가됐다. 본 가이드는 AWS 콘솔(또는 CLI)에서 1회성 셋업하는 절차 + 검증 + 자주 보는 에러 6건을 한 곳에 모았다.

ts009(VPC)·ts008(ECS)와 cross-link. 1인 운영자가 1회만 실행.

---

## 1. 전제

- **VPC 셋업 완료** (ts009 §1-8) — VPC ID + public subnet 2개(public-2a, public-2c) + alb-sg ID 발행
- **도메인 보유 시**: Route 53 hosted zone + 도메인 DNS 위임 완료 → ACM 인증서 발급 가능
- **도메인 미보유 시**: HTTPS 불가 → HTTP only 운영 (Listener 80 단일, `jwt.cookie.secure=false`로 application-prod.yml 임시 변경 필요. 운영용 아님)
- AWS 콘솔 ALB 관리 권한 (`elasticloadbalancing:*`, `acm:RequestCertificate`, `route53:ChangeResourceRecordSets`)
- AWS CLI 권장

> **본 가이드는 도메인 보유 가정** (`thirdstool.com` — application-prod.yml에 기명시). 도메인 미보유 시 §2 ACM 단계 skip + §5 HTTPS 443 listener 미생성.

---

## 2. ACM 인증서 발급 (도메인 보유 시)

> **사전 발급 권장**: ACM DNS 검증은 NS 전파 + 발급까지 **최대 72시간** 소요 가능 (보통 5-30분). 본 Story 운영 셋업 일정 1주 전에 §2를 선행 실행해 ISSUED 상태로 미리 발급해두는 것이 D-Day 일정 차단 방지에 효과적. milestone.md "리스크와 관찰 포인트" 표에 추가 검토 권장.


```bash
# wildcard 또는 multi-domain 인증서
CERT_ARN=$(aws acm request-certificate \
  --domain-name "thirdstool.com" \
  --subject-alternative-names "*.thirdstool.com" "api.thirdstool.com" "staging.thirdstool.com" \
  --validation-method DNS \
  --region ap-northeast-2 \
  --query 'CertificateArn' --output text)
echo "CERT_ARN=$CERT_ARN"

# DNS 검증 record 확인 후 Route 53에 자동 추가
aws acm describe-certificate --certificate-arn $CERT_ARN --region ap-northeast-2 \
  | jq '.Certificate.DomainValidationOptions[].ResourceRecord'
```

각 도메인의 `ResourceRecord` (`_xxx.thirdstool.com → _yyy.acm-validations.aws.`)를 Route 53 hosted zone에 CNAME 추가.

```bash
# ISSUED 도달 대기 (5분 ~ 72시간 — 보통 5-30분)
aws acm wait certificate-validated --certificate-arn $CERT_ARN --region ap-northeast-2
```

ISSUED 상태 확인:
```bash
aws acm describe-certificate --certificate-arn $CERT_ARN --region ap-northeast-2 | jq '.Certificate.Status'
# "ISSUED"
```

---

## 3. Target Group 2개 생성

VPC ID + alb-sg ID 필요 (ts009 §8 출력).

```bash
VPC_ID=vpc-xxxxxxxx  # ts009 출력

# prod Target Group
PROD_TG_ARN=$(aws elbv2 create-target-group \
  --name thirdtool-prod-tg \
  --protocol HTTP \
  --port 8080 \
  --vpc-id $VPC_ID \
  --target-type ip \
  --protocol-version HTTP1 \
  --health-check-protocol HTTP \
  --health-check-path /health \
  --health-check-port traffic-port \
  --health-check-interval-seconds 30 \
  --health-check-timeout-seconds 5 \
  --healthy-threshold-count 2 \
  --unhealthy-threshold-count 2 \
  --matcher HttpCode=200 \
  --tags Key=Project,Value=ThirdTool Key=Environment,Value=prod Key=ManagedBy,Value=Story-049 \
  --region ap-northeast-2 \
  --query 'TargetGroups[0].TargetGroupArn' --output text)

# deregistration_delay 단축 (default 300s → 30s)
aws elbv2 modify-target-group-attributes \
  --target-group-arn $PROD_TG_ARN \
  --attributes Key=deregistration_delay.timeout_seconds,Value=30 \
  --region ap-northeast-2

# staging Target Group (동일 절차, name + tags만 변경)
STAGING_TG_ARN=$(aws elbv2 create-target-group \
  --name thirdtool-staging-tg \
  --protocol HTTP --port 8080 --vpc-id $VPC_ID --target-type ip \
  --protocol-version HTTP1 \
  --health-check-path /health \
  --health-check-interval-seconds 30 --health-check-timeout-seconds 5 \
  --healthy-threshold-count 2 --unhealthy-threshold-count 2 \
  --matcher HttpCode=200 \
  --tags Key=Environment,Value=staging \
  --region ap-northeast-2 \
  --query 'TargetGroups[0].TargetGroupArn' --output text)

aws elbv2 modify-target-group-attributes \
  --target-group-arn $STAGING_TG_ARN \
  --attributes Key=deregistration_delay.timeout_seconds,Value=30 \
  --region ap-northeast-2

echo "PROD_TG_ARN=$PROD_TG_ARN"
echo "STAGING_TG_ARN=$STAGING_TG_ARN"
```

> **target-type=ip 필수**: Fargate는 instance 등록 불가. instance로 잘못 생성 시 §8 ts010-1 발생.

---

## 4. ALB 생성

```bash
PUB_2A=subnet-xxxxxxxx  # ts009 §8 출력
PUB_2C=subnet-yyyyyyyy
ALB_SG=sg-xxxxxxxx

ALB_ARN=$(aws elbv2 create-load-balancer \
  --name thirdtool-alb \
  --type application \
  --scheme internet-facing \
  --ip-address-type ipv4 \
  --subnets $PUB_2A $PUB_2C \
  --security-groups $ALB_SG \
  --tags Key=Project,Value=ThirdTool Key=Environment,Value=shared Key=ManagedBy,Value=Story-049 \
  --region ap-northeast-2 \
  --query 'LoadBalancers[0].LoadBalancerArn' --output text)

# 보안·라우팅 속성 적용 (preserve_host_header, drop_invalid_header_fields, xff_client_port)
aws elbv2 modify-load-balancer-attributes \
  --load-balancer-arn $ALB_ARN \
  --attributes \
    Key=routing.http.drop_invalid_header_fields.enabled,Value=true \
    Key=routing.http.preserve_host_header.enabled,Value=true \
    Key=routing.http.xff_client_port.enabled,Value=true \
    Key=idle_timeout.timeout_seconds,Value=60 \
  --region ap-northeast-2

# ALB DNS 확인
ALB_DNS=$(aws elbv2 describe-load-balancers --load-balancer-arns $ALB_ARN --region ap-northeast-2 \
  | jq -r '.LoadBalancers[0].DNSName')
echo "ALB_DNS=$ALB_DNS"
```

---

## 5. Listener 2개 + 라우팅 규칙

### 5.1 Listener 80 (HTTP → HTTPS 301 redirect)

```bash
aws elbv2 create-listener \
  --load-balancer-arn $ALB_ARN \
  --protocol HTTP --port 80 \
  --default-actions 'Type=redirect,RedirectConfig={Protocol=HTTPS,Port=443,StatusCode=HTTP_301,Host=#{host},Path=/#{path},Query=#{query}}' \
  --region ap-northeast-2
```

### 5.2 Listener 443 (HTTPS, default forward prod, host rule staging)

```bash
HTTPS_LISTENER_ARN=$(aws elbv2 create-listener \
  --load-balancer-arn $ALB_ARN \
  --protocol HTTPS --port 443 \
  --ssl-policy ELBSecurityPolicy-TLS13-1-2-2021-06 \
  --certificates CertificateArn=$CERT_ARN \
  --default-actions Type=forward,TargetGroupArn=$PROD_TG_ARN \
  --region ap-northeast-2 \
  --query 'Listeners[0].ListenerArn' --output text)

# staging host-based rule (priority 10)
aws elbv2 create-rule \
  --listener-arn $HTTPS_LISTENER_ARN \
  --priority 10 \
  --conditions Field=host-header,Values=staging.thirdstool.com \
  --actions Type=forward,TargetGroupArn=$STAGING_TG_ARN \
  --region ap-northeast-2
```

---

## 6. Route 53 alias record (도메인 보유 시)

ALB DNS → 사용자 도메인 매핑. ALB의 hosted zone ID는 region별 고정 (ap-northeast-2 = `Z3JE5OI70F1IRD`).

```bash
HOSTED_ZONE_ID=Z<your-hosted-zone-id>  # Route 53 hosted zone 본인 도메인
ALB_HOSTED_ZONE_ID=Z3JE5OI70F1IRD       # ap-northeast-2 ALB 고정

# api.thirdstool.com → ALB
cat > /tmp/dns-change.json <<EOF
{
  "Changes": [
    {
      "Action": "UPSERT",
      "ResourceRecordSet": {
        "Name": "api.thirdstool.com",
        "Type": "A",
        "AliasTarget": {
          "HostedZoneId": "$ALB_HOSTED_ZONE_ID",
          "DNSName": "dualstack.$ALB_DNS",
          "EvaluateTargetHealth": true
        }
      }
    },
    {
      "Action": "UPSERT",
      "ResourceRecordSet": {
        "Name": "staging.thirdstool.com",
        "Type": "A",
        "AliasTarget": {
          "HostedZoneId": "$ALB_HOSTED_ZONE_ID",
          "DNSName": "dualstack.$ALB_DNS",
          "EvaluateTargetHealth": true
        }
      }
    }
  ]
}
EOF

aws route53 change-resource-record-sets --hosted-zone-id $HOSTED_ZONE_ID --change-batch file:///tmp/dns-change.json
```

---

## 7. 검증

### 7.1 ARN 출력 메모 (후속 Story placeholder 치환용)

```
PROD_TG_ARN=$PROD_TG_ARN
# → infra/ecs/service-prod.json <PROD_TARGET_GROUP_ARN> 치환

STAGING_TG_ARN=$STAGING_TG_ARN
# → infra/ecs/service-staging.json <STAGING_TARGET_GROUP_ARN> 치환

ALB_ARN=$ALB_ARN
ALB_DNS=$ALB_DNS
CERT_ARN=$CERT_ARN
HTTPS_LISTENER_ARN=$HTTPS_LISTENER_ARN
```

### 7.2 Target Group 상태 (ECS Service 등록 후만 의미)

```bash
aws elbv2 describe-target-health --target-group-arn $PROD_TG_ARN --region ap-northeast-2 \
  | jq '.TargetHealthDescriptions[] | {Target: .Target.Id, State: .TargetHealth.State}'
# ECS Service 미생성 시 빈 배열. Service 등록 후 1-2분 대기 → "healthy" 도달
```

### 7.3 HTTP → HTTPS redirect 검증 (도메인 보유 시)

```bash
curl -sI http://api.thirdstool.com/health
# 기대: HTTP/1.1 301 Moved Permanently
#       Location: https://api.thirdstool.com/health

curl -sI https://api.thirdstool.com/health
# 기대: HTTP/2 200 (ECS Service healthy 도달 후)
```

### 7.4 staging host header 분기 검증

```bash
curl -sI https://staging.thirdstool.com/health
# staging-tg → staging Service (별도 Task)로 라우팅
```

---

## 8. 트러블슈팅

### ts010-1: Target Group의 Target이 `unhealthy` 상태 영구 지속

**원인 가능성**:
- Target Group `target-type=instance`로 잘못 생성 (Fargate는 ip 강제) — 그러나 ECS Service의 LoadBalancers config에서 IP를 등록할 수 없으므로 등록 자체 실패
- ECS Task의 healthCheck가 ALB Target Group healthCheck 도달 전에 실패
- ALB SG (`alb-sg`) → app-sg ingress 8080 누락
- ECS Service `healthCheckGracePeriodSeconds`(90s) < ALB threshold × interval (2 × 30 = 60s) 라 healthy 안정 도달 전 Task 종료

**해결**:
1. Target Group describe → `TargetType` 확인 → `ip`가 아니면 재생성 필수
2. `alb-sg` ingress: 80/443 from 0.0.0.0/0 / `app-sg` ingress: 8080 from alb-sg (ts009 §7.1-7.2 확인)
3. ECS Exec로 Task 진입 → `wget -O- http://localhost:8080/health` 직접 200 확인 (Spring Boot 부팅 완료 검증)
4. Service `healthCheckGracePeriodSeconds`를 120s로 늘리거나 ECS Task healthCheck `startPeriod`를 늘림

### ts010-2: ACM 인증서 `PENDING_VALIDATION` 영구 지속

**원인**: DNS 검증 record가 Route 53에 추가됐지만 NS 위임 미설정으로 전파 안 됨. 또는 도메인 registrar에서 NS 변경 5-72시간 대기.

**해결**:
1. `dig _xxx.thirdstool.com CNAME` → 등록한 검증 CNAME이 보이는지 확인
2. 안 보이면 Route 53 hosted zone NS와 도메인 registrar(가비아·후이즈·Route 53 등) NS가 일치하는지 확인
3. NS 변경 직후라면 최대 72시간 대기 (보통 5-30분)

### ts010-3: HTTP→HTTPS redirect가 1회만 동작, 이후 무한 loop

**원인**: ECS Task가 `X-Forwarded-Proto` 헤더 무시. Spring Boot가 HTTPS 인지 못 해 자체 응답에 `http://` URL 반환 → 브라우저가 다시 80으로 요청.

**해결**:
1. `application-prod.yml`에 `server.forward-headers-strategy: native` 또는 `framework` 추가
2. `routing.http.xff_client_port.enabled=true` ALB attribute 확인 (§4 명시)
3. ALB가 `X-Forwarded-Proto: https` 헤더 전달 중인지 Task 로그 확인

### ts010-4: `jwt.cookie.secure=true` + HTTPS 브라우저에서 쿠키 못 받음

**원인**: ALB → ECS는 HTTP, application-prod.yml은 `jwt.cookie.secure=true` 명시. Spring이 응답에 `Secure` flag 붙이지만 ALB가 `X-Forwarded-Proto: https`로 알려야 정상.

**해결**: ts010-3 해결과 동일. `forward-headers-strategy` 적용.

### ts010-5: ACM 인증서 SubjectAlternativeNames 빠진 도메인 추가

**원인**: 인증서 발급 시 `staging.thirdstool.com` 누락 → 발급 후 도메인 추가 시 인증서 재발급 필요.

**해결**:
1. 새 인증서 발급(`--subject-alternative-names`에 누락 도메인 포함)
2. ALB Listener 443의 certificate 교체 (`aws elbv2 modify-listener --certificates`)
3. 기존 인증서 삭제 (ALB 미참조 확인 후)

### ts010-6: Listener rule priority 충돌 (`PriorityInUse`)

**원인**: 새 rule 추가 시 동일 priority 사용 중. priority는 1-50000 unique 필수.

**해결**:
```bash
aws elbv2 describe-rules --listener-arn $HTTPS_LISTENER_ARN --region ap-northeast-2 \
  | jq '.Rules[] | {Priority, Conditions}'
# 사용 중 priority 확인 후 다른 값(예: 20, 30) 사용
```

---

## 9. 후속 (별도 Story)

- **Story-TBD(ECS Service 치환)**: Story-047 `service-{prod,staging}.json`의 `<*_TARGET_GROUP_ARN>` 치환 + ECS Service 생성/업데이트
- **Story-TBD(WAF)**: ALB 앞 AWS WAF web ACL — Rate-based + SQLi/XSS/Common rules (M2 보안 강화)
- **Story-TBD(Route 53 IaC)**: Terraform 모듈로 alias record 자동 관리 (M2)
- **Story-TBD(sticky sessions)**: 현재 JWT stateless라 불요. 세션 기반 전환 시 검토
- **Story-TBD(connection draining 튜닝)**: 현재 30s. 부하 테스트 후 ECS Task graceful shutdown 시간 매칭
- **Story-TBD(Custom error pages)**: 503/504 default 페이지 customizing
- **Story-TBD(ALB access logs S3)**: 트래픽 감사 — 보안 사고 추적 (cost trade-off)

---

## 10. 관련

- [`ts009-vpc-setup.md`](ts009-vpc-setup.md) — alb-sg + public subnet 발행 (선행)
- [`ts008-ecs-cluster-setup.md`](ts008-ecs-cluster-setup.md) — ECS Service의 LoadBalancers config가 본 TG ARN 사용
- [ADR016](../../adr/ADR016-alb-listener-target-group.md) — ALB·Target Group 설계 결정 배경
- `infra/alb/alb-spec.json` · `target-group-spec.json` — 단일 진실 소스 JSON
- Story-049 — milestone 0.0.1v item #13
- AWS handoff 통합: `workflow/task/pes/handoff/aws-setup-0.0.1v.md` Stage 7
