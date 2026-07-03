package com.example.thirdtool.Card.domain.model;

import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.UserSchedule.domain.model.LearningMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Card 하이브리드 시나리오 (Story-CARD-E3-S3-5).
 *
 * M3 down/up shift 시나리오 4건:
 *  1. down + 이미 넘긴 카드 → hasScheduleExhausted=true, effectiveMaxDays 새 max로 cap
 *  2. up + 기존 카드 → effectiveMaxDays 변화 없음 (createdMode 상한 유지)
 *  3. down + 아직 안 넘긴 카드 → isDueOn 새 max에 맞춰 반영
 *  4. returnToField 후 → 새 createdMode 적용
 */
@DisplayName("Card 하이브리드 시나리오 (Story-CARD-E3-S3-5)")
class CardHybridScenarioTest {

    private static Card cardWithEnteredDaysAgo(LearningMode createdMode, int daysAgo) {
        UserEntity user = UserEntity.ofLocal("tester", "encoded-pw", "닉네임", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        Deck deck = Deck.createFromAxis(user, 10L, "축");

        LocalDate enteredDate = LocalDate.now().minusDays(daysAgo);
        Card card = Card.create(
                deck,
                MainNote.of("본문", null),
                Summary.of("요약."),
                List.of("키워드"),
                null,
                createdMode,
                enteredDate
        );
        return card;
    }

    @Nested
    @DisplayName("1. down-shift + 이미 넘긴 카드")
    class DownAlreadyExceeded {

        @Test
        @DisplayName("downCap_이미넘긴카드_hasScheduleExhausted_true")
        void downCap_이미넘긴카드_hasScheduleExhausted_true() {
            // createdMode=MODE_28D, entered 10일 전. 사용자 down-shift → MODE_7D.
            // effectiveMaxDays = min(28, 7) = 7. daysSinceEntered=10 > 7 → exhausted.
            Card card = cardWithEnteredDaysAgo(LearningMode.MODE_28D, 10);
            LocalDate today = LocalDate.now();

            assertThat(card.effectiveMaxDays(LearningMode.MODE_7D)).isEqualTo(7);
            assertThat(card.hasScheduleExhausted(LearningMode.MODE_7D, today)).isTrue();
        }
    }

    @Nested
    @DisplayName("2. up-shift + 기존 카드")
    class UpNoExpansion {

        @Test
        @DisplayName("up_기존카드_effectiveMaxDays_변화없음")
        void up_기존카드_effectiveMaxDays_변화없음() {
            // createdMode=MODE_7D. 사용자 up-shift → MODE_28D.
            // effectiveMaxDays = min(7, 28) = 7. 계약 확장 없음.
            Card card = cardWithEnteredDaysAgo(LearningMode.MODE_7D, 3);

            assertThat(card.effectiveMaxDays(LearningMode.MODE_28D)).isEqualTo(7);
            assertThat(card.effectiveIntervals(LearningMode.MODE_28D))
                    .containsExactly(1, 3, 7);
        }
    }

    @Nested
    @DisplayName("3. down-shift + 아직 안 넘긴 카드")
    class DownNotYetExceeded {

        @Test
        @DisplayName("down_아직안넘긴카드_새max로cap_isDueOn_반영")
        void down_아직안넘긴카드_새max로cap_isDueOn_반영() {
            // createdMode=MODE_28D, entered 3일 전. 사용자 down-shift → MODE_7D.
            // effectiveMaxDays = 7. daysSinceEntered = 3 → exhausted 아님.
            // effectiveIntervals = [1, 3, 7] (28은 cap 초과라 제외).
            // isDueOn(today, MODE_7D) → daysSinceEntered=3이 [1,3,7]에 포함되므로 true.
            Card card = cardWithEnteredDaysAgo(LearningMode.MODE_28D, 3);
            LocalDate today = LocalDate.now();

            assertThat(card.effectiveMaxDays(LearningMode.MODE_7D)).isEqualTo(7);
            assertThat(card.effectiveIntervals(LearningMode.MODE_7D))
                    .containsExactly(1, 3, 7);
            assertThat(card.hasScheduleExhausted(LearningMode.MODE_7D, today)).isFalse();
            assertThat(card.isDueOn(today, LearningMode.MODE_7D)).isTrue();
        }
    }

    @Nested
    @DisplayName("경계값 - MODE_60D 하이브리드 판정")
    class Mode60DEdge {

        @Test
        @DisplayName("MODE_60D_카드_down_MODE_14D_effectiveMaxDays_14로_cap")
        void MODE_60D_카드_down_MODE_14D_effectiveMaxDays_14로_cap() {
            // createdMode=MODE_60D (intervals=[1,3,7,14,28,60]), userCurrent=MODE_14D.
            // effectiveMaxDays = min(60, 14) = 14. effectiveIntervals = [1, 3, 7, 14].
            Card card = cardWithEnteredDaysAgo(LearningMode.MODE_60D, 14);

            assertThat(card.effectiveMaxDays(LearningMode.MODE_14D)).isEqualTo(14);
            assertThat(card.effectiveIntervals(LearningMode.MODE_14D))
                    .containsExactly(1, 3, 7, 14);
            assertThat(card.hasScheduleExhausted(LearningMode.MODE_14D, LocalDate.now())).isFalse();
            assertThat(card.isDueOn(LocalDate.now(), LearningMode.MODE_14D)).isTrue();
        }
    }

    @Nested
    @DisplayName("예외 - null 입력")
    class NullInputs {

        @Test
        @DisplayName("effectiveMaxDays_null_userCurrentMode_예외")
        void effectiveMaxDays_null_userCurrentMode_예외() {
            Card card = cardWithEnteredDaysAgo(LearningMode.MODE_14D, 3);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> card.effectiveMaxDays(null))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("isDueOn_null_today_예외")
        void isDueOn_null_today_예외() {
            Card card = cardWithEnteredDaysAgo(LearningMode.MODE_14D, 3);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> card.isDueOn(null, LearningMode.MODE_14D))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("returnToField_null_userCurrentMode_예외")
        void returnToField_null_userCurrentMode_예외() {
            Card card = cardWithEnteredDaysAgo(LearningMode.MODE_28D, 5);
            card.archive();
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> card.returnToField(null, LocalDate.now()))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("4. returnToField fresh 재시작")
    class ReturnToFieldFresh {

        @Test
        @DisplayName("returnToField_모드바뀐후_새createdMode적용")
        void returnToField_모드바뀐후_새createdMode적용() {
            // createdMode=MODE_28D, entered 30일 전. 이후 archive됐다고 가정.
            Card card = cardWithEnteredDaysAgo(LearningMode.MODE_28D, 30);
            card.archive();
            assertThat(card.getStatus()).isEqualTo(CardStatus.ARCHIVE);

            // 사용자가 이후 MODE_7D로 모드 변경. returnToField 시 새 createdMode 스냅샷 + enteredFieldAt=today.
            LocalDate today = LocalDate.now();
            card.returnToField(LearningMode.MODE_7D, today);

            assertThat(card.getStatus()).isEqualTo(CardStatus.ON_FIELD);
            assertThat(card.getCreatedMode()).isEqualTo(LearningMode.MODE_7D);
            assertThat(card.getEnteredFieldAt()).isEqualTo(today.atStartOfDay());
            assertThat(card.getViewCount()).isZero();
            assertThat(card.getLastViewedAt()).isNull();

            // 새 createdMode(7)로 하이브리드가 새로 시작 — daysSinceEntered=0, exhausted=false, isDueOn(1)=false, isDueOn(0)?→[]에 포함 안 됨.
            assertThat(card.effectiveMaxDays(LearningMode.MODE_7D)).isEqualTo(7);
            assertThat(card.hasScheduleExhausted(LearningMode.MODE_7D, today)).isFalse();
        }

        @Test
        @DisplayName("returnToField_이미ON_FIELD_상태이면_노변경_멱등")
        void returnToField_이미ON_FIELD_상태이면_노변경_멱등() {
            Card card = cardWithEnteredDaysAgo(LearningMode.MODE_28D, 3);
            LocalDate originalEntered = card.getEnteredFieldAt().toLocalDate();
            LearningMode originalMode = card.getCreatedMode();

            // 이미 ON_FIELD 상태에서 returnToField 호출 → no-op
            card.returnToField(LearningMode.MODE_7D, LocalDate.now());

            assertThat(card.getStatus()).isEqualTo(CardStatus.ON_FIELD);
            assertThat(card.getEnteredFieldAt().toLocalDate()).isEqualTo(originalEntered);
            assertThat(card.getCreatedMode()).isEqualTo(originalMode);
        }
    }
}
