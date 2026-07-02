-- =============================================================
-- R21__rollback_axis_selection.sql
-- V21 롤백: axis_selection 컨테이너 테이블 제거.
-- 주의: 자식 axis_selection_node가 있으면 먼저 R22 실행 필요.
-- =============================================================

DROP TABLE IF EXISTS axis_selection;
