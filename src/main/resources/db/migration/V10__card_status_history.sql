-- =============================================================
-- V10. card_status_history 테이블 신설
--
-- product-card.md Epic 1·2 — Card 운영 위치 전환 이력 보존.
-- 도메인 클래스: Card/domain/model/CardStatusHistory.java
--   - id PK
--   - card FK (ON DELETE CASCADE — Card Soft Delete가 아닌 실삭제 시 함께 제거)
--   - fromStatus / toStatus (CardStatus enum, VARCHAR(20))
--   - reason (ArchiveReason enum, NULL 허용 — ON_FIELD 복귀 시는 reason 없음)
--   - changedAt (이력 기록 시각, immutable)
--
-- 이력 규칙 (CardStatusHistoryAppender / CardStatusHistory.of)
--   - fromStatus == toStatus 인 호출은 멱등 no-op (이력 미생성)
--   - toStatus == ARCHIVE 이면 reason 필수 (MANUAL | MAX_VIEW | MAX_DURATION)
--   - toStatus == ON_FIELD 이면 reason 금지
-- =============================================================

CREATE TABLE card_status_history
(
    card_status_history_id BIGINT      NOT NULL AUTO_INCREMENT,
    card_id                BIGINT      NOT NULL,
    from_status            VARCHAR(20) NOT NULL,
    to_status              VARCHAR(20) NOT NULL,
    reason                 VARCHAR(20)          DEFAULT NULL,
    changed_at             DATETIME(6) NOT NULL,

    PRIMARY KEY (card_status_history_id),

    CONSTRAINT fk_card_status_history_card
        FOREIGN KEY (card_id) REFERENCES card (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_card_status_history_from_status
        CHECK (from_status IN ('ON_FIELD', 'ARCHIVE')),

    CONSTRAINT chk_card_status_history_to_status
        CHECK (to_status IN ('ON_FIELD', 'ARCHIVE')),

    CONSTRAINT chk_card_status_history_reason
        CHECK (reason IS NULL OR reason IN ('MANUAL', 'MAX_VIEW', 'MAX_DURATION')),

    -- 멱등 no-op 호출은 이력을 만들지 않으므로 self-transition은 row 자체가 발생하지 않는다.
    CONSTRAINT chk_card_status_history_transition
        CHECK (from_status <> to_status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_card_status_history_card_id    ON card_status_history (card_id);
CREATE INDEX idx_card_status_history_changed_at ON card_status_history (changed_at);
