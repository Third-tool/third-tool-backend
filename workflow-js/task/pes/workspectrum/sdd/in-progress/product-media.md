# [Product] 미디어 업로드 — Card 시각 학습 지원 (S3 presigned URL + CloudFront)

> **상태 메모**: 1차 권장 = "보류 (v0.0.3v+ 검토)". 본 SDD는 backlog에 미리 정착시켜 두는 설계 초안 — Card BC `contentType` enum 확장과 mainNote 타입 재설계가 동반되는 아키텍처 충격이 크므로, 텍스트 기반 SRS 경험이 안정된 후 진입한다.

## Product Vision

> ThirdTool의 Card가 텍스트 단독에서 텍스트 + 이미지 조합으로 확장된다.
> 시각-언어 이중 코딩 이론(dual-coding theory)을 기반으로 시각적 기억 단서를 제공해 SRS 학습 효율을 끌어올린다.
> 이미지 본문 자체는 도메인의 책임이 아니라 외부 객체 스토리지(S3)에 위임하고, Card는 객체 키와 메타데이터만 보유한다.
> "본문은 도메인 안에, 미디어는 도메인 밖에" — 미디어 도입이 Card Aggregate의 무게를 늘리지 않게 한다.

## 배경 및 문제

- 현재 상황 (As-Is)
    - `Card.mainNote` 타입이 `TEXT` 단일 — 이미지 첨부 불가
    - `Card.contentType` enum은 (가칭) `TEXT_ONLY` 단일 (또는 미정)
    - 자료(`LearningMaterial`)는 URL 링크만 — 자체 첨부 이미지 보관 인프라 없음
    - 객체 스토리지(S3) 도입 이력 없음 — 인프라 결정 부재
    - 클라이언트가 이미지 업로드 시 서버를 거치는가, presigned URL 방식인가에 대한 결정 부재
- 발생하는 문제
    - 시각 단서(시간표·도표·도면·필기 사진)가 SRS 학습 효율을 크게 끌어올린다는 학습심리학 증거가 있음에도 활용 불가
    - 이미지가 필요한 학습 도메인(해부학·기계·언어 어휘 시각화)에서 사용자가 ThirdTool을 선택하지 않음
    - 자료(LearningMaterial) URL이 외부 사이트 의존 — 외부 사이트가 삭제되면 학습 자산 소실
    - 채용 포트폴리오 관점: "이미지 첨부도 없는 SRS 서비스" 즉시 감점 신호
    - 텍스트 + 이미지 조합이 가능한 경쟁 서비스(Anki·RemNote) 대비 명확한 약점
- 왜 지금 해결해야 하는가
    - 본 SDD는 **즉시 착수 대상이 아님** — `done/product-card.md`의 텍스트 기반 SRS 경험이 충분히 안정되고, MAU 임계가 확인된 후 진입
    - 단, 진입 시점이 오면 Card BC 모델 변경 + 인프라(S3·CloudFront) + 보안(presigned URL 만료·CORS) + 비용(CDN 대역폭)이 한꺼번에 들어옴 → 사전 설계 비용을 미리 지불해 두는 것이 가역성에 유리
    - product-search.md가 이미지 OCR 검색을 v2 항목으로 분리 — 본 Product의 모델 결정이 search 인덱스 매핑에 영향
    - product-cache.md의 LearningFacade 캐시·LLM 캐시와 별개로, 미디어 메타데이터 캐시 영역이 추가될 가능성

## 목표 (To-Be)

- Card가 `TEXT_ONLY` · `IMAGE_ONLY` · `TEXT_AND_IMAGE` 3종 타입을 지원한다
- 이미지 업로드는 클라이언트 → S3 직접(presigned URL) — 서버 대역폭 부담 0
- 이미지 조회는 CloudFront edge cache로 응답 — 원본 S3 비용 절감
- 이미지 객체 키는 사용자별·카드별 격리 (`media/{userId}/{cardId}/{uuid}.{ext}`)
- 단일 카드당 이미지 최대 N장 (v1 = 3장) — 도메인 규칙으로 강제
- 이미지 크기·MIME 타입을 사전 검증 (presigned URL 발급 단계 + 서버 검증)
- Card 삭제 시 연결된 S3 객체도 자동 정리 (eventually consistent, 야간 배치)
- 이미지 도입이 Card Aggregate 무게를 늘리지 않는다 — 객체 키·메타데이터만 도메인 안에 보유, 본문은 도메인 밖

