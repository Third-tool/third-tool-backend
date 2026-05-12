-- =============================================================
-- V6__learning_material_type_extension.sql
-- Epic-003 Story-003-4 (Story 4-1): 학습 자료 타입 확장 + 부가 속성 5종
-- - MaterialType 2종(TOP_DOWN/BOTTOM_UP) → 4종(BOOK/COURSE/AI_CONVERSATION/WEB_RESOURCE)
-- - learning_material에 부가 속성 5종(author/platform/ai_provider/web_source/memo) 추가 — 모두 nullable
-- - 기존 데이터 이관: TOP_DOWN → BOOK, BOTTOM_UP → WEB_RESOURCE (table-spec.md §4-1)
-- - 부가 속성은 모두 NULL로 초기화 (도메인 v5: 타입 정합성 미강제, 귀속 모호 §8 디폴트)
-- =============================================================

-- 1) 신규 컬럼 추가 (모두 nullable)
ALTER TABLE learning_material
    ADD COLUMN author      VARCHAR(100)  NULL AFTER url,
    ADD COLUMN platform    VARCHAR(100)  NULL AFTER author,
    ADD COLUMN ai_provider VARCHAR(50)   NULL AFTER platform,
    ADD COLUMN web_source  VARCHAR(50)   NULL AFTER ai_provider,
    ADD COLUMN memo        VARCHAR(1000) NULL AFTER web_source;

-- 2) 기존 CHECK 제약 제거 (CHECK 제약명이 존재하면 우선 제거)
ALTER TABLE learning_material
    DROP CONSTRAINT IF EXISTS learning_material_chk_1;
ALTER TABLE learning_material
    DROP CONSTRAINT IF EXISTS chk_learning_material_type;

-- 3) enum 값 데이터 변환 (CHECK 제거 후 진행)
UPDATE learning_material SET material_type = 'BOOK'         WHERE material_type = 'TOP_DOWN';
UPDATE learning_material SET material_type = 'WEB_RESOURCE' WHERE material_type = 'BOTTOM_UP';

-- 4) 갱신된 CHECK 제약 재부여 (4종)
ALTER TABLE learning_material
    ADD CONSTRAINT chk_learning_material_type
    CHECK (material_type IN ('BOOK', 'COURSE', 'AI_CONVERSATION', 'WEB_RESOURCE'));
