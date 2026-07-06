# [Product] Card FE — 카드 표시·스터디·아카이브 UI

## Product Vision

> **백엔드가 Card를 "ON_FIELD / ARCHIVE 운영 위치"로 재정의한 도메인 결정을, FE UX에서 "실패 어휘 0건 + 상태 전이가 조용히 흡수되는" 화면 흐름으로 그대로 살려낸다.**
> 카드 퇴장은 실패가 아니라 운영 규칙이라는 백엔드 철학이 카드 스터디·아카이브·상세·에디터의 문구·시각 언어·상호작용 전반에 일관되게 드러난다.

## 설계 결정 (Design Decisions)

> Card FE의 큰 결정. 백엔드 도메인 결정을 UX 언어로 번역한 결과다.

- **상태 표기 어휘 — "실패" 금지, "정리됨/보관됨/휴식 중" 어휘로 통일**
  - 백엔드 KPI "퇴장 메시지 실패 어휘 0건"의 FE 대응. 카드 상세·목록·토스트·빈 상태 메시지 전 영역
  - `docs/ux/wip-language.md`(백엔드 rules) 참조 + FE 문구 매핑 테이블 (`workflows/backend-boundary/ux-writing.md`) 갱신
- **UI 상태 = 서버 상태의 파생. TanStack Query 캐시가 진실 소스**
  - `useCards()` / `useCard(cardId)` / `useArchivedCards()` 3개 hook이 정본. 로컬 스토어에 status 별도 보관 X
  - `record-view` mutation 후 `queryClient.invalidateQueries(['cards', ...])`로 갱신 — 낙관적 업데이트는 하지 않음(멱등이지만 서버 파생 필드 viewCount/lastViewedAt 진실 유지)
- **`contentType`(TEXT_ONLY/IMAGE_ONLY/TEXT_AND_IMAGE) 렌더 분기는 카드 상세 컴포넌트 내부에서**
  - 목록 응답에는 `mainNote` 본문 미포함 (백엔드 응답 컨벤션) → 목록에서는 텍스트 요약(summary) + tag chip만 렌더
  - 상세 진입 시에만 `mainNote` 전체 로드
  - v0.0.1v에는 IMAGE 지원 미구현 (`product-media.md`가 backlog)이므로 `TEXT_ONLY` 케이스만 실제 사용. 다른 케이스는 폴백 컴포넌트 예약
- **Tag find-or-create UX = 자유 입력 + 자동 완성 후보 표시**
  - 사용자가 텍스트 입력 → 시스템 전역 태그 목록에서 후보 제안 (미리 만들어진 hook `useTagSuggestions(query)`)
  - 없으면 새 값 그대로 카드 부착 요청 — 백엔드가 find-or-create로 흡수
  - 카드당 최대 3개 클라이언트 사전 검증 + 백엔드 `CARD_TAG_LIMIT_EXCEEDED` 안전망
- **키워드 cue 입력 = 최소 1개 클라이언트 검증 + 마지막 항목 제거 버튼 비활성화**
  - `CARD_KEYWORD_MIN_REQUIRED` / `CARD_KEYWORD_LAST_CANNOT_REMOVE` 백엔드 응답을 화면 도달 전에 차단
  - Zod 스키마 `CardCreateInputSchema.keywords.min(1)`, 폼 상태에서 keywords.length === 1일 때 삭제 아이콘 disabled
- **`recordView()` 트리거는 스터디 진입 시점, 상세 조회는 트리거 아님**
  - 상세(`/archive/:cardId`)에서는 조회 mutation을 부르지 않는다 — viewCount는 학습 세션에서만 누적된다는 백엔드 의도 보존
  - StudyPage에서 다음 카드 노출 시 `POST /api/v1/review/{sessionId}/next` 응답 처리 후 그 카드에 대해 부수 효과 없음 (recordView는 백엔드 내부)
- **archive/returnToField 요청은 낙관적 UI 유지 + 실패 시 롤백**
  - 아카이브 토글은 사용자 체감에 즉각 반영 필요 (멱등이라 안전)
  - `useMutation` `onMutate`로 캐시 낙관 갱신 → 실패 시 `onError` 롤백 + 토스트

## 대안 검토 (Alternatives Considered)

### 상태 진실 소스

