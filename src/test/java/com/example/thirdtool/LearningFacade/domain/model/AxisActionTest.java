package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AxisAction 단위 테스트 (운영 코드 v1).
 *
 * <p>sol §4의 AxisTopicTest는 Epic 2 v2(AxisTopic 모델) 가정. 현재 운영 코드는
 * v1(AxisAction — 단일 동사 강제 + revisionCount + 동사 변경 시 커버리지 NO_MATERIAL
 * 강제 초기화)이므로, 본 테스트는 v1의 의도된 동작을 검증한다.
 *
 * <p>v2 리팩토링이 머지되면 본 파일은 삭제되고 sol §4 시나리오가 AxisTopicTest로
 * 신규 작성된다.
 */
@DisplayName("AxisAction (v1)")
class AxisActionTest {

    private LearningAxis axis;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.ofLocal(
                "tester-1", "encoded-pw", "닉네임-1", "tester1@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        LearningFacade facade = LearningFacade.create(user, "백엔드 개발자");
        axis = LearningAxis.create(facade, "API 설계", 1);
    }

    // ─── 1. 생성 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("유효한 단일 동사로 생성하면 coverageStatus=NO_MATERIAL, revisionCount=0이다")
        void create_valid_초기상태() {
            // when
            AxisAction action = AxisAction.create(axis, "설계하다");

            // then
            assertThat(action.getDescription()).isEqualTo("설계하다");
            assertThat(action.getCoverageStatus()).isEqualTo(CoverageStatus.NO_MATERIAL);
            assertThat(action.getRevisionCount()).isZero();
        }

        @Test
        @DisplayName("description의 앞뒤 공백은 trim된다")
        void create_description_trim처리() {
            // when
            AxisAction action = AxisAction.create(axis, "  설계하다  ");

            // then
            assertThat(action.getDescription()).isEqualTo("설계하다");
        }

