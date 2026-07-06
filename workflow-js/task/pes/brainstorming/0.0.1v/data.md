# Brainstorming · 데이터 (Data) 횡단 카탈로그

> 데이터 — 백업·복원·GDPR·소프트 삭제·audit·분석 웨어하우스·실시간 vs 배치 — 횡단 관심사를 모은다.

---

## [후보 1] 백업 / 복원 전략 (RTO·RPO 명시)

> 데이터 손실 발생 시 얼마나 빨리 / 얼마만큼 잃을 수 있는지를 결정으로 못박는다.

### 배경
- 현재 RDS 사용 가정이지만 RTO(복원 시간) · RPO(허용 손실량) 명시된 곳이 없음.
- `product-infra-ops.md`에 운영 모니터링은 다루지만 백업·복원 리허설은 미명시.
- 1인 운영 환경 — 인시던트 발생 시 매뉴얼 없으면 당황 가능.

### 후보
- **A안**: RDS automated backup 7일 보관 + 일 1회 snapshot + 월 1회 복원 리허설. 장점: AWS 네이티브. 비용: snapshot 스토리지 비용 + 리허설 시간.
- **B안**: A안 + 별도 S3로 mysqldump 일 1회 outbound 백업. 장점: 리전 장애에도 살아남음. 비용: 추가 스토리지·이전 비용.
- **C안**: 현재 상태 유지 (RDS 자동 백업만 신뢰). 비용: 복원 절차 미검증 → 실제 사고 시 RTO 예측 불가.

### 1차 권장
A안. 1인 운영에서 B안은 과함. 리허설은 분기에 1회 정도로 시작.

### PES 승격 경로
- `product-infra-ops.md`에 "데이터 백업 & 복원 리허설" Epic 추가
- ADR 후보: RTO/RPO 결정 + 리허설 주기

### 미해결 질문
- 현재 사용자 N명 / 데이터 M GB 기준 RTO 어느 수준이 적절한가?

---

## [후보 2] GDPR 데이터 내보내기 + 영구 삭제

> 사용자 요청 시 본인 데이터 일괄 내보내기 + 영구 삭제 권리 보장.

### 배경
- FE `MePage`에 "데이터 내보내기" 버튼 UI가 이미 그려져 있음 — BE 미구현.
- Soft delete (`@SQLRestriction(deleted_at IS NULL)`)는 적용되어 있지만 **영구 삭제 절차는 없음** — 사용자가 "완전 삭제"를 요청해도 row가 남아 있음.
- 한국 개인정보보호법·GDPR 모두 보유 기간 명시 + 파기 의무.

### 후보
- **A안**: `POST /api/v1/me/export` (비동기 job) → 24시간 내 메일로 ZIP 링크. `DELETE /api/v1/me` → 30일 보관 후 hard delete 배치. 장점: 표준 패턴. 비용: 비동기 job 인프라 (SQS·worker).
- **B안**: 동기 export (작은 데이터셋만 가능) + 즉시 hard delete. 장점: 인프라 단순. 비용: 데이터 큰 사용자 timeout 위험.
- **C안**: 운영자가 콘솔로 수동 처리. 장점: 코드 0. 비용: 1인 운영 부담 + 응답 시간 보장 불가.

### 1차 권장
A안. 30일 보관 정책은 후보 3(soft-deleted 영구 삭제)과 같은 배치로 통합 가능.

### PES 승격 경로
- 신규 Product: `product-data-rights.md` (export + 영구 삭제 + audit log 회수)
- ADR 후보: 보관 기간 정책 (30일 vs 7일 vs 즉시)

---

## [후보 3] Soft-deleted 데이터 영구 삭제 배치

> `deleted_at` 마킹된 row의 영구 파기 정책을 명시한다.

### 배경
- conventions.md §3.4에 Soft Delete 적용 대상이 명시 (Card·Deck·LearningFacade·LearningMaterial·User).
- **영구 삭제(파기) 시점이 어디에도 정의되어 있지 않음** — 무한히 누적되는 중.
- DB 사이즈 증가 + 후보 2(GDPR)와 결합 시 명확한 정책 필요.

### 후보
- **A안**: 야간 배치 — `deleted_at < now() - 30일` row를 hard delete. 장점: 단순. 비용: 외래키 cascade 영향 검증 필요.
- **B안**: 90일 보관. 장점: 사용자 복원 요청에 더 유리. 비용: 사이즈 큼.
- **C안**: 즉시 hard delete (Soft delete 폐기). 장점: 단순. 비용: 사용자 실수 복구 불가.

### 1차 권장
A안 (30일). 후보 2와 동일 정책으로 통일.

### PES 승격 경로
- 후보 2 (`product-data-rights.md`)에 흡수

---

## [후보 4] 분석 웨어하우스 (학습 패턴 분석)

> 사용자 학습 행동을 운영 DB 부담 없이 분석.

### 배경
- ReviewSession의 카드 노출·archive·복귀 이벤트가 운영 DB에만 존재.
- "1일·3일·7일 soft schedule이 사용자별로 어떻게 작동하는가" 같은 분석 쿼리를 운영 DB에서 돌리면 사용자 응답 시간 영향.
- 포트폴리오 관점에서 "학습 데이터 분석" 사례를 만들 수 있는 자산이지만 인프라 미정.

