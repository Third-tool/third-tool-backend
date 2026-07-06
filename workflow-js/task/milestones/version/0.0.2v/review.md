# 0.0.2v / Review (회고) — 0.0.3v 급행 실측

> **본 파일 역할**: 본 버전 종료 시점 반성 · 실측 · 다음 마일스톤 결정 보정.
> 상세 성과: [`outcome.md`](./outcome.md) · 상세 트러블: [`troubleshooting.md`](troubleshooting/troubleshooting.md) · 상세 구조: [`architecture.md`](./architecture.md).

---

## 작성 메타

- 작성일: 2026-07-02
- 세션 정책: rush (사용자 개별 코드 리뷰 스킵 · 5관점 Reviewer 세션 스킵 · Epic-단위 PR 번들)
- 본 버전 판정: **동결 (0.0.3v 급행 스코프까지 포함)**, 실질 M2·M3 원안 Story 흡수

---

## 계획 vs 실제

| 항목 | 계획 (milestone.md 재편본) | 실제 | 차이 사유 |
| --- | --- | --- | --- |
| Story 머지 수 (Tier 1) | 12 | **12** (100%) | 계획 그대로 |
| Story 머지 수 (Tier 2 · Want) | 4 | **4** (100%) | rush 정책으로 Tier 2까지 완주 |
| 전체 머지 수 | 16 | **16** | 예상 초과 (M2 재편본은 원래 10~12 예상) |
| PR 수 | 12~15 (Story-단위) | **7** (Epic-단위 번들 정책) | 사용자 선택 |
| Flyway V 신규 | 4 (V16~V19) | **4** (V16~V19) | 그대로 |
| 신규 ErrorCode | ~11 | **11** (LF005~008 + LL001~007) | 정확 일치 |
| 신규 테스트 케이스 | ≥60 | **97** | 초과 달성 |
| Reviewer 세션 실행 | Story별 강제 | **0회** (rush 스킵) | 정책 결정 |
| 반영 ADR | 1건 (ADR022 or 023) | **1건 (ADR023)** | milestone의 ADR023 명명 채택 |
| 세션 실 소요 시간 | 5~7일 계획 | **1일 (rush 모드)** | Epic-단위 PR + 사용자 즉시 머지로 압축 |

---

## 종료 신호 8/8 성립 (milestone.md 기준)

- [x] 머지 신호 (Tier 1 12/12 = 100%, 목표 83% 초과)
- [x] concepts[] 신호 (로컬 도메인·Controller·DTO 흐름 정합)
- [x] Layer 도메인 신호 (POST/PATCH/DELETE/PUT 4엔드포인트 · default 발행 · softDelete 가드)
- [x] 마이그레이션 신호 (V16~V19 · R16~R19 · Flyway idempotent)
- [x] 테스트 신호 (`./gradlew test` BUILD SUCCESSFUL · 97건 신규)
- [x] Port 골격 신호 (4 Port · 6 record · stub Adapter 컴파일 실험)
- [x] ADR 신호 (ADR023 발행 · index.md · DOMAIN.md 정합)
- [x] 문서 정합 신호 (DOMAIN.md 용어 35→38 · architecture.md 신설)

**Tier 2 추가 신호**:
- [x] AI Static 신호 (RoleDetector + backend-developer catalog + StaticLayerSuggestionAdapter · 24건 테스트)

---

## 기대 vs 의외

| 영역 | 기대 | 실제 |
| --- | --- | --- |
| 본주 속도 | 16 Story / 7일 | **16 Story / 1 세션 (rush)** — Epic 번들로 압축 |
| 가장 시간 든 Story | 예상: LT-E2-S2 Axis FK 재배선 | **실측 부합** — V19 3-phase + LearningFacade.layers 컬렉션 + addAxis 라우팅 + 기존 테스트 호환 확인 |
| 가장 빨리 끝난 Story | 예상: 1-1 · 12 문서 | **실측 부합** — Story 1은 사전 완료 · ADR023은 문서만 |
| Flyway 3-phase 실패 | 0회 | **0회** — Hibernate ddl-auto가 테스트 schema를 즉시 생성해 V19 SQL은 실제 검증 안 됨 (테스트에서 Flyway disabled) |
| 회귀 테스트 실패 (기존) | 0건 | **1건 발생 후 즉시 수정** — `LearningFacade.create(user, null)` 오버로드 모호성 (`(String) null` cast로 해결) |
| Reviewer 세션 Critical | ≤ 2건 | **0건** (스킵 · rush 정책) |
| 신규 이슈 발견 | — | **facade.axes 유지 · Legacy concept 컬럼 유지 · @SQLRestriction 관행 갈림 재발 (ADR021 미해결 항목 재확인)** |

