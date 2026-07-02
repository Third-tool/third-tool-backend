package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.AxisRoadmapNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AxisRoadmapNodeJpaRepository extends JpaRepository<AxisRoadmapNode, Long> {

    List<AxisRoadmapNode> findByAxisIdOrderByDisplayOrderAsc(Long axisId);
}
