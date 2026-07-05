-- V31__axis_absorbs_deck.sql
-- Deck BC 폐기 (LT E5 · M5 · Story 5-1) · Deck 6 책임 필드를 LearningAxis로 흡수
--
-- 근거:
--   · SDD: product-learning-tower.md Epic 5 Story 5-1 (line 1917~1966)
--   · 이슈: issue-13-deck-abolition-axis-absorption.md
--   · Topology: workflow/topologys/migration-policy/v1-migration-policy.md
--
-- 결정 정책 (모두 SDD 우선):
--   1. mode enum 재정의: DeckMode { ON_FIELD, ARCHIVE } → AxisLearningMode { STUDY, REVIEW }
--      · 기존 데이터 백필 스킵 (모든 기존 axis는 default STUDY) — SDD 재정의 근거 · 스키마 fresh start
--   2. 계층 폐기: parentDeck/depth/subDecks 이관 안 함 (Layer가 유일 상위)
--   3. scoring_algorithm_type: V1 legacy · Java 참조 zero · 이관 안 함 (_archived_deck에만 잔존)
--   4. Card·Review 참조: Story 5-2·5-3에서 별도 처리 (본 V파일은 스키마만)
--
-- 3-phase 불필요: 모든 신규 컬럼이 default 또는 nullable → 즉시 NOT NULL 가능.

-- ─── Step 1: 6 컬럼 추가 ──────────────────────────────────
ALTER TABLE learning_axis
    ADD COLUMN progress_status      VARCHAR(20)  NOT NULL DEFAULT 'NOT_STARTED' AFTER display_order,
    ADD COLUMN mode                 VARCHAR(20)  NOT NULL DEFAULT 'STUDY'       AFTER progress_status,
    ADD COLUMN last_accessed_at     DATETIME(6)  NULL                            AFTER mode,
    ADD COLUMN learning_material_id BIGINT       NULL                            AFTER last_accessed_at,
    ADD COLUMN on_library           TINYINT(1)   NOT NULL DEFAULT 0              AFTER learning_material_id,
    ADD COLUMN published_at         DATETIME(6)  NULL                            AFTER on_library;

-- ─── Step 2: CHECK 제약 (ADR002 · Enum 저장 방식) ──────────
ALTER TABLE learning_axis
    ADD CONSTRAINT chk_learning_axis_progress_status
        CHECK (progress_status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED'));

ALTER TABLE learning_axis
    ADD CONSTRAINT chk_learning_axis_mode
        CHECK (mode IN ('STUDY', 'REVIEW'));

-- ─── Step 3: FK (learning_material_id · ON DELETE SET NULL) ──
-- 자료 삭제 시 자동 해제 · axis는 유지 (Deck.markMaterialDeleted() 계승)
ALTER TABLE learning_axis
    ADD CONSTRAINT fk_learning_axis_material
        FOREIGN KEY (learning_material_id) REFERENCES learning_material (learning_material_id)
        ON DELETE SET NULL;

-- ─── Step 4: 인덱스 ───────────────────────────────────────
-- on_library: 라이브러리 공개 axis 목록 조회 (사용 빈도 높음 · 커버링 후보)
CREATE INDEX idx_learning_axis_on_library ON learning_axis (on_library);
-- progress_status: 상태별 필터 조회 (Epic 6 대시보드 소스)
CREATE INDEX idx_learning_axis_progress_status ON learning_axis (progress_status);

-- ─── Step 5: deck → axis 데이터 백필 (mode 제외 · SDD 재정의 근거) ──
--
-- 이관 컬럼 (5개): progress_status · last_accessed_at · learning_material_id · on_library · published_at
-- 스킵 컬럼: mode (SDD 재정의로 fresh start) · scoring_algorithm_type (V1 legacy)
--
-- Deck과 Axis는 axis_id (V15 이후 NOT NULL)로 연결. 활성 Deck만 이관 대상.
UPDATE learning_axis la
INNER JOIN deck d ON la.learning_axis_id = d.axis_id AND d.deleted = FALSE
SET
    la.progress_status      = d.progress_status,
    la.last_accessed_at     = d.last_accessed,
    la.learning_material_id = d.learning_material_id,
    la.on_library           = d.on_library,
    la.published_at         = d.published_at
WHERE la.deleted_at IS NULL;

-- ─── 검증 SQL (수동 실행 · migration-policy topology 준수) ──
-- 마이그레이션 후 다음 SQL로 백필 정합 확인:
--
-- (a) 백필된 axis 수 vs 활성 deck 수 (일치해야 함)
-- SELECT
--     (SELECT COUNT(*) FROM deck WHERE deleted = FALSE) AS active_deck_count,
--     (SELECT COUNT(*) FROM learning_axis la
--        INNER JOIN deck d ON la.learning_axis_id = d.axis_id AND d.deleted = FALSE
--      WHERE la.deleted_at IS NULL) AS backfilled_axis_count;
--
-- (b) progress_status 분포 (모두 유효 enum 값)
-- SELECT progress_status, COUNT(*) FROM learning_axis WHERE deleted_at IS NULL GROUP BY progress_status;
--
-- (c) mode 분포 (모두 STUDY · SDD 재정의)
-- SELECT mode, COUNT(*) FROM learning_axis WHERE deleted_at IS NULL GROUP BY mode;
