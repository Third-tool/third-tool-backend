# M7 / 0.0.7v — Week of 2026-08-05 ~ 2026-08-11 (D1 = 2026-08-05 Wed)

> **마일스톤의 역할**: M6가 08-04에 동결됐다 (LLM Adapter 4종 + Cascade 폴백 + Rate Limit + `RoadmapInteractionSession` Aggregate 착지 · Vertex AI 첫 실측 발생). 본 M7은 **첫 사용자 릴리스 (0.1.0v) 대비 배포 파이프라인 완주 + AWS 리소스 실물 신설 + 관측 baseline + AS Epic 6 관측성 + AIR Epic 2 axisDraft 완주** 를 목표로 한다. **v1 사용자에게 실제 프로덕션 URL로 열리는 마일스톤**.
>
> 한 주 = 한 버전 = `version/0.0.X v/` 폴더 하나. 본 버전(0.0.7v)에는 다음 7 파일이 들어간다:
> - `milestone.md` *(본 문서)* — 잡힌 양 + 일정 + 의존 + Epic PR 매트릭스
> - `infra.md` — **본 버전 최초로 프로덕션 배포 발생** — AWS VPC + ALB + RDS + ECR + ECS + Route53 + ACM 착지 반영
> - `performance.md` — Web Vitals + P95 응답 baseline (Grafana 첫 관찰)
> - `outcome.md` — 사용자·기능 성과 (프로덕션 URL 첫 도달 + 관측 지표 배선 + Session Wizard 축적)
> - `cost.md` — 비용 성과 (AWS 리소스 실측 첫 발생 · Vertex AI 실측 M6 대비 확장)
> - `review.md` — 회고 + 다음 버전 진입 신호 (M8 = 릴리스 준비 주간)
> - `eval.md` — AI 응답 품질 평가 프레임 확장 (Cascade 실측 통계 baseline + Session Wizard UX 관찰)

**릴리스 대응**: 본 M7은 첫 사용자 릴리스(0.1.0v, 2026-08-19) 6주 로드맵의 **여섯 번째** 마일스톤. **M8 완료 후 릴리스 예정**. 자세한 스코프는 `workflow/task/milestones/release/version/0.0.1v/release.md` §Product별 릴리스 스코프 순회 참조.

**FE 대응 마일스톤**: `workflow/task/pes/fe/fe-milestones/version/0.0.7v/milestone.md` — 동일 주간, Session Wizard axisDraft UI + 프로덕션 URL 접속 검증.

---

## 0.0.7v 스코프 결정 (M6 이후, 2026-08-05)

**M6 착지 결과 요약**:
- ✅ AS Epic 4 완주 (LLM Adapter 4종 · Vertex AI Gemini Flash 2.5)
- ✅ AS Epic 5 완주 (SuggestionCascade · Bucket4j Rate Limit · SuggestionController 재편)
- ✅ AIR Epic 1 완주 (RoadmapInteractionSession Aggregate · 상태기계 · 시작 API · TTL)
- ✅ Flyway V39 착지 (Session 테이블)
- ✅ Vertex AI 실측 20~50 호출 · 비용 관찰 첫 데이터 · `cost.md` §M6 실측 반영
- ✅ Reviewer 5관점 세션 4회 진행

**M7 축 결정 — 배포 라인 완주 + 관측 baseline + AS Epic 6 + AIR Epic 2**:

M6 review.md §다음 마일스톤 결정 예고 반영. 릴리스 6주 로드맵의 클라이맥스 · AWS 실물 리소스 신설로 프로덕션 첫 URL 도달. M8 릴리스 대비 dry-run 준비 완료.

이 지시를 반영해 본 M7은 다음 4축으로 구성:

1. **인프라 배포 라인 완주** — 4 Product (deploy · network · ops) Epic 착지 · AWS 리소스 실물 신설. **주력 (~45%)**.
2. **관측 baseline 착지** — 2 Product (log · op) + AS Epic 6 (관측성). **부차 (~25%)**.
3. **AIR Epic 2 axisDraft 완주** — Story 2-1~2-4 (AxisDraftSnapshot VO + 6-Port 호출 + refresh + 실패 처리). **경량 (~15%)**.
4. **User BC 잔재 완주 + Card BC 안정화 관찰** — User Story 1건 잔재 · Card 회귀 관찰. **경량 (~15%)**.

**Epic 단위 PR 7건 예정 (본주 목표)**:

| Epic PR | 대응 | 예상 SP | 종료 목표 |
|---|---|---|---|
| PR#1 (INFRA-DEPLOY-CI) | product-infra-deploy Epic 1~4 (Dockerfile + ECR + ECS Fargate + GHA OIDC + 환경 분리) | 6 | D3 |
| PR#2 (INFRA-NETWORK-AWS) | product-infra-network Epic 1~3 (VPC + ALB + Route53 + ACM + RDS Multi-AZ) | 5 | D3 |
| PR#3 (INFRA-OPS-SECRETS) | product-infra-ops Epic 1~2 (Secrets Manager · PITR + S3 + CloudWatch Alarms) | 4 | D4 |
| PR#4 (LOG-BASELINE) | product-log Epic 1~4 (JSON logstash + MDC + GlobalExceptionHandler + Slack 배포 이벤트) | 4 | D5 |
| PR#5 (OP-BASELINE) | product-op Epic 1~3 (Actuator + Prometheus + Grafana baseline) | 3 | D5 |
| PR#6 (AS-E6-OBSERVABILITY) | AS Epic 6 (`thirdtool.suggestion.*` 메트릭 + MDC 확장 + ErrorCode + Runbook) | 2.5 | D6 |
| PR#7 (AIR-E2-AXIS-DRAFT) | AIR Epic 2 (AxisDraftSnapshot + 6-Port + refresh + 실패 처리) | 5.5 | D7 |

