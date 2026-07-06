# M6 / 0.0.6v — Local UX 확인 가이드

> **파일의 역할**: M6 (2026-07-29 ~ 08-04) 완료 시점에 **배포 없이 로컬에서 BE + FE를 켜서 확인 가능한 것**을 정리한다. 사용자님이 릴리스 없이 진척 상황을 직접 눈으로 볼 수 있는 창.
>
> **본 M6의 특성 — M6 하이라이트**: "LLM 첫 응답 도착 + Cascade 자동 폴백 + Session Wizard 첫 진입". Vertex AI Gemini Flash 2.5로 실제 LLM 호출 첫 발생 · Static→LLM cascade 폴백 원칙 (ADR010) 실동작 · `RoadmapInteractionSession` Aggregate 상태기계 착지. Flyway V39 1건 신규 착지 (Session 테이블).

---

## 확인 목적

- BE·FE가 **여전히 로컬에서 부팅되는가** — Flyway V39 정상 착지 + Spring AI 배선 정합
- **6개 축 실 기능 확인**:
  1. Spring AI + Vertex AI Gemini 2.5 첫 응답 도착 (AS E4 S4-1)
  2. LLM Adapter 4종 (Layer/Axis/Roadmap/Selection) — `provider=llm` 응답 확인 (AS E4 S4-2~4-5)
  3. SuggestionCascade — LLM 실패 시 자동 Static 폴백 (AS E5 S5-1)
  4. Bucket4j Rate Limit — 429 응답 · `SUGGESTION_RATE_EXCEEDED` (AS E5 S5-2)
  5. **`RoadmapInteractionSession` Aggregate + 상태기계 첫 진입** — `POST /roadmap-sessions` → 201 · 상태 전이 예외 검증 (AIR E1 M6 하이라이트)
  6. 기존 M5 회귀 없음 — DailyBatch·Session·Dashboard·Deck 폐기·Layer progressStatus 여전히 동작

---

## 사전 조건

- Java 21 toolchain 활성 (`./gradlew --version` 통과)
- Node 20+ + pnpm 활성 (FE 프로젝트 기준)
- 브라우저 (Chrome/Edge · DevTools Network·Console)
- 로컬 포트: BE 8080 · FE 5173
- Postman or `curl` (BE API 확인)
- **Google Cloud service account key** — `~/.config/gcloud/thirdtool-dev-key.json` 저장 · Vertex AI API 활성 프로젝트
- **M5 로컬 상태**: dev H2 그대로 유지 가능 (V39는 additive 마이그레이션 · dropped column 없음)

---

## BE 로컬 부팅 절차 (dev 프로필 · H2 + Vertex AI 배선)

```bash
# 1. 부팅 (dev 프로필 자동 · Vertex AI 크레덴셜 로드)
export GOOGLE_APPLICATION_CREDENTIALS=~/.config/gcloud/thirdtool-dev-key.json
./gradlew bootRun

# 2. 정상 부팅 확인
curl -s http://localhost:8080/actuator/health
# → {"status":"UP"}

# 3. Flyway V39 착지 확인
curl -s http://localhost:8080/actuator/flyway
# → migrations 배열에 V39 성공 상태 (1건 신규)

# 4. Vertex AI ChatModel bean 배선 확인 (Spring Actuator beans)
curl -s http://localhost:8080/actuator/beans | jq '.contexts.application.beans | keys | map(select(. | contains("chatModel") or contains("vertexAi")))'
# → ["vertexAiGeminiChatModel", ...] 등장
```

**주의**: M6에서 신규 V버전 1건.
- V39 `roadmap_interaction_session.sql` — `roadmap_interaction_session` 테이블 신설

Spring AI 배선 실패 시 부팅 자체가 실패할 수 있음. `application.yml`의 `spring.ai.vertex.ai.gemini.project-id`·`location` 필수 확인.

## FE 로컬 부팅 절차

```bash
pnpm install
pnpm dev
# → http://localhost:5173 부팅
```

`VITE_API_BASE_URL=http://localhost:8080` 설정 확인.

---

## 로컬 확인 체크리스트

### ✅ BE 단독 확인 (20분)