### 후보
- **A안**: AWS Athena + S3 — Card·CardStatusHistory를 일 1회 parquet export. 장점: 서버리스. 비용: 초기 ETL 작성.
- **B안**: PostgreSQL read replica 또는 별도 분석 DB. 장점: SQL 그대로 사용. 비용: 인프라 비용 큼.
- **C안**: 현재 상태 (필요할 때 운영 DB로 직접 쿼리). 비용: 사용자 응답 영향 위험.

### 1차 권장
A안. 1인 운영 환경에서 비용·운영 부담 가장 작음. v2 이후 본격 분석 시점에 검토.

### PES 승격 경로
- 신규 Product (v2): `product-analytics-warehouse.md`
- 단기적으로는 brainstorming 유지 — 사용자 N명 도달 후 본격화

---

## [후보 5] 데이터 품질 모니터링

> 이상치(예: viewCount > maxView인 ON_FIELD 카드, 컨셉 없는데 자료 있는 facade)를 자동 탐지.

### 배경
- 도메인 불변식은 도메인 메서드 + DB CHECK로 이중 방어 (conventions.md §1.6).
- 그럼에도 마이그레이션 누락 / 코드 버그로 이상 데이터가 생길 가능성 존재.
- 현재 탐지 절차 없음 — 사용자가 신고할 때야 발견.

### 후보
- **A안**: 일 1회 배치 — 도메인 불변식 검증 SQL 세트 실행 → Slack/메일 알림. 장점: 자동. 비용: 검증 SQL 작성 + 운영.
- **B안**: 도메인 메서드 호출 시점 invariant 로그 (WARN). 장점: 실시간. 비용: 성능 영향 + 노이즈.
- **C안**: 현재 상태. 비용: 이상 데이터 늦게 발견.

### 1차 권장
A안. 검증 SQL은 작성 가벼움 + 야간 배치 시간 활용.

### PES 승격 경로
- `product-infra-ops.md` 또는 후보 2(`product-data-rights.md`)에 흡수

---

## [후보 6] Audit log 확장 (LearningFacade 변경 이력)

> 사용자가 자기 학습 지도를 어떻게 진화시켰는지 시간 축으로 회상 가능하게.

### 배경
- `CardStatusHistory`는 이미 존재 (Story 1-1로 도입됨).
- LearningFacade 측 변경 이력은 `TopicRevisionHistory`(주제 단련) + `topic-deletions`(주제 삭제 — Story-003-4)만 존재.
- **축 이름 변경 / 자료 추가·삭제 / 컨셉 변경**은 audit log 없음.
- v1.5+ "되어가는 나" 화면(HomePage의 40% 진행률)을 의미있게 만들려면 시간 축 데이터 필요.

### 후보
- **A안**: `LearningFacadeAuditLog` 신규 — 모든 변경을 단일 테이블 + JSON payload. 장점: 단순. 비용: 쿼리 어려움.
- **B안**: 변경 종류별 분리 테이블 (`AxisHistory` / `ConceptHistory` / `MaterialHistory`). 장점: 쿼리 친화. 비용: 테이블 폭증.
- **C안**: 이벤트 sourcing (이벤트가 1차, 상태는 derived). 장점: 완전한 시간 축. 비용: 시스템 복잡도 큼.

### 1차 권장
A안. v1.5 진입 전 도입 — "되어가는 나" 차트가 의미를 가지려면 시간 축 데이터가 누적되어야 함.

### PES 승격 경로
- `done/product-learningFacade.md`에 Epic 추가 또는 신규 `product-learningFacade-history.md`
- ADR 후보: audit log 패턴 (단일 vs 분리 vs 이벤트 소싱)

---

## [후보 7] 실시간 vs 배치 분기 정책

> "노출 시점 즉시 판정" vs "야간 배치"를 명확한 룰로 정착.

### 배경
- 카드 만료 판정은 두 곳에서 일어남:
  1. `CardExpiryBatchService` (야간 배치 — `feat/031`)
  2. 노출 시점 `Card.recordView()` 호출 후 즉시 archive 판정
- 두 경로의 동작이 모순되지 않는지 통합 테스트로 검증되었으나, 새 도메인 행위 추가 시 어떤 경로로 가야 하는지 **결정 기준이 명문화되지 않음**.
- 향후 사용자 비활성 알림, dailyTarget 리마인드 등 유사 분기 필요해질 가능성.

### 후보
- **A안**: ADR로 결정 트리 명문화 — "사용자 경험에 즉각 영향 = 즉시 / 통계·정합성 보정 = 배치". 장점: 의사결정 시간 단축. 비용: 작성 시간.
- **B안**: 모든 판정 즉시 실행 (배치 폐기). 장점: 일관성. 비용: 노출 빈도 낮은 카드의 만료 시점이 부정확.
- **C안**: 모든 판정 배치 (즉시 폐기). 장점: 일관성. 비용: 사용자가 이미 만료된 카드를 한 번 더 보는 경험.

### 1차 권장
A안. 현재 구조(이중 트리거)가 합리적임을 ADR로 정착시키고, 새 행위 추가 시 동일 룰 적용.

### PES 승격 경로
- ADR 후보 (Product화 불요): "도메인 판정의 실시간/배치 분기 룰"
