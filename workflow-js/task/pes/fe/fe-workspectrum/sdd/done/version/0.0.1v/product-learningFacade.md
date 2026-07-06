# [Product] LearningFacade FE — 축·주제·자료 트리 UI

## Product Vision

> **유저가 "내가 무엇을 하고 싶은 사람인가"를 concept으로 정의한 뒤, 그 아래 축(Axis)·주제(Topic)·자료(Material) 트리를 시각적으로 조작·회고할 수 있게 한다.**
> 백엔드 4-level 계층(Facade → Axis → Topic → Material)과 커버리지 상태(NO_MATERIAL/PARTIAL/COVERED)를 FE에서 직관적 색상·뱃지·드래그 UX로 살려낸다.

## 설계 결정 (Design Decisions)

- **useLearningFacade() hook = LearningFacade 진입점**
  - `features/auth/hooks/useLearningFacade.ts` 이미 존재 → 모든 축/주제/자료 UI가 이 hook을 상위에서 소비
  - 소유권/존재 여부 확인은 hook 내부에서. 페이지 컴포넌트는 `data`/`isPending`/`error`만 다룸
- **커버리지 상태 색상 매핑 = Tailwind semantic 클래스**
  - `NO_MATERIAL` → gray-300 (미개시), `PARTIAL` → amber-400 (진행), `COVERED` → green-500 (충분)
  - 매핑 상수는 `features/map/constants.ts` 등 공유 상수 파일에 두고 dark mode 대응
- **권장 한도 초과 = 저장 허용 + 안내 뱃지 표시**
  - 백엔드가 `isAxisCountExceedsRecommended`/`isTopicCountExceedsRecommended` 응답 필드로 boolean 제공
  - FE는 6번째 축 이후 뱃지 "권장 한도 초과" + tooltip "권장 5개 이내, 저장은 허용됩니다"
  - 저장 차단 X — 백엔드 정책과 정합
- **displayOrder reorder = drag-and-drop, id 집합 동일성 보장**
  - `dnd-kit` 등 미리 도입되지 않았다면 도입 결정 (FE-ADR 후보)
  - reorder 완료 시 백엔드 `PUT /api/v1/learning-facade/axes/reorder`로 orderedIds 전송
  - 백엔드는 id 집합 불일치 시 예외 — FE는 화면에 보이는 현재 목록 id 그대로 전송하므로 정상 케이스는 보장. 예외 발생 시 캐시 재조회 + 재시도 UX
- **다건 입력(예: AI 제안 다수 주제 추가) UI = 원자 트랜잭션 인지**
  - 한 건 실패 시 전체 롤백 → FE는 "N개 중 1개 검증 실패 — 모두 저장 취소됨" 안내 + 실패 항목 하이라이트
  - 부분 성공 UX는 만들지 않음 (백엔드 정책)
- **커버리지 재계산 트리거 UX = TopicMaterial 연결/해제 직후 응답에 반영**
  - 백엔드가 동일 트랜잭션 안에서 `coverageStatus` 갱신 후 응답 → FE는 응답 데이터로 직접 색상 갱신
  - `invalidateQueries(['learning-facade'])`로 전체 트리 캐시 재조회
- **Focus Top N(권장 상위 3) = 뱃지 "지금 집중"**
  - 백엔드 `isFocused: true` 응답 플래그 그대로 렌더

## 대안 검토 (Alternatives Considered)

### 트리 렌더 라이브러리

**Option A — 자체 재귀 컴포넌트**
- 장점: 의존 없음, 커스텀 자유
- 거부 이유: 드래그·가상화·펼침 상태 관리를 직접 구현하는 비용

**Option B (선택) — dnd-kit 기반 재귀 자체 컴포넌트**
- 비용: 라이브러리 도입 (FE-ADR 필요)
- 보상: 드래그 접근성(키보드 지원)·성능 검증됨 + reorder UX 표준

**Option C — react-arborist 등 트리 전용 라이브러리**
- 거부 이유: 4-level 정도의 얕은 계층에서는 과함. 스타일 커스터마이즈 부담

### 커버리지 색상 정책

**Option A — 3단계 이산 색상 (선택)**
- 보상: 백엔드 `CoverageStatus` enum과 1:1 대응, 인지 명확

**Option B — 자료 개수 비율 그래디언트**
- 거부 이유: 백엔드가 이산 상태를 진실로 두므로 FE가 별도 계산은 진실 흐림. 정보 밀도 초과

### 권장 한도 초과 처리

**Option A — 저장 차단**
- 거부 이유: 백엔드 정책 위반 (안내만, 저장 허용)

**Option B (선택) — 뱃지 안내 + 저장 허용**
- 보상: 백엔드 정합, 유저 자유

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 / 라우트 배치

