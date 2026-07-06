-- V37 · REV E3 · Story 3-2/3-5/3-8 — Recommendation + UserNotification 신설
--
-- Recommendation Aggregate — 규칙 판정 이력 · accept/dismiss 상태 · v2 자동 조정 근거
-- UserNotification Aggregate — in-app 알림 (v1 minimal: DB row + polling)

-- 1. recommendation 테이블
CREATE TABLE recommendation (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    type          VARCHAR(30)  NOT NULL,
    from_mode     VARCHAR(20)  NOT NULL,
    to_mode       VARCHAR(20)  NOT NULL,
    reason        VARCHAR(500) NOT NULL,
    triggered_at  DATETIME(6)  NOT NULL,
    resolved_at   DATETIME(6)  NULL,
    action        VARCHAR(20)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_recommendation_user
        FOREIGN KEY (user_id) REFERENCES user_entity (id),
    CONSTRAINT chk_recommendation_type
        CHECK (type IN ('SUGGEST_DOWNGRADE', 'SUGGEST_UPGRADE')),
    CONSTRAINT chk_recommendation_action
        CHECK (action IS NULL OR action IN ('ACCEPTED', 'DISMISSED'))
);

CREATE INDEX idx_recommendation_user_triggered ON recommendation (user_id, triggered_at);
CREATE INDEX idx_recommendation_user_unresolved ON recommendation (user_id, resolved_at);

-- 2. user_notification 테이블 (v1 minimal)
CREATE TABLE user_notification (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    user_id           BIGINT        NOT NULL,
    type              VARCHAR(30)   NOT NULL,
    payload_json      VARCHAR(2000) NOT NULL,
    recommendation_id BIGINT        NULL,
    created_at        DATETIME(6)   NOT NULL,
    read_at           DATETIME(6)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notification_user
        FOREIGN KEY (user_id) REFERENCES user_entity (id),
    CONSTRAINT fk_notification_recommendation
        FOREIGN KEY (recommendation_id) REFERENCES recommendation (id),
    CONSTRAINT chk_notification_type
        CHECK (type IN ('SUGGEST_DOWNGRADE', 'SUGGEST_UPGRADE', 'WEEKLY_SUMMARY'))
);

CREATE INDEX idx_notification_user_read ON user_notification (user_id, read_at);
CREATE INDEX idx_notification_user_created ON user_notification (user_id, created_at);
