# M7 / 0.0.7v — Local + Production UX 확인 가이드

> **파일의 역할**: M7 (2026-08-05 ~ 08-11) 완료 시점에 **로컬 + 프로덕션 URL로 BE + FE를 확인 가능한 것**을 정리한다. **본 M7이 첫 프로덕션 URL 도달 마일스톤이므로 종전과 달리 "prod URL" 검증 절이 추가**된다.
>
> **본 M7의 특성 — M7 하이라이트**: "프로덕션 첫 URL 도달 + Grafana 관찰 배선 + Session axisDraft 완주". AWS VPC + ALB + RDS + ECS + Route53 + ACM + Secrets Manager 실물 신설 · Prometheus + Grafana baseline · Slack 배포 이벤트 · AS `thirdtool.suggestion.*` 메트릭 · `roadmap_axis_draft` 신설. Flyway V40 프로덕션 첫 착지.

---

## 확인 목적

- **로컬 (dev H2)**: 여전히 부팅 · Flyway V40 dev H2에도 착지 · axisDraft 흐름 로컬 검증
- **프로덕션 (AWS)**: `https://api.thirdtool.io` 실제 URL 도달 · Grafana 관찰 배선 · Slack 알림 배선
- **9개 축 실 기능 확인**:
  1. Docker 이미지 build → ECR push → ECS Fargate 배포 (INFRA-DEPLOY)
  2. AWS VPC + ALB + Route53 + ACM 인증서 · https 첫 도달 (INFRA-NW)
  3. Secrets Manager 5 secret 부팅 시 자동 로드 (INFRA-OPS)
  4. RDS MySQL 8 · Flyway V1~V40 프로덕션 착지 (INFRA-NW)
  5. JSON logstash 로그 · CloudWatch Logs 수집 · MDC 정합 (LOG)
  6. Actuator + Prometheus + Grafana 4 판넬 실측 (OP)
  7. `thirdtool.suggestion.*` 메트릭 조회 · Runbook 5 시나리오 (AS E6)
  8. **`RoadmapInteractionSession` axisDraft 첫 완주** — Session INIT → SUBTREES_DRAFTED 자동 전이 (AIR E2 M7 하이라이트)
  9. 기존 M6 회귀 없음 — LLM Adapter · Cascade · Rate Limit · Session Aggregate 여전히 동작

---

## 사전 조건

- **로컬**: Java 21 · Node 20+ · pnpm · GCP service account key
- **프로덕션 접속**: AWS IAM 사용자 (콘솔 접근권) · Secrets Manager read 권한 · CloudWatch Logs read
- **AWS CLI 로컬 설정** (`aws configure` 완료)
- 브라우저 (Chrome/Edge · DevTools)
- Postman or `curl`
- Slack `#thirdtool-deploy` 채널 접근권 (배포 알림 확인)
- Grafana 대시보드 접속 URL (예: `https://grafana.thirdtool.io` 또는 SSH 터널)

---

## 로컬 BE 부팅 절차 (dev H2 · Vertex AI 배선)

```bash
# 1. 부팅 (dev 프로필 · V40까지 착지)
export GOOGLE_APPLICATION_CREDENTIALS=~/.config/gcloud/thirdtool-dev-key.json
./gradlew clean bootRun

# 2. 정상 부팅 확인
curl -s http://localhost:8080/actuator/health
# → {"status":"UP"}

# 3. Flyway V40 착지 확인
curl -s http://localhost:8080/actuator/flyway
# → migrations 배열에 V40 성공 상태 (1건 신규)

# 4. Prometheus 메트릭 확인 (M7 신규)
curl -s http://localhost:8080/actuator/prometheus | grep thirdtool_suggestion
# → thirdtool_suggestion_calls_total{provider="llm"} 등 메트릭 노출
```

**M7에서 신규 V버전 1건**:
- V40 `roadmap_axis_draft.sql` — `roadmap_axis_draft` 테이블 신설 (FK to Session)

## 프로덕션 URL 접속 확인 (M7 하이라이트)

