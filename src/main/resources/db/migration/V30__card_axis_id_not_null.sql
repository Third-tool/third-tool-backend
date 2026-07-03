-- =============================================================
-- V30. Card.axisId NOT NULL 승격 + FK 추가 (3단계 마이그레이션 3단계)
--
-- Story-LT-E4-S4-3 · milestone 0.0.4v PR#4
--
-- V29 백필로 모든 행이 값을 가져야 한다.
-- FK는 learning_axis 삭제 시 카드도 함께 삭제(ON DELETE CASCADE) — Axis 삭제가 카드 소유권 종결.
-- =============================================================

ALTER TABLE card
    MODIFY COLUMN axis_id BIGINT NOT NULL;

ALTER TABLE card
    ADD CONSTRAINT fk_card_axis
        FOREIGN KEY (axis_id) REFERENCES learning_axis (id)
        ON DELETE CASCADE;
