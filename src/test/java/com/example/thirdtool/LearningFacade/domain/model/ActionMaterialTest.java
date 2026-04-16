package com.example.thirdtool.LearningFacade.domain.model;


import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ActionMaterial")
class ActionMaterialTest {

    // ─── 테스트 픽스처 헬퍼 ───────────────────────────────────────────────

    private AxisAction action;
    private LearningMaterial material;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.create(1L);
        LearningFacade facade = LearningFacade.create(user, "백엔드 개발자");
        LearningAxis axis = LearningAxis.create(facade, "API 설계", 1);

        action = AxisAction.create(axis, "설계하다");
        material = LearningMaterial.create(facade, "도메인 주도 설계", MaterialType.TOP_DOWN, null);
    }

    @Test
    @DisplayName("유효한 action과 material로 생성하면 linkedAt이 null이 아니다")
    void create_valid_linkedAt설정() {
        //when
        ActionMaterial actionMaterial = ActionMaterial.create(action, material);

        //then
        assertThat(actionMaterial.getAction()).isEqualTo(action);
        assertThat(actionMaterial.getMaterial()).isEqualTo(material);
        assertThat(actionMaterial.getLinkedAt()).isNotNull();
    }

    @Test
    @DisplayName("action이 null이면 예외가 발생한다")
    void create_action_null_예외() {
        //when & then
        assertThatThrownBy(() -> ActionMaterial.create(null, material))
                .isInstanceOf(LearningFacadeDomainException.class);
    }

    @Test
    @DisplayName("material이 null이면 예외가 발생한다")
    void create_material_null_예외() {
        //when & then
        assertThatThrownBy(() -> ActionMaterial.create(action, null))
                .isInstanceOf(LearningFacadeDomainException.class);
    }
}

