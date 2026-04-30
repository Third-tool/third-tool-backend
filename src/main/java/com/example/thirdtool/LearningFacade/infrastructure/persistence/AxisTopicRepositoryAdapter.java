package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.AxisTopic;
import com.example.thirdtool.LearningFacade.domain.model.CoverageStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AxisTopicRepositoryAdapter implements AxisTopicRepository {

    private final AxisTopicJpaRepository jpa;

    @Override
    public Optional<AxisTopic> findById(Long topicId) {
        return jpa.findById(topicId);
    }

    @Override
    public List<AxisTopic> findByAxisIdOrderByDisplayOrderAsc(Long axisId) {
        return jpa.findByAxisIdOrderByDisplayOrderAsc(axisId);
    }

    @Override
    public List<AxisTopic> findByAxisIdAndCoverageStatus(Long axisId, CoverageStatus status) {
        return jpa.findByAxisIdAndCoverageStatus(axisId, status);
    }

    @Override
    public AxisTopic save(AxisTopic topic) {
        return jpa.save(topic);
    }
}
