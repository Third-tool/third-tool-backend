package com.example.thirdtool.Card.domain.model;

import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import static com.example.thirdtool.support.DomainFixture.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;


@DisplayName("Card 도메인 모델")
class CardDomainTest {
    com.example.thirdtool.support.DomainFixture domainFixture = new com.example.thirdtool.support.DomainFixture();

    // =========================================================================
    // MainNote - 순수 자바 객체로 테스트
    // =========================================================================
    // Card 생성 폴더
    @Nested
    @DisplayName("MainNote 생성")
    class MainNoteCreate {

        @Test
        @DisplayName("텍스트만 있으면 TEXT_ONLY 타입으로 생성된다")
        void textOnly() {
            MainNote note = MainNote.of("학습 내용", null);

            assertAll(
                    () -> assertThat(note.getContentType()).isEqualTo(MainContentType.TEXT_ONLY),
                    () -> assertThat(note.getTextContent()).isEqualTo("학습 내용"),
                    () -> assertThat(note.getImageUrl()).isNull()
                     );
        }

        @Test
        @DisplayName("이미지만 있으면 IMAGE_ONLY 타입으로 생성된다")
        void imageOnly() {
            MainNote note = MainNote.of(null, "https://img.example.com/a.png");

            assertAll(
                    () -> assertThat(note.getContentType()).isEqualTo(MainContentType.IMAGE_ONLY),
                    () -> assertThat(note.getImageUrl()).isEqualTo("https://img.example.com/a.png"),
                    () -> assertThat(note.getTextContent()).isNull()
                     );
        }

        @Test
        @DisplayName("텍스트와 이미지가 함께 있으면 MIXED 타입으로 생성된다")
        void mixed() {
            MainNote note = MainNote.of("텍스트", "https://img.example.com/a.png");

            assertThat(note.getContentType()).isEqualTo(MainContentType.MIXED);
        }

        @Test
        @DisplayName("텍스트는 저장 전에 trim 처리된다")
        void textIsTrimmed() {
            MainNote note = MainNote.of("  내용  ", null);

            assertThat(note.getTextContent()).isEqualTo("내용");
        }
    }

    // MainNote 검증 폴더
    @Nested
    @DisplayName("MainNote 검증")
    class MainNoteValidation {

        @Test
        @DisplayName("텍스트와 이미지가 모두 null 이면 CARD_MAIN_NOTE_EMPTY 예외가 발생한다")
        void bothNull() {
            CardDomainException ex = assertThrows(CardDomainException.class,
                    () -> MainNote.of(null, null));

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CARD_MAIN_NOTE_EMPTY);
        }

