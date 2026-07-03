-- =============================================================
-- V29. Card.axisId 백필 (3단계 마이그레이션 2단계)
--
-- Story-LT-E4-S4-2 · milestone 0.0.4v PR#4
--
-- 백필 소스: card.deck_id → deck.axis_id 경유. Deck은 이미 axis_id NOT NULL.
-- Soft-deleted 카드도 백필 대상 (V30 NOT NULL 제약 통과 위함).
--
-- 검증 SQL (마이그레이션 전후 실행 권장)
--   -- 이관 전:
--   -- SELECT COUNT(*) FROM card WHERE axis_id IS NULL;
--   -- 이관 후:
--   -- SELECT COUNT(*) FROM card WHERE axis_id IS NULL;
--   -- 기대: 이관 후 0건.
--
-- 이관 후 정합성 확인:
--   -- SELECT c.id AS card_id, c.axis_id AS card_axis, d.axis_id AS deck_axis
--   --   FROM card c JOIN deck d ON c.deck_id = d.id
--   --  WHERE c.axis_id != d.axis_id;
--   -- 기대: 0건 (card.axis_id = deck.axis_id 항상 성립).
-- =============================================================

UPDATE card c
   JOIN deck d ON c.deck_id = d.id
   SET c.axis_id = d.axis_id
 WHERE c.axis_id IS NULL;