**Total: 7 Epic PR · 총 30 SP** (M6 20.5 SP 대비 +46% · **도전적**. 인프라 실물 신설이 절반 · AWS 리소스 실제 발생 · dry-run 병행. Reviewer 5관점 세션 7회 필수).

**본 버전 제외 사유**:
- **AIR Epic 3·4·5** (Roadmap/Selection draft · 사용자 액션 · Commit) — v2 이관 유지
- **AS Epic 4~5 완주분 튜닝** — M6 안정. M7는 관측만
- **부하 테스트** — v1 이후 이관 유지
- **미디어 CDN 세부 · 검색** — v2 이관 유지
- **다중 리전** — v1 최소 인스턴스 유지 · v2 이후
- **APM (분산 트레이싱)** — v2 이관 유지

---

## 진행 중 Product 잔여 인벤토리 (Before/After — M7 진입 시 vs 종료 후 예상)

| Product | 총 Story | M7 진입 시 완료 | M7 진입 시 잔여 | M7 대상 | **M7 종료 후 예상 잔여** | **해결율** |
| --- | --- | --- | --- | --- | --- | --- |
| 1. 인증 (`product-auth.md`) | 10 | 10 | 0 | — | 0 | ✅ 완주 |
| 2. **User BC** (`Product.md`) | 8 | 7 | 1 | **1 Story (잔재)** | 0 | ✅ 완주 |
| 3. Learning Tower (`product-learning-tower.md`) | 42 | 32 | 10 | — | 10 | 0% |
| 4. **AI Suggestion** (`product-ai-suggestion.md`) | 34 | 23 (E1~E5) | 11 | **4 Story (E6 관측성)** | **7** | **36%** |
| 5. **AI Interactive Roadmap** (`product-ai-interactive-roadmap.md`) | ~19 | 4 (E1) | ~15 | **4 Story (E2 axisDraft)** | **~11** | **27%** |
| 6. Card (`product-card.md`) | 16 | 16 | 0 | — | 0 | ✅ 완주 |
| 7. Review (`product-review.md`) | 23 | 23 | 0 | — | 0 | ✅ 완주 |
| 8. **컨테이너 배포** (`product-infra-deploy.md`) | 9 | 5 | 4 | **4 Story (Epic 1~4)** | **0** | **100%** ✅ |
| 9. **AWS 네트워크** (`product-infra-network.md`) | 8 | 3 | 5 | **3 Story (Epic 1~3)** | **2** | **60%** |
| 10. **Secrets·백업·관측** (`product-infra-ops.md`) | 10 | 2 | 8 | **2 Story (Epic 1~2)** | **6** | **25%** |
| 11. **로깅** (`product-log.md`) | 9 | 3 | 6 | **5 Story (Epic 1~4)** | **1** | **83%** |
| 12. **메트릭** (`product-op.md`) | 7 | 3 | 4 | **3 Story (Epic 1~3)** | **1** | **75%** |
| 13. 검색 (`product-search.md`) | ~18 | 0 | ~18 | — | ~18 | 0% (v2) |
| 14. 미디어 (`product-media.md`) | 보류 | 0 | — | — | — | — |
| 15. 캐시 (`product-cache.md`) | — | 0 | — | — | — | — |
| 16. 알림 (`product-notification.md`) | — | 0 | — | — | — | — |
| 17. Admin (`product-admin.md`) | — | 0 | — | — | — | — |
| 18. FE CDN (`product-fe-cdn.md`) | — | 0 | — | — | — | — |
| 19. 부하 테스트 (`product-load-test.md`) | 6 | 0 | 6 | — | 6 | 0% (v1 이후) |
| **합계** | **~206** | **~131** | **~75** | **26 Story · 7 Epic PR · 30 SP** | **~50** | **35%** ↑ |

### 📊 M7 예상 성과 카드

- **총 해결 대상**: 26 Story (전체 잔여 ~75의 **35% 소진**)
- **완주 예상 SDD Product**:
  - `product-infra-deploy.md` — 완주 100% (Dockerfile · ECR · ECS · GHA OIDC · 환경 분리)
  - `product-log.md` — 83% (Epic 1~4 · Slack 배포 이벤트 포함 · Epic 5 잔여 1건 M8)
  - `product-op.md` — 75% (Actuator · Prometheus · Grafana · Epic 4 잔여 1건 M8)
  - User BC — 완주 100% (잔재 1 Story 소진)
- **부분 진행 SDD Product**:
  - `product-infra-network.md` 60% (Epic 1~3 · CloudFront 잔여)
  - `product-infra-ops.md` 25% (Secrets + PITR/CloudWatch · 나머지 6 Story M8 or v2)
  - `product-ai-suggestion.md` E6 완주 · SUPERSEDED 잔재 7 Story는 M8 or v2
  - `product-ai-interactive-roadmap.md` E2 완주 · Epic 3·4·5는 v2 이관
- **M7 종료 후 남는 것** (다음 마일스톤 트라젝토리):
  - **M8 (릴리스 준비 주간)**: fix 이슈 소진 + UX 5시나리오 3명 실측 + Reviewer 5관점 · 파이프라인 dry-run + 프로덕션 문서화
  - v2 이관: AIR Epic 3·4·5 · AS SUPERSEDED 잔재 · LT Epic 3 SUPERSEDED 잔재 · 검색 · 미디어 · 캐시 · 알림 · Admin · 부하

---

## Epic PR 매트릭스 (본주 잡힌 양)

