-- =============================================================
-- V22__axis_selection_node.sql
-- LT Epic 3 Story 3-10: axis_selection_node 테이블 신설
--
-- Selection 챕터 노드 (이슈 #16):
--   · 컨테이너 hard delete 정책 계승 → 자식 노드도 soft delete 없음
--   · FK ON DELETE CASCADE — 컨테이너 삭제 시 자식 노드 자동 삭제
--   · display_order 도메인 1-based, DB CHECK ≥ 1
--   · body MEDIUMTEXT (16MB) — ASCII subtree 통짜 저장
-- =============================================================

CREATE TABLE axis_selection_node
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    selection_id   BIGINT       NOT NULL,
    display_order  INT          NOT NULL,
    title          VARCHAR(200) NOT NULL,
    rationale      VARCHAR(500) NULL,
    body           MEDIUMTEXT   NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_axis_selection_node_selection
        FOREIGN KEY (selection_id) REFERENCES axis_selection (id) ON DELETE CASCADE,
    CONSTRAINT chk_axis_selection_node_order
        CHECK (display_order >= 1),
    INDEX idx_axis_selection_node_selection_order (selection_id, display_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