**Option A — 별도 Zustand/Jotai 스토어에 로컬 카드 상태 보관**
- 장점: 클라이언트 즉시 반응
- 거부 이유:
  - 서버 파생 필드(`viewCount`/`lastViewedAt`/`enteredFieldAt`)는 클라이언트가 결정하지 않음 — 로컬 스토어를 두면 진실이 흐려짐
  - `Card.recordView()` 멱등성이 FE 낙관 업데이트로 왜곡될 위험 (2번 노출됐다고 로컬이 계산하면 서버와 어긋남)
  - TanStack Query 이미 도입됨 — 중복 도구 회피

**Option B (선택) — TanStack Query 캐시가 유일한 진실**
- 비용: 매 상태 전이 후 `invalidateQueries` 호출 필요
- 보상: 서버 파생 필드 진실 유지. 캐시 stale-while-revalidate로 UX 자연스러움

### archive UI 낙관 업데이트 vs 서버 대기

**Option A — 서버 응답 확인 후 UI 갱신**
- 거부 이유: 사용자 체감 지연 (P95 ~200ms 왕복). 아카이브는 자주 발생

**Option B (선택) — 낙관 업데이트 + 실패 롤백**
- 비용: 실패 시나리오 UX 처리 필요
- 보상: 카드 목록 전이가 즉시 반영. `Card.archive()` 멱등이라 안전

### mainNote 본문 프리로드 vs 상세 진입 시 로드

**Option A — 목록 응답에 mainNote 포함**
- 거부 이유: 백엔드 응답 컨벤션 위반 (목록은 무거운 필드 제외). 응답 크기 폭증

**Option B (선택) — 상세 진입 시 별도 GET**
- 비용: 상세 진입 시 1회 추가 요청
- 보상: 목록 응답 경량, 페이지네이션 성능. 백엔드 API 컨벤션 일치

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 / 라우트 배치

```
[React Router]
    │
    ├─ /home           → HomePage (오늘 스터디 진입)
    ├─ /study          → StudyPage (ReviewSession UI)
    ├─ /cards/new      → CardEditorPage (생성/편집)
    ├─ /archive        → ArchivePage (ARCHIVE 목록 + 필터)
    ├─ /archive/:cardId → CardDetailPage
    └─ /tags/:tagId    → TagDetailPage (연결 카드 목록)
         │
         ▼
┌──────────────────────────────────────────────────────────────────┐
│  features/cards/                                                  │
│  ├─ StudyPage.tsx            (스터디 세션 진입/카드 노출)          │
│  ├─ ArchivePage.tsx          (아카이브 목록 + 태그 필터)           │
│  ├─ CardDetailPage.tsx       (mainNote 본문 + 키워드/태그/이력)    │
│  └─ hooks/                                                        │
│       ├─ useCards(filter)                                         │
│       ├─ useCard(cardId)                                          │
│       ├─ useArchiveCard() / useReturnToField() (낙관 update)      │
│       └─ useCardMutations() (create / update / delete)            │
│  features/card-editor/                                            │
│  ├─ CardEditorPage.tsx       (생성/편집 폼 + Zod 검증)             │
│  └─ components/                                                    │
│       ├─ KeywordCueInput.tsx (min 1, 마지막 삭제 비활성)           │
│       ├─ TagChipsInput.tsx   (find-or-create + max 3 검증)         │
│       └─ SummaryInput.tsx    (1~3 문장 검증)                       │
│  features/tags/                                                   │
│  └─ hooks/useTagSuggestions(query)                                │
└──────────────────────────────────────────────────────────────────┘
    │
    │  Zod validate + api client
    ▼
┌──────────────────────────────────────────────────────────────────┐
│  lib/api/schemas/card.ts     (CardSchema / CardCreateInputSchema)│
│  lib/api/endpoints/card.ts   (createCard, updateCard, archive,    │
│                                returnToField, listCards, getCard) │
└──────────────────────────────────────────────────────────────────┘
    │
    ▼
[Backend api.thirdtool.dev/api/v1/cards/...]
```

### 핵심 플로우

**1. 카드 생성 (CardEditorPage → 서버)**

```
사용자가 텍스트/요약/키워드/태그 입력
   │
   ▼
CardEditorPage.onSubmit
   ├─ CardCreateInputSchema.parse(form)  ← 클라이언트 사전 검증
   │    ├─ keywords.length ≥ 1
   │    ├─ summary sentences ∈ [1,3]
   │    ├─ tags.length ≤ 3
   │    └─ trim 후 blank 값 차단
   ├─ useMutation createCard(input)
   │    ├─ POST /api/v1/cards
   │    └─ 응답 201 + Card DTO
   ├─ queryClient.invalidateQueries(['cards'])
   ├─ toast "저장됨"
   └─ navigate('/archive/{cardId}')  또는 /home
```

