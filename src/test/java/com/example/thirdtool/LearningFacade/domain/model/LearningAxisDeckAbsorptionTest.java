package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LearningAxis의 Deck 흡수 필드·행위 도메인 단위 테스트.
 *
 * <p>LT E5 · M5 · Story 5-1 산출물. Deck BC 폐기로 이관된 6 필드 및 5 도메인 행위 검증.
 *
 * <p>3구분: 해피/엣지/예외 (`.claude/rules/conventions.md` §4.2)
 */
@DisplayName("LearningAxis — Deck 흡수 필드·행위 (LT E5 · M5)")
class LearningAxisDeckAbsorptionTest {

    private LearningFacade facade;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.ofLocal(
                "tester-1", "encoded-pw", "닉네임-1", "tester1@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        facade = LearningFacade.create(user, "백엔드 개발자");
    }

    private LearningAxis createAxis() {
        return facade.addAxis("API 설계");
    }

    // ─── 초기 상태 (default 값) ─────────────────────────────

    @Nested
    @DisplayName("초기 상태 · default 값")
    class InitialState {

        @Test
        @DisplayName("progressStatus 초기값은 NOT_STARTED")
        void progressStatus_초기값_NOT_STARTED() {
            LearningAxis axis = createAxis();

            assertThat(axis.getProgressStatus()).isEqualTo(AxisProgressStatus.NOT_STARTED);
        }

        @Test
        @DisplayName("mode 초기값은 STUDY (SDD 재정의 · fresh start)")
        void mode_초기값_STUDY() {
            LearningAxis axis = createAxis();

            assertThat(axis.getMode()).isEqualTo(AxisLearningMode.STUDY);
        }

        @Test
        @DisplayName("onLibrary 초기값은 false")
        void onLibrary_초기값_false() {
            LearningAxis axis = createAxis();

            assertThat(axis.isOnLibrary()).isFalse();
        }

        @Test
        @DisplayName("lastAccessedAt·learningMaterialId·publishedAt 초기값은 null")
        void nullable_필드_초기값_null() {
            LearningAxis axis = createAxis();

            assertThat(axis.getLastAccessedAt()).isNull();
            assertThat(axis.getLearningMaterialId()).isNull();
            assertThat(axis.getPublishedAt()).isNull();
        }
    }

    // ─── markInProgress() 멱등 ──────────────────────────────

    @Nested
    @DisplayName("markInProgress · NOT_STARTED → IN_PROGRESS 전이")
    class MarkInProgress {

