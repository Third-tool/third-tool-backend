-- =============================================================
-- V1__init.sql
-- 경로: src/main/resources/db/migration/V1__init.sql
-- =============================================================

-- -------------------------------------------------------------
-- 1. user_entity
-- -------------------------------------------------------------
CREATE TABLE user_entity
(
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    username             VARCHAR(255) NOT NULL,
    password             VARCHAR(255) NOT NULL,
    is_lock              TINYINT(1)   NOT NULL DEFAULT 0,
    is_social            TINYINT(1)   NOT NULL DEFAULT 0,
    social_provider_type VARCHAR(20)           DEFAULT NULL,  -- KAKAO | NAVER | NULL(자체로그인)
    role_type            VARCHAR(20)  NOT NULL,               -- USER | ADMIN
    nickname             VARCHAR(255)          DEFAULT NULL,
    email                VARCHAR(255)          DEFAULT NULL,
    created_date         DATETIME(6)           DEFAULT NULL,
    updated_date         DATETIME(6)           DEFAULT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_user_entity_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- -------------------------------------------------------------
-- 2. social_member  (JOINED 상속 전략 — 부모 테이블)
-- -------------------------------------------------------------
CREATE TABLE social_member
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT                DEFAULT NULL,
    social_id     VARCHAR(255) NOT NULL,
    refresh_token VARCHAR(512)          DEFAULT NULL,
    provider_type VARCHAR(31)  NOT NULL, -- JPA discriminator column

    PRIMARY KEY (id),
    UNIQUE KEY uk_social_member_social_id (social_id),
    CONSTRAINT fk_social_member_user
        FOREIGN KEY (user_id) REFERENCES user_entity (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- -------------------------------------------------------------
-- 3. kakao_member  (social_member 자식)
-- -------------------------------------------------------------
CREATE TABLE kakao_member
(
    id BIGINT NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_kakao_member_social
        FOREIGN KEY (id) REFERENCES social_member (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- -------------------------------------------------------------
-- 4. naver_member  (social_member 자식)
-- -------------------------------------------------------------
CREATE TABLE naver_member
(
    id BIGINT NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_naver_member_social
        FOREIGN KEY (id) REFERENCES social_member (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- -------------------------------------------------------------
-- 5. deck
-- -------------------------------------------------------------
CREATE TABLE deck
(
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    name                   VARCHAR(100) NOT NULL,
    last_accessed          DATETIME(6)           DEFAULT NULL,
    scoring_algorithm_type VARCHAR(50)  NOT NULL,
    on_library             TINYINT(1)   NOT NULL DEFAULT 0,
    published_at           DATETIME(6)           DEFAULT NULL,
    parent_deck_id         BIGINT                DEFAULT NULL,
    depth                  INT          NOT NULL DEFAULT 0,
    user_id                BIGINT       NOT NULL,

    PRIMARY KEY (id),
    -- Deck 엔티티의 @UniqueConstraint(name = "uk_deck_user_name", ...)
    UNIQUE KEY uk_deck_user_name (user_id, name),
    CONSTRAINT fk_deck_parent
        FOREIGN KEY (parent_deck_id) REFERENCES deck (id),
    CONSTRAINT fk_deck_user
        FOREIGN KEY (user_id) REFERENCES user_entity (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- -------------------------------------------------------------
-- 6. card   (@Embedded: MainNote + Summary 인라인)
-- -------------------------------------------------------------
CREATE TABLE card
(
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    deck_id           BIGINT      NOT NULL,

    -- MainNote (@Embedded)
    main_text_content TEXT                 DEFAULT NULL,
    main_image_url    VARCHAR(2048)        DEFAULT NULL,
    main_content_type VARCHAR(20) NOT NULL,  -- TEXT_ONLY | IMAGE_ONLY | MIXED

    -- Summary (@Embedded)
    summary_value     TEXT        NOT NULL,

    -- Soft Delete
    deleted           TINYINT(1)  NOT NULL DEFAULT 0,

    created_date      DATETIME(6) NOT NULL,
    updated_date      DATETIME(6)          DEFAULT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_card_deck
        FOREIGN KEY (deck_id) REFERENCES deck (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- -------------------------------------------------------------
-- 7. keyword_cue
-- -------------------------------------------------------------
CREATE TABLE keyword_cue
(
    keyword_cue_id BIGINT       NOT NULL AUTO_INCREMENT,
    value          VARCHAR(200) NOT NULL,
    card_id        BIGINT       NOT NULL,

    PRIMARY KEY (keyword_cue_id),
    CONSTRAINT fk_keyword_cue_card
        FOREIGN KEY (card_id) REFERENCES card (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE jwt_refresh_entity
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    username     VARCHAR(255) NOT NULL UNIQUE,
    refresh      VARCHAR(512) NOT NULL,
    created_date DATETIME(6)           DEFAULT NULL,
    updated_date DATETIME(6)           DEFAULT NULL,

    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;