## 설계 결정 (Design Decision)

> **이미지는 S3 presigned URL로 클라이언트가 직접 업로드. 서버를 경유하지 않는다.**
> 서버 대역폭·메모리 부담 회피 + 보안 표면 최소화.
>
> - 클라이언트가 `POST /media/presigned-upload` 호출 → 서버가 presigned URL (5분 만료) 발급 → 클라이언트가 S3에 PUT
> - 업로드 완료 후 클라이언트가 `POST /cards/{cardId}/media` 호출 → 서버가 객체 존재 검증 + 메타데이터 저장
> - presigned URL 발급 시 사전 검증: contentType, 최대 크기(예: 5MB), 사용자별 일일 업로드 한도
> - 이 결정은 ADR로 별도 기록한다 (`ADR-MEDIA-001: Upload Topology — Presigned URL Direct`)

> **CloudFront를 정적 자산 전달 경로로 사용한다. S3 직접 조회는 막는다.**
> S3 GET 비용 + 응답 지연 감소.
>
> - CloudFront origin = S3 (origin access control 적용)
> - 객체 URL은 CloudFront 도메인으로만 노출
> - S3 버킷은 직접 public access 차단 (block all public access ON)
> - CloudFront에 OAC + Signed URL 또는 Signed Cookie (private 카드 이미지 보호)
> - 이 결정은 ADR로 별도 기록한다 (`ADR-MEDIA-002: Delivery — CloudFront with OAC`)

> **Card는 `MediaAttachment` 자식 Entity 컬렉션을 보유한다. 본문은 S3에 위임.**
> Card Aggregate 무게 유지.
>
> - `Card.attachments` = `List<MediaAttachment>` (자식 Entity, displayOrder + s3Key + mimeType + sizeBytes + uploadedAt)
> - `Card.addAttachment(MediaCommand)` / `removeAttachment(attachmentId)` / `reorderAttachments(orderedIds)` 행위
> - 정적 팩토리 `MediaAttachment.of(s3Key, mimeType, sizeBytes)` — 외부 new 금지
> - displayOrder 1-based, reorder 시 id 집합 불일치 → 예외 (도메인 컨벤션 1.4 준수)
> - 최대 N장 강제 (`addAttachment` 안에서 검증)

> **Card.contentType은 첨부 상태에 따라 도메인이 자동 결정. 외부에서 직접 주입 불가.**
>
> - 텍스트만 있음 → `TEXT_ONLY`
> - 이미지만 있음 → `IMAGE_ONLY` (mainNote가 비어있는 경우)
> - 텍스트 + 이미지 → `TEXT_AND_IMAGE`
> - `addAttachment` / `removeAttachment` / `updateMainNote` 호출 시 내부 재계산
> - 외부 setter 없음 (도메인 컨벤션 1.11)

> **S3 객체는 Card 삭제 시 즉시 지우지 않고 야간 배치로 정리한다.**
> Soft Delete + eventually consistent.
>
> - Card가 SoftDelete(soft delete = `deleted_at IS NOT NULL`) — 일정 기간(30일) 복원 가능
> - 즉시 S3 객체 삭제 시 복원 불가
> - 야간 배치(`MediaOrphanCleanupJob`, 03:00)가 다음을 정리:
>     - 삭제된 카드의 객체 (deleted_at < now - 30일)
>     - `media/temp/` 미사용 presigned 객체 (uploaded_at < now - 24시간, Card 연결 없음)
> - 이 결정은 ADR로 별도 기록한다 (`ADR-MEDIA-003: Cleanup Strategy — Nightly Batch, Soft Delete Window`)

