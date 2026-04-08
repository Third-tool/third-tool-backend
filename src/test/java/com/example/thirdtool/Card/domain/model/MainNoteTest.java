package com.example.thirdtool.Card.domain.model;

import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("MainNote")
class MainNoteTest {

    @Nested
    @DisplayName("of() — 정상 생성")
    class Create {

        @Test
        @DisplayName("텍스트만 있으면 TEXT_ONLY 타입으로 생성된다")
        void of_textOnly_returnsTextOnlyContentType() {
            // given
            String textContent = "학습 내용";

            // when
            MainNote note = MainNote.of(textContent, null);

            // then
            assertThat(note.getContentType()).isEqualTo(MainContentType.TEXT_ONLY);
            assertThat(note.getTextContent()).isEqualTo("학습 내용");
            assertThat(note.getImageUrl()).isNull();
        }

        @Test
        @DisplayName("이미지만 있으면 IMAGE_ONLY 타입으로 생성된다")
        void of_imageOnly_returnsImageOnlyContentType() {
            // given - 그냥 테스트용
            String imageUrl = "https://img.example.com/a.png";

            // when
            MainNote note = MainNote.of(null, imageUrl);

            // then
            assertThat(note.getContentType()).isEqualTo(MainContentType.IMAGE_ONLY);
            assertThat(note.getTextContent()).isNull();
            assertThat(note.getImageUrl()).isEqualTo(imageUrl);
        }

        @Test
        @DisplayName("텍스트와 이미지가 함께 있으면 MIXED 타입으로 생성된다")
        void of_textAndImage_returnsMixedContentType() {
            // given
            String textContent = "텍스트";
            String imageUrl = "https://img.example.com/a.png";

            // when
            MainNote note = MainNote.of(textContent, imageUrl);

            // then
            assertThat(note.getContentType()).isEqualTo(MainContentType.MIXED);
        }

        @Test
        @DisplayName("textContent는 저장 전에 trim 처리된다")
        void of_textContentWithWhitespace_trimmedBeforeSaving() {
            // given
            String textContentWithWhitespace = "  내용  ";

            // when
            MainNote note = MainNote.of(textContentWithWhitespace, null);

            // then
            assertThat(note.getTextContent()).isEqualTo("내용");
        }

        @Test
        @DisplayName("텍스트가 blank이고 이미지가 있으면 IMAGE_ONLY로 분기된다")
        void of_blankTextWithValidImage_returnsImageOnlyContentType() {
            // given
            String blankText = "   ";
            String imageUrl = "https://img.example.com/a.png";

            // when
            MainNote note = MainNote.of(blankText, imageUrl);

            // then
            assertThat(note.getContentType()).isEqualTo(MainContentType.IMAGE_ONLY);
        }
    }

    @Nested
    @DisplayName("of() — 유효성 검증")
    class Validation {

        @Test
        @DisplayName("텍스트와 이미지가 모두 null이면 CARD_MAIN_NOTE_EMPTY 예외가 발생한다")
        void of_bothNull_throwsCardMainNoteEmptyException() {
            // given - no setup

            // when & then
            assertThatThrownBy(() -> MainNote.of(null, null))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CARD_MAIN_NOTE_EMPTY);
        }

        @Test
        @DisplayName("텍스트와 이미지가 모두 blank이면 CARD_MAIN_NOTE_EMPTY 예외가 발생한다")
        void of_bothBlank_throwsCardMainNoteEmptyException() {
            // given - no setup

            // when & then
            assertThatThrownBy(() -> MainNote.of("  ", "   "))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CARD_MAIN_NOTE_EMPTY);
        }
    }
}