```bash
# https 첫 도달
curl -s https://api.thirdtool.io/actuator/health
# → {"status":"UP"}   ★ M7 프로덕션 첫 응답

# TLS 인증서 검증
curl -vI https://api.thirdtool.io 2>&1 | grep -E "issuer|subject|expire"
# → issuer: Amazon RSA 2048 M02 · subject: api.thirdtool.io

# Prometheus 메트릭 프로덕션 조회
curl -s https://api.thirdtool.io/actuator/prometheus | head -30
# → JVM · HTTP · DB pool · thirdtool.suggestion.* 메트릭

# Flyway 프로덕션 이력
curl -s https://api.thirdtool.io/actuator/flyway
# → V1~V40 프로덕션 성공 착지
```

## FE 로컬 부팅 절차

```bash
pnpm install
pnpm dev
# → http://localhost:5173

# 프로덕션 접속 (FE도 CDN 배포된 경우)
open https://www.thirdtool.io
```

---

## 로컬 확인 체크리스트

### ✅ BE 로컬 확인 (25분)

- [ ] **부팅 성공** — `Started ThirdToolApplication` 출력
- [ ] **Flyway V40 이력** — H2 콘솔:
  ```sql
  SELECT version, description, success FROM flyway_schema_history WHERE version = '40';
  ```
- [ ] **`roadmap_axis_draft` 테이블** — H2 콘솔:
  ```sql
  SHOW COLUMNS FROM roadmap_axis_draft;
  ```
  → `id`·`session_id` (FK)·`snapshot_json` (TEXT)·`layer_draft_json`·`axis_draft_json`·`roadmap_outline_json`·`roadmap_subtree_json`·`created_at`·`refreshed_at`
- [ ] **Prometheus endpoint 노출** — `/actuator/prometheus` 200 · Micrometer 메트릭 존재
- [ ] **`thirdtool.suggestion.*` 메트릭** — Prometheus에서:
  ```
  thirdtool_suggestion_calls_total{port="chapters-outline",provider="llm"}
  thirdtool_suggestion_duration_seconds_bucket{port="chapters-outline",provider="llm",le="0.5"}
  thirdtool_suggestion_fallback_total{reason="llm_error"}
  ```
- [ ] **MDC 필드 확장** — 로그 JSON에서:
  ```
  {"@timestamp":"...", "level":"INFO", "requestId":"...", "userId":"...",
   "port":"chapters-outline", "provider":"llm", "duration":"1250", ...}
  ```
- [ ] **ErrorCode 5종 등록** — `ErrorCode.java` grep:
  - `SUGGESTION_LLM_UNAVAILABLE`
  - `SUGGESTION_LLM_TIMEOUT`
  - `SUGGESTION_PARSE_FAILED`
  - `SUGGESTION_RATE_EXCEEDED`
  - `SUGGESTION_UNSUPPORTED_ROLE`
- [ ] **`docs/runbooks/suggestion.md` 존재** — 5 장애 시나리오 (Rate limit · Vertex AI quota · timeout · parse error · 5xx) 대응 절차 명시
- [ ] **`AxisDraftSnapshot` VO 클래스** — grep:
  ```bash
  grep -rn "class AxisDraftSnapshot" src/main/java
  # → 1건 존재
  ```

### ✅ BE 로컬 API 확인 (30분)

