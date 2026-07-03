-- =============================================================
-- V28. Card.axisId 컬럼 추가 (3단계 마이그레이션 1단계 · nullable)
--
-- Story-LT-E4-S4-1 · milestone 0.0.4v PR#4
-- 상위 이슈: workflow/task/fix/brainstorming/version/0.0.2v/issue-07-card-axis-direct-mapping.md
--
-- 목적
--   Card가 Deck.axis_id를 경유해 축을 간접 참조하던 구조에서 축을 직접 소유하도록 재편.
--   Deck 폐기 (M5 Epic 5 예정) 대비 사전 인프라 정착.
--
-- 이 단계는 NULL 컬럼 + 인덱스만 열어둔다. 백필은 V29, NOT NULL 승격은 V30에서 수행.
--
-- 컬럼
--   card.axis_id BIGINT NULL
--   INDEX idx_card_axis (axis_id)
-- =============================================================

ALTER TABLE card
    ADD COLUMN axis_id BIGINT NULL AFTER deck_id;

CREATE INDEX idx_card_axis ON card (axis_id);