        @Test
        @DisplayName("텍스트와 이미지가 모두 공백이면 CARD_MAIN_NOTE_EMPTY 예외가 발생한다")
        void bothBlank() {
            CardDomainException ex = assertThrows(CardDomainException.class,
                    () -> MainNote.of("  ", "   "));

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CARD_MAIN_NOTE_EMPTY);
        }
    }

    // =========================================================================
    // Summary
    // =========================================================================

    @Nested
    @DisplayName("Summary 생성")
    class SummaryCreate {

        @Test
        @DisplayName("1문장 요약을 생성할 수 있다. ")
        void oneSentence() {
            Summary summary = Summary.of("스택은 lifo이다. ");

            assertThat(summary.getValue()).isEqualTo("스택은 lifo이다. ");
        }

        @Test
        @DisplayName("3문장 요약을 생성할 수 있다")
        void threeSentences() {
            assertThatCode(() -> Summary.of("첫째. 둘째. 셋째."))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("구분자 없는 단일 문장도 허용된다")
        void singleSentenceWithoutDelimiter() {
            assertThatCode(() -> Summary.of("구분자 없는 문장"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("value는 저장 전에 trim 처리된다")
        void valueIsTrimmed() {
            Summary summary = Summary.of("  핵심 내용.  ");

            assertThat(summary.getValue()).isEqualTo("핵심 내용.");
        }
    }

    @Nested
    @DisplayName("Summary 검증")
    class SummaryValidation {

        @Test
        @DisplayName("null 이면 CARD_SUMMARY_EMPTY 예외가 빌생합니다.")
        void nullValue() {
            //given
            //when
            CardDomainException ex=assertThrows(CardDomainException.class,
                    () -> Summary.of(null));

            //Then
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CARD_SUMMARY_EMPTY);
        }

        @Test
        @DisplayName("공백이면 CARD_SUMMARY_EMPTY 예외가 발생한다")
        void blankValue() {
            CardDomainException ex = assertThrows(CardDomainException.class,
                    () -> Summary.of("   "));

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CARD_SUMMARY_EMPTY);
        }

//        @Test
//        @DisplayName("4문장 이상이면 CARD_SUMMARY_SENTENCE_OUT_OF_RANGE 예외가 발생하고 현재 문장 수가 메시지에 포함된다")
//        void moreThanThreeSentences() {
//            CardDomainException ex = assertThrows(CardDomainException.class,
//                    () -> Summary.of("1. 2. 3. 4."));
//
//            assertAll(
//                    () -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CARD_SUMMARY_SENTENCE_OUT_OF_RANGE),
//                    () -> assertThat(ex.getMessage()).contains("현재 4문장")
//                     );
//        }
        @Test
        @DisplayName("4문장 이상이면, CARD_SUMMARY_SENTENCE_OUT_OF_RANGE 예외 발생하고 현재 문장 수가 메시지에 포함됨")
        void moreThanThreeSentence(){
            //given
            //when
            CardDomainException ex=assertThrows(CardDomainException.class,
                    ()-> Summary.of("1. 2. 3. 4."));

            //Then
            assertAll(
                    ()-> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CARD_SUMMARY_SENTENCE_OUT_OF_RANGE),
                    ()-> assertThat(ex.getMessage()).contains("현재 4문장")
                     );
        }

    }

    // =========================================================================
    // KeywordCue
    // =========================================================================

    @Nested
    @DisplayName("KeywordCue 검증")
    class KeywordCueValidation {

        @Test
        @DisplayName("value가 공백이면 CARD_KEYWORD_BLANK 예외가 발생한다")
        void blankValue() {
            //given
            Card card = com.example.thirdtool.support.DomainFixture.sampleCard();

            CardDomainException ex = assertThrows(CardDomainException.class,
                    () -> KeywordCue.create(card, "  "));

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CARD_KEYWORD_BLANK);
        }

        @Test
        @DisplayName("card가 null이면 INVALID_INPUT 예외가 발생한다")
        void nullCard() {
            CardDomainException ex = assertThrows(CardDomainException.class,
                    () -> KeywordCue.create(null, "키워드"));

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
        }
    }


    // =========================================================================
    // Card 생성
    // =========================================================================

    @Nested
    @DisplayName("Card.create()")
    class CardCreate {
        @Test
        @DisplayName("유효한 입력이면 카드가 생긴다.")
        void success() {
            //given
            Card card = com.example.thirdtool.support.DomainFixture.sampleCard();
            //when
            //Then
            assertAll(
                    ()->assertThat(card).isNotNull(),
                    ()->assertThat(card.getKeywordCues()).hasSize(3),
                    ()->assertThat(card.getKeywordCues())
                            .extracting(KeywordCue::getValue )
                            .containsExactly("LIFO", "push", "pop")
                     );
        }

        @Test
        @DisplayName("생성된 KeywordCue는 해당 Card 인스턴스를 참조한다")
        void keywordCueBelongsToCard() {
            Card card = com.example.thirdtool.support.DomainFixture.sampleCard();

            assertThat(card.getKeywordCues())
                    .allSatisfy(cue -> assertThat(cue.getCard()).isSameAs(card));
        }

        @Test
        @DisplayName("mainNote가 null이면 INVALID_INPUT 예외가 발생한다")
        void nullMainNote() {
            CardDomainException ex = assertThrows(CardDomainException.class,
                    () -> Card.create(null,MainNote.of("내용", null), Summary.of("요약."),List.of("키워드")));

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("summary가 null이면 INVALID_INPUT 예외가 발생한다")
        void nullSummary() {
            CardDomainException ex = assertThrows(CardDomainException.class,
                    () -> Card.create(com.example.thirdtool.support.DomainFixture.sampleDeck(),MainNote.of("내용", null), null, List.of("키워드")));

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("키워드 목록이 비어있으면 CARD_KEYWORD_MIN_REQUIRED 예외가 발생한다")
        void emptyKeywords() {
            CardDomainException ex = assertThrows(CardDomainException.class,
                    () -> Card.create(com.example.thirdtool.support.DomainFixture.sampleDeck(),MainNote.of("내용", null), Summary.of("요약."), List.of()));

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CARD_KEYWORD_MIN_REQUIRED);
        }
    }


    // =========================================================================
    // Card 수정
    // =========================================================================

    @Nested
    @DisplayName("Card.changeMainNote()")
    class ChangeMainNote {

        @Test
        @DisplayName("유효한 값으로 MainNote를 수정할 수 있다")
        void success() {
            Card card = com.example.thirdtool.support.DomainFixture.sampleCard();
            card.changeMainNote("수정 내용", null);

            assertThat(card.getMainNote().getTextContent()).isEqualTo("수정 내용");
        }

        @Test
        @DisplayName("텍스트와 이미지가 모두 공백이면 CARD_MAIN_NOTE_EMPTY 예외가 발생한다")
        void bothBlank() {
            Card card = com.example.thirdtool.support.DomainFixture.sampleCard();

            CardDomainException ex = assertThrows(CardDomainException.class,
                    () -> card.changeMainNote("", null));

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CARD_MAIN_NOTE_EMPTY);
        }
    }

    @Nested
    @DisplayName("Card.changeSummary()")
    class ChangeSummary {

        @Test
        @DisplayName("유효한 값으로 Summary를 수정할 수 있다")
        void success() {
            Card card = com.example.thirdtool.support.DomainFixture.sampleCard();
            card.changeSummary("수정된 요약.");

            assertThat(card.getSummary().getValue()).isEqualTo("수정된 요약.");
        }

        @Test
        @DisplayName("4문장으로 수정하면 CARD_SUMMARY_SENTENCE_OUT_OF_RANGE 예외가 발생한다")
        void moreThanThreeSentences() {
            Card card = com.example.thirdtool.support.DomainFixture.sampleCard();

            CardDomainException ex = assertThrows(CardDomainException.class,
                    () -> card.changeSummary("1. 2. 3. 4."));

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CARD_SUMMARY_SENTENCE_OUT_OF_RANGE);
        }
    }

    // =========================================================================
    // Keywords 교체
    // =========================================================================

    @Nested
    @DisplayName("Card.replaceKeywords()")
    class ReplaceKeywords {

        @Test
        @DisplayName("새 목록으로 전체 교체되고 기존 키워드는 사라진다")
        void success() {
            Card card = com.example.thirdtool.support.DomainFixture.sampleCard();
            card.replaceKeywords(List.of("스택", "큐", "덱"));

            assertAll(
                    () -> assertThat(card.getKeywordCues()).hasSize(3),
                    () -> assertThat(card.getKeywordCues())
                            .extracting(KeywordCue::getValue)
                            .containsExactly("스택", "큐", "덱")
                            .doesNotContain("LIFO", "push", "pop")
                     );
        }

        @Test
        @DisplayName("빈 목록으로 교체하면 CARD_KEYWORD_MIN_REQUIRED 예외가 발생한다")
        void emptyList() {
            Card card = com.example.thirdtool.support.DomainFixture.sampleCard();

            CardDomainException ex = assertThrows(CardDomainException.class,
                    () -> card.replaceKeywords(List.of()));

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CARD_KEYWORD_MIN_REQUIRED);
        }
    }




}