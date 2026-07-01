-- =============================================================
-- R14__rollback_learning_axis_soft_delete.sql
-- V14 롤백 — LearningAxis Soft Delete 도입 되돌리기
-- (Fix — Axis↔Deck 완전 통합, BE-Story 1 롤백)
--
-- 짝 SDD: workflow/task/pes/workspectrum/sdd/fix/fix-axis-deck-full-integration.md
-- 짝 정방향: V14__learning_axis_soft_delete.sql
--
-- 주의:
--   - Flyway `undo` 명령(Community Edition 미지원, Teams+ 기능)이나 수동 실행 시 사용.
--   - Soft Delete로 아카이브된 축(deleted_at != NULL)의 정보는 컬럼 drop 시 유실된다.
--     실행 전 감사 목적으로 `SELECT * FROM learning_axis WHERE deleted_at IS NOT NULL` 백업 권장.
-- =============================================================

-- ─────────────────────────────────────────────────────────────
-- Step 1: UNIQUE 제약 원복 — (facade_id, name, deleted_at) → (facade_id, name)
-- ─────────────────────────────────────────────────────────────
-- 롤백 전에 소프트 삭제된 축을 hard delete로 정리해야 (facade_id, name) UNIQUE 재적용이
-- 활성 축과 충돌하지 않는다. 활성 축과 name이 겹치는 소프트 삭제된 row가 있으면 UNIQUE 위반.
DELETE FROM learning_axis WHERE deleted_at IS NOT NULL;

ALTER TABLE learning_axis
    DROP INDEX uk_learning_axis_facade_name,
    ADD CONSTRAINT uk_learning_axis_facade_name
        UNIQUE (learning_facade_id, name);

-- ─────────────────────────────────────────────────────────────
-- Step 2: 인덱스·컬럼 제거
-- ─────────────────────────────────────────────────────────────
DROP INDEX idx_learning_axis_deleted ON learning_axis;

ALTER TABLE learning_axis
    DROP COLUMN deleted_at;
