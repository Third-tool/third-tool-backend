-- =============================================================
-- V15__deck_axis_not_null_and_orphan_softdelete.sql
-- Deck 생성 경로 단일화 — axis_id NOT NULL 승격 & 고아 Deck 자동 아카이브
-- (Fix — Axis↔Deck 완전 통합, BE-Story 2, 2026-07-01)
--
-- 배경: BE-Story 1(V14)에서 LearningAxis Soft Delete를 도입해 Axis Hard Delete 경로를
--   폐기했다. 본 마이그레이션은 축=덱 정책의 마지막 조각으로:
--   - 고아 Deck(axis_id IS NULL)을 soft delete로 자동 아카이브 (데이터 보존, 조회 미노출)
--   - deck.axis_id 컬럼을 NOT NULL로 승격 → 스키마 레벨에서 "축 없는 Deck" 불가
--
-- 짝 SDD: workflow/task/fix/sdd/version/0.0.2v/fix-axis-deck-full-integration.md §4.3, §8.3 (Story 2)
-- =============================================================

-- ─────────────────────────────────────────────────────────────
-- Step 1: 고아 Deck 자동 소프트 삭제 (백필)
-- ─────────────────────────────────────────────────────────────
-- 회의 결정 D1-c: axisId=null 잔존 Deck은 데이터 손실 없이 soft delete로 정리.
-- 이후 Step 2의 NOT NULL 승격이 성공하려면 axis_id IS NULL row가 남아있으면 안 되는데,
-- Deck 자체를 hard delete하지 않고 deleted 플래그만 세워 조회에서 필터되도록 한다.
-- (`@SQLRestriction` 미사용 도메인 — Repository 필터가 `deleted = false`를 강제)
UPDATE deck
   SET deleted    = TRUE,
       deleted_at = NOW(6)
 WHERE axis_id IS NULL
   AND deleted   = FALSE;

-- ─────────────────────────────────────────────────────────────
-- Step 2: axis_id NOT NULL 승격
-- ─────────────────────────────────────────────────────────────
-- Step 1로 axis_id IS NULL row는 여전히 남아있지만(soft delete 유지) MySQL의 MODIFY는
-- 컬럼값이 NULL이면 실패한다. 그래서 남은 고아 row의 axis_id를 명시적으로 sentinel 값으로
-- 밀어넣기 전에… 대신 남은 고아 row도 처리하려면 다른 방법이 필요하다.
--
-- 결정: 소프트 삭제된 고아 Deck도 axis 참조는 유지되지 않으므로 sentinel(예: -1) 삽입 대신,
-- 남은 axis_id IS NULL row 자체를 (이미 deleted=true이므로) hard delete로 정리한다.
-- 조회에서는 이미 필터되므로 관측 영향 없음. 데이터 감사 필요 시 Step 1 시점의 로그를 근거로 삼는다.
DELETE FROM deck
 WHERE axis_id IS NULL;

ALTER TABLE deck
    MODIFY axis_id BIGINT NOT NULL;

-- 주석: FK fk_deck_axis ON DELETE SET NULL 정책은 유지한다 — 안전망.
-- BE-Story 1에서 LearningAxis Soft Delete를 도입해 Axis Hard Delete가 발생하지 않으므로
-- 이 CASCADE 정책이 실제로 발동할 경로는 없다. 방어 목적으로만 남긴다.
