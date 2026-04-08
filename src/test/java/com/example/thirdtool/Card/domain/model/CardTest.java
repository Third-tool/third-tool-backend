package com.example.thirdtool.Card.domain.model;

import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.example.thirdtool.support.DomainFixture.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Card (Aggregate Root)")
class CardTest {

    // =========================================================================
    // 생성
    // =========================================================================

    @Nested
    @DisplayName("create() — 정상 생성")
    class CreateTest {

        @Test
        @DisplayName("태그 포함 생성 시 초기 상태(ON_FIELD, viewCount=0, enteredFieldAt)가 설정된다")
        void create_withTags_initializesCorrectly() {
            // given
            Tag tag = sampleTagWithId(1L, "트랜잭션");

            // when
            Card card = Card.create(
                    sampleDeck(),
                    MainNote.of("내용", null),
                    Summary.of("요약."),
                    List.of("키워드1"),
                    List.of(tag)
                                   );

            // then
            assertThat(card.getStatus()).isEqualTo(CardStatus.ON_FIELD);
            assertThat(card.getViewCount()).isEqualTo(0);
            assertThat(card.getEnteredFieldAt()).isNotNull();
            assertThat(card.getCardTags()).hasSize(1);
        }

        @Test
        @DisplayName("태그 없이 생성하면 cardTags가 빈 목록으로 초기화된다")
        void create_withNullTags_initializesWithEmptyCardTags() {
            // given - tags=null

            // when
            Card card = Card.create(
                    sampleDeck(),
                    MainNote.of("내용", null),
                    Summary.of("요약."),
                    List.of("키워드1"),
                    null
                                   );

            // then
            assertThat(card.getCardTags()).isEmpty();
        }

        @Test
        @DisplayName("생성된 카드의 초기 status는 ON_FIELD이다")
        void create_initialStatus_isOnField() {
            // given - no setup

            // when
            Card card = sampleCard();

            // then
            assertThat(card.getStatus()).isEqualTo(CardStatus.ON_FIELD);
        }

        @Test
        @DisplayName("생성된 카드의 초기 viewCount는 0이다")
        void create_initialViewCount_isZero() {
            // given - no setup

            // when
            Card card = sampleCard();

            // then
            assertThat(card.getViewCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("생성된 KeywordCue는 해당 Card 인스턴스를 참조한다")
        void create_keywordCues_referenceCreatedCard() {
            // given - no setup

            // when
            Card card = sampleCard();

            // then
            assertThat(card.getKeywordCues())
                    .allSatisfy(cue -> assertThat(cue.getCard()).isSameAs(card));
        }
    }

    @Nested
    @DisplayName("create() — 유효성 검증")
    class CreateValidationTest {

        @Test
        @DisplayName("deck이 null이면 INVALID_INPUT 예외가 발생한다")
        void create_nullDeck_throwsInvalidInputException() {
            // given - no setup

            // when & then
            assertThatThrownBy(() -> Card.create(
                    null,
                    MainNote.of("내용", null),
                    Summary.of("요약."),
                    List.of("키워드")
                                                ))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("keywords가 비어 있으면 CARD_KEYWORD_MIN_REQUIRED 예외가 발생한다")
        void create_emptyKeywords_throwsKeywordMinRequiredException() {
            // given - no setup

            // when & then
            assertThatThrownBy(() -> Card.create(
                    sampleDeck(),
                    MainNote.of("내용", null),
                    Summary.of("요약."),
                    List.of()
                                                ))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CARD_KEYWORD_MIN_REQUIRED);
        }

        @Test
        @DisplayName("태그가 4개 이상이면 CARD_TAG_LIMIT_EXCEEDED 예외가 발생한다")
        void create_fourTags_throwsTagLimitExceededException() {
            // given
            List<Tag> fourTags = List.of(
                    sampleTagWithId(1L, "A"), sampleTagWithId(2L, "B"),
                    sampleTagWithId(3L, "C"), sampleTagWithId(4L, "D")
                                        );

            // when & then
            assertThatThrownBy(() -> Card.create(
                    sampleDeck(), MainNote.of("내용", null), Summary.of("요약."),
                    List.of("키워드"), fourTags
                                                ))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CARD_TAG_LIMIT_EXCEEDED);
        }
    }

