# [Product] 미디어 업로드 FE — presigned URL 흐름 + 이미지 선택 UX

> **상태 메모**: 백엔드가 backlog 신호 명시("보류 (v0.0.3v+)"). FE도 backlog에 미리 정착시켜 두는 설계 초안. Card 텍스트 SRS UX가 안정된 후 진입.

## Product Vision

> Card `contentType`이 `TEXT_ONLY → IMAGE_ONLY/TEXT_AND_IMAGE`로 확장될 때, FE에서 presigned URL 흐름 + 진행률 + 미리보기 + 첨부 관리 UX를 원자적으로 지원한다. 이미지 본문은 클라이언트가 S3에 직접 업로드하고, 서버는 메타데이터만 수신. FE는 파일 선택 → presigned URL → 직접 PUT → 서버 통보 3단계 흐름을 사용자에게 매끄럽게 감춘다.

## 배경 및 문제

- **현재 상황 (As-Is)**
  - Card `contentType`이 `TEXT_ONLY` 단일 (또는 미정)
  - 이미지 첨부 UI 없음
  - `CardEditorPage`에 파일 입력 없음
- **발생하는 문제**
  - 시각 단서 학습(해부학·기계·언어 어휘) 사용자를 지원 못 함
  - 경쟁 서비스(Anki·RemNote) 대비 즉시 감점
- **왜 지금 해결해야 하는가**
  - 텍스트 SRS 안정화 후 진입 (백엔드 정책 정합)
  - 진입 시점 도래하면 Card 모델 + 인프라 + 보안 + 비용이 한꺼번에 → 사전 설계 미리 준비

## 목표 (To-Be)

- Card 첨부가 `TEXT_ONLY` / `IMAGE_ONLY` / `TEXT_AND_IMAGE` 3종 지원
- 이미지 선택 → presigned URL POST → S3 PUT → 완료 통보 흐름을 사용자에게 단일 액션으로 감춤
- 진행률 표시 (axios `onUploadProgress`)
- 미리보기 (선택 직후 blob URL, 완료 후 CDN URL)
- 실패 시 재시도 CTA (파일 단위)
- 최대 N장(v1=3) 클라이언트 사전 검증 + 서버 안전망
- 미리보기 콜라주 or 슬라이더 UI
- Card 조회 시 CloudFront edge cache 사용

## 설계 결정 (Design Decisions)

- **업로드 flow 3단계 hook으로 캡슐화 — `useMediaUpload()`**
  - 사용자는 파일만 선택 → hook 내부에서 3단계 흐름 (presign → S3 PUT → 서버 통보)
  - 진행률 state 노출 → UI가 progress bar 렌더
- **`MediaAttachmentInput` 컴포넌트** — `CardEditorPage`가 사용
  - drag-drop + 파일 선택 + 미리보기 그리드 + 삭제 아이콘
  - `dnd-kit` 재사용(learningFacade와 공통) 검토
- **`contentType`은 서버가 결정 — FE는 표시만**
  - 백엔드가 `addAttachment`/`removeAttachment`/`updateMainNote` 호출 시 자동 재계산
  - FE 응답 필드 그대로 표시. FE에서 직접 계산 X
- **이미지 사전 검증 = MIME + 크기 + 개수**
  - `image/png` / `image/jpeg` / `image/webp` 화이트리스트
  - ≤ 5MB (v1)
  - Card당 최대 3장
  - 위반 시 파일 선택 즉시 인라인 에러 (presign 요청 안 감)
- **진행률 표시 = 개별 파일 + 합계**
  - 다중 파일 동시 업로드 시 개별 progress bar + 상단 합계
- **미리보기 = blob URL → CDN URL 전환**
  - 선택 직후 `URL.createObjectURL(file)`로 blob 미리보기
  - 서버 통보 완료 후 응답의 `cdnUrl`로 교체
- **실패 시 재시도 = 개별 파일 단위**
  - 3개 중 1개 실패 시 나머지는 성공 유지, 실패 파일만 재시도 CTA

## 대안 검토 (Alternatives Considered)

### 업로드 topology

**Option A (선택) — 백엔드 정합: presigned URL + CloudFront**
- 비용: 3단계 흐름 캡슐화 필요
- 보상: 서버 대역폭 0, CDN 캐시

**Option B — 서버 경유 업로드**
- 거부 이유: 서버 부담, 백엔드 정책 위반

