package com.example.thirdtool.Card.domain.model;

import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.example.thirdtool.support.DomainFixture.sampleCard;
import static org.assertj.core.api.Assertions.*;

@DisplayName("KeywordCue")
class KeywordCueTest {

    @Nested
    @DisplayName("create() — 정상 생성")
    class Create {

        @Test
        @DisplayName("유효한 입력으로 KeywordCue를 생성할 수 있다")
        void create_validInput_success() {
            // given
            Card card = sampleCard();
            String value = "단서";

            // when & then
            assertThatCode(() -> KeywordCue.create(card, value))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("value는 저장 전에 trim 처리된다")
        void create_valueWithWhitespace_trimmedBeforeSaving() {
            // given
            Card card = sampleCard();
            String valueWithWhitespace = "  단서  ";

            // when
            KeywordCue cue = KeywordCue.create(card, valueWithWhitespace);

            // then
            assertThat(cue.getValue()).isEqualTo("단서");
        }
    }

    @Nested
    @DisplayName("create() — 유효성 검증")
    class Validation {

        @Test
        @DisplayName("value가 blank이면 CARD_KEYWORD_BLANK 예외가 발생한다")
        void create_blankValue_throwsCardKeywordBlankException() {
            // given
            Card card = sampleCard();
            String blankValue = "   ";

            // when & then
            assertThatThrownBy(() -> KeywordCue.create(card, blankValue))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CARD_KEYWORD_BLANK);
        }

        @Test
        @DisplayName("value가 null이면 CARD_KEYWORD_BLANK 예외가 발생한다")
        void create_nullValue_throwsCardKeywordBlankException() {
            // given
            Card card = sampleCard();

            // when & then
            assertThatThrownBy(() -> KeywordCue.create(card, null))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CARD_KEYWORD_BLANK);
        }

        @Test
        @DisplayName("card가 null이면 INVALID_INPUT 예외가 발생한다")
        void create_nullCard_throwsInvalidInputException() {
            // given - no setup

            // when & then
            assertThatThrownBy(() -> KeywordCue.create(null, "키워드"))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }
    }
}