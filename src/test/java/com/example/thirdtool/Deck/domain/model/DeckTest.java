package com.example.thirdtool.Deck.domain.model;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Deck — createFromLearningMaterial + markMaterialDeleted (Story-005-1)")
class DeckTest {

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = UserEntity.ofLocal("tester", "encoded-pw", "닉네임", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    // ─── createFromLearningMaterial ──────────────────────────────────────

    @Test
    @DisplayName("createFromLearningMaterial_valid — 모든 필드 정상 매핑")
    void createFromLearningMaterial_valid() {
        Deck deck = Deck.createFromLearningMaterial(user, 100L, 200L, "도메인 주도 설계");

        assertThat(deck.getName()).isEqualTo("도메인 주도 설계");
        assertThat(deck.getAxisId()).isEqualTo(100L);
        assertThat(deck.getLearningMaterialId()).isEqualTo(200L);
        assertThat(deck.getUser()).isEqualTo(user);
        assertThat(deck.getParentDeck()).isNull();
        assertThat(deck.getDepth()).isZero();
        assertThat(deck.getMode()).isEqualTo(DeckMode.ON_FIELD);
        assertThat(deck.isDeleted()).isFalse();
        assertThat(deck.getLastAccessed()).isNotNull();
    }

    @Test
    @DisplayName("createFromLearningMaterial_axisId_null_허용 — linkedTopicIds 비어있는 자료")
    void createFromLearningMaterial_axisId_null_허용() {
        Deck deck = Deck.createFromLearningMaterial(user, null, 200L, "리소스 미연결 자료");

        assertThat(deck.getAxisId()).isNull();
        assertThat(deck.getLearningMaterialId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("createFromLearningMaterial_name_trim — 앞뒤 공백 제거")
    void createFromLearningMaterial_name_trim() {
        Deck deck = Deck.createFromLearningMaterial(user, 100L, 200L, "  클린 아키텍처  ");

        assertThat(deck.getName()).isEqualTo("클린 아키텍처");
    }

    @Test
    @DisplayName("createFromLearningMaterial_user_null_예외")
    void createFromLearningMaterial_user_null_예외() {
        assertThatThrownBy(() -> Deck.createFromLearningMaterial(null, 100L, 200L, "이름"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("user");
    }

    @Test
    @DisplayName("createFromLearningMaterial_materialId_null_예외")
    void createFromLearningMaterial_materialId_null_예외() {
        assertThatThrownBy(() -> Deck.createFromLearningMaterial(user, 100L, null, "이름"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("materialId");
    }

    @Test
    @DisplayName("createFromLearningMaterial_name_blank_예외 — DECK_NAME_BLANK")
    void createFromLearningMaterial_name_blank_예외() {
        assertThatThrownBy(() -> Deck.createFromLearningMaterial(user, 100L, 200L, "   "))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.DECK_NAME_BLANK);
    }

    @Test
    @DisplayName("createFromLearningMaterial_name_null_예외 — DECK_NAME_BLANK")
    void createFromLearningMaterial_name_null_예외() {
        assertThatThrownBy(() -> Deck.createFromLearningMaterial(user, 100L, 200L, null))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.DECK_NAME_BLANK);
    }

    // ─── markMaterialDeleted ──────────────────────────────────────

    @Test
    @DisplayName("markMaterialDeleted — learningMaterialId만 null, axisId는 보존")
    void markMaterialDeleted_정상() {
        Deck deck = Deck.createFromLearningMaterial(user, 100L, 200L, "자료 기반 Deck");

        deck.markMaterialDeleted();

        assertThat(deck.getLearningMaterialId()).isNull();
        assertThat(deck.getAxisId()).isEqualTo(100L);  // 보존
        assertThat(deck.getName()).isEqualTo("자료 기반 Deck");  // 이름 보존
    }

    @Test
    @DisplayName("markMaterialDeleted_멱등 — 이미 null이어도 예외 X")
    void markMaterialDeleted_멱등() {
        Deck deck = Deck.createFromLearningMaterial(user, 100L, 200L, "자료 기반 Deck");
        deck.markMaterialDeleted();

        // 2번째 호출도 안전
        deck.markMaterialDeleted();

        assertThat(deck.getLearningMaterialId()).isNull();
        assertThat(deck.getAxisId()).isEqualTo(100L);
    }
}
