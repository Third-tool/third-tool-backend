package com.example.thirdtool.Card.domain.model;

import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Card.infrastructure.persistence.CardStatusHistoryRepository;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.example.thirdtool.support.DomainFixture.sampleCard;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("CardStatusHistoryAppender")
class CardStatusHistoryAppenderTest {

    private CardStatusHistoryRepository historyRepository;
    private CardStatusHistoryAppender appender;

    @BeforeEach
    void setUp() {
        historyRepository = mock(CardStatusHistoryRepository.class);
        appender = new CardStatusHistoryAppender(historyRepository);
    }

    @Nested
    @DisplayName("append() — 멱등 no-op")
    class Idempotent {

        @Test
        @DisplayName("fromStatus == toStatus 인 호출은 이력을 생성하지 않는다 (ON_FIELD → ON_FIELD)")
        void append_sameStatus_onField_doesNotPersist() {
            // given
            Card card = sampleCard();

            // when
            appender.append(card, CardStatus.ON_FIELD, CardStatus.ON_FIELD, null);

            // then
            verify(historyRepository, never()).save(any());
        }

        @Test
        @DisplayName("fromStatus == toStatus 인 호출은 이력을 생성하지 않는다 (ARCHIVE → ARCHIVE)")
        void append_sameStatus_archive_doesNotPersist() {
            // given
            Card card = sampleCard();

            // when — 같은 상태 재전이 시 reason이 있어도 무시한다 (멱등 우선)
            appender.append(card, CardStatus.ARCHIVE, CardStatus.ARCHIVE, ArchiveReason.MANUAL);

            // then
            verify(historyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("append() — 정상 이력 기록")
    class HappyPath {

        @Test
        @DisplayName("ON_FIELD → ARCHIVE 전이는 reason과 함께 저장된다")
        void append_onFieldToArchive_withReason_persists() {
            // given
            Card card = sampleCard();

            // when
            appender.append(card, CardStatus.ON_FIELD, CardStatus.ARCHIVE, ArchiveReason.MAX_VIEW);

            // then
            verify(historyRepository, times(1)).save(any(CardStatusHistory.class));
        }

        @Test
        @DisplayName("ARCHIVE → ON_FIELD 복귀는 reason 없이 저장된다")
        void append_archiveToOnField_withoutReason_persists() {
            // given
            Card card = sampleCard();

            // when
            appender.append(card, CardStatus.ARCHIVE, CardStatus.ON_FIELD, null);

            // then
            verify(historyRepository, times(1)).save(any(CardStatusHistory.class));
        }
    }

    @Nested
    @DisplayName("append() — 방향별 reason 규칙 위반")
    class Validation {

        @Test
        @DisplayName("ON_FIELD → ARCHIVE 호출에 reason이 null이면 INVALID_INPUT 예외가 발생하고 저장되지 않는다")
        void append_onFieldToArchive_withoutReason_throwsAndDoesNotPersist() {
            // given
            Card card = sampleCard();

            // when & then
            assertThatThrownBy(() ->
                    appender.append(card, CardStatus.ON_FIELD, CardStatus.ARCHIVE, null))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);

            verify(historyRepository, never()).save(any());
        }

        @Test
        @DisplayName("ARCHIVE → ON_FIELD 호출에 reason이 있으면 INVALID_INPUT 예외가 발생하고 저장되지 않는다")
        void append_archiveToOnField_withReason_throwsAndDoesNotPersist() {
            // given
            Card card = sampleCard();

            // when & then
            assertThatThrownBy(() ->
                    appender.append(card, CardStatus.ARCHIVE, CardStatus.ON_FIELD, ArchiveReason.MAX_DURATION))
                    .isInstanceOf(CardDomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);

            verify(historyRepository, never()).save(any());
        }

        @Test
        @DisplayName("ON_FIELD → ARCHIVE 호출에 reason이 정상이면 예외 없이 저장된다 (대조군)")
        void append_onFieldToArchive_validReason_doesNotThrow() {
            // given
            Card card = sampleCard();

            // when & then
            assertThatCode(() ->
                    appender.append(card, CardStatus.ON_FIELD, CardStatus.ARCHIVE, ArchiveReason.MAX_DURATION))
                    .doesNotThrowAnyException();

            verify(historyRepository, times(1)).save(any(CardStatusHistory.class));
        }
    }
}
