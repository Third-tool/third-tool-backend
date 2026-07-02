-- =============================================================
-- V18__learning_layer.sql
-- LT Epic 2 Story 2-1: learning_layer 테이블 신설 + LearningLayer Aggregate 기반 스키마
--
-- Layer 정책 (ADR003 v2 확장 · ADR021 관행 · issue-05):
--   · Layer는 사용자 자산성 도메인 → Soft Delete 적용 (deleted_at 컬럼)
--   · 동일 Facade 내 활성 Layer 이름 유일 → UNIQUE (facade_id, name, deleted_at)
--     MySQL은 NULL을 서로 다른 값으로 취급하므로 활성 대상에서만 유일성 강제,
--     softDeleted Layer의 이름 재사용은 허용.
--   · display_order는 도메인은 1-based, DB CHECK는 ≥ 0 안전망.
--   · FK ON DELETE 미지정 (기본 RESTRICT) — LearningFacade는 soft delete 도메인이라
--     하드 삭제 경로 없음.
-- =============================================================

CREATE TABLE learning_layer
(
    learning_layer_id  BIGINT       NOT NULL AUTO_INCREMENT,
    learning_facade_id BIGINT       NOT NULL,
    name               VARCHAR(100) NOT NULL,
    display_order      INT          NOT NULL,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    deleted_at         DATETIME(6)  NULL,

    PRIMARY KEY (learning_layer_id),
    CONSTRAINT uk_learning_layer_facade_name_deleted
        UNIQUE (learning_facade_id, name, deleted_at),
    CONSTRAINT fk_learning_layer_facade
        FOREIGN KEY (learning_facade_id) REFERENCES learning_facade (learning_facade_id),
    CONSTRAINT chk_learning_layer_order
        CHECK (display_order >= 0),
    INDEX idx_learning_layer_facade_order (learning_facade_id, display_order),
    INDEX idx_learning_layer_deleted (deleted_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