> **이미지는 원본 그대로 저장한다. 서버 측 리사이징·변환은 v2.**
> v1 단순화.
>
> - 클라이언트가 업로드 전 적절한 크기로 압축 (UI 가이드)
> - 서버는 contentType / size 검증만 — 변환 처리 안 함
> - v2에 Lambda@Edge 또는 S3 Object Lambda로 리사이징·WebP 변환 검토

## 대안 검토 (Alternatives Considered)

### 업로드 토폴로지

**Option A (선택) — S3 presigned URL + CloudFront**
- 비용: presigned URL 발급 로직 + CloudFront 비용 + 클라이언트 측 업로드 흐름 복잡도
- 보상: 서버 대역폭 0, 보안 표면 최소화, CDN으로 글로벌 지연 감소
- 트레이드오프 수용 근거: 표준 패턴. 향후 트래픽 증가에 강함

**Option B — S3 presigned URL만 (CloudFront 없음)**
- 장점: CDN 없이 단순, 비용 절감
- 거부 이유:
    - S3 GET 비용 누적 + 글로벌 지연
    - 다음 단계 전환 시 URL 형식 변경 비용 발생
    - CloudFront 없이 private 객체 보호는 presigned GET URL 발급 부담

**Option C — Base64 인코딩 후 DB 저장**
- 거부 이유:
    - DB 비대화 (이미지당 수 MB) → 백업·복제 비용 폭증
    - 조회 응답 지연 (대용량 row)
    - 미디어 도메인 분리 원칙 정면 위반

### Card 도메인 모델 변경

**Option A (선택) — Card에 MediaAttachment 자식 Entity 컬렉션**
- 비용: Card Aggregate에 컬렉션 1종 추가 + displayOrder 관리
- 보상: 도메인 모델 일관성 (자식 컬렉션 관리 = 기존 keyword·tag 패턴 재사용)

**Option B — 별도 Media BC + Card는 mediaIds만 보유**
- 거부 이유:
    - 미디어는 Card에 종속 — 독립 Aggregate 가치 낮음
    - 트랜잭션 경계 복잡화 (Card 저장 시 Media도 일관성)
    - 다른 도메인이 미디어를 공유하게 되면 그때 분리 검토

**Option C — Card.mainNote 타입을 union(text + image[]) JSON으로 변경**
- 거부 이유:
    - JSON 안에서 displayOrder·검증 강제 어려움
    - 검색·인덱싱 시 매핑 복잡
    - 향후 contentType 추가 시 schema 변경 비용

### 정리 전략

**Option A (선택) — 야간 배치 + 30일 복원 윈도우**
- 비용: 배치 운영 + 30일간 미사용 객체 비용
- 보상: 사용자 실수 복원 가능, 즉시 삭제 사고 방지

**Option B — 즉시 삭제 (트랜잭션 안)**
- 거부 이유:
    - 트랜잭션 실패 시 S3 객체 고아 발생
    - 사용자 실수 복원 불가

**Option C — S3 Lifecycle Policy로 자동 만료**
- 거부 이유:
    - 카드 라이프사이클과 S3 라이프사이클이 불일치 (사용자 행위 ≠ 객체 나이)
    - 정밀 제어 불가

### CDN 배포 채택 시점

**Option A (선택) — v1부터 CloudFront 도입**
- 비용: 초기 인프라 설정 부담
- 보상: 향후 트래픽 증가 무비용 흡수, S3 비용 절감

**Option B — v1은 S3 직접, v2에 CloudFront 전환**
- 거부 이유:
    - URL 형식 변경 시 클라이언트·DB 마이그레이션 부담
    - 작을 때 깔아두는 게 가역성 최대

## 전체 아키텍처 (High-Level Architecture)

### 컴포넌트 배치

