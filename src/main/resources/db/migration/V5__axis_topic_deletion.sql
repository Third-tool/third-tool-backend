-- =============================================================
-- V5__axis_topic_deletion.sql
-- Epic-003 Story-003-4: 주제 삭제 이력 archive 테이블
-- - ADR003 정합: AxisTopic은 soft delete 대신 archive 패턴 (구조 편집 단위)
-- - FK 없음 — 원본 axis_topic / learning_axis 행이 사라져도 archive는 보존
-- - 인덱스: (learning_axis_id, deleted_at) — 축별 최신순 조회 커버
-- =============================================================

CREATE TABLE axis_topic_deletion
(
    axis_topic_deletion_id BIGINT       NOT NULL AUTO_INCREMENT,
    original_topic_id      BIGINT       NOT NULL,
    learning_axis_id       BIGINT       NOT NULL,
    name                   VARCHAR(100) NOT NULL,
    description            VARCHAR(500)          DEFAULT NULL,
    revision_count         INT          NOT NULL,
    deleted_at             DATETIME(6)  NOT NULL,

    PRIMARY KEY (axis_topic_deletion_id),
    INDEX idx_axis_topic_deletion_axis_deleted_at (learning_axis_id, deleted_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
