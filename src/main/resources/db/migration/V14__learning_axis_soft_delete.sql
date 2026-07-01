-- =============================================================
-- V14__learning_axis_soft_delete.sql
-- LearningAxis Soft Delete 도입 (Fix — Axis↔Deck 완전 통합, 2026-07-01)
--
-- 배경: LearningAxis는 그동안 Hard Delete만 지원했다 (V2 스키마에 deleted_at 없음).
--   LearningFacade.removeAxis()가 orphanRemoval을 통해 DB row 자체를 삭제해
--   "카드 만들 때 축이 인식 안 되고 화면 나가면 사라진다" 이슈의 최유력 원인이었다.
--   본 마이그레이션은 Card/Deck/Facade와 동일한 Soft Delete 정책으로 통일한다.
--
-- 짝 SDD: workflow/task/fix/sdd/version/0.0.2v/fix-axis-deck-full-integration.md
-- =============================================================

ALTER TABLE learning_axis
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER created_at;

-- 조회 필터( @SQLRestriction("deleted_at IS NULL") )가 인덱스를 활용하도록 보조.
CREATE INDEX idx_learning_axis_deleted ON learning_axis (deleted_at);
