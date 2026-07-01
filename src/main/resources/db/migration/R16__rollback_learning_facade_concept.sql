-- =============================================================
-- R16__rollback_learning_facade_concept.sql
-- V16 롤백 (수동 참조·복구용, Flyway 자동 실행 대상 아님)
--
-- 본 스크립트는 V16 이 만든 learning_facade_concept 테이블을 제거한다.
--
-- ⚠️ 데이터 소실 경고:
--   Story 1-3 (백필) 이후 실행 시 컨셉 데이터 전량 소실.
--   운영 환경에서는 아래 순서로 안전 검증 필수:
--     1) 컨셉 데이터 백업: SELECT * FROM learning_facade_concept ;
--     2) 소비자 코드가 concept_value 를 참조하지 않음을 확인 (grep)
--     3) 해당 세션의 트랜잭션 격리 확인
--     4) 아래 DROP 실행
--
--   로컬 개발 (H2 in-memory) 환경에서만 무해하게 실행 가능.
-- =============================================================

DROP TABLE IF EXISTS learning_facade_concept;
