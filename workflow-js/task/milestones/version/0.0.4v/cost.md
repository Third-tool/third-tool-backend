# M4 / 0.0.4v — Cost (비용 실측)

> **본 파일의 역할**: M4가 실제로 얼마의 신규 비용을 발생시켰는가를 실측·기록한다. `outcome.md`가 산출을 말한다면 본 파일은 그 산출을 만들기 위해 지출한 금액을 말한다. M1 잔존 (참조용)과 신규 (본 버전 착지 시 발생) 를 분리.

**작성 시점**: 2026-07-21 (D7) · M4 동결 판정 시점.

---

## 본 버전 신규 비용 (실측)

| 카테고리 | 항목 | 금액 (7일 합산) | 비고 |
| --- | --- | --- | --- |
| AWS 리소스 | 신규 배포·리소스 | **$0** | M4는 로컬 스코프 · 신규 AWS 리소스 생성 없음 |
| LLM API | Vertex AI Gemini | **$0** | LLM Adapter 배선 M6 이관 · M4는 여전히 Static Adapter만 |
| SaaS 구독 | Slack·PagerDuty·GitHub 등 | **$0** | 기존 구독 유지 · 신규 구독 없음 |
| CDN·미디어 | CloudFront 트래픽 | **$0** | 배포 없음 |
| **Total New Cost** | | **$0** | 로컬 개발 스코프 유지 |

**결론**: M4는 M3와 동일하게 **$0 신규 비용**. Card BC 대재편 · Card→Axis 3-phase 마이그레이션 · role catalog 3종 신설 모두 로컬 classpath / dev H2 / 프로덕션 배포 없이 착지.

---

## M1 잔존 비용 (이관 · 참조용)

M1(0.0.1v · 2026-06-23~28)에 신설된 AWS 리소스는 dev 프로필에서 여전히 실행. 참조용으로 유지:

| 리소스 | 카테고리 | 대략 일일 비용 (USD) | 상태 |
| --- | --- | --- | --- |
| ECS Fargate (dev · 1 Task) | 컴퓨트 | ~$0.60 | dev 환경 유지 |
| ALB (dev · 1 LB) | 네트워크 | ~$0.55 | dev 유지 |
| Route53 · ACM | DNS · TLS | ~$0.05 | 유지 |
| RDS (dev · db.t4g.micro) | 데이터베이스 | ~$0.40 | dev 유지 |
| Secrets Manager (5 secret) | 보안 | ~$0.17 | 유지 |
| ECR (10 이미지 유지) | 아티팩트 | ~$0.03 | 유지 |
| CloudWatch Logs (dev) | 관측 | ~$0.15 | 유지 |
| NAT Gateway | 네트워크 | ~$4.18 | dev 유지 · 주요 잔존 비용 |
| Prometheus/Grafana (dev docker-compose) | 관측 | $0 | 로컬 docker · AWS 비용 없음 |
| **M1 일일 잔존** | | **~$6.13/일** | |
| **M4 7일 잔존 (참고)** | | **~$42.91** | M3와 동일 |

**주의**: M1 잔존 비용은 M4 신규가 아닌 M1 시점부터 계속 발생하고 있는 것. 본 파일 §신규 비용에는 포함되지 않음.

---

## 비용 관리 (로컬 스코프 관점)

- [x] **AWS Budgets** 모니터링 유지 · M4 기간 예산 초과 알림 없음
- [x] **NAT Gateway 트래픽** 관찰 · dev 개발자 접속 외 이상 트래픽 없음
- [x] **LLM 호출 예방** — Static Adapter만 · Vertex AI 크레덴셜 로컬 미배선 (M6 이관 유지)
- [x] **dev 프로필 유지** — `application-dev.yml`이 `spring.ai.enabled=false` 유지
- [x] **Reviewer subagent 비용** — 사용자 계정 별도 요금 · 프로젝트 AWS 비용에 포함되지 않음 (참고: M4 5 PR × 5관점 = 25 reviewer 발사 · M3 15건 대비 +67%)

---

## 신규 비용 유발 조치 (본 버전 중)

**없음**. Card BC 재편·Card→Axis 마이그레이션·role catalog 3종 모두 classpath 리소스로 착지.

- Card 도메인 재편 → 코드 · classpath만
- Flyway V23~V30 → dev H2 인메모리 · 프로덕션 RDS 미착지 (M7 이관)
- role catalog 3종 JSON → `src/main/resources/ai/catalog/` classpath 리소스만
- 테스트 재작성 → 로컬 실행만

**AWS 리소스 변경 로그 (M4)**:
- 신규 생성: 0
- 수정: 0
- 삭제: 0
- IAM 정책 변경: 0
- Secrets 신규 등록: 0

---

## AI Static Adapter role 3종 신설에도 왜 비용 0인가

