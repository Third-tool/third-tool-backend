-- V36 · REV E2 · Story 2-1/2-3/2-6 — ReviewSession 재편 (batch 참조 + finished_at)
--
-- 배경:
-- - PR#2에서 심은 review_session.scope · scope_id · axis_id 컬럼이 supersede됨 (SDD Epic 2 명시)
-- - DailyLearningBatch (V35)가 하루 큐의 원천이 됨 · 세션은 batch 참조로 재편
-- - Story 2-3 자동 finish 정책: finished_at 컬럼 신설

-- 1. 신규 컬럼 (batch 참조 + finished_at)
ALTER TABLE review_session ADD COLUMN batch_id BIGINT NULL;
ALTER TABLE review_session ADD COLUMN finished_at DATETIME(6) NULL;

-- 2. FK · daily_learning_batch 참조
ALTER TABLE review_session
    ADD CONSTRAINT fk_review_session_batch
    FOREIGN KEY (batch_id) REFERENCES daily_learning_batch (id);

-- 3. 인덱스
CREATE INDEX idx_review_session_batch ON review_session (batch_id);
CREATE INDEX idx_review_session_user_finished ON review_session (user_id, finished_at);

-- 4. PR#2 궤적 폐기 (scope · scope_id)
ALTER TABLE review_session DROP COLUMN scope;
ALTER TABLE review_session DROP COLUMN scope_id;

-- 5. Deprecated axis_id (LT-E5-S5-3) 완전 제거 · batch 참조가 대체
ALTER TABLE review_session DROP COLUMN axis_id;

-- 6. 완전 폐기: deck_id (LT-E5 Deck 폐기 궤적 · V33 아카이브 후 잔재 컬럼)
--    V33에서 deck 테이블이 _archived_deck로 RENAME되면서 FK가 breaking 상태였음.
ALTER TABLE review_session DROP COLUMN deck_id;
