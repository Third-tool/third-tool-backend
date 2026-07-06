# [Product] 검색 FE — 카드·자료·주제 통합 검색 UX

## Product Vision

> 사용자가 자연어 질의를 입력하면 200ms 이내에 카드·자료·주제 통합 결과를 표시하고, 한국어 형태소 매칭 위치를 `<mark>`로 하이라이트한다. 검색 입력은 디바운스(300ms), 결과는 타입별 그룹 카드 + 필터 chips. 검색은 학습 자산이 쌓일수록 자연스럽게 필수가 되는 접근성 인프라 — 30~50개 임계 사용자가 이탈하지 않도록 매끄러운 UX 확보.

## 배경 및 문제

- **현재 상황 (As-Is)**
  - FE에 검색창·검색 결과 페이지 없음
  - Card 목록은 태그 정확 일치 필터만 지원
  - 사용자가 카드 찾기 위해 목록 스크롤에 의존
- **발생하는 문제**
  - 카드 30+ 임계에서 검색 없이 "그 카드 어디 있더라" 부담 증가
  - 자료 검색이 없어 자료 연결 동기 저하
  - "프로그래밍" 검색 → "프로그래머" 매칭 못 함 (한국어 형태소 미지원 UX)
- **왜 지금 해결해야 하는가**
  - Card 도메인 모델(summary·mainNote·keyword 컬렉션) 안정 후 검색 UI 정합
  - 백엔드 OpenSearch 도입 결정 확정 시점에 FE UX 표준화

## 목표 (To-Be)

- 검색창(App shell 상단 or 전용 페이지)에 자연어 질의 입력 → 300ms 디바운스 → 결과 목록
- 결과 카드에 타입별 그룹(카드/자료/주제) + `<mark>` 하이라이트
- 필터 chips (docType 필터, 태그 필터)
- eventually consistent 안내 — 최근 편집된 카드가 검색에 안 뜰 수 있다는 UX 힌트
- OpenSearch 장애 시 503 처리 (`ApiError('MAINTENANCE', ...)`)로 검색 페이지에 오프라인 배너
- 검색 결과 클릭 → 원본 라우트(`/archive/{cardId}` 등)로 이동

## 설계 결정 (Design Decisions)

- **검색 입력 = 디바운스 300ms + minLength 2**
  - 짧은 입력에 대한 과도한 요청 방지
  - `useSearch(query)` hook 내부에서 디바운스
- **결과 표시 = 타입별 그룹 카드 (카드/자료/주제 3섹션)**
  - 백엔드가 `docType` 필드로 구분해 응답 → FE는 group by 후 섹션 렌더
  - 각 섹션 최대 5개 + "더 보기"
- **하이라이트 = `<mark>` 태그**
  - 백엔드 응답의 `_highlight.body` / `_highlight.title` 필드를 `dangerouslySetInnerHTML`로 렌더
  - `isomorphic-dompurify`로 XSS 방어 (이미 도입됨)
- **결과 클릭 = 원본 라우트로 이동**
  - 카드 → `/archive/{cardId}`, 자료/주제 → `/map` (해당 노드 스크롤 anchor)
- **필터 = URL query 파라미터로 sync**
  - 검색 상태를 URL에 유지 → 새로고침·공유 링크 대응
- **eventually consistent 안내 = 결과 상단 힌트**
  - "최근 편집한 카드는 잠시 후 반영될 수 있어요"
  - 첫 검색에만 표시, 이후 dismiss 가능
- **OpenSearch 장애 = 검색 페이지 오프라인 배너**
  - `MAINTENANCE` code 수신 시 검색 결과 자리에 "검색이 일시적으로 불가합니다. 목록 탐색을 이용하세요."

## 대안 검토 (Alternatives Considered)

### 검색 입력 위치

**Option A — App shell 상단 항상 표시**
- 장점: 접근성
- 거부 이유: 좁은 화면에서 공간 낭비

**Option B (선택) — 전용 라우트 `/search` + 헤더 아이콘 진입점**
- 비용: 진입 클릭 1회 추가
- 보상: 검색 UX 전용 공간 (필터·히스토리)

### 하이라이트 렌더 방식

**Option A — 텍스트 파싱 후 span 직접 생성**
- 거부 이유: 서버가 반환한 `<mark>` 위치 재계산 부담

