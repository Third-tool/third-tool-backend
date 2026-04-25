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
 * LearningAxis 단위 테스트.
 *
 * <p>본 테스트는 운영 코드 v1(AxisAction 모델) 기준으로 작성되었다.
 * sol §3의 #43~#57(addTopic/removeTopic/reorderTopics)은 Epic 2 v2 운영 코드를
 * 가정하므로 v2 리팩토링 PR에서 보강된다. 본 클래스는 v1의 create / updateName /
 * addAction / removeAction을 검증한다.
 */
@DisplayName("LearningAxis")
class LearningAxisTest {

    private LearningFacade facade;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.ofLocal(
                "tester-1", "encoded-pw", "닉네임-1", "tester1@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        facade = LearningFacade.create(user, "백엔드 개발자");
    }

    // ─── 1. 생성 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("유효한 facade·name·displayOrder로 생성하면 actions가 비어있다")
        void create_valid() {
            // when
            LearningAxis axis = LearningAxis.create(facade, "API 설계", 1);

            // then
            assertThat(axis.getName()).isEqualTo("API 설계");
            assertThat(axis.getDisplayOrder()).isEqualTo(1);
            assertThat(axis.getActions()).isEmpty();
        }

        @Test
        @DisplayName("facade가 null이면 예외가 발생한다 — 소속 없는 축 차단")
        void create_facade_null_예외() {
            // when & then
            assertThatThrownBy(() -> LearningAxis.create(null, "API 설계", 1))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("name이 null이면 예외가 발생한다")
        void create_name_null_예외() {
            // when & then
            assertThatThrownBy(() -> LearningAxis.create(facade, null, 1))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("name이 공백 문자열이면 예외가 발생한다")
        void create_name_blank_예외() {
            // when & then
            assertThatThrownBy(() -> LearningAxis.create(facade, "  ", 1))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("displayOrder가 1 미만이면 예외가 발생한다 — 1-based 정렬 기준 오염 방어")
        void create_displayOrder_0이하_예외() {
            // when & then
            assertThatThrownBy(() -> LearningAxis.create(facade, "API 설계", 0))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    // ─── 2. 이름 수정 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("이름 수정")
    class UpdateName {

        @Test
        @DisplayName("유효한 새 이름으로 수정하면 name이 갱신된다")
        void updateName_valid() {
            // given
            LearningAxis axis = LearningAxis.create(facade, "API 설계", 1);

            // when
            axis.updateName("REST API 설계");

            // then
            assertThat(axis.getName()).isEqualTo("REST API 설계");
        }

        @Test
        @DisplayName("공백 문자열로 수정하면 예외가 발생한다")
        void updateName_blank_예외() {
            // given
            LearningAxis axis = LearningAxis.create(facade, "API 설계", 1);

            // when & then
            assertThatThrownBy(() -> axis.updateName("  "))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("null로 수정하면 예외가 발생한다")
        void updateName_null_예외() {
            // given
            LearningAxis axis = LearningAxis.create(facade, "API 설계", 1);

            // when & then
            assertThatThrownBy(() -> axis.updateName(null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    // ─── 3. 행동 추가 (v1: addAction; v2에서 addTopic으로 재명명) ──────────

    @Nested
    @DisplayName("행동 추가")
    class AddAction {

        @Test
        @DisplayName("정상 추가 시 반환된 action이 actions 컬렉션에 포함된다 — orphanRemoval 동작의 전제")
        void addAction_actions컬렉션에포함() {
            // given
            LearningAxis axis = LearningAxis.create(facade, "API 설계", 1);

            // when
            AxisAction action = axis.addAction("설계하다");

            // then
            assertThat(axis.getActions()).contains(action);
        }

        @Test
        @DisplayName("정상 추가 시 커버리지 초기 상태는 NO_MATERIAL이다 — 통계 오염 방지")
        void addAction_coverageStatus_NO_MATERIAL() {
            // given
            LearningAxis axis = LearningAxis.create(facade, "API 설계", 1);

            // when
            AxisAction action = axis.addAction("설계하다");

            // then
            assertThat(action.getCoverageStatus()).isEqualTo(CoverageStatus.NO_MATERIAL);
        }

        @Test
        @DisplayName("description이 null이면 예외가 발생한다")
        void addAction_description_null_예외() {
            // given
            LearningAxis axis = LearningAxis.create(facade, "API 설계", 1);

            // when & then
            assertThatThrownBy(() -> axis.addAction(null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("description이 공백 문자열이면 예외가 발생한다")
        void addAction_description_blank_예외() {
            // given
            LearningAxis axis = LearningAxis.create(facade, "API 설계", 1);

            // when & then
            assertThatThrownBy(() -> axis.addAction("  "))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    // ─── 4. 행동 삭제 (v1: removeAction; v2에서 removeTopic으로 재명명) ────

    @Nested
    @DisplayName("행동 삭제")
    class RemoveAction {

        @Test
        @DisplayName("존재하는 actionId로 삭제하면 actions에서 제거된다")
        void removeAction_valid() {
            // given
            LearningAxis axis = LearningAxis.create(facade, "API 설계", 1);
            AxisAction action = axis.addAction("설계하다");
            ReflectionTestUtils.setField(action, "id", 100L);

            // when
            axis.removeAction(100L);

            // then
            assertThat(axis.getActions()).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 actionId로 삭제하면 예외가 발생한다 — 잘못된 id 방어")
        void removeAction_존재하지않는actionId_예외() {
            // given
            LearningAxis axis = LearningAxis.create(facade, "API 설계", 1);

            // when & then
            assertThatThrownBy(() -> axis.removeAction(999L))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }
}
