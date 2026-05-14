-- =============================================================
-- V8. deck 테이블에 진행 상태(progress_status) 컬럼 추가
--
-- Story-005-2: Deck 진행 상태 자동 갱신.
-- NOT_STARTED   — Card가 0개인 Deck (초기 상태)
-- IN_PROGRESS   — Card가 1개 이상 + ARCHIVE 상태 아닌 Card 존재
-- COMPLETED     — Card가 1개 이상 + 모든 Card가 ARCHIVE 상태
--
-- 갱신 트리거 (CardCommandService에서 호출):
-- - 첫 Card 추가 시 markInProgress()
-- - Card archive/returnToField 시 recalculateProgressStatus()
-- =============================================================

ALTER TABLE deck
    ADD COLUMN progress_status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED';

ALTER TABLE deck
    ADD CONSTRAINT chk_deck_progress_status
        CHECK (progress_status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED'));

CREATE INDEX idx_deck_progress_status ON deck (progress_status);
