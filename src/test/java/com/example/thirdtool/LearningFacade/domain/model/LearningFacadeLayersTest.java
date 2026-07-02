package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
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
 * Story-LT-E2-S2/S3 · LearningFacade.layers 컬렉션 & default Uncategorized 라우팅 검증.
 */
@DisplayName("LearningFacade · layers 자동 default")
class LearningFacadeLayersTest {

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = UserEntity.ofLocal("tester", "encoded-pw", "닉", "t@t.com");
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Nested
    @DisplayName("create — default Uncategorized 자동 생성")
    class CreateAutoLayer {

        @Test
        @DisplayName("create_단수_concept_default_Uncategorized_layer_1건_자동_생성")
        void create_단수_concept_default_Uncategorized_layer_1건_자동_생성() {
            LearningFacade facade = LearningFacade.create(user, "백엔드");

            assertThat(facade.getLayers()).hasSize(1);
            LearningLayer defaultLayer = facade.getDefaultLayer();
            assertThat(defaultLayer.isDefault()).isTrue();
            assertThat(defaultLayer.getName()).isEqualTo(LearningLayer.DEFAULT_LAYER_NAME);
            assertThat(defaultLayer.getDisplayOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("create_concepts_리스트_default_Uncategorized_layer_1건_자동_생성")
        void create_concepts_리스트_default_Uncategorized_layer_1건_자동_생성() {
            LearningFacade facade = LearningFacade.create(user,
                    java.util.List.of("A", "B", "C"));

            assertThat(facade.getLayers()).hasSize(1);
            assertThat(facade.getDefaultLayer().isDefault()).isTrue();
        }
    }

    @Nested
    @DisplayName("addAxis — default layer 자동 라우팅")
    class AddAxisRouting {

        @Test
        @DisplayName("addAxis_axis에_default_layer_할당됨")
        void addAxis_axis에_default_layer_할당됨() {
            LearningFacade facade = LearningFacade.create(user, "백엔드");

            LearningAxis axis = facade.addAxis("API 설계");

            LearningLayer defaultLayer = facade.getDefaultLayer();
            assertThat(axis.getLayer()).isSameAs(defaultLayer);
            assertThat(defaultLayer.getAxes()).contains(axis);
            assertThat(facade.getAxes()).contains(axis);
        }

        @Test
        @DisplayName("addAxis_facade_이름_중복_거부")
        void addAxis_facade_이름_중복_거부() {
            LearningFacade facade = LearningFacade.create(user, "백엔드");
            facade.addAxis("API");

            assertThatThrownBy(() -> facade.addAxis("API"))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_AXIS_DUPLICATE_NAME);
        }
    }

    @Nested
    @DisplayName("findLayer / getLayers")
    class Query {

        @Test
        @DisplayName("findLayer_존재하지_않는_id_예외")
        void findLayer_존재하지_않는_id_예외() {
            LearningFacade facade = LearningFacade.create(user, "백엔드");

            assertThatThrownBy(() -> facade.findLayer(999L))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_LAYER_NOT_FOUND);
        }

        @Test
        @DisplayName("getLayers_softDeleted_제외")
        void getLayers_softDeleted_제외() {
            LearningFacade facade = LearningFacade.create(user, "백엔드");
            LearningLayer target = facade.getDefaultLayer();
            // softDelete는 활성 axis 없어야 성공
            target.softDelete();

            assertThat(facade.getLayers()).isEmpty();
        }
    }
}
