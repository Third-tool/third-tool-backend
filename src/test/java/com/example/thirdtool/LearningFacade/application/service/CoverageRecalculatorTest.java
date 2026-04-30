package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.LearningFacade.domain.model.AxisTopic;
import com.example.thirdtool.LearningFacade.domain.model.CoverageStatus;
import com.example.thirdtool.LearningFacade.domain.model.ProficiencyLevel;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicMaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("CoverageRecalculator")
class CoverageRecalculatorTest {

    private TopicMaterialRepository topicMaterialRepository;
    private CoverageRecalculator recalculator;

    @BeforeEach
    void setUp() {
        topicMaterialRepository = Mockito.mock(TopicMaterialRepository.class);
        recalculator = new CoverageRecalculator(topicMaterialRepository);
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
}