- [ ] **부팅 성공** — `Started ThirdToolApplication` 출력
- [ ] **Vertex AI ChatModel 배선** — `/actuator/beans`에 `vertexAiGeminiChatModel` bean 존재
- [ ] **Actuator health** — UP
- [ ] **Swagger UI 진입** — `/swagger-ui.html` · **suggestion 그룹에 LLM 응답 예시 · `providerContext` 필드 명시**
- [ ] **H2 콘솔** — `/h2-console` 진입
- [ ] **Flyway V39 이력** — H2 콘솔:
  ```sql
  SELECT version, description, success FROM flyway_schema_history
  WHERE version = '39';
  ```
  → 1건 `success=true`
- [ ] **`roadmap_interaction_session` 테이블** — H2 콘솔:
  ```sql
  SHOW COLUMNS FROM roadmap_interaction_session;
  ```
  → `id`·`user_id`·`state` (VARCHAR CHECK IN INIT·LAYERS_DRAFTED·AXES_DRAFTED·CHAPTERS_DRAFTED·SUBTREES_DRAFTED·REVIEWING·COMMITTED·ABANDONED)·`facade_id`·`concepts_snapshot` (TEXT)·`started_at`·`expires_at`·`terminated_at` 존재
- [ ] **프롬프트 template 리소스 존재** — `src/main/resources/ai/prompts/`:
  - `concept-spec.txt` · `layers.txt` · `axes.txt` · `chapters-outline.txt` · `chapter-subtree.txt` · `selection-outline.txt` · `selection-subtree.txt`
  - 각 파일 UTF-8 유효 · 손상 없음
- [ ] **Bucket4j 의존 반영** — `build.gradle`에 `com.bucket4j:bucket4j-core` 명시 · Actuator 로그에 rate limit bucket 배선 반영
- [ ] **`SuggestionCascade` 클래스 존재** — grep:
  ```bash
  grep -rn "class SuggestionCascade" src/main/java
  # → 1건 존재 (AS E5 S5-1)
  ```
- [ ] **`RoadmapInteractionSession` Aggregate 클래스** — grep · `SessionState` enum 7 값 정합

### ✅ BE API 실행 확인 (40분, curl 예시)

이하 예시는 로그인 후 `{JWT}` 획득한 상태 전제.

**① 🌟 LLM Adapter 첫 응답 (AS E4 · M6 하이라이트)**
```bash
# LLM 모드 강제 (Cascade가 우선 LLM 시도)
curl -X POST http://localhost:8080/api/v1/suggestions/layers \
  -H "Authorization: Bearer {JWT}" -H "Content-Type: application/json" \
  -d '{
    "concepts": ["백엔드 개발자"],
    "facadeContext": {"conceptSummary": "..."}
  }'
# → 200
# {
#   "layers": [
#     {"name": "기능의 구현", "rationale": "..."},
#     {"name": "설계 원리", "rationale": "..."},
#     ...
#   ],
#   "providerContext": "llm:vertex-ai-gemini-2.5-flash",   ★ LLM 첫 응답
#   "suggestionsAvailable": true
# }

# Axis Adapter
curl -X POST http://localhost:8080/api/v1/suggestions/axes \
  -d '{"concepts": ["백엔드 개발자"], "layerName": "기능의 구현"}'
# → 200 + providerContext: "llm:vertex-ai-gemini-2.5-flash" + Axis 리스트

# Roadmap chapters-outline (M4에서 Static이었던 것 · M6 LLM 우선)
curl -X POST http://localhost:8080/api/v1/suggestions/chapters-outline \
  -d '{"concepts": ["백엔드 개발자"], "axisName": "하네스 엔지니어링", ...}'
# → 200 + providerContext: "llm:vertex-ai-gemini-2.5-flash" + 5 챕터

# Roadmap chapter-subtree
curl -X POST http://localhost:8080/api/v1/suggestions/chapter-subtree \
  -d '{"chapter": {...}}'
# → 200 + bodyAsciiTree

# Selection Adapter
curl -X POST http://localhost:8080/api/v1/suggestions/selection-outline \
  -d '{"concepts": ["백엔드"], "axisName": "..."}'
# → 200 + providerContext: "llm:vertex-ai-gemini-2.5-flash" + Selection 컨테이너
```

