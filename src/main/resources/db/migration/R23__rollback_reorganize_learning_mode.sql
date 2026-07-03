-- =============================================================
-- R23. LearningMode 재편 롤백 (MODE_7D/14D/28D/60D → MODE_10D/20D/30D)
--
-- Story-CARD-E1-S1-3 rollback
-- 주의
--   MODE_60D 값은 이관 전 체계에 없으므로 롤백 시 근사(MODE_30D)로 축소한다.
--   축소된 유저는 롤백 후 원래 인터벌 계약을 잃는다 — 사전 안내 필요.
-- =============================================================

-- ─── user_schedule_config ────────────────────────────────────

ALTER TABLE user_schedule_config
    DROP CONSTRAINT chk_user_schedule_config_mapped_mode;

UPDATE user_schedule_config SET mapped_mode = 'MODE_10D' WHERE mapped_mode = 'MODE_7D';
UPDATE user_schedule_config SET mapped_mode = 'MODE_20D' WHERE mapped_mode = 'MODE_14D';
UPDATE user_schedule_config SET mapped_mode = 'MODE_30D' WHERE mapped_mode = 'MODE_28D';
UPDATE user_schedule_config SET mapped_mode = 'MODE_30D' WHERE mapped_mode = 'MODE_60D';

ALTER TABLE user_schedule_config
    ADD CONSTRAINT chk_user_schedule_config_mapped_mode
        CHECK (mapped_mode IN ('MODE_10D', 'MODE_20D', 'MODE_30D'));

-- ─── user_schedule_config_history ────────────────────────────

ALTER TABLE user_schedule_config_history
    DROP CONSTRAINT chk_user_schedule_config_history_from_mode;

ALTER TABLE user_schedule_config_history
    DROP CONSTRAINT chk_user_schedule_config_history_to_mode;

UPDATE user_schedule_config_history SET from_mode = 'MODE_10D' WHERE from_mode = 'MODE_7D';
UPDATE user_schedule_config_history SET from_mode = 'MODE_20D' WHERE from_mode = 'MODE_14D';
UPDATE user_schedule_config_history SET from_mode = 'MODE_30D' WHERE from_mode = 'MODE_28D';
UPDATE user_schedule_config_history SET from_mode = 'MODE_30D' WHERE from_mode = 'MODE_60D';

UPDATE user_schedule_config_history SET to_mode = 'MODE_10D' WHERE to_mode = 'MODE_7D';
UPDATE user_schedule_config_history SET to_mode = 'MODE_20D' WHERE to_mode = 'MODE_14D';
UPDATE user_schedule_config_history SET to_mode = 'MODE_30D' WHERE to_mode = 'MODE_28D';
UPDATE user_schedule_config_history SET to_mode = 'MODE_30D' WHERE to_mode = 'MODE_60D';

ALTER TABLE user_schedule_config_history
    ADD CONSTRAINT chk_user_schedule_config_history_from_mode
        CHECK (from_mode IS NULL OR from_mode IN ('MODE_10D', 'MODE_20D', 'MODE_30D'));

ALTER TABLE user_schedule_config_history
    ADD CONSTRAINT chk_user_schedule_config_history_to_mode
        CHECK (to_mode IN ('MODE_10D', 'MODE_20D', 'MODE_30D'));
