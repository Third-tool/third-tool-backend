-- =============================================================
-- V25. Card.createdMode 컬럼 추가 (3단계 마이그레이션 1단계 · nullable)
--
-- Story-CARD-E3-S3-1 · milestone 0.0.4v PR#3
-- 상위 이슈: workflow/task/fix/brainstorming/version/0.0.2v/issue-23-card-created-mode-hybrid.md
--
-- 목적
--   각 카드의 생성 시점 LearningMode를 스냅샷 보관하기 위한 컬럼 추가.
--   기존 카드는 다음 마이그레이션(V26)에서 default MODE_14D로 백필된다.
--
-- 컬럼
--   card.created_mode VARCHAR(10) NULL — LearningMode enum (@Enumerated STRING)
--   허용 값: MODE_7D / MODE_14D / MODE_28D / MODE_60D
--
-- 이 단계는 백필 여지가 있는 nullable 상태로 열어두는 것이 전부다.
-- 무결성 CHECK는 V27에서 NOT NULL 승격 시점에 추가한다.
-- =============================================================

ALTER TABLE card
    ADD COLUMN created_mode VARCHAR(10) NULL AFTER entered_field_at;