```
[React Router]
    │
    ├─ /onboarding   → OnboardingPage (concept 초기 입력)
    ├─ /home         → HomePage (축 요약 카드)
    ├─ /map          → MapPage (전체 4-level 트리 + reorder)
    │
    ▼
┌──────────────────────────────────────────────────────────────────┐
│  features/map/                                                    │
│  ├─ MapPage.tsx                (트리 컨테이너 + drag scope)        │
│  ├─ components/                                                    │
│  │    ├─ ConceptHeader.tsx     (Layer 1 concept 표시/편집)        │
│  │    ├─ AxisList.tsx          (Axis reorder)                     │
│  │    ├─ AxisNode.tsx          (Topic list + coverage 색상)       │
│  │    ├─ TopicNode.tsx         (Material chip + focus 뱃지)       │
│  │    ├─ CoverageBadge.tsx     (NO_MATERIAL/PARTIAL/COVERED)      │
│  │    ├─ RecommendedLimitBadge.tsx                                │
│  │    └─ MaterialLinkDialog.tsx (자료 연결 dialog)                │
│  └─ hooks/                                                         │
│       ├─ useAxisReorder()      (dnd-kit sensor + mutation)        │
│       └─ useTopicMaterial()    (연결/해제 mutation)                │
│  features/auth/hooks/                                             │
│  └─ useLearningFacade()  (이미 존재 — 확장 포인트)                 │
│  features/onboarding/                                             │
│  └─ OnboardingPage.tsx  (concept 초기 입력 + 첫 축 3개 자동/수동)  │
└──────────────────────────────────────────────────────────────────┘
    │
    ▼
lib/api/schemas/facade.ts / material.ts
lib/api/endpoints/learningFacade.ts / material.ts
    │
    ▼
[Backend /api/v1/learning-facade/**, /api/v1/materials/**]
```

### 핵심 플로우

**1. Axis 추가 (자동 displayOrder)**

```
사용자가 "+ 축 추가" 클릭 → 이름 입력 dialog
   │
   ▼
useMutation createAxis({ name })
   ├─ Zod: LearningAxisCreateSchema (name 1~100자, trim)
   ├─ POST /api/v1/learning-facade/axes
   │      응답: { axisId, isAxisCountExceedsRecommended }
   ├─ invalidateQueries(['learning-facade'])
   └─ 6번째 이상이면 뱃지 "권장 한도 초과" 표시
```

**2. Topic 다건 추가 (전체 롤백)**

```
사용자가 AI 제안 3개를 채택 → "일괄 추가"
   │
   ▼
useMutation addTopics(axisId, [{ name }, { name }, { name }])
   ├─ POST /api/v1/learning-facade/axes/{axisId}/topics/batch
   ├─ 성공: invalidate + toast "3개 저장됨"
   └─ 400 (검증 실패): "저장 취소됨 — 실패 항목: {name}" + 로컬 폼 유지
```

**3. Axis 드래그 reorder**

```
사용자가 축 드래그 완료 (dnd-kit onDragEnd)
   │
   ▼
useAxisReorder.mutate(orderedIds: Long[])
   ├─ 낙관 update: 캐시 순서 즉시 갱신
   ├─ PUT /api/v1/learning-facade/axes/reorder
   ├─ 성공: invalidate + 도메인 displayOrder 재부여 결과 반영
   └─ 실패(예: id 집합 불일치): 낙관 롤백 + refetch 강제 + 토스트
```

**4. Material 연결 → 커버리지 자동 갱신**

```
사용자가 Topic에서 "자료 연결" → MaterialLinkDialog에서 선택
   │
   ▼
useMutation linkMaterial(topicId, materialId)
   ├─ POST /api/v1/topics/{topicId}/materials
   │      백엔드가 동일 트랜잭션에서 CoverageRecalculator 실행
   │      응답: 갱신된 topic + coverageStatus
   ├─ invalidate(['learning-facade'])
   └─ CoverageBadge 색상 즉시 갱신
```

### 외부 의존

- **백엔드 `/api/v1/learning-facade/**`, `/api/v1/materials/**`** — 진실 소스
- **`workflows/backend-boundary/api-learning-facade.md`** — 계약 상세
- **Deck BC 동기 이벤트 결과** — Material 등록 시 Deck 자동 생성됨. FE는 응답의 `deckCreated / deckId / deckName` 렌더

## 실패 모드 / 운영 관측

### 실패 시나리오와 UX 응답

