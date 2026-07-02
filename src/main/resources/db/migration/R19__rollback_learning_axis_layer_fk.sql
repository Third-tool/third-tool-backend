-- =============================================================
-- R19__rollback_learning_axis_layer_fk.sql
-- Story-LT-E2-S2/S3 rollback.
--
-- ⚠️ 순서 준수: index 삭제 → FK 삭제 → NOT NULL 되돌리기 → 컬럼 삭제.
-- 백필된 Uncategorized layer 는 R18(learning_layer 폐기)에서 처리하지 않고 남긴다 —
-- 데이터 손실 방지. 필요 시 별도 스크립트로 정리.
-- =============================================================

DROP INDEX idx_learning_axis_layer ON learning_axis;

ALTER TABLE learning_axis
    DROP FOREIGN KEY fk_learning_axis_layer;

ALTER TABLE learning_axis
    MODIFY COLUMN learning_layer_id BIGINT NULL;

ALTER TABLE learning_axis
    DROP COLUMN learning_layer_id;