**① 🌟 axisDraft 흐름 (AIR E2 · M7 하이라이트)**
```bash
# Session 시작 (M6 유지)
curl -X POST http://localhost:8080/api/v1/roadmap-sessions \
  -H "Authorization: Bearer {JWT}" \
  -d '{"facadeId": 1, "conceptsSnapshot": ["백엔드 개발자"]}'
# → 201 + { id: 1, state: "INIT", ... }

# axisDraft 최초 생성 (M7 신규)
curl -X POST http://localhost:8080/api/v1/roadmap-sessions/1/axis-drafts \
  -d '{"axisName": "하네스 엔지니어링"}'
# → 200
# {
#   "sessionId": 1,
#   "state": "SUBTREES_DRAFTED",   ★ 자동 상태 전이
#   "axisDraftSnapshot": {
#     "layerDraft": [ ... ],
#     "axisDraft": [ ... ],
#     "roadmapOutline": [ ... 5개 챕터 ... ],
#     "roadmapSubtree": [ ... 5개 bodyAsciiTree ... ]
#   },
#   "providerContext": "llm:vertex-ai-gemini-2.5-flash"
# }

# axisDraft refresh (재요청 · 기존 대체)
curl -X POST http://localhost:8080/api/v1/roadmap-sessions/1/axis-drafts/refresh \
  -d '{"axisName": "하네스 엔지니어링", "hint": "테스트 스위트 강조"}'
# → 200 + 새 snapshot · 이전 snapshot 폐기

# 4-Port 중 1개 실패 시나리오 (Vertex AI 강제 실패로 Roadmap 부분 실패)
# → 200 + { "roadmapOutline": null, "suggestionsAvailable": {"roadmapOutline": false, ...} }
# 또는 전체 fallback: providerContext: "static:*"
```

**② 관측 지표 조회 (Prometheus 로컬)**
```bash
# 로컬에서 Prometheus 스크랩 확인
curl -s http://localhost:8080/actuator/prometheus | grep -E "thirdtool_suggestion|http_server_requests"

# 여러 번 호출 후 카운터 증가 확인
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/v1/suggestions/layers -d '{"concepts":["백엔드"]}'
done
curl -s http://localhost:8080/actuator/prometheus | grep 'thirdtool_suggestion_calls_total{.*port="layers"'
# → 카운터 값 증가 확인
```

**③ M6 회귀 검증 (LLM Adapter · Cascade · Rate Limit · Session)**
```bash
# LLM 4 Adapter (M6 유지)
curl -X POST http://localhost:8080/api/v1/suggestions/chapters-outline -d '{"concepts":["백엔드"],...}'
# → 200 + providerContext: "llm:..."

# Cascade 폴백 (M6 유지)
# (Vertex AI disable 후 호출 → static:*)

# Rate Limit 429 (M6 유지)

# Session Aggregate (M6 유지)
curl -X POST http://localhost:8080/api/v1/roadmap-sessions -d '{...}'
```

### ✅ 프로덕션 URL 확인 (30분 · **M7 하이라이트**)

- [ ] **https 도달** — `curl https://api.thirdtool.io/actuator/health` → 200
- [ ] **TLS 인증서 유효** — 만료일 90일 이상 · issuer Amazon RSA
- [ ] **Route53 A record** — DNS 조회 시 ALB 주소 반환
- [ ] **ALB target group** — AWS 콘솔에서 target group healthy count > 0
- [ ] **ECS Fargate task 실행** — `aws ecs describe-tasks --cluster thirdtool-prod` → RUNNING · desired = 1 (v1은 단일 인스턴스)
- [ ] **CloudWatch Logs 수집** — `aws logs tail /ecs/thirdtool-prod --follow` → JSON 로그 실시간
- [ ] **Secrets Manager 로드** — 부팅 시 CloudWatch Logs에서 "Secrets loaded: db-master, jwt-signing, vertex-ai-key, kakao-oauth, smtp" 확인
- [ ] **RDS 접속** — AWS 콘솔에서 RDS status Available · Flyway V1~V40 마이그레이션 이력 확인
- [ ] **Prometheus scrape 정상** — Grafana에서 up{job="thirdtool"} = 1
- [ ] **Grafana dashboard 판넬 4개 실측** — P50/P95/P99 latency · JVM · DB pool · 5xx rate
- [ ] **Slack 배포 알림** — `#thirdtool-deploy` 채널에 최근 배포 (버전 · git_sha · 배포자 · 배포 시각) 도착
- [ ] **CloudWatch Alarms 5종 설정** — RDS CPU · Connection · ECS memory · ALB 5xx · APM error rate 각 SNS · Slack 배선

### ✅ 프로덕션 API 스팟 체크 (15분)