| 시나리오 | ApiError code | HTTP | UX 동작 |
| --- | --- | --- | --- |
| Facade 미존재 (신규 유저 concept 미입력) | `LEARNING_FACADE_NOT_FOUND` | 404 | `/onboarding` redirect |
| concept 미설정 상태에서 축 추가 | `LF_CONCEPT_REQUIRED` | 400 | Onboarding 안내 카드 + `/onboarding` 진입 CTA |
| 축 이름 중복 | `LEARNING_AXIS_DUPLICATE_NAME` | 409 | 폼 인라인 "이미 존재하는 이름" + 기존 축 하이라이트 |
| 축 이름 blank | `LEARNING_AXIS_NAME_BLANK` | 400 | Zod 사전 차단 + 인라인 에러 |
| Axis/Topic 미존재 | `LEARNING_AXIS_NOT_FOUND` / `AXIS_TOPIC_NOT_FOUND` | 404 | 트리 refetch 후 삭제된 노드로 안내 토스트 |
| Topic 이름 중복 | `AXIS_TOPIC_DUPLICATE_NAME` | 409 | 인라인 |
| reorder id 집합 불일치 | (도메인 예외 → 400) | 400 | 낙관 롤백 + 트리 refetch + 재시도 안내 |
| Material 연결 중복 | 409 | 409 | 무시 (find-or-link 특성) |
| 자료 등록 시 동명 Deck | `DECK_NAME_DUPLICATE` | 409 | 자료 저장 자체 롤백 → confirm dialog "다른 이름으로 저장" |

### 관측 지표

- `map_render_latency_ms` — 트리 초기 렌더 (Axis 5+ Topic 20+ 자료 30+ 케이스)
- `reorder_rollback_total` — id 집합 불일치 발생률 (백엔드/FE 동기화 진단)
- `coverage_badge_flicker_ms` — 자료 연결 후 응답 갱신까지 시간

## 롤아웃 / 마이그레이션

### 전제

LearningFacade 도메인은 백엔드 done 상태. FE는 `features/map/`, `features/onboarding/`, `features/auth/hooks/useLearningFacade`가 존재하며 본 Product는 다음을 정리:

- CoverageBadge 색상 매핑 표준화
- dnd-kit 도입 여부 결정 (FE-ADR)
- 다건 트랜잭션 실패 UX 정착
- Material 연결 응답의 Deck 자동 생성 결과 표시

### Epic 의존성 그래프

```
Epic 1 (트리 렌더 + 색상 매핑)
  Story 1-1 (AxisNode/TopicNode 컴포넌트) ─► 1-2 (CoverageBadge)
                                             ─► 1-3 (Focus/Recommended 뱃지)

Epic 2 (Axis/Topic reorder — dnd-kit)
  Story 2-1 (라이브러리 도입 + FE-ADR) ─► 2-2 (useAxisReorder) ─► 2-3 (useTopicReorder)

Epic 3 (Material 연결 UX)
  Story 3-1 (MaterialLinkDialog) ─► 3-2 (연결/해제 mutation + coverage 반영)
                                     ─► 3-3 (동명 Deck 자동 생성 결과 표시)

Epic 4 (다건 추가 UX)
  Story 4-1 (다건 폼 + Zod 배열 검증) ─► 4-2 (전체 롤백 응답 UX)
```

## 성공 지표 (KPI)

| 지표 | 목표 값 | 측정 방법 |
| --- | --- | --- |
| CoverageBadge 색상 매핑 불일치 | 0건 | 상수 파일 단일 진실 + snapshot 테스트 |
| Axis/Topic reorder 낙관 → 최종 순서 불일치 | 0건 | E2E |
| Material 연결 후 coverage 즉시 반영 지연 | P95 ≤ 300ms | 텔레메트리 |
| 다건 추가 롤백 시 부분 저장 UI 노출 | 0건 | E2E |

## Scope

- **In Scope**
  - `features/map/` 전체 (트리 + reorder + 자료 연결 dialog)
  - `features/onboarding/` (concept + 초기 축 입력)
  - `features/auth/hooks/useLearningFacade` 확장
  - `lib/api/schemas/facade.ts`, `.../material.ts`
  - 관련 MSW handler
- **Out of Scope**
  - AI Axis/Topic 제안 UI → `product-aisuggestion.md`
  - AI 인터랙티브 로드맵 → `product-ai-interactive-roadmap.md`
  - 자료 URL 메타데이터 자동 추출 → v2
  - 갭 인지형 개인화 컨텍스트 → v1.5

## 연결된 Epic 목록

- [x] Epic 1: 트리 렌더 + 색상 매핑
- [x] Epic 2: Axis/Topic reorder
- [x] Epic 3: Material 연결 UX
- [x] Epic 4: 다건 추가 UX

## 관련 문서

- 백엔드 원본: `workflow/task/pes/workspectrum/sdd/done/product-learningFacade.md`
- 백엔드 BC 인계: `workflows/backend-boundary/api-learning-facade.md`
- FE 후속: `../in-progress/product-aisuggestion.md`, `../in-progress/product-ai-interactive-roadmap.md`

## 열린 질문

- **dnd-kit vs 대체 라이브러리** — 최종 FE-ADR로 확정
- **트리 가상화 도입 시점** — Axis·Topic이 수십 개를 넘기 시작하면 검토 (v2)
- **Coverage 색상 dark mode 정합성** — Tailwind semantic 토큰 도입 시 재검토
