-- =============================================================
-- V4__axis_topic_revision_count.sql
-- Epic-003 Story-003-3: 주제 이름 수정 누적 횟수(revisionCount) 컬럼 추가
-- - AxisTopic.updateName()이 실제 변경 시 +1 (동일 값·설명 단독 수정 미증가)
-- - REFINEMENT_THRESHOLD(=3) 도달 시 isRefinementSuggested = true
-- - db-conventions.md §8 정합: 컬럼 추가 + NOT NULL 전환은 3단계
-- =============================================================

-- 1. NULL 허용 + DEFAULT 0으로 컬럼 추가
ALTER TABLE axis_topic
    ADD COLUMN revision_count INT DEFAULT 0;

-- 2. 기존 행 백필 (DEFAULT가 처리하지만 명시적 보장)
UPDATE axis_topic SET revision_count = 0 WHERE revision_count IS NULL;

-- 3. NOT NULL 전환
ALTER TABLE axis_topic
    MODIFY COLUMN revision_count INT NOT NULL DEFAULT 0;
