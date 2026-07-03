-- =============================================================
-- R27. Card.createdMode NOT NULL 승격 롤백
-- Story-CARD-E3-S3-1 rollback
-- =============================================================

ALTER TABLE card
    DROP CONSTRAINT chk_card_created_mode;

ALTER TABLE card
    MODIFY COLUMN created_mode VARCHAR(10) NULL;