**Option B (선택) — DOMPurify로 sanitize 후 dangerouslySetInnerHTML**
- 비용: XSS 위험 요소 존재
- 보상: sanitize 통과. 이미 프로젝트 도입 (Card mainNote 마크다운 렌더에 사용)

### 필터 상태 관리

**Option A — 컴포넌트 로컬 state**
- 거부 이유: 새로고침·공유 시 유실

**Option B (선택) — URL query params (React Router `useSearchParams`)**
- 비용: URL sync 로직 유지
- 보상: 링크 공유·새로고침 안전

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 / 라우트 배치

```
[Router]
  /search   → SearchPage
  헤더 아이콘 → navigate('/search')

features/search/
├─ SearchPage.tsx
├─ components/
│    ├─ SearchInput.tsx      (디바운스 + minLength)
│    ├─ FilterChips.tsx      (docType / tag 필터)
│    ├─ ResultGroupCard.tsx  (타입별 그룹)
│    ├─ ResultItem.tsx       (개별 결과 + <mark>)
│    └─ EventualConsistencyHint.tsx
└─ hooks/
     ├─ useSearch(query, filters)   (TanStack Query)
     └─ useSearchHistory()          (LocalStorage 최근 검색어)

lib/api/schemas/search.ts   (SearchResultSchema)
lib/api/endpoints/search.ts (searchAll)
```

### 핵심 플로우

**1. 검색 입력 → 결과 렌더**
```
사용자가 SearchInput에 입력
   │
   ▼
디바운스 300ms + minLength 2 통과
   │
   ▼
useSearch(query, { docType, tags }).data
   ├─ GET /search?q=...&docType=...&page=...
   ├─ 응답: { results: [{ docType, refId, title, body, _highlight, ... }], total }
   ├─ Zod parse
   └─ URL params sync (?q=...&docType=...)

SearchPage renders:
   ├─ EventualConsistencyHint (첫 검색만)
   ├─ FilterChips
   └─ 타입별 그룹
        ├─ 카드 섹션 (max 5 + "더 보기")
        ├─ 자료 섹션
        └─ 주제 섹션
```

**2. 결과 클릭 → 원본 라우트**
```
사용자가 ResultItem 클릭
   │
   ▼
switch (docType) {
  case 'card':     navigate(`/archive/${refId}`)
  case 'material': navigate(`/map#material-${refId}`)
  case 'topic':    navigate(`/map#topic-${refId}`)
}
```

**3. OpenSearch 장애 fallback**
```
GET /search → 503 MAINTENANCE
   │
   ▼
SearchPage 오프라인 배너
   "검색이 일시적으로 불가합니다. 목록 탐색을 이용하세요."
   [/archive 이동] CTA