        @Test
        @DisplayName("axis가 null이면 예외가 발생한다 — 소속 없는 행동 차단")
        void create_axis_null_예외() {
            // when & then
            assertThatThrownBy(() -> AxisAction.create(null, "설계하다"))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("description이 null이면 예외가 발생한다")
        void create_description_null_예외() {
            // when & then
            assertThatThrownBy(() -> AxisAction.create(axis, null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("description이 공백 문자열이면 예외가 발생한다")
        void create_description_blank_예외() {
            // when & then
            assertThatThrownBy(() -> AxisAction.create(axis, "  "))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("description에 중간 공백이 있으면 예외가 발생한다 — v1 단일 동사 강제 규칙")
        void create_description_공백포함_예외() {
            // when & then
            assertThatThrownBy(() -> AxisAction.create(axis, "설계 하다"))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    // ─── 2. 동사 수정 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("동사 수정")
    class UpdateDescription {

        @Test
        @DisplayName("다른 값으로 수정하면 ActionChangeRecord.isChanged()가 true이고 description이 갱신된다")
        void updateDescription_변경값_changed반환() {
            // given
            AxisAction action = AxisAction.create(axis, "설계하다");

            // when
            ActionChangeRecord record = action.updateDescription("검증하다");

            // then
            assertThat(record.isChanged()).isTrue();
            assertThat(action.getDescription()).isEqualTo("검증하다");
        }

        @Test
        @DisplayName("동일한 값으로 수정하면 isChanged()가 false이고 description이 유지된다")
        void updateDescription_동일값_unchanged반환() {
            // given
            AxisAction action = AxisAction.create(axis, "설계하다");

            // when
            ActionChangeRecord record = action.updateDescription("설계하다");

            // then
            assertThat(record.isChanged()).isFalse();
            assertThat(action.getDescription()).isEqualTo("설계하다");
        }

        @Test
        @DisplayName("실제 변경 시 revisionCount가 1 증가한다 — v1 수정 횟수 추적 규칙")
        void updateDescription_변경시_revisionCount_증가() {
            // given
            AxisAction action = AxisAction.create(axis, "설계하다");
            int before = action.getRevisionCount();

            // when
            action.updateDescription("검증하다");

            // then
            assertThat(action.getRevisionCount()).isEqualTo(before + 1);
        }

        @Test
        @DisplayName("실제 변경 시 coverageStatus는 NO_MATERIAL로 강제 초기화된다 — v1 동사 변경 규칙")
        void updateDescription_변경시_coverageStatus_초기화() {
            // given — 자료 연결을 가정해 PARTIAL 상태로 만든 후 동사 변경
            AxisAction action = AxisAction.create(axis, "설계하다");
            action.updateCoverageStatus(CoverageStatus.PARTIAL);

            // when
            action.updateDescription("검증하다");

            // then
            assertThat(action.getCoverageStatus()).isEqualTo(CoverageStatus.NO_MATERIAL);
        }

        @Test
        @DisplayName("동일 값 재입력 시 revisionCount는 증가하지 않는다 — 멱등성")
        void updateDescription_동일값_revisionCount_불변() {
            // given
            AxisAction action = AxisAction.create(axis, "설계하다");
            int before = action.getRevisionCount();

            // when
            action.updateDescription("설계하다");

            // then
            assertThat(action.getRevisionCount()).isEqualTo(before);
        }

        @Test
        @DisplayName("blank로 수정하면 예외가 발생한다")
        void updateDescription_blank_예외() {
            // given
            AxisAction action = AxisAction.create(axis, "설계하다");

            // when & then
            assertThatThrownBy(() -> action.updateDescription("  "))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("null로 수정하면 예외가 발생한다")
        void updateDescription_null_예외() {
            // given
            AxisAction action = AxisAction.create(axis, "설계하다");

            // when & then
            assertThatThrownBy(() -> action.updateDescription(null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    // ─── 3. 커버리지 상태 변경 ────────────────────────────────────────────

    @Nested
    @DisplayName("커버리지 상태 변경")
    class UpdateCoverageStatus {

        @Test
        @DisplayName("NO_MATERIAL → PARTIAL 전이가 적용된다")
        void updateCoverageStatus_NO_MATERIAL_PARTIAL() {
            // given
            AxisAction action = AxisAction.create(axis, "설계하다");

            // when
            action.updateCoverageStatus(CoverageStatus.PARTIAL);

            // then
            assertThat(action.getCoverageStatus()).isEqualTo(CoverageStatus.PARTIAL);
        }

        @Test
        @DisplayName("PARTIAL → COVERED 전이가 적용된다 — MASTERED 자료 연결 시 최종 상태")
        void updateCoverageStatus_PARTIAL_COVERED() {
            // given
            AxisAction action = AxisAction.create(axis, "설계하다");
            action.updateCoverageStatus(CoverageStatus.PARTIAL);

            // when
            action.updateCoverageStatus(CoverageStatus.COVERED);

            // then
            assertThat(action.getCoverageStatus()).isEqualTo(CoverageStatus.COVERED);
        }

        @Test
        @DisplayName("null로 변경하면 예외가 발생한다 — 외부 호출 방어")
        void updateCoverageStatus_null_예외() {
            // given
            AxisAction action = AxisAction.create(axis, "설계하다");

            // when & then
            assertThatThrownBy(() -> action.updateCoverageStatus(null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    // ─── 4. 보조 조회 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("isRefinementSuggested — v1 수정 횟수 임계값")
    class IsRefinementSuggested {

        @Test
        @DisplayName("revisionCount=0이면 false (개선 권장 안 됨)")
        void isRefinementSuggested_0회_false() {
            // given
            AxisAction action = AxisAction.create(axis, "설계하다");

            // when & then
            assertThat(action.isRefinementSuggested()).isFalse();
        }

        @Test
        @DisplayName("revisionCount=2이면 false — 임계값(3) 미만")
        void isRefinementSuggested_2회_false() {
            // given
            AxisAction action = AxisAction.create(axis, "설계하다");
            ReflectionTestUtils.setField(action, "revisionCount", 2);

            // when & then
            assertThat(action.isRefinementSuggested()).isFalse();
        }

        @Test
        @DisplayName("revisionCount=3이면 true — 경계값(임계값 도달)")
        void isRefinementSuggested_3회_true() {
            // given
            AxisAction action = AxisAction.create(axis, "설계하다");
            ReflectionTestUtils.setField(action, "revisionCount", 3);

            // when & then
            assertThat(action.isRefinementSuggested()).isTrue();
        }
    }
}
