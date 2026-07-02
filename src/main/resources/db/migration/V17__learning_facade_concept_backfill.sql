-- =============================================================
-- V17__learning_facade_concept_backfill.sql
-- LT Epic 1 Story 1-3: 단수 learning_facade.concept → learning_facade_concept 백필
--
-- 기존 facade의 단수 concept 값을 자식 테이블 첫 항목(displayOrder=1)으로 이전.
-- - concept IS NULL / TRIM(concept) = '' 는 skip
-- - 이미 자식 row가 존재하는 facade는 skip (idempotent)
-- - learning_facade.concept 컬럼은 유지 (컬럼 DROP은 별도 릴리스)
--
-- 안전성: 자식 테이블 UNIQUE (learning_facade_id, concept_value)로 재실행 시 실패 없이
-- 조용히 skip (NOT EXISTS 서브쿼리로 명시적 idempotency).
--
-- conventions.md §3.8: 새 V 버전 파일만 추가 — V16 수정 금지.
-- =============================================================

INSERT INTO learning_facade_concept (learning_facade_id, concept_value, display_order, created_at)
SELECT lf.learning_facade_id,
       TRIM(lf.concept),
       1,
       CURRENT_TIMESTAMP(6)
FROM   learning_facade lf
WHERE  lf.concept IS NOT NULL
  AND  TRIM(lf.concept) <> ''
  AND  NOT EXISTS (
         SELECT 1
         FROM   learning_facade_concept lfc
         WHERE  lfc.learning_facade_id = lf.learning_facade_id
       );
