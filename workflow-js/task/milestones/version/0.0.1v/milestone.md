# M1 / 0.0.1v — Week of 2026-06-23 ~ 2026-06-28

> **마일스톤의 역할**: 큰 작업 단위(Product)는 `workflow/product/pes/`에 정의되어 있고 굴러가고 있다. 본 문서는 **이번 주에 그 Product들에서 얼마만큼을 잡아 갈지의 분배 결정** + **버전 단위 산출물 묶음**.
>
> 한 주 = 한 버전 = `version/{0.0.X v}/` 폴더 하나. 본 버전(0.0.1v)에는 다음 6 파일이 들어간다:
> - `milestone.md` *(본 문서)* — 잡힌 양 + 일정 + 의존
> - [`infra.md`](./infra.md) — 인프라 진척 + 배포 산출물
> - [`performance.md`](./performance.md) — 성능 향상 / baseline 측정
> - [`outcome.md`](./outcome.md) — 사용자·기능 성과
> - [`cost.md`](./cost.md) — 비용 성과 (AWS·LLM·기타)
> - [`review.md`](./review.md) — 회고 + 다음 버전 진입 신호

---

## 0.0.1v 스코프 변경 메모 (M1 확장)

초기 분배안은 "로깅 토대 + AI Epic 2 시작 + User BC 종료" 6 Story였으나, **운영자가 본주 푸시 강도를 높여 다음을 합치기로 결정**:

1. **메트릭 가시화(Product 0-b)를 M1으로 끌어옴** — 로깅과 같이 묶어 인프라 관측성 한꺼번에
2. **배포 자체를 본주 안에 완수** — Product 5(ECS Fargate) + 6(AWS 네트워크) + 7(Secrets·Terraform)에서 **깔 수 있는 만큼** 진입
3. **본주 종료 = 코드 머지가 아니라 dev 환경에 떠 있는 상태** 까지가 합격선

확장 사유: 인프라 의존 chain이 길어 한 주에 나눠 잡으면 매주 의존 대기 발생. 한 번에 끝내는 게 효율적.

---

## 진행 중 Product 잔여 인벤토리 (M1 시점)

| Product | 총 Story | 완료(머지) | 잔여 | M1 대상 |
| --- | --- | --- | --- | --- |
| 1. 인증 인프라 (`in-progress/product-auth.md`) | 10 | ~10 | 0~1 | tail 정리 (예비) |
| 2. User BC 정합성 (`in-progress/Product.md`) | 8 | 6 | 2 | **1 Story** (5-4) |
| 3. AI Suggestion (`in-progress/product-aisuggestion.md`) | 14 | 3 | 11 | **2 Story** (2-1, 2-2) |
| 4. AI Roadmap v2 (`ready/product-ai-interactive-roadmap.md`) | 18 | 0 | 18 | — (v2) |
| 5. 컨테이너 배포 (`in-progress/product-infra-deploy.md`) | 9 | 0 | 9 | **5 Story** (1-1·1-2·2-1·2-2·3-1) |
| 6. AWS 네트워크 (`in-progress/product-infra-network.md`) | 8 | 0 | 8 | **3 Story** (1-1·1-2·2-1) |
| 7. Secrets·Terraform (`in-progress/product-infra-ops.md`) | 10 | 0 | 10 | **2 Story** (1-1·1-2) — Terraform Epic 2는 M2 |
| 8. k6 부하 테스트 (`ready/product-load-test.md`) | 6 | 0 | 6 | — (메트릭 선행 필요, M2) |
| 0-a. 로깅 (`in-progress/product-log.md`) | 9 | 0 | 9 | **3 Story** (1-1·2-1·3-1) |
| 0-b. 메트릭 (`in-progress/product-op.md`) | 7 | 0 | 7 | **3 Story** (1-1·2-1·3-1) |
| **합계** | **99** | **~19** | **~80** | **19 Story + Tier 1-확장 10 Story** |

---

## 본주 잡힌 양 (M1 확장 — 19 Story)

### Tier 1 · Must (M1 합격선 — 15 Story)

