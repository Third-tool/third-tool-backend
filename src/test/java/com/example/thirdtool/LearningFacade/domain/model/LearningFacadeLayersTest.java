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

    // ─── addLayer / reorderLayers / removeLayer (Story-S4·S5) ─

    @Nested
    @DisplayName("addLayer / renameLayer")
    class LayerLifecycle {

        @Test
        @DisplayName("addLayer_default이후_displayOrder는_max_plus_1")
        void addLayer_default이후_displayOrder는_max_plus_1() {
            LearningFacade facade = LearningFacade.create(user, "백엔드");

            LearningLayer added = facade.addLayer("UI");

            assertThat(added.getDisplayOrder()).isEqualTo(2);
            assertThat(facade.getLayers()).extracting(LearningLayer::getName)
                    .containsExactly("Uncategorized", "UI");
        }

        @Test
        @DisplayName("addLayer_동일이름_중복_예외")
        void addLayer_동일이름_중복_예외() {
            LearningFacade facade = LearningFacade.create(user, "백엔드");
            facade.addLayer("UI");

            assertThatThrownBy(() -> facade.addLayer("UI"))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_LAYER_DUPLICATE_NAME);
        }

        @Test
        @DisplayName("addLayer_5개_초과시_권장_한도_플래그_true")
        void addLayer_5개_초과시_권장_한도_플래그_true() {
            LearningFacade facade = LearningFacade.create(user, "백엔드");
            facade.addLayer("L2");
            facade.addLayer("L3");
            facade.addLayer("L4");
            facade.addLayer("L5"); // 총 5개 (default 포함)

            assertThat(facade.isLayerCountExceedsRecommended()).isFalse();

            facade.addLayer("L6"); // 6개

            assertThat(facade.isLayerCountExceedsRecommended()).isTrue();
        }

        @Test
        @DisplayName("renameLayer_동일값_no_op_false")
        void renameLayer_동일값_no_op_false() {
            LearningFacade facade = LearningFacade.create(user, "백엔드");
            LearningLayer layer = facade.addLayer("UI");
            ReflectionTestUtils.setField(layer, "id", 20L);

            boolean changed = facade.renameLayer(20L, "UI");

            assertThat(changed).isFalse();
        }

        @Test
        @DisplayName("renameLayer_다른_활성layer와_중복_예외")
        void renameLayer_다른_활성layer와_중복_예외() {
            LearningFacade facade = LearningFacade.create(user, "백엔드");
            LearningLayer ui = facade.addLayer("UI");
            LearningLayer view = facade.addLayer("View");
            ReflectionTestUtils.setField(ui, "id", 20L);
            ReflectionTestUtils.setField(view, "id", 30L);

            assertThatThrownBy(() -> facade.renameLayer(30L, "UI"))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_LAYER_DUPLICATE_NAME);
        }
    }

    @Nested
    @DisplayName("removeLayer — default 보호")
    class RemoveLayer {

        @Test
        @DisplayName("removeLayer_default_Uncategorized_삭제_시도_예외")
        void removeLayer_default_Uncategorized_삭제_시도_예외() {
            LearningFacade facade = LearningFacade.create(user, "백엔드");
            LearningLayer defaultLayer = facade.getDefaultLayer();
            ReflectionTestUtils.setField(defaultLayer, "id", 10L);

            assertThatThrownBy(() -> facade.removeLayer(10L))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_LAYER_HAS_ACTIVE_AXES);
        }

        @Test
        @DisplayName("removeLayer_non_default_활성_axis_없음_성공")
        void removeLayer_non_default_활성_axis_없음_성공() {
            LearningFacade facade = LearningFacade.create(user, "백엔드");
            LearningLayer ui = facade.addLayer("UI");
            ReflectionTestUtils.setField(ui, "id", 20L);

            facade.removeLayer(20L);

            assertThat(ui.isDeleted()).isTrue();
            assertThat(facade.getLayers()).hasSize(1); // default만 남음
        }
    }

    @Nested
    @DisplayName("reorderLayers")
    class Reorder {

        @Test
        @DisplayName("reorderLayers_정상_displayOrder_재부여")
        void reorderLayers_정상_displayOrder_재부여() {
            LearningFacade facade = LearningFacade.create(user, "백엔드");
            LearningLayer defaultLayer = facade.getDefaultLayer();
            ReflectionTestUtils.setField(defaultLayer, "id", 10L);
            LearningLayer ui = facade.addLayer("UI");
            LearningLayer view = facade.addLayer("View");
            ReflectionTestUtils.setField(ui, "id", 20L);
            ReflectionTestUtils.setField(view, "id", 30L);

            facade.reorderLayers(java.util.List.of(30L, 10L, 20L));

            assertThat(facade.getLayers()).extracting(LearningLayer::getName)
                    .containsExactly("View", "Uncategorized", "UI");
        }

        @Test
        @DisplayName("reorderLayers_id_집합_불일치_예외")
        void reorderLayers_id_집합_불일치_예외() {
            LearningFacade facade = LearningFacade.create(user, "백엔드");
            LearningLayer defaultLayer = facade.getDefaultLayer();
            ReflectionTestUtils.setField(defaultLayer, "id", 10L);
            LearningLayer ui = facade.addLayer("UI");
            ReflectionTestUtils.setField(ui, "id", 20L);

            assertThatThrownBy(() -> facade.reorderLayers(java.util.List.of(10L, 99L)))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_LAYER_REORDER_MISMATCH);
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