```bash
# 회원가입 · 로그인 (auth 회귀)
curl -X POST https://api.thirdtool.io/api/v1/auth/signup -d '{"email":"...","password":"..."}'

# LearningFacade 진입 (LT 회귀)
curl https://api.thirdtool.io/api/v1/facades/me -H "Authorization: Bearer {JWT}"

# AI Suggestion 프로덕션 첫 호출
curl -X POST https://api.thirdtool.io/api/v1/suggestions/layers \
  -H "Authorization: Bearer {JWT}" \
  -d '{"concepts":["백엔드 개발자"]}'
# → 200 + providerContext: "llm:vertex-ai-gemini-2.5-flash"   ★ 프로덕션 첫 LLM

# Session 시작 · axisDraft 흐름 (프로덕션)
curl -X POST https://api.thirdtool.io/api/v1/roadmap-sessions -d '{"facadeId":1,"conceptsSnapshot":["백엔드"]}'
curl -X POST https://api.thirdtool.io/api/v1/roadmap-sessions/1/axis-drafts -d '{"axisName":"..."}'
```

### ✅ FE 확인 (15분)

- [ ] **FE 로컬 부팅** — `Local: http://localhost:5173/`
- [ ] **프로덕션 FE 접속** — `https://www.thirdtool.io` 렌더 · TLS · 콘솔 에러 없음
- [ ] **M6 컴포넌트 회귀 없음** — `<ProviderContextIndicator>`·`<CascadeFallbackNotice>`·`<RateLimitNotice>`·`<RoadmapSessionEntryButton>` 여전히 동작
- [ ] **`<AxisDraftPanel>` 표시** — Session 상세에서 axisDraft 결과 렌더 (Layer + Axis + Roadmap outline · subtree 통합)
- [ ] **`<AxisDraftRefreshButton>` 동작** — 클릭 시 `POST /axis-drafts/refresh` → 새 snapshot 반영
- [ ] **`<SessionStateBadge>` M7 상태 반영** — `SUBTREES_DRAFTED` 배지 표시 · 상태 진행 시각화
- [ ] **`<PartialFallbackNotice>` 표시** — 4-Port 중 일부만 실패 시 사용자 안내

### ✅ BE+FE 통합 확인 (프로덕션 사이클 45분 · release.md §MVP 컨셉 사이클 예습)

**시나리오 A — 🌟 프로덕션 첫 사이클 (M7 하이라이트)**

1. `https://www.thirdtool.io` 접속 (v1은 FE CDN 별도 or SSR)
2. 회원가입 → 이메일 확인 (실제 SMTP 전송) → 로그인
3. LearningFacade 첫 진입 → concepts chip 3개 저장
4. Layer 1개 생성 → Axis 1개 생성
5. `<RoadmapSessionEntryButton>` 클릭 → Session 시작 → axisDraft 자동 요청 (LLM)
6. **관찰**: 3~7초 응답 · axisDraft 결과 표시 · `<ProviderContextIndicator>` "LLM 응답"
7. axisDraft에서 챕터 5개 확인 → 사용자가 편집 or 확정
8. (Session Epic 3·4·5는 v2 이관이므로 여기서 종료 or 수동 확정)

**시나리오 B — Session axisDraft refresh 흐름**

1. 시나리오 A에서 axisDraft 결과 확인 후
2. `<AxisDraftRefreshButton>` 클릭 → 힌트 없이 refresh
3. **관찰**: 새 응답 · 이전 챕터 리스트 대체 · Session 상태 유지 (SUBTREES_DRAFTED)
4. 힌트 추가 후 refresh (`hint: "테스트 강조"`) → 콘텐츠 변화 관찰

**시나리오 C — Grafana 관찰 확인**

1. Grafana dashboard 접속 (`https://grafana.thirdtool.io`)
2. 4 판넬 확인:
   - P50/P95/P99 latency (수 초)
   - JVM heap · non-heap
   - DB connection pool active/idle
   - HTTP 5xx rate (0 near-real-time)
