# 0.0.1v / Cost 성과

> M1에서 처음 AWS 인프라가 가동되므로 본주는 **비용이 추가되는 첫 사이클**.
> 본 파일은 본주 시작 시 추정 + D6 종료 시 실측을 기록한다.

---

## 본주 발생 비용 카테고리

### AWS (인프라 첫 가동)

| 항목 | 추정 일 비용 (USD) | 실측 (D6) | 비고 |
| --- | --- | --- | --- |
| ECS Fargate Task (prod 1 vCPU / 2GB · 2대 + staging 0.5 vCPU / 1GB · 1대 SPOT 50%) | ~$2.80 | _____ | Story-047 정의 반영 — prod ~$2.46/일 (1vCPU·2GB·24h × 2) + staging ~$0.31/일 (SPOT 50% 가중) |
| ALB (시간 + LCU) | ~$0.80 | _____ | LCU는 트래픽 적어 최소. Story-049 ALB 1개 공유 결정(ADR016) |
| Route 53 hosted zone (1개) | ~$0.02 | _____ | $0.50/월 ÷ 30. Story-049 ALB alias record 추가 시 필수. query 비용 별도 (M1 트래픽 0) |
| ACM 인증서 (AWS 공인 wildcard) | $0 | _____ | AWS 관리형 무료. private CA는 별도 |
| RDS MySQL `db.t4g.micro` single-AZ + 20GB | ~$0.45 | _____ | M1은 single-AZ |
| Secrets Manager (5 비밀 × $0.40/월) | ~$0.07 | _____ | 거의 무시 |
| ECR (스토리지 + 데이터 전송) | ~$0.05 | _____ | 이미지 < 200MB |
| CloudWatch Logs (수집 + 보존) | ~$0.10 | _____ | JSON 로그 부피 영향 |
| NAT Gateway (private subnet → 외부) | ~$1.44 | _____ | Story-048 ts009 §1 명시 ($0.06/h × 24h). 데이터 전송 비용은 트래픽 발생 후 별도 |
| VPC endpoint (대안 — NAT 회피) | ~$0.20 | _____ | M2에서 검토 |
| Prometheus + Grafana 호스팅 (별도 EC2 t4g.small 가정) | ~$0.40 | _____ | 또는 로컬 docker-compose 시 $0 |
| **본주 총 추정** | **~$6.13 / 일** | **_____ / 일** | Story-047 ECS + Story-048 NAT + Story-049 Route 53 누계 (이전 추정 $3.27) |
| **M1 6일 누적 추정** | **~$36.78** | **_____** | |

### LLM (M1에는 미사용 — baseline)

| 항목 | 본주 비용 |
| --- | --- |
| Gemini API 호출 | $0 (M1은 Static Adapter만 사용) |
| OpenAI / Claude | $0 |
| (M2 진입 시 첫 LLM 호출 비용 baseline 측정 필요) | |

### 외부 도구

| 항목 | 본주 비용 |
| --- | --- |
| GitHub (private repo) | $0 (Free 또는 기존 구독) |
| Sentry / 에러 트래커 | $0 (도입 안 함) |
| Cloudflare (선택) | $0 |

---

## 비용 절감 측면 — 본주 작업이 만든 영향

| 작업 | 비용 영향 |
| --- | --- |
| EC2 SSH 폐기 → ECS Fargate | EC2 instance 비용 절감 vs Fargate 비용 +. 트래픽 적은 단계에서는 약간 증가 |
| Secrets Manager 도입 | GitHub Secrets 의존성 폐기 → 보안 향상 (정성). $0.07/일 추가 |
| Single Dockerfile (multi-stage) | 이미지 크기 감소 → ECR 스토리지 + pull 시간 감소 |
| LLM 미연결 (M1) | Gemini 비용 0 — M2 진입 시 baseline 측정 후 예산 산정 |

---

## 비용 통제 가드

- [ ] AWS Budgets 설정 — 월 한도 `$_____` + 80% 도달 시 메일 (ops.md 후보 4)
- [ ] NAT Gateway 켜져 있는지 확인 — 안 쓰면 즉시 끄거나 VPC endpoint 대체
- [ ] RDS Multi-AZ 의도치 않게 켜지지 않았는지 확인 (M1은 single-AZ)
- [ ] ECS Task autoscaling 미설정 — Story-047로 prod desiredCount=2 / staging=1 고정. autoscaling 도입 시 cost 재산정
- [ ] Task 사양은 운영 안정화 후 right-sizing 검토 — CloudWatch Container Insights 실측 후 (product-infra-deploy.md Q9)
- [ ] CloudWatch Logs 보존 기간 7~30일로 제한 — 무한 보존 시 비용 폭증

---

## D6 종료 시 비용 회고

> D6에 채움.

| 항목 | 추정 | 실측 | 차이 사유 |
| --- | --- | --- | --- |
| 일 평균 비용 | $3.27 | $_____ | _____ |
| 가장 큰 비용 항목 | (예상: NAT) | (실측) | _____ |
| 예상 못 한 비용 | — | $_____ | 항목명 + 사유 |

### M2 비용 산정 입력

- M1 일 평균 비용 × 30 = **월 추정 $_____**
- LLM 진입 시 추가 예산 산정 (Story 단위 비용 모델링)
- 사용자 N명 도달 시 예상 비용 (capacity 모델 — ops.md 후보 5와 연결)
