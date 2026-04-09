package com.example.thirdtool.Card.domain.model;

import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.thirdtool.support.DomainFixture.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("RelatedCardCandidate")
class RelatedCardCandidateTest {

    @Nested
    @DisplayName("of() — 정상 생성")
    class Create {

        @Test
        @DisplayName("sharedTagCount는 외부 주입 없이 sharedTags.size()로 자동 계산된다")
        void of_validInput_calculatesSharedTagCountFromSize() {
            // given
            Card card = sampleCard();
            Tag tag1 = sampleTagWithId(1L, "트랜잭션");
            Tag tag2 = sampleTagWithId(2L, "DB");

            // when
            RelatedCardCandidate candidate = RelatedCardCandidate.of(card, List.of(tag1, tag2));

            // then
            assertThat(candidate.getSharedTagCount()).isEqualTo(2);
            assertThat(candidate.getCard()).isSameAs(card);
            assertThat(candidate.getSharedTags()).containsExactly(tag1, tag2);
        }
    }

    @Nested
    @DisplayName("of() — 유효성 검증")
    class Validation {

        @Test
        @DisplayName("card가 null이면 INVALID_INPUT 예외가 발생한다")
        void of_nullCard_throwsInvalidInputException() {
            // given
            Tag tag = sampleTagWithId(1L, "트랜잭션");

            // when & then
            assertThatThrownBy(() -> RelatedCardCandidate.of(null, List.of(tag)))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("sharedTags가 비어 있으면 INVALID_INPUT 예외가 발생한다")
        void of_emptySharedTags_throwsInvalidInputException() {
            // given
            Card card = sampleCard();

            // when & then
            assertThatThrownBy(() -> RelatedCardCandidate.of(card, List.of()))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("sharedTags가 null이면 INVALID_INPUT 예외가 발생한다")
        void of_nullSharedTags_throwsInvalidInputException() {
            // given
            Card card = sampleCard();

            // when & then
            assertThatThrownBy(() -> RelatedCardCandidate.of(card, null))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }
    }
}