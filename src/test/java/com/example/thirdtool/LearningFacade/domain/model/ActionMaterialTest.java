package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ActionMaterial(매핑 엔티티) 단위 테스트 (운영 코드 v1).
 */
@DisplayName("ActionMaterial (v1 매핑 엔티티)")
class ActionMaterialTest {

    private AxisAction action;
    private LearningMaterial material;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.ofLocal(
                "tester-1", "encoded-pw", "닉네임-1", "tester1@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        LearningFacade facade = LearningFacade.create(user, "백엔드 개발자");
        LearningAxis axis = LearningAxis.create(facade, "API 설계", 1);
        action = AxisAction.create(axis, "설계하다");
        material = LearningMaterial.create(facade, "DDD 책", MaterialType.TOP_DOWN, "https://example.com/ddd");
    }

    @Test
    @DisplayName("유효한 action·material로 생성하면 linkedAt이 자동 기록된다")
    void create_valid_linkedAt설정() {
        // when
        ActionMaterial mapping = ActionMaterial.create(action, material);

        // then
        assertThat(mapping.getAction()).isSameAs(action);
        assertThat(mapping.getMaterial()).isSameAs(material);
        assertThat(mapping.getLinkedAt()).isNotNull();
    }

    @Test
    @DisplayName("action이 null이면 예외가 발생한다 — 행동 없는 매핑 차단")
    void create_action_null_예외() {
        // when & then
        assertThatThrownBy(() -> ActionMaterial.create(null, material))
                .isInstanceOf(LearningFacadeDomainException.class);
    }

    @Test
    @DisplayName("material이 null이면 예외가 발생한다 — 자료 없는 매핑 차단")
    void create_material_null_예외() {
        // when & then
        assertThatThrownBy(() -> ActionMaterial.create(action, null))
                .isInstanceOf(LearningFacadeDomainException.class);
    }
}
