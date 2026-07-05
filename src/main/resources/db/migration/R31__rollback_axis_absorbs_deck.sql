-- R31__rollback_axis_absorbs_deck.sql
-- Rollback for V31__axis_absorbs_deck.sql (LT E5 · M5 · Story 5-1)
--
-- 안전 특성: Deck 테이블·데이터가 여전히 존재하므로 axis 흡수 컬럼만 제거하면 롤백 가능.
-- 데이터 손실 위험: 마이그레이션 이후 axis 흡수 필드에 신규 write가 있었다면 소실.

-- ─── Step 1: 인덱스 삭제 ──────────────────────────────────
DROP INDEX idx_learning_axis_on_library ON learning_axis;
DROP INDEX idx_learning_axis_progress_status ON learning_axis;

-- ─── Step 2: FK 삭제 ─────────────────────────────────────
ALTER TABLE learning_axis DROP FOREIGN KEY fk_learning_axis_material;

-- ─── Step 3: CHECK 제약 삭제 ─────────────────────────────
ALTER TABLE learning_axis DROP CONSTRAINT chk_learning_axis_progress_status;
ALTER TABLE learning_axis DROP CONSTRAINT chk_learning_axis_mode;

-- ─── Step 4: 컬럼 삭제 (역순) ─────────────────────────────
ALTER TABLE learning_axis
    DROP COLUMN published_at,
    DROP COLUMN on_library,
    DROP COLUMN learning_material_id,
    DROP COLUMN last_accessed_at,
    DROP COLUMN mode,
    DROP COLUMN progress_status;
