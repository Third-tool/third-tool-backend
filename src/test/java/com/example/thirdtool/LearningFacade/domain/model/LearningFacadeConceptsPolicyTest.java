package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story-LT-E1-S5 · concepts 도메인 상수 · ErrorCode 정합 검증.
 *
 * <p>다른 레이어(Application Service / Controller / FE)가 도메인 상수를
 * 재정의하지 않고 참조하는지 정적 확인 — 여기서는 도메인 상수 자체가 존재하고
 * ErrorCode 4종이 enum에 등록됐는지만 검증한다.
 */
@DisplayName("LearningFacade · concepts 정책 상수 · ErrorCode 정합")
class LearningFacadeConceptsPolicyTest {

    @Test
    @DisplayName("도메인_상수_3종_등록")
    void 도메인_상수_3종_등록() {
        assertThat(LearningFacade.MIN_CONCEPT_COUNT).isEqualTo(1);
        assertThat(LearningFacade.MAX_CONCEPT_COUNT).isEqualTo(5);
        assertThat(LearningFacade.MAX_CONCEPT_VALUE_LENGTH).isEqualTo(100);
    }

    @Test
    @DisplayName("ErrorCode_4종_등록_SIZE_INVALID_DUPLICATE_BLANK_TOO_LONG")
    void ErrorCode_4종_등록_SIZE_INVALID_DUPLICATE_BLANK_TOO_LONG() {
        // BLANK는 LF003 (기존 재사용), SIZE_INVALID·DUPLICATE·TOO_LONG는 신규 (LF005~007)
        assertThat(ErrorCode.LEARNING_FACADE_CONCEPT_BLANK.getCode()).isEqualTo("LF003");
        assertThat(ErrorCode.LEARNING_FACADE_CONCEPTS_SIZE_INVALID.getCode()).isEqualTo("LF005");
        assertThat(ErrorCode.LEARNING_FACADE_CONCEPT_DUPLICATE.getCode()).isEqualTo("LF006");
        assertThat(ErrorCode.LEARNING_FACADE_CONCEPT_TOO_LONG.getCode()).isEqualTo("LF007");
    }

    @Test
    @DisplayName("ErrorCode_HttpStatus_매핑_정합")
    void ErrorCode_HttpStatus_매핑_정합() {
        // BAD_REQUEST → 검증 실패, CONFLICT → 중복
        assertThat(ErrorCode.LEARNING_FACADE_CONCEPT_BLANK.getStatus().value()).isEqualTo(400);
        assertThat(ErrorCode.LEARNING_FACADE_CONCEPTS_SIZE_INVALID.getStatus().value()).isEqualTo(400);
        assertThat(ErrorCode.LEARNING_FACADE_CONCEPT_TOO_LONG.getStatus().value()).isEqualTo(400);
        assertThat(ErrorCode.LEARNING_FACADE_CONCEPT_DUPLICATE.getStatus().value()).isEqualTo(409);
    }
}
