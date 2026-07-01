-- =============================================================
-- V16__learning_facade_concept.sql
-- LT Epic 1 Story 1-1: LearningFacade.concept 단일 문자열 → concepts[] 다중화
--
-- learning_facade_concept 자식 테이블 신설
-- - facade_id FK + concept_value + display_order (1-based, CHECK ≥ 0 안전망)
-- - UNIQUE (facade_id, concept_value) — 사용자 자산 내 컨셉 중복을 스키마 레벨에서 차단
-- - FK ON DELETE CASCADE — LearningFacade 는 soft delete 도메인이라 실제 발동 경로 없음.
--   V2 topic_material FK 관행 그대로 채택 (안전망 유지). RESTRICT 로의 강제는
--   LearningFacade soft delete 컬럼 도입이 확정된 이후 별도 결정 (§열린 질문).
--
-- 백필 (기존 learning_facade.concept → learning_facade_concept)은 Story 1-3에서 수행.
-- Entity/Repository 는 Story 1-2 에서 도입 — 본 Story 는 스키마만.
--
-- 컬럼명 주의: `value` 는 MySQL 예약어 (SQL:2003 reserved word).
--   Hibernate 매핑 시 backtick escaping 필요 위험 → `concept_value` 로 명명해 회피.
-- =============================================================

CREATE TABLE learning_facade_concept
(
    learning_facade_concept_id BIGINT       NOT NULL AUTO_INCREMENT,
    learning_facade_id         BIGINT       NOT NULL,
    concept_value              VARCHAR(100) NOT NULL,
    display_order              INT          NOT NULL,
    created_at                 DATETIME(6)  NOT NULL,

    PRIMARY KEY (learning_facade_concept_id),
    CONSTRAINT uk_learning_facade_concept_facade_value
        UNIQUE (learning_facade_id, concept_value),
    CONSTRAINT fk_learning_facade_concept_facade
        FOREIGN KEY (learning_facade_id) REFERENCES learning_facade (learning_facade_id)
            ON DELETE CASCADE,
    CONSTRAINT chk_learning_facade_concept_order
        CHECK (display_order >= 0),
    INDEX idx_learning_facade_concept_facade_order (learning_facade_id, display_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