3. Suggestion dashboard (별도 판넬):
   - `thirdtool.suggestion.calls_total{provider}` 시계열
   - `thirdtool.suggestion.duration_seconds` heatmap
   - `thirdtool.suggestion.fallback_total{reason}` 사유별

**시나리오 D — Slack 배포 알림**

1. GHA 배포 트리거 (`main` 브랜치에 PR 머지) → 자동 배포
2. **관찰**: 5~10분 이내 `#thirdtool-deploy` 채널에:
   ```
   ✅ Deployed v0.0.7 · git_sha=abc1234 · by @junseong-kim · env=prod
   ```
3. CloudWatch Alarm 임계값 강제 발동 (임시로 ALB 5xx 5개 인위 발생) → SNS · Slack `#thirdtool-alerts` 알림 도착

**시나리오 E — 개발자용 · Runbook 5 시나리오 실측**

1. `docs/runbooks/suggestion.md` 확인:
   - Rate limit 초과 → 대응
   - Vertex AI quota exceeded → 대응
   - LLM timeout → 대응
   - LLM parse error → 대응
   - 5xx server error → 대응
2. 각 시나리오에 실측 로그 예시 · 판별 쿼리 · 임시 대응 · 근본 대응 명시

**시나리오 F — 개발자용 · PITR restore drill (dry-run)**

1. RDS 스냅샷 임의 시점 → staging 인스턴스 복원
2. staging에 복원된 DB로 데이터 조회 · Flyway 이력 정합 확인
3. drill 결과 `docs/runbooks/db-recovery.md`에 기록

**시나리오 G — 개발자용 · Secrets rotation dry-run**

1. Secrets Manager에서 `jwt-signing` secret 새 값 저장
2. ECS Task rolling restart 트리거
3. 부팅 시 새 secret 로드 확인 (CloudWatch Logs)
4. 기존 JWT는 만료 처리 (v1은 무효화만 · rotation 자동은 v2)

---

## M7 확인 못하는 것 (M8 or v2 이관)

