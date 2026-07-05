-- R32__rollback_card_deck_id.sql
-- Rollback for V32__card_deck_id_soft_deprecate.sql (LT E5 · M5 · Story 5-2)
--
-- 주의: rollback 시 deck_id NOT NULL 복원 조건 · 기존 카드 모두 deck_id 값 보유 필요.
--   V32 이후 신규 생성 카드가 deck_id NULL이면 rollback 실패 · 사전 백필 필요.

-- ─── Step 1: NOT NULL 복원 (사전 백필 필요) ────────────────
-- 백필: 신규 카드가 있다면 axis_id로부터 deck_id 유도
UPDATE card c
JOIN deck d ON c.axis_id = d.axis_id AND d.deleted = FALSE
SET c.deck_id = d.id
WHERE c.deck_id IS NULL;

-- NOT NULL 승격
ALTER TABLE card MODIFY COLUMN deck_id BIGINT NOT NULL;

-- ─── Step 2: FK 복원 ──────────────────────────────────────
ALTER TABLE card
    ADD CONSTRAINT fk_card_deck
        FOREIGN KEY (deck_id) REFERENCES deck (id);
