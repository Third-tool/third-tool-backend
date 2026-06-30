# CloudFront 배포 가이드

## 실행 순서 (ACM us-east-1 ISSUED 후)

### 1. OAC 생성 (먼저)
```bash
aws cloudfront create-origin-access-control \
  --cli-input-json file://infra/cloudfront/cloudfront-oac.json
# → OriginAccessControl.Id 저장 (OAC_ID)
```

### 2. cloudfront-dist.json의 <OAC_ID> 치환
```bash
sed -i "s/<OAC_ID>/$OAC_ID/" infra/cloudfront/cloudfront-dist.json
sed -i "s/<ACM_US_EAST_1_ARN>/$ACM_US_EAST_1_ARN/" infra/cloudfront/cloudfront-dist.json
```

### 3. CloudFront 배포 생성
```bash
aws cloudfront create-distribution \
  --cli-input-json file://infra/cloudfront/cloudfront-dist.json
# → Distribution.Id (CF_DIST_ID), Distribution.DomainName (CLOUDFRONT_DOMAIN) 저장
```

### 4. S3 버킷 정책 업데이트 (CloudFront ARN 주입)
```bash
CF_ARN="arn:aws:cloudfront::<AWS_ACCOUNT_ID>:distribution/$CF_DIST_ID"
sed -i "s|<CLOUDFRONT_DISTRIBUTION_ARN>|$CF_ARN|" infra/s3/s3-fe-bucket-policy.json

aws s3api put-bucket-policy \
  --bucket thirdtool-fe-prod \
  --policy file://infra/s3/s3-fe-bucket-policy.json
```

### 5. Route53 FE 레코드 (CLOUDFRONT_DOMAIN 주입 후)
```bash
sed -i "s/<CLOUDFRONT_DOMAIN>/$CLOUDFRONT_DOMAIN/" infra/route53/route53-fe-record.json
sed -i "s/<HOSTED_ZONE_ID>/$HZ_ID/" infra/route53/route53-fe-record.json

aws route53 change-resource-record-sets \
  --hosted-zone-id $HZ_ID \
  --change-batch file://infra/route53/route53-fe-record.json
```

## 캐시 정책 ID 설명
`658327ea-f89d-4fab-a63d-7e88639e58f6` = AWS managed "CachingOptimized" 정책
