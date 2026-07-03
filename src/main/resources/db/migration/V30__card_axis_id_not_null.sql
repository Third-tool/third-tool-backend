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

-- Reviewer 조치: LearningAxis는 Soft Delete(ADR021, V14), Card도 Soft Delete(ADR003).
-- CASCADE는 hard delete 시나리오만 트리거되지만 관리자·복구 흐름에서 조용한 데이터 소실 위험.
-- RESTRICT로 두어 Application이 명시적으로 카드 정리 후 축 삭제하도록 강제.
ALTER TABLE card
    ADD CONSTRAINT fk_card_axis
        FOREIGN KEY (axis_id) REFERENCES learning_axis (id)
        ON DELETE RESTRICT;
