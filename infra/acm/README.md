# ACM 인증서 신청 가이드

## 신청 순서 (2개 병렬 — 동시 실행)

### 1. ap-northeast-2 (ALB용)
```bash
aws acm request-certificate \
  --cli-input-json file://infra/acm/acm-ap-northeast-2.json \
  --region ap-northeast-2
# → CertificateArn 저장 (ACM_AP_ARN)
```

### 2. us-east-1 (CloudFront용 — 필수 us-east-1)
```bash
aws acm request-certificate \
  --cli-input-json file://infra/acm/acm-us-east-1.json \
  --region us-east-1
# → CertificateArn 저장 (ACM_US_EAST_1_ARN)
```

## DNS 검증 CNAME 추가
Route53 콘솔 → 인증서 상세 → "Route 53에서 레코드 생성" 클릭 (1회로 두 인증서 모두 검증됨)

## ISSUED 확인
```bash
aws acm describe-certificate --certificate-arn $ACM_AP_ARN --region ap-northeast-2 \
  --query "Certificate.Status"
aws acm describe-certificate --certificate-arn $ACM_US_EAST_1_ARN --region us-east-1 \
  --query "Certificate.Status"
# → "ISSUED" 확인 후 ALB·CloudFront 작업 진행
```

## 주의사항
- `.dev` TLD = HSTS preloaded → HTTP 불허, HTTPS 필수
- CloudFront는 **us-east-1 ACM만** 허용 — 리전 혼동 주의
- DNS 검증 CNAME은 두 인증서가 동일 → Route53에 1회만 추가하면 양쪽 검증