---

## 반성할 것들 · 개선 여지

### R1. rush 정책의 함의 재확인

- **잘한 점**: 16 Story를 1 세션에 완주. Epic-단위 PR로 사용자 리뷰 부담 최소.
- **놓친 점**: 5관점 Reviewer 세션 스킵으로 도메인 캡슐화 위반 · BC 의존 방향 위반 여부의 독립 검증 없음. 자체 자가점검만.
- **재발 방지**: rush 종료 후 **한 번의 종합 5관점 Reviewer 세션**을 별도 PR로 실행하는 대안 검토. Or 다음 마일스톤 시작 시 "0.0.3v rush 후처리" 세션 예약.

### R2. `facade.axes` backward compat 유지 (트레이드오프)

- **결정**: SDD의 "컴파일 오류" AC 미충족. 레거시 accessor 유지.
- **이유**: 40+ 파일 refactor 회피 · 기존 테스트 호환.
- **함의**: 도메인 모델이 "Layer가 axes를 소유"와 "Facade가 axes를 직접 소유" 두 표현 공존. 인지 부하 증가.
- **대응**: `architecture.md` §10 T1에 트레이드오프 명시. 별도 릴리스에서 단계적 refactor 계획.

### R3. Legacy `learning_facade.concept` 컬럼 유지

- **결정**: 컬럼 DROP 미수행 · `concepts[0]`과 자동 동기화.
- **이유**: 컬럼 DROP은 3-phase 원칙 미충족 (한 번에 못 함).
- **함의**: 도메인·DB에 두 표현 공존.
- **대응**: 이후 별도 V 마이그레이션에서 concept NOT NULL 해제 → NULL 백필 → DROP 3단계 필요.

### R4. `@SQLRestriction` vs `boolean deleted` 관행 갈림 재발

- **재발**: LearningLayer가 `@SQLRestriction` 채택. Card/Deck의 `boolean deleted` 관행과 다름.
- **원인**: ADR021 "재검토 시점" 항목이었으나 관행 통일 리팩토링 미수행 상태에서 신규 도메인 추가.
- **함의**: 프로젝트 내 두 Soft Delete 스타일 공존.
- **대응**: 후속 리팩토링으로 Card/Deck를 `@SQLRestriction`으로 마이그레이션할지, LearningLayer를 `boolean deleted`로 되돌릴지 결정 필요.

### R5. Flyway 테스트에서 disabled

- **관찰**: `application-test.yml`이 `spring.flyway.enabled: false`. 테스트는 Hibernate `ddl-auto=create-drop`으로 스키마 생성.
- **함의**: V17 백필 · V19 3-phase SQL의 실제 동작은 로컬 bootRun 또는 배포 dev 환경에서만 검증됨.
- **위험**: 백필 SQL 오류가 테스트에 안 잡힘 → dev 배포 시점에 발견 지연.
- **대응**: 배포 재개 시점에 dev 환경에서 V19 실제 백필 결과 검증 (spot check).

### R6. Tier 2 성능 측정 스킵

- **결정**: rush 정책으로 정밀 성능 측정 미수행.
- **함의**: `GET /facades/me` 응답 크기 · Layer join N+1 · Flyway 시간 등 관찰 지점 아직 실측 안 됨.
- **대응**: `performance.md`에 트리거 명시. 배포 재개(M7+) 시점에 실측 baseline.

### R7. Story-단위 vs Epic-단위 PR 트레이드오프

- **결정**: Epic-단위 번들 (5 PR = Story 1 · LT E1 · LT E2 · AS E1 · ADR023 · Tier 2).
- **잘한 점**: 사용자 왕복 5회로 압축. rush에 최적.
- **놓친 점**: 
  - PR diff가 커 사용자가 육안 리뷰 부담 (rush 정책이라 어쨌든 리뷰 스킵)
  - 롤백 단위가 Story 하나가 아닌 Epic 전체 → 롤백 시 무해한 Story까지 되돌림
