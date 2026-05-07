-- =============================================================
-- V3__topic_revision.sql
-- Epic-003 Story-003-2: 주제(AxisTopic) 수정 이력 + 수정 이유 선택지
-- - revision_reason_option: 운영 중 DB로 관리되는 수정 이유 선택지 (라벨/순서/활성)
-- - topic_revision: 주제 이름 수정 이력 (이전/이후 이름 + 이유 라벨 스냅샷)
-- - 이유 라벨은 FK가 아닌 텍스트 스냅샷으로 보관 (관리자 라벨 변경 시 과거 이력 보존)
-- - 단일 동사 강제 폐지(ADR004)에 따라 동사 변경 강제 초기화 로직 없음
-- =============================================================

-- -------------------------------------------------------------
-- 1. revision_reason_option (관리 테이블)
-- -------------------------------------------------------------
CREATE TABLE revision_reason_option
(
    revision_reason_option_id BIGINT       NOT NULL AUTO_INCREMENT,
    label                     VARCHAR(100) NOT NULL,
    display_order             INT          NOT NULL,
    active                    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                DATETIME(6)  NOT NULL,

    PRIMARY KEY (revision_reason_option_id),
    INDEX idx_revision_reason_active_order (active, display_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 초기 시드 (주제 맥락에 맞춘 라벨)
INSERT INTO revision_reason_option (label, display_order, active, created_at) VALUES
    ('기존 주제가 너무 좁았다',  1, TRUE, NOW(6)),
    ('기존 주제가 너무 넓었다',  2, TRUE, NOW(6)),
    ('더 정확한 표현을 찾았다',  3, TRUE, NOW(6)),
    ('방향 자체가 바뀌었다',     4, TRUE, NOW(6));


-- -------------------------------------------------------------
-- 2. topic_revision (주제 이름 수정 이력)
-- -------------------------------------------------------------
CREATE TABLE topic_revision
(
    topic_revision_id      BIGINT       NOT NULL AUTO_INCREMENT,
    axis_topic_id          BIGINT       NOT NULL,
    previous_name          VARCHAR(100) NOT NULL,
    new_name               VARCHAR(100) NOT NULL,
    revision_reason_label  VARCHAR(100)          DEFAULT NULL,
    revised_at             DATETIME(6)  NOT NULL,

    PRIMARY KEY (topic_revision_id),
    CONSTRAINT fk_topic_revision_topic
        FOREIGN KEY (axis_topic_id) REFERENCES axis_topic (axis_topic_id)
            ON DELETE CASCADE,
    INDEX idx_topic_revision_topic_revised (axis_topic_id, revised_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
