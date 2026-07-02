-- =============================================================
-- R18__rollback_learning_layer.sql
-- Story-LT-E2-S1 rollback: learning_layer 테이블 폐기.
--
-- ⚠️ V19에서 learning_axis.learning_layer_id FK가 추가되면
-- 이 rollback을 실행하기 전에 V19의 R19를 먼저 실행해야 한다 (FK 무결성).
-- =============================================================

DROP TABLE IF EXISTS learning_layer;
