-- V35__daily_learning_batch.sql
-- REV E1 · M5 · Story 1-1 · DailyLearningBatch + DailyCardEntry 테이블 신설
--
-- 근거:
--   · SDD: product-review.md Epic 1 Story 1-1 (line 447~482)
--   · 이슈: issue-24-daily-learning-batch.md
--   · Topology: workflow/topologys/migration-policy/v1-migration-policy.md
--
-- 결정: batch 생성은 lazy (자정 배치가 아니라 사용자 진입 시)
--   자정 close cron만 별도 (Story 1-5).

-- ─── daily_learning_batch ────────────────────────────────
CREATE TABLE daily_learning_batch (
    id                        BIGINT       NOT NULL AUTO_INCREMENT,
    user_id                   BIGINT       NOT NULL,
    batch_date                DATE         NOT NULL,
    generated_at              DATETIME(6)  NOT NULL,
    closed_at                 DATETIME(6)           DEFAULT NULL,
    user_mode_at_generation   VARCHAR(20)  NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_daily_batch_user_date (user_id, batch_date),
    CONSTRAINT chk_daily_batch_mode
        CHECK (user_mode_at_generation IN ('MODE_7D', 'MODE_14D', 'MODE_28D', 'MODE_60D')),
    CONSTRAINT fk_daily_batch_user
        FOREIGN KEY (user_id) REFERENCES user_entity (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_daily_batch_user_date ON daily_learning_batch (user_id, batch_date);

-- ─── daily_card_entry ────────────────────────────────────
CREATE TABLE daily_card_entry (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    batch_id            BIGINT       NOT NULL,
    card_id             BIGINT       NOT NULL,
    card_interval_day   INT          NOT NULL,
    exposed_at          DATETIME(6)  NOT NULL,
    viewed_at           DATETIME(6)           DEFAULT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_daily_entry_batch_card (batch_id, card_id),
    CONSTRAINT fk_daily_entry_batch
        FOREIGN KEY (batch_id) REFERENCES daily_learning_batch (id) ON DELETE CASCADE,
    CONSTRAINT fk_daily_entry_card
        FOREIGN KEY (card_id) REFERENCES card (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_entry_card ON daily_card_entry (card_id);
CREATE INDEX idx_entry_batch_viewed ON daily_card_entry (batch_id, viewed_at);
