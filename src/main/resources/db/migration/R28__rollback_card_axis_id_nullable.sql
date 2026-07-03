-- =============================================================
-- R28. Card.axisId 컬럼 추가 롤백
-- Story-LT-E4-S4-1 rollback
-- =============================================================

DROP INDEX idx_card_axis ON card;

ALTER TABLE card
    DROP COLUMN axis_id;