        @Test
        @DisplayName("해피: NOT_STARTED에서 markInProgress 호출 시 IN_PROGRESS로 전이")
        void markInProgress_NOT_STARTED_IN_PROGRESS로전이() {
            LearningAxis axis = createAxis();

            axis.markInProgress();

            assertThat(axis.getProgressStatus()).isEqualTo(AxisProgressStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("엣지: 이미 IN_PROGRESS 상태에서 재호출 시 상태 변화 없음 (멱등)")
        void markInProgress_이미IN_PROGRESS_멱등() {
            LearningAxis axis = createAxis();
            axis.markInProgress();

            axis.markInProgress();

            assertThat(axis.getProgressStatus()).isEqualTo(AxisProgressStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("엣지: COMPLETED에서 markInProgress 호출 시 상태 유지 (회귀 방지)")
        void markInProgress_COMPLETED에서_상태유지() {
            LearningAxis axis = createAxis();
            axis.recalculateProgressStatus(0, 3); // 활성 0 · 아카이브 3 → COMPLETED

            axis.markInProgress();

            assertThat(axis.getProgressStatus()).isEqualTo(AxisProgressStatus.COMPLETED);
        }
    }

    // ─── recalculateProgressStatus (활성/아카이브 카운트 기반) ─────

    @Nested
    @DisplayName("recalculateProgressStatus · Application 카운트 기반")
    class RecalculateProgressStatus {

        @Test
        @DisplayName("해피: 활성 3 · 아카이브 0 → IN_PROGRESS")
        void 활성카드있음_IN_PROGRESS() {
            LearningAxis axis = createAxis();

            axis.recalculateProgressStatus(3, 0);

            assertThat(axis.getProgressStatus()).isEqualTo(AxisProgressStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("해피: 활성 0 · 아카이브 5 → COMPLETED (모두 아카이브)")
        void 모두아카이브_COMPLETED() {
            LearningAxis axis = createAxis();

            axis.recalculateProgressStatus(0, 5);

            assertThat(axis.getProgressStatus()).isEqualTo(AxisProgressStatus.COMPLETED);
        }

        @Test
        @DisplayName("엣지: 활성 0 · 아카이브 0 → NOT_STARTED (카드 없음)")
        void 카드없음_NOT_STARTED() {
            LearningAxis axis = createAxis();
            axis.recalculateProgressStatus(3, 2); // 먼저 IN_PROGRESS로 만듦

            axis.recalculateProgressStatus(0, 0); // 카드 전멸

            assertThat(axis.getProgressStatus()).isEqualTo(AxisProgressStatus.NOT_STARTED);
        }

        @Test
        @DisplayName("엣지: 활성 2 · 아카이브 3 (혼합) → IN_PROGRESS")
        void 혼합_IN_PROGRESS() {
            LearningAxis axis = createAxis();

            axis.recalculateProgressStatus(2, 3);

            assertThat(axis.getProgressStatus()).isEqualTo(AxisProgressStatus.IN_PROGRESS);
        }
    }

    // ─── updateLastAccessed ────────────────────────────────

    @Nested
    @DisplayName("updateLastAccessed · 최근 접근 시각 갱신")
    class UpdateLastAccessed {

        @Test
        @DisplayName("해피: 호출 시 lastAccessedAt이 현재 시각으로 갱신")
        void updateLastAccessed_해피() {
            LearningAxis axis = createAxis();
            LocalDateTime before = LocalDateTime.now();

            axis.updateLastAccessed();

            assertThat(axis.getLastAccessedAt())
                    .isNotNull()
                    .isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("엣지: 재호출 시 값 갱신 (멱등이 아니라 반복 갱신 · Deck.updateLastAccessed 계승)")
        void updateLastAccessed_재호출_시각갱신() throws InterruptedException {
            LearningAxis axis = createAxis();
            axis.updateLastAccessed();
            LocalDateTime firstAccess = axis.getLastAccessedAt();
            Thread.sleep(2); // 시각 차이 보장

            axis.updateLastAccessed();

            assertThat(axis.getLastAccessedAt()).isAfter(firstAccess);
        }
    }

    // ─── changeMode ────────────────────────────────────────

    @Nested
    @DisplayName("changeMode · 학습 모드 변경 (SDD 재정의: STUDY/REVIEW)")
    class ChangeMode {

        @Test
        @DisplayName("해피: STUDY → REVIEW 변경")
        void changeMode_STUDY_to_REVIEW() {
            LearningAxis axis = createAxis();

            axis.changeMode(AxisLearningMode.REVIEW);

            assertThat(axis.getMode()).isEqualTo(AxisLearningMode.REVIEW);
        }

        @Test
        @DisplayName("엣지: 동일 값으로 변경 (updated_at 갱신 트리거 · 값 유지)")
        void changeMode_동일값() {
            LearningAxis axis = createAxis();

            axis.changeMode(AxisLearningMode.STUDY);

            assertThat(axis.getMode()).isEqualTo(AxisLearningMode.STUDY);
        }

        @Test
        @DisplayName("예외: null 전달 시 INVALID_INPUT 예외")
        void changeMode_null_예외() {
            LearningAxis axis = createAxis();

            assertThatThrownBy(() -> axis.changeMode(null))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .hasMessageContaining("AxisLearningMode는 null일 수 없습니다.");
        }
    }

    // ─── markMaterialDeleted 멱등 ───────────────────────────

    @Nested
    @DisplayName("markMaterialDeleted · 자료 해제 · 멱등")
    class MarkMaterialDeleted {

        @Test
        @DisplayName("해피: learningMaterialId가 있는 상태에서 호출 시 null로 전환")
        void markMaterialDeleted_해피_null전환() {
            LearningAxis axis = createAxis();
            ReflectionTestUtils.setField(axis, "learningMaterialId", 42L);

            axis.markMaterialDeleted();

            assertThat(axis.getLearningMaterialId()).isNull();
        }

        @Test
        @DisplayName("엣지: 이미 null인 상태에서 호출 시 무영향 (멱등)")
        void markMaterialDeleted_이미null_멱등() {
            LearningAxis axis = createAxis();

            axis.markMaterialDeleted();

            assertThat(axis.getLearningMaterialId()).isNull();
        }

        @Test
        @DisplayName("엣지: 재호출 시에도 null 유지")
        void markMaterialDeleted_재호출_null유지() {
            LearningAxis axis = createAxis();
            ReflectionTestUtils.setField(axis, "learningMaterialId", 42L);
            axis.markMaterialDeleted();

            axis.markMaterialDeleted();

            assertThat(axis.getLearningMaterialId()).isNull();
        }
    }
}
