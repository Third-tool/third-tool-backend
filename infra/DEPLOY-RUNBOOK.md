# 1차 배포 완성 런북 — Route53 + HTTPS + FE CDN

> 실행 전 전제: `AWS_ACCOUNT_ID`, `HZ_ID` 등 변수를 shell에 export 후 진행

## Phase 0 (사용자 직접): Route53 도메인 등록

AWS 콘솔 → Route53 → Registered domains → Register domain → `thirdtool.dev` 검색 → 구매 (~$12/년)
- 등록 완료 시 Hosted Zone 자동 생성
- `HZ_ID=$(aws route53 list-hosted-zones-by-name --dns-name thirdtool.dev --query 'HostedZones[0].Id' --output text | cut -d/ -f3)`

---

## Phase 1: ACM 인증서 2개 동시 신청

```bash
# ALB용 (ap-northeast-2)
ACM_AP_ARN=$(aws acm request-certificate \
  --cli-input-json file://infra/acm/acm-ap-northeast-2.json \
  --region ap-northeast-2 \
  --query CertificateArn --output text)
echo "ACM_AP_ARN=$ACM_AP_ARN"

# CloudFront용 (us-east-1 필수)
ACM_US_ARN=$(aws acm request-certificate \
  --cli-input-json file://infra/acm/acm-us-east-1.json \
  --region us-east-1 \
  --query CertificateArn --output text)
echo "ACM_US_ARN=$ACM_US_ARN"
```

Route53 콘솔 → 각 인증서 상세 → "Route 53에서 레코드 생성" (1회로 양쪽 검증)

```bash
# ISSUED 대기 (약 5~10분)
aws acm wait certificate-validated --certificate-arn $ACM_AP_ARN --region ap-northeast-2
aws acm wait certificate-validated --certificate-arn $ACM_US_ARN --region us-east-1
echo "✅ 인증서 발급 완료"
```

---

## Phase 2: ALB HTTPS 전환

```bash
ALB_ARN=$(aws elbv2 describe-load-balancers --names third-tool-alb \
  --query 'LoadBalancers[0].LoadBalancerArn' --output text)
TG_ARN=$(aws elbv2 describe-target-groups --names third-tool-tg \
  --query 'TargetGroups[0].TargetGroupArn' --output text)
HTTP_LISTENER_ARN=$(aws elbv2 describe-listeners --load-balancer-arn $ALB_ARN \
  --query 'Listeners[?Port==`80`].ListenerArn' --output text)
ALB_SG_ID=$(aws elbv2 describe-load-balancers --names third-tool-alb \
  --query 'LoadBalancers[0].SecurityGroups[0]' --output text)

# 443 인바운드 허용
sed "s/<ALB_SG_ID>/$ALB_SG_ID/" infra/vpc/security-group-alb-443.json | \
  aws ec2 authorize-security-group-ingress --cli-input-json file:///dev/stdin

# HTTPS 리스너 생성
sed -e "s|<ALB_ARN>|$ALB_ARN|" \
    -e "s|<ACM_AP_NORTHEAST_2_ARN>|$ACM_AP_ARN|" \
    -e "s|<TARGET_GROUP_ARN>|$TG_ARN|" \
    infra/alb/alb-https-listener.json | \
  aws elbv2 create-listener --cli-input-json file:///dev/stdin

# HTTP → HTTPS 리다이렉트
sed "s|<HTTP_80_LISTENER_ARN>|$HTTP_LISTENER_ARN|" infra/alb/alb-http-to-https-redirect.json | \
  aws elbv2 modify-listener --cli-input-json file:///dev/stdin

echo "✅ ALB HTTPS 완료"
```

---

## Phase 3: Route53 api.thirdtool.dev → ALB

```bash
ALB_DNS=$(aws elbv2 describe-load-balancers --names third-tool-alb \
  --query 'LoadBalancers[0].DNSName' --output text)
ALB_ZONE=$(aws elbv2 describe-load-balancers --names third-tool-alb \
  --query 'LoadBalancers[0].CanonicalHostedZoneId' --output text)

# json 인라인 치환 후 적용
python3 -c "
import json, sys
with open('infra/route53/route53-api-record.json') as f:
    d = json.load(f)
target = d['Changes'][0]['ResourceRecordSet']['AliasTarget']
target['HostedZoneId'] = '$ALB_ZONE'
target['DNSName'] = '$ALB_DNS'
print(json.dumps(d))
" > /tmp/route53-api-record.json

aws route53 change-resource-record-sets \
  --hosted-zone-id $HZ_ID \
  --change-batch file:///tmp/route53-api-record.json
echo "✅ api.thirdtool.dev → ALB 완료"
```