**② 🌟 Cascade 자동 폴백 (AS E5 S5-1 · M6 핵심)**
```bash
# Vertex AI 크레덴셜을 일시 무효화 (or 강제 실패 flag)
# application-dev.yml에 spring.ai.vertex.ai.gemini.enabled=false 임시 설정

curl -X POST http://localhost:8080/api/v1/suggestions/chapters-outline \
  -d '{"concepts": ["백엔드 개발자"], ...}'
# → 200 + providerContext: "static:backend-developer"   ★ Static 자동 폴백
# 로그: cascadeFallbackReason=llm_disabled

# 응답 shape은 LLM 응답과 동일 (Cascade 원칙)

# 크레덴셜 복원 후 재요청 → provider=llm 복귀 확인
```

**③ 🌟 Rate Limit (AS E5 S5-2)**
```bash
# 동일 IP에서 11번 연속 호출
for i in {1..11}; do
  curl -X POST http://localhost:8080/api/v1/suggestions/layers \
    -H "Authorization: Bearer {JWT}" \
    -d '{"concepts": ["백엔드"]}'
  echo "---"
done
# → 처음 10건: 200
# → 11번째: 429 Too Many Requests
# {
#   "code": "SUGGESTION_RATE_EXCEEDED",
#   "message": "요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요."
# }
```

**④ 🌟 Session Aggregate 첫 진입 (AIR E1 · M6 하이라이트)**
```bash
# 세션 시작
curl -X POST http://localhost:8080/api/v1/roadmap-sessions \
  -H "Authorization: Bearer {JWT}" -H "Content-Type: application/json" \
  -d '{
    "facadeId": 1,
    "conceptsSnapshot": ["백엔드 개발자", "기획자"]
  }'
# → 201
# {
#   "id": 1,
#   "state": "INIT",
#   "expiresAt": "2026-07-30T09:00:00Z",   (24h TTL)
#   "startedAt": "2026-07-29T09:00:00Z"
# }

# 세션 조회
curl http://localhost:8080/api/v1/roadmap-sessions/1
# → 200 + 현재 상태 · conceptsSnapshot

# 무효 전이 시도 (INIT → AXES_DRAFTED 직접 · LAYERS_DRAFTED 없이)
# (Epic 2 axis-drafts 엔드포인트 있어야 실제 전이 가능 · M7에 신설)
# 도메인 단위 테스트에서 SessionState.INIT.transitionTo(AXES_DRAFTED) 예외 확인

# abandon
curl -X POST http://localhost:8080/api/v1/roadmap-sessions/1/abandon
# → 200 + state: "ABANDONED" + terminatedAt

# TTL 만료 확인 (dev 프로필에서 5분 TTL 설정 시)
# application-dev.yml: session.ttl.minutes=5
# → 세션 시작 후 5분 대기 · 조회 시 자동 state: "ABANDONED"
```

**⑤ 회귀 검증: M5 시나리오 여전히 동작 (Review BC · DailyBatch · Dashboard)**
```bash
# DailyBatch 오늘 조회 (M5 유지)
curl -X POST http://localhost:8080/api/v1/daily-batch/today
# → 200 + 오늘 batch + entries[]

# ReviewSession 시작 (M5 유지)
curl -X POST http://localhost:8080/api/v1/review-sessions/from-batch
# → 200 + session

# Dashboard (M5 유지)
curl http://localhost:8080/api/v1/dashboard/summary
# → 200 + today/7day/30day + streak

# Deck 폐기 회귀 (M5 유지)
curl http://localhost:8080/api/v1/decks/1
# → 410 Gone

# → 모두 M5 응답과 동일 결과 · M6 재편이 M5 자산 파괴 없음 확인
```

### ✅ FE 단독 확인 (15분)

