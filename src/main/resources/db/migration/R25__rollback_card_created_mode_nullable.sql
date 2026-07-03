-- =============================================================
-- R25. Card.createdMode 컬럼 추가 롤백
-- Story-CARD-E3-S3-1 rollback
-- =============================================================

ALTER TABLE card
    DROP COLUMN created_mode;
