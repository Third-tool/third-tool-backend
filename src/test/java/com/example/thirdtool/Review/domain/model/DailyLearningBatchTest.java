package com.example.thirdtool.Review.domain.model;

import com.example.thirdtool.Review.domain.exception.ReviewSessionException;
import com.example.thirdtool.UserSchedule.domain.model.LearningMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * REV E1 · M5 · Story 1-2 · DailyLearningBatch Aggregate 도메인 단위 테스트.
 */
@DisplayName("DailyLearningBatch (REV E1 · Story 1-2)")
class DailyLearningBatchTest {

    @Nested
    @DisplayName("create · 초기 상태")
    class Create {

        @Test
        @DisplayName("해피: 정상 인자 · 초기 open · entries 0 · completionRatio 1.0 (perfect clear 간주)")
        void create_해피() {
            DailyLearningBatch batch = DailyLearningBatch.create(1L, LocalDate.now(), LearningMode.MODE_14D);

            assertThat(batch.getUserId()).isEqualTo(1L);
            assertThat(batch.isClosed()).isFalse();
            assertThat(batch.totalCount()).isZero();
            assertThat(batch.completionRatio()).isEqualTo(1.0);
            assertThat(batch.isPerfectClear()).isTrue();
        }

        @Test
        @DisplayName("예외: userId null")
        void create_userId_null() {
            assertThatThrownBy(() -> DailyLearningBatch.create(null, LocalDate.now(), LearningMode.MODE_14D))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("close · 자정 cron")
    class Close {

        @Test
        @DisplayName("해피: open batch에 close 호출 · closedAt 세팅")
        void close_해피() {
            DailyLearningBatch batch = DailyLearningBatch.create(1L, LocalDate.now(), LearningMode.MODE_14D);
            LocalDateTime closedAt = LocalDateTime.now();

            batch.close(closedAt);

            assertThat(batch.isClosed()).isTrue();
            assertThat(batch.getClosedAt()).isEqualTo(closedAt);
        }

        @Test
        @DisplayName("엣지: 이미 closed batch에 재호출 · no-op (멱등 · 원 closedAt 유지)")
        void close_재호출_no_op() {
            DailyLearningBatch batch = DailyLearningBatch.create(1L, LocalDate.now(), LearningMode.MODE_14D);
            LocalDateTime first = LocalDateTime.now();
            batch.close(first);
            LocalDateTime second = first.plusHours(1);

            batch.close(second);

            assertThat(batch.getClosedAt()).isEqualTo(first);  // 원 값 유지
        }
    }

    @Nested
    @DisplayName("markViewed · closed 상태에서 예외")
    class MarkViewed {

        @Test
        @DisplayName("예외: closed batch에 markViewed · DAILY_BATCH_CLOSED")
        void markViewed_closed_예외() {
            DailyLearningBatch batch = DailyLearningBatch.create(1L, LocalDate.now(), LearningMode.MODE_14D);
            batch.close(LocalDateTime.now());

            assertThatThrownBy(() -> batch.markViewed(100L, LocalDateTime.now()))
                    .isInstanceOf(ReviewSessionException.class)
                    .hasMessageContaining("오늘 학습 세션이 종료되었습니다");
        }
    }

    @Nested
    @DisplayName("completionRatio · 0으로 나누기 방지")
    class CompletionRatio {

        @Test
        @DisplayName("엣지: entries 0 · 1.0 반환 (perfect clear 간주)")
        void entries_0_1_0() {
            DailyLearningBatch batch = DailyLearningBatch.create(1L, LocalDate.now(), LearningMode.MODE_14D);

            assertThat(batch.completionRatio()).isEqualTo(1.0);
            assertThat(batch.isPerfectClear()).isTrue();
        }
    }

    @Nested
    @DisplayName("isOwner")
    class IsOwner {

        @Test
        @DisplayName("해피: 동일 userId · true")
        void owner_일치() {
            DailyLearningBatch batch = DailyLearningBatch.create(1L, LocalDate.now(), LearningMode.MODE_14D);

            assertThat(batch.isOwner(1L)).isTrue();
            assertThat(batch.isOwner(2L)).isFalse();
        }
    }
}
