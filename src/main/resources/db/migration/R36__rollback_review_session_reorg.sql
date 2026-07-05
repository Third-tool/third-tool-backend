-- R36 · V36 rollback
-- WARNING: batch 참조로 재편된 review_session 데이터는 복원 불가.
-- 아래는 스키마 형태만 원복. 데이터는 별도 백업 필요.

ALTER TABLE review_session DROP CONSTRAINT fk_review_session_batch;
DROP INDEX idx_review_session_batch ON review_session;
DROP INDEX idx_review_session_user_finished ON review_session;

ALTER TABLE review_session DROP COLUMN batch_id;
ALTER TABLE review_session DROP COLUMN finished_at;

-- PR#2 궤적 원복
ALTER TABLE review_session ADD COLUMN scope VARCHAR(10) NOT NULL DEFAULT 'AXIS';
ALTER TABLE review_session ADD COLUMN scope_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE review_session ADD COLUMN axis_id BIGINT NULL;
ALTER TABLE review_session ADD COLUMN deck_id BIGINT NULL;