Epic 단위 PR 진행 원칙 (M6 유지):
- **한 Epic PR = 한 논리 단위 = 한 base 브랜치**. Squash merge 또는 rebase merge.
- Epic PR 안의 Story 커밋은 순차 누적 (`feat(scope): ...`).
- Reviewer 세션은 Epic PR 단위 (PR 하나에 5관점 병렬 발사).
- **본 M7은 AWS 실물 리소스 신설을 포함하므로 각 PR의 Reviewer에 Infra 관점 강조**.

### Epic PR #1 — `INFRA-DEPLOY-CI` (Dockerfile + ECR + ECS Fargate + GHA OIDC + 환경 분리)

**Base 브랜치**: `feat/046-infra-deploy-ci`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-infra-deploy.md`
- Epic 범위: Epic 1 (Dockerfile) · Epic 2 (ECS Fargate + IAM 3-role) · Epic 3 (GHA OIDC + rolling deployment + auto-rollback) · Epic 4 (환경 분리 dev/staging/prod + branch protection)

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | INFRA-DEPLOY E1 S1-1 | 단일 Dockerfile + ECR lifecycle policy + git_sha 태그 | 1 |
| 2 | INFRA-DEPLOY E2 S2-1 | ECS Fargate cluster + Task Definition + IAM 3-role (task execution · task · CI) | 2 |
| 3 | INFRA-DEPLOY E3 S3-1 | GHA OIDC → ECS rolling deployment + auto-rollback | 2 |
| 4 | INFRA-DEPLOY E4 S4-1 | 환경 분리 (dev/staging/prod) + branch protection (main·develop) | 1 |

**PR 종료 신호**:
- `Dockerfile` 루트에 존재 · `docker build .` 성공 · 이미지 ECR push 성공
- ECR lifecycle policy: 최근 10개 이미지만 유지 · 오래된 이미지 자동 정리
- ECS Fargate cluster `thirdtool-prod` · Task Definition v1 등록 · Task 시작 · Health Check green
- IAM 3-role 확립: task execution role (ECR pull) · task role (Secrets Manager · CloudWatch) · CI role (OIDC · ECS deploy)
- `.github/workflows/deploy.yml` GHA OIDC 인증 성공 · 자동 rollback (health check 실패 시)
- branch protection: `main`·`develop`에 direct push 금지 · PR review 필수 · CI status 통과 필수

**병렬 진입 조건**: PR#2 (Network)와 동시 착수 가능 (D1). Network가 먼저 완주해야 Deploy 실제 배포 가능하지만 IAM/ECR/Dockerfile은 독립.

**Reviewer 세션**: 5관점 발사. **특히 Architecture Reviewer + Sceptical Reviewer가 "IAM 3-role의 최소 권한 준수 · 오래된 이미지 정리 정책 · rollback 자동성"을 판정**.

### Epic PR #2 — `INFRA-NETWORK-AWS` (VPC + ALB + Route53 + ACM + RDS)

**Base 브랜치**: `feat/047-infra-network-aws`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-infra-network.md`
- Epic 범위: Epic 1 (VPC CIDR + 2 AZ + 6 subnet + 5 SG) · Epic 2 (ALB + Route53 + ACM TLS + host-header routing) · Epic 3 (RDS Multi-AZ prod + single-AZ staging + PITR 7day)

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | INFRA-NW E1 S1-1 | VPC CIDR `10.0.0.0/16` · 2 AZ · 6 subnet (public 2 · private-app 2 · private-db 2) · 5 SG | 2 |
| 2 | INFRA-NW E2 S2-1 | ALB + Route53 A record + ACM TLS 인증서 + host-header routing (`api.thirdtool.io` · `www.thirdtool.io`) | 2 |
| 3 | INFRA-NW E3 S3-1 | RDS MySQL 8 Multi-AZ prod + single-AZ staging + PITR 7일 + auto backup window | 1 |

**PR 종료 신호**:
- VPC 생성 · 2 AZ subnet · 5 SG (ALB · ECS task · RDS · Bastion · VPC endpoint) 각각 최소 권한 규칙
- ALB target group · Route53 A record → ALB · ACM TLS 인증서 자동 갱신 활성
- `https://api.thirdtool.io/actuator/health` → 200 (실제 접속 가능)
- RDS endpoint 접속 · `mysql -h ... -u thirdtool -p` 연결 확인 · Flyway V1~V39 프로덕션 착지
- PITR 7일 활성 · daily backup window 04:00 KST

**의존**: PR#1 (Deploy CI) 병행 진행 가능. RDS endpoint 확립 후 실 배포 실행.

**Reviewer 세션**: 5관점 발사. **특히 Architecture + Sceptical Reviewer가 "VPC 서브넷 구조·SG 최소 권한·RDS 백업 정책이 프로덕션 급 안정성인가"를 판정**.

### Epic PR #3 — `INFRA-OPS-SECRETS` (Secrets Manager + PITR + CloudWatch Alarms)

