package com.example.thirdtool.Card.domain.model;

import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Tag")
class TagTest {

    @Nested
    @DisplayName("of() — 정상 생성")
    class Create {

        @Test
        @DisplayName("유효한 value로 Tag를 생성할 수 있다")
        void of_validValue_success() {
            // given
            String value = "트랜잭션";

            // when & then
            assertThatCode(() -> Tag.of(value))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("value는 저장 전에 trim 처리된다")
        void of_valueWithWhitespace_trimmedBeforeSaving() {
            // given
            String valueWithWhitespace = "  DB  ";

            // when
            Tag tag = Tag.of(valueWithWhitespace);

            // then
            // trim 후 저장. 공백 포함 값이 저장되면 uk_tag_value 제약 우회 가능
            assertThat(tag.getValue()).isEqualTo("DB");
        }
    }

    @Nested
    @DisplayName("of() — 유효성 검증")
    class Validation {

        @Test
        @DisplayName("value가 blank이면 TAG_VALUE_BLANK 예외가 발생한다")
        void of_blank_throwsTagValueBlankException() {
            // given - no setup

            // when & then
            assertThatThrownBy(() -> Tag.of("  "))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TAG_VALUE_BLANK);
        }

        @Test
        @DisplayName("value가 null이면 TAG_VALUE_BLANK 예외가 발생한다")
        void of_null_throwsTagValueBlankException() {
            // given - no setup

            // when & then
            assertThatThrownBy(() -> Tag.of(null))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TAG_VALUE_BLANK);
        }
    }
}