    // =========================================================================
    // 학습 구조 수정
    // =========================================================================

    @Nested
    @DisplayName("changeMainNote()")
    class ChangeMainNoteTest {

        @Test
        @DisplayName("유효한 값으로 MainNote를 수정할 수 있다")
        void changeMainNote_validInput_updatesMainNote() {
            // given
            Card card = sampleCard();

            // when
            card.changeMainNote("수정 내용", null);

            // then
            assertThat(card.getMainNote().getTextContent()).isEqualTo("수정 내용");
            assertThat(card.getMainNote().getContentType()).isEqualTo(MainContentType.TEXT_ONLY);
        }

        @Test
        @DisplayName("텍스트와 이미지가 모두 blank이면 CARD_MAIN_NOTE_EMPTY 예외가 발생한다")
        void changeMainNote_bothBlank_throwsMainNoteEmptyException() {
            // given
            Card card = sampleCard();

            // when & then
            assertThatThrownBy(() -> card.changeMainNote("", null))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CARD_MAIN_NOTE_EMPTY);
        }
    }

    @Nested
    @DisplayName("changeSummary()")
    class ChangeSummaryTest {

        @Test
        @DisplayName("유효한 값으로 Summary를 수정할 수 있다")
        void changeSummary_validInput_updatesSummary() {
            // given
            Card card = sampleCard();

            // when
            card.changeSummary("수정된 요약.");

            // then
            assertThat(card.getSummary().getValue()).isEqualTo("수정된 요약.");
        }