```

### 외부 의존

- **백엔드 `/api/v1/search`**: `endpoints/search.ts` 위임 호출
- **`isomorphic-dompurify`**: 하이라이트 sanitize
- **LocalStorage**: 최근 검색어 (개인 브라우저만)
- **product-card**: 결과 클릭 시 `/archive/{cardId}` 라우트 소비
- **product-learningFacade**: 자료/주제 결과 클릭 시 `/map` 라우트 소비

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | ApiError code | HTTP | 클라이언트 권장 동작 (UX) |
| --- | --- | --- | --- |
| minLength 미달 | (사전 차단) | - | 결과 렌더 안 함 + 힌트 "2자 이상 입력" |
| OpenSearch 장애 | `MAINTENANCE` | 503 | 오프라인 배너 + 목록 탐색 CTA |
| 결과 0건 | (`total:0`) | 200 | "결과 없음" + 검색 팁 |
| 최근 편집 반영 지연 | (백엔드 정합) | 200 | EventualConsistencyHint |
| 인증 만료 | `AUTH002` | 401 | 인터셉터 처리 (product-auth) |
| Rate limit | `SEARCH_RATE_LIMIT` | 429 | 안내 + retry-after 카운트다운 |
| XSS 시도 (highlight sanitize 통과 후 잔존) | (사전 차단) | - | DOMPurify가 tag 제거 |

MSW handler로 5개 백엔드 시나리오 재현. `msw/handlers/search.ts`.

### 로깅 정책 (FE)

- **항상 기록 (Sentry)**:
  - MAINTENANCE 응답 수신 이벤트
  - XSS 유사 패턴 감지 시 (DOMPurify가 태그 제거) 카운터
  - 하이라이트 응답 필드 부재 시 (백엔드 스키마 미스매치) 이벤트
- **debug**: `useSearch` 응답 raw + 디바운스 진입 시점
- **절대 금지**:
  - 사용자 검색어 원문 (텔레메트리 opt-in 후만)
  - Card mainNote 본문 · 자료 원본 내용 reflection
  - LocalStorage 최근 검색어 서버 전송

### 관측 지표

- 검색 입력부터 결과 렌더까지 P95 ≤ 300ms (네트워크 제외)
- 디바운스 미준수(과도 요청) = 0건 / 주
- 하이라이트 XSS 필터 우회 = 0건
- MAINTENANCE 응답 발생율 (주간)
- 결과 0건 비율 (검색 개선 시그널)
- SearchPage LCP P95 ≤ 1.2s (Web Vitals)
- INP (검색 입력 반응) P95 ≤ 100ms

## 롤아웃 / 마이그레이션 (Rollout)

### 전제

- 현재 사용자: 검색 UI 부재 → 첫 도입 (기존 사용자 리스크 낮음)
- 백엔드 OpenSearch 도입 완료가 선행 조건
- Card 도메인 모델 안정화 완료

### Product 의존성

- 선행: `product-auth.md` (검색 API 인증 필수)
- 선행: `../done/product-card.md` (검색 결과 카드 라우트)
- 선행: `../done/product-learningFacade.md` (검색 결과 자료/주제 라우트)
- 후행: (없음, v2에 aisuggestion 연계)

### Epic·Story 의존성 그래프

```
Epic 1 (SearchPage 기본 UI)
  Story 1-1 (SearchInput + 디바운스) ─► 1-2 (useSearch hook)
                                        ─► 1-3 (결과 그룹 렌더)

Epic 2 (하이라이트 + sanitize)
  Story 2-1 (<mark> 렌더) ─► 2-2 (DOMPurify 정합)

Epic 3 (필터 + URL sync)
  Story 3-1 (FilterChips) ─► 3-2 (useSearchParams sync)

Epic 4 (fallback UX)
  Story 4-1 (오프라인 배너) ─► 4-2 (EventualConsistencyHint)

Epic 5 (최근 검색어)
  Story 5-1 (useSearchHistory)
