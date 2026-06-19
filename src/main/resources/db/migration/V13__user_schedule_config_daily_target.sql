-- =============================================================
-- V13. user_schedule_config 테이블에 daily_target 컬럼 추가
--
-- product-card.md Epic 6 Story 6-2 — 유저별 하루 학습 목표 카드 수.
-- 동적 state 비율 추천(Story 6-2)이 dailyTarget × state 비례로 배분한다.
--
-- 기본값 20장 (Open Question 4 v1 잠정) — 첫 진입 사용자도 즉시 학습 가능하도록
-- 운영 데이터 누적 후 재조정.
--
-- CHECK daily_target >= 1 (도메인은 1 이상 강제).
-- 기존 row는 DEFAULT 20으로 자동 백필.
-- =============================================================

ALTER TABLE user_schedule_config
    ADD COLUMN daily_target INT NOT NULL DEFAULT 20 AFTER mapped_mode;

ALTER TABLE user_schedule_config
    ADD CONSTRAINT chk_user_schedule_config_daily_target
        CHECK (daily_target >= 1);
