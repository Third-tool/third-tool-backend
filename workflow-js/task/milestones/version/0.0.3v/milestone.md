# M3 / 0.0.3v — Week of 2026-07-08 ~ 2026-07-14 (D1 = 2026-07-08 Wed)

> **마일스톤의 역할**: M2가 07-07에 동결됐다 (LT Epic 1 Story 1 concept 테이블 · V16 착지 완료 + AI/Card/Review 리팩토링 도큐먼트 5건 착지 완료). 본 M3는 M2의 도메인 첫 조각 완주 + AI 결과 첫 노출을 목표로 한다.
>
> 한 주 = 한 버전 = `version/0.0.X v/` 폴더 하나. 본 버전(0.0.3v)에는 다음 6 파일이 들어간다:
> - `milestone.md` *(본 문서)* — 잡힌 양 + 일정 + 의존 + Epic PR 매트릭스
> - `infra.md` — (본 버전 배포 미포함 사유 명시)
> - `performance.md` — (본 버전 성능 측정 미수행 사유 명시)
> - `outcome.md` — 사용자·기능 성과 (concepts[] + Layer + Roadmap/Selection 노드 스키마 + AI 첫 응답 도착)
> - `cost.md` — 비용 성과 (본 버전 로컬 개발 한정 → 비용 발생 0)
> - `review.md` — 회고 + 다음 버전 진입 신호

**릴리스 대응**: 본 M3는 첫 사용자 릴리스(0.1.0v, ~2026-08-19) 6주 로드맵의 두 번째 마일스톤. 자세한 스코프는 `workflow/task/milestones/release/version/0.0.1v/release.md` §Product별 릴리스 스코프 순회 참조.

**FE 대응 마일스톤**: `workflow/task/pes/fe/fe-milestones/version/0.0.3v/milestone.md` — 동일 주간, 원안 Tier 1 미완주 완주 + 노드 UI Zod + 6-Port hook 스켈레톤.

---

## 0.0.3v 스코프 결정 (M2 이후, 2026-07-08)

