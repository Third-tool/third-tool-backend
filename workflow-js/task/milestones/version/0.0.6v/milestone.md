# M6 / 0.0.6v — Week of 2026-07-29 ~ 2026-08-04 (D1 = 2026-07-29 Wed)

> **마일스톤의 역할**: M5가 07-28에 동결됐다 (Deck BC 완전 폐기 + Layer 진행률 착지 + Review BC 3 Epic 완주 · DailyLearningBatch + ReviewSession cross-layer + Dashboard). 본 M6는 **AI Suggestion Static→LLM 전환 착수 (AS Epic 4 Vertex AI Gemini Flash 2.5) + Cascade fallback + Rate Limit (AS Epic 5) + Interactive Roadmap Session Epic 1 (RoadmapInteractionSession Aggregate + 상태기계)** 를 목표로 한다.
>
> 한 주 = 한 버전 = `version/0.0.X v/` 폴더 하나. 본 버전(0.0.6v)에는 다음 7 파일이 들어간다:
> - `milestone.md` *(본 문서)* — 잡힌 양 + 일정 + 의존 + Epic PR 매트릭스
> - `infra.md` — (본 버전 배포 미포함 사유 명시 · dev 환경 Vertex AI 배선만)
> - `performance.md` — (본 버전 성능 측정 미수행 사유 명시 · LLM latency는 관찰만)
> - `outcome.md` — 사용자·기능 성과 (LLM 첫 응답 도착 + Cascade 첫 실행 + Session Wizard 첫 진입)
> - `cost.md` — 비용 성과 (Vertex AI 실측 첫 발생 · 개발 단계 예산 관찰)
> - `review.md` — 회고 + 다음 버전 진입 신호
> - `eval.md` — AI 응답 품질 평가 프레임 확장 (Static→LLM 전환 baseline + Cascade fallback 판정 프레임 신설)

**릴리스 대응**: 본 M6은 첫 사용자 릴리스(0.1.0v, 2026-08-19) 6주 로드맵의 **다섯 번째** 마일스톤. 자세한 스코프는 `workflow/task/milestones/release/version/0.0.1v/release.md` §Product별 릴리스 스코프 순회 참조.

**FE 대응 마일스톤**: `workflow/task/pes/fe/fe-milestones/version/0.0.6v/milestone.md` — 동일 주간, Session Wizard 초기 UI + Cascade 배지 (`provider: llm | static`) + Rate limit 안내 UI 착지.

---

## 0.0.6v 스코프 결정 (M5 이후, 2026-07-29)