```

### 환경별 설정 분기

| 항목 | dev (`.env.development`) | prod (`.env.production`) |
| --- | --- | --- |
| `VITE_API_BASE_URL` | http://localhost:8080 | https://api.thirdtool.dev |
| MSW | enabled (`msw/handlers/search.ts`) | disabled |
| Sentry | local | enabled |
| MAINTENANCE 시뮬레이션 | `?forceMaintenance=true` query 허용 | disabled |

## 성공 지표 (KPI)

| 지표 | 목표 |
| --- | --- |
| 검색 입력부터 결과 렌더까지 P95 | ≤ 300ms (네트워크 제외) |
| 디바운스 미준수(과도 요청) | 0건 |
| 하이라이트 XSS 필터 우회 | 0건 |
| OpenSearch 장애 시 사용자 이탈 감소 | 오프라인 fallback 진입율 측정 |
| SearchPage LCP | P95 ≤ 1.2s |
| INP (검색 입력 반응) | P95 ≤ 100ms |

## Scope

**In Scope**:
- `features/search/` 전체 (SearchPage, hooks, components)
- `lib/api/schemas/search.ts`, `endpoints/search.ts`
- MSW handler 5개 시나리오
- LocalStorage 최근 검색어 (Epic 5)

**Out of Scope**:
- OCR 이미지 검색 → v2
- 자연어 질의 확장 (Card 유사도) → v2 (product-aisuggestion 연계)
- 검색 사용 텔레메트리 대시보드 → v2
- 명령 팔레트(⌘K) → 열린 질문

## 대상 사용자

- **성숙 학습자 (카드 30장 이상)** — 목록 스크롤 부담 임계 도달, 검색 필수
- **자료 편집자** — 자료 재사용 위해 이름/설명 검색
- **주제 탐색자** — 축·주제 구조 안에서 특정 개념 위치 확인

## 연결된 Epic 목록 (진행 순서)

| 순서 | Epic | 제목 | Story 수 | 선행 의존 |
| --- | --- | --- | --- | --- |
| 1 | Epic 1 | SearchPage 기본 UI | 3 | (없음) |
| 2 | Epic 2 | 하이라이트 + sanitize | 2 | Epic 1 |
| 3 | Epic 3 | 필터 + URL sync | 2 | Epic 1 |
| 4 | Epic 4 | fallback UX | 2 | Epic 1 |
| 5 | Epic 5 | 최근 검색어 | 1 | Epic 1 |

- [ ] Epic 1: SearchPage 기본 UI
- [ ] Epic 2: 하이라이트 + sanitize
- [ ] Epic 3: 필터 + URL sync
- [ ] Epic 4: fallback UX
- [ ] Epic 5: 최근 검색어

## 관련 문서

- 백엔드 원본: `workflow/task/pes/workspectrum/sdd/in-progress/product-search.md`
- 백엔드 ADR: ADR-SEARCH-001~004
- 인접 FE: `../done/product-card.md`, `../done/product-learningFacade.md`
- FE-ADR 후보: `FE-SEARCH-001: 하이라이트 sanitize 정책`, `FE-SEARCH-002: URL sync 규칙`

## 열린 질문

- **검색 진입점 UX** — 헤더 아이콘 vs 명령 팔레트(⌘K) 도입 시점
- **오프라인 fallback 방식** — 로컬 인덱스(Fuse.js)로 client-side 검색을 backup으로 두는 검토
- **검색 결과 미리보기** — 카드 mainNote lazy prefetch로 결과 카드에서 바로 미리보기 표시할지
- **검색 텔레메트리 opt-in** — 검색 개선을 위해 익명 검색어 수집 여부 (v2)

---

# [Epic 1] SearchPage 기본 UI

## Epic 목표

`/search` 라우트에 검색 입력 → 300ms 디바운스 → 결과 그룹 렌더의 기본 흐름 확립.

## 배경

Product 진입점. 후속 Epic(하이라이트, 필터, fallback, 히스토리)은 본 Epic이 확립한 SearchPage 위에서 확장.

## 완료 기준

- [ ] Story 1-1, 1-2, 1-3 완료
- [ ] `useSearch` hook 단위 테스트 (Vitest + MSW)
- [ ] E2E: 입력 → 300ms 대기 → 결과 렌더

## [Story 1-1] SearchInput + 디바운스 + minLength

### User Story
- As a 사용자
- I want 검색창에 입력하면 300ms 후 요청되고 2자 미만은 요청 안 되기를
- so that 서버 부하 없이 검색을 시작할 수 있다

### 설명
- `features/search/components/SearchInput.tsx` — controlled input
- 300ms 디바운스 (커스텀 useDebounce hook)
- minLength=2 미달 시 hint 표시

### 완료 기준 (AC)
- Given 2자 입력 후 300ms 대기 / When 렌더 / Then useSearch trigger
- Given 1자 입력 / When 렌더 / Then hint "2자 이상 입력"
- *(엣지 - 연속 입력)* Given 2자 → 4자 (400ms 내) / When 마지막 입력 300ms 후 / Then 1회만 요청

### 의존성
- 선행: (없음)
- 후행: Story 1-2

## [Story 1-2] useSearch(query, filters) — TanStack Query

### User Story
- As a SearchPage
- I want query와 filter 변경 시 자동 refetch 되기를
- so that 컴포넌트가 데이터 페칭 로직을 몰라도 되도록

### 설명
- `features/search/hooks/useSearch.ts` — TanStack Query
- queryKey: `['search', query, filters]`
- staleTime: 60s
- 응답 Zod parse

### 완료 기준 (AC)
- Given query="react" / When mutate / Then { results, total } 반환
- Given filter 변경 / When refetch / Then 새 queryKey로 자동 fetch
- *(엣지 - 502)* Given 백엔드 5xx / When fetch / Then retry 1회 + 실패 시 error state

### 의존성
- 선행: Story 1-1
- 후행: Story 1-3

## [Story 1-3] 결과 그룹 렌더 — ResultGroupCard

### User Story
- As a 사용자
- I want 결과가 카드/자료/주제 3섹션으로 그룹화되어 표시되기를
- so that 어떤 타입의 자산을 찾았는지 즉시 인식할 수 있다

### 설명
- `features/search/components/ResultGroupCard.tsx` — 섹션 컨테이너
- `ResultItem.tsx` — 개별 결과
- 각 섹션 최대 5개 + "더 보기" 페이지네이션
- 결과 클릭 → docType 별 라우트 이동

### 완료 기준 (AC)
- Given results=[카드3, 자료2, 주제1] / When 렌더 / Then 3섹션 표시
- Given 카드 결과 클릭 / When 클릭 / Then `/archive/{refId}` navigate
- *(엣지 - 결과 0건)* Given total=0 / When 렌더 / Then "결과 없음" 힌트

### 의존성
- 선행: Story 1-2
- 후행: (없음)

---

# [Epic 2] 하이라이트 + sanitize

## Epic 목표

백엔드 응답의 `_highlight.body` / `_highlight.title`을 `<mark>` 태그로 렌더하되 DOMPurify로 sanitize.

## 배경

Epic 1의 결과 렌더에 하이라이트 스타일 추가. XSS 우려 필수 대응.

## 완료 기준

- [ ] Story 2-1, 2-2 완료
- [ ] XSS 테스트 케이스(script/img onerror 등) 통과
- [ ] FE-ADR-SEARCH-001 (하이라이트 sanitize 정책) 작성

## [Story 2-1] `<mark>` 렌더 with dangerouslySetInnerHTML

### User Story
- As a 사용자
- I want 검색어 매칭 위치가 노란 하이라이트로 강조되기를
- so that 어디서 매칭되었는지 즉시 인식할 수 있다

### 설명
- `features/search/components/ResultItem.tsx`
- `_highlight.body`가 있으면 `dangerouslySetInnerHTML={{ __html: sanitized }}`
- 없으면 plain body 렌더

### 완료 기준 (AC)
- Given `_highlight.body="<mark>react</mark> is..."` / When 렌더 / Then `<mark>` 노란 배경
- Given `_highlight` 필드 부재 / When 렌더 / Then plain body
- *(엣지)* Given `_highlight` 필드에 script tag / When 렌더 / Then Story 2-2 sanitize로 제거

### 의존성
- 선행: Epic 1
- 후행: Story 2-2

## [Story 2-2] DOMPurify 정합

### User Story
- As a 보안 담당
- I want 검색 결과 하이라이트에서 XSS가 원천 차단되기를
- so that 사용자 데이터가 안전하다

### 설명
- `isomorphic-dompurify.sanitize()` 호출 — `<mark>`만 허용
- ALLOWED_TAGS: ['mark']
- ALLOWED_ATTR: []
- 프로젝트 이미 도입 (Card mainNote 마크다운 렌더)

### 완료 기준 (AC)
- Given `<script>alert()</script>` 포함 응답 / When sanitize / Then script 제거
- Given `<mark>` 포함 응답 / When sanitize / Then `<mark>` 유지
- *(엣지)* Given `<img src=x onerror=alert()>` / When sanitize / Then 완전 제거

### 의존성
- 선행: Story 2-1
- 후행: (없음)

---

# [Epic 3] 필터 + URL sync

## Epic 목표

docType/tag 필터 chips 제공. URL query params와 sync해서 새로고침·공유 링크 유지.

## 배경

검색 UX의 필수 기능. Epic 1의 결과 렌더에 필터 적용.

## 완료 기준

- [ ] Story 3-1, 3-2 완료
- [ ] URL sync 새로고침 E2E
- [ ] FE-ADR-SEARCH-002 (URL sync 규칙) 작성

## [Story 3-1] FilterChips

### User Story
- As a 사용자
- I want 결과를 카드/자료/주제 타입별로, 또는 태그별로 필터링하기를
- so that 관심 영역만 좁혀볼 수 있다

### 설명
- `features/search/components/FilterChips.tsx`
- docType chips: 카드/자료/주제 (다중 선택 가능)
- tag chips: 카드에 붙은 상위 20개 태그 노출
- chip 선택/해제 시 filter state 갱신

### 완료 기준 (AC)
- Given "카드" chip 선택 / When 필터 / Then docType=card로 useSearch refetch
- Given "카드" + "자료" 다중 선택 / When 필터 / Then docType=card,material
- *(엣지)* Given 모든 chip 해제 / When 필터 / Then 필터 없이 전체 조회

### 의존성
- 선행: Epic 1
- 후행: Story 3-2

## [Story 3-2] useSearchParams sync

### User Story
- As a 사용자
- I want 검색 상태(query + filters)가 URL에 유지되기를
- so that 새로고침·링크 공유해도 결과가 복구된다

### 설명
- React Router `useSearchParams`
- query, docType, tags를 URL query params로 sync
- 페이지 진입 시 URL params → state 복구

### 완료 기준 (AC)
- Given query 입력 / When 결과 도착 / Then URL에 `?q=react` 갱신
- Given URL `/search?q=react&docType=card` 새로고침 / When 진입 / Then 상태 복구 후 자동 fetch
- *(엣지)* Given filter 해제 / When URL sync / Then 해당 param 제거

### 의존성
- 선행: Story 3-1
- 후행: (없음)

---

# [Epic 4] fallback UX

## Epic 목표

OpenSearch 장애(MAINTENANCE) 시 오프라인 배너, eventually consistent 안내로 사용자 이탈 감소.

## 배경

검색 인프라 장애를 사용자에게 정직하게 알리고 대체 경로 안내가 신뢰의 핵심.

## 완료 기준

- [ ] Story 4-1, 4-2 완료
- [ ] MAINTENANCE 시뮬레이션 E2E

## [Story 4-1] 오프라인 배너 — MAINTENANCE

### User Story
- As a 사용자
- I want 검색이 일시적으로 불가할 때 목록 탐색으로 우회 안내받기를
- so that 완전한 이탈 없이 다른 경로로 이어갈 수 있다

### 설명
- MAINTENANCE code 수신 시 결과 자리에 배너 렌더
- "검색이 일시적으로 불가합니다. 목록 탐색을 이용하세요."
- [/archive 이동] CTA

### 완료 기준 (AC)
- Given 503 MAINTENANCE / When useSearch throw / Then 배너 렌더
- Given [/archive 이동] 클릭 / When 클릭 / Then navigate('/archive')
- *(엣지)* Given MAINTENANCE 해제 후 재시도 / Then 배너 사라지고 정상 결과

### 의존성
- 선행: Epic 1
- 후행: Story 4-2

## [Story 4-2] EventualConsistencyHint

### User Story
- As a 사용자
- I want 최근 편집한 카드가 아직 검색에 안 뜨는 경우가 있음을 안내받기를
- so that "왜 안 나오지?"의 인지 부담 없이 잠시 대기할 수 있다

### 설명
- `features/search/components/EventualConsistencyHint.tsx`
- 첫 검색에만 렌더 (LocalStorage로 dismissed flag)
- "최근 편집한 카드는 잠시 후 반영될 수 있어요"

### 완료 기준 (AC)
- Given 첫 검색 진입 / When 결과 렌더 / Then hint 표시
- Given hint dismiss / When 다음 검색 / Then hint 렌더 안 함
- *(엣지 - 브라우저 초기화)* Given LocalStorage 초기화 / When 검색 / Then hint 재표시

### 의존성
- 선행: Story 4-1
- 후행: (없음)

---

# [Epic 5] 최근 검색어

## Epic 목표

사용자별 최근 검색어를 LocalStorage에 저장하고 SearchPage 진입 시 노출. 브라우저 로컬만 (서버 전송 X).

## 배경

편의 기능. UX 개선 후속 Epic.

## 완료 기준

- [ ] Story 5-1 완료
- [ ] LocalStorage 정책 준수 (서버 전송 없음) 확인

## [Story 5-1] useSearchHistory

### User Story
- As a 사용자
- I want 이전에 검색했던 키워드를 다시 제안받기를
- so that 반복 입력 부담 감소

### 설명
- `features/search/hooks/useSearchHistory.ts`
- LocalStorage 키: `search-history`
- 최대 10개 유지, LRU
- SearchInput 포커스 시 dropdown으로 노출

### 완료 기준 (AC)
- Given 검색 5회 / When 최근 검색어 조회 / Then 최신순 5개
- Given 11번째 검색 / When 저장 / Then 가장 오래된 항목 제거
- *(엣지 - 개인정보)* Given 이력 삭제 / When 클리어 / Then LocalStorage 초기화

### 의존성
- 선행: Epic 1
- 후행: (없음)