**M2 착지 결과 요약**:
- ✅ LT Epic 1 Story 1 (concept 테이블 Flyway V16 + R16) — 07-01~07-02
- ✅ AI Roadmap 재설계 도큐먼트 (이슈 #15~#20 + product-learning-tower/ai-suggestion/ai-interactive-roadmap 3개 spec 개정)
- ✅ Card + Review 재설계 도큐먼트 (이슈 #21~#26 + product-card/product-review 2개 spec 신설)
- ✅ 마일스톤 문서 개정 + 첫 릴리스 계획 확정 (release.md v0.0.1)
- ✅ FE 마일스톤 동기화

**M3 축 결정 — AI 결과 빠른 착지 우선**:

사용자 지시: "ai로 우선 결과를 빨리 뽑기 위해서 양이 조금 많아도 괜찮습니다, epic 단위로 pr 진행할 예정입니다."

이 지시를 반영해 본 M3는 다음 3축으로 구성:

1. **도메인 착지 완주** — LT Epic 1·2 잔여 완주 (concepts[] + Layer). AI Port 소비의 입력 도메인 확보.
2. **Roadmap/Selection 노드 스키마 완주** — 이슈 #15/#16 이관. AI Port 결과 저장 대상 확보.
3. **AI 첫 결과 도착** — 이슈 #17 partial + #19: 6-Port 인터페이스 + `backend-developer` role Static Adapter가 실제 응답 발행. 로컬에서 `POST /api/v1/suggestions/chapters-outline` → 챕터 outline 리스트 응답 확인.

**Epic 단위 PR 6건 예정 (본주 목표)**:

| Epic PR | 대응 | 예상 SP | 종료 목표 |
|---|---|---|---|
| PR#1 (LT-E1-COMPLETE) | LT Epic 1 Story 1-2~1-5 | 5 | D3 |
| PR#2 (LT-E2-LAYER) | LT Epic 2 Story 2-1~2-5 | 9 | D4 |
| PR#3 (ISSUE-15-ROADMAP-NODE) | 이슈 #15 (Roadmap 노드 스키마 + 도메인 + API) | 10 | D5 |
| PR#4 (ISSUE-16-SELECTION-NODE) | 이슈 #16 (Selection 노드 스키마 + 도메인 + API) | 8 | D6 |
| PR#5 (ISSUE-17-19-AI-FIRST-RESPONSE) | 6-Port 인터페이스 + Static Adapter(backend-developer) + 개념 명세 프롬프트 | 10 | D7 |
| PR#6 (ADR023-TERMINOLOGY) | ADR023 (roadmap/selections 용어 재정의) 문서 정합 | 1 | D3 |

**Total: 6 Epic PR · 총 43 SP** (M2 원안 23 SP 대비 ~2배. "양이 조금 많아도" 사용자 지시 반영).

**본 버전 제외 사유**:
- **LLM Adapter (Vertex AI)** — M6 이관. v1 릴리스 시점엔 Static만.
- **Card 리팩토링 (이슈 #21~#23)** — M4 이관. Mode enum 재편이 stable해야 Card `createdMode`가 성립.
- **Review 재편 (이슈 #24~#26)** — M5 이관. Card가 선행되어야 Daily batch가 소비 가능.
- **배포 라인 · 성능 baseline** — M7 이관 (릴리스 문서 §Product 7 참조).
- **챕터 노드 재생성 API (이슈 #18)** — v2 이관 (0.1.1v).
- **AI 비용 예산 cap (이슈 #20)** — v1은 관찰 지표만. Cap 자체 v2.

---

## 진행 중 Product 잔여 인벤토리 (Before/After — M3 진입 시 vs 종료 후 예상)

**해결율 계산**: `M3 대상 / M3 진입 시 잔여 × 100%` — Product별 M3가 얼마나 소진하는지 즉시 파악.

| Product | 총 Story | M3 진입 시 완료 | M3 진입 시 잔여 | M3 대상 | **M3 종료 후 예상 잔여** | **해결율** |
| --- | --- | --- | --- | --- | --- | --- |
| 1. 인증 (`product-auth.md`) | 10 | 10 | 0 | — | 0 | ✅ 완주 (M1) |
| 2. User BC (`Product.md`) | 8 | 7 | 1 | — | 1 | 0% (M4~) |
| 3. **Learning Tower** (`product-learning-tower.md`, 6개 Epic + E3 재정의판) | **42** (신설 6건 포함) | 1 (E1S1) | 41 | **16 Story** | **25** | **39%** ↑ |
| 4. **AI Suggestion** (`product-ai-suggestion.md`, 7 Epic 재편) | **34** (신설 9건 포함) | 0 | 34 | **11 Story** | **23** | **32%** ↑ |
| 5. AI Interactive Roadmap (`product-ai-interactive-roadmap.md`) | ~15 | 0 | ~15 | — | ~15 | 0% (M6~M7) |
| 6. Card (`product-card.md`, 신설) | 16 | 0 | 16 | — | 16 | 0% (M4) |
| 7. Review (`product-review.md`, 신설) | 23 | 0 | 23 | — | 23 | 0% (M5) |
| 8. 컨테이너 배포 (`product-infra-deploy.md`) | 9 | 5 | 4 | — | 4 | 0% (M7) |
| 9. AWS 네트워크 (`product-infra-network.md`) | 8 | 3 | 5 | — | 5 | 0% (M7) |
| 10. Secrets·백업·관측 (`product-infra-ops.md`) | 10 | 2 | 8 | — | 8 | 0% (M7) |
| 11. 로깅 (`product-log.md`) | 9 | 3 | 6 | — | 6 | 0% (M7) |
| 12. 메트릭 (`product-op.md`) | 7 | 3 | 4 | — | 4 | 0% (M7) |
| 13. 검색 (`product-search.md`) | ~18 | 0 | ~18 | — | ~18 | 0% (v2) |
| 14. 미디어 (`product-media.md`) | 보류 | 0 | — | — | — | — |
| 15. 캐시 (`product-cache.md`, 신설) | — | 0 | — | — | — | — |
| 16. 알림 (`product-notification.md`, 신설) | — | 0 | — | — | — | — |
| 17. Admin (`product-admin.md`, 신설) | — | 0 | — | — | — | — |
| 18. FE CDN (`product-fe-cdn.md`) | — | 0 | — | — | — | — |
| 19. 부하 테스트 (`product-load-test.md`) | 6 | 0 | 6 | — | 6 | 0% (v1 이후) |
| **합계** | **~206** | **~40** | **~166** | **28 Story · 6 Epic PR · ~39.5 SP** | **~138** | **17%** ↑ |

### 📊 M3 예상 성과 카드

- **총 해결 대상**: 28 Story (전체 잔여 ~166의 **17% 소진**)
- **완주 예상 SDD Product**:
  - `product-learning-tower.md` Epic 1 (concepts[]) · Epic 2 (Layer) — 완주
  - `product-ai-suggestion.md` Epic 7 (개념 명세) — 완주 (Story 1건, 신설 Epic)
- **부분 진행 SDD Product**:
  - `product-learning-tower.md` Epic 3 (재정의판) — Roadmap 노드(3-6~3-8) + Selection 노드(3-9~3-11) 신설 6건 완주. 기존 Story 3-5 (문서화) 별도 잔여
  - `product-ai-suggestion.md` Epic 1 (6-Port DTO 4건 신설분) · Epic 2 (Static Adapter 4건 신설분) · Epic 3 Story 3-2 · Epic 5 Story 5-3 (Controller 부분)
- **M3 종료 후 남는 것** (다음 마일스톤 트라젝토리):
  - Learning Tower **25 Story 잔여** → M4 Epic 4 (Card→Axis · 5 Story) + M5 Epic 5 (Deck 폐기 · 5 Story) + M5 Epic 6 (Review · 5 Story) + Epic 3 문서화 잔여 + Epic 3 원안 Story 3-1~3-4 (SUPERSEDED 상태로 잔존)
  - AI Suggestion **23 Story 잔여** → M4~M5 Epic 3 3~5 role catalog 확장 (3 Story) + M6 Epic 4 (LLM Adapter · 5 Story) + M6~M7 Epic 5 (Cascade · Rate Limit · Controller 확장 · 3 Story) + M7 Epic 6 (관측성 · 4 Story) + 신설 Story SUPERSEDED 잔재
  - Card / Review / AI Interactive Roadmap — M4 (Card) · M5 (Review) · M6~M7 (Interactive) 진입 대기
  - 배포·관측·로깅·부하 — M7 배포 재개 대기
  - v2 이관: 검색 · 미디어 · 캐시 · 알림 · Admin

---

## Epic PR 매트릭스 (본주 잡힌 양)

Epic 단위 PR 진행 원칙:
- **한 Epic PR = 한 논리 단위 = 한 base 브랜치**. Squash merge 또는 rebase merge.
- Epic PR 안의 Story 커밋은 순차 누적 (`feat(scope): ...`).
- Reviewer 세션은 Epic PR 단위 (PR 하나에 5관점 병렬 발사).

### Epic PR #1 — `LT-E1-COMPLETE` (concepts[] 다중화 완주)

**Base 브랜치**: `feat/026-lt-e1-concepts-multiplex-complete` (M2의 `feat/025-lt-e1-s1-concept-table` 후속)

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-learning-tower.md`
- Epic: `# [Epic 1] LearningFacade.concepts[] 도입` (line 489~757)
- Story 범위: `## [Story 1-2]` (line 573) ~ `## [Story 1-5]` (line 712)
- 상위 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-04-concept-list.md`
- 기 완료: Story 1-1 (line 531, M2 V16 착지)

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | E1 S1-2 | `LearningFacadeConcept` Entity + 컬렉션 API + 정적 팩토리 | 2 |
| 2 | E1 S1-3 | 단수 `concept` 값 → `concepts[0]` 백필 + `updateConcepts()` 다건 행위 (부분성공 불허) | 1 |
| 3 | E1 S1-4 | API/DTO `concepts: string[]` 스위치 (Request/Response DTO 재작성) | 1 |
| 4 | E1 S1-5 | 도메인 검증 (1~5 · trim · blank/중복 거부) + ErrorCode 4종 (`LEARNING_FACADE_CONCEPTS_*`) | 1 |

**PR 종료 신호**: `POST /facades/me/concepts` `["백엔드","기획자","AI 엔지니어링"]` → 201 + `GET /facades/me.concepts` 3개 배열 확인 + blank/중복/6개 400 응답.

**병렬 진입 조건**: PR#6 (ADR023) 초안 병행. 다른 PR과 독립.

**Reviewer 세션**: 5관점 발사 (Domain / Architecture / API-Exception / Test / Sceptical).

### Epic PR #2 — `LT-E2-LAYER` (Layer 서버 도메인 신설)

**Base 브랜치**: `feat/027-lt-e2-layer-domain`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-learning-tower.md`
- Epic: `# [Epic 2] Layer 도메인 승격` (line 758~1041)
- Story 범위: `## [Story 2-1]` (line 799) ~ `## [Story 2-5]` (line 1000)
- 상위 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-05-layer-server-domain.md`

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | E2 S2-1 | `learning_layer` 테이블 + `Layer` Aggregate + `LearningFacade.addLayer()` 도메인 API | 2 |
| 2 | E2 S2-2 | `LearningAxis.facade_id` → `LearningAxis.layer_id` FK 재배선 (3단계 Flyway) | 2 |
| 3 | E2 S2-3 | default "Uncategorized" Layer 자동 생성 + 기존 axis 백필 | 2 |
| 4 | E2 S2-4 | `Layer` softDelete + orphan 정책 (자식 axis 확인 → 예외) | 1 |
| 5 | E2 S2-5 | Layer Controller + Layer displayOrder policy + 최대 개수 (10) | 2 |

**PR 종료 신호**: `POST /facades/me/layers { name:"기능의 구현" }` → 201 → `POST /layers/{id}/axes { name:"하네스 엔지니어링" }` → 200 → `GET /facades/me`가 `layers[].axes[]` 구조로 응답.

**의존**: PR#1 완주 이후 진입 (facade 도메인 stable 필요).

**Reviewer 세션**: 5관점 발사.

### Epic PR #3 — `LT-E3-ROADMAP-NODE` (Roadmap 노드 first-class 스키마·도메인·API)

**Base 브랜치**: `feat/028-lt-e3-roadmap-node-model`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-learning-tower.md`
- Epic: `# [Epic 3] Roadmap/Selection Dual-Axis + Selection 정책` (line 1042~1603) **재정의 판 (2026-07-02)**
- Story 범위: `## [Story 3-6]` (line 1335) ~ `## [Story 3-8]` (line 1421) — Roadmap 노드 3건 신설
- SUPERSEDED (참고): Story 3-1~3-4 (line 1094~1294) — 이슈 #6 원안 결정, `axis_roadmap.content TEXT` 통짜 방식
- 상위 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-15-roadmap-node-model.md`

| # | SDD Story | 한 줄 | SP |
|---|---|---|---|
| 1 | **LT E3 S3-6** | `AxisRoadmapNode` 도메인 신설 (`LearningAxis` Aggregate 확장, `addRoadmapNode`, `reorderRoadmapNodes`, `removeRoadmapNode`) | 2 |
| 2 | **LT E3 S3-7** | Flyway `V18__axis_roadmap_node.sql` + `R18__` 롤백 + `axis_roadmap` 아카이브 RENAME + best-effort 파싱 마이그레이션 | 2 |
| 3 | **LT E3 S3-8** | Roadmap 노드 API 6종 + ErrorCode 4종 (`ROADMAP_NODE_*`) + 기존 `PUT .../roadmap` 410 Gone + Controller/Repository Slice 테스트 | 3 |

**PR 종료 신호**: 
- `POST /axes/{axisId}/roadmap-nodes { title, rationale, body: "├── 1-1. 정의와 본질\n│       모델 + 하네스 — ..." }` → 201
- `PUT /axes/{axisId}/roadmap-nodes/order [nodeId1, nodeId2, ...]` → 200 + display_order 재부여 검증
- 이슈 #6 결정 뒤집기 문서에 명시 (이 PR 본문에 "SUPERSEDES issue-06" 표기)

**의존**: PR#2 완주 (axis가 Layer 아래 안정적으로 배선된 상태).

**Reviewer 세션**: 5관점 발사.

### Epic PR #4 — `LT-E3-SELECTION-NODE` (Selection 컨테이너 + 자식 노드 스키마)

**Base 브랜치**: `feat/029-lt-e3-selection-node-model`

**SDD 위치**:
- 파일: `workflow/task/pes/workspectrum/sdd/in-progress/product-learning-tower.md`
- Epic: `# [Epic 3] Roadmap/Selection Dual-Axis + Selection 정책` (line 1042~1603) **재정의 판 (2026-07-02)**
- Story 범위: `## [Story 3-9]` (line 1473) ~ `## [Story 3-11]` (line 1556) — Selection 노드 3건 신설
- 계승: 이슈 #11 컨테이너 정책 (name UNIQUE · created_at DESC · hard delete) — Story 3-9 내부 명시
- 상위 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-16-selection-node-model.md` + `issue-11-selections-version-policy.md` (재설계)

| # | SDD Story | 한 줄 | SP |
|---|---|---|---|
| 1 | **LT E3 S3-9** | `AxisSelectionNode` 도메인 신설 + 이슈 #11 컨테이너 정책(name UNIQUE · created_at DESC · hard delete) 계승 | 2 |
| 2 | **LT E3 S3-10** | Flyway `V19__axis_selection_node.sql` + `V20__archive_axis_selection_content.sql` + 롤백 · 파싱 마이그레이션 (Story 3-7 파서 재사용) | 2 |
| 3 | **LT E3 S3-11** | Selection 노드 API 5종 + ErrorCode 3종 (`SELECTION_NODE_*`) + Controller/Repository Slice 테스트 | 2.5 |

**PR 종료 신호**:
- Selection 컨테이너 생성 후 `POST /selections/{selectionId}/nodes` → 201
- 컨테이너 정책 (name UNIQUE, created_at DESC, hard delete) 유지 검증
- 이슈 #11 재설계 문서에 명시 ("컨테이너 정책 계승 · 노드 정책 신설")

**의존**: PR#3 완주 (Roadmap 노드 인프라 재사용, 파서·API 패턴 등).

**Reviewer 세션**: 5관점 발사.

### Epic PR #5 — `AS-6PORT-STATIC-CONCEPT` (6-Port + Static Adapter + 개념 명세 프롬프트)

**Base 브랜치**: `feat/030-as-6port-static-concept`

**SDD 위치** (5 Epic 걸침 — 복합 PR):

| SDD 부분 | 파일·위치 | 상위 이슈 |
|---|---|---|
| AS E1 Story 1-5~1-8 | `product-ai-suggestion.md` Epic 1 재정의 판 (line 485), Story 1-5 (line 716) ~ Story 1-8 (line 823) | `issue-17-ai-two-step-generation.md` |
| AS E2 Story 2-5~2-8 | `product-ai-suggestion.md` Epic 2 재정의 판 (line 859), Story 2-5 (line 1060) ~ Story 2-8 (line 1161) | `issue-17-ai-two-step-generation.md` |
| AS E3 Story 3-2 | `product-ai-suggestion.md` Epic 3 (line 1193), Story 3-2 (line 1291) — 기존 정의 그대로 활용 | `issue-10-role-catalog-expansion.md` (M2 착지분 계승) |
| AS E7 Story 7-1 | `product-ai-suggestion.md` Epic 7 신설 (line 2101), Story 7-1 (line 2133) | `issue-19-roadmap-selection-concept-spec.md` |
| AS E5 Story 5-3 (부분) | `product-ai-suggestion.md` Epic 5 (line 1697), Story 5-3 (line 1819) — M3 최소 형태로 6엔드포인트만 신설, 나머지 M4~M5 이관 | `issue-17-ai-two-step-generation.md` |

**SUPERSEDED (참고)**: AS E1 Story 1-3 (`RoadmapSuggestionPort`), Story 1-4 (`SelectionsSuggestionPort`), AS E2 Story 2-3, Story 2-4 — 4-Port 원안 결정, 6-Port로 뒤집힘.

| # | SDD Story | 한 줄 | SP |
|---|---|---|---|
| 1 | **AS E1 S1-5** | `ChaptersOutlinePort` + DTO record (Request/Response) | 0.5 |
| 2 | **AS E1 S1-6** | `ChapterSubtreePort` + DTO record | 0.5 |
| 3 | **AS E1 S1-7** | `SelectionOutlinePort` + DTO record | 0.5 |
| 4 | **AS E1 S1-8** | `SelectionSubtreePort` + DTO record | 0.5 |
| 5 | **AS E2 S2-5** | `StaticChaptersOutlineAdapter` — role-keyed catalog 조회 + `providerContext: "static:{role}"` | 1.5 |
| 6 | **AS E2 S2-6** | `StaticChapterSubtreeAdapter` | 1.5 |
| 7 | **AS E2 S2-7** | `StaticSelectionOutlineAdapter` | 1 |
| 8 | **AS E2 S2-8** | `StaticSelectionSubtreeAdapter` | 1 |
| 9 | **AS E3 S3-2** | `backend-developer.json` catalog (Layer/Axis/챕터 outline/subtree/Selection outline 통합 payload) | 2 |
| 10 | **AS E7 S7-1** | `concept-spec.txt` static asset + 6개 프롬프트 템플릿 include (roadmap/selections 개념 명세 · 판별기준) | 1 |

**참고**: AS E1 S1-1(LayerPort) · S1-2(AxisPort)는 이미 SDD에 정의됨. 본 PR에는 스켈레톤 확인만 (2-Port skeleton 유효). 신규 4-Port(1-5~1-8)만 실제 신설 대상. AS E5 (SuggestionController · REST 엔드포인트)는 M4~M5 이관 — v1엔 6-Port Port·Adapter 계약만 확립. AS E5 Story 5-3(Controller 4 → 6 엔드포인트 확장)은 M4 우선순위.

**보정 조치**: PR#5는 SDD 원본이 요구하는 REST Controller 정의 (AS E5 S5-3)를 부분 앞당김 — `SuggestionController`에 6개 엔드포인트 신설 (`POST /api/v1/suggestions/chapters-outline` · `chapter-subtree` · `selection-outline` · `selection-subtree` · `layer` · `axis`). SDD상 위치는 Epic 5지만 M3에 필요한 최소 형태로만.

| # | SDD Story (보정) | 한 줄 | SP |
|---|---|---|---|
| 11 | **AS E5 S5-3 (부분)** | `SuggestionController` 6 엔드포인트 신설 (M3 최소 형태) + `suggestionsAvailable: true` 응답 형식 + Rate Limit 유지 (기존 10rpm) | 1 |

**PR 종료 신호** (본 PR이 M3의 하이라이트 · "AI 결과 첫 노출"):
- `POST /api/v1/suggestions/chapters-outline { concepts:["백엔드","기획자"], layerName:"기능의 구현", axisName:"하네스 엔지니어링" }` → 200 + `{ chapters: [{title:"1. 하네스 엔지니어링 기초", rationale:"..."}, ...] }` (5개 챕터)
- `POST /api/v1/suggestions/chapter-subtree { chapter:{title, rationale}, siblings:[...] }` → 200 + `{ bodyAsciiTree: "├── 1-1. 정의와 본질\n│       ..." }`
- `providerContext: "static:backend-developer"` 응답 필드 존재
- Rate limit 10rpm 도달 시 429 + `Retry-After` 헤더

**의존**: PR#3·PR#4 완주 이후 진입 (챕터/노드 저장 대상 존재해야 후속 저장 orchestration 가능).

**Reviewer 세션**: 5관점 발사. **특히 Sceptical Reviewer가 "AI 결과가 실제로 사용자 만족할 초안인가"를 검증** (사용자 예시 하네스 로드맵 형태와 비교).

### Epic PR #6 — `ADR023-TERMINOLOGY` (roadmap/selections 용어 재정의)

**Base 브랜치**: `docs/024-adr023-roadmap-selections-terminology`

**문서 위치** (SDD 외부 · ADR):
- 신설 파일: `docs/adr/ADR023-terminology-roadmap-selections.md` (Plan 실행 시 신설)
- 인덱스 갱신: `docs/adr/index.md`
- 도메인 문서 정합 확인: `docs/DOMAIN.md` §LearningAxis · §학습 도메인
- 상위 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-19-roadmap-selection-concept-spec.md` §부속 결정 §도메인 spec embed (사용자 회의 2026-07-02 개념 명세)

| # | Story | 한 줄 | SP |
|---|---|---|---|
| 1 | ADR | ADR023 신설 (`docs/adr/ADR023-terminology-roadmap-selections.md`) — roadmap = 수렴/기준의 저장소 · selections = 발산/가능성의 저장소. 이식용 정의문 포함 | 1 |
| 2 | Index | `docs/adr/index.md` 업데이트 + DOMAIN.md §LearningAxis 정합 확인 | 0.1 |

**PR 종료 신호**:
- ADR023 머지 + `docs/adr/index.md`에서 참조 가능
- `docs/DOMAIN.md`의 관련 용어 사용처 정합 (roadmap/selection 표기 통일)

**의존**: 독립. D2에 착수 가능.

**Reviewer 세션**: 문서 전용이므로 review.md §6 스킵 조건 검토 (사용자 확인 후 결정).

---

## Story 카테고리별 합계 (SDD Story 단위)

| 카테고리 | SDD Epic/Story | Story 수 | SP | 비중 |
| --- | --- | --- | --- | --- |
| LT Epic 1 완주 (concepts[]) | LT E1 S1-2~S1-5 | 4 | 5 | 12% |
| LT Epic 2 (Layer) | LT E2 S2-1~S2-5 | 5 | 9 | 21% |
| LT Epic 3 재정의 (Roadmap 노드) | LT E3 S3-6~S3-8 | 3 | 7 | 16% |
| LT Epic 3 재정의 (Selection 노드) | LT E3 S3-9~S3-11 | 3 | 6.5 | 15% |
| AS Epic 1 확장 (6-Port DTO) | AS E1 S1-5~S1-8 | 4 | 2 | 5% |
| AS Epic 2 확장 (Static Adapter 4개) | AS E2 S2-5~S2-8 | 4 | 5 | 12% |
| AS Epic 3 (backend-developer catalog) | AS E3 S3-2 | 1 | 2 | 5% |
| AS Epic 7 신설 (개념 명세) | AS E7 S7-1 | 1 | 1 | 2% |
| AS Epic 5 부분 (Controller 6엔드포인트) | AS E5 S5-3 (부분) | 1 | 1 | 2% |
| ADR023 문서 | 해당 없음 (문서 전용) | 2 | 1 | 2% |
| **합계** | | **28** | **39.5** | 100% |

**분배 근거**:
- M2 원안 23 SP 대비 ~1.7배 (사용자 지시 "양이 조금 많아도" 반영). SDD Story 단위로 다시 분할하며 세밀도 조정.
- **AI 카테고리 26%** (AS E1+E2+E3+E5+E7) — "AI 결과 빠른 착지" 우선순위 반영. Static Adapter만으로도 사용자 초기 만족도 확보.
- **도메인 카테고리 64%** (LT E1+E2+E3) — 릴리스 스코프의 학습 대상 구조가 M3에 완주되어야 M4~M6이 그 위에서 자란다.
- **6개 Epic PR**: 각 PR = SDD Epic 하위 Story 묶음. Reviewer 세션 5관점 필수.
- **SDD Story 규칙 준수**: 모든 M3 작업이 `product-learning-tower.md`·`product-ai-suggestion.md`의 명시된 Epic/Story에 대응. 새 Story 추가 시 SDD 파일 선반영 후 milestone 참조 (본 M3에서 LT E3 S3-6~S3-11, AS E1 S1-5~S1-8, AS E2 S2-5~S2-8, AS E7 S7-1 총 12건 신설).

---

## 종료 신호 — "로컬에서 학습 대상 만들고 AI 초안 받고 저장까지 돈다"

본주 종료 시점에 다음이 모두 성립해야 한다. (8 신호 중 7개 이상 → 0.0.3v 동결)

- [ ] **머지 신호**: Epic PR 6개 중 최소 5개 머지 (83%)
- [ ] **concepts[] 신호**: `POST /facades/me/concepts ["A","B","C"]` → 201 + `GET /facades/me.concepts` 3개 배열. blank/중복/6개 400 응답 정합
- [ ] **Layer 신호**: `POST /facades/me/layers` → `POST /layers/{id}/axes` → `GET /facades/me`가 `layers[].axes[]` 계층 응답
- [ ] **Roadmap 노드 신호**: `POST /axes/{axisId}/roadmap-nodes` (title, rationale, body ASCII) → 201 + `PUT /axes/{axisId}/roadmap-nodes/order` → 200 (display_order 재부여). 이슈 #6 뒤집기 문서 자국 명시
- [ ] **Selection 노드 신호**: 컨테이너 생성 후 노드 CRUD 정상 + 이슈 #11 컨테이너 정책 유지
- [ ] **AI 첫 응답 신호** (M3 하이라이트): `POST /api/v1/suggestions/chapters-outline` → 200 + 챕터 리스트. `POST /api/v1/suggestions/chapter-subtree` → 200 + body ASCII 트리. `providerContext: "static:backend-developer"` 확인
- [ ] **ADR023 신호**: `docs/adr/ADR023-terminology-roadmap-selections.md` 머지 + `docs/adr/index.md` 반영 + `docs/DOMAIN.md` 정합
- [ ] **테스트 신호**: 5개 Epic PR 각각 도메인 단위 테스트 · Repository Slice · Controller Slice 통과. `./gradlew test` BUILD SUCCESSFUL. 신규 테스트가 해피/엣지/예외 3구분 각각 1건 이상

**미합격 처리**: 위 8 신호 중 6개 미만 성립 시 M3를 0.0.3v로 동결하지 않고 0.0.3.1v 패치 발행 → 다음 주 초까지 연장. (사용자 지시로 양이 늘어난 만큼 리스크 허용도 상승)

---

## 의존 chain

```
[선행: M2 착지]
LT E1 S1-1 (concept 테이블 V16) ✅ 완료

[D1: 병렬 착수]
PR#1 (LT-E1-COMPLETE)     ──►  PR#2 (LT-E2-LAYER)
  LT E1 S1-2 ~ S1-5              LT E2 S2-1 ~ S2-5
     │                              │
     └─► concepts[] 완주             └─► Layer 계층 완주
                                      │
                                      ▼
                              PR#3 (LT-E3-ROADMAP-NODE)
                              LT E3 S3-6 ~ S3-8
                                      │
                                      ▼
                              PR#4 (LT-E3-SELECTION-NODE)
                              LT E3 S3-9 ~ S3-11
                                      │
                                      ▼
                              PR#5 (AS-6PORT-STATIC-CONCEPT)
                              AS E1 S1-5~S1-8 · E2 S2-5~S2-8 · E3 S3-2 · E7 S7-1 · E5 S5-3 부분
                                      │
                                      ▼
                                   AI 첫 응답

PR#6 (ADR023) ── 독립 · D2에 착수 · D3 머지 가능
```

**병렬 진입 가능 묶음**:
- **A** (D1 · 07-08 Wed): PR#1 (LT E1 S1-2 착수) + PR#2 (LT E2 S2-1 Flyway V17 준비) + PR#6 (ADR023 초안) 동시 시작
- **B** (D2 · 07-09 Thu): PR#1 LT E1 S1-3 진행 + PR#2 LT E2 S2-2 (Axis FK 재배선) + PR#6 머지 준비
- **C** (D3 · 07-10 Fri): PR#1 LT E1 S1-4/S1-5 완주 → PR#1 머지 · PR#2 LT E2 S2-3 + PR#6 머지
- **D** (D4 · 07-11 Sat): PR#2 LT E2 S2-4/S2-5 완주 → PR#2 머지 · PR#3 착수 (LT E3 S3-6 Roadmap 노드 도메인)
- **E** (D5 · 07-12 Sun): PR#3 LT E3 S3-7 (Flyway V18) + S3-8 (API) 완주 → PR#3 머지 · PR#4 착수 (LT E3 S3-9 Selection 노드 도메인)
- **F** (D6 · 07-13 Mon): PR#4 LT E3 S3-10/S3-11 완주 → PR#4 머지 · PR#5 착수 (AS E1 S1-5~S1-8 Port DTO)
- **G** (D7 · 07-14 Tue): PR#5 AS E2 S2-5~S2-8 + E3 S3-2 + E7 S7-1 + E5 S5-3 부분 완주 → PR#5 머지 (Static Adapter backend-developer catalog 완성 + AI 첫 응답 검증) · 통합 로컬 검증 · 문서(outcome/review) 골격

**직렬 (M3 합격선까지)**: PR#6 (D2-D3 독립) + PR#1 (D1-D3) + PR#2 (D1-D4) → PR#3 (D4-D5) → PR#4 (D5-D6) → PR#5 (D6-D7). 순차 chain이 D7에 종료.

---

## 작업 일정 (Epic PR 단위 체크리스트)

D1 = 2026-07-08 (Wed). 종료 D7 = 2026-07-14 (Tue). 7일 안에 6 Epic PR.

| 일 | 날짜 | 잡힌 작업 (Epic PR 진행) — SDD Story 기준 |
| --- | --- | --- |
| D1 (수) | 07-08 | PR#1 착수 (**LT E1 S1-2** `LearningFacadeConcept` Entity 시작) + PR#2 착수 (**LT E2 S2-1** `learning_layer` 테이블 Flyway V17 준비) + PR#6 착수 (ADR023 초안) |
| D2 (목) | 07-09 | PR#1 **LT E1 S1-3** (`updateConcepts()` 다건) + PR#2 **LT E2 S2-2** (Axis FK 재배선) + PR#6 머지 준비 |
| D3 (금) | 07-10 | PR#1 **LT E1 S1-4** (DTO 스위치) + **S1-5** (ErrorCode) 완주 → **PR#1 머지 + Reviewer 세션** · PR#2 **LT E2 S2-3** (default Layer 백필) + PR#6 머지 |
| D4 (토) | 07-11 | PR#2 **LT E2 S2-4** (softDelete) + **S2-5** (Controller) 완주 → **PR#2 머지 + Reviewer 세션** · PR#3 착수 (**LT E3 S3-6** `AxisRoadmapNode` 도메인) |
| D5 (일) | 07-12 | PR#3 **LT E3 S3-7** (Flyway V18 + 파싱) + **S3-8** (API + ErrorCode + 테스트) 완주 → **PR#3 머지 + Reviewer 세션** · PR#4 착수 (**LT E3 S3-9** `AxisSelectionNode` 도메인) |
| D6 (월) | 07-13 | PR#4 **LT E3 S3-10** (Flyway V19·V20) + **S3-11** (API + ErrorCode + 테스트) 완주 → **PR#4 머지 + Reviewer 세션** · PR#5 착수 (**AS E1 S1-5~S1-8** 4개 Port + DTO) |
| D7 (화) | 07-14 | PR#5 **AS E2 S2-5~S2-8** (Static Adapter 4개) + **E3 S3-2** (`backend-developer.json`) + **E7 S7-1** (concept-spec.txt) + **E5 S5-3 부분** (6 엔드포인트) 완주 → **PR#5 머지 + Reviewer 세션 (Sceptical 강조)** · 통합 로컬 검증 3시나리오 + `outcome.md` · `review.md` 골격 · 0.0.3v 동결 판정 |

**Reviewer 세션 규칙**: 각 Epic PR 머지 직전 5관점 병렬 발사 (`.claude/rules/review.md` §3~§7). 사용자 확인 후 머지.

**Flyway V 버전 순서 관리** (M2에서 V16 완주됨):
- V17: `learning_layer` 테이블 (PR#2)
- V18: `axis_roadmap_node` 테이블 + `axis_roadmap` archive (PR#3)
- V19: `axis_selection_node` 테이블 (PR#4)
- V20: `axis_selection.content` archive (PR#4)

D1 시작 전 V 버전 할당표를 사용자 확인 필수. 충돌 방지.

---

## 리스크와 관찰 포인트

| 영역 | 리스크 | 관찰 포인트·완화 |
| --- | --- | --- |
| 6 Epic PR 촘촘 일정 | D3까지 3개 머지가 목표. PR#1이 슬립 시 PR#2·PR#3 이후 chain 전체 밀림 | D3 종료 시점에 PR#1·PR#6 머지 진행률 확인. 미달 시 D5부터 여유 있는 조정 (PR#4를 M4로 이관 or PR#5의 Static Adapter role 축소 — backend-developer 하나만 유지) |
| Roadmap `axis_roadmap.content` 이관 파싱 | 기존 데이터가 있으면 파서 미완성 시 backfill 실패 | M3 초기 3명 사용자 규모라 데이터 극소량. Best-effort 파서 + 실패 시 통짜 노드로 저장 + 로깅. PR#3에서 명시적 검증 |
| Flyway V버전 순서 (V17~V20) | 두 개 Epic PR이 병렬 진행 시 V버전 겹침 위험 | D1 오전에 V버전 할당표 사용자 확인. PR#2 V17 · PR#3 V18 · PR#4 V19·V20 순차 확정 |
| Static Adapter catalog 품질 | `backend-developer.json`의 챕터 outline · subtree 예시 품질이 M3 하이라이트 (AI 첫 응답)의 사용자 만족도를 결정 | D6 시작 시점에 사용자 예시(하네스 로드맵) 반영 confirmation. PR#5의 Sceptical Reviewer가 "이 초안이 실제로 사용자 만족할 수준인가" 명시 판정 |
| 6-Port 인터페이스 정합 | 6개 Port의 Request/Response record 시그니처가 M4~M6에 소비될 것. 여기서 이름·타입 잘못 잡으면 후속 마일스톤 재작업 | PR#5 착수 전 `product-ai-suggestion.md` §6-Port 시그니처 조회 확인. record 필드 명명은 spec과 정확히 일치 |
| PR 리뷰 병목 | 5관점 Reviewer 세션이 각 PR마다 필요. 6 PR × 5관점 = 30 reviewer 발사 | 병렬 발사로 시간 압축 (`.claude/rules/review.md` §7). 리뷰 결과 종합 후 사용자 판정 |
| 이슈 #6·#11 뒤집기 자국 | PR#3·PR#4는 이슈 #6·#11의 결정을 뒤집는 것. PR 본문에 명시 필요 | PR 본문 템플릿에 "SUPERSEDES issue-06" · "SUPERSEDES issue-11 (컨테이너 정책 계승)" 표기. Reviewer가 확인 |
| M2 미완주 잔여 | `feat/025-lt-e1-s1-concept-table` 브랜치에 미머지 커밋 존재? | D1 시작 전 M2 브랜치 상태 확인. LT E1 S1이 이미 머지됐다면 clean start · 미머지면 D1 오전 정리 |
| 스코프 확장 압박 | "양이 조금 많아도" 사용자 지시로 43 SP · 2배 폭. 실제 속도 미검증 | D3 진행률 (PR#1·PR#6 머지) 확인 → 미달 시 PR#5 축소 (Static Adapter backend-developer만 유지, 다른 role은 M4~M5) |
| ADR023 반전 부담 | roadmap/selections 어휘가 이미 spec에 반영됐지만 ADR 공식 결정은 M3 | PR#6 D2 초안 확정 → D3 사용자 검토 후 머지. 반전 시 spec 조정 여지 (드물지만 대비) |

---

## 다음 마일스톤 (M4 / 0.0.4v) 후보

본주 결과를 보고 결정하지만, 릴리스 문서에 잠긴 트라젝토리 그대로:

**M4 / 0.0.4v (07-15 ~ 07-21) — Card 리팩토링 + AI 6-Port 스켈레톤 확장**
- **이슈 #21 이관** (Mode enum 재편 — `MODE_7D/14D/28D/60D`) + `LearningModeMappingPolicy` 재작성 + 데이터 마이그레이션 (`MODE_10D→7D` 등)
- **이슈 #22 이관** (OnFieldBudget 폐기 + `ArchiveReason` 재편 — `MANUAL / SCHEDULE_EXHAUSTED / MODE_DOWNGRADED`)
- **이슈 #23 이관** (Card `createdMode` 필드 + M3 하이브리드 도메인 메서드 — `effectiveMaxDays`, `isDueOn`, `hasScheduleExhausted`)
- **AI Static Adapter 다중 role 확장** — planner / designer / problem-solver catalog 3종 신설 (M3의 backend-developer 이후 3개 role catalog 완주)
- **이슈 #07 잔여** (Card → Axis 직접 매핑 마이그레이션, Coverage 재계산) 병행 검토

**Epic PR 예상 4~5건** (Card 리팩토링 3건 + AI role catalog 확장 1건 + Card→Axis 이관 1건).

---

## Product 상태 전환 신호 (M3 종료 시)

- `in-progress/product-learning-tower.md` — **Epic 1·2 완주** 표기. **Epic 3(Roadmap/Selection 노드) 완주** 표기 (이슈 #15/#16 이관). Epic 4(Card→Axis) M4 이관. Epic 5(Deck 폐기) M5 이관. Epic 6(Review 재편) M5 이관
- `in-progress/product-ai-suggestion.md` — **6-Port 인터페이스 확정** 표기. Static Adapter backend-developer 활성. 나머지 3 role · LLM Adapter · Cascade는 M4~M6
- `in-progress/product-ai-interactive-roadmap.md` — 진입 X, 상태 유지 (M7 이관)
- `in-progress/product-card.md` — 진입 X, 상태 유지 (M4 진입)
- `in-progress/product-review.md` — 진입 X, 상태 유지 (M5 진입)
- `fix/brainstorming/version/0.0.2v/issue-04/05/08` → resolved (M2 파생 완료)
- `fix/brainstorming/version/0.0.2v/issue-15/16/17/19` → **partial resolved** — 노드 스키마·6-Port·개념 명세 각각 M3 이관 완료. LLM Adapter는 M6 남음
- `fix/brainstorming/version/0.0.2v/issue-06/11` → **SUPERSEDED** 표기 확정 (M3 PR#3·PR#4에서 뒤집힘)
- `fix/brainstorming/version/0.0.2v/issue-18/20/21/22/23/24/25/26` → M4~M6 이관 유지
- `docs/adr/ADR023-terminology-roadmap-selections.md` → 신설·머지 완료

---

## brainstorming 트리거

본 M3 완료 후 `workflow/task/pes/brainstorming/0.0.4v/` 신설:
- Card Mode 재편 UX (`brainstorming/0.0.4v/mode-migration.md`) — 사용자 mode 다운 시 카드 예상 archive 사전 안내 UX 결정
- Static Adapter 4-role catalog 튜닝 (`brainstorming/0.0.4v/role-catalog-tuning.md`) — backend-developer 관찰 결과 반영 · 나머지 3-role 착수
- Card→Axis 직접 매핑 마이그레이션 시나리오 (issue-07) — Coverage 재계산 후보
- 6-Port outline → subtree flow의 세션 상태 반영 (`brainstorming/0.0.4v/session-state-expansion.md`) — M7 대비 Wizard 7단계 확장 · session state 확장 명세 세밀화
- Static → LLM Adapter 전환 시점 (`brainstorming/0.0.4v/llm-adapter-timing.md`) — Cascade fallback 정책 · Vertex AI GCP ADC 인프라 준비 시작 시점

---

## 참고

- 잔여 Story 인벤토리 출처: `workflow/task/pes/workspectrum/sdd/in-progress/` 19개 Product 파일 (Product · admin · ai-interactive-roadmap · ai-suggestion · auth · cache · card · fe-cdn · infra-deploy · infra-network · infra-ops · learning-tower · load-test · log · media · notification · op · review · search)
- 07-02 pivot 이슈 원천: `workflow/task/fix/brainstorming/version/0.0.2v/issue-15 ~ #26` 12건
- 마일스톤 패키지 의도: `workflow/task/milestones/references/001.md`
- FE 대응 마일스톤: `workflow/task/pes/fe/fe-milestones/version/0.0.3v/milestone.md` (동일 주간)
- 이전 마일스톤: `workflow/task/milestones/version/0.0.2v/milestone.md` — M2 결과 · pivot 산출물
- **첫 릴리스 계획**: `workflow/task/milestones/release/version/0.0.1v/release.md` — 0.1.0v (~2026-08-19) 스코프 · M3~M8 트라젝토리
- 본 버전의 산출물 5종: `infra.md`(스킵 사유만), `performance.md`(스킵 사유만), `outcome.md`, `cost.md`, `review.md`
- 양식 진화: 본 milestone은 0.0.2v milestone.md 양식 답습 + **Epic PR 매트릭스** 신설 (사용자 지시 "epic 단위로 pr 진행" 반영) + **SDD Story 단위 정합 규칙** 적용 (사용자 지시 "sdd 기준의 epic, story 단위로 쪼개는 규칙" 반영 · SDD 파일에 신규 Story 선반영 후 milestone 참조)
- **SDD 신설 Story (M3 선반영)**:
  - `product-learning-tower.md` Epic 3 재정의 판 — Story 3-6, 3-7, 3-8, 3-9, 3-10, 3-11 신설 (이슈 #15/#16 이관)
  - `product-ai-suggestion.md` Epic 1 재정의 판 — Story 1-5, 1-6, 1-7, 1-8 신설 (6-Port 확장)
  - `product-ai-suggestion.md` Epic 2 재정의 판 — Story 2-5, 2-6, 2-7, 2-8 신설 (Static Adapter 6-Port)
  - `product-ai-suggestion.md` Epic 7 신설 — Story 7-1 (개념 명세 프롬프트 embed, 이슈 #19 이관)
