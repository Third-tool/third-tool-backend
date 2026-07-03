-- =============================================================
-- V26. Card.createdMode 백필 (3단계 마이그레이션 2단계)
--
-- Story-CARD-E3-S3-1 · milestone 0.0.4v PR#3
--
-- 백필 규칙
--   - 사용자 스케줄이 있는 카드: 해당 유저의 user_schedule_config.mapped_mode 사용
--   - 사용자 스케줄이 없는 카드: default 값 'MODE_14D' 사용 (신규 유저 lazy 생성 default와 일치)
--   - Soft-deleted 카드도 백필 대상 (V27 NOT NULL 제약 통과 위함)
--
-- 검증 SQL (마이그레이션 전후 실행 권장)
--   -- 이관 전:
--   -- SELECT COUNT(*) FROM card WHERE created_mode IS NULL;
--   -- 이관 후:
--   -- SELECT COUNT(*) FROM card WHERE created_mode IS NULL;
--   -- 기대: 이관 후 0건.
--   -- SELECT created_mode, COUNT(*) FROM card GROUP BY created_mode;
-- =============================================================

-- 1단계: user_schedule_config가 있는 카드 → 해당 유저의 mapped_mode로 백필
UPDATE card c
   JOIN deck d ON c.deck_id = d.id
   JOIN user_schedule_config u ON d.user_id = u.user_id
   SET c.created_mode = u.mapped_mode
 WHERE c.created_mode IS NULL;

-- 2단계: user_schedule_config가 없는 유저의 카드 → default MODE_14D
UPDATE card
   SET created_mode = 'MODE_14D'
 WHERE created_mode IS NULL;
