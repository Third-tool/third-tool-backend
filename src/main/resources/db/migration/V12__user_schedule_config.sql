-- =============================================================
-- V12. user_schedule_config + user_schedule_config_history 신설
--
-- product-card.md Epic 4 Story 4-2 — 사용자별 설정값 독립 저장 및 조회.
-- 도메인 클래스:
--   - UserSchedule/domain/model/UserScheduleConfig.java
--   - UserSchedule/domain/model/UserScheduleConfigHistory.java
--
-- 의미
--   - user_schedule_config: 유저 1명당 1행. raw_input_days(사용자 입력 일수) + mapped_mode(매핑 결과 enum).
--   - user_schedule_config_history: 모드 변경 이력 append-only. 최초 생성 시 from_mode=NULL.
--
-- 도메인 규약
--   - 유저당 1개 강제 (UNIQUE(user_id))
--   - mapped_mode CHECK: MODE_10D | MODE_20D | MODE_30D (LearningMode enum)
--   - raw_input_days >= 1 (도메인 검증)
--   - history.from_mode NULL 허용 (createDefault/처음 생성 시) — to_mode는 NOT NULL
-- =============================================================

CREATE TABLE user_schedule_config
(
    user_schedule_config_id BIGINT      NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT      NOT NULL,
    raw_input_days          INT         NOT NULL,
    mapped_mode             VARCHAR(20) NOT NULL,
    created_at              DATETIME(6) NOT NULL,
    updated_at              DATETIME(6) NOT NULL,

    PRIMARY KEY (user_schedule_config_id),

    CONSTRAINT uk_user_schedule_config_user_id UNIQUE (user_id),

    CONSTRAINT fk_user_schedule_config_user
        FOREIGN KEY (user_id) REFERENCES user_entity (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_user_schedule_config_mapped_mode
        CHECK (mapped_mode IN ('MODE_10D', 'MODE_20D', 'MODE_30D')),

    CONSTRAINT chk_user_schedule_config_raw_input_days
        CHECK (raw_input_days >= 1)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE user_schedule_config_history
(
    user_schedule_config_history_id BIGINT      NOT NULL AUTO_INCREMENT,
    user_schedule_config_id         BIGINT      NOT NULL,
    from_mode                       VARCHAR(20)          DEFAULT NULL,
    to_mode                         VARCHAR(20) NOT NULL,
    raw_input_days                  INT         NOT NULL,
    changed_at                      DATETIME(6) NOT NULL,

    PRIMARY KEY (user_schedule_config_history_id),

    CONSTRAINT fk_user_schedule_config_history_config
        FOREIGN KEY (user_schedule_config_id) REFERENCES user_schedule_config (user_schedule_config_id)
        ON DELETE CASCADE,

    CONSTRAINT chk_user_schedule_config_history_from_mode
        CHECK (from_mode IS NULL OR from_mode IN ('MODE_10D', 'MODE_20D', 'MODE_30D')),

    CONSTRAINT chk_user_schedule_config_history_to_mode
        CHECK (to_mode IN ('MODE_10D', 'MODE_20D', 'MODE_30D')),

    CONSTRAINT chk_user_schedule_config_history_raw_input_days
        CHECK (raw_input_days >= 1)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_user_schedule_config_history_config_id
    ON user_schedule_config_history (user_schedule_config_id);
CREATE INDEX idx_user_schedule_config_history_changed_at
    ON user_schedule_config_history (changed_at);
