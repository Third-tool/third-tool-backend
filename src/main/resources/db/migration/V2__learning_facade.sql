-- =============================================================
-- V2__learning_facade.sql
-- LearningFacade BC v2 스키마 신규 생성 (AxisTopic 모델)
-- - V1__init.sql에 LearningFacade 관련 테이블이 없으므로 신규 생성
-- - 단일 동사 강제(AxisAction) 모델 폐지, 명사구 + 선택 설명(AxisTopic) 모델 채택
-- - Epic 3 (TopicRevision)은 별도 마이그레이션
-- =============================================================

-- -------------------------------------------------------------
-- 1. learning_facade
-- -------------------------------------------------------------
CREATE TABLE learning_facade
(
    learning_facade_id BIGINT       NOT NULL AUTO_INCREMENT,
    user_id            BIGINT       NOT NULL,
    concept            VARCHAR(100) NOT NULL,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,

    PRIMARY KEY (learning_facade_id),
    CONSTRAINT uk_learning_facade_user UNIQUE (user_id),
    CONSTRAINT fk_learning_facade_user
        FOREIGN KEY (user_id) REFERENCES user_entity (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- -------------------------------------------------------------
-- 2. learning_axis
-- -------------------------------------------------------------
CREATE TABLE learning_axis
(
    learning_axis_id   BIGINT       NOT NULL AUTO_INCREMENT,
    learning_facade_id BIGINT       NOT NULL,
    name               VARCHAR(100) NOT NULL,
    display_order      INT          NOT NULL,
    created_at         DATETIME(6)  NOT NULL,

    PRIMARY KEY (learning_axis_id),
    CONSTRAINT uk_learning_axis_facade_name UNIQUE (learning_facade_id, name),
    CONSTRAINT fk_learning_axis_facade
        FOREIGN KEY (learning_facade_id) REFERENCES learning_facade (learning_facade_id)
            ON DELETE CASCADE,
    INDEX idx_learning_axis_facade_order (learning_facade_id, display_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- -------------------------------------------------------------
-- 3. axis_topic (구 axis_action을 대체하는 v2 모델)
-- -------------------------------------------------------------
CREATE TABLE axis_topic
(
    axis_topic_id    BIGINT       NOT NULL AUTO_INCREMENT,
    learning_axis_id BIGINT       NOT NULL,
    name             VARCHAR(100) NOT NULL,
    description      VARCHAR(500)          DEFAULT NULL,
    display_order    INT          NOT NULL,
    coverage_status  VARCHAR(20)  NOT NULL DEFAULT 'NO_MATERIAL',
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,

    PRIMARY KEY (axis_topic_id),
    CONSTRAINT fk_axis_topic_axis
        FOREIGN KEY (learning_axis_id) REFERENCES learning_axis (learning_axis_id)
            ON DELETE CASCADE,
    CONSTRAINT chk_axis_topic_coverage
        CHECK (coverage_status IN ('NO_MATERIAL', 'PARTIAL', 'COVERED')),
    INDEX idx_axis_topic_axis_order (learning_axis_id, display_order),
    INDEX idx_axis_topic_coverage (coverage_status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- -------------------------------------------------------------
-- 4. learning_material
-- -------------------------------------------------------------
CREATE TABLE learning_material
(
    learning_material_id BIGINT        NOT NULL AUTO_INCREMENT,
    learning_facade_id   BIGINT        NOT NULL,
    name                 VARCHAR(200)  NOT NULL,
    material_type        VARCHAR(20)   NOT NULL,
    url                  VARCHAR(2048)          DEFAULT NULL,
    proficiency_level    VARCHAR(20)   NOT NULL DEFAULT 'UNRATED',
    created_at           DATETIME(6)   NOT NULL,
    updated_at           DATETIME(6)   NOT NULL,

    PRIMARY KEY (learning_material_id),
    CONSTRAINT fk_learning_material_facade
        FOREIGN KEY (learning_facade_id) REFERENCES learning_facade (learning_facade_id)
            ON DELETE CASCADE,
    CONSTRAINT chk_learning_material_type
        CHECK (material_type IN ('TOP_DOWN', 'BOTTOM_UP')),
    CONSTRAINT chk_learning_material_proficiency
        CHECK (proficiency_level IN ('UNRATED', 'UNFAMILIAR', 'GETTING_USED', 'MASTERED')),
    INDEX idx_learning_material_facade (learning_facade_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- -------------------------------------------------------------
-- 5. topic_material (구 action_material을 대체하는 v2 매핑)
-- -------------------------------------------------------------
CREATE TABLE topic_material
(
    topic_material_id    BIGINT      NOT NULL AUTO_INCREMENT,
    axis_topic_id        BIGINT      NOT NULL,
    learning_material_id BIGINT      NOT NULL,
    linked_at            DATETIME(6) NOT NULL,

    PRIMARY KEY (topic_material_id),
    CONSTRAINT uk_topic_material UNIQUE (axis_topic_id, learning_material_id),
    CONSTRAINT fk_topic_material_topic
        FOREIGN KEY (axis_topic_id) REFERENCES axis_topic (axis_topic_id)
            ON DELETE CASCADE,
    CONSTRAINT fk_topic_material_material
        FOREIGN KEY (learning_material_id) REFERENCES learning_material (learning_material_id)
            ON DELETE CASCADE,
    INDEX idx_topic_material_topic (axis_topic_id),
    INDEX idx_topic_material_material (learning_material_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