| # | Product | Story | 한 줄 | SP |
| --- | --- | --- | --- | --- |
| 1 | 0-a 로깅 | **1-1** Logback + LogstashEncoder + profile 분기 | JSON 포맷 + dev/prod 톤. 다음 Story 기반 | 3 |
| 2 | 0-a 로깅 | **2-1** `MdcLoggingFilter` (requestId echo + finally clear) | `X-Request-Id` 자동 echo. FE 시나리오 05의 5xx 추적 기반 | 3 |
| 3 | 0-a 로깅 | **3-1** `GlobalExceptionHandler` 로그 레벨 분리 | ERROR/WARN/INFO 톤 분리 | 2 |
| 4 | 0-b 메트릭 | **1-1** Actuator + Micrometer Prometheus endpoint | `/actuator/prometheus` 노출 + 화이트리스트 | 3 |
| 5 | 0-b 메트릭 | **2-1** Prometheus + Grafana docker-compose + 볼륨 | 별도 컨테이너로 띄움 (ECS 옆) | 3 |
| 6 | 3 AI 제안 | **2-1** `AxisTopicSuggestionPort` + VO | Story 1-1 패턴 답습 | 2 |
| 7 | 3 AI 제안 | **2-2** `StaticAxisTopicAdapter` | Story 1-2 axis 버전 | 2 |
| 8 | 2 User BC | **5-4** `UserUpdateRequestDTO` 정리 | Product 2 종료 도장 | 2 |
| 9 | 5 ECS | **1-1** 단일 `Dockerfile` 통합 (multi-stage, JDK 21) | 이미지 < 200MB 목표 | 3 |
| 10 | 5 ECS | **2-1** GHA OIDC AssumeRole + Workload Identity | EC2 SSH 키 폐기 진입점 | 3 |
| 11 | 5 ECS | **2-2** ECS Task Definition + Service | dev/prod Cluster 2개. **체크 시점**: 코드 머지 ≠ 완료. #12 VPC + #13 ALB + #14 RDS + #15 Secrets 모두 치환 후 Task RUNNING 도달 시점에 ✓ | 3 |
| 12 | 6 네트워크 | **1-1** VPC 10.0.0.0/16 + 2 AZ + public/app/data 3-layer subnet (ADR015 §대안 비교 D) | 가장 큰 의존 — 다른 인프라 선행. **체크 시점**: 코드 머지 ≠ 완료. ts009 1회 실행 + 6 subnet ID + 5 SG ID 발행 완료 시점에 ✓ | 3 |
| 13 | 6 네트워크 | **1-2** ALB (internet-facing, 1개 공유 + host-header staging routing) + Target Group type=ip + Listener 80→443 (ADR016) | Story 6 1-1 선행. **체크 시점**: 코드 머지 ≠ 완료. ts010 1회 실행 + ALB DNS + 2 Target Group ARN + ACM ISSUED + Route 53 alias record 발행 시점에 ✓ | 3 |
| 14 | 6 네트워크 | **2-1** RDS MySQL prod single-AZ (Multi-AZ는 M2). db.t4g.micro + utf8mb4_unicode_ci + Asia/Seoul (ADR018) | Story 6 1-1 선행. Flyway V1~V13 dev → prod. **체크 시점**: 코드 머지 ≠ 완료. ts011 1회 실행 + DB endpoint 발행 + Story-052(Secrets) 등록 + ECS Task 부팅 시 Flyway 마이그레이션 성공 시점에 ✓ | 3 |
| 15 | 7 Secrets | **1-1** Secrets Manager 5종 비밀 저장 (db·jwt·oauth·gemini) | GitHub Secrets 폐기 | 2 |

**Tier 1 합계: 15 Story · ~39 SP**

### Tier 1.5 · ADR follow-up (즉시 후속)

| # | Product | Story | 한 줄 | SP |
| --- | --- | --- | --- | --- |
| F1 | 5 ECS | **Story-050** Spring forward-headers + graceful shutdown | ADR016/ADR017. application.yml + application-prod.yml 3줄. ts010-3/ts010-4 application 측 전제 차단 + ADR016 deregistration_delay 매칭. ECS Task 첫 배포 시 효과 발현 | 1 |

### Tier 2 · Want (도전 — 4 Story)

| # | Product | Story | 한 줄 | SP |
| --- | --- | --- | --- | --- |
| 16 | 0-b 메트릭 | **3-1** Grafana 4섹션 대시보드 (상태·API·JVM·DB) | 메트릭 가시화 완성 | 2 |
| 17 | 5 ECS | **1-2** git_sha 기반 이미지 태깅 | latest 금지 + 추적 가능 | 2 |
| 18 | 5 ECS | **3-1** ALB Target Group health check 통합 | 자동 롤백 진입점 | 2 |
| 19 | 7 Secrets | **1-2** Spring Boot 부팅 시 Secrets 로딩 (spring-cloud-aws) | 컨테이너 안에서 비밀 주입 | 2 |

**Tier 2 합계: 4 Story · ~8 SP**

