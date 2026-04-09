package com.example.thirdtool.Card.domain.model;

import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.example.thirdtool.support.DomainFixture.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("CardStatusHistory")
class CardStatusHistoryTest {

    @Nested
    @DisplayName("of() — 정상 생성")
    class Create {

        @Test
        @DisplayName("ON_FIELD → ARCHIVE 방향에 reason이 있으면 이력이 생성된다")
        void of_onFieldToArchiveWithReason_success() {
            // given
            Card card = sampleCard();

            // when & then
            assertThatCode(() ->
                    CardStatusHistory.of(card, CardStatus.ON_FIELD, CardStatus.ARCHIVE, ArchiveReason.MANUAL))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ARCHIVE → ON_FIELD 방향에 reason=null이면 이력이 생성된다")
        void of_archiveToOnFieldWithoutReason_success() {
            // given
            Card card = sampleCard();

            // when & then
            assertThatCode(() ->
                    CardStatusHistory.of(card, CardStatus.ARCHIVE, CardStatus.ON_FIELD, null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("of() — 유효성 검증")
    class Validation {

        @Test
        @DisplayName("fromStatus와 toStatus가 같으면 INVALID_INPUT 예외가 발생한다")
        void of_sameFromAndToStatus_throwsInvalidInputException() {
            // given
            Card card = sampleCard();

            // when & then
            assertThatThrownBy(() ->
                    CardStatusHistory.of(card, CardStatus.ON_FIELD, CardStatus.ON_FIELD, null))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("ARCHIVE 방향 이력에 reason이 없으면 INVALID_INPUT 예외가 발생한다")
        void of_archiveStatusWithoutReason_throwsInvalidInputException() {
            // given
            Card card = sampleCard();

            // when & then
            assertThatThrownBy(() ->
                    CardStatusHistory.of(card, CardStatus.ON_FIELD, CardStatus.ARCHIVE, null))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("ON_FIELD 복귀 방향 이력에 reason이 있으면 INVALID_INPUT 예외가 발생한다")
        void of_onFieldStatusWithReason_throwsInvalidInputException() {
            // given
            Card card = sampleCard();

            // when & then
            assertThatThrownBy(() ->
                    CardStatusHistory.of(card, CardStatus.ARCHIVE, CardStatus.ON_FIELD, ArchiveReason.MANUAL))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("card가 null이면 INVALID_INPUT 예외가 발생한다")
        void of_nullCard_throwsInvalidInputException() {
            // given - no setup

            // when & then
            assertThatThrownBy(() ->
                    CardStatusHistory.of(null, CardStatus.ON_FIELD, CardStatus.ARCHIVE, ArchiveReason.MANUAL))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("fromStatus가 null이면 INVALID_INPUT 예외가 발생한다")
        void of_nullFromStatus_throwsInvalidInputException() {
            // given
            Card card = sampleCard();

            // when & then
            assertThatThrownBy(() ->
                    CardStatusHistory.of(card, null, CardStatus.ARCHIVE, ArchiveReason.MANUAL))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("toStatus가 null이면 INVALID_INPUT 예외가 발생한다")
        void of_nullToStatus_throwsInvalidInputException() {
            // given
            Card card = sampleCard();

            // when & then
            assertThatThrownBy(() ->
                    CardStatusHistory.of(card, CardStatus.ON_FIELD, null, ArchiveReason.MANUAL))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }
    }
}