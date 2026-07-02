-- =============================================================
-- V19__learning_axis_layer_fk.sql
-- LT Epic 2 Story 2-2/2-3: learning_axis.facade_id → learning_axis.learning_layer_id 재배선
--
-- 3-phase 마이그레이션 (conventions.md §3.8 준수):
--   1) ADD COLUMN nullable
--   2) 기존 axis 백필: facade별 Uncategorized layer 생성 → axis.learning_layer_id 매핑
--   3) NOT NULL 승격 + FK 제약 (ON DELETE RESTRICT — Layer softDelete 시 활성 axis 존재 검증은
--      도메인 LearningLayer.softDelete가 담당하며, DB 제약은 이중 안전망)
--
-- 컬럼 유지: learning_axis.learning_facade_id 는 삭제하지 않는다 (backward compat + 3-phase 원칙).
-- 별도 릴리스에서 facade FK 제거 검토.
-- =============================================================

-- ── Phase 1 ─────────────────────────────────────────────
ALTER TABLE learning_axis
    ADD COLUMN learning_layer_id BIGINT NULL;

-- ── Phase 2-a: facade별 default Uncategorized layer 생성 (idempotent) ──
INSERT INTO learning_layer (learning_facade_id, name, display_order, created_at, updated_at)
SELECT lf.learning_facade_id,
       'Uncategorized',
       1,
       CURRENT_TIMESTAMP(6),
       CURRENT_TIMESTAMP(6)
FROM   learning_facade lf
WHERE  lf.deleted_at IS NULL
  AND  NOT EXISTS (
         SELECT 1
         FROM   learning_layer ll
         WHERE  ll.learning_facade_id = lf.learning_facade_id
           AND  ll.name = 'Uncategorized'
           AND  ll.deleted_at IS NULL
       );

-- ── Phase 2-b: 기존 axis 를 Uncategorized layer 로 매핑 (idempotent) ──
UPDATE learning_axis la
JOIN   learning_layer ll
       ON ll.learning_facade_id = la.learning_facade_id
      AND ll.name = 'Uncategorized'
      AND ll.deleted_at IS NULL
SET    la.learning_layer_id = ll.learning_layer_id
WHERE  la.learning_layer_id IS NULL;

-- ── Phase 3-a: NOT NULL 승격 ─────────────────────────────
ALTER TABLE learning_axis
    MODIFY COLUMN learning_layer_id BIGINT NOT NULL;

-- ── Phase 3-b: FK 제약 ──────────────────────────────────
ALTER TABLE learning_axis
    ADD CONSTRAINT fk_learning_axis_layer
        FOREIGN KEY (learning_layer_id) REFERENCES learning_layer (learning_layer_id)
        ON DELETE RESTRICT;

-- ── Phase 3-c: 조회 인덱스 ──────────────────────────────
CREATE INDEX idx_learning_axis_layer ON learning_axis (learning_layer_id);
