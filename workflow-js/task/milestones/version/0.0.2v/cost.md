# 0.0.2v / Cost — 로컬 스코프 (0.0.3v 급행 실측)

> **범위**: 본 버전(0.0.2v)은 **로컬 개발 스코프** · 실질 0.0.3v Tier 1+2 완주 포함. 신규 AWS 리소스·LLM 호출 없음. **신규 비용 발생 = 0원 / 0 USD**.

---

## 본 버전 신규 비용 (Tier 1 + Tier 2 실측)

| 카테고리 | 신규 비용 (일) | 실측 근거 | 비고 |
| --- | --- | --- | --- |
| AWS 신규 리소스 | **$0** | 리소스 신설 SQL·API 0건 | 인프라 신설 없음 |
| LLM (Gemini · GPT · Claude) | **$0** | 호출 코드 0건, `provider=static` 기본 | Static Adapter (JSON catalog) 만 활성. LLM Adapter 미도입 |
| 외부 도구·SaaS | **$0** | package.json / build.gradle 변경 없음 | 도입 없음 |
| Vercel / Cloudflare CDN | **$0** | FE 배포 미수행 | M1 잔존 유지 |
| **합계 (신규)** | **$0** | | 목표 달성 |

---

## M1 잔존 비용 (이관 · 참조용)

M1 에서 신설된 AWS 리소스가 여전히 가동 중이므로 비용은 지속 발생. 본 버전 스코프 밖.

| 항목 | M1 실측 (참조) | M2 예상 유지 | 비고 |
| --- | --- | --- | --- |
| ECS Fargate (prod 2 · staging 1) | ~$2.80 / 일 | ~$2.80 / 일 | 재배포 없음, 코드 변경 반영 안 됨 |
| ALB | ~$0.80 / 일 | ~$0.80 / 일 | |
| Route 53 hosted zone | ~$0.02 / 일 | ~$0.02 / 일 | |
| RDS single-AZ | ~$0.45 / 일 | ~$0.45 / 일 | |
| Secrets Manager | ~$0.07 / 일 | ~$0.07 / 일 | |
| ECR | ~$0.05 / 일 | ~$0.05 / 일 | |
| CloudWatch Logs | ~$0.10 / 일 | ~$0.10 / 일 | 배포 없이 로그 축적만 |
| NAT Gateway | ~$1.44 / 일 | ~$1.44 / 일 | |
| Prometheus + Grafana | ~$0.40 / 일 | ~$0.40 / 일 | |
| **잔존 총 (참조)** | **~$6.13 / 일** | **~$6.13 / 일** | 배포 정지 상태의 유지 비용 |

**M2 7일 잔존 누적 (참조)**: ~$42.91

> 상세 M1 실측: `../0.0.1v/cost.md`

---

## 비용 관리 (로컬 스코프 관점)

본 버전 활동 중 다음 확인:

- [ ] AWS Budgets 알림 이상 없음 확인 (월 한도 대비 진척)
- [ ] NAT Gateway 트래픽 급증 없음 (본 버전 미배포이므로 트래픽 0 예상)
- [ ] LLM 호출 실수 방지 (Adapter 미구현이므로 원천 차단)
- [ ] 로컬 개발 중 실수로 AWS SDK 호출 발생 안 하도록 profile 관리 (`local` profile 강제)

---

## 신규 비용 유발 조치 (본 버전 중)

- 없음. 본 버전 스코프상 인프라·외부 서비스 도입 금지.

---

## M3 (0.0.3v) 이후 비용 재산정 트리거

다음 조건 중 최소 1개 성립 시 재산정:

1. LLM Adapter 활성화 (AS Epic 4) → Vertex AI Gemini 첫 비용 baseline 필요
2. dev 환경 재배포 → CloudWatch 로그 축적 · 트래픽 재개
3. RDS Multi-AZ 전환 → 비용 배수
4. k6 부하 테스트 (Product 8) → 트래픽 폭증 시 데이터 전송 비용

---

## 참고

- M1 상세 비용 실측: `../0.0.1v/cost.md`
- 배포 재개 시 재측정 대상: ECS · ALB · RDS · Grafana · LLM 요금 baseline
- 비용 통제 원칙: 정당화 없는 인프라 신설 금지 (본 버전 준수)

---

## Tier 2 (AI Static Adapter) 도입에도 왜 비용 0인가

Tier 2에서 도입된 자산이 **로컬 리소스 + classpath JSON만**이라서:
- `RoleDetector` — 하드코드 사전 (Java Map). 외부 호출 없음.
- `SuggestionCatalogLoader` — classpath 리소스 로딩 + JVM 캐시. 외부 호출 없음.
- `StaticLayerSuggestionAdapter` — Static 카탈로그 조회. LLM 호출 없음.
- `resources/ai/catalog/backend-developer.json` · `generic.json` — 정적 JSON 파일 (프로젝트 JAR 내부).

**LLM Adapter 활성 시점 (예상 비용 baseline 필요)**:
- AS Epic 4에서 Vertex AI Gemini 도입 시 첫 API 호출 요금 발생.
- 예상 요금: $0.075 / 1M input tokens · $0.30 / 1M output tokens (Gemini 1.5 Flash 참조).
- 예상 요청 크기: LayerSuggestion 1건당 ~500 input tokens + ~200 output tokens.
- 트리거 시점에 `cost.md` 재산정.

---

*작성일: 2026-07-02 | 본 버전 스코프: 로컬 개발 · **비용 신규 발생 $0** (Tier 1+2 완주 후) | 참조: `../0.0.1v/cost.md`*
