-- =============================================================
-- V11. tag + card_tag 테이블 신설
--
-- product-card.md Epic 5 — Tag 시스템 전역 유니크 + find-or-create.
-- 도메인 클래스:
--   - Card/domain/model/Tag.java        — 시스템 전역 UNIQUE value, AR(작은 Aggregate)
--   - Card/domain/model/CardTag.java    — Card ↔ Tag 매핑 (linkedAt 보존)
--
-- 패턴 메모
--   - Tag는 시스템 전역 UNIQUE — `uk_tag_value`. 같은 값은 어느 사용자가 만들었든 단일 row 재사용.
--   - CardTag 매핑은 surrogate id PK + UNIQUE(card_id, tag_id) (ADR001 — 모든 PK BIGINT AUTO_INCREMENT).
--   - 카드당 최대 3개 부착은 도메인(Card.MAX_TAG_COUNT)에서 강제하며 DB 제약은 두지 않는다.
--   - Soft Delete 미적용 (conventions §3.4 — 연결 사실 기록만 있고 복원 요구 낮음).
-- =============================================================

CREATE TABLE tag
(
    tag_id    BIGINT      NOT NULL AUTO_INCREMENT,
    tag_value VARCHAR(50) NOT NULL,

    PRIMARY KEY (tag_id),
    CONSTRAINT uk_tag_value UNIQUE (tag_value)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE card_tag
(
    card_tag_id BIGINT      NOT NULL AUTO_INCREMENT,
    card_id     BIGINT      NOT NULL,
    tag_id      BIGINT      NOT NULL,
    linked_at   DATETIME(6) NOT NULL,

    PRIMARY KEY (card_tag_id),

    CONSTRAINT fk_card_tag_card
        FOREIGN KEY (card_id) REFERENCES card (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_card_tag_tag
        FOREIGN KEY (tag_id) REFERENCES tag (tag_id)
        ON DELETE CASCADE,

    CONSTRAINT uk_card_tag UNIQUE (card_id, tag_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_card_tag_card_id ON card_tag (card_id);
CREATE INDEX idx_card_tag_tag_id  ON card_tag (tag_id);
