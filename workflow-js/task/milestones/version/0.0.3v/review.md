# 0.0.3v / Review (회고)

> **본 파일 역할**: 본 버전 종료 시점 반성 · 실측 · 다음 마일스톤 결정 보정.
> 상세 성과: [`outcome.md`](./outcome.md) · 비용: [`cost.md`](./cost.md) · 인프라 스킵: [`infra.md`](./infra.md) · 성능 스킵: [`performance.md`](./performance.md) · AI 평가: [`eval.md`](./eval.md).

---

## 작성 메타

- 작성일: 2026-07-03
- 세션 정책: M2 rush 종료 후 첫 마일스톤 · **Reviewer 5관점 세션 재개** (각 Epic PR마다 정식 발사) · Epic-단위 PR 유지 (사용자 지시 "epic 단위로 pr 진행")
- 본 버전 판정: **동결** (3 Epic PR 모두 develop 머지 · 8/8 종료 신호 성립)

---

## 계획 vs 실제

| 항목 | 계획 (milestone.md 원안) | 실제 | 차이 사유 |
| --- | --- | --- | --- |
| Epic PR 수 (M3) | **6** (PR#1~#6) | **3** (PR#3·#4·#5) | PR#1(concepts[])·PR#2(Layer)·PR#6(ADR023)는 M2에 이미 착지 상태였음이 실측에서 확인 → 사용자 승인 후 실제 미구현분만 진행 |
| Story 목표 (SP) | 43 SP | **~24.5 SP** | 원안의 이미 완료 3 PR (18.5 SP 상당) 재분류 |
| Flyway V 신규 | 계획 V17~V20 | **실제 V20~V22** | V16~V19가 M2에서 이미 소모. 사용자 승인 후 V번호 재할당 |
| 신규 ErrorCode | 4 (Roadmap) + 3 (Selection) = 7 | **11** (Roadmap 4 + Selection 3 + Selection Node 4) | Selection Node reorder mismatch 코드 신설 (도메인 실측에서 필요 발견) |
| 신규 테스트 케이스 | 미지정 (원안엔 "각 PR 해피/엣지/예외") | **~90 신규** | 3 Epic PR 각각 15~30건 |
| Reviewer 5관점 세션 실행 | Epic PR별 강제 | **3회** (PR별 5관점 병렬 발사) | 정책 재개 · M2와 대비 |
| 반영 ADR | 원안 ADR023 M3 대상 | **0건 신설** (ADR023은 M2에 이미 착지) | 밀스톤 문서와 코드 정합 지연 |
| 세션 실 소요 시간 | 7일 계획 (D1~D7) | **약 2일 (Epic PR 각 반 나절 + Reviewer 30~60분 + 조치 30분)** | Reviewer 재개했지만 rush 잔재 유지 |
| 신규 엔드포인트 | 계획 미명시 (SDD 참조) | **18** (Roadmap 5 + Selection 컨테이너 4 + Selection Node 5 + Suggestion 4) | 정합 |

---

## 종료 신호 8/8 성립 (milestone.md 기준)

- [x] 머지 신호 (실제 대상 3 PR 모두 100% 머지)
- [x] concepts[] 신호 (M2 이관 유지 · 회귀 0)
- [x] Layer 신호 (M2 이관 유지 · 회귀 0)
- [x] Roadmap 노드 신호 (`POST /api/v1/axes/{axisId}/roadmap-nodes` 201 + `PUT .../order` 200)
- [x] Selection 노드 신호 (컨테이너 + 자식 · 이슈 #11 정책 계승 검증)
- [x] **AI 첫 응답 신호 (M3 하이라이트)** — chapters-outline 5개 챕터 + chapter-subtree bodyAsciiTree + `providerContext:"static:backend-developer"`
- [x] ADR023 신호 (M2 이관 유지)
- [x] 테스트 신호 (`./gradlew test` BUILD SUCCESSFUL 3회 유지 · 신규 ~90건)

---

## 기대 vs 의외

| 영역 | 기대 | 실제 |
| --- | --- | --- |
| 본주 속도 | 6 PR / 7일 (rush 종료 감안) | **3 PR / 2 세션 (rush 잔재 유지)** — 원안 6 PR 중 3개가 M2에 이미 완주 상태였음 |
| 가장 시간 든 PR | 예상: PR#5 (AI 6-Port · M3 하이라이트) | **실측 부합** — 4 Port + 4 Adapter + Controller + application/dto Result 신설 + Reviewer 조치 6건 → 6 커밋 |
| 가장 빨리 끝난 PR | 예상: PR#3 (Roadmap 노드 단일 도메인) | **실측 부합** — 도메인·Flyway·API 순차 · 3 커밋 |
| Reviewer Critical 발견 | ≤ 2건 (재개 후 첫 세션) | **각 PR 1~3건 Critical** — 총 6건 · 즉시 조치 후 머지 |
| 회귀 테스트 실패 (기존) | 0건 | **2건 발생 후 즉시 수정** — SuggestionCatalogLoaderTest layers 5→6 · StaticSuggestionAdapterConditionalTest provider=llm 컨텍스트 로드 실패 |
| 신규 이슈 발견 (예상 밖) | — | **밀스톤 문서 vs 코드 실상태 정합 지연** (PR#1·#2·#6가 M2에 이미 완주) · **catalog↔milestone 예시 완전 불일치** (Sceptical Reviewer가 M3 하이라이트 데모 실패 감지) · **AppService presentation import 역참조** (아키텍처 위반 · Reviewer가 발견) |
| PATCH null 판별 | 첫 PR에서 지적 예상 → 이후 해소 | **PR#3·#4·#5 모두 반복 지적됨** — v2 JsonNullable 이관으로 통일 |

---

## 반성할 것들 · 개선 여지

### R1. 밀스톤 문서 vs 코드베이스 실상태 정합 지연

- **발생**: M3 착수 시점에 milestone.md는 PR#1(concepts[])·PR#2(Layer)·PR#6(ADR023)를 "M3 대상"으로 명시. 실제로는 M2 PR #196·#197·#199에서 이미 완주 상태.
- **감지 방식**: exploration agent가 코드 실측 후 사용자에게 보고 → 사용자 승인 후 PR#3부터 착수.
- **원인**: M2에서 rush 정책으로 "0.0.3v 급행 스코프"를 실질 흡수했으나 milestone.md 원안이 이 사실을 반영 안 함 (M2 review.md에도 명시된 트레이드오프).
- **함의**: M3 원안 시 43 SP 잡았지만 실제 미구현분은 ~24.5 SP였다. 사용자 시간 감안하면 이 정합이 사전에 됐으면 계획 정확도가 훨씬 높았을 것.
- **재발 방지**: 다음 마일스톤 milestone.md 작성 시 **코드 실측 (exploration)을 계획 확정 전에 강제**. M2 완료 상태의 재실측 없이는 M3 계획 확정하지 않는 프로토콜.

### R2. Flyway V번호 재할당 문서 반영 지연

- **발생**: milestone.md 원안이 V17~V20을 계획. 실제 V16~V19는 M2에 소모됨. M3는 V20~V22로 재할당.
- **감지**: 코드 실측 확인 후 사용자에게 승인 요청 → 재할당 확정.
- **함의**: 문서상 V번호와 실제 코드베이스가 편차. 후속 M4에서 이 편차가 축적되면 SDD 참조 시 혼란.
- **재발 방지**: milestone.md 작성 시 Flyway 현재 최신 V번호를 실측 후 재할당표 사전 확정. M4~ 마일스톤 시작 전 Reviewer가 검증.

### R3. PATCH JSON null 판별 문제 3회 반복

- **발생**: PR#3 (Roadmap Node update), PR#4 (Selection Node update), PR#5 (Suggestion 관련 아니지만 유사 구조) 모두 Reviewer가 "PATCH의 `request.title() != null`이 JSON `null`과 필드 부재를 구분 못함"을 반복 지적.
- **원인**: 첫 PR#3 지적 시 별도 이슈로 이관만 하고 즉시 조치 안 함. PR#4/PR#5에서도 동일 패턴 유지되어 반복.
- **함의**: 동일 아키텍처 결함이 새 코드에 계속 확산.
- **재발 방지**: **Reviewer 반복 지적은 별도 브랜치가 아닌 즉시 조치 규칙** 신설. 또는 첫 지적 시점에 `JsonNullable<T>` 도입 계획을 확정하고 이후 PR에서 새로 도입되는 PATCH 필드는 이 패턴 강제.

### R4. AppService presentation dto 역참조 (아키텍처 위반)

- **발생**: PR#5 `SuggestionAppService`가 `presentation.dto.SuggestionResponse`를 직접 반환.
- **감지**: Architecture Reviewer가 Critical로 지적 → Sceptical Reviewer도 동일 지적.
- **조치**: `application/dto/SuggestionCommand` + `SuggestionResult` record 신설 · Controller가 Request→Command → Result→Response 매핑 담당.
- **원인**: PR#5 초안 작성 시 Controller가 직접 응답 조립하는 패턴을 고려하지 않고 Application Service가 즉시 REST 응답 형식을 반환. 시간 압축 판단.
- **함의**: `docs/PACKAGE.md` §3 계층 방향 위반. 다행히 Reviewer가 감지.
- **재발 방지**: **application/dto Result 레이어를 코드 스캐폴딩 시점부터 강제**. M2에도 유사 문제가 있었을 가능성 있음 (자체 자가점검만 신뢰) → M2/M3 코드 대상 재검증 세션 검토.

### R5. catalog↔milestone 예시 완전 불일치

- **발생**: milestone.md line 251은 `axisName:"하네스 엔지니어링"` 예시. PR#5 초안의 `backend-developer.json`엔 "하네스 엔지니어링" 없음.
- **감지**: Sceptical Reviewer가 "M3 하이라이트 데모 자체가 실패" 지적.
- **조치**: catalog에 "기능의 구현" Layer + "하네스 엔지니어링" Axis + 5 챕터 (1. 기초 · 2. 메모리 · 3. 툴 · 4. 판단루프 · 5. 안전) subtree 추가 → milestone 예시 그대로 실행 성공.
- **원인**: milestone.md 원안이 콘텐츠 예시를 특정 axis로 잡았으나 실제 catalog 구현 시 이를 검증 안 함.
- **함의**: Sceptical Reviewer가 안 잡았으면 M3 종료 신호 데모 자체가 실패했을 것.
- **재발 방지**: **milestone.md 종료 신호 예시는 실제 catalog 실측으로 사전 검증**. AI 응답 예시는 catalog 커밋과 동시에 milestone 예시 갱신.

### R6. Aggregate 3단계 계층 DOMAIN.md 문서화 지연

- **발생**: LearningAxis가 이제 4개 자식 컬렉션 보유 (topics · roadmapNodes · selections). AxisSelection도 nodes 컬렉션 보유.
- **감지**: Architecture Reviewer가 "Aggregate 계층화가 무거워짐 · DOMAIN.md에 4단계 계층 명시 필요" 지적.
- **미조치**: 별도 브랜치 이관.
- **함의**: 새로 들어오는 개발자가 Aggregate 경계를 파악 어려움.
- **재발 방지**: 다음 마일스톤 진입 전 DOMAIN.md §LearningAxis 갱신 (Facade → Layer → Axis → (Topic|RoadmapNode|Selection) → SelectionNode 4단계 계층 명시).

### R7. Test partial update 3 시나리오 반복 미보강

- **발생**: PR#3·PR#4의 Roadmap/Selection Node update 시나리오 테스트가 "title만 갱신 · rationale만 갱신 · body만 갱신" 개별 케이스가 없고 "복합 갱신" 1건만 있음.
- **감지**: Test Reviewer가 두 PR 모두 지적.
- **미조치**: 별도 브랜치 이관. PR#5에서도 같은 패턴이지만 Suggestion은 partial update 자체가 없어 무관.
- **원인**: 시간 압축 판단으로 통합 시나리오만 확인.
- **재발 방지**: 다음 마일스톤에서 backlog 우선 해소 · Test Reviewer가 반복 지적한 항목은 즉시 조치 프로토콜.

---

## 트러블슈팅 요약

| 이벤트 | 소요시간 | 해결 방식 | 학습 |
| --- | --- | --- | --- |
| T#001 QueryDSL Q클래스 dirty로 branch 전환 시 stash 필요 | ~2분 | `git stash push -u -m "WIP: QueryDSL Q classes"` 후 develop 이동 | M2와 동일 · `.gitignore` 미배치가 원인. chore/untrack-querydsl-generated 브랜치가 별도로 진행 중이었음을 확인 |
| T#002 SuggestionCatalogLoaderTest layers 5건 assertion 실패 | ~1분 | catalog에 "기능의 구현" Layer 추가 후 → 테스트 6건으로 갱신 (M3: 기능의 구현 추가 주석 포함) | catalog 확장 시 참조 테스트 동시 확인 필요 |
| T#003 StaticSuggestionAdapterConditionalTest provider=llm 컨텍스트 로드 실패 | ~5분 | SuggestionAppService · SuggestionController에 `@ConditionalOnBean(Port.class)` 추가 → LLM 미구현 상태에서도 컨텍스트 로드 성공 | 새 Port를 필드 주입할 때 provider 별 조건부 활성화도 함께 배선 |
| T#004 Selection variantHint 매칭 "능 아키텍처" 오타 → 데모 실패 | ~3분 | catalog 오타 수정 "능 아키텍처" → "기능 아키텍처" · Adapter 테스트 갱신 | Sceptical Reviewer가 catalog 콘텐츠 품질 지적 (eval.md 첫 실측 근거) |
| T#005 Reviewer 발사 후 5 subagent 병렬 응답 종합 소요 | 각 세션 40~90초 | 자연 소진 · 병렬 발사 유지 | PR 규모가 커질수록 Sceptical Reviewer 스캔 시간 선형 증가 |

**Top 3 막힘**: T#003 (컨텍스트 로드 실패) → T#004 (catalog 오타) → T#002 (layers 5→6). 모두 5분 이내 해결. Critical한 domain·JPA 오류 없음.

**총 트러블 소요시간**: ~15분. 세션 전체 대비 무의미 수준.

---

## 리뷰 지적 요약

Reviewer 5관점 세션 3회 (PR#3·PR#4·PR#5 각각) 실행 결과:

| PR | Critical | Major | Minor | Nit | 즉시 조치 | 별도 이관 |
| --- | --- | --- | --- | --- | --- | --- |
| PR#3 (Roadmap Node) | 1 | 3 | 3 | 2 | 2 (@NotBlank + body MEDIUMTEXT) | PATCH null · Update partial 테스트 · Aggregate 계층 문서 |
| PR#4 (Selection Node) | 3 | 4 | 2 | 3 | 3 (axisId 정합 검증 · body 상한 · addSelection trim) | 3중 loop 성능 · PATCH null 반복 · updated_at 컬럼 · Update partial 테스트 |
| PR#5 (AI 6-Port) | 3 | 5 | 3 | 4 | 7 (application/dto 신설 · catalog 하네스 · subList 불변 · @ResponseStatus · trim · 오타 · @ConditionalOnBean) | Rate Limit · Layer/Axis endpoint · prompt include 처리 · 인증 정책 명시 |
| **총계** | **7 Critical** | **12 Major** | **8 Minor** | **9 Nit** | **12 즉시** | **~10 별도 이관** |

**5관점별 발견 특성**:
- **Domain Reviewer**: 캡슐화 (Collections.unmodifiableList) · 정규화 (trim/blank→null) 대체로 통과
- **Architecture Reviewer**: **PR#5의 AppService presentation import 역참조**를 Critical로 감지 (M3 최대 발견)
- **API Reviewer**: @NotBlank/@ResponseStatus 강도 지적 · 이중 방어 원칙
- **Test Reviewer**: partial update 시나리오 반복 지적
- **Sceptical Reviewer**: **catalog↔milestone 예시 불일치 · variantHint 오타** 등 콘텐츠 품질 감지 (eval.md 근거)

**총평**: rush 스킵 없이 재개한 첫 마일스톤에서 M2에 은닉되어 있었을 가능성이 큰 아키텍처 위반 · 콘텐츠 품질 결함을 다수 감지. **Reviewer 재개의 가치가 실측으로 확인됨**.

---

## 백엔드 담당관 시각 회고

### 본 버전이 잘 굴러갔는가

**Yes** (M2 대비 품질 상승).
- 3 Epic PR 각 Reviewer 5관점 정식 발사 → M2에서 놓쳤을 가능성이 있던 아키텍처 위반 (application → presentation 역참조) 감지 · 조치.
- M3 하이라이트 "AI 첫 응답" 로컬 실현. milestone.md 예시 그대로 실행 성공.
- Reviewer 조치를 개별 커밋으로 남겨 감사·복원 가능.
- 트러블 총 15분 · rush 잔재로 소요 압축 유지.

**조건**: Reviewer 재개했지만 **rush 잔재로 5관점 각각의 세부 후속 조치 (예: partial update 테스트 3건, DOMAIN.md 갱신)는 별도 브랜치 이관**. 완전한 정식 진행은 다음 마일스톤부터.

### Trap을 피했는가

**대체로 예방**:
- Flyway V번호 충돌 — 사전 코드 실측으로 V20~V22 확정. 실제 충돌 없음.
- 도메인 캡슐화 위반 — Reviewer가 사전 감지 (getRoadmapNodes/getSelections unmodifiableList 강제).
- catalog↔milestone 정합 실패 — Sceptical Reviewer 감지 후 즉시 조치.
- provider=llm 시 컨텍스트 로드 실패 — 기존 회귀 테스트가 감지 → @ConditionalOnBean 조치.

**살짝 밟은 트랩**:
- catalog 오타 "능 아키텍처" (T#004) — Sceptical Reviewer 없었으면 데모 실패했을 것.
- AppService presentation import (R4) — Architecture Reviewer가 아니었으면 M4에도 확산됐을 것.
- SuggestionCatalogLoaderTest 회귀 (T#002) — catalog 확장 시 참조 테스트 확인 습관 부재.

**실질적 실패 없음**: 3 PR 모두 첫 리뷰에서 발견된 결함을 조치 후 머지 성공.

### 속도 · 완성도 · 품질 균형

- **속도**: 상 (3 Epic PR · 2 세션). M2 rush (16 Story · 1 세션) 대비 하락했지만 Reviewer 재개 감안 시 정상.
- **완성도**: 상상 (도메인 + Flyway + API + 테스트 + ErrorCode 모두 정합 · Reviewer 조치까지 반영).
- **품질**: 상 (5관점 Reviewer 3회 발사 · Critical 7건 즉시 조치 · 별도 이관 목록 명확).

**M2 대비 개선**:
- ✅ Reviewer 재개 → M2에서 은닉됐을 가능성이 큰 위반 감지
- ✅ 각 PR 조치 별도 커밋으로 감사 가능
- ✅ 아키텍처 위반 (application → presentation) 감지 · 재발 방지 프로토콜 명확화

**M2 대비 후퇴**:
- ❌ 밀스톤 문서 vs 코드 정합 지연 (M2에서 rush로 미갱신된 잔재 · R1)
- ❌ Reviewer 반복 지적 (PATCH null 3회) 즉시 조치 안 하고 이관 (R3)

### 다음 버전 진입 전 개선할 셀프 프로세스

1. **milestone.md 계획 확정 전 코드 실측 강제** — M4 착수 전 fix issue 상태 + Flyway V번호 + 완료 Story 실측 (R1·R2 재발 방지)
2. **Reviewer 반복 지적 즉시 조치 프로토콜** — 첫 지적 시점에 즉시 조치 또는 명시적 backlog 등록 (R3 재발 방지)
3. **application/dto Result 스캐폴딩 강제** — Application Service 신설 시 즉시 application/dto Command·Result 신설 (R4 재발 방지)
4. **milestone.md 예시는 catalog와 동시 갱신** — AI 응답 예시가 걸린 milestone은 catalog 커밋 시점에 함께 갱신 (R5 재발 방지)
5. **DOMAIN.md Aggregate 계층 갱신** — M4 진입 전 4단계 계층 문서화 (R6 조치)
6. **Test partial update 시나리오 backlog 즉시 해소** — PR#3·PR#4 지적된 항목 다음 브랜치에서 즉시 (R7 조치)

---

## 다음 마일스톤 (M4 / 0.0.4v) 결정 보정

### 다음 주 잡힐 양 (실측 반영)

M3 실측 (3 Epic PR · 2 세션 · Reviewer 재개) → M4는 유사 규모(3~4 Epic PR · 3 세션) 가능. Reviewer 5관점 세션 소요 반영 시 M3 대비 -10% 조정 검토.

### M4 우선순위 (M3 결과 반영)

- [ ] **Card 리팩토링 진입 (이슈 #21~#23)** — Mode enum 재편 (MODE_7D/14D/28D/60D) · OnFieldBudget 폐기 → ArchiveReason · Card `createdMode` 필드 신설 · M3 하이브리드 도메인 메서드
- [ ] **AI Static Adapter role catalog 3개 신설** — planner.json · designer.json · problem-solver.json · concept-spec 카탈로그 6+5 정합 큐레이션
- [ ] **Card→Axis 직접 매핑 이관 검토 (이슈 #07)** — `card.axis_id NOT NULL` 3단계 마이그레이션 · Coverage 재계산 axis 스코프
- [ ] **DOMAIN.md Aggregate 4단계 계층 갱신** (R6 조치)
- [ ] **Roadmap/Selection PATCH partial 테스트 3 시나리오 보강** (R7 조치)
- [ ] **PATCH JSON null 판별 통일 (JsonNullable 도입 또는 대안)** (R3 조치)
- [ ] **fix/brainstorming/0.0.3v 신설** — Reviewer 반복 지적 8건 + backlog 이관 항목 정리

### brainstorming 0.0.4v 신설 트리거

- `fix issue-15 (Roadmap 노드) · issue-16 (Selection 노드) · issue-17 (AI 2-step) · issue-19 (개념 명세)` → **partial resolved** 표기 (M6 LLM Adapter 남음)
- `fix issue-06 · 11` → **SUPERSEDED** 표기 (M3에서 뒤집힘)
- **신규 검토**: Card Mode 재편 UX (사용자 mode 다운 시 카드 archive 사전 안내) · Static Adapter role 3종 catalog 튜닝 · Static → LLM Adapter 전환 시점

---

## 본 버전 동결 선언

**8/8 종료 신호 성립** → 0.0.3v 동결. M3 하이라이트 "AI 첫 응답" 로컬 실현.

- [x] 3 Epic PR 모두 develop 머지 (#201·#202·#203)
- [x] 산출물 6종 신설 (outcome/cost/review/infra/performance/eval)
- [x] Reviewer 5관점 세션 3회 실행 · Critical 7건 즉시 조치
- [x] 코드베이스 실측 반영 (Flyway V20~V22 · ErrorCode 11종 · 테스트 ~90 신규)
- [ ] `product-learning-tower.md` Epic 3 재정의판 completed 표기 (다음 세션)
- [ ] `product-ai-suggestion.md` Epic 1~7 완주 항목 completed 표기 (다음 세션)
- [ ] fix issue 상태 파일 갱신 (15/16/17/19 → partial · 06/11 → SUPERSEDED)
- [ ] `workflow/task/pes/brainstorming/0.0.4v/` 신설 검토 (M4 준비)

*작성일: 2026-07-03 | 동결 판정: 8/8 신호 성립 · M2 rush 종료 후 Reviewer 재개 첫 마일스톤 성공 | 반성 항목 R1~R7 · 트러블 5건 · 개선 6건 명시*
