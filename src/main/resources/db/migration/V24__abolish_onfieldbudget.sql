-- =============================================================
-- V24. OnFieldBudget 폐기 + ArchiveReason 재편
--
-- Story-CARD-E2-S2-3, S2-5 · milestone 0.0.4v PR#2
-- 상위 이슈: workflow/task/fix/brainstorming/version/0.0.2v/issue-22-onfieldbudget-abolish.md
--
-- 목적
--   이중 게이트(maxView + maxDuration) 개념 폐기. ArchiveReason enum을 3개 값
--   (MANUAL / SCHEDULE_EXHAUSTED / MODE_DOWNGRADED)으로 재정의.
--
-- 데이터 이관 매핑
--   MAX_VIEW     → SCHEDULE_EXHAUSTED (원본 lifecycle 정보 소실)
--   MAX_DURATION → SCHEDULE_EXHAUSTED
--   MANUAL       → MANUAL (변경 없음)
--   MODE_DOWNGRADED: 신규 값 — 이관 대상 없음
--
-- 검증 SQL (마이그레이션 전후 실행 권장)
--   -- 이관 전:
--   -- SELECT reason, COUNT(*) FROM card_status_history WHERE reason IS NOT NULL GROUP BY reason;
--   -- 이관 후:
--   -- SELECT reason, COUNT(*) FROM card_status_history WHERE reason IS NOT NULL GROUP BY reason;
--   -- 기대: MAX_VIEW / MAX_DURATION 각각 0건, SCHEDULE_EXHAUSTED에 합산되어 있음.
--
-- Card 테이블 자체는 maxView·maxDuration 컬럼 부재 (Budget은 UserSchedule에서 파생됨).
-- 따라서 컬럼 삭제는 없고 CHECK 재작성 + 이력 UPDATE만 수행한다.
-- =============================================================

-- ─── card_status_history ─────────────────────────────────────

ALTER TABLE card_status_history
    DROP CONSTRAINT chk_card_status_history_reason;

UPDATE card_status_history
   SET reason = 'SCHEDULE_EXHAUSTED'
 WHERE reason IN ('MAX_VIEW', 'MAX_DURATION');

ALTER TABLE card_status_history
    ADD CONSTRAINT chk_card_status_history_reason
        CHECK (reason IS NULL OR reason IN ('MANUAL', 'SCHEDULE_EXHAUSTED', 'MODE_DOWNGRADED'));