**Base 브랜치**: `feat/048-infra-ops-secrets`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-infra-ops.md`
- Epic 범위: Epic 1 (Secrets Manager 5-secret integration · Spring Boot boot-time loading) · Epic 2 (RDS PITR backup drill + S3 versioning + CloudWatch Alarms 5종)

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | INFRA-OPS E1 S1-1 | Secrets Manager 5 secret 신설 (db-master · jwt-signing · vertex-ai-key · kakao-oauth · smtp) + Spring Boot boot-time loading | 2 |
| 2 | INFRA-OPS E2 S2-1 | RDS PITR restore drill + S3 versioning (백업 · lifecycle) + CloudWatch Alarms 5종 (RDS CPU · connection · ECS memory · ALB 5xx · APM error rate) | 2 |

**PR 종료 신호**:
- Secrets Manager 5 secret 존재 · IAM task role이 read 권한 · Spring Boot 부팅 시 자동 로드
- `application-prod.yml`에서 credentials가 Secrets Manager 참조 (직접 값 없음)
- PITR restore drill 성공 (임의 시점 복원 → staging 검증)
- CloudWatch Alarms 5종 SNS · Slack 알림 배선
- S3 `thirdtool-backup` 버킷 · versioning ON · lifecycle 90일

**의존**: PR#1 + PR#2 완주 후 진입 (ECS + RDS + IAM 확립 후에만 Secrets 로드 검증 가능).

**Reviewer 세션**: 5관점 발사. **특히 Sceptical Reviewer가 "PITR restore drill 실행 가능 · Secrets rotation 방침 · Alarm 임계값 적정성"을 판정**.

### Epic PR #4 — `LOG-BASELINE` (JSON logstash + MDC + GlobalExceptionHandler + Slack)

**Base 브랜치**: `feat/049-log-baseline`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-log.md`
- Epic 범위: Epic 1 (JSON logstash-encoder) · Epic 2 (MDC requestId/userId propagation · thread-safe context) · Epic 3 (GlobalExceptionHandler level-mapping · ERROR for 5xx only) · Epic 4 (배포 이벤트 Slack 최소)

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | LOG E1 S1-1 | JSON logstash-encoder + structured logging + `logback-spring.xml` prod 프로필 | 1 |
| 2 | LOG E2 S2-1 | MDC requestId 자동 부여 + userId propagation | 0.5 |
| 3 | LOG E2 S2-2 | Thread-safe MDC context + Async 로그 propagation | 1 |
| 4 | LOG E3 S3-1 | GlobalExceptionHandler level-mapping (ERROR for 5xx · WARN for 4xx · INFO for 2xx) | 0.5 |
| 5 | LOG E4 S4-1 | 배포 이벤트 Slack 최소 (GHA post-deploy 알림) | 1 |

**PR 종료 신호**:
- 로컬 prod 프로필 부팅 시 JSON 로그 출력 · CloudWatch Logs 자동 수집 (M7 배포 후)
- 각 요청 로그에 `requestId=uuid` · `userId={...}` MDC 필드 포함
- 5xx 응답 발생 시 로그 level=ERROR · 4xx는 WARN
- GHA 배포 완료 시 Slack `#thirdtool-deploy` 채널에 배포 알림 (버전 · git_sha · 배포자)

**의존**: PR#1 (Deploy CI) 완주 후 Slack 알림 실검증 가능.

**Reviewer 세션**: 5관점 발사. **특히 Architecture Reviewer가 "MDC propagation · thread-safe · Async 정합성"을 판정**.

### Epic PR #5 — `OP-BASELINE` (Actuator + Prometheus + Grafana baseline)

**Base 브랜치**: `feat/050-op-baseline`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-op.md`
- Epic 범위: Epic 1 (Actuator + Prometheus metrics exposure) · Epic 2 (Prometheus scrape + docker-compose monitoring stack) · Epic 3 (Grafana dashboard · P50/P95/P99 · JVM · DB connection pool)

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | OP E1 S1-1 | Actuator + Micrometer + Prometheus metrics endpoint (`/actuator/prometheus`) | 1 |
| 2 | OP E2 S2-1 | Prometheus scrape config + docker-compose monitoring stack (Grafana + Prom) | 1 |
| 3 | OP E3 S3-1 | Grafana dashboard baseline (P50/P95/P99 latency · JVM memory · DB pool · HTTP 5xx rate) | 1 |

**PR 종료 신호**:
- `/actuator/prometheus` → 200 + Micrometer 메트릭 (JVM · HTTP · DB pool · GC)
- Prometheus scrape config 정합 · 15초 주기 수집
- Grafana dashboard 4 판넬 (Latency · JVM · DB · 5xx) 실측 데이터 표시
- 프로덕션 배포 후 Grafana에서 실 트래픽 관찰 (M7 후반부)

**의존**: PR#1 + PR#2 완주 후 프로덕션에서 실측 관찰 가능.

**Reviewer 세션**: 5관점 발사. **특히 Sceptical Reviewer가 "Grafana 판넬이 사용자 UX 임계값을 시각화하는가"를 판정**.

### Epic PR #6 — `AS-E6-OBSERVABILITY` (thirdtool.suggestion.* + MDC + ErrorCode + Runbook)

**Base 브랜치**: `feat/051-as-e6-observability`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-ai-suggestion.md`
- Epic 범위: `# [Epic 6] 관측성 · thirdtool.suggestion.* 메트릭` (line 1919~2099)
- 상위 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-09-ai-suggestion-3layer.md`

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | AS E6 S6-1 | `thirdtool.suggestion.*` 메트릭 등록 (호출 · 성공 · 실패 · latency · provider별) | 1 |
| 2 | AS E6 S6-2 | MDC 로그 필드 확장 (`port`·`provider`·`cascadeFallbackReason`·`cost.estimate`) | 0.5 |
| 3 | AS E6 S6-3 | ErrorCode 5종 등록 (`SUGGESTION_LLM_UNAVAILABLE`·`SUGGESTION_LLM_TIMEOUT`·`SUGGESTION_PARSE_FAILED`·`SUGGESTION_RATE_EXCEEDED`·`SUGGESTION_UNSUPPORTED_ROLE`) | 0.5 |
| 4 | AS E6 S6-4 | Runbook 발행 (`docs/runbooks/suggestion.md`) · 장애 대응 절차 | 0.5 |

**PR 종료 신호**:
- Grafana에서 `thirdtool.suggestion.calls_total{provider="llm"}` · `duration_p95` 메트릭 조회 성공
- 로그 MDC에 확장 필드 포함 · JSON 형식 유효
- ErrorCode 5종 등록 · Swagger UI 명세 반영 · Controller Slice 테스트 통과
- `docs/runbooks/suggestion.md`에 장애 시나리오 5개 · 대응 절차 명시 (Rate limit · Vertex AI quota · timeout · parse error · 5xx)

**의존**: PR#5 (Prometheus + Grafana) 완주 후 진입.

**Reviewer 세션**: 5관점 발사. **특히 Sceptical Reviewer가 "Runbook의 장애 시나리오가 M6 실측 데이터에 기반하는가"를 판정**.

### Epic PR #7 — `AIR-E2-AXIS-DRAFT` (AxisDraftSnapshot + 6-Port + refresh + 실패)

**Base 브랜치**: `feat/052-air-e2-axis-draft`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-ai-interactive-roadmap.md`
- Epic: `# [Epic 2] axisDraft 흐름 (6-Port 호출 + refresh + 챕터 재생성)` (line 695~897)
- Story 범위: `## [Story 2-1]` (line 731) ~ `## [Story 2-4]` (line 861)
- 상위 이슈 원천: `issue-17-ai-two-step-generation.md` + `issue-18-node-regeneration-with-hint.md`

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | AIR E2 S2-1 | `AxisDraftSnapshot` VO + Session 자식 저장 (Flyway V40) | 1 |
| 2 | AIR E2 S2-2 | `POST /roadmap-sessions/{id}/axis-drafts` — 최초 6-Port 호출 (Layer/Axis + Roadmap outline/subtree 초안) | 2 |
| 3 | AIR E2 S2-3 | `POST /roadmap-sessions/{id}/axis-drafts/refresh` — 재요청 (기존 snapshot 대체) | 1.5 |
| 4 | AIR E2 S2-4 | 4-Port 호출 실패 응답 처리 (`suggestionsAvailable: false` fallback · retry 정책) | 1 |

