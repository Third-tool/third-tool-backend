package com.example.thirdtool.Card.domain.model;

import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.example.thirdtool.support.DomainFixture.*;
import static org.assertj.core.api.Assertions.*;


@DisplayName("CardTag")
class CardTagTest {

    @Nested
    @DisplayName("link() — 정상 생성")
    class Create {

        @Test
        @DisplayName("유효한 card와 tag로 CardTag를 생성하면 linkedAt이 자동 기록된다")
        void link_validCardAndTag_setsLinkedAt() {
            // given
            Card card = sampleCard();
            Tag tag = sampleTag("트랜잭션");

            // when
            CardTag cardTag = CardTag.link(card, tag);

            // then
            assertThat(cardTag.getCard()).isSameAs(card);
            assertThat(cardTag.getTag()).isSameAs(tag);
            assertThat(cardTag.getLinkedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("link() — 유효성 검증")
    class Validation {

        @Test
        @DisplayName("card가 null이면 INVALID_INPUT 예외가 발생한다")
        void link_nullCard_throwsInvalidInputException() {
            // given
            Tag tag = sampleTag("트랜잭션");

            // when & then
            assertThatThrownBy(() -> CardTag.link(null, tag))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("tag가 null이면 INVALID_INPUT 예외가 발생한다")
        void link_nullTag_throwsInvalidInputException() {
            // given
            Card card = sampleCard();

            // when & then
            assertThatThrownBy(() -> CardTag.link(card, null))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }
    }
}