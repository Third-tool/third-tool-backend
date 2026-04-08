package com.example.thirdtool.Card.domain.model;

import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Summary")
class SummaryTest {

    @Nested
    @DisplayName("of() — 정상 생성")
    class Create {

        @Test
        @DisplayName("1문장 요약을 생성할 수 있다")
        void of_oneSentence_success() {
            // given
            String oneSentence = "핵심만 담는다.";

            // when & then
            assertThatCode(() -> Summary.of(oneSentence))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("3문장 요약을 생성할 수 있다 — 최댓값 경계")
        void of_threeSentences_success() {
            // given
            String threeSentences = "문장1. 문장2. 문장3.";

            // when & then
            assertThatCode(() -> Summary.of(threeSentences))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("value는 저장 전에 trim 처리된다")
        void of_valueWithWhitespace_trimmedBeforeSaving() {
            // given
            String valueWithWhitespace = "  핵심만 담는다.  ";

            // when
            Summary summary = Summary.of(valueWithWhitespace);

            // then
            assertThat(summary.getValue()).isEqualTo("핵심만 담는다.");
        }
    }

    @Nested
    @DisplayName("of() — 유효성 검증")
    class Validation {

        @Test
        @DisplayName("null이면 CARD_SUMMARY_EMPTY 예외가 발생한다")
        void of_null_throwsCardSummaryEmptyException() {
            // given - no setup

            // when & then
            assertThatThrownBy(() -> Summary.of(null))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CARD_SUMMARY_EMPTY);
        }

        @Test
        @DisplayName("blank이면 CARD_SUMMARY_EMPTY 예외가 발생한다")
        void of_blank_throwsCardSummaryEmptyException() {
            // given - no setup

            // when & then
            assertThatThrownBy(() -> Summary.of("   "))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CARD_SUMMARY_EMPTY);
        }

        @Test
        @DisplayName("4문장 이상이면 CARD_SUMMARY_SENTENCE_OUT_OF_RANGE 예외가 발생하고 현재 문장 수가 메시지에 포함된다")
        void of_fourSentences_throwsSentenceOutOfRangeException() {
            // given
            String fourSentences = "문장1. 문장2. 문장3. 문장4.";

            // when & then
            assertThatThrownBy(() -> Summary.of(fourSentences))
                    .isInstanceOf(CardDomainException.class)
                    .satisfies(ex -> {
                        CardDomainException cde = (CardDomainException) ex;
                        assertThat(cde.getErrorCode()).isEqualTo(ErrorCode.CARD_SUMMARY_SENTENCE_OUT_OF_RANGE);
                        assertThat(cde.getMessage()).contains("현재 4문장");
                    });
        }
    }

    @Nested
    @DisplayName("countSentences() — 문장 수 판정")
    class CountSentences {

        @Test
        @DisplayName("빈 문장은 카운트에서 제외된다")
        void countSentences_withEmptySentences_excludesThemFromCount() {
            // given
            String textWithEmptySentence = "문장1.  . 문장2.";

            // when
            int count = Summary.countSentences(textWithEmptySentence);

            // then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("!, ? 도 문장 구분자로 처리된다")
        void countSentences_withMultipleDelimiters_countsAllSentences() {
            // given
            String textWithMultipleDelimiters = "문장1! 문장2? 문장3.";

            // when
            int count = Summary.countSentences(textWithMultipleDelimiters);

            // then
            assertThat(count).isEqualTo(3);
        }
    }
}