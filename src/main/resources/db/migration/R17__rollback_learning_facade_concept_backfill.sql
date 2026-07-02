-- =============================================================
-- R17__rollback_learning_facade_concept_backfill.sql
-- Story-LT-E1-S3 rollback: V17 백필로 생성된 첫 항목(displayOrder=1)을
-- concept 값과 일치하는 경우에만 되돌린다.
--
-- ⚠️ Story-LT-E1-S3 이후 사용자가 addConcept로 추가한 항목은 삭제하지 않는다 —
-- concept_value가 단수 concept와 일치하고 displayOrder=1이며,
-- 해당 facade에 자식 row가 정확히 1개인 경우에만 rollback 대상.
-- =============================================================

DELETE lfc
FROM   learning_facade_concept lfc
JOIN   learning_facade lf ON lf.learning_facade_id = lfc.learning_facade_id
WHERE  lfc.display_order = 1
  AND  lfc.concept_value = TRIM(lf.concept)
  AND  (SELECT COUNT(*)
        FROM   learning_facade_concept x
        WHERE  x.learning_facade_id = lfc.learning_facade_id) = 1;