**Option C — Base64 인라인**
- 거부 이유: 요청 크기, 백엔드 거부

### 진행률 표시

**Option A — 완료/실패 2상태만**
- 거부 이유: 대용량 파일 UX 불안

**Option B (선택) — axios onUploadProgress 실시간 %**
- 비용: 진행률 state 관리
- 보상: 사용자 체감 안정

### 다중 파일 실패 처리

**Option A — 전체 롤백**
- 거부 이유: 성공한 것도 다시 해야 함

**Option B (선택) — 개별 파일 단위 재시도**
- 비용: 파일별 상태 관리
- 보상: 부분 성공 유지

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 / 라우트 배치

```
features/card-editor/
├─ CardEditorPage.tsx
│    └─ MediaAttachmentInput.tsx
│         ├─ FilePicker (drag-drop + <input type=file>)
│         ├─ PreviewGrid (blob URL → cdnUrl)
│         └─ ProgressList (파일별 %)
└─ hooks/
     └─ useMediaUpload()   ← 3단계 캡슐

features/card-detail/
└─ CardDetailPage.tsx
     └─ AttachmentGrid.tsx (cdnUrl 렌더)

lib/api/
├─ schemas/media.ts   (MediaAttachmentSchema)
└─ endpoints/media.ts (presignUpload, attachToCard, removeAttachment)
```

### 핵심 플로우

**1. 이미지 첨부 (사용자 단일 액션)**
```
사용자가 파일 선택 (or drag)
   │
   ▼
MediaAttachmentInput.onChange(files)
   ├─ 사전 검증 (MIME/size/count)
   ├─ 각 파일마다 useMediaUpload.mutateAsync(file)
   │    ├─ [Step 1] POST /media/presigned-upload { contentType, sizeBytes }
   │    │      응답: { uploadUrl, s3Key, expiresAt }
   │    ├─ [Step 2] PUT uploadUrl (S3 직접, onUploadProgress로 % 업데이트)
   │    ├─ [Step 3] POST /cards/{cardId}/media { s3Key }
   │    │      응답: { attachmentId, cdnUrl }
   │    └─ blob URL → cdnUrl 교체
   ├─ 완료 시 attachmentId 목록을 Card 편집 state에 반영
   └─ 실패 시 파일 단위로 재시도 CTA 표시
```

**2. 카드 조회 (첨부 표시)**
```
GET /cards/{id}/full
   └─ 응답: { ..., attachments: [{ id, cdnUrl, displayOrder }] }

CardDetailPage
   └─ AttachmentGrid (cdnUrl로 렌더, CloudFront edge cache)
```

**3. 첨부 삭제 (Card SoftDelete 정책 준수)**
```
사용자가 첨부 아이콘의 X 클릭
   │
   ▼
useRemoveAttachment.mutate({ cardId, attachmentId })
   ├─ DELETE /cards/{cardId}/media/{attachmentId}
   ├─ 즉시 S3 삭제 안 함 (백엔드 야간 배치)
   ├─ contentType 자동 재계산 응답 반영
   └─ invalidate ['card', cardId]
```

### 외부 의존

- **백엔드 `/media/presigned-upload`, `/cards/{id}/media`**: `endpoints/media.ts` 위임 호출
- **AWS S3**: PUT 직접 (presigned URL로 인증)
- **CloudFront**: GET 조회 (cdnUrl은 CloudFront distribution URL)
- **product-fe-cdn**: 배포 인프라 참조

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | ApiError code | HTTP | 클라이언트 권장 동작 (UX) |
| --- | --- | --- | --- |
| MIME 미지원 | (사전 차단) | - | 인라인 "PNG/JPG/WebP만 지원" |
| 크기 초과 | (사전 차단) | - | 인라인 "5MB 이하만" |
| 개수 초과 | (사전 차단) | - | Add 버튼 disabled |
| presigned URL 만료 (5분 지연) | S3 403 | 403 | 자동 재발급 + 재시도 (invisible retry) |
| S3 PUT 5xx | (any 5xx) | 5xx | 재시도 CTA + Sentry 로깅 |
| 서버 통보 실패 (headObject 미존재) | `MEDIA_OBJECT_NOT_FOUND` | 400 | 재업로드 유도 |
| 일일 업로드 한도 초과 | `MEDIA_DAILY_LIMIT_EXCEEDED` | 429 | 안내 dialog |
| 첨부 삭제 시도 실패 | `MEDIA_ATTACHMENT_NOT_FOUND` | 404 | invalidate + 무시 |