```
┌─────────────────────────────────────────────────────────────┐
│ Presentation                                                 │
│   MediaController       (POST /media/presigned-upload)       │
│   CardMediaController   (POST /cards/{cardId}/media,          │
│                          DELETE /cards/{cardId}/media/{id})   │
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────────┐
│ Application                                                  │
│   MediaCommandService   (presigned URL 발급 + 사용자 한도)    │
│   CardMediaCommandService (Card.addAttachment 조율)          │
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────────┐
│ Domain                                                       │
│   Card Aggregate (attachments 컬렉션 추가)                    │
│   MediaAttachment (자식 Entity — s3Key·mimeType·size·order)  │
│   ContentType enum 확장 (TEXT_ONLY/IMAGE_ONLY/TEXT_AND_IMAGE)│
│   MediaStoragePort (outbound)                                │
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────────┐
│ Infrastructure                                               │
│   S3MediaStorageAdapter (AWS SDK v2 — presigned + headObject)│
│   CloudFrontUrlBuilder  (객체 키 → 배포 URL)                  │
│   MediaOrphanCleanupJob (@Scheduled 03:00 야간 배치)          │
└─────────────────────────────────────────────────────────────┘
```

### 핵심 플로우

**1. 업로드 흐름 (클라이언트 → S3 직접)**
```
[Step 1] POST /media/presigned-upload  { contentType, sizeBytes }
   │
   ▼
MediaCommandService.issuePresignedUpload(userId, ...)
   ├─ 검증: contentType ∈ {image/png, image/jpeg, image/webp}, sizeBytes ≤ 5MB
   ├─ 일일 업로드 한도 확인 (사용자별 카운터)
   ├─ s3Key = "media/temp/{userId}/{uuid}.{ext}"
   └─ S3MediaStorageAdapter.presign(s3Key, contentType, expiry=5min)
                  │
                  ▼
            응답: { uploadUrl, s3Key, expiresAt }

[Step 2] 클라이언트가 S3에 PUT (서버 미경유)

[Step 3] POST /cards/{cardId}/media  { s3Key }
   │
   ▼
CardMediaCommandService.attach(cardId, userId, s3Key)
   ├─ S3MediaStorageAdapter.headObject(s3Key) — 객체 실제 존재 확인
   ├─ 객체를 영구 경로로 이동 ("media/{userId}/{cardId}/{uuid}.{ext}")
   ├─ card.addAttachment(MediaCommand(newKey, mimeType, sizeBytes))
   │     ├─ 최대 N장 검증
   │     ├─ displayOrder = 마지막 + 1
   │     └─ contentType 자동 재계산
   └─ cardRepository.save(card)
                  │
                  ▼
            응답: 201 { attachmentId, cdnUrl }
```

**2. 조회 흐름 (CloudFront edge)**
```
GET /cards/{cardId}/full
   │
   ▼
CardQueryService.getCard(cardId, userId)
   ├─ card.attachments → CloudFrontUrlBuilder.build(s3Key) for each
   └─ 응답: { ..., attachments: [{ id, cdnUrl, displayOrder, ... }] }

[브라우저] GET https://cdn.thirdtool.com/media/{userId}/{cardId}/{uuid}.png
   │
   ▼
CloudFront edge cache
   ├─ hit → 즉시 응답
   └─ miss → S3 origin (OAC 인증) → cache 저장
```

**3. 야간 정리 흐름**
```
@Scheduled(cron = "0 0 3 * * *") (KST 03:00)
MediaOrphanCleanupJob.run()
   ├─ Card.deleted_at < now - 30일 → 해당 카드의 모든 attachment s3Key 수집
   ├─ media/temp/* 중 uploaded_at < now - 24시간 + Card 연결 없음
   ├─ S3MediaStorageAdapter.deleteBatch(keys) (1,000건 단위)
   └─ 메트릭 + 정리 결과 INFO 로그
```

### Out-of-Process 의존

- **AWS S3** — 객체 스토리지 (버킷 1개: `thirdtool-media-{env}`, OAC 적용, public access 차단)
- **AWS CloudFront** — 정적 자산 배포 (origin = S3, OAC, signed URL 옵션 v2 검토)
- **MySQL** — Card·MediaAttachment 메타데이터 진실 소스 (객체 본문은 S3)
- **Secrets Manager** — CloudFront key pair (signed URL 발급 시, v2)

