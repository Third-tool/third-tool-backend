-- =============================================================
-- V7. deck 테이블에 LearningFacade 연결 컬럼 추가
--
-- Epic 5 (Story-005-1): 학습 자료 등록 시 동명의 Deck 자동 생성.
-- - axis_id: Deck이 귀속된 LearningAxis (자료 삭제와 무관하게 영구 보존)
-- - learning_material_id: Deck이 어느 LearningMaterial로 만들어졌는지 추적
--                        (자료 삭제 시 NULL로 전환 → "자료 미연결 Deck"으로 유지)
--
-- 둘 다 nullable — 기존 Deck(자료 무관)도 그대로 동작해야 하므로.
-- FK 정책: ON DELETE SET NULL (축·자료 삭제 시 Deck 자체는 보존, 연결만 끊김).
-- =============================================================

ALTER TABLE deck
    ADD COLUMN axis_id              BIGINT NULL,
    ADD COLUMN learning_material_id BIGINT NULL;

ALTER TABLE deck
    ADD CONSTRAINT fk_deck_axis
        FOREIGN KEY (axis_id) REFERENCES learning_axis (learning_axis_id)
            ON DELETE SET NULL;

ALTER TABLE deck
    ADD CONSTRAINT fk_deck_learning_material
        FOREIGN KEY (learning_material_id) REFERENCES learning_material (learning_material_id)
            ON DELETE SET NULL;

CREATE INDEX idx_deck_axis ON deck (axis_id);
CREATE INDEX idx_deck_learning_material ON deck (learning_material_id);