        @Test
        @DisplayName("4문장으로 수정하면 CARD_SUMMARY_SENTENCE_OUT_OF_RANGE 예외가 발생한다")
        void changeSummary_fourSentences_throwsSentenceOutOfRangeException() {
            // given
            Card card = sampleCard();

            // when & then
            assertThatThrownBy(() -> card.changeSummary("1. 2. 3. 4."))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CARD_SUMMARY_SENTENCE_OUT_OF_RANGE);
        }
    }

    // =========================================================================
    // Keyword 수정
    // =========================================================================

    @Nested
    @DisplayName("replaceKeywords()")
    class ReplaceKeywordsTest {

        @Test
        @DisplayName("새 목록으로 교체하면 기존 키워드가 모두 제거되고 새 키워드로 대체된다")
        void replaceKeywords_validList_replacesAllKeywords() {
            // given
            Card card = sampleCard(); // LIFO, push, pop

            // when
            card.replaceKeywords(List.of("새단서1", "새단서2"));

            // then
            assertThat(card.getKeywordCues()).hasSize(2);
            assertThat(card.getKeywordCues())
                    .extracting(KeywordCue::getValue)
                    .containsExactly("새단서1", "새단서2")
                    .doesNotContain("LIFO", "push", "pop");
        }

        @Test
        @DisplayName("빈 목록으로 교체하면 CARD_KEYWORD_MIN_REQUIRED 예외가 발생한다")
        void replaceKeywords_emptyList_throwsKeywordMinRequiredException() {
            // given
            Card card = sampleCard();

            // when & then
            assertThatThrownBy(() -> card.replaceKeywords(List.of()))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CARD_KEYWORD_MIN_REQUIRED);
        }
    }

    @Nested
    @DisplayName("removeKeyword()")
    class RemoveKeywordTest {

        @Test
        @DisplayName("마지막 keyword를 제거하면 CARD_KEYWORD_LAST_CANNOT_REMOVE 예외가 발생한다")
        void removeKeyword_lastKeyword_throwsLastCannotRemoveException() {
            // given
            Card card = sampleCard();
            card.replaceKeywords(List.of("마지막단서"));
            Long lastKeywordId = card.getKeywordCues().get(0).getId();

            // when & then
            assertThatThrownBy(() -> card.removeKeyword(lastKeywordId))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CARD_KEYWORD_LAST_CANNOT_REMOVE);
        }

        @Test
        @DisplayName("존재하지 않는 keywordCueId로 제거하면 CARD_KEYWORD_NOT_FOUND 예외가 발생한다")
        void removeKeyword_notFound_throwsKeywordNotFoundException() {
            // given
            Card card = sampleCard();
            Long nonExistentId = 9999L;

            // when & then
            assertThatThrownBy(() -> card.removeKeyword(nonExistentId))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CARD_KEYWORD_NOT_FOUND);
        }
    }

    // =========================================================================
    // 태그 관리
    // =========================================================================

    @Nested
    @DisplayName("addTag()")
    class AddTagTest {

        @Test
        @DisplayName("유효한 태그를 추가하면 cardTags에 반영된다")
        void addTag_valid_addsTag() {
            // given
            Card card = sampleCard();
            card.addTag(sampleTagWithId(1L, "A"));
            card.addTag(sampleTagWithId(2L, "B"));
            Tag newTag = sampleTagWithId(3L, "C");

            // when
            card.addTag(newTag);

            // then
            assertThat(card.getCardTags()).hasSize(3);
        }

        @Test
        @DisplayName("이미 3개인 상태에서 추가하면 CARD_TAG_LIMIT_EXCEEDED 예외가 발생한다")
        void addTag_alreadyThreeTags_throwsTagLimitExceededException() {
            // given
            Card card = sampleCard();
            card.addTag(sampleTagWithId(1L, "A"));
            card.addTag(sampleTagWithId(2L, "B"));
            card.addTag(sampleTagWithId(3L, "C"));

            // when & then
            assertThatThrownBy(() -> card.addTag(sampleTagWithId(4L, "D")))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CARD_TAG_LIMIT_EXCEEDED);
        }

        @Test
        @DisplayName("이미 연결된 태그를 다시 추가하면 CARD_TAG_ALREADY_EXISTS 예외가 발생한다")
        void addTag_duplicateTag_throwsTagAlreadyExistsException() {
            // given
            Card card = sampleCard();
            Tag tag = sampleTagWithId(1L, "트랜잭션");
            card.addTag(tag);

            // when & then
            assertThatThrownBy(() -> card.addTag(tag))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CARD_TAG_ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("removeTag()")
    class RemoveTagTest {

        @Test
        @DisplayName("연결된 태그를 제거하면 cardTags에서 삭제된다")
        void removeTag_valid_removesTag() {
            // given
            Card card = sampleCard();
            Tag tag = sampleTagWithId(1L, "트랜잭션");
            card.addTag(tag);

            // when
            card.removeTag(tag.getId());

            // then
            assertThat(card.getCardTags()).isEmpty();
        }

        @Test
        @DisplayName("연결되지 않은 tagId로 제거하면 CARD_TAG_NOT_FOUND 예외가 발생한다")
        void removeTag_notFound_throwsTagNotFoundException() {
            // given
            Card card = sampleCard();
            Long nonExistentTagId = 9999L;

            // when & then
            assertThatThrownBy(() -> card.removeTag(nonExistentTagId))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CARD_TAG_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("replaceTags()")
    class ReplaceTagsTest {

        @Test
        @DisplayName("4개 이상의 목록으로 교체하면 CARD_TAG_LIMIT_EXCEEDED 예외가 발생한다")
        void replaceTags_fourOrMoreTags_throwsTagLimitExceededException() {
            // given
            Card card = sampleCard();
            List<Tag> fourTags = List.of(
                    sampleTagWithId(1L, "A"), sampleTagWithId(2L, "B"),
                    sampleTagWithId(3L, "C"), sampleTagWithId(4L, "D")
                                        );

            // when & then
            assertThatThrownBy(() -> card.replaceTags(fourTags))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CARD_TAG_LIMIT_EXCEEDED);
        }

        @Test
        @DisplayName("null로 교체하면 기존 cardTags가 모두 제거된다")
        void replaceTags_null_clearsAllTags() {
            // given
            Card card = sampleCard();
            card.addTag(sampleTagWithId(1L, "기존태그"));

            // when
            card.replaceTags(null);

            // then
            assertThat(card.getCardTags()).isEmpty();
        }
    }

    // =========================================================================
    // 상태 전환 (CardStatus)
    // =========================================================================

    @Nested
    @DisplayName("archive() / returnToField()")
    class StatusTransitionTest {

        @Test
        @DisplayName("ON_FIELD 상태에서 archive()를 호출하면 ARCHIVE로 전환된다")
        void archive_onField_changesStatusToArchive() {
            // given
            Card card = sampleCard();

            // when
            card.archive();

            // then
            assertThat(card.getStatus()).isEqualTo(CardStatus.ARCHIVE);
        }

        @Test
        @DisplayName("이미 ARCHIVE 상태에서 archive()를 호출하면 상태와 필드값이 유지된다 — 멱등성 보장")
        void archive_alreadyArchived_isIdempotent() {
            // given
            Card card = sampleCard();
            card.recordView(); // viewCount=1
            card.archive();
            int viewCountBeforeSecondCall = card.getViewCount();

            // when
            card.archive();

            // then
            assertThat(card.getStatus()).isEqualTo(CardStatus.ARCHIVE);
            assertThat(card.getViewCount()).isEqualTo(viewCountBeforeSecondCall);
        }

        @Test
        @DisplayName("archive() 호출 후 enteredFieldAt이 보존된다 — 마지막 ON_FIELD 구간 통계 근거")
        void archive_enteredFieldAt_preserved() {
            // given
            Card card = sampleCard();
            LocalDateTime enteredFieldAtBefore = card.getEnteredFieldAt();

            // when
            card.archive();

            // then
            assertThat(card.getEnteredFieldAt()).isEqualTo(enteredFieldAtBefore);
        }

        @Test
        @DisplayName("ARCHIVE에서 returnToField() 호출 시 viewCount=0, lastViewedAt=null, enteredFieldAt이 재기록된다")
        void returnToField_archived_resetsAllFields() {
            // given
            Card card = sampleCard();
            card.recordView(); // viewCount=1, lastViewedAt 설정
            card.archive();

            // when
            card.returnToField();

            // then
            // 하나라도 누락 시 이전 구간 데이터로 schedule 오판 발생
            assertThat(card.getStatus()).isEqualTo(CardStatus.ON_FIELD);
            assertThat(card.getViewCount()).isEqualTo(0);
            assertThat(card.getLastViewedAt()).isNull();
        }

        @Test
        @DisplayName("이미 ON_FIELD 상태에서 returnToField()를 호출하면 enteredFieldAt이 재기록되지 않는다 — 멱등성 보장")
        void returnToField_alreadyOnField_isIdempotent() {
            // given
            Card card = sampleCard();
            LocalDateTime enteredFieldAtBefore = card.getEnteredFieldAt();

            // when
            card.returnToField();

            // then
            assertThat(card.getEnteredFieldAt()).isEqualTo(enteredFieldAtBefore);
        }
    }

    // =========================================================================
    // ON_FIELD 체류 추적
    // =========================================================================

    @Nested
    @DisplayName("recordView() / isMaxViewReached() / isLastView()")
    class ViewTrackingTest {

        @Test
        @DisplayName("ON_FIELD 카드에 recordView()를 호출하면 viewCount가 증가하고 lastViewedAt이 갱신된다")
        void recordView_onFieldCard_incrementsViewCountAndSetsLastViewedAt() {
            // given
            Card card = sampleCard();

            // when
            card.recordView();

            // then
            assertThat(card.getViewCount()).isEqualTo(1);
            assertThat(card.getLastViewedAt()).isNotNull();
        }

        @Test
        @DisplayName("ARCHIVE 카드에 recordView()를 호출하면 아무 변화가 없다")
        void recordView_archivedCard_isIgnored() {
            // given
            Card card = sampleCard();
            card.archive();

            // when
            card.recordView();

            // then
            assertThat(card.getViewCount()).isEqualTo(0);
            assertThat(card.getLastViewedAt()).isNull();
        }

        @Test
        @DisplayName("viewCount가 maxView에 도달하면 isMaxViewReached()가 true를 반환한다")
        void isMaxViewReached_viewCountEqualsMaxView_returnsTrue() {
            // given
            Card card = sampleCard();
            card.recordView();
            card.recordView();
            card.recordView(); // viewCount=3

            // when
            boolean reached = card.isMaxViewReached(3);

            // then
            assertThat(reached).isTrue();
        }

        @Test
        @DisplayName("viewCount가 maxView 미만이면 isMaxViewReached()가 false를 반환한다")
        void isMaxViewReached_viewCountBelowMaxView_returnsFalse() {
            // given
            Card card = sampleCard();
            card.recordView();
            card.recordView(); // viewCount=2

            // when
            boolean reached = card.isMaxViewReached(3);

            // then
            assertThat(reached).isFalse();
        }

        @Test
        @DisplayName("maxView가 0 이하이면 isMaxViewReached()는 항상 false를 반환한다")
        void isMaxViewReached_zeroOrNegativeMaxView_returnsFalse() {
            // given
            Card card = sampleCard();

            // when
            boolean reached = card.isMaxViewReached(0);

            // then
            assertThat(reached).isFalse();
        }

        @Test
        @DisplayName("recordView() 후 viewCount가 maxView와 같으면 isLastView()가 true를 반환한다")
        void isLastView_viewCountEqualsMaxView_returnsTrue() {
            // given
            Card card = sampleCard();
            card.recordView();
            card.recordView();
            card.recordView(); // viewCount=3 = maxView

            // when
            boolean isLast = card.isLastView(3);

            // then
            assertThat(isLast).isTrue();
        }
    }

    @Nested
    @DisplayName("isDurationExceeded() / isScheduleAvailable()")
    class TimeBasedTrackingTest {

        @Test
        @DisplayName("체류 기간이 maxDuration을 초과하면 isDurationExceeded()가 true를 반환한다")
        void isDurationExceeded_durationExceeded_returnsTrue() {
            // given
            Card card = sampleCardWithEnteredFieldAt(LocalDateTime.now().minusDays(11));

            // when
            boolean exceeded = card.isDurationExceeded(Duration.ofDays(10));

            // then
            assertThat(exceeded).isTrue();
        }

        @Test
        @DisplayName("enteredFieldAt이 null이면 isDurationExceeded()가 false를 반환한다 — NPE 방어")
        void isDurationExceeded_nullEnteredFieldAt_returnsFalse() {
            // given
            Card card = sampleCard();
            ReflectionTestUtils.setField(card, "enteredFieldAt", null);

            // when
            boolean exceeded = card.isDurationExceeded(Duration.ofDays(10));

            // then
            assertThat(exceeded).isFalse();
        }

        @Test
        @DisplayName("lastViewedAt이 null이면 isScheduleAvailable()이 true를 반환한다 — 신규 카드 즉시 가용")
        void isScheduleAvailable_nullLastViewedAt_returnsTrue() {
            // given
            Card card = sampleCard(); // lastViewedAt=null

            // when
            boolean available = card.isScheduleAvailable(Duration.ofDays(1));

            // then
            assertThat(available).isTrue();
        }

        @Test
        @DisplayName("최소 간격이 충족되면 isScheduleAvailable()이 true를 반환한다")
        void isScheduleAvailable_intervalMet_returnsTrue() {
            // given
            Card card = sampleCardWithLastViewedAt(LocalDateTime.now().minusDays(2));

            // when
            boolean available = card.isScheduleAvailable(Duration.ofDays(1));

            // then
            assertThat(available).isTrue();
        }

        @Test
        @DisplayName("최소 간격이 미충족되면 isScheduleAvailable()이 false를 반환한다")
        void isScheduleAvailable_intervalNotMet_returnsFalse() {
            // given
            Card card = sampleCardWithLastViewedAt(LocalDateTime.now().minusHours(12));

            // when
            boolean available = card.isScheduleAvailable(Duration.ofDays(1));

            // then
            assertThat(available).isFalse();
        }

        @Test
        @DisplayName("minInterval이 null이면 isScheduleAvailable()이 항상 true를 반환한다")
        void isScheduleAvailable_nullMinInterval_returnsTrue() {
            // given
            Card card = sampleCardWithLastViewedAt(LocalDateTime.now().minusMinutes(1));

            // when
            boolean available = card.isScheduleAvailable(null);

            // then
            assertThat(available).isTrue();
        }
    }
}