### Tier 1-확장 · 1차 배포 완성 (도메인 + HTTPS + FE CDN)

> M1 합격선 이후 추가 당겨옴. 브라우저에서 도메인 주소로 FE + BE 모두 접근 가능한 상태까지를 0.0.1v 범위로 확장.
> **D8 · D9는 코드 변경 완료** (커밋됨). D1 · D10은 사용자 직접 액션.

| # | Story | 한 줄 | SP | 상태 |
| --- | --- | --- | --- | --- |
| D1 | Route53 `thirdtool.dev` 도메인 등록 | 콘솔 직접 구매 (~$12/년). 등록 완료 시 Hosted Zone + NS 자동 생성 | 1 | 사용자 직접 |
| D2 | ACM 인증서 2개 신청 (병렬) | ap-northeast-2 (ALB용) + us-east-1 (CloudFront 필수). DNS 검증 CNAME Route53 자동 추가 → ISSUED | 1 | 대기 |
| D3 | ALB HTTPS 전환 | 443 리스너 추가 + 80 → 301 리다이렉트 + ALB SG 443 인바운드 허용. `.dev` HSTS 강제 | 2 | 대기 |
| D4 | Route53 `api.thirdtool.dev` → ALB | A alias 레코드 생성. BE API 도메인 완성 | 1 | 대기 |
| D5 | S3 FE 버킷 생성 | `thirdtool-fe-prod` 버킷 + 퍼블릭 접근 차단 (OAC 경유만 허용) | 1 | 대기 |
| D6 | CloudFront 배포 생성 | OAC 생성 → cloudfront-dist.json 적용. SPA 404→index.html. TLS 1.2+. us-east-1 ACM 연결 | 2 | 대기 |
| D7 | Route53 `thirdtool.dev`/`www` → CloudFront | A alias 레코드 2건. FE CDN 도메인 완성 | 1 | 대기 |
| D8 | BE CORS + OAuth URI → `thirdtool.dev` 전환 | SecurityConfig + MvcConfig + application-prod.yml. `thirdstool.com` 완전 제거 | 1 | ✅ 완료 |
| D9 | GHA FE 배포 워크플로우 | `deploy-fe.yml` — OIDC AssumeRole + npm build + S3 sync + CF invalidation | 2 | ✅ 완료 |
| D10 | FE `.env.production` 환경변수 | `VITE_API_BASE_URL=https://api.thirdtool.dev`. FE 레포에서 사용자 직접 | 1 | 사용자 직접 |

**Tier 1-확장 합계: 10 Story · ~13 SP** (D8·D9 완료 포함)

---

### 카테고리별 합계

| 카테고리 | Story 수 | SP | 비중 |
| --- | --- | --- | --- |
| BE 코드 (기능) | 6 | 14 | 22% |
| 메트릭 (관측성) | 3 | 8 | 13% |
| 인프라 (배포) | 10 | 24 | 38% |
| 도메인 + CDN (1차 배포 완성) | 10 | 13 | 21% |
| ADR follow-up | 1 | 1 | 2% |
| Tier 2 (Want) | 4 | 8 | 13% |
| **합계** | **29+F1** | **60+** | 100% |

**분배 근거**:
- 평소 속도 6~10 Story/주 × 본주 푸시 강도 ↑ × Claude 보조 = 15~20 Story 가능 범위
- 인프라 Story 비중이 큰 이유: 한 번 깔면 후속 마일스톤 진입 비용이 크게 떨어짐
- AI Suggestion Controller 노출(Story 2-3 이상)은 M2로 미룸 — LLM 어댑터(Epic 3)와 묶는 게 효율적

---

## 종료 신호 — "dev 환경에 떠 있다"

본주 종료 시점에 다음이 모두 성립해야 한다. (Tier 1 기준)

- [ ] **머지 신호**: Tier 1 15 Story 중 최소 12 머지 (80%)
- [ ] **배포 신호**: dev 환경 ECS Fargate Task가 1개 이상 RUNNING 상태로 `ALB → Task → RDS` 라우팅 통과
- [ ] **헬스 신호**: `https://{dev domain}/health` 200 OK 응답
- [ ] **인증 신호**: dev 환경에서 `POST /login` → AT 쿠키 + RT 바디 발급 성공 (시드 사용자 1명 기준)
- [ ] **관측 신호**: `https://{dev domain}/actuator/prometheus` 노출 + Grafana 대시보드 진입 시 메트릭 1건 이상 시각화
- [ ] **로그 신호**: dev 환경 로그가 JSON 라인으로 출력 + `X-Request-Id` 응답 헤더 echo back
- [ ] **비밀 신호**: 컨테이너 안에서 Secrets Manager 비밀이 환경변수로 주입 (GitHub Secrets 의존 0건)
- [ ] **AI 신호**: dev 환경에서 `provider=static`으로 axis · axisTopic 양쪽 정적 응답 가능 (Controller 노출은 X)