MSW handler로 5개 백엔드 시나리오 재현. S3 PUT은 로컬 mock server 또는 MSW로 재현. `msw/handlers/media.ts`.

### 로깅 정책 (FE)

- **항상 기록 (Sentry)**:
  - S3 PUT 5xx 발생 시 code + s3Key + sizeBytes
  - 사전 검증 실패 카운터 (MIME/size/count 별)
  - MEDIA_OBJECT_NOT_FOUND 발생 (server-side 재현 실패 시그널)
- **debug**: `useMediaUpload` 각 단계 진입/응답 raw
- **절대 금지**:
  - 파일 blob 원본 · base64 인코딩 데이터
  - 사용자 파일명 (PII 우려 — s3Key만 로깅)
  - cdnUrl 원본 (권한이 있는 URL 우려)

### 관측 지표

- 3MB 이미지 업로드 P95 ≤ 3s
- 사전 검증 통과 후 백엔드 400 응답 = 0건 / 주
- 파일 단위 재시도 성공률 ≥ 95%
- CDN cache hit rate (이미지) > 90%
- S3 PUT 5xx 재시도 후 최종 성공률 ≥ 98%
- CardEditorPage LCP (첨부 미포함) P95 ≤ 1.5s

## 롤아웃 / 마이그레이션 (Rollout)

### 전제

- 현재 사용자: Card 텍스트 편집 사용자 (첨부 UI 부재)
- 백엔드 `/media/*` API 확정 완료가 선행 조건 [v0.0.3v+ 예정]
- S3 버킷 · CloudFront distribution · presigned URL 서명자 구성 완료
- product-fe-cdn의 CloudFront 배포 인프라가 먼저 확립

### Product 의존성

- 선행: `product-auth.md` (인증 필수, presigned URL 요청 시 인터셉터)
- 선행: `product-fe-cdn.md` (CloudFront 배포 인프라)
- 선행: `../done/product-card.md` (contentType 확장 매핑)
- 후행: (없음)

### Epic·Story 의존성 그래프

```
Epic 1 (Media hook + schemas)
  Story 1-1 (useMediaUpload 3단계 캡슐) ─► 1-2 (Zod MediaAttachmentSchema)

Epic 2 (MediaAttachmentInput UI)
  Story 2-1 (FilePicker + drag-drop) ─► 2-2 (PreviewGrid blob→cdn)
                                         ─► 2-3 (ProgressList 파일별 %)

Epic 3 (CardDetailPage 첨부 표시 + 삭제)
  Story 3-1 (AttachmentGrid 렌더) ─► 3-2 (삭제 mutation)

Epic 4 (실패 시나리오 UX)
  Story 4-1 (재시도 CTA + invisible retry) ─► 4-2 (사전 검증 정합)
```

### 환경별 설정 분기

| 항목 | dev (`.env.development`) | prod (`.env.production`) |
| --- | --- | --- |
| `VITE_API_BASE_URL` | http://localhost:8080 | https://api.thirdtool.dev |
| `VITE_S3_UPLOAD_URL` | http://localhost:9000 (MinIO) | S3 endpoint (presigned URL) |
| `VITE_CDN_BASE_URL` | (blob URL 사용) | https://cdn.thirdtool.dev |
| MSW | enabled (`msw/handlers/media.ts`) | disabled |
| Sentry | local | enabled |

## 성공 지표 (KPI)

| 지표 | 목표 |
| --- | --- |
| 3MB 이미지 업로드 P95 | ≤ 3s |
| 사전 검증 통과 후 백엔드 400 응답 | 0건 |
| 파일 단위 재시도 성공률 | ≥ 95% |
| CDN cache hit rate (이미지) | > 90% |
| S3 PUT 5xx 최종 성공률 | ≥ 98% |
| MEDIA_OBJECT_NOT_FOUND 발생 | 0건 / 주 |

## Scope

**In Scope**:
- `features/card-editor/MediaAttachmentInput.tsx`
- `features/card-detail/AttachmentGrid.tsx`
- `useMediaUpload()` hook
- `lib/api/schemas/media.ts`, `lib/api/endpoints/media.ts`
- CardDetailPage 첨부 표시
- MSW handler 5개 시나리오

**Out of Scope**:
- 서버측 리사이징 (v2 백엔드)
- OCR 검색 (v2)
- 이미지 편집 (crop/rotate) v2
- 오프라인 업로드 큐 v3
- PDF/기타 문서 첨부 (v2)

