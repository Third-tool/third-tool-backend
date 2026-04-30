package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.AxisTopic;
import com.example.thirdtool.LearningFacade.domain.model.CoverageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AxisTopicJpaRepository extends JpaRepository<AxisTopic, Long> {

    List<AxisTopic> findByAxisIdOrderByDisplayOrderAsc(Long axisId);

    List<AxisTopic> findByAxisIdAndCoverageStatus(Long axisId, CoverageStatus status);
}
