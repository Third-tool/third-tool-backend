# FE 0.0.1v / Cost 성과

> FE M1에서 처음 FE 자산이 CloudFront/S3로 나가는 사이클. 본 파일은 **FE 관점에서 발생하는 비용**을 추정 + 실측한다.
> 백엔드 `cost.md`가 ECS/RDS/NAT 비용을 다루는 반면, 본 파일은 **CloudFront + S3 + Route53(FE 몫) + 도구 라이선스**를 다룬다.
> 인프라 비용의 상당 부분은 BE와 공유(Route53 hosted zone 등) → 여기서는 FE에 배분 가능한 항목만 추적.

---

## FE 인프라 비용 (M1 첫 가동)

### CloudFront (FE 진영 신규)

| 항목 | 추정 일 비용 (USD) | 실측 (D6) | 비고 |
| --- | --- | --- | --- |
| CloudFront 데이터 전송 (첫 1TB/월 무료 티어) | ~$0 | `_____` | AWS Free Tier 12개월 + 무기한 1TB/월 |
| CloudFront HTTP/HTTPS requests (1천만/월 무료) | ~$0 | `_____` | M1 트래픽 매우 낮음 |
| CloudFront invalidation (첫 1,000/월 무료) | ~$0 | `_____` | GHA 배포당 1회 |
| **CF 소계** | **~$0.00 / 일** | **`_____`** | 무료 티어 안 |

### S3 (FE 버킷)

| 항목 | 추정 일 비용 (USD) | 실측 (D6) | 비고 |
| --- | --- | --- | --- |
| S3 스토리지 (~10MB, 무료 티어 5GB) | ~$0 | `_____` | dist/ 크기 |
| S3 GET requests (CF Origin fetch — 캐시 miss 시만) | ~$0 | `_____` | CF cache hit이면 0 |
| S3 PUT requests (GHA sync 배포당 ~20건) | ~$0 | `_____` | 무료 티어 2,000 PUT/월 |
| **S3 소계** | **~$0.00 / 일** | **`_____`** | 무료 티어 안 |

### Route53 (BE와 공유)

| 항목 | 추정 일 비용 (USD) | 실측 (D6) | 비고 |
| --- | --- | --- | --- |
| Hosted Zone (1개, 총합) | ~$0.02 | `_____` | $0.50/월 ÷ 30. BE와 공유 → FE 몫 실질 $0 (기존 BE 부담) |
| DNS query (FE 트래픽 발생 시) | ~$0 | `_____` | 첫 100만 query/월 $0.40 |
| **Route53 소계 (FE 몫)** | **~$0.00 / 일** | **`_____`** | BE와 공동 부담 |

### ACM 인증서 (us-east-1, CloudFront 용)

| 항목 | 추정 일 비용 (USD) | 실측 (D6) | 비고 |
| --- | --- | --- | --- |
| ACM 공인 인증서 | $0 | $0 | AWS 관리형 무료 |

### FE 인프라 총합

| 카테고리 | 일 비용 (USD) | M1 6일 누계 |
| --- | --- | --- |
| CloudFront | ~$0.00 | ~$0.00 |
| S3 | ~$0.00 | ~$0.00 |
| Route53 (FE 몫) | ~$0.00 | ~$0.00 |
| ACM | $0.00 | $0.00 |
| **본주 FE 인프라 총 추정** | **~$0.00 / 일** | **~$0.00** |
| **본주 FE 인프라 실측** | **`_____ / 일`** | **`_____`** |

> M1 트래픽 0명 · 자산 10MB 규모에서 FE 인프라 비용은 거의 발생하지 않는다 (무료 티어 안).
> BE 인프라 비용($6.13/일 추정)이 인프라 총 비용의 대부분이며 FE 몫은 실질 $0.

---

## FE 개발 도구 / 라이선스 비용

M1 기준 도입된 도구.

| 도구 | 라이선스 | 월 비용 (USD) | 비고 |
| --- | --- | --- | --- |
| Vite / React / TypeScript / Tailwind | OSS | $0 | |
| TanStack Query / React Router / Zod / Axios | OSS | $0 | |
| MSW / Vitest / Testing Library | OSS | $0 | |
| GitHub (private repo) | Free 또는 개인 subscription | $0 | 기존 |
| GitHub Actions (public repo 무료 / private 2000분/월 무료) | Free | $0 | GHA runner 시간 M1 예상 ~10분 |
| Figma (design) | Free tier | $0 (개인) | 팀 확장 시 재검토 |
| Sentry / 에러 트래커 | 도입 안 함 | $0 | M2 이후 검토 |
| Firebase (FCM) | 도입 안 함 | $0 | product-notification backlog |
| Vercel / Netlify | 미사용 | $0 | AWS 통합 유지 |
| **도구 총 M1** | | **$0.00** | |

---

## UX/디자인 성과 대비 비용 (정성)