**1차 배포 완성 추가 신호** (Tier 1-확장 기준):

- [ ] **도메인 신호**: `https://api.thirdtool.dev/actuator/health` → 200 OK (ALB HTTPS + Route53 alias)
- [ ] **리다이렉트 신호**: `http://api.thirdtool.dev` → 301 → `https://api.thirdtool.dev` (HTTP 강제 전환)
- [ ] **FE 신호**: `https://thirdtool.dev` → FE 메인 페이지 로드 (CloudFront + S3)
- [ ] **E2E 신호**: FE에서 로그인 시도 → `https://api.thirdtool.dev/login` 호출 성공 (CORS OK, SameSite=Strict 쿠키 전달)

**미합격 처리**: 위 8 신호 중 6개 미만 성립 시 M1을 0.0.1v로 동결하지 않고 0.0.1.1v 패치 발행 → 다음주 초까지 연장.
1차 배포 완성 신호 4개는 독립 체크. 기본 8 신호 통과 후 도메인 구매 일정에 따라 순차 달성.

---

## 의존 chain

```
[로깅] 0-a 1-1 → 2-1 → 3-1  (선형, 독립)
[AI]   3 2-1 → 2-2          (선형, 독립)
[User] 2 5-4                (독립)

[인프라 — 의존 큼]
6 1-1 VPC ────────┬── 6 1-2 ALB ───┐
                  └── 6 2-1 RDS    │
                                   │
7 1-1 Secrets ── 7 1-2 Boot 로딩 ──┤
                                   │
5 1-1 Dockerfile ─── 5 1-2 태깅 ──┤
                                   │
5 2-1 OIDC ── 5 2-2 Task Def ─────┼── 첫 배포
                                   │
0-b 1-1 Actuator ── 5 3-1 ALB hc ─┘
                  └── 0-b 2-1 Prom + Grafana ── 3-1 대시보드
```

**병렬 진입 가능 묶음**:
- A: 0-a 로깅 3종 (독립)
- B: 3 AI 2종 (독립)
- C: 2 5-4 User (독립)
- D: 7 1-1 Secrets + 5 1-1 Dockerfile (독립적, 인프라 시작점)
- E: 6 1-1 VPC (단독 — D와 병렬)

**직렬 (배포까지)**: E → 6 1-2 + 6 2-1 → 7 1-2 + 5 2-1 + 5 2-2 → 5 3-1 → 첫 배포 통과

```
[1차 배포 완성 — Tier 1-확장]
D1 도메인 등록 ──┬── D2 ACM 2개 병렬 ──┬── D3 ALB HTTPS ── D4 api 레코드  (BE 도메인 완성)
(콘솔 직접)      │   (ap-ne-2 + us-e-1)  └── D5 S3 버킷 ── D6 CloudFront ── D7 FE 레코드  (FE CDN 완성)
                 │
D8 CORS 전환 ✅ (완료, 독립)
D9 GHA FE WF ✅ (완료, D5·D6 완료 후 첫 실행)
D10 FE env    (FE 레포, 사용자 직접)
```

**주의**: D3은 ACM ap-ne-2 ISSUED 후, D6은 ACM us-e-1 ISSUED 후 진행. `.dev` HSTS — HTTP 서비스 불가, 양쪽 HTTPS 필수.

---

## 작업 일정 (체크리스트)

오늘이 D1(화). 종료가 D6(일). 6일 안에 19 Story.