### 핵심 컴포넌트

| 컴포넌트 | 위치 | 책임 |
| --- | --- | --- |
| `MediaController` | `Media/presentation/` | presigned upload 발급 API |
| `CardMediaController` | `Card/presentation/` | Card에 attachment 연결·해제·재배치 |
| `MediaCommandService` | `Media/application/` | presigned URL 발급 + 한도 |
| `CardMediaCommandService` | `Card/application/` | Card.addAttachment 조율 + S3 영구 경로 이동 |
| `MediaAttachment` | `Card/domain/model/` | Card 자식 Entity |
| `MediaStoragePort` | `Media/domain/port/` | outbound port |
| `S3MediaStorageAdapter` | `Media/infrastructure/s3/` | AWS SDK v2 어댑터 |
| `CloudFrontUrlBuilder` | `Media/infrastructure/cdn/` | 객체 키 → 배포 URL |
| `MediaOrphanCleanupJob` | `Media/infrastructure/scheduler/` | 야간 정리 배치 |

## 실패 모드 / 운영 관측 (Failure Modes & Observability)

### 실패 시나리오와 응답

| 시나리오 | ErrorCode | HTTP | 클라이언트 권장 동작 |
| --- | --- | --- | --- |
| MIME 타입 미허용 | `MEDIA_MIME_NOT_ALLOWED` | 400 | 지원 형식 안내 |
| 크기 초과 (> 5MB) | `MEDIA_SIZE_EXCEEDED` | 400 | 압축 후 재시도 |
| 일일 업로드 한도 초과 | `MEDIA_DAILY_LIMIT_EXCEEDED` | 429 | 다음 날 재시도 |
| presigned URL 만료 후 PUT | (S3 403) | — | 클라이언트가 재발급 후 재시도 |
| Card에 attachment 최대치 (3장) 초과 | `CARD_MEDIA_LIMIT_EXCEEDED` | 400 | 기존 첨부 제거 후 재시도 |
| 존재하지 않는 s3Key로 attach 시도 | `MEDIA_OBJECT_NOT_FOUND` | 404 | 업로드 재시도 |
| 다른 사용자의 s3Key 사용 시도 | `MEDIA_OWNERSHIP_VIOLATION` | 403 | 보안 사고 — 차단 + WARN 로그 |
| Card SoftDelete 후 야간 정리 실패 | (내부) ERROR | — | 다음 배치에서 재시도 |
| CloudFront 다운 | (외부) | — | 응답 지연. 도메인 행위는 영향 없음 |
| S3 다운 | (외부) | — | 업로드/조회 차단. Card 도메인 행위는 정상 |

### 로깅 정책

- **항상 기록**:
    - 업로드 발급 (INFO, userId·contentType·size·s3Key 8자 해시)
    - attachment 연결 (INFO, cardId·attachmentId·s3Key 해시)
    - 정리 배치 결과 (INFO, deletedCount·orphanCount·durationMs)
    - 소유권 위반 시도 (WARN, actor·target·s3Key)
- **DEBUG**: S3 응답 본문 (prod 비활성)
- **절대 금지**:
    - s3Key 원문 (해시 8자만 — 객체 추측 방지)
    - CloudFront signed URL 발급 키
    - 이미지 본문 자체 (절대 로깅 대상 아님)

### 관측 지표

| 지표 | 형식 | 의미 |
| --- | --- | --- |
| `media_upload_issued_total{status}` | 카운터 | presigned 발급 수. status=`success`/`size_exceeded`/`limit_exceeded` |
| `media_attach_total{status}` | 카운터 | attach 결과. status=`success`/`not_found`/`limit_exceeded` |
| `media_storage_bytes_total` | 게이지 | 전체 S3 저장 용량 (비용 추적) |
| `media_orphan_cleanup_total` | 카운터 | 야간 정리로 삭제된 객체 수 |
| `media_cdn_hit_ratio` | 게이지 | CloudFront cache hit률 (CloudWatch 메트릭) |
| `media_user_quota_usage_ratio` | 게이지 | 사용자별 일일 한도 소진율 |

