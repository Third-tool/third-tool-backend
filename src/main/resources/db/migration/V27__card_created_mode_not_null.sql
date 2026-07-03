-- =============================================================
-- V27. Card.createdMode NOT NULL 승격 + CHECK 제약 (3단계 마이그레이션 3단계)
--
-- Story-CARD-E3-S3-1 · milestone 0.0.4v PR#3
--
-- 이 단계 전에 V26 백필로 모든 행이 값을 가져야 한다.
-- CHECK 제약은 LearningMode enum 4개 값만 허용한다.
-- =============================================================

ALTER TABLE card
    MODIFY COLUMN created_mode VARCHAR(10) NOT NULL;

ALTER TABLE card
    ADD CONSTRAINT chk_card_created_mode
        CHECK (created_mode IN ('MODE_7D', 'MODE_14D', 'MODE_28D', 'MODE_60D'));