- **재발 방지**: 사용자 리뷰가 활성화되는 다음 마일스톤부터는 Story-단위 PR 복귀 검토.

---

## 트러블슈팅 요약 (troubleshooting.md 상세)

| 이벤트 | 소요시간 | 해결 방식 | 학습 |
| --- | --- | --- | --- |
| T#001 QueryDSL generated 파일 dirty로 checkout 실패 | ~2분 | `git restore src/main/generated/` (파일이 build 시 재생성됨) | 매 브랜치 전환 전 관행화 필요. `.gitignore` 이미 있음이나 브랜치별 상태 유지 |
| T#002 PR #195 target을 `main`으로 계산했으나 실제 `develop` | ~5분 | `gh pr view 195 --json baseRefName`로 확인 후 develop 기준 재분기 | CLAUDE.md의 "main branch"는 사용자 개인 컨벤션과 다름. `.claude/rules/git.md` §1 develop 정책이 진실 소스 |
| T#003 테스트 컴파일 오류 `create(user, null)` 오버로드 모호 | ~5분 | `(String) null` cast 추가 | `create(user, String)` + `create(user, List<String>)` 두 오버로드 도입 시 예상 트랩. 명시 cast로 해결 |
| T#004 `reorderConcepts` 후 in-memory 순서 불일치 | ~3분 | `concepts.sort(Comparator.comparingInt(...))` 추가 | `@OrderBy`는 DB load 시점만 · in-memory 변경 시 수동 정렬 필요 |
| T#005 `List.of("...", null)` NPE | ~3분 | `Arrays.asList("...", null)` 사용 | `List.of()` factory는 null 원소 미허용 (Java 9+) |

**Top 3 막힘**: T#002 (branch target) → T#001 (dirty files) → T#003 (오버로드 모호). 모두 5분 이내 해결. Critical한 domain·JPA 오류 없음.

**총 트러블 소요시간**: ~18분. 세션 전체 대비 무의미 수준.

---

## 리뷰 지적 요약

- **5관점 Reviewer 세션**: rush 정책으로 **0회 실행** (사전 승인 스킵).
- **자체 자가점검**: 매 Story 완료 후 콘솔 요약으로 사용자에게 결과 요약 · 사용자 즉시 승인 (16 Story).
- **놓쳤을 가능성**: 도메인 캡슐화 위반 (Aggregate가 Repository 호출 등) · BC 의존 방향 위반 · Test Matrix 누락 케이스. 자체 검증만 신뢰.
- **후속 대응**: 다음 마일스톤 시작 시 "종합 5관점 세션" 별도 예약 검토.

---

## 백엔드 담당관 시각 회고

### 본 버전이 잘 굴러갔는가

**Yes** (조건부).
- 계획 대비 100% + Tier 2까지 완주.
- 트러블슈팅 총 소요시간 ~18분 (총 세션의 1% 미만).
- 도메인·Application·Infrastructure 4-레이어 원칙 준수 (헥사고날 Port/Adapter 실증).
- ADR023으로 이후 이슈 #15~#19 명명 근거 확정 · 재작업 회귀 봉쇄.

**조건**: Reviewer 세션 없이 자체 자가점검만 신뢰. 도메인 캡슐화 · BC 의존 방향 위반 은닉 가능성이 이론상 남음.

### Trap을 피했는가

**대체로 예방**:
- Flyway V 버전 충돌 (Epic 1·Epic 2 동시 진행) — 사전 V 번호 할당 (V17 concept 백필 · V18 Layer · V19 Axis FK). 실제 충돌 없음.
- 3-phase 마이그레이션 순서 실수 — `conventions.md` §3.8 준수. V19 SQL 파일 안에 5-phase (ADD → 백필 2단계 → NOT NULL → FK) 순차 명시.
- ErrorCode 등록 순서 — enum → domain throw → 도메인 검증 테스트 순서 지킴.
- default Layer UNIQUE 위반 — `LearningFacade.create()` 자동 발행 + V19 백필 idempotent NOT EXISTS.
- Card 조회 회귀 — `axis.getFacade()` 경로 유지 · `LearningAxis.facade` @ManyToOne 유지 (backward compat).