FE M1의 실질 비용 결정은 인프라가 아니라 **개발 시간 투자**다. 정량 기록:

| 항목 | 시간 투자 추정 (h) | 실측 (D6) |
| --- | --- | --- |
| BE 인프라 spec 이해 및 사용자 액션 대기 조정 | `_____` | `_____` |
| `.env.production` + GHA workflow 정합 | `_____` | `_____` |
| CORS + SameSite E2E 검증 | `_____` | `_____` |
| BE Story 5-4 대응 grep · 정합 | `_____` | `_____` |
| LoginPage 상수화 (social-providers.ts) | `_____` | `_____` |
| MSW 8종 401 handler + Vitest | `_____` | `_____` |
| done 정합 grep + CI job | `_____` | `_____` |
| CoverageBadge 상수 + snapshot | `_____` | `_____` |
| Lighthouse baseline (Tier 2) | `_____` | `_____` |
| dnd-kit FE-ADR 초안 (Tier 2) | `_____` | `_____` |
| 마일스톤 문서 작성 (D6) | `_____` | `_____` |
| **본주 FE 시간 투자 총합** | `_____` | `_____` |

시간 대비 UX 성과:

- 브라우저에서 도메인 접속 가능 = ROI 매우 높음 (사용자 진입점 확보)
- BE 계약 정합 = drift 예방 (미래 debugging 시간 절감)
- MSW 8종 handler = 인증 회복 UX의 사전 검증 인프라

---

## 비용 절감 측면 — 본주 작업이 만든 영향

| 작업 | 비용 영향 |
| --- | --- |
| Vercel / Netlify 대신 CloudFront 채택 | 무료 티어 안에서 CDN 확보. 트래픽 증가 시 예측 가능한 요금 |
| S3 정적 호스팅 대신 CF+OAC 채택 | S3 GET 요청 비용 절감 (CF cache hit 시) |
| GHA `workflow_dispatch` v1 (자동 트리거 v2 유예) | 오배포 방지 → 반복 배포 비용 절감 |
| MSW 사용 (실 API 호출 없이 인증 시나리오 검증) | LLM/API 호출 비용 0 (BE M1은 Gemini 미사용) |
| Sentry 미도입 | 월 $0. 대신 브라우저 콘솔 관찰만 (M2 이후 재검토) |

---

## 비용 통제 가드

- [ ] AWS Budgets 설정 확인 (BE와 공유) — 월 한도 도달 시 알림
- [ ] CloudFront 트래픽 비정상 증가 감시 — 무료 티어(1TB/월) 도달 시 알림
- [ ] S3 스토리지 폭증 감시 — dist/ 크기 5GB 접근 시 검토 (현재 10MB 수준)
- [ ] GHA runner 시간 감시 — private repo 2000분/월 무료
- [ ] Sentry 도입 시점에 별도 예산 계획 수립 — `FE-ADR-CANDIDATES.md` P1-11
- [ ] Firebase (FCM) 도입 시점에 별도 예산 계획 — `product-notification.md` backlog
- [ ] Lighthouse CI 도입 시 GHA runner 시간 여유 확인 — Tier 2 이후

---

## D6 종료 시 비용 회고

> D6에 채움.

| 항목 | 추정 | 실측 | 차이 사유 |
| --- | --- | --- | --- |
| FE 일 평균 인프라 비용 | ~$0.00 | $`_____` | `_____` |
| CF 데이터 전송 (첫 6일) | ~$0.00 | $`_____` | 무료 티어 안 여부 |
| GHA runner 사용 시간 (M1 총) | ~10분 | `_____ 분` | 배포 재시도 횟수 |
| 예상 못 한 비용 | — | $`_____` | 항목명 + 사유 |

### M2 비용 산정 입력

- M1 FE 실측 → M2 FE 예상 (트래픽 여전히 낮음 → 유사)
- 사용자 유입 시 CloudFront 트래픽 첫 임계 도달 시점 예상
- Sentry / RUM 도입 시 예산 산정 (M2 이후 결정)
- Firebase FCM 도입 시 예산 산정 (`product-notification.md` 진입 시)

---

## 인프라 vs 개발 시간 총합 (정성)

FE M1은 인프라 비용이 실질 $0이므로, 실질 비용은 **개발 시간 투자**로만 측정된다. 이는:

- BE M1과 달리 FE는 **자원 비용보다 UX 완결도**가 성과 지표
- 작은 절감(무료 티어 안 CDN) + 큰 성과(도메인 접속 가능한 첫 배포) = ROI 명확
- 미래 사용자 유입 시 첫 임계는 CloudFront 트래픽(1TB/월). 그 임계 도래 시 재산정

FE 관점의 "비용"은 앞으로도 개발 시간 + 도구 라이선스가 주를 이룰 것이며, 인프라 비용은 사용자 수 임계 도래 시 별도 산정 예정.
