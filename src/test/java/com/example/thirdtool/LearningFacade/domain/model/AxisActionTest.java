package com.example.thirdtool.learningfacade.domain.model;

import com.example.thirdtool.LearningFacade.domain.model.*;
import com.example.thirdtool.learningfacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.user.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AxisAction")
class AxisActionTest {

    // ─── 테스트 픽스처 헬퍼 ───────────────────────────────────────────────

    private LearningAxis axis;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.create(1L);
        LearningFacade facade = LearningFacade.create(user, "백엔드 개발자");
        axis = LearningAxis.create(facade, "API 설계", 1);
    }

    private AxisAction createAction() {
        return AxisAction.create(axis, "설계하다");
    }

    /**
     * revisionCount를 원하는 값으로 올리는 헬퍼.
     * updateDescription()을 반복 호출해 revisionCount를 조작한다.
     */
    private AxisAction createActionWithRevisionCount(int count) {
        AxisAction action = createAction();
        for (int i = 0; i < count; i++) {
            action.updateDescription(i % 2 == 0 ? "검증하다" : "설계하다");
        }
        return action;
    }

    // ─── 1. 생성 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("유효한 파라미터로 생성하면 coverageStatus=NO_MATERIAL, revisionCount=0으로 초기화된다")
        void create_valid_초기상태() {
            //when
            AxisAction action = AxisAction.create(axis, "설계하다");

            //then
            assertThat(action.getCoverageStatus()).isEqualTo(CoverageStatus.NO_MATERIAL);
            assertThat(action.getRevisionCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("axis가 null이면 예외가 발생한다")
        void create_axis_null_예외() {
            //when & then
            assertThatThrownBy(() -> AxisAction.create(null, "설계하다"))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("description이 null이면 예외가 발생한다")
        void create_description_null_예외() {
            //when & then
            assertThatThrownBy(() -> AxisAction.create(axis, null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("description이 공백 문자열이면 예외가 발생한다")
        void create_description_blank_예외() {
            //when & then
            assertThatThrownBy(() -> AxisAction.create(axis, "  "))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("description에 공백이 포함되면 단일 동사 원칙 위반으로 예외가 발생한다")
        void create_description_공백포함_예외() {
            //when & then — "설계하다"는 허용, "설계하고 분석하다"는 불허
            assertThatThrownBy(() -> AxisAction.create(axis, "설계하고 분석하다"))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    // ─── 2. 동사 수정 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("동사 수정")
    class UpdateDescription {

        @Test
        @DisplayName("다른 값으로 수정하면 ActionChangeRecord.isChanged()가 true를 반환한다")
        void updateDescription_변경_changed반환() {
            //given
            AxisAction action = createAction();

            //when
            ActionChangeRecord record = action.updateDescription("검증하다");

            //then
            assertThat(record.isChanged()).isTrue();
        }

        @Test
        @DisplayName("동사를 변경하면 coverageStatus가 NO_MATERIAL로 초기화된다")
        void updateDescription_변경_coverageStatus_초기화() {
            //given
            AxisAction action = createAction();
            action.updateCoverageStatus(CoverageStatus.PARTIAL);   // PARTIAL 상태 설정

            //when
            action.updateDescription("검증하다");

            //then
            assertThat(action.getCoverageStatus()).isEqualTo(CoverageStatus.NO_MATERIAL);
        }

        @Test
        @DisplayName("동사를 변경하면 revisionCount가 1 증가한다")
        void updateDescription_변경_revisionCount_증가() {
            //given
            AxisAction action = createActionWithRevisionCount(1);  // revisionCount = 1
            int beforeCount = action.getRevisionCount();

            //when
            action.updateDescription("문서화하다");

            //then
            assertThat(action.getRevisionCount()).isEqualTo(beforeCount + 1);
        }

        @Test
        @DisplayName("동일한 값으로 수정하면 isChanged()가 false를 반환한다")
        void updateDescription_동일값_unchanged반환() {
            //given
            AxisAction action = createAction();

            //when
            ActionChangeRecord record = action.updateDescription("설계하다");

            //then
            assertThat(record.isChanged()).isFalse();
        }

        @Test
        @DisplayName("동일한 값으로 수정하면 PARTIAL 상태의 coverageStatus가 초기화되지 않는다")
        void updateDescription_동일값_coverageStatus_불변() {
            //given
            AxisAction action = createAction();
            action.updateCoverageStatus(CoverageStatus.PARTIAL);

            //when
            action.updateDescription("설계하다");

            //then
            assertThat(action.getCoverageStatus()).isEqualTo(CoverageStatus.PARTIAL);
        }

        @Test
        @DisplayName("동일한 값으로 수정하면 revisionCount가 증가하지 않는다")
        void updateDescription_동일값_revisionCount_불변() {
            //given
            AxisAction action = createActionWithRevisionCount(2);  // revisionCount = 2
            int countBefore = action.getRevisionCount();

            //when
            action.updateDescription(action.getDescription());  // 현재 description 그대로 재입력

            //then
            assertThat(action.getRevisionCount()).isEqualTo(countBefore);
        }

        @Test
        @DisplayName("공백 문자열로 수정하면 예외가 발생한다")
        void updateDescription_blank_예외() {
            //given
            AxisAction action = createAction();

            //when & then
            assertThatThrownBy(() -> action.updateDescription("  "))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("null로 수정하면 예외가 발생한다")
        void updateDescription_null_예외() {
            //given
            AxisAction action = createAction();

            //when & then
            assertThatThrownBy(() -> action.updateDescription(null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("공백 포함 동사로 수정하면 단일 동사 원칙 위반으로 예외가 발생한다")
        void updateDescription_공백포함_예외() {
            //given
            AxisAction action = createAction();

            //when & then
            assertThatThrownBy(() -> action.updateDescription("검증하고 기록하다"))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    // ─── 3. 커버리지 상태 변경 ───────────────────────────────────────────

    @Nested
    @DisplayName("커버리지 상태 변경")
    class UpdateCoverageStatus {

        @Test
        @DisplayName("NO_MATERIAL → PARTIAL 전이가 정상적으로 적용된다")
        void updateCoverageStatus_NO_MATERIAL→PARTIAL() {
            //given
            AxisAction action = createAction();   // 초기: NO_MATERIAL

            //when
            action.updateCoverageStatus(CoverageStatus.PARTIAL);

            //then
            assertThat(action.getCoverageStatus()).isEqualTo(CoverageStatus.PARTIAL);
        }

        @Test
        @DisplayName("PARTIAL → COVERED 전이가 정상적으로 적용된다")
        void updateCoverageStatus_PARTIAL→COVERED() {
            //given
            AxisAction action = createAction();
            action.updateCoverageStatus(CoverageStatus.PARTIAL);

            //when
            action.updateCoverageStatus(CoverageStatus.COVERED);

            //then
            assertThat(action.getCoverageStatus()).isEqualTo(CoverageStatus.COVERED);
        }

        @Test
        @DisplayName("COVERED → NO_MATERIAL 전이가 정상적으로 적용된다 — 자료 전체 해제 시 복귀")
        void updateCoverageStatus_COVERED→NO_MATERIAL() {
            //given
            AxisAction action = createAction();
            action.updateCoverageStatus(CoverageStatus.COVERED);

            //when
            action.updateCoverageStatus(CoverageStatus.NO_MATERIAL);

            //then
            assertThat(action.getCoverageStatus()).isEqualTo(CoverageStatus.NO_MATERIAL);
        }
    }

    // ─── 4. 단련 안내 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("단련 안내")
    class RefinementSuggested {

        @Test
        @DisplayName("revisionCount가 임계값(3) 미만이면 isRefinementSuggested가 false를 반환한다")
        void isRefinementSuggested_임계값미만_false() {
            //given — revisionCount = 2
            AxisAction action = createActionWithRevisionCount(2);

            //when & then
            assertThat(action.isRefinementSuggested()).isFalse();
        }

        @Test
        @DisplayName("revisionCount가 임계값(3)에 정확히 도달하면 isRefinementSuggested가 true를 반환한다")
        void isRefinementSuggested_임계값정확히도달_true() {
            //given — revisionCount = 3
            AxisAction action = createActionWithRevisionCount(3);

            //when & then
            assertThat(action.isRefinementSuggested()).isTrue();
        }

        @Test
        @DisplayName("revisionCount가 임계값(3)을 초과해도 isRefinementSuggested가 true를 반환한다")
        void isRefinementSuggested_임계값초과_true() {
            //given — revisionCount = 5
            AxisAction action = createActionWithRevisionCount(5);

            //when & then
            assertThat(action.isRefinementSuggested()).isTrue();
        }
    }
}