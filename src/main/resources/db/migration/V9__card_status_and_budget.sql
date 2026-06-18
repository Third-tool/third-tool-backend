-- =============================================================
-- V9. card 테이블 상태/예산 컬럼 추가
--
-- product-card.md Epic 1 Story 1-1 (Card ON_FIELD/Archive 상태 정의 및 DB 반영)
-- product-card.md Epic 2 Story 2-1 (ON_FIELD 진입 시점 및 노출 횟수 기록)
--
-- 추가 컬럼
--   - status            VARCHAR(20)  — ON_FIELD | ARCHIVE 운영 위치
--   - entered_field_at  DATETIME(6)  — 현재 ON_FIELD 구간 진입 시각
--   - view_count        INT          — 현재 구간 노출 횟수
--   - last_viewed_at    DATETIME(6)  — 마지막 ReviewSession 노출 시각 (Soft Schedule)
--   - deleted_at        DATETIME(6)  — Soft Delete 시각 (도메인엔 있으나 V1 누락분 보강)
--
-- 인덱스
--   - idx_card_status          — Archive 목록 조회·만료 배치 후보 검색
--   - idx_card_last_viewed_at  — Soft Schedule 간격 필터 (Epic 3)
--
-- 백필
--   - 기존 row는 모두 ON_FIELD 상태로 가정.
--   - entered_field_at은 created_date로 백필 (현 구간 시작 시각을 알 수 없으므로 생성 시각으로 근사).
--   - view_count는 DEFAULT 0으로 충분.
-- =============================================================

ALTER TABLE card
    ADD COLUMN status            VARCHAR(20)  NOT NULL DEFAULT 'ON_FIELD' AFTER summary_value,
    ADD COLUMN entered_field_at  DATETIME(6)           DEFAULT NULL       AFTER status,
    ADD COLUMN view_count        INT          NOT NULL DEFAULT 0          AFTER entered_field_at,
    ADD COLUMN last_viewed_at    DATETIME(6)           DEFAULT NULL       AFTER view_count,
    ADD COLUMN deleted_at        DATETIME(6)           DEFAULT NULL       AFTER deleted;

ALTER TABLE card
    ADD CONSTRAINT chk_card_status
        CHECK (status IN ('ON_FIELD', 'ARCHIVE'));

ALTER TABLE card
    ADD CONSTRAINT chk_card_view_count_nonneg
        CHECK (view_count >= 0);

-- 기존 row 백필: entered_field_at <- created_date
UPDATE card
SET entered_field_at = created_date
WHERE entered_field_at IS NULL;

CREATE INDEX idx_card_status         ON card (status);
CREATE INDEX idx_card_last_viewed_at ON card (last_viewed_at);
