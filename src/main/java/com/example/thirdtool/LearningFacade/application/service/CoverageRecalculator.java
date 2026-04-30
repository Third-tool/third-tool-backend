package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.LearningFacade.domain.model.AxisTopic;
import com.example.thirdtool.LearningFacade.domain.model.CoverageStatus;
import com.example.thirdtool.LearningFacade.domain.model.ProficiencyLevel;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicMaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoverageRecalculator {

    private final TopicMaterialRepository topicMaterialRepository;

    public CoverageStatus recalculate(AxisTopic topic) {
        CoverageStatus newStatus = calculate(topic.getId());
        topic.updateCoverageStatus(newStatus);
        return newStatus;
    }

    private CoverageStatus calculate(Long topicId) {
        long count = topicMaterialRepository.countByTopicId(topicId);
        if (count == 0) {
            return CoverageStatus.NO_MATERIAL;
        }
        boolean hasMastered = topicMaterialRepository
                .existsByTopicIdAndMaterialProficiencyLevel(topicId, ProficiencyLevel.MASTERED);
        return hasMastered ? CoverageStatus.COVERED : CoverageStatus.PARTIAL;
    }
}
