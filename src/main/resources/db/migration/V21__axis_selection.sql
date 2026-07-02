-- =============================================================
-- V21__axis_selection.sql
-- LT Epic 3 Story 3-10: axis_selection 컨테이너 테이블 신설
--
-- 이슈 #16 · 이슈 #11 정책 계승:
--   · name UNIQUE per axis (활성 컨테이너)
--   · created_at DESC 정렬 (조회는 최신 우선)
--   · hard delete (컨테이너 삭제 시 자식 노드 CASCADE — V22 참조)
--   · Soft Delete 없음
-- =============================================================

CREATE TABLE axis_selection
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    axis_id        BIGINT       NOT NULL,
    name           VARCHAR(100) NOT NULL,
    created_at     DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_axis_selection_axis_name
        UNIQUE (axis_id, name),
    CONSTRAINT fk_axis_selection_axis
        FOREIGN KEY (axis_id) REFERENCES learning_axis (learning_axis_id) ON DELETE CASCADE,
    INDEX idx_axis_selection_axis_created (axis_id, created_at DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