**PR 종료 신호**:
- V40 성공 · `roadmap_axis_draft` 테이블 + FK to `roadmap_interaction_session`
- `POST /roadmap-sessions/{id}/axis-drafts` → 200 + `AxisDraftSnapshot` (Layer·Axis·Roadmap outline·subtree 4-Port 응답 통합)
- 세션 상태 자동 전이 `INIT → LAYERS_DRAFTED → AXES_DRAFTED → CHAPTERS_DRAFTED → SUBTREES_DRAFTED`
- refresh 호출 시 기존 snapshot 대체 · 이력 로그 남김
- 4-Port 중 1개 이상 실패 시 부분 응답 or 전체 폴백 (ADR010 정합)

**의존**: PR#6 (관측성) 완주 후 진입 · Session Aggregate (M6 E1) 안정.

**Reviewer 세션**: 5관점 발사. **특히 Sceptical Reviewer가 "6-Port 호출 실패 처리가 사용자 UX에 자연스러운가"를 판정**.

---

## Story 카테고리별 합계 (SDD Story 단위)

| 카테고리 | SDD Epic/Story | Story 수 | SP | 비중 |
| --- | --- | --- | --- | --- |
| Infra Deploy (Dockerfile · ECR · ECS · GHA) | E1~E4 | 4 | 6 | 20% |
| Infra Network (VPC · ALB · Route53 · RDS) | E1~E3 | 3 | 5 | 17% |
| Infra Ops (Secrets · PITR · CloudWatch) | E1~E2 | 2 | 4 | 13% |
| Log Baseline (JSON · MDC · Handler · Slack) | E1~E4 | 5 | 4 | 13% |
| Op Baseline (Actuator · Prometheus · Grafana) | E1~E3 | 3 | 3 | 10% |
| AS Epic 6 (관측성) | AS E6 S6-1~S6-4 | 4 | 2.5 | 8% |
| AIR Epic 2 (axisDraft) | AIR E2 S2-1~S2-4 | 4 | 5.5 | 19% |
| **합계** | | **25** | **30** | 100% |

**분배 근거**:
- M6 실 착지 ~20.5 SP 대비 +46%. AWS 리소스 실물 신설이 절반 · 실측 검증 대·M8 릴리스 준비 대비.
- **인프라 카테고리 60%** (Deploy + Network + Ops + Log + Op) — 첫 프로덕션 URL 도달 · Grafana 첫 관찰 · Slack 배포 이벤트 · Secrets 관리 착지.
- **AS Epic 6 8%** + **AIR Epic 2 19%** — 관측성 배선 + Session Wizard axisDraft 흐름 완주. v1 릴리스 필수 라인.
- **7개 Epic PR**: PR 수가 M4~M6 대비 많음. 인프라 특성상 각 Product Epic이 독립적. Reviewer 세션 7회.
- **SDD Story 규칙 준수**: 모든 M7 작업이 5개 Infra Product · AS · AIR SDD Epic/Story에 대응.

---

## 종료 신호 — "프로덕션 첫 URL 도달 + Grafana 관찰 배선 + Session axisDraft 완주"

본주 종료 시점에 다음이 모두 성립해야 한다. (10 신호 중 8개 이상 → 0.0.7v 동결 · M8 진입)

