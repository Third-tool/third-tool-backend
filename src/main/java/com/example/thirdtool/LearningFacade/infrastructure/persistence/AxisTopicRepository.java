package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.AxisTopic;
import com.example.thirdtool.LearningFacade.domain.model.CoverageStatus;

import java.util.List;
import java.util.Optional;

public interface AxisTopicRepository {

    Optional<AxisTopic> findById(Long topicId);

    List<AxisTopic> findByAxisIdOrderByDisplayOrderAsc(Long axisId);

    List<AxisTopic> findByAxisIdAndCoverageStatus(Long axisId, CoverageStatus status);

    AxisTopic save(AxisTopic topic);
}