**2. 스터디 세션 카드 노출**

```
사용자가 /study 진입
   │
   ▼
StudyPage
   ├─ POST /api/v1/review/today  → 세션 시작
   ├─ useReviewSession(sessionId) 상태 관리
   ├─ POST /api/v1/review/{sid}/next → 카드 표시
   │    (백엔드 내부에서 recordView 트리거)
   ├─ 사용자가 "다음" 클릭 → 다음 카드 반복
   └─ 세션 종료 시 POST /api/v1/review/{sid}/finish
```

**3. 아카이브 토글 (낙관 UI)**

```
사용자가 아카이브 버튼 클릭 (아카이브 or 되돌리기)
   │
   ▼
useArchiveCard.mutate(cardId)
   ├─ onMutate: queryClient 캐시에서 해당 card status 즉시 갱신
   ├─ POST /api/v1/cards/{id}/archive  (or returnToField)
   ├─ onSuccess: invalidateQueries(['cards', ...])
   └─ onError: 낙관 갱신 롤백 + 토스트
```

### 외부 의존

- **백엔드 `/api/v1/cards/**`, `/api/v1/tags/**`, `/api/v1/review/**`** — 실제 도메인 진실 소스
- **`workflows/backend-boundary/api-card.md`** — 계약 상세 (BE 팀 SDD-lite)
- **`workflows/backend-boundary/error-codes.md`** — ErrorCode ↔ UX 문구 매핑
- **`docs/ux/wip-language.md`** (BE 저장소) — 실패 어휘 금지 정책

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 UX 응답

| 시나리오 | ApiError code | HTTP | UX 동작 |
| --- | --- | --- | --- |
| 존재하지 않는 카드 접근 | `CARD_NOT_FOUND` | 404 | `/archive` 또는 `/home`으로 replace + 토스트 "카드를 찾을 수 없습니다" |
| Keyword 미입력 폼 제출 | (사전 차단) `CARD_KEYWORD_MIN_REQUIRED` | 400 | Zod가 사전 차단 → 인라인 에러 "최소 1개 이상 입력하세요" |
| 마지막 Keyword 제거 시도 | 폼 UI 비활성 | - | 삭제 아이콘 disabled + tooltip "최소 1개 이상 필요합니다" |
| Summary 문장 수 범위 초과 | (사전) `CARD_SUMMARY_SENTENCE_OUT_OF_RANGE` | 400 | 인라인 에러 + 현재 문장 수 카운터 |
| MainNote 텍스트·이미지 모두 빈 | (사전) `CARD_MAIN_NOTE_EMPTY` | 400 | 인라인 에러 |
| Tag 4개 이상 부착 | (사전) `CARD_TAG_LIMIT_EXCEEDED` | 400 | Tag 4번째 chip 추가 시도 시 인라인 "최대 3개까지" + chip 추가 차단 |
| 이미 부착된 Tag 재부착 | `CARD_TAG_ALREADY_EXISTS` | 409 | 조용히 무시 (find-or-create 특성) |
| 다른 유저 카드 접근 | `AUTH_FORBIDDEN` | 403 | `/home`으로 replace + 토스트 "접근 권한이 없습니다" |
| 네트워크 단절 / MAINTENANCE | `INTERNAL_ERROR` / `MAINTENANCE` | 5xx / 503 | 재시도 버튼 + 토스트. `/maintenance` 라우트로 fallback (503) |

### FE 로깅 정책

- **항상 기록** (Sentry 도입 시): `ApiError.code + status + requestId` (X-Request-Id 헤더 매핑, `lib/api/client.ts` 이미 지원)
- **debug**: 낙관 업데이트 롤백 발생 시 이전/이후 상태 diff
- **절대 금지**: 카드 본문(`mainNote`) 텍스트를 로그로 전송. 사용자 학습 콘텐츠 (PII 준)

### 관측 지표 (v2 — Web Vitals / RUM 도입 시)

- `card_editor_submit_duration_ms` — 폼 제출부터 응답까지 P95
- `card_list_scroll_depth` — 무한 스크롤 시 사용 패턴
- `archive_toggle_rollback_total` — 낙관 롤백 발생 카운트 (원인 진단)

## 롤아웃 / 마이그레이션 (Rollout)

