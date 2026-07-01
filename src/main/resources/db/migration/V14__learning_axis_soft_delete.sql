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

-- ─────────────────────────────────────────────────────────────
-- UNIQUE 제약 재정의 (facade_id, name) → (facade_id, name, deleted_at)
-- ─────────────────────────────────────────────────────────────
-- V2에서 정의한 uk_learning_axis_facade_name (facade_id, name)은 소프트 삭제된 축의
-- name도 여전히 점유해 활성 축 신규 생성 시 DataIntegrityViolationException을 유발한다.
-- MySQL은 NULL을 UNIQUE 검사에서 서로 다른 값으로 취급하므로, deleted_at을 조합에
-- 포함시키면:
--   - 활성 축(deleted_at IS NULL)끼리는 여전히 (facade_id, name) 유일 보장
--   - 소프트 삭제된 축(deleted_at != NULL)은 서로 다른 시각을 가지므로 name 중복 허용
--   - 활성 축 name과 이미 소프트 삭제된 축 name이 겹쳐도 삽입 성공
-- (부분 인덱스 미지원 회피 — 3-column composite unique로 등가 효과.)
ALTER TABLE learning_axis
    DROP INDEX uk_learning_axis_facade_name,
    ADD CONSTRAINT uk_learning_axis_facade_name
        UNIQUE (learning_facade_id, name, deleted_at);