---

## Phase 4: S3 FE 버킷 생성

```bash
aws s3api create-bucket \
  --cli-input-json file://infra/s3/s3-fe-bucket.json

aws s3api put-public-access-block \
  --cli-input-json file://infra/s3/s3-fe-bucket-public-access-block.json

echo "✅ S3 버킷 생성 완료"
```

---

## Phase 5: CloudFront 배포 (ACM us-east-1 ISSUED 후)

```bash
# OAC 생성
OAC_ID=$(aws cloudfront create-origin-access-control \
  --cli-input-json file://infra/cloudfront/cloudfront-oac.json \
  --query 'OriginAccessControl.Id' --output text)

# cloudfront-dist.json에 OAC + ACM ARN 주입
python3 -c "
import json
with open('infra/cloudfront/cloudfront-dist.json') as f:
    d = json.load(f)
d['DistributionConfig']['Origins']['Items'][0]['OriginAccessControlId'] = '$OAC_ID'
d['DistributionConfig']['ViewerCertificate']['ACMCertificateArn'] = '$ACM_US_ARN'
with open('/tmp/cloudfront-dist.json', 'w') as f:
    json.dump(d, f)
"

CF_RESULT=$(aws cloudfront create-distribution \
  --cli-input-json file:///tmp/cloudfront-dist.json)
CF_DIST_ID=$(echo $CF_RESULT | python3 -c "import json,sys; print(json.load(sys.stdin)['Distribution']['Id'])")
CF_DOMAIN=$(echo $CF_RESULT | python3 -c "import json,sys; print(json.load(sys.stdin)['Distribution']['DomainName'])")
CF_ARN=$(echo $CF_RESULT | python3 -c "import json,sys; print(json.load(sys.stdin)['Distribution']['ARN'])")

echo "CF_DIST_ID=$CF_DIST_ID"
echo "CF_DOMAIN=$CF_DOMAIN"

# S3 버킷 정책 (CloudFront ARN 주입)
python3 -c "
import json
with open('infra/s3/s3-fe-bucket-policy.json') as f:
    d = json.load(f)
d['Statement'][0]['Condition']['StringEquals']['AWS:SourceArn'] = '$CF_ARN'
with open('/tmp/s3-fe-bucket-policy.json', 'w') as f:
    json.dump(d, f)
"
aws s3api put-bucket-policy \
  --bucket thirdtool-fe-prod \
  --policy file:///tmp/s3-fe-bucket-policy.json

echo "✅ CloudFront 배포 완료"
```

---

## Phase 6: Route53 thirdtool.dev → CloudFront

```bash
python3 -c "
import json
with open('infra/route53/route53-fe-record.json') as f:
    d = json.load(f)
for ch in d['Changes']:
    ch['ResourceRecordSet']['AliasTarget']['DNSName'] = '$CF_DOMAIN'
with open('/tmp/route53-fe-record.json', 'w') as f:
    json.dump(d, f)
"

aws route53 change-resource-record-sets \
  --hosted-zone-id $HZ_ID \
  --change-batch file:///tmp/route53-fe-record.json
echo "✅ thirdtool.dev → CloudFront 완료"
```

---

## Phase 7: GHA 변수 등록

GitHub 콘솔 → Settings → Secrets and variables → Actions → Variables:
- `CF_DIST_ID` = CloudFront Distribution ID
- `AWS_ACCOUNT_ID` = AWS 계정 ID (이미 있으면 스킵)

---

## 검증 체크리스트

```bash
# BE API
curl -I https://api.thirdtool.dev/actuator/health
# → HTTP/2 200

# HTTP → HTTPS 리다이렉트
curl -I http://api.thirdtool.dev/actuator/health
# → 301 Location: https://...

# CloudFront (배포 후 DNS 전파 최대 60분)
curl -I https://thirdtool.dev
# → HTTP/2 200

# CORS 확인
curl -I -H "Origin: https://thirdtool.dev" https://api.thirdtool.dev/actuator/health
# → access-control-allow-origin: https://thirdtool.dev
```