- [ ] **FE 부팅 성공** — `Local: http://localhost:5173/`
- [ ] **홈 진입 · 콘솔 에러 없음**
- [ ] **M5 신규 컴포넌트 회귀 없음** — `<DailyBatchQueue>`·`<ReviewSessionPage>`·`<DashboardSummary>` M5 동작 유지
- [ ] **`<ProviderContextIndicator>` LLM 배지 지원** — 응답 `providerContext: "llm:vertex-ai-gemini-2.5-flash"` 시 "LLM 응답" 배지 · Static 폴백 시 "Static 폴백" 배지
- [ ] **`<CascadeFallbackNotice>` 표시** — Cascade가 폴백 실행 시 사용자에게 안내 (dev only 배지 or 프로덕션도 표시 결정)
- [ ] **`<RateLimitNotice>` 표시** — 429 응답 시 "잠시 후 재시도 안내" toast
- [ ] **`<RoadmapSessionEntryButton>` 진입** — LearningFacade 화면에서 "AI 로드맵 만들기" 진입 → `POST /roadmap-sessions` → INIT 상태 세션 카드 표시
- [ ] **`<SessionStateBadge>` 표시** — 현재 세션 상태 배지 (`INIT` · `LAYERS_DRAFTED` · ...)
- [ ] **`<SessionExpiresCountdown>` 표시** — TTL 남은 시간 표시 · 만료 임박 시 색상 변경

### ✅ BE+FE 통합 확인 (40분 · 사용자 시점 시나리오)

**시나리오 A — LLM 첫 응답 도착 사용자 흐름**

1. FE 홈 진입 · 로그인 (M5 완주 상태 유지)
2. LearningFacade 진입 → concepts 3개 chip: "백엔드 개발자" · "기획자" · "AI"
3. Axis 상세 → Roadmap 탭 → `<ChaptersOutlineButton>` 클릭
4. `<ChaptersOutlineDialog>` open · 로딩
5. **관찰**: 응답 도착 시간 3~7초 (Vertex AI Gemini Flash 2.5 latency) · `<ProviderContextIndicator>` 배지 "LLM 응답 (Gemini 2.5)"
6. 5개 챕터 표시 · 각 챕터 title + rationale · M4 Static 응답과 콘텐츠 다양성 비교 관찰

**시나리오 B — Cascade 자동 폴백 사용자 흐름**

1. dev 프로필에서 Vertex AI 강제 비활성 (`spring.ai.vertex.ai.gemini.enabled=false`)
2. 시나리오 A 반복
3. **관찰**: 응답 도착 즉시 (< 1초) · `<ProviderContextIndicator>` 배지 "Static 폴백" · `<CascadeFallbackNotice>` toast "LLM 응답 실패 · Static 카탈로그로 대체"
4. 5개 챕터 · M4 M5 Static 응답과 동일 shape · role catalog 정합

**시나리오 C — Rate Limit 사용자 흐름**

1. FE에서 동일 세션에서 `<ChaptersOutlineButton>` 반복 클릭 (11회 이내)
2. **관찰**: 처음 10회는 정상 응답 · 11번째 429 응답 · `<RateLimitNotice>` toast "요청 한도 초과 · 잠시 후 재시도"
3. 1분 후 재시도 → 정상 응답 복귀 (Bucket4j refill)

**시나리오 D — 🌟 Session Wizard 첫 진입 (M6 하이라이트)**

1. LearningFacade 진입 → 새 배너 "AI로 학습 로드맵 초안 만들기" 클릭
2. `<RoadmapSessionEntryButton>` → `POST /roadmap-sessions` → 201 응답
3. Session 카드 표시:
   - `<SessionStateBadge>` "시작 · INIT"
   - `<SessionExpiresCountdown>` "23시간 59분 남음"
4. Session 상세 진입 → 현재는 M6 Epic 1까지만 (Epic 2 axisDraft는 M7)
5. `<SessionAbandonButton>` 클릭 → `POST /roadmap-sessions/{id}/abandon` → 상태 `ABANDONED` 반영
6. **관찰**: 상태 전이 명확 · TTL 실시간 감소 · abandon 후 재진입 시 새 세션 생성 안내

**시나리오 E — 개발자용 · LLM 응답 파싱 실패 판정**

1. dev 콘솔에서 강제 malformed 응답 시뮬레이션 (mock LLM 응답에 JSON 오염 주입)
2. `SuggestionCascade` 로그 관찰:
   - `WARN: llm_parse_failed provider=llm reason=json_parse_error`
   - `INFO: cascade_fallback triggered reason=llm_parse_error target=static`
3. 응답 shape은 유지 (`providerContext: "static:*"`) · 사용자에게는 자연스러운 폴백

**시나리오 F — 개발자용 · Session 상태기계 매트릭스**

