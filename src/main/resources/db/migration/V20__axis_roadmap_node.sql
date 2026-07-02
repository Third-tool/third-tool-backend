-- =============================================================
-- V20__axis_roadmap_node.sql
-- LT Epic 3 Story 3-7: axis_roadmap_node 테이블 신설
--
-- Roadmap 챕터 노드 (이슈 #15, 2026-07-02 개정):
--   · 이슈 #6의 axis_roadmap.content TEXT 통짜 방식 SUPERSEDED
--   · 챕터 first-class Entity로 승격 → 챕터별 저장·조회·순서변경·재생성 가능
--   · body에는 챕터 subtree(1-1~1-N + 리프 본문) ASCII 통짜 저장
--
-- 정책:
--   · Soft Delete 적용 (deleted_at) — 노드 개별 복원 가능
--   · display_order 도메인은 1-based, DB CHECK ≥ 1 (Layer가 ≥ 0인 것과 다름)
--   · title/body는 필수. rationale은 선택
--   · FK ON DELETE CASCADE — axis가 하드 삭제되면 노드도 함께
--     (실제 axis는 soft delete가 표준. hard delete는 개발/테스트 초기화 상황용)
--
-- 이전(#6)의 axis_roadmap 테이블은 어느 릴리스에도 존재한 적 없어 아카이브 대상 없음.
-- =============================================================

CREATE TABLE axis_roadmap_node
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    axis_id        BIGINT       NOT NULL,
    display_order  INT          NOT NULL,
    title          VARCHAR(200) NOT NULL,
    rationale      VARCHAR(500) NULL,
    body           MEDIUMTEXT   NOT NULL,       -- ASCII subtree 통짜 저장. TEXT 64KB 상한 회피 (up to 16MB)
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    deleted_at     DATETIME(6)  NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_axis_roadmap_node_axis
        FOREIGN KEY (axis_id) REFERENCES learning_axis (learning_axis_id) ON DELETE CASCADE,
    CONSTRAINT chk_axis_roadmap_node_order
        CHECK (display_order >= 1),
    INDEX idx_axis_roadmap_node_axis_order (axis_id, display_order),
    INDEX idx_axis_roadmap_node_deleted (deleted_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
