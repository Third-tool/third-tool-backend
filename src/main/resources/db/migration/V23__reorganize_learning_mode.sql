-- =============================================================
-- V23. LearningMode 재편 (MODE_10D/20D/30D → MODE_7D/14D/28D/60D)
--
-- Story-CARD-E1-S1-3 · milestone 0.0.4v PR#1
-- 상위 이슈: workflow/task/fix/brainstorming/version/0.0.2v/issue-21-card-mode-reorganization.md
--
-- 목적
--   기존 3개 값 체계를 4개 값(7/14/28/60) 체계로 이관.
--   mapped_mode 컬럼 + history.from_mode / to_mode 컬럼 세 곳 모두 CHECK 제약 재작성.
--
-- 도메인 매핑
--   MODE_10D  → MODE_7D  (신규 최단 인터벌 = 1/3/7)
--   MODE_20D  → MODE_14D (신규 = 1/3/7/14)
--   MODE_30D  → MODE_28D (신규 = 1/3/7/14/28)
--   신규 MODE_60D는 이관 대상 없음 — 신규 유저부터 채택.
--
-- 검증 SQL (마이그레이션 전후 실행 권장)
--   -- 이관 전:
--   -- SELECT mapped_mode, COUNT(*) FROM user_schedule_config GROUP BY mapped_mode;
--   -- 이관 후:
--   -- SELECT mapped_mode, COUNT(*) FROM user_schedule_config GROUP BY mapped_mode;
--   -- 기대: MODE_10D/20D/30D 각각 0건, MODE_7D/14D/28D로 이관 완료.
-- =============================================================

-- ─── user_schedule_config ────────────────────────────────────

ALTER TABLE user_schedule_config
    DROP CONSTRAINT chk_user_schedule_config_mapped_mode;

UPDATE user_schedule_config SET mapped_mode = 'MODE_7D'  WHERE mapped_mode = 'MODE_10D';
UPDATE user_schedule_config SET mapped_mode = 'MODE_14D' WHERE mapped_mode = 'MODE_20D';
UPDATE user_schedule_config SET mapped_mode = 'MODE_28D' WHERE mapped_mode = 'MODE_30D';

ALTER TABLE user_schedule_config
    ADD CONSTRAINT chk_user_schedule_config_mapped_mode
        CHECK (mapped_mode IN ('MODE_7D', 'MODE_14D', 'MODE_28D', 'MODE_60D'));

-- ─── user_schedule_config_history ────────────────────────────

ALTER TABLE user_schedule_config_history
    DROP CONSTRAINT chk_user_schedule_config_history_from_mode;

ALTER TABLE user_schedule_config_history
    DROP CONSTRAINT chk_user_schedule_config_history_to_mode;

UPDATE user_schedule_config_history SET from_mode = 'MODE_7D'  WHERE from_mode = 'MODE_10D';
UPDATE user_schedule_config_history SET from_mode = 'MODE_14D' WHERE from_mode = 'MODE_20D';
UPDATE user_schedule_config_history SET from_mode = 'MODE_28D' WHERE from_mode = 'MODE_30D';

UPDATE user_schedule_config_history SET to_mode = 'MODE_7D'  WHERE to_mode = 'MODE_10D';
UPDATE user_schedule_config_history SET to_mode = 'MODE_14D' WHERE to_mode = 'MODE_20D';
UPDATE user_schedule_config_history SET to_mode = 'MODE_28D' WHERE to_mode = 'MODE_30D';

ALTER TABLE user_schedule_config_history
    ADD CONSTRAINT chk_user_schedule_config_history_from_mode
        CHECK (from_mode IS NULL OR from_mode IN ('MODE_7D', 'MODE_14D', 'MODE_28D', 'MODE_60D'));

ALTER TABLE user_schedule_config_history
    ADD CONSTRAINT chk_user_schedule_config_history_to_mode
        CHECK (to_mode IN ('MODE_7D', 'MODE_14D', 'MODE_28D', 'MODE_60D'));