1. 도메인 단위 테스트 실행:
   ```bash
   ./gradlew test --tests "com.example.thirdtool.RoadmapInteraction.domain.model.RoadmapInteractionSessionTest"
   ```
2. 테스트 항목:
   - `create_INIT_시작상태` (해피)
   - `transitionTo_INIT_LAYERS_DRAFTED_성공` (해피)
   - `transitionTo_INIT_AXES_DRAFTED_예외` (예외 · 무효 전이)
   - `abandon_INIT_ABANDONED` (해피)
   - `abandon_COMMITTED_불변` (엣지 · 이미 종료된 세션)
   - `expired_INIT_ABANDONED_자동` (엣지 · TTL 만료)
3. 모두 BUILD SUCCESSFUL

**시나리오 G — 개발자용 · Vertex AI 비용 관찰**

1. 로그에서 `cost.estimate` 항목 검색:
   ```bash
   grep "cost.estimate" build/logs/thirdtool.log | tail -20
   ```
2. 각 LLM 호출에 대해 `cost.estimate=Xtokens · provider=vertex-ai · model=gemini-flash-2.5` 기록 확인
3. `cost.md` §M6 초기 실측 · 20~50회 호출 후 총 tokens 합계 · 예상 USD 환산

---

## M6 확인 못하는 것 (다음 마일스톤 이관)

배포 없이 로컬에서 확인 불가한 M6 out of scope 항목:

- ❌ AI Interactive Roadmap Epic 2 (axisDraft 흐름 · 6-Port 호출 · refresh) (**M7**)
- ❌ AS Epic 6 관측성 (`thirdtool.suggestion.*` 메트릭 · MDC 확장 · Runbook) (**M7**)
- ❌ 챕터 노드 재생성 API + hint UI (**v2 · 이슈 #18**)
- ❌ AI 비용 예산 cap 자동 컷 (**v2 · 이슈 #20 · M6은 관찰만**)
- ❌ 배포 라인 · CloudFront · AWS 인프라 · Web Vitals baseline (**M7**)
- ❌ 관측 지표 대시보드 (Grafana) (**M7 · product-op.md**)
- ❌ MySQL RDS · Secrets Manager · Vertex AI 크레덴셜 전환 (**M7**)
- ❌ 검색·미디어·부하 테스트 (**v2 · v1 이후**)
- ❌ Session Wizard UI axisDraft 편집 · 챕터 재생성 UI (**M7~v2**)

---

## 관찰 지표 로깅 확인 (M5 계승 · M6 LLM 반영)

M5까지 착지한 관찰 지표 로깅 인프라 (`build/logs/thirdtool.log`)가 M6의 LLM Adapter · Cascade · Session을 반영해야 한다.

- **LLM 호출 시 로그 항목**:
  - `port=layers` or `axes` or `chapters-outline` or `chapter-subtree` or `selection-outline` or `selection-subtree`
  - `provider=llm` (또는 폴백 시 `provider=static`)
  - `model=gemini-flash-2.5`
  - `duration=Xms` · `promptTokens=X` · `completionTokens=X` · `cost.estimate=X`
  - `cascadeFallbackReason=null` (LLM 성공) or `llm_timeout|llm_error|llm_parse_error|llm_quota_exceeded|llm_disabled` (Static 폴백)
- **Rate Limit 발동 시**:
  - `WARN: rate_limit_exceeded bucket=ip_10rpm ip={...}` or `bucket=user_30rpm userId={...}`
- **Session 상태 전이**:
  - `INFO: session_state_transition sessionId={...} from=INIT to=LAYERS_DRAFTED`
  - `INFO: session_abandoned sessionId={...} reason=explicit|ttl_expired`

M7에 `thirdtool.suggestion.*` Prometheus 메트릭 배선 예정 (AS Epic 6).

---

## 문제 발생 시 (Troubleshooting)

| 증상 | 원인 후보 | 해결 |
| --- | --- | --- |
| 부팅 실패 with "Failed to create ChatModel" | Vertex AI 크레덴셜 파일 경로 잘못 or GCP API 미활성 | `GOOGLE_APPLICATION_CREDENTIALS` 환경변수 · GCP Console에서 Vertex AI API 활성 확인 |
| LLM 호출 500 with "quota exceeded" | GCP 프로젝트 daily quota 초과 | GCP Console에서 quota 확인 · dev 환경 daily budget 관찰 · `cost.md` 기록 |
| 응답이 항상 `providerContext: "static:*"` | Vertex AI 배선 실패 · Cascade가 즉시 폴백 | 로그에서 `cascadeFallbackReason` 확인 · `spring.ai.vertex.ai.gemini.enabled=true` 확인 · 크레덴셜 재검증 |
| 429 응답이 예상보다 자주 발생 | Bucket4j bucket 크기 설정 오류 | `application.yml`의 `suggestion.rate-limit.ip-per-minute=10` 확인 · dev에서 임시 상한 조정 |
| `roadmap_interaction_session` 조회 안 됨 | V39 미착지 or JPA 매핑 실수 | `flyway_schema_history` 확인 · Entity 매핑 재검토 · `@Entity` `@Table` 이름 정합 |
| 세션 무효 전이 예외 발생 안 함 | `SessionState.transitionTo()` 로직 미구현 | AIR E1 S1-2 착지 확인 · 도메인 단위 테스트 실행 |
| TTL 만료가 자동으로 발생하지 않음 | cron scheduler 미배선 or `@EnableScheduling` 누락 | `RoadmapInteractionSessionExpirationScheduler` 존재 확인 · `@Scheduled(fixedRate = 30 * 60 * 1000)` 확인 |
| 로그에 `cost.estimate` 없음 | Vertex AI usage 응답 미파싱 | `VertexAiGeminiChatResponseMetadata` 파싱 로직 재검토 · Spring AI 버전 호환 확인 |
| `<CascadeFallbackNotice>` 표시 안 됨 | FE Zod schema에 `providerContext` prefix 판별 로직 미갱신 | `provider.startsWith("static:")` 판정 로직 확인 |

---

## 마무리 판정

M6 완료 GO/NO-GO는 아래 5개 조건 모두 만족 시 통과:

- [ ] BE 부팅 성공 + Flyway V39 착지 + Vertex AI ChatModel 배선 + 프롬프트 template 7개 embed 확인
- [ ] BE API 6개 축 실행 확인 (LLM 4 Adapter · Cascade 폴백 · Rate Limit · Session 상태기계 · Vertex AI 비용 로깅 · M5 회귀 없음)
- [ ] FE 부팅 성공 + M6 신규 컴포넌트 (`<ProviderContextIndicator>` LLM 배지 · `<CascadeFallbackNotice>` · `<RateLimitNotice>` · `<RoadmapSessionEntryButton>` · `<SessionStateBadge>`·`<SessionExpiresCountdown>`) 렌더
- [ ] 7개 시나리오 완주 — 특히 **시나리오 A/B/D (LLM 첫 응답 · Cascade 폴백 · Session 진입)**
- [ ] Session 상태 매트릭스 도메인 단위 테스트 통과 + Vertex AI 실측 비용 20~50 호출 관찰 반영 (`cost.md` §M6 초기 실측)

→ 조건 만족 시 M6 동결 · **M7 진입** (2026-08-05 Wed).

---

## 참고

- 마일스톤 원본: `./milestone.md`
- 이전 마일스톤 UX 확인: `../0.0.5v/ux-check.md`
- 릴리스 스코프: `../../release/version/0.0.1v/release.md` — 0.1.0v (~08-19) 사이클 10단계
- 다음 마일스톤: 08-05 진입 · `../0.0.7v/milestone.md`
- FE 대응 마일스톤: `../../../pes/fe/fe-milestones/version/0.0.6v/milestone.md`
- SDD 참조:
  - `../../../pes/workspectrum/sdd/in-progress/product-ai-suggestion.md` — Epic 4·5 (LLM Adapter + Cascade)
  - `../../../pes/workspectrum/sdd/in-progress/product-ai-interactive-roadmap.md` — Epic 1 (Session Aggregate)
- 07-02 pivot 이슈: `../../../fix/brainstorming/version/0.0.2v/issue-09` (3-layer AI) · `issue-17` (Two-Step) · `issue-19` (concept-spec)
- ADR: ADR010 (Cascade 폴백 원칙) · ADR022 (용어)