- [ ] **머지 신호**: Epic PR 7개 중 최소 6개 머지 (85%)
- [ ] **프로덕션 URL 신호**: `https://api.thirdtool.io/actuator/health` → 200 (프로덕션 첫 접속 성공)
- [ ] **RDS MySQL 신호**: Flyway V1~V40 프로덕션 착지 · `SELECT COUNT(*) FROM flyway_schema_history` → 40+
- [ ] **ECS Task 신호**: ECS Fargate task 실행 · Health Check green · GHA rolling deployment 성공
- [ ] **Secrets Manager 신호**: `application-prod.yml`에서 credentials 직접 값 없음 · Secrets Manager 참조 · 부팅 시 자동 로드 검증
- [ ] **관측 신호**: Grafana dashboard에서 P95 latency · JVM · DB pool · 5xx rate 실측 관찰 · Prometheus scrape 정합
- [ ] **Slack 배포 이벤트 신호**: GHA 배포 완료 시 `#thirdtool-deploy` 채널 알림 도착
- [ ] **AS Epic 6 신호**: Grafana에서 `thirdtool.suggestion.*` 메트릭 조회 성공 · Runbook 5 시나리오 존재
- [ ] **AIR Epic 2 신호**: `POST /roadmap-sessions/{id}/axis-drafts` → 200 · Session 상태 자동 전이 확인 · refresh 정합
- [ ] **테스트 신호**: 7개 Epic PR 각각 도메인 단위 · Repository Slice · Controller Slice 통과. `./gradlew test` BUILD SUCCESSFUL. 통합 테스트 시나리오 (E2E · 프로덕션 사이클 3건)

**미합격 처리**: 위 10 신호 중 8개 미만 성립 시 M7를 0.0.7v로 동결하지 않고 0.0.7.1v 패치 발행 → **릴리스 지연 위험 발동**. AIR E2가 미달일 경우 v2 이관 검토.

---

## 의존 chain

```
[선행: M6 착지]
AS Epic 4·5 (LLM Adapter + Cascade + Rate Limit) ✅ 완료
AIR Epic 1 (Session Aggregate + 상태기계) ✅ 완료

[D1~D2: 병렬 착수 · AWS 신설]
PR#1 (INFRA-DEPLOY-CI · Dockerfile + ECR + ECS + GHA)  ──►  PR#2 (INFRA-NETWORK-AWS · VPC + ALB + RDS)
      │                                                          │
      └──────────────────┬───────────────────────────────────────┘
                         ▼
                 PR#3 (INFRA-OPS-SECRETS · Secrets + PITR + Alarms)
                         │
                         ▼
                 PR#4 (LOG-BASELINE · JSON + MDC + Slack)  ──►  PR#5 (OP-BASELINE · Actuator + Prometheus + Grafana)
                                                                     │
                                                                     ▼
                                                             PR#6 (AS-E6-OBSERVABILITY)
                                                                     │
                                                                     ▼
                                                             PR#7 (AIR-E2-AXIS-DRAFT)
                                                                     │
                                                                     ▼
                                                             프로덕션 배선 완주 · M8 진입 준비
```

**병렬 진입 가능 묶음**:
- **A** (D1 · 08-05 Wed): PR#1 + PR#2 병렬 착수 (Deploy CI 코드 + AWS 리소스 신설)
- **B** (D2 · 08-06 Thu): PR#1 · PR#2 진행 · 부분 완주 시작
- **C** (D3 · 08-07 Fri): PR#1 · PR#2 완주 → 머지 · PR#3 착수 (Secrets · PITR)
- **D** (D4 · 08-08 Sat): PR#3 완주 → 머지 · PR#4 + PR#5 병렬 착수 (Log + Op baseline)
- **E** (D5 · 08-09 Sun): PR#4 · PR#5 완주 → 머지 · PR#6 착수 (AS 관측성)
- **F** (D6 · 08-10 Mon): PR#6 완주 → 머지 · PR#7 착수 (AIR axisDraft)
- **G** (D7 · 08-11 Tue): PR#7 완주 → 머지 · 통합 E2E 검증 · `outcome.md` · `infra.md` · `performance.md` · `cost.md` · `review.md` · `eval.md` 골격 · 0.0.7v 동결 판정

**직렬 (M7 합격선까지)**: PR#1·PR#2 (D1-D3) → PR#3 (D4) → PR#4·PR#5 (D4-D5) → PR#6 (D5-D6) → PR#7 (D6-D7).

---

## 작업 일정 (Epic PR 단위 체크리스트)

D1 = 2026-08-05 (Wed). 종료 D7 = 2026-08-11 (Tue). 7일 안에 7 Epic PR.

| 일 | 날짜 | 잡힌 작업 |
| --- | --- | --- |
| D1 (수) | 08-05 | PR#1 착수 (**Dockerfile + ECR** · Epic 1) + PR#2 착수 (**VPC + 6 subnet + SG** · Epic 1 병렬) |
| D2 (목) | 08-06 | PR#1 **ECS Fargate + IAM 3-role** (Epic 2) + PR#2 **ALB + Route53 + ACM** (Epic 2) |
| D3 (금) | 08-07 | PR#1 **GHA OIDC + rolling deploy** (Epic 3) + **환경 분리** (Epic 4) 완주 → **PR#1 머지 + Reviewer 세션 (Architecture 강조)** · PR#2 **RDS Multi-AZ + PITR** (Epic 3) 완주 → **PR#2 머지 + Reviewer 세션 (Sceptical 강조 · SG · 백업)** · PR#3 착수 (**Secrets Manager 5 secret**) |
| D4 (토) | 08-08 | PR#3 **PITR restore drill + CloudWatch Alarms** 완주 → **PR#3 머지 + Reviewer 세션 (Sceptical 강조)** · PR#4 착수 (**JSON logstash + MDC** · Epic 1~2) · PR#5 착수 (**Actuator + Prometheus** 병렬 · Epic 1) |
| D5 (일) | 08-09 | PR#4 **GlobalExceptionHandler + Slack 배포 이벤트** 완주 → **PR#4 머지 + Reviewer 세션 (Architecture 강조)** · PR#5 **Grafana baseline dashboard** 완주 → **PR#5 머지 + Reviewer 세션 (Sceptical 강조)** · PR#6 착수 (**AS E6 S6-1~S6-4** 관측성) |
| D6 (월) | 08-10 | PR#6 완주 → **PR#6 머지 + Reviewer 세션 (Sceptical 강조 · Runbook 실측 정합)** · PR#7 착수 (**AIR E2 S2-1~S2-3** axisDraft VO + 6-Port 호출 + refresh) |
| D7 (화) | 08-11 | PR#7 **AIR E2 S2-4** (4-Port 실패 처리) 완주 → **PR#7 머지 + Reviewer 세션 (Sceptical 강조)** · **통합 E2E 검증 3시나리오** (핵심 사이클 10단계 · Session Wizard axisDraft · 프로덕션 URL 접속) · `outcome.md` · `infra.md` · `performance.md` · `cost.md` · `review.md` · `eval.md` 골격 · 0.0.7v 동결 판정 |