## 대상 사용자

- **시각 학습자** — 해부학·기계·언어 어휘 등 이미지 단서가 필요한 학습자
- **기존 텍스트 학습자** — TEXT_ONLY 카드에 이미지 첨부로 확장
- **모바일 사용자** — 카메라 촬영으로 즉시 첨부 (v1 이후 확장)

## 연결된 Epic 목록 (진행 순서)

| 순서 | Epic | 제목 | Story 수 | 선행 의존 |
| --- | --- | --- | --- | --- |
| 1 | Epic 1 | Media hook + schemas | 2 | (없음) |
| 2 | Epic 2 | MediaAttachmentInput UI | 3 | Epic 1 |
| 3 | Epic 3 | CardDetailPage 첨부 표시 + 삭제 | 2 | Epic 1 |
| 4 | Epic 4 | 실패 시나리오 UX | 2 | Epic 2, 3 |

- [ ] Epic 1: Media hook + schemas
- [ ] Epic 2: MediaAttachmentInput UI
- [ ] Epic 3: CardDetailPage 첨부 표시 + 삭제
- [ ] Epic 4: 실패 시나리오 UX

## 관련 문서

- 백엔드 원본: `workflow/task/pes/workspectrum/sdd/in-progress/product-media.md`
- 백엔드 ADR: ADR-MEDIA-001~003
- 인접 FE: `../done/product-card.md` (contentType 확장), `./product-fe-cdn.md` (CloudFront)
- FE-ADR 후보: `FE-MEDIA-001: presigned URL 3단계 캡슐`, `FE-MEDIA-002: 파일 단위 재시도 정책`

## 열린 질문

- **모바일 카메라 직접 촬영** — `<input type=file capture>` 지원 여부
- **다중 파일 병렬 업로드 개수 제한** — 3개 병렬 vs 순차
- **미리보기 그리드 vs 슬라이더** — Card 상세에서 3장 표시 UX
- **PDF/기타 문서 첨부** — v2 확장
- **업로드 중 이탈 시 처리** — beforeunload 경고 vs 자동 abort

---

# [Epic 1] Media hook + schemas

## Epic 목표

Media 업로드의 3단계 흐름(presign → S3 PUT → 서버 통보)을 `useMediaUpload` hook 하나로 캡슐화하고, Zod 스키마로 응답 검증.

## 배경

Product의 인프라 Epic. 후속 Epic(UI, 첨부 표시, 실패 UX)은 본 Epic이 확립한 hook을 소비.

## 완료 기준

- [ ] Story 1-1, 1-2 완료
- [ ] `useMediaUpload` 단위 테스트 (Vitest + MSW)
- [ ] Zod 스키마 응답 검증 통과

## [Story 1-1] `useMediaUpload()` — 3단계 캡슐

### User Story
- As a MediaAttachmentInput
- I want 파일 하나만 넘기면 3단계 흐름이 자동 실행되기를
- so that UI 컴포넌트가 흐름 세부를 몰라도 되도록

### 설명
- `features/card-editor/hooks/useMediaUpload.ts`
- 3단계: presign 요청 → S3 PUT (onUploadProgress) → 서버 통보
- progress state (0~100) 노출
- 각 단계 실패 시 개별 error state

### 완료 기준 (AC)
- Given 유효 파일 / When mutate / Then 3단계 완료 + { attachmentId, cdnUrl } 반환
- Given S3 PUT 진행 중 / When onUploadProgress / Then progress state 업데이트
- *(엣지 - presigned URL 만료)* Given 5분 초과 / When PUT 시도 / Then 자동 재발급 후 재시도

### 의존성
- 선행: (없음)
- 후행: Story 1-2

## [Story 1-2] Zod MediaAttachmentSchema

### User Story
- As a FE 개발자
- I want 백엔드 응답을 Zod로 검증하기를
- so that 응답 스키마 변경 시 즉시 인지할 수 있다

### 설명
- `lib/api/schemas/media.ts` — `PresignResponseSchema`, `MediaAttachmentSchema`
- 필수 필드: `attachmentId`, `s3Key`, `cdnUrl`, `contentType`
- Zod parse 실패 시 Sentry 로깅

