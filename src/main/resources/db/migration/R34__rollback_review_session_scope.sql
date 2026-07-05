-- R34__rollback_review_session_scope.sql
-- Rollback for V34 (LT E6 · M5 · Story 6-1)

-- ─── Step 1: 인덱스 삭제 ──────────────────────────────────
DROP INDEX idx_review_session_scope ON review_session;

-- ─── Step 2: axis_id NOT NULL 복원 (LAYER 세션 삭제 후) ──
DELETE FROM review_session WHERE axis_id IS NULL;
ALTER TABLE review_session MODIFY COLUMN axis_id BIGINT NOT NULL;

-- ─── Step 3: CHECK 제약 삭제 ─────────────────────────────
ALTER TABLE review_session DROP CONSTRAINT chk_review_session_scope;

-- ─── Step 4: 컬럼 삭제 ────────────────────────────────────
ALTER TABLE review_session DROP COLUMN scope_id;
ALTER TABLE review_session DROP COLUMN scope;
