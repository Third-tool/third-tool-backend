-- =============================================================
-- V16__learning_facade_concept.sql
-- LT Epic 1 Story 1-1: LearningFacade.concept 단일 문자열 → concepts[] 다중화
--
-- learning_facade_concept 자식 테이블 신설
-- - facade_id FK + value + display_order (1-based, CHECK ≥ 0)
-- - UNIQUE (facade_id, value) — 사용자 자산 내 중복 컨셉 방지
-- - FK ON DELETE CASCADE — facade 하드삭제 경로가 없으나 안전망 유지 (V15 관행)
--
-- 백필 (기존 learning_facade.concept → learning_facade_concept)은 Story 1-3에서 수행.
-- Entity/Repository는 Story 1-2에서 도입. 본 Story는 스키마만.
-- =============================================================

CREATE TABLE learning_facade_concept
(
    learning_facade_concept_id BIGINT       NOT NULL AUTO_INCREMENT,
    learning_facade_id         BIGINT       NOT NULL,
    value                      VARCHAR(100) NOT NULL,
    display_order              INT          NOT NULL,
    created_at                 DATETIME(6)  NOT NULL,

    PRIMARY KEY (learning_facade_concept_id),
    CONSTRAINT uk_learning_facade_concept
        UNIQUE (learning_facade_id, value),
    CONSTRAINT fk_learning_facade_concept_facade
        FOREIGN KEY (learning_facade_id) REFERENCES learning_facade (learning_facade_id)
            ON DELETE CASCADE,
    CONSTRAINT chk_learning_facade_concept_order
        CHECK (display_order >= 0),
    INDEX idx_learning_facade_concept_facade_order (learning_facade_id, display_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