**M5 착지 결과 요약**:
- ✅ Deck BC 완전 폐기 (LT E5 S5-1~5-5 + LT E3 S3-5 병합) — `_archived_deck` RENAME · `card.deck_id` 폐기 · `/decks/*` 410 Gone · `docs/DOMAIN.md` §Deck 삭제
- ✅ LT E6 이중 스코프 착지 (S6-1~6-5) — ReviewScope enum · Layer.progressStatus 파생 · FE 용어 대체 (S6-1·2·3은 PR#4가 supersede하며 폐기)
- ✅ Review BC 3 Epic 완주 (Review E1·E2·E3) — DailyLearningBatch + ReviewSession cross-layer 재편 + Dashboard 규칙 추천 + 주간 요약 cron
- ✅ Flyway V31~V37 착지 (Deck 폐기 · scope 폐기·재편 · DailyBatch 신설)
- ✅ Reviewer 5관점 세션 5회 진행
- ✅ AS Epic 3 안정화 관찰 (eval.md §M5 baseline 흡수)

**M6 축 결정 — LLM Adapter 첫 도착 + Cascade fallback + Session Aggregate 신설**:

M5 review.md §다음 마일스톤 결정 예고 반영. 사용자 지시 "Epic 단위 PR 진행" 유지 · Reviewer 5관점 세션 유지 · Vertex AI 실제 호출 첫 발생 (비용 관찰 착수).

이 지시를 반영해 본 M6은 다음 3축으로 구성:

1. **AS Epic 4 LLM Adapter 완주** — Story 4-1~4-5 (Spring AI + Vertex AI Gemini Flash 2.5 + 6-Port LLM Adapter 5종). **주력 (~46%)**.
2. **AS Epic 5 Cascade + Rate Limit + REST 재편** — Story 5-1~5-4 (SuggestionCascade Static→LLM 폴백 + Bucket4j rate limit + SuggestionController 4엔드포인트 + Application Service). **부차 (~24%)**.
3. **AI Interactive Roadmap Epic 1 착지** — Story 1-1~1-4 (`RoadmapInteractionSession` Aggregate + `SessionState` enum + 세션 시작 API + abandon/TTL). **경량 (~30%)**.

**Epic 단위 PR 5건 예정 (본주 목표)**:

| Epic PR | 대응 | 예상 SP | 종료 목표 |
|---|---|---|---|
| PR#1 (AS-E4-LLM-INFRA) | AS Epic 4 Story 4-1 (Spring AI + Vertex AI 인프라 · concept-spec.txt 리소스 embed · 이슈 #19) | 1.5 | D2 |
| PR#2 (AS-E4-LLM-ADAPTERS) | AS Epic 4 Story 4-2~4-5 (4 LLM Adapter · Layer/Axis/Roadmap/Selection) | 8 | D4 |
| PR#3 (AS-E5-CASCADE-RATE) | AS Epic 5 Story 5-1~5-4 (Cascade + Bucket4j + Controller + Command Service) | 5 | D5 |
| PR#4 (AIR-E1-SESSION) | Interactive Roadmap Epic 1 Story 1-1~1-4 (Session Aggregate + 상태 · 시작 API · abandon/TTL) | 6 | D7 |

**Total: 4 Epic PR · 총 20.5 SP** (M5 42.5 SP 대비 -52% · **적정 부하 · Vertex AI 실측 발생 첫주라 관측·비용 확인 여유 유지**. Reviewer 5관점 세션 4회).

**본 버전 제외 사유**:
- **Review E3 Dashboard 확장** — M5에 완주. M6 관찰만 (`cost.md`·`eval.md`에 실측 반영)
- **Card BC 안정화 관찰** — M4 완주 이후 관찰만. 회귀 발견 시 별도 M6.1 패치
- **Interactive Roadmap Epic 2** (axisDraft 흐름) — M7 이관 (Session Aggregate 안정화 이후)
- **AS Epic 6 관측성** (`thirdtool.suggestion.*` 메트릭) — M7 이관 (관측 baseline과 함께)
- **AI 비용 예산 cap 자동 컷** (이슈 #20) — v2 이관 유지. 본 M6에서 실측 관찰만
- **Roadmap 노드 재생성 API + hint** (이슈 #18) — v2 이관 유지
- **배포 라인 · MySQL RDS · Secrets Manager** — M7 이관 유지
- **부하 테스트 · APM** — v2 이관 유지
- **v2 이관**: 검색·미디어 CDN·캐시·알림·Admin

---

## 진행 중 Product 잔여 인벤토리 (Before/After — M6 진입 시 vs 종료 후 예상)

| Product | 총 Story | M6 진입 시 완료 | M6 진입 시 잔여 | M6 대상 | **M6 종료 후 예상 잔여** | **해결율** |
| --- | --- | --- | --- | --- | --- | --- |
| 1. 인증 (`product-auth.md`) | 10 | 10 | 0 | — | 0 | ✅ 완주 (M1) |
| 2. User BC (`Product.md`) | 8 | 7 | 1 | — | 1 | 0% (M7~) |
| 3. Learning Tower (`product-learning-tower.md`) | 42 | 32 (E1·E2·E3-재정의판·E4·E5·E6+E3S3-5) | 10 | — (SUPERSEDED 잔재 관찰만) | 10 | 0% |
| 4. **AI Suggestion** (`product-ai-suggestion.md`, 7 Epic 재편) | **34** | 14 | 20 | **9 Story (E4 5 + E5 4)** | **11** | **45%** ↑ |
| 5. **AI Interactive Roadmap** (`product-ai-interactive-roadmap.md`, 5 Epic) | **~19** | 0 | ~19 | **4 Story (E1 1-1~1-4)** | **~15** | **21%** ↑ |
| 6. Card (`product-card.md`) | 16 | 16 | 0 | — | 0 | ✅ 완주 (M4) |
| 7. Review (`product-review.md`) | 23 | 23 | 0 | — | 0 | ✅ 완주 (M5) |
| 8. 컨테이너 배포 (`product-infra-deploy.md`) | 9 | 5 | 4 | — | 4 | 0% (M7) |
| 9. AWS 네트워크 (`product-infra-network.md`) | 8 | 3 | 5 | — | 5 | 0% (M7) |
| 10. Secrets·백업·관측 (`product-infra-ops.md`) | 10 | 2 | 8 | — | 8 | 0% (M7) |
| 11. 로깅 (`product-log.md`) | 9 | 3 | 6 | — | 6 | 0% (M7) |
| 12. 메트릭 (`product-op.md`) | 7 | 3 | 4 | — | 4 | 0% (M7) |
| 13. 검색 (`product-search.md`) | ~18 | 0 | ~18 | — | ~18 | 0% (v2) |
| 14. 미디어 (`product-media.md`) | 보류 | 0 | — | — | — | — |
| 15. 캐시 (`product-cache.md`) | — | 0 | — | — | — | — |
| 16. 알림 (`product-notification.md`) | — | 0 | — | — | — | — |
| 17. Admin (`product-admin.md`) | — | 0 | — | — | — | — |
| 18. FE CDN (`product-fe-cdn.md`) | — | 0 | — | — | — | — |
| 19. 부하 테스트 (`product-load-test.md`) | 6 | 0 | 6 | — | 6 | 0% (v1 이후) |
| **합계** | **~206** | **~118** | **~88** | **13 Story · 4 Epic PR · 20.5 SP** | **~75** | **15%** ↑ |

### 📊 M6 예상 성과 카드

- **총 해결 대상**: 13 Story (전체 잔여 ~88의 **15% 소진**)
- **완주 예상 SDD Product**:
  - `product-ai-suggestion.md` Epic 4 (LLM Adapter) — 완주
  - `product-ai-suggestion.md` Epic 5 (Cascade + Rate Limit + REST) — 완주
- **부분 진행 SDD Product**:
  - `product-ai-interactive-roadmap.md` Epic 1 (Session Aggregate) — 4/4 Story 완주 · Epic 2 (axisDraft)는 M7
- **M6 종료 후 남는 것** (다음 마일스톤 트라젝토리):
  - AI Suggestion **11 Story 잔여** → M7 Epic 6 (관측성 4 Story) + SUPERSEDED 잔재 7 Story
  - AI Interactive Roadmap **15 Story 잔여** → M7 Epic 2 (axisDraft · 4 Story) + Epic 3·4·5 (v2 이관)
  - 인프라 4개 Product (deploy·network·ops·log·op) **27 Story 잔여** → M7 착지
  - Review·Card 안정화 관찰만
  - v2 이관: 검색·미디어·캐시·알림·Admin·부하

---

## Epic PR 매트릭스 (본주 잡힌 양)

Epic 단위 PR 진행 원칙 (M5 유지):
- **한 Epic PR = 한 논리 단위 = 한 base 브랜치**. Squash merge 또는 rebase merge.
- Epic PR 안의 Story 커밋은 순차 누적 (`feat(scope): ...`).
- Reviewer 세션은 Epic PR 단위 (PR 하나에 5관점 병렬 발사).

### Epic PR #1 — `AS-E4-LLM-INFRA` (Spring AI + Vertex AI 인프라 + 프롬프트 embed)

**Base 브랜치**: `feat/042-as-e4-llm-infra`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-ai-suggestion.md`
- Epic: `# [Epic 4] LLM Adapter (Vertex AI Gemini Flash 2.5)` (line 1451~1695)
- Story 범위: `## [Story 4-1]` (line 1488)
- 상위 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-09-ai-suggestion-3layer.md` + `issue-19-roadmap-selection-concept-spec.md` (프롬프트 embed)

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | AS E4 S4-1 | Spring AI + Vertex AI 배선 (`spring-ai-vertex-ai-gemini-spring-boot-starter`) + `application.yml` credentials + `concept-spec.txt` + 6개 프롬프트 template resource embed (M3 기 확립분) | 1.5 |

**PR 종료 신호**:
- `build.gradle` Spring AI 의존 명시 · `application.yml`에 `spring.ai.vertex.ai.gemini.project-id` · `location` · `chat.options.model=gemini-flash-2.5` 설정
- 로컬 부팅 시 `ChatModel` bean 자동 배선 · `curl` 첫 호출 성공 (dev 크레덴셜 · `--dry-run` 옵션으로 실제 호출 없이 검증도 지원)
- `src/main/resources/ai/prompts/` 6개 template embed (`concept-spec.txt` + `layers.txt` + `axes.txt` + `chapters-outline.txt` + `chapter-subtree.txt` + `selection-outline.txt` + `selection-subtree.txt`) · UTF-8 · Reviewer 정합 확인
- `dev` 프로필: `spring.ai.enabled=false` fallback (test 시 실제 호출 안 함)

**병렬 진입 조건**: 독립. D1에 착수 가능.

**Reviewer 세션**: 5관점 발사. **특히 Architecture Reviewer가 "spring-ai 의존이 프로젝트 구조에 자연스럽게 안착되는가"를 판정** (BC 의존 방향 · Common/Infrastructure 배치 여부).

### Epic PR #2 — `AS-E4-LLM-ADAPTERS` (4개 LLM Adapter · Layer/Axis/Roadmap/Selection)

**Base 브랜치**: `feat/043-as-e4-llm-adapters`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-ai-suggestion.md`
- Epic: `# [Epic 4] LLM Adapter` (line 1451~1695)
- Story 범위: `## [Story 4-2]` (line 1530) ~ `## [Story 4-5]` (line 1664)
- 상위 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-09-ai-suggestion-3layer.md` + ADR010 폴백 원칙

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | AS E4 S4-2 | `LlmLayerSuggestionAdapter` — concepts[] + facadeContext → Layer 리스트 (JSON 응답 파싱 · `suggestionsAvailable` 판정) | 2 |
| 2 | AS E4 S4-3 | `LlmAxisSuggestionAdapter` — layer + concepts → Axis 리스트 (concept-spec 6+5 카탈로그 정합) | 2 |
| 3 | AS E4 S4-4 | `LlmRoadmapSuggestionAdapter` — axis + chapter → RoadmapNode 챕터 outline + subtree (하네스 엔지니어링 5 챕터 스타일) | 2 |
| 4 | AS E4 S4-5 | `LlmSelectionsSuggestionAdapter` — axis + concept → Selection 컨테이너 outline + node subtree (판례/발산 예시) | 2 |

**PR 종료 신호**:
- `POST /api/v1/suggestions/layers` (LLM 모드) → 200 + `providerContext: "llm:vertex-ai-gemini-2.5-flash"` + Layer 리스트 3~5개
- `POST /api/v1/suggestions/axes` (LLM 모드) → 200 + Axis 리스트 · concepts에 따른 응답 다양성 확인
- `POST /api/v1/suggestions/chapters-outline` (LLM 모드) → 200 + 5개 챕터 · 각 챕터에 title + rationale
- `POST /api/v1/suggestions/chapter-subtree` (LLM 모드) → 200 + `bodyAsciiTree` 문자열 (사용자 편집 대상)
- `POST /api/v1/suggestions/selection-outline` + `selection-subtree` (LLM 모드) → 200 + Selection 컨테이너 + Selection Node subtree
- 각 응답이 6-Port 필드 정합 (Static 응답 구조와 shape 일치)
- 응답 JSON 파싱 실패 시 `suggestionsAvailable: false` 폴백 원칙 (ADR010)

**의존**: PR#1 (LLM 인프라) 완주 후 진입.

**Reviewer 세션**: 5관점 발사. **특히 Sceptical Reviewer가 "LLM 응답 파싱 실패·헛소리 응답에 대한 폴백 원칙 (ADR010) 준수 여부"를 판정**.

### Epic PR #3 — `AS-E5-CASCADE-RATE` (SuggestionCascade + Rate Limit + REST 재편)

**Base 브랜치**: `feat/044-as-e5-cascade-rate`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-ai-suggestion.md`
- Epic: `# [Epic 5] SuggestionCascade + Rate Limit + REST API` (line 1697~1917)
- Story 범위: `## [Story 5-1]` (line 1731) ~ `## [Story 5-4]` (line 1861)
- 상위 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-09-ai-suggestion-3layer.md` + ADR010

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | AS E5 S5-1 | `SuggestionCascade` 구현 (LLM 우선 호출 → 실패 시 Static 폴백 · ADR010 정합) | 1 |
| 2 | AS E5 S5-2 | Bucket4j Rate Limit (IP 10rpm · User 30rpm · concurrent 5) | 1 |
| 3 | AS E5 S5-3 | `SuggestionController` 4 엔드포인트 재편 (`/suggestions/layers` · `/axes` · `/chapters-outline` · `/chapter-subtree` + selection 2개) | 1.5 |
| 4 | AS E5 S5-4 | `SuggestionCommandService` — Application Service 조율 (Cascade 호출 · rate 검사 · 로깅) | 1.5 |

**PR 종료 신호**:
- Cascade 시나리오 검증: 유효 크레덴셜 → `provider=llm` 응답 · 크레덴셜 강제 실패 → `provider=static` 폴백 자동
- Bucket4j Rate Limit: 동일 IP 11번째 호출 → `429 Too Many Requests` + `code: SUGGESTION_RATE_EXCEEDED`
- `/suggestions/*` 4 엔드포인트 모두 Swagger UI 반영 · request/response contract 동일 shape (LLM/Static 무관)
- `SuggestionCommandService`가 로그에 `provider=llm` or `static` · `duration=Xms` · `userId={...}` · `cascadeFallbackReason=...` 기록

**의존**: PR#2 (LLM Adapter 4종) 완주 후 진입.

**Reviewer 세션**: 5관점 발사. **특히 Domain Reviewer가 "Cascade의 폴백 결정 로직이 SuggestionCommandService에 자연스럽게 응집되는가 · 도메인 서비스 vs Application Service 경계 준수"를 판정**.

### Epic PR #4 — `AIR-E1-SESSION` (RoadmapInteractionSession Aggregate + 상태기계 + 시작 API)

**Base 브랜치**: `feat/045-air-e1-session`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-ai-interactive-roadmap.md`
- Epic: `# [Epic 1] RoadmapInteractionSession Aggregate + 상태기계` (line 466~693)
- Story 범위: `## [Story 1-1]` (line 508) ~ `## [Story 1-4]` (line 656)
- 상위 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-17-ai-two-step-generation.md`

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | AIR E1 S1-1 | Flyway V39 `roadmap_interaction_session.sql` (테이블) + `RoadmapInteractionSession` Aggregate 도메인 신설 | 2 |
| 2 | AIR E1 S1-2 | `SessionState` enum + 상태 전이 검증 (`INIT → LAYERS_DRAFTED → AXES_DRAFTED → CHAPTERS_DRAFTED → SUBTREES_DRAFTED → REVIEWING → COMMITTED/ABANDONED`) | 1 |
| 3 | AIR E1 S1-3 | 세션 시작 API `POST /roadmap-sessions` + Response DTO | 1.5 |
| 4 | AIR E1 S1-4 | 세션 조회 + abandon + TTL 만료 (예: 24시간 후 자동 ABANDONED) | 1.5 |

**PR 종료 신호**:
- V39 성공 · `roadmap_interaction_session` 테이블 (`id`·`user_id`·`state`·`facade_id`·`concepts_snapshot`·`started_at`·`expires_at`·`terminated_at`)
- `POST /roadmap-sessions` → 201 + `{ id: X, state: "INIT", expiresAt: ISO-8601 }`
- 상태 전이 검증: `INIT → AXES_DRAFTED` 시도 → 예외 (`SESSION_STATE_INVALID_TRANSITION`)
- `GET /roadmap-sessions/{id}` → 200 + 현재 상태 + snapshot
- `POST /roadmap-sessions/{id}/abandon` → 200 + `state: "ABANDONED"`
- TTL 만료 cron (30분 주기) 확인 · 24시간 초과 세션 자동 ABANDONED
- `AIR E1 S1-2` 도메인 단위 테스트 (상태 매트릭스 · 유효/무효 전이 각 3건 이상)

**의존**: PR#1 (Spring AI 인프라) 완주 후 진입 가능 (선택 · Session 자체는 LLM 호출 전 진입점이므로 독립 가능하지만 M6 흐름상 PR#3 완주 후 진입 권장).

**Reviewer 세션**: 5관점 발사. **특히 Domain Reviewer가 "Session Aggregate의 상태기계·불변식·TTL 처리가 자연스러운가"를 판정**.

---

## Story 카테고리별 합계 (SDD Story 단위)

| 카테고리 | SDD Epic/Story | Story 수 | SP | 비중 |
| --- | --- | --- | --- | --- |
| AS Epic 4 (LLM Adapter · 인프라 + 4종) | AS E4 S4-1~S4-5 | 5 | 9.5 | 46% |
| AS Epic 5 (Cascade + Rate + REST) | AS E5 S5-1~S5-4 | 4 | 5 | 24% |
| AIR Epic 1 (Session Aggregate + 상태) | AIR E1 S1-1~S1-4 | 4 | 6 | 30% |
| **합계** | | **13** | **20.5** | 100% |

**분배 근거**:
- M5 실 착지 ~42.5 SP 대비 -52%. Vertex AI 실측 호출 첫 발생 주라 관측·비용 확인·튜닝 여유 유지.
- **AS 카테고리 70%** (E4+E5) — LLM 실제 도착 · Cascade 폴백 원칙 착지가 M6 축.
- **AIR Epic 1 30%** — Session Wizard의 근간 · Aggregate·상태기계 신설.
- **4개 Epic PR**: 각 PR = SDD Epic 하위 Story 묶음 (Epic 4는 인프라+4종 어댑터 2 PR로 분리). Reviewer 세션 4관점 4회 (5관점 유지).
- **SDD Story 규칙 준수**: 모든 M6 작업이 `product-ai-suggestion.md` E4·E5 · `product-ai-interactive-roadmap.md` E1의 명시된 Story에 대응.

---

## 종료 신호 — "LLM 첫 응답 도착 + Cascade 자동 폴백 + Session 첫 진입"

본주 종료 시점에 다음이 모두 성립해야 한다. (7 신호 중 5개 이상 → 0.0.6v 동결)

- [ ] **머지 신호**: Epic PR 4개 중 최소 3개 머지 (75%)
- [ ] **LLM 첫 응답 신호**: `POST /suggestions/chapters-outline` (LLM 모드) → 200 + `providerContext: "llm:vertex-ai-gemini-2.5-flash"` + 5 챕터 반환. 4 Adapter (Layer/Axis/Roadmap/Selection) 모두 응답 도착
- [ ] **Cascade 폴백 신호**: LLM 강제 실패 시나리오 → 자동 Static 폴백 · 응답 `providerContext: "static:*"` · 로그 `cascadeFallbackReason=llm_timeout|llm_error|...`
- [ ] **Rate Limit 신호**: Bucket4j 11번째 요청 → 429 + `SUGGESTION_RATE_EXCEEDED` · Swagger UI 명세 반영
- [ ] **Session 첫 진입 신호**: `POST /roadmap-sessions` → 201 · `INIT` 상태 · `expiresAt` 24h · abandon 명시적 호출 · 상태 무효 전이 예외 확인
- [ ] **비용 관찰 신호**: Vertex AI 실측 호출 후 로컬 로그에 `cost.estimate=Xtokens` 반영 · `cost.md` §M6 초기 실측 항목 갱신 · `application.yml` 비용 관찰 활성
- [ ] **테스트 신호**: 4개 Epic PR 각각 도메인 단위 · Repository Slice · Controller Slice 통과. `./gradlew test` BUILD SUCCESSFUL. LLM Adapter는 mock 응답 기반 단위 테스트 · 실제 호출은 `@Tag("llm-live")` 격리 (CI에서 스킵)

**미합격 처리**: 위 7 신호 중 5개 미만 성립 시 M6를 0.0.6v로 동결하지 않고 0.0.6.1v 패치 발행 → 다음 주 초까지 연장. AIR E1 (Session)이 미달일 경우 M7로 이관 검토.

---

## 의존 chain

```
[선행: M5 착지]
Review BC 3 Epic 완주 (DailyBatch + Session + Dashboard) ✅ 완료
Deck BC 폐기 · Layer 이중 스코프 착지 ✅ 완료

[D1: 착수]
PR#1 (AS-E4-LLM-INFRA · 독립)
  AS E4 S4-1 (Spring AI + Vertex AI + 프롬프트 embed)
     │
     ▼
PR#2 (AS-E4-LLM-ADAPTERS)
  AS E4 S4-2 ~ S4-5 (4 Adapter)
     │
     ▼
PR#3 (AS-E5-CASCADE-RATE)
  AS E5 S5-1 ~ S5-4
     │
     ▼
PR#4 (AIR-E1-SESSION · 병렬 가능)
  AIR E1 S1-1 ~ S1-4
     │
     ▼
  LLM 첫 응답 · Cascade · Session 첫 진입
```

**병렬 진입 가능 묶음**:
- **A** (D1 · 07-29 Wed): PR#1 착수 (Spring AI 배선) + PR#4 병행 착수 가능 (AIR E1 S1-1 테이블·Aggregate · LLM 무관)
- **B** (D2 · 07-30 Thu): PR#1 완주 → PR#1 머지 · PR#2 착수 (LLM Adapter 4종) · PR#4 S1-2 (상태기계)
- **C** (D3 · 07-31 Fri): PR#2 S4-2/S4-3 (Layer·Axis Adapter) · PR#4 S1-3 (세션 시작 API)
- **D** (D4 · 08-01 Sat): PR#2 S4-4/S4-5 (Roadmap·Selection Adapter) 완주 → PR#2 머지 · PR#4 S1-4 (abandon·TTL)
- **E** (D5 · 08-02 Sun): PR#3 착수 (Cascade + Rate + Controller + Service) 완주 → PR#3 머지 · PR#4 마무리
- **F** (D6 · 08-03 Mon): PR#4 완주 → PR#4 머지 · 통합 로컬 검증 3시나리오 (LLM 정상 · LLM 실패 폴백 · Rate 초과)
- **G** (D7 · 08-04 Tue): `outcome.md`·`review.md`·`eval.md`·`cost.md` 골격 · 0.0.6v 동결 판정

**직렬 (M6 합격선까지)**: PR#1 → PR#2 → PR#3 → 통합. PR#4는 D1부터 병렬 진행.

---

## 작업 일정 (Epic PR 단위 체크리스트)

D1 = 2026-07-29 (Wed). 종료 D7 = 2026-08-04 (Tue). 7일 안에 4 Epic PR.

| 일 | 날짜 | 잡힌 작업 (Epic PR 진행) — SDD Story 기준 |
| --- | --- | --- |
| D1 (수) | 07-29 | PR#1 착수 (**AS E4 S4-1** Spring AI + Vertex AI 배선 + 프롬프트 embed) + PR#4 착수 (**AIR E1 S1-1** V39 + Session Aggregate 도메인) |
| D2 (목) | 07-30 | PR#1 완주 → **PR#1 머지 + Reviewer 세션 (Architecture 강조)** · PR#2 착수 (**AS E4 S4-2** LlmLayerSuggestionAdapter) · PR#4 **AIR E1 S1-2** (상태기계 + 전이 검증) |
| D3 (금) | 07-31 | PR#2 **AS E4 S4-3** (LlmAxisSuggestionAdapter) 진행 · PR#4 **AIR E1 S1-3** (세션 시작 API) |
| D4 (토) | 08-01 | PR#2 **AS E4 S4-4** (LlmRoadmapSuggestionAdapter) + **S4-5** (LlmSelectionsSuggestionAdapter) 완주 → **PR#2 머지 + Reviewer 세션 (Sceptical 강조 · ADR010 폴백 판정)** · PR#4 **AIR E1 S1-4** (abandon + TTL) |
| D5 (일) | 08-02 | PR#3 착수 (**AS E5 S5-1** Cascade + **S5-2** Bucket4j + **S5-3** Controller + **S5-4** CommandService) 완주 → **PR#3 머지 + Reviewer 세션 (Domain 강조)** · PR#4 완주 → **PR#4 머지 + Reviewer 세션 (Domain 강조 · 상태기계)** |
| D6 (월) | 08-03 | 통합 로컬 검증 3시나리오 (LLM 정상 · LLM 실패 → Static 폴백 · Rate 초과 429) · Vertex AI 실측 비용 로그 관찰 · `cost.md` §M6 초기 실측 |
| D7 (화) | 08-04 | `outcome.md` · `review.md` · `eval.md` · `cost.md` 골격 · 0.0.6v 동결 판정 |

**Reviewer 세션 규칙**: 각 Epic PR 머지 직전 5관점 병렬 발사 (`.claude/rules/review.md` §3~§7). 사용자 확인 후 머지. M5 표준 스케일 유지 · 4회 진행.

**Flyway V 버전 순서 관리** (M5에서 V31~V37 소모):
- V38: (예약 · 필요 시 · 대부분 신설 없음)
- V39: `roadmap_interaction_session.sql` (PR#4 S1-1)

**Vertex AI 크레덴셜 관리**: dev 환경 GCP service account key는 `~/.config/gcloud/thirdtool-dev-key.json` 저장 · `.gitignore` 등록 · `application-dev.yml`에 경로만 명시. prod는 M7 Secrets Manager 배선 시점에 전환.

---

## 리스크와 관찰 포인트

| 영역 | 리스크 | 관찰 포인트·완화 |
| --- | --- | --- |
| Vertex AI 첫 실측 호출 · 비용 | Gemini Flash 2.5 첫 호출 · 개발 반복 시 예산 폭주 위험 | dev 프로필에서 `spring.ai.vertex.ai.gemini.chat.options.max-output-tokens=2048` 상한 · 로컬 스크립트 반복 호출 자동화 금지 · `cost.md` 실측 관찰 |
| LLM 응답 파싱 실패 | Gemini가 JSON 요구를 무시하고 자연어 응답 반환 · shape mismatch | 응답 파서에 `suggestionsAvailable: false` 폴백 원칙 (ADR010) · Sceptical Reviewer 판정 · 실측 파싱 실패율 로깅 |
| Cascade 폴백 결정 로직 | LLM 실패 신호 판정 (timeout · parse error · quota 초과)에 따른 자동 폴백 실행 | `cascadeFallbackReason` 필수 로깅 · 폴백 사유별 통계 관찰 · Rate 초과는 폴백 대상 아님 (사용자에게 429 반환) |
| Rate Limit 임계값 튜닝 | IP 10rpm · User 30rpm · concurrent 5 초기값이 사용자 UX에 자연스러운가 | v1은 초기값 유지 · 실측 통계 관찰 후 v2 튜닝 · `application.yml` externalize |
| Session Aggregate 상태 폭발 | 7 state · 6 transition · TTL 만료 · abandon · concurrent modification | 상태 매트릭스 도메인 단위 테스트 우선 · 유효/무효 전이 각 3건 이상 · TTL 만료 cron 별도 검증 |
| TTL 만료 cron 로컬 검증 | 24시간 TTL이므로 로컬에서 즉시 검증 어려움 · Clock 주입 필수 | `Clock` 도메인 서비스 주입 · 테스트에서 시각 조작 · dev 프로필 5분 TTL 옵션 (`session.ttl.minutes=5`) |
| 프롬프트 embed 자산 관리 | `concept-spec.txt` + 6개 template의 재고 (M3~M5 진화분) · Reviewer 정합 필요 | M3~M5 진화 이력을 `docs/adr/ADR-prompt-history.md`에 반영 검토 (별도 커밋) · Reviewer가 catalog 정합 판정 |
| Reviewer 세션 병목 (M5 실측) | 4 PR × 5관점 = 20 reviewer 발사 · 병렬 유지 | 병렬 발사 유지 · Sceptical Reviewer가 ADR010 폴백 원칙에 반복 참조 시 이전 판정 문서화 |
| dev 환경 GCP 크레덴셜 노출 위험 | service account key가 실수로 커밋되는 위험 | `.gitignore` 등록 필수 · `pre-commit hook`에 credential scan 검토 · M7 Secrets Manager 배선 이후 자동 전환 |
| Vertex AI 지역 라우팅 | `location=asia-northeast3` (서울) 선택 시 latency 최소화 · 다른 리전은 latency 대 | v1은 `asia-northeast3` 고정 · v2 서비스 확장 시 리전 fallback 검토 |
| DOMAIN.md 갱신 | AIR Session Aggregate 신설 · `docs/DOMAIN.md` §AI Interactive Roadmap 신설 필요 | PR#4에서 Session Aggregate 설명 추가 · 상태기계 다이어그램 ASCII 포함 |

---

## 다음 마일스톤 (M7 / 0.0.7v) 후보

본주 결과를 보고 결정하지만, 릴리스 문서에 잠긴 트라젝토리 그대로:

**M7 / 0.0.7v (08-05 ~ 08-11) — 배포 라인 완주 + 관측 baseline + Session Epic 2 완주**
- **product-infra-deploy Epic 1~4** (컨테이너 배포 · ECR · ECS Fargate · GHA OIDC · 환경 분리) — 4 Story
- **product-infra-network Epic 1~3** (VPC · ALB + Route53 + ACM · RDS Multi-AZ) — 3 Story
- **product-infra-ops Epic 1~2** (Secrets Manager · PITR + S3 + CloudWatch) — 2 Story
- **product-log Epic 1~4** (JSON logstash + MDC + GlobalExceptionHandler + 배포 이벤트 Slack) — 5 Story
- **product-op Epic 1~3** (Actuator + Prometheus + Grafana baseline) — 3 Story
- **AS Epic 6 관측성** (thirdtool.suggestion.* 메트릭 + MDC 확장 + ErrorCode + Runbook) — 4 Story
- **AIR Epic 2 axisDraft** (`AxisDraftSnapshot` VO + 6-Port 호출 + refresh + 실패 처리) — 4 Story

**Epic PR 예상 6~7건** (Infra 계열 3~4 PR · AS Epic 6 · AIR Epic 2 · 관측 baseline).

---

## Product 상태 전환 신호 (M6 종료 시)

- `in-progress/product-ai-suggestion.md` — **Epic 4·5 완주** 표기 (LLM Adapter 4종 + Cascade + Rate Limit + REST 재편 착지). Epic 6 관측성 M7 이관
- `in-progress/product-ai-interactive-roadmap.md` — **Epic 1 완주** 표기 (Session Aggregate + 상태기계 + 시작 API + TTL). Epic 2 (axisDraft) M7 이관
- `in-progress/product-review.md` — 상태 유지 (M5 완주 · M6는 관찰만)
- `in-progress/product-card.md` — 상태 유지 (M4 완주 · M6는 관찰만)
- `in-progress/product-learning-tower.md` — 상태 유지 (M5 완주 · M6는 관찰만)
- `fix/brainstorming/version/0.0.2v/issue-09` → **partial resolved** (E4·E5 착지 · E6는 M7)
- `fix/brainstorming/version/0.0.2v/issue-17` → **partial resolved** (Session Aggregate 착지 · Epic 2 axisDraft M7)
- `fix/brainstorming/version/0.0.2v/issue-19` → **resolved** (프롬프트 embed 완주 · concept-spec.txt + 6 template)
- `fix/brainstorming/version/0.0.2v/issue-18/20` → v2·M7 이관 유지 (Roadmap 재생성 · 예산 cap)

---

## brainstorming 트리거

본 M6 완료 후 `workflow/task/pes/brainstorming/0.0.7v/` 신설:
- LLM 응답 파싱 실패 사례 관찰 (`brainstorming/0.0.7v/llm-parse-failure-log.md`) — 실측 파싱 실패 시나리오 · 폴백 트리거 통계
- Cascade 폴백 사유 통계 (`brainstorming/0.0.7v/cascade-fallback-reasons.md`) — timeout · error · quota · parse 사유별 발생 비율
- Rate Limit 실측 트리거 UX (`brainstorming/0.0.7v/rate-limit-user-ux.md`) — 429 응답 시 사용자에게 안내할 UI 정책
- Session Wizard UI 흐름 초안 (`brainstorming/0.0.7v/session-wizard-flow.md`) — 7단계 상태 진행 사용자 화면 흐름
- Vertex AI 비용 예산 관찰 (`brainstorming/0.0.7v/vertex-ai-cost-observation.md`) — M6 실측 후 예산 임계값 초안 (v2 cap 대비)

---

## 참고

- 잔여 Story 인벤토리 출처: `workflow/task/pes/workspectrum/sdd/in-progress/` 19개 Product 파일
- M2 pivot 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-09` (3-layer AI) · `issue-17` (Two-Step Generation) · `issue-19` (concept-spec)
- 마일스톤 패키지 의도: `workflow/task/milestones/references/001.md`
- FE 대응 마일스톤: `workflow/task/pes/fe/fe-milestones/version/0.0.6v/milestone.md` (동일 주간)
- 이전 마일스톤: `workflow/task/milestones/version/0.0.5v/milestone.md` — M5 원안 · `../0.0.5v/outcome.md` · `../0.0.5v/review.md` — M5 결과 · `../0.0.5v/eval.md` — AI 응답 품질 평가 프레임
- **첫 릴리스 계획**: `workflow/task/milestones/release/version/0.0.1v/release.md` — 0.1.0v (2026-08-19) 스코프 · M3~M8 트라젝토리 · M6 목표: "프롬프트 embed + 대시보드 최소판 + Interactive Roadmap Session Epic 1 시작"
- 본 버전의 산출물 7종: `milestone.md`(본 문서) · `infra.md`(스킵) · `performance.md`(스킵) · `outcome.md` · `cost.md` · `review.md` · `eval.md`
- 양식 진화: 본 milestone은 0.0.5v milestone.md 양식 답습. **eval.md는 M6에 Static→LLM 전환 baseline + Cascade 폴백 판정 프레임 신설로 세 번째 확장**. **cost.md는 M6이 Vertex AI 첫 실측 발생으로 첫 실질 콘텐츠 확장**.
- **주요 SDD 참조 비율**:
  - `product-ai-suggestion.md` **~70%** (Epic 4·5 · Story 4-1~5-4 · 9 Story · SP 14.5)
  - `product-ai-interactive-roadmap.md` **~30%** (Epic 1 Story 1-1~1-4 · 4 Story · SP 6)