### 전제

카드 도메인은 백엔드 done 상태. FE는 이미 `features/cards/`, `features/card-editor/`, `features/tags/`가 존재하며 본 Product는 다음을 정리한다:

- 실패 어휘 → 운영 위치 어휘 grep + 치환
- `useCards` / `useCard` hook 정합성 점검 (invalidate 누락 없는지)
- Zod 스키마와 실제 백엔드 응답 매칭 검증 (MSW handler 최신화)

### Epic 의존성 그래프

```
Epic 1 (문구 재정비 — 실패 어휘 → 운영 위치)
  Story 1-1 (grep + 치환) ─► Story 1-2 (backend-boundary/ux-writing.md 갱신)

Epic 2 (폼 사전 검증 강화)
  Story 2-1 (Zod 정합) ─► Story 2-2 (인라인 에러 UI)
                           ─► Story 2-3 (마지막 삭제 비활성)

Epic 3 (낙관 update + 롤백)
  Story 3-1 (useArchiveCard) ─► Story 3-2 (useReturnToField)

Epic 4 (mainNote 상세 로드 최적화)
  Story 4-1 (상세 페이지 진입 시 lazy fetch)
```

### 환경별 설정 분기

| 항목 | dev | prod |
| --- | --- | --- |
| `VITE_API_BASE_URL` | http://localhost:8080 | https://api.thirdtool.dev |
| MSW handlers | `mocks/handlers/card.ts` 활성 | 비활성 |
| TanStack Query devtools | 표시 | 숨김 |

## 성공 지표 (KPI)

| 지표 | 목표 값 | 측정 방법 |
| --- | --- | --- |
| "실패" 어휘 잔존 | 0건 | CI grep — `src/features/cards`, `src/features/card-editor` 하위 |
| 사전 검증 통과 후 백엔드 400 응답 | 0건 (일치성) | E2E 테스트 |
| 낙관 업데이트 롤백 비율 | ≤ 1% | 프로덕션 텔레메트리 |
| 상세 페이지 mainNote LCP | P95 ≤ 1.5s | Web Vitals |
| Zod 스키마 파싱 실패 (백엔드 응답 미매칭) | 0건 | Sentry 알림 |

## Scope

- **In Scope**
  - `features/cards/` (StudyPage, ArchivePage, CardDetailPage)
  - `features/card-editor/` (CardEditorPage, KeywordCueInput, TagChipsInput, SummaryInput)
  - `features/tags/` (find-or-create UX 부분)
  - `lib/api/schemas/card.ts`, `lib/api/endpoints/card.ts` (엔티티 스키마 정합)
  - 관련 MSW handler (`mocks/handlers/card.ts`)
- **Out of Scope**
  - IMAGE 컨텐츠 렌더 (`product-media.md` backlog 진입 시)
  - Card 검색 UI (`product-search.md`)
  - AI 제안 UI (`product-aisuggestion.md`, `product-ai-interactive-roadmap.md`)
  - Deck 컨테이너 UI (`product-deck.md`)

## 대상 사용자

- 학습자 — 매일 스터디 세션에 진입하고 카드를 만드는 주요 페르소나
- (미래) 관리자 — 카드 검토/신고 처리는 backlog `product-admin.md`

## 연결된 Epic 목록

- [x] Epic 1: 문구 재정비 — 실패 어휘 → 운영 위치
- [x] Epic 2: 폼 사전 검증 강화
- [x] Epic 3: 낙관 update + 롤백
- [x] Epic 4: mainNote 상세 로드 최적화

(done 상태로 이관됨)

## 관련 문서

- 백엔드 원본: `workflow/task/pes/workspectrum/sdd/done/product-card.md`
- 백엔드 BC 인계 문서: `workflows/backend-boundary/api-card.md`, `workflows/backend-boundary/ux-writing.md`
- 후속 FE Product: `../in-progress/product-deck.md`, `../in-progress/product-media.md`

## 열린 질문 (Open Questions)

- **낙관 업데이트를 archive 외 다른 mutation에도 확장할지** — v2. 태그 부착/제거도 낙관 후보이나 실패 시나리오가 카드보다 많아 신중히 결정
- **카드 상세 mainNote 마크다운 렌더** — 현재 `marked` + `isomorphic-dompurify` 사용. XSS 정책 재검토 시점
- **오프라인 카드 열람** — Service Worker 캐시 정책 도입 시점 (product-notification과 SW 정합 필요)
