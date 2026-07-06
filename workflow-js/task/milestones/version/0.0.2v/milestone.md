# M2 / 0.0.2v — Week of 2026-07-01 ~ 2026-07-07 (2차 재편 D2 = 2026-07-02 Thu)

> **마일스톤의 역할**: 큰 작업 단위(Product)는 `workflow/task/pes/workspectrum/sdd/in-progress/`에 정의되어 있고, M1을 통과해 0.0.1v가 동결됐다. 본 문서는 **두 번째 버전에 잡힌 양의 분배 결정** + **버전 단위 산출물 묶음**.
>
> 한 주 = 한 버전 = `version/0.0.X v/` 폴더 하나. 본 버전(0.0.2v)에는 다음 6 파일이 들어간다:
> - `milestone.md` *(본 문서)* — 잡힌 양 + 일정 + 의존
> - `infra.md` — (본 버전 배포 미포함 사유 명시)
> - `performance.md` — (본 버전 성능 측정 미수행 사유 명시)
> - `outcome.md` — 사용자·기능 성과 (concepts[] 다중화 + Layer 서버 도메인 도입 + AI/Card/Review 리팩토링 도큐먼트 착지 결과)
> - `cost.md` — 비용 성과 (본 버전 로컬 개발 한정 → 비용 발생 0)
> - `review.md` — 회고 + 다음 버전 진입 신호

**본 버전의 릴리스 대응**: 본 M2에서 착지된 도큐먼트·이슈들이 **첫 번째 사용자 릴리스(0.1.0v, ~2026-08-19경)** 의 기반이 된다. 실제 릴리스 계획은 `workflow/task/milestones/release/version/0.0.1v/release.md` 참조 — 본 문서는 M2 주간 스코프, 릴리스 문서는 MVP 스코프·기간·기능 라인.

---

## 0.0.2v 2차 재편 (D2 pivot, 2026-07-02) — AI/Card/Review 도큐먼트 대전환

**07-02 pivot 트리거**: 07-01 재편으로 도메인 첫 조각(concepts[]+Layer)에 집중하기로 했으나, D2에 다음 세 회의가 연달아 열리며 **AI Roadmap 생성·Card lifecycle·Review 세션 전체 재설계**가 확정됨:

1. **AI Roadmap 생성 방식 재설계 회의** (D2 오전) — `RoadmapSuggestionPort` 통짜 생성 → `ChaptersOutlinePort` + `ChapterSubtreePort` 2단계 flow(A) 전환. Coarse 노드 승격 + roadmap/selections 개념 명세 프롬프트 embed. **6개 신규 이슈 발행 (#15~#20)**, 3개 product spec refactoring (learning-tower / ai-suggestion / ai-interactive-roadmap).
2. **Card + Review 리팩토링 회의** (D2 오후) — OnFieldBudget 이중 게이트 폐기 → fixed interval queue + M3 하이브리드 mode 매핑(down=cap, up=새 card만). DailyLearningBatch aggregate 신설 (cross-layer 짬뽕 + 캐시 용량 측정 대시보드). **6개 신규 이슈 발행 (#21~#26)**, 2개 product spec 신설(product-card·product-review, 각 SDD 풀 양식).
3. **SDD 양식 재정합** — 두 신규 product spec을 sdd.md 양식(15섹션 + Epic·Story 바디)에 맞춰 완전 재작성. product-card 3 Epic·16 Story, product-review 3 Epic·23 Story.

**07-02 pivot 결과 — 산출물 요약**:

| 산출물 | 수량 | 위치 |
|---|---|---|
| 신규 fix 이슈 (Roadmap/Selection 노드 스키마 · 6-Port AI · 개념 명세 · 비용 관찰) | 6건 | `workflow/task/fix/brainstorming/version/0.0.2v/issue-15 ~ #20` |
| 신규 fix 이슈 (Mode enum 재편 · OnFieldBudget 폐기 · Card createdMode M3 · DailyBatch · Cross-layer · 대시보드) | 6건 | `workflow/task/fix/brainstorming/version/0.0.2v/issue-21 ~ #26` |
| Product spec refactoring (in-place, in-progress 갱신) | 3건 | `product-learning-tower.md` · `product-ai-suggestion.md` · `product-ai-interactive-roadmap.md` |
| Product spec 신설 (SDD 풀 양식) | 2건 | `product-card.md` · `product-review.md` |

**Total: 12개 신규 이슈 · 5개 product spec 갱신·신설 · Story 규모 100+ 신설.**

**정책 재확정 (D2 pivot 이후)**: 
- **본 M2의 실제 산출물은 도큐먼트(이슈 + product spec)** 이 주력. LT Epic 1 Story 1(concept 테이블 Flyway V16)만 실제 코드 구현 완료 (07-01/02 브랜치 `feat/025-lt-e1-s1-concept-table`, V16/R16 커밋 306514b·9542b43).
- **원안 Tier 1 구현 12 Story 중 나머지 11건 (LT E1 S2~S5, LT E2 S1~S5, AS E1, ADR023)는 M3 이후로 이관**.
- **M3부터 본격 구현 착수**: 이슈 #15~#26의 이관 산출물이 M3~M8 스코프의 원천 (릴리스 문서 참조).

---

## 0.0.2v 스코프 메모 (mid-week 재편, 2026-07-01)

원래 0.0.2v 목표는 "LLM 실제 활성화 + 운영 안정화 + 성능 baseline 측정"(42 SP)이었으나, 다음 두 신호가 겹쳐 **전면 재편**한다:

1. **도메인 리팩토링 신호** — `workflow/task/fix/brainstorming/version/0.0.2v/issue-04 ~ 14` (11건)가 발행되며 도메인 자체의 큰 변경이 확정됐다. concepts[] 다중화 (04) / Layer 서버 도메인 승격 (05) / Roadmap·Selection 이원 축 (06) / Card→Axis 직접 매핑 (07) / SDD 용어 재정의 (08) / AI 4-Port 확장 (09) / Role 카탈로그 (10) / Selection 정책 (11) / 순서 정합 (12) / Deck 완전 폐기 (13) / Review 재설계 (14).
2. **정책 전환 신호** — 배포 이전에 **local에서 의도가 드러나는 데까지 먼저 진행**. 이번 버전의 성과 측정은 "**local 기능 구현 + local 기능 테스트**"이며 배포·인프라·관측성은 후속 버전으로 미룬다.

두 신호를 반영해 본 버전은 다음 3축으로 재편:

1. **도메인 첫 조각 착지 (concepts[] + Layer)** — issue-04/05가 이후 모든 리팩토링의 전제라 우선 착수. LearningFacade의 concept 단일 필드를 다중 컬렉션으로, Axis의 상위를 Layer로 재배선.
2. **AI Port 골격 (issue-09 partial)** — 실제 Adapter는 다음 버전으로 미루되, 4-Port 인터페이스와 Context/VO record 스켈레톤은 확정. 후속 Story가 Port를 구현하는 시그니처가 잠긴다.
3. **용어 정합 (issue-08 ADR)** — Roadmap/Selection 어휘가 사용자 정의(헌법/판례)와 SDD 내부 정의(초안/합의최종안) 사이에 충돌 중. 어휘 확정 ADR을 먼저 머지해 이후 리팩토링의 기준선을 만든다.

**본 버전 제외 사유**:
- **원안 3축(배포·운영·성능)** → 배포 자체가 후속 버전. LLM 실제 활성화는 AI Static Adapter가 먼저 자리 잡은 뒤. 성능 baseline은 도메인 안정 후.
- **Deck 완전 폐기 (issue-13) · Review 이중 스코프 재편 (issue-14)** → 매우 큰 breaking change로 Card→Axis 직접 매핑(07) + Deck 폐기(13) + Review 재설계(14)를 한 클러스터로 묶어 다음 버전 이후에 처리.
- **LT Epic 3 (Roadmap/Selection) · Epic 4 (Card→Axis) · AI Adapter 구현** → 도메인 첫 조각 착지 후 다음 버전.

---

## 진행 중 Product 잔여 인벤토리 (M2 재편 시점)

| Product | 총 Story | 완료(머지) | 잔여 | M2 대상 |
| --- | --- | --- | --- | --- |
| 1. 인증 인프라 (`in-progress/product-auth.md`) | 10 | 10 | 0 | — (M3에 done/ 이동 검토) |
| 2. User BC 정합성 (`in-progress/Product.md`) | 8 | 7 | 1 | — (잔여 1 Story는 M3 정리) |
| 3. Learning Tower (`in-progress/product-learning-tower.md`, 신설) | 36 | 0 | 36 | **10 Story** (Epic 1 · 1-1~1-5 + Epic 2 · 2-1~2-5) |
| 4. AI Suggestion (`in-progress/product-ai-suggestion.md`, 4-Port 재구조) | 31 | 5 | 26 | **1 Story** (Epic 1 · 4-Port 인터페이스 스켈레톤) |
| 5. AI Interactive Roadmap (`in-progress/product-ai-interactive-roadmap.md`) | 12 | 0 | 12 | — (M3 이후) |
| 6. 컨테이너 배포 (`in-progress/product-infra-deploy.md`) | 9 | 5 | 4 | — (배포 미포함) |
| 7. AWS 네트워크 (`in-progress/product-infra-network.md`) | 8 | 3 | 5 | — (배포 미포함) |
| 8. Secrets·백업·관측 (`in-progress/product-infra-ops.md`) | 10 | 2 | 8 | — (배포 미포함) |
| 9. k6 부하 테스트 (`ready/product-load-test.md`) | 6 | 0 | 6 | — (배포 미포함) |
| 0-a. 로깅 (`in-progress/product-log.md`) | 9 | 3 | 6 | — (배포 미포함) |
| 0-b. 메트릭 (`in-progress/product-op.md`) | 7 | 3 | 4 | — (배포 미포함) |
| 10. 검색 (`in-progress/product-search.md`) | ~18 | 0 | ~18 | — |
| 11. 미디어 (`in-progress/product-media.md`) | 보류 | 0 | — | — |
| **합계** | **~164** | **~38** | **~126** | **11 Story (Tier 1) + 4 Story (Tier 2)** |

> **신설/재구조 신호**: `product-learning-tower.md`는 fix issue-04~07/13/14 통합 신설. `product-ai-suggestion.md`는 4-Port 아키텍처로 재구조(기존 Epic 1~2는 흡수, Epic 3 LLM 활성화는 4개 Adapter Epic으로 재분해).
> **배포 라인 6~11번 Product**: 본 버전 스코프에서 전량 제외. 각각 다음 버전 재검토.

---

## 본주 잡힌 양 (M2 재편 — 15 Story)

### Tier 1 · Must (M2 합격선 — 11 Story)

| # | Product | Story | 한 줄 | SP |
| --- | --- | --- | --- | --- |
| 1 | LT | **Epic 1 / 1-1** `learning_facade_concept` 테이블 + Flyway (V+R) | facade_id FK + display_order + `UNIQUE(facade_id, value)` | 1 |
| 2 | LT | **Epic 1 / 1-2** `LearningFacadeConcept` Entity + 컬렉션 API | 정적 팩토리 + 1-based displayOrder + `addConcept()` / `reorderConcepts()` | 2 |
| 3 | LT | **Epic 1 / 1-3** 기존 concept 값 백필 + `updateConcepts()` 다건 행위 | 부분성공 불허 + 결과 VO(`ConceptsChangeRecord`) | 1 |
| 4 | LT | **Epic 1 / 1-4** API/DTO `concepts: string[]` 스위치 | `LearningFacadeUpdateRequest` + Response DTO 수정 | 1 |
| 5 | LT | **Epic 1 / 1-5** 도메인 검증(1~5개, blank/중복 거부) + ErrorCode 등록 | `LEARNING_FACADE_CONCEPTS_*` 4종 (LIMIT_EXCEEDED / BLANK / DUPLICATE / MIN_REQUIRED) | 1 |
| 6 | LT | **Epic 2 / 2-1** `learning_layer` 테이블 + Layer Aggregate | facade_id FK + name + display_order + deleted_at | 2 |
| 7 | LT | **Epic 2 / 2-2** `LearningAxis.facade_id` → `LearningAxis.layer_id` FK 재배선 | Flyway V+R, axis→facade 경유 쿼리를 layer 경유로 | 2 |
| 8 | LT | **Epic 2 / 2-3** default "Uncategorized" Layer 자동 생성 + 백필 | facade별 1개 default Layer 자동 발행 + 기존 axis 전체 이관 | 2 |
| 9 | LT | **Epic 2 / 2-4** Layer softDelete (ADR003 확장) + orphan-safe cascade | `@SQLRestriction` + Layer 삭제 시 axis 처리 정책 명시 | 1 |
| 10 | LT | **Epic 2 / 2-5** Layer 최대 개수·displayOrder 정책 + Controller | `/facades/me/layers/*` POST·PATCH·DELETE·PUT reorder 4엔드포인트 + validation | 2 |
| 11 | AS | **Epic 1** 4-Port 인터페이스 + Context/VO record 스켈레톤 | `LayerSuggestionPort` · `AxisSuggestionPort` · `RoadmapSuggestionPort` · `SelectionsSuggestionPort` + 각 3 record | 2 |
| 12 | 문서 | **ADR023** SDD 용어 재정의 (Roadmap = 헌법 / Selection = 판례) | `docs/adr/ADR023-terminology-roadmap-selections.md` + `docs/adr/index.md` + `docs/DOMAIN.md` 정합 확인 | 1 |

**Tier 1 합계: 12 Story · ~18 SP**

### Tier 2 · Want (도전 — AI Static 첫 발, 4 Story)

| # | Product | Story | 한 줄 | SP |
| --- | --- | --- | --- | --- |
| 13 | AS | **Epic 3 / 3-1** `RoleDetector` 구현 (concepts→role 자동 감지) | keyword-match 기반 4-role 감지 + 다중 role 처리 규칙 | 1 |
| 14 | AS | **Epic 3 / 3-2** `backend-developer.json` catalog 리소스 파일 | layer/axis/roadmap/selection sample payload 4종 | 1 |
| 15 | AS | **Epic 2 / 2-1** `StaticLayerSuggestionAdapter` + RoleCatalog 조회 | role hint 우선 → problem-solver 폴백 | 2 |
| 16 | 문서 | 0.0.2v `infra.md`·`performance.md` 스킵 사유 + `outcome.md`·`cost.md`·`review.md` 골격 | 본 버전 배포·성능 미포함 명시 + 종료 신호 판정 기록지 준비 | 1 |

**Tier 2 합계: 4 Story · ~5 SP**

### 카테고리별 합계

| 카테고리 | Story 수 | SP | 비중 |
| --- | --- | --- | --- |
| 도메인 (LT Epic 1 concepts[]) | 5 | 6 | 26% |
| 도메인 (LT Epic 2 Layer) | 5 | 9 | 39% |
| AI Port 골격 (AS Epic 1) | 1 | 2 | 9% |
| 문서 / ADR | 1 | 1 | 4% |
| **Tier 1 소계** | **12** | **18** | **78%** |
| Tier 2 (AI Static 첫 발 + 산출물 골격) | 4 | 5 | 22% |
| **합계** | **16** | **23** | 100% |

**분배 근거**:
- M1(47 SP)과 M2 원안(42 SP) 대비 **약 절반(23 SP)** — 도메인 리팩토링은 마이그레이션·백필·역호환 검증 부담이 커서 SP 총량을 낮추고 완성도를 높인다.
- **도메인 65% + AI 9% + 문서 4% + Want 22%** — 배포·성능·관측성 카테고리 전량 제외. 로컬 features + testing에만 집중.
- **AI Port 골격만 Tier 1**: Adapter 구현 착수 전 시그니처 확정으로 이후 Story가 병렬로 진입 가능해진다.
- **문서 축의 ADR023**: Roadmap/Selection 어휘가 후속 리팩토링 전반의 명명에 영향. 어휘 결정 지연 시 코드 이름 재작업 위험.

---

## 종료 신호 — "로컬에서 concepts[] + Layer 도메인이 돌아가고 테스트된다"

본주 종료 시점에 다음이 모두 성립해야 한다. (Tier 1 기준, 8 신호 중 6개 이상 → 0.0.2v 동결)

- [ ] **머지 신호**: Tier 1 12 Story 중 최소 10 머지 (83%)
- [ ] **concepts[] 신호**: 로컬 H2에서 `POST /facades/me/concepts` `["A","B","C"]` → 201 + `GET /facades/me` 응답에 `concepts: ["A","B","C"]` 순서·개수 일치. blank / 중복 / 6개 이상 시 400 + `LEARNING_FACADE_CONCEPTS_*` ErrorCode 응답
- [ ] **Layer 도메인 신호**: 로컬 H2에서 `POST /facades/me/layers { name:"UI" }` → 201 → `POST /layers/{id}/axes { name:"컴포넌트 설계" }` → 200 → `GET /facades/me` 응답에 `layers[0].axes[0]` 포함
- [ ] **마이그레이션 신호**: `./gradlew clean bootRun` 시 Flyway가 신규 V 버전 전부 오류 없이 통과 + 기존 facade의 concept 단일값 → `concepts[0]` 백필 검증 + 기존 axis 전부 default "Uncategorized" Layer로 이관 검증
- [ ] **테스트 신호**: `./gradlew test`의 신규 테스트가 **해피 / 엣지 / 예외** 3구분으로 각각 1케이스 이상 존재 + BUILD SUCCESSFUL 유지 (`.claude/rules/conventions.md` §4.2)
- [ ] **Port 골격 신호**: 4개 Port 인터페이스 + Context/VO record 컴파일 통과 + 향후 Adapter 구현체가 Port를 implement할 수 있는 시그니처 확정 (임의 Adapter stub 컴파일 실험 통과)
- [ ] **ADR 신호**: `docs/adr/ADR023-terminology-roadmap-selections.md` 머지 + `docs/adr/index.md` 반영 + `docs/DOMAIN.md`의 관련 용어 사용처 정합 확인
- [ ] **문서 정합 신호**: `docs/DOMAIN.md` §LearningFacade / §LearningAxis 절에 concepts[]·Layer 반영 + fix issue 링크 (issue-04/05/08)

**Tier 2 추가 신호** (Want, 미달 시 M2 판정에 영향 없음):
- [ ] **AI Static 신호**: 로컬에서 `RoleDetector.detect(["Spring Boot","JPA"])` → `backend-developer` role 반환 + `StaticLayerSuggestionAdapter.suggest(context)`가 catalog JSON을 로드해 응답 발행

**미합격 처리**: 위 8 신호 중 6개 미만 성립 시 M2를 0.0.2v로 동결하지 않고 0.0.2.1v 패치 발행 → 다음 주 초까지 연장.

---

## 의존 chain

```
[LT Epic 1 concepts[] — 선형]
1 (테이블) ── 2 (Entity) ── 3 (백필 + 도메인 API) ── 4 (DTO 스위치) ── 5 (검증 + ErrorCode)

[LT Epic 2 Layer — 선형, Epic 1과 병렬]
6 (테이블 + Aggregate) ── 7 (Axis FK 재배선) ── 8 (default Layer 백필) ── 9 (softDelete) ── 10 (Controller)

[AI Port 골격 — 독립]
11 (4 Port 인터페이스 + record)

[문서]
12 (ADR023 + DOMAIN.md)

[Tier 2 — Tier 1 완료 후]
13 (RoleDetector) ── 14 (catalog JSON) ── 15 (StaticLayerAdapter — Port 11 + RoleDetector 13 의존)
16 (0.0.2v 산출물 골격)
```

**병렬 진입 가능 묶음**:
- **A** (D1): 1 (테이블 concept) + 6 (테이블 layer) + 11 (Port skel) + 12 (ADR 초안) 동시 발행
- **B** (D2-D3): 2/3/4/5 (Epic 1 후속) + 7/8 (Epic 2 FK 재배선·백필) 병렬
- **C** (D4-D5): 9/10 (Layer softDelete + Controller) + Tier 2 13~15 진입
- **D** (D6): 통합 로컬 검증 (H2 부팅 + 시나리오 3종 + 테스트 통과)
- **E** (D7): 문서(infra/perf skip 명시 + outcome/cost/review 골격) + 0.0.2v 동결 판정

**직렬 (M2 합격선까지)**: Epic 1과 Epic 2가 두 병렬 chain으로 진행 → D5에 두 chain의 마지막(5·10) 머지 → D6 통합 검증 → D7 동결.

---

## 작업 일정 (체크리스트)

재편 D1 = 2026-07-01 (수). 종료 D7 = 2026-07-07 (화). 7일 안에 16 Story.

| 일 | 날짜 | 잡힌 작업 |
| --- | --- | --- |
| D1 (수) | 07-01 | **도메인**: 1 (`learning_facade_concept` 테이블) + 6 (`learning_layer` 테이블) 동시 발행 / **문서**: 12 (ADR023 초안) + 11 (4-Port 인터페이스 파일 생성) |
| D2 (목) | 07-02 | **도메인**: 1 머지 + 2 (Entity + 컬렉션 API) + 6 머지 + 7 (Axis FK 재배선 Flyway 초안) |
| D3 (금) | 07-03 | **도메인**: 2 머지 + 3 (백필 + `updateConcepts()`) + 7 머지 + 8 (default Layer 백필) |
| D4 (토) | 07-04 | **도메인**: 3 머지 + 4 (DTO 스위치) + 5 (검증 + ErrorCode) 시작 + 8 머지 + 9 (softDelete) |
| D5 (일) | 07-05 | **도메인**: 4·5 머지 + 9 머지 + 10 (Layer Controller) / **AI**: 11 머지 + (Want) 13 RoleDetector 시작 |
| D6 (월) | 07-06 | **검증**: 10 머지 + 통합 로컬 검증 (H2 부팅 + Postman 3 시나리오 + `./gradlew test` 통과 확인) + (Want) 14/15 |
| D7 (화) | 07-07 | **마무리**: 12 머지 + `infra.md` (배포 미포함 사유) + `performance.md` (측정 미수행 사유) + `outcome.md`·`cost.md`·`review.md` 골격 + 0.0.2v 동결 판정 |

> **Tier 2 처리 규칙**: D5-D6에 여력 남으면 진입. Tier 1 12개 통과가 우선. Tier 2 미완은 다음 버전으로 이관.

> **선행 조건**: 현재 브랜치 `fix/deck-single-create-path`가 in-flight. D1 시작 전 해당 PR 머지 확인. 미완이면 D1 오전에 완주.

---

## 리스크와 관찰 포인트

| 영역 | 리스크 | 관찰 포인트 |
| --- | --- | --- |
| 두 Flyway 리팩토링 동시 진행 | Epic 1(concepts) + Epic 2(Layer)의 마이그레이션 순서가 꼬이면 백필 실패 | V버전 번호를 미리 할당 (concept: V(N+1) / layer: V(N+2) / axis FK 재배선: V(N+3) / default Layer 백필: V(N+4)). D1 종료 시 순서 확정 후 Flyway history 저장 |
| Axis FK 재배선 (Story 7) | 기존 `learning_axis.facade_id NOT NULL` → `layer_id NOT NULL` 전환 시 백필 누락 → 부팅 실패 | 3단계 분리 강제 (`.claude/rules/conventions.md` §3.8): ADD NULL → 백필 → NOT NULL. D3 종료 시 백필 SQL 사전 실행 확인 |
| default Layer 정책 | facade별 default Layer "Uncategorized"가 UNIQUE `(facade_id, name)` 위반 (facade 신규 생성 시 자동 생성 로직 미비 시 조회 실패) | facade 생성 시 항상 default Layer 1개 자동 발행 + Repository 조회 시 default Layer가 없으면 도메인 예외 |
| Layer 개수 상한 | 사용자 정책 미확정 (5? 10?) | issue-05 재확인 + Story 10에서 상수 결정. 결정 지연 시 임시 상한 10 부여 + 후속 Story에서 최종화 |
| Card 참조 | Epic 4(Card→Axis)를 이번 버전에 넣지 않음 → Card는 여전히 topic 경유. Card가 Layer/Axis 구조 변화 영향 받는지 확인 필요 | Card는 `axis_topic` FK 유지. `axis_topic`은 axis 하부 그대로. Story 7의 Axis FK 재배선이 Card 조회에 영향 없음 검증 |
| 테스트 커버리지 | 도메인 리팩토링 규모 대비 테스트 작성 시간 소요 | Story별 `test-author` 서브에이전트 활용 + Repository Slice 필수 (Flyway 정합) + `.claude/rules/conventions.md` §4.4 명시 검증 항목 준수 |
| 기존 fix 이슈 in-flight | `fix/deck-single-create-path`가 미머지 상태 | 본주 D1 시작 전 머지 확인. 미완이면 D1 오전에 완주 |
| ErrorCode 신규 | 새 ErrorCode 5종(concepts 관련) + Layer 관련이 `GlobalExceptionHandler`에 정합 | ErrorCode enum 등록 → 도메인 예외 throw → Controller Slice 테스트로 응답 형식 검증 (`.claude/rules/conventions.md` §2.3·2.4) |
| ADR023 어휘 반전 부담 | 사용자 정의(Roadmap=헌법)와 기존 SDD 정의(Roadmap=초안)가 반대 → 향후 코드 명명(`RoadmapSuggestionPort` 등)이 어휘 정합에 따라 결정됨 | D1 종료 시점에 ADR 초안 사용자 검토. 반전 확정 시 Story 11의 record 명명 그대로 진행 |
| SP 여유 부족 | Tier 1 12건이 촘촘함. 한 건이라도 슬립 시 D7 마무리 시간 부족 | D3 종료 시점에 6개 이상 머지 진행률 (50%) 확인. 미달 시 D5부터 Tier 2 진입 중단 |

---

## 다음 마일스톤 (M3 / 0.0.3v) 후보 — 07-02 pivot 반영판

07-02 pivot으로 후속 마일스톤 전면 재구성. **첫 릴리스(0.1.0v, ~08-19)를 6주 정도 뒤로 잡고 그 사이 M3~M8을 순차 진행**. 상세 릴리스 스코프·성공 기준은 `release/version/0.0.1v/release.md` 참조.

**M3 / 0.0.3v (07-08 ~ 07-14) — 도메인 착지 마무리 + Roadmap/Selection 노드 스키마 착수**
- LT Epic 1 Story 2~5 (concepts[] 완주 — Entity·API·백필·DTO·ErrorCode)
- LT Epic 2 Story 1~5 (Layer 완주 — 테이블·Aggregate·FK 재배선·백필·softDelete·Controller)
- **이슈 #15 이관 착수** (Roadmap 노드 스키마 신설) — `axis_roadmap_node` 테이블·`AxisRoadmapNode` Aggregate·엔드포인트 세트
- **이슈 #16 이관 착수** (Selection 노드 스키마) — `axis_selection_node` 테이블·자식 Entity·API
- ADR023(용어 재정의) 머지

**M4 / 0.0.4v (07-15 ~ 07-21) — Card 리팩토링 + AI 6-Port 골격**
- **이슈 #21** 이관 (Mode enum 재편 — `MODE_7D/14D/28D/60D`) + 데이터 마이그레이션
- **이슈 #22** 이관 (OnFieldBudget 폐기 + `ArchiveReason` 재편)
- **이슈 #23** 이관 (Card `createdMode` 필드 + M3 하이브리드 도메인 메서드)
- **이슈 #17** 이관 착수 (AI 6-Port 인터페이스 확장 — `ChaptersOutlinePort` + `ChapterSubtreePort` + `SelectionOutlinePort` + `SelectionSubtreePort` 스켈레톤)
- Card 이슈 #7 잔여 (Card → Axis 직접 매핑) 병행

**M5 / 0.0.5v (07-22 ~ 07-28) — Review 리팩토링 + Static Adapter 완주**
- **이슈 #24** 이관 (DailyLearningBatch Aggregate + DailyCardEntry 자식)
- **이슈 #25** 이관 (ReviewSession cross-layer 재편 · deck 스코프 폐기)
- **AS Epic 3 완주** — 4-role catalog JSON + 6-Port Static Adapter 완주
- **이슈 #13** (Deck 완전 폐기) 클러스터 시작

**M6 / 0.0.6v (07-29 ~ 08-04) — AI LLM Adapter + 대시보드 착지**
- **이슈 #19** 이관 (roadmap/selections 개념 명세 프롬프트 embed — `concept-spec.txt` + few-shot)
- **이슈 #26** 이관 (캐시 측정 대시보드 L3 + 관찰 지표 로깅)
- AS Epic 4 (LLM Adapter Vertex AI Gemini) — Spring AI ChatClient + Cascade fallback
- **이슈 #18** 이관 (챕터 노드 재생성 API + hint)

**M7 / 0.0.7v (08-05 ~ 08-11) — 배포 라인 완주 + 통합 테스트**
- **배포 라인 재개** (`product-infra-deploy` · `product-infra-network` · `product-infra-ops` 잔여 Story)
- **관측 baseline** (`product-log` · `product-op` 잔여)
- E2E 통합 테스트 시나리오 (3명 사용자 학습 루틴 커버)
- `product-ai-interactive-roadmap` Epic 1~2 (INIT → LAYERS/AXES/CHAPTERS/SUBTREES_DRAFTED 세션 상태 머신)

**M8 / 0.0.8v (08-12 ~ 08-18) — 첫 릴리스 대비 fix 이슈 소진 + UX 테스트**
- 릴리스 스코프 fix 이슈 잔여 소진
- UX 테스트 3명 사용자 (0.0.2v `ux-test.md` 시나리오 활용)
- Reviewer 세션 병렬 발사
- 릴리스 파이프라인 dry-run
- **→ 첫 릴리스 0.1.0v 발행 준비**

**릴리스 제외 (0.1.0v v1 out of scope)**:
- 이슈 #20 (AI 비용 예산 상한) — 관찰 지표만 v1 포함, cap 자체 v2
- 이슈 #26 L4 (자동 mode 조정) — v2
- 이슈 #26 T3 실시간 push notification — v1은 DB row + 프론트 polling
- Layer 시각화 진행률 API — v2
- 검색 (`product-search`) — v1 이후
- 미디어 업로드 (`product-media`) — v1 이후
- Interactive Roadmap Session Epic 3+ (advanced 세션 상태) — v1은 Epic 1~2까지

---

## Product 상태 전환 신호 (M2 종료 시)

- `in-progress/product-learning-tower.md` — LT Epic 1 Story 1(concept 테이블)만 완료. **Epic 1 Story 2~5 + Epic 2 전체는 M3 이관**. Epic 3(구 이원 축) → **이슈 #15/#16의 노드 스키마로 재구조** 반영 완료
- `in-progress/product-ai-suggestion.md` — 07-02 pivot으로 **4-Port → 6-Port로 재구조 반영 완료** (문서). Epic 1(Port 인터페이스) 및 하위 Story는 M4 이관
- `in-progress/product-ai-interactive-roadmap.md` — 세션 상태 머신 확장(CHAPTERS_DRAFTED, SUBTREES_DRAFTED) 반영 완료 (문서). Story 진입은 M6~M7
- `in-progress/product-card.md` (신설) — Cornell 노트·M3 하이브리드·Fixed interval queue 도메인 정착. Story 진입 M4
- `in-progress/product-review.md` (신설) — Daily batch·Cross-layer·L3 대시보드·T3 조건부 알림 도메인 정착. Story 진입 M5~M6
- `fix/brainstorming/version/0.0.2v/issue-04/05/08` → resolved 표시. issue-15~#20/#21~#26은 M3~M8 이관 산출물 원천으로 활용
- `fix/brainstorming/version/0.0.2v/issue-06/11` → **개정판(#15/#16)으로 뒤집힘** — 이슈 헤더에 SUPERSEDED 표기 (해당 이슈 원본은 archive로 보존)
- `fix/brainstorming/version/0.0.2v/issue-07/12/13/14` → M3~M5 클러스터 이관
- 원안 M2 스코프의 Product 6~11번 (배포·관측·로깅·부하) — 상태 유지 (M7에서 재개)

---

## brainstorming 트리거

본 M2 완료 후 `workflow/task/pes/brainstorming/0.0.3v/` 신설:
- Layer 서버 도메인 도입 → FE 통합 시나리오 (`brainstorming/0.0.3v/fe-integration.md`)
- Card→Axis 직접 매핑 마이그레이션 (issue-07) → Coverage 재계산 후보
- 6-Port AI Static Adapter 4종 완주 → LLM Adapter 도입 시점 (`brainstorming/0.0.3v/llm.md`)
- Deck 폐기·Review 재편 클러스터 (issue-13/14) → 마이그레이션 순서·롤백 시나리오 정리
- **첫 릴리스 대비 MVP 스코프 협의** — release/version/0.0.1v/release.md 참조. UX 테스트 3명 시나리오 확정 (`ux-test.md` 계승)

---

## 참고

- 잔여 Story 인벤토리 출처: `workflow/task/pes/workspectrum/sdd/in-progress/` 신규 Product 3종 (learning-tower / ai-suggestion / ai-interactive-roadmap) + 기존 배포·관측 Product 파일들
- 재편 근거: `workflow/task/fix/brainstorming/version/0.0.2v/issue-04 ~ 14` 11건
- 마일스톤 패키지 의도: `workflow/task/milestones/references/001.md`
- 본 버전의 산출물 5종: `infra.md`(스킵 사유만), `performance.md`(스킵 사유만), `outcome.md`, `cost.md`, `review.md`
- 양식 진화: 본 milestone은 0.0.1v의 milestone.md 양식을 그대로 답습. 양식 변경이 필요하면 별도 ADR로 결정
- 원안 M2(배포·LLM 실제·성능 baseline)의 42 SP 계획은 다음 배포 재개 마일스톤에서 참조용으로 사용 가능