**Q**: M4에 planner.json · designer.json · problem-solver.json 3개 role catalog가 신설됐다. AI 응답 다양성이 4배 (1→4 role) 확장됐는데도 비용이 0인 이유?

**A**: **catalog는 정적 JSON 파일이며, 응답 생성이 여전히 Static Adapter의 in-memory 조회이기 때문**. 다음 3가지 요건이 모두 성립.

1. **JSON은 classpath 리소스** — `src/main/resources/ai/catalog/*.json`은 배포 시 JAR에 임베드. 외부 저장소 (S3 · DynamoDB) 미사용.
2. **응답 생성이 LLM 호출 없음** — 각 role별 `LayerSuggestionAdapter`·`ChaptersOutlineAdapter` 등이 `SuggestionCatalogLoader`에서 JSON 조회 후 그대로 반환. Vertex AI Gemini · OpenAI · Claude 등 유료 LLM 호출 0건.
3. **`RoleDetector`도 in-memory 로직** — concepts 배열의 키워드 매칭으로 role 판정. 외부 API 없음.

M4의 Card BC 재편도 동일 원리:
- Flyway V23~V30 → dev H2 인메모리 실행 (프로덕션 배포 없음 · M7 이관)
- `createdMode` · `axisId` 컬럼 추가 → 로컬 스키마만
- 도메인 재편 · 폐기 → 코드 리팩토링만

**만약 신규 비용이 발생했다면** (M4 out of scope 예시):
- v2 LLM Adapter 도입 → Vertex AI 호출당 비용 발생 (M6 이관)
- MySQL 프로덕션 RDS 마이그레이션 → RDS storage · IOPS · Multi-AZ 추가 비용 (M7 이관)
- CloudFront CDN 배포 → CDN 트래픽 비용 (M7 이관)

**정리**: M4는 도메인 재편 + AI role 다각화의 실질 가치를 로컬 스코프에서 확보. 비용 부담은 M6 (LLM) · M7 (배포) 시점에 이연.

---

## M5 이후 비용 재산정 트리거

1. **M5 Review BC 신설 · DailyBatch cron** — 자정 close cron은 배포 시점(M7)에 CloudWatch Events / EventBridge 비용 발생 가능성. v1은 여전히 dev 스코프 · $0 유지.
2. **M6 LLM Adapter 배선** — Vertex AI 크레덴셜 로컬 배선 · 실 호출 발생. Gemini Flash 2.5 기준 세션당 <$0.001 예상. `cost.md` §M6 실측으로 첫 실질 콘텐츠 확장.
3. **M7 배포 라인 완주** — VPC · ALB · RDS · Secrets Manager 실물 신설. 프로덕션 상시 비용 최소 ~$10~15/일 예상 (v1 최소 인스턴스).
4. **M8 프로덕션 실사용자 접속 · Vertex AI 반복 호출** — 3명 사용자 실측 시나리오 5건 · 각 시나리오당 Session Wizard axisDraft 반복 → Vertex AI daily 예산 관찰 필요.
5. **v2 예산 cap 자동 컷** — 이슈 #20 이관 유지 · v1은 관찰 지표만.

---

## 관찰 지표 로깅 v1 (M6~M7 배선 예정)

M4까지는 로컬 로그에 지표 축적만. M6 LLM Adapter 배선 시점부터 다음 항목이 Prometheus 메트릭으로 승격:

- `thirdtool.suggestion.calls_total{provider,port}` — Static vs LLM 호출 카운트
- `thirdtool.suggestion.tokens_total{provider,direction}` — prompt/completion tokens (M6부터)
- `thirdtool.suggestion.regenerations_total` — refresh 호출 카운트 (AIR Epic 2 · M7)
- `thirdtool.suggestion.fallback_ratio` — Cascade 폴백 비율 (M6~)
- `thirdtool.suggestion.rate_limit_exceeded_total` — 429 발동 카운트

**M4 시점 조치**: 로그 필드 `provider=static · role=planner|designer|problem-solver|backend-developer` 로 축적. M6~M7 Prometheus 승격 시 MDC 필드 그대로 활용.

---

## 참고

- 상위 계획: `./milestone.md`
- 성과: `./outcome.md` (기술 자산 · role catalog 3종 · Flyway V23~V30)
- 회고: `./review.md` (M4 계획 vs 실제 · SP 실측)
- 인프라: `./infra.md` (로컬 스코프 유지 · M5+ 재개 조건)
- AI 평가: `./eval.md` (M4 role 4종 catalog baseline)
- 이전 마일스톤: `../0.0.3v/cost.md` (M1 잔존 참조 · 동일 $0 실측)
- 다음 마일스톤: `../0.0.5v/milestone.md` (Review BC 신설 · 여전히 로컬 스코프)
- 릴리스 로드맵: `../../release/version/0.0.1v/release.md` — M6 LLM · M7 배포 시점에 첫 신규 비용 발생 예정
