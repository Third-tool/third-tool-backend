package com.example.thirdtool.Review.domain.model;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Review.domain.exception.ReviewSessionException;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.UserSchedule.domain.model.LearningMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * REV E2 · Story 2-1 · ReviewSession.startFrom 팩토리 단위 테스트.
 *
 * <p>batch 원천 세션 생성 · 소유권/closed/empty 검증 · currentIndex/finished 초기 상태.
 */
@DisplayName("ReviewSession.startFrom (REV E2 · Story 2-1)")
class ReviewSessionStartFromTest {

    private final UserEntity user = mockUser(1L);

    private DailyLearningBatch batchWith(int cardCount) {
        DailyLearningBatch batch = DailyLearningBatch.create(1L, LocalDate.now(), LearningMode.MODE_14D);
        // batchId · totalCount는 entries 크기 기반 — Reflection으로 id 세팅해 batch.getId() null 방어.
        ReflectionTestUtils.setField(batch, "id", 100L);
        return batch;
    }

    @Nested
    @DisplayName("정상 케이스")
    class Happy {

        @Test
        @DisplayName("해피: batch open · 카드 3개 · 세션 생성 + cardReviews 3개 + currentIndex=0 + not finished")
        void 해피_3장() {
            DailyLearningBatch batch = batchWith(3);
            List<Card> cards = List.of(mockCard(11L), mockCard(12L), mockCard(13L));
            LocalDateTime now = LocalDateTime.now();

            ReviewSession session = ReviewSession.startFrom(batch, cards, user, now);

            assertThat(session.getCardReviews()).hasSize(3);
            assertThat(session.getCurrentIndex()).isZero();
            assertThat(session.isFinished()).isFalse();
            assertThat(session.getAvailableCardCount()).isEqualTo(3);
            assertThat(session.getBatch()).isSameAs(batch);
            assertThat(session.getStartedAt()).isEqualTo(now);
        }
    }

    @Nested
    @DisplayName("예외 케이스")
    class Failures {

        @Test
        @DisplayName("예외: batch가 이미 closed · DAILY_BATCH_CLOSED_FOR_NEW_SESSION")
        void closed_batch_예외() {
            DailyLearningBatch batch = batchWith(1);
            batch.close(LocalDateTime.now());
            List<Card> cards = List.of(mockCard(11L));

            assertThatThrownBy(() -> ReviewSession.startFrom(batch, cards, user, LocalDateTime.now()))
                    .isInstanceOf(ReviewSessionException.class)
                    .hasMessageContaining("closed batch");
        }

        @Test
        @DisplayName("예외: availableCards 빈 리스트 · DAILY_BATCH_HAS_NO_CARDS")
        void 빈_카드_예외() {
            DailyLearningBatch batch = batchWith(0);

            assertThatThrownBy(() -> ReviewSession.startFrom(batch, List.of(), user, LocalDateTime.now()))
                    .isInstanceOf(ReviewSessionException.class)
                    .hasMessageContaining("오늘 학습할 카드가 없습니다");
        }

        @Test
        @DisplayName("예외: 타인 batch · REVIEW_SESSION_FORBIDDEN")
        void 소유권_예외() {
            DailyLearningBatch batch = batchWith(1);
            UserEntity otherUser = mockUser(999L);
            List<Card> cards = List.of(mockCard(11L));

            assertThatThrownBy(() -> ReviewSession.startFrom(batch, cards, otherUser, LocalDateTime.now()))
                    .isInstanceOf(ReviewSessionException.class)
                    .hasMessageContaining("본인의 리뷰 세션이 아닙니다");
        }
    }

    @Nested
    @DisplayName("finish 멱등성 (Story 2-3)")
    class Finish {

        @Test
        @DisplayName("해피: finish 호출 · finished=true · finishedAt 세팅")
        void finish_해피() {
            DailyLearningBatch batch = batchWith(1);
            List<Card> cards = List.of(mockCard(11L));
            ReviewSession session = ReviewSession.startFrom(batch, cards, user, LocalDateTime.now());

            LocalDateTime finishAt = LocalDateTime.now().plusMinutes(5);
            session.finish(finishAt);

            assertThat(session.isFinished()).isTrue();
            assertThat(session.isActive()).isFalse();
            assertThat(session.getFinishedAt()).isEqualTo(finishAt);
        }

        @Test
        @DisplayName("엣지: 이미 finished에 재호출 · no-op (finishedAt 유지)")
        void finish_재호출_no_op() {
            DailyLearningBatch batch = batchWith(1);
            List<Card> cards = List.of(mockCard(11L));
            ReviewSession session = ReviewSession.startFrom(batch, cards, user, LocalDateTime.now());

            LocalDateTime first = LocalDateTime.now().plusMinutes(1);
            LocalDateTime second = first.plusMinutes(10);
            session.finish(first);
            session.finish(second);

            assertThat(session.getFinishedAt()).isEqualTo(first); // 원 값 유지
        }
    }

    // -------------------------------------------------------
    // 헬퍼
    // -------------------------------------------------------

    private static UserEntity mockUser(Long id) {
        UserEntity u = mock(UserEntity.class);
        Mockito.when(u.getId()).thenReturn(id);
        return u;
    }

    private static Card mockCard(Long id) {
        Card c = mock(Card.class);
        Mockito.when(c.getId()).thenReturn(id);
        return c;
    }
}