- ❌ AIR Epic 3·4·5 (Roadmap draft · 사용자 액션 · Commit) (**v2 이관**)
- ❌ 챕터 노드 재생성 API + hint UI (**v2 · 이슈 #18**)
- ❌ AI 비용 예산 cap 자동 컷 (**v2 · 이슈 #20**)
- ❌ 부하 테스트 (**v1 이후**)
- ❌ 검색 · 미디어 CDN 세부 (**v2**)
- ❌ APM (분산 트레이싱) (**v2**)
- ❌ AS SUPERSEDED 잔재 정리 (**M8**)
- ❌ LT E3 SUPERSEDED 잔재 정리 (**M8 or v2**)
- ❌ **M8: 3명 사용자 UX 실측 · fix 이슈 소진 · 파이프라인 dry-run**

---

## 관찰 지표 로깅 확인 (M6 계승 · M7 프로덕션 반영)

M7에서 CloudWatch Logs · Prometheus 실측 배선. 로컬은 stdout · 프로덕션은 CloudWatch.

- **HTTP 요청 로그** (모든 요청 · JSON):
  - `@timestamp · level · requestId · userId · method · path · status · duration_ms`
- **AI Suggestion 로그** (M6 계승 + M7 확장):
  - `port · provider · model · duration · promptTokens · completionTokens · cost.estimate`
  - `cascadeFallbackReason` (있는 경우)
- **Session 상태 전이**:
  - `INFO: session_state_transition sessionId={...} from=INIT to=SUBTREES_DRAFTED`
- **Rate Limit · Error**:
  - `WARN: rate_limit_exceeded ...`
  - `ERROR: suggestion_llm_timeout requestId=... provider=llm timeout_ms=15000`
- **배포 이벤트** (GHA · Slack):
  - `deploy.started git_sha=... env=prod`
  - `deploy.completed git_sha=... env=prod duration=Xs`

---

## 문제 발생 시 (Troubleshooting)

| 증상 | 원인 후보 | 해결 |
| --- | --- | --- |
| ECS Task 시작 실패 | Task Definition · IAM · Docker image 오류 | `aws ecs describe-services --cluster thirdtool-prod` · Task 로그 CloudWatch 확인 |
| `https://api.thirdtool.io` 502 Bad Gateway | ECS target unhealthy · health check 실패 | ALB target group · Task 로그 · `/actuator/health` 응답 확인 |
| Flyway 프로덕션 마이그레이션 실패 | V번호 이력 불일치 · 데이터 백업 없이 실행 | Flyway 로그 · 임시 rollback R-file · 사전 dry-run 재검토 |
| Secrets Manager 로드 실패 with 403 | IAM task role 권한 부족 | policy simulator · `secretsmanager:GetSecretValue` 권한 확인 |
| Prometheus target down | scrape config 잘못 · 인증 필요 | Prometheus targets 페이지 · `/actuator/prometheus` 직접 접근 |
| Grafana 대시보드 empty | Prometheus 데이터 없음 · scrape 실패 | Prometheus timerange · datasource UID 확인 |
| Slack 알림 안 옴 | GHA secret · webhook URL 오류 | GHA Actions 로그 · webhook 수동 테스트 |
| Vertex AI 프로덕션 quota exceeded | GCP daily quota 초과 · 예산 부족 | GCP Console quota · `cost.md` 예산 관찰 |
| axisDraft 응답이 항상 `providerContext: "static:*"` | Vertex AI 배선 실패 · Cascade 즉시 폴백 | `cascadeFallbackReason` 로그 · 크레덴셜 · 네트워크 확인 |
| CloudWatch Alarm 발동 안 함 | 임계값 잘못 · 지표 소스 잘못 | Alarm history · 지표 실측값 · 임계값 재조정 |

---

## 마무리 판정

M7 완료 GO/NO-GO는 아래 6개 조건 모두 만족 시 통과:

- [ ] BE 로컬 부팅 + Flyway V40 착지 + Prometheus 메트릭 노출 + Runbook 5 시나리오 존재
- [ ] **프로덕션 첫 URL 도달** — `https://api.thirdtool.io/actuator/health` 200 + TLS + RDS + Secrets 로드 정합
- [ ] Grafana 4 판넬 실측 데이터 표시 + `thirdtool.suggestion.*` 메트릭 조회 성공
- [ ] Slack 배포 알림 + CloudWatch Alarm 5종 배선 · 임시 발동 검증 (staging에서 하나 이상)
- [ ] BE API 8개 축 실행 확인 (axisDraft · Session · Cascade · Rate · LLM · 프로덕션 회원가입·로그인·suggestion 첫 호출)
- [ ] 7개 시나리오 완주 — 특히 **시나리오 A (프로덕션 첫 사이클) · 시나리오 C (Grafana) · 시나리오 D (Slack)**

→ 조건 만족 시 M7 동결 · **M8 진입** (2026-08-12 Wed) · **08-19 릴리스 6일 남음**.

---

## 참고

- 마일스톤 원본: `./milestone.md`
- 이전 마일스톤 UX 확인: `../0.0.6v/ux-check.md`
- 릴리스 스코프: `../../release/version/0.0.1v/release.md` — 0.1.0v (2026-08-19) 사이클 10단계
- 다음 마일스톤: 08-12 진입 · `../0.0.8v/milestone.md` (릴리스 준비 주간)
- FE 대응 마일스톤: `../../../pes/fe/fe-milestones/version/0.0.7v/milestone.md`
- SDD 참조 (M7 착지):
  - 5개 인프라 Product SDD (`product-infra-deploy` · `product-infra-network` · `product-infra-ops` · `product-log` · `product-op`)
  - `product-ai-suggestion.md` Epic 6 (관측성)
  - `product-ai-interactive-roadmap.md` Epic 2 (axisDraft)
- 07-02 pivot 이슈: `../../../fix/brainstorming/version/0.0.2v/issue-09` · `issue-17` · `issue-18`
- Runbook: `docs/runbooks/suggestion.md` · `docs/runbooks/db-recovery.md` (M7 신설)
- **AWS 리소스 리스트**: `infra.md` §M7 신설 항목 참조
