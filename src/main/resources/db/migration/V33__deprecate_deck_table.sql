-- V33__deprecate_deck_table.sql
-- Deck BC 폐기 (LT E5 · M5 · Story 5-4) · deck 테이블 RENAME 아카이브
--
-- 근거:
--   · SDD: product-learning-tower.md Epic 5 Story 5-4 (line 2060~2097)
--   · 이슈: issue-13-deck-abolition-axis-absorption.md
--   · Topology: workflow/topologys/migration-policy/v1-migration-policy.md (soft-deprecate · RENAME 아카이브)
--
-- 정책: 즉시 DROP 대신 RENAME → `_archived_deck`. 다음 릴리스 (v0.1.0v 이후)에 완전 DROP.
--   sub_deck은 별도 테이블 없이 self-referencing (parent_deck_id) 이므로 RENAME으로 함께 이관.
--
-- 사전 조건: V32 (card_deck_id_soft_deprecate)로 fk_card_deck 제거 완료.
--   card.deck_id 컬럼은 nullable 상태로 유지 · 다음 릴리스 DROP.

-- ─── Step 1: deck 테이블 → _archived_deck ────────────────
-- self-referencing FK (fk_deck_parent)는 유지됨 · RENAME 시 자동 참조 갱신.
RENAME TABLE deck TO _archived_deck;

-- ─── 검증 SQL ─────────────────────────────────────────────
-- (a) deck 없음 · _archived_deck 존재
-- SHOW TABLES LIKE 'deck';       -- 예상: 결과 없음
-- SHOW TABLES LIKE '_archived_deck';  -- 예상: 1건
--
-- (b) _archived_deck 컬럼·데이터 유지
-- SELECT COUNT(*) FROM _archived_deck;
-- SELECT COUNT(*) FROM _archived_deck WHERE axis_id IS NOT NULL AND deleted = FALSE;
