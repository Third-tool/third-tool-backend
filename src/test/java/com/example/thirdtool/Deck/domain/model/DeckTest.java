package com.example.thirdtool.Deck.domain.model;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    // ─── DeckProgressStatus (Story-005-2) ──────────────────────────

    private Card mockCard(boolean archived, boolean deleted) {
        Card card = mock(Card.class);
        when(card.isArchived()).thenReturn(archived);
        when(card.isDeleted()).thenReturn(deleted);
        return card;
    }

    private void injectCards(Deck deck, Card... cards) {
        List<Card> list = new ArrayList<>(List.of(cards));
        ReflectionTestUtils.setField(deck, "cards", list);
    }

    @Test
    @DisplayName("기본 progressStatus — Deck 생성 직후 NOT_STARTED")
    void 기본_progressStatus_NOT_STARTED() {
        Deck deck = Deck.of("새 Deck", null, user);
        assertThat(deck.getProgressStatus()).isEqualTo(DeckProgressStatus.NOT_STARTED);
    }

    @Test
    @DisplayName("markInProgress — NOT_STARTED → IN_PROGRESS")
    void markInProgress_NOT_STARTED_전환() {
        Deck deck = Deck.of("새 Deck", null, user);

        deck.markInProgress();

        assertThat(deck.getProgressStatus()).isEqualTo(DeckProgressStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("markInProgress_멱등 — 이미 IN_PROGRESS면 무시")
    void markInProgress_이미IN_PROGRESS_무시() {
        Deck deck = Deck.of("새 Deck", null, user);
        deck.markInProgress();

        deck.markInProgress();  // 2회 호출

        assertThat(deck.getProgressStatus()).isEqualTo(DeckProgressStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("markInProgress_COMPLETED_무시 — 회귀는 recalculate가 담당")
    void markInProgress_COMPLETED_무시() {
        Deck deck = Deck.of("Deck", null, user);
        injectCards(deck, mockCard(true, false));
        deck.recalculateProgressStatus();
        assertThat(deck.getProgressStatus()).isEqualTo(DeckProgressStatus.COMPLETED);

        deck.markInProgress();  // COMPLETED 상태에서 호출 — 무시

        assertThat(deck.getProgressStatus()).isEqualTo(DeckProgressStatus.COMPLETED);
    }

    @Test
    @DisplayName("recalculate_빈Deck — NOT_STARTED 유지")
    void recalculate_빈Deck_NOT_STARTED() {
        Deck deck = Deck.of("빈 Deck", null, user);

        deck.recalculateProgressStatus();

        assertThat(deck.getProgressStatus()).isEqualTo(DeckProgressStatus.NOT_STARTED);
    }

    @Test
    @DisplayName("recalculate_모두ARCHIVE — COMPLETED")
    void recalculate_모두ARCHIVE_COMPLETED() {
        Deck deck = Deck.of("Deck", null, user);
        injectCards(deck, mockCard(true, false), mockCard(true, false));

        deck.recalculateProgressStatus();

        assertThat(deck.getProgressStatus()).isEqualTo(DeckProgressStatus.COMPLETED);
    }

    @Test
    @DisplayName("recalculate_일부ARCHIVE — IN_PROGRESS")
    void recalculate_일부ARCHIVE_IN_PROGRESS() {
        Deck deck = Deck.of("Deck", null, user);
        injectCards(deck, mockCard(true, false), mockCard(false, false));

        deck.recalculateProgressStatus();

        assertThat(deck.getProgressStatus()).isEqualTo(DeckProgressStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("recalculate_soft delete된 Card 제외 — 활성만 기준")
    void recalculate_softDelete_제외() {
        Deck deck = Deck.of("Deck", null, user);
        // 활성 1개(ARCHIVE) + soft delete 1개(ON_FIELD) — 활성만 보면 모두 ARCHIVE → COMPLETED
        injectCards(deck, mockCard(true, false), mockCard(false, true));

        deck.recalculateProgressStatus();

        assertThat(deck.getProgressStatus()).isEqualTo(DeckProgressStatus.COMPLETED);
    }

    @Test
    @DisplayName("recalculate_모두 soft delete — NOT_STARTED")
    void recalculate_모두soft_delete_NOT_STARTED() {
        Deck deck = Deck.of("Deck", null, user);
        injectCards(deck, mockCard(true, true), mockCard(false, true));

        deck.recalculateProgressStatus();

        assertThat(deck.getProgressStatus()).isEqualTo(DeckProgressStatus.NOT_STARTED);
    }

    @Test
    @DisplayName("recalculate_COMPLETED→returnToField로 회귀 시 IN_PROGRESS")
    void recalculate_COMPLETED_회귀() {
        Deck deck = Deck.of("Deck", null, user);
        Card a = mockCard(true, false);
        Card b = mockCard(true, false);
        injectCards(deck, a, b);
        deck.recalculateProgressStatus();
        assertThat(deck.getProgressStatus()).isEqualTo(DeckProgressStatus.COMPLETED);

        // a를 ON_FIELD로 회귀 시뮬레이션
        when(a.isArchived()).thenReturn(false);
        deck.recalculateProgressStatus();

        assertThat(deck.getProgressStatus()).isEqualTo(DeckProgressStatus.IN_PROGRESS);
    }
}