| 일 | 날짜 | 잡힌 작업 (코드 / 인프라) |
| --- | --- | --- |
| D1 (화) | 06-23 | **코드**: 0-a 1-1 (Logback) + 2 5-4 (DTO 정리) 시작 / **인프라**: 7 1-1 Secrets Manager 5종 비밀 등록 + 5 1-1 Dockerfile 통합 |
| D2 (수) | 06-24 | **코드**: 0-a 1-1 머지 + 0-a 2-1 (MdcLoggingFilter) 시작 + 3 2-1 (Port + VO) / **인프라**: 6 1-1 VPC 구축 |
| D3 (목) | 06-25 | **코드**: 0-a 2-1 머지 + 3 2-1 머지 + 3 2-2 (Static Adapter) / **인프라**: 6 1-2 ALB + 6 2-1 RDS |
| D4 (금) | 06-26 | **코드**: 3 2-2 머지 + 0-a 3-1 (로그 레벨) + 2 5-4 머지 / **인프라**: 7 1-2 (Boot 로딩) + 5 2-1 (OIDC) + 5 2-2 (Task Def) 동시 |
| D5 (토) | 06-27 | **코드**: 0-a 3-1 머지 + 0-b 1-1 (Actuator) + 0-b 2-1 (Prom/Grafana 스택) / **인프라**: 첫 배포 시도 — Task Def + ALB + Secrets 통합 |
| D6 (일) | 06-28 | **검증**: 종료 신호 8개 셀프 체크 + Want Tier 시도 (0-b 3-1 대시보드 / 5 1-2 태깅 / 5 3-1 ALB health) + `infra.md` / `performance.md` / `outcome.md` / `cost.md` / `review.md` 작성 |

> 19 Story가 무리이면 Tier 2 4개를 D6에 모두 미루기 허용. Tier 1만 통과해도 M1 합격.

---

## 리스크와 관찰 포인트

| 영역 | 리스크 | 관찰 포인트 |
| --- | --- | --- |
| 의존 chain | VPC 구축 지연 → ALB/RDS 모두 대기 | D2 종료 시점에 VPC subnet 4개 생성 완료 여부 |
| Secrets 주입 | Spring Boot가 부팅 단계에서 Secrets Manager에 도달 못함 (IAM Role 누락) | 로컬에서 AWS profile로 먼저 검증 후 ECS Task Role 적용 |
| 첫 배포 | Task가 RUNNING이지만 ALB target unhealthy (헬스체크 path 미정) | `/health` 엔드포인트가 SecurityFilterChain의 `permitAll` 안에 있는지 확인 |
| 비용 폭증 | NAT Gateway / RDS Multi-AZ 의도치 않게 켜져 일 비용 급증 | 본주 일 비용 1회/일 확인 — `cost.md` 추적 |
| 로깅 + 메트릭 동시 진입 | Logback / Micrometer 의존성 충돌 | 통합 테스트로 로그 라인 1건 + 메트릭 1건 동시 확인 |
| FE 영향 | UserUpdateRequestDTO 정리(2 5-4)가 FE 호출 깨뜨림 | FE `third-tool-fe/untitled` grep — `UserUpdate.*username` 참조 0건 확인 |
| 본주 속도 | 19 Story가 평소 대비 2배 — 막힘 시 누적 위험 | D3 종료 시점에 8 Story 머지 진행률 (40% 이상) |

---

## 다음 마일스톤 (M2 / 0.0.2v) 후보

본주 결과를 보고 결정하지만, 현재 시점 후보:

- **AI Epic 3 진입** (`in-progress/product-aisuggestion.md`): Story 3-1 Spring AI ChatClient 빈 + 3-2 BeanOutputConverter + 3-3 GCP ADC 인증 — LLM 어댑터 첫 등록
- **AI Suggestion Controller 노출**: FE OnboardingPage가 v1.5 활성 가능해짐
- **인프라 완성**: Product 5 잔여 4 Story (1-3 ECR 라이프사이클 / 2-3 Rolling / 3-2 Circuit Breaker / 3-3 자동 롤백) + Product 6 잔여 5 Story
- **Terraform IaC 진입**: Product 7 Epic 2 (Terraform 모듈 + State + GHA workflow) — 큰 작업이라 단독 마일스톤 가능성
- **k6 부하 테스트**: Product 8 Story 1-1 ~ 1-3 (스크립트 + 4 API 시나리오 + VU 50) — 본주 메트릭 위에서 baseline 측정

**기본 권장 M2**: AI Epic 3 첫 진입 + 인프라 잔여 정리 + k6 baseline 측정 (메트릭 토대 활용).

---

## brainstorming 트리거

본 M1 완료 후 `workflow/product/pes/brainstorming/0.0.2v/` 신설 — 0.0.1v 후보들의 상태 전이 반영:
- 로깅 인프라 진입 → 후보 다수 promoted/resolved
- 메트릭 도입 → ops.md 후보 1·3·8 상태 전이
- 첫 배포 완료 → deploy.md 후보 일부 promoted

---

## 참고

- 잔여 Story 인벤토리 출처: `workflow/product/pes/` 10개 Product 파일 + `git log` 머지 패턴
- 마일스톤 패키지 의도: `workflow/product/milestones/references/001.md`
- 본 버전의 산출물 5종: `infra.md`, `performance.md`, `outcome.md`, `cost.md`, `review.md`