**Reviewer 세션 규칙**: 각 Epic PR 머지 직전 5관점 병렬 발사. 사용자 확인 후 머지. **본 M7은 특히 Sceptical Reviewer가 인프라 신설 각 PR별로 프로덕션 급 안정성 판정 강조**.

**Flyway V 버전 순서 관리** (M6에서 V39 소모):
- V40: `roadmap_axis_draft.sql` (PR#7 S2-1) · 프로덕션 첫 착지 시 dev H2 이력과 정합 검증 필수

**AWS 실물 리소스 신설 체크리스트** (PR#1·PR#2·PR#3 진행 중 실물 생성):
- ECR repository `thirdtool` · lifecycle policy 10건 유지
- ECS cluster `thirdtool-prod` · Task Definition v1
- VPC `10.0.0.0/16` · 2 AZ (`ap-northeast-2a`·`ap-northeast-2c`)
- ALB `thirdtool-prod-alb` · target group · listener 443
- Route53 hosted zone `thirdtool.io` · A record `api.thirdtool.io`
- ACM 인증서 `api.thirdtool.io` · auto-renewal
- RDS instance `thirdtool-prod-db` · MySQL 8 · Multi-AZ · PITR 7일
- Secrets Manager 5 secret
- S3 `thirdtool-backup` · versioning · lifecycle 90일
- CloudWatch Alarms 5종 · SNS · Slack webhook

---

## 리스크와 관찰 포인트

| 영역 | 리스크 | 관찰 포인트·완화 |
| --- | --- | --- |
| AWS 리소스 실물 신설 규모 | 30+ AWS 리소스 신설 · Terraform 없이 콘솔 클릭 시 실수 위험 | Terraform 도입 검토 (M8 or v2) · v1은 콘솔 + AWS CLI 스크립트 · 리소스 명명 컨벤션 (`thirdtool-<env>-<resource>`) 강제 |
| ACM 인증서 발행 지연 | ACM DNS 검증 최대 30분 · 발행 실패 시 https 배선 지연 | D1에 ACM 요청 우선 · Route53 CNAME 검증 즉시 반영 · Cloudflare 등 alt 유지 |
| RDS 마이그레이션 실패 | 프로덕션 RDS에 Flyway V1~V40 첫 착지 · 데이터 없음 상태이지만 SQL 실행 실패 위험 | staging RDS에 사전 dry-run · Flyway 명령 로그 관찰 · rollback R-file 사전 준비 (특히 V32/V33/V37) |
| Secrets Manager 로드 실패 | Spring Boot 부팅 시 IAM 권한 실수로 Secrets 미로드 → 부팅 실패 | IAM policy simulation · staging에서 사전 검증 · fallback 크레덴셜 (dev only) |
| ECS Task 시작 실패 | Docker 이미지 · Task Definition · health check 정합 실패 | ECS 이벤트 로그 관찰 · Task 로그 CloudWatch 즉시 확인 · rollback 자동 |
| GHA OIDC 인증 실패 | IAM role trust policy 실수 · OIDC provider 미등록 | staging에서 사전 배포 · GHA log 관찰 |
| Grafana dashboard 배선 | Prometheus scrape 설정 · 인증 · 메트릭 매핑 | docker-compose 로컬 검증 · Grafana provisioning 사용 (config as code) |
| Runbook 실측 정합 (AS E6) | M6 실측 없이 Runbook 작성 시 가상 시나리오 · 실효성 낮음 | M6 실측 로그 (`cascadeFallbackReason` 통계) 활용 · Runbook에 M6 실측 데이터 링크 |
| AIR axisDraft LLM 호출 시 비용 폭주 | Session 진행 시 4-Port 호출 (Layer + Axis + Roadmap outline + subtree) · 반복 요청 시 Vertex AI 비용 | Cascade가 우선 폴백 검토 · Rate Limit이 이미 방어 · `cost.md`에 axisDraft 회당 예산 관찰 |
| Reviewer 세션 병목 (M6 실측) | 7 PR × 5관점 = 35 reviewer 발사 · 병렬 유지 | 병렬 발사 유지 · Infra PR 3건은 Sceptical + Architecture 강조 · Sceptical 반복 판정 문서화 |
| 스코프 확장 압박 | 30 SP 목표 · AWS 신설 지연 시 M8 릴리스 준비 여유 축소 | D3 진행률 확인 · 미달 시 PR#6 (AS E6) 축소 (Runbook만 M8 · 메트릭 등록만 M7) · AIR E2 v2 이관 검토 |
| 프로덕션 최초 배포 후 즉시 관찰 | 배포 후 실 트래픽 없음 · Grafana 판넬 empty | M7 D7에 sanity check 트래픽 5회 · `curl` health check + suggestion 호출 |

---

## 다음 마일스톤 (M8 / 0.0.8v) 후보

**M8 / 0.0.8v (08-12 ~ 08-18) — 릴리스 대비 잔여 소진 + 3명 사용자 UX 실측 + 파이프라인 dry-run**
- **fix 이슈 소진** — M3~M7 관찰 후 잔여 fix 이슈 처리 (예상 5~8건)
- **UX 5시나리오 3명 실측** — release.md §사전 검증 시나리오 5건 (첫 진입 · 카드 저장 · Mode 다운 · Cross-layer · Streak) 각각 3명 완주 확인
- **Reviewer 5관점 발사** — 전체 마일스톤 (M1~M7) 산출물 통합 리뷰 · 특히 Card lifecycle · Review · AI 3종 축 정합
- **파이프라인 dry-run** — 프로덕션 배포 리허설 · rollback 리허설 · Slack 알림 · CloudWatch Alarms 발동 확인
- **릴리스 문서화** — 사용자 안내 문서 (README · 사용 가이드 · privacy 정책 · 서비스 약관 최소판)
- **GO/NO-GO 판정** — release.md §릴리스 성공 기준 6 신호 최종 판정
- **🚀 릴리스** (2026-08-19 수) — 0.1.0v 발행 · 3명 사용자 안내

**Epic PR 예상 3~5건** (fix 이슈 소진 PR + 릴리스 문서 PR + 파이프라인 dry-run 리허설).

---

## Product 상태 전환 신호 (M7 종료 시)

- `in-progress/product-infra-deploy.md` — **완주** (Epic 1~4 착지)
- `in-progress/product-infra-network.md` — **부분 완주** (Epic 1~3 · CloudFront/CDN 잔재 M8 or v2)
- `in-progress/product-infra-ops.md` — **부분 완주** (Epic 1~2 · 나머지 8 Story 중 6건 M8 or v2)
- `in-progress/product-log.md` — **Epic 1~4 완주** (Epic 5는 M8)
- `in-progress/product-op.md` — **Epic 1~3 완주** (Epic 4는 M8)
- `in-progress/product-ai-suggestion.md` — **Epic 6 완주** (SUPERSEDED 잔재 v2 이관)
- `in-progress/product-ai-interactive-roadmap.md` — **Epic 1·2 완주** (Epic 3·4·5는 v2 이관)
- `in-progress/Product.md` (User BC) — **완주 100%** (잔재 1 Story 소진)
- `fix/brainstorming/version/0.0.2v/issue-09` → **resolved** (AS Epic 4·5·6 완주)
- `fix/brainstorming/version/0.0.2v/issue-17` → **resolved** (AIR Epic 1·2 완주)
- `fix/brainstorming/version/0.0.2v/issue-18` → v2 이관 유지 (Roadmap 노드 재생성 API)
- `fix/brainstorming/version/0.0.2v/issue-20` → v2 이관 유지 (비용 예산 cap · M7까지 관찰만)

---

## brainstorming 트리거

본 M7 완료 후 `workflow/task/pes/brainstorming/0.0.8v/` 신설:
- 3명 사용자 안내 문서 초안 (`brainstorming/0.0.8v/user-onboarding-doc.md`) — 첫 진입 유저 안내
- 프로덕션 사용자 실측 UX 관찰 프레임 (`brainstorming/0.0.8v/prod-ux-observation-frame.md`) — 3명 개별 관찰 방법론
- 릴리스 후 로드맵 우선순위 (`brainstorming/0.0.8v/post-release-roadmap.md`) — 0.1.1v · 0.2.0v 스코프 초안
- 관측 지표 임계값 재조정 (`brainstorming/0.0.8v/observability-threshold-tuning.md`) — M7 실측 후 CloudWatch Alarm 임계값 튜닝

---

## 참고

- 잔여 Story 인벤토리 출처: `workflow/task/pes/workspectrum/sdd/in-progress/` 19개 Product 파일
- **핵심 인프라 SDD**: `product-infra-deploy.md` · `product-infra-network.md` · `product-infra-ops.md` · `product-log.md` · `product-op.md`
- M2 pivot 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-09` (AS 3-layer) · `issue-17`·`issue-18` (AIR)
- 마일스톤 패키지 의도: `workflow/task/milestones/references/001.md`
- FE 대응 마일스톤: `workflow/task/pes/fe/fe-milestones/version/0.0.7v/milestone.md`
- 이전 마일스톤: `workflow/task/milestones/version/0.0.6v/milestone.md` — M6 원안 · `../0.0.6v/outcome.md` · `../0.0.6v/review.md` · `../0.0.6v/cost.md` (Vertex AI 첫 실측)
- **첫 릴리스 계획**: `workflow/task/milestones/release/version/0.0.1v/release.md` — 0.1.0v (2026-08-19) 스코프 · M7 목표: "배포 라인 완주 · 관측 baseline · E2E 통합 테스트 · Session Epic 2 완주"
- 본 버전의 산출물 7종: `milestone.md`(본 문서) · **`infra.md`(실측 반영 · AWS 리소스 리스트)** · **`performance.md`(Grafana baseline)** · `outcome.md` · `cost.md` · `review.md` · `eval.md`
- 양식 진화: 본 milestone은 0.0.6v milestone.md 양식 답습. **infra.md는 본 M7이 첫 실제 인프라 콘텐츠 확장 · performance.md도 첫 Grafana baseline 반영**.
- **주요 SDD 참조 비율**:
  - 인프라 5개 Product **~60%** (Deploy + Network + Ops + Log + Op · 17 Story · SP 22)
  - `product-ai-suggestion.md` **~8%** (Epic 6 · 4 Story · SP 2.5)
  - `product-ai-interactive-roadmap.md` **~19%** (Epic 2 · 4 Story · SP 5.5)
  - User BC 잔재 **~1%** (1 Story)