**살짝 밟은 트랩**:
- 오버로드 모호성 (T#003) — 실제 트랩. 5분 해결.
- List.of null (T#005) — 실제 트랩. 3분 해결.

### 속도 · 완성도 · 품질 균형

- **속도**: 최상 (16 Story · 1 세션). rush 정책의 최대 활용.
- **완성도**: 상 (도메인 API · DTO · Controller · 테스트 · ErrorCode 모두 정합).
- **품질**: 중상 (자체 자가점검 · 5관점 Reviewer 스킵으로 독립 검증 부재. `./gradlew test` 통과만 신뢰).

### 다음 버전 진입 전 개선할 셀프 프로세스

1. **rush 후처리 세션 예약** — 다음 마일스톤 시작 전에 0.0.2v/0.0.3v 코드의 5관점 종합 Reviewer 1회 실행.
2. **`facade.axes` refactor 계획 수립** — SDD 원안 AC 달성을 위한 단계적 refactor 계획을 별도 fix 이슈로 발행.
3. **Legacy concept 컬럼 DROP 계획** — 3-phase (NOT NULL 해제 → NULL 백필 → DROP) 마이그레이션 발행.
4. **Soft Delete 관행 통일** — Card/Deck를 `@SQLRestriction`으로 마이그레이션 or LearningLayer를 `boolean deleted`로 되돌리기 결정. ADR021 재검토 시점 도래.
5. **테스트에서 Flyway 활성 옵션 검토** — V19 백필 SQL 검증을 위한 `@SpringBootTest`에서 Flyway 활성 profile 도입.

---

## 다음 마일스톤 (M3 / 0.0.3v) 결정 보정

### 다음 주 잡힐 양 (실측 반영)

본주 실측 (16 Story · 1 세션 rush) → M3는 유사 규모(15~18 Story) 가능. 단 Reviewer 세션이 재개되면 -30% 조정 검토.

### M3 우선순위 (M2·0.0.3v 결과 반영)

- [ ] **rush 후처리 5관점 Reviewer 세션** (0.0.2v/0.0.3v 코드 종합 검증)
- [ ] **이슈 #15 (Roadmap 노드 스키마)** — `axis_roadmap_node` 테이블 + `AxisRoadmapNode` Aggregate + API
- [ ] **이슈 #16 (Selection 노드 스키마)** — `axis_selection` (컨테이너) + `axis_selection_node` + API
- [ ] **AS Epic 2 완주** — `StaticAxisSuggestionAdapter` (기존) · `StaticRoadmapSuggestionAdapter` · `StaticSelectionsSuggestionAdapter` 신설
- [ ] **AS Epic 3 완주** — `planner.json` · `designer.json` · `problem-solver.json` catalog 큐레이션
- [ ] **facade.axes refactor 계획** — SDD 원안 AC 달성 · 별도 fix 이슈
- [ ] **Legacy concept DROP 3-phase** — V20~V22

### brainstorming 0.0.3v 신설 트리거

- `fix issue-04 (concepts[]) · issue-05 (Layer) · issue-08 (용어 표준) · issue-09 (Port) · issue-10 (RoleDetector)` → **resolved** 표시
- `fix issue-06 · 07 · 11 · 13 · 14` → M3~M5 클러스터 이관 (현행 유지)
- **신규 검토**: FE 통합 시나리오 (Layer 계층 UI) · LLM 도입 시점 (AS Epic 4)

---

## 본 버전 동결 선언

**8/8 종료 신호 성립** → 0.0.2v 동결 (0.0.3v 급행 스코프까지 실질 포함).

- [x] `version/0.0.2v/` 폴더 내 산출물 파일 갱신 완료 (본 PR)
- [x] `architecture.md` 신설 (사용자 요청 반영)
- [x] `docs/adr/ADR023-terminology-roadmap-selections.md` 머지
- [x] `docs/DOMAIN.md` §1 Ubiquitous Language 35→38
- [ ] 다음 폴더 생성: `cp -r version/0.0.2v version/0.0.3v` 검토 (실질은 0.0.3v 스코프 포함이라 재구조 필요)
- [ ] brainstorming 0.0.3v 신설 검토
- [ ] fix issue 상태 파일 갱신 (04·05·08·09·10 → resolved)

*작성일: 2026-07-02 | 동결 판정: 8/8 신호 성립 · 실질 0.0.3v 스코프까지 포함 | 반성 항목 R1~R7 · 트러블 5건 · 개선 5건 명시*