### 완료 기준 (AC)
- Given 정상 응답 / When Zod parse / Then 파싱 성공 + 타입 인식
- Given 필드 부재 / When Zod parse / Then 에러 throw + Sentry 로깅
- *(엣지 - v2 필드 추가)* Given 새 optional 필드 / When parse / Then 무시하고 파싱 성공

### 의존성
- 선행: (없음)
- 후행: Epic 2

---

# [Epic 2] MediaAttachmentInput UI

## Epic 목표

CardEditorPage에서 드래그-드롭 + 파일 선택 + 미리보기 그리드 + 진행률 표시가 통합된 UI 컴포넌트를 제공.

## 배경

Epic 1의 hook을 소비하는 UI 컴포넌트 레이어. 사용자 입력 경로의 핵심.

## 완료 기준

- [ ] Story 2-1, 2-2, 2-3 완료
- [ ] 드래그-드롭 접근성 (키보드 대체 경로) 통과
- [ ] 다중 파일 동시 업로드 E2E

## [Story 2-1] FilePicker + drag-drop

### User Story
- As a 사용자
- I want 이미지를 드래그하거나 클릭해서 선택하기를
- so that 자연스러운 첨부 UX를 경험한다

### 설명
- `features/card-editor/MediaAttachmentInput.tsx`
- `<input type=file multiple accept=image/*>` + drop zone
- 사전 검증 (MIME/size/count) 즉시 인라인 에러

### 완료 기준 (AC)
- Given 유효 파일 3개 / When 드래그-드롭 / Then 3개 useMediaUpload 각각 실행
- Given MIME 미지원 파일 / When 드래그 / Then 인라인 에러 + presign 안 함
- *(엣지 - 4번째 파일)* Given Card당 3장 초과 / When 추가 / Then 인라인 안내 + Add disabled

### 의존성
- 선행: Epic 1
- 후행: Story 2-2

## [Story 2-2] PreviewGrid — blob URL → cdnUrl 전환

### User Story
- As a 사용자
- I want 파일 선택 직후 즉시 미리보기가 표시되고 업로드 완료 후에는 CDN URL로 자동 전환되기를
- so that 대기 없이 진행 상태를 확인한다

### 설명
- `features/card-editor/components/PreviewGrid.tsx`
- 선택 직후: `URL.createObjectURL(file)`
- 업로드 완료 후: `attachment.cdnUrl`로 교체
- URL.revokeObjectURL로 blob cleanup

### 완료 기준 (AC)
- Given 파일 선택 / When 렌더 / Then blob URL preview 즉시 표시
- Given 업로드 완료 / When 렌더 / Then cdnUrl로 교체 + blob revoke
- *(엣지 - 컴포넌트 unmount)* Given blob 활성 상태에서 페이지 이탈 / When unmount / Then 모든 blob revoke

### 의존성
- 선행: Story 2-1
- 후행: Story 2-3

## [Story 2-3] ProgressList — 파일별 %

### User Story
- As a 사용자
- I want 다중 파일 업로드 시 각 파일의 진행률을 개별로 보기를
- so that 어느 파일이 느린지 인지할 수 있다

### 설명
- `features/card-editor/components/ProgressList.tsx`
- 각 파일마다 progress bar (0~100%)
- 상단에 합계 progress
- 완료된 파일은 체크 아이콘, 실패는 X + 재시도 CTA

### 완료 기준 (AC)
- Given 3개 파일 업로드 중 / When 렌더 / Then 3개 progress bar + 합계
- Given 1개 완료 / When 렌더 / Then 해당 파일 체크 아이콘
- *(엣지 - 실패)* Given 1개 실패 / When 렌더 / Then X 아이콘 + 재시도 버튼

### 의존성
- 선행: Story 2-2
- 후행: (없음)

---

# [Epic 3] CardDetailPage 첨부 표시 + 삭제

## Epic 목표

CardDetailPage에서 첨부 이미지를 그리드로 표시하고, 삭제 아이콘으로 개별 제거 가능. 삭제 시 백엔드 SoftDelete 정책 준수.

## 배경

카드 조회 시 첨부의 소비 UX. Epic 2와 병렬 진행 가능.

## 완료 기준

- [ ] Story 3-1, 3-2 완료
- [ ] AttachmentGrid 접근성 통과 (alt 텍스트, 키보드 탐색)
- [ ] 삭제 mutation E2E

## [Story 3-1] AttachmentGrid — cdnUrl 렌더

### User Story
- As a 학습자
- I want 카드 상세에서 첨부 이미지들을 한눈에 보기를
- so that 시각 단서를 즉시 활용할 수 있다

