-- =============================================================
-- R24. OnFieldBudget 폐기 롤백 (SCHEDULE_EXHAUSTED / MODE_DOWNGRADED → 기존 3값)
--
-- Story-CARD-E2-S2-3, S2-5 rollback
--
-- 주의
--   SCHEDULE_EXHAUSTED는 원래 MAX_VIEW / MAX_DURATION의 합산이라 롤백 시 원본 구분 불가능.
--   근사로 SCHEDULE_EXHAUSTED → MAX_VIEW 매핑한다. MAX_DURATION 이력은 소실 확정.
--   MODE_DOWNGRADED도 이관 전 체계에 없으므로 MAX_VIEW로 축소.
-- =============================================================

ALTER TABLE card_status_history
    DROP CONSTRAINT chk_card_status_history_reason;

UPDATE card_status_history
   SET reason = 'MAX_VIEW'
 WHERE reason IN ('SCHEDULE_EXHAUSTED', 'MODE_DOWNGRADED');

ALTER TABLE card_status_history
    ADD CONSTRAINT chk_card_status_history_reason
        CHECK (reason IS NULL OR reason IN ('MANUAL', 'MAX_VIEW', 'MAX_DURATION'));