## 롤아웃 / 마이그레이션 (Rollout)

### 전제 — 텍스트 SRS 안정 + Card BC 모델 confidence

본 Product 진입 전제:
1. `done/product-card.md` 텍스트 모델이 충분히 안정 (SRS 핵심 행위에 마지막 변경 후 N개월 이상)
2. MAU 임계 (사용자 N명 이상)
3. 이미지 학습 요구가 데이터로 확인됨 (설문 또는 사용자 인터뷰)

이 3개 조건 미달 시 backlog 유지 — 본 SDD는 사전 설계만 마련.

### Product 의존성

- **선행 Product**:
    - `done/product-card.md` — Card BC 안정 baseline
    - `in-progress/product-infra-network.md` — VPC + S3 endpoint
    - `in-progress/product-infra-ops.md` — Secrets Manager (CloudFront 키 v2)
    - `in-progress/product-op.md` — 메트릭 노출
- **후행 Product (가능성)**:
    - `backlog/product-search.md` v2 — 이미지 OCR 검색 시 본 모델 활용
    - `backlog/product-cache.md` — 미디어 메타데이터 캐시 region 추가
    - `backlog/product-admin.md` — 운영자가 이상 미디어 강제 삭제 API (v2)

### Epic·Story 의존성 그래프

```
Epic 1 (인프라 + 도메인 모델)
  Story 1-1 Terraform — S3 버킷(OAC, public access block) + CloudFront 배포
  Story 1-2 ContentType enum 확장 + Card.attachments 컬렉션
  Story 1-3 MediaAttachment Entity + 정적 팩토리 + 단위 테스트
  Story 1-4 Card.addAttachment/removeAttachment/reorderAttachments 도메인 행위
  Story 1-5 Flyway 마이그레이션 (media_attachment 테이블 + FK + 인덱스)
       │
       ▼
Epic 2 (Storage 어댑터 + presigned URL)
  Story 2-1 MediaStoragePort + S3MediaStorageAdapter (presign/headObject/move/delete)
  Story 2-2 MediaController POST /media/presigned-upload + 일일 한도
  Story 2-3 CloudFrontUrlBuilder + 응답 DTO에 cdnUrl
       │
       ▼
Epic 3 (Card 연결 API)
  Story 3-1 POST /cards/{cardId}/media — temp → 영구 경로 이동 + addAttachment
  Story 3-2 DELETE /cards/{cardId}/media/{attachmentId}
  Story 3-3 PATCH /cards/{cardId}/media/order — reorder
  Story 3-4 Card 조회 응답에 attachments + cdnUrl 포함
       │
       ▼
Epic 4 (정리 + 운영)
  Story 4-1 MediaOrphanCleanupJob (@Scheduled 03:00)
  Story 4-2 메트릭 노출 + media_storage_bytes 알림 (비용 추적)
  Story 4-3 docs/media.md + 운영 Runbook (장애·복구·수동 정리)
       │
       ▼
Epic 5 (v2 — 보안·리사이징, 별도 진입)
  Story 5-1 CloudFront Signed URL/Cookie (private 카드 이미지 보호)
  Story 5-2 Lambda@Edge 또는 S3 Object Lambda 리사이징
  Story 5-3 WebP 변환 + 다단계 크기 생성
```

### 환경별 설정 분기

| 항목 | dev | prod |
| --- | --- | --- |
| S3 버킷 | `thirdtool-media-dev` | `thirdtool-media-prod` |
| CloudFront 도메인 | `dev-cdn.thirdtool.com` | `cdn.thirdtool.com` |
| presigned 만료 | 5분 | 5분 |
| 일일 업로드 한도 | 50건/사용자 | 20건/사용자 |
| 카드당 최대 첨부 | 3장 | 3장 |
| 야간 정리 cron | `0 0 3 * * *` (KST) | 동일 |
| Soft Delete 윈도우 | 7일 | 30일 |

## 성공 지표 (KPI)

