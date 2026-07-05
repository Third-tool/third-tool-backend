-- R33__rollback_deprecate_deck_table.sql
-- Rollback for V33__deprecate_deck_table.sql (LT E5 · M5 · Story 5-4)

-- ─── Step 1: _archived_deck 테이블 → deck ────────────────
RENAME TABLE _archived_deck TO deck;
