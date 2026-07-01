-- =============================================================
-- R15__rollback_deck_axis_not_null.sql
-- V15 롤백 — Deck axis_id NOT NULL 승격 되돌리기
-- (Fix — Axis↔Deck 완전 통합, BE-Story 2 롤백)
--
-- 짝 SDD: workflow/task/pes/workspectrum/sdd/fix/fix-axis-deck-full-integration.md
-- 짝 정방향: V15__deck_axis_not_null_and_orphan_softdelete.sql
--
-- 주의 (데이터 손실):
--   - V15 Step 2에서 axis_id IS NULL row는 hard delete 되었다 (soft delete 후 잔존 NULL 정리).
--     그 row들은 본 롤백으로 **복원되지 않는다**. R15는 스키마 제약만 되돌린다.
--   - V15 Step 1에서 soft delete 된 row(deleted=true, deleted_at != NULL, axis_id=원값)는
--     그대로 유지된다. 필요 시 수동으로 `UPDATE deck SET deleted=false, deleted_at=NULL WHERE ...`
--     로 복원할 수 있다.
--   - Flyway `undo` 명령(Community Edition 미지원, Teams+ 기능)이나 수동 실행 시 사용.
-- =============================================================

-- ─────────────────────────────────────────────────────────────
-- Step 1: axis_id NOT NULL → nullable 원복
-- ─────────────────────────────────────────────────────────────
ALTER TABLE deck
    MODIFY axis_id BIGINT NULL;

-- ─────────────────────────────────────────────────────────────
-- Step 2: (선택) 소프트 삭제된 고아 Deck 복원 — 주석 처리
-- ─────────────────────────────────────────────────────────────
-- V15 Step 1로 soft delete된 고아 Deck을 조회 가능 상태로 되돌리려면 아래 SQL을 수동 실행.
-- V15에서 축 결합 상태(axis_id 값)로 복원한 사실이 감사 로그에 남아있어야 안전.
--
-- UPDATE deck
--    SET deleted    = FALSE,
--        deleted_at = NULL
--  WHERE axis_id IS NULL
--    AND deleted    = TRUE
--    AND deleted_at IS NOT NULL;
--
-- 주석: FK fk_deck_axis ON DELETE SET NULL 정책은 V15에서 유지되었으므로 별도 원복 불요.
