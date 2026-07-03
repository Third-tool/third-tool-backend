package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.Card.domain.model.CardStatus;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.LearningFacade.domain.model.AxisTopic;
import com.example.thirdtool.LearningFacade.domain.model.CoverageStatus;
import com.example.thirdtool.LearningFacade.domain.model.ProficiencyLevel;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicMaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DisplayName("CoverageRecalculator")
class CoverageRecalculatorTest {

    private TopicMaterialRepository topicMaterialRepository;
    private CardRepository cardRepository;
    private CoverageRecalculator recalculator;

    @BeforeEach
    void setUp() {
        topicMaterialRepository = Mockito.mock(TopicMaterialRepository.class);
        cardRepository = Mockito.mock(CardRepository.class);
        recalculator = new CoverageRecalculator(topicMaterialRepository, cardRepository);
    }

    private AxisTopic mockTopic(Long id) {
        AxisTopic topic = Mockito.mock(AxisTopic.class);
        when(topic.getId()).thenReturn(id);
        when(topic.getCoverageStatus()).thenReturn(CoverageStatus.NO_MATERIAL);
        return topic;
    }

    @Test
    @DisplayName("매핑이 0개이면 NO_MATERIAL이고 도메인 메서드로 상태가 갱신된다")
    void recalculate_no_mapping_returns_NO_MATERIAL() {
        AxisTopic topic = mockTopic(100L);
        when(topicMaterialRepository.countByTopicId(100L)).thenReturn(0L);

        CoverageStatus result = recalculator.recalculate(topic);

        assertThat(result).isEqualTo(CoverageStatus.NO_MATERIAL);
        Mockito.verify(topic).updateCoverageStatus(CoverageStatus.NO_MATERIAL);
    }

    @Test
    @DisplayName("매핑이 있고 MASTERED가 없으면 PARTIAL이다")
    void recalculate_no_mastered_returns_PARTIAL() {
        AxisTopic topic = mockTopic(100L);
        when(topicMaterialRepository.countByTopicId(100L)).thenReturn(2L);
        when(topicMaterialRepository.existsByTopicIdAndMaterialProficiencyLevel(
                100L, ProficiencyLevel.MASTERED)).thenReturn(false);

        CoverageStatus result = recalculator.recalculate(topic);

        assertThat(result).isEqualTo(CoverageStatus.PARTIAL);
        Mockito.verify(topic).updateCoverageStatus(CoverageStatus.PARTIAL);
    }

    @Test
    @DisplayName("매핑 중 MASTERED가 1개 이상이면 COVERED이다")
    void recalculate_has_mastered_returns_COVERED() {
        AxisTopic topic = mockTopic(100L);
        when(topicMaterialRepository.countByTopicId(100L)).thenReturn(2L);
        when(topicMaterialRepository.existsByTopicIdAndMaterialProficiencyLevel(
                100L, ProficiencyLevel.MASTERED)).thenReturn(true);

        CoverageStatus result = recalculator.recalculate(topic);

        assertThat(result).isEqualTo(CoverageStatus.COVERED);
        Mockito.verify(topic).updateCoverageStatus(CoverageStatus.COVERED);
    }

    // ─── recalculateByAxis (Story-LT-E4-S4-4) ─────────────────────

    @Nested
    @DisplayName("recalculateByAxis — 축 스코프 Coverage 재계산")
    class RecalculateByAxis {

        @Test
        @DisplayName("recalculateByAxis_카드없음_NO_MATERIAL")
        void recalculateByAxis_카드없음_NO_MATERIAL() {
            when(cardRepository.countByAxisIdAndDeletedFalse(200L)).thenReturn(0L);

            CoverageStatus result = recalculator.recalculateByAxis(200L);

            assertThat(result).isEqualTo(CoverageStatus.NO_MATERIAL);
            Mockito.verify(cardRepository, Mockito.never())
                   .countByAxisIdAndStatusAndDeletedFalse(Mockito.anyLong(), Mockito.any());
        }

        @Test
        @DisplayName("recalculateByAxis_모두ARCHIVE_COVERED")
        void recalculateByAxis_모두ARCHIVE_COVERED() {
            when(cardRepository.countByAxisIdAndDeletedFalse(200L)).thenReturn(3L);
            when(cardRepository.countByAxisIdAndStatusAndDeletedFalse(200L, CardStatus.ARCHIVE))
                    .thenReturn(3L);

            CoverageStatus result = recalculator.recalculateByAxis(200L);

            assertThat(result).isEqualTo(CoverageStatus.COVERED);
        }

        @Test
        @DisplayName("recalculateByAxis_일부ON_FIELD_PARTIAL")
        void recalculateByAxis_일부ON_FIELD_PARTIAL() {
            when(cardRepository.countByAxisIdAndDeletedFalse(200L)).thenReturn(3L);
            when(cardRepository.countByAxisIdAndStatusAndDeletedFalse(200L, CardStatus.ARCHIVE))
                    .thenReturn(2L);

            CoverageStatus result = recalculator.recalculateByAxis(200L);

            assertThat(result).isEqualTo(CoverageStatus.PARTIAL);
        }

        @Test
        @DisplayName("recalculateByAxis_null_axisId_예외")
        void recalculateByAxis_null_axisId_예외() {
            assertThatThrownBy(() -> recalculator.recalculateByAxis(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