- 이미지 업로드 성공률 ≥ 95%
- 업로드 후 카드 조회까지 (cdnUrl 첫 hit) P95 ≤ 1초
- CloudFront cache hit률 ≥ 70%
- 고아 객체 비율(전체 객체 대비) ≤ 2% (야간 배치 기준)
- 사용자별 일일 한도 초과 신고 = 0건 (UX가 한도를 사전 안내)
- 다른 사용자 객체 노출 사고 = 0건 (소유권 검증)
- S3 비용 + CloudFront 비용 월 합계가 예측 대비 ±20% 이내

## Scope

**In Scope (v1)**:
- 이미지 업로드 (PNG·JPEG·WebP), 최대 5MB
- Card 1개당 최대 3장 첨부
- Card.contentType 3종 자동 결정
- presigned URL (5분 만료) + S3 직접 업로드
- CloudFront 배포 + OAC
- 야간 고아 객체 정리 배치
- 사용자별 일일 업로드 한도

**Out of Scope (v1)**:
- 동영상·오디오 첨부 — 비용·복잡도 별도 결정
- PDF 첨부 — 별도 Product 검토
- 이미지 리사이징·WebP 변환 — v2 (Epic 5)
- CloudFront Signed URL/Cookie (private 보호) — v2
- 이미지 OCR 검색 — `backlog/product-search.md` v2 연계
- 이미지 편집(crop·회전) — 클라이언트 책임 v1, 서버 책임은 v2 검토
- 글로벌 멀티 리전 S3 — 단일 리전 v1

## 대상 사용자

- **학습자** — 시각 단서(도표·필기·도면)가 필요한 학습 도메인에서 카드를 만들고 복습
- **시각 의존 학습 도메인 (해부학·기계·언어·요리 등)** — 이전엔 ThirdTool 선택지 외였던 사용자 흡수
- **운영자** — 미디어 저장 용량·CDN 비용 메트릭으로 비용 추적

## 연결된 Epic 목록

- [ ] Epic 1: 인프라 + 도메인 모델
- [ ] Epic 2: Storage 어댑터 + presigned URL
- [ ] Epic 3: Card 연결 API
- [ ] Epic 4: 정리 + 운영
- [ ] Epic 5: v2 — 보안·리사이징

## 관련 문서

- 의존 Product: `done/product-card.md` (Card BC), `in-progress/product-infra-network.md`, `in-progress/product-infra-ops.md`, `in-progress/product-op.md`
- 관련 ADR (예정): `ADR-MEDIA-001 ~ 003` (업로드 토폴로지 / 배포 / 정리 전략)
- DOMAIN.md 갱신 예정 섹션: `Card BC — 미디어 첨부` 신규 절
- PACKAGE.md 추가 예정 섹션: `com.example.thirdtool.Media.*` 4계층 매핑 + Card BC 확장
- 연계 brainstorming: `brainstorming/0.0.2v/generic-domains.md` 후보 5

## 열린 질문 (Open Questions)

- 이미지 OCR 검색을 본 Product v1에 포함할지, `product-search.md` v2로 분리할지 — 인덱스 동기화 결합도 확인 필요
- 카드당 최대 첨부 3장이 적정한지 — 사용자 행동 데이터 확보 후 조정
- 일일 업로드 한도(사용자당 20건/일)가 적정한지 — 남용 vs 정상 사용 경계 측정
- Soft Delete 윈도우 30일이 적정한지 — S3 비용 vs 복원 가능성 트레이드오프
- 미디어 BC를 신규 BC로 둘지, Card BC 안에 둘지 — 현재 안은 Card 안에 첨부 모델 + Media 외부 어댑터. 향후 다른 도메인(LearningMaterial 등)이 미디어 사용 시 분리 검토
- CloudFront Signed URL을 v1에 포함할지 — 모든 카드가 사용자 본인만 접근 가정 → v1은 비공개 URL prefix 난수만으로 충분할 수 있음. 정밀한 위협 모델 확인 필요
- 이미지 EXIF에 포함될 수 있는 위치정보·디바이스 정보 처리 — 자동 strip할지, 사용자에게 노출 동의를 받을지
