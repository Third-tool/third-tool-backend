-- V32__card_deck_id_soft_deprecate.sql
-- Deck BC 폐기 (LT E5 · M5 · Story 5-2) · card.deck_id FK·NOT NULL 제약 완화
--
-- 근거:
--   · SDD: product-learning-tower.md Epic 5 Story 5-2 (line 1969~2007)
--   · 이슈: issue-13-deck-abolition-axis-absorption.md
--   · Topology: workflow/topologys/migration-policy/v1-migration-policy.md (soft-deprecate 원칙)
--
-- 정책 결정: SDD는 "card.deck_id 완전 폐기 (DROP FOREIGN KEY + DROP COLUMN)" 이지만
--   실제 코드에서 Card.deck @ManyToOne 필드·getDeck()·다수 JPQL이 `c.deck` 참조 중.
--   즉시 DROP 시 대량 컴파일 오류 발생 · 순차 이관 필요.
--
--   본 V32는 **soft-deprecate**: FK 제거 + NOT NULL 완화 (nullable).
--   컬럼은 유지 · 다음 릴리스 (v0.1.0v 이후)에 완전 DROP 예정.
--
--   migration-policy topology §Boundaries "DROP 경계" 준수:
--     "컬럼 즉시 DROP 금지. soft-deprecate → 다음 릴리스에 별도 마이그레이션으로 DROP"

-- ─── Step 1: FK 제약 삭제 ─────────────────────────────────
-- deck 테이블 RENAME (V33 Story 5-4) 이전에 FK 삭제 필요.
ALTER TABLE card DROP FOREIGN KEY fk_card_deck;

-- ─── Step 2: NOT NULL 완화 ────────────────────────────────
-- Card 도메인이 axis_id를 직접 참조하는 이관 기간 동안 deck_id는 nullable.
-- 신규 카드 생성 시 axisId만으로 성공 (card.deck 필드는 nullable 상태로 남음).
ALTER TABLE card MODIFY COLUMN deck_id BIGINT NULL;

-- ─── 검증 SQL ─────────────────────────────────────────────
-- 마이그레이션 후 다음 SQL로 정합 확인:
--
-- (a) FK 제거 확인
-- SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS
-- WHERE TABLE_NAME = 'card' AND CONSTRAINT_TYPE = 'FOREIGN KEY';
-- 예상: fk_card_deck 없음 · fk_card_axis 유지
--
-- (b) 컬럼 nullable 확인
-- SELECT COLUMN_NAME, IS_NULLABLE FROM information_schema.COLUMNS
-- WHERE TABLE_NAME = 'card' AND COLUMN_NAME IN ('deck_id', 'axis_id');
-- 예상: deck_id = 'YES' · axis_id = 'NO'
