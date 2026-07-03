-- =============================================================
-- R30. Card.axisId NOT NULL 승격 롤백
-- Story-LT-E4-S4-3 rollback
-- =============================================================

ALTER TABLE card
    DROP FOREIGN KEY fk_card_axis;
-- R30 롤백은 FK 자체를 제거하므로 CASCADE/RESTRICT 옵션 무관.

ALTER TABLE card
    MODIFY COLUMN axis_id BIGINT NULL;
