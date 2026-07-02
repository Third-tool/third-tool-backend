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
 * Story-LT-E2-S1 · LearningLayer 도메인 단위 테스트.
 * 팩토리·정규화·softDelete 가드·isDefault 판정.
 */
@DisplayName("LearningLayer")
class LearningLayerTest {

    private UserEntity user;
    private LearningFacade facade;

    @BeforeEach
    void setUp() {
        user = UserEntity.ofLocal("tester", "encoded-pw", "닉", "t@t.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        facade = LearningFacade.create(user, "백엔드");
        ReflectionTestUtils.setField(facade, "id", 100L);
    }

    // ─── 팩토리 ────────────────────────────────────────────

    @Nested
    @DisplayName("of — 팩토리")
    class Of {

        @Test
        @DisplayName("of_유효값_생성")
        void of_유효값_생성() {
            LearningLayer layer = LearningLayer.of(facade, "UI", 1);

            assertThat(layer.getFacade()).isSameAs(facade);
            assertThat(layer.getName()).isEqualTo("UI");
            assertThat(layer.getDisplayOrder()).isEqualTo(1);
            assertThat(layer.isDeleted()).isFalse();
            assertThat(layer.isDefault()).isFalse();
        }

        @Test
        @DisplayName("of_공백_trim_처리")
        void of_공백_trim_처리() {
            LearningLayer layer = LearningLayer.of(facade, "  UI  ", 1);
            assertThat(layer.getName()).isEqualTo("UI");
        }

        @Test
        @DisplayName("of_default_이름은_isDefault_true")
        void of_default_이름은_isDefault_true() {
            LearningLayer layer = LearningLayer.of(facade, "Uncategorized", 1);
            assertThat(layer.isDefault()).isTrue();
        }

        @Test
        @DisplayName("of_name_blank_예외")
        void of_name_blank_예외() {
            assertThatThrownBy(() -> LearningLayer.of(facade, "  ", 1))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_LAYER_NAME_BLANK);
        }

        @Test
        @DisplayName("of_name_null_예외")
        void of_name_null_예외() {
            assertThatThrownBy(() -> LearningLayer.of(facade, null, 1))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_LAYER_NAME_BLANK);
        }

        @Test
        @DisplayName("of_name_101자_TOO_LONG_예외")
        void of_name_101자_TOO_LONG_예외() {
            assertThatThrownBy(() -> LearningLayer.of(facade, "a".repeat(101), 1))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_LAYER_NAME_TOO_LONG);
        }

        @Test
        @DisplayName("of_facade_null_예외")
        void of_facade_null_예외() {
            assertThatThrownBy(() -> LearningLayer.of(null, "UI", 1))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("of_displayOrder_0_이하_예외")
        void of_displayOrder_0_이하_예외() {
            assertThatThrownBy(() -> LearningLayer.of(facade, "UI", 0))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        }
    }

    // ─── softDelete ─────────────────────────────────────

    @Nested
    @DisplayName("softDelete — 활성 axis 가드")
    class SoftDelete {

        @Test
        @DisplayName("softDelete_axis_0건_성공_deletedAt_설정")
        void softDelete_axis_0건_성공_deletedAt_설정() {
            LearningLayer layer = LearningLayer.of(facade, "UI", 1);

            layer.softDelete();

            assertThat(layer.isDeleted()).isTrue();
            assertThat(layer.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("softDelete_이미_삭제된_상태_ALREADY_DELETED_예외")
        void softDelete_이미_삭제된_상태_ALREADY_DELETED_예외() {
            LearningLayer layer = LearningLayer.of(facade, "UI", 1);
            layer.softDelete();

            assertThatThrownBy(layer::softDelete)
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_LAYER_ALREADY_DELETED);
        }
    }

    // ─── updateName ──────────────────────────────────────

    @Nested
    @DisplayName("updateName")
    class UpdateName {

        @Test
        @DisplayName("updateName_동일값_no_op_false_반환")
        void updateName_동일값_no_op_false_반환() {
            LearningLayer layer = LearningLayer.of(facade, "UI", 1);

            boolean changed = layer.updateName("UI");

            assertThat(changed).isFalse();
        }

        @Test
        @DisplayName("updateName_새값_true_반환_변경")
        void updateName_새값_true_반환_변경() {
            LearningLayer layer = LearningLayer.of(facade, "UI", 1);

            boolean changed = layer.updateName("View");

            assertThat(changed).isTrue();
            assertThat(layer.getName()).isEqualTo("View");
        }
    }
}