### 설명
- `features/card-detail/AttachmentGrid.tsx`
- Card 응답의 `attachments[]` 소비
- displayOrder 기준 정렬
- `<img loading=lazy>` 성능 최적화

### 완료 기준 (AC)
- Given attachments 3건 / When 렌더 / Then 3개 이미지 그리드
- Given attachments=[] / When 렌더 / Then AttachmentGrid 렌더 안 함
- *(엣지 - CDN 5xx)* Given cdnUrl 401/403 / When 이미지 로드 실패 / Then fallback placeholder + Sentry 로깅

### 의존성
- 선행: Epic 1
- 후행: Story 3-2

## [Story 3-2] 첨부 삭제 mutation

### User Story
- As a 카드 편집자
- I want 첨부 X 아이콘 클릭으로 개별 첨부를 제거하기를
- so that 필요 없는 이미지를 정리할 수 있다

### 설명
- `features/card-detail/hooks/useRemoveAttachment.ts`
- `DELETE /cards/{cardId}/media/{attachmentId}`
- 응답의 새 contentType으로 카드 캐시 갱신
- confirm dialog로 실수 방지

### 완료 기준 (AC)
- Given 첨부 1건 / When X 클릭 → confirm / Then DELETE 호출 + 그리드에서 제거
- Given 첨부 미존재 (이미 삭제) / When 재시도 / Then MEDIA_ATTACHMENT_NOT_FOUND 무시 + invalidate
- *(엣지 - contentType 변경)* Given 텍스트 없는 카드에서 마지막 이미지 삭제 / Then 백엔드 응답에 새 contentType 반영

### 의존성
- 선행: Story 3-1
- 후행: (없음)

---

# [Epic 4] 실패 시나리오 UX

## Epic 목표

presigned URL 만료·S3 5xx·서버 통보 실패·일일 한도 초과 등 실패 시나리오에 대한 회복 UX를 정립.

## 배경

Product의 안전망. 실패 처리가 부실하면 이미지 유실 리스크.

## 완료 기준

- [ ] Story 4-1, 4-2 완료
- [ ] 재시도 3회 후 자동 포기 정책 확립
- [ ] MSW로 각 실패 시나리오 재현 E2E

## [Story 4-1] 재시도 CTA + invisible retry

### User Story
- As a 사용자
- I want presigned URL 만료 같은 백엔드 tricky 이슈는 사용자가 인지 못하게 자동 재시도되기를
- so that 매번 불필요한 에러를 보지 않는다

### 설명
- presigned URL 만료(S3 403): 자동 재발급 + 재시도 (최대 2회, invisible)
- S3 PUT 5xx: 재시도 CTA (사용자 명시적 클릭)
- 서버 통보 실패(MEDIA_OBJECT_NOT_FOUND): 재업로드 유도
- 각 실패 유형별 다른 UX

### 완료 기준 (AC)
- Given presigned URL 만료 5분 / When PUT 재시도 / Then invisible re-presign + PUT 재시도
- Given S3 5xx / When 실패 / Then 재시도 CTA 표시 (사용자 클릭 필요)
- *(엣지 - 3회 실패)* Given 재시도 3회 실패 / Then 포기 + 안내 "일시 오류. 잠시 후 다시"

### 의존성
- 선행: Epic 2, 3
- 후행: Story 4-2

## [Story 4-2] 사전 검증 정합

### User Story
- As a FE 개발자
- I want 클라이언트 사전 검증이 백엔드 정책과 정확히 일치하기를
- so that 사전 통과 후 백엔드에서 실패하는 미스매치가 없다

### 설명
- MIME 화이트리스트: `image/png`, `image/jpeg`, `image/webp` (백엔드와 상수 공유 검토)
- 최대 크기: 5MB (v1)
- Card당 최대: 3장
- 백엔드 응답 코드로 미스매치 감지 시 Sentry alarm

### 완료 기준 (AC)
- Given 사전 통과 후 백엔드 400 / When 발생 / Then Sentry critical alarm
- Given 백엔드 정책 갱신 필요 / When Story 발견 / Then 상수 공유 방식 재검토
- *(엣지 - 정책 갱신 후)* Given 백엔드에서 정책 변경 / When FE 미갱신 / Then 백엔드 400 → Sentry alarm으로 catch

### 의존성
- 선행: Story 4-1
- 후행: (없음)
