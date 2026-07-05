-- V34__review_session_scope.sql
-- LT E6 · M5 · Story 6-1 · ReviewSession 이중 스코프 확장 (AXIS/LAYER)
--
-- 근거:
--   · SDD: product-learning-tower.md Epic 6 Story 6-1 (line 2184~2238)
--   · 이슈: issue-14-review-strategy-axis-layer-scope.md
--   · 궤적 관리: M5 PR#4 (Review E2)가 issue-25 supersede로 폐기 예정 (cross-layer 짬뽕 큐)
--     · milestone.md § M5 리스크 명시: V34 심은 scope·scopeId는 V37이 폐기 예정
--
-- 정책: axis_id는 nullable로 완화 (LAYER 스코프 세션은 axis_id=null).
--   기존 세션은 백필로 scope=AXIS, scope_id=axis_id 세팅.

-- ─── Step 1: scope · scope_id 컬럼 추가 (default로 즉시 NOT NULL 가능) ─
ALTER TABLE review_session
    ADD COLUMN scope    VARCHAR(10) NOT NULL DEFAULT 'AXIS' AFTER axis_id,
    ADD COLUMN scope_id BIGINT      NOT NULL DEFAULT 0      AFTER scope;

-- ─── Step 2: CHECK 제약 (ADR002) ─────────────────────────
ALTER TABLE review_session
    ADD CONSTRAINT chk_review_session_scope
        CHECK (scope IN ('AXIS', 'LAYER'));

-- ─── Step 3: 기존 세션 백필 (scope=AXIS · scope_id=axis_id) ─
UPDATE review_session
   SET scope    = 'AXIS',
       scope_id = axis_id
 WHERE axis_id IS NOT NULL AND scope_id = 0;

-- ─── Step 4: axis_id NULL 허용 (LAYER 스코프 대비) ───────
ALTER TABLE review_session
    MODIFY COLUMN axis_id BIGINT NULL;

-- ─── Step 5: 인덱스 (scope, scope_id) 커버링 조회 ────────
CREATE INDEX idx_review_session_scope ON review_session (scope, scope_id);

-- ─── 검증 SQL ─────────────────────────────────────────────
-- (a) 컬럼 존재
-- SHOW COLUMNS FROM review_session WHERE Field IN ('scope', 'scope_id', 'axis_id');
--
-- (b) 백필 정합 (기존 AXIS 세션은 scope_id == axis_id)
-- SELECT COUNT(*) FROM review_session WHERE scope = 'AXIS' AND scope_id != axis_id;
-- 예상: 0
--
-- (c) 인덱스
-- SHOW INDEX FROM review_session WHERE Key_name = 'idx_review_session_scope';
