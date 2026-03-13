-- V1__create_card_tables.sql
-- Card 도메인 초기 스키마

-- ============================================================
-- card 테이블
-- MainNote(main_text, main_image_url, main_content_type)와
-- Summary(summary_value)를 Card와 같은 테이블에 포함한다.
-- ============================================================

CREATE TABLE card
(
    card_id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '카드 식별자',

    -- MainNote (Embedded)
    main_text         TEXT         NULL     COMMENT '학습 본문 텍스트',
    main_image_url    VARCHAR(2048) NULL     COMMENT '학습 이미지 경로',
    main_content_type VARCHAR(20)  NOT NULL COMMENT 'TEXT_ONLY | IMAGE_ONLY | MIXED',

    -- Summary (Embedded)
    summary_value     TEXT         NOT NULL COMMENT '핵심 압축 요약 (1~3문장)',

    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',

    PRIMARY KEY (card_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '회상 가능한 학습 단위';


-- ============================================================
-- keyword_cue 테이블
-- Card Aggregate 내부 Entity. card_id FK로 Card에 종속된다.
-- ============================================================

CREATE TABLE keyword_cue
(
    keyword_cue_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '단서 식별자',
    card_id        BIGINT       NOT NULL COMMENT '소속 카드 FK',
    value          VARCHAR(200) NOT NULL COMMENT '단서 내용',

    PRIMARY KEY (keyword_cue_id),

    CONSTRAINT fk_keyword_cue_card
        FOREIGN KEY (card_id) REFERENCES card (card_id)
            ON DELETE CASCADE   -- Card 삭제 시 단서도 함께 삭제 (orphanRemoval 보완)
            ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '회상 단서 (Card Aggregate Entity)';


-- ============================================================
-- 인덱스
-- ============================================================

-- 단서 → 카드 조회 (페치 조인 시 FK 인덱스 활용)
CREATE INDEX idx_keyword_cue_card_id ON keyword_cue (card_id);

-- Summary 키워드 검색 (LIKE '%keyword%'는 인덱스 미사용, FULLTEXT 권장)
ALTER TABLE card
    ADD FULLTEXT INDEX ft_card_summary (summary_value) WITH PARSER ngram;

-- MainNote contentType 필터 조회
CREATE INDEX idx_card_content_type ON card (main_content_type);